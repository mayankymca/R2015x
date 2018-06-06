/*
 *  msoiMSFInboxTaskBase.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 * @quickreview 16:05:27 AMA3 IR-419558-3DEXPERIENCER2017x Outlook Task: commands incorrectly displayed, handled type and its translation
 *
 */
 
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Access;
import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectAttributes;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.Group;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.Relationship;
import matrix.db.Role;
import matrix.util.List;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.SelectList;
import matrix.util.StringList;

import com.matrixone.apps.common.Document;
import com.matrixone.apps.common.InboxTask;
import com.matrixone.apps.common.Organization;
import com.matrixone.apps.common.Route;
import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.framework.ui.UIUtil;

import com.matrixone.apps.domain.util.i18nNow;

public class msoiMSFInboxTask_mxJPO extends emxDomainObject_mxJPO
{
     private static final String sAttrTitle = PropertyUtil.getSchemaProperty("attribute_Title");
	private static final String strAttrTitle="attribute["+sAttrTitle+"]";
	private static final String sAttrRouteAction = PropertyUtil.getSchemaProperty("attribute_RouteAction");
	private static final String strAttrRouteAction = "attribute["+sAttrRouteAction +"]";
	private static final String sTypeWorkflowTask = PropertyUtil.getSchemaProperty("type_WorkflowTask");
	private static final String sRelWorkflowTask = PropertyUtil.getSchemaProperty("relationship_WorkflowTask");
	private static final String workflowNameSelectStr = "to["+sRelWorkflowTask+"].from.name";
	private static final String sRelRouteTask = PropertyUtil.getSchemaProperty("relationship_RouteTask");
	private static final String sRelRouteScope = PropertyUtil.getSchemaProperty("relationship_RouteScope");
	private static final String attrworkFlowInstructions = PropertyUtil.getSchemaProperty("attribute_Instructions");
	private static String routeNameSelectStr ="from["+sRelRouteTask+"].to.name";	
	private static String objectIdSelectStr="from["+sRelRouteTask+"].to.to["+sRelRouteScope+"].from.id";
	private static String objectNameSelectStr="from["+sRelRouteTask+"].to.to["+sRelRouteScope+"].from.name";

	private static final String sAttrScheduledCompletionDate = PropertyUtil.getSchemaProperty("attribute_ScheduledCompletionDate");
    private static final String strAttrCompletionDate ="attribute["+sAttrScheduledCompletionDate+"]";
    private static final String attrTaskEstinatedFinishDate = PropertyUtil.getSchemaProperty("attribute_TaskEstimatedFinishDate");
    private static String strAttrTaskEstimatedFinishDate = "attribute[" + attrTaskEstinatedFinishDate + "]";
    private static final String attrworkFlowDueDate = PropertyUtil.getSchemaProperty("attribute_DueDate");	
	private static String strAttrTaskEstimatedStartDate = "attribute[" + PropertyUtil.getSchemaProperty("attribute_TaskEstimatedStartDate") + "]";
	private static String strAttrTaskStartDate = "attribute[" + PropertyUtil.getSchemaProperty("attribute_TaskActualStartDate") + "]";
	
