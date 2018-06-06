/*
 **  DSCRelatedDrawing
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  JPO to find where this object is used
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

public class DSCRelatedDrawing_mxJPO
{
	private MCADGlobalConfigObject gco = null;
	private String relationshipName    = "";

	public DSCRelatedDrawing_mxJPO(Context context, String[] args) throws Exception
	{
	}
@com.matrixone.apps.framework.ui.ProgramCallable
	public Object getParent(Context context, String[] args) throws Exception
	{
		String objectEnd    = "";	
		String relDirection = getRelationshipInfo(args);

		if(relDirection.length() != 0)
		{
			objectEnd = relDirection;

			if("from".equalsIgnoreCase(relDirection))
			{
				relDirection = "to";
			}
			else
			{
				relDirection = "from";
			}
		}		
		return getList(context, args, relDirection,objectEnd, false);
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
		return getList(context, args, relDirection,objectEnd, true);
	}

	private String getRelationshipInfo(String[] args) throws Exception
	{
		String relDirection	= "";
		String typeName		= "";

		HashMap paramMap  = (HashMap)JPO.unpackArgs(args);
		typeName          = (String)paramMap.get("typeName");
		gco	          = (MCADGlobalConfigObject)paramMap.get("GCO");

		try
		{				
			relDirection = gco.getRelationshipDirection(typeName);
			relationshipName = gco.getRelationshipName(typeName);
		}
		catch(Exception e)
		{
			System.out.println("[DSCRelatedDrawing::getRelationshipInfo] Exception : " + e.getMessage());
			e.printStackTrace();
		}

		//Fix for PBN: 83995: Related Models, Related Drawings and Where Used does not work for MxUG and MxProE
		if (typeName.equals("CADDrawing") && (relationshipName == null || relationshipName.trim().length() == 0)&& 
				(relDirection == null || relDirection.trim().length() == 0) && gco != null)
		{
			try
			{
				relDirection = gco.getRelationshipDirection("drawing");
				relationshipName = gco.getRelationshipName("drawing");				
			}
			catch(Exception e)
			{
				System.out.println("[DSCRelatedDrawing::getRelationshipInfo] Exception : " + e.getMessage());
			}	
		}
		return relDirection;
	}

	private Object getList(Context context, String[] args, String relDirection, String objectEnd, boolean isGetChild) throws Exception
	{
		MapList retBusObjectList	= new MapList();
		HashMap	finalDrwObjMap		= new HashMap();
		HashMap paramMap			= (HashMap)JPO.unpackArgs(args);
		String objectId				= (String)paramMap.get("objectId");
		
		if(null == objectId || "".equals(objectId))
			objectId				= (String)paramMap.get("inputObjId");

		String languageStr			= (String)paramMap.get("languageStr");
		MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)paramMap.get("GCO");

		MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle(languageStr);
		IEFGlobalCache	cache							= new IEFGlobalCache();

		MCADServerGeneralUtil serverGeneralUtil	= new MCADServerGeneralUtil(context, gco, serverResourceBundle, cache);
		MCADMxUtil mxUtil						= new MCADMxUtil(context, serverResourceBundle, cache);
		//String actualObjectIDToWork				= serverGeneralUtil.getRelevantObjId(context, objectId);		

		//if(actualObjectIDToWork != null && !"".equals(actualObjectIDToWork))
		//	objectId = actualObjectIDToWork;
		boolean showDrawingsForAllVersions	= false;
		BusinessObject busObj				= new BusinessObject(objectId);
		busObj.open(context);

		boolean isFinalized					= serverGeneralUtil.isBusObjectFinalized(context, busObj);

		BusinessObject majorBusObj	= null;
		BusinessObject activeMinor	= null;
		String activeMinorId		= null;

        majorBusObj		            = mxUtil.getMajorObject(context, busObj);
//[NDM] H68
		if(majorBusObj == null) //input object is major object
		{
			showDrawingsForAllVersions	= false;
				majorBusObj					= busObj;
				activeMinor					= mxUtil.getActiveMinor(context, majorBusObj);

				if(activeMinor != null)
					activeMinorId				= activeMinor.getObjectId(context);
			}

		else
		{
			majorBusObj.open(context);
			activeMinor		= mxUtil.getActiveMinor(context, majorBusObj);
			activeMinorId	= activeMinor.getObjectId(context);
			if(activeMinorId.equals(objectId) && !isFinalized)
				showDrawingsForAllVersions = true;			
		}

		if(isGetChild) // Do not float for related models
			showDrawingsForAllVersions = false;

		java.util.Set busIdList			= new java.util.HashSet();
		Vector ascendingInputTNRList	= new Vector();

		if(showDrawingsForAllVersions)
		{
			BusinessObjectList minorBusObjList		= mxUtil.getMinorObjects(context, majorBusObj);
			BusinessObjectItr minorBusObjectsItr	= new BusinessObjectItr(minorBusObjList);
			majorBusObj.close(context);

			while(minorBusObjectsItr.next())
			{
				BusinessObject minorBusObject	= minorBusObjectsItr.obj();
				minorBusObject.open(context);
				String minorBusId				= minorBusObject.getObjectId();

				busIdList.add(minorBusId);

				StringBuffer tnrBuffer = new StringBuffer();
				tnrBuffer.append(minorBusObject.getTypeName());
				tnrBuffer.append("|");
				tnrBuffer.append(minorBusObject.getName());
				tnrBuffer.append("|");
				tnrBuffer.append(minorBusObject.getRevision());

				ascendingInputTNRList.add(tnrBuffer.toString());

				if(minorBusId.equals(activeMinorId))
					break;
			}
		}
		else
		{
			busIdList.add(objectId);
		}

		String[] objIds	= new String[busIdList.size()];
		busIdList.toArray(objIds);		

		StringBuffer SELECT_DRAWING = new StringBuffer();
		SELECT_DRAWING.append(objectEnd);
		SELECT_DRAWING.append("[");
		SELECT_DRAWING.append(relationshipName);
		SELECT_DRAWING.append("].");
		SELECT_DRAWING.append(relDirection);
		SELECT_DRAWING.append(".");

		String SELECT_DRAWING_ID   = new StringBuffer(SELECT_DRAWING.toString()).append("id").toString();
		String SELECT_DRAWING_TYPE = new StringBuffer(SELECT_DRAWING.toString()).append("type").toString();
		String SELECT_DRAWING_NAME = new StringBuffer(SELECT_DRAWING.toString()).append("name").toString();
		String SELECT_DRAWING_REV  = new StringBuffer(SELECT_DRAWING.toString()).append("revision").toString();

		StringList busSelectionList = new StringList();
		busSelectionList.addElement("id");
		busSelectionList.addElement("type");
		busSelectionList.addElement("name");
		busSelectionList.addElement("revision");
		busSelectionList.addElement(SELECT_DRAWING_ID);
		busSelectionList.addElement(SELECT_DRAWING_TYPE);
		busSelectionList.addElement(SELECT_DRAWING_NAME);
		busSelectionList.addElement(SELECT_DRAWING_REV);
		
		java.util.Set filteredDrawingTNList	= new java.util.HashSet();
		Vector	sortedOutputList			= (Vector)ascendingInputTNRList.clone();

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);		
		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{			
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);

			if(showDrawingsForAllVersions)
			{
				String type		= busObjectWithSelect.getSelectData("type");
				String name		= busObjectWithSelect.getSelectData("name");
				String rev		= busObjectWithSelect.getSelectData("revision");

				StringBuffer tnrBuffer = new StringBuffer();
				tnrBuffer.append(type);
				tnrBuffer.append("|");
				tnrBuffer.append(name);
				tnrBuffer.append("|");
				tnrBuffer.append(rev);

				String tnr	= tnrBuffer.toString();
				int index	= ascendingInputTNRList.indexOf(tnr);

				sortedOutputList.setElementAt(busObjectWithSelect, index);
			}
			else
			{
				sortedOutputList.addElement(busObjectWithSelect);
			}
		}

		for(int i = sortedOutputList.size()-1; i >= 0; --i)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)sortedOutputList.get(i);

			StringList idList						= busObjectWithSelect.getSelectDataList(SELECT_DRAWING_ID);
			StringList typeList						= busObjectWithSelect.getSelectDataList(SELECT_DRAWING_TYPE);
			StringList nameList						= busObjectWithSelect.getSelectDataList(SELECT_DRAWING_NAME);
			StringList revList						= busObjectWithSelect.getSelectDataList(SELECT_DRAWING_REV);	

			try
			{
				ArrayList localTNList = new ArrayList();
				for(int j = 0; j < idList.size(); j++)
				{
					String drawingBusid 	= (String) idList.get(j);
					String connectedDrwType = (String)typeList.get(j);
					String connectedDrwName = (String)nameList.get(j);

					StringBuffer drwTNBuffer = new StringBuffer();
					drwTNBuffer.append(connectedDrwType);
					drwTNBuffer.append("|");
					drwTNBuffer.append(connectedDrwName);
					String drwTN = drwTNBuffer.toString();

					boolean addToFilteredList = false;
					if(!filteredDrawingTNList.contains(drwTN))
					{
						addToFilteredList = true;
						localTNList.add(drwTN);						
					}
					//localTNList gives all the drawings connected to each version
					else if(localTNList.contains(drwTN))
					{
						addToFilteredList = true;
					}
					
					//Updating flag addToFilteredList with extra checks
					if(addToFilteredList)
					{
						String correspondingDrwType	= gco.getCorrespondingType(connectedDrwType);
						StringBuffer correspondingDrwTNBuff = new StringBuffer();
						correspondingDrwTNBuff.append(correspondingDrwType);
						correspondingDrwTNBuff.append("|");
						correspondingDrwTNBuff.append(connectedDrwName);
						String correspondingDrwTN = correspondingDrwTNBuff.toString(); 						
						
						String activeDrawingMinorId	= "";
						
						if(!mxUtil.isMajorObject(context, drawingBusid)) // [NDM] QWJ
						{
							activeDrawingMinorId = mxUtil.getActiveVersionObjectFromMinor(context, drawingBusid);
						}
						else
						{
							activeDrawingMinorId = mxUtil.getActiveVersionObject(context, drawingBusid);
						}
						
						if(finalDrwObjMap.containsKey(correspondingDrwTN))
						{
							HashMap	minorDrwMap		= (HashMap)finalDrwObjMap.get(correspondingDrwTN);								
							String  activeMinorCacheRevision		= (String)minorDrwMap.get("ActiveVersion");
							
							if(!mxUtil.isMajorObject(context, drawingBusid)) // [NDM] QWJ
							{
								if(activeMinorCacheRevision != null && !"".equals(activeMinorCacheRevision) && activeMinorCacheRevision.equals(activeDrawingMinorId))
									continue;
							}
							else if(mxUtil.isMajorObject(context, drawingBusid)) // [NDM] QWJ
							{
								if((activeMinorCacheRevision!= null && !"".equals(activeMinorCacheRevision) && activeMinorCacheRevision.equals(activeDrawingMinorId)))
									retBusObjectList.remove(minorDrwMap);
							}
						}
					
						HashMap tempHashMap = new HashMap();
						tempHashMap.put(DomainObject.SELECT_ID, drawingBusid);
						tempHashMap.put(DomainObject.SELECT_TYPE, connectedDrwType);
						tempHashMap.put(DomainObject.SELECT_NAME, connectedDrwName);
						tempHashMap.put(DomainObject.SELECT_REVISION, revList.get(j));						
						tempHashMap.put("ActiveVersion", activeDrawingMinorId);	

						finalDrwObjMap.put(drwTN, tempHashMap);
						filteredDrawingTNList.add(drwTN);
						retBusObjectList.add(tempHashMap);
					}						
				}
			}
			catch(Exception e)
			{
				//e.printStackTrace();
				// If there is no Drawing for component
				// Do nothing
			}
		}
		
		ArrayList finalBusList = new ArrayList();
		
		MapList returnList	= new MapList();
		
		if(!retBusObjectList.isEmpty())
		{
			for (int i = 0; i < retBusObjectList.size(); i++)
			{
				HashMap returnMap = (HashMap) retBusObjectList.get(i);
				String busid 	  = (String) returnMap.get(DomainObject.SELECT_ID);
				
				if(!finalBusList.contains(busid))
				{
					finalBusList.add(busid);
					returnList.add(returnMap);
				}
			}
		}
		
		return returnList;
	}
}
