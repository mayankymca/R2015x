/*
 *  emxTeamContentBase.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 * All Rights Reserved. 
 *
 */

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import matrix.db.Access;
import matrix.db.BusinessType;
import matrix.db.Context;
import matrix.db.FileList;
import matrix.db.FormatItr;
import matrix.db.FormatList;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.SelectList;
import matrix.util.StringItr;
import matrix.util.StringList;

import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.common.CommonDocumentable;
import com.matrixone.apps.common.Company;
import com.matrixone.apps.common.VCDocument;
import com.matrixone.apps.common.Workspace;
import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.AccessUtil;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.SetUtil;
import com.matrixone.apps.domain.util.StringUtil;
import com.matrixone.apps.domain.util.VaultUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.framework.ui.UIUtil;

/**
 * @version Team 10-0-1-0 Release - Copyright (c) 2002, MatrixOne, Inc.
 */

public class emxTeamContentBase_mxJPO extends emxDomainObject_mxJPO
{

  static final String sAttributeBracket        = "attribute[";
  static final String sCloseBracket            = "]";
  static final String sToBracket               = "to[";
  static final String sBracketFromId           = "].from.id";
  static final String sBracketToId             = "].to.id";
  static final String sBracketFromName         = "].from.name";
  static final String hasReadStr               = "current.access[read,checkout]";
  static final String sFromBracket             = "from[";
  static final String sBracketToToBracket      = "].to.to.[";
  static final String hasReadWriteStr          = "current.access[checkin,modify,lock,unlock,revise]";
  static final String hasRemoveStr             = "current.access[delete,todisconnect,fromdisconnect]";
  static final String formatJTStr                    = PropertyUtil.getSchemaProperty("format_JT");
  // Designer Central Changes
  static final String SELECT_ACTIVE_VERSION_ID = "relationship[Active Version].to.id";
  // Designer Central Changes
  String objectId                              = null;
  String docRouteId                            = null;
  String routeId                               = null;
  String docLocked                             = null;
  String docLocker                             = null;
  String docOwner                              = null;
  String Title                                 = null;
  String docDesc                               = null;
  String docType                               = null;
  String strFileFormat                         = null;
  String DocumentsinMultipleRoutes             = null;
  String canRouteCheckoutDocuments             = null;

    // sort default by content title
    protected static final String sbSortKey = sAttributeBracket + DomainObject.ATTRIBUTE_TITLE + sCloseBracket;

    // String for selecting workspaceid through RouteScope rel
    protected static final String sbSelWsId = sToBracket+DomainObject.RELATIONSHIP_ROUTE_SCOPE+sBracketFromId;

    protected static final String sbSelMsgId  = sToBracket+DomainObject.RELATIONSHIP_MESSAGE_ATTACHMENTS+sBracketFromId;

    protected static final String sbSelMeetId = sToBracket+DomainObject.RELATIONSHIP_MEETING_ATTACHMENTS+sBracketFromId;

    protected static final String sbSelFolId  = sToBracket+DomainObject.RELATIONSHIP_VAULTED_DOCUMENTS+sBracketFromId;

    protected static final String sbSelRtId   = sFromBracket+DomainObject.RELATIONSHIP_OBJECT_ROUTE+sBracketToId;

    protected static final String sbSelRelActVerDesc = "relationship["+CommonDocument.RELATIONSHIP_ACTIVE_VERSION+"].to.description";

    protected static final String sbSelRelActVerRev = "relationship["+CommonDocument.RELATIONSHIP_ACTIVE_VERSION+"].to.revision";

    protected static final String sbSelRelActVerId = "relationship["+CommonDocument.RELATIONSHIP_ACTIVE_VERSION+sBracketToId;

    protected static final String sbLockedSelect = "relationship["+CommonDocument.RELATIONSHIP_ACTIVE_VERSION+"].to.locked";

    protected static final String sbLockerSelect = "relationship["+CommonDocument.RELATIONSHIP_ACTIVE_VERSION+"].to.locker";

  boolean boolRouted               = false;
  boolean boolLock                 = false;
  boolean accessFlag               = false;
  boolean isWorkspaceLead          = false;
  boolean bIsTypeWorkspaceVault    = false;
  boolean bIsTypeRoute             = false;
  boolean hasWorkspace             = false;
  boolean hasParentReadAccess      = false;
  boolean hasParentReadWriteAccess = false;
  boolean hasParentRemoveAccess    = false;

  StringList selListType                      = new StringList(4);
  FileList fileList                           = null;
  FormatItr formatItr                         = null;

  /**
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds no arguments
   * @throws Exception if the operation fails
   * @since Team 10-0-1-0
   */
  public emxTeamContentBase_mxJPO (Context context, String[] args)
    throws Exception
  {
    super(context, args);
    // Updating the String Buffers so that they can be used
    // through out the program

    selListType.add(hasReadStr);
    selListType.add(hasReadWriteStr);
    selListType.add(hasRemoveStr);

    DocumentsinMultipleRoutes=EnoviaResourceBundle.getProperty(context,"emxTeamCentral.DocumentsinMultipleRoutes");
    canRouteCheckoutDocuments =EnoviaResourceBundle.getProperty(context,"emxTeamCentral.CanRouteCheckoutDocuments");
  }

  /**
   * This method will be called when ever we invoke the JPO with out calling any method.explicitly
   * @param context the eMatrix <code>Context</code> object
   * @param args holds no value
   * @return int
   * @throws Exception if the operation fails
   * @since Team 10-0-1-0
   */
  public int mxMain(Context context, String[] args)
    throws Exception
  {
    if (!context.isConnected())
      throw new Exception(ComponentsUtil.i18nStringNow("emxTeamCentral.Generic.NotSupportedOnDesktopClient", context.getLocale().getLanguage()));
      return 0;
  }

