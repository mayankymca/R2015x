/*
**  DECModifyUpdateStamp
**
**  Copyright Dassault Systemes, 1992-2012.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
*/

import java.util.Hashtable;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.util.StringList;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectItr;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADStringUtils;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import java.util.Locale;

public class DECModifyUpdateStamp_mxJPO {

	private boolean isCheckinEx      = false;
	private boolean isCheckin        = false;
	private boolean isAttributeSynch = false;

	private boolean isLockUnlock	 = false;
	private boolean isCheckout	 	 = false;
	private boolean isCheckoutEx	 = false;
	MCADMxUtil mxUtil 				 = null;

	private String attrName							= null;

	private String typeGlobalConfig					= null;
	MCADServerResourceBundle serverResourceBundle	= null;
	IEFGlobalCache globalcache						= null;
	MCADServerGeneralUtil serverGeneralUtil			= null;

	private static Hashtable eventsMappedToGCO = new Hashtable();
	private static Hashtable eventAttributeMap = new Hashtable();

	private Hashtable integrationNameGCOMap = new Hashtable();

	public DECModifyUpdateStamp_mxJPO ()
	{
	}

	static
	{
		eventsMappedToGCO.put("Checkin", MCADAppletServletProtocol.UPDATESTAMP_EVENT_CHECKIN);
		eventsMappedToGCO.put("ChangeOwner", MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION);
		eventsMappedToGCO.put("ChangePolicy", MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION);
		eventsMappedToGCO.put("ChangeVault", MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION);
		eventsMappedToGCO.put("Lock", MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION);
		eventsMappedToGCO.put("Unlock", MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION);
		eventsMappedToGCO.put("Removefile", MCADAppletServletProtocol.UPDATESTAMP_EVENT_CHECKIN);

		eventAttributeMap.put("ChangeOwner", "$$owner$$");
		eventAttributeMap.put("ChangePolicy", "$$policy$$");
		eventAttributeMap.put("ChangeVault", "$$vault$$");
		eventAttributeMap.put("Lock", "$$locker$$");
		eventAttributeMap.put("Unlock", "$$locker$$");
	}

	public DECModifyUpdateStamp_mxJPO (Context context, String args[] ) throws Exception
	{
		String language   = "en-us";
		mxUtil 			  = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

		isCheckin   	  = getRPEValue(context, mxUtil, MCADServerSettings.IEF_CHECKIN, true).equalsIgnoreCase("true");
		isCheckinEx 	  = getRPEValue(context, mxUtil, MCADServerSettings.IEF_CHECKINEX, true).equalsIgnoreCase("true");
		isAttributeSynch  = getRPEValue(context, mxUtil, MCADServerSettings.IEF_ATTR_SYNC, true).equalsIgnoreCase("true");

		isLockUnlock = getRPEValue(context, mxUtil, MCADServerSettings.IEF_LOCK_UNLOCK, true).equalsIgnoreCase("true");

		isCheckout = getRPEValue(context, mxUtil, MCADServerSettings.IEF_CHECKOUT, true).equalsIgnoreCase("true");
		isCheckoutEx = getRPEValue(context, mxUtil, MCADServerSettings.IEF_CHECKOUTEX, true).equalsIgnoreCase("true");

		attrName				= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");
		typeGlobalConfig		= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");

		globalcache			= new IEFGlobalCache();

		serverResourceBundle 	= new MCADServerResourceBundle("");
	}

	public int mxMain(Context context, String []args) throws Exception
	{
		return 0;
	}

