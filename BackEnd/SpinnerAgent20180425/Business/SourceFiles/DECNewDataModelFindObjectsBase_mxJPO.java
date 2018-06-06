import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.util.MqlUtil;

public class DECNewDataModelFindObjectsBase_mxJPO 
{
	BufferedWriter consoleWriter		= null;
	BufferedWriter logWriter			= null;
	BufferedWriter objIdWriter			= null;
	PrintStream errorStream				= null;

	String logDirectory					= "";
	String pageObjName					= "";
	String gcoBusId						= "";
	String timeStamp					= "";
	static final String fileSeparator	= java.io.File.separator;
	int chunkSize						= 1000;
	MCADGlobalConfigObject 	gcoObject	= null;
	
	public DECNewDataModelFindObjectsBase_mxJPO (Context context, String[] args)throws Exception
	{
		timeStamp = getTimeStamp();
	}

	public int mxMain(Context context, String[] args) throws Exception
	{		
		if(!context.isConnected())
		{
			throw new Exception("not supported on desktop client");
		}

		long startTime = System.currentTimeMillis();

		initialize(context, args);

		writeMessageToConsole("====================================================================================");
		writeMessageToConsole("	Querying for "+pageObjName+" Objects...\n");
		writeMessageToConsole("			("+ chunkSize + ") Objects per File");
		writeMessageToConsole("			Writing files to: " + logDirectory );
		writeMessageToConsole("====================================================================================\n");

		try
		{
			getObjectIds(context);
		}
		catch(Exception exception)
		{
			closeLogStream();
			throw exception;
		}

		writeMessageToConsole("====================================================================================");
		writeMessageToConsole("	Querying for Objects COMPLETED");
		writeMessageToConsole("			Time:"+ (System.currentTimeMillis() - startTime) + "ms");
		writeMessageToConsole("			Objectid log files written to : " + DECNewDataModelMigration_mxJPO.inputDirectory);
		writeMessageToConsole("			Step 1 of Migration         :  SUCCESS ");
		writeMessageToConsole("====================================================================================\n");

		closeLogStream();

		return 0;
	}

	private String getObjectIds(Context context) throws Exception
	{
		String typesToSearch = getTypeListFromPage(context);
		
		writeMessageToConsole("\n\n\t[DECNewDataModelFindObjects:getObjectIds] Searching objects for "+ pageObjName + "........\n\n");

		String command 	= "temp query bus $1 $2 $3  vault $4 limit 1 where $5";
		String result 	= "";

		DECNewDataModelMigration_mxJPO._counter  		= 0;
		DECNewDataModelMigration_mxJPO._sequence  		= 1;
		DECNewDataModelMigration_mxJPO.pageObjectName	= pageObjName;
		DECNewDataModelMigration_mxJPO._oidsFile 		= null;
		DECNewDataModelMigration_mxJPO._fileWriter 		= null;
		DECNewDataModelMigration_mxJPO._objectidList 	= null;

		if (DECNewDataModelMigration_mxJPO._fileWriter == null)
		{
			try
			{
				DECNewDataModelMigration_mxJPO.inputDirectory = logDirectory + "Results" + fileSeparator;
				DECNewDataModelMigration_mxJPO._oidsFile = new java.io.File(logDirectory + "Results" + fileSeparator + pageObjName + "_Objectids_1.log");
				DECNewDataModelMigration_mxJPO._oidsFile.getParentFile().mkdirs();
				DECNewDataModelMigration_mxJPO._fileWriter = new BufferedWriter(new FileWriter(DECNewDataModelMigration_mxJPO._oidsFile));
				DECNewDataModelMigration_mxJPO._chunk = chunkSize;
				DECNewDataModelMigration_mxJPO._objectidList = new StringList(chunkSize);
			}
			catch(FileNotFoundException eee)
			{
				throw eee;
			}
		}

		try
		{
			String args[] 		= new String[5];
			args[0] 			= typesToSearch;
			args[1] 			= "*";
			args[2] 			= "*";
			args[3] 			= "*";
			args[4] 			= "program[DECNewDataModelMigration -method writeOID ${OBJECTID} \"${TYPE}\" \"${NAME}\" \"${REVISION}\"] == true";
			
			result  = MqlUtil.mqlCommand(context, command, args);
		}
		catch(Exception me)
		{
			writeMessageToConsole("[DECNewDataModelFindObjects:getObjectIds] Exception while fetching object id list : "+me.getMessage());
			writeErrorToFile("[DECNewDataModelFindObjects:getObjectIds] Exception while fetching object id list : "+me.getMessage());
			me.printStackTrace(errorStream);
			throw me;
		}

		// call cleanup to write the left over oids to a file
		DECNewDataModelMigration_mxJPO.cleanup();    
		
		return result;
	}

