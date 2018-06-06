/*
 **  DSCShowColumnLabel
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
import java.util.Locale;
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
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.util.MapList;

public class DSCShowColumnLabel_mxJPO
{
	private HashMap integrationNameGCOTable					= null;
	private MCADServerResourceBundle serverResourceBundle	= null;
	private MCADMxUtil util									= null;
	private String localeLanguage							= null;
	private IEFGlobalCache cache							= null;
	public String attrMoveFilesToVersion					= "";
	public String attrIsVersionObject						= "";

	// IR-492366-3DEXPERIENCER2015x, Feb 07,2017
	// Implementation of cache for NLS
	// Fetching of NLS string for each object state is expensive
	// This map stores the NLS and name of the state
	private static  HashMap _StateNLSMap						= null;

	public DSCShowColumnLabel_mxJPO(Context context, String[] args) throws Exception
	{
	}

	private boolean isVersionObject(HashMap objectMap)
	{
		String result = (String)objectMap.get(attrIsVersionObject);

		if (result == null || result.length() < 0)
			return false;
		return result.equalsIgnoreCase("true");
	}

	private boolean fileInMinor(HashMap objectMap)
	{
		String result = (String)objectMap.get(attrMoveFilesToVersion);
		if (result == null || result.length() < 0)
			return false;
		return result.equalsIgnoreCase("true");
	}

	private List initialize(Context context, String[] args) throws Exception
	{
		HashMap paramMap			= (HashMap)JPO.unpackArgs(args);  
		MapList relBusObjPageList	= (MapList)paramMap.get("objectList");
		
		HashMap paramList 			= (HashMap)paramMap.get("paramList");

		if(paramList != null)
		{
			integrationNameGCOTable	= (HashMap)paramList.get("GCOTable");
						
			if(paramList.containsKey("languageStr"))
				localeLanguage	   	= paramList.get("languageStr").toString();
			else
			{
				localeLanguage		= (String)paramList.get("LocaleLanguage");
				
				if(localeLanguage == null || localeLanguage.equals(""))
				{
						Locale LocaleObj	= (Locale)paramList.get("localeObj");

						if(null != LocaleObj)
						{
							localeLanguage = LocaleObj.toString();
						}
				}
			}
		}

		if(integrationNameGCOTable == null)
			integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");

		serverResourceBundle		= new MCADServerResourceBundle(localeLanguage);
		cache						= new IEFGlobalCache();
		util					    = new MCADMxUtil(context, serverResourceBundle, cache);

		return  relBusObjPageList;
	}

	//IR-492366-3DEXPERIENCER2015x, Feb 07,2017
	private static String getNLSFromMap(String iNameKey )
	{
		String nlsName = null;
			
		if (null != _StateNLSMap && _StateNLSMap.containsKey(iNameKey))
			nlsName = (String)_StateNLSMap.get(iNameKey);
		
		return nlsName;
	}
	
	//IR-492366-3DEXPERIENCER2015x, Feb 07,2017
	private static void updateNLSNameInMap(String iNameKey, String iValue)
	{
		if (null == _StateNLSMap)
			_StateNLSMap = new HashMap();
		
		if (null != _StateNLSMap && !_StateNLSMap.containsKey(iNameKey))
			_StateNLSMap.put(iNameKey, iValue);
	}

	// DSC Changes 10.6 SP1
	public Object getWhereUsedVersionLabel(Context context, String[] args) throws Exception
	{
		DSC_CommonUtil_mxJPO jpoUtil	= new DSC_CommonUtil_mxJPO();

		Vector columnCellContentList	= new Vector();

		List relBusObjPageList 			= initialize(context,args);

		for(int i =0 ; i<relBusObjPageList.size(); i++)
		{
			String version = "";

			try
			{
				Map objDetails			= (Map)relBusObjPageList.get(i);
				String objectId			= (String)objDetails.get("id");
				String integrationName	= util.getIntegrationName(context, objectId);				
				attrIsVersionObject		= MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
				attrMoveFilesToVersion	= MCADMxUtil.getActualNameForAEFData(context, "attribute_MoveFilesToVersion");

				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName))
				{
					MCADGlobalConfigObject globalConfigObject              = (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
					if (null != globalConfigObject && false == globalConfigObject.isCreateVersionObjectsEnabled())
					{
						version = "--";
					}
					else
					{
						BusinessObject busObject = new BusinessObject(objectId);
						busObject.open(context);

						BusinessObject majorBusObject = util.getMajorObject(context, busObject);
						if(majorBusObject != null)
						{
							majorBusObject.open(context);
							String majorRevision = majorBusObject.getRevision();
							String minorRevision = busObject.getRevision();

							version = MCADUtil.getVersionFromMinorRevision(majorRevision, minorRevision);
							majorBusObject.close(context);
						}		
						else
						{
							HashMap attrMap = jpoUtil.getCommonDocumentAttributes(context, objectId);

							String retObjectId = objectId;
							if (isVersionObject(attrMap) == false)
							{
								version = "";
							}
							else
							{
								if (fileInMinor(attrMap) == false)
									retObjectId = util.getActiveVersionObject(context, objectId);
								else
									retObjectId = util.getLatestMinorID(context, busObject);
								if (retObjectId != null)
								{
									BusinessObject minorObject = new BusinessObject(retObjectId);
									minorObject.open(context);
									String minorRevision = minorObject.getRevision();
									int pos = minorRevision.indexOf('.');
									if (pos > 0)  
									{
										version = minorRevision.substring(pos+1, minorRevision.length());
									}
									minorObject.close(context);
								}
							}
						}

						busObject.close(context);
					}
				}
				else 
				{
					HashMap attrMap = jpoUtil.getCommonDocumentAttributes(context, objectId);
					BusinessObject busObject = null;
					if (isVersionObject(attrMap))
						busObject = new BusinessObject(objectId);
					else
						busObject = new BusinessObject(util.getActiveVersionObject(context, objectId));
					if (busObject != null)
					{
						busObject.open(context);                                                     
						String minorVersion = busObject.getRevision();

						if (minorVersion != null)
						{
							int pos = minorVersion.indexOf('.');
							if (pos > 0)
								version = minorVersion.substring(pos+1);
							else
								version = minorVersion;
						}

						busObject.close(context);

					}
				}
			} 
			catch(Exception e) 
			{

			}

			columnCellContentList.add(version);
		}

		return columnCellContentList;
	}

	public Object getMajorStateLabel(Context context, String[] args) throws Exception
	{
		List relBusObjPageList 	  = initialize(context,args);		
		String [] objIds 		  =  new String[relBusObjPageList.size()];	

		for(int i =0; i<relBusObjPageList.size(); i++)
		{	
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");
		}

		return getMajorStateLabelForTable(context, objIds);
	}

	public Object getMajorStateLabelForFrameworkTable(Context context, String[] args) throws Exception
	{
		List relBusObjPageList = initialize(context,args);		
		String[] objIds					= new String[relBusObjPageList.size()];

		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");
		}

		return getMajorStateLabelForTable(context, objIds);
	}

	public Object getMajorStateLabelForTable(Context context, String [] objIds) throws Exception
	{
		Vector columnCellContentList = new Vector();		
		try
		{			
			String relVersionOf				= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");			
			String SELECT_ON_VERSIONOF_REL = "from[" + relVersionOf + "]";
			String SELECT_ON_MAJOR			= SELECT_ON_VERSIONOF_REL + ".to.";
			String attrSource				= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";

			/*	Latest Version information is fetched to support bulk loading object where VersonOf Relationship not exist	*/
			String REL_LATEST_VERSION		= MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion");
			String SELECT_LATEST_VERSION	= "to[" + REL_LATEST_VERSION + "].from.";			
	
			String IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context,"attribute_IsVersionObject");
			String SELECT_ISVERSIONOBJ = "attribute["+IS_VERSION_OBJ+"]";
			
			StringList busSelectionList = new StringList(11);

			busSelectionList.addElement("id");
			busSelectionList.addElement("type");

			busSelectionList.addElement(attrSource); //To get Integrations name
			busSelectionList.addElement("current"); //from major
			busSelectionList.addElement(SELECT_ON_VERSIONOF_REL); //from minor for Bulk loading check
			busSelectionList.addElement(SELECT_ON_MAJOR + "current"); //from minor
			busSelectionList.addElement(SELECT_ISVERSIONOBJ);
			busSelectionList.addElement("policy"); //from major
			busSelectionList.addElement(SELECT_ON_MAJOR + "policy"); //from minor
			
			busSelectionList.addElement(SELECT_LATEST_VERSION + "current"); //from minor
			busSelectionList.addElement(SELECT_LATEST_VERSION + "policy"); //from minor					

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{			
				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
				String policyName			="";
				String stateLabel			= "";
				String integrationName		= null;

				String busType				= busObjectWithSelect.getSelectData("type");
				String integrationSource	= busObjectWithSelect.getSelectData(attrSource);
				// [NDM]
				String busId				= busObjectWithSelect.getSelectData("id");
				String sIsVersion= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
				boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}

				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName))
				{
					MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
					// [NDM]
					if(!isVersion || gco.isTemplateType(busType))
					{
						stateLabel = busObjectWithSelect.getSelectData("current");
						policyName 	= busObjectWithSelect.getSelectData("policy");
					}
					else
					{
						/*	Extra Check is added to support bulk loading object where VersonOf Relationship not exist	*/
						String isVersionOfExist = busObjectWithSelect.getSelectData(SELECT_ON_VERSIONOF_REL);
						if(!MCADUtil.getBoolean(isVersionOfExist))
			        	{						
							stateLabel = busObjectWithSelect.getSelectData(SELECT_LATEST_VERSION + "current");
							policyName 	= busObjectWithSelect.getSelectData(SELECT_LATEST_VERSION + "policy");						
						}else
						{
							stateLabel = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "current");
							policyName 	= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "policy");
						}
					}
				}
				else
				{
					stateLabel = busObjectWithSelect.getSelectData("current");
				}

				//IR-492366-3DEXPERIENCER2015x, Feb 07,2016
				// Try to fetch the NLS from MAP 
				String tmpNameKey = stateLabel;
				String tmpStateLabel =  DSCShowColumnLabel_mxJPO.getNLSFromMap(stateLabel);
										
				if (null != tmpStateLabel && !tmpStateLabel.isEmpty())
					stateLabel = tmpStateLabel;
				else{
				stateLabel = MCADMxUtil.getNLSName(context, "State", stateLabel, "Policy", policyName , localeLanguage);
					DSCShowColumnLabel_mxJPO.updateNLSNameInMap(tmpNameKey, stateLabel);
				}
				
				columnCellContentList.add(stateLabel);
			}
		}
		catch(Exception e) 
		{

		}

		return columnCellContentList;
	}
	public Object getLatestMajorStateForFrameworkTable(Context context, String[] args) throws Exception
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
				Map returnTable	= objGetLatestJPO.getLatestForObjectIds(context, objIds);

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

		return getMajorStateLabelForTable(context, objIds);
	}

	public Object getStateLabelForFrameworkTable(Context context, String[] args) throws Exception
	{
		List relBusObjPageList = initialize(context,args);		
		String[] objIds					= new String[relBusObjPageList.size()];

		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");
		}

		return getStateLabelForTable(context, objIds);
	}

	public Object getStateLabelForTable(Context context, String [] objIds) throws Exception
	{
		Vector columnCellContentList = new Vector();

		try
		{
			String attrSource				= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";

			StringList busSelectionList = new StringList();

			busSelectionList.addElement("id");
			busSelectionList.addElement("type");

			busSelectionList.addElement(attrSource); //To get Integrations name
			busSelectionList.addElement("current"); 
			busSelectionList.addElement("policy");
			

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{			
				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
				String policyName			="";
				String stateLabel			= "";
				String integrationName		= null;

				String busType				= busObjectWithSelect.getSelectData("type");
				String integrationSource	= busObjectWithSelect.getSelectData(attrSource);

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}

				stateLabel = busObjectWithSelect.getSelectData("current");
				policyName 	= busObjectWithSelect.getSelectData("policy");
				
				//IR-492366-3DEXPERIENCER2015x, Feb 07,2016
				// Try to fetch the NLS from MAP 
				String tmpNameKey = stateLabel;
				String tmpStateLabel =  DSCShowColumnLabel_mxJPO.getNLSFromMap(stateLabel);
				
				if (null != tmpStateLabel && !tmpStateLabel.isEmpty())
					stateLabel = tmpStateLabel;
				else{
				stateLabel = MCADMxUtil.getNLSName(context, "State", stateLabel, "Policy", policyName , localeLanguage);
					DSCShowColumnLabel_mxJPO.updateNLSNameInMap(tmpNameKey, stateLabel);
				}
				
				columnCellContentList.add(stateLabel);
			}
		}
		catch(Exception e) 
		{

		}

		return columnCellContentList;
	}
}

