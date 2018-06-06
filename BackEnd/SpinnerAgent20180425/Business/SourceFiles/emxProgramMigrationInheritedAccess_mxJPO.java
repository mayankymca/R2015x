/*
 * ${CLASSNAME}.java
 * program migrates the data into Common Document model i.e. to 10.5 schema.
 *
 * Copyright (c) 1992-2016 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import matrix.db.*;
import java.lang.*;

public class emxProgramMigrationInheritedAccess_mxJPO extends emxProgramMigrationInheritedAccessBase_mxJPO
{

    /**
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since R417
     * @grade 0
     */
    public emxProgramMigrationInheritedAccess_mxJPO (Context context, String[] args)
        throws Exception
    {
      super(context, args);
    }
}
