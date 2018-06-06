/*
 * IEFFamilyMigrationFindObjects.java program to get all document type Object Ids.
 *
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
 *
 */

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.MQLCommand;
import matrix.db.MatrixWriter;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class IEFFamilyMigrationFindObjects_mxJPO
{
    private BufferedWriter writer		= null;
	private FileWriter iefIDsFile		= null;
    private String documentDirectory	= "";
    private String gcoNames				= "";

	private MCADMxUtil mxUtil			= null;

    /**
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @grade 0
     */
    public IEFFamilyMigrationFindObjects_mxJPO (Context context, String[] args)
        throws Exception
    {
		writer = new BufferedWriter(new MatrixWriter(context));
		mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle("en-US"), new IEFGlobalCache());
    }

    /**
     * This method is executed if a specific method is not specified.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @returns nothing
     * @throws Exception if the operation fails
     */
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
		catch (IllegalArgumentException iExp)
		{
			writeErrorToConsole(iExp.getMessage());
			writer.close();
			return 0;
		}

		try
		{
			long startTime = System.currentTimeMillis();

			writeLineToConsole();
			writeMessageToConsole("                Querying for Document Objects...");
			writeMessageToConsole("                Writing files to: " + documentDirectory);
			writeLineToConsole();

			mxUtil.startTransaction(context);
			getIds(context);

			writeLineToConsole();
			writeMessageToConsole("                Querying for Family Objects  COMPLETE");
			writeMessageToConsole("                Time:"+ (System.currentTimeMillis() - startTime) + "ms ");
			writeMessageToConsole("                Step 1 of Migration         :  SUCCESS");
			writeLineToConsole();
			context.commit();
		}
		catch (FileNotFoundException fEx)
		{
			writeErrorToConsole("Directory does not exist or does not have access to the directory");
			context.abort();
		}
		catch (Exception ex)
		{
			writeErrorToConsole("Find Family objects Query failed - " + ex.getMessage());
			ex.printStackTrace();
			context.abort();
		}

		closeIDsFile();
		writer.flush();
		writer.close();
        return 0;
    }

	private void validateInputArguments(String[] args) throws IllegalArgumentException
	{
		if (args.length < 2 )
			throw new IllegalArgumentException("Wrong number of arguments");

		documentDirectory = args[0];
		gcoNames = args[1];
		// documentDirectory does not ends with "/" add it
		String fileSeparator = java.io.File.separator;
		if(documentDirectory != null && !documentDirectory.endsWith(fileSeparator))
		{
			documentDirectory = documentDirectory + fileSeparator;
		}
	}

    /**
     * Evalutes a temp query to get all the Family objects in the system
     * @param context the eMatrix <code>Context</code> object
     * @param chunksize has the no. of objects to be stored in file.
     * @return void
     * @exception Exception if the operation fails.
     */
    private void getIds(Context context) throws Exception
    {
		String familyBusTypeList = getFamilyTypeNames(context);
		if(familyBusTypeList.equals(""))
		{
			writeErrorToConsole("Can not find family type mapping is given GCOs");
			return;
		}
		writeMessageToConsole("Searching types: " + familyBusTypeList);
		String attrSource 	= MCADMxUtil.getActualNameForAEFData(context, "attribute_Source");
		String relVersionOf = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		
        String command = "temp query bus '" + familyBusTypeList + "' '*' '*' select attribute["
        				 + attrSource +"] revisions.id revisions.to["+ relVersionOf +"].from.id dump |";

		MQLCommand mqlCommand = new MQLCommand();
		mqlCommand.executeCommand(context, command);
		String result = mqlCommand.getResult();

		StringTokenizer token = new StringTokenizer(result, "\n");
		if(token.hasMoreElements())
			createNewIDsFile();

		while(token.hasMoreElements())
		{
			String objLine = (String)token.nextElement();
			writeToIDsFile(objLine);
		}

		closeIDsFile();
    }

	private String getFamilyTypeNames(Context context) throws Exception
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

				Vector familyClassList = getValueFromMapping(typeClassMapping, "TYPE_FAMILY_LIKE");
				for(int i=0; i<familyClassList.size(); i++)
				{
					String cadType = (String) familyClassList.elementAt(i);
					Vector mappedTypes = getValueFromMapping(typeFormatMapping, cadType);
					for(int j=0; j<mappedTypes.size(); j+=2)
					{
						String mxType = (String) mappedTypes.elementAt(j);
						typeList += mxType + ",";
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
		mqlCommand.executeCommand(context, "temp query bus $1 $2 $3 select $4 dump $5", "MCADInteg-GlobalConfig",gcoName, "1", "id", "|");
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

	private void closeIDsFile() throws Exception
	{
		if(iefIDsFile != null)
		{
			iefIDsFile.flush();
			iefIDsFile.close();
			iefIDsFile = null;
		}
    }

	private void createNewIDsFile() throws Exception
	{
		String fileNameWithPath = documentDirectory + "IEF_MigrationIDs_Family.txt";
		iefIDsFile				= new FileWriter(fileNameWithPath);
		writer.write("Created file " + fileNameWithPath + "\n");
	}

	private void writeToIDsFile(String id) throws Exception
	{
		try
		{
			iefIDsFile.write(id + "\n");
		}
		catch(Exception e)
		{
			writer.write("ERROR in writeing ID " + id + "   -   " + e.getMessage() + "\n");
		}
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
		writeMessageToConsole("Step 1 of Migration     : FAILED");
		writeLineToConsole();
		writer.flush();
	}
}

