/*
 **  DSCWebFormActionsLinkBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to display Action Icons
 */
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;

import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.MCADIntegration.utils.UUID;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;


public class DSCWebFormActionsLinkBase_mxJPO
{
	protected HashMap integrationNameGCOTable				= null;
	protected MCADServerResourceBundle serverResourceBundle	= null;
	protected IEFIntegAccessUtil util				= null;
	protected MCADServerGeneralUtil serverGeneralUtil       = null;
	protected MCADMxUtil _mxUtil							= null;
	protected String localeLanguage							= null;

	//static String formatJTStr			= PropertyUtil.getSchemaProperty("format_JT");
	protected HashMap paramMap			= null;
	protected String strFileFormat		= "";
	protected IEFGlobalCache	cache	= null;

	public String attrMoveFilesToVersion						= "";
	public String attrIsVersionObject							= "";
	public String attrSource 							= "";

	DSC_CommonUtil_mxJPO jpoUtil						    = new DSC_CommonUtil_mxJPO();

	public DSCWebFormActionsLinkBase_mxJPO(Context context, String[] args) throws Exception
	{
		//super(context, args);
	}

	protected boolean isVersionObject(HashMap objectMap)
	{
		String result = (String)objectMap.get(attrIsVersionObject);
		if (result == null || result.length() < 0)
			return false;
		return result.equalsIgnoreCase("true");
	}

	protected boolean fileInMinor(HashMap objectMap)
	{
		String result = (String)objectMap.get(attrMoveFilesToVersion);
		if (result == null || result.length() < 0)
			return false;
		return result.equalsIgnoreCase("true");
	}

