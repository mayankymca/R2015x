/*
 **  DSCShowActionsLinkBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to display Checkout Icon
 */
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.MCADIntegration.utils.UUID;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.i18nNow;

public class DSCShowActionsLinkBase_mxJPO
{
	protected HashMap integrationNameGCOTable				= null;
	protected MCADServerResourceBundle serverResourceBundle	= null;
	protected IEFIntegAccessUtil util				= null;
	protected MCADServerGeneralUtil serverGeneralUtil       = null;
	protected String localeLanguage							= null;

	//static String formatJTStr								= PropertyUtil.getSchemaProperty("format_JT");
	protected HashMap paramMap								= null;
	protected String strFileFormat							= "";
	protected IEFGlobalCache	cache						= null;

	protected	String REL_VERSION_OF						= "";
	protected	String REL_ACTIVE_VERSION					= "";
	protected	String REL_DERIVED_OUTPUT					= "";
	protected	String REL_VIEWABLE							= "";
	protected	String REL_PART_SPECIFICATION				= "";
	protected	String REL_AFFECTED_ITEM					= "";
	protected	String SELECT_ON_ECO						= "";
	protected	String SELECT_ON_SPC_PART					= "";
	protected	String SELECT_ON_MAJOR						= "";
	protected	String SELECT_ON_ACTIVE_MINOR				= "";
	protected	String SELECT_ON_DERIVED_OUTPUT				= "";
	protected	String SELECT_ON_VIEWABLE					= "";
	protected	String ATTR_SOURCE							= "";
	protected 	String ATTR_CAD_TYPE 						= "";
	protected	String ATTR_TITLE							= "";
	protected	String REL_VAULTED_DOCUMENTS      			= "";
	protected 	String TYPE_WORKSPACE_VAULT 				= "";
	protected   String GET_VAULT_IDS 						= "";
	protected	String REL_CHANGE_AFFECTED_ITEM					= "";
	protected	String SELECT_ON_CA						= "";
	protected String IS_VERSION_OBJ							= "";
	protected String SELECT_ISVERSIONOBJ			= "";

	public DSCShowActionsLinkBase_mxJPO(Context context, String[] args) throws Exception
	{
		//super(context, args);
	}

	public Object getHtmlString(Context context, String[] args) throws Exception
	{		
		paramMap					= (HashMap)JPO.unpackArgs(args);
		MapList relBusObjPageList	= (MapList)paramMap.get("objectList");
		Map paramList				= (Map)paramMap.get("paramList");


		localeLanguage			= (String)paramList.get("languageStr");

		String portalName		= (String)paramList.get("portCmdName");
		integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");
		return getHtmlStringForTable(context, relBusObjPageList, portalName);
	}

	public Object getHtmlStringForFrameworkTable(Context context, String[] args) throws Exception
	{		
		paramMap					= (HashMap)JPO.unpackArgs(args);
		MapList relBusObjPageList	= (MapList)paramMap.get("objectList");

		localeLanguage			= (String)paramMap.get("languageStr");
		String portalName		= (String)paramMap.get("portCmdName");

		HashMap paramList = (HashMap)paramMap.get("paramList");			
		if(localeLanguage == null)
			localeLanguage			= (String)paramList.get("languageStr");

		integrationNameGCOTable			= (HashMap)paramList.get("GCOTable");

		if(integrationNameGCOTable == null)
			integrationNameGCOTable			= (HashMap)paramMap.get("GCOTable");

		return getHtmlStringForTable(context, relBusObjPageList, portalName);
	}

	protected Object getHtmlStringForTable(Context context, MapList relBusObjPageList, String portalName) throws Exception
	{		
		Vector columnCellContentList	= new Vector();
		String sUseMinor				= "";
		HashMap formatViewerMap			= new HashMap();

		serverResourceBundle			= new MCADServerResourceBundle(localeLanguage);
		cache							= new IEFGlobalCache();
		util							= new IEFIntegAccessUtil(context, serverResourceBundle, cache);

		String[] objIds					= new String[relBusObjPageList.size()];

		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");			

			// Designer Central 10.6.0.1			
			sUseMinor = (String)objDetails.get("UseMinor");
		}

