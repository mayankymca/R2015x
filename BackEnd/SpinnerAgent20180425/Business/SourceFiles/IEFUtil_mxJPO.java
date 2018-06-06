//IEFUtil.java


//Copyright (c) 2002 MatrixOne, Inc.
//All Rights Reserved
//This program contains proprietary and trade secret information of
//MatrixOne, Inc.  Copyright notice is precautionary only and does
//not evidence any actual or intended publication of such program.

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.MatrixWriter;
import matrix.util.MatrixException;
import matrix.util.StringItr;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFCache;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADXMLUtils;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNodeImpl;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIInquiry;
import com.matrixone.apps.framework.ui.UIMenu;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
/**
 * The <code>IEFUtil</code> class represents the JPO for
 * obtaining the MS Office integration menus
 *
 * @version AEF 10.5 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class IEFUtil_mxJPO
{
    private StringBuffer selectStateType                = new StringBuffer("");
    private StringBuffer selectStringDataType           = new StringBuffer("");
    private StringBuffer selectNumericDataType          = new StringBuffer("");
    private StringBuffer selectTimeDataType             = new StringBuffer("");
    private StringBuffer selectBooleanDataType          = new StringBuffer("");
	private StringBuffer selectBinaryDataType           = new StringBuffer("");                           //L86 : 		IR-542275-3DEXPERIENCER2015x
	private StringBuffer selectOtherDataTypeGeneric = new StringBuffer("");            //L86 : 		IR-542275-3DEXPERIENCER2015x
    private BufferedWriter writer                       = null;
    private IEFCache  _GlobalCache                      = new IEFGlobalCache();
    private String[] initArgs                           = null;
    
    //private static final String MY_CHECKEDOUT_DOCUMENTS     = CommonDocument.DEFAULT_DOCUMENT_TYPE + "," + PropertyUtil.getSchemaProperty("type_GenericDocument") +  "," + PropertyUtil.getSchemaProperty("type_CADDrawing") + "," + PropertyUtil.getSchemaProperty("type_CADModel");

    /**
     * Constructs a new IEFUtil JPO object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args an array of String arguments for this method
     * @throws Exception if the operation fails
     * @since AEF 10.5
     */
    public IEFUtil_mxJPO (Context context, String[] args) throws Exception
    {
        // Call the super constructor
        super();
        initArgs = args;
    }

    /**
     * Parses the HREF values of a command and puts the parameter value
     * in a Map
     * Returns a map which has the name value pair of the params
     *
     * @param context the eMatrix <code>Context</code> object
     * @param commandName the name of the command being evaluated
     * @param commandHRef the HRef value of the command
     * @throws Exception if the operation fails
     * @since AEF 10.5
     */

    public Map getHRefParams(Context context, String[] args) throws Exception
    {
        HashMap argsTable  = (HashMap) JPO.unpackArgs(args);
        String commandName      = (String) argsTable.get("commandName");
        String commandHRef      = (String) argsTable.get("commandHRef");
        Map retMap   			= getHRefParams(context, commandName, commandHRef);
        
        return retMap; 
    }
    
    public  Map getHRefParams(Context context, String commandName, String commandHRef) throws MatrixException
    {
    	boolean areParamsProper = false;
        Map HRefParams 		    = new HashMap();
        try
        {
            //check if the command specified is UI3-enabled or not by checking
            //for emxTable.jsp in the HREF string
            //If emxTable.jsp is not found in the HREF throw an error message
            if(commandHRef != null && (commandHRef.indexOf("emxTable.jsp") != -1 || commandHRef.indexOf("?") > 0 ))
            {
            	areParamsProper = true;

            	String params = commandHRef.substring(commandHRef.indexOf("?")+1, commandHRef.length());
                StringTokenizer parser = new StringTokenizer(params,"&");
                while (parser.hasMoreTokens())
                {
                    String sValue = parser.nextToken();
                    if (sValue == null)
                        sValue = "";
                    else
                    {
                        StringTokenizer value = new StringTokenizer(sValue,"=");
                        while (value.hasMoreTokens())
                        {
                            String nValue = value.nextToken();
                            if(value.hasMoreTokens())
                            {
                                HRefParams.put(nValue, value.nextToken());
                            }
                            else
                            {
                                //no value exist for this token
                                HRefParams.put(nValue, "");
                            }
                        }
                    }
                }
            }

            if(!areParamsProper)
            {
                //the command is not UI3 component, the HREF does not have the
                //1. program or inquiry to retrieve objects
                //2. It does not have table name to display the objects
                //
                //throw an error message (this must be an old command
                //which refers to a JSP page)
                throw (new FrameworkException("The command " + commandName + "is not UI3 enabled. The HREF does not use program or inquiry or table to display objects in it. Contact your system administrator."));
            }
        }
        catch (Exception ex)
        {
            throw (new FrameworkException("IEFUtil:getHRefParams : " + ex.toString()) );
        }
        
        return HRefParams;
    }

    public  MapList getObjectMapList(Context context, String[] args) throws Exception
    {
    	Hashtable argsTable = (Hashtable) JPO.unpackArgs(args);
        Map HRefParams      = (Map) argsTable.get("HRefParams");
        String[] arg        = (String[]) argsTable.get("args");
    	return getObjectMapList(context, HRefParams, arg);
    }
    /**
     * Parses the HREF values of a command and puts the parameter value
     * in a Map
     * Returns a map which has the name value pair of the params
     *
     * @param context the eMatrix <code>Context</code> object
     * @param commandName the name of the command being evaluated
     * @param commandHRef the HRef value of the command
     * @throws Exception if the operation fails
     * @since AEF 10.5
     */
       
    public  MapList getObjectMapList(Context context, Map HRefParams, String[] args) throws MatrixException
    {
        String EMPTY_STRING  = "";

        String lang = args[0];
        String commandName = args[1];
        String objectId = null;
        String parentRelName = null;
       if(args.length >= 4)
        {
            objectId = args[2];
            if("Command".equals(objectId))
                objectId = null;

            parentRelName = args[3];
        }

        if("Command".equals(objectId))
            objectId = null;

        //get the inquiry or program to be used to get the list of objects
        boolean programExists = true;
        boolean inquiryExists = false;
        String inquiryName = null;
        if(parentRelName == null || parentRelName.length() == 0)
        {
            parentRelName = (String)HRefParams.get("parentRelName");
            if(parentRelName == null || parentRelName.length() == 0)
            {
                parentRelName = (String)HRefParams.get("rel");
                if(parentRelName == null || parentRelName.length() == 0)
                {
                    parentRelName = (String)HRefParams.get("relName");
                    if(parentRelName == null || parentRelName.length() == 0)
                    {
                        parentRelName = "relationship_ReferenceDocument";
                    }
                }
            }
        }

        String programName = (String) HRefParams.get("program");
        StringTokenizer programs = null;
        //In a command either the programName exists
        //or the inquiryName not both,
        //if the programName is null then get the inquiryName
        if(programName == null || EMPTY_STRING.equals(programName))
        {
            programExists = false;
            //check to see if an inquiry is specified
            inquiryName = (String) HRefParams.get("inquiry");
            if(inquiryName != null && !EMPTY_STRING.equals(inquiryName))
            {
                //more than one inquiry name can exist in the HREF for displaying the
                //results based on the various states of the object,
                //for MSOI we are going to just use the first inquiry
                //Show filters in future releases of MSOI ?????
                inquiryExists = true;
                if(inquiryName.indexOf(",") != -1)
                {
                    inquiryName = inquiryName.substring(0, inquiryName.indexOf(","));
                }
            }
        }
        else
        {
            //more than one program name can exist in the HREF for displaying the
            //results based on the various states of the object,
            //for MSOI we are going to just use the first program
            //Show filters in future releases of MSOI ?????
            inquiryExists = false;
            programExists = true;

            //To handle multiple programs.
            programs = new StringTokenizer(programName,",");

            if(programName.indexOf(",") != -1)
            {
                programName = programName.substring(0, programName.indexOf(","));
            }
        }

        MapList objectMapList = new MapList();
        StringList idList=new StringList();
        if(programExists)
        {
            Map argsMap = new HashMap();
            argsMap.put("args", args);            
            argsMap.put("objectId", objectId);
            //Some JPO uses parentRelName as relationship.
            argsMap.put("parentRelName", parentRelName);
            //Test case and Use case JPO uses rel as relationship.
            argsMap.put("rel", parentRelName);
            //Required for WBS tasks.
            argsMap.put("hRef", HRefParams);

            //currently the core does not support calling new ${CLASS:classname}
            //where classname can be a variable when calling a JPO from one
            //JPO this methodology needs to be called since it is faster
            //so adding the known function names in this format everything
            //else will call JPO.invoke( )
            while(programs.hasMoreTokens())
            {
                programName = (String)programs.nextToken();
                MapList tempObjectMapList = new MapList();
                try
                {
                    int index = programName.indexOf(":");
                    String jpoName = "";
                    String jpoMethodName = "";
                    if (index != -1)
                    {
                        jpoName = programName.substring(0, index);
                        jpoMethodName = programName.substring(index+1);
                        if(jpoMethodName.startsWith("getAllContext"))
                            continue;
                        if(programName.indexOf("emxCommonFileUI:getFiles") != -1 || programName.indexOf("emxCommonFileUI:getFileVersions") != -1)
                        {
                            //Check whether the object has any versions or not. Is it versionable?
                            Hashtable argsTable = new Hashtable(1);
                            argsTable.put("busId", objectId);
                            Boolean isVersionableObject = (Boolean)JPO.invoke(context, "IEFCDMUtil", new String[] {objectId}, "iefIsVersionable", JPO.packArgs(argsTable), Boolean.class);
                            
                            boolean isVersionable = isVersionableObject.booleanValue();
                           
                            MCADMxUtil _util = new MCADMxUtil(context, null,_GlobalCache);

                            if(programName.indexOf("emxCommonFileUI:getFiles") != -1)
                            {
                                if(!isVersionable)
                                {
                                    jpoName = "IEFCDMUtil";
                                    jpoMethodName = "getNonVersionableFiles";
                                }
                            } 
                            else if(programName.indexOf("emxCommonFileUI:getFileVersions") != -1)
                            {
                                if(!isVersionable || !_util.isCDMInstalled(context))
                                {
                                    throw new Exception(i18nNow.getI18nString("mcadIntegration.Server.Message.DocumentHasNoVersions",  "iefStringResource", lang));
                                }
                            }
                        } 
                        else if(programName.indexOf("emxAEFCollection:getObjects") != -1)
                        {
                            String strSystemGeneratedCollectionLabel = UINavigatorUtil.getI18nString("emxFramework.ClipBoardCollection.NameLabel", "emxFrameworkStringResource", lang);
                            if(strSystemGeneratedCollectionLabel.equals(parentRelName))
                            {
                                //Modified for Bug 342586
                                parentRelName = FrameworkProperties.getProperty("emxFramework.ClipBoardCollection.Name");
                            }
                            //in case of collections the parentRelName stores the collection Name
                            //emxAEFCollection:getObjects expects the treeLabel value
                            //as a encoded value, if this value is not encoded this causes
                            //problem in Japanese or multibyte char env (Bug #290407),
                            //therefore encode parentRelName to use JPO emxAEFCollection
                            parentRelName = FrameworkUtil.encodeURL(parentRelName,"UTF-8");

                            // Added as the latest version of emxAEFCollectionBase JPO requires "charSet"......22nd Dec 2006
                            //argsMap.put("charSet", "UTF-8");
                            String mcadCharset = "UTF-8";
			ResourceBundle frameworkBundle = null;
							try
							{
	                             frameworkBundle = ResourceBundle.getBundle("framework");
                            try
                            {
                                mcadCharset = frameworkBundle.getString("ematrix.encoding");
                            }
                            catch(MissingResourceException _ex)
                            {
                                mcadCharset = "UTF-8";
                            }
							}
							catch (Exception e)
							{
							}
							String id = args[3];
							argsMap.put("relId", id);
                            argsMap.put("charSet", mcadCharset);
                            // Added as the latest version of emxAEFCollectionBase JPO requires "charSet"......22nd Dec 2006
                            argsMap.put("treeLabel", parentRelName);
                            argsMap.put("languageStr", args[0]);
                        }
                        else if(programName.indexOf("emxAEFCollection:getCollections") != -1)
                        {
                           // Added as the latest version of emxAEFCollectionBase JPO requires "charSet"......4th Oct 2007
                            argsMap.put("charSet", "UTF-8");
                            // Added as the latest version of emxAEFCollectionBase JPO requires "charSet"......4th Oct 2007
							argsMap.put("languageStr", args[0]);
                        }
                        
                        String[] methodargs = JPO.packArgs(argsMap);
                        
                        tempObjectMapList = (MapList)JPO.invoke(context, jpoName, initArgs, jpoMethodName, methodargs, MapList.class);
                    }
                }
                catch (Exception ex)
                {   
                    System.out.println("[IEFUtil.getObjectMapList] Error while executing program : " + programName);
                    throw (new FrameworkException(ex.getMessage()));
                }

                if(tempObjectMapList != null && !(tempObjectMapList.isEmpty()))
                {
                    int tempObjectMapListSize = tempObjectMapList.size();
					// special handling to get Collections
		    if(programName.equals("emxAEFCollection:getCollections"))
		    {		
                    for (int index = 0; index < tempObjectMapListSize; index++)
                    {
                        Map tempObj = (Map)tempObjectMapList.get(index);
						String id=(String)tempObj.get("id[connection]");
						
				
						//for  IR-475553-3DEXPERIENCER2015x : user unable to create documents in Clipboard(Japanese language)
						// emxAEFCollectionBase JPO -->BPS translates the System generated Colletion (i.e. Clipboard) to locale specific , MSF reads the response and sends the Clipboard name in that specific locale(e.g. Japanese)
						String collname=(String)tempObj.get("name");                		
							
					        String displayname = collname;
                			
                			String strSystemGeneratedCollection = UINavigatorUtil.getI18nString("emxFramework.ClipBoardCollection.NameLabel", "emxFrameworkStringResource", "en");                			
                			
                			String strSystemGeneratedCollectionLabelLang = UINavigatorUtil.getI18nString("emxFramework.ClipBoardCollection.NameLabel", "emxFrameworkStringResource", lang);                			
                			
                			if(strSystemGeneratedCollectionLabelLang.equalsIgnoreCase(collname))
     					   {
     					        collname = strSystemGeneratedCollection;
								tempObj.put("name", collname);
     					   }
						   
						   tempObj.put("displayname", displayname);						                   			                			
                									  					
                        if(!(objectMapList.contains(tempObj))){
                            if(idList.contains(id))			
							 continue;
                            objectMapList.add(tempObjectMapList.get(index));
							idList.add(id);
							
						}	
                    }
                }
					else
						 for (int index = 0; index < tempObjectMapListSize; index++)
                    {
                        Map tempObj = (Map)tempObjectMapList.get(index);
						String id=(String)tempObj.get("id");
                        if(!(objectMapList.contains(tempObj))){
                            if(idList.contains(id))										
							continue;
                            objectMapList.add(tempObjectMapList.get(index));
							idList.add(id);
							
						}	
                    }
                }
            } //End of while(programs.hasMoreTokens())
        } //End of if(programExists)
        else
        {
            if(inquiryExists)
            {
                try
                {
                    objectMapList = UIInquiry.evaluateInquiry(context, inquiryName, null);
                }
                catch (Exception ex)
                {
                    System.out.println("[IEFUtil.getObjectMapList] Error while evaluating inquiry : " + inquiryName);
                    throw (new FrameworkException(ex.getMessage()));
                }
            }
        }
        return objectMapList;
    }

    /**
     * Parses the HREF values of a command and puts the parameter value
     * in a Map
     * Returns a map which has the name value pair of the params
     *
     * @param context the eMatrix <code>Context</code> object
     * @param commandName the name of the command being evaluated
     * @param commandHRef the HRef value of the command
     * @throws Exception if the operation fails
     * @since AEF 10.5
     */

    public StringList evaluateInquiry(Context context, String commandName, String commandHRef) throws MatrixException
    {

        return new StringList();
    }

    /**
     * Parses the HREF values of a command and puts the parameter value
     * in a Map
     * Returns a map which has the name value pair of the params
     *
     * @param context the eMatrix <code>Context</code> object
     * @param commandName the name of the command being evaluated
     * @param commandHRef the HRef value of the command
     * @throws Exception if the operation fails
     * @since AEF 10.5
     */

    public MapList generateTable(Context context, String commandName, String commandHRef) throws MatrixException
    {

        return new MapList();
    }

   public String getBusId(Context context, String[] args) throws Exception
   {
      String busId    = null;
      HashMap argsMap = (HashMap) JPO.unpackArgs(args);
      String type = (String)argsMap.get("type");
      String name = (String)argsMap.get("name");;
      String rev  = (String)argsMap.get("rev");;
      
       try
       {
          MQLCommand mql = new MQLCommand();
			
			boolean eval = mql.executeCommand(context, "print bus $1 $2 $3 select $4 dump $5",type,name,rev,"id","|");
           String result = mql.getResult();
           String error = null;
          if(result != null && ! "".equals(result))
          {
            busId = result.trim();
          }
          else
           {
              error = mql.getError();
              if(error != null && error.length() > 0)
               {
                    int index = error.lastIndexOf(": BusinessObject");
                    if(index > 0)
                   {
                        error = error.substring(index+1);
                   }
                    throw new Exception(error);
               }
           }
       }
       catch (Exception e)
       {
           throw new MatrixException(e.getMessage());
       }
      return busId;
   }
   
    public String getBusId(Context context, String type, String name, String rev) throws MatrixException
    {
        String busId = null;
        try
        {
            MQLCommand mql = new MQLCommand();

            boolean eval = mql.executeCommand(context, "print bus $1 $2 $3 select $4 dump $5",type,name,rev,"id","|");
            String result = mql.getResult();
            String error = null;
            if(result != null && ! "".equals(result))
            {
                busId = result.trim();
            }
            else
            {
                error = mql.getError();
                if(error != null && error.length() > 0)
                {
                    int index = error.lastIndexOf(": BusinessObject");
                    if(index > 0)
                    {
                        error = error.substring(index+1);
                    }
                    throw new Exception(error);
                }
            }
        }
        catch (Exception e)
        {
            throw new MatrixException(e.getMessage());
        }
        return busId;
    }

    /**
     * getMyCheckedOutDocs - Get the list of Master Documents which have
     * at least one file (Version Document) checked out by context user
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @returns eMatrix <code>MapList</code> containing list of Master Documents with at least one checked out file
     * @throws Exception if the operation fails
     * @since DC 10.5
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public  MapList getMyCheckedOutDocs(Context context, String[] args) throws Exception
    {
        MapList newDocList = null;
        try
        {
        	StringList busSelects     = new StringList(1);
            String REL_ACTIVE_VERSION = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
        	
            busSelects.add("to[" + REL_ACTIVE_VERSION + "].from." + DomainConstants.SELECT_ID);
            busSelects.add("to[" + REL_ACTIVE_VERSION + "].from." + DomainConstants.SELECT_TYPE);
            busSelects.add("to[" + REL_ACTIVE_VERSION + "].from." + DomainConstants.SELECT_REVISION);
            busSelects.add("to[" + REL_ACTIVE_VERSION + "].from." + CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
            busSelects.add(CommonDocument.SELECT_TITLE);
            busSelects.add("to[" + REL_ACTIVE_VERSION + "].from." + DomainConstants.SELECT_FILE_NAME);
            busSelects.add("locked");
            busSelects.add("to[" + REL_ACTIVE_VERSION + "].from." + CommonDocument.SELECT_TITLE);
            busSelects.add("to[" + REL_ACTIVE_VERSION + "].from." + DomainConstants.SELECT_NAME);
            busSelects.add("revision");
            busSelects.add("type");
            busSelects.add("to[" + REL_ACTIVE_VERSION + "]." + CommonDocument.SELECT_HAS_ROUTE);
            busSelects.add("to[" + REL_ACTIVE_VERSION + "].from." + CommonDocument.SELECT_SUSPEND_VERSIONING);
            busSelects.add("to[" + REL_ACTIVE_VERSION + "].from." + CommonDocument.SELECT_HAS_CHECKOUT_ACCESS);
            busSelects.add("to[" + REL_ACTIVE_VERSION + "].from." + CommonDocument.SELECT_HAS_CHECKIN_ACCESS);

            //FIXTHIS : For 10.0.1.0 integrations the major object is locked
            //So check if the major is locked
            String attrIsVersionObject 	 = PropertyUtil.getSchemaProperty(context,DomainObject.SYMBOLIC_attribute_IsVersionObject);
            StringBuffer objectCDMWhere  = new StringBuffer();
            Boolean isCDMSupportedObject = (Boolean)JPO.invoke(context, "IEFCDMSupport", null, "iefIsCDMSupported", new String[0], Boolean.class);
            
            boolean isCDMSupported = isCDMSupportedObject.booleanValue();

        	StringBuffer objectNONCDMWhere 	 = new StringBuffer();
        	objectNONCDMWhere.append("(locked== TRUE && locker==\"");
        	objectNONCDMWhere.append(context.getUser());
        	objectNONCDMWhere.append("\")");//[SUPPORT()]  Non CDM platform
        	
        	if(isCDMSupported)
        	{
        		 objectCDMWhere.append("(attribute[");
                 objectCDMWhere.append(attrIsVersionObject);
                 objectCDMWhere.append("]==True && locker==\"");
                 objectCDMWhere.append(context.getUser());
                 objectCDMWhere.append("\")");
        	}
        	else
        		objectCDMWhere = objectNONCDMWhere;

        	// get all DOCUMENTS subtype
            //what happens to integration data?
        	String MY_CHECKEDOUT_DOCUMENTS     = CommonDocument.DEFAULT_DOCUMENT_TYPE + "," + PropertyUtil.getSchemaProperty(context,"type_GenericDocument") +  "," + PropertyUtil.getSchemaProperty(context,"type_CADDrawing") + "," + PropertyUtil.getSchemaProperty(context,"type_CADModel");
            MapList docList = DomainObject.findObjects(context,  MY_CHECKEDOUT_DOCUMENTS, null, objectCDMWhere.toString(), busSelects);
            
            newDocList 		= new MapList(docList.size());
            
            for(int itr=0; itr < docList.size(); itr++)
            {
            	Map docMap = (Map)docList.get(itr);
                Map newDocMap = new HashMap();
                newDocMap.put(DomainConstants.SELECT_ID, docMap.get("to[" + REL_ACTIVE_VERSION + "].from." + DomainConstants.SELECT_ID));
                newDocMap.put(DomainConstants.SELECT_TYPE, docMap.get("to[" + REL_ACTIVE_VERSION + "].from." + DomainConstants.SELECT_TYPE));
                newDocMap.put(DomainConstants.SELECT_REVISION, docMap.get("to[" + REL_ACTIVE_VERSION + "].from." + DomainConstants.SELECT_REVISION));
                newDocMap.put(CommonDocument.SELECT_MOVE_FILES_TO_VERSION, docMap.get("to[" + REL_ACTIVE_VERSION + "].from." + CommonDocument.SELECT_MOVE_FILES_TO_VERSION));
                newDocMap.put(DomainConstants.SELECT_FILE_NAME, docMap.get("to[" + REL_ACTIVE_VERSION + "].from." + DomainConstants.SELECT_FILE_NAME));
                newDocMap.put(CommonDocument.SELECT_ACTIVE_FILE_LOCKED, docMap.get("to[" + REL_ACTIVE_VERSION + "].from." + CommonDocument.SELECT_ACTIVE_FILE_LOCKED));
                newDocMap.put(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION, docMap.get("to[" + REL_ACTIVE_VERSION + "].from." + CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION));
                newDocMap.put(CommonDocument.SELECT_TITLE, docMap.get("to[" + REL_ACTIVE_VERSION + "].from." + CommonDocument.SELECT_TITLE));
                newDocMap.put(DomainConstants.SELECT_NAME, docMap.get("to[" + REL_ACTIVE_VERSION + "].from." + DomainConstants.SELECT_NAME));
                newDocMap.put(CommonDocument.SELECT_ACTIVE_FILE_VERSION, docMap.get(DomainConstants.SELECT_REVISION));
                newDocMap.put(CommonDocument.SELECT_HAS_ROUTE, docMap.get("to[" + REL_ACTIVE_VERSION + "]." + CommonDocument.SELECT_HAS_ROUTE));
                newDocMap.put(CommonDocument.SELECT_SUSPEND_VERSIONING, docMap.get("to[" + REL_ACTIVE_VERSION + "].from." + CommonDocument.SELECT_SUSPEND_VERSIONING));
                newDocMap.put(CommonDocument.SELECT_HAS_CHECKOUT_ACCESS, docMap.get("to[" + REL_ACTIVE_VERSION + "].from." + CommonDocument.SELECT_HAS_CHECKOUT_ACCESS));
                newDocMap.put(CommonDocument.SELECT_HAS_CHECKIN_ACCESS, docMap.get("to[" + REL_ACTIVE_VERSION + "].from." + CommonDocument.SELECT_HAS_CHECKIN_ACCESS));
                newDocList.add(newDocMap);
            }
            
            newDocList.addAll((MapList)JPO.invoke(context, "IEFObjectsLockedBy", null, "getListForDesktopClient", initArgs, MapList.class));
        }
        catch(Exception ex)
        {
            throw ex;
        }
        
        return newDocList;
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
    public  MapList getSavedQueries(Context context, String[] args) throws MatrixException
    {
        String lang = args[0];
        MapList queryMapList = new MapList();
        MQLCommand mql = new MQLCommand();
        String result = null;
        try
        {
 
            boolean eval = mql.executeCommand(context, "list $1", "query");
        }
        catch (Exception e)
        {
            // if there was no error from mql
            if (mql.getError().length() != 0)
            {
                throw new MatrixException("IEFDataGenerator:getSavedQueries : ERROR - " + mql.getError());
            }
        }

        result = mql.getResult();
        try
        {
            BufferedReader in = new BufferedReader(new StringReader(mql.getResult()));

            //place the header order to be displayed in the columnHeaderMap
            //this will be used the XML generator to generate the columns in the same order as
            //the table columns
            //create a map of column name and header value
            Map columnHeaderMap = new HashMap();
            String sName = i18nNow.getI18nString("emxComponents.Common.Name",  "emxComponentsStringResource", lang);
            MCADMxUtil _util = new MCADMxUtil(context, null,_GlobalCache);
            if(!_util.isCDMInstalled(context))
                sName = i18nNow.getI18nString("emxComponents.Common.Name",  "IEFDesignCenterStringResource", lang);

            columnHeaderMap.put("Name", sName);
            columnHeaderMap.put("columnHeaderOrder", "Name,");
            queryMapList.add(columnHeaderMap);
            int count = 1;
            while((result = in.readLine()) != null)
            {
                Map queryMap = new HashMap();
                if(! result.startsWith("."))
                {
                    queryMap.put("Name", result);
                    queryMap.put("_objectName", result);
                    //set the symbolicname as "savedQuery" this will be checked
                    //by IEFBuildFolderStructure.fetchContent()
                    queryMap.put("_symbolicName", "savedQuery");
                    //for menu, commands and SavedQuery the superType is always "admin"
                    queryMap.put("_superType", "admin");
                    queryMap.put("_keyname", result + "_@" + count);
                    count++;
                    queryMapList.add(queryMap);
                }
            }
        } 
        catch (Exception ex) 
        {
            String emxExceptionString = (ex.toString()).trim();
            System.out.println(emxExceptionString);

            // set the error string in the Error object
            if ( (emxExceptionString != null) && (emxExceptionString.trim().length() > 0) )
                throw new MatrixException(emxExceptionString);
        } 
 
        return queryMapList;
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
    public  MapList evaluateSavedQueries(Context context, String[] args) throws MatrixException
    {
        MapList queryResultList = new MapList();
        MQLCommand mql = new MQLCommand();
        mql.open(context);
        String result = null;
        String queryName = "";
        try
        {
            HashMap initArgsMap = (HashMap)JPO.unpackArgs(args);
            //In this case the "parentRelName" has the query name
            queryName = (String)initArgsMap.get("parentRelName");

            //Following lines are added to honor the query limit in MSOI general search result.
            int querylimit = getQueryLimitFromProperty(context, queryName);
            boolean eval = false;
            String sCmd = "quote on;evaluate query  '" + queryName + "'";
            if(querylimit > 0)
            	eval = mql.executeCommand(context, "quote on; evaluate query $1 limit $2; quote off",queryName,Integer.toString(querylimit));
            else
            	eval = mql.executeCommand(context, "quote on; evaluate query $1 ; quote off",queryName);
            mql.close(context);
        }
        catch (Exception e)
        {
            // if there was no error from mql
            if (mql.getError().length() != 0)
            {
                throw new MatrixException("IEFUtil:evaluateSavedQueries : ERROR - " + mql.getError());
            }
        }

        result = mql.getResult();
        try
        {
            BufferedReader in = new BufferedReader(new StringReader(mql.getResult()));

            mql.open(context);
            while((result = in.readLine()) != null)
            {
            	boolean eval = mql.executeCommand(context, "print bus $1 select $2 dump $3",result,"id","|");
                result = mql.getResult();
                if(result != null || ! result.equals(""))
                {
                    Map idMap = new HashMap();
                    idMap.put("id", result);
                    queryResultList.add(idMap);
                }

            }
            mql.close(context);
        }
        catch (Exception ex)
        {

            String emxExceptionString = (ex.toString()).trim();
            System.out.println(emxExceptionString);

            // set the error string in the Error object
            if ( (emxExceptionString != null) && (emxExceptionString.trim().length() > 0) )
                throw new MatrixException(emxExceptionString);
        }
        
        return queryResultList;
    }

    private  int getQueryLimitFromProperty(Context context, String queryName) throws Exception
    {
        int queryLimit = -1;
       
        Hashtable argsTable = new Hashtable(1);
        argsTable.put("Query", queryName);
	String queryLimitValue = null;
	String [] args = JPO.packArgs(argsTable); 
	queryLimitValue = (String)JPO.invoke(context, "IEFSearch", initArgs, "getQueryLimit", args, String.class);
	queryLimit =Integer.parseInt(queryLimitValue);
 
        return queryLimit;
    }
    
    public Boolean isTypeOf(Context context, String [] args) throws Exception
    {
        Hashtable argsTable = (Hashtable) JPO.unpackArgs(args);
        String sType        = (String) argsTable.get("Type");
        String sRootType    = (String)argsTable.get("RootType");
        
        return new Boolean(isTypeOf(context, sType, sRootType));
    }

    public  boolean isTypeOf(Context context, String sType, String sRootType)
    throws MatrixException, FrameworkException
    {

		MQLCommand mqlCommand = new MQLCommand();

		boolean bReturnVal = mqlCommand.executeCommand(context, "print $1 $2 select $3 dump $4","type",sRootType,"derivative","|");
		String result = null;
		StringList SubTypesList = null;

		if (bReturnVal)
		{	
			result = mqlCommand.getResult();
			if ((result == null) || (result.equals("")) || !(result.length() > 0))
			{
				System.out.println("[IEFUtil.isTypeOf]: Null MQL command Result ");
			}
			if(result.endsWith("\n"))
        {
				result = result.substring(0, (result.lastIndexOf("\n")));
			}
			SubTypesList = FrameworkUtil.split(result, "|");
        }
		StringTokenizer strTok = new StringTokenizer(sType, ",");
        boolean flag = false;
		if(SubTypesList != null)
        {
			while(strTok.hasMoreElements())
            {
				sType = (String) strTok.nextElement();
				if (sType != null && sType.startsWith("type_"))
                {
					sType = PropertyUtil.getSchemaProperty(context, sType);
            }

				if(sRootType.equals(sType))
            {
					flag = true;
					break;
				}
				int i = SubTypesList.indexOf(sType);
				if(i >= 0)
                {
                    flag = true;
                    break;
                }
            }
		}
        return flag;
    }
    
    public  String getDisplayValue(Context context, String[] args) throws Exception
    {
        Hashtable argumentsTable  = (Hashtable)JPO.unpackArgs(args);
        String adminType          = (String) argumentsTable.get("adminType");
        String languageStr        = (String) argumentsTable.get("language");
        String adminName          = (String) argumentsTable.get("adminName");
        
        return getDisplayValue(adminType, adminName, languageStr);
    }

    public  String getDisplayValue(String admin, String adminName, String languageStr)
    {
        String tempDisplayName = adminName;
        try
        {
            tempDisplayName = i18nNow.getAdminI18NString(admin, adminName, languageStr);
        }catch(Exception me)
        {
            tempDisplayName = adminName;
        }

        return tempDisplayName;
    }
    
    public  IEFXmlNode getCommandPacket(Context context, String[] args) throws Exception
    {
        Hashtable argumentsTable  = (Hashtable)JPO.unpackArgs(args);
        String commandString      = (String) argumentsTable.get("commandString");
        
        return getCommandPacket(commandString);
    }

    public  IEFXmlNode getCommandPacket(String commandString)
    {
        IEFXmlNode commandPacket = null;

        try
        {
            ResourceBundle mcadIntegrationBundle = ResourceBundle.getBundle("ief");
            String charset = mcadIntegrationBundle.getString("mcadIntegration.MCADCharset");
            if(charset == null)
                charset = "UTF8";

            int i = commandString.indexOf("?xml");

            if(i < 0)
                commandString = cleanPacketContent("<?xml version='1.0'?>" + commandString);
            else
                commandString = cleanPacketContent(commandString);

            commandPacket = MCADXMLUtils.parse(commandString, charset);
        }
        catch(Exception e)
        {
            commandPacket = null;
        }

        return commandPacket;
    }

    public  String cleanPacketContent(String inputContent)
    {
        String outputContent = inputContent;

        int ltIndex = inputContent.lastIndexOf('<');
        int gtIndex = inputContent.lastIndexOf('>');

        if(ltIndex>=0 && gtIndex>=0 && gtIndex>ltIndex)
        {
            String endTag = inputContent.substring(ltIndex+1, gtIndex).trim();
            if(endTag.startsWith("/") || endTag.endsWith("/"))
            {
                outputContent = inputContent.substring(0, gtIndex + 1);
            }
        }

        return outputContent;
    }
    
    public  HashSet getAttributeSetToFilter(Context context, String[] args)
    {
        return getAttributeSetToFilter(context);
    }

    public  HashSet getAttributeSetToFilter(Context context)
    {
        HashSet notAllowedAttributes = new HashSet();

        notAllowedAttributes.add(PropertyUtil.getSchemaProperty(context, "attribute_CADType"));
        notAllowedAttributes.add(PropertyUtil.getSchemaProperty(context, "attribute_IEF-EBOMSync-PartTypeAttribute"));
        notAllowedAttributes.add(PropertyUtil.getSchemaProperty(context, "attribute_MCADInteg-Comment"));
        notAllowedAttributes.add(PropertyUtil.getSchemaProperty(context, "attribute_IEF-FileMessageDigest"));
        notAllowedAttributes.add(PropertyUtil.getSchemaProperty(context, "attribute_MCADLabel"));
        notAllowedAttributes.add(PropertyUtil.getSchemaProperty(context, "attribute_ModelType"));
        notAllowedAttributes.add(PropertyUtil.getSchemaProperty(context, "attribute_ModifiedinMatrix"));
        notAllowedAttributes.add(PropertyUtil.getSchemaProperty(context, "attribute_NewlyCreatedinMatrix"));
        notAllowedAttributes.add(PropertyUtil.getSchemaProperty(context, "attribute_RenamedFrom"));
        notAllowedAttributes.add(PropertyUtil.getSchemaProperty(context, "attribute_Source"));        
        notAllowedAttributes.add("originated");

        return notAllowedAttributes;
    }

    public  HashMap getOperatorvalues(Context context, String[] args) throws Exception
    {
        HashMap argumentsTable   = (HashMap)JPO.unpackArgs(args);
        String sOperator         = (String) argumentsTable.get("Operator");
        String languageStr       = (String) argumentsTable.get("language");
        
        return getOperatorvalues(sOperator, languageStr);
    }

    public  HashMap getOperatorvalues(String sOperator, String languageStr)
    {
        HashMap operatorMap = new HashMap();
        String sOperatorvalue = null;
        if(sOperator != null)
        {
            //Initialize the values which will be shown in the drop down of attribute's operator
            initOperatorValues(languageStr);
            if("string".equalsIgnoreCase(sOperator))
            {
                sOperatorvalue = selectStringDataType.toString();
                operatorMap.put("operator", "STRING");
                operatorMap.put("operatorvalues", sOperatorvalue);
            }else if("integer".equalsIgnoreCase(sOperator))
            {
                sOperatorvalue = selectNumericDataType.toString();
                operatorMap.put("operator", "INTEGER");
                operatorMap.put("operatorvalues", sOperatorvalue);
            }else if("real".equalsIgnoreCase(sOperator))
            {
                sOperatorvalue = selectNumericDataType.toString();
                operatorMap.put("operator", "REAL");
                operatorMap.put("operatorvalues", sOperatorvalue);
            }else if("timestamp".equalsIgnoreCase(sOperator))
            {
                sOperatorvalue = selectTimeDataType.toString();
                operatorMap.put("operator", "DATE");
                operatorMap.put("operatorvalues", sOperatorvalue);
            }else if("boolean".equalsIgnoreCase(sOperator))
            {
                sOperatorvalue = selectBooleanDataType.toString();
                operatorMap.put("operator", "BOOLEAN");
                operatorMap.put("operatorvalues", sOperatorvalue);
            }else if("binary".equalsIgnoreCase(sOperator))                                           //L86 : 		IR-542275-3DEXPERIENCER2015x
            {
            	sOperatorvalue = selectBinaryDataType.toString();
            	operatorMap.put("operator", "BINARY");
            	operatorMap.put("operatorvalues", sOperatorvalue);
            }else                                                                                                                  //L86 : 		IR-542275-3DEXPERIENCER2015x
	    { 
		sOperatorvalue = selectOtherDataTypeGeneric.toString();
		operatorMap.put("operator" , sOperator);
		operatorMap.put("operatorvalues", sOperatorvalue);				
            }
        }

        return operatorMap;
    }

    public  String getSelectedValues(Context context, String[] args) throws Exception
    {
        Hashtable argumentsTable  = (Hashtable)JPO.unpackArgs(args);
        AttributeType attrTypeObj = (AttributeType) argumentsTable.get("AttributeType");
        String languageStr        = (String) argumentsTable.get("language");
        
        return getSelectedValues(context, attrTypeObj, languageStr);
    }
    
    public  String getSelectedValues(Context context, AttributeType attrTypeObj, String languageStr)
    {
        StringList choices = new StringList();
        String sSelectedValue = "";
        try
        {
            choices = attrTypeObj.getChoices(context);
            StringItr stringListItr = new StringItr(choices);
            StringBuffer tempBuffer = new StringBuffer("|");
            while(stringListItr.next())
            {
                String sChoiceVal = (String)stringListItr.obj();
                tempBuffer.append(sChoiceVal);
                tempBuffer.append(";");
                tempBuffer.append(getMXI18NString(sChoiceVal,attrTypeObj.getName(), languageStr,"Range"));
                tempBuffer.append("|");
            }
            sSelectedValue = tempBuffer.toString();
            if(sSelectedValue.endsWith("|"))
                sSelectedValue = sSelectedValue.substring(0, sSelectedValue.length()-1);
        }
        catch(Exception ex)
        {
            System.out.println("[IEFSearch.getSelectedValues] EXCEPTION : " + ex.getMessage());
        }

        return sSelectedValue;
    }

    //
    // Get the I18N Translated String for the given String, passing in the prefix
    // Either: Attribute, Basic or Range
    // Return passed in String if property file not found
    //
    public  String getMXI18NString(String preString,String postString, String languageStr,String prefix) throws MatrixException
    {
        com.matrixone.apps.domain.util.i18nNow loc = new com.matrixone.apps.domain.util.i18nNow();
        String text = "";
        if (!"".equals(postString)){
            text = "emxFramework." + prefix + "." + postString.replace(' ','_') + "." +  preString.replace(' ','_');
        }else{
            text = "emxFramework." + prefix + "." + preString.replace(' ','_');
        }
        String returnString = preString;
        try{
            String I18NreturnString = (String) loc.GetString("emxFrameworkStringResource ", languageStr, text);
            if ((!"".equals(I18NreturnString)) && (I18NreturnString != null)){
                returnString = I18NreturnString;
            }
        }
        catch(Exception e)
        {
            //Do Nothing Value Already Set
            //String must not have been in Property File or another Exception
            System.out.println("[IEFSearch.getAttributeList] EXCEPTION : " + text + " not found in emxFrameworkStringResource");
        }

        return returnString;
    }

    public  void initOperatorValues(String languageStr)
    {
        selectStateType         = new StringBuffer("");
        selectStringDataType    = new StringBuffer("");
        selectNumericDataType   = new StringBuffer("");
        selectTimeDataType      = new StringBuffer("");
        selectBooleanDataType   = new StringBuffer("");

        try
        {
            // English string operators - Start
            String sMatrixIncludes     = i18nNow.getI18nString("emxFramework.AdvancedSearch.Includes",  "emxFrameworkStringResource", "en");
            String sMatrixIsExactly    = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsExactly",  "emxFrameworkStringResource", "en");
            String sMatrixIsNot        = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsNot",  "emxFrameworkStringResource", "en");
            String sMatrixMatches      = i18nNow.getI18nString("emxFramework.AdvancedSearch.Matches",  "emxFrameworkStringResource", "en");
            String sMatrixBeginsWith   = i18nNow.getI18nString("emxFramework.AdvancedSearch.BeginsWith",  "emxFrameworkStringResource", "en");
            String sMatrixEndsWith     = i18nNow.getI18nString("emxFramework.AdvancedSearch.EndsWith",  "emxFrameworkStringResource", "en");
            String sMatrixEquals       = i18nNow.getI18nString("emxFramework.AdvancedSearch.Equals",  "emxFrameworkStringResource", "en");
            String sMatrixDoesNotEqual = i18nNow.getI18nString("emxFramework.AdvancedSearch.DoesNotEqual",  "emxFrameworkStringResource", "en");
            String sMatrixIsBetween    = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsBetween",  "emxFrameworkStringResource", "en");
            String sMatrixIsAtMost     = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsAtMost",  "emxFrameworkStringResource", "en");
            String sMatrixIsAtLeast    = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsAtLeast",  "emxFrameworkStringResource", "en");
            String sMatrixIsMoreThan   = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsMoreThan",  "emxFrameworkStringResource", "en");
            String sMatrixIsLessThan   = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsLessThan",  "emxFrameworkStringResource", "en");
            String sMatrixIsOn         = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsOn",  "emxFrameworkStringResource", "en");
            String sMatrixIsOnOrBefore = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsOnOrBefore",  "emxFrameworkStringResource", "en");
            String sMatrixIsOnOrAfter  = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsOnOrAfter",  "emxFrameworkStringResource", "en");
            // English string operators - End

            // Translated string operators - Start
            String sMatrixIncludesTrans     = i18nNow.getI18nString("emxFramework.AdvancedSearch.Includes",  "emxFrameworkStringResource", languageStr);
            String sMatrixIsExactlyTrans    = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsExactly",  "emxFrameworkStringResource", languageStr);
            String sMatrixIsNotTrans        = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsNot",  "emxFrameworkStringResource", languageStr);
            String sMatrixMatchesTrans      = i18nNow.getI18nString("emxFramework.AdvancedSearch.Matches",  "emxFrameworkStringResource", languageStr);
            String sMatrixBeginsWithTrans   = i18nNow.getI18nString("emxFramework.AdvancedSearch.BeginsWith",  "emxFrameworkStringResource", languageStr);
            String sMatrixEndsWithTrans     = i18nNow.getI18nString("emxFramework.AdvancedSearch.EndsWith",  "emxFrameworkStringResource", languageStr);
            String sMatrixEqualsTrans       = i18nNow.getI18nString("emxFramework.AdvancedSearch.Equals",  "emxFrameworkStringResource", languageStr);
            String sMatrixDoesNotEqualTrans = i18nNow.getI18nString("emxFramework.AdvancedSearch.DoesNotEqual",  "emxFrameworkStringResource", languageStr);
            String sMatrixIsBetweenTrans    = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsBetween",  "emxFrameworkStringResource", languageStr);
            String sMatrixIsAtMostTrans     = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsAtMost",  "emxFrameworkStringResource", languageStr);
            String sMatrixIsAtLeastTrans    = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsAtLeast",  "emxFrameworkStringResource", languageStr);
            String sMatrixIsMoreThanTrans   = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsMoreThan",  "emxFrameworkStringResource", languageStr);
            String sMatrixIsLessThanTrans   = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsLessThan",  "emxFrameworkStringResource", languageStr);
            String sMatrixIsOnTrans         = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsOn",  "emxFrameworkStringResource", languageStr);
            String sMatrixIsOnOrBeforeTrans = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsOnOrBefore",  "emxFrameworkStringResource", languageStr);
            String sMatrixIsOnOrAfterTrans  = i18nNow.getI18nString("emxFramework.AdvancedSearch.IsOnOrAfter",  "emxFrameworkStringResource", languageStr);
            // Translated string operators - Start

            //Set select options for States
            selectStateType.append("|");
            selectStateType.append(sMatrixEquals + ";" + sMatrixEqualsTrans + "|");
            selectStateType.append(sMatrixIsNot + ";" + sMatrixIsNotTrans);

            //Set select options for string data type
            selectStringDataType.append("|");
            selectStringDataType.append(sMatrixBeginsWith + ";" + sMatrixBeginsWithTrans + "|");
            selectStringDataType.append(sMatrixEndsWith  + ";" + sMatrixEndsWithTrans + "|");
            selectStringDataType.append(sMatrixIncludes + ";" + sMatrixIncludesTrans + "|");
            selectStringDataType.append(sMatrixIsExactly + ";" + sMatrixIsExactlyTrans + "|");
            selectStringDataType.append(sMatrixIsNot + ";" + sMatrixIsNotTrans + "|");
            selectStringDataType.append(sMatrixMatches + ";" + sMatrixMatchesTrans);

            //Set select options for numeric data type
            selectNumericDataType.append("|");
            selectNumericDataType.append(sMatrixIsAtLeast + ";" + sMatrixIsAtLeastTrans + "|");
            selectNumericDataType.append(sMatrixIsAtMost + ";" + sMatrixIsAtMostTrans + "|");
            selectNumericDataType.append(sMatrixDoesNotEqual + ";" + sMatrixDoesNotEqualTrans + "|");
            selectNumericDataType.append(sMatrixEquals + ";" + sMatrixEqualsTrans + "|");
            selectNumericDataType.append(sMatrixIsBetween + ";" + sMatrixIsBetweenTrans + "|");
            selectNumericDataType.append(sMatrixIsLessThan + ";" + sMatrixIsLessThanTrans + "|");
            selectNumericDataType.append(sMatrixIsMoreThan + ";" + sMatrixIsMoreThanTrans);

            //Set select options for date/time data type
            selectTimeDataType.append("|");
            selectTimeDataType.append(sMatrixIsOn + ";" + sMatrixIsOnTrans + "|");
            selectTimeDataType.append(sMatrixIsOnOrBefore + ";" + sMatrixIsOnOrBeforeTrans + "|");
            selectTimeDataType.append(sMatrixIsOnOrAfter + ";" + sMatrixIsOnOrAfterTrans);

            //Set select options for boolean data type
            selectBooleanDataType.append("|");
            selectBooleanDataType.append(sMatrixIsExactly + ";" + sMatrixIsExactlyTrans + "|");
            selectBooleanDataType.append(sMatrixIsNot + ";" + sMatrixIsNotTrans);
			
			 //Set select options for binary data type                   //L86 : 		IR-542275-3DEXPERIENCER2015x
            selectBinaryDataType.append("|");
            selectBinaryDataType.append(sMatrixIsExactly + ";" + sMatrixIsExactlyTrans + "|");
			
			//Set select options for any other generic data type     //L86 : 		IR-542275-3DEXPERIENCER2015x
			selectOtherDataTypeGeneric.append("|");
			selectOtherDataTypeGeneric.append(sMatrixIsExactly + ";" + sMatrixIsExactlyTrans + "|");
			
        }
        catch(Exception ex)
        {
            System.out.println("[IEFSearch.initOperatorValues] EXCEPTION : " + ex.getMessage());
        }
    }


    public  BusinessObject iefGetCDMMajorObject(Context context, String args[]) throws Exception
    {
		MCADMxUtil util = new MCADMxUtil(context, null,_GlobalCache);
		Hashtable argsTable = (Hashtable) JPO.unpackArgs(args);
                BusinessObject bus  = (BusinessObject) argsTable.get("bus");
		return iefGetCDMMajorObject(context,bus,util);
    }
    /**
     * get majorobj. from input minor object
     */
    public  BusinessObject iefGetCDMMajorObject(Context context, BusinessObject bus, MCADMxUtil util) throws Exception
    {
        BusinessObject majBusObj = null;
        try
        {
            if(util.isVersionable(context, bus.getObjectId(context)))
            {
                //String sRelName = "Active Version";
                String sRelName = MCADMxUtil.getActualNameForAEFData(context,"relationship_ActiveVersion");
                BusinessObjectList list = util.getRelatedBusinessObjects(context,bus,sRelName,"to");
                if(list != null && list.size() > 0)
                {
                    BusinessObjectItr itr = new BusinessObjectItr(list);
                    while(itr.next())
                    {
                        majBusObj = itr.obj();
                    }
                }
                else
                {
                    //Check whether the object is connected to major with the relationship "Latest Version".
                    //This is the case in which the data is either migrated and any intermediate
                    //version is finalized before migration. The two relationships are pointing to
                    //two different minors
                    //sRelName = "Latest Version";
                    sRelName = MCADMxUtil.getActualNameForAEFData(context,"relationship_LatestVersion");
                    list = util.getRelatedBusinessObjects(context,bus,sRelName,"to");
                    if(list != null && list.size() > 0)
                    {
                        BusinessObjectItr itr = new BusinessObjectItr(list);
                        while(itr.next())
                        {
                            majBusObj = itr.obj();
                        }
                    }
                    else
                    {
                        majBusObj = bus;
                    }
                }
            }
            else
            {
                majBusObj = bus;
            }
        }
        catch(Exception me)
        {
            throw me;
        }
        return majBusObj;
    }

    /**
     * get minorobj. from input minor object
     */
    public  BusinessObject iefGetCDMMinorObject(Context context, String[] args) throws Exception
    {
        Hashtable argsTable = (Hashtable) JPO.unpackArgs(args);
        BusinessObject bus  = (BusinessObject) argsTable.get("bus");
        return iefGetCDMMinorObject(context, bus);
    }
    
    public  BusinessObject iefGetCDMMinorObject(Context context, BusinessObject bus) throws Exception
    {
        MCADMxUtil util = new MCADMxUtil(context, null,_GlobalCache);
        
        BusinessObject minorBusObj = null;
        try
        {
            String sRelName = MCADMxUtil.getActualNameForAEFData(context,"relationship_ActiveVersion");
            BusinessObjectList list = util.getRelatedBusinessObjects(context,bus,sRelName,"from");
            if(list != null && list.size() > 0)
            {
                BusinessObjectItr itr = new BusinessObjectItr(list);
                while(itr.next())
                {
                    minorBusObj = itr.obj();
                }
            }
            else
            {
                minorBusObj = bus;
            }
        }
        catch(Exception me)
        {
            throw me;
        }
        return minorBusObj;
    }

    /**
     * Generic function that the Applet calls for getting actions menu details.
     * returns an xml response after traversing the object
     *
     * @param args input details as provided by the Applet
     * @throws MatrixException if the operation fails
     */

    public  String iefGetActionCommandDetails(Context context, String[] args) throws MatrixException
    {
        String lang = args[0];
        if(lang == null)
            lang = "en";

        IEFXmlNodeImpl actionMenuNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
        actionMenuNode.setName("actionmenudetails");

        IEFXmlNodeImpl menuListNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
        menuListNode.setName("menulist");
        createActionsCommandPacket(context, lang, menuListNode, "menu_Actions", "", "true");

        actionMenuNode.addNode(menuListNode);

        return actionMenuNode.getXmlString();
    }

    private  void createActionsCommandPacket(Context context, String lang, IEFXmlNodeImpl menuListNode, String menuName, String parent, String haschildren) throws MatrixException
    {
	    String actualCommandName = PropertyUtil.getSchemaProperty(context, menuName);
	    if(actualCommandName != null)
        {
            //get the user assignments
            Vector userRoleList = PersonUtil.getAssignments(context);

            try
            {
                MapList menuMapList = new MapList();
                menuMapList = UIMenu.getMenu(context, actualCommandName, userRoleList);

                if ( menuMapList != null)
                {    
                    Iterator menuItr = menuMapList.iterator();
                    while (menuItr.hasNext())
                    {
                        HashMap componentMap = (HashMap)menuItr.next();

                        // Get component details
                        String componentName = UIMenu.getName(componentMap);
                        String componentLabel = UIMenu.getLabel(componentMap);
                        String componentAlt = UIMenu.getAlt(componentMap);
                        String componentHRef = UIMenu.getHRef(componentMap);
                        String sRegisteredSuite = UIMenu.getSetting(componentMap, MCADMxUtil.getActualNameForAEFData(context, "attribute_RegisteredSuite"));
                        String sWindowHeight = UIMenu.getSetting(componentMap, "Window Height");
                        String sWindowWidth = UIMenu.getSetting(componentMap, "Window Width");
                        String sActionType = UIMenu.getSetting(componentMap, "Action Type");
                        if (sActionType.equalsIgnoreCase("Separator"))
                        {
                            continue;
                        }
                        if(sWindowHeight == null || sWindowHeight.length() == 0)
                        {
                            sWindowHeight = "600";
                        }

                        if(sWindowWidth == null || sWindowWidth.length() == 0)
                        {
                            sWindowWidth = "700";
                        }

                        if(componentLabel != null && componentLabel.length() > 0 && (componentLabel.equalsIgnoreCase("emxFramework.Suites.Display.IEF") || componentLabel.equalsIgnoreCase("emxFramework.Suites.Display.Integration") || componentLabel.equalsIgnoreCase("emxFramework.Suites.Display.DesignerCentral")))
                        {
                            //do not show the Integrations or Designer link
                            continue;
                        }

                        // Get the directory and resourceFileId for the Registered Suite from
                        // the system.properties file
                        String menuRegisteredDir = "";
                        String stringResFileId = "";

                        if ( (sRegisteredSuite != null) && (sRegisteredSuite.trim().length() > 0 ) )
                        {
                            menuRegisteredDir = UINavigatorUtil.getRegisteredDirectory(sRegisteredSuite);
                            stringResFileId = UINavigatorUtil.getStringResourceFileId(sRegisteredSuite);
                        }

                        // Get the sAltText with the internationalization string
                        String sCommandLabelEn = UINavigatorUtil.getI18nString(componentLabel, stringResFileId, "en");
                        String sCommandLabel = UINavigatorUtil.getI18nString(componentLabel, stringResFileId, lang);
                        String uiComponentType = null;

                        IEFXmlNodeImpl menuNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
                        menuNode.setName("menu");
                        Hashtable menuAttributes = new Hashtable();
                        menuAttributes.put("name", sCommandLabelEn);
                        menuAttributes.put("displayvalue", sCommandLabel);
                        menuAttributes.put("parentmenu", parent);

                        if(UIMenu.isCommand(componentMap))
                        {
                            uiComponentType = "Command";
                            haschildren = "false";
                            menuAttributes.put("haschildren", haschildren);
                            menuAttributes.put("url", getFormattedComponentHRef(context, componentHRef, menuRegisteredDir, sRegisteredSuite, stringResFileId, sWindowHeight, sWindowWidth));

                        }
                        else if(UIMenu.isMenu(componentMap))
                        {
                            uiComponentType = "Menu";
                            haschildren = "true";
                            menuAttributes.put("haschildren", haschildren);
                            menuAttributes.put("url", "");
                        }
                        menuNode.setAttributes(menuAttributes);
                        menuListNode.addNode(menuNode);

                        String uiComponentSymbolicName = FrameworkUtil.getAliasForAdmin(context,
                                uiComponentType,
                                componentName,
                                true);

                        if(writer != null)
                        {
                            writer.write("uiComponentType :  " + uiComponentType + "\n");
                            writer.write("Name :  " + componentName + "\n");
                            writer.write("uiComponentSymbolicName :  " + uiComponentSymbolicName + "\n");
                        }
                        //recursive call
                        if("false".equals(haschildren))
                            createActionsCommandPacket(context, lang, menuListNode, uiComponentSymbolicName, parent, haschildren);
                        else
                            createActionsCommandPacket(context, lang, menuNode, uiComponentSymbolicName, sCommandLabelEn, haschildren);
                    }
                }
            }
            catch (Exception ex)
            {
                if(writer != null)
                {
                    try
                    {
                        writer.write("[IEFDataGenerator:iefGetActionCommandDetails] EXCEPTION : " + ex.toString() + "\n");
                    }
                    catch (Exception we)
                    {
                        //do nothing
                    }
                }
            }
        }
    }

    private  String getFormattedComponentHRef(Context context, String componentHRef, String menuRegisteredDir, String sRegisteredSuite, String stringResFileId, String sWindowHeight, String sWindowWidth) throws Exception
    {
        String formattedHref = "";
        String sMacro = "";
        int startIndex = componentHRef.indexOf("${");
        if(startIndex >= 0)
        {
	    sMacro = "SUITE_DIR";
            componentHRef = componentHRef.substring(startIndex+2);
        } else if(componentHRef.startsWith("SUITE_DIR"))
        {
            sMacro = "SUITE_DIR";
            componentHRef = componentHRef.substring(sMacro.length());
        } else if(componentHRef.startsWith("ROOT_DIR"))
        {
            sMacro = "ROOT_DIR";
            componentHRef = componentHRef.substring(sMacro.length());
        } else if(componentHRef.startsWith("COMMON_DIR"))
        {
            sMacro = "COMMON_DIR";
            componentHRef = componentHRef.substring(sMacro.length());
        } else if(componentHRef.startsWith("COMPONENT_DIR"))
        {
            sMacro = "COMPONENT_DIR";
            componentHRef = componentHRef.substring(sMacro.length());
        }
	else if(!componentHRef.startsWith("../"))
	{
	    sMacro = "COMMON_DIR";
	    componentHRef = "/"+componentHRef;
	}
		
        int endIndex = componentHRef.indexOf("}");
        if(endIndex > 0)
        {
            sMacro = componentHRef.substring(0, endIndex);
            componentHRef = componentHRef.substring(endIndex+1);
        }

        if("SUITE_DIR".equalsIgnoreCase(sMacro))
        {
            //get SUITE_DIR defined in emxSystem.properties
            String suiteDIR = FrameworkProperties.getProperty("eServiceSuite" + sRegisteredSuite + ".Directory");
            sMacro = suiteDIR;
        }
        else if("ROOT_DIR".equalsIgnoreCase(sMacro))
        {
            //get ROOT_DIR defined in emxSystem.properties
            String rootDIR = FrameworkProperties.getProperty("eServiceSuiteFramework.RootDirectory");
            sMacro = rootDIR;
        }
        else if("COMMON_DIR".equalsIgnoreCase(sMacro))
        {
            //get COMMON_DIR defined in emxSystem.properties
            String commonDIR = FrameworkProperties.getProperty("eServiceSuiteFramework.CommonDirectory");
            sMacro = commonDIR;
        }
        else if("COMPONENT_DIR".equalsIgnoreCase(sMacro))
        {
            //get COMPONENT_DIR defined in emxSystem.properties
            String componentDIR = FrameworkProperties.getProperty("eServiceSuiteFramework.ComponentDirectory");
            sMacro = componentDIR;
        }

        if(sMacro != null && sMacro.length() != 0 && ! sMacro.equalsIgnoreCase(".."))
            componentHRef = sMacro + componentHRef ;

        //Prepare an action url string
        if(componentHRef != null && componentHRef.indexOf("?") >= 0)
        {
            componentHRef = componentHRef + "&suiteKey="+ sRegisteredSuite + "&StringResourceFileId=" + stringResFileId + "&SuiteDirectory=" +  menuRegisteredDir;
        }
        else if(componentHRef != null && componentHRef.indexOf("?") < 0)
        {
            componentHRef = componentHRef + "?suiteKey="+ sRegisteredSuite + "&StringResourceFileId=" + stringResFileId + "&SuiteDirectory=" +  menuRegisteredDir;
        }

        if(componentHRef != null && componentHRef.length() > 0)
        {
            if(!componentHRef.startsWith("../"))
	       componentHRef = "../"+ componentHRef;
            formattedHref = componentHRef + "&windowheight=" + sWindowHeight + "&windowwidth=" + sWindowWidth;
        }
        return formattedHref;
    }
    public int mxMain(Context context, String[] args) throws Exception
    {
        writer = new BufferedWriter(new MatrixWriter(context));

        if(writer != null)
        {
            String str = iefGetActionCommandDetails(context, args);
            writer.write("==================================\n");
            writer.write("Please check the file 'C:/Documents and Settings/shashikantk/My Documents/menuDetails.xml'\n");
            writer.write("==================================\n");
            writer.flush();
            writer.close();

            FileWriter log  = new FileWriter("C:/Documents and Settings/shashikantk/My Documents/menuDetails.xml", true);
            log.write(str);
            log.flush();
            log.close();
        }
        return 0;
    }
}

