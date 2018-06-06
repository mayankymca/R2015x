/* 
** emxRMTConnectBase
**
** Copyright (c) 2007 MatrixOne, Inc.
**
** All Rights Reserved.
** This program contains proprietary and trade secret information of
** MatrixOne, Inc.  Copyright notice is precautionary only and does
** not evidence any actual or intended publication of such program.
**
*/

/*
Change History:
Date       Change By  Release   Bug/Functionality        Details
-----------------------------------------------------------------------------------------------------------------------------
File Creation (17-Aug-14)
17-Aug-14  HAT1 ZUD   V6R2016x  IR-512402-3DEXPERIENCER2016x   R419-STP: Application should support creation of coverage link between two requirements with the upstream requirement being in a Released state.

*/

import java.util.Hashtable;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.TreeOrderUtil;
import com.matrixone.apps.requirements.RequirementsUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

public class emxRMTConnectBase_mxJPO extends emxDomainObject_mxJPO
{	
	public emxRMTConnectBase_mxJPO(Context context, String[] args)
			throws Exception {
		super(context, args);
		// TODO Auto-generated constructor stub
	}

	/**
	   * Method
	   * @param context           the eMatrix <code>Context</code> object
	   * @param args              Derived Requirement relationship id.
	   * @return Relationship ID bw Parent and Derived Requirement.
	   * @throws Throwable 
	   * @throws Exception if the operation fails
	   * @since RequirementsManagement V6R2016x
	   */
	public static StringList createDerivationLinks(Context context, String[] args) throws Exception
	{
		StringList objectRelId = null;
		try {
			String[] IDs = (String[])JPO.unpackArgs(args);
			String objIdFrom = IDs[0];
			String objIdTo   = IDs[1];
			String strNewOrder   = IDs[2];
			
			DomainObject objFrom = DomainObject.newInstance(context, objIdFrom);
			DomainObject objTo   = DomainObject.newInstance(context, objIdTo);
			
			StringList relselect = new StringList();
			relselect.add(DomainRelationship.SELECT_ID);
			
			//Create Derived Requirement relationship.
			DomainRelationship newRel = DomainRelationship.connect(context, objFrom, RequirementsUtil.getDerivedRequirementRelationship(context), objTo);
			Hashtable RelInfo			= newRel.getRelationshipData(context, relselect);
			objectRelId = (StringList)RelInfo.get(DomainRelationship.SELECT_ID);

			//Setting TreeOrder attribute.
			newRel.setAttributeValue(context, "Sequence Order" , strNewOrder );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		return objectRelId;
	}

	  /**
	   * Method
	   * @param context           the eMatrix <code>Context</code> object
	   * @param args              Derived Requirement relationship id.
	   * @return Message if the relationship object is deleted.
	   * @throws Throwable 
	   * @throws Exception if the operation fails
	   * @since RequirementsManagement V6R2018x
	   */
	public static String deleteDerivationLinks(Context context, String[] args) throws Exception
	{
		String[] relID = (String[])JPO.unpackArgs(args);
		String relObjId = relID[0];
		
		try 
		{
			DomainRelationship.disconnect(context, relObjId, true);
			return "Derivation link is removed.";
		} 
		catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}
	}
	
}
