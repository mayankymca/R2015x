/**
 * DSCReplace.java
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 */

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectAttributes;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.util.MatrixException;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

//  ${CLASSNAME} DSCReplace
public class DSCReplace_mxJPO
{
	//Map that holds the GCO Table Vs the integration name.
	private HashMap integrationNameGCOTable = null;
	private MCADServerResourceBundle serverResourceBundle = null;
	//Utilities
	private MCADMxUtil util = null;
	private MCADServerGeneralUtil _generalUtil = null;
	//Cache and local language
	private IEFGlobalCache cache = null;
	private String localeLanguage = null;
	//List of the bus objects that the user selected on the page.

	/**
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public DSCReplace_mxJPO (Context context, String[] args) throws Exception
	{

	}

	/** Set the attribute on the relationship
	 * @param childParentRel The relationship between the child and the parent.
	 * @param attribName The attribute name.
	 * @param attrValue  The attribute value.
	 */
	public void setRelAttribute(Context _context, Relationship childParentRel, String attribName,
			String attrValue)
	{
		try
		{
			childParentRel.open(_context);
			AttributeList childParentAttr = childParentRel
					.getAttributes(_context);
			for (int childParAttr = 0; childParAttr < childParentAttr.size(); childParAttr++)
			{
				Attribute attrib = (Attribute) childParentAttr
						.get(childParAttr);
				if (attrib.getName().compareTo(attribName) == 0)
				{
					util.setRelationshipAttributeValue(_context, childParentRel,
							attribName, attrValue);
				}

			}
			childParentRel.close(_context);
		}
		catch (MatrixException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (MCADException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * @param busID The bus id(object) from which the attributes are obtained.
	 * @param attribName The name for which the value will be queried.
	 * @param hidden     True for retrieving hidden values other wise false.
	 * @return
	 */
	public String getBusAttribute(Context _context, String busID,String attribName,boolean hidden)
	{
		String attribValue = "";
		try
		{
			BusinessObject busObj = new BusinessObject(busID);
			busObj.open(_context);

			BusinessObjectAttributes objAttr = null;
			objAttr = busObj.getAttributes(_context);
			AttributeList busAttribLst = null;
			if (hidden == true)
			{
				busAttribLst = busObj.getAttributeValues(_context, hidden);
			}
			else
			{
				busAttribLst = objAttr.getAttributes();
			}

			for (int i = 0; i < busAttribLst.size(); i++)
			{
				Attribute attrib = (Attribute) busAttribLst.get(i);
				if (attrib.getName().compareToIgnoreCase(attribName) == 0)
				{
					attribValue =  attrib.getValue();
				}
			}
			busObj.close(_context);
		}
		catch (MatrixException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return attribValue;
	}
	/** Set the attributes on the business object.
	 * @param busID       The business object id on which the attributes will be set.
	 * @param attribName  The attribute name
	 * @param attribValue The attribute value
	 * @param hidden      If true the hidden attributes can be set
	 *                    If false the hidden attributes cannot be set.
	 */
	public void setBusAttribute(Context context, String busID, String attribName,
			                    String attribValue, boolean hidden)
	{
		try
		{
			BusinessObject busObj = new BusinessObject(busID);
			busObj.open(context);

			BusinessObjectAttributes objAttr = null;
			objAttr = busObj.getAttributes(context);
			AttributeList busAttribLst = null;
			if (hidden == true)
			{
				busAttribLst = busObj.getAttributeValues(context, hidden);
			}
			else
			{
				busAttribLst = objAttr.getAttributes();
			}

			for (int i = 0; i < busAttribLst.size(); i++)
			{
				Attribute attrib = (Attribute) busAttribLst.get(i);
				if (attrib.getName().compareToIgnoreCase(attribName) == 0)
				{
					util.setAttributeOnBusObject(context, busObj, attribName,
							attribValue);
				}
			}
			busObj.close(context);
		}
		catch (MatrixException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (MCADException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * @param relID The relationship object from which the attribute value is queried.
	 * @param attribName The attribute name for which the value is fetched.
	 * @param hidden "true" if hidden attributes are to be searched OR false if hidden a
	 *               attributes are to be ignored.
	 * @return  The attribute value for the attribName
	 */
	private String getRelAttribute(Context context, Relationship relID, String attribName,boolean hidden)
	{
		String attribValue = "";
		try
		{
			relID.open(context);

			AttributeList attribList = relID.getAttributeValues(context, hidden);
			for(int i=0;i<attribList.size();i++)
			{
				Attribute attrib = (Attribute)attribList.get(i);
				if (attrib.getName().compareToIgnoreCase(attribName) == 0 )
				{
					attribValue = attrib.getValue();
				}
			}

			relID.close(context);

		}
		catch (MatrixException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return attribValue;

	}
	/** Internal funtion for setting the attributes on business objects.
	 * @param currentParent The currentParent on which attributes are set.
	 * @param child         The child(old) of the parent which got replaced.
	 * @param replaceWith   The replaceWith object on which attributes are set.
	 * @param relID         The relationship between replaceWith and the parent.
	 * @param oldRelD       The old relationship between parent and old child.
	 */
	private void modifyRelBusAttributes(Context context, String currentParent, String child,
			                           String replaceWith, Relationship relID,
									   Relationship oldRelD)
	{
		try
		{
			String modifiedInMatrix = MCADMxUtil.getActualNameForAEFData(context,"attribute_ModifiedinMatrix");
			setBusAttribute(context, currentParent, modifiedInMatrix, "true", false);

			//For replacement alert set the attribute on the parent.
			String isReplacementDone = MCADMxUtil.getActualNameForAEFData(context,"attribute_DSC-IsReplacementDone");
			setBusAttribute(context, currentParent,isReplacementDone, "true", true);

			String newlyCreatedInMatrix = MCADMxUtil.getActualNameForAEFData(context,"attribute_NewlyCreatedinMatrix");
			setBusAttribute(context, replaceWith, newlyCreatedInMatrix, "false", false);

			setBusAttribute(context, replaceWith, modifiedInMatrix, "true", false);

			setBusAttribute(context, child, modifiedInMatrix, "true", false);
			setBusAttribute(context, child, newlyCreatedInMatrix, "false", false);

			String relModStatus = MCADMxUtil.getActualNameForAEFData(context, "attribute_RelationshipModificationStatusinMatrix");
			setRelAttribute(context, relID, relModStatus,"new");

			setRelAttribute(context, oldRelD, relModStatus,"deleted");

		    String spatialLocation = MCADMxUtil.getActualNameForAEFData(context, "attribute_SpatialLocation");

			 // Copy transformation matrix from the old rel to new rel as "new transformation" attrb
		    String oldSpatialLocationValue = getRelAttribute(context, relID,spatialLocation,true);
		    String newTransformationMatrixAttrName = MCADMxUtil.getActualNameForAEFData(context, "attribute_NewTransformationMatrix");
		    setRelAttribute(context, relID, newTransformationMatrixAttrName, oldSpatialLocationValue);

			//This attribute needs to be deleted once the protocol change has been made.
			String spatialLocationValue = getRelAttribute(context, relID,spatialLocation,true);
			setRelAttribute(context, oldRelD, spatialLocation, spatialLocationValue);
			//copy the Reference Designator attribute from the relID on to old rel ID
			//and clear off the attribute from the rel ID
			String refDes = MCADMxUtil.getActualNameForAEFData(context, "attribute_ReferenceDesignator");
			String refDesVal = getRelAttribute(context, relID,refDes,true);
			setRelAttribute(context, oldRelD,refDes,refDesVal);
			//Set the value of the ref designator on the rel id as empty.
			setRelAttribute(context, relID,refDes,"");
			//Set the quantity on the old relationship id
			String quantity = MCADMxUtil.getActualNameForAEFData(context, "attribute_Quantity");
			String quantityValue = getRelAttribute(context, relID,quantity,true);
			setRelAttribute(context, oldRelD,quantity,quantityValue);
			//Get the attribute for rename from relID and set it on
			//old rel attribute. Also clear off the rename attrib on relID
			String renamedFrom = MCADMxUtil.getActualNameForAEFData(context, "attribute_Renamed From");
			String renameFromVal = getRelAttribute(context, relID,renamedFrom,true);
			//Copy the RenamedFrom attrib from current to the old
			setRelAttribute(context, oldRelD,renamedFrom,renameFromVal);
			//Clear off the rename attribute from current rel relID
			setRelAttribute(context, relID,renamedFrom,"");

			//Fix :- To be discussed.
			//Copy the rename attribute from the replaceWith on the relationship
			//Get the value of "renamed from" from replacewith.
			//String replaceeRenamedFromVal = getBusAttribute(replaceWith,renamedFrom,true);
			//Set this attribute on the relationship of the replaceWith and the parent.
			//setRelAttribute(relID,renamedFrom,replaceeRenamedFromVal);



		}
		catch (MCADException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	/**
	 * @param currentParent The business object on which we need to perform lock.
	 * @param lock          If true , the bus object is locked, if false
	 *                      bus object is unlocked.
	 */
	public void lockUnlockObject(Context _context, String currentParent, boolean lock)
	{
		try
		{
			BusinessObject currentParObj = new BusinessObject(currentParent);
			currentParObj.open(_context);
			_generalUtil.performLockUnlockOnBO(_context, currentParObj, lock);
			currentParObj.close(_context);
		}
		catch (MatrixException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (MCADException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * @param child The child object that gets connected to the parent.
	 * @param currentParent The parent object
	 * @param relationShip The type of relationship like CAD SubComponent
	 * @param isFrom false if "FROM end and true if TO end
	 * @return The relationship between the child and the parent.
	 */
	public Relationship connectChildWithParent(Context _context, String child,
			                                   String currentParent,
											   String relationShip,
											   boolean isFrom)
	{
		Relationship rel = null;
		try
		{
			BusinessObject currentParObj = new BusinessObject(currentParent);
			currentParObj.open(_context);
			BusinessObject currentObj = new BusinessObject(child);
			currentObj.open(_context);
			rel = util.ConnectBusinessObjects(_context, currentObj, currentParObj,
					relationShip, isFrom);
			currentObj.close(_context);
			currentParObj.close(_context);
		}
		catch (MatrixException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (MCADException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rel;
	}
	/**
	 * @param child The child object that will be connected to the parent.
	 * @param currentParent The parent object.
	 * @param replaceWith The object that will replace the child.
	 * @param relID The relationship between replaceWith and the parent.
	 * @param isFrom false for CAD SubComponent relationship.
	 * @param lock true For locking the parent.
	 */
	public void lockConnectAndModifyAttribtues(Context _context, String child,
			String currentParent, String replaceWith, String relID,
			boolean isFrom, boolean lock) throws MCADException
		{

			//Lock the parent
		lockUnlockObject(_context, currentParent, lock);


			// Connect:- Create a relationship between the child and the parent.
			Relationship oldRelID = connectChildWithParent(_context, child, currentParent,
											   MCADMxUtil.getActualNameForAEFData(_context, "relationship_CADSubComponent"),
														   isFrom);
			//Relationship for the replacewith and the parent
			Relationship newRelID = new Relationship(relID);
			//Modify the business attributes.
			modifyRelBusAttributes(_context, currentParent, child, replaceWith, newRelID,
					oldRelID);

		}
	/** This function will replace the current design by the user selected
	 *  replacewith design This function will be called from the page.
	 * @param context Context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public Hashtable replace(Context context, String[] args) throws Exception
	{
		//The hash table that will hold the replace results.
		Hashtable replaceResults = new Hashtable();

		//Parameter map passed from the page.
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);

		//Create the cache and utilites
		localeLanguage = (String) paramMap.get("LocaleLanguage");
		integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");
		serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
		cache = new IEFGlobalCache();

		//Get the GCO
		String integration = (String) paramMap.get("IntegrationName");
		MCADGlobalConfigObject globConf = (MCADGlobalConfigObject) integrationNameGCOTable
				.get(integration);
		//Create the utilities.
		_generalUtil = new MCADServerGeneralUtil(context, globConf,
				                                 serverResourceBundle,
												 cache
												 );

		util = new MCADMxUtil(context, serverResourceBundle, cache);

		//The map that hold the list of the children and its parent.
		//These children are those displayed on the IEFReplaceContentTable page.
		//The replacement will be done in these parents
		Hashtable childVsParent =
			(Hashtable) paramMap.get("ChildVsParentForReplace");
		 boolean hasLockAccess 	= false;

		//Get the replaceWith object that the user searched for.
		String replaceWith = (String) paramMap.get("replaceWith");

		//Get the list of the parents for the given child.
		Enumeration keys = childVsParent.keys();
		while (keys.hasMoreElements())
		{
			//Get the child.
			String child = (String) keys.nextElement();
			//The remark string will contain the result of the replace operation.
			String remark = "";
			//Get the parents for the child.
			Vector parent = (Vector) childVsParent.get(child);
			for (int par = 0; par < parent.size(); par++)
			{
				//Get the parent
			  String currentParent = (String) parent.get(par);
			  BusinessObject currentParObj = new BusinessObject(currentParent);

			  currentParObj.open(context);

        	   hasLockAccess 	=	util.hasLockAccess(currentParObj,context);
        	   currentParObj.close(context);
        	  if(hasLockAccess)
        	  {
					//Get the list of relationship between the child and its parent.					
					Vector relLst = util.findRelationShip(context,child, currentParent,
						true, "all", MCADMxUtil.getActualNameForAEFData(context, "relationship_CADSubComponent"));
					
					if(relLst.size() == 0)
					{
						remark = serverResourceBundle.getString("mcadIntegration.Server.Message.ReplaceAbsentRelationship");
						replaceResults.put(child+currentParent+replaceWith,remark);
					}
					//Loop over the relationships
					for (int i = 0; i < relLst.size(); i++)
					{
						//Get the relationship.
						String relID = (String) relLst.get(i);
						//Check for circular reference.
						//Get the children of the replaceWith and
						//check if they match with the child's list of parents.
						//Get the list of the children .
					Vector replaceWithChildren = util.getImmParentOrImmChild(context,
								replaceWith, MCADMxUtil.getActualNameForAEFData(context, "relationship_CADSubComponent"), "to");					

						//Get the list of the parents
					Vector parentList = util.getImmParentOrImmChild(context, child,
								MCADMxUtil.getActualNameForAEFData(context, "relationship_CADSubComponent"), "from");
					
						for (int k = 0; k < replaceWithChildren.size(); k++)
						{
							//Get children of the replacewith if replacewith is
							// as sub assembly.
							String replaceChild = (String) replaceWithChildren
									.get(k);
							//Check if the child matches the list of the parents
							if (parentList.contains(replaceChild))
							{
								remark = serverResourceBundle.getString("mcadIntegration.Server.Message.ReplaceOperationCircularReference");
								break;
							}
							else
							{
								//Perform the replace operation
							boolean outcome = util.replaceWithRelationshipEnd(context, relID, true,
																replaceWith);
								if(outcome == true)
								{
									remark = serverResourceBundle.getString("mcadIntegration.Server.Message.ReplaceOperationSuccess");
								}
								else
								{
									remark = serverResourceBundle.getString("mcadIntegration.Server.Message.ReplaceOperationFailed");
								}
								//Lock the parent, connect the parent with the child
								//and set the attributes on the bus objects and
								//the relationship.
							lockConnectAndModifyAttribtues(context, child,
										currentParent, replaceWith, relID, false,
										true);
							}
						}
						//For part there will be no children or
						//for empty assembly there will be no children so directly
						//replace without any check for circular referencing.
						if (replaceWithChildren.size() == 0)
						{
							//Perform actual replace

						boolean outcome = util.replaceWithRelationshipEnd(context, relID, true,
									replaceWith);
							if(outcome == true)
							{
								remark = serverResourceBundle.getString("mcadIntegration.Server.Message.ReplaceOperationSuccess");
							}
							else
							{
								remark = serverResourceBundle.getString("mcadIntegration.Server.Message.ReplaceOperationFailed");
							}
							//Lock the parent, connect the parent with the child
							//and set the attributes on the bus objects and
							//the relationship.
						lockConnectAndModifyAttribtues(context, child, currentParent,
									replaceWith, relID, false, true);
						}
						//Add the remarks in the replaceResults table.
						replaceResults.put(child + currentParent + replaceWith, remark);
				}
			  }
				else
			{
					remark = serverResourceBundle.getString("mcadIntegration.Server.Message.NoLockAccess");

					replaceResults.put(child + currentParent + replaceWith,
							remark);
				}
			}
		}

		return replaceResults;
	}

}

