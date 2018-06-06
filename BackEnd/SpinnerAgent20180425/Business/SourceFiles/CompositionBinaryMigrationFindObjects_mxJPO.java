/*
 * ${CompositionBinaryMigrationFindObjects}.java program to get Ids of all objects which needs to generate Composition Binary Data.
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import matrix.db.Context;


public class CompositionBinaryMigrationFindObjects_mxJPO extends
CompositionBinaryMigrationFindObjectsBase_mxJPO {
    /**
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since FTR V6R2012x
     */
    public CompositionBinaryMigrationFindObjects_mxJPO(Context context,
            String[] args) throws Exception {
        super(context, args);
    }
}
