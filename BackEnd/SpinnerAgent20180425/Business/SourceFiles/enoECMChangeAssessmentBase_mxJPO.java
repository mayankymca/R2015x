
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.dassault_systemes.enovia.enterprisechangemgt.admin.ECMAdmin;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeManagement;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeOrder;
import com.dassault_systemes.enovia.enterprisechangemgt.util.ChangeUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

public class enoECMChangeAssessmentBase_mxJPO extends DomainObject
{
	String ChildItems = "Child Items";
	String ParentItems = "Parent Items";
	String ChildAndRelated = "Child and Related Items";	
	String ParentAndRelated = "Parent and Related Items";
	String RelatedItem = "Related Item";
	public static final String RELATIONSHIP_CANDIDATE_AFFECTED_ITEM = PropertyUtil.getSchemaProperty("relationship_CandidateAffectedItem");
	public static final String VAULT = "eService Production";
	
	/**
     * Constructs a new ChangeAssessmentBase JPO object.
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     */

    public enoECMChangeAssessmentBase_mxJPO(Context context, String[] args) throws Exception
    {
        super();
    }
    
    /**
     * Getting the selected Item from the URL and displaying in the table and making the checkbox disabled
     * @param context
     * @param args
     * @return Maplist of selected Item with the disable selection true
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getselectedItem(Context context, String[] args)throws Exception
 	{
 		 MapList mlOutput = new MapList(10);
 		try
 		{
 			HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);
 			String strSelectedItems = (String) hmParamMap.get("selectedItems");
 			strSelectedItems = strSelectedItems.substring(1, (strSelectedItems.length()-1));
 			StringList slSelectedItems = FrameworkUtil.split(strSelectedItems, ",");
 			
 			for(int i = 0; i < slSelectedItems.size(); i++)
 			{
 				String strSelectIndiviItem = (String) slSelectedItems.get(i);
 				strSelectIndiviItem = strSelectIndiviItem.trim();
 				Map map = new HashMap();
 				map.put(DomainConstants.SELECT_ID,strSelectIndiviItem);
 				map.put("disableSelection","true");
 				mlOutput.add(map);
 			}
 		}
 		catch(Exception Ex)
 		{
 			Ex.printStackTrace();
 			throw Ex;
 		}
 		return mlOutput;
 	}
    
    /**
     * Getting the Child/ Parent from the selected item
     * @param context
     * @param args
     * @return MapList of Child/ Parent
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getChangeAssessmentItems(Context context, String[] args)throws Exception
    {
    	MapList mlOutput = new MapList(10);
    	try
    	{
    		HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);
    		String strRec = "";
			String[] strarr;
			String strRelName = "";
			String strDir = "";
			String strLabel = "";
			Map tempmap;
			String[] arrTableRowIds = new String[1];
			String strTableRowID = (String)hmParamMap.get("emxTableRowId");
			arrTableRowIds[0]=strTableRowID;
			ChangeUtil changeUtil = new ChangeUtil ();
			StringList slObjectIds = changeUtil.getAffectedItemsIds(context, arrTableRowIds);
    					
			mlOutput = new ChangeManagement().getChangeAssessment(context, slObjectIds);
    	}
 		catch(Exception Ex)
 		{
 			Ex.printStackTrace();
 			throw Ex;
 		}
 		return mlOutput;
 	}
    
    /**
     * Fetching the Custom label for grouping the values
     * @param context
     * @param args
     * @return Maplist of connected Items
     * @throws Exception
     */
    public Vector getCustomLabel(Context context, String[] args)throws Exception {
		Vector columnValues = new Vector();
		try {
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			MapList objList = (MapList) programMap.get("objectList");
			Hashtable map;
			Iterator itr = objList.iterator();
			while (itr.hasNext()) {
				map = (Hashtable) itr.next();
				columnValues.addElement(map.get("strLabel"));
			}
		} catch (Exception ex) {
			throw new Exception(ex.getMessage());
		}
		return columnValues;
    }
    
