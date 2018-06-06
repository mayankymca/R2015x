//IEFCDMSupport.java

//This program gives additional support if the CDM is installed.

//Copyright (c) 2002 MatrixOne, Inc.
//All Rights Reserved
//This program contains proprietary and trade secret information of
//MatrixOne, Inc.  Copyright notice is precautionary only and does
//not evidence any actual or intended publication of such program.

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import matrix.db.BusinessType;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.common.Subscribable;
import com.matrixone.apps.common.SubscriptionManager;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkException;
/**
 * The <code>IEFCDMSupport</code> class represents the JPO for
 * obtaining additional support if the CDM is installed
 *
 * @since AEF 10.6.1.0 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class IEFCDMSupport_mxJPO
{
    /**
     * Constructs a new IEFCDMSupport JPO object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args an array of String arguments for this method
     * @throws Exception if the operation fails
     * @since AEF 10.6.1.0
     */
    public IEFCDMSupport_mxJPO ()
    {
        init();
    }

    /**
     * Constructs a new IEFCDMSupport JPO object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args an array of String arguments for this method
     * @throws Exception if the operation fails
     * @since AEF 10.6.1.0
     */
    public IEFCDMSupport_mxJPO (Context context, String[] args)
    {
        init();
    }

    private void init()
    {
    }

    /**
     * Generic function to add the CDM related variables
     */
    public void iefAddElement(StringList list, String element)
    {
        if (element != null && element.length() != 0 && !"unknown".equalsIgnoreCase(element))
        {
            list.addElement(element);
        }
    }

    /**
     * Generic function to add the CDM related variables
     */
    public Object iefGetElement(Map map, String element)
    {
        Object obj = null;
        if (element != null && element.length() != 0 && !"unknown".equalsIgnoreCase(element))
        { 
            obj =  map.get(element);
        }
        return obj;
    }

    /**
     * Generic function to remove the CDM related variables
     */
    public void iefRemoveElement(Map map, String element)
    {
        if (element != null && element.length() != 0 && !"unknown".equalsIgnoreCase(element))
        { 
            map.remove(element);
        }
    }

    public String iefGetParentType(Context context, String[] args) throws Exception
    {
        Hashtable argumentsTable = (Hashtable) JPO.unpackArgs(args);
        String type              = (String) argumentsTable.get("type");

        return iefGetParentType(context, type);
    }

    /**
     * Generic function to retrieve the parent type
     */
    public String iefGetParentType(Context context, String type) throws Exception
    { 
        String strParentType = "";
        String s1 = type;

        BusinessType businesstype = new BusinessType(s1, context.getVault());
        String s2;
        for(s2 = businesstype.getParent(context); s2 != null && !"".equals(s2) && !s2.equals(CommonDocument.TYPE_DOCUMENTS); s2 = businesstype.getParent(context))
        {
            s1 = s2;
            businesstype = new BusinessType(s1, context.getVault());
        }

        if(s2 != null && !"".equals(s2))
            strParentType =  s2;
        else
            strParentType = s1;

        return strParentType;
    }

    public DomainObject iefGetBusinessObject(Context context, String [] args) throws Exception
    { 

        Hashtable argsTable = (Hashtable) JPO.unpackArgs(args);
        String id           = (String)argsTable.get("id");

        return iefGetBusinessObject(context, id);
    }

    /**
     * Generic function to get the CommonDocument BO if CDM is installed or DomainObject BO if CDM is NOT installed
     */
    public DomainObject iefGetBusinessObject(Context context, String strMatrixAdmin) throws Exception
    { 
        DomainObject domainBus = null;
        try
        {
            domainBus = DomainObject.newInstance(context, strMatrixAdmin);
        }catch(Exception e)
        {
            System.out.println("[IEFCDMSupport.iefGetBusinessObject] EXCEPTION : " + e.getMessage());
        }

        return (CommonDocument)domainBus;
    }

	public Boolean iefAppendIsVersionObject(Context context, String [] args)
    {
        //CDM Supported
        return new Boolean(iefAppendIsVersionObject());
    }

    public boolean iefAppendIsVersionObject()
    {
        return true;
    }

    public Boolean iefIsCDMSupported(Context context, String [] args)
    {
        //CDM Supported
        return new Boolean(iefIsCDMSupported());
    }

    public boolean iefIsCDMSupported()
    {
        //CDM Supported
        return true;
    }

    public DomainObject iefCreateAndConnect(Context context, String[] args) throws Exception
    {
    	 HashMap argumentsTable   = (HashMap)JPO.unpackArgs(args);
         String designObjType       = (String) argumentsTable.get("designObjType");
         String designObjName       = (String) argumentsTable.get("designObjName");
         String designObjRevision   = (String) argumentsTable.get("designObjRevision");
         String designObjPolicy     = (String) argumentsTable.get("designObjPolicy");
         String s1 					= (String) argumentsTable.get("s1");
         String vaultName 			= (String) argumentsTable.get("vaultName");
         String s2 					= (String) argumentsTable.get("s2");
         String s3 					= (String) argumentsTable.get("s3");
         DomainObject docHolder 	= (DomainObject) argumentsTable.get("docHolder");
         String relationship		= (String) argumentsTable.get("relationship");
         String s4 					= (String) argumentsTable.get("s4");
         Map attrNameValMap 		= (Map) argumentsTable.get("attrNameValMap");
         DomainObject domainObj 	= (DomainObject) argumentsTable.get("domainObj");
         String lang				= (String) argumentsTable.get("lang");
         
         return iefCreateAndConnect(context, designObjType, designObjName, designObjRevision, designObjPolicy, s1, vaultName, s2, s3, docHolder, relationship, s4, attrNameValMap, domainObj, lang);
    }
    
    public DomainObject iefCreateAndConnect(Context context, String designObjType, String designObjName, String designObjRevision, String designObjPolicy, String s1, String vaultName, String s2, String s3, DomainObject docHolder, String relationship, String s4, Map attrNameValMap, DomainObject domainObj, String lang)
    {
        DomainObject obj = null;
        try
        {
            obj = ((CommonDocument)domainObj).createAndConnect(context, designObjType, designObjName, designObjRevision, designObjPolicy, "", vaultName, s2, null, docHolder, relationship, null, (Map)attrNameValMap);
        }
        catch(FrameworkException fe)
        {
            System.out.println("[IEFCDMSupport.iefCreateAndConnect] EXEPTION : " + fe.getMessage());
        }
        return obj;
    }
    
    public String iefFireSubscriptionEvent(Context context, String [] args) throws Exception
    {
        Hashtable argsTable = (Hashtable)JPO.unpackArgs(args);
        String busId        = (String)argsTable.get("busId");
        String strEvent     = (String)argsTable.get("event");
        Integer itrate      = (Integer)argsTable.get("count");
        
        iefFireSubscriptionEvent(context, busId, strEvent, itrate.intValue());
        
        return "";
    }

    public void iefFireSubscriptionEvent(Context context, String busId, String strEvent, int itrate)
    {
        //Fire Subscription Event(s)
        try
        {
            Subscribable parentObject = (Subscribable)DomainObject.newInstance(context, busId);
            SubscriptionManager subscriptionMgr = parentObject.getSubscriptionManager();
            do
            {
                subscriptionMgr.publishEvent(context, strEvent, busId);
                itrate--;
            }
            while(itrate > 0);
        }catch(FrameworkException fe)
        {
            System.out.println("[IEFCDMSupport.iefFireSubscriptionEvent] EXEPTION : " + fe.getMessage());
        }
    }

    public Object iefGetDocuments(Context context, String [] args) throws Exception
    {
        Map argsMap = (Map) JPO.unpackArgs(args);
        return iefGetDocuments(context, argsMap);
    }

    public Object iefGetDocuments(Context context, Map argsMap) throws Exception
    {
        return JPO.invoke(context, "emxCommonDocumentUI", null, "getDocuments", JPO.packArgs(argsMap), Object.class);
    }

    public void iefDeleteVersion(Context context, DomainObject domainObject, String[] oids, boolean bVal) throws FrameworkException
    {
        ((CommonDocument)domainObject).deleteVersion(context, oids, bVal);  
    }


    public void iefDeleteDocuments(Context context, DomainObject domainObject, String[] oids) throws FrameworkException
    {
        ((CommonDocument)domainObject).deleteDocuments(context, oids);
    }

    public void iefRevise(Context context, DomainObject domainObject, boolean copyFiles) throws FrameworkException
    {
        ((CommonDocument)domainObject).revise(context, copyFiles);
    }

    public String iefCreateVersion(Context context, String[] args) throws Exception
    {
    	Hashtable argsTable = (Hashtable)JPO.unpackArgs(args);
    	DomainObject object = (DomainObject)argsTable.get("domainObject");
        String s1           = (String)argsTable.get("s1");
        String s2           = (String)argsTable.get("s2");
        Map attrMap			= (Map)argsTable.get("attrMap");
        
        return iefCreateVersion(context, object, s1, s2, attrMap);    	
    }
    
    public String iefCreateVersion(Context context, DomainObject object, String s1, String s2, Map attrMap) throws FrameworkException
    {
        return ((CommonDocument)object).createVersion(context, s1, s2, attrMap);
    }

    public boolean checkAccess(Context context, String[] args) throws MatrixException
    {
        Boolean jpoOutput = (Boolean) JPO.invoke(context, "emxMSProjectIntegration", null, "checkAccess", args, Boolean.class); 

        boolean editFlag = jpoOutput.booleanValue();
        return editFlag;
    }
    //To show the context menus "Edit in MSP" and "View in MSP" in CSE file dialog and windows explorer
    public boolean checkAccess(Context context, String busIdToCheck, short accessConstantModify) throws Exception
    {
        HashMap argsMap = new HashMap(2);
        argsMap.put("busIdToCheck", busIdToCheck);
        argsMap.put("accessConstantModify", Short.toString(accessConstantModify));
        Boolean jpoOutput = (Boolean) JPO.invoke(context, "emxMSProjectIntegration", null, "checkAccess", JPO.packArgs(argsMap), Boolean.class); 

        boolean editFlag = jpoOutput.booleanValue();
        return editFlag;
    }
}
