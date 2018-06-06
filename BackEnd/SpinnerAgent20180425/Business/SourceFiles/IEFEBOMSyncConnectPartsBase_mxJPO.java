/*
 **  IEFEBOMSyncConnectPartsBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program for connecting Part objects with MCAD objects.
 */

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Collections;
import java.util.ResourceBundle;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.ExpandRelationship;
import matrix.db.ExpandRelationshipItr;
import matrix.db.ExpandRelationshipList;
import matrix.db.Expansion;
import matrix.db.JPO;
import matrix.db.Policy;
import matrix.db.QueryIterator;
import matrix.db.Relationship;
import matrix.db.RelationshipWithSelect;
import matrix.db.RelationshipWithSelectList;
import matrix.db.Visuals;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADStringUtils;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;

public class IEFEBOMSyncConnectPartsBase_mxJPO
{
	protected final String MATCH_CADMODEL_REV		= "MATCH_CADMODEL_REV";

	protected String parentNameAttr			= null;
	protected String relPartSpecification	= null;
	protected String relEBOM				= null;
	protected String attrSource				= null;
	protected String attrQuantity			= null;
	protected String attrFindNumber			= null;
	protected String attrExcludeFromBOM		= null;
	protected String relAssociateDrawing	= null;
	protected String attrCADType			= null;
	protected String REL_VERSION_OF			= "";
	protected String SELECT_ON_MAJOR		= "";

	protected String ATTR_QUANTITY				= "";
	protected String LOCAL_CONFIG_TYPE			= "";
	protected Vector associatedSpecificationInfoList	= null;
	protected String ATTR_COMPONENT_LOCATION = "";
	protected String ATTR_SPATIAL_LOCATION = "";

	//During EBOM synch, EBOM and Part Specification connection of revision of CAD Models other than current model revision are
	// are disconnected  if disconnectOldEBOMPartSpecRels is true. To preserver the connections with previous revisions also
	// set disconnectOldEBOMPartSpecRels = false.
	//protected boolean disconnectOldEBOMPartSpecRels	= true;  [NDM]

	MCADMxUtil					mxUtil					= null;
	MCADServerResourceBundle	serverResourceBundle	= null;
	IEFGlobalCache				cache					= null;
	public IEFEBOMConfigObject			ebomConfObject			= null;
	private MCADServerGeneralUtil serverGeneralUtil		= null;

	protected HashMap policySequenceMap = new HashMap();

	public IEFEBOMSyncConnectPartsBase_mxJPO()
	{
	}

	public IEFEBOMSyncConnectPartsBase_mxJPO(Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("not supported no desktop client", null);

	}

	public int mxMain(Context context, String[] args) throws Exception
	{
		return 0;
	}

	protected void init(Context context) throws Exception
	{
		parentNameAttr			= MCADMxUtil.getActualNameForAEFData(context, "attribute_CADObjectName");
		relPartSpecification	= MCADMxUtil.getActualNameForAEFData(context, "relationship_PartSpecification");
		relEBOM					= MCADMxUtil.getActualNameForAEFData(context, "relationship_EBOM");
		attrSource				= MCADMxUtil.getActualNameForAEFData(context, "attribute_Source");
		attrQuantity			= MCADMxUtil.getActualNameForAEFData(context, "attribute_Quantity");
		attrExcludeFromBOM		= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-ExcludeFromBOM");
		attrFindNumber			= MCADMxUtil.getActualNameForAEFData(context, "attribute_FindNumber");
		relAssociateDrawing		= MCADMxUtil.getActualNameForAEFData(context, "relationship_AssociatedDrawing");
		attrCADType				= MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType");
		ATTR_QUANTITY			= MCADMxUtil.getActualNameForAEFData(context, "attribute_Quantity");
		LOCAL_CONFIG_TYPE		= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-LocalConfig");
		REL_VERSION_OF			= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		SELECT_ON_MAJOR			= "from[" + REL_VERSION_OF + "].to.";
		ATTR_COMPONENT_LOCATION = MCADMxUtil.getActualNameForAEFData(context, "attribute_ComponentLocation");
		ATTR_SPATIAL_LOCATION	= MCADMxUtil.getActualNameForAEFData(context, "attribute_SpatialLocation");

		associatedSpecificationInfoList	= ebomConfObject.getAttributeAsVector(IEFEBOMConfigObject.ATTR_ASSOC_SPEC_INFO_LIST, "\n");
	}

	public Hashtable connectPartWithCADObject(Context context, String[] args) throws Exception
	{
		String[] packedGCO = new String[2];
		String partObjId		= args[0];
		String cadObjId			= args[1];
		String parentName		= args[2];
		boolean isMinorType		= "true".equalsIgnoreCase(args[3]);
		String ebomRegType		= args[4];
		String ebomRegName		= args[5];
		String ebomRegRev		= args[6];
		String language			= args[7];
		String sourceName		= args[8];
        packedGCO[0] 			= args[9];
        packedGCO[1] 			= args[10];
		String cadModelRevision = "";
		String cadModelName		= "";
		String cadModelType		= "";
		String actCadModelType	= "";
		String actCadModelRev	= "";

        MCADGlobalConfigObject globalConfigObject 	= (MCADGlobalConfigObject) JPO.unpackArgs(packedGCO);
		ebomConfObject			  = new IEFEBOMConfigObject(context, ebomRegType, ebomRegName, ebomRegRev);
		String matchingPartString = ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_PART_MATCHING_RULE);

		serverResourceBundle	= new MCADServerResourceBundle(language);
		cache					= new IEFGlobalCache();
		mxUtil					= new MCADMxUtil(context, serverResourceBundle, cache);
		serverGeneralUtil		= new MCADServerGeneralUtil(context,globalConfigObject, serverResourceBundle, cache);

		if(matchingPartString.equals(MATCH_CADMODEL_REV))
		{			
			matchRevisionSequence(context, cadObjId, partObjId, isMinorType);
			BusinessObject cadObject = new BusinessObject(cadObjId);

			cadObject.open(context);

			cadModelName	= cadObject.getName();
			actCadModelType	= cadObject.getTypeName();
			actCadModelRev	= cadObject.getRevision();

			/*if(isMinorType)  //[NDM]
			{
				BusinessObject majorObject = mxUtil.getMajorObject(context, cadObject);
				majorObject.open(context);
				cadModelRevision = majorObject.getRevision();
				cadModelType	 = majorObject.getTypeName();

				majorObject.close(context);
			}
			else*/
			{
				cadModelRevision = cadObject.getRevision();
				cadModelType	 = cadObject.getTypeName();
			}

			cadObject.close(context);
		}
		else
		{
			BusinessObject cadObject = new BusinessObject(cadObjId);

			cadObject.open(context);

			cadModelName	= cadObject.getName();
			actCadModelType	= cadObject.getTypeName();
			actCadModelRev	= cadObject.getRevision();

			cadObject.close(context);
		}

		init(context);

		Vector newlyCreatedPartSpecObjectNames = new Vector(8);

		BusinessObject newPartObj = new BusinessObject(partObjId);
		newPartObj.open(context);
		String partObjName = newPartObj.getName();
		newPartObj.close(context);

		Vector relatedDocsList = new Vector();
		/*if(isMinorType) //[NDM]
		{
			relatedDocsList = getRelatedDocuments(context, cadObjId, actCadModelType, cadModelName, actCadModelRev, isMinorType, new Vector(), new Vector());
		}
		else*/
		{
			relatedDocsList = getRelatedDocuments(context, cadObjId, isMinorType, new Vector());
		}
		//relatedDocsList contains cadid+drwaing+presentation 
		relatedDocsList.add(cadObjId);

		//String assignPartToMajor 				= ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_ASSIGN_PART_TO_MAJOR); {NDM]
		
		/* P.S. relationship break logic start */
		managePartSpecificationRelationship(context,cadObjId,partObjId/*[NDM], assignPartToMajor*/,globalConfigObject,partObjName,sourceName);
		
		String partObjIdForPartSpecCreation = null;

