
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

public class emxCommonIssueAssigneeOwnershipMigrationBase_mxJPO extends emxCommonMigration_mxJPO
{

    private static final long serialVersionUID = -5029177381386073045L;
    
    public static String relationship_AssignedIssue = "";
    public static String relationship_AssignedIssue_id = "";
    public static String accessName = "";  
    
    

    public emxCommonIssueAssigneeOwnershipMigrationBase_mxJPO(Context context, String[] args) throws Exception {
      super(context, args);
    }

    public static void init(Context context) throws FrameworkException
    {
    	relationship_AssignedIssue = PropertyUtil.getSchemaProperty(context, "relationship_AssignedIssue");         
        relationship_AssignedIssue_id = "to["+ relationship_AssignedIssue +"].from.id";
          
    }

    public void migrateObjects(Context context, StringList objectList) throws Exception
    {     
        init(context);
        StringList objectSelects = new StringList(15);
        objectSelects.addElement("id");
        objectSelects.addElement("name");
        objectSelects.addElement("type");
        objectSelects.addElement("revision");        
        objectSelects.addElement(relationship_AssignedIssue_id);        
    
        DomainObject.MULTI_VALUE_LIST.add(relationship_AssignedIssue_id);
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
		            StringList issueAssigneeList = (StringList)valueMap.get(relationship_AssignedIssue_id);
		            
		            if(issueAssigneeList != null){
		            	updateIssueAssigneeAsSOVonIssueObject(context, strObjId, issueAssigneeList);
		            }		            	           
		             
		            //Updating non migratedId's information
		            checkAndWriteUnconvertedOID( strType+","+strName+","+strRevision+", Ownership Update is not required \n" , strObjId);		                          
		            
		        } catch (Exception ex) {
		            mqlLogRequiredInformationWriter("Failed to update ownership ");
		        }
		    }
		    
		} catch(Exception ex){        
        }finally{
         ContextUtil.popContext(context);
        }     
    }
    
    private void updateIssueAssigneeAsSOVonIssueObject(Context context, String ObjectID, StringList issueAssigneeList) throws Exception{
    	
    	for(int i = 0; i < issueAssigneeList.size(); i++){
    		String assigneeId = (String)issueAssigneeList.get(i);
    		if(!UIUtil.isNullOrEmpty(ObjectID) && !UIUtil.isNullOrEmpty(assigneeId)){    	 
        		mqlLogRequiredInformationWriter("Updating  Ownership on "+ ObjectID +" with Assignee Id "+ assigneeId +" as SOV............"); 
        		if(UIUtil.isNullOrEmpty(accessName)){
        			StringList accessNames = DomainAccess.getLogicalNames(context, ObjectID);	
            		accessName = (String)accessNames.get(5);         
        		}    		
        		DomainAccess.createObjectOwnership(context, ObjectID, assigneeId, accessName, DomainAccess.COMMENT_MULTIPLE_OWNERSHIP);              
        		checkAndloadMigratedOids(ObjectID);
        	}    		
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
