/**
 ** IEFPromoteIfDesignCenterCommand.java
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such programimport java.util.*;
 **
 **/

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.Environment;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerLogger;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFPromoteDemoteHelper;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADLogger;
import com.matrixone.MCADIntegration.utils.MCADXMLUtils;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import java.util.Iterator;

public class IEFPromoteIfDesignCenterCommand_mxJPO
{
	private  HashSet operatedObjectIds				= null;
	private String REL_VERSION_OF					= null;
	private String typeGlobalConfig					= null;
	private String typeLocalConfig					= null;
	private String attrName							= null;

	private MCADMxUtil	util								= null;
	private MCADServerResourceBundle serverResourceBundle	= null;
	private IEFGlobalCache globalcache						= null;
	private MCADGlobalConfigObject globalConfigObj			= null;
	private MCADLocalConfigObject localConfigObject		= null;
	private MCADServerGeneralUtil serverGeneralUtil			= null;
	private String integrationName							= null;
	private MCADServerLogger logger							= null;
	private boolean isDebugOn								= false;
	FileOutputStream logOutputStream						= null;
	private BusinessObject _busObject                               = null;

	/**
	 * The no-argument constructor.
	 */
	public IEFPromoteIfDesignCenterCommand_mxJPO()
	{

	}

	/**
	 * Constructor which accepts the Matrix context and an array of String
	 * arguments.
	 */
	public IEFPromoteIfDesignCenterCommand_mxJPO(Context context, String[] args) throws Exception
	{
		if(isDebugOn)
			logger				= intiateLogger(context);

		operatedObjectIds	= new HashSet();
	
		REL_VERSION_OF			= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		typeGlobalConfig		= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
		typeLocalConfig			= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-LocalConfig");
		
		attrName				= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");
	}

