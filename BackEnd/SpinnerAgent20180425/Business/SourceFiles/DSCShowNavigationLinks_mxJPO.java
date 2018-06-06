
/*
 **  DSCShowNavigationLinks
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Class defining basic infrastructure, contains common data members required
 **  for executing any IEF related actions.
 */

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.MCADIntegration.utils.MCADUtil;

public class DSCShowNavigationLinks_mxJPO
{

	public  DSCShowNavigationLinks_mxJPO()
	{
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	public Object getNameColumnLink(Context context, String[] args) throws Exception
	{		
		return getHtmlString(context, args,false);
	}

	public Object getDetailsPopupLink(Context context, String[] args) throws Exception
	{
		return getHtmlString(context, args,true);
	}

	private Object getHtmlString(Context context, String[] args, boolean showIcon) throws Exception
	{	

		Vector columnCellContentList	= new Vector();

		HashMap paramMap				= (HashMap)JPO.unpackArgs(args);
		MapList relBusObjPageList		= (MapList)paramMap.get("objectList");
		Map paramList					= (Map)paramMap.get("paramList");
		String showMajor				= (String)paramList.get("showMajor");
		String reportFormat = (String) paramList.get("reportFormat");
		try{	

			String[] objIds					= new String[relBusObjPageList.size()];

			for(int i =0; i<relBusObjPageList.size(); i++)
			{
				Map objDetails	= (Map)relBusObjPageList.get(i);
				objIds[i]		= (String)objDetails.get("id");			
			}

			String REL_VERSION_OF				= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
			String SELECT_ON_MAJOR				= "from[" + REL_VERSION_OF + "].to.";

			StringList busSelectionList = new StringList();

			busSelectionList.addElement("id");
			busSelectionList.addElement("type");
			busSelectionList.addElement("name");
			busSelectionList.addElement(SELECT_ON_MAJOR + "id"); //Major object id.

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

			for(int j = 0; j < buslWithSelectionList.size(); j++)
			{
				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(j);

				String objectId				= busObjectWithSelect.getSelectData("id");
				String name					= busObjectWithSelect.getSelectData("name");
				String displayName 	= MCADUtil.escapeStringForHTML(name);

				String majorObjID			= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "id");

				if(null != showMajor && showMajor.equalsIgnoreCase("true") && null != majorObjID && !"".equals(majorObjID))
					objectId = majorObjID;

				StringBuffer htmlBuffer = new StringBuffer();
				htmlBuffer.append("<a href=\"javascript:showNonModalDialog('../common/emxTree.jsp?mode=insert&amp;suiteKey=IEFDesignCenter&amp;targetLocation=popup&amp;objectId="+objectId+"','875','550')\">");
				if(showIcon)
					htmlBuffer.append("<img src=\"../common/images/iconActionNewWindow.gif\" border=\"0\" />");
				else
					htmlBuffer.append(displayName);

				htmlBuffer.append("</a>");
				if("CSV".equalsIgnoreCase(reportFormat)){ 
				columnCellContentList.add(displayName);
				}else{ 
					columnCellContentList.add(htmlBuffer.toString());
				}
				//columnCellContentList.add(htmlBuffer.toString());
			}
		}
		catch(Exception e)
		{

		}

		return columnCellContentList;
	}
	public String showVersionOfLink(Context context, String[] args) throws Exception
	{
		StringBuffer returnString = new StringBuffer();
		String url = new String();
		String TNR = "";

		try
		{           
			HashMap params	   = (HashMap)JPO.unpackArgs(args);
			HashMap requestMap = (HashMap)params.get("requestMap");	
			String objectId	   = (String) requestMap.get("objectId");
			String localeLanguage	  						= (String) requestMap.get("languageStr");

			IEFGlobalCache cache							= new IEFGlobalCache();
			MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle(localeLanguage);
			MCADMxUtil util									= new MCADMxUtil(context, serverResourceBundle, cache);
			String majorId = "";

			BusinessObject busObj = new BusinessObject(objectId);
			BusinessObject majorObj = util.getMajorObject(context,busObj);

			majorObj.open(context);
			TNR= MCADMxUtil.getNLSName(context, "Type", majorObj.getTypeName(), "", "", localeLanguage)+" "+majorObj.getName()+" "+majorObj.getRevision();


			majorId = majorObj.getObjectId();
			majorObj.close(context);
			url = "javascript:parent.getTopWindow().showNonModalDialog('../common/emxTree.jsp?objectId="+majorId+"&targetLocation=popup','700','500')";
			returnString.append("<a href=\""); 
			returnString.append(url);
			returnString.append("\">");
			returnString.append(TNR);
			returnString.append("</a>");
		}
		catch (Exception e)
		{ 
			System.out.println("DSCShowNavigationLinks:showVersionOfLink :Error "+e.getMessage());	
		}
		return returnString.toString();
	}

