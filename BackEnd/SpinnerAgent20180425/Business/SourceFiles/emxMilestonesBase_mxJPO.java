/*
 ** emxMilestoneBase
 **
 **Copyright (c) 1993-2015 Dassault Systemes.
 ** All Rights Reserved.getActiveManufacturingPlans
 **This program contains proprietary and trade secret information of
 **Dassault Systemes.
 **Copyright notice is precautionary only and does not evidence any actual
 **or intended publication of such program
 */
 
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.StringUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.program.Milestone;
import com.matrixone.apps.program.ProgramCentralConstants;
import com.matrixone.apps.program.ProgramCentralUtil;


/**
 * This JPO class has some methods pertaining to Milestone Track.
 *
 * @author IXE
 * @since PRG R215
 */

public class emxMilestonesBase_mxJPO extends emxTask_mxJPO
{
	public static final String SUITE_KEY ="ProgramCentral";
	 protected static final String FIELD_DISPLAY_CHOICES = "field_display_choices";

	 /** A string constant with the value field_choices. */
	protected static final String FIELD_CHOICES = "field_choices";
	// public static MQLCommand mqlCommand = null;

	/**
     * Constructs a new emxTask JPO object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - String containing the id
     * @throws Exception if the operation fails
     * @since AEF 9.5.1.1
     */
    public emxMilestonesBase_mxJPO (Context context, String[] args)
        throws Exception
    {
        // Call the super constructor
        super(context,args);
        if (args != null && args.length > 0)
        {
            setId(args[0]);
        }
    }



