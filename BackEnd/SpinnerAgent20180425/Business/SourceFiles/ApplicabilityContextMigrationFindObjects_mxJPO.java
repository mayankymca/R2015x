/*
 * ApplicabilityContextMigrationFindObjects.java program to get all document type Object Ids.
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 * static const char RCSID[] = $Id: /ENOEnterpriseChange/ENOECHJPO.mj/src/${CLASSNAME}.java 1.1.1.1 Thu Oct 28 22:27:16 2010 GMT przemek Experimental$
 */

import matrix.db.Context;

public class ApplicabilityContextMigrationFindObjects_mxJPO extends ApplicabilityContextMigrationFindObjectsBase_mxJPO {
    /**
     *
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since EnterpriseChange R211
     * @grade 0
     */
    public ApplicabilityContextMigrationFindObjects_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

}

