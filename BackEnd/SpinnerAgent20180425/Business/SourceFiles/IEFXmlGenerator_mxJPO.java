//IEFXmlGenerator.java

//Copyright (c) 2002 MatrixOne, Inc.
//All Rights Reserved
//This program contains proprietary and trade secret information of
//MatrixOne, Inc.  Copyright notice is precautionary only and does
//not evidence any actual or intended publication of such program.

import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.jdom.Document;
import com.matrixone.jdom.Element;
import com.matrixone.jdom.output.XMLOutputter;
import com.matrixone.apps.domain.util.FrameworkUtil;

/**
 * The <code>IEFXmlGenerator</code> class represents the JPO for
 * obtaining the MS Office integration menus
 *
 * @version AEF 10.5.0.0 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class IEFXmlGenerator_mxJPO
{
	public IEFXmlGenerator_mxJPO (Context context, String[] args) throws Exception
	{
	}

	public String buildXmlOutput(Context context, String[] args) throws Exception
	{ 
		MapList detailsMapList = (MapList) JPO.unpackArgs(args);
		return buildXmlOutput(context, detailsMapList) ;
	}

	public String buildXmlOutput(Context context, MapList detailsMapList) throws Exception
	{
		MCADServerGeneralUtil generalUtil = new MCADServerGeneralUtil();
		String xmlStringValue = null;
		try 
		{
			// create the response placeholder (root node) -- Details node
			Element elDetailsNode = new Element("Details");

			//create headerList and objectList node
			Element elAttributeHeaderNode = new Element("AttributeHeaderDetails");
			Element elActionmenuNode = new Element("ActionMenu");
			Element elObjectListNode = new Element("objectList");

			//add create headerList and objectList node to the Details node
			elDetailsNode.addContent(elAttributeHeaderNode);
			elDetailsNode.addContent(elActionmenuNode);
			elDetailsNode.addContent(elObjectListNode);

			//Iterate through the list of menu contents and get the required info
			ListIterator detailsMapItr = detailsMapList.listIterator();

			 String datetimepattern  = null;
			 String locale			= null;
	    	 String timezoneoffset   = null;
	    	
			int count = 0;
			while (detailsMapItr.hasNext())
			{
				Map detailsMap = (Map) detailsMapItr.next();
				Set mapKeySet = detailsMap.keySet();
				Iterator mapKeyIterator = mapKeySet.iterator();

				//the first map has the attribute details
				//rest of the maps contain the name/value pair

				if(count == 0)
				{
					//special fix for maintaining the order of the table columns
					//Issue : The table details are read and the header name 
					//and display name is added to a map, the map does not 
					//guarantee the order of the column names so this 
					//code makes sure that the xml generated is in the same order 
					//as the display order

					//the "columnHeaderOrder" entry in the map is the comma separated 
					//list of column names in the same order as the one defined in the 
					//table use this to generate the AttributeHeaderNode
					String columnHeaderOrder = (String) detailsMap.get("columnHeaderOrder");
					MapList actionMenuInfo 	 = (MapList) detailsMap.get("actionMenu");
					
						datetimepattern  = (String) detailsMap.get("datetimepattern");
						if(datetimepattern!=null)
						{
							locale 		  	 = (String) detailsMap.get("locale");
							timezoneoffset   = (String) detailsMap.get("timezoneoffset");
					    	
						}

					
					if(actionMenuInfo != null && actionMenuInfo.size() > 0)
					{
						String actionMenuName = (String) detailsMap.get("actionMenuName");
						
						elActionmenuNode.setAttribute("_menukey", actionMenuName);
						
						getActionMenuNode(elActionmenuNode, actionMenuInfo);
					}
					
					StringTokenizer parser = new StringTokenizer(columnHeaderOrder, ",");
					while (parser.hasMoreTokens())
					{
						String sValue = parser.nextToken();
						if (sValue != null)
						{
							String newMapKeyName = MCADUtil.removeSpace(sValue); 
							elAttributeHeaderNode.setAttribute(newMapKeyName, (String) detailsMap.get(sValue));
						}
					}
					
					//header xml node built, skip the rest of the code when count==0
					count++;
					continue;
				}

				//add the object column details
				Element elObjectNode = new Element("object");
				elObjectListNode.addContent(elObjectNode);
				while (mapKeyIterator.hasNext())
				{
					String selectPropValue=null;
					String selectProp=null;
					String mapKeyName = (String) mapKeyIterator.next();
					if(mapKeyName.equalsIgnoreCase("language"))
						continue;
					//remove space in the name
					String newMapKeyName = MCADUtil.removeSpace(mapKeyName); 
					try
					{
						String value = (String) detailsMap.get(mapKeyName);
						
						value = generalUtil.getAttributeValueForDateTimeForJPO(context,newMapKeyName,  value,  null,  false, datetimepattern, locale, timezoneoffset);
						if(value!=null && !value.equals(""))
						{
								  if (newMapKeyName.equalsIgnoreCase("type")){
											
												
												String selectValue=value.trim();
												selectProp = "emxFramework.Type." + selectValue.replace(' ', '_');
												selectPropValue = UINavigatorUtil.getI18nString(selectProp,"emxFrameworkStringResource",(String) detailsMap.get("language"));
												
									}
									else if(newMapKeyName.equalsIgnoreCase("current")){
										
									String selectValue=value.trim();
										String selectPolicy = (String)detailsMap.get("policy");
										selectProp = "emxFramework.State." + selectPolicy.replace(' ', '_') + "." + selectValue.replace(' ', '_');
										selectPropValue = UINavigatorUtil.getI18nString(selectProp, "emxFrameworkStringResource", (String) detailsMap.get("language"));
										
									}						
									else if(newMapKeyName.equalsIgnoreCase("policy"))
									{
										String selectValue=value.trim();
										
										selectProp = "emxFramework.Policy." + selectValue.replace(' ', '_');
										selectPropValue = UINavigatorUtil.getI18nString(selectProp, "emxFrameworkStringResource", (String) detailsMap.get("language"));
										
									}						
									if(selectPropValue!=null && !selectPropValue.equals(""))
									{
										if(selectPropValue.equals(selectProp)){
										}
										else
										value=selectPropValue;
									}
						}			
						String newMapKeyNameWithoutSplChars = "";
						if(newMapKeyName.contains("attribute["))
						{
							 newMapKeyNameWithoutSplChars = MCADUtil.replaceString(newMapKeyName,"attribute[", "");
							 newMapKeyNameWithoutSplChars = MCADUtil.replaceString(newMapKeyNameWithoutSplChars,"]", "");
						}
						 else
							newMapKeyNameWithoutSplChars =  newMapKeyName;
                         
						if(value != null)
							elObjectNode.setAttribute(newMapKeyNameWithoutSplChars, value);
						else
							elObjectNode.setAttribute(newMapKeyNameWithoutSplChars, "");
					}
					catch (ClassCastException ex)
					{
						StringList value = (StringList) detailsMap.get(mapKeyName);
						
						if(value != null){
							String sFileNames = FrameworkUtil.join(value, ",");
							elObjectNode.setAttribute(newMapKeyName, (String) sFileNames);
							
						}
						else{
							elObjectNode.setAttribute(newMapKeyName, "");
					}
				}
				}
				count++;
			}

			Document docResponse 		= new Document(elDetailsNode);
			docResponse.setDocType(null);

			XMLOutputter xmloResponse 	= new XMLOutputter();
			xmlStringValue = xmloResponse.outputString(docResponse);

			ContextUtil.commitTransaction(context);
		} 
		catch (Exception ex)
		{
			ContextUtil.abortTransaction(context);
			String message = ex.getMessage();
			MCADServerException.createException(message, ex);
		}

		return xmlStringValue;
	}

	private void getActionMenuNode(Element elActionmenuNode, MapList actionMenuDetails)
	{
		for(int i= 0; i < actionMenuDetails.size();i++)
		{
			Map detailsMap = (HashMap) actionMenuDetails.get(i);
			String uiType  = (String) detailsMap.get("_objectType");

			if(uiType.equalsIgnoreCase("command"))
			{
				Element elCommandNode = new Element("Command");

				elActionmenuNode.addContent(elCommandNode);

				Iterator childKeys = detailsMap.keySet().iterator();
				while(childKeys.hasNext())
				{
					Object key = childKeys.next();
					if(key instanceof String)
					{
						Object value = detailsMap.get(key);
						if(value instanceof String)
						{
							String newMapKeyName = MCADUtil.removeSpace((String) key);
							elCommandNode.setAttribute((String) newMapKeyName, (String) value);
						}
					}
				}
			}
			else if(uiType.equalsIgnoreCase("menu"))
			{
				MapList childMenuInfo = (MapList) detailsMap.get("childMenuInfo");

				if(childMenuInfo != null)
				{
					Element elchildMenuNode = new Element("ActionMenu");
					elActionmenuNode.addContent(elchildMenuNode);
					getActionMenuNode(elchildMenuNode, childMenuInfo);

					Iterator childKeys = detailsMap.keySet().iterator();
					while(childKeys.hasNext())
					{
						Object key = childKeys.next();
						if(key instanceof String)
						{
							Object value = detailsMap.get(key);
							if(value instanceof String)
							{
								String newMapKeyName = MCADUtil.removeSpace((String) key);
								elchildMenuNode.setAttribute(newMapKeyName, (String) value);
							}
						}
					}
				}
			}
		}
	}
}