		try
		{
			Vector assignedIntegrations = util.getAssignedIntegrations(context);

			String openToolTip			= serverResourceBundle.getString("mcadIntegration.Server.AltText.Open");
			String downloadToolTip		= serverResourceBundle.getString("mcadIntegration.Server.AltText.Download");
			String lockUnlockToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.LockUnlock");
			String  subscribeToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.Subscribe");

			REL_VERSION_OF				= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
			REL_ACTIVE_VERSION			= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
			REL_DERIVED_OUTPUT			= MCADMxUtil.getActualNameForAEFData(context, "relationship_DerivedOutput");
			REL_VIEWABLE				= MCADMxUtil.getActualNameForAEFData(context, "relationship_Viewable");
			REL_AFFECTED_ITEM			= MCADMxUtil.getActualNameForAEFData(context, "relationship_AffectedItem");
			REL_CHANGE_AFFECTED_ITEM	= MCADMxUtil.getActualNameForAEFData(context, "relationship_ChangeAffectedItem");

			String ecoType				= MCADMxUtil.getActualNameForAEFData(context, "type_ECO");
			String sTypeCA = MCADMxUtil.getActualNameForAEFData(context,"type_ChangeAction");

			SELECT_ON_MAJOR				= "from[" + REL_VERSION_OF + "].to.";
			SELECT_ON_ACTIVE_MINOR		= "from[" + REL_ACTIVE_VERSION + "].to.";		
			SELECT_ON_DERIVED_OUTPUT	= "from[" + REL_DERIVED_OUTPUT + "].to.";
			SELECT_ON_VIEWABLE			= "from[" + REL_VIEWABLE + "].to.";
			SELECT_ON_ECO				= "to[" + REL_AFFECTED_ITEM + "].from[" + ecoType + "].";
			SELECT_ON_CA				= "to[" + REL_CHANGE_AFFECTED_ITEM + "].from[" + sTypeCA + "].";

			REL_VAULTED_DOCUMENTS		= MCADMxUtil.getActualNameForAEFData(context, "relationship_VaultedDocuments");
			TYPE_WORKSPACE_VAULT 		= MCADMxUtil.getActualNameForAEFData(context, "type_ProjectVault");
			GET_VAULT_IDS 				= "to[" + this.REL_VAULTED_DOCUMENTS + "].from[" + this.TYPE_WORKSPACE_VAULT + "].id";

			ATTR_SOURCE					= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";
			ATTR_TITLE					= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Title") + "]";
			ATTR_CAD_TYPE				= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType") + "]";

			StringList busSelectionList = new StringList();

			busSelectionList.addElement("id");
			busSelectionList.addElement("logicalid");
			busSelectionList.addElement("physicalid");
			busSelectionList.addElement("name");
			busSelectionList.addElement("revision");
			busSelectionList.addElement(ATTR_TITLE);
			busSelectionList.addElement("type");
			busSelectionList.addElement("format.file.name");
			busSelectionList.addElement("format.file.format");

			busSelectionList.addElement(ATTR_SOURCE); //To get Integrations name.
			busSelectionList.addElement(ATTR_CAD_TYPE); 
			busSelectionList.addElement(SELECT_ON_MAJOR + "id"); //Major object id.
			busSelectionList.addElement(SELECT_ON_MAJOR + "logicalid"); //Major logical id.
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + "id"); //Minor ID
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + "type");

			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + "format.file.name");
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + "format.file.format");

			busSelectionList.addElement(SELECT_ON_MAJOR + "format.file.name");
			busSelectionList.addElement(SELECT_ON_MAJOR + "format.file.format");

			busSelectionList.addElement("locked"); //To get Integrations name.
			busSelectionList.addElement(SELECT_ON_MAJOR + "locked"); //from Minor.

			busSelectionList.addElement(SELECT_ON_ECO + "id");// For ECO Icon
			busSelectionList.addElement(SELECT_ON_CA + "id");// For ECO Icon
			
			busSelectionList.addElement(SELECT_ON_DERIVED_OUTPUT + "id"); //DerivedOutput Id.
			busSelectionList.addElement(SELECT_ON_DERIVED_OUTPUT + "type");//DerivedOutput obj type.
			busSelectionList.addElement(SELECT_ON_DERIVED_OUTPUT + "format.file.name");
			busSelectionList.addElement(SELECT_ON_DERIVED_OUTPUT + "format.file.format");
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + SELECT_ON_DERIVED_OUTPUT + "id"); //DerivedOutput Id.
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + SELECT_ON_DERIVED_OUTPUT + "format.file.name");
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + SELECT_ON_DERIVED_OUTPUT + "format.file.format");

			busSelectionList.addElement(SELECT_ON_VIEWABLE + "id"); //DerivedOutput Id.
			busSelectionList.addElement(SELECT_ON_VIEWABLE + "type"); //DerivedOutput obj type.
			busSelectionList.addElement(SELECT_ON_VIEWABLE + "format.file.name");
			busSelectionList.addElement(SELECT_ON_VIEWABLE + "format.file.format");
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + SELECT_ON_VIEWABLE + "id"); //DerivedOutput Id.
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + SELECT_ON_VIEWABLE  + "type"); //DerivedOutput obj type.
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + SELECT_ON_VIEWABLE + "format.file.name");
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + SELECT_ON_VIEWABLE + "format.file.format");

			busSelectionList.addElement(GET_VAULT_IDS);
			busSelectionList.addElement(SELECT_ON_MAJOR + GET_VAULT_IDS);

			IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context,"attribute_IsVersionObject");
			SELECT_ISVERSIONOBJ = "attribute["+IS_VERSION_OBJ+"]";	
			busSelectionList.addElement(SELECT_ISVERSIONOBJ);

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{
				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);

				StringBuffer htmlBuffer = new StringBuffer();
				String integrationName	= null;

				String objectId				= busObjectWithSelect.getSelectData("id");
				String integrationSource	= busObjectWithSelect.getSelectData(ATTR_SOURCE);

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}

				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
				{
					MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

					if(null == cache)
					{
						cache	= new IEFGlobalCache();
					}

					String majorObjectId		= "";
					String ecoObjId				= "";
					String caObjId				= ""; 
					String canShowLockUnlock	= "";

					String objID				= objectId;
					
					majorObjectId				= objectId;
					String sIsVersion 			= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
					boolean isVersion 			= Boolean.valueOf(sIsVersion).booleanValue();
					//boolean isMajor				= util.isMajorObject(context ,objID); //[NDM] : H68
					StringList ecoList = busObjectWithSelect.getSelectDataList(SELECT_ON_ECO + "id");
					if(ecoList != null){
						ecoObjId				= (String) ecoList.lastElement();
					} 
					StringList caList = busObjectWithSelect.getSelectDataList(SELECT_ON_CA + "id");
					if(caList != null){
						caObjId				= (String) caList.lastElement();
					}

					//[NDM]
					if(!isVersion)
					{
						majorObjectId		= objectId;
						canShowLockUnlock	= busObjectWithSelect.getSelectData("locked");

						// Designer Central 10.6.0.1
						if(sUseMinor != null && sUseMinor.equals("true"))
							objectId = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + "id");
						// Designer Central 10.6.0.1
					}
					else
					{
						majorObjectId		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "id");
						//ecoObjId			= busObjectWithSelect.getSelectData(SELECT_ON_SPC_PART + "id");
						canShowLockUnlock	= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "locked");
					}

					// add subscription icon
					htmlBuffer.append(addSubscriptionLink(context, majorObjectId, integrationName, subscribeToolTip));

					// [NDM] QWJ
					htmlBuffer.append(addDownloadStructureLink(context, objectId, isVersion));

					String jpoName	= gco.getFeatureJPO("OpenFromWeb");

					if(null != jpoName && !"".equals(jpoName))	
					{
						//add open from web link
						htmlBuffer.append(addOpenFromWebLink(context, busObjectWithSelect, gco, integrationName, objID, jpoName, openToolTip ));
					}

					// Check for connected ECO and disply of ECO icon
