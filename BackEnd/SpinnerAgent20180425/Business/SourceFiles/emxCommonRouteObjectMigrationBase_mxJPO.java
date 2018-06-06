
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import matrix.util.Pattern;

import matrix.db.Access;
import matrix.db.AccessList;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.util.SelectList;
import matrix.util.StringList;

import com.matrixone.apps.common.Route;
import com.matrixone.apps.domain.DomainAccess;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.AccessUtil;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

public class emxCommonRouteObjectMigrationBase_mxJPO extends emxCommonMigration_mxJPO
{

    private static final long serialVersionUID = -5029177381386073045L;
    static private  Map<String, Integer> _accessMasksConstMapping = new HashMap<String, Integer>(35);
    
    public static String relationship_ObjectRoute = "";   
    
    public static String relationship_ObjectRoute_id = "";    
    public static String relationship_ObjectRoute_type = "";    
    public static String relationship_ObjectRoute_name = "";
    public static final String SEL_ROUTE_TASK_USER =
            getAttributeSelect(ATTRIBUTE_ROUTE_TASK_USER);
    
    static final String AEF_ROUTE_DELEGATION_GRANTOR_USERNAME = "Route Delegation Grantor";
    private static final String ROUTE_ACCESS_GRANTOR = "Route Access Grantor";
       

    public emxCommonRouteObjectMigrationBase_mxJPO(Context context, String[] args) throws Exception {
      super(context, args);
    }

