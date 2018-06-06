//IEFCDMUtil.java

//Copyright (c) 2002 MatrixOne, Inc.
//All Rights Reserved
//This program contains proprietary and trade secret information of
//MatrixOne, Inc.  Copyright notice is precautionary only and does
//not evidence any actual or intended publication of such program.


import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Locale;

import matrix.db.Access;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.FileItr;
import matrix.db.FileList;
import matrix.db.FormatItr;
import matrix.db.FormatList;
import matrix.db.JPO;
import matrix.util.StringList;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;

import matrix.db.BusinessObjectWithSelectList;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import matrix.db.BusinessObjectWithSelect;

public class IEFCDMUtil_mxJPO
{
    public IEFCDMUtil_mxJPO (Context context, String[] args) throws Exception
    {
    }

    /**
     * Revise a document object (Major object)
     * Generates XML string representing the Project Space.
     *
     * @param busid String value of the BusID identifying the Project Space object
     * @throws Exception if the operation fails
     */
    public String createRevision(Context context, String[] args) throws Exception
    {
        String xmlOutput = "";
        String lang = args[0];
        try
        {
            ContextUtil.startTransaction(context, true);

            //regional language used by the CSE
            String type = args[1];
            String name = args[2];
            String rev = args[3];
            xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.DocumentRevisedSuccessfully",  "iefStringResource", lang);

            boolean copyFiles = new Boolean(args[4]).booleanValue();

            String oid = getBusId(context,args[1],args[2],args[3]);
            
            //Fire Subscription Event(s)
            iefFireSubscriptionEvent(context, oid, "Revised", 1);

            Hashtable argumentTable = new Hashtable(1);
            argumentTable.put("id", oid);
            
            DomainObject domainObject = iefGetBusinessObject(context,oid);
            
           iefRevise(context, domainObject, copyFiles);

            ContextUtil.commitTransaction(context);
        } 
        catch (Exception ex)
        {
            ContextUtil.abortTransaction(context);

            MCADServerException.createException(ex.getMessage(), ex);
        }

        return xmlOutput;
    }
    
    private DomainObject iefGetBusinessObject(Context context, String oid) throws Exception
    {
        Hashtable argumentTable = new Hashtable(1);
        argumentTable.put("id", oid);
        
        return (DomainObject)JPO.invoke(context, "IEFCDMSupport", null, "iefGetBusinessObject", JPO.packArgs(argumentTable), DomainObject.class);
    }
    
    private void iefRevise(Context context, DomainObject domainObject, boolean copyFiles) throws Exception
    {
        ((CommonDocument)domainObject).revise(context, copyFiles);
    }

    public void iefDeleteDocuments(Context context, DomainObject domainObject, String[] oids) throws FrameworkException
    {
        ((CommonDocument)domainObject).deleteDocuments(context, oids);
    }
    
    public void iefFireSubscriptionEvent(Context context, String oid, String event,  int count) throws Exception
    {
        Hashtable argsTable = new Hashtable(3);
        argsTable.put("busId", oid);
        argsTable.put("event", event);
        argsTable.put("count", new Integer(count));
        
        JPO.invoke(context, "IEFCDMSupport", null, "iefFireSubscriptionEvent", JPO.packArgs(argsTable), String.class);
    }

