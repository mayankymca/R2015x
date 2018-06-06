/**
 * IEFSynchronizeAssignmentsBase.java jpo
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 * This JPO assigns integrations to given person and updates values
 * of attributes in the corresponding local config object for assigned
 * integrations.
 */

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Person;
import matrix.util.MatrixException;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.cache.IEFCache;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADUtil;

public class IEFSynchronizeAssignmentsBase_mxJPO
{
    protected IEFIntegAccessUtil util       = null;
	protected MCADServerResourceBundle serverResourceBundle = null;
	protected IEFGlobalCache cache							= null;

    Hashtable gcoNameAttributesMap		= null;

    protected String LOCAL_CONFIG_OBJ_REV   = "TEAM";
    protected final String ATTRIBUTE_PREFIX       = "IEF-Pref-";
    protected final String ADD_LCO_JPO_NAME       = "MCADLocalConfigObjectActions";

    protected final String KEYVALUEPAIR_SEP		= "\n";
    protected final String KEYVALUE_SEP			= "|";
    protected final String DATA_SEP				= ",";

	protected String bulkLoadingAttrName		= "";
	protected String bulkLoaderRoleName			= "";
	protected String harnessUserRoleName		= "";
	protected String designerRoleName			= "";
	protected String exchangeUserRoleName		= "";

	Vector integrationsToIgnoreForDesignerRole	= null;

	protected boolean	isContextUserBusinessAdmin	= false;
	protected boolean allIntegrationsUnassigned		= false;
	protected boolean isResetPreferences			= false;

    public IEFSynchronizeAssignmentsBase_mxJPO()
    {
    }

    public IEFSynchronizeAssignmentsBase_mxJPO (Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            MCADServerException.createException("not supported no desktop client", null);

    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }

    protected void init (Context context, String languageName, String resetPreferences)throws Exception
    {
        this.gcoNameAttributesMap = new Hashtable();

		this.serverResourceBundle 	= new MCADServerResourceBundle(languageName);
		this.cache					= new IEFGlobalCache();
		this.util					= new IEFIntegAccessUtil(context, serverResourceBundle, cache);

		// The integrations specified in this list will NOT be assigned BOTH Designer and Exchange User roles.
		integrationsToIgnoreForDesignerRole = new Vector(5);
		integrationsToIgnoreForDesignerRole.addElement("msoffice");

		bulkLoadingAttrName		= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-UseBulkLoading");
		bulkLoaderRoleName		= MCADMxUtil.getActualNameForAEFData(context, "role_IEFBulkLoader");
		harnessUserRoleName		= MCADMxUtil.getActualNameForAEFData(context, "role_HarnessUser");
		designerRoleName		= MCADMxUtil.getActualNameForAEFData(context, "role_Designer");
		exchangeUserRoleName	= MCADMxUtil.getActualNameForAEFData(context, "role_ExchangeUser");

		isContextUserBusinessAdmin = isContextUserBusinessAdmin(context);
		isResetPreferences		   = resetPreferences.equalsIgnoreCase("true");
		LOCAL_CONFIG_OBJ_REV       = MCADMxUtil.getConfigObjectRevision(context);
    }

	protected boolean isContextUserBusinessAdmin(Context context)
	{
		boolean bRet = false;
		try
		{
			Person contextUser = new Person(context.getUser());

			contextUser.open(context);
			if(contextUser.isBusinessAdmin())
			{
				bRet = true;
			}
			contextUser.close(context);
		}
		catch(Exception e)
		{
		}

		return bRet;
	}

    public String synchAssignmentsForUsers(Context context, String[] args) throws Exception
    {
        String sRet = "true";

        String personNamesList						= args[0];
        String assignedIntegrationsAndGCONames		= args[1];
        String languageName                         = args[2];
		String resetPreferences                     = args[3];

        //initialize
        init(context, languageName, resetPreferences);

        //assign and unassign integrations to each person in the list
        StringTokenizer personNames	= new StringTokenizer(personNamesList, "|");

        while(personNames.hasMoreTokens())
        {
			//reset this flag to false for each user
			allIntegrationsUnassigned = false;
            String personName = personNames.nextToken();
			synchAssignmentsForIndividualUser(context, personName, assignedIntegrationsAndGCONames);
        }

        return sRet;
    }

