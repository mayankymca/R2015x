/*
 ** ${CLASS:MarketingFeature}
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved.
 ** This program contains proprietary and trade secret information of
 ** Dassault Systemes.
 ** Copyright notice is precautionary only and does not evidence any actual
 ** or intended publication of such program
 */

import matrix.db.Context;

/**
 * The <code>emxSortHTMLNumericAlpha</code> class contains methods for comparision.
 *
 * @version AEF 10.0.1.0 - Copyright (c) 2005, MatrixOne, Inc.
 */

public class emxSortHTMLNumericAlpha_mxJPO extends emxSortHTMLNumericAlphaBase_mxJPO
{

    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since EC 10.0.0.0
     */

    public emxSortHTMLNumericAlpha_mxJPO (Context context, String[] args)
        throws Exception
    {
        super(context, args);
    }

    /**
     * Default Constructor.
     */

    public emxSortHTMLNumericAlpha_mxJPO ()
    {
        super();
    }

}