    private static String strAttrworkFlowDueDate = "attribute[" + attrworkFlowDueDate + "]";
    private static final String TaskActionformat="<Name actionType=\"%1$s\" displayName=\"%2$s\" />";
    /**
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public msoiMSFInboxTask_mxJPO (Context context, String[] args)
        throws Exception
    {
      super(context, args);
    }

    /**
     * This method is executed if a specific method is not specified.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @returns nothing
     * @throws Exception if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public int mxMain(Context context, String[] args)
        throws Exception
    {
        if (!context.isConnected())
            throw new Exception(ComponentsUtil.i18nStringNow("emxComponents.Generic.NotSupportedOnDesktopClient", context.getLocale().getLanguage()));
        return 0;
    }	

	/**
     * showTaskName - displays the owner with lastname,firstname format
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object
     * @throws Exception if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public Vector showTaskName(Context context, String[] args)
        throws Exception
    {
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList)programMap.get("objectList");
            DomainObject taskObject = new DomainObject();
            Vector vecShowTaskName  = new Vector();
            String name= "";
            String sTaskName  ="";
            String taskId  ="";         

            Iterator objectListItr = objectList.iterator();
            while(objectListItr.hasNext())
            {
                Map objectMap = (Map) objectListItr.next();
                //modified for IR-050410V6R2011x
               // sTaskName  = (String)objectMap.get("strAttrTitle");
                sTaskName  = (String)objectMap.get("name");
                taskId  = (String)objectMap.get(DomainObject.SELECT_ID);
                //Bug 318463. Modified if condition and assigning title to name instead of adding it directly to vector.
                if(sTaskName!= null && !sTaskName.equals("")) {
                    name = sTaskName;
                }
                else
                {
                    taskObject.setId((String)objectMap.get(taskObject.SELECT_ID));
                    name=taskObject.getInfo(context,"name");
                }

                vecShowTaskName.add(name);
                //End- Bug 318463.
            }
            return vecShowTaskName;
        }
        catch (Exception ex)
        {
            System.out.println("Error in showTaskName= " + ex.getMessage());
            throw ex;
        }
    }

    /** Added for IR-050410V6R2011x
     * showTaskTitle - displays the Inbox Task Title
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object
     * @throws Exception if the operation fails
     * @since R210
     * @grade 0
     */
    public Vector showTaskTitle(Context context, String[] args)
        throws Exception
    {
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList)programMap.get("objectList");
            DomainObject taskObject = new DomainObject();
            Vector vecShowTaskTitle  = new Vector();
            String sTaskTitle  ="";

