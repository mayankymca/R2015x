/*
**  emxWorkflowEngineBase.java
**
**  Copyright (c) 1992-2015 Dassault Systemes.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of MatrixOne,
**  Inc.  Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**
*/

import java.util.*;

import matrix.db.*;
import matrix.util.*;

import java.text.SimpleDateFormat;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.common.Workflow;
import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;




/**
 * The <code>emxWorkflowEngineBase</code> class contains methods for document.
 *
 *
 */

public class emxWorkflowEngineBase_mxJPO
{
    //Constants
    static final String workflowType = PropertyUtil.getSchemaProperty("type_Workflow");
    static final String workflowTaskType = PropertyUtil.getSchemaProperty("type_WorkflowTask");
    static final String workflowPolicy = PropertyUtil.getSchemaProperty("policy_Workflow");
    static final String workflowTaskPolicy = PropertyUtil.getSchemaProperty("policy_WorkflowTask");

    //Instance Variables
    String _processType;
    String _processName;
    String _vault;
    Workflow _workflow;
    String _parentProcessName = null;
    String _parentProcessType = null;

    //Constructor
    public emxWorkflowEngineBase_mxJPO (Context context, String[] args)throws Exception
    {
        if (args.length < 3) {
            return;
        }
        _processType = args[0];
        _processName = args[1];
        _vault = args[2];

        if(args.length == 5)
        {
            _parentProcessType = args[3];
            _parentProcessName = args[4];
        }


        StringList selects = new StringList();
        selects.add(DomainObject.SELECT_ID);

        MapList resList = DomainObject.findObjects(context, workflowType, _processName, "*", "*", "*", null, false, selects);

        /*
        String command = "print bus \"" + workflowType + "\" \"" + _processName + "\" * select id dump";
        HashMap resMap = processMql(context, command);
        */
        String command = "";
        String result = null;
        String objectId = null;
        if(resList != null && resList.size() > 0){
            HashMap mapTemp = (HashMap)resList.get(0);
            if(mapTemp != null) {
                objectId = (String)mapTemp.get(DomainObject.SELECT_ID);
            }

        }

        if(objectId == null)
        {
            //Create new Workflow object;
            BusinessObject bo = new BusinessObject (workflowType, _processName, "-", context.getVault().getName());
            bo.create(context, workflowPolicy);
            String owner = MqlUtil.mqlCommand(context, "print workflow '"+_processType+"' '"+_processName+"' select owner dump |").trim();
            bo.open (context);
            bo.setOwner (context, owner);

            _workflow = new Workflow(bo);
            _workflow.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process"), _processType);

            //Check this is a sub process,
            //if it is, get its parent and connect with relationship Workflow Sub Process
            if(_parentProcessName != null && !_parentProcessName.equals(""))
            {
                command = "print bus \"" + workflowType + "\" \"" + _parentProcessName + "\" \"-\" select id dump";
                HashMap map = processMql(context, command);
                result = ((String)map.get("result")).trim();
                if(result != null)
                {
                    DomainObject obj = DomainObject.newInstance(context, result);
                    if(obj != null)
                    {
                        String wfOwner = obj.getInfo(context, "owner");
                        String wfDueDate = obj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_DueDate"));
                        _workflow.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_DueDate"), wfDueDate);
                        _workflow.setOwner(context, wfOwner);
                        ContextUtil.pushContext(context, wfOwner, null, null);
                        obj.addRelatedObject(context,
                                         new RelationshipType(PropertyUtil.getSchemaProperty(context, "relationship_WorkflowSubProcess")),
                                         false, _workflow.getInfo(context, "id"));
                        ContextUtil.popContext(context);
                    }

                }
            }
        } else
        {
            _workflow = new Workflow(objectId);
        }

    }

