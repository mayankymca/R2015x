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
import java.util.Iterator;
import java.util.List;
import matrix.db.Context;
import matrix.util.StringList;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.effectivity.EffectivityFramework;

/**
 * The <code>ChangeBinaryUpgradeMigrationBase.java class contains the utilities
 * necessary to migrate the stored binary data for Effectiviy expression to new format. It must be run on all CFF data
 * created prior to V6R2013x.
 */
  public class ChangeBinaryUpgradeMigrationBase_mxJPO extends emxCommonMigration_mxJPO
  {

      /**
      * Default constructor.
      * @param context the eMatrix <code>Context</code> object
      * @param args String array containing program arguments
      * @throws Exception if the operation fails
      */
      public ChangeBinaryUpgradeMigrationBase_mxJPO (Context context, String[] args)
              throws Exception
      {

          super(context, args);
          this.warningLog = new FileWriter(documentDirectory + "ChangeBinaryUpgradeMigration.log", true);

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
      	  boolean bIsException=false;
          warningLog = new FileWriter(documentDirectory + "ChangeBinaryUpgradeMigration.log", true);
/* Uncomment the code once migration checks are implemented
          if(migrationStatus < 2)
          {
        	  mqlLogRequiredInformationWriter("Migration pre-check is not complete.Please complete migration pre-check before running CFF Binary Upgrade migration. \n");
        	  bIsException=true;
        	  return;
          }*/
          try
          {
        	  //STEP1 - turn triggers off
    		  String strMqlCommand = "trigger off";
    		  MqlUtil.mqlCommand(context,strMqlCommand,true);
    		  mqlLogRequiredInformationWriter("Change Binary Upgrade Migration is in Process \n");
        		  
    		  //STEP 2 - migrate binary on Named Effectivity objects
    		  refreshEffectivityOnChange(context, objectIdList);                                            

    		  //STEP 3 - turn triggers on
    		  strMqlCommand = "trigger on";
    		  MqlUtil.mqlCommand(context,strMqlCommand,true);
 
    		  mqlLogRequiredInformationWriter("Change Binary Upgrade Migration Complete \n");
          }
          catch(Exception ex)
          {
              ex.printStackTrace();
              bIsException=true;
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
						   "eServiceSystemInformation.tcl",
						   "property[ChangeBinaryUpgradeMigration].value");
						   
  	   
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
				   "eServiceSystemInformation.tcl","ChangeBinaryUpgradeMigration",strStatus);
				   
	}


	/**
	 * To refresh the expression and binary on Named Effectivity 
	 * 
	 * @param context
	 * @param listNEObjects
	 * @param lisChangeObjects
	 * @throws Exception
	 */
	private void refreshEffectivityOnChange(Context context, List listNEObjects) throws Exception{

		StringList objSelects = new StringList(3);
		objSelects.add(DomainConstants.SELECT_ID);
		objSelects.add(EffectivityFramework.SELECT_ATTRIBUTE_EFFECTIVITY_EXPRESSION);
		objSelects.add(EffectivityFramework.SELECT_ATTRIBUTE_EFFECTIVITY_VARIABLE_INDEXES);
		EffectivityFramework effObject = new EffectivityFramework();
		try 
		{	
			convertedOidsLog.write("Executing refreshEffectivityOnChange()...\n");
			String [] objectsArray = new String[listNEObjects.size()];
			objectsArray = (String[])listNEObjects.toArray(objectsArray);
			MapList changeObjectMaplist = DomainObject.getInfo(context, objectsArray, objSelects);
			effObject.updateEffectivityOnChange(context, changeObjectMaplist);
			for (Iterator itr=listNEObjects.iterator();itr.hasNext();)
			{
				convertedOidsLog.write(itr.next() + "\n");
			}
			convertedOidsLog.write("End refreshEffectivityOnChange()\n");
		}catch(Exception ex)
		{
			ex.printStackTrace();
			throw new FrameworkException("refreshEffectivityOnChange() fails:" + ex.getMessage());
		}

	}
	/**
	 * 
	 * @param context
	 * @param obj
	 * @return
	 * @throws FrameworkException
	 */
	private static StringList convertObjToStringList(Context context, Object obj) throws Exception{
		StringList strLst = new StringList();
		try {
			if(obj!=null){
				if(obj instanceof StringList){
					strLst = (StringList) obj;
				}else if(obj instanceof String){
					strLst.add((String) obj);
				}
			}
		} catch (Exception e) {
			throw new FrameworkException(e.getMessage());
		}		
		return strLst;
	}
	
	/**
	 * This is a utility method to check for Null
	 * 
	 * @param context
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	public static boolean isObjectNullOrEmpty(Object obj) throws FrameworkException{
		boolean bResult = false;
		if(obj==null || "".equals(obj)){
			bResult = true;
		}
		return bResult;
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
         writer.write(" C:/Migration/ChangeBinaryUpgradeMigration \n");           
         writer.write("\n");
         writer.write(" Step2: Perform  Migration \n");    
         writer.write(" \n");
         writer.write(" Step2.1: Find all objects that need binary generated and save the ids to a list of files \n");
         writer.write(" Example: \n");
         writer.write(" execute program ChangeBinaryUpgradeMigrationFindObjects  1000 C:/Migration/ChangeBinaryUpgradeMigration ; \n");
         writer.write(" First parameter  = indicates number of object per file \n");
         writer.write(" Second Parameter  = the directory where files should be written \n");
         writer.write(" \n");
         writer.write(" Step2.2: Generate new Change Binary \n");
         writer.write(" Example: \n");
         writer.write(" execute program  ChangeBinaryUpgradeMigration  C:/Migration/ChangeBinaryUpgradeMigration 1 n; \n");
         writer.write(" First parameter  = the directory to read the files from\n");
         writer.write(" Second Parameter = minimum range of file to start migrating  \n");
         writer.write(" Third Parameter  = maximum range of file to end migrating  \n");
         writer.write("        - value of 'n' means all the files starting from mimimum range\n");           
         writer.write("================================================================================================\n");
         writer.close();
     }	
}