  /**
   * getFolderContent - This method is used to get the content in the folder
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the following input arguments:
   *        0 - object Id of the folder
   *        1 - object Id of the workspace
   * @return MapList
   * @throws Exception if the operation fails
   * @deprecated TC 10-7-SP1 use getFolderContentIds
   *        Due to performance issue, the method only re turns ids.
   * @since Team 10-0-1-0
   */
  @com.matrixone.apps.framework.ui.ProgramCallable
  public MapList getFolderContent(Context context, String[] args)
    throws Exception
  {
    MapList contentMapList = null;
    try
    {
      HashMap programMap        = (HashMap) JPO.unpackArgs(args);
      Map paramList = (Map)programMap.get("paramList");
      String workspaceId = (String)programMap.get("workspaceId");

      // Object ID of the folder or route
      objectId = (String)programMap.get("objectId");

      hasWorkspace = false;
      boolean isWorkspaceOwner =false;
      //Constructing the Workspace Object instance
      Workspace Workspace = (Workspace) DomainObject.newInstance(context,DomainConstants.TYPE_WORKSPACE,DomainConstants.TEAM);

      //Building the domain Object instance
      DomainObject domainObject        = DomainObject.newInstance(context,objectId);
      String sTypeName                 = domainObject.getInfo(context, "type");

      boolean bIsTypeRoute          = sTypeName.equals(DomainObject.TYPE_ROUTE);
      boolean bIsTypeWorkspaceVault = sTypeName.equals(DomainObject.TYPE_WORKSPACE_VAULT);

      // If workspace Id is null or "" then get the workspace Id using
      // getWorkspaceid for a Folder or using a query if it is a Route
      if ((null == workspaceId)            ||
          ("".equals(workspaceId))         ||
          ("#DENIED!".equals(workspaceId)) ||
          ("null".equals(workspaceId)))
      {
        if(bIsTypeWorkspaceVault)
        {
          workspaceId = getWorkspaceId(context,objectId);
        }
        else if(bIsTypeRoute)
        {
          StringBuffer select_workspaceId = new StringBuffer(64);
          select_workspaceId.append(sFromBracket);
          select_workspaceId.append(DomainConstants.RELATIONSHIP_MEMBER_ROUTE);
          select_workspaceId.append("sBracketToToBracket");
          select_workspaceId.append(DomainConstants.RELATIONSHIP_PROJECT_MEMBERS);
          select_workspaceId.append(sBracketFromId);
          workspaceId = domainObject.getInfo(context,select_workspaceId.toString());
        }
      }

      //getting the acess for the Person on the Workspace.
      Access contextAccess = domainObject.getAccessMask(context);
      hasParentReadAccess =  AccessUtil.hasReadAccess(contextAccess);
      hasParentReadWriteAccess =  AccessUtil.hasReadWriteAccess(contextAccess);
      hasParentRemoveAccess    =AccessUtil.hasRemoveAccess(contextAccess);
      boolean hasWorkspace             = false;

      if ((null == workspaceId)            ||
          ("".equals(workspaceId))         ||
          ("#DENIED!".equals(workspaceId)) ||
          ("null".equals(workspaceId)))
      {
        workspaceId  ="";
      }
      else
      {
        hasWorkspace = true;
      }

      if(hasWorkspace)
      {
        Workspace.setId(workspaceId);
        //checking whether the User has a Project Lead access.
        isWorkspaceLead = Workspace.isProjectLead(context,com.matrixone.apps.common.Person.getPerson(context));
        String strUser = context.getUser();//com.matrixone.apps.common.Person.getPerson(context).getInfo(context,"name");
        String sOwnerWorkspace  = Workspace.getInfo(context,Workspace.SELECT_OWNER);
        // checking that whether the Owner is the User Loged in.
        if(sOwnerWorkspace.equals(strUser)){
          isWorkspaceOwner =true;
        }
      }


      // build select params
      StringList selectTypeStmts = new StringList(25);
      selectTypeStmts.add(DomainConstants.SELECT_ID);
      selectTypeStmts.add(DomainConstants.SELECT_NAME);
      selectTypeStmts.add(DomainConstants.SELECT_TYPE);
      selectTypeStmts.add(DomainConstants.SELECT_REVISION);
      selectTypeStmts.add(DomainConstants.SELECT_ORIGINATED);
      selectTypeStmts.add(DomainConstants.SELECT_DESCRIPTION);
      selectTypeStmts.add(sbLockedSelect.toString());
      selectTypeStmts.add(sbLockerSelect.toString());
      selectTypeStmts.add(DomainConstants.SELECT_OWNER);
      selectTypeStmts.add(DomainConstants.SELECT_FILE_NAME);
      selectTypeStmts.add(sbSelRtId.toString());
      selectTypeStmts.add(sbSelMsgId.toString());
      selectTypeStmts.add(sbSelMeetId.toString());
      selectTypeStmts.add(sbSelRelActVerDesc.toString());
      selectTypeStmts.add(sbSelRelActVerRev.toString());
      selectTypeStmts.add(sbSelRelActVerId.toString());
      selectTypeStmts.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
      selectTypeStmts.add(CommonDocument.SELECT_TITLE);
      selectTypeStmts.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION);
      selectTypeStmts.add(CommonDocument.SELECT_HAS_ROUTE);
      selectTypeStmts.add(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
      selectTypeStmts.add(CommonDocument.SELECT_SUSPEND_VERSIONING);
      selectTypeStmts.add(CommonDocument.SELECT_HAS_CHECKOUT_ACCESS);
      selectTypeStmts.add(CommonDocument.SELECT_HAS_CHECKIN_ACCESS);
      selectTypeStmts.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
//Added for DSFA
      selectTypeStmts.add(CommonDocument.SELECT_IS_KIND_OF_VC_DOCUMENT);
   //Taken out due to performance issue, these are taken care by jsps
  //    selectTypeStmts.add(CommonDocument.SELECT_VCFILE_LOCKED);
  //    selectTypeStmts.add(CommonDocument.SELECT_VCFILE_LOCKER);
      selectTypeStmts.add(CommonDocument.SELECT_VCFOLDER);
      selectTypeStmts.add("vcfile");

      // If Route is Parent object then select the Folder id of Vaulted Object
      if(bIsTypeRoute) {
        selectTypeStmts.add(sbSelFolId.toString());
      }

//commented for bug 352726
     // String sObjWhere = "current.access[read] == TRUE && revision ~~ last";
      //modified for  bug 352726
      String sObjWhere = "current.access[read] == TRUE";

      //if it is a Workspace Vault (Folder)
      if (bIsTypeWorkspaceVault)
      {
        contentMapList = domainObject.getRelatedObjects(context,
                                                        DomainObject.RELATIONSHIP_VAULTED_DOCUMENTS,
                                                         "*",
                                                         selectTypeStmts,
                                                         null,
                                                         false,
                                                         true,
                                                         (short)1,
                                                         sObjWhere,
                                                         null,
                                                         null,
                                                         null,
                                                         null);

      }
      else // Route
      {
        contentMapList = domainObject.getRelatedObjects(context,
                                                      DomainObject.RELATIONSHIP_OBJECT_ROUTE,
                                                      "*",
                                                      selectTypeStmts,
                                                      null,
                                                      true,
                                                      false,
                                                      (short)1,
                                                      sObjWhere,
                                                      null,
                                                      null,
                                                      null,
                                                      null);
      }

      Iterator contentListItr = contentMapList.iterator();
      Map contentMap          = null;
      String sType            = DomainConstants.TYPE_DOCUMENT;
      String contentName      = null;
      String parentType = null;
      DomainObject document      = DomainObject.newInstance(context);
      boolean hasContentReadAccess      = false;
      boolean hasContentReadWriteAccess = false;
      boolean hasContentRemoveAccess    = false;

      while(contentListItr.hasNext())
      {

        contentMap   = (Map)contentListItr.next();
        sType        = (String) contentMap.get(DomainConstants.SELECT_TYPE);
        parentType = CommonDocument.getParentType(context, sType);

        hasContentReadAccess = false;
        hasContentReadWriteAccess= false;
        hasContentRemoveAccess= false;
        //addding the bIsTypeRoute,bIsTypeWorkspaceVault,hasWorkspace so that
        //they can be used later in other Methods

        contentMap.put("bIsTypeRoute",Boolean.valueOf(bIsTypeRoute));
        contentMap.put("bIsTypeWorkspaceVault",Boolean.valueOf(bIsTypeWorkspaceVault));
        contentMap.put("hasWorkspace",Boolean.valueOf(hasWorkspace));
        contentMap.put("isWorkspaceLead",Boolean.valueOf(isWorkspaceLead));
        contentMap.put("isWorkspaceOwner",Boolean.valueOf(isWorkspaceOwner));
        contentMap.put("hasParentRemoveAccess",Boolean.valueOf(hasParentRemoveAccess));
        contentMap.put("objectId",objectId);
        if (!parentType.equals(CommonDocument.TYPE_DOCUMENTS))
        {
          // for sourcing content, title would be empty; hence populate title
          // keys with sourcing objects name
          // Required to have common sort key in framework maplist iterator
          contentName = (String)contentMap.get(DomainConstants.SELECT_NAME);
          contentMap.put(CommonDocument.SELECT_TITLE, contentName);
        }
        else if(parentType.equals(CommonDocument.TYPE_DOCUMENTS))
        {
           StringList files = new StringList(1);

           boolean moveFilesToVersion = (Boolean.valueOf((String) contentMap.get(CommonDocument.SELECT_MOVE_FILES_TO_VERSION))).booleanValue();
           if ( moveFilesToVersion )
           {
                try
                {
                    files = (StringList)contentMap.get(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
                }
                catch(ClassCastException cex )
                {
                    files.add((String)contentMap.get(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION));
                }
           } else {
               try
                {
                    files = (StringList)contentMap.get(CommonDocument.SELECT_FILE_NAME);
                }
                catch(ClassCastException cex )
                {
                    files.add((String)contentMap.get(CommonDocument.SELECT_FILE_NAME));
                }
           }
           contentMap.put(CommonDocument.SELECT_FILE_NAME, files);

           StringList locked = new StringList(1);
           try
           {
              locked = (StringList)contentMap.get(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
              if (locked == null)
                locked = new StringList();
           }
           catch(ClassCastException cex)
           {
              String sLocked = (String)contentMap.get(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
              if (sLocked == null)
                sLocked = "";
              locked.add(sLocked);
           }
           contentMap.put(CommonDocument.SELECT_ACTIVE_FILE_LOCKED, locked);

           routeId = (String)contentMap.get(DomainConstants.SELECT_ID);
           document.setId(routeId);
           // for Gettting the access on the Document Object and Updating
           // the Map with the Details.

           Access contextAccess1 = document.getAccessMask(context);
           hasContentReadAccess =  AccessUtil.hasReadAccess(contextAccess1);
           hasContentReadWriteAccess =  AccessUtil.hasReadWriteAccess(contextAccess1);
           hasContentRemoveAccess    =AccessUtil.hasRemoveAccess(contextAccess1);


           //Modified on 29/Mar/2006 for fixing the bug 309621
           hasContentReadWriteAccess = hasContentReadWriteAccess && hasParentReadAccess;
           hasContentRemoveAccess = hasParentRemoveAccess && hasContentReadWriteAccess;

           contentMap.put("hasContentReadAccess",Boolean.valueOf(hasContentReadAccess));
           contentMap.put("hasContentReadWriteAccess",Boolean.valueOf(hasContentReadWriteAccess));
           contentMap.put("hasContentRemoveAccess",Boolean.valueOf(hasContentRemoveAccess));
           //gettting the Default Format for the document.
           //Added for bug 373244
           FormatList formatlist = document.getFormats(context);
           strFileFormat = formatlist.size() > 0 ?  document.getDefaultFormat(context) : null;
           //Commented for bug 373244
           //strFileFormat  = document.getDefaultFormat(context);
           //gettting all the formats that the Document Supports.
           formatItr      = new FormatItr(formatlist);
           while (formatItr.next() )
           {
               strFileFormat = formatItr.obj().getName();
               fileList      = document.getFiles(context,strFileFormat);
               if(fileList.size() > 0 ) {
                   break;
               }
           }
           contentMap.put("strFileFormat",strFileFormat);
        } //end of else - type document
      } //end of while
    }
    catch (Exception ex)
    {
     System.out.println("Error in getFolderContent = " + ex.getMessage());
     ex.printStackTrace(System.out);
     throw ex;
    }
    finally
    {
     return contentMapList;
    }
  }

  /**
  * Determine if Document contains multiple files.
  * @param context the eMatrix <code>Context</code> object.
  * @param docOID - the Object ID of the Document.
  * @return boolean.
  * @exception Exception if the operation fails.
  */

  public static boolean containsMultipleFiles(Context context, String docOID)
      throws Exception
  {
    boolean multipleFiles = false;
    DomainObject docObject = DomainObject.newInstance(context, docOID);
    docObject.open(context);
    StringList tmpList = docObject.getInfoList(context, sbSelRelActVerId.toString());
    if (tmpList != null)
    {
      if (tmpList.size() > 1)
        multipleFiles = true;
    }

    docObject.close(context);
    return multipleFiles;
  }

  /**
   * showCheckbox - This Method will check whether the check box must be
   *                enabled or Disabled
   *                Called in the CheckBox column
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the following input arguments:
   *        0 - objectList MapList
   * @return Object of type Vector
   * @throws Exception if the operation fails
   * @since Team 10-0-1-0
   */
  public Vector showCheckbox(Context context, String[] args)
    throws Exception
  {
    Vector enableCheckbox = new Vector();
    try
    {
      HashMap programMap = (HashMap) JPO.unpackArgs(args);
      MapList objectList = (MapList)programMap.get("objectList");
      String strUser = context.getUser();

      isWorkspaceLead = false;
      Map  objectMap = null;
      int objectListSize = 0 ;
      if(objectList != null)
      {
        objectListSize = objectList.size();
      }

      for(int i = 0 ; i < objectListSize  ; i++)
      {
        docRouteId="";
        objectMap = (Hashtable)objectList.get(i);

        bIsTypeWorkspaceVault = ((Boolean)objectMap.get("bIsTypeWorkspaceVault")).booleanValue();
        bIsTypeRoute = ((Boolean)objectMap.get("bIsTypeRoute")).booleanValue();
        hasWorkspace = ((Boolean)objectMap.get("hasWorkspace")).booleanValue();
        isWorkspaceLead = ((Boolean)objectMap.get("isWorkspaceLead")).booleanValue();
        hasParentRemoveAccess = ((Boolean)objectMap.get("hasParentRemoveAccess")).booleanValue();
        if (objectMap.get(sbSelRtId.toString()) != null)
        {
          docRouteId  = (objectMap.get(sbSelRtId.toString())).toString();
        }

        String documentId = (String)objectMap.get(DomainConstants.SELECT_ID);
        boolean multiFile = containsMultipleFiles(context, documentId);
        if (!multiFile)
        {
          docLocked = (String)objectMap.get(sbLockedSelect.toString());
          if (docLocked == null)
            docLocked = "";
          docOwner = (String)objectMap.get(sbLockerSelect.toString());
          if (docOwner == null)
            docOwner = "";
        }
        else
        {
          docLocked = "";
          docOwner = "";
        }

        boolRouted = false;
        if ((docRouteId != null)     &&
            (!"".equals(docRouteId)) &&
            (!"null".equals(docRouteId)))
        {
          boolRouted = true;
        }

        if (docLocked.equals("TRUE"))
        {
          boolLock = true;
        }
        else
        {
          boolLock = false;
        }

        if (strUser.equals(docOwner) ||
            isWorkspaceLead ||
            hasParentRemoveAccess)
        {
          accessFlag = true;
        }

        if (boolRouted &&
            DocumentsinMultipleRoutes.equals("false") &&
            !bIsTypeRoute)
        {
          enableCheckbox.add("false");
        }
        else if ((bIsTypeWorkspaceVault && !boolLock) ||
                 (boolRouted && !bIsTypeWorkspaceVault && !boolLock) ||
                 (bIsTypeWorkspaceVault &&
                  boolLock &&
                  canRouteCheckoutDocuments.equals("true")))
        {
          if(accessFlag)
          {
             enableCheckbox.add("true");
          }
          else
          {
            enableCheckbox.add("false");
          }
        }
        else
        {
         enableCheckbox.add("false");
        }
      }
    }
    catch (Exception ex)
    {
      System.out.println("Error in showCheckbox()");
      throw ex;
    }
    finally
    {
     return enableCheckbox;
    }
  }

  /**
   * getLockImage - This method is used to show the Lock image.
   *                This method is called from the Column Lock Image.
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the following input arguments:
   *        0 - objectList MapList
   * @return Object of type Vector
   * @throws Exception if the operation fails
   * @since V10 Patch1
   */
  public Vector getLockImage(Context context, String[] args)
    throws Exception
  {
    Vector showLock= new Vector();
    String statusImageString = "";
    try
    {
      HashMap programMap = (HashMap) JPO.unpackArgs(args);
      MapList objectList = (MapList)programMap.get("objectList");
      Map objectMap = null;
      boolean boolRouted = false;
      boolean boolLock = false;
      boolean bIsTypeWorkspaceVault=false;
      boolean bIsTypeRoute=false;

      routeId       = "";
      docLocked     = "";
      docType       = "";
      strFileFormat = "";
      int objectListSize = 0 ;
      if(objectList != null)
      {
        objectListSize = objectList.size();
      }
      for(int i=0; i< objectListSize; i++)
      {
        objectMap = (Hashtable) objectList.get(i);
        statusImageString = "";
        strFileFormat     = "";
        docRouteId        = "";
        routeId = (String) objectMap.get(DomainConstants.SELECT_ID);

        // Check for Documents from other Apps that contain multiple files.
        String documentId = (String)objectMap.get(DomainConstants.SELECT_ID);
        boolean multiFile = containsMultipleFiles(context, documentId);
        if (!multiFile)
        {
          docLocked = (String)objectMap.get(sbLockedSelect.toString());
          if (docLocked == null)
            docLocked = "";
        }
        else
        {
          docLocked = "";
        }

        docType=(String) objectMap.get(DomainConstants.SELECT_TYPE);
        bIsTypeWorkspaceVault = ((Boolean)objectMap.get("bIsTypeWorkspaceVault")).booleanValue();
        bIsTypeRoute = ((Boolean)objectMap.get("bIsTypeRoute")).booleanValue();
        strFileFormat=(String)objectMap.get("strFileFormat");
        if (objectMap.get(sbSelRtId.toString()) != null)
            docRouteId  = (objectMap.get(sbSelRtId.toString())).toString();
        boolRouted = false;
        if ((docRouteId != null)     &&
            (!"".equals(docRouteId)) &&
            (!"null".equals(docRouteId)))
          boolRouted = true;

        boolLock=false;
        if (docLocked.equals("TRUE"))
           boolLock = true;

        if (boolRouted && bIsTypeWorkspaceVault)
        {
           statusImageString = "<img style='border:0; padding: 2px;' src='../common/images/iconStatusRouteLocked.gif' alt=''>";
        }
        else if (boolLock)
        {
           statusImageString = "<img style='border:0; padding: 2px;' src='../common/images/iconStatusLocked.gif' alt=''>";
        }
        showLock.add(statusImageString);
      }

    }
    catch (Exception ex)
    {
      System.out.println("Error in getLockImage= " + ex.getMessage());
      throw ex;
    }
    finally
    {
      return  showLock;
    }
  }


    /**
     * getVersion - This method is used to get the Version based on the
     *              Type of the Content.
     * For Documents, it shows the Version and For Type like Part, RFQ, etc.
     * it shows ""
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @return Object of type Vector
     * @throws Exception if the operation fails
     * @since Team 10-0-1-0
     */
    public Vector getVersion(Context context, String[] args)throws Exception{
      Vector version= new Vector();
      try
      {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);

        MapList objectList = (MapList)programMap.get("objectList");
        String sFileTableHeader = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getSession().getLocale(), "emxComponents.Common.Versions");

        //Added for IR-071890V6R2012 starts
        Map paramList = (Map)programMap.get("paramList");
        boolean isprinterFriendly = false;
        if(paramList.get("reportFormat") != null)
        {
           isprinterFriendly = true;
        }
        //  //Added for IR-071890V6R2012 ends
        Iterator objectListItr = objectList.iterator();

        while(objectListItr.hasNext())
        {
          StringBuffer sbURL = new StringBuffer(128);
          Map objectMap = (Map) objectListItr.next();



          docType = (String) objectMap.get(DomainConstants.SELECT_TYPE);


          String documentId = (String)objectMap.get(DomainConstants.SELECT_ID);
          DomainObject dombj = new DomainObject();
          dombj.setId(documentId);
          if(UIUtil.isNullOrEmpty(docType))
        	  docType=dombj.getInfo(context, "type");
          if (docType.equals(DomainConstants.TYPE_DOCUMENT))
          {

           // BJW - Added to check if doc is versionable - START
            boolean isVersionable = true;
            if( CommonDocument.TYPE_DOCUMENTS.equals(CommonDocument.getParentType(context, docType)) )
            {
              CommonDocumentable commonDocument = (CommonDocumentable)DomainObject.newInstance(context,docType);
              if ( (documentId != null && !"".equals(documentId) && !"null".equals(documentId)) )
              {
                isVersionable = CommonDocument.allowFileVersioning(context, documentId);
              } else {
                isVersionable = CommonDocument.checkVersionableType(context, docType);
              }
            }
            else
            {
              isVersionable = false;
            }

            if (isVersionable)
            {

            // BJW - Added to check if doc is versionable - END

              boolean multiFile = containsMultipleFiles(context, documentId);
              if (!multiFile)
              {
                String docVer = (String)objectMap.get(sbSelRelActVerRev.toString());
                if(UIUtil.isNullOrEmpty(docVer))
                {
                	docVer =  dombj.getInfo(context, sbSelRelActVerRev.toString());
                }
                //The below code is written to handle non-versioned documents
                if(docVer == null || docVer.length() == 0 )
                docVer = "";
                //end of code

                docVer.trim();
                String objectId = documentId;//(String)objectMap.get("id");
                String parentId = (String)objectMap.get(DomainConstants.SELECT_ID);
                StringBuffer sbNextURL = new StringBuffer(64);
                sbNextURL.append("../common/emxTable.jsp?HelpMarker=emxhelpversions&amp;program=emxCommonFileUI:getFileVersions&amp;table=APPFileVersions&amp;sortColumnName=Version&amp;sortDirection=descending&amp;header=");
                sbNextURL.append(sFileTableHeader);
                sbNextURL.append("&amp;objectId=");
                sbNextURL.append(objectId);
                sbNextURL.append("&amp;parentOID=");
                sbNextURL.append(objectId);
                //Added for IR-071890V6R2012 starts
                if(!isprinterFriendly)
                {
                    sbURL.append("<a href =\"javascript:showModalDialog('");
                    sbURL.append(sbNextURL.toString());
                    sbURL.append("',575,575)\">");
                    sbURL.append(docVer).append("</a>");
                }
                else {
                     sbURL.append(XSSUtil.encodeForHTML(context, docVer));
                }
                //Added for IR-071890V6R2012 ends
              }
            // BJW - Added to check if doc is versionable - START
            }
            // BJW - Added to check if doc is versionable - END
          }
          else
          {
            sbURL.append("&#160;");
          }
          version.add(sbURL.toString());
        }
      }
      catch(Exception ex)
      {
        System.out.println("Error in getVersion=" + ex.getMessage());
      }
      finally
      {
        return version;
      }
   }


     /**
     * getRevison - This method is used to get the Revision  basing on the Type of the Content.
     *              For Documents It shows "" and For Type like Part, RFQ etc etc it Shows Revision
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - objectList MapList
     * @return Object of type Vector
     * @throws Exception if the operation fails
     * @since Team 10-0-1-0
     */
    public Vector getRevision(Context context, String[] args)throws Exception{
      Vector revision= new Vector();
      try
      {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList)programMap.get("objectList");
        Iterator objectListItr = objectList.iterator();

        while(objectListItr.hasNext()){
          Map objectMap = (Map) objectListItr.next();
          docType = (String) objectMap.get(DomainConstants.SELECT_TYPE);

          if (!docType.equals(DomainConstants.TYPE_DOCUMENT)) {
            revision.add(objectMap.get(DomainConstants.SELECT_REVISION));
          }else{
            revision.add("");
          }
        }
      }catch(Exception ex){
      System.out.println("Error in getRevison=" + ex.getMessage());
      }finally{
      return revision;
      }
    }

