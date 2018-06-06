/*
 * ${CLASSNAME}.java
 * program for ownership migration.
 *
 * Copyright (c) 1992-2017 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;


import matrix.db.Context;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainAccess;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;

public class emxUpdateOwnershipBase_mxJPO extends emxCommonMigrationBase_mxJPO
{

    private static final String SELECT_OWNERSHIP = "ownership";
    private static final String SELECT_OWNERSHIP_ACCESS = "ownership.access";
    private static final String SELECT_KINDOF_PROJECT_SPACE = "type.kindof[Project Space]";
    private static final String SELECT_IS_PROJECT_SPACE = "type.kindof["+DomainConstants.TYPE_PROJECT_SPACE+"]";
    private static final String SELECT_IS_PROJECT_CONCEPT = "type.kindof["+DomainConstants.TYPE_PROJECT_CONCEPT+"]";
    private static final String SELECT_ASSIGNED_TASKS_PERSON_NAME = "to[Assigned Tasks].from.name";

    static
    {
        DomainObject.MULTI_VALUE_LIST.add(SELECT_OWNERSHIP);
        DomainObject.MULTI_VALUE_LIST.add(SELECT_OWNERSHIP_ACCESS);
        DomainObject.MULTI_VALUE_LIST.add(SELECT_ASSIGNED_TASKS_PERSON_NAME);
    }

    private static final String KEY_PROJECT_MEMBER = "Project Member";
    private static final String KEY_PROJECT_LEAD = "Project Lead";

    public emxUpdateOwnershipBase_mxJPO(Context context, String[] args)
            throws Exception {
        super(context, args);
        // TODO Auto-generated constructor stub
    }

    public void migrateObjects(Context context, StringList objectIdList) throws Exception{
        try {
            // mqlLogRequiredInformationWriter("In emxUpdateOwnershipBase::migrateObjects");
            StringList logicalNames = DomainConstants.EMPTY_STRINGLIST;
            StringList objectSelects = new StringList(6);

            objectSelects.add(DomainConstants.SELECT_ID);
            objectSelects.add(DomainConstants.SELECT_TYPE);
            objectSelects.add(DomainConstants.SELECT_NAME);
            objectSelects.add(SELECT_OWNERSHIP);
            objectSelects.add(SELECT_OWNERSHIP_ACCESS);
            objectSelects.add(SELECT_IS_PROJECT_SPACE);
            objectSelects.add(SELECT_IS_PROJECT_CONCEPT);
            objectSelects.add(SELECT_ASSIGNED_TASKS_PERSON_NAME);
            
            String[] oidsArray = new String[objectIdList.size()];
            oidsArray = (String[])objectIdList.toArray(oidsArray);
            MapList objectList = DomainObject.getInfo(context, oidsArray, objectSelects);

            // TODO - Should use iterator here to build physical/logical map
            logicalNames = DomainAccess.getLogicalNamesForPolicy(context, DomainConstants.POLICY_PROJECT_SPACE);
            String sReader = DomainAccess.getPhysicalAccessMasksForPolicy(context, DomainConstants.POLICY_PROJECT_SPACE, (String)logicalNames.get(0));
            String sLeader = DomainAccess.getPhysicalAccessMasksForPolicy(context, DomainConstants.POLICY_PROJECT_SPACE, (String)logicalNames.get(1));
            String sAuthor = DomainAccess.getPhysicalAccessMasksForPolicy(context, DomainConstants.POLICY_PROJECT_TASK, "Author");

            Iterator itr = objectList.iterator();

            String physicalMask,s,o,access;
            StringList tokens;
            boolean bChanged = false;
            Map m;
            Iterator ownershipItr;
            String org,role,bits,cmd;

            while (itr.hasNext())
            {
                m = (Map)itr.next();

                String objectId = (String)m.get(DomainConstants.SELECT_ID);
                String objectType = (String)m.get(DomainConstants.SELECT_TYPE);
                String objectName = (String)m.get(DomainConstants.SELECT_NAME);
                StringList ownership = (StringList)m.get(SELECT_OWNERSHIP);
                String isKindOfProjectSpace = (String)m.get(SELECT_IS_PROJECT_SPACE);
                String isKindOfProjectConcept = (String)m.get(SELECT_IS_PROJECT_CONCEPT);
                StringList taskAssigneeNames = (StringList)m.get(SELECT_ASSIGNED_TASKS_PERSON_NAME);
                String sMOAUserName;
                
                ownershipItr = ownership.iterator();

                while (ownershipItr.hasNext())
                {
                    o = (String)ownershipItr.next();

                    //
                    // If not personal security context (i.e. xyz_PRJ) nothing to do
                    //
                    if (-1 == o.indexOf("_PRJ")) {
						continue;
					}
                    
                    s = SELECT_OWNERSHIP + "[" + o + "].access";

                    //
                    // Get org and role (project) from ownership string
                    // (needed for MQL add/remove ownership)
                    //
                    tokens = FrameworkUtil.splitString(o,"|");
                    org = (String)tokens.remove(0);
                    role = (String)tokens.remove(0);
                    sMOAUserName = role.replace("_PRJ", "");

                    //
                    // Get ownership.access from the map
                    //
                    access = (String)m.get(s);

                    //
                    // Determine Project Lead vs. Project Member by checking actual mask for modify access
                    //
                    if (access.contains("modify")) {
                        // if modify access bit then we know we need to re-stamp leader if we are migrating a PS or PC
                        if((Boolean.valueOf(isKindOfProjectSpace)) || (Boolean.valueOf(isKindOfProjectConcept))) {
                           physicalMask = sLeader;
                        }
                        else {
                           // if the user was assigned to this task we will change them from leader to author
                           if ((taskAssigneeNames != null) && (taskAssigneeNames.contains(sMOAUserName))) {
                              physicalMask = sAuthor;
                           }
                           else // the user was given lead access to a task thru MOA page to re-stamp as Leader
                           {
                              physicalMask = sLeader;
                           }   
                        }
                    }else{
                        physicalMask = sReader;
                    }

                    cmd = "modify bus " + objectId + " remove ownership '" + org + "' '" + role + "' for '" + DomainAccess.COMMENT_MULTIPLE_OWNERSHIP + "' as all";
                    MqlUtil.mqlCommand(context,cmd);
                    cmd = "modify bus " + objectId + " add ownership '" + org + "' '" + role + "' for '" + DomainAccess.COMMENT_MULTIPLE_OWNERSHIP + "' as " + physicalMask;
                    MqlUtil.mqlCommand(context,cmd);
                }
                if (bChanged) {
                    loadMigratedOids(objectId);
                }else{
                    String comment = "Skipping object <<" + objectName + ">> SOV_OK";
                    writeUnconvertedOID(comment, objectId);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
