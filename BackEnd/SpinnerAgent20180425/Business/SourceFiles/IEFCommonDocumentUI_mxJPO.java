/*
**   IEFCommonDocumentUI.java
**
**   Copyright (c) 1992-2000 MatrixOne, Inc.
**   All Rights Reserved.
**   This program contains proprietary and trade secret information of MatrixOne,
**   Inc.  Copyright notice is precautionary only
**   and does not evidence any actual or intended publication of such program
**
**   static const char RCSID[] = $Id: /ENODesignerCentral/CNext/Modules/ENODesignerCentral/AppInstall/Programs/schema/MCAD_Server/IEFCommonDocumentUI.java 1.1.2.1 Fri Nov 14 16:11:06 2008 GMT ds-rtembhurnikar Experimental$
*/

import matrix.db.Context;

public class IEFCommonDocumentUI_mxJPO extends IEFCommonDocumentUIBase_mxJPO
{
      /**
       * Constructor.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param args holds no arguments
       * @throws Exception if the operation fails
       * @since Common 10.0.0.0
       * @grade 0
       */
      public IEFCommonDocumentUI_mxJPO (Context context, String[] args)
          throws Exception
      {
          super(context, args);
      }

      /**
       * This method is executed if a specific method is not specified.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param args holds no arguments
       * @returns int
       * @throws Exception if the operation fails
       * @since Common 10.0.0.0
       */
      public int mxMain(Context context, String[] args)
          throws Exception
      {
          if (true)
          {
              throw new Exception("must specify method on IEFCommonDocumentUI invocation");
          }
          return 0;
      }
}
