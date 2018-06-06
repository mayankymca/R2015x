/*
 **  IEFActionAccess
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to check action access
 */

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.ResourceBundle;
import java.util.PropertyResourceBundle;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCMessage;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCQueue;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.IEFLicenseUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADStringBufferUtils;
import com.matrixone.MCADIntegration.utils.MCADStringUtils;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

/**
 * The <code>emxENCActionLinkAcess</code> class contains code for the "Action Link".
 *
 * @version EC 10.5 - Copyright Dassault Systemes, 1992-2007.All Rights Reserved.
 */
public class IEFActionAccess_mxJPO
{
	/**
	 * Constructor.
	 *
	 * @param context the eMatrix <code>Context</code> object.
	 * @param args holds no arguments.
	 * @throws Exception if the operation fails.
	 * @since EC 10.5
	 */
	public IEFActionAccess_mxJPO (Context context, String[] args)
	throws Exception
	{
	}

	/**
	 * This is overridden by IEF. For IEF types do not show revise 
	 * with files option. For minors even revise should be restricted.
	 *
	 * @param context the eMatrix <code>Context</code> object.
	 * @param args holds objectId.
	 * @return Boolean.
	 * @throws Exception If the operation fails.
	 * @since EC10-5.
	 *
	 */

	public Boolean  canRevise(Context context,String[] args) throws Exception
	{
		boolean isTypeAccessable = true;
		try
		{
			if(isTypeAccessable)
			{
				HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
				String objectId		= (String) paramMap.get("objectId");
				String language		= (String) paramMap.get("languageStr");

				// [NDM] QWJ
				/*BusinessObject bus = new BusinessObject(objectId);
				bus.open(context);
				String type = bus.getTypeName();
				bus.close(context);*/
				if(isIEFType(context, objectId) && (isMinorObject(context, objectId, language) || !hasIntegrationAccess(context, objectId, language)))
				{
					isTypeAccessable = false;
				}

				if(!hasLicenseAssigned(context, objectId, language))
				{
					isTypeAccessable = false;
				}

			}
		}
		catch(Throwable e)
		{
		}

		return new Boolean(isTypeAccessable);
	}

	public Boolean  canDelete(Context context,String[] args) throws Exception
	{
		boolean isTypeAccessable = true;
		try
		{
			if(isTypeAccessable)
			{
				HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
				String objectId		= (String) paramMap.get("objectId");
				String language		= (String) paramMap.get("languageStr");

				// [NDM] QWJ
				/*BusinessObject bus = new BusinessObject(objectId);
				bus.open(context);
				String type = bus.getTypeName();
				bus.close(context);*/
				if(isIEFType(context, objectId) && (isMinorObject(context, objectId, language) || !hasIntegrationAccess(context, objectId, language)))
				{
					isTypeAccessable = false;
				}
				if(!hasLicenseAssigned(context, objectId, language))
				{
					isTypeAccessable = false;
				}
			}
		}
		catch(Throwable e)
		{
		}

		return new Boolean(isTypeAccessable);
	}

	private boolean isIEFType(Context context, String objectId)
	{
		try
		{
			String typeMCADModel 			= MCADMxUtil.getActualNameForAEFData(context, "type_MCADModel");
			String typeECADModel 			= MCADMxUtil.getActualNameForAEFData(context, "type_ECADModel");
			String typeMCADDrawing 		= MCADMxUtil.getActualNameForAEFData(context, "type_MCADDrawing");
			String typeMCADVersionedDrawing  	= MCADMxUtil.getActualNameForAEFData(context, "type_MCADVersionedDrawing");

			DomainObject ddd = new DomainObject(objectId);

			if(ddd.isKindOf(context, typeMCADModel) || ddd.isKindOf(context, typeECADModel) || ddd.isKindOf(context, typeMCADDrawing) || ddd.isKindOf(context, typeMCADVersionedDrawing))
				return true;
		}
		catch(Throwable e)
		{
		}

		return false;
	}

