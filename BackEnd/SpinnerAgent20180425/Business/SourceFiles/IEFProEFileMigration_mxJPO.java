/*
 * IEFProEFileMigration.java program to get all document type Object Ids.
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.MQLCommand;
import matrix.db.MatrixWriter;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class IEFProEFileMigration_mxJPO
{
    private BufferedWriter writer		= null;
	private FileWriter iefLog			= null;
	private long startTime				= 0L;

	private MCADMxUtil mxUtil			= null;
    private String documentDirectory	= "";
    private String operation			= "";
    private String inputFile			= "";

	private Hashtable integNameGCOMap	= null;
	private Hashtable locationFileMap	= null;

    public IEFProEFileMigration_mxJPO (Context context, String[] args)
        throws Exception
    {
		writer = new BufferedWriter(new MatrixWriter(context));
		mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle("en-US"), new IEFGlobalCache());
    }

    public int mxMain(Context context, String[] args)
        throws Exception
    {
		if(!context.isConnected())
		{
			throw new Exception("not supported on desktop client");
		}

		try
		{
			validateInputArguments(args);
		}
		catch (Exception iExp)
		{
			writeErrorToConsole(iExp.getMessage());
			writer.close();
			return 0;
		}

		try
		{
			startIEFLog();
			String Args[] = new String[1];
			Args[0] = "off";
			mxUtil.executeMQL(context,"trigger $1", Args);
			integNameGCOMap	= new Hashtable();
			locationFileMap = new Hashtable();

			logTimeForEvent("START ProE FindFiles");
			startTime = System.currentTimeMillis();

			if(operation.equalsIgnoreCase("find"))
			{
				mxUtil.startTransaction(context);
				findFilesForProE(context);
				log("Commiting transaction");
				context.commit();
			}
			else if(operation.equalsIgnoreCase("rename"))
			{
				renameFilesForProE(context);
			}


			logTimeForEvent("ProE FindFiles COMPLETE");

			endLocationFiles();
			writeSuccessToConsole();
		}
		catch (FileNotFoundException fEx)
		{
			writeErrorToConsole("Directory does not exist or does not have access to the directory");
		}
		catch (Exception ex)
		{
			writeErrorToConsole("Family migration failed: " + ex.getMessage());
			ex.printStackTrace(new PrintWriter(iefLog, true));
		}

		endIEFLog();
		writer.flush();
		writer.close();
        return 0;
    }

	private void renameFilesForProE(Context context) throws Exception
	{
		writer.write("Reading ID file: " + inputFile + "\n");

		String fileNameWithPath		= documentDirectory + inputFile;
		BufferedReader iefIDsReader = new BufferedReader(new FileReader(fileNameWithPath));

        String objectIdLine = "";
		while((objectIdLine = iefIDsReader.readLine()) != null)
		{
			StringTokenizer tokenizer = new StringTokenizer(objectIdLine, "|");
			if(!tokenizer.hasMoreElements())
				continue;

			String id				= (String) tokenizer.nextElement();
			String format			= (String) tokenizer.nextElement();
			String fileName			= (String) tokenizer.nextElement();
			String changedFileName	= (String) tokenizer.nextElement();

			try
			{
				mxUtil.startTransaction(context);
				renameFile(context, id, format, fileName, changedFileName);
				resetFileRelatedAttributes(context, id, fileName, changedFileName);
				context.commit();
			}
			catch(Exception e)
			{
				log("ERROR: Can not rename file: " + e.getMessage());
				log(objectIdLine);
				log("");
				context.abort();
			}

		}
	}

	private void resetFileRelatedAttributes(Context context, String id, String fileName, String changedFileName) throws Exception
	{
		String attrTitle 				= MCADMxUtil.getActualNameForAEFData(context, "attribute_Title");
		String attrIEFileMessageDigest 	= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-FileMessageDigest");
		

		String SELECT_TITLE               = new StringBuffer("attribute[").append(attrTitle).append("]").toString();
		String SELECT_FILE_MESSAGE_DIGEST = new StringBuffer("attribute[").append(attrIEFileMessageDigest).append("]").toString();

		StringList  selectAttrList        = new StringList();
		selectAttrList.add(SELECT_TITLE);
		selectAttrList.add(SELECT_FILE_MESSAGE_DIGEST);
		
		BusinessObjectWithSelectList busWithSelectList  = BusinessObject.getSelectBusinessObjectData(context, new String[] {id}, selectAttrList);
		BusinessObjectWithSelect     busWithSelect      = busWithSelectList.getElement(0);
		
		String title    = busWithSelect.getSelectData(SELECT_TITLE);
		String hashcode  = busWithSelect.getSelectData(SELECT_FILE_MESSAGE_DIGEST);

		if(title != null)
			title = mxUtil.replace(title, fileName, changedFileName);
		
		if(hashcode != null)
				hashcode = mxUtil.replace(hashcode, fileName, changedFileName);

		BusinessObject busObject = new BusinessObject(id);
		
		AttributeList attributelist = new AttributeList(2);
		attributelist.addElement(new Attribute(new AttributeType(attrTitle), title));
		attributelist.addElement(new Attribute(new AttributeType(attrIEFileMessageDigest), hashcode));
        busObject.open(context,false);
		busObject.setAttributes(context, attributelist);
        busObject.close(context);
	}

	private void renameFile(Context context, String id, String format, String fileName, String changedFileName) throws Exception
	{
		BusinessObject busObject = new BusinessObject(id);
		log("Bus Object: " + id);
		busObject.checkoutFile(context, false, format, fileName, documentDirectory);
		java.io.File fileToRename	= new java.io.File(documentDirectory + fileName);
		java.io.File newFilePath	= new java.io.File(documentDirectory + changedFileName);

		boolean isRenamed = fileToRename.renameTo(newFilePath);
		if(isRenamed)
		{
			busObject.checkinFile(context, false, false, "localhost", format, changedFileName, documentDirectory);
			newFilePath.delete();
		}
		else
		{
			log("We have encountered an error while rename the file :" + fileName);
			fileToRename.delete();
		}
		log("Filename changed from " + fileName + " to " + changedFileName);
	}

	private String executeMQL(Context context,String cmd, String args[]) throws Exception
	{
		log("Executing MQL Command: " + cmd);
		MQLCommand mqlCommand = new MQLCommand();
		boolean bRet = mqlCommand.executeCommand(context, cmd, args);
		if (bRet)
			return mqlCommand.getResult();
		else
			throw new Exception(mqlCommand.getError());
	}

	private void validateInputArguments(String[] args) throws Exception
	{
		if (args.length < 2 )
			throw new IllegalArgumentException("Wrong number of arguments");

		documentDirectory	= args[0];
		operation			= args[1];

		if(operation.equalsIgnoreCase("rename"))
		{
			if (args.length < 3)
				throw new IllegalArgumentException("Wrong number of arguments");
			inputFile = args[2];
		}

		String fileSeparator = java.io.File.separator;
		if(documentDirectory != null && !documentDirectory.endsWith(fileSeparator))
			documentDirectory = documentDirectory + fileSeparator;

	}


///////////////////////////////////////////////////////////////////////////////////

	private void findFilesForProE(Context context) throws Exception
	{
		MCADGlobalConfigObject globalConf = getGlobalConfigObject(context, "MxPRO");
		if(globalConf != null)
		{
			log("MxPro is installed. Migrating file names");
			Vector cadTypeList = globalConf.getTypeListForClass("TYPE_CADMODEL_LIKE");
			for(int i=0; i<cadTypeList.size(); i++)
			{
				String cadType = (String) cadTypeList.elementAt(i);
				Vector typeList = globalConf.getMxTypeListForCADTypeFromFormatMapping(cadType);
				Vector templateTypes = globalConf.getTemplateTypesForCADType(cadType);
				for(int j=0; j<typeList.size(); j++)
				{
					String mxType	= (String) typeList.elementAt(j);
					if(templateTypes != null && templateTypes.contains(mxType))
						continue;

					String format	= globalConf.getFormatsForType(mxType, cadType);
					String minorType= mxUtil.getCorrespondingType(context, mxType);

					String Args[] = new String[7];
					Args[0] = mxType+","+minorType;
					Args[1] = "*";
					Args[2] = "*";
					Args[3] = "id";
					Args[4] = "format[" + format + "].file.name";
					Args[5] = "format[" + format + "].file.location";
					Args[6] = "|";

					String mxProObjects		 = executeMQL(context,"temp query bus $1 $2 $3 select $4 $5 $6 dump $7", Args);
					StringTokenizer rowToken = new StringTokenizer(mxProObjects, "\n");
					while(rowToken.hasMoreElements())
					{
						String mxProRow			 = (String) rowToken.nextElement();
						StringTokenizer colToken = new StringTokenizer(mxProRow, "|");
						if(colToken.hasMoreElements())
						{
							String type = (String) colToken.nextElement();
							String name = (String) colToken.nextElement();
							String rev  = (String) colToken.nextElement();
							String id	= (String) colToken.nextElement();
							while(colToken.hasMoreElements())
							{
								String fileName = (String) colToken.nextElement();
								String changedFileName = truncateNumeralExtension(fileName);
								if(!fileName.equals(changedFileName))
								{
									String location = "UNKNOWN";
									if(colToken.hasMoreElements())
										location = (String) colToken.nextElement();

									writeToLocationFile(location, id, format, fileName, changedFileName);
								}
							}
						}
					}
				}
			}
		}
	}

	private MCADGlobalConfigObject getGlobalConfigObject(Context context, String integrationName) throws Exception
	{
		MCADGlobalConfigObject gco = (MCADGlobalConfigObject) integNameGCOMap.get(integrationName);
		if(gco == null)
		{
			log("Reading GCO Integration: '" + integrationName + "'");
			IEFGetRegistrationDetails_mxJPO registrationDetailsReader = new IEFGetRegistrationDetails_mxJPO(context, null);
			String args[] = new String[1];
			args[0] = integrationName;
			String registrationDetails = registrationDetailsReader.getRegistrationDetails(context, args);

			if(registrationDetails != null && !"".equals(registrationDetails.trim()))
			{
				String gcoName = registrationDetails.substring(registrationDetails.lastIndexOf("|")+1);
				log("gcoName = " + gcoName);

				MCADConfigObjectLoader configLoader = new MCADConfigObjectLoader(null);
				gco = configLoader.createGlobalConfigObject(context, mxUtil, MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig"), gcoName);
				integNameGCOMap.put(integrationName, gco);
			}
		}

		return gco;
	}

	private String truncateNumeralExtension(String origFileName)
	{
		String fileName = origFileName;

		int lastIndex = origFileName.lastIndexOf(".");
		String extension = null;
		if(lastIndex > 0)
			extension = origFileName.substring(lastIndex+1);

		if (extension != null)
		{
			try
			{
				int number = Integer.parseInt(extension);
				fileName = origFileName.substring(0, lastIndex);
				log("File name chenged from '" + origFileName + "' to '" + fileName + "'");
			}
			catch(NumberFormatException nfe)
			{
			}
		}

		return fileName;
	}

	private void writeToLocationFile(String location, String id, String format, String fileName, String changedFileName) throws Exception
	{
		FileWriter idWriter = (FileWriter)locationFileMap.get(location);
		if(idWriter == null)
		{
			idWriter = new FileWriter(documentDirectory + "ProEObjectID_" + location + ".txt");
			locationFileMap.put(location, idWriter);
		}
		idWriter.write(id + "|" + format + "|" + fileName + "|" + changedFileName + "\n");
	}

	private void endLocationFiles() throws Exception
	{
		log("Closing Object ID Files");
		Enumeration locEnum = locationFileMap.keys();
		while(locEnum.hasMoreElements())
		{
			String location = (String) locEnum.nextElement();
			FileWriter idWriter = (FileWriter) locationFileMap.get(location);
			idWriter.flush();
			idWriter.close();
		}
	}


///////////////////////////////////////////////////////////////////////////////////

	private void writeLineToConsole() throws Exception
	{
		writeMessageToConsole("=======================================================");
	}

	private void writeMessageToConsole(String message) throws Exception
	{
		writer.write(message + "\n");
	}

	private void writeErrorToConsole(String message) throws Exception
	{
		writeLineToConsole();
		writeMessageToConsole(message);
		writeMessageToConsole("Step 2 of Migration     : FAILED");
		writeLineToConsole();
		writer.flush();
	}

	private void writeSuccessToConsole() throws Exception
	{
		writeLineToConsole();
		writeMessageToConsole("                Migration for Family Objects  COMPLETE");
		writeMessageToConsole("                Time:"+ (System.currentTimeMillis() - startTime) + "ms ");
		writeMessageToConsole("                Step 2 of Migration     : SUCCESS");
		writeLineToConsole();
		writer.flush();
	}

	private void startIEFLog() throws Exception
	{
		try
		{
			iefLog	= new FileWriter(documentDirectory + "iefProEMigration10_6.log");
		}
		catch(Exception e)
		{
			writeMessageToConsole("ERROR: Can not create log file. " + e.getMessage());
		}
	}

	private void endIEFLog()
	{
		try
		{
			iefLog.write("\n\n");
			iefLog.flush();
			iefLog.close();
		}
		catch(Exception e)
		{
		}
	}

	private void log(String message)
	{
		try
		{
			iefLog.write(message + "\n");
		}
		catch(Exception e)
		{
		}
	}

	private void logTimeForEvent(String event)
	{
		log("\n\n" + event + " Time: " + System.currentTimeMillis() + "\n\n");
	}
}