		for (int i = 0; i < relatedDocsList.size(); i++)
		{			
			String boId = (String) relatedDocsList.elementAt(i);

			/*if("true".equalsIgnoreCase(assignPartToMajor)) //[NDM]
			{				
				BusinessObject busObj = new BusinessObject(boId);
				busObj.open(context);

				BusinessObject majorObj = mxUtil.getMajorObject(context, busObj);			
				if(majorObj!= null)
				{
					majorObj.open(context);
					boId = majorObj.getObjectId();
					majorObj.close(context);
					isMinorType = false;
				}

				busObj.close(context);
			}*/

			boolean relationExists = mxUtil.doesRelationExist(context, boId, partObjId, relPartSpecification);
			if (!relationExists)
			{				
				BusinessObject relatedDoc = new BusinessObject(boId);
				String relatedDocName = "";
				if(boId.equals(cadObjId))
				{
					relatedDocName = parentName;
				}
				else
				{
					relatedDoc.open(context);
					relatedDocName = relatedDoc.getName();
					relatedDoc.close(context);
				}

				//Disconnect EBOM and Par Specification relations with CAD MODEL revisions other than current
				// revision. To preserve the connections init disconnectOldEBOMPartSpecRels variable to false
			//[NDM]	if(disconnectOldEBOMPartSpecRels == true)
				if(!isPartReleased(context,partObjId))
					{
						BusinessObjectList streamBOList = mxUtil.getRevisionBOsOfAllStreams(context, relatedDoc, isMinorType);					
					    disconnectEBOMRelation(context, streamBOList, partObjId, sourceName, boId);
					}// [NDM] End

				Hashtable attrNameValTable = new Hashtable(2);
				attrNameValTable.put(parentNameAttr, relatedDocName);

				if (partObjIdForPartSpecCreation == null)
				{
					// revise the released part with matching revision if the Matching rule is MATCH_CADMODEL_REV
					partObjIdForPartSpecCreation = getPartRevisionIfReleased(context, partObjId, cadModelRevision, cadModelType, matchingPartString);
					
					if(!partObjIdForPartSpecCreation.equals(partObjId))
					{
						copyAttribsFromCadObjToPart(context,partObjIdForPartSpecCreation,cadObjId,actCadModelType,"",globalConfigObject,language);
						relationExists = mxUtil.doesRelationExist(context, boId, partObjIdForPartSpecCreation, relPartSpecification);
					}
					else if(matchingPartString.equals(MATCH_CADMODEL_REV))
					{
						// input part is not released - > Check if the revision matches in this case						
						matchPartAndCADModelRevision(context, partObjIdForPartSpecCreation, cadModelRevision, cadModelName, cadModelType);
					}
				}
				else
					relationExists = mxUtil.doesRelationExist(context, boId, partObjIdForPartSpecCreation, relPartSpecification);

				if(!relationExists){
				
				serverGeneralUtil.grantAccessForUser(context, partObjIdForPartSpecCreation, context.getUser(), "modify,toConnect", "AccessParent");
				serverGeneralUtil.grantAccessForUser(context, boId, context.getUser(), "modify,fromConnect", "AccessChild");

				mxUtil.connectBusObjectsWithSameContext(context, partObjIdForPartSpecCreation,boId,relPartSpecification,true,attrNameValTable,null);
				serverGeneralUtil.revokeAccessForUser(context, partObjIdForPartSpecCreation, "AccessParent");				
				serverGeneralUtil.revokeAccessForUser(context, boId, "AccessChild");									
				}
				newlyCreatedPartSpecObjectNames.addElement(relatedDocName);
			}
		}

		Hashtable createdPartIdMessageDetails = new Hashtable(2);
		if(partObjIdForPartSpecCreation != null)
			createdPartIdMessageDetails.put(partObjIdForPartSpecCreation, newlyCreatedPartSpecObjectNames);
		else
			createdPartIdMessageDetails.put(partObjId, newlyCreatedPartSpecObjectNames);

