/*
 **  IEFUserAccountsUtil
 **
 **  Copyright Dassault Systemes, 1992-2010.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to compute user account usage for PDS.
 */
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import matrix.db.*;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.IEFUserAccountsHelper;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.util.BackgroundProcess;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.MCADIntegration.notification.IEFNotificationController;

public class IEFUserAccountsUtil_mxJPO
{
	protected MCADServerResourceBundle _serverResourceBundle = null;
	protected MCADMxUtil _util						         = null;
	protected IEFGlobalCache _cache							 = null;

	protected String SELECT_ALL_FILE_SIZE					 = "format.file.size";

	private static final String TRANS_HISTORY_HISTORY_STRAT_DELIMITER 	 = "history=";
	private static final String TRANS_HISTORY_OBJECT_DELIMITER 			 = "id=";
	private static final String TRANS_HISTORY_EVENT_DELIMITER 			 = "- user:";
	private static final String TRANS_HISTORY_TYPE_DELIMITER 			 = "type=";

	public IEFUserAccountsUtil_mxJPO() throws Exception
	{
	}

	public IEFUserAccountsUtil_mxJPO(Context context, String[] args) throws Exception
	{
		String language		  = context.getLocale().getLanguage();

		_serverResourceBundle = new MCADServerResourceBundle(language);
		_cache				  = new IEFGlobalCache();
		_util				  = new MCADMxUtil(context, _serverResourceBundle, _cache);
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	//NOTE : This will not be used for delete
	public int setUserSpaceUsageBasedOnTransactionHistoryInBackground(Context context, String[] args) throws Exception
	{
		int result = 0;

		String transHistories = args[0];

		if(transHistories != null && !"".equals(transHistories))
		{
			try
			{
				Context frameContext = context.getFrameContext("IEFUserAccountsUtil");
				BackgroundProcess backgroundProcess = new BackgroundProcess();
				backgroundProcess.submitJob(frameContext, "IEFUserAccountsUtil", "setUserSpaceUsageBasedOnTransactionHistory", args , (String)null);
			}
			catch(Exception ex)
			{
				ContextUtil.abortTransaction(context);
				throw ex;
			}
		}

		return result;
	}

	public int setUserSpaceUsageBasedOnTransactionHistory(Context context, String[] args) throws Exception
	{
		int result = 0;

		String transHistories = args[0];

		if(transHistories != null && !"".equals(transHistories))
		{
			boolean isUserAccountSpaceUpdateRequired = isUserAccountSpaceUpdateRequired(context, transHistories);
			if(isUserAccountSpaceUpdateRequired)
			{
				String prviousDiskUsage = getDiskspaceUsageForPerson(context);
				setUserSpaceUsage(context, args);
				
				IEFNotificationController notificationManager = new IEFNotificationController(context);
				notificationManager.sendDiskspaceWarningNotifications(context,prviousDiskUsage);
			}
		}

		return result;
	}

	public int setUserSpaceUsageInBackground(Context context, String[] args) throws Exception
	{
		int result = 0;

		try
		{
			Context frameContext 				= context.getFrameContext("IEFUserAccountsUtil");
			BackgroundProcess backgroundProcess = new BackgroundProcess();

			backgroundProcess.submitJob(frameContext, "IEFUserAccountsUtil", "setUserSpaceUsage", args, (String)null);
		}
		catch(Exception ex)
		{
			ContextUtil.abortTransaction(context);
			throw ex;
		}

		return result;
	}

	private String getTypesForQuery(Context context) throws Exception
	{
		StringBuffer resultBuffer = new StringBuffer();

		IEFSimpleConfigObject simpleLCO = IEFSimpleConfigObject.getSimpleLCO(context);
		Hashtable integNameGCOMapping	= simpleLCO.getAttributeAsHashtable(MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping"), "\n", "|");

		HashSet integrationTypes = new HashSet();
		
		Enumeration integrationNames = integNameGCOMapping.keys();
		while (integrationNames.hasMoreElements())
		{
			String integrationName   = (String) integrationNames.nextElement();
			
			IEFSimpleConfigObject simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, integrationName);
			
			String busTypeAttrb		 	= MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-BusTypeMapping");
			Hashtable busTypeAttrbTable = simpleGCO.getAttributeAsHashtable(busTypeAttrb, "\n", "|");
			
			Enumeration mappedCADTypes = busTypeAttrbTable.keys();
			while(mappedCADTypes.hasMoreElements())
			{
				String mappedCADType = (String) mappedCADTypes.nextElement();
				
				String mappedTypesString = (String) busTypeAttrbTable.get(mappedCADType);
				
				Vector mappedTypes = MCADUtil.getVectorFromString(mappedTypesString, ",");
				
				integrationTypes.addAll(mappedTypes);
			}
		}
		
		String typeIEFCommentsHolder = MCADMxUtil.getActualNameForAEFData(context, "type_IEFCommentsHolder");

		resultBuffer.append(typeIEFCommentsHolder).append(",");
		
		resultBuffer.append(MCADUtil.getStringFromCollection(integrationTypes, ","));
		
		//add the minor types
		Iterator integrationMajorTypes = integrationTypes.iterator();
		while (integrationMajorTypes.hasNext())
		{
			String majorType = (String) integrationMajorTypes.next();
			
			String minorType = this._util.getCorrespondingType(context, majorType);
			
			resultBuffer.append(",").append(minorType);
		}

		return resultBuffer.toString();
	}

	public Boolean setUserSpaceUsage(Context context, String[] args) throws Exception
	{
		boolean resultValue = false;

		double totalfilesSize = 0;

		String userName = context.getUser();

		try
		{
			_util.startReadOnlyTransaction(context);
			
			String typesForQuery = getTypesForQuery(context);

			Query query = new Query();
			query.setBusinessObjectType(typesForQuery);
			query.setOwnerPattern(userName);

			StringList busSelects = new StringList();
			busSelects.add("id");
			busSelects.add(SELECT_ALL_FILE_SIZE);

			QueryIterator queryIterator = null;

			try
			{
				queryIterator = query.getIterator(context, busSelects, (short)100);

				while(queryIterator.hasNext())
				{
					BusinessObjectWithSelect busWithSelect = queryIterator.next();

					StringList fileSizeList 		= busWithSelect.getSelectDataList(SELECT_ALL_FILE_SIZE);

					if(fileSizeList != null && fileSizeList.contains(""))
						MCADUtil.removeAllOccurenceFromCollection(fileSizeList, "");

					if(fileSizeList != null && !fileSizeList.isEmpty())
					{
						for (int j = 0; j < fileSizeList.size(); j++)
						{
							String fileSize = (String)fileSizeList.elementAt(j);
							if(fileSize != null && fileSize.length() >  0)
							{
								totalfilesSize = totalfilesSize + (Double.parseDouble(fileSize)/(1024 * 1024));
							}
						}					
					}
				}

			}
			finally 
			{
				if(queryIterator != null)
					queryIterator.close();
			}

			_util.commitTransaction(context);
		}
		catch (Exception e)
		{
			_util.abortReadOnlyTransaction(context);
		}

		long spaceUsage = Math.round(totalfilesSize);

		setSpaceUsageOnPersonObject(context, spaceUsage);

		resultValue = true;

		return new Boolean(resultValue);
	}


	private boolean isUserAccountSpaceUpdateRequired(Context context, String transHistories) throws Exception
	{
		try
		{
			boolean isUpdateRequired = false;

			int idIndex 			 = 0;

			do
			{
				//splitting the transhistory
				idIndex = transHistories.indexOf(TRANS_HISTORY_OBJECT_DELIMITER);

				if(idIndex != -1)
				{
					//String busid   = transHistories.substring((idIndex + 3), transHistories.indexOf("\n"));

					//String integrationName = this._util.getIntegrationName(context, busid);

					int itypeIndex = transHistories.indexOf(TRANS_HISTORY_TYPE_DELIMITER);

					if(itypeIndex != -1)
						transHistories = transHistories.substring(itypeIndex + 5);

					int ihistoryIndex = transHistories.indexOf(TRANS_HISTORY_HISTORY_STRAT_DELIMITER);

					//Since we had splitted the oid and type, modify the
					//transHistories to contains the rest of the content from 'history=' keyword
					transHistories = transHistories.substring(ihistoryIndex + 8);
					//While finding the id= keyword from second time onwards searching with \nkeyword
					idIndex = transHistories.indexOf("\n"+TRANS_HISTORY_OBJECT_DELIMITER);
					String strEventsHistory = "";

					/*
					 * if the transhistory contains more id then get the list of history events for the first object
					 * and store it in strEventsHistory variable and the rest of the content in transHistories.
					 * if no more id is found then assign the transhistories to strEventsHistory variable directly
					 */
					if(idIndex != -1)
					{
						strEventsHistory = transHistories.substring(0,idIndex);
						transHistories = transHistories.substring(idIndex);
					}
					else
						strEventsHistory = transHistories;
					//Invoke the method to parse history Events of every object using the '- user:'
					isUpdateRequired = doesEventsRequireUserSpaceUpdate(context, strEventsHistory);

					if(isUpdateRequired)
						break;
				}
			}
			while(idIndex != -1);

			return isUpdateRequired;
		}
		catch(Exception ex)
		{
			throw ex;
		}
	}

	private boolean doesEventsRequireUserSpaceUpdate(Context context, String strEventsHistory) throws Exception
	{
		boolean doesEventsRequireUserSpaceUpdate = false;

		int iEventIndex = strEventsHistory.indexOf(TRANS_HISTORY_EVENT_DELIMITER);
		while(iEventIndex != -1)
		{
			String strTemp = strEventsHistory.substring(0,iEventIndex).trim();
			/*
			 * We need to take last index of \n since while spliting with '- user:' word we will get the value
			 * something like the below one if the description contains more than one lines.
			 * Eg:
			 *   Test Everything  time: 07/01/2008 04:36:51 PM  state: Preliminary  description: asdfwe
			 *   asdfwasdfaw
			 *   asdfsadfwesad
			 *   sdafawwefasdf
			 *   modify - user: Test Everything  time: 7/1/2008 4:36:51 PM  state: Preliminary  Design Purchase: Design  was:
			 * Here we have taken care of taking the event name from the last index of \n.
			 */
			int iStartEvent  = strTemp.lastIndexOf("\n");
			String strEvent  = strEventsHistory.substring(iStartEvent + 1, iEventIndex).trim();
			strEventsHistory = strEventsHistory.substring(iEventIndex + 7);

			if(strEvent.equals("checkin") || strEvent.equals("change owner") || strEvent.equals("delete file"))
			{
				doesEventsRequireUserSpaceUpdate = true;
				break;
			}

			iEventIndex = strEventsHistory.indexOf(TRANS_HISTORY_EVENT_DELIMITER);
		}

		return doesEventsRequireUserSpaceUpdate;
	}

	private void setSpaceUsageOnPersonObject(Context context, long spaceUsage) throws MCADException
	{
		String userName = context.getUser();
		//check if property already exists.

		String Args[] = new String[2];
		Args[0] = userName;
		Args[1] = "property[" + IEFUserAccountsHelper.IEF_CURRENT_DISK_SPACE_USED_PERSON_PROP + "].name";
		String result = _util.executeMQL(context, "print person $1 select $2 dump", Args);
		if(result.startsWith("true|"))
		{
			String propName = result.substring(5);
			if(propName.equals(""))
			{
				addSpaceUsageForPersonObject(context, spaceUsage);
			}
			else
			{
				modifySpaceUsageForPersonObject(context, spaceUsage);
			}
		}
		else if(result.startsWith("false"))
		{
			MCADServerException.createException(result.substring(6), null);
		}
	}

	private void addSpaceUsageForPersonObject(Context context,  long spaceUsage) throws MCADException
	{
		String userName = context.getUser();

		String mqlCmd = "modify person \"" + userName + "\" add property \"" + IEFUserAccountsHelper.IEF_CURRENT_DISK_SPACE_USED_PERSON_PROP + "\" value \"" + spaceUsage + "\"";

		String Args[] = new String[6];
		Args[0] = userName;
		Args[1] = "add";
		Args[2] = "property";
		Args[3] = IEFUserAccountsHelper.IEF_CURRENT_DISK_SPACE_USED_PERSON_PROP;
		Args[4] = "value";
		Args[5] = Long.toString(spaceUsage);
		String result = _util.executeMQLAsShadowAgent(context, "modify person $1 $2 $3 $4 $5 $6", Args);
		if(result.startsWith("false|"))
		{
			MCADServerException.createException(result.substring(6), null);
		}
	}

	private void modifySpaceUsageForPersonObject(Context context, long spaceUsage) throws MCADException
	{
		String userName = context.getUser();

		//get the property value
		String Args[] = new String[2];
		Args[0] = userName;
		Args[1] = "property[" + IEFUserAccountsHelper.IEF_CURRENT_DISK_SPACE_USED_PERSON_PROP + "].value";
		
		String userDiskspaceUsed = _util.executeMQL(context, "print person $1 select $2 dump", Args);

		if(userDiskspaceUsed.startsWith("false|"))
		{
			MCADServerException.createException(userDiskspaceUsed.substring(6), null);
		}

		Args = new String[5];
		Args[0] = userName;
		Args[1] = "property";
		Args[2] = IEFUserAccountsHelper.IEF_CURRENT_DISK_SPACE_USED_PERSON_PROP;
		Args[3] = "value";
		Args[4] = Long.toString(spaceUsage);
		String result = _util.executeMQLAsShadowAgent(context, "modify person $1 $2 $3 $4 $5", Args);
		if(result.startsWith("false|"))
		{
			MCADServerException.createException(result.substring(6), null);
		}
	}

	private String getDiskspaceUsageForPerson(Context context) throws MCADException
	{
		String userName = context.getUser();

		//get the property value
		String Args[] = new String[2];
		Args[0] = userName;
		Args[1] = "property[" + IEFUserAccountsHelper.IEF_CURRENT_DISK_SPACE_USED_PERSON_PROP + "].value";

		String userDiskspaceUsed = _util.executeMQL(context, "print person $1 select $2 dump", Args);
		
		if(userDiskspaceUsed.startsWith("true|"))
		{
			userDiskspaceUsed = userDiskspaceUsed.substring(5);
		}
		else
		{
			MCADServerException.createException(userDiskspaceUsed.substring(6), null);
		}
		return userDiskspaceUsed;
		
	}
}

