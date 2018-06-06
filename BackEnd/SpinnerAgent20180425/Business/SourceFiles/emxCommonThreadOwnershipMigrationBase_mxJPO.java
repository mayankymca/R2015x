
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

public class emxCommonThreadOwnershipMigrationBase_mxJPO extends emxCommonMigration_mxJPO
{

    private static final long serialVersionUID = -5029177381386073045L;
    //static private  Map<String, Integer> _accessMasksConstMapping = new HashMap<String, Integer>(35);
    
    public static String relationship_Thread = "";
    public static String relationship_Message = "";
   
    public static String WorskpsaceId = "";
    public static String ThreadId = "";
	public static String MessageId = "";
	
	public static String Worskpsace_type = "";
    public static String Message_type = "";

    public emxCommonThreadOwnershipMigrationBase_mxJPO(Context context, String[] args) throws Exception {
      super(context, args);
    }

    public static void init(Context context) throws FrameworkException
    {
         relationship_Thread = PropertyUtil.getSchemaProperty(context, "relationship_Thread");
         relationship_Message = PropertyUtil.getSchemaProperty(context, "relationship_Message");
                  
         WorskpsaceId = "to["+ relationship_Thread +"].from.id";
         MessageId = "from["+ relationship_Message +"].to.id";
         
         Worskpsace_type = "to["+ relationship_Thread +"].from.type";
         Message_type = "from["+ relationship_Message +"].to.type";
    }

    public void migrateObjects(Context context, StringList objectList) throws Exception
    {     
        init(context);
        StringList objectSelects = new StringList(15);
        objectSelects.addElement("id");
        objectSelects.addElement("name");
        objectSelects.addElement("type");        
        objectSelects.addElement("revision");
        
        objectSelects.addElement(WorskpsaceId);
        objectSelects.addElement(MessageId);
        
        objectSelects.addElement(Worskpsace_type);
        objectSelects.addElement(Message_type);
     
        String[] oidsArray = new String[objectList.size()];
        oidsArray = (String[])objectList.toArray(oidsArray);        
        MULTI_VALUE_LIST.add(MessageId);
        
        MapList mapList = DomainObject.getInfo(context, oidsArray, objectSelects);
        MULTI_VALUE_LIST.remove(MessageId);
        
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
	                
	                String strParentId = (String)valueMap.get(WorskpsaceId);
	                String strParentType = (String)valueMap.get(Worskpsace_type); 
	                	                
	                // mqlLogRequiredInformationWriter("Checking  Ownership for Objects Connected with Thread relationship............");                
	                updateDocumentOwnership(context, strObjId, strParentId,  strParentType, true);
	
	                Object strParentIdList = valueMap.get(MessageId);
	                 
	                if(strParentIdList instanceof String){
	                	updateDocumentOwnership(context, (String)strParentIdList, strObjId,  strType, true);
	                }else if(strParentIdList instanceof StringList){
	                	StringList objList = (StringList)strParentIdList;
	                	for(int i = 0; i< objList.size();i++){
	                		updateDocumentOwnership(context, (String)objList.get(i), strObjId,  strType, true);
	                		checkAndWriteUnconvertedOID( strType+","+strName+","+strRevision+",Ownership Update is not required \n" , strObjId);
	                	}
	                }	                
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