	public String showActiveVersionLink(Context context,String[] args) throws Exception
	{
		StringBuffer returnString = new StringBuffer();
		String url = new String();
		String TNR = "";

		try
		{           
			HashMap params									= (HashMap)JPO.unpackArgs(args);
			HashMap requestMap								= (HashMap)params.get("requestMap");	
			String objectId									= (String) requestMap.get("objectId");
			String localeLanguage	  						= (String) requestMap.get("languageStr");
			IEFGlobalCache cache							= new IEFGlobalCache();
			MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle(localeLanguage);
			MCADMxUtil util									= new MCADMxUtil(context, serverResourceBundle, cache);
			String minorId = "";

			BusinessObject busObj = new BusinessObject(objectId);
			BusinessObject minorObj = util.getActiveMinor(context,busObj);

			minorObj.open(context);

			TNR= MCADMxUtil.getNLSName(context, "Type", minorObj.getTypeName(), "", "", localeLanguage)+" "+minorObj.getName()+" "+minorObj.getRevision();

			minorId = minorObj.getObjectId();
			minorObj.close(context);
			url = "javascript:parent.getTopWindow().showNonModalDialog('../common/emxTree.jsp?objectId="+minorId+"&targetLocation=popup','700','500')";
			returnString.append("<a href=\""); 
			returnString.append(url);
			returnString.append("\">");
			returnString.append(TNR);
			returnString.append("</a>");
		}
		catch (Exception e)
		{ 
			System.out.println("DSCShowNavigationLinks:showActiveVersionLink :Error "+e.getMessage());	
		}
		return returnString.toString();
	}

	public String showInstanceOfLink(Context context,String[] args) throws Exception
	{
		StringBuffer returnString = new StringBuffer();
		String url = new String();
		String TNR = "";

		try
		{           
			HashMap params			= (HashMap)JPO.unpackArgs(args);
			HashMap requestMap		= (HashMap)params.get("requestMap");	
			String instanceId		= (String) requestMap.get("objectId");
			String localeLanguage	  						= (String) requestMap.get("languageStr");
			String familyId = "";

			IEFGlobalCache cache							= new IEFGlobalCache();
			MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle(localeLanguage);
			MCADMxUtil util									= new MCADMxUtil(context, serverResourceBundle, cache);
			MCADGlobalConfigObject gco = getGlobalConfigObject(context,instanceId,util);
			String integrationName   = util.getIntegrationName(context, instanceId);
			MCADServerGeneralUtil serverGeneralUtil = new MCADServerGeneralUtil(context,gco,serverResourceBundle,cache);



			BusinessObject bustmpObj = new BusinessObject(instanceId);

			BusinessObject familyObj = serverGeneralUtil.getFamilyObjectForInstance(context,bustmpObj);
			
			
			
			familyObj.open(context);
			TNR= MCADMxUtil.getNLSName(context, "Type", familyObj.getTypeName(), "", "", localeLanguage)+" "+familyObj.getName()+" "+familyObj.getRevision();
			familyId = familyObj.getObjectId();
			familyObj.close(context);

			url = "javascript:parent.getTopWindow().showNonModalDialog('../common/emxTree.jsp?objectId="+familyId+"&targetLocation=popup','700','500')";
			returnString.append("<a href=\""); 
			returnString.append(url);
			returnString.append("\">");
			returnString.append(TNR);
			returnString.append("</a>");
		}
		catch (Exception e)
		{ 
			System.out.println("DSCShowNavigationLinks:showInstanceOfLink :Error "+e.getMessage());	
		}
		return returnString.toString();
	}

	public boolean canShowFamily(Context context, String [] args) throws Exception
	{
		HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
		String objectId		= (String) paramMap.get("objectId");
		String language		= (String) paramMap.get("languageStr");

		MCADServerResourceBundle serverResourceBundle	 = new MCADServerResourceBundle(language);
		IEFGlobalCache cache							 = new IEFGlobalCache();
		MCADMxUtil mxUtil			 = new MCADMxUtil(context, serverResourceBundle, cache);

		BusinessObject busObject = new BusinessObject(objectId);
		busObject.open(context);
		String cadType = mxUtil.getCADTypeForBO(context, busObject);
		MCADGlobalConfigObject gco = getGlobalConfigObject(context,objectId,mxUtil);
		MCADServerGeneralUtil servergeneralUtil = new MCADServerGeneralUtil(context,gco,serverResourceBundle,cache);
		boolean isInstance      = gco.isTypeOfClass(cadType,MCADAppletServletProtocol.TYPE_INSTANCE_LIKE);
		busObject.close(context);
		return isInstance;
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

		String gcoType  = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
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

	protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String objectId, MCADMxUtil mxUtil) throws Exception
	{
		String typeGlobalConfig							= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
		String gcoName									= this.getGlobalConfigObjectName(context, objectId);

		MCADGlobalConfigObject gcoObject	= null;

		if(gcoName != null && gcoName.length() > 0)
		{
			MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(null);
			gcoObject							= configLoader.createGlobalConfigObject(context, mxUtil, typeGlobalConfig, gcoName);
		}
		return gcoObject;
	}

}

