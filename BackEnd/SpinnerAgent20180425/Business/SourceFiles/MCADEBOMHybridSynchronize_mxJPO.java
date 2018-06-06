/*
 **  MCADEBOMHybridSynchronize
 **
 **  Copyright Dassault Systemes, 1992-2010.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  JPO for BOM-VPM team to invoke DEC "EBOM synchronization".
 */
import java.util.*;

import matrix.db.*;

import com.matrixone.MCADIntegration.utils.*;
import com.matrixone.MCADIntegration.server.*;
import com.matrixone.MCADIntegration.server.beans.*;
import com.matrixone.MCADIntegration.server.cache.*;

/**
 * This JPO is invoked by BOM-VPM
 * Do not remove/modify this JPO 
 * @author UNG
 */

public class MCADEBOMHybridSynchronize_mxJPO
{
	private Context _context								= null;
	private String _objectId 								= null;
	private String acceptLanguage							= null;
	private String isClientOSWindows 						= null;
	private String uniqueID									= null;
	private MCADServerResourceBundle serverResourceBundle	= null;
	private IEFGlobalCache cache							= null;
	private MCADMxUtil mxUtil								= null;
	private MCADGlobalConfigObject globalConfigObject		= null;
	
	private static final String PART_CREATED = "PartCreated";
	private static final String PART_UPDATED = "PartUpdated";
	private static final String PART_FAILED = "FailedToCreatePart";

	public MCADEBOMHybridSynchronize_mxJPO()
	{
	}

	public int mxMain(Context context, String[] args) throws Exception
	{
		return 0;
	}

	//Initializes all required classes to invoke DEC's MCADEBOMSynchronize JPO
	private void init() throws Exception
	{
		this.serverResourceBundle	= new MCADServerResourceBundle(acceptLanguage);
		this.cache					= new IEFGlobalCache();
		this.mxUtil					= new MCADMxUtil(_context, serverResourceBundle, cache);

		this.globalConfigObject = getGlobalConfigObject(_context, acceptLanguage, _objectId, isClientOSWindows);
	}

	//Entry point
	public Hashtable execute(Context context, String[] args) throws Exception
	{
		Hashtable BOMSyncResultTable = new Hashtable();

		//Set context
		_context = context;

		//Read arguments
		Hashtable argsTable	= (Hashtable)JPO.unpackArgs(args);

		this._objectId 			= (String)argsTable.get("OBJECTID");
		this.acceptLanguage		= (String)argsTable.get("LanguageName");
		this.isClientOSWindows 	= (String)argsTable.get("IsClientOSWindows");
		this.uniqueID			= java.util.UUID.randomUUID().toString();

		//initialization
		init();

		try
		{
			//Preparing arguments for JPO
			Hashtable classArgs = new Hashtable();

			//Pack the arguments for JPO
			String [] packClassArgs = JPO.packArgs(classArgs);

			//Instantiation of MCADEBOMSynchronize JPO
			MCADEBOMSynchronize_mxJPO syncJPO = new MCADEBOMSynchronize_mxJPO(context, packClassArgs);

			//Preparing arguments for JPO
			Hashtable methodArgs = new Hashtable();
			methodArgs.put(MCADServerSettings.OBJECT_ID, _objectId);
			methodArgs.put(MCADServerSettings.GCO_OBJECT, globalConfigObject);
			methodArgs.put(MCADServerSettings.OPERATION_UID, uniqueID);
			methodArgs.put(MCADServerSettings.LANGUAGE_NAME, acceptLanguage);

			//Pack the arguments for JPO
			String [] packMethodArgs = JPO.packArgs(methodArgs);

			//Executing EBOM Sync
			Hashtable jpoExecutionStatusTable = syncJPO.execute(context, packMethodArgs);

			updateBOMSyncResult(_objectId, jpoExecutionStatusTable, BOMSyncResultTable);
		}
		catch (Exception e) 
		{
			BOMSyncResultTable.put(_objectId, e.getMessage());
		}

		return BOMSyncResultTable;
	}