    /**
     * gets all Milestones to dispaly in the Project Milestone summary table
     *
     * @param context the ENOVIA <code>Context</code> object
     * @param args holds the HashMap containing the
	 *            following arguments
     * @return Object - MapList containing the ids of all Milestones of the project
     * @throws Exception
	 *             if the operation fails
     */

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getMilestonesAllSubtasks(Context context, String[] args) throws Exception {
    	com.matrixone.apps.program.Task task = (com.matrixone.apps.program.Task) DomainObject.newInstance(context, DomainConstants.TYPE_TASK, "PROGRAM");
    	HashMap arguMap = (HashMap)JPO.unpackArgs(args);
    	String strObjectId = (String) arguMap.get("objectId");
    	String selectedTable = (String) arguMap.get("selectedTable");
    	MapList wbsSubTasks = new MapList();
    	String strExpandLevel = (String) arguMap.get("expandLevel");
    	short nExpandLevel = ProgramCentralUtil.getExpandLevel(strExpandLevel);

    	task.setId(strObjectId);

    	StringList objectSelects = new StringList(2);
    	StringList relationshipSelects = new StringList(4);
    	objectSelects.addElement(DomainConstants.SELECT_ID);
    	objectSelects.addElement(DomainConstants.SELECT_NAME);
    	relationshipSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
    	relationshipSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_TYPE);
    	relationshipSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_NAME);
    	relationshipSelects.addElement(DomainConstants.SELECT_LEVEL);

    	if(task.isKindOf(context, DomainConstants.TYPE_PROJECT_SPACE)||task.isKindOf(context, DomainConstants.TYPE_PROJECT_CONCEPT) || task.isKindOf(context, TYPE_TASK_MANAGEMENT)){
    		wbsSubTasks = getMilestonesTasks(context,strObjectId,DomainConstants.RELATIONSHIP_SUBTASK+","+DomainConstants.RELATIONSHIP_DELETED_SUBTASK,nExpandLevel,null);
    	}
    	
    	for(int i=0;i<wbsSubTasks.size();i++){
    		Map mapTask = (Map)wbsSubTasks.get(i);
    		String strlevel = (String)mapTask.get(DomainConstants.SELECT_LEVEL);
    		int level = Integer.parseInt(strlevel);
    		if(level!=1){
    			mapTask.put(DomainConstants.SELECT_LEVEL, "1");
    		}
    	}
    	return wbsSubTasks;
    }


    /**
     * gets all Milestones Tracks of the Product
     *
     * @param context the ENOVIA <code>Context</code> object
     * @param args holds the HashMap containing the
	 *            following arguments
     * @return Object - MapList containing the ids of all Milestones of the project
     * @throws Exception
	 *             if the operation fails
     */

 protected MapList getMilestonesTasks(Context context, String objectId, String relPattern,short nExpandLevel,String strMilestoneDiscipline)
 {
	 MapList mapList = new MapList();
	 MapList mlReturn = new MapList();
	 try {
		 String rootNode = objectId;
		 StringList rootNodeSelects = new StringList(3);
		 DomainObject rootNodeObj = DomainObject.newInstance(context, rootNode);
		 String typePattern=null;
		 boolean getFrom = false;
		 boolean getTo = false;
		 String busWhereClause = null;
		 String relWhereClause =null;
		 String postRelPattern = null;
		 String postTypePattern = null;
		 Map postPatterns = null;
		 String strDirection = null;

		 // Object and Relationship selects

		  StringList objectSelects = new StringList(5);
		 StringList relationshipSelects = new StringList(4);
		 objectSelects.addElement(DomainConstants.SELECT_ID);
		 objectSelects.addElement(DomainConstants.SELECT_NAME);
		 objectSelects.addElement(DomainConstants.SELECT_TYPE);
		 if (nExpandLevel != 0)
		 {
			 //selectable to determine if task is summary to display plus sign in SB.
			 objectSelects.addElement("from[" + relPattern + "]");
		 }
		 relationshipSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

		 typePattern = ProgramCentralConstants.TYPE_MILESTONE;
		 getFrom = true;
		 strDirection = "from";
		 if(null != strMilestoneDiscipline && !"null".equals(strMilestoneDiscipline) && !"".equals(strMilestoneDiscipline)){
			 String strProject = rootNodeObj.getInfo(context,SELECT_ID);
			 Milestone Mile = new Milestone();
			 MapList mlProduct = Mile.getRealtedProjectsProducts(context,strProject);
			 for(int i=0;i<mlProduct.size();i++){
				 Map mapProduct = (Map)mlProduct.get(i);
				 String strProduct = (String)mapProduct.get(SELECT_ID);
				 MapList mlMT = Mile.getMilestoneTracksofProduct(context,strProduct,strMilestoneDiscipline);

				 for(int m=0;m<mlMT.size();m++){
					 Map mapMT = (Map)mlMT.get(m);
					 String strMT = (String)mapMT.get(SELECT_ID);
					 DomainObject domMT= new DomainObject(strMT);

					 StringList objectSelects1 = new StringList(2);
			   		 objectSelects1.add(SELECT_ID);
			   		 objectSelects1.add(SELECT_NAME);
			   		 objectSelects1.add(SELECT_TYPE);
			   		 StringList relationshipSelects1 = new StringList();

					 mapList = domMT.getRelatedObjects(context,
				   				ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM,
				   				 DomainConstants.QUERY_WILDCARD,
				   				 objectSelects1,
				   				 relationshipSelects1,
				   				 false,	//to relationship - changed
				   				 true,	//from relationship
				   				 (short)1,
				   				 DomainConstants.EMPTY_STRING,
				   				 DomainConstants.EMPTY_STRING,
				   				 0);
				 }
			 }
		 }else{
			 mapList = rootNodeObj.getRelatedObjects(context,
					 relPattern,
					 "*",
					 getTo,
					 getFrom,
					 0,
					 objectSelects,
					 relationshipSelects,
					 busWhereClause,
					 relWhereClause,
					 postRelPattern,
					 postTypePattern,
					 postPatterns) ;
		 }


		for(int i=0;i<mapList.size();i++){
			Map mapTask = (Map)mapList.get(i);
			String strTask = (String)mapTask.get(SELECT_ID);
			DomainObject domTask = new DomainObject(strTask);
			if(domTask.isKindOf(context, ProgramCentralConstants.TYPE_MILESTONE)){
				mlReturn.add(mapTask);
			}
		}
	 }
	 catch(Exception e)
	 {
		 e.printStackTrace();
		 throw e;
	 }
	 finally
	 {
		 return mlReturn;
	 }
 }


 /**
  * Decides the Discipline column should be displayed or not in the milestones table of Project
  *
  * @param context the ENOVIA <code>Context</code> object
  * @param args holds the HashMap containing the
	 *            following arguments
  * @return Object - MapList containing the ids of all Milestones of the project
  * @throws Exception
	 *             if the operation fails
  */

 public boolean isDisciplineViewAllowed(Context context,String args[])
 {
	 boolean bView = false;
	 try {
		 HashMap inputMap = (HashMap)JPO.unpackArgs(args);
		 String strObjectId = (String) inputMap.get("objectId");
		 DomainObject dmoObject = DomainObject.newInstance(context,strObjectId);
		 String relatedProjectRelationship = PropertyUtil.getSchemaProperty(context, DomainObject.SYMBOLIC_relationship_RelatedProjects);
		 String selectable = "to[" + relatedProjectRelationship + "].from.id";
		 StringList objectSelects = new StringList(2);
		 objectSelects.addElement(DomainConstants.SELECT_ID);
		 objectSelects.addElement(DomainConstants.SELECT_NAME);
		 StringList relationshipSelects = new StringList();
		 StringList slResult = new StringList();

		 slResult = dmoObject.getInfoList(context, selectable);

		 MapList relatedProducts = dmoObject.getRelatedObjects(context,
				 relatedProjectRelationship,
				 DomainConstants.QUERY_WILDCARD,
				 objectSelects,
				 relationshipSelects,
				 true,	//to relationship
				 false,	//from relationship
				 (short)1,
				 DomainConstants.EMPTY_STRING,
				 DomainConstants.EMPTY_STRING,
				 0);

		 if(relatedProducts.size() != 0){
			 bView = true;
		 }

	 } catch (Exception e) {
		 e.printStackTrace();
	 }
	 return bView;
 }


 /**
  * Gets the Engineering discipline milestones in the Milestones table of Project
  *
  * @param context the ENOVIA <code>Context</code> object
  * @param args holds the HashMap containing the
	 *            following arguments
  * @return Object - MapList containing the ids of all Milestones of the project
  * @throws Exception
	 *             if the operation fails
  */

 @com.matrixone.apps.framework.ui.ProgramCallable
 public MapList getEngineeringMilestones(Context context, String[] args) throws Exception {
	 com.matrixone.apps.program.Task task = (com.matrixone.apps.program.Task) DomainObject.newInstance(context, DomainConstants.TYPE_TASK, "PROGRAM");
	 HashMap arguMap = (HashMap)JPO.unpackArgs(args);
	 String strObjectId = (String) arguMap.get("objectId");
	 String selectedTable = (String) arguMap.get("selectedTable");
	 MapList wbsSubTasks = new MapList();
	 String strExpandLevel = (String) arguMap.get("expandLevel");
	 String strMilestoneDiscipline = "Engineering";
	 short nExpandLevel = ProgramCentralUtil.getExpandLevel(strExpandLevel);
	 task.setId(strObjectId);

	 StringList objectSelects = new StringList(2);
	 StringList relationshipSelects = new StringList(4);

	 objectSelects.addElement(DomainConstants.SELECT_ID);
	 objectSelects.addElement(DomainConstants.SELECT_NAME);
	 relationshipSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
	 relationshipSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_TYPE);
	 relationshipSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_NAME);
	 relationshipSelects.addElement(DomainConstants.SELECT_LEVEL);

	 if(task.isKindOf(context, DomainConstants.TYPE_PROJECT_SPACE)||task.isKindOf(context, DomainConstants.TYPE_PROJECT_CONCEPT) || task.isKindOf(context, TYPE_TASK_MANAGEMENT)){
		 wbsSubTasks = getMilestonesTasks(context,strObjectId,DomainConstants.RELATIONSHIP_SUBTASK+","+DomainConstants.RELATIONSHIP_DELETED_SUBTASK,nExpandLevel,strMilestoneDiscipline);
	 }
 	for(int i=0;i<wbsSubTasks.size();i++){
		Map mapTask = (Map)wbsSubTasks.get(i);
		String strlevel = (String)mapTask.get(DomainConstants.SELECT_LEVEL);
		int level = Integer.parseInt(strlevel);
		if(level!=1){
			mapTask.put(DomainConstants.SELECT_LEVEL, "1");
		}
	}
	 return wbsSubTasks;
 }

 /**
  * Gets the Manufacturing discipline milestones in the Milestones table of Project
  *
  * @param context the ENOVIA <code>Context</code> object
  * @param args holds the HashMap containing the
	 *            following arguments
  * @return Object - MapList containing the ids of all Milestones of the project
  * @throws Exception
	 *             if the operation fails
  */
 @com.matrixone.apps.framework.ui.ProgramCallable
 public MapList getManufacturingMilestones(Context context, String[] args) throws Exception {
	 com.matrixone.apps.program.Task task = (com.matrixone.apps.program.Task) DomainObject.newInstance(context, DomainConstants.TYPE_TASK, "PROGRAM");
	 HashMap arguMap = (HashMap)JPO.unpackArgs(args);
	 String strObjectId = (String) arguMap.get("objectId");
	 String selectedTable = (String) arguMap.get("selectedTable");
	 MapList wbsSubTasks = new MapList();
	 String strExpandLevel = (String) arguMap.get("expandLevel");
	 String strMilestoneDiscipline = "Manufacturing";
	 short nExpandLevel = ProgramCentralUtil.getExpandLevel(strExpandLevel);
	 task.setId(strObjectId);
	 StringList objectSelects = new StringList(2);
	 StringList relationshipSelects = new StringList(4);

	 objectSelects.addElement(DomainConstants.SELECT_ID);
	 objectSelects.addElement(DomainConstants.SELECT_NAME);
	 relationshipSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
	 relationshipSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_TYPE);
	 relationshipSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_NAME);
	 relationshipSelects.addElement(DomainConstants.SELECT_LEVEL);

	 if(task.isKindOf(context, DomainConstants.TYPE_PROJECT_SPACE)||task.isKindOf(context, DomainConstants.TYPE_PROJECT_CONCEPT) || task.isKindOf(context, TYPE_TASK_MANAGEMENT)){
		 wbsSubTasks = getMilestonesTasks(context,strObjectId,DomainConstants.RELATIONSHIP_SUBTASK+","+DomainConstants.RELATIONSHIP_DELETED_SUBTASK,nExpandLevel,strMilestoneDiscipline);
	 }
 	for(int i=0;i<wbsSubTasks.size();i++){
		Map mapTask = (Map)wbsSubTasks.get(i);
		String strlevel = (String)mapTask.get(DomainConstants.SELECT_LEVEL);
		int level = Integer.parseInt(strlevel);
		if(level!=1){
			mapTask.put(DomainConstants.SELECT_LEVEL, "1");
		}
	}
	 return wbsSubTasks;
 }

 /**
  * Creates Milestone Track object when Discipline column value in Milestones table is edited
  *
  * @param context the ENOVIA <code>Context</code> object
  * @param args holds the HashMap containing the
	 *            following arguments
  * @return Object - MapList containing the ids of all Milestones of the project
  * @throws Exception
	 *             if the operation fails
  */
 public void createMilestoneTrack(Context context, String[] args)throws Exception {
	 try{
		 HashMap arguMap = (HashMap)JPO.unpackArgs(args);
		 Map mpParamMap = (HashMap)arguMap.get("paramMap");
		 Map requestMap = (HashMap)arguMap.get("requestMap");


		 String strProjectId = (String) requestMap.get("objectId");
		 String strProjectMilestoneObjectId = (String) mpParamMap.get("objectId");
		 String NewValue = (String)mpParamMap.get("New Value");
		 MapList mlClubedMilestones = new MapList();
		 int index=0;
		 int dateindex=0;
		 StringList objectSelects = new StringList(2);
		 objectSelects.addElement(SELECT_ID);
		 objectSelects.addElement(SELECT_NAME);
		 
		 StringList objSelectablesMS = new StringList();
		 objSelectablesMS.addAll(objectSelects);
		 objSelectablesMS.add(ProgramCentralConstants.SELECT_ATTRIBUTE_MILESTONE_DATE);
		 objSelectablesMS.add("to["+ProgramCentralConstants.RELATIONSHIP_FIRST_CONFIGURATION_ITEM+"].id");
		
		 StringList relationshipSelects = new StringList();
		 relationshipSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);

		 DomainObject domProjectMilestone = new DomainObject(strProjectMilestoneObjectId);
		 String strMilestoneState = domProjectMilestone.getInfo(context,SELECT_CURRENT);
		 if("Complete".equalsIgnoreCase(strMilestoneState)){
				String sErrorMsg=EnoviaResourceBundle.getProperty(context,SUITE_KEY,"emxProgramCentral.Alert.CannotEditMilestoneDiscipline",context.getSession().getLanguage());
				emxContextUtil_mxJPO.mqlError(context,sErrorMsg);
			}else{

		 //Get the products connected to Project by Related Projects or Governing Project relationship
		 Milestone Mile = new Milestone();
		 MapList mlProducts = Mile.getRealtedProjectsProducts(context,strProjectId);

		 if(!"None".equals(NewValue)){

			 if(mlProducts.size() != 0 ){
				 String strProductId = "";
				 String strProductMilestoneTrack = "";
				 for(int i=0;i<mlProducts.size();i++){
					 Map map = (Map)mlProducts.get(i);
					 strProductId = (String)map.get(SELECT_ID);
					 DomainObject domProductId = new DomainObject(strProductId);
					 
					 String MilestoneTrackDiscipline = NewValue ;
					 String busWhereClause = "(attribute[" + ProgramCentralConstants.ATTRIBUTE_MILESTONE_TRACK_DISCIPLINE + "]" + "== \"" + MilestoneTrackDiscipline + "\")" ;

					 //Get the Milestone Track object ids connected with Product
					 MapList mlProductMilestoneTrack = domProductId.getRelatedObjects(context,
							 ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_CRITERIA,
							 ProgramCentralConstants.TYPE_MILESTONE_TRACK,
							 objectSelects,
							 relationshipSelects,
							 false,	//to relationship
							 true,	//from relationship
							 (short)1,
							 busWhereClause,
							 DomainConstants.EMPTY_STRING,
							 0);

					 String strMilestoneDiscipline = "";
					 if("Manufacturing".equals(MilestoneTrackDiscipline)){
						 strMilestoneDiscipline = "Manufacturing";
					 }else if("Engineering".equals(MilestoneTrackDiscipline)){
						 strMilestoneDiscipline = "Engineering";
					 }

					 MapList mlEngOrManfMilestoneTrack = Mile.getMilestoneTracksofProduct(context,strProductId,strMilestoneDiscipline);

					 if(mlProductMilestoneTrack.size() == 0 ){
						 //Get autogenerated Milestone Track name
						 String autoName = "";
						 String strObjectGeneratorName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE,
								 ProgramCentralConstants.TYPE_MILESTONE_TRACK, true);
						 autoName = DomainObject.getAutoGeneratedName(context,strObjectGeneratorName,null);
						 DomainObject productMilestoneTrackObj = DomainObject.newInstance(context);
						 //Creating Milestone Track object
						 productMilestoneTrackObj.createObject(context, ProgramCentralConstants.TYPE_MILESTONE_TRACK, autoName, "", ProgramCentralConstants.POLICY_MILESTONE_TRACK, context.getVault().getName());

						 if(null != productMilestoneTrackObj){
							 productMilestoneTrackObj.setAttributeValue(context, ProgramCentralConstants.ATTRIBUTE_MILESTONE_TRACK_DISCIPLINE, NewValue);
						 }
						//Push context as no fromconnect access to Configuration Manager in Product policy.
						 ProgramCentralUtil.pushUserContext(context); 
						 DomainRelationship.connect(context, domProductId,ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_CRITERIA, productMilestoneTrackObj);
						 ProgramCentralUtil.popUserContext(context);
						 //If the milestone is first time edited to Engneering and then again edited to Manufacturing, the exisitng connection is deleted and then the new is done.

						 //Get the Relationship Id between Milestone Track object and WBS Milestone object
						 MapList mlMT = domProjectMilestone.getRelatedObjects(context,
								 ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM,
								 DomainConstants.QUERY_WILDCARD,
								 objectSelects,
								 relationshipSelects,
								 true,	//to relationship - changed
								 false,	//from relationship
								 (short)1,
								 DomainConstants.EMPTY_STRING,
								 DomainConstants.EMPTY_STRING,
								 0);

						 for(int m=0;m<mlMT.size();m++){
							 Map mapMT = (Map)mlMT.get(m);
							 String strRelMT = (String)mapMT.get(SELECT_RELATIONSHIP_ID);
							 if(null != strRelMT || "".equals(strRelMT) || !"null".equals(strRelMT) ){
								 DomainRelationship.disconnect(context,strRelMT);
							 }
						 }
						 //Giving inherited access Read ,show to display milestone objects to Product Manager who is not Project User.
						 strProductMilestoneTrack = productMilestoneTrackObj.getId(context);
						 ProgramCentralUtil.pushUserContext(context); 
						 String strAccess="read,show,changetype,changeowner,fromconnect,fromdisconnect,toconnect,todisconnect";
						 StringList accessList = StringUtil.split(strAccess,  ",");
						 for (String accessInfo : (Iterable<String>)accessList){
							 MqlUtil.mqlCommand(context, false, true,
									 "modify bus $1 add access bus $2 as $3",
									 true, strProjectMilestoneObjectId, strProductMilestoneTrack, accessInfo);
						 } 
						 ProgramCentralUtil.popUserContext(context); 
						 DomainRelationship.connect(context,productMilestoneTrackObj ,ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM, domProjectMilestone);

						 String strEstimatedEndDate = "";
						 String SELECT_ATTRIBUTE_MILESTONE_ESTIMATED_FINISH_DATE = "attribute["+DomainConstants.ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE+"]";
						 strEstimatedEndDate = domProjectMilestone.getInfo(context, SELECT_ATTRIBUTE_MILESTONE_ESTIMATED_FINISH_DATE) ;
						//add interface Configuration Milestone on Project Milestone & set Interface attribute Milestone Date
						 Map attributeMap = new HashMap();


						 DomainObject domProjMile = new DomainObject(strProjectMilestoneObjectId);
						 String SELECT_INTERFACE_CONFIGURATION_MILESTONE = "interface["+ProgramCentralConstants.INTERFACE_CONFIGURATION_MILESTONE+"]";
						 String strInterface = domProjMile.getInfo(context, SELECT_INTERFACE_CONFIGURATION_MILESTONE);
						 attributeMap.put(ProgramCentralConstants.ATTRIBUTE_MILESTONE_DATE, strEstimatedEndDate);
						 ProgramCentralUtil.pushUserContext(context); 
						 if("FALSE".equals(strInterface)){
						 Mile.addInterfaceAndSetAttributes(context, strProjectMilestoneObjectId, "bus",
								 ProgramCentralConstants.INTERFACE_CONFIGURATION_MILESTONE, attributeMap);
						 }else{
							 domProjMile.setAttributeValues(context,attributeMap);
						 }
						 ProgramCentralUtil.popUserContext(context); 
					 }else{

						 for(int n=0;n<mlEngOrManfMilestoneTrack.size();n++){
							 Map mapMilestoneTrack = (Map)mlEngOrManfMilestoneTrack.get(n);
							 strProductMilestoneTrack = (String)mapMilestoneTrack.get(SELECT_ID);
							 DomainObject domMilestoneTrack = new DomainObject(strProductMilestoneTrack);
							 StringList objectSelects1 = new StringList(2);
							 objectSelects1.add(SELECT_ID);
							 objectSelects1.add(SELECT_NAME);
							 StringList relationshipSelects1 = new StringList();
							 relationshipSelects1.add(DomainConstants.SELECT_RELATIONSHIP_ID);

							 //Get the Relationship Id between Milestone Track object and WBS Milestone object
							 MapList mlResult = domMilestoneTrack.getRelatedObjects(context,
									 ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM,
									 DomainConstants.QUERY_WILDCARD,
									 objectSelects1,
									 relationshipSelects1,
									 false,	//to relationship - changed
									 true,	//from relationship
									 (short)1,
									 DomainConstants.EMPTY_STRING,
									 DomainConstants.EMPTY_STRING,
									 0);

							 for(int p=0;p<mlResult.size();p++){
								 Map mapMilestone = (Map)mlResult.get(p);
								 String MilestoneObj = (String)mapMilestone.get(SELECT_ID);
								 String strRelID = (String)mapMilestone.get(SELECT_RELATIONSHIP_ID);

								 if(strProjectMilestoneObjectId.equals(MilestoneObj)){
									 //disconnecting the existing connection between milestone track and WBS Milestone while editing discipline column value
									 DomainRelationship.disconnect(context, strRelID);
								 }
							 }
						 }
						 
						 String strEstimatedEndDate = "";
						 String strActualEndDate = "";
						 String SELECT_ATTRIBUTE_MILESTONE_ESTIMATED_FINISH_DATE = "attribute["+DomainConstants.ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE+"]";
						 String SELECT_ATTRIBUTE_MILESTONE_ESTIMATED_ACTUAL_DATE = "attribute["+DomainConstants.ATTRIBUTE_TASK_ACTUAL_FINISH_DATE+"]";
						 strEstimatedEndDate = domProjectMilestone.getInfo(context, SELECT_ATTRIBUTE_MILESTONE_ESTIMATED_FINISH_DATE) ;
						 strActualEndDate = domProjectMilestone.getInfo(context, SELECT_ATTRIBUTE_MILESTONE_ESTIMATED_ACTUAL_DATE) ;

						 Map attributeMap = new HashMap();


						 DomainObject domProjMile = new DomainObject(strProjectMilestoneObjectId);
						 String SELECT_INTERFACE_CONFIGURATION_MILESTONE = "interface["+ProgramCentralConstants.INTERFACE_CONFIGURATION_MILESTONE+"]";
						 String strInterface = domProjMile.getInfo(context, SELECT_INTERFACE_CONFIGURATION_MILESTONE);

						 if(null == strActualEndDate || "null".equals(strActualEndDate) || "".equals(strActualEndDate)){
							 attributeMap.put(ProgramCentralConstants.ATTRIBUTE_MILESTONE_DATE, strEstimatedEndDate);
						 }else{
							 attributeMap.put(ProgramCentralConstants.ATTRIBUTE_MILESTONE_DATE, strActualEndDate);
						 }
						 ProgramCentralUtil.pushUserContext(context); 
						 if("FALSE".equals(strInterface)){
						 Mile.addInterfaceAndSetAttributes(context, strProjectMilestoneObjectId, "bus",
								 ProgramCentralConstants.INTERFACE_CONFIGURATION_MILESTONE, attributeMap);
						 }else{
							 domProjMile.setAttributeValues(context,attributeMap);
						 }
						 ProgramCentralUtil.popUserContext(context);
						 
						 String strModelMilestoneTrackId = "";
						 String strModelId = "";
						 MapList mlModelMT = new MapList();
						  MapList mlMT = new MapList();
						  boolean bDepend = false;
						 //below for loop added for - If milestone is added after the existing milestone Track is published to model level,
						 //the already present Dependency relationship between Project Milestones will be removed and new Project Milestone will get connected to Model Milestone Track
						 for(int m=0;m<mlProductMilestoneTrack.size();m++){
							 Map mapProductMilestoneTrack = (Map)mlProductMilestoneTrack.get(m);
							 strProductMilestoneTrack = (String)mapProductMilestoneTrack.get(SELECT_ID);

							//If the milestone is first time edited to Engneering and then again edited to Manufacturing, the exisitng connection is deleted and then the new is done.
							 
							 //Get the Relationship Id between Milestone Track object and WBS Milestone object
							 MapList mlMT1 = domProjectMilestone.getRelatedObjects(context,
									 ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM,
									 DomainConstants.QUERY_WILDCARD,
									 objectSelects,
									 relationshipSelects,
									 true,	//to relationship - changed
									 false,	//from relationship
									 (short)1,
									 DomainConstants.EMPTY_STRING,
									 DomainConstants.EMPTY_STRING,
									 0);

							 for(int n=0;n<mlMT1.size();n++){
								 Map mapMT = (Map)mlMT1.get(n);
								 String strRelMT = (String)mapMT.get(SELECT_RELATIONSHIP_ID);
								 if(null != strRelMT || "".equals(strRelMT) || !"null".equals(strRelMT) ){
									 DomainRelationship.disconnect(context,strRelMT);
								 }
							 }
							 //Giving inherited access Read ,show to display milestone objects to Product Manager who is not Project User.
							 ProgramCentralUtil.pushUserContext(context); 
							 String strAccess="read,show,changetype,changeowner,fromconnect,fromdisconnect,toconnect,todisconnect";
							 StringList accessList = StringUtil.split(strAccess,  ",");
							 for (String accessInfo : (Iterable<String>)accessList){
								 MqlUtil.mqlCommand(context, false, true,
										 "modify bus $1 add access bus $2 as $3",
										 true, strProjectMilestoneObjectId, strProductMilestoneTrack, accessInfo);
							 } 
							 ProgramCentralUtil.popUserContext(context); 
							 
							 DomainObject domProductMilestoneTrack = new DomainObject(strProductMilestoneTrack);
							 DomainRelationship.connect(context,domProductMilestoneTrack  ,ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM,domProjectMilestone);

							 String SELECT_CONFIGURATION_HOLDER_TO_SIDE =  "to[" + ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_CRITERIA + "].from.id";
							 String strProduct = domProductMilestoneTrack.getInfo(context, SELECT_CONFIGURATION_HOLDER_TO_SIDE);
							 DomainObject domProduct = new DomainObject(strProduct);
							 String SELECT_MAIN_PRODUCT_FROM_ID =  "to[" + ProgramCentralConstants.RELATIONSHIP_MAIN_PRODUCT + "].from.id";

							 strModelId= domProduct.getInfo(context, SELECT_MAIN_PRODUCT_FROM_ID);
							 if(null == strModelId ||  "".equals(strModelId)){
								 String SELECT_PRODUCTS_FROM_ID =  "to[" + ProgramCentralConstants.RELATIONSHIP_PRODUCTS+ "].from.id";
								 strModelId= domProduct.getInfo(context, SELECT_PRODUCTS_FROM_ID);
							 }
							 DomainObject domModelId = new DomainObject(strModelId);

							 String busWhere = "(attribute[" + ProgramCentralConstants.ATTRIBUTE_MILESTONE_TRACK_DISCIPLINE + "]" + "== \"" + strMilestoneDiscipline + "\")" ;

							 mlModelMT =  domModelId.getRelatedObjects(context,
									 ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_CRITERIA,
									 DomainConstants.QUERY_WILDCARD,
									 objectSelects,
									 relationshipSelects,
									 false,	//to relationship
									 true,	//from relationship
									 (short)1,
									 busWhere,
									 DomainConstants.EMPTY_STRING,
									 0);

							 for(int n=0;n<mlModelMT.size();n++){
								 Map mapModelMT = (Map)mlModelMT.get(n);
								 strModelMilestoneTrackId = (String)mapModelMT.get(SELECT_ID);
								 DomainObject domModelMilestoneTrack = new DomainObject(strModelMilestoneTrackId);

								  mlMT = domModelMilestoneTrack.getRelatedObjects(context,
										 ProgramCentralConstants.RELATIONSHIP_TRACK_TRACEABILITY,
										 ProgramCentralConstants.TYPE_MILESTONE_TRACK,
										 objectSelects,
										 relationshipSelects,
										 true,	//to relationship
										 false,	//from relationship
										 (short)1,
										 DomainConstants.EMPTY_STRING,
										 DomainConstants.EMPTY_STRING,
										 0);




								  for(int j =0;j<mlMT.size();j++){
									  Map mapMT = (Map)mlMT.get(j);
									  String strMT = (String)mapMT.get(SELECT_ID);

									  if(strProductMilestoneTrack.equals(strMT)){
										  MapList mlPublishedMilestones = getModelMilestoneTracks(context, 
													 strModelId, new StringList(strMilestoneDiscipline), objSelectablesMS, relationshipSelects, false,true);
										  mlPublishedMilestones=(MapList)((Map)mlPublishedMilestones.get(0)).get(strMilestoneDiscipline);
										  
										  for(int k=0; k<mlPublishedMilestones.size();k++){
											  	 Map tempMap = (Map)mlPublishedMilestones.get(k);
												 tempMap.put("indexmilestone", ""+index++);
												 mlClubedMilestones.add(tempMap);
											 }	
										  

											 MapList mlNewMilestones = domProductMilestoneTrack.getRelatedObjects(context,
														ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM,
														DomainConstants.QUERY_WILDCARD,
														objSelectablesMS,
														relationshipSelects,
														false,	//to relationship - changed
														true,	//from relationship
														(short)1,
														"id=="+strProjectMilestoneObjectId,
														DomainConstants.EMPTY_STRING,
														0);
											  Map tempMap = (Map)mlNewMilestones.get(0);
											  tempMap.put("indexmilestone", ""+index++);
											  mlClubedMilestones.add(tempMap);
											  
										  //MapList mlSortedMilestones = Mile.sortProjectMilestonesWithDate(context, mlMilestones);
										  //This needs to uncommented later
										  //Mile.disConnectMilestoneswithDependency(context,mlSortedMilestones);

										  DomainRelationship.connect(context,domModelMilestoneTrack ,ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM, domProjectMilestone);
										  bDepend = true;
									  }
								  }

							 }
						 }
						 
												 //below loop added for - If milestone is added after the existing milestone Track is published to model level,
						 //the newly added project milestone will get connected to Model milestone track with Configuration Item relationship
						 //and the dependency relationship will be newly added between Project milestones connected to Model milestone track.


							 if(bDepend){
								 if(mlModelMT.size() > 0){
									  
									 MapList mlNewClubedSortedmilestoens = Mile.sortProjectMilestonesWithDate(context, mlClubedMilestones);
									 
									 for(int k=0; k<mlNewClubedSortedmilestoens.size();k++){
										 Map tempMap = (Map)mlNewClubedSortedmilestoens.get(k);
										 tempMap.put("dateindex", ""+dateindex++);
									 }

									 mlNewClubedSortedmilestoens.addSortKey("dateindex", "ascending", "integer");
									 mlNewClubedSortedmilestoens.addSortKey("indexmilestone", "ascending", "integer");
									 mlNewClubedSortedmilestoens.sort();

									 //recalculate code here
									 Mile.recalculateMilestonesDependency(context,mlNewClubedSortedmilestoens);
									 //Mile.connectMilestoneswithDependency(context,mlNewSortedMilestones);

									 HashMap paramMap = new HashMap();
										paramMap.put("strMilestoneTrack",strProductMilestoneTrack);
										paramMap.put("strProductID",strProductId);
										String[] args1 = JPO.packArgs(paramMap);

									 JPO.invoke(context,"emxPLCMilestones", args1,
												"addInterfaceDerivationIndex",
												args1, null);

								 }
						 }
					 }
				 }
			 }
		 }

		 if("None".equals(NewValue)){

			 StringList objectSelectable = new StringList(2);
			 objectSelectable.add(SELECT_ID);
			 objectSelectable.add(SELECT_NAME);
			 StringList relationshipSelectable = new StringList();
			 relationshipSelectable.add(DomainConstants.SELECT_RELATIONSHIP_ID);

			 MapList mlMT = domProjectMilestone.getRelatedObjects(context,
					 ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM,
					 DomainConstants.QUERY_WILDCARD,
					 objectSelectable,
					 relationshipSelectable,
					 true,	//to relationship - changed
					 false,	//from relationship
					 (short)1,
					 DomainConstants.EMPTY_STRING,
					 DomainConstants.EMPTY_STRING,
					 0);

			 for(int p=0;p<mlMT.size();p++){
				 Map mapMilestoneTrack = (Map)mlMT.get(p);
				 String MilestoneTrackObj = (String)mapMilestoneTrack.get(SELECT_ID);
				 String strRelID = (String)mapMilestoneTrack.get(SELECT_RELATIONSHIP_ID);
				 //disconnecting the existing connection between milestone track and WBS Milestone while editing discipline column value
				 DomainRelationship.disconnect(context, strRelID);

				 Map attributeMap = new HashMap();
				 Mile.removeInterface(context, strProjectMilestoneObjectId, "bus",ProgramCentralConstants.INTERFACE_CONFIGURATION_MILESTONE);
			 }
		 }
			}
	 }
	 catch(FrameworkException frameworkException){
		 throw frameworkException;
	 }
	 catch(Exception ex){
		 String sErrorMsg=EnoviaResourceBundle.getProperty(context,SUITE_KEY,"emxProgramCentral.Milestone.EditMilestoneNotAllowed",context.getSession().getLanguage());
		 emxContextUtil_mxJPO.mqlError(context,sErrorMsg);
		 ex.printStackTrace();
	 }
 }

 /**
  * Gives the ProjecT Milestone objects connected to Model Milestone Track
  *
  * @param context the ENOVIA <code>Context</code> object
  * @param args holds the HashMap containing the
	 *            following arguments
  * @return Object - MapList containing the ids of all Milestones of the project
  * @throws Exception
  *             if the operation fails
  */
