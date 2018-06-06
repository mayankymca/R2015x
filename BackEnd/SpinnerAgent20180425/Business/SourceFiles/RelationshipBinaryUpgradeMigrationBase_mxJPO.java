/*
 * ${CLASSNAME}.java
 * program migrates the data into Common Document model i.e. to 10.5 schema.
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
import matrix.db.Context;
import matrix.util.StringList;

import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.effectivity.EffectivityFramework;

/**
 * The <code>${CLASS:RelationshipBinaryUpgradeMigrationBase}</code> class contains the utilities
 * necessary to migrate the stored binary data for Effectiviy expression to new format. It must be run on all CFF data
 * created prior to V6R2013x.
 */
  public class RelationshipBinaryUpgradeMigrationBase_mxJPO extends emxCommonMigration_mxJPO
  {
       
      /**
      * Default constructor.
      * @param context the eMatrix <code>Context</code> object
      * @param args String array containing program arguments
      * @throws Exception if the operation fails
      */
      public RelationshipBinaryUpgradeMigrationBase_mxJPO (Context context, String[] args)
              throws Exception
      {

          super(context, args);
          this.warningLog = new FileWriter(documentDirectory + "RelationshipBinaryUpgradeMigration.log", true);

      }

      /**
       * Main migration method to handle migrating Features, GBOM and Rules.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param objectIdList StringList holds list objectids to migrate
       * @returns nothing
       * @throws Exception if the operation fails
       */
      public void  migrateObjects(Context context, StringList objectIdList)
      throws Exception
      {
          int migrationStatus = getAdminMigrationStatus(context);
          String logString = null;
          boolean bIsException=false;
          /* Below code can be uncommented after Migration Prechecks implementation
          if(migrationStatus < 2)
          {
        	  mqlLogRequiredInformationWriter("Migration pre-check is not complete.Please complete migration pre-checks before running Relationship Binary Upgrade migration. \n");
        	  bIsException=true;
        	  return;
          }*/
          try
          {
        	  //STEP 1 - turn triggers off
    		  String strMqlCommand = "trigger off";
    		  MqlUtil.mqlCommand(context,strMqlCommand,true);
    		  mqlLogRequiredInformationWriter("Relationship Binary Upgrade Migration is in Process \n");

              String[] oidsArray = new String[objectIdList.size()];
              oidsArray = (String[])objectIdList.toArray(oidsArray);
              mqlLogRequiredInformationWriter("Convert Effectivity Data...\n");

              EffectivityFramework.convertEffectivityData(context, oidsArray);
 
              mqlLogRequiredInformationWriter("...Conversion Complete.\n");
              for (int i = 0; i < objectIdList.size(); i++)
              {
            	  loadMigratedOids((String)objectIdList.get(i));
              }
              
        	  //STEP 3 - turn triggers on
    		  strMqlCommand = "trigger on";
    		  MqlUtil.mqlCommand(context,strMqlCommand,true);

    		  mqlLogRequiredInformationWriter("Relationship Binary Upgrade Migration Complete \n");
    	      
         }
          catch(Exception ex)
          {
        	  bIsException=true;
              ex.printStackTrace();
              for (int i = 0; i < objectIdList.size(); i++)
              {
            	  logString = "" + ","+(String)objectIdList.get(i)+",,";
            	  writeUnconvertedOID(logString+ex.getMessage()+"\n", (String)objectIdList.get(i));
              }
              
              throw ex;
          }
          finally{
      		  if(!bIsException){
      	  		  setAdminMigrationStatus(context,"MigrationCompleted");
      	  		}
          }
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
						   "eServiceSystemInformation.tcl","property[RelationshipBinaryUpgradeMigration].value");
						   
  	   
  	    if(result.equalsIgnoreCase("MigrationPostCheckCompleted"))
  		{
  			return 3;  			
  		}
  	    else if(result.equalsIgnoreCase("MigrationCompleted"))
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
		MqlUtil.mqlCommand(context, 
				   "modify program $1 property $2 value $3","eServiceSystemInformation.tcl","RelationshipBinaryUpgradeMigration",strStatus);
	}

	/**
     * Outputs the help for this migration.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args String[] containing the command line arguments
     * @throws Exception if the operation fails
     */
     public void help(Context context, String[] args) throws Exception
    {
         if(!context.isConnected())
         {
             throw new FrameworkException("not supported on desktop client");
         }

         writer.write("================================================================================================\n");
         writer.write(" Change Binary Migration steps::-  \n");
         writer.write("\n");
         writer.write(" Step1: Create the directory hierarchy like:- \n");
         writer.write(" C:/Migration \n");
         writer.write(" C:/Migration/RelationshipBinaryUpgradeMigration \n");           
         writer.write("\n");
         writer.write(" Step2: Perform  Migration \n");    
         writer.write(" \n");
         writer.write(" Step2.1: Find all objects that need binary generated and save the ids to a list of files \n");
         writer.write(" Example: \n");
         writer.write(" execute program RelationshipBinaryUpgradeMigrationFindObjects  1000 C:/Migration/RelationshipBinaryUpgradeMigration ; \n");
         writer.write(" First parameter  = indicates number of object per file \n");
         writer.write(" Second Parameter  = the directory where files should be written \n");
         writer.write(" \n");
         writer.write(" Step2.2: Generate new Change Binary \n");
         writer.write(" Example: \n");
         writer.write(" execute program  RelationshipBinaryUpgradeMigration  C:/Migration/RelationshipBinaryUpgradeMigration 1 n; \n");
         writer.write(" First parameter  = the directory to read the files from\n");
         writer.write(" Second Parameter = minimum range of file to start migrating  \n");
         writer.write(" Third Parameter  = maximum range of file to end migrating  \n");
         writer.write("        - value of 'n' means all the files starting from mimimum range\n");           
         writer.write("================================================================================================\n");
         writer.close();
     }
		
}
