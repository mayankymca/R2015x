/*
**  DSC_GetBatchProcessorDetailsBase
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to store the user and queue information for batch processor.
*/

import java.util.Enumeration;
import java.util.Hashtable;

import matrix.db.Context;

public class DSC_GetBatchProcessorDetailsBase_mxJPO 
{
	protected final String SHADOW_USER_NAME		= "ShadowUserName";
	protected final String SHADOW_USER_PASSWORD	= "ShadowUserPassword";	
	protected final String DEFAULT_QUEUE		= "DefaultQueue";
	protected final String QUEUE_NAME			= "QueueName";
	protected final String SITE_DETAILS_TABLE	= "SiteDetailsTable";

	protected Hashtable defaultUserDetails		= null;
	protected Hashtable batchOperationDetails	= null;

	public DSC_GetBatchProcessorDetailsBase_mxJPO ()
    {		
		//*****************************************************************
		//User definition for batch processor		
		defaultUserDetails = new Hashtable();

		defaultUserDetails.put(SHADOW_USER_NAME, "Test Everything");
		defaultUserDetails.put(SHADOW_USER_PASSWORD, "");
		//*****************************************************************
				
		//*****************************************************************
		//Operation definition for batch processor
		batchOperationDetails = new Hashtable();
		
		//Rename operation
		Hashtable renameOperationDetails = new Hashtable(2);
		renameOperationDetails.put(DEFAULT_QUEUE, "RenameQueue");

		//Derived Output operation
		Hashtable derivedOutputOperationDetails = new Hashtable(2);
		derivedOutputOperationDetails.put(DEFAULT_QUEUE, "DerivedOutputQueue");

		Hashtable siteNameSiteDetailsTable = new Hashtable();

		/*To support individual batch processing for each site, please uncomment
		 the below code and populate the siteDetails table accordingly for each site.
		*/
		/*Hashtable site1Details = new Hashtable();
		site1Details.put(SHADOW_USER_NAME, "<SITE1_USER_NAME>");
		site1Details.put(SHADOW_USER_PASSWORD, "<SITE1_USER_PASSWORD>");
		site1Details.put(QUEUE_NAME, "<SITE1_QUEUE_NAME>");

		Hashtable site2Details = new Hashtable();
		site2Details.put(SHADOW_USER_NAME, "<SITE2_USER_NAME>");
		site2Details.put(SHADOW_USER_PASSWORD, "<SITE2_USER_PASSWORD>");
		site2Details.put(QUEUE_NAME, "<SITE2_QUEUE_NAME>");

		siteNameSiteDetailsTable.put("<SITE1>", site1Details);
		siteNameSiteDetailsTable.put("<SITE2>", site2Details);*/

		renameOperationDetails.put(SITE_DETAILS_TABLE, siteNameSiteDetailsTable);
		batchOperationDetails.put("BatchRename", renameOperationDetails);

		derivedOutputOperationDetails.put(SITE_DETAILS_TABLE, siteNameSiteDetailsTable);		
		batchOperationDetails.put("BackgroundDerivedOutput", derivedOutputOperationDetails);

		/*
		//Other operation
		Hashtable otherOperationDetails = new Hashtable();
		otherOperationDetails.put(QUEUE_NAME, "OtherQueue");
		batchOperationDetails.put("BatchOther", otherOperationDetails);
		*/
		//*****************************************************************
    }
    
	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	public String getDefaultBatchOperationUserName(Context context, String []args) throws Exception
	{						
		return (String)defaultUserDetails.get(SHADOW_USER_NAME);
	}

	public String getDefaultBatchOperationUserPassword(Context context, String []args) throws Exception
	{				
		return (String)defaultUserDetails.get(SHADOW_USER_PASSWORD);
	}

	public String getDefaultSiteBatchOperationQueueName(Context context, String []args) throws Exception
	{	
		String operationName	= args[0];
		String queueName		= "";
		
		Enumeration batchOperationList	= batchOperationDetails.keys();

		while(batchOperationList.hasMoreElements())
		{			
			String batchOperationName = (String)batchOperationList.nextElement();
			if(batchOperationName.equalsIgnoreCase(operationName))
			{
				Hashtable batchOperationTable	= (Hashtable)batchOperationDetails.get(batchOperationName);
				queueName						= (String)batchOperationTable.get(DEFAULT_QUEUE);
				break;
			}			
		}

		return queueName;
	}

	public String getBatchOperationQueueNameForSite(Context context, String[] args) throws Exception
	{				
		String operationName	= args[0];
		String siteName			= args[1];
		String queueName		= "";
		
		Enumeration batchOperationList	= batchOperationDetails.keys();

		while(batchOperationList.hasMoreElements())
		{			
			String batchOperationName = (String)batchOperationList.nextElement();
			if(batchOperationName.equalsIgnoreCase(operationName))
			{
				Hashtable batchOperationTable		= (Hashtable)batchOperationDetails.get(batchOperationName);
				Hashtable siteNameSiteDetailsTable	= (Hashtable)batchOperationTable.get(SITE_DETAILS_TABLE);
				Hashtable particularSiteDetails		= (Hashtable)siteNameSiteDetailsTable.get(siteName);

				queueName = (String)particularSiteDetails.get(QUEUE_NAME);
				break;
			}			
		}

		return queueName;
	}

	public String getBatchOperationUserNameForSite(Context context, String[] args) throws Exception
	{				
		String operationName	= args[0];
		String siteName			= args[1];
		String userName			= "";
		
		Enumeration batchOperationList	= batchOperationDetails.keys();

		while(batchOperationList.hasMoreElements())
		{			
			String batchOperationName = (String)batchOperationList.nextElement();
			if(batchOperationName.equalsIgnoreCase(operationName))
			{
				Hashtable batchOperationTable		= (Hashtable)batchOperationDetails.get(batchOperationName);
				Hashtable siteNameSiteDetailsTable	= (Hashtable)batchOperationTable.get(SITE_DETAILS_TABLE);
				Hashtable particularSiteDetails		= (Hashtable)siteNameSiteDetailsTable.get(siteName);

				userName = (String)particularSiteDetails.get(SHADOW_USER_NAME);
				break;
			}			
		}

		return userName;
	}

	public String getBatchOperationUserPasswordForSite(Context context, String[] args) throws Exception
	{				
		String operationName	= args[0];
		String siteName			= args[1];
		String userPassword		= "";
		
		Enumeration batchOperationList	= batchOperationDetails.keys();

		while(batchOperationList.hasMoreElements())
		{			
			String batchOperationName = (String)batchOperationList.nextElement();
			if(batchOperationName.equalsIgnoreCase(operationName))
			{
				Hashtable batchOperationTable		= (Hashtable)batchOperationDetails.get(batchOperationName);
				Hashtable siteNameSiteDetailsTable	= (Hashtable)batchOperationTable.get(SITE_DETAILS_TABLE);
				Hashtable particularSiteDetails		= (Hashtable)siteNameSiteDetailsTable.get(siteName);

				userPassword = (String)particularSiteDetails.get(SHADOW_USER_PASSWORD);
				break;
			}			
		}

		return userPassword;
	}
}
