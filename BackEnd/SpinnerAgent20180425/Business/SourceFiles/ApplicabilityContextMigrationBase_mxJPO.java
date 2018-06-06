/*
 * ApplicabilityContextMigrationBase.java program to get all document type Object Ids.
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 * static const char RCSID[] = $Id: /ENOEnterpriseChange/ENOECHJPO.mj/src/${CLASS:ApplicabilityContextMigrationBase}.java 1.1.1.1 Thu Oct 28 22:27:16 2010 GMT przemek Experimental$
 */

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.enterprisechange.Decision;
import com.matrixone.apps.enterprisechange.EnterpriseChangeConstants;

public class ApplicabilityContextMigrationBase_mxJPO extends emxCommonMigration_mxJPO {

	/**
	 * Default Constructor.
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args holds no arguments
	 * @throws Exception if the operation fails
	 * @since EnterpriseChange R212_HFDerivations
	 * @grade 0
	 */
	public ApplicabilityContextMigrationBase_mxJPO(Context context, String[] args) throws Exception {
		super(context, args);
		writer     = new BufferedWriter(new MatrixWriter(context));
		mqlCommand = MqlUtil.getMQL(context);
	}

	/**
	 * This method is executed if a specific method is not specified.
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args holds no arguments
	 * @returns nothing
	 * @throws Exception if the operation fails
	 * @since EnterpriseChange R212_HFDerivations
	 */
	public int mxMain(Context context, String[] args) throws Exception{
		if(!context.isConnected()){
			throw new Exception("not supported on desktop client");
		}
		int argsLength = args.length;
		error = "";
		try{
			// writer     = new BufferedWriter(new MatrixWriter(context));
			if(args.length < 3){
				error = "Wrong number of arguments";
				throw new IllegalArgumentException();
			}
			documentDirectory = args[0];

			// documentDirectory does not ends with "/" add it
			String fileSeparator = java.io.File.separator;
			if(documentDirectory != null && !documentDirectory.endsWith(fileSeparator)){
				documentDirectory = documentDirectory + fileSeparator;
			}

			minRange = Integer.parseInt(args[1]);

			if("n".equalsIgnoreCase(args[2])){
				maxRange = getTotalFilesInDirectory();
			}else{
				maxRange = Integer.parseInt(args[2]);
			}

			if(minRange > maxRange){
				error = "Invalid range for arguments, minimum is greater than maximum range value";
				throw new IllegalArgumentException();
			}

			if(minRange == 0 || minRange < 1 || maxRange == 0 || maxRange < 1){
				error = "Invalid range for arguments, minimum/maximum range value is 0 or negative";
				throw new IllegalArgumentException();
			}
		}catch (IllegalArgumentException iExp){
			writer.write("====================================================================\n");
			writer.write(error + " \n");
			writer.write("Step 2 of Migration :     FAILED \n");
			writer.write("====================================================================\n");
			// if scan is true, writer will be closed by the caller
			if(!scan){
				writer.close();
			}
			return 0;
		}

		String debugString = "false";

		if(argsLength >= 4){
			debugString = args[3];
			if("debug".equalsIgnoreCase(debugString)){
				debug = true;
			}
		}
		try{
			errorLog = new FileWriter(documentDirectory + "unConvertedIds.csv", true);
			errorLog.write("OID,TYPE,NAME,REVISION\n");
			errorLog.flush();
			convertedOidsLog = new FileWriter(documentDirectory + "convertedIds.csv", true);
			convertedOidsLog.write("OID,TYPE,NAME,REVISION\n");
			convertedOidsLog.flush();
			warningLog = new FileWriter(documentDirectory + "ApplicabilityContextMigration.log", true);
		}catch(FileNotFoundException fExp){
			// check if user has access to the directory
			// check if directory exists
			writer.write("=================================================================\n");
			writer.write("Directory does not exist or does not have access to the directory\n");
			writer.write("Step 2 of Migration :     FAILED \n");
			writer.write("=================================================================\n");
			// if scan is true, writer will be closed by the caller
			if(!scan){
				writer.close();
			}
			return 0;
		}

		int i = 0;
		boolean bIsException=false;
		try{
			ContextUtil.pushContext(context);
			String cmd = "trigger off";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd);
			mqlLogRequiredInformationWriter("=======================================================\n\n");
			mqlLogRequiredInformationWriter("                Migrating Objects...\n");
			mqlLogRequiredInformationWriter("                File (" + minRange + ") to (" + maxRange + ")\n");
			mqlLogRequiredInformationWriter("                Reading files from: " + documentDirectory + "\n");
			mqlLogRequiredInformationWriter("                Objects which have be migrated will be written to:  convertedIds.csv\n");
			mqlLogRequiredInformationWriter("                Objects which cannot be migrated will be written to:  unConvertedIds.csv\n");
			mqlLogRequiredInformationWriter("                Logging of this Migration will be written to: migration.log\n\n");
			mqlLogRequiredInformationWriter("=======================================================\n\n");
			migrationStartTime = System.currentTimeMillis();
			statusBuffer.append("File Name, Status, Object Failed (OR) Time Taken in MilliSec\n");
			for(i = minRange;i <= maxRange; i++){
				try{
					int migrationStatus = getAdminMigrationStatus(context);
			      	  
			          /* Uncomment the code once migration checks are implemented
			          if(migrationStatus < 2)
			          {
			        	  mqlLogRequiredInformationWriter("Migration pre-check is not complete.Please complete migration pre-check before running CFF Binary Upgrade migration. \n");
			        	  bIsException=true;
			        	  return;
			          }*/
					ContextUtil.startTransaction(context,true);
					mqlLogWriter("Reading file: " + i + "\n");
					StringList objectList = new StringList();
					migratedOids = new StringBuffer(20000);
					try{
						objectList = readFiles(i);
					}catch(FileNotFoundException fnfExp){
						// throw exception if file does not exists
						throw fnfExp;
					}
					migrateObjects(context, objectList);
					ContextUtil.commitTransaction(context);
	
					mqlLogRequiredInformationWriter("<<< Time taken for migration of objects & write ConvertedOid.txt for file in milliseconds :" + documentDirectory + "objectids_" + i + ".txt"+ ":=" +(System.currentTimeMillis() - startTime) + ">>>\n");

					// write after completion of each file
					mqlLogRequiredInformationWriter("=================================================================\n");
					mqlLogRequiredInformationWriter("Migration of Decision in file objectids_" + i + ".txt COMPLETE \n");
					statusBuffer.append("objectids_");
					statusBuffer.append(i);
					statusBuffer.append(".txt,COMPLETE,");
					statusBuffer.append((System.currentTimeMillis() - startTime));
					statusBuffer.append("\n");
					mqlLogRequiredInformationWriter("=================================================================\n");
				}catch(FileNotFoundException fnExp){
					// log the error and proceed with migration for remaining files
					mqlLogRequiredInformationWriter("=================================================================\n");
					mqlLogRequiredInformationWriter("File objectids_" + i + ".txt does not exist \n");
					mqlLogRequiredInformationWriter("=================================================================\n");
					bIsException=true;
					ContextUtil.abortTransaction(context);
				}catch (Exception exp){
					// abort if identifyModel or migration fail for a specific file
					// continue the migration process for the remaining files
					bIsException=true;
					mqlLogRequiredInformationWriter("=======================================================\n");
					mqlLogRequiredInformationWriter("Migration of Decision in file objectids_" + i + ".txt FAILED \n");
					mqlLogRequiredInformationWriter("=="+ exp.getMessage() +"==\n");
					mqlLogRequiredInformationWriter("=======================================================\n");
					statusBuffer.append("objectids_");
					statusBuffer.append(i);
					statusBuffer.append(".txt,FAILED,");
					statusBuffer.append(failureId);
					statusBuffer.append("\n");
					exp.printStackTrace();
					ContextUtil.abortTransaction(context);
				}
			}

			mqlLogRequiredInformationWriter("=======================================================\n");
			mqlLogRequiredInformationWriter("                Migrating Decision Objects  COMPLETE\n");
			mqlLogRequiredInformationWriter("                Time: " + (System.currentTimeMillis() - migrationStartTime) + " ms\n");
			mqlLogRequiredInformationWriter(" \n");
			mqlLogRequiredInformationWriter("Step 2 of Migration :     SUCCESS \n");
			mqlLogRequiredInformationWriter(" \n");
			mqlLogRequiredInformationWriter("                Objects which have be migrated will be written to:  convertedIds.csv\n");
			mqlLogRequiredInformationWriter("                Objects which cannot be migrated will be written to:  unConvertedIds.csv\n");
			mqlLogRequiredInformationWriter("                Logging of this Migration will be written to: migration.log\n\n");
			mqlLogRequiredInformationWriter("=======================================================\n");
		}catch (FileNotFoundException fEx){
			bIsException=true;
			ContextUtil.abortTransaction(context);
		}
		catch (Exception ex){
			// abort if identifyModel fail
			bIsException=true;
			mqlLogRequiredInformationWriter("=======================================================\n");
			mqlLogRequiredInformationWriter("Migration of Decision in file objectids_" + i + ".txt failed \n");
			mqlLogRequiredInformationWriter("Step 2 of Migration     : FAILED \n");
			mqlLogRequiredInformationWriter("=======================================================\n");
			ex.printStackTrace();
			ContextUtil.abortTransaction(context);
		}finally{
    		  if(!bIsException){
      	  		  setAdminMigrationStatus(context,"MigrationCompleted");
      	  		}
			mqlLogRequiredInformationWriter("<<< Total time taken for migration in milliseconds :=" + (System.currentTimeMillis() - migrationStartTime) + ">>>\n");
			String cmd = "trigger on";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd);

