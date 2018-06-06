/*
 **  MCADUndoFinalizeBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to undo the finalization of an MCAD model object.
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeItr;
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
import matrix.db.ExpansionWithSelect;
import matrix.db.Relationship;
import matrix.db.RelationshipItr;
import matrix.db.RelationshipWithSelect;
import matrix.db.RelationshipWithSelectList;
import matrix.db.Visuals;
import matrix.util.MatrixException;
import matrix.util.StringList;
import java.util.Iterator;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFBaselineHelper;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNodeImpl;
import com.matrixone.apps.domain.util.PropertyUtil;

public class MCADUndoFinalizeBase_mxJPO extends IEFCommonUIActions_mxJPO
{
	protected final boolean COPY_BLANK_ATTR_DURING_UNDO_FINALIZE	= false;

	//actual name for "Source" attribute
	protected String _attrSource	= "";
	protected String _sourceName	= "";
	
	protected IEFXmlNodeImpl responseCadObjectList;
	
	protected List errorMessageList = new ArrayList();
	
	public  MCADUndoFinalizeBase_mxJPO  () {

	}
	public MCADUndoFinalizeBase_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("not supported no desktop client", null);

	}
	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	// Business Logic for implementing
	protected void canPerformOperationCustom(Context _context, Hashtable resultDataTable) throws MCADException
	{
		try
		{
			if (_busObject == null)
			{
				MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.BusinessObjectNotFound"), null);
			}

			_busObject.open(_context);
			if(!_util.isRoleAssigned(_context,"role_VPLMAdmin"))
			{
				if(_util.isMajorObject(_context, _busObjectID))//_globalConfig.isMajorType(_busObject.getTypeName()) ) //[NDM] OP6
				{
					boolean bFinalized = _generalUtil.isBusObjectFinalized(_context, _busObject);
					if (!bFinalized)
					{
						MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.BusinessObjectNotFinalized"), null);
					}
					else if (!_generalUtil.checkLockStatus(_context, _busObject))
					{
						Hashtable exceptionDetails = new Hashtable(4);
						exceptionDetails.put("TYPE",_busObject.getTypeName());
						exceptionDetails.put("NAME",_busObject.getName());
						exceptionDetails.put("REVISION",_busObject.getRevision());
						exceptionDetails.put("LOCKER",_busObject.getLocker(_context).getName());
						MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FinalizedBusObjLocked",exceptionDetails), null);
					}
				}
				else
				{
					MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.UndoFinalizeNotValidForMinorTypes"), null);
				}
			}
			_busObject.close(_context);
		}
		catch(Exception e)
		{
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToUndoFinalize") + e.getMessage(), e);
		}
	}


	// Entry point
	public void executeCustom(Context _context, Hashtable resultAndStatusTable)  throws MCADException
	{
		_attrSource = MCADMxUtil.getActualNameForAEFData(_context, "attribute_Source");
		String Args[] = new String[3];
		Args[0] = "global";
		Args[1] = "IsDesignCenterCommand";
		Args[2] = "true";
		_util.executeMQL(_context,"set env $1 $2 $3", Args);

		responseCadObjectList = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		responseCadObjectList.setName("cadobjectlist");
		
		try
		{
			String cadType	= _util.getCADTypeForBO(_context,_busObject);
			BusinessObject famObj		= null;

			if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE) || _globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
			{
				famObj = _busObject;
				if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
				{
					BusinessObject instObj = _busObject;
					if(!_generalUtil.isBusObjectFinalized(_context, instObj))
						instObj = _util.getActiveMinor(_context,_busObject);
							
					famObj = _generalUtil.getFamilyObjectForInstance(_context, instObj);
				}
			
			}

			if(isInFinalizationState(_context, _busObject))
			{
				StringBuffer majorMinorObjIdString = new StringBuffer();
				String majorMinorIds = "";

				if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
				{
					String famObjId				= _busObject.getObjectId(_context);
					ArrayList instObjectIdList  = new ArrayList();
					
					String [] oids		= new String[1];
					oids[0]				= famObjId;
					instObjectIdList	= _generalUtil.getFamilyStructureRecursively(_context, oids, new Hashtable(),null);
					
							lockCheck(_context,famObjId);
							Iterator<String> itr = instObjectIdList.iterator();
							while(itr.hasNext())
							{
								lockCheck(_context,itr.next());
							}
					majorMinorIds = undoFinalize(_context, _busObject, "");
					majorMinorObjIdString.append(majorMinorIds);
					majorMinorObjIdString.append("@");

					//added for managing relationships created in 10.7.2
					BusinessObject activeMinorFamObj = _util.getActiveMinor(_context, _busObject);
					// Move all FAMILY_LIKE relationship from Major to ActiveMinor
					
					// Start [NDM]- NE4:-
					//_generalUtil.disconnectRelationshipsForObject(_context,activeMinorFamObj, true, MCADServerSettings.FAMILY_LIKE);
					//_generalUtil.shuffleRelatinships(_context, _busObject, activeMinorFamObj, true, false, MCADServerSettings.FAMILY_LIKE);
					
					//_generalUtil.disconnectRelationshipsForObject(_context,_busObject, true, MCADServerSettings.FAMILY_LIKE);

					// End [NDM]- NE4

					String famCurrentState = _util.getCurrentState(_context, _busObject).getName();
					
					demoteConnectedInstances(_context, instObjectIdList, famCurrentState, famObjId, "", majorMinorObjIdString);
				}
				else if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
				{
					famObj.open(_context);
					boolean isFamilyInFinalizationState = _generalUtil.isBusObjectFinalized(_context, famObj);
					String famObjId				= famObj.getObjectId(_context);
					
					String [] oids = new String[1];
					oids[0] = famObjId;
					ArrayList instObjectIdList = _generalUtil.getFamilyStructureRecursively(_context, oids, new Hashtable(),null);
					
							lockCheck(_context,famObjId);
							Iterator<String> itr = instObjectIdList.iterator();
							while(itr.hasNext())
							{
								lockCheck(_context,itr.next());
							}
	
							
					if(isFamilyInFinalizationState)
					{
					majorMinorIds = undoFinalize(_context, famObj, "");
					majorMinorObjIdString.append(majorMinorIds);
					majorMinorObjIdString.append("@");

						BusinessObject activeMinorFamObj = _util.getActiveMinor(_context, famObj);
						// Move all FAMILY_LIKE relationship from Major to ActiveMinor
					
						// Start [NDM]- NE4:-

						//_generalUtil.disconnectRelationshipsForObject(_context, activeMinorFamObj, true, MCADServerSettings.FAMILY_LIKE);
						//_generalUtil.shuffleRelatinships(_context, famObj, activeMinorFamObj, true, false, MCADServerSettings.FAMILY_LIKE);
		
			            //_generalUtil.disconnectRelationshipsForObject(_context,famObj, true, MCADServerSettings.FAMILY_LIKE);
					}
					BusinessObject famMajorObj = famObj;
					if(!_util.isMajorObject(_context, famObjId))//!_globalConfig.isMajorType(famObj.getTypeName())) //[NDM] OP6
							famMajorObj = _util.getMajorObject(_context, famObj);

					String famCurrentState = _util.getCurrentState(_context, famMajorObj).getName();
					
					demoteConnectedInstances(_context, instObjectIdList, famCurrentState, famObjId, _busObject.getObjectId(_context), majorMinorObjIdString);
				}
				else
				{
					majorMinorIds = undoFinalize(_context, _busObject, "");
					majorMinorObjIdString.append(majorMinorIds);
					majorMinorObjIdString.append("@");
				}

				// [NDM]
				//doFileOperations(_context, majorMinorObjIdString.toString());
				resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT, majorMinorObjIdString.toString());
			}
			else
			{

				if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE) || _globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
				{
					
					if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
					{
						String famObjId				= famObj.getObjectId(_context);

						String [] oids = new String[1];
						oids[0] = famObjId;

						ArrayList instObjectIdList = _generalUtil.getFamilyStructureRecursively(_context, oids, new Hashtable(),null);
						BusinessObject famMajorObj = famObj;
						if(!_util.isMajorObject(_context, famObjId))//!_globalConfig.isMajorType(famObj.getTypeName()))  //[NDM] OP6
							famMajorObj = _util.getMajorObject(_context, famObj);

						String instStateBeforeDemote = _util.getCurrentState(_context, _busObject).getName();
						String famStateBeforeDemote = _util.getCurrentState(_context, famMajorObj).getName();
						demoteBusObject(_context, _busObject);
						if(instStateBeforeDemote.equalsIgnoreCase(famStateBeforeDemote))
							demoteBusObject(_context, famMajorObj);

						String famCurrentState = _util.getCurrentState(_context, famMajorObj).getName();

						// Copy all FAMILY_LIKE relationship from Major to ActiveMinor for data created in or prior to V6R2009x
						if(_generalUtil.isBusObjectFinalized(_context, famObj))
						{
							BusinessObject activeMinorFamObj = _util.getActiveMinor(_context, famMajorObj);
						// [NDM]
						//	_generalUtil.disconnectRelationshipsForObject(_context, activeMinorFamObj, true, MCADServerSettings.FAMILY_LIKE);
						//	_generalUtil.shuffleRelatinships(_context, famMajorObj, activeMinorFamObj, true, true, MCADServerSettings.FAMILY_LIKE);
						}

						demoteConnectedInstances(_context, instObjectIdList, famCurrentState, famObjId, _busObject.getObjectId(_context), new StringBuffer());
					}
					else
					{
						boolean isConnFamObjFinalized = _generalUtil.isBusObjectFinalized(_context, famObj);
						if(!isConnFamObjFinalized)
							famObj = _util.getActiveMinor(_context,_busObject);
						String famObjId				= famObj.getObjectId(_context);

						String [] oids = new String[1];
						oids[0] = famObjId;

						ArrayList instObjectIdList = _generalUtil.getFamilyStructureRecursively(_context, oids, new Hashtable(),null);
						demoteBusObject(_context, _busObject);

						String famCurrentState = _util.getCurrentState(_context, _busObject).getName();

						// Copy all FAMILY_LIKE relationship from Major to ActiveMinor for data created in or prior to V6R2009x
						if(isConnFamObjFinalized)
						{
							BusinessObject activeMinorFamObj = _util.getActiveMinor(_context, _busObject);
						// [NDM]
						//	_generalUtil.disconnectRelationshipsForObject(_context, activeMinorFamObj, true, MCADServerSettings.FAMILY_LIKE);
						//	_generalUtil.shuffleRelatinships(_context, _busObject, activeMinorFamObj, true, true, MCADServerSettings.FAMILY_LIKE);
						}

						demoteConnectedInstances(_context, instObjectIdList, famCurrentState, famObjId, "", new StringBuffer());

					}
					
				}
				else
				{
					demoteBusObject(_context, _busObject);
				}
			}
		}
		catch(Exception e)
		{
			Args = new String[2];
			Args[0] = "global";
			Args[1] = "IsDesignCenterCommand";
			
			_util.executeMQL(_context,"unset env $1 $2", Args);
			errorMessageList.add(e.getMessage());
			resultAndStatusTable.put(MCADServerSettings.JPO_FAILED_MESSAGES,errorMessageList);
			MCADServerException.createException(e.getMessage(), e);
			
		}

		Args = new String[2];
		Args[0] = "global";
		Args[1] = "IsDesignCenterCommand";
		_util.executeMQL(_context,"unset env $1 $2", Args);
		resultAndStatusTable.put(MCADServerSettings.JPO_SELECTED_OBJECTS_LIST, responseCadObjectList);
	}

	private void demoteFinalizedInstance(Context context, BusinessObject famObj, BusinessObject majorInstObject, StringBuffer majorMinorObjIdString, StringList familyIds) throws Exception
	{
		String minorFamilyObjectId = "";

		//if(_globalConfig.isMajorType(famObj.getTypeName()))

		// start [NDM]:- NE4
		if(_util.isMajorObject(context,famObj.getObjectId()))
		{
		//End [NDM] :- NE4
			BusinessObject minorFamilyObject	= _util.getActiveMinor(context,famObj);
			minorFamilyObjectId					= minorFamilyObject.getObjectId(context);
		}
		else
			minorFamilyObjectId = famObj.getObjectId();

		String minorObjId = "";
		
		String instMajorMinorIds = undoFinalize(context, majorInstObject, minorFamilyObjectId);
		majorMinorObjIdString.append(instMajorMinorIds);
		majorMinorObjIdString.append("@");

		StringTokenizer toks = new StringTokenizer(instMajorMinorIds, "|");
		while(toks.hasMoreTokens())
			minorObjId = toks.nextToken();

		BusinessObject minorBus = new BusinessObject(minorObjId);
		
		// start [NDM]:- NE4

		/*for(int k=0; k <familyIds.size(); k++)
		{
			BusinessObject famObjToConnWithInst = new BusinessObject((String)familyIds.get(k));
			_generalUtil.connectFamilyClassObjects(context,famObjToConnWithInst, minorBus);

		}*/
		
		//_generalUtil.disconnectRelationshipsForObject(context,majorInstObject, false, MCADServerSettings.FAMILY_LIKE);
		// end [NDM]:- NE4
	}

	private void demoteConnectedInstances(Context context, Collection busIDs, String targetState, 
			String demotedFamId, String demotedInstId, StringBuffer majorMinorObjIdString) throws MCADException
    {
    	String name	= "";
		String id	= "";
        String type = "";

        try
	{
           	Hashtable relsAndEnds               = _globalConfig.getRelationshipsOfClass(MCADServerSettings.FAMILY_LIKE);
			Hashtable relNameBasicSelect        = new Hashtable(relsAndEnds.size());
			String REL_VERSION_OF			 	= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
			Enumeration allRels                 = relsAndEnds.keys();
			while(allRels.hasMoreElements()) 
		{
				String relName  = (String)allRels.nextElement();
				String end      = (String)relsAndEnds.get(relName);

				StringBuffer relSelect = new StringBuffer();
				relSelect.append(end);
				relSelect.append("[");
				relSelect.append(relName);
				relSelect.append("].");
				
				relNameBasicSelect.put(relName, relSelect.toString());
			}
		   
		    // start [NDM]:- NE4
			//String FAMILY_STATE					= "";
			String FAMILY_STATE_Major					= "";
			String FAMILY_STATE_Minor					= "";
			//End [NDM]:- NE4

			String FAMILY_BUS_ID				= "";
			String MAJOR_BUS_ID					= "";
			String MAJOR_BUS_STATE		= "";
			String MAJOR_BUS_STATE_LIST = "";
			
			Enumeration relNamesBasicSelectList = relNameBasicSelect.keys();
		   
			while(relNamesBasicSelectList.hasMoreElements()) 
			{
				String relName      = (String)relNamesBasicSelectList.nextElement();
				String basicSelect  = (String)relNameBasicSelect.get(relName);
				String end          = (String)relsAndEnds.get(relName);
				
				String familyEnd = "from";
				if(end.equals("from"))
					familyEnd = "to";
				
				StringBuffer familyBuffer = new StringBuffer(basicSelect);
				familyBuffer.append(familyEnd);
				familyBuffer.append(".");
				// [NDM]
				FAMILY_STATE_Major = familyBuffer.toString();
				FAMILY_STATE_Major = FAMILY_STATE_Major + "current";
				
				StringBuffer familyIdBuffer = new StringBuffer(familyBuffer.toString());
				familyIdBuffer.append("id");
				FAMILY_BUS_ID       = familyIdBuffer.toString();

				StringBuffer familyMajorBuffer = new StringBuffer(familyBuffer.toString());
				familyMajorBuffer.append("from[");
				familyMajorBuffer.append(REL_VERSION_OF);
				
				StringBuffer familyStateBuffer = new StringBuffer(familyMajorBuffer.toString());
				familyStateBuffer.append("].to.current");
				FAMILY_STATE_Minor       = familyStateBuffer.toString(); // [NDM]
		}

			StringBuffer majorBuffer = new StringBuffer();
			majorBuffer.append("from[");
			majorBuffer.append(REL_VERSION_OF);
			
			StringBuffer majorIdBuffer = new StringBuffer(majorBuffer.toString());
			majorIdBuffer.append("].to.id");
			MAJOR_BUS_ID       = majorIdBuffer.toString();

			StringBuffer majorIdStateBuffer = new StringBuffer(majorBuffer.toString());
			majorIdStateBuffer.append("].to.current");
			MAJOR_BUS_STATE       = majorIdStateBuffer.toString();

			StringBuffer majorIdStateListBuffer = new StringBuffer(majorBuffer.toString());
			majorIdStateListBuffer.append("].to.state");
			MAJOR_BUS_STATE_LIST       = majorIdStateListBuffer.toString();
			
			String []  oids = new String[busIDs.size()];
			busIDs.toArray(oids);
			
			StringList busSelects = new StringList();
			busSelects.add("id");
			busSelects.add("name");
			busSelects.add("current");
			busSelects.add("type");
			busSelects.add("state");

			// start [NDM]:- NE4
			// busSelects.add(FAMILY_STATE);
			busSelects.add(FAMILY_STATE_Minor);
			busSelects.add(FAMILY_STATE_Major);
			// End [NDM]:- NE4

			busSelects.add(FAMILY_BUS_ID);
			busSelects.add(MAJOR_BUS_ID);
			busSelects.add(MAJOR_BUS_STATE);
			busSelects.add(MAJOR_BUS_STATE_LIST);
			
			BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, oids, busSelects);
			for(int i = 0; i < busWithSelectList.size(); i++)
			{
				BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(i);
				name                                   = busWithSelect.getSelectData("name");
				id									   = busWithSelect.getSelectData("id");
				type								   = busWithSelect.getSelectData("type");		
				
				//start [NDM] :- NE4
				//StringList familyStates                = busWithSelect.getSelectDataList(FAMILY_STATE);
				StringList familyStates = null;

				// [NDM]:- NE4:- assumption is familyIds contain single id of family all the time
				StringList familyIds                   = busWithSelect.getSelectDataList(FAMILY_BUS_ID);
				String familyId = (String) familyIds.get(0);
						boolean isMajor = _util.isMajorObject(context,familyId);

				if(true)
					familyStates                = busWithSelect.getSelectDataList(FAMILY_STATE_Major);
				else
					familyStates                = busWithSelect.getSelectDataList(FAMILY_STATE_Minor);
				//End [NDM] :- NE4				
				
				boolean demoteInst					   = true;
					
				BusinessObject instanceObject		   = new BusinessObject(id);
				BusinessObject majorInstObject		   = instanceObject;
				instanceObject.open(context);
				
				String instCurrentState = busWithSelect.getSelectData("current");
				StringList stateList 	= busWithSelect.getSelectDataList("state");
				
				if(!_util.isMajorObject(context, id))//!_globalConfig.isMajorType(type)) // [NDM] OP6
				{
					majorInstObject = new BusinessObject(busWithSelect.getSelectData(MAJOR_BUS_ID));
					stateList 		 = busWithSelect.getSelectDataList(MAJOR_BUS_STATE_LIST);
					instCurrentState = busWithSelect.getSelectData(MAJOR_BUS_STATE);
				}
				
				String targetStateTodemoteInst = targetState;
				for(int j = 0; j <familyIds.size(); j++)
				{
					String connFamId		   = (String)familyIds.get(j);
					
					for(int k=0; k <familyStates.size(); k++)
					{	
						String stateOfConnectedFam = (String)familyStates.get(k);
						instanceObject.close(context);
						
						if(demotedInstId.equals(majorInstObject.getObjectId(context)) && !connFamId.equals(demotedFamId) && stateList.indexOf(stateOfConnectedFam) >= stateList.indexOf(instCurrentState))
						{
							BusinessObject famObj = new BusinessObject(connFamId);
							BusinessObject connFamMajorObj	= _util.getMajorObject(context, famObj);
							if(null != connFamMajorObj)
								famObj	= connFamMajorObj;
							if(!famObj.getObjectId(context).equals(demotedFamId))
							{
								if(!targetState.equals((String)stateList.firstElement()))
								{
									famObj	= new BusinessObject(demotedFamId);
									BusinessObject demotedFamMajorObj	= _util.getMajorObject(context, famObj);
									if(null != demotedFamMajorObj)
										famObj	= demotedFamMajorObj;									
								}
								String stateOfFam = _util.getCurrentState(context, famObj).getName();

								if(!stateOfFam.equals((String)stateList.firstElement()))
								{
									Hashtable tokensTable = new Hashtable(2);
									famObj.open(context);
									tokensTable.put("NAME",famObj.getName() );
									tokensTable.put("TYPE",famObj.getTypeName());
									tokensTable.put("REVISION",famObj.getRevision());
									famObj.close(context);
									
									String msg = _serverResourceBundle.getString("mcadIntegration.Server.Message.InstanceDemoteOperationFailed",tokensTable);
									MCADServerException.createException(msg, null);
								}
								
							}
						}
						
						if(stateList.indexOf(stateOfConnectedFam) > stateList.indexOf(targetState))
						{
							targetStateTodemoteInst = stateOfConnectedFam;
						}

						if((stateList.indexOf(stateOfConnectedFam) >= stateList.indexOf(instCurrentState)
								&& stateList.indexOf(targetStateTodemoteInst) < stateList.indexOf(instCurrentState)
								&& !connFamId.equals(demotedFamId)) || instCurrentState.equals((String)stateList.firstElement()))
						{
							demoteInst = false;
							break;
						}
					}
				}
				
				if(demoteInst)
				{
					while (!targetStateTodemoteInst.equalsIgnoreCase(instCurrentState) && !_util.isInitialState(context, _globalConfig, majorInstObject.getObjectId()))
					{
						if(isInFinalizationState(context, majorInstObject))
						{
							BusinessObject famObj = _generalUtil.getFamilyObjectForInstance(context, majorInstObject);
							famObj.open(context);
							
							demoteFinalizedInstance(context, famObj, majorInstObject, majorMinorObjIdString, familyIds);

							famObj.close(context);
						}
						else
						{
							demoteBusObject(context, majorInstObject);
						}
						
						instCurrentState = _util.getCurrentState(context, majorInstObject).getName();
					}
				}
			}
           		
		}
		catch(Exception exception)
        {
			MCADServerException.createException(exception.getMessage(), exception);
        }
		
	}

	/*
        Undoes the finalization process.
        Steps:
            1. Files will be moved back to the versioned object.
            2. "Finalized" relationship will be disconnected.
            3. All "whereused" cases of major object will be rplaced by minor object.
	 */
	protected String undoFinalize(Context _context, BusinessObject majorObject, String familyMinorObjId) throws MCADException, MatrixException
	{
		StringBuffer retStr = new StringBuffer();
		String sVersionOfRelName 	= MCADMxUtil.getActualNameForAEFData(_context, "relationship_VersionOf");
		try
		{
			// start [NDM] :- NE4
			/*String sFinalizedRelName = MCADMxUtil.getActualNameForAEFData(_context, "relationship_Finalized");
			//Get the minor type business object
			BusinessObject minorObject = null;
			Relationship finalizedRel = null;

			String minorObjId = "";
			String majorObjId = "";
		*/
			majorObject.open(_context);
			dodemotionOperation(_context, majorObject);
		/*

			BusinessObjectList list  = _util.getRelatedBusinessObjects(_context, majorObject, sFinalizedRelName, "to");
			if(list != null && list.size() > 0)
			{
				BusinessObjectItr itr = new BusinessObjectItr(list);
				while(itr.next())
				{
					minorObject = itr.obj();
				}
			}

			if(minorObject == null)
			{
				//check the "IsFinalized" attribute on the "VersionOf" relationship
				String sFinalizedAttribName = MCADMxUtil.getActualNameForAEFData(_context, "attribute_IsFinalized");

				RelationshipItr relItr = new RelationshipItr(_util.getToRelationship(_context, majorObject, (short)0, false));
				while (relItr.next())
				{
					Relationship rel = relItr.obj();					
					if(sVersionOfRelName.equals(rel.getTypeName()))
					{
						//get the attributes
						AttributeItr attribItr = new AttributeItr(rel.getAttributes(_context));
						while (attribItr.next())
						{
							Attribute thisAttrib = attribItr.obj();
							if( (thisAttrib.getName().equals(sFinalizedAttribName))&&
									(thisAttrib.getValue().equalsIgnoreCase("true")))
							{
								minorObject = rel.getFrom();
								finalizedRel = rel;
								break;
							}
						}
					}
					if(minorObject!=null)
					{
						break;
					}
				}
			}
			*/

			/**
			 * If the Major object is directly finalized at the time of checkin,
			 * then Minor object doesn't exists. But for UNdo-finalize, our Assumption is
			 * that atleast one minor is always existing. If it doesn't then Undo-finalize will fail.
			 * To make the undo finalize work, we need to forcefully create one minor with version 0
			 * and connect it with Finalized major by "VersionOf" Relationship.
			 */


		/*	boolean bDirectlyFinalized   = false;
			boolean isVersionOfRelExists = false;
			if(minorObject == null)
			{
				// Check if there is any minor object in this stream
				// If yes,
				//  createNextMinorInStream
				// else
				//  createFirstMinorInStream
				// get bogus object of CDM
				minorObject	= _util.getActiveMinor(_context, majorObject);				
				if(minorObject == null)
				{
					minorObject = createFirstMinorInStream(_context, majorObject);
				}
				else if(_util.isCDMInstalled(_context))
				{
					minorObjId        = minorObject.getObjectId();
					String Args[] = new String[2];
					Args[0] = minorObjId;
					Args[1] = "from[" + sVersionOfRelName + "].to.id";
					
					String result     = _util.executeMQL(_context, "print bus $1 select $2 dump", Args);
					if( result.startsWith("false") || result.equals("true|") )
					{
						//do nothing. minor object found is bogus minor.
					}
					else
					{
						String versionedType      = _util.getCorrespondingType(_context, majorObject.getTypeName());
						String majObjName         = majorObject.getName();
						String majRev             = majorObject.getRevision();
						String latestminorversion = _generalUtil.getLatestRevisionStringForStream(_context, versionedType,majObjName,majRev, majRev);
						minorObject = createNextMinorInStream(_context, versionedType, majObjName, latestminorversion);
						isVersionOfRelExists = true;
					}
				}
				else
				{
					String versionedType      = _util.getCorrespondingType(_context, majorObject.getTypeName());
					String majObjName         = majorObject.getName();
					String majRev             = majorObject.getRevision();
					String latestminorversion = _generalUtil.getLatestRevisionStringForStream(_context, versionedType,majObjName,majRev, majRev);

					minorObject = createNextMinorInStream(_context, versionedType, majObjName, latestminorversion);
					isVersionOfRelExists = true;
				}

				minorObjId = minorObject.getObjectId();				

				if(!isVersionOfRelExists)
				{
					//connect the newly created bus obj to the Target Rev BO
					_util.connectBusObjects(_context, minorObjId, majorObject.getObjectId(), sVersionOfRelName, true,null);					
				}
				bDirectlyFinalized = true;
			}

			majorObjId = majorObject.getObjectId();
			minorObjId = minorObject.getObjectId();

			if(bDirectlyFinalized)
			{
				String cadType = _util.getCADTypeForBO(_context, majorObject);

				if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE) && !familyMinorObjId.equals(""))
				{
					BusinessObject familyObject			= new BusinessObject(familyMinorObjId);
					familyObject.open(_context);

					BusinessObject majorFamilyObject	= _util.getMajorObject(_context, familyObject);

					if(majorFamilyObject == null)
					{
						Hashtable exceptionDetails = new Hashtable(3);
						exceptionDetails.put("TYPE", familyObject.getTypeName());
						exceptionDetails.put("NAME", familyObject.getName());
						exceptionDetails.put("REVISION", familyObject.getRevision());

						MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.BulkLoadedIndividualFamilyNotDemoted",exceptionDetails), null);
					}

					_generalUtil.connectFamilyClassObjects(_context, familyObject, minorObject);
					familyObject.close(_context);
				}
				else if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
				{
					String instanceOfRelName = MCADMxUtil.getActualNameForAEFData(_context,"relationship_InstanceOf");
					_util.moveRelationShips(_context, majorObject, minorObject, instanceOfRelName, "from");
				}
			}
			retStr.append(majorObjId);
			retStr.append("|");
			retStr.append(minorObjId);
			*/

			// Now do validations
			// [NDM] NE4 start
			//areAllParentsNonFinalized(_context, majorObject);
			// [NDM] NE4 end

		
			// start [NDM] :- NE4

			/*
			String breakPartSpec = (String)_argumentsTable.get("BreakPartSpec");
			String partSpecRelName = MCADMxUtil.getActualNameForAEFData(_context,"relationship_PartSpecification");
			if("true".equalsIgnoreCase(breakPartSpec))
			{
				_sourceName = _generalUtil.getCSENameForBusObject(_context, majorObject);
				// Break Part Specification from the major Object
				removePartRelations(_context, majorObject);
				//If EBOM Synch is done with the minor object
				//then get a list of all the minor Objects and
				//break part specification
				BusinessObjectList MinorObjList = _util.getMinorObjects(_context, majorObject);
				BusinessObjectItr MinorObjListItr = new BusinessObjectItr(MinorObjList);
				while(MinorObjListItr.next())
				{
					BusinessObject MinorObjectNew = MinorObjListItr.obj();
					removePartRelations(_context, MinorObjectNew);
				}

			}
			//end [NDM] NE4

			// start [NDM] :- NE4
			/*else if(partSpecRelName != null && partSpecRelName.length() > 0)
			{
				if(!isAssignPartToMajor(_context))
				{
					_util.moveRelationShips(_context, majorObject, minorObject, partSpecRelName, "to");

					StringList selectBus = new StringList();
					StringList selectRel = new StringList();

					ExpansionWithSelect expansionWithSelect = _util.expandSelectBusObject(_context, minorObject, partSpecRelName, "*", selectBus, selectRel, true, true, (short)1, (short)0, true);
					RelationshipWithSelectList relSelList	= expansionWithSelect.getRelationships();
					if(relSelList.size() > 0)
					{
						String cadObjectAttrName = MCADMxUtil.getActualNameForAEFData(_context,"attribute_CADObjectName");

						Enumeration relationsList = relSelList.elements();
						while(relationsList.hasMoreElements())
						{
							RelationshipWithSelect select	= (RelationshipWithSelect)relationsList.nextElement();
							select.open(_context);
							boolean isPushed	= false;
							try
							{
								com.matrixone.apps.domain.util.ContextUtil.pushContext(_context);
								isPushed= true;
								
							_util.setRelationshipAttributeValue(_context, select, cadObjectAttrName, minorObject.getName());
							}
							finally
							{
								if(isPushed)
										com.matrixone.apps.domain.util.ContextUtil.popContext(_context);
							}
							select.close(_context);
						}
					}
				}
			}
			*/

			/*
				Step 2: "Finalized" relationship will be disconnected.
			 */
			// Change the move files to version Attribute to true
			// Tranfer attributes from Major Object to Minor Object
			// And Reset attributes on major object
			
			// start [NDM] :- NE4
			/*
			if(_util.isCDMInstalled(_context))
			{
				String moveFilesToVersionAttrName = MCADMxUtil.getActualNameForAEFData(_context,"attribute_MoveFilesToVersion");
				_util.setAttributeOnBusObject(_context, majorObject,moveFilesToVersionAttrName,"True");
			}

			if(_globalConfig.isCreateVersionObjectsEnabled())
				_util.copyAttributesForFinalization(_context, _globalConfig, majorObject, minorObject,COPY_BLANK_ATTR_DURING_UNDO_FINALIZE);

			// transfer description Also.
			String desc = majorObject.getDescription(_context);
			if(!desc.equals("") || COPY_BLANK_ATTR_DURING_UNDO_FINALIZE)
				minorObject.setDescription(_context,desc);
			minorObject.update(_context);

			//Reset Attributes on Minor
			//resetAttributes(majorObject);

			RelationshipItr relItr = new RelationshipItr(_util.getToRelationship(_context, majorObject,(short)0, false));
			while (relItr.next())
			{
				Relationship rel = relItr.obj();
				if(sFinalizedRelName.equals(rel.getTypeName()))
				{
					majorObject.disconnect(_context, rel);
				}
			}

			*/


			// start [NDM] NE4:-

		/*	String sFinalizedAttribName = MCADMxUtil.getActualNameForAEFData(_context,"attribute_IsFinalized");
			if(finalizedRel!= null)
				_util.setRelationshipAttributeValue(_context, finalizedRel, sFinalizedAttribName, "false");

			/*
				Step 3: All "whereused" cases of major object will be replaced by
						minor object.
			 */

			 
			//manageRelationShipsAfterUndoFinalization(_context, majorObject, minorObject, bDirectlyFinalized);
			// end [NDM] NE4:-

			majorObject.close(_context);
		}
		catch(Exception e)
		{
			String errMessage = _serverResourceBundle.getString("mcadIntegration.Server.Message.UndoFinalizationFailed") + " " + e.getMessage();
			MCADServerException.createException(errMessage, e);
		}
		return retStr.toString();
	}

	/* [NDM] start NE4:-
	protected BusinessObject createFirstMinorInStream(Context _context, BusinessObject majorObject) throws MCADException, MatrixException
	{
		//Assumption: majorObject BusinessObject is Opened and closed by caller.
		BusinessObject minorObject = null;

		String versionedType = _util.getCorrespondingType(_context, majorObject.getTypeName());
		String version = _util.getFirstVersionStringForStream(majorObject.getRevision());
		String policy = _globalConfig.getDefaultPolicyForType(versionedType);
		String name = majorObject.getName();

		minorObject = new BusinessObject(versionedType, name, version, "");
		if(!minorObject.exists(_context))
		{
			minorObject.create(_context, policy);
			//Set the CAD Type Attribute on newly created minor
			String cadType		  = _util.getCADTypeForBO(_context, majorObject);
			String cadTypeAttName = MCADMxUtil.getActualNameForAEFData(_context,"attribute_CADType");
			_util.setAttributeValue(_context, minorObject, cadTypeAttName, cadType);
			minorObject.open(_context);
			String majorObjId = majorObject.getObjectId();
			String minorObjId = minorObject.getObjectId();
			minorObject.close(_context);
			// connect CDM Relationships for newly created minor objects
			String relLatestVersion		= MCADMxUtil.getActualNameForAEFData(_context,"relationship_LatestVersion");
			String relActiveVersion		 = MCADMxUtil.getActualNameForAEFData(_context, "relationship_ActiveVersion");
			_util.connectBusObjects(_context, minorObjId, majorObjId, relLatestVersion, false, null);
			_util.connectBusObjects(_context, minorObjId, majorObjId, relActiveVersion, false, null);

		}

		return minorObject;
	}

	protected BusinessObject createNextMinorInStream(Context _context, String versionedType, String boName, String version) throws MCADException, MatrixException
	{
		BusinessObject minorObject = null;

		BusinessObject boToBeRevised = new BusinessObject(versionedType, boName, version, "");

		minorObject = _util.reviseBusinessObject(_context, boToBeRevised, "", false, false);

		return minorObject;
	}
*/

	protected Vector getAttributesToReset(Context _context) throws MCADException
	{
		String mcadLabelAttrName					= MCADMxUtil.getActualNameForAEFData(_context,"attribute_MCADLabel");
		String renamedFromAttrName					= MCADMxUtil.getActualNameForAEFData(_context,"attribute_RenamedFrom");
		String simplifiedRepsAttName				= MCADMxUtil.getActualNameForAEFData(_context,"attribute_ProESimplifiedReps");
		String fileMsgDigestAttName					= MCADMxUtil.getActualNameForAEFData(_context,"attribute_IEF-FileMessageDigest");

		Vector attrToBeResetList = new Vector();
		attrToBeResetList.addElement(mcadLabelAttrName);
		attrToBeResetList.addElement(renamedFromAttrName);
		attrToBeResetList.addElement(simplifiedRepsAttName);
		attrToBeResetList.addElement(fileMsgDigestAttName);

		return attrToBeResetList;
	}

	// start [NDM] :- NE4

	/*
	protected void removePartRelations(Context _context, BusinessObject busObject) throws Exception
	{
		// For a drawing object (or any type of object which is part of
		// Associated Spec setting in EBOM config object),
		// only break Part Spec. The associated Model should not be touched.
		boolean isDrawing = isObjectTypeAssociatedSpec(_context, busObject);
		String policyPartSpecification = MCADMxUtil.getActualNameForAEFData(_context, "policy_PartSpecification");        
		if (!isDrawing)
		{
			BusinessObjectList mainPartList = _util.getRelatedBusinessObjects(_context, busObject, policyPartSpecification, "to");
			BusinessObjectItr mainPartsItr	= new BusinessObjectItr(mainPartList);
			while(mainPartsItr.next())
			{
				BusinessObject mainPart = mainPartsItr.obj();
				disconnectRelationships(_context, mainPart, MCADMxUtil.getActualNameForAEFData(_context, "relationship_EBOM"), "from");
			}
		}
		_util.disconnectRelationShips(_context, busObject, policyPartSpecification, "to");

		//get all the children of busObject and recursively remove
		//part relations
		Hashtable childObjectList = _generalUtil.getAllWheareUsedObjectsForFinalization(busObject, true, MCADServerSettings.ASSEMBLY_LIKE);
        Enumeration childObjects = childObjectList.keys();
        while(childObjects.hasMoreElements())
        {
            BusinessObject childObject = (BusinessObject)childObjects.nextElement();
		    removePartRelations(childObject);
	}
	}
*/

	// [NDM] NE4 start:-
	/*protected boolean isAssignPartToMajor(Context _context)  throws Exception
	{
		boolean bAssignedToMajor = false;
		String confObjTNR = _globalConfig.getEBOMRegistryTNR();
		StringTokenizer token = new StringTokenizer(confObjTNR, "|");
		if(token.countTokens() < 3)
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.EBOMRegistryNotDefined"), null);

		String confObjType			= (String) token.nextElement();
		String confObjName			= (String) token.nextElement();
		String confObjRev			= (String) token.nextElement();
		IEFEBOMConfigObject ebomConfigObj = new IEFEBOMConfigObject(_context, confObjType, confObjName, confObjRev);

		String assignPartToMajor = ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_ASSIGN_PART_TO_MAJOR);

		if("true".equalsIgnoreCase(assignPartToMajor))
		{
			bAssignedToMajor = true;
		}

		return bAssignedToMajor;
	}
	// [NDM] NE4 End:-
	*/

	protected boolean isObjectTypeAssociatedSpec(Context _context, BusinessObject busObject) throws Exception
	{
		String confObjTNR = _globalConfig.getEBOMRegistryTNR();
		StringTokenizer token = new StringTokenizer(confObjTNR, "|");
		if(token.countTokens() < 3)
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.EBOMRegistryNotDefined"), null);

		String confObjType			= (String) token.nextElement();
		String confObjName			= (String) token.nextElement();
		String confObjRev			= (String) token.nextElement();
		IEFEBOMConfigObject ebomConfigObj = new IEFEBOMConfigObject(_context, confObjType, confObjName, confObjRev);
		Hashtable confAttrDwgRelationInfo = ebomConfigObj.getAttributeAsHashtable(IEFEBOMConfigObject.ATTR_DRAWING_RELATION_INFO, "\n", "|");

		boolean isDrawing		= false;
		String busType			= busObject.getTypeName();
		String versionedType	= _util.getCorrespondingType(_context, busType);

		if(confAttrDwgRelationInfo.containsKey(busType))
		{
			isDrawing = true;
		}
		else if(versionedType != null && confAttrDwgRelationInfo.containsKey(versionedType))
		{
			isDrawing = true;
		}

		return isDrawing;
	}

	/**
	 * Disconnect all the relationships of given name in which the input bus participates
	 * at the input "end".
	 */
	protected boolean disconnectRelationships(Context _context, BusinessObject fromBus, String relName,
			String relEnd) throws Exception
			{
		boolean bRet = true;
		try
		{
			short level = 1;
			Visuals vis = _context.getDefaultVisuals();

			//build the relationship where clause to be used in expand on bus object
			StringBuffer relationshipWhereClause = new StringBuffer();
			relationshipWhereClause.append("(name == const\"");
			relationshipWhereClause.append(relName);
			relationshipWhereClause.append("\"");
			relationshipWhereClause.append(" && attribute[");
			relationshipWhereClause.append(_attrSource);
			relationshipWhereClause.append("] == const\"");
			relationshipWhereClause.append(_sourceName);
			relationshipWhereClause.append("\"");
			relationshipWhereClause.append(")");

			//call the expand method on the business object
			Expansion expansion = _util.expandBusObject(_context, fromBus, level,"",relationshipWhereClause.toString(), (short)0, false, vis);

			ExpandRelationshipList filteredRelationshipList = expansion.getRelationships();
			ExpandRelationshipItr expandItr = new ExpandRelationshipItr(filteredRelationshipList);

			while (expandItr.next())
			{
				ExpandRelationship expandRel = expandItr.obj();
				boolean isFrom = expandRel.isFrom();
				if((relEnd.equals("from") && isFrom) || (relEnd.equals("to") && !isFrom))
				{
					expandRel.remove(_context);
				}
			}
		}
		catch(Exception me)
		{
			MCADServerException.createException(me.getMessage(), me);
		}
		return bRet;
			}


	/**
	 * Checks if all the first level parents of the input bus object are finalized
	 * (Only CAD Subcomponent relationship is considered for now..)
	 * thorws Exception incase anything unfinalized found
	 */
	protected void areAllParentsNonFinalized(Context _context, BusinessObject inBus) throws Exception
	{
		// Obtain all the 1st level parents (dependents)
		try
		{
			java.util.Hashtable busObjects = _generalUtil.getAllWheareUsedObjectsForFinalization(_context, inBus, false, MCADServerSettings.ASSEMBLY_LIKE);
			String activeMinorId 			= _util.getActiveMinor(_context, inBus).getObjectId();
			
			Enumeration allObjs = busObjects.keys();
			while(allObjs.hasMoreElements())
			{
				// check "end", this is end for child node
				BusinessObject bo = (BusinessObject)allObjs.nextElement();
				
				if(bo.getObjectId().equals(inBus.getObjectId()) || bo.getObjectId().equals(activeMinorId))
					continue;
				
				// Start [NDM] NE4:-
				 // boolean bFinalized = _globalConfig.isMajorType(bo.getTypeName());
				 boolean bFinalized = false;
				
				String stateName = _util.getCurrentState(_context,bo).getName();
				String policyName = MCADMxUtil.getActualNameForAEFData(_context,"policy_DesignPolicy");
				String strApprovedState = PropertyUtil.getSchemaProperty(_context,"policy",policyName,"state_Approved");

				if(stateName.equals(strApprovedState))
					bFinalized = true;
				//End [NDM] NE4:-

				Hashtable msgTable = new Hashtable();
				msgTable.put("TYPE", bo.getTypeName());
				msgTable.put("NAME", bo.getName());
				msgTable.put("REVISION", bo.getRevision());
				//if type is Major type then it is finalized
				if(bFinalized)
				{
					MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.ParentObjectFinalized", msgTable), null);
				}
				else
				{
					bFinalized = _generalUtil.isBusObjectFinalized(_context, bo);
					if(bFinalized)
					{
						MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.ParentObjectFinalized", msgTable), null);
					}
				}
			}
		}
		catch (Exception me)
		{
			MCADServerException.createException(me.getMessage(), me);
		}
	}

	/**
	 * Do proper rev replace after successful finalization.
	 * can implement business rules, default impl. is
	 * Disconnect all the children (dependees) of this object undoFinalizedFromObj,
	 * Move the 'Parent (from)' relationships from undoFinalizedFromBO to the undoFinalizedToBO
	 */
	 // [NDM] NE4 start:-
	/*
	protected boolean manageRelationShipsAfterUndoFinalization(Context _context, BusinessObject undoFinalizedFromObj,
			BusinessObject undoFinalizedToObj,
			boolean bDirectlyFinalized ) throws Exception
			{
		boolean bRet = true;
				boolean isPushed    = false;

		try
		{

			com.matrixone.apps.domain.util.ContextUtil.pushContext(_context);
			isPushed    = true;

			// Disconnect all the children (dependees) of this object, undoFinalizedFromBO
			// Skip the External Ref relations

			if(bDirectlyFinalized)
			{
				_generalUtil.shuffleAssemblyLikeRelatinships(_context, undoFinalizedFromObj, undoFinalizedToObj, true, false, MCADServerSettings.ASSEMBLY_LIKE);
			}
			else
			{
				_generalUtil.disconnectAssemblyRelationshipsForObject(_context, undoFinalizedFromObj, true, MCADServerSettings.ASSEMBLY_LIKE);
			}

			// Handle external ref relations - Move all from Ext. Ref. relationships
			_generalUtil.shuffleRelatinships(_context, undoFinalizedFromObj, undoFinalizedToObj, true, false, MCADServerSettings.EXTERNAL_REFERENCE_LIKE);

			// Move all DERIVEDOUTPUT_LIKE relationship from Major to Minor
			_generalUtil.shuffleRelatinships(_context, undoFinalizedFromObj,undoFinalizedToObj ,true, false, MCADServerSettings.DERIVEDOUTPUT_LIKE);

			// Move the 'Parent (from)' relationships (wheare the object in questin is a dependee)
			// of the undoFinalizedFromBO to the undoFinalizedToBO
			// Skip the External Ref relations
			//_generalUtil.shuffleRelatinships(undoFinalizedFromObj, undoFinalizedToObj, false, false, MCADServerSettings.ASSEMBLY_LIKE);
			_generalUtil.shuffleAssemblyLikeRelatinships(_context, undoFinalizedFromObj, undoFinalizedToObj, false, false, MCADServerSettings.ASSEMBLY_LIKE);

			// Handle external ref relations - Move all from Ext. Ref. relationships
			_generalUtil.shuffleRelatinships(_context, undoFinalizedFromObj, undoFinalizedToObj, false, false, MCADServerSettings.EXTERNAL_REFERENCE_LIKE);
			
			String cadType = this._util.getCADTypeForBO(_context, undoFinalizedFromObj);
			
			if(this._globalConfig.isTypeOfClass(cadType,MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
			{
				String strAciveInstance = MCADMxUtil.getActualNameForAEFData(_context, "relationship_ActiveInstance");
				_util.disconnectRelationShips(_context, undoFinalizedFromObj,strAciveInstance,"from");
			}
			else if(_globalConfig.isTypeOfClass(cadType,MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
				_generalUtil.shuffleActiveInstanceRelatinship(_context, undoFinalizedFromObj, undoFinalizedToObj);
		}
		catch(Exception me)
		{
			MCADServerException.createException(me.getMessage(), me);
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
					MCADServerException.createException(ex.getMessage(), ex);
				}
			}
		}

		return bRet;
			}

*/
	public void dodemotionOperation(Context _context, BusinessObject majorObject) throws Exception
	{
		String finalizationState	= _globalConfig.getFinalizationState(majorObject.getPolicy(_context).getName());
		String currentStateName		= _util.getCurrentState(_context, majorObject).getName();
		if(currentStateName.equalsIgnoreCase(finalizationState))
		{
			majorObject.demote(_context);

			addSelectedObjectToResponse(_context,majorObject);
			if(_globalConfig.isModificationEvent(MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION))
				modifyUpdateStamp(_context, majorObject);
		}
	}
	
	private void modifyUpdateStamp(Context _context, BusinessObject majorObject)throws MatrixException, Exception
	{
		majorObject.open(_context);
		String majorCadType = _util.getCADTypeForBO(_context, majorObject);
		String mxType = majorObject.getTypeName();
		majorObject.close(_context);

		Vector attr =  _globalConfig.getCADAttribute(mxType, "current", majorCadType);

		if(!attr.isEmpty())					
			_util.modifyUpdateStamp(_context, majorObject.getObjectId(_context));
	}

	protected void doFileOperations(Context _context, String majorMinorObjIdString) throws MCADException
	{
		StringTokenizer majorMinorObjIdToken = new StringTokenizer(majorMinorObjIdString, "@");
		String majorObjId = "";
		String minorObjId = "";
		while(majorMinorObjIdToken.hasMoreTokens())
		{
			String majorMinorObjId 	= majorMinorObjIdToken.nextToken();
			StringTokenizer toks 	= new StringTokenizer(majorMinorObjId, "|");
			if(toks.hasMoreTokens())
			{
				majorObjId = toks.nextToken();
				minorObjId = toks.nextToken();

				doUnFinalizeFileOperations(_context, majorObjId, minorObjId);
			}
		}
	}

	protected void doUnFinalizeFileOperations(Context _context, String busID, String minorObjId) throws MCADException
	{
		try
		{
			BusinessObject majorObject = new BusinessObject(busID);
			BusinessObject minorObject = new BusinessObject(minorObjId);

			//do the file transfer from major to minor
			_util.moveFilesFcsSupported(_context, majorObject, minorObject);

			majorObject.open(_context);

			minorObject.open(_context);

			if(_globalConfig.isCreateVersionObjectsEnabled())
			{
				//copy attributes of Latest minor to major object.
				_util.copyAttributesForFinalization(_context, _globalConfig, majorObject, minorObject,COPY_BLANK_ATTR_DURING_UNDO_FINALIZE);
			}

			// transfer description Also.
			String desc = majorObject.getDescription(_context);
			if(!desc.equals("") || COPY_BLANK_ATTR_DURING_UNDO_FINALIZE)
				minorObject.setDescription(_context,desc);

			minorObject.update(_context);

			String currentLatestMinorID = _util.getLatestMinorID(_context, majorObject);

			String inputActiveMinorID   = minorObject.getObjectId(_context);

			if(!inputActiveMinorID.equals(currentLatestMinorID))
			{
				BusinessObject latestMinorObject = new BusinessObject(currentLatestMinorID);

				latestMinorObject.open(_context);

				_util.resetActiveVersionRelationship(_context, majorObject.getObjectId(_context), currentLatestMinorID);

				//copy attributes of Latest minor to major object.
				_util.copyAttributesForFinalization(_context, _globalConfig, latestMinorObject, majorObject, COPY_BLANK_ATTR_DURING_UNDO_FINALIZE);

				desc = latestMinorObject.getDescription(_context);

				if(!desc.equals("") || COPY_BLANK_ATTR_DURING_UNDO_FINALIZE)
					majorObject.setDescription(_context,desc);

				majorObject.update(_context);

				latestMinorObject.close(_context);
			}

			minorObject.close(_context);
			majorObject.close(_context);

			if(_util.isCDMInstalled(_context))
			{
				String titleAttribute	= MCADMxUtil.getActualNameForAEFData(_context,"attribute_Title");
				if(_globalConfig.isObjectAndFileNameDifferent())
				{
					String oldTitle = _util.getAttributeForBO(_context, majorObject, titleAttribute);
					_util.setAttributeValue(_context, majorObject, titleAttribute, oldTitle);
				}
				else
				_util.setAttributeValue(_context, majorObject, titleAttribute, majorObject.getName());
				
			}
		}
		catch (Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
	}

	protected boolean isInFinalizationState(Context _context, BusinessObject busObj) throws Exception
	{
		boolean isInFinalizationState	= false;

		busObj.open(_context);
		String finalizationState		= _globalConfig.getFinalizationState(busObj.getPolicy(_context).getName());
		String currentState				= _util.getCurrentState(_context, busObj).getName();
		if(finalizationState.equalsIgnoreCase(currentState))
			isInFinalizationState =  true;
		busObj.close(_context);
		return isInFinalizationState;
	}

	protected void demoteBusObject(Context _context, BusinessObject busObj) throws Exception
	{
		try
		{
			busObj.open(_context);
			
			if(_util.isInitialState(_context, _globalConfig, busObj.getObjectId()))
			{
				MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.ObjectInInitialState"), null);
			}
			else
			{
				busObj.demote(_context);

				addSelectedObjectToResponse(_context,busObj);
				if(_globalConfig.isModificationEvent(MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION))
					modifyUpdateStamp(_context, busObj);
			}
			busObj.close(_context);
		}
		catch (Exception me)
		{
			busObj.close(_context);
			MCADServerException.createException(me.getMessage(), me);
		}
	}
	
	public void addSelectedObjectToResponse(Context context,BusinessObject businessObject) throws MCADException{
		Hashtable cadNodeContentsTable = new Hashtable();
		IEFXmlNodeImpl responseCadObjectNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		responseCadObjectNode.setName("cadobject");
		
		String busid = businessObject.getObjectId();
		
		cadNodeContentsTable.put("busid", busid);
		
		responseCadObjectNode.setAttributes(cadNodeContentsTable);
		responseCadObjectList.addNode(responseCadObjectNode);

	}
	
	private void lockCheck(Context context, String objectId) throws Exception
	{
		try
		{	
                         if(!_util.isRoleAssigned(context,"role_VPLMAdmin"))
			{
			_busObject	= new BusinessObject(objectId);
		
			if (_busObject == null)
			{
				MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.BusinessObjectNotFound"), null);
			}

			//Lock not to be checked for Admin
			
				if(_util.isMajorObject(context, objectId))
				{
                                        _busObject.open(context);
					if (!_generalUtil.checkLockStatus(context, _busObject))
					{
						Hashtable exceptionDetails = new Hashtable(4);
						exceptionDetails.put("TYPE",_busObject.getTypeName());
						exceptionDetails.put("NAME",_busObject.getName());
						exceptionDetails.put("REVISION",_busObject.getRevision());
						exceptionDetails.put("LOCKER",_busObject.getLocker(context).getName());
										
						MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FinalizedBusObjLocked",exceptionDetails), null);
					}
                                        _busObject.close(context);
								
				}
				else
				{
						MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.UndoFinalizeNotValidForMinorTypes"), null);
				}
			
			

                        }
		}
		catch(Exception e)
		{
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToUndoFinalize") + e.getMessage(), e);
		}
	}
}