	public int modifyUpdateStamp(matrix.db.Context context, String[] args) throws Exception
	{	
		if(!(isCheckin || isCheckinEx || isAttributeSynch || isLockUnlock || isCheckout || isCheckoutEx))
		{
			String event	 = args[0];
			String objectId  = args[1];
			String sKindOfChng  = args[2];

			if(sKindOfChng != null && !sKindOfChng.equalsIgnoreCase("null")) {
		 		if (("owner").equalsIgnoreCase(sKindOfChng)) {
			updateOwnerInfo(context, event, objectId);
				}
			}
			if(eventsMappedToGCO.containsKey(event))
			{
				String mappedGCOEvent  = (String) eventsMappedToGCO.get(event);
				String integrationName =  mxUtil.getIntegrationName(context, objectId);
				if(integrationName != null && integrationName.contains("DENIED")){
					return 0;
				} else {
				if(!MCADStringUtils.isNullOrEmpty(integrationName))
				{
					MCADGlobalConfigObject globalConfigObj		= getGlobalConfigObject(context, integrationName, mxUtil);
						if(globalConfigObj!=null){
					if(!MCADStringUtils.isNullOrEmpty(mappedGCOEvent) && globalConfigObj.isModificationEvent(mappedGCOEvent))
					{
						BusinessObject busObject = new BusinessObject(objectId);
						busObject.open(context, false);
						
						String cadType = mxUtil.getCADTypeForBO(context, busObject);
						String mxType = busObject.getTypeName();
						
						busObject.close(context);
						
						// [NDM] OP6
						/*if(!globalConfigObj.isMajorType(mxType))
							mxType = mxUtil.getCorrespondingType(context, mxType);*/

						if(event.equalsIgnoreCase("ChangeOwner") || event.equalsIgnoreCase("ChangePolicy")
								|| event.equalsIgnoreCase("ChangeVault") || event.equalsIgnoreCase("Lock") || 
								event.equalsIgnoreCase("Unlock"))
						{	
							Vector attr =  globalConfigObj.getCADAttribute(mxType, (String)eventAttributeMap.get(event), cadType);

							if(!attr.isEmpty())
								mxUtil.modifyUpdateStamp(context, objectId);
						}
						else if(event.equalsIgnoreCase("checkin") || event.equalsIgnoreCase("removefile"))
						{
							String format  = this.getRPEValue(context, mxUtil, "FORMAT", false);

							String primaryFormat = globalConfigObj.getFormatsForType(mxType, cadType);

							if(primaryFormat.equals(format))
								mxUtil.modifyUpdateStamp(context, objectId);
						}
					}
				}
			}
		}
		}
		}

		return 0;
	}

	private String getGlobalConfigObjectName(Context context, String integrationName) throws Exception
	{
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

	protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String integrationName, MCADMxUtil mxUtil) throws Exception
	{
		MCADGlobalConfigObject gcoObject	= null;

		if(!integrationNameGCOMap.containsKey(integrationName))
		{
			String gcoName = this.getGlobalConfigObjectName(context, integrationName);			
			if(gcoName == null && MCADMxUtil.isSolutionBasedEnvironment(context))
			{
				IEFGetRegistrationDetails_mxJPO registrationDetailsReader = new IEFGetRegistrationDetails_mxJPO(context, null);
				String args[] = new String[1];
				args[0] = integrationName;
				String registrationDetails = registrationDetailsReader.getRegistrationDetails(context, args);
				
				gcoName 	           = registrationDetails.substring(registrationDetails.lastIndexOf("|")+1);
			}

				MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(null);
				gcoObject							= configLoader.createGlobalConfigObject(context, mxUtil, this.typeGlobalConfig, gcoName);

			integrationNameGCOMap.put(integrationName, gcoObject);
		}
		else 
			gcoObject = (MCADGlobalConfigObject) integrationNameGCOMap.get(integrationName);


		return gcoObject;
	}

	private String getRPEValue(Context context, MCADMxUtil mxUtil,  String variableName, boolean isGlobal)
	{
		String sResult = "";
		String Args[] = new String[2];
		Args[0] = "global";
		Args[1] = variableName;
		if(isGlobal)
			sResult = mxUtil.executeMQL(context,"get env $1 $2",Args);
		else
		{
			Args= new String[1];
			Args[0] = variableName;
			sResult = mxUtil.executeMQL(context, "get env $1", Args);
		}
		String result	= "";

		if(sResult.startsWith("true"))
		{
			result = sResult.substring(sResult.indexOf("|")+1, sResult.length());
		}

		return result;
	}

