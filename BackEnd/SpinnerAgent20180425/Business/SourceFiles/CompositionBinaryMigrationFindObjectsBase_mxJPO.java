/*
 * ${CLASS:CompositionBinaryMigrationFindObjects}.java program to get Ids of all objects which needs to generate Composition Binary Data.
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.productline.ProductLineCommon;
import java.util.Iterator;
import matrix.db.Context;
import matrix.db.MQLCommand;


/**
 * The <code>CompositionBinaryMigrationFindObjects</code> class contains implementation code for list out all the 
 * Products, Builds and Manufacturing Plan.
 * This is the very first step of Migration.
 *
 * @version FTR V6R2012x - Copyright (c) 2011-2015, Dassault Systemes, Inc.
 */
public class CompositionBinaryMigrationFindObjectsBase_mxJPO extends emxCommonFindObjects_mxJPO
{
	public static MQLCommand mqlCommand = null;
	public static String[] parentCompositionType = null;
	public static String[] childCompositionType = null;
	
	FileWriter findObjectLog = null; 
	
    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since FTR V6R2012x
     */
    public CompositionBinaryMigrationFindObjectsBase_mxJPO (Context context, String[] args)
        throws Exception
    {
        super(context, args);
        mqlCommand = MqlUtil.getMQL(context);        
    }

     /**
     * This method is executed if a specific method is not specified.
     * This is the actual method to get all the all ids of objects that need composition binary generated and save the ids to a list of filesname objectids_xx.txt
     * 
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @returns nothing
     * @throws Exception if the operation fails
     */
    public int mxMain(Context context, String[] args)
        throws Exception
    {	
    	StringBuffer typeToQuery = new StringBuffer();
    	
    	documentDirectory = args[1] + java.io.File.separator;
        findObjectLog = new FileWriter(documentDirectory + "findObject.log", true);
        
        findObjectLog.write("Starting Find Object Process \n");  
        
    	setAdminMigrationStatus(context,"FindObjectInProgress");
    	
    	findObjectLog.write("Getting Types To Generate Composition Binary For \n");     
    	
    	childCompositionType = getCompositionType(context, "Child Composition Binary");    	
    	parentCompositionType = getCompositionType(context, "Parent Composition Binary");
    	    	
    	findObjectLog.write("Found Child Composition Binary Type: "+Arrays.toString(childCompositionType)+" \n");
    	findObjectLog.write("Found Parent Composition Binary Type: "+Arrays.toString(parentCompositionType)+" \n");
    	
    	// to remove duplicate if any in child and parent composition
    	Set typeToQuerySet = new HashSet(Arrays.asList(childCompositionType));
    	typeToQuerySet.addAll(Arrays.asList(parentCompositionType));
    	
    	Iterator itr = typeToQuerySet.iterator();
    	while(itr.hasNext()){
    		typeToQuery.append((String)itr.next());
    		typeToQuery.append(",");
    	}
    	
    	if(typeToQuery.length() != 0) {
    		typeToQuery.deleteCharAt(typeToQuery.length() - 1);
    	}    	
    	else{
    		findObjectLog.write("Could Not Find Type To Query, Migration Will Not Be Continued \n");
    		return -1;
    	}    	
    	
        try
        {
            if (args.length < 2 )
            {
                throw new IllegalArgumentException();
            }
            findObjectLog.write("Fetching Objects In Database \n");   
            
            String[] newArgs = new String[7];
            newArgs[0] = args[0];
            newArgs[1] = typeToQuery.toString();
            newArgs[2] = args[1];
            newArgs[3] = "emxCommonMigration";
            newArgs[4] = "*";
            newArgs[5] = "*";            
            newArgs[6] = "type";             
            
            int findObjectStatus = super.mxMain(context, newArgs);  
           
            setAdminMigrationStatus(context,"FindObjectCompleted");
            findObjectLog.write("Find Object Completed \n"); 
            
            return findObjectStatus;
        }
        catch (IllegalArgumentException iExp)
        {
        	findObjectLog.write("=================================================================\n");
        	findObjectLog.write("Wrong number of arguments Or Invalid number of Oids per file\n");
        	findObjectLog.write("Step 2.1 of Migration CompositionBinaryMigrationFindObjects_mxJPO :   " + iExp.toString() + "   : FAILED!!!! \n");
        	findObjectLog.write("=================================================================\n");
        	findObjectLog.close();
            return 0;
        }  
        finally{
        	findObjectLog.close();
        }
    }
    
    
    public String[] getCompositionType(Context context, String strSelection) throws Exception{
    	
    	String[] typeToQueryArr = new String[0];
    	
    	String cmd = "print relationship \"" + strSelection + "\" select totype dump";
    	String typeToQuery = MqlUtil.mqlCommand(context, mqlCommand, cmd);
    	if(ProductLineCommon.isNotNull(typeToQuery)){
    		typeToQueryArr = typeToQuery.split(",");
    	}
    	
    	return typeToQueryArr;
    }
    
    /**
	 * Sets the migration status as a property setting.
	 * Status could be :-
	 * MigrationPreCheckCompleted
	 * MigrationCompleted
	 * MigrationPostCheckCompleted
	 * 
     * @param context the eMatrix <code>Context</code> object
 	 * @param strStatus String containing the status setting
	 * @throws Exception
	 */
	public void setAdminMigrationStatus(Context context,String strStatus) throws Exception
	{
		String cmd = "modify program eServiceSystemInformation.tcl property CompositionBinaryMigration value "+strStatus;
		MqlUtil.mqlCommand(context, mqlCommand,  cmd);		
	}

	/**
	 * Gets the migration status as an integer value.  Used to enforce an order of migration.
     * @param context the eMatrix <code>Context</code> object
 	 * @return integer representing the status
	 * @throws Exception
	 */	
	public int getAdminMigrationStatus(Context context) throws Exception
	{
		String cmd = "print program eServiceSystemInformation.tcl select property[CompositionBinaryMigration].value dump";
	    String result =	MqlUtil.mqlCommand(context, mqlCommand, cmd);
	   
	    if(result.equalsIgnoreCase("MigrationPreCheckCompleted")){
			return 1;
		}
	    else if(result.equalsIgnoreCase("MigrationCompleted")){
			return 2;
		}
	    else if(result.equalsIgnoreCase("MigrationPostCheckCompleted")){
			return 3;
	    }    
	    
	    return 0;
	}
	
}
