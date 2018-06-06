/*
 * DerivationMigrationValidationBase.java program to validate PLC Migration
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 * 
 */

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.productline.ProductLineConstants;
import com.matrixone.apps.productline.ProductLineUtil;

public class DerivationMigrationValidationBase_mxJPO extends emxCommonMigration_mxJPO {
	public static final String RELATIONSHIP_MAIN_PRODUCT = PropertyUtil.getSchemaProperty("relationship_MainProduct");
	public static final String RELATIONSHIP_MAIN_DERIVED = PropertyUtil.getSchemaProperty("relationship_MainDerived");
	public static final String RELATIONSHIP_MANAGED_ROOT = PropertyUtil.getSchemaProperty("relationship_ManagedRoot");	  
	public static final String RELATIONSHIP_SERIESMASTER                   = PropertyUtil.getSchemaProperty("relationship_SeriesMaster");
	public static final String RELATIONSHIP_ASSOCIATE_MANUFACTURINGPLAN                   = PropertyUtil.getSchemaProperty("relationship_AssociatedManufacturingPlans");
	public static final String TYPE_MANUFACTURING_PLAN_MASTER                      = PropertyUtil.getSchemaProperty("type_ManufacturingPlanMaster");
	public static final String TYPE_MANUFACTURING_PLAN                     = PropertyUtil.getSchemaProperty("type_ManufacturingPlan");
	public static final String RELATIONSHIP_DERIVED                     = PropertyUtil.getSchemaProperty("relationship_Derived");
	public static final String ATTRIBUTE_REV_INDEX					= PropertyUtil.getSchemaProperty("attribute_RevIndex");
	public static final String SELECT_REV_INDEX					= "revindex";
	public static final String ATTRIBUTE_NODE_INDEX                     = PropertyUtil.getSchemaProperty("attribute_NodeIndex");
	public static final String ATTRIBUTE_CHILD_NODE_AVAILABLE_INDEX                     = PropertyUtil.getSchemaProperty("attribute_ChildNodeAvailableIndex");
	public static final String ATTRIBUTE_DERIVATION_LEVEL                    = PropertyUtil.getSchemaProperty("attribute_DerivationLevel");
	public static final String ATTRIBUTE_TITLE					= PropertyUtil.getSchemaProperty("attribute_Title");
	public static final String ATTRIBUTE_DERIVED_CONTEXT = PropertyUtil.getSchemaProperty("attribute_DerivedContext");
	public static final String POLICY_ARCHIVED           = PropertyUtil.getSchemaProperty("policy_Archived");
	public static final String POLICY_MANUFACTURING_PLAN                    = PropertyUtil.getSchemaProperty("policy_ManufacturingPlan");
	public static final String SELECT_PREVIOUS_REVISION_ID					= "previous.id";
	public static final String SELECT_NEXT_REVISION_ID					= "next.id";
	public static final String SELECT_FIRST_REVISION_ID					= "first.id";
	public static final String SELECT_LAST_REVISION_ID					= "last.id";
	/**
	 * Default Constructor.
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args holds no arguments
	 * @throws Exception if the operation fails
	 * @grade 0
	 */
	public DerivationMigrationValidationBase_mxJPO(Context context, String[] args) throws Exception {
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
	 */
	public int mxMain(Context context, String[] args) throws Exception{
		if(!context.isConnected()){
			throw new Exception("not supported on desktop client");
		}
		int argsLength = args.length;
		error = "";
		try{
			// writer     = new BufferedWriter(new MatrixWriter(context));
			if(args.length < 1){
				error = "Wrong number of arguments";
				throw new IllegalArgumentException();
			}
			documentDirectory = args[0];

			// documentDirectory does not ends with "/" add it
			String fileSeparator = java.io.File.separator;
			if(documentDirectory != null && !documentDirectory.endsWith(fileSeparator)){
				documentDirectory = documentDirectory + fileSeparator;
			}

		}catch (IllegalArgumentException iExp){
			writer.write("====================================================================\n");
			writer.write(error + " \n");
			writer.write("Validation of Migration FAILED \n");
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
			errorLog = new FileWriter(documentDirectory + "Errors.csv", true);
			errorLog.write("OID,TYPE,NAME,REVISION\n");
			errorLog.flush();
			convertedOidsLog = new FileWriter(documentDirectory + "Converted.csv", true);
			convertedOidsLog.write("OID,TYPE,NAME,REVISION\n");
			convertedOidsLog.flush();
			warningLog = new FileWriter(documentDirectory + "MigrationValidation.log", true);
		}catch(FileNotFoundException fExp){
			// check if user has access to the directory
			// check if directory exists
			writer.write("=================================================================\n");
			writer.write("Directory does not exist or does not have access to the directory\n");
			writer.write("Validation of Migration FAILED \n");
			writer.write("=================================================================\n");
			// if scan is true, writer will be closed by the caller
			if(!scan){
				writer.close();
			}
			return 0;
		}

		int i = 0;
		try{

			String cmd = "trigger off";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd);
			generateObjectIdFiles(context);
			minRange=1;
			maxRange=getTotalFilesInDirectory();

			migrationStartTime = System.currentTimeMillis();

			for(i = minRange;i <= maxRange; i++){
				try{

					mqlLogWriter("Reading file: " + i + "\n");
					StringList objectList = new StringList();
					migratedOids = new StringBuffer(20000);
					try{
						objectList = readFiles(i);
					}catch(FileNotFoundException fnfExp){
						// throw exception if file does not exists
						throw fnfExp;
					}

					validateMigration(context ,objectList);

					mqlLogRequiredInformationWriter("<<< Time taken for Validation of objects & write ConvertedOid.txt for file in milliseconds :" + documentDirectory + "objectids_" + i + ".txt"+ ":=" +(System.currentTimeMillis() - startTime) + ">>>\n");

					// write after completion of each file
					mqlLogRequiredInformationWriter("=================================================================\n");
					mqlLogRequiredInformationWriter("Validation of Objects in file objectids_" + i + ".txt COMPLETE \n");
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

				}catch (Exception exp){
					// abort if identifyModel or migration fail for a specific file
					// continue the migration process for the remaining files
					mqlLogRequiredInformationWriter("=======================================================\n");
					mqlLogRequiredInformationWriter("Validation of Objects in file objectids_" + i + ".txt FAILED \n");
					mqlLogRequiredInformationWriter("=="+ exp.getMessage() +"==\n");
					mqlLogRequiredInformationWriter("=======================================================\n");
					statusBuffer.append("objectids_");
					statusBuffer.append(i);
					statusBuffer.append(".txt,FAILED,");
					statusBuffer.append(failureId);
					statusBuffer.append("\n");
					exp.printStackTrace();

				}
			}

			mqlLogRequiredInformationWriter("=======================================================\n");
			mqlLogRequiredInformationWriter("                Validation of  Objects  COMPLETE\n");
			mqlLogRequiredInformationWriter("                Time: " + (System.currentTimeMillis() - migrationStartTime) + " ms\n");
			mqlLogRequiredInformationWriter(" \n");
			mqlLogRequiredInformationWriter("                 Validation:     SUCCESS \n");
			mqlLogRequiredInformationWriter(" \n");
			mqlLogRequiredInformationWriter("=======================================================\n");

		}catch (FileNotFoundException fEx){

		}
		catch (Exception ex){
			// abort if identifyModel fail
			mqlLogRequiredInformationWriter("=======================================================\n");
			mqlLogRequiredInformationWriter("Validation in file objectids_" + i + ".txt failed \n");
			mqlLogRequiredInformationWriter("Validation of Migration     : FAILED \n");
			mqlLogRequiredInformationWriter("=======================================================\n");
			ex.printStackTrace();

		}finally{
			mqlLogRequiredInformationWriter("<<< Total time taken for Validation in milliseconds :=" + (System.currentTimeMillis() - migrationStartTime) + ">>>\n");
			String cmd = "trigger on";
			MqlUtil.mqlCommand(context, mqlCommand,  cmd);

			statusLog   = new FileWriter(documentDirectory + "fileStatus.csv", true);
			statusLog.write(statusBuffer.toString());
			statusLog.flush();
			statusLog.close();


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

	private void generateObjectIdFiles(Context context) throws Exception{
		try{
			mqlLogRequiredInformationWriter("=======================================================\n");
			mqlLogRequiredInformationWriter("Generating Objectid Files ..\n");
			String str[] = new String[2];
			str[0]="500";
			str[1]=documentDirectory;
			DerivationMigrationFindObjects_mxJPO  derivationMigrationFindObjects = new DerivationMigrationFindObjects_mxJPO(context,str);
			derivationMigrationFindObjects.mxMain(context,str);
			//MqlUtil.mqlCommand(context, "execute prog PLCConfigurationDerivationFindObjects 500 c:\temp");
			mqlLogRequiredInformationWriter("Generation of Objectid files completed.\n"+getTotalFilesInDirectory()+" Files generated\n");
			mqlLogRequiredInformationWriter("=======================================================\n");

		}catch(Exception exception){
			mqlLogRequiredInformationWriter("\nException in method generateObjectIdFiles");
			mqlLogRequiredInformationWriter("\n\n Error Detials : \n");
			mqlLogRequiredInformationWriter(exception.getMessage());
			throw exception; 
		}
	}

	public void validateMigration(Context context ,StringList objectsList) throws Exception{
		try {

			mqlLogRequiredInformationWriter("=======================================================\n");
			mqlLogRequiredInformationWriter("Validation of Migration Started\n");
			mqlLogRequiredInformationWriter("=======================================================\n");

			StringList objectSelect = new StringList();
			objectSelect.addElement(DomainConstants.SELECT_ID);


			if (objectsList!=null && !objectsList.isEmpty()) {
				Iterator<String> objectsListItr = objectsList.iterator();
				while (objectsListItr.hasNext()) {
					String objectId = objectsListItr.next();
					boolean bMigrationSuccessful=true;
					if (objectId!=null && !objectId.isEmpty()) {

						short iLevel = 1;
						int limit = 0;
						boolean getTo = true;            
						boolean getFrom = true;
						String strObjWhere = DomainConstants.EMPTY_STRING;
						String strRelWhere = DomainConstants.EMPTY_STRING;

						StringList slObjSelects = new StringList(ProductLineConstants.SELECT_ID);              
						slObjSelects.add(ProductLineConstants.SELECT_TYPE);
						slObjSelects.add(ProductLineConstants.SELECT_NAME);
						slObjSelects.add(ProductLineConstants.SELECT_REVISION);

						slObjSelects.add(SELECT_REV_INDEX);
						slObjSelects.add(SELECT_PREVIOUS_REVISION_ID);
						slObjSelects.add(SELECT_NEXT_REVISION_ID);
						slObjSelects.add(SELECT_FIRST_REVISION_ID);
						slObjSelects.add(SELECT_LAST_REVISION_ID);
						slObjSelects.add(SELECT_LAST_REVISION_ID);


						slObjSelects.add("from["+RELATIONSHIP_MAIN_PRODUCT+"].to.id");
						slObjSelects.add("from["+RELATIONSHIP_MAIN_PRODUCT+"].to.previous.id");
						slObjSelects.add("from["+RELATIONSHIP_MAIN_PRODUCT+"].to.next.id");

						slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.id");
						slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.previous.id");
						slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.next.id");

						slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.id");
						slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.previous.id");
						slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.next.id");

						slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.from["+RELATIONSHIP_MAIN_DERIVED+"].to.id");
						slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.from["+RELATIONSHIP_MAIN_DERIVED+"].to.previous.id");
						slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.from["+RELATIONSHIP_MAIN_DERIVED+"].to.next.id");

						slObjSelects.add("from["+RELATIONSHIP_MAIN_DERIVED+"].to.id");
						slObjSelects.add("from["+RELATIONSHIP_MAIN_DERIVED+"].to.previous.id");
						slObjSelects.add("from["+RELATIONSHIP_MAIN_DERIVED+"].to.next.id");

						slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"]");
						slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].to.attribute["+ProductLineConstants.ATTRIBUTE_IS_VERSION+"]");
						
						StringList slRelSelects = new StringList(ProductLineConstants.SELECT_RELATIONSHIP_ID);
						slRelSelects.add(ProductLineConstants.SELECT_RELATIONSHIP_TYPE);

						StringBuffer strTypePattern = new StringBuffer(TYPE_MANUFACTURING_PLAN_MASTER);
						strTypePattern.append(",");
						strTypePattern.append(ProductLineConstants.TYPE_PRODUCTS);


						StringBuffer strRelPattern = new StringBuffer(RELATIONSHIP_SERIESMASTER);
						strRelPattern.append(",");
						strRelPattern.append(ProductLineConstants.RELATIONSHIP_MAIN_PRODUCT);
						strRelPattern.append(",");
						strRelPattern.append(ProductLineConstants.RELATIONSHIP_PRODUCTS);

						DomainObject objModel = new DomainObject(objectId);
						MapList mapListModel = (MapList)objModel.getRelatedObjects(context, strRelPattern.toString(), strTypePattern.toString(), slObjSelects, slRelSelects, getTo,
								getFrom, iLevel, strObjWhere, strRelWhere, limit);
						MapList productsMapList = new MapList();
						MapList manuPlanMapList = new MapList();
						for(int i=0;i<mapListModel.size();i++){
							Map modelMap = (Map)mapListModel.get(i);
							if(mxType.isOfParentType(context, modelMap.get(SELECT_TYPE).toString(), ProductLineConstants.RELATIONSHIP_PRODUCTS)){
								productsMapList.add(modelMap);
							}else if(mxType.isOfParentType(context, modelMap.get(SELECT_TYPE).toString(), TYPE_MANUFACTURING_PLAN)){
								manuPlanMapList.add(modelMap);
							}
						}
						if(productsMapList!=null){
							bMigrationSuccessful=validateProductMigration(context,productsMapList,objectId);
						}
						if(manuPlanMapList!=null && bMigrationSuccessful){
							bMigrationSuccessful=validateManuPlanMigration(context,manuPlanMapList,objectId);
						}
					}
					if(bMigrationSuccessful){
						mqlLogRequiredInformationWriter("Migration Succesfull for Model "+objectId+"\n");
					}
				}//end of while objectsList
			}

			mqlLogRequiredInformationWriter("=======================================================\n");
			mqlLogRequiredInformationWriter("Validation of Migration Completed\n");
			mqlLogRequiredInformationWriter("=======================================================\n");
		} catch (Exception exception) {
			mqlLogRequiredInformationWriter("\nException in method validateMigration");
			mqlLogRequiredInformationWriter("\n\n Error Detials : \n");
			mqlLogRequiredInformationWriter(exception.getMessage());
			throw exception; 
		}

	}