	// [NDM] QWJ
	private boolean isMinorObject(Context context, String objectId, String language)
	{
		boolean isMinor = false;
		// [NDM] QWJ Start
		//try
		//{
			MCADMxUtil util			= new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
		isMinor 		= !util.isMajorObject(context, objectId);
		/*String integrationName  = util.getIntegrationName(context, objectId);

			String attrBusTypeMapping		= MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-BusTypeMapping");
			IEFSimpleConfigObject simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, integrationName);
			Hashtable typeMapping			= simpleGCO.getAttributeAsHashtable(attrBusTypeMapping, "\n", "|");

			Vector mxTypes = new Vector();

			Enumeration enumTypeMapping = typeMapping.elements() ;
			while(enumTypeMapping.hasMoreElements())
			{
				String sTypes = (String)enumTypeMapping.nextElement();
				StringTokenizer tokenizer = new StringTokenizer(sTypes, ",");
				while(tokenizer.hasMoreElements())
				{
					String sType   = (String) tokenizer.nextElement();
					mxTypes.addElement(sType.trim());
				}			
			}

			if(!mxTypes.contains(type))
				isMinor = true;*/
		//}
		//catch(Throwable e)
		//{
		//}
		// [NDM] QWJ End

		return isMinor;
	}

	public Boolean canShowChangeOwnerCommand(Context context,String[] args) throws Exception
	{		
		boolean isShowCommand = false;
		
		try
		{
			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
			String objectId		= (String) paramMap.get("objectId");
			String language		= (String) paramMap.get("languageStr");

			 if ((MCADStringUtils.isNullOrEmpty(objectId) || !isMinorObject(context, objectId, language)))
					  isShowCommand = true;
		}
		catch(Exception e)
		{
				
		}		 
		return new Boolean(isShowCommand);		
	}

	private boolean hasIntegrationAccess(Context context,String objectId, String language)
	{
		boolean hasIntegrationAccess = true;
		try
		{
			IEFIntegAccessUtil util		= new IEFIntegAccessUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
			String integrationName  = util.getIntegrationName(context, objectId);

			if(!util.getAssignedIntegrations(context).contains(integrationName))
				hasIntegrationAccess = false;
		}
		catch(Throwable e)
		{
		}

		return hasIntegrationAccess;
	}

	public Boolean canShowLifecycleCommandForIntegUser(Context context,String[] args) throws Exception
	{
		boolean isShowCommand = true;
		try
		{
					HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
					String objectId		= (String) paramMap.get("objectId");
					String language		= (String) paramMap.get("languageStr");					
					if(!hasIntegrationAccess(context, objectId, language) || isMinorObject(context, objectId, language))
					{
						isShowCommand = false;
					}
		}
		catch(Throwable e)
		{
		}

		return new Boolean(isShowCommand);
	}

	public Boolean canShowCommandForIntegUser(Context context,String[] args) throws Exception
	{
		boolean isShowCommand = true;
		try
		{
			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
			String objectId		= (String) paramMap.get("objectId");
			String language		= (String) paramMap.get("languageStr");

			if(!hasIntegrationAccess(context, objectId, language))
			{
				isShowCommand = false;
			}
		}
		catch(Throwable e)
		{
		}

		return new Boolean(isShowCommand);
	}

	public Boolean canShowCommandForIntegUserForFrameworkTable(Context context,String[] args) throws Exception
	{
		boolean isShowCommand = true;
		try
		{
			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
			String objectId		= (String) paramMap.get("objectId");

			if(null == objectId || "".equals(objectId))
				objectId		= (String)paramMap.get("inputObjId");

			String language		= (String) paramMap.get("languageStr");

			if(!hasIntegrationAccess(context, objectId, language))
			{
				isShowCommand = false;
			}
		}
		catch(Throwable e)
		{
		}

		return new Boolean(isShowCommand);
	}

	public Boolean isNotIntegrationUser(Context context,String[] args) throws Exception
	{
		boolean isShowCommand = false;
		try
		{
			String roleIntegrationUser	= MCADMxUtil.getActualNameForAEFData(context, "role_HarnessUser");
			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
			String objectId		= (String) paramMap.get("objectId");
			String language		= (String) paramMap.get("languageStr");
			boolean isAssigned  = context.isAssigned(roleIntegrationUser);

			if(!isAssigned || !hasIntegrationAccess(context, objectId, language))
			{
				isShowCommand = true;
			}
		}
		catch(Throwable e)
		{
		}

		return new Boolean(isShowCommand);
	}

