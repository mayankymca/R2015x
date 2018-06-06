import java.util.HashMap;
import java.util.Map;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.requirements.RequirementsUtil;



/**
*
*${CLASS:emxRequirementGroup}.java
*
*class for handling user interaction with the Requirement Group data model
*
*/

/**
 * The <code>emxRequirementGroupBase</code> class provides the functionality related to Requirement Group
 * @author NZR
 */
/*
 @quickreview LX6 JX5 13 Sep 12(IR-123110V6R2014  "FIR : Requirement Groups having parent are displayed in search result while attach Existing Groups.")
 @quickreview QYG     02/05/2013 	IR-216195V6R2014	workaround regression with effectivity toolbar
 @quickreview ZUD HAT1 03/06/2013   IR-282618V6R2015x  STP: Unwanted error message is displayed when user try to add sub Specification Folder which is attached to any other Specification Folder  
*/
public class emxRequirementGroupBase_mxJPO extends DomainObject
{

	/*serialization compatibility level*/
	private static final long serialVersionUID=1L; 
	
	/**
	 * constructor
	 * @param context the eMatrix <code>Context</code> object
	 * @param args  holds arguments
	 * @throws Exception if operation fails
	 * @since R212
	 * @author NZR
	 */
	public emxRequirementGroupBase_mxJPO (Context context, String[] args)
	throws Exception
	{
		super();	
	
	}

	/**
	 * Main method
	 * @param context context the eMatrix <code>Context</code> object
	 * @param args no argument
	 * @return an integer - status code. 0 if OK. 
	 * @throws Exception if problem in AEF or unconnected context.
	 * @since R212
	 * @author NZR
	 */
	public int mxMain(Context context, String[] args)
	throws Exception
	{
		if(!context.isConnected())
		{
			throw new Exception("Not supported on desktop client"); 
		}
		return 0;
	}

	/**
	* GetOwnedGroups: returns all bundle owned by the current user
	*
	* @param context : matrix one context
	* @param args : matrixone packed argument - not used
	* @return a MapList containing the owned requirement bundle - can be empty.
	* @throws Exception if bundle retrieval fails.
	* @since R212
	* @author NZR
	*/
	public MapList getOwnedGroups(final Context context, final String[] args)
	throws Exception
	{

		String strOwner = context.getUser();
		StringList objectSelect = new StringList(DomainConstants.SELECT_ID);
		String strType = RequirementsUtil.getRequirementGroupType(context);//need to be put as ProductLineConstants.TYPE_REQUIREMENTBUNDLE.
		MapList mapBusId = findObjects(context,strType,
			 null,null,strOwner,DomainConstants.QUERY_WILDCARD,"",true,objectSelect);
		return mapBusId;
	}

	/**
	* GetAllGroups: get all requirement bundle from the system
	* 
	* @param context matrix one context
	* @param args  matrixone packed argument - not used
	* @return a MapList containing all requirement bundles - can be empty.
	* @throws Exception if bundle retrieval fails.
	* @since R212
	* @author NZR
	*/
	public MapList getAllGroups(final Context context, final String[] args)
	throws Exception
	{
		StringList objectSelect = new StringList(DomainConstants.SELECT_ID);
		String strType = RequirementsUtil.getRequirementGroupType(context); //need to be put as ProductLineConstants.TYPE_REQUIREMENTBUNDLE.
		MapList mapBusId = findObjects(context,strType,null,null,DomainConstants.QUERY_WILDCARD, 
			DomainConstants.QUERY_WILDCARD,"",true,objectSelect);
		return mapBusId;
	}

