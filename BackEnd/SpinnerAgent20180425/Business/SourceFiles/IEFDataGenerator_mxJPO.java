//IEFDataGenerator.java


//Copyright (c) 2002 MatrixOne, Inc.
//All Rights Reserved
//This program contains proprietary and trade secret information of
//MatrixOne, Inc.  Copyright notice is precautionary only and does
//not evidence any actual or intended publication of such program.

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.beans.IEFDesktopHelper;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFCache;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIComponent;
import com.matrixone.apps.framework.ui.UIMenu;
import com.matrixone.apps.framework.ui.UINavigatorUtil;

/**
 * The <code>emxOfficeIntegrationMenus</code> class represents the JPO for
 * obtaining the MS Office integration menus
 *
 * @version AEF 10.5 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class IEFDataGenerator_mxJPO
{
	private long iLevel				= 0;
	private  String sHyperlink		= "";
	private  IEFCache  _GlobalCache = new IEFGlobalCache();
	private  String[] initArgs		= null;
	Hashtable initArgsTable			= new Hashtable();
	private IEFDesktopConfigurations_mxJPO desktopConfigurations	=  null;
	/**
	 * Constructs a new emxOfficeIntegrationMenus JPO object.
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args an array of String arguments for this method
	 * @throws Exception if the operation fails
	 * @since AEF 10.5
	 */
	public IEFDataGenerator_mxJPO () throws Exception
	{

	}

	public IEFDataGenerator_mxJPO (Context context, String[] args) throws Exception
	{
		initArgs		 = args;
		initArgsTable	 = (Hashtable) JPO.unpackArgs(args);
		sHyperlink		 =  (String) initArgsTable.get("Hyperlink");
		if(sHyperlink == null)
			sHyperlink = "";

		desktopConfigurations = new IEFDesktopConfigurations_mxJPO(context, args);
	}

	public MapList getMenuComponentDetails(Context context,  String[] args) throws Exception
	{
		Hashtable argsTable		= (Hashtable) JPO.unpackArgs(args);
		String lang				= (String) argsTable.get("language");
		String symbolicMenuName	= (String) argsTable.get("symbolicMenuName");
		String rootLevel		= (String) argsTable.get("rootLevel");
		String recursive		= (String) argsTable.get("recursive");

		boolean isRootLevel		= false;
		if(rootLevel != null && rootLevel.equalsIgnoreCase("true"))
			isRootLevel = true;
		
		boolean isRecursive		= false;
		if(recursive != null && recursive.equalsIgnoreCase("true"))
			isRecursive = true;

		return getMenuComponentDetails(context, lang, symbolicMenuName, isRootLevel, isRecursive);
	}
	/**
	 * Genereates the details of the Menu being accessed
	 * Returns a maplist which has the details of the menu or command
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param lang the language used by the client machine
	 * @param menuName the name of the menu being accessed
	 * @throws Exception if the operation fails
	 * @since AEF 10.5
	 */

	public MapList getMenuComponentDetails(Context context, String lang, String symbolicMenuName, boolean rootLevel, boolean recursive) throws MatrixException
	{
		String actualCommandName 	= PropertyUtil.getSchemaProperty(context, symbolicMenuName);
		
		if(actualCommandName == null)
			throw new MatrixException("Menu " + symbolicMenuName + " is not available or the menu is not registered. Please contact your system administrator");

		//create a maplist for details fetched. This maplist will be used by
		//the IEFXmlUtility to build an XML output
		MapList menuDetailsMapList  = new MapList();

		//get the user assignments
		Vector userRoleList 		= PersonUtil.getAssignments(context);

		try
		{
			MapList menuMapList 	= new MapList();
			menuMapList 			= UIMenu.getMenu(context, actualCommandName, userRoleList);

			if ( menuMapList != null)
			{
				Iterator menuItr = menuMapList.iterator();
				int count = 0;

				while (menuItr.hasNext())
				{
					HashMap componentMap = (HashMap)menuItr.next();

					// Get component details
					String componentName = UIMenu.getName(componentMap);
					String componentLabel = UIMenu.getLabel(componentMap);
					String componentAlt = UIMenu.getAlt(componentMap);
					String componentHRef = UIMenu.getHRef(componentMap);
					String sRegisteredSuite = UIMenu.getSetting(componentMap, "Registered Suite");
					String command = UIMenu.getSetting(componentMap, "_fetchcontentcmd");
					
					HashMap commandSettings = UIMenu.getSettings(componentMap);
 /***Aceess Program  Changes****/
					HashMap oHashMap = new HashMap();
					boolean hasAccess = true;
					try{
					  hasAccess = UIMenu.hasAccess(context,(String)null,oHashMap, componentMap);
					}
					catch(Exception e)
					{
						System.out.println(e);
					}
					if(hasAccess == false)
					{
						  continue;
					}
 /***Aceess Program  Changes****/
					
					// Get the directory and resourceFileId for the Registered Suite from
					// the system.properties file
					String menuRegisteredDir = "";
					String stringResFileId = "";
					String suiteKey = "";

					if ( (sRegisteredSuite != null) && (sRegisteredSuite.trim().length() > 0 ) )
					{
						menuRegisteredDir = UINavigatorUtil.getRegisteredDirectory(sRegisteredSuite);
						stringResFileId = UINavigatorUtil.getStringResourceFileId(sRegisteredSuite);
						suiteKey = sRegisteredSuite;
					}

					// Get the sAltText with the internationalization string
					String sAltText = UINavigatorUtil.getI18nString(componentAlt, stringResFileId, lang);
					String sCommandLabel = UINavigatorUtil.getI18nString(componentLabel, stringResFileId, lang);
					String uiComponentType = null;

					if(UIMenu.isCommand(componentMap))
						uiComponentType = "Command";
					else if(UIMenu.isMenu(componentMap))
						uiComponentType = "Menu";

					String uiComponentSymbolicName = FrameworkUtil.getAliasForAdmin(context,
							uiComponentType,
							componentName,
							true);

					if(!recursive && count == 0) // folder display will not be recursive
					{
						Map headerMap = new HashMap();
						String sName  = i18nNow.getI18nString("emxComponents.Common.Name",  "emxComponentsStringResource", lang);

						MCADMxUtil _util = new MCADMxUtil(context, null,_GlobalCache);
						if(!_util.isCDMInstalled(context))
							sName = i18nNow.getI18nString("emxComponents.Common.Name",  "iefdesignCenterStringResource", lang);

						headerMap.put("name", sName);
						headerMap.put("columnHeaderOrder", "name");
						menuDetailsMapList.add(headerMap);
						count++;
					}

					Map detailsMap = new HashMap();
					detailsMap.put("name", sCommandLabel);
					detailsMap.put("_objectType", uiComponentType);
					detailsMap.put("_symbolicName", uiComponentSymbolicName);
					//for menu or commands the superType is always "admin"
					detailsMap.put("_superType", "admin");
					//Following line will be used by CSE to form a unique key.
					detailsMap.put("_keyname", sCommandLabel);
					
					if(recursive && uiComponentType.equalsIgnoreCase("Menu"))
					{
						detailsMap.put("childMenuInfo", getMenuComponentDetails(context, lang, uiComponentSymbolicName, false, recursive));
					}
					
					detailsMap.putAll(commandSettings);

					menuDetailsMapList.add(detailsMap);
				}
			}
		}
		catch (Exception ex)
		{
			throw (new MatrixException("[IEFDataGenerator:getMenuComponentDetails] EXCEPTION : " + ex.toString()) );
		}

		return menuDetailsMapList;
	}

	/**
	 * Genereates the details of the Menu being accessed
	 * Returns a maplist which has the details of the menu or command
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param lang the language used by the client machine
	 * @param symbolicCommandName the name of the command being accessed
	 * @throws Exception if the operation fails
	 * @since AEF 10.5
	 */

	public  MapList getCommandDetails(Context context, String[] args) throws MatrixException
	{
		String lang 				= args[0];
		String symbolicCommandName 	= args[1];
		String actualCommandName 	= PropertyUtil.getSchemaProperty(context, symbolicCommandName);

		if(actualCommandName == null)
			throw new MatrixException("Command " + symbolicCommandName + " is not available or the command is not registered. Please contact your system administrator");

		MapList renderedMapList = new MapList();

		try
		{
			HashMap componentMap 	= (HashMap) UIMenu.getCommand(context, actualCommandName);

			// Get component details
			String componentName 	= UIComponent.getName(componentMap);
			String componentLabel 	= UIComponent.getLabel(componentMap);
			String componentAlt 	= UIComponent.getAlt(componentMap);
			String componentHRef 	= UIComponent.getHRef(componentMap);
			String sRegisteredSuite = UIComponent.getSetting(componentMap, "Registered Suite");
			HashMap commandSettings	= UIComponent.getSettings(componentMap);
									
			String menuRegisteredDir = "";
			String stringResFileId 	 = "";
			String suiteKey = "";

			if ( (sRegisteredSuite != null) && (sRegisteredSuite.trim().length() > 0 ) )
			{
				menuRegisteredDir 	= UINavigatorUtil.getRegisteredDirectory(sRegisteredSuite);
				stringResFileId 	= UINavigatorUtil.getStringResourceFileId(sRegisteredSuite);
				suiteKey 			= sRegisteredSuite;
			}

			// Get the sAltText with the internationalization string
			String sAltText 		= UINavigatorUtil.getI18nString(componentAlt, stringResFileId, lang);
			String sCommandLabel 	= UINavigatorUtil.getI18nString(componentLabel, stringResFileId, lang);

			//get the inquiry/program name and table name from the command

			Map HRefParams 	 		= getHRefParams(context, actualCommandName,componentHRef);
			String tableName 		= (String) HRefParams.get("table");

			Hashtable argsTable1 = new Hashtable(2);
			argsTable1.put("HRefParams", HRefParams);
			argsTable1.put("args", args);

			MapList objectMapList 	= (MapList) JPO.invoke(context, "IEFUtil", initArgs, "getObjectMapList", JPO.packArgs(argsTable1), MapList.class);

			//Saved queries does not require table
			//IEFUitl.getSavedQueries generate the required data for building XML
			//skip generating data from table for command_MsoiSavedQueries
			if(args[1].equals("command_MsoiSavedQueries"))
			{
				renderedMapList = objectMapList;
			}
			else
			{
				sHyperlink = sHyperlink + "./emxTree.jsp?appendParameters=true&relId=null&parentOID=null&jsTreeID=null";
				if("programcentral".equalsIgnoreCase(menuRegisteredDir))
					sHyperlink = sHyperlink + "&treeMenu=PMCtype_ProjectVault&pmc=true";

				sHyperlink = sHyperlink + "&emxSuiteDirectory="+ menuRegisteredDir +"&suiteKey="+suiteKey;
				
				String actionMenu = IEFDesktopHelper.getActionMenuNameFromCommandParameters(HRefParams, commandSettings);
				
				if(actionMenu == null || actionMenu.trim().length() == 0)
					actionMenu = desktopConfigurations.getDefaultActionMenu(context);
				
				initArgsTable.put("actionMenuName", actionMenu);
				
				if (args.length >= 7)
				{
					if (args[6].startsWith("timeZone;"))
					{
						Map<String,String> paramListMap = new HashMap<String, String>();			
						paramListMap.put("timeZone", args[6].substring(9));
						renderedMapList = renderTable(context, tableName, lang, objectMapList, sHyperlink, paramListMap);
					}
                                        else
	                                	renderedMapList 		= renderTable(context, tableName, lang, objectMapList, sHyperlink);
				}
				else
				renderedMapList 		= renderTable(context, tableName, lang, objectMapList, sHyperlink);
				
				HashMap tableHeaderMap  = (HashMap) renderedMapList.get(0);

				tableHeaderMap.put("actionMenuName", actionMenu);
				tableHeaderMap.put("actionMenu", getActionMenu(context, lang, actionMenu));
			}
		} 
		catch (Exception ex)
		{
			System.out.println("[IEFDataGenerator.getCommandDetails] EXCEPTION : " + ex.getMessage());
			throw (new MatrixException("Exception:"+ex.getMessage()) );
		}

		return renderedMapList;
	}
	
	public MapList getActionMenu(Context context, String [] args) throws Exception
	{
		HashMap inputMap 	= (HashMap) JPO.unpackArgs(args);
		String language  	= (String) inputMap.get("language");
		String menuName  = (String)inputMap.get("menuName"); 
		
		return getActionMenu(context, language, menuName);
	}
	
	public MapList getActionMenu(Context context, String language, String actualMenuName) throws Exception
	{
		String menuSymbolicName = FrameworkUtil.getAliasForAdmin(context, "menu", actualMenuName, true);
		MapList menuInfo 		= getMenuComponentDetails(context, language, menuSymbolicName, false, true);
		
		return menuInfo;
	}
	
	/**
	 * Parse the result of "print command abc".
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param command the result of "print command abc"
	 * @return a map representing the command
	 * @since AEF 9.5.2.0
	 * @grade 0
	 */
	public  MapList evaluateUIComponent(Context context, String[] args) throws Exception
	{
		MapList renderedMapList = new MapList();
		try
		{

			//regional language used by the CSE
			String lang 					= args[0];
			String componentName 			= args[1];
			String objectId 				= args[2];
			String parentRelName 			= args[3];

			String datetimepattern  		= null;
			String locale					= null;
	    	String timezoneoffset   		= null;
	    	
	    
	    	int iArgNo = 4;
			if(args.length>4)
        	{
        		datetimepattern = args[iArgNo++];
        		if(datetimepattern!=null)
            	{
        			locale =  args[iArgNo++];
        		    timezoneoffset = args[iArgNo++];
        		   
        		}
        	}
			
			Map componentDetailsMap 		= parseUIComponent(context, lang, componentName);
			String componentHRef 			= (String) componentDetailsMap.get("href");

			Map HRefParams 					= getHRefParams(context, null,componentHRef);
			java.util.HashMap settingsMap 	= (java.util.HashMap) componentDetailsMap.get("settings");
			String sRegisteredSuite 		= (String) settingsMap.get("Registered Suite");

			String menuRegisteredDir 		= "";
			String suiteKey 				= "";

			if ( (sRegisteredSuite != null) && (sRegisteredSuite.trim().length() > 0 ) )
			{
				menuRegisteredDir = UINavigatorUtil.getRegisteredDirectory(sRegisteredSuite);
				suiteKey = sRegisteredSuite;
			}

			Hashtable argsTable1 = new Hashtable(2);
			argsTable1.put("HRefParams", HRefParams);
			argsTable1.put("args", args);
			
			IEFUtil_mxJPO iefUtil = new IEFUtil_mxJPO(context, initArgs);
			MapList objectMapList = iefUtil.getObjectMapList(context, JPO.packArgs(argsTable1));

			String tableName = (String) HRefParams.get("table");


			sHyperlink = sHyperlink + "./emxTree.jsp?appendParameters=true&relId=null&parentOID=null&jsTreeID=null";
			if("programcentral".equalsIgnoreCase(menuRegisteredDir))
				sHyperlink = sHyperlink + "&treeMenu=PMCtype_ProjectVault&pmc=true";

			sHyperlink = sHyperlink + "&emxSuiteDirectory=" + menuRegisteredDir + "&suiteKey=" + suiteKey;
			
			String actionMenu = IEFDesktopHelper.getActionMenuNameFromCommandParameters(HRefParams, settingsMap);

			if(actionMenu == null || actionMenu.trim().length() == 0)
				actionMenu = desktopConfigurations.getDefaultActionMenu(context);
				
			initArgsTable.put("actionMenuName", actionMenu);
			
			renderedMapList = renderTable(context, tableName, lang, objectMapList, sHyperlink);

			HashMap tableHeaderMap  = (HashMap) renderedMapList.get(0);

			tableHeaderMap.put("actionMenuName", actionMenu);
			tableHeaderMap.put("actionMenu", getActionMenu(context, lang, actionMenu));
			
			if(datetimepattern!=null)
	        {
				//tableHeaderMap.put("canChangeDateTimePattern", canChangeDateTimePattern);
				tableHeaderMap.put("datetimepattern", datetimepattern);
				tableHeaderMap.put("locale", locale);
				tableHeaderMap.put("timezoneoffset", timezoneoffset);
	        }
		} 
		catch (Exception ex)
		{
			System.out.println("[IEFDataGenerator.evaluateUIComponent] EXCEPTION : " + ex.getMessage());
			throw (new MatrixException("Exception:"+ex.getMessage()) );
		}
		return renderedMapList;
	}

	/**
	 * Parse the result of "print command abc".
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param command the result of "print command abc"
	 * @return a map representing the command
	 * @since AEF 9.5.2.0
	 * @grade 0
	 */
	public  HashMap parseUIComponent(Context context, String lang, String componentName) throws Exception
	{
		HashMap map = new HashMap();
		HashMap properties = new HashMap();
		HashMap settings = new HashMap();
		StringList roles = new StringList();

		String actualComponentName = PropertyUtil.getSchemaProperty(context, componentName);
		if(actualComponentName == null)
			throw new MatrixException(componentName + " is not available or it is not registered. Please contact your system administrator");

		MQLCommand mql = new MQLCommand();
		String result = null;
		try
		{
			//symbolic name should have the type of UI object
			if(componentName.indexOf("menu") != -1)
			{
				boolean eval = mql.executeCommand(context, "print menu $1" ,actualComponentName);
			}
			else if(componentName.indexOf("command") != -1)
			{
				boolean eval = mql.executeCommand(context, "print command $1",actualComponentName);
			}
		}
		catch (Exception e)
		{
			// if there was no error from mql
			if (mql.getError().length() != 0)
			{
				throw new MatrixException("IEFDataGenerator:parseUIComponent : ERROR - " + mql.getError());
			}
		}

		result = mql.getResult();
		BufferedReader in = new BufferedReader(new StringReader(mql.getResult()));
		try
		{
			int spaceIndex;
			String value;
			StringList assignList;
			StringList nameAndValue;

			map.put("type", "componentName");
			while((result = in.readLine()) != null)
			{
				result = result.trim();
				spaceIndex = result.indexOf(' ');
				value = "";

				if (spaceIndex != -1 && result.length() > spaceIndex + 1)
				{
					value = unquote(result.substring(spaceIndex + 1));
				}

				if (result.startsWith("description"))
				{
					map.put("description", value);
				}
				else if (result.startsWith("icon"))
				{
					map.put("icon", value);
				}
				else if (result.startsWith("label"))
				{
					map.put("label", value);
				}
				else if (result.startsWith("href"))
				{
					map.put("href", value);
				}
				else if (result.startsWith("alt"))
				{
					map.put("alt", value);
				}
				else if (result.startsWith("code"))
				{
					map.put("code", value);
				}
				else if (result.startsWith("input"))
				{
					map.put("input", value);
				}
				else if (result.startsWith("user"))
				{
					if (value.length() > 0)
					{
						// Get children roles/groups for the value
						assignList = PersonUtil.getChildrenUser(context, value);

						// Add the children roles/groups in the role list
						if (assignList != null && assignList.size() > 0)
						{
							roles.addAll(assignList);
						}

						// Add the value itself in the role list
						roles.addElement(value);
					}
				}
				else if (result.startsWith("property"))
				{
					try
					{
						nameAndValue = parseNameAndValue(value);
						properties.put(nameAndValue.elementAt(0), nameAndValue.elementAt(1));
					}
					catch (Exception e)
					{
						// just catch it
					}
				}
				else if (result.startsWith("setting"))
				{
					try
					{
						nameAndValue = parseNameAndValue(value);
						settings.put(nameAndValue.elementAt(0), nameAndValue.elementAt(1));
					}
					catch (Exception e)
					{
						// just catch it
					}
				}
			}
		}
		catch (Exception ex)
		{
		}

		if (roles.size() > 0)
		{
			map.put("roles", roles);
		}

		if (properties.size() > 0)
		{
			map.put("properties", properties);
		}

		if (settings.size() > 0)
		{
			map.put("settings", settings);
		}

		return (map);
	}

	/**
	 * Parse name and value from the given string.
	 *  ex. car value volkswagen
	 *
	 * @param the strings containg the name and value
	 * @return the name and value strings.
	 * @since AEF 9.5.0.0
	 * @grade 0
	 */
	String VALUE = " value " ;

	int valueLength = VALUE.length();
	protected  StringList parseNameAndValue(String text)
	{
		StringList nameAndValue = new StringList(2);
		int start = text.indexOf(VALUE);

		nameAndValue.addElement(text.substring(0, start));
		nameAndValue.addElement(text.substring(start + valueLength));
		return (nameAndValue);
	}

	/**
	 * Genereates the details of the Menu being accessed
	 * Returns a maplist which has the details of the menu or command
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param lang the language used by the client machine
	 * @param symbolicCommandName the name of the command being accessed
	 * @throws Exception if the operation fails
	 * @since AEF 10.5
	 */

	public  MapList evaluateRelationshipAsFolders(Context context, String[] args) throws MatrixException
	{
		//create a maplist for details fetched. This maplist will be used by
		//the IEFXmlUtility to build an XML output
		MapList menuDetailsMapList = new MapList();

		String lang = args[0];
		String symbolicMenuName = args[1];
		String sTypeName = args[2];
		String sObjectName = args[3];
		String sRev	= args[4];

		String actualCommandName = PropertyUtil.getSchemaProperty(context, symbolicMenuName);
		if(actualCommandName == null)
		{
			throw new MatrixException("Menu " + symbolicMenuName + " is not available or the menu is not registered. Please contact your system administrator");
		}

		//get the user assignments
		Vector userRoleList = PersonUtil.getAssignments(context);

		try
		{
			MapList menuMapList = new MapList();
			menuMapList = UIMenu.getMenu(context, actualCommandName, userRoleList);

			if ( menuMapList != null)
			{
				Iterator menuItr = menuMapList.iterator();
				int count = 0;
				while (menuItr.hasNext())
				{
					HashMap componentMap = (HashMap)menuItr.next();

					// Get component details
					String componentName = UIMenu.getName(componentMap);
					String componentLabel = UIMenu.getLabel(componentMap);
					String componentAlt = UIMenu.getAlt(componentMap);
					String componentHRef = UIMenu.getHRef(componentMap);
					String sRegisteredSuite = UIMenu.getSetting(componentMap, "Registered Suite");

					// Get resourceFileId for the Registered Suite from
					// the system.properties file
					String stringResFileId = "";

					if ( (sRegisteredSuite != null) && (sRegisteredSuite.trim().length() > 0 ) )
						stringResFileId = UINavigatorUtil.getStringResourceFileId(sRegisteredSuite);

					// Get the sAltText with the internationalization string
					String sCommandLabel = UINavigatorUtil.getI18nString(componentLabel, stringResFileId, lang);
					String uiComponentType = null;

					if(UIMenu.isCommand(componentMap))
						uiComponentType = "Command";
					else if(UIMenu.isMenu(componentMap))
						uiComponentType = "Menu";

					String uiComponentSymbolicName = FrameworkUtil.getAliasForAdmin(context,
							uiComponentType,
							componentName,
							true);

					if(count == 0)
					{
						Map headerMap = new HashMap();
						String sName = i18nNow.getI18nString("emxComponents.Common.Name",  "emxComponentsStringResource", lang);

						com.matrixone.MCADIntegration.server.beans.MCADMxUtil _util = new com.matrixone.MCADIntegration.server.beans.MCADMxUtil(context, null,_GlobalCache);
						if(!_util.isCDMInstalled(context))
							sName = i18nNow.getI18nString("emxComponents.Common.Name",  "IEFDesignCenterStringResource", lang);

						headerMap.put("name", sName);
						headerMap.put("columnHeaderOrder", "name");
						menuDetailsMapList.add(headerMap);
						count++;
					}

					String sAccess = "";
					BusinessObject obj = new BusinessObject(sTypeName, sObjectName, sRev, "");
					String busId = obj.getObjectId(context);
					com.matrixone.apps.domain.DomainObject domainObject = new com.matrixone.apps.domain.DomainObject();
					domainObject.setId(busId);
					StringList busSelect = new StringList();
					busSelect.add("current.access");
					Map accessMap = domainObject.getInfo(context, busSelect);
					sAccess = (String)accessMap.get("current.access");
					if(sAccess == null)
						sAccess = "";

					Map detailsMap = new HashMap();
					detailsMap.put("name", sCommandLabel);

					detailsMap.put("_symbolicName", "rel_"+uiComponentSymbolicName);
					detailsMap.put("_objectType",  sTypeName);
					detailsMap.put("_objectRev",  sRev);
					detailsMap.put("_objectName", sObjectName);
					detailsMap.put("_superType","admin");
					detailsMap.put("_objectAccess", sAccess);
					//Following line will be used by CSE to form a unique key.
					//detailsMap.put("_keyname", sCommandLabel + "_@" + busId);iLevel
					//detailsMap.put("_keyname", sCommandLabel + "_@" + iLevel);
					long lHashCode = busId.hashCode();
					detailsMap.put("_keyname", sCommandLabel + "_@" + lHashCode);
					iLevel++;

					//Following is required for folder navigation
					if((componentLabel != null) && (componentLabel.endsWith(".Specification") || componentLabel.endsWith(".ReferenceDocument") || componentLabel.endsWith(".ReferenceDocuments") || componentLabel.endsWith(".ClassifiedItems")))
					{
						Map HRefParams = getHRefParams(context, null, componentHRef);
						String parentRelName = (String)HRefParams.get("parentRelName");
						
						if(parentRelName == null || parentRelName.length() == 0)
						{
							parentRelName = (String)HRefParams.get("rel");
							if(parentRelName == null || parentRelName.length() == 0)
							{
								parentRelName = (String)HRefParams.get("relName");
							}
						}

						if(parentRelName == null || parentRelName.length() == 0)
						{
							if(componentLabel.endsWith(".ReferenceDocument"))
								parentRelName = "relationship_ReferenceDocument";
						}
						
						detailsMap.put("_superType","container");
						detailsMap.put("relwithparent", parentRelName);
					}

					menuDetailsMapList.add(detailsMap);
				}
			}
		}
		catch (Exception ex)
		{
			throw (new MatrixException("[IEFDataGenerator:evaluateRelationshipAsFolders] EXCEPTION : " + ex.toString()) );
		}
		return menuDetailsMapList;
	}

	/**
	 * Remove beginning and ending quotes from a string.
	 *
	 * @param quotedString the string containing the quotes
	 * @return an unquoted string
	 * @since AEF 9.5.0.0
	 * @grade 0
	 */
	protected  String unquote(String quotedString)
	{
		String unquotedString = quotedString;

		if (quotedString != null)
		{
			int strLength = quotedString.length();
			if (strLength > 2 &&
					(quotedString.startsWith("\"") || quotedString.startsWith("\'")) &&
					(quotedString.endsWith("\"") || quotedString.endsWith("\'")))
			{
				unquotedString = quotedString.substring(1, strLength - 1);
			}
		}

		return (unquotedString);
	}

	/**
	 * Remove beginning and ending quotes from a string.
	 *
	 * @param quotedString the string containing the quotes
	 * @return an unquoted string
	 * @since AEF 9.5.0.0
	 * @grade 0
	 */
	public  MapList getProjectFolders(Context context, String[] args) throws MatrixException
	{
		MapList folderMapList = new MapList();
		MapList renderedMapList = new MapList();
		String result = null;
		String lang = args[0];
		try
		{
			folderMapList = (MapList) JPO.invoke(context, "IEFPMCUtil", null, "getProjectFolders", args, MapList.class);   

			sHyperlink = sHyperlink + "./emxTree.jsp?appendParameters=true&relId=null&parentOID=null&jsTreeID=null";
			sHyperlink = sHyperlink + "&treeMenu=PMCtype_ProjectVault&emxSuiteDirectory=programcentral&suiteKey=ProgramCentral";

			renderedMapList = renderTable(context, "table_IEFDesktopObjectSummary", lang, folderMapList, sHyperlink);
		}
		catch (Exception ex)
		{
			throw (new MatrixException("IEFDataGenerator:getProejctFolders : " + ex.toString()) );
		}
		finally
		{
			return renderedMapList;
		}
	}

	private Map getHRefParams(Context context, String actualCommandName, String componentHRef) throws Exception
	{
		HashMap argsMap = new HashMap(2);
		argsMap.put("commandName", actualCommandName);
		argsMap.put("commandHRef", componentHRef);

		Map HRefParams = (Map)JPO.invoke(context, "IEFUtil", null, "getHRefParams", JPO.packArgs(argsMap), Map.class);
		return HRefParams;
	}

	private MapList renderTable(Context context, String tableName, String language, MapList objectMapList, String sHyperlink) throws Exception
	{
		Hashtable argsTable = new Hashtable(4);
		argsTable.put("tableName", tableName);
		argsTable.put("language", language);
		argsTable.put("inputMapList", objectMapList);
		argsTable.put("hyperlink", sHyperlink);
		initArgs = JPO.packArgs(initArgsTable);
		
	    IEFUITableUtility_mxJPO iefUITableUtilty = new IEFUITableUtility_mxJPO(context, initArgs);
		MapList renderedMapList = iefUITableUtilty.renderTable(context, JPO.packArgs(argsTable));
		return renderedMapList;
	}

	private MapList renderTable(Context context, String tableName, String language, MapList objectMapList, String sHyperlink, Map<String,String> paramListMap) throws Exception
	{
		Hashtable argsTable = new Hashtable(4);
		argsTable.put("tableName", tableName);
		argsTable.put("language", language);
		argsTable.put("inputMapList", objectMapList);
		argsTable.put("hyperlink", sHyperlink);
		argsTable.put("paramListMap", paramListMap);
		initArgs = JPO.packArgs(initArgsTable);
		MapList renderedMapList = (MapList)JPO.invoke(context, "IEFUITableUtility", initArgs, "renderTable", JPO.packArgs(argsTable), MapList.class);
		return renderedMapList;
	}

	public void setHyperlink(String hyperlink)
	{
		sHyperlink = hyperlink + "/common/emxNavigator.jsp?ContentPage=";
	}
}
