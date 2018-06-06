/*
 **  DSCAttributeVisibility
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to check the visibility of the Renamed From attribute
 */

import matrix.db.Context;
import java.util.HashMap;
import matrix.db.JPO;
import matrix.db.BusinessObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;


public class DSCAttributeVisibility_mxJPO
{
	public  DSCAttributeVisibility_mxJPO()
	{
	}

	public boolean checkRenamedFromAttrVisiblity(Context context,String[] args) throws Exception
	{
		
		boolean isVisible = true;
		HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
		String objectId		= (String) paramMap.get("objectId");
		String language		= (String) paramMap.get("languageStr");
		MCADMxUtil mxUtil	 = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
		BusinessObject bo = new BusinessObject(objectId);
		String attributeName = MCADMxUtil.getActualNameForAEFData(context, "attribute_RenamedFrom");
		String attrValue = (bo.getAttributeValues(context, attributeName)).getValue();
		
		if(attrValue==null || attrValue.equals(""))
			isVisible=false; 

		return isVisible;
	}                         
}
