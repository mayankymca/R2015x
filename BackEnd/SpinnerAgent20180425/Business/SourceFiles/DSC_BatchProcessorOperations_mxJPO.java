/*
**  DSC_BatchProcessorOperations
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
** Program to do Batch-Processor Related operations for Rename
*/

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.User;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCMessage;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCQueue;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNodeImpl;

public class DSC_BatchProcessorOperations_mxJPO
{
	Hashtable _argumentsTable								= null;
	private MCADGlobalConfigObject _globalConfig			= null;
	private MCADMxUtil _util								= null;
	private MCADServerGeneralUtil _generalUtil				= null;
	private MCADServerResourceBundle _serverResourceBundle	= null;
	private Vector majorObjectsValidatedList				= null;
	private StringBuffer messageErrorDiffrentLocker			= null;
	private StringBuffer messageErrorNotInPreliminaryState	= null;
	private final String BATCH_RENAME_OPERATION				= "BatchRename";

	public DSC_BatchProcessorOperations_mxJPO (Context context, MCADMxUtil util, MCADServerGeneralUtil generalUtil, MCADGlobalConfigObject globalConfig, MCADServerResourceBundle serverResourceBundle)
    {
		this._util					= util;
		this._globalConfig			= globalConfig;
		this._serverResourceBundle	= serverResourceBundle;
		this._generalUtil			= generalUtil;
    }