    // Designer Central

    private HashMap getViewerURLInfo(Context context, String objectId, String fileName)
    {
      HashMap viewerInfoMap = null;
      try
      {
           DomainObject obj = DomainObject.newInstance(context, objectId);
           MapList associatedFileList = obj.getAllFormatFiles(context);
           for (int i = 0; i < associatedFileList.size(); i++)
           {
             Map associatedFile = (Map)associatedFileList.get(i);
             if (fileName.equals((String)associatedFile.get("filename")))
             {
                 viewerInfoMap = new HashMap();
           viewerInfoMap.put("fileName", fileName);
           viewerInfoMap.put("format", associatedFile.get("format"));
           viewerInfoMap.put("id", objectId);
             break;
         }
           }

        }
        catch (Exception ex)
        {
            System.out.println("Error in getViewerURLInfo=" + ex.getMessage());
            ex.printStackTrace();
        }
        return viewerInfoMap;
    }

  // Designer Central


  /**
   * getContentActions - This method will be called to get the Actions that
   *                     can be performed on the Content
   *                     This is called in the Actions Column of the Table
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the following input arguments:
   *        0 - objectList MapList
   * @return Object of type Vector
   * @throws Exception if the operation fails
   * @since V10 Patch1
   */
  public Vector getContentActions(Context context, String[] args)
    throws Exception
  {
     Vector vActions = new Vector();
     try
     {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList)programMap.get("objectList");
         Map paramList = (Map)programMap.get("paramList");
         String uiType = (String)paramList.get("uiType");
         String customSortColumns = (String)paramList.get("customSortColumns");
         String customSortDirections = (String)paramList.get("customSortDirections");
         String table = (String)paramList.get("table");
        if(objectList == null || objectList.size() <= 0)
        {
            return vActions;
        }

        boolean isprinterFriendly = false;
        if (paramList.get("reportFormat") != null)
        {
            isprinterFriendly = true;

        }

         Locale locale = context.getSession().getLocale();
         String sTipView = EnoviaResourceBundle.getProperty(context,  "emxTeamCentralStringResource", locale, "emxTeamCentral.ContentSummary.ToolTipView");
         String sTipNoView =EnoviaResourceBundle.getProperty(context,"emxTeamCentralStringResource", locale, "emxTeamCentral.ViewerLaunch.NoViewerFound");
         String sTipDownload = EnoviaResourceBundle.getProperty(context,  "emxTeamCentralStringResource", locale, "emxTeamCentral.ContentSummary.ToolTipDownload");
         String sTipSubscription = EnoviaResourceBundle.getProperty(context, "emxTeamCentralStringResource", locale, "emxTeamCentral.ContentSummary.ToolTipSubscription");
         String sTipCheckout = EnoviaResourceBundle.getProperty(context, "emxTeamCentralStringResource", locale, "emxTeamCentral.ContentSummary.ToolTipCheckout");
         String sTipCheckin = EnoviaResourceBundle.getProperty(context, "emxTeamCentralStringResource", locale, "emxTeamCentral.ContentSummary.ToolTipCheckin");
         String sTipAddFiles = EnoviaResourceBundle.getProperty(context, "emxTeamCentralStringResource", locale, "emxTeamCentral.ContentSummary.ToolTipAddFiles");

		StringList selectTypeStmts = new StringList(20);
        selectTypeStmts.add(DomainConstants.SELECT_ID);
        selectTypeStmts.add(DomainConstants.SELECT_TYPE);
        selectTypeStmts.add(CommonDocument.SELECT_SUSPEND_VERSIONING);
        selectTypeStmts.add(CommonDocument.SELECT_HAS_CHECKOUT_ACCESS);
        selectTypeStmts.add(CommonDocument.SELECT_HAS_CHECKIN_ACCESS);
         selectTypeStmts.add(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
         selectTypeStmts.add(CommonDocument.SELECT_FILE_NAME);
         selectTypeStmts.add(CommonDocument.SELECT_FILE_FORMAT);
        selectTypeStmts.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
        selectTypeStmts.add(CommonDocument.SELECT_IS_KIND_OF_VC_DOCUMENT);
        selectTypeStmts.add("vcfile");
        selectTypeStmts.add("vcmodule");
         selectTypeStmts.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
         selectTypeStmts.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKER);
         selectTypeStmts.add(CommonDocument.SELECT_HAS_TOCONNECT_ACCESS);
         selectTypeStmts.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION_ID);
         selectTypeStmts.add(CommonDocument.SELECT_OWNER);
         selectTypeStmts.add(CommonDocument.SELECT_LOCKED);
         selectTypeStmts.add(CommonDocument.SELECT_LOCKER);
         selectTypeStmts.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION);
        
         selectTypeStmts.add(CommonDocument.SELECT_CURRENT);
         //Getting all the content ids
		HashMap versionMap = new HashMap();	
        String oidsArray[] = new String[objectList.size()];
        for (int i = 0; i < objectList.size(); i++)
        {
           try
           {
               oidsArray[i] = (String)((HashMap)objectList.get(i)).get("id");
           } catch (Exception ex)
           {
               oidsArray[i] = (String)((Hashtable)objectList.get(i)).get("id");
           }
        }

        MapList objList = DomainObject.getInfo(context, oidsArray, selectTypeStmts);

		String linkAttrName = PropertyUtil.getSchemaProperty(context,"attribute_MxCCIsObjectLinked");
         Iterator objectListItr = objList.iterator();
        while(objectListItr.hasNext()){
          Map contentObjectMap = (Map)objectListItr.next();
          Map lockCheckinStatusMap = CommonDocument.getLockAndCheckinIconStatus(context, contentObjectMap);
          boolean isAnyFileLockedByContext = (boolean)lockCheckinStatusMap.get("isAnyFileLockedByContext");
          boolean isContextDocumentOwner= (boolean)lockCheckinStatusMap.get("isContextDocumentOwner");

          StringList fileList = (StringList) contentObjectMap.get(CommonDocument.SELECT_FILE_NAME);
          StringList fileFormatList = (StringList) contentObjectMap.get(CommonDocument.SELECT_FILE_FORMAT);
          StringList tempfileList  = new StringList();
          for(int ii =0; ii< fileFormatList.size(); ii++){
          	String format = (String)fileFormatList.get(ii);
          	if(!DomainObject.FORMAT_MX_MEDIUM_IMAGE.equalsIgnoreCase(format)){
          		tempfileList.add(fileList.get(ii));
          	}
          }
          fileList =tempfileList;

          String versionId = null;
          String version   = null;

          StringList versionIdList = (StringList)contentObjectMap.get(CommonDocument.SELECT_ACTIVE_FILE_VERSION_ID);
          if(versionIdList != null && versionIdList.size()>0){
        	  versionId = (String)versionIdList.get(0);
          }
          StringList versonList = (StringList)contentObjectMap.get(CommonDocument.SELECT_ACTIVE_FILE_VERSION);
          if(versonList != null && versonList.size()>0){
        	  version = (String)versonList.get(0);
          }

          int fileCount = 0;
          String vcInterface = "";
          boolean vcDocument = false;
          boolean vcFile = false;
          String docType = "";
          String activeFileVersionID = "";
          String sFileName = "";
          String newURL = "";

          try{

              docLocker = (String)contentObjectMap.get(CommonDocument.SELECT_ACTIVE_FILE_LOCKER);
              if(docLocker==null)
                  docLocker = (String)contentObjectMap.get(CommonDocument.SELECT_LOCKER);
              }catch(ClassCastException ex){

                  docLocker = ((StringList)contentObjectMap.get(CommonDocument.SELECT_ACTIVE_FILE_LOCKER)).elementAt(0).toString();
                  if(docLocker==null)
                      docLocker = ((StringList)contentObjectMap.get(CommonDocument.SELECT_LOCKER)).elementAt(0).toString();
                  }

          boolean moveFilesToVersion = (Boolean.valueOf((String) contentObjectMap.get(CommonDocument.SELECT_MOVE_FILES_TO_VERSION))).booleanValue();
          String documentId = (String)contentObjectMap.get(DomainConstants.SELECT_ID);
		  String strFileFormat  = (String)fileFormatList.get(0);     

          //   For getting the count of files
          HashMap filemap = new HashMap();
          filemap.put(CommonDocument.SELECT_MOVE_FILES_TO_VERSION, contentObjectMap.get(CommonDocument.SELECT_MOVE_FILES_TO_VERSION));
          filemap.put(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION, contentObjectMap.get(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION));
          filemap.put(CommonDocument.SELECT_FILE_NAME, fileList);
          fileCount = CommonDocument.getFileCount(context,filemap);
          contentObjectMap.put("fileCount",String.valueOf(fileCount));// Integer.toString(fileCount));

          vcInterface = (String)contentObjectMap.get(CommonDocument.SELECT_IS_KIND_OF_VC_DOCUMENT);
                vcDocument = "TRUE".equalsIgnoreCase(vcInterface)?true:false;

          // Can View
          docType    = (String)contentObjectMap.get(DomainConstants.SELECT_TYPE);
          
		  if(!versionMap.containsKey(docType)){
		  	versionMap.put(docType, CommonDocument.checkVersionableType(context, docType));
		  }			

          String parentType = CommonDocument.getParentType(context, docType);
            if (CommonDocument.TYPE_DOCUMENTS.equals(parentType))
            {
			if(CommonDocument.canView(context, contentObjectMap,strFileFormat )){					  
              Object fileObj = contentObjectMap.get(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
              if (fileObj instanceof String) {
                  sFileName = (String)fileObj;
              } else if(fileObj instanceof StringList) {
                  sFileName = ((StringList)fileObj).elementAt(0).toString();
                  }
              if (!isprinterFriendly){
              if (moveFilesToVersion){
                      Object obj = contentObjectMap.get(CommonDocument.SELECT_ACTIVE_FILE_VERSION_ID);
                      if (obj instanceof String) {
                          activeFileVersionID = (String)obj;
                      } else if(obj instanceof StringList) {
                          activeFileVersionID = ((StringList)obj).elementAt(0).toString();
                }
                     // get the format that the Active version object contains the file
                  HashMap viewerURLMap = getViewerURLInfo(context, activeFileVersionID, sFileName);
                  if (viewerURLMap != null){
                          //XSSOK
                          newURL = emxCommonFileUI_mxJPO.getViewerURL(context,
                                              activeFileVersionID,
                                                      (String)viewerURLMap.get("format"),
                                  sFileName);
                     }
                  }
                  else
                  { // Designer Central Changes

                     //XSSOK
                     newURL = emxCommonFileUI_mxJPO.getViewerURL(context, documentId,
                                                                      strFileFormat,
                             sFileName);
                  }
                }
              }
          //Can Download
          if(CommonDocument.canDownload(context, contentObjectMap)){
                if (!isprinterFriendly)
                {
                	newURL+="<a href=\"javascript:callCheckout('"+XSSUtil.encodeForJavaScript(context, documentId)+"','download','','','"+XSSUtil.encodeForJavaScript(context, customSortColumns)+"','"+XSSUtil.encodeForJavaScript(context, uiType)+"','"+XSSUtil.encodeForJavaScript(context, table)+"');\"><img style=\"border:0; padding: 2px;\" src=\"../common/images/iconActionDownload.png\" alt=\""+sTipDownload+ "\" title=\""+sTipDownload+"\"></img></a>";

                }
                else
                {
                	newURL+="<img style=\"border:0; padding: 2px;\" src=\"../common/images/iconActionDownload.png\" alt=\""+sTipDownload+ "\" ></img>";

                }
      // Changes for CLC start here..
      //Show Download Icon for ClearCase Linked Objects
                     //DomainObject ccLinkedObject  = DomainObject.newInstance(context, documentId);
		
        String isObjLinked = null;
        if(linkAttrName!=null && !linkAttrName.equals(""))
        {
						  DomainObject docObject = DomainObject.newInstance(context,documentId);            
                         isObjLinked = docObject.getAttributeValue(context,linkAttrName);
        }

        if(isObjLinked!=null && !isObjLinked.equals(""))
        {
          if(isObjLinked.equalsIgnoreCase("True"))
          {
          //show download icon for Linked Objects
        	  newURL+="<a href=\"../servlet/MxCCCS/MxCCCommandsServlet.java?commandName=downloadallfiles&objectId="+XSSUtil.encodeForURL(context, documentId)+"\"><img style=\"border:0; padding: 2px;\" src=\"../common/images/iconActionDownload.png\" alt=\""+sTipDownload+"\" title=\""+sTipDownload+"\"></img></a>";

          }

        }
          }
          // show subscription link
                if (!isprinterFriendly)
                {
                	StringBuffer strBuf = new StringBuffer(1256);
                	strBuf.append("../components/emxSubscriptionDialog.jsp?objectId=");
              strBuf.append(XSSUtil.encodeForJavaScript(context, documentId));
                	//newURL+="<a href=\"javascript:showModalDialog('" +strBuf.toString()+ "','575','575')\"><img style=\"border:0; padding: 2px;\" src=\"../common/images/iconSmallSubscription.png\" alt=\""+sTipSubscription+"\" title=\""+sTipSubscription+"\"></img></a>";
                	newURL+="<a href=\"javascript:getTopWindow().showSlideInDialog('" +strBuf.toString()+ "')\"><img style=\"border:0; padding: 2px;\" src=\"../common/images/iconSmallSubscription.png\" alt=\""+sTipSubscription+"\" title=\""+sTipSubscription+"\"></img></a>";

                }
                else
                {
                	newURL+="<img style=\"border:0; padding: 2px;\" src=\"../common/images/iconSmallSubscription.png\" alt=\""+sTipSubscription+ "\" ></img>";

                }

          // Can Checkout
				  if(CommonDocument.canCheckout(context, contentObjectMap, false, ((Boolean) versionMap.get(docType)).booleanValue())){
                       if (!isprinterFriendly)
                       {
                    	   newURL+="<a href=\"javascript:callCheckout('"+XSSUtil.encodeForJavaScript(context, documentId)+"','checkout','','','"+XSSUtil.encodeForJavaScript(context, customSortColumns)+"','"+XSSUtil.encodeForJavaScript(context, customSortDirections)+"', '"+XSSUtil.encodeForJavaScript(context, uiType)+"', '"+XSSUtil.encodeForJavaScript(context, table)+"');\"><img style=\"border:0; padding: 2px;\" src=\"../common/images/iconActionCheckOut.png\" alt=\""+sTipCheckout+ "\" title=\""+sTipCheckout+"\"></img></a>";

                       }
                       else
                       {
                    	   newURL+="<img style=\"border:0; padding: 2px;\" src=\"../common/images/iconActionCheckOut.png\" alt=\""+sTipCheckout+ "\" ></img>";

                       }
                    }

          // Can Checkout
				  
          if(CommonDocument.canCheckin(context, contentObjectMap) || VCDocument.canVCCheckin(context, contentObjectMap)){
						  
				 vcFile =(Boolean.valueOf((String) contentObjectMap.get("vcfile"))).booleanValue();
						  if(!isprinterFriendly)
						  {
							  StringBuffer strBuf = new StringBuffer(1256);
							  if (isAnyFileLockedByContext) {
					if (!vcDocument)
							{
							  strBuf.append("'../components/emxCommonDocumentIntermediatePreCheckin.jsp?objectId=");
					          strBuf.append(XSSUtil.encodeForJavaScript(context, documentId));
					          strBuf.append("&amp;folderId=");
					          strBuf.append(objectId);
							  strBuf.append("&amp;customSortColumns="); //Added for Bug #371651 starts
							  strBuf.append(XSSUtil.encodeForJavaScript(context, customSortColumns));
							  strBuf.append("&amp;customSortDirections=");
							  strBuf.append(XSSUtil.encodeForJavaScript(context, customSortDirections));
							  strBuf.append("&amp;uiType=");
							  strBuf.append(XSSUtil.encodeForJavaScript(context, uiType));
							  strBuf.append("&amp;table=");
							  strBuf.append(XSSUtil.encodeForJavaScript(context, table));                 //Added for Bug #371651 ends
							  strBuf.append("&amp;showFormat=true&amp;showComments=required&amp;objectAction=update&amp;JPOName=emxTeamDocumentBase&amp;appDir=teamcentral&amp;appProcessPage=emxTeamPostCheckinProcess.jsp&amp;refreshTableContent=true','730','450'");
							  newURL+="<a href=\"javascript:emxTableColumnLinkClick("+strBuf.toString()+",false,'listHidden','',null,'true');\"><img style=\"border:0; padding: 2px;\" src=\"../common/images/iconActionCheckIn.png\" alt=\""+sTipCheckin+"\" title=\""+sTipCheckin+"\"></img></a>";

							}else {
					  if(vcFile){
							  strBuf.append("'../components/emxCommonDocumentPreCheckin.jsp?objectId=");
					  strBuf.append(XSSUtil.encodeForJavaScript(context, documentId));
					  strBuf.append("&amp;folderId=");
					  strBuf.append(objectId);
							  strBuf.append("&amp;customSortColumns=");         //Added for Bug #371651 starts
							  strBuf.append(XSSUtil.encodeForJavaScript(context, customSortColumns));
							  strBuf.append("&amp;customSortDirections=");
							  strBuf.append(XSSUtil.encodeForJavaScript(context, customSortDirections));
							  strBuf.append("&amp;uiType=");
							  strBuf.append(XSSUtil.encodeForJavaScript(context, uiType));
							  strBuf.append("&amp;table=");
							  strBuf.append(XSSUtil.encodeForJavaScript(context, table));                 //Added for Bug #371651 ends
							  strBuf.append("&amp;showFormat=false&amp;showComments=required&amp;objectAction=checkinVCFile&amp;allowFileNameChange=false&amp;noOfFiles=1&amp;JPOName=emxVCDocument&amp;methodName=checkinUpdate&amp;refreshTableContent=true','730','450'");
							  newURL+="<a href=\"javascript:showModalDialog("+strBuf.toString()+");\"><img style=\"border:0; padding: 2px;\" src=\"../common/images/iconActionCheckIn.png\" alt=\""+sTipCheckin+"\" title=\""+sTipCheckin+"\"></img></a>";

							}
							 else {
							  strBuf.append("'../components/emxCommonDocumentPreCheckin.jsp?objectId=");
						 strBuf.append(XSSUtil.encodeForJavaScript(context, documentId));
						 strBuf.append("&amp;folderId=");
						 strBuf.append(objectId);
							  strBuf.append("&amp;customSortColumns=");     //Added for Bug #371651 starts
							  strBuf.append(XSSUtil.encodeForJavaScript(context, customSortColumns));
							  strBuf.append("&amp;customSortDirections=");
							  strBuf.append(XSSUtil.encodeForJavaScript(context, customSortDirections));
							  strBuf.append("&amp;uiType=");
							  strBuf.append(XSSUtil.encodeForJavaScript(context, uiType));
							  strBuf.append("&amp;table=");
							  strBuf.append(XSSUtil.encodeForJavaScript(context, table));                 //Added for Bug #371651 ends
							  strBuf.append("&amp;override=false&amp;showFormat=false&amp;showComments=required&amp;objectAction=checkinVCFile&amp;allowFileNameChange=true&amp;noOfFiles=1&amp;JPOName=emxVCDocument&amp;methodName=checkinUpdate&amp;refreshTableContent=true','730','450'");
							  newURL+="<a href=\"javascript:showModalDialog("+strBuf.toString()+");\"><img style=\"border:0; padding: 2px;\" src=\"../common/images/iconActionCheckIn.png\" alt=\""+sTipCheckin+"\" title=\""+sTipCheckin+"\"></img></a>";

							 }
							}

					if (fileCount<=1 && !vcDocument)
				  {
				  //can unlock document 
		        	  
                         // strBuf.append("<a href=\"../teamcentral/emxTeamUnlockDocument.jsp?&docId="); //Commented for Bug #371651
                         // Modified for Bug #371651 starts
						
						if (isContextDocumentOwner || isAnyFileLockedByContext) {
                          String titleText =  EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(), "emxComponents.Common.LockedBy");
                		  StringBuffer strngBuf = new StringBuffer(1256);
                          strngBuf.append("'../teamcentral/emxTeamUnlockDocument.jsp?docId=");
                          strngBuf.append(XSSUtil.encodeForJavaScript(context, documentId));
                          strngBuf.append("&amp;customSortColumns=");
                          strngBuf.append(XSSUtil.encodeForJavaScript(context, customSortColumns));
                          strngBuf.append("&amp;customSortDirections=");
                          strngBuf.append(XSSUtil.encodeForJavaScript(context, customSortDirections));
                          strngBuf.append("&amp;uiType=");
                          strngBuf.append(XSSUtil.encodeForJavaScript(context, uiType));
                          strngBuf.append("&amp;table=");
                          strngBuf.append(XSSUtil.encodeForJavaScript(context, table));
                          strngBuf.append("'");// Modified for Bug #371651 ends
                          newURL+="<a href=\"javascript:submitWithCSRF("+strngBuf.toString()+", findFrame(getTopWindow(),'hiddenFrame'));\"><img style=\"border:0; padding: 2px;\" src=\"../common/images/iconActionUnlock.png\" alt=\""+docLocker+"\" title=\""+titleText+" : "+docLocker+"\"></img></a>";
                       }
		    		
						  }
					} else
	                       {
  							   String titleText =  EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(), "emxComponents.Common.LockedBy");
							   if (isAnyFileLockedByContext) {
	                    	   newURL+="<img style=\"border:0; padding: 2px;\" src=\"../common/images/iconActionCheckIn.png\" alt=\""+sTipCheckin+"\" title=\""+sTipCheckin+"\"></img>";
								}
	                    	   if (fileCount<=1)
	                    	   {
	       							if (isContextDocumentOwner || isAnyFileLockedByContext) {
										newURL+="<img style=\"border:0; padding: 2px;\" src=\"../common/images/iconActionUnlock.png\" alt=\""+titleText+" : "+docLocker+"\" title=\""+docLocker+"\"></img>";
	                    		   }
	                    	   }
                        	}
						 }
                       }


          // Can Add Files
          if(CommonDocument.canAddFiles(context, contentObjectMap)){
                      if(!isprinterFriendly)
                      {
                    	  StringBuffer strBuf = new StringBuffer(1256);
                        strBuf.append("'../components/emxCommonDocumentPreCheckin.jsp?objectId=");
                strBuf.append(XSSUtil.encodeForJavaScript(context, documentId));
                strBuf.append("&amp;folderId=");
                strBuf.append(objectId);
                        strBuf.append("&amp;customSortColumns=");       //Added for Bug #371651 starts
                        strBuf.append(XSSUtil.encodeForJavaScript(context, customSortColumns));
                        strBuf.append("&amp;customSortDirections=");
                        strBuf.append(XSSUtil.encodeForJavaScript(context, customSortDirections));
                        strBuf.append("&amp;uiType=");
                        strBuf.append(XSSUtil.encodeForJavaScript(context, uiType));
                        strBuf.append("&amp;table=");
                        strBuf.append(XSSUtil.encodeForJavaScript(context, table));                   //Added for Bug #371651 ends
                        strBuf.append("&amp;showFormat=true&amp;showDescription=required&amp;objectAction=checkin&amp;showTitle=true&amp;JPOName=emxTeamDocumentBase&amp;appDir=teamcentral&amp;appProcessPage=emxTeamPostCheckinProcess.jsp&amp;refreshTableContent=true','730','450'");
                        newURL+="<a href=\"javascript:showModalDialog("+strBuf.toString()+");\"><img style=\"border:0; padding: 2px;\" src=\"../common/images/iconActionAppend.png\" alt=\""+sTipAddFiles+"\" title=\""+sTipAddFiles+"\"></img></a>";

                       }
                       else
                       {
                    	   newURL+="<img style=\"border:0; padding: 2px;\" src=\"../common/images/iconActionAppend.png\" alt=\""+sTipAddFiles+"\" title=\""+sTipAddFiles+"\"></img>";

                       }
              }
              if (newURL.length() == 0)
                newURL+="&#160;";
            }
            else
            {
            	newURL+="&#160;";
            }

            vActions.add(newURL);
        }
        } catch(Exception ex){
            ex.printStackTrace();
      throw ex;
    }
    finally{
     return vActions;
    }
  }

  // The following method are called internally in the main methods.

 /**
   * CheckActionBarLinkAccess - global method for checking the conditions for top and bottom action bar links
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the objectId
   * @return boolean type
   * @throws Exception if the operation fails
   * @since V10 Pacth1
   */
  public static boolean CheckActionBarLinkAccess(Context context, String args[]) throws Exception
  {
    HashMap programMap         = (HashMap) JPO.unpackArgs(args);
    String objectId            = (String) programMap.get("objectId");

    String  workspaceId = null;
    boolean boolChecking = false;
    boolean hasProject = false;
    DomainObject BaseObject   = DomainObject.newInstance(context,objectId);
    BaseObject.open(context);
    String baseType = BaseObject.getInfo(context, BaseObject.SELECT_TYPE);
    String routeState = BaseObject.getInfo(context, BaseObject.SELECT_CURRENT);
    String stateComplete=FrameworkUtil.lookupStateName(context,DomainConstants.POLICY_ROUTE,"state_Complete");

    // if workspaceId is not passed, get the workspaceId
    if (baseType.equals(DomainConstants.TYPE_WORKSPACE_VAULT)) {
       workspaceId = getWorkspaceId(context,objectId);
    }
    if(baseType.equals(DomainConstants.TYPE_ROUTE)) {
      String selectWorkspaceId = "from["+DomainConstants.RELATIONSHIP_MEMBER_ROUTE+"].to.to["+DomainConstants.RELATIONSHIP_PROJECT_MEMBERS+"].from.id";
      workspaceId = BaseObject.getInfo(context,selectWorkspaceId);
    }
    if(!(null == workspaceId || "#DENIED!".equals(workspaceId) || "".equals(workspaceId) || "null".equals(workspaceId))) {
      hasProject = true;
    }
    if(hasProject) {
      if(!routeState.equalsIgnoreCase(stateComplete)){
         boolChecking = true;
      }
    }
   return boolChecking;
  }


  // The following methods are called for Topaction bars.display.
  /**
   * hasWorkspaceAndFileUpload - to check whether to display the Add Workspace Content and Upload External File.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the objectId
   * @return boolean type
   * @throws Exception if the operation fails
   * @since Team 10-0-1-0
   */
  public static boolean hasWorkspaceAndFileUpload(Context context, String args[]) throws Exception
  {
    HashMap programMap         = (HashMap) JPO.unpackArgs(args);
    String objectId            = (String) programMap.get("objectId");
    boolean boolChecking = false;
    DomainObject BaseObject   = DomainObject.newInstance(context,objectId);
    // REmoved the base object open
    // get the route object name for the header display.
    Access access = BaseObject.getAccessMask(context);
    if(CheckActionBarLinkAccess(context,args))
    {
      if(AccessUtil.hasAddAccess(access)){
        boolChecking = true;
      }
    }
   return boolChecking;
  }

  public static boolean hasWorkspaceAndFileUploadForOwner(Context context, String args[]) throws Exception
  {
    HashMap programMap         = (HashMap) JPO.unpackArgs(args);
    String objectId            = (String) programMap.get("objectId");
    boolean boolChecking = false;
    DomainObject BaseObject   = DomainObject.newInstance(context,objectId);
    // REmoved the base object open
    // get the route object name for the header display.
    String loggedInRole = PersonUtil.getDefaultSecurityContext(context);
    String roleOwner =   PropertyUtil.getSchemaProperty(context,"role_VPLMProjectAdministrator");
    String roleAdmin =   PropertyUtil.getSchemaProperty(context,"role_VPLMAdmin");
    if (loggedInRole.contains(roleOwner) || loggedInRole.contains(roleAdmin))  {
           return false;         
    }
    Access access = BaseObject.getAccessMask(context);
    if(CheckActionBarLinkAccess(context,args))
    {
      if(AccessUtil.hasAddAccess(access)){
        boolChecking = true;
      }
    }
   return boolChecking;
  }

  // added for bug 348885

   /**
   * getFolderOption-to check whether the user has create folder option is true or not.
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the objectId
   * @return boolean type
   * @since V6R2009x
   */
  public static boolean getFolderOption(Context context,String args[])
  {

    try
  {
        HashMap programMap;
        programMap      = (HashMap) JPO.unpackArgs(args);
        String objectId   = (String) programMap.get("objectId");
        String  workspaceId = getWorkspaceId(context,objectId);
        String contextuser  = context.getUser();
        String wid      = "expand bus"+" "+workspaceId+" "+"from rel 'Project Members' select bus id dump | where 'relationship[Project Members].to.relationship[Project Membership].from.name=="+"\""+ contextuser+"\" '";
        String string   =   MqlUtil.mqlCommand(context,wid);
        StringList stringList=new StringList(7);
        stringList        = FrameworkUtil.split(string,"|");
        if(stringList.size() > 0)
        {
          String loggedUserId =(String) stringList.get(6);
          String createFValue ="print bus"+" "+loggedUserId+" "+"select attribute[Create Folder]  dump |";
          String createFolderValue=MqlUtil.mqlCommand(context,createFValue);


          if(createFolderValue.equalsIgnoreCase("Yes"))
          {
            return true;
          }
        }
    }
  catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }

    return false;
  }

