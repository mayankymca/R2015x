/*
 * ChangeBinaryUpgradeMigrationFindObjectsBase.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import matrix.db.Context;
import matrix.util.StringList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;



/**
 * The <code>ChangeBinaryUpgradeMigrationFindObjectsBase</code> class contains implementation code for
 * ChangeBinaryUpgradeMigrationFindObjectsBase.
 *
 * @version CFF V6R2013x - Copyright (c) 2011-2015, Dassault Systemes, Inc.
 */
public class ChangeBinaryUpgradeMigrationFindObjectsBase_mxJPO extends emxCommonFindObjects_mxJPO
{

	static final String TYPE_NAMED_EFFECTIVITY = PropertyUtil.getSchemaProperty("type_NamedEffectivity");

    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since CFF V6R2013x
     */
    public ChangeBinaryUpgradeMigrationFindObjectsBase_mxJPO (Context context, String[] args)
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

    	ChangeBinaryUpgradeMigrationBase_mxJPO bupMigrationObj = new ChangeBinaryUpgradeMigrationBase_mxJPO(context, new String[0]);
    	StringBuffer typeList = new StringBuffer();
    	typeList.append(TYPE_NAMED_EFFECTIVITY);
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
            return super.mxMain(context, newArgs);


        }
        catch (IllegalArgumentException iExp)
        {
            writer.write("=================================================================\n");
            writer.write("Wrong number of arguments Or Invalid number of Oids per file\n");
            writer.write("Step 1 of Migration ChangeBinaryUpgradeMigrationFindObjects_mxJPO :   " + iExp.toString() + "   : FAILED \n");
            writer.write("=================================================================\n");
            writer.close();
            return 0;
        }
    }

    /**
     * Executes a query to get all Named Effectivity ids having effectivity defined
     * @param context the eMatrix <code>Context</code> object
     * @param chunksize has the no. of objects to be stored in file.
     * @return void
     * @exception Exception if the operation fails.
     */
    public void getIds(Context context, int chunkSize) throws Exception
    {

        String vaultList = "";

        if(vaultList == null || "null".equals(vaultList) || "".equals(vaultList))
        {
            vaultList = "*";
        }

        String result ="";

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
			if( !isType )
			{
				result  = MqlUtil.mqlCommand(context, "query connection relationship $1 limit $2 where $3",
						 type,"1",
						 "program[" + migrationProgramName + " -method writeOID ${OBJECTID} \"${TYPE}\"] == true ");
			}else{
				result  = MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 vault $4 limit $5 where $6",
						 type,name,revision,vaultList,"1",
						 "program[" + migrationProgramName + " -method writeOID ${OBJECTID} \"${TYPE}\"] == true ");
			}
        }
        catch(Exception me)
        {
            throw me;
        }

        // call cleanup to write the left over oids to a file
        emxCommonMigration_mxJPO.cleanup();

        try
        {
        	String objectId = "";
        	int idsCount = (emxCommonMigration_mxJPO._sequence - 1) * emxCommonMigration_mxJPO._chunk;
        	String fileName = "objectids_"+emxCommonMigration_mxJPO._sequence+".txt";

        	java.io.File file = new java.io.File(documentDirectory + fileName);
			//file is null if 0 IDs or totalIDs perfectly divisible by Chunk size
			if(file.exists() && !file.isDirectory())                                          //IR-375334-3DEXPERIENCER2015x
			{
				BufferedReader fileReader = new BufferedReader(new FileReader(file));
				while(fileReader.readLine() != null)
				{
					idsCount++;
				}
			}
            writer.write("==================Total No Of Objects==============================\n");
            writer.write("                  " + String.valueOf(idsCount) + "                 \n");
            writer.write("===================================================================\n");
			if(idsCount == 0)    
				writer.write("No objects found to migrate");	
        }
        catch(FileNotFoundException fExp)
        {
            throw fExp;
        }
   }
}