	private boolean validateProductMigration(Context context,MapList modelMapList,String strModelId) throws Exception{
		boolean bMigrationSuccessful=true;
		try{
			String strMainProductId="";
			String strFirstRevId="";
			String strLastRevId="";

			Map modelRevInfo = new HashMap();

			for(int j=0;j<modelMapList.size();j++){

				Map productMap=(Map)modelMapList.get(j);

				strFirstRevId=productMap.get(SELECT_FIRST_REVISION_ID).toString();
				strLastRevId=productMap.get(SELECT_LAST_REVISION_ID).toString();

				// pre-processing for 'Main Derived' Rel
				if(productMap.containsKey("from["+RELATIONSHIP_MAIN_PRODUCT+"].to.id")){
					strMainProductId=productMap.get("from["+RELATIONSHIP_MAIN_PRODUCT+"].to.id").toString();

					if(strFirstRevId.compareTo(strMainProductId)!=0){
						mqlLogRequiredInformationWriter("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
						mqlLogRequiredInformationWriter("\nMigration Unsuccessful for  :"+ productMap.get(SELECT_ID)+" !!");
						mqlLogRequiredInformationWriter("\nThe product connected to by relationship "+ RELATIONSHIP_MAIN_PRODUCT+" is not first revision\n");
						mqlLogRequiredInformationWriter("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
						bMigrationSuccessful=false;
					}else{
						Map revIdMap = new HashMap();
						revIdMap.put(SELECT_NEXT_REVISION_ID, productMap.get(SELECT_NEXT_REVISION_ID).toString());
						revIdMap.put(SELECT_PREVIOUS_REVISION_ID, productMap.get(SELECT_PREVIOUS_REVISION_ID).toString());
						if(revIdMap.size()>0){
							modelRevInfo.put( productMap.get(SELECT_ID).toString(), revIdMap);
						}
					}
				}else{
					Map revIdMap = new HashMap();
					if(productMap.containsKey(SELECT_NEXT_REVISION_ID)){
						revIdMap.put(SELECT_NEXT_REVISION_ID, productMap.get(SELECT_NEXT_REVISION_ID).toString());
					}
					if(productMap.containsKey(SELECT_PREVIOUS_REVISION_ID)){
						revIdMap.put(SELECT_PREVIOUS_REVISION_ID, productMap.get(SELECT_PREVIOUS_REVISION_ID).toString());
					}
					if(productMap.containsKey("from["+RELATIONSHIP_MAIN_DERIVED+"].to.id")){
						revIdMap.put("from["+RELATIONSHIP_MAIN_DERIVED+"].to.id",productMap.get("from["+RELATIONSHIP_MAIN_DERIVED+"].to.id").toString());
					}
					//If there is no Previous,Next or Main Derived than it is just a single Product connected to Model
					if(revIdMap.size()>0){
						modelRevInfo.put( productMap.get(SELECT_ID).toString(), revIdMap);
					}

				}
				
				// Checking for Derived Rel
				if(productMap.containsKey("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"]")){
					
					Object temp= productMap.get("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].to.attribute["+ProductLineConstants.ATTRIBUTE_IS_VERSION+"]");
					StringList attVersionList = ProductLineUtil.convertObjToStringList(context,temp);
					boolean isVersion=false;
					if(attVersionList.contains("TRUE") ||attVersionList.contains("True") || attVersionList.contains("true") ){
						isVersion=true;
					}
					
					if(isVersion){
						bMigrationSuccessful=false;
						mqlLogRequiredInformationWriter("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
						mqlLogRequiredInformationWriter("\nMigration Unsuccessful for  :"+ productMap.get(SELECT_ID)+" !!");
						mqlLogRequiredInformationWriter("\nThe product connected to by relationship "+ ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+" to a the Product Version\n");
						mqlLogRequiredInformationWriter("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
					}
					
				}
			}
			if(modelRevInfo.size()>0){
				bMigrationSuccessful=checkforMainDerived(strFirstRevId,strLastRevId,modelRevInfo,strModelId);
			}
			if(modelRevInfo.size()>0){
				bMigrationSuccessful=checkforDerived(modelRevInfo,strModelId);
			}
		} catch (Exception exception) {
			mqlLogRequiredInformationWriter("\nException in method validateProductMigration");
			mqlLogRequiredInformationWriter("\n\n Error Detials : \n");
			mqlLogRequiredInformationWriter(exception.getMessage());
			throw exception; 
		}
		return bMigrationSuccessful;
	}
	private boolean validateManuPlanMigration(Context context,MapList modelMapList,String strModelId) throws Exception{
		boolean bMigrationSuccessful=true;
		try{
			String strManagedRootId="";
			String strFirstRevId="";
			String strLastRevId="";
			Map modelRevInfo = new HashMap();

			for(int j=0;j<modelMapList.size();j++){

				Map modelMap=(Map)modelMapList.get(j);
				mqlLogRequiredInformationWriter("\nmodelMap : "+modelMap);

				if(modelMap.containsKey("from["+RELATIONSHIP_MAIN_PRODUCT+"].to.id")){
					strManagedRootId=modelMap.get("from["+RELATIONSHIP_MANAGED_ROOT+"].to.id").toString();
					strFirstRevId=modelMap.get(SELECT_FIRST_REVISION_ID).toString();
					strLastRevId=modelMap.get(SELECT_LAST_REVISION_ID).toString();
					if(strFirstRevId.compareTo(strManagedRootId)!=0){
						mqlLogRequiredInformationWriter("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
						mqlLogRequiredInformationWriter("\nMigration Unsuccessful for  :"+ modelMap.get(SELECT_ID)+" !!");
						mqlLogRequiredInformationWriter("\nThe Manufacturing Plan connected to by relationship "+ RELATIONSHIP_MANAGED_ROOT+" is not first revision\n");
						mqlLogRequiredInformationWriter("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
						bMigrationSuccessful= false;
					}else{
						Map revIdMap = new HashMap();
						revIdMap.put(SELECT_NEXT_REVISION_ID, modelMap.get(SELECT_NEXT_REVISION_ID).toString());
						revIdMap.put(SELECT_PREVIOUS_REVISION_ID, modelMap.get(SELECT_PREVIOUS_REVISION_ID).toString());
						if(revIdMap.size()>0){
							modelRevInfo.put( modelMap.get(SELECT_ID).toString(), revIdMap);
						}
					}
				}else{
					Map revIdMap = new HashMap();
					if(modelMap.containsKey(SELECT_NEXT_REVISION_ID)){
						revIdMap.put(SELECT_NEXT_REVISION_ID, modelMap.get(SELECT_NEXT_REVISION_ID).toString());
					}
					if(modelMap.containsKey(SELECT_PREVIOUS_REVISION_ID)){
						revIdMap.put(SELECT_PREVIOUS_REVISION_ID, modelMap.get(SELECT_PREVIOUS_REVISION_ID).toString());
					}
					if(modelMap.containsKey("from["+RELATIONSHIP_MAIN_DERIVED+"].to.id")){
						revIdMap.put("from["+RELATIONSHIP_MAIN_DERIVED+"].to.id",modelMap.get("from["+RELATIONSHIP_MAIN_DERIVED+"].to.id").toString());
					}
					//If there is no Previous,Next or Main Derived than it is just a single Manufacturing Plan connected to the Master Manufacturing Plan
					if(revIdMap.size()>0){
						modelRevInfo.put( modelMap.get(SELECT_ID).toString(), revIdMap);
					}
				}
			}
			if(modelRevInfo.size()>0){
				bMigrationSuccessful=checkforMainDerived(strFirstRevId,strLastRevId,modelRevInfo,strModelId);
			}
		} catch (Exception exception) {
			mqlLogRequiredInformationWriter("\nException in method validateManuPlanMigration");
			mqlLogRequiredInformationWriter("\n\n Error Detials : \n");
			mqlLogRequiredInformationWriter(exception.getMessage());
			throw exception; 
		}
		return bMigrationSuccessful;
	}

	private boolean checkforMainDerived(String strFirstRevId,String strLastRevId,Map modelRevInfo,String strModelId) throws Exception {
		boolean bMigrationSuccessful=true;
		try{
			Object strNextRevId=((Map)modelRevInfo.get(strFirstRevId)).get(SELECT_NEXT_REVISION_ID);
			Object strDerivedToRevId=((Map)modelRevInfo.get(strFirstRevId)).get("from["+RELATIONSHIP_MAIN_DERIVED+"].to.id");

			if(strNextRevId!=null && strDerivedToRevId!=null && strNextRevId.toString().compareTo(strDerivedToRevId.toString())==0){
				if(strNextRevId.toString().compareTo(strLastRevId)!=0){
					bMigrationSuccessful=checkforMainDerived(strNextRevId.toString(),strLastRevId,modelRevInfo,strModelId);
					if(!bMigrationSuccessful){
						return bMigrationSuccessful;
					}
				}
			}else if (strNextRevId!=null && strDerivedToRevId==null){
				mqlLogRequiredInformationWriter("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
				mqlLogRequiredInformationWriter("\nMigration Unsuccessful for  :"+ strModelId+" !!");
				mqlLogRequiredInformationWriter("\nThe product "+ strNextRevId.toString()+" does not have relationship "+RELATIONSHIP_MAIN_DERIVED+"\n");
				mqlLogRequiredInformationWriter("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
				bMigrationSuccessful=false;
			}else{
				mqlLogRequiredInformationWriter("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
				mqlLogRequiredInformationWriter("\nMigration Unsuccessful for  :"+ strModelId+" !!");
				mqlLogRequiredInformationWriter("\nThe product "+ strNextRevId.toString()+" is not connected to its next revision by relationship "+RELATIONSHIP_MAIN_DERIVED+"\n");
				mqlLogRequiredInformationWriter("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");
				bMigrationSuccessful=false;
			}

		} catch (Exception exception) {
			mqlLogRequiredInformationWriter("\nException in method checkforMainDerived");
			mqlLogRequiredInformationWriter("\n\n Error Detials : \n");
			mqlLogRequiredInformationWriter(exception.getMessage());
			throw exception; 
		}
		return bMigrationSuccessful;
	}

	private boolean checkforDerived(Map modelRevInfo,String strModelId) throws Exception {
		boolean bMigrationSuccessful=true;
		try{

		} catch (Exception exception) {
			mqlLogRequiredInformationWriter("\nException in method checkforMainDerived");
			mqlLogRequiredInformationWriter("\n\n Error Detials : \n");
			mqlLogRequiredInformationWriter(exception.getMessage());
			throw exception; 
		}
		return bMigrationSuccessful;
	}
}



