/*
 **  DSCProductStructureEditorBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 */
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Policy;
import matrix.db.Relationship;
import matrix.db.RelationshipWithSelect;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFBaselineHelper;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleObjectExpanderWithView;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UITableIndented;
import com.matrixone.jdom.Element;
import com.matrixone.apps.domain.util.FrameworkProperties;
public class DSCProductStructureEditorBase_mxJPO
{
	protected MCADServerResourceBundle serverResourceBundle	= null;
	protected MCADMxUtil util								= null;
	protected String localeLanguage							= null;
	protected IEFGlobalCache cache							= null;
	protected MCADGlobalConfigObject gco                    = null;
	protected MCADLocalConfigObject lco                     = null;
	protected String relModificationStatus                  = "";
	protected String viewProgram                  			= MCADAppletServletProtocol.VIEW_AS_BUILT;  // [NDM] : QWJ
	protected String ATTR_QUANTITY							= "";
	private  HashSet addedInMap = null;

	public DSCProductStructureEditorBase_mxJPO(Context context,String[] argv)
	{		
		addedInMap  = new HashSet();
	}

	public int mxMain(Context context, String []argv)  throws Exception
	{  
		return 0;
	}
	@com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getExpandedBusObjects(Context context,String[] argv) throws Exception
	{			
		MapList relBusObjList = new MapList();
		boolean isViewAsSaved = false;
		try
		{ 			
			relBusObjList	  = getRelatedData (context, argv, null,isViewAsSaved);
			Iterator itr	  = relBusObjList.iterator();
			MapList tempList  = new MapList();
			while(itr.hasNext())
			{
				Map newMap = (Map)itr.next();
				newMap.put("selection", "multiple");
				tempList.add (newMap);
			}
			relBusObjList.clear();
			relBusObjList.addAll(tempList);
		}
		catch (Exception e)
		{
			System.out.println("\nDSCProductStructureEditor:getExpandedBusObjects :Error "+e.getMessage());	
		}
		return relBusObjList;
	}

	@com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getExpandedBusObjectsForAsSavedView(Context context,String[] argv) throws Exception
	{			
		MapList relBusObjList = new MapList();
		boolean isViewAsSaved = true;
		try
		{ 			
			relBusObjList	  = getRelatedData (context, argv, null, isViewAsSaved);
			Iterator itr	  = relBusObjList.iterator();
			MapList tempList  = new MapList();
			while(itr.hasNext())
			{
				Map newMap = (Map)itr.next();
				newMap.put("selection", "multiple");
				tempList.add (newMap);
			}
			relBusObjList.clear();
			relBusObjList.addAll(tempList);
		}
		catch (Exception e)
		{
			System.out.println("\nDSCProductStructureEditor:getExpandedBusObjects :Error "+e.getMessage());	
		}
		return relBusObjList;
	}
	
	@com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getBaselineStructure(Context context, String[] argv) throws Exception
	{			
		MapList relBusObjList				= new MapList();
		IEFBaselineHelper baselineHelper	= new IEFBaselineHelper(context, gco, serverResourceBundle, new IEFGlobalCache());
		boolean isViewAsSaved = false;
		try
		{
			HashMap programMap		= (HashMap)JPO.unpackArgs(argv);
			String baselineId		= (String)programMap.get("baselineId");

			String[] baselineIds	= new String[1];
			baselineIds[0]			= baselineId;

			HashMap baselineDetails		= baselineHelper.getBaselineConnectedObjectMap(context, baselineIds);
			Hashtable connectedObjMap	= (Hashtable)baselineDetails.get(baselineId);

			relBusObjList			= getRelatedData(context, argv, connectedObjMap,isViewAsSaved);

			Iterator itr			= relBusObjList.iterator();

			MapList tempList		= new MapList();
			while(itr.hasNext())
			{
				Map newMap = (Map)itr.next();
				newMap.put("selection", "multiple");
				tempList.add (newMap);
			}
			relBusObjList.clear();
			relBusObjList.addAll(tempList);
		}
		catch (Exception e)
		{
			System.out.println("DSCProductStructureEditor:getBaselineStructure :Error "+e.getMessage());	
		}

		return relBusObjList;
	}

	public MapList getRelatedData(Context context, String[] argv, Hashtable baselineConnectedObjects,boolean isViewAsSaved) throws Exception
	{
		MapList relBusObjList = new MapList();

		HashMap programMap		= (HashMap) JPO.unpackArgs(argv);		

		localeLanguage			= (String)programMap.get("LocaleLanguage");	
		serverResourceBundle	= new MCADServerResourceBundle(localeLanguage);
		cache					= new IEFGlobalCache();
		util					= new MCADMxUtil(context, serverResourceBundle, cache);
		String attrQuantityName	= MCADMxUtil.getActualNameForAEFData(context, "attribute_Quantity");

		ATTR_QUANTITY 			= "attribute[" + attrQuantityName + "]";

		String objectId			= (String)programMap.get("objectId");
		if(isViewAsSaved && util.isMajorObject(context, objectId))
		{
			objectId = util.getActiveVersionObject(context, objectId);		
		}	
		
		String expandLevel 		= (String)programMap.get("expandLevel");
        	int expandLevelVal = 0;
		BusinessObject busObj = new BusinessObject(objectId);

		busObj.open(context);
		
		if(expandLevel == null || expandLevel.equals(""))
		{
			expandLevel= (String)programMap.get("Expand Level");
		}

		if(!"All".equalsIgnoreCase(expandLevel) && expandLevel != null && !(expandLevel.equals("")))
			expandLevelVal = Integer.parseInt(expandLevel);

		MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject)programMap.get("GCO");		
		MCADServerGeneralUtil	serverGeneralUtil = new MCADServerGeneralUtil(context, globalConfigObject, serverResourceBundle, new IEFGlobalCache());

		Hashtable relClassMapTable = globalConfigObject.getRelationshipsOfClass(MCADAppletServletProtocol.ASSEMBLY_LIKE);

		Hashtable relclassTableClone = new Hashtable(relClassMapTable);

		if(!isShowCircularExternalReferences())
		{
			Hashtable externalReferenceLikeRelsAndEnds = globalConfigObject.getRelationshipsOfClass(MCADServerSettings.CIRCULAR_EXTERNAL_REFERENCE_LIKE);
			externalReferenceLikeRelsAndEnds.putAll(globalConfigObject.getRelationshipsOfClass(MCADServerSettings.EXTERNAL_REFERENCE_LIKE));

		Enumeration externalReferenceRels = externalReferenceLikeRelsAndEnds.keys();
		while (externalReferenceRels.hasMoreElements())
		{
			String relName = (String) externalReferenceRels.nextElement();

			if(relclassTableClone.containsKey(relName))
				relclassTableClone.remove(relName);
		}
		}
		else
		{
			relclassTableClone.putAll(globalConfigObject.getRelationshipsOfClass(MCADServerSettings.CIRCULAR_EXTERNAL_REFERENCE_LIKE));
			relclassTableClone.putAll(globalConfigObject.getRelationshipsOfClass(MCADServerSettings.EXTERNAL_REFERENCE_LIKE));
		}

