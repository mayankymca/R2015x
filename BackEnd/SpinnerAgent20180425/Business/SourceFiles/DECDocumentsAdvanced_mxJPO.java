/*
 **  ${CLASSNAME}
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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
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
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;

public class DECDocumentsAdvanced_mxJPO
{
	private HashMap integrationNameGCOTable					= null;
	private MCADServerResourceBundle serverResourceBundle	= null;	
	private IEFIntegAccessUtil util					= null;
	private IEFGlobalCache cache							= null;
	private String localeLanguage							= null;
	protected MCADServerGeneralUtil generalUtil				= null;
	protected MCADGlobalConfigObject globalConfigObject		= null;

	private static int LOCKPATHCONST						= 0;
	private static int COMPNAMECONST						= 1;
	private static int LATESTVERSIONCONST					        = 2;
	private String IS_VERSION_OBJ = "";
	private String SELECT_ISVERSIONOBJ = "";


	public DECDocumentsAdvanced_mxJPO(Context context, String[] args) throws Exception
	{
		 IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context,"attribute_IsVersionObject");
		 SELECT_ISVERSIONOBJ = "attribute["+IS_VERSION_OBJ+"]";
	}	
	
	public Object getLockPathValue(Context context, String[] args) throws Exception
	{
		return getComputerNLockpathInfo(context,args,LOCKPATHCONST);
	}

	public Object getComputerNameValue(Context context, String[] args) throws Exception
	{
		return getComputerNLockpathInfo(context,args,COMPNAMECONST);
	}

	public Object getLatestVersionValue(Context context, String[] args) throws Exception
	{
		return getComputerNLockpathInfo(context,args,LATESTVERSIONCONST);
	}

	public Object getComputerNLockpathInfo(Context context, String[] args,int iOptions) throws Exception
	{
		Vector columnCellContentList = new Vector();

		try
		{

			HashMap paramMap 			= (HashMap)JPO.unpackArgs(args);
			MapList relBusObjPageList	= (MapList)paramMap.get("objectList");
			HashMap paramList 			= (HashMap)paramMap.get("paramList");

			if(paramList != null)
			{
				integrationNameGCOTable	= (HashMap)paramList.get("GCOTable");
				localeLanguage			= (String)paramList.get("LocaleLanguage");
			}

			if(localeLanguage == null)
				localeLanguage = (String)paramMap.get("LocaleLanguage");

			if(integrationNameGCOTable == null)
				integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");

			String[] objIds	= new String[relBusObjPageList.size()];
			for(int i =0; i<relBusObjPageList.size(); i++)
			{
				Map objDetails	= (Map)relBusObjPageList.get(i);
				objIds[i]		= (String)objDetails.get("id");			
			}

			StringList busSelectionList = new StringList(8);
			String ATTR_SOURCE	   		  = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";
			String ATTR_LOCKINFO				= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-LockInformation") + "]";
			String relLatestVersion = MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion");
			String relVersionOf= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
			String SELECT_LATEST_VERSION_ID = "from[" + relLatestVersion + "].to.revision";
			String SELECT_MAJOR_FROM_ID = "from[" + relVersionOf + "].to.";

			busSelectionList.addElement("type");
			busSelectionList.addElement("id");   // [NDM] OP6
			busSelectionList.addElement(ATTR_SOURCE);
			busSelectionList.addElement(SELECT_ISVERSIONOBJ);

			if(iOptions == LOCKPATHCONST || iOptions == COMPNAMECONST)
			{
				busSelectionList.addElement(ATTR_LOCKINFO);
				busSelectionList.addElement(SELECT_MAJOR_FROM_ID + ATTR_LOCKINFO);
			}

			if(iOptions == LATESTVERSIONCONST )
			{
				busSelectionList.addElement(SELECT_LATEST_VERSION_ID);
				busSelectionList.addElement(SELECT_MAJOR_FROM_ID + SELECT_LATEST_VERSION_ID);
			}

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{
				BusinessObjectWithSelect busObjectWithSelect = buslWithSelectionList.getElement(i);

				String integrationName	 = null;
				String attributeData = "";
				String integrationSource = busObjectWithSelect.getSelectData(ATTR_SOURCE);
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
					cache						= new IEFGlobalCache();
					serverResourceBundle		= new MCADServerResourceBundle(localeLanguage);
					util						= new IEFIntegAccessUtil(context, serverResourceBundle, cache);

					String busType = busObjectWithSelect.getSelectData("type");
					String busId = busObjectWithSelect.getSelectData("id");

					if(iOptions == LOCKPATHCONST)
					{
						if(!isVersion)//gco.isMajorType(busType)) // [NDM] OP6
						{
							attributeData = busObjectWithSelect.getSelectData(ATTR_LOCKINFO);
							if(attributeData.length() > 0 && attributeData.contains("|"))
								attributeData = attributeData.substring(attributeData.indexOf("|")+1, attributeData.length());
						}
						else
						{
							attributeData = busObjectWithSelect.getSelectData(SELECT_MAJOR_FROM_ID + ATTR_LOCKINFO);
							if(attributeData.length() > 0 && attributeData.contains("|"))
								attributeData = attributeData.substring(attributeData.indexOf("|")+1, attributeData.length());
						}
					}
					else if(iOptions == COMPNAMECONST)
					{
						if(!isVersion)//gco.isMajorType(busType)) // [NDM] OP6
						{
							attributeData = busObjectWithSelect.getSelectData(ATTR_LOCKINFO);
							if(attributeData.length() > 0 && attributeData.contains("|"))
								attributeData = attributeData.substring(0, attributeData.indexOf("|"));
						}
						else
						{
							attributeData = busObjectWithSelect.getSelectData(SELECT_MAJOR_FROM_ID + ATTR_LOCKINFO);
							if(attributeData.length() > 0 && attributeData.contains("|"))
								attributeData = attributeData.substring(0, attributeData.indexOf("|"));
						}
					}
					else if(iOptions == LATESTVERSIONCONST )
					{
						if(!isVersion)//gco.isMajorType(busType)) // [NDM] OP6
							attributeData = busObjectWithSelect.getSelectData(SELECT_LATEST_VERSION_ID);
						else
							attributeData = busObjectWithSelect.getSelectData(SELECT_MAJOR_FROM_ID + SELECT_LATEST_VERSION_ID);
					}

				}
				columnCellContentList.add(attributeData);	
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return 	columnCellContentList;
	}
	private StringList getBusSelectListForChildCount(Hashtable relsAndEnds)
	{
		StringList busSelectionList = new StringList();

		Enumeration relList 		= relsAndEnds.keys();

		// For expanding
		while(relList.hasMoreElements())
		{
			String relName          = (String)relList.nextElement();

			StringList relExpnSelects   =  new StringList();

			String relEnd   = (String)relsAndEnds.get(relName);
			String expEnd = "";

			if(relEnd.equals("to"))
				expEnd = "from";
			else
				expEnd = "to";

			relExpnSelects.addElement(expEnd + "[" + relName + "].id"); // rel id

			busSelectionList.addAll(relExpnSelects);
		}


		return busSelectionList;
	}

	private int getRelCount(String selectPrefix, BusinessObjectWithSelect busObjectWithSelect, HashSet busselectsForRelIds)
	{
		int childCount = 0;

		Iterator busSelectForRelIdItr = busselectsForRelIds.iterator();

		while (busSelectForRelIdItr.hasNext())
		{
			String busSelectForRelId = selectPrefix + (String) busSelectForRelIdItr.next();

			StringList relIdList = busObjectWithSelect.getSelectDataList(busSelectForRelId);

			if(relIdList != null)
				childCount = childCount + relIdList.size();
		}

		return childCount;
	}

	public Object getChildCount(Context context, String[] args) throws MCADException
	{
		Vector returnVector = new Vector();

		try
		{			
			HashMap paramMap 			= (HashMap)JPO.unpackArgs(args);

			MapList relBusObjPageList	= (MapList)paramMap.get("objectList");
			cache						= new IEFGlobalCache();
			serverResourceBundle		= new MCADServerResourceBundle(localeLanguage);
			util						= new IEFIntegAccessUtil(context, serverResourceBundle, cache);
			HashMap paramList 			= (HashMap)paramMap.get("paramList");

			if(paramList != null)
			{
				localeLanguage			= (String)paramList.get("LocaleLanguage");
				integrationNameGCOTable	= (HashMap)paramList.get("GCOTable");
			}

			if(integrationNameGCOTable == null)
				integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");

			if(localeLanguage == null)
				localeLanguage = (String)paramMap.get("LocaleLanguage");

			String[] objIds	= new String[relBusObjPageList.size()];
			for(int i =0; i<relBusObjPageList.size(); i++)
			{
				Map objDetails	= (Map)relBusObjPageList.get(i);
				objIds[i]		= (String)objDetails.get("id");			
			}

			String REL_ACTIVE_MINOR		  = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");

			String SELECT_ON_MAJOR_THROUGH_ACTIVE_MINOR = "to[" + REL_ACTIVE_MINOR + "].from.";

			String SELECT_ON_ACTIVE_MINOR = "from[" + REL_ACTIVE_MINOR + "].to.";

			String ATTR_SOURCE	   		  = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";

			StringList busSelectionList = new StringList(4);

			busSelectionList.addElement("id");
			busSelectionList.addElement("type");
			busSelectionList.addElement(ATTR_SOURCE); 
			busSelectionList.addElement(SELECT_ISVERSIONOBJ);

			HashSet busselectsForRelIds = new HashSet();
			Iterator integrationGCOItr = integrationNameGCOTable.keySet().iterator();

			while (integrationGCOItr.hasNext())
			{
				String integrationName = (String) integrationGCOItr.next();
				MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

				Hashtable relsAndEnds = new Hashtable(gco.getRelationshipsOfClass(MCADServerSettings.ASSEMBLY_LIKE));

				Hashtable externalReferenceLikeRelsAndEnds = gco.getRelationshipsOfClass(MCADServerSettings.EXTERNAL_REFERENCE_LIKE);

				Enumeration externalReferenceRels = externalReferenceLikeRelsAndEnds.keys();
				while (externalReferenceRels.hasMoreElements())
				{
					String relName = (String) externalReferenceRels.nextElement();

					if(relsAndEnds.containsKey(relName))
						relsAndEnds.remove(relName);
				}

				StringList integBusselectsForRelIds = this.getBusSelectListForChildCount(relsAndEnds);

				busselectsForRelIds.addAll(integBusselectsForRelIds);
			}

			Iterator busSelectForRelIdItr = busselectsForRelIds.iterator();

			while (busSelectForRelIdItr.hasNext())
			{
				String busSelectForRelId = (String) busSelectForRelIdItr.next();

				busSelectionList.addElement(busSelectForRelId);

				busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + busSelectForRelId);

				busSelectionList.addElement(SELECT_ON_MAJOR_THROUGH_ACTIVE_MINOR + busSelectForRelId);
			}

			Vector assignedIntegrations = util.getAssignedIntegrations(context);

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{
				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);	

				String integrationName	 = null;

				String integrationSource = busObjectWithSelect.getSelectData(ATTR_SOURCE);
				
				String sIsVersion= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
				boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
				
				int totalChildCount	 = 0;

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}

				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
				{					
					MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

					String busType = busObjectWithSelect.getSelectData("type");
					String busId = busObjectWithSelect.getSelectData("id");

					if(!isVersion)//gco.isMajorType(busType)) // [NDM] OP6
					{
						totalChildCount = getRelCount("", busObjectWithSelect, busselectsForRelIds);

						if(totalChildCount == 0)
							totalChildCount = getRelCount(SELECT_ON_ACTIVE_MINOR, busObjectWithSelect, busselectsForRelIds);
					}
					else
					{
						totalChildCount = getRelCount(SELECT_ON_MAJOR_THROUGH_ACTIVE_MINOR, busObjectWithSelect, busselectsForRelIds);

						if(totalChildCount == 0)
							totalChildCount = getRelCount("", busObjectWithSelect, busselectsForRelIds);
					}
				}

				returnVector.add(String.valueOf(totalChildCount));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return returnVector;

	}

	public Object getSizeInByte(Context context, String[] args) throws Exception
	{		
		Vector returnVector = new Vector();

		try
		{			
			HashMap paramMap = (HashMap)JPO.unpackArgs(args);

			MapList relBusObjPageList	= (MapList)paramMap.get("objectList");
			cache						= new IEFGlobalCache();
			serverResourceBundle		= new MCADServerResourceBundle(localeLanguage);
			util						= new IEFIntegAccessUtil(context, serverResourceBundle, cache);
			HashMap paramList 			= (HashMap)paramMap.get("paramList");

			if(paramList != null)
			{
				localeLanguage			= (String)paramList.get("LocaleLanguage");
				integrationNameGCOTable	= (HashMap)paramList.get("GCOTable");
			}

			if(integrationNameGCOTable == null)
				integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");

			if(localeLanguage == null)
				localeLanguage = (String)paramMap.get("LocaleLanguage");

			String[] objIds	= new String[relBusObjPageList.size()];
			for(int i =0; i<relBusObjPageList.size(); i++)
			{
				Map objDetails	= (Map)relBusObjPageList.get(i);
				objIds[i]		= (String)objDetails.get("id");			
			}

			String REL_ACTIVE_MINOR		  = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");

			String SELECT_ON_MAJOR_THROUGH_ACTIVE_MINOR = "to[" + REL_ACTIVE_MINOR + "].from.";

			String SELECT_ON_ACTIVE_MINOR = "from[" + REL_ACTIVE_MINOR + "].to.";

			String ATTR_SOURCE	   		  = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";
			String ATTR_CADTYPE	   		  = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType") + "]";
			String IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context,"attribute_IsVersionObject");
			String SELECT_ISVERSIONOBJ = "attribute["+IS_VERSION_OBJ+"]";
			

			StringList busSelectionList = new StringList();

			busSelectionList.addElement("id");
			busSelectionList.addElement("type");
			busSelectionList.addElement(ATTR_SOURCE); 
			busSelectionList.addElement(ATTR_CADTYPE);
			busSelectionList.addElement(SELECT_ISVERSIONOBJ);

			HashSet mxFormatForSelect = new HashSet();
			Iterator integrationGCOItr = integrationNameGCOTable.keySet().iterator();

			while (integrationGCOItr.hasNext())
			{
				String integrationName = (String) integrationGCOItr.next();
				MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

				Vector nativeCADFormats =  gco.getNativeCADMxFormats();

				mxFormatForSelect.addAll(nativeCADFormats);
			}

			Iterator mxFormatForSelectItr = mxFormatForSelect.iterator();

			while (mxFormatForSelectItr.hasNext())
			{
				String mxFormat = (String) mxFormatForSelectItr.next();

				String SELECT_FILE_SIZE_ON_FORMAT = "format[" + mxFormat + "].file.size";

				busSelectionList.addElement(SELECT_FILE_SIZE_ON_FORMAT);

				busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + SELECT_FILE_SIZE_ON_FORMAT);

				busSelectionList.addElement(SELECT_ON_MAJOR_THROUGH_ACTIVE_MINOR + SELECT_FILE_SIZE_ON_FORMAT);
			}

			Vector assignedIntegrations = util.getAssignedIntegrations(context);

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{
				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);	
				String sIsVersion= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
				boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();

				String integrationName	 = null;

				String integrationSource = busObjectWithSelect.getSelectData(ATTR_SOURCE);
				String totalfileSize	 = "0";
				StringList fileSizeList = null;

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}

				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
				{					
					MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

					String busType = busObjectWithSelect.getSelectData("type");
					String busId = busObjectWithSelect.getSelectData("id");
					String cadType = busObjectWithSelect.getSelectData(ATTR_CADTYPE);

					if(!isVersion)//gco.isMajorType(busType)) // [NDM] OP6
					{
						String fileFormat				  = gco.getFormatsForType(busType, cadType);

						String SELECT_FILE_SIZE_ON_FORMAT = "format[" + fileFormat + "].file.size";

						fileSizeList = busObjectWithSelect.getSelectDataList(SELECT_FILE_SIZE_ON_FORMAT);

						if(fileSizeList != null && fileSizeList.contains(""))
							fileSizeList.remove("");

						if(fileSizeList == null || fileSizeList.isEmpty())
							fileSizeList = busObjectWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + SELECT_FILE_SIZE_ON_FORMAT);
					}
					else
					{
						String majorType 				  = util.getCorrespondingType(context, busType);
						String fileFormat				  = gco.getFormatsForType(majorType, cadType);

						String SELECT_FILE_SIZE_ON_FORMAT = "format[" + fileFormat + "].file.size";

						fileSizeList 					  = busObjectWithSelect.getSelectDataList(SELECT_FILE_SIZE_ON_FORMAT);

						if(fileSizeList != null && fileSizeList.contains(""))
							fileSizeList.remove("");

						//Get it from the major 
						if((fileSizeList == null || fileSizeList.isEmpty()))
							fileSizeList = busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR_THROUGH_ACTIVE_MINOR + SELECT_FILE_SIZE_ON_FORMAT);
					}
				}

				if(fileSizeList != null && !fileSizeList.isEmpty())
				{
					long totalFileSizel = 0;

					for (int j = 0; j < fileSizeList.size(); j++)
					{
						String fileSize = (String)fileSizeList.elementAt(j);
						if(fileSize != null && fileSize.length() >  0)
						{
							totalFileSizel = totalFileSizel + Long.parseLong(fileSize);
						}
					}

					if(totalFileSizel > 0 )
						totalfileSize = String.valueOf(totalFileSizel);
				}

				returnVector.add(totalfileSize);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return returnVector;
	}

	public Object getInitialVersionId(Context context, String[] args) throws Exception
	{		
		Vector intialVersionIDsList = new Vector();

		try
		{			
			HashMap paramMap = (HashMap)JPO.unpackArgs(args);

			MapList relBusObjPageList	= (MapList)paramMap.get("objectList");
			cache						= new IEFGlobalCache();
			serverResourceBundle		= new MCADServerResourceBundle(localeLanguage);
			util						= new IEFIntegAccessUtil(context, serverResourceBundle, cache);
			HashMap paramList 			= (HashMap)paramMap.get("paramList");

			if(paramList != null)
			{
				localeLanguage			= (String)paramList.get("LocaleLanguage");
				integrationNameGCOTable	= (HashMap)paramList.get("GCOTable");
			}

			if(integrationNameGCOTable == null)
				integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");

			if(localeLanguage == null)
				localeLanguage = (String)paramMap.get("LocaleLanguage");

			String[] objIds	= new String[relBusObjPageList.size()];
			for(int i =0; i<relBusObjPageList.size(); i++)
			{
				Map objDetails	= (Map)relBusObjPageList.get(i);
				objIds[i]		= (String)objDetails.get("id");			
			}

			String REL_ACTIVE_MINOR		  = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
			String SELECT_ON_ACTIVE_MINOR = "from[" + REL_ACTIVE_MINOR + "].to.";

			StringList busSelectionList = new StringList();
			busSelectionList.add("first.id");
			busSelectionList.add(SELECT_ON_ACTIVE_MINOR + "first.id");
			busSelectionList.add(DomainConstants.SELECT_ID);

			BusinessObjectWithSelectList busWithSelectionList 	= 	BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);
			for(int i=0; i<busWithSelectionList.size(); i++)
			{
				BusinessObjectWithSelect busObjectWithSelect  =  (BusinessObjectWithSelect)busWithSelectionList.get(i);

				String intialVersionID 	 =	busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + "first.id");
				if(intialVersionID == null || intialVersionID.equals(""))
				{
					intialVersionID = busObjectWithSelect.getSelectData("first.id");
				}	

				if(intialVersionID == null)
					intialVersionID = "";

				intialVersionIDsList.add(intialVersionID);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return intialVersionIDsList;
	}

	/*Favorite is yet to be implemented*/
	public Object isFavorite(Context context, String[] args) throws Exception
	{
		HashMap paramMap = (HashMap)JPO.unpackArgs(args);

		MapList relBusObjPageList	= (MapList)paramMap.get("objectList");

		Vector isFavoriteList = new Vector();

		String[] busIds = new String[relBusObjPageList.size()];

		for(int i =0; i < relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			String objectID	= (String)objDetails.get("id");		
			busIds[i] = objectID;
		}

		String personId = PersonUtil.getPersonObjectID(context);
		String relation = MCADMxUtil.getActualNameForAEFData(context,"relationship_FavouriteOf");

		String IS_FAVORITE_SELECT = "evaluate[to[" + relation + "].from.id==\"" + personId + "\"]";

		StringList busSelect = new StringList();
		busSelect.add(IS_FAVORITE_SELECT);

		BusinessObjectWithSelectList busSelectList = BusinessObject.getSelectBusinessObjectData(context,busIds,busSelect);

		for(int i = 0; i < busSelectList.size(); i++)
		{
			BusinessObjectWithSelect busObjSelect = busSelectList.getElement(i);

			String isFavorite = busObjSelect.getSelectData(IS_FAVORITE_SELECT);

			if(isFavorite != null)
			{
				isFavoriteList.add(isFavorite.toLowerCase());
			}

		}

		return isFavoriteList;
	}

	public Object getPartNumber(Context context, String[] args) throws Exception
	{
		Vector partNumberList = new Vector();

		try
		{			
			HashMap paramMap = (HashMap)JPO.unpackArgs(args);

			MapList relBusObjPageList	= (MapList)paramMap.get("objectList");
			cache						= new IEFGlobalCache();
			serverResourceBundle		= new MCADServerResourceBundle(localeLanguage);
			util						= new IEFIntegAccessUtil(context, serverResourceBundle, cache);
			HashMap paramList 			= (HashMap)paramMap.get("paramList");

			HashMap ebomTNRebomConfigObjMap = new HashMap();

			if(paramList != null)
			{
				localeLanguage			= (String)paramList.get("LocaleLanguage");
				integrationNameGCOTable	= (HashMap)paramList.get("GCOTable");
			}

			if(integrationNameGCOTable == null)
				integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");

			if(localeLanguage == null)
				localeLanguage = (String)paramMap.get("LocaleLanguage");

			String[] objIds	= new String[relBusObjPageList.size()];
			for(int i =0; i<relBusObjPageList.size(); i++)
			{
				Map objDetails	= (Map)relBusObjPageList.get(i);
				objIds[i]		= (String)objDetails.get("id");			
			}

			String REL_VERSION_OF		  = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
			String REL_ACTIVE_MINOR		  = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
			String REL_PART_SPECIFICATION = MCADMxUtil.getActualNameForAEFData(context, "relationship_PartSpecification");

			String SELECT_ON_SPC_PART_NAME = "to[" + REL_PART_SPECIFICATION + "].from.name";

			String SELECT_ON_MAJOR		   = "from[" + REL_VERSION_OF + "].to.";

			String SELECT_ON_ACTIVE_MINOR  = "from[" + REL_ACTIVE_MINOR + "].to.";

			String ATTR_SOURCE	   		   = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";

			StringList busSelectionList = new StringList();

			busSelectionList.addElement("id");
			busSelectionList.addElement("type");
			busSelectionList.addElement(ATTR_SOURCE); 
			busSelectionList.addElement(SELECT_ON_SPC_PART_NAME); 
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + SELECT_ON_SPC_PART_NAME); 
			busSelectionList.addElement(SELECT_ON_MAJOR + SELECT_ON_SPC_PART_NAME); 
			busSelectionList.addElement(SELECT_ISVERSIONOBJ); 

			Vector assignedIntegrations = util.getAssignedIntegrations(context);

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{
				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);	
				String sIsVersion= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
				boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();

				String integrationName	 = null;

				String integrationSource = busObjectWithSelect.getSelectData(ATTR_SOURCE);
				String partName	 		= "";

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}

				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
				{	
					String type = busObjectWithSelect.getSelectData("type");
					String id	= busObjectWithSelect.getSelectData("id");

					MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

					// show the Part alert
					IEFEBOMConfigObject ebomConfigObject = null;

					String sEBOMRegistryTNR = gco.getEBOMRegistryTNR();

					if(!ebomTNRebomConfigObjMap.containsKey(sEBOMRegistryTNR))
					{
						StringTokenizer token = new StringTokenizer(sEBOMRegistryTNR, "|");

						if(token.countTokens() >= 3)
						{
							String sEBOMRConfigObjType			= (String) token.nextElement();
							String sEBOMRConfigObjName			= (String) token.nextElement();
							String sEBOMRConfigObjRev			= (String) token.nextElement();

							ebomConfigObject	= new IEFEBOMConfigObject(context, sEBOMRConfigObjType, sEBOMRConfigObjName, sEBOMRConfigObjRev);

							ebomTNRebomConfigObjMap.put(sEBOMRegistryTNR, ebomConfigObject);
						} 
					} 
					else
						ebomConfigObject = (IEFEBOMConfigObject) ebomTNRebomConfigObjMap.get(sEBOMRegistryTNR);

					String assignPartToMajor = "true";//ebomConfigObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_ASSIGN_PART_TO_MAJOR); [NDM]

					StringList partNames = busObjectWithSelect.getSelectDataList(SELECT_ON_SPC_PART_NAME);	

					if(partNames == null)
					{
						// [NDM] OP6
						if(!isVersion && assignPartToMajor.equalsIgnoreCase(MCADAppletServletProtocol.FALSE))
							partNames = busObjectWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + SELECT_ON_SPC_PART_NAME);
						else if(isVersion && assignPartToMajor.equalsIgnoreCase(MCADAppletServletProtocol.TRUE))
							partNames = busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR + SELECT_ON_SPC_PART_NAME);
					}

					if(partNames != null && !partNames.isEmpty())
						partName = (String)partNames.elementAt(0);
				}

				partNumberList.add(partName);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return partNumberList;
	}

	public Object getDateCreated(Context context, String[] args) throws Exception
	{
		Vector returnList = new Vector();

		try
		{			
			HashMap paramMap = (HashMap)JPO.unpackArgs(args);

			MapList relBusObjPageList	= (MapList)paramMap.get("objectList");
			cache						= new IEFGlobalCache();
			serverResourceBundle		= new MCADServerResourceBundle(localeLanguage);
			util						= new IEFIntegAccessUtil(context, serverResourceBundle, cache);
			HashMap paramList 			= (HashMap)paramMap.get("paramList");

			if(paramList != null)
			{
				localeLanguage			= (String)paramList.get("LocaleLanguage");
				integrationNameGCOTable	= (HashMap)paramList.get("GCOTable");
			}

			if(integrationNameGCOTable == null)
				integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");

			if(localeLanguage == null)
				localeLanguage = (String)paramMap.get("LocaleLanguage");

			String[] objIds	= new String[relBusObjPageList.size()];
			for(int i =0; i<relBusObjPageList.size(); i++)
			{
				Map objDetails	= (Map)relBusObjPageList.get(i);
				objIds[i]		= (String)objDetails.get("id");			
			}

			String REL_ACTIVE_MINOR		  = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");

			String SELECT_ON_ACTIVE_MINOR  = "from[" + REL_ACTIVE_MINOR + "].to.";

			String ATTR_SOURCE	   		   = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";


			StringList busSelectionList = new StringList(6);

			busSelectionList.addElement("id");
			busSelectionList.addElement("type");
			busSelectionList.addElement(ATTR_SOURCE); 
			busSelectionList.addElement("originated.generic"); 
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + "originated.generic"); 
			busSelectionList.addElement(SELECT_ISVERSIONOBJ); 

			Vector assignedIntegrations = util.getAssignedIntegrations(context);

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{
				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);	

				String sIsVersion= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
				boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
				
				String integrationName	 = null;

				String integrationSource = busObjectWithSelect.getSelectData(ATTR_SOURCE);
				String dateCreated  	 = busObjectWithSelect.getSelectData("originated.generic");

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}

				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
				{	
					//String type = busObjectWithSelect.getSelectData("type"); // [NDM] OP6
					String id	= busObjectWithSelect.getSelectData("id");

					MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

					if(!isVersion) // [NDM] OP6
						dateCreated  	 = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + "originated.generic");
				}

				returnList.add(dateCreated);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return returnList;
	}

	public Object getLockunlockValue(Context context, String[] args) throws Exception
	{
		Vector columnCellContentList = new Vector();

		HashMap paramMap = (HashMap)JPO.unpackArgs(args);

		MapList relBusObjPageList	= (MapList)paramMap.get("objectList");
		cache						= new IEFGlobalCache();
		serverResourceBundle		= new MCADServerResourceBundle(localeLanguage);
		util						= new IEFIntegAccessUtil(context, serverResourceBundle, cache);

		HashMap paramList 			= (HashMap)paramMap.get("paramList");			
		if(paramList != null)
		{
			localeLanguage			= (String)paramList.get("LocaleLanguage");
			integrationNameGCOTable	= (HashMap)paramList.get("GCOTable");
		}

		if(integrationNameGCOTable == null)
			integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");

		if(localeLanguage == null)
			localeLanguage = (String)paramMap.get("LocaleLanguage");

		String[] objIds	= new String[relBusObjPageList.size()];
		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");			
		}

		String REL_VERSION_OF			= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		String REL_LATEST_VERSION		= MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion");
		
		String SELECT_ON_VERSIONOF_REL 	= "from[" + REL_VERSION_OF + "]";
		String SELECT_ON_MAJOR			= "from[" + REL_VERSION_OF + "].to.";
		String SELECT_ON_LATEST			= "to[" + REL_LATEST_VERSION + "].from.";
		String ATTR_SOURCE				= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";

		StringList busSelectionList = new StringList();

		busSelectionList.addElement("id");
		busSelectionList.addElement("type");
		busSelectionList.addElement(ATTR_SOURCE); //To get Integrations name.
		busSelectionList.addElement("locked"); //To get Integrations name.
		busSelectionList.addElement("locker"); //To get Integrations name.
		busSelectionList.addElement(SELECT_ON_VERSIONOF_REL);
		busSelectionList.addElement(SELECT_ON_MAJOR + "locked"); //from Minor.
		busSelectionList.addElement(SELECT_ON_MAJOR + "locker"); //from Minor.
		busSelectionList.addElement(SELECT_ON_LATEST + "locked"); //from bulk loaded Minor.
		busSelectionList.addElement(SELECT_ON_LATEST + "locker"); //from bulk loaded Minor.
		busSelectionList.addElement(SELECT_ISVERSIONOBJ);

		Vector assignedIntegrations = util.getAssignedIntegrations(context);

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
			String sIsVersion= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
			boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();

			String integrationName	= null;

			String integrationSource	= busObjectWithSelect.getSelectData(ATTR_SOURCE);
			String isLocked				= "";
			String showLocker			= "";

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
					cache = new IEFGlobalCache();
				}

				//String busType				= busObjectWithSelect.getSelectData("type"); // [NDM] OP6
				String id					= busObjectWithSelect.getSelectData("id");

				if(!isVersion) // [NDM] OP6
				{
					isLocked		= busObjectWithSelect.getSelectData("locked");
					showLocker		= busObjectWithSelect.getSelectData("locker");
				}
				else
				{
					String isVersionOfExist	= busObjectWithSelect.getSelectData(SELECT_ON_VERSIONOF_REL); 
					if(MCADUtil.getBoolean(isVersionOfExist))
					{
						isLocked		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "locked");
						showLocker		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "locker");
					}
					else
					{
						isLocked		= busObjectWithSelect.getSelectData(SELECT_ON_LATEST + "locked");
						showLocker		= busObjectWithSelect.getSelectData(SELECT_ON_LATEST + "locker");
					}
				}
			}

			columnCellContentList.add(isLocked);
		}

		return columnCellContentList;
	}

	public Object getPolicy(Context context, String[] args) throws Exception
	{
		Vector columnCellContentList = new Vector();

		HashMap paramMap = (HashMap)JPO.unpackArgs(args);

		MapList relBusObjPageList	= (MapList)paramMap.get("objectList");
		cache						= new IEFGlobalCache();
		serverResourceBundle		= new MCADServerResourceBundle(localeLanguage);
		util						= new IEFIntegAccessUtil(context, serverResourceBundle, cache);

		HashMap paramList 			= (HashMap)paramMap.get("paramList");			
		if(paramList != null)
		{
			localeLanguage			= (String)paramList.get("LocaleLanguage");
			integrationNameGCOTable	= (HashMap)paramList.get("GCOTable");
		}

		if(integrationNameGCOTable == null)
			integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");

		if(localeLanguage == null)
			localeLanguage = (String)paramMap.get("LocaleLanguage");

		String[] objIds	= new String[relBusObjPageList.size()];
		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");			
		}

		String REL_VERSION_OF			= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		String SELECT_ON_MAJOR			= "from[" + REL_VERSION_OF + "].to.";
		String ATTR_SOURCE				= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";

		StringList busSelectionList = new StringList();

		busSelectionList.addElement("id");
		busSelectionList.addElement("type");
		busSelectionList.addElement(ATTR_SOURCE); //To get Integrations name.
		busSelectionList.addElement("policy");		
		busSelectionList.addElement(SELECT_ON_MAJOR + "policy"); //from Minor.		
		busSelectionList.addElement(SELECT_ISVERSIONOBJ);

		Vector assignedIntegrations = util.getAssignedIntegrations(context);

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);

			String integrationName	= null;

			String integrationSource	= busObjectWithSelect.getSelectData(ATTR_SOURCE);
			String policy				= "";
			String sIsVersion= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
			boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
			if(integrationSource != null)
			{
				StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

				if(integrationSourceTokens.hasMoreTokens())
					integrationName  = integrationSourceTokens.nextToken();
			}

			if(integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
			{					
				//MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName); // [NDM] OP6

				if(null == cache)
				{
					cache = new IEFGlobalCache();
				}

				// [NDM] OP6
				//String busType				= busObjectWithSelect.getSelectData("type");
				String id					= busObjectWithSelect.getSelectData("id");

				if(!isVersion) // [NDM] OP6
				{
					policy		= busObjectWithSelect.getSelectData("policy");					
				}
				else
				{
					policy		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "policy");					
				}
			}

			columnCellContentList.add(policy);
		}

		return columnCellContentList;
	}
	public Object getFolderChildCount(Context context, String[] args)
	{
		Vector returnVector = new Vector();
		try
		{			
			HashMap paramMap 			= (HashMap)JPO.unpackArgs(args);

			MapList relBusObjPageList	= (MapList)paramMap.get("objectList");
			cache						= new IEFGlobalCache();
			serverResourceBundle		= new MCADServerResourceBundle(localeLanguage);
			util						= new IEFIntegAccessUtil(context, serverResourceBundle, cache);
			HashMap paramList 			= (HashMap)paramMap.get("paramList");

			if(paramList != null)
			{
				localeLanguage			= (String)paramList.get("LocaleLanguage");
				integrationNameGCOTable	= (HashMap)paramList.get("GCOTable");
			}

			if(integrationNameGCOTable == null)
				integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");

			if(localeLanguage == null)
				localeLanguage = (String)paramMap.get("LocaleLanguage");

			String[] objIds	= new String[relBusObjPageList.size()];
			for(int i =0; i<relBusObjPageList.size(); i++)
			{
				Map objDetails	= (Map)relBusObjPageList.get(i);
				objIds[i]		= (String)objDetails.get("id");			
			}

			String ATTR_COUNT  		  = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Count") + "]";
			StringList busSelectList = new StringList();
			busSelectList.addElement(ATTR_COUNT);

			BusinessObjectWithSelectList businessObjectWithSelectList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectList);

			for ( int index = 0; index < businessObjectWithSelectList.size(); index++ )
			{
				String totalCount                = "0";

				BusinessObjectWithSelect busObjectWithSelect	= ( BusinessObjectWithSelect) businessObjectWithSelectList.elementAt(index);
				totalCount = busObjectWithSelect.getSelectData(ATTR_COUNT);

				returnVector.addElement(totalCount);
			}


		}
		catch(Exception exp)
		{
			exp.printStackTrace();
		}
		return returnVector;

	}
}
