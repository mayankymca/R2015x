/*
**   MSFUtil.java
**
**   Copyright (c) 1992-2015 Dassault Systemes.
**   All Rights Reserved.
**   This program contains proprietary and trade secret information of MatrixOne,
**   Inc.  Copyright notice is precautionary only
**   and does not evidence any actual or intended publication of such program
**
*/

import matrix.db.Context;

/**
 * The <code>MSFUtil</code> class contains code for the general MSF activities.
 *
 * @version Common 10.5.1.2 - Copyright (c) 2004, MatrixOne, Inc.
 */

public class MSFUtil_mxJPO extends MSFUtilBase_mxJPO
{
      /**
       * Constructor.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param args holds no arguments
       * @throws Exception if the operation fails
       * @since Common 10.5.1.2
       * @grade 0
       */
      public MSFUtil_mxJPO (Context context, String[] args)
    		  throws Exception {
    	  super(context, args);
      }
}