	private void updateOwnerInfo(Context context, String event, String objectId) throws Exception
	{
		boolean isPushed    = false;
		try
		{
			ContextUtil.pushContext(context);
			 isPushed = true;
			if(objectId != null && event.equalsIgnoreCase("ChangeOwner"))
			{					
				if(mxUtil.isMajorObject(context,objectId))
				{
					BusinessObject majorObj = new BusinessObject(objectId);
					BusinessObject activeMinorObj = mxUtil.getActiveMinor(context,majorObj);
					mxUtil.executeMQL(context, "set env DECIgnoreChangeOwnerCheck true");	
					//update attributes on minor object
					mxUtil.updateOwnerInfo(context,majorObj,activeMinorObj);
					mxUtil.executeMQL(context, "unset env DECIgnoreChangeOwnerCheck");

					//update attributes on derived output
					updateAllDerivedOutputObjects(context,majorObj);

					
					
					String integrationName =  mxUtil.getIntegrationName(context, objectId);
					if(integrationName != null && integrationName.contains("DENIED")){
						return ;
					} else {
					if(!MCADStringUtils.isNullOrEmpty(integrationName))
					{
						MCADGlobalConfigObject globalConfigObj		= getGlobalConfigObject(context, integrationName, mxUtil);
					
					if(globalConfigObj == null)
					    return;
					
					DomainObject boTemp = DomainObject.newInstance(context, objectId);
					String cad_type = mxUtil.getCADTypeForBO(context, boTemp);					
					
					boolean isFamilyLike=globalConfigObj.isTypeOfClass(cad_type,MCADAppletServletProtocol.TYPE_FAMILY_LIKE);
					
					if(isFamilyLike){
					propagateChangeOwnershipToInstances(context, majorObj);
					}
				}
				
					}
				}
			}
		}
		catch (Exception ex)
		{
			String language = context.getLocale().getLanguage();
			String message =EnoviaResourceBundle.getProperty(context,"iefStringResource",new Locale(language),"mcadIntegration.Server.Message.ErrorWhilePropagatingChangeOwner");
			MCADServerException.createException(message+" : "+ex.getMessage(), ex);
		}
		finally
		{
			if(isPushed)
			{ 
				try
				{
					ContextUtil.popContext(context);
				}
				catch(Exception ex)
				{
					MCADServerException.createException(ex.getMessage(), ex);
				}
	        }
	   }
	}	
	private void updateAllDerivedOutputObjects(Context context, BusinessObject bus)throws Exception
	{
		
		String	derivedOutputRel	= MCADMxUtil.getActualNameForAEFData(context, "relationship_DerivedOutput");
		String	viewableRel			= MCADMxUtil.getActualNameForAEFData(context, "relationship_Viewable");
		
		ArrayList	relList	= new ArrayList();
		relList.add(viewableRel);
		relList.add(derivedOutputRel);

		java.util.List<BusinessObjectList> tempListInList = new java.util.ArrayList<BusinessObjectList>();

		Iterator<String> relItr	= relList.iterator();
		while(relItr.hasNext())
		{
			String	relName	= (String)relItr.next();
			tempListInList.add(mxUtil.getRelatedBusinessObjects(context, bus, relName, "from"));
		}

		if(null != tempListInList && tempListInList.size() > 0)
		{
			Iterator busObjItr = tempListInList.iterator();
			while(busObjItr.hasNext())
			{
				BusinessObjectList	tempList	= (BusinessObjectList)busObjItr.next();
				if(null != tempList && tempList.size() > 0)
				{
					BusinessObjectItr tempObjItr = new BusinessObjectItr(tempList);			
					while(tempObjItr.next())
					{
						BusinessObject dpBusObj = (BusinessObject)tempObjItr.obj();
						mxUtil.updateOwnerInfo(context,bus,dpBusObj);
					}
				}
			}
		}		
	}
	
	public void propagateChangeOwnershipToInstances(Context context, BusinessObject fromBusObject) throws Exception
	{	

		try {
			
			String objectId  = fromBusObject.getObjectId();		
			StringList objectSelects = new StringList(1);
			objectSelects.addElement(DomainConstants.SELECT_ID);
			DomainObject boTemp = DomainObject.newInstance(context, objectId);			
			String instanceOfRelName 	= MCADMxUtil.getActualNameForAEFData(context, "relationship_InstanceOf");
			
			StringBuffer stbRelPattern = new StringBuffer(50);
			stbRelPattern.append(instanceOfRelName);		
			MapList mlConnectedData = boTemp.getRelatedObjects(context,stbRelPattern.toString(),
					"*", objectSelects, null,false, true, (short)1, null, null, 0);
				
			String owner  = fromBusObject.getOwner(context).getName();
			String org 	= fromBusObject.getOrganizationOwner(context).getName();
			String prjOwn = fromBusObject.getProjectOwner(context).getName();
			String command = "modify bus $1 project $2 organization $3 owner $4";
			
			for(int h=0;h<mlConnectedData.size();h++){
				Map mpEach = (Map) mlConnectedData.get(h);
				String idEach = (String)mpEach.get(DomainConstants.SELECT_ID);
				MqlUtil.mqlCommand(context, command, idEach, prjOwn,org,owner);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}	
	
		return;
	}
}