    /**
     * Expanding the Item according to the criteria passed
     * @param context
     * @param args
     * @return Maplist of connected Items
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getexpandItems(Context context, String[] args)throws Exception
    {
    	MapList mlOutput = new MapList(10);
    	MapList mlOutput1 = new MapList(10);
    	try
    	{
    		String strExpandRel = "";
    		String strRelatedRel = "";
    		StringList slRelatedList = new StringList();
    		StringList slExpandRel = new StringList();
    		HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);    		
    		String strObjectId = (String) hmParamMap.get("objectId");
    		String strChangeOrderId = (String) hmParamMap.get("contextCOId");
    		StringList slSelects = new StringList(DomainConstants.SELECT_TYPE);
    		slSelects.add(DomainConstants.SELECT_POLICY);
    		String RelatedFilter = (String) hmParamMap.get("ECMRelatedItemFilter"); 
    		setId(strObjectId);
    		Map map = getInfo(context, slSelects);
    		StringList objSelects   = new StringList(ChangeConstants.SELECT_ID);
    		StringList relSelects   = new StringList(ChangeConstants.SELECT_RELATIONSHIP_ID);
    		StringList slRegisteredTypes = (StringList)ECMAdmin.getRegisteredTypesActual(context);
    		String strRegisteredTypes = ChangeUtil.convertingListToString(context, slRegisteredTypes);
    		String strRelatedExp = ECMAdmin.getRelatedExpression(context, (String)map.get(DomainConstants.SELECT_TYPE), (String)map.get(DomainConstants.SELECT_POLICY));

    		if(RelatedFilter != null && "Related Item".equalsIgnoreCase(RelatedFilter))
    		{    			
    			slRelatedList = (StringList)getInfoList(context, strRelatedExp);
    			mlOutput = excludingConnectedCAAndAI(context, slRelatedList, strChangeOrderId, slRegisteredTypes);
    		}
    		else if("Child Items".equalsIgnoreCase(RelatedFilter))
    		{
    			slExpandRel = (StringList)ECMAdmin.getExpandRelActual(context, (String)map.get(DomainConstants.SELECT_TYPE), (String)map.get(DomainConstants.SELECT_POLICY));
    			strExpandRel = ChangeUtil.convertingListToString(context, slExpandRel);
    			mlOutput = getRelatedObjects(context, strExpandRel, strRegisteredTypes, objSelects, relSelects, false, true, (short)1, null, null, (short)0);
    		}
    		else if("Child and Related Items".equalsIgnoreCase(RelatedFilter))
    		{
    			System.out.println("inside");
    			slExpandRel = (StringList)ECMAdmin.getExpandRelActual(context, (String)map.get(DomainConstants.SELECT_TYPE), (String)map.get(DomainConstants.SELECT_POLICY));
    			strExpandRel = ChangeUtil.convertingListToString(context, slExpandRel);
    			strRelatedRel = strRelatedExp.substring(strRelatedExp.lastIndexOf("[")+1, strRelatedExp.indexOf("]"));  			
    			mlOutput = getRelatedObjects(context, strExpandRel + "," + strRelatedRel, strRegisteredTypes, objSelects, relSelects, false, true, (short)1, null, null, (short)0);
    		}
    		else if("Parent Items".equalsIgnoreCase(RelatedFilter))
    		{
    			slExpandRel = (StringList)ECMAdmin.getExpandRelActual(context, (String)map.get(DomainConstants.SELECT_TYPE), (String)map.get(DomainConstants.SELECT_POLICY));
    			strExpandRel = ChangeUtil.convertingListToString(context, slExpandRel);
    			mlOutput = getRelatedObjects(context, strExpandRel, strRegisteredTypes, objSelects, relSelects, true, false, (short)1, null, null, (short)0);
    		}
    		else if("Parent and Related Items".equalsIgnoreCase(RelatedFilter))
    		{
    			slExpandRel = (StringList)ECMAdmin.getExpandRelActual(context, (String)map.get(DomainConstants.SELECT_TYPE), (String)map.get(DomainConstants.SELECT_POLICY));
    			strExpandRel = ChangeUtil.convertingListToString(context, slExpandRel);
    			strRelatedRel = strRelatedExp.substring(strRelatedExp.lastIndexOf("[")+1, strRelatedExp.indexOf("]")); 
    			mlOutput = getRelatedObjects(context, strExpandRel, strRegisteredTypes, objSelects, relSelects, true, false, (short)1, null, null, (short)0);
    			mlOutput1 = getRelatedObjects(context, strRelatedRel, strRegisteredTypes, objSelects, relSelects, false, true, (short)1, null, null, (short)0);
    			mlOutput.addAll(mlOutput1);
    		}
    		
    		connectedObjectsDisable(context, strChangeOrderId, mlOutput);
    		
    	}
    	catch(Exception Ex)
    	{
    		Ex.printStackTrace();
    		throw Ex;
    	}
    	return mlOutput;
    }
    /**
     * Excluding the connected Candidate Item and Affected Item and converting into MapList
     * @param context
     * @param slRelatedList
     * @param strChangeOrderId
     * @param slRegisteredTypes
     * @return MapList
     * @throws Exception
     */
    public MapList excludingConnectedCAAndAI(Context context, StringList slRelatedList, String strChangeOrderId, StringList slRegisteredTypes)throws Exception
    {
    	MapList mlOutput = new MapList();
    	try
    	{
    		StringList slExclusionList = (StringList)getAffectedAndCandidateItems(context, strChangeOrderId);
    		
    		for(int i = 0 ; i < slRelatedList.size(); i++)
    		{
    			Map map = new HashMap();
    			String strRelatedItem = (String) slRelatedList.get(i);
    			String strRelatedType = new DomainObject(strRelatedItem).getInfo(context, DomainConstants.SELECT_TYPE);
    			if(slExclusionList != null && !slExclusionList.isEmpty() && !slExclusionList.contains(strRelatedItem))
    			{
        			String strParentType = MqlUtil.mqlCommand(context, "print type $1 select $2 dump",
                                                                          strRelatedType,"derived");
        			
        			if((slRegisteredTypes != null && !slRegisteredTypes.isEmpty()) || (slRegisteredTypes.contains(strRelatedType))|| slRegisteredTypes.contains(strParentType))
        			{
    				map.put(DomainConstants.SELECT_ID, strRelatedItem);
    				mlOutput.add(map);
        			} 
    			}    			
    		}
    	}
    	catch(Exception Ex)
    	{
    		Ex.printStackTrace();
    		throw Ex;
    	}
    	return mlOutput;
    }
    