//End of 348885
  /**
   * isVCCommandsEnabled - This method is used to determine if the VC Commands in the Attachments
   *                       Summray can be enabled or not.
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the following input arguments:
   *        0 - object Id
   * @return boolean
   * @throws Exception if the operation fails
   * @since Sourcing 11-0
   */
  public boolean isVCCommandsEnabled(Context context, String[] args)
    throws Exception
  {
    boolean access = false;
    try
    {
      emxVCDocumentUI_mxJPO vcDocUI = new emxVCDocumentUI_mxJPO(context, null);
      boolean dsServer = vcDocUI.hasDesignSyncServer(context,args);
      access = dsServer && hasWorkspaceAndFileUpload(context,args);
    }
    catch (Exception ex)
    {
      throw ex;
    }
    finally
    {
      return access;
    }
  }
  /**
   * hasMultipleFiles - to check whether to display the Multiple Upload File and Multiple Update File.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the objectId
   * @return boolean type
   * @throws Exception if the operation fails
   * @since Team 10-0-1-0
   */
  public static boolean hasMultipleFiles(Context context, String args[]) throws Exception
  {
    boolean boolChecking = false;
    HashMap programMap         = (HashMap) JPO.unpackArgs(args);
    String objectId            = (String) programMap.get("objectId");
    DomainObject BaseObject   = DomainObject.newInstance(context,objectId);
    String baseType = BaseObject.getInfo(context, BaseObject.SELECT_TYPE);
    Access access = BaseObject.getAccessMask(context);
    if(CheckActionBarLinkAccess(context,args))
    {
      if(AccessUtil.hasAddAccess(access)){
        if(!baseType.equals(DomainConstants.TYPE_ROUTE)){
          boolChecking= true;
        }
      }
    }
   return boolChecking;
  }

  /**
   * isJTEnabled - to check whether to display the Link UploadExternalJTFile.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the objectId
   * @return boolean type
   * @throws Exception if the operation fails
   * @since Team 10-0-1-0
   */
  public static boolean isJTEnabled(Context context, String args[]) throws Exception
  {
    boolean boolChecking = false;
    HashMap programMap         = (HashMap) JPO.unpackArgs(args);
    String objectId            = (String) programMap.get("objectId");
    DomainObject BaseObject   = DomainObject.newInstance(context,objectId);
    String baseType = BaseObject.getInfo(context, BaseObject.SELECT_TYPE);
    String strEAIVismarkViewerEnabled=EnoviaResourceBundle.getProperty(context,"emxTeamCentral.EAIVismarkViewerEnabled");
    Access access = BaseObject.getAccessMask(context);
    if(CheckActionBarLinkAccess(context,args))
    {
      if(AccessUtil.hasAddAccess(access)){
        if( strEAIVismarkViewerEnabled != null && "true".equals(strEAIVismarkViewerEnabled)){
          boolChecking= true;
        }
      }
    }
   return boolChecking;
  }

  /**
   * hasRemoveContent - to check whether to display the Link Remove Selected.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the objectId
   * @return boolean type
   * @throws Exception if the operation fails
   * @since Team 10-0-1-0
   */
  public static boolean hasRemoveContent(Context context, String args[]) throws Exception
  {
      HashMap programMap         = (HashMap) JPO.unpackArgs(args);
      String objectId            = (String) programMap.get("objectId");
      com.matrixone.apps.common.Person person = (com.matrixone.apps.common.Person) DomainObject.newInstance(context, DomainConstants.TYPE_PERSON);
      boolean boolChecking = false;
      DomainObject BaseObject   = DomainObject.newInstance(context,objectId);
      BaseObject.open(context); // Need to open and close the  Domain Object as we need o get the owner Name.
      String routeState = BaseObject.getInfo(context, BaseObject.SELECT_CURRENT);
      String sOwner     = BaseObject.getOwner(context).getName();
      BaseObject.close(context);
      person = person.getPerson(context);
      String strPerson = person.getName();
      String stateComplete=FrameworkUtil.lookupStateName(context,DomainConstants.POLICY_ROUTE,"state_Complete");
      Access access = BaseObject.getAccessMask(context);
      if(sOwner.equals(strPerson) || (!routeState.equalsIgnoreCase(stateComplete))) {
        if(AccessUtil.hasRemoveAccess(access)){
          boolChecking = true;
        }
      }
     return boolChecking;
  }

  /**
   * hasMoveContent - to check whether to display the Link Move Selected.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the objectId
   * @return boolean type
   * @throws Exception if the operation fails
   * @since Team 10-0-1-0
   */
  public static boolean hasMoveContent(Context context, String args[]) throws Exception
  {
      HashMap programMap         = (HashMap) JPO.unpackArgs(args);
      String objectId            = (String) programMap.get("objectId");
      boolean boolChecking = false;
      DomainObject BaseObject   = DomainObject.newInstance(context,objectId);
      String baseType = BaseObject.getInfo(context, BaseObject.SELECT_TYPE);
      if( hasRemoveContent(context,args) && baseType.equals(DomainConstants.TYPE_WORKSPACE_VAULT)) {
          boolChecking = true;
      }
     return boolChecking;
  }

  /**
   * hasEditBlockLifeCycle - to check whether to display the Link EditLifeCycle Blocks.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the objectId
   * @return boolean type
   * @throws Exception if the operation fails
   * @since Team 10-0-1-0
   */
  public static boolean hasEditBlockLifeCycle(Context context, String args[]) throws Exception
  {
      HashMap programMap         = (HashMap) JPO.unpackArgs(args);
      String objectId            = (String) programMap.get("objectId");
      boolean boolChecking = false;
      DomainObject BaseObject   = DomainObject.newInstance(context,objectId);
      String baseType = BaseObject.getInfo(context, BaseObject.SELECT_TYPE);
      if( CheckActionBarLinkAccess(context,args) && baseType.equals(DomainConstants.TYPE_ROUTE) && (context.getUser().equals(BaseObject.getInfo(context, BaseObject.SELECT_OWNER)))) {
          boolChecking = true;
      }
     return boolChecking;
  }

  /**
   * hasRouteSelected - to check whether to display the Link RouteSelected .
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the objectId
   * @return boolean type
   * @throws Exception if the operation fails
   * @since Team 10-0-1-0
   */

  public static boolean hasRouteSelected(Context context, String args[]) throws Exception
  {
      HashMap programMap         = (HashMap) JPO.unpackArgs(args);
      String objectId            = (String) programMap.get("objectId");
      com.matrixone.apps.common.Person person = (com.matrixone.apps.common.Person) DomainObject.newInstance(context, DomainConstants.TYPE_PERSON);
      person = person.getPerson(context);
      boolean boolChecking = false;
      String workspaceId =null;
      DomainObject BaseObject   = DomainObject.newInstance(context,objectId);
      String baseType = BaseObject.getInfo(context, BaseObject.SELECT_TYPE);
      if (baseType.equals(DomainConstants.TYPE_WORKSPACE_VAULT)) {
         workspaceId = getWorkspaceId(context,objectId);
      }
      if(baseType.equals(DomainConstants.TYPE_ROUTE)) {
        String selectWorkspaceId = "from["+DomainConstants.RELATIONSHIP_MEMBER_ROUTE+"].to.to["+DomainConstants.RELATIONSHIP_PROJECT_MEMBERS+"].from.id";
        workspaceId = BaseObject.getInfo(context,selectWorkspaceId);
      }
      String sCreateRoute = "";
      StringList objectSelects = new StringList();
      objectSelects.add("attribute["+DomainConstants.ATTRIBUTE_CREATE_ROUTE+"]");
      String objectWhere = "(to["+BaseObject.RELATIONSHIP_PROJECT_MEMBERS+"].from.id == '"+workspaceId+"')";
      MapList mapList = person.getRelatedObjects(context,
                                         BaseObject.RELATIONSHIP_PROJECT_MEMBERSHIP,
                                         BaseObject.TYPE_PROJECT_MEMBER,
                                         false,
                                         true,
                                         (short)1,
                                         objectSelects,
                                         null,
                                         objectWhere,
                                         null,
                                         "",
                                         "",
                                         null);
      Iterator memberItr  = mapList.iterator();
      Map map=null;
      if(memberItr.hasNext()){
        map =(Map)memberItr.next();
        sCreateRoute = (String)map.get("attribute["+DomainConstants.ATTRIBUTE_CREATE_ROUTE+"]");
      }

      if( CheckActionBarLinkAccess(context,args) && baseType.equals(DomainConstants.TYPE_WORKSPACE_VAULT) && "Yes".equals(sCreateRoute)) {
          boolChecking = true;
      }

    return boolChecking;
  }

  /**
   * getJTAssemblyType - this Method will be called Internally by Other methods to get the Type of JT File added  .
   * @param context the eMatrix <code>Context</code> object
   * @param objectId  The id of the Document
   * @return Integer type
   * @throws MatrixException if the operation fails
   * @since Team 10-0-1-0
   */

 static public Integer getJTAssemblyType(matrix.db.Context context, String objectId) throws Exception,MatrixException
  {
    Integer PARENT = new Integer(1);
    Integer CHILD  = new Integer(2);
    Integer ORPHAN   = new Integer(3);
    Integer assemblyType = ORPHAN;
    try{
      String relDocumentStructure = PropertyUtil.getSchemaProperty(context,"relationship_DocumentStructure" );
      SelectList busSelects = new SelectList();
      busSelects.add(DomainConstants.SELECT_ID);
      DomainObject object = new DomainObject(objectId);
      DomainObject objectFrom = null;
      DomainObject objectTo = null;
      Map objectToMap =object.getRelatedObject(context,
                                               relDocumentStructure,
                                               true,
                                               busSelects,null);

      Map objectFromMap =object.getRelatedObject(context,
                                               relDocumentStructure,
                                               false,
                                               busSelects,null);
      if(objectToMap != null)
      {
        String toId= (String)objectToMap.get(DomainConstants.SELECT_ID);
        if(toId != null && !toId.equals(""))
        {
          objectTo = new DomainObject(toId);
        }
      }

      if(objectFromMap != null)
      {
        String fromId= (String)objectFromMap.get(DomainConstants.SELECT_ID);
        if(fromId != null && !fromId.equals(""))
        {
          objectFrom =new DomainObject(fromId);
        }
      }

      if(objectTo != null ) {
        assemblyType = (objectTo == null)? ORPHAN : PARENT;
      } else {
        assemblyType = (objectFrom == null)? ORPHAN : CHILD;
      }

    } catch(Exception e){throw e;}

    finally{
      return assemblyType;
    }
  }


  /**
   * getWorkspaceId - this Method will be called Internally by Other methods to get the Project Id by passing the Document Id.
   * @param context the eMatrix <code>Context</code> object
   * @param folderId  The Object id of the Document
   * @return String type
   * @throws MatrixException if the operation fails
   * @since Team 10-0-1-0
   */

