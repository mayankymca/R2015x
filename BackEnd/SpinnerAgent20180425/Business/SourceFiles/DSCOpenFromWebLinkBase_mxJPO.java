import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.beans.MCADFolderUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADObjectsIdentificationUtil;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.util.PersonUtil;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.util.StringList;


public class DSCOpenFromWebLinkBase_mxJPO extends IEFCommonUIActions_mxJPO {

	MCADFolderUtil folderUtil						= null;

	public DSCOpenFromWebLinkBase_mxJPO()
	{

	}
	
	public DSCOpenFromWebLinkBase_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("not supported no desktop client", null);

	}

	@Override
	protected void executeCustom(Context _context, Hashtable resultDataTable) throws MCADException 
	{
		try{
			folderUtil	= new MCADFolderUtil(_context, _serverResourceBundle, _cache);

			String integrationName					= (String) _argumentsTable.get(MCADAppletServletProtocol.INTEGRATION_NAME);
			String commandName						= (String) _argumentsTable.get("featureName");
			BusinessObjectWithSelect busWithSelect 	= (BusinessObjectWithSelect) _argumentsTable.get("selectionList");
			Hashtable argumentsList 				= getOpenFromWebArgumentsList(_context,  integrationName, busWithSelect);

			StringBuffer hrefString = new StringBuffer(50);
			hrefString.append(integrationName);
			hrefString.append(":");
			hrefString.append(commandName);
			hrefString.append(":");
			hrefString.append("busid=");
			hrefString.append(MCADUrlUtil.hexEncode(_busObjectID));
			hrefString.append(":");

			Set objectKeys	= argumentsList.keySet();		
			Iterator iter 	= objectKeys.iterator();

			while(iter.hasNext())
			{
				String key 		= (String) iter.next();
				String value	= (String) argumentsList.get(key);
				value			= MCADUrlUtil.hexEncode(value);

				hrefString.append(key).append("=").append(value);
				if(iter.hasNext())
					hrefString.append(":");
			}

			resultDataTable.put("hrefString", hrefString.toString());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private Hashtable getOpenFromWebArgumentsList(Context context, String integrationName, BusinessObjectWithSelect busWithSelect) throws Exception
	{
		Hashtable argumentsList = new Hashtable();

		String userName					= context.getUser();
		String userID					= PersonUtil.getPersonObjectID(context, userName);
		String role						= context.getRole();

		String REL_VERSION_OF			= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		String SELECT_ON_MAJOR			= "from[" + REL_VERSION_OF + "].to.";
		String ATTR_TITLE				= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Title") + "]";
		String REL_VAULTED_DOCUMENTS	= MCADMxUtil.getActualNameForAEFData(context, "relationship_VaultedDocuments");
		String TYPE_WORKSPACE_VAULT 	= MCADMxUtil.getActualNameForAEFData(context, "type_ProjectVault");
		String GET_VAULT_IDS 			= "to[" + REL_VAULTED_DOCUMENTS + "].from[" + TYPE_WORKSPACE_VAULT + "].id";
		String ATTR_CAD_TYPE			= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType") + "]";

		if(null == busWithSelect)
		{
			StringList busSelectionList =  new StringList();
			busSelectionList.addElement("id");
			busSelectionList.addElement(SELECT_ON_MAJOR + "logicalid");
			busSelectionList.addElement("logicalid");
			busSelectionList.addElement("physicalid");
			busSelectionList.addElement("name");
			busSelectionList.addElement("revision");
			busSelectionList.addElement(ATTR_TITLE);
			busSelectionList.addElement(GET_VAULT_IDS);
			busSelectionList.addElement(SELECT_ON_MAJOR + GET_VAULT_IDS);
			busSelectionList.addElement(ATTR_CAD_TYPE); 

			busWithSelect = (BusinessObjectWithSelect) BusinessObject.getSelectBusinessObjectData(context, new String[]{_busObjectID}, busSelectionList).elementAt(0);
		}

		String cadType		= busWithSelect.getSelectData(ATTR_CAD_TYPE);
		
		String logicalID		= busWithSelect.getSelectData("logicalid");		
		String majorLogicalID	= busWithSelect.getSelectData(SELECT_ON_MAJOR + "logicalid");
		
		if(null == majorLogicalID || "".equals(majorLogicalID))
		{
			majorLogicalID	= logicalID;
		}

		StringList connectedFolderIDs           = busWithSelect.getSelectDataList(SELECT_ON_MAJOR + GET_VAULT_IDS);
		if(null == connectedFolderIDs || connectedFolderIDs.size() < 1)
		{
			connectedFolderIDs           = busWithSelect.getSelectDataList(GET_VAULT_IDS);
		}

		HashMap workspaceIdFolderListMap	= new HashMap();
		if(null != connectedFolderIDs && connectedFolderIDs.size() > 0)
		{
			Enumeration foldersEnum	= connectedFolderIDs.elements();
			while(foldersEnum.hasMoreElements())
			{
				String folderId	= ( String ) foldersEnum.nextElement();
				BusinessObject workspaceObj	= folderUtil.getTopWorkspaceObject(context, folderId);
				workspaceObj.open(context);
				if(!workspaceIdFolderListMap.containsKey(workspaceObj.getObjectId(context)))
				{
					HashSet folderIdSet = new HashSet();
					folderIdSet.add(folderId);
					workspaceIdFolderListMap.put(workspaceObj.getObjectId(context), folderIdSet);
				}
				else
				{
					HashSet folderIdsSet	= (HashSet) workspaceIdFolderListMap.get(workspaceObj.getObjectId(context));
					folderIdsSet.add(folderId);

				}
				workspaceObj.close(context);
			}

		}

		String workspaceIds	= "";
		StringBuffer folderIds	= new StringBuffer();

		Set workspaceIdsSet	= workspaceIdFolderListMap.keySet();
		workspaceIds	= MCADUtil.getDelimitedStringFromCollection(workspaceIdsSet, ",");

		Iterator workspaceIter	= workspaceIdsSet.iterator();
		while(workspaceIter.hasNext())
		{
			String workspaceID	=  (String) workspaceIter.next();
			HashSet folderIdsSet	= (HashSet) workspaceIdFolderListMap.get(workspaceID);
			folderIds.append(MCADUtil.getDelimitedStringFromCollection(folderIdsSet, "|"));

			if(workspaceIter.hasNext())
				folderIds.append(",");
		}

		argumentsList.put("userid", userID);
		argumentsList.put("securitycontext", role);
		
		argumentsList.put("workspaceid", workspaceIds);
		argumentsList.put("folderid", folderIds.toString());
		argumentsList.put("lid", majorLogicalID);	
		
		if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
		{
			Set instanceIdsList				 = new HashSet();
			instanceIdsList.add(_busObjectID);
			Hashtable instanceIdtoProcessMap = _generalUtil.getValidObjectIdToGetParent(context, instanceIdsList);
			
			Set inputInstanceIDS = new HashSet(instanceIdtoProcessMap.values().size()); 
			inputInstanceIDS.addAll(instanceIdtoProcessMap.values());

			Hashtable instanceIdFamilyId 	= _generalUtil.getTopLevelFamilyObjectForInstanceIDs(context, inputInstanceIDS, true);
			
			Vector familyIds	= new Vector(instanceIdFamilyId.values());
			
			MCADObjectsIdentificationUtil objIdentificationUtil		= new MCADObjectsIdentificationUtil(context,_serverResourceBundle, _cache);
			Hashtable familyIdLogicalId	= (Hashtable) objIdentificationUtil.getLogicalIdListFromBusIdList(context, familyIds);
			
			String familyLogicalId	= (String) familyIdLogicalId.get(familyIds.elementAt(0));
			argumentsList.put("familymajorlid", familyLogicalId);

		}
		else if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
		{
			argumentsList.put("familymajorlid", majorLogicalID);
		}
		
		argumentsList.put("phid", busWithSelect.getSelectData("physicalid"));
		argumentsList.put("busname", busWithSelect.getSelectData("name"));
		argumentsList.put("busrevision", busWithSelect.getSelectData("revision"));
		argumentsList.put("title", busWithSelect.getSelectData(ATTR_TITLE));

		return argumentsList;
	}
}
