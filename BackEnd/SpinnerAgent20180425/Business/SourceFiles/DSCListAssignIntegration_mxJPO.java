/*
** DSCListAssignIntegration.java
** Created on Jun 28, 2007
** Dassault Systemes, 1993  2007. All rights reserved.
** All Rights Reserved
** This program contains proprietary and trade secret information of
** Dassault Systemes.  Copyright notice is precautionary only and does
** not evidence any actual or intended publication of such program.
*/

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class DSCListAssignIntegration_mxJPO
{
	private MCADServerResourceBundle	serverResourceBundle	= null;
	private IEFGlobalCache				cache					= null;
	private MCADMxUtil					mxUtil					= null;

	/**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails     
     */
	public DSCListAssignIntegration_mxJPO(Context context, String[] args)	throws Exception
	{		
	}

    /**
     * This method is executed if a specific method is not specified.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @return int 0, status code.
     * @throws Exception if the operation fails     
     */
    public int mxMain(Context context, String[] args)
        throws Exception
    {
        if (!context.isConnected())
            throw new Exception("not supported on desktop client");
        return 0;
    }

	public HashMap getAssignIntegrations(Context context, String[] args) throws Exception
	{
		HashMap tempMap = new HashMap();

		HashMap programMap	= (HashMap) JPO.unpackArgs(args);
		HashMap requestMap	= (HashMap) programMap.get("requestMap");
		HashMap paramMap	= (HashMap) programMap.get("paramMap");

		String integrationName	= (String) requestMap.get("integrationName");
		String languageStr		= (String) paramMap.get("languageStr");
		
		StringList fieldRangeValues			= new StringList();
		StringList fieldDisplayRangeValues	= new StringList();

		serverResourceBundle	 = new MCADServerResourceBundle(languageStr);
		cache					 = new IEFGlobalCache();
		mxUtil					 = new MCADMxUtil(context, serverResourceBundle, cache);

		IEFSimpleConfigObject simpleLCO	= IEFSimpleConfigObject.getSimpleLCO(context);
		Hashtable integNameGCOMapping	= simpleLCO.getAttributeAsHashtable(MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping"), "\n", "|");

		Enumeration integrationNames	= integNameGCOMapping.keys();

		while(integrationNames.hasMoreElements())
		{
			String integName	= (String)integrationNames.nextElement();

			if(integrationName != null && !"".equals(integrationName) && integrationName.equals(integName))
			{
				fieldRangeValues.add(0, integName);
				fieldDisplayRangeValues.add(0, integName);
			}
			else
			{
				fieldRangeValues.addElement(integName);
				fieldDisplayRangeValues.addElement(integName);
			}
		}

		tempMap.put("field_choices", fieldRangeValues);
		tempMap.put("field_display_choices", fieldDisplayRangeValues);
		return tempMap;
	}
}

