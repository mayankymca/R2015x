/*
**  IEFInstanceDeleteCheck.java
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**	*/
import java.util.Hashtable;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Context;

 
public class IEFInstanceDeleteCheck_mxJPO 
{	
	private MCADServerResourceBundle serverResourceBundle;
	private MCADMxUtil util;
	private String attrName;
	private String typeGlobalConfig;
	public IEFInstanceDeleteCheck_mxJPO (Context context, String[] args) throws Exception
	{
		serverResourceBundle 	=  new MCADServerResourceBundle("");
		util					= new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());
		attrName				= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");
		typeGlobalConfig		= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
	}	

	public void checkDeleteInstance(Context context, String[] args) throws Exception
	{
		String Args[] = new String[2];
		Args[0] = "global";
		Args[1] = "IsFamilyDeleted";
		String result		= util.executeMQL(context, "get env $1 $2",Args);
		
		Args = new String[2];
		Args[0] = "global";
		Args[1] = "MCADINTEGRATION_CONTEXT";
		String resultForRevise		= util.executeMQL(context, "get env $1 $2",Args);
		
		Args = new String[2];
		Args[0] = "global";
		Args[1] = "IsInstancePurged";
		String resultForInstancePurge		= util.executeMQL(context, "get env $1 $2",Args);
		
		if(!result.endsWith("true") && !resultForRevise.endsWith("true") && !resultForInstancePurge.endsWith("true"))
		{
			String busId = args[0];
			//String gcoName = getGlobalConfigObjectName( context, busId);
			//MCADGlobalConfigObject globalConfigObject = getGlobalConfigObject(context, gcoName, typeGlobalConfig, util);
			//String sUser = context.getUser();
			String integrationName				= util.getIntegrationName(context, busId);
			
			String gcoRev     = MCADMxUtil.getConfigObjectRevision(context);

			StringBuffer sbKey = new StringBuffer(100);
			sbKey.append(integrationName);
			sbKey.append("|");
			sbKey.append(gcoRev);
			String sKey = sbKey.toString();				
	
			
			//MCADGlobalConfigObject globalConfig = getGlobalConfigObject(_context, _sObjectID);
			MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject)util.getUserSpecificFullGCODefinition(context,sKey);			
			if(globalConfigObject==null){
					String gcoName = getGlobalConfigObjectName( context, busId);
					globalConfigObject = getGlobalConfigObject(context, gcoName, typeGlobalConfig, util);
			}
			
			DomainObject domObj = new DomainObject();
			domObj.setId(busId);
			domObj.open(context);
			String cadType		= util.getCADTypeForBO(context, domObj);
			
			if(globalConfigObject.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
			{
				MCADServerException.createManagedException("IEF0202200336", serverResourceBundle.getString("mcadIntegration.Server.Message.CantDeleteInstance"),null);
			}	
		}		
	}
	
	
	//We can add following methods in MxMcadUtil
	protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String gcoName, String gcoType, MCADMxUtil mxUtil) throws Exception
	{
		MCADGlobalConfigObject gcoObject	= null;

		if(gcoName != null && gcoName.length() > 0)
		{
			MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(null);
			gcoObject							= configLoader.createGlobalConfigObject(context, mxUtil, gcoType, gcoName);
		}
		return gcoObject;
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

		String rpeUserName = PropertyUtil.getGlobalRPEValue(context,ContextUtil.MX_LOGGED_IN_USER_NAME);

		IEFSimpleConfigObject simpleLCO = IEFSimpleConfigObject.getSimpleLCO(context, rpeUserName);

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
}
