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
 *
 * The <code>emxStructureRule</code> class/interface contains... 
 *
 * @version CBP X.X.X.X - Copyright (c) 1992,2015 Dassault Systemes.
 */
public abstract class emxStructureRule_mxJPO extends emxStructureRuleBase_mxJPO {

    /**
     * 
     */
    public emxStructureRule_mxJPO (Context context, String[] args) 
        throws Exception 
    {
        super(context, args);
    }
    public emxStructureRule_mxJPO ()
        throws Exception
    {
	super();
    }
}