	private String getTypeListFromPage(Context context) throws Exception
	{
		HashSet<String> typeListSet = new HashSet<String>();
		String MQLResult 			= "";

		try 
		{
			// Get Types from Page object
			String args[] 		= new String[1];
			args[0] 			= pageObjName;
			
			MQLResult  			= MqlUtil.mqlCommand(context, "print page $1 select content dump", args);

			if (MQLResult == null || MQLResult.length() == 0)
			{
				writeMessageToConsole("[DECNewDataModelFindObjects:getTypeListFromPage] Page Object is blank. Please update Page Object with Type information");
				throw new Exception("Page Object is blank. Please update Page Object with Type information");
			}
		} 
		catch (Exception exception) 
		{
			writeMessageToConsole("[DECNewDataModelFindObjects:getTypeListFromPage] Failure in getting type list from Page obejct : "+exception.getMessage());
			exception.printStackTrace(errorStream);
			throw exception;
		}
		
		byte[] bytes 			= MQLResult.getBytes("UTF-8");
		InputStream input 		= new ByteArrayInputStream(bytes);

		Properties properties	= new Properties();

		properties.load(input);

		if(properties.keySet() != null)
		{
			Iterator keyTypeSymbolicNames  = properties.keySet().iterator();
			while(keyTypeSymbolicNames.hasNext())
			{
				String keyType = (String)keyTypeSymbolicNames.next();

				if(keyType.endsWith(".changeTo"))
				{
					keyType = MCADMxUtil.getActualNameForAEFData(context, keyType.substring(0, keyType.indexOf(".changeTo")));

					typeListSet.add(keyType);
				}
			}
		}

		typeListSet.addAll(gcoObject.getAllMappedTypes());

		StringBuffer typeList 	= new StringBuffer();
		Iterator iterator 		= typeListSet.iterator();
		
		while(iterator.hasNext())
		{
			typeList.append((String) iterator.next());
			
			if(iterator.hasNext())
				typeList.append(",");
		}
		
		
		return typeList.toString();
	}

	private void initialize(Context context, String[] args) throws Exception
	{
		try
		{
			consoleWriter = new BufferedWriter(new MatrixWriter(context));

			if (args.length != 3 && args.length != 4)
			{
				throw new IllegalArgumentException("Wrong number of arguments or arguments with wrong values!");
			}

			logDirectory = args[0];

			// documentDirectory does not ends with "/" add it
			if(logDirectory != null && !logDirectory.endsWith(fileSeparator))
			{
				logDirectory = logDirectory + fileSeparator;
			}

			logDirectory = logDirectory + timeStamp + fileSeparator;

			File debugLogFile = new File(logDirectory + "FindObjects_DebugLog.log");
			File errorLogFile = new File(logDirectory + "FindObjects_ErrorLog.log");

			// Create Directory Structure
			debugLogFile.getParentFile().mkdirs();

			logWriter 	= new BufferedWriter(new FileWriter(debugLogFile));
			errorStream = new PrintStream(new FileOutputStream(errorLogFile));

			pageObjName	= args[1];

			String gcoBusId	= args[2];

			// Get Types from GCO BusTypeMapping
			MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(null);
			MCADMxUtil util 					= new MCADMxUtil(context, new MCADServerResourceBundle(""), new IEFGlobalCache());
	
			gcoObject							= configLoader.createGlobalConfigObject(context, util, gcoBusId);
						
			if(args.length == 4)
			{
				chunkSize	= Integer.parseInt(args[3]);
				if (chunkSize == 0 || chunkSize < 1 )
				{
					throw new IllegalArgumentException();
				}
			}
		}
		catch (Exception iExp)
		{

			writeMessageToConsole("[DECNewDataModelFindObjects:initialize] Exception in initialization : "+iExp.getMessage());
			writeErrorToFile("[DECNewDataModelFindObjects:initialize] Exception in initialization : "+iExp.getMessage());

			iExp.printStackTrace(errorStream);

			closeLogStream();

			return;
		}
	}

	private String getTimeStamp()
	{
		Date date = new Date();
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd").format(date)+"T"+ new SimpleDateFormat("HH:mm:ss").format(date);

		timeStamp = timeStamp.replace('-', '_');
		timeStamp = timeStamp.replace(':', '_');

		return timeStamp;
	}

	private void closeLogStream() throws IOException
	{
		try 
		{
			if(null != consoleWriter)
				consoleWriter.close();
			
			if(null != logWriter)
				logWriter.close();
			
			if(null != errorStream)
				errorStream.close();
		} 
		catch (IOException e) 
		{
			System.out.println("Exception while closing log stream "+e.getMessage());
			e.printStackTrace();
		}
	}

	private void writeMessageToConsole(String message) throws Exception
	{
		consoleWriter.write(message + "\n");
		consoleWriter.flush();
		writeMessageToLogFile(message);
	}

	private void writeMessageToLogFile(String message) throws Exception
	{
		logWriter.write(MCADUtil.getCurrentTimeForLog() + message + "\n");
	}

	private void writeErrorToFile(String message) throws Exception
	{
		errorStream.write(message.getBytes("UTF-8"));
		errorStream.write("\n".getBytes("UTF-8"));
	}
}
