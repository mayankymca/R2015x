/*
 **  IEFReviseMajorTrigger
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to use as trigger on Revise event of Major Objects
 */
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.db.Relationship;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;

public class IEFReviseMajorTrigger_mxJPO
{
	private boolean isCheckinEx = false;
	private boolean isCheckin   = false;
	private boolean isPromote 	= false;

	MatrixWriter	_mxWriter	= null;

	private HashSet revisedObjectIds 				= null;
	private Hashtable familyidSelectForReviseMap 	= null;
	private Hashtable instanceRevisedMap 			= null;

	private int recursionCount 						= 0;
	MCADMxUtil util									= null;
	MCADServerGeneralUtil serverGeneralUtil 		= null;

	String iefFileMessageSigest						= "";
	String iefFileSourceAttrActualName				= "";
	String cadType									= "";
	String attrSource								= "";
	String instanceOfRelName 	                	= "";
	/**
	 * The no-argument constructor.
	 */
	public  IEFReviseMajorTrigger_mxJPO()
	{
	}

	/**
	 * Constructor which accepts the Matrix context and an array of String
	 * arguments.
	 */
	public  IEFReviseMajorTrigger_mxJPO(Context context, String[] args) throws Exception
	{
		_mxWriter 					= new MatrixWriter(context);
		util 						= new MCADMxUtil(context, null, new IEFGlobalCache());

		isCheckin   	  			= getRPEforOperation(context, util, MCADServerSettings.IEF_CHECKIN).equalsIgnoreCase("true");
		isCheckinEx 	  			= getRPEforOperation(context, util, MCADServerSettings.IEF_CHECKINEX).equalsIgnoreCase("true");
		isPromote	      			= getRPEforOperation(context, util, "Finalize").equalsIgnoreCase("true");

		revisedObjectIds  			= new HashSet();
		familyidSelectForReviseMap  = new Hashtable();
		instanceRevisedMap 			= new Hashtable();

		iefFileMessageSigest		= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-FileMessageDigest");
		iefFileSourceAttrActualName	= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-FileSource");
		cadType						= MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType");			
		attrSource					= MCADMxUtil.getActualNameForAEFData(context, "attribute_Source");
		instanceOfRelName			= MCADMxUtil.getActualNameForAEFData(context, "relationship_InstanceOf");
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	private String getRPEforOperation(matrix.db.Context context,MCADMxUtil mxUtil,  String operationName)
	{

		String Args[] = new String[2];
		Args[0] = "global";
		Args[1] = operationName;
		String sResult = mxUtil.executeMQL(context, "get env $1 $2", Args);
		String result	= "";
		if(sResult.startsWith("true"))
		{
			result = sResult.substring(sResult.indexOf("|")+1, sResult.length());
		}

		return result;
	}

	public int reviseObjectAndCreateRealtionship(Context context, String []args) throws Exception
	{
		if(isCheckin || isCheckinEx || isPromote)
			return 0;

		try
		{
			String newRevision		= args[0];
			String _sObjectID		= args[1];

			String ATTR_SOURCE		= "attribute[" + attrSource + "]";
			String ATTR_CAD_TYPE	= "attribute[" + cadType + "]";

			StringList selectList	= new StringList(4);

			selectList.add("type");
			selectList.add("name");
			selectList.add(ATTR_SOURCE);
			selectList.add(ATTR_CAD_TYPE);

			BusinessObjectWithSelect busWithSelect 	= (BusinessObjectWithSelect)BusinessObject.getSelectBusinessObjectData(context, new String[]{_sObjectID}, selectList).get(0);

			String busName							=  busWithSelect.getSelectData("name");

			BusinessObject revisedMajorObject		= new BusinessObject(busWithSelect.getSelectData("type"), busName, newRevision, "");

			BusinessObject oldBusObject				= new BusinessObject(_sObjectID);
			BusinessObject oldMinorObject 			= null;

			if(familyidSelectForReviseMap.containsKey(_sObjectID))
			{
				String familyMajorId = (String)familyidSelectForReviseMap.get(_sObjectID);
				oldBusObject	 	 = new BusinessObject(familyMajorId);
			}

			// to take the structure of instance from the proper file
			if(instanceRevisedMap.containsKey(_sObjectID))
			{
				String id 		= (String)instanceRevisedMap.get(_sObjectID);
				oldBusObject	= new BusinessObject(id);
			}

			oldMinorObject		= util.getActiveMinor(context, oldBusObject);

			//in case incoming object is minor when a family in pre-finalization state revised.
			//if(oldMinorObject == null)
			//	oldMinorObject = oldBusObject;

			String revisedObjectId					= revisedMajorObject.getObjectId(context);

			//put new revised object id so can be checked for future in.
			revisedObjectIds.add(revisedObjectId);
			revisedObjectIds.add(_sObjectID);

			MCADGlobalConfigObject globalConfigObj	= getGlobalConfigObject(context, revisedObjectId);

			Hashtable childBusAndRelDetailsTable	= new Hashtable();
			BusinessObject revisedMinorObject		= null;
			BusinessObject famObj					= null;
			BusinessObject connectedFamilyObject	= null;

			String sDefaultFamRevMode				= globalConfigObj.getDefaultFamRevMode();
			String attrFTRevisionMode 				= "";

			if(null == serverGeneralUtil)
				serverGeneralUtil = new MCADServerGeneralUtil(context, globalConfigObj, new MCADServerResourceBundle("en-US"), new IEFGlobalCache());

			revisedMajorObject.open(context);

			String attrVal							= busWithSelect.getSelectData(ATTR_SOURCE);
			String cadTypeVal						= busWithSelect.getSelectData(ATTR_CAD_TYPE);

			String majorType						= revisedMajorObject.getTypeName();
			String busType							= "";
			String policy							= "";
			String revisedBusRev                    = "";

			if(recursionCount == 0)
				util.executeMQL(context, "set env global MCADINTEGRATION_CONTEXT true");

			recursionCount++;

			if(util.isMajorObject(context, revisedObjectId))
			{
				busType 				= util.getCorrespondingType(context, majorType);
				policy 					= globalConfigObj.getDefaultPolicyForType(busType);
				String versionPolicy	= util.getRelatedPolicy(context, policy);
				String minorVault		= revisedMajorObject.getVault().toString();
				revisedBusRev 			= MCADUtil.getFirstVersionStringForStream(newRevision);

				revisedMinorObject		= new BusinessObject(busType, busName, revisedBusRev, minorVault);

				revisedMinorObject.create(context, versionPolicy);
			}
			else
			{	
				revisedMinorObject = revisedMajorObject;
				revisedBusRev = revisedMajorObject.getRevision();
			}

			oldMinorObject.open(context);

			Hashtable relsAndEnds 			= null;	
			Hashtable relsAndEndsForMajor 	= null;	
			Hashtable relIdBusToConnectData = new Hashtable();
			Hashtable activeInstanceRels 	= new Hashtable();

			if(globalConfigObj.isTypeOfClass(cadTypeVal, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
			{
				//if(serverGeneralUtil.isBusObjectFinalized(context, oldBusObject))
				//{
				relsAndEnds					= serverGeneralUtil.getPrimaryRelationshipFromInstance(context, oldBusObject);
				famObj						= serverGeneralUtil.getFamilyObjectForInstance(context, oldBusObject);
				connectedFamilyObject      	= famObj;

				BusinessObject majorFamObj	= null;

				if(util.isMajorObject(context, famObj.getObjectId(context)))
					majorFamObj	= famObj;
				else
					majorFamObj = util.getMajorObject(context, famObj);
				/*}
				else
				{
					relsAndEnds	= serverGeneralUtil.getPrimaryRelationshipFromInstance(context, oldMinorObject);

					famObj				  = serverGeneralUtil.getFamilyObjectForInstance(context, oldMinorObject);
					connectedFamilyObject = famObj;
					famObj		= util.getMajorObject(context,famObj );
				}*/

				Hashtable relsAndEndsAssemLike	=  serverGeneralUtil.getAllWheareUsedRelationships(context, oldBusObject, true, MCADAppletServletProtocol.ASSEMBLY_LIKE);					
				relsAndEnds.putAll(relsAndEndsAssemLike);

				getFirstLevelChildrenDetails(context, relsAndEnds, childBusAndRelDetailsTable);

				if(null != sDefaultFamRevMode && !"".equals(sDefaultFamRevMode))
				{
					String familyId 	= famObj.getObjectId();
					attrFTRevisionMode  = serverGeneralUtil.getFTRevisionMode(context,familyId);
				}
			}
			else if(globalConfigObj.isTypeOfClass(cadTypeVal, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
			{
				relsAndEnds				=  serverGeneralUtil.getAllWheareUsedRelationships(context, oldBusObject, true, MCADServerSettings.FAMILY_LIKE);

				getFirstLevelChildrenDetails(context, relsAndEnds, childBusAndRelDetailsTable);

				if(null != sDefaultFamRevMode && !"".equals(sDefaultFamRevMode))
					attrFTRevisionMode = serverGeneralUtil.getFTRevisionMode(context,_sObjectID);

				String cadTypeRevisedMinorObject = util.getCADTypeForBO(context, oldBusObject);
				boolean isFamily = globalConfigObj.isTypeOfClass(cadTypeRevisedMinorObject,MCADAppletServletProtocol.TYPE_FAMILY_LIKE);
				activeInstanceRels = serverGeneralUtil.getActiveInstanceRelationships(context, oldBusObject,isFamily);			
			}
			else
			{
				relsAndEnds	= serverGeneralUtil.getAllWheareUsedRelationships(context, oldBusObject, true, MCADAppletServletProtocol.ASSEMBLY_LIKE);

				if(relsAndEnds.size() > 0)
					getFirstLevelChildrenDetails(context, relsAndEnds, childBusAndRelDetailsTable);
			}

			oldMinorObject.close(context);

			//BusinessObject objectForWhereUsed = oldBusObject;

			String sDescription = oldBusObject.getDescription(context);

			if(sDescription != null && !sDescription.equals(""))
			{
				String sMinorDescription = revisedMinorObject.getDescription(context);
				if(!sMinorDescription.equals(sDescription))
					revisedMinorObject.setDescription(context,sDescription);
			}

			//boolean isBusObjectFinalized = serverGeneralUtil.isBusObjectFinalized(context, oldMinorObject);
			//if(!isBusObjectFinalized)
			//	objectForWhereUsed = oldMinorObject;
			copyAttributesAndFiles(context, oldBusObject, oldMinorObject, revisedMajorObject, revisedMinorObject, attrVal);

			//Copying the First level Child ren Associated with Parent to New Revision from previous Minor Object.
			copyRelationships(context, globalConfigObj, busName, childBusAndRelDetailsTable, revisedMajorObject, 
					revisedMinorObject, revisedBusRev, relIdBusToConnectData, oldBusObject);

			connectMajorAndMinorObjects(context, revisedMajorObject, revisedObjectId, revisedMinorObject);

			revisedMajorObject.update(context);
			revisedMajorObject.close(context);

			if(globalConfigObj.isTypeOfClass(cadTypeVal, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
			{
				reviseFamily(context, _sObjectID, oldBusObject, revisedMinorObject, famObj, connectedFamilyObject, 
						attrFTRevisionMode, globalConfigObj);
			}

			if("together".equalsIgnoreCase(attrFTRevisionMode) && globalConfigObj.isTypeOfClass(cadTypeVal, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
			{
				reviseAllInstances(context, globalConfigObj, oldBusObject, connectedFamilyObject, cadTypeVal);
			}

			if(globalConfigObj.isModificationEvent(MCADAppletServletProtocol.UPDATESTAMP_EVENT_CHECKIN))
			{
				util.modifyUpdateStamp(context, revisedObjectId);
			}

			copyActiveInstanceRelationships(context, globalConfigObj, activeInstanceRels, revisedMinorObject);
		}
		catch(Exception me)
		{
			_mxWriter.write("Error occurred:" + me.getMessage());
			MCADServerException.createException(me.getMessage(), me);
		}
		finally
		{
			if(recursionCount == 1)
				util.executeMQL(context, "unset env global MCADINTEGRATION_CONTEXT");

			recursionCount--;
		}

		return 0;
	}

	private void connectMajorAndMinorObjects(Context context, BusinessObject revisedBusinessObject,	String objectId, 
			BusinessObject minorObject) throws MCADException, MatrixException,	Exception 
			{
		String minorObjID                        = minorObject.getObjectId();
		String majorIdForConnection              = objectId;
		boolean  connectVersionOfRel             = true;
		boolean  isMajorMinorConnectionRequired  = true; 

		//if incoming object is minor
		if(minorObjID.equals(objectId))
		{
			BusinessObject majorObject = util.getMajorObject(context, minorObject);
			majorIdForConnection       = majorObject.getObjectId(context);

			String revisedBusObjID     = revisedBusinessObject.getObjectId();

			String latestVersionId   = util.getLatestMinorID(context, majorObject);
			String activeVersionId   = util.getActiveVersionObject(context, majorIdForConnection);

			if(!latestVersionId.equals(revisedBusObjID) && !activeVersionId.equals(revisedBusObjID))
			{
				String relLatestVersion  = MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion");
				String relActiveVersion  = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");

				util.disconnectBusObjects(context, majorIdForConnection, latestVersionId, relLatestVersion, true);
				util.disconnectBusObjects(context, majorIdForConnection, activeVersionId, relActiveVersion, true);
			}
			else
				isMajorMinorConnectionRequired = false;

			connectVersionOfRel       = false;
		}

		if(isMajorMinorConnectionRequired)
			serverGeneralUtil.connectMajorAndMinorObjects(context, minorObjID , majorIdForConnection,connectVersionOfRel);
			}

	// [NDM] QWJ
	private void copyAttributesAndFiles(Context context, BusinessObject oldBusObject, BusinessObject oldMinorObject, 
			BusinessObject revisedBusinessObject, BusinessObject revisedMinorObject, String attrVal) throws MatrixException, MCADException 
			{
		String expositionModeAttrName = MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-EBOMExpositionMode");
		String attrIsVersionObject		 = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
		String	attrMoveFilesToVersion	= MCADMxUtil.getActualNameForAEFData(context, "attribute_MoveFilesToVersion");

		AttributeList attributeListToUpdate		= oldMinorObject.getAttributeValues(context, false);
		revisedMinorObject.setAttributes(context, attributeListToUpdate);

		// [NDM] QWJ
		//if(isBusObjectFinalized)
		//	util.copyFilesFcsSupported( context,  oldMinorObject, revisedMinorObject);
		//else
		//util.copyFilesFcsSupported( context,  revisedMinorObject, revisedBusinessObject);
		util.copyFilesFcsSupported(context,  oldBusObject, revisedBusinessObject);
		util.copyFilesFcsSupported(context, oldMinorObject, revisedMinorObject);

		AttributeList attributesToUpdateList		= new AttributeList();
		Attribute attributeToUpdate					= new Attribute(new AttributeType(iefFileMessageSigest), "");
		Attribute attributeIEFFileSource			= new Attribute(new AttributeType(iefFileSourceAttrActualName), MCADAppletServletProtocol.FILESOURCE_SAVEAS);
		Attribute attributeToUpdateForSource		= new Attribute(new AttributeType(attrSource), attrVal);
		
		attributesToUpdateList.addElement(new Attribute(new AttributeType(attrMoveFilesToVersion), "True"));
		attributesToUpdateList.addElement(attributeToUpdate);
		attributesToUpdateList.addElement(attributeIEFFileSource);
		attributesToUpdateList.addElement(attributeToUpdateForSource);

		if(serverGeneralUtil.doesAttributeExistsOnType(context, revisedMinorObject.getTypeName(), expositionModeAttrName))
		{
			Attribute expositionMode = new Attribute(new AttributeType(expositionModeAttrName), util.getAttributeForBO(context, revisedBusinessObject, expositionModeAttrName));
			attributesToUpdateList.addElement(expositionMode);
		}

		revisedBusinessObject.setAttributes(context, attributesToUpdateList);

		attributesToUpdateList.addElement(new Attribute(new AttributeType(attrMoveFilesToVersion), "False"));
		attributesToUpdateList.addElement(new Attribute(new AttributeType(attrIsVersionObject), "True"));

		revisedMinorObject.setAttributes(context, attributesToUpdateList);
		revisedMinorObject.update(context);
			}

	private void reviseFamily(Context context, String _sObjectID, BusinessObject oldBusObject, BusinessObject minorObject, BusinessObject famObj, 
			BusinessObject connectedFamilyObject, String attrFTRevisionMode, MCADGlobalConfigObject globalConfigObj) throws MCADException, MatrixException, Exception
			{
		BusinessObject familyForRevise = null;
		String         famNewRev       = "";

		if("together".equalsIgnoreCase(attrFTRevisionMode)) 
		{
			familyForRevise = famObj;

			//Take the latest revision for revise
			BusinessObjectList majorRevisionsList 	= familyForRevise.getRevisions(context);

			familyForRevise = (BusinessObject)majorRevisionsList.lastElement();
			famNewRev       = familyForRevise.getNextSequence(context);
		}
		else
		{
			familyForRevise                 = connectedFamilyObject;
			String latestRevId              = util.getLatestRevisionID(context, familyForRevise.getObjectId(context));
			BusinessObject latestRevisionObj = new BusinessObject(latestRevId);
			latestRevisionObj.open(context);
			
			famNewRev                       = latestRevisionObj.getNextSequence(context); 
			//famNewRev                     = util.getNextVersionString(latestRevisionObj.getRevision());
			if(!familyForRevise.getObjectId(context).equals(latestRevId))
			{
				familyForRevise=latestRevisionObj;
			}
			
			latestRevisionObj.close(context);
		}

		if(!revisedObjectIds.contains(famObj.getObjectId(context)) && !revisedObjectIds.contains(familyForRevise.getObjectId(context)))
		{ 
			familyForRevise.open(context);

			String   famvault   		= familyForRevise.getVault();
			String familyObjectId 		= familyForRevise.getObjectId();
			String connectedObjectId 	= connectedFamilyObject.getObjectId();

			familyidSelectForReviseMap.put(familyObjectId, connectedObjectId);

			BusinessObject revisedFamilyObject = familyForRevise.revise(context,famNewRev,famvault);

			if(globalConfigObj.isModificationEvent(MCADAppletServletProtocol.UPDATESTAMP_EVENT_CHECKIN))
				util.modifyUpdateStamp(context, revisedFamilyObject.getObjectId(context));

			familyForRevise.close(context);
		}
		else
		{
			BusinessObject revisedFamilyActiveMinor = util.getActiveMinor(context, familyForRevise);

			if(revisedFamilyActiveMinor == null)
				revisedFamilyActiveMinor = familyForRevise;

			/*
			 * if instance is not finalized then take the ID of active Minor 
			 */
			String disconnectObjectId = _sObjectID;

			if(this.instanceRevisedMap.containsKey(_sObjectID)){
				disconnectObjectId = (String)this.instanceRevisedMap.get(_sObjectID);
			}


			
			if(util.isMajorObject(context, disconnectObjectId))
			{

				BusinessObject instanceMajorObject		= new BusinessObject(disconnectObjectId);
				BusinessObject instanceMinorObject		= util.getActiveMinor(context, instanceMajorObject);
				String sOldInstMinorId = instanceMinorObject.getObjectId();



			//disconnect old instance from this family 
			util.disconnectBusObjects(context, revisedFamilyActiveMinor.getObjectId(context),sOldInstMinorId, instanceOfRelName, true);

			//connect new instance's active minor to this family
			util.ConnectBusinessObjects(context, revisedFamilyActiveMinor, minorObject, instanceOfRelName, true);	

			BusinessObject newInstMajorObj = util.getMajorObject(context,minorObject );
				//disconnect old instance from this family 
				util.disconnectBusObjects(context, familyForRevise.getObjectId(context),disconnectObjectId, instanceOfRelName, true);

				//connect new instance's active minor to this family
				util.ConnectBusinessObjects(context, familyForRevise, newInstMajorObj, instanceOfRelName, true);	
			}


		}
			}

	// [NDM] QWJ
	private void copyRelationships(Context context, MCADGlobalConfigObject globalConfigObj, String busName, Hashtable childBusAndRelDetailsTable, 
			BusinessObject revisedBusinessObject, BusinessObject revisedMinorObject, String busRevision, Hashtable relIdBusToConnectData, 
			BusinessObject oldBusObject) throws MatrixException, MCADException, Exception
			{
		Enumeration childRelationDetails	= childBusAndRelDetailsTable.keys();

		Hashtable relIdBusToConnectDataForMinor = new Hashtable();
		while(childRelationDetails.hasMoreElements())
		{
			String relid = (String)childRelationDetails.nextElement();

			Relationship relation		= new Relationship(relid);
			Vector indvChildDetails		= (Vector) childBusAndRelDetailsTable.get(relid);

			String childBusID			= (String)indvChildDetails.elementAt(0);
			String relName				= (String)indvChildDetails.elementAt(1);
			String isFromStr			= (String)indvChildDetails.elementAt(10);	

			boolean isFrom				= false;
			if(isFromStr.equalsIgnoreCase("to"))
				isFrom					= true;

			BusinessObject childBusObject 	= new BusinessObject(childBusID);

			// [NDM] QWJ Start
			boolean isChildMajorObject		= util.isMajorObject(context, childBusID);
			/*childBusObject.open(context);

			String childTypeName = childBusObject.getTypeName();

			childBusObject.close(context);*/
			// [NDM] QWJ End

			Relationship relationship	= null;
		
				//get latest instance and connect
				BusinessObject childBusMajorObj = null;

				// [NDM] QWJ
				//if(!globalConfigObj.isMajorType(childTypeName))
				if(!isChildMajorObject)
				{
					childBusMajorObj = util.getMajorObject(context,childBusObject );
				}
				else
				{
					childBusMajorObj = childBusObject;
					childBusObject	 = util.getActiveMinor(context, childBusObject);
				}

				childBusMajorObj.open(context);				
				String childMajorId = childBusMajorObj.getObjectId();						
				childBusMajorObj.close(context);

				BusinessObject latestMajorChildObj = null;
				BusinessObject latestMinorChildObj = null;

				if(revisedObjectIds.contains(childMajorId))
				{
					String lastRevisionBusId = util.getLatestRevisionID(context, childMajorId);
					latestMajorChildObj 		= new BusinessObject(lastRevisionBusId);
					String latestMinorChildId = util.getLatestMinorID(context, latestMajorChildObj);
					latestMinorChildObj 		= new BusinessObject(latestMinorChildId);
				}
				else
				{
					latestMajorChildObj = childBusMajorObj;
					latestMinorChildObj = childBusObject;
				}
			if(relName.equals(instanceOfRelName) && isFrom )
			{
				// [NDM] : QWJ
				//if(serverGeneralUtil.isBusObjectFinalized(context, latestMinorChildObj))
				relationship	= util.ConnectBusinessObjects(context, revisedBusinessObject, latestMajorChildObj, relName, isFrom);	
				util.copyAttributesonRelatinship(context, relation, relationship);
				//else
				relationship	= util.ConnectBusinessObjects(context, revisedMinorObject, latestMinorChildObj, relName, isFrom);

				util.copyAttributesonRelatinship(context, relation, relationship);
			}
			else if(!relName.equals(instanceOfRelName))
			{
				Hashtable attrNameVals   = util.getRelationshipAttrNameValMap(context, relation);

				Hashtable busObjectDataMap = new Hashtable();
				Hashtable busObjectDataMapForMinor = new Hashtable();
				// [NDM] QWJ
				if(isChildMajorObject)
				{
					busObjectDataMap.put(MCADMxUtil.FROM_BUS_ID,revisedBusinessObject.getObjectId(context));
					busObjectDataMap.put(MCADMxUtil.TO_BUS_ID, childMajorId);

					busObjectDataMapForMinor.put(MCADMxUtil.FROM_BUS_ID,revisedMinorObject.getObjectId(context));
					busObjectDataMapForMinor.put(MCADMxUtil.TO_BUS_ID, latestMinorChildObj.getObjectId(context));
				}
				else
				{
					busObjectDataMap.put(MCADMxUtil.FROM_BUS_ID,revisedMinorObject.getObjectId(context));
					busObjectDataMap.put(MCADMxUtil.TO_BUS_ID, latestMinorChildObj.getObjectId(context));

					busObjectDataMapForMinor.put(MCADMxUtil.FROM_BUS_ID,revisedBusinessObject.getObjectId(context));
					busObjectDataMapForMinor.put(MCADMxUtil.TO_BUS_ID, childMajorId);
				}				

				busObjectDataMap.put(MCADMxUtil.RELATION_NAME, relName);
				busObjectDataMap.put(MCADMxUtil.IS_REL_FROM_SIDE, String.valueOf(isFrom));
				busObjectDataMap.put(MCADMxUtil.REL_ATTR_TABLE, attrNameVals);

				relIdBusToConnectData.put(relid, busObjectDataMap);

				busObjectDataMapForMinor.put(MCADMxUtil.RELATION_NAME, relName);
				busObjectDataMapForMinor.put(MCADMxUtil.IS_REL_FROM_SIDE, String.valueOf(isFrom));
				busObjectDataMapForMinor.put(MCADMxUtil.REL_ATTR_TABLE, attrNameVals);

				relIdBusToConnectDataForMinor.put(relid, busObjectDataMapForMinor);
			}
		}

		util.connectBusObjectAndHandleRelLogicalID(context, relIdBusToConnectData);

		util.connectBusObjectAndHandleRelLogicalID(context, relIdBusToConnectDataForMinor);

		//copying the Derieved Output Objects
		//Get all relationships connected to "bus" object with type = DependentDocumentLike.
		// [NDM] QWJ
		copyDerivedOutput(context, busName, revisedBusinessObject, revisedMinorObject, busRevision, oldBusObject, globalConfigObj);
			}

	// [NDM] QWJ
	private void copyDerivedOutput(Context context, String busName, BusinessObject majorObject, BusinessObject minorObject, String busRevision, 
			BusinessObject oldBusObject, MCADGlobalConfigObject globalConfigObj) throws Exception
			{
		Hashtable relsAndEnds1	= serverGeneralUtil.getAllWheareUsedRelationships(context, oldBusObject, true, MCADAppletServletProtocol.DERIVEDOUTPUT_LIKE);
		Enumeration allRels		= relsAndEnds1.keys();

		String vault			= minorObject.getVault().toString();

		while(allRels.hasMoreElements())
		{
			Relationship rel			= (Relationship)allRels.nextElement();
			String end					= (String)relsAndEnds1.get(rel);
			BusinessObject busDepDoc	= null;
			rel.open(context);
			// The other object is at the other "end"
			if (end.equals("from"))
			{
				busDepDoc = rel.getTo();
			}
			else
			{
				busDepDoc = rel.getFrom();
			}
			boolean isDerivedOutputLike = globalConfigObj.isTypeOfClass(util.getCADTypeForBO(context, busDepDoc), MCADAppletServletProtocol.TYPE_DERIVEDOUTPUT_LIKE);

			if(isDerivedOutputLike)
			{
				String relationshipName			= rel.getTypeName();
				String relId   = rel.getName();
				StringList selectRelList = new StringList();
				selectRelList.add(MCADAppletServletProtocol.STR_LOGICALID);
				Hashtable relationShipData = serverGeneralUtil.getSelectDataForRelation( context , relId , selectRelList);				

				rel.close(context);
				String  depDocBoName            = busDepDoc.getName();
				String  depTypeName             = busDepDoc.getTypeName();

				BusinessObject depDocObject  = new BusinessObject(depTypeName, depDocBoName , busRevision, vault);

				BusinessObject clonedDepDocBO = null;
				if (depDocObject.exists(context))
				{
					clonedDepDocBO = depDocObject;
				}
				else
				{
					clonedDepDocBO	= busDepDoc.clone(context, null,depDocBoName, busRevision, vault , true, false, true);
				}

				String varNameAttr				= MCADMxUtil.getActualNameForAEFData(context,"attribute_CADObjectName");

				Hashtable attributeValues        = new Hashtable();
				attributeValues.put(varNameAttr, busName);

				util.connectBusObjects(context, minorObject.getObjectId(), clonedDepDocBO.getObjectId(), relationshipName, true, attributeValues , relationShipData);

				if(!util.doesRelationExist(context, majorObject.getObjectId(context), clonedDepDocBO.getObjectId(), relationshipName))
				{
					// Need to check previous version object relationship with Major object
					util.connectBusObjects(context, majorObject.getObjectId(context), clonedDepDocBO.getObjectId(), relationshipName, true, attributeValues , relationShipData);
				}
			}
		}
			}

	private void reviseAllInstances(Context context, MCADGlobalConfigObject globalConfigObj, BusinessObject oldBusObject, 
			BusinessObject connectedFamilyObject, String cadTypeVal) throws MatrixException, MCADException
			{	
		String familyId = "";

		if(globalConfigObj.isTypeOfClass(cadTypeVal, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
			familyId = connectedFamilyObject.getObjectId(context);
		else if(globalConfigObj.isTypeOfClass(cadTypeVal, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
			familyId = oldBusObject.getObjectId(context);

		String [] oids  = new String[1];
		oids[0]  		= familyId;

		Hashtable familyidInstanceStructureTable		= new Hashtable();

		//instance list related to the revised family 
		ArrayList instanceList = serverGeneralUtil.getFamilyStructureRecursively(context, oids,familyidInstanceStructureTable,null);

		for(int i=0; i < instanceList.size(); i++)
		{
			String instanceID = (String) instanceList.get(i);

			BusinessObject instanceBusObject = new BusinessObject(instanceID);

			/*instanceBusObject.open(context);

			String instanceType = instanceBusObject.getTypeName();

			instanceBusObject.close(context);*/

			BusinessObject instanceMajorObject;
			BusinessObject instanceMinorObject;

			if(util.isMajorObject(context, instanceID)) // [NDM] : QWJ
			{
				instanceMajorObject		= instanceBusObject;
				instanceMinorObject		= util.getActiveMinor(context, instanceBusObject);
			}
			else
			{
				instanceMajorObject		= util.getMajorObject(context, instanceBusObject);
				instanceMinorObject		= instanceBusObject;
			}

			instanceMajorObject.open(context);

			String instanceVault	= instanceMajorObject.getVault().toString();
			String instanceMajorId  = instanceMajorObject.getObjectId(context);

			//for taking latest revision of instance to revise
			BusinessObjectList majorRevisionsList 	= instanceMajorObject.getRevisions(context);

			BusinessObject latestinstanceMajorObject = (BusinessObject)majorRevisionsList.lastElement();
			String latestInstanceMajorId = latestinstanceMajorObject.getObjectId();

			BusinessObject latestInstanceMinorObject = util.getActiveMinor(context, latestinstanceMajorObject);

			instanceRevisedMap.put(latestInstanceMajorId, instanceID);
			instanceRevisedMap.put(latestInstanceMinorObject.getObjectId(context), instanceMinorObject.getObjectId(context));

			if(!instanceMajorId.equalsIgnoreCase(latestInstanceMajorId))
			{
				revisedObjectIds.add(instanceMajorId);
				instanceMajorId = latestInstanceMajorId;
			}

			String instanceNewRev   = latestinstanceMajorObject.getNextSequence(context);

			if(!revisedObjectIds.contains(instanceMajorId) )
				latestinstanceMajorObject.revise(context, instanceNewRev, instanceVault);

			instanceMajorObject.close(context);
		}
			}

	private Hashtable getFirstLevelChildrenDetails(Context context, Hashtable relsAndEnds, Hashtable childBusAndRelDetailsTable)
	{
		try
		{
			Enumeration allRels = relsAndEnds.keys();
			while(allRels.hasMoreElements())
			{
				// check "end", this is end for child node
				Relationship reln = (Relationship)allRels.nextElement();
				String end = (String)relsAndEnds.get(reln);

				// add to new list, depending on the "end"!!
				BusinessObject busToAdd		= null;
				reln.open(context);

				String relationshipName		= reln.getTypeName();
				String relId				= reln.getName();

				if(end.equals("from"))
				{
					busToAdd				= reln.getTo();
					end = "to";
				}else{
					end = "from";
					busToAdd				= reln.getFrom();
				}

				reln.close(context);

				busToAdd.open(context);
				String childObjectId		= busToAdd.getObjectId();
				busToAdd.close(context);

				Vector individualChildDetails = new Vector();

				individualChildDetails.addElement(childObjectId);
				individualChildDetails.addElement(relationshipName);
				individualChildDetails.addElement("");
				individualChildDetails.addElement("");
				individualChildDetails.addElement("");
				individualChildDetails.addElement("");
				individualChildDetails.addElement("");
				individualChildDetails.addElement("");
				individualChildDetails.addElement("");
				individualChildDetails.addElement("");
				individualChildDetails.addElement(end);

				childBusAndRelDetailsTable.put(relId, individualChildDetails);
			}
		}
		catch (Exception e)
		{
		}

		return childBusAndRelDetailsTable;

	}

	private String getGlobalConfigObjectName(Context context, String busId) throws Exception
	{
		// Get the IntegrationName
		IEFGuessIntegrationContext_mxJPO guessIntegration = new IEFGuessIntegrationContext_mxJPO(context, null);
		String jpoArgs[] = new String[1];
		jpoArgs[0] = busId;
		String integrationName = guessIntegration.getIntegrationName(context, jpoArgs);

		// Get the relevant GCO Name 

		String gcoName = null;

		IEFSimpleConfigObject simpleLCO = IEFSimpleConfigObject.getSimpleLCO(context);

		String gcoType  = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
		String attrName = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");
		if(simpleLCO.isObjectExists())
		{
			Hashtable integNameGcoMapping = simpleLCO.getAttributeAsHashtable(attrName, "\n", "|");
			gcoName = (String)integNameGcoMapping.get(integrationName);	        
		}
		else
		{
			IEFGetRegistrationDetails_mxJPO registrationDetailsReader = new IEFGetRegistrationDetails_mxJPO(context, null);
			String args[] = new String[1];
			args[0] = integrationName;
			String registrationDetails = registrationDetailsReader.getRegistrationDetails(context, args);
			gcoName 	           = registrationDetails.substring(registrationDetails.lastIndexOf("|")+1);
		}

		return gcoName;
	}

	protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String objectId) throws Exception
	{
		String typeGlobalConfig							= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
		String gcoName									= this.getGlobalConfigObjectName(context, objectId);

		MCADGlobalConfigObject gcoObject	= null;

		if(gcoName != null && gcoName.length() > 0)
		{
			MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(null);
			gcoObject							= configLoader.createGlobalConfigObject(context, util, typeGlobalConfig, gcoName);
		}
		return gcoObject;
	}

	private void copyActiveInstanceRelationships(Context context, MCADGlobalConfigObject globalConfigObj,
			Hashtable childBusAndRelDetailsTable, BusinessObject minorObject) throws MatrixException, MCADException, Exception
			{
		Enumeration childRelationDetails	= childBusAndRelDetailsTable.keys();

		while(childRelationDetails.hasMoreElements())
		{
			String relId  = (String) childRelationDetails.nextElement();

			String relName				= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveInstance");
			String isFromStr			= (String)childBusAndRelDetailsTable.get(relId);	

			Relationship relation = new Relationship(relId);
			relation.open(context);

			BusinessObject childBusObject = relation.getTo();
			boolean isFrom				= true;
			if(isFromStr.equalsIgnoreCase("to"))
			{
				isFrom					= false;
				childBusObject			= relation.getFrom();
			}
			relation.close(context);
			/*childBusObject.open(context,false);

			String childTypeName = childBusObject.getTypeName();

			childBusObject.close(context);*/
			String childObjId = childBusObject.getObjectId(context);

			//get latest instance and connect
			BusinessObject childBusMajorObj = null;
			//if(!globalConfigObj.isMajorType(childTypeName))
			if(!util.isMajorObject(context, childObjId))
			{
				childBusMajorObj = util.getMajorObject(context,childBusObject );
			}
			else
			{
				childBusMajorObj = childBusObject;
			}

			childBusMajorObj.open(context,false);	

			String childMajorId = childBusMajorObj.getObjectId();						
			childBusMajorObj.close(context);

			BusinessObject latestMajorChildObj = null;
			BusinessObject latestMinorChildObj = null;

			if(revisedObjectIds.contains(childMajorId))
			{
				String lastRevisionBusId = util.getLatestRevisionID(context, childMajorId);
				latestMajorChildObj 		= new BusinessObject(lastRevisionBusId);
				String latestMinorChildId = util.getLatestMinorID(context, latestMajorChildObj);
				latestMinorChildObj 		= new BusinessObject(latestMinorChildId);
			}
			else
			{
				latestMajorChildObj = childBusMajorObj;
				latestMinorChildObj = childBusObject;
			}

			Relationship relationship	= null;
			//if(serverGeneralUtil.isBusObjectFinalized(context, latestMinorChildObj))
			relationship	= util.ConnectBusinessObjects(context, minorObject, latestMajorChildObj, relName, isFrom);		
			//else
			//	relationship	= util.ConnectBusinessObjects(context, minorObject, latestMinorChildObj, relName, isFrom);

			util.copyAttributesonRelatinship(context, relation, relationship);
		}
			}
}
