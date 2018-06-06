/*
 * ${CLASSNAME} program to migrate Inclusion Rule on GBOM/Logical Features rel to Effectivity
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 */

import matrix.db.*;


public class InclusionRuleMigrationForGBOMAndLF_mxJPO extends
InclusionRuleMigrationForGBOMAndLFBase_mxJPO {
	/**
	 *
	 * @param context
	 *            the eMatrix <code>Context</code> object
	 * @param args
	 *            holds no arguments
	 * @throws Exception
	 *             if the operation fails
	 */
	public InclusionRuleMigrationForGBOMAndLF_mxJPO(Context context,
			String[] args) throws Exception {
		super(context, args);
	}

}
