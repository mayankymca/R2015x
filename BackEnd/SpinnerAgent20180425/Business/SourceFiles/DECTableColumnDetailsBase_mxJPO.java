/*
 **  DECTableColumnDetailsBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  To get the table column details for immersive UI
 */
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Access;
import matrix.db.AccessList;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADFolderUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADObjectsIdentificationUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.AccessUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.framework.ui.UINavigatorUtil;

public class DECTableColumnDetailsBase_mxJPO
{
	protected HashMap integrationNameGCOTable				= null;
	protected MCADServerResourceBundle serverResourceBundle	= null;
	protected IEFIntegAccessUtil util						= null;
	protected MCADServerGeneralUtil serverGeneralUtil       = null;
	protected MCADObjectsIdentificationUtil objIdentificationUtil = null;
	protected String localeLanguage							= null;
	protected IEFGlobalCache	cache						= null;	
	protected HashMap paramMap								= null;

	protected MCADFolderUtil mcadFolderUtil 				= null;
	protected MCADMxUtil mxUtil								= null;

	protected String integrationName						= "";
	protected String REL_VERSION_OF							= "";
	protected String REL_LATEST_VERSION						= "";
	protected String REL_VAULTED_OBJECTS					= "";
	protected String REL_DATA_VAULTS						= "";
	protected String SELECT_ON_VERSIONOF_REL				= "";			
	protected String SELECT_ON_MAJOR						= "";
	protected String SELECT_ON_LATEST						= "";
	protected String SELECT_ON_ACTIVE_MINOR					= "";
	protected String SELECT_ON_VAULTED_OBJECTS				= "";
	protected String SELECT_ON_DATA_VAULTS					= "";
	protected String ATTR_SOURCE							= "";
	protected String ATTR_CADTYPE							= "";
	protected String REL_VAULTED_DOCUMENTS 					= "";
	protected String TYPE_WORKSPACE_VAULT					= "";
	protected String GET_VAULT_IDS							= "";
	protected String GET_VAULT_NAMES 						= "";
	protected String SELECT_LOCK_INFO_ATT					= "";
	protected String IS_VERSION_OBJ							= "";
	protected String SELECT_ISVERSIONOBJ			= "";
	protected HashMap latestIdInfo							= null;
	protected String SELECT_LATEST_VERSION = "";
	private String REL_ACTIVE_VERSION						= "";

