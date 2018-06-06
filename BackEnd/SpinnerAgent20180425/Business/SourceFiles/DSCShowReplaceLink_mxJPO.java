/**
 * DSCShowReplaceLink.java
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 */

import java.util.HashMap;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.domain.util.MapList;

//       ${CLASSNAME}         DSCShowReplaceLink
public class DSCShowReplaceLink_mxJPO
{
	private HashMap integrationNameGCOTable = null;
	private MCADServerResourceBundle serverResourceBundle = null;
	private MCADMxUtil util = null;
	private IEFGlobalCache cache = null;
	private String localeLanguage = null;
	private MapList relBusObjPageList = null;
	private MCADServerGeneralUtil _generalUtil = null;

	public DSCShowReplaceLink_mxJPO(Context context, String[] args) throws Exception
	{
	}

	/** Gets called from the MCADGenericActionProcess.jsp
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public String getURL(Context context, String[] args) throws Exception
	{
		//Unpack the JPO arguments.
		HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		relBusObjPageList = (MapList) paramMap.get("objectList");
		String currentObjectId = (String) paramMap.get("currentObjectId");

		localeLanguage = (String) paramMap.get("LocaleLanguage");
		integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

		serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
		cache = new IEFGlobalCache();
		util = new MCADMxUtil(context, serverResourceBundle, cache);

		//Create a | seperated list of selected parents from where used page
		StringBuffer whereUsedSelectedParents = new StringBuffer();

		for (int j = 0; j < relBusObjPageList.size(); j++)
		{
			HashMap objDetails = (HashMap) relBusObjPageList.get(j);
			String objectId = (String) objDetails.get("id");

			if (j == relBusObjPageList.size() - 1)
			{
				whereUsedSelectedParents.append(objectId);
			}
			else
			{
				whereUsedSelectedParents.append(objectId + "|");
			}

		}
		String replaceURL = "";
        //Get the integration name and create the replace url
		String integrationName = util.getIntegrationName(context, currentObjectId);
		if (integrationName != null
				&& integrationNameGCOTable.containsKey(integrationName))
		{
			replaceURL =
				    "IEFReplaceTableFS.jsp?header=mcadIntegration.Server.Title.Replace&funcPageName="
					+ "&relName=CAD SubComponent&end=to&objectId="
					+ currentObjectId
					+ "&integrationName="
					+ integrationName
					+ "&selectedRows=" + whereUsedSelectedParents.toString();

		}

		return replaceURL;
	}

}
