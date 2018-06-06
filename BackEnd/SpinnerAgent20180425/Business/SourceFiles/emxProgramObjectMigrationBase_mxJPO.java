import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.common.MemberRelationship;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.domain.DomainAccess;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.CacheUtil;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.program.ProgramCentralConstants;
import com.matrixone.apps.program.ProgramCentralUtil;

import matrix.db.Context;
import matrix.db.State;
import matrix.util.StringList;
/**
 * @author 
 *
 */
public class emxProgramObjectMigrationBase_mxJPO extends emxCommonMigration_mxJPO {

	private static final long serialVersionUID = -5029177381386073045L;
	final String IS_KINDOF_WORKSPACE_VAULT = "type.kindof[" + TYPE_PROJECT_VAULT + "]";
	final String IS_KINDOF_FINANCIAL_ITEM = "type.kindof[" + TYPE_FINANCIAL_ITEM + "]";
	final String IS_KINDOF_ISSUE = "type.kindof[" + ProgramCentralConstants.TYPE_ISSUE + "]";
	final String IS_KINDOF_PROJECT_SPACE = "type.kindof[" + TYPE_PROJECT_SPACE + "]";
	final String IS_KINDOF_TASK_MANAGEMENT = "type.kindof[" + TYPE_TASK_MANAGEMENT + "]";

