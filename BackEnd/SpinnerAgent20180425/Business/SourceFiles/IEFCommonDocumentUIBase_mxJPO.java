/*
 *  IEFCommonDocumentUIBase.java
 *
 * Copyright (c) 1992-2002 MatrixOne, Inc.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 * static const char RCSID[] = $Id: /AppInstall/Programs/schema/MCAD_Server/IEFCommonDocumentUIBase.java 1.1.1.1.1.3 Sat Dec 13 12:21:37 2008 GMT ds-hbhatia Experimental$
 */
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.BusinessType;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainSymbolicConstants;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;

/**
 * The <code>emxDocumentUtilBase</code> class contains utility methods for
 * getting data using configurable table APPDocumentSummary
 *
 * @version Common 10-5 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class IEFCommonDocumentUIBase_mxJPO
{


    static public Map typeMapping = new HashMap();
    private Hashtable initHashtable                         = null;
    private HashMap gcoTable                                = null;
	
   /**
     * Constructor
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since Common 10.5
     * @grade 0
     */
    public IEFCommonDocumentUIBase_mxJPO (Context context, String[] args)
        throws Exception
    {
		if(args.length > 1)
		{
			initHashtable           = (Hashtable) JPO.unpackArgs(args);
			gcoTable                = (HashMap) initHashtable.get("gcoTable"); 
		}

    }

    /**
     * This method is executed if a specific method is not specified.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @returns nothing
     * @throws Exception if the operation fails
     * @since Common 10.5
     * @grade 0
     */
    public int mxMain(Context context, String[] args)
        throws Exception
    {
        if (!context.isConnected())
            throw new Exception("not supported on desktop client");
        return 0;
    }

    /**
     * gets the list of connected DOCUMENTS to the master Object
     * Used for APPDocumentSummary table
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectId - parent object OID
     * @returns Object
     * @throws Exception if the operation fails
     * @since Common 10.5
     * @grade 0
     */
    public Object getDocuments(Context context, String[] args)
        throws Exception
    {
        try
        {
            HashMap programMap        = (HashMap) JPO.unpackArgs(args);
            String  parentId          = (String) programMap.get("objectId");
            String  parentRel         = (String) programMap.get("parentRelName");
            Pattern relPattern        = new Pattern("");

            // If parent relation ship is passed separated by comma
            // Tokenize and add it rel pattern

            if(parentRel != null)
            {
                StringTokenizer relString = new StringTokenizer(parentRel,",");
                while (relString.hasMoreTokens())
                {
                    String relStr = relString.nextToken().trim();
                    if(relStr != null && !"null".equals(relStr) && !"".equals(relStr))
                    {
                        String actRelName = PropertyUtil.getSchemaProperty(context, relStr);
                        if(actRelName != null && !"null".equals(actRelName) && !"".equals(actRelName))
                        {
                           relPattern.addPattern(actRelName);
                        }
                    }
                }
            }

            // if not passed, or non-existing relationship passed then default to "Reference Document" relationship
            if("".equals(relPattern.getPattern()))
            {
                relPattern.addPattern(PropertyUtil.getSchemaProperty(context, DomainSymbolicConstants.SYMBOLIC_relationship_ReferenceDocument));
            }

            String objectWhere = "";//CommonDocument.SELECT_IS_VERSION_OBJECT + "==\"False\"";

            DomainObject masterObject = DomainObject.newInstance(context, parentId);

            StringList typeSelects = new StringList();
            typeSelects.add(DomainConstants.SELECT_ID);
            typeSelects.add(DomainConstants.SELECT_TYPE);
            typeSelects.add(DomainConstants.SELECT_FILE_NAME);
			typeSelects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
			typeSelects.add(CommonDocument.SELECT_TITLE);
            typeSelects.add(DomainConstants.SELECT_REVISION);
            typeSelects.add(DomainConstants.SELECT_NAME);
			typeSelects.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION);
			typeSelects.add(CommonDocument.SELECT_HAS_ROUTE);
			typeSelects.add(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
			typeSelects.add(CommonDocument.SELECT_SUSPEND_VERSIONING);
			typeSelects.add(CommonDocument.SELECT_HAS_CHECKOUT_ACCESS);
			typeSelects.add(CommonDocument.SELECT_HAS_CHECKIN_ACCESS);
			typeSelects.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);

            StringList relSelects = new StringList();
            relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
            MapList documentList = masterObject.getRelatedObjects(context,
                                                          relPattern.getPattern(),
                                                          "*",
                                                          typeSelects,
                                                          relSelects,
                                                          false,
                                                          true,
                                                          (short)1,
                                                          objectWhere,
                                                          null,
                                                          null,
                                                          null,
                                                          null);

            return documentList;
        }
        catch (Exception ex)
        {
            //System.out.println("Error in getDocuments = " + ex.getMessage());
            //ex.printStackTrace();
            throw ex;
        }
    }

   /**
    *  Get Maplist containing Revisions Info for Id passed In
    *  Used for Revision Summary Page in APPDocumentSummary table
    *  revision column
    *
    *  @param context the eMatrix <code>Context</code> object
    *  @param args an array of String arguments for this method
    *  @return MapList containing Revisions Info
    *  @throws Exception if the operation fails
    *
    * @since Common 10.5
    */
    public static MapList getRevisions(Context context, String[] args)
        throws Exception
    {

        HashMap map = (HashMap) JPO.unpackArgs(args);

        String       objectId = (String) map.get("objectId");
        DomainObject busObj   = DomainObject.newInstance(context, objectId);

        StringList busSelects = new StringList(1);
        busSelects.add(busObj.SELECT_ID);

        // for the Id passed, get revisions Info
        MapList revisionsList = busObj.getRevisionsInfo(context,busSelects,
                                                          new StringList(0));

        return revisionsList;
    }

   /**
    *  This method is to be called from a UI component Table. The method
    *  suppress the revision value for the type type_ProjectVault.
    *  The type type_ProjectVault uses a auto generated value and users don't
    *  want to see the value in the UI interface.  The method takes the
    *  objectList from the UI table and parses through the revision values.
    *  Objects of type type_ProjectVault have the revision value replaced with
    *  a blank value.  For all other types, the revision values is returned.
    *
    *  @param context the eMatrix <code>Context</code> object
    *  @param args an array of String arguments for this method
    *  @return Vector object that contains a vector of revision values
    *          for the column.
    *  @throws Exception if the operation fails
    *
    *  @since Common 10.5
    *  @grade 0
    */
    public Object getRevisionLevel ( Context context, String[] args )
          throws Exception
    {

        // unpack and get parameter
        HashMap programmap  = (HashMap)JPO.unpackArgs(args);
        MapList objectList = (MapList)programmap.get("objectList");

        Vector revisionVector = new Vector(objectList.size());
        Map objectMap = null;

        // loop through objects that are in the UI table.  populate Vector
        // with the appropriate revision value.
        for ( int i = 0; i < objectList.size(); i++ )
        {

           objectMap       = (Map) objectList.get(i);
           String objectId = (String)objectMap.get(DomainConstants.SELECT_ID);

           // get object type, revision
           DomainObject domainObject = DomainObject.newInstance(context, objectId );

           StringList busSelects = new StringList(1);
           busSelects.add(DomainConstants.SELECT_ID);
           busSelects.add(DomainConstants.SELECT_TYPE);
           busSelects.add(DomainConstants.SELECT_REVISION);

           Map objectDataMap = domainObject.getInfo(context,busSelects);

           String typeName = (String)objectDataMap.get (DomainConstants.SELECT_TYPE);


           // initialize to be blank, will be returned for type_ProjectVault
           // objects.
           String revLevel = "";

           // if the type isn't a type_ProjectVault, set gather revision level.
           if ( ! typeName.equals (
                  PropertyUtil.getSchemaProperty (context, "type_ProjectVault" ) ) )
           {
             revLevel = (String)objectDataMap.get (DomainConstants.SELECT_REVISION);
           }

           // set a revision level for the object.
           revisionVector.add(revLevel);
        }

        return revisionVector;
    }

    protected static String removeSpace(String formatStr) {

       int flag = 1;
       int strLength = 0;
       int index = 0;

       while  (flag != 0)  {
         strLength = formatStr.length();
         index = 0;
         index = formatStr.indexOf(' ');
         if (index == -1) {
           flag = 0;
           break;
         }

         String tempStr1 = formatStr.substring(0,index);
         String tempStr2 = formatStr.substring(index+1,strLength);
         formatStr = tempStr1 + tempStr2;
       }
       return formatStr;
    }
    /**
     * getNameOrTitle - Will get the Name/Title for Content Summary Table
     *       Will be called in the Name Column
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object of type Vector
     * @throws Exception if the operation fails
     * @since V10 Patch1
     */

    public Vector getNameOrTitle(Context context, String[] args)
      throws Exception
    {
        Vector vName = new Vector();
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            MapList objectList = (MapList)programMap.get("objectList");
            Map paramList = (Map)programMap.get("paramList");
            Map objectMap = null;
            boolean isprinterFriendly = false;
            if(paramList.get("reportFormat") != null)
            {
               isprinterFriendly = true;
            }

            String jsTreeID = (String)paramList.get("jsTreeID");
            int objectListSize = 0 ;
            String name = "";
            String title = "";
            String nameOrTitle = "";
            String objectType = "";
            String parentType = "";
            if(objectList != null)
            {
                objectListSize = objectList.size();
            }
            StringBuffer sBuff= new StringBuffer(256);
            StringBuffer sbNextURL= new StringBuffer(128);
            String objectId = "";
            String objectIcon = "";

            for(int i = 0 ; i < objectListSize  ; i++)
            {
                sBuff= new StringBuffer(256);
                sbNextURL= new StringBuffer(128);
                objectMap = (Hashtable)objectList.get(i);
                objectId = (String) objectMap.get(DomainConstants.SELECT_ID);
                name = (String) objectMap.get(DomainConstants.SELECT_NAME);
                title = (String) objectMap.get(CommonDocument.SELECT_TITLE);
                objectType = (String) objectMap.get(DomainConstants.SELECT_TYPE);
                objectIcon = "../common/images/iconSmall"+removeSpace(objectType)+".gif";
                //parentType = CommonDocument.getParentType(context, objectType);
				
                Hashtable argumentsTable = new Hashtable(1);
                argumentsTable.put("type", objectType);
                parentType = (String)JPO.invoke(context, "IEFCDMSupport", null , "iefGetParentType", JPO.packArgs(argumentsTable), String.class);  //[SUPPORT()]
                // Get the image of the Type. If a specific naming convention
                // of the image is followed , the following code can be generalized.
                if (title != null && !"".equals(title) && !"null".equals(title))
                {
                  // For a Document the Name will be in the Title object
                  nameOrTitle = title;
                }
                else
                {
                   nameOrTitle = name;
                }
                sbNextURL.append("../common/emxTree.jsp?objectId=");
                sbNextURL.append(objectId);
                sbNextURL.append("&mode=insert&jsTreeID=");
                sbNextURL.append(jsTreeID);
                sbNextURL.append("&DefaultCategory=APPDocumentFiles&AppendParameters=true");
                if(!isprinterFriendly)
                {
                    sBuff.append("<a href ='");
                    sBuff.append(sbNextURL.toString());
                    sBuff.append(" ' class='object' target=\"content\">");
                }
                sBuff.append("<img src='");
                sBuff.append(objectIcon);
                sBuff.append("' border=0 />");
                sBuff.append(MCADUtil.escapeStringForHTML(nameOrTitle));

                if(!isprinterFriendly)
                {
                    sBuff.append("</a>");
                }
                vName.add(sBuff.toString());
            }
        }
        catch(Exception e)
        {
           //System.out.println("Error in getName()"+e);
           throw e;
        }
        finally
        {
           return vName;
        }
    }

    /**
     * getLockStatus- This method is used to show the Lock image.
     *                This method is called from the Column Lock Image.
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object of type Vector
     * @throws Exception if the operation fails
     * @since V10 Patch1
     */
    public Vector getLockStatus(Context context, String[] args)
                                  throws Exception
    {
        Vector showLock= new Vector();
		boolean isVersionable = true;
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList)programMap.get("objectList");
            Map objectMap =  new HashMap();
            Map paramList = (Map)programMap.get("paramList");
            boolean isprinterFriendly = false;
            if(paramList.get("reportFormat") != null)
            {
               isprinterFriendly = true;
            }
            int objectListSize = 0 ;
            if(objectList != null)
            {
              objectListSize = objectList.size();
            }
            StringBuffer baseURLBuf = new StringBuffer(256);

            String statusImageString ="";
            StringList files = new StringList();
            StringList locked = new StringList();
            String  objectType = "";
            String parentType = "";
            String lock = "";
            int lockCount = 0;
            int fileCount = 0;
            String objectId = "";
            String file ="";
            StringBuffer urlBuf = new StringBuffer(256);
            boolean moveFilesToVersion = false;