		String relModificationStatusActualName	= MCADMxUtil.getActualNameForAEFData(context, "attribute_RelationshipModificationStatusinMatrix");
		relModificationStatus					= "attribute[" + relModificationStatusActualName + "]"; 

		try
		{	
			String [] oidList	= new String [1];
			oidList[0]			= objectId;

			//Preparing relationship and type pattern
			StringList busSelectList	= new StringList();
			busSelectList.add("type");
			busSelectList.add("name");

			StringList relSelects		= getRelSelectList(relclassTableClone);

			IEFSimpleObjectExpanderWithView  simpleObject	= new IEFSimpleObjectExpanderWithView(oidList, relclassTableClone, busSelectList, relSelects, (short)expandLevelVal,viewProgram,serverResourceBundle,globalConfigObject,cache);
			simpleObject.expandInputObjects(context, isShowCircularExternalReferences());

			ArrayList alreadyExpandedNodesInPath = new ArrayList();

			alreadyExpandedNodesInPath.add(objectId);


			relBusObjList = getObjectList(context, objectId,simpleObject, relclassTableClone, globalConfigObject, baselineConnectedObjects, alreadyExpandedNodesInPath,util,serverGeneralUtil);

			//To avoid JPO call for each expansion
			HashMap hmTemp = new HashMap();
			hmTemp.put("expandMultiLevelsJPO", "true"); 

			relBusObjList.add(hmTemp);
		}
		catch(Exception ex) 
		{
			System.out.println("\nDSCProductStructureEditor:getRelatedData : Error " + ex.getMessage());	
		}	
		return  relBusObjList;         
	}

	protected MapList getObjectList(Context context, String inputObjectID, IEFSimpleObjectExpanderWithView  simpleObjectExpander, Hashtable relClassMapTable, MCADGlobalConfigObject globalConfigObject, Hashtable baselineConnectedObjects, ArrayList alreadyExpandedNodesInPath,MCADMxUtil util,MCADServerGeneralUtil serverGeneralUtil) throws Exception
	{
		MapList relBusObjList 	  = new MapList();

		HashMap relIdChildBusidMap = simpleObjectExpander.getRelidChildBusIdList(inputObjectID);

		if(relIdChildBusidMap == null)
			return relBusObjList;

		Iterator relids = relIdChildBusidMap.keySet().iterator();
		String relid     = "";
		String level	 = "";
		String childID = "";
		String relName		= "";
		Map objectIsSuppressed = new HashMap(5);

		while(relids.hasNext())
		{
			 relid     = (String)relids.next();

			if(relid == null || relid.equals(""))
				continue;

			 childID = (String)relIdChildBusidMap.get(relid);

			RelationshipWithSelect relWithSelect = simpleObjectExpander.getRelationshipWithSelect(relid);
			BusinessObjectWithSelect busWithSelect = simpleObjectExpander.getBusinessObjectWithSelect(childID);

			if(busWithSelect == null)
			{
				StringList busSelectList	= new StringList();
				busSelectList.add("type");
				busSelectList.add("name");

				busWithSelect = BusinessObject.getSelectBusinessObjectData(context, new String[]{childID}, busSelectList).getElement(0);
			}

			 relName		= (String)relWithSelect.getSelectData("name");
			String quantityOnRel	= (String)relWithSelect.getSelectData(ATTR_QUANTITY);

			String childType	= (String)busWithSelect.getSelectData("type");
			String childName	= (String)busWithSelect.getSelectData("name");

			 level		= String.valueOf(alreadyExpandedNodesInPath.size());

			if((!isShowCircularExternalReferences() && globalConfigObject.isRelationshipOfClass(relName,MCADServerSettings.CIRCULAR_EXTERNAL_REFERENCE_LIKE)||(!validateRelationship(context,relid))))
				continue;

			if(baselineConnectedObjects != null)
			{
				StringBuffer tnBuffer = new StringBuffer();
				tnBuffer.append(childType);
				tnBuffer.append("|");
				tnBuffer.append(childName);

				String corrBaselinedChildId = (String)baselineConnectedObjects.get(tnBuffer.toString());

				if(corrBaselinedChildId == null || corrBaselinedChildId.equals(""))
				{
					childType = globalConfigObject.getCorrespondingType(childType);
					tnBuffer = new StringBuffer();
					tnBuffer.append(childType);
					tnBuffer.append("|");
					tnBuffer.append(childName);

					corrBaselinedChildId = (String)baselineConnectedObjects.get(tnBuffer.toString());
				}

				if(corrBaselinedChildId != null && !corrBaselinedChildId.equals(""))
				{
					childID = corrBaselinedChildId.substring(0, corrBaselinedChildId.indexOf("|"));
				}
			}

			String attrExcludeFromBOM		= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-ExcludeFromBOM");
			String attrMustInStructure  		= MCADMxUtil.getActualNameForAEFData(context, "attribute_MustInStructure");
			String relModificationStatusActualName	= MCADMxUtil.getActualNameForAEFData(context, "attribute_RelationshipModificationStatusinMatrix");				
			Relationship relationship			= new Relationship(relid);
			String excludeFromBOM				= relationship.getAttributeValues(context,attrExcludeFromBOM).getValue();
			String mustInStructure				= relationship.getAttributeValues(context,attrMustInStructure).getValue();
			String relMod = relationship.getAttributeValues(context,relModificationStatusActualName).getValue();				

				
			if(!globalConfigObject.isExpandedSubComponent())
			{
				if(mustInStructure.equalsIgnoreCase("false") && (!objectIsSuppressed.containsKey(childID) || "true".equalsIgnoreCase((String)objectIsSuppressed.get(childID))))
					objectIsSuppressed.put(childID , "true");
				else
					objectIsSuppressed.put(childID , "false");
					
				if(excludeFromBOM.equalsIgnoreCase("TRUE"))
				{

					if(!addedInMap.contains(childID))
					{
						/* IR-438688-3DEXPERIENCER2015x : Check the value of attribute_RelationshipModificationStatusInMatrix and if value is deleted then remove the entry of that object from relBusObjectList Map. */
						if(!relMod.equalsIgnoreCase("deleted"))
						{
							Map newMap	= new HashMap(5);
							newMap.put("level", level);
							newMap.put("id", childID);
							newMap.put("id[connection]", relid);
							newMap.put("relationship", relName);
							newMap.put("quantity", "0"); 
							relBusObjList.add(newMap);
			
							addedInMap.add(childID);
						}
					}
				}
				else
				{
				boolean isSameChild = addToResult( relBusObjList,  level,  childID,  relName);
				if(isSameChild)
					continue;
				}
			}

			if(!addedInMap.contains(childID))
			{
				if(null == quantityOnRel || "".equals(quantityOnRel.trim()))
				quantityOnRel = "1";

				/* IR-438688-3DEXPERIENCER2015x : Check the value of attribute_RelationshipModificationStatusInMatrix and if value is deleted then remove the entry of that object from relBusObjectList Map. */
				if(!relMod.equalsIgnoreCase("deleted"))
				{
					Map newMap	= new HashMap(5);
					newMap.put("level", level);
					newMap.put("id", childID);
					newMap.put("id[connection]", relid);
					newMap.put("relationship", relName);
					newMap.put("quantity", quantityOnRel); 

					if(mustInStructure.equalsIgnoreCase("false"))
						newMap.put("styleRows", "CellBackGroundColorDisabled");

					relBusObjList.add(newMap);
				}
				if(!globalConfigObject.isExpandedSubComponent())
				addedInMap.add(childID);
			}
			
			if(!alreadyExpandedNodesInPath.contains(childID))
			{
				alreadyExpandedNodesInPath.add(childID);
				MapList childBusMapList = getObjectList(context, childID, simpleObjectExpander, relClassMapTable, globalConfigObject, baselineConnectedObjects, alreadyExpandedNodesInPath,util,serverGeneralUtil);
				relBusObjList.addAll(childBusMapList);
				alreadyExpandedNodesInPath.remove(childID);
			}
		}

		greyOutNode(context,relBusObjList, objectIsSuppressed);
		return relBusObjList;
	}


	public Vector getQuantity(Context context, String [] args) throws Exception
	{
		Vector columnCellContentList = new Vector();

		HashMap paramMap				= (HashMap)JPO.unpackArgs(args);
		MapList relBusObjPageList		= (MapList)paramMap.get("objectList");

		for (int i = 0; i < relBusObjPageList.size(); i++)
		{
			Map objDetails  = (Map)relBusObjPageList.get(i);
			String quantity = (String)objDetails.get("quantity");
			columnCellContentList.add(quantity);
		}

		return columnCellContentList;
	}

	public Vector getFeatureIconForExcludeFromBOM(Context context, String [] args) throws Exception
	{
		Vector columnCellContentList = new Vector();
	try{
		HashMap paramMap				= (HashMap)JPO.unpackArgs(args);

		MapList relBusObjPageList		= (MapList)paramMap.get("objectList");
		
		HashMap paramList  = (HashMap) paramMap.get("paramList");
		String localeLanguage = (String)paramList.get("languageStr");

		MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle(localeLanguage);

		for (int i = 0; i < relBusObjPageList.size(); i++)
		{
			Map objDetails  = (Map)relBusObjPageList.get(i);
			String relId				= (String)objDetails.get("id[connection]");
			if(relId!=null && !relId.equals("")){
			String attrExcludeFromBOM		= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-ExcludeFromBOM");
			Relationship relationship			= new Relationship(relId);
			String excludeFromBOM = "";
			try
			{
				String alertMsg = serverResourceBundle.getString("mcadIntegration.Server.Message.ObjectExcludedFromBOM");
				excludeFromBOM		= relationship.getAttributeValues(context,attrExcludeFromBOM).getValue();
				StringBuffer htmlBuffer = new StringBuffer();
				htmlBuffer.append(getFeatureIconContent("Exclude", "iconAlertChanged.gif", alertMsg));
				
				if(excludeFromBOM.equals("TRUE"))
					columnCellContentList.add(htmlBuffer.toString());
				else
					columnCellContentList.add("");

			}
			catch(Exception e)
			{
				e.printStackTrace();
			System.out.println("error massage ===============" + e.getMessage());
			}
			} else {
			columnCellContentList.add("");
		}

		}

		}catch(Exception ex){
		ex.printStackTrace();
		}
		return columnCellContentList;
	}

	   	private String getFeatureIconContent(String href, String featureImage, String toolTop)
	{
		StringBuffer featureIconContent = new StringBuffer();
		featureIconContent.append("<a><img src=\"../iefdesigncenter/images/");
		featureIconContent.append(featureImage);
		featureIconContent.append("\" border=\"0\" title=\"");
		featureIconContent.append(toolTop);
		featureIconContent.append("\"/></a>");

		return featureIconContent.toString();
	}

	private boolean addToResult(MapList relBusObjList, String currentLevel, String id, String relName) 
	{
		boolean isChildAlreadyPresent = false;
		try{
		for(Iterator it = relBusObjList.iterator();it.hasNext();)
		{
			Map dataMap    = (Map)it.next();
			String level   = (String)dataMap.get("level");
			String childId = (String)dataMap.get("id");
			String relationShipName = (String)dataMap.get("relationship");
			
			String quantity = (String)dataMap.get("quantity");  
String decimalSym = FrameworkProperties.getProperty("emxFramework.DecimalSymbol");

		
			float quantValue = Float.parseFloat(quantity);

			if(level.equals(currentLevel) && childId.equals(id) && relationShipName.equals(relName))
                        {
				quantValue++;
				String quantityValue = quantValue + "";
				dataMap.put("quantity", quantityValue);
				isChildAlreadyPresent = true;
                                break;
                        }
		}
		}
catch(Exception e)
{
			
}		
		return isChildAlreadyPresent;
	}
	
	private void greyOutNode(Context context,MapList relBusObjList, Map objectIsSuppressed) throws Exception
	{
		boolean isChildAlreadyPresent = false;
		
		for(Iterator it = relBusObjList.iterator();it.hasNext();)
		{
			Map dataMap    = (Map)it.next();
			String childId = (String)dataMap.get("id");
			String quantity = (String)dataMap.get("quantity");  
String decimalSym = FrameworkProperties.getProperty("emxFramework.DecimalSymbol");
		if(decimalSym != ".")
                {	
		quantity = MCADMxUtil.localeDecimalConversion(context,quantity);
			}
			
			float quantValue = Float.parseFloat(quantity);

			if(quantValue == 0)
				dataMap.put("quantity", "");
			
			if("true".equalsIgnoreCase((String)objectIsSuppressed.get(childId)))
			   dataMap.put("styleRows", "CellBackGroundColorDisabled");
		}
	}
	
	protected StringList getRelSelectList(Hashtable relsAndEnds)
	{
		StringList relSelectionList = new StringList();

		relSelectionList.addElement("id");
		relSelectionList.addElement("name");
		relSelectionList.addElement(ATTR_QUANTITY);
		relSelectionList.addElement(relModificationStatus);

		return relSelectionList;
	}
              @com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getObjectsWithLatestVersions(Context context,String[] argv) throws Exception
	{	
		String viewProgram		= "MCADIntegGetLatestVersion";		
		MapList relBusObjList	= new MapList();

		try
		{
			relBusObjList = getLateralNavigation(context,argv,viewProgram);				
		}
		catch (Exception e)
		{
			System.out.println("DSCProductStructureEditor:getObjectsWithLatestVersions :Error "+e.getMessage());				
		}	

		return relBusObjList;
	}
                  @com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getObjectsWithLatestRevisions(Context context,String[] argv) throws Exception
	{
		String viewProgram		= "MCADIntegGetLatestRevision";		
		MapList relBusObjList	= new MapList();

		try
		{
			relBusObjList = getLateralNavigation(context,argv,viewProgram);				
		}
		catch (Exception e)
		{
			System.out.println("DSCProductStructureEditor:getObjectsWithLatestRevisions :Error "+e.getMessage());		
		}	

		return relBusObjList;
	}
                  
     public MapList getObjectsWithLatestInWorkRevision(Context context,String[] argv) throws Exception
              	{
              		String viewProgram		= "MCADIntegGetLatestInWorkRevision";		
              		MapList relBusObjList	= new MapList();

              		try
              		{
              			relBusObjList = getLateralNavigation(context,argv,viewProgram);				
              		}
              		catch (Exception e)
              		{
              			System.out.println("DSCProductStructureEditor:getObjectsWithLatestApprovedRevisions :Error "+e.getMessage());		
              		}	

              		return relBusObjList;
              	}
     public MapList getObjectsWithLatestFrozenRevision(Context context,String[] argv) throws Exception
   	{
   		String viewProgram		= "MCADIntegGetLatestFrozenRevision";		
   		MapList relBusObjList	= new MapList();

   		try
   		{
   			relBusObjList = getLateralNavigation(context,argv,viewProgram);				
   		}
   		catch (Exception e)
   		{
   			System.out.println("DSCProductStructureEditor:getObjectsWithLatestApprovedRevisions :Error "+e.getMessage());		
   		}	

   		return relBusObjList;
   	}
     
     public MapList getObjectsWithLatestReviewRevisions(Context context,String[] argv) throws Exception
    	{
    		String viewProgram		= "MCADIntegGetLatestReviewRevision";		
    		MapList relBusObjList	= new MapList();

    		try
    		{
    			relBusObjList = getLateralNavigation(context,argv,viewProgram);				
    		}
    		catch (Exception e)
    		{
    			System.out.println("DSCProductStructureEditor:getObjectsWithLatestApprovedRevisions :Error "+e.getMessage());		
    		}	

    		return relBusObjList;
    	}
     
               @com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getObjectsWithLatestReleasedRevisions(Context context,String[] argv) throws Exception
	{	
		String viewProgram		= "MCADIntegGetLatestReleasedRevision";  
		MapList relBusObjList	= new MapList();

		try
		{
			relBusObjList = getLateralNavigation(context,argv,viewProgram);						
		}
		catch (Exception e)
		{
			System.out.println("DSCProductStructureEditor:getObjectsWithLatestReleasedRevisions :Error "+e.getMessage());			
		}	

		return relBusObjList;
	}
