/*
 *  msoiMSFTasks.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 * @quickreview 16:05:27 AMA3 IR-419558-3DEXPERIENCER2017x Outlook Task: commands incorrectly displayed, added constant reference
 */

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;
import com.matrixone.apps.common.InboxTask;
import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.eMatrixDateFormat;

/**
 * @version AEF Rossini - Copyright (c) 2002, MatrixOne, Inc.
 */
public class msoiMSFTasks_mxJPO extends emxDomainObject_mxJPO {
	private String[] initArgs = null;
	
	/**
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public msoiMSFTasks_mxJPO (Context context, String[] args)
        throws Exception
    {
      super(context, args);
	  initArgs = args;
    }

    public int mxMain(Context context, String[] args)
        throws Exception
    {
        if (!context.isConnected())
            throw new Exception(ComponentsUtil.i18nStringNow("emxComponents.Generic.NotSupportedOnDesktopClient", context.getLocale().getLanguage()));
        return 0;
    }
	
    //SJ7+ Gets all Active tasks for the user. This includes the route tasks as well as WBS tasks
	public MapList getActiveTasks(Context context, String[] args)
	{
		MapList taskList = new MapList();
		try{
			HashMap initMap = (HashMap)JPO.unpackArgs(args);			
			String[] methodArgs = (String[])initMap.get("args");
			String[] passingArgs = new String[]{methodArgs[0], methodArgs[4], methodArgs[5]};
			
			taskList.addAll(invokeJPO(context, passingArgs, "PRGSupportBase", "getTaskRelatedInfo", "MSFAPPTaskSummary"));
			taskList.addAll(getRouteTasks(context, args));
			return taskList;
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return null;
	}
	
	//SJ7+ Gets all PRG tasks for the user
	public MapList getTaskRelatedInfo(Context context, String[] args) throws FrameworkException
	{		
		try{
			return invokeJPO(context, args, "PRGSupportBase", "getTaskRelatedInfo", "MSFAPPTaskSummary");			
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	//SJ7+ Function to update the tasks and return the result of update as success or failed.
	public String updateTask(Context context, String[] args) throws Exception
	{
		try{
			// Route Task
			if (args[3].equals("Comment") || args[3].equals("Approve") || args[3].equals("Notify Only")) {
				
				HashMap result = new HashMap();
				
				try {
					
					HashMap<String, HashMap<String, String>> programMap = new HashMap<String, HashMap<String, String>>();
					HashMap<String, String> requestMap = new HashMap<String, String>();
					
					programMap.put("requestMap", requestMap);
					requestMap.put("languageStr", null);
					requestMap.put("localeObj", null);
					requestMap.put("timeZone", null);
					requestMap.put("objectId", args[1]);
					requestMap.put("taskId", args[1]);
					requestMap.put("Comments", args[4]);
					requestMap.put("ReviewerComments", null);
					requestMap.put("DueDate", null);
					requestMap.put("routeTime", null);
					requestMap.put("approvalStatus", "Approve");
					requestMap.put("mode", "edit");
	
					result = (HashMap)JPO.invoke(context, "emxInboxTask", null, "updateTaskDetails", JPO.packArgs(programMap), HashMap.class);
				
					InboxTask inboxTaskObj = (InboxTask)DomainObject.newInstance(context,DomainConstants.TYPE_INBOX_TASK);
			        inboxTaskObj.setId(args[1]);
			        inboxTaskObj.open(context);
			        
			        inboxTaskObj.promote(context);
			        
			        inboxTaskObj.update(context);
			        inboxTaskObj.close(context);
			        
				} catch (Exception e) {
					result.put(e.getClass(), e.getMessage());
				}
			        
				return result.size() == 0 ? "success" : result.toString();
				
			// WBS Task
			} else if (args[3].equals(TYPE_TASK)) {
				return (String)JPO.invoke(context, "PRGSupportBase", null, "updateTaskPercentComplete", new String[]{args[1], args[2]}, String.class);
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	//SJ7+ Gets all PRG Tasks after a Date
	public MapList getTaskRelatedInfoAfterDate(Context context, String[] args) throws Exception 
	{
		MapList taskList = new MapList();
		try {
			
			HashMap initMap = (HashMap)JPO.unpackArgs(args);	
			String[] methodArgs = (String[])initMap.get("args");
			String[] passingArgs = new String[]{methodArgs[0], methodArgs[4], methodArgs[5], methodArgs[7]};
							
			Boolean isPRGInstalled = Boolean.valueOf(methodArgs[8]);
			if(isPRGInstalled) {
				taskList.addAll(invokeJPO(context, passingArgs, "PRGSupportBase", "getTaskRelatedInfoAfterDate", "MSFAPPTaskSummary"));
			}
			
			taskList.addAll(getRouteTasksByDate(context, methodArgs));
			
			if(initArgs.length > 0)
			updateLCOValue(context, initArgs, "MSF-TasksLastSyncDateTime", methodArgs[7]);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		return taskList;
	}
	
	//SJ7+ Function to get Completed Tasks
	public MapList getCompletedTasks(Context context, String[] args) throws Exception
	{
		try {
			return invokeJPO(context, args, "emxInboxTask", "getCompletedTasks", "MSFAPPTaskSummary");						
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return null;
	}
	
	//Get All Tasks
	public MapList getAllTasks(Context context, String[] args) throws Exception
	{
		MapList allTasks = new MapList();
		try {
			allTasks.addAll(invokeJPO(context, args, "emxInboxTask", "getMyDeskTasks", "MSFAPPTaskSummary"));
			allTasks.addAll(invokeJPO(context, args, "emxInboxTask", "getActiveTasks", "MSFAPPTaskSummary"));
			allTasks.addAll(invokeJPO(context, args, "emxInboxTask", "getCompletedTasks", "MSFAPPTaskSummary"));
			allTasks.addAll(invokeJPO(context, args, "emxInboxTask", "getTasksToBeAccepted", "MSFAPPTaskSummary"));			
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return allTasks;
	}
	
	//SJ7+ Function to get all Active and Assigned Route Tasks
	public MapList getRouteTasks(Context context, String[] args) throws Exception
	{
		MapList returnList = new MapList();
		try {
			MapList routeTaskList = invokeJPO(context, args, "emxInboxTask", "getAllTasks", "MSFAPPTaskSummary");			
			Map taskMap = null;
			String taskType = "";
			
			for(Iterator iter = routeTaskList.iterator(); iter.hasNext();) {
				taskMap = (Map) iter.next();
				taskType = (String) taskMap.get("type");
				
				if(taskType.contains("Inbox")){
					returnList.add(taskMap);
				}
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return returnList;
	}
	
	public MapList getRouteTasksByDate(Context context, String[] args)
	{
		MapList returnList = new MapList();
		try {
			MapList routeTaskList = getRouteTasks(context, args);
			String clientDtFormat = args[5];
			String dateTime	= args[7];
			
			Locale clientLocale = context.getLocale();
			int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
			SimpleDateFormat clientDateFormat = new SimpleDateFormat(clientDtFormat,clientLocale);
			clientDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date datePassed = clientDateFormat.parse(dateTime);
			SimpleDateFormat format = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat());
			format.setTimeZone(TimeZone.getTimeZone("UTC"));
			
			Iterator iterator = routeTaskList.iterator();

			while (iterator.hasNext()) {
				Map taskInfo = (Map) iterator.next();
				String modifiedDate = (String) taskInfo.get(DomainConstants.SELECT_MODIFIED);
				if (null != modifiedDate)
				{
					Date dateModified = format.parse(modifiedDate);
					if (dateModified.compareTo(datePassed) >= 0) {
						returnList.add(taskInfo);
					}
				}
				else
					returnList.add(taskInfo);
			}			
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return returnList;
	}
	
	//SJ7+ Changes for Outlook Tasks Integration
	private MapList invokeJPO(Context context, String[] args, String programName, String functionName, String tableName) throws Exception
	{	
		MapList returnList = new MapList();
		
		try{			
			//Call the JPO program and its function			
			returnList = (MapList)JPO.invoke(context, programName, null, functionName, args, MapList.class);			
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw (new MatrixException(programName + ":" + functionName + "=>" + ex.toString()) );
		}
				
		return returnList;
	}
	
	public Vector showTaskName(Context context, String[] args)
	{
		try{
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList)programMap.get("objectList");
            DomainObject taskObject = new DomainObject();
            Vector vecShowTaskName  = new Vector();
			String taskName = "";
			Iterator objectListItr = objectList.iterator();
            
            while(objectListItr.hasNext()) {
                Map objectMap = (Map) objectListItr.next();                
                taskName  = (String)objectMap.get("name");
				vecShowTaskName.add(taskName);
			}
			
			return vecShowTaskName;
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	//SJ7+ Update the LCO
	private void updateLCOValue(Context context, String[] args, String lcoAttribute, String lcoAttributeValue) throws Exception
	{
		try{			
			MCADMxUtil util = null;
			Hashtable initArgsTable = (Hashtable)JPO.unpackArgs(args);			
			MCADLocalConfigObject localConfigObject = (MCADLocalConfigObject)initArgsTable.get("lco");			
			//TODO: language is coming as empty. check that
			String localeLanguage = args[0];
			IEFGlobalCache cache = new IEFGlobalCache();
			MCADServerResourceBundle serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
			util = new MCADMxUtil(context, serverResourceBundle, cache);
			
			util.setLCOAttribute(context, localConfigObject, lcoAttribute, lcoAttributeValue);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}
}
