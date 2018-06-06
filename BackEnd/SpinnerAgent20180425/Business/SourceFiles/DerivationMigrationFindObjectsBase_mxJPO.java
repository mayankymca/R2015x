/*
 * DerivationMigrationFindObjectsBase.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.productline.ProductLineConstants;
import matrix.db.Context;
import matrix.db.MQLCommand;


/**
 * The <code>DerivationMigrationFindObjectsBase</code> class contains implementation code for list out all the Models in the system.
 * This is the very first step of Migration.
 *
 * @version PLC V6R2014x - Copyright (c) 2011-2015, Dassault Systemes, Inc.
 */
public class DerivationMigrationFindObjectsBase_mxJPO extends emxCommonFindObjects_mxJPO
{
	public static MQLCommand mqlCommand = null;
	
    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     */
    public DerivationMigrationFindObjectsBase_mxJPO (Context context, String[] args)
        throws Exception
    {
        super(context, args);
        mqlCommand = MqlUtil.getMQL(context);
    }

     /**
     * This method is executed if a specific method is not specified.
     * This is the actual method to get all the Model ids in system and write them in file name objectids_xx.txt
     * 
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @returns nothing
     * @throws Exception if the operation fails
     */
    public int mxMain(Context context, String[] args)
        throws Exception
    {
    	               
        StringBuffer typeList = new StringBuffer(100);
        typeList.append(ProductLineConstants.TYPE_MODEL);
        
        try
        {
            if (args.length < 2 )
            {
                throw new IllegalArgumentException();
            }
            String[] newArgs = new String[7];
            newArgs[0] = args[0];
            newArgs[1] = typeList.toString();
            newArgs[2] = args[1];
            newArgs[3] = "emxCommonMigration";
            newArgs[4] = "*";
            newArgs[5] = "*";            
            newArgs[6] = "type";             
            int findObjectStatus = super.mxMain(context, newArgs);
            return findObjectStatus;

        }
        catch (IllegalArgumentException iExp)
        {
            writer.write("=================================================================\n");
            writer.write("Wrong number of arguments Or Invalid number of Oids per file\n");
            writer.write("Step 1 of Migration ${DerivationMigrationFindObjectsBase} :   " + iExp.toString() + "   : FAILED!!!! \n");
            writer.write("=================================================================\n");
            writer.close();
            return 0;
        }        
    }
    
    
	
}