	/**
	 * Expand bundles Program used to perform expand operation in the Requirement bundle Structure view
	 * 
	 * @param context matrix one context
	 * @param args matrix one packed arguments. Expecting at least: the object Id in the paramMap
	 * @return Maplist containing the retrieved objects.
	 * @throws Exception if navigation fails.
	 * @since R212
	 * @author NZR
	 */
	@com.matrixone.apps.framework.ui.ProgramCallable
	public MapList expandGroup(final Context context, final String[] args)
	throws Exception
	{
		Map programMap = (HashMap)JPO.unpackArgs(args);
		String objectId = (String)programMap.get("objectId");
		if (null == objectId || objectId.length() == 0)
		{
			throw new Exception("failed to retrieve Object Id in parameters.");
		}


		String expandLvl = (String)programMap.get("expandLevel");
		if (null == expandLvl)
		{	//can happen. see bug 361315
			expandLvl = (String)programMap.get("compareLevel"); 
		}
		if ("All".equalsIgnoreCase(expandLvl))
		{
			expandLvl = "0";
		}
		if (expandLvl == null || expandLvl.length() == 0)
		{
			expandLvl = "1";
		}
		int maxLevels = Integer.parseInt(expandLvl);
		StringList objSelect = new StringList(SELECT_ID);
			objSelect.addElement(SELECT_TYPE);
			objSelect.addElement(SELECT_NAME);
			objSelect.addElement(SELECT_REVISION);
			objSelect.addElement(SELECT_DESCRIPTION);
		StringList relSelect = new StringList(SELECT_RELATIONSHIP_ID);
		
		String relTypes =  RequirementsUtil.getSubRequirementGroupRelationship(context) + ","+ RequirementsUtil.getRequirementGroupContentRelationship(context);
		String objTypes = RequirementsUtil.getRequirementSpecificationType(context) +","+ RequirementsUtil.getRequirementGroupType(context);

		DomainObject obj = new DomainObject(objectId);
	    String  effectivityFilter = (String) programMap.get("CFFExpressionFilterInput_OID");
        //BPS regression, temp fix
        if("undefined".equalsIgnoreCase(effectivityFilter)){
       	 effectivityFilter = null;
        }
	    MapList expanded = null ;
	    if(effectivityFilter != null && effectivityFilter.length() != 0)
	    {
	    	expanded = obj.getRelatedObjects(context,
						 relTypes,     // relationship pattern
						 objTypes,                     // object pattern
						 objSelect,                      // object selects
						 relSelect,                 // relationship selects
						  false,                               // to direction
						  true,                               // from direction
						  (short) maxLevels,                 // recursion level
						  null,                              // object where clause
						  null,                              // relationship where clause
						  (short)0,                        	 // limit
						  CHECK_HIDDEN,            				 // check hidden
						  PREVENT_DUPLICATES,   			// prevent duplicates
						  PAGE_SIZE,                   		// pagesize
						  null,                              // includeType
						  null,                              // includeRelationship
						  null,                              // includeMap
						  null,                              // relKeyPrefix
						  effectivityFilter);            // Effectivity filter expression from the SB toolbar
	         
	         
	    	
	    }
	    else
	    	expanded =  obj.getRelatedObjects(context, relTypes, objTypes, objSelect, relSelect, false, true,(short) maxLevels, "", "",0);
	    
        HashMap hmTemp = new HashMap();
    	hmTemp.put("expandMultiLevelsJPO","true");
    	expanded.add(hmTemp);
	    
    	return expanded;
	}
	
	/**
	 * Returns a list containing all parents and children ids of a given Requirement Group
	 * 
	 * This method is used to check if a given requirement group is part of a structure (connect check - and exclude program for search)
	 * 
	 * @param context MatrixOne context object
	 * @param args packed arguments.  Expecting at least the "objectId"  of a requirement group in the Program Map
	 * @return a StringList containing all ids of parents and children of the requirement group.
	 * @throws Exception if the navigation fails.
	 */
	public StringList getParentsAndChildrenIds(final Context context,final String[] args)
	throws Exception
	{
		//extract current object
		Map programMap = (HashMap)JPO.unpackArgs(args);
		String objectId = (String)programMap.get("objectId");
		if(objectId == null || objectId.length() == 0)
			throw new IllegalArgumentException("getParentsAndChildrenIds method failure: invalid object id passed as parameter.");
		
		return  getParentsAndChildrenIds(context, objectId);

	}
	
	private StringList getParentsAndChildrenIds(final Context context, final String objectId )
	throws Exception
	{
		DomainObject obj = new DomainObject(objectId); 
		
		//include current object id 
		StringList ids= new StringList(objectId); 
		//get his parents (up to the root)
		StringList objSelect = new StringList(SELECT_ID);
		objSelect.addElement(SELECT_TYPE);
		objSelect.addElement(SELECT_NAME);
		objSelect.addElement(SELECT_REVISION);
		objSelect.addElement(SELECT_DESCRIPTION);
		StringList relSelect = new StringList(SELECT_RELATIONSHIP_ID);
		

		String relTypes =  RequirementsUtil.getSubRequirementGroupRelationship(context);
		String objTypes =  RequirementsUtil.getRequirementGroupType(context);
			
		MapList parentsInfo = obj.getRelatedObjects(context, relTypes, objTypes, objSelect, relSelect, true , false,(short) 0, "", "",0);
		//add parents ids to the list
		for (int i = 0; i < parentsInfo.size() ; i++)
		{
			Map infos = (Map)parentsInfo.get(i);
			String id = (String) infos.get(DomainObject.SELECT_ID);
			if(!ids.contains(id))
			{
				ids.add(id); 
			}
		}
		
		//get his children (down to the leaves)
		MapList childrenInfo =  obj.getRelatedObjects(context, relTypes, objTypes, objSelect, relSelect, false , true,(short) 0, "", "",0);
		//add children IDs to the list
		for (int i = 0; i < childrenInfo.size() ; i++)
		{
			Map infos = (Map)childrenInfo.get(i);
			String id = (String) infos.get(DomainObject.SELECT_ID);
			if(!ids.contains(id))
			{
				ids.add(id); 
			}
		}		
		return ids; 
		
	}
	
