/**
 * MCADSaveAsBase.java
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 */
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Access;
import matrix.db.Relationship;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADFolderUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADSaveAsUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNodeImpl;
import com.matrixone.apps.domain.DomainConstants;

public class MCADSaveAsBase_mxJPO extends IEFCommonUIActions_mxJPO
{
	protected Hashtable sharedPartsIDClonedBusObjectsTable	= new Hashtable();
	protected Hashtable retTable							= new Hashtable();

	// Hashtable cotaining info on old/new names of instances getting cloned
	// Format : key - "<famid>|<oldname>" , value - "<newname>"
	protected Hashtable _instanceOldNewNamesTable			= new Hashtable();
	protected Hashtable _ObjectNewNamesTitleTable			= new Hashtable();
	protected Hashtable _externalReferenceRels				= new Hashtable();
	protected HashSet alreadyExpandedNodesInPath			= new HashSet();
	protected Hashtable _busIDPartIDTable					= new Hashtable();
	protected Hashtable _busIDSpecificationTable			= new Hashtable();
	protected Hashtable _busIDFolderInfoTable				= new Hashtable();
	protected Hashtable	busIDCommonPartTable				= new Hashtable();
	protected Hashtable verticalNavigationProgNameMapping	= null;

	protected boolean isSystemCaseSensitive					= true;
	protected boolean isFamilyTableSaveAs					= false;

	protected IEFEBOMConfigObject _ebomConfigObj			= null;
	protected MCADFolderUtil _folderUtil					= null;
	protected MCADLocalConfigObject _localConfig 			= null;
	private String   integrationName                        = null;
	protected String viewProgram							= null;
	protected Hashtable CADTypeNameVerticalViewTable		= null;
	protected Hashtable CadTypeNameBusIDTable				= null;
	private Hashtable savedAsObjList 				   		= new Hashtable();

	private List processedRelIds 							=  new ArrayList();
	private String mcsURL;
	private String strActiveInstanceRelationship;
	private boolean isObjectAndFileNameDifferent				= false;
	private boolean isParentSavedAs = false;
	
	public  MCADSaveAsBase_mxJPO  () {

	}
	public MCADSaveAsBase_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("not supported no desktop client", null);

	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	// Business Logic for implementong
	protected void canPerformOperationCustom(Context context, Hashtable resultDataTable) throws MCADException
	{
		try
		{
		}
		catch(Exception e)
		{
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.SaveAsNotAllowed") + e.getMessage(), e);
		}
	}

	protected void initialize(Context _context) throws MCADException
	{
		//Get config registry object only if part association on saveas is true
		String confObjTNR = _globalConfig.getEBOMRegistryTNR();
		this.CADTypeNameVerticalViewTable = new Hashtable();
		this.CadTypeNameBusIDTable		= new Hashtable();
		StringTokenizer token = new StringTokenizer(confObjTNR, "|");

		if(token.countTokens() < 3)
		{
			String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.EBOMRegistryNotDefined");
			MCADServerException.createException(errorMessage, null);
		}				

		String confObjType			= (String)token.nextElement();
		String confObjName			= (String)token.nextElement();
		String confObjRev			= (String)token.nextElement();
		_ebomConfigObj				= new IEFEBOMConfigObject(_context, confObjType, confObjName, confObjRev);
		_localConfig				= (MCADLocalConfigObject)_argumentsTable.get(MCADServerSettings.LCO_OBJECT);
		mcsURL                  	= (String)_argumentsTable.get(MCADServerSettings.MCS_URL);

		_folderUtil = new MCADFolderUtil(_context, _serverResourceBundle, _cache);
		String ViewRegistryName 		 = _localConfig.getViewRegistryName((String)_argumentsTable.get(MCADAppletServletProtocol.INTEGRATION_NAME));
		this.verticalNavigationProgNameMapping   = _util.readVerticalNavigationProgNameMapping(_context, ViewRegistryName);
		this.strActiveInstanceRelationship = MCADMxUtil.getActualNameForAEFData(_context,"relationship_ActiveInstance");
		isObjectAndFileNameDifferent = _globalConfig.isObjectAndFileNameDifferent();
	}

