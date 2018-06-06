/*
 * emxIntegrationMigration.java
 * program migrates the data into Common Document model i.e. to 10.5 schema.
 *
 * Copyright (c) 1992-2002 MatrixOne, Inc.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 * static const char RCSID[] = "$Id: emxIntegrationMigration.java,v 1.1 2004/04/23 15:47:44 vcheela Exp $"
 */

import matrix.db.*;
import java.lang.*;
/**
 * @version AEF 10.5 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxMSOIMigration_mxJPO extends emxMSOIMigrationBase_mxJPO
{

    /**
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public emxMSOIMigration_mxJPO (Context context, String[] args)
        throws Exception
    {
      super(context, args);
    }
}
