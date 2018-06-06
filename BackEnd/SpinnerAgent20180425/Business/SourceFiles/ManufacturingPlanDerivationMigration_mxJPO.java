/*
 * ${CLASSNAME}.java program to migrate Product and Manufacturing Plan structure of found Models.
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import matrix.db.*;


public class ManufacturingPlanDerivationMigration_mxJPO extends
ManufacturingPlanDerivationMigrationBase_mxJPO {
    /**
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     */
    public ManufacturingPlanDerivationMigration_mxJPO(Context context,
            String[] args) throws Exception {
        super(context, args);
    }

}
