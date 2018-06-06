/*
 **  IEFShowVersionLabel
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to display version label.
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

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.util.MapList;

public class IEFShowVersionLabel_mxJPO
{
	private HashMap integrationNameGCOTable					= null;

	private String REL_VERSION_OF							= "";
	private String REL_ACTIVE_VERSION						= "";
	private String REL_LATEST_VERSION						= "";
	private String SELECT_ON_ACTIVE_MINOR					= "";
	private	String ATTR_SOURCE								= "";
	private String localeLanguage							= "";

	//NDM 3line
	private MCADMxUtil util									= null;
	private IEFGlobalCache cache							= null;
	private MCADServerResourceBundle serverResourceBundle	= null;
	private String IS_VERSION_OBJ = "";
	private String SELECT_ISVERSIONOBJ = "";
	
public IEFShowVersionLabel_mxJPO(Context context, String[] args) throws Exception
	{
		REL_VERSION_OF 			= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		REL_LATEST_VERSION		= MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion");
		REL_ACTIVE_VERSION		= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
		SELECT_ON_ACTIVE_MINOR	= "from[" + REL_ACTIVE_VERSION + "].to.";
		ATTR_SOURCE				= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";
		 IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context,"attribute_IsVersionObject");
		 SELECT_ISVERSIONOBJ = "attribute["+IS_VERSION_OBJ+"]";
	}

	private List initialize(Context context, String[] args) throws Exception 
	{
		HashMap paramMap			= (HashMap)JPO.unpackArgs(args);  
		MapList relBusObjPageList	= (MapList)paramMap.get("objectList");					
		localeLanguage				= (String)paramMap.get("LocaleLanguage");
		HashMap paramList 			= (HashMap)paramMap.get("paramList");

		integrationNameGCOTable		= (HashMap)paramList.get("GCOTable");

		if(integrationNameGCOTable == null)
			integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");

//NDM start
		serverResourceBundle		= new MCADServerResourceBundle(localeLanguage);
		cache						= new IEFGlobalCache();
		util					    = new MCADMxUtil(context, serverResourceBundle, cache);
//NDM end

		return relBusObjPageList;
	}

	public Object getHtmlString(Context context, String[] args) throws Exception
	{			
		List relBusObjPageList 		= initialize(context,args);
		String[] objIds				= new String[relBusObjPageList.size()];

		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");
		}

		return getHtmlStringForTable(context, objIds);
	}

	public Object getHtmlStringForFrameworkTable(Context context, String[] args) throws Exception
	{
		List relBusObjPageList 		= initialize(context,args);
		String[] objIds				= new String[relBusObjPageList.size()];


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
			StringList busSelectionList = new StringList(5);

			busSelectionList.addElement("id");
			
			busSelectionList.addElement(ATTR_SOURCE); //To get Integrations name
			busSelectionList.addElement("revision"); //from minor
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + "revision"); //from major
			busSelectionList.addElement(SELECT_ISVERSIONOBJ);

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{			
				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);

				String sIsVersion = busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
				boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
				
				String version				= "";
				String integrationName		= null;

				String busId				= busObjectWithSelect.getSelectData("id");
				String integrationSource	= busObjectWithSelect.getSelectData(ATTR_SOURCE);

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}
				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName))
				{
					MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

					if (null != gco && false == gco.isCreateVersionObjectsEnabled())
					{
						version = "--";
					}

					else
					{
						String minorVersion = "";
						if(!isVersion)  // [NDM] is Major Object
						{
							minorVersion = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + "revision");
						}		
						else
						{
							minorVersion = busObjectWithSelect.getSelectData("revision");
						}

						if (minorVersion != null)
						{
							int pos = minorVersion.lastIndexOf('.');
							if (pos > -1)
								version = minorVersion.substring(pos+1);
							else
								version = minorVersion;
						}
					}
				}
				else 
				{
					String minorVersion = busObjectWithSelect.getSelectData("revision");

					if (minorVersion != null)
					{
						int pos = minorVersion.lastIndexOf('.');
						if (pos > -1)
							version = minorVersion.substring(pos+1);
						else
						{
							minorVersion = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + "revision");
							pos = minorVersion.lastIndexOf('.');
							if (pos > -1)
								version = minorVersion.substring(pos+1);
							else				
								version = minorVersion;
						}
					}
				}
				columnCellContentList.add(version);
			} 
		}
		catch(Exception e) 
		{
		}

		return columnCellContentList;
	}

	public Object getLatestVersion(Context context, String[] args) throws Exception
	{
		Vector columnLatestVersionList 						= new Vector();
		BusinessObjectWithSelectList buslWithSelectionList	= null;

		HashMap paramMap 			= (HashMap)JPO.unpackArgs(args);
		List relBusObjPageList		= (List)paramMap.get("objectList");
		String[] objIds				= new String[relBusObjPageList.size()];

		for(int i = 0; i < relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");			
		}

		String SELECT_ON_MAJOR		= "from[" + REL_VERSION_OF + "].to.";
		String SELECT_ON_LATEST		= "from[" + REL_LATEST_VERSION + "].to.";
		String REVISION_ON_MAJOR	= SELECT_ON_LATEST + "revision";
		String REVISION_ON_MINOR	= SELECT_ON_MAJOR + SELECT_ON_LATEST + "revision";

		StringList busSelectionList = new StringList();
		busSelectionList.addElement("id");
		busSelectionList.addElement(REVISION_ON_MAJOR);  	// latest minor revision from major
		busSelectionList.addElement(REVISION_ON_MINOR);  	// latest minor revision from minor

		buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
			String revision = busObjectWithSelect.getSelectData(REVISION_ON_MINOR);

			// if revision is null then objectid is major
			if(revision == null || revision.trim().equals(""))
				revision = busObjectWithSelect.getSelectData(REVISION_ON_MAJOR);

			columnLatestVersionList.add(revision);
		}

		return columnLatestVersionList;
	}

	private Object getActiveVersion(Context context, String[] objIds) throws Exception 
	{
		Vector columnLatestVersionList  = new Vector();

		StringList busSelectionList 	= new StringList(5);

		busSelectionList.addElement("id");
		busSelectionList.addElement(ATTR_SOURCE); //To get Integrations name
		busSelectionList.addElement("revision"); //from minor
		busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + "revision"); //from major
		busSelectionList.addElement(SELECT_ISVERSIONOBJ);

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
			String busId			= busObjectWithSelect.getSelectData("id");
			String integrationSource					 = busObjectWithSelect.getSelectData(ATTR_SOURCE);

			String sIsVersion= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
			boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
			
			String revision 		= ""; 
			String integrationName 	= null;

			if(integrationSource != null)
			{
				StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

				if(integrationSourceTokens.hasMoreTokens())
					integrationName  = integrationSourceTokens.nextToken();
			}

			if(integrationName != null && integrationNameGCOTable.containsKey(integrationName))
			{
				
				if(!isVersion)  // [NDM] is Major Object
					revision = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + "revision");

				else
					revision = busObjectWithSelect.getSelectData("revision");
			}

			columnLatestVersionList.add(revision);
		}

		return columnLatestVersionList;
	}

	public Object getLatestVersionAcrossRevision(Context context, String[] args) throws Exception
	{
		String	paramKey			= "latestid";

		List relBusObjPageList 		= initialize(context,args);
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
					
					if(latestId != null && !latestId.equals(""))
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

		return getActiveVersion(context, objIds);
	}
}

