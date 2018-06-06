// ${CLASSNAME}.java
//
// Copyright (c) 2001-2015 Dassault Systemes.
//
// $Log: ${CLASSNAME}.java,v $
// Revision 10.6.3.0  9/26/2006
// for supporting resource allocation
// PMC MSP Integration

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;
import com.matrixone.apps.program.ProgramCentralConstants;
import matrix.db.AccessConstants;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.BusinessObject;
import matrix.util.MatrixException;
import matrix.util.StringList;
import com.matrixone.MCADIntegration.server.cache.IEFCache;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADXMLUtils;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;
import com.matrixone.apps.common.AssignedTasksRelationship;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.common.SubtaskRelationship;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.program.ProjectSpace;
import com.matrixone.apps.program.Task;
import com.matrixone.jdom.CDATA;
import com.matrixone.jdom.Document;
import com.matrixone.jdom.Element;
import com.matrixone.jdom.Namespace;
import com.matrixone.jdom.input.SAXBuilder;
import com.matrixone.jdom.output.XMLOutputter;
import com.matrixone.servlet.Framework;
import com.matrixone.apps.domain.util.FrameworkProperties;

/**
 * The <code>emxMSProjectIntegration</code> class represents the JPO for
 * the MS Project integration synchronization mechanism
 *
 * @version AEF 10.6.1.0 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxMSProjectIntegration_mxJPO extends ProjectSpace
{

  public static final String RELATIONSHIP_SUBTASK =
	         PropertyUtil.getSchemaProperty("relationship_Subtask");

  //cache map of person "lastname firstname" and personId
  private static Hashtable _personInfo = new Hashtable();
  private  StringList ProjectspaceSubtypes =null;
  private  StringList ProjectIds =new StringList();
  private  StringList AllOldProjectIds =new StringList();

  /** state "Create" for the "Project Task" policy. */
  public static final String STATE_PROJECT_TASK_CREATE =
          PropertyUtil.getSchemaProperty("policy",
                                         POLICY_PROJECT_TASK,
                                         "state_Create");

  /** state "Assign" for the "Project Task" policy. */
  public static final String STATE_PROJECT_TASK_ASSIGN =
          PropertyUtil.getSchemaProperty("policy",
                                         POLICY_PROJECT_TASK,
                                         "state_Assign");

  /** state "Active" for the "Project Task" policy. */
  public static final String STATE_PROJECT_TASK_ACTIVE =
          PropertyUtil.getSchemaProperty("policy",
                                         POLICY_PROJECT_TASK,
                                         "state_Active");

  /** state "Review" for the "Project Task" policy. */
  public static final String STATE_PROJECT_TASK_REVIEW =
          PropertyUtil.getSchemaProperty("policy",
                                         POLICY_PROJECT_TASK,
                                         "state_Review");

  /** state "Complete" for the "Project Task" policy. */
  public static final String STATE_PROJECT_TASK_COMPLETE =
          PropertyUtil.getSchemaProperty("policy",
                                         POLICY_PROJECT_TASK,
                                         "state_Complete");

  /**
   * Constant to select ATTRIBUTE_PROJECT_SCHEDULE_FROM
   */
  //2011x
  public static final String SELECT_ATTRIBUTE_PROJECT_SCHEDULE_FROM = "attribute[" + ATTRIBUTE_PROJECT_SCHEDULE_FROM + "]";
  public static final String ATTRIBUTE_SCHEDULEBASEDON = (String)PropertyUtil.getSchemaProperty("attribute_ScheduleBasedOn");
  
  String projectNamespaceUri = "http://schemas.microsoft.com/project";

  String msprojectTaskDuration = null;
  File fLocalFile = null;
  String xmlForClient = "";
  String sProjectSchduleBasedOn = "";
  String sScheduleFrom = "";

  //bean variables
  ProjectSpace project = null;
  ProjectSpace subProject = null;
  com.matrixone.apps.common.DependencyRelationship dependency = null;
  Task task = null;
  Task parentTask = null;
  Person person = new Person();
  AssignedTasksRelationship assignee = null;

  /** THIS FLAG SHOULD BE USED TO TURN DEBUGGING ON IN THE JPO
   *  THIS WILL ENABLE THE SYSTEM.OUT MESSAGES TO FIRE WHICH ALLOW FOR BETTER DEBUGGING
   *  IN THIS JPO.  THIS SHOULD NOT BE SET TO "TRUE" IN PRODUCTION SYSTEMS AS
   *  IT CAN BE A PERFORMANCE BOTTLENECK
   *
   *  true - enable debugging
   *  false - disable debugging (default)
   */

  boolean debug = false;

  Namespace ns = Namespace.getNamespace("http://schemas.microsoft.com/project");
  Namespace validateNS = Namespace.getNamespace("");

  SubtaskRelationship subtask = null;

  //counters and indexmaps
  StringList projectList = new StringList();
  Map taskIndexMap = new HashMap();
  Map taskParentMap = new HashMap();
  Map tasktypeDefaultPolicyMap = new HashMap(); //[: for caching the default task policy]
  String codeRegn = "";

  Date date = null;
  //start date format
  java.text.SimpleDateFormat sdf =
	             new java.text.SimpleDateFormat("yyyy-MM-dd'TO'H:mm:ss");
  //finish date format
  java.text.SimpleDateFormat fdf =
	             new java.text.SimpleDateFormat("yyyy-MM-dd'TO'H:mm:ss");
  //use MatrixDateFormat's pattern
  java.text.SimpleDateFormat MATRIX_DATE_FORMAT =
	         new java.text.SimpleDateFormat(
                     eMatrixDateFormat.getEMatrixDateFormat(),
                     Locale.US);

  // JPO version
  public static String strMSPJPOVersion = "V6R2015x";

  public long mergeStartTime = 0;
  public long mergeEndTime = 0;

  public long StartValidateResourceTime = 0;
  public long EndValidateResourceTime = 0;

  public long xmlfileGen_BeginTime = 0;
  public long xmlfileGen_EndTime = 0;

  public long xmlDataForMerge_BeginTime = 0;
  public long xmlDataForMerge_EndTime = 0;

  public long xmlDataForValidateResource_BeginTime = 0;
  public long xmlDataForValidateResource_EndTime = 0;

  public long getTaskStructure_startTime = 0;
  public long getTaskStructure_endTime = 0;

  public long addAssignees_startTime = 0;
  public long addAssignees_endTime = 0;

  public long afterDeleteStartTime = 0;
  public long afterDeleteEndTime = 0;

  // Iteration for each new added task
  public long iterationStartTime = 0;
  public long iterationEndTime = 0;

  public long ifTaskCreate = 0;
  public long ifTaskCreated = 0;

  public float timeToCreate = 0;

  public String thisTaskId = null;
  Map tasks = null;

  // Enables or Disables the massUpdates
  public boolean updateDatesSingle = true;

  //Reading from MSOI GCO
  com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject _msoiGlobalConf = null;
  com.matrixone.MCADIntegration.utils.MCADLocalConfigObject _msoiLocalConf = null;
  String msoi_in_rip_mode = null;
  String msoi_local_project_filepath = null;
  Context m_ctxUserContext = null;
  String userLanguage = "en";

  private IEFCache  _GlobalCache = new IEFGlobalCache();;

  public emxMSProjectIntegration_mxJPO ()
  {
	  //Do nothing
  }

  /**
   * Constructs a new emxMSProjectIntegration JPO object.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args an array of String arguments for this method
   * @throws Exception if the operation fails
   * @since AEF 10.0.0.0
  */
  public emxMSProjectIntegration_mxJPO (Context context, String[] args)
      throws Exception
  {
	  // Call the super constructor
	  super();
	  m_ctxUserContext = context;

	  if (debug)
	  {
		  System.out.println("============================================================================");
		  System.out.println("Loaded emxMSProjectIntegration JPO: "+strMSPJPOVersion +" @ "+Calendar.getInstance().getTime());
	  }
	  if (args != null && args.length > 0)
	  {
		  setId(args[0]);
		  Hashtable initArgsTable = (Hashtable)JPO.unpackArgs(args);
  		  
		  if(initArgsTable != null)
		  {
			  HashMap _GcoTable			  = (HashMap)initArgsTable.get("gcoTable");
			  _msoiGlobalConf = (com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject)_GcoTable.get("MSProject");
			  _msoiLocalConf = (com.matrixone.MCADIntegration.utils.MCADLocalConfigObject)initArgsTable.get("lco");

			  //msoi_in_rip_mode = _msoiGlobalConf.getMSPIntegrationRunningInRIPMode();
			  //msoi_local_project_filepath = _msoiGlobalConf.getMSPIntegrationLocalTransactionXMLPath();
			  msoi_in_rip_mode = _msoiGlobalConf.getCustomAttribute("MSPIntegrationRunningInRIPMode");
			  msoi_local_project_filepath = _msoiGlobalConf.getCustomAttribute("MSPIntegrationLocalTransactionXMLPath");
		  }
	  }
	  //instantiate beans
	  dependency = (com.matrixone.apps.common.DependencyRelationship) DomainRelationship.newInstance(m_ctxUserContext, DomainConstants.RELATIONSHIP_DEPENDENCY);
	  task = (Task) DomainObject.newInstance(m_ctxUserContext, DomainConstants.TYPE_TASK, DomainConstants.PROGRAM);
	  parentTask = (Task) DomainObject.newInstance(m_ctxUserContext, DomainConstants.TYPE_TASK, DomainConstants.PROGRAM);
	  project = (ProjectSpace) DomainObject.newInstance(m_ctxUserContext, DomainConstants.TYPE_PROJECT_SPACE, DomainConstants.PROGRAM);
	  subProject = (ProjectSpace) DomainObject.newInstance(m_ctxUserContext, DomainConstants.TYPE_PROJECT_SPACE, DomainConstants.PROGRAM);
	  if (debug)
		  System.out.println("Framework and Common Beans instantiated");
  }

  /**
  /**
   * Constructs a new emxMSProjectIntegration JPO object.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param id the business object id
   * @throws Exception if the operation fails
   * @since AEF 10.0.0.0
  */
  public emxMSProjectIntegration_mxJPO (String id)
      throws Exception
  {
	  super(id);
  }

  public String mxMain(Context context, String[] args) throws Exception
  {
	  return "";
  }

  /**
   * Checks out a Project Space for synchronization.
   * Generates XML string representing the Project Space and is downloaded
   * on the client desktop where MS Project resides
   *
   * @param busid String value of the BusID identifying the Project Space object
   * @throws Exception if the operation fails
  */
  public String executeFindForCheckout(Context context, String[] args) throws MatrixException
  {
	  if(debug)
		  System.out.println("==========================START   executeFindForCheckout============================================" );
	  codeRegn = "inside executeFindForCheckout";

	  try
	  {
		  xmlfileGen_BeginTime = System.currentTimeMillis();
		  // get the settings required to load the xml dropped by servlet

	      /*
  		  //Following Code is not required now since this JPO now reads the values from the MSOI GCO
		  IntegrationGlobalConfigObject gco = new IntegrationGlobalConfigObject();
		  getAttribute(context, gco, IntegrationGlobalConfigObject.IN_RIP_MODE);
		  getAttribute(context, gco, IntegrationGlobalConfigObject.LOCAL_PROJECT_FILEPATH);
		  */
		  
		  codeRegn = "GCO read.";	  
		  // load the xml
  		  if (debug)
			  System.out.println("Building XML file...");
 		  //Element elCommandRoot = loadXMLSentFromServlet(gco, args[0], context);
  		  Element elCommandRoot = loadXMLSentFromServlet(args[0], context);

		  codeRegn = "XML sent from servlet read.";
  		  if (debug)
			  System.out.println("Reading XML stream...done");

		  // create the response placeholder
 		  Element elResponseRoot = new Element("transaction");
		  // copy/modify attributes from request

		  java.util.List attribList = elCommandRoot.getAttributes();
 		  ListIterator litAtr = attribList.listIterator();

		  while (litAtr.hasNext())
		  {
			  //Element elAttrib = (Element) litAtr.next();
			  litAtr.next();

			  elResponseRoot.setAttribute("focusbrowser", elCommandRoot.getAttributeValue("focusbrowser"));

			  elResponseRoot.setAttribute("loglevel", elCommandRoot.getAttributeValue("loglevel"));

			  elResponseRoot.setAttribute("cname", elCommandRoot.getAttributeValue("cname"));

			  elResponseRoot.setAttribute("tid", elCommandRoot.getAttributeValue("tid"));

			  elResponseRoot.setAttribute("type", elCommandRoot.getAttributeValue("type"));

			  elResponseRoot.setAttribute("mpecver", elCommandRoot.getAttributeValue("mpecver"));

			  elResponseRoot.setAttribute("MSP", elCommandRoot.getAttributeValue("MSP"));
		  }

		  //elResponseRoot.setAttributes(elCommandRoot.getAttributes());
		  elResponseRoot.getAttribute("type").setValue("response");
		  codeRegn = "Preliminary response header prepared.";

		  // get bus id and project xml
		  String strBusId = "";
		  Element elCommandArguments = elCommandRoot.getChild("arguments");
		  List lArguments = elCommandArguments.getChildren("argument");
		  ListIterator litCtr = lArguments.listIterator();
		  Element elProjectRoot = null;
		  String strEditStatus = "";

		  while (litCtr.hasNext())
		  {
			  Element elArgument = (Element) litCtr.next();
			  if(elArgument.getAttributeValue("name").equals("busid"))
			  {
				  strBusId = elArgument.getText();
			  }
			  if(elArgument.getAttributeValue("name").equals("foredit"))
			  {
				  strEditStatus = elArgument.getText();
			  }
		  }

		  codeRegn = "Project XML and BusID got.";
 		  if (debug)
			  System.out.println(codeRegn);

		  // now create placeholder for response
 		  Element elResponseArgumentsNode = new Element("arguments");
		  elResponseRoot.addContent(elResponseArgumentsNode);

		  // create the busid argument placeholder
		  Element elBusIdArgument = new Element("argument");
		  elBusIdArgument.setAttribute("name", "busid");
		  elBusIdArgument.setText(strBusId);
		  elResponseArgumentsNode.addContent(elBusIdArgument);

		  // create the edit status argument placeholder
		  Element elEditStatusArgument = new Element("argument");
		  elEditStatusArgument.setAttribute("name", "foredit");
		  elEditStatusArgument.setText(strEditStatus);
		  elResponseArgumentsNode.addContent(elEditStatusArgument);

		  String projectId = strBusId;							
 		  project.setId(projectId);
 		  sProjectSchduleBasedOn = project.getAttributeValue(context, ATTRIBUTE_SCHEDULEBASEDON);
 		  
 		  //create the edit status argument placeholder
		  Element elProjectScheduleOnArgument = new Element("argument");
		  elProjectScheduleOnArgument.setAttribute("name", "ProjectScheduleOn");
		  elProjectScheduleOnArgument.setText(sProjectSchduleBasedOn);
		  elResponseArgumentsNode.addContent(elProjectScheduleOnArgument);
		  
		  // create the project xml argument placeholder
		  Element elProjectArgument = new Element("argument");
		  elProjectArgument.setAttribute("name", "projectxml");
		  elResponseArgumentsNode.addContent(elProjectArgument);

		  // create empty node for project xml
		  elProjectRoot = new Element("Project", null, projectNamespaceUri);
		  elProjectArgument.addContent(elProjectRoot);

		  codeRegn = "Read Project data.";
		  if (debug)
			  System.out.println(codeRegn);

		  // Define selectables for each Task object.
		  StringList busSelects = new StringList(11);
		  busSelects.add(SELECT_ID);
		  busSelects.add(SELECT_TYPE);
		  busSelects.add(SELECT_NAME);
		  busSelects.add(SELECT_CURRENT);
 		  busSelects.add(SELECT_ORIGINATED);
		  busSelects.add(SELECT_OWNER);
		  busSelects.add(SELECT_DESCRIPTION);
	  	  busSelects.add(SELECT_COMPANY_NAME);
		  busSelects.add(task.SELECT_PERCENT_COMPLETE);
		  busSelects.add(task.SELECT_TASK_ESTIMATED_DURATION);
		  busSelects.add(task.SELECT_TASK_ESTIMATED_START_DATE);
		  busSelects.add(task.SELECT_TASK_ESTIMATED_FINISH_DATE);
		  busSelects.add(task.SELECT_TASK_ACTUAL_DURATION);
		  busSelects.add(task.SELECT_TASK_ACTUAL_START_DATE);
		  busSelects.add(task.SELECT_TASK_ACTUAL_FINISH_DATE);
		  busSelects.add(SELECT_POLICY);
		  busSelects.add(SELECT_BASELINE_CURRENT_END_DATE);
		  busSelects.add(task.SELECT_TASK_CONSTRAINT_DATE);

		  //[2011x Start: Adding Project Schedule, Constraint Type in ProjectStringList]
		  busSelects.add("attribute"+"["+ATTRIBUTE_PROJECT_SCHEDULE_FROM+"]");		  
		  busSelects.add("attribute"+"["+ATTRIBUTE_DEFAULT_CONSTRAINT_TYPE+"]");		
		  //[2011x End: Adding Project Schedule, Constraint Type in ProjectStringList]
		  
		  StringList taskRelSelects = new StringList(11);
		  taskRelSelects.add(subtask.SELECT_TASK_WBS);

		  StringList taskSelects = new StringList(11);
		  taskSelects.add(task.SELECT_ID);
		  taskSelects.add(task.SELECT_NAME);
		  taskSelects.add(task.SELECT_OWNER);
		  taskSelects.add(SELECT_ORIGINATED);
		  taskSelects.add(task.SELECT_TYPE);
		  taskSelects.add(task.SELECT_DESCRIPTION);
		  taskSelects.add(task.SELECT_CURRENT);
		  taskSelects.add(task.SELECT_PERCENT_COMPLETE);
		  taskSelects.add(task.SELECT_TASK_ESTIMATED_DURATION);
		  taskSelects.add(task.SELECT_TASK_ESTIMATED_START_DATE);
		  taskSelects.add(task.SELECT_TASK_ESTIMATED_FINISH_DATE);
		  taskSelects.add(task.SELECT_TASK_ACTUAL_DURATION);
		  taskSelects.add(task.SELECT_TASK_ACTUAL_START_DATE);
		  taskSelects.add(task.SELECT_TASK_ACTUAL_FINISH_DATE);
		  
		  //[2011x Start: Adding task Constraint Type and Date in TaskStringList]
		  taskSelects.add(task.SELECT_TASK_CONSTRAINT_TYPE);
		  taskSelects.add(task.SELECT_DEFAULT_CONSTRAINT_TYPE);
		  taskSelects.add(task.SELECT_TASK_CONSTRAINT_DATE);
		  //[2011x End: Adding task Constraint Type and Date in TaskStringList]
		  
		  taskSelects.add(task.SELECT_PREDECESSOR_IDS);
		  taskSelects.add(task.SELECT_PREDECESSOR_TYPES);
		  taskSelects.add(task.SELECT_PREDECESSOR_LAG_TIMES);
		  taskSelects.add(task.SELECT_TASK_REQUIREMENT);
		  taskSelects.add(task.SELECT_HAS_SUBTASK);
		  taskSelects.add(task.SELECT_POLICY);
		  taskSelects.add(task.SELECT_BASELINE_CURRENT_END_DATE);

	 	  //String projectId = strBusId;							
 		  //project.setId(projectId);
		  task.setId(projectId);

		  //get the project info
		  Map projectMap = project.getInfo(context, busSelects);

		  codeRegn = "Project data Read";
		  if (debug)
			  System.out.println(codeRegn);

		  //get the task info
		  /// POSSIBLE BUG ???? RETURNS an in-correct duration for a task with rel - SF
		  MapList taskList = task.getTasks(context, task, 1, taskSelects, taskRelSelects);

		  codeRegn = "Fill project xml";
		  if (debug)
			  System.out.println(codeRegn);

		  Element elCurrentNode = null;
		  Element elTasksNode = null;
		  Element elParentNode = elProjectRoot;
		  String content = "";

		  createTaskIndexMap(context, projectId);
		  String thisProjectIndex = Integer.toString(projectList.indexOf(projectId));
		  addToParent(elParentNode, "UID",thisProjectIndex);
		  addToParent(elParentNode, "ID", thisProjectIndex);

		  content = (String) projectMap.get(SELECT_NAME);
		  addToParent(elParentNode, "Name", content);
		  
		  content = (String) projectMap.get(SELECT_NAME);
		  addToParent(elParentNode, "Title", content);

		  //[2011x Start: Creating xml node for Project Schedule in response xml]
		  sScheduleFrom = (String) projectMap.get("attribute"+"["+ATTRIBUTE_PROJECT_SCHEDULE_FROM+"]");
		  if(sScheduleFrom != null)
		  {
			  if(sScheduleFrom.equals("Project Start Date"))
				  content="True";
			  else
				  content="False";			  
		  }
		  addToParent(elParentNode, "ScheduleFrom", content);
		  
		  String sConstraintType = (String) projectMap.get("attribute"+"["+ATTRIBUTE_DEFAULT_CONSTRAINT_TYPE+"]");
		  content = GetConstraintEnumValues(sConstraintType);
		  addToParent(elParentNode, "ConstraintType", content);
		  addToParent(elParentNode,"ScheduleBasedOn",sProjectSchduleBasedOn);
		  //[2011x End: Creating xml node for Project Schedule in response xml]
		  
		  content = (String) projectMap.get(SELECT_OWNER);
		  content = getPersonLastNameFirstName(context, content);
		  addToParent(elParentNode, "Author", content);

		  // Define DefaultTaskType - PMC supports ONLY fixed duration
		  // 0 = Fixed Unit, 1 = Fixed Duration and 2 = Fixed Work
	 	  // We will hard-code it to 1

		  //addToParent(elParentNode, "DefaultTaskType", "1");

		  // Define NewTasksEffortDriven - Tasks are not effort driven in PMC
		  // 0 = Disabled and 1 = Enabled
		  //addToParent(elParentNode, "NewTasksEffortDriven", "0");

		  content = (String) projectMap.get(SELECT_ORIGINATED);
		  addToParent(elParentNode, "CreationDate", content);

		  content = (String) projectMap.get(SELECT_DESCRIPTION);
		  addToParent(elParentNode, "Description", content);

		  content = (String) projectMap.get(SELECT_COMPANY_NAME);
		  addToParent(elParentNode, "Company", content);

		  content = (String) projectMap.get(SELECT_PERCENT_COMPLETE);
		  //Integer newPercent = new Integer(Double.valueOf(content).intValue());//if value is 46.7, prev it was returning 46 now it returns 47
		  Integer newPercent = new Integer(Math.round(Float.valueOf(content)));
		  content = Integer.toString(newPercent.intValue());
		  addToParent(elParentNode, "PercentComplete", content);

		  addToParent(elParentNode, "DurationFormat", "7");

		  // Date format -> 2003-03-11T08:00:00
		  if(sScheduleFrom.equals("Project Start Date"))
		  	  content = (String) projectMap.get(SELECT_TASK_CONSTRAINT_DATE);
		  else
			  content = (String) projectMap.get(SELECT_TASK_ESTIMATED_START_DATE);
		  
		  Date date = MATRIX_DATE_FORMAT.parse(content,new java.text.ParsePosition(0));
		  content = sdf.format(date);
		  addToParent(elParentNode, "StartDate", content);

		  if(sScheduleFrom.equals("Project Finish Date"))
			  content = (String) projectMap.get(SELECT_TASK_CONSTRAINT_DATE);
		  else
			  content = (String) projectMap.get(SELECT_TASK_ESTIMATED_FINISH_DATE);
		  
		  date = MATRIX_DATE_FORMAT.parse(content,new java.text.ParsePosition(0));
		  content = fdf.format(date);
		  addToParent(elParentNode, "FinishDate", content);

		  elCurrentNode = new Element("ExtendedAttributes", null, projectNamespaceUri);
		  elParentNode.addContent(elCurrentNode);
		  elParentNode= elCurrentNode;
		  Element elExtendedAttributes = elCurrentNode;

		  elCurrentNode= new Element("ExtendedAttribute", null, projectNamespaceUri);
		  elParentNode.addContent(elCurrentNode);

		  elParentNode = elCurrentNode;

		  addToParent(elParentNode, "FieldID", "188743731");
		  addToParent(elParentNode, "FieldName", "Text1");
		  addToParent(elParentNode, "Alias", "TaskOID");

 		  elParentNode= elExtendedAttributes;
		  elCurrentNode= new Element("ExtendedAttribute", null, projectNamespaceUri);
		  elParentNode.addContent(elCurrentNode);

		  elParentNode = elCurrentNode;
		  addToParent(elParentNode, "FieldID", "205520904");
		  addToParent(elParentNode, "FieldName", "Text1");
		  addToParent(elParentNode, "Alias", "ResourceOID");

		  elParentNode= elProjectRoot;

		  codeRegn = "Add tasks as XML nodes";
		  elCurrentNode = new Element("Tasks", null, projectNamespaceUri);
		  elParentNode.addContent(elCurrentNode);
		  elTasksNode = elCurrentNode;
		  elParentNode = elTasksNode;

		  elCurrentNode = new Element("Task", null, projectNamespaceUri);
		  elParentNode.addContent(elCurrentNode);

		  elParentNode = elCurrentNode;
		  addToParent(elParentNode, "UID", "0");
		  addToParent(elParentNode, "ID", "0");
		  content = (String) projectMap.get(SELECT_NAME);

		  addToParent(elParentNode, "Name", content);
		  if (debug)
			  System.out.println ("Task Name: "+content +" ");

		  addToParent(elParentNode, "Summary", "1");
		  addToParent(elParentNode, "OutlineNumber", "0");
		  addToParent(elParentNode, "OutlineLevel", "0");
		  addToParent(elParentNode, "Rollup", "0");

		  Element elExtendedAttributeNode = new Element("ExtendedAttribute", null, projectNamespaceUri);
 		  elParentNode.addContent(elExtendedAttributeNode);
		 
		  addToParent(elExtendedAttributeNode, "FieldID", "188743731");
		  content = (String) projectMap.get(task.SELECT_ID);
		  addToParent(elExtendedAttributeNode, "Value", content);

		  //PMC supports types which are "Fixed Duration" and are "Not Effort Driven"
		  // We will support only these type of tasks

		  //<EffortDriven>0</EffortDriven>
		  // 0 = Disabled
		  // 1 = Enabled
		  //String effortDrivenFlag = "0";
		  //addToParent(elParentNode, "EffortDriven", effortDrivenFlag);

		  content = (String) projectMap.get(SELECT_CURRENT);
		  addToParent(elParentNode, "State", content);
		  
		  content = (String) projectMap.get(SELECT_DESCRIPTION);
		  addToParent(elParentNode, "Description", content);

		  content = (String) projectMap.get(SELECT_OWNER);
		  content = getPersonLastNameFirstName(context, content);
		  addToParent(elParentNode, "Owner", content);

		  content = (String) projectMap.get(SELECT_PERCENT_COMPLETE);
		  newPercent = new Integer(Double.valueOf(content).intValue());
		  content = Integer.toString(newPercent.intValue());
		  addToParent(elParentNode, "PercentComplete", content);

		  //[2011x Start: Creating xml node for Project Summary Task Constraint Type and date in response xml]
		  sConstraintType = (String) projectMap.get("attribute"+"["+ATTRIBUTE_DEFAULT_CONSTRAINT_TYPE+"]");
		  content = GetConstraintEnumValues(sConstraintType);		  	

		  addToParent(elParentNode, "ConstraintType", content);
		  
		  content= (String)projectMap.get("attribute"+"["+ATTRIBUTE_CONSTRAINT_DATE+"]");
		  if (content!= null && !content.equals(""))				//This check is necessary otherwise it will give exception
		  {
			  Date constraintDate = MATRIX_DATE_FORMAT.parse(content,new java.text.ParsePosition(0));
			  content= sdf.format(constraintDate);
		  }	
//		  else
//			  content="NA";
		  addToParent(elParentNode, "ConstraintDate", content);
		  //[2011x End: Creating xml node for Project Summary Task Constraint Type and date in response xml]
		  
		  // Date format -> 2003-03-11T08:00:00
		  content = (String) projectMap.get(SELECT_TASK_ESTIMATED_START_DATE);
		  date = MATRIX_DATE_FORMAT.parse(content,new java.text.ParsePosition(0));
	  	  content = sdf.format(date);
		  addToParent(elParentNode, "Start", content);

		  content = (String) projectMap.get(SELECT_TASK_ESTIMATED_FINISH_DATE);
		  date = MATRIX_DATE_FORMAT.parse(content,new java.text.ParsePosition(0));
		  content = fdf.format(date);
		  addToParent(elParentNode, "Finish", content);

		  String taskEstDuration = null;
		  taskEstDuration = (String) projectMap.get(SELECT_TASK_ESTIMATED_DURATION);

		  if (!taskEstDuration.equals(""))
		  {
			  Integer newDuration = new Integer(Double.valueOf(taskEstDuration).intValue());
			  Double new2Duration = new Double(Double.valueOf(taskEstDuration).doubleValue());
			  content = "PT" + Integer.toString(newDuration.intValue() * 8) + "H" 
					+ Long.toString(Math.round(new2Duration.doubleValue() * 8 * 60 - newDuration.intValue() * 8 * 60 )) + "M0S"; 
		  }
		  addToParent(elParentNode, "Duration", content);

		  if(sProjectSchduleBasedOn.equals("Actual"))
	 	  {	
			  //IR-185678V6R2013x Begin
			  content = (String) projectMap.get(SELECT_TASK_ACTUAL_START_DATE);
			  if (content!= null && !content.equals(""))
			  {
				  date = MATRIX_DATE_FORMAT.parse(content,new java.text.ParsePosition(0));
				  content = sdf.format(date);
			  }
			  addToParent(elParentNode, "ActualStart", content);

			  content = (String) projectMap.get(SELECT_TASK_ACTUAL_FINISH_DATE);
			  if (content!= null && !content.equals(""))
			  {
				  date = MATRIX_DATE_FORMAT.parse(content,new java.text.ParsePosition(0));
				  content = fdf.format(date);
			  }
			  addToParent(elParentNode, "ActualFinish", content);
			  //IR-185678V6R2013x end 
			  
			  String taskActualDuration = null;
			  taskActualDuration = (String) projectMap.get(SELECT_TASK_ACTUAL_DURATION);
	
			  if (!taskActualDuration.equals(""))
			  {
				  Integer newDuration = new Integer(Double.valueOf(taskActualDuration).intValue());
				  Double new2Duration = new Double(Double.valueOf(taskActualDuration).doubleValue());
				  content = "PT" + Integer.toString(newDuration.intValue() * 8) + "H" 
						 + Long.toString(Math.round(new2Duration.doubleValue() * 8 * 60 - newDuration.intValue() * 8 * 60 )) + "M0S"; 
		 	  }
			  addToParent(elParentNode, "ActualDuration", content);
	 	  }

		  content = (String) projectMap.get(SELECT_TYPE);
		  addToParent(elParentNode, "NodeType", content);

		  //Begin to create policy xmlnode for Project Summary Node IR 057630V6R2011x
  		  content = (String) projectMap.get(SELECT_POLICY);
		  addToParent(elParentNode, "Policy", content);
		  //End to create policy xmlnode for Project Summary Node 

		  elParentNode= elProjectRoot;
		  Element elResourcesNode = new Element("Resources", null, projectNamespaceUri);
		  elParentNode.addContent(elResourcesNode);

		  //summary node for resource
		  elCurrentNode = new Element("Resource", null, projectNamespaceUri);
		  elParentNode = elResourcesNode;
		  elParentNode.addContent(elCurrentNode);
		  elParentNode = elCurrentNode;
		  addToParent(elParentNode, "UID", "0");
		  addToParent(elParentNode, "ID", "0");
		  addToParent(elParentNode, "Type", "1");
		  addToParent(elParentNode, "IsNull", "0");
		  addToParent(elParentNode, "WorkGroup", "0");
		  addToParent(elParentNode, "OverAllocated", "0");
		  addToParent(elParentNode, "CanLevel", "1");
		  addToParent(elParentNode, "AccrueAt", "3");

		  //Add project Members as resources
		  //this will show up in resource sheet of MS Project
		  //and users will be able to assign them as task assignees in MS Project
		  busSelects.clear();
		  busSelects.add(person.SELECT_ID);
		  busSelects.add(person.SELECT_NAME);
		  busSelects.add(person.SELECT_FIRST_NAME);
		  busSelects.add(person.SELECT_MIDDLE_NAME);
		  busSelects.add(person.SELECT_LAST_NAME);
		  busSelects.add(person.SELECT_EMAIL_ADDRESS);
		  busSelects.add(person.SELECT_COMPANY_NAME);
		  MapList membersList = project.getMembers(context, busSelects, null, null, null);
		  ListIterator membersItr = membersList.listIterator();

		  Map resourceIndexMap = new HashMap();
		  int resourceCounter =1;
		  while (membersItr.hasNext())
		  {
			  Map membersMap = (Map) membersItr.next();
			  String personName = (String)membersMap.get(person.SELECT_LAST_NAME) + " " + (String)membersMap.get(person.SELECT_FIRST_NAME);
			  //add to the resourceMap if the person is not already added to the resource list
			  if(resourceIndexMap.get(personName) == null)
			  {
				  //int resourceCounter = resourceIndexMap.size();
				  resourceIndexMap.put(personName,  String.valueOf(resourceCounter));
				  //create the resource details
				  elCurrentNode = new Element("Resource", null, projectNamespaceUri);
				  elParentNode = elResourcesNode;
				  elParentNode.addContent(elCurrentNode);
				  elParentNode = elCurrentNode;

				  content = String.valueOf(resourceCounter);
				  addToParent(elParentNode, "UID", content);
				  addToParent(elParentNode, "ID", content);

				  content = personName;
				  addToParent(elParentNode, "Name", content);
				  addToParent(elParentNode, "Type", "1");
				  addToParent(elParentNode, "IsNull", "0");
				  addToParent(elParentNode, "WorkGroup", "0");

			  	  content = (String)membersMap.get(person.SELECT_EMAIL_ADDRESS);
				  addToParent(elParentNode, "EmailAddress", content);
				  addToParent(elParentNode, "OverAllocated", "0");
				  addToParent(elParentNode, "CanLevel", "1");
				  addToParent(elParentNode, "AccrueAt", "3");

				  elCurrentNode = new Element("ExtendedAttribute", null, projectNamespaceUri);
				  elParentNode.addContent(elCurrentNode);

				  elParentNode = elCurrentNode;

				  content = String.valueOf(resourceCounter);
				  addToParent(elParentNode, "UID", content);

				  addToParent(elParentNode, "FieldID", "205520904");

				  content = (String) membersMap.get(person.SELECT_ID);
				  addToParent(elParentNode, "Value", content);
				  resourceCounter++;

				  //find the person id of the person with the given Last name First Name
				  //if it is not found then add to the _personInfo for caching
				  if(_personInfo != null && _personInfo.get(personName) == null)
				  {
					  //found new Last Name First Name, add to cache
					  //this will be reused when the user adds the user as task assignee
					  _personInfo.put(personName, membersMap.get(person.SELECT_NAME));
				  }
			  }
		  }

		  elParentNode= elProjectRoot;
		  Element elAssignmentsNode = new Element("Assignments", null, projectNamespaceUri);
		  elParentNode.addContent(elAssignmentsNode);

		  codeRegn = "Assignment XML done.";
		  //Loops through all the task of the project to get the task info
		  ListIterator itr = taskList.listIterator();
		  while (itr.hasNext())
		  {
			  Map map = new HashMap();
			  map = (Map) itr.next();

			  getTaskStructure(context, map, elTasksNode, elResourcesNode, elAssignmentsNode, resourceIndexMap, 1);
	 	  }

		  elResponseRoot.setAttribute("result", "success");
		  if (debug)
	  	  {
			  System.out.println("Sending processed XML back to MS Project...Done");
			  xmlfileGen_EndTime = System.currentTimeMillis();

			  long total_xmlfileGenTime = xmlfileGen_EndTime - xmlfileGen_BeginTime;
		  }
		  //return "true|" + dumpTransactionXMLForServlet(context, gco, args[0], elResponseRoot);
		  xmlfileGen_EndTime = System.currentTimeMillis();
		  long total_xmlfileGenTime = xmlfileGen_EndTime - xmlfileGen_BeginTime;
		  if(debug)
			  System.out.println("==========================END   executeFindForCheckout============================================");
		  return "true|" + dumpTransactionXMLForServlet(context, args[0], elResponseRoot);	  
	  }
	  catch(Exception e)
	  {
		  //e.printStackTrace();
		  System.out.println("{Exception} : " + e.getMessage());
		  throw new MatrixException(e.getMessage());
	  }
  }

  void createTaskIndexMap(Context context, String thisProjectId)throws Exception
  {
	  projectList.add(thisProjectId);
	  StringList sl = new StringList();
	  sl.add(thisProjectId);
	  ListIterator itr = sl.listIterator();

	  StringList taskSelects = new StringList(4);
	  taskSelects.add(task.SELECT_ID);
	  taskSelects.add(subtask.SELECT_TASK_WBS);
	  taskSelects.add(task.SELECT_TYPE);
	  taskSelects.add(task.SELECT_HAS_SUBTASK);

	  while(itr.hasNext())
	  {
		  String nextProjectId = (String)itr.next();
		  task.setId(nextProjectId);
		  MapList taskListForDependency = task.getTasks(context, task, 1, taskSelects, null);

		  ListIterator taskItr = taskListForDependency.listIterator();
		  int counter = 1;
		  while (taskItr.hasNext())
		  {
			  Map map = (Map) taskItr.next();
			  String id = (String) map.get(task.SELECT_ID);
			  String type = (String)map.get(task.SELECT_TYPE);
			  String  isSummaryTask = (String)map.get(task.SELECT_HAS_SUBTASK);

			  taskParentMap.put(id, nextProjectId);
			  taskIndexMap.put(id, String.valueOf(counter++));
			  if(isProjectSpace(context,type))
			  {
				  createTaskIndexMap(context,id);
			  }
			  else
			  {
				  if(isSummaryTask.equalsIgnoreCase("true"))
					  counter = getTaskStructureForTIMap(context,id,counter,nextProjectId);
			  }
		  }
	  }
  }

  int getTaskStructureForTIMap(Context context, String taskId, int nextTaskCount, String projectId)throws Exception
  {
	  StringList taskSelects = new StringList(4);
	  taskSelects.add(task.SELECT_ID);
	  taskSelects.add(subtask.SELECT_TASK_WBS);
	  taskSelects.add(task.SELECT_TYPE);
	  taskSelects.add(task.SELECT_HAS_SUBTASK);

	  task.setId(taskId);
	  MapList taskListForDependency = task.getTasks(context, task, 1, taskSelects, null);

	  ListIterator taskItr = taskListForDependency.listIterator();
	  while (taskItr.hasNext())
	  {
		  Map map = (Map) taskItr.next();
		  String id = (String) map.get(task.SELECT_ID);
		  String type = (String)map.get(task.SELECT_TYPE);
		  String  isSummaryTask = (String)map.get(task.SELECT_HAS_SUBTASK);

		  taskParentMap.put(id, projectId);
		  taskIndexMap.put(id, String.valueOf(nextTaskCount++));
		  if(isProjectSpace(context,type))
		  {
			  createTaskIndexMap(context,id);
		  }
		  else
		  {
			  if(isSummaryTask.equalsIgnoreCase("true"))
				  nextTaskCount =getTaskStructureForTIMap(context,id,nextTaskCount,projectId);
		  }
	  }
	  return nextTaskCount;
  }
	
 public Element executeCheckoutSubproject(Context context, String[] args) throws MatrixException
  {
	  if(debug)
		  System.out.println("==========================START   executeCheckoutSubproject============================================");
	  codeRegn = "inside executeCheckoutSubproject";

	  try
	  {
		  Element elCommandRoot = loadXMLSentFromServlet(args[0], context);

	  	  codeRegn = "XML sent from servlet read.";

		  Element elResponseRoot = new Element("transaction");
		  java.util.List attribList = elCommandRoot.getAttributes();
		  ListIterator litAtr = attribList.listIterator();

		  while (litAtr.hasNext())
		  {
			  litAtr.next();

			  elResponseRoot.setAttribute("focusbrowser", elCommandRoot.getAttributeValue("focusbrowser"));
			  elResponseRoot.setAttribute("loglevel", elCommandRoot.getAttributeValue("loglevel"));
			  elResponseRoot.setAttribute("cname", elCommandRoot.getAttributeValue("cname"));
			  elResponseRoot.setAttribute("tid", elCommandRoot.getAttributeValue("tid"));
			  elResponseRoot.setAttribute("type", elCommandRoot.getAttributeValue("type"));
			  elResponseRoot.setAttribute("mpecver", elCommandRoot.getAttributeValue("mpecver"));
			  elResponseRoot.setAttribute("MSP", elCommandRoot.getAttributeValue("MSP"));
		  }

		  //elResponseRoot.setAttributes(elCommandRoot.getAttributes());
		  elResponseRoot.getAttribute("type").setValue("response");
		  codeRegn = "Preliminary response header prepared.";

		  // get bus id and project xml
		  String strBusId = "";
		  Element elCommandArguments = elCommandRoot.getChild("arguments");
		  List lArguments = elCommandArguments.getChildren("argument");
		  ListIterator litCtr = lArguments.listIterator();
		  Element elProjectRoot = null;
		  String strEditStatus = "";

		  while (litCtr.hasNext())
	  	  {
			  Element elArgument = (Element) litCtr.next();
			  if(elArgument.getAttributeValue("name").equals("busid"))
			  {
				  strBusId = elArgument.getText();
			  }
			  if(elArgument.getAttributeValue("name").equals("foredit"))
			  {
				  strEditStatus = elArgument.getText();
			  }
		  }

		  codeRegn = "Project XML and BusID got.";
		  if (debug)
			  System.out.println(codeRegn);
		  // now create placeholder for response
		  Element elResponseArgumentsNode = new Element("arguments");
		  elResponseRoot.addContent(elResponseArgumentsNode);

		  // create the busid argument placeholder
		  Element elBusIdArgument = new Element("argument");
		  elBusIdArgument.setAttribute("name", "busid");
		  elBusIdArgument.setText(strBusId);
		  elResponseArgumentsNode.addContent(elBusIdArgument);

		  // create the edit status argument placeholder
		  Element elEditStatusArgument = new Element("argument");
		  elEditStatusArgument.setAttribute("name", "foredit");
		  elEditStatusArgument.setText(strEditStatus);
		  elResponseArgumentsNode.addContent(elEditStatusArgument);

		  // create the project xml argument placeholder
		  Element elProjectArgument = new Element("argument");
		  elProjectArgument.setAttribute("name", "projectxml");
		  elResponseArgumentsNode.addContent(elProjectArgument);

		  // create empty node for project xml
		  elProjectRoot = new Element("Project", null, projectNamespaceUri);
		  //elProjectArgument.addContent(elProjectRoot);

		  codeRegn = "Read Project data.";
		  if (debug)
			  System.out.println(codeRegn);

		  // Define selectables for each Task object.
		  StringList busSelects = new StringList(11);
		  busSelects.add(SELECT_ID);
		  busSelects.add(SELECT_TYPE);
		  busSelects.add(SELECT_NAME);
		  busSelects.add(SELECT_CURRENT);
		  busSelects.add(SELECT_ORIGINATED);
		  busSelects.add(SELECT_OWNER);
		  busSelects.add(SELECT_DESCRIPTION);
		  busSelects.add(SELECT_COMPANY_NAME);
		  busSelects.add(task.SELECT_PERCENT_COMPLETE);
		  busSelects.add(task.SELECT_TASK_ESTIMATED_DURATION);
		  busSelects.add(task.SELECT_TASK_ESTIMATED_START_DATE);
		  busSelects.add(task.SELECT_TASK_ESTIMATED_FINISH_DATE);
		  busSelects.add(task.SELECT_TASK_ACTUAL_DURATION);
		  busSelects.add(task.SELECT_TASK_ACTUAL_START_DATE);
		  busSelects.add(task.SELECT_TASK_ACTUAL_FINISH_DATE);
		  busSelects.add(SELECT_POLICY);
		  busSelects.add(SELECT_BASELINE_CURRENT_END_DATE);
		  busSelects.add("attribute"+"["+ATTRIBUTE_PROJECT_SCHEDULE_FROM+"]");
		  busSelects.add(task.SELECT_TASK_CONSTRAINT_DATE);

		  //[2011x Start:Adding Constraint Type in SubProjectStringList]
		  busSelects.add(SELECT_DEFAULT_CONSTRAINT_TYPE);
		  //[2011x End:Adding Constraint Type in SubProjectStringList]

		  StringList taskRelSelects = new StringList(11);
		  taskRelSelects.add(subtask.SELECT_TASK_WBS);

		  StringList taskSelects = new StringList(11);
		  taskSelects.add(task.SELECT_ID);
		  taskSelects.add(task.SELECT_NAME);
		  taskSelects.add(task.SELECT_OWNER);
		  taskSelects.add(SELECT_ORIGINATED);
		  taskSelects.add(task.SELECT_TYPE);
		  taskSelects.add(task.SELECT_DESCRIPTION);
		  taskSelects.add(task.SELECT_CURRENT);
		  taskSelects.add(task.SELECT_PERCENT_COMPLETE);
		  taskSelects.add(task.SELECT_TASK_ESTIMATED_DURATION);
		  taskSelects.add(task.SELECT_TASK_ESTIMATED_START_DATE);
		  taskSelects.add(task.SELECT_TASK_ESTIMATED_FINISH_DATE);
		  taskSelects.add(task.SELECT_TASK_ACTUAL_DURATION);
		  taskSelects.add(task.SELECT_TASK_ACTUAL_START_DATE);
		  taskSelects.add(task.SELECT_TASK_ACTUAL_FINISH_DATE);
		  taskSelects.add(task.SELECT_PREDECESSOR_IDS);
		  taskSelects.add(task.SELECT_PREDECESSOR_TYPES);
		  taskSelects.add(task.SELECT_PREDECESSOR_LAG_TIMES);
		  taskSelects.add(task.SELECT_TASK_REQUIREMENT);
		  taskSelects.add(task.SELECT_HAS_SUBTASK);
		  taskSelects.add(task.SELECT_POLICY);
		  taskSelects.add(task.SELECT_BASELINE_CURRENT_END_DATE);

		  //[2011x Start: Adding Constraint Type in taskStringList]
		  taskSelects.add(task.SELECT_TASK_CONSTRAINT_TYPE);
		  taskSelects.add(task.SELECT_DEFAULT_CONSTRAINT_TYPE);
		  taskSelects.add(task.SELECT_TASK_CONSTRAINT_DATE);
		  //[2011x End: Adding Constraint Type in taskStringList]		  
		  
		  String projectId = strBusId;							
		  project.setId(projectId);
		  task.setId(projectId);
		
		  //get the project info
		  Map projectMap = project.getInfo(context, busSelects);

		  codeRegn = "Project data Read";
		  if (debug)
		  	  System.out.println(codeRegn);

		  //get the task info
		  /// POSSIBLE BUG ???? RETURNS an in-correct duration for a task with rel - SF
		  MapList taskList = task.getTasks(context, task, 1, taskSelects, taskRelSelects);

		  codeRegn = "Fill project xml";
		  if (debug)
			  System.out.println(codeRegn);

		  Element elCurrentNode = null;
		  Element elTasksNode = null;
		  Element elParentNode = elProjectRoot;
		  String content = "";

		  createTaskIndexMap(context, projectId);
		  String thisProjectIndex = Integer.toString(projectList.indexOf(projectId));
		  addToParent(elParentNode, "UID",thisProjectIndex);
		  addToParent(elParentNode, "ID", thisProjectIndex);

		  content = (String) projectMap.get(SELECT_NAME);
		  addToParent(elParentNode, "Name", content);

		  content = (String) projectMap.get(SELECT_NAME);
		  addToParent(elParentNode, "Title", content);

		  //HF-099715V6R2011x_ Begin
		  content = (String) projectMap.get("attribute"+"["+ATTRIBUTE_PROJECT_SCHEDULE_FROM+"]");		 
		  if(content.equals("Project Start Date"))
			  content="True";
		  else
			  content="False";			  
		  addToParent(elParentNode, "ScheduleFrom", content);
		  //HF-099715V6R2011x_ end

		  content = (String) projectMap.get(SELECT_OWNER);
		  content = getPersonLastNameFirstName(context, content);
		  addToParent(elParentNode, "Author", content);

		  content = (String) projectMap.get(SELECT_ORIGINATED);
		  addToParent(elParentNode, "CreationDate", content);

		  content = (String) projectMap.get(SELECT_DESCRIPTION);
		  addToParent(elParentNode, "Description", content);

		  content = (String) projectMap.get(SELECT_COMPANY_NAME);
		  addToParent(elParentNode, "Company", content);

		  content = (String) projectMap.get(SELECT_PERCENT_COMPLETE);
		  //Integer newPercent = new Integer(Double.valueOf(content).intValue());//if value is 46.7, prev it was returning 46 now it returns 47
		  Integer newPercent = new Integer(Math.round(Float.valueOf(content)));
		  content = Integer.toString(newPercent.intValue());
		  addToParent(elParentNode, "PercentComplete", content);

		  //[2011x Start: Creating xml node for Sub Project Summary Task Constraint Type and date in response xml]
		  String sConstraintType = (String) projectMap.get(SELECT_DEFAULT_CONSTRAINT_TYPE);
		  content = GetConstraintEnumValues(sConstraintType);
		  addToParent(elParentNode, "ConstraintType", content);
		  
		  content= (String)projectMap.get(SELECT_CONSTRAINT_DATE);
		  if (content!= null && !content.equals(""))				//This check is necessary otherwise it will give exception
		  {
			  Date constraintDate = MATRIX_DATE_FORMAT.parse(content,new java.text.ParsePosition(0));
			  content= sdf.format(constraintDate);
		  }	
		  addToParent(elParentNode, "ConstraintDate", content);
		  //[2011x End: Creating xml node for Sub Project Summary Task Constraint Type and date in response xml]

		  
		  addToParent(elParentNode, "DurationFormat", "7");

		  if(sScheduleFrom.equals("Project Start Date"))
			  content = (String) projectMap.get(SELECT_TASK_CONSTRAINT_DATE);
		  else
		  content = (String) projectMap.get(SELECT_TASK_ESTIMATED_START_DATE);
		  date = new Date(content);
		  content = sdf.format(date);
		  addToParent(elParentNode, "StartDate", content);

		  if(sScheduleFrom.equals("Project Finish Date"))
			  content = (String) projectMap.get(SELECT_TASK_CONSTRAINT_DATE);
		  else
		  content = (String) projectMap.get(SELECT_TASK_ESTIMATED_FINISH_DATE);
		  date = new Date(content);
		  content = fdf.format(date);
		  addToParent(elParentNode, "FinishDate", content);

		  elCurrentNode = new Element("ExtendedAttributes", null, projectNamespaceUri);
		  elParentNode.addContent(elCurrentNode);
		  elParentNode= elCurrentNode;
		  Element elExtendedAttributes = elCurrentNode;

		  elCurrentNode= new Element("ExtendedAttribute", null, projectNamespaceUri);
		  elParentNode.addContent(elCurrentNode);

		  elParentNode = elCurrentNode;

		  addToParent(elParentNode, "FieldID", "188743731");
		  addToParent(elParentNode, "FieldName", "Text1");
		  addToParent(elParentNode, "Alias", "TaskOID");

		  elParentNode= elExtendedAttributes;
		  elCurrentNode= new Element("ExtendedAttribute", null, projectNamespaceUri);
		  elParentNode.addContent(elCurrentNode);

		  elParentNode = elCurrentNode;
		  addToParent(elParentNode, "FieldID", "205520904");
		  addToParent(elParentNode, "FieldName", "Text1");
		  addToParent(elParentNode, "Alias", "ResourceOID");

		  elParentNode= elProjectRoot;

		  codeRegn = "Add tasks as XML nodes";
		  elCurrentNode = new Element("Tasks", null, projectNamespaceUri);
		  elParentNode.addContent(elCurrentNode);
		  elTasksNode = elCurrentNode;
		  elParentNode = elTasksNode;

		  elCurrentNode = new Element("Task", null, projectNamespaceUri);
		  elParentNode.addContent(elCurrentNode);

		  elParentNode = elCurrentNode;
		  addToParent(elParentNode, "UID", "0");
		  addToParent(elParentNode, "ID", "0");
		  content = (String) projectMap.get(SELECT_NAME);

		  addToParent(elParentNode, "Name", content);
		  if (debug)
			  System.out.println ("Task Name: "+content +" ");

		  addToParent(elParentNode, "Summary", "1");
		  addToParent(elParentNode, "OutlineNumber", "0");
		  addToParent(elParentNode, "OutlineLevel", "0");
		  addToParent(elParentNode, "Rollup", "0");

		  content = (String) projectMap.get(SELECT_CURRENT);
		  addToParent(elParentNode, "State", content);

		  content = (String) projectMap.get(SELECT_DESCRIPTION);
		  addToParent(elParentNode, "Description", content);

		  content = (String) projectMap.get(SELECT_OWNER);
		  content = getPersonLastNameFirstName(context, content);
		  addToParent(elParentNode, "Owner", content);

		  content = (String) projectMap.get(SELECT_PERCENT_COMPLETE);
		  //newPercent = new Integer(Double.valueOf(content).intValue());////if value is 46.7, prev it was returning 46 now it returns 47
		  newPercent = new Integer(Math.round(Float.valueOf(content)));
		  content = Integer.toString(newPercent.intValue());
		  addToParent(elParentNode, "PercentComplete", content);

		  content = (String) projectMap.get(SELECT_TASK_ESTIMATED_START_DATE);
		  date = new Date(content);
		  content = sdf.format(date);
		  addToParent(elParentNode, "Start", content);

		  content = (String) projectMap.get(SELECT_TASK_ESTIMATED_FINISH_DATE);
		  date = new Date(content);
		  content = fdf.format(date);
		  addToParent(elParentNode, "Finish", content);

		  String taskEstDuration = null;
		  taskEstDuration = (String) projectMap.get(SELECT_TASK_ESTIMATED_DURATION);

		  if (!taskEstDuration.equals(""))
		  {
			  Integer newDuration = new Integer(Double.valueOf(taskEstDuration).intValue());
			  Double new2Duration = new Double(Double.valueOf(taskEstDuration).doubleValue());
			  content = "PT" + Integer.toString(newDuration.intValue() * 8) + "H" 
					 + Long.toString(Math.round(new2Duration.doubleValue() * 8 * 60 - newDuration.intValue() * 8 * 60 )) + "M0S"; 
		  }
		  addToParent(elParentNode, "Duration", content);

		  if(sProjectSchduleBasedOn.equals("Actual"))
		  {
			  String taskActualDuration = null;
			  taskActualDuration = (String) projectMap.get(SELECT_TASK_ACTUAL_DURATION);
	
			  if (!taskActualDuration.equals(""))
			  {
				  Integer newDuration = new Integer(Double.valueOf(taskActualDuration).intValue());
				  Double new2Duration = new Double(Double.valueOf(taskActualDuration).doubleValue());
				  content = "PT" + Integer.toString(newDuration.intValue() * 8) + "H" 
						 + Long.toString(Math.round(new2Duration.doubleValue() * 8 * 60 - newDuration.intValue() * 8 * 60 )) + "M0S"; 	
			  }
			  addToParent(elParentNode, "ActualDuration", content);
		  }

		  content = (String) projectMap.get(SELECT_TYPE);
		  addToParent(elParentNode, "NodeType", content);

		  elParentNode= elProjectRoot;
		  Element elResourcesNode = new Element("Resources", null, projectNamespaceUri);
		  elParentNode.addContent(elResourcesNode);

		  //summary node for resource
		  elCurrentNode = new Element("Resource", null, projectNamespaceUri);
		  elParentNode = elResourcesNode;
		  elParentNode.addContent(elCurrentNode);
		  elParentNode = elCurrentNode;
		  addToParent(elParentNode, "UID", "0");
		  addToParent(elParentNode, "ID", "0");
		  addToParent(elParentNode, "Type", "1");
		  addToParent(elParentNode, "IsNull", "0");
		  addToParent(elParentNode, "WorkGroup", "0");
		  addToParent(elParentNode, "OverAllocated", "0");
		  addToParent(elParentNode, "CanLevel", "1");
		  addToParent(elParentNode, "AccrueAt", "3");

		  //Add project Members as resources
		  //this will show up in resource sheet of MS Project
		  //and users will be able to assign them as task assignees in MS Project
		  busSelects.clear();
		  busSelects.add(person.SELECT_ID);
		  busSelects.add(person.SELECT_NAME);
		  busSelects.add(person.SELECT_FIRST_NAME);
		  busSelects.add(person.SELECT_MIDDLE_NAME);
		  busSelects.add(person.SELECT_LAST_NAME);
		  busSelects.add(person.SELECT_EMAIL_ADDRESS);
		  busSelects.add(person.SELECT_COMPANY_NAME);
		
		  MapList membersList = project.getMembers(context, busSelects, null, null, null);
		  ListIterator membersItr = membersList.listIterator();

		  Map resourceIndexMap = new HashMap();
		  int resourceCounter = 1;
		  while (membersItr.hasNext())
	  	  {
			  Map membersMap = (Map) membersItr.next();
			  String personName = (String)membersMap.get(person.SELECT_LAST_NAME) + " " + (String)membersMap.get(person.SELECT_FIRST_NAME);
			  //add to the resourceMap if the person is not already added to the resource list
			  if(resourceIndexMap.get(personName) == null)
			  {
				  resourceIndexMap.put(personName,  String.valueOf(resourceCounter));
				  //create the resource details
				  elCurrentNode = new Element("Resource", null, projectNamespaceUri);
				  elParentNode = elResourcesNode;
				  elParentNode.addContent(elCurrentNode);
				  elParentNode = elCurrentNode;

				  content = String.valueOf(resourceCounter);
				  addToParent(elParentNode, "UID", content);
				  addToParent(elParentNode, "ID", content);

				  content = personName;
				  addToParent(elParentNode, "Name", content);
				  addToParent(elParentNode, "Type", "1");
				  addToParent(elParentNode, "IsNull", "0");
				  addToParent(elParentNode, "WorkGroup", "0");

				  content = (String)membersMap.get(person.SELECT_EMAIL_ADDRESS);
				  addToParent(elParentNode, "EmailAddress", content);
				  addToParent(elParentNode, "OverAllocated", "0");
				  addToParent(elParentNode, "CanLevel", "1");
				  addToParent(elParentNode, "AccrueAt", "3");

				  elCurrentNode = new Element("ExtendedAttribute", null, projectNamespaceUri);
				  elParentNode.addContent(elCurrentNode);

				  elParentNode = elCurrentNode;

				  content = String.valueOf(resourceCounter);
				  addToParent(elParentNode, "UID", content);

				  addToParent(elParentNode, "FieldID", "205520904");

				  content = (String) membersMap.get(person.SELECT_ID);
				  addToParent(elParentNode, "Value", content);
				  resourceCounter++;

				  //find the person id of the person with the given Last name First Name
				  //if it is not found then add to the _personInfo for caching
				  if(_personInfo != null && _personInfo.get(personName) == null)
				  {
					  //found new Last Name First Name, add to cache
					  //this will be reused when the user adds the user as task assignee
					  _personInfo.put(personName, membersMap.get(person.SELECT_NAME));
				  }
			  }
		  }

		  elParentNode= elProjectRoot;
		  Element elAssignmentsNode = new Element("Assignments", null, projectNamespaceUri);
		  elParentNode.addContent(elAssignmentsNode);

		  codeRegn = "Assignment XML done.";
		  //Loops through all the task of the project to get the task info
		  ListIterator itr = taskList.listIterator();

		  while (itr.hasNext())
		  {
			  Map map = new HashMap();
			  map = (Map) itr.next();

			  getTaskStructure(context, map, elTasksNode, elResourcesNode, elAssignmentsNode, resourceIndexMap, 1);
		  }

		  elResponseRoot.setAttribute("result", "success");
		  if (debug)
		  {
			  System.out.println("Sending processed XML back to MS Project...Done");
			  xmlfileGen_EndTime = System.currentTimeMillis();

			  long total_xmlfileGenTime = xmlfileGen_EndTime - xmlfileGen_BeginTime;
		  }

		  xmlfileGen_EndTime = System.currentTimeMillis();
		  long total_xmlfileGenTime = xmlfileGen_EndTime - xmlfileGen_BeginTime;
		  if(debug)
			  System.out.println("==========================END   executeCheckoutSubproject============================================");
		  return elProjectRoot;
	  }
 	  catch(Exception e)
	  {
		  //e.printStackTrace();
		  System.out.println("{Exception} : " + e.getMessage());
		  throw new MatrixException(e.getMessage());
	  }
  }

  /**
   * Get an  XML string representing the Project Space.
   *
   * @param busid String value of the BusID identifying the Project Space object
   * @throws Exception if the operation fails
   */
  protected void getTaskStructure(Context context,
		                  Map     map,
				  Element elTasksNode,
				  Element elResourcesNode,
				  Element elAssignmentsNode,
				  Map resourceIndexMap,
				  int     currentLevel
				) throws Exception
  {
	  if(debug)
		  System.out.println("==========================START   getTaskStructure============================================" + map);
	  getTaskStructure_startTime = System.currentTimeMillis();

	  Task newTask = new Task();
	  StringList taskRelSelects = new StringList(1);
	  taskRelSelects.add(subtask.SELECT_TASK_WBS);

	  StringList taskSelects = new StringList(11);
	  taskSelects.add(task.SELECT_ID);
	  taskSelects.add(task.SELECT_NAME);
	  taskSelects.add(task.SELECT_OWNER);
  	  taskSelects.add(task.SELECT_TYPE);
	  taskSelects.add(task.SELECT_DESCRIPTION);
	  taskSelects.add(task.SELECT_CURRENT);
	  taskSelects.add(task.SELECT_PERCENT_COMPLETE);
	  taskSelects.add(task.SELECT_TASK_ESTIMATED_DURATION);
	  taskSelects.add(task.SELECT_TASK_ESTIMATED_START_DATE);
	  taskSelects.add(task.SELECT_TASK_ESTIMATED_FINISH_DATE);
	  taskSelects.add(task.SELECT_TASK_ACTUAL_DURATION);
	  taskSelects.add(task.SELECT_TASK_ACTUAL_START_DATE);
	  taskSelects.add(task.SELECT_TASK_ACTUAL_FINISH_DATE);
	  
	  //[2011x Start: Adding Task Constraint Type and Date in TaskStringList]
	  taskSelects.add(task.SELECT_TASK_CONSTRAINT_TYPE);
	  taskSelects.add(task.SELECT_TASK_CONSTRAINT_DATE);
	  taskSelects.add(task.SELECT_DEFAULT_CONSTRAINT_TYPE);
	  //[2011x End : Adding Task Constraint Type in TaskStringList]
	  
	  taskSelects.add(task.SELECT_PREDECESSOR_IDS);
	  taskSelects.add(task.SELECT_PREDECESSOR_TYPES);
	  taskSelects.add(task.SELECT_PREDECESSOR_LAG_TIMES);
	  taskSelects.add(task.SELECT_TASK_REQUIREMENT);
	  taskSelects.add(task.SELECT_HAS_SUBTASK);
	  taskSelects.add(task.SELECT_POLICY);
	  taskSelects.add(task.SELECT_BASELINE_CURRENT_END_DATE);

	  String currentTaskId =  (String) map.get(task.SELECT_ID);
  	  String parentProjectId = (String)taskParentMap.get(currentTaskId);
	  String taskCounter = (String)taskIndexMap.get(currentTaskId);

	  newTask.setId(currentTaskId);

	  // determine dependencies
	  String preType = new String();
	  String preId = new String();
	  String lagTime = "0";
	  preType = "";

	  String tn = (String) map.get(task.SELECT_NAME);
	  Object listPreds = (Object) map.get(task.SELECT_PREDECESSOR_IDS);

	  Object listTypes = (Object) map.get(task.SELECT_PREDECESSOR_TYPES);

	  Object listPredsLagTime = (Object) map.get(task.SELECT_PREDECESSOR_LAG_TIMES);
	  //	Object listPredsLagTimeUnit = (Object) map.get("from[Dependency].attribute[" + DependencyRelationship.ATTRIBUTE_LAG_TIME + "].inputunit");

	  Element elCurrentNode = null;
	  Element elParentNode = null;
	  Element elTaskNode = null;
	  String content = "";
	  String sConstraintType = null;

	  elTaskNode = new Element("Task", null, projectNamespaceUri);
	  elTasksNode.addContent(elTaskNode);
	  elParentNode = elTaskNode;
	
	  int parentIndx1 = projectList.indexOf(parentProjectId);
	  content = String.valueOf(taskCounter);
	  content = Integer.toString(parentIndx1)+"#"+content;
	  addToParent(elParentNode, "UID", content);

	  content = String.valueOf(taskCounter);
	  addToParent(elParentNode, "ID", content);

  	  String taskName = null;
	  taskName = (String) map.get(task.SELECT_NAME);
	  addToParent(elParentNode, "Name", taskName);

	  content = (String) map.get(subtask.SELECT_TASK_WBS);
	  addToParent(elParentNode, "WBS", content);

	  content = (String) map.get(subtask.SELECT_TASK_WBS);
	  addToParent(elParentNode, "OutlineNumber", content);

	  content = (String) map.get(task.SELECT_PERCENT_COMPLETE);
	  //Integer newPercent = new Integer(Double.valueOf(content).intValue());//if value is 46.7, prev it was returning 46 now it returns 47
	  Integer newPercent = new Integer(Math.round(Float.valueOf(content)));
	  content = Integer.toString(newPercent.intValue());
	  addToParent(elParentNode, "PercentComplete", content);

	  if(isProjectSpace(context,(String)map.get(task.SELECT_TYPE)))
	  {
		  sConstraintType = (String) map.get(task.SELECT_DEFAULT_CONSTRAINT_TYPE);
		  content = GetConstraintEnumValues(sConstraintType);			
			addToParent(elParentNode, "ConstraintType", content);
	  }
	  else
	  {
		  //[2011x Start: Task Constraint Type and Date  nodes are created  for response xml]	
		  sConstraintType = (String) map.get(task.SELECT_TASK_CONSTRAINT_TYPE);
		  content = GetConstraintEnumValues(sConstraintType);
		  
		  addToParent(elParentNode, "ConstraintType", content);
	  }	  
	  
	  content= (String)map.get(SELECT_TASK_CONSTRAINT_DATE);
	  if (content!= null && !content.equals(""))
	  {
		  Date taskConstraintDateFormat= MATRIX_DATE_FORMAT.parse(content,new java.text.ParsePosition(0));
		  content = sdf.format(taskConstraintDateFormat);
  	  }

	  addToParent(elParentNode, "ConstraintDate", content);
	  //[2011x End: Task Constraint Type and Date  nodes are created  for response xml]
	  
	  addToParent(elParentNode, "DurationFormat", "53");
	  String sDuration = (String) map.get(task.SELECT_TASK_ESTIMATED_DURATION);

	  if (!sDuration.equals(""))
 	  {
		  Integer newDuration = new Integer(Double.valueOf(sDuration).intValue());
		  Double new2Duration = new Double(Double.valueOf(sDuration).doubleValue());
		  content = "PT" + Integer.toString(newDuration.intValue() * 8) + "H" 
			+ Long.toString(Math.round(new2Duration.doubleValue() * 8 * 60 - newDuration.intValue() * 8 * 60 )) + "M0S"; //+ "H0M0S";
	  }
	  addToParent(elParentNode, "Duration", content);

	  //	String duarationValue = (String) map.get("attribute[Task Estimated Duration].inputvalue");
	  //	String duarationUnit = (String) map.get("attribute[Task Estimated Duration].inputunit");
	  //	content = duarationValue+ " " + duarationUnit;
	  //   	addToParent(elParentNode, "estDuration", content);

	  // Date formate -> 2003-03-11T08:00:00
	  //set the estimated start and duration the finish date is computed.
	  content = (String) map.get(SELECT_TASK_ESTIMATED_START_DATE);
	  ///////////////////////////////////10.8.0.2.LA /////////////////////////////////////////////
	  //date = new Date(content);
	  Date date = MATRIX_DATE_FORMAT.parse(content,new java.text.ParsePosition(0));
	  
	  content = sdf.format(date);
	  addToParent(elParentNode, "Start", content);

	  content = (String) map.get(SELECT_TASK_ESTIMATED_FINISH_DATE);
 	  if(!content.equals(""))
	  {
		  ///////////////////////////////////10.8.0.2.LA ///////////////////////////////////////////// 
	     //date = new Date(content);
         date = MATRIX_DATE_FORMAT.parse(content,new java.text.ParsePosition(0));
		 content = fdf.format(date);
		 addToParent(elParentNode, "Finish", content);
	  }
	  
 	 content = "";
 	  if(sProjectSchduleBasedOn.equals("Actual"))
 	  {	  	  
		  content = (String) map.get(SELECT_TASK_ACTUAL_START_DATE);
		  if (content!= null && !content.equals(""))
		  {
			  date = MATRIX_DATE_FORMAT.parse(content,new java.text.ParsePosition(0));
			  content = sdf.format(date);
		  }
		  addToParent(elParentNode, "ActualStart", content);

		  content = (String) map.get(SELECT_TASK_ACTUAL_FINISH_DATE);
		  if (content!= null && !content.equals(""))
		  {
			  date = MATRIX_DATE_FORMAT.parse(content,new java.text.ParsePosition(0));
			  content = fdf.format(date);
		  }
		  addToParent(elParentNode, "ActualFinish", content);

		  sDuration = (String) map.get(task.SELECT_TASK_ACTUAL_DURATION);

		  if (!sDuration.equals(""))
		  {
			  Integer newDuration = new Integer(Double.valueOf(sDuration).intValue());
			  Double new2Duration = new Double(Double.valueOf(sDuration).doubleValue());
	
			  content = "PT" + Integer.toString(newDuration.intValue() * 8) + "H" 
					+ Long.toString(Math.round(new2Duration.doubleValue() * 8 * 60 - newDuration.intValue() * 8 * 60 )) + "M0S"; //+ "H0M0S";
			  //Integer newDuration = new Integer(Double.valueOf(sDuration).intValue());
	  		  //content = "PT" + Integer.toString(newDuration.intValue() * 8) + "H0M0S";
		  }
		  addToParent(elParentNode, "ActualDuration", content);
 	  }
 	  else
 	  {
 		 addToParent(elParentNode, "ActualStart", content);
 		 addToParent(elParentNode, "ActualFinish", content);
 		 addToParent(elParentNode, "ActualDuration", content);
 	  } 	   

	  content = (String) map.get(task.SELECT_CURRENT);
	  addToParent(elParentNode, "State", content);
	  
	  content = (String) map.get(task.SELECT_OWNER);
  	  content = getPersonLastNameFirstName(context, content);
	  addToParent(elParentNode, "Owner", content);

	  content = (String) map.get(task.SELECT_DESCRIPTION);

	  addToParent(elParentNode, "Description", content);

	  content = (String) map.get(task.SELECT_TASK_REQUIREMENT);
	  addToParent(elParentNode, "TaskReq", content);

	  content = (String) map.get(task.SELECT_TYPE);
	  addToParent(elParentNode, "NodeType", content);

	  //Begin to create policy xmlnode for Task Node - 057630V6R2011x
	  content = (String) map.get(task.SELECT_POLICY);
	  addToParent(elParentNode, "Policy", content);
	  //End to create policy xmlnode for Task Node

	  String effortDrivenFlag = "0";
	  addToParent(elParentNode, "EffortDriven", effortDrivenFlag);
	  elCurrentNode = new Element("ExtendedAttribute", null, projectNamespaceUri);
	  elParentNode.addContent(elCurrentNode);

	  elParentNode = elCurrentNode;
	  content = String.valueOf(taskCounter);
	  addToParent(elParentNode, "UID", content);
	  addToParent(elParentNode, "FieldID", "188743731");
	  content = (String) map.get(task.SELECT_ID);
	  addToParent(elParentNode, "Value", content);

	  elParentNode = elTaskNode;
	  String  taskType= (String) map.get(task.SELECT_TYPE);
	  //create predecessor(dependency) tags
	  if (listPreds == null)
	  {
		  preType = "";
		  preId = "";
		  lagTime = "0";
	  }
	  else if (listPreds instanceof String)
	  {
		  String predId = (String) listPreds;
		  preId = (String)taskIndexMap.get(predId);
		  String preParentId = (String)taskParentMap.get(predId);

		  int preParentIndx = projectList.indexOf(preParentId);

		  if(preParentIndx>=0)
		  {
			  preId = preParentIndx + "#" + preId;
		  }

		  preType = (String) listTypes;
		  lagTime = (String) listPredsLagTime ;

		  if(lagTime.charAt(0) != '-' && !lagTime.equals("0"))
		  {
				lagTime = "+" + lagTime;
		  }
		  else if(lagTime.equals("0"))
		  {
			  lagTime = "+0";
		  }

		  if (preId == null)
		  {
			  //preString = "*"; //external ids not supported in MSP
		  }
		  else
		  {
			  elCurrentNode = new Element("PredecessorLink", null, projectNamespaceUri);
			  elParentNode.addContent(elCurrentNode);
			  elParentNode = elCurrentNode;

			  //				AttributeType attrType = new AttributeType(DependencyRelationship.ATTRIBUTE_LAG_TIME);
			  //				Dimension dimension = attrType.getDimension(context);
			  //				UnitList unitList = null;
			  //				try {
			  //                //Dimension dimension = attrType.getDimension(context);
			  //                if (dimension == null) {
			  //                    unitList = new UnitList();
			  //                }
			  //                else {
			  //                    unitList = dimension.getUnits(context);
			  //                }
			  //            }
			  //            catch (Exception e) {
			  //                unitList = new UnitList();
			  //            }
			  //
			  //					   if (unitList.size() != 0) {
		      //                            for(UnitItr uitr = new UnitItr(unitList); uitr.next();) {
			  //                                Unit unit = (Unit) uitr.obj();
			  //                                if (unit.getName().equals(lagTimeUnit)) {
			  //                                    lagTime = unit.denormalize(lagTime);
			  //                                    break;
			  //                                }
		      //                            }
			  //                        }
			  //
			  content = preId;
			  addToParent(elParentNode, "PredecessorUID", content);
			  String mspPreType = getPredecessorType(preType, true);
			  addToParent(elParentNode, "Type", mspPreType);
			  addToParent(elParentNode, "LinkLag", lagTime);
			  //				addToParent(elParentNode, "LagFormat", lagTimeUnit);

			  //set the parent node back to the task
			  elParentNode = elTaskNode;
		  }
	  }//else if (listPreds instanceof String)
	  else if (listPreds instanceof StringList)
	  {
		  StringList sl = (StringList) listPreds;
		  StringList st = (StringList) listTypes;
		  StringList sLag = (StringList) listPredsLagTime ;
		  for (int k =0; k<sl.size(); k++)
		  {
			  String predId = (String) (String) sl.elementAt(k);
			  preId = (String)taskIndexMap.get(predId);
			  String preParentId =(String) taskParentMap.get(predId);
			  int preParentIndx = projectList.indexOf(preParentId);
			
			  if(preParentIndx>=0)
			  {
				  preId = preParentIndx + "#" + preId;
			  }
			  if (preId != null)
			  {
				  preType = (String) st.elementAt(k);
				  lagTime = (String) sLag.elementAt(k);
				  if(lagTime.charAt(0) != '-' && !lagTime.equals("0"))
				  {
					  lagTime = "+" + lagTime;
				  }
				  else if(lagTime.equals("0"))
				  {
					  lagTime = "+0";
				  }

				  String mspPreType = getPredecessorType(preType, true);
				  elCurrentNode = new Element("PredecessorLink", null, projectNamespaceUri);
				  elParentNode.addContent(elCurrentNode);
				  elParentNode = elCurrentNode;

				  content = preId;
				  addToParent(elParentNode, "PredecessorUID", content);
				  String sPreType = getPredecessorType(preType, true);
				  addToParent(elParentNode, "Type", mspPreType);
				  addToParent(elParentNode, "LinkLag", lagTime);

				  //set the parent node back to the task
				  elParentNode = elTaskNode;
			  }
		  }//for (int k =0; k<sl.size(); k++)
	  }//else if (listPreds instanceof StringList)
		
	  //get the assignees info
	  StringList busSelects = new StringList();
	  busSelects.add(person.SELECT_ID);
	  busSelects.add(person.SELECT_NAME);
	  busSelects.add(person.SELECT_FIRST_NAME);
	  busSelects.add(person.SELECT_MIDDLE_NAME);
	  busSelects.add(person.SELECT_LAST_NAME);
	  busSelects.add(person.SELECT_EMAIL_ADDRESS);
	  busSelects.add(person.SELECT_COMPANY_NAME);
	  MapList assigneesList = newTask.getAssignees(context, busSelects, null, null);
	  ListIterator assigneeItr = assigneesList.listIterator();
  	  
	  //Map resourceIndexMap = new HashMap();
	  int assigneeCounter = 0;
	  while (assigneeItr.hasNext())
	  {
		  Map assigneeMap = (Map) assigneeItr.next();
		  String personName = (String)assigneeMap.get(person.SELECT_LAST_NAME) + " " + (String)assigneeMap.get(person.SELECT_FIRST_NAME);
		  //String personName = (String)assigneeMap.get(person.SELECT_NAME);
		  //add to the resourceMap if the person is not already added to the resource list
		  if(resourceIndexMap.get(personName) == null)
		  {
			  int resourceCounter = resourceIndexMap.size() + 1;
			  resourceIndexMap.put(personName,  String.valueOf(resourceCounter));
			  //create the resource details
			  elCurrentNode = new Element("Resource", null, projectNamespaceUri);
			  elParentNode = elResourcesNode;
			  elParentNode.addContent(elCurrentNode);
			  elParentNode = elCurrentNode;

			  content = String.valueOf(resourceCounter);
			  addToParent(elParentNode, "UID", content);
			  addToParent(elParentNode, "ID", content);

			  content = personName;
			  addToParent(elParentNode, "Name", content);
			  addToParent(elParentNode, "Type", "1");
			  addToParent(elParentNode, "IsNull", "0");
			  addToParent(elParentNode, "WorkGroup", "0");

			  content = (String)assigneeMap.get(person.SELECT_EMAIL_ADDRESS);
			  addToParent(elParentNode, "EmailAddress", content);
			  addToParent(elParentNode, "OverAllocated", "0");
			  addToParent(elParentNode, "CanLevel", "1");
			  addToParent(elParentNode, "AccrueAt", "3");

			  elCurrentNode = new Element("ExtendedAttribute", null, projectNamespaceUri);
			  elParentNode.addContent(elCurrentNode);

			  elParentNode = elCurrentNode;
			  content = String.valueOf(resourceCounter);
			  addToParent(elParentNode, "UID", content);

			  addToParent(elParentNode, "FieldID", "205520904");

			  content = (String) assigneeMap.get(person.SELECT_ID);
			  addToParent(elParentNode, "Value", content);
		  }
			
  		  //assign the resource to a task
		  elCurrentNode = new Element("Assignment", null, projectNamespaceUri);
		  elParentNode = elAssignmentsNode;
		  elParentNode.addContent(elCurrentNode);
		  elParentNode = elCurrentNode;
			
		  //content = String.valueOf(resourceCounter);
		  content = String.valueOf(assigneeCounter);
		  addToParent(elParentNode, "UID", content);
		  content = String.valueOf(taskCounter);
		  addToParent(elParentNode, "TaskID", content);
		  String taskUid = taskCounter;
		  int taskParentIndx = projectList.indexOf(parentProjectId);
		  if(taskParentIndx>=0)
		  {
			  taskUid = taskParentIndx + "#" + taskUid;
		  }
		  content = taskUid;
		  addToParent(elParentNode, "TaskUID", content);
		  //get the resource id from the map
		  content = (String) resourceIndexMap.get(personName);
		  addToParent(elParentNode, "ResourceUID", content);

		  String currentPersonName = (String)assigneeMap.get(person.SELECT_NAME);
		  String percent = getAllocationPercent(context, currentTaskId, currentPersonName);
		  double allocatedPercent = (Double.valueOf(percent)).doubleValue();

		  Float f = new Float((1.0)*allocatedPercent);
		  addToParent(elParentNode, "Units", f.toString());
	  }//while (assigneeItr.hasNext())

  	  //set the parent node back to the task
	  elParentNode = elTaskNode;
	  
	  //check if the current task has sub-tasks and build the sub-task structure
	  if(((String)map.get(task.SELECT_HAS_SUBTASK)).equalsIgnoreCase("true") &&isProjectSpace(context,(String)map.get(task.SELECT_TYPE))!=true )
	  {
		  addToParent(elParentNode, "Type", "1");
		  addToParent(elParentNode, "Summary", "1");
		  addToParent(elParentNode, "Rollup", "1");
		  content = Integer.toString(currentLevel++);
		  addToParent(elParentNode, "OutlineLevel", content);

		  MapList subTaskList = newTask.getTasks(context, newTask, 1, taskSelects, taskRelSelects);

		  //Loop through the subtask of the current task and get the task info
		  ListIterator itr = subTaskList.listIterator();

	  	  while (itr.hasNext())
	 	  {
			  Map subTaskMap = (Map) itr.next();
 
			  // in getTaskStructure(..)
			  getTaskStructure(context, subTaskMap, elTasksNode, elResourcesNode, elAssignmentsNode, resourceIndexMap, currentLevel);
		  }
	  }
	  else if(isProjectSpace(context,(String)map.get(task.SELECT_TYPE))==true )
	  {
		  addToParent(elParentNode, "Type", "1");
		  addToParent(elParentNode, "Summary", "1");
		  addToParent(elParentNode, "Rollup", "1");
		  content = Integer.toString(currentLevel++);
		  addToParent(elParentNode, "OutlineLevel", content);

		  Element elFFCRoot = new Element("transaction");
		  elFFCRoot.setAttribute("focusbrowser", "7");
		  elFFCRoot.setAttribute("loglevel", "true");
		  elFFCRoot.setAttribute("cname", "findforcheckout");
		  elFFCRoot.setAttribute("tid", "2");
		  elFFCRoot.setAttribute("type", "command");
		  elFFCRoot.setAttribute("mpecver", "1.0.0.0");
		  elFFCRoot.setAttribute("MSP", "MSP2000");

		  // now create placeholder for response
		  Element elResponseArgumentsNode = new Element("arguments");
		  elFFCRoot.addContent(elResponseArgumentsNode);

		  // create the busid argument placeholder
		  Element elBusIdArgument = new Element("argument");
		  elBusIdArgument.setAttribute("name", "busid");
		  elBusIdArgument.setText(currentTaskId);
		  elResponseArgumentsNode.addContent(elBusIdArgument);

		  //Doubt
		  // create the edit status argument placeholder
		  // on Merge to ematrix the edit flag will be reset to false
		  // the user will have to explicitly "Launch the project in Edit mode"
		  // inorder to make any further changes
		  Element elEditStatusArgument = new Element("argument");
		  elEditStatusArgument.setAttribute("name", "foredit");
		  elEditStatusArgument.setText("true");
		  elResponseArgumentsNode.addContent(elEditStatusArgument);

		  //use a temp name which is consistent with the way the servlet
		  //names the file such that dumpTransactionXMLForServlet can be reused.
		  String tempFileName = "findForCheckOut_1_2_3";
		  //String fileName = dumpTransactionXMLForServlet (context, gco, tempFileName, elFFCRoot);
		  String fileName = dumpTransactionXMLForServlet (context, tempFileName, elFFCRoot);
		  if (debug)
			  System.out.println("done dumping the transaction xml file");
		  String[] newArgs = new String[1];
		  newArgs[0] = fileName;

		  Element elSubprojectNode = executeCheckoutSubproject(context, newArgs);
		  elParentNode.addContent(elSubprojectNode);
	  }//else if(isProjectSpace(context,(String)map.get(task.SELECT_TYPE))==true )
	  else
	  {
		  /**
		  Definition values for DefaultTaskType
		  0 = Fixed Unit
		  1 = Fixed Duration
		  2 = Fixed Work
		  */
		  // Because the task is currently set to "Fixed Unit" always
		  // the duration on the client is in-correct when resource allocation is supported
		  // PMC Only supports "Fixed Duration" at this time, so the integration
		  // should support only "Fixed Duration which is: 1"
		  addToParent(elParentNode, "Type", "1");

		  addToParent(elParentNode, "Summary", "0");
		  addToParent(elParentNode, "Rollup", "0");
		  content = Integer.toString(currentLevel++);
		  addToParent(elParentNode, "OutlineLevel", content);
	  }
	  
	  if (debug)
	  {
		  getTaskStructure_endTime = System.currentTimeMillis();
		  long getTaskStructure_totalTime = getTaskStructure_endTime - getTaskStructure_startTime;
	  	 System.out.println("==========================END   getTaskStructure============================================");
	  }
  }

  /**
   * Get an  XML string representing the Project Space.
   *
   * @param busid String value of the BusID identifying the Project Space object
   * @throws Exception if the operation fails
  */
  public String executeSynchronizeFromeMatrix(Context context, String[] args) throws MatrixException
  {
	  return executeFindForCheckout(context, args);
  }

  /**
   * Get an  XML string representing the Project Space.
   *
   * @param busid String value of the BusID identifying the Project Space object
   * @throws Exception if the operation fails
  */
  public String executeSynchronizeToeMatrix(Context context, String[] args) throws MatrixException
  {
	  return executeSynchronizeMerge(context, args); 
  }

  public String executeProjectExistenceCheck(Context context, String[] args)
  throws MatrixException
  {
	  try
	  {
		  Element elCommandRoot = loadXMLSentFromServlet(args[0], context);
		  Element elCommandArguments = elCommandRoot.getChild("arguments");

		  List lArguments = elCommandArguments.getChildren("argument");
		  ListIterator litCtr = lArguments.listIterator();
		  
		  String strBusId = "";	 
		  Boolean bExistingProject = false;
		  
		  while (litCtr.hasNext())
		  {
			  Element elArgument = (Element) litCtr.next();

			  List lArgu = elArgument.getChildren();
			  ListIterator litArg = lArgu.listIterator();

			  if(elArgument.getAttributeValue("name").equals("busid"))
			  {
				  if(elArgument != null)
				  {
					  strBusId = elArgument.getText();

					  if(strBusId != null && !strBusId.equals(""))
					  {
						  bExistingProject = checkObjectExists(context,strBusId);						  
					  }					  
				  }
			  }
		  }
		  Element elRespCommandRoot = loadXMLSentFromServlet(args[0], context);
		  Element elResponseRoot = new Element("transaction");
		  java.util.List attribList = elRespCommandRoot.getAttributes();
		  ListIterator litAtr = attribList.listIterator();

		  while (litAtr.hasNext())
		  {
			  litAtr.next();

			  elResponseRoot.setAttribute("focusbrowser", elRespCommandRoot.getAttributeValue("focusbrowser"));

			  elResponseRoot.setAttribute("loglevel", elRespCommandRoot.getAttributeValue("loglevel"));

			  elResponseRoot.setAttribute("cname", elRespCommandRoot.getAttributeValue("cname"));

			  elResponseRoot.setAttribute("tid", elRespCommandRoot.getAttributeValue("tid"));

			  elResponseRoot.setAttribute("type", elRespCommandRoot.getAttributeValue("type"));

			  elResponseRoot.setAttribute("mpecver", elRespCommandRoot.getAttributeValue("mpecver"));

			  elResponseRoot.setAttribute("MSP", elRespCommandRoot.getAttributeValue("MSP"));
		  }

		  //generate the xml
		  elResponseRoot.getAttribute("type").setValue("response");		  
		  
		  Element isProjectExist = new Element("ProjectExist");
		  isProjectExist.setText(bExistingProject.toString());
		  elResponseRoot.addContent(isProjectExist);

		  elResponseRoot.setAttribute("result", "success");
		  String tempFileName = "IsProjectExist_1_2_3";		  
		  String fileName = dumpTransactionXMLForServlet (context, tempFileName, elResponseRoot);
		
		  if(debug)
			  System.out.println("==========================END   executeProjectExistenceCheck============================================");
		  return tempFileName;
	  }
	  catch(Exception e)
	  {
		  e.printStackTrace();
		  throw new MatrixException(e.getMessage());
	  }
  }
  //merge-
  /**
   * Get an  XML string representing the Project Space.
   *
   * @param busid String value of the BusID identifying the Project Space object
   * @throws Exception if the operation fails
  */
  public String executeSynchronizeMerge(Context context, String[] args)
	                                                 throws MatrixException
  {
	  codeRegn = "inside executeSynchronizeMerge";
	  if(debug)
	  {
		  System.out.println("==========================START   executeSynchronizeMerge============================================");
		  System.out.println(codeRegn);
		  mergeStartTime = System.currentTimeMillis();
	  }
	  mergeStartTime = System.currentTimeMillis();
	  try
	  {
		  // start a write transaction
		  ContextUtil.startTransaction(context, true);

		  //this reflects the user modified project in MS Project
		  Element elProjectRoot = null;
		  //this reflects the project data when user downloaded the project for modification
		  Element elOldProjectRoot = null;
		  //this reflects the current project data in Program Central
		  //(changes could have happened in PC when the user was modifying
		  //the data in MS Project)
		  Element elCurProjectRoot = null;

		  // get the settings required to load the xml dropped by servlet
		  /*
		  //Following Code is not required now since this JPO now reads the values from the MSOI GCO
		  IntegrationGlobalConfigObject gco = new IntegrationGlobalConfigObject();
		  getAttribute(context, gco, IntegrationGlobalConfigObject.IN_RIP_MODE);
		  getAttribute(context, gco, IntegrationGlobalConfigObject.LOCAL_PROJECT_FILEPATH);
		  */
		  codeRegn = "GCO read.";
		  // load the xml
		  //Element elCommandRoot = loadXMLSentFromServlet(gco, args[0], context);
		  Element elCommandRoot = loadXMLSentFromServlet(args[0], context);
		  if (debug)
			  System.out.println("Read the xml sent from client...done");

		  codeRegn = "XML sent from servlet read.";

		  // get the projectXml, Original Project XML
		  Element elCommandArguments = elCommandRoot.getChild("arguments");

		  List lArguments = elCommandArguments.getChildren("argument");
		  ListIterator litCtr = lArguments.listIterator();

		  String strBusId = "";
		  boolean existingProject = false;
		  if (debug)
			  System.out.println("ExistingProject = "+existingProject);
		  while (litCtr.hasNext())
		  {
			  Element elArgument = (Element) litCtr.next();

			  List lArgu = elArgument.getChildren();
			  ListIterator litArg = lArgu.listIterator();

			  if(elArgument.getAttributeValue("name").equals("projectxml"))
			  {
			  }
			  if(elArgument.getAttributeValue("name").equals("prevprojectxml"))
			  {
			  }
			  if(elArgument.getAttributeValue("name").equals("busid"))
			  {
				  if(elArgument != null)
				  {
					  strBusId = elArgument.getText();
					  project.setId(strBusId);
					  if(strBusId != null && !strBusId.equals(""))
					  {
						  existingProject = true;
					  }
					  if(existingProject)
					  {
						  // determine if user can edit
						  boolean editFlag = project.checkAccess(m_ctxUserContext, (short) AccessConstants.cModify);
						  if(!editFlag)
						  {
							  String stringValue = "";
							  try
							  {
								  stringValue = i18nStringNow("emxIEFDesignCenter.Common.NoModify", userLanguage);
							  }
							  catch(Exception ex)
							  {
								  if(debug)
									  System.out.println("[emxMSProjectIntegration.executeSynchronizeMerge] No modify access. ERROR : " + ex.getMessage());
								  stringValue = "No modify access";
							  }
							  throw new Exception(stringValue);
						  }
					  }
				  }
			  }
			  if(elArgument.getAttributeValue("name").equals("projectxml"))
			  {
				  elProjectRoot = elArgument.getChild("Project",ns);
			  }
			  else if(elArgument.getAttributeValue("name").equals("prevprojectxml"))
			  {
				  elOldProjectRoot = elArgument.getChild("Project",ns);
			  }
		  }//while (litCtr.hasNext())
		
		  codeRegn = "Read XML for MS Project data";
		  Map taskIndexMap = new HashMap();
		  MapList dependencyMapList = new MapList();
		  //newProjectMap collects data of current project coming from MS Project through tempCommand.xml  
		  Map newProjectMap = readXMLDataForMerge(context, elProjectRoot, existingProject,true,taskIndexMap,dependencyMapList);
		  if (debug)
			  System.out.println("Read XML data...done");

		  //obtain data from the map
		  //project info, task info, resource map, assignments
		  Map prjInfoMap          = (Map) newProjectMap.get("projectData");
		  Map resourceIndexMap    = (Map) newProjectMap.get("resourceIndexMap");
		  StringList resourceList = (StringList) newProjectMap.get("resourceList");
		  MapList assigneeMapList     = (MapList) newProjectMap.get("assigneeMapList");
		  //[2011x Start: taskdataMapList is the place where task constraint type and dates are stored from taskData coming from readXMLDataForMerge method]
		  MapList taskDataMapList = (MapList) newProjectMap.get("taskData");
		  //[2011x End: taskdataMapList is the place where task constraint type and dates are stored from taskData coming from readXMLDataForMerge method]
		  
		  Map taskLevelMap = (Map) newProjectMap.get("taskLevelMap");
		  Map taskUnitMap = (Map) newProjectMap.get("taskUnitsDataMap");
		  		  
		  if (debug)
			  System.out.println("taskLevelMap : "+taskLevelMap);

		  //java.text.SimpleDateFormat mspDate = new java.text.SimpleDateFormat("MM/dd/yyyy");
		  java.text.SimpleDateFormat mspDate = new java.text.SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(),
                     Locale.US);
		  codeRegn = "accessed the map values ";

		  String owner = ProgramCentralConstants.EMPTY_STRING;
		  String estStartDate = ProgramCentralConstants.EMPTY_STRING;
		  String estFinishDate = ProgramCentralConstants.EMPTY_STRING;
		  String constraintDate=ProgramCentralConstants.EMPTY_STRING;
		  String actStartDate = ProgramCentralConstants.EMPTY_STRING;
		  String actFinishDate = ProgramCentralConstants.EMPTY_STRING;

		  if(!existingProject)
		  {		
			  //IR-092738V6R2013 begin
			  ArrayList<String> inValidResourceNamesList = new ArrayList<String>();
			  if (assigneeMapList.size() != 0)
			  {
				  Map resourceNameMap    = (Map) newProjectMap.get("resourceNameMap");
				  ListIterator listAssignmentCtr = assigneeMapList.listIterator();
				  while (listAssignmentCtr.hasNext())
				  {					  
					  Map assigneeMap = (Map) listAssignmentCtr.next();
					  String resourceUID = (String) assigneeMap.get("resourceUID");
					  String personID = (String) resourceIndexMap.get(resourceUID);
					  String personNm = ProgramCentralConstants.EMPTY_STRING;
					  if(ProgramCentralConstants.EMPTY_STRING.equalsIgnoreCase(personID))
					  {
						  personNm= (String) resourceNameMap.get(resourceUID);
						  inValidResourceNamesList.add(personNm);
					  }					  
				  }
				  if(inValidResourceNamesList.size() > 0)
				  {
					 return "true| " + executeResponseForValidateResource(context,args,inValidResourceNamesList, "invalidresource");					 
				  }	
				  //IR-092738V6R2013 end
			  }
			  if (debug)
				  System.out.println("existingProject = "+existingProject);
			  taskLevelMap.clear();
			  taskIndexMap.clear();
			  //busId does not exist, the xml supplied is for a brand new project
			  //get details of the project and create project
			  String projectName = (String) prjInfoMap.get("projectName");
              
              //[2011x Start: Collecting ProjectSchedule Info, Constraint Types for Project Summary Node from prjInfoMap]
              String schedulefromstart=(String)prjInfoMap.get("ScheduleFrom");
              String constraintTypeForProject=(String)prjInfoMap.get("constraintType");
              //[2011x End: Collecting ProjectSchedule Info, Constraint Types for Project Summary Node from prjInfoMap]
             
			  String description = (String) prjInfoMap.get("description");
			  owner = (String) prjInfoMap.get("Author");
			  sProjectSchduleBasedOn = (String) prjInfoMap.get("ScheduleBasedOn");
			  //String company = (String) prjInfoMap.getChild("Company").getText();
			  if (debug)
				  System.out.println("Not an existing project...");

			  String ctxPersonId = person.getPerson(context).getId();							   
			  person.setId(ctxPersonId);		  
			  //personName serves the purpose of allowing
			  // .getVault() method to return the vault name instead of "null" value
			  //DO NOT delete personName
			  String personName = person.getName(context);		 
			  String userVault = person.getVault();				 

			  //StringList vaultsList = GetVaults(context, ctxPersonId);
			  codeRegn = "created new project ";
			  if (debug)
				  System.out.println(codeRegn);

			  //project.create(context, projectName, project.getDefaultPolicy(context), project.getDefaultVault(context, null));
			  //[Changes made to support creation of Projects of Sub types from MSProject]
			  //project.create(context, projectName, project.getDefaultPolicy(context), userVault);
			  String projectType = "";
			  ListIterator Itr = taskDataMapList.listIterator();
			  if(Itr.hasNext())
			  {
				  Map projectMap = (Map) Itr.next();
				  projectType = (String) projectMap.get("taskType");
			  }
			  mspiCreateProject(context,  project,  projectType,  projectName,  userVault) ;
			  //[/Changes made to support creation of Projects of Sub types from MSProject]
			  project.setDescription(context, description);		   
			  
              //[2011x Start: Setting Project Schedule attribute, Constraint Type in Program Central]
			  final String ATTRIBUTE_DEFAULT_CONSTRAINT_TYPE = PropertyUtil.getSchemaProperty("attribute_DefaultConstraintType");
			  Map projectAttributes = new HashMap();			  
			  projectAttributes.put(ATTRIBUTE_PROJECT_SCHEDULE_FROM, schedulefromstart);
			  projectAttributes.put(ATTRIBUTE_DEFAULT_CONSTRAINT_TYPE, constraintTypeForProject);
              
			  if(schedulefromstart.equals("Project Start Date"))
				  projectAttributes.put(ATTRIBUTE_TASK_CONSTRAINT_DATE, (String) prjInfoMap.get("ProjectStartDate"));
			  else
				  projectAttributes.put(ATTRIBUTE_TASK_CONSTRAINT_DATE, (String) prjInfoMap.get("ProjectFinishDate"));
			  
              project.setAttributeValues(context,projectAttributes);
              //[2011x End: Setting Project Schedule attribute, Constraint Type in Program Central]
              
              //To set Project Schedule Based on option to Actual                            
              project.setAttributeValue(context, ATTRIBUTE_SCHEDULEBASEDON, sProjectSchduleBasedOn);//FrameworkProperties.getProperty(context,"emxFramework.Range.Schedule_Based_On.Actual"));
			  
			  strBusId = project.getId();			   
			  ListIterator taskDataItr = taskDataMapList.listIterator();
			  String outlineNumber = ProgramCentralConstants.EMPTY_STRING;
			  String taskUID = ProgramCentralConstants.EMPTY_STRING;
			  String taskReq = ProgramCentralConstants.EMPTY_STRING;
			  if(taskDataItr.hasNext())
			  {
				  Map nodeMap = (Map) taskDataItr.next();

				  codeRegn = "setting the attributes ";
				  estStartDate = (String) nodeMap.get("estStartDate");
				  estFinishDate = (String) nodeMap.get("estFinishDate");
				  String percentComplete = (String) nodeMap.get("percentComplete");
				  String estDuration = (String) nodeMap.get("estDuration");
				  String actDuration = (String) nodeMap.get("actDuration");

				  codeRegn = "set the attribute values into a map";
				  Map attributeMap = new HashMap(5);

				  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_START_DATE, estStartDate);
				  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE, estFinishDate);

				  if(percentComplete != null && !percentComplete.equals("0"))
					  attributeMap.put(ATTRIBUTE_PERCENT_COMPLETE, percentComplete);

				  //if(estDuration != null && !estDuration.equals("0")) //To fix 91053 : Milestone is created with duration of 1 days in PMC when project is merged to PMC. Now PMC accepts the tasks with 0 (zero) duration.
				  if(estDuration != null && estDuration.length() > 0)
					  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_DURATION, estDuration);
				  //if(actDuration != null && !actDuration.equals("0")) //To fix 91053 : Milestone is created with duration of 1 days in PMC when project is merged to PMC. Now PMC accepts the tasks with 0 (zero) duration.
				  if(actDuration != null && actDuration.length() > 0)
					  attributeMap.put(ATTRIBUTE_TASK_ACTUAL_DURATION, actDuration);

				  //Doubtful
				  // setting the actual start and finish is controlled by the trigger JPO
				  // of the project on completion of the task/project. so not needed?
				  //busSelects.add(task.SELECT_TASK_ACTUAL_START_DATE);
				  //busSelects.add(task.SELECT_TASK_ACTUAL_FINISH_DATE);

				  //the first task node corresponds to the project

				  codeRegn = "setting project attribute values ";
				  if (debug)
					  System.out.println(codeRegn);
				  project.setAttributeValues(context, attributeMap);							 
			  }//if(taskDataItr.hasNext())
			
			  codeRegn = "create tasks";
			  Map taskActualDataList = new HashMap();
			  while (taskDataItr.hasNext())
			  {
				  Map taskActualDataMap = new HashMap();
				  project.setId(strBusId);
				  Map nodeMap = (Map) taskDataItr.next();
				  taskUID = (String) nodeMap.get("taskUID");
				  String taskName = (String) nodeMap.get("taskName");
				  //[2011x: Collecting Task constraint type info from map coming from readXMLDataForMerge method]
				  String constraintType=(String)nodeMap.get("constraintType");
				  //[/2011x: Collecting Task constraint type info from map coming from readXMLDataForMerge method]
				 
				  String taskDescription = (String) nodeMap.get("description");
				  String outlineLevel = (String) nodeMap.get("outlineLevel");
				  outlineNumber = (String) nodeMap.get("outlineNumber");
				  if (debug)
				  {
					  System.out.println("creating task with name: "+taskName);
					  System.out.println("outlineNumber : " + outlineNumber);
					  System.out.println("outlineLevel : " + outlineLevel);
				  }
				  taskReq = (String) nodeMap.get("taskReq");
				  String taskState = (String) nodeMap.get("taskState");
				  //estStartDate  = (String) nodeMap.get("estStartDate");
				  //estFinishDate = (String) nodeMap.get("estFinishDate");
				  owner = (String) nodeMap.get("owner");
				  String taskType = (String) nodeMap.get("taskType");

				  codeRegn = "Creating subproject";
				  Map subprojectMap = (Map) nodeMap.get("subprojectMap");

				  //If Task is a subproject then control will go inside this block
				  if (subprojectMap != null)
				  {
					  Map subprojectTaskIndexMap = new HashMap();
					  subprojectMap.put("taskIndexMap", subprojectTaskIndexMap);
					  String subprojectId = SynchronizeNewProject(context, subprojectMap);

					  subprojectTaskIndexMap = (Map) subprojectMap.get("taskIndexMap");
					  taskIndexMap.putAll(subprojectTaskIndexMap);

					  //used for obtaining a tasks parent given a outlineNumber of the child
					  taskLevelMap.put(outlineNumber, subprojectId);
					  //used for building resources and dependencies
					  //provides the PC taksID based on a MS Project task UID
					  taskIndexMap.put(taskUID, subprojectId);

					  String seqNo = ProgramCentralConstants.EMPTY_STRING;
					  String parentId;
					  String levelId = ProgramCentralConstants.EMPTY_STRING;

					  if(outlineNumber.lastIndexOf(".") == -1)
					  {
						  parentId = strBusId;
					  }
					  else
					  {
						  levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
						  parentId = (String) taskLevelMap.get(levelId);
					  }	

					  task.setId(parentId);
					  String []childIds = new String[1];
					  childIds[0] = subprojectId;
					  task.addExisting(context, childIds , parentId);
			
					  ProjectIds.add(subprojectId);
				  }//if (subprojectMap != null)
				  else if (isProjectSpace(context,taskType)==true)
				  {
					  mspiCreateProject(context,  subProject,  taskType,  taskName,  userVault) ;
					  String subprojectId = subProject.getId();
					  task.setId(subprojectId);

					  //used for obtaining a tasks parent given a outlineNumber of the child
					  taskLevelMap.put(outlineNumber, subprojectId);
					  //used for building resources and dependencies
					  //provides the PC taksID based on a MS Project task UID
					  taskIndexMap.put(taskUID, subprojectId);

					  subProject.setDescription(context, taskDescription);

					  codeRegn = "setting the attributes ";
					  estStartDate = (String) nodeMap.get("estStartDate");
					  estFinishDate = (String) nodeMap.get("estFinishDate");
					  String percentComplete = (String) nodeMap.get("percentComplete");
					  String estDuration = (String) nodeMap.get("estDuration");
					  String actDuration = (String) nodeMap.get("actDuration");
					  String constraintTypeForSubProject=(String)nodeMap.get("constraintType");

					  codeRegn = "set the attribute values into a map";
					  Map attributeMap = new HashMap(5);

					  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_START_DATE, estStartDate);
					  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE, estFinishDate);
					  attributeMap.put(ATTRIBUTE_DEFAULT_CONSTRAINT_TYPE, constraintTypeForSubProject);

					  if(percentComplete != null && !percentComplete.equals("0"))
						  attributeMap.put(ATTRIBUTE_PERCENT_COMPLETE, percentComplete);

					  if(estDuration != null && estDuration.length() > 0)
						  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_DURATION, estDuration);

					  if(actDuration != null && actDuration.length() > 0)
						  attributeMap.put(ATTRIBUTE_TASK_ACTUAL_DURATION, actDuration);

					  codeRegn = "setting project attribute values ";
					  subProject.setAttributeValues(context, attributeMap);		
					  subProject.setAttributeValue(context, ATTRIBUTE_SCHEDULEBASEDON, sProjectSchduleBasedOn); //set ScheduleBasedOn value same as Parent Project
					  String seqNo = null;
					  String parentId;

					  if(outlineNumber.lastIndexOf(".") == -1)
					  {
						  parentId = strBusId;
					  }
					  else
					  {
						  String levelId = ProgramCentralConstants.EMPTY_STRING;
						  levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
						  parentId = (String) taskLevelMap.get(levelId);
					  }	
					
					  task.setId(parentId);
					  String []childIds = new String[1];
					  childIds[0] = subprojectId;
					  task.addExisting(context, childIds , parentId);

					  ProjectIds.add(subprojectId);
				  }//else if (isProjectSpace(context,taskType)==true)
				  else
				  {
					  //START:Added:P6E:16-Aug-2011:PRG:R213:Bug 121547V6R2013
					  //task retaining policy of previous task, to get rid off that Initializing task again.
					  task = (Task) DomainObject.newInstance(m_ctxUserContext, DomainConstants.TYPE_TASK, DomainConstants.PROGRAM);
  					  //End:P6E:16-Aug-2011:PRG:R213:Bug 121547V6R2013
					  if(outlineNumber.lastIndexOf(".") == -1)
					  {
						  //if outlineNumber == "-1" then these are direct children of project
						  //String taskPolicy =  task.getDefaultPolicy(context,DomainConstants.TYPE_TASK);
						  // task.create(context, DomainConstants.TYPE_TASK, taskName, taskPolicy, project);
						  boolean retValue = mspiCreateTask(context, task, taskType, taskName, project, true);
						  codeRegn = "created task 1";
					  }
					  else
					  {
						  String levelId = ProgramCentralConstants.EMPTY_STRING;
						  levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
						  String parentId = (String) taskLevelMap.get(levelId);
						  if (debug)
						  {
							  System.out.println("levelId : " + levelId);
							  System.out.println("parentId : " + parentId);
						  }
						  parentTask.setId(parentId);
						  //String taskPolicy =  task.getDefaultPolicy(context,DomainConstants.TYPE_TASK);
						  //task.create(context, DomainConstants.TYPE_TASK, taskName, taskPolicy, parentTask);
						  boolean retValue = mspiCreateTask(context, task, taskType, taskName, parentTask, false);
						  codeRegn = "created task 2";
					  }

					  task.setDescription(context, taskDescription);				   
					  if(taskState != null && !taskState.equals(""))	   
						  task.setState(context, taskState);			   
					  task.setOwner(context, owner);				 

					  String currentTaskId = task.getId();
					  //used for obtaining a tasks parent given a outlineNumber of the child
					  taskLevelMap.put(outlineNumber, currentTaskId);
					  //used for building resources and dependencies
					  //provides the PC taksID based on a MS Project task UID
					  taskIndexMap.put(taskUID, currentTaskId);

					  codeRegn = "setting the attributes ";
					  estStartDate = (String) nodeMap.get("estStartDate");
					  estFinishDate = (String) nodeMap.get("estFinishDate");
					  actStartDate = (String) nodeMap.get("actStartDate");
					  actFinishDate = (String) nodeMap.get("actFinishDate");
					  constraintDate=(String) nodeMap.get("constraintDate");
					  String percentComplete = (String) nodeMap.get("percentComplete");
					  String estDuration = (String) nodeMap.get("estDuration");
					  String actDuration = (String) nodeMap.get("actDuration");

					  codeRegn = "set the attribute values into a map";
					  Map attributeMap = new HashMap(5);
					  //attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_START_DATE, estStartDate);

					  //do not set the finish date duration and start date is fine
					  //setting the finish date moves the start date back by 1 day
					  //for duration = 1

					  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_START_DATE, estStartDate);
					  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE, estFinishDate);

					  if(actStartDate != null && actStartDate.length() > 0)
					  {						
						 taskActualDataList.put(currentTaskId, taskActualDataMap);
						  taskActualDataMap.put("Summary", (String) nodeMap.get("Summary").toString());
						  taskActualDataMap.put("actualStartDate", actStartDate);
						  /*
						  if(actStartDate != "")
						  {
							  HashMap programMap = new HashMap();
							  HashMap paramMap = new HashMap();	
							  HashMap requestLocaleMap = new HashMap();	
							  String[] arrJPOArguments = new String[1];
								  
							  Locale locale = new Locale("en_US");
							  Date date1 = eMatrixDateFormat.getJavaDate(actStartDate);
							  int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
							  java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
							  actStartDate  = format.format(date1);
								  
							  programMap.put("New Value", actStartDate);
							  programMap.put("Old Value", null);
							  programMap.put("objectId", currentTaskId);	
							  paramMap.put("paramMap", programMap);
								  
							  requestLocaleMap.put("locale", locale);
							  paramMap.put("requestMap", requestLocaleMap);
								  
							  arrJPOArguments = JPO.packArgs(paramMap);
							  JPO.invoke(context,"emxTaskBase",null,"updateTaskActualStartDate",arrJPOArguments);
						  }*/
					  }
					
					  if(actFinishDate != null && actFinishDate.length() > 0)
					  {						
						  taskActualDataMap.put("actualFinishDate", actFinishDate);
						  /*
						  if(actFinishDate != "")
						  {
							  HashMap programMap = new HashMap();
							  HashMap requestLocaleMap = new HashMap();	
							  HashMap paramMap = new HashMap();	
							  String[] arrJPOArguments = new String[1];
								  
							  Locale locale = new Locale("en_US");
							  Date date1 = eMatrixDateFormat.getJavaDate(actFinishDate);
							  int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
							  java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
							  actFinishDate  = format.format(date1);
								  
							  programMap.put("New Value", actFinishDate);
							  programMap.put("Old Value", null);
							  programMap.put("objectId", currentTaskId);	
							  paramMap.put("paramMap", programMap);
							  	  
							  requestLocaleMap.put("locale", locale);
							  paramMap.put("requestMap", requestLocaleMap);
							  	  
							  arrJPOArguments = JPO.packArgs(paramMap);
							  JPO.invoke(context,"emxTaskBase",null,"updateTaskActualFinishDate",arrJPOArguments);
						  }			  
						  */	  
					  }				  
					
					  if(percentComplete != null && !percentComplete.equals("0"))
						  taskActualDataMap.put(ATTRIBUTE_PERCENT_COMPLETE, percentComplete);
						 // attributeMap.put(ATTRIBUTE_PERCENT_COMPLETE, percentComplete);

					  //if(estDuration != null && !estDuration.equals("0")) //To fix 91053 : Milestone is created with duration of 1 days in PMC when project is merged to PMC. Now PMC accepts the tasks with 0 (zero) duration.
					  if(estDuration != null && estDuration.length() > 0)
						  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_DURATION, estDuration);
					  //if(actDuration != null && !actDuration.equals("0")) //To fix 91053 : Milestone is created with duration of 1 days in PMC when project is merged to PMC. Now PMC accepts the tasks with 0 (zero) duration.
					  if(actDuration != null && actDuration.length() > 0)
						  attributeMap.put(ATTRIBUTE_TASK_ACTUAL_DURATION, actDuration);

					  if(constraintType!= null && constraintType.length() > 0)
						  attributeMap.put(ATTRIBUTE_TASK_CONSTRAINT_TYPE, constraintType);
					  
					  if(constraintDate!= null && constraintDate.length() > 0)
						  attributeMap.put(ATTRIBUTE_TASK_CONSTRAINT_DATE, constraintDate);
					  
					  //Doubtful
					  // setting the actual start and finish is controlled by the trigger JPO
					  // of the project on completion of the task/project. so not needed?
					  //busSelects.add(task.SELECT_TASK_ACTUAL_START_DATE);
					  //busSelects.add(task.SELECT_TASK_ACTUAL_FINISH_DATE);

					  //the first task node corresponds to the project
					  //only those specific to task added here
					  if(taskReq != null && !taskReq.equals(""))
					  {
						  attributeMap.put(task.ATTRIBUTE_TASK_REQUIREMENT, taskReq);
					  }
					  codeRegn = "setting task attribute values ";	   
					  task.setAttributeValues(context, attributeMap);					 
				  }			  
			  }//while (taskDataItr.hasNext())
			  if(task.exists(context))
				  task.rollupAndSave(context);

			  codeRegn = "adding dependency";
			  if(dependencyMapList != null)
			  //if (dependencyMapList.size() != 0)
			  {
				  if (debug)
					  System.out.println(codeRegn);
				  //create dependencies for the project created/updated
				  MapList pcDependencyMapList = convertDependency(dependencyMapList, taskIndexMap,null);
				  // commenting out the previous fix
				  //addDependency(context, pcDependencyMapList, null, taskIndexMap);
				  addDependency(context, pcDependencyMapList, null, taskIndexMap, false);
			  }

			  ArrayList taskObjectIds = new ArrayList(taskActualDataList.keySet());
			  for(int iActualDataTaskList = 0; iActualDataTaskList < taskObjectIds.size(); iActualDataTaskList++)
			  {		
				  String currentTaskId = taskObjectIds.get(iActualDataTaskList).toString();
				  actFinishDate = DomainConstants.EMPTY_STRING;
				  actStartDate = DomainConstants.EMPTY_STRING;
				  Map taskDataMap = (Map)taskActualDataList.get(currentTaskId);
				  task.setId(currentTaskId);
				  String summaryTask = taskDataMap.get("Summary").toString();
				  if("0".equals(summaryTask)){ //0 means leaf task
					  if(taskDataMap.get(ATTRIBUTE_PERCENT_COMPLETE) != null)
						  task.setAttributeValue(context, ATTRIBUTE_PERCENT_COMPLETE, taskDataMap.get(ATTRIBUTE_PERCENT_COMPLETE).toString());
					  if(taskDataMap.get("actualStartDate") != null)
						  actStartDate = taskDataMap.get("actualStartDate").toString();
					  if(taskDataMap.get("actualFinishDate") != null)
						  actFinishDate = taskDataMap.get("actualFinishDate").toString();
					  if(actStartDate != "")
					  {
						  HashMap programMap = new HashMap();
						  HashMap paramMap = new HashMap();	
						  HashMap requestLocaleMap = new HashMap();	
						  String[] arrJPOArguments = new String[1];

						  Locale locale = new Locale("en_US");
						  Date date1 = eMatrixDateFormat.getJavaDate(actStartDate);
						  int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
						  java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
						  actStartDate  = format.format(date1);

						  programMap.put("New Value", actStartDate);
						  programMap.put("Old Value", null);
						  programMap.put("objectId", currentTaskId);	
						  paramMap.put("paramMap", programMap);

						  requestLocaleMap.put("locale", locale);
						  paramMap.put("requestMap", requestLocaleMap);

						  arrJPOArguments = JPO.packArgs(paramMap);
						  JPO.invoke(context,"emxTaskBase",null,"updateTaskActualStartDate",arrJPOArguments);
					  }
					  if(actFinishDate != "")
					  {
						  HashMap programMap = new HashMap();
						  HashMap requestLocaleMap = new HashMap();	
						  HashMap paramMap = new HashMap();	
						  String[] arrJPOArguments = new String[1];

						  Locale locale = new Locale("en_US");
						  Date date1 = eMatrixDateFormat.getJavaDate(actFinishDate);
						  int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
						  java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
						  actFinishDate  = format.format(date1);

						  programMap.put("New Value", actFinishDate);
						  programMap.put("Old Value", null);
						  programMap.put("objectId", currentTaskId);	
						  paramMap.put("paramMap", programMap);

						  requestLocaleMap.put("locale", locale);
						  paramMap.put("requestMap", requestLocaleMap);

						  arrJPOArguments = JPO.packArgs(paramMap);
						  JPO.invoke(context,"emxTaskBase",null,"updateTaskActualFinishDate",arrJPOArguments);
					  }
				  }
			  }
			  codeRegn = "remove dependency";
			  //if(assigneeMapList != null)
			  if (assigneeMapList.size() != 0)
		 	  {
				  if (debug)
					  System.out.println("Adding Assignees");
				  //create assignees for project created/updated
				  //addAssignees(context, assigneeMapList, resourceIndexMap, taskIndexMap, null, null, null);
				  addAssignees(context, assigneeMapList, resourceIndexMap, taskIndexMap, null, null, null, taskUnitMap);
			  }
			  if (debug)
				  System.out.println("Adding Assignees...done");		  
		  }//if(!existingProject)
		  
		  //This else-block comes into picture when project is already present in PRG
		  else
		  {
			  //[added for sub project support]
			  String ctxPersonId = person.getPerson(context).getId();
			  person.setId(ctxPersonId);
			  String personName = person.getName(context);
			  String userVault = person.getVault();
			  //[/added for sub project support]
			  codeRegn = "Reading XML for Exisiting PC Project";
			  existingProject = true;
			  if (debug)
				  System.out.println(codeRegn);

			  Map oldTaskIndexMap = new HashMap();
			  MapList oldDependencyMapList = new MapList();
			  Map oldProjectMap = readXMLDataForMerge(context, elOldProjectRoot, existingProject,false,oldTaskIndexMap,oldDependencyMapList);
			  codeRegn = "Read XML for old PC Project";
			  if (debug)
				  System.out.println(codeRegn);

			  //Get the data of the project
			  //get project info, task info, resource map, assignments
			  Map oldPrjInfoMap          = (Map) oldProjectMap.get("projectData");
			  Map oldResourceIndexMap    = (Map) oldProjectMap.get("resourceIndexMap");
			  StringList oldResourceList = (StringList) oldProjectMap.get("resourceList");
			  MapList oldAssigneeMapList     = (MapList) oldProjectMap.get("assigneeMapList");
			  MapList oldTaskDataMapList = (MapList) oldProjectMap.get("taskData");

			  Map oldTaskUnitMap = (Map) oldProjectMap.get("taskUnitsDataMap");
	   	      HashMap NextOutlineMap = new HashMap();
			  HashMap TaskLevelMap = new HashMap();
			  HashMap TaskLevelMapOld = new HashMap();
			  //check if there is any modification in project name and description
			  //and update accordingly -
			  //case 1: only the MS Project user modified the project data
			  //case 2: only the PC user modified the project data
			  //case 3: the PC user as well as the MS Project user
			  //         modified the project data
			  //
			  //NOTE:
			  //for case 2 and case 3 do nothing PC is the master

			  //project-
			  // Define selectables for each Task object.
			  StringList busSelects = new StringList(11);
			  busSelects.add(SELECT_NAME);
			  busSelects.add(SELECT_DESCRIPTION);
			  busSelects.add(SELECT_ATTRIBUTE_PROJECT_SCHEDULE_FROM);	
			  busSelects.add(task.SELECT_TASK_CONSTRAINT_DATE);
			  
			  project.setId(strBusId);																														  
			  project.open(context);
			  Map curProjectMap = project.getInfo(context, busSelects);
			  String curProjectName = (String)curProjectMap.get(SELECT_NAME);
			  String curProjectDesc = (String)curProjectMap.get(SELECT_DESCRIPTION);
			  //[2011x Start: String variable to collect Project Schedule From Info from Current Project]
			  String curProjectScheduleFromStart=(String)curProjectMap.get(SELECT_ATTRIBUTE_PROJECT_SCHEDULE_FROM);			  
			  sProjectSchduleBasedOn = (String)prjInfoMap.get("ScheduleBasedOn");
			   //[2011x End: String variable to collect Project Schedule From Info from Current Project]
			  String curProjectDate = (String)curProjectMap.get(SELECT_TASK_CONSTRAINT_DATE);
			  Date date = mspDate.parse(curProjectDate);				
			  curProjectDate = MATRIX_DATE_FORMAT.format(date);
			  

			  //for case 2 and case 3 do nothing PC is the master
			  if(curProjectName.equals(oldPrjInfoMap.get("projectName")) &&
				!(oldPrjInfoMap.get("projectName")).equals(prjInfoMap.get("projectName")))
			  {
				  project.setName(context, (String) prjInfoMap.get("projectName"));				   
			  }
			  if(curProjectDesc.equals(oldPrjInfoMap.get("description")) &&
				!(oldPrjInfoMap.get("description")).equals(prjInfoMap.get("description")))
			  {
				  project.setDescription(context, (String) prjInfoMap.get("description"));			
			  }
			  if(prjInfoMap.get("ScheduleFrom").equals("Project Start Date"))
			  {
				  if(curProjectDate.equals(oldPrjInfoMap.get("ProjectStartDate")) &&
							!(oldPrjInfoMap.get("ProjectStartDate")).equals(prjInfoMap.get("ProjectStartDate")))
				  {
					  project.setAttributeValue(context, ATTRIBUTE_TASK_CONSTRAINT_DATE,(String) prjInfoMap.get("ProjectStartDate"));			
				  }
			  }
			  else if(prjInfoMap.get("ScheduleFrom").equals("Project Finish Date"))
			  {
				  if(curProjectDate.equals(oldPrjInfoMap.get("ProjectFinishDate")) &&
							!(oldPrjInfoMap.get("ProjectFinishDate")).equals(prjInfoMap.get("ProjectFinishDate")))
				  {
					  project.setAttributeValue(context, ATTRIBUTE_TASK_CONSTRAINT_DATE,(String) prjInfoMap.get("ProjectFinishDate"));			
				  }
			  }			  

			  //[2011x Start: Setting Project Schedule From Info and Project Constraint Type for already existing project]
			  if(curProjectScheduleFromStart.equals(oldPrjInfoMap.get("ScheduleFrom")) &&
						!(oldPrjInfoMap.get("ScheduleFrom")).equals(prjInfoMap.get("ScheduleFrom")))
			  {
		  		  Map projectAttributes = new HashMap();		
		  		  String projectScheduleFrom=(String) prjInfoMap.get("ScheduleFrom");
		  		  String projectConstraintType=null;
		  		  if(projectScheduleFrom.equals("Project Start Date"))
		  				projectConstraintType="As Soon As Possible";
		  		  else
		  				projectConstraintType="As Late As Possible";
		  			  
				  projectAttributes.put(ATTRIBUTE_PROJECT_SCHEDULE_FROM, projectScheduleFrom);
				  projectAttributes.put(ATTRIBUTE_DEFAULT_CONSTRAINT_TYPE, projectConstraintType);
	              project.setAttributeValues(context,projectAttributes);
			  }
			  project.setAttributeValue(context, ATTRIBUTE_SCHEDULEBASEDON, sProjectSchduleBasedOn);
              //[2011x End: Setting Project Schedule From Info and Project Constraint Type for already existing project ]

			  project.close(context);
			  //get the new data of the project
			  MapList newTaskDataMapList = (MapList) newProjectMap.get("taskData");
			  Map newPrjInfoMap          = (Map) newProjectMap.get("projectData");

			  // cn - check
			  MapList addedList    = new MapList();
			  MapList removedList  = new MapList();
			  MapList retainedList = new MapList();
			  MapList modifiedList = new MapList();		  

			  HashMap oldSubprojectMap =  new HashMap();
			  HashMap newSubprojectMap =  new HashMap();

			  //generate a new maplist for comparison of structure
			  //the new maplist only consists of taskId and level (outlineNumber)
			  //if there is no taskId then there is a new task added
			  //if there is change in level then the task has been moved
			  //to a different level
			  MapList oldStructMapList = new MapList();
			  MapList newStructMapList = new MapList();
			  HashMap oldDataMap = new HashMap();
			  HashMap newDataMap = new HashMap();
			  String parentOutline =ProgramCentralConstants.EMPTY_STRING;		
			  String parentId = ProgramCentralConstants.EMPTY_STRING;

			  HashMap OutlineIDMap = new HashMap();
			  HashMap OutlineIDMapOld = new HashMap();
			
			  if(oldTaskDataMapList.size() > 0)
			  {
				  ListIterator oldTaskDataMapListItr = oldTaskDataMapList.listIterator();
				  int i = 0;
				  while(oldTaskDataMapListItr.hasNext())
				  {
					  //skip the first value
					  //this corresponds to the project details
					  HashMap buildMap = new HashMap();
					  Map oldTaskMap = (Map) oldTaskDataMapListItr.next();
					  if(i != 0)
					  {
						  String id = (String) oldTaskMap.get("taskId");
						  String outlineNumber = (String) oldTaskMap.get("outlineNumber");
						  buildMap.put("taskId", id);
						  buildMap.put("outlineNumber", outlineNumber);

						  if(outlineNumber != null && !outlineNumber.equals("0"))
						  {
							  oldStructMapList.add(buildMap);
						  }

						  if(outlineNumber.indexOf(".") > -1)
						  {
							  parentOutline = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
							  parentId = (String)TaskLevelMapOld.get(parentOutline);
							  if(parentId !=null)
								  buildMap.put("parentTaskId", parentId);
						  }
						  //build an hashmap to locate the node details
						  //based on taskId as key
						  //if taskId does not exist then use outlineNumber as key
						  if(id != null || !id.equals(""))
						  {
							  oldDataMap.put(id, oldTaskMap);
							  TaskLevelMapOld.put(outlineNumber,id);
							  OutlineIDMapOld.put(id,outlineNumber);
							  Map subprojectmap =(Map)oldTaskMap.get("subprojectMap");
							  if(subprojectmap != null)
								  AllOldProjectIds.add(id);
						  }
						  else
					  	  {
							  oldDataMap.put(outlineNumber, oldTaskMap);
						  }
					 }
					 i++;
				  }//while(oldTaskDataMapListItr.hasNext())
			  }//if(oldTaskDataMapList.size() > 0)
		
			  String prevOutlineNumber = "aaaaa";
	
			  if(newTaskDataMapList.size() > 0)
			  {
				  ListIterator newTaskDataMapListItr = newTaskDataMapList.listIterator();
				  int i = 0;
				  while(newTaskDataMapListItr.hasNext())
				  {
					  HashMap buildMap = new HashMap();
					  Map newTaskMap = (Map) newTaskDataMapListItr.next();
					  if(i != 0) 
					  {
						  String id = (String) newTaskMap.get("taskId");
						  String outlineNumber = (String) newTaskMap.get("outlineNumber");
						  buildMap.put("taskId", id);
						  buildMap.put("outlineNumber", outlineNumber);
						  if(outlineNumber.indexOf(".") > -1)
						  {
							  parentOutline = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
							  parentId = (String)TaskLevelMap.get(parentOutline);
							  if(parentId !=null)
								  buildMap.put("parentTaskId", parentId);
						  }

						  NextOutlineMap.put(prevOutlineNumber,outlineNumber);
						  prevOutlineNumber = outlineNumber;

						  if(outlineNumber != null && !outlineNumber.equals("0")) 
						  {
							  newStructMapList.add(buildMap);
					      }
					      //build an hashmap to locate the node details based on taskId as key
						  //if taskId does not exist then use outlineNumber as key
					      if(id != null && !id.equals("")) 
						  {
							  newDataMap.put(id, newTaskMap);
							  TaskLevelMap.put(outlineNumber, id);
							  OutlineIDMap.put(id,outlineNumber);
						  } 
						  else
						  {
							  newDataMap.put(outlineNumber, newTaskMap);
						  }
					  }//if(i != 0) 
					  i++;
				  }//while(newTaskDataMapListItr.hasNext())			  
			  }//if(newTaskDataMapList.size() > 0)
			
			  if (oldStructMapList.size() > 0 || newStructMapList.size() > 0 )
			  {
				  // Get a list of all added items (new - old).
				  addedList.addAll(newStructMapList);
				  addedList.removeAll(oldStructMapList);

				  // Get a list of all removed items (old - new).
				  removedList.addAll(oldStructMapList);
				  removedList.removeAll(newStructMapList);

				  //store the removedList as a reference
				  //required while updating the attributes
				  MapList refRemovedList = new MapList();
				  refRemovedList.addAll(removedList);

				  // Get a list of all retained items (new & old).
				  retainedList.addAll(oldStructMapList);
				  retainedList.retainAll(newStructMapList);

				  // Get a list of tasks neither added or removed
				  // but could have been modified (old - removed).
				  // (no structure modification but only attribute modification)
				  modifiedList.addAll(newStructMapList);

				  // rpandia
				  tasks = (Map) new HashMap();

				  if(addedList != null)
				  //if (addedList.size() != 0)
				  {
					  ListIterator addedListItr = addedList.listIterator();

					  while (addedListItr.hasNext())
					  {
						  Map addedListMap = (Map) addedListItr.next();
						  String taskId = (String) addedListMap.get("taskId");
						  // cn - check
						  if(taskId != null && !taskId.equals(""))
						  {
							  //the removedList has entries of same taskId since the
							  //outlineLevelNumber get modified due to structure change
							  //if this task is handeled already then remove if from the
							  //removedList
							  Iterator removeItr = removedList.iterator();
							  int i = 0;
							  while(removeItr.hasNext())
							  {
								  Map removedMap = (Map) removeItr.next();
								  if(taskId.equals(removedMap.get("taskId")))
								  {
									  break;
								  }
								  i++;
							  }
							  if(i < removedList.size())
							  {
								  removedList.remove(i);
	  							  refRemovedList.remove(i);
							  }						 
						  }//if(taskId != null && !taskId.equals(""))
					  }//while (addedListItr.hasNext())
				  }//if(addedList != null)
				
				  // cn - check
				  if(addedList != null)
				  //if (addedList.size() != 0)
				  {
					  codeRegn = "Merge- Adding the task";

					  ListIterator addedListItr = addedList.listIterator();

					  while (addedListItr.hasNext())
					  {
						  if (debug)
							  iterationStartTime = System.currentTimeMillis();
						  
						  Map addedListMap = (Map) addedListItr.next();
						  String taskId = (String) addedListMap.get("taskId");
						  String outlineNumber = (String) addedListMap.get("outlineNumber");

						  Map oldTaskValueMap = new HashMap();
						  Map newTaskValueMap = new HashMap();
						  if((taskId != null) && !taskId.equals(""))
						  {
							  oldTaskValueMap = (Map) oldDataMap.get(taskId);
							  newTaskValueMap = (Map) newDataMap.get(taskId);
					 	  }
						  else
						  {
							  newTaskValueMap = (Map) newDataMap.get(outlineNumber);
						  }

						  String levelId = ProgramCentralConstants.EMPTY_STRING;
						  String newParentId = ProgramCentralConstants.EMPTY_STRING;

						  String taskUID = (String) newTaskValueMap.get("taskUID");
						  String taskName = (String) newTaskValueMap.get("taskName");
						  if (debug)
						  {
							  System.out.println("Processing task: " +taskName);
						  }
						  String taskDesc = (String) newTaskValueMap.get("description");
						  owner = (String) newTaskValueMap.get("owner");
						  String taskType = (String)newTaskValueMap.get("taskType");
						  String taskState = (String)newTaskValueMap.get("taskState");
						  String percentComplete  = (String) newTaskValueMap.get("percentComplete");
						  // BUG
						  estStartDate  = (String) newTaskValueMap.get("estStartDate");
						  estFinishDate  = (String) newTaskValueMap.get("estFinishDate");

						  String estDuration  = (String) newTaskValueMap.get("estDuration");

						  //[2011x Start: Getting value for ConstraintType and ConstraintDate, when a task is added or modified using sync window]
						  String constraintType=(String) newTaskValueMap.get("constraintType");
						  constraintDate=(String) newTaskValueMap.get("constraintDate");
						  //[2011x End: Getting value for ConstraintType and ConstraintDate, when a task is added or modified using sync window]
			  
						  actStartDate = (String) newTaskValueMap.get("actStartDate");
						  actFinishDate = (String) newTaskValueMap.get("actFinishDate");
			  
						  String outlineLevel = (String) newTaskValueMap.get("outlineLevel");
						  String taskReq = (String) newTaskValueMap.get("taskReq");

						  //if taskUID is not found this is a special case where
						  //the addedList contains details of project skip this value
						  if(taskUID == null || taskUID.equals(""))
							  continue;
						
						  // cn - check
						  if(taskId != null && !taskId.equals("") && oldTaskValueMap != null)
						  {
							  //taskLevelMap.put(outlineNumber, taskId);						
							  if (isProjectSpace(context,(String)oldTaskValueMap.get("taskType"))==true)
							  {
								  Map newSubprojectmap =(Map)newTaskValueMap.get("subprojectMap");
								  Map oldSubprojectmap =(Map)oldTaskValueMap.get("subprojectMap");

								  if (newSubprojectmap != null && oldSubprojectmap != null)
								  {
									  oldSubprojectmap.put("taskIndexMap", oldTaskIndexMap);
									  newSubprojectmap.put("taskIndexMap", taskIndexMap);
									  SynchronizeExistingProject(context, taskId,newSubprojectmap,oldSubprojectmap,tasks);
									  Map taskIndexMap2 = (Map)newSubprojectmap.get("taskIndexMap");
									  Map oldTaskIndexMap2 =(Map) oldSubprojectmap.get("taskIndexMap");
									  taskIndexMap.putAll(taskIndexMap2);
									  oldTaskIndexMap.putAll(oldTaskIndexMap2);
								  }
								  //get the releationship id
								  subProject.setId(taskId);
								  StringList busSelect = new StringList();
								  busSelect.add("to[" + RELATIONSHIP_SUBTASK + "].id");
								  Map taskInfo = subProject.getInfo(context, busSelect);
								  String connectionId = (String) taskInfo.get("to[" + RELATIONSHIP_SUBTASK + "].id");
								  DomainRelationship.disconnect(context, connectionId);

								  if(! outlineNumber.equals(oldTaskValueMap.get("outlineNumber")))
								  {
									  //structure modified for an existing subproject
									  if(outlineNumber.lastIndexOf(".") == -1)
									  {
										  newParentId = strBusId;
									  }
									  else
									  {
										  levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
										  newParentId = (String) taskLevelMap.get(levelId);
									  }

									  task.setId(strBusId);	
									  task.reSequence(context, strBusId);
									
									  task.setId(newParentId);		
									  String []childIds = new String[1];
									  childIds[0] = taskId;
									  String nextTaskId = getNextTaskId( outlineNumber,NextOutlineMap, TaskLevelMap,TaskLevelMapOld, OutlineIDMap, OutlineIDMapOld);
									  if (nextTaskId == null)
									  {
										  nextTaskId = newParentId;
									  }
									  task.addExisting(context, childIds , nextTaskId);
								  }//if(! outlineNumber.equals(oldTaskValueMap.get("outlineNumber")))
								  ProjectIds.add(taskId);
							  }//if (isProjectSpace(context,(String)oldTaskValueMap.get("taskType"))==true)
							  else
						  	  {
								  //get the releationship id
								  task.setId(taskId);
								  StringList busSelect = new StringList();
								  busSelect.add("to[" + RELATIONSHIP_SUBTASK + "].id");			 
								  Map taskInfo = task.getInfo(context, busSelect);																	  
								  String connectionId = (String) taskInfo.get("to[" + RELATIONSHIP_SUBTASK + "].id");		 
								  DomainRelationship.disconnect(context, connectionId);				
	
								  //Circular dependency : case 1
								  //before modifying the structure of the current task check if
								  //currenttask has existing predecessors which on structure change
								  //will result in being the current tasks parent
								  //since a parent cannot have its childern has a predecessor
								  //i.e taskId cannot not be a predecessor of newParentId			  
								  MapList predList = task.getPredecessors(context, null, null, null);			 
								  Iterator predListItr = predList.iterator();
	
								  if (debug)
									  System.out.println("Checking for Circular Dependency: case 1");
	
								  while (predListItr.hasNext())
								  {
									  Map predecessorObj = (Map) predListItr.next();
									  String predecessorId = (String) predecessorObj.get(task.SELECT_ID);
									  ArrayList taskLevelKeys = new ArrayList(taskLevelMap.keySet());
									  //BUG? []: changed from predecessorObj.keySet() to taskLevelMap.keySet()
									  ArrayList taskLevelValues = new ArrayList(taskLevelMap.values());
									  
									  String predLevel = null;
									  if(taskLevelValues.indexOf(predecessorId) != -1)
									  {
										  int predPos = taskLevelValues.indexOf(predecessorId);
										  if(taskLevelKeys.size() > predPos)
											  //BUG: []: changed from (taskLevelKeys.size() >= predPos) to (taskLevelKeys.size() > predPos)(zero based index)?
										  {
											  predLevel = (String) taskLevelKeys.get(predPos);
											  if (predLevel != null &&  outlineNumber.indexOf(predLevel) != -1)
											  {
												  //removing dependency of a given object
												  String predConnectionId = (String) predecessorObj.get(dependency.SELECT_ID);		  
												  task.removePredecessor(context, predConnectionId);							 
											  }
										  }
									  } // end if
								  } // while (predListItr.hasNext())
	
								  //if(! outlineNumber.equals(oldTaskValueMap.get("outlineNumber")))
								  {
									  //structure modified for an existing task

									  if(outlineNumber.lastIndexOf(".") == -1)
									  {
										  //direct subtask of a project
										  newParentId = strBusId;
									  }
									  else
									  {
										  levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
										  newParentId = (String) taskLevelMap.get(levelId);

										  //Circular dependency : case 2
										  //before changing the parent of the current task check if
										  //currenttask is a predecessor of the parent task
										  //since a parent cannot have its childern has a predecessor
										  //i.e taskId cannot not be a predecessor of newParentId
										  task.setId(newParentId);
										  if(debug)
											  System.out.println("Checking for Circular Dependency: case 2");

										  MapList predecessorList = task.getPredecessors(context, null, null, null);			
										  Iterator predecessorItr = predecessorList.iterator();

										  while (predecessorItr.hasNext())
										  {
											  Map predecessorObj = (Map) predecessorItr.next();
											  String predecessorId = (String) predecessorObj.get(task.SELECT_ID);
											  if (predecessorId.equals(taskId))
											  {
												  String depConnectionId = (String) predecessorObj.get(dependency.SELECT_ID);	 
												  task.removePredecessor(context, depConnectionId);					   
											  }
										  }
										  task.setId(taskId);
									  }
												  
								      task.setId(strBusId);	
								  	  task.reSequence(context, strBusId);

									  task.setId(newParentId);		
									  String []childIds = new String[1];
									  childIds[0] = taskId;
									  String nextTaskId = getNextTaskId( outlineNumber,NextOutlineMap, TaskLevelMap,TaskLevelMapOld,OutlineIDMap, OutlineIDMapOld);
									  if (nextTaskId == null)
									  {
										  nextTaskId = newParentId;
									  }
									  task.addExisting(context, childIds , nextTaskId);
									  TaskLevelMap.put(outlineNumber,taskId);
								  }//if(! outlineNumber.equals(oldTaskValueMap.get("outlineNumber")))	
							 }
						  }//if(taskId != null && !taskId.equals("")&& oldTaskValueMap != null)
						  else
					 	  {
							  Map subprojectmap =(Map)newTaskValueMap.get("subprojectMap");
							  if (subprojectmap == null && isProjectSpace(context,taskType)==true)
							  {
								  mspiCreateProject(context,  subProject,  taskType,  taskName,  userVault) ;
								  String subprojectId = subProject.getId();
								  task.setId(subprojectId);
								  //used for obtaining a tasks parent given a outlineNumber of the child
								  taskLevelMap.put(outlineNumber, subprojectId);
								  //used for building resources and dependencies
								  //provides the PC taksID based on a MS Project task UID
								  taskIndexMap.put(taskUID, subprojectId);
								  subProject.setDescription(context, taskDesc);
								  codeRegn = "set the attribute values into a map";
								  Map attributeMap = new HashMap(5);
								  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_START_DATE, estStartDate);
								  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE, estFinishDate);
								  if(percentComplete != null && !percentComplete.equals("0"))
									  attributeMap.put(ATTRIBUTE_PERCENT_COMPLETE, percentComplete);
								  if(estDuration != null && estDuration.length() > 0)
									  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_DURATION, estDuration);
								 
								  attributeMap.put(ATTRIBUTE_DEFAULT_CONSTRAINT_TYPE, constraintType);
								  
								  //if(actDuration != null && actDuration.length() > 0)
								  //attributeMap.put(ATTRIBUTE_TASK_ACTUAL_DURATION, actDuration);
								  codeRegn = "setting project attribute values ";
								  subProject.setAttributeValues(context, attributeMap);		
								
								  //String parentId;
								  if(outlineNumber.lastIndexOf(".") == -1)
								  {
									  parentId = strBusId;
								  }
								  else
								  {
									  levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
									  parentId = (String) taskLevelMap.get(levelId);
									  parentTask.setId(parentId);
								  }

								  task.setId(parentId);		
								  String []childIds = new String[1];
								  childIds[0] = subprojectId;
								  String nextTaskId = getNextTaskId( outlineNumber,NextOutlineMap, TaskLevelMap,TaskLevelMap,OutlineIDMap, OutlineIDMapOld);
								  if (nextTaskId == null)
								  {
									  nextTaskId = parentId;
								  }
								  task.addExisting(context, childIds , nextTaskId);

							  	  ProjectIds.add(subprojectId);								
							  }//if (isProjectSpace(context,taskType)==true)
							  else
							  {
								  if (subprojectmap != null )
								  {
									  String subprojectId = SynchronizeNewProject(context, subprojectmap);
									  Map  subprojectTaskIndexMap = (Map) subprojectmap.get("taskIndexMap");
									  taskIndexMap.putAll(subprojectTaskIndexMap);
	
									  //used for obtaining a tasks parent given a outlineNumber of the child
									  taskLevelMap.put(outlineNumber, subprojectId);
									  //used for building resources and dependencies
									  //provides the PC taksID based on a MS Project task UID
									  taskIndexMap.put(taskUID, subprojectId);

									  //String parentId;
									  if(outlineNumber.lastIndexOf(".") == -1)
									  {
										  parentId = strBusId;
									  }
									  else
									  {
										  levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
										  parentId = (String) taskLevelMap.get(levelId);
									  }		

									  String nextTaskId = getNextTaskId( outlineNumber,NextOutlineMap, TaskLevelMap,TaskLevelMapOld,OutlineIDMap, OutlineIDMapOld);
									  task.setId(parentId);		
									  String []childIds = new String[1];
									  childIds[0] = subprojectId;
									  task.addExisting(context, childIds , nextTaskId);
						
									  ProjectIds.add(subprojectId);
								  }
								  else
							 	  {
									  task = (Task) DomainObject.newInstance(m_ctxUserContext, DomainConstants.TYPE_TASK, DomainConstants.PROGRAM);//IR-185394V6R2013x
									  String nextTaskId= getNextTaskId( outlineNumber,NextOutlineMap, TaskLevelMap,TaskLevelMapOld,OutlineIDMap, OutlineIDMapOld);;
																					
									  String taskParentId = null;
									  //during merge newly added tasks
									  codeRegn = "setting strBusId " + strBusId;
									  project.setId(strBusId);
									  codeRegn = "project set ";

									  if(outlineNumber.lastIndexOf(".") == -1)
									  {
										  //if outlineNumber == "-1" then these are direct childern of project
										  //String taskPolicy =  task.getDefaultPolicy(context,DomainConstants.TYPE_TASK);
										  codeRegn = "creating task under project ";
										  taskParentId = strBusId;
										  if (debug)
										  {
											  ifTaskCreate = System.currentTimeMillis();
										  }
										  //task.create(context, DomainConstants.TYPE_TASK, taskName, taskPolicy, project);
										  boolean retValue = mspiCreateTask(context, task, taskType, taskName, project, true,nextTaskId);
										  if (debug)
										  {
											  ifTaskCreated = System.currentTimeMillis();
											  timeToCreate = (float)ifTaskCreated - ifTaskCreate/1000;
										  }
									  }
									  else
									  {
										  levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
										  parentId = (String) taskLevelMap.get(levelId);
										  parentTask.setId(parentId);
										  //String taskPolicy =  task.getDefaultPolicy(context,DomainConstants.TYPE_TASK);
										  codeRegn = "creating hierarchial task " + strBusId;
										  task.clear();
										  taskParentId = parentId;
										  if (debug)
											  System.out.println("Task creation started - else");
										  //task.create(context, DomainConstants.TYPE_TASK, taskName, taskPolicy, parentTask);
										  if(nextTaskId == null)
											  nextTaskId = taskParentId;
										  boolean retValue = mspiCreateTask(context, task, taskType, taskName, parentTask, false,nextTaskId);
										  if (debug)
											  System.out.println("Task creation completed");
									  }
									  task.setDescription(context, taskDesc);
									  if(taskState != null && !taskState.equals(""))
										  task.setState(context, taskState);
									  String newTaskId = task.getId();
									  taskLevelMap.put(outlineNumber, newTaskId);
									  taskIndexMap.put(taskUID, newTaskId);

									  //newattrib-
									  //set the atttibutes of newly created tasks
									  //codeRegn = "set the attribute values into a map";
									  Map attributeMap = new HashMap(5);

									  ///////////////////////////////////10.8.0.2.LA /////////////////////////////////////////////
                                      //Date tempStartDate = mspDate.parse(estStartDate);
                                      Date tempStartDate = MATRIX_DATE_FORMAT.parse(estStartDate);                
									  task.updateStartDate(context, tempStartDate, true);

                                      ///////////////////////////////////10.8.0.2.LA /////////////////////////////////////////////          									
                                      //Date tempFinishDate = mspDate.parse(estFinishDate);
                                      Date tempFinishDate = MATRIX_DATE_FORMAT.parse(estFinishDate);        
									  task.updateFinishDate(context, tempFinishDate, true);

									  if(actStartDate != null && actStartDate.length() > 0)
									  {						
										  if(actStartDate != "")
										  {
											  HashMap programMap = new HashMap();
											  HashMap paramMap = new HashMap();	
											  HashMap requestLocaleMap = new HashMap();
											  String[] arrJPOArguments = new String[1];
												  
											  Locale locale = new Locale("en_US");
											  Date date1 = eMatrixDateFormat.getJavaDate(actStartDate);
											  int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
											  java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
											  actStartDate  = format.format(date1);
												  
											  programMap.put("New Value", actStartDate);
											  programMap.put("Old Value", null);
											  programMap.put("objectId", newTaskId);	
											  paramMap.put("paramMap", programMap);
												  
											  requestLocaleMap.put("locale", locale);
											  paramMap.put("requestMap", requestLocaleMap);
												  
											  arrJPOArguments = JPO.packArgs(paramMap);
											  JPO.invoke(context,"emxTaskBase",null,"updateTaskActualStartDate",arrJPOArguments);
										  }
										  else
										  {
											  task.setAttributeValue(context, task.ATTRIBUTE_TASK_ACTUAL_START_DATE, "");
										  }
									  }
									
									  if(actFinishDate != null && actFinishDate.length() > 0)
									  {						
										  if(actFinishDate != "")
										  {
											  HashMap programMap = new HashMap();
											  HashMap paramMap = new HashMap();	
											  HashMap requestLocaleMap = new HashMap();
											  String[] arrJPOArguments = new String[1];
												  
											  Locale locale = new Locale("en_US");
											  Date date1 = eMatrixDateFormat.getJavaDate(actFinishDate);
											  int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
											  java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
											  actFinishDate  = format.format(date1);
												  
											  programMap.put("New Value", actFinishDate);
											  programMap.put("Old Value", null);
											  programMap.put("objectId", newTaskId);	
											  paramMap.put("paramMap", programMap);
												  
											  requestLocaleMap.put("locale", locale);
											  paramMap.put("requestMap", requestLocaleMap);
												  
											  arrJPOArguments = JPO.packArgs(paramMap);
											  JPO.invoke(context,"emxTaskBase",null,"updateTaskActualFinishDate",arrJPOArguments);
										  }	
										  else
										  {
											  task.setAttributeValue(context, task.ATTRIBUTE_TASK_ACTUAL_FINISH_DATE, "");
										  }										  
									  }	

									  //[2011x Start: Saving ConstraintType and ConstraintDate in PMC of a newly added or modified task through sync window]
									  attributeMap.put(ATTRIBUTE_TASK_CONSTRAINT_TYPE, constraintType);
									  									  
									  if(constraintDate!= null && constraintDate.length() > 0)
										  attributeMap.put(ATTRIBUTE_TASK_CONSTRAINT_DATE, constraintDate);	
									  else
										  attributeMap.put(ATTRIBUTE_TASK_CONSTRAINT_DATE, null);	
									  //[2011x End: Saving ConstraintType and ConstraintDate in PMC of a newly added or modified task through sync window]
									  
									  if(percentComplete != null && !percentComplete.equals("0"))
										  attributeMap.put(ATTRIBUTE_PERCENT_COMPLETE, percentComplete);
									  //if(estDuration != null && !estDuration.equals("0"))
									  if(estDuration != null && estDuration.length() > 0)
									  {
										  //attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_DURATION, estDuration);
										  //fraction data support
										  //Integer tempDuration = new Integer(Double.valueOf(estDuration).intValue());
										  //task.updateDuration(context, tempDuration, true);
										  task.updateDuration(context, estDuration);
									  }

									  //if(actDuration != null && !actDuration.equals("0"))
									  //attributeMap.put(ATTRIBUTE_TASK_ACTUAL_DURATION, actDuration);
									  if(taskReq != null && !taskReq.equals(""))
										  attributeMap.put(task.ATTRIBUTE_TASK_REQUIREMENT, taskReq);

									  codeRegn = "setting task attribute ";
									  task.setAttributeValues(context, attributeMap);

									  task.setOwner(context, owner);
								  }
							  } // end else
						  }
						  if (debug)
						  {
							  iterationEndTime = System.currentTimeMillis();
							  long iterationTime = iterationEndTime - iterationStartTime;

							  //System.out.println("TOTAL TIME in Iteration for new tasks (in Secs): "+(float)iterationTime/1000);
						  }
					  }//while (addedListItr.hasNext())
				  } // if(addedList != null)

				  // rpandia - what is this for ? What's being deleted here
				  // This is getting executed every time, it should get executed
				  // only if a row of data is deleted from MSP
				  // Changing the API from "removedList != null" to "removedList.size() != 0"
				  // which takes care of the condition

				  // cn - check
				  if(removedList.size() != 0)
				  //if(removedList != null)
				  {
					  codeRegn = "Merge - Deleting the task";
					  if (debug)
						  System.out.println(codeRegn);
					  
					  deleteTasks(context, removedList, taskLevelMap, strBusId);
					  if (debug)
						  System.out.println("Done deleting tasks");
				  }
				  else
				  {
					  if (debug)
						  System.out.println("Nothing to Delete");
				  }

				  if (debug)
					  afterDeleteStartTime = System.currentTimeMillis();
				
			      codeRegn = "Merge - Modify the task attributes";
				  ListIterator taskDataMapListItr = taskDataMapList.listIterator();
				  Map oldMap = new HashMap();
				  Map mspMap = new HashMap();

				  while (taskDataMapListItr.hasNext())
				  {
					  mspMap = (Map) taskDataMapListItr.next();
				  }

				  int taskCounter = taskDataMapList.size();

				  while (taskDataMapListItr.hasPrevious())
				  {
					  mspMap = (Map) taskDataMapListItr.previous();
					  if(taskCounter == 1)
				 	  {
						  continue;
					  }
					  taskCounter--;
					  String mspTaskId = (String) mspMap.get("taskId");
					  if(mspTaskId != null && !mspTaskId.equals(""))
					  {
						  ListIterator oldTaskDataMapListItr = oldTaskDataMapList.listIterator();
						  while (oldTaskDataMapListItr.hasNext())
						  {
							  oldMap = (Map) oldTaskDataMapListItr.next();
							  String oldTaskId = (String) oldMap.get("taskId");
							  if(oldTaskId != null && oldTaskId.equals(mspTaskId))
							  {
								  break;
							  }
						  }
						  //need to check whether this shud be there for sub project also
						  if (ProjectIds.indexOf(mspTaskId) >= 0 )
						  {
							  continue;
						  }
						  Map newSubprojectmap =(Map)mspMap.get("subprojectMap");
						  Map oldSubprojectmap =(Map)oldMap.get("subprojectMap");

						  if (newSubprojectmap != null && oldSubprojectmap != null)
						  {
							  oldSubprojectmap.put("taskIndexMap", oldTaskIndexMap);
							  newSubprojectmap.put("taskIndexMap", taskIndexMap);
							  SynchronizeExistingProject(context, mspTaskId,newSubprojectmap,oldSubprojectmap,tasks);
							  Map taskIndexMap2 = (Map)newSubprojectmap.get("taskIndexMap");
							  Map oldTaskIndexMap2 =(Map) oldSubprojectmap.get("taskIndexMap");
							  taskIndexMap.putAll(taskIndexMap2);
							  oldTaskIndexMap.putAll(oldTaskIndexMap2);
							  continue;
						  }
					  }//if(mspTaskId != null && !mspTaskId.equals(""))
				  }//while (taskDataMapListItr.hasPrevious())
				  MapList addedDependencyList    = new MapList();
				  MapList removedDependencyList  = new MapList();
				  MapList retainedDependencyList = new MapList();

				  StringList SFDependentTaskList = new StringList();
				  //update dependencies
				  if (oldDependencyMapList.size() > 0 || dependencyMapList.size() > 0)
				  {
					  if (debug)
						  System.out.println("update dependencies");
					  //the values contained in the maplist is using uid from MS Project
					  //convert it to ids of PC and compare the two maplist
					  MapList pcDependencyMapList = convertDependency(dependencyMapList, taskIndexMap,SFDependentTaskList);
					  MapList pcOldDependencyMapList = convertDependency(oldDependencyMapList, oldTaskIndexMap,null);
					  //ready for comparison
					  // Get a list of all added items (new - old).
					  addedDependencyList.addAll(pcDependencyMapList);
					  addedDependencyList.removeAll(pcOldDependencyMapList);
					  // Get a list of all removed items (old - new).
					  removedDependencyList.addAll(pcOldDependencyMapList);
					  removedDependencyList.removeAll(pcDependencyMapList);
					  // Get a list of all retained items (new & old).
					  retainedDependencyList.addAll(pcOldDependencyMapList);
					  retainedDependencyList.retainAll(pcDependencyMapList);
					  MapList updatedRemoveDependencyList = new MapList();
					  if(addedDependencyList != null)
					  {
						  //newly added or modified dependency
						  //updatedRemoveDependencyList = addDependency(context, addedDependencyList, removedDependencyList, taskIndexMap);
						  updatedRemoveDependencyList = addDependency(context, addedDependencyList, removedDependencyList, taskIndexMap, true);
						  if (debug)
							  System.out.println("back from addDependency()");
					  }
					  //if(updatedRemoveDependencyList != null)
					  if (updatedRemoveDependencyList.size() != 0)
					  {
						  //removed dependency
						  removeDependency(context, updatedRemoveDependencyList, taskIndexMap);
						  if (debug)
							  System.out.println("back from removeDependency()");
					  }
				  }//if (oldDependencyMapList.size() > 0 || dependencyMapList.size() > 0)
				
				  //check if the task attributes are modified and set them
				  // cn - check

				  codeRegn = "Merge - Modify the task attributes";

				  //get to the end of the map list
				  //the maplist is top-down wrt to the project tasks
				  //when we try to save the attributes of a task if we set the
				  //parents percentage as 100% PC's trigger will block the event
				  //(since the child is still 0%) so go to the end of the list and
				  //start the attribute setting from the child then going updwards
				  taskDataMapListItr = taskDataMapList.listIterator();
				  while (taskDataMapListItr.hasNext())
				  {
					  mspMap = (Map) taskDataMapListItr.next();
				  }

				  taskCounter = taskDataMapList.size();
				  while (taskDataMapListItr.hasPrevious())
				  {
					  mspMap = (Map) taskDataMapListItr.previous();
					  if(taskCounter == 1)
					  {
						  continue;
					  }
					  taskCounter--;
					  String mspTaskId = (String) mspMap.get("taskId");
					  Iterator refRemovedItr = refRemovedList.iterator();
					  int i = 0;
					  boolean nodeInRemovedList = false;
					  while(refRemovedItr.hasNext())
					  {
						  Map refRemovedMap = (Map) refRemovedItr.next();
						  if(mspTaskId.equals(refRemovedMap.get("taskId")))
						  {
							  nodeInRemovedList = true;
							  break;
						  }
						  i++;
					  }
					  if(nodeInRemovedList)
					  {
						  continue;
					  }

					  if(mspTaskId != null && !mspTaskId.equals(""))
					  {
						  ListIterator oldTaskDataMapListItr = oldTaskDataMapList.listIterator();
						  while (oldTaskDataMapListItr.hasNext())
						  {
							  oldMap = (Map) oldTaskDataMapListItr.next();
							  String oldTaskId = (String) oldMap.get("taskId");
							  if(oldTaskId != null && oldTaskId.equals(mspTaskId))
							  {
								  break;
							  }
						  }

						  //need to check whether this shud be there for sub project also
						  if (ProjectIds.indexOf(mspTaskId) >= 0 )
						  {
							  continue;
						  }
						  //task data when the project was downloaded
						  String oldTaskName = (String)oldMap.get("taskName");
						  String oldTaskDesc = (String)oldMap.get("description");
						  String oldTaskOwner = (String)oldMap.get("owner");
						  String oldTaskType = (String)oldMap.get("taskType");
						  String oldTaskCurrent = (String)oldMap.get("taskState");
						  String oldTaskStart = (String)oldMap.get("estStartDate");
						  String oldTaskFinish = (String)oldMap.get("estFinishDate");
						  String oldActTaskStart = "";
						  String oldActTaskFinish = "";
						  String mspActTaskStart = "";
						  String mspActTaskFinish = "";
						  boolean bActTaskStartChanged = false;
						  boolean bActTaskFinishChanged = false;
						  if(sProjectSchduleBasedOn.equals("Actual"))
						  {
							  if(oldMap.get("actStartDate") != null)
								  oldActTaskStart = (String)oldMap.get("actStartDate");
							  if(oldMap.get("actFinishDate") != null)
							  oldActTaskFinish = (String)oldMap.get("actFinishDate");
						  }
						  String oldTaskDuration = (String)oldMap.get("estDuration");
						  String oldTaskPercent = (String)oldMap.get("percentComplete");
						  String oldTaskRequirement = (String)oldMap.get("taskReq");
						  //[2011x Start: Collect ConstraintType and ConstraintDate from oldMap in String holder]
						  String oldTaskConstraintType=(String) oldMap.get("constraintType");
						  String oldTaskConstraintDate=(String) oldMap.get("constraintDate");
						  //[2011x End: Collect ConstraintType and ConstraintDate from oldMap in String holder]

						  //task data, as obtained from MS Project
						  String mspTaskName = (String)mspMap.get("taskName");
						  String mspTaskDesc = (String)mspMap.get("description");
						  String mspTaskOwner = (String)mspMap.get("owner");
						  String mspTaskType = (String)mspMap.get("taskType");
						  String mspTaskCurrent = (String)mspMap.get("taskState");
						  String mspTaskPercent = (String)mspMap.get("percentComplete");
						  String mspTaskStart = (String)mspMap.get("estStartDate");
						  String mspTaskFinish = (String)mspMap.get("estFinishDate");
						  if(sProjectSchduleBasedOn.equals("Actual"))
						  {
							  if(mspMap.get("actStartDate").toString().length() > 0)
								  mspActTaskStart = (String)mspMap.get("actStartDate");
							  if(mspMap.get("actFinishDate").toString().length() > 0)
								  mspActTaskFinish = (String)mspMap.get("actFinishDate");
						  }
						  String mspTaskDuration = (String)mspMap.get("estDuration");
						  String mspTaskRequirement = (String)mspMap.get("taskReq");
						  //[2011x Start: Collect ConstraintType and ConstraintDate from mspMap in String holder]
						  String mspTaskConstraintType=(String)mspMap.get("constraintType");
						  String mspTaskConstraintDate=(String)mspMap.get("constraintDate");
						  //[2011x End: Collect ConstraintType and ConstraintDate from mspMap in String holder]

						  /*  IR 12357 Begin
						  boolean bTaskNameChanged = !(oldTaskName.equals(mspTaskName));
						  boolean bTaskDescChanged =  !(oldTaskDesc.equals(mspTaskDesc));
						  boolean bTaskDescChanged =  !(oldTaskDesc.equals(oldTaskDesc));
						  boolean bTaskOwnerChanged = !(oldTaskOwner.equals(mspTaskOwner));
						  boolean bTaskStateChanged =  !(oldTaskCurrent.equals(mspTaskCurrent));
						  boolean bTaskPercentChanged = !(oldTaskPercent.equals(mspTaskPercent));
						  boolean bTaskStartChanged = !(oldTaskStart.equals(mspTaskStart));*/
						  
						  boolean bTaskNameChanged = false;
						  if(oldTaskName != null)
							  bTaskNameChanged = !(oldTaskName.equals(mspTaskName));
						  
						  boolean bTaskDescChanged =  false;
						  if(oldTaskDesc != null)
							  bTaskDescChanged = !(oldTaskDesc.equals(oldTaskDesc));
						  
						  boolean bTaskOwnerChanged = false;
						  if(oldTaskOwner != null)
							  bTaskOwnerChanged =!(oldTaskOwner.equals(mspTaskOwner));
						  
						  boolean bTaskStateChanged = false;
						  if(oldTaskCurrent != null)
							  bTaskStateChanged =!(oldTaskCurrent.equals(mspTaskCurrent));
						  
						  boolean bTaskPercentChanged = false;
						  if(oldTaskPercent != null)
							  bTaskPercentChanged =!(oldTaskPercent.equals(mspTaskPercent));
						  
						  boolean bTaskStartChanged = false;
						  if(oldTaskStart != null)
							  bTaskStartChanged =!(oldTaskStart.equals(mspTaskStart));
						  
						  if(sProjectSchduleBasedOn.equals("Actual"))
						  {
							  if(oldActTaskStart != null)
								  bActTaskStartChanged = !(oldActTaskStart.equals(mspActTaskStart));
							  if(oldActTaskFinish != null)
								  bActTaskFinishChanged = !(oldActTaskFinish.equals(mspActTaskFinish));
						  }
						  
						  boolean bTaskReqChanged = false;
						  if(oldTaskRequirement != null)
							  bTaskReqChanged= !(oldTaskRequirement.equals(mspTaskRequirement));
						  						  
						  boolean bTaskDurationChanged = false;
						  if(oldTaskDuration != null)
							  bTaskDurationChanged = !(oldTaskDuration.equals(mspTaskDuration));
						  
						  //[2011x Start: Compare ConstraintType and ConstraintDate Values from oldMap and mspMap]
						  boolean bTaskConstraintTypeChanged= false;
						  if(oldTaskConstraintType != null)
							  bTaskConstraintTypeChanged= !(oldTaskConstraintType.equals(mspTaskConstraintType));
						  
						  boolean bTaskConstraintDateChanged= false;
						  if(oldTaskConstraintDate != null)
							  bTaskConstraintDateChanged = !(oldTaskConstraintDate.equals(mspTaskConstraintDate));
						  //[2011x End: Compare ConstraintType and ConstraintDate Values from oldMap and mspMap]
						  //IR 12357 End
						  
					 	  if(bTaskNameChanged || bTaskDescChanged || bTaskOwnerChanged ||
							  bTaskStateChanged ||	bTaskPercentChanged || bTaskStartChanged ||
	 						  bTaskDurationChanged || bTaskReqChanged || bTaskConstraintTypeChanged || bTaskConstraintDateChanged||
		 						 bActTaskStartChanged || bActTaskFinishChanged)
						  {
						  	  StringList taskSelects = new StringList(10);
							  taskSelects.add(task.SELECT_NAME);
							  taskSelects.add(task.SELECT_OWNER);
							  taskSelects.add(task.SELECT_TYPE);
							  taskSelects.add(task.SELECT_DESCRIPTION);
							  taskSelects.add(task.SELECT_CURRENT);
							  taskSelects.add(task.SELECT_PERCENT_COMPLETE);
							  taskSelects.add(task.SELECT_TASK_ESTIMATED_DURATION);
							  taskSelects.add(task.SELECT_TASK_ESTIMATED_START_DATE);
							  taskSelects.add(task.SELECT_TASK_ESTIMATED_FINISH_DATE);
							  taskSelects.add(task.SELECT_TASK_ACTUAL_START_DATE);
							  taskSelects.add(task.SELECT_TASK_ACTUAL_FINISH_DATE);							  
							  taskSelects.add(task.SELECT_TASK_REQUIREMENT);
							  taskSelects.add(task.SELECT_HAS_SUBTASK);

							  //[2011x Start: Adding Task ConstraintType and ConstraintDate Info From Project(saved in PRG) into StringList taskSelects]
							  taskSelects.add(task.SELECT_TASK_CONSTRAINT_TYPE);
							  taskSelects.add(task.SELECT_TASK_CONSTRAINT_DATE);
							  //[2011x End: Adding Task ConstraintType and ConstraintDate Info From Project(saved in PRG) into StringList taskSelects]
							  
							  //this task might have been deleted
							  //check if this task exists if it exists then remove the assignee
							  //if(taskIndexMap.containsValue(mspTaskId)) {
							  task.setId(mspTaskId);
							  task.open(context);
							  //get current task data from PC

							  Map pcTaskMap = task.getInfo(context, taskSelects);  //WHAT IF THIS IS A PROJECT....?
							  String pcTaskName = (String)pcTaskMap.get(task.SELECT_NAME);
							  String pcTaskDesc = (String)pcTaskMap.get(task.SELECT_DESCRIPTION);
							  String pcTaskOwner = (String)pcTaskMap.get(task.SELECT_OWNER);
							  String pcTaskType = (String)pcTaskMap.get(task.SELECT_TYPE);
							  String pcTaskCurrent = (String)pcTaskMap.get(task.SELECT_CURRENT);
							  String pcTaskPercent = (String)pcTaskMap.get(task.SELECT_PERCENT_COMPLETE);
							  String pcTaskStart = (String)pcTaskMap.get(task.SELECT_TASK_ESTIMATED_START_DATE);
							  String pcTaskFinish = (String)pcTaskMap.get(task.SELECT_TASK_ESTIMATED_FINISH_DATE);
							  String pcActTaskStart = (String)pcTaskMap.get(task.SELECT_TASK_ACTUAL_START_DATE);
							  String pcActTaskFinish = (String)pcTaskMap.get(task.SELECT_TASK_ACTUAL_FINISH_DATE);
							  String pcTaskDuration = (String)pcTaskMap.get(task.SELECT_TASK_ESTIMATED_DURATION);
							  String pcTaskRequirement = (String)pcTaskMap.get(task.SELECT_TASK_REQUIREMENT);
							  String pcTaskHasSubTask = (String)pcTaskMap.get(task.SELECT_HAS_SUBTASK);

							  //[2011x Start: Collect Task ConstraintType and ConstraintDate in String holder so as to compare it with oldMap's task ConstraintType and ConstraintDate info]
							  String pcTaskConstraintType = (String)pcTaskMap.get(task.SELECT_TASK_CONSTRAINT_TYPE);
							  String pcTaskConstraintDate = (String)pcTaskMap.get(task.SELECT_TASK_CONSTRAINT_DATE);
							  //[2011x End: Collect Task ConstraintType and ConstraintDate in String holder so as to compare it with oldMap's task ConstraintType and ConstraintDate info]

							  //attrib-
							  //check if there is any modification to task name, description
							  //start date, end date, duration, %age completion etc and update accordingly -
							  //case 1: only the MS Project user modified the task data
							  //case 2: only the PC user modified the task data
							  //case 3: the PC user as well as the MS Project user
							  //         modified the task data
							  //
							  //NOTE:
							  //for case 2 and case 3 do nothing PC is the master
							  Map attributeMap = new HashMap();

							  // rpandia: Do we need to check and then set ?
							  // Don't know if there's a need to compare here
							  // Shouldn't we blindly update the MSP data here
							  // There should be no need to compare with PCdata and the Downloaded data
							  // All this is happening in the big while loop, this is slowing it down

							  if(bTaskNameChanged)
								  if(pcTaskName.equals(oldTaskName))
								  {
							  		  task.setName(context, mspTaskName);
							      }
							  if(bTaskDescChanged)
								if(pcTaskDesc.equals(oldTaskDesc))
								{
									task.setDescription(context, mspTaskDesc);
								}
							  if(bTaskOwnerChanged)
								if(pcTaskOwner.equals(oldTaskOwner))
								{
									task.setOwner(context, mspTaskOwner);
								}
							  if(bTaskStateChanged)
								if(pcTaskCurrent.equals(oldTaskCurrent))
								{
									//IR-069442V6R2011x Begin -- added try catch for setState
									//String state = getStateName(context, mspTaskCurrent);
									//task.setState(context, mspTaskCurrent);
									String sWellFormedMessage = "";
									try
									{
										task.setState(context, mspTaskCurrent);
									}
									catch(Exception ex)
									{
										if(debug)
											System.out.println("[emxMSProjectIntegration.executeSynchronizeMerge] Notice: \nPlease Take appropriate actions manually to Complete the Gate ERROR : " + ex.getMessage());
										if(mspTaskCurrent.equals(task.STATE_PROJECT_SPACE_COMPLETE))//IR-072641V6R2012 
											sWellFormedMessage = ex.getMessage() + "\nNotice: Please Take appropriate actions manually to Complete the Gate";
										else
											sWellFormedMessage = ex.getMessage();
 										throw new Exception(sWellFormedMessage);
									}
									//IR-069442V6R2011x End
								}
							  if(bTaskPercentChanged)
							  {
								  Integer newPercent = new Integer(Double.valueOf(pcTaskPercent).intValue());
								  pcTaskPercent = Integer.toString(newPercent.intValue());
								  if(pcTaskPercent.equals(oldTaskPercent) )
								  {
									  attributeMap.put(task.ATTRIBUTE_PERCENT_COMPLETE, mspTaskPercent);
								  }
							  }

							  if(SFDependentTaskList.indexOf(mspTaskId) < 0)
							  {
								  //BUG
							      //java.text.SimpleDateFormat mspDate = new java.text.SimpleDateFormat("MM/dd/yyyy");
								  codeRegn = "in the modify task attribute method";
							      if(bTaskStartChanged)
								  {
									  Date startDate = mspDate.parse(pcTaskStart);
									  pcTaskStart = MATRIX_DATE_FORMAT.format(startDate);
									  // Compare the value of StartDate (from PMC) and the value in MSP (when it was retrieved from PMC)
									  // with the old value that was in MSP to the changed value in MSP (if it was changed)
									  // If there is a change in any of them, update the StartDate

	 								  if(pcTaskStart.equals(oldTaskStart))
									  {
										  //attributeMap.put(task.ATTRIBUTE_TASK_ESTIMATED_START_DATE, mspTaskStart);
	 									  Date tempDate = mspDate.parse(mspTaskStart);
									      addUpdate(tasks, task.getId(), task.ATTRIBUTE_TASK_ESTIMATED_START_DATE, tempDate);
									      //task.updateStartDate(context, tempDate, true);
									      //addUpdate(tasks, task.getId(), "startDate", tempDate);
									  }
								  }

	 							  Date finishDate = mspDate.parse(pcTaskFinish);
	 							  pcTaskFinish = MATRIX_DATE_FORMAT.format(finishDate);
							  }
							  
							  if(bTaskDurationChanged)
							  {
								  Double newDuration = new Double(Double.valueOf(pcTaskDuration).doubleValue());
								  pcTaskDuration = Double.toString(newDuration.doubleValue());

								  if(pcTaskDuration.equals(oldTaskDuration) )//need to check with PMC
								  {
									  // Incident 288584: Because of the behavior where a task's duration is updated,
									  // the dependent's task's duration is also updated.  So, we will forcibly
									  // update the duration for all the tasks, except for the summary task.

									  if(((String)pcTaskMap.get(task.SELECT_HAS_SUBTASK)).equalsIgnoreCase("false"))
									  {
										  //attributeMap.put(task.ATTRIBUTE_TASK_ESTIMATED_DURATION, mspTaskDuration);
										  Integer tempDuration2 = new Integer(Double.valueOf(mspTaskDuration).intValue());
										  Long tempDuration = new Long(Double.valueOf(mspTaskDuration).intValue());
										  //addUpdate(tasks, task.getId(), task.ATTRIBUTE_TASK_ESTIMATED_DURATION, tempDuration);
										  //task.updateDuration(context, tempDuration2, true);
										  task.updateDuration(context, mspTaskDuration);
										  //addUpdate(tasks, task.getId(), "duration", tempDuration);
									  }
								  }
							  }//if(bTaskDurationChanged)
							  
							  HashMap programMap = new HashMap();
							  HashMap paramMap = new HashMap();
							  HashMap requestLocaleMap = new HashMap();
							  String[] arrJPOArguments = new String[1];	
							  Locale locale = new Locale("en_US");
							  
							  if(bActTaskStartChanged)
							  {
								  if(pcActTaskStart != null && !pcActTaskStart.equals(""))
								  {
									  Date startDate = mspDate.parse(pcActTaskStart);
									  pcActTaskStart = MATRIX_DATE_FORMAT.format(startDate);
								  }
								  
 								  if(pcActTaskStart.equals(oldActTaskStart))
								  {
 									 if(mspActTaskStart != "")
 									 {
 										 Date date1 = eMatrixDateFormat.getJavaDate(mspActTaskStart);
 										 int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
 										 java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
 										 mspActTaskStart  = format.format(date1);
 										 
 										 programMap.put("New Value", mspActTaskStart);
 										 programMap.put("Old Value", pcActTaskStart);
 										 programMap.put("objectId", mspTaskId);
 										 paramMap.put("paramMap", programMap);
 										 
 										 requestLocaleMap.put("locale", locale);
 										 paramMap.put("requestMap", requestLocaleMap);
 										 
	 									  arrJPOArguments = JPO.packArgs(paramMap);
	 									  JPO.invoke(context,"emxTaskBase",null,"updateTaskActualStartDate",arrJPOArguments); 									  
 									 }
 									 else
 									 {
 										task.setAttributeValue(context, task.ATTRIBUTE_TASK_ACTUAL_START_DATE, "");
 									 }
								  }
 								  else   //IR-111314V6R2012x begin
 								  {
 									  if(pcActTaskStart != null && mspActTaskStart != null)
 									  {
 										  Date date1 = null;
 										  if(mspActTaskStart !="")
 										  {
 											  date1 = eMatrixDateFormat.getJavaDate(mspActTaskStart);
 											  int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
 											  java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
 											  mspActTaskStart  = format.format(date1);
 										   										  
	 										  programMap.put("New Value", mspActTaskStart); 	
	 										  programMap.put("Old Value", pcActTaskStart); 	
	 										  programMap.put("objectId", mspTaskId);	 
	 										  paramMap.put("paramMap", programMap);
	 										 
	 										  requestLocaleMap.put("locale", locale);
	 										  paramMap.put("requestMap", requestLocaleMap);
	 										 
	 										  arrJPOArguments = JPO.packArgs(paramMap);
	 										  JPO.invoke(context,"emxTaskBase",null,"updateTaskActualStartDate",arrJPOArguments);
 										  }
 										  else
 										  {
 											  task.setAttributeValue(context, task.ATTRIBUTE_TASK_ACTUAL_START_DATE, "");
 										  }
 									  }
 								  }  //IR-111314V6R2012x end
							  }
							  
							  if(bActTaskFinishChanged)
							  {								  
								  if(pcActTaskFinish != null && !pcActTaskFinish.equals(""))
								  {
									  Date FinishDate = mspDate.parse(pcActTaskFinish);
									  pcActTaskFinish = MATRIX_DATE_FORMAT.format(FinishDate);
								  }
								  
 								  if(pcActTaskFinish.equals(oldActTaskFinish))
								  {
 									 if(mspActTaskFinish != "")
 									 {
 										  Date date1 = eMatrixDateFormat.getJavaDate(mspActTaskFinish);
 										  int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
 										  java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
 										  mspActTaskFinish  = format.format(date1);
 										  
										  programMap.put("New Value", mspActTaskFinish); 	
										  programMap.put("Old Value", pcActTaskFinish); 	
										  programMap.put("objectId", mspTaskId);	 
										  paramMap.put("paramMap", programMap);
										  
										  requestLocaleMap.put("locale", locale);
	 									  paramMap.put("requestMap", requestLocaleMap);
										  
										  arrJPOArguments = JPO.packArgs(paramMap);
										  JPO.invoke(context,"emxTaskBase",null,"updateTaskActualFinishDate",arrJPOArguments);
 									 }
 									 else
 									 {
 										task.setAttributeValue(context, task.ATTRIBUTE_TASK_ACTUAL_FINISH_DATE, "");
 									 }
								  }
							  }
							  
							  //[2011x Start: Set Modified Value of ConstraintType and ConstraintDate coming from mspMap]
							  if(bTaskConstraintTypeChanged)
							  {  
								  if(pcTaskConstraintType.equals(oldTaskConstraintType))
									  attributeMap.put(task.ATTRIBUTE_TASK_CONSTRAINT_TYPE,mspTaskConstraintType);
							  }
							  //Any modifications in Task Start or Finsh dates will also modify Task Constraint type(MS Project Behaviour)
							  // Therefore for such cases, this else part is written
							  else
								  if(pcTaskConstraintType.equals(oldTaskConstraintType))
									  attributeMap.put(task.ATTRIBUTE_TASK_CONSTRAINT_TYPE,oldTaskConstraintType);  

							  if(bTaskConstraintDateChanged)
							  {
								  if(pcTaskConstraintDate!= null && pcTaskConstraintDate.length() > 0)
								  {
									  Date parsedConstraintDate = mspDate.parse(pcTaskConstraintDate);
									  pcTaskConstraintDate= MATRIX_DATE_FORMAT.format(parsedConstraintDate );

									  if(pcTaskConstraintDate.equals(oldTaskConstraintDate))
										  attributeMap.put(task.ATTRIBUTE_TASK_CONSTRAINT_DATE,mspTaskConstraintDate);
								  }	  
								  else
									  if(oldTaskConstraintDate==null || oldTaskConstraintDate.equals(""))
										  attributeMap.put(task.ATTRIBUTE_TASK_CONSTRAINT_DATE,mspTaskConstraintDate);
							  }
  						  							  
							  //[2011x End: Set Modified Value of ConstraintType and ConstraintDate coming from mspMap]
							  
							  if(bTaskReqChanged)
							  {
								  if(pcTaskRequirement.equals(oldTaskRequirement))
								  {
									  attributeMap.put(task.ATTRIBUTE_TASK_REQUIREMENT, mspTaskRequirement);
								  }
							  }
							  if(attributeMap != null && attributeMap.size()>0  )
							  {
								  task.setAttributeValues(context, attributeMap);
							  }
						  }//if
					  }//if(mspTaskId != null && !mspTaskId.equals(""))
				  }//while (taskDataMapListItr.hasPrevious())
			  }//if (oldStructMapList.size() > 0 || newStructMapList.size() > 0 )
			  // public static java.lang.String updateDates(matrix.db.Context context,
			  //                           java.util.Map newObjectDates, boolean updateDB, boolean frmMSP)
			  //String message = task.updateDates(context, tasks, true, true);
			  if(null!=task)
				  if(null!=task.getName())
				  {
					//issue: consider project with 3 tasks and delete one of the tasks.. it failed to delete
					  task = (com.matrixone.apps.program.Task)DomainObject.newInstance(context, TYPE_TASK, DomainConstants.PROGRAM); //added new line, suggested by KYP
					  task.setId(strBusId);
					  task.rollupAndSave(context);
				  }	  

			  //Pred-
			  //update dependencies

			  // rpandia: We should check here for the size of the maplist
			  //if(assigneeMapList.size() != 0 || oldAssigneeMapList.size() != 0)
			  if(assigneeMapList != null || oldAssigneeMapList != null)
			  {
				  //add new assignee or update existing assignees
				  if (debug)
					  System.out.println("add new assignee or update existing assignees");
				  addAssignees(context, assigneeMapList, resourceIndexMap, taskIndexMap, oldAssigneeMapList, oldResourceIndexMap, oldTaskIndexMap, taskUnitMap);
			  }
			  else
			  {
				  if (debug)
					  System.out.println("No need to call addAssignees");
			  }
		  }
		  codeRegn = "Done with merge";
		  if (debug)
			  System.out.println(codeRegn);
		  //Get the latest data for the current project and display in MSP
		  //by reusing findForCheckOut function
		  //
		  //Generate response such that to get the latest using findForCheckOut
		  Element elFFCRoot = new Element("transaction");
		  elFFCRoot.setAttribute("focusbrowser", "7");
		  elFFCRoot.setAttribute("loglevel", "true");
		  elFFCRoot.setAttribute("cname", "findforcheckout");
		  elFFCRoot.setAttribute("tid", "2");
		  elFFCRoot.setAttribute("type", "command");
		  elFFCRoot.setAttribute("mpecver", "1.0.0.0");
		  elFFCRoot.setAttribute("MSP", "MSP2000");

		  // now create placeholder for response
		  Element elResponseArgumentsNode = new Element("arguments");
		  elFFCRoot.addContent(elResponseArgumentsNode);

		  // create the busid argument placeholder
		  Element elBusIdArgument = new Element("argument");
		  elBusIdArgument.setAttribute("name", "busid");
		  elBusIdArgument.setText(strBusId);
		  elResponseArgumentsNode.addContent(elBusIdArgument);

		  //Doubt
		  // create the edit status argument placeholder
		  // on Merge to ematrix the edit flag will be reset to false
		  // the user will have to explicitly "Launch the project in Edit mode"
		  // inorder to make any further changes
		  Element elEditStatusArgument = new Element("argument");
		  elEditStatusArgument.setAttribute("name", "foredit");
		  elEditStatusArgument.setText("true");
		  elResponseArgumentsNode.addContent(elEditStatusArgument);

		  //use a temp name which is consistent with the way the servlet
		  //names the file such that dumpTransactionXMLForServlet can be reused.
		  String tempFileName = "findForCheckOut_1_2_3";
		  //String fileName = dumpTransactionXMLForServlet (context, gco, tempFileName, elFFCRoot);
		  String fileName = dumpTransactionXMLForServlet (context, tempFileName, elFFCRoot);
		  if (debug)
			  System.out.println("done dumping the transaction xml file");
		  
		  String[] newArgs = new String[2];
		  newArgs[0] = fileName;

		  codeRegn = "Commiting transaction";
		  if (debug)
			  System.out.println(codeRegn);

		  // commit work
		  ContextUtil.commitTransaction(context);

		  if (debug)
		  {
			  mergeEndTime = System.currentTimeMillis();
			  long totalMergeTime = mergeEndTime - mergeStartTime;

			  long afterDeleteTotalTime = mergeEndTime - afterDeleteStartTime;

			  //System.out.println("TOTAL MERGE TIME afterDeleteTotalTime (in Secs): "+(float)afterDeleteTotalTime/1000);
			  if(debug)
				  System.out.println("TOTAL MERGE TIME (in Secs): "+(float)totalMergeTime/1000);
		  }

		  mergeEndTime = System.currentTimeMillis();
		  long totalMergeTime = mergeEndTime - mergeStartTime;
		  //get the latest for this current project and display it in MSP
		  if(debug)
			  System.out.println("==========================END   executeSynchronizeMerge============================================");
		  return executeFindForCheckout(context, newArgs);	
	  }
	  catch(Exception e)
	  {
		  ContextUtil.abortTransaction(context);
		  if (e.toString().indexOf("well-formed character") != -1)
		  {
			  String wellFormedMessage = "One of the tasks in the project contains characters that is not supported, Check the task names and the notes. \n";
			  throw new MatrixException(wellFormedMessage + e.getMessage());
		  }
		  else
		  {
			  e.printStackTrace();
			  throw new MatrixException(e.getMessage());
		  }
	  }	
  }

  public String executeValidateResource(Context context, String[] args)
  {
	  if(debug)
		  System.out.println("==========================START   executeValidateResource============================================");
	  codeRegn = "inside executeValidateResource";
	  if (debug)
	  {
		  System.out.println(codeRegn);
		  StartValidateResourceTime = System.currentTimeMillis();
	  }
	  StartValidateResourceTime = System.currentTimeMillis();
	  try
	  {
		  Element elResourcesRoot = null;

		  Element elCommandRoot = loadXMLSentFromServlet(args[0], context);
		  if (debug)
			  System.out.println("Read the xml sent from client...done");

		  codeRegn = "XML sent from servlet read.";

		  // get the ResourceXml
		  Element elCommandArguments = elCommandRoot.getChild("arguments");

		  List lArguments = elCommandArguments.getChildren("argument");
		  ListIterator litCtr = lArguments.listIterator();

		  String strBusId = "";
		  while (litCtr.hasNext())
		  {
			  Element elArgument = (Element) litCtr.next();

			  List lArgu = elArgument.getChildren();
			  ListIterator litArg = lArgu.listIterator();

			  if(elArgument.getAttributeValue("name").equals("validatexml"))
			  {
				  System.out.println("Text validatexml: "+elArgument.getAttributeValue("name"));
				  elResourcesRoot = elArgument.getChild("Resources");
			  }						
		  }//while (litCtr.hasNext())
		
		  codeRegn = "Read XML for getting Resource Name";
		  Map taskIndexMap = new HashMap();
		  ArrayList ResourceNamesList = readXMLDataForValidateResource(context, elResourcesRoot);
		  System.out.println("ResourceNamesList " + ResourceNamesList);
		
		  Iterator<String> ResourceNameItr = ResourceNamesList.iterator();	
		  ArrayList<String> MatchedResourceNamesList = new ArrayList<String>();
		  while(ResourceNameItr.hasNext())
		  {
			  String strResourceName = ResourceNameItr.next();
			  System.out.println("strResourceName " + strResourceName);
			  if(_personInfo != null && _personInfo.get(strResourceName) == null)
			  {
				  //found new Last Name First Name, add to cache
				  addLastFirstNameToCache(context, strResourceName);
			  }

			  //get the personName from cache map
			  String personName = (String) _personInfo.get(strResourceName);
			  if(personName != null && !personName.equals("")) 			
				  System.out.println("personName exists");
			  else
			  {
				  MatchedResourceNamesList.add(strResourceName);
				  System.out.println("personName does not exist");
			  }
		  }
		  String fileName = executeResponseForValidateResource(context,args,MatchedResourceNamesList,"success");
		
		  EndValidateResourceTime = System.currentTimeMillis();
		  long totalValidateResourceTime = EndValidateResourceTime - StartValidateResourceTime;
		  if(debug)
			  System.out.println("==========================END   executeValidateResource============================================");

		  return "true| " + fileName;
	  }
	  catch(Exception e)
	  {
		  //throw new MatrixException(e.getMessage());		
		  System.out.println(e.getMessage());
		  return "Failure";
	  }			
  }

 public String executeResponseForValidateResource(Context context, String[] args, ArrayList ResourceNamesList, String result) throws MatrixException
  {
	  if(debug)
		  System.out.println("==========================START   executeResponseForValidateResource============================================" );
	  codeRegn = "inside executeResponseForValidateResource";

	  try
	  {
		  Element elCommandRoot = loadXMLSentFromServlet(args[0], context);
		  Element elResponseRoot = new Element("transaction");
		  java.util.List attribList = elCommandRoot.getAttributes();
		  ListIterator litAtr = attribList.listIterator();

		  while (litAtr.hasNext())
		  {
			  litAtr.next();

			  elResponseRoot.setAttribute("focusbrowser", elCommandRoot.getAttributeValue("focusbrowser"));

			  elResponseRoot.setAttribute("loglevel", elCommandRoot.getAttributeValue("loglevel"));

			  elResponseRoot.setAttribute("cname", elCommandRoot.getAttributeValue("cname"));

			  elResponseRoot.setAttribute("tid", elCommandRoot.getAttributeValue("tid"));

			  elResponseRoot.setAttribute("type", elCommandRoot.getAttributeValue("type"));

			  elResponseRoot.setAttribute("mpecver", elCommandRoot.getAttributeValue("mpecver"));

			  elResponseRoot.setAttribute("MSP", elCommandRoot.getAttributeValue("MSP"));
		  }

		  elResponseRoot.getAttribute("type").setValue("response");

		  // now create placeholder for response
		  Element elResponseArgumentsNode = new Element("arguments");
		  elResponseRoot.addContent(elResponseArgumentsNode);

		  // create the busid argument placeholder
		  Element elArgument = new Element("argument");
		  elArgument.setAttribute("name", "validatexml");
		  elResponseArgumentsNode.addContent(elArgument);

		  Element elResourceArgument = new Element("Resources");
		  elArgument.addContent(elResourceArgument);
		
		  Iterator<String> ResourceNameItr = ResourceNamesList.iterator();
		  String strResourceName;
		  while(ResourceNameItr.hasNext())
		  {
			  strResourceName = ResourceNameItr.next();
			  Element elCurrentResourceNode = new Element("Resource");
			  elCurrentResourceNode.setText(strResourceName);
			  elResourceArgument.addContent(elCurrentResourceNode);
		  }

		  elResponseRoot.setAttribute("result", result/*"success"*/);
		  String tempFileName = "ValidateResource_1_2_3";
		  //String fileName = dumpTransactionXMLForServlet (context, gco, tempFileName, elFFCRoot);
		  String fileName = dumpTransactionXMLForServlet (context, tempFileName, elResponseRoot);
		
		  if(debug)
			  System.out.println("==========================END   executeResponseForValidateResource============================================");
		  return tempFileName;//"Success";
	  }
	  catch(Exception e)
	  {
		  //e.printStackTrace();
		  System.out.println("{Exception} : " + e.getMessage());
		  throw new MatrixException(e.getMessage());
	  }
  }

  public String SynchronizeNewProject(Context context, Map newProjectMap)
	                                                 throws MatrixException
  {
	  if(debug)
		  System.out.println("==========================START   SynchronizeNewProject============================================");
 	  String strBusId = "";

	  try
	  {
		  boolean existingProject = false;

		  //obtain data from the map
	  	  //project info, task info, resource map, assignments
	 	  Map prjInfoMap          = (Map) newProjectMap.get("projectData");
		  Map resourceIndexMap    = (Map) newProjectMap.get("resourceIndexMap");
		  StringList resourceList = (StringList) newProjectMap.get("resourceList");
		  MapList dependencyMapList  = (MapList) newProjectMap.get("dependencyMapList");
		  MapList assigneeMapList     = (MapList) newProjectMap.get("assigneeMapList");
		  MapList taskDataMapList = (MapList) newProjectMap.get("taskData");
		  Map taskLevelMap = (Map) newProjectMap.get("taskLevelMap");
		  Map taskIndexMap = new HashMap();//(Map) newProjectMap.get("taskIndexMap");
		  //rp
		  Map taskUnitMap = (Map) newProjectMap.get("taskUnitsDataMap");

		  if (debug)
			  System.out.println("taskLevelMap : "+taskLevelMap);

		  java.text.SimpleDateFormat mspDate = new java.text.SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(),
                     Locale.US);
		  codeRegn = "accessed the map values ";

		  String owner = null;
		  String estStartDate = null;
		  String estFinishDate = null;
		  String actStartDate = null;
		  String actFinishDate = null;

		  taskLevelMap.clear();
		  String projectName = (String) prjInfoMap.get("projectName");
		  String description = (String) prjInfoMap.get("description");
		  String spProjectConstraintType=(String) prjInfoMap.get("constraintType");
		  String schedulefromstart =(String)prjInfoMap.get("ScheduleFrom");
		  owner = (String) prjInfoMap.get("Author");
		  
		  if (debug)
			  System.out.println("Not an existing project...");
		  String ctxPersonId = person.getPerson(context).getId();							   
		  person.setId(ctxPersonId);		  

		  String personName = person.getName(context);		 
		  String userVault = person.getVault();				 

		  codeRegn = "created new project ";
		  if (debug)
			  System.out.println(codeRegn);

		  String projectType = "";
		  ListIterator Itr = taskDataMapList.listIterator();
		  if(Itr.hasNext())
		  {
			  Map projectMap = (Map) Itr.next();
			  projectType = (String) projectMap.get("taskType");
		  }
		  mspiCreateProject(context,  project,  projectType,  projectName,  userVault) ;

		  project.setDescription(context, description);		   
		  project.setAttributeValue(context, ATTRIBUTE_PROJECT_SCHEDULE_FROM, schedulefromstart);
		  strBusId = project.getId();			   

		  Map spAttributes = new HashMap();			  
		  spAttributes.put(ATTRIBUTE_DEFAULT_CONSTRAINT_TYPE, spProjectConstraintType);
		  
		  if(schedulefromstart.equals("Project Start Date"))
        	  spAttributes.put(ATTRIBUTE_TASK_CONSTRAINT_DATE, (String) prjInfoMap.get("ProjectStartDate"));
		  else
			  spAttributes.put(ATTRIBUTE_TASK_CONSTRAINT_DATE, (String) prjInfoMap.get("ProjectFinishDate"));
          
          project.setAttributeValues(context,spAttributes);

		  ListIterator taskDataItr = taskDataMapList.listIterator();
		  String outlineNumber = null;
		  String taskUID = null;
		  String taskReq = null;
		  if(taskDataItr.hasNext()) // special case for summary node
		  {
			  project.setId(strBusId);
			  Map nodeMap = (Map) taskDataItr.next();

			  codeRegn = "setting the attributes ";
			  estStartDate = (String) nodeMap.get("estStartDate");
			  estFinishDate = (String) nodeMap.get("estFinishDate");
			  String percentComplete = (String) nodeMap.get("percentComplete");
			  String estDuration = (String) nodeMap.get("estDuration");
			  String actDuration = (String) nodeMap.get("actDuration");
 			  
			  //[2011x]
			  String spConstraintType=(String) nodeMap.get("constraintType");
			  codeRegn = "set the attribute values into a map";
			  Map attributeMap = new HashMap(5);

			  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_START_DATE, estStartDate);
			  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE, estFinishDate);

			  if(percentComplete != null && !percentComplete.equals("0"))
				  attributeMap.put(ATTRIBUTE_PERCENT_COMPLETE, percentComplete);

  			  if(estDuration != null && estDuration.length() > 0)
				  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_DURATION, estDuration);
			  
			  if(actDuration != null && actDuration.length() > 0)
				  attributeMap.put(ATTRIBUTE_TASK_ACTUAL_DURATION, actDuration);

			  codeRegn = "setting project attribute values ";
			  if (debug)
				  System.out.println(codeRegn);
			  
			  project.setAttributeValues(context, attributeMap);
			  //To set Project Schedule Based on option to Actual
              String ATTRIBUTE_SCHEDULEBASEDON = (String)PropertyUtil.getSchemaProperty("attribute_ScheduleBasedOn");              
              project.setAttributeValue(context, ATTRIBUTE_SCHEDULEBASEDON, sProjectSchduleBasedOn);
		  }

		  codeRegn = "create tasks";
		  while (taskDataItr.hasNext())
		  {
			  project.setId(strBusId);
			  Map nodeMap = (Map) taskDataItr.next();
			  taskUID = (String) nodeMap.get("taskUID");
			  String taskName = (String) nodeMap.get("taskName");
			  //[2011x Start: Collect Subproject Task Constraint Type and Date]
			  String spConstraintType=(String) nodeMap.get("constraintType");
			  String spConstraintDate=(String) nodeMap.get("constraintDate");
			  //[2011x End: Collect Subproject Task Constraint Type and Date]

			  if (debug)
				  System.out.println("creating task with name: "+taskName);

			  String taskDescription = (String) nodeMap.get("description");
			  String outlineLevel = (String) nodeMap.get("outlineLevel");
			  outlineNumber = (String) nodeMap.get("outlineNumber");
			  if (debug)
			  {
				  System.out.println("outlineNumber : " + outlineNumber);
				  System.out.println("outlineLevel : " + outlineLevel);
			  }

			  taskReq = (String) nodeMap.get("taskReq");
			  String taskState = (String) nodeMap.get("taskState");
			  owner = (String) nodeMap.get("owner");
			  String taskType = (String) nodeMap.get("taskType");

			  codeRegn = "Creating subproject";
			  Map subprojectMap = (Map) nodeMap.get("subprojectMap");
			  if (subprojectMap != null)
			  {
				  Map subprojectTaskIndexMap = new HashMap();
				  subprojectMap.put("taskIndexMap", subprojectTaskIndexMap);
				  String subprojectId = SynchronizeNewProject(context, subprojectMap);
				  subprojectTaskIndexMap = (Map) subprojectMap.get("taskIndexMap");
				  taskIndexMap.putAll(subprojectTaskIndexMap);

				  taskLevelMap.put(outlineNumber, subprojectId);
				  taskIndexMap.put(taskUID, subprojectId);

				  String seqNo = null;
				  String parentId;
				  String levelId = null;

				  if(outlineNumber.lastIndexOf(".") == -1)
				  {
					  parentId = strBusId;
				  }
				  else
				  {						
					  levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
					  parentId = (String) taskLevelMap.get(levelId);
				  }				

				  task.setId(parentId);
				  String []childIds = new String[1];
				  childIds[0] = subprojectId;
				  task.addExisting(context, childIds , parentId);
			
				  ProjectIds.add(subprojectId);
			  }//if (subprojectMap != null)
			  else if (isProjectSpace(context,taskType)==true)
			  {
				  mspiCreateProject(context,  subProject,  taskType,  taskName,  userVault) ;
				  String subprojectId = subProject.getId();
				  task.setId(subprojectId);

				  taskLevelMap.put(outlineNumber, subprojectId);
				  taskIndexMap.put(taskUID, subprojectId);
				  subProject.setDescription(context, taskDescription);

				  codeRegn = "setting the attributes ";
				  estStartDate = (String) nodeMap.get("estStartDate");
				  estFinishDate = (String) nodeMap.get("estFinishDate");
				  String percentComplete = (String) nodeMap.get("percentComplete");
				  String estDuration = (String) nodeMap.get("estDuration");
				  String actDuration = (String) nodeMap.get("actDuration");

				  codeRegn = "set the attribute values into a map";
				  Map attributeMap = new HashMap(5);

				  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_START_DATE, estStartDate);
				  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE, estFinishDate);
				  
				  if(percentComplete != null && !percentComplete.equals("0"))
					  attributeMap.put(ATTRIBUTE_PERCENT_COMPLETE, percentComplete);

				  if(estDuration != null && estDuration.length() > 0)
					  attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_DURATION, estDuration);

				  if(actDuration != null && actDuration.length() > 0)
					  attributeMap.put(ATTRIBUTE_TASK_ACTUAL_DURATION, actDuration);
				  
				  codeRegn = "setting project attribute values ";
				  
				  subProject.setAttributeValues(context, attributeMap);		

				  String seqNo = null;
				  String parentId;

				  if(outlineNumber.lastIndexOf(".") == -1)
				  {
					  parentId = strBusId;
				  }
				  else
				  {
					  String levelId = null;
					  levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
					  parentId = (String) taskLevelMap.get(levelId);
				  }				

				  task.setId(parentId);
				  String []childIds = new String[1];
				  childIds[0] = subprojectId;
				  task.addExisting(context, childIds , parentId);

				  ProjectIds.add(subprojectId);
			   }//else if (isProjectSpace(context,taskType)==true)
			   else
			   {
				   //START:Added:P6E:16-Aug-2011:PRG:R213:Bug 121547V6R2013
				   //task retaining policy of previous task, to get rid off that Initializing task again.
				   task = (Task) DomainObject.newInstance(m_ctxUserContext, DomainConstants.TYPE_TASK, DomainConstants.PROGRAM);
				   //End:P6E:16-Aug-2011:PRG:R213:Bug 121547V6R2013
				   
				   if(outlineNumber.lastIndexOf(".") == -1)
				   {
					   boolean retValue = mspiCreateTask(context, task, taskType, taskName, project, true);
					   codeRegn = "created task 1";
				   }
				   else
				   {
					   String levelId = null;
					   levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
					   String parentId = (String) taskLevelMap.get(levelId);
					   if (debug)
					   {
						   System.out.println("levelId : " + levelId);
						   System.out.println("parentId : " + parentId);
					   }
					   parentTask.setId(parentId);
					   boolean retValue = mspiCreateTask(context, task, taskType, taskName, parentTask, false);
					   codeRegn = "created task 2";
				   }

				   task.setDescription(context, taskDescription);				   
				   if(taskState != null && !taskState.equals(""))	   
					   task.setState(context, taskState);			   
				   task.setOwner(context, owner);				 

				   String currentTaskId = task.getId();
				   taskLevelMap.put(outlineNumber, currentTaskId);
				   taskIndexMap.put(taskUID, currentTaskId);

				   codeRegn = "setting the attributes ";
				   estStartDate = (String) nodeMap.get("estStartDate");
				   estFinishDate = (String) nodeMap.get("estFinishDate");
				   actStartDate = (String) nodeMap.get("actStartDate");
				   actFinishDate = (String) nodeMap.get("actFinishDate");				   
				   String percentComplete = (String) nodeMap.get("percentComplete");
				   String estDuration = (String) nodeMap.get("estDuration");
				   String actDuration = (String) nodeMap.get("actDuration");

				   codeRegn = "set the attribute values into a map";
				   Map attributeMap = new HashMap(5);

				   attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_START_DATE, estStartDate);
				   attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE, estFinishDate);

				   if(actStartDate != null && actStartDate.length() > 0)
				   {
					   if(actStartDate != "")
					   {
						   HashMap programMap = new HashMap();
						   HashMap requestLocaleMap = new HashMap();
						   HashMap paramMap = new HashMap();
						   String[] arrJPOArguments = new String[1];
						   
						   Locale locale = new Locale("en_US");
						   Date date1 = eMatrixDateFormat.getJavaDate(actStartDate);
						   int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
						   java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
						   actStartDate  = format.format(date1);
						   
						   programMap.put("New Value", actStartDate);
						   programMap.put("Old Value", null);
						   programMap.put("objectId", currentTaskId);
						   paramMap.put("paramMap", programMap);
						   
						   requestLocaleMap.put("locale", locale);
						   paramMap.put("requestMap", requestLocaleMap);
						   
						   arrJPOArguments = JPO.packArgs(paramMap);
						   JPO.invoke(context,"emxTaskBase",null,"updateTaskActualStartDate",arrJPOArguments);
					   }
					   else
					   {
						   task.setAttributeValue(context, task.ATTRIBUTE_TASK_ACTUAL_START_DATE, "");
					   }
				   }
					
				   if(actFinishDate != null && actFinishDate.length() > 0)
				   {
					   if(actFinishDate != "")
					   {
						  HashMap programMap = new HashMap();
						  HashMap requestLocaleMap = new HashMap();	
						  HashMap paramMap = new HashMap();	
						  String[] arrJPOArguments = new String[1];
							  
						  Locale locale = new Locale("en_US");
						  Date date1 = eMatrixDateFormat.getJavaDate(actFinishDate);
						  int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
						  java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
						  actFinishDate  = format.format(date1);
							  
						  programMap.put("New Value", actFinishDate);
						  programMap.put("Old Value", null);
						  programMap.put("objectId", currentTaskId);	
						  paramMap.put("paramMap", programMap);
							  
						  requestLocaleMap.put("locale", locale);
						  paramMap.put("requestMap", requestLocaleMap);
							  
						  arrJPOArguments = JPO.packArgs(paramMap);
						  JPO.invoke(context,"emxTaskBase",null,"updateTaskActualFinishDate",arrJPOArguments);
					   }
					   else
					   {
						   task.setAttributeValue(context, task.ATTRIBUTE_TASK_ACTUAL_FINISH_DATE, "");
					   }
				   }

				   if(percentComplete != null && !percentComplete.equals("0"))
					   attributeMap.put(ATTRIBUTE_PERCENT_COMPLETE, percentComplete);

				   if(estDuration != null && estDuration.length() > 0)
					   attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_DURATION, estDuration);

				   if(actDuration != null && actDuration.length() > 0)
					   attributeMap.put(ATTRIBUTE_TASK_ACTUAL_DURATION, actDuration);

				   if(taskReq != null && !taskReq.equals(""))
				   {
					   attributeMap.put(task.ATTRIBUTE_TASK_REQUIREMENT, taskReq);
				   }
				   
				   //[2011x Start: Inserting Subproject-Task Constraint Type and Date in attribut map]
				   if(spConstraintType!= null && spConstraintType.length() > 0)
						  attributeMap.put(ATTRIBUTE_TASK_CONSTRAINT_TYPE, spConstraintType);
				  
				   if(spConstraintDate!= null && spConstraintDate.length() > 0)
						  attributeMap.put(ATTRIBUTE_TASK_CONSTRAINT_DATE, spConstraintDate);
				   //[2011x Start: Inserting Subproject-Task Constraint Type and Date in attribut map]
					  				   
				   codeRegn = "setting task attribute values ";	   
				   task.setAttributeValues(context, attributeMap);					 
				}		  
		  }//while (taskDataItr.hasNext())
			
		  newProjectMap.put("taskIndexMap", taskIndexMap);

		  codeRegn = "adding dependency";

		  //if(assigneeMapList != null)
		  if (assigneeMapList.size() != 0)
		  {
			  if (debug)
				  System.out.println("Adding Assignees");
			  //create assignees for project created/updated
			  //addAssignees(context, assigneeMapList, resourceIndexMap, taskIndexMap, null, null, null);
			  addAssignees(context, assigneeMapList, resourceIndexMap, taskIndexMap, null, null, null, taskUnitMap);
  		  }
		  if (debug)
			  System.out.println("Adding Assignees...done");
		  // commit work
	   }
	   catch(Exception e)
  	   {
		   ContextUtil.abortTransaction(context);
		   if (e.toString().indexOf("well-formed character") != -1)
		   {
			   String wellFormedMessage = "One of the tasks in the project contains characters that is not supported, Check the task names and the notes. \n";
			   throw new MatrixException(wellFormedMessage + e.getMessage());
		   }
		   else
		   {
			   throw new MatrixException(e.getMessage());
		   }
	   }
	   if(debug)
		   System.out.println("==========================END   SynchronizeNewProject============================================" +strBusId);

	   return strBusId;
   }

	public String SynchronizeExistingProject(Context context,String strBusId, Map newProjectMap, Map oldProjectMap, Map tasks)
	                                                 throws MatrixException
	{
		if(debug)
			System.out.println("==========================START   SynchronizeExistingProject============================================");

		codeRegn = "inside SynchronizeExistingProject";
		if (debug)
		{
			System.out.println(codeRegn);
			mergeStartTime = System.currentTimeMillis();
		}
		mergeStartTime = System.currentTimeMillis();
		try
		{
			boolean existingProject = true;

			//obtain data from the map
			//project info, task info, resource map, assignments
			Map prjInfoMap          = (Map) newProjectMap.get("projectData");
			Map resourceIndexMap    = (Map) newProjectMap.get("resourceIndexMap");
			StringList resourceList = (StringList) newProjectMap.get("resourceList");
			MapList dependencyMapList  = (MapList) newProjectMap.get("dependencyMapList");
			MapList assigneeMapList     = (MapList) newProjectMap.get("assigneeMapList");
			MapList taskDataMapList = (MapList) newProjectMap.get("taskData");
			Map taskLevelMap = (Map) newProjectMap.get("taskLevelMap");
			Map taskIndexMap = (Map) newProjectMap.get("taskIndexMap");
			//rp
			Map taskUnitMap = (Map) newProjectMap.get("taskUnitsDataMap");

			if (debug)
				System.out.println("taskLevelMap : "+taskLevelMap);

			java.text.SimpleDateFormat mspDate = new java.text.SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(),
						 Locale.US);
			codeRegn = "accessed the map values ";

			String owner = null;
			String estStartDate = null;
			String estFinishDate = null;
			String actStartDate = null;
			String actFinishDate = null;

			String ctxPersonId = person.getPerson(context).getId();
			person.setId(ctxPersonId);
			String personName = person.getName(context);
			String userVault = person.getVault();
			
			codeRegn = "Reading XML for Exisiting PC Project";
			existingProject = true;
			//Merge-
			//obtain data from the map

			//Get the data of the project
			//get project info, task info, resource map, assignments
			Map oldPrjInfoMap          = (Map) oldProjectMap.get("projectData");
			Map oldResourceIndexMap    = (Map) oldProjectMap.get("resourceIndexMap");
			StringList oldResourceList = (StringList) oldProjectMap.get("resourceList");
			MapList oldDependencyMapList  = (MapList) oldProjectMap.get("dependencyMapList");
			MapList oldAssigneeMapList     = (MapList) oldProjectMap.get("assigneeMapList");
			MapList oldTaskDataMapList = (MapList) oldProjectMap.get("taskData");
			Map oldTaskIndexMap = (Map) oldProjectMap.get("taskIndexMap");

			Map oldTaskUnitMap = (Map) oldProjectMap.get("taskUnitsDataMap");
			HashMap NextOutlineMap = new HashMap();

			//check if there is any modification in project name and description
			//and update accordingly -
			//case 1: only the MS Project user modified the project data
			//case 2: only the PC user modified the project data
			//case 3: the PC user as well as the MS Project user
			//         modified the project data
			//
			//NOTE:
			//for case 2 and case 3 do nothing PC is the master

			//project-
			// Define selectables for each Task object.
			StringList busSelects = new StringList(11);
			busSelects.add(SELECT_NAME);
			busSelects.add(SELECT_DESCRIPTION);
			busSelects.add(SELECT_ATTRIBUTE_PROJECT_SCHEDULE_FROM);
			project.setId(strBusId);																														  
			project.open(context);
			Map curProjectMap = project.getInfo(context, busSelects);
			String curProjectName = (String)curProjectMap.get(SELECT_NAME);
			String curProjectDesc = (String)curProjectMap.get(SELECT_DESCRIPTION);
			String curProjectScheduleFromStart = (String)curProjectMap.get(SELECT_ATTRIBUTE_PROJECT_SCHEDULE_FROM);

			//for case 2 and case 3 do nothing PC is the master
			if(curProjectName.equals(oldPrjInfoMap.get("projectName")) &&
				!(oldPrjInfoMap.get("projectName")).equals(prjInfoMap.get("projectName")))
			{
				project.setName(context, (String) prjInfoMap.get("projectName"));				   
			}
			if(curProjectDesc.equals(oldPrjInfoMap.get("description")) &&
				!(oldPrjInfoMap.get("description")).equals(prjInfoMap.get("description")))
			{
				project.setDescription(context, (String) prjInfoMap.get("description"));			
			}
			
			if(curProjectScheduleFromStart.equals(oldPrjInfoMap.get("ScheduleFrom")) &&
					!(oldPrjInfoMap.get("ScheduleFrom")).equals(prjInfoMap.get("ScheduleFrom")))
			{
				project.setAttributeValue(context, ATTRIBUTE_PROJECT_SCHEDULE_FROM, (String) prjInfoMap.get("ScheduleFrom"));
			}
			
			project.setAttributeValue(context, ATTRIBUTE_SCHEDULEBASEDON, sProjectSchduleBasedOn);
			project.close(context);
			//get the new data of the project
			MapList newTaskDataMapList = (MapList) newProjectMap.get("taskData");
			Map newPrjInfoMap          = (Map) newProjectMap.get("projectData");

			// cn - check
			MapList addedList    = new MapList();
			MapList removedList  = new MapList();
			MapList retainedList = new MapList();
			MapList modifiedList = new MapList();
			
			//generate a new maplist for comparison of structure
			//the new maplist only consists of taskId and level (outlineNumber)
			//if there is no taskId then there is a new task added
			//if there is change in level then the task has been moved
			//to a different level
			MapList oldStructMapList = new MapList();
			MapList newStructMapList = new MapList();
			HashMap oldDataMap = new HashMap();
			HashMap newDataMap = new HashMap();
			HashMap oldSubprojectMap =  new HashMap();
			HashMap newSubprojectMap =  new HashMap();

			HashMap OIDMap = new HashMap();
			HashMap OIDMapOld = new HashMap();
			HashMap TaskLevelMap = new HashMap();
			HashMap TaskLevelMapOld = new HashMap();
			String parentOutline =null;
			String parentId = null;
			if(oldTaskDataMapList.size() > 0)
			{
				ListIterator oldTaskDataMapListItr = oldTaskDataMapList.listIterator();
				int i = 0;
				while(oldTaskDataMapListItr.hasNext())
				{
					//skip the first value
					//this corresponds to the project details
					HashMap buildMap = new HashMap();
					Map oldTaskMap = (Map) oldTaskDataMapListItr.next();
					if(i != 0)
					{
						String id = (String) oldTaskMap.get("taskId");
						String outlineNumber = (String) oldTaskMap.get("outlineNumber");
						buildMap.put("taskId", id);
						buildMap.put("outlineNumber", outlineNumber);

						if(outlineNumber != null && !outlineNumber.equals("0"))
						{
							oldStructMapList.add(buildMap);
						}
						if(outlineNumber.indexOf(".") > -1)
						{
							parentOutline = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
							parentId = (String)TaskLevelMapOld.get(parentOutline);
							if(parentId !=null)
								buildMap.put("parentTaskId", parentId);
						}
						//build an hashmap to locate the node details
						//based on taskId as key
						//if taskId does not exist then use outlineNumber as key
						if(id != null || !id.equals(""))
						{
							oldDataMap.put(id, oldTaskMap);
							TaskLevelMapOld.put(outlineNumber,id);
							OIDMapOld.put(id,outlineNumber);
							Map subprojectmap =(Map)oldTaskMap.get("subprojectMap");
							if(subprojectmap != null)
								AllOldProjectIds.add(id);
						}
						else
						{
							oldDataMap.put(outlineNumber, oldTaskMap);
						}
					}//if(i != 0)
					i++;
				}//while(oldTaskDataMapListItr.hasNext())
			}//if(oldTaskDataMapList.size() > 0)
			
			String prevOutlineNumber = "aaaaa";

			if(newTaskDataMapList.size() > 0)
			{
				ListIterator newTaskDataMapListItr = newTaskDataMapList.listIterator();
				int i = 0;
				while(newTaskDataMapListItr.hasNext())
				{
					HashMap buildMap = new HashMap();
					Map newTaskMap = (Map) newTaskDataMapListItr.next();
					if(i != 0) 
					{
						String id = (String) newTaskMap.get("taskId");
						String outlineNumber = (String) newTaskMap.get("outlineNumber");
						buildMap.put("taskId", id);
						buildMap.put("outlineNumber", outlineNumber);
			
						if(outlineNumber.indexOf(".") > -1)
						{
							parentOutline = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
							parentId = (String)TaskLevelMap.get(parentOutline);
							if(parentId !=null)
								buildMap.put("parentTaskId", parentId);
						}

						NextOutlineMap.put(prevOutlineNumber,outlineNumber);
						prevOutlineNumber = outlineNumber;

						if(outlineNumber != null && !outlineNumber.equals("0")) 
						{
							newStructMapList.add(buildMap);
						}
						//build an hashmap to locate the node details based on taskId as key
						//if taskId does not exist then use outlineNumber as key
						if(id != null && !id.equals("")) 
						{
							newDataMap.put(id, newTaskMap);
							TaskLevelMap.put(outlineNumber, id);
							OIDMap.put(id,outlineNumber);
						}
						else
						{
							newDataMap.put(outlineNumber, newTaskMap);
						}
					}//if(i != 0) 
					i++;
				}//while(newTaskDataMapListItr.hasNext())
			}//if(newTaskDataMapList.size() > 0)

			if (oldStructMapList.size() > 0 || newStructMapList.size() > 0)
			{
				// Get a list of all added items (new - old).
				addedList.addAll(newStructMapList);
				addedList.removeAll(oldStructMapList);

				// Get a list of all removed items (old - new).
				removedList.addAll(oldStructMapList);
				removedList.removeAll(newStructMapList);

				//store the removedList as a reference
				//required while updating the attributes
				MapList refRemovedList = new MapList();
				refRemovedList.addAll(removedList);

				// Get a list of all retained items (new & old).
				retainedList.addAll(oldStructMapList);
				retainedList.retainAll(newStructMapList);

				// Get a list of tasks neither added or removed
				// but could have been modified (old - removed).
				// (no structure modification but only attribute modification)
				modifiedList.addAll(newStructMapList);

				// cn - check
				if(addedList != null)
				//if (addedList.size() != 0)
				{					
					ListIterator addedListItr = addedList.listIterator();

					while (addedListItr.hasNext())
					{
						Map addedListMap = (Map) addedListItr.next();
						String taskId = (String) addedListMap.get("taskId");
						// cn - check
						if(taskId != null && !taskId.equals(""))
						{
							//the removedList has entries of same taskId since the outlineLevelNumber get modified due to structure change
							//if this task is handeled already then remove if from the removedList
							Iterator removeItr = removedList.iterator();
							int i = 0;	
							while(removeItr.hasNext())
							{
								Map removedMap = (Map) removeItr.next();
								if(taskId.equals(removedMap.get("taskId")))
								{
									break;
								}
								i++;
							}
							if(i < removedList.size())
							{
								removedList.remove(i);
								refRemovedList.remove(i);
							}
						}//if(taskId != null && !taskId.equals(""))
					}//while (addedListItr.hasNext())
				}//if(addedList != null)

				// cn - check
				if(addedList != null)
				{
					codeRegn = "Merge- Adding the task";

					ListIterator addedListItr = addedList.listIterator();

					while (addedListItr.hasNext())
					{
						if (debug)
							iterationStartTime = System.currentTimeMillis();

						Map addedListMap = (Map) addedListItr.next();
						String taskId = (String) addedListMap.get("taskId");
						String outlineNumber = (String) addedListMap.get("outlineNumber");

						Map oldTaskValueMap = new HashMap();
						Map newTaskValueMap = new HashMap();
						if((taskId != null) && !taskId.equals(""))
						{
							oldTaskValueMap = (Map) oldDataMap.get(taskId);
							newTaskValueMap = (Map) newDataMap.get(taskId);
						}
						else
						{
							newTaskValueMap = (Map) newDataMap.get(outlineNumber);
						}

						String levelId = null;
						String newParentId = null;

						String taskUID = (String) newTaskValueMap.get("taskUID");
						String taskName = (String) newTaskValueMap.get("taskName");
						if (debug)
						{
							System.out.println("Processing task: " +taskName);
						}
						String taskDesc = (String) newTaskValueMap.get("description");
						owner = (String) newTaskValueMap.get("owner");
						String taskType = (String)newTaskValueMap.get("taskType");
						String taskState = (String)newTaskValueMap.get("taskState");
						String percentComplete  = (String) newTaskValueMap.get("percentComplete");
						
						//Constraint Type And Dates
						String spConstraintType=null;
						String spConstraintDate=null;
						
						spConstraintType=(String)newTaskValueMap.get("constraintType");
						spConstraintDate=(String)newTaskValueMap.get("constraintDate");
						
						estStartDate  = (String) newTaskValueMap.get("estStartDate");
						estFinishDate  = (String) newTaskValueMap.get("estFinishDate");

						actStartDate  = (String) newTaskValueMap.get("actStartDate");
						actFinishDate  = (String) newTaskValueMap.get("actFinishDate");

						String estDuration  = (String) newTaskValueMap.get("estDuration");
						String outlineLevel = (String) newTaskValueMap.get("outlineLevel");
						String taskReq = (String) newTaskValueMap.get("taskReq");

						//if taskUID is not found this is a special case where
						//the addedList contains details of project skip this value
						if(taskUID == null || taskUID.equals(""))
							continue;

						//[existing task--update structure]
						// cn - check
						if(taskId != null && !taskId.equals("") && oldTaskValueMap != null)
						{
							//taskLevelMap.put(outlineNumber, taskId);
							if (isProjectSpace(context,(String)oldTaskValueMap.get("taskType"))==true)
							{
								Map newSubprojectmap =(Map)newTaskValueMap.get("subprojectMap");
								Map oldSubprojectmap =(Map)oldTaskValueMap.get("subprojectMap");

								if (newSubprojectmap != null && oldSubprojectmap != null)
								{
									oldSubprojectmap.put("taskIndexMap", oldTaskIndexMap);
									newSubprojectmap.put("taskIndexMap", taskIndexMap);
									SynchronizeExistingProject(context, taskId,newSubprojectmap,oldSubprojectmap,tasks);
									Map taskIndexMap2 = (Map)newSubprojectmap.get("taskIndexMap");
									Map oldTaskIndexMap2 =(Map) oldSubprojectmap.get("taskIndexMap");
									taskIndexMap.putAll(taskIndexMap2);
									oldTaskIndexMap.putAll(oldTaskIndexMap2);
								}
								//get the releationship id
								subProject.setId(taskId);
								StringList busSelect = new StringList();
								busSelect.add("to[" + RELATIONSHIP_SUBTASK + "].id");
								Map taskInfo = subProject.getInfo(context, busSelect);
								String connectionId = (String) taskInfo.get("to[" + RELATIONSHIP_SUBTASK + "].id");
								DomainRelationship.disconnect(context, connectionId);

								if(! outlineNumber.equals(oldTaskValueMap.get("outlineNumber")))
								{
									//structure modified for an existing subproject
									if(outlineNumber.lastIndexOf(".") == -1)
									{
										//direct subtask of a project
										newParentId = strBusId;
									}
									else
									{
										levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
										newParentId = (String) taskLevelMap.get(levelId);
									}
									task.setId(strBusId);	
									task.reSequence(context, strBusId);

									task.setId(newParentId);		
									String []childIds = new String[1];
									childIds[0] = taskId;
									String nextTaskId = getNextTaskId( outlineNumber,NextOutlineMap, TaskLevelMap,TaskLevelMapOld,OIDMap, OIDMapOld);
									if (nextTaskId == null)
									{
										nextTaskId = newParentId;
									}
									task.addExisting(context, childIds , nextTaskId);
									
								}//if(! outlineNumber.equals(oldTaskValueMap.get("outlineNumber")))
								ProjectIds.add(taskId);
							}//if (isProjectSpace(context,(String)oldTaskValueMap.get("taskType"))==true)
							else
							{
								//get the releationship id
								task.setId(taskId);
									StringList busSelect = new StringList();
									busSelect.add("to[" + RELATIONSHIP_SUBTASK + "].id");			 
									Map taskInfo = task.getInfo(context, busSelect);																	  
									String connectionId = (String) taskInfo.get("to[" + RELATIONSHIP_SUBTASK + "].id");		 
									DomainRelationship.disconnect(context, connectionId);				
	
									//Circular dependency : case 1
									//before modifying the structure of the current task check if
									//currenttask has existing predecessors which on structure change
									//will result in being the current tasks parent
									//since a parent cannot have its childern has a predecessor
									//i.e taskId cannot not be a predecessor of newParentId			  
									MapList predList = task.getPredecessors(context, null, null, null);			 
									Iterator predListItr = predList.iterator();
	
									if (debug)
										System.out.println("Checking for Circular Dependency: case 1");
	
									while (predListItr.hasNext())
									{
										Map predecessorObj = (Map) predListItr.next();
										String predecessorId = (String) predecessorObj.get(task.SELECT_ID);
										ArrayList taskLevelKeys = new ArrayList(taskLevelMap.keySet());
										//BUG? []: changed from predecessorObj.keySet() to taskLevelMap.keySet()
										ArrayList taskLevelValues = new ArrayList(taskLevelMap.values());
	
										String predLevel = null;
										if(taskLevelValues.indexOf(predecessorId) != -1)
										{
											int predPos = taskLevelValues.indexOf(predecessorId);
											if(taskLevelKeys.size() > predPos)
											//BUG: []: changed from (taskLevelKeys.size() >= predPos) to (taskLevelKeys.size() > predPos)(zero based index)?
											{
												predLevel = (String) taskLevelKeys.get(predPos);
												if (predLevel != null &&  outlineNumber.indexOf(predLevel) != -1)
												{
													//removing dependency of a given object
													String predConnectionId = (String) predecessorObj.get(dependency.SELECT_ID);		  
													task.removePredecessor(context, predConnectionId);							 
												}
											}
										} // end if
									} // while (predListItr.hasNext())
									
									//if(!outlineNumber.equals(oldTaskValueMap.get("outlineNumber")))
									{
										//structure modified for an existing task	
										if(outlineNumber.lastIndexOf(".") == -1)
										{
											//direct subtask of a project
											newParentId = strBusId;
										}
										else
										{
											levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
											newParentId = (String) taskLevelMap.get(levelId);
											//Circular dependency : case 2
											//before changing the parent of the current task check if
											//currenttask is a predecessor of the parent task
											//since a parent cannot have its childern has a predecessor
											//i.e taskId cannot not be a predecessor of newParentId
											task.setId(newParentId);
											if(debug)
												System.out.println("Checking for Circular Dependency: case 2");
	
											MapList predecessorList = task.getPredecessors(context, null, null, null);			
											Iterator predecessorItr = predecessorList.iterator();
	
											while (predecessorItr.hasNext())
											{
												Map predecessorObj = (Map) predecessorItr.next();
												String predecessorId = (String) predecessorObj.get(task.SELECT_ID);
												if (predecessorId.equals(taskId))
												{
													String depConnectionId = (String) predecessorObj.get(dependency.SELECT_ID);	 
													task.removePredecessor(context, depConnectionId);					   
												}
											}
											task.setId(taskId);
										}						  
										task.setId(strBusId);	
										task.reSequence(context, strBusId);
	
										task.setId(newParentId);		
										String []childIds = new String[1];
										childIds[0] = taskId;
										String nextTaskId = getNextTaskId( outlineNumber,NextOutlineMap, TaskLevelMap,TaskLevelMapOld,OIDMap, OIDMapOld);
										if (nextTaskId == null)
										{
											nextTaskId = newParentId;
										}
										task.addExisting(context, childIds , nextTaskId);								
									}//if(! outlineNumber.equals(oldTaskValueMap.get("outlineNumber")))
							}//else
						}//if(taskId != null && !taskId.equals(""))
						else
						{
							Map subprojectmap =(Map)newTaskValueMap.get("subprojectMap");
							if (subprojectmap == null && isProjectSpace(context,taskType)==true)
							{
								mspiCreateProject(context,  subProject,  taskType,  taskName,  userVault) ;
								String subprojectId = subProject.getId();
								task.setId(subprojectId);
								
								taskLevelMap.put(outlineNumber, subprojectId);
								taskIndexMap.put(taskUID, subprojectId);
								
								subProject.setDescription(context, taskDesc);
								
								codeRegn = "set the attribute values into a map";
								Map attributeMap = new HashMap(5);
								attributeMap.put(ATTRIBUTE_TASK_CONSTRAINT_TYPE, spConstraintType);
								attributeMap.put(ATTRIBUTE_TASK_CONSTRAINT_DATE, spConstraintDate);
								attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_START_DATE, estStartDate);
								attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE, estFinishDate);
								if(percentComplete != null && !percentComplete.equals("0"))
									attributeMap.put(ATTRIBUTE_PERCENT_COMPLETE, percentComplete);
								if(estDuration != null && estDuration.length() > 0)
									attributeMap.put(ATTRIBUTE_TASK_ESTIMATED_DURATION, estDuration);

								codeRegn = "setting project attribute values ";
								subProject.setAttributeValues(context, attributeMap);	
								
								//String parentId;
								if(outlineNumber.lastIndexOf(".") == -1)
								{
									parentId = strBusId;
								}
								else
								{
									levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
									parentId = (String) taskLevelMap.get(levelId);
									parentTask.setId(parentId);
								}
								task.setId(parentId);		
								String []childIds = new String[1];
								childIds[0] = subprojectId;
								String nextTaskId = getNextTaskId( outlineNumber,NextOutlineMap, TaskLevelMap,TaskLevelMapOld,OIDMap, OIDMapOld);
								if (nextTaskId == null)
								{
									nextTaskId = parentId;
								}
								task.addExisting(context, childIds , nextTaskId);

								ProjectIds.add(subprojectId);
							}//if (isProjectSpace(context,taskType)==true)
							else
							{
								if (subprojectmap != null )
								{
									String subprojectId = SynchronizeNewProject(context, subprojectmap);
									Map  subprojectTaskIndexMap = (Map) subprojectmap.get("taskIndexMap");
									taskIndexMap.putAll(subprojectTaskIndexMap);

									taskLevelMap.put(outlineNumber, subprojectId);
									taskIndexMap.put(taskUID, subprojectId);

									//String parentId;
									if(outlineNumber.lastIndexOf(".") == -1)
									{
										parentId = strBusId;
									}
									else
									{
										levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
										parentId = (String) taskLevelMap.get(levelId);
									}		

									String nextTaskId = getNextTaskId( outlineNumber,NextOutlineMap, TaskLevelMap,TaskLevelMapOld,OIDMap, OIDMapOld);
									task.setId(parentId);		
									String []childIds = new String[1];
									childIds[0] = subprojectId;
									task.addExisting(context, childIds , nextTaskId);
						
									ProjectIds.add(subprojectId);
								}
								else
								{
									task = (Task) DomainObject.newInstance(m_ctxUserContext, DomainConstants.TYPE_TASK, DomainConstants.PROGRAM);//IR-185394V6R2013x
									String nextTaskId= getNextTaskId( outlineNumber,NextOutlineMap, TaskLevelMap,TaskLevelMapOld,OIDMap, OIDMapOld);;
																					
									String taskParentId = null;
									codeRegn = "setting strBusId " + strBusId;
									project.setId(strBusId);
									codeRegn = "project set ";

									if(outlineNumber.lastIndexOf(".") == -1)
									{
										codeRegn = "creating task under project ";
										taskParentId = strBusId;
										if (debug)
										{
											ifTaskCreate = System.currentTimeMillis();
										}
										boolean retValue = mspiCreateTask(context, task, taskType, taskName, project, true,nextTaskId);
										if (debug)
										{
											ifTaskCreated = System.currentTimeMillis();
											timeToCreate = (float)ifTaskCreated - ifTaskCreate/1000;
										}
									}//if(outlineNumber.lastIndexOf(".") == -1)
									else
									{
										levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
										parentId = (String) taskLevelMap.get(levelId);
										parentTask.setId(parentId);
										codeRegn = "creating hierarchial task " + strBusId;
										task.clear();
										taskParentId = parentId;
										if (debug)
										{
											System.out.println("Task creation started - else");
										}
										if(nextTaskId == null)
											nextTaskId = taskParentId;
										boolean retValue = mspiCreateTask(context, task, taskType, taskName, parentTask, false,nextTaskId);
										if (debug)
										{
											System.out.println("Task creation completed");
										}
									}//else
									task.setDescription(context, taskDesc);
									if(taskState != null && !taskState.equals(""))
										task.setState(context, taskState);
									String newTaskId = task.getId();
									taskLevelMap.put(outlineNumber, newTaskId);
									taskIndexMap.put(taskUID, newTaskId);

									//newattrib-
									//set the atttibutes of newly created tasks
									//codeRegn = "set the attribute values into a map";
									Map attributeMap = new HashMap(5);

									Date tempStartDate = mspDate.parse(estStartDate);
									task.updateStartDate(context, tempStartDate, true);

									Date tempFinishDate = mspDate.parse(estFinishDate);
									task.updateFinishDate(context, tempFinishDate, true);

									if(percentComplete != null && !percentComplete.equals("0"))
										attributeMap.put(ATTRIBUTE_PERCENT_COMPLETE, percentComplete);

									if(estDuration != null && estDuration.length() > 0)
									{
										task.updateDuration(context, estDuration);
									}
									
									if(actStartDate != null && actStartDate.length() > 0)
									  {						
										if(actStartDate != "")
										{									  
										  HashMap programMap = new HashMap();
										  HashMap paramMap = new HashMap();	
										  HashMap requestLocaleMap = new HashMap();
										  String[] arrJPOArguments = new String[1];
											
										  Locale locale = new Locale("en_US");
										  Date date1 = eMatrixDateFormat.getJavaDate(actStartDate);
										  int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
										  java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
										  actStartDate  = format.format(date1);
											
										  programMap.put("New Value", actStartDate);
										  programMap.put("Old Value", null);
										  programMap.put("objectId", newTaskId);	
										  paramMap.put("paramMap", programMap);
											
										  requestLocaleMap.put("locale", locale);
										  paramMap.put("requestMap", requestLocaleMap);
											
										  arrJPOArguments = JPO.packArgs(paramMap);
										  JPO.invoke(context,"emxTaskBase",null,"updateTaskActualStartDate",arrJPOArguments);
									  }
										else
											task.setAttributeValue(context, task.ATTRIBUTE_TASK_ACTUAL_START_DATE, "");
									}
									
									  if(actFinishDate != null && actFinishDate.length() > 0)
									  {						
										  if(actFinishDate != "")
										  {
										  HashMap programMap = new HashMap();
										  HashMap paramMap = new HashMap();	
										  HashMap requestLocaleMap = new HashMap();
										  String[] arrJPOArguments = new String[1];
											  
										  Locale locale = new Locale("en_US");
										  Date date1 = eMatrixDateFormat.getJavaDate(actFinishDate);
										  int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
										  java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
										  actFinishDate  = format.format(date1);
												
										  programMap.put("New Value", actFinishDate);
										  programMap.put("Old Value", null);
										  programMap.put("objectId", newTaskId);	
										  paramMap.put("paramMap", programMap);
											  
										  requestLocaleMap.put("locale", locale);
										  paramMap.put("requestMap", requestLocaleMap);
												
										  arrJPOArguments = JPO.packArgs(paramMap);
										  JPO.invoke(context,"emxTaskBase",null,"updateTaskActualFinishDate",arrJPOArguments);
									  }	
										  else
											  task.setAttributeValue(context, task.ATTRIBUTE_TASK_ACTUAL_FINISH_DATE, "");
									  }	
									if(taskReq != null && !taskReq.equals(""))
										attributeMap.put(task.ATTRIBUTE_TASK_REQUIREMENT, taskReq);

									//[2011x Start: Adding Constraint Type and Date for newly added task in subproject]
									attributeMap.put(task.ATTRIBUTE_TASK_CONSTRAINT_TYPE, spConstraintType);
									if(spConstraintDate!= null && spConstraintDate.length() > 0)
									{
										Date tempTaskConstraintDate= mspDate.parse(spConstraintDate);
										attributeMap.put(ATTRIBUTE_TASK_CONSTRAINT_DATE, spConstraintDate);
									}
									else
										attributeMap.put(ATTRIBUTE_TASK_CONSTRAINT_DATE, null);										
									//[2011x End: Adding Constraint Type and Date for newly added task in subproject]
								
									codeRegn = "setting task attribute ";
									task.setAttributeValues(context, attributeMap);
									//set the owner
									task.setOwner(context, owner);
								}
							}
						}

						if (debug)
						{
							iterationEndTime = System.currentTimeMillis();
							long iterationTime = iterationEndTime - iterationStartTime;

							//System.out.println("TOTAL TIME in Iteration for new tasks (in Secs): "+(float)iterationTime/1000);
						}
					}//while (addedListItr.hasNext())
				} // if(addedList != null)

				// rpandia - what is this for ? What's being deleted here
				// This is getting executed every time, it should get executed
				// only if a row of data is deleted from MSP
				// Changing the API from "removedList != null" to "removedList.size() != 0"
				// which takes care of the condition

				// cn - check
				//System.out.println("removedList: "+removedList);
				if(removedList.size() != 0)
				//if(removedList != null)
				{
					codeRegn = "Merge - Deleting the task";
					if (debug)
					{
						System.out.println(codeRegn);
					}
					deleteTasks(context, removedList, taskLevelMap, strBusId);
					if (debug)
					{
						System.out.println("Done deleting tasks");
					}
				}
				else
				{
					if (debug)
					{
						System.out.println("Nothing to Delete");
					}
				}

				//check if the task attributes are modified and set them
				// cn - check

				codeRegn = "Merge - Modify the task attributes";
				Map oldMap = new HashMap();
				Map mspMap = new HashMap();
				ListIterator taskDataMapListItr = taskDataMapList.listIterator();

				//get to the end of the map list
				//the maplist is top-down wrt to the project tasks
				//when we try to save the attributes of a task if we set the
				//parents percentage as 100% PC's trigger will block the event
				//(since the child is still 0%) so go to the end of the list and
				//start the attribute setting from the child then going updwards
				while (taskDataMapListItr.hasNext())
				{
					mspMap = (Map) taskDataMapListItr.next();
				}

				int taskCounter = taskDataMapList.size();

				// rpandia
				//tasks = (Map) new HashMap();
				while (taskDataMapListItr.hasPrevious())
				{
					mspMap = (Map) taskDataMapListItr.previous();
					if(taskCounter == 1)
					{
						continue;
					}
					taskCounter--;
					String mspTaskId = (String) mspMap.get("taskId");
					if(mspTaskId != null && !mspTaskId.equals(""))
					{		
						ListIterator oldTaskDataMapListItr = oldTaskDataMapList.listIterator();
						while (oldTaskDataMapListItr.hasNext())
						{
							oldMap = (Map) oldTaskDataMapListItr.next();
							String oldTaskId = (String) oldMap.get("taskId");
							if(oldTaskId != null && oldTaskId.equals(mspTaskId))
							{
								break;
							}
						}
						//need to check whether this shud be there for sub project also
						if (ProjectIds.indexOf(mspTaskId) >= 0 )
						{
							continue;
						}

						Map newSubprojectmap =(Map)mspMap.get("subprojectMap");
						Map oldSubprojectmap =(Map)oldMap.get("subprojectMap");

						if (newSubprojectmap != null && oldSubprojectmap != null)
						{
							oldSubprojectmap.put("taskIndexMap", oldTaskIndexMap);
							newSubprojectmap.put("taskIndexMap", taskIndexMap);
							SynchronizeExistingProject(context, mspTaskId,newSubprojectmap,oldSubprojectmap,tasks);
							Map taskIndexMap2 = (Map)newSubprojectmap.get("taskIndexMap");
							Map oldTaskIndexMap2 =(Map) oldSubprojectmap.get("taskIndexMap");
							taskIndexMap.putAll(taskIndexMap2);
							oldTaskIndexMap.putAll(oldTaskIndexMap2);
							continue;
						}
					}//if(mspTaskId != null && !mspTaskId.equals(""))
				}//while (taskDataMapListItr.hasPrevious())
						
				//check if the task attributes are modified and set them
				// cn - check

				codeRegn = "Merge - Modify the task attributes";

				//get to the end of the map list
				//the maplist is top-down wrt to the project tasks
				//when we try to save the attributes of a task if we set the
				//parents percentage as 100% PC's trigger will block the event
				//(since the child is still 0%) so go to the end of the list and
				//start the attribute setting from the child then going updwards
				taskDataMapListItr = taskDataMapList.listIterator();
				while (taskDataMapListItr.hasNext())
				{
					mspMap = (Map) taskDataMapListItr.next();
				}

				taskCounter = taskDataMapList.size();

				// rpandia
				//tasks = (Map) new HashMap();

				while (taskDataMapListItr.hasPrevious())
				{
					mspMap = (Map) taskDataMapListItr.previous();
					if(taskCounter == 1)
					{
						continue;
					}
					taskCounter--;
					String mspTaskId = (String) mspMap.get("taskId");
					Iterator refRemovedItr = refRemovedList.iterator();
					int i = 0;
					boolean nodeInRemovedList = false;
					while(refRemovedItr.hasNext())
					{
						Map refRemovedMap = (Map) refRemovedItr.next();
						if(mspTaskId.equals(refRemovedMap.get("taskId")))
						{
							nodeInRemovedList = true;
							break;
						}
						i++;
					}
					if(nodeInRemovedList)
					{
						continue;
					}

					if(mspTaskId != null && !mspTaskId.equals(""))
					{					
						ListIterator oldTaskDataMapListItr = oldTaskDataMapList.listIterator();
						while (oldTaskDataMapListItr.hasNext())
						{
							oldMap = (Map) oldTaskDataMapListItr.next();
							String oldTaskId = (String) oldMap.get("taskId");
							if(oldTaskId != null && oldTaskId.equals(mspTaskId))
							{
								break;
							}
						}
						//need to check whether this shud be there for sub project also
						if (ProjectIds.indexOf(mspTaskId) >= 0 )
						{
							continue;
						}
						//task data when the project was downloaded
						String oldTaskName = (String)oldMap.get("taskName");
						String oldTaskDesc = (String)oldMap.get("description");
						String oldTaskOwner = (String)oldMap.get("owner");
						String oldTaskType = (String)oldMap.get("taskType");
						String oldTaskCurrent = (String)oldMap.get("taskState");
						String oldTaskStart = (String)oldMap.get("estStartDate");
						String oldTaskFinish = (String)oldMap.get("estFinishDate");
						String oldTaskDuration = (String)oldMap.get("estDuration");
						String oldActTaskStart = "";
						String oldActTaskFinish = "";
						String mspActTaskStart = "";
						String mspActTaskFinish = "";
						boolean bActTaskStartChanged = false;
						boolean bActTaskFinishChanged = false;
						if(sProjectSchduleBasedOn.equals("Actual"))
						{
							if(oldMap.get("actStartDate") != null)
								oldActTaskStart = (String)oldMap.get("actStartDate");
							if(oldMap.get("actFinishDate") != null)
							    oldActTaskFinish = (String)oldMap.get("actFinishDate");
						}						
						String oldTaskConstraintType= (String)oldMap.get("constraintType");
						String oldTaskConstraintDate= (String)oldMap.get("constraintDate");
						String oldTaskPercent = (String)oldMap.get("percentComplete");
						String oldTaskRequirement = (String)oldMap.get("taskReq");
						//System.out.println("oldMap: "+oldMap);

						//task data, as obtained from MS Project
						String mspTaskName = (String)mspMap.get("taskName");
						String mspTaskDesc = (String)mspMap.get("description");
						String mspTaskOwner = (String)mspMap.get("owner");
						String mspTaskType = (String)mspMap.get("taskType");
						String mspTaskCurrent = (String)mspMap.get("taskState");
						String mspTaskPercent = (String)mspMap.get("percentComplete");
						String mspTaskStart = (String)mspMap.get("estStartDate");
						String mspTaskFinish = (String)mspMap.get("estFinishDate");
						String mspTaskDuration = (String)mspMap.get("estDuration");
						if(sProjectSchduleBasedOn.equals("Actual"))
						{
							if(mspMap.get("actStartDate") != null)
								mspActTaskStart = (String)mspMap.get("actStartDate");
							if(mspMap.get("actStartDate") != null)
								mspActTaskFinish = (String)mspMap.get("actFinishDate");
						}
						String mspTaskConstraintType= (String)mspMap.get("constraintType");
						String mspTaskConstraintDate= (String)mspMap.get("constraintDate");
						String mspTaskRequirement = (String)mspMap.get("taskReq");

						boolean bTaskNameChanged = !(oldTaskName.equals(mspTaskName));
						boolean bTaskDescChanged =  !(oldTaskDesc.equals(mspTaskDesc));
						boolean bTaskOwnerChanged = !(oldTaskOwner.equals(mspTaskOwner));
						boolean bTaskStateChanged =  !(oldTaskCurrent.equals(mspTaskCurrent));
						boolean bTaskPercentChanged = !(oldTaskPercent.equals(mspTaskPercent));
						boolean bTaskStartChanged = !(oldTaskStart.equals(mspTaskStart));
						if(sProjectSchduleBasedOn.equals("Actual"))
						{
							bActTaskStartChanged = !(oldActTaskStart.equals(mspActTaskStart));
							bActTaskFinishChanged = !(oldActTaskFinish.equals(mspActTaskFinish));
						}						
						boolean bTaskReqChanged = !(oldTaskRequirement.equals(mspTaskRequirement)) ;
						boolean bTaskDurationChanged = !(oldTaskDuration.equals(mspTaskDuration));
						boolean bTaskConstraintTypeChanged = !(oldTaskConstraintType.equals(mspTaskConstraintType));
						boolean bTaskConstraintDateChanged = !(oldTaskConstraintDate.equals(mspTaskConstraintDate));
						
						if(bTaskNameChanged || bTaskDescChanged || bTaskOwnerChanged || bTaskStateChanged ||
								bTaskPercentChanged || bTaskStartChanged || bTaskDurationChanged ||
								bTaskReqChanged || bTaskConstraintTypeChanged || bTaskConstraintDateChanged ||
								bActTaskStartChanged || bActTaskFinishChanged)
						{
							StringList taskSelects = new StringList(10);
							taskSelects.add(task.SELECT_NAME);
							taskSelects.add(task.SELECT_OWNER);
							taskSelects.add(task.SELECT_TYPE);
							taskSelects.add(task.SELECT_DESCRIPTION);
							taskSelects.add(task.SELECT_CURRENT);
							taskSelects.add(task.SELECT_PERCENT_COMPLETE);
							taskSelects.add(task.SELECT_TASK_ESTIMATED_DURATION);
							taskSelects.add(task.SELECT_TASK_ESTIMATED_START_DATE);
							taskSelects.add(task.SELECT_TASK_ESTIMATED_FINISH_DATE);
							taskSelects.add(task.SELECT_TASK_ACTUAL_START_DATE);
							taskSelects.add(task.SELECT_TASK_ACTUAL_FINISH_DATE);	

							taskSelects.add(task.SELECT_TASK_CONSTRAINT_TYPE);
							taskSelects.add(task.SELECT_TASK_CONSTRAINT_DATE);

							taskSelects.add(task.SELECT_TASK_REQUIREMENT);
							taskSelects.add(task.SELECT_HAS_SUBTASK);

							task.setId(mspTaskId);
							task.open(context);

							Map pcTaskMap = task.getInfo(context, taskSelects);  //WHAT IF THIS IS A PROJECT....?
							String pcTaskName = (String)pcTaskMap.get(task.SELECT_NAME);
							String pcTaskDesc = (String)pcTaskMap.get(task.SELECT_DESCRIPTION);
							String pcTaskOwner = (String)pcTaskMap.get(task.SELECT_OWNER);
							String pcTaskType = (String)pcTaskMap.get(task.SELECT_TYPE);
							String pcTaskCurrent = (String)pcTaskMap.get(task.SELECT_CURRENT);
							String pcTaskPercent = (String)pcTaskMap.get(task.SELECT_PERCENT_COMPLETE);
							String pcTaskStart = (String)pcTaskMap.get(task.SELECT_TASK_ESTIMATED_START_DATE);
							String pcTaskFinish = (String)pcTaskMap.get(task.SELECT_TASK_ESTIMATED_FINISH_DATE);
							String pcTaskDuration = (String)pcTaskMap.get(task.SELECT_TASK_ESTIMATED_DURATION);
							String pcActTaskStart = (String)pcTaskMap.get(task.SELECT_TASK_ACTUAL_START_DATE);
							String pcActTaskFinish = (String)pcTaskMap.get(task.SELECT_TASK_ACTUAL_FINISH_DATE);
							String pcTaskRequirement = (String)pcTaskMap.get(task.SELECT_TASK_REQUIREMENT);
							String pcTaskHasSubTask = (String)pcTaskMap.get(task.SELECT_HAS_SUBTASK);

							String pcTaskConstraintType= (String)pcTaskMap.get(task.SELECT_TASK_CONSTRAINT_TYPE);
							String pcTaskConstraintDate= (String)pcTaskMap.get(task.SELECT_TASK_CONSTRAINT_DATE);

							//attrib-
							//check if there is any modification to task name, description
							//start date, end date, duration, %age completion etc and update accordingly -
							//case 1: only the MS Project user modified the task data
							//case 2: only the PC user modified the task data
							//case 3: the PC user as well as the MS Project user
							//         modified the task data
							//
							//NOTE:
							//for case 2 and case 3 do nothing PC is the master
							Map attributeMap = new HashMap();

							// rpandia: Do we need to check and then set ?
							// Don't know if there's a need to compare here
							// Shouldn't we blindly update the MSP data here
							// There should be no need to compare with PCdata and the Downloaded data
							// All this is happening in the big while loop, this is slowing it down

							if(bTaskNameChanged)
							if(pcTaskName.equals(oldTaskName))
							{
								task.setName(context, mspTaskName);
							}
							if(bTaskDescChanged)
							if(pcTaskDesc.equals(oldTaskDesc))
							{
								task.setDescription(context, mspTaskDesc);
							}
							if(bTaskOwnerChanged)
								if(pcTaskOwner.equals(oldTaskOwner))
								{
									task.setOwner(context, mspTaskOwner);
								}
							if(bTaskStateChanged)
								if(pcTaskCurrent.equals(oldTaskCurrent) )
								{
									task.setState(context, mspTaskCurrent);
								}
							if(bTaskPercentChanged)
							{
								Integer newPercent = new Integer(Double.valueOf(pcTaskPercent).intValue());
								pcTaskPercent = Integer.toString(newPercent.intValue());
								if(pcTaskPercent.equals(oldTaskPercent) )
								{
									attributeMap.put(task.ATTRIBUTE_PERCENT_COMPLETE, mspTaskPercent);
								}
							}

							//BUG
							//java.text.SimpleDateFormat mspDate = new java.text.SimpleDateFormat("MM/dd/yyyy");
							codeRegn = "in the modify task attribute method";
							if(bTaskStartChanged)
							{
								///////////////////////////////////10.8.0.2.LA /////////////////////////////////////////////	  
								//Date startDate = mspDate.parse(pcTaskStart);
								Date startDate = MATRIX_DATE_FORMAT.parse(pcTaskStart);   						   
								pcTaskStart = MATRIX_DATE_FORMAT.format(startDate);
								// Compare the value of StartDate (from PMC) and the value in MSP (when it was retrieved from PMC)
								// with the old value that was in MSP to the changed value in MSP (if it was changed)
								// If there is a change in any of them, update the StartDate
								if(pcTaskStart.equals(oldTaskStart))
								{
									//attributeMap.put(task.ATTRIBUTE_TASK_ESTIMATED_START_DATE, mspTaskStart);
									
									///////////////////////////////////10.8.0.2.LA /////////////////////////////////////////////
									Date tempDate = MATRIX_DATE_FORMAT.parse(mspTaskStart,new java.text.ParsePosition(0));
									//Date tempDate = mspDate.parse(mspTaskStart);
									///////////////////////////////////10.8.0.2.LA /////////////////////////////////////////////
									
									addUpdate(tasks, task.getId(), task.ATTRIBUTE_TASK_ESTIMATED_START_DATE, tempDate);
									//task.updateStartDate(context, tempDate, true);
									//addUpdate(tasks, task.getId(), "startDate", tempDate);
								}
							}

							Date finishDate = mspDate.parse(pcTaskFinish);
							pcTaskFinish = MATRIX_DATE_FORMAT.format(finishDate);

							if(bTaskDurationChanged)
							{
								//Fraction Data Support
								Double newDuration = new Double(Double.valueOf(pcTaskDuration).doubleValue());
								pcTaskDuration = Double.toString(newDuration.doubleValue());
								//Integer newDuration = new Integer(Double.valueOf(pcTaskDuration).intValue());
								//pcTaskDuration = Integer.toString(newDuration.intValue());

								if(pcTaskDuration.equals(oldTaskDuration) )
								{
									// Incident 288584: Because of the behavior where a task's duration is updated,
									// the dependent's task's duration is also updated.  So, we will forcibly
									// update the duration for all the tasks, except for the summary task.

									if(((String)pcTaskMap.get(task.SELECT_HAS_SUBTASK)).equalsIgnoreCase("false"))
									{
										//attributeMap.put(task.ATTRIBUTE_TASK_ESTIMATED_DURATION, mspTaskDuration);
										Integer tempDuration2 = new Integer(Double.valueOf(mspTaskDuration).intValue());
										Long tempDuration = new Long(Double.valueOf(mspTaskDuration).intValue());

										//addUpdate(tasks, task.getId(), task.ATTRIBUTE_TASK_ESTIMATED_DURATION, tempDuration);
										//Fraction Data Support
										task.updateDuration(context, mspTaskDuration);
										//task.updateDuration(context, tempDuration2, true);
										//addUpdate(tasks, task.getId(), "duration", tempDuration);
									}
								}
							}
							
							if(bActTaskStartChanged)
							{
								if(pcActTaskStart != null && !pcActTaskStart.equals(""))
								{
									Date startDate = mspDate.parse(pcActTaskStart);
									pcActTaskStart = MATRIX_DATE_FORMAT.format(startDate);
								}
								  
							    if(pcActTaskStart.equals(oldActTaskStart))
							    {
							    	if(mspActTaskStart != "")
									{							    		
								    	HashMap programMap = new HashMap();
								    	HashMap paramMap = new HashMap();
								    	HashMap requestLocaleMap = new HashMap();
								    	String[] arrJPOArguments = new String[1];
									    	
										Locale locale = new Locale("en_US");
										Date date1 = eMatrixDateFormat.getJavaDate(mspActTaskStart);
										int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
										java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
										mspActTaskStart  = format.format(date1);
										  
								    	programMap.put("New Value", mspActTaskStart); 	
								    	programMap.put("Old Value", pcActTaskStart); 	
								    	programMap.put("objectId", mspTaskId);	 
								    	paramMap.put("paramMap", programMap);
								    	
										requestLocaleMap.put("locale", locale);
										paramMap.put("requestMap", requestLocaleMap);
								    	
								    	arrJPOArguments = JPO.packArgs(paramMap);
								    	JPO.invoke(context,"emxTaskBase",null,"updateTaskActualStartDate",arrJPOArguments);						      
									}
							    	else
							    		task.setAttributeValue(context, task.ATTRIBUTE_TASK_ACTUAL_START_DATE, "");
							    }
							}
							  
							if(bActTaskFinishChanged)
							{
								if(pcActTaskFinish != null && !pcActTaskFinish.equals(""))
								{
									Date FinishDate = mspDate.parse(pcActTaskFinish);
									pcActTaskFinish = MATRIX_DATE_FORMAT.format(FinishDate);
								}
								  
								if(pcActTaskFinish.equals(oldActTaskFinish))
								{
									if(mspActTaskFinish != "")
									{																			  
										HashMap programMap = new HashMap();
										HashMap requestLocaleMap = new HashMap();
										HashMap paramMap = new HashMap();
										String[] arrJPOArguments = new String[1];
											
										Locale locale = new Locale("en_US");
										Date date1 = eMatrixDateFormat.getJavaDate(mspActTaskFinish);
										int iDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
										java.text.DateFormat format = DateFormat.getDateTimeInstance(iDateFormat, iDateFormat, Locale.US);
										mspActTaskFinish  = format.format(date1);
											  
										programMap.put("New Value", mspActTaskFinish); 	
										programMap.put("Old Value", pcActTaskFinish); 	
										programMap.put("objectId", mspTaskId);	 
										paramMap.put("paramMap", programMap);
											
										requestLocaleMap.put("locale", locale);
										paramMap.put("requestMap", requestLocaleMap);
											
										arrJPOArguments = JPO.packArgs(paramMap);
										JPO.invoke(context,"emxTaskBase",null,"updateTaskActualFinishDate",arrJPOArguments);									  								      
									}
									else
										task.setAttributeValue(context, task.ATTRIBUTE_TASK_ACTUAL_FINISH_DATE, "");
								}
							}
							
							if(bTaskReqChanged)
							{
								if(pcTaskRequirement.equals(oldTaskRequirement))
								{
									attributeMap.put(task.ATTRIBUTE_TASK_REQUIREMENT, mspTaskRequirement);
								}
							}
							
							 //[2011x Start: Set Subproject's Modified Value of ConstraintType and ConstraintDate coming from mspMap]
							  if(bTaskConstraintTypeChanged)
								  if(pcTaskConstraintType.equals(oldTaskConstraintType))
									  attributeMap.put(task.ATTRIBUTE_TASK_CONSTRAINT_TYPE,mspTaskConstraintType);
							  //Any modifications in Task Start or Finsh dates will also modify Task Constraint type(MS Project Behaviour)
							  // Therefore for such cases, this else part is written
							  else
								  if(pcTaskConstraintType.equals(oldTaskConstraintType))
									  attributeMap.put(task.ATTRIBUTE_TASK_CONSTRAINT_TYPE,oldTaskConstraintType);  

							  if(bTaskConstraintDateChanged)
							  {
								  if(pcTaskConstraintDate!= null && pcTaskConstraintDate.length() > 0)
								  {
									  Date parsedConstraintDate = mspDate.parse(pcTaskConstraintDate);
									  pcTaskConstraintDate= MATRIX_DATE_FORMAT.format(parsedConstraintDate );

									  if(pcTaskConstraintDate.equals(oldTaskConstraintDate))
										  attributeMap.put(task.ATTRIBUTE_TASK_CONSTRAINT_DATE,mspTaskConstraintDate);
								  }	  
								  else
									  if(oldTaskConstraintDate==null || oldTaskConstraintDate.equals(""))
										  attributeMap.put(task.ATTRIBUTE_TASK_CONSTRAINT_DATE,mspTaskConstraintDate);
							  }
							  //[2011x End: Set Subproject's Modified Value of ConstraintType and ConstraintDate coming from mspMap]
							
							if(attributeMap != null && attributeMap.size()>0  )
							{
								task.setAttributeValues(context, attributeMap);
							}
						}
					}//if(mspTaskId != null && !mspTaskId.equals(""))
				}//while (taskDataMapListItr.hasPrevious())

				// public static java.lang.String updateDates(matrix.db.Context context,
				//                           java.util.Map newObjectDates, boolean updateDB, boolean frmMSP)
				//String message = task.updateDates(context, tasks, true, true);

				//Pred-

				//update dependencies

				// rpandia: We should check here for the size of the maplist
				//if(assigneeMapList.size() != 0 || oldAssigneeMapList.size() != 0)
				if(assigneeMapList != null || oldAssigneeMapList != null)
				{
					//add new assignee or update existing assignees
					if (debug)
					{
						System.out.println("add new assignee or update existing assignees");
					}
					addAssignees(context, assigneeMapList, resourceIndexMap, taskIndexMap, oldAssigneeMapList, oldResourceIndexMap, oldTaskIndexMap, taskUnitMap);
				}
				else
				{
					if (debug)
						System.out.println("No need to call addAssignees");
				}
			}//if (oldStructMapList.size() > 0 || newStructMapList.size() > 0)
			codeRegn = "Done with merge";
			if (debug)
				System.out.println(codeRegn);
			//Get the latest data for the current project and display in MSP
			//by reusing findForCheckOut function
			//
			//Generate response such that to get the latest using findForCheckOut
			Element elFFCRoot = new Element("transaction");
			elFFCRoot.setAttribute("focusbrowser", "7");
			elFFCRoot.setAttribute("loglevel", "true");
			elFFCRoot.setAttribute("cname", "findforcheckout");
			elFFCRoot.setAttribute("tid", "2");
			elFFCRoot.setAttribute("type", "command");
			elFFCRoot.setAttribute("mpecver", "1.0.0.0");
			elFFCRoot.setAttribute("MSP", "MSP2000");

			// now create placeholder for response
			Element elResponseArgumentsNode = new Element("arguments");
			elFFCRoot.addContent(elResponseArgumentsNode);

			// create the busid argument placeholder
			Element elBusIdArgument = new Element("argument");
			elBusIdArgument.setAttribute("name", "busid");
			elBusIdArgument.setText(strBusId);
			elResponseArgumentsNode.addContent(elBusIdArgument);

			//Doubt
			// create the edit status argument placeholder
			// on Merge to ematrix the edit flag will be reset to false
			// the user will have to explicitly "Launch the project in Edit mode"
			// inorder to make any further changes
			Element elEditStatusArgument = new Element("argument");
			elEditStatusArgument.setAttribute("name", "foredit");
			elEditStatusArgument.setText("true");
			elResponseArgumentsNode.addContent(elEditStatusArgument);

			//use a temp name which is consistent with the way the servlet
			//names the file such that dumpTransactionXMLForServlet can be reused.
			String tempFileName = "findForCheckOut_1_2_3";
			//String fileName = dumpTransactionXMLForServlet (context, gco, tempFileName, elFFCRoot);
			String fileName = dumpTransactionXMLForServlet (context, tempFileName, elFFCRoot);
			if (debug)
			{
				System.out.println("done dumping the transaction xml file");
			}
			String[] newArgs = new String[1];
			newArgs[0] = fileName;

			codeRegn = "Commiting transaction";
			if (debug)
			{
				System.out.println(codeRegn);
				mergeEndTime = System.currentTimeMillis();
				long totalMergeTime = mergeEndTime - mergeStartTime;

				long afterDeleteTotalTime = mergeEndTime - afterDeleteStartTime;

				System.out.println("TOTAL MERGE TIME (in Secs): "+(float)totalMergeTime/1000);
			}

			mergeEndTime = System.currentTimeMillis();
			long totalMergeTime = mergeEndTime - mergeStartTime;
			//get the latest for this current project and display it in MSP
			if(debug)
				System.out.println("==========================END   SynchronizeExistingProject============================================");

			return "";
		}
		catch(Exception e)
		{
			ContextUtil.abortTransaction(context);
			if (e.toString().indexOf("well-formed character") != -1)
			{
				String wellFormedMessage = "One of the tasks in the project contains characters that is not supported, Check the task names and the notes. \n";
				throw new MatrixException(wellFormedMessage + e.getMessage());
			}
			else
			{
				throw new MatrixException(e.getMessage());
			}
		}
	}

	/**
	 * Gets the value of a named attribute of the GCO Business Object.
	 *
	 * @param gco Instance of GlobalConfigObject class which represents the GCO Business Object
	 * @param attribName String representing the named attribute
	 */

	/*
	//Following code is not required now since this JPO now reads the values from the MSOI GCO
	private void getAttribute(Context context, IntegrationGlobalConfigObject gco, String attribName) throws MatrixException
	{
		//String mqlCmdString = "print bus \"MSP Integration Global Configuration\" \"mspIntegrationGCO\" \"0.0.0.a\" select attribute[" + attribName + "] dump;";
		String mqlCmdString = "print bus \""+_msoiGlobalConf.getTypeName()+"\" \""+_msoiGlobalConf.getName()+"\" \""+_msoiGlobalConf.getRev()+"\" select attribute[" + attribName + "] dump;";

		String attribValue = executeMQL(context, mqlCmdString);
		gco.setIntegrationSetting(attribName, attribValue);
	}
	*/

	/*
	* JPO Entry point from servlet
	*/
	public String dumpTransactionXMLForJPO(Context context, String[] args) throws Exception
	{
		///////////////////////////////////10.8.0.2.LA /////////////////////////////////////////////
		eMatrixDateFormat.setEMatrixDateFormat();
		java.text.SimpleDateFormat MATRIX_DATE_FORMAT =
					new java.text.SimpleDateFormat(
					eMatrixDateFormat.getEMatrixDateFormat(),
					Locale.US);
		if(debug)
			System.out.println("=========================START    dumpTransactionXMLForJPO============================");
		String xmlOutPut = null;
		try
		{
			String language = args[0];
			userLanguage = language;

			String XMLInputString = args[1];
			//Read the charset value from the ief.properties file
			ResourceBundle mcadIntegrationBundle = ResourceBundle.getBundle("ief");
			String mcadCharset = "UTF8";
			try
			{
				mcadCharset	= mcadIntegrationBundle.getString("mcadIntegration.MCADCharset");
			}
			catch(MissingResourceException _ex)
			{
				mcadCharset = "UTF8";
			}

			IEFXmlNode pxTransactionXML = MCADXMLUtils.parse(XMLInputString, mcadCharset);
			String m_strRequestName = pxTransactionXML.getAttribute("cname");
			String m_strTransactionID = pxTransactionXML.getAttribute("tid");
			if (debug)
			{
				System.out.println("[dumpTransactionXMLForJPO] Request Name : " + m_strRequestName);
				System.out.println("[dumpTransactionXMLForJPO] Transaction  ID : " + m_strTransactionID);
			}
			String m_strLocalProjectFilePath = msoi_local_project_filepath; //(String) gco.getIntegrationSetting(IntegrationGlobalConfigObject.LOCAL_PROJECT_FILEPATH);//This line is not required now since this JPO now reads the values from the MSOI GCO
			String m_strLocalProjectFileName = m_strRequestName + "_" + m_strTransactionID;
			//Now save the file to use loadXMLSentFromServlet() with NO changes.
			String strTimePattern  = "HH.mm.ss_MM.dd.yy";
			SimpleDateFormat sdfFormatter = (SimpleDateFormat)DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.US);
			sdfFormatter.applyPattern(strTimePattern);
			String strTimeStamp = sdfFormatter.format(Calendar.getInstance().getTime());
			m_strLocalProjectFileName = m_strLocalProjectFileName  + "_" + strTimeStamp;
			if (debug)
				System.out.println("[dumpTransactionXMLForJPO] m_strLocalProjectFileName: " + m_strLocalProjectFileName);
			try
			{
				FileOutputStream fosTransactionOutput = new FileOutputStream(m_strLocalProjectFilePath + m_strLocalProjectFileName);
				pxTransactionXML.getXmlStream(fosTransactionOutput, mcadCharset);
				fosTransactionOutput.close();

				// Special handling for non-RIP mode: check in the XML into GCO

				// if(m_strOperateInRIPMode.equalsIgnoreCase(IntegrationAppletServletProtocol.FALSE))
				if("false".equalsIgnoreCase(msoi_in_rip_mode))
				{
					//BusinessObject boGCO = new BusinessObject("MSP Integration Global Configuration", "mspIntegrationGCO", "0.0.0.a", "eService Administration");
					String gcoName  = _msoiLocalConf.getGCONameForIntegration("MSProject");

					com.matrixone.MCADIntegration.server.beans.MCADMxUtil mxUtil = new com.matrixone.MCADIntegration.server.beans.MCADMxUtil(context, null,_GlobalCache);

					String GCOId = mxUtil.getGlobalConfigObjectID(context, "MCADInteg-GlobalConfig",gcoName);

					//BusinessObject boGCO = new BusinessObject(_msoiGlobalConf.getTypeName(), _msoiGlobalConf.getName(), _msoiGlobalConf.getRev(), "");
					BusinessObject boGCO = new BusinessObject(GCOId);

					if (debug)
						System.out.println("[XMLTransactionProcessor.dumpTransactionXMLForJPO] Creating Business Object.");
					
					boGCO.open(m_ctxUserContext);
					if (debug)
						System.out.println("[XMLTransactionProcessor.dumpTransactionXMLForJPO] Business Object opened.");
					
					String strHost = Framework.getPropertyValue("ematrix.server.host");
					boGCO.checkinFile(m_ctxUserContext, true, true, strHost, boGCO.getDefaultFormat(m_ctxUserContext), m_strLocalProjectFileName, m_strLocalProjectFilePath);
					if (debug)
						System.out.println("[XMLTransactionProcessor.dumpTransactionXMLForJPO] Checked in file to Business Object.");
					
					boGCO.close(m_ctxUserContext);
					File fLocalFile = new File(m_strLocalProjectFilePath + m_strLocalProjectFileName);
					fLocalFile.delete();
				}
			}
			catch(FileNotFoundException fnfe)
			{
				throw new Exception("File not found exception. Exception: " + fnfe.getMessage());
			}
			catch(IOException ioe)
			{
				throw new Exception("I/O exception while writing to file. Exception: " + ioe.toString());
			}
			if (debug)
				System.out.println("1: " + m_strLocalProjectFilePath + m_strLocalProjectFileName);
			
			//Now invoke the respective method
			if(m_strRequestName.equals("findforcheckout"))//Open a project
			{
				xmlOutPut = executeFindForCheckout(context, new String[] {m_strLocalProjectFileName});
				if (debug)
					System.out.println("[dumpTransactionXMLForJPO] findforcheckout : " + xmlOutPut);
			}
			else if(m_strRequestName.equals("synchronizetoemx"))//reload existing
			{
				xmlOutPut = executeSynchronizeToeMatrix(context, new String[] {m_strLocalProjectFileName});
			}
			else if(m_strRequestName.equals("synchronizefromemx")) // User clicks view/edit in MSP from WBS page of browser
			{
				xmlOutPut = executeSynchronizeFromeMatrix(context, new String[] {m_strLocalProjectFileName});
			}
			else if(m_strRequestName.equals("synchronizemerge")) //save
			{
				xmlOutPut = executeSynchronizeMerge(context, new String[] {m_strLocalProjectFileName});
			}
			else if(m_strRequestName.equals("validateresource"))
			{
				xmlOutPut = executeValidateResource(context, new String[] {m_strLocalProjectFileName});
			}
			else if(m_strRequestName.equals("isprojectexist"))//To chk project existence
			{				
				xmlOutPut = executeProjectExistenceCheck(context, new String[] {m_strLocalProjectFileName});				
			}
			if(xmlForClient != null)
				xmlOutPut = xmlForClient;

		}
		catch (Exception ee)
		{
			//ee.printStackTrace();
			System.out.println("[[EXCEPTION]] :  " + ee.getMessage());
			throw new Exception(ee.getMessage());
		}
		if(debug)
			System.out.println("=========================END    dumpTransactionXMLForJPO============================");
		return xmlOutPut;
	}

	/*private void dumpXMLResponse(String file) throws Exception
	{
		System.out.println("[getXMLResponse] 3: " + file);
		File fLocalFile = new File(file);

		com.matrixone.MSPIntegration.Utils.xml.tiny.ParsedXML pxTransactionXML = IntegrationXMLUtils.parse(new FileInputStream(fLocalFile));

		if(pxTransactionXML != null)
			xmlForClient = pxTransactionXML.getXmlString();
		else
			xmlForClient = "pxTransactionXML is null in getXMLResponse()";
	}*/

	/**
	* Get the Transaction XML file dumped by the servlet by directly reading
	* from a local file or by checking it out.
	*
	* @param gco Instance of GlobalConfigObject class which represents the GCO Business Object
	* @param transactionFileName Name of the transaction xml file
	*/

	//private Element loadXMLSentFromServlet (IntegrationGlobalConfigObject gco, String transactionFileName, Context context) throws Exception
	private Element loadXMLSentFromServlet (String transactionFileName, Context context) throws Exception
	{
		if(debug)
			System.out.println("==========================START   loadXMLSentFromServlet============================================");
		Element elCommandRoot = null;
		File tempFileForTransaction = null;
		//Following line is not required now since this JPO now reads the values from the MSOI GCO
		//    if((gco.getIntegrationSetting(IntegrationGlobalConfigObject.IN_RIP_MODE)).equalsIgnoreCase(IntegrationAppletServletProtocol.TRUE))
		if("true".equalsIgnoreCase(msoi_in_rip_mode))
		{
			//String localProjectFilePath = (String) gco.getIntegrationSetting(IntegrationGlobalConfigObject.LOCAL_PROJECT_FILEPATH);//This line is not required now since this JPO now reads the values from the MSOI GCO
			String localProjectFilePath = msoi_local_project_filepath;
			tempFileForTransaction = new File(localProjectFilePath + transactionFileName);
		}
		else
		{
			//BusinessObject bo = new BusinessObject("MSP Integration Global Configuration", "mspIntegrationGCO", "0.0.0.a", "eService Administration");
			String gcoName  = _msoiLocalConf.getGCONameForIntegration("MSProject");
			com.matrixone.MCADIntegration.server.beans.MCADMxUtil mxUtil = new com.matrixone.MCADIntegration.server.beans.MCADMxUtil(context, null,_GlobalCache);
			String GCOId = mxUtil.getGlobalConfigObjectID(context, "MCADInteg-GlobalConfig",gcoName);
			
			//BusinessObject boGCO = new BusinessObject(_msoiGlobalConf.getTypeName(), _msoiGlobalConf.getName(), _msoiGlobalConf.getRev(), "");
			BusinessObject bo = new BusinessObject(GCOId);
			//BusinessObject bo = new BusinessObject(_msoiGlobalConf.getTypeName(), _msoiGlobalConf.getName(), _msoiGlobalConf.getRev(), "");
			bo.open(context);
			tempFileForTransaction = File.createTempFile(transactionFileName, null);
			String fileFormat = bo.getDefaultFormat(context);
			FileOutputStream servletOutput = new FileOutputStream(tempFileForTransaction);
			bo.checkoutFile(context, false, fileFormat, transactionFileName, servletOutput);
			//String delCmd = "delete bus \"MSP Integration Global Configuration\" \"mspIntegrationGCO\" \"0.0.0.a\" format" + " \"" + fileFormat + "\" file \"" + transactionFileName + "\"";
			//String delCmd = "delete bus \""+_msoiGlobalConf.getTypeName()+"\" \""+_msoiGlobalConf.getName()+"\" \""+_msoiGlobalConf.getRev()+"\" format" + " \"" + fileFormat + "\" file \"" + transactionFileName + "\"";
			String delCmd = "delete bus \""+ GCOId+"\" format" + " \"" + fileFormat + "\" file \"" + transactionFileName + "\"";

			executeMQL(context, delCmd);
			bo.close(context);
			servletOutput.flush();
			servletOutput.close();
		}
		if (debug)
			System.out.println("Loading XML on the server, RIP Mode: "+ msoi_in_rip_mode);//IntegrationGlobalConfigObject.IN_RIP_MODE);

		FileInputStream fileInputStream = new FileInputStream(tempFileForTransaction);

		SAXBuilder dombCommandLoader = new SAXBuilder ();
		dombCommandLoader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		dombCommandLoader.setFeature("http://xml.org/sax/features/external-general-entities", false);
		dombCommandLoader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		
		Document dCommand = dombCommandLoader.build (fileInputStream);

		if (debug)
			System.out.println("loadXMLSentFromServlet(), Doc root created using file: "+tempFileForTransaction);
		elCommandRoot = dCommand.getRootElement();
		if (elCommandRoot == null)
		{
			throw new Exception("Root element in loadXMLSentFromServlet() was: "+dCommand);
		}

		//	if (!debug)
		//	{
		//		tempFileForTransaction.delete();
		//	}
		if (debug)
		{
			System.out.println("Not deleting "+transactionFileName);		
			System.out.println("==========================END   loadXMLSentFromServlet============================================");
		}
		return elCommandRoot;
	}

	/**
	* Dump the Transaction XML file for the servlet.
	*
	* @throws Exception if the operation fails
	*/
	//  private String dumpTransactionXMLForServlet (Context context, IntegrationGlobalConfigObject gco, String transactionFileName, Element elResponseRoot) throws Exception
	private String dumpTransactionXMLForServlet (Context context, String transactionFileName, Element elResponseRoot) throws Exception
	{
		if(debug)
		System.out.println("==========================START   dumpTransactionXMLForServlet============================================");
		String timePattern  = "HH.mm.ss_MM.dd.yy";
		SimpleDateFormat formatter = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.US);
		formatter.applyPattern(timePattern);
		String datetimeStamp = formatter.format(Calendar.getInstance().getTime());
		String localProjectFileName = transactionFileName;
		
		localProjectFileName = localProjectFileName.substring(0, localProjectFileName.lastIndexOf('_') - 1);// removing date stamp
		
		localProjectFileName = localProjectFileName.substring(0, localProjectFileName.lastIndexOf('_'));// removing time stamp
		
		localProjectFileName = localProjectFileName + "_" + datetimeStamp;// adding date-time stamp
		//String ripMode = (String) gco.getIntegrationSetting(IntegrationGlobalConfigObject.IN_RIP_MODE);//This line is not required now since this JPO now reads the values from the MSOI GCO
		File fLocalFile = null;
		String localProjectFilePath = null;
		if("true".equalsIgnoreCase(msoi_in_rip_mode))
		{
			//localProjectFilePath = (String) gco.getIntegrationSetting(IntegrationGlobalConfigObject.LOCAL_PROJECT_FILEPATH); //This line is not required now since this JPO now reads the values from the MSOI GCO
			localProjectFilePath = msoi_local_project_filepath;
			fLocalFile = new File(localProjectFilePath, localProjectFileName);
		}
		else
		{
			String mqlCmdString = "execute program eServicecommonUtFileGetTmpDir.tcl";
			localProjectFilePath = executeMQL(context, mqlCmdString);
			fLocalFile = new File(localProjectFilePath, localProjectFileName);
		}
		FileOutputStream fosOutput = new FileOutputStream(fLocalFile);
		XMLOutputter xmloResponse = new XMLOutputter();
		Document dResponse = new Document(elResponseRoot);
		dResponse.setDocType(null);
		xmloResponse.output(dResponse, fosOutput);
		fosOutput.flush();
		fosOutput.close();
		xmlForClient = xmloResponse.outputString(dResponse);  //dumpXMLResponse(localProjectFilePath+localProjectFileName);

		//if(ripMode.equalsIgnoreCase(IntegrationAppletServletProtocol.FALSE))
		if("false".equalsIgnoreCase(msoi_in_rip_mode))
		{
			String gcoName  = _msoiLocalConf.getGCONameForIntegration("MSProject");
			com.matrixone.MCADIntegration.server.beans.MCADMxUtil mxUtil = new com.matrixone.MCADIntegration.server.beans.MCADMxUtil(context, null,_GlobalCache);
			String GCOId = mxUtil.getGlobalConfigObjectID(context, "MCADInteg-GlobalConfig",gcoName);
			
			//BusinessObject boGCO = new BusinessObject(_msoiGlobalConf.getTypeName(), _msoiGlobalConf.getName(), _msoiGlobalConf.getRev(), "");
			BusinessObject bo = new BusinessObject(GCOId);
			//BusinessObject bo = new BusinessObject("MSP Integration Global Configuration", "mspIntegrationGCO", "0.0.0.a", "eService Administration");
			//BusinessObject bo = new BusinessObject(_msoiGlobalConf.getTypeName(), _msoiGlobalConf.getName(), _msoiGlobalConf.getRev(), "");
			bo.open(context);
			bo.checkinFile(context, true, true, "localhost", bo.getDefaultFormat(context), localProjectFileName, localProjectFilePath);
			bo.close(context);
			if (!debug)
			{
				fLocalFile.delete();
				System.out.println("Not deleting "+fLocalFile.toString());
			}
		}
		if(debug)
		System.out.println("==========================END   dumpTransactionXMLForServlet============================================");
		return localProjectFileName;
	}

	private void addToParent(Element elParentNode, String tagName, String nodeContent)
	{
		Element elCurrentNode = new Element(tagName, null, projectNamespaceUri);
		elCurrentNode.setText(nodeContent);
		elParentNode.addContent(elCurrentNode);
	}

    private void addCDATAToParent(Element elParentNode, String tagName, String nodeContent) throws Exception
    {
		CDATA cd = null;
		if (nodeContent != null)
		{
			cd = new CDATA(nodeContent);
		}
		else
		{
			addToParent(elParentNode, tagName, nodeContent);
			return;
		}

		ByteArrayOutputStream buf = new ByteArrayOutputStream();

		PrintWriter pw = new PrintWriter(buf, true);

		XMLOutputter xmo = new XMLOutputter();
		xmo.output(cd, pw);

		Element elCurrentNode = new Element(tagName, null, projectNamespaceUri);

		// Set the CDATA element as the text of the current node
		elCurrentNode.setText(buf.toString());
		elParentNode.addContent(elCurrentNode);
	}

	private String executeMQL(Context context, String mqlCmdString) throws MatrixException
	{
		if(debug)
		System.out.println("==========================START   executeMQL============================================");
		MQLCommand mqlCommand = new MQLCommand();
		boolean bReturnVal = mqlCommand.executeCommand(context, mqlCmdString);
		String result = null;
		if (!bReturnVal)
		{
		  throw new MatrixException(mqlCommand.getError());
		}
		result = mqlCommand.getResult();
		if ((result == null) || (result.equals("")))
		{
		  throw new MatrixException("Null value returned.");
		}
		if(result.endsWith("\n"))
		{
		  result = result.substring(0, (result.lastIndexOf("\n")));
		}
		if(debug)
			System.out.println("==========================END   executeMQL============================================");
		return result;
	}

	/**
	* Converts the predecessor type from FF, FS,... format to
	* Microsoft predecessor format and viceversa
	*
	* @param preType can be either FF, FS, SF, SS or 0, 1, 2, 3
	* @param convertToMSProject true/false
	*   true : change from PC based values to MS Project based predecessor Types
	*   false: change from MS Project based values to PC based predecessor Types
	*/
	private String getPredecessorType(String preType, boolean convertToMSProject)
	{
		String mspPreType = null;
	    if(convertToMSProject) 
		{
			mspPreType = "1"; //default type
		    if(preType.equals("FF"))
				mspPreType = "0";
		    else if(preType.equals("FS"))
				mspPreType = "1";
		    else if(preType.equals("SF"))
				mspPreType = "2";
		    else if(preType.equals("SS"))
				mspPreType = "3";
	    }
		else
		{
			if(preType.equals("0"))
				mspPreType = "FF";
		    else if(preType.equals("1"))
				mspPreType = "FS";
		    else if(preType.equals("2"))
				mspPreType = "SF";
		    else if(preType.equals("3"))
				mspPreType = "SS";
		}
		return mspPreType;
	}

	private void addLastFirstNameToCache(Context context, String userLastNameFirstName)
			throws FrameworkException 
	{
		if(debug)
			System.out.println("==========================START   addLastFirstNameToCache============================================");
		String vaultFilter = "*";
		StringList busSelects = new StringList();

		busSelects.addElement(person.SELECT_NAME);
		busSelects.addElement(person.SELECT_FIRST_NAME);
		busSelects.addElement(person.SELECT_LAST_NAME);

		//IR-016606V6R2012 fix
		busSelects.addElement("to[Member].from.type.kindof[Organization]");

		//value supplied is for example "Everything Test"
		//if the user specified in the defined format ignore the value
		if(userLastNameFirstName.indexOf(" ") != -1) {

			String lastName = userLastNameFirstName.substring(0, userLastNameFirstName.indexOf(" "));
			String firstName = userLastNameFirstName.substring(userLastNameFirstName.lastIndexOf(" ")).trim();

			String whereClause = person.SELECT_FIRST_NAME + " match '*" + firstName + "'";
			whereClause += " && " + person.SELECT_LAST_NAME + " match '" + lastName + "*" + "'";

			MapList ml = findObjects(
					context,        // eMatrix context
					TYPE_PERSON,    // type pattern
					"*",    	    // name pattern
					"*",            // revision pattern
					"*",            // owner pattern
					vaultFilter,    // vault pattern
					whereClause,    // where expression
					false,          // expand type
					busSelects);    // object selects

			Iterator mlitr = ml.iterator();

			while (mlitr.hasNext()) {
				Map map = (Map) mlitr.next();

				String personFullName = (String) map.get(person.SELECT_LAST_NAME) + " " + map.get(person.SELECT_FIRST_NAME);

				if(personFullName.equals(userLastNameFirstName))
				{
					//IR-016606V6R2012 fix
					String isItPresentInOrg = (String) map.get("to[Member].from.type.kindof[Organization]");
					if(isItPresentInOrg != null) {					
						//add the values to the cache
						_personInfo.put(userLastNameFirstName, map.get(person.SELECT_NAME));
					}
					break;
				}
			}
		}

		if(debug)
			System.out.println("==========================END   addLastFirstNameToCache============================================");
	}
	/**
	* Read the project node and fetch all the details in a maplist
	* for processing merge command
	*
	* @param preType can be either FF, FS, SF, SS or 0, 1, 2, 3
	* @param convertToMSProject true/false
	*   true : change from PC based values to MS Project based predecessor Types
	*   false: change from MS Project based values to PC based predecessor Types
	*/
	private Map readXMLDataForMerge(Context context, Element projectNode, boolean existingProject, boolean projectxml,Map taskIndexMap,MapList taskMapListForDependency)//[] extra param to indicate whether projectNode is projectxml or prevproject xml
	                    throws MatrixException
	{
		if(debug)
			System.out.println("==========================START   readXMLDataForMerge============================================");
		try
		{
			if (debug)
			{
				xmlDataForMerge_BeginTime = System.currentTimeMillis();
			}

			String estStartDate = null;
			String estFinishDate = null;
			String actStartDate 			= null;
			String actFinishDate 			= null;		
			String constraintType=null;
			String constraintTypeShortValue=null;
			String scheduleFromStart=null;
			String owner = null;
			String taskReq = null;
			String actDuration				= null;
			
			java.text.SimpleDateFormat mspDate = new java.text.SimpleDateFormat("yyyy-M-d'TO'H:mm:ss");

			Map projectInfoMap = new HashMap();
			Map taskLevelMap = new HashMap();
			Map ProjectSpaceMap = new HashMap();
			Map CWBS2WBSMap = new HashMap();
			Map WBS2ChildCountMap = new HashMap();
			Map seqNoMap = new HashMap();

			projectInfoMap.put("projectName", projectNode.getChild("Title",ns).getText());
			projectInfoMap.put("description", projectNode.getChild("Description",ns).getText());
			projectInfoMap.put("owner", projectNode.getChild("Author",ns).getText());
			if(projectNode.getChild("ScheduleBasedOn",ns)!=null)
				projectInfoMap.put("ScheduleBasedOn", projectNode.getChild("ScheduleBasedOn",ns).getText());
			
            //[2011x Start : This is to add ScheduleFromStart status(boolean), Constraint Type of Project Summary node from xml data into projectInfo map]
			
			//Project Schedule Information
			if(projectNode.getChild("ScheduleFrom",ns)!=null)	//This check is provided so as to bypass subproject-case
			{
				scheduleFromStart= (String)projectNode.getChild("ScheduleFrom",ns).getText();
			if(scheduleFromStart.equals("True"))
				scheduleFromStart="Project Start Date";
              else
            	  scheduleFromStart="Project Finish Date";
            projectInfoMap.put("ScheduleFrom", scheduleFromStart);
			}
            
            //Project Constraint Type
            if(projectNode.getChild("ConstraintType",ns)!=null)
            {
                constraintTypeShortValue=(String)projectNode.getChild("ConstraintType",ns).getText();
                constraintType = GetConstraintType(constraintTypeShortValue);    			
            }
			else 	
			  constraintType="";
			
			 projectInfoMap.put("constraintType", constraintType);
	            
            //[2011x End : This is to add ScheduleFromStart status(boolean), Constraint Type of Project Summary node from xml data into projectInfo map]
            
			estStartDate =  projectNode.getChild("StartDate",ns).getText();
			String prjStartDate =  estStartDate;
			if (prjStartDate != null && !prjStartDate.equals(""))
			{				
				Date startDate = mspDate.parse(prjStartDate);				
				prjStartDate = MATRIX_DATE_FORMAT.format(startDate);
			}
			projectInfoMap.put("ProjectStartDate", prjStartDate);
			
			estFinishDate = projectNode.getChild("FinishDate",ns).getText();
			String prjFinishDate =  estFinishDate;
			if (prjFinishDate != null && !prjFinishDate.equals(""))
			{				
				Date finishDate = mspDate.parse(prjFinishDate);				
				prjFinishDate = MATRIX_DATE_FORMAT.format(finishDate);
			}
			projectInfoMap.put("ProjectFinishDate", prjFinishDate);
			
			//projectInfoMap.put("company", projectNode.getChild("Company").getText());

			/*if(projectNode.getChild("ActualStart",ns)!=null)
				actStartDate = projectNode.getChild("ActualStart",ns).getText();
			if(projectNode.getChild("ActualFinish",ns)!=null)
			actFinishDate = projectNode.getChild("ActualFinish",ns).getText();*/

			Element elTasksNode = projectNode.getChild("Tasks",ns);

			//List lTasks = elTasksNode.getChildren("Task");
			List lTasks = elTasksNode.getChildren("Task",ns);

			ListIterator litTaskCtr = lTasks.listIterator();
			int i = 0;
			int WBSCounter =1;
			String strSeqNo= null;
			int seqNo = 0;
			String ParentChildCount;
			String ParentCWBS;
			String ParentWBS;

			MapList taskInfoMapList = new MapList();
			while (litTaskCtr.hasNext())
			{
				String constraintDate=null;
				Map subProjectMap = null;
				//the first task is the summary task of the project, this has only
				//few details like duration, %age complete etc
				Element elNextArgumentNode = (Element) litTaskCtr.next();
				Map taskMap = new HashMap();

				//this portion is common for project (first value in the iterator)
				//as well as task
				codeRegn = "GET %AGE complete, start, end dates...";
				
				//[2011x Start: Collecting Task Constraint Type] 
				 if(elNextArgumentNode.getChild("ConstraintType",ns)!=null)
				 {
						constraintTypeShortValue=elNextArgumentNode.getChild("ConstraintType",ns).getText();
					 constraintType = GetConstraintType(constraintTypeShortValue);						
				 }
				else 	
					constraintType="";
				
				//[2011x End: Collecting Task Constraint Type]
				
				String estDuration = elNextArgumentNode.getChild("Duration",ns).getText();
				if(elNextArgumentNode.getChild("ActualDuration",ns)!=null)
				{
					actDuration = elNextArgumentNode.getChild("ActualDuration",ns).getText();					
					if(actDuration != "")
					{
						String actHrsDuration, actMinsDuration;
						actHrsDuration = actDuration.substring(2, actDuration.indexOf("H"));
						actMinsDuration = actDuration.substring(actDuration.indexOf("H") + 1,actDuration.indexOf("M"));
						actDuration = Double.toString((Double.valueOf(actHrsDuration) / 8 + Double.valueOf(actMinsDuration) / (60 * 8) ));
					}
				}					
			
				String estHrsDuration, estMinsDuration;
				estHrsDuration = estDuration.substring(2, estDuration.indexOf("H"));
				estMinsDuration = estDuration.substring(estDuration.indexOf("H") + 1,estDuration.indexOf("M"));
				//Double newHrsDuration = new Double(Double.valueOf(estHrsDuration).doubleValue());
				//Double newMinsDuration = new Double(Double.valueOf(estMinsDuration).doubleValue());
				estDuration = Double.toString((Double.valueOf(estHrsDuration) / 8 + Double.valueOf(estMinsDuration) / (60 * 8) ));
				//359340 Begin
				Double dEstimated = Double.parseDouble(estDuration) * 100;
				Double dEstimated1,dEstimated2;
			
				dEstimated1 = Math.rint(dEstimated);
				dEstimated2 = dEstimated1/100;
				estDuration = Double.toString(dEstimated2);			
				//359340 End
				//estDuration = Double.toString((newDuration.doubleValue() / 8));

				/*String actHrsDuration, actMinsDuration;
				actHrsDuration = actDuration.substring(2, actDuration.indexOf("H"));
				actMinsDuration = actDuration.substring(actDuration.indexOf("H") + 1,actDuration.indexOf("M"));
				//newHrsDuration = new Double(Double.valueOf(actHrsDuration).doubleValue());
				//newMinsDuration = new Double(Double.valueOf(actMinsDuration).doubleValue());
				actDuration = Double.toString((Double.valueOf(actHrsDuration) / 8 + Double.valueOf(actMinsDuration) / (60 * 8) ));*/
				String summaryTask = elNextArgumentNode.getChild("Summary",ns).getText();

				String percentComplete = elNextArgumentNode.getChild("PercentComplete",ns).getText();
				if(i != 0)
				{
					//get the task start and end dates
					estStartDate = elNextArgumentNode.getChild("Start",ns).getText();
					estFinishDate =  elNextArgumentNode.getChild("Finish",ns).getText();

					actStartDate = null;
					actFinishDate = null;
					//get the actual task start and end dates
					if(elNextArgumentNode.getChild("ActualStart",ns)!=null)
						actStartDate = elNextArgumentNode.getChild("ActualStart",ns).getText();
					if(elNextArgumentNode.getChild("ActualFinish",ns)!=null)
					actFinishDate = elNextArgumentNode.getChild("ActualFinish",ns).getText();

					//[2011x Start: Collecting Task Constraint Date] 
					if(elNextArgumentNode.getChild("ConstraintDate",ns)!=null)
						constraintDate = elNextArgumentNode.getChild("ConstraintDate",ns).getText();
					//[2011x End: Collecting Task Constraint Date]
				}

				codeRegn = "Got Start and Finish dates";

				//estStartDate is either obtained at the project level
				//or at the task level
	
				if (estStartDate != null && !estStartDate.equals(""))
				{
					//estStartDate = estStartDate.substring(0, estStartDate.indexOf("T"));
					Date startDate = mspDate.parse(estStartDate);
					///////////////////////////////////10.8.0.2.LA /////////////////////////////////////////////
					//startDate = new Date(startDate.getTime() +43200000L ); //12h * 60 mins * 60secs * 1000 ms (To convert time from 12PM to 12 AM)
					estStartDate = MATRIX_DATE_FORMAT.format(startDate);
				}
				if (estFinishDate != null)
				{
					//estFinishDate = estFinishDate.substring(0, estFinishDate.indexOf("T"));
					Date finishDate = mspDate.parse(estFinishDate);
					///////////////////////////////////10.8.0.2.LA /////////////////////////////////////////////
					//finishDate = new Date(finishDate.getTime() +43200000L ); 
					estFinishDate = MATRIX_DATE_FORMAT.format(finishDate);
				}
				
				if (actStartDate != null && !actStartDate.equals(""))
				{
					Date startDate = mspDate.parse(actStartDate);
					actStartDate = MATRIX_DATE_FORMAT.format(startDate);
				}
				if (actFinishDate != null && !actFinishDate.equals(""))
				{
					Date finishDate = mspDate.parse(actFinishDate);
					actFinishDate = MATRIX_DATE_FORMAT.format(finishDate);
				}
				
				if(constraintDate != null && !constraintDate.equals(""))
				{
					Date taskConstraintDate= mspDate.parse(constraintDate);
					constraintDate= MATRIX_DATE_FORMAT.format(taskConstraintDate);
				}
				
				codeRegn = "converted Start and Finish dates";

				taskMap.put("estStartDate", estStartDate);
				taskMap.put("estFinishDate", estFinishDate);
				taskMap.put("actStartDate", actStartDate);
				taskMap.put("actFinishDate", actFinishDate);
				taskMap.put("percentComplete", percentComplete);
				taskMap.put("estDuration", estDuration);
				taskMap.put("actDuration", actDuration);
				taskMap.put("Summary", summaryTask);
				//[2011x Start: Collecting Task Constraint type and date in taskMap]
				taskMap.put("constraintType", constraintType);
				taskMap.put("constraintDate", constraintDate);
				//[2011x End: Collecting Task Constraint type and date in taskMap]

				//[Changes made to support creation of Projects of Sub types from MSProject]
				String taskType = elNextArgumentNode.getChild("NodeType",ns).getText();
				taskMap.put("taskType", taskType);
				//[/Changes made to support creation of Projects of Sub types from MSProject]
				Element elProjectNode = elNextArgumentNode.getChild("Project",ns);
				if (elProjectNode!= null)
				{
					subProjectMap =  new HashMap();
					subProjectMap = readXMLDataForMerge(context, elProjectNode, existingProject,projectxml,taskIndexMap,taskMapListForDependency);

					if (subProjectMap != null) 
					{
						taskMap.put("subprojectMap", subProjectMap);
					}
				}

				codeRegn = "getting uid name and level ";
				String outlineNumber = elNextArgumentNode.getChild("OutlineNumber",ns).getText();
				if(i != 0)
				{
					taskMap.put("taskUID", elNextArgumentNode.getChild("UID",ns).getText());
					String taskName = elNextArgumentNode.getChild("Name",ns).getText();
					taskMap.put("taskName", elNextArgumentNode.getChild("Name",ns).getText());
					// if(personId != null && personId != "")
					Element temp = elNextArgumentNode.getChild("Description",ns);
					
					if (elNextArgumentNode.getChild("Description",ns).getText() != null)
					{
						taskMap.put("description", elNextArgumentNode.getChild("Description",ns).getText());
					}
					else
					{
						taskMap.put("description", "");
					}
					taskMap.put("outlineLevel", elNextArgumentNode.getChild("OutlineLevel",ns).getText());
					String WBS= null;
					String prjSpaceOutlineNumber= null;
				
					if(outlineNumber.indexOf(".")<0)
					{
						WBS = Integer.toString(WBSCounter);
						WBSCounter ++;
						CWBS2WBSMap.put(outlineNumber,WBS);
						WBS2ChildCountMap.put(WBS, "0");
					}
					else
					{
						ParentCWBS =  outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
						ParentWBS =(String) CWBS2WBSMap.get(ParentCWBS);
						ParentChildCount = (String)WBS2ChildCountMap.get(ParentWBS);

						WBS = ParentWBS + "." +( Integer.valueOf(ParentChildCount)+1);
						CWBS2WBSMap.put(outlineNumber,WBS);
						WBS2ChildCountMap.put(ParentWBS, Integer.toString(Integer.valueOf(ParentChildCount)+1));
						WBS2ChildCountMap.put(WBS, "0");
					}
					taskMap.put("WBS", WBS);
					outlineNumber = WBS;
					if(isProjectSpace(context,taskType))
					{
						ProjectSpaceMap.put(outlineNumber,outlineNumber);
						seqNoMap.put(outlineNumber,"0");
					}
					else if(outlineNumber.lastIndexOf(".") >-1)
					{
						prjSpaceOutlineNumber = (String)ProjectSpaceMap.get(outlineNumber.substring(0, outlineNumber.lastIndexOf(".")));
						if (prjSpaceOutlineNumber!=null)
						{
							ProjectSpaceMap.put(outlineNumber,prjSpaceOutlineNumber);
						}
					}
					if(outlineNumber.lastIndexOf(".") == -1)
					{
						WBS = outlineNumber;
					}
					else
					{
						String levelId = null;
						levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
						prjSpaceOutlineNumber = (String)ProjectSpaceMap.get(levelId);
						if (prjSpaceOutlineNumber== null)
						{
							WBS = outlineNumber;
						}
						else
							WBS = outlineNumber.substring(prjSpaceOutlineNumber.length() + 1, outlineNumber.length());
					}
					if(outlineNumber.lastIndexOf(".") >-1)
					{
						String levelId = null;
						levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));
						prjSpaceOutlineNumber = (String)ProjectSpaceMap.get(levelId);
					
						if(prjSpaceOutlineNumber ==null || prjSpaceOutlineNumber.equals(""))
						{
							seqNo++;
							taskMap.put("SeqNo", Integer.toString(seqNo));
						}
						else
						{
							strSeqNo = (String)seqNoMap.get(prjSpaceOutlineNumber);
							taskMap.put("SeqNo", Integer.toString(Integer.valueOf(strSeqNo) +1));
							seqNoMap.put(prjSpaceOutlineNumber,Integer.toString(Integer.valueOf(strSeqNo) +1));
						}
					}
					else
					{
						seqNo++;
						taskMap.put("SeqNo", Integer.toString(seqNo));
					}
					taskMap.put("WBS", WBS);
					taskMap.put("outlineNumber", outlineNumber);

					taskMap.put("taskReq", elNextArgumentNode.getChild("TaskReq",ns).getText());
					taskMap.put("taskType", elNextArgumentNode.getChild("NodeType",ns).getText());
					taskMap.put("taskState", elNextArgumentNode.getChild("State",ns).getText());

					String ownerLFName = elNextArgumentNode.getChild("Owner",ns).getText();
					//the owner value above has the lastName (space) firstName
					//(as mentioned by the user in MSP)
					//convert the lastName (space) firstName into personId
					//find the person id of the person with the given Last name First Name
					//if it is not found then add to the _personInfo for caching
					if(_personInfo != null && _personInfo.get(ownerLFName) == null)
					{
						//found new Last Name First Name, add to cache
						addLastFirstNameToCache(context, ownerLFName);
					}

					//get the personName from cache map
					String personName = (String) _personInfo.get(ownerLFName);
					if(personName != null && !personName.equals("")) 
					{
						taskMap.put("owner", personName);
					}

					codeRegn = "Reading PredLink";
	
					//List lPreds = elNextArgumentNode.getChildren("PredecessorLink");
					List lPreds = elNextArgumentNode.getChildren("PredecessorLink",ns);
					ListIterator listPredsCtr = lPreds.listIterator();

					while (listPredsCtr.hasNext())
					{
						Element predecessorElement = (Element) listPredsCtr.next();
						Element predecessorUIDElement = (Element) predecessorElement.getChild("PredecessorUID",ns);
						if(predecessorUIDElement != null)
						{
							String predecessorUID = predecessorElement.getChild("PredecessorUID",ns).getText();
							String preType = predecessorElement.getChild("Type",ns).getText();
							//convert the pretype into PC based based pretype values
							String pcPreType = getPredecessorType(preType, false);
							String linkLag = predecessorElement.getChild("LinkLag",ns).getText();
							//String linkLagUnit = predecessorElement.getChild("LagUnit",ns).getText();
							//the default format is in hours
							//convert it into days
							Double iLinkLag;
							if(null != linkLag && !"".equals(linkLag))
							{
								iLinkLag = new Double(Double.valueOf(linkLag).doubleValue());
								if (projectxml) //BUG:[: in case of prevproject xml, this conversion not reqd as its already done when loaded from PMC to MSP
								{
									linkLag = Double.toString(iLinkLag.doubleValue() / ( 8  * 60));
								}
								else
									linkLag = Double.toString(iLinkLag.doubleValue());
							}
							else
							{
								linkLag = "0";
							}
							//332795
							Double dLinkLag = Double.parseDouble(linkLag) * 100;
							Double dTempLinkLag1,dTempLinkLag2;	
							dTempLinkLag1 = Math.rint(dLinkLag);
							dTempLinkLag2 = dTempLinkLag1/100;
							linkLag = Double.toString(dTempLinkLag2);

							Map dependencyMap = new HashMap();
							dependencyMap.put("taskUID", elNextArgumentNode.getChild("UID",ns).getText());
							dependencyMap.put("preUID", predecessorUID );
							dependencyMap.put("linkLag", linkLag);
							dependencyMap.put("pcPreType", pcPreType);

							//store the values in a maplist for creating dependency at a later stage
							taskMapListForDependency.add(dependencyMap);
						}//if(predecessorUIDElement != null)
					}//while (listPredsCtr.hasNext())
				}//if(i != 0)

				//if existing project then the taskIds of the task is available in MS Project
				//generate a list for adding/deleting/modifying structure on merge
				codeRegn = "done with dependency, assignee, resources. Reading taskUID";

				if(existingProject && i!= 0)
				{
					Element elExtendedAttrib = elNextArgumentNode.getChild("ExtendedAttribute",ns);
					String currentTaskId = elExtendedAttrib.getChild("Value",ns).getText();
					taskLevelMap.put(/*elNextArgumentNode.getChild("OutlineNumber",ns).getText()*/outlineNumber, currentTaskId);
					taskIndexMap.put(elNextArgumentNode.getChild("UID",ns).getText(), currentTaskId);
					taskMap.put("taskId", currentTaskId);
				}

				taskInfoMapList.add(taskMap);
				i++;
			}//while (litTaskCtr.hasNext())

			codeRegn = "Done with tasks and dependency";

			//build the resource list
			codeRegn = "fetch resources";
			Map resourceIndexMap = new HashMap();
			Map resourceNameMap = new HashMap();
			Element elResources = projectNode.getChild("Resources",ns);
			//List lResources = elResources.getChildren("Resource");
			List lResources = elResources.getChildren("Resource",ns);
	
			ListIterator listResourceCtr = lResources.listIterator();
			//String userLastNameFirstNameList = "";
			//boolean resourcesExist = false;
			int k = 1;
			StringList resourceList = new StringList();
			while (listResourceCtr.hasNext())
			{
				Element elResource = (Element) listResourceCtr.next();
				if(k == 1)
				{
					//the first set of values is the summary node of MSP datamodel
					//ignore the same for PC model
					k++;
					continue;
				}
				String resourceUID = elResource.getChild("UID",ns).getText();
				String userLastFirstName = elResource.getChild("Name",ns).getText();

				//the resourceIndexMap generated above has the lastName (space) firstName
				//for the resourcename (as mentioned by the user in MSP)
				//convert the lastName (space) firstName into personId
				//find the person id of the person with the given Last name First Name
				//if it is not found then add to the _personInfo for caching
				if(_personInfo != null && _personInfo.get(userLastFirstName) == null)
				{
					//found new Last Name First Name, add to cache
					addLastFirstNameToCache(context, userLastFirstName);
				}

				//get the personName from cache map
				String personName = (String) _personInfo.get(userLastFirstName);
				resourceNameMap.put(resourceUID, userLastFirstName);
				if(personName != null && !personName.equals("")) 
				{
					String personId = person.getPerson(context, personName).getId();
					resourceIndexMap.put(resourceUID, personId);
				}
				else
				{
					resourceIndexMap.put(resourceUID, "");
				}
				if(!userLastFirstName.equals(""))
					resourceList.add(userLastFirstName);
			}//while (listResourceCtr.hasNext())

			//build the assignment list
			codeRegn = "READ XML DATA: fetch assignments";

			if (debug)
				System.out.println(codeRegn);
			Element elAssignments = projectNode.getChild("Assignments",ns);
	
			//List lAssignment = elAssignments.getChildren("Assignment");
			List lAssignment = elAssignments.getChildren("Assignment",ns);

			MapList assigneeMapList = new MapList();
			ListIterator listAssignmentCtr = lAssignment.listIterator();

			Map taskUnitsDataMap = new HashMap();

			while (listAssignmentCtr.hasNext())
			{
				Element elAssignment = (Element) listAssignmentCtr.next();
				Map assigneeMap = new HashMap();

				String taskUID = elAssignment.getChild("TaskUID",ns).getText();
				String resourceUID = elAssignment.getChild("ResourceUID",ns).getText();
				// rp
				String units = elAssignment.getChild("Units",ns).getText();
				boolean firstTime = true;
	
				if (firstTime)
				{
					taskUnitsDataMap.put("taskUID", taskUID);
					taskUnitsDataMap.put("resourceUID", resourceUID);
					taskUnitsDataMap.put("units", units);
					firstTime = false;
				}
	
				assigneeMap.put("taskUID", elAssignment.getChild("TaskUID",ns).getText());
				assigneeMap.put("resourceUID", elAssignment.getChild("ResourceUID",ns).getText());
				// rp
				assigneeMap.put("units", elAssignment.getChild("Units",ns).getText());
				assigneeMapList.add(assigneeMap);
			}

			Map projectDataMap = new HashMap();
			projectDataMap.put("projectData", projectInfoMap);
			projectDataMap.put("taskData", taskInfoMapList);
			projectDataMap.put("resourceIndexMap", resourceIndexMap);
			projectDataMap.put("resourceList", resourceList);
			projectDataMap.put("assigneeMapList", assigneeMapList);
			projectDataMap.put("taskLevelMap", taskLevelMap);
			projectDataMap.put("taskUnitsDataMap", taskUnitsDataMap);
			projectDataMap.put("resourceNameMap", resourceNameMap);
			xmlDataForMerge_EndTime = System.currentTimeMillis();

			if (debug)
			{
				long total_xmlDataForMerge_Time = xmlDataForMerge_EndTime - xmlDataForMerge_BeginTime;
				System.out.println("==========================END   readXMLDataForMerge============================================");
			}
			return projectDataMap;
		}
		catch(Exception e)
		{
			String message = null;
			if (e.toString().indexOf("System Error") != -1)
			{
				message = "Found Invalid characters, Aborting transaction.";
				throw new MatrixException(message + e.toString());
			}
			else
			{
				throw new MatrixException(codeRegn + e.toString());
			}
		}
	}


	private ArrayList readXMLDataForValidateResource(Context context, Element ResourceNode)
	                    throws MatrixException
	{
		if(debug)
			System.out.println("==========================START   readXMLDataForValidateResource============================================");
		try
		{
			if (debug)
			{
				xmlDataForValidateResource_BeginTime = System.currentTimeMillis();
			}			
			
			//Element elTasksNode = projectNode.getChild("Tasks",ns);
			List lResource = ResourceNode.getChildren("Resource");
			ArrayList<String> Resourcelist = new ArrayList<String>();

			ListIterator listResourceCtr = lResource.listIterator();		
		
			while (listResourceCtr.hasNext())
			{						
				Element elResource = (Element) listResourceCtr.next();
				String sResourceName = elResource.getText();
				System.out.println("elResource " + elResource);
				Resourcelist.add(sResourceName);

			}
			System.out.println("Resourcelist" + Resourcelist);
			xmlDataForValidateResource_EndTime = System.currentTimeMillis();

			if (debug)
			{
				long total_xmlDataForValidateResource_Time = xmlDataForValidateResource_EndTime - xmlDataForValidateResource_BeginTime;
			System.out.println("==========================END   readXMLDataForValidateResource============================================");
			}
			return Resourcelist;
		}
		catch(Exception e)
		{
			String message = null;
			if (e.toString().indexOf("System Error") != -1)
			{
				message = "Found Invalid characters, Aborting transaction.";
				throw new MatrixException(message + e.toString());
			}
			else
			{
				throw new MatrixException(codeRegn + e.toString());
			}
		}
	}

	/**
	* Create dependencies for a given task
	*
	* @param dependencyMapList Maplist of contents that needs to added or modified
	* @param removedDependencyMapList Maplist of contents that needs to be deleted
	*   true : change from PC based values to MS Project based predecessor Types
	*   false: change from MS Project based values to PC based predecessor Types
	*/
	private MapList addDependency(Context context, MapList dependencyMapList, MapList removedDependencyList, Map taskIndexMap) throws MatrixException
	{
		try
		{
			MapList tempRemovedDependencyList = new MapList();
		    if(removedDependencyList != null)
			{
		        tempRemovedDependencyList.addAll(removedDependencyList);
			}
		    //build the dependency
		    codeRegn = "Setting dependencies";
		    ListIterator dependencyItr = dependencyMapList.listIterator();

		    Map dependencyIndexMap = new HashMap();
		    while (dependencyItr.hasNext())
		    {
				Map dependencyMap = (Map) dependencyItr.next();
		        String preTaskId = (String)dependencyMap.get("preId");
				String taskId = (String)dependencyMap.get("taskId");
			    String pcPreType = (String)dependencyMap.get("pcPreType");
		        String linkLag = (String)dependencyMap.get("linkLag");

				StringList busSelects = new StringList(1);
		        StringList relSelects = new StringList(1);

				busSelects.add(task.SELECT_ID);
		        relSelects.add(dependency.SELECT_DEPENDENCY_TYPE);
				relSelects.add(dependency.SELECT_LAG_TIME);

				boolean addPred = false;
				boolean modifyPred = false;

				//this task might have been deleted
				//check if this task exists if it exists then remove the assignee
				if(taskIndexMap.containsValue(taskId))
				{
					//task exists
			        task.setId(taskId);
			  	    MapList predecessorList = task.getPredecessors(context, busSelects, relSelects, null);

			        //if no predecessors exist then add it
			 	    if(predecessorList.size() == 0)
				    {
						addPred = true;
				    }
				    else
					{
					    //check if this pred already exists
					    //if it exists then check if the values are different
					    addPred = true;
			            Iterator predecessorItr = predecessorList.iterator();
			            while (predecessorItr.hasNext())
					    {
							Map predecessorObj = (Map) predecessorItr.next();
			                String predecessorId = (String) predecessorObj.get(task.SELECT_ID);
			                if (predecessorId.equals(preTaskId))
					        {
								String depType = (String) predecessorObj.get(dependency.SELECT_DEPENDENCY_TYPE);
				                String depLag = (String) predecessorObj.get(dependency.SELECT_LAG_TIME);
						        if(!depType.equals(pcPreType) || !depLag.equals(linkLag))
								{
									String connectionId = (String) predecessorObj.get(dependency.SELECT_ID);
					                HashMap attributes = new HashMap();
					                HashMap modifyPredMap = new HashMap();
					                attributes.put(dependency.ATTRIBUTE_LAG_TIME,linkLag);
					                attributes.put(dependency.ATTRIBUTE_DEPENDENCY_TYPE, pcPreType);
					                modifyPredMap.put(connectionId,attributes);
					                task.modifyPredecessors(context, (Map) modifyPredMap);
					  	        }
								//found the dependency
				                addPred = false;
						        break;
							}					      
						}	
					}

					if(addPred)
					{
						Map attributes = new HashMap();
						Map addPredMap = new HashMap ();
						attributes.put(dependency.ATTRIBUTE_LAG_TIME, linkLag);
						attributes.put(dependency.ATTRIBUTE_DEPENDENCY_TYPE, pcPreType);
						addPredMap.put(preTaskId, attributes);
						task.addPredecessors(context, (Map) addPredMap, true);
					}
				    else
			        {
						//existing predecessor

						//on comparing the existing dependencyMapList with
					    //oldDependencyMapList, if the value of the dependency is modified
	  			        //then it ends up in the removedDependencyMapList,
					    //since modifications are already taken care of
				        //remove it from the removedDependencyMapList

	  				    if(removedDependencyList != null) 
					    {
							Iterator removedDependencyListItr = removedDependencyList.iterator();
					        while (removedDependencyListItr.hasNext())
							{
								Map dMap = (Map) removedDependencyListItr.next();
			                    String remPreTaskId = (String)dMap.get("preId");
					            String remTaskId = (String)dMap.get("taskId");
 				                if(remPreTaskId.equals(preTaskId) && remTaskId.equals(taskId))
						        {
									tempRemovedDependencyList.remove(dMap);
				                }
					        }
				        }
					}
                }
	      }//while (dependencyItr.hasNext())
	      return tempRemovedDependencyList;
		}	
	    catch(Exception e)
		{
	       throw new MatrixException(codeRegn + e.getMessage());
		}
	}

	/**
    * Create dependencies for a given task
    *
    * @param dependencyMapList Maplist of contents that needs to added or modified
    * @param removedDependencyMapList Maplist of contents that needs to be deleted
    *   true : change from PC based values to MS Project based predecessor Types
    *   false: change from MS Project based values to PC based predecessor Types
    */
	private MapList addDependency(Context context, MapList dependencyMapList, MapList removedDependencyList, Map taskIndexMap, boolean projectExists) throws MatrixException
	{
		if(debug)
		System.out.println("==========================START   addDependency============================================");
		try
		{
			MapList tempRemovedDependencyList = new MapList();
			if(removedDependencyList != null)
			//if (removedDependencyList.size() != 0)
			{
				tempRemovedDependencyList.addAll(removedDependencyList);
			}
			//build the dependency
			codeRegn = "Setting dependencies";
			ListIterator dependencyItr = dependencyMapList.listIterator();

			Map dependencyIndexMap = new HashMap();

			int counter = 0;
			//get the number of dependency
			int numDependency = dependencyMapList.size();

			while (dependencyItr.hasNext())
			{
				Map dependencyMap = (Map) dependencyItr.next();
				String preTaskId = (String)dependencyMap.get("preId");
				String taskId = (String)dependencyMap.get("taskId");
				String pcPreType = (String)dependencyMap.get("pcPreType");
				String linkLag = (String)dependencyMap.get("linkLag");

				StringList busSelects = new StringList(1);
				StringList relSelects = new StringList(1);

				busSelects.add(task.SELECT_ID);
				relSelects.add(dependency.SELECT_DEPENDENCY_TYPE);
				relSelects.add(dependency.SELECT_LAG_TIME);

				boolean addPred = false;
				boolean modifyPred = false;

				//this task might have been deleted
				//check if this task exists if it exists then remove the assignee
				if(taskIndexMap.containsValue(taskId))
				{
					//task exists
					task.setId(taskId);
					if(projectExists)
					{
						if (debug)
						{
							System.out.println("getting the predecessors");
						}
				
						MapList predecessorList = task.getPredecessors(context, busSelects, relSelects, null);				 

						//if no predecessors exist then add it
						if(predecessorList.size() == 0)
						{
							addPred = true;
						}
						else
						{
							//check if this pred already exists
							//if it exists then check if the values are different
							if (debug)
								System.out.println("========Modifying a predecessor...");

							addPred = true;
							Iterator predecessorItr = predecessorList.iterator();
							while (predecessorItr.hasNext())
							{
								Map predecessorObj = (Map) predecessorItr.next();
								String predecessorId = (String) predecessorObj.get(task.SELECT_ID);
								if (debug)
									System.out.println(" predecessorId: "+predecessorId);
								if (predecessorId.equals(preTaskId))
								{
									String depType = (String) predecessorObj.get(dependency.SELECT_DEPENDENCY_TYPE);
									String depLag = (String) predecessorObj.get(dependency.SELECT_LAG_TIME);
									if (debug)
										System.out.println(" depType: "+depType +" depLag = "+depLag);

									if(!depType.equals(pcPreType) || !depLag.equals(linkLag))
									{
										String connectionId = (String) predecessorObj.get(dependency.SELECT_ID);
										HashMap attributes = new HashMap();
										HashMap modifyPredMap = new HashMap();
										attributes.put(dependency.ATTRIBUTE_LAG_TIME,linkLag);
										attributes.put(dependency.ATTRIBUTE_DEPENDENCY_TYPE, pcPreType);
										modifyPredMap.put(connectionId,attributes);            
										task.modifyPredecessors(context, (Map) modifyPredMap);			

										//Begin Code to handle duration keyword IR-031267V6R2012
										String [] argsDependancy = new String[2];
										argsDependancy[0] = connectionId;
										argsDependancy[1] = taskId;			
										JPO.invoke(context,"emxDurationKeywordsBase",null,"triggerModifyDependencyDurationKeyword",argsDependancy);
										//End Code to handle duration keyword IR-031267V6R2012										

										//To fix the issue : When direction of the dependency is changed. PBN Issue 95925
										//Remove the task/predecessor from tempRemovedDependencyList since the dependency is modified.
										ListIterator tempDependencyItr = tempRemovedDependencyList.listIterator();
										while (tempDependencyItr.hasNext())
										{
											Map _map = (Map) tempDependencyItr.next();
											if(preTaskId.equals((String)_map.get("preId")))
											{
												int index = tempRemovedDependencyList.indexOf(_map);
												if(tempRemovedDependencyList.contains(_map) && index >= 0)
												{
													tempRemovedDependencyList.remove(index);
													break;
												}
											}
										}
									}
									//found the dependency
									addPred = false;
									break;
								}
							}
						}
					}  // check if project exists
					else
					{
						//project does not exist so just add the dependency no need to check
						addPred = true;
					}

					if(addPred)
					{
						//System.out.println("========Adding new predecessor...");
						Map attributes = new HashMap();
						Map addPredMap = new HashMap ();
						attributes.put(dependency.ATTRIBUTE_LAG_TIME, linkLag);
						attributes.put(dependency.ATTRIBUTE_DEPENDENCY_TYPE, pcPreType);
						addPredMap.put(preTaskId, attributes);

						// In previous versions
						// task.addPredecessors(context, (Map) addPredMap, true);

						//adding predecessors in bulk, do not rollup dates for each task
						//rollup the dates only for the last task
						// Fix for faster first time checkin

						if(counter != numDependency - 1)
						{
							if (debug)
							{
								System.out.println("Adding new predecessor - No dates rollup " + counter);
							}			  
							task.addPredecessors(context, (Map) addPredMap, true);														  
						}
						else
						{
							if (debug)
							{
								System.out.println("Adding new predecessor - Dates rolling up " + counter);
							}
							//[Changes made in 10.7.SP1.PQ1, Mar 22 07 to fix the BUG: 
							//Dates are not merged correctly for dependnt task of the last dependency]
							task.addPredecessors(context, (Map) addPredMap, true);
							//[/Changes made in 10.7.SP1.PQ1, Mar 22 07 to fix the BUG: 
							//Dates are not merged correctly for dependnt task of the last dependency]
						}
						counter++;
					} // add predecessor
				}
				else
				{
					//existing predecessor

					//on comparing the existing dependencyMapList with
					//oldDependencyMapList, if the value of the dependency is modified
					//then it ends up in the removedDependencyMapList,
					//since modifications are already taken care of
					//remove it from the removedDependencyMapList

					//if (removedDependencyList.size() != 0)
					if(removedDependencyList != null)
					{
						Iterator removedDependencyListItr = removedDependencyList.iterator();
						while (removedDependencyListItr.hasNext())
						{
							Map dMap = (Map) removedDependencyListItr.next();
							String remPreTaskId = (String)dMap.get("preId");
							String remTaskId = (String)dMap.get("taskId");

							if(remPreTaskId.equals(preTaskId) && remTaskId.equals(taskId))
							{
								tempRemovedDependencyList.remove(dMap);
							}
						} // end while
					}
				}
			}//while (dependencyItr.hasNext())
			if(debug)
			System.out.println("==========================END   addDependency============================================");
			return tempRemovedDependencyList; //To fix the issue : When direction of the dependency is changed. PBN Issue 95925
		} // end try
		catch(Exception e)
		{
			throw new MatrixException(codeRegn + e.getMessage());
		}
	}

    /**
    * Delete dependencies
    *
    * @param preType can be either FF, FS, SF, SS or 0, 1, 2, 3
    * @param convertToMSProject true/false
    *   true : change from PC based values to MS Project based predecessor Types
    *   false: change from MS Project based values to PC based predecessor Types
    */
	private void removeDependency(Context context, MapList dependencyMapList, Map taskIndexMap) throws MatrixException
	{
		if(debug)
			System.out.println("==========================START   removeDependency============================================");
		try
		{
			//build the dependency
			codeRegn = "Deleting dependencies";
			ListIterator dependencyItr = dependencyMapList.listIterator();

			if (debug)
			System.out.println(codeRegn);
			Map dependencyIndexMap = new HashMap();
			while (dependencyItr.hasNext())
			{
				Map dependencyMap = (Map) dependencyItr.next();
				String preTaskId = (String)dependencyMap.get("preId");
				String taskId = (String)dependencyMap.get("taskId");
				String pcPreType = (String)dependencyMap.get("pcPreType");
				String linkLag = (String)dependencyMap.get("linkLag");

				//this task might have been deleted
				//check if this task exists if it exists then remove the dependency
				if(taskIndexMap.containsValue(taskId))
				{
					//task exists
					//set the Id of the task and obtain its dependencies
					task.setId(taskId);
					StringList busSelects = new StringList(1);
					StringList relSelects = new StringList(1);
					busSelects.add(task.SELECT_ID);
					relSelects.add(dependency.SELECT_DEPENDENCY_TYPE);

					MapList predecessorList = task.getPredecessors(context, busSelects, relSelects, null);
					Iterator predecessorItr = predecessorList.iterator();

					while (predecessorItr.hasNext())
					{
						Map predecessorObj = (Map) predecessorItr.next();
						String predecessorId = (String) predecessorObj.get(task.SELECT_ID);
						if (predecessorId.equals(preTaskId))
						{
							if(debug)
								  System.out.println("========Removing predecessor...");

							String connectionId = (String) predecessorObj.get(dependency.SELECT_ID);
							task.removePredecessor(context, connectionId);
							break;
						}
					}
				} // end if
			}
		}
		catch(Exception e)
		{
				throw new MatrixException(codeRegn + e.getMessage());
		}
		if(debug)
	   		System.out.println("==========================END   removeDependency============================================");
	}

    /**
    * Add assignees for a given task
    *
    * @param assigneeMapList
    * @param resourceIndexMap
    * @param taskIndexMap
    *
    */
	//private void addAssignees(Context context, MapList assigneeMapList, Map resourceIndexMap, Map taskIndexMap, MapList oldAssigneeMapList, Map oldResourceIndexMap, Map oldTaskIndexMap) throws MatrixException
	private void addAssignees(Context context, MapList assigneeMapList, Map resourceIndexMap, Map taskIndexMap, MapList oldAssigneeMapList, Map oldResourceIndexMap, Map oldTaskIndexMap, Map taskUnitMap) throws MatrixException
	{
		if(debug)
			System.out.println("==========================START   addAssignees============================================");
		try
		{
			addAssignees_startTime = System.currentTimeMillis();

			//build the assignment list
			codeRegn = "add assignments";
			if(debug)
				System.out.println(codeRegn);
			//if oldAssigneeMapList is not null then addAssignees is for merge case of existing project
			//if null then add assignees is called during creation of a brand new project
			if(oldAssigneeMapList != null)
			//if (oldAssigneeMapList.size() != 0)
			{
				MapList comparisonAssigneeMapList = new MapList();
				MapList comparisonOldAssigneeMapList = new MapList();
				Map taskUnitsDataMap = new HashMap();
				Map assigneeUnitsMap = new HashMap();

				//convert the MS Project resource uid and task uid
				//into PC person id and task id and compare if any new
				//assignments have been made or if existing assignments
				//are modified
				ListIterator listAssignmentCtr = assigneeMapList.listIterator();
				while (listAssignmentCtr.hasNext())
				{
					Map assigneeMap = (Map) listAssignmentCtr.next();
					String taskUID = (String) assigneeMap.get("taskUID");
					String resourceUID = (String) assigneeMap.get("resourceUID");
					//get the person id for the above resoure uid
					String personId = (String) resourceIndexMap.get(resourceUID);
					//get the task id for the above task uid
					String taskId = (String) taskIndexMap.get(taskUID);
					if(debug)
						System.out.println("**** personId = "+personId);

					// rp
					String alloUnits = (String) assigneeMap.get("units");
					//7/6/2006
					taskUnitsDataMap = new HashMap();
					taskUnitsDataMap.put("taskUID", taskUID);
					taskUnitsDataMap.put("resourceUID", resourceUID);
					taskUnitsDataMap.put("units", alloUnits);

					if(debug)
						System.out.println("**** setValues1 was called with AM units = "+alloUnits);
					setValues(context, taskId, personId, alloUnits);

					Map personMap = new HashMap();
					personMap.put("personId", personId);
					personMap.put("taskId", taskId);

					comparisonAssigneeMapList.add(personMap);
					assigneeUnitsMap.put(personMap, taskUnitsDataMap);
				}

				ListIterator listOldAssignmentCtr = oldAssigneeMapList.listIterator();
				while (listOldAssignmentCtr.hasNext())
				{
					Map oldAssigneeMap = (Map) listOldAssignmentCtr.next();
					String taskUID = (String) oldAssigneeMap.get("taskUID");
					String resourceUID = (String) oldAssigneeMap.get("resourceUID");
					//get the person id for the above resoure uid
					String personId = (String) oldResourceIndexMap.get(resourceUID);
					//get the task id for the above task uid
					String taskId = (String) oldTaskIndexMap.get(taskUID);

					// rp
					String alloUnits = (String) oldAssigneeMap.get("units");
					//7/6/2006
					taskUnitsDataMap = new HashMap();
					taskUnitsDataMap.put("taskUID", taskUID);
					taskUnitsDataMap.put("resourceUID", resourceUID);
					taskUnitsDataMap.put("units", alloUnits);

					Map personMap = new HashMap();
					personMap.put("personId", personId);
					personMap.put("taskId", taskId);
					comparisonOldAssigneeMapList.add(personMap);
					assigneeUnitsMap.put(personMap, taskUnitsDataMap);
				}
				//now iterate through the comparisonAssigneeMapList, check if each element
				//already exists in comparisonOldAssigneeMapList
				ListIterator comparisonAssigneeMapListItr = comparisonAssigneeMapList.listIterator();
				while (comparisonAssigneeMapListItr.hasNext())
				{
					Map assigneeMap = (Map) comparisonAssigneeMapListItr.next();
					if(!(comparisonOldAssigneeMapList.contains(assigneeMap)))
					{
						//newly added or modified assingnee
						String taskId = (String) assigneeMap.get("taskId");
						if (ProjectIds.indexOf(taskId) >= 0 )
						{
							continue;
						}
						String personId = (String) assigneeMap.get("personId");
						Map _taskUnitsDataMap = (Map) assigneeUnitsMap.get(assigneeMap);
						task.setId(taskId);
						//if the user specified name is not found then personId is ""
						//in the map ignore this input
						codeRegn = "Iterate and add Assignee";
						String allocatedUnits = (String) assigneeMap.get("units");

						if(personId != null && personId != "")
						{
							//377672 begin
							//task.addAssignee(context, personId, null);
							String units = (String)_taskUnitsDataMap.get("units");
							Float s = new Float((1.0*100)* (Double.valueOf(units)).doubleValue());
							units = s.toString();

							task.addAssignee(context, personId, null,units);												
							//if (units != null)
							//{						
							//	setValues(context, taskId, personId, (String)_taskUnitsDataMap.get("units"));						
								//setValues(context, taskId, personId, (String)assigneeMap.get("units"));
							//}
							// 377672 end
						}
					}
					else
					{
						//no change in the map remove it from the maplist
						comparisonOldAssigneeMapList.remove(assigneeMap);
					}
				}
				//after iterating the list the values not removed in the comparisonOldAssigneeMapList
				//are assignees removed from a given task, so remove the assignees
				ListIterator comparisonOldAssigneeMapListItr = comparisonOldAssigneeMapList.listIterator();
				while (comparisonOldAssigneeMapListItr.hasNext())
				{
					Map assigneeMap = (Map) comparisonOldAssigneeMapListItr.next();
					//newly added or modified assignee
					String taskId = (String) assigneeMap.get("taskId");
					String personId = (String) assigneeMap.get("personId");

					//this task might have been deleted
					//check if this task exists if it exists then remove the assignee
					if(taskIndexMap.containsValue(taskId))
					{
						//task exists
						task.setId(taskId);

						//find the membership id
						StringList busSelect = new StringList();
						StringList relSelect = new StringList();
						busSelect.add(person.SELECT_ID);
						relSelect.add(assignee.SELECT_ID);
						MapList assigneeInfo = task.getAssignees(context, busSelect, relSelect, null);
						ListIterator assigneeInfoItr = assigneeInfo.listIterator();
						String membershipId = null;
						while (assigneeInfoItr.hasNext())
						{
							Map aMap = (Map) assigneeInfoItr.next();
							if(aMap.get(person.SELECT_ID).equals(personId))
							{
								membershipId = (String) aMap.get(assignee.SELECT_ID);
								break;
							}
						}
						if(personId != null && personId != "" && membershipId != null)
						{
							codeRegn = "Iterate and remove Assignee";
							task.removeAssignee(context, membershipId);
						}
					}
				}
			}
			else
			{
				//brand new project no comparisons required just add the assignees
				ListIterator listAssignmentCtr = assigneeMapList.listIterator();
				while (listAssignmentCtr.hasNext())
				{
					Map assigneeMap = (Map) listAssignmentCtr.next();
					String taskUID = (String) assigneeMap.get("taskUID");
					String resourceUID = (String) assigneeMap.get("resourceUID");

					//get the person id for the above resoure uid
					String personId = (String) resourceIndexMap.get(resourceUID);
					//get the task id for the above task uid
					String taskId = (String) taskIndexMap.get(taskUID);

					//if (debug)
					//	System.out.println("New Project-> taskUID = "+taskUID +" resourceUID = "+taskUID +" personId = "+personId +" taskID = "+taskId);
					if (ProjectIds.indexOf(taskId) >= 0 )
					{
						continue;
					}

					task.setId(taskId);
					//if the user specified name is not found then personId is ""
					//in the map ignore this input
					if(personId != null && personId != "")
					{
						if(debug)
						System.out.println("15 - Assignee Added");
											   
						//task.addAssignee(context, personId, null);																					  
						// rp
						String units = (String) assigneeMap.get("units");
							Float s = new Float((1.0*100)* (Double.valueOf(units)).doubleValue());
							units = s.toString();

							task.addAssignee(context, personId, null,units);
						//if (units != null)
						{
								//setValues(context, taskId, personId, units);
						}
					}
				}
			}
			if (debug)
			{
				addAssignees_endTime = System.currentTimeMillis();
				long addAssignees_totalTime = addAssignees_endTime - addAssignees_startTime;

				System.out.println("TOTAL TIME IN addAssignees (in Secs): " + addAssignees_totalTime);
				if(debug)
				System.out.println("==========================START   addAssignees============================================");
			}

		}
		catch (Exception e)
		{
			throw new MatrixException(codeRegn + e.getMessage());
		}
	}

    /**
    * Check if a object exists
    *
    * @param objectId
    */
    private boolean checkObjectExists(Context context, String objectId) throws MatrixException
	{
		try
		{
			codeRegn = "check object exists";
			boolean objectExists = false;
		    String result = null;

			String mqlCmdString = "print bus " + objectId + " select exists dump |;";
		    try
		    {
				result = executeMQL(context, mqlCmdString);
		    }
		    catch(Exception ex)
		    {
				objectExists = false;
		    }

			if(result != null && result.equalsIgnoreCase("TRUE"))
		    {
				objectExists = true;
			}
			return objectExists;
		}
		catch (Exception e)
		{
			throw new MatrixException(e.getMessage());
		}
	}
	 

    private String executeMQL2(Context context, String mqlCmdString) throws MatrixException
    {
		if(debug)
			System.out.println("==========================START   executeMQL2============================================");
		MQLCommand mqlCommand = new MQLCommand();
		boolean bReturnVal = mqlCommand.executeCommand(context, mqlCmdString);
		String result = null;
		if (!bReturnVal)
		{
			result = "";
		}
		else 
		{
			result = mqlCommand.getResult();
			if ((result == null) || (result.equals("")))
		    {
				throw new MatrixException("Null value returned.");
			}
	  	    if(result.endsWith("\n"))
		    {
				result = result.substring(0, (result.lastIndexOf("\n")));
			}
		}
		if(debug)
			System.out.println("==========================END   executeMQL2============================================");
	   return result;
	}

    /**
    * Deletes the objects specified by the ids.
    *
    * @param context the eMatrix <code>Context</code> object
    * @param objectIds the ids of the objects to delete
    * @throws FrameworkException if the operation fails
    * @since PC 10.0.0.0
    * @grade 0
    */
	private void deleteTasks(Context context, MapList removedList, Map taskLevelMap, String parentId)
			throws FrameworkException
	{
		if(debug)
			System.out.println("==========================START   deleteTasks============================================");
		try
		{
			com.matrixone.apps.program.Task object = (com.matrixone.apps.program.Task)DomainObject.newInstance(context, TYPE_TASK, DomainConstants.PROGRAM);
			ListIterator removedListItr = removedList.listIterator();
	
			int j = 0;
			String[] taskIds = new String[removedList.size()];
			String[] outlineNumbers = new String[removedList.size()];
			String taskState;

			while (removedListItr.hasNext())
			{
				Map mspMap = (Map) removedListItr.next();
			}
	
			while (removedListItr.hasPrevious())
			{
				Map removedListMap = (Map) removedListItr.previous();
				taskIds[j] = (String) removedListMap.get("taskId");
				outlineNumbers[j] = (String) removedListMap.get("OutlineNumber");
				j++;
			}

			StringList busSelects = new StringList(3);
			busSelects.add(DomainConstants.SELECT_CURRENT);
			busSelects.add(DomainConstants.SELECT_ID);
			busSelects.add(DomainConstants.SELECT_NAME);
			MapList taskInfoList = DomainObject.getInfo(context, taskIds, busSelects);
			StringList taskInCreate = new StringList();
			StringList taskInNonCreate = new StringList();
	
			for (int iLen = 0; iLen < taskInfoList.size(); iLen++) {

				Map infoMap = (Map) taskInfoList.get(iLen);
				taskState = (String) infoMap.get(DomainObject.SELECT_CURRENT);
				String taskId = (String) infoMap.get(DomainConstants.SELECT_ID);
				String taskName = (String) infoMap.get(DomainConstants.SELECT_NAME);
	
				if (AllOldProjectIds.indexOf(taskId) >= 0 )
				{
					subProject.setId(taskId);
					StringList busSelect = new StringList();
					busSelect.add("to[" + RELATIONSHIP_SUBTASK + "].id");
					Map taskInfo = subProject.getInfo(context, busSelect);
					String connectionId = (String) taskInfo.get("to[" + RELATIONSHIP_SUBTASK + "].id");
					DomainRelationship.disconnect(context, connectionId);
					continue;
				}
	
				if (!("Create".equalsIgnoreCase(taskState) || "Assign".equalsIgnoreCase(taskState))) {
					taskInNonCreate.add(taskName);	
				} else {
					taskInCreate.add(taskId);
					taskLevelMap.remove(outlineNumbers[iLen]);
				}
			}

			String[] tasksToDelete = (String[]) taskInCreate.toArray(new String[taskInCreate.size()]);

			if (tasksToDelete.length > 0) {
				object.setId(parentId);
				object.delete(context, tasksToDelete);
			}
			if (taskInNonCreate.size() > 0) {
				StringBuilder errorMessage = new StringBuilder(
						"Unable to delete listed task(s). Task is beyond Assign state: \n [");
				int taskListSize = taskInNonCreate.size();
				for (int k = 0; k < taskListSize; k++) {
	
					errorMessage.append(taskInNonCreate.get(k));
					if (k <= taskListSize - 2) {
						errorMessage.append(",");
					}
				}
				errorMessage.append("]");
				throw new Exception(errorMessage.toString());
			}
		}
		catch (Exception e)
		{
			ContextUtil.abortTransaction(context);
			throw (new FrameworkException(e));
		}
		if(debug)
			System.out.println("==========================END   deleteTasks============================================");
	}

	/**
	* Disconnects all the assignees from the task.
	*
	* @param context the user context object for the current session.
	* @throws FrameworkException if operation fails.
	* @since AEF 10.0.0.0
	* @grade 0
	*/
	private void disconnectAssignees(Context context, String taskId)
			throws FrameworkException
	{
		if(debug)
			System.out.println("==========================START   disconnectAssignees============================================");
		StringList busSelects = new StringList(1);
		busSelects.add(SELECT_ID);
		StringList relSelects = new StringList(1);
		relSelects.add(SELECT_RELATIONSHIP_ID);

		task.setId(taskId);

		MapList mapList = task.getAssignees (context,
									busSelects,
									relSelects,
									null);
		if(debug)
			System.out.println("Before disconnect(): "+ContextUtil.isTransactionActive(context) +" "+ mapList.size());

		Iterator itr = mapList.iterator();
		while (itr.hasNext())
		{
			Map map = (Map) itr.next();
			String connectionId = (String) map.get (SELECT_RELATIONSHIP_ID);
			DomainRelationship.disconnect(context, connectionId);
			if(debug)
				System.out.println("After disconnect(): "+ContextUtil.isTransactionActive(context));
		}
		if(debug)
			System.out.println("==========================END   disconnectAssignees============================================");
	}

	/**
    * The values contained in the dependency maplist is MS Project uids
    * these uids change with each operation
    * This function converts the uid into PC taskIds
    *
    * @param context the user context object for the current session.
    * @throws FrameworkException if operation fails.
    * @since AEF 10.0.0.0
    * @grade 0
    */
	private MapList convertDependency(MapList dependencyMapList, Map taskIndexMap, StringList SFDependentTaskList)
			throws FrameworkException
	{
		if(debug)
		   	System.out.println("==========================START   convertDependency============================================");
		MapList pcDependencyMapList = new MapList();
		//the values contained in the maplist is using uid from MS Project
		//convert it to ids of PC and compare the two maplist
		if( SFDependentTaskList == null)
		{
			ListIterator dependencyItr = dependencyMapList.listIterator();
			while (dependencyItr.hasNext())
			{
				Map dependencyMap = (Map) dependencyItr.next();
				String preUID = (String)dependencyMap.get("preUID");
				String taskUID = (String)dependencyMap.get("taskUID");
				String pcPreType = (String)dependencyMap.get("pcPreType");
				String linkLag = (String)dependencyMap.get("linkLag");

				//get the dependency taskId from the map
				String preTaskId = (String) taskIndexMap.get(preUID);
				String taskId = (String) taskIndexMap.get(taskUID);

				//store the map back in terms of pc taskId and predIds
				Map pcDependencyMap = new HashMap();
				pcDependencyMap.put("preId", preTaskId);
				pcDependencyMap.put("taskId", taskId);
				pcDependencyMap.put("pcPreType", pcPreType);
				pcDependencyMap.put("linkLag", linkLag);
				pcDependencyMapList.add(pcDependencyMap);
			}
		}
		else
		{
			ListIterator dependencyItr = dependencyMapList.listIterator();
			while (dependencyItr.hasNext())
			{
				Map dependencyMap = (Map) dependencyItr.next();
				String preUID = (String)dependencyMap.get("preUID");
				String taskUID = (String)dependencyMap.get("taskUID");
				String pcPreType = (String)dependencyMap.get("pcPreType");
				String linkLag = (String)dependencyMap.get("linkLag");

				//get the dependency taskId from the map
				String preTaskId = (String) taskIndexMap.get(preUID);
				String taskId = (String) taskIndexMap.get(taskUID);
					if(pcPreType.equals("SF"))
						SFDependentTaskList.add( taskId);

				//store the map back in terms of pc taskId and predIds
				Map pcDependencyMap = new HashMap();
				pcDependencyMap.put("preId", preTaskId);
				pcDependencyMap.put("taskId", taskId);
				pcDependencyMap.put("pcPreType", pcPreType);
				pcDependencyMap.put("linkLag", linkLag);
				pcDependencyMapList.add(pcDependencyMap);
			}
		}

		if(debug)
   			System.out.println("==========================END   convertDependency============================================");
		return pcDependencyMapList;
	}

    /**
    * The values contained in the dependency maplist is MS Project uids
    * these uids change with each operation
    * This function converts the uid into PC taskIds
    *
    * @param context the user context object for the current session.
    * @throws FrameworkException if operation fails.
    * @since AEF 10.0.0.0
    * @grade 0
    */
	private String getStateName(Context context, String actualName)
				    throws FrameworkException
	{
		String stateName = null;
	    return stateName;
	}

    /**
    * The values contained in the dependency maplist is MS Project uids
    * these uids change with each operation
    * This function converts the uid into PC taskIds
    *
    * @param context the user context object for the current session.
    * @throws FrameworkException if operation fails.
    * @since AEF 10.0.0.0
    * @grade 0
    */
	private String getPersonLastNameFirstName(Context context, String userName)
			throws FrameworkException
	{
		//get the last name "  " first name for the owner
		java.util.Set userList = new java.util.HashSet();
		userList.add(userName);

		StringList personSelect = new StringList();
		String ownerLFName = null;
		personSelect.add(person.SELECT_FIRST_NAME);
		personSelect.add(person.SELECT_LAST_NAME);
		Map ownerInfo = person.getPersonsFromNames(context, userList, personSelect);
		Map ownerMap = (Map) ownerInfo.get(userName);
		if (ownerMap != null)
		{
			ownerLFName = ownerMap.get(person.SELECT_LAST_NAME) + " " +
			ownerMap.get(person.SELECT_FIRST_NAME);
		}
		return ownerLFName;
	}

	/**
	* This method will build the list of all - start date, end date and duration
	* for modification.
	* @param tasks Map of tasks
	* @param taskId the bus-id of the task
	* @param attribute the attribute to be modified
	* @param value attribute value to be modified
	*/

	static public void addUpdate(Map tasks, String taskId, String attribute, Object value)
	{
		Map taskInfo = (Map) tasks.get(taskId);
		if((DomainConstants.ATTRIBUTE_TASK_ESTIMATED_DURATION).equalsIgnoreCase(attribute))
		{
			if(value != null && "0".equalsIgnoreCase(value.toString()))
				taskInfo = null; // To set the task duration to 0 if specified as 0
		}

	    if(taskInfo == null)
		{
			taskInfo = (Map) new HashMap();
	        tasks.put(taskId, taskInfo);
		}

	    taskInfo.put(attribute, value);
	}

	/**
	* This method retrieves allocation percentage from Program Central
	* @param context the user context object for the current session.
	* @param objectId the object id
	* @param sRelPattern the relationship pattern
	* @param sTypePattern the type pattern
	* @param boolGetTo retrive the to side
	* @param boolGetFrom retrieve the from side
	* @param iLevel the recursive level to expand
	* throws FrameworkException if operation fails.
	* @since AEF 10.0.0.0
	*
	*/
	public Vector getAllocation(matrix.db.Context context, String objectid, String sRelPattern, String sTypePattern, boolean boolGetTo, boolean boolGetFrom, short iLevel)
		throws matrix.util.MatrixException
	{
		if(debug)
   			System.out.println("==========================START   getAllocation============================================");
		BusinessObject bo = new BusinessObject(objectid);
		bo.open(context);
		matrix.db.ExpansionWithSelect effortSelect = null;
		matrix.util.SelectList selectObjStmts = new matrix.util.SelectList();
		matrix.util.SelectList selectRelStmts = new matrix.util.SelectList();
		selectObjStmts.addId();

		String rel_allocation=(String)PropertyUtil.getSchemaProperty("attribute_PercentAllocation");
		selectRelStmts.addAttribute(rel_allocation);

		effortSelect = bo.expandSelect(context, sRelPattern, sTypePattern, selectObjStmts,
												selectRelStmts, boolGetTo, boolGetFrom, iLevel);

		matrix.db.RelationshipWithSelectItr relObjectItr = new matrix.db.RelationshipWithSelectItr(effortSelect.getRelationships());

		String percentAllocation = new String();
		Hashtable objectRelAttributes = new Hashtable();
		Hashtable effortBusObjAttributes = new Hashtable();
		Vector vPercentAlloc = new Vector();

		while (relObjectItr.next())
		{
			effortBusObjAttributes =  relObjectItr.obj().getTargetData();
			String sTaskId = (String)effortBusObjAttributes.get("id");
			objectRelAttributes =  relObjectItr.obj().getRelationshipData();
			percentAllocation =(String)objectRelAttributes.get("attribute[" + rel_allocation + "]");
			if (percentAllocation != null && percentAllocation.trim().length() > 0 )
			{
			   HashMap hm = new HashMap();
			   hm.put("id",sTaskId);
			   hm.put("allocation",percentAllocation);
			   vPercentAlloc.addElement(hm);
			}
		}
		bo.close(context);
    	if(debug)
   			System.out.println("==========================END   getAllocation============================================");
		return vPercentAlloc;
	}

	public String getAllocationPercent (Context context, String taskId, String personId) throws Exception
	{
		if(debug)
   			System.out.println("==========================START   getAllocationPercent============================================");
		HashMap ret_alloc = new HashMap();
		String allo = null;

		String type_Person=(String)PropertyUtil.getSchemaProperty("type_Person");

		String rel_assigned=(String)PropertyUtil.getSchemaProperty("relationship_AssignedTasks");

		String attribute_Originator=(String)PropertyUtil.getSchemaProperty("attribute_Originator");

		Vector taskVec = getAllocation(context,taskId,rel_assigned,type_Person,true,false,(short)1);
		String person_name = null;

		for (int i = 0; i < taskVec.size(); i++)
		{
			HashMap hm = (HashMap)taskVec.get(i);
			String sid = (String)hm.get("id");

			allo = (String)hm.get("allocation");

			if(sid!=null && sid.trim().length() > 0 )
			{
				DomainObject obj = new DomainObject(sid);
	            obj.open(context);
		        person_name = obj.getName();
			    if(person_name !=null && personId.equalsIgnoreCase(person_name))
				{
					return allo;
	            }
		    }
	    }
    	if(debug)
   			System.out.println("==========================END   getAllocationPercent============================================");
	   return allo;
  }

  /**
  * Sets the attribute values for resource allocation
  *
  * @param context the user context object for the current session.
  * @param taskId the taskId for the task
  * @param value the value to be set in the attribute
  * @throws FrameworkException if operation fails.
  * @since AEF 10.0.0.0
  * @grade 0
  */
  private void setValues(Context context, String taskId, String personId, String value)
        throws MatrixException
  {
	  if(debug)
	  {
	       	System.out.println("==========================START   setValues============================================");
			System.out.println("[setValues] *********************************");
			System.out.println("[setValues] taskId :" + taskId);
			System.out.println("[setValues] personId :" + personId);
			System.out.println("[setValues] allocation value :" + value);
	  }
	  
	  // Pass Id and rel ID
	  StringList busSelects = new StringList(1);
	  busSelects.add(SELECT_ID);
	  StringList relSelects = new StringList(1);
	  relSelects.add(SELECT_RELATIONSHIP_ID);
	  
	  task.setId(taskId);

	  //Convert the percent value to float
	  Float s = new Float((1.0*100)* (Double.valueOf(value)).doubleValue());
	  value = s.toString();
	 
	  MapList maplist = task.getAssignees (context,
                                         busSelects,
                                         relSelects,
                                         null);			

	  String rel_allocation=(String)PropertyUtil.getSchemaProperty("attribute_PercentAllocation");

	  //Vector objIds = new Vector();
	  //MapList maplistResults = new MapList();
	  for (int i = 0; i < maplist.size(); i++)
	  {
		  //HashMap hm = new HashMap();
		  String idConn =(String)((Map)maplist.get(i)).get("id[connection]");
		  //objIds.addElement(idConn);
		  String id =(String)((Map)maplist.get(i)).get("id");
		  try
		  {
			  DomainRelationship dr = new DomainRelationship(idConn);
			  dr.open(context);

			  //String alloc = dr.getAttributeValue(context,rel_allocation);
			  DomainObject dmo = new DomainObject(id);
			  dmo.open(context);
			  String assigneeName = dmo.getName();

			  DomainObject dmo2 = new DomainObject(personId);
			  dmo2.open(context);
			  String currentUserName = dmo2.getName();
			
			  if (assigneeName.equalsIgnoreCase(currentUserName))
			  {
				  if(debug)
				  {
					  System.out.println("In if - the assigneeName :" + assigneeName + " and currentUserName :" + currentUserName);
					  System.out.println("In if - allocation value :" + value);
				  }			   
				  dr.setAttributeValue(context, rel_allocation, value);					
			  }
			  dr.close(context);
			  dmo.close(context);
			  dmo2.close(context);
         }
		 catch (Exception e)
         {
			 if(debug)
				 System.out.println("Exception in SetValues: "+e);
             throw new MatrixException(codeRegn + e.getMessage());
         }
     }
      if(debug)
    	  System.out.println("==========================END   setValues============================================");
  }

	private boolean mspiCreateTask(Context context, Task task, String taskType, String taskName, Object obj, boolean isProject) throws Exception
	{
		if(debug)
			System.out.println("==========================START   mspiCreateTask============================================");
		boolean retVal = true;
		if(taskType == null || taskType.length() == 0)
		{
			String taskPolicy =  task.getDefaultPolicy(context,DomainConstants.TYPE_TASK);
			//Fix for [Issue ID: 92434] : Project having multilevel tasks cannot be merged to PMC (New Project creation).
			if(isProject)
			{
				ProjectSpace project = (ProjectSpace) obj;
				task.create(context, DomainConstants.TYPE_TASK, taskName, taskPolicy, project);
			}
			else
			{
				Task pTask = (Task) obj;
				task.create(context, DomainConstants.TYPE_TASK, taskName, taskPolicy, pTask);
			}
		}
		else
		{
			//Check task type in database
			boolean doesTaskTypeExist = checkTaskTypeinDB(context, taskType);
			if(doesTaskTypeExist)
			{
				String taskPolicy =(String) tasktypeDefaultPolicyMap.get(taskType);//[: check in the cache]
				if (taskPolicy == null ||taskPolicy.length()==0 )
				{
					taskPolicy =  task.getDefaultPolicy(context,taskType);
					tasktypeDefaultPolicyMap.put(taskType,taskPolicy);//[:cache default task policy, avoid repeated call to task.getDefaultPolicy()]
				}
				if(isProject)
				{
					ProjectSpace project = (ProjectSpace) obj;
					task.create(context, taskType, taskName, taskPolicy, project);
				}
				else
				{
					Task pTask = (Task) obj;
					task.create(context, taskType, taskName, taskPolicy, pTask);
				}
			}
			else
			{
				String taskPolicy =  task.getDefaultPolicy(context,DomainConstants.TYPE_TASK);
				if(isProject)
				{
					ProjectSpace project = (ProjectSpace) obj;
					task.create(context, DomainConstants.TYPE_TASK, taskName, taskPolicy, project);
				}
				else
				{
					Task pTask = (Task) obj;
					task.create(context, DomainConstants.TYPE_TASK, taskName, taskPolicy, pTask);
				}
			}
		}
		if(debug)
			System.out.println("==========================END   mspiCreateTask============================================");
		return retVal;
	}

	private boolean checkTaskTypeinDB(Context context, String taskType)
	{
		if(debug)
			System.out.println("==========================START   checkTaskTypeinDB============================================");
		boolean retVal = false;

		if(DomainConstants.TYPE_TASK.equals(taskType))
			retVal = true;
		else
		{
			try
			{
				com.matrixone.MCADIntegration.server.beans.MCADMxUtil mxUtil = new com.matrixone.MCADIntegration.server.beans.MCADMxUtil(context, null,_GlobalCache);
				String typeExists = mxUtil.executeMQL(context,"list type \"" + taskType + "\"");
				if(typeExists.startsWith("true|") && typeExists.length() > 5)
				{
					retVal = true;
				}
			}
			catch(Exception e)
			{
				if (debug)
					System.out.println("[emxMSProjectIntegration.checkTaskTypeinDB] EXCEPTION : " + e.getMessage());
			}
		}
		if(debug)
			System.out.println("==========================END   checkTaskTypeinDB============================================");
	  return retVal;
	}

		private String i18nStringNow(String text, String languageStr)
    {
		String returnString = text;
		com.matrixone.apps.domain.util.i18nNow  infoLoc = new com.matrixone.apps.domain.util.i18nNow();
			String I18NreturnString = infoLoc.GetString("emxIEFDesignCenterStringResource ", languageStr, text);
		if ((!"".equals(I18NreturnString)) && (I18NreturnString != null)){
			returnString = I18NreturnString.trim();
		}
		return returnString;
    }

	//The following method will be used by emxMsoiCDMSupport JPO
  //To show the context menus "Edit in MSP" and "View in MSP" in CSE file dialog and windows explorer
  public boolean checkAccess(Context context, String busIdToCheck, short accessConstantModify) throws MatrixException
  {
	    com.matrixone.apps.program.ProjectSpace project = (com.matrixone.apps.program.ProjectSpace) DomainObject.newInstance(context, DomainConstants.TYPE_PROJECT_SPACE, "PROGRAM");
		project.setId(busIdToCheck);
		boolean editFlag = project.checkAccess(context, (short) AccessConstants.cModify);
		//IR-057571 Begin Projects in Hold state are editable from MSP application.
		String policyName = project.getInfo(context, SELECT_POLICY);
		if((ProgramCentralConstants.POLICY_PROJECT_SPACE_HOLD_CANCEL).equalsIgnoreCase(policyName))		
			editFlag = false;
		//IR-057571 End 
		return editFlag;
  }
  
  public String CheckAccessOfLoggedInUser(Context context, String[] args) throws MatrixException
  {
	  String busIdToCheckStr = args[0];
	  String accessConstantModify = args[1];
	  com.matrixone.apps.program.ProjectSpace project = (com.matrixone.apps.program.ProjectSpace) DomainObject.newInstance(context, DomainConstants.TYPE_PROJECT_SPACE, "PROGRAM");
	  project.setId(busIdToCheckStr);
	  boolean readFlag = project.checkAccess(context, (short) AccessConstants.cRead);
	  if(readFlag == false)
		  return "none";
	  if(checkAccess(context,busIdToCheckStr,new Short(accessConstantModify).shortValue()) == true)
		  return "edit";
	  else
		  return "view";
  }
  
  public boolean checkAccess(Context context, String[] args) throws MatrixException
  {
      HashMap argsMap = null;
	  String busIdToCheckStr     = null;
	  String accessConstantModify           =null;

	  try
	  {
		   argsMap          = (HashMap)JPO.unpackArgs(args);
		    busIdToCheckStr     = (String)argsMap.get("busIdToCheck");
            accessConstantModify           = (String)argsMap.get("accessConstantModify");
	  }catch(Exception e )
	   {}
	   return  checkAccess(context,busIdToCheckStr,new Short(accessConstantModify).shortValue());
   }
   
   //[Added to support creation of Projects of Sub types from MSProject]
   private boolean mspiCreateProject(Context context, ProjectSpace project, String projectType, String projectName, String vault) throws Exception
	{
		if(debug)
			System.out.println("==========================START   mspiCreateProject============================================");
		boolean retVal = true;
		if(projectType == null || projectType.length() == 0)
		{	 
			//In 2011x PRG's Default Project Policy is "Project Space Hold Cancel". However in MS Project there is no support for "Project Space Hold Cancel", 
			//therefore String projectPolicy is replaced by DomainConstants.TYPE_PROJECT_SPACE 
			
			//[Commented in 2011x]
			//String projectPolicy =  project.getDefaultPolicy(context,DomainConstants.TYPE_PROJECT_SPACE);			
			//project.create(context,DomainConstants.TYPE_PROJECT_SPACE, projectName, projectPolicy, vault);	
			//[Commented in 2011x]
			
			project.create(context,DomainConstants.TYPE_PROJECT_SPACE, projectName, DomainConstants.TYPE_PROJECT_SPACE, vault);
		}
		else
			{
				boolean doesProjectTypeExist = checkProjectTypeinDB(context, projectType);
				if(doesProjectTypeExist && isDerivedType(context,DomainConstants.TYPE_PROJECT_SPACE,projectType) == true)
				{
					//String projectPolicy =  project.getDefaultPolicy(context,projectType);
					//project.create(context, projectType, projectName, projectPolicy, vault);
					project.create(context,projectType, projectName, DomainConstants.TYPE_PROJECT_SPACE, vault);
				}
				else
				{
					//String projectPolicy =  project.getDefaultPolicy(context,DomainConstants.TYPE_PROJECT_SPACE);
					//project.create(context, DomainConstants.TYPE_PROJECT_SPACE, projectName, projectPolicy, vault);
					project.create(context, DomainConstants.TYPE_PROJECT_SPACE, projectName, DomainConstants.TYPE_PROJECT_SPACE, vault);
				}
			}
		
		//To set currency to project as per user's set currency 
		String sPreferredCurrency = PersonUtil.getCurrency(context);
		if(sPreferredCurrency != null)
		{
			if (sPreferredCurrency.equals("") || sPreferredCurrency.equals("As Entered") || sPreferredCurrency.equals("Unassigned")) 
				project.setAttributeValue(context, ATTRIBUTE_CURRENCY, "Dollar");
			else 
				project.setAttributeValue(context, ATTRIBUTE_CURRENCY, sPreferredCurrency);
		}
		  
			if(debug)
				System.out.println("==========================END   mspiCreateProject============================================");
			return retVal;
	}
   
   	private boolean checkProjectTypeinDB(Context context, String projectType)
	{
   		if(debug)
   			System.out.println("==========================START   checkProjectTypeinDB============================================");
   		boolean retVal = false;
   		if(DomainConstants.TYPE_PROJECT_SPACE.equals(projectType))
   			retVal = true;
   		else
		{
			try
			{
				com.matrixone.MCADIntegration.server.beans.MCADMxUtil mxUtil = new com.matrixone.MCADIntegration.server.beans.MCADMxUtil(context, null,_GlobalCache);
				String typeExists = mxUtil.executeMQL(context, "list type \"" + projectType + "\"");
				if(typeExists.startsWith("true|") && typeExists.length() > 5)
				{
					retVal = true;
				}
			}
			catch(Exception e)
			{
				if (debug)
				{
					System.out.println("[emxMSProjectIntegration.checkTaskTypeinDB] EXCEPTION : " + e.getMessage());
				}
			}
		}
   		if(debug)
   			System.out.println("==========================END   checkProjectTypeinDB============================================");
	  return retVal;
	}
   	private  boolean isDerivedType(Context context, String parentType, String objectType) throws MatrixException
   	{
   		// Check if objectType is same as parentType
   		if(parentType.equals(objectType))
   		{
   			return true;
   		}
		// variable to store whether the current object type is either parentType or its derived
		// Initialize to false
		boolean bIsDerivedType = false;
	
		if (ProjectspaceSubtypes == null)
		{
			// Command to retrieve derived types of parentType
			MQLCommand mqlCommand = new MQLCommand();
	
			//  The MQLCommand returns all the Derived type of ParentType
			// Creating MQL Command String for retrieving all the derived types of 'parentType'
			String mqlDocumentTypeCheck = new String("print type '" + parentType + "' select derivative dump |;");
			// Executing Command, return value is true if the Command has Derived types. False otherwise.
			boolean bReturnVal = mqlCommand.executeCommand(context, mqlDocumentTypeCheck);
			// variable to store the MQL result String
			String result = null;
	
			//MQLCommand returned false
			if (!bReturnVal)
			{	
				result = "";
			} 
			//MQLCommand returned true
			else 
			{
				// Get all derived types of 'parentType'
				result = mqlCommand.getResult();
				// Exit if result is null or Blank
					if ((result == null) || (result.equals("")))
				{
					throw new MatrixException("Null value returned.");
				}
				//Proper result returned. Process
				if(result.endsWith("\n"))
				{	
					//Trim Result
					result = result.substring(0, (result.lastIndexOf("\n")));
					// Check if the current Object type is present in MQL result
					// which means the current object type is derived from 'parentType'
					// set the variable to true
					ProjectspaceSubtypes = FrameworkUtil.split(result, "|");
					int i = ProjectspaceSubtypes.indexOf(objectType);
					if(i>=0)
					bIsDerivedType = true;
					/*if(result.indexOf(objectType) != -1)
					{
					bIsDerivedType = true;
					}*/
				}
			}
		}
		else
			{	
				int i = ProjectspaceSubtypes.indexOf(objectType);
				if(i>=0)
				bIsDerivedType = true;
		}
		return bIsDerivedType;
   	}
	
   	//[/Added  to support creation of Projects of Sub types from MSProject]
	private  boolean isProjectSpace(Context context,  String projectType) throws MatrixException
	{
		return isDerivedType(context,DomainConstants.TYPE_PROJECT_SPACE,projectType);
	}
	
	public  String GetProjectSpaceTypes(Context context, String[] args) throws MatrixException
	{
		MQLCommand mqlCommand = new MQLCommand();

		//  The MQLCommand returns all the Derived type of ParentType
		// Creating MQL Command String for retrieving all the derived types of 'parentType'
		String mqlDocumentTypeCheck = new String("print type '" + DomainConstants.TYPE_PROJECT_SPACE + "' select derivative dump |;");
		// Executing Command, return value is true if the Command has Derived types. False otherwise.
		boolean bReturnVal = mqlCommand.executeCommand(context, mqlDocumentTypeCheck);
		// variable to store the MQL result String
		String result = null;

		//MQLCommand returned false
		if (!bReturnVal)
		{
			result = "";//setting result to blank string
		} 
		//MQLCommand returned true
		else 
		{
			// Get all derived types of 'parentType'
			result = mqlCommand.getResult();
			// Exit if result is null or Blank
			if ((result == null) || (result.equals("")))
			{
				throw new MatrixException("Null value returned.");
			}
			//Proper result returned. Process
			if(result.endsWith("\n"))
			{
				result = result.substring(0, (result.lastIndexOf("\n")));//Trim Result				
			}
		}
		return  DomainConstants.TYPE_PROJECT_SPACE +"|"+ result;
	}

	private boolean mspiCreateTask(Context context, Task task, String taskType, String taskName, Object obj, boolean isProject, String nextTaskId) throws Exception
	{
		if(nextTaskId != null)
		{
			if (nextTaskId.equals(""))
		{
			nextTaskId=null;
		}
		}

		boolean retVal = true;
		if(taskType == null || taskType.length() == 0)
		{
			String taskPolicy =  task.getDefaultPolicy(context,DomainConstants.TYPE_TASK);

			//Fix for [Issue ID: 92434] : Project having multilevel tasks cannot be merged to PMC (New Project creation).
			if(isProject)
			{
				ProjectSpace project = (ProjectSpace) obj;
				task.create(context, DomainConstants.TYPE_TASK, taskName, taskPolicy, project,nextTaskId);
			}
			else
			{
				Task pTask = (Task) obj;
				task.create(context, DomainConstants.TYPE_TASK, taskName, taskPolicy, pTask,nextTaskId);
			}
		}
		else
		{
			//Check task type in database
			boolean doesTaskTypeExist = checkTaskTypeinDB(context, taskType);
			if(doesTaskTypeExist)
			{
				String taskPolicy =(String) tasktypeDefaultPolicyMap.get(taskType);//[: check in the cache]
				if (taskPolicy == null ||taskPolicy.length()==0 )
				{
					taskPolicy =  task.getDefaultPolicy(context,taskType);
					tasktypeDefaultPolicyMap.put(taskType,taskPolicy);//[:cache default task policy, avoid repeated call to task.getDefaultPolicy()]
				}
				if(isProject)
				{
					ProjectSpace project = (ProjectSpace) obj;
					task.create(context, taskType, taskName, taskPolicy, project,nextTaskId);
				}
				else
				{
					Task pTask = (Task) obj;
					task.create(context, taskType, taskName, taskPolicy, pTask,nextTaskId);
				}
					} else
			{
				String taskPolicy =  task.getDefaultPolicy(context,DomainConstants.TYPE_TASK);
				if(isProject)
				{
					ProjectSpace project = (ProjectSpace) obj;
					task.create(context, DomainConstants.TYPE_TASK, taskName, taskPolicy, project,nextTaskId);
				}
				else
				{
					Task pTask = (Task) obj;
					task.create(context, DomainConstants.TYPE_TASK, taskName, taskPolicy, pTask,nextTaskId);
				}
			}
		}
		return retVal;
	}
	//374646 Begin
	String getNextTaskId(String outlineNumber,Map NextOutlineMap, Map TaskLevelMap, Map TaskLevelMapOld, Map  OIDMap, Map OIDMapOld)
	{
		String NextOutlineNo = (String)NextOutlineMap.get(outlineNumber);
		String nextTaskId=null;
		String nextLevelId;
		String levelId = null;
		String level = null;
		String p1 = null;
		String p2 = null;
		if(outlineNumber.lastIndexOf(".") >0)
			levelId = outlineNumber.substring(0, outlineNumber.lastIndexOf("."));	   
		while(NextOutlineNo!=null)
		{
			boolean sibs = false;
			if (NextOutlineNo.lastIndexOf(".") == -1 &&outlineNumber.lastIndexOf(".") == -1)
			{
				sibs = true;
			}
			else if (NextOutlineNo.lastIndexOf(".") != -1 && outlineNumber.lastIndexOf(".") != -1)
			{
				nextLevelId = NextOutlineNo.substring(0, NextOutlineNo.lastIndexOf("."));
				if(nextLevelId.equals(levelId))
					sibs = true;
			}
			if(sibs)
			{
				nextTaskId = (String)TaskLevelMap.get(NextOutlineNo);
				if(nextTaskId == null ||nextTaskId.trim()=="" )
				{							
					NextOutlineNo = (String)NextOutlineMap.get(NextOutlineNo);
				}
				else
				{
					String OL = (String) OIDMap.get(nextTaskId);
					String OLOld = (String) OIDMapOld.get(nextTaskId);
					if(OL !=null && OLOld != null)
					{
						if(OLOld.equals(OL))
						{
							if( OL.indexOf(".") != -1 )
							{
							   level = OL.substring(0,OL.lastIndexOf("."));
								p1 = (String)TaskLevelMap.get(level);
								p2 = (String)TaskLevelMapOld.get(level);
								if(p1 ==null ||p2 == null || p1.equals(p2))
									break;
								else
								  NextOutlineNo = (String)NextOutlineMap.get(NextOutlineNo);
							}
							else
							{
								break;
							}
						}
						else
						{
							NextOutlineNo = (String)NextOutlineMap.get(NextOutlineNo);
						}						
					}
					else
						NextOutlineNo = (String)NextOutlineMap.get(NextOutlineNo);
				}
			}
			else
			{
				NextOutlineNo = (String)NextOutlineMap.get(NextOutlineNo);
			}
		}

		if(NextOutlineNo == null ||NextOutlineNo.equals(""))
		{	
			nextTaskId = null;			
		}
		return nextTaskId;
	}
//374646 end
	 
	 String GetConstraintType(String constraintTypeShortValue)
	 {
		 String sConstrintType = "";
		 if(constraintTypeShortValue.equals("0"))
			 sConstrintType = "As Soon As Possible";
		 else if(constraintTypeShortValue.equals("1"))
			 sConstrintType = "As Late As Possible";
		 else if(constraintTypeShortValue.equals("2"))
			 sConstrintType = "Must Start On";
		 else if(constraintTypeShortValue.equals("3"))
			 sConstrintType = "Must Finish On";
		 else if(constraintTypeShortValue.equals("4"))
			 sConstrintType = "Start No Earlier Than";
		 else if(constraintTypeShortValue.equals("5"))
			 sConstrintType = "Start No Later Than";
		 else if(constraintTypeShortValue.equals("6"))
			 sConstrintType = "Finish No Earlier Than";
		 else if(constraintTypeShortValue.equals("7"))
			 sConstrintType = "Finish No Later Than";		
		 return sConstrintType;
	 }
	 
	 String GetConstraintEnumValues(String sConstraintType)
	 {
		 String sConstantValue ="";
		 if(sConstraintType.equals("As Soon As Possible"))
			 sConstantValue="0";
		 else if(sConstraintType.equals("As Late As Possible"))
			 sConstantValue="1";
		 else if(sConstraintType.equals("Must Start On"))
			 sConstantValue="2";
		 else if(sConstraintType.equals("Must Finish On"))
			 sConstantValue="3";
		 else if(sConstraintType.equals("Start No Earlier Than"))
			 sConstantValue="4";
		 else if(sConstraintType.equals("Start No Later Than"))
			 sConstantValue="5";
		 else if(sConstraintType.equals("Finish No Earlier Than"))
			 sConstantValue="6";
		 else if(sConstraintType.equals("Finish No Later Than"))
			 sConstantValue="7";
		 else 	
			 sConstantValue=null;	
		 return sConstantValue;
	 }
	 
	 public String GetSpecialCharacters(Context context, String[] args) throws MatrixException
	 {
		//Read the charset value from the emxSystem.properties file		 
		String emxNameBadChars = FrameworkProperties.getProperty("emxFramework.Javascript.NameBadChars");
		
		if ((emxNameBadChars == null) || (emxNameBadChars.equals("")))
		{
			throw new MatrixException("emxFramework.Javascript.BadChars property is null/not set");
		}
		 return emxNameBadChars;
	 }
	 
	 public String ChangeEstSchedulingOnActual(Context context, String[] args) throws MatrixException
	 {
		// If yes then change estimated scheduling to actual otherwise not
		String sChangeScheduling = "Yes";

		ResourceBundle mcadIntegrationBundle = ResourceBundle.getBundle("emxProgramCentral");
		
		try {
			sChangeScheduling = mcadIntegrationBundle
					.getString("emxProgramCentral.ChangeEstimatedSchedulingOnActualDates");
		} catch (MissingResourceException _ex) {
			sChangeScheduling = "Yes";
		}
		return sChangeScheduling;
	}
}
