// emxMsoiPMCUtilBase.java
//
// 
// Copyright (c) 2002 MatrixOne, Inc.
// All Rights Reserved
// This program contains proprietary and trade secret information of
// MatrixOne, Inc.  Copyright notice is precautionary only and does
// not evidence any actual or intended publication of such program.

import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import matrix.db.*;
import matrix.util.*;

import com.matrixone.apps.framework.ui.*;
import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;

/**
 * The <code>IEFUtil</code> class represents the JPO for
 * obtaining the MS Office integration menus
 *
 * @version AEF 10.5 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class IEFPMCUtilBase_mxJPO 
{

/**
   * Constructs a new IEFUtil JPO object.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args an array of String arguments for this method
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */
  public IEFPMCUtilBase_mxJPO (Context context, String[] args)
      throws Exception
  {
    // Call the super constructor
    super();
  }
   
  /**
   * Get Projects of a the current user
   * Returns a maplist of the current users project ids
   *
   * @param context the eMatrix <code>Context</code> object
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */

   public static MapList getCurrentUserProjects(Context context, String[] args) throws MatrixException
   {
     MapList projectList = new MapList();
     try
     {
		// rp3 : This needs to be tested to make sure the result creates no issue and returns the same set
	   projectList = (MapList)JPO.invoke(context, "emxProjectSpace", null, "getAllProjects", args, MapList.class);

		// rp3 : Replace this code with the JPO invoke from the program central API.
        /* 
         com.matrixone.apps.common.Person person = (com.matrixone.apps.common.Person) DomainObject.newInstance(context, DomainConstants.TYPE_PERSON);
         com.matrixone.apps.program.ProjectSpace project = (com.matrixone.apps.program.ProjectSpace) DomainObject.newInstance(context, DomainConstants.TYPE_PROJECT_SPACE,DomainConstants.PROGRAM);

         String ctxPersonId = person.getPerson(context).getId();
         person.setId(ctxPersonId);

          //System.out.println("the project id is " + ctxPersonId);

          StringList busSelects = new StringList(11);
          busSelects.add(project.SELECT_ID);
          busSelects.add(project.SELECT_TYPE);
          busSelects.add(project.SELECT_NAME);
          busSelects.add(project.SELECT_CURRENT);

         //projectList = project.getUserProjects(context,person,null,null,null,null);
         // expand to get project members
        Pattern typePattern = new Pattern(DomainConstants.TYPE_PROJECT_SPACE);
        typePattern.addPattern(DomainConstants.TYPE_PROJECT_CONCEPT);

        projectList = (person.getRelatedObjects(
                            context,                   // context
                            DomainConstants.RELATIONSHIP_MEMBER,       // relationship pattern
                            typePattern.getPattern(),  // type filter.
                            busSelects,             // business selectables
                            null,                      // relationship selectables
                            true,                      // expand to direction
                            false,                     // expand from direction
                            (short) 1,                 // level
                            null,               // object where clause
                            null));       // relationship where clause
         */
	 
         //System.out.println("the project list is " + projectList);
     }
     catch (Exception ex) 
     {
       throw (new MatrixException("emxMsoiPMCUtil:getCurrentUserProjects : " + ex.toString()) );
     }
     return projectList;
   }

  /**
   * Get the folders of a Project
   * Returns a maplist of folders 
   *
   * @param context the eMatrix <code>Context</code> object
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */

   public static MapList getProjectFolders(Context context, String[] args) throws MatrixException
   {
     MapList folderList = new MapList();
     try
     {
          HashMap initArgsMap = (HashMap)JPO.unpackArgs(args);
       HashMap paramMap = new HashMap();
          paramMap.put("objectId", (String)initArgsMap.get("objectId"));       
       HashMap argsMap = new HashMap();
       argsMap.put("paramMap", paramMap);
         String[] methodargs = JPO.packArgs(argsMap);

		// rp3 Fix for JPO compilation issue
	   folderList = (MapList)JPO.invoke(context, "emxProjectSpace", null, "getProjectSpaceFolderList", methodargs, MapList.class);

       //folderList = ${CLASS:emxProjectSpace}.getProjectSpaceFolderList(context, methodargs);
     }
     catch (Exception ex) 
     {
       throw (new MatrixException("emxMsoiPMCUtil:getProjectFolders: " + ex.toString()) );
     }
     return folderList;
   }
}