@com.matrixone.apps.framework.ui.ProgramCallable
	protected MapList getLateralNavigation(Context context, String[] argv, String viewProgram) throws Exception
	{		
		MapList relBusObjList		= new MapList();

		try
		{
			//can't modify signature of getExpandedBusObjects
			this.viewProgram	= viewProgram;
			relBusObjList 		= getExpandedBusObjects(context,argv);
		}
		catch (Exception e)
		{
			System.out.println("DSCProductStructureEditor:getObjectsWithLatestReleasedRevisions :Error "+e.getMessage());				
		}	

		return relBusObjList;
	}


	public boolean checkAccess(Context context, String[] argv) throws Exception
	{
		boolean hasAccess = true;

		HashMap paramMap			= (HashMap)JPO.unpackArgs(argv);  	
		String objectId				= (String) paramMap.get("objectId");
		localeLanguage				= (String)paramMap.get("LocaleLanguage");	

		MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle(localeLanguage);
		IEFGlobalCache cache						    = new IEFGlobalCache();
		IEFIntegAccessUtil util					= new IEFIntegAccessUtil(context, serverResourceBundle, cache);	
		try
		{
			String integrationName = util.getIntegrationName(context, objectId);
			BusinessObject busObj = new BusinessObject(objectId);
			busObj.open(context);

			Policy policy = busObj.getPolicy(context);
			String sPolicyName = null;
			if(policy != null)
			{
				sPolicyName = policy.getName();
			}
			IEFSimpleConfigObject simpleGCO = null;

			if(util.getUnassignedIntegrations(context).contains(integrationName))
			{
				simpleGCO = IEFSimpleConfigObject.getSimpleGCOForUnassginedInteg(context, integrationName);
			}
			else
			{
				simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, integrationName);
			}

			String sReleaseStateName = null;
			if(simpleGCO != null)
			{
				String attrIEFReleasedState = MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-ReleasedState");
				Hashtable raleaseStateTable = simpleGCO.getAttributeAsHashtable(attrIEFReleasedState, "\n", "|");
				if(raleaseStateTable != null)
				{
					sReleaseStateName = (String)raleaseStateTable.get(sPolicyName);
				}
			}

			if(sReleaseStateName != null && !"".equalsIgnoreCase(sReleaseStateName))
			{
				String currentState = util.getCurrentState(context, objectId);
				Vector assignedRoles = PersonUtil.getUserRoles(context);				
				String actualName = (String)PropertyUtil.getSchemaProperty(context, "role_CADRevisionManager");

				if(currentState != null && !"".equalsIgnoreCase(currentState) && sReleaseStateName.equalsIgnoreCase(currentState))
				{
					if(!assignedRoles.contains(actualName))
						hasAccess = false;
				}
			}
			busObj.close(context);
		}

		catch (Exception e)
		{
			hasAccess = false;
		}	

		return hasAccess;
	}

	@com.matrixone.apps.framework.ui.ConnectionProgramCallable 
	public static HashMap editConnections(Context context, String[] argv) throws Exception
	{		
		HashMap connectionMap	= new HashMap();

		String relId			= "";
		String childId			= "";
		String markup			= "";
		String integrationName	= "";		
		String relName			= "";
		String relEnd           = "";
		String tobeRemovedChildId = "";
		String isRemove			= "";

		boolean isTo						= false;
		boolean doesAttrRelModStatusExist   = false;
		boolean doesAttrNewTransMatrixExist = false;

		HashMap programMap		= (HashMap) JPO.unpackArgs(argv);		
		String localeLanguage	= (String)programMap.get("LocaleLanguage");		

		MCADGlobalConfigObject globalConfigObject	= (MCADGlobalConfigObject)programMap.get("GCO");		
		MCADServerResourceBundle resourceBundle = new MCADServerResourceBundle(localeLanguage);
		MCADMxUtil util							= new MCADMxUtil(context,  new MCADServerResourceBundle(localeLanguage), new IEFGlobalCache());	

		String relModificationStatus	= MCADMxUtil.getActualNameForAEFData(context, "attribute_RelationshipModificationStatusinMatrix"); 
		String modifiedInMatrix			= MCADMxUtil.getActualNameForAEFData(context,"attribute_ModifiedinMatrix");

		String parentId	= (String) programMap.get("objectId");		
		integrationName = util.getIntegrationName(context, parentId);	

		IEFSimpleConfigObject simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, integrationName);

		String modifiedEventsActualName	= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-ModificationEvents");

		Vector modificationEvents = simpleGCO.getAttributeAsVector(modifiedEventsActualName, ",");

		Element elm				= (Element)programMap.get("contextData");
		MapList chgRowsMapList	= UITableIndented.getChangedRowsMapFromElement(context, elm);

		MapList newChgRowsMapList = new MapList();
		HashMap objDetailsMap		= new HashMap();
		try
		{			
			for (int i = 0; i < chgRowsMapList.size(); i++)
			{
				HashMap changedRowMap = (HashMap) chgRowsMapList.get(i);

				childId			= (String)changedRowMap.get("childObjectId");
				markup			= (String)changedRowMap.get("markup");
				relId			= (String)changedRowMap.get("relId");			
				String rowId	= (String)changedRowMap.get("rowId");
				String relType	= (String)changedRowMap.get("relType");		

				if( (relType == null) || ("".equalsIgnoreCase(relType)) || ("undefined".equalsIgnoreCase(relType)))
				{
					relType							= getRelationshipDetails(context, simpleGCO, util);
				}

				Enumeration relDetails = MCADUtil.getTokensFromString(relType,"|");

				if(relDetails.hasMoreElements())
				{
					relName = (String)relDetails.nextElement();					
					if(relDetails.hasMoreElements())
					{
						relEnd  = (String)relDetails.nextElement();
						if( (relEnd!=null) && !("".equals(relEnd)) && (relEnd.equalsIgnoreCase("to")))
						{
							isTo   = true;						
						}
						//For "Revision Replace"
						//find clean way to distinguish between "Replace" and "Revision Replace"
						//may be AEF can give seperate markup.
						if(relDetails.hasMoreElements())
						{
							isRemove  = (String)relDetails.nextElement();
							if( (isRemove!=null) && !("".equals(isRemove)) && (isRemove.equalsIgnoreCase("remove")))
							{
								isRemove   = "true";
								tobeRemovedChildId = (String)relDetails.nextElement();
							}
						}
					}
				}

				String[] incomingIds = new String[]{parentId, childId};
				HashMap idRelatedId = getRelatedIds(context, incomingIds);
				HashMap majorIdMinorIdMap = (HashMap)idRelatedId.get(parentId); //Major Id and Minor Ids for parent
				BusinessObject majorParentObjectBO  =  new BusinessObject((String)majorIdMinorIdMap.get("MajorId"));
				BusinessObject minorParentObjectBO  =  new BusinessObject((String)majorIdMinorIdMap.get("MinorId"));
				majorIdMinorIdMap = (HashMap)idRelatedId.get(childId);//Major Id and Minor Ids for child
				BusinessObject majorChildObjectBO   =  new BusinessObject((String)majorIdMinorIdMap.get("MajorId"));
				BusinessObject minorChildObjectBO   =  new BusinessObject((String)majorIdMinorIdMap.get("MinorId"));

				BusinessObject childBus		= new BusinessObject(childId);
				BusinessObject parentBus	= new BusinessObject(parentId);

				if((markup!=null) && !("".equalsIgnoreCase(markup)) && markup.equalsIgnoreCase("add") && !isRemove.equalsIgnoreCase("true"))
				{					
					relId = connectBO(context,majorParentObjectBO,majorChildObjectBO,relName,isTo,util,relModificationStatus);//Connect Major BusinessObject
					connectBO(context,minorParentObjectBO,minorChildObjectBO,relName,isTo,util,relModificationStatus);//Connect Minor BusinessObject
					objDetailsMap.put("oid", childId);
					objDetailsMap.put("rowId", rowId);
					objDetailsMap.put("pid", parentId);
					objDetailsMap.put("relid", relId);
					objDetailsMap.put("markup", markup);	
					newChgRowsMapList.add(objDetailsMap);

					if(!modificationEvents.isEmpty() && modificationEvents.contains(MCADAppletServletProtocol.UPDATESTAMP_EVENT_ADD_CHILD))
						util.modifyUpdateStamp(context, parentId);
				}
				//For "Revision Replace"
				else if((markup!=null) && !("".equalsIgnoreCase(markup)) && markup.equalsIgnoreCase("add"))
				{
					String sRelName = relName;
					String newRelId = "";
					boolean bRet = replaceRelationship(context, parentId, 
							tobeRemovedChildId, childId,
							simpleGCO, util, relName);

					if(bRet)
					{
						Vector relationsList = findRelationShip(context, util, childId, parentId, null, true, "1", sRelName);
						if( relationsList.isEmpty())
						{
							String msg = resourceBundle.getString("mcadIntegration.Server.Message.OnlySupportedForCADSubComponentLikeRelationships");
							MCADServerException.createException(msg, null);
						}
						newRelId = (String)relationsList.elementAt(0);
					}

					objDetailsMap.put("oid", childId);
					objDetailsMap.put("rowId", rowId);
					objDetailsMap.put("pid", parentId);
					objDetailsMap.put("relid", newRelId);
					objDetailsMap.put("markup", markup);	
					newChgRowsMapList.add(objDetailsMap);

					Relationship rel = new Relationship(newRelId);	
					rel.open(context);
					doesAttrRelModStatusExist   = doesRelAttributeExist(context,rel,relModificationStatus);	

					if(doesAttrRelModStatusExist)
					{
						util.setRelationshipAttributeValue(context,rel,relModificationStatus,"");
					}

					rel.update(context);
					rel.close(context);

					if(!modificationEvents.isEmpty() && 
							(modificationEvents.contains(MCADAppletServletProtocol.UPDATESTAMP_EVENT_ADD_CHILD) || modificationEvents.contains(MCADAppletServletProtocol.UPDATESTAMP_EVENT_DELETE_CHILD)))
						util.modifyUpdateStamp(context, parentId);
				}
				//For cut
				else if((markup!=null) && !("".equalsIgnoreCase(markup)) && (markup.equalsIgnoreCase("cut")))
				{					
					Relationship rel = new Relationship(relId);	
					rel.open(context);

					if(!rel.getTypeName().equals(MCADMxUtil.getActualNameForAEFData(context, "relationship_CADSubComponent")))
					{
						String msg = resourceBundle.getString("mcadIntegration.Server.Message.OnlySupportedForCADSubComponentLikeRelationships");
						MCADServerException.createException(msg, null);
					}

					doesAttrRelModStatusExist = doesRelAttributeExist(context, rel, relModificationStatus);	

					if(doesAttrRelModStatusExist)
					{
						util.setRelationshipAttributeValue(context, rel, relModificationStatus, "deleted");
					}

					rel.update(context);
					rel.close(context);

					objDetailsMap.put("oid", childId);
					objDetailsMap.put("relid", relId);
					objDetailsMap.put("rowId", rowId);
					objDetailsMap.put("markup", markup);
					newChgRowsMapList.add(objDetailsMap);

					if(!modificationEvents.isEmpty() && 
							modificationEvents.contains(MCADAppletServletProtocol.UPDATESTAMP_EVENT_DELETE_CHILD))
						util.modifyUpdateStamp(context, parentId);
				}
				//For resequence
				else if((markup!=null) && !("".equalsIgnoreCase(markup)) && (markup.equalsIgnoreCase("resequence")))
				{
					Relationship rel = new Relationship(relId);	
					rel.open(context);
					doesAttrRelModStatusExist = doesRelAttributeExist(context, rel, relModificationStatus);	

					if(doesAttrRelModStatusExist)
					{
						util.setRelationshipAttributeValue(context, rel, relModificationStatus, "");
					}

					rel.update(context);
					rel.close(context);

					objDetailsMap.put("oid", childId);
					objDetailsMap.put("relid", relId);
					objDetailsMap.put("rowId", rowId);
					objDetailsMap.put("markup", markup);
					newChgRowsMapList.add(objDetailsMap);
				}
				boolean isPushed    = false;
				try
				{           
					com.matrixone.apps.domain.util.ContextUtil.pushContext(context);
					isPushed    = true;                                   
					util.setAttributeValue(context, parentBus, modifiedInMatrix, "true");
					util.setAttributeValue(context, childBus, modifiedInMatrix, "true");	
				}
				catch(Exception ex)
				{
					MCADServerException.createException(ex.getMessage(), ex);
				}
				finally
				{
					if(isPushed)
					{ 
						try
						{
							com.matrixone.apps.domain.util.ContextUtil.popContext(context);
						}
						catch(Exception ex)
						{
							MCADServerException.createException(ex.getMessage(), ex);
						}
					}
				}

			}
			connectionMap.put("changedRows", newChgRowsMapList);
			connectionMap.put("Action", "refresh");

			IEFBaselineHelper baselineHeper = new IEFBaselineHelper(context, globalConfigObject, resourceBundle , new IEFGlobalCache());
			newChgRowsMapList = (MapList)connectionMap.get("changedRows");

			for (int i = 0; i < newChgRowsMapList.size(); i++)
			{				
				objDetailsMap = (HashMap) newChgRowsMapList.get(i);

				childId			= (String)objDetailsMap.get("oid");
				markup			= (String)objDetailsMap.get("markup");
				relId			= (String)objDetailsMap.get("relId");			
				String pid		= (String)objDetailsMap.get("pid");

				boolean isObjectConnectedToBaseline = baselineHeper.isBaselineRelationshipExistsForId(context, pid);

				if(pid != null && !"".equals( pid )  && isObjectConnectedToBaseline)
				{
					BusinessObject parentObjId	= new BusinessObject(pid);			
					parentObjId.open(context);

					Hashtable tokensTable = new Hashtable(4);
					tokensTable.put("TYPE",parentObjId.getTypeName() );
					tokensTable.put("NAME",parentObjId.getName() );
					tokensTable.put("REVISION",parentObjId.getRevision() );

					parentObjId.close(context);

					String msg = resourceBundle.getString("mcadIntegration.Server.Message.CannotEditBaselineStructure",tokensTable);
					MCADServerException.createException(msg, null);						
				}				
			}
		}
		catch (Exception e)
		{
			connectionMap.put("Action", "ERROR");
			connectionMap.put("Message",e.getMessage());
		}
		return connectionMap;
	}

	public static HashMap getRelatedIds(Context context,String[] busids) throws Exception
	{
		String IsVersionObject	  = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
		String SELECT_ON_MINOR  	  = new StringBuffer("from[").append(MCADMxUtil.getActualNameForAEFData(context,"relationship_VersionOf")).append("].to.id").toString();
		String SELECT_ON_MAJOR = new StringBuffer("from[").append(MCADMxUtil.getActualNameForAEFData(context,"relationship_ActiveVersion")).append("].to.id").toString();

		StringList busSelectionList = new StringList();		

		busSelectionList.addElement(DomainConstants.SELECT_ID);
		busSelectionList.addElement("attribute["+IsVersionObject+"].value");
		busSelectionList.addElement(SELECT_ON_MINOR);
		busSelectionList.addElement(SELECT_ON_MAJOR);
		
		HashMap idRelatedIds = new HashMap();
		
		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, busids, busSelectionList);

		for(int j = 0; j < buslWithSelectionList.size(); j++)
		{
			HashMap objectIDsMap 	= new HashMap();
			String  majorBusId 		= "";
			String  minorBusId 		= "";
			String  selectId 		= "";
			
			BusinessObjectWithSelect busObjectWithSelect = buslWithSelectionList.getElement(j);
			Boolean isMinorBus = Boolean.valueOf(busObjectWithSelect.getSelectData("attribute["+IsVersionObject+"].value"));

			if(isMinorBus)
			{
				 minorBusId		= busObjectWithSelect.getSelectData(DomainConstants.SELECT_ID);
				 selectId 		= minorBusId;
				 majorBusId 	= busObjectWithSelect.getSelectData(SELECT_ON_MINOR);
			}
			else
			{				
				 majorBusId		 =  busObjectWithSelect.getSelectData(DomainConstants.SELECT_ID);
				 minorBusId 	 =  busObjectWithSelect.getSelectData(SELECT_ON_MAJOR);
				 selectId 		 =  majorBusId;
			}
			
			objectIDsMap.put("MajorId", majorBusId);
			objectIDsMap.put("MinorId", minorBusId);
			idRelatedIds.put(selectId,objectIDsMap);

		}

		return idRelatedIds;
	}
	
	
	private static String connectBO(Context context,BusinessObject parentBO,BusinessObject childBO,String relName,boolean isTo,MCADMxUtil util,String relModificationStatus) throws Exception
	{
		String newTransformMatrix		= MCADMxUtil.getActualNameForAEFData(context, "attribute_NewTransformationMatrix"); 
		Relationship rel = util.ConnectBusinessObjects(context, parentBO, childBO, relName, isTo);						
		rel.open(context);
		String relId = rel.getName();
		
		boolean doesAttrRelModStatusExist   = doesRelAttributeExist(context, rel, relModificationStatus);						
		boolean doesAttrNewTransMatrixExist = doesRelAttributeExist(context, rel, newTransformMatrix);						

		if(doesAttrRelModStatusExist && doesAttrNewTransMatrixExist)
		{
			util.setRelationshipAttributeValue(context, rel, relModificationStatus, "new");			
			util.setRelationshipAttributeValue(context, rel, newTransformMatrix, "1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1");
		}

		rel.update(context);
		rel.close(context);				
		return relId;
	}
	
	

	protected static boolean doesRelAttributeExist(Context context, Relationship rel, String attrName)throws Exception
	{
		boolean doesRelAttrExist = false; 
		rel.open(context);
		AttributeList relAttrList = rel.getAttributes(context);
		AttributeItr attItr = new AttributeItr(relAttrList);

		while (attItr.next())
		{
			matrix.db.Attribute attribute = attItr.obj();
			String relAttrName = attribute.getName();
			if(relAttrName.equalsIgnoreCase(attrName))
			{
				doesRelAttrExist = true;
			}						
		}
		rel.close(context);
		return doesRelAttrExist;
	}


	protected static String getRelationshipDetails(Context context, IEFSimpleConfigObject simpleGCO, MCADMxUtil util)throws Exception
	{
		String relDetails           = "";
		String relName              = "";
		String relEnd               = "";
		String relClassMappingAttr	= MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-RelationShipClassMapping");  	      

		String relClassMappingAttrValue = simpleGCO.getConfigAttributeValue(relClassMappingAttr);		

		String token1 = "\n";
		String token2 = "|";
		String token3 = ",";

		java.util.StringTokenizer relClassDetailsTokens = new java.util.StringTokenizer(relClassMappingAttrValue,token1);
		String relClassDetails = "";
		while (relClassDetailsTokens.hasMoreTokens())
		{
			relClassDetails = relClassDetailsTokens.nextToken();
			java.util.StringTokenizer st1 = new java.util.StringTokenizer(relClassDetails,token2);

			if(st1.hasMoreTokens())
			{                
				String cadRelName = (st1.nextToken()).trim();				
				if(st1.hasMoreTokens())
				{
					String  relClasses  = (st1.nextToken()).trim();
					java.util.StringTokenizer st2         = new java.util.StringTokenizer(relClasses,token3);
					while(st2.hasMoreTokens())
					{
						String relClass = (st2.nextToken()).trim();		

						if( MCADServerSettings.CAD_SUBCOMPONENT_LIKE.equalsIgnoreCase(relClass))
						{
							relName = cadRelName;	
							break;
						}
					}
				}
			}
		}

		String cadToMxRelMapping		= MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-RelMapping");  
		String cadToMxRelMappingValue = simpleGCO.getConfigAttributeValue(cadToMxRelMapping);

		java.util.StringTokenizer cadToMxRelMapDetailsTokens = new java.util.StringTokenizer(cadToMxRelMappingValue,token1);
		String cadToMxRelMapDetails = "";
		while (cadToMxRelMapDetailsTokens.hasMoreTokens())
		{
			cadToMxRelMapDetails = cadToMxRelMapDetailsTokens.nextToken();
			java.util.StringTokenizer st1 = new java.util.StringTokenizer(cadToMxRelMapDetails,token2);           
			String cadRelName = st1.nextToken().trim();

			String mxRelDetails = (st1.nextToken()).trim();          
			java.util.StringTokenizer st2 = new java.util.StringTokenizer(mxRelDetails , ",");
			String mxRelEnd = (st2.nextToken()).trim();
			String mxRelName = (st2.nextToken()).trim();
			if(cadRelName.equalsIgnoreCase(relName))
			{
				relEnd  = mxRelEnd;
				relName = mxRelName;
				break;
			}
		}
		relDetails = relName + "|" + relEnd;	

		return relDetails;
	}
