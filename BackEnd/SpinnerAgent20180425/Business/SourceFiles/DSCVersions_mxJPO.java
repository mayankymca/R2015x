/*
**  DSCVersions
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

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

public class DSCVersions_mxJPO
{
	public DSCVersions_mxJPO(Context context, String[] args) throws Exception
	{
	}
@com.matrixone.apps.framework.ui.ProgramCallable
	public Object getVersions(Context context, String[] args) throws Exception
	{
		HashMap paramMap		= (HashMap)JPO.unpackArgs(args);
		String objectId			= (String)paramMap.get("objectId");
		
		if(null == objectId || "".equals(objectId))
			objectId			= (String)paramMap.get("inputObjId");
		
		String languageStr		= (String)paramMap.get("languageStr");
		MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)paramMap.get("GCO");

		MapList retBusObjectList = new MapList();
						
		String[] objIds	= new String[1];
		objIds[0]	= objectId;
		
		String relActiveVersion		   = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
		String SELECT_VERSION_REV          = "from[" + relActiveVersion + "].to.revisions";
		String SELECT_VERSION_REV_ID       = new StringBuffer(SELECT_VERSION_REV).append(".id").toString();	
		String SELECT_VERSION_REV_NAME     = new StringBuffer(SELECT_VERSION_REV).append(".name").toString();	
		String SELECT_VERSION_REV_TYPE     = new StringBuffer(SELECT_VERSION_REV).append(".type").toString();
		String SELECT_VERSION_REV_REVISION = new StringBuffer(SELECT_VERSION_REV).append(".revision").toString();
		String SELECT_ACTIVEVERSION_ID = "from[" + relActiveVersion +"].to.id";

		StringList busSelectionList = new StringList(5);
		busSelectionList.addElement(SELECT_VERSION_REV);
		busSelectionList.addElement(SELECT_VERSION_REV_ID);
		busSelectionList.addElement(SELECT_VERSION_REV_NAME);
		busSelectionList.addElement(SELECT_VERSION_REV_TYPE);
		busSelectionList.addElement(SELECT_VERSION_REV_REVISION);
		busSelectionList.addElement(SELECT_ACTIVEVERSION_ID);
				
		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);
		
		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{			
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
			
			StringList revisionList = busObjectWithSelect.getSelectDataList(SELECT_VERSION_REV);
						
			try
			{
				for(int j = 0; j < revisionList.size(); j++)
				{
					String currentRevision = (String) revisionList.elementAt(j);
					StringList idList = busObjectWithSelect.getSelectDataList(SELECT_VERSION_REV+"["+currentRevision+"].id");
					StringList nameList = busObjectWithSelect.getSelectDataList(SELECT_VERSION_REV+"["+currentRevision+"].name");
					StringList typeList = busObjectWithSelect.getSelectDataList(SELECT_VERSION_REV+"["+currentRevision+"].type");
					StringList revList = busObjectWithSelect.getSelectDataList(SELECT_VERSION_REV+"["+currentRevision+"].revision");
					String activeVersionId = busObjectWithSelect.getSelectData(SELECT_ACTIVEVERSION_ID);
				
					try
					{
						for(int k = 0; k < idList.size(); k++)
						{
							HashMap tempHashMap = new HashMap();
							tempHashMap.put(DomainObject.SELECT_ID, idList.get(k));
							tempHashMap.put(DomainObject.SELECT_TYPE, typeList.get(k));
							tempHashMap.put(DomainObject.SELECT_NAME, nameList.get(k));
							tempHashMap.put(DomainObject.SELECT_REVISION, revList.get(k));
							if(activeVersionId != null && activeVersionId.equals(idList.get(k)))
								tempHashMap.put("styleRows", "RowBackGroundColor");
							retBusObjectList.add(tempHashMap);
						}						
					}
					catch(Exception e)
					{
					}
				}					
			}
			catch(Exception exp)
			{
			}
		}
		return retBusObjectList;
	}
}