    /**
     * Delete a document object
     * Based on the parameter passed a major object or version object could be deleted
     *
     * @param busid String value of the BusID identifying the Project Space object
     * @throws Exception if the operation fails
     */
    public String delete(Context context, String[] args) throws Exception
    {
        String xmlOutput = "";
        String lang = args[0];
        try
        {
			
			Locale localeLang=new Locale(lang);
			context.setLocale(localeLang);
            ContextUtil.startTransaction(context, true);

            xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.DocumentDeletedSuccessfully",  "iefStringResource", lang);

            //currently only one action supported
            String action = "delete";
            String objectId = null;

            //regional language used by the CSE
            String type = args[1];
            String name = args[2];
            String rev  = args[3];
            String parentType = null;
            String parentName = null;
            String parentRev  = null;
            String filename   = null;

            if(args.length > 7)
            {
                parentType = args[4];
                parentName = args[5];
                parentRev  = args[6];
                action     = args[7];
                if(args.length > 8)
                    filename = args[8];
            }

            //get document Id
            String busId = getBusId(context,args[1],args[2],args[3]);
            
            String[] oids = {busId};

            if((args.length > 7) && (parentType != null) && (!parentType.equals("Command")))
            {
                objectId = getBusId(context,args[4],args[5],args[6]);
            }
            else
            {
                objectId = iefGetCDMMajorObject(context, busId);
            }

            //reuse the CDM code from here

            //support disconnect, deleteFile from CSE in the future?
            if ( "disconnect".equals(action) )
            {
                //CommonDocument.removeDocuments(context, relIds, false);
            } else if ("delete".equalsIgnoreCase(action) )
            {
                //delete Master Object
                //Fire Subscription Event(s)
                
				StringList slSelectsForInputID=new StringList(2);
				String activeVersion="from[Active Version].to.locked";
				slSelectsForInputID.addElement(activeVersion);
				slSelectsForInputID.addElement("id");
				String [] objectIds =new String[1];
				objectIds[0]=busId;
				BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objectIds, slSelectsForInputID);
				boolean isLocked=false;
				for(int i =0; i <buslWithSelectionList.size(); i++){
						BusinessObjectWithSelect busObjectWithSelect 		= (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
						String busid         = (String)busObjectWithSelect.getSelectData("id");
						System.out.println("id::"+busid);
						StringList activeMinorLockStatus         = (StringList)busObjectWithSelect.getSelectDataList(activeVersion);
							if(activeMinorLockStatus!=null && activeMinorLockStatus.size()>0){	
						 for(int j =0; j <activeMinorLockStatus.size(); j++){
							
								String lockStatus=(String)activeMinorLockStatus.elementAt(j);
								
								if(lockStatus.equalsIgnoreCase("true"))
								{
									isLocked=true;
									break;
								}
							} 
						}else{
								isLocked=false;
							}			
				}
				if(isLocked)
                {
					String selectPropValue = i18nNow.getI18nString("mcadIntegration.Server.Message.deleteFailuredueToLock",  "iefStringResource", lang);
					Exception ex=new Exception(selectPropValue);
					throw ex;
				}
				else{
						for (int i=0;i<oids.length;i++){
                    String oid = oids[i];
                    iefFireSubscriptionEvent(context, oid, "Deleted", 1);
                }
                
                DomainObject domain_Object = iefGetBusinessObject(context,busId);
                
                iefDeleteDocuments(context, domain_Object, oids);
            }
			}
            else if ("deleteVersion".equalsIgnoreCase(action) )
            {
                //delete single file
                //Fire Subscription Event(s)
                iefFireSubscriptionEvent(context, objectId, "Content_Deleted", oids.length);

                DomainObject domainObject1 = iefGetBusinessObject(context, objectId);
                
                iefDeleteVersion(context, domainObject1, oids, false);
            } 
            else if ("deleteFile".equalsIgnoreCase(action))
            {
                //delete all files
                //Fire Subscription Event(s)
                Hashtable argumentsTable = new Hashtable(3);
                argumentsTable.put("busId", objectId);
                argumentsTable.put("event", "Content_Deleted");
                argumentsTable.put("count", new Integer(oids.length));
                
                JPO.invoke(context, "IEFCDMSupport", null, "iefFireSubscriptionEvent", JPO.packArgs(argumentsTable), String.class);

                xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.FileDeletedSuccessfully",  "iefStringResource", lang);

                Hashtable argsTable = new Hashtable(1);
                argsTable.put("id", objectId);
                
                DomainObject domainObject2 = iefGetBusinessObject(context, objectId);
                
                if(!(isVersionable(context, objectId)))
                {
                    if(filename != null && filename.length() != 0)
                    {
                        if(deleteFileFromNonVersionableObject(context, domainObject2, objectId, filename))
                            xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.FileDeletedSuccessfully",  "iefStringResource", lang);

                    }
                }
                else
                {
                    iefDeleteVersion(context, domainObject2, oids, true);
                }
            }
            ContextUtil.commitTransaction(context);
        }
        catch (Exception ex)
        {
            ContextUtil.abortTransaction(context);
            MCADServerException.createException(ex.getMessage(), ex);
        }
        
        return xmlOutput;
    }

    private void iefDeleteVersion(Context context, DomainObject domainObject, String[] oids, boolean b) throws Exception
    {
		//IR-498673-3DEXPERIENCER2015x : JPO error is displayed when trying to delete released document
    	 try
     	{ 	
        ((CommonDocument)domainObject).deleteVersion(context, oids, b);
}
     	catch(Exception ex)
     	{
     		//to check delete access
     		//if access throws user defined error message
     		if(!deleteAccessCheck(context,domainObject)){
     			ex=new Exception(deleteErrorMessage(context,domainObject));
         		throw ex;
     		}//kernel based error message
     		else
     		{
     			throw ex;
     		}
     	}
    }

private boolean deleteAccessCheck(Context context,DomainObject domainObject) throws Exception
    {
    	boolean check=false;
    	BusinessObject busObject = domainObject;
 		busObject.open(context);
 		Access access = busObject.getAccessMask(context);
 		if(access.hasDeleteAccess())
     	{
     		check=true;
     	}
 		busObject.close(context);
    	return check;
    }

 private String deleteErrorMessage(Context context,DomainObject domainObject) throws Exception
    {
    	Hashtable messageTokens = new Hashtable();
		messageTokens.put("TYPE",domainObject.getTypeName());
		messageTokens.put("TITLE",domainObject.getName());
		messageTokens.put("REVISION",domainObject.getRevision());
		MCADServerResourceBundle serverResourceBundle=new MCADServerResourceBundle(context.getSession().getLanguage());
		String errMsg = serverResourceBundle.getString("mcadIntegration.Server.Message.IEF020520657",messageTokens);
		return errMsg;
    }