	public DSC_BatchProcessorOperations_mxJPO (Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            throw new Exception("not supported no desktop client");

    }

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}
	public void executeCustom(Hashtable resultAndStatusTable)  throws MCADException
	{
	}

	public void doBatchprocessorRelatedOperations(Context context, String[] args) throws Exception
	{
		_argumentsTable = (Hashtable) JPO.unpackArgs(args);

		String languageName				= (String)_argumentsTable.get(MCADServerSettings.LANGUAGE_NAME);
		String integrationName			= (String)_argumentsTable.get(MCADAppletServletProtocol.INTEGRATION_NAME);
		String newName					= (String)_argumentsTable.get(MCADServerSettings.NEW_NAME);
		String priority					= (String)_argumentsTable.get(MCADServerSettings.MESSAGE_PRIORITY);
		String _busObjectID				= (String)_argumentsTable.get(MCADServerSettings.OBJECT_ID);
		BusinessObjectList objectsList	= (BusinessObjectList)_argumentsTable.get(MCADServerSettings.SELECTED_OBJECTID_LIST);

		//Added for structure rename support
		String omitPendingMessageEntry	= (String)_argumentsTable.get("updateMessage");

		_globalConfig					= (MCADGlobalConfigObject)_argumentsTable.get(MCADServerSettings.GCO_OBJECT);

		_serverResourceBundle	= new MCADServerResourceBundle(languageName);
		IEFGlobalCache cache	= new IEFGlobalCache();
		_util					= new MCADMxUtil(context, _serverResourceBundle, cache);
		_generalUtil			= new MCADServerGeneralUtil(context,_globalConfig, _serverResourceBundle, cache);

		BusinessObject _busObject									= new BusinessObject(_busObjectID);
		DSC_GetBatchProcessorDetails_mxJPO batchProcessorDetails = new DSC_GetBatchProcessorDetails_mxJPO();
		majorObjectsValidatedList									= new Vector();
		Hashtable assemblyRelationshipTable							= new Hashtable();
		Hashtable revisionInfoTable									= new Hashtable();

		String personSiteName		= getPersonSiteName(context);
		BusinessObjectItr objItr	= new BusinessObjectItr(objectsList);

		while(objItr.next())
		{
			BusinessObject bus = objItr.obj();
			bus.open(context);

			boolean addToRevisionsList = true;      // [NDM] : L86 Since Finalization is no longer supported in NDM.
		       /*	if(!_util.isMajorObject(context, bus.getObjectId()) && !_generalUtil.isBusObjectFinalized(context, bus))
				addToRevisionsList = true;
			else if(_util.isMajorObject(context, bus.getObjectId()) && _generalUtil.isBusObjectFinalized(context, bus)) // _globalConfig.isMajorType(bus.getTypeName()) // [NDM] OP6
				addToRevisionsList = true;*/

			if(addToRevisionsList)
			{
				String busRevision	= bus.getRevision();
				String cadType		= _util.getCADTypeForBO(context, bus);
				Vector revisionInfo	= new Vector();

				revisionInfo.addElement(cadType);
				if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
				{
					BusinessObject familyObject = _generalUtil.getFamilyObjectForInstance(context, bus);
					String parentId				= familyObject.getObjectId(context);

					revisionInfo.addElement(parentId);
				}
				else
					revisionInfo.addElement("");

					revisionInfoTable.put(busRevision, revisionInfo);
			}

			lockDesignForBatchProcessOperation(context, bus, batchProcessorDetails, personSiteName);
			addToAssemblyRelationshipTable(context, bus, assemblyRelationshipTable);
			bus.close(context);
		}

		String queueName = "";
		if(personSiteName == null || personSiteName.trim().equals(""))
		{
			String[] args1 = new String[1];
			args1[0]			= BATCH_RENAME_OPERATION;		

			queueName = batchProcessorDetails.getDefaultSiteBatchOperationQueueName(context, args1);
		}
		else
		{
			try
			{
				String[] args2 = new String[2];
				args2[0]			= BATCH_RENAME_OPERATION;
				args2[1]			= personSiteName;
				
				queueName = batchProcessorDetails.getBatchOperationQueueNameForSite(context, args2);
			}
			catch(Exception exception)
			{
				Hashtable exceptiontable = new Hashtable(1);
				exceptiontable.put("OPERATION",BATCH_RENAME_OPERATION);
				MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToGetBatchOperationQueueName", exceptiontable), exception);
			}
		}

		DSCQueue renameQueue = new DSCQueue(context,MCADMxUtil.getActualNameForAEFData(context, "type_DSCQueue"), queueName);

		if(!renameQueue.exists(context))
		{
			renameQueue.create(context, MCADMxUtil.getActualNameForAEFData(context, "policy_DSCQueuePolicy"));
		}

		renameQueue.open(context);

		String uniqueName				  = newName + ":" + _busObject.getObjectId(context);
		BusinessObject renamedMajorObject = _util.getMajorObject(context, _busObject);
		if(renamedMajorObject == null)
			renamedMajorObject = _busObject;

		String messageBody	= createMessageBody(context, integrationName, uniqueName, renamedMajorObject, revisionInfoTable, assemblyRelationshipTable, batchProcessorDetails, personSiteName);

		if(messageErrorDiffrentLocker != null || messageErrorNotInPreliminaryState != null)
		{
			// This means that either some design is locked or some design is not in preliminary state
			String errorMessage = "";
			if(messageErrorDiffrentLocker != null)
				errorMessage = messageErrorDiffrentLocker.toString();
			if(messageErrorNotInPreliminaryState != null)
				errorMessage = errorMessage + messageErrorNotInPreliminaryState.toString();
			MCADServerException.createException(errorMessage, null);
		}

		DSCMessage messageObject	= new DSCMessage(context, uniqueName, messageBody);
		if(!messageObject.exists(context))
		{
			messageObject.create(context, MCADMxUtil.getActualNameForAEFData(context,"policy_DSCMessagePolicy"));
			_util.setAttributeOnBusObject(context, messageObject, MCADMxUtil.getActualNameForAEFData(context,"attribute_Title"), BATCH_RENAME_OPERATION);
		}

		messageObject.open(context);
		messageObject.setBody(context, messageBody);
		messageObject.setPriority(context, priority);

		if(omitPendingMessageEntry == null)
		{
			renameQueue.addPendingMessage(context, messageObject);
		}
		else if(!omitPendingMessageEntry.equals("") && !omitPendingMessageEntry.equals("true"))
		{
			renameQueue.addPendingMessage(context, messageObject);
		}

	}

	private String createMessageBody(Context context, String integrationName,  String messageName, BusinessObject renamedMajorObject, Hashtable revisionInfoTable, Hashtable assemblyRelationshipTable, DSC_GetBatchProcessorDetails_mxJPO batchProcessorDetails, String personSiteName) throws Exception
	{
		IEFXmlNodeImpl message = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		message.setName("message");

		IEFXmlNodeImpl messageNameNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		messageNameNode.setName("messagename");
		messageNameNode.setContent(messageName);

		IEFXmlNodeImpl operationname = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		operationname.setName("operationname");
		operationname.setContent(BATCH_RENAME_OPERATION);

		IEFXmlNodeImpl integrationname = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		integrationname.setName("integrationname");
		integrationname.setContent(integrationName);

		IEFXmlNodeImpl messagebody = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		messagebody.setName("messagebody");

		IEFXmlNodeImpl renamedComponentNode 	= new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		renamedComponentNode.setName("renamedcomponent");

		IEFXmlNodeImpl revisionList = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		revisionList.setName("revisionlist");

		Enumeration renamedRevisions	= revisionInfoTable.keys();
		Vector handledRenamedRevisions	= new Vector();
		while(renamedRevisions.hasMoreElements())
		{
			IEFXmlNodeImpl revisionInfoNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
			revisionInfoNode.setName("revisioninfo");

			String renamedRevision		= (String)renamedRevisions.nextElement();
			Vector revisionInfoNodeData	= (Vector)revisionInfoTable.get(renamedRevision);
			String type					= (String)revisionInfoNodeData.elementAt(0);
			String parentId				= (String)revisionInfoNodeData.elementAt(1);

			StringBuffer handledRenamedRevision = new StringBuffer();
			handledRenamedRevision.append(renamedRevision);
			handledRenamedRevision.append(":");
			handledRenamedRevision.append(parentId);

			if(!handledRenamedRevisions.contains(handledRenamedRevision.toString()))
			{
				handledRenamedRevisions.addElement(handledRenamedRevision.toString());

				IEFXmlNodeImpl typeNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
				typeNode.setName("type");
				typeNode.setContent(type);
				revisionInfoNode.addNode(typeNode);

				IEFXmlNodeImpl renamedRevisionNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
				renamedRevisionNode.setName("revision");
				renamedRevisionNode.setContent(renamedRevision);
				revisionInfoNode.addNode(renamedRevisionNode);

				IEFXmlNodeImpl parentIdNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
				parentIdNode.setName("parentid");
				parentIdNode.setContent(parentId);
				revisionInfoNode.addNode(parentIdNode);

				revisionList.addNode(revisionInfoNode);
			}
		}

		renamedComponentNode.addNode(revisionList);

		IEFXmlNodeImpl assemblylist = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		assemblylist.setName("assemblylist");

		renamedMajorObject.open(context);

		addTypeNameRevision(context, renamedMajorObject, renamedComponentNode, null);

		Enumeration connectedAssemblies = assemblyRelationshipTable.keys();
		while(connectedAssemblies.hasMoreElements())
		{
			String associatedBusId			= (String)connectedAssemblies.nextElement();
			String relationshipName			= (String)assemblyRelationshipTable.get(associatedBusId);
			BusinessObject associatedBus	= new BusinessObject(associatedBusId);
			associatedBus.open(context);

			// For validating associated designs for State & Lock.
			//[NDM] OP6
			//String busType					= associatedBus.getTypeName();
			boolean isMinorType				= !_util.isMajorObject(context, associatedBusId);//!_globalConfig.isMajorType(busType);
			BusinessObjectList newObjList	= _util.getRevisionBOsOfAllStreams(context, associatedBus, isMinorType);
			BusinessObjectItr objItr		= new BusinessObjectItr(newObjList);

			while(objItr.next())
			{
				BusinessObject busObject	= objItr.obj();
				busObject.open(context);
				lockDesignForBatchProcessOperation(context, busObject, batchProcessorDetails, personSiteName);
				busObject.close(context);
			}

			IEFXmlNodeImpl assemblyNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
			assemblyNode.setName("assembly");

			addTypeNameRevision(context, associatedBus, assemblyNode, relationshipName);

			assemblylist.addNode(assemblyNode);
			associatedBus.close(context);
		}

		renamedMajorObject.close(context);
		messagebody.addNode(assemblylist);
		messagebody.addNode(renamedComponentNode);
		message.addNode(messageNameNode);
		message.addNode(operationname);
		message.addNode(integrationname);
		message.addNode(messagebody);

		return message.getXmlString();
	}

	private void lockDesignForBatchProcessOperation(Context context, BusinessObject busObject, DSC_GetBatchProcessorDetails_mxJPO batchProcessorDetails, String personSiteName) throws Exception
	{

		Vector objectsToLock		= new Vector();
		BusinessObject majorBus		= _util.getMajorObject(context, busObject);
		if(majorBus == null)
			majorBus = busObject;
		else
			majorBus.open(context);
		String majorId = majorBus.getObjectId(context);
		
		objectsToLock.addElement(majorId);
		String cadType	= _util.getCADTypeForBO(context, majorBus);

		if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
		{
			BusinessObject activeMinor		= _util.getActiveMinor(context, majorBus);
			String topLevelFamilyObjectID	= _generalUtil.getTopLevelFamilyObjectForInstance(context, activeMinor.getObjectId(context));
			BusinessObject familyObject = new BusinessObject(topLevelFamilyObjectID);

			BusinessObject majorFamilyObject = _util.getMajorObject(context, familyObject);
			if(majorFamilyObject == null)
				majorFamilyObject = familyObject;
			objectsToLock.addElement(majorFamilyObject.getObjectId(context));
		}

		Enumeration objectsToValidate = objectsToLock.elements();
		while(objectsToValidate.hasMoreElements())
		{
			String majorObjectId = (String)objectsToValidate.nextElement();
			if(!majorObjectsValidatedList.contains(majorObjectId))
			{
				majorObjectsValidatedList.addElement(majorObjectId);
				BusinessObject majorObject = new BusinessObject(majorObjectId);
				majorObject.open(context);
				String busName	= majorObject.getName();
				String revision	= majorObject.getRevision();

				User lockUser		= majorObject.getLocker(context);
				String objectLocker = lockUser.getName();

				if(objectLocker != null)
				{
					if(objectLocker.equalsIgnoreCase(context.getUser()))
					{
						String Args[] = new String[1];
						Args[0] = majorObjectId;
						String result		= _util.executeMQLAsShadowAgent(context,"unlock bus $1", Args);

						if(!result.startsWith("true"))
						{
							Hashtable messageDetails = new Hashtable(2);
							messageDetails.put("LOCKER", context.getUser());

							MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToUnlockDesign", messageDetails), null);
						}
					}
					else if(!objectLocker.equals("") && !objectLocker.equalsIgnoreCase(context.getUser()))
					{
						Hashtable messageTable = new Hashtable();
						messageTable.put("NAME", busName);
						messageTable.put("REVISION", revision);
						messageTable.put("LOCKER", objectLocker);
						if(messageErrorDiffrentLocker == null)
						{
							messageErrorDiffrentLocker = new StringBuffer("<br><br>");
							messageErrorDiffrentLocker.append(_serverResourceBundle.getString("mcadIntegration.Server.Message.RevisionStreamLocked"));
							messageErrorDiffrentLocker.append("<br>");
						}
						// For creating Table on Exception Details Page
						String messageString = "<br>";
						messageString += _serverResourceBundle.getString("mcadIntegration.Server.Message.NameRevisionLocker", messageTable);
						messageErrorDiffrentLocker.append(messageString);
					}

					String batchUserName		= "";
					String batchUserPassword	= "";

					if(personSiteName == null || personSiteName.equals(""))
					{
						String[] args1			= new String[0];
						batchUserName		= batchProcessorDetails.getDefaultBatchOperationUserName(context,args1);
						batchUserPassword	= batchProcessorDetails.getDefaultBatchOperationUserPassword(context,args1);
					}
					else
					{
						try
						{
							String[] args2 = new String[2];
							args2[0]			= BATCH_RENAME_OPERATION;
							args2[1]			= personSiteName;
							
							batchUserName		= batchProcessorDetails.getBatchOperationUserNameForSite(context,args2);
							batchUserPassword	= batchProcessorDetails.getBatchOperationUserPasswordForSite(context,args2);
						}
						catch(Exception exception)
						{
							MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToGetBatchDetails"), exception);
						}
					}

					if(batchUserName != null && !batchUserName.equals("") )
					{
						try
						{
							String Args[] = new String[2];
							Args[0] = batchUserName;
							Args[1] = batchUserPassword;
							_util.executeMQL(context ,"push context user $1 password $2", Args);
							Args = new String[1];
							Args[0] = majorObject.getObjectId();
							_util.executeMQL(context ,"lock bus $1", Args);
						}
						finally
						{
							String Args[] = new String[1];
							Args[0] = "context";						
							_util.executeMQL(context,"pop $1", Args);
						}
					}
				}

				majorObject.close(context);
			}
		}
	}

	private void addTypeNameRevision(Context context, BusinessObject bus, IEFXmlNodeImpl parentNode, String relationshipName) throws Exception
	{
		String cadType 		= _util.getCADTypeForBO(context, bus);
		String busName 		= bus.getName();
		String busRevision 	= bus.getRevision();

		IEFXmlNodeImpl type = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		type.setName("type");

		type.setContent(cadType);
		parentNode.addNode(type);

		IEFXmlNodeImpl name = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		name.setName("name");
		name.setContent(busName);
		parentNode.addNode(name);

		IEFXmlNodeImpl revision = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		revision.setName("revision");
		revision.setContent(busRevision);
		parentNode.addNode(revision);

		if (relationshipName != null)
		{
			IEFXmlNodeImpl relationship = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
			relationship.setName("relationship");
			relationship.setContent(relationshipName);
			parentNode.addNode(relationship);

		}

		if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
		{
			BusinessObject familyObject = _generalUtil.getFamilyObjectForInstance(context, bus);
			String parentId				= "";
			if(familyObject != null)
				parentId = familyObject.getObjectId(context);

			IEFXmlNodeImpl parentIdNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
			parentIdNode.setName("parentid");
			parentIdNode.setContent(parentId);
			parentNode.addNode(parentIdNode);
		}
		else
		{
			IEFXmlNodeImpl parentIdNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
			parentIdNode.setName("parentid");
			parentIdNode.setContent("");
			parentNode.addNode(parentIdNode);
		}
	}

	private void addToAssemblyRelationshipTable(Context context, BusinessObject bus, Hashtable assemblyRelationshipTable) throws Exception
	{
		Hashtable assemblyRelsAndEnds		=  _globalConfig.getRelationshipsOfClass(MCADServerSettings.ASSEMBLY_LIKE);
		Hashtable externalRefRelsAndEnds	=  _globalConfig.getRelationshipsOfClass(MCADServerSettings.EXTERNAL_REFERENCE_LIKE);
		assemblyRelsAndEnds.putAll(externalRefRelsAndEnds);

		Enumeration assemblyRelsList = assemblyRelsAndEnds.keys();
		while(assemblyRelsList.hasMoreElements())
		{
			String relName	= (String)assemblyRelsList.nextElement();
			String relEnd	= (String)assemblyRelsAndEnds.get(relName);

			BusinessObjectList relatedSubCompRelObjects	= _util.getRelatedBusinessObjects(context, bus, relName, relEnd);
			BusinessObjectItr itr						= new BusinessObjectItr(relatedSubCompRelObjects);
			while(itr.next())
			{
				BusinessObject associatedBus	= itr.obj();
				associatedBus.open(context);
				String busId					= associatedBus.getObjectId();

				boolean addToTable = true; // [NDM] : L86 Since Finalization is no longer supported in NDM.
				/*if(!_util.isMajorObject(context, busId) && !_generalUtil.isBusObjectFinalized(context, associatedBus))
					addToTable = true;
				else if(_util.isMajorObject(context, busId) && _generalUtil.isBusObjectFinalized(context, associatedBus))//_globalConfig.isMajorType(associatedBus.getTypeName()) // [NDM] Op6
					addToTable = true;*/

				if(addToTable)
					assemblyRelationshipTable.put(busId, relName);

				associatedBus.close(context);
			}
		}
	}

	private String getPersonSiteName(Context context) throws MCADException
	{
		String personSite	= null;
		String Args[] = new String[1];
		Args[0] = context.getUser();
		String mqlResult	= _util.executeMQL(context ,"print person $1 select site dump", Args);

		if(mqlResult.startsWith("true|"))
			personSite = mqlResult.substring(mqlResult.indexOf("|")+1, mqlResult.length());
		else
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToGetSiteName") + mqlResult, null);

		return personSite;
	}
}