	public Boolean isIntegrationObject(Context context,String[] args)
	{
		boolean isIntegrationObject = true;
		try
		{
			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
			String objectId		= (String) paramMap.get("objectId");
			String language		= (String) paramMap.get("languageStr");

			MCADMxUtil util			= new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
			String integrationName  = util.getIntegrationName(context, objectId);

			if(integrationName.equals(""))
				isIntegrationObject = false;
		}
		catch(Throwable e)
		{
		}

		return new Boolean(isIntegrationObject);
	}

	public Boolean canResubmit(Context context,String[] args)
	{
		boolean canResubmit = false;
		try
		{ 

			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
			String objectId		= (String) paramMap.get("objectId");
			String language		= (String) paramMap.get("languageStr");

			MCADMxUtil util		= new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

			if(util.getCurrentState(context, objectId).equals("Completed"))
				canResubmit = true;
		}
		catch(Throwable e)
		{
		}


		return new Boolean(canResubmit);
	}

	public Boolean isPeriodicMessage(Context context,String[] args)
	{
		boolean isPeriodicMessage = false;
		try
		{ 

			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
			String objectId		= (String) paramMap.get("objectId");

			DSCMessage message						 = new DSCMessage(context, objectId);
			message.open(context);

			DSCQueue queue	 = message.getRelatedQueue(context);
			queue.open(context);

			isPeriodicMessage = queue.isPeriodicMessage(context,objectId);

			queue.close(context);

			message.close(context);


		}
		catch(Throwable e)
		{
		}


		return new Boolean(isPeriodicMessage);
	}

	public Boolean canShowWhereUsed(Context context,String[] args)
	{
		boolean canShowWhereUsed = true;
		try
		{ 
			if(isIntegrationObject(context, args).equals(Boolean.FALSE) || isFamilylike(context, args).equals(Boolean.TRUE))
				canShowWhereUsed = false;
		}
		catch(Throwable e)
		{
		}

		return new Boolean(canShowWhereUsed);
	}

	public Boolean isFamilylike(Context context, String []args)  throws Exception
	{
		HashMap paramMap								 = (HashMap)JPO.unpackArgs(args);
		String objectId									 = (String) paramMap.get("objectId");
		String language									 = (String) paramMap.get("languageStr");

		MCADServerResourceBundle serverResourceBundle	 = new MCADServerResourceBundle(language);
		IEFGlobalCache cache							 = new IEFGlobalCache();
		IEFIntegAccessUtil mxUtil				 = new IEFIntegAccessUtil(context, serverResourceBundle, cache);

		String integrationName							 = mxUtil.getIntegrationName(context, objectId);

		IEFSimpleConfigObject simpleGCO = null;

		if(mxUtil.getUnassignedIntegrations(context).contains(integrationName))
			simpleGCO = IEFSimpleConfigObject.getSimpleGCOForUnassginedInteg(context, integrationName);
		else
			simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, integrationName);

		if(simpleGCO != null)
		{
			Hashtable typeClassMapping		= simpleGCO.getAttributeAsHashtable(MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-TypeClassMapping"), "\n", "|");
			String familyLikeCADTypes		= (String)typeClassMapping.get("TYPE_FAMILY_LIKE");

			if(familyLikeCADTypes!=null)
			{
				BusinessObject busObject	= new BusinessObject(objectId);

				String cadType				= mxUtil.getCADTypeForBO(context, busObject);

				Vector familyTypesList		= MCADUtil.getVectorFromString(familyLikeCADTypes, ",");

				if(familyTypesList.contains(cadType))
					return new Boolean(true);
				else
					return new Boolean(false);
			}
			else
			{
				return new Boolean(false);
			}
		}
		else
		{
			return new Boolean(false);
		}
	}

	public Boolean canShowIntegrationListFilter(Context context, String []args)  throws Exception
	{
		boolean result					= false;
		HashMap paramMap				= (HashMap)JPO.unpackArgs(args);
		String showIntegrationOption	= (String)paramMap.get("showIntegrationOption");

		if(showIntegrationOption != null && !"".equals(showIntegrationOption) && showIntegrationOption.equalsIgnoreCase("true"))
		{
			result = true;
		}
		return new Boolean(result);
	}

