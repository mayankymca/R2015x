/*
 * IEFFamilyAttrToObjectMigrator.java program to get all document type Object Ids.
 *
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
 *
 */

/*
 * Family table modifications
 * 1. Create objects for instances (major and minor)
 * 2. Connect instances with family like relation
 * 3. Change type of family type objects
 * 4. set attributes on instance objects
 * 5. set lock and owner
 * 6. Clean attributes related to Attribute based configuration
 * 7. Move CAD SubComponent/Part Specification relations to appropriate instance object
 * 8. Promote Instances to Family object's state
 *
 * ProE file name modification
 * 1. remove .N extension for ProE files
 * 2. Change Hashcode attribute to refer to this new file name
 *
 * Working set modifications
 * 1. Change type of working set entries of family objects
 * 2. Add corresponding instance object entries for each family entry
 * 3. Change file name entry for ProE files to chop off .N extension
 *
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.MQLCommand;
import matrix.db.MatrixWriter;
import matrix.db.State;
import matrix.db.StateItr;
import matrix.db.StateList;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNodeImpl;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;

public class IEFFamilyAttrToObjectMigrator_mxJPO
{
    private BufferedWriter writer		= null;
	private FileWriter iefLog		= null;
	private FileWriter wsDataFile		= null;
	private FileWriter errorIDsFile		= null;
	private long startTime				= 0L;

	private MCADMxUtil mxUtil		= null;
	private String documentDirectory	= "";
	private String migrationComponent	= "";
	private String wsDataFileName		= "IEFMigrationWorkingSetData.txt";
	private String migrationIDsFileName	= "IEF_MigrationIDs_Family.txt";

	private Hashtable integNameGCOMap	= null;
	private Hashtable gcoTypeDetailsMap	= null;
	private Hashtable gcoObjSelectMap	= null;
	private Vector handledObjIDs		= null;
	private String coreVersion		= null;

	private Hashtable familyTNRInstanceDetailsTable = null;

	private String REL_VERSION_OF				= "";
	private String REL_FINALIZED				= "";
	private String REL_ACTIVE_VERSION			= "";
	private String REL_LATEST_VERSION			= "";
	private String REL_PART_SPECIFICATION			= "";
	private String ATTR_CAD_TYPE				= "";
	private String ATTR_SOURCE				= "";
	private String ATTR_CHILD_INSTANCE			= "";
	private String ATTR_PARENT_INSTANCE			= "";
	private String ATTR_INSTANCE_NAMES			= "";
	private String ATTR_INSTANCE_STRUCTURE			= "";
	private String ATTR_INSTANCES				= "";
	private String ATTR_INSTANCE_ATTR			= "";
	private String ATTR_INSTANCE_SPECIFIC_ATTR		= "";
	private String ATTR_IS_FINALIZED			= "";
	private String ATTR_CAD_OBJECT_NAME			= "";

	private String SELECT_ATTR_REL_IS_FINALIZED_FOR_MAJOR	= "";

	private String SELECT_REVISION_IDS			= "";
	private String SELECT_REVISION_LIST			= "";
	private String SELECT_ATTR_CADTYPE			= "";
	private String SELECT_ATTR_SOURCE			= "";
	private String SELECT_MAJOR_ACTIVE_REL			= "";
	private String SELECT_MAJOR_LATEST_REL			= "";
	private String SELECT_MAJOR_ID				= "";
	private String SELECT_MINOR_ID_LIST			= "";
	private String SELECT_FINALIZED_MINOR_ID		= "";
	private String SELECT_MAJOR_FINALIZED_REL		= "";
	private String SELECT_ATTR_INSTANCE_NAMES		= "";
	private String SELECT_ATTR_INSTANCE_STRUCTURE		= "";
	private String SELECT_ATTR_INSTANCES			= "";
	private String SELECT_ATTR_INSTANCE_ATTR		= "";
	private String SELECT_ATTR_INSTANCE_SPECIFIC_ATTR	= "";
	private String SELECT_PART_SPECIFICATION_ID		= "";
	private String SELECT_PART_SPEC_ATTR_CAD_OBJ_NAME	= "";


    public IEFFamilyAttrToObjectMigrator_mxJPO (Context context, String[] args)
        throws Exception
    {
		writer = new BufferedWriter(new MatrixWriter(context));
		mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle("en-US"), new IEFGlobalCache());
		
		REL_VERSION_OF				= PropertyUtil.getSchemaProperty(context,"relationship_VersionOf");
		REL_FINALIZED				= PropertyUtil.getSchemaProperty(context,"relationship_Finalized");
		REL_ACTIVE_VERSION			= PropertyUtil.getSchemaProperty(context,"relationship_ActiveVersion");
		REL_LATEST_VERSION			= PropertyUtil.getSchemaProperty(context,"relationship_LatestVersion");
		REL_PART_SPECIFICATION			= PropertyUtil.getSchemaProperty(context,"relationship_PartSpecification");
		ATTR_CAD_TYPE				= PropertyUtil.getSchemaProperty(context,"attribute_CADType");
		ATTR_SOURCE				= PropertyUtil.getSchemaProperty(context,"attribute_Source");
		ATTR_CHILD_INSTANCE			= PropertyUtil.getSchemaProperty(context,"attribute_ChildInstance");
		ATTR_PARENT_INSTANCE			= PropertyUtil.getSchemaProperty(context,"attribute_ParentInstance");
		ATTR_INSTANCE_NAMES			= PropertyUtil.getSchemaProperty(context,"attribute_MCADInteg-InstanceNames");
		ATTR_INSTANCE_STRUCTURE			= PropertyUtil.getSchemaProperty(context,"attribute_MCADInteg-InstanceStructure");
		ATTR_INSTANCES				= PropertyUtil.getSchemaProperty(context,"attribute_MCADInteg-Instances");
		ATTR_INSTANCE_ATTR			= PropertyUtil.getSchemaProperty(context,"attribute_MCADInteg-InstanceAttributes");
		ATTR_INSTANCE_SPECIFIC_ATTR		= PropertyUtil.getSchemaProperty(context,"attribute_MCADInteg-InstanceSpecificAttributes");
		ATTR_IS_FINALIZED			= PropertyUtil.getSchemaProperty(context,"attribute_IsFinalized");
		ATTR_CAD_OBJECT_NAME			= PropertyUtil.getSchemaProperty(context,"attribute_CADObjectName");

		SELECT_ATTR_REL_IS_FINALIZED_FOR_MAJOR	= "to[" + REL_VERSION_OF + "].attribute[" + ATTR_IS_FINALIZED + "]";

		SELECT_REVISION_IDS			= "revisions.id";
		SELECT_REVISION_LIST			= "revisions";
		SELECT_ATTR_CADTYPE			= "attribute[" + ATTR_CAD_TYPE + "]";
		SELECT_ATTR_SOURCE			= "attribute[" + ATTR_SOURCE + "]";
		SELECT_MAJOR_ACTIVE_REL			= "to[" + REL_ACTIVE_VERSION + "].from.id";
		SELECT_MAJOR_LATEST_REL			= "to[" + REL_LATEST_VERSION + "].from.id";
		SELECT_MAJOR_ID				= "from[" + REL_VERSION_OF + "].to.id";
		SELECT_MINOR_ID_LIST			= "to[" + REL_VERSION_OF + "].from.id";
		SELECT_FINALIZED_MINOR_ID		= "to[" + REL_FINALIZED + "].from.id";
		SELECT_MAJOR_FINALIZED_REL		= "from[" + REL_FINALIZED + "].to.id";
		SELECT_ATTR_INSTANCE_NAMES		= "attribute[" + ATTR_INSTANCE_NAMES + "]";
		SELECT_ATTR_INSTANCE_STRUCTURE		= "attribute[" + ATTR_INSTANCE_STRUCTURE + "]";
		SELECT_ATTR_INSTANCES			= "attribute[" + ATTR_INSTANCES + "]";
		SELECT_ATTR_INSTANCE_ATTR		= "attribute[" + ATTR_INSTANCE_ATTR + "]";
		SELECT_ATTR_INSTANCE_SPECIFIC_ATTR	= "attribute[" + ATTR_INSTANCE_SPECIFIC_ATTR + "]";
		SELECT_PART_SPECIFICATION_ID		= "to[" + REL_PART_SPECIFICATION + "].id";
		SELECT_PART_SPEC_ATTR_CAD_OBJ_NAME	= "to[" + REL_PART_SPECIFICATION + "].attribute[" + ATTR_CAD_OBJECT_NAME + "]";

		DomainObject.MULTI_VALUE_LIST.add(SELECT_MINOR_ID_LIST);
		DomainObject.MULTI_VALUE_LIST.add(SELECT_REVISION_LIST);
		DomainObject.MULTI_VALUE_LIST.add(SELECT_PART_SPECIFICATION_ID);
		DomainObject.MULTI_VALUE_LIST.add(SELECT_PART_SPEC_ATTR_CAD_OBJ_NAME);
    }

    public int mxMain(Context context, String[] args)
        throws Exception
    {
		if(!context.isConnected())
		{
			throw new Exception("not supported on desktop client");
		}

		try
		{
			validateInputArguments(args);
		}
		catch (Exception iExp)
		{
			writeErrorToConsole(iExp.getMessage());
			writer.close();
			return 0;
		}

		try
		{
			startIEFLog();
			String Args[] = new String[1];
			Args[0] = "off";
			mxUtil.executeMQL(context,"trigger $1", Args);
			integNameGCOMap					= new Hashtable();
			gcoTypeDetailsMap				= new Hashtable();
			gcoObjSelectMap					= new Hashtable();
			handledObjIDs					= new Vector();

			logTimeForEvent("START MIGRATION");
			startTime = System.currentTimeMillis();

			if(migrationComponent.equalsIgnoreCase("all") || migrationComponent.equalsIgnoreCase("Object"))
			{
				startWSDataFile();
				Hashtable objectListIntegNameTable = readIDsFile(migrationIDsFileName);
				logTimeForEvent("START PROCESSING FILE: ");

				Enumeration objListEnum = objectListIntegNameTable.keys();
				while(objListEnum.hasMoreElements())
				{
					StringList objectList = (StringList) objListEnum.nextElement();
					if(objectList.size() > 0 && !handledObjIDs.contains(objectList.elementAt(0)))
					{
						try
						{
							String integrationName = (String) objectListIntegNameTable.get(objectList);

							mxUtil.startTransaction(context);
							familyTNRInstanceDetailsTable = new Hashtable();
							migrateAllFamilyObjects(context, objectList, integrationName);
							writeWSData();
							log("Commiting transaction");
							context.commit();
						}
						catch(Exception e)
						{
							e.printStackTrace(new PrintWriter(iefLog, true));
							context.abort();
							writeIDToErrorFile(context, (String)objectList.elementAt(0));
						}
					}
				}
				closeWSDataFile();
			}

			if(migrationComponent.equalsIgnoreCase("all") || migrationComponent.equalsIgnoreCase("WorkingSet"))
			{
				readWSDataFile();
				migrateAllWorkingSets(context);
			}

			if(migrationComponent.equalsIgnoreCase("all") || migrationComponent.equalsIgnoreCase("ProEFile"))
			{
				renameFilesForProE(context);
			}

			logTimeForEvent("MIGRATION COMPLETE");
			writeSuccessToConsole();
		}
		catch (FileNotFoundException fEx)
		{
			writeErrorToConsole("Directory does not exist or does not have access to the directory");
		}
		catch (Exception ex)
		{
			writeErrorToConsole("Family migration failed: " + ex.getMessage());
			ex.printStackTrace(new PrintWriter(iefLog, true));
		}

		endIEFLog();
		writer.flush();
		writer.close();
        return 0;
    }

	private void migrateAllFamilyObjects(Context context, StringList objectIdList, String integrationName) throws Exception
	{
		String[] oidsArray	= new String[objectIdList.size()];
		oidsArray			= (String[])objectIdList.toArray(oidsArray);

		MCADGlobalConfigObject globalConf = getGlobalConfigObject(context, integrationName);
		log("GCO found for Integration: '" + integrationName + "'");
		StringList objectSelectElements = getObjectSelectElements(context, globalConf);
		MapList mapList = DomainObject.getInfo(context, oidsArray, objectSelectElements);
		Hashtable idDetailsTable = getDetailsTable(mapList);
		//log("idDetailsTable = " + idDetailsTable);

		Hashtable familyTower	= getFamilyTower(oidsArray[0], globalConf, idDetailsTable);
		migrateFamilyTower(context, familyTower, idDetailsTable, globalConf, integrationName);
    }

	private void migrateAllWorkingSets(Context context) throws Exception
	{
		log("\n\n===============================================");
		log("\nStarted Working set migration");
		log("familyTNRInstanceDetailsTable = " + familyTNRInstanceDetailsTable);

		String Args[] = new String[6];
		Args[0] = "MCADInteg-LocalConfig";
		Args[1] = "*";
		Args[2] = "1";
		Args[3] = "id";
		Args[4] = "format.file.store";
		Args[5] = "|";
		String lcoIDList = executeMQL(context,"temp query bus $1 $2 $3 select $4 $5 dump $6", Args);
		try
		{
			StringTokenizer tokenizer = new StringTokenizer(lcoIDList, "\n");
			while(tokenizer.hasMoreElements())
			{
				String lcoObjDetails = (String) tokenizer.nextElement();
				log("lcoObjDetails = " + lcoObjDetails);
				StringTokenizer lcoTok = new StringTokenizer(lcoObjDetails, "|");
				if(lcoTok.hasMoreElements())
				{
					String lcoType	= (String) lcoTok.nextElement();
					String lcoName	= (String) lcoTok.nextElement();
					String lcoRev	= (String) lcoTok.nextElement();
					String lcoID	= (String) lcoTok.nextElement();

					String store = null;
					if(lcoTok.hasMoreElements())
						store = (String) lcoTok.nextElement();
					else
						continue;

					String tempDir = documentDirectory;
					modifyWorkingSetFile(context, lcoID, store, tempDir);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace(new PrintWriter(iefLog, true));
		}
		log("\nCompleted Working set migration");
	}

	private void migrateFamilyTower(Context context, Hashtable familyTower, Hashtable idDetailsTable, MCADGlobalConfigObject globalConf, String integrationName) throws Exception
	{
		log("Started family tower migration");
		Hashtable instancePrevMajorNameIdTable = null;
		Vector majorRevList = (Vector) familyTower.get("MajorRevList");

		Map majorMap = (Map) idDetailsTable.get(majorRevList.elementAt(0));
		String familyCadType = (String) majorMap.get(SELECT_ATTR_CADTYPE);
		if(familyCadType == null || familyCadType.equals("") )
		{
			Vector minorList = (Vector) familyTower.get(majorRevList.elementAt(0));
			if(minorList != null && minorList.size() > 0)
			{
				String minorFamId	= (String) minorList.elementAt(minorList.size()-1);
				Map minorFamilyMap	= (Map) idDetailsTable.get(minorFamId);
				familyCadType		= (String) minorFamilyMap.get(SELECT_ATTR_CADTYPE);
			}
		}

		for(int i=0; i<majorRevList.size(); i++)
		{
			String majorId = (String) majorRevList.elementAt(i);
			log("migrating majorId = " + majorId);
			Map majorRevMap = (Map) idDetailsTable.get(majorId);
			String majorRevType = (String) majorRevMap.get(DomainObject.SELECT_TYPE);
			changeFamilyType(context, globalConf, majorId, familyCadType, majorRevType, true, integrationName);
			Hashtable instanceMajorNameIdTable = createAndConnectInstances(context, majorId, null, instancePrevMajorNameIdTable, globalConf, idDetailsTable, true);

			Hashtable instancePrevMinorNameIdTable = null;
			Vector minorList = (Vector) familyTower.get(majorId);
			for(int j=0; j<minorList.size(); j++)
			{
				String minorId = (String) minorList.elementAt(j);
				log("migrating minorId = " + minorId);
				Map minorRevMap = (Map) idDetailsTable.get(minorId);
				String minorRevType = (String) minorRevMap.get(DomainObject.SELECT_TYPE);
				changeFamilyType(context, globalConf, minorId, familyCadType, minorRevType, false, integrationName);
				instancePrevMinorNameIdTable = createAndConnectInstances(context, minorId, instanceMajorNameIdTable, instancePrevMinorNameIdTable, globalConf, idDetailsTable, false);
			}

			instancePrevMajorNameIdTable = instanceMajorNameIdTable;
		}
		log("Completed family tower migration\n\n");
    }

	private Hashtable createAndConnectInstances(Context context, String familyId, Hashtable instanceMajorNameIdTable, Hashtable instanceOldRevNameIdTable, MCADGlobalConfigObject globalConf, Hashtable idDetailsTable, boolean isMajor) throws Exception
	{
		Hashtable instanceNameIdTable = new Hashtable();

		Map actualFamilyMap		= (Map) idDetailsTable.get(familyId);
		Map familyMap			= actualFamilyMap;
		String familyCadType	= (String) familyMap.get(SELECT_ATTR_CADTYPE);
		if(familyCadType == null || familyCadType.equals("") )
		{
			Vector minorList = (Vector) familyMap.get(SELECT_MINOR_ID_LIST);
			if(minorList != null && minorList.size() > 0)
			{
				String minorFamId	= (String) minorList.elementAt(minorList.size()-1);
				familyMap			= (Map) idDetailsTable.get(minorFamId);
				familyCadType		= (String) familyMap.get(SELECT_ATTR_CADTYPE);
			}
		}

		String familyName			= (String) actualFamilyMap.get(DomainObject.SELECT_NAME);
		log("familyName = " + familyName);
		String familyType			= (String) actualFamilyMap.get(DomainObject.SELECT_TYPE);
		String familyRev			= (String) actualFamilyMap.get(DomainObject.SELECT_REVISION);

		String instanceNames		= (String) familyMap.get(SELECT_ATTR_INSTANCE_NAMES);
		String activeMajorId		= (String) familyMap.get(SELECT_MAJOR_ACTIVE_REL);
		IEFXmlNodeImpl instanceStructure = (IEFXmlNodeImpl)convertToObject((String)familyMap.get(SELECT_ATTR_INSTANCE_STRUCTURE));
		Hashtable instances			= (Hashtable)convertToObject((String)familyMap.get(SELECT_ATTR_INSTANCES));

		if(instances == null || instanceStructure == null)
			return new Hashtable();

		Hashtable instParentTable	= getInstanceStructure(instanceStructure, instances);
		log("instanceNames = " + instanceNames);

		StringTokenizer nameTokenizer = new StringTokenizer(instanceNames, "|");
		while(nameTokenizer.hasMoreElements())
		{
			String instanceName = (String) nameTokenizer.nextElement();
			String instOldRevId = null;
			String instMajorId = null;

			if(instanceOldRevNameIdTable != null)
				instOldRevId = (String) instanceOldRevNameIdTable.get(instanceName);
			if(instanceMajorNameIdTable != null)
				instMajorId = (String) instanceMajorNameIdTable.get(instanceName);

			String instObjName = instanceName;
			if(!globalConf.isUniqueInstanceNameInDBOn())
				instObjName = instanceName + MCADServerSettings.INSTANCE_FAMILY_OPEN_BRACE + familyName + MCADServerSettings.INSTANCE_FAMILY_CLOSE_BRACE;
			log("instObjName = " + instObjName);

			String instanceType = getInstanceType(globalConf, familyCadType, isMajor);
			IEFXmlNodeImpl cadObjNode = (IEFXmlNodeImpl)instances.get(instanceName);
			if(!isMajor && instMajorId == null)
			{
				String majorInstanceType = getInstanceType(globalConf, familyCadType, true);
				String familyMajorRev	= familyRev.substring(0, familyRev.lastIndexOf("."));
				String familyMajorId	= (String) familyMap.get(SELECT_MAJOR_ID);
				Map familyMajorMap		= (Map) idDetailsTable.get(familyMajorId);
				String majorPolicy		= getInstancePolicy(globalConf, familyMajorMap, majorInstanceType);

				String newMajorInstId	= createInstaceObject(context, globalConf, majorInstanceType, instObjName, familyMajorRev, majorPolicy, null, familyMajorMap, cadObjNode);
				log("newMajorInstId = " + newMajorInstId);
				lockAndPromoteInstance(context, familyMajorMap, newMajorInstId, globalConf, majorPolicy);
				instanceMajorNameIdTable.put(instanceName, newMajorInstId);
			}

			String instPolicy = getInstancePolicy(globalConf, familyMap,instanceType);
			String instId = createInstaceObject(context, globalConf, instanceType, instObjName, familyRev, instPolicy, instOldRevId, familyMap, cadObjNode);
			log("instId = " + instId);
			lockAndPromoteInstance(context, actualFamilyMap, instId, globalConf, instPolicy);

			String instFormat = "";
			String instFile = "";
			addInstanceToFamilyTableCache(instanceType, instObjName, instId, instFormat, instFile, familyType, familyName, familyRev);
			
			String parentId = familyId;
			String parentInstName = (String) instParentTable.get(instanceName);
			if(parentInstName != null && instanceNameIdTable.contains(parentInstName))
				parentId = (String) instanceNameIdTable.get(parentInstName);

			connectFamilyLikeRelations(context, globalConf, idDetailsTable, instId, instMajorId, parentId);
			instanceNameIdTable.put(instanceName, instId);
		}
		connectAssemblyLikeRelations(context, globalConf, actualFamilyMap, instanceNameIdTable);
		connectPartSpecRelations(context, globalConf, actualFamilyMap, instanceNameIdTable);

		return instanceNameIdTable;
	}

	private String getInstancePolicy(MCADGlobalConfigObject globalConf, Map familyMap, String instType)
	{
		String familyPolicy = (String) familyMap.get(DomainObject.SELECT_POLICY);
		Vector policyList = globalConf.getPolicyListForType(instType);
		
		String policy = null;
		if(policyList != null && policyList.contains(familyPolicy))
			policy = familyPolicy;
		else
			policy = globalConf.getDefaultPolicyForType(instType);
		return policy;
	}

	private void addInstanceToFamilyTableCache(String instanceType, String instObjName, String instId, String instFormat, String instFile, String familyType, String familyName, String familyRev)
	{
		String familyKey = familyType + "|" + familyName + "|" + familyRev;
		Vector instWSDetails = (Vector)familyTNRInstanceDetailsTable.get(familyKey);
		if(instWSDetails == null)
		{
			instWSDetails = new Vector();
			familyTNRInstanceDetailsTable.put(familyKey, instWSDetails);
		}

		String preDirectoryStr = instanceType + "|" + instObjName + "|" + familyRev + "|" + instId + "|" + instFormat + "|";
		String postDirectoryStr = "|" + instFile + "|false|0";
		instWSDetails.addElement(preDirectoryStr);
		instWSDetails.addElement(postDirectoryStr);
	}

	private void connectPartSpecRelations(Context context, MCADGlobalConfigObject globalConf, Map familyMap, Hashtable instanceNameIdTable) throws Exception
	{
		Vector relIdList		= (Vector) familyMap.get(SELECT_PART_SPECIFICATION_ID);
		Vector relCadObjNameList= (Vector) familyMap.get(SELECT_PART_SPEC_ATTR_CAD_OBJ_NAME);
		if(relIdList != null)
		{
			for(int i=0; i<relIdList.size(); i++)
			{
				String relId		= (String) relIdList.elementAt(i);
				String relCadObjName= (String) relCadObjNameList.elementAt(i);
				String instanceId	= (String) instanceNameIdTable.get(relCadObjName);
				if(instanceId != null)
				{
					String Args[] = new String[3];
					Args[0] = relId;
					Args[1] = "to"; 
					Args[2] = instanceId;
					executeMQL(context,"modify connection $1 $2 $3",Args);
				}
			}
		}
	}

	private void connectAssemblyLikeRelations(Context context, MCADGlobalConfigObject globalConf, Map familyMap, Hashtable instanceNameIdTable) throws Exception
	{
		Hashtable integSettingMap = (Hashtable)gcoTypeDetailsMap.get(globalConf);

		Vector relNameList						= (Vector)integSettingMap.get("relName");
		Vector relDirectionList					= (Vector)integSettingMap.get("relDirection");
		Vector relIdParentSelectList			= (Vector)integSettingMap.get("relIdParentSelect");
		Vector relParentChildInstanceSelectList	= (Vector)integSettingMap.get("relParentChildInstanceSelect");
		Vector relIdChildSelectList				= (Vector)integSettingMap.get("relIdChildSelect");
		Vector relChildParentInstanceSelectList	= (Vector)integSettingMap.get("relChildParentInstanceSelect");
		for(int i=0; i<relNameList.size(); i++)
		{
			String relName		= (String)relNameList.elementAt(i);
			String relDirection = (String)relDirectionList.elementAt(i);
			String oppDirection = "from";
			if(relDirection.equals("from"))
				oppDirection = "to";

			String relIdParentSelect			= (String) relIdParentSelectList.elementAt(i);
			String relParentChildInstanceSelect	= (String) relParentChildInstanceSelectList.elementAt(i);
			Vector relIdParent					= (Vector) familyMap.get(relIdParentSelect);
			Vector relParentChildInstance		= (Vector) familyMap.get(relParentChildInstanceSelect);
			if(relIdParent != null)
			{
				for(int j=0; j<relIdParent.size(); j++)
				{
					String relId			= (String) relIdParent.elementAt(j);
					String relChildInstance = (String) relParentChildInstance.elementAt(j);
					String instanceId		= (String) instanceNameIdTable.get(relChildInstance);
					if(instanceId != null)
					{
						String Args[] = new String[4];
						Args[0] = relId;
						Args[1] = ATTR_CHILD_INSTANCE;
						Args[2] = relDirection;
						Args[3] = instanceId;
						executeMQL(context, "modify connection $1 $2 $3 $4", Args);
					}
				}
			}

			String relIdChildSelect				= (String) relIdChildSelectList.elementAt(i);
			String relChildParentInstanceSelect	= (String) relChildParentInstanceSelectList.elementAt(i);
			Vector relIdChild					= (Vector) familyMap.get(relIdChildSelect);
			Vector relChildParentInstance		= (Vector) familyMap.get(relChildParentInstanceSelect);
			if(relIdChild != null)
			{
				for(int j=0; j<relIdChild.size(); j++)
				{
					String relId				= (String) relIdChild.elementAt(j);
					String relParentInstance	= (String) relChildParentInstance.elementAt(j);
					String instanceId			= (String) instanceNameIdTable.get(relParentInstance);
					if(instanceId != null)
					{
						String Args[] = new String[4];
						Args[0] = relId;
						Args[1] = ATTR_PARENT_INSTANCE;
						Args[2] = oppDirection;
						Args[3] = instanceId;
						executeMQL(context,"modify connection $1 $2 $3 $4",Args);
					}
				}
			}
		}
	}

	private void connectFamilyLikeRelations(Context context, MCADGlobalConfigObject globalConf, Hashtable idDetailsTable, String instId, String instMajorId, String familyId) throws Exception
	{
		boolean isBulkLoadedMajor = false;
		Map familyMap = (Map) idDetailsTable.get(familyId);


		if(instMajorId != null)
		{
			String Args[] = new String[3];
			Args[0] = instId;
			Args[1] = REL_VERSION_OF;
			Args[2] = "to.id";
			String query1Result = executeMQL(context,"expand bus $1 relationship $2 select rel $3 dump",Args);
			log("query1Result======= " + query1Result);
			if(query1Result != null && query1Result.indexOf(instMajorId) > -1)
			{
				Args= new String[3];
				Args[0] = instId;
				Args[1] = REL_VERSION_OF;
				Args[2] = instMajorId;
				executeMQL(context,"connect bus $1 relationship $2 to $3",Args);
			}
			else if (null == query1Result || "".equalsIgnoreCase(query1Result))
			{
				Args= new String[3];
				Args[0] = instId;
				Args[1] = REL_VERSION_OF;
				Args[2] = instMajorId;
				executeMQL(context,"connect bus $1 relationship $2 to $3",Args);
			}

			String majorIdFromActiveRel = (String) familyMap.get(SELECT_MAJOR_ACTIVE_REL);
			if(majorIdFromActiveRel != null)
			{
				Args= new String[3];
				Args[0] = instMajorId;
				Args[1] = REL_ACTIVE_VERSION;
				Args[2] = instId;
				executeMQL(context,"connect bus $1 relationship $2 to $3 ", Args);
			}

			String majorIdFromLatestRel = (String) familyMap.get(SELECT_MAJOR_LATEST_REL);
			if(majorIdFromLatestRel != null)
			{
				Args= new String[3];
				Args[0] = instMajorId;
				Args[1] = REL_LATEST_VERSION;
				Args[2] = instId;
				executeMQL(context,"connect bus $1 relationship $2 to $3", Args);
			}

			String majorIdFromFinalizedRel = (String) familyMap.get(SELECT_MAJOR_FINALIZED_REL);
			if(majorIdFromFinalizedRel != null)
			{
				Args= new String[3];
				Args[0] = instMajorId;
				Args[1] = REL_FINALIZED;
				Args[2] = instId;
				executeMQL(context,"connect bus $1 relationship $2 from $3", Args);
			}
		}
		else
		{
			Vector minorList = (Vector) familyMap.get(SELECT_MINOR_ID_LIST);
			if(minorList == null || minorList.size() == 0)
				isBulkLoadedMajor = true;
		}

		String finalizedMinorId	= (String) familyMap.get(SELECT_FINALIZED_MINOR_ID);
		if(instMajorId != null || finalizedMinorId != null || isBulkLoadedMajor)
		{
			Hashtable integSettingMap = (Hashtable)gcoTypeDetailsMap.get(globalConf);
			String relInstanceOf = (String)integSettingMap.get("rel|InstanceOf|Name");
			String relInstanceOfDir = (String)integSettingMap.get("rel|InstanceOf|Direction");
			String Args[] = new String[4];
			Args[0] = familyId;
			Args[1] = relInstanceOf;
			Args[2] = relInstanceOfDir;
			Args[3] = instId;
			executeMQL(context,"connect bus $1 relationship $2 $3 $4", Args);
		}
	}

	private void changeFamilyType(Context context, MCADGlobalConfigObject globalConf, String familyId, String familyCadType, String familyType, boolean isMajor, String integrationName) throws Exception
	{
		String typeName = familyType;
		Vector typeList = null;
		Hashtable integSettingMap = (Hashtable)gcoTypeDetailsMap.get(globalConf);

		log("familyCadType = " + familyCadType + "; familyType = " + familyType);
		if(globalConf.isTypeOfClass(familyCadType, "TYPE_ASSEMBLY_FAMILY_LIKE"))
			typeList = (Vector)integSettingMap.get("type|assemblyFamily|" + isMajor);
		else if(globalConf.isTypeOfClass(familyCadType, "TYPE_COMPONENT_FAMILY_LIKE"))
			typeList = (Vector)integSettingMap.get("type|componentFamily|" + isMajor);

		if(!typeList.contains(familyType))
			typeName = (String) typeList.elementAt(0);

		String Args[] = new String[8];
		Args[0] = familyId;
		Args[1] = "type";
		Args[2] = typeName;
		Args[3] = ATTR_INSTANCE_NAMES;
		Args[4] = ATTR_INSTANCE_STRUCTURE;
		Args[5] = ATTR_INSTANCES;
		Args[6] = ATTR_INSTANCE_ATTR;
		Args[7] = ATTR_INSTANCE_SPECIFIC_ATTR;
		executeMQL(context,"modify bus $1 $2 $3 $4 $5 $6 $7 $8", Args);
	}

	private void renameFilesForProE(Context context) throws Exception
	{
		MCADGlobalConfigObject globalConf = getGlobalConfigObject(context, "MxPRO");
		if(globalConf != null)
		{
			log("MxPro is installed. Migrating file names");
			Vector cadTypeList = globalConf.getTypeListForClass("TYPE_CADMODEL_LIKE");
			for(int i=0; i<cadTypeList.size(); i++)
			{
				String cadType = (String) cadTypeList.elementAt(i);
				Vector typeList = globalConf.getMxTypeListForCADTypeFromFormatMapping(cadType);
				Vector templateTypes = globalConf.getTemplateTypesForCADType(cadType);
				for(int j=0; j<typeList.size(); j++)
				{
					String mxType	= (String) typeList.elementAt(j);
					if(templateTypes != null && templateTypes.contains(mxType))
						continue;

					String format	= globalConf.getFormatsForType(mxType, cadType);
					String minorType= mxUtil.getCorrespondingType(context, mxType);

					String Args[] = new String[5];
					Args[0] = mxType+","+minorType;
					Args[1] = "*";
					Args[2] = "*";
					Args[3] = "format[" + format + "].file.name";
					Args[4] =  "|";	

					String mxProObjects = executeMQL(context, "temp query bus $1 $2 $3 select $4 dump $5", Args);
					StringTokenizer rowToken = new StringTokenizer(mxProObjects, "\n");
					while(rowToken.hasMoreElements())
					{
						String mxProRow = (String) rowToken.nextElement();
						StringTokenizer colToken = new StringTokenizer(mxProRow, "|");
						if(colToken.hasMoreElements())
						{
							String type = (String) colToken.nextElement();
							String name = (String) colToken.nextElement();
							String rev  = (String) colToken.nextElement();
							while(colToken.hasMoreElements())
							{
								String fileName = (String) colToken.nextElement();
								String changedFileName = truncateNumeralExtension(fileName);
								if(!fileName.equals(changedFileName))
								{
									renameFile(context, type, name, rev, format, fileName, changedFileName);
									resetFileRelatedAttributes(context, type, name, rev, fileName, changedFileName);
								}
							}
						}
					}
				}
			}
		}
	}

	private void resetFileRelatedAttributes(Context context, String type, String name, String rev, String fileName, String changedFileName) throws Exception
	{
		String attrTitle 				= MCADMxUtil.getActualNameForAEFData(context, "attribute_Title");
		String attrIEFFileMessageDigest = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-FileMessageDigest");
		

		BusinessObject busObject = new BusinessObject(type,name, rev, "");
		String         id        = busObject.getObjectId(context);
		
		String SELECT_TITLE               = new StringBuffer("attribute[").append(attrTitle).append("]").toString();
		String SELECT_FILE_MESSAGE_DIGEST = new StringBuffer("attribute[").append(attrIEFFileMessageDigest).append("]").toString();
		
		StringList  selectAttrList        = new StringList();
		selectAttrList.add(SELECT_TITLE);
		selectAttrList.add(SELECT_FILE_MESSAGE_DIGEST);
		
		BusinessObjectWithSelectList busWithSelectList  = BusinessObject.getSelectBusinessObjectData(context, new String[] {id}, selectAttrList);
		BusinessObjectWithSelect     busWithSelect      = busWithSelectList.getElement(0);

		String title    = busWithSelect.getSelectData(SELECT_TITLE);
		String hashcode  = busWithSelect.getSelectData(SELECT_FILE_MESSAGE_DIGEST);

		if(title != null)
			title = mxUtil.replace(title, fileName, changedFileName);
		
		if(hashcode != null)
				hashcode = mxUtil.replace(hashcode, fileName, changedFileName);

		
		
		AttributeList attributelist = new AttributeList(2);
		attributelist.addElement(new Attribute(new AttributeType(attrTitle), title));
		attributelist.addElement(new Attribute(new AttributeType(attrIEFFileMessageDigest), hashcode));
        busObject.open(context,false);
		busObject.setAttributes(context, attributelist);
        busObject.close(context);
		
	}

	private void renameFile(Context context, String type, String name, String rev, String format, String fileName, String changedFileName) throws Exception
	{
		BusinessObject busObject = new BusinessObject(type, name, rev, "");
		log("Bus Object: '" + type + "' '" + name + "' " + rev);
		if(isAEFVersion106(context))
		{
			
			String Args[] = new String[10];
			Args[0] = type;
			Args[1] = name;
			Args[2] = rev;
			Args[3] = "rename";
			Args[4] = "format";
			Args[5] = format;
			Args[6] = "!propagaterename";
			Args[7] = "file";
			Args[8] = fileName;
			Args[9] = changedFileName;
			executeMQL(context,"modify bus $1 $2 $3 $4 $5 $6 $7 $8 $9 $10", Args);
		}
		else
		{
			busObject.checkoutFile(context, false, format, fileName, documentDirectory);
			java.io.File fileToRename	= new java.io.File(documentDirectory  + java.io.File.separator + fileName);
			java.io.File newFilePath	= new java.io.File(documentDirectory  + java.io.File.separator + changedFileName);

			boolean isRenamed = fileToRename.renameTo(newFilePath);
			if(isRenamed)
			{
				busObject.checkinFile(context, false, false, "localhost", format, changedFileName, documentDirectory);
				newFilePath.delete();
			}
			else
			{
				log("We have encountered an error while rename the file :" + fileName);
				fileToRename.delete();
			}
		}
		log("Filename changed from " + fileName + " to " + changedFileName);
	}

	private boolean isAEFVersion106(Context context) throws Exception
	{
		if(coreVersion == null)
		{
			String Args[] = new String[1];
			Args[0] = "version";
			coreVersion = executeMQL(context,"$1", Args);
		}

		if(coreVersion.startsWith("10.6"))
			return true;
		else
			return false;
	}

	private Hashtable getInstanceStructure(IEFXmlNodeImpl instanceStructure, Hashtable instances)
	{
		Hashtable instParentTable = new Hashtable();
		Hashtable cadIdNameTable = new Hashtable();
		log("instanceStructure -> " + instanceStructure.getXmlString());
		Enumeration instKeys = instances.keys();
		while(instKeys.hasMoreElements())
		{
			String instName = (String)instKeys.nextElement();
			IEFXmlNodeImpl instNode = (IEFXmlNodeImpl) instances.get(instName);
			String cadId = instNode.getAttribute("cadid");
			cadIdNameTable.put(cadId, instName);
		}
		addChildrenForParent(instanceStructure, null, instParentTable, cadIdNameTable);
		return instParentTable;
	}

	private void addChildrenForParent(IEFXmlNodeImpl instanceStructure, String parentName, Hashtable instParentTable, Hashtable cadIdNameTable)
	{
		if(instanceStructure.getChildCount()>0)
		{
			Enumeration childs = instanceStructure.elements();
			while(childs.hasMoreElements())
			{
				IEFXmlNodeImpl child = (IEFXmlNodeImpl)childs.nextElement();
				String childCadId = child.getAttribute("cadid");
				String instName = (String) cadIdNameTable.get(childCadId);
				if(parentName != null)
					instParentTable.put(instName, parentName);
				addChildrenForParent(child, instName, instParentTable, cadIdNameTable);
			}
		}
	}

	private String createInstaceObject(Context context, MCADGlobalConfigObject globalConf, String instType, String instName, String instRev, String policy, String instOldRevId, Map familyMap, IEFXmlNodeImpl cadObjNode) throws Exception
	{
		String tnrStr = "'" + instType + "' '" + instName + "' '" + instRev + "'";
		log("Creating instance object: " + tnrStr);
		String cmd = "";
		if(instOldRevId == null)
		{
			String vault = (String) familyMap.get(DomainObject.SELECT_VAULT);
			String Args[] = new String[5];
			Args[0] = instType;
			Args[1] = instName;
			Args[2] = instRev;
			Args[3] = policy;
			Args[4] = vault;			
			executeMQL(context,"add bus $1 $2 $3 policy $4 vault $5", Args);
		}
		else
		{
			String Args[] = new String[2];
			Args[0] = instOldRevId;
			Args[1] = instRev;			
			executeMQL(context,"revise bus $1 to $2", Args);
		}

		String Args[] = new String[4];
		Args[0] = instType;
		Args[1] = instName;
		Args[2] = instRev;
		Args[3] = "id";	
		String instId = executeMQL(context,"print bus $1 $2 $3 select $4 dump", Args).trim();

		Hashtable attrNameValTable = new Hashtable();
		Hashtable integSettingMap = (Hashtable)gcoTypeDetailsMap.get(globalConf);

		String familyCadType = (String) familyMap.get(SELECT_ATTR_CADTYPE);
		String cadType = null;
		if(globalConf.isTypeOfClass(familyCadType, "TYPE_ASSEMBLY_FAMILY_LIKE"))
			cadType = (String)integSettingMap.get("cadType|assemblyInstance");
		else
			cadType = (String)integSettingMap.get("cadType|componentInstance");

		String owner = (String) familyMap.get(DomainObject.SELECT_OWNER);
		attrNameValTable.put("owner", owner);
		attrNameValTable.put("Originator", owner);
		attrNameValTable.put(ATTR_CAD_TYPE, cadType);
		attrNameValTable.put(ATTR_SOURCE, familyMap.get(SELECT_ATTR_SOURCE));
		setInstanceSpecificAttrTable(cadObjNode, attrNameValTable);

	    cmd = "modify bus " + instId + getAttrModifyCommand(attrNameValTable);
		executeMQL(context,cmd);

		return instId;
	}

	private void lockAndPromoteInstance(Context context, Map familyMap, String instId, MCADGlobalConfigObject globalConf, String policy)throws Exception
	{
		String owner		= (String)familyMap.get(DomainObject.SELECT_OWNER);
		String locker		= (String)familyMap.get(DomainObject.SELECT_LOCKER);
		String familyPolicy = (String)familyMap.get(DomainObject.SELECT_POLICY);
		String currState	= (String)familyMap.get(DomainObject.SELECT_CURRENT);

		lockInstance(context, instId, locker);
		if(familyPolicy.equals(policy))
			promoteInstance(context, instId, policy, currState, owner);
	}

	private void lockInstance(Context context, String instId, String locker)throws Exception
	{
		if(locker != null && !locker.equals(""))
		{
			log("Locking " + instId + " using user '" + locker + "'");
			String Args[] = new String[1];
			Args[0] = locker;
			executeMQL(context,"push context user $1", Args);
			Args[0]= instId;
			executeMQL(context,"lock bus $1", Args);
			Args[0] = "context";
			executeMQL(context,"pop $1", Args);
		}
	}

	private void promoteInstance(Context context, String instId, String instPolicy, String currState, String owner) throws Exception
	{
		log("Promoting " + instId + " to '" + currState + "' state");
		BusinessObject inBus = new BusinessObject(instId);

		StateList busStateList = inBus.getStates(context);
		StateItr busStateItr = new StateItr(busStateList);
		while(busStateItr.next())
		{
			State busState = busStateItr.obj();
			if(currState.equalsIgnoreCase(busState.getName()))
				break;
			
			// Promote the Instance object
			String Args[] = new String[1];
			Args[0] = instId;
			executeMQL(context,"override bus $1", Args);
			executeMQL(context,"promote bus $1", Args);
		}
	}


	private void setInstanceSpecificAttrTable(IEFXmlNodeImpl cadObjNode, Hashtable attrNameValTable)
	{
		log("cadObjNode -> " + cadObjNode.getXmlString());
        Enumeration childElements = cadObjNode.elements();
        while(childElements.hasMoreElements())
        {
            IEFXmlNode childNode = (IEFXmlNode)childElements.nextElement();
            if(childNode.getName().equals("attribute"))
            {
				String attrName		= childNode.getChildByName("name").getFirstChild().getContent();
				String attrValue	= childNode.getChildByName("value").getFirstChild().getContent();
				attrNameValTable.put(attrName, attrValue);
            }
        }
	}

	private String getAttrModifyCommand(Hashtable attrNameValTable)
	{
		StringBuffer modifyCmd = new StringBuffer(" ");
		Enumeration attrNameEnum = attrNameValTable.keys();
		while(attrNameEnum.hasMoreElements())
		{
			String attrName		= (String) attrNameEnum.nextElement();
			String attrValue	= (String) attrNameValTable.get(attrName);
			modifyCmd.append("'");
			modifyCmd.append(attrName);
			modifyCmd.append("'");
			modifyCmd.append(" '");
			modifyCmd.append(attrValue);
			modifyCmd.append("' ");
		}
		return modifyCmd.toString();
	}

	private Hashtable getFamilyTower(String id, MCADGlobalConfigObject globalConf, Hashtable idDetailsTable) throws Exception
	{
		log("Getting family Tower for : '" + id + "'");
		Hashtable familyTower = new Hashtable();

		Map majorMap = (Map) idDetailsTable.get(id);
		String type = (String) majorMap.get(DomainObject.SELECT_TYPE);
		String name = (String) majorMap.get(DomainObject.SELECT_NAME);

		Vector revisionIDs = getRevisionIds(majorMap);
		log("majors = " + revisionIDs);

		for(int i=0; i<revisionIDs.size(); i++)
		{
			String revId = (String)revisionIDs.elementAt(i);
			Map revMap = (Map) idDetailsTable.get(revId);
			Vector versionIDs = (Vector) revMap.get(SELECT_MINOR_ID_LIST);
			if(versionIDs == null)
				versionIDs = new Vector();

			if(versionIDs.size() > 0)
			{
				Map versionMap = (Map) idDetailsTable.get(versionIDs.elementAt(0));
				versionIDs = getRevisionIds(versionMap);
			}

			handledObjIDs.addAll(versionIDs);
			familyTower.put(revId, versionIDs);
		}

		familyTower.put("MajorRevList", revisionIDs);
		handledObjIDs.addAll(revisionIDs);
		log("Family tower extracted....");
		return familyTower;
    }

	private Vector getRevisionIds(Map map) throws Exception
	{
		Vector revisionIDs = new Vector();
		Vector revisions = (Vector) map.get(SELECT_REVISION_LIST);
		for(int i=0; i<revisions.size(); i++)
		{
			revisionIDs.addElement(map.get("revisions["+revisions.elementAt(i)+"].id"));
		}
		return revisionIDs;
	}

	private String getInstanceType(MCADGlobalConfigObject globalConf, String familyCadType, boolean isMajor) throws Exception
	{
		String typeName = null;
		Hashtable integSettingMap = (Hashtable)gcoTypeDetailsMap.get(globalConf);
		log("familyCadType = " + familyCadType + "; isMajor = " + isMajor);

		if(globalConf.isTypeOfClass(familyCadType, "TYPE_ASSEMBLY_FAMILY_LIKE"))
			typeName = (String)integSettingMap.get("type|assemblyInstance|" + isMajor);
		else if(globalConf.isTypeOfClass(familyCadType, "TYPE_COMPONENT_FAMILY_LIKE"))
			typeName = (String)integSettingMap.get("type|componentInstance|" + isMajor);

		if(typeName == null)
			throw new Exception("Mapping not found in GCO for Instance types");

		return typeName;
	}

	private Object convertToObject(String serializedObj) throws Exception
	{
		if(serializedObj != null)
			return MCADUtil.covertToObject(serializedObj,true);
		else
			return null;
	}

	private MCADGlobalConfigObject getGlobalConfigObject(Context context, String integrationName) throws Exception
	{
		MCADGlobalConfigObject gco = (MCADGlobalConfigObject) integNameGCOMap.get(integrationName);
		if(gco == null)
		{
			log("Reading GCO Integration: '" + integrationName + "'");
			IEFGetRegistrationDetails_mxJPO registrationDetailsReader = new IEFGetRegistrationDetails_mxJPO(context, null);
			String args[] = new String[1];
			args[0] = integrationName;
			String registrationDetails = registrationDetailsReader.getRegistrationDetails(context, args);

			if(registrationDetails != null && !"".equals(registrationDetails.trim()))
			{
				String gcoName = registrationDetails.substring(registrationDetails.lastIndexOf("|")+1);
				log("gcoName = " + gcoName);

				MCADConfigObjectLoader configLoader = new MCADConfigObjectLoader(null);
				gco = configLoader.createGlobalConfigObject(context, mxUtil, MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig"), gcoName);
				integNameGCOMap.put(integrationName, gco);
			}
		}

		return gco;
	}

	private void modifyWorkingSetFile(Context context, String lcoID, String store, String tempDir) throws Exception
	{
		log("Working set file processing started for LCO ID: " + lcoID);
		log("Temp Dir === " + tempDir);
		StringBuffer modifiedWSEntry = new StringBuffer("");
		BusinessObject lcoObj = new BusinessObject(lcoID);
		lcoObj.checkoutFile(context, false, "generic", "DefaultWorkingSet", tempDir);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tempDir + java.io.File.separator + "DefaultWorkingSet")));
		String line = null;
		while((line = br.readLine()) != null)
		{
			StringTokenizer tokenizer = new StringTokenizer(line , "|");
			if(tokenizer.countTokens() > 3)
			{
				String type			= (String) tokenizer.nextElement();
				String name			= (String) tokenizer.nextElement();
				String rev			= (String) tokenizer.nextElement();
				String id			= (String) tokenizer.nextElement();
				String format		= (String) tokenizer.nextElement();
				String origDirectory= (String) tokenizer.nextElement();
				String origFileName	= (String) tokenizer.nextElement();
				String lock			= "false";
				String refCount		= "0";

				if(tokenizer.hasMoreElements())
					lock = (String) tokenizer.nextElement();
				if(tokenizer.hasMoreElements())
					refCount = (String) tokenizer.nextElement();

				String fileName		= truncateNumeralExtension(origFileName);
				String directory	= changeDirectoryNameCase(origDirectory);

				if(familyTNRInstanceDetailsTable.containsKey(type+"|"+name+"|"+rev))
				{
					log("Family Found: " + type+"|"+name+"|"+rev);					
					Vector instanceWSDetails = (Vector)familyTNRInstanceDetailsTable.get(type+"|"+name+"|"+rev);
					for(int i=0; i<instanceWSDetails.size(); i+=2)
					{
						String preDirectoryStr = (String)instanceWSDetails.elementAt(i);
						String postDirectoryStr = (String)instanceWSDetails.elementAt(i+1);

						modifiedWSEntry.append(preDirectoryStr).append(directory)
										.append(postDirectoryStr).append("\n");
					}
				}

				if(fileName.equals(origFileName) && directory.equals(origDirectory))
					modifiedWSEntry.append(line).append("\n");
				else
					modifiedWSEntry.append(type).append("|")
									.append(name).append("|")
									.append(rev).append("|")
									.append(id).append("|")
									.append(format).append("|")
									.append(directory).append("|")
									.append(fileName).append("|")
									.append(lock).append("|")
									.append(refCount).append("\n");
			}
			else
			{
				modifiedWSEntry.append(line).append("\n");
			}
		}
		br.close();

		log("File update started");
		FileWriter wsWriter	= new FileWriter(documentDirectory + "DefaultWorkingSet");
		wsWriter.write(modifiedWSEntry.toString());
		wsWriter.flush();
		wsWriter.close();

		log("File checkin started");
		lcoObj.checkinFile(context, false, false, "localhost", "generic", store, "DefaultWorkingSet", tempDir);
		log("Working set file processing completed\n");
		log("*******************************************");
	}

	private String changeDirectoryNameCase(String origDirectory)
	{
		String directory = origDirectory;
		if(!origDirectory.startsWith("/"))
		{
			java.io.File dirFile = new java.io.File(origDirectory);
			directory = dirFile.getPath().toLowerCase();
			if(directory.length() == 2)
			{
				directory += MCADAppletServletProtocol.WIN_FILE_SEPARATOR;
			}
			else if(directory.endsWith(MCADAppletServletProtocol.WIN_FILE_SEPARATOR) && directory.length() > 3)
			{
				directory = directory.substring(0, directory.length()-1);
			}
		}
		return directory;
	}

	private String truncateNumeralExtension(String origFileName)
	{
		String fileName = origFileName;

		int lastIndex = origFileName.lastIndexOf(".");
		String extension = null;
		if(lastIndex > 0)
			extension = origFileName.substring(lastIndex+1);

		if (extension != null)
		{
			try
			{
				int number = Integer.parseInt(extension);
				fileName = origFileName.substring(0, lastIndex);
				log("File name chenged from '" + origFileName + "' to '" + fileName + "'");
			}
			catch(NumberFormatException nfe)
			{
			}
		}

		return fileName;
	}

	private String executeMQL(Context context,String cmd, String args[]) throws Exception
	{
		log("Executing MQL Command: " + cmd);
		MQLCommand mqlCommand = new MQLCommand();
		boolean bRet = mqlCommand.executeCommand(context, cmd, args);
		log("Executing MQL Command mqlCommand.getResult(): " + mqlCommand.getResult());
		if (bRet)
			return mqlCommand.getResult();
		else
			throw new Exception(mqlCommand.getError());
	}
	private String executeMQL(Context context,String cmd) throws Exception
	{
		log("Executing MQL Command: " + cmd);
		MQLCommand mqlCommand = new MQLCommand();
		boolean bRet = mqlCommand.executeCommand(context, cmd);
		log("Executing MQL Command mqlCommand.getResult(): " + mqlCommand.getResult());
		if (bRet)
			return mqlCommand.getResult();
		else
			throw new Exception(mqlCommand.getError());
	}

	private Hashtable getDetailsTable(MapList mapList)
	{
		Hashtable idDetailsTable = new Hashtable();
		Iterator itr = mapList.iterator();
		while( itr.hasNext())
		{
			Map map = (Map) itr.next();
			String id = (String) map.get(DomainObject.SELECT_ID);
			idDetailsTable.put(id, map);
		}
		return idDetailsTable;
	}

	private Hashtable readIDsFile(String fileName) throws Exception
	{
        Hashtable objectListIntegNameTable = new Hashtable();
		writer.write("Reading ID file: " + fileName + "\n");

		String fileNameWithPath		= documentDirectory + fileName;
		BufferedReader iefIDsReader = new BufferedReader(new FileReader(fileNameWithPath));

        String objectIdLine = "";
		while((objectIdLine = iefIDsReader.readLine()) != null)
		{
			StringList familyTableIDs = new StringList();
			StringTokenizer tokenizer = new StringTokenizer(objectIdLine, "|");
			if(!tokenizer.hasMoreElements())
				continue;

			String type			= (String) tokenizer.nextElement();
			String name			= (String) tokenizer.nextElement();
			String revision		= (String) tokenizer.nextElement();
			String integName	= (String) tokenizer.nextElement();
			String integVersion = (String) tokenizer.nextElement();
			while(tokenizer.hasMoreElements())
			{
				familyTableIDs.addElement(tokenizer.nextElement());
			}
			objectListIntegNameTable.put(familyTableIDs, integName);
		}

        return objectListIntegNameTable;
	}

	private void readWSDataFile() throws Exception
	{
        familyTNRInstanceDetailsTable = new Hashtable();
		writer.write("Reading Working Set Data file: " + wsDataFileName + "\n");

		String fileNameWithPath		= documentDirectory + wsDataFileName;
		BufferedReader iefWSDataReader = new BufferedReader(new FileReader(fileNameWithPath));

		String lastKey = null;
		Vector instData = null;
        String wsDataLine = "";
		while((wsDataLine = iefWSDataReader.readLine()) != null)
		{
			if(wsDataLine.startsWith("KEY:"))
			{
				if(lastKey != null)
					familyTNRInstanceDetailsTable.put(lastKey, instData);

				lastKey = wsDataLine.substring(4);
				instData = new Vector();
			}
			else
			{
				instData.addElement(wsDataLine);
			}
		}
	}

	private void writeWSData()
	{
		try
		{
			Enumeration familyKeys = familyTNRInstanceDetailsTable.keys();
			while(familyKeys.hasMoreElements())
			{
				String familyKey		= (String)familyKeys.nextElement();
				wsDataFile.write("KEY:" + familyKey + "\n");

				Vector instWSDetails	= (Vector)familyTNRInstanceDetailsTable.get(familyKey);
				for(int i=0; i<instWSDetails.size(); i++)
				{
					String oneInstDetail = (String)instWSDetails.elementAt(i);
					wsDataFile.write(oneInstDetail + "\n");
				}
			}

			wsDataFile.flush();
		}
		catch(Exception e)
		{
		}
	}

	private void closeWSDataFile()
	{
		try
		{
			wsDataFile.flush();
			wsDataFile.close();
		}
		catch(Exception e)
		{
		}
	}

	private void startWSDataFile() throws Exception
	{
		try
		{
			wsDataFile	= new FileWriter(documentDirectory + wsDataFileName);
		}
		catch(Exception e)
		{
			writeMessageToConsole("ERROR: Can not create log file. " + e.getMessage());
		}
	}

	private void validateInputArguments(String[] args) throws Exception
	{
		if (args.length < 2)
			throw new IllegalArgumentException("Wrong number of arguments");

		documentDirectory	= args[0];
		migrationComponent	= args[1];

		if(migrationComponent.equalsIgnoreCase("Object"))
		{
			if (args.length < 4)
				throw new IllegalArgumentException("Wrong number of arguments");
			migrationIDsFileName	= args[2];
			wsDataFileName			= args[3];
		}
		else if(migrationComponent.equalsIgnoreCase("WorkingSet"))
		{
			if (args.length < 3)
				throw new IllegalArgumentException("Wrong number of arguments");
			wsDataFileName = args[2];
		}

		String fileSeparator = java.io.File.separator;
		if(documentDirectory != null && !documentDirectory.endsWith(fileSeparator))
			documentDirectory = documentDirectory + fileSeparator;
	}

	private StringList getObjectSelectElements(Context context, MCADGlobalConfigObject globalConf) throws Exception
	{
		StringList objectSelectElements = (StringList)gcoObjSelectMap.get(globalConf);
		if(objectSelectElements == null)
		{
			Hashtable typeDetailsMap = new Hashtable();
			objectSelectElements = getStandardObjectSelectElements();

			Vector instanceCadTypes = globalConf.getTypeListForClass("TYPE_INSTANCE_LIKE");
			Vector assemblyCadTypes = globalConf.getTypeListForClass("TYPE_ASSEMBLY_LIKE");
			Vector componentCadTypes = globalConf.getTypeListForClass("TYPE_COMPONENT_LIKE");

			for(int i=0; i<instanceCadTypes.size(); i++)
			{
				String cadType = (String) instanceCadTypes.elementAt(i);
				if(assemblyCadTypes.contains(cadType))
				{
					String assemblyInstType        = (String)globalConf.getMappedBusTypes(cadType).elementAt(0);
					String assemblyInstVersionType = mxUtil.getCorrespondingType(context, assemblyInstType);
					typeDetailsMap.put("cadType|assemblyInstance", cadType);
					typeDetailsMap.put("type|assemblyInstance|true", assemblyInstType);
					typeDetailsMap.put("type|assemblyInstance|false", assemblyInstVersionType);
				}
				else if(componentCadTypes.contains(cadType))
				{
					String componentInstType		= (String)globalConf.getMappedBusTypes(cadType).elementAt(0);
					String componentInstVersionType = mxUtil.getCorrespondingType(context, componentInstType);
					typeDetailsMap.put("cadType|componentInstance", cadType);
					typeDetailsMap.put("type|componentInstance|true", componentInstType);
					typeDetailsMap.put("type|componentInstance|false", componentInstVersionType);
				}
			}

			Vector asmFamilyCadTypes = globalConf.getTypeListForClass("TYPE_ASSEMBLY_FAMILY_LIKE");
			if(asmFamilyCadTypes != null && asmFamilyCadTypes.size() > 0)
			{
				Vector assemblyFamilyTypes = new Vector();
				Vector assemblyFamilyVersionTypes = new Vector();
				setFamilyTypeClassList(context, globalConf, asmFamilyCadTypes, assemblyFamilyTypes, assemblyFamilyVersionTypes);
				log("assemblyFamilyTypes = " + assemblyFamilyTypes);

				typeDetailsMap.put("type|assemblyFamily|true", assemblyFamilyTypes);
				typeDetailsMap.put("type|assemblyFamily|false", assemblyFamilyVersionTypes);
			}

			Vector compFamilyCadTypes = globalConf.getTypeListForClass("TYPE_COMPONENT_FAMILY_LIKE");
			if(compFamilyCadTypes != null && compFamilyCadTypes.size() > 0)
			{
				Vector componentFamilyTypes = new Vector();
				Vector componentFamilyVersionTypes = new Vector();
				setFamilyTypeClassList(context, globalConf, compFamilyCadTypes, componentFamilyTypes, componentFamilyVersionTypes);
				log("componentFamilyTypes = " + componentFamilyTypes);

				typeDetailsMap.put("type|componentFamily|true", componentFamilyTypes);
				typeDetailsMap.put("type|componentFamily|false", componentFamilyVersionTypes);
			}

			populateRelationDetails(typeDetailsMap, objectSelectElements, globalConf);
			gcoTypeDetailsMap.put(globalConf, typeDetailsMap);
			gcoObjSelectMap.put(globalConf, objectSelectElements);
		}

		return objectSelectElements;
	}

	private void setFamilyTypeClassList(Context context, MCADGlobalConfigObject globalConf, Vector familyCadTypes, Vector majorTypes, Vector minorTypes)
	{
		for(int i=0; i<familyCadTypes.size(); i++)
		{
			String cadType = (String) familyCadTypes.elementAt(i);
			Vector majorMxTypesList = globalConf.getMappedBusTypes(cadType);
			for(int j=0; j<majorMxTypesList.size(); j++)
			{
				String majorMxType = (String) majorMxTypesList.elementAt(j);
				String minorMxType = mxUtil.getCorrespondingType(context, majorMxType);
				majorTypes.addElement(majorMxType);
				minorTypes.addElement(minorMxType);
			}
		}
	}

	private StringList getStandardObjectSelectElements() throws Exception
	{
		StringList objectSelectElements = new StringList();

		objectSelectElements.add(DomainObject.SELECT_TYPE);
		objectSelectElements.add(DomainObject.SELECT_NAME);
		objectSelectElements.add(DomainObject.SELECT_REVISION);
		objectSelectElements.add(DomainObject.SELECT_VAULT);
		objectSelectElements.add(DomainObject.SELECT_POLICY);
		objectSelectElements.add(DomainObject.SELECT_OWNER);
		objectSelectElements.add(DomainObject.SELECT_ID);
		objectSelectElements.add(DomainObject.SELECT_LOCKED);
		objectSelectElements.add(DomainObject.SELECT_CURRENT);
		objectSelectElements.add(DomainObject.SELECT_LOCKER);
        objectSelectElements.add(DomainObject.SELECT_FILE_FORMAT);
        objectSelectElements.add(DomainObject.SELECT_FILE_NAME);
        objectSelectElements.add(DomainObject.SELECT_VAULT);
		objectSelectElements.add(SELECT_REVISION_LIST);
		objectSelectElements.add(SELECT_REVISION_IDS);
		objectSelectElements.add(SELECT_ATTR_CADTYPE);
		objectSelectElements.add(SELECT_ATTR_SOURCE);
		objectSelectElements.add(SELECT_MAJOR_ACTIVE_REL);
		objectSelectElements.add(SELECT_MAJOR_LATEST_REL);
		objectSelectElements.add(SELECT_MAJOR_ID);
		objectSelectElements.add(SELECT_MINOR_ID_LIST);
		objectSelectElements.add(SELECT_FINALIZED_MINOR_ID);
		objectSelectElements.add(SELECT_MAJOR_FINALIZED_REL);
		objectSelectElements.add(SELECT_ATTR_REL_IS_FINALIZED_FOR_MAJOR);
		objectSelectElements.add(SELECT_ATTR_INSTANCE_NAMES);
		objectSelectElements.add(SELECT_PART_SPECIFICATION_ID);
		objectSelectElements.add(SELECT_PART_SPEC_ATTR_CAD_OBJ_NAME);
		objectSelectElements.add(SELECT_ATTR_INSTANCE_STRUCTURE);
		objectSelectElements.add(SELECT_ATTR_INSTANCES);
		objectSelectElements.add(SELECT_ATTR_INSTANCE_ATTR);
		objectSelectElements.add(SELECT_ATTR_INSTANCE_SPECIFIC_ATTR);

		return objectSelectElements;
	}

	private void populateRelationDetails(Hashtable typeDetailsMap, StringList objectSelectElements, MCADGlobalConfigObject globalConf)
	{
		Hashtable relsAndEnds = globalConf.getRelationshipsOfClass("FamilyLike");
		Enumeration relsEnum = relsAndEnds.keys();
		if(relsEnum.hasMoreElements())
		{
			String rel = (String) relsEnum.nextElement();
			String end = (String) relsAndEnds.get(rel);
			typeDetailsMap.put("rel|InstanceOf|Name", rel);
			typeDetailsMap.put("rel|InstanceOf|Direction", end);
		}

		Vector relName						= new Vector();
		Vector relDirection					= new Vector();
		Vector relIdParentSelect			= new Vector();
		Vector relParentChildInstanceSelect	= new Vector();
		Vector relIdChildSelect				= new Vector();
		Vector relChildParentInstanceSelect	= new Vector();

		relsAndEnds = globalConf.getRelationshipsOfClass("AssemblyLike");
		relsAndEnds.putAll(globalConf.getRelationshipsOfClass("ExternalRefereneLike"));
		relsAndEnds.putAll(globalConf.getRelationshipsOfClass("DerivedOutputLike"));
		//log("relsAndEnds -> " + relsAndEnds);

		relsEnum = relsAndEnds.keys();
		while(relsEnum.hasMoreElements())
		{
			String rel = (String) relsEnum.nextElement();
			String end = (String) relsAndEnds.get(rel);
			String oppEnd = "from";
			if(end.equals("from"))
				oppEnd = "to";

			String relIdParent 				= end + "[" + rel + "].id";
			String relParentChildInstance 	= end + "[" + rel + "].attribute[" + ATTR_CHILD_INSTANCE + "].value";
			String relIdChild 				= oppEnd + "[" + rel + "].id";
			String relChildParentInstance 	= oppEnd + "[" + rel + "].attribute[" + ATTR_PARENT_INSTANCE + "].value";
			
			objectSelectElements.addElement(relIdParent);
			objectSelectElements.addElement(relParentChildInstance);
			objectSelectElements.addElement(relIdChild);
			objectSelectElements.addElement(relChildParentInstance);
			DomainObject.MULTI_VALUE_LIST.add(relIdParent);
			DomainObject.MULTI_VALUE_LIST.add(relParentChildInstance);
			DomainObject.MULTI_VALUE_LIST.add(relIdChild);
			DomainObject.MULTI_VALUE_LIST.add(relChildParentInstance);

			relName.addElement(rel);
			relDirection.addElement(end);
			relIdParentSelect.addElement(relIdParent);
			relParentChildInstanceSelect.addElement(relParentChildInstance);
			relIdChildSelect.addElement(relIdChild);
			relChildParentInstanceSelect.addElement(relChildParentInstance);
		}

		typeDetailsMap.put("relName", relName);
		typeDetailsMap.put("relDirection", relDirection);
		typeDetailsMap.put("relIdParentSelect", relIdParentSelect);
		typeDetailsMap.put("relParentChildInstanceSelect", relParentChildInstanceSelect);
		typeDetailsMap.put("relIdChildSelect", relIdChildSelect);
		typeDetailsMap.put("relChildParentInstanceSelect", relChildParentInstanceSelect);
		//log("typeDetailsMap -> " + typeDetailsMap);
	}


	private void writeIDToErrorFile(Context context, String objId)
	{
		try
		{
			String attrSource 	= MCADMxUtil.getActualNameForAEFData(context, "attribute_Source");
			String relVersionOf = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");			
			String Args[] = new String[8];
			Args[0] = objId;
			Args[1] = "type";
			Args[2] = "name";
			Args[3] = "revision";
			Args[4] = "attribute["+ attrSource + "]";
			Args[5] = "revisions.id";
			Args[6] = "revisions.to["+ relVersionOf +"].from.id";
			Args[7] = "|";
			
		String idLine	= executeMQL(context,"print bus $1 select $2 $3 $4 $5 $6 $7 dump $8", Args);
		writeDataToErrorFile(idLine);
		}
		catch(Exception e)
		{
			log("ERROR in writing to failed file list.");
			e.printStackTrace(new PrintWriter(iefLog, true));
		}
	}



///////////////////////////////////////////////////////////////////////////////////

	private void writeDataToErrorFile(String idLine)
	{
		try
		{
			if(errorIDsFile == null)
			{
				errorIDsFile = new FileWriter(documentDirectory + "iefMigrationErrorIDs.txt");
			}

			errorIDsFile.write(idLine + "\n");
		}
		catch(Exception e)
		{
		}
	}

	private void writeLineToConsole() throws Exception
	{
		writeMessageToConsole("=======================================================");
	}

	private void writeMessageToConsole(String message) throws Exception
	{
		writer.write(message + "\n");
	}

	private void writeErrorToConsole(String message) throws Exception
	{
		writeLineToConsole();
		writeMessageToConsole(message);
		writeMessageToConsole("Step 2 of Migration     : FAILED");
		writeLineToConsole();
		writer.flush();
	}

	private void writeSuccessToConsole() throws Exception
	{
		String migrationType = "Family Objects";
		if(migrationComponent.equalsIgnoreCase("ProEFile"))
			migrationType = "ProE Files";
		else if(migrationComponent.equalsIgnoreCase("WorkingSet"))
			migrationType = "WorkingSet";

		writeLineToConsole();
		writeMessageToConsole("                Migration for " + migrationType + "  COMPLETE");
		writeMessageToConsole("                Time:"+ (System.currentTimeMillis() - startTime) + "ms ");
		writeMessageToConsole("                Step 2 of Migration     : SUCCESS");
		writeLineToConsole();
		writer.flush();
	}

	private void startIEFLog() throws Exception
	{
		try
		{
			iefLog		= new FileWriter(documentDirectory + "iefMigration10_6.log");
		}
		catch(Exception e)
		{
			writeMessageToConsole("ERROR: Can not create log file. " + e.getMessage());
		}
	}

	private void endIEFLog()
	{
		try
		{
			iefLog.write("\n\n");
			iefLog.flush();
			iefLog.close();

			if(errorIDsFile != null)
			{
				errorIDsFile.flush();
				errorIDsFile.close();
			}
		}
		catch(Exception e)
		{
		}
	}

	private void log(String message)
	{
		try
		{
			iefLog.write(message + "\n");
		}
		catch(Exception e)
		{
		}
	}

	private void logTimeForEvent(String event)
	{
		log("\n\n" + event + " Time: " + System.currentTimeMillis() + "\n\n");
	}
}

