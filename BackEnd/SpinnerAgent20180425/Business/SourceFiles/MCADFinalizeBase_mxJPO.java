/*
 **  MCADFinalizeBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program for performing finalization.
 */
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Set;

import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.ExpansionWithSelect;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.db.RelationshipWithSelect;
import matrix.db.RelationshipWithSelectList;
import matrix.db.Signature;
import matrix.db.SignatureItr;
import matrix.db.SignatureList;
import matrix.db.State;
import matrix.db.StateList;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFProgressCounter;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.DSCServerErrorMessageTable;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.MCADIntegration.utils.MCADXMLUtils;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNodeImpl;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainRelationship;


public class MCADFinalizeBase_mxJPO extends IEFCommonUIActions_mxJPO
{
	
	Hashtable revisedInstanceFamilyIdTable= new Hashtable();
	
	protected final boolean COPY_BLANK_ATTR_DURING_FINALIZATION		= true;

	protected boolean isMultiPromote								= false;
	protected boolean isRootNodeFamilyLike							= false;

	protected StringBuffer promotionErrorMessage					= null;
	protected Vector finalizationCandidates 						= null;	
	protected String selectAllChildren								= null;
	protected	String isLateralViewSelected						= null;
	protected	String appliedlateralViewName						= null;

	protected String versionOfRelActualName							= null;
	protected String cadTypeAttrActualName							= null;
	protected String MCADModelBusTypeActualName						= null;
	protected String MCADDrawingBusTypeActualName					= null;
	protected String mcsURL                                         = null;

	protected final String SELECT_EXPRESSION_NAME 					= "name";
	protected final String SELECT_EXPRESSION_TO_CHILD_ID			= "to.id";
	protected final String SELECT_EXPRESSION_FROM_CHILD_ID			= "from.id";
	protected final String SELECT_EXPRESSION_TO_CHILD_POLICY		= "to.policy";
	protected final String SELECT_EXPRESSION_FROM_CHILD_POLICY		= "from.policy";

	protected String SELECT_EXPRESSION_FROM_CHILD_MAJOR_ID	        = "";
	protected String SELECT_EXPRESSION_TO_CHILD_MAJOR_ID			= "";
	protected String SELECT_EXPRESSION_FROM_CHILD_MAJOR_POLICY		= "";
	protected String SELECT_EXPRESSION_TO_CHILD_MAJOR_POLICY		= "";
	protected String SELECT_EXPRESSION_FROM_CHILD_ATTR_CAD_TYPE		= "";
	protected String SELECT_EXPRESSION_TO_CHILD_ATTR_CAD_TYPE		= "";

	protected HashSet  processedInstanceStructChildren				= null;
	protected Hashtable objIdSignatureDetailsTable					= new Hashtable();
	protected Vector alreadyProcessedIds							= new Vector();
	protected Vector removedRelIds									= new Vector();

	IEFEBOMSyncFindMatchingPart_mxJPO ebomFMJPO						= null;

	protected List errorMessageList 								= new ArrayList();
	protected IEFXmlNodeImpl responseCadObjectList;