	public Boolean isCADIntegAssigned(Context context, String []args)  throws Exception
	{
		boolean returnVal = true;

		try
		{	HashMap paramMap			  = (HashMap)JPO.unpackArgs(args);
			HashMap settings				  = (HashMap) paramMap.get("SETTINGS");
			String isRecentlyCheckedInFiles	=null;
			if(settings!=null && !settings.isEmpty()){
			 isRecentlyCheckedInFiles				  = (String) settings.get("isRecentlyCheckedInFiles");
                        }
			IEFIntegAccessUtil mxUtil = new IEFIntegAccessUtil(context, new MCADServerResourceBundle(""), new IEFGlobalCache());

			Vector assignIntegrations = mxUtil.getAssignedIntegrations(context);

			Enumeration assignIntegEnum = assignIntegrations.elements();

			while(assignIntegEnum.hasMoreElements())
			{
				String integName = assignIntegEnum.nextElement().toString();

				if(integName.equalsIgnoreCase("MSOffice") && isRecentlyCheckedInFiles !=null && isRecentlyCheckedInFiles.equalsIgnoreCase("true"))
				{
					returnVal = false;
					break;
				}
			}
		}
		catch(Exception exception)
		{
			returnVal = true;
		}

		return new Boolean(returnVal);
	}

	public Boolean canShowAddNewCommand(Context context,String[] args) throws Exception
	{
		boolean isShowCommand	= true;

		if(isCATIAIntegrationObject(context, args))
		{
			isShowCommand = isConnectedToBaseline(context, args);
		}
		else
		{
			isShowCommand = false;
		}
		return new Boolean(isShowCommand);
	}

	public Boolean isConnectedToBaseline(Context context,String[] args) throws Exception
	{
		boolean isShowCommand	= true;

		try
		{
			HashMap paramMap			  = (HashMap)JPO.unpackArgs(args);
			String objectId				  = (String) paramMap.get("objectId");
			String language				  = (String) paramMap.get("languageStr");

			if(!hasIntegrationAccess(context, objectId, language) ||  isDesignBaselined(context, objectId, language))
			{
				isShowCommand = false;
			}
		}
		catch(Throwable e)
		{
			isShowCommand = false;
		}

		return new Boolean(isShowCommand);
	}

	private boolean isDesignBaselined(Context context, String objectId, String language)
	{
		boolean isDesignBaselined = false;

		try
		{
			String REL_DECBASELINE		  = MCADMxUtil.getActualNameForAEFData(context, "relationship_DesignBaseline");
			String REL_ACTIVE_VERSION	  = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");

			String SELECT_ON_BASELINE	  = "from[" + REL_DECBASELINE + "].to.";
			String SELECT_ON_ACTIVE_MINOR = "from[" + REL_ACTIVE_VERSION + "].to.";
			String BASELINE_ID			  = SELECT_ON_BASELINE + "id";
			String baselineId			  = "";

			String[] objectIds			  = new String[1];
			objectIds[0]				  = objectId;

			StringList busSelectionList	  = new StringList();
			//busSelectionList.add("type");  // [NDM] QWJ
			busSelectionList.add(BASELINE_ID);//baseline on Major
			busSelectionList.add(SELECT_ON_ACTIVE_MINOR + BASELINE_ID); // Baseline On Active Minor

			BusinessObjectWithSelectList busWithSelectionList 	= 	BusinessObject.getSelectBusinessObjectData(context, objectIds, busSelectionList);

			for(int i = 0 ; i < busWithSelectionList.size() ; i++)
			{
				BusinessObjectWithSelect busObjectWithSelect  =  (BusinessObjectWithSelect)busWithSelectionList.get(i);					
				// [NDM] QWJ
				//String busType								  = busObjectWithSelect.getSelectData("type");

				if(this.isMinorObject(context, objectIds[0], language))
				{
					baselineId = busObjectWithSelect.getSelectData(BASELINE_ID);
				}
				else
				{
					baselineId = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + BASELINE_ID);
				}

				if (baselineId != null && !baselineId.equals("") && baselineId.length() > 0)
				{
					isDesignBaselined = true;
				}
			}
		}
		catch(Throwable e)
		{
		}

