
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import matrix.db.Context;
import matrix.util.StringList;
import com.matrixone.apps.domain.DomainAccess;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

public class emxCommonDocumentOwnershipMigrationBase_mxJPO extends emxCommonMigration_mxJPO
{

    private static final long serialVersionUID = -5029177381386073045L;
    static private  Map<String, Integer> _accessMasksConstMapping = new HashMap<String, Integer>(35);
    
    public static String relationship_ObjectRoute = "";
    public static String relationship_ReferenceDocument = "";
    public static String relationship_ResolvedTo = "";
    public static String relationship_MeetingAttachments = "";
    public static String relationship_TaskDeliverable = "";
    public static String relationship_MessageAttachments = "";
    public static String relationship_Issue = "";

    public static String relationship_ObjectRoute_id = "";
    public static String relationship_ReferenceDocument_id = "";
    public static String relationship_ResolvedTo_id = "";
    public static String relationship_MeetingAttachments_id = "";
    public static String relationship_TaskDeliverable_id = "";
    public static String relationship_MessageAttachments_id = "";
    public static String relationship_Issue_id = "";
    
    public static String relationship_ObjectRoute_type = "";
    public static String relationship_ReferenceDocument_type = "";
    public static String relationship_ResolvedTo_type = "";
    public static String relationship_MeetingAttachments_type = "";
    public static String relationship_TaskDeliverable_type = "";
    public static String relationship_MessageAttachments_type = "";
    public static String relationship_Issue_type = "";
    

    public emxCommonDocumentOwnershipMigrationBase_mxJPO(Context context, String[] args) throws Exception {
      super(context, args);
    }

    public static void init(Context context) throws FrameworkException
    {
         relationship_ObjectRoute = PropertyUtil.getSchemaProperty(context, "relationship_ObjectRoute");
         relationship_ReferenceDocument = PropertyUtil.getSchemaProperty(context, "relationship_ReferenceDocument");
         relationship_ResolvedTo = PropertyUtil.getSchemaProperty(context, "relationship_ResolvedTo");
         relationship_MeetingAttachments = PropertyUtil.getSchemaProperty(context, "relationship_MeetingAttachments");
         relationship_TaskDeliverable = PropertyUtil.getSchemaProperty(context, "relationship_TaskDeliverable");
         relationship_MessageAttachments = PropertyUtil.getSchemaProperty(context, "relationship_MessageAttachments");
         relationship_Issue = PropertyUtil.getSchemaProperty(context, "relationship_Issue");
         
         relationship_ObjectRoute_id = "to["+ relationship_ObjectRoute +"].from.id";
         relationship_ReferenceDocument_id = "to["+ relationship_ReferenceDocument +"].from.id";
         relationship_ResolvedTo_id = "to["+ relationship_ResolvedTo +"].from.id";
         relationship_MeetingAttachments_id = "to["+ relationship_MeetingAttachments +"].from.id";
         relationship_TaskDeliverable_id = "to["+ relationship_TaskDeliverable +"].from.id";
         relationship_MessageAttachments_id = "to["+ relationship_MessageAttachments +"].from.id";
         relationship_Issue_id = "to["+ relationship_Issue +"].from.id";
         
        
         relationship_ObjectRoute_type = "to["+ relationship_ObjectRoute +"].from.type";
         relationship_ReferenceDocument_type = "to["+ relationship_ReferenceDocument +"].from.type";
         relationship_ResolvedTo_type = "to["+ relationship_ResolvedTo +"].from.type";
         relationship_MeetingAttachments_type = "to["+ relationship_MeetingAttachments +"].from.type";
         relationship_TaskDeliverable_type = "to["+ relationship_TaskDeliverable +"].from.type";
         relationship_MessageAttachments_type = "to["+ relationship_MessageAttachments +"].from.type";
         relationship_Issue_type = "to["+ relationship_Issue +"].from.type";    
    }

