/*
 **   IEFUITableUtility
 **   This is an extract of the emxTable.jsp from AEF
 **   The extract of the JSP is made into a JPO since the Office Integration does not go through the jsp layer
 **   Minor modifcations are done to support special use cases
 **
 **   Copyright (c) 1992-2003 MatrixOne, Inc.
 **   All Rights Reserved.
 **   This program contains proprietary and trade secret information of MatrixOne,
 **   Inc.  Copyright notice is precautionary only
 **   and does not evidence any actual or intended publication of such program
 **
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.AccessConstants;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.BusinessType;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.Relationship;
import matrix.db.RelationshipWithSelect;
import matrix.db.RelationshipWithSelectList;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.server.managers.IEFDesktopCommandManager;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UITable;

/**
 * The <code>IEFUITableUtility</code> class contains methods for UI Table Component.
 */

public class IEFUITableUtility_mxJPO
{
	//cache the type and parent type mapping
	private Map _parentTypeMapping                          = new HashMap();
	private long iLevel                                     = 0;
	private boolean showFileNameColumn                      = true;
	private String [] initargs                              = null;
	private Hashtable initHashtable                         = null;
	private HashMap gcoTable                                = null;

	//Action Menu name is used only for cad models 
	private String actionMenuName							= null;

	private MCADMxUtil util                                 = null;
	private IEFGlobalCache globalCache                      = new IEFGlobalCache();
	private MCADServerResourceBundle resourceBundle         = null;
	private static String SELECT                            = "<";
	private static String MACRO                             = "{";
	private static String VARIABLE                          = "[";
	private static String DELIMITER_START                   = "$";
	private static String DELIMITER_TYPE                    = SELECT + MACRO + VARIABLE;
	private static String TEXT                              = "text";
	private static String END_SELECT                        = ">";
	private static String END_MACRO                         = "}";
	private static String SCHEMA                            = "schema:";
	private static String END_VARIABLE                     = "]";


	/**
	 * Constructor.
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args holds no arguments
	 * @throws Exception if the operation fails
	 */

	public IEFUITableUtility_mxJPO (Context context, String[] args) throws Exception
	{
		initargs = args;
		init(context);
	}

	private void init(Context context) throws Exception
	{
		initHashtable           = (Hashtable) JPO.unpackArgs(initargs);
		gcoTable                = (HashMap) initHashtable.get("gcoTable");     
		String language         = Locale.getDefault().getLanguage();
		resourceBundle          = new MCADServerResourceBundle(language);
		util                    = new MCADMxUtil(context, resourceBundle, globalCache);

		if(initHashtable.containsKey("actionMenuName"))
			actionMenuName  = (String) initHashtable.get("actionMenuName");
	}

	public MapList renderTable(Context context, String[] args) throws Exception
	{
		Hashtable argumentMap     = (Hashtable)JPO.unpackArgs(args);
		String tableName        = (String) argumentMap.get("tableName");
		String language         = (String) argumentMap.get("language");
		MapList totalResultList = (MapList) argumentMap.get("inputMapList");
		String hyperlink        = (String) argumentMap.get("hyperlink");
		Object paramListObject = argumentMap.get("paramListMap");
		if (null != paramListObject)
		{
			Map paramListMap = (Map)paramListObject;
			initHashtable.putAll(paramListMap);
		}

		return renderTable(context, tableName, language, totalResultList, hyperlink);
	}


	public MapList sortMapList(Context context,String[] args) throws Exception
	{
		HashMap hm                           = (HashMap)JPO.unpackArgs(args);
		String comparatorJPO                 = (String)hm.get("comparatorName");
		MapList list                         = (MapList)hm.get("list");
		String compName                      = (String)JPO.invoke(context, comparatorJPO, args, "getClassName", null, String.class);
		Class cls                            = Class.forName(compName);
		emxCommonBaseComparator_mxJPO cmp = (emxCommonBaseComparator_mxJPO)cls.newInstance();

		cmp.setSortKeys(hm);
		Collections.sort(list, cmp);
		return list;
	}

	/**
	 * Method for rendering a table
	 * This is an extract of the emxTable.jsp from AEF
	 * The extract of the JSP is made into a JPO since the Office Integration does not go through the jsp layer
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param tableName the name of the table to be used for rendering
	 * @param totalResult the list of ids that needs to be rendered
	 *                     this list is obtained by running the JPO or inquiry
	 *                     if id is absent "nodeType" key must be present with value 
	 *                          "file"   - denoting file like node
	 *                          "folder" - denoting folder like node
	 * @return a MapList object
	 * @throws Exception if the operation fails
	 */