		return isDesignBaselined;
	}

	public Boolean isCATIAUser(Context context, String []args)  throws Exception
	{
		HashMap paramMap					  = (HashMap)JPO.unpackArgs(args);
		String language						  = (String) paramMap.get("languageStr");

		MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle(language);
		IEFGlobalCache cache							= new IEFGlobalCache();
		IEFIntegAccessUtil               util           = new IEFIntegAccessUtil(context, serverResourceBundle, cache);

		Vector assignedIntegration                      = util.getAssignedIntegrations(context);

		boolean isUserHasAccess = false;
		if (assignedIntegration.contains("MxCATIAV5") || assignedIntegration.contains("MxCATIAV4") )		
		{
			isUserHasAccess = true;
		}
		else
		{
			isUserHasAccess = false;
		}
		return isUserHasAccess;
	}


	public Boolean checkAccessToLaunchCadTool(Context context, String []args)  throws Exception
	{
		boolean isLaunchCadTool = false;
		try
		{
			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
			String objectId		= (String) paramMap.get("objectId");
			String language		= (String) paramMap.get("languageStr");
			if(objectId != null && objectId.length() > 0)
			{
				MCADMxUtil util					= new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
				String integrationName			= util.getIntegrationName(context, objectId);				
				String launchCadToolAttr		= MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-CSELaunchBinaryDetails");
				IEFSimpleConfigObject simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, integrationName);
				String launchCadToolAttrValue	= simpleGCO.getConfigAttributeValue(launchCadToolAttr);

				if(launchCadToolAttrValue != null && launchCadToolAttrValue.length() > 0)
				{
					isLaunchCadTool = true;	
				}
			}
			else
			{
				HashMap integrationNameGCOTable	 = (HashMap)paramMap.get("GCOTable");
				if(integrationNameGCOTable != null && integrationNameGCOTable.size() > 0)
				{
					Iterator keysItr = integrationNameGCOTable.keySet().iterator();
					while(keysItr.hasNext())
					{
						String gcoName = (String) keysItr.next();
						if(integrationNameGCOTable.get(gcoName) != null)
						{
							MCADGlobalConfigObject gco = (MCADGlobalConfigObject)integrationNameGCOTable.get(gcoName);
							if(gco != null && gco.getCSELaunchBinaryDetails() != null)
							{
								String launchCadToolAttrValue = (String)gco.getCSELaunchBinaryDetails();					
								if(launchCadToolAttrValue != null && launchCadToolAttrValue.length() > 0)
								{
									isLaunchCadTool = true;
									break;
								}
							}
						}
					}
				}
			}
		}catch(Exception e)
		{			
		}
		return isLaunchCadTool;
	}

	public Boolean canShowEbomSync(Context context , String []args) throws Exception 
	{
		boolean isEbomSyncAccess = false; 
		try
		{
			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
			String objectId		= (String) paramMap.get("objectId");
			String language		= (String) paramMap.get("languageStr");
			String ecVersion = FrameworkUtil.getApplicationVersion(context, "X-BOMEngineering");

			 if(ecVersion != null && !ecVersion.equals("") && (null == objectId || !isMinorObject(context, objectId, language)))
				isEbomSyncAccess = true;
			
		}
		catch(Exception e)
		{
			isEbomSyncAccess = false;
		}

		return new Boolean(isEbomSyncAccess);
	}


	public Boolean hasLicenseAssigned(Context context , String []args) throws Exception 
	{
		boolean isLicenseAssigned = false; 
		try
		{ 
			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
			String objectId		= (String) paramMap.get("objectId");
			String language		= (String) paramMap.get("languageStr");

			isLicenseAssigned = hasLicenseAssigned(context,objectId,language);
		}
		catch(Exception me)
		{
			isLicenseAssigned = false;
		}
		return new Boolean(isLicenseAssigned);
	}


	public Boolean hasLicenseAssigned(Context context ,String objectId,String language) throws Exception 
	{
		boolean isLicenseAssigned = false; 
		try
		{ 
			MCADMxUtil util			= new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
			String integrationName  = util.getIntegrationName(context, objectId);


			Hashtable parametersTable = new Hashtable();
			parametersTable.put("IntegrationName",integrationName);

			IEFLicenseUtil.checkLicenseForDesignerCentral(context, parametersTable, null);
			isLicenseAssigned = true;

		}
		catch(Exception me)
		{
			isLicenseAssigned = false;
		}

		return new Boolean(isLicenseAssigned);
	}


	public Boolean canEditBaseline(Context context , String []args) throws Exception 
	{
		boolean isLicenseAssigned = false; 
		try
		{ 
			String rootNodeDetailsAttrActualName 	= MCADMxUtil.getActualNameForAEFData(context, "attribute_RootNodeDetails");
			String rootNodeDetails					= null;

			HashMap paramMap						= (HashMap)JPO.unpackArgs(args);
			String baselineObjectId					= (String) paramMap.get("objectId");
			String language							= (String) paramMap.get("languageStr");

			StringList busSelectionList	  = new StringList();
			busSelectionList.add("attribute[" + rootNodeDetailsAttrActualName + "]");

			String[] objectIds			  = new String[1];
			objectIds[0]				  = baselineObjectId;


			BusinessObjectWithSelectList busWithSelectionList;
			busWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objectIds, busSelectionList);

			if(busWithSelectionList.size() > 0)
			{
				BusinessObjectWithSelect busObjectWithSelect	= (BusinessObjectWithSelect)busWithSelectionList.get(0);					
				rootNodeDetails									= busObjectWithSelect.getSelectData("attribute["+ rootNodeDetailsAttrActualName +"]");


				if(rootNodeDetails != null)
				{

					Enumeration tokens	= MCADUtil.getTokensFromString(rootNodeDetails, "|");
					String busType		 = (String)tokens.nextElement();
					String busName		 = (String)tokens.nextElement();
					String busRev		 = (String)tokens.nextElement();

					BusinessObject	busObj		=  new BusinessObject(busType,busName,busRev,"");
					busObj.open(context);
					String	objectId	= busObj.getObjectId();
					busObj.close(context);

					isLicenseAssigned = hasLicenseAssigned(context,objectId,language);
				}
			}
		}
		catch(Exception me)
		{
			isLicenseAssigned = false;
		}

		return new Boolean(isLicenseAssigned);
	}

	public Boolean isCATIAIntegrationObject(Context context,String[] args) throws Exception
	{
		boolean returnVal = false;

		try 
		{
			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
			String objectId		= (String) paramMap.get("objectId");
			String language		= (String) paramMap.get("languageStr");
			MCADMxUtil util			= new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
			String integrationName  = util.getIntegrationName(context, objectId);
			if(integrationName!=null && (integrationName.equalsIgnoreCase("MxCATIAV4")||integrationName.equalsIgnoreCase("MxCATIAV5")))
			{
				returnVal = true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			returnVal = false;
		}
		return new Boolean(returnVal);
	}

	public static Boolean isNonSLWIntegPresent(Context context,String[] args)
	{
		boolean returnVal = true;

		try 
		{
			HashMap paramMap = (HashMap)JPO.unpackArgs(args);
			String language	 = (String) paramMap.get("languageStr");
			String funcPageName	 = (String) paramMap.get("funcPageName");
			String integrationName	 = (String) paramMap.get("integrationName");

			if(funcPageName != null && funcPageName.equals("MyLockedObjects") && integrationName !=null && integrationName.equals(MCADAppletServletProtocol.SOLIDWORKS_INTEG_NAME))
			{
				returnVal = false;
			}
			else
			{
				String allRegisteredIntegrations = (String)JPO.invoke(context, "IEFGetRegisteredIntegrations", null, "getRegisteredIntegrations", null, String.class);

				Vector allAvailableIntegrations = MCADUtil.getVectorFromString(allRegisteredIntegrations, "|");

				IEFIntegAccessUtil util		= new IEFIntegAccessUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

				Vector assignedIntegrations = util.getAssignedIntegrations(context);

				if((allAvailableIntegrations.size() == 1 && allAvailableIntegrations.contains(MCADAppletServletProtocol.SOLIDWORKS_INTEG_NAME)) 
						|| (assignedIntegrations.size() == 1 && assignedIntegrations.contains(MCADAppletServletProtocol.SOLIDWORKS_INTEG_NAME)))
				{
					ResourceBundle iefProperties		= PropertyResourceBundle.getBundle("ief");
					String supportFamilyInStartDesign	= "false";
					
					try
					{
						if(iefProperties.getString("mcadIntegration.SupportFamilyInStartDesign") != null)
						{
							supportFamilyInStartDesign = iefProperties.getString("mcadIntegration.SupportFamilyInStartDesign");
						}
					}
					catch(Exception ex)
					{
						supportFamilyInStartDesign	= "false";
					}
					
					if(supportFamilyInStartDesign.equalsIgnoreCase("false"))
					returnVal = false;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			returnVal = true;
		}

		return new Boolean(returnVal);
	}

	public Boolean canShowSaveAs(Context context , String []args) throws Exception 
	{
		boolean isSaveAsAccess = true; 
		try
		{
			HashMap paramMap = (HashMap)JPO.unpackArgs(args);
			MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject)paramMap.get("GCO");
			String language	 = (String) paramMap.get("languageStr");
			
			String allRegisteredIntegrations = (String)JPO.invoke(context, "IEFGetRegisteredIntegrations", null, "getRegisteredIntegrations", null, String.class);

			Vector allAvailableIntegrations = MCADUtil.getVectorFromString(allRegisteredIntegrations, "|");

			IEFIntegAccessUtil util		= new IEFIntegAccessUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

			Vector assignedIntegrations = util.getAssignedIntegrations(context);

			if( ((allAvailableIntegrations.size() == 1 && allAvailableIntegrations.contains(MCADAppletServletProtocol.SOLIDWORKS_INTEG_NAME)) 
					|| (assignedIntegrations.size() == 1 && assignedIntegrations.contains(MCADAppletServletProtocol.SOLIDWORKS_INTEG_NAME))))
			{
				isSaveAsAccess = false;
			}
			
		}
		catch(Exception e)
		{
			isSaveAsAccess = true;
		}

		return new Boolean(isSaveAsAccess);
	}

	public Boolean blockSaveAsForSolidWorks(Context context,String[] args)
	{
		boolean returnVal = true;

		try 
		{
			HashMap paramMap = (HashMap)JPO.unpackArgs(args);
			String objectId	 = (String) paramMap.get("objectId");
			String language	 = (String) paramMap.get("languageStr");
			MCADMxUtil mxUtil	 = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
			String gcoName		= this.getGlobalConfigObjectName(context, objectId);
			MCADGlobalConfigObject globalConfigObject		= getGlobalConfigObject(context, gcoName, MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig") ,mxUtil);
			
			if(objectId != null)
			{
				String integrationName  = mxUtil.getIntegrationName(context, objectId);

				if((integrationName != null && integrationName.equalsIgnoreCase(MCADAppletServletProtocol.SOLIDWORKS_INTEG_NAME)) || !hasIntegrationAccess(context, objectId, language))
				{
					returnVal = false;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			returnVal = true;
		}
		return new Boolean(returnVal);
	}
	
	public Boolean canShowAsSavedView(Context context , String []args) throws Exception 
	{
		boolean isAsSavedViewEnabled = false; 
		try
		{
			HashMap paramMap = (HashMap)JPO.unpackArgs(args);
			String language	 = (String) paramMap.get("languageStr");
			MCADMxUtil mxUtil	 = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
			isAsSavedViewEnabled = mxUtil.isAsSavedViewEnabled();	
		}
		catch(Exception e)
		{
			isAsSavedViewEnabled = false;
		}

		return new Boolean(isAsSavedViewEnabled);
	}
	
	private String getGlobalConfigObjectName(Context context, String busId) throws Exception
	{
		// Get the IntegrationName
		IEFGuessIntegrationContext_mxJPO guessIntegration = new IEFGuessIntegrationContext_mxJPO(context, null);
		String jpoArgs[] = new String[1];
		jpoArgs[0] = busId;
		String integrationName = guessIntegration.getIntegrationName(context, jpoArgs);
		String gcoType					= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
		String attrName				= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");
		
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

	protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String gcoName, String gcoType, MCADMxUtil mxUtil) throws Exception
	{
		MCADGlobalConfigObject gcoObject	= null;

		if(gcoName != null && gcoName.length() > 0)
		{
			MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader();
			gcoObject							= configLoader.createGlobalConfigObject(context, mxUtil, gcoType, gcoName);
		}
		return gcoObject;
	}
	
	public Boolean blockForSolidWorks(Context context,String[] args)
	{
		boolean returnVal = true;

		try 
		{
			HashMap paramMap = (HashMap)JPO.unpackArgs(args);
			String objectId	 = (String) paramMap.get("objectId");
			String language	 = (String) paramMap.get("languageStr");

			returnVal  = isNonSLWIntegPresent(context, args).booleanValue();

			if(objectId != null && returnVal)
			{
				MCADMxUtil mxUtil	 = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

				String integrationName  = mxUtil.getIntegrationName(context, objectId);

				if((integrationName != null && integrationName.equalsIgnoreCase(MCADAppletServletProtocol.SOLIDWORKS_INTEG_NAME)) || !hasIntegrationAccess(context, objectId, language))
				{
					returnVal = false;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			returnVal = true;
		}

		return new Boolean(returnVal);
	}

	public Boolean checkQuickCheckoutLanchCommand(Context context,String[] args) throws Exception
	{
		boolean returnVal = false;
		if(isCATIAIntegrationObject(context, args) && canRevise(context, args))
		{
			returnVal=true;
		}
		return new Boolean(returnVal);
	}

	public Boolean checkQuickCheckoutCommand(Context context,String[] args) throws Exception
	{
		boolean returnVal = false;
		if(isCATIAIntegrationObject(context, args) && canShowCommandForIntegUser(context, args))
		{
			returnVal=true;
		}
		return new Boolean(returnVal);
	}

	public Boolean checkAccessMultipleQuickCheckoutLanchCommand(Context context, String []args)  throws Exception
	{
		//check is CATIA integration is assigned.
		return isCATIAUser(context, args);
	}

	public Boolean checkAccessMultipleQuickCheckout(Context context, String []args)  throws Exception
	{
		//check is CATIA integration is assigned.
		return isCATIAUser(context, args);
	}
	
	public Boolean isSLWIntegrationObject(Context context,String[] args) throws Exception
	{
		boolean returnVal = false;

		try 
		{
			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
		    String objectId		= (String) paramMap.get("objectId");
		    String language		= (String) paramMap.get("languageStr");
			MCADMxUtil util			= new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
			String integrationName  = util.getIntegrationName(context, objectId);
			if(integrationName!=null && integrationName.equalsIgnoreCase(MCADAppletServletProtocol.SOLIDWORKS_INTEG_NAME))
			{
				returnVal = true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			returnVal = false;
		}
		return new Boolean(returnVal);
        }
	
	public Boolean checkCheckoutCommand(Context context,String[] args) throws Exception
	{
		boolean returnVal = false;
		if(!isSLWIntegrationObject(context, args) && canShowCommandForIntegUser(context, args))
		{
			returnVal=true;
		}
		return new Boolean(returnVal);
	}


	public Boolean checkAccessForExpandedSubComponent(Context context , String []args) throws Exception 
	{
		boolean	showQuantityColumn = true;

		try
		{	
			HashMap paramMap			              = (HashMap)JPO.unpackArgs(args);
			String objectId				              = (String) paramMap.get("objectId");
			MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject)paramMap.get("GCO");
			boolean isExpandedSubComponent	          =  globalConfigObject.isExpandedSubComponent();
			
			if(objectId != null && !"".equals(objectId))
			{
				if(isExpandedSubComponent)
					showQuantityColumn	= false;
			}
		}
		catch(Exception e)
		{
			showQuantityColumn = false;
		}

		return new Boolean(showQuantityColumn);
	}

	public Boolean checkShowAccessForExcludeFormBOM(Context context , String []args) throws Exception 
	{
		boolean	showQuantityColumn = false;

		try
		{	
			HashMap paramMap			              = (HashMap)JPO.unpackArgs(args);
			String objectId				              = (String) paramMap.get("objectId");
			MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject)paramMap.get("GCO");
			boolean isExpandedSubComponent	          =  globalConfigObject.isExpandedSubComponent();
			
			if(objectId != null && !"".equals(objectId))
			{
				if(isExpandedSubComponent)
					showQuantityColumn	= true;
			}
		}
		catch(Exception e)
		{
			showQuantityColumn = true;
		}

		return new Boolean(showQuantityColumn);
	}
}

