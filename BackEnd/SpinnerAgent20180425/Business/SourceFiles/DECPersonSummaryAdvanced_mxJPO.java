/*
 **  DECPersonSummaryAdvanced
 **
 **  Copyright Dassault Systemes, 1992-2010.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Fetches all the workspace associations of input users
 */

import java.util.Map;
import java.util.Vector;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;

import matrix.db.BusinessObject;
import matrix.db.Access;
import matrix.db.AccessList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.AccessUtil;

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.utils.MCADUtil;

public class DECPersonSummaryAdvanced_mxJPO
{
	public Object getWorkspaceAssociations ( Context context, String[] args ) throws Exception
	{
		Vector returnValues = new Vector();
		boolean pushedContext = false;
                String roleProjectLead =  MCADMxUtil.getActualNameForAEFData(context,"role_ProjectLead");
		try
		{
			Map dataMap = (Map) JPO.unpackArgs(args);

			List objectList    = (List) dataMap.get("objectList");

			String[] personIds = new String[objectList.size()];

			for ( int i = 0; i < objectList.size(); i++ )
			{
				Map idsMap   = (Map) objectList.get(i);
				personIds[i] = idsMap.get("id").toString();
			}

			String relProjectMembershipActualName  	= MCADMxUtil.getActualNameForAEFData(context, "relationship_ProjectMembership");
			String relProjectMembersActualName     	= MCADMxUtil.getActualNameForAEFData(context, "relationship_ProjectMembers");

			// Condition clause that select all WORKSPACE names where person is a member
			String SELECT_ON_WORKSPACE			= "from[" + relProjectMembershipActualName + "].to.to[" + relProjectMembersActualName + "].from";
			String SELECT_WORKSPACE_NAME 		= SELECT_ON_WORKSPACE + ".name";
			String SELECT_WORKSPACE_ID 			= SELECT_ON_WORKSPACE + ".id";
			String SELECT_WORKSPACE_STATE 		= SELECT_ON_WORKSPACE + ".current";
			String SELECT_ATTR_PROJECT_ACCESS 	= "from[" + relProjectMembershipActualName + "].to.to[" + relProjectMembersActualName + "].to." + DomainObject.getAttributeSelect(DomainObject.ATTRIBUTE_PROJECT_ACCESS);
			String SELECT_ON_OWNER 				= SELECT_ON_WORKSPACE + "." + DomainObject.SELECT_OWNER;

			StringList busSelectList 		= new StringList();
			busSelectList.addElement(SELECT_WORKSPACE_NAME);
			busSelectList.addElement(SELECT_WORKSPACE_ID);
			busSelectList.addElement(SELECT_WORKSPACE_STATE);
			busSelectList.addElement(SELECT_ATTR_PROJECT_ACCESS);
			busSelectList.addElement(SELECT_ON_OWNER);
			busSelectList.addElement("name");

			com.matrixone.apps.domain.util.ContextUtil.pushContext(context);
			pushedContext = true;

			BusinessObjectWithSelectList businessObjectWithSelectList = BusinessObject.getSelectBusinessObjectData(context, personIds, busSelectList);

			for ( int index = 0; index < businessObjectWithSelectList.size(); index++ )
			{				
				BusinessObjectWithSelect data 	= businessObjectWithSelectList.getElement(index);
				StringList workspaceNamesList 	= data.getSelectDataList(SELECT_WORKSPACE_NAME);
				StringList workspaceStatesList 	= data.getSelectDataList(SELECT_WORKSPACE_STATE);
				StringList workspaceIdsList 	= data.getSelectDataList(SELECT_WORKSPACE_ID);
				StringList attrProjectAccess 	= data.getSelectDataList(SELECT_ATTR_PROJECT_ACCESS);
				StringList owners 				= data.getSelectDataList(SELECT_ON_OWNER);
				String userName                 = data.getSelectData("name");

				StringBuffer workspaceInfo = new StringBuffer();

				if(null != workspaceIdsList)
				{
					for(int i=0; i<workspaceIdsList.size(); ++i)
					{
						String workspaceState 	= (String)workspaceStatesList.elementAt(i);

						if(null != workspaceState && (workspaceState.equals("Create") || workspaceState.equals("Assign")))
						{
							continue;
						}

						String workspaceId 		= (String)workspaceIdsList.elementAt(i);
						String workspaceName 	= (String)workspaceNamesList.elementAt(i);
						String owner 			= (String)owners.elementAt(i);
						String projectAccess 	= (String)attrProjectAccess.elementAt(i);

						String workspaceAccess = AccessUtil.NONE;
						if(null != owner && owner.equals(userName))
						{
							workspaceAccess = "Owner";
						}
						else if(projectAccess != null && projectAccess.equals(roleProjectLead))
						{
							workspaceAccess = "CoOwner";
						}
						else
						{										
							workspaceAccess = getUserWorkspaceAccess(context, userName, workspaceId);
						}

						workspaceInfo.append(workspaceName).append(",").append(workspaceAccess);

						if(i<workspaceIdsList.size()-1)
						{
							workspaceInfo.append("|");
						}
					}

					returnValues.addElement(workspaceInfo.toString());
				}
				else
				{
					returnValues.addElement("");
				}
			}				
		}
		catch ( Exception e )
		{
			//e.printStackTrace();
		}
		finally
		{
			if(pushedContext)
			{ 
				try
				{
					com.matrixone.apps.domain.util.ContextUtil.popContext(context);
				}
				catch(Exception ex)
				{
					MCADServerException.createException(ex.getMessage(), ex);
				}
			}
		}

		return returnValues;
	}

	private String getUserWorkspaceAccess(Context context, String userName, String workspaceId) throws Exception
	{
		DomainObject workspaceObject     	= DomainObject.newInstance(context);
		workspaceObject.setId(workspaceId);

		AccessList workspaceAccessList 		= workspaceObject.getAccessForGrantor(context, AccessUtil.WORKSPACE_ACCESS_GRANTOR);

		HashMap granteeWorkspaceAccessMap 	= getGranteeAccessMap(workspaceAccessList);

		String sAccessString = AccessUtil.NONE;

		if(granteeWorkspaceAccessMap.containsKey(userName))
		{
			Access access = (Access) granteeWorkspaceAccessMap.get(userName);
			String accessString = AccessUtil.checkAccess(access);

			if(accessString.equals(AccessUtil.ADD_REMOVE))
				sAccessString = "Contributor";
			else if(accessString.equals(AccessUtil.READ_WRITE))
				sAccessString = "Reviewer";
		}

		return sAccessString;
	}

	private HashMap getGranteeAccessMap(AccessList workspaceAccessList)
	{
		HashMap granteeWorkspaceAccessMap = new HashMap();

		Iterator iterator = workspaceAccessList.iterator();

		while(iterator.hasNext())
		{
			matrix.db.Access access = (Access)iterator.next();			
			String grantee 			= access.getUser();

			granteeWorkspaceAccessMap.put(grantee, access);
		}

		return granteeWorkspaceAccessMap;
	}
}

