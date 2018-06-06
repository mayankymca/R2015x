/*
**  DSCRelatedInstances.java
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

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
public class DSCRelatedInstances_mxJPO
{
	private MCADGlobalConfigObject gco = null;
	private String relationshipName    = "";
	
	public DSCRelatedInstances_mxJPO(Context context, String[] args) throws Exception
	{
	}
@com.matrixone.apps.framework.ui.ProgramCallable
	public Object getChild(Context context, String[] args) throws Exception
	{
		String objectEnd    = "";	
		String relDirection = getRelationshipInfo(args);
		
		if(relDirection.length() != 0)
		{
			if("from".equalsIgnoreCase(relDirection))
			{
				objectEnd = "to";
			}
			else
			{
				objectEnd = "from";
			}
		}		
		return getList(context, args, relDirection,objectEnd);
	}

	private String getRelationshipInfo(String[] args) throws Exception
	{
		String relDirection = "";
		String typeName	    = "";

		HashMap paramMap    = (HashMap)JPO.unpackArgs(args);
		typeName            = (String)paramMap.get("typeName");
		gco		    = (MCADGlobalConfigObject)paramMap.get("GCO");
 
		try
		{				
			relDirection	 = gco.getRelationshipDirection(typeName);
			relationshipName = gco.getRelationshipName(typeName);
		}
		catch(Exception e)
		{
			System.out.println("[DSCRelatedInstances::getRelationshipInfo] Exception : " + e.getMessage());
			e.printStackTrace();
		}

		//Fix for PBN: 83995: Related Models, Related Drawings and Where Used does not work for MxUG and MxProE
		if (typeName.equals("CADInstanceOf") && (relationshipName == null || relationshipName.trim().length() == 0)&& 
		    (relDirection == null || relDirection.trim().length() == 0) && gco != null)
		{
			try
			{
				relDirection	 = gco.getRelationshipDirection("InstanceOf");
				relationshipName = gco.getRelationshipName("InstanceOf");				
			}
			catch(Exception e)
			{
				System.out.println("[DSCRelatedInstances::getRelationshipInfo] Exception : " + e.getMessage());
			}	
		}

		return relDirection;
	}

	private Object getList(Context context, String[] args, String relDirection, String objectEnd) throws Exception
	{
		MapList retBusObjectList		= new MapList();
		HashMap paramMap				= (HashMap)JPO.unpackArgs(args);
		String objectId					= (String)paramMap.get("objectId");
		
		if(null == objectId || "".equals(objectId))
			objectId					= (String)paramMap.get("inputObjId");
		
		String languageStr				= (String)paramMap.get("languageStr");
		MCADGlobalConfigObject gco		= (MCADGlobalConfigObject)paramMap.get("GCO");
		
		MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle(languageStr);
		IEFGlobalCache	cache				= new IEFGlobalCache();

		MCADServerGeneralUtil serverGeneralUtil		= new MCADServerGeneralUtil(context, gco, serverResourceBundle, cache);
		//String actualObjectIDToWork			= serverGeneralUtil.getRelevantObjId(context, objectId);
		
		//if(actualObjectIDToWork != null && !"".equals(actualObjectIDToWork))
			//objectId = actualObjectIDToWork;

		String[] objIds	= new String[1];
		objIds[0]	= objectId;

		StringBuffer SELECT_INSTANCE = new StringBuffer();
		SELECT_INSTANCE.append(objectEnd);
		SELECT_INSTANCE.append("[");
		SELECT_INSTANCE.append(relationshipName);
		SELECT_INSTANCE.append("].");
		SELECT_INSTANCE.append(relDirection);
		
		String SELECT_INSTANCE_ID   = new StringBuffer(SELECT_INSTANCE.toString()).append(".id").toString();
		String SELECT_INSTANCE_TYPE = new StringBuffer(SELECT_INSTANCE.toString()).append(".type").toString();
		String SELECT_INSTANCE_NAME = new StringBuffer(SELECT_INSTANCE.toString()).append(".name").toString();
		String SELECT_INSTANCE_REV  = new StringBuffer(SELECT_INSTANCE.toString()).append(".revision").toString();
		String SELECT_ACTIVEINSTANCE_ID = "from[" + MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveInstance") +"].to.id";
		
		StringList busSelectionList = new StringList();
		busSelectionList.addElement("id");
		busSelectionList.addElement(SELECT_INSTANCE_ID);
		busSelectionList.addElement(SELECT_INSTANCE_TYPE);
		busSelectionList.addElement(SELECT_INSTANCE_NAME);
		busSelectionList.addElement(SELECT_INSTANCE_REV);
		busSelectionList.addElement(SELECT_ACTIVEINSTANCE_ID);
		
		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);
		
		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{			
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
			
			StringList idList = busObjectWithSelect.getSelectDataList(SELECT_INSTANCE_ID);
			StringList typeList = busObjectWithSelect.getSelectDataList(SELECT_INSTANCE_TYPE);
			StringList nameList = busObjectWithSelect.getSelectDataList(SELECT_INSTANCE_NAME);
			StringList revList = busObjectWithSelect.getSelectDataList(SELECT_INSTANCE_REV);	
			
			String activeInstanceId = busObjectWithSelect.getSelectData(SELECT_ACTIVEINSTANCE_ID);
			try
			{
				for(int j = 0; j < idList.size(); j++)
				{
					HashMap tempHashMap = new HashMap();
					tempHashMap.put(DomainObject.SELECT_ID, idList.get(j));
					tempHashMap.put(DomainObject.SELECT_TYPE, typeList.get(j));
					tempHashMap.put(DomainObject.SELECT_NAME, nameList.get(j));
					tempHashMap.put(DomainObject.SELECT_REVISION, revList.get(j));
					
					if(activeInstanceId != null && activeInstanceId.equals(idList.get(j)))
						tempHashMap.put("styleRows", "RowBackGroundColor");
					retBusObjectList.add(tempHashMap);
				}						
			}
			catch(Exception e)
			{
				// If there is no Drawing for component
				// Do nothing
			}
		}
		return retBusObjectList;
	}
}
