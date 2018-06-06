/*
**  emxWorkflowTaskBase
**
**  Copyright (c) 1992-2015 Dassault Systemes.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of MatrixOne,
**  Inc.  Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
*/

import matrix.db.*;
import matrix.util.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;
import com.matrixone.apps.common.*;
import com.matrixone.apps.framework.ui.UIForm;
import com.matrixone.apps.framework.ui.UINavigatorUtil;


/**
 * The <code>emxWorkflowBase</code> class contains methods for document.
 *
 *
 */

public class emxWorkflowTaskBase_mxJPO extends emxDomainObject_mxJPO
{

    private static final String _workflowTask_type = PropertyUtil.getSchemaProperty("type_WorkflowTask");
    private static final String _workflowTask_policy = PropertyUtil.getSchemaProperty("policy_WorkflowTask");
    private static final String _workflow_type = PropertyUtil.getSchemaProperty("type_Workflow");
    private static final String _workflow_policy = PropertyUtil.getSchemaProperty("policy_Workflow");
    private static final String sProcess                  = PropertyUtil.getSchemaProperty("attribute_Process");
    private static String strProcess                ="attribute["+sProcess+"]";
    private static final String _workflow_task_relationship = PropertyUtil.getSchemaProperty("relationship_WorkflowTask");

    Context _context;
    protected HashMap _requestMap = null;
    protected HashMap _settingMap = null;

    // Maximum number of colums in the table
    protected int _maxCol;

    // For i18n purposes
    protected String _lang;
    protected String _timeZone;

    //To Display dateFormat
    protected String _localDateFormat;
    protected String _allowKeyableDates = "false";

    // for special handling of redundant attributes
    protected HashMap _redundancyCounts = new HashMap();
    protected int _totalRedundancyCount = 0;


    // We instantiate our own UIForm and use it to render fields
    UIForm _uif = new UIForm();

        /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *    0 - String that holds the document object id.
     * @throws Exception if the operation fails
     *
     */

    public emxWorkflowTaskBase_mxJPO(Context context, String[] args)
      throws Exception
    {
        super(context, args);
    }

