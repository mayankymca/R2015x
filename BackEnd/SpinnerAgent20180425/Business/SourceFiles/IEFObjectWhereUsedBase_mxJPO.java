/*
**  IEFObjectWhereUsed
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  JPO to find where this object is used
*/

import java.util.HashMap;

import matrix.util.StringList;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

public class IEFObjectWhereUsedBase_mxJPO
{
    public IEFObjectWhereUsedBase_mxJPO(Context context, String[] args) throws Exception
    {
    }
    @com.matrixone.apps.framework.ui.ProgramCallable
//[NDM] H68: Start

    public Object getList(Context context, String[] args) throws Exception
        {
    		MapList retBusObjectList	= new MapList();
    		HashMap paramMap			= (HashMap)JPO.unpackArgs(args);
            String objectId				= (String)paramMap.get("objectId");
    		String funcPageName			= (String)paramMap.get("funcPageName");

    		String[] objIds	= new String[1];
    		objIds[0]		= objectId;
    		
           	String relationshipName = (String)paramMap.get("relationship");
    		if(relationshipName != null)
    		{
    			String relDirection	= (String)paramMap.get("end");
    			String languageStr	= (String)paramMap.get("languageStr");
    			String instanceName	= (String)paramMap.get("instanceName");
    			
    			String relOppDirection	= "from";
    			if(relDirection.equalsIgnoreCase("from"))
    				relOppDirection = "to";

    			MCADGlobalConfigObject gco= (MCADGlobalConfigObject)paramMap.get("GCO");

    			MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle(languageStr);
    			IEFGlobalCache	cache							= new IEFGlobalCache();
    			MCADMxUtil mxUtil								= new MCADMxUtil(context, serverResourceBundle, cache);
    			MCADServerGeneralUtil serverGeneralUtil			= new MCADServerGeneralUtil(context, gco, serverResourceBundle, cache);
    			String actualObjectIDToWork						= serverGeneralUtil.getRelevantObjId(context, objectId);
    			
    			if(actualObjectIDToWork != null && !"".equals(actualObjectIDToWork))
    				objectId = actualObjectIDToWork;
   
    			String attrName = "";
    			String attrVal  = "";
    			if(instanceName != null && !"null".equalsIgnoreCase(instanceName))
    			{			
    				attrVal  = MCADUrlUtil.hexDecode(instanceName);
    				attrName = (String) MCADMxUtil.getActualNameForAEFData(context,"attribute_ChildInstance");
}

    			String relLatestVersion		= MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion");
    			String relCADSubComponent	= MCADMxUtil.getActualNameForAEFData(context, "relationship_CADSubComponent");

    			String SELECT_RELATIONSHIP_ID	= relOppDirection +"[" + relationshipName + "]." + relDirection + ".id";
    			String SELECT_LATESTVERSION_ID	= relDirection + "[" + relLatestVersion +"]." + relOppDirection + ".id";
    			String SELECT_CADSUBCOMPONENT	= "relationship["+relCADSubComponent+"].attribute["+attrName+"]";

    			StringList busSelectionList = new StringList(2);

    			busSelectionList.addElement(SELECT_LATESTVERSION_ID);
    			busSelectionList.addElement(SELECT_RELATIONSHIP_ID);

    			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);
    			
    			StringList latestVersionID	= new StringList();
    			StringList objectIDs		= new StringList();
    			StringList childInstance	= new StringList();

    		
    			for (int i = 0; i < buslWithSelectionList.size(); i++)
    			{
    				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) buslWithSelectionList.elementAt(i);
    				objectIDs	= busObjectWithSelect.getSelectDataList(SELECT_RELATIONSHIP_ID);
    				if(null != objectIDs)
    				{
    					if(instanceName != null && !"null".equalsIgnoreCase(instanceName) && !"".equals(instanceName.trim()))
    					{
    						childInstance			= busObjectWithSelect.getSelectDataList(SELECT_CADSUBCOMPONENT);
    						if(!childInstance.equals(instanceName))
    							continue;
    					}	
    					if("Purge".equalsIgnoreCase(funcPageName))
    					{			
    						latestVersionID 		= busObjectWithSelect.getSelectDataList(SELECT_LATESTVERSION_ID);
    						String sLatestVersionID = (String) latestVersionID.get(0);
    						objectIDs.removeElement(sLatestVersionID);

    					}

    					for(int j=0; j< objectIDs.size() ; j++)
    					{
    						String strObjectId = (String)objectIDs.elementAt(j);				
    						BusinessObject connectedObject = new BusinessObject(strObjectId);
    						connectedObject.open(context);
    						HashMap tempHashMap = new HashMap();
    						tempHashMap.put(DomainObject.SELECT_ID, strObjectId);
    						tempHashMap.put(DomainObject.SELECT_TYPE, connectedObject.getTypeName());
    						tempHashMap.put(DomainObject.SELECT_NAME, connectedObject.getName());
    						tempHashMap.put(DomainObject.SELECT_REVISION, connectedObject.getRevision());
    						connectedObject.close(context);
    						//emxInfoTable takes the MapList as the input parameter to display the output.
    						retBusObjectList.add(tempHashMap);
    					}
    				}
    			}
    			
    		}
    	   
            return retBusObjectList;
        }

}

