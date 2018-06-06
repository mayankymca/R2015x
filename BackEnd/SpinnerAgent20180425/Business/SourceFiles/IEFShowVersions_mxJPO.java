/*
**  IEFShowVersions
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to check if versioning flag in GCO is enabled.
*/

import java.util.HashMap;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class IEFShowVersions_mxJPO
{
    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object.
     * @param args holds no arguments.
     * @throws Exception if the operation fails.
     */
    public IEFShowVersions_mxJPO (Context context, String[] args)
        throws Exception
    {
    }

	public Boolean isVersioningEnabled(Context context,String[] args) throws Exception
	{
		boolean isVersioningEnabled = true;

		try
		{
			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
			String objectId		= (String) paramMap.get("objectId");
			String language		= (String) paramMap.get("languageStr");

			IEFIntegAccessUtil util		= new IEFIntegAccessUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
			String integrationName  = util.getIntegrationName(context,objectId);
			
			if(!util.getAssignedIntegrations(context).contains(integrationName))
				return new Boolean(false);
				
			IEFSimpleConfigObject simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, integrationName);
			String createVersionObjectsAttr = MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-CreateVersionObjects");
			String createVersionObjectsFlag = simpleGCO.getConfigAttributeValue(createVersionObjectsAttr);

			if(createVersionObjectsFlag.equalsIgnoreCase("false"))
				isVersioningEnabled = false;
			else
				isVersioningEnabled = true;
		}
		catch(Throwable e)
		{
		}

		return new Boolean(isVersioningEnabled);
	}
}

