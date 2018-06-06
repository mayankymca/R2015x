/**
 * DSCReplaceWhereUsedParents.java
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 */
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.db.RelationshipList;
import matrix.db.User;
import matrix.util.MatrixException;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.MapList;

public class DSCReplaceWhereUsedParents_mxJPO 
{			
	private MCADMxUtil util									= null;
	private MCADServerGeneralUtil _generalUtil				= null;
	private IEFGlobalCache cache							= null;
	private String localeLanguage							= null;
	private MCADServerResourceBundle serverResourceBundle	= null;

	//Map that holds the GCO Table Vs the integration name.
	private HashMap integrationNameGCOTable	= null;	
	
	public DSCReplaceWhereUsedParents_mxJPO (Context context, String[] args) throws Exception
	{
	}
	/**
	 * @param objId The object id for which we check if its locked or finalized.
	 * @return      The remark string that will appear on the page if it is locked and
	 *              finalized.
	 */
	private String checkForLockFinalize(Context _context, String objId) throws MCADException
	{		
		StringBuffer remark = new StringBuffer();

		try
		{
			String integName					= util.getIntegrationName(_context, objId);
			MCADGlobalConfigObject gco			= (MCADGlobalConfigObject) integrationNameGCOTable.get(integName);
			MCADServerGeneralUtil _generalUtil	= new MCADServerGeneralUtil(_context, gco, serverResourceBundle, cache);
		
			BusinessObject obj = new BusinessObject(objId);
			obj.open(_context);
			String user = _context.getUser();
			
			String majLockOwner	= null;
			
			BusinessObject majorObj = util.getMajorObject(_context, obj);
			if(majorObj == null)
			{
				User majOwner	= obj.getLocker(_context);
				majLockOwner	= majOwner.getName();
			}
			else
			{
				User majOwner	= majorObj.getLocker(_context);
				majLockOwner	= majOwner.getName();
			}

			boolean isFinalized			= _generalUtil.isBusObjectFinalized(_context, obj);		    
			String versionNLockerStr	= util.getLockedVersionNLocker(_context, obj, gco);
			
			//locked and finalized
			if (isFinalized && !versionNLockerStr.equals("") && !majLockOwner.equals(user))
			{				
				if (majLockOwner.trim().length()> 0 && user.equalsIgnoreCase(majLockOwner) == false)
				{
					String comment = serverResourceBundle.getString("mcadIntegration.Server.Message.ReplaceParentFinalizedLocked");
					remark.append(comment);
					remark.append(majLockOwner);
				}
				else
				{
					String comment = serverResourceBundle.getString("mcadIntegration.Server.Message.ReplaceParentFinalized");
					remark.append(comment);
					remark.append(majLockOwner);
				}				
			}
			//locked
			else if (!versionNLockerStr.equals("") && !isFinalized && !majLockOwner.equals(user))
			{
				
				if (majLockOwner.trim().length()> 0 && user.equalsIgnoreCase(majLockOwner) == false)
				{
					String comment = serverResourceBundle.getString("mcadIntegration.Server.Message.ReplaceParentLocked");
					remark.append(comment);
					remark.append(majLockOwner);
				}
			}
			//finalized
			else if (isFinalized)
			{				
				String comment = serverResourceBundle.getString("mcadIntegration.Server.Message.ReplaceParentFinalized");
				remark.append(comment);
			}
			else
			{
				remark.append("");
			}

			obj.close(_context);
		}
		catch (Exception exception)
		{
			MCADServerException.createException(exception.getMessage(), exception);
			exception.printStackTrace();
		}
		
		String finalRemark = "";
		if (remark.length() > 0)
		{
			finalRemark = remark.toString();
		}

		return finalRemark;
	}
	/**
	 * @param inBus Input object for which its minors for the revision are found.
	 * @return The list of major objects and minor objects for the revision.
	 */
	private BusinessObjectList getRevisionObjects(Context _context, BusinessObject inBus) throws MCADException
	{
        //List of the businessobjects that has a list of minors + major
		BusinessObjectList revObjects = null;
		try
		{
			BusinessObject majorObj = util.getMajorObject(_context, inBus);

			//This means that the major object is available 
			if(majorObj == null) 
			{
				revObjects = util.getMinorObjects(_context, inBus);
				if(!revObjects.contains(inBus))
    			{
				  revObjects.add(inBus);
	   			}
			}
			else
			{
				revObjects = util.getMinorObjects(_context, majorObj);
				if(revObjects.contains(majorObj) == false)
    			{
				  revObjects.add(majorObj);
    			}	
			}
			return revObjects;
		}
		catch (MCADException exception)
		{
			MCADServerException.createException(exception.getMessage(), exception);
			exception.printStackTrace();
		}
		return revObjects;
	}