    /**
     * Get Affected Items and Candidate Items connected to change Order
     * @param context
     * @param strChangeOrderId
     * @return StringList of Items connected
     * @throws Exception
     */
    public StringList getAffectedAndCandidateItems(Context context, String strChangeOrderId)throws Exception
    {
    	StringList slOutput = new StringList();
    	try
    	{
    		String strAffectedItemRelId = "";
    		StringList slAffectedItemId = new StringList();
    		slOutput = getInfoList(context, "from["+ RELATIONSHIP_CANDIDATE_AFFECTED_ITEM+"].to.id");
    		ChangeOrder changeOrder = new ChangeOrder(strChangeOrderId);
    		MapList mlAffectedItems = changeOrder.getAffectedItems(context);
    		for(int i = 0 ; i < mlAffectedItems.size(); i++)
    		{
    			Map map = (Map) mlAffectedItems.get(i);
    			strAffectedItemRelId = (String) map.get(SELECT_RELATIONSHIP_ID);
    			Hashtable htAffectedItems = new DomainRelationship(strAffectedItemRelId).getRelationshipData(context, new StringList("to.id"));
    			slAffectedItemId = (StringList) htAffectedItems.get("to.id");
    			if(!slOutput.contains(slAffectedItemId.get(0)))
    			{
    				slOutput.add(slAffectedItemId.get(0));
    			}
    		}
    	}
    	catch(Exception Ex)
    	{
    		Ex.printStackTrace();
    		throw Ex;
    	}

    	return slOutput;
    }
    
    
    /**
     * Regenerating the mapList by diabling the already connected Objects
     * @param context
     * @param strChangeOrderId
     * @param mlOutput
     * @return MapList
     * @throws Exception
     */
    public MapList connectedObjectsDisable(Context context,String strChangeOrderId,MapList mlOutput)throws Exception
    {
    	MapList mlFinalOutput = new MapList(); 
    	try
    	{
    		StringList slConnectedItemsList = getAffectedAndCandidateItems(context, strChangeOrderId);
    		for(int i = 0 ; i < mlOutput.size(); i++)
    		{
    			Map map = (Map) mlOutput.get(i);
    			String strObjectId = (String) map.get(DomainConstants.SELECT_ID);
    			if(strObjectId != null && !strObjectId.isEmpty() && slConnectedItemsList.contains(strObjectId))
    			{
    				map.put("disableSelection","true");    				
    			}
    			mlFinalOutput.add(map);
    		}
    	}
    	catch(Exception Ex)
    	{
    		Ex.printStackTrace();
    		throw Ex;
    	}
    	return mlFinalOutput;
    }
    
    /**
     *Filter option range Function for Related item
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getOptionForFilter(Context context, String[] args)throws Exception
    {
    	StringList slOutput = new StringList();
    	try
    	{
    		HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);
    		HashMap hmRequestMap = (HashMap) hmParamMap.get("requestMap");
    		String strToside = (String) hmRequestMap.get("ToSide");
    		
    		if(strToside != null && strToside.equals("true"))
    		{
    			slOutput.add(ChildItems);
    			slOutput.add(ChildAndRelated);        		
    		}
    		else if(strToside != null && strToside.equals("false"))
    		{
    			slOutput.add(ParentItems);
    			slOutput.add(ParentAndRelated);        		
    		}
    		else
    		{
    			slOutput.add(RelatedItem);
    		}
    	}
    	catch(Exception Ex)
    	{
    		Ex.printStackTrace();
    		throw Ex;
    	}
    	return slOutput;
    }
	/**
	 * To display all the Candiate Item on the table which are connected to the Candidate Object.
	 * @param context
	 * @param args Context Object ID (CO)
	 * @return MapList
	 * @throws Exception
	 */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getCandidateItems(Context context,String[] args)throws Exception
    {    	
    	MapList mlOutput = new MapList();
    	try
    	{
    		HashMap programMap = (HashMap)JPO.unpackArgs(args);
    		StringList objectSelects = new StringList(DomainConstants.SELECT_ID);
    		StringList relSelects = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
    		short recureLevel = 1;  		
    		
    		//getting parent object Id from args
    		String strCOId = (String)programMap.get("objectId");
    		this.setId(strCOId);
    		
    		mlOutput  = getRelatedObjects(context,
    				RELATIONSHIP_CANDIDATE_AFFECTED_ITEM,
    				"*",
    				objectSelects,
    				relSelects,
    				false,
    				true,
    				recureLevel,
    				"",
    				DomainConstants.EMPTY_STRING,
    				0);
    	}
    	catch(Exception Ex)
    	{
    		Ex.printStackTrace();
    		throw Ex;
    	}
    	return mlOutput;
    }
}