    public void accept(Context context, String[] args) throws Exception
    {
        String taskId = getInfo(context, DomainObject.SELECT_ID);
        DomainObject taskObj = null;
        if(taskId != null)
        {
            taskObj = new DomainObject(taskId);
        }
        StringList objSelects = new StringList();
        objSelects.add(DomainObject.SELECT_ID);
        Map tempMap = (Map)taskObj.getRelatedObject(context, PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask"), false, objSelects,new StringList());
        String workflowId = null;
        if(tempMap != null)
        {
            workflowId = (String)tempMap.get(DomainObject.SELECT_ID);
        }
        DomainObject workflowObj = null;
        if(workflowId != null)
        {
            workflowObj = new DomainObject(workflowId);
        }

        String processName = workflowObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process"));
        String workflowName = workflowObj.getInfo(context, "name");
        String activityName = taskObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Activity"));

        accept(context, processName, workflowName, activityName);

        //Promote the object
        taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_BranchTo"), "Assigned");
        taskObj.promote(context);

        //Send mail to the workflow owner
        StringList toList = new StringList();
        toList.add(workflowObj.getInfo(context, "owner"));

        StringList objectIdList = new StringList();
        objectIdList.add(taskObj.getId());

        String[] messageKeys = {"User"};
        String[] messageValues = {taskObj.getInfo(context, "owner")};

        MailUtil.sendNotification(context,
                toList,   // To List
                null,     // Cc List
                null,     // Bcc List
                "emxComponents.Workflow.WorkflowTaskAcceptedSubject",  // Subject key
                null,                                       // Subject keys
                null,                                       // Subject values
                "emxComponents.Workflow.WorkflowTaskAcceptedMessage",  // Message key
                messageKeys,         // Message keys
                messageValues,         // Message values
                objectIdList, // Object list
                null,         // company name
                "emxComponentsStringResource");     // Property file


    }

    protected void accept(Context context, String processName, String workflowName, String activityName) throws Exception
    {
        StringBuffer command = new StringBuffer(256);
        command.append("modify workflow '");
        command.append(processName);
        command.append("' '");
        command.append(workflowName);
        command.append("' ");
        command.append("interactive '");
        command.append(activityName);
        command.append("' acceptactivity");

        HashMap resMap = processMql(context, command.toString());
        String error = (String)resMap.get("error");
        if(error != null)
        {
            throw new Exception(error);
        }

    }

    public void suspend(Context context, String[] args) throws Exception
    {
        String taskId = getInfo(context, DomainObject.SELECT_ID);
        DomainObject taskObj = null;
        if(taskId != null)
        {
            taskObj = new DomainObject(taskId);
        }
        StringList objSelects = new StringList();
        objSelects.add(DomainObject.SELECT_ID);
        String relWorkflowTask       = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
        Pattern relPattern        = new Pattern("");
        relPattern.addPattern(relWorkflowTask);



        SelectList typeSelects = new SelectList(2);
        typeSelects.add(DomainObject.SELECT_ID);
        typeSelects.add(DomainObject.SELECT_NAME);
        StringList relSelects = new StringList(1);
        relSelects.add(DomainObject.SELECT_RELATIONSHIP_ID);

        MapList workflowList = taskObj.getRelatedObjects(context,
                 relPattern.getPattern(),
                 "*",
                 typeSelects,
                 relSelects,
                 true,
                 false,
                 (short)0,
                  null, //objectWhere
                 null, //relWhere,
                  null,
                 null,
                 null);

         String workflowId = null;
         Iterator mapItr = workflowList.iterator();
         while (mapItr.hasNext() )
         {
             Map item = (Map)mapItr.next();
             workflowId = (String)item.get(DomainObject.SELECT_ID);
         }



        DomainObject workflowObj = null;
        if(workflowId != null)
        {
            workflowObj = new DomainObject(workflowId);
        }

        String processName = workflowObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process"));
        String workflowName = workflowObj.getInfo(context, "name");
        String activityName = taskObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Activity"));

        suspend(context, processName, workflowName, activityName);

//      Promote the object
        taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_BranchTo"), "Suspended");
        taskObj.promote(context);
//      Send mail to Task Assignee.
        StringList toList = new StringList();
        toList.add(taskObj.getInfo(context, "owner"));

        StringList objectIdList = new StringList();
        objectIdList.add(taskObj.getId());

        String[] messageKeys = {"WorkflowTaskType", "TaskName"};
        String[] messageValues = {taskObj.getInfo(context, "type"), taskObj.getInfo(context, "name")};

        MailUtil.sendNotification(context,
                toList,   // To List
                null,     // Cc List
                null,     // Bcc List
                "emxComponents.Workflow.WorkflowTaskSuspendedSubject",  // Subject key
                null,                                       // Subject keys
                null,                                       // Subject values
                "emxComponents.Workflow.WorkflowTaskSuspendedMessage",  // Message key
                messageKeys,         // Message keys
                messageValues,         // Message values
                objectIdList, // Object list
                null,         // company name
                "emxComponentsStringResource");     // Property file
    }

    protected void suspend(Context context, String processName, String workflowName, String activityName) throws Exception
    {
        StringBuffer command = new StringBuffer(256);
        command.append("modify workflow '");
        command.append(processName);
        command.append("' '");
        command.append(workflowName);
        command.append("' ");
        command.append("interactive '");
        command.append(activityName);
        command.append("' suspendactivity");

        HashMap resMap = processMql(context, command.toString());
        String error = (String)resMap.get("error");
        if(error != null)
        {
            throw new Exception(error);
        }

    }

    public void resume(Context context, String[] args) throws Exception
    {
        String taskId = getInfo(context, DomainObject.SELECT_ID);
        DomainObject taskObj = null;
        if(taskId != null)
        {
            taskObj = new DomainObject(taskId);
        }
        StringList objSelects = new StringList();
        objSelects.add(DomainObject.SELECT_ID);
        String relWorkflowTask       = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
        Pattern relPattern        = new Pattern("");
        relPattern.addPattern(relWorkflowTask);



        SelectList typeSelects = new SelectList(2);
        typeSelects.add(DomainObject.SELECT_ID);
        typeSelects.add(DomainObject.SELECT_NAME);
        StringList relSelects = new StringList(1);
        relSelects.add(DomainObject.SELECT_RELATIONSHIP_ID);

         MapList workflowList = taskObj.getRelatedObjects(context,
                 relPattern.getPattern(),
                 "*",
                 typeSelects,
                 relSelects,
                 true,
                 false,
                 (short)0,
                  null, //objectWhere
                 null, //relWhere,
                  null,
                 null,
                 null);

         String workflowId = null;
         Iterator mapItr = workflowList.iterator();
         while (mapItr.hasNext() )
         {
             Map item = (Map)mapItr.next();
             workflowId = (String)item.get(DomainObject.SELECT_ID);
         }



        DomainObject workflowObj = null;
        if(workflowId != null)
        {
            workflowObj = new DomainObject(workflowId);
        }

        String processName = workflowObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process"));
        String workflowName = workflowObj.getInfo(context, "name");
        String activityName = taskObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Activity"));

        resume(context, processName, workflowName, activityName);
        //Demote the object
        taskObj.demote(context);
        //Send mail to Task assignee
        StringList toList = new StringList();
        toList.add(taskObj.getInfo(context, "owner"));

        StringList objectIdList = new StringList();
        objectIdList.add(taskObj.getId());

        String[] messageKeys = {"WorkflowTaskType", "TaskName"};
        String[] messageValues = {taskObj.getInfo(context, "type"), taskObj.getInfo(context, "name")};

        MailUtil.sendNotification(context,
                toList,   // To List
                null,     // Cc List
                null,     // Bcc List
                "emxComponents.Workflow.WorkflowTaskResumedSubject",  // Subject key
                null,                                       // Subject keys
                null,                                       // Subject values
                "emxComponents.Workflow.WorkflowTaskResumedMessage",  // Message key
                messageKeys,         // Message keys
                messageValues,         // Message values
                objectIdList, // Object list
                null,         // company name
                "emxComponentsStringResource");     // Property file
    }

    protected void resume(Context context, String processName, String workflowName, String activityName) throws Exception
    {
        StringBuffer command = new StringBuffer(256);
        command.append("modify workflow '");
        command.append(processName);
        command.append("' '");
        command.append(workflowName);
        command.append("' ");
        command.append("interactive '");
        command.append(activityName);
        command.append("' resumeactivity");

        HashMap resMap = processMql(context, command.toString());
        String error = (String)resMap.get("error");
        if(error != null)
        {
            throw new Exception(error);
        }

    }

    public void override(Context context, String[] args) throws Exception
    {
        String taskId = getInfo(context, DomainObject.SELECT_ID);
        DomainObject taskObj = null;
        if(taskId != null)
        {
            taskObj = new DomainObject(taskId);
        }
        StringList objSelects = new StringList();
        objSelects.add(DomainObject.SELECT_ID);
        HashMap tempMap = (HashMap)taskObj.getRelatedObject(context, PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask"), true, objSelects,new StringList());
        String relWorkflowTask       = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
        Pattern relPattern        = new Pattern("");
        relPattern.addPattern(relWorkflowTask);



        SelectList typeSelects = new SelectList(2);
        typeSelects.add(DomainObject.SELECT_ID);
        typeSelects.add(DomainObject.SELECT_NAME);
        StringList relSelects = new StringList(1);
        relSelects.add(DomainObject.SELECT_RELATIONSHIP_ID);

         MapList workflowList = taskObj.getRelatedObjects(context,
                 relPattern.getPattern(),
                 "*",
                 typeSelects,
                 relSelects,
                 true,
                 false,
                 (short)0,
                  null, //objectWhere
                 null, //relWhere,
                  null,
                 null,
                 null);

         String workflowId = null;
         Iterator mapItr = workflowList.iterator();
         while (mapItr.hasNext() )
         {
             Map item = (Map)mapItr.next();
             workflowId = (String)item.get(DomainObject.SELECT_ID);
         }



        DomainObject workflowObj = null;
        if(workflowId != null)
        {
            workflowObj = new DomainObject(workflowId);
        }

        String processName = workflowObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process"));
        String workflowName = workflowObj.getInfo(context, "name");
        String activityName = taskObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Activity"));

        override(context, processName, workflowName, activityName);

//      Promote the object
        taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_BranchTo"), "Overridden");
// modified by Yue Li for #330727            
//        SimpleDateFormat sdf = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat());
        SimpleDateFormat sdf = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat(),Locale.US);
        String formatedDate = sdf.format(new Date());
        taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_ActualCompletionDate"), formatedDate);
        taskObj.promote(context);
//      perform post complete actions means accept any tasks that are having a single assignee
        performPostCompleteActions(context, taskObj.getId());

//      Send mail to task assignee
        StringList toList = new StringList();
        toList.add(taskObj.getInfo(context, "owner"));

        StringList objectIdList = new StringList();
        objectIdList.add(taskObj.getId());

        MailUtil.sendNotification(context,
                toList,   // To List
                null,     // Cc List
                null,     // Bcc List
                "emxComponents.Workflow.WorkflowTaskOverrideSubject",  // Subject key
                null,                                       // Subject keys
                null,                                       // Subject values
                "emxComponents.Workflow.WorkflowTaskOverrideMessage",  // Message key
                null,         // Message keys
                null,         // Message values
                objectIdList, // Object list
                null,         // company name
                "emxComponentsStringResource");     // Property file
    }

    protected void override(Context context, String processName, String workflowName, String activityName) throws Exception
    {
        StringBuffer command = new StringBuffer(256);
        command.append("modify workflow '");
        command.append(processName);
        command.append("' '");
        command.append(workflowName);
        command.append("' ");
        command.append("interactive '");
        command.append(activityName);
        command.append("' overrideactivity");

        HashMap resMap = processMql(context, command.toString());
        String error = (String)resMap.get("error");
        if(error != null)
        {
            throw new Exception(error);
        }

    }

    private void updateAssignee(Context context, String taskId, String personName) throws Exception
    {
        DomainObject domObj = new DomainObject(taskId);
        String taskOwner = domObj.getInfo(context, "owner");

        StringList objSelects = new StringList();
        objSelects.add(DomainObject.SELECT_ID);
        String relWorkflowTaskAssignee = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTaskAssignee");
        Pattern relPattern = new Pattern("");
        relPattern.addPattern(relWorkflowTaskAssignee);

        SelectList typeSelects = new SelectList(2);
        typeSelects.add(DomainObject.SELECT_ID);
        typeSelects.add(DomainObject.SELECT_NAME);
        StringList relSelects = new StringList(1);
        relSelects.add(DomainObject.SELECT_RELATIONSHIP_ID);

         MapList personList = domObj.getRelatedObjects(context,
                 relPattern.getPattern(),
                 "Person",
                 typeSelects,
                 relSelects,
                 false,
                 true,
                 (short)0,
                  null, //objectWhere
                 null, //relWhere,
                  null,
                 null,
                 null);

         String relId = null;
         Iterator mapItr = personList.iterator();
         DomainObject personObj = new DomainObject();
         while (mapItr.hasNext() )
         {
             Map item = (Map)mapItr.next();
             relId = (String)item.get(DomainObject.SELECT_RELATIONSHIP_ID);
         }

         HashMap map = PersonUtil.getPersonMap(context, personName);
         personObj.setId((String)map.get("id"));

        /*
        Bug - 319310 - Start
        If relid is null means task is assigned to a role, so do the task assignment via the super user
        If relid is not null then do the task assignment as task owner as owner has all access.
        */
        if (relId == null)
        {
            // set the task owneship and assignment via the workflow owner
            /** The parent/workflow owner of the task. */
            String workflowOwner = domObj.getInfo(context, "to[" + _workflow_task_relationship + "].from.owner");
            try
            {
                ContextUtil.pushContext(context, workflowOwner, null, null);

                //Bug - 319310 - get the current state of the object
                String taskState = domObj.getInfo(context, "current");

                domObj.addToObject(context, new RelationshipType(relWorkflowTaskAssignee), (String)map.get("id"));

                String stateWorkFlowTaskStarted = PropertyUtil.getSchemaProperty(context,"policy", PropertyUtil.getSchemaProperty(context,"policy_WorkflowTask"), "state_Started");
                if (taskState.equals(stateWorkFlowTaskStarted))
                {
                    String stateWorkFlowTaskAssigned = PropertyUtil.getSchemaProperty(context,"policy", PropertyUtil.getSchemaProperty(context,"policy_WorkflowTask"), "state_Assigned");
                    domObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context,"attribute_BranchTo"), stateWorkFlowTaskAssigned);
                    domObj.promote(context);
                }

                domObj.setOwner(context, personName);
            }
            catch(Exception e)
            {
                throw e;
            }
            finally
            {
                 ContextUtil.popContext(context);
            }
        }
        else
        {
            try
            {
                ContextUtil.pushContext(context, taskOwner, null, null);
                DomainRelationship.setToObject(context, relId, personObj);
                domObj.setOwner(context, personName);
            }
            catch(Exception e)
            {
                throw e;
            }
            finally
            {
                 ContextUtil.popContext(context);
            }
        }
        // 319310 - End

    }

    public void reassign(Context context, String[] args) throws Exception
    {
        HashMap paramMap = (HashMap)JPO.unpackArgs(args);
        String user = "";
        if(paramMap != null)
        {
            user = (String)paramMap.get("user");
        }
        String taskId = getInfo(context, DomainObject.SELECT_ID);
        DomainObject taskObj = null;
        if(taskId != null)
        {
            taskObj = new DomainObject(taskId);
        }
        StringList objSelects = new StringList();
        objSelects.add(DomainObject.SELECT_ID);
        String relWorkflowTask       = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
        Pattern relPattern        = new Pattern("");
        relPattern.addPattern(relWorkflowTask);



        SelectList typeSelects = new SelectList(2);
        typeSelects.add(DomainObject.SELECT_ID);
        typeSelects.add(DomainObject.SELECT_NAME);
        StringList relSelects = new StringList(1);
        relSelects.add(DomainObject.SELECT_RELATIONSHIP_ID);

         MapList workflowList = taskObj.getRelatedObjects(context,
                 relPattern.getPattern(),
                 "*",
                 typeSelects,
                 relSelects,
                 true,
                 false,
                 (short)0,
                  null, //objectWhere
                 null, //relWhere,
                  null,
                 null,
                 null);

         String workflowId = null;
         Iterator mapItr = workflowList.iterator();
         while (mapItr.hasNext() )
         {
             Map item = (Map)mapItr.next();
             workflowId = (String)item.get(DomainObject.SELECT_ID);
         }



        DomainObject workflowObj = null;
        if(workflowId != null)
        {
            workflowObj = new DomainObject(workflowId);
        }

        String processName = workflowObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process"));
        String workflowName = workflowObj.getInfo(context, "name");
        String activityName = taskObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Activity"));

        //Send notification to original owner
        StringList toList = new StringList();
        toList.add(taskObj.getInfo(context, "owner"));

        String[] messageKeys = {"WorkflowTaskType", "TaskName", "WorkflowType", "WorkflowName", "WorkflowOwner"};
        String[] messageValues = {taskObj.getInfo(context, "type"),
                                  taskObj.getInfo(context, "name"),
                                  workflowObj.getInfo(context, "type"),
                                  workflowObj.getInfo(context, "name"),
                                  workflowObj.getInfo(context, "owner")};

        MailUtil.sendNotification(context,
                toList,   // To List
                null,     // Cc List
                null,     // Bcc List
                "emxComponents.Workflow.WorkflowTaskReassignSubject",  // Subject key
                null,                                       // Subject keys
                null,                                       // Subject values
                "emxComponents.Workflow.WorkflowTaskReassignMessage",  // Message key
                messageKeys,         // Message keys
                messageValues,         // Message values
                null, // Object list
                null,         // company name
                "emxComponentsStringResource");     // Property file

        //End notification

        reassign(context, processName, workflowName, activityName, user);
        updateAssignee(context, taskId, user);
    }

    protected void reassign(Context context, String processName, String workflowName, String activityName, String user) throws Exception
    {
        StringBuffer command = new StringBuffer(256);
        command.append("modify workflow '");
        command.append(processName);
        command.append("' '");
        command.append(workflowName);
        command.append("' ");
        command.append("interactive '");
        command.append(activityName);
        command.append("' reassignactivity user '");
        command.append(user);
        command.append("'");

        HashMap resMap = processMql(context, command.toString());
        String error = (String)resMap.get("error");
        if(error != null)
        {
            throw new Exception(error);
        }

    }

    public void complete(Context context, String[] args) throws Exception
    {
        try
        {
            //UnCommented for BX2 Bug 332190 3 Begin
            ContextUtil.startTransaction(context, true);
            //UnCommented for BX2 Bug 332190 3  End
            String taskId = getInfo(context, DomainObject.SELECT_ID);
            DomainObject taskObj = null;
            if(taskId != null)
            {
                taskObj = new DomainObject(taskId);
            }
            StringList objSelects = new StringList();
            objSelects.add(DomainObject.SELECT_ID);
            //HashMap tempMap = (HashMap)taskObj.getRelatedObject(context, PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask"), false, objSelects,new StringList());

            String relWorkflowTask       = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
            Pattern relPattern        = new Pattern("");
            relPattern.addPattern(relWorkflowTask);



            SelectList typeSelects = new SelectList(2);
            typeSelects.add(DomainObject.SELECT_ID);
            typeSelects.add(DomainObject.SELECT_NAME);
            StringList relSelects = new StringList(1);
            relSelects.add(DomainObject.SELECT_RELATIONSHIP_ID);



             MapList workflowList = taskObj.getRelatedObjects(context,
                     relPattern.getPattern(),
                     "*",
                     typeSelects,
                     relSelects,
                     true,
                     false,
                     (short)0,
                      null, //objectWhere
                     null, //relWhere,
                      null,
                     null,
                     null);

             String workflowId = null;
             Iterator mapItr = workflowList.iterator();
             while (mapItr.hasNext() )
             {
                 Map item = (Map)mapItr.next();
                 workflowId = (String)item.get(DomainObject.SELECT_ID);
             }


            DomainObject workflowObj = null;
            if(workflowId != null)
            {
                workflowObj = new DomainObject(workflowId);
            }

            String processName = workflowObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process"));
            String workflowName = workflowObj.getInfo(context, "name");
            String activityName = taskObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Activity"));

            complete(context, processName, workflowName, activityName);
    //      Promote the object
            taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_BranchTo"), "Completed");
// modified by Yue Li for #330727                
//            SimpleDateFormat sdf = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat());
            SimpleDateFormat sdf = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat(),Locale.US);
            String formatedDate = sdf.format(new Date());
            taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_ActualCompletionDate"), formatedDate);
            taskObj.promote(context);
    //      perform post complete actions means accept any tasks that are having a single assignee
            performPostCompleteActions(context, taskObj.getId());
    //      Send mail to workflow owner
            StringList toList = new StringList();
            toList.add(workflowObj.getInfo(context, "owner"));

            StringList objectIdList = new StringList();
            objectIdList.add(taskObj.getId());

            String[] messageKeys = {"WorkflowTaskType", "TaskName", "WorkflowType", "WorkflowName"};
            String[] messageValues = {taskObj.getInfo(context, "type"),
                                      taskObj.getInfo(context, "name"),
                                      workflowObj.getInfo(context, "type"),
                                      workflowObj.getInfo(context, "name")};

            MailUtil.sendNotification(context,
                    toList,   // To List
                    null,     // Cc List
                    null,     // Bcc List
                    "emxComponents.Workflow.WorkflowTaskCompletedSubject",  // Subject key
                    null,                                       // Subject keys
                    null,                                       // Subject values
                    "emxComponents.Workflow.WorkflowTaskCompletedMessage",  // Message key
                    messageKeys,         // Message keys
                    messageValues,         // Message values
                    objectIdList, // Object list
                    null,         // company name
                    "emxComponentsStringResource");     // Property file
        }
        catch(Exception ex)
        {
            //UnCommented for BX2 Bug 332190 3 Begin
            ContextUtil.abortTransaction(context);
            //UnCommented for BX2 Bug 332190 3 End
        }
        finally
        {
            //UnCommented for BX2 Bug 332190 3 Begin
            ContextUtil.commitTransaction(context);
            //UnCommented for BX2 Bug 332190 3 End
        }
    }

    protected void complete(Context context, String processName, String workflowName, String activityName) throws Exception
    {
        StringBuffer command = new StringBuffer(256);
        command.append("modify workflow '");
        command.append(processName);
        command.append("' '");
        command.append(workflowName);
        command.append("' ");
        command.append("interactive '");
        command.append(activityName);
        command.append("' completeactivity");

        HashMap resMap = processMql(context, command.toString());
        String error = (String)resMap.get("error");
        if(error != null)
        {
            throw new Exception(error);
        }

    }

    private static HashMap processMql(Context context, String cmd) throws Exception
    {
         MQLCommand mql = new MQLCommand();
         HashMap returnMap = new HashMap();
         boolean bResult = mql.executeCommand(context, cmd);
         if(bResult)
         {
             returnMap.put("result", mql.getResult().trim());
             return returnMap;
         }
         else
         {
             returnMap.put("error", mql.getError());
             return returnMap;
         }
    }

    /**
     * getWorkflowTaskAttributes - Displas the process attributes
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns String
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     */

    public String getWorkflowTaskAttributes(Context context, String[] args)
        throws Exception
    {
        HashMap programMap  = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap  = (HashMap) programMap.get("requestMap");
        HashMap paramMap    = (HashMap) programMap.get("paramMap");
        Integer maxColInt   = (Integer) programMap.get("maxCols");
        HashMap fieldMap    = (HashMap) programMap.get("fieldMap");
        String objectId     = (String) paramMap.get("objectId");
        String mode         = (String) requestMap.get("mode");
        StringBuffer finalHTML = new StringBuffer();

        _context = context;
        _requestMap = requestMap;
        _maxCol = maxColInt.intValue() * 2;  // label+value
        _lang = (String)requestMap.get("languageStr");
// modified by Yue Li for #330727        
//        _localDateFormat = ((java.text.SimpleDateFormat)java.text.DateFormat.getDateInstance(eMatrixDateFormat.getEMatrixDisplayDateFormat(),
//                            (Locale)_requestMap.get("localeObj"))).toLocalizedPattern();
        _localDateFormat = ((java.text.SimpleDateFormat)java.text.DateFormat.getDateInstance(eMatrixDateFormat.getEMatrixDisplayDateFormat(),
                            Locale.US)).toLocalizedPattern();
        _timeZone = (String)requestMap.get("timeZone");

        if(fieldMap != null) {
            _settingMap = (HashMap) fieldMap.get("settings");
        }

        try
        {

            String relWorkflowTask       = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
            Pattern relPattern        = new Pattern("");
            relPattern.addPattern(relWorkflowTask);

            DomainObject taskObject = DomainObject.newInstance(context, objectId);
            String activityName = taskObject.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Activity"));

            SelectList typeSelects = new SelectList(2);
            typeSelects.add(DomainObject.SELECT_ID);
            typeSelects.add(DomainObject.SELECT_NAME);


            StringList relSelects = new StringList(1);
            relSelects.add(DomainObject.SELECT_RELATIONSHIP_ID);
            MapList workflowList = taskObject.getRelatedObjects(context,
                                                          relPattern.getPattern(),
                                                          "*",
                                                          typeSelects,
                                                          relSelects,
                                                          true,
                                                          false,
                                                          (short)0,
                                                          null, //objectWhere
                                                          null, //relWhere,
                                                          null,
                                                          null,
                                                          null);

            String tempId ="";
            Iterator mapItr = workflowList.iterator();
            while (mapItr.hasNext() )
            {
               Map item = (Map)mapItr.next();
               tempId = (String)item.get(DomainObject.SELECT_ID);
            }

            setId(tempId);
            String result = getInfo(context,strProcess);


           String cmd = "print workflow \""+result+"\""+ " "+"\""+getName()+"\""+ " select interactive["  + activityName+"].attribute[].value";

              HashMap processMap = processMql(context,cmd);
              result = (String)processMap.get("result");
              // Tested by assigning the mql output is like below code
              if(result!=null && !"".equals(result)) {
                  StringTokenizer st = new StringTokenizer(result,"\n");                  
                  while(st.hasMoreTokens()) {
                      String token = (String)st.nextToken();
                      if(token.indexOf("attribute[")!=-1) {
                      String attrName  = token.substring(token.indexOf("attribute[")+10,token.indexOf("].value"));
                      String attrValue = token.substring(token.indexOf("=")+1,token.length());
                      if ("edit".equalsIgnoreCase(mode)) {
                          finalHTML.append(getFieldEditHTML(attrName,attrValue));
                      } else {
                          finalHTML.append(getFieldViewHTML(attrName,attrValue));
                          }
                      }
                  } //end of while loop
              }  // end of if(result
        }
        catch (Exception ex)
        {
            throw ex;
        }
        String retHTMLValue = finalHTML.toString();
        if("".equals(retHTMLValue))
            return "<!-- No attributes -->";
        return retHTMLValue;

    }


    /**
     * Displays attribute in .webform view mode
     *
     * @param String
     *            Attribute Name
     * @param String
     *            Attribute Value
     * @return String html code to display attribute in view mode
     * @throws Exception
     *             if the operation fails
     * @since 10.6.SP2
     */

    public String getFieldViewHTML(String attributeName, String attributeValue)
            throws Exception {

        StringBuffer buf = new StringBuffer();
        AttributeType attribType = new AttributeType(attributeName);
        attribType.open(_context);
        String sDataType = attribType.getDataType();

        HashMap settings = new HashMap();
        settings.put("Required", "false");
        settings.put("Field Type", "attribute");
        settings.put("Registered Suite", "Framework");
        settings.put("Editable", "true");

        if ("timestamp".equalsIgnoreCase(sDataType)) {
            settings.put("format", "date");
        }
        if ("boolean".equalsIgnoreCase(sDataType)){
           StringList aChoices = new StringList();
           aChoices.add("FALSE");
           aChoices.add("TRUE");
           aChoices = UINavigatorUtil.getAttrRangeI18NStringList(attributeName, aChoices,_lang);
           attributeValue = (String)aChoices.get(attributeValue.equalsIgnoreCase("TRUE") ? 1 : 0);
        }

        HashMap field = new HashMap();
        field.put("settings", settings);
        field.put("field_value", attributeValue);
        field.put("hasAccess", "true");
//      field.put("field_display_value", attributeValue);
        field.put("label", i18nNow.getAttributeI18NString(attributeName, _lang));
        field.put("StringResourceFileId", (String)_requestMap.get("StringResourceFileId"));
        field.put("suiteDirectory", (String)_requestMap.get("SuiteDirectory"));
        field.put("suiteKey", (String)_requestMap.get("suiteKey"));
        field.put("name", "Name");

        buf.append("<tr>");
        buf.append(_uif.drawFormViewElement(_context, _requestMap, field,
                _timeZone, "false", true, _maxCol));
        buf.append("</tr>");

        return buf.toString();
    }


    /**
     * Displays attribute in .webform edit mode
     *
     * @param String
     *            Attribute Name
     * @param String
     *            Attribute Value
     * @return String html code to display attribute in edit mode
     * @throws Exception
     *             if the operation fails
     * @since 10.6.SP2
     */

    public String getFieldEditHTML(String attributeName, String attributeValue)
            throws Exception {

        StringBuffer buf = new StringBuffer();

        AttributeType attribType = new AttributeType(attributeName);
        attribType.open(_context);
        String dataType = attribType.getDataType(_context);

        HashMap field = new HashMap();
        HashMap settings = new HashMap();
        settings.put("Required", "false");
        settings.put("Field Type", "attribute");

        String manualEdit = (String)_settingMap.get("Allow Manual Edit");

        if(manualEdit != null && !"null".equals(manualEdit)) {
            settings.put("Allow Manual Edit", manualEdit);
        }

        if (dataType.equalsIgnoreCase("string")) {
            StringList aChoices = attribType.getChoices();
            if (aChoices != null && aChoices.size() > 0) {
                settings.put("Input Type", "combobox");

                field.put("field_choices", aChoices);

                StringList aDispChoices = UINavigatorUtil.getAttrRangeI18NStringList(attributeName, aChoices,_lang);
                field.put("field_display_choices", aDispChoices);
                String dispValue = UINavigatorUtil.getAttrRangeI18NString(attributeName, attributeValue,_lang);
                field.put("field_display_value", dispValue);
            }
        } else if (dataType.equalsIgnoreCase("boolean")) {
            settings.put("Input Type", "combobox");

            StringList aChoices = new StringList();
            aChoices.add("FALSE");
            aChoices.add("TRUE");
            field.put("field_choices", aChoices);

            StringList aDispChoices = UINavigatorUtil.getAttrRangeI18NStringList(attributeName, aChoices,_lang);
            field.put("field_display_choices", aDispChoices);
            String dispValue = UINavigatorUtil.getAttrRangeI18NString(attributeName, attributeValue,_lang);
            field.put("field_display_value", dispValue);

        } else if ("timestamp".equalsIgnoreCase(dataType)) {
            settings.put("format", "date");
        }

        settings.put("Registered Suite", "Framework");
        settings.put("Editable", "true");

        HashMap inputMap = new HashMap();
        inputMap.put("componentType", "form");
        inputMap.put("localDateFormat", _localDateFormat);
        inputMap.put("allowKeyableDates", _allowKeyableDates);

        field.put("settings", settings);
        field.put("field_value", attributeValue);
        field.put("hasAccess", "true");
        //field.put("expression_businessobject",attrSym);
//      field.put("field_display_value", attributeValue);
        field.put("label", i18nNow.getAttributeI18NString(attributeName, _lang));
        field.put("StringResourceFileId", (String)_requestMap.get("StringResourceFileId"));
        field.put("suiteDirectory", (String)_requestMap.get("SuiteDirectory"));
        field.put("suiteKey", (String)_requestMap.get("suiteKey"));

        String inputName;
        Integer redundancyCount = (Integer)_redundancyCounts.get(attributeName);

        if (redundancyCount == null) {
            redundancyCount = new Integer(1);
        } else {
            redundancyCount = new Integer(redundancyCount.intValue() + 1);
            _totalRedundancyCount++;
        }
        _redundancyCounts.put(attributeName, redundancyCount);

        inputName = "#" + redundancyCount.intValue() + "," + attributeName;
        field.put("name", inputName);

        boolean isReadOnly = false;
        String editable = (String)_settingMap.get("Editable");
        if(editable != null && !"null".equals(editable)) {
            if("false".equalsIgnoreCase(editable)) {
                isReadOnly = true;
            }
        }


        buf.append("<tr>");
        String uifOut = _uif.drawFormEditElement(_context, _requestMap, field,
                inputMap,
                _timeZone,
                true, // drawLabel
                _maxCol,
                -1, // fieldCounter
                isReadOnly);
        buf.append(uifOut);
        buf.append("</tr>");
        return buf.toString();
    }

     /**
     * updateProcessAttributes - Updates process attributes
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns String
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     */

    public void updateWorkflowTaskAttributes(Context context, String[] args)
        throws Exception
    {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap   = (HashMap) programMap.get("paramMap");
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        String  objectId   = (String) paramMap.get("objectId");

        try
        {

            String relWorkflowTask       = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
            Pattern relPattern        = new Pattern("");
            relPattern.addPattern(relWorkflowTask);

            DomainObject taskObject = DomainObject.newInstance(context, objectId);
            String activityName = taskObject.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Activity"));

            SelectList typeSelects = new SelectList(2);
            typeSelects.add(DomainObject.SELECT_ID);
            typeSelects.add(DomainObject.SELECT_NAME);


            StringList relSelects = new StringList(1);
            relSelects.add(DomainObject.SELECT_RELATIONSHIP_ID);
            MapList workflowList = taskObject.getRelatedObjects(context,
                                                          relPattern.getPattern(),
                                                          "*",
                                                          typeSelects,
                                                          relSelects,
                                                          true,
                                                          false,
                                                          (short)0,
                                                          null, //objectWhere
                                                          null, //relWhere,
                                                          null,
                                                          null,
                                                          null);

            String tempId ="";
            Iterator mapItr = workflowList.iterator();
            while (mapItr.hasNext() )
            {
               Map item = (Map)mapItr.next();
               tempId = (String)item.get(DomainObject.SELECT_ID);
            }

            setId(tempId);
            String result = getInfo(context,strProcess);
            String workflowName = result;
            String cmd = "print workflow \""+result+"\""+ " "+"\""+getName()+"\""+ " select interactive["  + activityName+"].attribute[].value";

            HashMap processMap = processMql(context,cmd);
            result = (String)processMap.get("result");
            StringBuffer mQLAttrClass = new StringBuffer();
            // Tested by assigning the mql output is like below code
            if(result!=null && !"".equals(result)) {
                StringTokenizer st = new StringTokenizer(result,"\n");

                while(st.hasMoreTokens()) {
                    String token = (String)st.nextToken();
                    if(token.indexOf("attribute[")!=-1) {
                    String attrName  = token.substring(token.indexOf("attribute[")+10,token.indexOf("].value"));
                    String attrValue = extractVal(requestMap.get("#1,"+attrName));
                    mQLAttrClass.append(" interactive ");
                    mQLAttrClass.append("\"");
                    mQLAttrClass.append(activityName);
                    mQLAttrClass.append("\"");
                    mQLAttrClass.append(" ");
                    mQLAttrClass.append(" \"");
                    mQLAttrClass.append(attrName);
                    mQLAttrClass.append("\"");
                    mQLAttrClass.append(" ");
                    mQLAttrClass.append("\"");
                    mQLAttrClass.append(attrValue);
                    mQLAttrClass.append("\"");
                    }
                } //end of while loop
            }




            String strCmd = "modify workflow \""+ workflowName+"\""+" "+"\""+getName()+"\"" + mQLAttrClass.toString();
            MQLCommand mql = new MQLCommand();
            mql.executeCommand(context, strCmd);

        }
        catch (Exception ex)
        {
            throw ex;
        }
    }

    private static String extractVal(Object valObj) {
        String[] strArr = {};
        if (valObj !=null && valObj.getClass() == strArr.getClass()) {
            return ((String[])valObj)[0];
        } else if (valObj !=null && valObj.getClass() == String.class) {
            return (String)valObj;
        } else {
            return "";
        }
    }

    private static void performPostCompleteActions(Context context, String taskId) throws Exception
    {
        //Create DomainObject with taskId
        DomainObject dObj = new DomainObject(taskId);
        //Get the workflow object id for which the task is connected to
        String relWorkflowTask       = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
        Pattern relPattern        = new Pattern("");
        relPattern.addPattern(relWorkflowTask);

        SelectList typeSelects = new SelectList(1);
        typeSelects.add(DomainObject.SELECT_ID);
        StringList relSelects = new StringList();

        MapList workflowList = dObj.getRelatedObjects(context,
                                                      relPattern.getPattern(),
                                                      "*",
                                                      typeSelects,
                                                      relSelects,
                                                      true,
                                                      false,
                                                      (short)1,
                                                      null, //objectWhere
                                                      null, //relWhere,
                                                      null,
                                                      null,
                                                      null);

        String tempId ="";
        Iterator mapItr = workflowList.iterator();
        while (mapItr.hasNext() )
        {
           Map item = (Map)mapItr.next();
           tempId = (String)item.get(DomainObject.SELECT_ID);
           emxWorkflow_mxJPO.acceptTasks(context, tempId);
           emxWorkflow_mxJPO.acceptTasksForSubProcess(context, tempId);
        }
    }
     /**
     * This function notifies the owner that a new Deliverable has been
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args OBJECTID
     * @throws Exception if operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */
    public void notifyOwnerofNewDeliverables(Context context, String[] args)
        throws Exception
    {
        /** The parent owner of the task. */
        String SELECT_PARENT_OWNER = "to[" + _workflow_task_relationship + "].from.owner";

        try
        {
           // get values from args, get the Workflow Task ID
           String workflowTaskId = args[0];

           // get the "Workflow Task" name and the Workflow owner
           setId(workflowTaskId);
           StringList busSelects = new StringList(3);
           busSelects.add(SELECT_NAME);
           busSelects.add(SELECT_TYPE);
           busSelects.add(SELECT_PARENT_OWNER);
           Map objMap = getInfo(context, busSelects);
           String workflowTaskName = (String) objMap.get(SELECT_NAME);
           String workflowTaskType = (String) objMap.get(SELECT_TYPE);
           String workflowOwner = (String) objMap.get(SELECT_PARENT_OWNER);

           // Send notification if we are connecting to Workflow Task object
           if (workflowTaskType.equals(_workflowTask_type))
           {
              // setup the To lists
              StringList toList = new StringList(1);
              toList.addElement(workflowOwner);

              // setup the id list
              StringList objectIdList = new StringList();
              objectIdList.add(workflowTaskId);

              // setup message key and value pairs
              String[] messageKeys = {"WorkflowTaskName"};
              String[] messageValues = {workflowTaskName};

              MailUtil.sendNotification(context,
                      toList,        // To List
                      null,          // Cc List
                      null,          // Bcc List
                      "emxComponents.Workflow.NewDeliverableSubject",  // Subject key
                      null,                                            // Subject keys
                      null,                                            // Subject values
                      "emxComponents.Workflow.NewDeliverableMessage",  // Message key
                      messageKeys,   // Message keys
                      messageValues, // Message values
                      objectIdList,  // Object list
                      null,          // company name
                      "emxComponentsStringResource");                  // Property file
           }
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }

    @com.matrixone.apps.framework.ui.PostProcessCallable
    public HashMap notifyAssigneeOfModification(Context context, String[] args) throws Exception
    {
        HashMap returnMap = new HashMap();
        try
        {
            HashMap map = (HashMap)JPO.unpackArgs(args);
            HashMap requestMap = (HashMap)map.get("requestMap");
            String objectId = (String)requestMap.get("objectId");
            DomainObject taskObj = new DomainObject(objectId);

            StringList toList = new StringList(1);
            toList.addElement(taskObj.getInfo(context, "owner"));
            StringList objectIdList = new StringList();
            objectIdList.add(objectId);

            String[] messageKeys = {"WorkflowTaskType", "WorkflowTaskName"};
            String[] messageValues = {PropertyUtil.getSchemaProperty(context, "type_WorkflowTask"), taskObj.getInfo(context, "name")};

            MailUtil.sendNotification(context,
                      toList,        // To List
                      null,          // Cc List
                      null,          // Bcc List
                      "emxComponents.Workflow.WorkflowTaskModifiedSubject",  // Subject key
                      null,                                            // Subject keys
                      null,                                            // Subject values
                      "emxComponents.Workflow.WorkflowTaskModifiedMessage",  // Message key
                      messageKeys,   // Message keys
                      messageValues, // Message values
                      objectIdList,  // Object list
                      null,          // company name
                      "emxComponentsStringResource");


        }
        catch(Exception ex)
        {
            returnMap.put("Message", ex.getMessage());
        }

        return returnMap;
    }
        

}
