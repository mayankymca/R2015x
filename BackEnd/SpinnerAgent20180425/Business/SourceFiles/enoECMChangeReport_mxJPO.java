/*
 ** ${CLASS:enoECMChangeOrder}
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved.
 */

import matrix.db.Context;

/**
 * The <code>enoECMChangeOrder</code> class contains code for the "Change Order" business type.
 *
 * @version ECM R215  - # Copyright (c) 1992-2015 Dassault Systemes.
 */
  public class enoECMChangeReport_mxJPO extends enoECMChangeReportBase_mxJPO
  {
      /**
       * Constructor.
       *
       * @param context the eMatrix <code>Context</code> object.
       * @param args holds no arguments.
       * @throws Exception if the operation fails.
       * @since ECM R215.
       */

      public enoECMChangeReport_mxJPO (Context context, String[] args) throws Exception {
          super(context, args);
      }
  }