    /**
     * Logic for synchronizing assignments is as follows :- For each person, all integrations
     * assigned to all the roles and groups to which this person is assigned, are collected
     * and then lco is updated based on this list.If an integration belonging to this list is not
     * found in the IEF-IntegrationToGCOMapping in lco, an entry is made for this integration in
     * the mapping.Also default values for this integration for all lco attributes are retreived
     * from given gco (from "IEF-Pref-" attributes values) and lco attributes are updated with
     * values for this integrations.
     * The same process is repeated even if the integraion exists.
	 *
     * @param personName
     * @param assignedIntegrationsAndGCONames
     * @throws Exception
     */
    protected void synchAssignmentsForIndividualUser(Context context, String personName, String assignedIntegrationsAndGCONames) throws Exception
    {
        boolean isAssignRoleOnly = true;		
		if(assignedIntegrationsAndGCONames !=null && assignedIntegrationsAndGCONames.length() > 0)
		{
			//will be false if update assignment is done.
			isAssignRoleOnly = false;
		}		
        Person person = new Person(personName);
        //if role "Integration User" is not assigned to the user, and the user is a business admin, assign the role.
		if(isContextUserBusinessAdmin && !person.isAssigned(context, harnessUserRoleName) )
		{
			assignRoleToUser(context, personName, harnessUserRoleName);
		}

		Hashtable assignedIntegrationsGCONamesMap = util.getAssignedIntegrationsTable(context, personName); 
        if(!assignedIntegrationsAndGCONames.equals(""))
        {
            modifyGCOForAssignedIntegration(assignedIntegrationsGCONamesMap, assignedIntegrationsAndGCONames);
        }

		boolean assignDesignerRole = getAssignDesignerRoleFlag(assignedIntegrationsGCONamesMap);
		//if role "Designer" is not assigned to the user, assign it.
		if(assignDesignerRole && !person.isAssigned(context, designerRoleName) && isContextUserBusinessAdmin)
		{
			assignRoleToUser(context, personName, designerRoleName);
		}

		//if role "Exchange User" is not assigned to the user, assign it.
		if(assignDesignerRole && exchangeUserRoleName != null && !exchangeUserRoleName.equals("") && !person.isAssigned(context, exchangeUserRoleName) && isContextUserBusinessAdmin)
		{
			assignRoleToUser(context, personName, exchangeUserRoleName);
		}

		//now (create and) update local config objects
        if(assignedIntegrationsGCONamesMap.size() > 0)
		{
			//get the local config object id. Create it if it does not already exist
			addLocalConfigObject(context, personName);
			String localConfigObjType     = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-LocalConfig");
			
			BusinessObject localConfigObj = new BusinessObject(localConfigObjType, personName, LOCAL_CONFIG_OBJ_REV, "");

			localConfigObj.open(context);
			addInterfaceToLCO(context, localConfigObj.getObjectId(), assignedIntegrationsGCONamesMap);
			updateLCOAttributes(context, localConfigObj, assignedIntegrationsGCONamesMap,isAssignRoleOnly);
			localConfigObj.close(context);
		}
		else
			allIntegrationsUnassigned = true;

		//if all integrations to the user are unassigned, remove role "Harness User"
		if(allIntegrationsUnassigned)
		{
			if(person.isAssigned(context, harnessUserRoleName) && isContextUserBusinessAdmin)
			unassignRoleToUser(context, personName, harnessUserRoleName);

			deleteLocalConfigObject(context, personName);
                        allIntegrationsUnassigned = false;
		}

		boolean unAssignDesignerRole = getUnAssignDesignerRoleFlag(assignedIntegrationsGCONamesMap);
		if(unAssignDesignerRole && person.isAssigned(context, designerRoleName) && isContextUserBusinessAdmin)
		{
			unassignRoleToUser(context, personName, designerRoleName);
		}
    }

