/**
 * IEFShowInstances.java
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 */
 
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.apps.domain.util.MapList;

public class IEFShowInstances_mxJPO
{
	private MCADGlobalConfigObject globalConfigObject		= null;
	private MCADServerResourceBundle serverResourceBundle	= null;
	private	IEFGlobalCache cache							= null;
	private MCADMxUtil util                                 = null;
	private MCADServerGeneralUtil serverGeneralUtil			= null;
    private HashMap integrationNameGCOTable					= null;
	private String localeLanguage							= null;

	public IEFShowInstances_mxJPO(Context context, String[] args) throws Exception
	{
	}

	public Object getHtmlString(Context context, String[] args) throws Exception
	{

		Vector columnCellContentList	= new Vector();

		HashMap paramMap				= (HashMap)JPO.unpackArgs(args);  
		MapList objectsList				= (MapList)paramMap.get("objectList");		
		HashMap objectIDinstanceListMap	= (HashMap)paramMap.get("InstanceList");		
		localeLanguage					= (String)paramMap.get("LocaleLanguage");
		integrationNameGCOTable			= (HashMap)paramMap.get("GCOTable");
	
		serverResourceBundle	= new MCADServerResourceBundle(localeLanguage);
		cache					= new IEFGlobalCache();
		util					= new MCADMxUtil(context, serverResourceBundle, cache);
		serverGeneralUtil		= new MCADServerGeneralUtil(context, globalConfigObject, serverResourceBundle, cache);
		
		for(int i =0 ; i<objectsList.size(); i++)
		{
            try
			{			
				HashMap objDetails		= (HashMap)objectsList.get(i);
				String objectID			= (String)objDetails.get("id");
				String integrationName	= util.getIntegrationName(context, objectID);
									
				if(integrationName != null && integrationNameGCOTable.containsKey(integrationName))
				{
					globalConfigObject		= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
					if (globalConfigObject == null)
					{
						return columnCellContentList;
					}

					StringBuffer htmlBuffer = new StringBuffer();
				
					BusinessObject busObj = new BusinessObject(objectID);
					busObj.open(context);
					String boType		= busObj.getTypeName();
					busObj.close(context);
					  
					String instancesList =(String)objectIDinstanceListMap.get(objectID);
					StringTokenizer instancesListTokens = new StringTokenizer(instancesList, "|");
					while(instancesListTokens.hasMoreTokens())
					{					     
						String instanceName			= instancesListTokens.nextToken();
						String encodedInstanceName	= MCADUrlUtil.hexEncode(instanceName);

						String url			= "../integrations/MCADInstanceDetailsFS.jsp?busId=" + objectID + "&instanceName=" + encodedInstanceName + "&targetFrame=popup";
						String urlCommand	= "javascript:showModalDialog('" + url + "', '" + 600 + "', '" + 600 + "', 'false' , 'popup')";
						
						htmlBuffer.append(getFeatureIconContent(urlCommand, instanceName, ""));					    
					}
						
					columnCellContentList.add(htmlBuffer.toString());
				} 
			}
			catch(Exception e)
			{
            }
        }

		return columnCellContentList;
	}

	private String getFeatureIconContent(String href, String featureImage, String toolTop)
	{
	    StringBuffer featureIconContent = new StringBuffer();

        featureIconContent.append("<a href=\"");
		featureIconContent.append(href);
		featureIconContent.append("\">");
		featureIconContent.append(featureImage);
		featureIconContent.append("<BR> </a>");

		return featureIconContent.toString();
	}
}