	/**
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public emxProgramObjectMigrationBase_mxJPO(Context context,
			String[] args) throws Exception {
		super(context, args);
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void migrateObjects(Context context, StringList objectList) throws Exception
	{
		StringList mxObjectSelects = new StringList(4);
		mxObjectSelects.addElement(SELECT_ID);
		mxObjectSelects.addElement(SELECT_TYPE);
		mxObjectSelects.addElement(SELECT_NAME);
		mxObjectSelects.addElement(SELECT_REVISION);
		mxObjectSelects.addElement(SELECT_VAULT);
		mxObjectSelects.addElement(SELECT_POLICY);
		mxObjectSelects.addElement(IS_KINDOF_WORKSPACE_VAULT);
		mxObjectSelects.addElement(IS_KINDOF_FINANCIAL_ITEM);
		mxObjectSelects.addElement(SELECT_ATTRIBUTE_DEFAULT_USER_ACCESS);
		mxObjectSelects.addElement(IS_KINDOF_ISSUE);
		mxObjectSelects.addElement(IS_KINDOF_PROJECT_SPACE);
		mxObjectSelects.addElement(IS_KINDOF_TASK_MANAGEMENT); 
		MapList issueList = new MapList();
		String SELECT_FOLDER_PROJECT_ID = "to[" + DomainConstants.RELATIONSHIP_PROJECT_VAULTS + "].from.id" ;
		String SELECT_PARENT_FOLDER_ID = "to[" + DomainConstants.RELATIONSHIP_SUB_VAULTS + "].from.id" ;
		mxObjectSelects.addElement(SELECT_FOLDER_PROJECT_ID); 
		mxObjectSelects.addElement(SELECT_PARENT_FOLDER_ID); 

		String[] oidsArray = new String[objectList.size()];
		oidsArray = (String[])objectList.toArray(oidsArray);
		MapList objectInfoList = DomainObject.getInfo(context, oidsArray, mxObjectSelects);
		MapList financialItemList = new MapList();
		MapList workspaceVaultList = new MapList();
		MapList projectSpaceList = new MapList();
		MapList taskList = new MapList();

		try{


			ContextUtil.pushContext(context);
			String cmd = "trigger off";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd);
			Iterator objectInfoListIterator = objectInfoList.iterator();
			while (objectInfoListIterator.hasNext())
			{
				Map objectInfo = (Map)objectInfoListIterator.next();

				String isKindOfWorkspaceVault = (String)objectInfo.get(IS_KINDOF_WORKSPACE_VAULT);
				String isKindOfFinancialItem = (String)objectInfo.get(IS_KINDOF_FINANCIAL_ITEM);
				String isKindOfIssue = (String)objectInfo.get(IS_KINDOF_ISSUE);
				String isKindOfProjectSpace = (String)objectInfo.get(IS_KINDOF_PROJECT_SPACE);
				String isKindOfTaskManagement = (String)objectInfo.get(IS_KINDOF_TASK_MANAGEMENT);
				
				if("True".equalsIgnoreCase(isKindOfFinancialItem)){
					financialItemList.add(objectInfo);
				}else if("True".equalsIgnoreCase(isKindOfWorkspaceVault)){
					workspaceVaultList.add(objectInfo);
				}else if("True".equalsIgnoreCase(isKindOfIssue)){
					issueList.add(objectInfo);
				}else if("True".equalsIgnoreCase(isKindOfProjectSpace)){
					projectSpaceList.add(objectInfo);
				}else if("True".equalsIgnoreCase(isKindOfTaskManagement)){
					taskList.add(objectInfo);
				}
			}
			//migrateFinancialItemObjects(context, financialItemList);
			//migrateDefaultUserAccess(context, workspaceVaultList);
			//migrateIssueToReconnect(context, issueList);
			//migrateProjectToRestampProjectMember(context, projectSpaceList);
			//migrateProjectsDPJLeadersToMember(context, projectSpaceList);
			//migrateTaskDeliverableForOriginator(context, taskList);
			//migrateTaskToRestampAssignee(context, taskList);
			//migrateTaskDeliverableToReConnect(context, taskList); 
			//migrateSpecificFolderForProjectOwnerAccess(context, projectSpaceList);
			// migrateDocumentObjectForUnlockingAlreadyLockedFile(context);
            //migrateReferenceDocOfIssuesToReConnect(context, issueList);
			//migrateDomainAccessChanges(context, projectSpaceList);
			//migrateProjectFolderObjectsToReconnect(context, workspaceVaultList);
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally
		{
			String cmd = "trigger on";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd);
			ContextUtil.popContext(context);
		}
	}
	/**
	 * This method will change the type of object from "Financial Item" to "Budget"
	 */
	private  void migrateFinancialItemObjects(Context context, MapList objectList) throws Exception
	{

		try{

			ContextUtil.pushContext(context);
			String cmd = "trigger off";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd);
			Iterator objectListIterator = objectList.iterator();
			mqlLogRequiredInformationWriter("===================MIGRATION OF FINANCIAL ITEM STARTED=====================================");
			while(objectListIterator.hasNext())
			{
				Map objectInfo = (Map)objectListIterator.next();
				String objectType = (String)objectInfo.get(SELECT_TYPE);
				String objectId = (String)objectInfo.get(SELECT_ID);
				String objectName = (String)objectInfo.get(SELECT_NAME);
				String objectRevision = (String)objectInfo.get(SELECT_REVISION);
				String objectVault = (String)objectInfo.get(SELECT_VAULT);
				String objectPolicy = (String)objectInfo.get(SELECT_POLICY);

				if(TYPE_FINANCIAL_ITEM.equals(objectType)){					
					mqlLogRequiredInformationWriter("Changing the type of object " + objectId+" from "+objectType+" to "+ProgramCentralConstants.TYPE_BUDGET);
					String mqlCommand = "modify bus $1 type $2";
					MqlUtil.mqlCommand(context, mqlCommand, objectId, ProgramCentralConstants.TYPE_BUDGET);
					// Add object to list of converted OIDs
					loadMigratedOids(objectId);
				} else {
					mqlLogRequiredInformationWriter("Skipping object <<" + objectId + ">>, NO MIGRATION NEEDED");

					// Add object to list of unconverted OIDs
					String comment = "Skipping object <<" + objectId + ">> NO MIGRATIION NEEDED";
					writeUnconvertedOID(comment, objectId);
				}
			}
			mqlLogRequiredInformationWriter("===================MIGRATION OF FINANCIAL ITEM COMPLETED=====================================");
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally
		{
			String cmd = "trigger on";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd);
			ContextUtil.popContext(context);
		}
	}

	/**
	 * This method will change the "Default User Access" attribute value from "None" to "Read"
	 */
	private void migrateDefaultUserAccess(Context context, MapList objectList) throws Exception
	{

		try{

			ContextUtil.pushContext(context);
			String cmd = "trigger off";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd);
			Iterator objectListIterator = objectList.iterator();
			mqlLogRequiredInformationWriter("===================MIGRATION FOR CHANGING DEFAULT USER ACCESS STARTED=====================================");
			while(objectListIterator.hasNext())
			{
				Map objectInfo = (Map)objectListIterator.next();
				String objectId = (String)objectInfo.get(SELECT_ID);
				String defaultUserAccess = (String)objectInfo.get(SELECT_ATTRIBUTE_DEFAULT_USER_ACCESS); 

				if("None".equals(defaultUserAccess)){
					mqlLogRequiredInformationWriter("Modify value of Attribute Default User Access for folder " + objectId);
					defaultUserAccess = "Read";
					DomainObject folderObj = DomainObject.newInstance(context, objectId);       		
					folderObj.setAttributeValue(context, ATTRIBUTE_DEFAULT_USER_ACCESS, defaultUserAccess);
					// Add object to list of converted OIDs
					loadMigratedOids(objectId);
				}else{
					mqlLogRequiredInformationWriter("Skipping object <<" + objectId + ">>, NO MIGRATION NEEDED");

					// Add object to list of unconverted OIDs
					String comment = "Skipping object <<" + objectId + ">> NO MIGRATIION NEEDED";
					writeUnconvertedOID(comment, objectId);
				}
			}
			mqlLogRequiredInformationWriter("===================MIGRATION FOR CHANGING DEFAULT USER ACCESS COMPLETED=====================================");
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally
		{
			String cmd = "trigger on";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd);
			ContextUtil.popContext(context);
		}
	}
	/**
	 * This method will restamp the users who have project member access on project"
	 */
	private void migrateIssueToReconnect(Context context, MapList objectList) throws Exception {

		try{
			String RELATIONSHIP_ISSUE = PropertyUtil.getSchemaProperty("relationship_Issue" );
			ContextUtil.pushContext(context);
			Iterator objectListIterator = objectList.iterator();
			String accessBits = "all";
			mqlLogRequiredInformationWriter("===================MIGRATION FOR RECONNECTING ISSUE STARTED========================");
			while(objectListIterator.hasNext())
			{
				Map objectInfo = (Map)objectListIterator.next();
				String issueId = (String)objectInfo.get(SELECT_ID);
				DomainObject issueObj = DomainObject.newInstance(context, issueId);
				StringList busSelect = new StringList(ProgramCentralConstants.SELECT_ID);
				StringList relSelect = new StringList(DomainRelationship.SELECT_ID);

				MapList issueInfoList = issueObj.getRelatedObjects(context,
						RELATIONSHIP_ISSUE,
						QUERY_WILDCARD,
						false,
						true,
						1,
						busSelect,
						relSelect,
						"",
						null,
						0,
						null,
						null,
						null);

				Iterator issueInfoListIterator = issueInfoList.iterator();
				StringList parentIdsToAdd =  new StringList();
				
				while(issueInfoListIterator.hasNext()){
					Map issueInfoMap = (Map)issueInfoListIterator.next();
					String parentId =(String) issueInfoMap.get(ProgramCentralConstants.SELECT_ID);
					parentIdsToAdd.add(parentId);
					String connectionIdToDelete = (String) issueInfoMap.get("id[connection]");
					String select = "del connection "+connectionIdToDelete+"";
					String relId = MqlUtil.mqlCommand(context, select, true);
					String sResult = MqlUtil.mqlCommand(context,"print bus $1 select $2 dump",issueId,"access["+parentId+"|Inherited Access]");
					if("TRUE".equalsIgnoreCase(sResult)){
						String command = "modify bus " + issueId + " remove access bus " + parentId + " for 'Inherited Access' as " +  accessBits;
						MqlUtil.mqlCommand(context, command);
					}
				}

				//Reconnecting disconnected Issues...
				String cmd = "trigger on";
				MqlUtil.mqlCommand(context, mqlCommand,  cmd);
					
				for(int i=0 ; i< parentIdsToAdd.size();i++){
					String pObjectId = (String) parentIdsToAdd.get(i);
					DomainObject domParent = DomainObject.newInstance(context,pObjectId);
					DomainRelationship domRel = DomainRelationship.connect(context, 
					issueObj,
					RELATIONSHIP_ISSUE,
					domParent);
				}
				
				String cmd2 = "trigger off";
				MqlUtil.mqlCommand(context, mqlCommand,  cmd2);
				// Add object to list of converted OIDs
				loadMigratedOids(issueId);
			}
			mqlLogRequiredInformationWriter("==================MIGRATION FOR RECONNECTING ISSUE COMPLETED=========================");
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally
		{
			ContextUtil.popContext(context);
		}
	}
	/**
	 * This method will restamp the users who have project member access on project"
	 */
	private void migrateProjectToRestampProjectMember(Context context, MapList objectList) throws Exception
	{

		try{

			ContextUtil.pushContext(context);
			Iterator objectListIterator = objectList.iterator();
			mqlLogRequiredInformationWriter("================MIGRATION FOR RESTAMPING THE PROJCET MEMBER STARTED===================");
			String cmd = "trigger on";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd);
			while(objectListIterator.hasNext())
			{
				Map objectInfo = (Map)objectListIterator.next();
				String projectId = (String)objectInfo.get(SELECT_ID);
				String objectName = (String)objectInfo.get(SELECT_NAME);

				DomainObject projectObj = DomainObject.newInstance(context, projectId);
				StringList busSelect = new StringList(ProgramCentralConstants.SELECT_NAME);
				busSelect.add("from["+ProgramCentralConstants.RELATIONSHIP_MEMBER+"].to.name");
				busSelect.add(SELECT_CURRENT);

				StringList relSelect = new StringList(ProgramCentralConstants.SELECT_ID);
				relSelect.add(MemberRelationship.SELECT_PROJECT_ACCESS);
				relSelect.add(MemberRelationship.SELECT_PROJECT_ROLE);

				MapList projectInfoList = projectObj.getRelatedObjects(context,
						ProgramCentralConstants.RELATIONSHIP_MEMBER,
						ProgramCentralConstants.TYPE_PERSON,
						false,
						true,
						1,
						busSelect,
						relSelect,
						"",
						null,
						0,
						null,
						null,
						null);

				Iterator projectInfoListIterator = projectInfoList.iterator();
				StringList personIDstoAdd =  new StringList();
				StringList membersRestamped = new StringList();
				Map roleList = new HashMap();
				while(projectInfoListIterator.hasNext()){
					Map projectInfoMap = (Map)projectInfoListIterator.next();
					String accessOnProject = (String) projectInfoMap.get("attribute[Project Access].value");
					String roleOnProject = (String) projectInfoMap.get("attribute[Project Role].value");
					//If access is project member then delete connection
					if("Project Member".equalsIgnoreCase(accessOnProject)){
						String personName = (String) projectInfoMap.get(ProgramCentralConstants.SELECT_NAME);
						String personId = PersonUtil.getPersonObjectID(context, personName);
						String personState = (String) projectInfoMap.get(SELECT_CURRENT);
						if(personState.equalsIgnoreCase("Inactive")){
							continue;
						}
						ContextUtil.pushContext(context, personName ,DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
						String accessBits = MqlUtil.mqlCommand(context, "print bus "+projectId+" select current.access dump", false);
						ContextUtil.popContext(context);
						if(accessBits.contains("delete")){
							continue;
						}
						membersRestamped.add(personName);
						personIDstoAdd.add(personId);
						roleList.put(personId, roleOnProject);
						String connectionIdToDelete = (String) projectInfoMap.get("id[connection]");
						String select = "del connection "+connectionIdToDelete+"";
						String relId = MqlUtil.mqlCommand(context, select, true);
					}
				}
				//restamping deleted connections..
				for(int i=0 ; i< personIDstoAdd.size();i++){
					String personId = (String) personIDstoAdd.get(i);
					DomainObject domPerson = DomainObject.newInstance(context,personId);
					DomainRelationship domRel = DomainRelationship.connect(context, projectObj,  DomainConstants.RELATIONSHIP_MEMBER, domPerson);
					domRel.setAttributeValue(context,DomainConstants.ATTRIBUTE_PROJECT_ACCESS,"Project Member");
					domRel.setAttributeValue(context,DomainConstants.ATTRIBUTE_PROJECT_ROLE,(String)roleList.get(personId));

				}
				if(membersRestamped.size()>0){
					System.out.println("Member Restamping in project "+ objectName + " Completed");
					System.out.println("Restamped Members : "+membersRestamped);
				}
			}
			String cmd2 = "trigger off";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd2);
			mqlLogRequiredInformationWriter("=================MIGRATION FOR RESTAMPING THE PROJCET MEMBER COMPLETED======================");
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally
		{
			ContextUtil.popContext(context);
		}
	}

	/**
	 * This method converts access of DPJ members from Project Lead to project member on all projects"
	 */
	private void migrateProjectsDPJLeadersToMember(Context context, MapList objectList) throws Exception
	{

		try{

			ContextUtil.pushContext(context);
			Iterator objectListIterator = objectList.iterator();
			mqlLogRequiredInformationWriter("===============MIGRATION FOR TRANSLATION OF DPJ LEAD TO PROJCET MEMBER STARTED===================");
			while(objectListIterator.hasNext())
			{
				Map objectInfo = (Map)objectListIterator.next();
				String projectSpaceId = (String)objectInfo.get(SELECT_ID);
				String objectName = (String)objectInfo.get(SELECT_NAME);

				DomainObject projectspace = DomainObject.newInstance(context, projectSpaceId);
				StringList busSelect = new StringList(ProgramCentralConstants.SELECT_NAME);
				busSelect.add("from["+ProgramCentralConstants.RELATIONSHIP_MEMBER+"].to.name");
				busSelect.add(SELECT_CURRENT);

				StringList relSelect = new StringList(ProgramCentralConstants.SELECT_ID);
				relSelect.add(MemberRelationship.SELECT_PROJECT_ACCESS);
				relSelect.add(MemberRelationship.SELECT_PROJECT_ROLE);

				MapList projectInfoList = projectspace.getRelatedObjects(context,
						ProgramCentralConstants.RELATIONSHIP_MEMBER,
						ProgramCentralConstants.TYPE_PERSON,
						false,
						true,
						1,
						busSelect,
						relSelect,
						"",
						null,
						0,
						null,
						null,
						null);


				Iterator projectInfoListIterator = projectInfoList.iterator();
				StringList personNamesToAdd =  new StringList();
				Map roleList = new HashMap();
				while(projectInfoListIterator.hasNext()){

					Map projectInfoMap = (Map)projectInfoListIterator.next();
					String accessOnProject = (String) projectInfoMap.get("attribute[Project Access].value");
					String roleOnProject = (String) projectInfoMap.get("attribute[Project Role].value");
					String personState = (String) projectInfoMap.get(SELECT_CURRENT);
					if(personState.equalsIgnoreCase("Inactive")){
						continue;
					}
					boolean isProjectLead = false;
					//If access is project member then delete connection
					if("Project Owner".equalsIgnoreCase(accessOnProject)){
						continue;
					}
					String user  = (String)projectInfoMap.get(ProgramCentralConstants.SELECT_NAME);

					ContextUtil.pushContext(context, user ,DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
					String accessBits = MqlUtil.mqlCommand(context, "print bus "+projectSpaceId+" select current.access dump", false);
					ContextUtil.popContext(context);
					if(accessBits.contains("delete")){
						isProjectLead = true;
					}

					String mqlQuery = "print person $1 select $2 dump $3";
					List<String> mqlParameterList = new ArrayList<String>();
					mqlParameterList.add(user);
					mqlParameterList.add("product");
					mqlParameterList.add("|");

					String[] queryParameterArray = new String[mqlParameterList.size()];
					mqlParameterList.toArray(queryParameterArray);

					String productNameList = MqlUtil.mqlCommand(context, true, true, mqlQuery, true,queryParameterArray);
					StringList assignProductList = FrameworkUtil.split(productNameList, "|");

					if(assignProductList.contains(ProgramCentralConstants.DPJ_LICENSE) && !assignProductList.contains(ProgramCentralConstants.DPM_LICENSE) && isProjectLead){

						String personId = PersonUtil.getPersonObjectID(context,user);
						roleList.put(personId, roleOnProject);
						System.out.println("Access of user "+ user +" is changed from Project Lead to Project Member on Project "+ objectName);
						String user_PRJ = user+"_PRJ";

						DomainAccess.createObjectOwnership(context, projectSpaceId, "", user_PRJ, "Project Member", "Multiple Ownership For Object", true);

						String select = "print bus "+ projectSpaceId +" select from["+ DomainConstants.RELATIONSHIP_MEMBER +"|to.id==" + personId +"].id dump";
						String relId = MqlUtil.mqlCommand(context, select, true);
						DomainRelationship relOBj = new DomainRelationship(relId);
						relOBj.setAttributeValue(context,DomainConstants.ATTRIBUTE_PROJECT_ACCESS,"Project Member");
						relOBj.setAttributeValue(context,DomainConstants.ATTRIBUTE_PROJECT_ROLE,(String)roleList.get(personId));

					}
				}
			}
			mqlLogRequiredInformationWriter("================MIGRATION FOR TRANSLATION OF DPJ LEAD TO PROJCET MEMBER COMPLETED=====================");
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally
		{
			ContextUtil.popContext(context);
		}
	}

	/**
	 * This method will set Deliverable Owner as the originator for Task Deliverable"
	 */
	private void migrateTaskDeliverableForOriginator(Context context, MapList objectList) throws Exception {
		
		try{
			ContextUtil.pushContext(context);
			Iterator objectListIterator = objectList.iterator();
			mqlLogRequiredInformationWriter("===============MIGRATION FOR TASK DELIVERABLE TO SET ORIGINATOR STARTED===================");
			while(objectListIterator.hasNext())
			{
    			Map objectInfo = (Map)objectListIterator.next();
    			String taskId = (String)objectInfo.get(SELECT_ID);

    			DomainObject taskObj = DomainObject.newInstance(context, taskId);
    			
    			StringList busSelect = new StringList();
    			busSelect.add(ProgramCentralConstants.SELECT_ID);
    			busSelect.add("owner"); 

    			StringList relSelect = new StringList();

    			MapList DeliverableInfoList = taskObj.getRelatedObjects(context,
    					ProgramCentralConstants.RELATIONSHIP_TASK_DELIVERABLE,
    					ProgramCentralConstants.QUERY_WILDCARD,
    					false,
    					true,
    					1,
    					busSelect,
    					relSelect,
    					"",
    					null,
    					0,
    					null,
    					null,
    					null);

    			Iterator deliverableInfoListIterator = DeliverableInfoList.iterator();
    			while(deliverableInfoListIterator.hasNext()){

    				Map deliverableInfoMap = (Map)deliverableInfoListIterator.next();
    				String deliverableId = (String) deliverableInfoMap.get(ProgramCentralConstants.SELECT_ID);
    				String strUser = (String)deliverableInfoMap.get("owner");
    				
    				DomainObject devObject = new DomainObject();
    				
    				devObject.setId(deliverableId); 
    				devObject.setAttributeValue(context, ATTRIBUTE_ORIGINATOR, strUser);
    				loadMigratedOids(deliverableId);
    				
    			}
    		}
			mqlLogRequiredInformationWriter("================MIGRATION FOR TASK DELIVERABLE TO SET ORIGINATOR END=====================");
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally
		{
			ContextUtil.popContext(context);
		}
	}
	/**
	 * This method restamps the task assignee on task to reflect access bit changes from DomainAccess.xml
	 */
	private void migrateTaskToRestampAssignee(Context context, MapList objectList) throws Exception
	{
		try{
			ContextUtil.pushContext(context);
			Iterator objectListIterator = objectList.iterator();
			mqlLogRequiredInformationWriter("===============MIGRATION FOR RE-STAMPING TASK ASSIGNEES STARTED===================");

			final String SELECT_PARENTOBJECT_KINDOF_EXPERIMENT_PROJECT = ProgramCentralConstants.SELECT_PROJECT_TYPE+".kindof["+ProgramCentralConstants.TYPE_EXPERIMENT+"]";
		    final String SELECT_PARENTOBJECT_KINDOF_PROJECT_TEMPLATE = ProgramCentralConstants.SELECT_PROJECT_TYPE+".kindof["+DomainObject.TYPE_PROJECT_TEMPLATE+"]";
		    final String SELECT_PARENTOBJECT_KINDOF_PROJECT_CONCEPT = ProgramCentralConstants.SELECT_PROJECT_TYPE+".kindof["+DomainObject.TYPE_PROJECT_CONCEPT+"]";
		    com.matrixone.apps.program.Task task = (com.matrixone.apps.program.Task) DomainObject.newInstance(context, DomainConstants.TYPE_TASK, "PROGRAM");
		    
		    StringList objectSelects = new StringList(3);
	        objectSelects.add(SELECT_PARENTOBJECT_KINDOF_EXPERIMENT_PROJECT);
	        objectSelects.add(SELECT_PARENTOBJECT_KINDOF_PROJECT_TEMPLATE);
	        objectSelects.add(SELECT_PARENTOBJECT_KINDOF_PROJECT_CONCEPT);
		    
			while(objectListIterator.hasNext()) {

				Map objectInfo = (Map)objectListIterator.next();
				String taskId = (String)objectInfo.get(SELECT_ID);
				String taskName = (String)objectInfo.get(SELECT_NAME);

				task.setId(taskId);
				Map taskInfo = task.getInfo(context, objectSelects);
				
				String isKindOfProjectExperiment = (String) taskInfo.get(SELECT_PARENTOBJECT_KINDOF_EXPERIMENT_PROJECT);
				String isKindOfProjectTemplate = (String) taskInfo.get(SELECT_PARENTOBJECT_KINDOF_PROJECT_TEMPLATE);
				String isKindOfProjectConcept = (String) taskInfo.get(SELECT_PARENTOBJECT_KINDOF_PROJECT_CONCEPT);
				
				if("True".equalsIgnoreCase(isKindOfProjectExperiment) || "True".equalsIgnoreCase(isKindOfProjectTemplate) || "True".equalsIgnoreCase(isKindOfProjectConcept)){
					continue;
				}
				// Get list of the persons who have ownership on this task
				MapList ownershipMap = DomainAccess.getAccessSummaryList(context, taskId);
				Iterator itr = ownershipMap.iterator();

				while(itr.hasNext()){
					
					Map taskMemberInfo = (Map) itr.next();

					String accessOnTask = (String) taskMemberInfo.get("access");
					String project = (String) taskMemberInfo.get(SELECT_NAME);
					String isOwner = (String) taskMemberInfo.get("disableSelection");
					Boolean isOktoInherit = false;
					
					if(project.endsWith("_PRJ")){

						String personName = project.substring(0, project.indexOf("_PRJ"));

						String personId = PersonUtil.getPersonObjectID(context, personName);
						Person person = new Person(personId);
						State strState = person.getCurrentState(context);
						String personState = strState.getName();

						if("Inactive".equalsIgnoreCase(personState)){
							continue;
						}
						isOktoInherit =true;
					}
					
					if("Project Member".equalsIgnoreCase(accessOnTask) || "All".equalsIgnoreCase(accessOnTask)|| "True".equalsIgnoreCase(isOwner)){
						continue;
					}
					if(isOktoInherit){

						//create ownership on task for the user on that task
						DomainAccess.createObjectOwnership(context, taskId, null, project, "Project Lead", DomainAccess.COMMENT_MULTIPLE_OWNERSHIP, true);
					}
				} 
				System.out.println("Restamping the assignees of the Task -  : "+ taskName +"  is DONE");
			}
			mqlLogRequiredInformationWriter("===============MIGRATION FOR RE-STAMPING TASK ASSIGNEES COMPLETED====================");
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally
		{
			ContextUtil.popContext(context);
		}
	}
	
		/**
	 * This method will re-connect the Task Deliverables to tasks to create ownership & remove Inherited access"
	 */
	private void migrateTaskDeliverableToReConnect(Context context, MapList objectList) throws Exception {

		try{
			ContextUtil.pushContext(context);
			Iterator objectListIterator = objectList.iterator();
			String accessBits = "all";
			mqlLogRequiredInformationWriter("===================MIGRATION FOR RECONNECTING TASK DELIVERABLE STARTED========================");
			while(objectListIterator.hasNext())
			{
				Map objectInfo = (Map)objectListIterator.next();
				String taskId = (String)objectInfo.get(SELECT_ID);
				DomainObject taskObj = DomainObject.newInstance(context, taskId);
				StringList busSelect = new StringList(ProgramCentralConstants.SELECT_ID);
				StringList relSelect = new StringList(DomainRelationship.SELECT_ID);

				MapList deliverableInfoList = taskObj.getRelatedObjects(context,
    					ProgramCentralConstants.RELATIONSHIP_TASK_DELIVERABLE,
    					ProgramCentralConstants.QUERY_WILDCARD,
    					false,
    					true,
    					1,
    					busSelect,
    					relSelect,
    					"",
    					null,
    					0,
    					null,
    					null,
    					null);


				Iterator deliverableInfoListIterator = deliverableInfoList.iterator();
				StringList childIdsToAdd =  new StringList();
				
				while(deliverableInfoListIterator.hasNext()){
					Map deliverableInfoMap = (Map)deliverableInfoListIterator.next();
					String deliverableId =(String) deliverableInfoMap.get(ProgramCentralConstants.SELECT_ID);
					childIdsToAdd.add(deliverableId);
					String connectionIdToDelete = (String) deliverableInfoMap.get("id[connection]");
					String select = "del connection "+connectionIdToDelete+"";
					String relId = MqlUtil.mqlCommand(context, select, true);
					String sResult = MqlUtil.mqlCommand(context,"print bus $1 select $2 dump",deliverableId,"access["+taskId+"|Inherited Access]");
					if("TRUE".equalsIgnoreCase(sResult)){
						String command = "modify bus " + deliverableId + " remove access bus " + taskId + " for 'Inherited Access' as " +  accessBits;
						MqlUtil.mqlCommand(context, command);
					}
				}

				//Reconnecting disconnected Task Deliverables...
				for(int i=0 ; i< childIdsToAdd.size();i++){
					String cObjectId = (String) childIdsToAdd.get(i);
					DomainObject domChild = DomainObject.newInstance(context,cObjectId);
					DomainRelationship domRel = DomainRelationship.connect(context, taskObj,  ProgramCentralConstants.RELATIONSHIP_TASK_DELIVERABLE, domChild);
					DomainAccess.createObjectOwnership(context, cObjectId, taskId, DomainAccess.COMMENT_MULTIPLE_OWNERSHIP);
				}
				// Add object to list of converted OIDs(taskIds)
				loadMigratedOids(taskId);
			}
			mqlLogRequiredInformationWriter("==================MIGRATION FOR RECONNECTING TASK DELIVERABLE COMPLETED=========================");
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally
		{
			ContextUtil.popContext(context);
		}
	}

	/**
	 * This method will set access of the Project Owner on Specific Folder to "Full" 
	 */
	private void migrateSpecificFolderForProjectOwnerAccess(Context context, MapList objectList) throws Exception
	{
		try{
			ProgramCentralUtil.pushUserContext(context);
			Iterator objectListIterator = objectList.iterator();
			String access = "Full";
			mqlLogRequiredInformationWriter("===================MIGRATION FOR SETTING PROJECT OWNER ACESS TO FULL STARTED========================");
			while(objectListIterator.hasNext())
			{
				Map objectInfo = (Map)objectListIterator.next();
				String projectId = (String)objectInfo.get(SELECT_ID);
				DomainObject projectObj = DomainObject.newInstance(context, projectId);
				String owner = projectObj.getInfo(context, SELECT_OWNER);
				String ownerID = PersonUtil.getPersonObjectID(context, owner);
				String projectName = projectObj.getInfo(context, SELECT_NAME);


				StringList busSelect = new StringList(ProgramCentralConstants.SELECT_ID);
				StringList relSelect = new StringList();
				String whereClause = "attribute["+ProgramCentralConstants.ATTRIBUTE_ACCESS_TYPE+"] == Specific && owner != \""+owner+"\"" ;

				MapList folderInfoList = projectObj.getRelatedObjects(context,
						"Data Vaults,Sub Vaults", //pattern to match relationships
						ProgramCentralConstants.QUERY_WILDCARD, //pattern to match types
						busSelect, //the eMatrix StringList object that holds the list of select statement pertaining to Business Obejcts.
						relSelect, //the eMatrix StringList object that holds the list of select statement pertaining to Relationships.
						false, //get To relationships
						true, //get From relationships
						(short)0, //the number of levels to expand, 0 equals expand all.
						whereClause, //where clause to apply to objects, can be empty ""
						"", //where clause to apply to relationship, can be empty ""
						0);

				Iterator folderInfoListIterator = folderInfoList.iterator();

				mqlLogRequiredInformationWriter("Migrating for project: "+ projectName);
				
				while(folderInfoListIterator.hasNext()){
					Map folderInfo = (Map)folderInfoListIterator.next();
					String folderId = (String)folderInfo.get(ProgramCentralConstants.SELECT_ID);
					DomainAccess.createObjectOwnership(context,folderId, ownerID,access,DomainAccess.COMMENT_MULTIPLE_OWNERSHIP);
				}
			}
			mqlLogRequiredInformationWriter("==================MIGRATION FOR SETTING PROJECT OWNER ACESS TO FULL STARTED=========================");
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally
		{
			ProgramCentralUtil.popUserContext(context);
		}
	}
	
	/**
	 * This method will unlock the previously locked document files which can't be unlocked from UI
	 */
	private void migrateDocumentObjectForUnlockingAlreadyLockedFile(Context context) throws Exception
	{
		try{
			ProgramCentralUtil.pushUserContext(context);
			mqlLogRequiredInformationWriter("===================MIGRATION FOR UNLOCKING PREVIOUSLY OLDER REVISION LOCKED DOCUMENT STARTED========================");
			String typePattern = "DOCUMENTS,DOCUMENT CLASSIFICATION";
			StringList slRelSelects  = new StringList();
			StringList selectList = new StringList(3);
			selectList.add(DomainConstants.SELECT_ID);
			selectList.add(DomainConstants.SELECT_NAME);
			selectList.add(CommonDocument.SELECT_LAST_ID);
			
			String strWhere ="locked==TRUE";
			ContextUtil.startTransaction(context, true);
			MapList documentInfoList = DomainObject.findObjects(context,typePattern, DomainConstants.QUERY_WILDCARD, strWhere, selectList);
			DomainObject docObj = DomainObject.newInstance(context, DomainConstants.TYPE_DOCUMENT);
			int totalLockedDoc = documentInfoList.size();
			int totalUnlockedDoc = 0;
			for (int i =0 ;i< totalLockedDoc ; i++){
				Map infoMap = (Map)documentInfoList.get(i);
				String lastId = (String) infoMap.get(CommonDocument.SELECT_LAST_ID);
				String id = (String) infoMap.get(CommonDocument.SELECT_ID);
				String name = (String) infoMap.get(CommonDocument.SELECT_NAME);
				if(!lastId.equals(id)){
					docObj.setId(id);
					mqlLogRequiredInformationWriter("Unlocking Document: "+ name+" ID:"+id);
					docObj.unlock(context);
					loadMigratedOids(id);
					totalUnlockedDoc++;
				}else{
					writeUnconvertedOID(id);
				}
			}
			ContextUtil.commitTransaction(context);
			mqlLogRequiredInformationWriter(totalUnlockedDoc+" Objects unlocked out of "+totalLockedDoc);
			mqlLogRequiredInformationWriter("==================MIGRATION FOR UNLOCKING PREVIOUSLY OLDER REVISION LOCKED DOCUMENT ENDED=========================");
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally
		{
			ProgramCentralUtil.popUserContext(context);
		}
	}
	
	/**
	 * This script, removes the inherited access and creates ownership on all reference documents connected to the issues.
	 * @param context
	 * @param objectList
	 * @throws Exception
	 */
	private void migrateReferenceDocOfIssuesToReConnect(Context context, MapList objectList) throws Exception {

		try{
			ContextUtil.pushContext(context);
			Iterator objectListIterator = objectList.iterator();
			String accessBits = "all";
			mqlLogRequiredInformationWriter("===================MIGRATION FOR REFERENCE DOCUMENT OF ISSUES IS STARTED========================");
			while(objectListIterator.hasNext())
			{
				Map objectInfo = (Map)objectListIterator.next();
				String issueId = (String)objectInfo.get(SELECT_ID);
				DomainObject issueObj = DomainObject.newInstance(context, issueId);
				StringList busSelect = new StringList(ProgramCentralConstants.SELECT_ID);
				StringList relSelect = new StringList(DomainRelationship.SELECT_ID);

				MapList docInfoList = issueObj.getRelatedObjects(context,
						ProgramCentralConstants.RELATIONSHIP_REFERENCE_DOCUMENT,
						ProgramCentralConstants.QUERY_WILDCARD,
						false,
						true,
						1,
						busSelect,
						relSelect,
						"",
						null,
						0,
						null,
						null,
						null);


				Iterator docInfoListIterator = docInfoList.iterator();
				StringList childIdsToAdd =  new StringList();
				
				while(docInfoListIterator.hasNext()){
					Map docInfoMap = (Map)docInfoListIterator.next();
					String docId =(String) docInfoMap.get(ProgramCentralConstants.SELECT_ID);
					childIdsToAdd.add(docId);
					String connectionIdToDelete = (String) docInfoMap.get("id[connection]");
					String select = "del connection "+connectionIdToDelete+"";
					String relId = MqlUtil.mqlCommand(context, select, true);
					String sResult = MqlUtil.mqlCommand(context,"print bus $1 select $2 dump",docId,"access["+issueId+"|Inherited Access]");
					if("TRUE".equalsIgnoreCase(sResult)){
						String command = "modify bus " + docId + " remove access bus " + issueId + " for 'Inherited Access' as " +  accessBits;
						MqlUtil.mqlCommand(context, command);
					}
				}

				//Reconnecting disconnected Reference documents...
				String cmd = "trigger on";
				MqlUtil.mqlCommand(context, cmd);
				for(int i=0 ; i< childIdsToAdd.size();i++){
					String cObjectId = EMPTY_STRING;
					try{
					cObjectId = (String) childIdsToAdd.get(i);
					DomainObject domChild = DomainObject.newInstance(context,cObjectId);
					DomainRelationship domRel = DomainRelationship.connect(context, issueObj,  ProgramCentralConstants.RELATIONSHIP_REFERENCE_DOCUMENT, domChild);
					// Add object to list of converted OIDs(documentId)
					loadMigratedOids(cObjectId);
					}catch(Exception e){
						// Add object to list of unconverted OIDs(IssueId & documentId)
						writeUnconvertedOID("UnConverted IssueId : " + issueId+" & Document Id : "+cObjectId); 
						mqlLogRequiredInformationWriter("UnConverted IssueId : " + issueId+" & Document Id : "+cObjectId);
						e.printStackTrace();
					}
				}
				String cmd2 = "trigger off";
				MqlUtil.mqlCommand(context,cmd2);
			}
			mqlLogRequiredInformationWriter("==================MIGRATION FOR REFERENCE DOCUMENT OF ISSUES IS COMPLETED=========================");
		}
		finally
		{
			ContextUtil.popContext(context);
		}
	}
	
	/**
	 * This method will set access of the Folder Owner on Specific Folder to "Full" Earlier it was "Add Remove".
	 * This Migration written for IR-490492-3DEXPERIENCER2015x
	 */
	private void migrateSpecificFoldersForOwnerAccess(Context context, MapList objectList) throws Exception
	{
		try{
			ProgramCentralUtil.pushUserContext(context);
			Iterator objectListIterator = objectList.iterator();
			String access = "Full";
			mqlLogRequiredInformationWriter("===================MIGRATION FOR SETTING PROJECT OWNER ACESS TO FULL STARTED========================");
			while(objectListIterator.hasNext())
			{
				Map objectInfo = (Map)objectListIterator.next();
				String projectId = (String)objectInfo.get(SELECT_ID);
				DomainObject projectObj = DomainObject.newInstance(context, projectId);
				String projectName = projectObj.getInfo(context, SELECT_NAME);

				StringList busSelect = new StringList(2);
				busSelect.add(ProgramCentralConstants.SELECT_ID);
				busSelect.add(ProgramCentralConstants.SELECT_OWNER);
				StringList relSelect = new StringList();
				String whereClause = "attribute["+ProgramCentralConstants.ATTRIBUTE_ACCESS_TYPE+"] == Specific" ;

				MapList folderInfoList = projectObj.getRelatedObjects(context,
						"Data Vaults,Sub Vaults", //pattern to match relationships
						ProgramCentralConstants.QUERY_WILDCARD, //pattern to match types
						busSelect, //the eMatrix StringList object that holds the list of select statement pertaining to Business Obejcts.
						relSelect, //the eMatrix StringList object that holds the list of select statement pertaining to Relationships.
						false, //get To relationships
						true, //get From relationships
						(short)0, //the number of levels to expand, 0 equals expand all.
						whereClause, //where clause to apply to objects, can be empty ""
						"", //where clause to apply to relationship, can be empty ""
						0);

				Iterator folderInfoListIterator = folderInfoList.iterator();

				mqlLogRequiredInformationWriter("Migrating for project: "+ projectName);
				
				while(folderInfoListIterator.hasNext()){
					Map folderInfo = (Map)folderInfoListIterator.next();
					String folderId = (String)folderInfo.get(ProgramCentralConstants.SELECT_ID);
					String folderOwner = (String)folderInfo.get(ProgramCentralConstants.SELECT_OWNER);
					String ownerId = PersonUtil.getPersonObjectID(context, folderOwner);
					mqlLogRequiredInformationWriter("Migrating for folderId: "+ folderId);
					DomainAccess.createObjectOwnership(context,folderId, ownerId,access,DomainAccess.COMMENT_MULTIPLE_OWNERSHIP);
				}
			}
			mqlLogRequiredInformationWriter("==================MIGRATION FOR SETTING PROJECT OWNER ACESS TO FULL STARTED=========================");
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally
		{
			ProgramCentralUtil.popUserContext(context);
		}
	}
	
	/**
	 * Added for DomainAccess.xml changes.
	 * Added for 498140 but it will applicable for all DomainAccess.xml changes.
	 * @param context
	 * @param objectList
	 * @throws Exception
	 */
	private void migrateDomainAccessChanges(Context context, MapList objectList) throws Exception
	{
		try{
			mqlLogRequiredInformationWriter("================MIGRATION FOR RESTAMPING THE PROJCET MEMBER/LEAD STARTED===================");

			Map policiesLogicalNames = (Map) CacheUtil.getCacheObject(context, "policy_logical_mappings");

			for(int i=0,size = objectList.size(); i<size; i++){
				Map projectInfo 	= (Map)objectList.get(i);
				String projectId 	= (String)projectInfo.get(SELECT_ID);

				MapList accessList = DomainAccess.getAccessSummaryList(context, projectId);

				StringList personListForProjectLead = new StringList();
				StringList personListForProjectMember = new StringList();
				String projectMember = EMPTY_STRING;
				String projectLead = EMPTY_STRING;;

				for (int j=0; j < accessList.size(); j++) {
					Map<?, ?> map = (Map<?, ?>) accessList.get(j);
					String ownershipProject = (String)map.get("name");
					String accessMasks = (String) map.get(DomainAccess.KEY_ACCESS_GRANTED);

					if("All".equalsIgnoreCase(accessMasks)){
						continue;
					}

					if(!"Project Member".equalsIgnoreCase(accessMasks) && !"Project Lead".equalsIgnoreCase(accessMasks)){
						accessMasks =FrameworkUtil.findAndReplace(accessMasks, " ", "");
						StringList accessMaskList = FrameworkUtil.split(accessMasks, ",");
						int accessMasksSize = accessMaskList.size();

						if(policiesLogicalNames == null){ //hard coded based on FP1701
							projectMember = "read,show,checkout,fromconnect,fromdisconnect,execute";
							projectLead = "read,show,checkout,checkin,fromconnect,fromdisconnect,execute,changeowner,modify,delete,promote,demote,toconnect,todisconnect,changename,changetype,changepolicy,revise";
						}else{
							Map policySettings = (Map) policiesLogicalNames.get("Project Space");
							projectMember 	= (String)policySettings.get("Project Member");
							projectLead 	= (String)policySettings.get("Project Lead");
						}

						projectMember =FrameworkUtil.findAndReplace(projectMember, " ", "");
						projectLead =FrameworkUtil.findAndReplace(projectLead, " ", "");

						StringList projectMemberAccessBit = FrameworkUtil.split(projectMember, ",");
						StringList projectLeadAccessBit = FrameworkUtil.split(projectLead, ",");

						int memberAccessBit = projectMemberAccessBit.size();
						int leadAccessBit 	= projectLeadAccessBit.size();

						if(accessMasksSize < memberAccessBit){
							personListForProjectMember.addElement(ownershipProject);
						}else if(accessMasksSize == memberAccessBit){
							personListForProjectMember.addElement(ownershipProject);
						}else if(accessMasksSize > memberAccessBit && accessMasksSize < (memberAccessBit + 4)){ //corrector should correct this condition based on DomainAccess changes.
							personListForProjectMember.addElement(ownershipProject);
						}else if(leadAccessBit == accessMasksSize){
							personListForProjectLead.addElement(ownershipProject);
						}else{
							personListForProjectLead.addElement(ownershipProject);
						}
					}
				}

				//Remove existing ownership and create new

				if(personListForProjectMember != null && !personListForProjectMember.isEmpty()){
					for(int k=0;k<personListForProjectMember.size();k++){
						String ownershipPRJ =  (String)personListForProjectMember.get(k);
						String ownershipProject = MqlUtil.mqlCommand(context,"print role '" + ownershipPRJ + "' select person dump");
						String personId 	= PersonUtil.getPersonObjectID(context, ownershipProject); 
						String personState = MqlUtil.mqlCommand(context,"print bus "+ personId+" select current dump");

						if(personState.equalsIgnoreCase("Inactive")){
							continue;
						}

						DomainAccess.deleteObjectOwnership(context, projectId, null, ownershipPRJ, DomainAccess.COMMENT_MULTIPLE_OWNERSHIP);
						DomainAccess.createObjectOwnership(context, projectId, personId, "Project Member", DomainAccess.COMMENT_MULTIPLE_OWNERSHIP);
						loadMigratedOids(projectId);
					}
				}

				if(personListForProjectLead != null && !personListForProjectLead.isEmpty()){
					for(int k=0;k<personListForProjectLead.size();k++){
						String ownershipPRJ =  (String)personListForProjectLead.get(k);
						String ownershipProject 	= MqlUtil.mqlCommand(context,"print role '" + ownershipPRJ + "' select person dump");
						String personId 	= PersonUtil.getPersonObjectID(context, ownershipProject); 
						String personState 	= MqlUtil.mqlCommand(context,"print bus "+ personId+" select current dump");

						if(personState.equalsIgnoreCase("Inactive")){
							continue;
						}

						DomainAccess.deleteObjectOwnership(context, projectId, null, ownershipPRJ, DomainAccess.COMMENT_MULTIPLE_OWNERSHIP);
						DomainAccess.createObjectOwnership(context, projectId, personId, "Project Lead", DomainAccess.COMMENT_MULTIPLE_OWNERSHIP);
						loadMigratedOids(projectId);
					}
				}

			}

			mqlLogRequiredInformationWriter("=================MIGRATION FOR RESTAMPING THE PROJCET MEMBER COMPLETED======================");
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void mqlLogRequiredInformationWriter(String command) throws Exception
	{
		super.mqlLogRequiredInformationWriter(command +"\n");
	}
	public void mqlLogWriter(String command) throws Exception
	{
		super.mqlLogWriter(command +"\n");
	}
	
	
	/**
	 * This method will connect the object "Project Folder" which are not connected to any ProjectFolder/ProjectSpace
	 * These "Project Folder" objects are disconnected from parent object in CUT-PASTE operation
	 */
	private void migrateProjectFolderObjectsToReconnect(Context context, MapList objectList) throws Exception
	{
		try{
			ContextUtil.pushContext(context);
			String cmd = "trigger off";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd);
			Iterator objectListIterator = objectList.iterator();
			mqlLogRequiredInformationWriter("===================MIGRATION OF PROJECT FOLDER STARTED=====================================");

			while(objectListIterator.hasNext())
			{
				Map objectInfo = (Map)objectListIterator.next();
				String objectType = (String)objectInfo.get(SELECT_TYPE);
				String objectId = (String)objectInfo.get(SELECT_ID);
				String objectName = (String)objectInfo.get(SELECT_NAME);
				String objectRevision = (String)objectInfo.get(SELECT_REVISION);
				String isKindOfWorkspaceVault = (String)objectInfo.get(DomainConstants.SELECT_KINDOF_WORKSPACE_VAULT);
				String objectPolicy = (String)objectInfo.get(SELECT_POLICY);
				String SELECT_FOLDER_PROJECT_ID = "to[" + DomainConstants.RELATIONSHIP_PROJECT_VAULTS + "].from.id" ;
				String SELECT_PARENT_FOLDER_ID = "to[" + DomainConstants.RELATIONSHIP_SUB_VAULTS + "].from.id" ;
				String projectId = (String)objectInfo.get(SELECT_FOLDER_PROJECT_ID);
				String parentFolderId = (String)objectInfo.get(SELECT_PARENT_FOLDER_ID);

				if("true".equalsIgnoreCase(isKindOfWorkspaceVault) && ProgramCentralUtil.isNullString(projectId) && ProgramCentralUtil.isNullString(parentFolderId)){					
					DomainObject dobj = new DomainObject(objectId);
					StringList slHistory = dobj.getInfoList(context, "history.connect");
					for(int i = slHistory.size() -1; i>0; i-- ){
						String str = (String) slHistory.get(i); 
						if(str.indexOf("disconnect Sub Vaults") >= 0 || str.indexOf("disconnect Data Vaults") >= 0){
							//disconnect Sub Vaults from Workspace Vault eee CF4A4D5A00004524598952BA000004E5 - user: pl1  time: 8/8/2017 11:43:19 AM  state: Exists
							mqlLogRequiredInformationWriter("=====================================OBJECT ID OF MIGRATED PROJECT FOLDER : "+objectId+"=====================================");
							String type = str.indexOf("Workspace Vault")>0 ? "Workspace Vault" : "Controlled Folder";
							int typeIndex = str.indexOf(type) + type.length();
							int userIndex = str.indexOf("- user");
							String name = str.substring(typeIndex, userIndex).trim();
							int lastSpaceIndex  = name.lastIndexOf(" ");
							String revision = name.substring(lastSpaceIndex).trim();
							name = name.substring(0, lastSpaceIndex);							
							String command = "print bus \""+type+"\" \""+name+"\" "+revision+" select id dump |";
							mqlLogRequiredInformationWriter("==================command : "+command+"==================");
							String sResult = MqlUtil.mqlCommand(context,command,true);
							mqlLogRequiredInformationWriter("==================OBJECT ID OF PREVIOUS PARENT : "+sResult+"==================");
							DomainObject domParent = DomainObject.newInstance(context,sResult.trim());
							String relName = str.indexOf("Sub Vaults")>0 ? "Sub Vaults" : "Data Vaults";
							DomainRelationship domRel = DomainRelationship.connect(context,domParent,relName,dobj);
							break;
						}
					}
				} else {
					mqlLogRequiredInformationWriter("Skipping object <<" + objectId + ">>, NO MIGRATION NEEDED");
					// Add object to list of unconverted OIDs
					String comment = "Skipping object <<" + objectId + ">> NO MIGRATIION NEEDED";
					writeUnconvertedOID(comment, objectId);
				}
			}
			mqlLogRequiredInformationWriter("===================MIGRATION OF PROJECT FOLDER COMPLETED=====================================");
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		finally
		{
			String cmd = "trigger on";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd);
			ContextUtil.popContext(context);
		}
	}
	
}
