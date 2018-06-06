/*
 **  MCADEBOMSynchronizeBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  JPO for performing EBOM synchronization.
 */
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.List;
import java.util.Map;

import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.ExpandRelationship;
import matrix.db.ExpandRelationshipItr;
import matrix.db.ExpandRelationshipList;
import matrix.db.Expansion;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.db.Visuals;
import matrix.util.MatrixException;
import matrix.util.StringList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.MCADIntegration.utils.MCADXMLUtils;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.Policy;
import matrix.db.State;

public class MCADEBOMSynchronizeBase_mxJPO extends IEFCommonUIActions_mxJPO
{	
	protected final String MATCH_CADMODEL_REV		= "MATCH_CADMODEL_REV";
	protected final String LATEST_REV				= "LATEST_REV";
	

	protected String typeCADDrawing			= "";
	protected String relPartSpecification	= "";
	protected String attrMustInStructure	= "";
	protected String attrQuantity			= "";
	protected String attrCADType			= "";
	protected String relEBOM				= "";
	protected String relAssociateDrawing	= "";
	protected String attrBalloonNumbers		= "";
	
	

	protected IEFEBOMConfigObject ebomConfigObj		= null;
	protected boolean confAttrExpandEBOM			= false;
	protected boolean confAttrCreateNewPart			= false;
	protected boolean confAttrFailAtMissingPart		= false;
	protected Hashtable confAttrDwgRelationInfo		= null;
	protected String confAttrPartNumber				= "";
	protected String confAttrPartSeries				= "";
	protected String confAttrAssignPartToMajor		= "false";

	protected String _sourceName					= "";				
	protected HashMap cadNameBalloonNumberMap		= new HashMap();	
	protected Hashtable handledPartObjIdsTable		= null;
	protected Vector partsToBeCreated				= null;
	protected Vector relDetailsToNavigateList		= null;
	protected Vector invalidTypesList				= null;
	protected Hashtable partsTable					= null;
	protected String partIdForBalloonNumberUpdate	= null;
	protected String updateFindNumber				= "false";

	protected Hashtable busIdAutoNameTable			= new Hashtable(); 
	protected Hashtable instIdFamIdTable			= new Hashtable(); 
	private String lastparentCADObjId = new String();

	protected String EBOMRelIsRemovedPartObjIds = "";

	protected static final String PART_CREATED = "PartCreated";
	protected static final String PART_UPDATED = "PartUpdated";
	protected static final String PART_FAILED = "FailedToCreatePart";

	protected String recursiveEBOMError = "";
	// CCI2 start
	private Map allCAdUUID = new HashMap<>();
	private Map allOldParentUUID = new HashMap<>();
	// CCI2 end
	public  MCADEBOMSynchronizeBase_mxJPO  () 
	{
	}

