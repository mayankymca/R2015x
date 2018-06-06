/*
 **  IEFShowRevisionLabel
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to display Revision Label
 */
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.util.MapList;

public class IEFShowRevisionLabel_mxJPO
{
	private HashMap integrationNameGCOTable					= null;

	//NDM 3line
	private MCADMxUtil util									= null;
	private IEFGlobalCache cache							= null;
	private MCADServerResourceBundle serverResourceBundle	= null;

	private String REL_VERSION_OF							= "";
	private String SELECT_ON_MAJOR							= "";
	private	String ATTR_SOURCE								= "";
	private String localeLanguage							= "";
	private String reportFormat								= "";

	public IEFShowRevisionLabel_mxJPO(Context context, String[] args) throws Exception
	{
	}

	private List initialize(Context context, String[] args) throws Exception
	{
		HashMap paramMap			= (HashMap)JPO.unpackArgs(args);  
		HashMap paramList			= (HashMap)paramMap.get("paramList");

		reportFormat = (String) paramList.get("reportFormat");
		
		MapList relBusObjPageList 	= (MapList)paramMap.get("objectList");
		localeLanguage				= (String)paramMap.get("LocaleLanguage");

//NDM start
		serverResourceBundle		= new MCADServerResourceBundle(localeLanguage);
		cache						= new IEFGlobalCache();
		util					    = new MCADMxUtil(context, serverResourceBundle, cache);
//NDM end
		if(paramList != null)
			integrationNameGCOTable	= (HashMap)paramList.get("GCOTable");

		if(integrationNameGCOTable == null)
			integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");

		return relBusObjPageList;
	}

	public Object getHtmlString(Context context, String[] args) throws Exception
	{
		List relBusObjPageList 	  = initialize(context, args);
		String [] objIds 		  =  new String[relBusObjPageList.size()];

		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");
		}

		return  getHtmlStringForTable(context, objIds);	
	}

	public Object getHtmlStringForFrameworkTable(Context context, String[] args) throws Exception
	{
		List relBusObjPageList 	  = initialize(context, args);
		String [] objIds 		  =  new String[relBusObjPageList.size()];

		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");
		}

		return getHtmlStringForTable(context, objIds);	
	}

	public Object getHtmlStringForTable(Context context, String [] objIds) throws Exception
	{
		Vector columnCellContentList = new Vector();

		try
		{
			REL_VERSION_OF				= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
			SELECT_ON_MAJOR				= "from[" + REL_VERSION_OF + "].to.";
			ATTR_SOURCE					= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";
			String IS_VERSION_OBJ 			= MCADMxUtil.getActualNameForAEFData(context,"attribute_IsVersionObject");
			String SELECT_ISVERSIONOBJ 	= "attribute["+IS_VERSION_OBJ+"]";
			

			StringList busSelectionList = new StringList(6);

			busSelectionList.addElement("id");
			busSelectionList.addElement("type");

			busSelectionList.addElement(ATTR_SOURCE); //To get Integrations name
			busSelectionList.addElement("revision"); //from major
			busSelectionList.addElement(SELECT_ON_MAJOR + "revision"); //from minor
			busSelectionList.addElement(SELECT_ISVERSIONOBJ);

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{			
				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);

				String revision				= "";
				String integrationName		= null;

				String busType				= busObjectWithSelect.getSelectData("type");
				String integrationSource	= busObjectWithSelect.getSelectData(ATTR_SOURCE);
				String sIsVersion= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
				boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
				//NDM line
				String busId				= busObjectWithSelect.getSelectData("id");

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}
				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName))
				{
					MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
					if(!isVersion || gco.isTemplateType(busType))  // [NDM] is Major Object
					{
						revision = busObjectWithSelect.getSelectData("revision");
					}
					else
					{
						revision = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revision");
					}
				}
				else
				{
					revision = busObjectWithSelect.getSelectData("revision");
				}

				if("CSV".equalsIgnoreCase(reportFormat))
				{ 
					columnCellContentList.add("=\""+revision+"\"");
				} else {
					columnCellContentList.add(revision);
				}
			} 
		}
		catch(Exception e) 
		{

		}

		return columnCellContentList;
	}

	public Object getLatestRevisionForFrameworkTable(Context context, String[] args) throws Exception
	{
		String	paramKey			= "latestid";
		List relBusObjPageList 		= initialize(context, args);

		String [] objIds 			= new String[relBusObjPageList.size()];
		try
		{	
			if(!MCADUtil.checkForDataExistense(paramKey, relBusObjPageList))
			{
				MCADIntegGetLatestVersion_mxJPO objGetLatestJPO	= new MCADIntegGetLatestVersion_mxJPO(context, integrationNameGCOTable, localeLanguage);

				for(int i = 0; i < relBusObjPageList.size(); i++)
				{
					Map idMap 			= (Map)relBusObjPageList.get(i);
					objIds[i]	 		= idMap.get("id").toString();
				}
				Map returnTable			= objGetLatestJPO.getLatestForObjectIds(context, objIds);

				for(int i = 0; i < relBusObjPageList.size(); i++)
				{
					Map idMap 			= (Map)relBusObjPageList.get(i);
					String busId		= idMap.get("id").toString();
					String latestId		= (String)returnTable.get(busId);
					objIds[i]			= latestId;

					idMap.put("latestid", latestId);
				}
			}
			else
			{
				for(int i = 0; i < relBusObjPageList.size(); i++)
				{
					Map idMap 			= (Map)relBusObjPageList.get(i);
					objIds[i]			= idMap.get(paramKey).toString();
				}
			}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}

		return getHtmlStringForTable(context, objIds);
	}	
}