	// Entry Point
	public void executeCustom(Context _context, Hashtable resultAndStatusTable)  throws MCADException
	{
		String busObjectsString = (String)_argumentsTable.get(MCADServerSettings.SELECTED_OBJECTID_LIST);
		String objectViewInfo	= (String)_argumentsTable.get(MCADAppletServletProtocol.OBJECT_VIEW_INFO);
		integrationName	= (String)_argumentsTable.get(MCADAppletServletProtocol.INTEGRATION_NAME);
		Hashtable busObjExtendedTable = (Hashtable)_argumentsTable.get("extendedtable");
		
		isSystemCaseSensitive = (Boolean.valueOf((String)_argumentsTable.get(MCADServerSettings.CASE_SENSITIVE_FLAG))).booleanValue();

		viewProgram	= (String)_argumentsTable.get(MCADAppletServletProtocol.VERTICAL_VIEW_PROGRAM);

		initialize(_context);

		//get all the external reference relationships
		_externalReferenceRels = _globalConfig.getRelationshipsOfClass(MCADServerSettings.EXTERNAL_REFERENCE_LIKE);

		BusinessObject clonedObject = null;

		Hashtable busIDSaveAsNameTable		= new Hashtable();
		Hashtable busIDTargetRevTable		= new Hashtable();

		try
		{
			getViewsTable(objectViewInfo);

			getSelectedBusObjectTable(_context, busObjectsString, busIDSaveAsNameTable, busIDTargetRevTable);

			clonedObject = buildStructure(_context, _busObjectID, busIDSaveAsNameTable, busIDTargetRevTable, busObjExtendedTable);

			//alls well, now return the result
			resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT,retTable);

			String ebomSynchObjectIDInfo = getEBOMSynchInfo(_context, _busObjectID, clonedObject);
			resultAndStatusTable.put(MCADServerSettings.OBJECT_ID, ebomSynchObjectIDInfo);

			clonedObject.open(_context);			
			BusinessObject majorBusObj	= _util.getMajorObject(_context,clonedObject);
			String majorObjectId		= "";

			if(majorBusObj != null)
			{
				majorObjectId		= majorBusObj.getObjectId(_context);
			}
			else
			{
				majorObjectId		= clonedObject.getObjectId(_context);
			}

			clonedObject.close(_context);

			resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_STATUS, "true");
			resultAndStatusTable.put(MCADServerSettings.JPO_STATUS_MESSAGE, majorObjectId);
		}
		catch(Throwable exception)
		{			
			MCADServerException.createException(exception.getMessage(), exception);
		}
	}
	/**
	 * @param _context
	 * @param busObjectsString
	 * @param busIDSaveAsNameTable
	 * @param busIDTargetRevTable
	 * @throws Exception
	 * @throws MatrixException
	 */
	private void getSelectedBusObjectTable(Context _context, String busObjectsString, Hashtable busIDSaveAsNameTable, Hashtable busIDTargetRevTable) throws Exception,MatrixException
	{
		StringTokenizer busObjectTokens = new StringTokenizer(busObjectsString, "@");
		while(busObjectTokens.hasMoreTokens())
		{
			String busObjectToken = busObjectTokens.nextToken();

			Enumeration busObjectDetailsElements = MCADUtil.getTokensFromString(busObjectToken, "|");

			if(busObjectDetailsElements.hasMoreElements())
			{
				String busID            = (String)busObjectDetailsElements.nextElement();
				String instanceName     = (String)busObjectDetailsElements.nextElement();
				String busSaveAsName    = (String)busObjectDetailsElements.nextElement();
				String busTargetRev     = (String)busObjectDetailsElements.nextElement();
				String partID			= (String)busObjectDetailsElements.nextElement();
				String encodedSpec		= (String)busObjectDetailsElements.nextElement();
				String folderID			= (String)busObjectDetailsElements.nextElement();
				String applyToChild		= (String)busObjectDetailsElements.nextElement();
				String orgCADType		= (String)busObjectDetailsElements.nextElement();
				String commonPart		= (String)busObjectDetailsElements.nextElement();
				busIDCommonPartTable.put(busID, commonPart);
			

				if(isObjectAndFileNameDifferent)
				{
					String busSaveAsTitle	= (String)busObjectDetailsElements.nextElement();

					if (_ObjectNewNamesTitleTable == null)
					{
						_ObjectNewNamesTitleTable = new java.util.Hashtable();
					}

					if(busSaveAsTitle.equals(""))
					{
						BusinessObject busObj = new BusinessObject(busID);
						busSaveAsTitle = getAutoName(_context, busObj);
						busObj.close(_context);
					}
					_ObjectNewNamesTitleTable.put(busSaveAsName,busSaveAsTitle);
				}

				//putObjectInMajorMinorBusObejctsTable(_context, busID);      //[NDM] : L86

				if (instanceName != null && (instanceName.trim()).length() > 0)
				{
					if (_instanceOldNewNamesTable == null)
					{
						_instanceOldNewNamesTable = new java.util.Hashtable();
					}
					// we have an instance name valid entry, capture the instance details
					String key = getUniqueKeyStringForInstance(busID,instanceName);

					_instanceOldNewNamesTable.put(key,busSaveAsName);

					busIDSaveAsNameTable.put(busID, busSaveAsName);
					BusinessObject obj = new BusinessObject(busID);
					obj.open(_context);
					String orgBusName = obj.getName();
					obj.close(_context);

					busIDTargetRevTable.put(busID, busTargetRev);
				}
				else
				{
					busIDSaveAsNameTable.put(busID, busSaveAsName);
					BusinessObject obj = new BusinessObject(busID);
					obj.open(_context);
					String orgBusName = obj.getName();
					obj.close(_context);

					busIDTargetRevTable.put(busID, busTargetRev);
				}

				if(_globalConfig.isAssignCADModelNameFromPart())
				{
					String specification		= "";
					try
					{			
						specification = MCADUrlUtil.hexDecode(encodedSpec);
					}
					catch(Exception me)
					{							
					}

					if(partID != null && !"".equals(partID))
					{
						_busIDPartIDTable.put(busID, partID);
					}

					if(specification != null && !"".equals(specification))
					{
						_busIDSpecificationTable.put(busID, specification);
					}
				}
				if(folderID != null && !"".equals(folderID))
				{
					Vector folderInfo = new Vector(2);
					folderInfo.add(folderID);
					folderInfo.add(applyToChild);

					_busIDFolderInfoTable.put(busID, folderInfo);
				}
			}
		}
	}
	/**
	 * @param objectViewInfo
	 * @param busIDVerticalViewTable
	 * @param busIDLateralViewTable
	 */
	private void getViewsTable(String objectViewInfo )
	{
		StringTokenizer viewTokens = new StringTokenizer(objectViewInfo,"@");
		while(viewTokens.hasMoreTokens())
		{
			String viewToken = viewTokens.nextToken();

			Enumeration objectViewDetails = MCADUtil.getTokensFromString(viewToken, ",");

			if(objectViewDetails.hasMoreElements())
			{
				String cadTypeName 		   = (String)objectViewDetails.nextElement();
				String busID  				= (String)objectViewDetails.nextElement();
				String appliedVerticalView = (String)objectViewDetails.nextElement();

				CADTypeNameVerticalViewTable.put(cadTypeName.toString(), appliedVerticalView);
				CadTypeNameBusIDTable.put(cadTypeName.toString(),busID);
			}
		}
	}

	protected String getUniqueKeyStringForInstance(String id, String instName)
	{
		StringBuffer key = new StringBuffer(id);
		key.append("|");
		key.append(instName);

		return key.toString();
	}

	/**
	 * This method is for adding the newly creates instance through saveas to the instance
	 * structure for the family.This new instance is to be added at the same level in the
	 * structure as the instance from which it is cloned
	 * @param instStructureNode - instance structure
	 * @param originalInstanceCadId - cadid of the instance node from which the new instance
	 *                                was cloned
	 * @param newInstNodeForInstStructure - new instance node to be added to the structure
	 * @throws Exception
	 */
	protected void addNewInstanceToInstanceStructure(IEFXmlNode instStructureNode,String originalInstanceCadId,
			IEFXmlNode newInstNodeForInstStructure)throws Exception
			{
		IEFXmlNodeImpl parentForClonedInstance = null;
		String cadID = instStructureNode.getAttribute("cadid");

		if(originalInstanceCadId.equalsIgnoreCase(cadID))
		{
			parentForClonedInstance = (IEFXmlNodeImpl)instStructureNode.getParentNode();
			parentForClonedInstance.addNode((IEFXmlNodeImpl)newInstNodeForInstStructure);
		}
		else
		{
			Enumeration childElements = instStructureNode.getChildrenByName("cadobject");
			while(childElements.hasMoreElements() && parentForClonedInstance == null)
			{
				IEFXmlNode structChildNode = (IEFXmlNode)childElements.nextElement();

				addNewInstanceToInstanceStructure(structChildNode, originalInstanceCadId,newInstNodeForInstStructure);
			}
		}
			}


	//[NDM] : L86 Start     (method is commented since major and minor types are same in NDM and no finalisation concept in NDM)
	/*protected void putObjectInMajorMinorBusObejctsTable(Context _context, String busObjectID) throws Exception
	{
		BusinessObject busObject = new BusinessObject(busObjectID);
		busObject.open(_context);
		String sType	= busObject.getTypeName();
		busObject.close(_context);

		if(_globalConfig.isMinorType(sType) && _generalUtil.isBusObjectFinalized(_context, busObject))
		{
			BusinessObject minorObject          =  new BusinessObject(busObjectID);
			BusinessObject majorObj             =  _util.getMajorObject(_context, minorObject);
			String majorObjectID                =  majorObj.getObjectId(_context);
			majorMinorBusObejctsTable.put(busObjectID,majorObjectID);
		}
	}*/
	//[NDM] : L86 Start

	protected BusinessObject buildStructure(Context _context, String rootBusID, Hashtable busIDSaveAsNameTable, Hashtable busIDTargetRevTable, Hashtable busObjExtendedTable ) throws Exception
	{
		// Check for family or instance object
		BusinessObject busObject		= new BusinessObject(rootBusID);

		busObject.open(_context);   

		String oldObjectName			= busObject.getName();
		String cadType					= _util.getCADTypeForBO(_context, busObject);
		String cadTypeName				= cadType + "|" + oldObjectName;
		busObject.close(_context);

		BusinessObject clonedBusObject	= null;
	
		boolean isCloned				= busIDSaveAsNameTable.containsKey(rootBusID);

		if (_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
		{
			clonedBusObject = buildFamilyStructure(_context, rootBusID, busIDSaveAsNameTable, busIDTargetRevTable, busObjExtendedTable, clonedBusObject);
			}
		else if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
				{
			clonedBusObject = buildInstanceStructure(_context, busIDSaveAsNameTable, busIDTargetRevTable, busObjExtendedTable,
					rootBusID, busObject, clonedBusObject, cadTypeName, isCloned);
					}
		else
		{
			clonedBusObject = buildChildNode(_context, rootBusID, busIDSaveAsNameTable, busIDTargetRevTable, busObjExtendedTable,  true);
				}

		if(isCloned)
		{
			renameIndividual(_context, clonedBusObject, rootBusID, false, false);
			}

		return clonedBusObject;
		}

	private BusinessObject buildInstanceStructure(Context _context,Hashtable busIDSaveAsNameTable,Hashtable busIDTargetRevTable,Hashtable busObjExtendedTable,
			String rootBusID,BusinessObject busObject,BusinessObject clonedBusObject,String cadTypeName,boolean isCloned) throws Exception {

			if (isCloned)
			{
				String busSaveAsName = (String)busIDSaveAsNameTable.get(rootBusID);
				String busTargetRev  = (String)busIDTargetRevTable.get(rootBusID);
				String famObjID		 = _generalUtil.getTopLevelFamilyObjectForInstance(_context, rootBusID);

				BusinessObject famObj = new BusinessObject(famObjID);
				famObj.open(_context);
			String familyName = "";

			if(MCADSaveAsUtil.isSavedAs(rootBusID, savedAsObjList))
			{
				return MCADSaveAsUtil.getSavedAsObj(_context, rootBusID, savedAsObjList);
			}

			familyName = (String)busIDSaveAsNameTable.get(famObjID);

			if(null==familyName)
			{
				familyName = famObj.getName();
			}
			validateInstance(_context, famObj,busIDSaveAsNameTable);

				famObj.close(_context);
				
			clonedBusObject = saveAsInstanceObject(_context, rootBusID, busSaveAsName, busTargetRev, famObjID,familyName,busIDSaveAsNameTable);

			MCADSaveAsUtil.addSavedAsObjList(rootBusID, clonedBusObject, savedAsObjList);

				sharedPartsIDClonedBusObjectsTable.put(rootBusID, clonedBusObject);
				buildInstance(_context, clonedBusObject,busObject,busIDSaveAsNameTable,busIDTargetRevTable, busObjExtendedTable);
				createTowerForSavesAsInstance(_context, busObject,clonedBusObject,busTargetRev,busIDSaveAsNameTable);

			}
			else
			{				
				clonedBusObject = buildChildNode(_context, rootBusID, busIDSaveAsNameTable, busIDTargetRevTable, busObjExtendedTable , true);
			}
		applyVerticalViewOnObject(_context, rootBusID, busIDSaveAsNameTable,busIDTargetRevTable, clonedBusObject, cadTypeName);

		connectPartWithCADObject( _context, rootBusID, clonedBusObject);
			//set Parent Instance attribute as "Blank" on Instance Of relationship if parent instance is cloned
			String renamedObjID		 = clonedBusObject.getObjectId(_context);
			String instanceOf		 = MCADMxUtil.getActualNameForAEFData(_context, "relationship_InstanceOf");
			String parentInstance	 = MCADMxUtil.getActualNameForAEFData(_context, "attribute_ParentInstance");
		String renamedFrom	     = MCADMxUtil.getActualNameForAEFData(_context, "attribute_RenamedFrom");
			String [] oids			 = new String[2];
			oids[0]					 = renamedObjID;
			oids[1]					 = rootBusID;

		String selectOnInstOf	 = "to[" + instanceOf + "].";

			StringList busSelectList = new StringList(2);

		String selectInstOfRelId   	   = selectOnInstOf + "id";
		String selectInstOfRelAttr 	   = selectOnInstOf + "attribute[" + parentInstance + "]";
		String selectInstOfAttrRnmFrm  = "";
		String selectInstOfName  	   = "";

		selectInstOfAttrRnmFrm         = selectOnInstOf + "from.attribute[" + renamedFrom + "]";
		selectInstOfName  	  		   = selectOnInstOf + "from.name";

		busSelectList.add(selectInstOfAttrRnmFrm);
		busSelectList.add(selectInstOfName);

			busSelectList.add(selectInstOfRelId);
			busSelectList.add(selectInstOfRelAttr);

			BusinessObjectWithSelectList busWithSelectList		  = BusinessObjectWithSelect.getSelectBusinessObjectData(_context, oids, busSelectList);
			BusinessObjectWithSelect busWithSelectOnRenamedObject = busWithSelectList.getElement(0);
			BusinessObjectWithSelect busWithSelectOnRootObject    = busWithSelectList.getElement(1);

			String clonedRelId									  = (String)busWithSelectOnRenamedObject.getSelectData(selectInstOfRelId);
			String clonedRelAttr								  = (String)busWithSelectOnRenamedObject.getSelectData(selectInstOfRelAttr);
			String OriginalRelAttr								  = (String)busWithSelectOnRootObject.getSelectData(selectInstOfRelAttr);

			if(!clonedRelAttr.equals(OriginalRelAttr))
			{
				Relationship instRel							= new Relationship(clonedRelId);

				_util.setRelationshipAttributeValue(_context, instRel, parentInstance, OriginalRelAttr);
			}

		StringList attrRenFromList							  = busWithSelectOnRenamedObject.getSelectDataList(selectInstOfAttrRnmFrm);
		StringList attrInstNamesList					      = busWithSelectOnRenamedObject.getSelectDataList(selectInstOfName);

		if(attrRenFromList!=null)
		{
			for(int b=0 ; b < attrRenFromList.size(); b++)
			{
				String attrRenamedFrom    = (String)attrRenFromList.elementAt(b);
				String newName		      = (String)attrInstNamesList.elementAt(b);

				if(!"".equals(attrRenamedFrom))
				{
					Relationship nestedInstRel = new Relationship(clonedRelId);
					String indivInstName	   = _generalUtil.getIndivisualInstanceName(newName);
					_util.setRelationshipAttributeValue(_context, nestedInstRel, parentInstance, indivInstName);
				}
			}
		}

		MCADSaveAsUtil.addSavedAsObjList(rootBusID, clonedBusObject, savedAsObjList);

		return clonedBusObject;
	}

	private BusinessObject buildFamilyStructure(Context _context,String rootBusID,Hashtable busIDSaveAsNameTable
			,Hashtable busIDTargetRevTable,Hashtable  busObjExtendedTable,BusinessObject clonedBusObject) throws Exception {
		//go separate way for family
		clonedBusObject = buildChildNodeForFamily(_context, rootBusID, busIDSaveAsNameTable, busIDTargetRevTable, busObjExtendedTable, true);

		//Settting bareboard names attribute value blank.
		String attrBareboardName = MCADMxUtil.getActualNameForAEFData(_context,"attribute_BareboardNames");

		if(_util.hasAttributeForBO(_context, rootBusID, attrBareboardName))
		{
			_util.setAttributeValue(_context, clonedBusObject, attrBareboardName, "");
		}

		//set Parent Instance attribute on Instance Of relationship for nested instance if whole family structure is cloned.
		String renamedFamObjID   = clonedBusObject.getObjectId(_context);
		String instanceOf	     = MCADMxUtil.getActualNameForAEFData(_context, "relationship_InstanceOf");
		String parentInstance    = MCADMxUtil.getActualNameForAEFData(_context, "attribute_ParentInstance");
		String renamedFrom	     = MCADMxUtil.getActualNameForAEFData(_context, "attribute_RenamedFrom");
		String [] oids			 = new String[1];
		oids[0]					 = renamedFamObjID;
		StringList busSelectList = new StringList(5);
		String selectOnInstOfRel = "from[" + instanceOf + "].";

		busSelectList.add(selectOnInstOfRel + "to.attribute[" + renamedFrom + "]");
		busSelectList.add(selectOnInstOfRel + "id");
		busSelectList.add(selectOnInstOfRel + "attribute[" + parentInstance + "]");
		busSelectList.add(selectOnInstOfRel + "to.name");
		BusinessObjectWithSelectList busWithSelectList	= BusinessObjectWithSelect.getSelectBusinessObjectData(_context, oids, busSelectList);
		BusinessObjectWithSelect busWithSelect			= busWithSelectList.getElement(0);
		StringList attrRenFromList						= busWithSelect.getSelectDataList(selectOnInstOfRel + "to.attribute[" + renamedFrom + "]");
		StringList relIdList							= (StringList)busWithSelect.getSelectDataList(selectOnInstOfRel + "id");
		StringList attrParInstList						= (StringList)busWithSelect.getSelectDataList(selectOnInstOfRel + "attribute[" + parentInstance + "]");
		StringList attrInstNamesList					= (StringList)busWithSelect.getSelectDataList(selectOnInstOfRel + "to.name");

		if(attrRenFromList!=null)
		{
			for(int b=0 ; b < attrRenFromList.size(); b++)
			{
				String attrRenamedFrom    = (String)attrRenFromList.elementAt(b);
				String newName		      = (String)attrInstNamesList.elementAt(b);

				for(int c=0 ; c < attrParInstList.size(); c++)
				{
					String attrParentInst = (String)attrParInstList.elementAt(c);
					if(!"".equals(attrRenamedFrom) && attrParentInst.equals(attrRenamedFrom))
					{
						String relId			   = (String)relIdList.elementAt(c);
						Relationship nestedInstRel = new Relationship(relId);
						String indivInstName	   = _generalUtil.getIndivisualInstanceName(newName);
						_util.setRelationshipAttributeValue(_context, nestedInstRel, parentInstance, indivInstName);
					}
				}
			}
		}
		return clonedBusObject;
	}

	private BusinessObject buildChildNode(Context _context, String busID, Hashtable busIDSaveAsNameTable, Hashtable busIDTargetRevTable, Hashtable busObjExtendedTable,  boolean isFirstLevel) throws Exception
	{
		BusinessObject clonedBusObject  = null;
		boolean isCloned                = false;
		boolean isShared                = false;
		
		BusinessObject busObject = new BusinessObject(busID);
		busObject.open(_context);
		String cadType = _util.getCADTypeForBO(_context, busObject);
		String busName = busObject.getName();
		String cadTypeAndName= cadType+"|"+busName;

		busObject.close(_context);

		if(MCADSaveAsUtil.isSavedAs(busID, savedAsObjList)){
			BusinessObject savedAsObj = MCADSaveAsUtil.getSavedAsObj(_context, busID, savedAsObjList);

			boolean isChildSelectedForSaveAs = busIDSaveAsNameTable.containsKey(busID);
			boolean isChildShared = sharedPartsIDClonedBusObjectsTable.containsKey(busID);

			if(isChildSelectedForSaveAs && _globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
			{
				renameIndividual(_context, savedAsObj, busID,isChildShared, false);
			}
			return 	savedAsObj;
		}

		boolean isSavedAs = busIDSaveAsNameTable.containsKey(busID);

		if(isSavedAs && !MCADSaveAsUtil.isSavedAs(busID, savedAsObjList))
		{
			if(sharedPartsIDClonedBusObjectsTable.containsKey(busID))
			{
				clonedBusObject = (BusinessObject)sharedPartsIDClonedBusObjectsTable.get(busID);
				isShared = true;
			}
			else
			{
				if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
				{
					String busSaveAsName  = (String)busIDSaveAsNameTable.get(busID);
					String busTargetRev   = (String)busIDTargetRevTable.get(busID);
					BusinessObject famObj = _generalUtil.getFamilyObjectForInstance(_context, busObject);

					validateInstance(_context, famObj,busIDSaveAsNameTable);

					String familyName	= "";

					familyName	= (String)busIDSaveAsNameTable.get(famObj.getObjectId(_context));
					if(null==familyName){
						familyName = famObj.getName();
					}
					
					clonedBusObject = buildInstanceStructure(_context, busIDSaveAsNameTable, busIDTargetRevTable, busObjExtendedTable,
							busID, busObject, null, cadType, true);
					
				}
				else if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
				{
					clonedBusObject = buildFamilyStructure(_context, busID,
							busIDSaveAsNameTable, busIDTargetRevTable, busObjExtendedTable, busObject);
				}
				else
				{
					clonedBusObject = saveAsBusinessObject(_context, busID, busIDSaveAsNameTable, busIDTargetRevTable);
				}

				MCADSaveAsUtil.addSavedAsObjList(busID, clonedBusObject, savedAsObjList);
				sharedPartsIDClonedBusObjectsTable.put(busID, clonedBusObject);
				String commonPart = "false";
				if(busIDCommonPartTable != null && busIDCommonPartTable.size() > 0)
				{
					 commonPart = (String)busIDCommonPartTable.get(busID);
				}
				if(commonPart.equalsIgnoreCase("true"))
				{
					isCloned = false;
				}
				else
				{
				isCloned = true;
			}
			}

			//save related documents as well, if related document jpo name is not blank in GCO
			applyVerticalViewOnObject(_context, busID, busIDSaveAsNameTable,busIDTargetRevTable, clonedBusObject, cadTypeAndName);

			connectPartWithCADObject(_context, busID, clonedBusObject);
			// Check whether user aborted the operation, in that case throw error
			if(isOperationCancelled())
			{
				String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.UserCancelledTheOperation");
				MCADServerException.createException(errorMessage, null);
			}
			// Update metadata count, for progress bar update
			incrementMetaCurrentCount();
		}
		else
		{
			clonedBusObject = busObject;

			//save related documents as well, if related document jpo name is not blank in GCO
			applyVerticalViewOnObject(_context, busID, busIDSaveAsNameTable,busIDTargetRevTable, clonedBusObject, cadTypeAndName);

			// Check whether user aborted the operation, in that case throw error
			if(isOperationCancelled())
			{
				String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.UserCancelledTheOperation");
				MCADServerException.createException(errorMessage, null);
			}
			// Update metadata count, for progress bar update
			incrementMetaCurrentCount();
		}

		Hashtable childBusAndRelIds 	= getChildRelationshipIDsForAssembly(_context, busID , busObjExtendedTable);
		Enumeration childBusIds 		= childBusAndRelIds.keys();
		// Get all the relatinships of it's children, i.e. dependees

		// Go for children rel update only if NOT a shared guy
		if(!isShared && childBusIds.hasMoreElements())
		{
			//System.out.println("Get children of this bus to saveas " + busName);
			Hashtable extrenalRefParentRevIDsCurrIDTable = new Hashtable();
			Hashtable childBusAndRelDetailsTable		 = new Hashtable();
			Vector extrnlRefConflictTestList			 = new Vector();
			while(childBusIds.hasMoreElements())
			{
				String relId			 = (String)childBusIds.nextElement();
				Vector indvChildDetails  = (Vector)childBusAndRelIds.get(relId);

				String childBusID        = (String)indvChildDetails.elementAt(0);
				String relName           = (String)indvChildDetails.elementAt(1);

				if(_globalConfig.isRelationshipOfClass(relName, MCADServerSettings.EXTERNAL_REFERENCE_LIKE))
					extrnlRefConflictTestList.addElement(childBusID);
				childBusAndRelDetailsTable.put(childBusID, "true");
			}
			//FIXME: Remove callingPage. It's just a temporary fix. Changes to be done in the HashTable childBusAndRelDetailsTable
			childBusAndRelDetailsTable.put("callingPage", "MCADSaveAs");
			_generalUtil.storeConflictingParentIDs(_context, busID, extrnlRefConflictTestList, extrenalRefParentRevIDsCurrIDTable);
			_generalUtil.resolveExternalRefConflict(_context, childBusAndRelDetailsTable, extrnlRefConflictTestList, alreadyExpandedNodesInPath, null);

			Enumeration childObjIds 		= childBusAndRelIds.keys();
			while(childObjIds.hasMoreElements())
			{
				String isFromStr				= "";
				String relId			 = (String)childObjIds.nextElement();
				Relationship rel		 = new Relationship(relId);
				Vector indvChildDetails  = (Vector)childBusAndRelIds.get(relId);

				String childBusID        = (String)indvChildDetails.elementAt(0);
				String relName           = (String)indvChildDetails.elementAt(1);
				isFromStr				 = (String)indvChildDetails.elementAt(10);	

				boolean isFrom					= false;
				if(isFromStr.equalsIgnoreCase("to"))
					isFrom = true;

				BusinessObject childObj = new BusinessObject(childBusID);
				childObj.open(_context);
				String childBusName = childObj.getName();
				String childCADType = _util.getCADTypeForBO(_context, childObj);
				childObj.close(_context);

				boolean shouldAddBus = true;
				if(_globalConfig.isRelationshipOfClass(relName, MCADServerSettings.EXTERNAL_REFERENCE_LIKE))
				{
					Collection ids = childBusAndRelDetailsTable.keySet();
					if(!ids.contains(childBusID))
						shouldAddBus = false;
				}

				boolean isParentSelectedForSaveAs = isCloned;
				//applying lateral view on child node.
				StringBuffer cadTypeName		= new StringBuffer(childCADType);
				cadTypeName.append("|");
				cadTypeName.append(childBusName);


				if(CadTypeNameBusIDTable.containsKey(cadTypeName.toString()))
					childBusID = (String)CadTypeNameBusIDTable.get(cadTypeName.toString());			

				boolean isChildSelectedForSaveAs = busIDSaveAsNameTable.containsKey(childBusID);

				boolean isChildShared = sharedPartsIDClonedBusObjectsTable.containsKey(childBusID);

				BusinessObject clonedChildBusObject = null;
				if(!alreadyExpandedNodesInPath.contains(busID) && shouldAddBus)
				{
					alreadyExpandedNodesInPath.add(busID);
					BusinessObject childBusObj = new BusinessObject(childBusID);
					childBusObj.open(_context);
					String cadTypeForChild = _util.getCADTypeForBO(_context, childBusObj);

					if(_globalConfig.isTypeOfClass(cadTypeForChild, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
					{					
						clonedChildBusObject = buildInstanceStructure(_context, busIDSaveAsNameTable, busIDTargetRevTable, busObjExtendedTable,
								childBusID, childBusObj, clonedChildBusObject, cadTypeName.toString(), isCloned);
					}else{
						clonedChildBusObject = buildChildNode(_context, childBusID, busIDSaveAsNameTable, busIDTargetRevTable ,busObjExtendedTable,  false);
					}

					//[NDM] : L86 start
					String clonedBusMinorObjectId		= _util.getActiveVersionObject(_context, clonedBusObject.getObjectId(_context));
					String clonedChildBusMinorObjectID			= _util.getActiveVersionObject(_context, clonedChildBusObject.getObjectId(_context));
					
					BusinessObject clonedBusMinorObject = new BusinessObject(clonedBusMinorObjectId);
					BusinessObject clonedChildBusMinorObject = new BusinessObject(clonedChildBusMinorObjectID);
					//[NDM] : L86 end

					if(isParentSelectedForSaveAs && !MCADSaveAsUtil.isConnectedWith(relId, "", processedRelIds))

					{		
						Relationship relationship = _util.ConnectBusinessObjects(_context, clonedBusObject, clonedChildBusObject, relName, isFrom);
						// Copy attributes on new relationship	                         				
                                                            _util.copyAttributesonRelatinship(_context, rel, relationship);						
						MCADSaveAsUtil.addConnectedObjList(clonedChildBusObject.getObjectId(_context), clonedBusObject.getObjectId(_context), relId, relationship.getName(), savedAsObjList, processedRelIds);
						

						//[NDM] : L86 start
						Relationship relBetMinors = _util.ConnectBusinessObjects(_context, clonedBusMinorObject, clonedChildBusMinorObject, relName, isFrom);
						_util.copyAttributesonRelatinship(_context, rel, relBetMinors);	
				        MCADSaveAsUtil.addConnectedObjList(clonedBusMinorObjectId, clonedChildBusMinorObjectID, rel.getName(), relBetMinors.getName(), savedAsObjList, processedRelIds);
				      //[NDM] : L86 end
					}
					}

					if(isChildSelectedForSaveAs)
					{
					renameIndividual(_context, clonedChildBusObject, childBusID,isChildShared, false);
					}

					alreadyExpandedNodesInPath.remove(busID);
				}
			}

		// Get the family class objects upated for instance details
		if (isCloned)
		{
			if(_util.doesObjectHaveInstances(_context, busObject))
			{
				Hashtable instanceInfo = new Hashtable();
				String clonedObjName = clonedBusObject.getName();

				// Hashtable containing old name/new name pairs for family & instances
				Hashtable changedNameTable = new Hashtable();
				String newInstNameAndDepDocName = clonedObjName + "|" + clonedObjName;
				changedNameTable.put(busName, newInstNameAndDepDocName);
				boolean bInstanceUpdated = getUpdatedInstanceInfoTable(_context, busID,busObject,instanceInfo,null,
						changedNameTable,clonedObjName);
				// update files & rels only if one of the instances has also changed
				if(bInstanceUpdated)
				{
					updateInstanceFilesAndRelsForSaveAs(_context, instanceInfo,null,null,clonedBusObject);
				}

				saveDependentDocs(_context, busID, clonedBusObject, changedNameTable);
			}
			else
			{
				//save the dependent docs (if any)
				saveDependentDocs(_context, busID, clonedBusObject, null);
			}
		}
		MCADSaveAsUtil.addSavedAsObjList(busID, clonedBusObject, savedAsObjList);
		return clonedBusObject;

	}
	/**
	 * @param _context
	 * @param busID
	 * @param busIDSaveAsNameTable
	 * @param busIDTargetRevTable
	 * @param clonedBusObject
	 * @param cadTypeAndName
	 * @throws MCADException
	 */
	private boolean applyVerticalViewOnObject(Context _context, String busID,Hashtable busIDSaveAsNameTable, Hashtable busIDTargetRevTable,BusinessObject clonedBusObject, String cadTypeAndName) throws MCADException 
	{
		boolean isViewApplied   = false;
		String verticleViewName	= (String)CADTypeNameVerticalViewTable.get(cadTypeAndName);

		if(null != verticleViewName && !"".equals(verticleViewName) && !verticleViewName.equals(MCADAppletServletProtocol.VIEW_NONE))
		{
			isViewApplied = true;

			Vector viewsAppliedOnObject 		 = MCADUtil.getVectorFromString(verticleViewName, "|");
			Enumeration viewsAppliedOnObjectEnum = viewsAppliedOnObject.elements();
			while(viewsAppliedOnObjectEnum.hasMoreElements())
			{
				String nextVerticalView = (String)viewsAppliedOnObjectEnum.nextElement();
				if(null != nextVerticalView && !"".equals(nextVerticalView) && !nextVerticalView.equals(MCADAppletServletProtocol.VIEW_NONE))
				{
					Vector relatedDocProgDetails = (Vector)verticalNavigationProgNameMapping.get(nextVerticalView);
					if(relatedDocProgDetails != null)
					{
						String relatedDocJPOName = (String)relatedDocProgDetails.elementAt(0);
						saveRelatedDocumentForObject(_context, busID, clonedBusObject, busIDSaveAsNameTable, busIDTargetRevTable, relatedDocJPOName, "");
					}
				}
			}

			CADTypeNameVerticalViewTable.put(cadTypeAndName, MCADAppletServletProtocol.VIEW_NONE);
		}
		return isViewApplied;
	}

	private Hashtable getChildRelationshipIDsForAssembly(Context _context, String busID, Hashtable busIDDependentRelIDsTable) throws Exception
	{
		Hashtable childBusAndRelDetailsTable = (Hashtable)busIDDependentRelIDsTable.get(busID);

		if(childBusAndRelDetailsTable == null)
		{
			childBusAndRelDetailsTable = new Hashtable();

			BusinessObject busObject = new BusinessObject(busID);

			busObject.open(_context);

			Hashtable relsAndEnds =  _generalUtil.getAllWheareUsedRelationships(_context, busObject, true, MCADAppletServletProtocol.ASSEMBLY_LIKE);

			if(relsAndEnds.size() > 0)
			{
				Enumeration allRels = relsAndEnds.keys();

				while(allRels.hasMoreElements())
				{
					String isFromStr = "from";

					Relationship rel = (Relationship)allRels.nextElement();
					String end = (String)relsAndEnds.get(rel);

					// add to new list, depending on the "end"!!
					BusinessObject busToAdd = null;

					rel.open(_context);

					String relationshipName = rel.getTypeName();
					String relId			= rel.getName();

					// The other object is at the other "end"
					if (end.equals("from"))
					{
						busToAdd  = rel.getTo();
						isFromStr = "to";
					}
					else
						busToAdd = rel.getFrom();

					rel.close(_context);

					busToAdd.open(_context);
					String childObjectId = busToAdd.getObjectId();
					busToAdd.close(_context);

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
					individualChildDetails.addElement(isFromStr);

					childBusAndRelDetailsTable.put(relId, individualChildDetails);
				}
			}

			busObject.close(_context);
		}

		return childBusAndRelDetailsTable;
	}

	protected void saveRelatedDocumentForObject(Context _context, String originalBusID, BusinessObject clonedBusObj, Hashtable busIDSaveAsNameTable,
			Hashtable busIDTargetRevTable, String relatedDocumentJPOName, String newInstanceName) throws MCADException
			{
		try
		{
			//execute the JPO given by relatedDOcumentJPO and get the related documents
			String[] args = new String[3];
			args[0] = originalBusID;
			args[1] = "";
			args[2] = "false";

			Hashtable argsTable = new Hashtable();
			argsTable.put("GCO",_globalConfig);
			argsTable.put("language",_serverResourceBundle.getLanguageName());
			String []initArgs = JPO.packArgs(argsTable);

			IEFXmlNode viewDetailsNode = (IEFXmlNode)JPO.invoke(_context, relatedDocumentJPOName, initArgs, "getVerticalViewBOIDs", args, IEFXmlNode.class);

			if(viewDetailsNode != null )
			{
				if(!clonedBusObj.isOpen())
				{
					clonedBusObj.open(_context);
				}

				Enumeration viewNodes = viewDetailsNode.getChildrenByName("node");
				while (viewNodes.hasMoreElements())
				{
					IEFXmlNode viewNode = (IEFXmlNode)viewNodes.nextElement();
					String relID = (String) viewNode.getAttribute("relid");
					String relatedDocumentObjID = (String) viewNode.getAttribute("busid");

					BusinessObject relatedDocumentObj = new BusinessObject(relatedDocumentObjID);
					relatedDocumentObj.open(_context);
					//if related document is already saved skip it.
					if(sharedPartsIDClonedBusObjectsTable.containsKey(relatedDocumentObjID))
					{
						BusinessObject clonedRelatedBusObj = (BusinessObject)sharedPartsIDClonedBusObjectsTable.get(relatedDocumentObjID);
						if(MCADSaveAsUtil.isConnectedWith(relID, "", processedRelIds))
						{
							continue;
						}
						connectBusinessObject(_context, relID, originalBusID, clonedBusObj,clonedRelatedBusObj , newInstanceName);

						MCADSaveAsUtil.addConnectedObjList(clonedRelatedBusObj.getObjectId(_context), clonedBusObj.getObjectId(_context), relID, "", savedAsObjList, processedRelIds);
						boolean isFrom = false;
						Vector relNames = _util.getRelationNamesBetweenObjects(_context, clonedRelatedBusObj.getObjectId(), originalBusID);

						for(int j=0;j<relNames.size();j++)
						{
							_util.disconnectBusObjects(_context, clonedRelatedBusObj.getObjectId(), originalBusID, (String)relNames.get(j), isFrom);
						}

						continue;
					}

					String oldRelatedDocumentObjName = relatedDocumentObj.getName();
					String oldRelatedDocumentObjCADType = _util.getCADTypeForBO(_context, relatedDocumentObj);
					String oldRelatedDocumentObjCADTypeName = oldRelatedDocumentObjCADType + "|" + oldRelatedDocumentObjName;
					relatedDocumentObj.close(_context);

					BusinessObject clonedRelatedDocumentObj = null;

					if(busIDSaveAsNameTable.containsKey(relatedDocumentObjID))
					{
						//related document is also selected for SaveAs, therefore save it as well
						clonedRelatedDocumentObj = saveAsBusinessObject(_context, relatedDocumentObjID, busIDSaveAsNameTable, busIDTargetRevTable);

						//store the cloned related document object in the hashtable
						sharedPartsIDClonedBusObjectsTable.put(relatedDocumentObjID, clonedRelatedDocumentObj);

						connectPartWithCADObject(_context, relatedDocumentObjID, clonedRelatedDocumentObj);

						//rename this cloned object
						renameIndividual(_context, clonedRelatedDocumentObj, relatedDocumentObjID, false, false);

						if(MCADSaveAsUtil.isConnectedWith(relID, "", processedRelIds))
						{
							continue;
						}
						connectBusinessObject(_context, relID, originalBusID, clonedBusObj, clonedRelatedDocumentObj, newInstanceName);

						MCADSaveAsUtil.addConnectedObjList(clonedRelatedDocumentObj.getObjectId(_context), clonedBusObj.getObjectId(_context), relID, "", savedAsObjList, processedRelIds);			
						Hashtable relationsList = _generalUtil.getAllWheareUsedRelationships(_context, relatedDocumentObj,true,MCADAppletServletProtocol.ASSEMBLY_LIKE);
						Enumeration assemblyLikeRelList = relationsList.keys();

						while(assemblyLikeRelList.hasMoreElements())
						{
							Relationship rel = (Relationship)assemblyLikeRelList.nextElement();
							String relDirection=(String)relationsList.get(rel);
							rel.open(_context);
							String relName = rel.getTypeName();
							if(relDirection.equalsIgnoreCase("to")){
							   BusinessObject busObj = rel.getFrom();
							   String busId = busObj.getObjectId(_context);
							
							   if(!originalBusID.equals(busId) && !MCADSaveAsUtil.isConnectedWith(rel.getName(),"", processedRelIds))
							   {
								boolean isFrom = false;
								Relationship relationship = _util.ConnectBusinessObjects(_context, clonedRelatedDocumentObj, busObj, relName, isFrom);

								_util.copyAttributesonRelatinship(_context, rel, relationship);	
								MCADSaveAsUtil.addConnectedObjList(busObj.getObjectId(_context), clonedRelatedDocumentObj.getObjectId(_context), rel.getName(), relationship.getName(), savedAsObjList, processedRelIds);

							   }
	                                                }else{
							    BusinessObject busObj = rel.getTo();
						            String busId = busObj.getObjectId(_context);
							    if(!originalBusID.equals(busId) && !MCADSaveAsUtil.isConnectedWith(rel.getName(),"", processedRelIds))
							    {
								boolean isFrom = true;
								Relationship relationship = _util.ConnectBusinessObjects(_context, clonedRelatedDocumentObj, busObj, relName, isFrom);
                                                                _util.copyAttributesonRelatinship(_context, rel, relationship);
                                                                MCADSaveAsUtil.addConnectedObjList(busObj.getObjectId(_context), clonedRelatedDocumentObj.getObjectId(_context), rel.getName(), relationship.getName(), savedAsObjList, processedRelIds);
							    }
							}
						}
					}
					else
					{
						clonedRelatedDocumentObj	= relatedDocumentObj;
					}

					applyVerticalViewOnObject(_context, relatedDocumentObjID, busIDSaveAsNameTable, busIDTargetRevTable, clonedRelatedDocumentObj, oldRelatedDocumentObjCADTypeName);
				}

				clonedBusObj.close(_context);
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
			}

	protected void connectBusinessObject(Context _context, String relID, String originalBusID, 
			BusinessObject clonedBusObj, BusinessObject clonedRelatedDocumentObj, String newInstanceName) throws MCADException, MatrixException
			{

		boolean isFrom = false;

		Relationship relationship = new Relationship(relID);
		relationship.open(_context);

		String relationshipName = relationship.getTypeName();
		AttributeList relAttrList = relationship.getAttributeValues(_context, true);

		String toObjID = relationship.getTo().getObjectId();
		if(!toObjID.equals(originalBusID))
		{
			isFrom = true;
		}

		relationship.close(_context);

		//now connect the two objects
		Relationship newRelationship = _util.ConnectBusinessObjects(_context, clonedBusObj, clonedRelatedDocumentObj, relationshipName, isFrom);

		if(newInstanceName != null && newInstanceName.length() > 0)
		{
			setAttrValInAttrList(relAttrList, MCADMxUtil.getActualNameForAEFData(_context, "attribute_ChildInstance"), newInstanceName);
		}

		newRelationship.setAttributeValues(_context, relAttrList);
			}
	
	protected void setAttrValInAttrList(AttributeList attrList, String attrName, String attrVal) throws MCADException
	{
		try
		{
			AttributeItr itr = new AttributeItr(attrList);
			while(itr.next())
			{
				Attribute attribute = itr.obj();
				if(attribute.getName().equals(attrName))
				{
					attribute.setValue(attrVal);
					break;
				}
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
	}

	protected void updateInstanceFilesAndRelsForSaveAs(Context _context, Hashtable instanceInfo,Hashtable instanceAttrInfo,
			IEFXmlNodeImpl instanceStructure,BusinessObject clonedBusObject) throws Exception
			{
		// Post processing - adjust the file name for saved as instances
		// Also manage the side effect of changed names in relationships
		String sResult = runRenameInstancesForSaveAs(_context, clonedBusObject);

		//if all other rename related operations are successful,
		//do the file rename for newly created instances (ie, cloned instances)
		if(sResult.startsWith("true"))
		{
			if (_globalConfig.isFileRenameOnServerSide())
			{
				//sResult will be of the form:
				//"true|oldInstName1|newInstanceName1|fileToRename1@oldInstName2|newInstanceName2|fileToRename2..."
				sResult = sResult.substring(5);

				String objectId = clonedBusObject.getObjectId(_context);
				Vector instanceRenameDetails = null;
				if((instanceRenameDetails = (Vector)retTable.get("instanceRenameDetails")) != null)
				{
					instanceRenameDetails.addElement(objectId + "|" +sResult );
				}
				else
				{
					instanceRenameDetails = new Vector();
					instanceRenameDetails.addElement(objectId + "|" + sResult);
					retTable.put("instanceRenameDetails",instanceRenameDetails);
				}
			}
			else
			{

			}
		}
			}

	/**
	 *  This can be invoked from outside and is meant for renaming of files
	 *  and making rename related changes (for Instances only)
	 */
	public String runRenameInstancesForSaveAs(Context _context, BusinessObject renamedFamObj) throws MCADException
	{
		StringBuffer retStr = new StringBuffer();
		retStr.append("true|");

		try
		{
			String instName     = "";

			//get the instance info from the family object
			Hashtable instanceInfoTable = _generalUtil.getInstanceInfo(_context, renamedFamObj);
			if(instanceInfoTable != null)
			{

				Enumeration instNames = instanceInfoTable.keys();
				while(instNames.hasMoreElements())
				{
					instName = (String)instNames.nextElement();

					//get the instance node corresponding to this instance and
					//check the value of attribute "renamedfrom" on the node
					//if the value is blank (which means instance is not renamed)
					//no need to do anything to this instance
					IEFXmlNode instNode = (IEFXmlNode)instanceInfoTable.get(instName);

					String renamedFromVal = instNode.getAttribute("renamedfrom");

					if(renamedFromVal == null || renamedFromVal.trim().length()== 0)
					{
						continue;
					}

					//replace the old instNode with new instNode in the hashtable
					instanceInfoTable.remove(instName);
					instanceInfoTable.put(instName,instNode);
				}
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

		return retStr.toString();
	}

	private BusinessObject buildChildNodeForFamily(Context _context, String busID, Hashtable busIDSaveAsNameTable, Hashtable busIDTargetRevTable, Hashtable busObjExtendedTable,  boolean isFirstLevel) throws Exception
	{
		BusinessObject clonedBusObject  = null;
		boolean isCloned                = false;

		BusinessObject busObject = new BusinessObject(busID);

		boolean isSavedAs = busIDSaveAsNameTable.containsKey(busID);

		if(isSavedAs && !MCADSaveAsUtil.isSavedAs(busID, savedAsObjList))
		{
			isFamilyTableSaveAs = true;
			if(sharedPartsIDClonedBusObjectsTable.containsKey(busID))
			{
				clonedBusObject = (BusinessObject)sharedPartsIDClonedBusObjectsTable.get(busID);
			}
			else
			{
				clonedBusObject = saveAsBusinessObject(_context, busID, busIDSaveAsNameTable, busIDTargetRevTable);
				MCADSaveAsUtil.addSavedAsObjList(busID, clonedBusObject, savedAsObjList);			
				saveDependentDocs(_context, busID, clonedBusObject, null);
				sharedPartsIDClonedBusObjectsTable.put(busID, clonedBusObject);
                               isCloned = true;				 
			}
		}
		else
		{
			clonedBusObject = busObject;
		}

		// Do operations for instances and nested instances
		buildLowLevelNodeForInstances(_context, clonedBusObject.getObjectId(_context), busID, clonedBusObject, busObject, busIDSaveAsNameTable, busIDTargetRevTable, busObjExtendedTable , isCloned);
		return clonedBusObject;
	}

	// Handles the operations related to buliding structure related to instances and nested instances
	private void buildLowLevelNodeForInstances(Context _context, String familyId, String busID, BusinessObject clonedBusObject,  BusinessObject busObject, Hashtable busIDSaveAsNameTable, Hashtable busIDTargetRevTable, Hashtable busObjExtendedTable , boolean isCloned) throws Exception
	{
		Hashtable clonedChildIdCloneObjectMap = new Hashtable();
		BusinessObject familyObject = new BusinessObject(familyId);

		familyObject.open(_context);
		String familyName	= "";
		familyName	= (String)busIDSaveAsNameTable.get(familyId);

		if(null==familyName)
		{
			familyName = familyObject.getName();
		}

		familyObject.close(_context);

		java.util.Hashtable instObjects = _generalUtil.getAllWheareUsedObjects(_context, busObject, true, MCADServerSettings.FAMILY_LIKE);
		Enumeration allInstances		= instObjects.keys();
		while(allInstances.hasMoreElements())
		{
			BusinessObject childBusinessObject = (BusinessObject)allInstances.nextElement();
			String childBusID = childBusinessObject.getObjectId();
			if(!busIDSaveAsNameTable.containsKey(childBusID))
			{
				continue;
			}
			boolean isChildCloned = busIDSaveAsNameTable.containsKey(childBusID);
			String busSaveAsName = "";
			String busTargetRev  = "";
			childBusinessObject.open(_context);
			String oldChildObjectName	= childBusinessObject.getName();
			String oldChildObjectCADType = _util.getCADTypeForBO(_context, childBusinessObject);
			String oldChildObjectCADTypeName = oldChildObjectCADType + "|" + oldChildObjectName;

			childBusinessObject.close(_context);
			if(isCloned)
			{
				if(isChildCloned)
				{
					busSaveAsName = (String)busIDSaveAsNameTable.get(childBusID);
					busTargetRev  = (String)busIDTargetRevTable.get(childBusID);
				}
				else
				{
					busSaveAsName =_generalUtil.getIndivisualInstanceName(oldChildObjectName);
					busTargetRev  = (String)busIDTargetRevTable.get(busID);
				}

				BusinessObject clonedChildBusObject = null;
				
				if(!MCADSaveAsUtil.isSavedAs(childBusID, savedAsObjList))
				{
					clonedChildBusObject = buildInstanceStructure(_context, busIDSaveAsNameTable, busIDTargetRevTable, busObjExtendedTable,
							childBusID, childBusinessObject, null, oldChildObjectCADType, isCloned);
					clonedChildIdCloneObjectMap.put(childBusID, clonedChildBusObject);			
					MCADSaveAsUtil.addSavedAsObjList(childBusID, clonedChildBusObject, savedAsObjList);					

				}else 
				{	
					clonedChildBusObject = MCADSaveAsUtil.getSavedAsObj(_context, childBusID, savedAsObjList);
					clonedChildIdCloneObjectMap.put(childBusID, clonedChildBusObject);    //[NDM] : L86
				}
				
				applyVerticalViewOnObject(_context, childBusID, busIDSaveAsNameTable,busIDTargetRevTable, clonedChildBusObject, oldChildObjectCADTypeName);

				//clonedChildIdCloneObjectMap.put(childBusID, clonedChildBusObject);
				saveDependentDocs(_context, childBusID, clonedChildBusObject, null);
				connectPartWithCADObject(_context, childBusID, clonedChildBusObject);
				// update if child cloned for shared part handling
				if(isChildCloned)
				{
					sharedPartsIDClonedBusObjectsTable.put(childBusID, clonedChildBusObject);
				}

				boolean canConnect = false;
				Relationship relationship = null;
				boolean isFrom = true;
				String relName = "";

               
				Vector relNames = _util.getRelationNamesBetweenObjects(_context, clonedBusObject.getObjectId(_context), clonedChildBusObject.getObjectId(_context));
				for(int i=0;i<relNames.size();i++){
					relName = (String)relNames.get(i);
					relationship = _util.getRelationshipBetweenObjects(_context, clonedBusObject.getObjectId(_context), clonedChildBusObject.getObjectId(_context), relName,null, isFrom);
					relationship.open(_context);

					if(!MCADSaveAsUtil.isConnectedWith(relationship.getName(), "", processedRelIds)){
						canConnect = true;
					}
				}

				// Manage the structure under instance, (makes sense for assembly instance)
				buildInstance(_context, clonedChildBusObject,childBusinessObject,busIDSaveAsNameTable,busIDTargetRevTable, busObjExtendedTable);
				if (clonedChildBusObject != null && clonedBusObject != null)
				{
					
					if(canConnect)
					{
							Relationship newRelationship = _util.ConnectBusinessObjects(_context, clonedBusObject, clonedChildBusObject, relName, false);

							_util.copyAttributesonRelatinship(_context, relationship, newRelationship);	
							MCADSaveAsUtil.addConnectedObjList(clonedChildBusObject.getObjectId(_context), clonedBusObject.getObjectId(_context), relationship.getName(), newRelationship.getName(), savedAsObjList, processedRelIds);
					
					}
				
				}

				// Setting IEF-ClonedFrom attribute
				if(!isFamilyTableSaveAs)
				{
					Relationship rel		= null;
					Hashtable relationsInfo = _generalUtil.getPrimaryRelationshipFromInstance(_context, clonedChildBusObject);
					Enumeration keys		= relationsInfo.keys();
					if (keys.hasMoreElements())
					{
						rel = (Relationship)keys.nextElement();
					}
					String oldInstName = _generalUtil.getIndivisualInstanceName(oldChildObjectName);
					_util.setRelationshipAttributeValue(_context, rel, MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-ClonedFrom"), oldInstName);

				}
				else
				{
					String instanceOf		 = MCADMxUtil.getActualNameForAEFData(_context, "relationship_InstanceOf");
					String parentInstance	 = MCADMxUtil.getActualNameForAEFData(_context, "attribute_ParentInstance");
					String [] oids			 = new String[1];
					oids[0]					 = childBusID;

					StringList busSelectList = new StringList(2);
					String parentInstanceSel = "to[" + instanceOf + "].attribute[" + parentInstance + "]";
					String familyIdSel		 = "to[" + instanceOf + "].from.id";

					busSelectList.add(parentInstanceSel);
					busSelectList.add(familyIdSel);

					BusinessObjectWithSelectList busWithSelectList	= BusinessObjectWithSelect.getSelectBusinessObjectData(_context, oids, busSelectList);
					BusinessObjectWithSelect busWithSelect			= busWithSelectList.getElement(0);
					StringList familyIdList							= (StringList)busWithSelect.getSelectDataList(familyIdSel);
					StringList attrParInstList						= (StringList)busWithSelect.getSelectDataList(parentInstanceSel);

					for(int i=0 ; i<familyIdList.size(); i++)
					{
						String instFamilyId	= (String)familyIdList.elementAt(i);
						if(instFamilyId.equalsIgnoreCase(busObject.getObjectId(_context)))
						{
							String attrParInstVal = (String)attrParInstList.elementAt(i);
							String renamedObjID	= clonedChildBusObject.getObjectId(_context);
							oids				= new String[1];
							oids[0]				= renamedObjID;
							busSelectList		= new StringList(5);
							String relIdSel		= "to[" + instanceOf + "].id";
							busSelectList.add(relIdSel);
							busWithSelectList	= BusinessObjectWithSelect.getSelectBusinessObjectData(_context, oids, busSelectList);
							busWithSelect		= busWithSelectList.getElement(0);
							String relId		= (String)busWithSelect.getSelectData(relIdSel);
							Relationship rel	= new Relationship(relId);
							_util.setRelationshipAttributeValue(_context, rel, parentInstance, attrParInstVal);
						}
					}
				}
				renameIndividual(_context, clonedChildBusObject, childBusID,  false, isFamilyTableSaveAs);
				buildLowLevelNodeForInstances(_context, familyId, busID, clonedChildBusObject, childBusinessObject, busIDSaveAsNameTable, busIDTargetRevTable,busObjExtendedTable, isCloned);
			}
			else
			{
				BusinessObject clonedChildBusObject = null;

				boolean flag = true;

				flag = !MCADSaveAsUtil.isSavedAs(childBusID, savedAsObjList);

				if(isChildCloned && flag)
				{
					// Instance cloned - handle it it's way
					busSaveAsName = (String)busIDSaveAsNameTable.get(childBusID);
					busTargetRev  = (String)busIDTargetRevTable.get(childBusID);


					clonedChildBusObject = saveAsInstanceObject(_context, childBusID, busSaveAsName, busTargetRev, familyId,familyName,busIDSaveAsNameTable);
					MCADSaveAsUtil.addSavedAsObjList(childBusID, clonedChildBusObject, savedAsObjList);

					applyVerticalViewOnObject(_context, childBusID, busIDSaveAsNameTable,busIDTargetRevTable, clonedChildBusObject, oldChildObjectCADTypeName);

	connectPartWithCADObject(_context, childBusID, clonedChildBusObject);
					saveDependentDocs(_context, childBusID, clonedChildBusObject, null);					
					buildInstance(_context, clonedChildBusObject,childBusinessObject,busIDSaveAsNameTable,busIDTargetRevTable, busObjExtendedTable);
					createTowerForSavesAsInstance(_context, childBusinessObject,clonedChildBusObject,busTargetRev,busIDSaveAsNameTable);
					sharedPartsIDClonedBusObjectsTable.put(childBusID, clonedChildBusObject);
					renameIndividual(_context, clonedChildBusObject, childBusID,false,false);
					buildLowLevelNodeForInstances(_context, familyId, busID, clonedChildBusObject, childBusinessObject, busIDSaveAsNameTable, busIDTargetRevTable,busObjExtendedTable, isCloned);
				}
				else
				{
					clonedChildBusObject = buildChildNode(_context, childBusID, busIDSaveAsNameTable, busIDTargetRevTable, busObjExtendedTable , true);
				}
			}
		}


	/*	String activeInstanceId = _generalUtil.getActiveInstanceId(_context, busID);
		BusinessObject instanceClone = (BusinessObject) clonedChildIdCloneObjectMap.get(activeInstanceId);
		if(instanceClone!=null)
		{
			_util.connectBusObjects(_context, familyId, instanceClone.getObjectId(), strActiveInstanceRelationship, true, new Hashtable());
		}*/

                //[NDM] : L86 start           Active Instance Relationship

		BusinessObject famObj = new BusinessObject(familyId);
		String activeInstanceId = _generalUtil.getActiveInstanceId(_context, busID);

		BusinessObject instanceClone = (BusinessObject) clonedChildIdCloneObjectMap.get(activeInstanceId);
		if(instanceClone!=null)
		{	
			Relationship relBetMajors = _util.ConnectBusinessObjects(_context, famObj,instanceClone,strActiveInstanceRelationship, true);   
			//_util.copyAttributesonRelatinship(_context, relationship, relBetMajors);	
			MCADSaveAsUtil.addConnectedObjList(instanceClone.getObjectId(_context),familyId , relBetMajors.getName(), "", savedAsObjList, processedRelIds);

			String instanceCloneMinorObjectId		= _util.getActiveVersionObject(_context, instanceClone.getObjectId(_context));
			String familyMinorObjectID = _util.getActiveVersionObject(_context, familyId);

			BusinessObject instanceCloneMinorObject = new BusinessObject(instanceCloneMinorObjectId);
			BusinessObject familyMinorObject = new BusinessObject(familyMinorObjectID);

			if(instanceCloneMinorObjectId!=null)
			{
				Relationship relBetMinors = _util.ConnectBusinessObjects(_context, familyMinorObject, instanceCloneMinorObject, strActiveInstanceRelationship, true);   
				//   _util.copyAttributesonRelatinship(_context, relationship, relBetMinors);	
				MCADSaveAsUtil.addConnectedObjList(familyMinorObjectID, instanceCloneMinorObjectId, relBetMinors.getName(), "", savedAsObjList, processedRelIds);
		}
		}
		//[NDM] : L86 end}
	


	

	}

	// instance structure handling
	// input object is already "saved as"
	// busObject - Bus obj. selected for saveas
	// clonedBusObject - New object created from busObject
	private BusinessObject buildInstance(Context _context, BusinessObject clonedBusObject, BusinessObject busObject,Hashtable busIDSaveAsNameTable, Hashtable busIDTargetRevTable, Hashtable busObjExtendedTable  ) throws Exception
	{
		// Get all the relatinships of it's children, i.e. dependees
		java.util.Hashtable relsAndEnds =  _generalUtil.getAllWheareUsedRelationships(_context, busObject, true, MCADAppletServletProtocol.ASSEMBLY_LIKE);
		relsAndEnds.putAll( _generalUtil.getAllWheareUsedRelationships(_context, busObject, false, MCADAppletServletProtocol.FAMILY_LIKE));
		relsAndEnds.putAll( _generalUtil.getAllWheareUsedRelationships(_context, busObject, true, MCADServerSettings.EXTERNAL_REFERENCE_LIKE));		
		int size = relsAndEnds.size();

		// Go for children rel update only if NOT a shared guy
		if(size > 0)
		{
			Enumeration allRels = relsAndEnds.keys();
			while(allRels.hasMoreElements())
			{
				// check "end", this is end for child node
				Relationship rel = (Relationship)allRels.nextElement();
				String end = (String)relsAndEnds.get(rel);
				// add to new list, depending on the "end"!!
				BusinessObject busToAdd = null;
				rel.open(_context);
				// The other object is at the other "end"
				boolean isFrom = true;
				if (end.equals("from"))
				{
					busToAdd = rel.getTo();
					isFrom = true;
				}
				else
				{
					busToAdd = rel.getFrom();
					isFrom = false;
				}
				rel.close(_context);
				String childBusID = busToAdd.getObjectId(_context);

				BusinessObject childObj = new BusinessObject(childBusID);
				childObj.open(_context);
				String childBusName = childObj.getName();
				String childCADType = _util.getCADTypeForBO(_context, childObj);
				childObj.close(_context);

				StringBuffer cadTypeName		= new StringBuffer(childCADType);
				cadTypeName.append("|");
				cadTypeName.append(childBusName);

				if(CadTypeNameBusIDTable.containsKey(cadTypeName.toString()))
					childBusID = (String)CadTypeNameBusIDTable.get(cadTypeName.toString());	

				boolean isChildShared = sharedPartsIDClonedBusObjectsTable.containsKey(childBusID);
				BusinessObject clonedChildBusObject = buildChildNode(_context, childBusID, busIDSaveAsNameTable, busIDTargetRevTable, busObjExtendedTable ,  false);

				String relationshipName   = rel.getTypeName();
				if(!MCADSaveAsUtil.isConnectedWith(rel.getName(), "", processedRelIds)){
					boolean canConnect = true;

					BusinessObject newFamObj = _generalUtil.getFamilyObjectForInstance(_context, clonedChildBusObject);

					if(busIDSaveAsNameTable.containsKey(clonedChildBusObject.getObjectId(_context))){
						if(null==newFamObj){
							canConnect = false;
						}
						else{
							newFamObj.open(_context);
							String famName = "";
							if(isFrom){
								famName = clonedBusObject.getName();
							}else{
								famName = clonedChildBusObject.getName();
							}
							if(!newFamObj.getName().equals(famName)){
								canConnect = false;
							}
						}
					}
					clonedChildBusObject.open(_context);
					String tempCadType = _util.getCADTypeForBO(_context, clonedChildBusObject);
					if(tempCadType.equals("componentInstance")){
						if(null==_generalUtil.getFamilyObjectForInstance(_context, clonedChildBusObject)){
							canConnect = false;
						}
						if(clonedBusObject.getName().equals(clonedChildBusObject.getName())){
							canConnect = false;
						}
					}
					
					String sClonedObjId = clonedBusObject.getObjectId(_context);
					String sClonedChildObjId = clonedChildBusObject.getObjectId(_context);
					//[NDM] : L86 start
					//String clonedBusMinorObjectId		= _util.getActiveVersionObject(_context, sClonedObjId);
					//String clonedChildBusMinorObjectID			= _util.getActiveVersionObject(_context, sClonedChildObjId);
					
					
				StringList slGivenIds = new StringList(2);
				slGivenIds.addElement(sClonedObjId);
				slGivenIds.addElement(sClonedChildObjId);
				HashMap hmData = _util.getActiveVersionObjectMultiple(_context, slGivenIds);
				
				String clonedBusMinorObjectId = (String) hmData.get(sClonedObjId);
				String clonedChildBusMinorObjectID = (String) hmData.get(sClonedChildObjId);						
					
					BusinessObject clonedBusMinorObject = new BusinessObject(clonedBusMinorObjectId);
			    	BusinessObject clonedChildBusMinorObject = new BusinessObject(clonedChildBusMinorObjectID);
					

					//[NDM] : L86 end
					
					
					
					
					if(canConnect)
					{
				Relationship relationship = _util.ConnectBusinessObjects(_context, clonedBusObject, clonedChildBusObject, relationshipName, isFrom);
						_util.copyAttributesonRelatinship(_context, rel, relationship);	
						MCADSaveAsUtil.addConnectedObjList(clonedChildBusObject.getObjectId(_context), clonedBusObject.getObjectId(_context), rel.getName(), relationship.getName(), savedAsObjList, processedRelIds);

						//[NDM] : L86 start
						Relationship relBetMinors = _util.ConnectBusinessObjects(_context, clonedBusMinorObject, clonedChildBusMinorObject, relationshipName, isFrom);
						_util.copyAttributesonRelatinship(_context, rel, relBetMinors);	
				        MCADSaveAsUtil.addConnectedObjList(clonedBusMinorObjectId, clonedChildBusMinorObjectID, rel.getName(), relBetMinors.getName(), savedAsObjList, processedRelIds);
				      //[NDM] : L86 end

					}
				}									
				// Copy attributeson new relationship
				boolean isChildCloned = busIDSaveAsNameTable.containsKey(childBusID);
				if(isChildCloned && !isChildShared)
				{
					busToAdd.open(_context);
					String oldChildObjectName = busToAdd.getName();
					renameIndividual(_context, clonedChildBusObject, busToAdd.getObjectId(),false,isFamilyTableSaveAs);
					busToAdd.close(_context);
				}
				MCADSaveAsUtil.addSavedAsObjList(childBusID, clonedBusObject, savedAsObjList);
			}
		}
		return clonedBusObject;
	}

	protected BusinessObject saveAsBusinessObject(Context _context, String boID, Hashtable busIDSaveAsNameTable, Hashtable busIDTargetRevTable) throws Exception
	{
		BusinessObject newBO = null;
		String busSaveAsName = (String)busIDSaveAsNameTable.get(boID);
		String busTargetRev  = (String)busIDTargetRevTable.get(boID);

		String type = "";
		String name = "";
		String vault = "";

		String minorRevString = "";
		String majorPolicy = "";
		String minorPolicy = "";

		boolean newMajorBOExists = false;
		boolean newMinorBOExists = false;

		try
		{
			StringList slOid = new StringList(1);
			slOid.addElement(boID);
			
			String [] oidsTopLevel		  = new String [slOid.size()];
			slOid.toArray(oidsTopLevel);	
			String attrFileSourceName 			= MCADMxUtil.getActualNameForAEFData(_context,"attribute_IEF-FileSource");
			String attrTitle					= MCADMxUtil.getActualNameForAEFData(_context,"attribute_Title");		
			String attrMoveFilesToVersion		= MCADMxUtil.getActualNameForAEFData(_context, "attribute_MoveFilesToVersion");
			String attrIsVersionObject			= MCADMxUtil.getActualNameForAEFData(_context, "attribute_IsVersionObject");
			
			String SELECT_ISVERSIONOBJ = "attribute["+attrIsVersionObject+"]";	
			String SELECT_TITLE = "attribute["+attrTitle+"]";
			
			StringList slSelectsForInputID = new StringList(4);
			slSelectsForInputID.addElement(DomainConstants.SELECT_TYPE);
			slSelectsForInputID.addElement(DomainConstants.SELECT_NAME);
			slSelectsForInputID.addElement(SELECT_ISVERSIONOBJ);
			slSelectsForInputID.addElement(SELECT_TITLE);
			
			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(_context, oidsTopLevel, slSelectsForInputID);
			BusinessObjectWithSelect busObjectWithSelect 		= (BusinessObjectWithSelect)buslWithSelectionList.elementAt(0);
			
			BusinessObject bus = new BusinessObject (boID);
			bus.open(_context);
			//type	= bus.getTypeName();
			//name	= bus.getName();
			type         = (String)busObjectWithSelect.getSelectData(DomainConstants.SELECT_TYPE);
			name         = (String)busObjectWithSelect.getSelectData(DomainConstants.SELECT_NAME);
			
			vault	= _context.getVault().toString();

			BusinessObject busMajor = _util.getMajorObject(_context,bus);

			if(null == busMajor)
				busMajor = bus;

			if(!busMajor.isOpen())
				busMajor.open(_context);

			majorPolicy	= busMajor.getPolicy(_context).getName();
            minorPolicy                     = _util.getRelatedPolicy(_context, majorPolicy);    //[NDM] : L86

			minorRevString	= _util.getFirstVersionStringForStream(busTargetRev);

			newMinorBOExists = _util.doesBusinessObjectExist(_context, type, busSaveAsName, minorRevString);
			newMajorBOExists = _util.doesBusinessObjectExist(_context, type, busSaveAsName, busTargetRev);

			String commonPart = "false";
			if(busIDCommonPartTable != null && busIDCommonPartTable.size() > 0)
			{
				 commonPart = (String)busIDCommonPartTable.get(boID);
			}
			if(commonPart.equalsIgnoreCase("true"))
			{
				BusinessObject busNew = new BusinessObject (type,busSaveAsName,busTargetRev,"");
				return busNew;
			}
			
			//if any of the two objects exist in the DB, throw error.
			if(newMajorBOExists || newMinorBOExists)
			{
				//Close bus before throwing exception
				bus.close(_context);

				Hashtable messageTokens = new Hashtable();
				messageTokens.put("NAME",busSaveAsName);

				MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.NameNRevisionNotUniqueForSaveAs", messageTokens), null);
				
			}
			String isThisVersionObj         = (String)busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
			boolean isVersion = Boolean.valueOf(isThisVersionObj).booleanValue();
			//Clone the business object : creation of major type object
			//if(!_util.isMajorObject(_context, boID))
			if(isVersion)
			{
				// creating minor object by cloning the major object, since clone
				// will automatically copy files from major to new minor
				// eliminating the need to copy files again.

				// First clone major to a temp name, and major rev
				// copy bus OBJECTID to <temp_name> <targetRev> type <minorType> policy <minorPolicy>
				String tempName = _context.getUser() + "_" + Long.toString(System.currentTimeMillis());;

				String Args[] = new String[6];
				Args[0] = boID;
				Args[1] = tempName;
				Args[2] = busTargetRev;
				Args[3] = "type";
				Args[4] = type;
				Args[5] = majorPolicy;

				String result1 = _util.executeMQL(_context, "copy bus $1 to $2 $3 $4 $5 policy $6", Args);

				if(!result1.startsWith("true"))
				{
					MCADServerException.createException(result1.substring(6), null);
				}

				// Then change the temp object name and revision.

				Args = new String[5];
				Args[0] = type;
				Args[1] = tempName;
				Args[2] = busTargetRev;
				Args[3] = busSaveAsName;
				Args[4] = busTargetRev;

				String result2 = _util.executeMQL(_context, "modify bus $1 $2 $3 name $4 revision $5", Args);

				if(!result2.startsWith("true"))
				{
					MCADServerException.createException(result2.substring(6), null);
				}

				newBO = new BusinessObject(type, busSaveAsName, busTargetRev, "");

				String relClassifiedItem  = MCADMxUtil.getActualNameForAEFData(_context,"relationship_ClassifiedItem");

				_util.disconnectRelationShips(_context, newBO, relClassifiedItem, "to");

				//Should be closed at the end ???
				newBO.open(_context);
				_util.copyFilesFcsSupported(_context, mcsURL, bus, newBO); 
			}
			else
			{
				newBO = bus.clone(_context, busSaveAsName, busTargetRev, vault);
				newBO.open(_context);
				removeAllInterfaces(_context, newBO.getObjectId(_context));
			}

			//New implementation.
			String minObjId = _generalUtil.createAndConnectToMinorObject(_context, minorRevString, true, newBO, _globalConfig, true, false,minorPolicy);       //[NDM] : L86

			if(!_globalConfig.getModificationEvents().isEmpty())
				_util.modifyUpdateStamp(_context, minObjId);

			BusinessObject minorObject	= new BusinessObject(minObjId);
			
			//_util.executeMQL(_context, "set env global MCADINTEGRATION_CONTEXT true");
			//_util.copyFilesFcsSupported(_context, newBO, minorObject);
			//_util.executeMQL(_context, "set env global MCADINTEGRATION_CONTEXT false");
			
			AttributeList minorObjAttrList = new AttributeList(3);
			
			if(_busIDPartIDTable.containsKey(boID))
			{
				String specification		= (String)_busIDSpecificationTable.get(boID);
				String attrSpecification	= MCADMxUtil.getActualNameForAEFData(_context,"attribute_IEF-Specification");				
//				minorObject.open(_context);
//				_util.setAttributeValue(_context, minorObject, attrSpecification, specification);
//				minorObject.close(_context);
				minorObjAttrList.addElement(new Attribute(new AttributeType(attrSpecification), specification));
				
			}



			String titleAttValue = busSaveAsName;

			boolean isTMCSilentSaveAS = false;
			if( _argumentsTable.get("TMCSilentSaveAS") != null) 
				isTMCSilentSaveAS= (boolean)_argumentsTable.get("TMCSilentSaveAS") ;
			
			if(_globalConfig.isObjectAndFileNameDifferent()&&!isTMCSilentSaveAS) 
			{
				//String sourceObjectTitle = _util.getAttributeForBO(_context, bus.getObjectId(), attrTitle);
				String sourceObjectTitle         = (String)busObjectWithSelect.getSelectData(SELECT_TITLE);
				//String sourceBusName     = bus.getName();
				String sourceBusName     =(String)busObjectWithSelect.getSelectData(DomainConstants.SELECT_NAME);
			
				String strExtension = "";
				int index = sourceObjectTitle.lastIndexOf(".");
				if(index != -1)
					strExtension = sourceObjectTitle.substring(index);

				if(isObjectAndFileNameDifferent)
				{
					titleAttValue = (String)_ObjectNewNamesTitleTable.get(busSaveAsName);

					if(!titleAttValue.contains(strExtension))
					{
						titleAttValue = titleAttValue + strExtension;
					}

				}
				else
				{
					if(isNameAndTitleMatch(sourceBusName, sourceObjectTitle))
						titleAttValue = busSaveAsName + strExtension;
				}
				//minorObject.setAttributeValue(_context, attrTitle, titleAttValue);
				minorObjAttrList.addElement(new Attribute(new AttributeType(attrTitle), titleAttValue));
			}

			if(isTMCSilentSaveAS)
			{
				String designSeries = "";
				String prefix = "";
				designSeries = busSaveAsName.substring(busSaveAsName.lastIndexOf('-'));
				if(type.equalsIgnoreCase("CATPart"))
				{
					prefix = "Part";										
					titleAttValue = prefix + designSeries;	
					
				}
				else if(type.equalsIgnoreCase("CATProduct"))
				{
					prefix = "Product";					
					titleAttValue = prefix + designSeries;	
				}
				
				//minorObject.setAttributeValue(_context, attrTitle, titleAttValue);
				minorObjAttrList.addElement(new Attribute(new AttributeType(attrTitle), titleAttValue));
			}
			
			minorObject.setAttributes(_context, minorObjAttrList);
			minorObject.update(_context);
			
			AttributeList majorAttributelist = new AttributeList(5);
			majorAttributelist.addElement(new Attribute(new AttributeType(attrFileSourceName), MCADAppletServletProtocol.FILESOURCE_SAVEAS));
			majorAttributelist.addElement(new Attribute(new AttributeType(attrTitle), titleAttValue));
			majorAttributelist.addElement(new Attribute(new AttributeType(attrMoveFilesToVersion), "True"));              //[NDM] : L86
			majorAttributelist.addElement(new Attribute(new AttributeType(attrIsVersionObject), "False"));			

			
			if(isTMCSilentSaveAS)
			{				
				String SELECT_PHID 				= "physicalid";
				String SELECT_UPDATESTAMP 		= "updatestamp";
				
				StringList busSelects = new StringList();
				busSelects.add(SELECT_PHID);
				busSelects.add(SELECT_UPDATESTAMP);

				String [] oid = new String[]{boID};

				BusinessObjectWithSelect busWithSelect = BusinessObject.getSelectBusinessObjectData(_context, oid, busSelects).getElement(0);
				
				String physicalid		= busWithSelect.getSelectData(SELECT_PHID);
				String updatestamp		= busWithSelect.getSelectData(SELECT_UPDATESTAMP);
				
				String attrSourceObj	 	= MCADServerGeneralUtil.getActualNameForAEFData(_context, "attribute_MCADInteg-SourceObj"); 

				String attrSourceObjValue=	physicalid + "|" + updatestamp;
//				_util.setAttributeValue(_context, newBO, attrSourceObj, attrSourceObjValue);
				majorAttributelist.addElement(new Attribute(new AttributeType(attrSourceObj), attrSourceObjValue));		
			}
			
			newBO.setAttributes(_context, majorAttributelist);
			newBO.update(_context);
			
			if(isTMCSilentSaveAS && !isParentSavedAs)
			{
				
				Vector folderInfo	= (Vector)_busIDFolderInfoTable.get(boID);
				String folderID		= (String)folderInfo.get(0);
				String applyToChild	= (String)folderInfo.get(1);

				newBO.open(_context);
				String clonedBusName		= newBO.getName();
				String clonedBusType		= newBO.getTypeName();
				String clonedBusRevision	= newBO.getRevision();
				newBO.close(_context);

				boolean hasAssignedFolders = _folderUtil.hasAssignedFolders(_context, folderID , clonedBusType, clonedBusName, clonedBusRevision);

				if(!hasAssignedFolders)
				{
					_folderUtil.assignToFolder(_context, newBO, folderID, "false");
				}
				
				isParentSavedAs=true;
			}
			if(!isTMCSilentSaveAS)	
			connectToFolder(_context, boID, newBO);

			if(newBO.isOpen())
			{
				newBO.close(_context);
			}

			bus.close(_context);

			if(busMajor.isOpen())
				busMajor.close(_context);
		}
		catch (Exception e)
		{
			Hashtable messageTokens = new Hashtable();
			messageTokens.put("NAME",name);

			String messageString = _serverResourceBundle.getString("mcadIntegration.Server.Message.ErrorWhileSaveAsOfBO", messageTokens);
			MCADServerException.createException(messageString + e.getMessage(), e);
		}
		return newBO;
	}

	public void removeAllInterfaces(Context context, String objectId)
	{
		try
		{
			boolean isRACEEnvt = MCADMxUtil.isSolutionBasedEnvironment(context);

			String Args[] = new String[3];
			Args[0] = objectId;
			Args[1] = "interface";
			Args[2] = "|";

			String strResult    = _util.executeMQL(context, "print bus $1 select $2 dump $3", Args);
			if(strResult.startsWith("true|"))
			{
				strResult              = strResult.substring(strResult.indexOf("|"));
				StringTokenizer tokens = new StringTokenizer(strResult, "|");

				while (tokens.hasMoreTokens())
				{
					String interfaceName = tokens.nextToken();
					boolean bContinue = true;
					if(isRACEEnvt && interfaceName.startsWith("XP_")){
						bContinue = false;
					}
					if(bContinue){
					
						Args = new String[4];
						Args[0] = objectId;
						Args[1] = "remove";
						Args[2] = "interface";
						Args[3] = interfaceName;

						_util.executeMQL(context, "modify bus $1 $2 $3 $4", Args);	
					}		

				}
			}
		}
		catch (Exception e)
		{
		}
	}

	/**
	 * 
	 * input:
	 * boID - id of selected instance obnject
	 * busSaveAsName - new name
	 * busTargetRev - new target rev
	 * familyName - name of family of the instance
	 * Output:
	 * cloned bus object
	 * @param busIDSaveAsNameTable 
	 * 
	 */
	protected BusinessObject saveAsInstanceObject(Context _context, String boID, String busSaveAsName,String busTargetRev,
			String famObjID,String newFamName, Hashtable busIDSaveAsNameTable) throws Exception
			{
		BusinessObject famObj = new BusinessObject(famObjID);

		famObj.open(_context);
		String familyName	= "" ;	

		validateInstance(_context, famObj, busIDSaveAsNameTable);

		familyName	= newFamName;

		if(null==familyName){
			familyName = famObj.getName();
		}


		// The case where only instance is SaveAs. The instance will be connected to the original family. So it will take the Original family name.
		String attribTitle         = MCADMxUtil.getActualNameForAEFData(_context,"attribute_Title");
		
		String existingFamTitle           =  "" ;
		
		if(null!=famObj)
		{
			existingFamTitle     =  _util.getAttributeForBO(_context, famObjID, attribTitle);

			if(existingFamTitle.lastIndexOf(".") != -1)                                                         //this substring mey be required in case of ProE. Need to verify. 
				existingFamTitle     = existingFamTitle.substring(0, existingFamTitle.lastIndexOf("."));

			int    dotIndex             = existingFamTitle.lastIndexOf(".");
			if(dotIndex != -1)
			{
				existingFamTitle = existingFamTitle.substring(0, dotIndex);
			}
		}

		famObj.close(_context);

		BusinessObject newMajorBO = null;

		/*if(majorMinorBusObejctsTable.containsKey(boID))                                     //[NDM] : L86 
			boID = (String)majorMinorBusObejctsTable.get(boID);*/

		String type = "";
		String vault = "";
		String policy = "";
		//String minorTypeName = "";
		//String majorTypeName = "";
		String minorRevString = "";
		String majorPolicy = "";
		String minorPolicy = "";



		BusinessObject busObject = new BusinessObject (boID);

		if(busSaveAsName == null || "".equals(busSaveAsName)) // Instance not selected for Save As
		{
			busObject.open(_context);

			busSaveAsName = busObject.getName();

			if(!_globalConfig.isUniqueInstanceNameInDBOn())
				busSaveAsName 	= MCADUtil.getIndivisualInstanceName(_util.getAttributeForBO(_context, boID, attribTitle), true);

			busTargetRev = 	busObject.getRevision();

			busObject.close(_context);
		}

		// get new name of instance object
		String newInstName = _generalUtil.getNameForInstance(familyName,busSaveAsName);

		try
		{
			BusinessObject bus	= new BusinessObject (boID);
			bus.open(_context);

			type				= bus.getTypeName();
			vault				= _context.getVault().toString();            
			policy				= bus.getPolicy(_context).getName();  

			bus.close(_context);

			// [NDM] : L86 (since type will be same for both major as well as minor objects)
			/*if(_globalConfig.isMajorType(type))
			{
				majorTypeName = type;
				minorTypeName = _util.getCorrespondingType(_context, type);
			}
			else
			{
				minorTypeName = type;
				majorTypeName = _util.getCorrespondingType(_context, type);
			}*/

			BusinessObject busMajor = _util.getMajorObject(_context,bus);
			if(busMajor !=null)
			{
				majorPolicy			= busMajor.getPolicy(_context).getName();
			}else
			{
				majorPolicy = policy;
			}

			 minorPolicy                     = _util.getRelatedPolicy(_context, majorPolicy);    //[NDM] : L86

			//get the first minor revision string from the stream
			minorRevString			= _util.getFirstVersionStringForStream(busTargetRev);

			boolean newMajorBOExists = _util.doesBusinessObjectExist(_context, type, newInstName, busTargetRev);
			boolean newMinorBOExists = _util.doesBusinessObjectExist(_context, type, newInstName, minorRevString);

			//if any of the two objects exist in the DB, throw error.
			if(newMajorBOExists || newMinorBOExists)
			{
				//throw new Exception("Name and Revision not unique for object '" + newInstName + "'");
				Hashtable messageTokens = new Hashtable();
				messageTokens.put("NAME",newInstName);

				MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.NameNRevisionNotUniqueForSaveAs", messageTokens), null);

			}
			//Clone the business object : creation of minor type object
			/*//if(_globalConfig.isMajorType(type))*/                               //[NDM] : L86
			if(!_util.isMajorObject(_context, boID))

			{
				//String minorPolicy  = _globalConfig.getDefaultPolicyForType(minorTypeName);
	//boID				= 	_util.getActiveMinor(_context, bus).getObjectId(_context);                //[NDM] : L86
				StringBuffer mqlCmdBuff = new StringBuffer();
				//	boID				= 	_util.getActiveMinor(_context, bus).getObjectId(_context);

				String Args[] = new String[6];
				Args[0] = boID;
				Args[1] = newInstName;
				Args[2] = busTargetRev;
				Args[3] = "type";
				Args[4] = type;
				Args[5] = majorPolicy;
				String result = _util.executeMQL(_context, "copy bus $1 to $2 $3 $4 $5 policy $6", Args);


				if(!result.startsWith("true"))
				{
					MCADServerException.createException(result.substring(6), null);
				}
				newMajorBO = new BusinessObject(type, newInstName, busTargetRev, "");
				newMajorBO.open(_context);

				_util.copyFilesFcsSupported(_context, mcsURL, bus, newMajorBO);
			}
			else
			{
				bus.open(_context);
				newMajorBO = bus.clone(_context, newInstName, busTargetRev, vault);
			}

			bus.close(_context);
			if(newMajorBO.isOpen())
			{
				newMajorBO.close(_context);
			}
			//New implementation.
			String attrFileSourceName = MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-FileSource");
			_util.setAttributeValue(_context, newMajorBO, attrFileSourceName,MCADAppletServletProtocol.FILESOURCE_SAVEAS);

			String minObjId = _generalUtil.createAndConnectToMinorObject(_context, minorRevString, true, newMajorBO, _globalConfig, true, false,minorPolicy);        //[NDM] : L86

			if(!_globalConfig.getModificationEvents().isEmpty())
				_util.modifyUpdateStamp(_context, minObjId);

			BusinessObject minorObject	= new BusinessObject(minObjId);
			
			_util.executeMQL(_context, "set env global MCADINTEGRATION_CONTEXT true");
			_util.copyFilesFcsSupported(_context, newMajorBO, minorObject);
			_util.executeMQL(_context, "set env global MCADINTEGRATION_CONTEXT false"); 
			
			if(_busIDPartIDTable.containsKey(boID))
			{
				String specification		= (String)_busIDSpecificationTable.get(boID);
				String attrSpecification	= MCADMxUtil.getActualNameForAEFData(_context,"attribute_IEF-Specification");
				minorObject.open(_context);
				_util.setAttributeValue(_context, minorObject, attrSpecification, specification);
				minorObject.close(_context);
			}

			if(_globalConfig.isObjectAndFileNameDifferent())
			{
				String attrTitle            = MCADMxUtil.getActualNameForAEFData(_context,"attribute_Title");
				String familyTitle          =  "" ;

				BusinessObject savedAsFamObj = MCADSaveAsUtil.getSavedAsObj(_context, famObjID, savedAsObjList);

				if(null!=savedAsFamObj)
				{
					familyTitle          =  _util.getAttributeForBO(_context, savedAsFamObj.getObjectId(), attrTitle);
				}
				else
				{
					familyTitle = familyName;
				}

				if(isNameAndTitleMatch(familyName, familyTitle))
					familyTitle = familyTitle.substring(0, familyTitle.lastIndexOf("."));

				int    dotIndex             = familyTitle.lastIndexOf(".");
				if(dotIndex != -1)
				{
					familyTitle = familyTitle.substring(0, dotIndex);
				}
				String instanceTitle = "";
				
				//if(isObjectAndFileNameDifferent)
				//{
					familyTitle = (String)_ObjectNewNamesTitleTable.get(familyName);
				
						if(null == familyTitle)                                              // L86 : The value of (familyTitle = null) this means _ObjectNewNamesTitleTable does not contain the familyName bcoz Family is not SavedAs. SaveAs is performed only on the Instance .
						familyTitle = existingFamTitle;
				
					instanceTitle = (String)_ObjectNewNamesTitleTable.get(busSaveAsName);
				
				if(instanceTitle == null || "".equals(instanceTitle))  
					instanceTitle 	= MCADUtil.getIndivisualInstanceName(_util.getAttributeForBO(_context, boID, attribTitle), true);
				//}

				instanceTitle = MCADUtil.getDisplayNameForInstance(familyTitle, instanceTitle);

				if(!isFamilyTableSaveAs && !isValidForSaveAs(_context, instanceTitle, famObjID))
				{
					Hashtable messageTokens = new Hashtable();
					messageTokens.put("NAME",newInstName);
					messageTokens.put("FAMILYNAME", familyName);

					MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.InstanceNameNotUniqueForFamily", messageTokens), null);
				}

				newMajorBO.setAttributeValue(_context,  attrTitle, instanceTitle);
				minorObject.setAttributeValue(_context, attrTitle , instanceTitle);
			}

			

			if(null != integrationName && !integrationName.equalsIgnoreCase("solidworks"))
                        {
			connectToFolder(_context, boID, newMajorBO);
		        }

			if(_globalConfig.isModificationEvent(MCADAppletServletProtocol.UPDATESTAMP_EVENT_CLONE_INSTANCE))
				_util.modifyUpdateStamp(_context, famObjID);
		}
		catch (Exception e)
		{
			Hashtable messageTokens = new Hashtable();
			messageTokens.put("NAME", newInstName);

			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.ErrorWhileSaveAsOfBO", messageTokens) + e.getMessage(), e);
		}

		return newMajorBO;

			}

	private String getAutoName(Context _context, BusinessObject busObject) throws Exception {
		if(!busObject.isOpen())
		{
			busObject.open(_context);
		}
		String sType = "type_CADModel";
		boolean isTMCSilentSaveAS = false;
		if( _argumentsTable.get("TMCSilentSaveAS") != null) 
			isTMCSilentSaveAS= (boolean)_argumentsTable.get("TMCSilentSaveAS") ;

/*		if(isTMCSilentSaveAS)
		{
			sType = "type_MCADModel";
		}*/
		if(_generalUtil.isDrawingLike(_context, busObject.getTypeName()))
		{
			sType = "type_CADDrawing";
		}

		Vector autoNameSeries 	= _globalConfig.getAutoNameSeries(sType);
		Vector autoNames 		= _util.getAutonames(_context, sType, (String)autoNameSeries.get(0), 1);

		return (String) autoNames.firstElement();
		
	}
	// From the input instanceObject, gets all connected fellows from its tower,
	// revise all and connect to the input loned Obj.
	protected void createTowerForSavesAsInstance(Context _context, BusinessObject instObject, BusinessObject clonedInstObj,
			String targetRevision, Hashtable busIDSaveAsNameTable) throws Exception
			{
		try
		{
			Relationship rel = null;
			instObject.open(_context);
			String oldName = instObject.getName();
			instObject.close(_context);

			String oldInstName = _generalUtil.getIndivisualInstanceName(oldName);
			BusinessObject famObj = _generalUtil.getFamilyObjectForInstance(_context, clonedInstObj);

			if(null==famObj){
				famObj = _generalUtil.getFamilyObjectForInstance(_context, instObject);
				if(busIDSaveAsNameTable.containsKey(famObj.getObjectId(_context))){
					famObj = null;
				}
			}

			if(null!=famObj)
			{
				Vector relNames = _util.getRelationNamesBetweenObjects(_context, famObj.getObjectId(_context), clonedInstObj.getObjectId(_context));
				boolean isFrom = true;

				//[NDM] : L86 start
				String clonedBusMinorObjectId		= _util.getActiveVersionObject(_context, famObj.getObjectId(_context));
				String clonedChildBusMinorObjectID			= _util.getActiveVersionObject(_context, clonedInstObj.getObjectId(_context));
				
				BusinessObject clonedBusMinorObject = new BusinessObject(clonedBusMinorObjectId);
				BusinessObject clonedChildBusMinorObject = new BusinessObject(clonedChildBusMinorObjectID);
				//[NDM] : L86 end

				for(int i=0;i<relNames.size();i++)
				{
					String relName = (String)relNames.get(i);
					Relationship relationship = _util.getRelationshipBetweenObjects(_context, famObj.getObjectId(_context), clonedInstObj.getObjectId(_context), relName,null, isFrom);
					relationship.open(_context);

					//String relationshipName   = relationship.getTypeName();                             //[NDM] : L86   
					if(MCADSaveAsUtil.isConnectedWith(relationship.getName(), "", processedRelIds)){                      
						continue;
					}
					
						Relationship relBetMajors = _util.ConnectBusinessObjects(_context, famObj, clonedInstObj, relName, true);
					_util.copyAttributesonRelatinship(_context, relationship, relBetMajors);
					MCADSaveAsUtil.addConnectedObjList(clonedInstObj.getObjectId(_context), famObj.getObjectId(_context), relationship.getName(), relBetMajors.getName(), savedAsObjList, processedRelIds);
					
					//[NDM] : L86 start
					Relationship relBetMinors = _util.ConnectBusinessObjects(_context, clonedBusMinorObject, clonedChildBusMinorObject, relName, isFrom);
					_util.copyAttributesonRelatinship(_context, relationship, relBetMinors);	
			        MCADSaveAsUtil.addConnectedObjList(clonedBusMinorObjectId, clonedChildBusMinorObjectID, relationship.getName(), relBetMinors.getName(), savedAsObjList, processedRelIds);   
					//[NDM] : L86 end
				}
			}
			Hashtable relationsInfo = _generalUtil.getPrimaryRelationshipFromInstance(_context, clonedInstObj);

			Enumeration keys = relationsInfo.keys();
			if (keys.hasMoreElements())
			{
				rel = (Relationship)keys.nextElement();
			}

			boolean flag = true;		
			flag = null!=rel;		
			if(flag && !isFamilyTableSaveAs)
				_util.setRelationshipAttributeValue(_context, rel, MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-ClonedFrom"), oldInstName);
		}
		catch (Exception e)
		{
			//String result = "Error occured during SaveAs of object '" + newInstName + "'. Error is " + e.getMessage();
			String result = e.getMessage();

			MCADServerException.createException(result, e);
		}
			}

	// Relatinship attributes update as a result of file name change
	// Also create metadata information for file name change
	// Note : File renaming is done inside JAR file (in calling command handler)
	public void renameIndividual(Context _context, BusinessObject renamedObj, String originalObjectId,boolean isShared, boolean setRenamedFromOnFamilyLike) throws Exception
	{
		StringList slOid = new StringList(1);
		slOid.addElement(originalObjectId);
		
		String [] oidsTopLevel		  = new String [slOid.size()];
		slOid.toArray(oidsTopLevel);	
		
		String SELECT_TITLE_ATT  = new StringBuffer("attribute[").append(MCADMxUtil.getActualNameForAEFData(_context, "attribute_Title")).append("]").toString();
		
		StringList slSelectsForInputID = new StringList(2);
		slSelectsForInputID.addElement(SELECT_TITLE_ATT);
		slSelectsForInputID.addElement(DomainConstants.SELECT_NAME);
		
		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(_context, oidsTopLevel, slSelectsForInputID);
		BusinessObjectWithSelect busObjectWithSelect 		= (BusinessObjectWithSelect)buslWithSelectionList.elementAt(0);
			
			
		//String oldName  = _util.getBusNameFromId(_context, originalObjectId);
		String oldName         = (String)busObjectWithSelect.getSelectData(DomainConstants.SELECT_NAME);		
		

		renamedObj.open(_context);
		String newName  = renamedObj.getName();
		String objectId = renamedObj.getObjectId();
		renamedObj.close(_context);

		if (renamedObj != null)
		{
			// Step 1 : File movements/renaming

			// check for instance type
			String cad_type = _util.getCADTypeForBO(_context, renamedObj);
		
			boolean isInstanceLike = false;

			if (_globalConfig.isTypeOfClass(cad_type, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
			{
				String orgOldName = oldName;
				// Getting Individual name for instance
				oldName = _generalUtil.getIndivisualInstanceName(oldName);
				BusinessObject newMajorObj = _util.getMajorObject(_context,renamedObj);

				if(newMajorObj != null && !"".equals(newMajorObj))
				{
					//newMajorObj.open(_context);
					_util.setRenamedFromOnBO(_context,newMajorObj, orgOldName);
					//newMajorObj.close(_context);
				}
				isInstanceLike = true;
			}

			boolean isDerivedOutputLike = _globalConfig.isTypeOfClass(cad_type, MCADAppletServletProtocol.TYPE_DERIVEDOUTPUT_LIKE);
			if(!isDerivedOutputLike)
			{
				if(isObjectAndFileNameDifferent)
				{
					newName = (String)_ObjectNewNamesTitleTable.get(newName);
				}
				String valueForRenamedFrom     = oldName;
				String newNameForFileOperation = newName; 

				if(!isInstanceLike && _globalConfig.isObjectAndFileNameDifferent())
				{
					//String attrTitle        = MCADMxUtil.getActualNameForAEFData(_context,"attribute_Title");	
//					valueForRenamedFrom     = _util.getAttributeForBO(_context, originalObjectId, attrTitle);
					valueForRenamedFrom = (String)busObjectWithSelect.getSelectData(SELECT_TITLE_ATT);	
					
					if(isNameAndTitleMatch(oldName, valueForRenamedFrom)) //May need to add check based on primary file format
						newNameForFileOperation = newNameForFileOperation + valueForRenamedFrom.substring(valueForRenamedFrom.lastIndexOf("."));
				}
				// Get server operations like relationship attrb update done
				try
				{
					boolean  hasAccess = renamedObj.checkAccess(_context,(short)Access.cModify);
					if(hasAccess)
				_generalUtil.doActualRename(_context,renamedObj,newName,oldName, newNameForFileOperation, valueForRenamedFrom, setRenamedFromOnFamilyLike,false,false);
				}catch(Exception e)
				{
			
				}
				//if other rename related operations are successfully done, rename the files


				
				String attributeVersionOf 				= MCADMxUtil.getActualNameForAEFData(_context, "attribute_IsVersionObject");
				String SELECT_ATTR_IS_VERSION_OF 		= "attribute[" + attributeVersionOf + "].value";
				String activeVersionRelActualName		= MCADMxUtil.getActualNameForAEFData(_context, "relationship_ActiveVersion");
				String versionOfRelActualName		 	= MCADMxUtil.getActualNameForAEFData(_context, "relationship_VersionOf");
				String SELECT_MAJOR_ID 					= "from[" + versionOfRelActualName + "].to.id"; 
				String SELECT_ACTIVE_MINOR				= "from[" + activeVersionRelActualName + "].to.id";
				
				StringList busSelects = new StringList(3);
				busSelects.add(SELECT_ATTR_IS_VERSION_OF);
				busSelects.add(SELECT_MAJOR_ID);
				busSelects.add(SELECT_ACTIVE_MINOR);

				String [] oid = new String[]{objectId};

				BusinessObjectWithSelect busWithSelect = BusinessObject.getSelectBusinessObjectData(_context, oid, busSelects).getElement(0);
				
				String isMinor		= busWithSelect.getSelectData(SELECT_ATTR_IS_VERSION_OF);
				
				String businessObjId = "";
				
				if("true".equalsIgnoreCase(isMinor))
					businessObjId = busWithSelect.getSelectData(SELECT_MAJOR_ID);
				else
					 businessObjId = busWithSelect.getSelectData(SELECT_ACTIVE_MINOR);

				BusinessObject businessObj = new BusinessObject(businessObjId);
				
				try
				{
				
				if(businessObj != null && !"".equals(businessObj))
					{
						boolean  hasAccess = businessObj.checkAccess(_context,(short)Access.cModify);
						if(hasAccess)
					_generalUtil.doActualRename(_context,businessObj,newName,oldName, newNameForFileOperation, valueForRenamedFrom, setRenamedFromOnFamilyLike,false,true);
					}
				}catch(Exception e)
				{
				
				}
			}

			if (_globalConfig.isFileRenameOnServerSide() && !isShared)
			{
				if (_globalConfig.isTypeOfClass(cad_type, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
					newName = _generalUtil.getIndivisualInstanceName(newName);

				updateObjectRenameDetails(objectId, oldName, newName);

				String minorID = _util.getActiveVersionObject(_context, objectId);
				if(minorID != null && !"".equals(minorID))  
					updateObjectRenameDetails(minorID, oldName, newName);
			
			}
		}
	}

	protected void updateObjectRenameDetails(String objectId, String oldName, String newName) throws Exception
	{
		Vector objectRenameDetails = null;
		if((objectRenameDetails = (Vector)retTable.get("objectRenameDetails")) != null)
		{
			String strObjectRenameDetails = objectId + "|" + oldName + "|" + newName;
			if(!objectRenameDetails.contains(strObjectRenameDetails))
			{
				objectRenameDetails.addElement(strObjectRenameDetails);
			}
		}
		else
		{
			objectRenameDetails = new Vector();
			objectRenameDetails.addElement(objectId + "|" + oldName + "|" + newName);
			retTable.put("objectRenameDetails",objectRenameDetails);
		}
	}

	/* Interface function to get the new minor object ID*/
	public String getNewBoId()
	{
		String sBusId = null;
		return sBusId;
	}

	/**
	 * Copies Objects connected with busID object with "Dependent Document"
	 * relationship, and connect them with oldNewDepDocNames.
	 * If arg3(oldNewDepDocNames) is null, name of the dependent doc object will
	 * be same as oldNewDepDocNames, otherwise name in the oldNewDepDocNames will
	 * be used.
	 * Note that the oldNewDepDocNames contains a string of the form:
	 * newInstName + "|" + newDepDocName
	 */
	protected void saveDependentDocs(Context _context, String busID, BusinessObject clonedBusObject, Hashtable oldNewDepDocNames)throws Exception
	{
		String name = "";
		String vault = "";
		String minorRevString = "";               //[NDM] : L86    
		try
		{
			BusinessObject bus = new BusinessObject (busID);
			bus.open(_context);
			name = bus.getName();
			vault = _context.getVault().toString();
			bus.close(_context);

			String busSaveAsName = "";
			String sParentName = "";

			//Get all relationships connected to "bus" object with type = DependentDocumentLike.
			java.util.Hashtable relsAndEnds =  _generalUtil.getAllWheareUsedRelationships(_context, bus, true, MCADAppletServletProtocol.DERIVEDOUTPUT_LIKE);
			Enumeration allRels = relsAndEnds.keys();
			while(allRels.hasMoreElements())
			{
				Relationship rel = (Relationship)allRels.nextElement();
				String end = (String)relsAndEnds.get(rel);

				BusinessObject busDepDoc = null;
				rel.open(_context);
				// The other object is at the other "end"
				if (end.equals("from"))
				{
					busDepDoc = rel.getTo();
				}
				else
				{
					busDepDoc = rel.getFrom();
				}
				String relationshipName = rel.getTypeName();
				rel.close(_context);

				if(oldNewDepDocNames!=null)
				{
					String sInstanceNameAndDepDocName = (String)MCADUtil.getObject(oldNewDepDocNames, busDepDoc.getName(), isSystemCaseSensitive);
					if(sInstanceNameAndDepDocName == null)
					{
						continue;
					}
					StringTokenizer stTokens = new StringTokenizer(sInstanceNameAndDepDocName, "|");

					// 1st token is the Instance Name / Parent Name
					if ( stTokens.hasMoreTokens() )
						sParentName = (stTokens.nextToken()).trim();
					// 2nd token is the new dep doc name
					if ( stTokens.hasMoreTokens() )
						busSaveAsName = (stTokens.nextToken()).trim();
					if( (sParentName == null) || (busSaveAsName==null) )
					{
						continue;
					}
				}
				else
				{
					busSaveAsName =  clonedBusObject.getName();
					sParentName = clonedBusObject.getName();
				}

				String revision = clonedBusObject.getRevision();
				minorRevString			= _util.getFirstVersionStringForStream(revision);              //[NDM] : L86          

				String orgDepDocName = busDepDoc.getName();
				if(_globalConfig.isTypeOfClass(_util.getCADTypeForBO(_context,bus), MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
				{
					name            = _generalUtil.getIndivisualInstanceName(name);
					busSaveAsName   = _generalUtil.getIndivisualInstanceName(busSaveAsName);
				}
				busSaveAsName =_util.replace(orgDepDocName, name, busSaveAsName);

				if(busSaveAsName.equals(orgDepDocName))
				{
					busSaveAsName = MCADMxUtil.getUniqueObjectName(integrationName);
				}

				BusinessObject clonedDepDocBO = null;
				String busDepDocObjectId 	= busDepDoc.getObjectId(_context);

				if(!MCADSaveAsUtil.isSavedAs(busDepDocObjectId, savedAsObjList))
				{
					clonedDepDocBO = busDepDoc.clone(_context,busSaveAsName, minorRevString, vault);       //[NDM] : L86    "minorRevString" sent instead of "revision"
					renameIndividual(_context, clonedDepDocBO, busDepDoc.getObjectId(_context), false, false);
					MCADSaveAsUtil.addSavedAsObjList(busDepDocObjectId, clonedDepDocBO, savedAsObjList);
				}
				else
				{
					clonedDepDocBO = MCADSaveAsUtil.getSavedAsObj(_context, busDepDocObjectId, savedAsObjList);
				}

				//set Parent name attribute on relatoinship.

                //[NDM] : L86 start
				String clonedBusObjId  = clonedBusObject.getObjectId(_context);
				String clonedDepDocBOId = clonedDepDocBO.getObjectId(_context);
				String clonedBusMinorObjId = _util.getActiveVersionObject(_context, clonedBusObject.getObjectId(_context));
				//[NDM] : L86 end



				Hashtable atrTable = new java.util.Hashtable();
				String varNameAttr = MCADMxUtil.getActualNameForAEFData(_context,"attribute_CADObjectName");
				String varNameAttrVal = sParentName;
				atrTable.put(varNameAttr, varNameAttrVal);
				if (!MCADSaveAsUtil.isConnectedWith(rel.getName(), "",processedRelIds))
				{
					//[NDM] : L86  start    
					if(!_util.doesRelationExist(_context, clonedBusObjId, clonedDepDocBOId, relationshipName))
					{
						_util.connectBusObjects(_context, clonedBusObjId, clonedDepDocBOId, relationshipName, true, atrTable);
					}
					MCADSaveAsUtil.addConnectedObjList(clonedDepDocBOId, clonedBusObjId, rel.getName(), "",
							savedAsObjList, processedRelIds);

					if(!_util.doesRelationExist(_context, clonedBusMinorObjId, clonedDepDocBOId, relationshipName))
					{
						_util.connectBusObjects(_context, clonedBusMinorObjId, clonedDepDocBOId, relationshipName, true, atrTable);
					}

					MCADSaveAsUtil.addConnectedObjList(clonedDepDocBOId, clonedBusMinorObjId, rel.getName(), "",
							savedAsObjList, processedRelIds);

					//[NDM] : L86 end

				}
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.DependentDocsCopyingFailed")
					+e.getMessage(), e);
		}
	}

	/**
	 * Returns a hashtable of new instances to be set on newly cloned family object
	 * contains the instances of the original object - if the instance also got saved as, the new name
	 * is sent back, otherwise the original name is retained.
	 */
	protected boolean getUpdatedInstanceInfoTable(Context _context, String familyID,BusinessObject familyBusObject,
			Hashtable retInstanceInfoTable,Hashtable retInstanceAttrInfoTable,
			Hashtable changedNameTable,String clonedObjName) throws Exception
			{
		boolean bRet = false;
		try
		{
			familyBusObject.open(_context);
			String famBusName = familyBusObject.getName();
			familyBusObject.close(_context);

			//get instance info table
			Hashtable instanceInfo = _generalUtil.getInstanceInfo(_context, familyBusObject);

			Enumeration allBusInstances = instanceInfo.keys();
			while(allBusInstances.hasMoreElements())
			{
				String instanceName = (String)allBusInstances.nextElement();

				IEFXmlNode instanceInfoNode = (IEFXmlNode)instanceInfo.get(instanceName);
				// Check if the instance exists in the saved as list
				String IdAndInstanceName = getUniqueKeyStringForInstance(familyID,instanceName);
				String newInstanceName	  = null;

				if(_instanceOldNewNamesTable != null)
				{
					newInstanceName = (String)MCADUtil.getObject(_instanceOldNewNamesTable, IdAndInstanceName, isSystemCaseSensitive);
					if(newInstanceName != null)
					{
						if(MCADUtil.containsKey(instanceInfo, newInstanceName, isSystemCaseSensitive))
						{
							//check if this newInstanceName exists in _instanceOldNewNamesTable
							String key = getUniqueKeyStringForInstance(familyID,newInstanceName);
							String instNameIfExists = (String)MCADUtil.getObject(_instanceOldNewNamesTable, key, isSystemCaseSensitive);

							if(instNameIfExists == null)
							{
								//This is the case where a new name provided for an instance matches with the name
								//of an instance in the original family, for which no new name is provided. For example
								//there are 2 instances I1 and I2 and I1 is given a new name as I2 but no new name is
								//provided for instance I2.Throw Exception for this case
								Hashtable detailsTable = new Hashtable(2);
								detailsTable.put("INSTANCENAME", newInstanceName);
								detailsTable.put("FAMILYNAME", famBusName);
								MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.InstanceWithNameFoundInOrignalFamily",detailsTable), null);
							}
						}
					}
				}

				if ( _instanceOldNewNamesTable == null || newInstanceName == null )
				{
					// No entry, - the instance has NOT been saved as
					// Take the entry from the original object
					retInstanceInfoTable.put(instanceName,instanceInfoNode);
					//do the same for instance attributes
					IEFXmlNode instanceAttrInfoNode = null;
					if((instanceAttrInfoNode = (IEFXmlNode)MCADUtil.getObject(null, instanceName, isSystemCaseSensitive)) != null)
					{
						retInstanceAttrInfoTable.put(instanceName,instanceAttrInfoNode);
					}
				}
				else
				{
					//if retInstanceInfoTable already contains newInstanceName -> same name has been entered for two
					//or more instance nodes.Throw exception
					if(MCADUtil.containsKey(retInstanceInfoTable, newInstanceName, isSystemCaseSensitive))
					{
						//throw exception
						Hashtable detailsTable = new Hashtable(2);
						detailsTable.put("INSTANCENAME", newInstanceName);
						detailsTable.put("FAMILYNAME", famBusName);
						MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.InstanceNameNotUnique",detailsTable), null);
					}

					// New node is getting added in place of old node...
					// update the "renamed from" value for this instance in the infonode
					String renamedFrom = instanceInfoNode.getAttribute("renamedfrom");
					if(renamedFrom != null && renamedFrom.trim().length()== 0)
					{
						instanceInfoNode.setAttributeValue("renamedfrom",instanceName);
					}
					retInstanceInfoTable.put(newInstanceName,instanceInfoNode);

					//Attributes for this instance also need to be saved as
					//therefore update table for instance attributes info
					IEFXmlNode instanceAttrInfoNode = null;
					if((instanceAttrInfoNode = (IEFXmlNode)MCADUtil.getObject(null, instanceName, isSystemCaseSensitive)) != null)
					{
						retInstanceAttrInfoTable.put(newInstanceName,instanceAttrInfoNode);
					}

					// Remove the entry from instances table, as it has been saved as..
					MCADUtil.removeKey(_instanceOldNewNamesTable, IdAndInstanceName, isSystemCaseSensitive);

					bRet = true;
				}

				// update table for dep docs. Dep doc names are combination of family
				//and instance name,get the concatenated name for dep docs
				String oldDepDocName = getConcatenatedName(_context, famBusName, instanceName);
				String newDepDocName = "";
				String newInstNameAndDepDocName = "";
				if(_instanceOldNewNamesTable == null ||newInstanceName == null)
				{
					newDepDocName = getConcatenatedName(_context, clonedObjName, instanceName);
					newInstNameAndDepDocName = instanceName + "|" + newDepDocName;
				}
				else
				{
					newDepDocName = getConcatenatedName(_context, clonedObjName, newInstanceName);
					newInstNameAndDepDocName = newInstanceName + "|" + newDepDocName;
				}

				changedNameTable.put(oldDepDocName, newInstNameAndDepDocName);
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
		return bRet;
			}

	protected String getUUID()
	{
		UID uniqueID = new UID();
		return uniqueID.toString();
	}

	protected String getConcatenatedName(Context _context, String familyName,String instanceName)throws Exception
	{
		String retName = "";

		String[] args = new String[3];

		args[0] = instanceName;
		args[1] = familyName;

		try
		{
			retName = _util.executeJPO(_context, "MCADGenerateName", "runGenerateName",args);
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
		return retName;

	}

	/**
	 * Input : ParsedXML inNode - Node to be copied from
	 * Returns : cloned ParsedXML
	 */
	protected IEFXmlNode getNewInstanceNodeFomNode(IEFXmlNode inNode)
	{
		IEFXmlNodeImpl retNode = null;

		//going to clone the the input node
		retNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		retNode.setName("cadobject");

		//get the attributes on the node
		Hashtable attributesTable = inNode.getAttributes();

		//get unique cadid
		String cadId = getUUID();

		if(attributesTable.containsKey("cadid"))
		{
			attributesTable.remove("cadid");
		}
		attributesTable.put("cadid",cadId);
		//set the attributes on the retNode
		retNode.setAttributes(attributesTable);

		//check if the input node has children
		//if it has children, add them to the cloned node
		Enumeration childNodes = inNode.elements();
		while(childNodes.hasMoreElements())
		{
			IEFXmlNodeImpl childNode = (IEFXmlNodeImpl)childNodes.nextElement();
			retNode.addNode(childNode);
		}

		return retNode;
	}

	protected void validateInstance(Context _context, BusinessObject famObj, Hashtable busIDSaveAsNameTable) throws Exception
	{
		String errorMessage = "";

		String type = famObj.getTypeName();
		String famObjID = famObj.getObjectId(_context);

		if(!_util.isInitialState(_context, _globalConfig, famObjID) && !busIDSaveAsNameTable.containsKey(famObjID))
		{
			errorMessage =_serverResourceBundle.getString("mcadIntegration.Server.Message.SaveAsNotAllowedForInstanceOfFamilyTower");
			MCADServerException.createException(errorMessage, null);
		}
	}

	/**
	 * This method is used to connect the EC Part with cad Object during SaveAs operation
	 * This method will use the connection JPO used in EBOM Synchronization.
	 */
	protected void connectPartWithCADObject(Context _context, String busID, BusinessObject clonedBusObject) throws MCADException
	{
		try
		{
			if(_busIDPartIDTable.containsKey(busID))
			{
				String partID = (String)_busIDPartIDTable.get(busID);

				BusinessObject busObject = new BusinessObject(busID);
				busObject.open(_context);
				String typeName		= busObject.getTypeName();
				String sourceName	= _generalUtil.getCSENameForBusObject(_context, busObject);
				busObject.close(_context);

				clonedBusObject.open(_context);
				String clonedBusObjectID	= clonedBusObject.getObjectId();
				String clonedBusObjectName	= clonedBusObject.getName();
				clonedBusObject.close(_context);

				String [] init = new String[] {}; 
				String [] args = new String[11];

				args[0] = partID;
				args[1] = clonedBusObjectID;
				args[2] = clonedBusObjectName;
				args[3] = "" + !_util.isMajorObject(_context, busID);//_globalConfig.isMinorType(typeName); // [NDM] OP6
				args[4] = _ebomConfigObj.getTypeName();
				args[5] = _ebomConfigObj.getName();
				args[6] = _ebomConfigObj.getRevision();
				args[7] = _serverResourceBundle.getLanguageName();
				args[8] = sourceName;

				String[] packedGCO = new String[2];
				packedGCO = JPO.packArgs(_globalConfig);
				args[9]  = packedGCO[0];
				args[10] = packedGCO[1];

				String jpoName 		= _ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_CONNECT_PARTS_JPO);
				String jpoMethod	= "connectPartWithCADObject";

				Hashtable createdPartIdMessageDetails = (Hashtable) JPO.invoke(_context, jpoName, init, jpoMethod, args, Hashtable.class);	
			}
		}
		catch (Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
	}

	/**
	 * This method is used to connect the newly created Object with the selected folder durign saveas 
	 * operation
	 */
	protected void connectToFolder(Context _context, String busID, BusinessObject clonedBusObject) throws MCADException
	{
		try
		{
			if(_busIDFolderInfoTable.containsKey(busID))
			{
				Vector folderInfo	= (Vector)_busIDFolderInfoTable.get(busID);
				String folderID		= (String)folderInfo.get(0);
				String applyToChild	= (String)folderInfo.get(1);

				clonedBusObject.open(_context);
				String clonedBusName		= clonedBusObject.getName();
				String clonedBusType		= clonedBusObject.getTypeName();
				String clonedBusRevision	= clonedBusObject.getRevision();
				clonedBusObject.close(_context);

				boolean hasAssignedFolders = _folderUtil.hasAssignedFolders(_context, folderID , clonedBusType, clonedBusName, clonedBusRevision);

				if(!hasAssignedFolders)
				{
					_folderUtil.assignToFolder(_context, clonedBusObject, folderID, applyToChild);
				}
			}
		}
		catch (Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
	}

	/**
	 *  This method will Create the information for EBOM Synchronization for selected root nodes
	 *  This method will return clonedObjectID or objectID1|objectID2|...
	 */
	protected String getEBOMSynchInfo(Context _context, String busID, BusinessObject clonedBusObject) throws MCADException
	{
		String ebomSynchObjectIDInfo = "";

		try
		{
			if(_globalConfig.isEBOMSynchOnDesignCreation())
			{
				clonedBusObject.open(_context);
				String clonedObjectId		= clonedBusObject.getObjectId();
				String clonedObjectcadType	= _util.getCADTypeForBO(_context, clonedBusObject);					
				clonedBusObject.close(_context);

				BusinessObject busObject = new BusinessObject(busID);

				//Root node is cloned				
				if(clonedObjectId !=null && !"".equals(clonedObjectId) && !clonedObjectId.equals(_busObjectID))
				{					
					if (!_globalConfig.isTypeOfClass(clonedObjectcadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
					{
						if(isEBOMSynchAllowed(_context, clonedBusObject))
						{
							ebomSynchObjectIDInfo = clonedObjectId;						
						}
					}
					else if(_globalConfig.isTypeOfClass(clonedObjectcadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
					{
						StringBuffer clonedInstanceIds = new StringBuffer();

						//TODO: LGE This method also used instead of following method _generalUtil.getInstanceListForFamilyObject(context, bo);				
						java.util.Hashtable instObjects = _generalUtil.getAllWheareUsedObjects(_context, busObject, true, MCADServerSettings.FAMILY_LIKE);
						Enumeration allInstances		= instObjects.keys();

						while(allInstances.hasMoreElements())
						{
							BusinessObject childBusinessObject = (BusinessObject)allInstances.nextElement();
							String childBusID = childBusinessObject.getObjectId();

							if(sharedPartsIDClonedBusObjectsTable.containsKey(childBusID))
							{
								BusinessObject clonedInstanceBusObject = (BusinessObject)sharedPartsIDClonedBusObjectsTable.get(childBusID);
								String clonedInstanceBusObjectId = clonedInstanceBusObject.getObjectId();

								if(isEBOMSynchAllowed(_context, clonedInstanceBusObject))
								{
									clonedInstanceIds.append(clonedInstanceBusObjectId);
									clonedInstanceIds.append("|");
								}

							}
						}

						ebomSynchObjectIDInfo = clonedInstanceIds.toString();
					}
				}
			}
		}
		catch (Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

		return ebomSynchObjectIDInfo;
	}

	/**
	 *  This method will check if EBOM Synch can be done or not
	 */
	protected boolean isEBOMSynchAllowed(Context _context, BusinessObject busObject) throws MCADException
	{
		boolean result = true;
		try
		{
			busObject.open(_context);
			String design	= busObject.getTypeName();
			String cadType	= _util.getCADTypeForBO(_context, busObject);
			busObject.close(_context);

			if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
			{
				result = false;
			} 
			else
			{
				Vector invalidTypesList	= _ebomConfigObj.getAttributeAsVector(IEFEBOMConfigObject.ATTR_INVALID_TYPES, "\n");				
				if(invalidTypesList.contains(design))
				{
					result = false;
				}
			}			
		}
		catch (Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

		return result;
	}


	private boolean isNameAndTitleMatch(String busName, String busTitle)
	{
		boolean isMatch = false;
		int    dotIndex             = busTitle.lastIndexOf(".");
		if(dotIndex != -1)
		{
			String busWithoutExtension = busTitle.substring(0, dotIndex);
			if(busWithoutExtension.equalsIgnoreCase(busName))
				isMatch = true;
		}
		return isMatch;
	}

	private  boolean isValidForSaveAs(Context context, String newTitle, String familyId) throws Exception
	{
		boolean isValid = true;
		String attrTitle              = MCADMxUtil.getActualNameForAEFData(context,"attribute_Title");	
		List instanceIdList           = _generalUtil.getFamilyStructureRecursively(context, new String[] {familyId}, new Hashtable(),null);
		HashMap instanceIDTitleMap    = _util.getAttributeForBusids(context, new HashSet(instanceIdList), attrTitle);
		Collection instancesTitleList = instanceIDTitleMap.values();

		if(instancesTitleList.contains(newTitle))
			isValid = false;

		return isValid;
	}

}