    /**
     * Returns a relationship that a given Document Holder connects an object
     * to the document
     *
     * @param busid String value of the BusID identifying the Project Space object
     * @throws Exception if the operation fails
     */
    public String getRelationshipToBrowse(Context context, String[] args) throws Exception
    {
        String sRelationshipName = null;

        String busId = args[0];
        String typeName = args[1];


        //from the IEFBuildFolderStructure the symbolic type name is passed,
        //but from the servlet the actual type name is passed take care of both
        //the cases
        String actualTypeName = PropertyUtil.getSchemaProperty(context, typeName);
        if((actualTypeName != null) && !"".equals(actualTypeName))
            typeName = actualTypeName;

        //Book does not implement the Document Holder
        String typeBook			= MCADMxUtil.getActualNameForAEFData(context, "type_Book");
        String typeWorkspaceVault	= MCADMxUtil.getActualNameForAEFData(context, "type_ProjectVault");
        String typeWorkspace		=  MCADMxUtil.getActualNameForAEFData(context, "type_Project");
        String typeFeature			=  MCADMxUtil.getActualNameForAEFData(context, "type_ConfigurableFeature");
        String typeRequirement		=  MCADMxUtil.getActualNameForAEFData(context, "type_Requirement");
        String typeTask			=  MCADMxUtil.getActualNameForAEFData(context, "type_Task");
        String typePart			=  MCADMxUtil.getActualNameForAEFData(context, "type_Part");
        String typePartFamily		=  MCADMxUtil.getActualNameForAEFData(context, "type_PartFamily");
        String typeECO				=  MCADMxUtil.getActualNameForAEFData(context, "type_ECO");
        String typeECR				=  MCADMxUtil.getActualNameForAEFData(context, "type_ECR");        
        //if(typeName.equals("Book"))
        if(typeName.equals(typeBook))
        {
            sRelationshipName = "relationship_HasDocuments";
        }
        else if(typeName.equals(typeWorkspaceVault))
        {
            String fromType = getFolderContainerType(context, busId);
            //the check for fromType only can check if there is a folder right below the workspace/project
            //but if there are sub-folders? then this will not work. This is a bug need to check for
            //a bean which can give me the top most object type
            if(fromType != null && !"".equals(fromType) && (fromType.equals(typeWorkspace) ||
                      fromType.equals(typeWorkspaceVault)))
            {
                sRelationshipName = "relationship_VaultedDocuments,relationship_SubVaults";
            }
            else
            {
                sRelationshipName = "relationship_VaultedDocumentsRev2,relationship_SubVaults";
            }
        }
        else if(typeName.equals("Library"))
        {
            sRelationshipName = "relationship_HasBookshelves";
        } 
        else if(typeName.equals(typeFeature) || typeName.equals("type_ConfigurableFeature"))        	
        {
            sRelationshipName = "relationship_FeaturesSpecification";
        } 
        else if(typeName.equals(typeRequirement) || typeName.equals("type_Requirement"))
        {
            sRelationshipName = "relationship_RequirementSpecification";
        } 
        else if(typeName.equals(typeTask) || typeName.equals("type_Task"))
        {
            sRelationshipName = "relationship_TaskDeliverable";
        } 
        else if(isTypeOf(context, typeName, PropertyUtil.getSchemaProperty(context, "type_Builds")))
        {
            sRelationshipName = "relationship_BuildSpecification";
        }
        else if(typeName.equals("BookShelf"))
        {
            sRelationshipName = "relationship_HasBooks";
        }
        else if(typeName.equals(typePart) || typeName.equals(typePartFamily) || typeName.equals(typeECO) || typeName.equals(typeECR))
        {
            sRelationshipName = "relationship_PartSpecification,relationship_ReferenceDocument";
        }
        else if(isTypeOf(context, typeName, PropertyUtil.getSchemaProperty(context, "type_Products")))//if(typeName.equals("type_SoftwareProduct") || typeName.equals("Software Product"))
        {
            sRelationshipName = "relationship_ProductSpecification";
        }
        else
        {
            sRelationshipName = "relationship_ReferenceDocument";
        }

        return sRelationshipName;
    }

    /**
     * Returns a relationship that a given Document Holder connects an object
     * to the document
     *
     * @param busid String value of the BusID identifying the Project Space object
     * @throws Exception if the operation fails
     */
    public String getDocumentRelationship(Context context, String[] args) throws Exception
    {
        String sRelationshipName = null;
	
        sRelationshipName = getRelationshipToConnectFromGCO(context, args);
        
		return sRelationshipName;
    }

