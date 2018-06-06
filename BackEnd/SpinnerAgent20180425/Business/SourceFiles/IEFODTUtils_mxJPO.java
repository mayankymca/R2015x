/*
 **  IEFODTUtils.java
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **	
 */
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADUtil;

import matrix.db.Context;
import matrix.db.JPO;


public class IEFODTUtils_mxJPO
{	

	/**
	 * Constructor.
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args holds no arguments
	 * @throws Exception if the operation fails
	 * @since Sourcing V6R2008-2
	 */
	public IEFODTUtils_mxJPO(Context context, String[] args) throws Exception
	{

	}	

	public String handleMQLWithFiles(Context context, String[] args) throws Exception
	{
		boolean isWorkspaceCreated = false;
		
		String response = null;
		
		MCADServerResourceBundle serverResourceBundle = new MCADServerResourceBundle("en");
		
		try
		{
			String filePath = context.createWorkspace();
			
			isWorkspaceCreated = true;
			
			HashMap argumentsMap = (HashMap)JPO.unpackArgs(args);
	
			String mqlCommand = (String)argumentsMap.get("mqlCommand");
			Hashtable filemap   = (Hashtable)argumentsMap.get("FileContents");
			Boolean isClientOSIsWindows = (Boolean)argumentsMap.get("ClientOSIsWindows");
	
			MCADMxUtil mxUtil = new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());
			mxUtil.clearClientTasksList(context);
	
			//Take decision for  response
			boolean fileCreateResult = createFiles(filePath, filemap);
	
			if(fileCreateResult)
			{
				mqlCommand = replaceFileMacro(mqlCommand, filePath, isClientOSIsWindows.booleanValue());
				
				System.out.println("[IEFODTUtils.handleMQLWithFiles] mqlCommand to be executed " + mqlCommand); 
	
				String messageString = mxUtil.executeMQL(context, mqlCommand);
	
				System.out.println("[IEFODTUtils.handleMQLWithFiles] Result of MQL messageString = " + messageString);
			}
			else
				MCADException.createException("Error occured while creating dump files",new Exception());
		}
		finally
		{
			if(isWorkspaceCreated)
				context.deleteWorkspace();
		}
		
		response = "success";

		return response;
	}

	private String replaceFileMacro(String mqlCommand, String filePath, boolean isClientOSIsWindows)
	{
		System.out.println("IEFODTUtils.replaceFileMacro() filePath " + filePath);
		StringBuffer sb = new StringBuffer("");
		int startIndex                = 0;
		int length                    = mqlCommand.length();

		while(startIndex < length )
		{
			if(mqlCommand.indexOf("<file-macro>",startIndex) > -1)
			{
				int macroStart = mqlCommand.indexOf("<file-macro>",startIndex);
				int macroEnd = mqlCommand.indexOf("</file-macro>",macroStart);


				sb.append(mqlCommand.substring(startIndex,macroStart));
				String fileName = mqlCommand.substring(macroStart+12,macroEnd);
				String fullpath = filePath + java.io.File.separator + fileName;
				
				fullpath = MCADUtil.replaceString(fullpath, java.io.File.separator, "/");
				
				sb.append(fullpath);

				startIndex = macroEnd + 13;
			}
			else
				break;

		}

		//Remaining part + no occurence found as it is copy.
		sb.append(mqlCommand.substring(startIndex));

		System.out.println("IEFODTUtils.replaceFileMacro() return value " + sb.toString());
		return sb.toString();
	}

	private boolean createFiles(String path ,Hashtable filemap) throws MCADException
	{
		boolean result = true;
		//use buffering
		Writer output = null;

		System.out.println("[IEFODTUtils.createFiles] path = " + path);
		
		Enumeration fileKeys =  filemap.keys();
		try
		{
			while(fileKeys.hasMoreElements())
			{
				String fileName     = (String)fileKeys.nextElement(); 
				String fileContent  = (String)filemap.get(fileName);

				java.io.File file = new java.io.File(path +java.io.File.separator + fileName);

				if(!file.exists())
					file.createNewFile();

				output = new BufferedWriter(new FileWriter(file));
				try
				{
					output.write( fileContent );
				}
				finally 
				{
					try
					{
						output.close();
					}
					catch (IOException e)
					{
						result = false;
						MCADException.createException(e.getMessage(),e);
					}
				}
			}
		}
		catch (IOException e)
		{
			result = false;
			MCADException.createException(e.getMessage(),e);
		} 

		return result;
	}

}