    public void migrateObjects(Context context, StringList objectList) throws Exception
    {     
        init(context);
        StringList objectSelects = new StringList(15);
        objectSelects.addElement("id");
        objectSelects.addElement("name");
        objectSelects.addElement("type");
        objectSelects.addElement("revision");
        
        objectSelects.addElement(relationship_ObjectRoute_id);
        objectSelects.addElement(relationship_ReferenceDocument_id);
        objectSelects.addElement(relationship_ResolvedTo_id);
        objectSelects.addElement(relationship_MeetingAttachments_id);
        objectSelects.addElement(relationship_TaskDeliverable_id);
        objectSelects.addElement(relationship_MessageAttachments_id);
        objectSelects.addElement(relationship_Issue_id);

        objectSelects.addElement(relationship_ObjectRoute_type);
        objectSelects.addElement(relationship_ReferenceDocument_type);
        objectSelects.addElement(relationship_ResolvedTo_type);
        objectSelects.addElement(relationship_MeetingAttachments_type);
        objectSelects.addElement(relationship_TaskDeliverable_type);
        objectSelects.addElement(relationship_MessageAttachments_type);
        objectSelects.addElement(relationship_Issue_type);
    
     
        String[] oidsArray = new String[objectList.size()];
        oidsArray = (String[])objectList.toArray(oidsArray);        
             
        MapList mapList = DomainObject.getInfo(context, oidsArray, objectSelects);
        try{
        ContextUtil.pushContext(context);
        
                
        Iterator itr = mapList.iterator();
        while( itr.hasNext())
        {
            try{
                Map valueMap = (Map) itr.next();
                
                String strObjId = (String)valueMap.get("id");
                
                String strType = (String)valueMap.get("type");
                String strName = (String)valueMap.get("name");
                String strRevision = (String)valueMap.get("revision");
                String strParentId = (String)valueMap.get(relationship_ObjectRoute_id);
                String strParentType = (String)valueMap.get(relationship_ObjectRoute_type); 
                
               // mqlLogRequiredInformationWriter("Checking  Ownership for Objects Connected with Object Route relationship............");
                updateDocumentOwnership(context, strObjId, strParentId,  strParentType, false);                
                                
                strParentId = (String)valueMap.get(relationship_ReferenceDocument_id);
                strParentType = (String)valueMap.get(relationship_ReferenceDocument_type);               
               // mqlLogRequiredInformationWriter("Checking  Ownership for Objects Connected with Reference Document relationship............");
                updateDocumentOwnership(context, strObjId, strParentId,  strParentType, false);
                 
                strParentId = (String)valueMap.get(relationship_Issue_id);
                strParentType = (String)valueMap.get(relationship_Issue_type);         
               // mqlLogRequiredInformationWriter("Checking  Ownership for Objects Connected with Issue relationship............");           
                updateDocumentOwnership(context, strParentId , strObjId,  strParentType, false);
                
                strParentId = (String)valueMap.get(relationship_ResolvedTo_id);
                strParentType = (String)valueMap.get(relationship_ResolvedTo_type);
               // mqlLogRequiredInformationWriter("Checking  Ownership for Objects Connected with Resolved To relationship............");                
                updateDocumentOwnership(context, strObjId, strParentId,  strParentType, true);

                strParentId = (String)valueMap.get(relationship_MeetingAttachments_id);
                strParentType = (String)valueMap.get(relationship_MeetingAttachments_type);                 
              //  mqlLogRequiredInformationWriter("Checking  Ownership for Objects Connected with Meeting Attachments relationship............"); 
                updateDocumentOwnership(context, strObjId, strParentId,  strParentType, true);
                
                               
                strParentId = (String)valueMap.get(relationship_MessageAttachments_id);
                strParentType = (String)valueMap.get(relationship_MessageAttachments_type);
                //mqlLogRequiredInformationWriter("Checking  Ownership for Objects Connected with Message Attachments relationship............" + strParentId);           
                updateDocumentOwnership(context, strObjId, strParentId,  strParentType, true); 
                
               
                strParentId = (String)valueMap.get(relationship_TaskDeliverable_id);
                strParentType = (String)valueMap.get(relationship_TaskDeliverable_type);
                //mqlLogRequiredInformationWriter("Checking  Ownership for Objects Connected with Task Delivarable relationship............");                
                updateDocumentOwnership(context, strObjId, strParentId,  strParentType, true);                
                 
                //Updating non migratedId's information
                checkAndWriteUnconvertedOID( strType+","+strName+","+strRevision+",Ownership Update is not required \n" , strObjId);
                              
                
            } catch (Exception ex) {
                mqlLogRequiredInformationWriter("Failed to update ownership ");
            }
        }
        
        } catch(Exception ex){        
        }finally{
         ContextUtil.popContext(context);
        }     
    }
    
    private void updateDocumentOwnership(Context context, String ObjectID, String strParentId, String  strParentType, boolean addComment) throws Exception{              
        
     String comment ="";
     if(addComment){
        comment ="Ownership Inheritance from " + strParentType;
     }
      
     if(!UIUtil.isNullOrEmpty(strParentType) && !UIUtil.isNullOrEmpty(strParentId)){
         mqlLogRequiredInformationWriter("Updating  Ownership on "+ ObjectID +" with parentOid "+ strParentId +"............"); 
         DomainAccess.createObjectOwnership(context, ObjectID, strParentId, comment, false);
         checkAndloadMigratedOids(ObjectID);
     }    
    }
    
    public void mqlLogRequiredInformationWriter(String command) throws Exception
    {
        super.mqlLogRequiredInformationWriter(command +"\n");
    }
    
    
    public void checkAndloadMigratedOids(String command) throws Exception
    {
     if(migratedOids.indexOf(command)<= -1){
        super.loadMigratedOids(command +"\n");
     }
        
    }
    public void checkAndWriteUnconvertedOID(String command, String ObjectId) throws Exception
    {
     if(migratedOids.indexOf(ObjectId)<= -1){
        super.writeUnconvertedOID(command, ObjectId);
     }
        
    }
    
}