	private void addInterfaceToLCO(Context context, String lcoObjectId, Hashtable assignedIntegrationsGCONamesMap) throws Exception 
	{
		Vector interfaceToAdd		= new Vector();
		Vector interfaceToRemove	= new Vector();
		
		Hashtable keyTable = new Hashtable();
		keyTable.put("Context", context);
		keyTable.put("KEY", "getIntegrationInterfaceList");

		Vector allIntegrationInterfacesList	= (Vector)cache.getFromCache(context,IEFCache.CACHETYPE_KEY_CACHE_TABLE, keyTable, false);	
		Enumeration assignedIntegrationList = assignedIntegrationsGCONamesMap.keys();
		
		Vector LCOAssignedInterfaces = util.getAllAssignedInterfacesFromBO(context, lcoObjectId);
		
		for(int i=0; i < LCOAssignedInterfaces.size(); i++)
		{
			interfaceToRemove.addElement(LCOAssignedInterfaces.get(i));
		}
		
		while(assignedIntegrationList.hasMoreElements())
		{
			String integrationName					= (String)assignedIntegrationList.nextElement();
			StringBuffer integrationInterfaceName	= new StringBuffer(integrationName);
			integrationInterfaceName.append(MCADServerSettings.INTEGRATION_INTERFACE_SUFFIX);
			
			// check whether integration interface exists 
			if(allIntegrationInterfacesList.contains(integrationInterfaceName.toString()))
			{
				if(LCOAssignedInterfaces.contains(integrationInterfaceName.toString()))
					interfaceToRemove.removeElement(integrationInterfaceName.toString());
				else
					interfaceToAdd.addElement(integrationInterfaceName.toString());
			}
		}
		
		for (int i = 0; i < interfaceToRemove.size(); i++)
		{
			util.modifyBusToAddRemoveInterface(context, lcoObjectId, (String)interfaceToRemove.elementAt(i), "remove");
		}
		
		for (int i = 0; i < interfaceToAdd.size(); i++)
		{
			util.modifyBusToAddRemoveInterface(context, lcoObjectId, (String)interfaceToAdd.elementAt(i), "add");
		}
	}

	/**
	 * This method determines whether to assigns role "Designer" depending on integrations being assigned
	 *
	 * @param assignedIntegrationsAndGCONames
	 */
	protected boolean getAssignDesignerRoleFlag(Hashtable assignedIntegrationsAndGCONames)
	{
		boolean assignDesignerRole	= false;

		Enumeration assignedTokens = assignedIntegrationsAndGCONames.keys();
		while(assignedTokens.hasMoreElements())
		{
			String assignedIntegration	= (String)assignedTokens.nextElement();

			for(int i = 0; i<integrationsToIgnoreForDesignerRole.size(); i++)
			{
				if(!assignedIntegration.equalsIgnoreCase((String)integrationsToIgnoreForDesignerRole.elementAt(i)))
				{
					assignDesignerRole = true;
					break;
				}
			}
		}

		return assignDesignerRole;
	}

	/**
	 * This method determines whether to unassign role "Designer" depending on integrations being assigned
	 *
	 * @param assignedIntegrationsAndGCONames
	 */
	protected boolean getUnAssignDesignerRoleFlag(Hashtable assignedIntegrationsAndGCONames)
	{
		boolean unAssignDesignerRole = true;

		Enumeration assignedTokens = assignedIntegrationsAndGCONames.keys();
		while(assignedTokens.hasMoreElements())
		{
			String assignedIntegration	= (String)assignedTokens.nextElement();

			for(int i = 0; i<integrationsToIgnoreForDesignerRole.size(); i++)
			{
				if(!assignedIntegration.equalsIgnoreCase((String)integrationsToIgnoreForDesignerRole.elementAt(i)))
				{
					unAssignDesignerRole = false;
					break;
				}
			}
		}

		return unAssignDesignerRole;
	}

	/**
	 * This method assigns role "Harness User" to person with input name
	 *
	 * @param personName
	 */
	protected void assignRoleToUser(Context context, String personName, String roleName) throws MCADException
	{
		String Args[] = new String[4];
		Args[0] = personName;
		Args[1] = "assign";
		Args[2] = "role";
		Args[3] = roleName;
		String result	  = util.executeMQL(context ,"modify person $1 $2 $3 $4", Args);
		if(result.startsWith("false|"))
		{
			MCADServerException.createException(result.substring(6), null);
		}
	}

	/**
	 * This method assigns role "Harness User" to person with input name
	 *
	 * @param personName
	 */
	protected void unassignRoleToUser(Context context, String personName, String roleName) throws MCADException
	{
		String Args[] = new String[5];
		Args[0] = personName;
		Args[1] = "remove";
		Args[2] = "assign";
		Args[3] = "role";
		Args[4] = roleName;
		String result	  = util.executeMQL(context ,"modify person $1 $2 $3 $4 $5", Args);
		if(result.startsWith("false|"))
		{
			MCADServerException.createException(result.substring(6), null);
		}
	}

