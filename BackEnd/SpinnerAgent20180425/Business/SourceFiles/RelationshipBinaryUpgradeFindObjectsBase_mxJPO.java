/*
 * ${CLASSNAME}.java
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
import java.util.Iterator;
import java.util.Scanner;
import java.util.StringTokenizer;

import matrix.db.Context;
import matrix.util.StringList;

import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.effectivity.EffectivityFramework;


/**
 * The <code>RelationshipBinaryUpgradeFindObjectsBase</code> class contains implementation code for 
 * RelationshipBinaryUpgradeFindObjectsBase.
 *
 * @version CFF V6R2013x - Copyright (c) 2011-2015, Dassault Systemes, Inc.
 */
public class RelationshipBinaryUpgradeFindObjectsBase_mxJPO extends emxCommonFindObjects_mxJPO
{
	
    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since CFF V6R2013x
     */
    public RelationshipBinaryUpgradeFindObjectsBase_mxJPO (Context context, String[] args)
        throws Exception
    {
        super(context, args);
    }

    /**
	 * Gets the migration status as an integer value.  Used to enforce an order of migration.
     * @param context the eMatrix <code>Context</code> object
 	 * @return integer representing the status
	 * @throws Exception
	 */
	public int getAdminMigrationStatus(Context context) throws Exception
	{

		String result =	MqlUtil.mqlCommand(context, "print program $1 select $2 dump",
						   "eServiceSystemInformation.tcl",
						   "property[RelationshipBinaryUpgradeMigration].value");
						   
	   
	    if(result.equalsIgnoreCase("MigrationPostCheckCompleted"))
		{
			return 3;
			
		}else if(result.equalsIgnoreCase("MigrationCompleted"))
		{
			return 2;
		}
		else if(result.equalsIgnoreCase("MigrationPreCheckCompleted"))
		{
			return 1;
		}
	    return 0;
	}
	
	/**
	 * Sets the migration status as a property setting.
   * @param context the eMatrix <code>Context</code> object
	 * @param strStatus String containing the status setting
	 * @throws Exception
	 */
	public void setAdminMigrationStatus(Context context,String strStatus) throws Exception
	{
		MqlUtil.mqlCommand(context, "modify program $1 property $2 value $3",
				   "eServiceSystemInformation.tcl","RelationshipBinaryUpgradeMigration",strStatus);
				   
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

    	
    	StringBuffer typeList = new StringBuffer();
        StringList relNamesList  = getEffectivityDefinedRels(context); 
		if(!relNamesList.isEmpty())
		{
			for(Iterator itr = relNamesList.iterator();itr.hasNext();){
				typeList.append((String)itr.next());
				if(itr.hasNext()){
					typeList.append(",");
				}
			}
		}
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
            newArgs[6] = "relationship";
            return super.mxMain(context, newArgs);
            

        }
        catch (IllegalArgumentException iExp)
        {
            writer.write("=================================================================\n");
            writer.write("Wrong number of arguments Or Invalid number of Oids per file\n");
            writer.write("Step 1 of Migration RelationshipBinaryUpgradeFindObjectsBase_mxJPO :   " + iExp.toString() + "   : FAILED \n");
            writer.write("=================================================================\n");
            writer.close();
            return 0;
        }
    }  
    
    /**
     * Executes a query to get all connection ids having effectivity defined
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
        String command = "";
        if( !isType )
        {
            command = "query connection relationship '" + type + "' limit 1 where 'interface["+EffectivityFramework.INTERFACE_EFFECTIVITY_FRAMEWORK+"]==TRUE && program[" + migrationProgramName + " -method writeOID ${OBJECTID} \"${TYPE}\"] == true '";
        }else{
        	command = "temp query bus '" + type + "' '" + name + "' '" + revision + "'  vault '" + vaultList + "' limit 1 where 'program[" + migrationProgramName + " -method writeOID ${OBJECTID} \"${TYPE}\"] == true'";
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
            result  = MqlUtil.mqlCommand(context, command);                                 
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
			if(file.exists() && !file.isDirectory())                               //IR-375334-3DEXPERIENCER2015x
			{
				BufferedReader fileReader = new BufferedReader(new FileReader(file));
				while((objectId = fileReader.readLine()) != null)
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
    
    /**
     * To return a list of all rel types on which property ENO_Effectivity is defined
     * @param context
     * @return
     * @throws Exception
     */
    private  StringList getEffectivityDefinedRels(Context context) throws Exception
    {
		StringList relTypeList  =  new StringList();
    	String command = "list rel * select name property[ENO_Effectivity].value dump |";
		String result = MqlUtil.mqlCommand(context, command);
		if(result != null && !result.isEmpty()){
			Scanner scanner = null;
	        try {
	        	scanner = new Scanner(result);
	            while (scanner.hasNextLine()) {
	                String line = scanner.nextLine();
	                StringTokenizer sTokensObj1 = new StringTokenizer(line,"|");
	                if(sTokensObj1.countTokens() > 1){
	                	relTypeList.add(sTokensObj1.nextToken().trim());
		        	}
	            }
	            scanner.close();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }finally {
	        	if(scanner != null)
	        		scanner.close();
	        }

		}
		return relTypeList;
    }
}