	public MCADEBOMSynchronizeBase_mxJPO (Context _context, String[] args) throws Exception
	{
		if (!_context.isConnected())
			MCADServerException.createException("not supported no desktop client", null);
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	// [NDM] Start
	/*public String canshowEBOMSynchronize(Context context, String []args) throws MCADException 
	{
		//_context = context;
		if(_util == null)
		{
			_util = new MCADMxUtil(context, new MCADServerResourceBundle(args[3]), new IEFGlobalCache());
		}

		if(isAEFInstalled(context))
			return "true";
		else
			return "false";
	}

	// Business Logic for implementing
	protected void canPerformOperationCustom(Context context, Hashtable resultDataTable) throws MCADException
	{
		//System.out.println("[MCADEBOMSynchronize.canPerformOperationCustom] :...");
		/*try
        {
			//
        }
        catch(Exception e)
        {
            throw new MCADException("EBOMSynch Operation Cannot be performed because of an error condition :" + e.getMessage());
        }*/
	//}
	//[NDM] end
	protected void init(Context _context) throws Exception
	{
		typeCADDrawing			= MCADMxUtil.getActualNameForAEFData(_context, "type_CADDrawing");
		relPartSpecification	= MCADMxUtil.getActualNameForAEFData(_context, "relationship_PartSpecification");
		attrMustInStructure		= MCADMxUtil.getActualNameForAEFData(_context, "attribute_MustInStructure");
		attrQuantity			= MCADMxUtil.getActualNameForAEFData(_context, "attribute_Quantity");
		attrCADType				= MCADMxUtil.getActualNameForAEFData(_context, "attribute_CADType");
		relAssociateDrawing		= MCADMxUtil.getActualNameForAEFData(_context, "relationship_AssociatedDrawing");
		attrBalloonNumbers		= MCADMxUtil.getActualNameForAEFData(_context, "attribute_BalloonNumbers");

		relEBOM					= MCADMxUtil.getActualNameForAEFData(_context, "relationship_EBOM");

		String confObjTNR = _globalConfig.getEBOMRegistryTNR();
		StringTokenizer token = new StringTokenizer(confObjTNR, "|");
		if(token.countTokens() < 3)
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.EBOMRegistryNotDefined"), null);

		String confObjType			= (String) token.nextElement();
		String confObjName			= (String) token.nextElement();
		String confObjRev			= (String) token.nextElement();
		ebomConfigObj				= new IEFEBOMConfigObject(_context, confObjType, confObjName, confObjRev);

		confAttrExpandEBOM			= "false".equalsIgnoreCase(ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_GENERATE_COLLAPSED_EBOM));
		confAttrCreateNewPart		= "true".equalsIgnoreCase(ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_CREATE_NEW_PART));
		confAttrFailAtMissingPart	= "true".equalsIgnoreCase(ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_FAIL_ON_NOT_FINDING_PART));
		confAttrDwgRelationInfo		= ebomConfigObj.getAttributeAsHashtable(IEFEBOMConfigObject.ATTR_DRAWING_RELATION_INFO, "\n", "|");

		confAttrPartNumber			= ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_PART_NUMBER);
		confAttrPartSeries			= ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_PART_SERIES);
		confAttrAssignPartToMajor 	= "true";//ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_ASSIGN_PART_TO_MAJOR); [NDM]
		
		partsToBeCreated			= new Vector();
		handledPartObjIdsTable		= new Hashtable();
		partsTable					= new Hashtable();

		relDetailsToNavigateList	= ebomConfigObj.getAttributeAsVector(IEFEBOMConfigObject.ATTR_REL_DETAILS_TO_NAVIGATE, "\n");
		invalidTypesList			= ebomConfigObj.getAttributeAsVector(IEFEBOMConfigObject.ATTR_INVALID_TYPES, "\n");
	}

	// Entry point
	public void executeCustom(Context _context, Hashtable resultAndStatusTable)  throws MCADException
	{
		try
		{
			init(_context);

			String instanceName			= (String)_argumentsTable.get(MCADServerSettings.INSTANCE_NAME);
			if(_argumentsTable.get(MCADServerSettings.MCAD_HASHTABLE_FOR_EBOM_AUTONAME) != null)
				busIdAutoNameTable	= (Hashtable)_argumentsTable.get(MCADServerSettings.MCAD_HASHTABLE_FOR_EBOM_AUTONAME);
			
			if(_argumentsTable.get(MCADServerSettings.MCAD_HASHTABLE_FOR_EBOM_INSTID_FAMID) != null)
				instIdFamIdTable	= (Hashtable)_argumentsTable.get(MCADServerSettings.MCAD_HASHTABLE_FOR_EBOM_INSTID_FAMID);

			
			//[NDM] start
			//_busObject.close(_context);
			
			if(!_util.isMajorObject(_context, _busObjectID))
			{
				_busObject = _util.getMajorObject(_context,_busObject);
				//_busObjectID = _busObject.getObjectId();
			}
			
			_busObject.open(_context);
			String busTypeName			= _busObject.getTypeName();

			if(invalidTypesList.contains(busTypeName))
				MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.InvalidTypeForEBOMSynch"), null);


			/*if(_globalConfig.isMajorType(busTypeName) && !_generalUtil.isBusObjectFinalized(_context,_busObject))
			{
				_busObject = _util.getActiveMinor(_context,_busObject);
				if(_busObject == null)
					MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.CantEBOMSynchNotFinalizedNoMinor"), null);

				_busObject.open(_context);
				_busObjectID	= _busObject.getObjectId(_context);
				busTypeName		= _busObject.getTypeName();
				_busObject.close(_context);
			}
			else if(_globalConfig.isMinorType(busTypeName) && _generalUtil.isBusObjectFinalized(_context,_busObject))*/
			//{
				//_busObject = _util.getMajorObject(_context,_busObject);
				
				//_busObject.open(_context);
			//[NDM] End
				_busObjectID	= _busObject.getObjectId();
				busTypeName		= _busObject.getTypeName();
				_busObject.close(_context);
			//}

			if(traverseToModelIfDrawing(_context))
			{				
				Vector allFirstlevelRelChildOfDrw =  getRelatedObjectsForDrawing(_context, _busObject.getTypeName());
				if(allFirstlevelRelChildOfDrw.size()==0)
				{	
					verifyAndDoEBOM(_context, _busObject,instanceName,resultAndStatusTable);
				}
				else
				{	

					for(int i = 0; i<allFirstlevelRelChildOfDrw.size();i++)
					{
						String row				= (String)allFirstlevelRelChildOfDrw.elementAt(i);
						Enumeration rowElements = MCADUtil.getTokensFromString(row, "|");
						String busObjectID		= (String)rowElements.nextElement();
						instanceName			= (String)rowElements.nextElement();						
						BusinessObject busObj	= new BusinessObject(busObjectID);
						verifyAndDoEBOM(_context, busObj,instanceName,resultAndStatusTable);
					}
				}
			}
			else
			{				
				verifyAndDoEBOM(_context, _busObject,instanceName,resultAndStatusTable);
			}			
		}
		catch(Exception e)
		{
			String error = e.getMessage();
			resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_STATUS, "false");

			if(error == null)
				error = "";

			resultAndStatusTable.put(MCADServerSettings.JPO_STATUS_MESSAGE, error);
			MCADServerException.createException(error, e);
		}

	}

	protected boolean traverseToModelIfDrawing(Context _context) throws Exception
	{

		boolean isDrawing		= false;
		String busType			= _busObject.getTypeName();
		String versionedType	= _util.getCorrespondingType(_context,busType);
		if((versionedType != null && confAttrDwgRelationInfo.containsKey(versionedType)) || confAttrDwgRelationInfo.containsKey(busType))
		{
			isDrawing = true;
		}

		return isDrawing;
	}

	protected boolean traverseToModelIfDrawing(Context _context, String busId) throws Exception
	{
		boolean isDrawing		= false;
		DomainObject busObject = DomainObject.newInstance(_context, busId);
		
		String busType = busObject.getInfo(_context, DomainObject.SELECT_TYPE);
		String versionedType	= _util.getCorrespondingType(_context,busType);
		
		if((versionedType != null && confAttrDwgRelationInfo.containsKey(versionedType)) || confAttrDwgRelationInfo.containsKey(busType))
		{
			isDrawing = true;
		}

		return isDrawing;
	}

	private String getIfRelatedDocumentExists(Context _context, String typeName, String childBusAndRelIds,StringBuffer childBusAndRelIdsBuffer,ArrayList busObjIDsList,ArrayList typeList) throws Exception
	{
		String childBusAndRelId								= "";
		String versionedType								= _util.getCorrespondingType(_context,typeName);

		if((versionedType != null && confAttrDwgRelationInfo.containsKey(versionedType)) &&(!confAttrDwgRelationInfo.containsKey(typeName)))
			typeName=versionedType;
		String relationDetails								= (String)confAttrDwgRelationInfo.get(typeName);
		StringTokenizer tokenizer							= new StringTokenizer(relationDetails, ",");
		String relName										= (String)tokenizer.nextElement();
		String direction									= (String)tokenizer.nextElement();
		Hashtable relsAndEnds								= new Hashtable(3);
		relsAndEnds.put(relName,direction);

		Vector relAtrNames									= new Vector(2);

		StringTokenizer strTok								= new StringTokenizer(childBusAndRelIds, "\n");
		while(strTok.hasMoreTokens())
		{
			String row = strTok.nextToken();

			Enumeration rowElements = MCADUtil.getTokensFromString(row, "|");
			String level                = (String)rowElements.nextElement();
			String resultRelName        = (String)rowElements.nextElement();
			String resultRelDirection   = (String)rowElements.nextElement();
			String childObjectId        = (String)rowElements.nextElement();
			String relId                = (String)rowElements.nextElement();

			BusinessObject model		= new BusinessObject(childObjectId);
			model.open(_context);
			String typeString			= model.getTypeName();
			String cadType				= _util.getCADTypeForBO(_context, model);
			model.close(_context);
			if ("presentation".equalsIgnoreCase(cadType) || "drawing".equalsIgnoreCase(cadType))
			{
				busObjIDsList.add(childObjectId);
				typeList.add(typeString);
			}
			else
			{
				childBusAndRelIdsBuffer.append(row+"\n"); 
			}
		}
		String tempString = "";
		if (busObjIDsList.size() > 0)
		{
			for (int i=0;i<busObjIDsList.size() ;i++ )
			{
				childBusAndRelId				= _generalUtil.getFilteredFirstLevelChildAndRelIds(_context,(String)busObjIDsList.get(i),false,relsAndEnds,null,true,relAtrNames);
				tempString						= (String)typeList.get(i);
				busObjIDsList.remove(i);
				typeList.remove(i);
				getIfRelatedDocumentExists(_context, tempString, childBusAndRelId,childBusAndRelIdsBuffer,busObjIDsList,typeList);
			}
		}

		return childBusAndRelIdsBuffer.toString();
	}

	protected Vector getRelatedObjectsForDrawing(Context _context, String typeName) throws Exception
	{
		String versionedType	= _util.getCorrespondingType(_context,typeName);
		if((versionedType != null && confAttrDwgRelationInfo.containsKey(versionedType)) &&(!confAttrDwgRelationInfo.containsKey(typeName)))
			typeName=versionedType;

		String relationDetails		= (String)confAttrDwgRelationInfo.get(typeName);
		StringTokenizer tokenizer	= new StringTokenizer(relationDetails, ",");
		String relName		= (String)tokenizer.nextElement();
		String direction	= (String)tokenizer.nextElement();
		Hashtable relsAndEnds = new Hashtable(3);
		relsAndEnds.put(relName,direction);
		Vector allFirstlevelRelChildOfDrw = new Vector();
		Vector relAtrNames = new Vector(2);
		String childBusAndRelIds =  _generalUtil.getFilteredFirstLevelChildAndRelIds(_context,_busObjectID,false,relsAndEnds,null,true,relAtrNames);
		try 
		{
			StringBuffer childBusAndRelIdsBuffer = new StringBuffer();
			ArrayList busObjIDsList				 = new ArrayList();
			ArrayList typeList					 = new ArrayList();

			childBusAndRelIds = getIfRelatedDocumentExists(_context, typeName, childBusAndRelIds,childBusAndRelIdsBuffer,busObjIDsList,typeList);
		} 
		catch (Exception e)
		{
			e.getMessage();
		}

		StringTokenizer strTok = new StringTokenizer(childBusAndRelIds, "\n");
		// ACTION : Need to handle case when there are more than 1 models
		// connected with the drawing. Code below needs to work on  each node.
		while(strTok.hasMoreTokens())
		{
			String row = strTok.nextToken();

			Enumeration rowElements = MCADUtil.getTokensFromString(row, "|");
			String level                = (String)rowElements.nextElement();
			String resultRelName        = (String)rowElements.nextElement();
			String resultRelDirection   = (String)rowElements.nextElement();
			String childObjectId        = (String)rowElements.nextElement();
			String relId                = (String)rowElements.nextElement();
			// Rel attributes sequence is important below.
			// It has to be consistent with the way query was set
			String childInstanceName    = "";
			if (rowElements.hasMoreElements())
			{
				childInstanceName    = (String)rowElements.nextElement();
			}
			BusinessObject model = new BusinessObject(childObjectId);

			validateDrawingForEBOM(_context,model);

			model.open(_context);
			
			//commented the condition after && operator for IR IR-117367V6R2013
			//Changes are done for issue IR-360545-V6R2013x, If Drawing does not have any model attached then create Part of Drawing
			//, for all other cases crete part of Model and attach drawing to it.
			if(model != null)// && (_generalUtil.isBusObjectFinalized(_context,_busObject) || !_generalUtil.isBusObjectFinalized(_context, model)))
			{
				String busOjectInstanceNameMapping = childObjectId + "|" + childInstanceName;  //[NDM]
				allFirstlevelRelChildOfDrw.addElement(busOjectInstanceNameMapping);

			}
			model.close(_context);
		}
		return allFirstlevelRelChildOfDrw;
	}

	/**
	 * @param context
	 * @param model
	 * @throws MatrixException
	 * @throws MCADException
	 * @throws MCADServerException
	 */
	protected void validateDrawingForEBOM(Context context, BusinessObject model) throws MatrixException, MCADException, MCADServerException
	{
		String PART_RELEASE_STATE 		= ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_RELEASE_PART_STATE);
		
		//BusinessObject majorObject      = _util.getMajorObject(context, _busObject); //[NDM]

		//if(majorObject == null)
		BusinessObject majorObject = _busObject;

		String REVISIONS 						= "revisions";
		String ID_ON_REVISIONS 					= "revisions.id";

		StringList busSelectionList = new StringList(2);
		busSelectionList.add(REVISIONS);
		busSelectionList.add(ID_ON_REVISIONS);
		
		BusinessObjectWithSelectList busWithSelectionList	= BusinessObject.getSelectBusinessObjectData(context, new String[]{majorObject.getObjectId(context)}, busSelectionList);
		BusinessObjectWithSelect busObjectWithSelect		= (BusinessObjectWithSelect)busWithSelectionList.elementAt(0);

		StringList majorRevisions							= busObjectWithSelect.getSelectDataList(REVISIONS);
		String partIdOfModel 								= getRelatedEBOMPart(context, model.getObjectId(context),"","");

		for(int j = majorRevisions.size()-1 ; j > -1 ; j--)
		{
			String prviousDrwRevisionId 			 = busObjectWithSelect.getSelectData("revisions[" + majorRevisions.get(j) + "].id");
			
			BusinessObject previousDrwRevisionObject = new BusinessObject(prviousDrwRevisionId);
			
			String partIdOfPreviousRevision = getRelatedEBOMPart(context, prviousDrwRevisionId, "","");

			if(partIdOfPreviousRevision != null && !"".equals(partIdOfPreviousRevision))
			{
				//to check previous revision in released state or not.
				Policy policy			 			= previousDrwRevisionObject.getPolicy(context);
				String policyName 					= policy.getName();
				String releasedState	 			= _globalConfig.getReleasedState(policyName);
				State stateofPrevisousRevision 		= _util.getCurrentState(context,previousDrwRevisionObject);
				String currentStateName 			= stateofPrevisousRevision.getName();

				boolean isPrevisiousRevisionReleased = currentStateName.equals(releasedState);

				//to check  model is in released state or not.
				policy						 = model.getPolicy(context);
				policyName	 		 		 = policy.getName();
				releasedState	 			 = _globalConfig.getReleasedState(policyName);
				State currentStateofModel	 = _util.getCurrentState(context,model);
				currentStateName 	 		 = currentStateofModel.getName();

				boolean isModelReleased = currentStateName.equals(releasedState);

				if(!_generalUtil.isBusObjectFinalized(context,_busObject) 
						&& isModelReleased &&  isPrevisiousRevisionReleased 
						&& partIdOfModel != null && partIdOfPreviousRevision != null
						&& partIdOfModel.equalsIgnoreCase(partIdOfPreviousRevision)
						&& _generalUtil.isPartReleased(context, partIdOfModel, PART_RELEASE_STATE)) 
				{
					String busType	= _busObject.getTypeName();
					String busName	= _busObject.getName();
					String busRev 	= _busObject.getRevision();

					Hashtable tokensTable = new Hashtable();
					tokensTable.put("TYPE", busType);
					tokensTable.put("NAME", busName);
					tokensTable.put("REVISION", busRev);

					String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.UnfinalizedDrawingEBOMSynchronizeRestricted", tokensTable);
					MCADServerException.createException(errorMessage, null);
				}

				break;
			}
		}
	}
	
	protected String getRelatedEBOMPart(Context _context, String busId, String busName, String instanceName)
	{       				
		String partId = "";
		try
		{	//[NDM]
			/*String assignPartToMajor = ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_ASSIGN_PART_TO_MAJOR);

			if("true".equalsIgnoreCase(assignPartToMajor))
			{
				BusinessObject busObj = new BusinessObject(busId);
				busObj.open(_context);
				BusinessObject majorObj = _util.getMajorObject(_context, busObj);			
				if(majorObj!= null)
				{
					majorObj.open(_context);
					busId = majorObj.getObjectId();
					majorObj.close(_context);
				}
				busObj.close(_context);
			}*/
			//[NDM] end
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
			String retVal = _util.executeMQL(_context, "expand bus $1 $2 select rel $3 where $4 dump $5", Args); 
			
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
			System.out.println("[MCADEBOMSynchronize : getRelatedEBOMPart] : Exception occurred " + e.getMessage());
		}

		return partId;
	}

	protected Hashtable getRelsAndEnds(Vector relInfoList)
	{
		Hashtable relsAndEnds = new Hashtable();

		for (int i = 0; i < relInfoList.size(); i++)
		{
			String details = (String) relInfoList.elementAt(i);
			StringTokenizer tokenizer = new StringTokenizer(details, ",");
			String relName = "";
			String end = "";

			if (tokenizer.hasMoreTokens())
				relName = tokenizer.nextToken();
			if (tokenizer.hasMoreTokens())
				end = tokenizer.nextToken();

			if (end.equals("from"))
			{
				relsAndEnds.put(relName, "to");
			}
			else
			{
				relsAndEnds.put(relName, "from");
			}

		}
		return relsAndEnds;
	}

	// Builds corresponding BOM tree by connecting Part objects via EBOM relationship.
	// The tree is simillar to CAD Assembly tree
	protected String RecursiveBom(Context _context, String parentPartObjId, Vector cadRelIds, String cadObjId, String cadBusName, String instanceName, 
			Hashtable relsAndEnds, String busTypeName, StringBuffer ebomDoneObjectBuffer,boolean isParentFantom,String parentCadObjID,String childObjId) throws Exception
			{
		String partObjId							= new String(parentPartObjId);
		Vector originalChildPartIds					= null;
		Vector latestChildPartIds					= null;
		String parentChildFindNumber				= null;
		String rootBusId							= null;
		boolean hasConnectedDrwWithBalloonNumber	= false;
		String excludeFromBOM 	= _util.getAttributeForBO(_context, cadObjId, MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-ExcludeFromBOM"));
		boolean isFantomNode	=  "true".equalsIgnoreCase(excludeFromBOM);
		try
		{
			try
			{
				if(null != cadObjId)
				{
					BusinessObject cadObject			= new BusinessObject(cadObjId);
					String cadType		= _util.getCADTypeForBO(_context,cadObject);
					if(excludeFromBOM != null && "false".equalsIgnoreCase(excludeFromBOM))
					{
						rootBusId							= _busObject.getObjectId(_context);
						
						if(parentCadObjID != null && !parentCadObjID.isEmpty())
						hasConnectedDrwWithBalloonNumber	= getBalloonNumberFromAssociatedDrawing(_context, cadObjId, parentCadObjID);
						else
							hasConnectedDrwWithBalloonNumber = true;
						
						if( parentPartObjId.length() == 0 || (partIdForBalloonNumberUpdate != null && parentPartObjId.equals(partIdForBalloonNumberUpdate)) )
						{
							updateFindNumber = "true";
						}
						else
						{
							updateFindNumber = "false";
						}
	
						
						partObjId = createNewPartSpec(_context, cadObjId,cadBusName,instanceName, busTypeName, cadType, ebomDoneObjectBuffer);
						//existing expandbom(CollapsedEBOM) atribute has reverse value.
						//Not referred.but protected.
						boolean isCollapsedEBOM = "TRUE".equalsIgnoreCase(ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_GENERATE_COLLAPSED_EBOM));
						//Reseting Qty attribute if collapseBOM=True
						// IR-351306
						/*if(isCollapsedEBOM)
						{
							resetQuantity(_context,partObjId);
						}*/
						StringBuffer keyWithNameBuffer = new StringBuffer();
						StringBuffer keyWithTitleBuffer = new StringBuffer();
						keyWithNameBuffer.append("name");
						keyWithNameBuffer.append("|");
						keyWithNameBuffer.append(cadBusName);
						keyWithNameBuffer.append("|");
						keyWithNameBuffer.append(cadType);
						keyWithNameBuffer.append("|");
						keyWithNameBuffer.append(parentCadObjID);
	
						String titleAttribute	= MCADMxUtil.getActualNameForAEFData(_context,"attribute_Title");
						String titleValue		= _util.getAttributeForBO(_context, cadObject, titleAttribute);
						keyWithTitleBuffer.append("title");
						keyWithTitleBuffer.append("|");
						keyWithTitleBuffer.append(titleValue);
						keyWithTitleBuffer.append("|");
						keyWithTitleBuffer.append(cadType);
						keyWithTitleBuffer.append("|");
						keyWithTitleBuffer.append(parentCadObjID);


						String keyWithName = keyWithNameBuffer.toString();
						String keyWithTitle = keyWithTitleBuffer.toString();


						if(_globalConfig != null && _globalConfig.isObjectAndFileNameDifferent())
						{
							if(cadNameBalloonNumberMap.containsKey(keyWithTitle))
						{					  
								parentChildFindNumber = (String)cadNameBalloonNumberMap.get(keyWithTitle);
							}
						}
						if(cadNameBalloonNumberMap.containsKey(keyWithName))
						{					  
							parentChildFindNumber = (String)cadNameBalloonNumberMap.get(keyWithName);
						}
					}
					else
					{
						//Already EBOM Done but now Excluded.check on cadobjid whether it has Part connected.
						//if yes then 1.disconnect Part Specification 2.disconnect EBOM child parts 
						BusinessObjectList relatedPartsList = getRelatedPartList(_context, cadObject);
						if(relatedPartsList != null && relatedPartsList.size() > 0)
						{
							BusinessObjectItr partListItr 	= new BusinessObjectItr(relatedPartsList);
							
							while(partListItr.next())
							{                	
								BusinessObject partObject = partListItr.obj(); 
								String partObjectId = partObject.getObjectId(_context);  
								//1.disconnect Part Specification
								_util.disconnectBusObjects(_context, partObjectId, cadObjId, relPartSpecification, true);
								//2.disconnect Parent EBOM
								if(!"".equals(parentPartObjId))
									_util.disconnectBusObjects(_context,parentPartObjId,partObjectId,relEBOM, true);
								//3.disconnect EBOM child parts 
								BusinessObjectList relatedChildPartsList = new BusinessObjectList();  
								relatedChildPartsList		= _util.getRelatedBusinessObjects(_context,partObject,relEBOM,"from");
								BusinessObjectItr childPartItr 	= new BusinessObjectItr(relatedChildPartsList);
								
								while(childPartItr.next())
								{
									BusinessObject childPartObject = childPartItr.obj(); 
									String childPartObjectId = childPartObject.getObjectId(_context); 
									if(_util.doesRelationExist(_context,childPartObjectId,partObjectId,relEBOM))
									{
										String Args[] = new String[5];
										Args[0] = partObjectId;
										Args[1] = "relationship";
										Args[2] = "EBOM";
										Args[3] = "to";
										Args[4] = childPartObjectId;
										_util.executeMQL(_context, "disconnect bus $1 $2 $3 $4 $5", Args);
									}
								}
								//deleteUnWantedParts(_context, partObjectId, null, childPartIdVector);
							}
						}
					}
					
					
				}//if ends
			}
			catch(MCADException e)
			{
				recursiveEBOMError = recursiveEBOMError + "<br>" + e.getMessage();
			}

			if(partObjId != null && !"".equals(partObjId))
			{
				if(excludeFromBOM == null || (excludeFromBOM != null && "false".equalsIgnoreCase(excludeFromBOM)))
				{
					BusinessObject partObject		= new BusinessObject(partObjId);
					partObject.open(_context);
					if(!partObjId.equalsIgnoreCase(parentPartObjId))
						originalChildPartIds		= getFirstLevelChildPartObjIds(_context, partObject);
					String stableEBOMRelEnabled 			 = isStableEBOMRelEnabled();

					if (!lastparentCADObjId.equals(parentCadObjID)) {
					if("true".equals(stableEBOMRelEnabled)){
						allCAdUUID = getAllUUIDfromCadID(_context, parentCadObjID);
						allOldParentUUID = getAllUUIDfromPartID(_context, parentPartObjId);
					}
						lastparentCADObjId = parentCadObjID;
					}
					String ceratedEBOM = createNewEBOM(_context, parentPartObjId, partObjId, cadRelIds, parentChildFindNumber, isParentFantom,parentCadObjID, childObjId);
					if (ceratedEBOM.equalsIgnoreCase("true"))
 {
						ebomDoneObjectBuffer.append(cadBusName + ",");
					}
				}

				// Update metadata count, for progress bar update
				incrementMetaCurrentCount();
			}
			

			// Check whether user aborted the operation, in that case throw error
			if(isOperationCancelled())
			{
				String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.UserCancelledTheOperation");
				MCADServerException.createException(errorMessage, null);
			}			

			Hashtable childAndRelIds = _generalUtil.getFirstLevelChildAndRelIds(_context, cadObjId, true, relsAndEnds, null);			

			if(childAndRelIds.size() > 0)
			{
				latestChildPartIds = new Vector();

				for (Enumeration childIds = childAndRelIds.keys() ; childIds.hasMoreElements() ;)
				{
					String childId      = (String) childIds.nextElement();
				
					String excludeFromBOMChild 	= _util.getAttributeForBO(_context, childId, MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-ExcludeFromBOM"));
					getChildRelIds(_context, relsAndEnds, childAndRelIds, childId, excludeFromBOMChild);
				}

				for (Enumeration childIds = childAndRelIds.keys() ; childIds.hasMoreElements() ;)
				{
					String childId      = (String) childIds.nextElement();
					Vector relIdsVector = (Vector) childAndRelIds.get(childId);

					String excludeFromBOMChild 	= _util.getAttributeForBO(_context, childId, MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-ExcludeFromBOM"));

					if(excludeFromBOMChild == null || (excludeFromBOMChild != null && "true".equalsIgnoreCase(excludeFromBOMChild)))
						continue;
			
					BusinessObject childBusObj = new BusinessObject(childId);
					childBusObj.open(_context);
					String busName		= childBusObj.getName();
					String busRevision	= childBusObj.getRevision();
					String childTypeName = childBusObj.getTypeName();

					validateRelationships(_context, childTypeName, busName, busRevision, relIdsVector);

					if(!_util.isMajorObject(_context, childId))//_globalConfig.isMinorType(childBusObj.getTypeName())) //[NDM] OP6
					{
						BusinessObject majorBusObject = _util.getMajorObject(_context, childBusObj);

						if(majorBusObject == null)
						{
							Hashtable messageDetails = new Hashtable(2);
							messageDetails.put("NAME", busName);

							MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.CannotPerformOperationAsMajorAbsentOrNotAccessible", messageDetails), null);
						}
					}

					childBusObj.close(_context);

									
					if(invalidTypesList.contains(childTypeName) || isProESkeletonModel(_context, childBusObj))
						continue;
					
					// relationship based exclude from BOM check
					String excludeFromBOMOnRel  =  isChildExcludedFromBOM(_context, relIdsVector,partObjId,childBusObj,confAttrExpandEBOM);

					//relationship has e
					if("true".equalsIgnoreCase(excludeFromBOMOnRel))
						continue;

					if(hasConnectedDrwWithBalloonNumber)
					{
						partIdForBalloonNumberUpdate = partObjId;
					}
					
					List relFilterList = applyRelationshipFilter(_context, relIdsVector);
					if(relFilterList != null && relFilterList.size() > 0)
					{
						String correspondingPartID = RecursiveBom(_context, partObjId, relIdsVector, childId, busName, null, relsAndEnds, childTypeName, ebomDoneObjectBuffer,isFantomNode,cadObjId,childId);
						
                                                latestChildPartIds.addElement(correspondingPartID);
					}
				}
			}
			// Cleanup - There may be components deleted from an assembly that got EBOM synched.
			// If such assembly had EBOM done earlier too, then Part corresponding to the deleted component
			// should be removed from the Assembly Part EBOM.
			if (originalChildPartIds != null)
			{
				deleteUnWantedParts(_context, partObjId, latestChildPartIds, originalChildPartIds);
			}
		}
		catch(MCADException mcade)
		{
			MCADServerException.createException(mcade.getMessage(), mcade);	
		}
		catch (Exception e)
		{
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.RecursiveLinkCreationFailed") + e.getMessage(), e);
		}

		return partObjId;
			}

	private void getChildRelIds(Context _context, Hashtable relsAndEnds, Hashtable childAndRelIds, String childId, String excludeFromBOM) throws MCADException 
	{	
		if(excludeFromBOM == null || (excludeFromBOM != null && "true".equalsIgnoreCase(excludeFromBOM)))
		{
			Hashtable childAndRelIdsTable = _generalUtil.getFirstLevelChildAndRelIds(_context, childId, true, relsAndEnds, null);

			for (Enumeration childIds = childAndRelIdsTable.keys() ; childIds.hasMoreElements() ;) 
			{
				String childID      = (String) childIds.nextElement();

				String excludeFromBOMChild 	= _util.getAttributeForBO(_context, childID, MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-ExcludeFromBOM"));

				getChildRelIds(_context, relsAndEnds, childAndRelIds, childID, excludeFromBOMChild);

				Vector relIdsVector = (Vector) childAndRelIdsTable.get(childID);

				if(excludeFromBOMChild == null || (excludeFromBOMChild != null && "false".equalsIgnoreCase(excludeFromBOMChild)))
				{
					if(childAndRelIds.containsKey(childID))
					{
						Vector relIds = (Vector) childAndRelIds.get(childID);
						relIds.addAll(relIdsVector);
						childAndRelIds.put(childID, relIds);
					}
					else
						childAndRelIds.put(childID, relIdsVector);
				}
			}
		}
	}

	// Delete the parts which are in asStoredChildIdVector, but NOT in latestChildIdVector
	// Delete their EBOM link with parentPartObdId
	// Note: latestChildIdVector can be null also
	protected void deleteUnWantedParts(Context _context, String parentPartObdId, Vector latestChildIdVector,Vector asStoredChildIdVector) throws Exception
	{
		if(parentPartObdId != null && parentPartObdId.length() >0)
		{			
			for(int i = 0 ; i < asStoredChildIdVector.size(); i++)
			{
				String childObjid = (String)asStoredChildIdVector.elementAt(i);
				if(!childObjid.equals("") && (latestChildIdVector == null || !latestChildIdVector.contains(childObjid)) )
				{

					if(_util.doesRelationExist(_context,childObjid,parentPartObdId,relEBOM))
					{
						String Args[] = new String[5];
						Args[0] = parentPartObdId;
						Args[1] = "relationship";
						Args[2] = "EBOM";
						Args[3] = "to";
						Args[4] = childObjid;
						_util.executeMQL(_context, "disconnect bus $1 $2 $3 $4 $5", Args);
	
						EBOMRelIsRemovedPartObjIds += childObjid;
					}
				}
			}
		}
	}
	protected void validateRelationships(Context _context, String busType, String busName, String busRevision, Vector relIdsVector) throws MCADException
	{
		if(relIdsVector != null && relIdsVector.size() > 0)
		{
			for (int i=0; i < relIdsVector.size(); i++)
			{
				String relId = (String)relIdsVector.elementAt(i);

				Relationship relationship		= new Relationship(relId);
				String attrRelModificaionStatus	= MCADMxUtil.getActualNameForAEFData(_context, "attribute_RelationshipModificationStatusinMatrix");
				String relModificaionStatus		= _util.getRelationshipAttributeValue(_context, relationship, attrRelModificaionStatus); 

				if(relModificaionStatus.equalsIgnoreCase("deleted"))
				{
					StringBuffer tnrStamp = new StringBuffer();
                                        try
					{
					busType = MCADMxUtil.getNLSName(_context,"Type", busType, "", "", _serverResourceBundle.getLanguageName());
					}
					catch(Exception e)
					{
						System.out.println("[MCADEBOMSynchronizeBase.validateRelationships] Exception occured while getting nls for type: " + e.getMessage());
					}
					

					tnrStamp.append("\"").append(busType).append("\" ");
					tnrStamp.append("\"").append(busName).append("\" ");
					tnrStamp.append("\"").append(busRevision).append("\"  ");

					MCADServerException.createException(tnrStamp.toString() + _serverResourceBundle.getString("mcadIntegration.Server.Message.CannotPerformOperationAsSomeRelationshipsDeleted"), null);

					break;
				}
			}
		}
	}

	protected String isChildExcludedFromBOM(Context _context, Vector relIdsVector,String parentPartObjId,BusinessObject childBus,boolean expandEBOM) throws Exception
	{
		String isExcludedFromBOM = "true";
		
		if(relIdsVector != null && relIdsVector.size() > 0)
		{
			for (int i=0; i < relIdsVector.size(); i++)
			{
				String relId = (String)relIdsVector.elementAt(i);

				Relationship relationship		= new Relationship(relId);
				String attrIEFExcludeFromBOM	= MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-ExcludeFromBOM");
				isExcludedFromBOM				= _util.getRelationshipAttributeValue(_context, relationship, attrIEFExcludeFromBOM); 

				if(isExcludedFromBOM.equalsIgnoreCase("false"))
				{
					isExcludedFromBOM = "false";
					break;
				}
			}
		}
		
		//All relationships excluded and Already EBOM then disconnect it.Partial disconnect is handled 
		//during Part to Part connect.
		if("true".equalsIgnoreCase(isExcludedFromBOM))
		{
			BusinessObjectList relatedPartsList = getRelatedPartList(_context, childBus);
			if(relatedPartsList != null && relatedPartsList.size() > 0)
			{
				BusinessObjectItr partListItr 	= new BusinessObjectItr(relatedPartsList);
				while(partListItr.next())
				{
					BusinessObject partObject = partListItr.obj(); 
					String partObjId = partObject.getObjectId(_context);  
					
					if(expandEBOM)
					{
						for(int i=0;i < relIdsVector.size();i++)
						{
							if(_util.doesRelationExist(_context,partObjId,parentPartObjId,relEBOM))
							_util.disconnectBusObjects(_context,parentPartObjId,partObjId,relEBOM, true);
						}
					}
					else
					{
						if(!"".equals(parentPartObjId) && !parentPartObjId.equals(partObjId) && _util.doesRelationExist(_context,partObjId,parentPartObjId,relEBOM))
							_util.disconnectBusObjects(_context,parentPartObjId,partObjId,relEBOM, true);
					}
				}
			}
		}
		
		return isExcludedFromBOM;
	}
     /** 
       * This method is provided for to customize the behavior of creating EC Part on EBOMSync
       * This validation can be based on the relationship attribute.
       * This method should return a List which will contain relationship ids for which the validation is suceessful.
       *
       * @param context  Context ENOVIA Context object reference
       * @param relIdsVector Vector This vector contains Relationship Id of the component with its parent
       */

	protected List applyRelationshipFilter(Context _context, Vector relIdsVector) throws MCADException
	{
		List filteredRelationships = new Vector();	
		filteredRelationships.add("dummy");
		return filteredRelationships;
	}

	// Create new Part Spec relation between Model and Part object.
	protected String createNewPartSpec(Context _context, String busObjectID, String busName, String instanceName, String busType, String cadType, StringBuffer ebomDoneObjectBuffer) throws Exception
	{
		String matchingPartID	= "";

		String PART_RELEASE_STATE 		= ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_RELEASE_PART_STATE);
		String matchingPartString 		= ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_PART_MATCHING_RULE);
		if(!handledPartObjIdsTable.containsKey(busObjectID))
		{
			StringBuffer cadObjName = new StringBuffer();
			matchingPartID = getMatchingPartObject(_context, instanceName, busObjectID, busName, cadObjName, busType, cadType);

			if(matchingPartID != null && matchingPartID.length() > 0)
			{	//IR-458535, IR-539331 : Check if the scenario is for CAD_MODEL_REV and EC Part is Released, No Update to be performed
				if(matchingPartString.equals(LATEST_REV) || (!_generalUtil.isPartReleased(_context, matchingPartID, PART_RELEASE_STATE) && matchingPartString.equals(MATCH_CADMODEL_REV))) {
					matchingPartID = connectPartWithCADObject(_context, matchingPartID, busObjectID, cadObjName.toString(), busType, ebomDoneObjectBuffer);
				}
			}   
		}
		else
		{
			String[] partDetails = (String[])handledPartObjIdsTable.get(busObjectID);
			matchingPartID = partDetails[0];
		}

		return matchingPartID;
	}

	protected String getUniqueKey(String busObjectID, String instanceName)
	{
		if(instanceName == null)
			instanceName = "";
		return busObjectID + ":" + instanceName;
	}

	protected String getMatchingPartObject(Context _context, String instanceName, String busObjectID, String busName, StringBuffer cadObjName, String busType, String cadType) throws Exception
	{
		// If a connection exists with a part, use it further ->
		String assignPartToMajor    = "true";//ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_ASSIGN_PART_TO_MAJOR); [NDM]
		
		String partObjId = "";
		
		if(traverseToModelIfDrawing(_context, busObjectID))
			partObjId = getRelatedEBOMPart(_context, busObjectID, busName, instanceName);
		else
			partObjId 			= _generalUtil.getRelatedEBOMPart(_context, busObjectID, cadType, assignPartToMajor,ebomConfigObj,true);
    
		if (partObjId == null || partObjId.length() <= 0)
		{
			if (instanceName != null && instanceName.length() > 0)
			{
				//Autoname:
				if(confAttrPartNumber.equalsIgnoreCase(IEFEBOMConfigObject.EBOM_PART_NUMBER_CONFIG_AUTONAME))
				{
					String partName  = "";
					if(busIdAutoNameTable.get(busObjectID) != null)
						partName = (String)busIdAutoNameTable.get(busObjectID);
					else
					{
						Vector autoNames = _util.getAutonames(_context, "type_Part", confAttrPartSeries, 1);
						partName  = (String)autoNames.get(0);
					}
					
					partObjId = createOrMatchPart(_context, partName, instanceName, busObjectID, busType, cadType);
				}
				else
				{
					//NOT considered exposition mode here since OOTB instanceName is always null
					String partName = getPartName(_context, busName, instanceName);
				partObjId = createOrMatchPart(_context, partName, instanceName, busObjectID, busType, cadType);			
				}
				cadObjName.append(instanceName);
			}
			else
			{
				//Autoname: change to constant
				if(confAttrPartNumber.equalsIgnoreCase(IEFEBOMConfigObject.EBOM_PART_NUMBER_CONFIG_AUTONAME))
				{
					String partName  = "";
					if(busIdAutoNameTable.get(busObjectID) != null)
						partName = (String)busIdAutoNameTable.get(busObjectID);
					else
					{
						Vector autoNames = _util.getAutonames(_context, "type_Part", confAttrPartSeries, 1);
						partName  = (String)autoNames.get(0);
					}
					
					partObjId = createOrMatchPart(_context, partName, instanceName, busObjectID, busType, cadType);
				}
				else
				{
					//OOTB CADMODEL
					String partName = busName;
					
					if(busIdAutoNameTable.get(busObjectID) != null)
						partName = (String)busIdAutoNameTable.get(busObjectID);
					else if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
					{
						String famID = "";
						if(instIdFamIdTable.containsKey(busObjectID))
							famID = (String)instIdFamIdTable.get(busObjectID);
						else
						{
							famID = _generalUtil.getTopLevelFamilyObjectForInstance(_context, busObjectID);
							instIdFamIdTable.put(busObjectID, famID);
						}
						String ebomExpositionMode 	= _util.getAttributeForBO(_context, famID, MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-EBOMExpositionMode"));
						if(null != ebomExpositionMode && "single".equalsIgnoreCase(ebomExpositionMode))
						{
							BusinessObject famObj = new BusinessObject(famID);
							famObj.open(_context);
							String famName = famObj.getName();
							busIdAutoNameTable.put(busObjectID, famName);
							famObj.close(_context);
							partName = famName;
						}
					}
					
					partObjId = createOrMatchPart(_context, partName, instanceName, busObjectID, busType, cadType);
				}
				
				cadObjName.append(busName);
			}

			updateHandledPartObjIdsTable(busObjectID, partObjId, PART_CREATED);
		}
		else
		{
			boolean relationExists =	_util.doesRelationExist(_context, busObjectID, partObjId, relPartSpecification);
			
			if(relationExists)
			{
			BusinessObject partObject = new BusinessObject(partObjId);

			partObject.open(_context);

			String partType = partObject.getTypeName();

			String args[] = new String[4];
			args[0] = ebomConfigObj.getTypeName();
			args[1] = ebomConfigObj.getName();
			args[2] = ebomConfigObj.getRevision();
			args[3] = (String)_argumentsTable.get(MCADServerSettings.LANGUAGE_NAME);

			String famID 				= null;
			String famBusType 			= busType;
			String ebomExpositionMode	= null;
			if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
			{
				if(instIdFamIdTable.containsKey(busObjectID))
					famID = (String)instIdFamIdTable.get(busObjectID);
				else
				{
					famID = _generalUtil.getTopLevelFamilyObjectForInstance(_context, busObjectID);
					instIdFamIdTable.put(busObjectID, famID);
				}
				
				if(famID != null)
				{
					ebomExpositionMode 	= _util.getAttributeForBO(_context, famID, MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-EBOMExpositionMode"));
					BusinessObject famObj = new BusinessObject(famID);
					famObj.open(_context);
					famBusType = famObj.getTypeName(); 
					famObj.close(_context);
				}
			}
			
			IEFEBOMSyncFindMatchingPartBase_mxJPO iefEBOMSyncFindMatchingParts = new IEFEBOMSyncFindMatchingPartBase_mxJPO(_context, args);
			
					Boolean grantAccess = false;
					String PART_RELEASE_STATE 		= ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_RELEASE_PART_STATE);
					
					if(_generalUtil.isPartReleased(_context, partObjId,PART_RELEASE_STATE))
					{
						StringList selectStmtsForMajor = new StringList(4, 1);
						selectStmtsForMajor.add("current.access");
		
						BusinessObjectWithSelectList busWithSelectListForMajor = BusinessObject.getSelectBusinessObjectData(_context, new String[]{partObjId}, selectStmtsForMajor);	

						BusinessObjectWithSelect busWithSelectForMajor  = (BusinessObjectWithSelect) busWithSelectListForMajor.elementAt(0);
						String sAccessList = busWithSelectForMajor.getSelectData("current.access");
						
						if(!sAccessList.contains("modify"))
						{
								_generalUtil.grantAccessForUser(_context, partObjId, _context.getUser(), "modify", "modifyAccess");
								grantAccess = true;
								
						}
					}
			//IR-458535, IR-539331 : If EC Part is released, no Object Attibute should be mapped
			if(!_generalUtil.isPartReleased(_context, partObjId,PART_RELEASE_STATE))
					{	
			if(famID != null && "single".equalsIgnoreCase(ebomExpositionMode))
				iefEBOMSyncFindMatchingParts.copyAttribsFromCadObjToPart(_context, partObject, new BusinessObject(famID), instanceName,famBusType , partType);
			else
			iefEBOMSyncFindMatchingParts.copyAttribsFromCadObjToPart(_context, partObject, new BusinessObject(busObjectID), instanceName, busType, partType);
					}

					if(grantAccess)
						_generalUtil.revokeAccessForUser(_context, partObjId, "modifyAccess");
					

			partObject.close(_context);
			}

			partsTable.put(busObjectID, new BusinessObject(partObjId));

			updateHandledPartObjIdsTable(busObjectID, partObjId, PART_UPDATED);
		}

		return partObjId;
	}

	/*private String getPartAutoName(Context context,String busName,String instanceName) throws Exception
	{
		String partName = null;
		
		if(busNameAutoNameTable.get(busName) != null)
			partName = (String)busNameAutoNameTable.get(busName);
		else
		{
			Hashtable<String, Object> paramJPO = new Hashtable<String, Object>(); 
			paramJPO.put("type_CADModel_Series", confAttrPartSeries);
			paramJPO.put("type_CADModel_Count", new Integer(1));
			//not needed
			paramJPO.put("type_CADDrawing_Series", confAttrPartSeries);
			paramJPO.put("type_CADDrawing_Count", new Integer(0));

			Hashtable typeAutoNameListTable = (Hashtable)JPO.invoke(context, "DECNameGenerator", new String[0], "getNames", JPO.packArgs(paramJPO), Hashtable.class);
			Vector cadModelAutonamesList	= (Vector) typeAutoNameListTable.get("type_CADModel_Names");
			partName = (String)cadModelAutonamesList.get(0);
		}
		return partName;
	}*/
	
	protected String createOrMatchPart(Context _context, String partName, String instanceName, String cadObjId, String busType, String cadType) throws Exception
	{		
		String partObjId = "";
		BusinessObject partObject = getMatchingPart(_context, cadObjId, instanceName, partName, busType, cadType);

		if(partObject == null && confAttrCreateNewPart)
		{
			if (!partsTable.containsKey(cadObjId))
			{
				try
				{					
					partObject = callCreatePartJPO(_context, partName, instanceName, cadObjId, busType, cadType);
					partsTable.put(cadObjId, partObject);
				}
				catch(Exception e)
				{
					if (partObject == null)
					{
						if (confAttrCreateNewPart)
						{
							Hashtable msgTable = new Hashtable();
							msgTable.put("NAME", partName);
							String errMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToCreatePart", msgTable);
							MCADServerException.createException(errMessage+e.getMessage(), e);
						}
						else
						{
							if(!partsToBeCreated.contains(partName))
							{
								partsToBeCreated.addElement(partName);
								updateHandledPartObjIdsTable(cadObjId, partName, PART_FAILED);
							}
						}
					}
				}
			}
			else
			{				
				partObject = (BusinessObject) partsTable.get(cadObjId);
			}
		}

		if (partObject == null)
		{
			if (confAttrCreateNewPart)
			{
				Hashtable msgTable = new Hashtable();
				msgTable.put("NAME", partName);
				String errMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToCreatePart", msgTable);
				MCADServerException.createException(errMessage, null);
			}
			else
			{
				if(!partsToBeCreated.contains(partName))
				{
					partsToBeCreated.addElement(partName);
					updateHandledPartObjIdsTable(cadObjId, partName, PART_FAILED);
				}
			}
		}
		else
		{
			if (!partObject.isOpen())
				partObject.open(_context);

			partObjId = partObject.getObjectId(_context);
			partObject.close(_context);

			if(!partsTable.containsKey(cadObjId)) //Cash the processed parts to aviod the repeatation
			{
				partsTable.put(cadObjId, partObject);
			}
		}
		return partObjId;
	}

	protected BusinessObject callCreatePartJPO(Context _context, String partName, String instanceName, String cadObjId, String busType, String cadType) throws Exception
	{
		String[] args = new String[10];
		args[0] = cadObjId;
		args[1] = partName;
		args[2] = instanceName;
		args[3] = ebomConfigObj.getTypeName();
		args[4] = ebomConfigObj.getName();
		args[5] = ebomConfigObj.getRevision();
		args[6] = (String)_argumentsTable.get(MCADServerSettings.LANGUAGE_NAME);
		args[7] = "" + !_util.isMajorObject(_context, cadObjId);//_globalConfig.isMinorType(busType);
		

		String famID = "";
		if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
		{
			if(instIdFamIdTable.containsKey(cadObjId))
				famID = (String)instIdFamIdTable.get(cadObjId);
			else
			{
				famID = _generalUtil.getTopLevelFamilyObjectForInstance(_context, cadObjId);
				instIdFamIdTable.put(cadObjId, famID);
			}
		}
		args[8] = famID;


		String[] init = new String[]{};
		String jpoName = ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_NEW_PART_CREATION_JPO);
		String functionName = "createPart";

		BusinessObject partObject = (BusinessObject) JPO.invoke(_context, jpoName, init, functionName, args, BusinessObject.class);			

		return partObject;
	}

	protected BusinessObject getMatchingPart(Context _context, String cadObjectId, String instanceName, String partName, String busType, String cadType) throws Exception
	{
		BusinessObject partObject = null;

		try
		{            

			String[] init = new String[]{};
			String[] args = new String[9];
			args[0] = cadObjectId;
			args[1] = partName;
			args[2] = instanceName;
			args[3] = ebomConfigObj.getTypeName();
			args[4] = ebomConfigObj.getName();
			args[5] = ebomConfigObj.getRevision();
			args[6] = (String)_argumentsTable.get(MCADServerSettings.LANGUAGE_NAME);
			//args[7] = "" + _globalConfig.isMinorType(busType);
			String famID = "";
			if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
			{
				if(instIdFamIdTable.containsKey(cadObjectId))
					famID = (String)instIdFamIdTable.get(cadObjectId);
				else
				{
					famID = _generalUtil.getTopLevelFamilyObjectForInstance(_context, cadObjectId);
					instIdFamIdTable.put(cadObjectId, famID);
				}
			}
			args[7] = famID;

			String jpoName = ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_FIND_MATCHING_PART_JPO);
			String functionName = "findMatchingPart";

			partObject = (BusinessObject) JPO.invoke(_context, jpoName, init, functionName, args, BusinessObject.class);			
		}
		catch (Exception ex)
		{
			BusinessObject cadObject = new BusinessObject(cadObjectId);
			cadObject.open(_context);
			String name = cadObject.getName();
			String type = cadObject.getTypeName();
			cadObject.close(_context);
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToGetMatchingPart") + name +" " + type + " " + ex.getMessage(), ex);
		}

		return partObject;
	}

	protected String getPartName(Context _context, String familyName, String instanceName) throws Exception
	{
		String name = "";

		String[] args = new String[3];

		args[0] = instanceName;
		args[1] = familyName;
		args[2] = (String)_argumentsTable.get(MCADServerSettings.LANGUAGE_NAME);

		String[] init = new String[]{};

		try
		{
			name = (String) JPO.invoke(_context, "MCADGenerateName", init, "runGenerateName", args, String.class);
		}
		catch (Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
		return name;
	}

	protected String connectPartWithCADObject(Context _context, String partObjId, String busId, String parentName, String busType, StringBuffer ebomDoneObjectBuffer) throws Exception
	{
		String connectedPartId = "";
		try
		{			
			String[] init = new String[]{};
			String[] args = new String[11];
			args[0] = partObjId;
			args[1] = busId;
			args[2] = parentName;
			args[3] = "" + !_util.isMajorObject(_context, busId);//_globalConfig.isMinorType(busType); [NDM] OP6
			args[4] = ebomConfigObj.getTypeName();
			args[5] = ebomConfigObj.getName();
			args[6] = ebomConfigObj.getRevision();
			args[7] = (String)_argumentsTable.get(MCADServerSettings.LANGUAGE_NAME);
			args[8] = _sourceName;

			String[] packedGCO = new String[2];
			packedGCO = JPO.packArgs(_globalConfig);
			args[9]  = packedGCO[0];
			args[10] = packedGCO[1];
			

			String jpoName = ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_CONNECT_PARTS_JPO);
			String functionName = "connectPartWithCADObject";

			Hashtable createdPartIdMessageDetails	= (Hashtable) JPO.invoke(_context, jpoName, init, functionName, args, Hashtable.class);
			Enumeration specDetails					= createdPartIdMessageDetails.keys();
			while (specDetails.hasMoreElements())
			{
				connectedPartId		= (String) specDetails.nextElement();
				Vector msg			= (Vector) createdPartIdMessageDetails.get(connectedPartId);
				if(msg != null && msg.size() > 0)
				{
					for (int i=0; i < msg.size(); i++)
					{
						String specName = (String)msg.elementAt(i);
						ebomDoneObjectBuffer.append(specName + ",");
					}
				}
			}

		}
		catch (Exception ex)
		{
			Hashtable msgTable = new Hashtable();
			msgTable.put("NAME", parentName);
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToCreatePart", msgTable) + ex.getMessage(), ex);
		}
		return connectedPartId;
	}    

	public Map<String, String> getAllUUIDfromCadID(Context context, String cadparentID) {

		StringList sele = new StringList();
		sele.add("from[CAD SubComponent].attribute[Relationship UUID]");
		sele.add("from[CAD SubComponent].to.id");
		MapList cadInfo;
		try {
			cadInfo = DomainObject.getInfo(context, new String[] { cadparentID }, sele);
		} catch (FrameworkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		Map cadChildInfoMap = convertListToMap(cadInfo, "from[CAD SubComponent].attribute[Relationship UUID]", "from[CAD SubComponent].to.id");

		return cadChildInfoMap;

	}

	public Map<String, String> getAllUUIDfromPartID(Context context, String partID) {
		Map<String, String> retVal = new HashMap<>();
		StringList selectRel = new StringList();
		selectRel.add("from[EBOM].attribute[Relationship UUID]");
		selectRel.add("from[EBOM].id");
		try {
			MapList relationshipAttr = DomainObject.getInfo(context, new String[] { partID }, selectRel);
			if (relationshipAttr == null || relationshipAttr.isEmpty()) {
				return null;
			}
			Map attributes = (Map) relationshipAttr.get(0);
			String relationshipUUID = (String) attributes.get("from[EBOM].attribute[Relationship UUID]");
			String relationshipID = (String) attributes.get("from[EBOM].id");
			if (relationshipID == null || relationshipID.isEmpty() || relationshipUUID == null || relationshipUUID.isEmpty()) {
				return null;
			}

			retVal = convertListToMap(relationshipAttr, "from[EBOM].attribute[Relationship UUID]", "from[EBOM].id");

			return retVal;
		} catch (FrameworkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

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

	protected String createNewEBOM(Context _context, String parentPartId, String childPartId, Vector uniqueRelIdsVector, String parentChildFindNumber,
			boolean isParentFantom, String parentCadObjID, String childObjId) throws Exception {
		String ceratedEBOM = "false";
		String PART_RELEASE_STATE 		= ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_RELEASE_PART_STATE);
		try
		{			
			//IR-458535, IR-539331 : If Parent EC Part is released, then no action be performed on that EC Part
			if (parentPartId != null && parentPartId.length() > 0 && 
					childPartId != null && childPartId.length() > 0 &&
					uniqueRelIdsVector != null && uniqueRelIdsVector.size() > 0 &&
					!parentPartId.equalsIgnoreCase(childPartId) && !_generalUtil.isPartReleased(_context, parentPartId, PART_RELEASE_STATE))
			{	
				// Pack the vector & pass as string to jpo
				String[] init			= new String[]{};

				Vector  argumentsVector	= new Vector();

				argumentsVector.add(parentPartId);
				argumentsVector.add(childPartId);
				argumentsVector.add(uniqueRelIdsVector);
				argumentsVector.add(ebomConfigObj.getTypeName());
				argumentsVector.add(ebomConfigObj.getName());
				argumentsVector.add(ebomConfigObj.getRevision());
				argumentsVector.add((String)_argumentsTable.get(MCADServerSettings.LANGUAGE_NAME));
				argumentsVector.add(_sourceName);
				argumentsVector.add(parentChildFindNumber);
				argumentsVector.add(updateFindNumber);
				argumentsVector.add(String.valueOf(isParentFantom));
				argumentsVector.add(parentCadObjID);
				argumentsVector.add(childObjId);
				argumentsVector.add(_globalConfig);
				argumentsVector.add(allCAdUUID);
				argumentsVector.add(allOldParentUUID);

				String[] args = JPO.packArgs(argumentsVector);

				String jpoName = ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_CONNECT_PARTS_JPO);
				String functionName = "connectPartWithPart";

				ceratedEBOM = (String) JPO.invoke(_context, jpoName, init, functionName, args, String.class);
			}
		}
		catch (Exception ex)
		{
			Hashtable msgTable = new Hashtable();
			//No need to check for null ids. The Exception will occur only when ids are not null
			BusinessObject parent = new BusinessObject(parentPartId);
			BusinessObject child = new BusinessObject(childPartId);
			parent.open(_context);
			child.open(_context);

			msgTable.put("NAME1", parent.getName());
			msgTable.put("NAME2", child.getName());

			parent.close(_context);
			child.close(_context);

			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToConnectBO", msgTable) + ex.getMessage(), ex);
		}

		return ceratedEBOM;
	}

	protected boolean isAEFInstalled(Context context)
	{
		boolean isInstalled = false;

		String Args[] = new String[1];
		Args[0] = "eServiceHelpAbout.tcl";
		String sResult = _util.executeMQL(context ,"execute program $1", Args);

		if (sResult.startsWith("true"))
		{
			if (sResult.indexOf("Framework") > -1)
			{
				isInstalled = true;
			}
		}

		return isInstalled;
	}

	public void verifyAndDoEBOM(Context _context, BusinessObject busObj, String instanceName, Hashtable resultAndStatusTable) throws Exception
	{

		busObj.open(_context);
		String busObjectID	= busObj.getObjectId(_context);
		String busName		= busObj.getName();
		String busTypeName	= busObj.getTypeName();
		String cadTypeAttr	= _util.getAttributeForBO(_context, busObjectID, attrCADType);

		StringBuffer ebomDoneObjectBuffer = new StringBuffer("");

		busObj.close(_context);
		
		if(invalidTypesList.contains(busTypeName) || isProESkeletonModel(_context, busObj))
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.InvalidTypeForEBOMSynch"), null);

		if(_globalConfig.isTypeOfClass(cadTypeAttr, "TYPE_FAMILY_LIKE") && (instanceName == null || "".equals(instanceName)))
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.NoEBOMSynchForFamilyLike"), null);

		if(!partsTable.containsKey(busObjectID))
		{
			_sourceName = _generalUtil.getCSENameForBusObject(_context, _busObject);

			Hashtable relsAndEnds = getRelsAndEnds(relDetailsToNavigateList);
			RecursiveBom(_context, "",null,busObjectID, busName, instanceName, relsAndEnds, busTypeName, ebomDoneObjectBuffer,false,"","");
			
			if(recursiveEBOMError.length()>0)
			{
				MCADServerException.createException(recursiveEBOMError, null);
			}

			String ebomDoneObject = ebomDoneObjectBuffer.toString();			

			if(partsToBeCreated.size() > 0)
			{
				String errMessage = "";
				for (int i = 0; i < partsToBeCreated.size(); i++)
				{
					errMessage += "<br>" + partsToBeCreated.get(i);
				}

				if (confAttrFailAtMissingPart)
				{
					errMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.EBOMCreationFailedPartsMissing")
					+ errMessage;
					resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_STATUS, "false");
					resultAndStatusTable.put(MCADServerSettings.JPO_STATUS_MESSAGE, errMessage);
				}
				else
				{
					errMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.EBOMCreationCompletedPartsMissing")
					+ errMessage;
					resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_STATUS, "true");
					resultAndStatusTable.put(MCADServerSettings.JPO_STATUS_MESSAGE, errMessage);
				}
			}
			else
			{
				if(ebomDoneObject.trim().equals("") && EBOMRelIsRemovedPartObjIds.equals("") && partsTable.containsKey(busObjectID))
				{				
					resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_STATUS, "true");
					resultAndStatusTable.put(MCADServerSettings.JPO_STATUS_MESSAGE, _serverResourceBundle.getString("mcadIntegration.Server.Message.EBOMSynchronizationAlreadyDone"));
				}
				else
				{
					resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_STATUS, "true");
					resultAndStatusTable.put(MCADServerSettings.JPO_STATUS_MESSAGE, _serverResourceBundle.getString("mcadIntegration.Server.Message.EBOMCreationSuccessful"));
				}
			}
		}
		else
		{				
		}

		resultAndStatusTable.put("handledPartObjIdsTable", handledPartObjIdsTable);
	}

	

	protected Vector getFirstLevelChildPartObjIds(Context _context, BusinessObject partObject) throws Exception 
	{
		Vector firstLevelChildPartObjIdList = new Vector();

		try
		{
			short level = 1;
			Visuals vis = _context.getDefaultVisuals();
			String sourceAttName = MCADMxUtil.getActualNameForAEFData(_context,"attribute_Source");
			//build the relationship where clause to be used in expand on bus object
			StringBuffer relationshipWhereClause = new StringBuffer();
			relationshipWhereClause.append("(name == const\"");
			relationshipWhereClause.append(relEBOM);
			relationshipWhereClause.append("\"");
			relationshipWhereClause.append(" && attribute[");
			relationshipWhereClause.append(sourceAttName);
			relationshipWhereClause.append("] == const\"");
			relationshipWhereClause.append(_sourceName);
			relationshipWhereClause.append("\"");
			relationshipWhereClause.append(")");

			//call the expand method on the business object
			Expansion expansion = _util.expandBusObject(_context, partObject, level,"",relationshipWhereClause.toString(), (short)0, false, vis);

			ExpandRelationshipList filteredRelationshipList = expansion.getRelationships();
			ExpandRelationshipItr expandItr = new ExpandRelationshipItr(filteredRelationshipList);

			while (expandItr.next())
			{
				ExpandRelationship expandRel = expandItr.obj();
				boolean isFrom = expandRel.isFrom();
				if(isFrom)
				{
					BusinessObject toBo = expandRel.getTo();
					if(toBo != null)
					{
						toBo.open(_context);
						String boid = toBo.getObjectId(_context);						
						firstLevelChildPartObjIdList.addElement(boid);
						toBo.close(_context);
					}
				}
			}
		}
		catch(Exception me)
		{
			MCADServerException.createException(me.getMessage(), me);            
		}

		return firstLevelChildPartObjIdList;
	}

	protected boolean isProESkeletonModel(Context _context, BusinessObject inBus) throws Exception
	{
		boolean skeletonModel = false;
		String cseName = _generalUtil.getCSENameForBusObject(_context, inBus);
		if(cseName.equalsIgnoreCase("MxPRO"))
		{
			String skeletonAttributeName = MCADMxUtil.getActualNameForAEFData(_context,"attribute_ProESkeleton");
			AttributeList attrList = (inBus.getAttributes(_context)).getAttributes();
			AttributeItr attrItr = new AttributeItr(attrList);
			while(attrItr.next())
			{
				Attribute attr = attrItr.obj();
				if(attr.getName().equals(skeletonAttributeName) && attr.getValue().equalsIgnoreCase("true"))
				{
					skeletonModel = true;
				}
			}
		}

		return skeletonModel;
	}

	protected boolean getBalloonNumberFromAssociatedDrawing(Context _context, String cadObjId, String parentBusId) throws Exception
	{
		boolean hasConnectedDrwWithBalloonNumber = false;

		StringList busSelect = new StringList();
		String []  oids = new String[1];
		String SELECT_EXPRESSION_BALOONNUMBERS_Drw = "";
		String SELECT_EXPRESSION_BALOONNUMBERS_Asm = "";
		String strBaloonValue = "";

		oids[0] = parentBusId;
		SELECT_EXPRESSION_BALOONNUMBERS_Drw = "revisions.to["+relAssociateDrawing+"].attribute["+attrBalloonNumbers + "]";
		SELECT_EXPRESSION_BALOONNUMBERS_Asm = "revisions.from["+relAssociateDrawing+"].attribute["+attrBalloonNumbers + "]";
		busSelect.add("revisions");
		busSelect.add(SELECT_EXPRESSION_BALOONNUMBERS_Drw);
		busSelect.add(SELECT_EXPRESSION_BALOONNUMBERS_Asm);

		BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(_context, oids, busSelect);
		for(int i = 0; i < busWithSelectList.size(); i++)
		{
			BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(i);
			StringList revisionList 		  = busWithSelect.getSelectDataList("revisions");

					
			for(int j = (revisionList.size() - 1 ); j > -1 ; j--)
			{
				String strRevision = (String) revisionList.elementAt(j);
				StringList strBaloonNumberList = busWithSelect.getSelectDataList("revisions["+ strRevision + "].to["+relAssociateDrawing+"].attribute["+attrBalloonNumbers + "]");
				if(null == strBaloonNumberList || strBaloonNumberList.size()==0)
					strBaloonNumberList = busWithSelect.getSelectDataList("revisions["+ strRevision + "].from["+relAssociateDrawing+"].attribute["+attrBalloonNumbers + "]");

		                if(strBaloonNumberList != null)
			        {
				      strBaloonValue = (String) strBaloonNumberList.elementAt(0);

				      if(!strBaloonValue.equals(""))
				           {
							populateCadNameBalloonNumberMap(strBaloonValue,parentBusId);
							hasConnectedDrwWithBalloonNumber = true;
							break;
				           }
			        }
			
			}
		}

		return hasConnectedDrwWithBalloonNumber;
	}

	protected void populateCadNameBalloonNumberMap(String attBallonNumberVal, String cadObjId) throws Exception 
	{
		IEFXmlNode commandPacket			= null;
		ResourceBundle mcadIntegrationBundle = ResourceBundle.getBundle("ief");
		String charset						 = mcadIntegrationBundle.getString("mcadIntegration.MCADCharset");

		if(charset == null)
			charset = "UTF8";

		if(attBallonNumberVal != null && attBallonNumberVal.length() >0)
		{
			int i = attBallonNumberVal.indexOf("?xml");	
			if(i < 0)
				attBallonNumberVal = "<?xml version='1.0'?>" + attBallonNumberVal;					   

			commandPacket			= MCADXMLUtils.parse(attBallonNumberVal, charset);
			if(commandPacket != null)
			{
				Enumeration paramChildEnum      = commandPacket.getChildrenByName("cadobject");
				while(paramChildEnum.hasMoreElements())
				{
					IEFXmlNode queryParamNode	= (IEFXmlNode)paramChildEnum.nextElement();
					String name 				= queryParamNode.getAttribute("name");
					String type 				= queryParamNode.getAttribute("type");
					String number 				= queryParamNode.getAttribute("number");
					String title 				= queryParamNode.getAttribute("title");

					if((_globalConfig.isTypeOfClass(type, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE)))
					{
						String familyName  = queryParamNode.getAttribute("familyname");

						if(null != familyName && !("".equals(familyName)))
						{
							name = _generalUtil.getNameForInstance(familyName,name);
						}
					}
					
					StringBuffer keyWithNameBuffer = new StringBuffer();
					keyWithNameBuffer.append("name");
					keyWithNameBuffer.append("|");
					keyWithNameBuffer.append(name);
					keyWithNameBuffer.append("|");
					keyWithNameBuffer.append(type);
					keyWithNameBuffer.append("|");
					keyWithNameBuffer.append(cadObjId);
					String balloonkey	= keyWithNameBuffer.toString();
	
					if(_globalConfig != null && _globalConfig.isObjectAndFileNameDifferent())
					{
						if(title != null && title.length() > 0)
						{
							StringBuffer keyWithTitleBuffer = new StringBuffer();
							keyWithTitleBuffer.append("title");
							keyWithTitleBuffer.append("|");
							keyWithTitleBuffer.append(title);
							keyWithTitleBuffer.append("|");
							keyWithTitleBuffer.append(type);
							keyWithTitleBuffer.append("|");
							keyWithTitleBuffer.append(cadObjId);

							balloonkey	= keyWithTitleBuffer.toString();							
						}
					}
					cadNameBalloonNumberMap.put(balloonkey,number);
				}
			}
		}
	}

	protected void updateHandledPartObjIdsTable(String objId, String partObjId, String operation)
	{
		String[] partDetails = new String[2];
		partDetails[0] = partObjId;
		partDetails[1] = operation;

		handledPartObjIdsTable.put(objId, partDetails);
	}
	
	protected void resetQuantity(Context context, String parentPartId) throws Exception
	{
		String Args[] = new String[7];
		Args[0] = parentPartId;
		Args[1] = "from";
		Args[2] = "relationship";
		Args[3] = relEBOM;
		Args[4] = "relationship";
		Args[5] = "id";
		Args[6] = "|";
		String mqlResult = _util.executeMQL(context,"expand bus $1 $2 $3 $4 select $5 $6 dump $7", Args);

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
				
				Relationship relationship	=  new Relationship(relId);
				AttributeList attributelist = new AttributeList();
				attributelist.addElement(new Attribute(new AttributeType(attrQuantity), "0"));
				relationship.setAttributes(context, attributelist);
				
			}

		}
		else
		{
			throw new Exception("Problem in expanding bus");
		}
	}
	
	private BusinessObjectList getRelatedPartList(Context _context,BusinessObject cadObject) throws Exception
	{
		BusinessObjectList relatedPartsList = new BusinessObjectList();
		/*if("true".equalsIgnoreCase(confAttrAssignPartToMajor))  //{NDM]
		{
			BusinessObject majorBusObject = _util.getMajorObject(_context, cadObject);
			if(majorBusObject == null)
			{
				majorBusObject = cadObject;
			}
			relatedPartsList = _util.getRelatedBusinessObjects(_context,majorBusObject,relPartSpecification,"to");    				
		}
		else*/
		{
			relatedPartsList = _util.getRelatedBusinessObjects(_context,cadObject,relPartSpecification,"to");    				
		}
		
		return relatedPartsList;
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