	public int mxMain(Context context, String []args)  throws Exception
	{		
		if(isDebugOn)
			logger.logDebug("[IEFPromoteIfDesignCenterCommand] operatedObjectIds : " + operatedObjectIds);
		
		String objectId = args[0];
		String event	= args[1];

		globalcache				= new IEFGlobalCache();
		serverResourceBundle 	= new MCADServerResourceBundle(context.getSession().getLanguage());
		util					= new MCADMxUtil(context, serverResourceBundle, globalcache);
		
		String gcoName		= this.getGlobalConfigObjectName(context, objectId);
		String lcoName		= this.getLocalConfigObjectName(context, objectId);
		
		globalConfigObj		= getGlobalConfigObject(context, gcoName, typeGlobalConfig ,util);
		localConfigObject	= getLocalConfigObject(context, lcoName, typeLocalConfig);
		serverGeneralUtil	= new MCADServerGeneralUtil(context, globalConfigObj, serverResourceBundle, globalcache);

		String Args[] = new String[1];
		Args[0] = "IsDesignCenterCommand";
		String result		= util.executeMQL(context, "get env $1", Args);

		int returnValue = 1;
		if(operatedObjectIds.contains(objectId))
			returnValue = 0;
		else if(result.endsWith("true"))
			returnValue  = 0;
		else if(!result.endsWith("true"))
		{
			BusinessObject checkObject	= new BusinessObject(objectId);
			checkObject.open(context);
			String sOwnerName = checkObject.getOwner().getName();
			String strContextUser = context.getUser();
			String userRole = context.getRole();
			String vplmProjectLeader	= MCADMxUtil.getActualNameForAEFData(context, "role_VPLMProjectLeader");
			String boName = checkObject.getName();
			String incomingType =	checkObject.getTypeName();
			checkObject.close(context);
			
			if(event.equals("Promote") && !strContextUser.equals(sOwnerName) && !userRole.contains(vplmProjectLeader))
			{
				Hashtable messageTokens = new Hashtable(2);
				messageTokens.put("PARENT_NAME", boName);
				messageTokens.put("USER_NAME", sOwnerName);
				MCADServerException.createException(serverResourceBundle.getString("mcadIntegration.Server.Message.OwnedByOtherUserNORLoggedAsLeader", messageTokens), null);
			}
			
			if(globalConfigObj.isTemplateType(incomingType))
			{
				MCADServerException.createException(serverResourceBundle.getString("mcadIntegration.Server.Message.InvalidTypeForOperation"), null);
			}

//[NDM] H68: finalization logic needs to remove AND value of isFinalized passed here in executePromote is not at all read in the method		
		boolean isFinalized = serverGeneralUtil.isBusObjectFinalized(context, objectId);

				if(event.equals("Promote") && !operatedObjectIds.contains(objectId))
				{
					if(isDebugOn)
						logger.logDebug("[IEFPromoteIfDesignCenterCommand] objectId : " + objectId);
					
						
					IEFPromoteDemoteHelper promoteDemoteHelper = new IEFPromoteDemoteHelper();
					Hashtable JPOResultData = promoteDemoteHelper.executePromote(context, globalConfigObj, localConfigObject, serverGeneralUtil, util, serverResourceBundle, integrationName, objectId, logger);

					if(isDebugOn)
						logger.logDebug("[IEFPromoteIfDesignCenterCommand] JPOResultData : " + JPOResultData);
					
					String resultJPO = (String)JPOResultData.get(MCADServerSettings.JPO_EXECUTION_STATUS);

					if (resultJPO.equalsIgnoreCase("true"))
					{
						IEFXmlNode finalizedPacket	= (IEFXmlNode)JPOResultData.get(MCADServerSettings.SELECTED_OBJECTID_LIST);
						IEFXmlNode parentNode = MCADXMLUtils.getChildNodeWithName(finalizedPacket, "structure");
						if(isDebugOn)
							logger.logDebug("[IEFPromoteIfDesignCenterCommand] parentNode : " + parentNode.getXmlString());
						updateOperatedObjectIdsMap(parentNode);
						
						returnValue  = 0;
					}
					else
					{
						String error = (String)JPOResultData.get(MCADServerSettings.JPO_STATUS_MESSAGE);
						MCADServerException.createException(error, null);
					}
				}
				else if((event.equals("Demote")))
			{
					String cadType								= MCADMxUtil.getActualNameForAEFData(context,"attribute_CADType");			
					String ATTR_CAD_TYPE						= "attribute[" + cadType + "]";

					StringList selectList						= new StringList();
					selectList.add("current");
					selectList.add(ATTR_CAD_TYPE);

					BusinessObjectWithSelectList busWithSelectList	= BusinessObject.getSelectBusinessObjectData(context, new String[]{objectId}, selectList);

					BusinessObjectWithSelect busWithSelect     		= busWithSelectList.getElement(0);

					String cadTypeVal							= busWithSelect.getSelectData(ATTR_CAD_TYPE);
					String currentState							= busWithSelect.getSelectData("current");	

					if(globalConfigObj.isTypeOfClass(cadTypeVal, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
					{
						operatedObjectIds.add(objectId);
						BusinessObject instObject	= new BusinessObject(objectId);
						if(!isFinalized)
						{
							BusinessObject instMinorObj  = util.getActiveMinor(context, instObject);
							instObject				= instMinorObj;
						}

						BusinessObject famObj = serverGeneralUtil.getFamilyObjectForInstance(context, instObject);
						String famObjId		  = famObj.getObjectId(context);

						String [] oids = new String[1];
						oids[0] = famObjId;

						ArrayList connectedObjectIdList = serverGeneralUtil.getFamilyStructureRecursively(context, oids, new Hashtable(),null);

						lockCheck(context,famObjId);
						Iterator<String> itr = connectedObjectIdList.iterator();
						while(itr.hasNext())
						{
							lockCheck(context,itr.next());
						}
						
						connectedObjectIdList.add(famObjId);

						promoteDemoteConnectedObjects(context, connectedObjectIdList, globalConfigObj, util, event, currentState);
					}
					else if(globalConfigObj.isTypeOfClass(cadTypeVal, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
					{
						operatedObjectIds.add(objectId);
						String famObjId = objectId;
						if(!isFinalized)
						{
							BusinessObject famObj = util.getActiveMinor(context, new BusinessObject(objectId));
							famObjId				= famObj.getObjectId(context);
						}

						String [] oids = new String[1];
						oids[0] = famObjId;
						ArrayList connectedObjectIdList = serverGeneralUtil.getFamilyStructureRecursively(context, oids, new Hashtable(),null);

						lockCheck(context,famObjId);
						Iterator<String> itr = connectedObjectIdList.iterator();
						while(itr.hasNext())
						{
							lockCheck(context,itr.next());
					}

						promoteDemoteConnectedObjects(context, connectedObjectIdList, globalConfigObj, util, event, currentState);
			}
			else
			{
						lockCheck(context,objectId);

			}

					returnValue  = 0;
			
		}
	}

		util.unsetRPEVariablesForIEFOperation(context, "Finalize");
		closeLogStream(logOutputStream);
		return returnValue;
	}

	private MCADServerLogger intiateLogger(Context context)
    {
		MCADServerLogger logger = null;
        try
		{
			logger = new MCADServerLogger("IEFPromoteIfDesignCenterCommand Trigger program");
			logger.setMessageLevel(MCADLogger.GENERAL);
        	logger.setPrintMessageLevel(MCADLogger.GENERAL);
        	String filePathDir = Environment.getValue(context, "MX_TRACE_FILE_PATH");
    		java.io.File logDir = new java.io.File(filePathDir);
        	if(!logDir.exists())
        		logDir.mkdir();

        	String logsDirectoryName = logDir.getAbsolutePath();

        	java.io.File logFile = new java.io.File(logsDirectoryName, context.getUser() + (new Date()).getTime() + "_IEFSilentOperation.log");
        	logOutputStream      = new FileOutputStream(logFile);
        	OutputStreamWriter logOutputStreamWriter    = new OutputStreamWriter(logOutputStream);
		
        	logger.setLoggerOutputStreamWriter(logOutputStreamWriter);
            
            logger.setLogFile(logFile);
	    }
	    catch(Exception e)
		{
		    System.out.println("Error while initiating the logger. Error:" + e.getMessage());
		}
		return logger;
    }

	private void closeLogStream(FileOutputStream logOutputStream)
    {
		try
		{	
		    if(logOutputStream != null)
		    {
		        logOutputStream.flush();
	            logOutputStream.close();
	        }
		}
		catch(Exception e)
		{
		    System.out.println("Error during session timeout. Error:" + e.getMessage());
		}
	}


	protected void updateOperatedObjectIdsMap(IEFXmlNode parentNode)
	{
		Enumeration childNodes = parentNode.elements();

		while (childNodes.hasMoreElements())
		{
			IEFXmlNode childNode = (IEFXmlNode) childNodes.nextElement();

			String oid 	 = childNode.getAttribute("majorobjectid");
			operatedObjectIds.add(oid);
			updateOperatedObjectIdsMap(childNode);
		}
		
	}

	private void promoteDemoteConnectedObjects(Context context, ArrayList connectedObjectIdList, MCADGlobalConfigObject globalConfigObj, MCADMxUtil util, String event, String currentState) throws Exception
	{
		String MAJOR_BUS_ID			= "";
		String MAJOR_BUS_STATE		= "";
		String objId				= "";
		String type					= "";

		StringBuffer majorBuffer = new StringBuffer();
		majorBuffer.append("from[");
		majorBuffer.append(REL_VERSION_OF);

		StringBuffer majorIdBuffer = new StringBuffer(majorBuffer.toString());
		majorIdBuffer.append("].to.id");
		MAJOR_BUS_ID       = majorIdBuffer.toString();

		StringBuffer majorIdStateBuffer = new StringBuffer(majorBuffer.toString());
		majorIdStateBuffer.append("].to.current");
		MAJOR_BUS_STATE       = majorIdStateBuffer.toString();

		String []  oids = new String[connectedObjectIdList.size()];
		connectedObjectIdList.toArray(oids);

		StringList busSelects = new StringList();

		busSelects.add("id");
		busSelects.add("current");
		busSelects.add("type");
		busSelects.add(MAJOR_BUS_ID);
		busSelects.add(MAJOR_BUS_STATE);

		BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, oids, busSelects);
		for(int i = 0; i < busWithSelectList.size(); i++)
		{
			BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(i);
			objId								   = busWithSelect.getSelectData("id");
			type								   = busWithSelect.getSelectData("type");		

			BusinessObject object		= new BusinessObject(objId);
			BusinessObject majorObject	= object;

			String objState			= busWithSelect.getSelectData("current");

			if(!util.isMajorObject(context, objId))//!globalConfigObj.isMajorType(type)) // [NDM] OP6
			{
				objId			= busWithSelect.getSelectData(MAJOR_BUS_ID);
				majorObject		= new BusinessObject(objId);
				objState		= busWithSelect.getSelectData(MAJOR_BUS_STATE);
			}

			if(!operatedObjectIds.contains(objId))
			{
				if(event.equals("Promote") && objState.equals(currentState))
					majorObject.promote(context);
				else if(event.equals("Demote") && objState.equals(currentState))
					majorObject.demote(context);

				operatedObjectIds.add(objId);
			}

			if(globalConfigObj.isModificationEvent(MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION))
			{
				majorObject.open(context);
				String majorCadType = util.getCADTypeForBO(context, majorObject);
				String mxType = majorObject.getTypeName();
				majorObject.close(context);

				Vector attr =  globalConfigObj.getCADAttribute(mxType, "$$current$$", majorCadType);

				if(!attr.isEmpty())	
					util.modifyUpdateStamp(context, majorObject.getObjectId());
			}
		}			
	}

	private String getGlobalConfigObjectName(Context context, String busId) throws Exception
	{
		// Get the IntegrationName
		IEFGuessIntegrationContext_mxJPO guessIntegration = new IEFGuessIntegrationContext_mxJPO(context, null);
		String jpoArgs[] = new String[1];
		jpoArgs[0] = busId;
		String integrationName = guessIntegration.getIntegrationName(context, jpoArgs);
		this.integrationName = integrationName;
		// Get the relevant GCO Name 
		String gcoName = null;

		String rpeUserName = PropertyUtil.getGlobalRPEValue(context,ContextUtil.MX_LOGGED_IN_USER_NAME);

		IEFSimpleConfigObject simpleLCO = IEFSimpleConfigObject.getSimpleLCO(context, rpeUserName);

		if(simpleLCO.isObjectExists())
		{
			Hashtable integNameGcoMapping = simpleLCO.getAttributeAsHashtable(attrName, "\n", "|");
			gcoName = (String)integNameGcoMapping.get(integrationName);	        
   
			if(gcoName == null || gcoName.equals(""))
			{
				Hashtable exceptionDetails = new Hashtable(1);
				exceptionDetails.put("INTEGRATION", integrationName);
				String errorCode = MCADAppletServletProtocol.DEFAULT_ERROR_CODE;
				String messageString = serverResourceBundle.getString("mcadIntegration.Server.Message.NoAccessToUserForIntegration", exceptionDetails);
				MCADServerException.createManagedException(errorCode, messageString, null);
			}
			
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
	private String getLocalConfigObjectName(Context context, String busId) throws Exception
	{
		// Get the IntegrationName
		IEFGuessIntegrationContext_mxJPO guessIntegration = new IEFGuessIntegrationContext_mxJPO(context, null);
		String jpoArgs[] = new String[1];
		jpoArgs[0] = busId;
		String integrationName = guessIntegration.getIntegrationName(context, jpoArgs);
		this.integrationName = integrationName;
		
		String rpeUserName = PropertyUtil.getGlobalRPEValue(context,ContextUtil.MX_LOGGED_IN_USER_NAME);

		IEFSimpleConfigObject simpleLCO = IEFSimpleConfigObject.getSimpleLCO(context, rpeUserName);
		return simpleLCO.getName();
	}

	protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String gcoName, String gcoType, MCADMxUtil mxUtil) throws Exception
	{
		MCADGlobalConfigObject gcoObject	= null;

		if(gcoName != null && gcoName.length() > 0)
		{
			MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(logger);
			gcoObject							= configLoader.createGlobalConfigObject(context, mxUtil, gcoType, gcoName);
		}
		return gcoObject;
	}

	protected MCADLocalConfigObject getLocalConfigObject(Context context, String lcoName, String lcoType) throws Exception
	{
		String lcoRev	= MCADMxUtil.getConfigObjectRevision(context);
		
		MCADLocalConfigObject lcoObject	= null;

		if(lcoName != null && lcoName.length() > 0)
		{
			MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(logger);
			lcoObject							= configLoader.createLocalConfigObject(lcoType, lcoName, lcoRev, context);
		}
		return lcoObject;
	}
	
	private void lockCheck(Context context, String objectId) throws Exception
	{
	      try
	      {					
                         if(!util.isRoleAssigned(context,"role_VPLMAdmin"))
			{
							_busObject	= new BusinessObject(objectId);
		
							if (_busObject == null)
							{
								MCADServerException.createException(serverResourceBundle.getString("mcadIntegration.Server.Message.BusinessObjectNotFound"), null);
							}
							
							//Lock not to be checked for Admin
			
								if(util.isMajorObject(context, objectId))
								{
                                        _busObject.open(context);
					if (!serverGeneralUtil.checkLockStatus(context, _busObject))
									{
										Hashtable exceptionDetails = new Hashtable(4);
										exceptionDetails.put("TYPE",_busObject.getTypeName());
										exceptionDetails.put("NAME",_busObject.getName());
										exceptionDetails.put("REVISION",_busObject.getRevision());
										exceptionDetails.put("LOCKER",_busObject.getLocker(context).getName());
										
										MCADServerException.createException(serverResourceBundle.getString("mcadIntegration.Server.Message.FinalizedBusObjLocked",exceptionDetails), null);
									}
                                        _busObject.close(context);
								
								}
								else
								{
									MCADServerException.createException(serverResourceBundle.getString("mcadIntegration.Server.Message.UndoFinalizeNotValidForMinorTypes"), null);
								}
			
			

							}
						}
						catch(Exception e)
						{
							MCADServerException.createException(serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToUndoFinalize") + e.getMessage(), e);
						}
	}
}
