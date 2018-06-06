//IEFBuildFolderStructure.java

//This program obtains the list of commands that need to be displayed
//in an office integration environment. The list of commands is obtained
//from the "Office Integration" menu defined in business

//Copyright (c) 2002 MatrixOne, Inc.
//All Rights Reserved
//This program contains proprietary and trade secret information of
//MatrixOne, Inc.  Copyright notice is precautionary only and does
//not evidence any actual or intended publication of such program.

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.ListIterator;
import java.util.Vector;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import matrix.db.Context;
import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectAttributes;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;
import java.util.StringTokenizer;
import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFCache;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNodeImpl;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import matrix.db.MQLCommand;
import java.util.Vector;

/**
 * The <code>emxOfficeIntegrationMenus</code> class represents the JPO for
 * obtaining the MS Office integration menus
 *
 * @version AEF 10.5.0.0 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class IEFBuildFolderStructure_mxJPO
{
	private MCADGlobalConfigObject _GlobalConf						= null;
	private HashMap _GcoTable										= null;
	private String sHyperlink										= "";
	private IEFCache  _GlobalCache									= new IEFGlobalCache();
	private IEFDesktopConfigurations_mxJPO desktopConfigurations	=  null;
	Hashtable initArgsTable = null;
	/**
	 * Constructs a new IEFBuildFolderStructure JPO object.
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args an array of String arguments for this method
	 * @throws Exception if the operation fails
	 * @since AEF 10.0.0.0
	 */
	public IEFBuildFolderStructure_mxJPO (Context context, String[] args) throws Exception
	{
		// Call the super constructor
		super();
		if(args != null && args.length > 0)
		{
			initArgsTable = (Hashtable)JPO.unpackArgs(args);

			if(initArgsTable != null)
			{
				String integratioName = (String )initArgsTable.get("integrationName");
				_GcoTable			  = (HashMap)initArgsTable.get("gcoTable");
				_GlobalConf			  = (MCADGlobalConfigObject)_GcoTable.get(integratioName);
				sHyperlink			  = (String)initArgsTable.get("hyperlink");
				setHyperlink(sHyperlink);
			}
		}

		desktopConfigurations = new IEFDesktopConfigurations_mxJPO(context, args);
	}

	/**
	 * Given a menu generates a xml representation of all the elements under a menu
	 *
	 * @param args[0]  - language of the Client machine
	 * @param args[1]  - Symbolic Name of the menu
	 *
	 * @throws Exception if the operation fails
	 */
	public String getMenuComponents(Context context, String[] args) throws MatrixException
	{ 
		String xmlOutput = "";
		try 
		{
			//regional language used by the CSE
			String lang = args[0];
			String symbolicMenuName = args[1]; 
			boolean rootLevel = true;

			Hashtable initArgsTable = new Hashtable();

			String argConstructor[] = JPO.packArgs(initArgsTable);

			Hashtable argsTable     = new Hashtable(3);
			argsTable.put("language", lang);
			argsTable.put("symbolicMenuName", symbolicMenuName);
			argsTable.put("rootLevel", new Boolean(rootLevel).toString());
			argsTable.put("recursive", "false");

                        int iArgNo = 2;
                        if(args.length>iArgNo)
                            {
				rootLevel = false;
				boolean  isDateTimeAvailable = false;
				if(args.length ==5)
				{
				 rootLevel = true;
            	                 argsTable.put("datetimepattern", args[iArgNo++]);
				 isDateTimeAvailable = true;
				}
				else if(args.length ==6)
				{
				 argsTable.put("datetimepattern", args[++iArgNo]);
				  isDateTimeAvailable = true;
				}
				
				if(isDateTimeAvailable)
				{
            	                  argsTable.put("locale", args[iArgNo++]);
            	                  argsTable.put("timezoneoffset", args[iArgNo++]);
                                }
		             }
			MapList menuDetailsMapList = (MapList)JPO.invoke(context, "IEFDataGenerator", argConstructor, "getMenuComponentDetails", JPO.packArgs(argsTable), MapList.class);

			xmlOutput = buildXmlOutput(context, menuDetailsMapList);
		}
		catch (Exception ex)
		{
			String emxExceptionString = (ex.toString()).trim();

			// set the error string in the Error object
			if ( (emxExceptionString != null) && (emxExceptionString.trim().length() > 0) )
				throw new MatrixException(emxExceptionString);
		} 
		return xmlOutput;
	}

	private String buildXmlOutput(Context context, MapList result) throws Exception
	{
		String xmlOutput = (String)JPO.invoke(context, "IEFXmlGenerator", null, "buildXmlOutput", JPO.packArgs(result), String.class);
		return xmlOutput;
	}


	/**
	 * Checks out a Project Space for synchronization.
	 * Generates XML string representing the Project Space.
	 *
	 * @param busid String value of the BusID identifying the Project Space object
	 * @throws Exception if the operation fails
	 */
	public String evaluateCommand(Context context, String[] args) throws Exception
	{ 
		String xmlOutput = null;
		try 
		{
			initArgsTable.put("Hyperlink", sHyperlink);

			String[] initArgs = JPO.packArgs(initArgsTable);

			MapList renderedMapList = (MapList)JPO.invoke(context, "IEFDataGenerator", initArgs, "getCommandDetails", args, MapList.class);

			xmlOutput = buildXmlOutput(context, renderedMapList);
		}
		catch (Exception ex) 
		{
			MCADServerException.createException(ex.getMessage(), ex);
		} 
		return xmlOutput;
	}

	/**
	 * For a Menu/Command provided the program reads the HRef and obtains an inquiry or program
	 * and renders the output into a table
	 * Generates XML string representing the output.
	 *
	 * @param busid String value of the BusID identifying the Project Space object
	 * @throws Exception if the operation fails
	 */
	public String evaluateUIComponent(Context context, String[] args) throws MatrixException
	{ 
		String xmlOutput = "";

		try 
		{
			initArgsTable.put("Hyperlink", sHyperlink);

			String []initArgs = JPO.packArgs(initArgsTable);

			MapList renderedMapList = (MapList)JPO.invoke(context, "IEFDataGenerator", initArgs, "evaluateUIComponent", args, MapList.class);

			xmlOutput = buildXmlOutput(context, renderedMapList);
		}
		catch (Exception ex)
		{
			ContextUtil.abortTransaction(context);

			String emxExceptionString = (ex.toString()).trim();

			// set the error string in the Error object
			if ( (emxExceptionString != null) && (emxExceptionString.trim().length() > 0) )
				throw new MatrixException(emxExceptionString);
		} 

		return xmlOutput;
	}

	/**
	 * For a Menu/Command provided the program reads the HRef and obtains an inquiry or program
	 * and renders the output into a table
	 * Generates XML string representing the output.
	 *
	 * @param busid String value of the BusID identifying the Project Space object
	 * @throws Exception if the operation fails
	 */
	public String fetchRelationshipAsFolders(Context context, String[] args) throws MatrixException
	{ 
		String xmlOutput = "";

		try 
		{
			initArgsTable.put("Hyperlink", sHyperlink);

			String []initArgs = JPO.packArgs(initArgsTable);

			MapList renderedMapList = (MapList)JPO.invoke(context, "IEFDataGenerator", initArgs, "evaluateRelationshipAsFolders", args, MapList.class);

			xmlOutput = buildXmlOutput(context, renderedMapList);
		}
		catch (Exception ex)
		{
			ContextUtil.abortTransaction(context);

			String emxExceptionString = (ex.toString()).trim();

			if ( (emxExceptionString != null) && (emxExceptionString.trim().length() > 0) )
				throw new MatrixException(emxExceptionString);
		} 

		return xmlOutput;
	}

	/**
	 * Generic function that the CSE calls for any clicks in the CSE
	 * This function evaluates the data sent and channels the request to 
	 * appropriate function
	 *
	 * returns an xml response after traversing the object 
	 *
	 * @param args input details as provided by the CSE
	 * @throws Exception if the operation fails
	 */
	public String fetchContent(Context context, String[] args) throws Exception
	{ 
		String xmlOutPut = null;
		String commandName = null;
		String busId = null;
		String sRelationshipName = null;
		boolean generateDataFromCommand = false;

		try 
		{
			if ("command_MsoiSaveToENOVIA".equalsIgnoreCase(args[1]))
			{
				xmlOutPut = buildXmlOutput(context, new MapList());
				generateDataFromCommand = false;
				return xmlOutPut;
			} 

			if("command_MsoiWBSTasks".equalsIgnoreCase(args[1]))
			{
				String[] WBSArgs = new String[3];
				WBSArgs[0] = args[0]; //Language
				WBSArgs[1] = "menu_MsoiWBSTasks"; //Menu Name
				WBSArgs[2] = "false"; //It is not a root level folder
				xmlOutPut = getMenuComponents(context, WBSArgs);
				generateDataFromCommand = false;
			} 
			else if(args[1].indexOf("collections") != -1 )
			{
				String collectionName = args[3]; 
				commandName = "command_IEFDesktopCollectionItems";
				//special case for collections
				generateDataFromCommand = false;
				String[] newArgs = {args[0], commandName, "", collectionName};
				xmlOutPut = evaluateCommand(context, newArgs);
			} 
			else if(args[1].indexOf("savedQuery") != -1 )
			{
				String savedQueryName = args[2]; 
				commandName = "command_MsoiEvaluateSavedQueries";
				//special case for savedQuery
				generateDataFromCommand = false;
				String[] newArgs = {args[0], commandName, "", savedQueryName};
				xmlOutPut = evaluateCommand(context, newArgs);
			} 
			else if(args[1].startsWith("menu_"))
			{
				xmlOutPut = getMenuComponents(context, args);
				generateDataFromCommand = false;
			} 
			else if(args[1].startsWith("command_"))
			{
				xmlOutPut = evaluateCommand(context, args);
				generateDataFromCommand = false;
			}
			else 
			{
				if(args[1].startsWith("type_"))
				{   
					//this handles cases for fetching objects container objects like
					//My Books, My Projects etc the command will have details of the 
					//inquiry and table in the command itself
					String sTypeNavigationMappings	= desktopConfigurations.getTypeNavigationMappings();
					String[] docArgs				= {busId, args[1], sTypeNavigationMappings};
					Hashtable mappingsTable			= (Hashtable)JPO.invoke(context, "IEFCDMUtil", null, "getDocumentRelationshipFromGCO", docArgs, Hashtable.class);

					String relName					= null;
					String commandName1				= null;
					String menuName					= null;

					if(mappingsTable != null)
					{
						relName = (String) mappingsTable.get("relationship");
						commandName1 = (String) mappingsTable.get("command");
						menuName = (String) mappingsTable.get("menu");
					}

					if(menuName == null)
					{
						String strRev = "";
						if(args.length > 4)
							strRev =args[4]; 
						busId = getBusId(context,args[2],args[3],strRev);

						sRelationshipName = relName;
						commandName = commandName1;
						generateDataFromCommand = true;
					} 
					else
					{
						String[] nArgs = new String[5];
						nArgs[0] = args[0]; //Language
						nArgs[1] = menuName; //Menu Name
						nArgs[2] = args[2]; //Type
						nArgs[3] = args[3]; //Name

						if(args.length >= 5)
							nArgs[4] = args[4]; //Rev
						else
							nArgs[4] = ""; //Rev

						xmlOutPut = fetchRelationshipAsFolders(context, nArgs);
						generateDataFromCommand = false;
					}
				} 
				else if (args[1].startsWith("rel_"))
				{
					String fourthArg = "";
					if(args.length >= 5)
						fourthArg = args[4];

					busId = getBusId(context,args[2],args[3],fourthArg);

					sRelationshipName = "";
					int index = args[1].indexOf("rel_");
					if(index != -1)
						commandName = args[1].substring(index+4);
					else
						commandName = "command_MsoiDocumentSummary";
					generateDataFromCommand = true;
				} 
				else 
				{
					busId = getBusId(context,args[2],args[3],args[4]);

					String[] docArgs = {busId, args[1]};

					sRelationshipName = (String)JPO.invoke(context, "IEFCDMUtil", null, "getRelationshipToBrowse", docArgs, String.class);
					commandName = "command_MsoiDocumentSummary";
					generateDataFromCommand = true;
				}
			}
			//evaluate the command to obtain the list of connected objects
			//and render it with the table defined in the command
			if(generateDataFromCommand) 
			{
				String[] newArgs = {args[0], commandName, busId, sRelationshipName};
				xmlOutPut = evaluateUIComponent(context, newArgs);
				generateDataFromCommand = true;
			}
		} 
		catch (Exception ex) 
		{
			ex.printStackTrace();
			MCADServerException.createException(ex.getMessage(), ex);
		} 

		return xmlOutPut;
	}

	/**
	 * Generic function that the CSE calls for any clicks in the CSE
	 * This function evaluates the data sent and channels the request to 
	 * appropriate function
	 *
	 * returns an xml response after traversing the object 
	 *
	 * @param args input details as provided by the CSE
	 * @throws Exception if the operation fails
	 */
	public String getBasicProperties(Context context, String[] args) throws Exception
	{
		String busId = null;
		String minorBusId = null;
		String xmlOutput = null;
		String relActiveVersion		= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
		String CDM_SELECT_ACTIVE_CURRENT_ACCESS	= "from[" + relActiveVersion + "].to.current.access";
		try 
		{
			String lang = args[0];
			String type = args[1];
			String name = args[2];
			String rev  = args[3];

			String sUpdate = args[4];
			String sFileName = null;
            String datetimepattern  = null;
            String locale			= null;
    	    String timezoneoffset   = null;
    	  
            int iArgNo = 5;
			if(args.length > iArgNo)
			{
            	sFileName = args[iArgNo++]; 

            if(args.length > iArgNo)
            {
            		if(args.length == 8)    
            		{
            			datetimepattern = sFileName;
			}
            		else if(args.length == 9) 
            {
            	datetimepattern = args[iArgNo++];
            		}
            		
            	locale          = args[iArgNo++];
            	timezoneoffset  = args[iArgNo++];
            }
            }

			boolean bIsUpdate = false;
			if(sUpdate != null && sUpdate.equals("true"))
				bIsUpdate = true;

			HashMap argsMap = new HashMap(3);
			argsMap.put("type", type);
			argsMap.put("name", name);
			argsMap.put("rev", rev);
			busId = getBusId(context,type,name,rev);

MCADServerResourceBundle serverResourceBundle=new MCADServerResourceBundle(lang);
			//Check whether the object is versionable or not
			Hashtable argsTable = new Hashtable(1);
			argsTable.put("busId", busId);
			Boolean isVersionableObject = (Boolean)JPO.invoke(context, "IEFCDMUtil", new String[] {busId}, "iefIsVersionable", JPO.packArgs(argsTable), Boolean.class);

			boolean isVersionable = isVersionableObject.booleanValue();
			MCADMxUtil _util = new MCADMxUtil(context, new com.matrixone.MCADIntegration.server.MCADServerLogger(""), new com.matrixone.MCADIntegration.server.MCADServerResourceBundle(lang),_GlobalCache);

			Hashtable argumentsTable = new Hashtable(1);
			argumentsTable.put("type", type);
			String parentType = (String)JPO.invoke(context, "IEFCDMSupport", null , "iefGetParentType", JPO.packArgs(argumentsTable), String.class);

			com.matrixone.apps.domain.DomainObject domainObject = new com.matrixone.apps.domain.DomainObject();
			StringList busSelect = new StringList();
			busSelect.add("current.access");
			busSelect.add(DomainObject.SELECT_LOCKED);
			busSelect.add(DomainObject.SELECT_LOCKER);
			busSelect.add(CDM_SELECT_ACTIVE_CURRENT_ACCESS);
			busSelect.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
			busSelect.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKER);
			busSelect.add(CommonDocument.SELECT_IS_VERSION_OBJECT);

			if(busId != null)
			{
				domainObject.setId(busId);

				busSelect.add(DomainObject.SELECT_NAME);
				busSelect.add(DomainObject.SELECT_TYPE);
				busSelect.add(DomainObject.SELECT_REVISION);
				busSelect.add(DomainObject.SELECT_POLICY);
				busSelect.add(DomainObject.SELECT_VAULT);
				busSelect.add(DomainObject.SELECT_CURRENT);
				busSelect.add(DomainObject.SELECT_DESCRIPTION);
				busSelect.add(DomainObject.SELECT_OWNER);
				busSelect.add(DomainConstants.SELECT_FILE_NAME); //"format.file.name"
				busSelect.add(CommonDocument.SELECT_TITLE);
				busSelect.add(DomainConstants.SELECT_FILE_FORMAT);
				Map propertiesMap = domainObject.getInfo(context, busSelect);
//issue related to multiple name display of image file
			Object  filenames = propertiesMap.get(DomainConstants.SELECT_FILE_NAME);
			Object  fileformats = propertiesMap.get(DomainConstants.SELECT_FILE_FORMAT);
			StringList formatList=(StringList)fileformats;
			StringList fileList=(StringList)filenames;
			ListIterator formatListItr = formatList.listIterator();
			ListIterator fileListItr = fileList.listIterator();
			StringList slPositionsToIgnore = new StringList();	
			int iPos = 0;
			StringList filenamesList = new StringList();
			while(formatListItr.hasNext())
			{
				String format = (String)formatListItr.next();
				if(format !=null)
				{
					if(format.equals(CommonDocument.FORMAT_MX_MEDIUM_IMAGE))
					{
						slPositionsToIgnore.addElement(iPos+"");
					}

				}
				iPos++;
			}
			int iCnt = 0;
			while(fileListItr.hasNext())
			{
				String sEachFile = (String)fileListItr.next();
				if(!slPositionsToIgnore.contains(iCnt+""))
				{
					filenamesList.add(sEachFile);
				}
				iCnt++;
			}
			propertiesMap.remove(DomainConstants.SELECT_FILE_NAME);
			propertiesMap.remove(DomainConstants.SELECT_FILE_FORMAT);
			propertiesMap.put(DomainConstants.SELECT_FILE_NAME,filenamesList);
				boolean multiFile = false;
				String strCDMTypeDcouments = CommonDocument.TYPE_DOCUMENTS;

				String strCDMIsVersionObject = "false";

				Object vObject = propertiesMap.get(CommonDocument.SELECT_IS_VERSION_OBJECT);
				if(vObject != null) strCDMIsVersionObject = (String) vObject;

				//if the type is document then the selectables have "[ ]" characters for e.g from[Active Version]
				//which is not acceptable by JDOM, so reset the keys without a bracket
				//For Only the versionable objects do the following tasks
				if ( isVersionable && parentType.equals(strCDMTypeDcouments) && //[SUPPORT]
						"false".equalsIgnoreCase(strCDMIsVersionObject ))
				{
					StringList filename = (StringList) propertiesMap.get(DomainConstants.SELECT_FILE_NAME);
					if(filename != null && filename.size() > 1)
					{
						multiFile = true;
						//Since the object is multifile object get the lock status for the versionable object using the file name
						if(sFileName != null && sFileName.length() > 0)
						{
							BusinessObject minorObjectWithFile = _util.getCDMMinorObjectWithFileNameAsTitle(context, new BusinessObject(busId), sFileName);
							if(minorObjectWithFile != null)
							{
								minorBusId = minorObjectWithFile.getObjectId(context);
								domainObject.setId(minorBusId);
								propertiesMap = domainObject.getInfo(context, busSelect);
								vObject = propertiesMap.get(CommonDocument.SELECT_IS_VERSION_OBJECT);
								if(vObject != null) 
									strCDMIsVersionObject = (String) vObject;
							}
						}
					}
				}

				if ( isVersionable && parentType.equals(strCDMTypeDcouments) && //[SUPPORT]
						"false".equalsIgnoreCase(strCDMIsVersionObject ))
				{
					StringList filename = (StringList) propertiesMap.get(DomainConstants.SELECT_FILE_NAME);
					if(filename != null && filename.size() > 1)       
						multiFile = true;

					StringList lockedList = null;
					StringList lockerList = null;

					String access = null;
					String locked = null;
					String locker = null;
					String state=null;
					String policy=null;

					access = "all";
					vObject = propertiesMap.get(CDM_SELECT_ACTIVE_CURRENT_ACCESS);
					if(vObject != null) access = (String) vObject;

					lockedList =  new StringList();
					vObject = propertiesMap.get(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
					if(vObject != null) lockedList = (StringList) vObject;

					lockerList = new StringList();
					vObject = propertiesMap.get(CommonDocument.SELECT_ACTIVE_FILE_LOCKER);
					if(vObject != null) lockerList = (StringList) vObject;
             vObject = propertiesMap.get(CommonDocument.SELECT_CURRENT);
			 if(vObject != null) state = (String) vObject;
			 vObject = propertiesMap.get(CommonDocument.SELECT_POLICY);
			 if(vObject != null) policy = (String) vObject;
					propertiesMap.put("update", "false");
					//if it is multiFile then it is a major BO dont show any 
					//lock status/locker/locked by the individual files 
					//represent who has locked and what the lock status is
					if(!multiFile && (lockedList != null && lockedList.size() > 0) && (lockerList != null && lockerList.size() > 0))
					{
						locked = (String) lockedList.get(0);
						locker = (String) lockerList.get(0);
					}

					if(access == null || "null".equals(access))
					{
						propertiesMap.put("current.access", "");
					}
					else
					{
						propertiesMap.put("current.access", access);
					}

					if(state == null || "null".equals(state))
					{
						propertiesMap.put("current", "");
					}
					else
					{
					//state = MCADMxUtil.getNLSName(context, "State", state, "Policy",policy, lang);	
						propertiesMap.put("current", state);
					}

					if(multiFile || locked == null || "null".equals(locked))
					{
						propertiesMap.put("locked", "");
					}
					else
					{
						propertiesMap.put("locked", locked);
					}

					if(multiFile || locker == null || "null".equals(locker))
					{
						propertiesMap.put("locker", "");
					}
					else
					{
						propertiesMap.put("locker", locker);
					}

				} else if ( isVersionable && parentType.equals(strCDMTypeDcouments) && //[SUPPORT]
						"true".equalsIgnoreCase(strCDMIsVersionObject))
				{
					vObject = propertiesMap.get(CommonDocument.SELECT_TITLE);
					if(vObject != null) 
						propertiesMap.put(DomainConstants.SELECT_FILE_NAME,  (String)vObject);          
				}

				if(bIsUpdate)
					propertiesMap.put("update", "true");
				else
					propertiesMap.put("update", "false");

				//these need not be added to the final xml output so remove them
				propertiesMap.remove(CDM_SELECT_ACTIVE_CURRENT_ACCESS);
				propertiesMap.remove(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
				propertiesMap.remove(CommonDocument.SELECT_ACTIVE_FILE_LOCKER);
				propertiesMap.remove(CommonDocument.SELECT_IS_VERSION_OBJECT);
				propertiesMap.remove(CommonDocument.SELECT_TITLE);
				//language added for NSL
				propertiesMap.put("language",lang);
				
				String nameLabel = i18nNow.getBasicI18NString("Name",lang);
				String typeLabel = i18nNow.getBasicI18NString("Type",lang);
				String revisionLabel = i18nNow.getBasicI18NString("Revision",lang);
				String policyLabel = i18nNow.getBasicI18NString( "Policy",lang);
				String vaultLabel = i18nNow.getBasicI18NString("Vault",lang);
				String descriptionLabel = i18nNow.getBasicI18NString("Description",lang);
				String stateLabel = serverResourceBundle.getString("mcadIntegration.Server.ColumnName.State");
				String ownerLabel = i18nNow.getBasicI18NString( "Owner",lang);
				String accessLabel = serverResourceBundle.getString( "mcadIntegration.Server.ColumnName.AccessStatus");
				String lockedLabel = serverResourceBundle.getString( "mcadIntegration.Server.Message.Locked");
				String lockedByLabel = serverResourceBundle.getString( "mcadIntegration.Server.ColumnName.LockedBy");
				String FileNameLabel = serverResourceBundle.getString( "mcadIntegration.Server.ColumnName.FileName");

				Map attributeHeaderMap = new HashMap();
				//The right string resources are shown by the CSE dll
				attributeHeaderMap.put(DomainObject.SELECT_NAME, nameLabel);
				attributeHeaderMap.put(DomainObject.SELECT_TYPE, typeLabel);
				attributeHeaderMap.put(DomainObject.SELECT_REVISION, revisionLabel);
				attributeHeaderMap.put(DomainObject.SELECT_POLICY, policyLabel);
				attributeHeaderMap.put(DomainObject.SELECT_VAULT, vaultLabel);
				attributeHeaderMap.put(DomainObject.SELECT_DESCRIPTION, descriptionLabel);
				attributeHeaderMap.put(DomainObject.SELECT_CURRENT, stateLabel);
				attributeHeaderMap.put(DomainObject.SELECT_OWNER, ownerLabel);

				attributeHeaderMap.put("current.access", accessLabel);
				attributeHeaderMap.put(DomainObject.SELECT_LOCKED, lockedLabel);
				attributeHeaderMap.put(DomainObject.SELECT_LOCKER, lockedByLabel);
				attributeHeaderMap.put(DomainObject.SELECT_FILE_NAME, FileNameLabel);

				MapList propertiesDetailMapList = new MapList();

				//generate the order in which the properties need to be displayed
				String attributeOrder =  DomainObject.SELECT_NAME     + "," + DomainObject.SELECT_TYPE        + "," +
				DomainObject.SELECT_REVISION + "," + DomainObject.SELECT_POLICY      + "," +
				DomainObject.SELECT_VAULT    + "," + DomainObject.SELECT_DESCRIPTION + "," +
				DomainObject.SELECT_OWNER    + "," + DomainObject.SELECT_LOCKED      + "," + 
				DomainObject.SELECT_FILE_NAME+ "," +
				DomainObject.SELECT_LOCKER   + "," + DomainObject.SELECT_CURRENT	 + "," + "current.access";
				attributeHeaderMap.put("columnHeaderOrder", attributeOrder);

				//add the properties display name map
				propertiesDetailMapList.add(attributeHeaderMap);
				//add the properties value map to the maplist
				propertiesDetailMapList.add(propertiesMap);

				xmlOutput = buildXmlOutput(context, propertiesDetailMapList);

				BusinessObject majorObj = null;
				String  majorBusId = null;
				if(isVersionable && !multiFile)
				{
					try
					{
						Hashtable argumentsTable1 = new Hashtable(1);
						argumentsTable1.put("bus", new BusinessObject(busId));
						majorObj = (BusinessObject)JPO.invoke(context, "IEFUtil", null, "iefGetCDMMajorObject", JPO.packArgs(argumentsTable1), BusinessObject.class);

						if(majorObj != null)
						{
							majorObj.open(context);
							majorBusId = majorObj.getObjectId(context);
							majorObj.close(context);
						}
					}
					catch(Exception exx)
					{
						System.out.println("[IEFBuildFolderStructure.getBasicProperties] EXCEPTION :   " + exx.getMessage());
					}
				}

				if(majorBusId != null)
					busId = majorBusId;

                xmlOutput = insertAttributeListNode(context, lang, busId, xmlOutput,datetimepattern, locale, timezoneoffset);
			} 
		} 
		catch (Exception ex)
		{
			String emxExceptionString = (ex.toString()).trim();

			// set the error string in the Error object
			if ( (emxExceptionString != null) && (emxExceptionString.trim().length() > 0) )
				throw new MatrixException(emxExceptionString);
		} 
		return xmlOutput;
	}

	/**
	 * Generic function that the CSE uses for updating attributes of an Object
	 * returns an xml response after updating the object 
	 *
	 * @param args input details as provided by the CSE
	 * @throws Exception if the operation fails
	 */
	public String updateProperties(Context context, String[] args) throws Exception
	{
    	MCADServerGeneralUtil generalUtil = new MCADServerGeneralUtil();
		String xmlOutput = "";
		try
		{
			String lang = args[0];
			xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.AttributesUpdatedSuccessfully",  "iefStringResource", lang);
			String type = args[1];
			String name = args[2];
			String rev  = args[3];
			String sAttributeList = args[4];
            String datetimepattern  = null;
    	    String locale			= null;
    	    String timezoneoffset   = null;  
			
			int iArgNo = 5;
			//ignoreLock is added only for Autoclau
			String ignoreLock = "false";

			if(args.length > iArgNo)
			{
			
				ignoreLock = args[iArgNo++];
			
			}

            if(args.length > iArgNo)
            {
				if(args.length == 8)
				{
					datetimepattern = ignoreLock;
				}
				if(args.length == 9)
				{
               datetimepattern = args[iArgNo++];
				}
              
               locale          = args[iArgNo++];
    	       	timezoneoffset  = args[iArgNo++];
             }
				
			String busId = null;
			if(sAttributeList == null)
				sAttributeList = "";
			else
				sAttributeList = sAttributeList.trim();

			if(sAttributeList != null && sAttributeList.length() != 0)
			{
				busId = getBusId(context,type,name,rev);

				com.matrixone.apps.domain.DomainObject document = new com.matrixone.apps.domain.DomainObject();
				Hashtable argsTable = new Hashtable(1);
				argsTable.put("busId", busId);
				Boolean isVersionableObject = (Boolean)JPO.invoke(context, "IEFCDMUtil", new String[] {busId}, "iefIsVersionable", JPO.packArgs(argsTable), Boolean.class);

				boolean isVersionable = isVersionableObject.booleanValue();


				StringList selects = new StringList();
				selects.add(DomainConstants.SELECT_FILE_NAME);
				selects.add(DomainConstants.SELECT_LOCKED);
				selects.add(DomainConstants.SELECT_LOCKER);
				selects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
				selects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKER);
				selects.add(CommonDocument.SELECT_IS_VERSION_OBJECT);

				document.setId(busId);
				Map objectSelectMap = document.getInfo(context,selects);

				StringList lockedList = new StringList();
				Object vObject = objectSelectMap.get(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
				if(vObject != null) lockedList = (StringList) vObject;

				StringList lockerList = new StringList();
				vObject = objectSelectMap.get(CommonDocument.SELECT_ACTIVE_FILE_LOCKER);
				if(vObject != null) lockerList = (StringList) vObject;


				String locked = null;
				String locker = null;

				String sIsMinorObj = "false";
				vObject = objectSelectMap.get(CommonDocument.SELECT_IS_VERSION_OBJECT);
				if(vObject != null) sIsMinorObj = (String) vObject;

				boolean bIsMinorObj = false;
				if("true".equalsIgnoreCase(sIsMinorObj))
					bIsMinorObj = true;

				if(bIsMinorObj)
				{
					//It is a minor object. 
					document.open(context);
					if(document.isLocked(context))
					{
						locked = "TRUE";
						locker = document.getLocker(context).getName();
					}
					else
					{
						locked = "FALSE";
						locker = "";
					}
					document.close(context);
				}
				else
				{
					if(isVersionable)
					{
						if(lockedList != null && lockedList.size() > 0 )
							locked = (String) lockedList.get(0);
						else
							locked = "";

						if(lockerList != null && lockerList.size() > 0)
							locker = (String) lockerList.get(0);
						else
							locker = "";
					}else
					{
						locked = (String)objectSelectMap.get(DomainConstants.SELECT_LOCKED);
						locker = (String)objectSelectMap.get(DomainConstants.SELECT_LOCKER);
					}
				}

				StringList filename = (StringList) objectSelectMap.get(DomainConstants.SELECT_FILE_NAME);
				boolean bMultiFile = false;
				if(filename != null && filename.size() > 1)
					bMultiFile = true;

				if(ignoreLock.equalsIgnoreCase("false") && ("FALSE".equals(locked) || "".equals(locked)))
				{
					throw new Exception(i18nNow.getI18nString("mcadIntegration.Server.Message.NoLockError",  "iefStringResource", lang));
				}

				if("TRUE".equals(locked) || ignoreLock.equalsIgnoreCase("true"))
				{
					if(locker.equals(context.getUser()) || ignoreLock.equalsIgnoreCase("true"))
					{
						//Object is locked by context user. So go ahead and update the BO.
						IEFXmlNode attributeList = null;
						HashMap attributesMap = new HashMap();
						if(sAttributeList.length() > 0)
						{
							Hashtable argumentsTable  = new Hashtable(1);
							argumentsTable.put("commandString", sAttributeList);

							attributeList = (IEFXmlNode)JPO.invoke(context, "IEFUtil", null, "getCommandPacket", JPO.packArgs(argumentsTable), IEFXmlNode.class);

							if(attributeList != null)
								attributesMap = createAttributesMap(attributeList);
							if(attributesMap != null)
							{
								MCADMxUtil _util = new MCADMxUtil(context, null,_GlobalCache);
								String attrTitle = MCADMxUtil.getActualNameForAEFData(context,"attribute_Title");
								String attrOwner = MCADMxUtil.getActualNameForAEFData(context,"attribute_Owner");

								BusinessObject tempObj = new BusinessObject(busId);
								BusinessObject minorDocument = null;
								BusinessObject majorDocument = null;
								try
								{
									if(bIsMinorObj)
									{
										//Get the major object
										tempObj.open(context);
										Hashtable argumentsTable1 = new Hashtable(1);
										argumentsTable1.put("bus", tempObj);
										majorDocument = (BusinessObject)JPO.invoke(context, "IEFUtil", null, "iefGetCDMMajorObject", JPO.packArgs(argumentsTable1), BusinessObject.class);

										if(!bMultiFile)
											minorDocument = document;
										tempObj.close(context);
									}

									if(!bIsMinorObj)
									{
										tempObj.open(context);
										if(!bMultiFile)
										{
											//Get the minor object
											Hashtable argumentsTable1 = new Hashtable(1);
											argumentsTable1.put("bus", tempObj);
											minorDocument = (BusinessObject)JPO.invoke(context, "IEFUtil", null, "iefGetCDMMinorObject", JPO.packArgs(argumentsTable1), BusinessObject.class);
										}
										majorDocument = document;
										tempObj.close(context);
									}

								}catch(Exception ex)
								{
									ex.printStackTrace();
									System.out.println("[IEFBuildFolderStructure.updateProperties] EXCEPTION 11 : " + ex.getMessage());
								}

								Iterator itr = (attributesMap.keySet()).iterator();
								while (itr.hasNext())
								{
									String MxAttribName = (String)itr.next();
									Object ObjMxAttribValue = (Object)attributesMap.get(MxAttribName);
									if(ObjMxAttribValue instanceof StringList )
									{
												StringList MxAttribValue=(StringList)ObjMxAttribValue;
													if(MxAttribName.equals("description"))
									{
														if(MxAttribValue!=null && MxAttribValue.size()>0)
									{
															document.setDescription(context,(String)MxAttribValue.getElement(0));
										document.update(context);                            
																if(minorDocument != null)
																{
																	minorDocument.setDescription(context, (String)MxAttribValue.getElement(0));
																	minorDocument.update(context);
									}

														}
														else
									{
															document.setDescription(context,"");
										document.update(context);
										if(minorDocument != null)
										{
																	minorDocument.setDescription(context,"");
											minorDocument.update(context);
										}
													
														}
										continue;
									}
													
					Iterator itrnew = MxAttribValue.iterator();
					StringList attribValueTempList= new StringList();
					while(itrnew.hasNext())
					{
							String tempValueAttr = (String)itrnew.next();
							String attribValueTemp = generalUtil.getAttributeValueForDateTimeForJPO(context,MxAttribName,  tempValueAttr,  null,  true, datetimepattern, locale, timezoneoffset);
							attribValueTempList.addElement(attribValueTemp);
					}
					MxAttribValue=attribValueTempList;
									
													Attribute att 			= new Attribute(new AttributeType(MxAttribName), MxAttribValue);
													
													AttributeList attList 	= new AttributeList();
													attList.addElement(att);
																					
													if(minorDocument != null && !(MxAttribName.equalsIgnoreCase(attrTitle)))
													{
														minorDocument.setAttributes(context,attList);
													}
													if(majorDocument != null)
													{
														majorDocument.setAttributes(context,attList);
													}
									}	//TODO
									
									else if (ObjMxAttribValue instanceof String)
									{	
												String MxAttribValue=(String)ObjMxAttribValue;

											if(attrTitle !=null && attrTitle.equalsIgnoreCase(MxAttribName) && bIsMinorObj)
											{
												//Do not allow CSE to change the title of the CDM Minor Objects.
												//Otherwise data may get corrupt.
												continue;
											}
                                  
											if(attrOwner !=null && attrOwner.equalsIgnoreCase(MxAttribName))
											{
												document.setOwner(MxAttribValue);
												document.update(context);                            
												continue;
											}
                                     
                                    MxAttribValue = generalUtil.getAttributeValueForDateTimeForJPO(context,MxAttribName,  MxAttribValue,  null,  true, datetimepattern, locale, timezoneoffset);
                                    Attribute att 			= new Attribute(new AttributeType(MxAttribName), MxAttribValue);
                                    
									AttributeList attList 	= new AttributeList();
									attList.addElement(att);
									if(minorDocument != null && !(MxAttribName.equalsIgnoreCase(attrTitle)))
									{	//_util.setAttributeValue(context,minorDocument, MxAttribName, MxAttribValue);
									minorDocument.setAttributes(context,attList);
									}
									if(majorDocument != null){
									//_util.setAttributeValue(context,majorDocument, MxAttribName, MxAttribValue);
									majorDocument.setAttributes(context,attList);
									}

								}
								
								}
								xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.AttributesUpdatedSuccessfully",  "iefStringResource", lang);
							}
						}
					}
					else
					{
						//The object is locked by some other user
						//throw exception
						throw new Exception(i18nNow.getI18nString("mcadIntegration.Server.Message.LockError",  "iefStringResource", lang) + " " + locker);                      
					}
				}               
			}
		}
		catch(Exception e)
		{
			String emxExceptionString = (e.toString()).trim();
			if ( (emxExceptionString != null) && (emxExceptionString.trim().length() > 0) )
			{String exMessage=e.getMessage();
			emxExceptionString=emxExceptionString+":"+exMessage;
				throw new Exception(emxExceptionString);
		}

		}

		return xmlOutput;
	}

    private String insertAttributeListNode(Context context, String languageStr, String busId, String xmlOutput,String datetimepattern, String locale, String timezoneoffset)
	{
		String retXmlString = xmlOutput;
		try
		{
			Hashtable argumentsTable  = new Hashtable(1);
			argumentsTable.put("commandString", xmlOutput);

			IEFXmlNode xmlNode = (IEFXmlNode)JPO.invoke(context, "IEFUtil", null, "getCommandPacket", JPO.packArgs(argumentsTable), IEFXmlNode.class);

			boolean bDone = false;
			if(xmlNode != null)
			{
				Enumeration nodeEnum = ((IEFXmlNodeImpl)xmlNode).elements();
				while(nodeEnum.hasMoreElements())
				{
					IEFXmlNodeImpl aNode = (IEFXmlNodeImpl)nodeEnum.nextElement(); 
					if(aNode != null)
					{                       
						Enumeration enum1 = aNode.elements();
						while(enum1.hasMoreElements())
						{
							IEFXmlNodeImpl cNode = (IEFXmlNodeImpl)enum1.nextElement(); 
							if(cNode != null)
							{
								if("object".equalsIgnoreCase(cNode.getName()))
								{
                                    IEFXmlNodeImpl attributeListNode = getAttributeList(context, languageStr, busId,datetimepattern, locale, timezoneoffset);
									cNode.addNode(attributeListNode);                                   
									bDone = true;
									break;
								}
							}
						}
						if(bDone)
							break;
					}
				}
				retXmlString = xmlNode.getXmlString();
			}           
		}
		catch(Exception e)
		{
			retXmlString = xmlOutput;
		}       
		return retXmlString;

	}

    private IEFXmlNodeImpl getAttributeList(Context context, String languageStr, String busId,String datetimepattern, String locale, String timezoneoffset) throws Exception
	{
		IEFXmlNodeImpl attributeListNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		attributeListNode.setName("attributelist");

		HashSet notAllowedAttributes = (HashSet)JPO.invoke(context, "IEFUtil", null, "getAttributeSetToFilter", null, HashSet.class);

		try
		{
			BusinessObject obj = new BusinessObject(busId);
			obj.open(context);
			BusinessObjectAttributes objAttribs = obj.getAttributes(context, false);
			obj.close(context);
			AttributeList attribList = objAttribs.getAttributes();
			AttributeItr  attributeListItr = new AttributeItr(attribList);
            MCADServerGeneralUtil generalUtil = new MCADServerGeneralUtil();
			while (attributeListItr.next())
			{               
				String selectPropValue=null;
				String selectProp=null;
				Attribute attributeObject = attributeListItr.obj();
				String attributeName = attributeObject.getName();

				if(!notAllowedAttributes.contains(attributeName))
				{
					Boolean  isMultiVal=attributeObject.isMultiVal();
					Boolean  isSingleVal=attributeObject.isSingleVal();
					Boolean  isRangeVal=attributeObject.isRangeVal();		
					Boolean isMultiLine=attributeObject.isMultiLine();
					StringList attribValue = attributeObject.getValueList();
					Iterator itr = attribValue.iterator();
					StringList attribValueTempList= new StringList();
					while(itr.hasNext())
					{
							
							String attribValueTemp = generalUtil.getAttributeValueForDateTimeForJPO(context,attributeName,  (String)itr.next(),  null,  false, datetimepattern, locale, timezoneoffset);
							attribValueTempList.addElement(attribValueTemp);
					}
					attribValue=attribValueTempList;
				
					/////if value of Attribute Status is Unknown
					
					if(attribValue!=null && attribValue.size()>0)
					{
							String tempAttribValue=(String)attribValue.elementAt(0);
							tempAttribValue=tempAttribValue.trim();
							if(attributeName.equalsIgnoreCase("Status") && attribValue.size()==1 && tempAttribValue.equalsIgnoreCase("Unknown")){
								selectProp = "emxFramework.Default." +attributeName;
								selectPropValue = UINavigatorUtil.getI18nString(selectProp, "emxFrameworkStringResource", languageStr);
								
							
						}
							else if(attributeName.equalsIgnoreCase("Designated User")&& attribValue.size()==1 && tempAttribValue.equalsIgnoreCase("Unassigned")){
										
								String selectValue=attributeName;
								selectProp = "emxFramework.Default." +selectValue.replace(' ', '_') ;
								selectPropValue = UINavigatorUtil.getI18nString(selectProp, "emxFrameworkStringResource", languageStr);
							}
							else if(attributeName.equalsIgnoreCase("Task Constraint Type")&& attribValue.size()==1){
								selectProp = "emxFramework.Range.Task_Constraint_Type." +tempAttribValue.replace(' ', '_') ;
								selectPropValue = UINavigatorUtil.getI18nString(selectProp, "emxFrameworkStringResource", languageStr);
							
							}
							else if(attributeName.equalsIgnoreCase("Task Requirement")&& attribValue.size()==1){
								selectProp = "emxFramework.Range.Task_Requirement." +tempAttribValue.replace(' ', '_') ;
								selectPropValue = UINavigatorUtil.getI18nString(selectProp, "emxFrameworkStringResource", languageStr);
							
						}
							else if(attributeName.equalsIgnoreCase("Project Role")&& attribValue.size()==1){
								selectProp = "emxFramework.Range.Project_Role." +tempAttribValue.replace(' ', '_') ;
								selectPropValue = UINavigatorUtil.getI18nString(selectProp, "emxFrameworkStringResource", languageStr);
								
							}
							else if(attributeName.equalsIgnoreCase("Access Type")&& attribValue.size()==1){
								selectProp = "emxFramework.Range.Access_Type." +tempAttribValue.replace(' ', '_') ;
								selectPropValue = UINavigatorUtil.getI18nString(selectProp, "emxFrameworkStringResource", languageStr);
						
							}
						
						if(selectPropValue!=null && !selectPropValue.equals(""))
						{
							if(selectPropValue.equals(selectProp)){
							}
							else
									attribValue.insertElementAt(selectPropValue,0);
								}
						}
						
					AttributeType attrTypeObj = new AttributeType(attributeName);
				   String sOperator1 = attrTypeObj.getDataType(context); //IR-548306-3DEXPERIENCER2015x : L86

					//IR-542275-3DEXPERIENCER2015x : Custom attributes of type string, date etc are not displayed in the document attributes,if custom binary attribute is added to type Document
					MCADMxUtil _util = new MCADMxUtil(context, null,_GlobalCache);
					String sOperator = _util.getTypeForAttribute(context, attributeName);									

					HashMap argsMap = new HashMap(2);
					argsMap.put("Operator", sOperator);
					argsMap.put("language", languageStr);

					HashMap operatorMap = (HashMap)JPO.invoke(context, "IEFUtil", null, "getOperatorvalues", JPO.packArgs(argsMap), HashMap.class);
					sOperator = (String)operatorMap.get("operator");

					Hashtable argumentsTable  = new Hashtable(2);
					argumentsTable.put("AttributeType", attrTypeObj);
					argumentsTable.put("language", languageStr);

					String sRangevalues = (String)JPO.invoke(context, "IEFUtil", null, "getSelectedValues", JPO.packArgs(argumentsTable), String.class);

					if(sRangevalues == null) sRangevalues = "";
					if ("|".equals((sRangevalues.trim()))) sRangevalues = "";

					Hashtable argumentsTable1  = new Hashtable(3);
					argumentsTable1.put("adminType", "Attribute");
					argumentsTable1.put("language", languageStr);
					argumentsTable1.put("adminName", attributeName );

					String displayAttributeName = (String)JPO.invoke(context, "IEFUtil", null, "getDisplayValue", JPO.packArgs(argumentsTable1), String.class);

					Hashtable attribTable = new Hashtable();
					attribTable.put("name", attributeName);
					attribTable.put("displayname", displayAttributeName);
					attribTable.put("operator", sOperator);
					attribTable.put("rangevalues", sRangevalues);
					attribTable.put("multivalue",Boolean.toString(isMultiVal));
					attribTable.put("multiline",Boolean.toString(isMultiLine));
					attribTable.put("singlevalue",Boolean.toString(isSingleVal));
					attribTable.put("rangevalue",Boolean.toString(isRangeVal));
					IEFXmlNodeImpl attributeNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
					attributeNode.setName("attribute");
					if(!isMultiVal && !isMultiLine)
					{
						
						if(attribValue!=null && attribValue.size()!=0){
							String enteredvalue=(String)attribValue.elementAt(0);
						    attribTable.put("enteredvalue",enteredvalue);
						}
						else
						{
						attribTable.put("enteredvalue","");
						}
					}
					else
					{	
						attribTable.put("enteredvalue","");
						
						Hashtable mutliValueTable = new Hashtable();
						
						int k=1;
						if(attribValue!=null && attribValue.size()!=0){
						Iterator itr1 = attribValue.iterator();
							while(itr1.hasNext())
								{
								IEFXmlNodeImpl attributeNodeMultiValue = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
								attributeNodeMultiValue.setName("value");
							
								String enteredvalue=(String)itr1.next();
								attributeNodeMultiValue.setContent(enteredvalue);
								attributeNode.addNode(attributeNodeMultiValue);
								
								}
							}	
					}
					attributeNode.setAttributes(attribTable);

					attributeListNode.addNode(attributeNode);
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("[IEFBuildFolderStructure.getAttributeList] EXCEPTION : " + e.getMessage());
		}
		return attributeListNode;
	}

	/**
	 * list all revisons of a document 
	 * Based on the parameter passed a major object or version object could be deleted    
	 *
	 * @param busid String value of the BusID identifying the Project Space object
	 * @throws Exception if the operation fails
	 */
	public String listDocumentRevisions(Context context, String[] args) throws MatrixException
	{ 
		String xmlOutput = "";
		try 
		{
			//regional language used by the CSE
			String lang = args[0];
			String type = args[1]; 
			String name = args[2]; 
			String rev  = args[3]; 

            String datetimepattern  = null;
            String locale			= null;
    	    String timezoneoffset   = null;
			
			int iArgNo = 4;
		

            if(args.length > iArgNo)
            {
               datetimepattern = args[iArgNo++];
               locale          = args[iArgNo++];
    	       timezoneoffset  = args[iArgNo++];
             }
			//get document Id
			String busId = getBusId(context,args[1],args[2],args[3]);

			//command which can fetch the document versions
			String symbolicCommandName = "command_APPDocumentRevisions";

            String[] newArgs = {args[0], symbolicCommandName, busId, null,datetimepattern,locale,timezoneoffset};
			xmlOutput = evaluateUIComponent(context, newArgs);
		}
		catch (Exception ex)
		{
			String emxExceptionString = (ex.toString()).trim();

			// set the error string in the Error object
			if ( (emxExceptionString != null) && (emxExceptionString.trim().length() > 0) )
				throw new MatrixException(emxExceptionString);
		} 
		return xmlOutput;
	}

	/**
	 * list all versions of a document 
	 * Based on the parameter passed a major object or version object could be deleted    
	 *
	 * @param busid String value of the BusID identifying the Project Space object
	 * @throws Exception if the operation fails
	 */
	public String listDocumentVersions(Context context, String[] args) throws MatrixException
	{ 
		String xmlOutput = "";
		try 
		{
			//regional language used by the CSE
			String lang = args[0];
			String type = args[1]; 
			String name = args[2]; 
			String rev  = args[3]; 
			String latestVersion  = null;
			String symbolicCommandName = null;
            String datetimepattern  = null;
            String locale			= null;
    	    String timezoneoffset   = null;
    	    
    	    int iArgNo = 4;

            if(args.length > iArgNo)
                latestVersion  = args[iArgNo++]; 
			else
				latestVersion = "false";

            if(!latestVersion.equals("true"))
            {
            	iArgNo = 4;
            }
            
           if(args.length > iArgNo)
           {
        	   	datetimepattern = args[iArgNo++];
        	   	locale          = args[iArgNo++];
   	       		timezoneoffset  = args[iArgNo++];
            }
            
			if(latestVersion.equals("true"))
			{
				//this command fetches the latest document versions of a given revision
				//used here for multifiles case
				symbolicCommandName = "command_APPDocumentFiles";
			}
			else
			{
				//this command fetches the all document versions of a given revision
				symbolicCommandName = "command_APPDocumentFileVersions";
			}

			//get document Id
			String busId = getBusId(context,args[1],args[2],args[3]);

			//special case for multifiles
			//when multifiles exists in a document then the user will navigate one more level to see the latest file versions
			//now on this file versions if the user clicks "List Versions" then the id sent is the version id so navigate the
			//active version relationship and get the major bo from the major BO all the versions can be shown
			com.matrixone.apps.domain.DomainObject versionObject = 
				new com.matrixone.apps.domain.DomainObject();
			versionObject.setId(busId);
			StringList busSelects = new StringList(3);
			busSelects.add(DomainConstants.SELECT_FILE_NAME);
			busSelects.add(CommonDocument.SELECT_IS_VERSION_OBJECT);
			busSelects.add(CommonDocument.SELECT_MASTER_ID);

			Map infoMap = versionObject.getInfo(context, busSelects);

			StringList fileList = (StringList) infoMap.get(DomainConstants.SELECT_FILE_NAME);
			boolean multiFile = false;
			if(fileList.size() > 1)
				multiFile = true;

			MCADMxUtil _util = new MCADMxUtil(context, null,_GlobalCache);
			if(!multiFile && !_util.isCDMInstalled(context))
				throw new Exception(i18nNow.getI18nString("mcadIntegration.Server.Message.DocumentHasNoVersions",  "iefStringResource", lang));

			Object vObject = infoMap.get(CommonDocument.SELECT_IS_VERSION_OBJECT);

			if(vObject != null && "true".equalsIgnoreCase((String) vObject))
			{
				symbolicCommandName = "command_APPFileVersions";
			}

            String[] newArgs = {args[0], symbolicCommandName, busId, null,datetimepattern,locale,timezoneoffset};
			xmlOutput = evaluateUIComponent(context, newArgs);

		} 
		catch (Exception ex)
		{
			ContextUtil.abortTransaction(context);

			String emxExceptionString = (ex.toString()).trim();

			// set the error string in the Error object
			if ( (emxExceptionString != null) && (emxExceptionString.trim().length() > 0) )
				throw new MatrixException(emxExceptionString);
		} 
		return xmlOutput;
	}

	/**
	 * Add newly created document object to a parent object 
	 * [eg. Book, Workspace Folder, Project Folder etc] 
	 *
	 * @param TNR of the document created 
	 * @param TNR of the parent object 
	 *
	 * @throws Exception if the operation fails
	 */
	public String addDocumentToContainer(Context context, String[] args) throws Exception
	{ 
		//regional language used by the CSE
		String lang = args[0];

		try 
		{
			//TNR of the parent object to which the document needs to be added 
			String parentType = args[1]; 
			String parentName = args[2]; 
			String parentRev  = args[3]; 
			//TNR of the document object created
			String type = args[4]; 
			String name = args[5]; 
			String rev  = args[6]; 

			//symbolic name of the relationship to be used for connecting a parent
			//object to a document object
			String sRelationshipName = null;

			//get document Id
			String parentId = getBusId(context,args[1],args[2],args[3]);
			String docId = getBusId(context,args[4],args[5],args[6]);

			com.matrixone.apps.domain.DomainObject docObject = 
				new com.matrixone.apps.domain.DomainObject();
			docObject.setId(docId);

			String[] newArgs = {parentId, args[1]};
			sRelationshipName = (String)JPO.invoke(context, "IEFUtil", null, "getDocumentRelationship", newArgs, String.class);

			if(sRelationshipName != null)
			{
				String actRelName = PropertyUtil.getSchemaProperty(context, sRelationshipName);

				com.matrixone.apps.domain.DomainObject parentObject = 
					new com.matrixone.apps.domain.DomainObject();
				parentObject.setId(parentId);
				DomainRelationship.connect(context,
						parentObject,
						actRelName, 
						docObject);
			}
		} 
		catch (Exception ex)
		{
			ContextUtil.abortTransaction(context);

			/* String emxExceptionString = (ex.toString()).trim();

            // set the error string in the Error object
            if ( (emxExceptionString != null) && (emxExceptionString.trim().length() > 0) )
                throw new MatrixException(emxExceptionString);*/

			MCADServerException.createManagedException("IEF0033200104", i18nNow.getI18nString("mcadIntegration.Server.Message.IEF0033200104",  "iefStringResource", lang), ex);
		} 

		return "";
	}

	private HashMap createAttributesMap(IEFXmlNode xmlContent)
	{
		HashMap attrMap = new HashMap();
		IEFXmlNodeImpl xmlNode = (IEFXmlNodeImpl)xmlContent;
		try
		{
			if(xmlNode != null)
			{
				Enumeration nodeEnum = xmlNode.elements();
				while(nodeEnum.hasMoreElements())
				{
					IEFXmlNodeImpl attributeNode = (IEFXmlNodeImpl)nodeEnum.nextElement(); 
					if(attributeNode != null)
					{
						String nodeName = attributeNode.getName();
						if("attribute".equals(nodeName))
						{
							String name = attributeNode.getAttribute("name");
							String enteredValue = attributeNode.getAttribute("enteredvalue");
							String multiline=attributeNode.getAttribute("multiline");
							String multivalue=attributeNode.getAttribute("multivalue");
							String rangevalue=attributeNode.getAttribute("rangevalue");
							String singlevalue=attributeNode.getAttribute("singlevalue");

							if(multivalue.equalsIgnoreCase("true") || multiline.equalsIgnoreCase("true"))
							{
								int childCount=attributeNode.getChildCount();
								Enumeration childNodesList=attributeNode.getChildrenByName("value");
								StringList values = new StringList();
								while(childNodesList.hasMoreElements())
								{
									IEFXmlNodeImpl childNode=(IEFXmlNodeImpl)childNodesList.nextElement();
									childNode=childNode.getFirstChild();
									values.addElement(childNode.getContent());
							
								}

								attrMap.put(name,values);
							}

							else
							{
							if(name == null)
							{
								continue;
							}
							else
								name = name.trim();

							if(enteredValue == null)
									{
										attrMap.put(name,"");
									//enteredValue = "";
									}
							else
									{
										
									
									attrMap.put(name, attributeNode.getAttribute("enteredvalue"));
									}
							}



						
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("[IEFBuildFolderStructure.createAttributesMap] EXCEPTION : " + e.getMessage());
		}
		return attrMap;
	}

	private String getBusId(Context context, String type, String name, String revision) throws Exception
	{
		HashMap docArgsMap = new HashMap(3);
		docArgsMap.put("type", type);
		docArgsMap.put("name", name);
		docArgsMap.put("rev",revision);

		return (String)JPO.invoke(context, "IEFUtil", null, "getBusId", JPO.packArgs(docArgsMap), String.class);
	}
	public void setHyperlink(String hyperlink)
	{
		sHyperlink = hyperlink + "/common/emxNavigator.jsp?ContentPage=";
	}
	public String getMetaDataInformationForCADToMXAttributes(Context context,String[] args) throws Exception
	{
		MCADServerGeneralUtil generalUtil = new MCADServerGeneralUtil();
		String xmlOutput=null;
		StringList mappedAttribute=new StringList();
		Hashtable CADToMXAttrMap = _GlobalConf.getCADMxAttributesMap();
		IEFXmlNodeImpl deatilsNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		deatilsNode.setName("Details");
		IEFXmlNodeImpl attributeListNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		attributeListNode.setName("attributelist");
		Enumeration keys = CADToMXAttrMap.keys();
		String _description="description";
		String _owner="owner";
		String _checkinReason = generalUtil.getActualNameForAEFData(context,"attribute_CheckinReason");
		Boolean  isMultiVal		=false;
		Boolean  isSingleVal	=false;
		Boolean  isRangeVal		=false;
		Boolean  isMultiLine	=false;
		String ranges="";
		String type = "";
		String sDefault = "";
		while(keys.hasMoreElements()){
			sDefault = "";
				IEFXmlNodeImpl attributeNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
				attributeNode.setName("attribute");
				String key = (String)keys.nextElement();
				int i = key.indexOf("|");
			if(i <= 0) {
					continue;
				}
				Vector VMxAttrName = (Vector)CADToMXAttrMap.get(key);
				String sMxAttrName = (String)VMxAttrName.elementAt(0);
				
							if(sMxAttrName.startsWith("$$")){
									sMxAttrName= sMxAttrName.replace('$',' ');
									sMxAttrName=sMxAttrName.trim();
							}
							if(!mappedAttribute.contains(sMxAttrName)){
								
									if(sMxAttrName.equalsIgnoreCase(_description)){
											  isMultiVal=false;
											  isSingleVal=true;
											  isRangeVal=false;
											  isMultiLine=true;
									}
									else if(sMxAttrName.equalsIgnoreCase(_owner)){
											isMultiVal=false;
											isSingleVal=true;
											isRangeVal=false;
											isMultiLine=false;
									}
									else{
										if(doesAttributeExist(context,sMxAttrName)){	
												MQLCommand command = new MQLCommand();
												try {
							if (command.executeCommand(context, "print attribute $1 select $2 $3 $4 $5 $6 dump $7", sMxAttrName, "Valuetype", "multiline", "type", "default", "range" ,"|")){
														String attrList = command.getResult().trim();
														StringTokenizer st = new StringTokenizer(attrList, "|");
														String Valuetype = st.nextToken();
														if(Valuetype.equalsIgnoreCase("singleval")){
															isMultiVal=false;
															isSingleVal=true;
															isRangeVal=false;
														}
														else if(Valuetype.equalsIgnoreCase("multival")){
																isMultiVal=true;
																isSingleVal=false;
																isRangeVal=false;
															
														}
														else{
																isMultiVal=false;
																isSingleVal=false;
																isRangeVal=true;
															}	
														String multiline = st.nextToken();
														if(multiline.equalsIgnoreCase("true")){
															isMultiLine=true;
														}else{
														isMultiLine=false;
														}
								type = st.nextToken();
								if(st.hasMoreTokens())
								{
									sDefault = st.nextToken();
									//if type is date then convert the date as per client locale format
									if("timestamp".equals(type))
									{
										try {
											sDefault = generalUtil.getAttributeValueForDateTimeForJPO(context, sMxAttrName, sDefault, null, false, args[1], args[2], args[3]);
										}
										catch(Exception ex)
										{
											ex.printStackTrace();
										}
									}
								}
													String choicesList="";
													while (st.hasMoreTokens()) {
															
															choicesList+=st.nextToken();
														 }
													ranges=choicesList.replace("=","|");
													}
												
												} 
												catch (Exception e)
												{
												System.out.println("attribute "+sMxAttrName+"doesn't exist");
												continue;
												}
											}
											else{
											System.out.println(sMxAttrName+" is not an attribute.");
											continue;
											}
										}	
										Hashtable attribTable = new Hashtable();
										attribTable.put("name", sMxAttrName);
										attribTable.put("multivalue",Boolean.toString(isMultiVal));
										attribTable.put("multiline",Boolean.toString(isMultiLine));
										attribTable.put("singlevalue",Boolean.toString(isSingleVal));
										attribTable.put("rangevalue",Boolean.toString(isRangeVal));
										attribTable.put("rangevalues",ranges);
				attribTable.put("attributedatatype", type);
				attribTable.put("defaultvalue", sDefault);
										attributeNode.setAttributes(attribTable);
										attributeListNode.addNode(attributeNode);
										mappedAttribute.add(sMxAttrName);
										
							}
			
		}
		deatilsNode.addNode(attributeListNode);
		xmlOutput=deatilsNode.getXmlString();
		
		return(xmlOutput);
	}
	private boolean doesAttributeExist(Context context, String attributeName) {

		boolean bExists = false;
		String argsToMQL[] = new String[] {attributeName};
		String sAttributeExists = MCADMxUtil.executeMQL("list attribute $1", context, argsToMQL);

		if(sAttributeExists.startsWith("true"))
		{
			sAttributeExists = sAttributeExists.substring(5);
			if(attributeName.equals(sAttributeExists)){
				bExists = true;
			}
		}
		return bExists;
	}    
	
}

