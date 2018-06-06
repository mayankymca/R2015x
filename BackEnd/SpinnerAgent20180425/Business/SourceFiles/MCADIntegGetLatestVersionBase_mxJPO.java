/*
 **  MCADIntegGetLatestVersionBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to get latest version object
 */
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADStringUtils;
import com.matrixone.MCADIntegration.utils.MCADUtil;

public class MCADIntegGetLatestVersionBase_mxJPO 
{
	protected MCADGlobalConfigObject _globalConfig			    = null;
	protected MCADServerResourceBundle _serverResourceBundle	= null;
	protected MCADMxUtil _util						            = null;
	protected IEFGlobalCache _cache								= null;
	protected MCADServerGeneralUtil _generalUtil				= null;

	protected final String dumpCharacter                        = "|";

	protected boolean retrieveHashcode                          = false;

	protected String REL_VERSION_OF							 	= "";
	protected String REL_ACTIVE_VERSION						 	= "";
	protected String REL_LATEST_VERSION							= "";

	protected String ATTR_IEFFILEMESSAGEDIGEST				    = "";

	protected String SELECT_ON_MAJOR						  	= "";
	protected String SELECT_ON_ACTIVE_MINOR					    = "";
	protected String SELECT_ATTR_IEFFILEMESSAGEDIGEST		    = "";
	protected String ATTR_SOURCE								= "";
		protected String IS_VERSION_OBJ							= "";
	protected String SELECT_ISVERSIONOBJ					= "";

	private HashMap integrationNameGCOTable						= null;

	public MCADIntegGetLatestVersionBase_mxJPO ()
	{
	}
	public MCADIntegGetLatestVersionBase_mxJPO(Context context, HashMap integrationNameGCOTable, String language) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("Not supported on desktop client!!!", null);

		this.integrationNameGCOTable = integrationNameGCOTable;