	/**
	 * Returns the exclude OID list used when searching for specifications 
	 * to attach to a group. 
	 * 
	 * @param context MatrixOne context object 
	 * @param args packed arguments expecting at least: the group id in the program map
	 * @return a string list containing the ids of specification that CANNOT be attached to this group.
	 * @throws Exception if the navigation fails.
	 */
	@com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
	public StringList getSearchReqSpecExcludeList(final Context context, final String[] args)
	throws Exception
	{
		
		Map programMap = (HashMap)JPO.unpackArgs(args);
		String objectId = (String)programMap.get("objectId");
		if(objectId == null || objectId.length() == 0)
			throw new IllegalArgumentException("getParentsAndChildrenIds method failure: invalid object id passed as parameter.");
		
		DomainObject obj = new DomainObject(objectId);
		String relTypes =  RequirementsUtil.getRequirementGroupContentRelationship(context);
		String objTypes =  RequirementsUtil.getRequirementSpecificationType(context) ;
		StringList objSelect = new StringList(SELECT_ID);
		objSelect.addElement(SELECT_TYPE);
		objSelect.addElement(SELECT_NAME);
		objSelect.addElement(SELECT_REVISION);
		objSelect.addElement(SELECT_DESCRIPTION);
		StringList relSelect = new StringList(SELECT_RELATIONSHIP_ID);
		
		MapList items = obj.getRelatedObjects(context, relTypes, objTypes, objSelect, relSelect, false , true,(short) 1, "", "",0);
		StringList ids= new StringList();
		for (int i = 0; i < items.size() ; i++)
		{
			Map infos = (Map)items.get(i);
			String id = (String) infos.get(DomainObject.SELECT_ID);
			if(!ids.contains(id))
			{
				ids.add(id); 
			}
		}
		return ids;
	}
	
	/**
	 * TRIGGERS 
	 */

	/**
	 * Checks if a requirement can be connected to another one.
	 * (used a "create"  trigger on the sub requirement group relationship)
	 * 
	 * @param context the MatrixOne Context object
	 * @param args array of arguments 
	 *  - args[0]: from object id 
	 *  - args[1]: to object id 
	 * @return 0 if ok, 1 otherwise.
	 * @throws Exception if the navigation fails.
	 */
	public int checkConnectSubGroup(final Context context, final String[] args)
	throws Exception
	{
		if(args == null || args.length == 0)
			throw new IllegalArgumentException("invalid arguments in checkConnectSubGroup method: no ids passed. Contact administrator.");
		if(args[0] == null || args[0].length() == 0 || args[1] == null || args[1].length() == 0 )
			throw new IllegalArgumentException("invalid arguments in checkConnectSubGroup method: invalid ids. Contact administrator.");
		
		final String parentId = args[0]; 
		final String childId = args[1];

		return  ( DomainObject.multiLevelRecursionCheck(context, parentId, childId, RequirementsUtil.getSubRequirementGroupRelationship(context), false) ) ? 1 : 0; 
		//StringList parentStructureIds = getParentsAndChildrenIds(context, parentId);
		//if(parentStructureIds.contains(childId))
		//	return 1; //the child is already part of its future parent structure
		
		//StringList childStructureIds = getParentsAndChildrenIds(context, childId); 
		//if(childStructureIds.contains(parentId))
		//	return 1; //the parent item is already part of the child group structure.
		
		//NOTE: i don't check that the child object doesn't already have a parent item. This
		// is checked at "model" level by the relationship cardinality.
	}

	//Start:IR:123110V6R2014:LX6
	/**
	 * Returns a list containing all excluded ids for a sub requirement Group Attachement
	 * 
	 * This method is used to exlude all Requirement Group that not fit as sub Requirement Group
	 * 
	 * @param context MatrixOne context object
	 * @param args packed arguments.  Expecting at least the "objectId"  of a requirement group in the Program Map
	 * @return a StringList containing all exluded ids.
	 * @throws Exception if the navigation fails.
	 */
	@com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
	public StringList getExcludeListForAttachExistingGroup(final Context context,final String[] args)
	throws Exception
	{
		Map programMap = (HashMap)JPO.unpackArgs(args);
		String MQLResult = "";
		String objectId = (String)programMap.get("objectId");
		if(objectId == null || objectId.length() == 0)
			throw new IllegalArgumentException("getParentsAndChildrenIds method failure: invalid object id passed as parameter.");
		StringList ids= new StringList(getParentsAndChildrenIds(context, objectId )); 
		MQLCommand command = new MQLCommand();
		command.open(context);
		//Create the Query
		// Start IR-282618V6R2015x ZUD : HAT1 Removing sub Specification Folders from Exclude list of Full Search dialog.
		/*if(command.executeCommand(context, "temp query bus $1 $2 $3 where $4 select $5 dump $6 recordsep $7",
                                          RequirementsUtil.getRequirementGroupType(context),"*","*",
                                          "relationship[" + RequirementsUtil.getSubRequirementGroupRelationship(context) + "].from!=name", "id", "/","&"))
		{
			MQLResult = command.getResult();
			//get each objects
			String[] Elements = MQLResult.split("&");
			for(int i=0; i<Elements.length; i++)
			{
				String[] ObjDetails = Elements[i].split("/");
				if(ObjDetails.length >= 4)
					ids.add(ObjDetails[3]);
			}
		}
		else
		{
			MQLResult = command.getError();
			throw new Exception(MQLResult);
		}*/
		// End IR-282618V6R2015x ZUD : HAT1
		return  ids;
	}
	//End:IR:123110V6R2014:LX6
}
