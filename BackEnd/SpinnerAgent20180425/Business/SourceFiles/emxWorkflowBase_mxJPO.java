/*
**  emxWorkflowBase
**
**  Copyright (c) 1992-2015 Dassault Systemes.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of MatrixOne,
**  Inc.  Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessType;
import matrix.db.BusinessTypeList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.RelationshipType;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.SelectList;
import matrix.util.StringList;

import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.common.Company;
import com.matrixone.apps.common.Document;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIForm;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.jsystem.util.StringUtils;

/**
 * The <code>emxWorkflowBase</code> class contains methods for document.
 *
 *
 */

public class emxWorkflowBase_mxJPO extends emxDomainObject_mxJPO
{

    private static final String _workflow_type = PropertyUtil.getSchemaProperty("type_Workflow");
    private static final String _workflow_policy = PropertyUtil.getSchemaProperty("policy_Workflow");
    private static final String _workflow_content_relationship = PropertyUtil.getSchemaProperty("relationship_WorkflowContent");


    private static final String NODE_DEF_START = "<nodeDef>";
    private static final String NODE_DEF_END = "</nodeDef>";
    private static final String LINK_DEF_START = "<linkDef>";
    private static final String LINK_DEF_END = "</linkDef>";
    private static final String NODE_DEF_LIST_START = "<nodeDefList";
    private static final String NODE_DEF_LIST_END = "</nodeDefList>";
    private static final String LINK_DEF_LIST_START = "<linkDefList";
    private static final String LINK_DEF_LIST_END = "</linkDefList>";
    private static final String DTD_INFO_START = "<dtdInfo>";
    private static final String DTD_INFO_END = "</dtdInfo>";
    private static final String NODE_TYPE_START = "<nodeType>";
    private static final String NODE_TYPE_END = "</nodeType>";
    private static final String NODE_NAME_START = "<name>";
    private static final String NODE_NAME_END = "</name>";
    private static final String NODE_ASSIGNEE_END = "</assignee>";
    private static final String NODE_ASSIGNEE_START = "</assignee>";

    private int minXLocation = 0;
    private int maxXLocation = 0;
    private int minYLocation = 0;
    private int maxYLocation = 0;

    private static final String sAttrDueDate              = PropertyUtil.getSchemaProperty("attribute_DueDate");
    private static final String sAttrActualCompletedDate  = PropertyUtil.getSchemaProperty("attribute_ActualCompletionDate");
    private static final String sProcess                  = PropertyUtil.getSchemaProperty("attribute_Process");

    private static final String strAttrDueDate            ="attribute["+sAttrDueDate+"]";
    private static final String strAttrTaskCompletionDate ="attribute["+sAttrActualCompletedDate+"]";
    private static final String strProcess                ="attribute["+sProcess+"]";

    private static final String policyWorkflowTask = PropertyUtil.getSchemaProperty("policy_WorkflowTask");

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

    public emxWorkflowBase_mxJPO(Context context, String[] args)
      throws Exception
    {
        super(context, args);
    }

    public Map create(Context context, String[] args) throws Exception
    {
        HashMap returnMap = null;
        try
        {
            ContextUtil.startTransaction(context, true);
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            if(paramMap != null) {
                String name = (String)paramMap.get("name");
                String description = (String)paramMap.get("description");
                String dueDate = (String)paramMap.get("dueDate");
                String processName = (String)paramMap.get("processName");
                String processAutoStart = (String)paramMap.get("processAutoStart");
                boolean startWorkflow = ((Boolean)paramMap.get("startWorkflow")).booleanValue();
                String promoteObject = (String)paramMap.get("promoteObject");
                HashMap content = (HashMap)paramMap.get("content");
                returnMap = create(context, name, description, dueDate, processName, startWorkflow, promoteObject, processAutoStart, content);

            }
        }
        catch(Exception ex)
        {
            ContextUtil.abortTransaction(context);
            throw ex;
        }
        finally
        {
            ContextUtil.commitTransaction(context);
        }

        return returnMap;
    }