	public MapList renderTable(Context context, String tableName, String language, MapList totalResultList, String hyperlink) throws Exception
	{
		Vector userRoleList       = PersonUtil.getAssignments(context);

		MapList resultMapList     = new MapList();

		MapList relBusObjPageList = totalResultList;

		HashMap paramMap          = new HashMap();
                String typeProjectSpace = MCADMxUtil.getActualNameForAEFData(context, "type_ProjectSpace");

		paramMap.put("languageStr", language);
		paramMap.putAll(initHashtable);

		if(tableName != null && tableName.length() > 0)
			tableName = tableName.trim();

		HashMap tableMap        = UITable.getTable(context, tableName);
		MapList finalMapList    = new MapList();

		if (tableMap == null)
		{
			Hashtable messageDetailsTable = new Hashtable();
			messageDetailsTable.put("UITYPE", "Table");

			String messageString = resourceBundle.getString("mcadIntegration.Server.Message.IEF0062000181", messageDetailsTable);

			MCADServerException.createException(messageString, null);
		}
		else
		{
			MapList columns = UITable.getColumns(context, tableName, userRoleList);

			if (columns == null || columns.size() == 0 )
			{
			}
			else
			{
				int noOfColumns = columns.size();

				MapList expandedColumnList = new MapList();
				
				for (int i=0; i < noOfColumns; i++)
				{
					HashMap columnMap	        = (HashMap)columns.get(i);
					String columnType           = UITable.getSetting(columnMap, "Column Type");

					if (columnType != null && columnType.length() > 0 )
					{
						if (columnType.equalsIgnoreCase("Dynamic") )
						{
							String dynamicColumnProgram = UITable.getSetting(columnMap, "Dynamic Column Program");
							String dynamicColumnFunction = UITable.getSetting(columnMap, "Dynamic Column Function");
							HashMap programMap = new HashMap();
							programMap.put("objectList", relBusObjPageList);
							MapList dynamicColumnList = new MapList();
							String[] methodargs = JPO.packArgs(programMap);
							try
							{
								dynamicColumnList = (MapList)JPO.invoke(context, dynamicColumnProgram, null, dynamicColumnFunction, methodargs, MapList.class);
								int noDynamicColumns = dynamicColumnList.size();
								
								HashMap dynamicColumnMap = new HashMap();
								for (int j=0; j < noDynamicColumns; j++)
								{
									dynamicColumnMap = (HashMap)dynamicColumnList.get(j);
									expandedColumnList.add(dynamicColumnMap);
								}
							}
							catch(Exception e)
							{
							}
						}
						else
						{
							expandedColumnList.add(columnMap);
						}
					}
					else
					{
						expandedColumnList.add(columnMap);
					}
				}
				columns = 	expandedColumnList;	
				noOfColumns = columns.size();
				
				String columnName[]         = new String[noOfColumns];
				String selectTypes[]        = new String[noOfColumns];
				String selectStrings[]      = new String[noOfColumns];
				String columnHeaders[]      = new String[noOfColumns];
				String programNames[]       = new String[noOfColumns];
				String functionNames[]      = new String[noOfColumns];
				String connectedOIDSelect[] = new String[noOfColumns];

				//re initialize the variables.
				HashMap columnMap       = new HashMap();
				String programName      = "";
				String functionName     = "";
				boolean typeSelectExist = false;
				boolean nameSelectExist = false;
				boolean revSelectExist  = false;

				for (int i=0; i < noOfColumns; i++)
				{
					columnMap           = (HashMap)columns.get(i);
					// Get column details
					String colName      = UITable.getName(columnMap);
					String columnLabel  = UITable.getLabel(columnMap);

					String sConnectedOIDselect  = "";
					String columnSelect         = UITable.getBusinessObjectSelect(columnMap);

					String columnType           = UITable.getSetting(columnMap, "Column Type");

					if (columnType != null && columnType.length() > 0 )
					{
						if (columnType.equalsIgnoreCase("program") )
						{
							columnSelect = UITable.getSetting(columnMap, "program");
							if (columnSelect != null && columnSelect.trim().length() > 0)
							{
								programName  = UITable.getSetting(columnMap, "program");
								functionName = UITable.getSetting(columnMap, "function");
								columnSelect = programName+functionName;
							}
						}
						else if (columnType.equalsIgnoreCase("programHTMLOutput") )
						{
							columnSelect = UITable.getSetting(columnMap, "program");
							if (columnSelect != null && columnSelect.trim().length() > 0)
							{
								programName = UITable.getSetting(columnMap, "program");
								functionName = UITable.getSetting(columnMap, "function");
								columnSelect = programName+functionName;
							}
						}
						else if (columnType.equalsIgnoreCase("icon") )
						{
							columnSelect = "";
						}
					}
					else
					{
						if (columnSelect != null && columnSelect.trim().length() > 0)
						{
							columnType = "businessobject";
							columnSelect = substituteValues(context, columnSelect);
							sConnectedOIDselect = UITable.getSetting(columnMap, "Connected Object Id");
							if (sConnectedOIDselect != null && sConnectedOIDselect.length() > 0 )
								sConnectedOIDselect = substituteValues(context, sConnectedOIDselect);
						}
						else
						{
							columnSelect = UITable.getRelationshipSelect(columnMap);
							if (columnSelect != null && columnSelect.trim().length() > 0)
							{
								columnType = "relationship";
								columnSelect = substituteValues(context, columnSelect);
							}
						}
					}

					//check if TNR select is added otherwise remove it
					//Check if "type" select is added
					if (columnSelect != null && columnSelect.equalsIgnoreCase("type") )
						typeSelectExist = true;
					//Check if "name" select is added
					if (columnSelect != null && columnSelect.equalsIgnoreCase("name") )
						nameSelectExist = true;
					//Check if "revision" select is added
					if (columnSelect != null && columnSelect.equalsIgnoreCase("revision") )
						revSelectExist = true;

					selectTypes[i]          = columnType;
					selectStrings[i]        = columnSelect;
					connectedOIDSelect[i]   = sConnectedOIDselect;

					String registeredSuite  = UITable.getSetting(columnMap, MCADMxUtil.getActualNameForAEFData(context, "attribute_RegisteredSuite"));
					columnName[i]           = colName;

					String StringResFileId  = "";

					if (registeredSuite != null && registeredSuite.length() != 0)
					{
						StringResFileId = UINavigatorUtil.getStringResourceFileId(registeredSuite);
					}

					if (columnLabel != null && columnLabel.trim().length() > 0 )
						columnHeaders[i] = UINavigatorUtil.getI18nString(columnLabel.trim(), StringResFileId , language);

					if (columnHeaders[i] == null || columnHeaders[i].trim().length() == 0 || columnHeaders[i].equals("null"))
						columnHeaders[i] = "";

					programNames[i]  = programName;
					functionNames[i] = functionName;
				}//end of column iteration

				// if selectType is program - get column result by invoking the specified JPO
				Vector programResult[] = new Vector[noOfColumns];

				for (int i = 0; i < noOfColumns; i++)
				{
					HashMap programMap = new HashMap();
					programMap.put("objectList", relBusObjPageList);
					programMap.put("paramList", paramMap);
					if ( selectTypes[i].compareTo("program") == 0 || selectTypes[i].compareTo("programHTMLOutput") == 0 )
					{
						if (programNames[i] != null && functionNames[i] != null)
						{
							//Looks like a bug in AEF
							//why is AEF table having Sourcing central JPO
							//if sourcing is not installed then it throws an error
							//skip this for now
							if (programNames[i].indexOf("emxSupplier")  == -1 )
							{
								//CDM version file actions shows errors for viewable icon
								//this is not going to be shown for MSOI skip this
								if(functionNames[i].indexOf("getVersionFileActions") == -1 && (!functionNames[i].equalsIgnoreCase("getLockIcon")))
								{
									Vector columnResult = new Vector();
									String[] methodargs = JPO.packArgs(programMap);
									try
									{
										columnResult = (Vector)JPO.invoke(context, programNames[i], initargs, functionNames[i], methodargs, Vector.class);

									}
									catch(Exception e)
									{

									}

									programResult[i] = columnResult;
								}
							}
						}
					}
				}

				// Form a set of business objects in the database
				// and execute select command on the set
				StringList sl_bus = new StringList();
				StringList sl_rel = new StringList();
				int noOfSelects_bus = 0;
				int noOfSelects_rel = 0;
				for (int i = 0; i < noOfColumns; i++)
				{
					if (selectTypes[i].compareTo("businessobject") == 0)
					{
						sl_bus.addElement(selectStrings[i]);
						noOfSelects_bus++;

						if (connectedOIDSelect[i] != null && connectedOIDSelect[i].length() > 0)
						{
							sl_bus.addElement(connectedOIDSelect[i]);
							noOfSelects_bus++;
						}
					}
					else if (selectTypes[i].compareTo("relationship") == 0)
					{
						sl_rel.addElement(selectStrings[i]);
						noOfSelects_rel++;
					}
				}

				if ( !(typeSelectExist) )
					sl_bus.addElement("type");
				if ( !(nameSelectExist) )
					sl_bus.addElement("name");
				if ( !(revSelectExist) )
					sl_bus.addElement("revision");

				sl_bus.addElement("policy");
				sl_bus.addElement("id");
				sl_bus.addElement("current.access");
				sl_bus.addElement(CommonDocument.SELECT_IS_VERSION_OBJECT);
				sl_bus.addElement(DomainConstants.SELECT_FILE_NAME);
				sl_bus.addElement(DomainConstants.SELECT_FILE_FORMAT);
				sl_bus.addElement(CommonDocument.SELECT_TITLE);

				String REL_ACTIVE_VERSION	  = (String) PropertyUtil.getSchemaProperty(context, "relationship_ActiveVersion");

				String SELECT_ON_ACTIVE_MINOR = "from[" + REL_ACTIVE_VERSION + "].to.";

				String REL_VERSION_OF		  = (String) PropertyUtil.getSchemaProperty(context, "relationship_VersionOf");

				String SELECT_ON_MAJOR		  = "from[" + REL_VERSION_OF + "].to.";

				String CDM_SELECT_ACTIVE_FORMAT_HAS_FILE = "to[" + REL_ACTIVE_VERSION + "].from.format.hasfile";

				String REL_LATEST_VERSION = (String) PropertyUtil.getSchemaProperty(context, "relationship_LatestVersion");
				
				String SELECT_ON_ACTIVE = "to[" + REL_ACTIVE_VERSION + "].from" ;
				String ACTIVE_VERSION_ID =	"from[" +REL_ACTIVE_VERSION+"].to.id";
				String SELECT_LATEST_VERSION_ID = ".from[" + REL_LATEST_VERSION + "].to.revision";
				
				String SELECT_LATEST_REVISION = "last";
				String TYPE_DOCUMENT 	= MCADMxUtil.getActualNameForAEFData(context, "type_Document");
				String IS_DOCUMENTType= "type.kindof[" + TYPE_DOCUMENT + "]";

				//added addtional select clauses - to select from major and select from active minor
				sl_bus.addElement(SELECT_ON_ACTIVE_MINOR + DomainConstants.SELECT_FILE_NAME);
				sl_bus.addElement(SELECT_ON_MAJOR + DomainConstants.SELECT_FILE_NAME);
				sl_bus.addElement(SELECT_ON_ACTIVE_MINOR + DomainConstants.SELECT_FILE_FORMAT);
				sl_bus.addElement(SELECT_ON_MAJOR + DomainConstants.SELECT_FILE_FORMAT);

				sl_bus.addElement(DomainConstants.SELECT_FORMAT_HASFILE);
				sl_bus.addElement(CDM_SELECT_ACTIVE_FORMAT_HAS_FILE);
				sl_bus.addElement(DomainConstants.SELECT_LOCKER);
				sl_bus.addElement(DomainConstants.SELECT_LOCKED);

				sl_bus.addElement(SELECT_ON_ACTIVE_MINOR + DomainConstants.SELECT_LOCKER);
				sl_bus.addElement(SELECT_ON_ACTIVE_MINOR + DomainConstants.SELECT_LOCKED);

				sl_bus.addElement(SELECT_ON_ACTIVE+SELECT_LATEST_VERSION_ID);
				sl_bus.addElement(SELECT_LATEST_REVISION);
				sl_bus.addElement(ACTIVE_VERSION_ID);
				sl_bus.addElement(IS_DOCUMENTType);
				RelationshipWithSelectList rwsl   = null;
				BusinessObjectWithSelectList bwsl = null;

                                         String bol_array[]=new String[0];
					if (noOfSelects_bus != 0 || noOfSelects_rel != 0) 
				{
					 bol_array = new String[relBusObjPageList.size()];

					for (int i = 0; i < relBusObjPageList.size(); i++)
					{
						Object relBusObjPage= relBusObjPageList.get(i);
						if(relBusObjPage instanceof HashMap){
							bol_array[i] = (String)((HashMap)relBusObjPage).get("id");
						}else{
							bol_array[i] = (String)((Hashtable)relBusObjPage).get("id");
						}
							
					}

					bwsl = BusinessObject.getSelectBusinessObjectData(context, bol_array, sl_bus);
				}

				if(bwsl != null)
				{
					resultMapList = FrameworkUtil.toMapList(bwsl);
				}
				if (!(sl_rel.contains("id[connection]")))
				{
					sl_rel.addElement("id[connection]");
					noOfSelects_rel++;
				}

				boolean addConnection = true;

				if (noOfSelects_rel > 1)
				{
					String rel_array[] = new String[relBusObjPageList.size()];

					for (int i = 0; i < relBusObjPageList.size(); i++)
					{
						try
						{
							if (((HashMap)relBusObjPageList.get(i)).get("id[connection]") == null || "".equals(((HashMap)relBusObjPageList.get(i)).get("id[connection]")))
							{
								addConnection = false;
								break;
							}

							rel_array[i] = (String)((HashMap)relBusObjPageList.get(i)).get("id[connection]");
						}
						catch (Exception ex)
						{
							if (((Hashtable)relBusObjPageList.get(i)).get("id[connection]") == null)
							{
								addConnection = false;
								break;
							}

							rel_array[i] = (String)((Hashtable)relBusObjPageList.get(i)).get("id[connection]");
						}
					}

					if(addConnection)
						rwsl = Relationship.getSelectRelationshipData(context, rel_array, sl_rel);
				}

				// Merge the relationship results into the main resultMapList
				for (int i = 0; i < noOfColumns; i++)
				{
					if ( selectTypes[i].compareTo("relationship") == 0 )
					{
						for (int k = 0; k < relBusObjPageList.size(); k++)
						{
							RelationshipWithSelect rws = (RelationshipWithSelect)rwsl.elementAt(k);
							String relColumnValue = rws.getSelectData(selectStrings[i]);
							HashMap resultMapItem = (HashMap)resultMapList.get(k);
							resultMapItem.put(selectStrings[i], relColumnValue);
						}
					}

					// Add the relationship "id[connection]" to the main resultMapList
					if (addConnection && sl_rel != null && sl_rel.size() > 0 && rwsl != null)
					{
						for (int k = 0; k < relBusObjPageList.size(); k++)
						{
							RelationshipWithSelect rws  = (RelationshipWithSelect)rwsl.elementAt(k);
							String relColumnValue       = (String)rws.getSelectData("id[connection]");
							HashMap resultMapItem       = (HashMap)resultMapList.get(k);
							resultMapItem.put("id[connection]", relColumnValue);
						}
					}
				}

				MapList tempResultMapList   = new MapList();
				HashSet nonExistingKeysList = new HashSet();

				// Merge the program and programHTMLOutput results into the main resultMapList
				if(!("AEFCollection".equals(tableName) ||"IEFCollection".equals(tableName)))
				{
					for (int i = 0; i < noOfColumns; i++)
					{
						if ( selectTypes[i].compareTo("program") == 0 || selectTypes[i].compareTo("programHTMLOutput") == 0 )
						{
							for (int k = 0; k < relBusObjPageList.size() && resultMapList != null; k++)
							{
								if(resultMapList.size() > 0)
								{
									HashMap existingResultMapItem = (HashMap)resultMapList.get(k);
									if (programResult[i] == null || "".equals(programResult[i]) || programResult[i].size() == 0)
									{
										existingResultMapItem.put(selectStrings[i], "");
									}
									else
									{
										existingResultMapItem.put(selectStrings[i], programResult[i].get(k));
									}
								}
								else
								{
									if(tempResultMapList.size() >= (k+1))
									{
										HashMap resultMapItem = (HashMap)tempResultMapList.get(k);

										if (programResult[i] == null || "".equals(programResult[i]) || programResult[i].size() == 0)
											resultMapItem.put(selectStrings[i], "");
										else
											resultMapItem.put(selectStrings[i], programResult[i].get(k));
									}
									else
									{
										HashMap resultMapItem = new HashMap(noOfColumns);
										tempResultMapList.add(resultMapItem);

										if (programResult[i] == null || "".equals(programResult[i]) || programResult[i].size() == 0)
											resultMapItem.put(selectStrings[i], "");
										else
											resultMapItem.put(selectStrings[i], programResult[i].get(k));
									}
								}
							}
						}
					}
				}
				else if("AEFCollection".equals(tableName) || "IEFCollection".equals(tableName))
				{
					for (int k = 0; k < relBusObjPageList.size() && resultMapList != null; k++)
					{
						HashMap resultMapItem = new HashMap();

						for (int i = 0; i < noOfColumns; i++)
						{
							if (( selectTypes[i].compareTo("program") == 0 || selectTypes[i].compareTo("programHTMLOutput") == 0 )&& programResult[i].size() > k )
								resultMapItem.put(selectStrings[i], programResult[i].get(k));
						}
						tempResultMapList.add(resultMapItem);
					}
				}

				if(tempResultMapList.size() > 0)
				{
					resultMapList.addAll(tempResultMapList);

					for (int k = 0; k < relBusObjPageList.size() && resultMapList != null; k++)
					{
							HashMap inputResultMap = null;
							Object obj = relBusObjPageList.get(k);
							if( obj instanceof Hashtable)
							{							
								inputResultMap.putAll((Hashtable)obj);
							}
							else
							{
								inputResultMap = (HashMap)relBusObjPageList.get(k);
							}						
						HashMap resultMapItem  = (HashMap)tempResultMapList.get(k);
						Iterator itr           = inputResultMap.keySet().iterator();

						while(itr.hasNext())
						{
							Object key = itr.next();

							if((key instanceof String) && !resultMapItem.containsKey(key))
							{
								Object value = inputResultMap.get(key);

								if(value instanceof String)
								{
									resultMapItem.put(key, value);
									nonExistingKeysList.add(key);
								}
							}
						}
					}
				}

				Map columnHeaderMap                 = new HashMap();
				String columnHeaderOrder            = "";
				String columnHeaderOrderForFileName = "";
				boolean isFileNameColumnAdded       = false;
				String typeImage 					= MCADMxUtil.getActualNameForAEFData(context, "type_Image");

				for (int i = 0; i < noOfColumns; i++)
				{
					//FIXTHIS : for now ignore icons. client side cannot display it
					if (  selectTypes[i].compareTo(typeImage) == 0 ||selectTypes[i].compareTo("icon") == 0  || selectTypes[i].compareTo("programHTMLOutput") == 0 || columnName[i].compareTo("PopupName") == 0 || columnName[i].compareTo("File") == 0 )
					{

					}
					else
					{
						//create a map of column name and header value
						columnHeaderMap.put(columnName[i], columnHeaders[i]);
						if(i < noOfColumns - 1)
						{
							columnHeaderOrder = columnHeaderOrder +  columnName[i] + ",";
							columnHeaderOrderForFileName = columnHeaderOrderForFileName +  columnName[i] + ",";//To show the "file name" column in File Dialog or windows explorer
						}
						else
						{
							columnHeaderOrder = columnHeaderOrder +  columnName[i];
							columnHeaderOrderForFileName = columnHeaderOrderForFileName +  columnName[i];//To show the "file name" column in File Dialog or windows explorer
						}
					}

					//To show the "file name" column in File Dialog or windows explorer //To fix PBN issue 99354
					if(!isFileNameColumnAdded && columnHeaderOrderForFileName.indexOf(",File,") == -1 && !columnHeaderOrderForFileName.startsWith("File,") && !columnHeaderOrderForFileName.endsWith(",File"))
					{
						if(columnHeaderOrderForFileName.endsWith(","))
							columnHeaderOrderForFileName = columnHeaderOrderForFileName +  "File,";
						else
							columnHeaderOrderForFileName = columnHeaderOrderForFileName +  ",File,";

						isFileNameColumnAdded = true;
					}
				}

				//place the header order to be displayed in the columnHeaderMap
				//this will be used the XML generator to generate the columns in the same order as
				//the table columns
				columnHeaderMap.put("columnHeaderOrder", columnHeaderOrder);
				//place the header list to be displayed in the CSE as the first entry
				//of the return maplist
				finalMapList.add(columnHeaderMap);

				//for collections resultMapList is zero since it is not a BusinessObj
				if(resultMapList == null || resultMapList.size() == 0)
					finalMapList.addAll(totalResultList);


				Hashtable integrationNameHT =null;
				if(!tableName.equals("AEFCollection"))
				{
					IEFGuessIntegrationContext_mxJPO guessIntegration = new IEFGuessIntegrationContext_mxJPO(context, null);
					integrationNameHT = guessIntegration.getIntegrationNameForBusIds(context, bol_array);
				}
					
				Iterator resultMapListItr       = resultMapList.iterator();
				int fileCount                   = 0;
				String isVersionObj             = "false";
				int rowCount                    = 1;
				HashMap objectIdCount			= new HashMap();
				while(resultMapListItr.hasNext())
				{
					Map sResultMap      = (Map) resultMapListItr.next();
					HashMap finalMap    = new HashMap();

					String rowName      = null;

					for (int i = 0; i < noOfColumns; i++)
					{
						//the resultMap has the values based on the order of the bus select and rel selects
						//this order is not same as table column, so generate the final map using the column names
						//in the same sequence as defined in the table
						String sResultValue = (String) sResultMap.get(selectStrings[i]);

						//special case for Collections and saved queries
						if(columnName[i].equalsIgnoreCase("name") && sResultValue != null)
							rowName = sResultValue;

						if(columnName[i].equalsIgnoreCase("type"))
						{
								
							sResultValue=MCADMxUtil.getNLSName(context, "Type", sResultValue, "", "", language);					
						}
						
						
						if(columnName[i].equalsIgnoreCase("state"))
						{
						
							String policyName=(String)sResultMap.get("policy");
							sResultValue=MCADMxUtil.getNLSName(context, "State", sResultValue, "Policy",policyName , language);
	}
						if(columnName[i].equalsIgnoreCase("LockedBy"))
						{
							sResultValue=MCADMxUtil.getNLSName(context, "Person", sResultValue, "","", language);
		
						}
					
						
						//FIXTHIS : for now ignore programHTML output and icons.
						//Client side cannot display it
						if ( selectTypes[i].compareTo(typeImage) == 0 || selectTypes[i].compareTo("icon") == 0  || selectTypes[i].compareTo("programHTMLOutput") == 0 || columnName[i].compareTo("PopupName") == 0)
						{

						}
						else
						{
							if(sResultValue == null || "&nbsp;".equals(sResultValue) || "".equals(sResultValue) )
							{
								finalMap.put(columnName[i], "");
							}
							else
							{
								finalMap.put(columnName[i], sResultValue);
							}
						}
					}

					//special case for Collections and Saved Queries
					//they do not have TNR or busId so put a fake symbolicname and _objectName
					if(tableName.equals("AEFCollection"))
					{
						finalMap.put("_symbolicName", "collections");
						finalMap.put("_superType", "container");
						//finalMap.put("_objectName", rowName);
						//Following line will be used by CSE to form a unique key.
						String uniqueKey = IEFDesktopCommandManager.getUniqueRowIdentifier(rowName, null, Integer.toString(rowCount));
						finalMap.put("_keyname", uniqueKey);
						String collectionId = (String)sResultMap.get("id[connection]");
						finalMap.put("_objectName", collectionId);
						sResultMap.remove("id[connection]");
						rowCount++;
					}
					else
					{
						//add _objectId, _objectType, _objectAccess, _symbolicName and _containerType
						//as column names this is not used for display but for the CSE to build
						//the request for accessing the object displayed
						//the names of the column added are prefixed "_" so that the CSE can skip
						//them from displaying it in the UI.
						String busId        = (String)sResultMap.get("id");
						String busTypeName  = (String)sResultMap.get("type");
						String busName      = (String)sResultMap.get("name");
						String busRev       = (String)sResultMap.get("revision");

						finalMap.put("_objectName", busName);
						finalMap.put("_objectRev", busRev);
						
						
						if(sResultMap.containsKey(ACTIVE_VERSION_ID)){
							StringList activeversion=(StringList)sResultMap.get(ACTIVE_VERSION_ID);
								if(activeversion.size()==1)
								{		
									String activeMinorbusIdbusId=(String)activeversion.get(0); 										
									String Args[] = new String[3];
									Args[0]=activeMinorbusIdbusId;
									Args[1]="current.access";
									Args[2]="dump";
									String mqlCMD="print bus $1 select $2 $3";
									String sResult = util.executeMQL(mqlCMD,context,Args);
									String resultMQL = "";
									if(sResult.startsWith("true")){
									
										resultMQL = sResult.substring(5);
									}
									else
									{
										// throw mcad exception, return for the time being
										//return result;
										resultMQL = sResult.substring(6);
										MCADServerException.createException(resultMQL, null);
									}	
													
									finalMap.put("_objectAccessActiveMinor", resultMQL);
								}
						}
		
						finalMap.put("_objectAccess", sResultMap.get("current.access"));
						finalMap.put("_objectId", busId);

						long lHashCode = rowCount;

						if(busTypeName != null && !busTypeName.equals(""))
							finalMap.put("_objectType", busTypeName);
						else
							finalMap.put("_objectType", "_keyname" + busName + "_@" + lHashCode);

						String tempBOID = (String) sResultMap.get("id");


						int objCount	= 0;

						if(!objectIdCount.containsKey(tempBOID))
						{
							objectIdCount.put(tempBOID,new Integer(0));
							objCount = 0;
						}
						else
						{		
							objCount = ((Integer)objectIdCount.get(tempBOID)).intValue();
							objCount++;
							objectIdCount.put(tempBOID,new Integer(objCount));	  
						}

						String uniqueKey = IEFDesktopCommandManager.getUniqueRowIdentifier(busName, tempBOID, Integer.toString(objCount));
						finalMap.put("_keyname", uniqueKey);
						iLevel++;
						rowCount++;

						String objType          = busTypeName;

						String symbolicName     = FrameworkUtil.getAliasForAdmin(context,"type",objType,true);
						finalMap.put("_symbolicName", symbolicName);

						String hasFile          = null;

						//If the current object is a document then set it the superType as "document"
						//else set it as "container". Using this key the office integration client side
						//will show the objects as either folder or documents
						//Navigate to the right objects inorder to take care of the CDM use cases
						boolean isCommonDocumentLike = false;
						boolean isVersionable        = false;
						boolean isCADLike			 = false;

						MCADGlobalConfigObject gco = null;
						String cadType =null;
						StringList formatList = null;

						if(busId != null && !busId.equals(""))
						{
							String integrationName = (String)integrationNameHT.get(busId);

							cadType             = util.getAttributeForBO(context, busId, MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType"));

							if(integrationName != null && !integrationName.equals(""))
							{
								gco = (MCADGlobalConfigObject) gcoTable.get(integrationName);
								finalMap.put("integrationname", integrationName);
							}

							if(gco != null)
							{
								isCommonDocumentLike =  true;

								if(gco.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_CADMODEL_LIKE) || gco.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
								{
									isCADLike =true;
									if(gco.isTypeOfClass(cadType,MCADAppletServletProtocol.TYPE_ASSEMBLY_LIKE) || gco.isTypeOfClass(cadType,MCADAppletServletProtocol.TYPE_ASSEMBLY_FAMILY_LIKE))
										finalMap.put("_superType", "cadassembly");
									else if(gco.isTypeOfClass(cadType,MCADAppletServletProtocol.TYPE_COMPONENT_LIKE) || gco.isTypeOfClass(cadType,MCADAppletServletProtocol.TYPE_COMPONENT_FAMILY_LIKE))
										finalMap.put("_superType", "cadcomponent");
									else 
										finalMap.put("_superType", "caddrawing");
								}
								else
								{
									//MSDI Specific
									if(gco.isTypeOfClass(cadType,MCADAppletServletProtocol.TYPE_ASSEMBLY_LIKE) || gco.isTypeOfClass(cadType,MCADAppletServletProtocol.TYPE_COMPONENT_LIKE))
										finalMap.put("_superType", "document");
								}
							}
							else if(objType != null && !objType.equals(""))
							{
								String parentType    = getParentType(context, objType);
								isCommonDocumentLike = parentType.equals(CommonDocument.TYPE_DOCUMENTS);

								if(isCommonDocumentLike)
									finalMap.put("_superType", "document");
							}

							//Check whether the object is versionable or not
							Hashtable argsTable = new Hashtable(1);
							argsTable.put("busId", busId);
							Boolean isVersionableObject = (Boolean)JPO.invoke(context, "IEFCDMUtil", new String[] {busId}, "iefIsVersionable", JPO.packArgs(argsTable), Boolean.class);

							isVersionable = isVersionableObject.booleanValue();
						}

						if(isCommonDocumentLike)//[SUPPORT]
						{
							//To show the "file name" column in File Dialog or windows explorer
							if(showFileNameColumn && !tableName.equals("APPFileVersions")&& !tableName.equals("APPFileSummary"))
							{
								String fileNameString = UINavigatorUtil.getI18nString("emxIEFDesignCenter.Common.MSDIFileName", UINavigatorUtil.getStringResourceFileId("IntegrationFramework"), language);
								if(!columnHeaderMap.containsKey("File") && !columnHeaderMap.containsValue(MCADUtil.removeSpace(fileNameString)))
								{
									columnHeaderMap.put("File", fileNameString);
									columnHeaderMap.put("columnHeaderOrder", columnHeaderOrderForFileName);
								}
								//place the header list to be displayed in the CSE as the first entry
								//of the return maplist
								//To fix PBN issue 99354
								if(finalMapList.contains(columnHeaderMap))
								{
									int index = finalMapList.indexOf(columnHeaderMap);
									if(index >= 0)
									{
										finalMapList.remove(index);
										finalMapList.add(index, columnHeaderMap);
									}
									else
										finalMapList.add(0, columnHeaderMap);
								}
								else
									finalMapList.add(0, columnHeaderMap);
							}

							String superType = (String)finalMap.get("_superType");

							if(actionMenuName != null && superType != null && (superType.equals("cadcomponent") || superType.equals("cadassembly") || superType.equals("caddrawing")))
								finalMap.put("_menukey", actionMenuName);

							Object obj = sResultMap.get(CommonDocument.SELECT_IS_VERSION_OBJECT);

							if(obj != null)
								isVersionObj = (String) obj;
							else
								isVersionObj = "false";

							finalMap.put("_minorObject", isVersionObj);

							//the object under consideration is a major BO
							if(isVersionObj != null && isVersionObj.equalsIgnoreCase("false"))
							{

								String latestRevision = (String) sResultMap.get(SELECT_LATEST_REVISION);
								finalMap.put("LatestRevision", latestRevision);
								String sFileNames = null;
								hasFile = (String) sResultMap.get(DomainConstants.SELECT_FORMAT_HASFILE);

								Object  filenames = sResultMap.get(DomainConstants.SELECT_FILE_NAME);
								Object  fileformats = sResultMap.get(DomainConstants.SELECT_FILE_FORMAT);

								StringList fileList = null;

								String sFileName	= null;

								if(filenames instanceof String)
									sFileName = (String) filenames;
								else
								{
									fileList = (StringList)filenames;
									formatList =(StringList)fileformats;
								}

								if((fileList != null && !fileList.isEmpty()) && (sFileName == null || sFileName.equals("")))
								{
									sFileName = (String) fileList.get(0);
									 sFileNames = FrameworkUtil.join(fileList, ",");
								}

								if(sFileName == null || sFileName.equals(""))
								{
									filenames = sResultMap.get(SELECT_ON_ACTIVE_MINOR + DomainConstants.SELECT_FILE_NAME);
									fileformats = sResultMap.get(SELECT_ON_ACTIVE_MINOR +DomainConstants.SELECT_FILE_FORMAT);

									if(bwsl != null)
									{
										BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)bwsl.elementAt(new Long(iLevel-1).intValue());  
										filenames = busObjectWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + DomainConstants.SELECT_FILE_NAME);
										fileformats = busObjectWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + DomainConstants.SELECT_FILE_FORMAT);
									}

									if(filenames instanceof String)
										sFileName = (String) filenames;
									else
									{
										fileList = (StringList) filenames;
										formatList =(StringList)fileformats;
									}
								}

								if((fileList != null && !fileList.isEmpty()) && (sFileName == null || sFileName.equals("")))
								{
									sFileName = (String) fileList.get(0);
									sFileNames = (String) fileList.get(0);
								}

								if(sFileName == null || sFileName.equals(""))
								{
									sFileName = (String) sResultMap.get("FileName");

									if(sFileName == null || sFileName.equals(""))
									{
										sFileName = (String) finalMap.get("FileName");
									}
								}

								if(hasFile != null && !"".equals(hasFile))
								{
									if(hasFile.indexOf("true") != -1 || hasFile.indexOf("TRUE") != -1)
										hasFile = "true";
									else
										hasFile = "false";
								}

								String multiFile = "false";

								if(fileList != null && fileList.size() > 1 && !tableName.equalsIgnoreCase("APPFileSummary"))
								{
									multiFile = "true";
								}

								StringList filenamesList = new StringList();
								
								if (multiFile.equals("true"))
								{
									
									if(gco != null)
									{
										String sMappedFormat = null; 
										if(!util.isMajorObject(context, busId))//gco.isMinorType(busTypeName)) [NDM] OP6 
										{		 
											sMappedFormat=  gco.getFormatsForType(gco.getCorrespondingType(busTypeName),cadType);
										}
										else 
											sMappedFormat = gco.getFormatsForType(busTypeName,cadType);

										if(sMappedFormat !=null && !sMappedFormat.equals(""))
										{
											ListIterator formatListItr = formatList.listIterator();
											ListIterator fileListItr = fileList.listIterator();
											StringList slPositionsToIgnore = new StringList();

											int iPos = 0;
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
										}
									}else{
									String isDocument=(String)sResultMap.get(IS_DOCUMENTType);
											if(isDocument.equalsIgnoreCase("true")){
											
												ListIterator formatListItr = formatList.listIterator();
												ListIterator fileListItr = fileList.listIterator();
												StringList slPositionsToIgnore = new StringList();
												 int iPos = 0;
												while(formatListItr.hasNext())
												{
													String format = (String)formatListItr.next();
													if(format !=null)
													{
														if(format.equals(CommonDocument.FORMAT_MX_MEDIUM_IMAGE))
														{
															System.out.println();
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

									}
									}
									if(! filenamesList.isEmpty())
									{
										sFileName = (String) filenamesList.get(0);
										sFileNames = FrameworkUtil.join(filenamesList, ",");
									}
								}
								
								/*added for IR-483991 to check input object contains multiple files or not after deleting dependant/redundant files*/
								if(filenamesList.size() == 1)
									multiFile = "false";
								
								finalMap.put("_fileExists", hasFile);
								finalMap.put("_multiFile", multiFile);

								if(isVersionable)
								{
									finalMap.put("_fileName",  sFileNames);
									//To show the "file name" column in File Dialog or windows explorer
									//if("false".equalsIgnoreCase(multiFile))
									finalMap.put("File",  sFileNames);
									//else
									//finalMap.put("File",  "--");
								}
								else if(tableName.equalsIgnoreCase("APPFileSummary"))
								{
									finalMap.put("_fileName",  fileList.elementAt(fileCount));
									//To show the "file name" column in File Dialog or windows explorer
									if("false".equalsIgnoreCase(multiFile))
										finalMap.put("File",  fileList.elementAt(fileCount));
									else
										finalMap.put("File",  "--");

									//FIX for showing file names in case of non-versionable objects having multiple files
									finalMap.put("Name", fileList.elementAt(fileCount));
									finalMap.put("_minorObject", "True");
									MCADMxUtil _util = new MCADMxUtil(context, null, new IEFGlobalCache());
									if(_util.isCDMInstalled(context))
										finalMap.put("Version", "");
									else
										finalMap.put("Version", fileList.elementAt(fileCount));

									fileCount++;
								}
								else
								{
									//This is non versionable id and table name is MsoiDocumentSummary
									finalMap.put("_fileName",  sFileName);
									//To show the "file name" column in File Dialog or windows explorer
									//if("false".equalsIgnoreCase(multiFile))
									finalMap.put("File",  sFileNames);
									//else
									//    finalMap.put("File",  "--");
								}
								//[1]
								
								String isMultiFile = (String) finalMap.get("_multiFile");
								if(isCADLike && isMultiFile.equals("false"))
								{

									finalMap.put("_fileLocker", sResultMap.get(DomainConstants.SELECT_LOCKER));
									finalMap.put("_fileLocked", sResultMap.get(DomainConstants.SELECT_LOCKED));
								}
								else if (isCADLike && isMultiFile.equals("true")) 
								{
									finalMap.put("_fileLocker", "");
									finalMap.put("_fileLocked", "");
								}
								if( ! isCADLike && isMultiFile.equals("false"))
								{
									finalMap.put("_fileLocker", sResultMap.get(SELECT_ON_ACTIVE_MINOR + DomainConstants.SELECT_LOCKER));
									finalMap.put("_fileLocked", sResultMap.get(SELECT_ON_ACTIVE_MINOR + DomainConstants.SELECT_LOCKED));
								}
								else if (! isCADLike && isMultiFile.equals("true")) 
								{
									finalMap.put("_fileLocker", "");
									finalMap.put("_fileLocked", "");
								}
								//[/1]
							}
							else
							{
								String  latestVersion  = (String) sResultMap.get(SELECT_ON_ACTIVE+SELECT_LATEST_VERSION_ID);
								if(latestVersion == null)
									finalMap.put("IsLatestVersion", "False");
								else
									finalMap.put("IsLatestVersion", "True");
								  
								StringList fileName = (StringList) sResultMap.get(DomainConstants.SELECT_FILE_NAME);

								String sFileName = null;

								if(fileName.size() == 0 || ((String) fileName.get(0)) == null || ((String) fileName.get(0)).equals(""))
								{
									Object  value = sResultMap.get(SELECT_ON_MAJOR + DomainConstants.SELECT_FILE_NAME);

									if(value instanceof String)
										sFileName = (String) value;
									else
										fileName = (StringList) value;
								}

								//for version objects which is not "LATEST" there is only one file per object
								//and the file resides in the version BO
								if( ( fileName != null && !fileName.isEmpty() )&& (sFileName == null || "".equals(sFileName)))
									sFileName = (String) fileName.get(0);

								if(sFileName == null || "".equals(sFileName))
								{
									//the object under consideration is a version BO which is the "LATEST" file
									Object objHasFile = sResultMap.get(CDM_SELECT_ACTIVE_FORMAT_HAS_FILE);

									if(objHasFile != null)
										hasFile= (String) objHasFile;  //[SUPPORT]
									else
										hasFile = "false";

									if(hasFile != null && !"".equals(hasFile))
									{
										if(hasFile.indexOf("true") != -1 || hasFile.indexOf("TRUE") != -1)
											hasFile = "true";
										else
											hasFile = "false";
									}

									Object objFileName = sResultMap.get(CommonDocument.SELECT_TITLE);
									if(objFileName != null && (((String) objFileName).trim()).length() != 0)
										sFileName = (String) objFileName;//[SUPPORT]
									else
										sFileName = (String) finalMap.get("Name");
								}
								else
								{
									//this is a version object rendering case
									//the object under consideration is a version BO rendered in list versions
									//and has the files in the object itself
									hasFile = "true";
								}

								finalMap.put("_fileExists", hasFile);
								//for version objects there are no multifiles so by default set it to "false"
								finalMap.put("_multiFile", "false");
								finalMap.put("_fileName",  sFileName);
								//To show the "file name" column in File Dialog or windows explorer
								finalMap.put("File",  sFileName);
								if( finalMap.get("_multiFile").equals("false"))
								{
								finalMap.put("_fileLocker", sResultMap.get(DomainConstants.SELECT_LOCKER));
								finalMap.put("_fileLocked", sResultMap.get(DomainConstants.SELECT_LOCKED));
							}
								else
								{
									finalMap.put("_fileLocker", "");
									finalMap.put("_fileLocked", "");	
								}
							}
						}
						else if(objType != null && !objType.equals(""))
						{
							finalMap.put("_superType", "container");

							finalMap.put("File",  "");
							//To show the context menus "Edit in MSP" and "View in MSP" in CSE file dialog and windows explorer
							//If PMC change the lookup for project then we need to change the following if condition. Look the file emxProgramCentralWBSSummaryFS.jsp to get how the "isProject" variable is set to true

							MQLCommand mqlCommand   = new MQLCommand();
							boolean bReturnVal      = mqlCommand.executeCommand(context, "print type $1 select derivative", typeProjectSpace );
							String result           = "";
							boolean bIsProjectType  = false;

							if (bReturnVal)
							{
								result = mqlCommand.getResult();
								if ((result == null) || (result.equals("")) || !(result.length() > 0))
								{
									throw new MatrixException("Null value returned.");
								}
								if(result.endsWith("\n"))
								{
									result = result.substring(0, (result.lastIndexOf("\n")));
									String testString = new String ("derivative = " + objType);
									if(result.indexOf(testString) != -1)
										bIsProjectType = true;
								}
							}

							if(objType != null && objType.equals(MCADMxUtil.getActualNameForAEFData(context, "type_ProjectSpace")))
								bIsProjectType = true;

							if(objType != null && bIsProjectType)
							{
								//This is a project data from PMC
								//To show the context menus "Edit in MSP" and "View in MSP" only when it is a project
								//Check the property 'emxProgramCentral.MSDI.MSPComponentInstalled'
								String MSDIPropertyKey = "emxProgramCentral.MSDI.MSPComponentInstalled";
								String MSDIPropertyValue = FrameworkProperties.getProperty(MSDIPropertyKey);
								//Show the context menus "Edit in MSP" and "View in MSP" only if the property is set to true
								if(MSDIPropertyValue != null && "true".equalsIgnoreCase(MSDIPropertyValue))
								{
									finalMap.put("_editProject", "false");
									HashMap argsMap = new HashMap(2);
									argsMap.put("busIdToCheck", busId);
									argsMap.put("accessConstantModify", Short.toString((short) AccessConstants.cModify));
									Boolean editFlag = (Boolean) JPO.invoke(context, "IEFCDMSupport", null, "checkAccess", JPO.packArgs(argsMap), Boolean.class); 

									if(editFlag.booleanValue())
										finalMap.put("_editProject", "true");
								}
							}
						}
						else
						{
							String nodeType = (String) sResultMap.get("nodeType");

							if(nodeType != null)
							{
								if(nodeType.equalsIgnoreCase("file"))
								{
									finalMap.put("_superType", "file"); 

									finalMap.put("_minorObject", "false");
									finalMap.put("_fileExists", "false");
									finalMap.put("_multiFile", "false");

									String fileName = (String) sResultMap.get(DomainConstants.SELECT_FILE_NAME);

									if(fileName == null || fileName.equals(""))
									{
										fileName = (String) sResultMap.get("FileName");

										if(fileName == null || fileName.equals(""))
										{
											fileName = (String) finalMap.get("FileName");
										}
									}

									finalMap.put("_fileName", fileName);

									finalMap.put("File",  sResultMap.get(DomainConstants.SELECT_FILE_NAME));
									
									if( finalMap.get("_multiFile").equals("false"))
									{
									finalMap.put("_fileLocker", sResultMap.get(DomainConstants.SELECT_LOCKER));
									finalMap.put("_fileLocked", sResultMap.get(DomainConstants.SELECT_LOCKED));
								}
									else
									{
										finalMap.put("_fileLocker", "");
										finalMap.put("_fileLocked", "");		
									}
									
								}
								else if(nodeType.equalsIgnoreCase("folder"))
								{
									finalMap.put("_superType", "container");

									finalMap.put("_keyname", busName);
									finalMap.put("File",  "");
								}
							}
						}
					}

					if(finalMap != null)
					{
						Iterator itr           = sResultMap.keySet().iterator();

						while(itr.hasNext())
						{
							Object key = itr.next();

							if(nonExistingKeysList.contains(key) && !finalMap.containsKey(key))
							{
								Object value = sResultMap.get(key);

								if(value instanceof String)
									finalMap.put(key, value);
							}
						}

						finalMapList.add(finalMap);
					}
				}
			} // end of columns null check
		} // end of tableMap null check

		return finalMapList;
	}

	public boolean isItContainer(Context context, String busTypeName, String busName, String busRev) throws Exception
	{
		boolean sRetVal = false;
		com.matrixone.MCADIntegration.server.beans.MCADMxUtil _util = new com.matrixone.MCADIntegration.server.beans.MCADMxUtil(context,null, new IEFGlobalCache());
		String Args[] = new String[5];
		Args[0] = busTypeName;
		Args[1] = busName;
		Args[2] = busRev;
		Args[3] = "from[].businessobject.Type.derived";
		Args[4] = "|";
		String sResult = _util.executeMQL(context,"temp query bus $1 $2 $3 select $4 dump $5", Args);

		if(sResult.startsWith("true"))
		{
			sResult = sResult.substring(5);
			if (sResult.indexOf("DOCUMENTS") > -1 || sResult.indexOf("Document") > -1)
				sRetVal = true;
			else
				sRetVal = false;
		}
		else
		{
			sRetVal = false;
		}

		return sRetVal;

	}

	//Get the top level parent for a given type
	public String getParentType(Context context, String type) throws Exception
	{
		String parentType = (String)_parentTypeMapping.get(type);
		if ( parentType == null )
		{
			setParentTypeMapping(context, type);
		}
		parentType = (String)_parentTypeMapping.get(type);
		if ( parentType != null )
		{
			return parentType;
		} else {
			return type;
		}
	}

	//Set the top level parent for a given type
	public void setParentTypeMapping(Context context, String type) throws Exception
	{
		String currentType = type;
		BusinessType bType = new BusinessType(currentType, context.getVault());
		String parentType = bType.getParent(context);
		//iterate until the top level parent is obtained
		while ( parentType != null && !"".equals(parentType))
		{
			currentType = parentType;
			bType = new BusinessType(currentType, context.getVault());
			parentType = bType.getParent(context);
		}
		if ( parentType != null && !"".equals(parentType) )
		{
			_parentTypeMapping.put(type, parentType);
		} else {
			_parentTypeMapping.put(type, currentType);
		}
	}

	private String substituteValues(Context context, String expression) throws FrameworkException
	{

		try
		{
			// Initialize the return string.
			StringBuffer message = new StringBuffer();

			// Create a mapping from token type to a set
			// that holds selects, macros, and variables.
			Map expressions = new HashMap(3);
			expressions.put(SELECT, new HashSet());
			expressions.put(MACRO, new HashSet());
			expressions.put(VARIABLE, new HashSet());

			// Create a mapping from token type to a map that maps
			// the selects, macros, and variables to their values.
			Map values = new HashMap(3);
			values.put(SELECT, new HashMap());
			values.put(MACRO, new HashMap());
			values.put(VARIABLE, new HashMap());

			// Create lists to hold the tokens and their types.
			ArrayList tokens = new ArrayList();
			ArrayList types = new ArrayList();

			// If expression starts with program then
			// do not evaluate macros in the expression
			boolean bIsProg = false;
			if (expression.trim().startsWith("program["))
			{
				bIsProg = true;
			}

			// Parse the input string into typed tokens.
			// Loop through the tokens until the end of the string.
			StringTokenizer st = new StringTokenizer(expression, DELIMITER_START, true);

			while (true)
			{
				String token = null;
				String type = null;

				try
				{
					// get everything up to the first delimiter
					token = st.nextToken(DELIMITER_START);

					// if this is the delimiter, then determine the token type
					if ("$".equals(token))
					{
						type = st.nextToken(DELIMITER_TYPE);

						// look for the matching end token
						String delimiter = (String) delimiters.get(type);

						// if this wasn't a known delimiter
						if (delimiter == null)
						{
							// must have been some text
							tokens.add(type);
							types.add(TEXT);
						}
						else
						{
							token = st.nextToken(delimiter);
							String sOriginalToken=token;
							if (type.equals(SELECT) && token.indexOf("_") > -1)
							{
								token = replaceSymbolicTokens(context, token);
							}

							tokens.add(token);
							types.add(type);
							if(!(sOriginalToken.startsWith("type_") ||
									sOriginalToken.startsWith("attribute_") ||
									sOriginalToken.startsWith("relationship_") ||
									sOriginalToken.startsWith("policy_") ||
									sOriginalToken.startsWith("vault_")))
							{
								((HashSet) expressions.get(type)).add(token);
							}

							// throw out the matching end token
							token = st.nextToken();
						}
					}
					// otherwise, save the token as text
					else
					{
						// must have been some text
						tokens.add(token);
						types.add(TEXT);
					}

				}
				catch (NoSuchElementException e)
				{
					break;
				}
			}

			// Get the selectable information from the object.
			// Create a select list from the select set.
			StringList selects = new StringList(((HashSet) expressions.get(SELECT)).size());
			Iterator itr = ((HashSet) expressions.get(SELECT)).iterator();

			while (itr.hasNext())
			{
				selects.add((String) itr.next());
			}

			if(selects!= null && selects.contains("current")) {
				selects.add("policy");
			}

			// Get the macro information from the RPE.
			// Build the macro map from the macro set.
			Map macros = new HashMap();

			itr = ((HashSet) expressions.get(MACRO)).iterator();
			while (itr.hasNext())
			{
				// get the data from the RPE
				String name = (String) itr.next();
				macros.put(name, MqlUtil.mqlCommand(context,"get env $1 $2", "global", name));
			}

			// Save the macro map.
			values.put(MACRO, macros);

			// Get the variable information from the RPE.
			// Build the variable map from the variable set.
			Map variables = new HashMap();

			itr = ((HashSet) expressions.get(VARIABLE)).iterator();
			while (itr.hasNext())
			{
				// get the data from the variable depending on the prefix
				String name = (String) itr.next();
				String value = null;
				// get info from the jsp request variable
				if (name.startsWith(SCHEMA))
				{
					value = (String) PropertyUtil.getSchemaProperty(context,
							name.substring(SCHEMA.length()));
				}

				variables.put(name, value);
			}

			// Save the variable map.
			values.put(VARIABLE, variables);

			// Put the string back together while replacing tokens with values.
			// Loop through the token and type list together.
			Iterator tokenItr = tokens.iterator();
			Iterator typeItr = types.iterator();

			while (tokenItr.hasNext())
			{
				String token = (String) tokenItr.next();
				String type = (String) typeItr.next();

				// Get the appropriate token map based on the type.
				Map valueMap = (Map) values.get(type);
				// If the type map is null, then the token is text,
				// so just put it in the message.
				if (valueMap == null)
				{
					message.append(token);
				}
				else
				{
					// if expression is calling program
					// then do not resolve MACRO
					if (type.equals(MACRO) && bIsProg)
					{
						String value = "$" + MACRO + token + END_MACRO;
						message.append(value);
					}
					else
					{
						// if there is no value, just place the token
						String value = (String) valueMap.get(token);
						message.append(value == null ? token : value);
					}
				}
			}

			return (message.toString());
		}
		catch (Exception e)
		{
			throw (new FrameworkException(e));
		}
	}

	private static Map delimiters = new HashMap(3);

	static
	{
		delimiters.put(SELECT, END_SELECT);
		delimiters.put(MACRO, END_MACRO);
		delimiters.put(VARIABLE, END_VARIABLE);
	}

	private String replaceSymbolicTokens(Context context, String expression) throws FrameworkException
	{
		String newExpression = expression;

		if (expression.indexOf("_") > -1)
		{
			StringTokenizer st = new StringTokenizer(expression, " [],\'\"", true);
			StringBuffer buffer = new StringBuffer();
			String token;

			while (true)
			{
				try
				{
					token = st.nextToken();

					if (token.indexOf("_") > -1)
					{
						buffer.append(lookupSymbolicToken(context, token));
					}
					else
					{
						buffer.append(token);
					}
				}
				catch (NoSuchElementException e)
				{
					break;
				}
			}

			newExpression = buffer.toString();
		}

		return (newExpression);
	}

	protected static String lookupSymbolicToken(Context context, String symbolicToken)
	{
		String token = null;

		try
		{
			token = PropertyUtil.getSchemaProperty(context,symbolicToken);
		}
		catch (Exception e)
		{
		}

		if (token == null || token.length() == 0)
		{
			token = symbolicToken;
		}

		return (token);
	}
}

