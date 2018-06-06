/*
 * IEFInstanceNameMigration.java program to get all document type Object Ids.
 *
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
 *
 */

/*
 * Instance Name modifications
 * 1. If instance name is of type instanceName[familyName] then change it to instanceName(familyName)
 *
 * Working set modifications
 * 1. If instance name is of type instanceName[familyName] then change it to instanceName(familyName)
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.MQLCommand;
import matrix.db.MatrixWriter;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class IEFInstanceNameMigration_mxJPO
{
    private BufferedWriter writer		= null;
	private FileWriter iefLog			= null;
	private FileWriter errorIDsFile		= null;
	private long startTime				= 0L;

	private MCADMxUtil mxUtil			= null;
    private String documentDirectory	= "";
	private String gcoNames				= "";
	private Hashtable instanceTNRinstanceNewNameTable = null;

    public IEFInstanceNameMigration_mxJPO (Context context, String[] args)
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
			instanceTNRinstanceNewNameTable = new Hashtable();

			logTimeForEvent("START MIGRATION");
			startTime = System.currentTimeMillis();

			mxUtil.startTransaction(context);
			renameInstances(context);
			migrateAllWorkingSets(context);
			log("Commiting transaction");
			context.commit();
			
			logTimeForEvent("MIGRATION COMPLETE");
			writeSuccessToConsole();
		}
		catch (FileNotFoundException fEx)
		{
			writeErrorToConsole("Directory does not exist or does not have access to the directory");
			context.abort();
		}
		catch (Exception ex)
		{
			writeErrorToConsole("InstanceName migration failed: " + ex.getMessage());
			ex.printStackTrace(new PrintWriter(iefLog, true));
			context.abort();
		}

		endIEFLog();
		writer.flush();
		writer.close();
        return 0;
    }

	private void validateInputArguments(String[] args) throws Exception
	{		
		if (args.length < 2)
			throw new IllegalArgumentException("Wrong number of arguments");

		documentDirectory	= args[0];
		gcoNames			= args[1];
	
		String fileSeparator = java.io.File.separator;
		if(documentDirectory != null && !documentDirectory.endsWith(fileSeparator))
			documentDirectory = documentDirectory + fileSeparator;
	}

	private void renameInstances(Context context) throws Exception
	{
		String instanceBusTypeList = getInstanceTypeNames(context);
		if(instanceBusTypeList.equals(""))
		{
			log("Mapping not found in given GCOs for Instance types");
			throw new Exception("Mapping not found in given GCOs for Instance types");
		}
		log("Getting types: " + instanceBusTypeList);

		String cmd = "temp query bus '" + instanceBusTypeList + "' \"*[*\" * select dump |";

		String mxObjects		 = executeMQL(context,cmd);
		StringTokenizer rowToken = new StringTokenizer(mxObjects, "\n");
		while(rowToken.hasMoreElements())
		{
			String mxRow = (String) rowToken.nextElement();
			StringTokenizer colToken = new StringTokenizer(mxRow, "|");
			if(colToken.hasMoreElements())
			{
				String type = (String) colToken.nextElement();
				String name = (String) colToken.nextElement();
				String rev  = (String) colToken.nextElement();
								
				if(name.indexOf("[") > 0 && name.indexOf("]") > 0)
				{
					renameObject(context, type, name, rev);
				}
			}
		}
	}

	private String getInstanceTypeNames(Context context) throws Exception
	{
		String typeList = "";
		StringTokenizer gcoToken = new StringTokenizer(gcoNames, ",");
		while(gcoToken.hasMoreElements())
		{
			String gcoName = (String) gcoToken.nextElement();
			BusinessObject gcoObject = getGCOObjectFromName(context, gcoName);
			if(gcoObject != null)
			{
				String typeClassMapping		= gcoObject.getAttributeValues(context, MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-TypeClassMapping")).getValue();
				String typeFormatMapping	= gcoObject.getAttributeValues(context, MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-TypeFormatMapping")).getValue();

				Vector instanceClassList = getValueFromMapping(typeClassMapping, "TYPE_INSTANCE_LIKE");
				for(int i=0; i<instanceClassList.size(); i++)
				{
					String cadType = (String) instanceClassList.elementAt(i);
					Vector mappedTypes = getValueFromMapping(typeFormatMapping, cadType);
					for(int j=0; j<mappedTypes.size(); j+=2)
					{
						String mxType	 = (String) mappedTypes.elementAt(j);
						String minorType = mxUtil.getCorrespondingType(context, mxType);
						typeList += mxType + "," + minorType + ",";
					}
				}
			}
			else
			{
				writeMessageToConsole("No GCO found with name: " + gcoName);
			}
		}
		return typeList;
	}

	private BusinessObject getGCOObjectFromName(Context context, String gcoName) throws Exception
	{
		BusinessObject gcoObject = null;
		MQLCommand mqlCommand = new MQLCommand();
		mqlCommand.executeCommand(context, "temp query bus $1 $2 $3 select $4 dump $5", "MCADInteg-GlobalConfig",gcoName, "1", "id", "|" );
		String result = mqlCommand.getResult();
		if(result != null && result.length() > 0)
		{
			String gcoId = result.substring(result.lastIndexOf("|")+1);
			gcoObject = new BusinessObject(gcoId);
		}
		return gcoObject;
	}

	private Vector getValueFromMapping(String mapping, String key)
	{
		Vector valueList = new Vector();
		StringTokenizer tokenizer = new StringTokenizer(mapping, "\n");
		while(tokenizer.hasMoreElements())
		{
			String row = (String) tokenizer.nextElement();
			if(row.startsWith(key+"|"))
			{
				String mappedValue = row.substring(row.indexOf("|")+1);
				StringTokenizer valTok = new StringTokenizer(mappedValue, ",");
				while(valTok.hasMoreElements())
				{
					valueList.addElement(valTok.nextElement());
				}
			}
		}
		return valueList;
	}

	private void renameObject(Context context, String type, String name, String rev) throws Exception
	{
		String changedName = name.replace('[' , '(');
		changedName = changedName.replace(']' , ')');

		log("Bus Object: '" + type + "' '" + name + "' " + rev);

		String Args[] = new String[4];
		Args[0] = type;
		Args[1] = name;
		Args[2] = rev;
		Args[3] = changedName;
		executeMQL(context,"modify bus $1 $2 $3 name $4", Args);

		instanceTNRinstanceNewNameTable.put(type+"|"+name+"|"+rev , changedName);
		
		log("Object changed from " + name + " to " + changedName);
	}

	private void migrateAllWorkingSets(Context context) throws Exception
	{
		if(instanceTNRinstanceNewNameTable.size() < 1)
		{
			log("\nCan not update working set as instanceTNRinstanceNewNameTable is empty");
			return;
		}

		log("\n\n===============================================");
		log("\nStarted Working set migration");
		log("instanceTNRinstanceNewNameTable = " + instanceTNRinstanceNewNameTable);
		String Args[] = new String[6];
		Args[0] = "MCADInteg-LocalConfig";
		Args[1] = "*";
		Args[2] = "1";
		Args[3] = "id";
		Args[4] = "format.file.store";
		Args[5] = "|";
		
		String lcoIDList = executeMQL(context,"temp query bus $1 $2 $3 select $4 $5 dump $6", Args);

		StringTokenizer tokenizer = new StringTokenizer(lcoIDList, "\n");
		while(tokenizer.hasMoreElements())
		{
			String lcoObjDetails = (String) tokenizer.nextElement();
			log("lcoObjDetails = " + lcoObjDetails);
			StringTokenizer lcoTok = new StringTokenizer(lcoObjDetails, "|");
			if(lcoTok.hasMoreElements())
			{
				String lcoType	= (String) lcoTok.nextElement();
				String lcoName	= (String) lcoTok.nextElement();
				String lcoRev	= (String) lcoTok.nextElement();
				String lcoID	= (String) lcoTok.nextElement();
				
				String store = null;
				if(lcoTok.hasMoreElements())
					store = (String) lcoTok.nextElement();
				else
					continue;

				String tempDir = documentDirectory;
				modifyWorkingSetFile(context, lcoID, store, tempDir);
			}
		}
		
		log("\nCompleted Working set migration");
	}

	private void modifyWorkingSetFile(Context context, String lcoID, String store, String tempDir) throws Exception
	{
		log("Working set file processing started for LCO ID: " + lcoID);
		log("Temp Dir === " + tempDir);
		StringBuffer modifiedWSEntry = new StringBuffer("");
		BusinessObject lcoObj = new BusinessObject(lcoID);
		lcoObj.checkoutFile(context, false, "generic", "DefaultWorkingSet", tempDir);

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tempDir + java.io.File.separator + "DefaultWorkingSet")));
		String line = null;
		while((line = br.readLine()) != null)
		{
			StringTokenizer tokenizer = new StringTokenizer(line , "|");
			if(tokenizer.countTokens() > 3)
			{
				String type			= (String) tokenizer.nextElement();
				String name			= (String) tokenizer.nextElement();
				String rev			= (String) tokenizer.nextElement();
				String id			= (String) tokenizer.nextElement();
				String format		= (String) tokenizer.nextElement();
				String fileName		= (String) tokenizer.nextElement();
				String lock			= (String) tokenizer.nextElement();
				String refCount		= "0";
				String hashCode		= "";

				if(tokenizer.hasMoreElements())
					refCount = (String) tokenizer.nextElement();
				if(tokenizer.hasMoreElements())
					hashCode = (String) tokenizer.nextElement();
				
				if(instanceTNRinstanceNewNameTable.containsKey(type+"|"+name+"|"+rev))
				{
					log("Instance Found: " + type+"|"+name+"|"+rev);					
					String changedName = (String)instanceTNRinstanceNewNameTable.get(type+"|"+name+"|"+rev);					
					modifiedWSEntry.append(type).append("|")
									.append(changedName).append("|")
									.append(rev).append("|")
									.append(id).append("|")
									.append(format).append("|")
									.append(fileName).append("|")
									.append(lock).append("|")
									.append(refCount).append("|")
									.append(hashCode).append("\n");
				}
				else
					modifiedWSEntry.append(line).append("\n");						
			}
			else
			{
				modifiedWSEntry.append(line).append("\n");
			}
		}
		br.close();

		log("File update started");
		FileWriter wsWriter	= new FileWriter(documentDirectory + "DefaultWorkingSet");
		wsWriter.write(modifiedWSEntry.toString());
		wsWriter.flush();
		wsWriter.close();

		log("File checkin started");
		lcoObj.checkinFile(context, false, false, "localhost", "generic", store, "DefaultWorkingSet", tempDir);
		log("Working set file processing completed\n");
		log("*******************************************");
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
	private String executeMQL(Context context,String cmd) throws Exception
	{
		log("Executing MQL Command: " + cmd);
		MQLCommand mqlCommand = new MQLCommand();
		boolean bRet = mqlCommand.executeCommand(context, cmd);
		if (bRet)
			return mqlCommand.getResult();
		else
			throw new Exception(mqlCommand.getError());
	}

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
		writeMessageToConsole("                Migration COMPLETE");
		writeMessageToConsole("                Time:"+ (System.currentTimeMillis() - startTime) + "ms ");
		writeMessageToConsole("                Step 2 of Migration     : SUCCESS");
		writeLineToConsole();
		writer.flush();
	}

	private void startIEFLog() throws Exception
	{
		try
		{
			iefLog		= new FileWriter(documentDirectory + "iefMigration10_6_SP2.log");
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

			if(errorIDsFile != null)
			{
				errorIDsFile.flush();
				errorIDsFile.close();
			}
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