	protected String getIntegrationName(HashMap objectMap)
	{
		String result = (String)objectMap.get(attrSource);
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

	public String getViewerValidObjectId(Context context, HashMap objectAttributeMap, String objectId, String majorObjectId) throws Exception
	{
		//[IR_456517:MAL3]:START
		//Changes done to return correct object id
		if(objectId.equalsIgnoreCase(util.getActiveVersionObject(context, majorObjectId)))
			return majorObjectId;
		else
			return objectId;	
		//[IR_3456517:MAL3]:END		
	}

	public String getHtmlString(Context context, String[] args) throws Exception
	{
		StringBuffer htmlBuffer			= new StringBuffer(300);

		paramMap						= (HashMap)JPO.unpackArgs(args);  
		HashMap requestMap				= (HashMap)paramMap.get("requestMap");
		String objectId					= (String)requestMap.get("objectId");
		attrIsVersionObject				= MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
		attrSource						= MCADMxUtil.getActualNameForAEFData(context, "attribute_Source");
		String validObjectId			= "";

		try
		{
			localeLanguage			= (String)requestMap.get("languageStr");
			serverResourceBundle	= new MCADServerResourceBundle(localeLanguage);
			cache					= new IEFGlobalCache();
			util					= new IEFIntegAccessUtil(context, serverResourceBundle, cache);
			_mxUtil			 		= new MCADMxUtil(context, serverResourceBundle, cache);

			HashMap attributeMap	= jpoUtil.getCommonDocumentAttributes(context,  objectId);
			BusinessObject busObj	= new BusinessObject(objectId);
			busObj.open(context);
			String majorObjectId	= objectId;

			// to get the ECO connected List
			Hashtable emptyHashtable	= new Hashtable();
			String sRelName				= MCADMxUtil.getActualNameForAEFData(context,"relationship_NewSpecificationRevision");
			BusinessObjectList busList	= util.getRelatedBusinessObjects(context, busObj, sRelName,"to",emptyHashtable);

			if(busList != null && busList.size() == 0)
			{
				sRelName	= MCADMxUtil.getActualNameForAEFData(context,"relationship_AffectedItem");
				busList		= util.getRelatedBusinessObjects(context, busObj, sRelName,"to",emptyHashtable);
				if(busList != null && busList.size() == 0){
					sRelName	= MCADMxUtil.getActualNameForAEFData(context,"relationship_ChangeAffectedItem");
					busList		= util.getRelatedBusinessObjects(context, busObj, sRelName,"to",emptyHashtable);
				}
			}

			if (true == isVersionObject(attributeMap))
			{
				BusinessObject majorObject = util.getMajorObject(context, busObj);
				majorObject.open(context);
				majorObjectId = majorObject.getObjectId();
				majorObject.close(context);
			}

			htmlBuffer.append(addSubscriptionLink(context, majorObjectId));
			
			//[IR_456517:MAL3]:START
			if(majorObjectId.equalsIgnoreCase(objectId))
				validObjectId = majorObjectId;
			else
				validObjectId = getViewerValidObjectId(context, attributeMap, objectId, majorObjectId);
			//[IR_456517:MAL3]:END
			
			String integrationName = getIntegrationName(attributeMap);

			if(integrationName != null && util.getAssignedIntegrations(context).contains(integrationName))
			{
				MCADGlobalConfigObject globalConfigObject = getGlobalConfigObject(context, objectId);

				String mode   = (String)requestMap.get("mode");
				
				String jpoName	= globalConfigObject.getFeatureJPO("OpenFromWeb");

				if(null != jpoName && !"".equals(jpoName))	
				{
					htmlBuffer.append(addOpenFromWebLink(context, globalConfigObject, integrationName, objectId, jpoName));
				}
				
				// [NDM] QWJ
				htmlBuffer.append(addDownloadStructureLink(context, objectId, isVersionObject(attributeMap)));
				
				// [NDM] : H68
				if (false == isVersionObject(attributeMap))
				htmlBuffer.append(addLockUnlockLink(context, majorObjectId,  integrationName, mode));

				// Check for connected ECO and disply of ECO icon
				if(busList != null && busList.size() > 0)
				{
					BusinessObject relatedObj	= null;
					String ecoObjId = "" ;
					String ecoObjType = "";
					for(int j=0; j<busList.size(); j++)
					{
						relatedObj	= busList.getElement(j);
						ecoObjType = relatedObj.getTypeName();
//						if(!"".equals(ecoObjType) && ecoObjType.equals("ECO"))
//						{
							ecoObjId = relatedObj.getObjectId();
//						}
					}	

					if(!"".equals(ecoObjId))
					{
//						htmlBuffer.append(addECOIconLink(context, ecoObjId));
						htmlBuffer.append(addChangeMgmtIconLink(context, ecoObjId, ecoObjType));
					}
				}
			}
			else
			{
				htmlBuffer.append(addDownloadLink(context, objectId));
			}

			htmlBuffer.append(addViewerLink(context, validObjectId));

			busObj.close(context);        
		}
		catch(Exception e)
		{
		}

		return htmlBuffer.toString();
	}

	private String addDownloadStructureLink(Context context, String objectId, boolean isVersionedObject)  // [NDM] QWJ
	{
		String downloadStructureToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.DownloadStructure");
		
		String downloadStructureURL			= "../integrations/DSCMCADGenericActionsProcess.jsp?action=DownloadStructure" + "&amp;Target Location=hiddenFrame" + "&amp;emxTableRowId=" + objectId + "&amp;isVersionedObject=" + isVersionedObject+"&amp;fromLocation=Table";
		String downloadStructureIcon		= "../../common/images/iconActionDownload.gif";
		
		String url = getFeatureIconContent(downloadStructureURL, downloadStructureIcon, downloadStructureToolTip, null, "listHidden");
		
		return url;
	}


	private String addOpenFromWebLink(Context context, MCADGlobalConfigObject globalConfigObject, String integrationName, String objectId, String jpoName) throws Exception
	{
		String returnVal = "";
		try
		{
			Hashtable jpoArgsTable = new Hashtable();
			jpoArgsTable.put(MCADServerSettings.GCO_OBJECT, globalConfigObject);
			jpoArgsTable.put(MCADServerSettings.LANGUAGE_NAME, serverResourceBundle.getLanguageName());
			jpoArgsTable.put(MCADServerSettings.OBJECT_ID, objectId);
			jpoArgsTable.put(MCADAppletServletProtocol.INTEGRATION_NAME, integrationName);
			jpoArgsTable.put(MCADServerSettings.OPERATION_UID, UUID.getNewUUIDString());
			jpoArgsTable.put("featureName", MCADGlobalConfigObject.FEATURE_OPEN);

			String[] args 			= JPO.packArgs(jpoArgsTable);

			Hashtable result 		= (Hashtable) JPO.invoke(context, jpoName, new String[] {}, "execute", args, Hashtable.class);
			String hrefLink			= (String) result.get("hrefString");

			String href				= "../integrations/IEFCustomProtocolHandler.jsp?hreflink=" + hrefLink + "&amp;integrationname=" + integrationName;
			String featureImage		= "iconActionCheckOut.gif";
			String openToolTip		= serverResourceBundle.getString("mcadIntegration.Server.AltText.Open");

			returnVal 				= getFeatureIconContent(href, featureImage, openToolTip, null, "listHidden");

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return returnVal;

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

		IEFSimpleConfigObject simpleLCO = IEFSimpleConfigObject.getSimpleLCO(context);

		String attrName = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");
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
	
	protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String objectId) throws Exception
	{
		String typeGlobalConfig							= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
		String gcoName									= this.getGlobalConfigObjectName(context, objectId);

		MCADGlobalConfigObject gcoObject	= null;

		if(gcoName != null && gcoName.length() > 0)
		{
			MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(null);
			gcoObject							= configLoader.createGlobalConfigObject(context, _mxUtil, typeGlobalConfig, gcoName);
		}
		return gcoObject;
	}

	protected String addViewerLink(Context context, String objectId) throws MatrixException
	{
		BusinessObject	bus	= null;
		try
		{	
			BusinessObjectList allObjectList	= new BusinessObjectList();

			String	viewableRel			= MCADMxUtil.getActualNameForAEFData(context, "relationship_Viewable");
			String	derivedOutputRel	= MCADMxUtil.getActualNameForAEFData(context, "relationship_DerivedOutput");
			ArrayList	relList	= new ArrayList();
			relList.add(viewableRel);
			relList.add(derivedOutputRel);

			bus		= new BusinessObject(objectId);
			allObjectList	= getAllDerivedOutputObjects(context, bus, relList);
			allObjectList.addElement(bus);

			HashMap paramMap		= new HashMap();
			paramMap.put("objectList", allObjectList);
			Vector ret = (Vector)JPO.invoke(context, "DSCShowViewerLink", null, "getHtmlString", JPO.packArgs(paramMap), Vector.class);
			StringBuffer htmlBuffer = new StringBuffer(300);
			htmlBuffer.append("&nbsp;");
			for (int i = 0; i < ret.size(); i++)
			{
				String html = (String)ret.elementAt(i);
				htmlBuffer.append(html);
			}
			return htmlBuffer.toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return "";
	}

	protected String addSubscriptionLink(Context context, String objectId)
	{
		String subscribeToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.Subscribe");

		StringBuffer subscribeURLBuffer = new StringBuffer();
		subscribeURLBuffer.append("'../components/emxSubscriptionDialog.jsp?objectId=");
		subscribeURLBuffer.append(objectId);
		subscribeURLBuffer.append("&suiteKey=Components&Target Location=popup'");

		String subscribeURL		= subscribeURLBuffer.toString();
		String subscribeIcon	= "iconSubscribeSmall.gif";

		StringBuffer subscribeHrefBuffer = new StringBuffer();
		subscribeHrefBuffer.append("javascript:parent.showNonModalDialog(");
		subscribeHrefBuffer.append(subscribeURL);
		subscribeHrefBuffer.append(",700,500)");

		String subscribeHref	= subscribeHrefBuffer.toString() ;

		String url = getFeatureIconContent(subscribeHref,  subscribeIcon,  subscribeToolTip, null, "hiddenFrame");

		return url;
	}

	protected String addECOIconLink(Context context, String objectId)
	{
		String  ecoToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.eco");

		String ecoURL		= "'../common/emxTree.jsp?AppendParameters=true&mode=insert&objectId=" + objectId+"&suiteKey=IEFDesignCenter"+"&Target Location=popup'";
		String ecoIcon		= "./iconSmallECO.gif";
		String ecoHref		= "javascript:parent.showNonModalDialog(" + ecoURL + ",700,500)";

		String url			= getFeatureIconContent(ecoHref,  ecoIcon,  ecoToolTip, null, "hiddenFrame");

		return url;
	}


	protected String getLockedVersionNLocker(Context context, String majorObjectId) throws MCADException
	{
		String retStr ="";
		try
		{
			BusinessObject majorObj = new BusinessObject(majorObjectId);
			majorObj.open(context);
			String locker = majorObj.getLocker(context).getName();
			majorObj.close(context);
			if(!locker.equals(""))
			{
				retStr = majorObj.getRevision();
				retStr = "true|true|" + retStr + "|" + locker;
			}
		}
		catch(MatrixException me)
		{
			String msg = "[MCADMxUtil.getLockedVersion]: Exception: " + me.getMessage();
			System.out.println(msg);
		}

		return retStr;
	}


	protected String addLockUnlockLink(Context context, String objectId,  String integrationName, String mode)
	{
		try
		{
			String formName = mode != null &&  mode.equals("view") ? "formViewHidden" : "formEditHidden";
			String canShowLockUnlock = getLockedVersionNLocker(context, objectId);
			String lockUnlockIcon = null;
			String lockUnlockToolTip	= null;
			if  (canShowLockUnlock.startsWith(MCADAppletServletProtocol.TRUE))
			{
				Enumeration lockUnlockElements = MCADUtil.getTokensFromString(canShowLockUnlock, "|");
				String canLock	= (String)lockUnlockElements.nextElement();
				String isLocked = (String)lockUnlockElements.nextElement();

				if(isLocked.equals(MCADAppletServletProtocol.TRUE))
				{
					lockUnlockIcon = "iconUnLocked.gif";
					objectId	   = objectId + "|" + MCADAppletServletProtocol.FALSE;
					lockUnlockToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.Unlock");
				}
				else
				{
					lockUnlockIcon = "iconLocked.gif";
					objectId	   = objectId + "|" + MCADAppletServletProtocol.TRUE;
					lockUnlockToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.Lock");
				}
			}
			else
			{
				lockUnlockIcon = "iconLocked.gif";
				objectId	   = objectId + "|" + MCADAppletServletProtocol.TRUE;
				lockUnlockToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.Lock");
			}

			String lockUnlockURL		= "";

			if (integrationName == null || "".equals(integrationName))
				lockUnlockURL = "'../integrations/DSCLockUnlockActionProcess.jsp?busDetails=None|true|" + objectId + ""+"&Target Location=formViewHidden'";
			else
				lockUnlockURL = "'../integrations/DSCLockUnlockActionProcess.jsp?busDetails=" + integrationName + "|true|" + objectId + ""+"&Target Location=formViewHidden'";

			String lockUnlockHref		= "#";
			String onClickAction		= "javascript:frames['"+ formName +"'].location.href=" + lockUnlockURL + ";return false";
			String url					= getFeatureIconContent(lockUnlockHref, lockUnlockIcon, lockUnlockToolTip, onClickAction, "");

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
				viewerHref.append("&objectId=" + objectId);
				viewerHref.append("&format="+strFileFormat);
				viewerHref.append("&fileName=" + fileName);

				strBuf.append(viewerHref.toString());		   													
			}

			return strBuf.toString();
		}
		catch (Exception e)
		{
			System.out.println(e.toString());			
		}
		return "";
	}

	protected String getFeatureIconContent(String href, String featureImage, String toolTop, String onClickAction, String targetName)
	{
		StringBuffer featureIconContent = new StringBuffer();

		featureIconContent.append("<a ");

		if(onClickAction != null && !onClickAction.equals(""))
		{
			featureIconContent.append("	onclick=\"");
			featureIconContent.append(onClickAction);
			featureIconContent.append("\"");
		}

		if(targetName.length() > 0)
		{
			featureIconContent.append(" target=\"");
			featureIconContent.append(targetName);
			featureIconContent.append("\"");
		}

		featureIconContent.append(" href=\"");
		featureIconContent.append(href);
		featureIconContent.append("\" ><img src=\"../iefdesigncenter/images/");
		featureIconContent.append(featureImage);
		featureIconContent.append("\" border=\"0\" title=\"");
		featureIconContent.append(toolTop);
		featureIconContent.append("\"></a>");

		return featureIconContent.toString();
	}

	protected String addDownloadLink(Context context, String majorObjId) throws Exception
	{
		String url = "";

		BusinessObject majorObj	   = new BusinessObject(majorObjId);
		majorObj.open(context);

		String latestMinorId	   = util.getLatestMinorID(context, majorObj);
		if(util.hasFiles(context,majorObjId) || util.hasFiles(context,latestMinorId))
		{
			String downloadToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.Download");
			String downloadURL	   = "../components/emxCommonDocumentPreCheckout.jsp?objectId=" + majorObjId + "&action=download";
			String downloadHref    = "javascript:parent.showNonModalDialog('" + downloadURL + "',700,500)";

			url					   = getFeatureIconContent(downloadHref, "../../common/images/iconActionDownload.gif", downloadToolTip, null, "hiddenFrame");
		}

		majorObj.close(context);

		return url;
	}

	private BusinessObjectList getAllDerivedOutputObjects(Context context, BusinessObject bus, ArrayList<String> relList)throws Exception
	{
		BusinessObjectList derivedOutputObjList	= new BusinessObjectList();
		java.util.List<BusinessObjectList> tempListInList		= new java.util.ArrayList<BusinessObjectList>();

		Iterator<String>	relItr	= relList.iterator();
		while(relItr.hasNext())
		{
			String	relName	= (String)relItr.next();
			tempListInList.add(util.getRelatedBusinessObjects(context, bus, relName, "from"));
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
						derivedOutputObjList.addElement(tempObjItr.obj());
					}
				}
			}
		}
		return derivedOutputObjList;
	}
	
	protected String addChangeMgmtIconLink(Context context, String objectId, String objType) throws MCADException
	{
		String sTypeECO = MCADMxUtil.getActualNameForAEFData(context,"type_ECO");
		String sTypeCA = MCADMxUtil.getActualNameForAEFData(context,"type_ChangeAction");
		
		String  ecoToolTip	= "";
		String ecoIcon		= "";
		if(sTypeECO.equals(objType)){
			ecoToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.eco");
			ecoIcon		= "./iconSmallECO.gif";
		} else if(sTypeCA.equals(objType)){
			ecoToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.CA");
			ecoIcon		= "../../common/images/iconSmallChangeAction.gif";
		}

		String ecoURL		= "'../common/emxTree.jsp?AppendParameters=true&mode=insert&objectId=" + objectId+"&suiteKey=IEFDesignCenter"+"&Target Location=popup'";
		String ecoHref		= "javascript:parent.showNonModalDialog(" + ecoURL + ",700,500)";

		String url			= getFeatureIconContent(ecoHref,  ecoIcon,  ecoToolTip, null, "hiddenFrame");

		return url;
	}
	
}