    public void notifyWorkflow(Context context, String[] args) throws Exception
    {
        String event = args[0];
        boolean isPushed = false;

        try
        {
            String wfOwner = _workflow.getInfo(context, "owner");
            if(!wfOwner.equals(context.getUser()))
            {
                ContextUtil.pushContext(context, wfOwner, null, null);
                isPushed = true;
            }

            //event = start
            //Promote associated Business object to Started state.

            if(event.equalsIgnoreCase("start"))
            {

                    _workflow.promote(context);

                    // Delete all tasks and sub processes in case of restarting.
                    // delete all tasks associated with this business object
                    deleteTasks(context, _workflow.getId());

                    //Get all sub processes connect to this business object and delete them.
                    StringList list = new StringList(1);
                    list.add(DomainObject.SELECT_ID);
                    MapList mList = _workflow.getRelatedObjects(context,PropertyUtil.getSchemaProperty(context, "relationship_WorkflowSubProcess"),
                                                                PropertyUtil.getSchemaProperty(context,"type_Workflow"),
                                                                list, new StringList(), false, true, (short)0, null, null);
                    Iterator itr = mList.iterator();
                    DomainObject obj;
                    while(itr.hasNext())
                    {
                        Map map = (Map)itr.next();
                        obj = new DomainObject((String)map.get(DomainObject.SELECT_ID));
                        deleteTasks(context, obj.getId());
                        obj.deleteObject(context);

                    }

            }

            //event = stop
            //Demote associated object to Definied state
            //Delete all tasks connected to the Business object.
            else if(event.equalsIgnoreCase("stop"))
            {

                    _workflow.demote(context);
                    //Send notification to all task active task owners
                    //TO-DO
                    // Get all active task owners
                    StringList selects = new StringList();
                    selects.add(DomainObject.SELECT_OWNER);
                    MapList mList = _workflow.getRelatedObjects(context, PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask"),
                                                                PropertyUtil.getSchemaProperty(context,"type_WorkflowTask"),
                                                                selects, new StringList(), false, true, (short)0, "current == 'Assigned'", null);
                    Iterator itr = mList.iterator();
                    StringList toList = new StringList();
                    Map map = null;
                    
                    String workflowOwner = (String) _workflow.getOwner().getName();
                    toList.add(workflowOwner);
                    
                    while(itr.hasNext())
                    {
                        map = (Map)itr.next();
                        toList.add((String)map.get(DomainObject.SELECT_OWNER));
                    }
                    StringList objectIdList = new StringList();
                    objectIdList.add(_workflow.getId());

                    MailUtil.sendNotification(context,
                            toList,   // To List
                            null,     // Cc List
                            null,     // Bcc List
                            "emxComponents.Workflow.WorkflowStopedSubject",  // Subject key
                            null,                                       // Subject keys
                            null,                                       // Subject values
                            "emxComponents.Workflow.WorkflowStopedMessage",  // Message key
                            null,         // Message keys
                            null,         // Message values
                            objectIdList, // Object list
                            null,         // company name
                            "emxComponentsStringResource");     // Property file

                    //TO_DO


            }
            else if(event.equalsIgnoreCase("suspend"))
            {


                _workflow.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_BranchTo"), "Suspended");
                _workflow.promote(context);
                    //Send notification to all task active task owners
                    //TO-DO
    //              Get all active task owners
                    StringList selects = new StringList();
                    selects.add(DomainObject.SELECT_OWNER);
                    MapList mList = _workflow.getRelatedObjects(context, PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask"),
                                                                PropertyUtil.getSchemaProperty(context,"type_WorkflowTask"),
                                                                selects, new StringList(), false, true, (short)0, "current == 'Assigned'", null);
                    Iterator itr = mList.iterator();
                    StringList toList = new StringList();
                    Map map = null;
                    while(itr.hasNext())
                    {
                        map = (Map)itr.next();
                        toList.add((String)map.get(DomainObject.SELECT_OWNER));
                    }
                    StringList objectIdList = new StringList();
                    objectIdList.add(_workflow.getId());

                    MailUtil.sendNotification(context,
                            toList,   // To List
                            null,     // Cc List
                            null,     // Bcc List
                            "emxComponents.Workflow.WorkflowSuspendSubject",  // Subject key
                            null,                                       // Subject keys
                            null,                                       // Subject values
                            "emxComponents.Workflow.WorkflowSuspendedMessage",  // Message key
                            null,         // Message keys
                            null,         // Message values
                            objectIdList, // Object list
                            null,         // company name
                            "emxComponentsStringResource");     // Property file
                    //TO_DO
                    //TO_DO


            }
            else if(event.equalsIgnoreCase("resume"))
            {


                    _workflow.demote(context);
                    //Send notification to all task active task owners
                    //TO-DO
                    //Get all active task owners
                    StringList selects = new StringList();
                    selects.add(DomainObject.SELECT_OWNER);
                    MapList mList = _workflow.getRelatedObjects(context, PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask"),
                                                                        PropertyUtil.getSchemaProperty(context,"type_WorkflowTask"),
                                                                        selects, new StringList(), false, true, (short)0, "current == 'Assigned'", null);
                    Iterator itr = mList.iterator();
                    StringList toList = new StringList();
                    Map map = null;
                    while(itr.hasNext())
                    {
                        map = (Map)itr.next();
                        toList.add((String)map.get(DomainObject.SELECT_OWNER));
                    }
                    StringList objectIdList = new StringList();
                    objectIdList.add(_workflow.getId());

                    MailUtil.sendNotification(context,
                            toList,   // To List
                            null,     // Cc List
                            null,     // Bcc List
                            "emxComponents.Workflow.WorkflowResumeSubject",  // Subject key
                            null,                                       // Subject keys
                            null,                                       // Subject values
                            "emxComponents.Workflow.WorkflowResumeMessage",  // Message key
                            null,         // Message keys
                            null,         // Message values
                            objectIdList, // Object list
                            null,         // company name
                            "emxComponentsStringResource");     // Property file
                    //TO_DO


            }
            else if(event.equalsIgnoreCase("finish"))
            {
// modified by Yue Li for #330727    
//                SimpleDateFormat sdf = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat());
                SimpleDateFormat sdf = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat(),Locale.US);
                String formatedDate = sdf.format(new Date());

                _workflow.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_BranchTo"), "Completed");
                _workflow.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_ActualCompletionDate"), formatedDate);
                _workflow.promote(context);
                //TO-DO
                //Send notification to workflow owner.
                //TO-DO
    //
                StringList toList = new StringList();
                toList.add(_workflow.getOwner(context).getName());
                StringList objectIdList = new StringList();
                objectIdList.add(_workflow.getId());

                MailUtil.sendNotification(context,
                        toList,   // To List
                        null,     // Cc List
                        null,     // Bcc List
                        "emxComponents.Workflow.WorkflowCompleteSubject",  // Subject key
                        null,                                       // Subject keys
                        null,                                       // Subject values
                        "emxComponents.Workflow.WorkflowCompleteMessage",  // Message key
                        null,         // Message keys
                        null,         // Message values
                        objectIdList, // Object list
                        null,         // company name
                        "emxComponentsStringResource");     // Property file
                //TO_DO
				promoteConnectedObjects(context);

            }

        }
        finally
        {
            if(isPushed)
            {
                ContextUtil.popContext(context);
            }
        }


    }


    public void deliver(Context context, String[] args) throws Exception
    {


        if(args.length == 0)
        {
            throw new Exception("Invalid arguments");
        }

        boolean isPushed = false;
        try
        {
            DomainObject taskObj = null;
            //Get the parameters.
            String nodeId = args[0];
            String taskName = args[1];
            String workflowOwner = args[2];
            String instructions = args[3];
            String dueDate = args[4];
            String priority = args[5];
            if(!workflowOwner.equals(context.getUser()))
            {
                ContextUtil.pushContext(context, workflowOwner, null, null);
                isPushed = true;
            }
            StringList assignees = new StringList();
            for(int i = 6; i < args.length; i++)
            {
                assignees.add(args[i]);
            }

            String activity = nodeId.substring( (nodeId.indexOf("\"")+1), nodeId.lastIndexOf("\""));


            //Check if object exists, if not Create new object and Connect it to Workflow object with Workflow Task relationship
            /*String command = "print bus \"" + workflowTaskType + "\" \"" + taskName + "\" '-' select id dump";
             HashMap resMap = processMql(context, command);
            */
            StringList selects = new StringList();
            selects.add(DomainObject.SELECT_ID);

            MapList resList = DomainObject.findObjects(context, workflowTaskType, taskName, "*", "*", "*", null, false, selects);

            String objectId = null;
            if(resList != null && resList.size() > 0){
                HashMap mapTemp = (HashMap)resList.get(0);
                if(mapTemp != null) {
                    objectId = (String)mapTemp.get(DomainObject.SELECT_ID);
                }
            }

            if(objectId == null)
            {

                taskObj = new DomainObject();
                taskObj.createObject(context, workflowTaskType, taskName, "-", workflowTaskPolicy, context.getVault().getName());
                taskObj.setOwner(context, workflowOwner);


                taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context,"attribute_Instructions"), instructions);
                taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context,"attribute_DueDate"), dueDate);
                taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context,"attribute_Activity"), activity);
                              

                String taskId = taskObj.getId();
                _workflow.addRelatedObject(context, new RelationshipType(PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask")), false, taskObj.getId());

                //Bug-318946 - Start - Fixed description issue.
                int intPriority = Integer.parseInt(priority.trim());
                if( intPriority == 0 ){
                	priority = "Urgent";
                } else if( intPriority == 1) {
                    priority = "High";
                } else if( intPriority == 2) {
                    priority = "Medium";
                } else if( intPriority == 3) {
                    priority = "Low";
                }
                taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context,"attribute_Priority"), priority);
                String relWorkflowTask = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
                String attrProcess = PropertyUtil.getSchemaProperty(context,"attribute_Process");

                String tempProcessName = taskObj.getInfo(context,"to["+relWorkflowTask+"].from.attribute["+attrProcess+"].value");
                //Modified to get process task assignee with description in 1 call....print process "testPS2" select interactive[task1].description interactive
                //[task1].assignee dump |;
                String command = "print process \"" + tempProcessName + "\" select interactive["+activity+"].description interactive["+activity+"].assignee dump |";
                HashMap mapTemp = processMql(context, command);
                String result = ((String)mapTemp.get("result")).trim();
                if(result != null && !"".equals(result)) {
                String wfTaskDescription=result.substring(0,result.indexOf("|"));
                String wfTaskOriginator=result.substring(result.indexOf("|")+1,result.length());
                if(wfTaskDescription != null && !"".equals(wfTaskDescription)) {
                    taskObj.setDescription(context, wfTaskDescription);
                }
                if(wfTaskOriginator != null && !"".equals(wfTaskOriginator)) {
                	taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context,"attribute_Originator"), wfTaskOriginator);
                }else{
                	// to be checked if the task owner to be moved to context user if not assigned.
                	//taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context,"attribute_Originator"), context.getUser());
                }
                }
                //Bug-318946 - End

            }
            else
            {
				try{
				//Added for the Bug No:332190  Begin
				if(!isPushed){
				ContextUtil.pushContext(context);
					isPushed = true;
				}
                // Task object already exists
                taskObj = DomainObject.newInstance(context, objectId);
                // Reset it to initial state
                taskObj.setState(context, "Started");
                // Restore ownership of task to workflow owner
                taskObj.setOwner(context, workflowOwner);
                taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context,"attribute_ActualCompletionDate"), "");
                dueDate = taskObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context,"attribute_DueDate"));
                // Disconnect any assignee
                StringList relSelect = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
                Map objectMap = taskObj.getRelatedObject(context,
                                PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTaskAssignee"),
                                true,
                                null,
                                relSelect);
                if (objectMap != null) {
                    String strRelId =(String) objectMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                    DomainRelationship.disconnect(context, strRelId);
                }
				}
				catch (Exception ex) {
					ContextUtil.popContext(context);
				}

				//Added for the Bug No:332190  End
            }

    //      If only one assignee, accept the activity, by changing the context to the assignee and promote workflow task
            //object to accepted state
            if(assignees.size() == 1)
            {

                String personId = PersonUtil.getPersonObjectID(context, (String)assignees.get(0));
                //taskObj.addToObject(context, new RelationshipType(PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTaskAssignee")), personId);


                taskObj.setOwner(context, (String)assignees.get(0));


                StringList toList = new StringList();
                toList.add((String)assignees.get(0));

                StringList objectIdList = new StringList();
                objectIdList.add(taskObj.getId());
				if(isPushed)
                {
                    ContextUtil.popContext(context);
                    isPushed = false;
                }

                String messageKeys[] = {"WorkflowTaskType", "WorkflowTaskName", "WorkflowType", "WorkflowName", "WorkflowOwner", "TaskInstructions", "TaskDueDate"};
                String messageValues[] = {PropertyUtil.getSchemaProperty(context, "type_WorkflowTask"),
                                            taskObj.getInfo(context, "name"),
                                            PropertyUtil.getSchemaProperty(context, "type_Workflow"),
                                            _workflow.getInfo(context, "name"),
                                            _workflow.getInfo(context, "owner"),
                                            instructions,
                                            dueDate};

                MailUtil.sendNotification(context,
                        toList,   // To List
                        null,     // Cc List
                        null,     // Bcc List
                        "emxComponents.Workflow.WorkflowTaskAssignedSubject",  // Subject key
                        null,                                       // Subject keys
                        null,                                       // Subject values
                        "emxComponents.Workflow.WorkflowTaskAssignedMessage",  // Message key
                        messageKeys,         // Message keys
                        messageValues,         // Message values
                        objectIdList, // Object list
                        null,         // company name
                        "emxComponentsStringResource");     // Property file

            }
            else
            {
                //Bug No 318930 start
                /*
                The below code executes only when
                a) Assignee of an activity is Role, Group having more than one user
                1. read the process name from workflow
                2. Got the assignee of activity
                3. set the assignee as owner
                */
                String processName = _workflow.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process"));
                String assignee = MqlUtil.mqlCommand(context, "print process '" + processName + "' select interactive[" + activity + "].assignee dump ~");
				String userType = null;
				String assigneeType = "Person";

                if (assignee != null && !"".equals(assignee))
                {
                    taskObj.setOwner(context, assignee);
					userType = MqlUtil.mqlCommand(context, "print user '" + assignee + "' select isarole dump ~");
                    if (userType != null && userType.equals("TRUE"))
                    {
                       assigneeType = EnoviaResourceBundle.getProperty(context,"emxComponentsStringResource", new Locale(context.getSession().getLanguage()),"emxComponents.Common.Role");
                    }
                    else
                    {
                       userType = MqlUtil.mqlCommand(context, "print user '" + assignee + "' select isagroup dump ~");
                       if (userType != null && userType.equals("TRUE"))
                       {
                          assigneeType = EnoviaResourceBundle.getProperty(context,"emxComponentsStringResource", new Locale(context.getSession().getLanguage()),"emxComponents.Common.Group");
                       }
                    }
                }
				if(isPushed)
                {
                    ContextUtil.popContext(context);
                    isPushed = false;
                }

                // figure out which message to send
                String subjectString = "emxComponents.Workflow.WorkflowTaskAssignedSubject";
                String messageString = "emxComponents.Workflow.WorkflowTaskAssignedMessage";
                if (assigneeType.equals("Role") || assigneeType.equals("Group"))
                {
                   subjectString = "emxComponents.Workflow.WorkflowTaskAssignedSubject.RoleOrGroup";
                   messageString = "emxComponents.Workflow.WorkflowTaskAssignedMessage.RoleOrGroup";
                }

                // send notifications to task assignee(s).
                StringList toList = new StringList();
                for(Iterator itr = assignees.iterator(); itr.hasNext();)
                {
                    toList.add((String)itr.next());
                }
                StringList objectIdList = new StringList();
                objectIdList.add(taskObj.getId());

				String subjectKeys[] = {"UserName", "UserType"};
				String subjectValues[] = {assignee,	assigneeType};
				String messageKeys[] = {"WorkflowTaskType", "WorkflowTaskName", "WorkflowType", "WorkflowName", "WorkflowOwner", "TaskInstructions", "TaskDueDate", "UserName", "UserType"};
				String messageValues[] = {PropertyUtil.getSchemaProperty(context, "type_WorkflowTask"),
											taskObj.getInfo(context, "name"),
											PropertyUtil.getSchemaProperty(context, "type_Workflow"),
											_workflow.getInfo(context, "name"),
											_workflow.getInfo(context, "owner"),
											instructions,
											dueDate,
											assignee,
											assigneeType};
                MailUtil.sendNotification(context,
                        toList,   // To List
                        null,     // Cc List
                        null,     // Bcc List
                        subjectString,  // Subject key
                        subjectKeys,                                       // Subject keys
                        subjectValues,                                       // Subject values
                        messageString,  // Message key
                        messageKeys,         // Message keys
                        messageValues,         // Message values
                        objectIdList, // Object list
                        null,         // company name
                        "emxComponentsStringResource");     // Property file
            }
            
        }
        catch(Exception ex)
        {
            if(isPushed)
            {
                ContextUtil.popContext(context);
            }
            throw ex;
        }


    }

    public void resolve(Context context, String[] args) throws Exception
    {
        String nodeId = args[0];
        String assignee = args[1];
        String activity = nodeId.substring( (nodeId.indexOf("\"")+1), nodeId.lastIndexOf("\""));

        //Get the task object, with the node id


        StringList selects = new StringList();
        selects.add(DomainObject.SELECT_ID);

        MapList resList = DomainObject.findObjects(context, workflowTaskType, _processName + " " + activity , "*", "*", "*", null, false, selects);

        String objectId = null;
        if(resList != null && resList.size() > 0){
            HashMap mapTemp = (HashMap)resList.get(0);
            if(mapTemp != null) {
                objectId = (String)mapTemp.get(DomainObject.SELECT_ID);
            }

        }
        DomainObject taskObj = DomainObject.newInstance(context, objectId);
        //push context promote to assigned state
        boolean contextPushed = false;
        try
        {
            ContextUtil.pushContext(context);
            contextPushed = true;

            //change owner ship to the context user
            taskObj.setOwner(context, assignee);

            String personId = PersonUtil.getPersonObjectID(context, assignee);

            taskObj.addToObject(context, new RelationshipType(PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTaskAssignee")), personId);

            ContextUtil.popContext(context);
            contextPushed = false;
            //send notification to workflow owner.
            StringList toList = new StringList();
            toList.add(_workflow.getOwner(context).getName());

            StringList objectIdList = new StringList();
            objectIdList.add(taskObj.getId());
            String sInstructions = taskObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context,"attribute_Instructions"));
            if (sInstructions == null)
            {
                sInstructions = "";
            }
            String sDueDate = taskObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context,"attribute_DueDate"));
            if (sDueDate == null)
            {
                sDueDate = "";
            }
            String messageKeys[] = {"WorkflowTaskType","User"};
            String messageValues[] = {PropertyUtil.getSchemaProperty(context, "type_WorkflowTask"),
                                        assignee};

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

            //pop the context
        }
        catch(Exception ex)
        {
            throw ex;
        }
        finally
        {
            if(contextPushed)
            ContextUtil.popContext(context);
        }
    }

    public void rescind(Context context, String[] args) throws Exception
    {
        String nodeId = args[0];
    }


    private void deleteTasks(Context context, String objectId) throws Exception
    {
        DomainObject obj = new DomainObject(objectId);
        StringList list = new StringList(1);
        list.add(DomainObject.SELECT_ID);
        MapList mList = obj.getRelatedObjects(context,PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask"),
                                                PropertyUtil.getSchemaProperty(context,"type_WorkflowTask"),
                                                list, new StringList(), false, true, (short)1, null, null);
        Iterator itr = mList.iterator();
        Map map;
       try
        {
	        ContextUtil.pushContext(context);
	        while(itr.hasNext())
	        {
	            map = (Map)itr.next();
	            DomainObject taskObj = new DomainObject((String)map.get(DomainObject.SELECT_ID));
	            taskObj.deleteObject(context);
	        }
        }
        catch(Exception ex)
        {
        	throw ex;
        }
        finally
        {
        	ContextUtil.popContext(context);
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

    private void promoteConnectedObjects(Context context) throws Exception
    {

        try
        {

            ContextUtil.pushContext(context);
            // verify the promote connected object attribute , if true get the
            // connected objects and promote them
            String promoteConnected = _workflow.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_PromoteConnected"));
            if(promoteConnected != null && promoteConnected.equalsIgnoreCase("true"))
            {
                String sRelWorkflowContent = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowContent");
                StringList objectSelects = new StringList(2);
                StringList relSelects = new StringList();
                objectSelects.add(DomainObject.SELECT_ID);
                objectSelects.add("current.satisfied");

                Pattern relPattern        = new Pattern("");
                relPattern.addPattern(sRelWorkflowContent);

                MapList mList = _workflow.getRelatedObjects(context,
                         relPattern.getPattern(),
                         "*",
                         objectSelects,
                         relSelects,
                         false,
                         true,
                         (short)1,
                          null, //objectWhere
                         null, //relWhere,
                          null,
                         null,
                         null);
                if(mList != null && mList.size() > 0)
                {
                    Iterator itr = mList.iterator();
                    while(itr.hasNext())
                    {
                        Map m = (Map)itr.next();
                        DomainObject dObj = new DomainObject((String)m.get(DomainObject.SELECT_ID));
                        boolean canPromote = false;
                        String satisfied = (String)m.get("current.satisfied");
                        if(satisfied != null && satisfied.length() > 0)
                        {
                            if(satisfied.equalsIgnoreCase("true"))
                            {
                                canPromote = true;
                            }
                        }
                        if(canPromote)
                        {
                            dObj.promote(context);
                        }
                    }
                }
            }
        }
        finally
        {
            ContextUtil.popContext(context);
        }

    }
}