		return createdPartIdMessageDetails;
	}

	private String getVersion(Context context/* {NDM],String confAttrAssignPartToMajor*/,BusinessObject cadObject) throws Exception
	{
		String	version = ""; 
		
	/*{NDM]	if("true".equalsIgnoreCase(confAttrAssignPartToMajor))
		{
			BusinessObject majorBusObject = mxUtil.getMajorObject(context, cadObject);
			if(majorBusObject == null)
			{
				majorBusObject = cadObject;
			}
		
			majorBusObject.open(context);
			version = majorBusObject.getRevision();
			majorBusObject.close(context);
		}
		else*/
		{
			cadObject.open(context);
			version = cadObject.getRevision();
			cadObject.close(context);
		}
		
		return version;
	}
	
	//manages Part Specification relationship if mode is single
	private void managePartSpecificationRelationship(Context context,String cadObjId,String partObjId/*[NDM],String assignPartToMajor*/,MCADGlobalConfigObject globalConfigObject,String partObjName,String sourceName) throws Exception
	{
		String ebomExpositionMode			 	= null;
		String cadType							= mxUtil.getAttributeForBO(context, cadObjId, attrCADType);
		BusinessObjectList relatedCadObjList 	= new BusinessObjectList();

		if(globalConfigObject.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
		{
			String famID 				= serverGeneralUtil.getTopLevelFamilyObjectForInstance(context, cadObjId);
			ebomExpositionMode 			= mxUtil.getAttributeForBO(context, famID, MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMExpositionMode"));
			
			String relatedPartId = getRelatedEBOMPart(context, famID);
			
			if(!MCADStringUtils.isNullOrEmpty(relatedPartId))
				disconnectPartSpecRelations(context, new BusinessObject(famID), new BusinessObject(relatedPartId));
						
			if("single".equalsIgnoreCase(ebomExpositionMode))
			{
				
			//	BusinessObject currentInstObj =  new BusinessObject(cadObjId); [NDM]
				//String currentVersion = getVersion(context,assignPartToMajor,currentInstObj);  [NDM]
				
				//get all instances for family
				ArrayList allInstanceList = serverGeneralUtil.getFamilyStructureRecursively(context, new String[]{famID}, new Hashtable(),null);
				Hashtable typeNameKeyIdTable = getTypeNameKeyIdTable(context,allInstanceList);
				allInstanceList.remove(cadObjId);
				
			//	DomainObject currentInst = DomainObject.newInstance(context, cadObjId); [NDM]
				//String currentObjType 	 = currentInst.getInfo(context, DomainObject.SELECT_TYPE); [NDM]
				
				//if(disconnectOldEBOMPartSpecRels == true)
				//	disconnecOldEBOMRelForSingleMode(context,currentInstObj,partObjName,sourceName,globalConfigObject,currentVersion); 
				
				
				//get specifications connected to part
				relatedCadObjList = mxUtil.getRelatedBusinessObjects(context,new BusinessObject(partObjId),relPartSpecification,"from");
				
				StringBuffer keyString = new StringBuffer();
				//check P.S needs to be moved to latest cad obj.
				BusinessObjectItr alreadySynchedInstObjListItr 	= new BusinessObjectItr(relatedCadObjList);
				while(alreadySynchedInstObjListItr.next())
				{
					BusinessObject instObjObject = alreadySynchedInstObjListItr.obj();
					keyString = new StringBuffer();
						
						//BusinessObject latestInstance; [NDM]
						//Old Stream Finalized and new in Preliminary
					/*	if(!"true".equals(assignPartToMajor) && globalConfigObject.isMajorType(instObjObject.getTypeName())&& !globalConfigObject.isMajorType(currentObjType))
						{
							String minorType = globalConfigObject.getCorrespondingType(instObjObject.getTypeName());
						keyString.append(minorType);
						keyString.append("|");
						keyString.append(instObjObject.getName());
						}
						else*/ // [NDM] End
					{
						keyString.append(instObjObject.getTypeName());
						keyString.append("|");
						keyString.append(instObjObject.getName());
					}
							
					if(typeNameKeyIdTable.containsKey(keyString.toString()))
					{
						String latestInstId = (String)typeNameKeyIdTable.get(keyString.toString());
						//latestInstance = new BusinessObject(latestInstId); [NDM]
				
						String latestVersion = getVersion(context/* [NDM],assignPartToMajor */ ,new BusinessObject(latestInstId));
						String oldVersion 	 = getVersion(context/* [NDM],assignPartToMajor*/ ,instObjObject);
						
						if(!latestVersion.equals(oldVersion))
						{
							//disconnect old P.S.
							//disconnecOldEBOMRelForSingleMode(context,instObjObject,partObjName,sourceName,globalConfigObject,latestVersion);
							disconnectPartSpecRelations(context,instObjObject,new BusinessObject(partObjId));
								
							boolean  relationExists 	= mxUtil.doesRelationExist(context, /*latestInstance.getObjectId(context) [NDM]*/latestInstId, partObjId, relPartSpecification);
							if(!relationExists && !isPartReleased(context, partObjId))
								{
									Hashtable attrNameValTable = new Hashtable();
									attrNameValTable.put(parentNameAttr, instObjObject.getName());
									serverGeneralUtil.grantAccessForUser(context, partObjId, context.getUser(), "modify,toConnect", "AccessParent");
				serverGeneralUtil.grantAccessForUser(context, latestInstId, context.getUser(), "modify,fromConnect", "AccessChild");											
				mxUtil.connectBusObjectsWithSameContext(context, partObjId,latestInstId,relPartSpecification,true,attrNameValTable,null);
				serverGeneralUtil.revokeAccessForUser(context, partObjId, "AccessParent");				
				serverGeneralUtil.revokeAccessForUser(context, latestInstId, "AccessChild");	
								}
						}
						
					}
					else
					{
							//not present in new structure.disconnect it
						//disconnecOldEBOMRelForSingleMode(context,instObjObject,partObjName,sourceName,globalConfigObject,"");
						String specificaionsCadType			= mxUtil.getAttributeForBO(context, instObjObject, attrCADType);
						if(globalConfigObject.isTypeOfClass(specificaionsCadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
							disconnectPartSpecRelations(context,instObjObject,new BusinessObject(partObjId));
						
					}
				}
			}
					}
					
				}
	
	protected String getRelatedEBOMPart(Context context, String busId)
	{       				
		String partId = "";
		try
		{	
			StringBuffer whereClause = new StringBuffer();
			whereClause.append("((name == const\"");
			whereClause.append(relPartSpecification);
			whereClause.append("\")");
			whereClause.append(" && ");
			whereClause.append("(to.id == \"");
			whereClause.append(busId);
			whereClause.append("\"))");

			String Args[] = new String[5];
			Args[0] = busId;
			Args[1] = "terse";
			Args[2] = "id";
			Args[3] = whereClause.toString();
			Args[4] = "|";
			String retVal = mxUtil.executeMQL(context, "expand bus $1 $2 select rel $3 where $4 dump $5", Args); 
			
			if (retVal.startsWith("true"))
			{
				StringTokenizer strTok = new StringTokenizer(retVal.substring(5), "|");
				if(strTok.hasMoreTokens())
				{
					String level = strTok.nextToken();
					String relName = strTok.nextToken();
					String direction = strTok.nextToken();

					partId = strTok.nextToken();
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("[IEFEBOMSyncConnectParts : getRelatedEBOMPart] : Exception occurred " + e.getMessage());
		}

		return partId;
	}
	
	private Hashtable getTypeNameKeyIdTable(Context context,ArrayList objList) throws MatrixException 
	{
		Hashtable typeNameKeyIdTable = new Hashtable();
		StringList busSelectList = new StringList();
		busSelectList.add("id");
		busSelectList.add("type");
		busSelectList.add("name");
		
		//To be done
	/*{NDM]	busSelectList.add(SELECT_ON_MAJOR + "id");
		busSelectList.add(SELECT_ON_MAJOR + "type");
		busSelectList.add(SELECT_ON_MAJOR + "name");*/
		
		String [] oids = new String[objList.size()];
		objList.toArray(oids);
		
		BusinessObjectWithSelectList busWithSelectList = BusinessObjectWithSelect.getSelectBusinessObjectData(context, oids, busSelectList);
		StringBuffer keyString = null;
		
		for(int i = 0; i < busWithSelectList.size();i++)
		{
			BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(i);

			keyString = new StringBuffer();
			keyString.append(busWithSelect.getSelectData("type"));
			keyString.append("|");
			keyString.append(busWithSelect.getSelectData("name"));
			typeNameKeyIdTable.put(keyString.toString(),busWithSelect.getSelectData("id"));
			
			/*[NDM]keyString = new StringBuffer();
			keyString.append(busWithSelect.getSelectData(SELECT_ON_MAJOR + "type"));
			keyString.append("|");
			keyString.append(busWithSelect.getSelectData(SELECT_ON_MAJOR + "name"));
			typeNameKeyIdTable.put(keyString.toString(),busWithSelect.getSelectData(SELECT_ON_MAJOR + "id"));*/
		}
		
		return typeNameKeyIdTable;
	}
	
	String isReplaceCase(Context context, String childPartId, String cadRelId, Map oldEBOMRelId) {
		String retVal = "";
		try {

			if (oldEBOMRelId == null || oldEBOMRelId.isEmpty()) {
				return retVal;
			}
			DomainRelationship cadRel = DomainRelationship.newInstance(context, cadRelId);
			String uuidCadValue = cadRel.getAttributeValue(context, "Relationship UUID");
			Set s = oldEBOMRelId.keySet();

			if (s.contains(uuidCadValue)) {
				String oldRelId = (String) oldEBOMRelId.get(uuidCadValue);
				// DomainRelationship oldRel =
				// DomainRelationship.newInstance(context, oldRelId);
				StringList sel = new StringList();
				sel.add("to.id");
				MapList ml = DomainRelationship.getInfo(context, new String[] { oldRelId }, sel);
				Map m = (Map) ml.get(0);
				String oldId = (String) m.get("to.id");
				if (!childPartId.equals(oldId)) {
					// replace
					DomainRelationship.setToObject(context, oldRelId, new DomainObject(childPartId));
				}
				retVal = oldRelId; // if the same part Exist return true to same
									// relation

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		return retVal;
	}

	public boolean updateEBOMAttribute(Context context, Relationship ebomRel, String cadrelId, String sourceName, IEFEBOMConfigObject ebomConfObj,
			String findNumber, String updateFindNumber) {
		Hashtable cadAttrTable;
		try {
			cadAttrTable = getRelAttibutesFromCADRelForEBOM(context, cadrelId, sourceName, ebomConfObj);

			if (findNumber != null && findNumber.length() > 0 && updateFindNumber != null && updateFindNumber.equals("true")) {
				cadAttrTable.put(attrFindNumber, findNumber);
			}

			mxUtil.setRelationshipAttributeValue(context, ebomRel, cadAttrTable);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	


	private Map<String, String> convertListToMap(MapList input, String keyOfOutPut, String attributeName) {
		if (input == null) {
			return new HashMap();
		}
		int ii = input.size();
		Map m = (Map) input.get(0);
		if (m == null || m.isEmpty()) {
			return new HashMap();
		}
		Map<String, String> retVal = new HashMap<String, String>();

		for (int i = 0; i < input.size(); i++) {
			Map map = (Map) input.get(i);
			String keyElement = (String) map.get(keyOfOutPut);
			if(!keyElement.isEmpty()){
			String[] listOfKeys = keyElement.split("");

			String attributeValue = (String) map.get(attributeName);
			String[] listOfattributeValue = attributeValue.split("");
			for (int j = 0; j < listOfattributeValue.length&&j < listOfKeys.length; j++) {
				retVal.put(listOfKeys[j], listOfattributeValue[j]);

			}
			}

		}

		return retVal;
	}
	
	public String connectPartWithPart(Context context, String[] args) throws Exception
	{
				
		String retval = "true";
	try{
		Vector argumentsVector		= (Vector)JPO.unpackArgs(args);
		String parentPartId			= (String)argumentsVector.elementAt(0);
		String childPartId			= (String)argumentsVector.elementAt(1);
		Vector cadRelIdVector		= (Vector)argumentsVector.elementAt(2);
		String ebomRegType			= (String)argumentsVector.elementAt(3);
		String ebomRegName			= (String)argumentsVector.elementAt(4);
		String ebomRegRev			= (String)argumentsVector.elementAt(5);
		String language				= (String)argumentsVector.elementAt(6);
		String sourceName			= (String)argumentsVector.elementAt(7);
		String findNumber		= (String)argumentsVector.elementAt(8);
		String updateFindNumber		= (String)argumentsVector.elementAt(9);
		String isParentFantom		= (String)argumentsVector.elementAt(10);
		String parentCadObjID		= (String)argumentsVector.elementAt(11);
		String childObjId			= (String)argumentsVector.elementAt(12);
		MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject)argumentsVector.elementAt(13);
		Map allCAdUUID = (Map) argumentsVector.elementAt(14); // all new UUID
		Map allOldParentUUID = (Map) argumentsVector.elementAt(15);// all old
																	// UUID
																	// part

		ebomConfObject			= new IEFEBOMConfigObject(context, ebomRegType, ebomRegName, ebomRegRev);
		serverResourceBundle	= new MCADServerResourceBundle(language);
		cache					= new IEFGlobalCache();
		mxUtil					= new MCADMxUtil(context, serverResourceBundle, cache);
		serverGeneralUtil    						= new MCADServerGeneralUtil(context,globalConfigObject, serverResourceBundle, cache);

		init(context);

		Vector relatedObjectsList				= getRelatedObjectsCount(context, parentPartId, childPartId);

		Vector cadRelIDVectorForInstances = new Vector();
		
		String cadrelId							= (String)cadRelIdVector.elementAt(0);
		float relatedObjectsCount				= ((Float)relatedObjectsList.lastElement()).floatValue();
		relatedObjectsList.removeElementAt(relatedObjectsList.size()-1);

		float cadSubComponentRelationShipCount 	= 0;
		String ebomExpositionMode			 	= null;
		String cadType				= mxUtil.getAttributeForBO(context, childObjId, attrCADType);
		if(!"".equals(parentCadObjID) && globalConfigObject.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
		{
			String famID 				= serverGeneralUtil.getTopLevelFamilyObjectForInstance(context, childObjId);
			ebomExpositionMode 			= mxUtil.getAttributeForBO(context, famID, MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMExpositionMode"));
			
			if(null != ebomExpositionMode && "single".equalsIgnoreCase(ebomExpositionMode))
			{
				ArrayList allInstanceList 			= serverGeneralUtil.getFamilyStructureRecursively(context, new String[]{famID}, new Hashtable(),null);
				Hashtable relsAndEnds 	 =  globalConfigObject.getRelationshipsOfClass(MCADAppletServletProtocol.ASSEMBLY_LIKE);
				Hashtable siblingObjects = serverGeneralUtil.getFirstLevelChildAndRelIds(context, parentCadObjID, true, relsAndEnds, null);

				cadSubComponentRelationShipCount	= singleModeCadSubComponentRelationShipCount(context, allInstanceList,siblingObjects, cadRelIDVectorForInstances);
			}
			else
				cadSubComponentRelationShipCount	= getcadSubComponentRelationShipCount(context, cadRelIdVector);
		}
		else
			cadSubComponentRelationShipCount	= getcadSubComponentRelationShipCount(context, cadRelIdVector);
		
		boolean isCollapsedEBOM = "TRUE".equalsIgnoreCase(ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_GENERATE_COLLAPSED_EBOM));
		
		//If parentPartId is of fantom node then struture calculation needs to be done. 
		if("true".equals(isParentFantom) && (relatedObjectsCount < cadSubComponentRelationShipCount))
		{
			relatedObjectsCount = 0;
		}
		
		if(ebomExpositionMode != null && "single".equalsIgnoreCase(ebomExpositionMode))
			cadRelIdVector = cadRelIDVectorForInstances;

		String stableEBOMRelEnabled 			 = isStableEBOMRelEnabled();


		if(isCollapsedEBOM)
		{		
			
			if(relatedObjectsList.size()==0 && cadSubComponentRelationShipCount !=0)
			{
				cadrelId				= (String)cadRelIdVector.elementAt(0);
				Relationship cadRelObj		= new Relationship(cadrelId);
				cadRelObj.open(context);
				String cadRelType	= cadRelObj.getRelationshipType().getName();
				Hashtable cadAttrTable = getAttributeMap(context, ebomConfObject, cadRelObj, cadRelType, relEBOM, sourceName);
				cadAttrTable.put(attrSource, sourceName);
				cadAttrTable.put(attrQuantity, String.valueOf(cadSubComponentRelationShipCount));

				//This is to support CROSS-REF HL
				if(cadAttrTable.containsKey(ATTR_COMPONENT_LOCATION)) 
				{
					StringBuffer attributeValue = getSpatialAttribute(context, cadRelIdVector);
					cadAttrTable.remove(ATTR_COMPONENT_LOCATION);
					cadAttrTable.put(ATTR_COMPONENT_LOCATION, attributeValue.toString());

				}
				cadRelObj.close(context);
				Hashtable relsAndEnds = new Hashtable();
				relsAndEnds.put(relEBOM,"to");
				if(findNumber != null && findNumber.length() >0 && updateFindNumber != null && updateFindNumber.equals("true"))
				{
					cadAttrTable.put(attrFindNumber,findNumber);
				}
				//TODO
				serverGeneralUtil.grantAccessForUser(context, parentPartId, context.getUser(), "modify,toConnect", "AccessParent");
				serverGeneralUtil.grantAccessForUser(context, childPartId, context.getUser(), "modify,fromConnect", "AccessChild");										
				mxUtil.connectBusObjectsWithSameContext(context, parentPartId,childPartId,relEBOM,true,cadAttrTable,null);
				serverGeneralUtil.revokeAccessForUser(context, parentPartId, "AccessParent");									
				serverGeneralUtil.revokeAccessForUser(context, childPartId, "AccessChild");

			}
			else
			{
				String removePart = "false";
				for(int i = 0; i < relatedObjectsList.size(); i++)
				{
					String relId				= (String)relatedObjectsList.elementAt(i);
					Relationship relationship	=  new Relationship(relId);
					
					/* New logic of Qty */
					Attribute qtyAttribute 		=  relationship.getAttributeValues(context, ATTR_QUANTITY);
					if(qtyAttribute != null)
					{
						String qty = qtyAttribute.getValue();
						//for single mode qty is calculated in singleModeCadSubComponentRelationShipCount for all inst in current level
						if(qty != null && !"".equals(qty) && null != ebomExpositionMode && !"single".equalsIgnoreCase(ebomExpositionMode))
						{
							float existinQty  = Float.valueOf(qty).floatValue();
							cadSubComponentRelationShipCount = cadSubComponentRelationShipCount + existinQty;
						}
					}
					
					//EBOM already done, now cadsubcomponet has exclude true  
					if(cadSubComponentRelationShipCount == 0)
					{
						relationship.remove(context);
						removePart = "true";
					}
					else
					{
						Hashtable cadAttribTable	= getRelAttibutesFromCADRelForEBOM(context, cadrelId, sourceName, ebomConfObject);
	
						if(cadAttribTable != null && cadAttribTable.size() > 0)
						{
							cadAttribTable.put(attrQuantity, String.valueOf(cadSubComponentRelationShipCount));
							cadAttribTable.remove(attrSource);				

							//This is to support CROSS-REF HL
							if(cadAttribTable.containsKey(ATTR_COMPONENT_LOCATION))
							{
								StringBuffer attributeValue = getSpatialAttribute(context, cadRelIdVector);

								cadAttribTable.remove(ATTR_COMPONENT_LOCATION);
								cadAttribTable.put(ATTR_COMPONENT_LOCATION, attributeValue.toString());
							}

							if(findNumber != null && findNumber.length() >0 && updateFindNumber != null && updateFindNumber.equals("true"))
							{
								cadAttribTable.put(attrFindNumber,findNumber);
							}
							
							serverGeneralUtil.grantAccessForUser(context, parentPartId, context.getUser(), "modify", "modifyAccess");						
							mxUtil.setRelationshipAttributeValue(context, relationship, cadAttribTable);
							serverGeneralUtil.revokeAccessForUser(context, parentPartId, "modifyAccess");
							
						}
						else
						{						
							serverGeneralUtil.grantAccessForUser(context, parentPartId, context.getUser(), "modify", "modifyAccess");						
							mxUtil.setRelationshipAttributeValue(context, relationship, ATTR_QUANTITY, String.valueOf(cadSubComponentRelationShipCount));
							serverGeneralUtil.revokeAccessForUser(context, parentPartId, "modifyAccess");
						}
					}
					/* New logic of Qty */
				}
				
				retval = removePart;
			}
		}
		else if(relatedObjectsCount > cadSubComponentRelationShipCount&&!"true".equals(stableEBOMRelEnabled))
		{
			// Now we need to remove the ExtraEbomRelationShips
			Float fdifference	= new Float(relatedObjectsCount - cadSubComponentRelationShipCount);
			int difference		= fdifference.intValue();
			for(int i = 0; i < difference; i++)
			{
				String relId				= (String)relatedObjectsList.elementAt(i);
				Relationship relationship	= new Relationship(relId);
				relationship.remove(context);
				// DeleteRelationShips relationShip.remove(context);
			}
		}
		else
		{
			if("true".equals(stableEBOMRelEnabled)){
				boolean createdNewEBOM=false;
			for (int i = 0; i < cadRelIdVector.size(); i++) {
				String cadRelId = (String) cadRelIdVector.elementAt(i);				
				Hashtable cadAttrTable = getRelAttibutesFromCADRelForEBOM(context, cadRelId, sourceName, ebomConfObject);
				if (findNumber != null && findNumber.length() > 0 && updateFindNumber != null && updateFindNumber.equals("true")) {
					cadAttrTable.put(attrFindNumber, findNumber);
				}
				String ebomRelId = isReplaceCase(context, childPartId, cadRelId, allOldParentUUID);
				if (!ebomRelId.isEmpty()) { // replace
					Relationship ebomRel = new Relationship(ebomRelId);
					updateEBOMAttribute(context, ebomRel, cadRelId, sourceName, ebomConfObject, findNumber, updateFindNumber);
					
					} else { // !Replace
						mxUtil.connectBusObjects(context, parentPartId, childPartId, relEBOM, true, cadAttrTable);
						createdNewEBOM = true;
				}
			}// END FOR
			if(!createdNewEBOM){
				retval = "false";
			}

			StringList selec = new StringList();
			selec.add("attribute[Relationship UUID]");
			selec.add("id");
			relatedObjectsList = getRelatedObjectsCount(context, parentPartId, childPartId);
			relatedObjectsList.removeElementAt(relatedObjectsList.size() - 1);
			if (relatedObjectsList.size() > cadRelIdVector.size()) {
			    MapList mlBOM = DomainRelationship.getInfo(context, (String[]) relatedObjectsList.toArray(new String[relatedObjectsList.size()]), selec);
				Map<String, String> mapBOM = convertListToMap(mlBOM, "attribute[Relationship UUID]", "id");
				List<String> relIdsToDelete = new ArrayList<String>();
				for(String s:mapBOM.keySet()){
					if (!allCAdUUID.containsKey(s)) {
						relIdsToDelete.add(mapBOM.get(s));
					}	
				}
				
				String[] toDelete = new String[relIdsToDelete.size()];
				relIdsToDelete.toArray(toDelete);
				DomainRelationship.disconnect(context, toDelete);

			}


			}else{
			Float fdifference	= new Float(cadSubComponentRelationShipCount - relatedObjectsCount);
			int difference		= fdifference.intValue();
			if(difference == 0 && relatedObjectsCount > 0)
			{
				for (int j = 0; j < relatedObjectsList.size(); j++ )
				{
					String relationshipId  = (String)relatedObjectsList.elementAt(j);
					Relationship ebomRel   = new Relationship(relationshipId);
						String relId		   = (String)cadRelIdVector.elementAt(j);
					Hashtable cadAttrTable = getRelAttibutesFromCADRelForEBOM(context, relId,sourceName,ebomConfObject);

					if(findNumber != null && findNumber.length() >0 && updateFindNumber != null && updateFindNumber.equals("true"))
					{
						cadAttrTable.put(attrFindNumber,findNumber);
					}
					
					serverGeneralUtil.grantAccessForUser(context, parentPartId, context.getUser(), "modify", "modifyAccess");
					mxUtil.setRelationshipAttributeValue(context, ebomRel, cadAttrTable);
					serverGeneralUtil.revokeAccessForUser(context, parentPartId, "modifyAccess");
				}
				
				retval = "false";
			}
			else
			{

					for(int i = 0; i < difference; i++)
					{
						String relId = (String)cadRelIdVector.elementAt(i);
						Hashtable cadAttrTable = getRelAttibutesFromCADRelForEBOM(context, relId,sourceName,ebomConfObject);
						if(findNumber != null && findNumber.length() >0 && updateFindNumber != null && updateFindNumber.equals("true"))
						{
							cadAttrTable.put(attrFindNumber,findNumber);
						}
						//TODO
				serverGeneralUtil.grantAccessForUser(context, parentPartId, context.getUser(), "modify,toConnect", "AccessParent");
				serverGeneralUtil.grantAccessForUser(context, childPartId, context.getUser(), "modify,fromConnect", "AccessChild");									
				mxUtil.connectBusObjectsWithSameContext(context, parentPartId,childPartId,relEBOM,true,cadAttrTable,null);
				serverGeneralUtil.revokeAccessForUser(context, parentPartId, "AccessParent");				
				serverGeneralUtil.revokeAccessForUser(context, childPartId, "AccessChild");					
					}
					}
				}
						}
		  
		  }catch(Exception ex){
			  System.out.println("ConnectPartwithPart---Error--->");
			  ex.printStackTrace();
		  }
		return retval;
	}

	//This is to support CROSS-REF HL
	private StringBuffer getSpatialAttribute(Context context, Vector cadRelIdVector) throws MatrixException 
	{
		StringBuffer attributeValue = new StringBuffer();

		StringList busSelectList = new StringList(2);

		busSelectList.addElement("attribute[" + ATTR_SPATIAL_LOCATION + "]");

		String ids[] = new String[cadRelIdVector.size()];
		cadRelIdVector.toArray(ids);

		RelationshipWithSelectList relationshipWithSelectList = Relationship.getSelectRelationshipData(context, ids, busSelectList);

		for(int i = 0; i < relationshipWithSelectList.size();i++)
		{
			RelationshipWithSelect relWithSelect = (RelationshipWithSelect) relationshipWithSelectList.elementAt(i);

			String spatialLocation = relWithSelect.getSelectData("attribute[" + ATTR_SPATIAL_LOCATION + "]");

			if(!MCADStringUtils.isNullOrEmpty(spatialLocation))
			{
				attributeValue.append(spatialLocation);

				if((relationshipWithSelectList.size()-1) != i)
					attributeValue.append("|");
			}
		}
		return attributeValue;
	}

	protected Hashtable getRelAttibutesFromCADRelForEBOM(Context context, String relId, String sourceName, IEFEBOMConfigObject ebomConfObject) throws Exception
	{
		Relationship cadRelObj		= new Relationship(relId);
		cadRelObj.open(context);
		String cadRelType	= cadRelObj.getRelationshipType().getName();
		Hashtable cadAttrTable = getAttributeMap(context, ebomConfObject, cadRelObj, cadRelType, relEBOM, sourceName);

		cadAttrTable.put(attrSource, sourceName);

		cadRelObj.close(context);

		return cadAttrTable;
	}

	protected Hashtable getAttributeMap(Context context, IEFEBOMConfigObject ebomConfObj, Relationship relObject,
			String cadRel, String partRel, String sourceName) throws Exception
			{		
		Hashtable attrValueHash = new Hashtable();
		Hashtable mandAttrNameHash = ebomConfObj.getMandRelationAttributeMapping(cadRel, partRel);

		Enumeration mandAttrNameHashEnum = mandAttrNameHash.keys();
		while(mandAttrNameHashEnum.hasMoreElements())
		{
			try
			{
				String cadAttrName	= (String) mandAttrNameHashEnum.nextElement();
				String partAttrName = (String) mandAttrNameHash.get(cadAttrName);
				String attrValue	= relObject.getAttributeValues(context, cadAttrName).getValue();

				attrValueHash.put(partAttrName, attrValue);
			}
			catch(Exception e)
			{
				MCADServerException.createException(e.getMessage(), e);
			}
		}

		//Retrieving Value from LocalConfig Object
		String userName		= context.getUser();
		String prefColElement = "";

		Hashtable localhash = new Hashtable();

		String localConfigObjectRevision = MCADMxUtil.getConfigObjectRevision(context);
		BusinessObject localObj		     = new BusinessObject(LOCAL_CONFIG_TYPE, userName, localConfigObjectRevision,"");
		Attribute prefObjectValue	= localObj.getAttributeValues(context, IEFEBOMConfigObject.ATTR_REL_ATTR_MAPPING);		
		String prefObjectMapping	= prefObjectValue.getValue();

		StringTokenizer prefObjectMappingToken		= new StringTokenizer(prefObjectMapping, "\n");

		while (prefObjectMappingToken.hasMoreElements())
		{
			prefColElement = (String)prefObjectMappingToken.nextElement();
			int firstIndex = prefColElement.indexOf("|");
			String integName =	prefColElement.substring(0,firstIndex);
			String prefValue =  prefColElement.substring(firstIndex+1,prefColElement.length());
			if(integName.equals(sourceName))
			{
				if(prefValue!=null && !prefValue.trim().equals(""))
				{
					Enumeration objectStringValue1 = MCADUtil.getTokensFromString(prefValue.trim(), "@");
					while (objectStringValue1.hasMoreElements())
					{
						String obj1 = (String)objectStringValue1.nextElement();
						localhash.put(obj1, obj1);
					}
				}
			}
		}
		//Formatting Value for EBOMSYNC	
		String cadTypeName		= "";
		String cadAttrName		= "";
		String partTypeName		= "";
		String partAttrName		= "";
		Enumeration localEnum	= localhash.keys();
		while(localEnum.hasMoreElements())
		{
			String name		= (String)localEnum.nextElement();				
			String value	= (String)localhash.get(name);

			StringTokenizer token = new StringTokenizer(value, "|");
			cadTypeName  = (String)token.nextElement();
			cadAttrName  = (String)token.nextElement();
			partTypeName = (String)token.nextElement();
			partAttrName = (String)token.nextElement();

			
			StringList attriList = new StringList();
			attriList.add(cadAttrName);

			AttributeList attributes = relObject.getAttributeValues(context, attriList,true);
			String attrValue		 =((Attribute)attributes.get(0)).getValue();

			attrValueHash.put(partAttrName,attrValue);
		}		
		return attrValueHash;
			}

	// Function that checks if the given relationship exists between the two given objects
	// If the relationship is found,it  reads the Quantity attribute
	// On the relationship and increments it by one and updates the value.
	// Returns TRUE if relationship exists, FALSE otherwise
/* {NDM]	protected boolean updateRelationship(Context context, String sRelName, String fromSideObjId, String toSideObjId) throws MatrixException, MCADException
	{
		boolean bRelExists = false;

		BusinessObject fromSideBO = new BusinessObject(fromSideObjId);

		// Open from side BOf
		//fromSideBO.open(_context);
		short level = 1;
		Visuals vis = new Visuals();

		//build the relationship where clause to be used in expand on bus object
		StringBuffer relationshipWhereClause = new StringBuffer();
		relationshipWhereClause.append("(name == const\"");
		relationshipWhereClause.append(sRelName);
		relationshipWhereClause.append("\"");
		relationshipWhereClause.append(")");

		//call the expand method on the business object
		Expansion expansion = mxUtil.expandBusObject(context, fromSideBO, level, "",relationshipWhereClause.toString(),(short)0, false, vis);

		ExpandRelationshipList filteredRelationshipList = expansion.getRelationships();
		ExpandRelationshipItr expandItr = new ExpandRelationshipItr(filteredRelationshipList);

		BusinessObject childObject = null;

		while (expandItr.next())
		{
			ExpandRelationship expandRel = expandItr.obj();

			// get the to side object and add it to ret list
			childObject = expandRel.getTo();

			if (childObject.getObjectId().equals(toSideObjId))
			{
				// Modify the relationship
				String qtyAttrVal = (expandRel.getAttributeValues(context,ATTR_QUANTITY)).getValue();
				float qty = 0;
				try
				{
					java.text.NumberFormat nf	= java.text.NumberFormat.getInstance(serverResourceBundle.getLocale());
					Number quantityNumber		= nf.parse(qtyAttrVal);
					float fQuantity				= quantityNumber.floatValue();
					qty							= fQuantity;
				}
				catch(Exception e)
				{
					qty = 0;
				}
				qty = qty + 1;
				String updatedQtyAttrVal = String.valueOf(qty);
				mxUtil.setRelationshipAttributeValue(context, expandRel, ATTR_QUANTITY, updatedQtyAttrVal);

				bRelExists = true;
				break;
			}
		}
		return bRelExists;
	}*/

	protected Vector getRelatedDocuments(Context context, String parentObjId, boolean isMinorType, Vector retListNew) throws Exception
	{
		Vector retList = retListNew;

		try
		{
			StringBuffer relations = new StringBuffer();

			// look at _associatedSpecificationInfoList
			for (int i = 0; i < associatedSpecificationInfoList.size(); i++)
			{
				String details = (String) associatedSpecificationInfoList.elementAt(i);
				// get the rel name, dirn, and type info
				// get rel name direction & type relevent on other end
				//debug = debug + "Details :" + details;
				StringTokenizer tokenizer = new StringTokenizer(details, ",");
				String relName = "", end = "", releventType = "";
				if (tokenizer.hasMoreTokens())
					relName = tokenizer.nextToken();
				if (tokenizer.hasMoreTokens())
					end = tokenizer.nextToken();
				if (tokenizer.hasMoreTokens())
					releventType = tokenizer.nextToken();

				/*if(isMinorType)   //[NDM]
				{
					String typeExists = mxUtil.executeMQL(context ,"list type \"" + releventType + "\"");
					if(typeExists.startsWith("true|") && typeExists.length() > 5)
					{
						String correspondingType = mxUtil.getCorrespondingType(context, releventType);
						if(correspondingType != null && !"".equals(correspondingType))
							releventType = correspondingType;
					}
				}*/

				// get relevant rels
				if (end.equals("from"))
				{
					relations.append("((name == const\"");
					relations.append(relName);
					relations.append("\")");
					relations.append(" && ");
					relations.append("(from.id == \"");
					relations.append(parentObjId);
					relations.append("\")");
					relations.append(" && ");
					relations.append("(to.type == const\"");
					relations.append(releventType);
					relations.append("\"))");
					relations.append(" || ");
				}
				else
				{
					relations.append("((name == const\"");
					relations.append(relName);
					relations.append("\")");
					relations.append(" && ");
					relations.append("(to.id == \"");
					relations.append(parentObjId);
					relations.append("\")");
					relations.append(" && ");
					relations.append("(from.type == const\"");
					relations.append(releventType);
					relations.append("\"))");
					relations.append(" || ");
				}
			}
			if (relations.length() > 0)
			{
				int index = relations.toString().lastIndexOf("|") - 2;
				relations.delete(index, relations.length());

				String Args[] = new String[5];
				Args[0] = parentObjId;
				Args[1] = "terse";
				Args[2] = "id";
				Args[3] = relations.toString();
				Args[4] = "|";

				String retVal = mxUtil.executeMQL(context,"expand bus $1 $2 select rel $3 where $4 dump $5", Args);
				if (retVal.startsWith("true"))
				{
					StringTokenizer strTok = new StringTokenizer(retVal.substring(5), "\n");
					while (strTok.hasMoreTokens())
					{
						String row = strTok.nextToken();
						StringTokenizer rowElements = new StringTokenizer(row, "|");
						String level = rowElements.nextToken();
						String relName = rowElements.nextToken();
						String direction = rowElements.nextToken();
						String connectedObjId = rowElements.nextToken();
						
						if(!retList.contains(connectedObjId))
						retList.add(connectedObjId);

						BusinessObject relatedDocObject	= new BusinessObject(connectedObjId);
						relatedDocObject.open(context);
						String cadType = mxUtil.getCADTypeForBO(context, relatedDocObject);
						relatedDocObject.close(context);
						if ("presentation".equalsIgnoreCase(cadType) || "drawing".equalsIgnoreCase(cadType))
						{
							getRelatedDocuments(context, connectedObjId, isMinorType, retList);
						}
					}
				}
			}
		}
		catch (Exception _ex)
		{
			MCADServerException.createManagedException(MCADAppletServletProtocol.DEFAULT_ERROR_CODE, serverResourceBundle.getString("mcadIntegration.Server.Message.RelatedDocumentError"), _ex);
		}
		return retList;
	}

	// getRelatedDocuments returns all latest version of drawing and presentation[Associated_Drawing] for cadObjectId
	/*protected Vector getRelatedDocuments(Context context, String parentObjId, String parentObjType, String parentObjName, String parentObjRevision, boolean isMinorType, Vector retListNew, Vector alreadyTraversedListNew) throws Exception
	{
		Vector retList					= retListNew;
		Vector alreadyTraversedList	= alreadyTraversedListNew;
		Hashtable objectIdTypeNameMap	= new Hashtable();
		Hashtable typeNameObjectIdMap	= new Hashtable();
		Hashtable typeNameObjectRevMap	= new Hashtable();
		QueryIterator resultItr=null;

		try
		{
			String typePattern		= parentObjType;
			String namePattern		= parentObjName;
			String revPattern		= "*";
			StringList busSelects	= new StringList(10);
			String selectOnDrwRelFrom   = "from[" + relAssociateDrawing + "].to.";
			String selectOnDrwRelTo     = "to[" + relAssociateDrawing + "].from.";

			busSelects.add("id");
			busSelects.add("type");
			busSelects.add("name");
			busSelects.add("revision");
			busSelects.add("revisions");
			busSelects.add(selectOnDrwRelFrom + "id");
			busSelects.add(selectOnDrwRelFrom + "type");
			busSelects.add(selectOnDrwRelFrom + "name");
			busSelects.add(selectOnDrwRelFrom + "revision");
			busSelects.add(selectOnDrwRelFrom + "revisions");
			busSelects.add(selectOnDrwRelFrom + "attribute[" + attrCADType + "]");
			busSelects.add(selectOnDrwRelTo + "id");
			busSelects.add(selectOnDrwRelTo + "type");
			busSelects.add(selectOnDrwRelTo + "name");
			busSelects.add(selectOnDrwRelTo + "revision");
			busSelects.add(selectOnDrwRelTo + "revisions");
			busSelects.add(selectOnDrwRelTo + "attribute[" + attrCADType + "]");

			resultItr = mxUtil.executeQuery(context, typePattern, namePattern, revPattern, busSelects);

			while(resultItr.hasNext())
			{
				BusinessObjectWithSelect busWithSelect = resultItr.next();

				String parentRevision			   = busWithSelect.getSelectData("revision");
				StringList parentRevisions		   = busWithSelect.getSelectDataList("revisions");
				StringList connectedObjIds		   = busWithSelect.getSelectDataList(selectOnDrwRelFrom + "id");
				StringList connectedObjTypes	   = busWithSelect.getSelectDataList(selectOnDrwRelFrom + "type");
				StringList connectedObjNames	   = busWithSelect.getSelectDataList(selectOnDrwRelFrom + "name");
				StringList connectedObjRevisions   = busWithSelect.getSelectDataList(selectOnDrwRelFrom + "revision");
				StringList connectedObjAllRevs     = busWithSelect.getSelectDataList(selectOnDrwRelFrom + "revisions");
				StringList cadTypes				   = busWithSelect.getSelectDataList(selectOnDrwRelFrom + "attribute[" + attrCADType + "]");
				StringList connectedObjIdsTo  	   = busWithSelect.getSelectDataList(selectOnDrwRelTo + "id");
				StringList connectedObjTypesTo	   = busWithSelect.getSelectDataList(selectOnDrwRelTo + "type");
				StringList connectedObjNamesTo	   = busWithSelect.getSelectDataList(selectOnDrwRelTo + "name");
				StringList connectedObjRevisionsTo = busWithSelect.getSelectDataList(selectOnDrwRelTo + "revision");
				StringList connectedObjAllRevsTo   = busWithSelect.getSelectDataList(selectOnDrwRelTo + "revisions");
				StringList cadTypesTo			   = busWithSelect.getSelectDataList(selectOnDrwRelTo + "attribute[" + attrCADType + "]");

				if(null != connectedObjIdsTo) 
				{
					if(null == connectedObjIds)
					{
						connectedObjIds       = new StringList();
						connectedObjTypes     = new StringList();
						connectedObjNames     = new StringList();
						connectedObjRevisions = new StringList();
						connectedObjAllRevs   = new StringList();
						cadTypes              = new StringList();
					}

					connectedObjIds.addAll(connectedObjIdsTo);
					connectedObjTypes.addAll(connectedObjTypesTo);
					connectedObjNames.addAll(connectedObjNamesTo);
					connectedObjRevisions.addAll(connectedObjRevisionsTo);
					connectedObjAllRevs.addAll(connectedObjAllRevsTo);
					cadTypes.addAll(cadTypesTo);
				}

				if(null != connectedObjIds)
				{
					for(int i=0; i<connectedObjIds.size(); i++)
					{
						String connectedObjId		= (String)connectedObjIds.get(i);
						String connectedObjType		= (String)connectedObjTypes.get(i);
						String connectedObjName		= (String)connectedObjNames.get(i);
						String connectedObjRevision	= (String)connectedObjRevisions.get(i);
						String cadType				= (String)cadTypes.get(i);

						if(null != connectedObjId && !connectedObjId.equals("") && isLatestRevision(parentObjRevision, parentRevision, parentRevisions) && ("presentation".equalsIgnoreCase(cadType) || "drawing".equalsIgnoreCase(cadType)))
						{
							StringBuffer typeNameKey = new StringBuffer();
							typeNameKey.append(connectedObjType);
							typeNameKey.append("|");
							typeNameKey.append(connectedObjName);

							if(!typeNameObjectIdMap.containsKey(typeNameKey.toString()))
							{
								objectIdTypeNameMap.put(connectedObjId, typeNameKey.toString());
								typeNameObjectIdMap.put(typeNameKey.toString(), connectedObjId);
								typeNameObjectRevMap.put(typeNameKey.toString(), connectedObjRevision);
							}
							else
							{
								// This is to replace existing version with later version to ensure latest version is kept in the retList.
								String existingEntryRevision = (String)typeNameObjectRevMap.get(typeNameKey.toString());

								if(isLatestRevision(connectedObjRevision, existingEntryRevision, connectedObjAllRevs))
								{
									objectIdTypeNameMap.put(connectedObjId, typeNameKey.toString());
									typeNameObjectIdMap.put(typeNameKey.toString(), connectedObjId);
									typeNameObjectRevMap.put(typeNameKey.toString(), connectedObjRevision);
								}
							}

							if(!retList.contains(connectedObjId))
							  	retList.add(connectedObjId);
						}
					}
				}
			}
			
			// Recursive call to check if the drawing or presentation has any associated_drawing relationship
					
			Iterator itr = retList.iterator();
			if(itr.hasNext())
			{
				String connectedObjId = (String) itr.next();
				if(!alreadyTraversedList.contains(connectedObjId))
				{
					alreadyTraversedList.add(connectedObjId);
					String typeNameKey          = (String)objectIdTypeNameMap.get(connectedObjId);
					Enumeration typeName        = MCADUtil.getTokensFromString(typeNameKey, "|");
					String connectedObjType     = (String)typeName.nextElement();
					String connectedObjName     = (String)typeName.nextElement();
					String connectedObjRevision = (String)typeNameObjectRevMap.get(typeNameKey);
					getRelatedDocuments(context, connectedObjId, connectedObjType, connectedObjName, connectedObjRevision, isMinorType, retList,alreadyTraversedList );
				}
			}
		}
		catch (Exception _ex)
		{
			MCADServerException.createManagedException(MCADAppletServletProtocol.DEFAULT_ERROR_CODE, serverResourceBundle.getString("mcadIntegration.Server.Message.RelatedDocumentError"), _ex);
		}
		finally 
		{
			resultItr.close();			
		}

		return retList;
	}*/

	protected boolean isLatestRevision(String presentRev, String previousRev, StringList objRevisions)
	{
		boolean isLatest = false;
		int presentRevIndex		= objRevisions.indexOf(presentRev);
		int previousRevIndex	= objRevisions.indexOf(previousRev);

		if(presentRevIndex != -1 && previousRevIndex != -1)
		{
			if(presentRevIndex >= previousRevIndex)
				isLatest = true;
		}
		return isLatest;
	}

	protected Vector getRelatedObjectsCount(Context context, String parentPartId, String childPartId) throws Exception
	{
		Vector relatedObjectsList = new Vector();
		float relationshipCount = 0;
		String Args[] = new String[6];
		Args[0] = parentPartId;
		Args[1] = "relationship";
		Args[2] = "EBOM";
		Args[3] = "relationship";
		Args[4] = "id";
		Args[5] = "|";
		String mqlResult = mxUtil.executeMQL(context, "expand bus $1 $2 $3 select $4 $5 dump $6", Args);

		if(mqlResult.startsWith("true"))
		{
			mqlResult = mqlResult.substring(5);
			StringTokenizer strTok = new StringTokenizer(mqlResult,"\n");
			while(strTok.hasMoreTokens())
			{
				String row					= strTok.nextToken();
				StringTokenizer rowElements = new StringTokenizer(row,"|");

				String level		= rowElements.nextToken();
				String relName		= rowElements.nextToken();
				String direction	= rowElements.nextToken();
				String objType		= rowElements.nextToken();
				String objName		= rowElements.nextToken();
				String objRev		= rowElements.nextToken();
				String relId		= rowElements.nextToken();

				BusinessObject relationObj = new BusinessObject(objType, objName, objRev, "");
				relationObj.open(context);
				String attrGenerateCollapsedEBOM	= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMSync-GenerateCollapsedEBOM");
				if(childPartId.equalsIgnoreCase(relationObj.getObjectId()))
				{
					boolean isCollapsedEBOM = "TRUE".equalsIgnoreCase(ebomConfObject.getConfigAttributeValue(attrGenerateCollapsedEBOM));
					if(isCollapsedEBOM)
					{
						Relationship relationship	= new Relationship(relId);
						String quantity				= (relationship.getAttributeValues(context,ATTR_QUANTITY)).getValue();

						java.text.NumberFormat nf	= java.text.NumberFormat.getInstance(Locale.ENGLISH);
						Number quantityNumber		= nf.parse(quantity);
						float fQuantity				= quantityNumber.floatValue();

						relationshipCount			= relationshipCount + fQuantity;
					}
					else
					{
						relationshipCount++;
					}
					relatedObjectsList.add(relId);
				}
				relationObj.close(context);
			}

		}
		else
		{
			throw new Exception("Problem in expanding bus");
		}

		relatedObjectsList.add(new Float(relationshipCount));
		return relatedObjectsList;
	}

	protected float getcadSubComponentRelationShipCount(Context context, Vector relIdsVector) throws Exception
	{
		float cadSubComponentRelationShipCount = 0;
		int count = 0;
		
		for(int i = 0 ; i < relIdsVector.size(); i++)
		{
			String relId = (String)relIdsVector.elementAt(i);
			Relationship relationship			= new Relationship(relId);
			String excludeFromBOM				= relationship.getAttributeValues(context,attrExcludeFromBOM).getValue();
			
			if(excludeFromBOM == null || (excludeFromBOM != null && !"true".equalsIgnoreCase(excludeFromBOM)))
			{
					String quantity						= relationship.getAttributeValues(context,ATTR_QUANTITY).getValue();
					java.text.NumberFormat nf			= java.text.NumberFormat.getInstance(Locale.ENGLISH);
					Number quantityNumber				= nf.parse(quantity);
					float fQuantity						= quantityNumber.floatValue();
	
					cadSubComponentRelationShipCount	= cadSubComponentRelationShipCount + fQuantity;
					
					Collections.swap(relIdsVector,count,i);
					count++;
				}

		}

		return cadSubComponentRelationShipCount;
	}

	protected float singleModeCadSubComponentRelationShipCount(Context context,ArrayList allInstanceList,Hashtable siblingObjects, Vector cadRelIDVectorForInstances) throws Exception
	{
		float cadSubComponentRelationShipCount = 0;
		
			for(int i=0; i < allInstanceList.size(); i++ )
			{
				String instanceId = (String)allInstanceList.get(i);
				String excludeFromBOM 	= mxUtil.getAttributeForBO(context, instanceId, MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-ExcludeFromBOM"));
				
				if(siblingObjects.keySet().contains(instanceId) && "false".equalsIgnoreCase(excludeFromBOM))
				{
					Vector relIdsVector =	(Vector)siblingObjects.get(instanceId);
					for(int j = 0 ; j < relIdsVector.size(); j++)
					{
						String relId = (String)relIdsVector.elementAt(j);
					
					cadRelIDVectorForInstances.add(relId);
					
						Relationship relationship			= new Relationship(relId);
						String excludeFromBOMStr				= relationship.getAttributeValues(context,attrExcludeFromBOM).getValue();
						if(excludeFromBOMStr == null || (excludeFromBOMStr != null && "false".equalsIgnoreCase(excludeFromBOMStr)))
						{
						String quantity						= relationship.getAttributeValues(context,ATTR_QUANTITY).getValue();

						java.text.NumberFormat nf			= java.text.NumberFormat.getInstance(Locale.ENGLISH);
						Number quantityNumber				= nf.parse(quantity);
						float fQuantity						= quantityNumber.floatValue();

						cadSubComponentRelationShipCount	= cadSubComponentRelationShipCount + fQuantity;
					}
				}
			}
		}
		
		return cadSubComponentRelationShipCount;
	}

//	Returns the revision of Part Object if it is released.
	protected String getPartRevisionIfReleased(Context context, String partObjectId, String targetRevision, String cadModelType, String partMatchRule) throws Exception
	{
		String revisedPartId = partObjectId;

		if(isPartReleased(context, partObjectId))
		{
			BusinessObject partObject = new BusinessObject(partObjectId);
			partObject.open(context);

			String sNewRev = partObject.getNextSequence(context);

			if(targetRevision != null && !targetRevision.equals(""))
				sNewRev = targetRevision;

			// changed !targetRevision.equals to targetRevision.equals for issue 355656 
			if(targetRevision != null && partMatchRule.equals(MATCH_CADMODEL_REV) && targetRevision.equals(partObject.getRevision()))
			{
				Hashtable messageDetails = new Hashtable(3);
				messageDetails.put("NAME", partObject.getName());
				messageDetails.put("CADMODELTYPE", cadModelType);

				String message = serverResourceBundle.getString("mcadIntegration.Server.Message.partWithMatchRevAlreadyReleased", messageDetails);

				MCADServerException.createException(message,null);
			}

			BusinessObject revisedPartObject = new BusinessObject(partObject.getTypeName(),partObject.getName(),sNewRev,partObject.getVault());

			if(revisedPartObject.exists(context))
			{
			revisedPartId = revisedPartObject.getObjectId(context);
			}
			else
			{
					revisedPartObject	= partObject.revise(context, sNewRev, partObject.getVault());
					revisedPartId				= revisedPartObject.getObjectId(context);
			}

			BusinessObjectList relatedCadObjList = mxUtil.getRelatedBusinessObjects(context,new BusinessObject(revisedPartId),relPartSpecification,"from");
			
			if(relatedCadObjList != null && relatedCadObjList.size() > 0)
			{
				for(int k =0;k < relatedCadObjList.size();k++)
				{
					BusinessObject busObj = (BusinessObject)relatedCadObjList.get(k);
					busObj.open(context);
					disconnectPartSpecRelations(context, busObj, revisedPartObject);
				}
			}
			
			
			partObject.close(context);
		}

		return revisedPartId;
	}

	protected boolean isPartReleased(Context context, String partObjectId) throws Exception
	{
		boolean bReleased = false;
		String PART_RELEASE_STATE = ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_RELEASE_PART_STATE);

		String [] oids = new String[]{partObjectId};
		StringList selectlist = new StringList(3);

		selectlist.add("current");
		selectlist.add("state");
		BusinessObjectWithSelect busWithSelect = BusinessObject.getSelectBusinessObjectData(context, oids, selectlist).getElement(0);

		String currentState 		= busWithSelect.getSelectData("current");
		StringList majorStateList 	= busWithSelect.getSelectDataList("state");

			//If part is released, then revise the part
		if(majorStateList.lastIndexOf(currentState) >= majorStateList.lastIndexOf(PART_RELEASE_STATE))
				bReleased = true;

		return bReleased;
	}

	protected void disconnectEBOMRelation(Context context, BusinessObjectList streamBOList, String partObjId, String sourceName, String currentModelID) throws Exception
	{
		BusinessObjectItr boItr = new BusinessObjectItr(streamBOList);
		String stableEBOMRelEnabled 			 = isStableEBOMRelEnabled();

		while(boItr.next())
		{
			BusinessObject streamBO = boItr.obj();
			// Skip disconnecting from the current CAD Model, disconnect for
			// all other objects in the stream, Part Spec and EBOM
			String streamBOID = streamBO.getObjectId();
			if (currentModelID.equals(streamBOID))
			{
				continue;
			}

			BusinessObjectList partObjList = mxUtil.getRelatedBusinessObjects(context, streamBO, relPartSpecification, "to");
			BusinessObjectItr partObjItr = new BusinessObjectItr(partObjList);
			while(partObjItr.next())
			{
				BusinessObject partObj = partObjItr.obj();
				partObj.open(context);
				String connectedPartObjId = partObj.getObjectId();
				partObj.close(context);

				// IR-523120-3DEXPERIENCER2016x: Skip disconnecting relationships which are not connected to new PART object.
				if(!partObjId.equals(connectedPartObjId))
					continue;

				if(!"true".equals(stableEBOMRelEnabled))
				disconnectEBOMRelations(context, partObj, sourceName);
				disconnectPartSpecRelations(context, streamBO, partObj);
			}
		}
	}

	protected boolean disconnectPartSpecRelations(Context context, BusinessObject streamBO, BusinessObject partObj) throws Exception
	{
		boolean bRet = true;
		try
		{
			short level = 1;
			Visuals vis = context.getDefaultVisuals();

			//build the relationship where clause to be used in expand on bus object
			StringBuffer relationshipWhereClause = new StringBuffer();
			relationshipWhereClause.append("(name == const\"");
			relationshipWhereClause.append(relPartSpecification);
			relationshipWhereClause.append("\"");
			relationshipWhereClause.append(" && to.id == const\"");
			relationshipWhereClause.append(streamBO.getObjectId());
			relationshipWhereClause.append("\"");
			relationshipWhereClause.append(")");

			//call the expand method on the business object
			Expansion expansion = partObj.expand(context,level,"",relationshipWhereClause.toString(),vis);

			ExpandRelationshipList filteredRelationshipList = expansion.getRelationships();
			ExpandRelationshipItr expandItr = new ExpandRelationshipItr(filteredRelationshipList);

			while (expandItr.next())
			{
				ExpandRelationship expandRel = expandItr.obj();
				expandRel.remove(context);
			}
		}
		catch(Exception me)
		{
			MCADServerException.createException(me.getMessage(), me);
		}
		return bRet;
	}

	protected boolean disconnectEBOMRelations(Context context, BusinessObject partBO, String sourceName) throws Exception
	{
		boolean bRet = true;
		try
		{
			short level = 1;
			Visuals vis = context.getDefaultVisuals();

			//build the relationship where clause to be used in expand on bus object
			StringBuffer relationshipWhereClause = new StringBuffer();
			relationshipWhereClause.append("(name == const\"");
			relationshipWhereClause.append(relEBOM);
			relationshipWhereClause.append("\"");
			relationshipWhereClause.append(" && attribute[");
			relationshipWhereClause.append(attrSource);
			relationshipWhereClause.append("] == const\"");
			relationshipWhereClause.append(sourceName);
			relationshipWhereClause.append("\"");
			relationshipWhereClause.append(")");

			//call the expand method on the business object
			Expansion expansion = partBO.expand(context,level,"",relationshipWhereClause.toString(),vis);

			ExpandRelationshipList filteredRelationshipList = expansion.getRelationships();
			ExpandRelationshipItr expandItr = new ExpandRelationshipItr(filteredRelationshipList);

			while (expandItr.next())
			{
				ExpandRelationship expandRel = expandItr.obj();
				boolean isFrom = expandRel.isFrom();
				if(isFrom)
				{
					expandRel.remove(context);
				}
			}
		}
		catch(Exception me)
		{
			MCADServerException.createException(me.getMessage(), me);
		}
		return bRet;
	}

	protected void matchRevisionSequence(Context context, String busObjectID, String matchingPartID, boolean isMinorType) throws MCADException
	{
		try
		{
			String cadPolicyName		   = "";
			String partPolicyName		   = "";
			Policy policyObject            = null;
			Policy policyMatchingObject    = null;
			String partObjSeq              = null;
			String matchingObjSeq          = null;

			BusinessObject  partBusObject  = new BusinessObject(busObjectID);
			partBusObject.open(context);

			policyObject				   = partBusObject.getPolicy(context);

			/*if(isMinorType) //[NDM]
			{				 	
				BusinessObject _busObject	= mxUtil.getMajorObject(context, partBusObject);
				_busObject.open(context);
				policyObject				= _busObject.getPolicy(context);
				_busObject.close(context);
			}*/

			BusinessObject  matchingObject    = new BusinessObject(matchingPartID);
			matchingObject.open(context);

			policyObject.open(context);
			cadPolicyName = policyObject.getName();

			policyMatchingObject		      = matchingObject.getPolicy(context);

			if(policySequenceMap.containsKey(cadPolicyName))
			{
				partObjSeq            = (String)policySequenceMap.get(cadPolicyName);
			}
			else
			{
				partObjSeq            = policyObject.getSequence();
				policySequenceMap.put(cadPolicyName,partObjSeq);
			}

			policyObject.close(context);

			policyMatchingObject.open(context);
			partPolicyName = policyMatchingObject.getName();

			if(policySequenceMap.containsKey(partPolicyName))
			{
				matchingObjSeq         = (String)policySequenceMap.get(partPolicyName);
			}
			else
			{
				matchingObjSeq         = policyMatchingObject.getSequence();
				policySequenceMap.put(partPolicyName, matchingObjSeq);
			}

			policyMatchingObject.close(context);

			if(!matchingObjSeq.equals(partObjSeq))
			{
				Hashtable messageDetails = new Hashtable(3);
				messageDetails.put("CADPOLICY", cadPolicyName);
				messageDetails.put("PARTPOLICY", partPolicyName);

				String message = serverResourceBundle.getString("mcadIntegration.Server.Message.RevisionSequenceCADandDevMisMatch", messageDetails);

				MCADServerException.createException(message , null);
			} 
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
	}

	protected void matchPartAndCADModelRevision(Context context, String partID, String cadModelRevision, String cadModelName, String cadModelType) throws Exception
	{
		BusinessObject  partObject    = new BusinessObject(partID);
		partObject.open(context);

		String partRevision			   =  partObject.getRevision();
		if(!partRevision.equals(cadModelRevision))
		{
			Hashtable messageDetails = new Hashtable(3);
			messageDetails.put("NAME", cadModelName);
			messageDetails.put("CADMODELTYPE", cadModelType);

			String message = serverResourceBundle.getString("mcadIntegration.Server.Message.partRevAndCADModelRevMismatch", messageDetails);
			MCADServerException.createException(message,null);
		}

		partObject.close(context);
	}

		private void copyAttribsFromCadObjToPart(Context _context,String partObjId,String busObjectID,String busType,String instanceName,
			MCADGlobalConfigObject globalConfigObject,String language) throws Exception
	{
		BusinessObject partObject 	= new BusinessObject(partObjId);
		String cadType				= mxUtil.getAttributeForBO(_context, busObjectID, attrCADType);
		
		partObject.open(_context);

		String partType = partObject.getTypeName();

		String args[] = new String[4];
		args[0] = ebomConfObject.getTypeName();
		args[1] = ebomConfObject.getName();
		args[2] = ebomConfObject.getRevision();
		args[3] = language;
		
		String famID 				= null;
		String famBusType 			= busType;
		String ebomExpositionMode	= null;
		if(globalConfigObject.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
		{
			famID = serverGeneralUtil.getTopLevelFamilyObjectForInstance(_context, busObjectID);

			if(famID != null)
			{
				ebomExpositionMode 	= mxUtil.getAttributeForBO(_context, famID, MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-EBOMExpositionMode"));
				
				if(famID != null && "single".equalsIgnoreCase(ebomExpositionMode))
				{
				DomainObject busObject = DomainObject.newInstance(_context, famID);
				famBusType = busObject.getInfo(_context, DomainObject.SELECT_TYPE);
			}
		}
		}

		IEFEBOMSyncFindMatchingPartBase_mxJPO iefEBOMSyncFindMatchingParts = new IEFEBOMSyncFindMatchingPartBase_mxJPO(_context, args);
		
		if(famID != null && "single".equalsIgnoreCase(ebomExpositionMode))
			iefEBOMSyncFindMatchingParts.copyAttribsFromCadObjToPart(_context, partObject, new BusinessObject(famID), instanceName,famBusType , partType);
		else
			iefEBOMSyncFindMatchingParts.copyAttribsFromCadObjToPart(_context, partObject, new BusinessObject(busObjectID), instanceName, busType, partType);

		partObject.close(_context);
		
	}
			private String isStableEBOMRelEnabled(){
			try{
				ResourceBundle iefProperties = ResourceBundle.getBundle("ief");
				String stableEBOMRelEnabled 			 = iefProperties.getString("mcadIntegration.Enable.StableEBOMRel");

				if(stableEBOMRelEnabled != null && !stableEBOMRelEnabled.trim().equals(""))
				{
						stableEBOMRelEnabled = stableEBOMRelEnabled.trim();
				}
				return stableEBOMRelEnabled;
			}catch(Exception ex){
				return "";
			}
		
		}

}