public static String getWorkspaceId(Context context, String  folderId)
{
      String workspaceId="";
      try{
        String strProjectVault = DomainConstants.RELATIONSHIP_PROJECT_VAULTS;
        String strSubVaultsRel = DomainConstants.RELATIONSHIP_SUB_VAULTS;
        String strProjectType  = DomainConstants.TYPE_PROJECT;
        String strProjectVaultType  = DomainConstants.TYPE_PROJECT_VAULT;
        //com.matrixone.framework.beans.DomainObject domainObject = new com.matrixone.framework.beans.DomainObject();
        DomainObject domainObject = DomainObject.newInstance(context);
        domainObject.setId(folderId);
        Pattern relPattern  = new Pattern(strProjectVault);
        relPattern.addPattern(strSubVaultsRel);
        Pattern typePattern = new Pattern(strProjectType);
        typePattern.addPattern(strProjectVaultType);

        Pattern includeTypePattern = new Pattern(strProjectType);

        StringList objSelects = new StringList();
        objSelects.addElement(domainObject.SELECT_ID);
        //need to include Type as a selectable if we need to filter by Type
        objSelects.addElement(domainObject.SELECT_TYPE);
        MapList mapList = domainObject.getRelatedObjects(context,
                                               relPattern.getPattern(),
                                               typePattern.getPattern(),
                                               objSelects,
                                               null,
                                               true,
                                               false,
                                               (short)0,
                                               "",
                                               "",
                                               includeTypePattern,
                                               null,
                                               null);

        Iterator mapItr = mapList.iterator();
        while(mapItr.hasNext())
        {
            Map map = (Map)mapItr.next();
            workspaceId = (String) map.get(domainObject.SELECT_ID);
        }
      }catch(Exception e){}

      return workspaceId;
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
     * Get  for the specified criteria
     * @param context the eMatrix <code>Context</code> object.
     * @param args contains a Map with the following entries:
     *   selType - a String containing the type(s) to search for
     *   txtName - a String containing a Name pattern to search for
     *   txtRev - a String containing a Revision pattern to search for
     *   txtOwner - a String containing an Owner pattern to search for
     *   txtWhere - a URL encoded String containing a Matrix where clause.
     *   txtOrginator - a String containing an Originator pattern to search for
     *   txtSearch - a String containing a text pattern to Search for.
     *   txtFormat - a String containing a format pattern to Search for.
     *   setRadio  - a String containing the Collection name to open.
     * @return MapList containing search result.
     * @exception Exception if the operation fails.
     */
     public MapList getSearchResult(Context context , String[] args)
                       throws Exception
     {
       HashMap paramMap = (HashMap)JPO.unpackArgs(args);

       //Retrieve Search criteria
       String selType          = (String)paramMap.get("selType");
       String txtName          = (String)paramMap.get("txtName");
       String txtRev           = (String)paramMap.get("txtRev");
       //Code modified for bug ID : 363745 put personName param in place of txtOwner
       String txtOwner         = (String)paramMap.get("personName");




       String txtWhere         = (String)paramMap.get("txtWhere");
       String txtOriginator    = (String)paramMap.get("txtOriginator");
       String txtDescription   = (String)paramMap.get("txtDesc");
       String txtSearch        = (String)paramMap.get("txtSearch");
       String txtFormat        = (String)paramMap.get("txtFormat");
       String languageStr      = (String)paramMap.get("languageStr");
       String sSetName         = (String)paramMap.get("setRadio");
       String chkLastRevision  = (String)paramMap.get("chkLastRevision");
       String slkupOriginator = PropertyUtil.getSchemaProperty(context,"attribute_Originator");
       String target       = "";
       String sAnd         = "&&";
       String sOr          = "||";
       char chDblQuotes    = '\"';

   /**************************Vault Code Start*****************************/
   // Get the user's vault option & call corresponding methods to get
   // the vault's.

         String txtVault   ="";
         String strVaults="";
         StringList strListVaults=new StringList();

         String txtVaultOption = (String)paramMap.get("vaultOption");
         if(txtVaultOption==null) {
           txtVaultOption="";
         }
         if(txtVaultOption.equals("selected"))
            txtVaultOption=(String)paramMap.get("selVaults");
         String vaultAwarenessString = (String)paramMap.get("vaultAwarenessString");

         if(vaultAwarenessString.equalsIgnoreCase("true")){

           if(txtVaultOption.equals("ALL_VAULTS") || txtVaultOption.equals(""))
           {
             strListVaults = com.matrixone.apps.common.Person.getCollaborationPartnerVaults(context,null);
             StringItr strItr = new StringItr(strListVaults);
             if(strItr.next()){
               strVaults =strItr.obj().trim();
             }
             while(strItr.next())
             {
               strVaults += "," + strItr.obj().trim();
             }
             txtVault = strVaults;
           }
           else if(txtVaultOption.equals("LOCAL_VAULTS"))
           {
             com.matrixone.apps.common.Person person = com.matrixone.apps.common.Person.getPerson(context);
             Company company = person.getCompany(context);
             txtVault = company.getLocalVaults(context);
           }
           else if (txtVaultOption.equals("DEFAULT_VAULT"))
           {
             txtVault = context.getVault().getName();
           }
           else
           {
             txtVault = txtVaultOption;

           }
         }
         else
         {
           if(txtVaultOption.equals("ALL_VAULTS") || txtVaultOption.equals(""))
           {
             // get ALL vaults
             Iterator mapItr = VaultUtil.getVaults(context).iterator();
             if(mapItr.hasNext())
             {
               txtVault =(String)((Map)mapItr.next()).get("name");

               while (mapItr.hasNext())
               {
                 Map map = (Map)mapItr.next();
                 txtVault += "," + (String)map.get("name");
               }
             }

           }
           else if(txtVaultOption.equals("LOCAL_VAULTS"))
           {
             // get All Local vaults
             strListVaults = VaultUtil.getLocalVaults(context);
             StringItr strItr = new StringItr(strListVaults);
             if(strItr.next()){
               strVaults =strItr.obj().trim();
             }
             while(strItr.next())
             {
               strVaults += "," + strItr.obj().trim();
             }
             txtVault = strVaults;
           }
           else if (txtVaultOption.equals("DEFAULT_VAULT"))
           {
             txtVault = context.getVault().getName();
           }
           else
           {
             txtVault = txtVaultOption;
           }
         }
         //trimming
         txtVault = txtVault.trim();

     /**************************Vault Code End*******************************/

       if (sSetName == null || sSetName.equals("null") || sSetName.equals("")){
         sSetName = "";
       }

       String savedQueryName   = (String)paramMap.get("savedQueryName");
       if (savedQueryName == null || savedQueryName.equals("null") || savedQueryName.equals("")){
         savedQueryName="";
       }

       String queryLimit = (String)paramMap.get("queryLimit");
       if (queryLimit == null || queryLimit.equals("null") || queryLimit.equals("")){
         queryLimit = "0";
       }

       if (txtName == null || txtName.equalsIgnoreCase("null") || txtName.length() <= 0){
         txtName = "*";
       }
       if (txtRev == null || txtRev.equalsIgnoreCase("null") || txtRev.length() <= 0){
         txtRev = "*";
       }
       if (txtOwner == null || txtOwner.equalsIgnoreCase("null") || txtOwner.length() <= 0){
         txtOwner = "*";
       }
       if (txtDescription != null && !txtDescription.equalsIgnoreCase("null") && txtDescription.equals("*")){
         txtDescription = "";
       }
       if (txtOriginator != null && !txtOriginator.equalsIgnoreCase("null") && txtOriginator.equals("*")){
         txtOriginator = "";
       }
       if (txtWhere == null || txtWhere.equalsIgnoreCase("null"))
       {
         txtWhere = "";
       }
       else
       {
         txtWhere = com.matrixone.apps.domain.util.XSSUtil.decodeFromURL(txtWhere);
       }
       String sWhereExp = txtWhere;

       if (!(txtOriginator == null || txtOriginator.equalsIgnoreCase("null") || txtOriginator.length() <= 0 )) {
         String sOriginatorQuery = "attribute[" + slkupOriginator + "] ~~ " + chDblQuotes + txtOriginator + chDblQuotes;
         if (sWhereExp == null || sWhereExp.equalsIgnoreCase("null") || sWhereExp.length()<=0 ){
           sWhereExp = sOriginatorQuery;
         } else {
           sWhereExp += sAnd + " " + sOriginatorQuery;
         }
       }

       if (!(txtDescription == null || txtDescription.equalsIgnoreCase("null") || txtDescription.length() <= 0 )) {
         String sDescQuery = "description ~~ " + chDblQuotes + txtDescription + chDblQuotes;
         if (sWhereExp == null || sWhereExp.equalsIgnoreCase("null") || sWhereExp.length()<=0 ){
           sWhereExp = sDescQuery;
         } else {
           sWhereExp += sAnd + " " + sDescQuery;
         }
       }

       if (selType.equals(DomainConstants.TYPE_DOCUMENT))
       {
         String verAttr = PropertyUtil.getSchemaProperty(context,"attribute_IsVersionObject");
         String docWhere = "attribute[" + verAttr + "] != True";
         if (sWhereExp == null ||
             sWhereExp.equalsIgnoreCase("null") ||
             sWhereExp.length()<=0 )
         {
           sWhereExp = docWhere;
         }
         else
         {
           sWhereExp += sAnd + " " + docWhere;
         }
       }
  // This code need to be taken out once Sourcing X+3 Migration is Completed     -SC
  // Start of Pre Migration Code -SC
    else if(selType.equals(DomainObject.TYPE_REQUEST_TO_SUPPLIER)){
      if ((sWhereExp == null) || (sWhereExp.equalsIgnoreCase("null")) || (sWhereExp.length()<=0 ))
      {
         sWhereExp = " (!to[" + DomainObject.RELATIONSHIP_COMPANY_RFQ + "]) ";
      }
      else
      {
         sWhereExp += sAnd + " " + " (!to[" + DomainObject.RELATIONSHIP_COMPANY_RFQ + "]) ";
      }
    } else if(selType.equals(DomainObject.TYPE_PACKAGE)){
      if ((sWhereExp == null) || (sWhereExp.equalsIgnoreCase("null")) || (sWhereExp.length()<=0 ))
      {
        sWhereExp = " (!to[" + DomainObject.RELATIONSHIP_COMPANY_PACKAGE + "]) ";
      }
      else
      {
        sWhereExp += sAnd + " " + " (!to[" + DomainObject.RELATIONSHIP_COMPANY_PACKAGE + "]) ";
      }
    } else if(selType.equals(DomainObject.TYPE_RTS_QUOTATION)){
      if ((sWhereExp == null) || (sWhereExp.equalsIgnoreCase("null")) || (sWhereExp.length()<=0 ))
      {
        sWhereExp = " (to[" + DomainObject.RELATIONSHIP_RTS_QUOTATION + "].from.to["+ DomainObject.RELATIONSHIP_COMPANY_RFQ +"]!='#DENIED!') && (!(to[" + DomainObject.RELATIONSHIP_RTS_QUOTATION + "].from.to["+ DomainObject.RELATIONSHIP_COMPANY_RFQ +"]))";
      }
      else
      {
        sWhereExp += sAnd + " " + "(to[" + DomainObject.RELATIONSHIP_RTS_QUOTATION + "].from.to["+ DomainObject.RELATIONSHIP_COMPANY_RFQ +"]!='#DENIED!') && (!(to[" + DomainObject.RELATIONSHIP_RTS_QUOTATION + "].from.to["+ DomainObject.RELATIONSHIP_COMPANY_RFQ +"]))";
      }
    }
  // End of Pre Migration Code - SC

       SelectList resultSelects = new SelectList(7);
       resultSelects.add(DomainObject.SELECT_ID);
       resultSelects.add(DomainObject.SELECT_TYPE);
       resultSelects.add(DomainObject.SELECT_NAME);
       resultSelects.add(DomainObject.SELECT_REVISION);
       resultSelects.add(DomainObject.SELECT_DESCRIPTION);
       resultSelects.add(DomainObject.SELECT_CURRENT);
       resultSelects.add(DomainObject.SELECT_POLICY);


       MapList totalresultList = null;

       if(chkLastRevision != null){
           if (sWhereExp != null && sWhereExp.trim().length()!= 0 )
               sWhereExp += sAnd + " ";
           sWhereExp += "(revision == last)";
       }

       // Check for a set name and use that set for results if present
       if (sSetName.equals(""))
       {
         if (savedQueryName.equals(""))
         {
           totalresultList = DomainObject.findObjects(context,
                                                      selType,
                                                      txtName,
                                                      txtRev,
                                                      txtOwner,
                                                      txtVault,
                                                      sWhereExp,
                                                      null,
                                                      true,
                                                      resultSelects,
                                                      Short.parseShort(queryLimit),
                                                      txtFormat,
                                                      txtSearch);
         }
         else
         {
           matrix.db.Query query = new matrix.db.Query(savedQueryName);
       try
       {
         ContextUtil.startTransaction(context,false);
         query.open(context);
         query.setObjectLimit(Short.parseShort(queryLimit));
               /*totalresultList = FrameworkUtil.toMapList(query.select(context,resultSelects));*/
         totalresultList = FrameworkUtil.toMapList(query.getIterator(context,resultSelects,Short.parseShort(queryLimit)), FrameworkUtil.MULTI_VALUE_LIST);
         query.close(context);
         ContextUtil.commitTransaction(context);
       }
       catch(Exception ex)
       {
        ContextUtil.abortTransaction(context);
        throw new Exception (ex);
       }
         }
       }
       else
       {
           totalresultList = SetUtil.getMembers(context,
                                                sSetName,
                                                resultSelects);
       }
       return totalresultList;
     }

  /**
   * getName- Will get the name/Title (for Folder )
   *       Will be called in the Name Column
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the following input arguments:
   *        0 - objectList MapList
   * @return Object of type Vector
   * @throws Exception if the operation fails
   * @since V10-5
   */
  public Vector getName(Context context, String[] args)
    throws Exception
  {
      Vector vName = new Vector();
      try
      {
          HashMap programMap = (HashMap) JPO.unpackArgs(args);
          MapList objectList = (MapList)programMap.get("objectList");

          if(objectList == null || objectList.size() <= 0)
          {
              return vName;
          }

          Map paramList = (Map)programMap.get("paramList");

          boolean isprinterFriendly = false;
          if(paramList.get("reportFormat") != null)
          {
             isprinterFriendly = true;
          }
          boolean isCSVExport = false;
          if(paramList.get("reportFormat") != null && "CSV".equalsIgnoreCase((String)paramList.get("reportFormat") ))
          {
            isCSVExport = true;
          }

          String jsTreeID = (String)paramList.get("jsTreeID");
          String objectId = (String)paramList.get("objectId"); //folder id
          String workspaceId = (String)paramList.get("workspaceId");
          StringBuffer sbNextURL= null;
          String target= null;
          String sRouteCheck= "false";
          String folderID= null;
          //instializing Global Variables
          strFileFormat       = null;
          Title = null;
          routeId = null;
          hasWorkspace= false;
          Map objectMap = null;
          String strFromRoute= "No";
          StringBuffer sBuff= null;
          int objectListSize = 0 ;

          //which holds the image name to Be displayed.
          String objectIcon = "";
          String strTreeName = "";

          //Building the domain Object instance
          DomainObject domainObject = DomainObject.newInstance(context,objectId);
          String sTypeName = domainObject.getInfo(context, DomainObject.SELECT_TYPE);
          boolean bIsTypeRoute  = sTypeName.equals(DomainObject.TYPE_ROUTE);
          boolean bIsTypeWorkspaceVault = sTypeName.equals(DomainObject.TYPE_WORKSPACE_VAULT);

          // If workspace Id is null or "" then get the workspace Id using
          // getWorkspaceid for a Folder or using a query if it is a Route
          if ((null == workspaceId)            ||
              ("".equals(workspaceId))         ||
              ("#DENIED!".equals(workspaceId)) ||
              ("null".equals(workspaceId)))
          {
            if(bIsTypeWorkspaceVault)
            {
              workspaceId = getWorkspaceId(context,objectId);
            }
            else if(bIsTypeRoute)
            {
              StringBuffer select_workspaceId = new StringBuffer(64);
              select_workspaceId.append(sFromBracket);
              select_workspaceId.append(DomainObject.RELATIONSHIP_MEMBER_ROUTE);
              select_workspaceId.append("sBracketToToBracket");
              select_workspaceId.append(DomainObject.RELATIONSHIP_PROJECT_MEMBERS);
              select_workspaceId.append(sBracketFromId);
              workspaceId = domainObject.getInfo(context,select_workspaceId.toString());
            }
          }

          boolean bHasWorkspace             = false;

          if ((null == workspaceId)            ||
              ("".equals(workspaceId))         ||
              ("#DENIED!".equals(workspaceId)) ||
              ("null".equals(workspaceId)))
          {
            workspaceId  ="";
          }
          else
          {
            bHasWorkspace = true;
          }


          Iterator objectListItr = null;
          if(bIsTypeRoute) {
		  // build select params if it is of RouteType
          StringList selectTypeStmts = new StringList();
          selectTypeStmts.add(DomainConstants.SELECT_ID);
          selectTypeStmts.add(DomainConstants.SELECT_NAME);
          selectTypeStmts.add(DomainConstants.SELECT_TYPE);
          selectTypeStmts.add(CommonDocument.SELECT_FILE_FORMAT);
            selectTypeStmts.add(sbSelFolId.toString());

          //retrieve all folder content ids
          String oidsArray[] = new String[objectList.size()];
          for (int i = 0; i < objectList.size(); i++)
          {
              try
              {
                  oidsArray[i] = (String)((HashMap)objectList.get(i)).get("id");
              } catch (Exception ex)
              {
                  oidsArray[i] = (String)((Hashtable)objectList.get(i)).get("id");
              }
          }

          MapList objList = DomainObject.getInfo(context, oidsArray, selectTypeStmts);
          objectListItr = objList.iterator();
          }else{
        	  objectListItr = objectList.iterator();
          }

          domainObject = new DomainObject();
          Object objFileFormat = new Object();
          while(objectListItr.hasNext())
          {
        	  String newURL = "";
            objectMap = (Map)objectListItr.next();

            // StringBuffer to hold the entire string that needs to be
            // displayed in the Name Column
            sBuff= new StringBuffer(128);

            //StringBuffer to hold the Href URL.
            sbNextURL= new StringBuffer(64);

            strTreeName = "";
            Title = (String) objectMap.get(DomainConstants.SELECT_NAME);
            // Check for multiple files.
            String documentId = (String)objectMap.get(DomainConstants.SELECT_ID);


            folderID= objectId;//(String)objectMap.get("objectId");

            //Added for bug 373244
        	  objFileFormat =  objectMap.get(CommonDocument.SELECT_FILE_FORMAT);
        	  if(objFileFormat instanceof String){
        		  strFileFormat = (String)objFileFormat;
        	  }else{
        		  strFileFormat = ((StringList)objFileFormat).elementAt(0).toString();
        	  }
        	              
        	  if(UIUtil.isNullOrEmpty(strFileFormat)){
        		  domainObject = DomainObject.newInstance(context,documentId);
        		  strFileFormat=CommonDocument.getFileFormat(context,domainObject);
        	  }

              //Added for bug 373244        	  
            //strFileFormat=(String)objectMap.get("strFileFormat");
            //Commented for bug 373244
            // strFileFormat  = document.getDefaultFormat(context);
            //gettting all the formats that the Document Supports.


            docType = (String) objectMap.get(DomainConstants.SELECT_TYPE);
            //bIsTypeRoute = ((Boolean)objectMap.get("bIsTypeRoute")).booleanValue();

            boolean boolRouted = false;
            if ((docRouteId != null) &&
                (!"".equals(docRouteId)) &&
                (!"null".equals(docRouteId)))
            {
              boolRouted = true;
            }
            if ( boolRouted )
            {
              sRouteCheck = "true";
            }
            if (bIsTypeRoute)
            {
              strFromRoute  = "No";
              if (bIsTypeRoute)
              {
                strFromRoute  = "Yes"; // to know whether content summary was
                                       // called from route/folder
                try
                {
                  folderID = (String)objectMap.get(sbSelFolId.toString());
                }
                catch(Exception e) { }
              }

              if(folderID == null)
              {
                folderID = "";
              }
            } // eof if (bIsTypeRoute)


            // Get the image of the Type. If a specific naming convention
            // of the image is followed , the following code can be generalized.
            //if (docType.equals(DomainConstants.TYPE_DOCUMENT))

            // Get the image of the Type. If a specific naming convention
            // of the image is followed , the following code can be generalized.
            if ("TRUE".equalsIgnoreCase((String)objectMap.get("type.kindof["+DomainConstants.TYPE_DOCUMENT+"]")))
            {
              objectIcon = "../teamcentral/images/iconSmallFile.gif";
                strTreeName = "TMCtype_Document";
            }
            else if (docType.equals(DomainConstants.TYPE_PACKAGE) )
            {
              objectIcon = "../common/images/iconSmallPackage.gif";
              strTreeName = "TMCtype_Package";
            }
            else if (docType.equals(DomainConstants.TYPE_RFQ) )
            {
              objectIcon = "../common/images/iconSmallRTS.gif";
              strTreeName = "TMCtype_RequestToSupplier";
            }
            else if (docType.equals(DomainConstants.TYPE_RTS_QUOTATION) )
            {
              objectIcon = "../common/images/iconSmallQuotation.gif";
              strTreeName = "TMCtype_RTSQuotation";
            }
            else if (docType.equals(DomainConstants.TYPE_PART) )
            {
              objectIcon = "../common/images/iconSmallPart.gif";
              strTreeName = "";
            }
            else if (docType.equals(DomainConstants.TYPE_PT_ARCHIVE))
            {
              objectIcon = "../common/images/"+getTypeIconProperty(context,docType);
            }
            else
            {
//Bug No:302539 Dt:13-May-2005
                objectIcon = "../common/images/"+getParentTypeIconProperty(context,docType);
              //objectIcon = "../common/images/iconSmall"+removeSpace(docType)+".gif";
//Bug No:302539 Dt:13-May-2005
            }

              sbNextURL.append("../common/emxTree.jsp?objectId=");
              sbNextURL.append(XSSUtil.encodeForJavaScript(context, documentId));
              sbNextURL.append("&amp;mode=insert&amp;jsTreeID=");
              sbNextURL.append(XSSUtil.encodeForJavaScript(context, jsTreeID));
              sbNextURL.append("&amp;AppendParameters=true&amp;folderId=");
              sbNextURL.append(XSSUtil.encodeForJavaScript(context, folderID));
              sbNextURL.append("&amp;workspaceId=");
              sbNextURL.append(XSSUtil.encodeForJavaScript(context, workspaceId));
              sbNextURL.append("&amp;routecheck=");
              sbNextURL.append(XSSUtil.encodeForJavaScript(context, strFromRoute));
              sbNextURL.append("&amp;treeMenu=");
              sbNextURL.append(XSSUtil.encodeForJavaScript(context, strTreeName));
              // Added for AEF Bug 371943
              // In emxTree.jsp page this param gets looked and wont refresh the Tree Structure frame
              sbNextURL.append("&amp;treeRefreshMenu=");
              sbNextURL.append("refreshFalse");
            //367711 LVC appended rmbid and aloid for Document RMB menu to be displayed
              sbNextURL.append("&amp;rmbid=");
              sbNextURL.append(XSSUtil.encodeForJavaScript(context, documentId));
              sbNextURL.append("&amp;aloid=true");
              // Ended
              target  = " target=\"content\"";
           // }
            //hasWorkspace=((Boolean)objectMap.get("hasWorkspace")).booleanValue();
            // For a JT Files the image depends on the type of the image.
            if(strFileFormat!= null && strFileFormat.equals(formatJTStr))
            {
              int intValue = getJTAssemblyType( context, documentId).intValue();
              switch (intValue)
              {
                case 1:
                  objectIcon = "../common/images/iconSmall3dParent.gif";
                  break;
                case 2:
                  objectIcon = "../teamcentral/images/iconSmall3dSubAssembly.gif";
                  break;
                case 3:
                  objectIcon = "../common/images/iconSmall3dChild.gif";
                  break;
              }//eof Switch
            }// eof if

            if(bHasWorkspace)
            {

              if(!isprinterFriendly)
              {
            	  newURL+="<a href=\""+sbNextURL.toString()+"\" class=\"object\""+target+">";

              }
              if (isCSVExport){

                  newURL+=Title;
              }
              else {
            	  newURL+="<img src=\""+objectIcon+"\" border=\"0\" />"+XSSUtil.encodeForXML(context,Title);

              }
              if(!isprinterFriendly)
              {
            	  newURL+="</a>";

              }
              if (!isCSVExport){

              }
            }
            else
            {
            	newURL+="<img src=\""+objectIcon+"\" border=\"0\" />"+XSSUtil.encodeForXML(context,Title);

           }
            vName.add(newURL);
          }
       }
       catch(Exception e)
       {
          System.out.println("Error in getName()"+e);
          throw e;
       }

       return vName;
   }


  /**
    *  This function gets the Icon file name for any given type
    *  from the emxSystem.properties file
    *
    * @param context  the eMatrix <code>Context</code> object
    * @param type     object type name
    * @return         String - icon name
    * @since          TC 10-6
    */

    public static String getTypeIconProperty(Context context, String type)
    {
        String icon = DomainConstants.EMPTY_STRING;
        String typeRegistered = DomainConstants.EMPTY_STRING;

        try
        {
            if (type != null && type.length() > 0 )
            {
                String propertyKey = DomainConstants.EMPTY_STRING;
                String propertyKeyPrefix = "emxFramework.smallIcon.";
                String defaultPropertyKey = "emxFramework.smallIcon.defaultType";

                // Get the symbolic name for the type passed in
                typeRegistered = FrameworkUtil.getAliasForAdmin(context, "type", type, true);

                if (typeRegistered != null && typeRegistered.length() > 0 )
                {
                    propertyKey = propertyKeyPrefix + typeRegistered.trim();

                    try {
                        icon = EnoviaResourceBundle.getProperty(context,propertyKey);
                    } catch (Exception e1) {
                        icon = DomainConstants.EMPTY_STRING;
                    }
                    if( icon == null || icon.length() == 0 )
                    {
                        // Get the parent types' icon
                        BusinessType busType = new BusinessType(type, context.getVault());
                        if (busType != null)
                        {
                            String parentBusType = busType.getParent(context);
                            if (parentBusType != null)
                                icon = getTypeIconProperty(context, parentBusType);
                        }

                        // If no icons found, return a default icon for propery file.
                        if (icon == null || icon.trim().length() == 0 )
                            icon = EnoviaResourceBundle.getProperty(context,defaultPropertyKey);
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Error getting type icon name : " + ex.toString());
        }

        return icon;
    }

    public static String getParentTypeIconProperty(Context context, String type)
    {
        String icon = DomainConstants.EMPTY_STRING;
        String typeRegistered = DomainConstants.EMPTY_STRING;

        try
        {
            if (type != null && type.length() > 0 )
            {
                String propertyKey = DomainConstants.EMPTY_STRING;
                String propertyKeyPrefix = "emxFramework.smallIcon.";

                // Get the symbolic name for the type passed in
                typeRegistered = FrameworkUtil.getAliasForAdmin(context, "type", type, true);

                if (typeRegistered != null && typeRegistered.length() > 0 )
                {
                    propertyKey = propertyKeyPrefix + typeRegistered.trim();

                    try {
                        icon = EnoviaResourceBundle.getProperty(context,propertyKey);
                    } catch (Exception e1) {
                        icon = DomainConstants.EMPTY_STRING;
                    }
                    if( icon == null || icon.length() == 0 )
                    {
                        // Get the parent types' icon
                        BusinessType busType = new BusinessType(type, context.getVault());
                        if (busType != null)
                        {
                            String parentBusType = busType.getParent(context);
                            if (parentBusType != null)
                                icon = getTypeIconProperty(context, parentBusType);
                        }

                        // If no icons found, return a default icon for propery file.
                        if (icon == null || icon.trim().length() == 0 )
                            icon = "iconSmall"+removeSpace(type)+".gif";
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Error getting type icon name : " + ex.toString());
        }

        return icon;
    }


  /**
   * getFolderContentIds - This method is used to get content ids in the folder
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the following input arguments:
   *        objectId is in the paramList
   *
   * @return MapList
   * @throws Exception if the operation fails
   * @since Team 10-7-SP1
   */
      @com.matrixone.apps.framework.ui.ProgramCallable
     public MapList getFolderContentIds(Context context, String[] args)
       throws Exception
     {
         //Due to performance issue when used in a configurable table,
         //this method should return only folder content ids.

         MapList contentMapList = null;
         try
         {
             HashMap programMap        = (HashMap) JPO.unpackArgs(args);
             Map paramList = (Map)programMap.get("paramList");
             String workspaceId = (String)programMap.get("workspaceId");
             String expLevel = (String)programMap.get("expandLevel");
             if(UIUtil.isNullOrEmpty(expLevel))
   	  {
   		expLevel = "1";
             }
             boolean expandAll = "0".equals(expLevel) || "All".equalsIgnoreCase(expLevel)? true : false;


             // Object ID of the folder or route
             objectId = (String)programMap.get("objectId");

             //Building the domain Object instance
             DomainObject domainObject = DomainObject.newInstance(context,objectId);
             String sTypeName = domainObject.getInfo(context, "type");

             boolean bIsTypeRoute = sTypeName.equals(DomainObject.TYPE_ROUTE);
             boolean bIsTypeWorkspaceVault = sTypeName.equals(DomainObject.TYPE_WORKSPACE_VAULT);

             // build select params
             StringList selectTypeStmts = new StringList(1);
             selectTypeStmts.add(DomainConstants.SELECT_ID);
             selectTypeStmts.add(DomainConstants.SELECT_TYPE);
             selectTypeStmts.add(DomainConstants.SELECT_NAME);
             selectTypeStmts.add(DomainConstants.SELECT_OWNER);            
			 selectTypeStmts.add(DomainConstants.SELECT_CURRENT);
			 selectTypeStmts.add(CommonDocument.SELECT_FILE_FORMAT);
             selectTypeStmts.add("type.kindof["+DomainConstants.TYPE_DOCUMENT+"]");     
             StringList selectRelStmts = new StringList(1);
             selectRelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);
   //commented for bug 352726
    // String sObjWhere = "current.access[read] == TRUE && revision ~~ last";

            //added for bug 352726
             String sObjWhere = "current.access[read] == TRUE";


             if (bIsTypeRoute) //Route
             {
               contentMapList = domainObject.getRelatedObjects(context,
                                                             DomainObject.RELATIONSHIP_OBJECT_ROUTE,
                                                             "*",
                                                             selectTypeStmts,
                                                             null,
                                                             true,
                                                             false,
                                                             (short)1,
                                                             sObjWhere,
                                                             null,
                                                             null,
                                                             null,
                                                             null);
             }
             else // Workspace Vault (Folder)
             {
             	  String relationshipPattern = DomainObject.RELATIONSHIP_VAULTED_DOCUMENTS+","+ DomainConstants.RELATIONSHIP_VAULTED_OBJECTS_REV2;
                  int expandLevel = 0;
                  if( !expandAll)
                  {
                	  expandLevel = Integer.parseInt(expLevel);
                  }

             	  contentMapList = domainObject.getRelatedObjects(context,
             			  										relationshipPattern,
                                                                "*",
                                                                selectTypeStmts,
                                                                selectRelStmts,
                                                                false,
                                                                true,
                                                                (short)expandLevel,
                                                                sObjWhere,
                                                                null,
                                                                null,
                                                                null,
                                                                null);

             }
         }catch(Exception ex)
         {
             throw new Exception(ex.toString());
         }
         return contentMapList;
     }


  /**
   * hasMoveAccess - to check whether to display the Link Move Selected.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the objectId
   * @return boolean type
   * @throws Exception if the operation fails
   * @since Team 10-0-1-0
   */
  public static boolean hasMoveAccess(Context context, String args[]) throws Exception
  {
      HashMap programMap         = (HashMap) JPO.unpackArgs(args);
      String objectId            = (String) programMap.get("objectId");
      boolean boolChecking = false;

      DomainObject BaseObject   = DomainObject.newInstance(context,objectId);
      String baseType = BaseObject.getInfo(context, BaseObject.SELECT_TYPE);
      if( hasRemoveContent(context,args) && baseType.equals(DomainConstants.TYPE_WORKSPACE_VAULT)) {
          boolChecking = true;
      }
     return boolChecking;
  }

  /**
   * getNewWindowIcon - to display the new window icon using programHTMLOutput
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the programMap
   * @return Vector type
   * @throws Exception if the operation fails
   * @since Team V6R2010
   */
  public Vector getNewWindowIcon(Context context, String[] args)
  throws Exception
{
  try
  {
      HashMap programMap = (HashMap) JPO.unpackArgs(args);
      MapList objectList = (MapList)programMap.get("objectList");
      DomainObject taskObject = new DomainObject();
      Vector vecShowNewWindowIcon  = new Vector();


      Map paramList = (Map)programMap.get("paramList");
      boolean isExporting = (paramList.get("reportFormat") != null);


      StringBuffer prefixLinkBuffer = new StringBuffer();
      prefixLinkBuffer.append("'../common/emxTree.jsp?mode=insert");

      StringBuffer tempLinkBuffer = new StringBuffer();
      tempLinkBuffer.append("&amp;relId=");
      tempLinkBuffer.append(XSSUtil.encodeForJavaScript(context, (String)paramList.get("relId")));
      tempLinkBuffer.append("&amp;parentOID=");
      tempLinkBuffer.append(XSSUtil.encodeForJavaScript(context, (String)paramList.get("parentOID")));
      tempLinkBuffer.append("&amp;jsTreeID=");
      tempLinkBuffer.append(XSSUtil.encodeForJavaScript(context, (String)paramList.get("jsTreeID")));
      tempLinkBuffer.append("&amp;objectId=");
      String sContextType = "";
      //Bug 318463. End: Added above variables.

      Iterator objectListItr = objectList.iterator();
      while(objectListItr.hasNext())
      {
    	  String newURL = "";
          String strTreeName = "type_DOCUMENTS";
          Map objectMap = (Map) objectListItr.next();
          docType = (String)objectMap.get(DomainConstants.SELECT_TYPE);

	if(docType!=null){
            // Get the treeMenu of the Type.
            if (docType.equals(DomainConstants.TYPE_DOCUMENT) || CommonDocument.TYPE_DOCUMENTS.equals(CommonDocument.getParentType(context, docType)))
            {
                strTreeName = "TMCtype_Document";
            }
            else if (docType.equals(DomainConstants.TYPE_PACKAGE) )
            {
              strTreeName = "TMCtype_Package";
            }
            else if (docType.equals(DomainConstants.TYPE_RFQ) )
            {
              strTreeName = "TMCtype_RequestToSupplier";
            }
            else if (docType.equals(DomainConstants.TYPE_RTS_QUOTATION) )
            {
              strTreeName = "TMCtype_RTSQuotation";
            }
            else
            {
              strTreeName = "";
            }
	}else{
		strTreeName = "";
	}
        StringBuffer finalURL = new StringBuffer();
        finalURL.append(prefixLinkBuffer.toString());
        finalURL.append(tempLinkBuffer.toString());
        finalURL.append(XSSUtil.encodeForJavaScript(context, (String)objectMap.get(taskObject.SELECT_ID)));
        finalURL.append("&amp;treeMenu=");
        finalURL.append(strTreeName);

        if(strTreeName.length() != 0){
          finalURL.append("&amp;suiteKey=");
          finalURL.append(XSSUtil.encodeForJavaScript(context, (String)paramList.get("suiteKey")));
          finalURL.append("&amp;emxSuiteDirectory=");
          finalURL.append(XSSUtil.encodeForJavaScript(context, (String)paramList.get("SuiteDirectory")));
        }

        newURL+="<a href=\"javascript:emxTableColumnLinkClick("+finalURL.toString()+"', '875', '550', 'false', 'popup', '');\"><img border=\"0\" src=\"images/iconNewWindow.gif\"></img></a>";


        vecShowNewWindowIcon.add(newURL);
      }
      return vecShowNewWindowIcon;
  }
  catch (Exception ex)
  {
      System.out.println("Error in newIconWindow= " + ex.getMessage());
      throw ex;
  }
}
  /**
   * Get  for the specified criteria
   * @param context the eMatrix <code>Context</code> object.
   * @param args contains a Map with the following entries:
   *   selType - a String containing the type(s) to search for
   *   txtName - a String containing a Name pattern to search for
   *   txtRev - a String containing a Revision pattern to search for
   *   txtOwner - a String containing an Owner pattern to search for
   *   txtWhere - a URL encoded String containing a Matrix where clause.
   *   txtOrginator - a String containing an Originator pattern to search for
   *   txtSearch - a String containing a text pattern to Search for.
   *   txtFormat - a String containing a format pattern to Search for.
   *   setRadio  - a String containing the Collection name to open.
   * @return MapList containing search result.
   * @exception Exception if the operation fails.
   */
   @com.matrixone.apps.framework.ui.ProgramCallable
   public MapList getGeneralSearchResults(Context context , String[] args)
                     throws Exception
   {
     HashMap paramMap = (HashMap)JPO.unpackArgs(args);

     //Retrieve Search criteria
     String selType          = (String)paramMap.get("Type");
     String txtName          = (String)paramMap.get("Name");
     String txtRev           = (String)paramMap.get("txtRev");
     //Code modified for bug ID : 363745 put personName param in place of txtOwner
     String txtOwner         = (String)paramMap.get("Owner");




     String txtWhere         = (String)paramMap.get("txtWhere");
     String txtOriginator    = (String)paramMap.get("Originator");
     String txtDescription   = (String)paramMap.get("Description");
     String txtSearch        = (String)paramMap.get("txtSearch");
     String txtFormat        = (String)paramMap.get("txtFormat");
     String languageStr      = (String)paramMap.get("languageStr");
     String sSetName         = (String)paramMap.get("setRadio");
     String chkLastRevision  = (String)paramMap.get("chkLastRevision");
     String slkupOriginator = PropertyUtil.getSchemaProperty(context,"attribute_Originator");
     String target       = "";
     String sAnd         = "&&";
     String sOr          = "||";
     char chDblQuotes    = '\"';

 /**************************Vault Code Start*****************************/
 // Get the user's vault option & call corresponding methods to get
 // the vault's.

       String txtVault   ="";
       String strVaults="";
       StringList strListVaults=new StringList();

       String txtVaultOption = (String)paramMap.get("vaultOption");
       if(txtVaultOption==null) {
         txtVaultOption="";
       }
       if(txtVaultOption.equals("selected"))
          txtVaultOption=(String)paramMap.get("selVaults");
       String vaultAwarenessString = (String)paramMap.get("vaultAwarenessString");
       if(vaultAwarenessString!= null)
       {

       if(vaultAwarenessString.equalsIgnoreCase("true")){

         if(txtVaultOption.equals("ALL_VAULTS") || txtVaultOption.equals(""))
         {
           strListVaults = com.matrixone.apps.common.Person.getCollaborationPartnerVaults(context,null);
           StringItr strItr = new StringItr(strListVaults);
           if(strItr.next()){
             strVaults =strItr.obj().trim();
           }
           while(strItr.next())
           {
             strVaults += "," + strItr.obj().trim();
           }
           txtVault = strVaults;
         }
         else if(txtVaultOption.equals("LOCAL_VAULTS"))
         {
           com.matrixone.apps.common.Person person = com.matrixone.apps.common.Person.getPerson(context);
           Company company = person.getCompany(context);
           txtVault = company.getLocalVaults(context);
         }
         else if (txtVaultOption.equals("DEFAULT_VAULT"))
         {
           txtVault = context.getVault().getName();
         }
         else
         {
           txtVault = txtVaultOption;

         }
       }
   }
       else
       {
         if(txtVaultOption.equals("ALL_VAULTS") || txtVaultOption.equals(""))
         {
           // get ALL vaults
           Iterator mapItr = VaultUtil.getVaults(context).iterator();
           if(mapItr.hasNext())
           {
             txtVault =(String)((Map)mapItr.next()).get("name");

             while (mapItr.hasNext())
             {
               Map map = (Map)mapItr.next();
               txtVault += "," + (String)map.get("name");
             }
           }

         }
         else if(txtVaultOption.equals("LOCAL_VAULTS"))
         {
           // get All Local vaults
           strListVaults = VaultUtil.getLocalVaults(context);
           StringItr strItr = new StringItr(strListVaults);
           if(strItr.next()){
             strVaults =strItr.obj().trim();
           }
           while(strItr.next())
           {
             strVaults += "," + strItr.obj().trim();
           }
           txtVault = strVaults;
         }
         else if (txtVaultOption.equals("DEFAULT_VAULT"))
         {
           txtVault = context.getVault().getName();
         }
         else
         {
           txtVault = txtVaultOption;
         }
       }
       //trimming
       txtVault = txtVault.trim();

   /**************************Vault Code End*******************************/

     if (sSetName == null || sSetName.equals("null") || sSetName.equals("")){
       sSetName = "";
     }

     String savedQueryName   = (String)paramMap.get("savedQueryName");
     if (savedQueryName == null || savedQueryName.equals("null") || savedQueryName.equals("")){
       savedQueryName="";
     }

     String queryLimit = (String)paramMap.get("queryLimit");
     if (queryLimit == null || queryLimit.equals("null") || queryLimit.equals("")){
       queryLimit = "0";
     }

     if (txtName == null || txtName.equalsIgnoreCase("null") || txtName.length() <= 0){
       txtName = "*";
     }
     if (txtRev == null || txtRev.equalsIgnoreCase("null") || txtRev.length() <= 0){
       txtRev = "*";
     }
     if (txtOwner == null || txtOwner.equalsIgnoreCase("null") || txtOwner.length() <= 0){
       txtOwner = "*";
     }
     if (txtDescription != null && !txtDescription.equalsIgnoreCase("null") && txtDescription.equals("*")){
       txtDescription = "";
     }
     if (txtOriginator != null && !txtOriginator.equalsIgnoreCase("null") && txtOriginator.equals("*")){
       txtOriginator = "";
     }
     if (txtWhere == null || txtWhere.equalsIgnoreCase("null"))
     {
       txtWhere = "";
     }
     else
     {
       txtWhere = com.matrixone.apps.domain.util.XSSUtil.decodeFromURL(txtWhere);
     }
     String sWhereExp = txtWhere;

     if (!(txtOriginator == null || txtOriginator.equalsIgnoreCase("null") || txtOriginator.length() <= 0 )) {
       String sOriginatorQuery = "attribute[" + slkupOriginator + "] ~~ " + chDblQuotes + txtOriginator + chDblQuotes;
       if (sWhereExp == null || sWhereExp.equalsIgnoreCase("null") || sWhereExp.length()<=0 ){
         sWhereExp = sOriginatorQuery;
       } else {
         sWhereExp += sAnd + " " + sOriginatorQuery;
       }
     }

     if (!(txtDescription == null || txtDescription.equalsIgnoreCase("null") || txtDescription.length() <= 0 )) {
       String sDescQuery = "description ~~ " + chDblQuotes + txtDescription + chDblQuotes;
       if (sWhereExp == null || sWhereExp.equalsIgnoreCase("null") || sWhereExp.length()<=0 ){
         sWhereExp = sDescQuery;
       } else {
         sWhereExp += sAnd + " " + sDescQuery;
       }
     }

     if (selType.equals(DomainConstants.TYPE_DOCUMENT))
     {
       String verAttr = PropertyUtil.getSchemaProperty(context,"attribute_IsVersionObject");
       String docWhere = "attribute[" + verAttr + "] != True";
       if (sWhereExp == null ||
           sWhereExp.equalsIgnoreCase("null") ||
           sWhereExp.length()<=0 )
       {
         sWhereExp = docWhere;
       }
       else
       {
         sWhereExp += sAnd + " " + docWhere;
       }
     }
    // This code need to be taken out once Sourcing X+3 Migration is Completed     -SC
    // Start of Pre Migration Code -SC
        else if(selType.equals(DomainObject.TYPE_REQUEST_TO_SUPPLIER)){
            if ((sWhereExp == null) || (sWhereExp.equalsIgnoreCase("null")) || (sWhereExp.length()<=0 ))
            {
                 sWhereExp = " (!to[" + DomainObject.RELATIONSHIP_COMPANY_RFQ + "]) ";
            }
            else
            {
                 sWhereExp += sAnd + " " + " (!to[" + DomainObject.RELATIONSHIP_COMPANY_RFQ + "]) ";
            }
        } else if(selType.equals(DomainObject.TYPE_PACKAGE)){
            if ((sWhereExp == null) || (sWhereExp.equalsIgnoreCase("null")) || (sWhereExp.length()<=0 ))
            {
                sWhereExp = " (!to[" + DomainObject.RELATIONSHIP_COMPANY_PACKAGE + "]) ";
            }
            else
            {
                sWhereExp += sAnd + " " + " (!to[" + DomainObject.RELATIONSHIP_COMPANY_PACKAGE + "]) ";
            }
        } else if(selType.equals(DomainObject.TYPE_RTS_QUOTATION)){
            if ((sWhereExp == null) || (sWhereExp.equalsIgnoreCase("null")) || (sWhereExp.length()<=0 ))
            {
                sWhereExp = " (to[" + DomainObject.RELATIONSHIP_RTS_QUOTATION + "].from.to["+ DomainObject.RELATIONSHIP_COMPANY_RFQ +"]!='#DENIED!') && (!(to[" + DomainObject.RELATIONSHIP_RTS_QUOTATION + "].from.to["+ DomainObject.RELATIONSHIP_COMPANY_RFQ +"]))";
            }
            else
            {
                sWhereExp += sAnd + " " + "(to[" + DomainObject.RELATIONSHIP_RTS_QUOTATION + "].from.to["+ DomainObject.RELATIONSHIP_COMPANY_RFQ +"]!='#DENIED!') && (!(to[" + DomainObject.RELATIONSHIP_RTS_QUOTATION + "].from.to["+ DomainObject.RELATIONSHIP_COMPANY_RFQ +"]))";
            }
        }
    // End of Pre Migration Code - SC

     SelectList resultSelects = new SelectList(7);
     resultSelects.add(DomainObject.SELECT_ID);
     resultSelects.add(DomainObject.SELECT_TYPE);
     resultSelects.add(DomainObject.SELECT_NAME);
     resultSelects.add(DomainObject.SELECT_REVISION);
     resultSelects.add(DomainObject.SELECT_DESCRIPTION);
     resultSelects.add(DomainObject.SELECT_CURRENT);
     resultSelects.add(DomainObject.SELECT_POLICY);


     MapList totalresultList = null;

     if(chkLastRevision != null){
         if (sWhereExp != null && sWhereExp.trim().length()!= 0 )
             sWhereExp += sAnd + " ";
         sWhereExp += "(revision == last)";
     }

     // Check for a set name and use that set for results if present
     if (sSetName.equals(""))
     {
       if (savedQueryName.equals(""))
       {
         totalresultList = DomainObject.findObjects(context,
                                                    selType,
                                                    txtName,
                                                    txtRev,
                                                    txtOwner,
                                                    txtVault,
                                                    sWhereExp,
                                                    null,
                                                    true,
                                                    resultSelects,
                                                    Short.parseShort(queryLimit),
                                                    txtFormat,
                                                    txtSearch);
       }
       else
       {
         matrix.db.Query query = new matrix.db.Query(savedQueryName);
           try
           {
               ContextUtil.startTransaction(context,false);
               query.open(context);
               query.setObjectLimit(Short.parseShort(queryLimit));
               totalresultList = FrameworkUtil.toMapList(query.getIterator(context,resultSelects,Short.parseShort(queryLimit)), FrameworkUtil.MULTI_VALUE_LIST);
               query.close(context);
               ContextUtil.commitTransaction(context);
           }
           catch(Exception ex)
           {
                ContextUtil.abortTransaction(context);
                throw new Exception (ex);
           }
       }
     }
     else
     {
         totalresultList = SetUtil.getMembers(context,
                                              sSetName,
                                              resultSelects);
     }
     return totalresultList;
   }


}