            Iterator objectListItr = objectList.iterator();
            while(objectListItr.hasNext())
            {
                Map objectMap = (Map) objectListItr.next();
                sTaskTitle  = (String)objectMap.get(strAttrTitle);
                sTaskTitle = UIUtil.isNullOrEmpty(sTaskTitle) ? EMPTY_STRING : sTaskTitle;
                vecShowTaskTitle.add(sTaskTitle);
            }
            return vecShowTaskTitle;
        }
        catch (Exception ex)
        {
            System.out.println("Error in showTaskTitle= " + ex.getMessage());
            throw ex;
        }
    }
	
	/**
     * showType - shows the type of the task
     * Inbox Task - Route Action attribute value
     * Workflow Task - Activity
     * Task - WBS Task
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object of type Vector
     * @throws Exception if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public Vector showType(Context context, String[] args) throws Exception
    {
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramList");
            MapList objectList = (MapList)programMap.get("objectList");
            String languageStr = (String)paramMap.get("languageStr");
            Vector vShowType = new Vector();
            Map objectMap = null;

            int objectListSize = 0 ;
            if(objectList != null)
            {
                objectListSize = objectList.size();
            }

            for(int i=0; i< objectListSize; i++)
            {
                try
                {
                    objectMap = (HashMap) objectList.get(i);
                }
                catch(ClassCastException cce)
                {
                    objectMap = (Hashtable) objectList.get(i);
                }

                String sTypeName = (String) objectMap.get(DomainObject.SELECT_TYPE);
                String sTypeString = "";

                if (TYPE_INBOX_TASK.equalsIgnoreCase(sTypeName))
                {
                    sTypeString = (String) objectMap.get(strAttrRouteAction);
                    if (sTypeString == null)
                    {
                        sTypeString = "";
                    }
                    else
                    {
			sTypeName=sTypeString;
                        sTypeString =  EnoviaResourceBundle.getProperty(context,"emxFrameworkStringResource", new Locale(languageStr),"emxFramework.Range.Route_Action."+sTypeString.replace(' ','_'));
                    }
                }
                else if (TYPE_TASK.equalsIgnoreCase(sTypeName))
                {
                    sTypeString = EnoviaResourceBundle.getProperty(context,"emxComponentsStringResource", new Locale(languageStr),"emxComponents.Route.Type."+sTypeName.replace(' ','_'));
                }
                else if (sTypeWorkflowTask.equalsIgnoreCase(sTypeName))
                {
                    sTypeString = EnoviaResourceBundle.getProperty(context,"emxComponentsStringResource", new Locale(languageStr),"emxComponents.Route.Type."+sTypeName.replace(' ','_'));
                }
                else
                {
                    sTypeString = EnoviaResourceBundle.getTypeI18NString(context,sTypeName, languageStr);
                }

		String TaskActionXML=String.format(TaskActionformat,sTypeName,sTypeString);

		vShowType.add(TaskActionXML);
            }

            return vShowType;
        }
        catch (Exception ex)
        {
            System.out.println("Error in showType= " + ex.getMessage());
            throw ex;
        }
    }
	
	/**
     * showType - shows the task instructions
     * Inbox Task - Route Instructions
     * Workflow Task - Instructions
     * Task - Notes
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object of type Vector
     * @throws Exception if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public Vector showInstructions(Context context, String[] args) throws Exception
    {

        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramList");
            MapList objectList = (MapList)programMap.get("objectList");

            Vector vShowNotes = new Vector();
            Map objectMap = null;

            int objectListSize = 0 ;
            if(objectList != null)
            {
                objectListSize = objectList.size();
            }

            for(int i=0; i< objectListSize; i++)
            {
                try
                {
                    objectMap = (HashMap) objectList.get(i);
                }
                catch(ClassCastException cce)
                {
                    objectMap = (Hashtable) objectList.get(i);
                }

                String sTypeName = (String) objectMap.get(DomainObject.SELECT_TYPE);
                String objectId = (String) objectMap.get(DomainObject.SELECT_ID);
                String sTypeNotes = "";

                DomainObject domObject = new DomainObject(objectId);

                if (TYPE_INBOX_TASK.equalsIgnoreCase(sTypeName))
                {
                    sTypeNotes = (String) domObject.getAttributeValue(context, DomainObject.ATTRIBUTE_ROUTE_INSTRUCTIONS);
                }
                else if (TYPE_TASK.equalsIgnoreCase(sTypeName))
                {
                    sTypeNotes = (String) domObject.getAttributeValue(context, DomainObject.ATTRIBUTE_NOTES);
                }

                else if (sTypeWorkflowTask.equalsIgnoreCase(sTypeName))
                {
                    sTypeNotes = (String) domObject.getAttributeValue(context, attrworkFlowInstructions);
                }
                if (sTypeNotes == null)
                {
                    sTypeNotes = "";
                }
                vShowNotes.add(sTypeNotes);
            }

            return vShowNotes;
        }
        catch (Exception ex)
        {
            System.out.println("Error in showInstructions= " + ex.getMessage());
            throw ex;
        }
    }
	
	/**
     * showRoute - Retrives the Tasks parent objects
     * Inbox Task - Route
     * Workflow Task - Workflow
     * Task - Project Space
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object of type Vector
     * @throws Exception if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public Vector showRoute(Context context, String[] args) throws Exception
    {
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramList");
            MapList objectList = (MapList)programMap.get("objectList");

            Map objectMap = null;
            Vector showRoute = new Vector();
            String statusImageString = "";
            String sRouteString = "";
            boolean isPrinterFriendly = false;
            String strPrinterFriendly = (String)paramMap.get("reportFormat");
            String languageStr = (String)paramMap.get("languageStr");

            String sAccDenied = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource",new Locale(languageStr),"emxComponents.Common.AccessDenied");

            if (strPrinterFriendly != null )
            {
                isPrinterFriendly = true;
            }

            int objectListSize = 0 ;
            if(objectList != null)
            {
                objectListSize = objectList.size();
            }
            for(int i=0; i< objectListSize; i++)
            {
                statusImageString = "";
                sRouteString = "";
                try
                {
                    objectMap = (HashMap) objectList.get(i);
                }
                catch(ClassCastException cce)
                {
                    objectMap = (Hashtable) objectList.get(i);
                }

                String sTypeName = (String) objectMap.get(DomainObject.SELECT_TYPE);                
                String sObjectName = "";

                if (TYPE_INBOX_TASK.equalsIgnoreCase(sTypeName))
                {
                    sObjectName = (String)objectMap.get(routeNameSelectStr);
                }
                else if (TYPE_TASK.equalsIgnoreCase(sTypeName))
                {
                    //Bug 318463. Commented below two lines and added 2 new lines to read id and name from main list.                  
                    sObjectName = (String)objectMap.get("Context Object Name");
                }
                else if (sTypeWorkflowTask.equalsIgnoreCase(sTypeName))
                {                  
                    sObjectName = (String)objectMap.get(workflowNameSelectStr);
                }
				
				//RZW+  For WBS Tasks we do not see the value becaus the map does not contain that attribute
                //Bug 318325. If object id and Name are null don't show context object.
                if(sObjectName != null )
                {
                    sRouteString = sObjectName;
                    showRoute.add(sRouteString);
                }
                else
                {
                    showRoute.add(sAccDenied);
                }
            }

            return showRoute;
        }
        catch (Exception ex)
        {
            System.out.println("Error in showRoute= " + ex.getMessage());
            throw ex;
        }
    }
	
	/**
     * showOwner - displays the owner with lastname,firstname format
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object
     * @throws Exception if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public Vector showWorkspace(Context context, String[] args)
        throws Exception
    {
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList)programMap.get("objectList");

            Vector vecShowWorkspace = new Vector();
            Iterator objectListItr = objectList.iterator();
            while(objectListItr.hasNext())
            {
                Map objectMap = (Map) objectListItr.next();
                String sRelatedObjectId   = (String)objectMap.get(objectIdSelectStr);
                String sRelatedObjectName = (String)objectMap.get(objectNameSelectStr);
				if (null == sRelatedObjectName)
					sRelatedObjectName = "";
					
                vecShowWorkspace.add(sRelatedObjectName);
            }

            return vecShowWorkspace;
        }
        catch (Exception ex)
        {
            System.out.println("Error in showWorkspace= " + ex.getMessage());
            throw ex;
        }
    }
    
    /**
     * showType - shows the due date for the task
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object of type Vector
     * @throws Exception if the operation fails
     */
    public Vector showDueDate(Context context, String[] args) throws Exception
    {
        Vector showDueDate = new Vector();
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList)programMap.get("objectList");
            Map paramList      = (Map)programMap.get("paramList");
            String sLanguage = (String)paramList.get("languageStr");

            String dueDate   = "";
            String dueDateOffset   = "";
            String dueDateOffsetFrom   = "";

            Iterator objectListItr = objectList.iterator();
            while(objectListItr.hasNext())
            {
                dueDate   = "";

                Map objectMap = (Map) objectListItr.next();
                String taskDueDate = "";
                String sTypeName = (String)objectMap.get(DomainObject.SELECT_TYPE);

                if ((DomainObject.TYPE_INBOX_TASK).equalsIgnoreCase(sTypeName))
                {
                    taskDueDate = (String)objectMap.get(strAttrCompletionDate);
                }
                else if ((DomainObject.TYPE_TASK).equalsIgnoreCase(sTypeName))
                {
                    taskDueDate = (String)objectMap.get(strAttrTaskEstimatedFinishDate);
                }
                else if (sTypeWorkflowTask.equalsIgnoreCase(sTypeName))
                {
                    taskDueDate = (String)objectMap.get(strAttrworkFlowDueDate);
                }
                else if(DomainConstants.RELATIONSHIP_ROUTE_NODE.equals(sTypeName))
                {
                    StringBuffer sb = new StringBuffer();
                    taskDueDate = (String)objectMap.get(strAttrCompletionDate);
                    dueDateOffset = (String)objectMap.get(getAttributeSelect(DomainObject.ATTRIBUTE_DUEDATE_OFFSET));
                    dueDateOffsetFrom = (String)objectMap.get(getAttributeSelect(DomainObject.ATTRIBUTE_DATE_OFFSET_FROM));
                    boolean bDueDateEmpty  = UIUtil.isNullOrEmpty(taskDueDate) ? true : false;
                    boolean bDeltaDueDate = (!UIUtil.isNullOrEmpty(dueDateOffset) && bDueDateEmpty) ? true : false;

                    if(!bDeltaDueDate){
                        sb.append(taskDueDate).append(" ");
                    }else{

                        sb.append(dueDateOffset).append(" ").append(EnoviaResourceBundle.getProperty(context,"emxComponentsStringResource", new Locale(sLanguage),"emxComponents.common.DaysFrom")).
                        append(" ").append(i18nNow.getRangeI18NString( DomainObject.ATTRIBUTE_DATE_OFFSET_FROM, dueDateOffsetFrom,sLanguage));
                    }
                    taskDueDate = sb.toString();
                }

                Locale locale =context.getLocale();
                String timeZone = (String) (paramList != null ? paramList.get("timeZone") : programMap.get("timeZone"));
                double clientTZOffset   = (new Double(timeZone)).doubleValue();
                
                if(! UIUtil.isNullOrEmpty(taskDueDate)){
                	try {
                	taskDueDate =   eMatrixDateFormat.getFormattedDisplayDateTime(taskDueDate, clientTZOffset, locale);
                	} catch (Exception dateException){
                		//do nothing,This exception is added to avoid formatting of taskduedate if the value is not of type date  i.e for ex: 4 days after Route start Date
                		taskDueDate = taskDueDate;
                	}
                }

                showDueDate.add(taskDueDate);
            }
        }
        catch (Exception ex)
        {
            System.out.println("Error in showDueDate= " + ex.getMessage());
            throw ex;
        }
        return showDueDate;

    }
	
	public Vector showEstimatedStartDate(Context context, String[] args) throws Exception
	{
		Vector showEstimatedStartDate = new Vector();
		try
		{
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			MapList objectList = (MapList)programMap.get("objectList");
			Map paramList      = (Map)programMap.get("paramList");

			Locale locale = context.getLocale();
			String timeZone = (String) (paramList != null ? paramList.get("timeZone") : programMap.get("timeZone"));
			double clientTZOffset   = (new Double(timeZone)).doubleValue();

			Iterator objectListItr = objectList.iterator();
			while(objectListItr.hasNext())
			{
				Map objectMap = (Map) objectListItr.next();
				String taskEstStartDate = (String)objectMap.get(strAttrTaskEstimatedStartDate);

				if(!UIUtil.isNullOrEmpty(taskEstStartDate))
				{
					try {
						taskEstStartDate = eMatrixDateFormat.getFormattedDisplayDateTime(taskEstStartDate, clientTZOffset, locale);
					}
					catch (Exception dateException)
					{
						//do nothing,This exception is added to avoid formatting of taskEstStartDate if the value is not of type date  i.e for ex: 4 days after Route start Date
						taskEstStartDate = taskEstStartDate;
					}
				}
				showEstimatedStartDate.add(taskEstStartDate);
			}
		}		
		catch (Exception ex)
		{
			System.out.println("Error in showEstimatedStartDate= " + ex.getMessage());
			throw ex;
		}
		return showEstimatedStartDate;
	}

	public Vector showStartDate(Context context, String[] args) throws Exception
	{
		Vector showStartDate = new Vector();
		try
		{
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			MapList objectList = (MapList)programMap.get("objectList");
			Map paramList      = (Map)programMap.get("paramList");

			Locale locale = context.getLocale();
			String timeZone = (String) (paramList != null ? paramList.get("timeZone") : programMap.get("timeZone"));
			double clientTZOffset   = (new Double(timeZone)).doubleValue();

			Iterator objectListItr = objectList.iterator();
			while(objectListItr.hasNext())
			{
				Map objectMap = (Map) objectListItr.next();
				String taskStartDate = (String)objectMap.get(strAttrTaskStartDate);

				if(!UIUtil.isNullOrEmpty(taskStartDate))
				{
					try 
					{
						taskStartDate = eMatrixDateFormat.getFormattedDisplayDateTime(taskStartDate, clientTZOffset, locale);
					}
					catch (Exception dateException)
					{
						//do nothing,This exception is added to avoid formatting of taskEstStartDate if the value is not of type date  i.e for ex: 4 days after Route start Date
						taskStartDate = taskStartDate;
					}
				}
				showStartDate.add(taskStartDate);
			}
		}
		catch (Exception ex)
		{
			System.out.println("Error in showStartDate= " + ex.getMessage());
			throw ex;
		}
		return showStartDate;
	}
} 