	public DECTableColumnDetailsBase_mxJPO(Context context, String[] args) throws Exception
	{
		cache						= new IEFGlobalCache();
		REL_VERSION_OF  			= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		REL_LATEST_VERSION			= MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion");
		
		SELECT_ON_VERSIONOF_REL 	= "from[" + REL_VERSION_OF + "]";
		SELECT_ON_MAJOR 			= "from[" + REL_VERSION_OF + "].to.";
		SELECT_ON_LATEST			= "to[" + REL_LATEST_VERSION + "].from.";
		ATTR_SOURCE					= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";
		SELECT_LOCK_INFO_ATT 		= new StringBuffer("attribute[").append(MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-LockInformation")).append("]").toString();  

		REL_VAULTED_DOCUMENTS 		= MCADMxUtil.getActualNameForAEFData(context, "relationship_VaultedDocuments");
		TYPE_WORKSPACE_VAULT 		= MCADMxUtil.getActualNameForAEFData(context, "type_ProjectVault");
		 SELECT_LATEST_VERSION	= "to[" + REL_LATEST_VERSION + "].from.";
		IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context,"attribute_IsVersionObject");
		SELECT_ISVERSIONOBJ = "attribute["+IS_VERSION_OBJ+"]";	
		REL_ACTIVE_VERSION		= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
		SELECT_ON_ACTIVE_MINOR	= "from[" + REL_ACTIVE_VERSION + "].to.";
		GET_VAULT_IDS 				= "to[" + REL_VAULTED_DOCUMENTS + "].from[" + TYPE_WORKSPACE_VAULT + "].id";
	}


	private void init(Context context,String[] args) throws Exception
	{
		paramMap	 				= (HashMap)JPO.unpackArgs(args);	

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

		serverResourceBundle		= new MCADServerResourceBundle(localeLanguage);		
		util						= new IEFIntegAccessUtil(context, serverResourceBundle, cache);					
		mcadFolderUtil 				= new MCADFolderUtil(context,serverResourceBundle,cache);
		mxUtil						= new MCADMxUtil(context,serverResourceBundle,cache);
		objIdentificationUtil		= new MCADObjectsIdentificationUtil(context,serverResourceBundle, cache);

	}

	public Object getStatusValue(Context context, String[] args) throws Exception
	{
		init(context, args);

		Vector columnCellContentList = new Vector();
		List relBusObjPageList		 = (List)paramMap.get("objectList");		

		String[] objIds	= new String[relBusObjPageList.size()];
		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");
		}

		for(int i = 0; i < objIds.length; i++)
		{
			columnCellContentList.add(MCADServerSettings.DEC_DEFAULT_COLUMN + "status");
		}

		return columnCellContentList;
	}
	public Object isLockedforCheckout(Context context, String[] args) throws Exception
	{
		String unlockedToolTip = "iconActionUnlock.gif|FALSE";
		String SELECT_ON_MAJOR_LOCKED	= "from[VersionOf].to.locked";
		String lockedToolTip = "iconStatusLocked.gif|TRUE";

		init(context, args);

		Vector columnCellContentList = new Vector();
		HashMap paramList 			= (HashMap)paramMap.get("paramList");	
		MapList mlData = (MapList) paramList.get("mlObjInfo");

		Vector assignedIntegrations = util.getAssignedIntegrations(context);

		for(int i = 0; i < mlData.size(); i++)
		{
			HashMap hmEach = (HashMap) mlData.get(i);

			String integrationName	= null;

			String integrationSource	= (String) hmEach.get(ATTR_SOURCE);
			
			String sIsVersion= (String) hmEach.get(SELECT_ISVERSIONOBJ);
			boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
			
			String isLocked				= "";

			if(integrationSource != null)
			{
				StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

				if(integrationSourceTokens.hasMoreTokens())
					integrationName  = integrationSourceTokens.nextToken();
			}

			if(integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
			{					
				    String id					= (String) hmEach.get("id");
				    if(isVersion){
					isLocked		= (String) hmEach.get(SELECT_ON_MAJOR_LOCKED);
				    } else {
				    	isLocked		= (String) hmEach.get("locked");
				    }
			}

			String columnString = unlockedToolTip;

			if(null != isLocked && isLocked.equalsIgnoreCase("TRUE"))
				columnString = lockedToolTip;

			columnCellContentList.add(columnString);
		}
		return columnCellContentList;
	}


	//new
	public Object isLocked(Context context, String[] args) throws Exception
	{
		String unlockedToolTip = "iconActionUnlock.gif|FALSE";

		String lockedToolTip = "iconStatusLocked.gif|TRUE";

		init(context, args);

		Vector columnCellContentList = new Vector();

		String[] objIds  			= getLatestIDs(context);

		StringList busSelectionList = new StringList(6);
		busSelectionList.addElement("id");  // [NDM] OP6
		busSelectionList.addElement("type");
		busSelectionList.addElement(ATTR_SOURCE); //To get Integrations name.
		busSelectionList.addElement("locked"); 
		busSelectionList.addElement(SELECT_ON_MAJOR + "locked"); //from Minor.
		busSelectionList.addElement(SELECT_ISVERSIONOBJ);

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

		Vector assignedIntegrations = util.getAssignedIntegrations(context);

		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);

			String integrationName	= null;

			String integrationSource	= busObjectWithSelect.getSelectData(ATTR_SOURCE);
			String isLocked				= "";

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
				//[NDM] Start OP6
				//MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

				//String busType				= busObjectWithSelect.getSelectData("type");
				String id					= busObjectWithSelect.getSelectData("id");

				if(!isVersion)//gco.isMajorType(busType)) //[NDM] End OP6
				{
					isLocked		= busObjectWithSelect.getSelectData("locked");
				}
				else
				{
					isLocked		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "locked");
				}
			}

			String columnString = unlockedToolTip;

			if(null != isLocked && isLocked.equalsIgnoreCase("TRUE"))
				columnString = lockedToolTip;

			columnCellContentList.add(columnString);
		}

		return columnCellContentList;
	}

	//new
	public Object getLockerInfo(Context context, String[] args) throws Exception
	{
		init(context, args);

		Vector columnCellContentList = new Vector();

		HashMap lockerBusidLidMap = new HashMap();

		try
		{
			String[] objIds			 	= getLatestIDs(context);

			StringList busSelectionList = new StringList(6);
			busSelectionList.addElement("id");  // [NDM] OP6
			busSelectionList.addElement("type");
			busSelectionList.addElement(ATTR_SOURCE); //To get Integrations name.
			busSelectionList.addElement("locker"); 
			busSelectionList.addElement(SELECT_ON_MAJOR + "locker"); //from Minor.
			busSelectionList.addElement(SELECT_ISVERSIONOBJ);

			Vector assignedIntegrations = util.getAssignedIntegrations(context);

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{
				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);

				String integrationName	= null;

				String integrationSource	= busObjectWithSelect.getSelectData(ATTR_SOURCE);
				String locker			= "";
				String lockerLID		= "";

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
					//[NDM] Start OP6
					//MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

					//String busType = busObjectWithSelect.getSelectData("type");
					String busId   = busObjectWithSelect.getSelectData("id");

					if(!isVersion)//gco.isMajorType(busType)) //[NDM] End OP6
					{
						locker		= busObjectWithSelect.getSelectData("locker");
					}
					else
					{
						locker		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "locker");
					}

						if(null != locker && !"".equals(locker))
						{
						String lockerId	= PersonUtil.getPersonObjectID(context, locker);
						
						StringList busIdList	= new StringList();
						
							busIdList.add(lockerId);
						
						if(!lockerBusidLidMap.containsKey(lockerId))
						{
							lockerLID	= (String)objIdentificationUtil.getLogicalIdListFromBusIdList(context, busIdList).get(lockerId);
							lockerBusidLidMap.put(lockerId, lockerLID);
						}
						else 
							lockerLID = (String)lockerBusidLidMap.get(lockerId);
					}
				}

				columnCellContentList.add(locker + "|" + lockerLID);
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}

		return columnCellContentList;
	}

	public Object getLockerInfoforCheckout(Context context, String[] args) throws Exception
	{
		init(context, args);
		Vector columnCellContentList = new Vector();
		HashMap lockerBusidLidMap = new HashMap();
		HashMap paramList 			= (HashMap)paramMap.get("paramList");	
		MapList mlData = (MapList) paramList.get("mlObjInfo");
		
		try
		{
			String SELECT_ON_MAJOR_LOCKER	= "from[VersionOf].to.locker";

			Vector assignedIntegrations = util.getAssignedIntegrations(context);
			
			for(int i = 0; i < mlData.size(); i++)
			{

				HashMap hmTemp = (HashMap)mlData.get(i);
				String integrationName	= null;

				String integrationSource	= (String)hmTemp.get(ATTR_SOURCE);
				String sIsVersion= (String)hmTemp.get(SELECT_ISVERSIONOBJ);
				boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
				
				String locker			= "";
				String lockerLID		= "";

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}

				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
				{					
					if(isVersion){		
					    //String busId   = (String)hmTemp.get("id");
						locker		= (String)hmTemp.get(SELECT_ON_MAJOR_LOCKER);
					} else {
						locker		= (String)hmTemp.get("locker");
					}
					
					if(null != locker && !"".equals(locker))
					{
						String lockerId	= PersonUtil.getPersonObjectID(context, locker);						
						StringList busIdList	= new StringList();						
						busIdList.add(lockerId);
						
						if(!lockerBusidLidMap.containsKey(lockerId))
						{
							lockerLID	= (String)objIdentificationUtil.getLogicalIdListFromBusIdList(context, busIdList).get(lockerId);
							lockerBusidLidMap.put(lockerId, lockerLID);
						}
						else 
							lockerLID = (String)lockerBusidLidMap.get(lockerId);
					}
				}
				if(locker==null){
					locker = "";
				}
				columnCellContentList.add(locker + "|" + lockerLID);
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return columnCellContentList;
	}
	
	
	public Object getLockunlockValue(Context context, String[] args) throws Exception
	{
		init(context, args);

		List relBusObjPageList	= (List)paramMap.get("objectList");

		String[] objIds	= new String[relBusObjPageList.size()];
		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");			
		}

		return getLockUnlockData(context, objIds);
	}

	public Object getLatestObjLockunlockValue(Context context, String[] args) throws Exception
	{
		init(context, args);

		String[] busIds	= getLatestIDs(context);

		return getLockUnlockData(context, busIds);
	}

	public Object getLatestLocker(Context context, String[] args) throws Exception
	{
		init(context, args);

		String[] busIds	= getLatestIDs(context);

		return getLockerData(context, busIds);
	}

	public Object getLocker(Context context, String[] args) throws Exception
	{		
		init(context, args);

		List relBusObjPageList		 = (List)paramMap.get("objectList");

		String[] objIds	= new String[relBusObjPageList.size()];
		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");			
		}

		return getLockerData(context, objIds);
	}

	public Object getLockerData(Context context, String[] objIds) throws Exception
	{
		Vector columnCellContentList = new Vector();

		StringList busSelectionList = new StringList(8);

		busSelectionList.addElement("id");
		busSelectionList.addElement("type");
		busSelectionList.addElement(ATTR_SOURCE); //To get Integrations name.
		busSelectionList.addElement(SELECT_ON_VERSIONOF_REL);
		busSelectionList.addElement("locker"); //To get Integrations name.
		busSelectionList.addElement(SELECT_ON_MAJOR + "locker"); //from Minor.
		busSelectionList.addElement(SELECT_ON_LATEST + "locker"); //from bulk loaded  Minor.
		busSelectionList.addElement(SELECT_ISVERSIONOBJ);
		

		Vector assignedIntegrations = util.getAssignedIntegrations(context);

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);

			String integrationName	= null;

			String integrationSource	= busObjectWithSelect.getSelectData(ATTR_SOURCE);
			String showLocker			= "";

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
				// [NDM] Start OP6
				//MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

				//String busType = busObjectWithSelect.getSelectData("type");
				String busId   = busObjectWithSelect.getSelectData("id");

				if(!isVersion)//gco.isMajorType(busType)) // [NDM] End OP6
				{
					showLocker		= busObjectWithSelect.getSelectData("locker");
				}
				else
				{
					String isVersionOfExist	= busObjectWithSelect.getSelectData(SELECT_ON_VERSIONOF_REL); 
					
					if(MCADUtil.getBoolean(isVersionOfExist))
						showLocker		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "locker");
					else
						showLocker		= busObjectWithSelect.getSelectData(SELECT_ON_LATEST + "locker");
				}
			}
			showLocker	= MCADMxUtil.getNLSName(context, "Person",showLocker, "", "" , localeLanguage); // nls change
			columnCellContentList.add(showLocker);
		}

		return columnCellContentList;
	}

	public Object getEnoviaFolderPath(Context context, String[] args) throws Exception
	{		
		init(context, args);

		Vector columnCellContentList = new Vector();

		try
		{
			List relBusObjPageList	 = (List)paramMap.get("objectList");

			mcadFolderUtil = new MCADFolderUtil(context,serverResourceBundle,new IEFGlobalCache());

			String[] objIds	= new String[relBusObjPageList.size()];
			for(int i =0; i<relBusObjPageList.size(); i++)
			{

				Map objDetails	= (Map)relBusObjPageList.get(i);
				objIds[i]		= (String)objDetails.get("id");
			}

			HashMap busIdFolderPathMap  = mcadFolderUtil.getAssignedFolder(context,objIds);

			for(int i =0; i<relBusObjPageList.size(); i++)
			{
				Map objDetails	= (Map)relBusObjPageList.get(i);
				String busId	= (String)objDetails.get("id");

				String connectedFolder = (String) busIdFolderPathMap.get(busId);

				if(connectedFolder != null && !connectedFolder.equals(""))
					columnCellContentList.add("iconSmallWorkspaceFolder.gif" + "|" + connectedFolder);
				else
					columnCellContentList.add("");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return columnCellContentList;
	}

	//new
	public Object getLatestConnectedFolderPaths(Context context, String[] args) throws Exception
	{		
		init(context, args);

		String []   busIds	= getLatestIDs(context);

		Vector columnCellContentList 	= new Vector();

		try
		{
			StringList selectStmts = new StringList(2);
			selectStmts.add("id");
			selectStmts.add("type");
			selectStmts.add(ATTR_SOURCE); //To get Integrations name.
			selectStmts.add(GET_VAULT_IDS);
			selectStmts.add(SELECT_ON_MAJOR + GET_VAULT_IDS);
			selectStmts.add(SELECT_ON_MAJOR + GET_VAULT_NAMES);

			BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, busIds, selectStmts);

			for (int i = 0; i < busWithSelectList.size(); i++) 
			{
				HashMap folderIDLogicalIDMap	= new HashMap();
				HashMap folderIDfolderPathMap	= new HashMap();

				BusinessObjectWithSelect busWithSelect  = (BusinessObjectWithSelect) busWithSelectList.elementAt(i);
				mcadFolderUtil.getPathForBusObject(context, null, folderIDfolderPathMap, folderIDLogicalIDMap, busWithSelect, ",", "|", false);

				if(!folderIDfolderPathMap.isEmpty())
				{
					String folderPaths = MCADUtil.getDelimitedStringFromCollection(folderIDfolderPathMap.values(), "|");
					columnCellContentList.add(folderPaths);
				}
				else
					columnCellContentList.add("");	
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return columnCellContentList;
	}

	//new
	public Object getConnectedFolderInfo(Context context, String[] args) throws Exception
	{		
		init(context, args);

		List relBusObjPageList	= (List)paramMap.get("objectList");

		String[] busIds	= new String[relBusObjPageList.size()];
		for(int i =0; i<relBusObjPageList.size(); i++)
		{

			Map objDetails	= (Map)relBusObjPageList.get(i);
			busIds[i]		= (String)objDetails.get("id");
		}

		return getConnectedFolderData(context, busIds);	
	}

	public Object getLatestObjConnectedFolderInfo(Context context, String[] args) throws Exception 
	{
		init(context, args);

		String[]   busIds	= getLatestIDs(context);

		return  getConnectedFolderData(context, busIds);
	}

	public Object getLatestObjConnectedFolderInfoForCheckout(Context context, String[] args) throws Exception 
	{
		init(context,args);
			HashMap paramList 			= (HashMap)paramMap.get("paramList");	
			MapList mlData = (MapList) paramList.get("mlObjInfo");
		return  getConnectedFolderDataForCheckout(context, mlData);
			}


	//new
	public Object getStateInfo(Context context, String[] args) throws Exception
	{		
		init(context, args);

		List relBusObjPageList	= (List)paramMap.get("objectList");

		String[] busIds	= new String[relBusObjPageList.size()];
		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			busIds[i]		= (String)objDetails.get("id");
		}

		return  getStateInfoData(context, busIds);
	}

	public Object getLatestObjStateInfo(Context context, String[] args) throws Exception
	{
		init(context, args);

		String[]   busIds	= getLatestIDs(context);

		return  getStateInfoData(context, busIds);
	}

	public Object getLatestObjectComputerName(Context context, String[] args) throws Exception
	{
		init(context, args);

		String[]   busIds	= getLatestIDs(context);

		return  getComputerNameForIds(context, busIds);
	}

	public Object getComputerName(Context context, String[] args) throws Exception
	{		
		init(context, args);

		List relBusObjPageList	= (List)paramMap.get("objectList");

		String[] busIds	= new String[relBusObjPageList.size()];
		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			busIds[i]		= (String)objDetails.get("id");
		}

		return  getComputerNameForIds(context, busIds);
	}

	//new
	private Object getComputerNameForIds(Context context, String[] busids) throws Exception
	{		
		Vector columnCellContentList = new Vector();

		try
		{
			String[] busIds  			= getLatestIDs(context);

			String selectLockInfoAtt		= SELECT_LOCK_INFO_ATT.toString();

			StringList selectStmts = new StringList(6);
			selectStmts.add("id");
			selectStmts.add("type");
			selectStmts.add(ATTR_SOURCE);
			selectStmts.add(selectLockInfoAtt);
			selectStmts.add(SELECT_ON_MAJOR + selectLockInfoAtt);
			selectStmts.add(SELECT_ISVERSIONOBJ);

			Vector assignedIntegrations = util.getAssignedIntegrations(context);

			BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, busIds, selectStmts);

			for (int i = 0; i < busWithSelectList.size(); i++) 
			{
				BusinessObjectWithSelect busWithSelect  = (BusinessObjectWithSelect) busWithSelectList.elementAt(i);
				String sIsVersion= busWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
				boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
				String lockInformation	= "";
				String integrationName	 = null;
				String integrationSource = busWithSelect.getSelectData(ATTR_SOURCE);

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}

				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
				{
					//[NDM] Start OP6
					//MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

					//String busType 			= busWithSelect.getSelectData("type");
					String busId 			= busWithSelect.getSelectData("id");

					if(!isVersion)//gco.isMajorType(busType)) //[NDM] End OP6
					{
						lockInformation					= busWithSelect.getSelectData(selectLockInfoAtt);
					}
					else
					{
						lockInformation					= busWithSelect.getSelectData(SELECT_ON_MAJOR + selectLockInfoAtt);
					}
				}

				if(lockInformation != null && !"".equals(lockInformation))
					columnCellContentList.add(lockInformation);
				else
					columnCellContentList.add("");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return columnCellContentList;
	}


	public Object getLatestObjectComputerNameforCheckout(Context context,String[] args) throws Exception
	{		
		Vector columnCellContentList = new Vector();
		init(context,args);
		try
		{
			HashMap paramList 			= (HashMap)paramMap.get("paramList");	
			MapList mlData = (MapList) paramList.get("mlObjInfo");

			Vector assignedIntegrations = util.getAssignedIntegrations(context);

			for (int i = 0; i < mlData.size(); i++) 
			{
				HashMap hmTem = (HashMap) mlData.get(i);
				String lockInformation	= "";
				String integrationName	 = null;
				String integrationSource = (String)hmTem.get(ATTR_SOURCE);

				String sIsVersion= (String)hmTem.get(SELECT_ISVERSIONOBJ);
				boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
				
				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}

				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
				{
					//String busId 			= (String)hmTem.get("id");
					if(isVersion){
					lockInformation					= (String)hmTem.get(SELECT_ON_MAJOR+SELECT_LOCK_INFO_ATT);
					}else {
						lockInformation					= (String)hmTem.get(SELECT_LOCK_INFO_ATT);
					}
				}

				if(lockInformation != null && !"".equals(lockInformation))
					columnCellContentList.add(lockInformation);
				else
					columnCellContentList.add("");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return columnCellContentList;
	}
	
	
	public Object getPDSWorkspaceRole(Context context, String[] args) throws Exception
	{		
		init(context, args);

		Vector columnCellContentList = new Vector();
                String roleProjectLead = MCADMxUtil.getActualNameForAEFData(context, "role_ProjectLead");
		try
		{
			List relBusObjPageList		= (List)paramMap.get("objectList");
			HashMap paramList 			= (HashMap)paramMap.get("paramList");			

			String workspaceId			= (String)paramList.get("workspaceid");

			if(workspaceId == null)
				workspaceId			= (String)paramMap.get("workspaceid");

			DomainObject workspaceObject     = DomainObject.newInstance(context);
			workspaceObject.setId(workspaceId);

			AccessList workspaceAccessList = workspaceObject.getAccessForGrantor(context, AccessUtil.WORKSPACE_ACCESS_GRANTOR);

			HashMap granteeWorkspaceAccessMap = getGranteeAccessMap(workspaceAccessList);

			for(int i =0; i < relBusObjPageList.size(); i++)
			{
				String sAccessString = AccessUtil.NONE;

				Map objDetails	= (Map)relBusObjPageList.get(i);
				String userName	= (String)objDetails.get("name");
				String isOwner  = (String)objDetails.get("isowner");
				String projectLead  = (String)objDetails.get("projectlead");

				if(isOwner != null && isOwner.equals(MCADAppletServletProtocol.TRUE))
				{
					sAccessString = "Owner";
				}
				else if(projectLead != null && projectLead.equals(roleProjectLead))
				{
					sAccessString = "CoOwner";
				}
				else if(granteeWorkspaceAccessMap.containsKey(userName))
				{
					Access access = (Access) granteeWorkspaceAccessMap.get(userName);
					String accessString = AccessUtil.checkAccess(access);

					if(accessString.equals(AccessUtil.ADD_REMOVE))
						sAccessString = "Contributor";
					else if(accessString.equals(AccessUtil.READ_WRITE))
						sAccessString = "Reviewer";
				}

				columnCellContentList.add(sAccessString);
			}


		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return columnCellContentList;
	}

	private HashMap getGranteeAccessMap(AccessList workspaceAccessList)
	{
		HashMap granteeWorkspaceAccessMap = new HashMap();

		Iterator iterator = workspaceAccessList.iterator();

		while(iterator.hasNext())
		{
			matrix.db.Access access = (Access)iterator.next();

			String grantee = access.getUser();

			granteeWorkspaceAccessMap.put(grantee, access);
		}

		return granteeWorkspaceAccessMap;
	}

	public Object getVault(Context context, String[] args) throws Exception
	{
		Vector columnCellContentList = new Vector();

		HashMap parameterMap = (HashMap)JPO.unpackArgs(args);

		List relBusObjPageList		 = (List)parameterMap.get("objectList");

		String[] objIds	= new String[relBusObjPageList.size()];
		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");			
		}

		StringList busSelectionList = new StringList();

		busSelectionList.addElement("id");
		busSelectionList.addElement("vault");		

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);

			String vault				= busObjectWithSelect.getSelectData("vault");

			if(null == vault)
			{
				vault = " ";
			}

			columnCellContentList.add(vault);
		}

		return columnCellContentList;
	}

	public Object getTemplate(Context context, String[] args) throws Exception
	{
		Vector columnCellContentList = new Vector();

		HashMap parameterMap = (HashMap)JPO.unpackArgs(args);

		List relBusObjPageList		 = (List)parameterMap.get("objectList");

		String[] objIds	= new String[relBusObjPageList.size()];
		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");			
		}

		StringList busSelectionList = new StringList();

		String REL_WORKSPACE_TEMPLATE			= MCADMxUtil.getActualNameForAEFData(context, "relationship_WorkspaceTemplate");

		String WORKSPACE_TEMPLATE_NAME_SELECT = "to[" + REL_WORKSPACE_TEMPLATE + "].from.name";

		busSelectionList.addElement("id");
		busSelectionList.addElement(WORKSPACE_TEMPLATE_NAME_SELECT);		

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);

			String workspaceTemplateName	= busObjectWithSelect.getSelectData(WORKSPACE_TEMPLATE_NAME_SELECT);

			if(null == workspaceTemplateName)
			{
				workspaceTemplateName = "";
			}

			columnCellContentList.add(workspaceTemplateName);
		}

		return columnCellContentList;
	}

	private Object getConnectedFolderData(Context context, String[] busIds) throws Exception 
	{
		HashMap folderIDLogicalIDMap	= new HashMap();
		HashMap folderIDfolderPathMap	= new HashMap();
		Vector columnCellContentList 	= new Vector();

		try
		{
			StringList selectStmts = new StringList(2);
			selectStmts.add(GET_VAULT_IDS);
			selectStmts.add(SELECT_ON_MAJOR + GET_VAULT_IDS);

			BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, busIds, selectStmts);

			for (int i = 0; i < busWithSelectList.size(); i++) 
			{
				BusinessObjectWithSelect busWithSelect  = (BusinessObjectWithSelect) busWithSelectList.elementAt(i);
				String folderPathFolderId = mcadFolderUtil.getPathForBusObject(context, null, folderIDfolderPathMap, folderIDLogicalIDMap, busWithSelect, ",", "|", false);

				if(folderPathFolderId != null && !folderPathFolderId.equals(""))
					columnCellContentList.add(folderPathFolderId);
				else
					columnCellContentList.add("");	
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return columnCellContentList;
	}

	private Object getConnectedFolderDataForCheckout(Context context, MapList mlObjInfo) throws Exception 
	{
		HashMap folderIDLogicalIDMap	= new HashMap();
		HashMap folderIDfolderPathMap	= new HashMap();
		Vector columnCellContentList 	= new Vector();

		try
		{


			for (int i = 0; i < mlObjInfo.size(); i++) 
			{
				Map mpEachObj = (Map) mlObjInfo.get(i);
				//BusinessObjectWithSelect busWithSelect  = (BusinessObjectWithSelect) busWithSelectList.elementAt(i);
				String folderPathFolderId = mcadFolderUtil.getPathForBusObjectForCheckout(context, null, folderIDfolderPathMap, folderIDLogicalIDMap, mpEachObj, ",", "|", false);

				if(folderPathFolderId != null && !folderPathFolderId.equals(""))
					columnCellContentList.add(folderPathFolderId);
				else
					columnCellContentList.add("");	
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return columnCellContentList;
	}
	
	
	public Object getStateLabelForTable(Context context, String [] args) throws Exception
	{
		Vector columnCellContentList = new Vector();		

		try
		{	
			init(context, args);
			
			HashMap paramList 			= (HashMap)paramMap.get("paramList");	
			MapList mlData = (MapList) paramList.get("mlObjInfo");
			
			for(int i = 0; i < mlData.size(); i++)
			{			
				HashMap hmTemp = (HashMap) mlData.get(i);
				String policyName			="";
				String stateLabel			= "";
				String integrationName		= null;

				String integrationSource	= (String) hmTemp.get(ATTR_SOURCE);
				// [NDM]
				//String busId				= (String) hmTemp.get("id");
				String busType				= (String) hmTemp.get("type");
				String sIsVersion= (String) hmTemp.get(SELECT_ISVERSIONOBJ);
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
						stateLabel = (String) hmTemp.get("current");
						policyName 	= (String) hmTemp.get("policy");
					}
					else
					{
					String isVersionOfExist = (String) hmTemp.get(SELECT_ON_VERSIONOF_REL);
					if(!MCADUtil.getBoolean(isVersionOfExist))
		        	{						
						stateLabel = (String) hmTemp.get(SELECT_LATEST_VERSION + "current");
						policyName 	= (String) hmTemp.get(SELECT_LATEST_VERSION + "policy");						
					}else
					{
						stateLabel = (String) hmTemp.get(SELECT_ON_MAJOR + "current");
						policyName 	= (String) hmTemp.get(SELECT_ON_MAJOR + "policy");
					}
				}

				}
				else
				{
					stateLabel = (String) hmTemp.get("current");
				}
				stateLabel = MCADMxUtil.getNLSName(context, "State", stateLabel, "Policy", policyName , localeLanguage);
				columnCellContentList.add(stateLabel);
			}
		}
		catch(Exception e) 
		{

		}
		return columnCellContentList;
	}	
	
	public Object getLatestTypeForFrameworkTable(Context context, String []args ) throws Exception
	{
		Vector columnCellContentList 	= new Vector();
		
		try
		{
			init(context, args);
			
			HashMap paramList 			= (HashMap)paramMap.get("paramList");	
			MapList mlData = (MapList) paramList.get("mlObjInfo");

			for(int i = 0; i < mlData.size(); i++)
			{			
				HashMap hmTemp = (HashMap) mlData.get(i);
				String designName			= "";
				String integrationName		= null;

				String busType				= (String)hmTemp.get("type");
				//String busId				= (String)hmTemp.get("id");  // [NDM] OP6
				String integrationSource	= (String)hmTemp.get(ATTR_SOURCE);
				String sIsVersion=  (String)hmTemp.get(SELECT_ISVERSIONOBJ);
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
					

						
						if(!isVersion || gco.isTemplateType(busType)) //gco.isMajorType(busType) // [NDM] OP6
						{
							designName	= busType;
						}
						else
						{						
					//		Extra Check is added to support bulk loading object where VersonOf Relationship not exist	
						String isVersionOfExist = (String)hmTemp.get(SELECT_ON_VERSIONOF_REL);

						if(!MCADUtil.getBoolean(isVersionOfExist))
							designName	= (String)hmTemp.get(SELECT_LATEST_VERSION + "type");			        	
						else
							designName	= (String)hmTemp.get(SELECT_ON_MAJOR + "type");					
				}
				}
				else   
				{
					designName	=  busType;
				}
				designName	= MCADMxUtil.getNLSName(context, "Type", designName, "", "", localeLanguage);
				columnCellContentList.add(designName);
			}
		} 
		catch(Exception e) 
		{

		}	
		return columnCellContentList;
	}
	
	public Object getLatestVersionAcrossRevision(Context context, String[] args ) throws Exception 
	{
		Vector columnLatestVersionList  = new Vector();
		init(context, args);
		HashMap paramList 			= (HashMap)paramMap.get("paramList");	
		MapList mlData = (MapList) paramList.get("mlObjInfo");

		for(int i = 0; i < mlData.size(); i++)
		{
			HashMap hmTemp = (HashMap) mlData.get(i);
			String busId			= (String) hmTemp.get("id");
			String integrationSource					 = (String) hmTemp.get(ATTR_SOURCE);

			String revision 		= ""; 
			String integrationName 	= null;
			String sIsVersion=(String) hmTemp.get(SELECT_ISVERSIONOBJ);
			boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
			if(integrationSource != null)
			{
				StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

				if(integrationSourceTokens.hasMoreTokens())
					integrationName  = integrationSourceTokens.nextToken();
			}

			if(integrationName != null && integrationNameGCOTable.containsKey(integrationName))
			{
				Object objTmp = null;
				if(!isVersion)  // [NDM] is Major Object
					objTmp = (Object)hmTemp.get(SELECT_ON_ACTIVE_MINOR + "revision");
				else
					objTmp = (Object)hmTemp.get("revision");
				
				if(objTmp instanceof StringList){
					revision = (String) ((StringList) objTmp).get(0);
				} else if (objTmp instanceof String)
				{
					revision = (String) objTmp;
				}

			}

			columnLatestVersionList.add(revision);
		}
		return columnLatestVersionList;
	}
	
	
	
	
	public Object getLatestRevisionForFrameworkTable(Context context, String [] args) throws Exception
	{
		Vector columnCellContentList = new Vector();

		try
		{
			init(context, args);
			HashMap paramList 			= (HashMap)paramMap.get("paramList");	
			MapList mlData = (MapList) paramList.get("mlObjInfo");

			for(int i = 0; i < mlData.size(); i++)
			{			
				HashMap hmTemp = (HashMap) mlData.get(i);
				
				String revision				= "";
				String integrationName		= null;

				String busType				= (String) hmTemp.get("type");
				String integrationSource	= (String) hmTemp.get(ATTR_SOURCE);
				String sIsVersion= (String) hmTemp.get(SELECT_ISVERSIONOBJ);
				boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
				//NDM line
				//String busId				= (String) hmTemp.get("id");

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
						revision = (String) hmTemp.get("revision");
					}
					else
					{
					revision = (String) hmTemp.get(SELECT_ON_MAJOR + "revision");
				}
					
				}
				else
				{
					revision = (String) hmTemp.get("revision");
				}

				columnCellContentList.add(revision);
			} 
		}
		catch(Exception e) 
		{

		}
		return columnCellContentList;
	}
	
	
	
	public Object getStateInfoDataforCheckout(Context context,String[] args)throws Exception
	{
		Vector columnCellContentList = new Vector();
		init(context, args);
		String SELECT_ON_MAJOR = "from[VersionOf].to.";
		
		try{
			HashMap paramList 			= (HashMap)paramMap.get("paramList");	
			MapList mlData = (MapList) paramList.get("mlObjInfo");
			Vector assignedIntegrations = util.getAssignedIntegrations(context);

			for (int i = 0; i < mlData.size(); i++) 
			{
				String currentState = "";
				String currentStateId = "";
				HashMap hmTemp = (HashMap) mlData.get(i);

				String integrationName	 = null;
				String integrationSource = (String)hmTemp.get(ATTR_SOURCE);
				String sIsVersion= (String)hmTemp.get(SELECT_ISVERSIONOBJ);
				boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}

				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
				{
						//String busId = (String)hmTemp.get("id");
					if(isVersion){
						currentState					= (String)hmTemp.get(SELECT_ON_MAJOR + "current");
						currentStateId					= (String)hmTemp.get(SELECT_ON_MAJOR + "policy.state[" + currentState + "].id");
					} else {
						currentState					= (String)hmTemp.get("current");
						currentStateId					= (String)hmTemp.get("policy.state[" + currentState + "].id");

					}
				}

				if(currentStateId != null && !"".equals(currentStateId))

					columnCellContentList.add(currentStateId);
				else
					columnCellContentList.add("");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return columnCellContentList;
	}
	private Object getStateInfoData(Context context, String[] objIds)throws Exception
	{
		Vector columnCellContentList = new Vector();

		try{
			StringList selectStmts = new StringList(8);
			selectStmts.add("id");
			selectStmts.add("type");
			selectStmts.add(ATTR_SOURCE);
			selectStmts.add("current");
			selectStmts.add("policy.state.id");
			selectStmts.add(SELECT_ON_MAJOR + "current");
			selectStmts.add(SELECT_ON_MAJOR + "policy.state.id");
			selectStmts.add(SELECT_ISVERSIONOBJ);

			Vector assignedIntegrations = util.getAssignedIntegrations(context);

			BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, objIds, selectStmts);

			for (int i = 0; i < busWithSelectList.size(); i++) 
			{
				String currentState = "";
				String currentStateId = "";

				BusinessObjectWithSelect busWithSelect  = (BusinessObjectWithSelect) busWithSelectList.elementAt(i);

				String integrationName	 = null;
				String integrationSource = busWithSelect.getSelectData(ATTR_SOURCE);
				String sIsVersion= busWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
				boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}

				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
				{
					// [NDM] Start OP6
					//MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

					//String busType = busWithSelect.getSelectData("type");
					String busId = busWithSelect.getSelectData("id");

					if(!isVersion)//gco.isMajorType(busType)) // [NDM] End OP6
					{
						currentState					= busWithSelect.getSelectData("current");
						currentStateId					= busWithSelect.getSelectData("policy.state[" + currentState + "].id");
					}
					else
					{
						currentState					= busWithSelect.getSelectData(SELECT_ON_MAJOR + "current");
						currentStateId					= busWithSelect.getSelectData(SELECT_ON_MAJOR + "policy.state[" + currentState + "].id");
					}
				}

				if(currentStateId != null && !"".equals(currentStateId))

					columnCellContentList.add(currentStateId);
				else
					columnCellContentList.add("");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return columnCellContentList;
	}

	public Object getLatestObjLockunlockValueforCheckout(Context context,String[] args)throws Exception
	{
		init(context,args);
				
		String lockedByStr = UINavigatorUtil.getI18nString("emxIEFDesignCenter.Common.LockedBy","emxIEFDesignCenterStringResource", localeLanguage);
		Vector columnCellContentList = new Vector();

		String SELECT_ON_MAJOR_LOCKER	= "from[VersionOf].to.locker";
		Vector assignedIntegrations = util.getAssignedIntegrations(context);

		String unlockedString = "iconActionUnlock.gif|" + lockedByStr + ": "; 
			
		HashMap paramList 			= (HashMap)paramMap.get("paramList");	
		MapList mlData = (MapList) paramList.get("mlObjInfo");
		
		for(int i = 0; i < mlData.size(); i++)
		{
			HashMap hmTemp = (HashMap) mlData.get(i);

			String integrationName	= null;

			String integrationSource	= (String)hmTemp.get(ATTR_SOURCE);
			
			String sIsVersion= (String)hmTemp.get(SELECT_ISVERSIONOBJ);
			boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();			
			
			String showLocker			= "";

			if(integrationSource != null)
			{
				StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

				if(integrationSourceTokens.hasMoreTokens())
					integrationName  = integrationSourceTokens.nextToken();
			}

			if(integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
			{					
				// [NDM] Start OP6
				//MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
				//String busId				= (String)hmTemp.get("id");
				if(isVersion){
				showLocker		= (String)hmTemp.get(SELECT_ON_MAJOR_LOCKER);
				} else {
					showLocker		= (String)hmTemp.get("locker");
				}
				
			}

			String columnString = unlockedString;

			if(null != showLocker && !showLocker.equals(""))
			{
				//Icon path and tool tip with '|' separation
				StringBuffer imageToolTipStr = new StringBuffer(60);

				imageToolTipStr.append("iconStatusLocked.gif").append("|");
				imageToolTipStr.append(lockedByStr).append(": ").append(showLocker);

				columnString = imageToolTipStr.toString();
			}

			columnCellContentList.add(columnString);
		}
		return columnCellContentList;
	}
	
	
	private Object getLockUnlockData(Context context, String[] objIds)throws Exception
	{
		String lockedByStr = UINavigatorUtil.getI18nString("emxIEFDesignCenter.Common.LockedBy","emxIEFDesignCenterStringResource", localeLanguage);

		Vector columnCellContentList = new Vector();

		StringList busSelectionList = new StringList(6);
		busSelectionList.addElement("id"); // [NDM] OP6
		busSelectionList.addElement("type");
		busSelectionList.addElement(ATTR_SOURCE); //To get Integrations name.
		busSelectionList.addElement("locker"); 
		busSelectionList.addElement(SELECT_ON_MAJOR + "locker"); //from Minor.
		busSelectionList.addElement(SELECT_ISVERSIONOBJ);
		Vector assignedIntegrations = util.getAssignedIntegrations(context);

		String unlockedString = "iconActionUnlock.gif|" + lockedByStr + ": "; 

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);

			String integrationName	= null;

			String integrationSource	= busObjectWithSelect.getSelectData(ATTR_SOURCE);
			String showLocker			= "";
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
				// [NDM] Start OP6
				//MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);

				//String busType				= busObjectWithSelect.getSelectData("type");
				String busId				= busObjectWithSelect.getSelectData("id");

				if(!isVersion)//gco.isMajorType(busType)) // [NDM] End OP6
				{
					showLocker		= busObjectWithSelect.getSelectData("locker");
				}
				else
				{
					showLocker		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "locker");
				}
			}

			String columnString = unlockedString;

			if(null != showLocker && !showLocker.equals(""))
			{
				//Icon path and tool tip with '|' separation
				StringBuffer imageToolTipStr = new StringBuffer(60);

				imageToolTipStr.append("iconStatusLocked.gif").append("|");
				imageToolTipStr.append(lockedByStr).append(": ").append(showLocker);

				columnString = imageToolTipStr.toString();
			}

			columnCellContentList.add(columnString);
		}

		return columnCellContentList;
	}

	private String[] getLatestIDs(Context context) throws Exception 
	{
		String	paramKey			= "latestid";
		List relBusObjPageList		= (List)paramMap.get("objectList");

		String [] objIds 			= new String[relBusObjPageList.size()];
		try
		{	
			if(!MCADUtil.checkForDataExistense(paramKey, relBusObjPageList))
			{
				// ${CLASS:MCADIntegGetLatestVersion} objGetLatestJPO =  new ${CLASS:MCADIntegGetLatestVersion}(context, integrationNameGCOTable, localeLanguage);
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

		return objIds;
	}
		
	public Object getHtmlStringForTableName(Context context, String [] args) throws Exception
	{
		Vector columnCellContentList = new Vector();

		try
		{
			init(context, args);
			HashMap paramList 			= (HashMap)paramMap.get("paramList");	
			MapList mlData = (MapList) paramList.get("mlObjInfo");

			for(int i = 0; i < mlData.size(); i++)
			{			
				HashMap hmTemp = (HashMap) mlData.get(i);			
				String name				= "";
				String integrationName		= null;
				String integrationSource	= (String) hmTemp.get(ATTR_SOURCE);

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}
				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName))
				{
					MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
				}
				else
				{
					name = (String) hmTemp.get(DomainConstants.SELECT_NAME);
				}

				columnCellContentList.add(name);
			} 
		}
		catch(Exception e) 
		{

		}

		return columnCellContentList;
	}

	
	public Object getHtmlStringForTableDesc(Context context, String [] args) throws Exception
	{
		Vector columnCellContentList = new Vector();

		try
		{
			init(context, args);
			HashMap paramList 			= (HashMap)paramMap.get("paramList");	
			MapList mlData = (MapList) paramList.get("mlObjInfo");

			for(int i = 0; i < mlData.size(); i++)
			{			
				
				HashMap hmTemp = (HashMap) mlData.get(i);				
				String name				= "";
				String integrationName		= null;
				String integrationSource	= (String) hmTemp.get(ATTR_SOURCE);

				if(integrationSource != null)
				{
					StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

					if(integrationSourceTokens.hasMoreTokens())
						integrationName  = integrationSourceTokens.nextToken();
				}
				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName))
				{
					MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
					name = (String) hmTemp.get("desc");
				}
				else
				{
					name = (String) hmTemp.get("desc");
				}
				columnCellContentList.add(name);
			} 
		}
		catch(Exception e) 
		{

		}
		return columnCellContentList;
	}
	
	
	public Object getHtmlStringForTableUpdatestampInst(Context context, String [] args) throws Exception
	{
		Vector columnCellContentList = new Vector();
		try
		{
			init(context, args);
			HashMap paramList 			= (HashMap)paramMap.get("paramList");	
			MapList mlData = (MapList) paramList.get("mlObjInfo");

			for(int i = 0; i < mlData.size(); i++)
			{			
				HashMap hmTemp = (HashMap) mlData.get(i);				
				String name				= "";
				String integrationName		= null;
				name = (String) hmTemp.get("instanceupdatestamp");
				columnCellContentList.add(name);
			} 
		}
		catch(Exception e) 
		{

		}
		return columnCellContentList;
	}	
	
	public Object getHtmlStringForTableUpdatestamp(Context context, String [] args) throws Exception
	{
		Vector columnCellContentList = new Vector();

		try
		{
			init(context, args);
			HashMap paramList 			= (HashMap)paramMap.get("paramList");	
			MapList mlData = (MapList) paramList.get("mlObjInfo");

			for(int i = 0; i < mlData.size(); i++)
			{			

				HashMap hmTemp = (HashMap) mlData.get(i);
				
				String name				= "";
				String integrationName		= null;
				name = (String) hmTemp.get("updatestamp");


				columnCellContentList.add(name);
			} 
		}
		catch(Exception e) 
		{

		}
		return columnCellContentList;
	}	
	
}