    public String getRelationshipToConnectFromGCO(Context context, String[] args) throws Exception
    {
        String relationshipName = null;
        //Read the relationship value from the GCO if it is not available from the request
        try
        {
            HashMap argsMap		  = (HashMap)JPO.unpackArgs(args);
			String integratioName = (String )argsMap.get("integrationName");
            HashMap   _GcoTable = (HashMap)argsMap.get("gcoTable");

			MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject)_GcoTable.get(integratioName);
            String typeName = (String) argsMap.get("type");
            String parentId = (String) argsMap.get("parentId");

            if(typeName.equals("type_ProjectVault"))
            {
                String fromType = getFolderContainerType(context, parentId);
                //the check for fromType only can check if there is a folder right below the workspace/project
                //but if there are sub-folders? then this will not work. This is a bug need to check for
                //a bean which can give me the top most object type
                if(fromType != null && !"".equals(fromType) &&
                        (fromType.equals("Workspace")))
                {
                    relationshipName = "relationship_VaultedDocuments";
                }
                else
                {
                    relationshipName = "relationship_VaultedDocumentsRev2";
                }
            }
            else
            {
                String sTypeRelationshipMappings = globalConfigObject.getCustomAttribute("MSOITypeRelationshipMappings");
                if(sTypeRelationshipMappings != null)
                {
                    StringTokenizer mappedTokens = new StringTokenizer(sTypeRelationshipMappings, "\n");
                    if(mappedTokens != null)
                    {
                        while(mappedTokens.hasMoreElements())
                        {
                            String mapping = (String)mappedTokens.nextElement();
                            StringTokenizer mappingTokens = new StringTokenizer(mapping, "|");
                            if(mappingTokens != null)
                            {
                                if(mappingTokens.hasMoreElements())
                                {
                                    String mappedTypeName = (String)mappingTokens.nextElement();
                                    String mappedRelValue = (String)mappingTokens.nextElement();
                                    if(typeName.equals(mappedTypeName.trim()))
                                    {
                                        relationshipName = mappedRelValue.trim();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
            MCADServerException.createException(e.getMessage(), e);
        }
        return relationshipName;
    }


    public Hashtable getDocumentRelationshipFromGCO(Context context, String[] args) throws Exception
    {

        Hashtable typeRelCommandTable		= null;
        Hashtable relCommandTable			= null;
        Hashtable retRelCmdTable			= null;
        String typeName						= args[1];
        String typeNavigationMappingValues	= args[2];
        String symbolicTypeName				= null;

        try
        {
            if(!(typeName.startsWith("type_")))
            {
                //get the symbolic type name
                symbolicTypeName = PropertyUtil.getSchemaProperty(context, typeName.trim());

            } else
            {
                symbolicTypeName = typeName.trim();
            }

            StringTokenizer mappedTokens = new StringTokenizer(typeNavigationMappingValues, "\n");
            if(mappedTokens != null)
            {
                typeRelCommandTable = new Hashtable();
                while(mappedTokens.hasMoreElements())
                {
                    relCommandTable = new Hashtable();
                    String mapping = (String)mappedTokens.nextElement();
                    StringTokenizer mappingTokens = new StringTokenizer(mapping, "|");
                    if(mappingTokens != null)
                    {
                        if(mappingTokens.hasMoreElements())
                        {
                            String mappedTypeName = (String)mappingTokens.nextElement();

                            if(mappedTypeName != null)
                                mappedTypeName = mappedTypeName.trim();

                            String mappedNavigationValues = (String)mappingTokens.nextElement();

                            if(mappedNavigationValues != null)
                                mappedNavigationValues = mappedNavigationValues.trim();

                            if(mappedNavigationValues.indexOf("@") >= 0)
                            {
                                if(mappedNavigationValues.startsWith("@"))
                                {
                                    mappedNavigationValues = mappedNavigationValues.substring(1).trim();
                                    relCommandTable.put("command", mappedNavigationValues);
                                    typeRelCommandTable.put(mappedTypeName, relCommandTable);
                                }
                                else
                                {
                                    StringTokenizer relMenuTokens = new StringTokenizer(mappedNavigationValues, "@");
                                    if(relMenuTokens != null)
                                    {
                                        if(relMenuTokens.hasMoreElements())
                                        {
                                            String relNames = (String)relMenuTokens.nextElement();
                                            String commandName = (String)relMenuTokens.nextElement();
                                            if(relNames != null)
                                                relCommandTable.put("relationship", relNames.trim());

                                            if(commandName != null)
                                                relCommandTable.put("command", commandName.trim());

                                            typeRelCommandTable.put(mappedTypeName, relCommandTable);
                                        }
                                    }
                                }
                            }
                            else
                            {
                                if(mappedNavigationValues.startsWith("menu_") && ((mappedNavigationValues.indexOf(",") == -1) && (mappedNavigationValues.indexOf("@") == -1) && (mappedNavigationValues.indexOf("|") == -1)))
                                {
                                    relCommandTable.put("menu", mappedNavigationValues.trim());
                                    typeRelCommandTable.put(mappedTypeName, relCommandTable);
                                }
                            }
                        }
                    }
                }
            }
            
            if(typeRelCommandTable != null && typeRelCommandTable.size() > 0)
            {
                //Look for the symbolicTypeName
                if(typeRelCommandTable.containsKey(symbolicTypeName))
                {
                    //Get the retRelCmdTable hashtable
                    retRelCmdTable = (Hashtable)typeRelCommandTable.get(symbolicTypeName);
                } else
                {
                    //Look for its super type
                    retRelCmdTable = lookForSuperTypeOf(context, typeRelCommandTable, symbolicTypeName);
                }
            }
        }
        catch(Exception ex)
        {
            MCADServerException.createException(ex.getMessage(), ex);
        }

        return retRelCmdTable;
    }

    private Hashtable lookForSuperTypeOf(Context context, Hashtable typeRelCommandTable, String symbolicTypeName) throws Exception
    {
        Hashtable retTable = new Hashtable();
        try
        {
            if(isTypeOf(context, symbolicTypeName, PropertyUtil.getSchemaProperty(context,"type_Products")))
                retTable = (Hashtable)typeRelCommandTable.get("type_Products");
            else if(isTypeOf(context, symbolicTypeName, PropertyUtil.getSchemaProperty(context, "type_Builds")))
                retTable = (Hashtable)typeRelCommandTable.get("type_Builds");
            else if(isTypeOf(context, symbolicTypeName, PropertyUtil.getSchemaProperty(context, "type_Requirement")))
                retTable = (Hashtable)typeRelCommandTable.get("type_Requirement");
            else if(isTypeOf(context, symbolicTypeName, PropertyUtil.getSchemaProperty(context, "type_Features")))
                retTable = (Hashtable)typeRelCommandTable.get("type_Features");
        }
        catch(Exception ex)
        {
            MCADServerException.createException(ex.getMessage(), ex);
        }
        return retTable;
    }
    
    private boolean isTypeOf(Context context, String symbolicTypeName, String rootType) throws Exception
    {
        Hashtable argumentsTable = new Hashtable(2);
        argumentsTable.put("Type",symbolicTypeName);
        argumentsTable.put("RootType",rootType);
        Boolean isTypeOf = (Boolean)JPO.invoke(context, "IEFUtil", null, "isTypeOf", JPO.packArgs(argumentsTable), Boolean.class);
        return isTypeOf.booleanValue();
    }

    /**
     * Returns the top level vault for the current vault. The current vault
     * is returned if it is the top vault.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param objectSelects optional list of selectables from this class
     * @return the top vault based on selectables
     * @throws FrameworkException if operation fails
     * @since AEF 9.5.1.0
     * @grade 0
     */
    public String getFolderContainerType(Context context, String busId) throws FrameworkException, MCADException
    {
        //Special case for Workspace and Project folder
        //both use the same type "type_ProjetVault" but use different
        //relationship to connect documents!!!
        //So for folders check where they are connected to
        //(either Project or Workspace or Workspace Vault (Folder) or
        //Project Vault(Vault))
        //based on the from type decide what relationship needs to be used
        com.matrixone.apps.domain.DomainObject domainObject = new com.matrixone.apps.domain.DomainObject();
        domainObject.setId(busId);

        //if fromType is null then it is a subfolder
        StringList vaultSelects = new StringList();
        vaultSelects.add(DomainObject.SELECT_ID);
        vaultSelects.add(DomainObject.SELECT_NAME);
        String topLevelVaultId = null;
        String  relDataVaults	= MCADMxUtil.getActualNameForAEFData(context, "relationship_ProjectVaults");
        // expand from this object
        MapList mapList = domainObject.getRelatedObjects(
                context,                   // context.
                DomainObject.RELATIONSHIP_SUB_VAULTS,   // rel filter.
                "*",                                    // type filter.
                vaultSelects,             // business object selectables.
                null,                      // relationship selectables.
                true,                      // expand to direction.
                false,                     // expand from direction.
                (short) 0,                 // level
                null,                      // object where clause
                null);                     // relationship where clause

        int size = mapList.size();
        if (size > 0)
        {
            //last map in the maplist is the toplevel parent
            size--;
            topLevelVaultId = (String) ((Map) mapList.get(size)).get(DomainObject.SELECT_ID);
        }
        else
        {
            //the current vault is the toplevel vault
            topLevelVaultId = busId;
        }

        //check what is connected to the topLevelVault? Project or Workspace?
        domainObject.setId(topLevelVaultId);
        StringList busSelect = new StringList();
        busSelect.add("to[" + relDataVaults + "].from.type");
        
        Map infoMap = domainObject.getInfo(context, busSelect);
        //for folder inside a workspace/project
        String fromType = (String) infoMap.get("to[" + relDataVaults + "].from.type");        
        return fromType;
    }
    
    private String getBusId(Context context, String type, String name, String revision) throws Exception
    {
        HashMap argsMap = new HashMap(3);
        argsMap.put("type", type);
        argsMap.put("name",name);
        argsMap.put("rev", revision);
        
        return (String)JPO.invoke(context, "IEFUtil", null, "getBusId", JPO.packArgs(argsMap), String.class); 
    }

    /**
     * Revise a document object (Major object)
     * Generates XML string representing the Project Space.
     *
     * @param busid String value of the BusID identifying the Project Space object
     * @throws Exception if the operation fails
     */
    public String lockActiveVersion(Context context, String[] args) throws Exception
    {
        String xmlOutput = "";
        boolean isCurrentlyLocked = false;
        //default action is lock
        String action = "lock";
        boolean isLockAction = true;
        String lang = args[0];
        String noDocumentToLock = null;
        String noDocumentToUnLock = null;
        String documentLocked = null;
        String documentUnlocked = null;
        String documentNotLocked = null;

        try
        {
            ContextUtil.startTransaction(context, true);

            //regional language used by the CSE
            String type = args[1];
            String name = args[2];
            String rev = args[3];
            if(args.length == 5)
            {
                action = args[4];
            }

            if(action.equals("unlock"))
            {
                isLockAction = false;
            }

            noDocumentToLock = i18nNow.getI18nString("mcadIntegration.Server.Message.NoDocumentToLock",  "iefStringResource", lang);
            noDocumentToUnLock = i18nNow.getI18nString("mcadIntegration.Server.Message.NoDocumentToUnLock",  "iefStringResource", lang);
            documentLocked = i18nNow.getI18nString("mcadIntegration.Server.Message.DocumentLockedSuccessfully",  "iefStringResource", lang);
            documentUnlocked = i18nNow.getI18nString("mcadIntegration.Server.Message.DocumentUnlockedSuccessfully",  "iefStringResource", lang);
            documentNotLocked = i18nNow.getI18nString("mcadIntegration.Server.Message.NoLockError",  "iefStringResource", lang);

            String oid = getBusId(context,args[1],args[2],args[3]);

            StringList busSelects = new StringList();
            busSelects.add(DomainObject.SELECT_ID);
            busSelects.add(DomainConstants.SELECT_LOCKED);
            busSelects.add(DomainConstants.SELECT_FORMAT_HASFILE);
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION_ID);
            busSelects.add(CommonDocument.SELECT_IS_VERSION_OBJECT);

            DomainObject domainObject1 = iefGetBusinessObject(context, oid);
            
            Map docMap = (Map) domainObject1.getInfo(context, busSelects);

            //Check whether the object has any versions. Is it versionable.
            String versionId = null;
            if(isVersionable(context, oid))
            {
                StringList versionIdList = new StringList();
                try
                {
                    if("false".equalsIgnoreCase((String) docMap.get(CommonDocument.SELECT_IS_VERSION_OBJECT) ))
                    {
                        versionId = (String) docMap.get(CommonDocument.SELECT_ACTIVE_FILE_VERSION_ID);
                    }
                    else
                    {
                        versionId = (String) docMap.get(DomainObject.SELECT_ID);
                        isCurrentlyLocked = new Boolean((String) docMap.get(DomainConstants.SELECT_LOCKED)).booleanValue();
                        if(! isCurrentlyLocked && action.equals("unlock") )
                        {
                            ContextUtil.abortTransaction(context);
                            return documentNotLocked;
                        }

                    }
                } catch (ClassCastException ex)
                {
                    if("false".equalsIgnoreCase((String) docMap.get(CommonDocument.SELECT_IS_VERSION_OBJECT)))
                    {
                        versionIdList = (StringList) docMap.get(CommonDocument.SELECT_ACTIVE_FILE_VERSION_ID);
                        //if the size is 1 then it is not a multifile document
                        if(versionIdList != null && versionIdList.size() == 1)
                            versionId = (String) versionIdList.get(0);
                        
                        Hashtable argumentTable = new Hashtable(1);
                        argumentTable.put("id", versionId);
                        
                        DomainObject domainObject2 = iefGetBusinessObject(context, versionId);
                        
                        Map docMap2 = (Map) domainObject2.getInfo(context, busSelects);
                        isCurrentlyLocked = new Boolean((String) docMap2.get(DomainConstants.SELECT_LOCKED)).booleanValue();
                        if(! isCurrentlyLocked && action.equals("unlock") )
                        {
                            ContextUtil.abortTransaction(context);
                            return documentNotLocked;
                        }


                    }
                }
            }
            else
            {
                //The Object is Non-Versionable so lock or unlock the major object
                versionId = oid;
                if("false".equalsIgnoreCase((String)docMap.get(DomainConstants.SELECT_FORMAT_HASFILE)))
                    action = "nofile";
            }

            if(versionId != null && !"".equals(versionId))
            {
                DomainObject domainObject = (DomainObject)DomainObject.newInstance(context,versionId);
                if(action.equals("lock"))
                {
                    domainObject.lock(context);
                }
                else
                {
                    domainObject.unlock(context);
                }
            }
            else
            {
                action = "nofile";
            }
            ContextUtil.commitTransaction(context);
            if(action.equals("nofile"))
            {
                if(isLockAction)
                {
                    xmlOutput = noDocumentToLock;
                }
                else
                {
                    xmlOutput = noDocumentToUnLock;
                }
            }
            else if(action.equals("lock"))
            {
                xmlOutput = documentLocked;
            }
            else
            {
                xmlOutput = documentUnlocked;
            }
        }
        catch (Exception ex)
        {
            ContextUtil.abortTransaction(context);
            MCADServerException.createException(ex.getMessage(), ex);
        }
        return xmlOutput;
    }

    /**
     * The following method will be called to delete a file from non-versionable CDM object which has multiple files.
     *
     * @param context
     * @param CommonDocument object
     * @param filename String value of the filename
     *
     */
    public boolean deleteFileFromNonVersionableObject(Context context, DomainObject domainObject, String objectId, String fileName)
    {
        try
        {
            domainObject.open(context);
            FormatList formatList = domainObject.getFormats(context);
            if (formatList != null && formatList.size() > 0)
            {
                FormatItr formatItr = new FormatItr(formatList);
                while(formatItr.next())
                {
                    boolean isFileDeleted = false;
                    String format = formatItr.obj().getName();
                    FileList fileList = domainObject.getFiles(context, format);
                    FileItr fileItr = new FileItr(fileList);

                    while (fileItr.next())
                    {
                        String filename  = fileItr.obj().getName();
                        if(fileName.equalsIgnoreCase(filename))
                        {
                        	MqlUtil.mqlCommand(context,"delete bus $1 format $2 file $3",objectId,format,fileName);
                            isFileDeleted = true;
                            break;
                        }
                    }

                    if(isFileDeleted)
                        break;
                }
            }
            domainObject.close(context);
        }
        catch(Exception ex)
        {
            return false;
        }
        
        return true;
    }

    public String iefGetCDMMajorObject(Context context, String busId) throws Exception
    {
        String majBusId = busId;
        try
        {
            MCADMxUtil _util = new MCADMxUtil(context, null, new IEFGlobalCache());
            if(isVersionable(context, busId))
            {
                //String sRelName = "Active Version";
                String sRelName = MCADMxUtil.getActualNameForAEFData(context,"relationship_ActiveVersion");
                BusinessObjectList list = _util.getRelatedBusinessObjects(context,new BusinessObject(busId),sRelName,"to");
                
                if(list != null && list.size() > 0)
                {
                    BusinessObjectItr itr = new BusinessObjectItr(list);
                    while(itr.next())
                    {
                        majBusId = itr.obj().getObjectId(context);
                    }
                }
                else
                {
                    //Check whether the object is connected to major with the relationship "Latest Version".
                    //This is the case in which the data is either migrated and any intermediate
                    //version is finalized before migration. The two relationships are pointing to
                    //two different minors
                    //sRelName = "Latest Version";
                    sRelName = MCADMxUtil.getActualNameForAEFData(context,"relationship_LatestVersion");
                    list = _util.getRelatedBusinessObjects(context,new BusinessObject(busId),sRelName,"to");
                    
                    if(list != null && list.size() > 0)
                    {
                        BusinessObjectItr itr = new BusinessObjectItr(list);
                        while(itr.next())
                        {
                            majBusId = itr.obj().getObjectId(context);
                        }
                    }
                }
            }
        }
        catch(Exception me)
        {
            MCADServerException.createException(me.getMessage(), me);
        }

        return majBusId;
    }

    /**
     * The following method will be called from ief IEF servlet.
     * It is a wrapper over the method "isVersionable()"
     *
     * @param busid String value of the BusID
     * @throws Exception if the operation fails
     */
    public Boolean iefIsVersionable(Context context, String[] params) throws Exception
    {
        boolean retVal = true;
        try
        {
	    Hashtable argsTable     = (Hashtable) JPO.unpackArgs(params);
	    String busId            = (String) argsTable.get("busId");

            MCADMxUtil _util = new MCADMxUtil(context, null, new IEFGlobalCache());
            if(_util.isCDMInstalled(context))
                retVal = isVersionable(context, busId);
            else
                retVal =  false;
        }
        catch(Exception ex)
        {
            MCADServerException.createException(ex.getMessage(), ex);
        }
        
        return new Boolean(retVal);
    }

    public Boolean isVersionable(Context context, String[] args) throws Exception
    {
        Hashtable argsTable     = (Hashtable) JPO.unpackArgs(args);
        String busId            = (String) argsTable.get("busId");
        boolean isVersionable   = isVersionable(context, busId);
        
        return new Boolean(isVersionable);
    }
    
    /**
     * Check whether versioning is allowed or not for the given object id
     * Return true or false
     *
     * @param busid String value of the BusID
     * @throws Exception if the operation fails
     */
    public boolean isVersionable(Context context, String busId) throws Exception
    {
        boolean isVersioable = true;
        try
        {
            //un-comment the following code after the CDM bean supports Non-Versioning probably in v10.6
            //boolean isVersioable = CommonDocument.allowFileVersioning(context, busId);

            //Throw away the following code after the CDM bean supports Non-Versioning probably in v10.6
            MCADMxUtil _util = new MCADMxUtil(context, null, new IEFGlobalCache());

            if(_util.isCDMInstalled(context))
                isVersioable = checkVersionable(context, busId);
            else
                isVersioable = false;
        }
        catch(Exception ex)
        {
            MCADServerException.createException(ex.getMessage(), ex);
        }
        
        return isVersioable;
    }

//  Throw away the following code after the CDM bean supports Non-Versioning probably in v10.6

    public boolean checkVersionable(Context context, String objectId) throws Exception
    {
        boolean isVersionable = true;
        try
        {
            DomainObject object = DomainObject.newInstance(context, objectId);
            String type         = object.getInfo(context, DomainConstants.SELECT_TYPE);
            isVersionable       = checkVersionableType(context, type);
        }
        catch (Exception ex)
        {
           MCADServerException.createException(ex.getMessage(), ex);
        }
        
        return isVersionable;
    }

    public boolean checkVersionableType(Context context, String type) throws Exception
    {
        try
        {
            String property = PropertyUtil.getAdminProperty(context, "Type", type, "DISALLOW_VERSIONING");
            if ( property != null && "true".equalsIgnoreCase(property) )
            {
                return false;
            }
        }
        catch (Exception ex)
        {
            MCADServerException.createException(ex.getMessage(), ex);
        }
        return true;
    }

    /**
     * This method is used to get the list of files in
     * master (i.e. document holder) object
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.5
     * @grade 0
     */

    public Object getNonVersionableFiles(Context context, String[] args) throws Exception
    {
        MapList fileMapList = new MapList();
        try
        {
            HashMap programMap         = (HashMap) JPO.unpackArgs(args);
            String  objectId     = (String) programMap.get("objectId");
            DomainObject object  = DomainObject.newInstance(context, objectId);

            //Added to make a single database call to
            StringList selectList = new StringList(12);
            selectList.add(DomainConstants.SELECT_ID);
            selectList.add(DomainConstants.SELECT_FILE_NAME);
            selectList.add(DomainConstants.SELECT_FILE_FORMAT);
            selectList.add(DomainConstants.SELECT_FILE_SIZE);
            selectList.add(DomainConstants.SELECT_LOCKED);
            selectList.add(DomainConstants.SELECT_LOCKER);

            selectList.add(CommonDocument.SELECT_HAS_CHECKOUT_ACCESS);
            selectList.add(CommonDocument.SELECT_HAS_CHECKIN_ACCESS);
            selectList.add(CommonDocument.SELECT_HAS_LOCK_ACCESS);
            selectList.add(CommonDocument.SELECT_SUSPEND_VERSIONING);
            selectList.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
            // get the Master Object data
            Map objectMap = object.getInfo(context,selectList);
            StringList fileList = (StringList)objectMap.get(DomainConstants.SELECT_FILE_NAME);
            StringList formatList = (StringList)objectMap.get(DomainConstants.SELECT_FILE_FORMAT);
            StringList fileSizeList = (StringList)objectMap.get(DomainConstants.SELECT_FILE_SIZE);

            Iterator fileItr = fileList.iterator();
            Iterator formatItr = formatList.iterator();
            Iterator fileSizeItr = fileSizeList.iterator();
            String file = "";
            String format = "";
            String fileSize   = "";
            String canCheckout = "false";
            Object obj = objectMap.get(CommonDocument.SELECT_HAS_CHECKOUT_ACCESS);
            if(obj != null) canCheckout = (String) obj;

            String canCheckin  = "false";
            obj = objectMap.get(CommonDocument.SELECT_HAS_CHECKIN_ACCESS);
            if(obj != null) canCheckin = (String) obj;

            String canLock     = "false";
            obj = objectMap.get(CommonDocument.SELECT_HAS_LOCK_ACCESS);
            if(obj != null) canLock = (String) obj;

            String locked     = (String) objectMap.get(DomainConstants.SELECT_LOCKED);
            String locker     = (String) objectMap.get(DomainConstants.SELECT_LOCKER);

            while( fileItr.hasNext() && formatItr.hasNext() && fileSizeItr.hasNext() )
            {
                file = (String)fileItr.next();
                format = (String)formatItr.next();
                fileSize = (String)fileSizeItr.next();
                if ( file != null && !"".equals(file) )
                {
                    Map fileMap = new HashMap();
                    fileMap.put("id", objectId+"~"+file+"~"+format);
                    fileMap.put("objectId", objectId);
                    fileMap.put(DomainConstants.SELECT_FILE_NAME, file);
                    fileMap.put(DomainConstants.SELECT_FILE_FORMAT, format);
                    fileMap.put(DomainConstants.SELECT_FILE_SIZE, fileSize);
                    Object vObject = objectMap.get(CommonDocument.SELECT_HAS_CHECKOUT_ACCESS);
                    if(vObject != null)
                        fileMap.put((String)vObject, canCheckout);

                    vObject = objectMap.get(CommonDocument.SELECT_HAS_CHECKIN_ACCESS);
                    if(vObject != null)
                        fileMap.put((String)vObject, canCheckin);

                    vObject = objectMap.get(CommonDocument.SELECT_HAS_LOCK_ACCESS);
                    if(vObject != null)
                        fileMap.put((String)vObject, canLock);

                    fileMap.put(DomainConstants.SELECT_LOCKED, locked);
                    fileMap.put(DomainConstants.SELECT_LOCKER, locker);
                    fileMapList.add(fileMap);
                }
            }
        }
        catch (Exception ex)
        {
            MCADServerException.createException(ex.getMessage(), ex);
        }
        
        return fileMapList;
    }
}