//Added by Anil to fix CLink #330979
			String []objectIDsArray = new String[objectListSize];
	        for(int i=0; i<objectListSize; i++)
	        {
				Object obj = objectList.get(i);
				if (obj instanceof HashMap)
				{
				   objectIDsArray[i] = (String)((HashMap)obj).get("id");
				}
				else if (obj instanceof Hashtable)
				{
					objectIDsArray[i] = (String)((Hashtable)obj).get("id");
	        }

	        }
            StringList selects = new StringList(7);
			selects.add(DomainConstants.SELECT_ID);
			selects.add(DomainConstants.SELECT_TYPE);
			selects.add(DomainConstants.SELECT_FILE_NAME);
			selects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
			selects.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
			selects.add(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
			selects.add(DomainConstants.SELECT_LOCKED);

			BusinessObjectWithSelectList busObjectSelectList = BusinessObject.getSelectBusinessObjectData(context, objectIDsArray, selects);
//Added by Anil to fix CLink #330979
            for(int i=0; i< objectListSize; i++)
            {
                urlBuf = new StringBuffer(256);
                lockCount = 0;
                fileCount = 0;
                files = new StringList();
                locked = new StringList();
                statusImageString = "";
		
		try
		{
			String objId =null;
			Object obj =   objectList.get(i);

				if (obj instanceof HashMap)
				{
					objId = (String)((HashMap)obj).get("id");
				  
				}
				else if (obj instanceof Hashtable)
				{
					 objId = (String)((Hashtable)obj).get("id");
				}
			
			    //Check whether the object is versionable or not
				Hashtable argsTable = new Hashtable(1);
	            argsTable.put("busId", objId);
	            Boolean isVersionableObject = (Boolean)JPO.invoke(context, "IEFCDMUtil", new String[] {objId}, "isVersionable", JPO.packArgs(argsTable), Boolean.class);
	            isVersionable = isVersionableObject.booleanValue();
			    	            
	            //Changes done by Anil to fix CLink #330979
				/*DomainObject domainObject = DomainObject.newInstance(context, objId);
				StringList selects = new StringList(7);
				selects.add(DomainConstants.SELECT_ID);
				selects.add(DomainConstants.SELECT_TYPE);
				selects.add(DomainConstants.SELECT_FILE_NAME);
				objIEFCDMSupport.IEFAddElement(selects, objIEFCDMSupport.CDM_SELECT_ACTIVE_FILE_LOCKED);
				objIEFCDMSupport.IEFAddElement(selects, objIEFCDMSupport.CDM_SELECT_MOVE_FILES_TO_VERSION);
				objIEFCDMSupport.IEFAddElement(selects, objIEFCDMSupport.CDM_SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
	
				if(!isVersionable) // If the object is not versionable then get the lock status from the major BO.
					selects.add(DomainConstants.SELECT_LOCKED);

				//objectMap = (Hashtable) objectList.get(i);
				objectMap = domainObject.getInfo(context,selects);*/
				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)busObjectSelectList.elementAt(i);	        		
						
				String strid 	= busObjectWithSelect.getSelectData(DomainConstants.SELECT_ID);
	        	String strtype 		= busObjectWithSelect.getSelectData(DomainConstants.SELECT_TYPE);
	        	String strfilename 					= busObjectWithSelect.getSelectData(DomainConstants.SELECT_FILE_NAME);
	        	String strActiveFileLocked 				= busObjectWithSelect.getSelectData(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
				String strMoveFilestoVersion 	= busObjectWithSelect.getSelectData(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
		        String stractiveversionfilename 		= busObjectWithSelect.getSelectData(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
		        String strLocked 					= busObjectWithSelect.getSelectData(DomainConstants.SELECT_LOCKED);
						
				objectMap.put(DomainConstants.SELECT_ID, strid);
				objectMap.put(DomainConstants.SELECT_TYPE, strtype);
				objectMap.put(DomainConstants.SELECT_FILE_NAME, strfilename);
				objectMap.put(CommonDocument.SELECT_ACTIVE_FILE_LOCKED, strActiveFileLocked);
				objectMap.put(CommonDocument.SELECT_MOVE_FILES_TO_VERSION, strMoveFilestoVersion);
				objectMap.put(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION, stractiveversionfilename);
				objectMap.put(DomainConstants.SELECT_LOCKED, strLocked);
//Changes done by Anil to fix CLink #330979
		}
		catch(ClassCastException ex)
		{
                  objectMap = (HashMap) objectList.get(i);
		}
		//System.out.println("the objectMap is " + objectMap);
                objectId = (String) objectMap.get(DomainConstants.SELECT_ID);
                objectType = (String) objectMap.get(DomainConstants.SELECT_TYPE);
                moveFilesToVersion = (new Boolean((String) objectMap.get(CommonDocument.SELECT_MOVE_FILES_TO_VERSION))).booleanValue();//[SUPPORT]
                //parentType = CommonDocument.getParentType(context, objectType);
                Hashtable argumentsTable = new Hashtable(1);
                argumentsTable.put("type", objectType);
                parentType = (String)JPO.invoke(context, "IEFCDMSupport", null , "iefGetParentType", JPO.packArgs(argumentsTable), String.class); //[SUPPORT()]
                              
				if ( parentType.equals(CommonDocument.TYPE_DOCUMENTS) ){
                    if ( moveFilesToVersion )
                    {
                        try
                        {
                            files = (StringList) objectMap.get(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);//[SUPPORT]
                        }
                        catch(ClassCastException cex )
                        {
                            files.add((String) objectMap.get(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION));
                        }
                    } else {
                        try
                        {
                            files = (StringList)objectMap.get(DomainConstants.SELECT_FILE_NAME);
                        }
                        catch(ClassCastException cex )
                        {
                            files.add((String)objectMap.get(DomainConstants.SELECT_FILE_NAME));
                        }
                    }
                    if ( files != null )
                    {
                        fileCount = files.size();
                        if ( fileCount == 1 )
                        {
                            file = (String)files.get(0);
                            if ( file == null || "".equals(file) || "null".equals(file) )
                            {
                                fileCount = 0;
                            }
                        }
                    }

                    try
                    {
                        if(isVersionable) 
                        locked = (StringList) objectMap.get(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);//[SUPPORT]
						else // If the object is not versionable then get the lock status from the major BO.
							locked = (StringList)objectMap.get(DomainConstants.SELECT_LOCKED);

                    } catch(ClassCastException cex)
                    {
						if(isVersionable) 
                        locked.add((String) objectMap.get(CommonDocument.SELECT_ACTIVE_FILE_LOCKED));
						else // If the object is not versionable then get the lock status from the major BO.
							locked.add((String)objectMap.get(DomainConstants.SELECT_LOCKED));
                    }
                    if ( locked != null )
                    {
                        Iterator itr = locked.iterator();
                        while (itr.hasNext())
                        {
                            lock = (String)itr.next();
                            if(lock.equalsIgnoreCase("true"))
                            {
								lockCount ++;
								/*if(isVersionable)
                                lockCount ++;
								else
								{
									lockCount = fileCount;
									break;
								}*/
                            }
                        }
                    }


urlBuf.append(lockCount + "/" + fileCount);
					/*if(isVersionable)
                        urlBuf.append(lockCount + "/" + fileCount);
					else // If the object is not versionable then lockCount = fileCount
						urlBuf.append(fileCount + "/" + fileCount);*/

                    showLock.add(urlBuf.toString());
                } else {
                    showLock.add("&nbsp;");
                }
            }

        }
        catch (Exception ex)
        {
            //System.out.println("Error in getLockStatus= " + ex.getMessage());
            //ex.printStackTrace();
            throw ex;
        }
        finally
        {
            return  showLock;
        }
    }

    /**
     * getRouteStatus- This method is used to show the Lock image.
     *                This method is called from the Column Lock Image.
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object of type Vector
     * @throws Exception if the operation fails
     * @since V10 Patch1
     */
    public Vector getRouteStatus(Context context, String[] args)
                                  throws Exception
    {
        Vector showRoute = new Vector();
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList)programMap.get("objectList");
            Map objectMap = null;
            int objectListSize = 0 ;
            if(objectList != null)
            {
              objectListSize = objectList.size();
            }
            String routeId = "";
            StringList routeIds = new StringList();
            for(int i=0; i< objectListSize; i++)
            {
				String objId = (String)((Hashtable)objectList.get(i)).get("id");
				DomainObject domainObject = DomainObject.newInstance(context, objId);
				StringList selects = new StringList(1);
				selects.add(CommonDocument.SELECT_HAS_ROUTE);

				//objectMap = (Hashtable) objectList.get(i);				  
				objectMap = domainObject.getInfo(context,selects);                

                try
                {
                    routeId = (String) objectMap.get(CommonDocument.SELECT_HAS_ROUTE);
                } catch (ClassCastException cex)
                {
                    routeIds = (StringList) objectMap.get(CommonDocument.SELECT_HAS_ROUTE);//[SUPPORT]
                    routeId = (String)routeIds.get(0);
                }
                // || routeIds != null
                if ( routeId != null)
                {
                    showRoute.add("<img border='0' src='../common/images/iconSmallRoute.gif' alt=''>");
                } else {
                    showRoute.add("&nbsp;");
                }
            }

        }
        catch (Exception ex)
        {
            //System.out.println("Error in getRouteStatus= " + ex.getMessage());
            throw ex;
        }
        finally
        {
            return  showRoute;
        }
    }

    /**
     * getRevisionStatus- This method is used to show the Lock image.
     *                This method is called from the Column Lock Image.
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object of type Vector
     * @throws Exception if the operation fails
     * @since V10 Patch1
     */
    public Vector getRevisionStatus(Context context, String[] args)
                                  throws Exception
    {
        Vector showRev = new Vector();
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList)programMap.get("objectList");            
            Map paramList = (Map)programMap.get("paramList");
            boolean isprinterFriendly = false;
            if(paramList.get("reportFormat") != null)
            {
               isprinterFriendly = true;
            }
            int objectListSize = 0 ;
            if(objectList != null)
            {
                objectListSize = objectList.size();
            }
            