public MapList getModelMilestoneTracks(Context context,String strModelMTId) {
	MapList mlModelMilestones = new MapList();
	try{
		DomainObject domModelMilestoneTrack = new DomainObject(strModelMTId);
		StringList objectSelects = new StringList(2);
		objectSelects.add(SELECT_ID);
		objectSelects.add(SELECT_NAME);
		objectSelects.add(SELECT_CURRENT);
		objectSelects.add(ProgramCentralConstants.SELECT_ATTRIBUTE_MILESTONE_DATE);
		objectSelects.add(ProgramCentralConstants.SELECT_ATTRIBUTE_MILESTONE_DISCIPLINE);
		StringList relationshipSelects1 = new StringList();

		mlModelMilestones =  domModelMilestoneTrack.getRelatedObjects(context,
				ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM,
				DomainConstants.QUERY_WILDCARD,
				objectSelects,
				relationshipSelects1,
				false,	//to relationship - changed
				true,	//from relationship
				(short)1,
				DomainConstants.EMPTY_STRING,
				DomainConstants.EMPTY_STRING,
				0);

	}catch(Exception ex){
		ex.printStackTrace();
	}
	return mlModelMilestones;
}




  /**
  * Decides the Discipline column should be editable or not in the milestones table of Project
  *
  * @param context the ENOVIA <code>Context</code> object
  * @param args holds the HashMap containing the
	 *            following arguments
  * @return Object - MapList containing the ids of all Milestones of the project
  * @throws Exception
  *             if the operation fails
  */
 public StringList isDisciplineEditallowed(Context context,String args[])
 {
	 StringList slEditable = new StringList();
	 boolean bEditable = false;
	 try {
		 String strConfigurationManager = ProgramCentralConstants.ROLE_CONFIGURATION_MANAGER;
		 boolean isConfManager = PersonUtil.getAssignments(context).contains(strConfigurationManager);
		 String strVPLMProjectLeader = PropertyUtil.getSchemaProperty(context,"role_VPLMProjectLeader");
		 String strRole = context.getRole();
		 HashMap arguMap = (HashMap)JPO.unpackArgs(args);
		 MapList objectList = (MapList) arguMap.get("objectList");
		 String strObjectId = "";
		 Iterator objectListItr = objectList.iterator();
		 while(objectListItr.hasNext()){
			 Map object = (Map) objectListItr.next();
			 if(isConfManager || strRole.contains(strVPLMProjectLeader)){
				 if(object!=null && object.size()>0){

					 strObjectId = (String)object.get(DomainConstants.SELECT_ID);
					 DomainObject dmoObject = DomainObject.newInstance(context,strObjectId);
					 StringList objSelects = new StringList();
					 String strEstimatedEndDate = "";
					 String strProjectMilestoneObjState = "";
					 String SELECT_ATTRIBUTE_MILESTONE_ESTIMATED_FINISH_DATE = "attribute["+DomainConstants.ATTRIBUTE_TASK_ESTIMATED_FINISH_DATE+"]";
					 objSelects.add(SELECT_ATTRIBUTE_MILESTONE_ESTIMATED_FINISH_DATE);
					 objSelects.add(SELECT_CURRENT);
					 Map mapOBject = dmoObject.getInfo(context, objSelects) ;
					 strProjectMilestoneObjState = (String)mapOBject.get(SELECT_CURRENT);
					 strEstimatedEndDate = (String)mapOBject.get(SELECT_ATTRIBUTE_MILESTONE_ESTIMATED_FINISH_DATE);
					 String SELECT_RELATIONSHIP_CONFIGURATION_MILESTONE_TO_ID = "to[" + ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM + "].from.id";
					 StringList slMT = dmoObject.getInfoList(context, SELECT_RELATIONSHIP_CONFIGURATION_MILESTONE_TO_ID);
					 if(!"Complete".equals(strProjectMilestoneObjState)){
						 if(null!=strEstimatedEndDate && !"null".equalsIgnoreCase(strEstimatedEndDate)&&!"".equalsIgnoreCase(strEstimatedEndDate)){
							 if(slMT.size() <= 1){
								 bEditable = true;
							 }else{
								 bEditable = false;
							 }
						 }
					 }
				 }
				 slEditable.add(bEditable);
			 }
			 else{

				 slEditable.add(bEditable);
			 }
		 }
	 } catch (Exception e) {
		 e.printStackTrace();
	 }
	 return slEditable;
 }


 /**
  * Creates the Model level Milestone Track objects
  *
  * @param context the ENOVIA <code>Context</code> object
  * @param args holds the HashMap containing the
	 *            following arguments
  * @return Object - MapList containing the ids of all Milestones of the project
  * @throws Exception
  *             if the operation fails
  */

 public int publishProductLeveltoModelLevel (Context context, String[] args){
	 Integer intReturn = 0;
	 int index=0;
	 int dateindex=0;
	 Milestone Mile = new Milestone();
	 MapList mlClubMilestones = new MapList();
	 String SELECT_MAIN_PRODUCT_FROM_ID =  "to[" + ProgramCentralConstants.RELATIONSHIP_MAIN_PRODUCT + "].from.id";
	 String SELECT_PRODUCTS_FROM_ID =  "to[" + ProgramCentralConstants.RELATIONSHIP_PRODUCTS+ "].from.id";
	 String strModelId = "";
	 StringList objectSelects = new StringList(2);
	 objectSelects.add(SELECT_ID);
	 objectSelects.add(SELECT_NAME);
	 objectSelects.add(SELECT_CURRENT);
	 StringList relationshipSelects = new StringList(SELECT_RELATIONSHIP_ID); 
	 MapList mlPublishedMilestones = new MapList();

	 try{
		 HashMap paramMap = (HashMap)JPO.unpackArgs(args);
		 String strProductMilestoneTrackObjId = (String) paramMap.get("strMilestoneTrack");
		 String strProductID = (String) paramMap.get("strProductID");

		 DomainObject domProductMTObj = DomainObject.newInstance(context, strProductMilestoneTrackObjId);

		 String strProductMTDiscipline = domProductMTObj.getAttributeValue(context, ProgramCentralConstants.ATTRIBUTE_MILESTONE_TRACK_DISCIPLINE);

		 DomainObject domProduct = new DomainObject(strProductID);		 
		 strModelId= domProduct.getInfo(context, SELECT_MAIN_PRODUCT_FROM_ID);

		 if(null == strModelId ||  "".equals(strModelId)){			 
			 strModelId= domProduct.getInfo(context, SELECT_PRODUCTS_FROM_ID);
		 }
		 DomainObject domModelId = new DomainObject(strModelId);

		 String busWhereClause = "(attribute[" + ProgramCentralConstants.ATTRIBUTE_MILESTONE_TRACK_DISCIPLINE + "]" + "== \"" + strProductMTDiscipline + "\")" ;

		 MapList mldomModelMT = domModelId.getRelatedObjects(context,
				 ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_CRITERIA,
				 DomainConstants.QUERY_WILDCARD,
				 objectSelects,
				 relationshipSelects,
				 false,	//to relationship
				 true,	//from relationship
				 (short)1,
				 busWhereClause,
				 DomainConstants.EMPTY_STRING,
				 0);
		 DomainObject modelMTObj = DomainObject.newInstance(context);
		 if(mldomModelMT.size() == 0){
			 String strObjectGeneratorName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE,
					 ProgramCentralConstants.TYPE_MILESTONE_TRACK, true);
			 //Autogenerating Model level Milestone track object name
			 String autoName = DomainObject.getAutoGeneratedName(context,strObjectGeneratorName,null);

			 //Creating Model level Milestone Track object
			 ProgramCentralUtil.pushUserContext(context); 
			 modelMTObj.createObject(context, ProgramCentralConstants.TYPE_MILESTONE_TRACK, autoName, "", ProgramCentralConstants.POLICY_MILESTONE_TRACK, context.getVault().getName());

			 //Setting Miletone Track Discipline attribute value on model level Milestone track object
			 modelMTObj.setAttributeValue(context, ProgramCentralConstants.ATTRIBUTE_MILESTONE_TRACK_DISCIPLINE, strProductMTDiscipline);

			 // Connecting Model object with Model level Milestone track object with Configuration Holder relationship
			 DomainRelationship.connect(context, domModelId,ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_CRITERIA, modelMTObj);
			 ProgramCentralUtil.popUserContext(context);
		 }else{
			 //publishing for second time, get all published Milestone in order of it's dependency
			 StringList objSelectablesMS = new StringList();
			 objSelectablesMS.addAll(objectSelects);
			 objSelectablesMS.add(ProgramCentralConstants.SELECT_ATTRIBUTE_MILESTONE_DATE);
			 objSelectablesMS.add("to["+ProgramCentralConstants.RELATIONSHIP_FIRST_CONFIGURATION_ITEM+"].id");

			 mlPublishedMilestones = getModelMilestoneTracks(context, 
					 strModelId, new StringList(strProductMTDiscipline), objSelectablesMS, null, false,true);
			 mlPublishedMilestones=(MapList)((Map)mlPublishedMilestones.get(0)).get(strProductMTDiscipline);
			 Map mapModelMT = (Map)mldomModelMT.get(0);
			 String strModelMT = (String)mapModelMT.get(SELECT_ID);
			 modelMTObj.setId(strModelMT);

			 for(int k=0; k<mlPublishedMilestones.size();k++){
				 Map tempMap = (Map)mlPublishedMilestones.get(k);
				 tempMap.put("indexmilestone", ""+index++);
				 mlClubMilestones.add(tempMap);
			 }			 
		 }
		 StringList objectSelectables = new StringList(2);
		 objectSelectables.addAll(objectSelects);
		 objectSelectables.add(ProgramCentralConstants.SELECT_ATTRIBUTE_MILESTONE_DATE);		 
		 //Get the Project Milestone objects from Product Milstone Track object
		 MapList mlProjectMilestones = domProductMTObj.getRelatedObjects(context,
				 ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM,
				 DomainConstants.QUERY_WILDCARD,
				 objectSelectables,
				 relationshipSelects,
				 false,	//to relationship - changed
				 true,	//from relationship
				 (short)1,
				 DomainConstants.EMPTY_STRING,
				 DomainConstants.EMPTY_STRING,
				 0);
		 MapList mlCompltedMilestones = new MapList();
		 MapList mlDummyMilestones = new MapList();
		 mlDummyMilestones = mlProjectMilestones;
		 for(int p=0;p<mlDummyMilestones.size();p++){
			 Map mapMilestone = (Map)mlDummyMilestones.get(p);
			 String strProjectMilestoneObjState = (String)mapMilestone.get(SELECT_CURRENT);
			 if("Complete".equalsIgnoreCase(strProjectMilestoneObjState)){
				 mlProjectMilestones.remove(mapMilestone);
				 mlCompltedMilestones.add(mapMilestone);
			 }
		 }
		 MapList mlSortedProjecTmilestones = Mile.sortProjectMilestonesWithDate(context,mlProjectMilestones);
		 ProgramCentralUtil.pushUserContext(context); 
		 for(int p=0;p<mlSortedProjecTmilestones.size();p++){
			 Map mapMilestone = (Map)mlSortedProjecTmilestones.get(p);
			 String strProjectMilestoneObj = (String)mapMilestone.get(SELECT_ID);
			 DomainObject domProjectMilestone = new DomainObject(strProjectMilestoneObj);

			 if(p==0){				 
				 MapList mlFirstCinfiguredItemMilestone =  modelMTObj.getRelatedObjects(context,
						 ProgramCentralConstants.RELATIONSHIP_FIRST_CONFIGURATION_ITEM,
						 DomainConstants.QUERY_WILDCARD,
						 objectSelectables,
						 relationshipSelects,
						 false,	//to relationship - changed
						 true,	//from relationship
						 (short)1,
						 DomainConstants.EMPTY_STRING,
						 DomainConstants.EMPTY_STRING,
						 0);
				 if(mlFirstCinfiguredItemMilestone.size() == 0){
					 //Connecting Model level Milestone Track object to first Project milestones with First Configuration Item realtionship
					 DomainRelationship domRel = DomainRelationship.connect(context,modelMTObj  ,ProgramCentralConstants.RELATIONSHIP_FIRST_CONFIGURATION_ITEM, domProjectMilestone);
				 }
				 // Connecting Product level Milestone track object with Model level Milestone track object with Milestone Track relationship
				 DomainRelationship.connect(context, domProductMTObj,ProgramCentralConstants.RELATIONSHIP_TRACK_TRACEABILITY, modelMTObj);
			 }
			 //Connecting Model level Milestone Track object to Project milestones with Configuration Item relationship
			 DomainRelationship.connect(context,modelMTObj,ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM, domProjectMilestone);

		 }
		 ProgramCentralUtil.popUserContext(context);

		 //	 if(mlPublishedMilestones.size() != 0 ){
		 for(int k=0; k<mlSortedProjecTmilestones.size();k++){
			 Map tempMap = (Map)mlSortedProjecTmilestones.get(k);
			 tempMap.put("indexmilestone", ""+index++);
			 mlClubMilestones.add(tempMap);
		 }

		 MapList mlNewClubedSortedmilestoens =  Mile.sortProjectMilestonesWithDate(context,mlClubMilestones);
		 for(int k=0; k<mlNewClubedSortedmilestoens.size();k++){
			 Map tempMap = (Map)mlNewClubedSortedmilestoens.get(k);
			 tempMap.put("dateindex", ""+dateindex++);
		 }

		 mlNewClubedSortedmilestoens.addSortKey("dateindex", "ascending", "integer");
		 mlNewClubedSortedmilestoens.addSortKey("indexmilestone", "ascending", "integer");
		 mlNewClubedSortedmilestoens.sort();
		 ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");

		 Mile.recalculateMilestonesDependency(context,mlNewClubedSortedmilestoens);

		//Displaying the message to user if the Milestone is in completed  state when publishing to Model level.
		 //also disconnecting that Milestone with Milestone Holder object of the Product.
		 if(mlCompltedMilestones.size() != 0 ){
			 for(int i = 0;i<mlCompltedMilestones.size();i++){
				 Map mapCompletedML = (Map)mlCompltedMilestones.get(i);
				 String strMilestone = (String)mapCompletedML.get(SELECT_ID);
				 DomainObject domMilestone = new DomainObject(strMilestone);
				 StringList objectSelects1 = new StringList(2);
				 objectSelects1.add(SELECT_ID);
				 objectSelects1.add(SELECT_NAME);

				 MapList mlMT = domMilestone.getRelatedObjects(context,
						 ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM, 
						 DomainConstants.QUERY_WILDCARD,
						 objectSelects1,
						 relationshipSelects,
						 true,	//to relationship - changed
						 false,	//from relationship
						 (short)1,
						 DomainConstants.EMPTY_STRING,
						 DomainConstants.EMPTY_STRING,
						 0);
				 ProgramCentralUtil.pushUserContext(context); 
				 for(int m=0;m<mlMT.size();m++){
					 Map mapMT = (Map)mlMT.get(m);
					 String strRelId = (String)mapMT.get(SELECT_RELATIONSHIP_ID);
					 DomainRelationship.disconnect(context, strRelId);
				 }				 
			 }
			 String sErrorMsg=EnoviaResourceBundle.getProperty(context,SUITE_KEY,"emxProgramCentral.Alert.CannotPublishMilestone",context.getSession().getLanguage());
			 emxContextUtil_mxJPO.mqlError(context,sErrorMsg);
		 }
		 ProgramCentralUtil.popUserContext(context);
	 }catch(Exception ex){
		 intReturn=1;
		 ex.printStackTrace();
	 } 

	 return intReturn;		
 }

 /**
  * returns milestones given model and milestone track discipline
  *
  * @param context the eMatrix <code>Context</code> object
  * @param modelId the id of the model milestone tracks attached to
  * @param disciplinesList the list of milestone tracks' discipline to retrieve(e.g. Engineering, Manufacturing)
  *                If empty string or null is provided, all milestone tracks will be retrieved
  * @param busSelects the business selects of milestone objects to return
  *                If not provided, only milestones' id will be returned
  * @param relSelects the relationship selects of milestone objects to return
  *                If not provided, no information on relationships returned
  * @param bInclMilestoneTrack If true, milestone tracks will be included in returned maplist
  * @param bIncludeCompleteMilestone If true, completed milestones will be included in returned maplist
  * @returns MapList of milestone tracks and milestones
  *                  discipline is the key in each map and its value is maplist
  *                  of milestonetrack\milestone objects, in which the first
  *                  item is milestonetrack
  * @throws Exception if the operation fails
  */
 public static MapList getModelMilestoneTracks(Context context, String modelId, StringList disciplinesList, StringList busSelects,
 		                                      StringList relSelects, boolean bInclMilestoneTrack, boolean bIncludeCompleteMilestone)
	throws Exception {
	 	MapList milestoneMapList = new MapList();
	 	if(modelId == null || "null".equals(modelId) || modelId.length() < 1){
	 		return  milestoneMapList;
	 	}

	 	try{
	 		ContextUtil.startTransaction(context, false);
			 //builds discipline where clause
			 String SELECT_MILESTONE_ATTRIBUTE = "attribute[" + ProgramCentralConstants.SELECT_ATTRIBUTE_MILESTONE_DISCIPLINE + "]";
			 StringBuffer strWhereBuf = new StringBuffer(50);
			 if(disciplinesList != null && disciplinesList.size() > 0){
				 for(int i=0; i < disciplinesList.size(); i++){
					 if(strWhereBuf.toString().length() > 0){
						 strWhereBuf.append(" || ");
					 }
					 strWhereBuf.append(SELECT_MILESTONE_ATTRIBUTE);
					 strWhereBuf.append("==");
					 strWhereBuf.append(disciplinesList.get(i));
				 }
			 }

			 //builds bus select list
			 StringList busSelectsList = null;
			 if(busSelects == null || busSelects.size() < 1){
				 busSelectsList = new StringList(1);
				 busSelectsList.add(DomainObject.SELECT_ID);
			 } else {
				 busSelectsList = busSelects;
			 }

			 busSelectsList.add(SELECT_MILESTONE_ATTRIBUTE);

			 StringBuffer relPatternsBuf = new StringBuffer(ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_CRITERIA);
			 relPatternsBuf.append(",");
			 relPatternsBuf.append(ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM);

			 StringBuffer typePatternBuf = new StringBuffer(ProgramCentralConstants.TYPE_MILESTONE_TRACK);
			 typePatternBuf.append(",");
			 typePatternBuf.append(ProgramCentralConstants.TYPE_MILESTONE);

			 //get milestone tracks
			 DomainObject domModel = DomainObject.newInstance(context, modelId);
			 MapList milestoneTrackML = domModel.getRelatedObjects(context,
					 ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_CRITERIA,
					 ProgramCentralConstants.TYPE_MILESTONE_TRACK,
					 busSelectsList,
					 relSelects,
					 false,
					 true,
					 (short)1,
					 strWhereBuf.toString(),
					 DomainConstants.EMPTY_STRING,
					 0);

			if(milestoneTrackML != null && milestoneTrackML.size() > 0){
				String STATE_COMPLETE="Complete";
				String milestoneDisciplineStr = PropertyUtil.getSchemaProperty(context, "attribute_MilestoneDiscipline");
				String objectWhere = "("+DomainConstants.SELECT_CURRENT+ " != \""+STATE_COMPLETE+"\")";

				if(bIncludeCompleteMilestone){
					objectWhere = "";
				}

				for(int j=0; j < milestoneTrackML.size(); j++){
					Map milestoneTrackMap = (Map)milestoneTrackML.get(j);
					String discipline = (String)milestoneTrackMap.get(SELECT_MILESTONE_ATTRIBUTE);
					DomainObject milestoneTrackDO = DomainObject.newInstance(context,
						                         (String)milestoneTrackMap.get(DomainObject.SELECT_ID));

					MapList milestoneML = milestoneTrackDO.getRelatedObjects(context,
							ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM,
							ProgramCentralConstants.TYPE_MILESTONE,
							 busSelectsList,
							 relSelects,
							 false,	//to relationship
							 true,	//from relationship
							 (short)1,
							 objectWhere,
							 DomainConstants.EMPTY_STRING,
							 0);

					if(milestoneML == null){
						milestoneML = new MapList();
					}

					//remove task connected via First Configuration Item
					int firstConfigMSIndex = -1;
					for(int k=0; k < milestoneML.size(); k++){
			   			Map objMap = (Map)milestoneML.get(k);
			   			String relationshipStr = (String)objMap.get("relationship");
			   			if(ProgramCentralConstants.RELATIONSHIP_FIRST_CONFIGURATION_ITEM.equals(relationshipStr)){
			   				firstConfigMSIndex = k;
			   				break;
			   			}

			   			//save milestone discipline of milestone track, in which milestone belongs to, in milestone object map
			   			objMap.put(milestoneDisciplineStr, discipline);
					}

					if(firstConfigMSIndex >= 0){
						Map firstConfigMilestoneMap = (Map)milestoneML.remove(firstConfigMSIndex);

						//set the order of milestones based upon dependency
						if(milestoneML.size()>1)
							milestoneML = reorderMilestoneList(context, milestoneML, firstConfigMilestoneMap);
					}

					//add milestone track at the top of maplist
					Map mMap = new HashMap();
					if(bInclMilestoneTrack){
						milestoneML.add(0, milestoneTrackMap);
					}
					mMap.put(discipline, milestoneML);
					milestoneMapList.add(mMap);
				}
			}

	   	    ContextUtil.commitTransaction(context);
	   	}catch(Exception ex){
	   		ex.printStackTrace();
	   		ContextUtil.abortTransaction(context);
	   		throw new Exception(ex.getMessage());
	   	}

	    return milestoneMapList;
	}

 /**
  * returns milestone list based milestone dependency list
  *
  * @param context the eMatrix <code>Context</code> object
  * @param originalList the list of milestones not in depedent order
  * @param firstConfigMilestoneMap the object map of first configuration item milestone in milestone track
  * @returns MapList of milestones in the dependent order
  * @throws Exception if the operation fails
  */
 private static MapList reorderMilestoneList(Context context, MapList originalList, Map firstConfigMilestoneMap)
	throws Exception {

 	if(firstConfigMilestoneMap == null || firstConfigMilestoneMap.size() < 1){
 		return originalList;
 	}

 	MapList retMapList = new MapList();
 	StringList busSelects = new StringList(1);
 	busSelects.add(DomainObject.SELECT_ID);
 	String firstMilestoneId = (String)firstConfigMilestoneMap.get(DomainObject.SELECT_ID);
		DomainObject milestoneObj = DomainObject.newInstance(context, firstMilestoneId);

		//get all subsequent dependent milestones
		MapList milestoneML = milestoneObj.getRelatedObjects(context,
				 RELATIONSHIP_DEPENDENCY,
				 ProgramCentralConstants.TYPE_MILESTONE,
				 busSelects,
				 null,
				 true,	//to relationship
				 false,	//from relationship
				 (short) 0, //expand all levels
				 DomainConstants.EMPTY_STRING,
				 DomainConstants.EMPTY_STRING,
				 0);

		//goes through the original map and reorder it
		int firstMilestoneIdx = -1;
		for(int k=0; k < milestoneML.size(); k++){
			String milestoneId = (String)((Map)milestoneML.get(k)).get(DomainObject.SELECT_ID);
			for(int i=0; i < originalList.size(); i++){
				Map milestoneMap = (Map) originalList.get(i);
				if(milestoneId.equals((String)milestoneMap.get(DomainObject.SELECT_ID))){
					retMapList.add(milestoneMap);
					break;
				} else if(firstMilestoneId.equals((String)milestoneMap.get(DomainObject.SELECT_ID))){
					firstMilestoneIdx = i;
				}
			}
		}

		//insert the first milestone back to the beginning of the list
		if(firstMilestoneIdx < 0){
			retMapList.add(0, originalList.get(originalList.size() - 1));
		} else {
			retMapList.add(0, originalList.get(firstMilestoneIdx));
		}

 	return retMapList;
 }


 /**
  * Gets the Project Milestone
  *
  * @param context the ENOVIA <code>Context</code> object
  * @param args holds the HashMap containing the
	 *            following arguments
  * @return Object - MapList containing the ids of all Milestones of the project
  * @throws Exception
  *             if the operation fails
  */

 public Vector getProjectMilestone(Context context, String[] args){
	 Vector VMilestonesProject = new Vector();
	 try{
		 HashMap arguMap = (HashMap)JPO.unpackArgs(args);

		 HashMap paramList = (HashMap)arguMap.get("paramList");
		 MapList objectList = (MapList) arguMap.get("objectList");

		 String reportFormat = (String)paramList.get("reportFormat");

		 Iterator objectListItr = objectList.iterator();
		 while(objectListItr.hasNext()){
			 String strProject = "";
			 StringBuffer strBuffer = new StringBuffer();
			 MapList mlResult = new MapList();
			 Map object = (Map) objectListItr.next();
			 if(object!=null && object.size()>0){
				 String ProjectMilestone = (String)object.get(DomainConstants.SELECT_ID);
				 DomainObject domProjectMilestone = new DomainObject(ProjectMilestone);

		            StringBuffer sbTypeIncludePattern = new StringBuffer();
		            List lstProjectSpaceChildTypes = ProgramCentralUtil.getSubTypesList(context, ProgramCentralConstants.TYPE_PROJECT_SPACE);
		            
		            if(lstProjectSpaceChildTypes.size()>0)
		            for (int i = 0; i < lstProjectSpaceChildTypes.size(); i++) {
		               sbTypeIncludePattern.append(lstProjectSpaceChildTypes.get(i));
		               if (i != lstProjectSpaceChildTypes.size() - 1) {
		                    	sbTypeIncludePattern.append(",");
		               }
		            }
					StringList objectSelects = new StringList();
					objectSelects.add(DomainObject.SELECT_ID);
					objectSelects.add(DomainObject.SELECT_NAME);
					objectSelects.add(DomainObject.SELECT_TYPE);

					StringList relationshipSelects = new StringList();

					Pattern strTypeIncludePattern=new Pattern(sbTypeIncludePattern.toString());
					MapList returnMapList = domProjectMilestone.getRelatedObjects(context,
							DomainConstants.RELATIONSHIP_SUBTASK,
							DomainConstants.QUERY_WILDCARD,
							objectSelects,
							relationshipSelects,
							true,	//to relationship
							false,	//from relationship
							(short)0,
							DomainConstants.EMPTY_STRING, //objectWhereClause
							DomainConstants.EMPTY_STRING, //relationshipWhereClause
							0,
							strTypeIncludePattern,
							null,
							null);
					 StringBuffer sbHead = new StringBuffer();
	  	    	        StringBuffer sbHref = new StringBuffer();
					for(int i = 0;i<returnMapList.size();i++){
						 Map mapProject = (Map)returnMapList.get(i);
						String strProjectID = (String)mapProject.get(SELECT_ID);
						 strProject = (String)mapProject.get(SELECT_NAME);

						 if (reportFormat != null && !("null".equalsIgnoreCase(reportFormat)) && reportFormat.length()>0){
							 sbHref.append(strProject);
		    				 VMilestonesProject.add(sbHref.toString());
						 }else{
		    				 sbHead.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?mode=insert");
							 sbHead.append("&amp;objectId=" + strProjectID + "'");
							 sbHead.append(", '800', '700', 'true', 'popup')\">");
							 sbHref.append(sbHead);
							 sbHref.append(XSSUtil.encodeForHTML(context,strProject));
							 sbHref.append("</a>");
							 VMilestonesProject.add(sbHref.toString());
		    			 }
					}
			 }
		 }

	 }catch(Exception ex){
		 ex.printStackTrace();
	 }
	 return VMilestonesProject;
 }


 /**
  * Returns range values for discipline column in Milestones of Projects
  *
  * @param context the ENOVIA <code>Context</code> object
  * @param args holds the HashMap containing the
	 *            following arguments
  * @return Object - MapList containing the ids of all Milestones of the project
  * @throws Exception
  *             if the operation fails
  */
	public Map getMilestoneAttributeRangeValues(Context context, String[] args)throws Exception
	{
		String strAttributeName = ProgramCentralConstants.ATTRIBUTE_MILESTONE_TRACK_DISCIPLINE;
    HashMap rangeMap = new HashMap();
    matrix.db.AttributeType attribName = new matrix.db.AttributeType(
            strAttributeName);
    attribName.open(context);

    List attributeRange = attribName.getChoices();

    List attributeDisplayRange = i18nNow
            .getAttrRangeI18NStringList(
            		ProgramCentralConstants.ATTRIBUTE_MILESTONE_TRACK_DISCIPLINE,
                    (StringList) attributeRange, context.getSession()
                            .getLanguage());
    rangeMap.put(FIELD_CHOICES, attributeRange);
    rangeMap.put(FIELD_DISPLAY_CHOICES, attributeDisplayRange);
    return rangeMap;
    }