	public  MCADFinalizeBase_mxJPO  () 
	{

	}
	public MCADFinalizeBase_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("not supported no desktop client", null);
		promotionErrorMessage = new StringBuffer("");


	}
	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}


	/* execute method is overwritten method of base class IEF-UICommonActions
	 * Reason :
	 * 	IEF-UICommonActions::execute calls canPerformOperation(..) before calling
	 * 	executeCustom(..).  Root bus id is passed to function canPerformOperation.
	 * 	It checks if root obj is already finalized, if yes, throws exception, because of
	 * 	which executeCustom(..)  is not called. In such case, showing consolidated
	 * 	error about finalization of children is not possible.
	 *
	 * 	In order to be able to show consolidated error, execute function is overwritten
	 * 	and call to canPerformOperation is removed. Now executeCustom collects all
	 * 	the errors and then throws exception.
	 */
	public Hashtable execute(Context context,String[] args) throws MCADException
	{
		Hashtable resultDataTable = new Hashtable();
		try
		{

			// Get all standard initialization,
			// including creation of GCO, logger, resource bundle etc.
			initialize(context,args);

			initOtherJPOsCalledFromThisJPO(context);

			//canPerformOperation(resultDataTable);
			executeCustom(context, resultDataTable);

		}
		catch(Exception e)
		{
			// Do not throw any exception back to JPO
			// For Any error/exception, send proper message back to the caller
			// using the resultDataTable
			String error = e.getMessage();
			resultDataTable.put(MCADServerSettings.JPO_EXECUTION_STATUS,"false");
			resultDataTable.put(MCADServerSettings.JPO_STATUS_MESSAGE,error);
			resultDataTable.put(MCADServerSettings.JPO_FAILED_MESSAGES,errorMessageList);
		}
		finally
		{
			IEFProgressCounter.removeCounter(_operationUID);
		}

		return resultDataTable;
	}

	// Business Logic for implementing
	protected void canPerformOperationCustom(Context _context, Hashtable resultDataTable) throws MCADException
	{
		DSCServerErrorMessageTable errorMessageTable	= new DSCServerErrorMessageTable(_context, _globalConfig, _util, _serverResourceBundle);

		try
		{
			if (_busObject == null)
			{
				MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.BusinessObjectNotFound"), null);
			}

			isObjectFinalizable(_context, _busObject, new Vector(), errorMessageTable);
		}
		catch(Exception e)
		{
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FinalizeNotAllowed") + e.getMessage(), e);
		}
	}

	void initOtherJPOsCalledFromThisJPO(Context _context) throws Exception
	{
		String confObjTNR = _globalConfig.getEBOMRegistryTNR();
		StringTokenizer token = new StringTokenizer(confObjTNR, "|");
		if(token.countTokens() < 3)
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.EBOMRegistryNotDefined"), null);

		String confObjType			= (String) token.nextElement();
		String confObjName			= (String) token.nextElement();
		String confObjRev			= (String) token.nextElement();

		String[] jpoArgs = new String[4];
		jpoArgs[0] = confObjType;
		jpoArgs[1] = confObjName;
		jpoArgs[2] = confObjRev;
		jpoArgs[3] = (String)_argumentsTable.get(MCADServerSettings.LANGUAGE_NAME);

		ebomFMJPO = new IEFEBOMSyncFindMatchingPart_mxJPO (_context, jpoArgs);

	}

	// Entry point
	public void executeCustom(Context _context, Hashtable resultAndStatusTable)  throws MCADException
	{
		versionOfRelActualName						= MCADMxUtil.getActualNameForAEFData(_context,"relationship_VersionOf");
		cadTypeAttrActualName						= MCADMxUtil.getActualNameForAEFData(_context,"attribute_CADType");
		MCADModelBusTypeActualName					= MCADMxUtil.getActualNameForAEFData(_context,"type_MCADModel");
		MCADDrawingBusTypeActualName				= MCADMxUtil.getActualNameForAEFData(_context,"type_MCADDrawing");

		SELECT_EXPRESSION_FROM_CHILD_MAJOR_ID		= "from.from[" + versionOfRelActualName + "].to.id";
		SELECT_EXPRESSION_TO_CHILD_MAJOR_ID			= "to.from[" + versionOfRelActualName + "].to.id";
		SELECT_EXPRESSION_FROM_CHILD_MAJOR_POLICY	= "from.from[" + versionOfRelActualName + "].to.policy";
		SELECT_EXPRESSION_TO_CHILD_MAJOR_POLICY		= "to.from[" + versionOfRelActualName + "].to.policy";
		SELECT_EXPRESSION_FROM_CHILD_ATTR_CAD_TYPE	= "from.attribute[" + cadTypeAttrActualName + "]";
		SELECT_EXPRESSION_TO_CHILD_ATTR_CAD_TYPE    = "to.attribute[" + cadTypeAttrActualName + "]";

		IEFXmlNode finalizePacket					= (IEFXmlNode)_argumentsTable.get(MCADServerSettings.SELECTED_OBJECTID_LIST);
		String isStructureDisturbed					= (String)_argumentsTable.get("isStructureDisturbed");

		String isMultiPromotion						= (String)_argumentsTable.get("isMultiPromote");

		processedInstanceStructChildren				= new HashSet();

		DSCServerErrorMessageTable errorMessageTable		= new DSCServerErrorMessageTable(_context, _globalConfig, _util, _serverResourceBundle);

		if(isMultiPromotion.equalsIgnoreCase("true"))
			this.isMultiPromote = true;

		selectAllChildren		= (String)_argumentsTable.get("selectAllChildren");
		isLateralViewSelected	= (String)_argumentsTable.get("isLateralViewSelected");
		appliedlateralViewName	= (String)_argumentsTable.get("lateralViewName");
		mcsURL                  = (String)_argumentsTable.get(MCADServerSettings.MCS_URL);

		finalizationCandidates	= new Vector();		

		 responseCadObjectList = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		 responseCadObjectList.setName("cadobjectlist");

		// if the strructure is disturbed, implies that the Virtual structure has been created by user so
		// we create Virtual structure in the database. Implementation done for 10.6
		try
		{
			if("true".equalsIgnoreCase(isStructureDisturbed))
			{
				// [NDM] NE4 start:-
				// createVirtualStructure(_context, finalizePacket, errorMessageTable);
				// [NDM] NE4 start:-
			}

			StringBuffer errorMessageBuffer	= new StringBuffer("");
			String Args[] = new String[3];
			Args[0] = "global";
			Args[1] = "IsDesignCenterCommand";
			Args[2] = "true";
			_util.executeMQL(_context, "set env $1 $2 $3", Args);

			initiateFinalization(_context, finalizePacket, resultAndStatusTable, errorMessageTable);			
			Args = new String[2];
			Args[0] = "global";
			Args[1] = "IsDesignCenterCommand";
			_util.executeMQL(_context,"unset env $1 $2", Args);

			String errorMessage = errorMessageBuffer.toString();
			if(!errorMessage.equals(""))
			{
				//Show consolidated error message
				Args = new String[2];
				Args[0] = "global";
				Args[1] = "IsDesignCenterCommand";
				_util.executeMQL(_context, "unset env $1 $2", Args);
				MCADServerException.createException(errorMessage, null);
			}
			resultAndStatusTable.put(MCADServerSettings.SELECTED_OBJECTID_LIST, finalizePacket);
			resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_STATUS, "true");
			resultAndStatusTable.put(MCADServerSettings.JPO_SELECTED_OBJECTS_LIST, responseCadObjectList);
			

			if(errorMessageTable.errorsOccured())
				MCADServerException.createException(errorMessageTable.getErrorMessageHTMLTable(), null);
		}
		catch(Exception exception)
		{
			MCADServerException.createException(exception.getMessage(), exception);
		}
	}

	protected void initiateFinalization(Context _context, IEFXmlNode finalizePacket , Hashtable resultAndStatusTable, DSCServerErrorMessageTable errorMessageTable) throws Exception
	{
		IEFXmlNode structureNode = MCADXMLUtils.getChildNodeWithName(finalizePacket, "structure");
		Vector handledBusIds	= new Vector();
		Vector compNodes		= new Vector();
		Vector assemNodes		= new Vector();
		Vector drawingNodes		= new Vector();
		Vector otherNodes		= new Vector();
		Vector familyNodes		= new Vector();
		
		alreadyProcessedIds.clear();
		
		Enumeration topLevelElements = structureNode.elements();

		setFinalizationCandidates(structureNode);

		while(topLevelElements.hasMoreElements())
		{
			// Get top node
			IEFXmlNode topNode = (IEFXmlNode)topLevelElements.nextElement();
			String cadType   = (String)topNode.getAttribute("cadtype");

			if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
			{
				familyNodes.addElement(topNode);
			}
			else if(cadType != null && cadType.indexOf("component") != -1)
			{
				compNodes.addElement(topNode);
			}
			else if(cadType != null && cadType.indexOf("assembly") != -1)
			{
				assemNodes.addElement(topNode);
			}
			else if(cadType != null && cadType.indexOf("drawing") != -1)
			{
				drawingNodes.addElement(topNode);
			}
			else
			{
				otherNodes.addElement(topNode);
			}
		}

		Vector masterVector = new Vector(5);
		masterVector.addElement(compNodes);
		masterVector.addElement(assemNodes);
		masterVector.addElement(drawingNodes);
		masterVector.addElement(otherNodes);
		masterVector.addElement(familyNodes);

		for(int k=0; k<masterVector.size(); k++)
		{
			Vector groupNodes = (Vector)masterVector.get(k);
			if(groupNodes != null && groupNodes.size() != 0)
			{
				for(int m=0; m<groupNodes.size(); m++)
				{
					IEFXmlNode topNode = (IEFXmlNode)groupNodes.get(m);

					BusinessObject rootbusObject	= new BusinessObject((String)topNode.getAttribute("id"));

					String rootNodeState			= (String)topNode.getAttribute("currentstate");
					String isAnyParentSelected		= (String)topNode.getAttribute("selected");
					errorMessageTable.setRootId((String)topNode.getAttribute("id"));

					rootbusObject.open(_context);
					String cadType					= _util.getCADTypeForBO(_context, rootbusObject);
					rootbusObject.close(_context);
					isRootNodeFamilyLike			= _globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE);

					Vector alreadyExpandedNodes = new Vector();
					//check whether parent-child states are valid before promotion
					validateParentChildStatesForPromotion(_context, topNode,errorMessageTable);
					initiateFinalizationforNode(_context, topNode, resultAndStatusTable, rootbusObject, handledBusIds, alreadyExpandedNodes, isAnyParentSelected, rootNodeState, errorMessageTable);
				}
			}
		}
	}

	protected void setFinalizationCandidates(IEFXmlNode topNode)
	{
		Enumeration childElements = topNode.elements();
		while(childElements.hasMoreElements())
		{
			IEFXmlNode childNode				= (IEFXmlNode)childElements.nextElement();
			String isSelected					= (String)childNode.getAttribute("selected");
			String isPreFinalizationState		= (String)childNode.getAttribute("isprefinalizationstate");

			if((MCADAppletServletProtocol.TRUE).equalsIgnoreCase(isSelected) && (MCADAppletServletProtocol.TRUE).equalsIgnoreCase(isPreFinalizationState))
			{
				finalizationCandidates.addElement(childNode.getAttribute("id"));				
			}

			setFinalizationCandidates(childNode);
		}
	}

	protected String validateNodeForPromotion(Context _context, IEFXmlNode childNode,String isAnyParentSelected, BusinessObject rootbusObject, String rootNodeState, DSCServerErrorMessageTable errorMessageTable) throws Exception
	{
		String jpoName					= _generalUtil.getMassPromoteConfigObjectValidationRuleJPOName(_context);
		String validateNodeForPromotion	= _generalUtil.getMassPromoteConfigObjectValidatePromotionFlag(_context);
		String validationResult			= "";

		if(!jpoName.equals("") && validateNodeForPromotion.equalsIgnoreCase("true"))
		{
			String jpoMethod	= "validateObjectForPromotion";

			Hashtable jpoArgsTable	= new Hashtable();
			jpoArgsTable.put("isAnyParentSelected", isAnyParentSelected);
			jpoArgsTable.put("rootBusObjId", rootbusObject.getObjectId(_context));
			jpoArgsTable.put("childBusObjId", (String)childNode.getAttribute("id"));
			jpoArgsTable.put("isChildSelected", (String)childNode.getAttribute("selected"));
			jpoArgsTable.put("childNodeState", (String)childNode.getAttribute("currentstate"));

			String[] jpoArgs  = JPO.packArgs(jpoArgsTable);

			String [] packedGCO = JPO.packArgs(_globalConfig);

			String []initArgs = new String[5];
			initArgs[0] = packedGCO[0];
			initArgs[1] = packedGCO[1];
			initArgs[2] = rootNodeState;
			initArgs[3] = _serverResourceBundle.getLanguageName();
			initArgs[4] = String.valueOf(this.isMultiPromote);

			try
			{			
				validationResult = (String)_util.executeJPO(_context,jpoName, initArgs, jpoMethod, jpoArgs, String.class);
			}
			catch (Exception e)
			{
				String errorMessage = e.getMessage();
				errorMessageTable.addErrorMessage(_context, (String)childNode.getAttribute("id"), errorMessage);
				errorMessageList.add(errorMessage);
			}
		}

		return validationResult;
	}

	protected void initiateFinalizationforNode(Context _context, IEFXmlNode cadObjectNode, Hashtable resultAndStatusTable, BusinessObject rootbusObject, Vector handledBusIds,  Vector alreadyExpandedNodes, String isAnyParentSelected, String rootNodeState, DSCServerErrorMessageTable errorMessageTable) throws Exception
	{
		IEFXmlNode sortedChildNodes = cadObjectNode;

		if(null != cadObjectNode && cadObjectNode.getChildCount() > 0)
		{
			sortedChildNodes = MCADXMLUtils.sortXMLChildNodes(cadObjectNode, "selected", true, false); 
		}

		Enumeration childNodeElements	= sortedChildNodes.elements();
		String isSelected				= (String)cadObjectNode.getAttribute("selected");
		Hashtable signNameSignActionTable	= null;		

		if(alreadyExpandedNodes.contains((String)cadObjectNode.getAttribute("id")))
			return;

		alreadyExpandedNodes.add((String)cadObjectNode.getAttribute("id"));

		if((MCADAppletServletProtocol.TRUE).equalsIgnoreCase(isSelected))
		{
			signNameSignActionTable = getSignatureDetails(_context, cadObjectNode);
			isAnyParentSelected  = MCADAppletServletProtocol.TRUE;
		}

		while(childNodeElements.hasMoreElements())
		{
			IEFXmlNode childNode		= (IEFXmlNode)childNodeElements.nextElement();
			String isNodeReplicated = (String)childNode.getAttribute("isnodereplicated");

			if(handledBusIds.contains((String)childNode.getAttribute("id")) || isNodeReplicated.equalsIgnoreCase("true"))
			{
				BusinessObject childBusObj = new BusinessObject((String)childNode.getAttribute("id"));
				childBusObj.open(_context);
				String cadtype = _util.getCADTypeForBO(_context,childBusObj);
				childBusObj.close(_context);

				Enumeration nextchildNodeElements	= childNode.elements();
				if(nextchildNodeElements.hasMoreElements())
				{
					IEFXmlNode nextLevelchildNode		= (IEFXmlNode)nextchildNodeElements.nextElement();	
					
					if(_globalConfig.isTypeOfClass(cadtype, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE) && handledBusIds.contains((String)nextLevelchildNode.getAttribute("id")))
				continue;
				}
				else
					continue;
			
			}

			String isChildSelected			= (String)childNode.getAttribute("selected");
			
			String relId					= (String)childNode.getAttribute("rid");
						
			if(null != relId && !relId.equals("") && !removedRelIds.contains(relId))
			{
				Relationship relationship		= new Relationship(relId);
				String attrRelModificaionStatus	= MCADMxUtil.getActualNameForAEFData(_context, "attribute_RelationshipModificationStatusinMatrix");
				String relModificaionStatus		= _util.getRelationshipAttributeValue(_context, relationship, attrRelModificaionStatus); 

				if(relModificaionStatus.equalsIgnoreCase("deleted"))
				{
					String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.CannotPerformOperationAsSomeRelationshipsDeleted");
					errorMessageTable.addErrorMessage(_context, (String)childNode.getAttribute("id"), errorMessage);
					errorMessageList.add(errorMessage);

					continue;
				}
			}

			String validationMessage = validateNodeForPromotion(_context, childNode, isAnyParentSelected, rootbusObject, rootNodeState, errorMessageTable);

			if (validationMessage.startsWith("false"))
			{
				validationMessage = validationMessage.substring(validationMessage.indexOf("|")+1, validationMessage.length());
				errorMessageTable.addErrorMessage(_context, (String)childNode.getAttribute("id"), validationMessage);
				errorMessageList.add(validationMessage);
			}

			String childMajorBusObjId	= (String)childNode.getAttribute("majorobjectid");
			if((MCADAppletServletProtocol.TRUE).equalsIgnoreCase(isChildSelected) && !handledBusIds.contains(childMajorBusObjId))
			{
				if(signNameSignActionTable != null)
				{
					String childCurrentState	= (String)childNode.getAttribute("currentstate");

					if(!handledBusIds.contains(childMajorBusObjId))
					{	
						applySignaturetoChild(_context, childMajorBusObjId, childCurrentState, signNameSignActionTable, errorMessageTable);
					}	
				}
			}

			initiateFinalizationforNode(_context, childNode, resultAndStatusTable, rootbusObject, handledBusIds,  alreadyExpandedNodes, isAnyParentSelected, rootNodeState, errorMessageTable);
		}

		alreadyExpandedNodes.remove((String)cadObjectNode.getAttribute("id"));

		String validationMessage = validateNodeForPromotion(_context, cadObjectNode, isAnyParentSelected, rootbusObject, rootNodeState, errorMessageTable);

		if (validationMessage.startsWith("false"))
		{
			validationMessage = validationMessage.substring(validationMessage.indexOf("|")+1, validationMessage.length());
			if(!errorMessageTable.containsErrorMessage((String)cadObjectNode.getAttribute("id"), validationMessage))
			{
				errorMessageTable.addErrorMessage(_context, (String)cadObjectNode.getAttribute("id"), validationMessage);
				errorMessageList.add(validationMessage);
			}
		}

		if((MCADAppletServletProtocol.TRUE).equalsIgnoreCase(isSelected))
		{
			String objectID					= (String)cadObjectNode.getAttribute("id");
			String isPreFinalizationState	= (String)cadObjectNode.getAttribute("isprefinalizationstate");
			String isNodeReplicated = (String)cadObjectNode.getAttribute("isnodereplicated");
			
			if(isNodeReplicated.equalsIgnoreCase("false"))
			{
				if(isPreFinalizationState.equalsIgnoreCase("True"))
				{
					BusinessObject bus = new BusinessObject(objectID);
					bus.open(_context);
					BusinessObject majorBusObj = _util.getMajorObject(_context,bus);
					if(majorBusObj == null)
						majorBusObj = bus;

					if(!majorBusObj.isOpen())
						majorBusObj.open(_context);

					String cadType = _util.getCADTypeForBO(_context,bus);
					bus.close(_context);
					if(!handledBusIds.contains(objectID))
					{
						if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
						{
							doFinalizeOperations(_context, bus, resultAndStatusTable, errorMessageTable, handledBusIds);

							handledBusIds.addElement(objectID);
							handledBusIds.addElement(majorBusObj.getObjectId());
						}
						else
						{
							doFinalizeOperations(_context, bus, resultAndStatusTable, errorMessageTable,handledBusIds);

							handledBusIds.addElement(objectID);
							handledBusIds.addElement(majorBusObj.getObjectId());
						}
						// Check whether user aborted the operation, in that case throw error
						if(isOperationCancelled())
						{
							String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.UserCancelledTheOperation");
							MCADServerException.createException(errorMessage, null);
						}

						// Update metadata count, for progress bar update
						incrementMetaCurrentCount();
					}

					if(majorBusObj.isOpen())
						majorBusObj.close(_context);
				}
				else
				{
					String majorBusObjectId	= (String)cadObjectNode.getAttribute("majorobjectid");
					String busObjectId		= (String)cadObjectNode.getAttribute("id");
					String currentState		= (String)cadObjectNode.getAttribute("currentstate");
					String policyName		= (String)cadObjectNode.getAttribute("policyname");

					if(!handledBusIds.contains(majorBusObjectId))
					{
						doPromoteOperations(_context, majorBusObjectId, busObjectId, currentState, policyName, signNameSignActionTable, handledBusIds, resultAndStatusTable, errorMessageTable);
					}
				}
			}
		}
	}
	protected void processAsStoredChildren(Context _context, String busObjId, String relNames, String relationClause, Vector handledBusIds, Hashtable relsAndEnds, StringList relSelectionList,Hashtable signatureActionTable, Hashtable resultAndStatusTable, DSCServerErrorMessageTable  errorMessageTable) throws Exception
	{
		StringBuffer busTypeNames	= new StringBuffer();
		busTypeNames.append(MCADModelBusTypeActualName);
		busTypeNames.append(",");
		busTypeNames.append(MCADDrawingBusTypeActualName);

		BusinessObject busObject = new BusinessObject(busObjId);
		busObject.open(_context);

		BusinessObject	majorBus = busObject;
		if(!_util.isMajorObject(_context, busObjId))//!_globalConfig.isMajorType(busObject.getTypeName())) // [NDM] OP6
			majorBus = _util.getMajorObject(_context, busObject);			

		if(!majorBus.isOpen())
			majorBus.open(_context);

		State majorCurrentState	= _util.getCurrentState(_context, majorBus);

		if(processedInstanceStructChildren.contains(busObjId))
			return;
		else
			processedInstanceStructChildren.add(busObjId);

		ExpansionWithSelect expansionWithSelect = busObject.expandSelect(_context, relNames, busTypeNames.toString(), new StringList(), relSelectionList, true, true, (short)1, "", relationClause, true);

		busObject.close(_context);

		if(majorBus.isOpen())
			majorBus.close(_context);

		Enumeration relationsList	= expansionWithSelect.getRelationships().elements();
		while(relationsList.hasMoreElements())
		{
			String childObjID       = null;
			String childMajorObjId	= null;
			String cadType			= null;
			String policyName		= null;

			RelationshipWithSelect relationship = (RelationshipWithSelect) relationsList.nextElement();
			relationship.open(_context);

			String relName = relationship.getSelectData(SELECT_EXPRESSION_NAME);
			String relEnd  = (String)relsAndEnds.get(relName);

			if(relEnd.equals("from"))
			{
				childObjID      = relationship.getSelectData(SELECT_EXPRESSION_FROM_CHILD_ID);
				childMajorObjId = relationship.getSelectData(SELECT_EXPRESSION_FROM_CHILD_MAJOR_ID);
				cadType         = relationship.getSelectData(SELECT_EXPRESSION_FROM_CHILD_ATTR_CAD_TYPE);
				if(childMajorObjId == null || childMajorObjId.equals(""))
				{
					childMajorObjId = childObjID;
					policyName      = relationship.getSelectData(SELECT_EXPRESSION_FROM_CHILD_POLICY);
				}
				else
					policyName = relationship.getSelectData(SELECT_EXPRESSION_FROM_CHILD_MAJOR_POLICY);
			}
			else
			{
				childObjID      = relationship.getSelectData(SELECT_EXPRESSION_TO_CHILD_ID);
				childMajorObjId = relationship.getSelectData(SELECT_EXPRESSION_TO_CHILD_MAJOR_ID);
				cadType         = relationship.getSelectData(SELECT_EXPRESSION_TO_CHILD_ATTR_CAD_TYPE);
				if(childMajorObjId == null || childMajorObjId.equals(""))
				{
					childMajorObjId = childObjID;
					policyName      = relationship.getSelectData(SELECT_EXPRESSION_TO_CHILD_POLICY);
				}
				else
					policyName = relationship.getSelectData(SELECT_EXPRESSION_TO_CHILD_MAJOR_POLICY);
			}

			BusinessObject childObject		= new BusinessObject(childObjID);
			BusinessObject childMajorObj	= new BusinessObject(childMajorObjId);

			childObject.open(_context);

			if(!childMajorObj.isOpen())
				childMajorObj.open(_context);

			State childMajorState = _util.getCurrentState(_context,childMajorObj);

			if(!childMajorState.getName().equals(majorCurrentState.getName()))
				continue;

			if(!handledBusIds.contains(childObjID) && !handledBusIds.contains(childMajorObjId))
			{
				if(_util.isPreFinalizationState(childMajorState, _globalConfig.getFinalizationState(policyName)).equalsIgnoreCase("true"))
				{
					if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
					{
						String topLevelFamilyObjectID	= _generalUtil.getTopLevelFamilyObjectForInstance(_context,childObjID);
						BusinessObject familyObject		= new BusinessObject(topLevelFamilyObjectID);
						familyObject.open(_context);

						String childRelationClause = MCADUtil.replaceString(relationClause, busObjId, childObjID);

						processAsStoredChildren(_context, childObjID, relNames, childRelationClause, handledBusIds, relsAndEnds,relSelectionList, signatureActionTable, resultAndStatusTable, errorMessageTable );
						if(!handledBusIds.contains(childObjID) && !handledBusIds.contains(childMajorObjId))
						{
							applySignaturetoChild(_context, childMajorObjId, childMajorState.getName(), signatureActionTable, errorMessageTable);
							doFinalizeOperations(_context, childObject,resultAndStatusTable, errorMessageTable ,handledBusIds);
						}

						familyObject.close(_context);
					}
					else
					{
						String childRelationClause = MCADUtil.replaceString(relationClause, busObjId, childObjID);
						processAsStoredChildren(_context, childObjID, relNames, childRelationClause, handledBusIds,relsAndEnds,relSelectionList, signatureActionTable, resultAndStatusTable, errorMessageTable);
						if(!handledBusIds.contains(childObjID) && !handledBusIds.contains(childMajorObjId))
						{
							verifyPrefinalizeState(_context, childObject,errorMessageTable);
							if(!handledBusIds.contains(childMajorObjId))
							{
								applySignaturetoChild(_context, childMajorObjId, childMajorState.getName(), signatureActionTable,  errorMessageTable);
							}
							doFinalizeOperations(_context, childObject, resultAndStatusTable, errorMessageTable,handledBusIds);
						}
					}
				}
				else if(!_util.isFinalState(childMajorState).equalsIgnoreCase("true"))
				{
					String childRelationClause = MCADUtil.replaceString(relationClause, busObjId, childObjID);
					processAsStoredChildren(_context, childObjID, relNames, childRelationClause, handledBusIds, relsAndEnds,relSelectionList, signatureActionTable, resultAndStatusTable, errorMessageTable);
					doPromoteOperations(_context, childMajorObjId, childObjID, childMajorState.getName(), policyName, signatureActionTable, handledBusIds, resultAndStatusTable, errorMessageTable);
				}

				handledBusIds.addElement(childObjID);
				handledBusIds.addElement(childMajorObjId);
			}

			childObject.close(_context);
			if(childMajorObj.isOpen())
				childMajorObj.close(_context);

			relationship.close(_context);
		}
	}


	
   
	public void validateParentChildStatesForPromotion(Context _context, IEFXmlNode cadObjectNode, DSCServerErrorMessageTable errorMessageTable)
	{
		IEFXmlNode sortedChildNodes = cadObjectNode;
		if(null != cadObjectNode && cadObjectNode.getChildCount() > 0)
			sortedChildNodes = MCADXMLUtils.sortXMLChildNodes(cadObjectNode, "selected", true, false); 

		Hashtable isSelectedObjectIdTable = new Hashtable();
		String isSelected			= (String)sortedChildNodes.getAttribute("selected");
		String objectId = (String)sortedChildNodes.getAttribute("id");
		isSelectedObjectIdTable.put(objectId, isSelected);

		getisSelectedIdTable(isSelectedObjectIdTable ,sortedChildNodes);
		areFirstLevelchildrenAhead(_context, isSelectedObjectIdTable, sortedChildNodes,errorMessageTable);
	}
	
   public void getisSelectedIdTable(Hashtable isSelectedIdTable , IEFXmlNode sortedChildNodes)
   {
	   Enumeration childElements = sortedChildNodes.elements();
	   while (childElements.hasMoreElements())
	   {
		   IEFXmlNode cadObjectListNode = (IEFXmlNode) childElements.nextElement();
		   getisSelectedIdTable( isSelectedIdTable , cadObjectListNode);
		   String isSelected			= (String)cadObjectListNode.getAttribute("selected");
		   String objectId = (String)cadObjectListNode.getAttribute("id");
		   isSelectedIdTable.put(objectId, isSelected);
	   }
   }
	
	public void areFirstLevelchildrenAhead(Context _context, Hashtable isSelectedIdTable, IEFXmlNode cadObjectNode,DSCServerErrorMessageTable errorMessageTable)
	{	
		try
		{	
			if (cadObjectNode != null)
			{
				checkIsParentAheadOfChild(_context ,  isSelectedIdTable, cadObjectNode, errorMessageTable);
				Enumeration childElements = cadObjectNode.elements();
				while (childElements.hasMoreElements())
				{
					IEFXmlNode cadObjectListNode = (IEFXmlNode) childElements.nextElement();
					areFirstLevelchildrenAhead(_context,isSelectedIdTable, cadObjectListNode,errorMessageTable);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void checkIsParentAheadOfChild(Context _context,Hashtable isSelectedIdTable,IEFXmlNode cadObjectListNode ,DSCServerErrorMessageTable errorMessageTable)
	{
		try
		{
			if (cadObjectListNode != null)
			{	
				String isSelected			= (String)cadObjectListNode.getAttribute("selected");
				String id = (String)cadObjectListNode.getAttribute("id");
				
				if((MCADAppletServletProtocol.TRUE).equalsIgnoreCase(isSelected) && !alreadyProcessedIds.contains(id))
				{
					BusinessObject busObj = new BusinessObject(id);
					State parentState = _util.getCurrentState(_context, busObj);
					StateList prStateLst = busObj.getStates(_context);
					StringList parentStateList = new StringList();

					Iterator itr = prStateLst.iterator();
					while(itr.hasNext())
					{
						State parentStateToAdd = (State)itr.next();
						parentStateList.add(parentStateToAdd.getName());
					}

					java.util.Hashtable relsAndEnds =  _generalUtil.getAllWheareUsedRelationships(_context, busObj, true,  MCADServerSettings.ASSEMBLY_LIKE);
					HashMap extrnlRefConflictTestList = new HashMap();
					Enumeration allRels = relsAndEnds.keys();
					while(allRels.hasMoreElements())
					{
						Relationship rel   = (Relationship)allRels.nextElement();
						String relTypeName = rel.getTypeName();
						
						String end = (String)relsAndEnds.get(rel);
						BusinessObject busToAdd = null;
						rel.open(_context);

						if (end.equals("from"))
							busToAdd = rel.getTo();
						else
							busToAdd = rel.getFrom();
						String busObjIdToAdd = busToAdd.getObjectId();
					
						Enumeration childElements = isSelectedIdTable.keys();
						String isChildSelected = "false";
						while (childElements.hasMoreElements())
						{
							String childID = (String)childElements.nextElement();
							if(childID.equalsIgnoreCase(busObjIdToAdd))
								isChildSelected	= (String)isSelectedIdTable.get(childID);							
						}
						busToAdd.open(_context);	
						State currentChildState  = _util.getCurrentState(_context, busToAdd);
						String childStateName = currentChildState.getName();				

						int childStateIndex = parentStateList.lastIndexOf(childStateName);
						int parentStateIndex = parentStateList.lastIndexOf(parentState.getName());
						busToAdd.close(_context);
						
						String attrMustInStructure  			= MCADMxUtil.getActualNameForAEFData(_context, "attribute_MustInStructure");
						String mustInStructure					= _util.getRelationshipAttributeValue(_context, rel, attrMustInStructure); 
						boolean isPromoteExternalRefLike 		= false; 
						boolean promoteUnRequiredRef 			= _globalConfig.getPromoteUnRequiredRef();
						
						if(_globalConfig.isRelationshipOfClass(relTypeName, MCADServerSettings.EXTERNAL_REFERENCE_LIKE))								
						{
							if(!promoteUnRequiredRef && mustInStructure.equalsIgnoreCase(MCADAppletServletProtocol.FALSE))
								isPromoteExternalRefLike = true;							
						}
						rel.close(_context);
						
						if(!isPromoteExternalRefLike)
						{
							if(isChildSelected.equalsIgnoreCase("false"))
							{
								if(parentStateIndex == childStateIndex)
								{
									String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.FirstLevelChildrenNotAheadOfParent") + ": \"" + busObj.getTypeName() + "\" \"" + busObj.getName()  + "\" \"" + busObj.getRevision() + "\"";
									errorMessageTable.addErrorMessage(_context, busObjIdToAdd,errorMessage );
									errorMessageList.add(errorMessage);
								}
							}
						}
					}
				}
				
				alreadyProcessedIds.add(id);
			}
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	
	protected void doPromoteOperations(Context _context, String majorBusObjectId, String objectId, String currentState, String policyName, Hashtable signatureActionTable, Vector handledBusIds, Hashtable resultAndStatusTable, DSCServerErrorMessageTable errorMessageTable) throws Exception
	{
		Vector connnectedMajorIds = new Vector();

		BusinessObject majorBus	= new BusinessObject(majorBusObjectId);
		majorBus.open(_context);
		String cadType			= _util.getCADTypeForBO(_context, majorBus);
		majorBus.close(_context);

		if (_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE) || _globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
		{
			BusinessObject minorBus = null;
			boolean isFinalized		= _generalUtil.isBusObjectFinalized(_context, majorBus);

			//This minor bus should be active minor
			minorBus		 = new BusinessObject(objectId);

			minorBus.open(_context);

			//String minorType = minorBus.getTypeName(); // {NDM] OP6

			minorBus.open(_context);

			if(_util.isMajorObject(_context, objectId))//_globalConfig.isMajorType(minorType)) // {NDM] OP6
				minorBus				= _util.getActiveMinor(_context,majorBus);

			String topLevelFamilyObjectID = "";
			if (_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
			{
				if(minorBus!=null && !isFinalized)
					topLevelFamilyObjectID = _generalUtil.getTopLevelFamilyObjectForInstance(_context, minorBus.getObjectId(_context));
				else
					topLevelFamilyObjectID = _generalUtil.getTopLevelFamilyObjectForInstance(_context, majorBus.getObjectId());
			}
			else
			{
				if (minorBus != null)
					topLevelFamilyObjectID = minorBus.getObjectId(_context);
				else
					topLevelFamilyObjectID = majorBusObjectId;
			}

			BusinessObject topLevelFamilyObject			= new BusinessObject(topLevelFamilyObjectID);
			topLevelFamilyObject.open(_context);
			BusinessObject topLevelFamilyMajorObject	= _util.getMajorObject(_context, topLevelFamilyObject);						

			if(topLevelFamilyMajorObject == null)
				topLevelFamilyMajorObject = topLevelFamilyObject;

			topLevelFamilyMajorObject.open(_context);

			topLevelFamilyObject.close(_context);
			topLevelFamilyMajorObject.close(_context);
		}

		if(!connnectedMajorIds.contains(majorBusObjectId))
			connnectedMajorIds.addElement(majorBusObjectId);

		majorBus.close(_context);
		Enumeration itr = connnectedMajorIds.elements();
		while (itr.hasMoreElements())
		{
			String majorId = (String)itr.nextElement();

			BusinessObject majorObject = new BusinessObject(majorId); //[NDM]

			if(!handledBusIds.contains(majorId))
			{
				
				
				if( _util.isSolutionBasedEnvironment(_context) )
				{
					String Args1[] = new String[2];
					Args1[0] = "DECIgnoreChangeOwnerCheck";
					Args1[1] = "true";
					_util.executeMQL(_context, "set env $1 $2", Args1);
					
				}
				
				
				String Args[] = new String[1];
				Args[0] = majorId;
				String mqlResult	= _util.executeMQL(_context, "promote bus $1", Args);

			
				Args = new String[1];
				Args[0] = "DECIgnoreChangeOwnerCheck";
				_util.executeMQL(_context,"unset env $1", Args);
				
				
				if(mqlResult.startsWith("false"))
				{
					mqlResult = mqlResult.substring(6);
					errorMessageTable.addErrorMessage(_context, majorId, mqlResult);
					errorMessageList.add(mqlResult);
				}
				else
				{
					addSelectedObjectToResponse(majorId);
				}

				handledBusIds.addElement(majorId);

				if(_globalConfig.isModificationEvent(MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION))
				{

					modifyUpdateStamp(_context, majorObject);
				}
			}
	
			transferRelationships(_context, majorObject, policyName, errorMessageTable);  //[NDM]
		}	

	}

	private void modifyUpdateStamp(Context _context, BusinessObject majorObject) throws MatrixException, Exception 
	{
		majorObject.open(_context);
		String majorCadType = _util.getCADTypeForBO(_context, majorObject);
		String mxType = majorObject.getTypeName();
		majorObject.close(_context);

		Vector attr =  _globalConfig.getCADAttribute(mxType, "$$current$$", majorCadType);

		if(!attr.isEmpty())
			_util.modifyUpdateStamp(_context, majorObject.getObjectId(_context));
	}

	protected void getEntireInstanceTower(Context _context, BusinessObject bus, Vector ObjectIdList, Vector finalObjectsList) throws Exception
	{
		Vector instanceTower = _generalUtil.getInstanceListForFamilyObject(_context, bus.getObjectId(_context));
		Enumeration keys = instanceTower.elements();
		while(keys.hasMoreElements())
		{
			BusinessObject instanceObj = (BusinessObject)keys.nextElement();
			String instanceId		   = instanceObj.getObjectId(_context);

			ObjectIdList.addElement(instanceId);
			finalObjectsList.addElement(instanceObj);
		}
	}

	protected void verifyPrefinalizeState(Context _context, BusinessObject bus, DSCServerErrorMessageTable errorMessageTable) throws Exception
	{
		String type		= bus.getTypeName();
		String name		= bus.getName();
		String revision = bus.getRevision();

		BusinessObject majorBus = bus;
		if(!_util.isMajorObject(_context, bus.getObjectId(_context)))//!_globalConfig.isMajorType(type)) // {NDM] OP6
			majorBus = _util.getMajorObject(_context, bus);			

		State currentState				= _util.getCurrentState(_context, majorBus);		
		String finalizationState		= _globalConfig.getFinalizationState(majorBus.getPolicy(_context).getName());
		String isPreFinalizationState	= _util.isPreFinalizationState(currentState, finalizationState);

		if(!isPreFinalizationState.equalsIgnoreCase("true"))
		{
			//throw new Exception(_serverResourceBundle.getString("mcadIntegration.Server.Message.BusinessObjectNotInPreFinalizationState") + ": \"" + type + "\" \"" + name  + "\" \"" + revision + "\"");
			String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.BusinessObjectNotInPreFinalizationState") + ": \"" + type + "\" \"" + name  + "\" \"" + revision + "\"";
			errorMessageTable.addErrorMessage(_context, majorBus.getObjectId(),errorMessage );
			errorMessageList.add(errorMessage);
		}
	}

	protected void doFinalizeOperations(Context _context, BusinessObject bus, Hashtable resultAndStatusTable,  DSCServerErrorMessageTable errorMessageTable, Vector handledBusIds) throws Exception
	{
		boolean isFinalizable = true;
		try
		{
			//Put the finalization button display check again..
			// This is to safe guard against inability of UI to refersh after
			// events like lock unlock etc. from CAD tool etc.
			//isObjectFinalizable(bus);
			isFinalizable = isObjectFinalizable(_context, bus, handledBusIds, errorMessageTable);
		}
		catch(Exception e)
		{
			String errorMessage = e.getMessage();
			errorMessageTable.addErrorMessage(_context, bus.getObjectId(),errorMessage );
			errorMessageList.add(errorMessage);
			//errorMessageBuffer.append("\n" + e.getMessage());
		}


		if( isFinalizable )
		{
			finalizeIndivisual(_context, bus, handledBusIds,errorMessageTable);
	}

	}

	protected void finalizeIndivisual(Context _context, BusinessObject bus, Vector handledBusIds,  DSCServerErrorMessageTable errorMessageTable) throws Exception
	{
		try
		{
			BusinessObject minorObj = null;
			
			boolean isMajor  = _util.isMajorObject(_context, bus.getObjectId());
			
			if(isMajor)
				minorObj = _util.getActiveMinor(_context,bus);
			else
				minorObj = bus;
				finalizeBO(_context, minorObj, handledBusIds, errorMessageTable);

			// Following code is required for Object based configuration mode
			/*String cadType = _util.getCADTypeForBO(_context, bus);

			if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
			{
				//NDM:- NE4
				boolean isMajorIns  = _util.isMajorObject(_context, bus.getObjectId());
				BusinessObject instMajorObject 	= null;
							
				if(isMajorIns)
					instMajorObject = bus;
				else
					instMajorObject 	= _util.getMajorObject(_context, bus);

				//BusinessObject instMajorObject 	= _util.getMajorObject(_context, bus);
				
				//End NDM:- NE4
				instMajorObject.open(_context);
				boolean isPushed    = false;

				try
				{
					com.matrixone.apps.domain.util.ContextUtil.pushContext(_context);
					isPushed    = true;

				//Start NDM:- NE4

				//_generalUtil.shuffleRelatinships(_context, bus, instMajorObject, true, false, MCADServerSettings.FAMILY_LIKE);
				//_generalUtil.shuffleRelatinships(_context, bus, instMajorObject, false, false, MCADServerSettings.FAMILY_LIKE);

				//End NDM:- NE4

				}catch(Exception me) {
					throw me;
				}
				finally
				{
					if(isPushed)
					{ 
						try {
							com.matrixone.apps.domain.util.ContextUtil.popContext(_context);
						} catch(Exception ex){
							String errorMessage = ex.getMessage();
							errorMessageTable.addErrorMessage(_context, instMajorObject.getObjectId(),errorMessage );
							errorMessageList.add(errorMessage);
						}
					}
				}
				instMajorObject.close(_context);
			}
			else if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
			{
				//Start [NDM] :- NE4
				boolean isMajorFam  = _util.isMajorObject(_context, bus.getObjectId());
				BusinessObject familyMajorObject 	= null;
			
				if(isMajorFam)
					familyMajorObject = bus;
				else
					familyMajorObject 	= _util.getMajorObject(_context, bus);
				//END [NDM] :- NE4


				// BusinessObject familyMajorObject 	= _util.getMajorObject(_context, bus);
				familyMajorObject.open(_context);

				boolean isPushed    = false;
				try
				{
					com.matrixone.apps.domain.util.ContextUtil.pushContext(_context);
					isPushed    = true;

				//Start [NDM]:- NE4
					//_generalUtil.shuffleRelatinships(_context, bus, familyMajorObject, true, true, MCADServerSettings.FAMILY_LIKE);
				//End [NDM]:- NE4

				}
				catch (Exception me) {
					throw me;
				}
				finally
				{
					if(isPushed)
					{ 
						try {
							com.matrixone.apps.domain.util.ContextUtil.popContext(_context);
						} catch(Exception ex){
							String errorMessage = ex.getMessage();
							errorMessageTable.addErrorMessage(_context, familyMajorObject.getObjectId(), errorMessage);
							errorMessageList.add(errorMessage);
						}
					}
				}
				familyMajorObject.close(_context);
			}*/
		}
		catch(Exception e)
		{
			if(!errorMessageTable.containsErrorMessage(bus.getObjectId(), e.getMessage()))
			{
				String errorMessage = e.getMessage();
				errorMessageTable.addErrorMessage(_context, bus.getObjectId(), errorMessage);
				errorMessageList.add(errorMessage);
			}
			//	MCADServerException.createException(e.getMessage(), e);
		}
	}

	public boolean isReplaceAttribPresent(Context _context, AttributeList busAttribLst)
	{
		boolean isReplaced = false;

		try
		{
			String isReplacementDone = MCADMxUtil.getActualNameForAEFData(_context, "attribute_DSC-IsReplacementDone");
			for(int i=0; i<busAttribLst.size(); i++)
			{
				Attribute attrib = (Attribute)busAttribLst.get(i);
				if(attrib.getName().compareToIgnoreCase(isReplacementDone) == 0)
				{
					String replaced = attrib.getValue();
					if(replaced.compareToIgnoreCase("true") == 0)
					{
						isReplaced = true;
						break;
					}
				}
			}
		}
		catch(MCADException e)
		{

		}

		return isReplaced;
	}

	public boolean isObjectFinalizable(Context _context, BusinessObject bo, Vector handledBusIds,  DSCServerErrorMessageTable errorMessageTable) throws MCADException
	{
		boolean isFinalizable = true;
		try
		{
			bo.open(_context);

			if(_generalUtil.isBusObjectFinalized(_context,bo))
			{
				String errorMessage =  _serverResourceBundle.getString("mcadIntegration.Server.Message.ObjectAlreadyFinalized");
				errorMessageTable.addErrorMessage(_context, bo.getObjectId(),errorMessage);
				errorMessageList.add(errorMessage);
				//MCADServerException.createException("Obejct is already Finalized", null);
			}
			String sType = bo.getTypeName();
			String sName = bo.getName();

			BusinessObject majorObject = null;
			//boolean bMinorType = _globalConfig.isMinorType(sType);
			boolean bMajorType = _util.isMajorObject(_context,bo.getObjectId());
			boolean bFinalized = false;

			//if minor obj get the major and see if it is finalized.
			if(!bMajorType)
				majorObject = _util.getMajorObject(_context, bo);
			else
				majorObject = bo;

			///If the replacement has been done on the parent,typically assembly then do not allow finalization.
			String parentType = _util.getCADTypeForBO(_context, majorObject);
			if(parentType.equalsIgnoreCase("assembly") )
			{
				AttributeList busAttribLst	= bo.getAttributeValues(_context,true);
				boolean result				= isReplaceAttribPresent(_context, busAttribLst);
				if(result)
				{
					String errorMessage =  _serverResourceBundle.getString("mcadIntegration.Server.Message.CannotFinalizeAsAssemblyHasReplacedDesign");
					errorMessageTable.addErrorMessage(_context, bo.getObjectId(),errorMessage);
					errorMessageList.add(errorMessage);
					//MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.CannotFinalizeAsAssemblyHasReplacedDesign"), null);
				}
			}

			//bFinalized = _generalUtil.isBusObjectFinalized(_context, majorObject);

			//can show finalize button only if it is not a finalized stream.
			if( !bFinalized )
			{
				/*if(!bMinorType)
				{
					BusinessObjectList minorsList = _util.getMinorObjects(_context, _busObject);
					int minorsListSize = minorsList.size();
					if( minorsListSize <= 0)
					{
						String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.CantFinalizeNoMinorFound");
						// Reach here only when the input bus is major, not finalized and has no minors
						errorMessageTable.addErrorMessage(_context, _busObject.getObjectId(), errorMessage);
						errorMessageList.add(errorMessage);
						//MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.CantFinalizeNoMinorFound"), null);
					}
				}*/
				if(!_generalUtil.checkLockStatus(_context, bo))
				{
					majorObject.open(_context);
					Hashtable exceptionDetails = new Hashtable();
					exceptionDetails.put("TYPE", majorObject.getTypeName());
					exceptionDetails.put("NAME",sName);
					exceptionDetails.put("REVISION",majorObject.getRevision());
					exceptionDetails.put("LOCKER",majorObject.getLocker(_context).getName());

					String errorMessage =_serverResourceBundle.getString("mcadIntegration.Server.Message.BusinessObjectLocked",exceptionDetails);
					errorMessageTable.addErrorMessage(_context, majorObject.getObjectId(),errorMessage );
					errorMessageList.add(errorMessage);
					//MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.BusinessObjectLocked",exceptionDetails), null);
					majorObject.close(_context);
				}
				if(!bMajorType)
				{
					majorObject.open(_context);
					String correspodingType = _util.getCorrespondingType(_context, sType);
					String majorType = majorObject.getTypeName();

					if(!correspodingType.equals(majorType))
					{
						Hashtable exceptionDetails = new Hashtable();
						exceptionDetails.put("TYPE", majorObject.getTypeName());
						exceptionDetails.put("NAME", sName);
						exceptionDetails.put("REVISION",majorObject.getRevision());
						String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.CannotPromoteObjectWithTypeChanged", exceptionDetails);
						errorMessageTable.addErrorMessage(_context, majorObject.getObjectId(),errorMessage);
						errorMessageList.add(errorMessage);
						//MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.CannotPromoteObjectWithTypeChanged"), null);
					}
					majorObject.close(_context);
				}
			}
			else if(!handledBusIds.contains(majorObject.getObjectId()))
			{
				BusinessObject finalizedMinor = _util.getFinalizedFromMinorObject(_context, majorObject);
				if(finalizedMinor == null)
					finalizedMinor = majorObject;
				finalizedMinor.open(_context);
				String finalizedMinorRevision = finalizedMinor.getRevision();
				finalizedMinor.close(_context);
				Hashtable tokensTable = new Hashtable();
				tokensTable.put("TYPE", sType);
				tokensTable.put("NAME", sName);
				tokensTable.put("REVISION", finalizedMinorRevision);
				String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.BusinessObjectFinalized", tokensTable);
				errorMessageTable.addErrorMessage(_context, majorObject.getObjectId(),errorMessage);
				errorMessageList.add(errorMessage);
				//MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.BusinessObjectFinalized", tokensTable), null);
			}
			else
			{
				isFinalizable = false;
			}

			bo.close(_context);
		}
		catch(Exception e)
		{
			if(!errorMessageTable.containsErrorMessage(bo.getObjectId(), e.getMessage()))
			{
				String errorMessage = e.getMessage();
				errorMessageTable.addErrorMessage(_context, bo.getObjectId(), errorMessage);
				errorMessageList.add(errorMessage);
			}
			
			//MCADServerException.createException(e.getMessage(), e);
		}
		return isFinalizable;
	}

	protected void finalizeBO(Context _context, BusinessObject bus, Vector handledBusIds,  DSCServerErrorMessageTable errorMessageTable) throws MCADException
	{

		BusinessObject majorObject = _util.getMajorObject(_context,bus);

		// Throw exception, incase finalization fails with the message in exception
		try
		{
			// Now do validations
			// [NE4] NDM implementation change 
			/*if(_globalConfig.getPromoteUnRequiredRef())
			areAllChildrenFinalized(_context, bus, errorMessageTable);
			*/
			// [NE4] NDM end 


		/*	if(_globalConfig.isCreateVersionObjectsEnabled())
			{
				// Attribute transfer
				_util.copyAttributesForFinalization(_context, _globalConfig, bus, majorObject,COPY_BLANK_ATTR_DURING_FINALIZATION);
			}
		*/
		//	bus.open(_context);
			majorObject.open(_context);

		/*	AttributeList attrList = new AttributeList();
			String fileMsgDigestAttrName = MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-FileMessageDigest");
			String fileMsgDigestAttrVal  = _util.getAttributeForBO(_context, bus,fileMsgDigestAttrName);	

			Attribute fileMsgDigest = new Attribute(new AttributeType(fileMsgDigestAttrName),fileMsgDigestAttrVal);
			attrList.addElement(fileMsgDigest);

			if(_util.isCDMInstalled(_context))
			{
				String moveFilesToVersionAttrName = MCADMxUtil.getActualNameForAEFData(_context, "attribute_MoveFilesToVersion");
				Attribute moveFilesToVersion = new Attribute(new AttributeType(moveFilesToVersionAttrName),"False");
				attrList.addElement(moveFilesToVersion);
			}

			if(attrList.size()>0)
			{
				majorObject.setAttributes(_context,attrList);
			}
			// transfer description Also.
			String desc = bus.getDescription(_context);
			if(!desc.equals("") || COPY_BLANK_ATTR_DURING_FINALIZATION)
				majorObject.setDescription(_context,desc);
			majorObject.update(_context);
*/
			// revision replace

			//manageRelationShipsAfterFinalization(_context, bus,majorObject,errorMessageTable);

			// start [NDM] :- NE4
			//do the file transfer from minor to major
			//_util.moveFilesFcsSupported(_context, bus, majorObject);	
			//End [NDM]:- NE4

			//Transfer PartSpecification relationship, its attributes and Trasfer attributes from
			// Finalized object to Part object as per mapping
			//TransferPartSpecRelAndAttributes(_context, majorObject, bus);

			//set active version
			//bus.open(_context);
			//_util.resetActiveVersion(_context, bus, _globalConfig);
			//_util.resetLatestVersion(_context, majorObject);				
			//bus.close(_context);

			// start [NDM] :- NE4
			// create relationship "finalized"
			//String sFinalizedRelName = MCADMxUtil.getActualNameForAEFData(_context, "relationship_Finalized");
			//_util.ConnectBusinessObjects(_context, bus, majorObject, sFinalizedRelName, true);
			//End [NDM] :- NE4
			majorObject.promote(_context);
			String sMajorOid = majorObject.getObjectId();
			addSelectedObjectToResponse(sMajorOid);
			
			if(_globalConfig.isModificationEvent(MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION))
				modifyUpdateStamp(_context, majorObject);
		
			if(!handledBusIds.contains(sMajorOid))
				handledBusIds.addElement(sMajorOid);

			//bus.close(_context);
			majorObject.close(_context);
		}
		catch(Exception e)
		{
			Hashtable exceptionDetails = new Hashtable();
			String type = majorObject.getTypeName();
			try
			{
				type = MCADMxUtil.getNLSName(_context,"Type", type, "", "", _serverResourceBundle.getLanguageName());
			}
			catch(Exception ex)
			{
				System.out.println("[DSCErrorMessageTable.addErrorMessage] Exception occured while getting nls for type: " + ex.getMessage());
			}
			exceptionDetails.put("TYPE", type);
			exceptionDetails.put("NAME", majorObject.getName());
			exceptionDetails.put("REVISION", majorObject.getRevision());

			String sMsg =  _serverResourceBundle.getString("mcadIntegration.Server.Message.FinalizationFailed", exceptionDetails);
			String errorMessage = MCADServerException.getErrorMessage(sMsg, e);
			errorMessageTable.addErrorMessage(_context, majorObject.getObjectId(), errorMessage);
			errorMessageList.add(errorMessage);
		}
	}

	//[NDM] start
	protected void transferRelationships(Context _context, BusinessObject majorObject, String policyName, DSCServerErrorMessageTable errorMessageTable) throws Exception
	{
		BusinessObject previousObject = majorObject.getPreviousRevision(_context);
		
		State currentState = _util.getCurrentState(_context, majorObject);
		String releasedState	 			= _globalConfig.getReleasedState(policyName);

		String Args[] = new String[5];
		Args[0] = "eService Trigger Program Parameters";
		Args[1] = "PolicyECPartStateApprovedPromoteAction";
		Args[2] = "FloatEBOMToEnd";
		Args[3] = "current";
		Args[4] = "|";
		       
		BusinessObject BO = new BusinessObject("eService Trigger Program Parameters","PolicyECPartStateApprovedPromoteAction","FloatEBOMToEnd","");        
		
		if(BO.exists(_context))
		{          
			
		    StringList slSelectsForInputID = new StringList(1);
		    slSelectsForInputID.addElement("current");		

		    StringList slOid = new StringList(1);				
		    slOid.addElement(BO.getObjectId(_context));

		    String [] oids	  = new String [slOid.size()];
		    slOid.toArray(oids);	

		    BusinessObjectWithSelectList busWithSelectionList = BusinessObject.getSelectBusinessObjectData(_context, oids, slSelectsForInputID);
		    BusinessObjectWithSelect busObjectWithSelect 		= (BusinessObjectWithSelect)busWithSelectionList.elementAt(0);
		    String stateECTrigger         = (String)busObjectWithSelect.getSelectData("current");
            	                
			boolean bisDECFloatOnRelEnabled = _util.isFloatOnReleaseEnabled(_context);             		

			if((stateECTrigger.equalsIgnoreCase("Active")&& !bisDECFloatOnRelEnabled)||(stateECTrigger.equalsIgnoreCase("Inactive")&& bisDECFloatOnRelEnabled))
			{     			
				String errorString = _serverResourceBundle.getString("mcadIntegration.Server.Message.CannotPromoteAsFloatOnReleaseNotInSync");
				errorMessageList.add(errorString);
				MCADServerException.createException(errorString, null);        		
			}                
			else if(stateECTrigger.equalsIgnoreCase("Active") && bisDECFloatOnRelEnabled){
		if(currentState.getName().equalsIgnoreCase(releasedState) && (previousObject != null) && !(previousObject.toString().trim().equals("..")))
		{	
			transferPartSpecRelAndAttributes(_context,majorObject);
			_generalUtil.shuffleAssemblyLikeRelatinships(_context, previousObject, majorObject, false, false, MCADServerSettings.ASSEMBLY_LIKE);
			_generalUtil.shuffleRelatinships(_context, previousObject, majorObject, false, false, MCADServerSettings.EXTERNAL_REFERENCE_LIKE);
		}
	}
		}

	}
	//[NDM] ENd
	
	protected void TransferPartSpecRelAndAttributes(Context _context, BusinessObject majorObject, BusinessObject minor) throws Exception
	{
		String partSpecRelName = MCADMxUtil.getActualNameForAEFData(_context,"relationship_PartSpecification");

		boolean success = true;

		String sVerOfRelName	 = MCADMxUtil.getActualNameForAEFData(_context, "relationship_VersionOf");
		String [] oids			 = new String[1];
		oids[0]					 = majorObject.getObjectId(_context);
		StringList busSelectList = new StringList(5);
		String selectVersionsOnMajor	 = "to[" + sVerOfRelName + "].from.id";

		busSelectList.add(selectVersionsOnMajor);
		BusinessObjectWithSelectList busWithSelectList	= BusinessObjectWithSelect.getSelectBusinessObjectData(_context, oids, busSelectList);
		BusinessObjectWithSelect busWithSelect			= busWithSelectList.getElement(0);
		StringList relatedBusidsList					= (StringList)busWithSelect.getSelectDataList(selectVersionsOnMajor);

		String [] oids1			  = new String[relatedBusidsList.size()];
		relatedBusidsList.toArray(oids1);
		StringList busSelectList1 			= new StringList(5);
		String SELECT_CONNECTED_PARTS	  	= "to[" + partSpecRelName + "].from.id";
		busSelectList1.add("id");
		busSelectList1.add(SELECT_CONNECTED_PARTS);

		BusinessObjectWithSelectList busWithSelectList1		= BusinessObjectWithSelect.getSelectBusinessObjectData(_context, oids1, busSelectList1);
		StringList partList	= null;
		for(int j=0; j<busWithSelectList1.size(); j++)
		{
			BusinessObjectWithSelect busWithSelect1			= busWithSelectList1.getElement(j);
			partList										= (StringList)busWithSelect1.getSelectDataList(SELECT_CONNECTED_PARTS);
			String minorID 									= busWithSelect1.getSelectData("id");

			if(partList!=null && !partList.isEmpty())
			{
				BusinessObject minorObject = new BusinessObject(minorID);
				success = _util.moveRelationShips(_context, minorObject, majorObject, partSpecRelName, "to", true, true);
				break;
			}		
		}	

		//Part Spec relationships are moved now cad object transfer attributes.
		if(success)
		{
			String end = "to";
			Hashtable relAttribListTable = new Hashtable();
			BusinessObjectList boList	 = _util.getRelatedBusinessObjects(_context, majorObject, partSpecRelName, end, relAttribListTable);			
			BusinessObjectItr itr		 = new BusinessObjectItr(boList);
			BusinessObject partObject	 = null;

			while(itr.next())
			{
				partObject = itr.obj();
				partObject.open(_context);
				String[] args = new String[5];
				args[0] = majorObject.getObjectId();
				args[1] = partObject.getObjectId();
				args[2] = ""; //TODO:instanceName is passed blank

				String[] packedGCO = new String[2];
				packedGCO = JPO.packArgs(_globalConfig);
				args[3]  = packedGCO[0];
				args[4] = packedGCO[1];

				partObject.close(_context);
				ebomFMJPO.transferCadAttribsToPart(_context, args);
			}
		}
	}

	//[NDM] Start
	public void transferPartSpecRelAndAttributes(Context _context , BusinessObject obj) throws Exception
	{
		String partSpecRelName = MCADMxUtil.getActualNameForAEFData(_context,"relationship_PartSpecification");
		
			String end = "to";
			
			Hashtable relAttribListTable = new Hashtable();
	
			BusinessObjectList boList	 = _util.getRelatedBusinessObjects(_context, obj, partSpecRelName, end, relAttribListTable);
			BusinessObjectItr itr		 = new BusinessObjectItr(boList);
			BusinessObject partObject	 = null;
	
			while(itr.next())
			{
				partObject = itr.obj();
				partObject.open(_context);
				
				String[] args = new String[5];
				args[0] = obj.getObjectId();
				args[1] = partObject.getObjectId();
				args[2] = ""; //TODO:instanceName is passed blank

				String[] packedGCO = new String[2];
				packedGCO = JPO.packArgs(_globalConfig);
				args[3]  = packedGCO[0];
				args[4] = packedGCO[1];

				partObject.close(_context);
				ebomFMJPO.transferCadAttribsToPart(_context, args);
			}
	}
	//[NDM] ENd
	
	/**
	 * Checks if all the first level children of the input bus object are finalized
	 * (Only CAD Subcomponent relationship is considered for now..)
	 * throws exception if any child is not finalized
	 */
	protected void areAllChildrenFinalized(Context _context, BusinessObject inBus, DSCServerErrorMessageTable errorMessageTable) throws MCADException
	{
		// First get all relatinships
		try
		{
			java.util.Hashtable relsAndEnds =  _generalUtil.getAllWheareUsedRelationships(_context, inBus, true,  MCADServerSettings.ASSEMBLY_LIKE);

			HashMap extrnlRefConflictTestList = new HashMap();

			Enumeration allRels = relsAndEnds.keys();
			while(allRels.hasMoreElements())
			{
				// check "end", this is end for child node
				Relationship rel   = (Relationship)allRels.nextElement();
				String relTypeName = rel.getTypeName();

				boolean isExternalReferenceLike = false;

				if(_globalConfig.isRelationshipOfClass(relTypeName, MCADServerSettings.EXTERNAL_REFERENCE_LIKE))
				{
					isExternalReferenceLike = true;
				}

				String end = (String)relsAndEnds.get(rel);

				// add to new list, depending on the "end"!!
				BusinessObject busToAdd = null;
				rel.open(_context);

				// The other object is at the other "end"
				if (end.equals("from"))
				{
					busToAdd = rel.getTo();
				}
				else
				{
					busToAdd = rel.getFrom();
				}

				rel.close(_context);

				if(!isExternalReferenceLike && !isFinalizedOrFinalizationCandidate(_context, busToAdd))
				{
					Hashtable msgTable = new Hashtable();
					msgTable.put("TYPE", busToAdd.getTypeName());
					msgTable.put("NAME", busToAdd.getName());
					msgTable.put("REVISION", busToAdd.getRevision());
					
                                        String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.ChildObjectNotFinalized", msgTable);
					errorMessageTable.addErrorMessage(_context, inBus.getObjectId(),errorMessage );
					errorMessageList.add(errorMessage);
					//MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.ChildObjectNotFinalized", msgTable), null);
				}					
				else if(isExternalReferenceLike)
				{
					extrnlRefConflictTestList.put(busToAdd.getObjectId(), String.valueOf(isFinalizedOrFinalizationCandidate(_context, busToAdd)));
				}
			}		

			if(!extrnlRefConflictTestList.isEmpty())
			{
				Vector uniqueGroupsList = new Vector();

				Vector inputList = new Vector();

				inputList.addAll(extrnlRefConflictTestList.keySet());

				if(inputList.size() > 1)
				{
					_generalUtil.keepUniqueIdsInList(_context, inputList, uniqueGroupsList, true);

					for(int i = 0 ; i < uniqueGroupsList.size(); i++)
					{
						Vector groupedItems = (Vector) uniqueGroupsList.elementAt(i);

						for(int j = 0 ; j < groupedItems.size(); j++)
						{
							String busId = (String)groupedItems.elementAt(j);

							String isFinalizedOrFinalizationCandidate = (String)extrnlRefConflictTestList.get(busId);

							if(isFinalizedOrFinalizationCandidate.equalsIgnoreCase(MCADAppletServletProtocol.TRUE))
							{
								break;
							}
							else if (j == (groupedItems.size() - 1))
							{
								//Time to throw error
								BusinessObject busObject = new BusinessObject(busId);
								busObject.open(_context);

								Hashtable msgTable = new Hashtable();
								msgTable.put("TYPE", busObject.getTypeName());
								msgTable.put("NAME", busObject.getName());
								msgTable.put("REVISION", busObject.getRevision());

								String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.ChildObjectNotFinalized", msgTable);
								errorMessageTable.addErrorMessage(_context, inBus.getObjectId(),errorMessage );
								errorMessageList.add(errorMessage);
								
								busObject.close(_context);
							}
						}
					}
				}
				else
				{
					String busId = (String)inputList.elementAt(0);

					String isFinalizedOrFinalizationCandidate = (String)extrnlRefConflictTestList.get(busId);

					if(!isFinalizedOrFinalizationCandidate.equalsIgnoreCase(MCADAppletServletProtocol.TRUE))
					{
						//Time to throw error
						BusinessObject busObject = new BusinessObject(busId);
						busObject.open(_context);

						Hashtable msgTable = new Hashtable();
						msgTable.put("TYPE", busObject.getTypeName());
						msgTable.put("NAME", busObject.getName());
						msgTable.put("REVISION", busObject.getRevision());

						String errorMessage =  _serverResourceBundle.getString("mcadIntegration.Server.Message.ChildObjectNotFinalized", msgTable);
						errorMessageTable.addErrorMessage(_context, inBus.getObjectId(),errorMessage);
						errorMessageList.add(errorMessage);

						busObject.close(_context);
					}
				}
			}
		}
		catch(Exception me)
		{
			if(!errorMessageTable.containsErrorMessage(inBus.getObjectId(), me.getMessage()))
			{
				String errorMessage = me.getMessage();
				errorMessageTable.addErrorMessage(_context, inBus.getObjectId(),errorMessage );
				errorMessageList.add(errorMessage);
			}
		}
	}

	private boolean isFinalizedOrFinalizationCandidate(Context _context, BusinessObject inputBus) throws Exception
	{
		boolean returnValue = false;

		// Start [NDM] NE4:-
		//	boolean isMajor = _globalConfig.isMajorType(inputBus.getTypeName());

		boolean isMajor = 	_util.isMajorObject(_context,inputBus.getObjectId());
	

		// End [NDM] NE4:-

		if(!isMajor)
		{
			returnValue = (_generalUtil.isBusObjectFinalized(_context, inputBus) || finalizationCandidates.contains(inputBus.getObjectId()) || finalizationCandidates.contains(_util.getMajorObject(_context, inputBus).getObjectId()));
		}
		else
		{
			returnValue = true;
		}

		return returnValue;
	}


	/**
	 * Do proper rev replace after successful finalization.
	 * can implement business rules, default impl. is
	 * copy all "from" relationships on finalized object
	 * move all "to" relationships on finalized object
	 */
	protected boolean manageRelationShipsAfterFinalization(Context _context, BusinessObject finalizedFromObj,BusinessObject finalizedToObj, DSCServerErrorMessageTable errorMessageTable) throws Exception
	{
		boolean bRet = true;
		boolean isPushed    = false;

		try
		{
			com.matrixone.apps.domain.util.ContextUtil.pushContext(_context);
			isPushed    = true;

			// Move all DERIVEDOUTPUT_LIKE relationship from Minor to Major
			_generalUtil.shuffleRelatinships(_context, finalizedFromObj, finalizedToObj, true, false, MCADServerSettings.DERIVEDOUTPUT_LIKE);

			// Copy all from relationships, Skip the External Ref relations
			//_generalUtil.shuffleRelatinships(finalizedFromObj, finalizedToObj, true, true, MCADServerSettings.ASSEMBLY_LIKE);

			_generalUtil.shuffleAssemblyLikeRelatinships(_context, finalizedFromObj, finalizedToObj, true, true, MCADServerSettings.ASSEMBLY_LIKE);
			// Handle external ref relations
			// Move all from Ext. Ref. relationships
			_generalUtil.shuffleRelatinships(_context, finalizedFromObj, finalizedToObj, true, false, MCADServerSettings.EXTERNAL_REFERENCE_LIKE);

			// Move all to relatinships, replace FinalizedFromBO with FinalizedToBO, Skip the External Ref relations
			//_generalUtil.shuffleRelatinships(finalizedFromObj, finalizedToObj, false, false, MCADServerSettings.ASSEMBLY_LIKE);
			_generalUtil.shuffleAssemblyLikeRelatinships(_context, finalizedFromObj, finalizedToObj, false, false, MCADServerSettings.ASSEMBLY_LIKE);

			// Handle external ref relations
			// Move all from Ext. Ref. relationships
			_generalUtil.shuffleRelatinships(_context, finalizedFromObj, finalizedToObj, false, false, MCADServerSettings.EXTERNAL_REFERENCE_LIKE);
			
			_generalUtil.shuffleActiveInstanceRelatinship(_context, finalizedFromObj, finalizedToObj);
		}
		catch(Exception me)
		{
			String errorMessage =  me.getMessage();
			errorMessageTable.addErrorMessage(_context, finalizedFromObj.getObjectId(),errorMessage);
			errorMessageList.add(errorMessage);
			//MCADServerException.createException(me.getMessage(), me);
		}
		finally
		{
			if(isPushed)
			{ 
				try
				{
					com.matrixone.apps.domain.util.ContextUtil.popContext(_context);
				}
				catch(Exception ex)
				{
					String errorMessage =  ex.getMessage();
					errorMessageTable.addErrorMessage(_context, finalizedFromObj.getObjectId(), errorMessage);
					errorMessageList.add(errorMessage);
				}
			}
		}

		return bRet;
	}

	// [NDM] NE4 start:-

	/*protected void createVirtualStructure(Context _context, IEFXmlNode finalizePacket, DSCServerErrorMessageTable errorMessageTable) throws Exception
	{
		IEFXmlNode structureNode = MCADXMLUtils.getChildNodeWithName(finalizePacket, "structure");
		Hashtable objIDNodeListTable	= new Hashtable();
		Hashtable objIDChangedIDTable	= new Hashtable();

		Enumeration rootCadObjectElements = structureNode.elements();
		Hashtable virtualStructureCreatedforNodes = new Hashtable();

		Hashtable processedIdcadobjNodeMap = new Hashtable();
		Hashtable objectIdRevisedBusObjMap = new Hashtable();
		
		
		while(rootCadObjectElements.hasMoreElements())
		{
			// Get top node
			IEFXmlNode rootCadObjectNode = (IEFXmlNode)rootCadObjectElements.nextElement();
			errorMessageTable.setRootId((String)rootCadObjectNode.getAttribute("id"));

			createVirtaulStructureforNode(_context, rootCadObjectNode, objIDNodeListTable, objIDChangedIDTable, virtualStructureCreatedforNodes, errorMessageTable,processedIdcadobjNodeMap, objectIdRevisedBusObjMap);
		}
	}

	protected void createVirtaulStructureforNode(Context _context, IEFXmlNode cadObjectNode, Hashtable objIDNodeListTable, Hashtable objIDChangedIDTable, Hashtable virtualStructureCreatedforNodes, DSCServerErrorMessageTable errorMessageTable,Hashtable processedIdcadobjNodeMap, Hashtable objectIdRevisedBusObjMap) throws Exception
	{
		Enumeration childCadObjectElements = cadObjectNode.elements();

		while(childCadObjectElements.hasMoreElements())
		{
			String parentid = cadObjectNode.getAttribute("id");
			
			processedIdcadobjNodeMap.put(parentid, cadObjectNode);

			IEFXmlNode childCadObjectNode = (IEFXmlNode)childCadObjectElements.nextElement();

			// call the function recursively
			createVirtaulStructureforNode(_context, childCadObjectNode, objIDNodeListTable, objIDChangedIDTable, virtualStructureCreatedforNodes, errorMessageTable,processedIdcadobjNodeMap, objectIdRevisedBusObjMap);
			
			processedIdcadobjNodeMap.remove(parentid);
		}

		boolean isFloatedVerticalViewNode = isFloatedVerticalViewNode(cadObjectNode);

		if(!virtualStructureCreatedforNodes.containsKey((String)cadObjectNode.getAttribute("id")))
		{
			// check if the revsion is required
			boolean isStructureDisturbed = isRevisionRequired(_context, cadObjectNode);

			String currObjID		= (String)cadObjectNode.getAttribute("id");
			String isPreFinalizationState = (String)cadObjectNode.getAttribute("isprefinalizationstate");
			String changedID 			  = currObjID;

			Vector sharedNodeList	= (Vector)objIDNodeListTable.get(currObjID);

			if(sharedNodeList == null)
			{
				sharedNodeList = new Vector();
				objIDNodeListTable.put(currObjID, sharedNodeList);
			}

			boolean isOtherNodeInPath = processedIdcadobjNodeMap.containsKey(currObjID);
			
			if(!isStructureDisturbed && isOtherNodeInPath)
			{
				IEFXmlNode otherCADObjectNode = (IEFXmlNode) processedIdcadobjNodeMap.get(currObjID);
				isStructureDisturbed 		  = isRevisionRequired(_context, otherCADObjectNode);
				
				if(!isStructureDisturbed && cadObjectNode.getParentNode() != null && isRevisionRequired(_context, cadObjectNode.getParentNode()))
					isStructureDisturbed = true;
			}
			
			if(isStructureDisturbed && isPreFinalizationState.equalsIgnoreCase(MCADAppletServletProtocol.TRUE))
			{
				if(_globalConfig.isCreateVersionObjectsEnabled())
					createRevisonAndRelationships(_context, cadObjectNode, virtualStructureCreatedforNodes, errorMessageTable, processedIdcadobjNodeMap,  objectIdRevisedBusObjMap, objIDNodeListTable);
				else
					connectStructureWithoutRevision(_context, cadObjectNode);

				changedID = (String)cadObjectNode.getAttribute("id");
				objIDChangedIDTable.put(currObjID, changedID);

				for(int i=0; i<sharedNodeList.size(); i++)
				{
					IEFXmlNode cadNode = (IEFXmlNode)sharedNodeList.elementAt(i);
					cadNode.setAttributeValue("id", changedID);
				}

			}

			if(isFloatedVerticalViewNode && isPreFinalizationState.equalsIgnoreCase(MCADAppletServletProtocol.TRUE))
			{
				//isFloatedVerticalViewNode - with create version of drawing cannot float
				if(!isStructureDisturbed && _globalConfig.isCreateVersionObjectsEnabled())
				{
					BusinessObject revisedMinorObject	= reviseWithLatestMinor(_context, currObjID);			

					changedID = revisedMinorObject.getObjectId(_context);

					cadObjectNode.setAttributeValue("id", changedID);

					objIDChangedIDTable.put(currObjID, changedID);

					virtualStructureCreatedforNodes.put(currObjID, changedID);

					finalizationCandidates.addElement(changedID);

					for(int i=0; i<sharedNodeList.size(); i++)
					{
						IEFXmlNode cadNode = (IEFXmlNode)sharedNodeList.elementAt(i);
						cadNode.setAttributeValue("id", changedID);
					}
				}

				IEFXmlNode parentNode = cadObjectNode.getParentNode();

				String childbusId = parentNode.getAttribute("id");

				_generalUtil.shuffleAssemblyLikeRelatinshipsForFloatedVerticalView(_context, new BusinessObject(currObjID), new BusinessObject(changedID), MCADAppletServletProtocol.ASSEMBLY_LIKE, childbusId);

			}

			if(!isStructureDisturbed && !isFloatedVerticalViewNode && objIDChangedIDTable.containsKey(currObjID))
			{
				changedID = (String)objIDChangedIDTable.get(currObjID);
				cadObjectNode.setAttributeValue("id", changedID);
			}

			sharedNodeList.addElement(cadObjectNode);
		}
		else
		{
			cadObjectNode.setAttributeValue("id" , (String)virtualStructureCreatedforNodes.get((String)cadObjectNode.getAttribute("id")));
		}
	}

	protected boolean isFloatedVerticalViewNode(IEFXmlNode cadObjectNode)
	{
		boolean isFloatedVerticalViewNode = false;

		String isSelected		  = (String)cadObjectNode.getAttribute("selected");
		String isVerticalViewNode = (String)cadObjectNode.getAttribute("isverticalviewnode");
		String isFloatedNode	  = (String)cadObjectNode.getAttribute("isfloated");

		if(isVerticalViewNode.equalsIgnoreCase("true") && isFloatedNode.equalsIgnoreCase("true") && (MCADAppletServletProtocol.TRUE).equalsIgnoreCase(isSelected))
			isFloatedVerticalViewNode = true;

		return isFloatedVerticalViewNode;
	}

	protected boolean isRevisionRequired(Context _context, IEFXmlNode cadObjectNode) throws Exception
	{
		String objectID			= (String)cadObjectNode.getAttribute("id");
		String isSelected		= (String)cadObjectNode.getAttribute("selected");
		String cadType			= (String)cadObjectNode.getAttribute("cadtype");

		if(!(MCADAppletServletProtocol.TRUE).equalsIgnoreCase(isSelected))
			return false;

		Vector childObjectIDs	= new Vector();
		String childBusInfoAndRelIds = "";

		if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE) || _globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
		{
			if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
				childBusInfoAndRelIds = childBusInfoAndRelIds + "\n" + _generalUtil.getChildRelsAndIdsForCheckout(_context,objectID, true, MCADAppletServletProtocol.FAMILY_LIKE, "", "", "", null);
			else
				childBusInfoAndRelIds = childBusInfoAndRelIds + "\n" + _generalUtil.getChildRelsAndIdsForCheckout(_context,objectID, false, MCADAppletServletProtocol.FAMILY_LIKE, "", "", "", null);
			
			childBusInfoAndRelIds = childBusInfoAndRelIds + "\n" + _generalUtil.getChildRelsAndIdsForCheckout(_context,objectID, true, MCADAppletServletProtocol.ASSEMBLY_LIKE, "", "", "", null);
		}
		else
			childBusInfoAndRelIds = _generalUtil.getChildRelsAndIdsForCheckout(_context, objectID, true, MCADAppletServletProtocol.ASSEMBLY_LIKE, "", "", "", null);

		StringTokenizer childBusInfoTokens	= new StringTokenizer(childBusInfoAndRelIds, "\n");

		while (childBusInfoTokens.hasMoreTokens())
		{
			String childBusInfo = childBusInfoTokens.nextToken();

			Enumeration childBusInfoElements = MCADUtil.getTokensFromString(childBusInfo, "|");
			String childLevel				= (String) childBusInfoElements.nextElement();
			String relationshipName			= (String) childBusInfoElements.nextElement();
			String relationshipDirection	= (String) childBusInfoElements.nextElement();
			String childObjectID			= (String) childBusInfoElements.nextElement();
			String relationshipID			= (String) childBusInfoElements.nextElement();

			childObjectIDs.addElement(childObjectID);
		}

		Enumeration cadObjectElements = cadObjectNode.elements();
		while(cadObjectElements.hasMoreElements())
		{
			IEFXmlNode childObjectNode = (IEFXmlNode)cadObjectElements.nextElement();

			String childObjectID		= (String)childObjectNode.getAttribute("id");
			String isChildSelected		= (String)childObjectNode.getAttribute("selected");
			String isVerticalViewNode	= (String)childObjectNode.getAttribute("isverticalviewnode");			

			if(isVerticalViewNode.equalsIgnoreCase("true"))
			{
				continue;
			}

			BusinessObject childBusObject = new BusinessObject(childObjectID);
			childBusObject.open(_context);

			The "isChildSelectable" flag is for handling the case when promoting a structure which after lateral navigation contains
			 a child which is already finalized, and which does not participate in the as-stored structure. In this case, even if the child is not selected,
			 the promotion of the structure to finalized state should be allowed 

			String childBusType				= childBusObject.getTypeName();
			BusinessObject childMajorObject = null;
			boolean isChildSelectable		= false;

			if(_globalConfig.isMinorType(childBusType))
				childMajorObject = _util.getMajorObject(_context, childBusObject);
			else
				childMajorObject = childBusObject;

			if((MCADAppletServletProtocol.TRUE).equalsIgnoreCase(isChildSelected) || _generalUtil.isBusObjectFinalized(_context, childMajorObject))
				isChildSelectable  = true;

			childBusObject.close(_context);

			if(!childObjectIDs.contains(childObjectID) && isChildSelectable)
				return true;
		}

		return false;
	}

	protected void createRevisonAndRelationships(Context _context, IEFXmlNode cadObjectNode, Hashtable virtualStructureCreatedforNodes, DSCServerErrorMessageTable errorMessageTable, Hashtable processedIdcadobjNodeMap, Hashtable objectIdRevisedBusObjMap, Hashtable objIDNodeListTable) throws Exception
	{
		String currentObjectId = null;	//Place holder for storing object id for giving object details in exception block.
		boolean bGrantCreatorFromconnectAccess = false;
		StringList slMajorIdWithAccess = new StringList();
		String key = "DECVirtualStructureFromConnect";
		try
		{
			String minorObjectID				= cadObjectNode.getAttribute("id");

			boolean isOtherNodeInPath           = processedIdcadobjNodeMap.containsKey(minorObjectID);
		
			currentObjectId						= minorObjectID;

			Hashtable revReplaceTable			= new Hashtable();			
		
			BusinessObject revisedMinorObject   = null;

			if(objectIdRevisedBusObjMap.containsKey(currentObjectId))
				revisedMinorObject = (BusinessObject)objectIdRevisedBusObjMap.get(currentObjectId);
			else
				revisedMinorObject	          = reviseVersionedTypeObject(_context, minorObjectID, revReplaceTable,virtualStructureCreatedforNodes,objIDNodeListTable);	

			String revisedMinorObjectID = revisedMinorObject.getObjectId();

			String revisedFamilyId = null;
			
			String familyId =(String)revisedInstanceFamilyIdTable.get(revisedMinorObjectID);
			
			Vector revisedNodeList=null;
			

			if(familyId != null && (String)virtualStructureCreatedforNodes.get(familyId) != null) 
			{			
				revisedFamilyId = (String)virtualStructureCreatedforNodes.get(familyId);
				revisedNodeList = (Vector)objIDNodeListTable.get(familyId);	
	
			}
			
			if(revisedNodeList != null)
			{
				
				for(int i=0; i<revisedNodeList.size(); i++)
				{
					IEFXmlNode cadNode = (IEFXmlNode)revisedNodeList.elementAt(i);
	
					cadNode.setAttributeValue("id", revisedFamilyId);
				}
                         }
			
			Hashtable childIdCadNodeTable		= getChildIdCadNodeTable(_context, cadObjectNode, revReplaceTable);
		
			String vplmCreatorRoleName = MCADMxUtil.getActualNameForAEFData(_context, "role_VPLMCreator");
			
			StringList roleList = new StringList();
			roleList.add(vplmCreatorRoleName);
			
			
			
			//if other node with same id present in path just create the new revision, cached it and connect from that node. IR-152872V6R2013
			if(!isOtherNodeInPath)
			{	
			virtualStructureCreatedforNodes.put(minorObjectID,revisedMinorObjectID);			

			_generalUtil.shuffleRelatinships(_context, new BusinessObject(minorObjectID), revisedMinorObject, true, false, MCADServerSettings.DERIVEDOUTPUT_LIKE);
			
			finalizationCandidates.addElement(revisedMinorObjectID);

			Enumeration childCadObjectElements = childIdCadNodeTable.keys();
			while(childCadObjectElements.hasMoreElements())
			{
				String childObjectID		= (String)childCadObjectElements.nextElement();
				
				
				
				if(MCADMxUtil.isSolutionBasedEnvironment(_context))
				{
					String currentRole = _context.getRole();
					if(currentRole.contains(vplmCreatorRoleName)){
						BusinessObject childTempObject = new BusinessObject(childObjectID);
						if(_generalUtil.isBusObjectFinalized(_context, childTempObject)){
							bGrantCreatorFromconnectAccess = true;
							_generalUtil.grantAccessForRole(_context,childObjectID,roleList,"fromconnect",key);
							slMajorIdWithAccess.addElement(childObjectID);
						}
					}			
				}
				
				currentObjectId				= childObjectID;
				Vector cadObjectNodeList	= (Vector)childIdCadNodeTable.get(childObjectID);

				for(int i=0; i<cadObjectNodeList.size(); i++)
				{
					IEFXmlNode childObjectNode			= (IEFXmlNode)cadObjectNodeList.elementAt(i);
					String isChildVerticalViewNode		= childObjectNode.getAttribute("isverticalviewnode");
					String childObjectRelationshipID	= (String)childObjectNode.getAttribute("rid");

					if(childObjectRelationshipID != null && !childObjectRelationshipID.equals(""))
					{
						Relationship childParentRelationship = new Relationship(childObjectRelationshipID);
						childParentRelationship.open(_context);

						String childParentRelationshipType	= childParentRelationship.getTypeName();
						Hashtable attrNameVals			= _util.getRelationshipAttrNameValMap(_context, childParentRelationship);

						childParentRelationship.close(_context);
						String cadRelTypeName	= _globalConfig.getCADRelFromMxRel(childParentRelationshipType);
						String relDirection		= _globalConfig.getRelatinshipEndFromMapping(cadRelTypeName, childParentRelationshipType);

						BusinessObject childObject = new BusinessObject(childObjectID);

						childObject.open(_context);					

						StringList selectList = new StringList();
						selectList.add(MCADAppletServletProtocol.STR_LOGICALID);
						
						Hashtable basicToSet = _generalUtil.getSelectDataForRelation(_context, childObjectRelationshipID, selectList);
						
						if(isChildVerticalViewNode == null || !isChildVerticalViewNode.equalsIgnoreCase("true"))
						{
							if(relDirection.equalsIgnoreCase("from"))
								_util.connectBusObjects(_context, childObjectID, revisedMinorObjectID, childParentRelationshipType, true, attrNameVals,basicToSet);
							else
								_util.connectBusObjects(_context, childObjectID, revisedMinorObjectID, childParentRelationshipType, false, attrNameVals,basicToSet);
						}
						else
						{
							boolean isNodeOnFromSide		= false;
							boolean isNodeParent			= false;
							BusinessObject fromSideObject	= childParentRelationship.getFrom();
							fromSideObject.open(_context);

							if(fromSideObject.getObjectId().equals(childObjectID))
								isNodeOnFromSide = true;

							fromSideObject.close(_context);

							if(relDirection.equals("from") && !isNodeOnFromSide)
								isNodeParent = true;
							else if(relDirection.equals("to") && isNodeOnFromSide)
								isNodeParent = true;

							if(isNodeParent)
							{
								removedRelIds.add(childObjectRelationshipID);
								childParentRelationship.remove(_context);
							}

							_util.connectBusObjects(_context, childObjectID, revisedMinorObjectID, childParentRelationshipType, isNodeOnFromSide, attrNameVals, basicToSet);							
						}

						childObject.close(_context);
					}
				}
			}

			}
			else
			{
				objectIdRevisedBusObjMap.put(currentObjectId, revisedMinorObject);
			}
			revisedMinorObject.close(_context);

			cadObjectNode.setAttributeValue("id" , revisedMinorObjectID);
		}
		catch(Exception e)
		{
			String errorMessage = e.getMessage();
			errorMessageTable.addErrorMessage(_context, currentObjectId, errorMessage);
			errorMessageList.add(errorMessage);
		} finally{
			if(bGrantCreatorFromconnectAccess)
			{
				for(int k=0;k<slMajorIdWithAccess.size();k++){
					String tempOid = (String) slMajorIdWithAccess.get(k);
					_generalUtil.revokeAccessForRole(_context, tempOid,key);
				}
			}	
		}
	}

	protected void connectStructureWithoutRevision(Context _context, IEFXmlNode cadObjectNode) throws Exception
	{
		try
		{
			String minorObjectID			= cadObjectNode.getAttribute("id");

			Hashtable revReplaceTable		= new Hashtable();
			Hashtable childIdCadNodeTable	= getChildIdCadNodeTable(_context, cadObjectNode, revReplaceTable);

			Hashtable relsAndEnds			= new Hashtable();
			Hashtable assemblyRelsAndEnds	= _globalConfig.getRelationshipsOfClass(MCADAppletServletProtocol.ASSEMBLY_LIKE);
			Hashtable extRefRelsAndEnds		= _globalConfig.getRelationshipsOfClass(MCADServerSettings.EXTERNAL_REFERENCE_LIKE);

			relsAndEnds.putAll(assemblyRelsAndEnds);

			Enumeration assemblyLikeRels = relsAndEnds.keys();

			while(assemblyLikeRels.hasMoreElements())
			{
				String assemblyLikeRel = (String)assemblyLikeRels.nextElement();
				if(extRefRelsAndEnds != null && extRefRelsAndEnds.containsKey(assemblyLikeRel))
					relsAndEnds.remove(assemblyLikeRel);
			}

			Hashtable childIdFirstLevelRelIdsTable	= _generalUtil.getFirstLevelChildAndRelIds(_context, minorObjectID, true, relsAndEnds, null);

			Enumeration childCadObjectElements = childIdCadNodeTable.keys();
			while(childCadObjectElements.hasMoreElements())
			{
				String childObjectID		= (String)childCadObjectElements.nextElement();
				Vector cadObjectNodeList	= (Vector)childIdCadNodeTable.get(childObjectID);

				IEFXmlNode childObjectNode			= (IEFXmlNode)cadObjectNodeList.elementAt(0);
				String isChildSelected				= (String)childObjectNode.getAttribute("selected");
				String childObjectRelationshipID	= (String)childObjectNode.getAttribute("rid");
				String isVerticalViewNode			= (String)childObjectNode.getAttribute("isverticalviewnode");
				String childMajorBusObjectId		= (String)childObjectNode.getAttribute("majorobjectid");

				BusinessObject childMajorObject		= new BusinessObject(childMajorBusObjectId);
				childMajorObject.open(_context);

				boolean isChildSelectable = false;
				if((MCADAppletServletProtocol.TRUE).equalsIgnoreCase(isChildSelected) || _generalUtil.isBusObjectFinalized(_context, childMajorObject))
					isChildSelectable = true; //Operation should continue even if child is not selected but is finalized.

				childMajorObject.close(_context);

				if(isVerticalViewNode.equalsIgnoreCase("true") || !isChildSelectable)
					continue;

				if(childObjectRelationshipID != null && !childObjectRelationshipID.equals(""))
				{
					Relationship childParentRelationship = new Relationship(childObjectRelationshipID);
					childParentRelationship.open(_context);

					String childParentRelationshipType	= childParentRelationship.getTypeName();
					Hashtable attrNameVals			= _util.getRelationshipAttrNameValMap(_context, childParentRelationship);

					childParentRelationship.close(_context);
					String cadRelTypeName	= _globalConfig.getCADRelFromMxRel(childParentRelationshipType);
					String relDirection		= _globalConfig.getRelatinshipEndFromMapping(cadRelTypeName, childParentRelationshipType);

					if(!_util.doesRelationExist(_context, childObjectID, minorObjectID, childParentRelationshipType))
					{
						StringList relSelectList = new StringList();
						relSelectList.add(MCADAppletServletProtocol.STR_LOGICALID);
                        
						Hashtable basicsToset                         = _generalUtil.getSelectDataForRelation(_context, childObjectRelationshipID, relSelectList);
						
						disconnectExistingVersionRelationship(_context, minorObjectID, childObjectID, childIdFirstLevelRelIdsTable);

						for(int i=0;i<cadObjectNodeList.size();++i)
						{
							if(relDirection.equalsIgnoreCase("from"))
								_util.connectBusObjects(_context, childObjectID, minorObjectID, childParentRelationshipType, true, attrNameVals,basicsToset);
							else
								_util.connectBusObjects(_context, childObjectID, minorObjectID, childParentRelationshipType, false, attrNameVals,basicsToset);
						}
					}
				}
			}
		}
		catch(Exception exception)
		{
			exception.printStackTrace();
			MCADServerException.createException(exception.getMessage(), exception);
		}
	}

	protected void disconnectExistingVersionRelationship(Context _context, String parentMinorObjId, String nodeChildObjId, Hashtable childIdFirstLevelRelIdsTable) throws Exception
	{
		Enumeration connectedChildObjIds	= childIdFirstLevelRelIdsTable.keys();
		BusinessObject nodeChildObject		= new BusinessObject(nodeChildObjId);

		BusinessObject nodeChildMajorObject	= _util.getMajorObject(_context, nodeChildObject);
		if(nodeChildMajorObject == null)
			nodeChildMajorObject = nodeChildObject;

		nodeChildMajorObject.open(_context);

		String nodeChildMajorObjType = nodeChildMajorObject.getTypeName();
		String nodeChildMajorObjName = nodeChildMajorObject.getName();

		while(connectedChildObjIds.hasMoreElements())
		{
			String connectedChildId		= (String)connectedChildObjIds.nextElement();
			Vector childRelationships	= (Vector)childIdFirstLevelRelIdsTable.get(connectedChildId);

			for(int i=0; i<childRelationships.size();++i)
			{
				String childRelationshipId		= (String)childRelationships.elementAt(i);

				Relationship childRelationship	= new Relationship(childRelationshipId);
				childRelationship.open(_context);

				BusinessObject connectedChildObj	= childRelationship.getTo();
				connectedChildObj.open(_context);

				BusinessObject connectedChildMajorObj = _util.getMajorObject(_context, connectedChildObj);
				if(connectedChildMajorObj == null)
					connectedChildMajorObj = connectedChildObj;
				else
					connectedChildMajorObj.open(_context);

				String connectedChildMajorObjType = connectedChildMajorObj.getTypeName();
				String connectedChildMajorObjName = connectedChildMajorObj.getName();

				if(connectedChildMajorObjType.equals(nodeChildMajorObjType) && connectedChildMajorObjName.equals(nodeChildMajorObjName))
				{
					childRelationship.remove(_context);
				}

				connectedChildMajorObj.close(_context);
			}
		}
	}

	protected Hashtable getChildIdCadNodeTable(Context _context, IEFXmlNode cadObjectNode, Hashtable revReplaceTable) throws Exception
	{
		Hashtable childIdCadNodeTable = new Hashtable();

		Enumeration childCadObjectElements = cadObjectNode.elements();
		while(childCadObjectElements.hasMoreElements())
		{
			IEFXmlNode childObjectNode	= (IEFXmlNode)childCadObjectElements.nextElement();
			String childObjectID		= (String)childObjectNode.getAttribute("id");
			String isSelected			= (String)childObjectNode.getAttribute("selected");
			String childMajorObjectId	= (String)childObjectNode.getAttribute("majorobjectid");
			boolean isMajor				= childMajorObjectId.equals(childObjectID);

			if(!(MCADAppletServletProtocol.TRUE).equalsIgnoreCase(isSelected))
			{
				BusinessObject childMajorObj = new BusinessObject(childMajorObjectId);
				childMajorObj.open(_context);

				if(_generalUtil.isBusObjectFinalized(_context, childMajorObj))
				{
					childObjectID = childMajorObjectId;
					isMajor = true;
				}

				childMajorObj.close(_context);
			}

			// If shared part is found, append the childIdCadNodesList with the cad node again.
			// This will be done for each shared part.
			if(childIdCadNodeTable.containsKey(childObjectID))
			{
				Vector childIdCadNodesList = (Vector)childIdCadNodeTable.get(childObjectID);
				childIdCadNodesList.addElement(childObjectNode);
				childIdCadNodeTable.put(childObjectID, childIdCadNodesList);
			}
			else
			{
				Vector childIdCadNodesList = new Vector();
				childIdCadNodesList.addElement(childObjectNode);
				childIdCadNodeTable.put(childObjectID, childIdCadNodesList);
			}

			Vector boIdList = _util.getRevisionBoIdsOfAllStreams(_context, childObjectID, !isMajor);
			for(int i=0; i<boIdList.size(); i++)
			{
				revReplaceTable.put(boIdList.elementAt(i), childObjectID);
			}
		}
		return childIdCadNodeTable;
	}

	protected BusinessObject reviseVersionedTypeObject(Context _context, String  minorObjectID, Hashtable revReplaceTable,Hashtable virtualStructureCreatedforNodes, Hashtable objIDNodeListTable) throws Exception
	{
		BusinessObject revisedMinor = null;
		String cadType				= _util.getCADTypeForBO(_context, new BusinessObject(minorObjectID));

		if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
		{
			Hashtable oldIdRevisedObjTable	= new Hashtable();
			String instanceId				= minorObjectID;
			String familyId					= _generalUtil.getTopLevelFamilyObjectForInstance(_context, instanceId);
			BusinessObject familyObj		= new BusinessObject(familyId);

			familyObj.open(_context);

			BusinessObject revisedFamily =  null;
			if(virtualStructureCreatedforNodes.containsKey(familyId))
			{
				revisedFamily = new BusinessObject(virtualStructureCreatedforNodes.get(familyId).toString());
			}
			else if(finalizationCandidates.contains(familyId))
			{
				revisedFamily = familyObj;
			}
			else
			{
				revisedFamily = reviseWithLatestMinor(_context, familyId);
				virtualStructureCreatedforNodes.put(familyId, revisedFamily.getObjectId());
				finalizationCandidates.add(revisedFamily.getObjectId());
			}


			reviseFamilyTower(_context, familyObj, revisedFamily, instanceId, oldIdRevisedObjTable, revReplaceTable);

			familyObj.close(_context);

			revisedMinor = (BusinessObject) oldIdRevisedObjTable.get(instanceId);
			revisedInstanceFamilyIdTable.put(revisedMinor.getObjectId(),familyId);

		}
		else
		{
			revisedMinor = reviseWithLatestMinor(_context, minorObjectID);
		}

		return revisedMinor;
	}

	private BusinessObject reviseWithLatestMinor(Context _context, String inputMinorId) throws Exception
	{
		String latestMinorObjectID		 = _util.getLatestRevisionID(_context, inputMinorId);

		BusinessObject latestMinorObject = new BusinessObject(latestMinorObjectID);

		latestMinorObject.open(_context);

		BusinessObject inputMinorObject = new BusinessObject(inputMinorId);

		inputMinorObject.open(_context);

		String fileMsgDigestAttrName = MCADMxUtil.getActualNameForAEFData(_context, "attribute_IEF-FileMessageDigest");
		String fileMsgDigestAttrVal  = _util.getAttributeForBO(_context, inputMinorObject,fileMsgDigestAttrName);	

		inputMinorObject.close(_context);

		BusinessObject revisedMinor		 = _util.reviseBusinessObject(_context, latestMinorObject, "", false, false);

		_util.setAttributeValue(_context, revisedMinor, fileMsgDigestAttrName, fileMsgDigestAttrVal);

		latestMinorObject.close(_context);

		_util.copyFilesFcsSupported(_context,mcsURL,inputMinorObject, revisedMinor);

		return revisedMinor;
	}

	protected void reviseFamilyTower(Context _context, BusinessObject familyObj, BusinessObject revisedFamily, String activeInstanceId, Hashtable oldIdRevisedObjTable, Hashtable revReplaceTable) throws Exception
	{
		Vector instanceList = _generalUtil.getInstanceListForFamilyObject(_context, familyObj.getObjectId(_context));
		
		//in case of second instance of family gets revised both input family-object will be same IR-152981V6R2013
		boolean isBothFamilySame = familyObj.getObjectId().equals(revisedFamily.getObjectId());
		
		for(int i=0; i<instanceList.size(); i++)
		{
			BusinessObject instObj = (BusinessObject) instanceList.elementAt(i);
			BusinessObject revisedInst	= instObj;

			instObj.open(_context);

			String instObjId = instObj.getObjectId();

			if(activeInstanceId.equals(instObjId))
			{
				revisedInst	= reviseWithLatestMinor(_context, instObjId);

				_generalUtil.connectFamilyClassObjects(_context, revisedFamily, revisedInst);

				if(isBothFamilySame)
				{
					String instanceOfRelName 	                = MCADMxUtil.getActualNameForAEFData(_context, "relationship_InstanceOf");
					_util.disconnectBusObjects(_context, revisedFamily.getObjectId(),instObjId, instanceOfRelName, true);
				}

				oldIdRevisedObjTable.put(instObjId, revisedInst);
			}
			else if(!isBothFamilySame)
			{
				_generalUtil.connectFamilyClassObjects(_context, revisedFamily, instObj);
				connectInstances(_context, null, instObj, instObjId, revReplaceTable);
			}

			instObj.close(_context);
		}
	}

	protected void connectInstances(Context _context, BusinessObject revisedInst, BusinessObject instObj, String instObjId, Hashtable revReplaceTable) throws Exception
	{
		Hashtable relationsList = _generalUtil.getAllWheareUsedRelationships(_context, instObj, true, MCADServerSettings.ASSEMBLY_LIKE);
		Enumeration allRels = relationsList.keys();

		Set disconnectedRelsInfo = new HashSet();

		while(allRels.hasMoreElements())
		{
			boolean isDisconnected = false;

			Relationship rel = (Relationship)allRels.nextElement();
			String end = (String)relationsList.get(rel);

			rel.open(_context);

			String relName = rel.getTypeName();

			if(_globalConfig.isRelationshipOfClass(relName, MCADServerSettings.EXTERNAL_REFERENCE_LIKE))
				continue;

			BusinessObject childBus = null;
			boolean bIsFrom = end.equals("from");

			if (bIsFrom)
				childBus = rel.getTo();
			else
				childBus = rel.getFrom();

			String childBusId = childBus.getObjectId(_context);

			StringBuffer relValidationInfo = new StringBuffer();
			relValidationInfo.append(instObjId);
			relValidationInfo.append("|");
			relValidationInfo.append(childBusId);
			relValidationInfo.append("|");
			relValidationInfo.append(relName);

			if(disconnectedRelsInfo.contains(relValidationInfo.toString()))
				continue;

			Hashtable attrNameVals	= new Hashtable();
			AttributeList attrList = rel.getAttributes(_context);
			AttributeItr attrItr = new AttributeItr(attrList);

			while (attrItr.next())
			{
				Attribute attr = attrItr.obj();
				attrNameVals.put(attr.getName(), attr.getValue());
			}

			rel.close(_context);

			if(revReplaceTable.containsKey(childBusId))
			{
				String childID = (String)revReplaceTable.get(childBusId);

				if (revisedInst == null && !childID.equals(instObjId))
				{
					_util.disconnectBusObjects(_context, instObjId, childBusId, relName, bIsFrom);

					isDisconnected = true;

					StringBuffer disconnectedRelInfo = new StringBuffer();
					disconnectedRelInfo.append(instObjId);
					disconnectedRelInfo.append("|");
					disconnectedRelInfo.append(childBusId);
					disconnectedRelInfo.append("|");
					disconnectedRelInfo.append(relName);

					disconnectedRelsInfo.add(disconnectedRelInfo.toString());
				}

				childBus = new BusinessObject(childID);
				childBus.open(_context);
			}

			if(revisedInst == null)
			{
				if(!isDisconnected)
					continue;

				// required for inactive instance virtual structure promotion. -> inactive instance connected to non-finalized minor
				BusinessObject majorObject = _util.getMajorObject(_context, childBus);

				if(majorObject != null)
				{
					majorObject.open(_context);

					if(_generalUtil.isBusObjectFinalized(_context, majorObject))
					{
						childBus.close(_context);
						childBus = majorObject;
					}
					else
					{
						majorObject.close(_context);
					}
				}

				_util.connectBusObjects(_context, instObj, childBus, relName, bIsFrom, attrNameVals);
			}
			else
				_util.connectBusObjects(_context, revisedInst, childBus, relName, bIsFrom, attrNameVals);

			childBus.close(_context);
		}
	}
*/


	protected Hashtable getSignatureDetails(Context _context, IEFXmlNode cadObjectNode) throws Exception
	{
		BusinessObject majorObject		= null;
		Hashtable signatureActionTable	= new Hashtable();
		String majorBusObjectId			= (String)cadObjectNode.getAttribute("majorobjectid");

		if(objIdSignatureDetailsTable.get(majorBusObjectId) != null)
		{
			signatureActionTable = (Hashtable)objIdSignatureDetailsTable.get(majorBusObjectId);
		}
		else
		{		
			// Coming from component finlaization page majorobjectid will be null
			if(majorBusObjectId == null || majorBusObjectId.trim().equals(""))
			{
				String objectID				= (String)cadObjectNode.getAttribute("id");
				BusinessObject busObject	= new BusinessObject(objectID);
				busObject.open(_context);
				majorObject	= _util.getMajorObject(_context, busObject);
				busObject.close(_context);
			}
			else
			{
				majorObject = new BusinessObject(majorBusObjectId);
			}
			majorObject.open(_context);
			State currentState = _util.getCurrentState(_context, majorObject);
			String targetState = (String)cadObjectNode.getAttribute("targetstate");
			SignatureList signatureList = majorObject.getSignatures(_context, currentState.getName(), targetState);
			SignatureItr signatureitr = new SignatureItr(signatureList);

			while(signatureitr.next())
			{
				Signature busSignature = signatureitr.obj();
				if(busSignature.isSigned())
				{
					String signatureAction = "";

					String busSignatureName = busSignature.getName();

					if(busSignature.isApproved())
						signatureAction = "approve";
					else if(busSignature.isRejected())
						signatureAction = "reject";
					else if(busSignature.isIgnored())
						signatureAction = "ignore";

					signatureActionTable.put(busSignatureName , signatureAction);
					objIdSignatureDetailsTable.put(majorBusObjectId, signatureActionTable);
				}
			}
			majorObject.close(_context);
		}

		return signatureActionTable;
	}

	protected void applySignaturetoChild(Context _context, String childMajorObjectId, String childCurrentState, Hashtable parentSignNameSignActionTable, DSCServerErrorMessageTable errorMessageTable) throws Exception
	{
		if(parentSignNameSignActionTable.size()>0)
		{
			String[] objId 						= {childMajorObjectId};
			HashMap childSelectExprSignNameMap 	= new HashMap();

			StringList busSelectionList = getChildSignStatusBusSelects(parentSignNameSignActionTable, childSelectExprSignNameMap, childCurrentState);
			if(!busSelectionList.isEmpty())
			{
				BusinessObjectWithSelectList busWithSelectionList 	= BusinessObject.getSelectBusinessObjectData(_context, objId, busSelectionList);
				BusinessObjectWithSelect busSelection 				= busWithSelectionList.getElement(0);

				for(int i=0; i<busSelectionList.size(); i++)
				{
					String signKey 		= (String)busSelectionList.get(i);
					String signStatus 	= busSelection.getSelectData(signKey);

					//Process the action only if the action is not already performed on the child. Output false denotes the action is not performed
					if(signStatus != null && signStatus.trim().equalsIgnoreCase("false"))
					{
						String   signatureName   =  (String)childSelectExprSignNameMap.get(signKey);
						String   signatureAction =  (String)parentSignNameSignActionTable.get(signatureName);

						//String strMqlCmd		 	=  signatureAction + " bus " + childMajorObjectId + " signature \"" + signatureName + "\"";
						String Args[] = new String[4];
						Args[0] = signatureAction;
						Args[1] = childMajorObjectId;
						Args[2] = "signature";
						Args[3] = signatureName;
						String result 				= _util.executeMQL(_context, "$1 bus $2 $3 $4", Args);
						if(result.startsWith("false"))
						{
							result = result.substring(6);

							Hashtable errorTokens = new Hashtable();
							errorTokens.put("SIGNATURE", signatureName);

							String errorMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.SignatureFailed", errorTokens) + result;
							errorMessageTable.addErrorMessage(_context, childMajorObjectId, errorMessage);
							errorMessageList.add(errorMessage);
						}
					}
				}
			}
		}
	}

	private StringList getChildSignStatusBusSelects(Hashtable parentSignNameSignActionTable, HashMap childSelectExprSignNameMap, String childCurrentState)
	{
		StringList busSelectionList = new StringList();

		Enumeration parentSignatureNamesList = parentSignNameSignActionTable.keys();

		while(parentSignatureNamesList.hasMoreElements())
		{
			String parentSignName 	= (String)parentSignatureNamesList.nextElement();
			String parentSignAction = (String)parentSignNameSignActionTable.get(parentSignName);

			String childSignActionSelect = "";

			if(null != parentSignAction)
			{
				if(parentSignAction.equals("approve"))
				{
					childSignActionSelect = "approved";
				}
				else if(parentSignAction.equals("reject"))
				{
					childSignActionSelect = "rejected";
				}
				else if(parentSignAction.equals("ignore"))
				{
					childSignActionSelect = "ignored";
				}

				StringBuffer childSignatureStatusSelectExpr = new StringBuffer("state[");
				childSignatureStatusSelectExpr.append(childCurrentState);
				childSignatureStatusSelectExpr.append("].signature[");
				childSignatureStatusSelectExpr.append(parentSignName);
				childSignatureStatusSelectExpr.append("].");
				childSignatureStatusSelectExpr.append(childSignActionSelect);

				busSelectionList.add(childSignatureStatusSelectExpr.toString());
				childSelectExprSignNameMap.put(childSignatureStatusSelectExpr.toString(), parentSignName);
			}
		}

		return busSelectionList;
	}

	public void addSelectedObjectToResponse(String busid)
	{
		Hashtable cadNodeContentsTable = new Hashtable();
		IEFXmlNodeImpl responseCadObjectNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		responseCadObjectNode.setName("cadobject");

		cadNodeContentsTable.put("busid", busid);
		
		responseCadObjectNode.setAttributes(cadNodeContentsTable);
		responseCadObjectList.addNode(responseCadObjectNode);

	}
}