//					if(!ecoObjId.equals("")) 
//					{
//						htmlBuffer.append(addECOLink(context, ecoObjId,  integrationName));
//					}
					if(!ecoObjId.equals("") || !caObjId.equals("")){
						htmlBuffer.append(addChangeManagementLink(context, ecoObjId, caObjId));
					}

					//Lock/UnLock Icon
					if(!isVersion) //[NDM] H68: not to show lock/unlock icon for minor
					{
					if(!canShowLockUnlock.equals("")) 
					{
						canShowLockUnlock = "TRUE|" + canShowLockUnlock;
					}
					//canShowLockUnlock = executeLinkJPO(context, majorObjectId, integrationName) ;
					htmlBuffer.append(addLockUnlockLink(context, majorObjectId, integrationName, canShowLockUnlock, lockUnlockToolTip));
					}
					htmlBuffer.append(addViewerLink(context, objectId, integrationName, formatViewerMap, busObjectWithSelect));
				}
				else
				{
					htmlBuffer.append(addSubscriptionLink(context, objectId,  "", subscribeToolTip));

					if(showFileDownloadIcon(context, objectId))
					{

						String checkoutURL				= "../components/emxCommonDocumentPreCheckout.jsp?objectId=" + objectId + "&amp;action=download&amp;Target Location=popup";
						htmlBuffer.append(getFeatureIconContent(checkoutURL, "../common/images/iconActionDownload.gif", downloadToolTip,"popup"));

						String actionURL = null;						
						integrationName	= util.getIntegrationName(context, objectId);

						if(integrationName != null && integrationNameGCOTable.containsKey(integrationName))
						{
							actionURL				= addViewerLink(context, objectId, integrationName, formatViewerMap, busObjectWithSelect);
							htmlBuffer.append(actionURL);
						}

					}
				}

				columnCellContentList.add(htmlBuffer.toString());
			}
		}
		catch(Exception e)
		{

		}
		return columnCellContentList;
	}

	protected String addChangeManagementLink(Context context, String ecoObjectId, String caObjectId)
	{

		String sObjectId = "";
		String ecoToolTip	= "";
		String ecoIcon		= "";
		if(!ecoObjectId.equals("")){
			ecoToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.eco");
			ecoIcon		= "../iefdesigncenter/images/iconSmallECO.gif";
			sObjectId = ecoObjectId;
		} else if(!caObjectId.equals("")){
			ecoToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.CA");
			ecoIcon		= "../common/images/iconSmallChangeAction.gif";
			sObjectId = caObjectId;
		}
		
		//String ecoURL		= "'../engineeringcentral/emxpartSpecRelatedECOFS.jsp?LaunchCADTool=false"+"&objectId="  + objectId+"&suiteKey=IEFDesignCenter"+"&Target Location=popup'";

		String ecoURL		= "'../common/emxTree.jsp?AppendParameters=true&amp;mode=insert&amp;objectId=" + sObjectId+"&amp;suiteKey=IEFDesignCenter"+"&amp;Target Location=popup'";
		//String ecoIcon		= "../iefdesigncenter/images/iconSmallECO.gif";
		String ecoHref		= "javascript:parent.showNonModalDialog(" + ecoURL + ",'700','500')";

		String url			= getFeatureIconContent(ecoHref,  ecoIcon,  ecoToolTip,"listHidden");

		return url;
	}
	
	// [NDM] QWJ
	private String addDownloadStructureLink(Context context, String objectId, boolean isVersionedObject) 
	{
		String downloadStructureToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.DownloadStructure");
		
		// [NDM] QWJ
		String downloadStructureURL			= "../integrations/DSCMCADGenericActionsProcess.jsp?action=DownloadStructure" + "&amp;Target Location=hiddenFrame" + "&amp;emxTableRowId=" + objectId + "&amp;isVersionedObject=" + isVersionedObject+"&amp;fromLocation=Table";
		String downloadStructureIcon		= "../common/images/iconActionDownload.gif";
		
		String url = getFeatureIconContent(downloadStructureURL, downloadStructureIcon, downloadStructureToolTip, "listHidden");
		
		return url;
	}
	
	private String addOpenFromWebLink(Context context, BusinessObjectWithSelect busObjectWithSelect, MCADGlobalConfigObject gco, String integrationName, String objectId, String jpoName, String openToolTip) throws Exception
	{
		String returnVal = "";
		try
		{
			Hashtable jpoArgsTable = new Hashtable();
			jpoArgsTable.put(MCADServerSettings.GCO_OBJECT, gco);
			jpoArgsTable.put(MCADServerSettings.LANGUAGE_NAME, serverResourceBundle.getLanguageName());
			jpoArgsTable.put(MCADServerSettings.OBJECT_ID, objectId);
			jpoArgsTable.put(MCADAppletServletProtocol.INTEGRATION_NAME, integrationName);
			jpoArgsTable.put(MCADServerSettings.OPERATION_UID, UUID.getNewUUIDString());
			jpoArgsTable.put("selectionList", busObjectWithSelect);
			jpoArgsTable.put("featureName", MCADGlobalConfigObject.FEATURE_OPEN);

			String[] args 		= JPO.packArgs(jpoArgsTable);

			Hashtable result 	= (Hashtable) JPO.invoke(context, jpoName, new String[] {}, "execute", args, Hashtable.class);
			String hrefLink		= (String) result.get("hrefString");

			String href			= "../integrations/IEFCustomProtocolHandler.jsp?hreflink=" + hrefLink + "&amp;integrationname=" + integrationName;
			String featureImage	= "../iefdesigncenter/images/iconActionCheckOut.gif";

			returnVal			= getFeatureIconContent(href ,featureImage , openToolTip , "listHidden");

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return returnVal;

	}

	protected String addViewerLink(Context context, String objectId, String integrationName, HashMap formatViewerMap, BusinessObjectWithSelect busObjectWithSelect)
	{
		try
		{			
			MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
			String lang					= (String)context.getSession().getLanguage();
			String sTipView				= i18nNow.getI18nString("emxTeamCentral.ContentSummary.ToolTipView", "emxTeamCentralStringResource", lang);
			StringBuffer htmlBuffer		= new StringBuffer(300);

			StringList filesList		= null;
			StringList formatsList		= null;
			short type					= 0;			
			StringList objectIdList		= null;
			StringList objectTypeList	= null;

			// [NDM] QWJ
			//String busType				= busObjectWithSelect.getSelectData("type");	
			String busId				= busObjectWithSelect.getSelectData("id");	
			String sIsVersion 			= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
			boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
			
			boolean isDerived = true;			

			if(!isVersion) // [NDM] QWJ
			{
				filesList		= (StringList)busObjectWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + "format.file.name");
				type			= busObjectWithSelect.getSelectDataType(SELECT_ON_ACTIVE_MINOR + "format.file.name");		
				formatsList		= (StringList)busObjectWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + "format.file.format");
				objectIdList	= (StringList)busObjectWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + "id");
				objectTypeList	= (StringList)busObjectWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + "type");

				if(filesList == null || type == 0)
				{
					//Finalized major
					filesList		= (StringList)busObjectWithSelect.getSelectDataList("format.file.name");
					formatsList		= (StringList)busObjectWithSelect.getSelectDataList("format.file.format");
					objectIdList	= (StringList)busObjectWithSelect.getSelectDataList("id");
					objectTypeList	= (StringList)busObjectWithSelect.getSelectDataList("type");
				}
			}
			else
			{
				filesList		= (StringList)busObjectWithSelect.getSelectDataList("format.file.name");
				type			= busObjectWithSelect.getSelectDataType("format.file.name");		
				formatsList 	= (StringList)busObjectWithSelect.getSelectDataList("format.file.format");
				objectIdList	= (StringList)busObjectWithSelect.getSelectDataList("id");
				objectTypeList	= (StringList)busObjectWithSelect.getSelectDataList("type");

				if(filesList == null || type == 0)
				{
					//Finalized minor
					filesList		= (StringList)busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR + "format.file.name");
					formatsList 	= (StringList)busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR + "format.file.format");
					objectIdList	= (StringList)busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR + "id");
					objectTypeList	= (StringList)busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR + "type");
				}
			}			

			appendViewerURLToBuffer(context, integrationName, objectIdList, objectTypeList, formatsList, filesList, formatViewerMap, sTipView, htmlBuffer,isDerived);		

			String depDocRelSelect = SELECT_ON_DERIVED_OUTPUT;
			StringList depDocObjectIdList	= null;
			StringList depDocObjectTypeList	= null;
			depDocObjectIdList = (StringList)busObjectWithSelect.getSelectDataList(depDocRelSelect + "id");

			if(depDocObjectIdList == null || depDocObjectIdList.equals(""))
			{
				depDocRelSelect			= SELECT_ON_ACTIVE_MINOR + SELECT_ON_DERIVED_OUTPUT;
				depDocObjectIdList		= (StringList)busObjectWithSelect.getSelectDataList(depDocRelSelect + "id");
				depDocObjectTypeList	= (StringList)busObjectWithSelect.getSelectDataList(depDocRelSelect + "type");

			}
			if(depDocObjectIdList != null && !depDocObjectIdList.equals(""))
			{			
				filesList	= (StringList)busObjectWithSelect.getSelectDataList(depDocRelSelect + "format.file.name");
				formatsList = (StringList)busObjectWithSelect.getSelectDataList(depDocRelSelect + "format.file.format");

				appendViewerURLToBuffer(context, integrationName, depDocObjectIdList, depDocObjectTypeList, formatsList, filesList, formatViewerMap, sTipView, htmlBuffer,isDerived);
			}


			depDocRelSelect			= SELECT_ON_VIEWABLE;
			depDocObjectIdList		= (StringList)busObjectWithSelect.getSelectDataList(depDocRelSelect + "id");
			depDocObjectTypeList	= (StringList)busObjectWithSelect.getSelectDataList(depDocRelSelect + "type");

			if(depDocObjectIdList == null || depDocObjectIdList.equals(""))
			{
				depDocRelSelect			= SELECT_ON_ACTIVE_MINOR + SELECT_ON_VIEWABLE;
				depDocObjectIdList		= (StringList)busObjectWithSelect.getSelectDataList(depDocRelSelect + "id");
				depDocObjectTypeList	= (StringList)busObjectWithSelect.getSelectDataList(depDocRelSelect + "type");

			}			
			if(depDocObjectIdList != null)
			{				
				filesList	= (StringList)busObjectWithSelect.getSelectDataList(depDocRelSelect + "format.file.name");
				formatsList = (StringList)busObjectWithSelect.getSelectDataList(depDocRelSelect + "format.file.format");
				isDerived = false;

				appendViewerURLToBuffer(context, integrationName, depDocObjectIdList, depDocObjectTypeList, formatsList, filesList, formatViewerMap, sTipView, htmlBuffer,isDerived);
			}

			return htmlBuffer.toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return "";
	}

	private void appendViewerURLToBuffer(Context context, String integrationName, StringList depDocObjectIdList, StringList depDocObjectTypeList, StringList formatsList, StringList filesList, HashMap formatViewerMap, String sTipView, StringBuffer htmlBuffer,boolean filesInSameObject) throws Exception
	{	
		MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

		if(depDocObjectIdList !=null && !depDocObjectIdList.equals("") && formatsList != null)
		{

			for (int i= 0; i<formatsList.size(); i++)
			{
				String format			= (String)formatsList.elementAt(i);
				String	objectId        = null;
				if(filesInSameObject)
				{
					objectId			= (String)depDocObjectIdList.elementAt(0);	

				}
				else
				{
					String mxType	= "";

					if(depDocObjectIdList.size()==1)
						objectId	= (String)depDocObjectIdList.elementAt(0);						

					else 
					{
						Vector cadMxTypeVector	= gco.getCADMxTypeForFormat(format);
						if(!cadMxTypeVector.isEmpty())
						{							
							Iterator cadMxTypeItr	= cadMxTypeVector.iterator();
							while(cadMxTypeItr.hasNext())
							{
								String	cadMxType = (String)cadMxTypeItr.next();
								if(cadMxType.indexOf("|")!= -1)
									mxType = cadMxType.substring(cadMxType.indexOf("|")+1, cadMxType.length());

								for (int j= 0; j<depDocObjectTypeList.size(); j++)
								{
									String	objectType	= (String)depDocObjectTypeList.elementAt(j);
									if(!"".equals(mxType) && mxType.equals(objectType))
										objectId	= (String)depDocObjectIdList.elementAt(j);									
								}
							}
						}
					}						

				}	
				if(objectId == null)
					objectId	= (String)depDocObjectIdList.elementAt(0);

				String[] viewerServletAndTip = getViewerServletAndTip(context, formatViewerMap, format);
				String sViewerServletName	 = viewerServletAndTip[0];
				String tipView				 = viewerServletAndTip[1];

				if (sViewerServletName == null || sViewerServletName.length() == 0)
					continue;

				if (tipView != null && tipView.length() != 0)
					sTipView = tipView;

				String fileName			 = (String)filesList.elementAt(i);
				String sFileViewerLink	 = "/servlet/" + sViewerServletName;
				String viewerURL		 = "../iefdesigncenter/emxInfoViewer.jsp?url=" + sFileViewerLink + "&amp;id=" + objectId + "&amp;format=" + format + "&amp;file=" + MCADUrlUtil.hexEncode(fileName);

				String viewerHref		 = viewerURL;
				viewerHref				 = "javascript:openWindow('"+ viewerURL + "')";

				String url				 = getFullPathFeatureIconContent(viewerHref, "../iefdesigncenter/images/iconActionViewer.gif", sTipView + " (" + format + ")");

				htmlBuffer.append(url);
			}
		}
	}

	protected String[] getValidObjctIdForCheckout(Context context, String objectId )
	{
		String[] checkoutObjectDetails = new String[3];
		try
		{
			Hashtable argsTable = new Hashtable();

			String integrationName		= util.getIntegrationName(context, objectId);
			MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

			argsTable.put(MCADServerSettings.GCO_OBJECT, gco);
			argsTable.put(MCADServerSettings.LANGUAGE_NAME, localeLanguage);
			argsTable.put(MCADServerSettings.OBJECT_ID, objectId);
			argsTable.put(MCADServerSettings.JPO_METHOD_NAME, MCADGlobalConfigObject.FEATURE_CHECKOUT );

			MCADJPOUtils_mxJPO MCADJPOUtilsJPO = new MCADJPOUtils_mxJPO(context, null);
			Hashtable resultDataTable = (Hashtable)MCADJPOUtilsJPO.execute(context, argsTable);

			String result = (String)resultDataTable.get(MCADServerSettings.JPO_EXECUTION_STATUS);
			if (result.equalsIgnoreCase("false"))
			{
				String error = (String)resultDataTable.get(MCADServerSettings.JPO_STATUS_MESSAGE);
				MCADServerException.createException(error, null);
			}	
			checkoutObjectDetails[0] = (String)resultDataTable.get(MCADServerSettings.JPO_EXECUTION_STATUS);
			checkoutObjectDetails[1] = (String)resultDataTable.get(MCADServerSettings.JPO_EXECUTION_RESULT);
			checkoutObjectDetails[2] = (String)resultDataTable.get(MCADServerSettings.JPO_STATUS_MESSAGE);
		}
		catch(Exception exception)
		{
			checkoutObjectDetails[0] = "false";
			checkoutObjectDetails[1] = objectId;
			checkoutObjectDetails[2] = exception.getMessage();
		}
		return checkoutObjectDetails;
	}

	protected String addSubscriptionLink(Context context, String objectId, String integrationName, String subscribeToolTip)
	{
		String subscribeURL			= "'../components/emxSubscriptionDialog.jsp?objectId="  + objectId+"&amp;suiteKey=Components"+"&amp;Target Location=popup'";
		String subscribeIcon		= "../iefdesigncenter/images/iconSubscribeSmall.gif";
		String subscribeHref		= "javascript:parent.showNonModalDialog(" + subscribeURL + ",'700','500')";

		String url = getFeatureIconContent(subscribeHref,  subscribeIcon,  subscribeToolTip,"listHidden");

		return url;
	}


	protected String addECOLink(Context context, String objectId, String integrationName)
	{

		String ecoToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.eco");

		//String ecoURL		= "'../engineeringcentral/emxpartSpecRelatedECOFS.jsp?LaunchCADTool=false"+"&objectId="  + objectId+"&suiteKey=IEFDesignCenter"+"&Target Location=popup'";

		String ecoURL		= "'../common/emxTree.jsp?AppendParameters=true&amp;mode=insert&amp;objectId=" + objectId+"&amp;suiteKey=IEFDesignCenter"+"&amp;Target Location=popup'";
		String ecoIcon		= "../iefdesigncenter/images/iconSmallECO.gif";
		String ecoHref		= "javascript:parent.showNonModalDialog(" + ecoURL + ",'700','500')";

		String url			= getFeatureIconContent(ecoHref,  ecoIcon,  ecoToolTip,"listHidden");

		return url;
	}

	protected String addLockUnlockLink(Context context, String objectId, String integrationName, String canShowLockUnlock, String lockUnlockToolTip)
	{
		try
		{
			String lockUnlockIcon = null;

			if  (canShowLockUnlock.startsWith("TRUE"))
			{
				Enumeration lockUnlockElements = MCADUtil.getTokensFromString(canShowLockUnlock, "|");
				String canLock	= (String)lockUnlockElements.nextElement();
				String isLocked = (String)lockUnlockElements.nextElement();

				if(isLocked.equalsIgnoreCase("TRUE"))
				{
					lockUnlockIcon		= "../iefdesigncenter/images/iconUnLocked.gif";
					objectId	   = objectId + "|" + MCADAppletServletProtocol.FALSE;
					lockUnlockToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.Unlock");
				}
				else
				{
					lockUnlockIcon		= "../iefdesigncenter/images/iconLocked.gif";
					objectId	   = objectId + "|" + MCADAppletServletProtocol.TRUE;
					lockUnlockToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.Lock");
				}

			}
			else
			{
				lockUnlockIcon		= "../iefdesigncenter/images/iconLocked.gif";
				objectId	   = objectId + "|" + MCADAppletServletProtocol.TRUE;
				lockUnlockToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.Lock");
			}

			String lockUnlockURL		= "../integrations/MCADObjectLockUnlock.jsp?busDetails=" + integrationName + "|true|" + objectId;

			String url = getFeatureIconContent(lockUnlockURL, lockUnlockIcon, lockUnlockToolTip, "listHidden");

			return url;

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return "";
	}


	protected String getViewerURL(Context context,  String[] args)
	{
		StringBuffer strBuf = new StringBuffer(1256);
		try
		{
			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);

			MapList relBusObjPageList	= (MapList)paramMap.get("objectList");

			for (int i  = 0; i < relBusObjPageList.size(); i++)
			{
				HashMap objDetails		= (HashMap)relBusObjPageList.get(i);
				String objectId			= (String)objDetails.get("id");
				DomainObject object = DomainObject.newInstance(context, objectId);
				StringList selects = new StringList(7);
				selects.add(DomainConstants.SELECT_TYPE);
				selects.add(DomainConstants.SELECT_ID);
				selects.add(CommonDocument.SELECT_TITLE);
				selects.add(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
				selects.add(CommonDocument.SELECT_SUSPEND_VERSIONING);
				selects.add(CommonDocument.SELECT_FILE_NAME);

				Map objectSelectMap = object.getInfo(context, selects);
				strFileFormat  = object.getDefaultFormat(context);

				String title = (String)objectSelectMap.get(CommonDocument.SELECT_TITLE);
				if (title == null) title = (String)objectSelectMap.get(CommonDocument.SELECT_FILE_NAME);

				StringList sFileName = (StringList)objectSelectMap.get(CommonDocument.SELECT_FILE_NAME);
				String fileName = "";
				for (int j= 0; i< sFileName.size(); j++)
				{
					String str = (String)sFileName.elementAt(i);
					fileName = str;
					break;
				}

				StringBuffer viewerHref =  new StringBuffer(128);
				viewerHref.append("../components/emxComponentsCheckout.jsp");
				viewerHref.append("?action=view");
				viewerHref.append("&amp;objectId=" + objectId);
				viewerHref.append("&amp;format="+strFileFormat);
				viewerHref.append("&amp;fileName=" + fileName);

				strBuf.append(viewerHref.toString());
			}

			return strBuf.toString();
		}
		catch (Exception e)
		{
			System.out.println("DSCShowActionsLinkBase_mxJPO.getViewerURL() :"+e.getMessage());			
		}
		return "";
	}

	protected String executeJPO(Context context, String objectID, String jpoName, String jpoMethod, String integrationName) throws Exception
	{
		MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

		String [] packedGCO = JPO.packArgs(globalConfigObject);
		String [] args = new String[5];
		args[0] = packedGCO[0];
		args[1] = packedGCO[1];
		args[2] = objectID;
		args[3] = localeLanguage;
		args[4] = "";

		String result = util.executeJPO(context, jpoName, jpoMethod, args);

		return result;
	}

	protected String canShowFeatureIcon(Context context, String objectID, String jpoName, String jpoMethod, String integrationName)
	{
		String canShowIcon = MCADAppletServletProtocol.FALSE;

		try
		{
			canShowIcon = executeJPO(context, objectID, jpoName, jpoMethod, integrationName);
		}
		catch(Exception e)
		{
			System.out.println(e.toString());
		}

		return canShowIcon;
	}

	protected String getFullPathFeatureIconContent(String href, String featureImagePath, String toolTop)
	{
		StringBuffer featureIconContent = new StringBuffer();

		featureIconContent.append("<a href=\"");
		featureIconContent.append(href);
		featureIconContent.append("\"><img src=\"");
		featureIconContent.append(featureImagePath);
		featureIconContent.append("\" border=\"0\" title=\"");
		featureIconContent.append(toolTop);
		featureIconContent.append("\" alt=\"");
		featureIconContent.append(toolTop);
		featureIconContent.append("\"/></a>");

		return featureIconContent.toString();
	}

	protected String getFeatureIconContent(String href, String featureImage, String toolTop, String targetName)
	{
		StringBuffer featureIconContent = new StringBuffer();

		featureIconContent.append("<a href=\"");
		featureIconContent.append(href);
		featureIconContent.append("\" ");
		if(targetName.length() > 0)
		{
			featureIconContent.append("target=\"");
			featureIconContent.append(targetName);
			featureIconContent.append("\"");
		}
		featureIconContent.append(" ><img src=\"");
		featureIconContent.append(featureImage);
		featureIconContent.append("\" border=\"0\" title=\"");
		featureIconContent.append(toolTop);
		featureIconContent.append("\" alt=\"");
		featureIconContent.append(toolTop);
		featureIconContent.append("\"/></a>");

		return featureIconContent.toString();
	}

	protected BusinessObjectList getDerivedOutputs(Context context, String sBusId, MCADGlobalConfigObject gco, MCADServerGeneralUtil _generalUtil, MCADMxUtil _util  ) throws Exception
	{
		BusinessObjectList depDocList = null;

		String sInstanceName	= (String)paramMap.get("instanceName");

		BusinessObject partbus = new BusinessObject (sBusId);
		partbus.open(context);
		String name		= partbus.getName();
		partbus.close(context);

		String objectName = name;
		if(sInstanceName != null && !sInstanceName.trim().equals(""))
			objectName = MCADUrlUtil.hexDecode(sInstanceName);

		depDocList = _generalUtil.getDependentDocObjects(context, partbus, MCADMxUtil.getActualNameForAEFData(context, "attribute_CADObjectName"), objectName);

		return depDocList;
	}

	protected boolean showFileDownloadIcon(Context context, String majorObjId) throws Exception
	{
		boolean showFileDownload	= false;
		BusinessObject majorObj		= new BusinessObject(majorObjId);

		try
		{
			majorObj.open(context);

			String latestMinorId = util.getLatestMinorID(context, majorObj);
			if(util.hasFiles(context,majorObjId) || util.hasFiles(context,latestMinorId))
				showFileDownload = true;

			majorObj.close(context);	
		}
		catch(Exception e)
		{
		}

		return showFileDownload;
	}

	protected String[] getViewerServletAndTip(Context context, HashMap formatViewerMap, String format) throws Exception
	{
		String[] viewerServletAndTip = new String[2];
		String viewerInfo = "";

		boolean cacheViewers = false;

		if( formatViewerMap == null )
			cacheViewers = false;
		else
			cacheViewers = true;

		if ( cacheViewers && formatViewerMap.containsKey(format) )
		{
			viewerInfo = (String)formatViewerMap.get(format);
		}
		else
		{
			// format and store all of them  in a String seperated by comma
			MQLCommand prMQL  = new MQLCommand();
			prMQL.open(context);
			prMQL.executeCommand(context,"execute program $1 $2","eServicecommonGetViewers.tcl",format);
			viewerInfo = prMQL.getResult().trim();
			if(cacheViewers)
				formatViewerMap.put(format, viewerInfo);
		}

		if( null != viewerInfo && viewerInfo.length() > 0)
		{
			StringTokenizer viewerTokenizer = new StringTokenizer(viewerInfo, "|", false);	
			String sErrorCode = "";
			if (viewerTokenizer.hasMoreTokens()) 
				sErrorCode = viewerTokenizer.nextToken();
			if (sErrorCode.equals("0"))
			{
				if (viewerTokenizer.hasMoreTokens()) 
					viewerServletAndTip[0] = viewerTokenizer.nextToken();
				if (viewerTokenizer.hasMoreTokens())
					viewerServletAndTip[1] = viewerTokenizer.nextToken();
			}
		}
		return viewerServletAndTip;
	}
         
	private String getEditImage(String strObjectId) throws Exception
                {

                                StringBuffer sbImage = new StringBuffer(80);
                                String strImageLockbegin = "<img src=\"../common/images/iconActionEdit.gif\" ";
                                String strLocker = " TITLE=\" ";
                                String strImageLockend = "\" border=\"0\"  align=\"middle\"/>";     
                                sbImage.append(strImageLockbegin);
                                sbImage.append(strLocker);
                                sbImage.append(strImageLockend);
                                StringBuffer sbHrefStart = null;
                                sbHrefStart  = new StringBuffer(100);
                                sbHrefStart.append("<u><a href=\"javascript:emxTableColumnLinkClick('../iefdesigncenter/DSCForm.jsp?");
                                sbHrefStart.append("mode=edit&amp;form=DSCObjectSummaryForm&amp;HelpMarker=emxhelpdscproperties&amp;suiteKey=DesignerCentral&amp;formHeader=emxIEFDesignCenter.Header.EditSlidein.Edit&amp;");
                                sbHrefStart.append("objectId="+strObjectId);
                                sbHrefStart.append("','', '', 'true', 'slidein', '')\" >");

                                sbHrefStart.append(sbImage.toString());
                                sbHrefStart.append("</a></u>");
                                return sbHrefStart.toString();
                }
  
	public Vector getEditIcon(Context context, String[] args) throws Exception
                {
	         paramMap			= (HashMap)JPO.unpackArgs(args);
		 MapList relBusObjPageList	= (MapList)paramMap.get("objectList");
	     //String[] objIds			= new String[relBusObjPageList.size()];
		 Vector columnCellContentList	= new Vector();
		for(int i =0; i<relBusObjPageList.size(); i++)
		{
		    StringBuffer htmlBuffer = new StringBuffer();
			Map objDetails	    = (Map)relBusObjPageList.get(i);
			String eachid	    = (String)objDetails.get("id");			
                        htmlBuffer.append(getEditImage(eachid));
			columnCellContentList.add(htmlBuffer.toString());
		}
	return columnCellContentList;
				}    
}