		init(context, null, language, "false");
	}

	public MCADIntegGetLatestVersionBase_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("Not supported on desktop client!!!", null);

		String retrieveHashCodeFromDB   = null;
		if(args!=null && args.length > 1){
		String [] packedGCO				= new String[2];
		packedGCO[0]					= args[0];
		packedGCO[1]					= args[1];
		String sLanguage				= args[2];

		if(args.length==3)
			retrieveHashCodeFromDB        = "false";
		else
			retrieveHashCodeFromDB        = args[3];

		if(retrieveHashCodeFromDB== null || retrieveHashCodeFromDB.equals(""))
			retrieveHashCodeFromDB        = "false";	

		init(context, packedGCO, sLanguage, retrieveHashCodeFromDB);
	}
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	protected void init(Context context, String[] packedGCO, String sLanguage, String retrieveHashCodeFromDB)  throws Exception
	{
		retrieveHashcode 		= MCADUtil.getBoolean(retrieveHashCodeFromDB);
		_serverResourceBundle	= new MCADServerResourceBundle(sLanguage);
		_cache					= new IEFGlobalCache();
		_util					= new MCADMxUtil(context, _serverResourceBundle, _cache);

		if(null != packedGCO)
		{
			_globalConfig			= (MCADGlobalConfigObject) JPO.unpackArgs(packedGCO);
			_generalUtil			= new MCADServerGeneralUtil(context,_globalConfig, _serverResourceBundle, _cache);
		}

		REL_VERSION_OF				= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		REL_ACTIVE_VERSION			= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
		 REL_LATEST_VERSION  = MCADMxUtil.getActualNameForAEFData(context,"relationship_LatestVersion");
		ATTR_IEFFILEMESSAGEDIGEST   = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-FileMessageDigest");
		ATTR_SOURCE					= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";

		SELECT_ON_MAJOR				            = "from[" + REL_VERSION_OF + "].to.";
		SELECT_ON_ACTIVE_MINOR		            = "from[" + REL_ACTIVE_VERSION + "].to.";
		SELECT_ATTR_IEFFILEMESSAGEDIGEST		= "attribute[" + ATTR_IEFFILEMESSAGEDIGEST + "]";
		IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context,"attribute_IsVersionObject");
		SELECT_ISVERSIONOBJ = "attribute["+IS_VERSION_OBJ+"]";
	}

	public String getLatest(Context context, String[] args) throws Exception
	{
		String busid			= args[0];
		// This is a MUST
		Hashtable returnTable	= getLatestForObjectIds(context, args);

		return (String)returnTable.get(busid);
	}

	private Hashtable getIdMajorIdMap(Context context, String[] oids, Hashtable busidBusSelectMap) throws Exception
	{
		Hashtable returnTable = new Hashtable();

		StringList busSelectionList = new StringList(7);

		busSelectionList.addElement("id");
		busSelectionList.addElement("type");

		if(retrieveHashcode)
			busSelectionList.addElement(SELECT_ATTR_IEFFILEMESSAGEDIGEST);

		String SELECT_ON_MAJOR_ID = "to[" + REL_LATEST_VERSION + "].from.id";
		busSelectionList.addElement(SELECT_ON_MAJOR_ID);

		busSelectionList.addElement(ATTR_SOURCE);

		busSelectionList.addElement(SELECT_ON_MAJOR + "id");
		busSelectionList.addElement(SELECT_ISVERSIONOBJ);
		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, oids, busSelectionList);

		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
			String sIsVersion= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
			boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
			String busID			 = busObjectWithSelect.getSelectData("id");
			String busType			 = busObjectWithSelect.getSelectData("type");
			String integrationSource = busObjectWithSelect.getSelectData(ATTR_SOURCE);

			String outputId			 = busID;

			if(!MCADStringUtils.isNullOrEmpty(integrationSource))
			{
				if(null == _globalConfig)
				{
					String integrationName		= null;

					if(integrationSource != null)
					{
						StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

						if(integrationSourceTokens.hasMoreTokens())
							integrationName  	= integrationSourceTokens.nextToken();
					}

					if(integrationName != null && integrationNameGCOTable.containsKey(integrationName))
					{
						_globalConfig	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
						_generalUtil	= new MCADServerGeneralUtil(context,_globalConfig, _serverResourceBundle, _cache);
					}
				}

				//if(!_globalConfig.isMajorType(busType))                         //[NDM]
                                if(isVersion) 
				{	
				if(_generalUtil.isObjectBulkLoaded(context, busID))
					outputId = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR_ID);
					else
						outputId = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "id");
				}
			}

			returnTable.put(busID, outputId);

			busidBusSelectMap.put(busID, busObjectWithSelect);
		}

		return returnTable;
	}

	public Hashtable getLatestForObjectIdsUpdated(Context context, String[] args) throws Exception {
		Hashtable returnTable = null;
    	try{
			//	System.out.println("in the jpo method");
			HashMap paramMap = (HashMap) JPO.unpackArgs( args );
			String[] arrOID = (String[])paramMap.get( "objIds" );//Product OID
			HashMap gcoTable = (HashMap) paramMap.get("GCOTable");
			String sLocaleLang = (String) paramMap.get("LocaleLang");
			
			this.integrationNameGCOTable = gcoTable;
			init(context, null, sLocaleLang, "false");
			//		System.out.println("in the jpo method..after init");
			returnTable	= getLatestForObjectIds(context, arrOID);
			//		System.out.println("in the jpo method..after getlatest --"+returnTable);
      	}catch(Exception ex){
    		ex.printStackTrace();
    	}

		return returnTable;
	}

	public Hashtable getLatestForObjectIds(Context context, String[] oids) throws Exception
	{
		Hashtable returnTable			= new Hashtable(oids.length);
		HashSet revisionCreatedManually = new HashSet();

		try
		{
			Hashtable busidBusselectMap = new Hashtable();

			Hashtable idMajoridMap = getIdMajorIdMap(context, oids, busidBusselectMap);

			String[] majoroids = new String[idMajoridMap.values().size()];

			idMajoridMap.values().toArray(majoroids);

			StringList busSelectionList = new StringList();

			busSelectionList.addElement("id");
			busSelectionList.addElement("last." + SELECT_ON_ACTIVE_MINOR + "id"); // latest minor from major
			busSelectionList.addElement("last.policy"); // major policy from minor
			busSelectionList.addElement("last.current"); // major state from minor
			busSelectionList.addElement("last.id"); // latest major id from major
			busSelectionList.addElement("last.state"); // latest major statelist from major
			busSelectionList.addElement(SELECT_ISVERSIONOBJ); // DEC_SLW_TRACE: Added this to be able to determine whether the object is a versioned object.

			if(retrieveHashcode)
			{
				busSelectionList.addElement("last." + SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST );
				busSelectionList.addElement("last." + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
			}

			HashMap majorIdBusSelectMap = new HashMap();

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, majoroids, busSelectionList);

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{
				BusinessObjectWithSelect busObjectWithSelect = buslWithSelectionList.getElement(i);

				String majorID			 = busObjectWithSelect.getSelectData("id");

				majorIdBusSelectMap.put(majorID, busObjectWithSelect);
			}

			Enumeration inputids = idMajoridMap.keys();

			while (inputids.hasMoreElements())
			{
				String outputId			 = null;
				String outputIdHashcode  = null;

				String busID   = (String)inputids.nextElement();
				BusinessObjectWithSelect busObjectBasicSelect = (BusinessObjectWithSelect) busidBusselectMap.get(busID);

				String integrationSource = busObjectBasicSelect.getSelectData(ATTR_SOURCE);

				if(!MCADStringUtils.isNullOrEmpty(integrationSource))
				{
					String majorID = (String) idMajoridMap.get(busID);

					BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)majorIdBusSelectMap.get(majorID);

					//handle major inbus
					String latestMajorPolicy		= busObjectWithSelect.getSelectData("last.policy");
					String latestMajorState			= busObjectWithSelect.getSelectData("last.current");
					StringList latestMajorStateList = busObjectWithSelect.getSelectDataList("last.state");

					String finalizationState		= _globalConfig.getFinalizationState(latestMajorPolicy);

					String sIsVersionObj 	= (String)busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
					boolean isVersionObject = Boolean.valueOf(sIsVersionObj).booleanValue();

					// XEW - IR-472394-3DEXPERIENCER2015x +
          
					/*if(_util.isSolutionBasedEnvironment(context)){
						if(latestMajorStateList.lastIndexOf(latestMajorState) >= latestMajorStateList.lastIndexOf(finalizationState))
						{
							outputId         = busObjectWithSelect.getSelectData("last.id"); // major id
							if(retrieveHashcode)
								outputIdHashcode = busObjectWithSelect.getSelectData("last."+SELECT_ATTR_IEFFILEMESSAGEDIGEST);
						}
						else
						{
							//outputId         = busID; //[NDM] id is coming as major id. No need to find minor id
							outputId         = busObjectWithSelect.getSelectData("last." + SELECT_ON_ACTIVE_MINOR + "id"); // minor id
							if(retrieveHashcode)
								outputIdHashcode = busObjectWithSelect.getSelectData("last." + SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
						}
					}else{
						outputId         = busObjectWithSelect.getSelectData("last.id"); // major id
						if(retrieveHashcode)
							outputIdHashcode = busObjectWithSelect.getSelectData("last."+SELECT_ATTR_IEFFILEMESSAGEDIGEST);
					}*/
					
          
					if(!_util.isSolutionBasedEnvironment(context)) // We are in a migrated (High End) environment
					{
						// Check whether the object is a versioned object.
						if (true == isVersionObject) // if it is a versioned object then return the id of the Major version.
						{
							outputId = busObjectWithSelect.getSelectData("last.id");
							if(retrieveHashcode)
                outputIdHashcode = busObjectWithSelect.getSelectData("last."+SELECT_ATTR_IEFFILEMESSAGEDIGEST);
						}
						else // otherwise return the id of the active minor iteration
						{	
							outputId         = busObjectWithSelect.getSelectData("last." + SELECT_ON_ACTIVE_MINOR + "id");
							if(retrieveHashcode)
                outputIdHashcode = busObjectWithSelect.getSelectData("last." + SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
						}
					}
					else // We are in OCDX environment
					{
						// Check whether the object is at or beyond the finalization state i.e. whether it is finalized.
						// if it is so, return the id of the Major version
						if(latestMajorStateList.lastIndexOf(latestMajorState) >= latestMajorStateList.lastIndexOf(finalizationState))
					  {
					    	outputId = busObjectWithSelect.getSelectData("last.id");
					    	if(retrieveHashcode)
                  outputIdHashcode = busObjectWithSelect.getSelectData("last."+SELECT_ATTR_IEFFILEMESSAGEDIGEST);
					  }
					  else // otherwise return the id of the Active Minor
					  {
					    	outputId = busObjectWithSelect.getSelectData("last." + SELECT_ON_ACTIVE_MINOR + "id");
					    	if(retrieveHashcode)
                  outputIdHashcode = busObjectWithSelect.getSelectData("last." + SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
					  }
					}

					// XEW - IR-472394-3DEXPERIENCER2015x -
          
					if(outputId == null || outputId.equals(""))
					{
						revisionCreatedManually.add(busID);

						outputId         = busID;

						if(retrieveHashcode)
							outputIdHashcode = busObjectBasicSelect.getSelectData(SELECT_ATTR_IEFFILEMESSAGEDIGEST);
					}

					if(retrieveHashcode)
						outputId = outputId + "|" + outputIdHashcode;

					returnTable.put(busID, outputId);
				}
				else
				{
					returnTable.put(busID, busID);
				}
			}

			if(revisionCreatedManually.size() > 0)
			{
				Hashtable newReturnTable =	processManuallyCreatedRevisions(context, revisionCreatedManually);
				returnTable.putAll(newReturnTable);
			}
		}
		catch(Exception e)
		{
			System.out.println("[MCADIntegGetLatestVersionBase.getLatestForObjectIds]: Error - " + e.getMessage());
		}

		return returnTable;
	}

	private Hashtable processManuallyCreatedRevisions(Context context, HashSet revisionCreatedManually) throws Exception
	{
		Hashtable returnTable = new Hashtable(revisionCreatedManually.size());

		try
		{
			String [] oids		  = new String [revisionCreatedManually.size()];
			revisionCreatedManually.toArray(oids);

			StringList busSelectionList = new StringList(21);

			busSelectionList.addElement("id");
			busSelectionList.addElement("type");

			if(null == _globalConfig)
				busSelectionList.addElement(ATTR_SOURCE);

			busSelectionList.addElement(SELECT_ON_MAJOR + "revisions");
			busSelectionList.addElement(SELECT_ON_MAJOR + "revisions." + SELECT_ON_ACTIVE_MINOR + "id");
			busSelectionList.addElement(SELECT_ON_MAJOR + "revisions.policy");
			busSelectionList.addElement(SELECT_ON_MAJOR + "revisions.current");
			busSelectionList.addElement(SELECT_ON_MAJOR + "revisions.id");
			busSelectionList.addElement(SELECT_ON_MAJOR + "revisions.state");

			busSelectionList.addElement("revisions"); 
			busSelectionList.addElement("revisions." + SELECT_ON_ACTIVE_MINOR + "id");
			busSelectionList.addElement("revisions.policy");
			busSelectionList.addElement("revisions.current");
			busSelectionList.addElement("revisions.id");
			busSelectionList.addElement("revisions.state");
			busSelectionList.addElement(SELECT_ISVERSIONOBJ);
			if(retrieveHashcode)
			{
				busSelectionList.addElement(SELECT_ATTR_IEFFILEMESSAGEDIGEST);
				busSelectionList.addElement(SELECT_ON_MAJOR + "revisions." + SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
				busSelectionList.addElement(SELECT_ON_MAJOR + "revisions." + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
				busSelectionList.addElement("revisions." + SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
				busSelectionList.addElement("revisions." + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
			}

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, oids, busSelectionList);

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{
				String outputId								 = null;
				String outputIdHashcode                      = null;

				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
			   String sIsVersion= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
			   boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
				String busID								 = busObjectWithSelect.getSelectData("id");
				//String busType								 = busObjectWithSelect.getSelectData("type"); // {NDM] OP6

				if(null == _globalConfig)
				{
					String integrationName		= null;
					String integrationSource	= busObjectWithSelect.getSelectData(ATTR_SOURCE);
					if(integrationSource != null)
					{
						StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

						if(integrationSourceTokens.hasMoreTokens())
							integrationName  = integrationSourceTokens.nextToken();
					}
					if(integrationName != null && integrationNameGCOTable.containsKey(integrationName))
						_globalConfig	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);						
				}

				if(!isVersion)//_globalConfig.isMajorType(busType)) // {NDM] OP6
				{
					StringList majorRevisions				 = busObjectWithSelect.getSelectDataList("revisions");
					for(int j = (majorRevisions.size() - 1); j >=0 ;j--)
					{
						String latestMajorPolicy			 = busObjectWithSelect.getSelectData("revisions[" + majorRevisions.elementAt(j) + "].policy");
						String latestMajorState				 = busObjectWithSelect.getSelectData("revisions[" + majorRevisions.elementAt(j) + "].current");
						StringList latestMajorStateList		 = busObjectWithSelect.getSelectDataList("revisions[" + majorRevisions.elementAt(j) + "].state");

						String finalizationState			 = _globalConfig.getFinalizationState(latestMajorPolicy);

						if(latestMajorStateList.lastIndexOf(latestMajorState) >= latestMajorStateList.lastIndexOf(finalizationState))
						{
							outputId         = busObjectWithSelect.getSelectData("revisions[" + majorRevisions.elementAt(j) + "].id"); // major id
							if(retrieveHashcode)
								outputIdHashcode = busObjectWithSelect.getSelectData("revisions[" + majorRevisions.elementAt(j) + "]."+ SELECT_ATTR_IEFFILEMESSAGEDIGEST);
						}
						else
						{
							outputId         = busObjectWithSelect.getSelectData("revisions[" + majorRevisions.elementAt(j) + "]." + SELECT_ON_ACTIVE_MINOR + "id"); // minor id
							if(retrieveHashcode)
								outputIdHashcode = busObjectWithSelect.getSelectData("revisions[" + majorRevisions.elementAt(j) + "]." + SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
						}

						if(outputId != null && !outputId.equals(""))
							break;
					}
				}
				else
				{
					StringList majorRevisions			= busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR + "revisions");
					for(int j = (majorRevisions.size() - 1); j >=0 ;j--)
					{
						String latestMajorPolicy		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + majorRevisions.elementAt(j) + "].policy");
						String latestMajorState			= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + majorRevisions.elementAt(j) + "].current");
						StringList latestMajorStateList = busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR + "revisions[" + majorRevisions.elementAt(j) + "].state");

						String finalizationState		= _globalConfig.getFinalizationState(latestMajorPolicy);

						if(latestMajorStateList.lastIndexOf(latestMajorState) >= latestMajorStateList.lastIndexOf(finalizationState))
						{
							outputId         = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + majorRevisions.elementAt(j) + "].id"); // major id
							if(retrieveHashcode)
								outputIdHashcode = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + majorRevisions.elementAt(j) + "]."+ SELECT_ATTR_IEFFILEMESSAGEDIGEST);
						}
						else
						{
							outputId         = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + majorRevisions.elementAt(j) + "]." + SELECT_ON_ACTIVE_MINOR + "id"); // minor id
							if(retrieveHashcode)
								outputIdHashcode = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + majorRevisions.elementAt(j) + "]." + SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
						}

						if(outputId != null && !outputId.equals(""))
							break;
					}
				}

				if(outputId == null || outputId.equals(""))
				{
					outputId = busID;
					if(retrieveHashcode)
						outputIdHashcode = busObjectWithSelect.getSelectData(SELECT_ATTR_IEFFILEMESSAGEDIGEST);
				}

				if(retrieveHashcode)
					outputId = outputId + "|" + outputIdHashcode;

				returnTable.put(busID, outputId);
			}
		}
		catch(Exception e)
		{
			System.out.println("[MCADIntegGetLatestVersionBase.processManuallyCreatedRevisions]: Error - " + e.getMessage());
		}

		return returnTable;
	}
	//new method added for SLW
	public Hashtable getLatestForObjectIdsUpdatedSLWPDM(Context context, String[] args) throws Exception {
		Hashtable returnTable = null;
    	try{
    	//	System.out.println("in the jpo method");
			HashMap paramMap = (HashMap) JPO.unpackArgs( args );
			String[] arrOID = (String[])paramMap.get( "objIds" );//Product OID
			HashMap gcoTable = (HashMap) paramMap.get("GCOTable");
			String sLocaleLang = (String) paramMap.get("LocaleLang");
			
			this.integrationNameGCOTable = gcoTable;
			init(context, null, sLocaleLang, "false");
	//		System.out.println("in the jpo method..after init");
			returnTable	= getLatestForObjectIdsSLWDPM(context, arrOID);
	//		System.out.println("in the jpo method..after getlatest --"+returnTable);
      	}catch(Exception ex){
    		ex.printStackTrace();
    	}

		return returnTable;
	}
	public Hashtable getLatestForObjectIdsSLWDPM(Context context, String[] oids) throws Exception
	{
		Hashtable returnTable			= new Hashtable(oids.length);
		HashSet revisionCreatedManually = new HashSet();

		try
		{
			Hashtable busidBusselectMap = new Hashtable();

			Hashtable idMajoridMap = getIdMajorIdMap(context, oids, busidBusselectMap);

			String[] majoroids = new String[idMajoridMap.values().size()];

			idMajoridMap.values().toArray(majoroids);

			StringList busSelectionList = new StringList();

			busSelectionList.addElement("id");
			busSelectionList.addElement("last." + SELECT_ON_ACTIVE_MINOR + "id"); // latest minor from major
			busSelectionList.addElement("last.policy"); // major policy from minor
			busSelectionList.addElement("last.current"); // major state from minor
			busSelectionList.addElement("last.id"); // latest major id from major
			busSelectionList.addElement("last.state"); // latest major statelist from major

			if(retrieveHashcode)
			{
				busSelectionList.addElement("last." + SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST );
				busSelectionList.addElement("last." + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
			}

			HashMap majorIdBusSelectMap = new HashMap();

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, majoroids, busSelectionList);

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{
				BusinessObjectWithSelect busObjectWithSelect = buslWithSelectionList.getElement(i);

				String majorID			 = busObjectWithSelect.getSelectData("id");

				majorIdBusSelectMap.put(majorID, busObjectWithSelect);
			}

			Enumeration inputids = idMajoridMap.keys();

			while (inputids.hasMoreElements())
			{
				String outputId			 = null;
				String outputIdHashcode  = null;

				String busID   = (String)inputids.nextElement();
				BusinessObjectWithSelect busObjectBasicSelect = (BusinessObjectWithSelect) busidBusselectMap.get(busID);

				String integrationSource = busObjectBasicSelect.getSelectData(ATTR_SOURCE);

				if(!MCADStringUtils.isNullOrEmpty(integrationSource))
				{
					String majorID = (String) idMajoridMap.get(busID);

					BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)majorIdBusSelectMap.get(majorID);

					//handle major inbus
					String latestMajorPolicy		= busObjectWithSelect.getSelectData("last.policy");
					String latestMajorState			= busObjectWithSelect.getSelectData("last.current");
					StringList latestMajorStateList = busObjectWithSelect.getSelectDataList("last.state");

					String finalizationState		= _globalConfig.getFinalizationState(latestMajorPolicy);

					if(_util.isSolutionBasedEnvironment(context)){
						//outputId         = busID; //[NDM] id is coming as major id. No need to find minor id
						outputId         = busObjectWithSelect.getSelectData("last." + SELECT_ON_ACTIVE_MINOR + "id"); // minor id
						if(retrieveHashcode)
							outputIdHashcode = busObjectWithSelect.getSelectData("last." + SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
					}else{
						outputId         = busObjectWithSelect.getSelectData("last.id"); // major id
						if(retrieveHashcode)
							outputIdHashcode = busObjectWithSelect.getSelectData("last."+SELECT_ATTR_IEFFILEMESSAGEDIGEST);
					}

					if(outputId == null || outputId.equals(""))
					{
						revisionCreatedManually.add(busID);

						outputId         = busID;

						if(retrieveHashcode)
							outputIdHashcode = busObjectBasicSelect.getSelectData(SELECT_ATTR_IEFFILEMESSAGEDIGEST);
					}

					if(retrieveHashcode)
						outputId = outputId + "|" + outputIdHashcode;

					returnTable.put(busID, outputId);
				}
				else
				{
					returnTable.put(busID, busID);
				}
			}

			if(revisionCreatedManually.size() > 0)
			{
				Hashtable newReturnTable =	processManuallyCreatedRevisions(context, revisionCreatedManually);
				returnTable.putAll(newReturnTable);
			}
		}
		catch(Exception e)
		{
			System.out.println("[MCADIntegGetLatestVersionBase.getLatestForObjectIds]: Error - " + e.getMessage());
		}

		return returnTable;
	}
}