//changes done for Performance improvement--
 String bol_array[]=new String[objectListSize];
            for(int i=0; i< objectListSize; i++)
            {
				String objId =null;
				try{
				 objId = (String)((Hashtable)objectList.get(i)).get("id");
				}
				catch(ClassCastException ex)
				{
                   objId = (String)((HashMap)objectList.get(i)).get("id");
				}
				if(objId.endsWith("\n"))
			{
					objId = objId.substring(0,objId.length()-1);
			}
				bol_array[i]=objId;
		}

			StringList sl_bus = new StringList();
			sl_bus.addElement(DomainConstants.SELECT_REVISION);
                        sl_bus.addElement(DomainConstants.SELECT_ID);
                
			BusinessObjectWithSelectList bwsl = BusinessObject.getSelectBusinessObjectData(context, bol_array, sl_bus);

			for(int i = 0; i < bwsl.size(); i++)
                    {
				BusinessObjectWithSelect busObjectWithSelect = bwsl.getElement(i);
				String revHref = busObjectWithSelect.getSelectData(DomainConstants.SELECT_REVISION);
                                String id      = busObjectWithSelect.getSelectData(DomainConstants.SELECT_ID);
                                if(bol_array[i].equals(id))
                    {                    
                showRev.add(revHref);
            }

        }

        }
        catch (Exception ex)
        {
            //System.out.println("Error in getRevisionStatus= " + ex.getMessage());
            throw ex;
        }
        
            return  showRev;
        }
    /**
     * getVersionStatus- This method is used to show the Lock image.
     *                This method is called from the Column Lock Image.
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @returns Object of type Vector
     * @throws Exception if the operation fails
     * @since V10 Patch1
     */
    public Vector getVersionStatus(Context context, String[] args)
                                  throws Exception
    {
        Vector showVer = new Vector();
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList)programMap.get("objectList");
            Map objectMap = null;
            Map paramList = (Map)programMap.get("paramList");
            boolean isprinterFriendly = false;
            if(paramList.get("reportFormat") != null)
            {
               isprinterFriendly = true;
            }
            int objectListSize = 0 ;
            if(objectList != null)
            {
              objectListSize = objectList.size();
            }
            String languageStr = (String)paramList.get("languageStr");
            String sTipFileVersion = i18nNow.getI18nString("emxComponents.Common.Alt.FileVersions",  "emxComponentsStringResource", languageStr);
            String ver ="";
            String objectType ="";
            String parentType ="";
            String version = "";
            String objectId = "";
            StringBuffer baseURLBuf = new StringBuffer(250);
            //baseURLBuf.append("emxTable.jsp?program=emxCommonFileUI:getFileVersions&popup=true&table=APPFileVersions&header=emxComponents.Common.DocumentVersionsPageHeading&subHeader=emxComponents.Menu.SubHeaderDocuments&HelpMarker=emxhelpcommondocuments&disableSorting=true&suiteKey=Components&FilterFramePage=../components/emxCommonDocumentCheckoutUtil.jsp&FilterFrameSize=1");
            StringBuffer urlBuf = new StringBuffer(250);
            for(int i=0; i< objectListSize; i++)
            {
                urlBuf = new StringBuffer(250);
	        try
		{
				String objId = null;
				try
				{
					 objId = (String)((Hashtable)objectList.get(i)).get("id");
				}
				
				catch(ClassCastException ex)
				{
                   objId = (String)((HashMap)objectList.get(i)).get("id");
				}
				if(objId.endsWith("\n"))
			{
					objId = objId.substring(0,objId.length()-1);
			}
				DomainObject domainObject = DomainObject.newInstance(context, objId);
				StringList selects = new StringList(3);
				selects.add(DomainConstants.SELECT_ID);
				selects.add(DomainConstants.SELECT_TYPE);
				selects.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION);

				//objectMap = (Hashtable) objectList.get(i);
				objectMap = domainObject.getInfo(context,selects);
		}
		catch(ClassCastException ex)
		{
                  objectMap = (HashMap) objectList.get(i);
		}

                objectType = (String) objectMap.get(DomainConstants.SELECT_TYPE);
                objectId = (String) objectMap.get(DomainConstants.SELECT_ID);
                Hashtable argumentsTable = new Hashtable(1);
                argumentsTable.put("type", objectType);
                parentType = (String)JPO.invoke(context, "IEFCDMSupport", null , "iefGetParentType", JPO.packArgs(argumentsTable), String.class); //CommonDocument.getParentType(context, objectType);

                if ( parentType.equals(CommonDocument.TYPE_DOCUMENTS) ){
                    try
                    {
			//System.out.println("getting version");
                        version = (String) objectMap.get(CommonDocument.SELECT_ACTIVE_FILE_VERSION);//[SUPPORT]
			//System.out.println("getting version");
                        if ( version == null || "".equals(version) || "null".equals(version) )
                        {
                            version = "&nbsp;";
                        }
                    }
                    catch(ClassCastException cex )
                    {
                        version = "<img border='0' src='../common/images/iconSmallFiles.gif' alt=\"" + sTipFileVersion +"\" title=\"" + sTipFileVersion +"\">";
			StringList versionList = (StringList) objectMap.get(CommonDocument.SELECT_ACTIVE_FILE_VERSION);//[SUPPORT]
			if(versionList.size() == 1)
			  version = (String) versionList.get(0);
			else 
			  version = "--";

                    }
		    /*
                    if(!isprinterFriendly && !"&nbsp;".equals(version) )
                    {
                        urlBuf.append("<a href =\"javascript:showNonModalDialog('");
                        urlBuf.append(baseURLBuf.toString());
                        urlBuf.append("&objectId=");
                        urlBuf.append(objectId);
                        urlBuf.append("',730,450)\">");
                    }
		    */
                    urlBuf.append(version);
		    /*
                    if(!isprinterFriendly && !"&nbsp;".equals(version))
                    {
                        urlBuf.append("</a>");
                    }
		    */
                    //showVer.add(urlBuf.toString());
		    showVer.add(version);
                } else {
                  showVer.add("&nbsp;");
                }
            }
        }
        catch (Exception ex)
        {
            //System.out.println("Error in getVersionStatus= " + ex.getMessage());
            throw ex;
        }
        finally
        {
            return  showVer;
        }
    }



    public static String getParentType(Context context, String type) throws Exception
    {
        String parentType = (String)typeMapping.get(type);
        if ( parentType == null )
        {
            setParentTypeMapping(context, type);
        }
        parentType = (String)typeMapping.get(type);
        if ( parentType != null )
        {
            return parentType;
        } else {
            return type;
        }
    }

    public static void setParentTypeMapping(Context context, String type) throws Exception
    {
        String currentType = type;
        BusinessType bType = new BusinessType(currentType, context.getVault());
        String parentType = bType.getParent(context);
        boolean isAbstract = true;
        if ( parentType != null && !"".equals(parentType) )
        {
            bType = new BusinessType(parentType, context.getVault());
            isAbstract = bType.isAbstract(context);
        }
        while ( !isAbstract && !parentType.equals(CommonDocument.TYPE_DOCUMENTS) )
        {
            currentType = parentType;
            bType = new BusinessType(currentType, context.getVault());
            parentType = bType.getParent(context);
            if ( parentType != null && !"".equals(parentType) )
            {
                bType = new BusinessType(parentType, context.getVault());
                isAbstract = bType.isAbstract(context);
            } else {
                isAbstract = true;
            }
        }
        if ( parentType != null && !"".equals(parentType) )
        {
            typeMapping.put(type, parentType);
        } else {
            typeMapping.put(type, currentType);
        }
/*
        if ( isAbstract && parentType.equals("DOCUMENTS")  )
        {
            typeMapping.put(type, currentType);
        } else {
            typeMapping.put(type, parentType);
        }
*/
    }


   /**
    *  Get Vector of Strings for Document Action Icons
    *
    *  @param context the eMatrix <code>Context</code> object
    *  @param args an array of String arguments for this method
    *  @return Vector object that contains a vector of html code to
    *        construct the Actions Column.
    *  @throws Exception if the operation fails
    *
    *  @since Common 10.5
    *  @grade 0
    */
    public static Vector getDocumentActions(Context context, String[] args)
        throws Exception
    {

        Vector vActions = new Vector();

        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList)programMap.get("objectList");
            Map paramList      = (Map)programMap.get("paramList");

            boolean isprinterFriendly = false;
            if(paramList.get("reportFormat") != null)
            {
                isprinterFriendly = true;
            }

            String languageStr = (String)paramList.get("languageStr");

            StringBuffer strActionURL = null;

            String objectId    = null;
            Map objectMap      = null;
            int objectListSize = 0 ;

            String sTipDownload = i18nNow.getI18nString("emxIEFDesignCenter.Common.ToolTip.Download", "emxIEFDesignCenterStringResource", languageStr);
            String sTipCheckout = i18nNow.getI18nString("emxIEFDesignCenter.Common.Checkout", "emxIEFDesignCenterStringResource", languageStr);
            String sTipCheckin  = i18nNow.getI18nString("emxIEFDesignCenter.Command.CheckIn",  "emxIEFDesignCenterStringResource", languageStr);
            String sTipUpdate   = i18nNow.getI18nString("emxIEFDesignCenter.Common.Update",   "emxIEFDesignCenterStringResource", languageStr);
            String sTipSubscriptions   = i18nNow.getI18nString("emxIEFDesignCenter.Common.ToolTipSubscriptions",   "emxIEFDesignCenterStringResource", languageStr);

            if(objectList != null)
            {
                objectListSize = objectList.size();
            }
            String objectType = "";
            String isVersionable = "true";
            StringList files = new StringList();
            String file ="";
            int fileCount = 0;
            StringList locked = new StringList();
            String lock ="";
            int lockCount = 0;
            boolean hasCheckoutAccess = true;
            boolean hasCheckinAccess = true;
            boolean moveFilesToVersion = false;
            for(int i=0; i< objectListSize; i++)
            {
                files = new StringList();
                file = "";
                fileCount = 0;
                locked = new StringList();
                lock = "";
                lockCount = 0;
                objectMap      = (Map) objectList.get(i);
                objectId       = (String)objectMap.get(DomainConstants.SELECT_ID);
                objectType = (String) objectMap.get(DomainConstants.SELECT_TYPE);
                isVersionable = (String) objectMap.get(CommonDocument.SELECT_SUSPEND_VERSIONING);//[SUPPORT]
                hasCheckoutAccess = (new Boolean((String) objectMap.get(CommonDocument.SELECT_HAS_CHECKOUT_ACCESS))).booleanValue();//[SUPPORT]
                hasCheckinAccess = (new Boolean((String) objectMap.get(CommonDocument.SELECT_HAS_CHECKIN_ACCESS))).booleanValue();//[SUPPORT]
                StringBuffer strBuf = new StringBuffer();
                //String parentType = CommonDocument.getParentType(context, objectType);
				
                Hashtable argumentsTable = new Hashtable(1);
                argumentsTable.put("type", objectType);
                String parentType = (String)JPO.invoke(context, "IEFCDMSupport", null , "iefGetParentType", JPO.packArgs(argumentsTable), String.class); //[SUPPORT()]
                
                moveFilesToVersion = (new Boolean((String) objectMap.get(CommonDocument.SELECT_MOVE_FILES_TO_VERSION))).booleanValue();//[SUPPORT]
                if ( moveFilesToVersion )
                {
                    try
                    {
                        files = (StringList) objectMap.get(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);//[SUPPORT]
                    }
                    catch(ClassCastException cex )
                    {
                        files.add((String) objectMap.get(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION));//[SUPPORT]
                    }
                } else {
                    try
                    {
                        files = (StringList)objectMap.get(DomainConstants.SELECT_FILE_NAME);
                    }
                    catch(ClassCastException cex )
                    {
                        files.add((String)objectMap.get(DomainConstants.SELECT_FILE_NAME));
                    }
                }
                if ( files != null )
                {
                    fileCount = files.size();
                    if ( fileCount == 1 )
                    {
                        file = (String)files.get(0);
                        if ( file == null || "".equals(file) || "null".equals(file) )
                        {
                            fileCount = 0;
                        }
                    }
                }

                try
                {
                    locked = (StringList) objectMap.get(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);//[SUPPORT]
                } catch(ClassCastException cex)
                {
                    locked.add((String) objectMap.get(CommonDocument.SELECT_ACTIVE_FILE_LOCKED));//[SUPPORT]
                }
                if ( locked != null )
                {
                    Iterator itr = locked.iterator();
                    while (itr.hasNext())
                    {
                        lock = (String)itr.next();
                        if(lock.equalsIgnoreCase("true"))
                        {
                            lockCount ++;
                        }
                    }
                }

                if ((CommonDocument.TYPE_DOCUMENTS).equals(parentType) ) {
                    if(!isprinterFriendly)
                    {
                            strActionURL = new StringBuffer("../components/emxSubscriptionDialog.jsp?suiteKey=Components&objectId=");
                            strActionURL.append(objectId);
                            strBuf.append("<a href='javascript:showNonModalDialog(\""+strActionURL.toString()+"\",730,450)'>");
                            strBuf.append("<img border='0' src='../common/images/iconSmallSubscription.gif' alt=\""+sTipSubscriptions+"\" title=\""+sTipSubscriptions+"\"></a>&nbsp;");

                    } else {
                        strBuf.append("<img border='0' src='../common/images/iconSmallSubscription.gif' alt=\""+sTipSubscriptions+"\" title=\""+sTipSubscriptions+"\">&nbsp;");
                    }
                    strActionURL       = new StringBuffer("../components/emxCommonDocumentPreCheckout.jsp?objectId=");
                    if ( fileCount != 0 && hasCheckoutAccess)
                    {
                        // Show download, checkout for all type of files.
                        if(!isprinterFriendly)
                        {
                            strActionURL       = new StringBuffer("../components/emxCommonDocumentPreCheckout.jsp?objectId=");
                            strActionURL.append(objectId);
                            strActionURL.append("&action=download");
                            strBuf.append("<a href='javascript:showNonModalDialog(\""+strActionURL.toString()+"\",730,450)'>");
                            strBuf.append("<img border='0' src='../common/images/iconActionDownload.gif' alt=\""+sTipDownload+"\" title=\""+sTipDownload+"\"></a>&nbsp;");
                        }
                        else
                        {
                            strBuf.append("<img border='0' src='../common/images/iconActionDownload.gif' alt=\""+sTipDownload+"\" title=\""+sTipDownload+"\">&nbsp;");
                        }
                    }

                    //checkout w/lock
                    if ( "false".equalsIgnoreCase(isVersionable) )
                    {
                        if ( fileCount != 0 && hasCheckoutAccess && hasCheckinAccess && lockCount != fileCount)
                        {
                            if(!isprinterFriendly)
                            {
                                strActionURL  = new StringBuffer("../components/emxCommonDocumentPreCheckout.jsp?objectId=");
                                strActionURL.append(objectId);
                                strActionURL.append("&action=checkout");
                                strBuf.append("<a href='javascript:showNonModalDialog(\""+strActionURL.toString()+"\",730,450)'>");
                                strBuf.append("<img border='0' src='../common/images/iconActionCheckOut.gif' alt=\""+sTipCheckout+"\" title=\""+sTipCheckout+"\"></a>&nbsp");
                            }
                            else
                            {
                                strBuf.append("<img border='0' src='../common/images/iconActionCheckOut.gif' alt=\""+sTipCheckout+"\" title=\""+sTipCheckout+"\">&nbsp");
                            }
                        }
                        if (hasCheckinAccess)
                        {
                            //checkin
                            if(!isprinterFriendly)
                            {
                                strBuf.append("<a href=\"javascript:showNonModalDialog('../components/emxCommonDocumentPreCheckin.jsp?objectId="+objectId+"&showComments=true&showFormat=true&objectAction=checkin','780','570');\">");
                                strBuf.append("<img border='0' src='../common/images/iconActionAppend.gif' alt=\""+sTipCheckin+"\" title=\""+sTipCheckin+"\"></a>&nbsp");
                            }
                            else
                            {
                                strBuf.append("<img border='0' src='../common/images/iconActionAppend.gif' alt=\""+sTipCheckin+"\" title=\""+sTipCheckin+"\">&nbsp");
                            }

                            //update
                            if ( lockCount > 0 )
                            {
                              if(!isprinterFriendly)
                              {
                                  strBuf.append("<a href=\"javascript:showNonModalDialog('../components/emxCommonDocumentPreCheckin.jsp?objectId="+objectId+"&showFormat=readonly&showComments=required&objectAction=update&allowFileNameChange=true','730','450');\">");
                                  strBuf.append("<img border='0' src='../common/images/iconActionCheckIn.gif' alt=\""+sTipUpdate+"\" title=\""+sTipUpdate+"\"></a>&nbsp");
                              }
                              else
                              {
                                  strBuf.append("<img border='0' src='../common/images/iconActionCheckIn.gif' alt=\""+sTipUpdate+"\" title=\""+sTipUpdate+"\">&nbsp");
                              }
                            }
                        }
                    }
                } else {
                    strBuf.append("&nbsp;");
                }
                vActions.add(strBuf.toString());
            }
        }
        catch (Exception ex)
        {
            //System.out.println("Error in getDocumentActions = " + ex);
            //ex.printStackTrace();
            throw ex;
        }
        finally
        {
            return vActions;
        }
    }

   /**
    *  Get Vector of Strings for Document Revision Action Icons
    *  @param context the eMatrix <code>Context</code> object
    *  @param args an array of String arguments for this method
    *  @return Vector object that contains a vector of html code to
    *        construct the Actions Column.
    *  @throws Exception if the operation fails
    *
    *  @since Common 10.5
    *  @grade 0
    */
    public static Vector getDocumentRevisionActions(Context context, String[] args)
        throws Exception
    {

        Vector vActions = new Vector();
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList)programMap.get("objectList");
            Map paramList      = (Map)programMap.get("paramList");

            boolean isprinterFriendly = false;
            if(paramList.get("reportFormat") != null)
            {
                isprinterFriendly = true;
            }

            String languageStr = (String)paramList.get("languageStr");

            StringBuffer strActionURL = null;

            String objectId    = null;
            Map objectMap      = null;
            int objectListSize = 0 ;

            String sTipDownload = i18nNow.getI18nString("emxIEFDesignCenter.Common.ToolTip.Download", "emxIEFDesignCenterStringResource", languageStr);

            if(objectList != null)
            {
                objectListSize = objectList.size();
            }

            for(int i=0; i< objectListSize; i++)
            {
                objectMap      = (Map) objectList.get(i);
                objectId       = (String)objectMap.get(DomainConstants.SELECT_ID);

                StringBuffer strBuf = new StringBuffer();

                strActionURL       = new StringBuffer("../components/emxCommonDocumentPreCheckout.jsp?objectId=");

                // Show download, checkout for all type of files.
                if(!isprinterFriendly)
                {
                    strActionURL       = new StringBuffer("../components/emxCommonDocumentPreCheckout.jsp?objectId=");
                    strActionURL.append(objectId);
                    strActionURL.append("&action=download");
                    strBuf.append("<a href='javascript:showNonModalDialog(\""+strActionURL.toString()+"\",730,450)'>");
                    strBuf.append("<img border='0' src='../common/images/iconActionDownload.gif' alt=\""+sTipDownload+"\" title=\""+sTipDownload+"\"></a>&nbsp;");
                }
                else
                {
                    strBuf.append("<img border='0' src='../common/images/iconActionDownload.gif' alt=\""+sTipDownload+"\" title=\""+sTipDownload+"\">&nbsp;");
                }

                vActions.add(strBuf.toString());
            }
        }
        catch (Exception ex)
        {
            //System.out.println("Error in getDocumentActions = " + ex);
            //ex.printStackTrace();
            throw ex;
        }
        finally
        {
            return vActions;
        }
    }

    public Object getLocker(Context context, String[] args) throws Exception
	{
    	Vector columnCellContentList = new Vector();

    	HashMap paramMap	 = (HashMap)JPO.unpackArgs(args);

		MapList relBusObjPageList	= (MapList)paramMap.get("objectList");
		HashMap paramList 			= (HashMap)paramMap.get("paramList");
		IEFGlobalCache cache		= new IEFGlobalCache();
		String languageStr 			= (String)paramList.get("languageStr");

		MCADServerResourceBundle serverResourceBundle		= new MCADServerResourceBundle(languageStr);
		IEFIntegAccessUtil util						= new IEFIntegAccessUtil(context, serverResourceBundle, cache);
		
		String[] objIds	= new String[relBusObjPageList.size()];
		for(int i =0; i<relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");			
		}

		String REL_VERSION_OF			= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		String REL_ACTIVE_VERSION		= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
		String SELECT_ON_MAJOR			= "from[" + REL_VERSION_OF + "].to.";
		String SELECT_ON_ACTIVE_MINOR	= "from[" + REL_ACTIVE_VERSION + "].to.";		
		String ATTR_SOURCE				= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";
		String ATTR_CADTYPE				= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType") + "]";
		
		Vector assignedIntegrations = util.getAssignedIntegrations(context);
		
		StringList busSelectionList = new StringList();
			
		busSelectionList.addElement("id");
		busSelectionList.addElement("type");
		busSelectionList.addElement(ATTR_SOURCE); //To get Integrations name.
		busSelectionList.addElement(SELECT_ON_MAJOR + "id"); //Major object id.
		busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + "id"); //Minor ID
		busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + "type");
		busSelectionList.addElement("locker"); //To get Integrations name.
		busSelectionList.addElement(SELECT_ON_MAJOR + "locker"); //from Minor.
		busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + "locker"); //Minor ID
		busSelectionList.addElement(ATTR_CADTYPE);

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);			

		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);

		    String integrationName	= null;

			String objectId				= busObjectWithSelect.getSelectData("id");
			String integrationSource	= busObjectWithSelect.getSelectData(ATTR_SOURCE);
			String showLocker			= "";
			//String majorObjectId		= "";

			if(integrationSource != null)
			{
				StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

				if(integrationSourceTokens.hasMoreTokens())
					integrationName  = integrationSourceTokens.nextToken();
			}

			if(integrationName != null && assignedIntegrations.contains(integrationName))
			{	
				//IEFSimpleConfigObject simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, integrationName); // [NDM] OP6

				if(null == cache)
				{
				   cache = new IEFGlobalCache();
			    }

				//String busType				= busObjectWithSelect.getSelectData("type"); // [NDM] OP6
				boolean isVersionObject 	= !util.isMajorObject(context, objectId);//isMinorType(context, busType, simpleGCO, util); // [NDM] OP6

				String cadType             = busObjectWithSelect.getSelectData(ATTR_CADTYPE);
				boolean isCADLike			=false;
				MCADGlobalConfigObject gco = null;
				gco = (MCADGlobalConfigObject) gcoTable.get(integrationName);
				if(gco != null)
				{
					if(gco.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_CADMODEL_LIKE) || gco.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
					{
						isCADLike = true;					
					}
				}

				if(!isVersionObject)
				{
					if(	isCADLike)
					{
						//majorObjectId		= objectId;
						showLocker	= busObjectWithSelect.getSelectData("locker");
					}
					else
					{
						StringList lockerList  	= busObjectWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + "locker");
						showLocker = 	 (String) lockerList.get(0);
					}
				}
				else
				{
					if(	isCADLike)
					{
						//majorObjectId		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "id");
						showLocker	= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "locker");
					}
					else
					{
						showLocker	= busObjectWithSelect.getSelectData("locker");
					}
				}
			}
			else
			{
				StringList lockerList = busObjectWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + "locker");
				if(lockerList !=null && lockerList.size() > 0)
				{
					showLocker	=  (String) lockerList.get(0);
				}
			}
			columnCellContentList.add(showLocker);
		}

		return columnCellContentList;
	}

 // [NDM] OP6
    //   Removed --private boolean isMinorType(matrix.db.Context context, String type, IEFSimpleConfigObject simpleGCO , MCADMxUtil mxUtil)

}