    protected void modifyGCOForAssignedIntegration(Hashtable assignedIntegrationsGCONamesMap, String integrationAndGCONames)
    {
		integrationAndGCONames = integrationAndGCONames.replace('~', '|');
		StringTokenizer tokens = new StringTokenizer(integrationAndGCONames, ";");

        while(tokens.hasMoreTokens())
        {
            String integAndGCOName = tokens.nextToken();
            String integrationName = integAndGCOName.substring(0, integAndGCOName.indexOf("|"));
            String gcoName         = integAndGCOName.substring(integAndGCOName.indexOf("|") + 1, integAndGCOName.length());

			if(assignedIntegrationsGCONamesMap.containsKey(integrationName))
			{				
				assignedIntegrationsGCONamesMap.put(integrationName, gcoName);
			}
        }
    }

    /**
	 * Iterate through the list which contains all the assigned integrations to the person.
     * If any integration belonging to this list is not found in the mapping in local config object
     * then add an entry for this integration in the local config object mapping. Also copy the attribute
     * values from "IEF-Pref-" type attributes in given gco to corresponding attributes in lco.
	 * Do the same for those integrations which already exist in the mapping in local config object.
     * 
     * @param lcoAttrNameValMap - hashtable containing the existing values of lco attributes
     * @param assignedIntegrationsGCONamesMap - hashtable containing all the assigned integrations and
     *                                          corresponding gco names as key-value pairs
     */
    protected void updateLCOAttributesForAssignment(Context context, String personName, Hashtable lcoAttrNameValMap, Hashtable assignedIntegrationsGCONamesMap,boolean isAssignRoleOnly)throws Exception
    {
        //get the integrations which are mapped in the local config object
		Hashtable integrationGCONameMap = (Hashtable)lcoAttrNameValMap.get(MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping"));

	//Getting Attribute From the EBOM
	    updateEBOMAttributes(context, lcoAttrNameValMap, assignedIntegrationsGCONamesMap);	
        boolean isBulkLoaderRoleAssigned = isRoleAssigned(context, personName, bulkLoaderRoleName);
        
        Enumeration keys = assignedIntegrationsGCONamesMap.keys();
        while(keys.hasMoreElements())
        {
            String integrationName  = (String)keys.nextElement();
			String gcoName          = (String)assignedIntegrationsGCONamesMap.get(integrationName);

			/*write here*/
			if(isAssignRoleOnly)
			{	
				if(!integrationGCONameMap.containsKey(integrationName))
				{					
					integrationGCONameMap.put(integrationName, gcoName);
				}
			}else
			{				
				integrationGCONameMap.put(integrationName, gcoName);
			}

			AttributeList mappedAttributesList = getMappedAttributesList(gcoName, context);
			AttributeItr attrItr               = new AttributeItr(mappedAttributesList);

			//for each attribute in lco, add an entry for this integration in the attribute value
			while (attrItr.next())
			{
				Attribute gcoAttribute = attrItr.obj();

				String gcoAttrName          = gcoAttribute.getName();
				String gcoAttrValueWithTag  = gcoAttribute.getValue();

				String lcoAttrName          = gcoAttrName.substring(ATTRIBUTE_PREFIX.length());
				Hashtable lcoAttrValTable   = (Hashtable)lcoAttrNameValMap.get(lcoAttrName);

				String gcoAttrValue = "";

				String defaultTypePolicySettingsGCOAttrName  = MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-Pref-MCADInteg-DefaultTypePolicySettings");
				String defaultConfigTableSettingsGCOAttrName = MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-Pref-IEF-DefaultConfigTables");
				String defaultWebformSettingsGCOAttrName 	 = MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-Pref-IEF-DefaultWebforms");

				if(gcoAttrName.equals(defaultTypePolicySettingsGCOAttrName))
				{
					StringTokenizer gcoAttrValToken = new StringTokenizer(gcoAttrValueWithTag, MCADAppletServletProtocol.KEYVALUEPAIR_SEP);
					while(gcoAttrValToken.hasMoreElements())
					{
						String gcoAttrVal = (String)gcoAttrValToken.nextElement();

						int index = gcoAttrVal.indexOf(")");
						if(index >= 0)
						{
							gcoAttrValue += gcoAttrVal.substring(index + 1) + MCADAppletServletProtocol.VALUE_SEP;
						}
					}
					if(gcoAttrValue.length() > 0)
					{
						//remove "@" in the end
						gcoAttrValue = gcoAttrValue.substring(0, gcoAttrValue.length() - 1);
					}
				}
				else if(gcoAttrName.equals(defaultConfigTableSettingsGCOAttrName) || gcoAttrName.equals(defaultWebformSettingsGCOAttrName))
				{
					StringTokenizer gcoAttrValToken = new StringTokenizer(gcoAttrValueWithTag, MCADAppletServletProtocol.VALUE_SEP);
					while(gcoAttrValToken.hasMoreElements())
					{
						String gcoAttrVal = (String)gcoAttrValToken.nextElement();
						int index = gcoAttrVal.indexOf(")");
						if(index >= 0)
						{
							gcoAttrValue += gcoAttrVal.substring(index + 1) + MCADAppletServletProtocol.VALUE_SEP;
						}
						else if(!gcoAttrVal.equals(""))
						{
							gcoAttrValue += gcoAttrVal + MCADAppletServletProtocol.VALUE_SEP;
						}
					}
					if(gcoAttrValue.length() > 0)
					{
						//remove "@" in the end
						gcoAttrValue = gcoAttrValue.substring(0, gcoAttrValue.length() - 1);
					}
				}
				else
				{
					gcoAttrValue = gcoAttrValueWithTag.substring(gcoAttrValueWithTag.indexOf(")") +1);
				}

				//if attribute is "IEF-UseBulkLoading" and user does not have role "IEF Bulk Loader"
				//assigned, keep the attribute value as false.
				if(lcoAttrValTable != null)
				{
				if(lcoAttrName.equals(bulkLoadingAttrName) && !isBulkLoaderRoleAssigned)
				{
					lcoAttrValTable.put(integrationName, "false");
				}
				else if(!lcoAttrValTable.containsKey(integrationName) || isResetPreferences)
				{
					lcoAttrValTable.put(integrationName, gcoAttrValue);
				}

				lcoAttrNameValMap.put(lcoAttrName, lcoAttrValTable);
			}
        }
    }
    }


    protected void updateLCOAttributesForUnassignment(Context context, Hashtable lcoAttrNameValMap, Hashtable assignedIntegrationsGCONamesMap) throws Exception // Harshal
    {
        //get the integrations which are mapped in the local config object
        String integrationToGCOMapping  = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");
    	Hashtable integrationGCONameMap = (Hashtable)lcoAttrNameValMap.get(integrationToGCOMapping);

        Enumeration keys = integrationGCONameMap.keys();
        while(keys.hasMoreElements())
        {
            String integrationName = (String)keys.nextElement();
            //if an entry is not found in assignedIntegrationsGCONamesMap table -> integration is unassigned
            if(!assignedIntegrationsGCONamesMap.containsKey(integrationName))
            {
                //remove the entries for this integration from all the attributes in lco
                Enumeration attrNames = lcoAttrNameValMap.keys();
                while(attrNames.hasMoreElements())
                {
                    String attrName      = (String)attrNames.nextElement();
                    Hashtable attrValMap = (Hashtable)lcoAttrNameValMap.get(attrName);
					
                    if(attrValMap.containsKey(integrationName))
                    {
                        attrValMap.remove(integrationName);
						lcoAttrNameValMap.put(attrName, attrValMap);
                    }
                }
            }
        }

		//check if all the integrations have been unassigned in which case set the appropriate flag
        Hashtable updatedIntegrationGCONameMap = (Hashtable)lcoAttrNameValMap.get(integrationToGCOMapping);
		if(updatedIntegrationGCONameMap.size() == 0)
		{
			allIntegrationsUnassigned = true;
		}
    }

    /**
     * This method gets the attributes list with values updated for all the assigned integrations
     * If an integration is already assigned to the person, attribute values are not updated for
     * that integration.If an integration is not previously assigned, attribute values are copied
     * from corresponding attributes in the assigned gco.
	 *
     * @param localConfigObj
     * @param assignedIntegrationsGCONamesMap - array containing assigned integration names as keys
     *                                          and corresponding gco names as values
     */
    protected void updateLCOAttributes(Context context, BusinessObject localConfigObj, Hashtable assignedIntegrationsGCONamesMap,boolean isAssignRoleOnly) throws Exception
    {
        try
        {
            Hashtable lcoAttrNameValMap     = loadLCOAttributes(context, localConfigObj);

			updateLCOAttributesForAssignment(context, localConfigObj.getName(), lcoAttrNameValMap, assignedIntegrationsGCONamesMap,isAssignRoleOnly);
            updateLCOAttributesForUnassignment(context, lcoAttrNameValMap, assignedIntegrationsGCONamesMap);

			//now get the attributelist from hashtable and set it on the local config object
				AttributeList attributeList = getAttributesListFromHashtable(lcoAttrNameValMap);
				localConfigObj.setAttributes(context, attributeList);
			}
        catch (Exception ex)
        {
			String errorMessage	= serverResourceBundle.getString("mcadIntegration.Server.Message.IEF0013200025");
			String errorCode	= "IEF0013200025";

			MCADServerException.createManagedException(errorCode, errorMessage, ex);
        }
    }

    protected Hashtable loadLCOAttributes(Context context, BusinessObject localConfigObj) throws MCADException
    {
        Hashtable lcoAttributeNameValMap = new Hashtable();

        try
        {
            AttributeList attrList = localConfigObj.getAttributeValues(context);
            AttributeItr attrItr   = new AttributeItr(attrList);

            while (attrItr.next())
            {
                Attribute attribute   = attrItr.obj();
                String attributeName  = attribute.getName();
                String attributeValue = attribute.getValue();

                lcoAttributeNameValMap.put(attributeName, MCADUtil.getTableFromString(attributeValue, KEYVALUEPAIR_SEP, KEYVALUE_SEP));
            }
        }
		catch(Exception ex)
        {
			String errorMessage	= serverResourceBundle.getString("mcadIntegration.Server.Message.IEF0013200026");
			String errorCode	= "IEF0013200026";

			MCADServerException.createManagedException(errorCode, errorMessage, ex);
        }

        return lcoAttributeNameValMap;
    }


    protected AttributeList getAttributesListFromHashtable(Hashtable lcoAttrNameValMap)
    {
        AttributeList attributeList    = new AttributeList(lcoAttrNameValMap.size());

        Enumeration attrNames       = lcoAttrNameValMap.keys();
        while(attrNames.hasMoreElements())
        {
            String attrName      = (String)attrNames.nextElement();
            Hashtable attrValMap = (Hashtable)lcoAttrNameValMap.get(attrName);
            String attrVal       = getStringFromTable(attrValMap, KEYVALUEPAIR_SEP, KEYVALUE_SEP);

			Attribute attribute = new Attribute(new AttributeType(attrName), attrVal);
            attributeList.addElement(attribute);
        }

        return attributeList;
    }

    protected AttributeList getMappedAttributesList(String gcoName, Context context)
    {
        AttributeList mappedAttributesList = new AttributeList();
        try
        {
            if(!gcoNameAttributesMap.containsKey(gcoName))
            {
				String globalConfigObjType		= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
				String globalConfigObjectID		= util.getGlobalConfigObjectID(context, globalConfigObjType, gcoName);
				BusinessObject globalConfigObj  = new BusinessObject(globalConfigObjectID);

				AttributeList attrList  = globalConfigObj.getAttributeValues(context);
                AttributeItr attrItr    = new AttributeItr(attrList);

                while (attrItr.next())
                {
                    Attribute attribute = attrItr.obj();
                    String AttrName  = attribute.getName();

                    if(AttrName.startsWith(ATTRIBUTE_PREFIX))
                    {
                        mappedAttributesList.addElement(attribute);
                    }
                }
                gcoNameAttributesMap.put(gcoName, mappedAttributesList);
            }
            else
            {
                mappedAttributesList = (AttributeList)gcoNameAttributesMap.get(gcoName);
            }
        }
        catch(Exception ex)
        {

        }
        return mappedAttributesList;
    }

    protected void addLocalConfigObject(Context context, String personName) throws MCADException
    {
        String[] args = new String[3];
        args[0] = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-LocalConfig");
        args[1] = personName;
        args[2] = LOCAL_CONFIG_OBJ_REV;

        String[] init = new String[]{};

        try
        {
            String result   = (String)JPO.invoke(context, ADD_LCO_JPO_NAME, init, "addLocalConfigObject", args, String.class);

            if(result.startsWith("false|"))
            {
				String errorMessage	= result.substring(6);
				String errorCode 	= "IEF0013300023";

				MCADServerException.createManagedException(errorCode, errorMessage, null);
            }

        }
        catch(Exception e)
        {
			String errorMessage = serverResourceBundle.getString("mcadIntegration.Server.Message.IEF0013300023");
			String errorCode 	= "IEF0013300023";

			MCADServerException.createManagedException(errorCode, errorMessage, e);
        }
    }

	protected void deleteLocalConfigObject(Context context, String personName) throws MCADException
    {
        String[] args = new String[3];
        args[0] = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-LocalConfig");
        args[1] = personName;
        args[2] = LOCAL_CONFIG_OBJ_REV;		
        String[] init = new String[]{};

        try
        {
            String result   = (String)JPO.invoke(context, ADD_LCO_JPO_NAME, init, "deleteLocalConfigObject", args, String.class);			
            if(result.startsWith("false|"))
            {
				String errorMessage	= result.substring(6);
				String errorCode 	= "IEF0013300024";

				MCADServerException.createManagedException(errorCode, errorMessage, null);
            }

        }
        catch(Exception e)
        {
			String errorMessage = serverResourceBundle.getString("mcadIntegration.Server.Message.IEF0013300024");
			String errorCode 	= "IEF0013300024";

			MCADServerException.createManagedException(errorCode, errorMessage, e);
        }
    }

    /**
     * This method takes a hashtable and returns string
     * For example,inTable contains key1="a" value1="1,2"
     * and key2="b" value2="3,4", and separator1=";" and
     * separator2="|", then this method will return
     * "a|1,2;b|3,4"
     */
    protected String getStringFromTable(Hashtable inTable,String separator1,String separator2)
    {
        StringBuffer retVal = new StringBuffer("");

        Enumeration keys = inTable.keys();
        while(keys.hasMoreElements())
        {
            String key = (String)keys.nextElement();
            String value = (String)inTable.get(key);

            retVal.append(key);

            if(!value.equals("dummy"))
            {
                retVal.append(separator2);
                retVal.append(value);
                retVal.append(separator1);
            }
        }

        if(retVal.length() > 0)
        {
            //remove the extra comma at the end
            int index = retVal.toString().lastIndexOf(separator1);

			if(index > -1)
			{
				retVal.delete(index,retVal.length());
			}
        }

        return retVal.toString();
    }

    	/**
	 * This method determines if given role is assigned to given person
	 *
	 * @param personName - person name
	 * @param roleName - role name (should pass actual name which can be
	 *					 get by using getActualNameForAEFData to get the actual name)
	 *
	 * @returns - true if role is assigned, false otherwise
	 * @throws - MCADException
	 */
    public boolean isRoleAssigned(Context context, String personName, String roleName) throws MCADException
    {
		boolean bRoleAssigned = false;
		try
		{
			Person person = new Person(personName);
			bRoleAssigned = person.isAssigned(context, roleName);
		}
		catch(MatrixException e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

        return bRoleAssigned;
    }

	private Hashtable updateEBOMAttributes(Context context, Hashtable lcoAttrNameValMap, Hashtable integrationGCONameMap)
	{
		String attrName				= "";
		try
		{
			String  OBJECT_ATTR_MAPPING_NAME = MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-EBOMSync-ObjectAttrMapping");
			String  REL_ATTR_MAPPING_NAME	 = MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-EBOMSync-RelAttrMapping");
			String attrNm 					 = OBJECT_ATTR_MAPPING_NAME+ "|" + REL_ATTR_MAPPING_NAME;
			
			IEFEBOMConfigObject ebomConfigObject = null;
			StringTokenizer attrToken = new StringTokenizer(attrNm,"|");
			
			while(attrToken.hasMoreElements())
			{
				attrName = (String) attrToken.nextElement();
				
				String eBOMTNRNm		= MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-EBOMSync-RegistryTNR");

				Enumeration keys		= integrationGCONameMap.keys();
				Hashtable valueMap		 = (Hashtable) lcoAttrNameValMap.get(attrName);
				
				while(keys.hasMoreElements())
				{
					String value = "";
				    String integrationName  = (String)keys.nextElement();
				    
					String gcoName          = (String)integrationGCONameMap.get(integrationName);
					
					String globalConfigObjType		= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
					String globalConfigObjectID		= util.getGlobalConfigObjectID(context, globalConfigObjType, gcoName);
					BusinessObject globalConfigObj  = new BusinessObject(globalConfigObjectID);

					Attribute eBOMRegistryTNR		= globalConfigObj.getAttributeValues(context, eBOMTNRNm);

					Vector validTypes 				= null;
					
					if(attrName.equals(REL_ATTR_MAPPING_NAME))
						validTypes = getValidTypesForRelationship(context, globalConfigObj);
					else
						validTypes = getValidTypesForObjectType(context, globalConfigObj);
					
					StringTokenizer token = new StringTokenizer(eBOMRegistryTNR.getValue(), "|");

					if(token.countTokens() >= 3)
					{
						String sEBOMRConfigObjType			= (String) token.nextElement();
						String sEBOMRConfigObjName			= (String) token.nextElement();
						String sEBOMRConfigObjRev			= (String) token.nextElement();
						BusinessObject eBomCongifObj = new BusinessObject(sEBOMRConfigObjType,sEBOMRConfigObjName,sEBOMRConfigObjRev,"");
						boolean eBomConfigExists = eBomCongifObj.exists(context);						
						if(eBomConfigExists)
						{
						ebomConfigObject	= new IEFEBOMConfigObject(context, sEBOMRConfigObjType, sEBOMRConfigObjName, sEBOMRConfigObjRev);
					}
					}					
					//Retrieving Value for Object Mapping
					StringBuffer valueBuffer = new StringBuffer();
					String attrMaping		= null;
					if(ebomConfigObject != null)
					{
						attrMaping		= ebomConfigObject.getConfigAttributeValue(attrName);
					}
					if(attrMaping != null && attrMaping.length() > 0 && validTypes.size() > 0)
					{
						StringTokenizer attrMappingTok = new StringTokenizer(attrMaping, "\n");
						while(attrMappingTok.hasMoreElements())
						{
							String attrMappingValue = (String) attrMappingTok.nextElement();
							
							StringTokenizer mappingTokens = new StringTokenizer(attrMappingValue, "|");
							
							String mcadType		  		  = (String)mappingTokens.nextElement();
							
							if(validTypes.contains(mcadType))
								valueBuffer.append(attrMappingValue).append("@");
					}
						
						value = valueBuffer.toString();
					}
					
					if(value.length() > 0)
					{
						//remove "@" in the end
						value = value.substring(0, value.length() - 1);
					}	
					
					if(!valueMap.containsKey(integrationName) || isResetPreferences)
					valueMap.put(integrationName, value);
				}
					
				lcoAttrNameValMap.put(attrName, valueMap);
			}
		}
		catch (Exception e)
		{
		}
		
		return 	lcoAttrNameValMap;
	}

	private Vector getValidTypesForObjectType(Context context, BusinessObject globalConfigObj) throws MCADException,MatrixException
	{
		String typePolicyMapping 		= MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-TypePolicyMapping");
		
		Vector validTypes 				= new Vector();
		
		Attribute typePolicyMappingAttr = globalConfigObj.getAttributeValues(context, typePolicyMapping);
		
		Hashtable typeAndPolicyMap 		= MCADUtil.getTableFromString(typePolicyMappingAttr.getValue(), KEYVALUEPAIR_SEP, KEYVALUE_SEP);

		validTypes.addAll(typeAndPolicyMap.keySet());
		
		return validTypes;
	}

	private Vector getValidTypesForRelationship(Context context, BusinessObject globalConfigObj) throws MCADException,MatrixException
	{
		String relMapping 		 = MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-RelMapping");
		
		Vector validTypes 		 = new Vector();
		
		Attribute relMappingAttr = globalConfigObj.getAttributeValues(context, relMapping);
		
		Hashtable relTypeMap 	 = MCADUtil.getTableFromString(relMappingAttr.getValue(), KEYVALUEPAIR_SEP, KEYVALUE_SEP);
		
		Iterator mxEndRelTypes 	 = relTypeMap.values().iterator();
		
		while(mxEndRelTypes.hasNext())
		{
			String [] endRelname = MCADUtil.getTokensSeperatedByFirstDelimiter((String)mxEndRelTypes.next(), ",");
			String relName 		 = endRelname[1].trim();
			
			validTypes.add(relName);
		}
		
		return validTypes;
	}
}

