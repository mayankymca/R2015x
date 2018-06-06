/*
 * ${CLASSNAME}
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;

import matrix.db.Context;
import matrix.util.StringList;

import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;


/**
 * The <code>emxVariantConfigurationFindIntermediateObjectsBase</code> class contains implementation code for emxVariantConfigurationFindIntermediateObjectsBase.
 *
 * @version FTR V6R2012x - Copyright (c) 2011-2015, Dassault Systemes, Inc.
 */
public class emxProductConfigurationMigrationFindObjectsBase_mxJPO extends emxCommonFindObjects_mxJPO
{
    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since FTR V6R2012x
     */
    public emxProductConfigurationMigrationFindObjectsBase_mxJPO (Context context, String[] args)
        throws Exception
    {
        super(context, args);
    }

     /**
     * This method is executed if a specific method is not specified.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @returns nothing
     * @throws Exception if the operation fails
     */
    public int mxMain(Context context, String[] args)
        throws Exception
    {
        try
        {
            if (args.length < 2 )
            {
                throw new IllegalArgumentException();
            }
            String[] newArgs = new String[7];
            newArgs[0] = args[0];
            newArgs[1] = PropertyUtil.getSchemaProperty(context,"type_ProductConfiguration");
            newArgs[2] = args[1];
            newArgs[3] = "emxCommonMigration";
            newArgs[4] = "*";
            newArgs[5] = "*";            
            newArgs[6] = "type";
            return super.mxMain(context, newArgs);
        }
        catch (IllegalArgumentException iExp)
        {
            writer.write("=================================================================\n");
            writer.write("Wrong number of arguments Or Invalid number of Oids per file\n");
            writer.write("Step 1 of Migration ${CLASS:emxVariantConfigurationIntermediateObjectFindObjectsBase} :   " + iExp.toString() + "   : FAILED1111 \n");
            writer.write("=================================================================\n");
            writer.close();
            return 0;
        }
    }  
    /**
     * Evalutes a query to get all the Product Configuration objects in the system
     * @param context the eMatrix <code>Context</code> object
     * @param chunksize has the no. of objects to be stored in file.
     * @return void
     * @exception Exception if the operation fails.
     */
    public void getIds(Context context, int chunkSize) throws Exception
    {

        String vaultList = "*";
        StringList commandList = new StringList(3);
        if( isType )
        {
    			commandList.addElement("temp query bus '" + type + "' '" + name + "' '" + revision + "'  vault '" + vaultList + "' limit 1 where 'program[" + migrationProgramName + " -method writeOID ${OBJECTID} \"${TYPE}\"] == true'");
        }
        
        //reset/set static variables back to original values every time this JPO is run
        emxCommonMigration_mxJPO._counter  = 0;
        emxCommonMigration_mxJPO._sequence  = 1;
        emxCommonMigration_mxJPO._oidsFile = null;
        emxCommonMigration_mxJPO._fileWriter = null;
        emxCommonMigration_mxJPO._objectidList = null;

        //set statics
        //create BW and file first time
        if (emxCommonMigration_mxJPO._fileWriter == null)
        {
            try
            {
                emxCommonMigration_mxJPO.documentDirectory = documentDirectory;
                emxCommonMigration_mxJPO._oidsFile = new java.io.File(documentDirectory + "objectids_1.txt");
                emxCommonMigration_mxJPO._fileWriter = new BufferedWriter(new FileWriter(emxCommonMigration_mxJPO._oidsFile));
                emxCommonMigration_mxJPO._chunk = chunkSize;
                emxCommonMigration_mxJPO._objectidList = new StringList(chunkSize);
            }
            catch(FileNotFoundException eee)
            {
                throw eee;
            }
        }
        try
        {
            String command = "";
            for (int i=0; i< commandList.size(); i++)
            {
                command = (String)commandList.get(i);
                MqlUtil.mqlCommand(context, command);
            }
        }
        catch(Exception me)
        {
            throw me;
        }
        // call cleanup to write the left over oids to a file
        emxCommonMigration_mxJPO.cleanup();
    }
}