			statusLog   = new FileWriter(documentDirectory + "fileStatus.csv", true);
			statusLog.write(statusBuffer.toString());
			statusLog.flush();
			statusLog.close();

			ContextUtil.popContext(context);
			// if scan is true, writer will be closed by the caller
			if(!scan){
				writer.close();
			}
			errorLog.close();
			warningLog.close();
			convertedOidsLog.close();
		}

		// always return 0, even this gives an impression as success
		// this way, matrixWriter writes to console
		// else writer.write statements do not show up in Application console
		// but it works in mql console
		return 0;
	}
	
	public void migrateObjects(Context context, StringList objectsList) throws Exception {
		try {
			if (objectsList!=null && !objectsList.isEmpty()) {
				Iterator<String> objectsListItr = objectsList.iterator();
				while (objectsListItr.hasNext()) {
					String objectId = objectsListItr.next();
					if (objectId!=null && !objectId.isEmpty()) {
						DomainObject domObject = new DomainObject(objectId);
						if (domObject.isKindOf(context, EnterpriseChangeConstants.TYPE_DECISION)) {
							this.migrateApplicabilityContext(context, objectId);
							
						} else {
							logUnMigratedObjectIds(context, objectId, false);
						}
					}
				}//end of while objectsList
			}
		} catch (Exception e) {
			throw e;
		}
	}
	
	public void migrateApplicabilityContext(Context context, String objectId) throws Exception {
		try {
			boolean hasBeenMigrated = false;
			if(debug){
				mqlLogRequiredInformationWriter("\n Processing Object with ID : "+objectId);
			}
			Decision decision = new Decision(objectId);
			
			if(debug){
				mqlLogRequiredInformationWriter(System.currentTimeMillis()+" Invoking decision.getApplicabilitySummary()\n");
			}
			Map<String,Map<String,Map<String,MapList>>> applicableItemsSortedByMastersAndDisciplinesAndTypes = decision.getApplicabilitySummary(context, null, null, null);
			if(debug){
				mqlLogRequiredInformationWriter(System.currentTimeMillis()+" applicableItemsSortedByMastersAndDisciplinesAndTypes.size :"+applicableItemsSortedByMastersAndDisciplinesAndTypes.size()+"\n");
			}
			if (applicableItemsSortedByMastersAndDisciplinesAndTypes!=null && !applicableItemsSortedByMastersAndDisciplinesAndTypes.isEmpty()) {
				StringList mastersList = new StringList();
				Set<String> masterKeys = applicableItemsSortedByMastersAndDisciplinesAndTypes.keySet();
				Iterator<String> masterKeysItr = masterKeys.iterator();
				while(masterKeysItr.hasNext()){
					String masterKey = masterKeysItr.next();
					if (masterKey!=null && !masterKey.isEmpty()) {
						mastersList.addElement(masterKey);
					}
				}//End of while masterKeys
				
				if (mastersList!=null && !mastersList.isEmpty()) {
					
					StringList objectSelect = new StringList();
					objectSelect.addElement(DomainConstants.SELECT_ID);
					objectSelect.addElement("from[" + EnterpriseChangeConstants.RELATIONSHIP_IMPACTED_OBJECT + "].to.id");
					
					MapList relatedChangeTasks = decision.getRelatedObjects(context,
							EnterpriseChangeConstants.RELATIONSHIP_DECISION_APPLIES_TO,	// relationship pattern
							EnterpriseChangeConstants.TYPE_CHANGE_TASK,					// object pattern
							objectSelect,					// object selects
							new StringList(DomainConstants.SELECT_RELATIONSHIP_ID),		// relationship selects
							false,														// to direction
							true,														// from direction
							(short) 1,													// recursion level
							DomainConstants.EMPTY_STRING,								// object where clause
							DomainConstants.EMPTY_STRING,								// relationship where clause
							0);
					
					if(relatedChangeTasks!=null && relatedChangeTasks.size()>0){
					Iterator<Map<String,Object>> relatedChangeTasksItr = relatedChangeTasks.iterator();
					
					while (relatedChangeTasksItr.hasNext()) {
						StringList impactedObjects = new StringList();
						Map<String,Object> relatedChangeTask = relatedChangeTasksItr.next();
						if (relatedChangeTask!=null && !relatedChangeTask.isEmpty()) {
							String relatedChangeTaskId = (String) relatedChangeTask.get(DomainConstants.SELECT_ID);
							Object relatedChangeTasksImpactedObjects = relatedChangeTask.get("from[" + EnterpriseChangeConstants.RELATIONSHIP_IMPACTED_OBJECT + "].to.id");
							if (relatedChangeTaskId!=null && !relatedChangeTaskId.isEmpty()) {
								if (relatedChangeTasksImpactedObjects!=null) {
									Class<?> relatedChangeTasksImpactedObjectsClass = relatedChangeTasksImpactedObjects.getClass();
									if (relatedChangeTasksImpactedObjectsClass.equals(String.class)) {
										impactedObjects.addElement((String)relatedChangeTasksImpactedObjects);
									} else if (relatedChangeTasksImpactedObjectsClass.equals(StringList.class)) {
										impactedObjects.addAll((StringList) relatedChangeTasksImpactedObjects);
									}
								}
								if(debug){
									mqlLogRequiredInformationWriter(System.currentTimeMillis()+" Impacted Objects List"+impactedObjects+"\n");
								}
								Iterator<String> mastersListItr = mastersList.iterator();
								while (mastersListItr.hasNext()) {
									String master = mastersListItr.next();
									if (master!=null && !master.isEmpty()) {
										if (!impactedObjects.contains(master) || impactedObjects.isEmpty()) {
											//Need to create Relationship
											if(debug){
												mqlLogRequiredInformationWriter(System.currentTimeMillis()+" Connecting "+relatedChangeTaskId+" with "+master+" with Rel "+EnterpriseChangeConstants.RELATIONSHIP_IMPACTED_OBJECT+"\n");
											}
											DomainRelationship.connect(context, new DomainObject(relatedChangeTaskId), EnterpriseChangeConstants.RELATIONSHIP_IMPACTED_OBJECT, new DomainObject(master));
											hasBeenMigrated = true;
										}
									}
								}
							}
						}
					}
				}
				}
			}
			if (hasBeenMigrated) {
				logMigratedObjectIds(context, objectId);
			} else {
				logUnMigratedObjectIds(context, objectId, true);
			}
		} catch (Exception exception) {
			mqlLogRequiredInformationWriter("Exception in method migrateApplicabilityContext "+exception.getMessage());
			exception.printStackTrace();
			logUnMigratedObjectIds(context, objectId, false);
			throw exception;
		}
	}
	

	
    private void logMigratedObjectIds(Context context, String objectId) throws Exception{
        DomainObject domObj = new DomainObject(objectId);
    	convertedOidsLog.write(objectId+",");
    	convertedOidsLog.write(domObj.getInfo(context, DomainConstants.SELECT_TYPE)+",");
    	convertedOidsLog.write(domObj.getInfo(context, DomainConstants.SELECT_NAME)+",");
    	convertedOidsLog.write(domObj.getInfo(context, DomainConstants.SELECT_REVISION)+",");
        convertedOidsLog.write(" \n");
        convertedOidsLog.flush();
    }

    private void logUnMigratedObjectIds(Context context, String objectId, Boolean Migrated) throws Exception{
        DomainObject domObj = new DomainObject(objectId);
        errorLog.write(objectId+",");
        errorLog.write(domObj.getInfo(context, DomainConstants.SELECT_TYPE)+",");
        errorLog.write(domObj.getInfo(context, DomainConstants.SELECT_NAME)+",");
        errorLog.write(domObj.getInfo(context, DomainConstants.SELECT_REVISION)+",");
		if(Migrated){
		errorLog.write("Already in Migrated state"+",");
		}
		else{
		errorLog.write("Error in migration"+",");
		}
        errorLog.write(" \n");
        errorLog.flush();
    }

    private void logMigratedRelIds(Context context, String objectId) throws Exception{
        DomainObject domObj = new DomainObject(objectId);
    	convertedOidsLog.write(objectId+",");
    	convertedOidsLog.write(domObj.getInfo(context, DomainConstants.SELECT_TYPE)+",");
    	convertedOidsLog.write(domObj.getInfo(context, DomainConstants.SELECT_NAME)+",");
    	convertedOidsLog.write(domObj.getInfo(context, DomainConstants.SELECT_REVISION)+",");
        convertedOidsLog.write(" \n");
        convertedOidsLog.flush();
    }

    private void logUnMigratedRelIds(Context context, String relId) throws Exception{
        DomainObject domObj = new DomainObject(relId);
        errorLog.write(relId);
        errorLog.write(" \n");
        errorLog.flush();
    }
    /**
  	 * Gets the migration status as an integer value.  Used to enforce an order of migration.
       * @param context the eMatrix <code>Context</code> object
   	 * @return integer representing the status
  	 * @throws Exception
  	 */
  	public int getAdminMigrationStatus(Context context) throws Exception
  	{

		//String result =	MqlUtil.mqlCommand(context, "print program $1 select $2 dump",
		//				   "eServiceSystemInformation.tcl",
		//				   "property[ApplicabilityContextMigration].value");
		String smqlCmd = "print program $1 select $2 dump";
		String result =	MqlUtil.mqlCommand(context, smqlCmd,
						   "eServiceSystemInformation.tcl",
						   "property[ApplicabilityContextMigration].value");
		
						   
  	   
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
		//MqlUtil.mqlCommand(context, "modify program $1 property $2 value $3",
		//		   "eServiceSystemInformation.tcl","ApplicabilityContextMigration",strStatus);
        String smqlCmd = "modify program $1 property $2 value $3";		
		MqlUtil.mqlCommand(context, smqlCmd,
				   "eServiceSystemInformation.tcl","ApplicabilityContextMigration",strStatus);	
	}
	public void help(Context context, String[] args) throws Exception {
		try{
			if (!context.isConnected()) {
				throw new Exception("not supported on desktop client");
			}

			writer.write("================================================================================================\n");
			writer.write(" Migration is a two step process  \n");
			writer.write(" Step1: Find all objects and write them into flat files \n");
			writer.write(" Example 1: \n");
			writer.write(" FindObjects for enterpriseChange Applicability Context Migration: \n");
			writer.write(" execute program ApplicabilityContextMigrationFindObjects -method findDecisionObjects 1000  C:/Temp/oids; \n");
			writer.write(" First parameter  = 1000 indicates no of oids per file \n");
			writer.write(" Second parameter  = C:/Temp/oids is the directory where files should be written  \n");
			writer.write(" \n");
			writer.write(" Step2: Migrate the objects \n");
			writer.write(" Example: \n");
			writer.write(" execute program ApplicabilityContextMigration 'C:/Temp/oids' 1 n ; \n");
			writer.write(" First parameter  = C:/Temp/oids directory to read the files from\n");
			writer.write(" Second Parameter = 1 minimum range  \n");
			writer.write(" Third Parameter  = n maximum range  \n");
			writer.write("        - value of 'n' means all the files starting from mimimum range\n");
			writer.write("================================================================================================\n");
			writer.close();
		}catch(Exception e){
			throw e;
		}
	}
	
	
	
}



