/*
 * ChangeBinaryUpgradeMigrationFindObjects.java program to get all objects with Effectivity Expression 
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


public class ChangeBinaryUpgradeMigrationFindObjects_mxJPO extends
	ChangeBinaryUpgradeMigrationFindObjectsBase_mxJPO {
    /**
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since CFF V6R2013x
     */
    public ChangeBinaryUpgradeMigrationFindObjects_mxJPO(Context context,
            String[] args) throws Exception {
        super(context, args);
    }

}
