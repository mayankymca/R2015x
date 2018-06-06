/* emxCommonProjectManagement.java

   Copyright (c) 1992-2015 Dassault Systemes.
   All Rights Reserved.
   This program contains proprietary and trade secret information of
   MatrixOne, Inc.  Copyright notice is precautionary only and does
   not evidence any actual or intended publication of such program.

*/

import java.io.*;
import java.util.*;
import matrix.db.*;
import matrix.util.*;
import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;

/**
 * The <code>emxProjectManagement</code> class represents the Project Management
 * JPO functionality for the AEF type.
 *
 * @version AEF 9.5.2.0 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxCommonProjectManagement_mxJPO extends emxCommonProjectManagementBase_mxJPO
{

    /**
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since PMC 10.0.0.0
     * @grade 0
     */
    public emxCommonProjectManagement_mxJPO (Context context, String[] args)
        throws Exception
    {
      super(context, args);
    }

    /**
     * Constructs a new emxProjectManagement JPO object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param id the business object id
     * @throws Exception if the operation fails
     * @since 10.0.0.0
     */
    public emxCommonProjectManagement_mxJPO (String id)
        throws Exception
    {
        // Call the super constructor
        super(id);
    }
}