/**
  * Gives the Milestones discipline value to display in the Milestone summary table.
  *
  * @param context the ENOVIA <code>Context</code> object
  * @param args holds the HashMap containing the
	 *            following arguments
  * @return Object - MapList containing the ids of all Milestones of the project
  * @throws Exception
  *             if the operation fails
  */
	public Vector getMilestoneDiscipline (Context context, String[] args){
		Vector returnVec = new Vector();
		try{
			HashMap inputMap = (HashMap)JPO.unpackArgs(args);
			MapList objectMap = (MapList) inputMap.get("objectList");
			StringList returnStringList = new StringList (objectMap.size());

			for (int i = 0; i < objectMap.size(); i++) {
				Map outerMap = (Map)objectMap.get(i);
				String MilestoneID = (String)outerMap.get(SELECT_ID);
				DomainObject domProjectMilestone = new DomainObject( MilestoneID);

				StringList objectSelects = new StringList(2);
				objectSelects.add(SELECT_ID);
				objectSelects.add(SELECT_NAME);
				StringList relationshipSelects = new StringList();

				MapList mlMT = domProjectMilestone.getRelatedObjects(context,
						ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM,
						DomainConstants.QUERY_WILDCARD,
						objectSelects,
						relationshipSelects,
						true,	//to relationship - changed
						false,	//from relationship
						(short)1,
						DomainConstants.EMPTY_STRING,
						DomainConstants.EMPTY_STRING,
						0);
				if(mlMT.size() != 0){
					for(int p=0;p<mlMT.size();p++){
						Map mapMilestoneTrack = (Map)mlMT.get(p);
						String strMT = (String)mapMilestoneTrack.get(SELECT_ID);
						DomainObject domMT = new DomainObject(strMT);

						Map mapMT = (Map)domMT.getAttributeMap(context,true);
						String strDiscipline =(String)mapMT.get(ProgramCentralConstants.SELECT_ATTRIBUTE_MILESTONE_DISCIPLINE);

						returnVec.add(strDiscipline);
						break;
					}
				}else{
					String strAttributeName = ProgramCentralConstants.ATTRIBUTE_MILESTONE_TRACK_DISCIPLINE;
					matrix.db.AttributeType attribName = new matrix.db.AttributeType(
							strAttributeName);
					attribName.open(context);
					returnVec.add(attribName.getDefaultValue(context));
				}
			}

		}catch(Exception e) {
			e.printStackTrace();
		}
		return returnVec;
	}

	 /**
	  * Decides the Milestone category should be displayed or not in the Project Categories
	  *
	  * @param context the ENOVIA <code>Context</code> object
	  * @param args holds the HashMap containing the
		 *            following arguments
	  * @return Object - MapList containing the ids of all Milestones of the project
	  * @throws Exception
		 *             if the operation fails
	  */

	 public boolean isMilestoneDisplayAllowed(Context context,String args[])
	 {
		 boolean bCFF = false;
		 try {

			 bCFF = FrameworkUtil.isSuiteRegistered(context,"appVersionVariantConfiguration",false,null,null);
			// if(bCFF){
				
			//	 String isMilestoneDisplayAllowed = com.matrixone.apps.domain.util.EnoviaResourceBundle.getProperty(context,"emxEffectivity.AllowMilestoneEffectivity");
			//	 if(!"true".equalsIgnoreCase(isMilestoneDisplayAllowed)){
			//		 bCFF = false;
			//	 }
			 //}

		 } catch (Exception e) {
			 //here BCFF is set to false as if key is not present in the properties file,
			 //it will throw exception and that time we don't want to dispaly Milestone category in Project.
			 bCFF = false;

		 }
		 return bCFF;
	 }



	 /**
	  * This trigger function updates the Milestone dependency if the milestone is deleted
	  *
	  * @param context the eMatrix <code>Context</code> object
	  * @param args holds the following input arguments:
	  *        0 - String containing the object id
	  * @throws Exception if operation fails
	  * @since PRG R215
	  */
	 public int updateMilestoneDependency(Context context, String[] args) throws Exception
	 {
		 String strProjectMilestoneobjectId = args[0];

		 if(null!=strProjectMilestoneobjectId && !"null".equalsIgnoreCase(strProjectMilestoneobjectId)&&!"".equalsIgnoreCase(strProjectMilestoneobjectId))
		 {
			 DomainObject domProjectMilestoneObj = new DomainObject(strProjectMilestoneobjectId);
			  StringList objectSelectables = new StringList(2);
			  objectSelectables.add(SELECT_ID);
			  objectSelectables.add(SELECT_NAME);
			  objectSelectables.add(SELECT_TYPE);
			  StringList relationshipSelectables = new StringList();
			  MapList mlMilestoneTracks = domProjectMilestoneObj.getRelatedObjects(context,
					  ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM,
					  DomainConstants.QUERY_WILDCARD,
					  objectSelectables,
					  relationshipSelectables,
					  true,	//to relationship - changed
					  false,	//from relationship
					  (short)1,
					  DomainConstants.EMPTY_STRING,
					  DomainConstants.EMPTY_STRING,
					  0);


			  for(int n=0;n<mlMilestoneTracks.size();n++){
				  Map mapMT = (Map)mlMilestoneTracks.get(n);
				  String strMT = (String)mapMT.get(SELECT_ID);

				  DomainObject domMT = new DomainObject(strMT);

				  MapList mlModel = domMT.getRelatedObjects(context,
						  ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_CRITERIA,
						  "*",
						  objectSelectables,
						  relationshipSelectables,
						  true,	//to relationship
						  false,	//from relationship
						  (short)1,
						  DomainConstants.EMPTY_STRING,
						  DomainConstants.EMPTY_STRING,
						  0);

				  for(int p=0;p<mlModel.size();p++){
					  Map mapModel = (Map)mlModel.get(p);
					  String strModel = (String)mapModel.get(SELECT_ID);
					  String strType = (String)mapModel.get(SELECT_TYPE);

					  if(!ProgramCentralConstants.TYPE_MODEL.equals(strType)){
						  mlMilestoneTracks.remove(mapMT);
					  }
			  }
			  }

			  for(int j=0;j<mlMilestoneTracks.size();j++){
				  Map mapMT = (Map)mlMilestoneTracks.get(j);
				  String strMT = (String)mapMT.get(SELECT_ID);
				  DomainObject domModelMTObj = new DomainObject(strMT);
				  objectSelectables.add(ProgramCentralConstants.SELECT_ATTRIBUTE_MILESTONE_DATE);
				  MapList mlModelMilestones = domModelMTObj.getRelatedObjects(context,
						  ProgramCentralConstants.RELATIONSHIP_CONFIGURATION_ITEM,
						  DomainConstants.QUERY_WILDCARD,
						  objectSelectables,
						  relationshipSelectables,
						  false,	//to relationship - changed
						  true,	//from relationship
						  (short)1,
						  DomainConstants.EMPTY_STRING,
						  DomainConstants.EMPTY_STRING,
						  0);

				  MapList mlOldMilestones = new MapList();
				  for(int k=0;k<mlModelMilestones.size();k++){
					  Map mapMilestones = (Map)mlModelMilestones.get(k);
					  String strRelationship = (String)mapMilestones.get("relationship");
					  String strDate = (String)mapMilestones.get(ProgramCentralConstants.SELECT_ATTRIBUTE_MILESTONE_DATE);
					  if(!ProgramCentralConstants.RELATIONSHIP_FIRST_CONFIGURATION_ITEM.equals(strRelationship) && null != strDate && !"null".equals(strDate) && !"".equals(strDate)){
						  mlOldMilestones.add(mapMilestones);
					  }
				  }

				  Milestone MT = new Milestone();
				  MapList mlSortedMilestones = MT.sortProjectMilestonesWithDate(context, mlOldMilestones);
				  //This needs to uncommented later
				  MT.disConnectMilestoneswithDependency(context,mlSortedMilestones);


				  MapList mlMilestones = new MapList();
				  for(int k=0;k<mlModelMilestones.size();k++){
					  Map mapMilestones = (Map)mlModelMilestones.get(k);
					  String strMilestoneID = (String)mapMilestones.get(SELECT_ID);
					  String strRelationship = (String)mapMilestones.get("relationship");
					  String strDate = (String)mapMilestones.get(ProgramCentralConstants.SELECT_ATTRIBUTE_MILESTONE_DATE);
					  if(!ProgramCentralConstants.RELATIONSHIP_FIRST_CONFIGURATION_ITEM.equals(strRelationship) && null != strDate && !"null".equals(strDate) && !"".equals(strDate) && !strMilestoneID.equals(strProjectMilestoneobjectId)){
						  mlMilestones.add(mapMilestones);
					  }
				  }

				  MapList mlNewSortedMilestones = MT.sortProjectMilestonesWithDate(context, mlMilestones);

				  MT.connectMilestoneswithDependency(context,mlNewSortedMilestones);

			  }
	 }
		 return 0;
	 }


}