	//Returns the GCO for the corresponding integration
	private MCADGlobalConfigObject getGlobalConfigObject(Context context, String acceptLanguage, String objId, String isClientOSWindows) throws Exception
	{
		MCADGlobalConfigObject gco = new MCADGlobalConfigObject();

		try 
		{
			//Instantiating MCADConfigObjectLoader & IEFSimpleConfigObject to create GCO
			MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(null);
			IEFSimpleConfigObject simpleConfig  = IEFSimpleConfigObject.getSimpleLCO(context);
			if(!simpleConfig.isObjectExists())
				simpleConfig = IEFSimpleConfigObject.getSimpleUnassignedIntegRegistry(context);

			String gcoType	= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
			String attrName	= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");

			//Get integration name from BO
			BusinessObject bo			= new BusinessObject(objId);
			String sourceAttribValue	= (bo.getAttributeValues(context, "Source")).getValue();
			String integrationName		= getIntegrationName(sourceAttribValue);

			Hashtable integNameGCOMapping = simpleConfig.getAttributeAsHashtable(attrName, "\n", "|");

			if(integNameGCOMapping.containsKey(integrationName))
			{
				String gcoName = (String)integNameGCOMapping.get(integrationName);
				gco = configLoader.createGlobalConfigObject(context, mxUtil, gcoType, gcoName);
			}
		} 
		catch(Exception e) 
		{
			throw new Exception("Failed while creating GCO error: " + e.getMessage());
		}

		return gco;
	}

	//Get integration name from the Source Attribute.
	private String getIntegrationName(String sourceAttrib)
	{
		String result = sourceAttrib;
		if (result == null || result.length() < 0)
			return "";

		int pos = result.indexOf("|");
		if (pos > 0)
		{
			return result.substring(0, pos);
		}

		if(result.length() > 0 )
			return result;
		else
			return "";
	}

	//Update results
	private void updateBOMSyncResult(String objId, Hashtable jpoExecutionStatusTable, Hashtable BOMSyncResultTable) throws Exception
	{
		//JPO Execution Status
		String jpoExecutionStatus = null;
		String jpoStatusMessage = null;
		Hashtable handledPartObjIdsTable = null;

		if(jpoExecutionStatusTable != null)
		{
			jpoExecutionStatus 		= (String)jpoExecutionStatusTable.get("jpoExecutionStatus");
			jpoStatusMessage 		= (String)jpoExecutionStatusTable.get("jpoStatusMessage");
			handledPartObjIdsTable 	= (Hashtable)jpoExecutionStatusTable.get("handledPartObjIdsTable");
		}

		if(null != jpoExecutionStatus && jpoExecutionStatus.equalsIgnoreCase("false") && null != jpoStatusMessage && !jpoStatusMessage.equals(""))
		{
			throw new Exception(jpoStatusMessage);
		}

		if(handledPartObjIdsTable != null && handledPartObjIdsTable.size() > 0)
		{
			Enumeration elements = handledPartObjIdsTable.keys();
			while(elements.hasMoreElements())
			{
				String busId = (String)elements.nextElement();
				String[] partIdOperation = (String[])handledPartObjIdsTable.get(busId);
				String partObjId = partIdOperation[0];
				String operationName = partIdOperation[1];

				Vector partDetails = new Vector();
				if(!operationName.equals(PART_FAILED))
				{
					BusinessObject partObj = new BusinessObject(partObjId);

					partObj.open(_context);
					String partType		= partObj.getTypeName();
					String partName		= partObj.getName();
					String partRev		= partObj.getRevision();
					partObj.close(_context);

					partDetails.add("emxVPLMSynchroStringResource");
					partDetails.add(operationName);
					partDetails.add(partType);
					partDetails.add(partName);
					partDetails.add(partRev);
				}
				else
				{
					//For the failed parts :: Since the part is not created, we know only the name of the part.
					String dummy = "_";

					partDetails.add("emxVPLMSynchroStringResource");
					partDetails.add(operationName);
					partDetails.add(dummy);
					partDetails.add(partObjId);
					partDetails.add(dummy);
				}

				BOMSyncResultTable.put(partObjId, partDetails);
			}
		}
	}
}