@com.matrixone.apps.framework.ui.PreProcessCallable
	public HashMap showAsStoredStructure(Context context, String []args) throws Exception
	{
		HashMap paramMap		= (HashMap)JPO.unpackArgs(args);
		HashMap returnMap		=  new HashMap();
		HashMap tableData		= (HashMap)paramMap.get("tableData");
		HashMap requestMap		= (HashMap)tableData.get("RequestMap");
		String languageStr		= (String)requestMap.get("languageStr");
		MapList objectList		= (MapList)tableData.get("ObjectList");
		String selectedProgram	= (String)requestMap.get("selectedProgram");

		String msg =  i18nNow.getI18nString("emxIEFDesignCenter.Common.CanNotShowEditMode","emxIEFDesignCenterStringResource",languageStr);	   

		if(!("DSCProductStructureEditor:getExpandedBusObjects".equals(selectedProgram)))
		{
			returnMap.put("Action","STOP");
			returnMap.put("Message",msg);
			returnMap.put("ObjectList",objectList);		
		}	
		return returnMap;
	}

	public static boolean replaceRelationship(Context context, String parentBusId, String fromBusId, String toBusId, IEFSimpleConfigObject simpleGCO, MCADMxUtil util, String relationshipName) throws Exception
	{		
		boolean bRet = true;
		String parentActiveMinorId = null;
		String sRelName = relationshipName;
		try
		{
			BusinessObject parentObj 	= new BusinessObject(parentBusId);
			boolean isParentFinalized = isBusObjectFinalized(context, parentObj, simpleGCO, util);

			if(isParentFinalized)
			{
				BusinessObject activeBus = util.getActiveMinor(context, parentObj);
				activeBus.open(context);
				parentActiveMinorId = activeBus.getObjectId(context);
				activeBus.close(context);
			}
			Vector relationsList = findRelationShip(context, util, fromBusId, parentBusId, parentActiveMinorId, true, "1", sRelName);
			Enumeration allRels = relationsList.elements();
			BusinessObject toChildObj 	= new BusinessObject(toBusId);

			while(allRels.hasMoreElements())
			{
				String relId = (String)allRels.nextElement();				
				Relationship rel = new Relationship(relId);
				rel.open(context);
				BusinessObject fromObj = rel.getFrom();
				AttributeList attrList = rel.getAttributes(context);
				// disconnect this rel
				rel.remove(context);

				rel.close(context);

				// Copy relatinship attributes on the newly created relationship
				Relationship newRelationship = util.ConnectBusinessObjects(context, toChildObj,fromObj,sRelName,false);
				if ( attrList != null )
				{
					newRelationship.setAttributes(context, attrList);
					newRelationship.update(context);
				}
			}
		}
		catch(Exception me)
		{
			bRet = false;
			String errMessage = me.getMessage();
			me.printStackTrace();
			throw new Exception(errMessage);
			// throw MCADException
			//String msg = "[MCADServerGeneralUtil.shuffleRelatinships]:" + errMessage;
			//log(msg, MCADLogger.ERROR);
			//MCADServerException.createException(errMessage, me);

		}
		return bRet;
	}

	/**
	 * @param currentId    The object id for which we are seeking relationships
	 * @param parentId     The object's parent 
	 * @param isTo         true for object's "to" relationship with parent.
	 *                     false for object's "from" relationship with parent.
	 * @param level         can be all or 1 or any , generally used all
	 * @param relationShip "CAD SubComponent"
	 * @return             List of the relationship that the current has with
	 *                     its parent. for occurences the list will have more that
	 *                     1 relationship with its parent.
	 */
	public static Vector findRelationShip(Context context, MCADMxUtil util, String currentId, String parentId, String parentActiveMinorId, boolean isTo, String level, String relationShip) throws Exception
	{
		Vector relIDs = new Vector();

		try
		{
			StringBuffer where = new StringBuffer("(");
			if(null != parentActiveMinorId  && !"".equalsIgnoreCase(parentActiveMinorId.trim()))
			{	
				where.append("(from.id == ");			
				where.append("\"" + parentActiveMinorId + "\" ) || ");
			}
			where.append("(from.id == ");
			where.append("\"" + parentId + "\" )");			
			where.append(")");
			String Args[] = new String[10];
			Args[0] = currentId;
			Args[1] = "recurse";
			Args[2] = level;
			Args[3]	= relationShip;
			Args[4]	= "terse";
			Args[5] = "id";
			Args[6] = where.toString();
			Args[7] = "|";
			Args[8] = "recordsep";
			Args[9] = "~";
			String result  = util.executeMQL(context, "expand bus $1 to $2 to $3 relationship $4 $5 select relationship $6 where $7 dump $8 $9 $10", Args);

			//Parse the command to get the result.
			boolean commandRes      = result.startsWith("true");
			if(commandRes)
			{
				StringBuffer buf    = new StringBuffer();
				String resStr       ="true";
				result              = result.substring(resStr.length()+1);

				StringTokenizer tokens = new StringTokenizer(result,"~");
				while(tokens.hasMoreTokens())
				{
					String item     = (String)tokens.nextElement();

					buf.append(item);

					int index       = item.lastIndexOf("|")+1;
					String relID    = buf.substring(index);

					buf.delete(index-1,buf.length());

					int secondIndex = buf.toString().lastIndexOf("|")+1;

					buf.delete(0,buf.length());

					if(!relIDs.contains(relID))
						relIDs.addElement(relID);
				}               
			}
			else
			{
				MCADServerException.createException(result, null);
			}
		}
		catch(Exception exception)
		{
			MCADServerException.createException(exception.getMessage(), exception);
			exception.printStackTrace();
		}

		return relIDs;
	}

	protected static boolean isBusObjectFinalized(Context context, BusinessObject busObject, IEFSimpleConfigObject simpleGCO, MCADMxUtil util) throws Exception
	{
		boolean bRet = false;		

		try
		{
			String busTypeAttrb		 	= MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-BusTypeMapping");
			Hashtable busTypeAttrbTable = simpleGCO.getAttributeAsHashtable(busTypeAttrb, "\n", "|");

			String sFinalizedRelName 	= MCADMxUtil.getActualNameForAEFData(context, "relationship_Finalized");

			String sVersionOfRelName 	= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
			String sFinalizedAttribName = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsFinalized");
			String sCADTypeAttribName	= MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType");

			String [] oids = new String[]{busObject.getObjectId(context)};

			StringList selectlist = new StringList(6);

			selectlist.add("type");
			selectlist.add("current");
			selectlist.add("policy");
			selectlist.add("state");
			selectlist.add("from[" + sFinalizedRelName + "].to.id");
			selectlist.add("from[" + sVersionOfRelName + "].attribute[" + sFinalizedAttribName + "]");
			selectlist.add("attribute[" + sCADTypeAttribName + "]");

			BusinessObjectWithSelect busWithSelect = BusinessObject.getSelectBusinessObjectData(context, oids, selectlist).getElement(0);

			boolean isMajorObject = false;
			if(busTypeAttrbTable != null)
			{
				String majorObjectTypes = (String)busTypeAttrbTable.get(busWithSelect.getSelectData("attribute[" + sCADTypeAttribName + "]"));
				if(null != majorObjectTypes && !"".equals(majorObjectTypes))
				{
					int index = majorObjectTypes.indexOf(busWithSelect.getSelectData("type")); 
					if(index != -1)
						isMajorObject = true;
				}
			}

			if (isMajorObject)
			{
				String finalizationState = "";

				String finalizationStateAttrb	 = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-FinalizationState");
				Hashtable finalizationStateTable = simpleGCO.getAttributeAsHashtable(finalizationStateAttrb, "\n", "|");
				if(finalizationStateTable != null)
				{
					finalizationState = (String)finalizationStateTable.get(busWithSelect.getSelectData("policy"));
				}

				String majorState			= busWithSelect.getSelectData("current");

				StringList majorStateList = busWithSelect.getSelectDataList("state");

				if(majorStateList.lastIndexOf(majorState) >= majorStateList.lastIndexOf(finalizationState))
				{
					bRet = true;
				}
			}
			else
			{
				String majorIdThroughFinalizedRelationship = busWithSelect.getSelectData("from[" + sFinalizedRelName + "].to.id");

				if(majorIdThroughFinalizedRelationship != null && !majorIdThroughFinalizedRelationship.equals(""))
				{
					// The object is in either finalized state or a higher state
					bRet = true;
				}
				else
				{
					String finalizedAttributeOnVersionOf = busWithSelect.getSelectData("from[" + sVersionOfRelName + "].attribute[" + sFinalizedAttribName + "]");

					if(finalizedAttributeOnVersionOf != null && finalizedAttributeOnVersionOf.equalsIgnoreCase("true"))
					{
						bRet = true;
					}
				}
			}
		}
		catch (Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

		return bRet;
	}

	/** 
	 * This method is provided for to customize the DSC Navigator page to show components based on some validation.
	 * This validation can be based on the relationship attribute.
	 * Method will return false if Must in Structure Attribute on Relation is false
	 * @param context  Context ENOVIA Context object reference
	 * @param relID String Relationship id	
	 */ 

	protected boolean validateRelationship(Context _context, String relid)
	{
		boolean isMustInStructure = true;
		return isMustInStructure;

	}

	/** 
	 * This method is provided for to customize the DSC Navigator page to show components based on some validation.
	 * This validation can be based on the relationship.
	 * 
	 * @return boolean 	If return value is false then Circular External References are not shown on Navigate page
	 * 				  	If return value is true then Circular External References will be shown on Navigate page,
	 *				  	While expanding structure expansion will stop at the Circular External reference object 
	 *				  	irrespecive of expand level.
	 */ 

	protected boolean isShowCircularExternalReferences()
	{
		boolean showCircularExternalReferences = false;
		return showCircularExternalReferences;		
	}
	
	
	public StringList getActiveInstanceStyle(Context context, String[] args)  throws Exception 
 	{

 		try 
 		{
 			StringList slStyles = new StringList();
 			// Get object list information from packed arguments
 			HashMap programMap = (HashMap) JPO.unpackArgs(args);
 			MapList objectList = (MapList) programMap.get("objectList");
			HashMap paramList  = (HashMap) programMap.get("paramList");
			String originalObjectId = (String)paramList.get("originalObjectId");
			String objectId = (String)paramList.get("objectId");
			MCADGlobalConfigObject gco = (MCADGlobalConfigObject)paramList.get("GCO");
			String languageStr = (String) paramList.get("languageStr");
			MCADServerResourceBundle resourceBundle = new MCADServerResourceBundle(languageStr);
			MCADServerGeneralUtil	serverGeneralUtil = new MCADServerGeneralUtil(context, gco, resourceBundle, new IEFGlobalCache());
			
			String objectIdForNavigation = null;
			String strActiveInstance	 = null;

			if(originalObjectId != null)
			{	
				strActiveInstance = serverGeneralUtil.getActiveInstanceId(context,originalObjectId);
			}

 			for (Iterator itrTableRows = objectList.iterator(); itrTableRows.hasNext();)
 			{
 				Map mapObjectInfo = (Map) itrTableRows.next();
				String level = (String) mapObjectInfo.get("level");
				if(level.equals("0"))
				{
					if(objectId.equals(strActiveInstance))
					{
						slStyles.addElement("RowBackGroundColor");
					}
					else
					{
						slStyles.addElement("");
					}
				}
				else
				{
					slStyles.addElement("");
				}
 			}
 			return slStyles;
 
 		} catch (Exception exp) {
 			exp.printStackTrace();
 			throw exp;
 		}
 	}

}

