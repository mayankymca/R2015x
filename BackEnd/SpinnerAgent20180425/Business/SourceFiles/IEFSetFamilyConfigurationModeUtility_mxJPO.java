/*
 * IEFSetFamilyConfigurationModeUtility.java program to set IEF-ObjectBasedConfigurationRevisionModeon all input major family ids.
 *
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
 *
 The utility script should be run to correct the existing Family Tables if they have different value of IEF-ObjectBasedConfigurationRevisionMode across Revisions or versions (mixed mode data). 
The script will take 2 arguments as an input when run through MQL. The 2 arguments should be as listed below:

1.	Input major bus id/path of text file containing list of major bus ids:
The first argument can be the major id of the family of any Revision OR the path of the text file which contains the list of major ids of different families. The extension of the file should be ".txt".
2.	Mode to set:
The second argument will be the IEF-ObjectBasedConfigurationRevisionMode which needs to be set on all input object ids. 

e.g. 
exec prog IEFSetFamilyConfigurationModeUtility 39184.13709.60768.40155 together;
OR
exec prog IEFSetFamilyConfigurationModeUtility E:\MajorIds.txt individual;

 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Date;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.io.IOException;
import java.util.Set;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.Environment;
import matrix.db.MatrixWriter;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;

public class IEFSetFamilyConfigurationModeUtility_mxJPO
{
    private BufferedWriter writer = null;
	private FileWriter logFile	  = null;
	private MCADMxUtil mxUtil	  = null;
	private String inputBusIds	  = "";
    private String inputMode	  = "";

    /**
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @grade 0
     */
    public IEFSetFamilyConfigurationModeUtility_mxJPO (Context context, String[] args)
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
		try
		{
			validateInputArguments(args);

			String filePathDir = Environment.getValue(context, "MX_TRACE_FILE_PATH");
    		File logDir = new File(filePathDir);
			if(!logDir.exists())
        		logDir.mkdir();

			String logsDirectoryName = logDir.getAbsolutePath();
			File fLogFile				= new File(logsDirectoryName, (new Date()).getTime() + "_IEFSetFamilyMode.log");
			writeMessageToConsole("Log File is created ...."+ fLogFile.getAbsolutePath());
			logFile				= new FileWriter(fLogFile.getAbsolutePath());
		}
		catch (IllegalArgumentException iExp)
		{
			writeErrorToConsole(iExp.getMessage());
			writer.close();
			return 0;
		}

		try
		{
			writeLineToConsole();
			writeMessageToConsole("Reading input BusinessObject Ids....");
			writeLineToConsole();

			mxUtil.startTransaction(context);

			Set idsList = new HashSet();
			if(inputBusIds.contains(".txt"))
				idsList = readDataFromIDFile(inputBusIds);
			else 
				idsList.add(inputBusIds);

			if(idsList.size() > 0)
				getAndProcessAllFamilyRevisionVersionIds(context, idsList);

			writeLineToConsole();
			writeMessageToConsole("Setting Family Configuration Mode COMPLETE");
			writeLineToConsole();
			context.commit();
		}
		catch (Exception ex)
		{
			writeErrorToConsole("Failure - " + ex.getMessage());
			ex.printStackTrace();
			context.abort();
		}

		closeFile();
		writer.flush();
		writer.close();
        return 0;
    }

	private void closeFile() throws Exception
	{
		if(logFile != null)
		{
			logFile.flush();
			logFile.close();
			logFile = null;
		}
    }

	private void validateInputArguments(String[] args) throws IllegalArgumentException
	{
		if (args.length < 2 )
			throw new IllegalArgumentException("Wrong number of arguments");

		inputBusIds = args[0];
		inputMode   = args[1];
	}

	private Set readDataFromIDFile(String inputBusIds) throws IOException
    {
        Set idsList			= new HashSet();
		File idFile			= new File(inputBusIds);
        BufferedReader br	= null;

        try
        {
            String id 	= null;			
            br 			= new BufferedReader(new FileReader(idFile));
			
            while((id = br.readLine()) != null)
            {	
				if(!id.equals(""))
					idsList.add(id);
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            idsList = null;
        }
        finally
        {
            br.close();
        }
		
        return idsList;
    }

    /**
     * Evalutes a temp query to get all the Family objects in the system
     * @param context the eMatrix <code>Context</code> object
     * @param chunksize has the no. of objects to be stored in file.
     * @return void
     * @exception Exception if the operation fails.
     */
    private void getAndProcessAllFamilyRevisionVersionIds(Context context, Set idsList) throws Exception
    {
		String ATTR_REVISION_MODE				= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-ObjectBasedConfigurationRevisionMode");
		String RELATIONSHIP_ACTIVE_VERSION		= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
		String SELECT_ON_ACTIVE_MINOR			= "from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.";
		String SELECT_EXPR_MINOR_REVISIONS		= "revisions.from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.revisions";
		String SELECT_EXPR_MINOR_REVISIONS_ID	= "revisions.from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.revisions.id";
		String SELECT_ATTR_REVISION_MODE		= "attribute[" + ATTR_REVISION_MODE + "]";
		
		String []  oids = new String[idsList.size()];
		idsList.toArray(oids);
		
		StringList busSelects = new StringList();
		busSelects.add("revisions");
		busSelects.add("revisions." + SELECT_ATTR_REVISION_MODE);
		busSelects.add("revisions.id");
		busSelects.add(SELECT_EXPR_MINOR_REVISIONS);
		busSelects.add(SELECT_EXPR_MINOR_REVISIONS + "." + SELECT_ATTR_REVISION_MODE);
		busSelects.add(SELECT_EXPR_MINOR_REVISIONS_ID);
		
		BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, oids, busSelects);
		
		Set allFamilyIds		= new HashSet();
		Set familyIdsToProcess	= new HashSet();

		for(int i = 0; i < busWithSelectList.size(); i++)
		{
			BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(i);

			StringList majorFamilyRevisions = busWithSelect.getSelectDataList("revisions");
			if(null != majorFamilyRevisions)
			{
				for(int j = 0; j < majorFamilyRevisions.size(); j++)
				{
					String majorRevision 			 = (String) majorFamilyRevisions.elementAt(j);
					String majorRevisionId			 = busWithSelect.getSelectData("revisions[" + majorRevision + "].id");
					String majorRevisionMode		 = busWithSelect.getSelectData("revisions[" + majorRevision + "]." + SELECT_ATTR_REVISION_MODE);
					allFamilyIds.add(majorRevisionId);
					if(!majorRevisionMode.equals(inputMode))
						familyIdsToProcess.add(majorRevisionId);

					StringList minorFamilyRevisions 		 = busWithSelect.getSelectDataList("revisions[" + majorRevision + "]." + SELECT_ON_ACTIVE_MINOR + "revisions");
					if(null != minorFamilyRevisions)
					{
						for(int k = 0; k < minorFamilyRevisions.size(); k++)
						{
							String minorRevision 			 = (String) minorFamilyRevisions.elementAt(k);
							String minorRevisionId			 = busWithSelect.getSelectData("revisions[" + majorRevision + "]." + SELECT_ON_ACTIVE_MINOR + "revisions[" + minorRevision + "].id");
							String minorRevisionMode		 = busWithSelect.getSelectData("revisions[" + majorRevision + "]." + SELECT_ON_ACTIVE_MINOR + "revisions[" + minorRevision + "]." + SELECT_ATTR_REVISION_MODE);
							allFamilyIds.add(minorRevisionId);
							if(!minorRevisionMode.equals(inputMode))
								familyIdsToProcess.add(minorRevisionId);
						}
					}
				}
			}		
		}

		writeMessageToConsole("Collected all BusinessObject Ids...." + allFamilyIds);
		logFile.write("Collected all BusinessObject Ids...." + allFamilyIds);

		if(familyIdsToProcess.size() > 0)
		{
			writeMessageToConsole("Processing " + familyIdsToProcess.size() + " BusinessObject Ids...." + familyIdsToProcess);
			logFile.write("\nProcessing " + familyIdsToProcess.size() + " BusinessObject Ids...." + familyIdsToProcess);
			Iterator itr = familyIdsToProcess.iterator();
			while(itr.hasNext())
			{
				String id 			  = (String)itr.next();
				String Args[] = new String[3];
				Args[0] = id;
				Args[1] = ATTR_REVISION_MODE;
				Args[2] = inputMode;
				String result = mxUtil.executeMQL(context,"mod bus $1 $2 $3", Args);
			
				if(result.startsWith("false"))
				{
					MCADException.createException("Failed to update revision mode for object:" + id + " Error: " + result, null);
				}
			}
		}
		else
			writeMessageToConsole("BusinessObject Ids does not require to change the mode....");
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
		writeMessageToConsole("FAILURE");
		writeLineToConsole();
		writer.flush();
	}
}