    public static void init(Context context) throws FrameworkException
    {
         relationship_ObjectRoute = PropertyUtil.getSchemaProperty(context, "relationship_ObjectRoute");         
         relationship_ObjectRoute_id = "to["+ relationship_ObjectRoute +"].from.id";        
         relationship_ObjectRoute_type = "to["+ relationship_ObjectRoute +"].from.type";
         relationship_ObjectRoute_name = "to["+ relationship_ObjectRoute +"].from.name";          
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
        objectSelects.addElement(relationship_ObjectRoute_type);   
     
        String[] oidsArray = new String[objectList.size()];
        oidsArray = (String[])objectList.toArray(oidsArray);
        DomainObject.MULTI_VALUE_LIST.add(relationship_ObjectRoute_id);
        DomainObject.MULTI_VALUE_LIST.add(relationship_ObjectRoute_type);
        DomainObject.MULTI_VALUE_LIST.add(relationship_ObjectRoute_name);
        
		mqlLogRequiredInformationWriter("getInfo ...................."); 
		
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
        			StringList strObjectRouteIdsList = (StringList)valueMap.get(relationship_ObjectRoute_id);
        			
        			MapList memberList = getMemberAccesList(context, new DomainObject(strObjId), valueMap);        			               
        			mqlLogRequiredInformationWriter("::valueMap ....................:: "+  valueMap.toString()); 
        			if("Route".equals(strType)) {
        			Route route = (Route)DomainObject.newInstance(context,strObjId);
        			
        				mqlLogRequiredInformationWriter("Updating SOV on Route and its content ....................");
        				route.addRouteMemberAccess(context, memberList,false);
        			
        				mqlLogRequiredInformationWriter("Revoking access on Route and its content for members....................");
        				revokeAccessGrantedByRouteAccessGrantorOnRouteContent(context, strObjId, strObjectRouteIdsList, memberList);
        			}else {
        				mqlLogRequiredInformationWriter("Updating SOV on Route Template ....................");
        				addRouteTemplateMemberAccess(context, memberList, strName, strObjId);
				
        				mqlLogRequiredInformationWriter("Revoking access on Route Template for members....................");
        				revokeAccessGrantedByRouteAccessGrantorOnRouteTemplate(context, strObjId, strObjectRouteIdsList, memberList);
        			}			       			
                 
        			//Updating non migratedId's information
        			checkAndWriteUnconvertedOID( strType+","+strName+","+strRevision+",Ownership Update is not required \n" , strObjId);                          
                
            } catch (Exception ex) {
            	mqlLogRequiredInformationWriter("Failed to upgrade ownership ");
            }
        }
        
        } catch(Exception ex){        
        }finally{
         ContextUtil.popContext(context);
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
    
    public void addRouteTemplateMemberAccess(Context context, MapList routeMemberList, String routeTemplateName, String routeId) throws FrameworkException
    {
    	            	
    	if(routeMemberList != null && routeMemberList.size() > 0){
    		Iterator routeMemberListItr = routeMemberList.iterator();                    
    		while(routeMemberListItr.hasNext())
    		{
    			Map routeMemberMap = (Map)routeMemberListItr.next();
    			String personId    = (String)routeMemberMap.get(DomainObject.SELECT_ID);
    			if (personId != null && !"".equals(personId)  && !"none".equals(personId))
    			{  
    				if(!"Role".equals(personId) && !"Group".equals(personId))
    				{
    					String sAccess     = ((String) routeMemberMap.get("access")).trim();
    					String personName    = (String)routeMemberMap.get(DomainObject.SELECT_NAME);     						
    					try {
    						ContextUtil.pushContext(context);
    						DomainAccess.createObjectOwnership(context, routeId, null, personName + "_PRJ", sAccess, DomainAccess.COMMENT_MULTIPLE_OWNERSHIP, true);
                                    		
    					} catch(Exception ex) {
    						throw new FrameworkException(ex);
    					} finally {
    						ContextUtil.popContext(context);
    					}
                                    	
    				}
    			}
    		}              

    	}
    }
    
    
    public static MapList getMemberAccesList(Context context, DomainObject routeBO, Map valueMap) throws Exception{
    	
        AccessList routeDelegateGranotrList = routeBO.getAccessForGrantor(context, AEF_ROUTE_DELEGATION_GRANTOR_USERNAME);
        AccessList routeAccessGranotrList = routeBO.getAccessForGrantor(context, AccessUtil.ROUTE_ACCESS_GRANTOR);

        Map routeDelegateMap = new HashMap(routeDelegateGranotrList.size());
        Map routeAccessMap = new HashMap(routeAccessGranotrList.size());

        for (int i = 0; i < routeDelegateGranotrList.size(); i++) {
            Access access = (Access) routeDelegateGranotrList.get(i);
            routeDelegateMap.put(access.getUser(), access);
        }
        
        for (int i = 0; i < routeAccessGranotrList.size(); i++) {
            Access access = (Access) routeAccessGranotrList.get(i);
            routeAccessMap.put(access.getUser(), access);
        }
        
        
        SelectList selectables = new SelectList(6);        
        selectables.addElement(Route.SELECT_ID);
        selectables.addElement(Route.SELECT_TYPE);
        selectables.addElement(Route.SELECT_NAME);       

        // build select params for Relationship
        SelectList selectPersonRelStmts = new SelectList(2);
        selectPersonRelStmts.addElement(Route.SELECT_ROUTE_TASK_USER);

        Pattern typePattern = new Pattern(TYPE_PERSON);
        typePattern.addPattern(TYPE_ROUTE_TASK_USER);
        
        
        MapList memberList = routeBO.getRelatedObjects(context, 
        												RELATIONSHIP_ROUTE_NODE, typePattern.getPattern(),
        												selectables, selectPersonRelStmts,
        												false, true, (short)1, 
        												EMPTY_STRING, EMPTY_STRING,
        												0);        

        StringList members  = new StringList(memberList.size());
        MapList tempMapList = new MapList(memberList.size());        
        String strOwner = (String) valueMap.get(SELECT_OWNER);

        Iterator itr1 = memberList.iterator();
        AccessUtil accessUtil   = new AccessUtil();
        while(itr1.hasNext()) {
            Map  membermap =  (Map) itr1.next();
            String memberName         = (String)membermap.get(SELECT_NAME);
            String routeTaskUser     = (String)membermap.get(Route.SELECT_ROUTE_TASK_USER);
            String nodeType           = (String)membermap.get(SELECT_TYPE);
            boolean isRouteOwner = memberName.equals(strOwner);
            String sAccess  = isRouteOwner ? "Add Remove" : "Read";
            String dispMemberName = nodeType.equals(TYPE_PERSON) ? memberName : UIUtil.isNullOrEmpty(routeTaskUser) ? "" : PropertyUtil.getSchemaProperty(context, routeTaskUser);

            if(!UIUtil.isNullOrEmpty(dispMemberName) && !members.contains(dispMemberName)) {
                String org = (String) membermap.get(com.matrixone.apps.common.Person.SELECT_COMPANY_NAME);
                               
                HashMap tempHash = new HashMap(10);
                String toNodeType      = (String)membermap.get(SELECT_TYPE);
                
                tempHash.put(SELECT_ID, membermap.get(SELECT_ID));                
                tempHash.put(SELECT_TYPE,toNodeType);
                tempHash.put(SELECT_NAME, memberName);
              
                String type = "";
                if(toNodeType.equals(TYPE_ROUTE_TASK_USER) && !UIUtil.isNullOrEmpty(routeTaskUser)) {
                    type = routeTaskUser.substring(0, routeTaskUser.indexOf("_") );
                    if("role".equals(type)){
                    	type ="Role";
                    }else{
                    	type ="Group";
                    }
                    String sGrantee = PropertyUtil.getSchemaProperty(context, routeTaskUser);
                    
                    tempHash.put(SELECT_ID, type);
                    tempHash.put(SELECT_NAME, routeTaskUser);
                    tempHash.put(SELECT_TYPE, type);
                } else {
                    String sGrantee = (String)membermap.get(SELECT_NAME) ;
                    Access access = (Access) routeDelegateMap.get(sGrantee);
                    access = access == null ? (Access) routeAccessMap.get(sGrantee) : access;

                    if(access != null && !isRouteOwner) {
                        sAccess = accessUtil.checkAccess(access);
                        if(accessUtil.WORKSPACE_LEAD.equals(sAccess)) {
                            sAccess = accessUtil.ADD_REMOVE;
                        }
                    }                     
                    
                }
                tempHash.put("access", sAccess);                
                
                members.add(dispMemberName);
                tempMapList.add(tempHash);
            }
        }
        return tempMapList;
    }
    
public void revokeAccessGrantedByRouteAccessGrantorOnRouteTemplate(Context context, String routeId, StringList strParentId, MapList memberList) throws Exception {
    
        if(memberList != null && memberList.size() > 0){
        	DomainObject[] domainObject =  new DomainObject[] { new DomainObject(routeId)};
            Iterator routeMemberListItr = memberList.iterator();            
            while(routeMemberListItr.hasNext())
            {
                    Map routeMemberMap = (Map)routeMemberListItr.next();
                    String personId    = (String)routeMemberMap.get(DomainObject.SELECT_ID);
    
                    if (personId != null && !"".equals(personId)  && !"none".equals(personId))
                    {
                    	String personName    = (String)routeMemberMap.get(DomainObject.SELECT_NAME);                      

                    	if(!"Role".equals(personId) && !"Group".equals(personId))
                    	{
                            	revokeAccessOnContent(context, domainObject, new String[] {personName});
                    	}
                    }
            }
            
            checkAndloadMigratedOids(routeId);            
        }
        
    }
    
    
    public void revokeAccessGrantedByRouteAccessGrantorOnRouteContent(Context context, String routeId, StringList strParentId, MapList memberList) throws Exception {
    
    		Route routeBO = (Route)DomainObject.newInstance(context,DomainConstants.TYPE_ROUTE);
            routeBO.setId(routeId);
        	
        	AccessList accessList =  routeBO.getAccessForGrantor(context, ROUTE_ACCESS_GRANTOR);
            String[] grantees = new String[accessList.size()];
            for (int i = 0; i < accessList.size(); i++) {
                grantees[i] = ((Access) accessList.get(i)).getUser();
            }
            
        mqlLogRequiredInformationWriter("List of grantees Identified on this Route "+routeId + " are ::::  "+ grantees.toString()); 
        if(strParentId != null && strParentId.size()>0){
            DomainObject[] connectedObjects = new DomainObject[strParentId.size()];
            mqlLogRequiredInformationWriter("List of Objects connected with Object Route Rel are ::::  ");
            for (int i = 0; i < strParentId.size(); i++) {
            	mqlLogRequiredInformationWriter(i+ ") "+ (String) strParentId.get(i)); 
                connectedObjects[i] = new DomainObject((String) strParentId.get(i));
            }
            
			mqlLogRequiredInformationWriter("revokeAccessOnContent for grantees on Content ...................."+ routeId); 
            revokeAccessOnContent(context, connectedObjects, grantees);
            
    	}else{
    		mqlLogRequiredInformationWriter("No objects are connected with Object Route relationship ...................."); 
    	}
        mqlLogRequiredInformationWriter("revokeAccessOnRoute for grantees ...................."+ grantees.toString());       
        
        if(memberList != null && memberList.size() > 0){
        	DomainObject[] domainObject =  new DomainObject[] { new DomainObject(routeId)};
            Iterator routeMemberListItr = memberList.iterator();            
            while(routeMemberListItr.hasNext())
            {
                    Map routeMemberMap = (Map)routeMemberListItr.next();
                    String personId    = (String)routeMemberMap.get(DomainObject.SELECT_ID);
    	
                    if (personId != null && !"".equals(personId)  && !"none".equals(personId))
                    {
                    	String personName    = (String)routeMemberMap.get(DomainObject.SELECT_NAME);                      

                    	if(!"Role".equals(personId) && !"Group".equals(personId))
                    	{
                            	revokeAccessOnContent(context, domainObject, new String[] {personName});
                    	}
                    }
            }
            
            checkAndloadMigratedOids(routeId);
            
        }
    	
    }
    
    
    private void revokeAccessOnContent(Context context, DomainObject[] contents, String[] grantees) throws Exception {
        if(contents == null || contents.length == 0 || grantees == null || grantees.length == 0) 
            return;

        try {
            ContextUtil.pushContext(context);
            for (int i = 0; i < contents.length; i++) {                
                for (int j = 0; j < grantees.length; j++) {
                    if(canRevokeAccessOnContentToGrantee(context,  contents[i], grantees[j])) {
                        StringBuffer command = new StringBuffer(100);
                        command.append("mod bus ");
                        command.append(contents[i].getId(context));
                        command.append(" revoke grantor '");
                        command.append(ROUTE_ACCESS_GRANTOR);
                        command.append("' grantee '");
                        command.append(grantees[j]);
                        command.append("';");
                        mqlLogRequiredInformationWriter("MQL Command ........."+ command.toString()); 
                        MqlUtil.mqlCommand(context, command.toString());
                        
                        mqlLogRequiredInformationWriter("MQL Command : "+ command.toString() + "....Executed successfully."); 
                    }
                }
            }
        } finally {
            ContextUtil.popContext(context);
        }
    }
    
    private boolean canRevokeAccessOnContentToGrantee(Context context, DomainObject content, String grantee) throws Exception {
        if(getAccessMask(context, content, ROUTE_ACCESS_GRANTOR, grantee) == null){
            return false;
        }else {
        	return true;
        }        
    }
       
  
    private Access getAccessMask(Context context, BusinessObject object, String grantor, String grantee) throws Exception {
        AccessList accessList = object.getAccessForGrantee(context, grantee);
        for (Iterator iter = accessList.iterator(); iter.hasNext();) {
            Access acc = (Access) iter.next();
            if(grantor.equals(acc.getGrantor())) {
            	mqlLogRequiredInformationWriter("AccessMask of grante.....: "+ grantee+" is :" + acc); 
                return acc;
            }
        }
        mqlLogRequiredInformationWriter("AccessMask of grantee -----: "+ grantee+" is :" + null);
        return null;
    }
    
}
