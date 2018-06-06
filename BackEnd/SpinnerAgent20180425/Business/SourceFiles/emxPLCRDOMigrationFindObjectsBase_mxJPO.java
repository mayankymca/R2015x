/*
 ** ${CLASSNAME}.java
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved.
 ** This program contains proprietary and trade secret information of
 ** Dassault Systemes.
 ** Copyright notice is precautionary only and does not evidence any actual
 ** or intended publication of such program
 */

import matrix.db.*;
import matrix.util.*;

import java.io.*;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.*;

public class emxPLCRDOMigrationFindObjectsBase_mxJPO extends emxCommonFindObjects_mxJPO
{
    String migrationProgramName = "emxPLCRDOMigration";

    /**
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @grade 0
     */
    public emxPLCRDOMigrationFindObjectsBase_mxJPO (Context context, String[] args)
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
        throws Exception {

    	// This property is a comma separated list of types.
		String typesToMigrate = EnoviaResourceBundle.getProperty(context, "emxProduct.RDOMigration.Types");
		

        try {
            if (args.length < 2 ) {
                throw new IllegalArgumentException();
            }
            String[] newArgs = new String[3];
            newArgs[0] = args[0];
            newArgs[1] = typesToMigrate;
            newArgs[2] = args[1];
            return super.mxMain(context, newArgs);


        } catch (IllegalArgumentException iExp) {
            writer.write("=================================================================\n");
            writer.write("Wrong number of arguments Or Invalid number of Oids per file\n");
            writer.write("Step 1 of Migration ${CLASS:emxCFFBinaryUpgradeChangeMigrationFindObjectsBase} :   " + iExp.toString() + "   : FAILED \n");
            writer.write("=================================================================\n");
            writer.close();
            return 0;
        }
    }
    
    

    /**
     * Evalutes a temp query to get all the DOCUMENTS objects in the system
     * @param context the eMatrix <code>Context</code> object
     * @param chunksize has the no. of objects to be stored in file.
     * @return void
     * @exception Exception if the operation fails.
     */
    public void getIds(Context context, int chunkSize) throws Exception
    {
        // Written using temp query to optimize performance in anticipation of large 1m+ Substitute in system.
        // Utilize query limit to use different algorithm in memory allocation    	

    	//===========================================================================================
    	// Find all objects for migration.
    	//===========================================================================================

    	String vaultList = "*";
                             
    	String whereClause = "(to[" + DomainConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY + "]==true) && " + 
    			"(program[" + migrationProgramName + " -method writeOID ${OBJECTID} \"${TYPE}\"] == true)";

    	String cmdParameterized = "temp query bus '$1' '$2' '$3'  vault '$4' limit $5 where '$6'";
        
        // Reset/set static variables back to original values every time this JPO is run
        emxPLCRDOMigration_mxJPO._counter  = 0;
        emxPLCRDOMigration_mxJPO._sequence  = 1;
        emxPLCRDOMigration_mxJPO._oidsFile = null;
        emxPLCRDOMigration_mxJPO._fileWriter = null;
        emxPLCRDOMigration_mxJPO._objectidList = null;

        // Set statics
        // Create BW and file first time
        if (emxPLCRDOMigration_mxJPO._fileWriter == null) {
            try {
            	emxPLCRDOMigration_mxJPO.documentDirectory = documentDirectory;
            	emxPLCRDOMigration_mxJPO._oidsFile = new java.io.File(documentDirectory + "objectids_1.txt");
            	emxPLCRDOMigration_mxJPO._fileWriter = new BufferedWriter(new FileWriter(emxPLCRDOMigration_mxJPO._oidsFile));
            	emxPLCRDOMigration_mxJPO._chunk = chunkSize;
            	emxPLCRDOMigration_mxJPO._objectidList = new StringList(chunkSize);
            } catch(FileNotFoundException eee) {
                throw eee;
            }
        }

        try {
        	MqlUtil.mqlCommand(context, cmdParameterized, type, "*", "*", vaultList, "2", whereClause);
        } catch(Exception me) {
            throw me;
        }

        // call cleanup to write the left over oids to a file
        emxPLCRDOMigration_mxJPO.cleanup();
    }
    
}
