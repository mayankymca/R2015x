/*
 *  emxRouteTimerBase.java
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
import matrix.util.*;
import java.io.*;
import java.util.*;
import java.lang.*;

import com.matrixone.apps.domain.*;
import com.matrixone.apps.common.*;
import com.matrixone.apps.domain.util.*;
import com.matrixone.apps.framework.ui.*;
import com.matrixone.apps.common.util.*;

/**

 *
 * @version AEF 9.5.0.0 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxRouteTimerBase_mxJPO extends Route
{
    /**
     * Constructs a new Route JPO object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args an array of String arguments for this method
     * @throws Exception if the operation fails
     * @since AEF 9.5.0.0
     */
    public emxRouteTimerBase_mxJPO (Context context, String[] args)
        throws Exception
    {
        // Call the super constructor
        super();
        if (args != null && args.length > 0)
        {
            setId(args[0]);
        }
    }
}