    private HashMap create(Context context,
                           String name,
                           String description,
                           String dueDate,
                           String processName,
                           boolean startWorkflow,
                           String promoteObject,
                           String processAutoStart,
                           HashMap content) throws Exception
    {
        HashMap returnMap = new HashMap();

        try
        {
            boolean bWorkflowExists = false;
            BusinessObject bObj = null;
            //Check workflow object exists with the name passed
            if(name != null){
                bObj = new BusinessObject(_workflow_type, name, "-", context.getVault().getName());
                bWorkflowExists = bObj.exists(context);
                if(bWorkflowExists) {
                    String sLanguage = context.getSession().getLanguage();
                    String sWfExists = EnoviaResourceBundle.getProperty(context,"emxComponentsStringResource",new Locale(sLanguage),"emxComponents.Workflow.Exists");
                    returnMap.put("error", sWfExists);
                    return returnMap;
                }
            }
            if(description == null)
            {
                description = "";
            }
            String processValidate = "validate process \"" + processName + "\"";
            MqlUtil.mqlCommand(context, processValidate, true);
            String user = context.getUser();
            String vault = context.getVault().getName();
            bObj = createBusObject(context, _workflow_type, name, "-", _workflow_policy, vault, user, description);
            DomainObject dObj = new DomainObject(bObj);
            dObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process"), processName);
            dObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_DueDate"), dueDate);
            dObj.setDescription(context, description);
            String cmd = "add workflow \"" + processName + "\" " + "\"" + dObj.getInfo(context, "name" ) + "\" description \"" + description + "\"";
            String result = MqlUtil.mqlCommand(context, cmd);

            if(content != null){
                if(promoteObject != null && "Yes".equalsIgnoreCase(promoteObject))
                {
                    promoteObject = "True";
                }
                else
                {
                    promoteObject = "False";
                }
                dObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_PromoteConnected"), promoteObject);
                String contentObjectState = "";
                java.util.Set values = content.keySet();
                Iterator itr = values.iterator();
                try
                {
                  ContextUtil.pushContext(context);

                    while(itr.hasNext())
                    {
                        String oId = (String)itr.next();
                        contentObjectState = (String)content.get(oId);
                        DomainRelationship rel = dObj.addRelatedObject(context, new RelationshipType(_workflow_content_relationship), false, oId);
                        Map attrMap = new HashMap();
                        attrMap.put(PropertyUtil.getSchemaProperty(context,"attribute_RouteBasePolicy"), new DomainObject(oId).getPolicy(context).getName());
                        if(contentObjectState != null && contentObjectState.length() > 0)
                        {
                            attrMap.put(PropertyUtil.getSchemaProperty(context, "attribute_RouteBaseState"), contentObjectState);
                        }
                        rel.setAttributeValues(context, attrMap);
                    }
                }
                catch (Exception ex)
                {
                  throw new FrameworkException(ex.getMessage());
                }
                finally
                {
                  ContextUtil.popContext(context);
                }
            }
            if(startWorkflow) {
                this.setId(dObj.getInfo(context, "id"));
                start(context, dObj.getName(), dObj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process")));
            }

            if(processAutoStart != null && processAutoStart.equalsIgnoreCase("true"))
            {
                acceptTasks(context, dObj.getId());
                acceptTasksForSubProcess(context, dObj.getId());
            }

            returnMap.put("objectId", dObj.getId());
        }
        catch(Exception ex)
        {
            throw ex;
        }

        return returnMap;
    }

    public void start(Context context, String[] args)throws Exception
    {
        //String name = getName();
        String name = getInfo(context, "name");
        String process = getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process"));
        start(context, name, process);
    }
    private void start(Context context, String name, String processName) throws Exception
    {


        StringBuffer sbuf = new StringBuffer();
        sbuf.append("start workflow \"");
        sbuf.append(processName);
        sbuf.append("\" \"");
        sbuf.append(name);
        sbuf.append("\"");
        HashMap map = processMql(context, sbuf.toString());
        String error = (String)map.get("error");
        if(error != null){
            throw new Exception(error);
        }
        acceptTasks(context, getInfo(context, "id"));
        acceptTasksForSubProcess(context, getInfo(context, "id"));
    }

    public void delete(Context context, String[] args)throws Exception
    {
        delete(context, this.getInfo(context, "name"), this.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process")));
        delete(context, this.getInfo(context, "id"));
        this.deleteObject(context);

    }

    private void delete(Context context, String objectId) throws Exception
    {
//      Get all tasks connected to the object and delete them.
        Pattern relPattern        = new Pattern("");
        String relWorkflowTask = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
        String relWorkflowSubProcess = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowSubProcess");
        String workflowType = PropertyUtil.getSchemaProperty(context, "type_Workflow");
        relPattern.addPattern(relWorkflowTask);
        relPattern.addPattern(relWorkflowSubProcess);
        StringList typeSelects = new StringList(3);
        typeSelects.add(DomainObject.SELECT_ID);
        typeSelects.add(DomainObject.SELECT_TYPE);
        typeSelects.add(DomainObject.SELECT_NAME);

        StringList relSelects = new StringList(1);
        DomainObject dObj = new DomainObject(objectId);

        MapList tasksList = dObj.getRelatedObjects(context,
                                                      relPattern.getPattern(),
                                                      "*",
                                                      typeSelects,
                                                      relSelects,
                                                      false,
                                                      true,
                                                      (short)0,
                                                      null, //null, //objectWhere
                                                      null, //relWhere,
                                                      null,
                                                      null,
                                                      null);
        if(tasksList != null && tasksList.size() > 0)
        {
            sendNotificationsForDeleteAction(context, this.getInfo(context, "id"));
            Map tmpMap = null;
            for(Iterator i = tasksList.iterator(); i.hasNext();)
            {
                 tmpMap = (Map)i.next();
                 if(tmpMap != null)
                 {
                     String oId = (String)tmpMap.get(DomainObject.SELECT_ID);
                     String sType = (String)tmpMap.get(DomainObject.SELECT_TYPE);
                     if(sType != null && sType.equalsIgnoreCase(workflowType)) {
                         sendNotificationsForDeleteAction(context, oId);
                     }
                 }
            }
            //Start Bug fix 318863.
            boolean isCtxPushed = false;
            try
            {

                ContextUtil.pushContext(context);
                isCtxPushed = true;
                Iterator itr = tasksList.iterator();

                while(itr.hasNext())
                {
                    tmpMap = (Map)itr.next();
                    if(tmpMap != null)
                    {
                        String oId = (String)tmpMap.get(DomainObject.SELECT_ID);
                        if(oId != null)
                        {
                            DomainObject obj = new DomainObject(oId);
                            obj.deleteObject(context);
                        }
                    }
                }
            }
            catch(Exception ex)
            {
                throw ex;
            }
            finally
            {
                if(isCtxPushed)
                {
                    ContextUtil.popContext(context);
                }
            }
        }
    }

    private void sendNotificationsForDeleteAction(Context context, String objectId) throws Exception
    {

        DomainObject dObj = new DomainObject(objectId);
        String messageKeys[] = {"WorkflowTaskType", "TaskName", "WorkflowType", "WorkflowName"};
        StringList selects = new StringList(2);
        selects.add(DomainObject.SELECT_OWNER);
        selects.add(DomainObject.SELECT_NAME);


        MapList mList = dObj.getRelatedObjects(context, PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask"),
                                                    PropertyUtil.getSchemaProperty(context,"type_WorkflowTask"),
                                                    selects, new StringList(), false, true, (short)0, "current == 'Assigned'", null);
        Iterator itr = mList.iterator();

        Map map = null;
        while(itr.hasNext())
        {
            StringList toList = new StringList(1);

            map = (Map)itr.next();
            toList.add((String)map.get(DomainObject.SELECT_OWNER));
            String messageValues[] = {PropertyUtil.getSchemaProperty(context, "type_WorkflowTask"),
                    (String)map.get(DomainObject.SELECT_NAME),
                    PropertyUtil.getSchemaProperty(context, "type_Workflow"),
                    dObj.getInfo(context, "name")
                    };

            MailUtil.sendNotification(context,
                    toList,   // To List
                    null,     // Cc List
                    null,     // Bcc List
                    "emxComponents.Workflow.WorkflowDeleteSubject",  // Subject key
                    null,                                       // Subject keys
                    null,                                       // Subject values
                    "emxComponents.Workflow.WorkflowDeleteMessage",  // Message key
                    messageKeys,         // Message keys
                    messageValues,         // Message values
                    null, // Object list
                    null,         // company name
                    "emxComponentsStringResource");
        }




    }

    private void delete(Context context, String name, String processName) throws Exception
    {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("delete workflow \"");
        sbuf.append(processName);
        sbuf.append("\" \"");
        sbuf.append(name);
        sbuf.append("\"");
        HashMap map = processMql(context, sbuf.toString());
        String error = (String)map.get("error");
        if(error != null){
            throw new Exception(error);
        }
    }

    public void stop(Context context, String[] args)throws Exception
    {
        stop(context, this.getInfo(context, "name"), this.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process")));
    }

    private void stop(Context context, String name, String processName) throws Exception
    {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("stop workflow \"");
        sbuf.append(processName);
        sbuf.append("\" \"");
        sbuf.append(name);
        sbuf.append("\"");
        HashMap map = processMql(context, sbuf.toString());
        String error = (String)map.get("error");
        if(error != null){
            throw new Exception(error);
        }
    }

    public void suspend(Context context, String[] args)throws Exception
    {

        suspend(context, this.getInfo(context, "name"), this.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process")));
    }

    private void suspend(Context context, String name, String processName) throws Exception
    {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("suspend workflow \"");
        sbuf.append(processName);
        sbuf.append("\" \"");
        sbuf.append(name);
        sbuf.append("\"");
        HashMap map = processMql(context, sbuf.toString());
        String error = (String)map.get("error");
        if(error != null){
            throw new Exception(error);
        }
    }

    public void resume(Context context, String[] args)throws Exception
    {
        resume(context, this.getInfo(context, "name"), this.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process")));
    }

    private void resume(Context context, String name, String processName) throws Exception
    {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("resume workflow \"");
        sbuf.append(processName);
        sbuf.append("\" \"");
        sbuf.append(name);
        sbuf.append("\"");
        HashMap map = processMql(context, sbuf.toString());
        String error = (String)map.get("error");
        if(error != null){
            throw new Exception(error);
        }
    }

    public void modify(Context context, String[] args)throws Exception
    {
        HashMap paramMap = (HashMap)JPO.unpackArgs(args);
        if(paramMap != null)
        {
            HashMap data = (HashMap)paramMap.get("ModifiedData");
            ArrayList values = (ArrayList)data.values();
            for(int i = 0; i < values.size(); i++)
            {
                if("name".equalsIgnoreCase((String)values.get(i)))
                {
                    this.setName(context, (String)data.get((String)values.get(i)));
                }else if("description".equalsIgnoreCase((String)values.get(i)))
                {
                    this.setDescription(context, (String)data.get((String)values.get(i)));
                }else if("attribute_PromoteConnectedObject".equals((String)values.get(i)))
                {
                    this.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_PromoteConnectedObject"), (String)data.get((String)values.get(i)));
                }
            }
        }
    }





    private BusinessObject createBusObject (Context context,
                                            String type,
                                            String name,
                                            String revision,
                                            String policy,
                                            String vault,
                                            String owner,
                                            String description)
    throws Exception
    {

        BusinessObject busObj = null;

        // Create using auto name if name is null
        if ( name == null || name.length() < 1)
        {
            String aliasType = FrameworkUtil.getAliasForAdmin (context,
                                   "type",
                                   type,
                                   true);

            String aliasPolicy = FrameworkUtil.getAliasForAdmin (context ,
                                     "policy" ,
                                     policy ,
                                     true);


            busObj = new BusinessObject(FrameworkUtil.autoName(context,
                    aliasType, revision, aliasPolicy, vault, revision, false, false));
        }
        else
        {
            busObj = new BusinessObject (type,
             name,
             revision,
             vault);
            busObj.create (context, policy);
        }


        busObj.open (context);
        busObj.setOwner (context, owner);

        // Seting The Description

        if ( (description != null) && (description.length () > 0) )
        {
            busObj.setDescription (context, description);
        }
        return busObj;
    }
    ////////////////////////////////////////////
    public String getWorkflowInfoAsXML(Context context, String[] args) throws Exception
    {
        StringBuffer xml = null;
        boolean isPushed = false;
        if(args.length == 0)
        {
          throw new Exception("Invalid arguments");
        }
        try
        {
            i18nNow i18nnow = new i18nNow();
            //String sLanguage = args[1];
            String sLanguage = context.getSession().getLanguage();
			Locale strLocale = new Locale(sLanguage);

            DomainObject dobj = new DomainObject(args[0]);
            String processName = dobj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process"));
            String cmd = "export process \"" + processName + "\" xml";

            ContextUtil.pushContext(context);
            isPushed = true;
            HashMap map = processMql(context, cmd);
            ContextUtil.popContext(context);
            isPushed = false;

            String result = (String)map.get("error");
            if(result != null)
            {
                throw new Exception(result);
            }
            xml = new StringBuffer((String)map.get("result"));

            ArrayList nodes = getNodes((String)map.get("result"));
            ArrayList links = getLinks((String)map.get("result"));

            //get the status and other attributes from workflow
            nodes = addWorkflowToProcess(context, nodes, args[0]);

            int index, end;

              // remove dtd stuff
              while ((index = (xml.toString()).indexOf("<!")) > -1 && (index != (xml.toString()).indexOf("<![CDATA")))
              {
                  end = (xml.toString()).indexOf(">\n", index) + 2;
                  xml.delete(index, end);
              }

              if ((index = (xml.toString()).indexOf(DTD_INFO_START)) > -1)
              {
                  end = (xml.toString()).indexOf(DTD_INFO_END) + DTD_INFO_END.length() + 1;
                  xml.delete(index, end);
              }

              // remove text between the nodeDefList start and end tags
              index = (xml.toString()).indexOf("<nodeDefList");
              if (index != -1)
              {
                  index = (xml.toString()).indexOf(">", index) + 1;
                  end = (xml.toString()).indexOf(NODE_DEF_LIST_END);
                  xml.delete(index, end);
              }

              // insert the nodes in alternate format
              if (nodes.size() > 0)
              {
                  Iterator itr = nodes.iterator();
                  while (itr.hasNext())
                  {
                      xml.insert(index, generateNode((String) itr.next()));
                  }

                  xml.insert((xml.toString()).indexOf(NODE_DEF_LIST_END), "\n");
              }

              // remove text between the linkDefList start and end tags
              index = (xml.toString()).indexOf("<linkDefList");
              if (index != -1)
              {
                  index = (xml.toString()).indexOf(">", index) + 1;
                  end = (xml.toString()).indexOf(LINK_DEF_LIST_END);
                  xml.delete(index, end);
              }

              // insert the links in alternate format
              if (links.size() > 0)
              {
                  Iterator itr = links.iterator();
                  while (itr.hasNext())
                  {
                      xml.insert(index, generateLink((String) itr.next()));
                  }

                  xml.insert((xml.toString()).indexOf(LINK_DEF_LIST_END), "\n");
              }

              StringBuffer sizeBuf = new StringBuffer(256);
              sizeBuf.append("\n");
              sizeBuf.append("<minXLocation>");
              sizeBuf.append(minXLocation);
              sizeBuf.append("</minXLocation>");
              sizeBuf.append("\n");
              sizeBuf.append("<maxXLocation>");
              sizeBuf.append(maxXLocation);
              sizeBuf.append("</maxXLocation>");
              sizeBuf.append("\n");
              sizeBuf.append("<minYLocation>");
              sizeBuf.append(minYLocation);
              sizeBuf.append("</minYLocation>");
              sizeBuf.append("\n");
              sizeBuf.append("<maxYLocation>");
              sizeBuf.append(maxYLocation);
              sizeBuf.append("</maxYLocation>");
              sizeBuf.append("\n<attributetranslations>\n");
              sizeBuf.append("<item name=\"name\"" + " translation=\"" + EnoviaResourceBundle.getProperty(context,"emxFrameworkStringResource",strLocale, "emxFramework.Basic.Name")+ "\" />\n");;
              sizeBuf.append("<item name=\"priority\"" + " translation=\"" + i18nNow.getAttributeI18NString("Priority" ,sLanguage) + "\" />\n");
              sizeBuf.append("<item name=\"owner\"" +" translation=\"" + i18nNow.getAttributeI18NString("Assignee" ,sLanguage) + "\" />\n");
              sizeBuf.append("<item name=\"assignee\"" + " translation=\"" + i18nNow.getAttributeI18NString("Owner" ,sLanguage) + "\" />\n");
            		  

              sizeBuf.append("<item name=\"duration\"" + " translation=\"" + i18nNow.getAttributeI18NString("Duration" ,sLanguage) + "\" />\n");
              sizeBuf.append("<item name=\"status\"" + " translation=\"" + i18nNow.getAttributeI18NString("Status" ,sLanguage) + "\" />\n");
              sizeBuf.append("<item name=\"rate\"" + " translation=\"" + i18nNow.getAttributeI18NString("Rate" ,sLanguage) + "\" />\n");
              sizeBuf.append("</attributetranslations>\n");


              int idx = (xml.toString()).indexOf(LINK_DEF_LIST_END) != -1 ? (xml.toString()).indexOf(LINK_DEF_LIST_END) : (xml.toString()).indexOf(NODE_DEF_LIST_END);
              xml.insert((idx + LINK_DEF_LIST_END.length()), sizeBuf.toString());
        }
        catch(Exception ex)
        {
            throw ex;
        }
        finally
        {
            if(isPushed)
            {
                ContextUtil.popContext(context);
            }
        }
        return xml.toString();
    }

    protected ArrayList addWorkflowToProcess(Context context, ArrayList nodes, String objectId) throws Exception
    {
        ArrayList newNodes = new ArrayList(nodes.size());
        DomainObject obj = new DomainObject(objectId);

        String node;
        StringBuffer command = new StringBuffer(256);
        String nodeType;
        String nodeName;
		Locale strLocale = new Locale(context.getSession().getLanguage());

        StringList selects = new StringList(5);
        selects.add(DomainObject.SELECT_ID);
        selects.add("attribute[Activity]");
        selects.add("current");
        selects.add("owner");
        selects.add("attribute[Due Date]");
        selects.add("attribute[Actual Completion Date]");

        MapList mList = obj.getRelatedObjects(context,PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask"),
                                               PropertyUtil.getSchemaProperty(context,"type_WorkflowTask"),
                                               selects, new StringList(), false, true, (short)1, null, null);


        selects = new StringList(2);
        selects.add(DomainObject.SELECT_ID);
        selects.add(DomainObject.SELECT_NAME);
        selects.add(DomainObject.SELECT_CURRENT);
        MapList subWFList = obj.getRelatedObjects(context,PropertyUtil.getSchemaProperty(context, "relationship_WorkflowSubProcess"),
                   PropertyUtil.getSchemaProperty(context,"type_Workflow"),
                   selects, new StringList(), false, true, (short)1, null, null);

        command.append("print workflow \"" + obj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process")) + "\" \"" + obj.getInfo(context, "name") + "\"");
        /*
        if(nodes.size() > 0)
        {
            command.append(" select ");
        }
        */
        Iterator itr = nodes.iterator();
        boolean selectAppended = false;
        while (itr.hasNext())
        {
            node = (String) itr.next();
            nodeType = getValue(node, NODE_TYPE_START, NODE_TYPE_END, 0);
            nodeName = getValue(node, NODE_NAME_START, NODE_NAME_END, 0);

            if ("interactiveNode".equals(nodeType))
            {
                if(!selectAppended)
                {
                    command.append(" select ");
                    selectAppended = true;
                }

                command.append("interactive[").append(nodeName).append("].status ");
            }
            else if ("automatedNode".equals(nodeType))
            {
                if(!selectAppended)
                {
                    command.append(" select ");
                    selectAppended = true;
                }
                command.append("automated[").append(nodeName).append("].status ");
            }
        }

        HashMap hMap = processMql(context, command.toString());
        String result = (String)hMap.get("error");
        if(result != null)
        {
            throw new Exception(result);
        }
        result = (String)hMap.get("result");

        BufferedReader in = new BufferedReader(new StringReader(result));

        try
        {
            // skip first line
            in.readLine();
        }
        catch (IOException e)
        {
        }

        /////////
        itr = nodes.iterator();
        String status;
        StringBuffer tempBuf = null;
        while (itr.hasNext())
        {
            node = (String) itr.next();
            nodeType = getValue(node, NODE_TYPE_START, NODE_TYPE_END, 0);
            nodeName = getValue(node, NODE_NAME_START, NODE_NAME_END, 0);
            tempBuf = new StringBuffer();
            tempBuf.append(node);

            if ("interactiveNode".equals(nodeType) || "automatedNode".equals(nodeType))
            {

                // add status info
                try
                {
                    if ((status = in.readLine()) != null)
                    {
                        status = status.substring(status.indexOf('=') + 1).trim();

                        if (status.length() > 0)
                        {

                            if(status.equals("overidden"))
                            {
                                status = "overridden";
                            }
                            tempBuf.append("<status>");
                            tempBuf.append(status);
                            tempBuf.append("</status>");

                        }
                    }
                }
                catch (IOException e)
                {
                }

                // add attribute information here
                Iterator iter = mList.iterator();
                String wfTaskName = null;
                Map tempMap = null;
                while(iter.hasNext())
                {
                    tempMap = (Map)iter.next();
                    wfTaskName = (String)tempMap.get("attribute[Activity]");
                    if(nodeName.endsWith(wfTaskName))
                    {
                        tempBuf.append("<objectId>");
                        tempBuf.append(tempMap.get(DomainObject.SELECT_ID));
                        tempBuf.append("</objectId>");
                        String contextUser = context.getUser();
                        String workflowOwner = getInfo(context, "owner");
                        String taskOwner = (String)tempMap.get("owner");
                        if(!UIUtil.isNullOrEmpty(taskOwner)){
		                        String cmd = MqlUtil.mqlCommand(context, "print user $1 select $2 dump $3",taskOwner ,"isaperson", "|");
		                        boolean isPerson = "TRUE".equalsIgnoreCase(cmd);
				                        if(isPerson){
							                        tempBuf.append("<owner>");
							                        tempBuf.append(tempMap.get("owner"));
							                        tempBuf.append("</owner>");
				                        }
                        }
                        if(contextUser.equals(workflowOwner) || contextUser.equals(taskOwner))
                        {
                            tempBuf.append("<editable>true</editable>");
                        }
                        tempBuf.append("<current>");
                        tempBuf.append(tempMap.get("current"));
                        tempBuf.append("</current>");
                        tempBuf.append("<statusDisplay>");
                        tempBuf.append(EnoviaResourceBundle.getProperty(context,"emxFrameworkStringResource",strLocale, "emxFramework.State.Workflow_Task."+tempMap.get("current")));
                        tempBuf.append("</statusDisplay>");        
                        tempBuf.append("<scheduledCompletionDate>");
                        tempBuf.append(tempMap.get("attribute[Due Date]"));
                        tempBuf.append("</scheduledCompletionDate>");
                        tempBuf.append("<actualCompletionDate>");
                        tempBuf.append(tempMap.get("attribute[Actual Completion Date]"));
                        tempBuf.append("</actualCompletionDate>");

                        //StringBuffer::indexOf() is not available until java 1.4
                        String tempStr = tempBuf.toString();
						//Added for the Bug No:332190  Begin
                        //tempBuf.replace((tempBuf.indexOf("<userRef>") + ("<userRef>".length())), (tempBuf.indexOf("</userRef>")+("</userRef>".length())), (String)tempMap.get("owner"));
                        tempBuf.append(tempMap.get("owner"));
                        tempBuf.append("</owner>");
						//Added for the Bug No:332190  End
                        break;
                    }

                }

            }
            else if("processNode".equals(nodeType))
            {
                Iterator iter = subWFList.iterator();
                String wfName = null;
                Map tempMap = null;
                while(iter.hasNext())
                {
                    tempMap = (Map)iter.next();
                    wfName = (String)tempMap.get(DomainObject.SELECT_NAME);
                    if(wfName != null && wfName.endsWith(nodeName))
                    {
                        tempBuf.append("<objectId>");
                        tempBuf.append(tempMap.get(DomainObject.SELECT_ID));
                        tempBuf.append("</objectId>");
                        String strStatus = (String)tempMap.get(DomainObject.SELECT_CURRENT);
                        if(strStatus != null && strStatus.equalsIgnoreCase("completed"))
                        {
                            tempBuf.append("<status>");
                            tempBuf.append("completed");
                            tempBuf.append("</status>");
                            tempBuf.append("<statusDisplay>");
                            tempBuf.append(EnoviaResourceBundle.getProperty(context,"emxFrameworkStringResource",strLocale, "emxFramework.State.Workflow.Completed")); 
                            tempBuf.append("</statusDisplay>");
                        }
                        else if(strStatus != null && strStatus.equalsIgnoreCase("suspended"))
                        {
                            tempBuf.append("<status>");
                            tempBuf.append("suspended");
                            tempBuf.append("</status>");
                            tempBuf.append("<statusDisplay>");
                            tempBuf.append(EnoviaResourceBundle.getProperty(context,"emxFrameworkStringResource",strLocale, "emxFramework.State.Workflow.Suspended"));
                            tempBuf.append("</statusDisplay>");
                        }
                        break;
                    }

                }
            }
            node = tempBuf.toString();
            newNodes.add(node);
        }
        ////////////
        return newNodes;
    }
    public String getProcessInfoAsXML(Context context, String[] args) throws Exception
    {
      StringBuffer xml = null;
      boolean isPushed = false;
      if(args.length == 0)
      {
          throw new Exception("Invalid arguments");
      }
      try
      {
          //String sLanguage = args[1];
        String sLanguage = context.getSession().getLanguage();
		Locale strLocale = context.getLocale();

//        System.out.println("sLanguage : " + sLanguage);
          i18nNow i18nnow = new i18nNow();
          String cmd = "export process \"" + args[0] + "\" xml";

          ContextUtil.pushContext(context);
          isPushed = true;
          HashMap map = processMql(context, cmd);
          ContextUtil.popContext(context);
          isPushed = false;

          String result = (String)map.get("error");
          if(result != null || "undefined".equals(args[0]))
          {
              throw new Exception(result);
          }
          xml = new StringBuffer((String)map.get("result"));

          ArrayList nodes = getNodes((String)map.get("result"));
          ArrayList links = getLinks((String)map.get("result"));

          int index, end;

              // remove dtd stuff
              while ((index = (xml.toString()).indexOf("<!")) > -1 && (index != (xml.toString()).indexOf("<![CDATA")))
              {
                  end = (xml.toString()).indexOf(">\n", index) + 2;
                  xml.delete(index, end);
              }

              if ((index = (xml.toString()).indexOf(DTD_INFO_START)) > -1)
              {
                  end = (xml.toString()).indexOf(DTD_INFO_END) + DTD_INFO_END.length() + 1;
                  xml.delete(index, end);
              }

              // remove text between the nodeDefList start and end tags
              index = (xml.toString()).indexOf("<nodeDefList");
              if (index != -1)
              {
                  index = (xml.toString()).indexOf(">", index) + 1;
                  end = (xml.toString()).indexOf(NODE_DEF_LIST_END);
                  xml.delete(index, end);
              }

              // insert the nodes in alternate format
              if (nodes.size() > 0)
              {
                  Iterator itr = nodes.iterator();
                  while (itr.hasNext())
                  {
                      xml.insert(index, generateNode((String) itr.next()));
                  }

                  xml.insert((xml.toString()).indexOf(NODE_DEF_LIST_END), "\n");
              }

              // remove text between the linkDefList start and end tags
              index = (xml.toString()).indexOf("<linkDefList");
              if (index != -1)
              {
                  index = (xml.toString()).indexOf(">", index) + 1;
                  end = (xml.toString()).indexOf(LINK_DEF_LIST_END);
                  xml.delete(index, end);
              }

              // insert the links in alternate format
              if (links.size() > 0)
              {
                  Iterator itr = links.iterator();
                  while (itr.hasNext())
                  {
                      xml.insert(index, generateLink((String) itr.next()));
                  }

                  xml.insert((xml.toString()).indexOf(LINK_DEF_LIST_END), "\n");
              }

              StringBuffer sizeBuf = new StringBuffer(256);
              sizeBuf.append("\n");
              sizeBuf.append("<minXLocation>");
              sizeBuf.append(minXLocation);
              sizeBuf.append("</minXLocation>");
              sizeBuf.append("\n");
              sizeBuf.append("<maxXLocation>");
              sizeBuf.append(maxXLocation);
              sizeBuf.append("</maxXLocation>");
              sizeBuf.append("\n");
              sizeBuf.append("<minYLocation>");
              sizeBuf.append(minYLocation);
              sizeBuf.append("</minYLocation>");
              sizeBuf.append("\n");
              sizeBuf.append("<maxYLocation>");
              sizeBuf.append(maxYLocation);
              sizeBuf.append("</maxYLocation>\n");
              sizeBuf.append("\n<attributetranslations>\n");
              sizeBuf.append("<item name=\"name\"" + " translation=\"" + EnoviaResourceBundle.getProperty(context,"emxFrameworkStringResource",strLocale, "emxFramework.Basic.Name")+ "\" />\n");
              sizeBuf.append("<item name=\"priority\"" + " translation=\"" + i18nNow.getAttributeI18NString("Priority" ,sLanguage) + "\" />\n");
              sizeBuf.append("<item name=\"owner\"" + " translation=\"" + i18nNow.getAttributeI18NString("Assignee" ,sLanguage) + "\" />\n");
              sizeBuf.append("<item name=\"assignee\"" +" translation=\"" + i18nNow.getAttributeI18NString("Owner" ,sLanguage) + "\" />\n");

              sizeBuf.append("<item name=\"duration\"" + " translation=\"" + i18nNow.getAttributeI18NString("Duration" ,sLanguage) + "\" />\n");
              sizeBuf.append("<item name=\"status\"" + " translation=\"" + i18nNow.getAttributeI18NString("Status" ,sLanguage) + "\" />\n");
              sizeBuf.append("<item name=\"rate\"" + " translation=\"" + i18nNow.getAttributeI18NString("Rate" ,sLanguage) + "\" />\n");
              sizeBuf.append("</attributetranslations>\n");
              int idx = (xml.toString()).indexOf(LINK_DEF_LIST_END) != -1 ? (xml.toString()).indexOf(LINK_DEF_LIST_END) : (xml.toString()).indexOf(NODE_DEF_LIST_END);
              xml.insert((idx + LINK_DEF_LIST_END.length()), sizeBuf.toString());

      }
      catch(Exception ex)
      {
          throw ex;
      }
      finally
      {
          if(isPushed)
          {
              ContextUtil.popContext(context);
          }
      }

      return xml.toString();
    }


    protected ArrayList getNodes(String xml)
    {
        ArrayList nodes = new ArrayList();
        String node;
        int index = 0;

        while ((node = getNode(xml, index)) != null)
        {
            nodes.add(node);
            index = (xml.toString()).indexOf(NODE_DEF_END, index) + NODE_DEF_END.length();
        }

        return (nodes);
    }

    protected String getNode(String xml, int index)
    {
        return (getValue(xml, NODE_DEF_START, NODE_DEF_END, index));
    }

    protected String generateNode(String input) throws Exception
    {
        StringBuffer node = new StringBuffer(input.length());
        try
        {
            node.append("\n<nodeDef ");
            node.append(generateAttribute(input, "nodeType"));
            node.append(generateAttribute(input, "name"));
            node.append(generateAttribute(input, "description"));
            node.append(generateAttribute(input, "process"));
            node.append(generateAttribute(input, "xLocation"));
            node.append(generateAttribute(input, "yLocation"));
            node.append(generateAttribute(input, "instructions"));
            node.append(generateAttribute(input, "priority"));
            node.append(generateAttribute(input, "duration"));
            node.append(generateAttribute(input, "rate"));
            node.append(generateAttribute(input, "assignee"));
            node.append(generateAttribute(input, "status"));
            node.append(generateAttribute(input, "statusDisplay"));
            node.append(generateAttribute(input, "current"));
            node.append(generateAttribute(input, "owner"));
            node.append(generateAttribute(input, "scheduledCompletionDate"));
            node.append(generateAttribute(input, "actualCompletionDate"));
            node.append(generateAttribute(input, "objectId"));
            node.append(generateAttribute(input, "editable"));
        }catch(Exception ex)
        {
            throw ex;
        }

        node.append("/>");
        return (node.toString());
    }

    /**
     * Generate a link node.
     *
     * @param input old link node
     * @return the new link node
     */
    protected String generateLink(String input) throws Exception
    {
        StringBuffer node = new StringBuffer(input.length());
        try
        {
            node.append("\n<linkDef ");
            node.append(generateAttribute(input, "fromNodeName"));
            node.append(generateAttribute(input, "toNodeName"));
            node.append("/>");
        }catch(Exception ex)
        {
            throw ex;
        }
        return (node.toString());
    }

    /**
     * Generate an attribute from tagged xml.
     *
     * @param input the xml containing the tag and value
     * @param tag the attribute tag
     * @return the generated xml attribute
     */
    protected String generateAttribute(String input, String tag) throws Exception
    {
        String attribute = "";
        String value;
        int start = input.indexOf(tag);


        if (start != -1)
        {
            int idxCloseTagStart = input.indexOf("</", start);
            int idxEndOfTag = (start + tag.length() + 1);
            if((idxCloseTagStart != -1) && (idxCloseTagStart > idxEndOfTag))
            {
                value = input.substring(idxEndOfTag, idxCloseTagStart);

                // strip "Node" off the end of node type attribute
                if ("nodeType".equals(tag) == true)
                {
                    if ((start = value.indexOf("Node")) > -1)
                    {
                        value = value.substring(0, start);
                    }
                }
                if("assignee".equals(tag))
                {
                    value = value.trim();
                    if ((start = value.indexOf("<userRef>")) > -1)
                    {
                        value = value.substring((start + 9));
                    }

                }
                if("process".equals(tag))
                {
                    start = input.indexOf("<parentName>");
                    int end = input.indexOf("</parentName>");
                    value = input.substring((start + "<parentName>".length()), end);

                }
                if("xLocation".equals(tag))
                {
                    try
                    {
                        int tmpXLocation = Integer.parseInt(value);
                        if(minXLocation == 0)
                        {
                            minXLocation = tmpXLocation;
                        }
                        else if(minXLocation != 0 && tmpXLocation < minXLocation)
                        {
                            minXLocation = tmpXLocation;
                        }
                        if(maxXLocation == 0)
                        {
                            maxXLocation = tmpXLocation;
                        }
                        else if(maxXLocation != 0 && tmpXLocation > maxXLocation)
                        {
                            maxXLocation = tmpXLocation;
                        }

                    }catch(Exception ex)
                    {
                        throw ex;
                    }

                }
                if("yLocation".equals(tag))
                {
                    try
                    {
                        int tmpXLocation = Integer.parseInt(value);
                        if(minYLocation == 0)
                        {
                            minYLocation = tmpXLocation;
                        }
                        else if(tmpXLocation < minYLocation)
                        {
                            minYLocation = tmpXLocation;
                        }
                        if(maxYLocation == 0)
                        {
                            maxYLocation = tmpXLocation;
                        }
                        else if(tmpXLocation > maxYLocation)
                        {
                            maxYLocation = tmpXLocation;
                        }

                    }catch(Exception ex)
                    {
                        throw ex;
                    }

                }
                //value = com.matrixone.apps.domain.util.XSSUtil.encodeForURL(value);
                attribute = tag + "=\"" + value + "\" ";
            }
        }

        return (attribute);
    }

    /**
     * Extract the link nodes from the original xml.
     *
     * @param xml the original xml export from mql
     * @return an ArrayList containing strings of links
     */
    protected ArrayList getLinks(String xml)
    {
        ArrayList links = new ArrayList();
        String link;
        int index = 0;

        while ((link = getLink(xml, index)) != null)
        {
            links.add(link);
            index = xml.indexOf(LINK_DEF_END, index) + LINK_DEF_END.length();
        }

        return (links);
    }

    /**
     * Extract a link node from the original xml.
     *
     * @param xml the original xml export from mql
     * @param index offset into the xml string.
     * @return
     */
    protected String getLink(String xml, int index)
    {
        return (getValue(xml, LINK_DEF_START, LINK_DEF_END, index));
    }

    /**
     * Extract the value between the start and end tags.
     *
     * @param xml the original xml export from mql
     * @param startTag the start tag
     * @param endTag the end tag
     * @param index offset into the xml string
     * @return the value between the tags
     */
    protected String getValue(String xml, String startTag, String endTag, int index)
    {
        int start, end;
        String value = null;

        if ((start = xml.indexOf(startTag, index)) > -1)
        {
            end = xml.indexOf(endTag, start);
            value = xml.substring(start + startTag.length(), end);
        }

        return (value);
    }


    /////////////////////////////////////////////

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
     * This method is executed to get the rollup deliverables of a workflow object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getRollupDeliverables(Context context, String[] args)
        throws Exception
    {
        try
        {
            HashMap programMap        = (HashMap) JPO.unpackArgs(args);
            String  workflowId          = (String) programMap.get("objectId");
            Pattern relPattern        = new Pattern("");

            //Rel patern for searching objects.
            String relWorkflowTask       = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
            String relTaskDeliverable    = PropertyUtil.getSchemaProperty(context, "relationship_TaskDeliverable");
            String relWorkflowSubProcess = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowSubProcess");

            relPattern.addPattern(relWorkflowTask);
            relPattern.addPattern(relTaskDeliverable);
            relPattern.addPattern(relWorkflowSubProcess);

            //Rel patern for including Task Deliverable.
            Pattern includeRelPattern        = new Pattern("");
            includeRelPattern.addPattern(relTaskDeliverable);

            RelationshipType relType = new RelationshipType(relTaskDeliverable);
            BusinessTypeList boList = relType.getToTypes(context);

            Iterator itr = boList.iterator();
            Pattern typePattern = null;
            while(itr.hasNext())
            {
                if(typePattern == null)
                {
                    typePattern = new Pattern(((BusinessType)itr.next()).getName());
                }
                else
                {
                    typePattern.addPattern(((BusinessType)itr.next()).getName());
                }
            }

            typePattern.addPattern(PropertyUtil.getSchemaProperty(context, "type_WorkflowTask"));
            typePattern.addPattern(PropertyUtil.getSchemaProperty(context, "type_Workflow"));


            DomainObject workflowObject = DomainObject.newInstance(context, workflowId);

            SelectList typeSelects = new SelectList(1);
            typeSelects.add(CommonDocument.SELECT_ID);
            // Multi value list is not working with getRelated objects - Throwing
            // class cast exception while retriving data so using normal expressions
            // to add selectables.
            //typeSelects.setComplexSelect("to["+relTaskDeliverable+"]","from.name");
            //DomainObject.MULTI_VALUE_LIST.add(typeSelects.getComplexSelect("to["+relTaskDeliverable+"]", "from.name"));

            //selectales for workflow task
            typeSelects.add("to["+relTaskDeliverable+"].from.id");
            typeSelects.add("to["+relTaskDeliverable+"].from.name");
            typeSelects.add("to["+relTaskDeliverable+"].from.type");

            //selectales for workflow
            typeSelects.add("to["+relTaskDeliverable+"].from.to["+relWorkflowTask+"].from.id");
            typeSelects.add("to["+relTaskDeliverable+"].from.to["+relWorkflowTask+"].from.name");
            typeSelects.add("to["+relTaskDeliverable+"].from.to["+relWorkflowTask+"].from.type");

            StringList relSelects = new StringList(1);
            relSelects.add(CommonDocument.SELECT_RELATIONSHIP_ID);
            MapList documentList = workflowObject.getRelatedObjects(context,
                                                          relPattern.getPattern(),
                                                          typePattern.getPattern(),
                                                          typeSelects,
                                                          relSelects,
                                                          false,
                                                          true,
                                                          (short)0,
                                                          null, //objectWhere
                                                          null, //relWhere,
                                                          null,
                                                          includeRelPattern,
                                                          null);

            return documentList;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw ex;
        }
    }

    /**
     * This method is executed to get workflow tasks connected to a workflow object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @param busWhere holds where condition to get objects
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */
    public Object getTasks(Context context, String[] args, String busWhere)
        throws Exception
    {
        try
        {
            HashMap programMap        = (HashMap) JPO.unpackArgs(args);
            String  workflowId          = (String) programMap.get("objectId");
            Pattern relPattern        = new Pattern("");
            String relWorkflowTask = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
            relPattern.addPattern(relWorkflowTask);
            this.setId(workflowId);

            StringList typeSelects = new StringList(3);
            typeSelects.add(CommonDocument.SELECT_ID);
            typeSelects.add(CommonDocument.SELECT_CURRENT);
            typeSelects.add(strAttrDueDate);
            typeSelects.add(strAttrTaskCompletionDate);
            StringList relSelects = new StringList(1);
            relSelects.add(CommonDocument.SELECT_RELATIONSHIP_ID);
            MapList tasksList = this.getRelatedObjects(context,
                                                          relPattern.getPattern(),
                                                          "*",
                                                          typeSelects,
                                                          relSelects,
                                                          false,
                                                          true,
                                                          (short)1,
                                                          busWhere, //null, //objectWhere
                                                          null, //relWhere,
                                                          null,
                                                          null,
                                                          null);

            return tasksList;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw ex;
        }
    }


    /**
     * This method is executed to get all workflow tasks connected to a workflow object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */
     @com.matrixone.apps.framework.ui.ProgramCallable
     public Object getAllTasks(Context context, String[] args) throws Exception
     {
        return getTasks(context,args,"");
     }

    /**
     * This method is executed to get active workflow tasks connected to a workflow object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */

     @com.matrixone.apps.framework.ui.ProgramCallable
     public Object getActiveTasks(Context context, String[] args) throws Exception
     {

        /*String stateStarted   = PropertyUtil.getSchemaProperty(context, "policy",DomainObject.SYMBOLIC_policy_WorkflowTask,"state_Started");
        String stateAssigned  = PropertyUtil.getSchemaProperty(context, "policy",DomainObject.SYMBOLIC_policy_WorkflowTask,"state_Assigned");
        String stateSuspended = PropertyUtil.getSchemaProperty(context, "policy",DomainObject.SYMBOLIC_policy_WorkflowTask,"state_Suspended");*/
        String stateStarted = FrameworkUtil.lookupStateName(context, policyWorkflowTask, "state_Started");
        String stateAssigned = FrameworkUtil.lookupStateName(context, policyWorkflowTask, "state_Assigned");
        String stateSuspended = FrameworkUtil.lookupStateName(context, policyWorkflowTask, "state_Suspended");

        StringBuffer busWhere = new StringBuffer("(current == ");
        busWhere.append(stateStarted);
        busWhere.append(") || (current == ");
        busWhere.append(stateAssigned);
        busWhere.append(") || (current == ");
        busWhere.append(stateSuspended);
        busWhere.append(")");

        //System.out.println("active: "+busWhere.toString());
        return getTasks(context,args,busWhere.toString());
     }


    /**
     * This method is executed to get completed workflow tasks connected to a workflow object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */
     @com.matrixone.apps.framework.ui.ProgramCallable
     public Object getCompletedTasks(Context context, String[] args) throws Exception
     {

        /*String stateOverridden = PropertyUtil.getSchemaProperty(context, "policy",DomainObject.SYMBOLIC_policy_WorkflowTask,"state_Overridden");
        String stateCompleted  = PropertyUtil.getSchemaProperty(context, "policy",DomainObject.SYMBOLIC_policy_WorkflowTask,"state_Completed");*/
        String stateOverride = FrameworkUtil.lookupStateName(context, policyWorkflowTask, "state_Override");
        String stateCompleted = FrameworkUtil.lookupStateName(context, policyWorkflowTask, "state_Completed");

        StringBuffer busWhere = new StringBuffer("(current == \"");
          busWhere.append(stateOverride);
          busWhere.append("\") || (current == \"");
          busWhere.append(stateCompleted);
          busWhere.append("\")");

          return getTasks(context,args,busWhere.toString());
     }


    /**
     * showTaskStatusIcon - gets the status gif to be shown in the column of the Task Summary table
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object of type Vector
     * @throws Exception if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public Vector showTaskStatusIcon(Context context, String[] args)
        throws Exception
    {
        try
        {
          HashMap programMap = (HashMap) JPO.unpackArgs(args);
          MapList objectList = (MapList)programMap.get("objectList");

          Vector statusIconList = new Vector();
          String stateComplete = FrameworkUtil.lookupStateName(context, policyWorkflowTask, "state_Completed");
          Date dueDate   = null;
          Date curDate = new Date();
          String statusImageString = "";
          String statusColor= "";
          Iterator objectListItr = objectList.iterator();
          while(objectListItr.hasNext())
          {
              Map objectMap = (Map) objectListItr.next();
              String taskState         = (String) objectMap.get(DomainObject.SELECT_CURRENT);
              String taskDueDate       = (String)objectMap.get(strAttrDueDate);
              String taskCompletedDate = (String)objectMap.get(strAttrTaskCompletionDate);
                if(!taskState.equals(""))
                {
                    if(taskDueDate == null || taskDueDate.equals("")) {
                        dueDate = new Date();
                    }else {
                      dueDate = com.matrixone.apps.domain.util.eMatrixDateFormat.getJavaDate(taskDueDate);
                    }
                    if(!taskState.equals(stateComplete)) {
                        if(dueDate != null && curDate.after(dueDate)) {
                            statusColor = "Red";
                        }else {
                            statusColor = "Green";
                        }
                    }else {
                        Date actualCompletionDate = com.matrixone.apps.domain.util.eMatrixDateFormat.getJavaDate(taskCompletedDate);
                        if(dueDate != null && actualCompletionDate.after(dueDate)) {
                            statusColor = "Red";
                        }else {
                            statusColor = "Green";
                        }
                    }
                    if(statusColor.equals("Red")){
                        statusImageString = "<img border='0' src='../common/images/iconStatusRed.gif' name='red' id='red' alt='*' />";
                    }else if(statusColor.equals("Green")){
                        statusImageString = "<img border='0' src='../common/images/iconStatusGreen.gif' name='green' id='green' alt='*' />";
                    }else{
                        statusImageString="&nbsp;";
                    }
                    statusIconList.add(statusImageString);
                }
          }
          return statusIconList;
        }catch (Exception ex){
            //System.out.println("Error in showTaskStatusIcon= " + ex.getMessage());
            throw ex;
        }
    }

    /**
     * This method is executed to get workflow content objects connected to a workflow object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @param busWhere holds where condition to get objects
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getContent(Context context, String[] args)
        throws Exception
    {
        try
        {
            HashMap programMap        = (HashMap) JPO.unpackArgs(args);
            String  workflowId          = (String) programMap.get("objectId");
            Pattern relPattern        = new Pattern("");
            String relWorkflowContent = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowContent");
            relPattern.addPattern(relWorkflowContent);
            setId(workflowId);

            StringList typeSelects = new StringList(1);
            typeSelects.add(CommonDocument.SELECT_ID);
            StringList relSelects = new StringList(2);
            relSelects.add(CommonDocument.SELECT_RELATIONSHIP_ID);
            relSelects.add("attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_RouteBaseState") + "]");

            MapList contentList = this.getRelatedObjects(context,
                                                          relPattern.getPattern(),
                                                          "*",
                                                          typeSelects,
                                                          relSelects,
                                                          false,
                                                          true,
                                                          (short)1,
                                                          null, //objectWhere
                                                          null, //relWhere,
                                                          null,
                                                          null,
                                                          null);

            return contentList;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw ex;
        }
    }

    public Object getContentStateBlock(Context context, String[] args) throws Exception
    {
        Vector contentList = new Vector();
        try
        {
            HashMap programMap        = (HashMap) JPO.unpackArgs(args);
            MapList objectList        = (MapList)programMap.get("objectList");
            HashMap requestMap        = (HashMap)programMap.get("paramList");
            String editTableMode = (String)requestMap.get("editTableMode");
            String selectible = "attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_RouteBaseState") + "]";

            if(editTableMode != null && editTableMode.equals("false"))
            {
                for(Iterator itr = objectList.iterator(); itr.hasNext();)
                {
                    Map m = (Map)itr.next();
                    DomainObject dObj = new DomainObject((String)m.get(DomainObject.SELECT_ID));
                    DomainRelationship relationship = new DomainRelationship((String)m.get(DomainObject.SELECT_RELATIONSHIP_ID));
                    String value = relationship.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_RouteBaseState"));
                    if(value != null && !value.equals("Ad Hoc"))
                    {
                        value = i18nNow.getStateI18NString(dObj.getPolicy(context).getName(), value, context.getSession().getLanguage());
                    }
                    else
                    {
                        value = EnoviaResourceBundle.getProperty(context,"emxComponentsStringResource", new Locale(context.getSession().getLanguage()),"emxComponents.Common.None");
                    }
                    contentList.add(value);

                }
            }
            else
            {
                String objStates = "";
                String objPolicy = "";
                String baseState = "";
                String sNone = EnoviaResourceBundle.getProperty(context,"emxComponentsStringResource", new Locale(context.getSession().getLanguage()),"emxComponents.AttachmentsDialog.none");

                for(Iterator itr = objectList.iterator(); itr.hasNext();)
                {
                    Map m = (Map)itr.next();
                    DomainObject dObj = new DomainObject((String)m.get(DomainObject.SELECT_ID));
                    DomainRelationship relationship = new DomainRelationship((String)m.get(DomainObject.SELECT_RELATIONSHIP_ID));
                    objStates = dObj.getInfoList(context, DomainObject.SELECT_STATES).toString();
                    if(!(objStates.indexOf('[') <0 ) )
                    {
                        objStates = objStates.substring(objStates.indexOf('[')+1,objStates.indexOf(']'));
                    }
                    objPolicy = dObj.getInfo(context, DomainObject.SELECT_POLICY);
                    baseState = relationship.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_RouteBaseState"));
                    contentList.add(populateCombo(context, objStates, objPolicy, baseState, sNone, dObj.getInfo(context, DomainObject.SELECT_CURRENT), (String)m.get(DomainObject.SELECT_RELATIONSHIP_ID)));
                }
            }
        }
        catch(Exception ex)
        {
            throw ex;
        }

        return contentList;
    }

    private String populateCombo(Context context, String allStates, String policy, String sStateValue, String sNoneValue, String curState, String relId) throws Exception
    {
        try
        {
            StringBuffer sBuf = new StringBuffer(256);
            String sSymbolicPolicyName = FrameworkUtil.getAliasForAdmin(context, "policy", policy, true);

            boolean canAdd = false;
            StringTokenizer sTok =  new StringTokenizer(allStates, ",");
            int numTokens = sTok.countTokens();
            int curtok = 1;

            if(sTok.hasMoreTokens())
            {
                sBuf.append("<select name=\"");
                sBuf.append(relId);
                sBuf.append("\" >");
                sBuf.append("<option value=\"");
                sBuf.append(sSymbolicPolicyName);
                sBuf.append("#Ad Hoc\" >");
                sBuf.append(sNoneValue);
                sBuf.append("</option>");

                while(sTok.hasMoreTokens())
                {
                    if(curtok == numTokens)
                    {
                        break;
                    }

                    curtok++;
                    String sStateName =  sTok.nextToken().trim();
                    if(curState.equals(sStateName))
                    {
                        canAdd = true;
                    }

                    String sSymbolicStateName = FrameworkUtil.reverseLookupStateName(context, policy, sStateName);
                    if(canAdd)
                    {
                        if (sStateValue.equals(sStateName))
                        {
                            sBuf.append("<option value=\"");
                            sBuf.append(sSymbolicPolicyName);
                            sBuf.append("#");
                            sBuf.append(sSymbolicStateName);
                            sBuf.append("\" selected >");
                            sBuf.append(i18nNow.getStateI18NString(policy,sStateName,context.getSession().getLanguage()));
                            sBuf.append("</option>");
                        }
                        else
                        {
                            sBuf.append("<option value=\"");
                            sBuf.append(sSymbolicPolicyName);
                            sBuf.append("#");
                            sBuf.append(sSymbolicStateName);
                            sBuf.append("\">");
                            sBuf.append(i18nNow.getStateI18NString(policy,sStateName,context.getSession().getLanguage()));
                            sBuf.append("</option>");
                        }
                    }
                }
            }
            sBuf.append("</select>");
            return sBuf.toString();
        }
        catch(Exception ex)
        {
            throw ex;
        }
    }

    public void setContentStateBlock(Context context, String[] args) throws Exception
    {
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap   = (HashMap) programMap.get("paramMap");
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String  objectId   = (String) paramMap.get("objectId");
            String relId = (String)paramMap.get("relId");

            if(relId != null && relId.length() > 0)
            {
                DomainRelationship relation = new DomainRelationship(relId);
                String[] tempArray = (String[])requestMap.get(relId);
                if(tempArray.length > 0)
                {
                    StringTokenizer tokens = new StringTokenizer(tempArray[0], "#");
                    String policy = tokens.nextToken();
                    String state = tokens.nextToken();
                    policy = PropertyUtil.getSchemaProperty(context, policy);
                    Map attrMap = new HashMap();
                    attrMap.put(PropertyUtil.getSchemaProperty(context,"attribute_RouteBasePolicy"), policy);
                    if(!state.equals("Ad Hoc"))
                    {
                        state = FrameworkUtil.lookupStateName(context, policy, state);
                    }
                    attrMap.put(PropertyUtil.getSchemaProperty(context, "attribute_RouteBaseState"), state);
                    relation.open(context);
                    relation.setAttributeValues(context, attrMap);
                    relation.close(context);
                }

            }
        }
        catch(Exception ex)
        {
            throw ex;
        }
    }

    /**
     * This method is executed to get deliverables for deliverables search results.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getTaskDeliverableSearchResult(Context context, String[] args)
        throws Exception
    {
        try
        {
            HashMap programMap      = (HashMap) JPO.unpackArgs(args);
            //Reading objects from programMap as reading from RequestValuesMap is throwing class cast exception
            //Map paramList      = (HashMap)programMap.get("RequestValuesMap");
            String Owner             = (String)programMap.get("Owner");
            String Name              = (String)programMap.get("Name");
            String Type              = (String)programMap.get("DocumentType");
            String CreateAfterDate   = (String)programMap.get("CreatedAfter");
            String CreateBeforeDate  = (String)programMap.get("CreatedBefore");
            String DocumentType      = (String)programMap.get("DocumentType");
            String Title             = (String)programMap.get("Title");
            String vaultType         = (String)programMap.get("vaultType");

            //Find objects which match search criteria.

            String vaultPattern = "";
            if(!vaultType.equals(PersonUtil.SEARCH_SELECTED_VAULTS))
            {
              vaultPattern = PersonUtil.getSearchVaults(context, false ,vaultType);
            }
            else
            {
              vaultPattern = (String)programMap.get("selectedVaults");
            }

            String queryLimit = (String)programMap.get("queryLimit");
            if (queryLimit == null || queryLimit.equals("null") || queryLimit.equals("")){
              queryLimit = "0";
            }

            String timeZone = (String)programMap.get("timeZone");
            double iClientTimeOffset = (new Double(timeZone)).doubleValue();
            boolean createAfterDateEntered = false;
            boolean createBeforeDateEntered = false;
            String noDate = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(), "emxComponents.Common.NoDate");

            if (!CreateAfterDate.equals(noDate)) {
              CreateAfterDate = eMatrixDateFormat.getFormattedInputDateTime(context,CreateAfterDate, "11:59:59 PM", iClientTimeOffset, context.getLocale());
              createAfterDateEntered = true;
            }
            if (!CreateBeforeDate.equals(noDate)) {
              CreateBeforeDate = eMatrixDateFormat.getFormattedInputDateTime(context,CreateBeforeDate, "12:00:00 AM", iClientTimeOffset, context.getLocale());
              createBeforeDateEntered = true;
            }

            String busWhere = null;
            if ((Name != null) && !Name.equals(null) && !Name.equals("null") &&
              !Name.equals("*") && !Name.equals("")) {
              if (busWhere != null){
                busWhere += " && \"" + Document.SELECT_NAME + "\" ~~ const\"" + Name + "\"";
              } else {
                busWhere = "\"" + Document.SELECT_NAME + "\" ~~ const\"" + Name + "\"";
              }
            } //end if name is not null

            if ((Title != null) && !Title.equals(null) && !Title.equals("null") &&
              !Title.equals("*") && !Title.equals("")) {
              if (busWhere != null){
                busWhere += " && \"" + Document.SELECT_TITLE + "\" ~~ const\"" + Title + "\"";
              } else {
                busWhere = "\"" + Document.SELECT_TITLE + "\" ~~ const\"" + Title + "\"";
              }
            }

            if ((Owner != null) && !Owner.equals(null) && !Owner.equals("null") &&
              !Owner.equals("*") && !Owner.equals("")) {
              if (busWhere != null){
                busWhere += " && \"" + Document.SELECT_OWNER + "\" ~~ \"" + Owner + "\"";
              } else {
                busWhere = "\"" + Document.SELECT_OWNER + "\" ~~ \"" + Owner + "\"";
              }
            } //end if Owner is not null

            if (createAfterDateEntered == true) {
              //if there is no version date attribute use the originated date
                if (busWhere != null){
                  busWhere += " && \"" + Document.SELECT_ORIGINATED + "\" gt \"" + CreateAfterDate + "\"";
                } else {
                  busWhere = "\"" + Document.SELECT_ORIGINATED + "\" gt \"" + CreateAfterDate + "\"";
                }//ends else
            }//ends if

            if (createBeforeDateEntered == true) {
              //if there is no version date attribute use the originated date
                if (busWhere != null){
                  busWhere += " && \"" + Document.SELECT_ORIGINATED + "\" lt \"" + CreateBeforeDate + "\"";
                } else {
                  busWhere = "\"" + Document.SELECT_ORIGINATED + "\" lt \"" + CreateBeforeDate + "\"";
                }//ends else
            }

            //add clause to fetch only master documents in case of DOCUMENTS type
            if ((Type != null) && !"".equals(Type) && !"null".equals(Type)){
              BusinessType docType = null;
              if(!Type.equals(Document.TYPE_DOCUMENTS))
              {
                docType = getAllSubTypes(context, Document.TYPE_DOCUMENTS, null).find(Type);
              }
              if(Type.equals(Document.TYPE_DOCUMENTS) || docType != null){
                if (busWhere != null){
                busWhere += " && \""+CommonDocument.SELECT_IS_VERSION_OBJECT + "\" ~~ \"false\"";
                }else{
                  busWhere = "\""+CommonDocument.SELECT_IS_VERSION_OBJECT + "\" ~~ \"false\"";
                }
              }
            }
            //added this condition to not to show the objects which does not have to connect access
            if(busWhere != null) {
                busWhere += " && \"current.access[toconnect]\" == \"TRUE\"";
            } else {
                busWhere = "\"current.access[toconnect]\" == \"TRUE\"";
            }

            String typePattern = "";
            if ((Type != null) && !Type.equals(null) && !Type.equals("null") && !Type.equals("*") && !Type.equals("")) {
                typePattern = Type;
            } else {
                typePattern = (String)programMap.get("docTypesStr");
            }

            String namePattern = null;
            if ((Name != null) && !Name.equals(null) && !Name.equals("null") && !Name.equals("*") && !Name.equals("")) {
                namePattern = Name;
            }

            String ownerPattern = "*";
            if (Owner != null && !Owner.equals(null) && !Owner.equals("null") && !Owner.equals("*") && !Owner.equals("")) {
                ownerPattern = Owner;
            }

            StringList busSelects = new StringList (1);
            busSelects.add (Document.SELECT_ID);

            MapList queryResultList = DomainObject.findObjects(
                                context,        // eMatrix context
                                typePattern,    // type pattern
                                namePattern,    // name pattern
                                "*",            // revision pattern
                                ownerPattern,   // owner pattern
                                vaultPattern,   // vault pattern
                                busWhere,       // where expression
                                null,             // queryName
                                true,           // expand type
                                busSelects,     // object selects
                                Short.parseShort(queryLimit) // object Limit
                                );

           return queryResultList;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw ex;
        }
    }



    /**
     * This method is executed to get checkboxes column for deliverables search results.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */
    public Object getResultsCheckboxColumn(Context context, String[] args)
        throws Exception
    {
        try
        {
            HashMap programMap        = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap        = (HashMap) programMap.get("paramList");
            String  taskId          = (String) paramMap.get("objectId");
            MapList objectList =     (MapList)programMap.get("objectList");
            Pattern relPattern        = new Pattern("");

            String relTaskDeliverable = PropertyUtil.getSchemaProperty(context, "relationship_TaskDeliverable");
            relPattern.addPattern(relTaskDeliverable);
            DomainObject workflowTaskObject = DomainObject.newInstance(context, taskId);

            StringList typeSelects = new StringList(1);
            typeSelects.add(CommonDocument.SELECT_ID);
            StringList relSelects = new StringList(1);
            relSelects.add(CommonDocument.SELECT_RELATIONSHIP_ID);

            //Find the selected task deliverables.
            MapList deliverableList = workflowTaskObject.getRelatedObjects(context,
                                                          relPattern.getPattern(),
                                                          "*",
                                                          typeSelects,
                                                          relSelects,
                                                          false,
                                                          true,
                                                          (short)1,
                                                          null, //objectWhere
                                                          null, //relWhere,
                                                          null,
                                                          null,
                                                          null);

              //Storing all to objects in a stringlist
              StringList deliverableObjList = new StringList(deliverableList.size());
              Hashtable tempTable = null;
              for(Iterator itr = deliverableList.iterator(); itr.hasNext();) {
                  tempTable = (Hashtable)itr.next();
                  deliverableObjList.add((String)tempTable.get("id"));
              }

            Vector chkBoxList  = new Vector();
            String disabledCbx = "<img border=\"0\" src=\"../common/images/utilTreeCheckOffDisabled.gif\">";
            String prefixCbx = "<input type=\"checkbox\" name=\"emxTableRowId\" value=\"";
            String suffixCbx = "\" onclick=\"doCheckboxClick(this); doSelectAllCheck(this)\">";


            HashMap tempMap = null;
            //Looking objectList agianist deliverableObjList to display disabled check box image.
              for(Iterator itr = objectList.iterator(); itr.hasNext();) {
                  tempMap = (HashMap)itr.next();
                  String tempId = (String)tempMap.get("id");
                  if(deliverableObjList.contains(tempId)) {
                      chkBoxList.add(disabledCbx);
                  } else {
                      chkBoxList.add(prefixCbx+tempId+suffixCbx);
                  }
              }

            return chkBoxList;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw ex;
        }
    }

    /**
     * This method is executed to get all subtypes of given type and
     * adding them to give businesstypelist.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */
    public BusinessTypeList getAllSubTypes(Context context, String type, BusinessTypeList busTypeList)
        throws MatrixException
      {
         if (busTypeList == null) {
             busTypeList = new BusinessTypeList();
         }
         BusinessType busType = new BusinessType(type, context.getVault());
         busType.open(context);
         BusinessTypeList tempTypeList = busType.getChildren(context);
         Iterator itr = tempTypeList.iterator();
         while ( itr.hasNext() ) {
             BusinessType busChildType = (BusinessType) itr.next();
             busTypeList.addElement(busChildType);
             busTypeList = getAllSubTypes(context, busChildType.getName(),busTypeList);
         }
         busType.close(context);
         return busTypeList;
      }

    /**
     * This method is executed to get Task column for workflow rollup deliverables.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */

    public Object getRollupDeliverableTaskColumn(Context context, String[] args)
        throws Exception
    {
        try
        {
            HashMap programMap        = (HashMap) JPO.unpackArgs(args);
            MapList objectList        = (MapList)programMap.get("objectList");
            String relTaskDeliverable =  PropertyUtil.getSchemaProperty(context, "relationship_TaskDeliverable");
            String relWorkflowTask    =  PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
            String typeWorkflowTask   =  PropertyUtil.getSchemaProperty(context, "type_WorkflowTask");

//          Begin : Bug 346997 code modification
            Map paramList = (Map)programMap.get("paramList");
            boolean isExporting = (paramList.get("reportFormat") != null);
//          End : Bug 346997 code modification            
            
            String prefixTaskUrl = "<a href=\"JavaScript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=";
            String anchorEnd = "</a>&nbsp";
            String suffixTaskUrl = "', '930', '650', 'false', 'popup', '')\" class=\"object\">";
            String strImg = "<img border=0 src=../common/images/iconTreeToArrow.gif></img>&nbsp" ;
            StringBuffer sbfURL = null;
            StringBuffer tempURL = null;  //Used to form url of all objects if multiple tasks are connected.

            Vector taskList  = new Vector();
            Map tempMap = null;
            StringList taskListName = null;
            StringList taskListId = null;
            StringList taskListType = null;
            String taskName = "";
            String taskId = "";
            String taskType = "";
            String workflowName = "";
            String workflowId = "";
            String workflowType = "";
            int    loopCounter;
            DomainObject workflowTask = new DomainObject();
            StringList typeSelects = new StringList(2);
            typeSelects.add("to["+relWorkflowTask+"].from.id");
            typeSelects.add("to["+relWorkflowTask+"].from.name");
            Map workflowMap = null;

              for(Iterator itr = objectList.iterator(); itr.hasNext();) {
                  tempMap = (Map)itr.next();
                  sbfURL = new StringBuffer();
                  try {
                      //Workflow Task information
                      taskListId   = (StringList)tempMap.get("to["+relTaskDeliverable+"].from.id");
                      taskListName = (StringList)tempMap.get("to["+relTaskDeliverable+"].from.name");
                      taskListType = (StringList)tempMap.get("to["+relTaskDeliverable+"].from.type");

                      tempURL = new StringBuffer();
                      loopCounter = 0;

                      /* Forming hyperlinks for only Workflow Tasks objects.
                         Other task objects like Task Management will be ignored. */

                      //for(Iterator idItr = taskListId.iterator(),nameItr=taskListName.iterator(),typeItr=taskListType.iterator(); idItr.hasNext();) {
                      for(Iterator idItr = taskListId.iterator(); idItr.hasNext(); loopCounter++) {
                          taskId   = (String)idItr.next();
                          taskName = (String)taskListName.elementAt(loopCounter);  //(String)nameItr.next();
                          taskType = (String)taskListType.elementAt(loopCounter);  //(String)typeItr.next();


                          if(taskType.equals(typeWorkflowTask)) {
                              workflowTask.setId(taskId);
                              workflowMap = workflowTask.getInfo(context, typeSelects);
                              workflowId   = (String)workflowMap.get("to["+relWorkflowTask+"].from.id");
                              workflowName = (String)workflowMap.get("to["+relWorkflowTask+"].from.name");
                              
//                            Begin : Bug 346997 code modification
                              if (isExporting) {
//                                workflow hyperlink formation
                                  tempURL.append(workflowName);

                                  //arrow display forwardarrow.gif
                                  tempURL.append("->");

                                  //task hyperlink formation
                                  tempURL.append(taskName);
                              }
                              else {
//                                workflow hyperlink formation
                                  tempURL.append(prefixTaskUrl);
                                  tempURL.append(workflowId);
                                  tempURL.append(suffixTaskUrl);
                                  tempURL.append(workflowName);
                                  tempURL.append(anchorEnd);

                                  //arrow display forwardarrow.gif
                                  tempURL.append(strImg);

                                  //task hyperlink formation
                                  tempURL.append(prefixTaskUrl);
                                  tempURL.append(taskId);
                                  tempURL.append(suffixTaskUrl);
                                  tempURL.append(taskName);
                                  tempURL.append(anchorEnd);
                                  tempURL.append("<br>"); //To show each task in different line.
                              }
//                            End : Bug 346997 code modification
                              
                                                        } else {
                              tempURL.append("");
                          }
                      }
                      sbfURL.append(tempURL.toString());
                  }
                  catch (ClassCastException cce) {

                      taskName = (String)tempMap.get("to["+relTaskDeliverable+"].from.name");
                      taskId   = (String)tempMap.get("to["+relTaskDeliverable+"].from.id");
                      taskType = (String)tempMap.get("to["+relTaskDeliverable+"].from.type");

                      //To filter and show only Workflow Tasks
                      if(taskType.equals(typeWorkflowTask)) {

                          workflowTask.setId(taskId);
                          workflowMap = workflowTask.getInfo(context, typeSelects);
                          workflowId   = (String)workflowMap.get("to["+relWorkflowTask+"].from.id");
                          workflowName = (String)workflowMap.get("to["+relWorkflowTask+"].from.name");

//                        Begin : Bug 346997 code modification
                          if (isExporting) {
                              sbfURL.append(workflowName);

                              //arrow display forwardarrow.gif
                              sbfURL.append("->");

                              sbfURL.append(taskName);
                          }
                          else {
                              sbfURL.append(prefixTaskUrl);
                              sbfURL.append(workflowId);
                              sbfURL.append(suffixTaskUrl);
                              sbfURL.append(workflowName);
                              sbfURL.append(anchorEnd);

                              //arrow display forwardarrow.gif
                              sbfURL.append(strImg);

                              sbfURL.append(prefixTaskUrl);
                              sbfURL.append(taskId);
                              sbfURL.append(suffixTaskUrl);
                              sbfURL.append(taskName);
                              sbfURL.append(anchorEnd);
                          }
//                        End : Bug 346997 code modification
                          
                          
                      } else {
                          sbfURL.append("");
                      }
                  }

                  taskList.add(sbfURL.toString());
              }

            return taskList;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw ex;
        }
    }

        /**
          * Method call to update assignee of Workflow task chosen by the user in the form page.
          *
          * @param context - the eMatrix <code>Context</code> object
          * @param args - args contains a Map with the following entries:
          *             paramMap - the Map of the parameters
          *             New Value - The new Value of assingee Name
          * @return int - Returns 0 in case of Check trigger is success and 1 in case of failure
          * @throws Exception if the operation fails
          * @since Common 10.6 SP2
          * @grade 0
          */
        public int updateAssignee(Context context, String[] args) throws Exception
        {

                HashMap programMap = (HashMap) JPO.unpackArgs(args);
                HashMap paramMap = (HashMap) programMap.get("paramMap");
                String strObjectId = (String) paramMap.get("objectId");

                this.setId(strObjectId);
                /* Reading parameter "New OID" from paramMap is coming as empty
                so reading New value and forming person object. */
                String strNewValue = (String) paramMap.get("New Value");
                String relWorkflowTaskAssignee = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTaskAssignee");

                HashMap requestMap = (HashMap) programMap.get("requestMap");
                updateConnection(
                        context,
                        strObjectId,
                        relWorkflowTaskAssignee,
                        strNewValue,
                        true);
                return 0;
        }

        /**
          * Method call to update the relationship.
          *
          * @param context - the eMatrix <code>Context</code> object
          * @param parentObjectId - Id of Workflow Task
          * @param strRelationshipName - Relationship Name passed by the calling method
          * @param strNewObjectId - New Id of Person
          * @param bIsFrom - boolean value to indicate 'From' type
          * @throws Exception if the operation fails
          * @since Common 10.6 SP2
          * @grade 0
          */
        protected void updateConnection(
                Context context,
                String parentObjectId,
                String strRelationshipName,
                String strNewObjectId,
                boolean bIsFrom)
                throws Exception {

                StringList relSelect = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
                //Context set with the product id
                this.setId(parentObjectId);

                //Relationship id of the previous Workflow Task and Person is fetched for disconnecting it.
                Map objectMap = this.getRelatedObject(
                                    context,
                                    strRelationshipName,
                                    bIsFrom,
                                    null,
                                    relSelect);

                if (objectMap != null) {
                        String strRelId = (String) objectMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                        //The relationship is disconnected
                        DomainRelationship.disconnect(context, strRelId);
                }

                //Function loop to avoid the connection from being established if no objects is being passed.
                if (!(strNewObjectId.equals("") || strNewObjectId.equals("null") || strNewObjectId == null)) {

                      /*  Web form param map is returning selected object id as empty
                          so forming Person bussiness object with type and name
                      */
                      //BusinessObject tempBO = new BusinessObject(strNewObjectId);
		    String strType = PropertyUtil.getSchemaProperty(context,"type_Person");
                      BusinessObject tempBO = new BusinessObject(strType,strNewObjectId,"-","");
                      tempBO.open(context);
                      strNewObjectId = tempBO.getObjectId();
                      tempBO.close(context);

                      //The new Person object is connected to the context Workflow Task
                        this.addRelatedObject(context,
                                              new RelationshipType(strRelationshipName),
                                              false,
                                              strNewObjectId);
                }
        }

        /**
          * This method returns Content Search Results Found as a MapList object
          *
          * @param context the eMatrix <code>Context</code> object
          * @param args holds no arguments
          * @returns MapList
          * @throws Exception if the operation fails
          * @since AEF 10.6 SP2
          */
        public MapList getContents(Context context, String args[]) throws Exception
        {

            HashMap programMap = (HashMap)JPO.unpackArgs(args);

            // Get search parameters
            String selType = (String)programMap.get("selType");
            String txtName = (String)programMap.get("txtName");
            String txtRev = (String)programMap.get("txtRev");
            String txtDescription = (String)programMap.get("txtDesc");
            String strObjectId = (String)programMap.get("objectId");

            SelectList resultSelects = new SelectList(6);
            resultSelects.add(DomainObject.SELECT_ID);
            //this.setId(strObjectId);

            String sWhereExp = "";
            String sAnd = "&&";
            String sOr = "||";
            char chDblQuotes = '\"';
            if (txtName==null || txtName.equalsIgnoreCase("null") || txtName.length()<=0) {
                txtName = "*";
            }
            if (txtRev==null || txtRev.equalsIgnoreCase("null") || txtRev.length()<=0)  {
                txtRev = "*";
            }
            if (txtDescription!=null && !txtDescription.equalsIgnoreCase("null") && txtDescription.equals("*"))  {
                txtDescription = "";
            }
            if (!(txtDescription == null || txtDescription.equalsIgnoreCase("null") || txtDescription.length() <= 0 ))  {
                String sDescQuery = "description ~~ " + chDblQuotes + txtDescription + chDblQuotes;
                if (sWhereExp == null || sWhereExp.equalsIgnoreCase("null") || sWhereExp.length()<=0 )  {
                    sWhereExp = sDescQuery;
                }
                else  {
                    sWhereExp += sAnd + " " + sDescQuery;
                }
            }
            if (selType.equals(DomainObject.TYPE_DOCUMENT))  {
                String strVersionObjectAttr = PropertyUtil.getSchemaProperty(context,"attribute_IsVersionObject");
                if ((sWhereExp == null) || (sWhereExp.equalsIgnoreCase("null")) || (sWhereExp.length()<=0 ))  {
                    sWhereExp = "(attribute[" + strVersionObjectAttr + "] == False)";
                }
                else  {
                    sWhereExp += sAnd + " " + "(attribute[" + strVersionObjectAttr + "] == False)";
                }
            }

            MapList searchResults = null;
            com.matrixone.apps.common.Person person=  com.matrixone.apps.common.Person.getPerson(context);
            Company company = person.getCompany(context);
            String companyVault=company.getVault();
            String SecondaryVaults = company.getSecondaryVaults(context);
            String Vaults = companyVault;
            if(SecondaryVaults != null)  {
                Vaults = Vaults+","+SecondaryVaults;
            }
            searchResults =DomainObject.querySelect(context,selType,txtName,txtRev,"*",Vaults,sWhereExp,true,resultSelects,null,false);
            return searchResults ;
        }

        /**
        * This method determines checkbox is enabled or disabled in Add content Search results of Workflow
        *
        * @param context the eMatrix <code>Context</code> object
        * @param args holds no arguments
        * @returns MapList
        * @throws Exception if the operation fails
        * @since AEF 10.6 SP2
        */
        public Vector showAddContentCheckbox(Context context, String args[]) throws Exception
        {
            Vector connectedObjVector = null;
            java.util.HashMap programMap=(java.util.HashMap)JPO.unpackArgs(args);
            HashMap map = (HashMap)programMap.get("paramList");
            String strObjectId = (String)map.get("objectId");
            if(strObjectId != null && !"null".equalsIgnoreCase(strObjectId) && !"".equalsIgnoreCase(strObjectId)) {
                this.setId(strObjectId);
                String relWorkflowContent = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowContent");
                Pattern relPattern        = new Pattern("");
                relPattern.addPattern(relWorkflowContent);

                // build select params
                StringList selListObj = new SelectList();
                selListObj.add(DomainConstants.SELECT_ID);
                StringList relSelects = new StringList(1);
                relSelects.add(CommonDocument.SELECT_RELATIONSHIP_ID);
                MapList connectedObjsList = this.getRelatedObjects(context,
                                                              relPattern.getPattern(),
                                                              "*",
                                                              selListObj,
                                                              relSelects,
                                                              false,
                                                              true,
                                                              (short)1,
                                                              null, //objectWhere
                                                              null, //relWhere,
                                                              null,
                                                              null,
                                                              null);

                if(connectedObjsList.size() != 0) {
                    connectedObjVector = new Vector();
                    Map connectedMap = null;
                    String connectedObjectId = null;
                    for(Iterator connectedObjsItr = connectedObjsList.iterator(); connectedObjsItr.hasNext();)  {
                        connectedMap = (Map)connectedObjsItr.next();
                        connectedObjectId = (String)connectedMap.get(DomainConstants.SELECT_ID);
                        connectedObjVector.add(connectedObjectId);
                    } // end of for(Iterator..
                } // end of if(connectedObjsList.size()
            }else {
                connectedObjVector = new Vector();
            }

            String disabledCbx = "<img border=\"0\" src=\"../common/images/utilTreeCheckOffDisabled.gif\">";
            String prefixCbx = "<input type=\"checkbox\" name=\"emxTableRowId\" value=\"";
            String suffixCbx = "\" onclick=\"doCheckboxClick(this); doSelectAllCheck(this)\">";
            Vector checkboxVector=new Vector();

            MapList objectList=(MapList)programMap.get("objectList");
            String objectId = null;
            ListIterator templateIterator=objectList.listIterator();
            for (; templateIterator.hasNext() ; )  {
                    Object obj = templateIterator.next();
                    if (obj instanceof HashMap) {
                        objectId = (String)((HashMap)obj).get(DomainConstants.SELECT_ID);
                    }
                    else if (obj instanceof Hashtable)
                    {
                        objectId = (String)((Hashtable)obj).get(DomainConstants.SELECT_ID);
                    }
                if(!(connectedObjVector != null && connectedObjVector.contains(objectId)) ) {
                    checkboxVector.add(prefixCbx+objectId+suffixCbx);
                } else {
                    checkboxVector.add(disabledCbx);
                }
            }
            return checkboxVector ;
        }


/**
     * This method is executed to get Workflows which are not yet started.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */
     @com.matrixone.apps.framework.ui.ProgramCallable
     public Object getMyworkflows(Context context, String[] args)
        throws Exception
     {
         try
         {
             String loggedInUser  = com.matrixone.apps.common.Person.getPerson(context).getName(context);
             String typePattern   = PropertyUtil.getSchemaProperty(context,"type_Workflow");
             HashMap programMap   = (HashMap) JPO.unpackArgs(args);
             HashMap paramMap     = (HashMap) programMap.get("paramList");
             String objectId      = (String)programMap.get("objectId");
             MapList workflowList = null;

            StringList typeSelects = new StringList(4);
            typeSelects.add(DomainObject.SELECT_ID);
            typeSelects.add(DomainObject.SELECT_NAME);
            typeSelects.add(DomainObject.SELECT_DESCRIPTION);
            typeSelects.add(DomainObject.SELECT_CURRENT);
            typeSelects.add("attribute["+PropertyUtil.getSchemaProperty(context,"attribute_DueDate")+"]");
            typeSelects.add("attribute["+ATTRIBUTE_ACTUAL_COMPLETION_DATE +"]");

            StringList relSelects     =null ;
            String relWorkflowContent = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowContent");
            Pattern relPattern        = new Pattern("");
            relPattern.addPattern(relWorkflowContent);



            if(objectId!=null && !"null".equalsIgnoreCase(objectId) && !"".equalsIgnoreCase(objectId))
            {
                    DomainObject domainObj = DomainObject.newInstance(context, objectId);
                    workflowList           = domainObj.getRelatedObjects(context,
                                                          relPattern.getPattern(),
                                                          "*",
                                                          typeSelects,
                                                          null,
                                                          true,
                                                          false,
                                                          (short)1,
                                                          null, //objectWhere
                                                          null //relWhere,
                                                          );
            }
            else
            {
                 workflowList = DomainObject.findObjects(context,
                                                            typePattern,
                                                            QUERY_WILDCARD, //namepattern
                                                            QUERY_WILDCARD, //revpattern
                                                            loggedInUser, //owner pattern
                                                            QUERY_WILDCARD, //vault pattern
                                                            null, //where exp
                                                            false,
                                                            typeSelects);
            }
           return workflowList;
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }



     @com.matrixone.apps.framework.ui.ProgramCallable
     public Object getNotStartedWorkflows(Context context, String[] args)
     throws Exception
  {
      try
      {
          String loggedInUser  = com.matrixone.apps.common.Person.getPerson(context).getName(context);
          String typePattern   = PropertyUtil.getSchemaProperty(context,"type_Workflow");
          HashMap programMap   = (HashMap) JPO.unpackArgs(args);
          HashMap paramMap     = (HashMap) programMap.get("paramList");
          String objectId      = (String)programMap.get("objectId");
          MapList workflowList = null;

         StringList typeSelects = new StringList(4);
         typeSelects.add(DomainObject.SELECT_ID);
         typeSelects.add(DomainObject.SELECT_NAME);
         typeSelects.add(DomainObject.SELECT_DESCRIPTION);
         typeSelects.add(DomainObject.SELECT_CURRENT);
         typeSelects.add("attribute["+PropertyUtil.getSchemaProperty(context,"attribute_DueDate")+"]");

         StringList relSelects     =null ;
         String relWorkflowContent = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowContent");
         Pattern relPattern        = new Pattern("");
         relPattern.addPattern(relWorkflowContent);

         String policy             =  PropertyUtil.getSchemaProperty(context,"policy_Workflow");
         String stateStopped      =   FrameworkUtil.lookupStateName(context,policy,"state_Stopped");

         if(objectId!=null && !"null".equalsIgnoreCase(objectId) && !"".equalsIgnoreCase(objectId))
         {
                 DomainObject domainObj = DomainObject.newInstance(context, objectId);
                 workflowList           = domainObj.getRelatedObjects(context,
                                                       relPattern.getPattern(),
                                                       "*",
                                                       typeSelects,
                                                       null,
                                                       true,
                                                       false,
                                                       (short)1,
                                                       "current=="+stateStopped, //objectWhere
                                                       null //relWhere,
                                                       );
         }
         else
         {
              workflowList = DomainObject.findObjects(context,
                                                         typePattern,
                                                         QUERY_WILDCARD, //namepattern
                                                         QUERY_WILDCARD, //revpattern
                                                         loggedInUser, //owner pattern
                                                         QUERY_WILDCARD, //vault pattern
                                                         "current=="+stateStopped, //where exp
                                                         false,
                                                         typeSelects);
         }
        return workflowList;
     }
     catch (Exception ex)
     {
         throw ex;
     }
 }

    /**
     * This method is executed to get Workflows which are in suspended state.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */

      @com.matrixone.apps.framework.ui.ProgramCallable
      public Object getPausedWorkflows(Context context, String[] args)
        throws Exception
     {
        try
        {
            String loggedInUser  = com.matrixone.apps.common.Person.getPerson(context).getName(context);
            String typePattern   = PropertyUtil.getSchemaProperty(context,"type_Workflow");

            HashMap programMap   = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap     = (HashMap) programMap.get("paramList");
            String objectId      = (String)programMap.get("objectId");
            MapList workflowList = null;

            StringList typeSelects = new StringList(4);
            typeSelects.add(DomainObject.SELECT_ID);
            typeSelects.add(DomainObject.SELECT_NAME);
            typeSelects.add(DomainObject.SELECT_DESCRIPTION);
            typeSelects.add(DomainObject.SELECT_CURRENT);
            typeSelects.add("attribute["+PropertyUtil.getSchemaProperty(context,"attribute_DueDate")+"]");
            StringList relSelects     =null ;
            String relWorkflowContent = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowContent");
            Pattern relPattern        = new Pattern("");
            relPattern.addPattern(relWorkflowContent);

            String policy             =  PropertyUtil.getSchemaProperty(context,"policy_Workflow");
            String stateSuspended      =   FrameworkUtil.lookupStateName(context,policy,"state_Suspended");

            if(objectId!=null && !"null".equalsIgnoreCase(objectId) && !"".equalsIgnoreCase(objectId))
            {
                    DomainObject domainObj = DomainObject.newInstance(context, objectId);
                    workflowList           = domainObj.getRelatedObjects(context,
                                                          relPattern.getPattern(),
                                                          "*",
                                                          typeSelects,
                                                          null,
                                                          true,
                                                          false,
                                                          (short)1,
                                                          "current=="+stateSuspended, //objectWhere
                                                          null //relWhere,
                                                          );
            }
            else
            {
                 workflowList = DomainObject.findObjects(context,
                                                            typePattern,
                                                            QUERY_WILDCARD, //namepattern
                                                            QUERY_WILDCARD, //revpattern
                                                            loggedInUser, //owner pattern
                                                            QUERY_WILDCARD, //vault pattern
                                                            "current=="+stateSuspended, //where exp
                                                            false,
                                                            typeSelects);
            }
           return workflowList;
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }

    /**
     * This method is executed to get Workflows which are in process.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */

     @com.matrixone.apps.framework.ui.ProgramCallable
     public Object getInprocessWorkflows(Context context, String[] args)
        throws Exception
    {
        try
        {
            String loggedInUser   =  com.matrixone.apps.common.Person.getPerson(context).getName(context);
            String typePattern    =  PropertyUtil.getSchemaProperty(context,"type_Workflow");

            HashMap programMap    = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap      = (HashMap) programMap.get("paramList");
            String objectId       = (String)programMap.get("objectId");
            MapList workflowList  = null;

            StringList typeSelects = new StringList(4);
            typeSelects.add(DomainObject.SELECT_ID);
            typeSelects.add(DomainObject.SELECT_NAME);
            typeSelects.add(DomainObject.SELECT_DESCRIPTION);
            typeSelects.add(DomainObject.SELECT_CURRENT);
            typeSelects.add("attribute["+PropertyUtil.getSchemaProperty(context,"attribute_DueDate")+"]");

            StringList relSelects     = null ;
            String relWorkflowContent = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowContent");
            Pattern relPattern        = new Pattern("");
            relPattern.addPattern(relWorkflowContent);

            String policy             =  PropertyUtil.getSchemaProperty(context,"policy_Workflow");
            String stateStarted      =   FrameworkUtil.lookupStateName(context,policy,"state_Started");

            if(objectId!=null && !"null".equalsIgnoreCase(objectId) && !"".equalsIgnoreCase(objectId))
            {
                    DomainObject domainObj = DomainObject.newInstance(context, objectId);
                    workflowList           = domainObj.getRelatedObjects(context,
                                                          relPattern.getPattern(),
                                                          "*",
                                                          typeSelects,
                                                          null,
                                                          true,
                                                          false,
                                                          (short)1,
                                                          "current=="+stateStarted, //objectWhere
                                                          null //relWhere,
                                                          );
            }
            else
            {
                 workflowList = DomainObject.findObjects(context,
                                                            typePattern,
                                                            QUERY_WILDCARD, //namepattern
                                                            QUERY_WILDCARD, //revpattern
                                                            loggedInUser, //owner pattern
                                                            QUERY_WILDCARD, //vault pattern
                                                            "current=="+stateStarted, //where exp
                                                            false,
                                                            typeSelects);
            }
           return workflowList;
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }



     /**
     * This method is executed to get Workflows completed in given 30 days.
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */
      @com.matrixone.apps.framework.ui.ProgramCallable
      public Object getCompletedWorkflowsInLast30Days(Context context, String[] args)
        throws Exception
    {
       return getCompletedWorkflowsInLastNumOfDays(context,args,30);
    }


     /**
     * This method is executed to get Workflows completed in given 60 days.
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */

      @com.matrixone.apps.framework.ui.ProgramCallable
      public Object getCompletedWorkflowsInLast60Days(Context context, String[] args)
        throws Exception
    {
        return getCompletedWorkflowsInLastNumOfDays(context,args,60);
    }

     /**
     * This method is executed to get Workflows completed in given 90 days.
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */
      @com.matrixone.apps.framework.ui.ProgramCallable
      public Object getCompletedWorkflowsInLast90Days(Context context, String[] args)
        throws Exception
    {
       return getCompletedWorkflowsInLastNumOfDays(context,args,90);
    }



    /**
     * This method is executed to get Workflows completed in given number of days.
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     * @grade 0
     */
      public Object getCompletedWorkflowsInLastNumOfDays(Context context, String[] args,int days)
        throws Exception
    {
        try
        {
            String loggedInUser   =  com.matrixone.apps.common.Person.getPerson(context).getName(context);
            String typePattern    =  PropertyUtil.getSchemaProperty(context,"type_Workflow");
            HashMap programMap    =  (HashMap) JPO.unpackArgs(args);
            HashMap paramMap      =  (HashMap) programMap.get("paramList");
            String objectId       =  (String)programMap.get("objectId");

            MapList workflowList  = null;
            int day1=0-days;
            StringList typeSelects = new StringList(4);
            typeSelects.add(DomainObject.SELECT_ID);
            typeSelects.add(DomainObject.SELECT_NAME);
            typeSelects.add(DomainObject.SELECT_DESCRIPTION);
            typeSelects.add(DomainObject.SELECT_CURRENT);
            typeSelects.add("attribute["+PropertyUtil.getSchemaProperty(context,"attribute_DueDate")+"]");
            typeSelects.add("attribute["+ATTRIBUTE_ACTUAL_COMPLETION_DATE +"]");

            MapList returnWorkflowList =  new MapList();
            StringList relSelects      =  null ;
            String relWorkflowContent  =  PropertyUtil.getSchemaProperty(context, "relationship_WorkflowContent");
            Pattern relPattern         =  new Pattern("");
            relPattern.addPattern(relWorkflowContent);
            String policy              =  PropertyUtil.getSchemaProperty(context,"policy_Workflow");
            String stateCompleted      =  FrameworkUtil.lookupStateName(context,policy,"state_Completed");

            GregorianCalendar cal      =  new GregorianCalendar();
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH,day1);
            String dateNumofDaysBack = (cal.get(Calendar.MONTH)+1)+"/"+cal.get(Calendar.DAY_OF_MONTH)+"/"+cal.get(Calendar.YEAR);


            if(objectId!=null && !"null".equalsIgnoreCase(objectId) && !"".equalsIgnoreCase(objectId))
            {
                    DomainObject domainObj = DomainObject.newInstance(context, objectId);
                    workflowList = domainObj.getRelatedObjects(context,
                                                          relPattern.getPattern(),
                                                          "*",
                                                          typeSelects,
                                                          null,
                                                          true,
                                                          false,
                                                          (short)1,
                                                           "current=="+stateCompleted+"&& \"attribute["+ATTRIBUTE_ACTUAL_COMPLETION_DATE +"]\" > \""+dateNumofDaysBack+"\"", //objectWhere
                                                          null //relWhere,
                                                         );
            }
            else
            {
                   workflowList = DomainObject.findObjects(context,
                                                            typePattern,
                                                            QUERY_WILDCARD, //namepattern
                                                            QUERY_WILDCARD, //revpattern
                                                            loggedInUser, //owner pattern
                                                            QUERY_WILDCARD, //vault pattern
                                                            "current=="+stateCompleted+"&& \"attribute["+ATTRIBUTE_ACTUAL_COMPLETION_DATE +"]\" > \""+dateNumofDaysBack+"\"" , //where exp
                                                            false,
                                                            typeSelects);
            }

            int index       = 0;
            int size        = workflowList.size();
            Map workflowMap = null;

          return workflowList;
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }

     /**
     * getWorkflowStatus - gets the status of workflow
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object
     * @throws Exception if the operation fails
     *
     */
    public Vector getWorkflowStatus(Context context, String[] args)
        throws Exception
    {
        try
        {
            HashMap programMap        = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap          = (HashMap) programMap.get("paramList");
            MapList workflowList      = (MapList)programMap.get("objectList");
            String loggedInUser       = com.matrixone.apps.common.Person.getPerson(context).getName(context);
            String typePattern        = PropertyUtil.getSchemaProperty(context,"type_Workflow");
            Vector StatusIcons        = new Vector();
            String eMatrixDueDate     = "";
            Date todaysDate           = new Date();
            Date dueDate              = new Date();
            Date actualCompletionDate = new Date();
            String currentState       = "";
            MapList returnWorkflowList= new MapList();
            int index                 = 0;
            int size                  = workflowList.size();
            Map workflowMap           = null;
            String eMatrixActualCompletionDate = "";
            String policy             =  PropertyUtil.getSchemaProperty(context,"policy_Workflow");
            String stateCompleted     =   FrameworkUtil.lookupStateName(context,policy,"state_Completed");

            while(index<size)
            {
                 workflowMap    = (Map) workflowList.get(index);
                 eMatrixDueDate = (String) workflowMap.get("attribute["+PropertyUtil.getSchemaProperty(context,"attribute_DueDate")+"]");
                 String name = (String) workflowMap.get(DomainObject.SELECT_NAME);
                 //dueDate        = new Date(eMatrixDueDate);
                 currentState   = (String) workflowMap.get(DomainObject.SELECT_CURRENT);
                if(eMatrixDueDate!=null && !"null".equalsIgnoreCase(eMatrixDueDate)&& !"".equalsIgnoreCase(eMatrixDueDate))
                {
                	//dueDate        = new Date(eMatrixDueDate);
                    dueDate = com.matrixone.apps.domain.util.eMatrixDateFormat.getJavaDate(eMatrixDueDate);
                    if(!currentState.equalsIgnoreCase(stateCompleted))
                    {
                        if(todaysDate.after(dueDate))
                        {
                            StatusIcons.add("<img border='0' src='../common/images/iconStatusRed.gif' name='Red' id='Red' alt='*' />");
                        }
                        else if(todaysDate.before(dueDate))
                        {
                             StatusIcons.add("<img border='0' src='../common/images/iconStatusGreen.gif' name='Green' id='Green' alt='*' />");
                        }
                    }
                    else
                    {
                        eMatrixActualCompletionDate = (String) workflowMap.get("attribute["+ATTRIBUTE_ACTUAL_COMPLETION_DATE +"]");
                        actualCompletionDate  = com.matrixone.apps.domain.util.eMatrixDateFormat.getJavaDate(eMatrixActualCompletionDate);
                        if(actualCompletionDate.after(dueDate))
                        {
                             StatusIcons.add("<img border='0' src='../common/images/iconStatusRed.gif' name='Red' id='Red' alt='*' />");
                        }
                        else if(actualCompletionDate.before(dueDate))
                        {
                             StatusIcons.add("<img border='0' src='../common/images/iconStatusGreen.gif' name='Green' id='Green' alt='*' />");
                        }
                    }
                }
                else
                {
                    StatusIcons.add("<img border='0' src='../common/images/iconStatusGreen.gif' name='Green' id='Green' alt='*' />");
                }
                index++;
            }

          return StatusIcons;
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }

         /*@param         context the eMatrix <code>Context</code> object
             * @param         args holds the HashMap containing the following arguments
             *  queryLimit    limit for displaying search query results
             *  User Name     the user name or name pattern of the Person search for
             *  First Name    the first name or name pattern of the Person search for
             *  Last Name     the last name or name pattern of the Person search for
             *  Company       the company name or name pattern of the Person search for
             *  Vault Option  the vault search option
             *  Vault Display the vault display search option
             * @return        MapList , the Object ids matching the search criteria
             * @throws        Exception if the operation fails
             *
             */
            @com.matrixone.apps.framework.ui.ProgramCallable
            public static MapList getPersons(Context context, String[] args)
              throws Exception
              {

                Map programMap = (Map) JPO.unpackArgs(args);
                String strType = DomainConstants.TYPE_PERSON;
                String strName = (String)programMap.get("User Name");
                String strFirstName = (String)programMap.get("First Name");
                String strLastName = (String)programMap.get("Last Name");
                String strCompany = (String)programMap.get("Company");
                StringList select = new StringList(1);
                select.addElement(DomainConstants.SELECT_ID);
                String strUserName    = strName;
                String whereClause = null;
                String firstname = PropertyUtil.getSchemaProperty(context,"attribute_FirstName");
                String lastname = PropertyUtil.getSchemaProperty(context,"attribute_LastName");
                String sPersonActiveState = PropertyUtil.getSchemaProperty(context,"policy", DomainConstants.POLICY_PERSON, "state_Active");
                String employeerel = PropertyUtil.getSchemaProperty(context,"relationship_Employee");
                String typePattern = PropertyUtil.getSchemaProperty(context,"type_Person");
                String strCompanyName = (String)programMap.get("Company");
                select.addElement(firstname);
                select.addElement(lastname);

                whereClause = "(" + "current" + " == " + "\"" + sPersonActiveState + "\")";

                if (!strUserName.equals("*"))
                {
                    whereClause += " && (" + "name" + " ~= " + "\"" + strUserName + "\")";
                }

               if (!strFirstName.equals("*"))
               {
                    whereClause += " && (\"" + "attribute["+firstname+"]" + "\"" + " ~= " + "\"" + strFirstName + "\")";
              }

              if (!strLastName.equals("*"))
             {
                 whereClause += " && (\"" + "attribute["+lastname+"]" + "\"" + " ~= " + "\"" + strLastName + "\")";
             }

             MapList mapList = new MapList();
            if(!(strCompanyName.equals("*") || "".equals(strCompanyName)))
            {
               mapList = (new Company(strCompanyName)).getPersons(context, select, whereClause);
           }
           else
           {
               whereClause += " && " + "(" + "to[" + employeerel + "] == True)";
               mapList = DomainObject.findObjects(context, typePattern, "*", whereClause, select);
           }
            return mapList;
        }


     /**
     * getProcessAttributes - Displas the process attributes
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @returns String
     * @throws Exception if the operation fails
     * @since Common 10.6 SP2
     */

    public String getProcessAttributes(Context context, String[] args)
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
        _localDateFormat = ((java.text.SimpleDateFormat)java.text.DateFormat.getDateInstance(eMatrixDateFormat.getEMatrixDisplayDateFormat(),
                            (Locale)_requestMap.get("localeObj"))).toLocalizedPattern();
        _timeZone = (String)requestMap.get("timeZone");

        if(fieldMap != null) {
            _settingMap = (HashMap) fieldMap.get("settings");
        }

        try
        {
            //Get the process associated with workflow
            setId(objectId);
            String result = getInfo(context,strProcess);



              String cmd = "print workflow \""+result+"\""+ " "+"\""+getName()+"\""+ " select attribute[].value";
              HashMap processMap = processMql(context,cmd);
              result = (String)processMap.get("result");
              // Tested by assigning the mql output is like below code
              if(result!=null && !"".equals(result)) {
                  StringTokenizer st = new StringTokenizer(result,"\n");
                  st.nextToken();
                  while(st.hasMoreTokens()) {
                      String token = (String)st.nextToken();
                      String attrName  = token.substring(token.indexOf("[")+1,token.indexOf("]"));
                      String attrValue = extractVal(token.substring(token.indexOf("=")+1,token.length()));
                      if ("edit".equalsIgnoreCase(mode)) {
                          finalHTML.append(getFieldEditHTML(attrName,attrValue));
                      } else {
                          finalHTML.append(getFieldViewHTML(attrName,attrValue));
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
        String[] strArr = {};
        HashMap field = new HashMap();
        field.put("settings", settings);
        field.put("field_value", attributeValue);
        field.put("hasAccess", "true");
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

    public void updateProcessAttributes(Context context, String[] args)
        throws Exception
    {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap   = (HashMap) programMap.get("paramMap");
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        String  objectId   = (String) paramMap.get("objectId");

        try
        {
            setId(objectId);
            String process = getInfo(context,strProcess);
            String []params = new String[1];
            params[0] = process;
            emxProcess_mxJPO processJPO = new emxProcess_mxJPO(context,params);
            HashMap processMap =processJPO.getProcessInfo(context,params);
            StringList attributes = (StringList)processMap.get("attributes");

            //Read the attributes and values from request map and construct
            //the mql command attribute clause
            StringBuffer mQLAttrClass = new StringBuffer();
            if(attributes != null)
            {
                int size = attributes.size();
                for(int i=0; i<size; i++) {
                    String attrName = (String)attributes.get(i);
                    String  attrValue = extractVal(requestMap.get("#1,"+attrName));
                    //173401 starts here
                    AttributeType objAttrType = new AttributeType(attrName);
        		    objAttrType.open(context);
        	            String isDateAttr = objAttrType.getDataType();
        		    objAttrType.close(context);
                   	    if( !UIUtil.isNullOrEmpty(attrValue) && "timestamp".equals(isDateAttr))
                            {
                                double iClientTimeOffset = (new Double((String) requestMap.get("timeZone"))).doubleValue();
                                Locale locale = (Locale)requestMap.get("localeObj");
        			attrValue = eMatrixDateFormat.getFormattedInputDate(context, attrValue, iClientTimeOffset, locale);
                             }
                   	//173401 ends here    
                    mQLAttrClass.append(" \"");
                    mQLAttrClass.append(attrName);
                    mQLAttrClass.append("\"");
                    mQLAttrClass.append(" ");
                    mQLAttrClass.append("\"");
                    mQLAttrClass.append(attrValue);
                    mQLAttrClass.append("\"");
                }
            }


            String cmd = "modify workflow \""+ process+"\""+" "+"\""+getName()+"\"" + mQLAttrClass.toString();
            MQLCommand mql = new MQLCommand();
            mql.executeCommand(context, cmd);
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

    public static void acceptTasks(Context context, String objectId) throws Exception
    {
        Pattern relPattern        = new Pattern("");
        String relWorkflowTask = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowTask");
        relPattern.addPattern(relWorkflowTask);
        String attrActivity = PropertyUtil.getSchemaProperty(context, "attribute_Activity");
        String sStateStarted = PropertyUtil.getSchemaProperty(context, "policy",
                                                                PropertyUtil.getSchemaProperty(context, "policy_WorkflowTask"), "state_Started");
        String sStateAssigned = PropertyUtil.getSchemaProperty(context, "policy",
                PropertyUtil.getSchemaProperty(context, "policy_WorkflowTask"), "state_Assigned");

        DomainObject obj = new DomainObject(objectId);
        String processName = obj.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_Process"));
        String workflowName = obj.getInfo(context, "name");
        StringList typeSelects = new StringList(4);
        typeSelects.add(CommonDocument.SELECT_ID);
        typeSelects.add("attribute[" + attrActivity + "]");
        typeSelects.add(DomainObject.SELECT_OWNER);
        typeSelects.add(DomainObject.SELECT_CURRENT);
        StringList relSelects = new StringList();
        MapList tasksList = obj.getRelatedObjects(context,
                                                      relPattern.getPattern(),
                                                      PropertyUtil.getSchemaProperty(context, "type_WorkflowTask"),
                                                      typeSelects,
                                                      relSelects,
                                                      false,
                                                      true,
                                                      (short)1,
                                                      null, //null, //objectWhere
                                                      null, //relWhere,
                                                      null,
                                                      null,
                                                      null);

        if(tasksList != null && tasksList.size() > 0)
        {
            Iterator itr = tasksList.iterator();
            while(itr.hasNext())
            {
                Map map = (Map)itr.next();
                String taskId = (String)map.get(CommonDocument.SELECT_ID);
                String taskAssignee = (String)map.get(DomainObject.SELECT_OWNER);
                String activityName = (String)map.get("attribute[" + attrActivity + "]");
                String currState = (String)map.get(DomainObject.SELECT_CURRENT);
                currState = currState == null ? "" : currState;
                taskAssignee = taskAssignee == null ? "" : taskAssignee;

                StringBuffer buffer = new StringBuffer(128);
                buffer.append("print workflow \"");
                buffer.append(processName);
                buffer.append("\" \"");
                buffer.append(workflowName);
                buffer.append("\" select interactive[");
                buffer.append(activityName);
                buffer.append("].assignee dump");

                String assignee = (MqlUtil.mqlCommand(context, buffer.toString())).trim();
                //Bug No 318930 executed MQl command to get type(Person, Role, Group) of assignee
                // and added the if condition to check "if assignee is person then only he can
                //promote task to assigned state
                String userType = MqlUtil.mqlCommand(context, "list user \"" + assignee + "\" select name isaperson dump ~");
                // Added for the Bug 336756 
                // Here the code is changed to parse the assignee string (i.e., split on ',') and handle multiple entries
                StringList userList = FrameworkUtil.split(userType, "\n");
                Iterator userItr = userList.iterator();
                String userName;
                while(userItr.hasNext()){
                    userName = (String) userItr.next();
                    String [] usrName = StringUtils.split(userName, "~");
                    String personName = usrName[0];
                    String isaPerson = usrName[1];

                    if (personName != null && "TRUE".equalsIgnoreCase(isaPerson))
                {
                    if(currState.equals(sStateStarted) && assignee.equals(taskAssignee))
                    {
                        try
                        {
                            ContextUtil.pushContext(context, taskAssignee, null, null);

                            DomainObject taskObj = new DomainObject(taskId);

                            StringBuffer sBuf = new StringBuffer(128);
                            sBuf.append("modify workflow \"");
                            sBuf.append(processName);
                            sBuf.append("\" \"");
                            sBuf.append(workflowName);
                            sBuf.append("\" interactive \"");
                            sBuf.append(activityName);
                            sBuf.append("\" acceptactivity");
                            HashMap resMap = processMql(context, sBuf.toString());
                            String error = (String)resMap.get("error");
                            if(error != null)
                            {
                                throw new Exception(error);
                            }
                            taskObj.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_BranchTo"), "Assigned");
                            taskObj.promote(context);
                            
                            String attrActualCompDate = PropertyUtil.getSchemaProperty(context, "attribute_ActualCompletionDate");
                            StringList objSelects = new StringList(2);
                            objSelects.addElement(DomainConstants.SELECT_CURRENT);
                            objSelects.addElement("attribute["+attrActualCompDate+"].value");
                            Map taskInfo = taskObj.getInfo(context, objSelects);
                            String curState = (String)taskInfo.get(DomainConstants.SELECT_CURRENT);
                            String sAttrActualCompDateValue = (String)taskInfo.get("attribute["+attrActualCompDate+"].value");
                            if(sStateAssigned.equals(curState) && !UIUtil.isNullOrEmpty(sAttrActualCompDateValue)) {
                            	taskObj.setAttributeValue(context, attrActualCompDate, "");
                            }
                                break;
                        }
                        catch (Exception e)
                        {
                            throw e;
                        }
                        finally
                        {
                            ContextUtil.popContext(context);
                        }

                        }
                    }
                }
            }
        }
    }

    public static void acceptTasksForSubProcess(Context context, String objectId) throws Exception
    {
        Pattern relPattern        = new Pattern("");
        String relWorkflowSubProcess = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowSubProcess");
        relPattern.addPattern(relWorkflowSubProcess);

        DomainObject obj = new DomainObject(objectId);
        StringList typeSelects = new StringList(1);
        typeSelects.add(DomainObject.SELECT_ID);
        StringList relSelects = new StringList();
        MapList wfList = obj.getRelatedObjects(context,
                                                      relPattern.getPattern(),
                                                      PropertyUtil.getSchemaProperty(context, "type_Workflow"),
                                                      typeSelects,
                                                      relSelects,
                                                      true,
                                                      true,
                                                      (short)0,
                                                      null, //null, //objectWhere
                                                      null, //relWhere,
                                                      null,
                                                      null,
                                                      null);

        if(wfList != null && wfList.size() > 0)
        {
            Iterator itr = wfList.iterator();
            while(itr.hasNext())
            {
                Map m = (Map)itr.next();
                String wfId = (String)m.get(DomainObject.SELECT_ID);
                if(wfId != null)
                {
                    acceptTasks(context, wfId);
                }
            }
        }
    }

    public void reassign(Context context, String[] args)throws Exception
    {
        try{
            String objectId = args[0];
            String person = args[1];
            setOwner(context,person);
            DomainObject dObj = new DomainObject(objectId);

            StringList objSelects = new StringList(3);
            objSelects.addElement(SELECT_NAME);
            objSelects.addElement("attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_Process") + "]");

            Map objMap = dObj.getInfo(context, objSelects);

            String processName = (String) objMap.get("attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_Process") + "]");
            String objName = (String) objMap.get(SELECT_NAME);

            String sMql = "modify workflow \"" + processName + "\" \"" + objName + "\" owner \"" + person + "\"";

            MQLCommand mql = new MQLCommand();
            HashMap returnMap = new HashMap();
            boolean bResult = mql.executeCommand(context, sMql);
            if(!bResult)
            {
                throw new Exception(mql.getError());
            }

        }catch(Exception ex)
        {
            throw ex;
        }

    }

}