	public BusinessObjectList getRemovedBusObjectsList(Context context, BusinessObject toRemove, BusinessObjectList lst) throws Exception
	{
		BusinessObjectList newLst = null;
		try
		{
			newLst = new BusinessObjectList();
			for(int i=0;i<lst.size();i++)
			{
				BusinessObject obj = (BusinessObject) lst.get(i);
				obj.open(context);

				String objID = obj.getObjectId();
				String toMatch = toRemove.getObjectId();
				if(!objID.equals(toMatch))
				{
				  newLst.add(obj);
				}
				obj.close(context);
		    }
			
		}
		catch (MatrixException exception)
		{
			MCADServerException.createException(exception.getMessage(), exception);
			exception.printStackTrace();
		}
		return newLst;
	}

	private String checkForAlreadyReplaced(Context context, String parentId, String childId) throws MCADException
	{	
		int l = 0;
		String remark = "";
		try
			{
				BusinessObject parentBus	= new BusinessObject(parentId);
				parentBus.open(context);

				BusinessObject childBus 	= new BusinessObject(childId);
				childBus.open(context);

				Relationship rel = null;

				RelationshipList relList = parentBus.getAllRelationship(context);
				for (l = 0; l < relList.size(); l++)
				{				
					rel	= (Relationship)relList.elementAt(l);
					
					BusinessObject tempBus = new BusinessObject(rel.getTo());					
					tempBus.open(context);
					if(tempBus.equals(childBus))
					{   
						tempBus.close(context);
						break;
					}

					tempBus = new BusinessObject(rel.getFrom());
					tempBus.open(context);

					if(tempBus.equals(childBus))
					{										
						tempBus.close(context);
						break;
					}
					tempBus.close(context);					
				}

				if(l < relList.size())
				{									
					String relModificationStatusAttName = MCADMxUtil.getActualNameForAEFData(context,"attribute_RelationshipModificationStatusinMatrix");

					AttributeList attrList = rel.getAttributeValues(context);
					for (l = 0; l < attrList.size(); l++)
					{
						Attribute relModificationStatusAtt 	= (Attribute)attrList.elementAt(l);
						if(relModificationStatusAttName.equals(relModificationStatusAtt.getName()))
						{					
							if(relModificationStatusAtt.getValue() != null && "deleted".equalsIgnoreCase(relModificationStatusAtt.getValue()) || "new".equalsIgnoreCase(relModificationStatusAtt.getValue()))
							{
								remark = serverResourceBundle.getString("mcadIntegration.Server.Message.ReplaceOperationAlreadyDone");												
							}
							break;
						}
					}
				}
			}
		catch (Exception exception)
		{
			MCADServerException.createException(exception.getMessage(), exception);
			exception.printStackTrace();
		}

		return remark;

	}

	public Hashtable getWhereUsedChildrenVsParentsSelection(Context _context, String[] args) throws Exception
	{
		//The parameter map passed from the page(IEFReplaceResultsTableContent)
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		
		//Get the user selected rows.
		String rowsSelection	= (String) paramMap.get("selectedRows");
		
		String languageStr		= (String) paramMap.get("languageStr");
		localeLanguage			= (String) paramMap.get("LocaleLanguage");
		integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");
		
		serverResourceBundle	= new MCADServerResourceBundle(localeLanguage);		
		cache					= new IEFGlobalCache();		
		util					= new MCADMxUtil(_context, serverResourceBundle, cache);
		
		String integration				= (String) paramMap.get("IntegrationName");
		MCADGlobalConfigObject globConf = (MCADGlobalConfigObject) integrationNameGCOTable.get(integration);
		String currentObjId				= (String) paramMap.get("objectId");
				
		BusinessObject inBus	= new BusinessObject(currentObjId);
		inBus.open(_context);
		
		//Create the selected object list from the page
		MapList objectList		= new MapList();
		
		//Create the list of the user selected objects
		Vector usrSelObj		= new Vector();

		//Split the rows into bus object ids
		StringTokenizer st = new StringTokenizer(rowsSelection, "|");
		while (st.hasMoreTokens())
		{
			//Token is the bus id
			String token = st.nextToken();
			usrSelObj.add(token);

			//Create a map of object id and the token.
			HashMap objectDetails = new HashMap();
			objectDetails.put("id", token);

			objectList.add(objectDetails);
		}

		//Create the list of user selected unduplicate bus ids
		//The where used parents page will list duplicate parents.
		//We need to dodge these bugs and create a list of 
		//unduplicate bus objects.
		Vector unduplicateWhereUsedParents = new Vector();
		for (int i = 0; i < usrSelObj.size(); i++)
		{
			//Get each user selected bus id
			String objId = (String) usrSelObj.get(i);
			
			BusinessObject itemBiz = new BusinessObject(objId);
			itemBiz.open(_context);

			//Get the list of all the major+ minor for the user selected design.
			BusinessObjectList lst = getRevisionObjects(_context, itemBiz);
			itemBiz.close(_context);

            //Create the unduplicated list of user selected minors + major.
			for (int l = 0; l < lst.size(); l++)
			{
				BusinessObject obj	= lst.getElement(l);
				String bizId		= obj.getObjectId();
				if (unduplicateWhereUsedParents.contains(bizId) == false)
				{
					unduplicateWhereUsedParents.add(bizId);
				}
			}
		}

		//Get the current object ids(minors + major) i.e. the one which will get replaced.
		MCADServerGeneralUtil _generalUtil		= new MCADServerGeneralUtil(_context, globConf, serverResourceBundle, cache);
    	BusinessObjectList revObjectsForRemoval = getRevisionObjects(_context, inBus);
    	BusinessObjectList revObjects			= null;   	
    	boolean isFinalized						= _generalUtil.isBusObjectFinalized(_context, inBus);

    	if(!isFinalized)
    	{
    		BusinessObject majorObj = util.getMajorObject(_context, inBus);
    		if(null!=majorObj)
    		{
    			majorObj.open(_context);
    			revObjects = getRemovedBusObjectsList(_context, majorObj,revObjectsForRemoval);
    			majorObj.close(_context);
    		}
    		else
    		{
    			revObjects = getRemovedBusObjectsList(_context, inBus,revObjectsForRemoval);
    		}
    	}
    	else
    	{
    		revObjects = revObjectsForRemoval;
    	}
    	
		inBus.close(_context);

		String[] init				= new String[]{};
		
		//Get the major+minors of the current object id from the object list.
		//Get the list of all the assemblies where the minors+major objects of the current object is used.
		Hashtable childVsParent		= new Hashtable();
		Hashtable parentVsRemark	= null;

		for (int j = 0; j < revObjects.size(); j++)
		{
			//Get the minor/major from the list.
			BusinessObject obj		= revObjects.getElement(j);
			HashMap objectDetails	= new HashMap();

			//Get the object id
			String currObjId		= obj.getObjectId();
			
			//Create parameter map for the JPO DSCWhereUsed
			paramMap.put("objectId", currObjId);
			paramMap.put("relationship", MCADMxUtil.getActualNameForAEFData(_context, "relationship_CADSubComponent"));
			paramMap.put("end", "to");
			paramMap.put("instanceName", "");
			paramMap.put("languageStr", languageStr);
			paramMap.put("filterLevel", "UpTo:1");
			paramMap.put("filterDesignVersion", "");
			
			String integName			= (String) paramMap.get("IntegrationName");
			MCADGlobalConfigObject gco	= (MCADGlobalConfigObject) integrationNameGCOTable.get(integName);
			
			paramMap.put("GCO", gco);

			//Get the list of the where used parents for the current design.
			MapList whereUsedObjectList = (MapList) JPO.invoke(_context, "DSCWhereUsed", init, "getList", JPO.packArgs(paramMap), MapList.class);

			//The list of parents belong to the current minor
			Vector parentList	= new Vector();
			Vector parentRemark = new Vector();
			
			//For the current design, get the list of parents and check if 
			//the current design's (major+minor) object's parents are seen
			//in the list of the where used parents.

			//Create a map of the parent Vs the remark.
			for (int k = 0; k < whereUsedObjectList.size(); k++)
			{
				parentVsRemark	= new Hashtable();
				Map objMap		= (Map) whereUsedObjectList.get(k);
				
				//Get the where used parent id
				String parentObjId = (String) objMap.get("id");
				
				//Check if this object id matches with any one
				//in the list of unduplicated user selected minors.
				if (unduplicateWhereUsedParents.contains(parentObjId))
				{
					if (parentList.contains(parentObjId) == false)
					{
						//Add to the list of parents
						parentList.add(parentObjId);

						String remark = checkForAlreadyReplaced(_context,parentObjId, currObjId);
						if("".equals(remark))
							remark = checkForLockFinalize(_context, parentObjId);
												
						//Add the remark for the locked / finalized parent.
						parentVsRemark.put(parentObjId, remark);
						parentRemark.add(parentVsRemark);
						
						childVsParent.put(currObjId, parentRemark);
					}
				}
			}	
		}

		return childVsParent;
	}
}

