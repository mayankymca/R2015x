/*
 * CFPMPDerivationMigrationBase.java
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

import matrix.db.*;
import matrix.util.*;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;

import java.io.*;

import com.matrixone.apps.productline.DerivationUtil;
import com.matrixone.apps.productline.ProductLineCommon;
import com.matrixone.apps.productline.ProductLineConstants;
import com.matrixone.apps.productline.ProductLineUtil;


/**
 * The <code>CFPMPDerivationMigrationBase</code> class contains code to migrate Manufacturing Plan structure
 * of the Models found in step1. This needs to be done for supporting Branching in 2012x
 */
  public class ManufacturingPlanDerivationMigrationBase_mxJPO extends emxCommonMigration_mxJPO
  {
	  public static final String RELATIONSHIP_MAIN_DERIVED = PropertyUtil.getSchemaProperty("relationship_MainDerived");
	  public static final String RELATIONSHIP_MANAGED_ROOT = PropertyUtil.getSchemaProperty("relationship_ManagedRoot");
	  public static final String POLICY_ARCHIVED           = PropertyUtil.getSchemaProperty("policy_Archived");
	  public static final String POLICY_MANUFACTURING_PLAN                    = PropertyUtil.getSchemaProperty("policy_ManufacturingPlan");
	  public static final String RELATIONSHIP_SERIESMASTER                   = PropertyUtil.getSchemaProperty("relationship_SeriesMaster");
	  public static final String TYPE_MANUFACTURING_PLAN_MASTER                      = PropertyUtil.getSchemaProperty("type_ManufacturingPlanMaster");
	  public static final String ATTRIBUTE_DERIVED_CONTEXT = PropertyUtil.getSchemaProperty("attribute_DerivedContext");
	  public static final String RELATIONSHIP_ASSOCIATE_MANUFACTURINGPLAN                   = PropertyUtil.getSchemaProperty("relationship_AssociatedManufacturingPlans");
	  public static final String TYPE_MANUFACTURING_PLAN                     = PropertyUtil.getSchemaProperty("type_ManufacturingPlan");
	  public static Map revIndexMap;
	  public static final String ATTRIBUTE_DERIVATION_LEVEL                    = PropertyUtil.getSchemaProperty("attribute_DerivationLevel");
	  public static final String RELATIONSHIP_DERIVED_ABSTRACT   = PropertyUtil.getSchemaProperty("relationship_DERIVED_ABSTRACT");
      public static final String ATTRIBUTE_REV_INDEX					= PropertyUtil.getSchemaProperty("attribute_RevIndex");
	  public static final String RELATIONSHIP_DERIVED                     = PropertyUtil.getSchemaProperty("relationship_Derived");
	  public static final String DELIMITER_PIPE         = "|";
	  
	  /**
      * Default constructor.
      * @param context the eMatrix <code>Context</code> object
      * @param args String array containing program arguments
      * @throws Exception if the operation fails
      */
      public ManufacturingPlanDerivationMigrationBase_mxJPO (Context context, String[] args)
              throws Exception
      {
          super(context, args);
          warningLog = new FileWriter(documentDirectory + "ManufacturingPlanDerivationMigration.log", true);
      }

      /**
       * This method is executed if a specific method is not specified.
       * This method checked for the status property and go ahead with MP migration if find object step is completed.
       * 
       * @param context the eMatrix <code>Context</code> object
       * @param args holds no arguments
       * @returns nothing
       * @throws Exception if the operation fails
       */
      public int mxMain(Context context, String[] args) throws Exception
      {  
    	  boolean bIsException=false;
    	  try{
    		  boolean isDMCInstall = FrameworkUtil.isSuiteRegistered(context,"appVersionDMCPlanning",false,null,null);
    		  String strMqlCommand = "trigger off";
    		  MqlUtil.mqlCommand(context,strMqlCommand,true);
    		  
    		  if(isDMCInstall){
    			  int migrationStatus = getAdminMigrationStatus(context);
    			  super.mxMain(context, args);
    			  /*mqlLogRequiredInformationWriter("Products Derivation Status is: "+ migrationStatus+" \n");

    		   This code needs to be uncommented after phase 2 implementation is done and Pre and Post checks are implemented 
              if(migrationStatus<2)
              {
            	  mqlLogRequiredInformationWriter("Products Derivation PreChecks not executed.Please complete PreChecks before migration. \n");
            	  bIsException=true;
            	  return -1;
              }*/
    		  }
    		  strMqlCommand = "trigger on";
    		  MqlUtil.mqlCommand(context,strMqlCommand,true);

    	  }
    	  catch(Exception e){
    		  bIsException=true;
        	  mqlLogRequiredInformationWriter("\n");
        	  mqlLogRequiredInformationWriter("Manufacturing Plan Migration Failed :: "+e.getMessage());
        	  mqlLogRequiredInformationWriter("\n");
              e.printStackTrace();
              throw e;
    	  }
    	  finally{
      		  if(!bIsException){
      	  		  setAdminMigrationStatus(context,"MigrationCompleted");
      	  		  }
    	  }
    	  
    	  return 0;
      }
      /**
       * Main migration method to handle migrating Manufacturing Plan structure under found Models.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param objectIdList StringList holds list objectids to migrate
       * @returns nothing
       * @throws Exception if the operation fails
       */
      public void  migrateObjects(Context context, StringList objectIdList)throws Exception
      {  
    	  String errorMessage = "";
    	  
          try
          {
        	  String logString = "";        	          	  
          	  boolean foundMPInfo = false;
              String strObjectId = "";
              String strName = "";
              String strID = "";
              String strType = "";
              String strRevision = "";              
              Map infoMap = new Hashtable();
              MapList mapModel;
              Iterator itr;
              short iLevel = 0;
          	  int limit = 0;
          	  String strObjWhere = DomainConstants.EMPTY_STRING;
          	  String strRelWhere = DomainConstants.EMPTY_STRING;
          	                
              StringList slObjSelects = new StringList(ProductLineConstants.SELECT_ID);
              slObjSelects.add(ProductLineConstants.SELECT_NAME);
              slObjSelects.add(ProductLineConstants.SELECT_TYPE);
              slObjSelects.add(ProductLineConstants.SELECT_REVISION);
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.id");                  
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].id");
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].type");
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.previous");  
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.next");  
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.revision");
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.policy");
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.to["+ProductLineConstants.RELATIONSHIP_DERIVED+"].id");
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.revindex");
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.name");
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.type");
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.revision"); 
              slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].id");
              slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.revindex");
              slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.id");
              slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.revision");
              slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.type");
              slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.name");
              slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.to["+RELATIONSHIP_ASSOCIATE_MANUFACTURINGPLAN+"].from.id");
        
                            
              StringList slRelSelects = new StringList(ProductLineConstants.SELECT_RELATIONSHIP_ID);
              
              boolean getTo = true;
              boolean getFrom = false;
              
              StringBuffer strTypePattern = new StringBuffer(TYPE_MANUFACTURING_PLAN_MASTER);
              
              StringBuffer strRelPattern = new StringBuffer(RELATIONSHIP_SERIESMASTER);
                  
              DomainObject domContextBus = new DomainObject();
              
              Iterator iterator = objectIdList.iterator();
              
              mqlLogRequiredInformationWriter("Number Of Model Found In System: "+ objectIdList.size()+" \n");
              
              //For each Model
              while (iterator.hasNext())
              {
            	  try{
            		  
            		  infoMap.clear();
            		  
            		  mqlLogRequiredInformationWriter("\n");
            		  
            		  foundMPInfo = false;                      
            		  
                      strObjectId = (String)iterator.next();
                      
                      mqlLogRequiredInformationWriter("Manufacturing Plan Migration Starts For Model Id= " + strObjectId +" \n");
                                            
                      domContextBus.setId(strObjectId);
                      
                      logString = "Model,"+strObjectId+",,";
                      
                      mapModel = (MapList)domContextBus.getRelatedObjects(context, strRelPattern.toString(), strTypePattern.toString(), slObjSelects, slRelSelects, getTo,
                      		getFrom, iLevel, strObjWhere, strRelWhere, limit);
                      
                      if(mapModel.size() != 0){
                    	  itr = mapModel.iterator();
                          
                          while(itr.hasNext()){
                          	infoMap = (Hashtable)itr.next();
                          	strType = (String)infoMap.get(ProductLineConstants.SELECT_TYPE);
                          	strName = (String)infoMap.get(ProductLineConstants.SELECT_NAME);
                          	strID = (String)infoMap.get(ProductLineConstants.SELECT_ID);
                          	strRevision = (String)infoMap.get(ProductLineConstants.SELECT_REVISION);
                          	
                          	if(ProductLineCommon.isNotNull(strType)){
                          		if(foundMPInfo == false){
                              		foundMPInfo = true;
                          		}
                          		else{
                          			errorMessage = "Found more than 1 Manufacturing Plan Master connected with the Model";
                          			throw new FrameworkException(errorMessage);
                          		}                      		
                          	}                      	
                          }
                                                
                          try{                    	  
                        		migrateManufacturingPlan(context, infoMap);                    	                      	  
                        		
                        		if(strType.compareTo(TYPE_MANUFACTURING_PLAN_MASTER)==0){
                        		// Generate Node Index
                        		DomainConstants.MULTI_VALUE_LIST.clear();
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.id");                  
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].id");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].type");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.previous");  
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.next");  
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.revision");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.policy");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.to["+ProductLineConstants.RELATIONSHIP_DERIVED+"].id");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.revindex");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.name");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.type");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.revision"); 
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+RELATIONSHIP_MANAGED_ROOT+"].id");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.revindex");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.id");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.revision");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.type");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.name");
                        		DomainConstants.MULTI_VALUE_LIST.add("from["+RELATIONSHIP_MANAGED_ROOT+"].to.to["+RELATIONSHIP_ASSOCIATE_MANUFACTURINGPLAN+"].from.id");
                        		
                        		DomainObject domMPMaster = DomainObject.newInstance(context,
                        				strID);
                        		infoMap  =  domMPMaster.getInfo(context,slObjSelects);
                        		DomainConstants.MULTI_VALUE_LIST.clear();
                                mqlLogRequiredInformationWriter("\nCalling migration for Node index for id "+strID+" \n");
                                migrateBranchedMPForNodeIndex(context, infoMap);  
                                mqlLogRequiredInformationWriter("Completed migration for Node index for id "+strID+" \n\n");
                        		}
                          }
                          catch(Exception exception){
                        	  mqlLogRequiredInformationWriter(exception.getMessage());
                        	  mqlLogRequiredInformationWriter("\n \n");
                        	  exception.printStackTrace();
                        	  writeUnconvertedOID(logString+errorMessage+"\n", strObjectId);  
                        	  mqlLogRequiredInformationWriter("\n");
                        	  mqlLogRequiredInformationWriter("########################## Manufacturing Plan Migration Failed For Model id= " + strObjectId + " ##########################");
                        	  mqlLogRequiredInformationWriter("\n");
                          }  
                      }
                      else{
                    	  mqlLogRequiredInformationWriter("Model Does Not Have Any MP Connected To It \n");
                      }
                      
                      loadMigratedOids(strObjectId);                      
                      mqlLogRequiredInformationWriter("Manufacturing Plan Migration Completed For Model id= " + strObjectId);
                      mqlLogRequiredInformationWriter("\n");
                      
                      verifyModelStructure(context, strObjectId); 
            	  }
            	  catch(Exception ex){            		  
                	  mqlLogRequiredInformationWriter(ex.getMessage());
                	  mqlLogRequiredInformationWriter("\n \n");
                	  ex.printStackTrace();
                	  writeUnconvertedOID(logString+errorMessage+"\n", strObjectId);  
                	  mqlLogRequiredInformationWriter("\n");
                	  mqlLogRequiredInformationWriter("########################## Manufacturing Plan Migration Failed For Model id= " + strObjectId + " ##########################");
                	  mqlLogRequiredInformationWriter("\n");
            	  }               
              }
          }
          catch(Exception e)
          {        	  
              throw e;
          }
              
      }


      /**
       * Method to be called from migrateObjects method to migrate Manufacturing Plan Structure below the Model 
       * @param context the eMatrix <code>Context</code> object
       * @param MPMap Map containing information about all the Manufacturing Plan under Master Manufacturing Plan connected with the Model
       * 					 
       * @throws Exception
       */
      private void  migrateManufacturingPlan(Context context, Map MPMap)throws Exception {
    	  
    	  try{
    		  int previousIndex = 0;
    		  int nextIndex = 0;
    		  
    		  StringList MPBusIdList;
              StringList MPConnectionIdList;
              StringList MPConnectionTypeList;              
              StringList MPPreviousInfoList;     
              StringList MPNextInfoList;     
              StringList MPRevisionInfoList;              
              StringList MPPolicyInfoList;
              StringList MPDerivedRelInfoList;              
              StringList MPManagedRootRelInfoList;
              
              String connectionToConvert = "";
              String relType = "";
              String previous;     
              String currentRevision;
              String currentId;
              String previousId;
              String next;
              Object tempObj;
              String strRelDerivedToDisconnect;
              DomainObject previousObj = new DomainObject();
              DomainObject currentObjMP = new DomainObject();
              
              if(MPMap.size() == 0){
            	  mqlLogRequiredInformationWriter("Model Does Not Have Any MP Connected To It \n");
              }
              else{
            	  tempObj = MPMap.get("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.id");
                  MPBusIdList = ProductLineUtil.convertObjToStringList(context, tempObj);
                  
                  tempObj = MPMap.get("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].id");
                  MPConnectionIdList = ProductLineUtil.convertObjToStringList(context, tempObj);
                  
                  tempObj = MPMap.get("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].type");
                  MPConnectionTypeList = ProductLineUtil.convertObjToStringList(context, tempObj);
                  
                  tempObj = MPMap.get("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.previous");
                  MPPreviousInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);
                  
                  tempObj = MPMap.get("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.next");
                  MPNextInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);
                  
                  tempObj = MPMap.get("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.revision");
                  MPRevisionInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);
                          
                  tempObj = MPMap.get("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.policy");
                  MPPolicyInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);
                  
                  tempObj = MPMap.get("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.to["+ProductLineConstants.RELATIONSHIP_DERIVED+"].id");
                  MPDerivedRelInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);
                  
                  tempObj = MPMap.get("from["+RELATIONSHIP_MANAGED_ROOT+"].id");
                  MPManagedRootRelInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);
                  
                  
                  if(MPManagedRootRelInfoList.size() > 0){
                	  mqlLogRequiredInformationWriter("Structure Under This Model Has Already Migrated. Migration Will Skip This Model. \n");
                  }
                  else{
                	//disconnect exsisting Derived relationship(case Create Manufacturing Plan From)
                      for(int i=0; i<MPDerivedRelInfoList.size(); i++){
                    	  strRelDerivedToDisconnect = (String)MPDerivedRelInfoList.get(i);
                    	  if(ProductLineCommon.isNotNull(strRelDerivedToDisconnect)){
                    		  mqlLogRequiredInformationWriter("Disconnecting Derived Relationship:: "+ strRelDerivedToDisconnect +" \n");
                    		  DomainRelationship.disconnect(context, strRelDerivedToDisconnect);
                    	  }                	  
                      }
                      
                      for(int j=0; j<MPPreviousInfoList.size(); j++){
        	              	previous = (String)MPPreviousInfoList.get(j);
        	              	currentId = (String)MPBusIdList.get(j);   
        	              	currentRevision = (String)MPRevisionInfoList.get(j);   
        	              	next = (String)MPNextInfoList.get(j);
        	              	relType = (String)MPConnectionTypeList.get(j);
        	              	
        	              	mqlLogRequiredInformationWriter("Info:: Previous: "+ previous +" Current: "+ currentRevision +" Next: "+ next +" Connected With: "+ relType +"\n");
        	              	
        	              	currentObjMP.setId(currentId);
        	              	
        	              	if(!ProductLineCommon.isNotNull(previous)){
        	              		connectionToConvert = (String)MPConnectionIdList.get(j);
        	              		
        	              		mqlLogRequiredInformationWriter("Found Very First Revision, Will Convert This Relationship To Managed Root:: Id: "+ connectionToConvert +" \n");
        	              	}
        	              	else{
        	              		previousIndex = MPRevisionInfoList.indexOf(previous);
        	              		
        	              		if(previousIndex == -1){	    
        	              			mqlLogRequiredInformationWriter("One or More Revision From MP Revision Chain Is Not Connected With Manufacturing Plan Master \n");
        	              			throw new FrameworkException("One or More Revision From MP Revision Chain Is Not Connected With Manufacturing Plan Master");
        	              		}
        	              		
        	              		//not the highest revision
        	              		if(ProductLineCommon.isNotNull(next)){
        	              			nextIndex = MPRevisionInfoList.indexOf(next);
        	              			if(nextIndex == -1){
        	              				mqlLogRequiredInformationWriter("One Or More Revision From MP Revision Chain Is Not Connected With Manufacturing Plan Master \n");
        		              			throw new FrameworkException("One Or More Revision From MP Revision Chain Is Not Connected With Manufacturing Plan Master");
        		              		}
        	              		}
        	              		
        	              		previousId =  (String)MPBusIdList.get(previousIndex);
        	              		previousObj.setId(previousId);
        	              		
        	              		//for Main Derived relationship
        	              		mqlLogRequiredInformationWriter("Connecting Main Derived Relationship Between : "+ previousId +" And: "+ currentId +"\n");
        	                  	DomainRelationship.connect(context,previousObj, RELATIONSHIP_MAIN_DERIVED, currentObjMP);
        	              	}
        	              	
        	              	//For switching Archive Policy to Manufacturing Plan policy for Archived MP
        	              	if(ProductLineCommon.isNotNull((String)MPPolicyInfoList.get(j)) && MPPolicyInfoList.get(j).toString().equals(POLICY_ARCHIVED) ){
        	              		mqlLogRequiredInformationWriter("Found Archived Policy, Changing It Back For Manufacturing Plan: "+ currentId +" \n");
        	              		currentObjMP.setPolicy(context, POLICY_MANUFACTURING_PLAN);          		
        	              	}
                      }
                      
                      //connect very first Revision to Managed Root relationship
                      if(ProductLineCommon.isNotNull(connectionToConvert)){
                    	  DomainRelationship.setType(context, connectionToConvert, RELATIONSHIP_MANAGED_ROOT);//change it to point to correct property file
                      }
                      
                  }
                  
                      
              }
    	  }
    	  catch(Exception e){
    		  throw e;
    	  }   	  
      }

      
      private void verifyModelStructure(Context context, String modelId)throws Exception {
    	  
    	  FileWriter verifyModelStructureLogFile = null;
    	     	          
    	  short iLevel = 1;
      	  int limit = 0;
      	  String strObjWhere = DomainConstants.EMPTY_STRING;
      	  String strRelWhere = DomainConstants.EMPTY_STRING;
      	                
          StringList slObjSelects = new StringList(ProductLineConstants.SELECT_ID);
          slObjSelects.add(ProductLineConstants.SELECT_NAME);
          slObjSelects.add(ProductLineConstants.SELECT_TYPE);
          slObjSelects.add(ProductLineConstants.SELECT_REVISION);                           
          slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].id");
          slObjSelects.add("from["+RELATIONSHIP_MANAGED_ROOT+"].id");
          slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.id"); 
          slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].type");
          slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.previous");  
          slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.next");  
          slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.revision");
          slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.policy");
          slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.to["+ProductLineConstants.RELATIONSHIP_DERIVED+"].id");
          slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.from["+RELATIONSHIP_MAIN_DERIVED+"].to.id");
                                  
          StringList slRelSelects = new StringList(ProductLineConstants.SELECT_RELATIONSHIP_ID);
          
          boolean getTo = true;
          boolean getFrom = true;
          
          StringBuffer strTypePattern = new StringBuffer(TYPE_MANUFACTURING_PLAN_MASTER);
          strTypePattern.append(",");
          strTypePattern.append(ProductLineConstants.TYPE_PRODUCTS);
          
          StringBuffer strRelPattern = new StringBuffer(RELATIONSHIP_SERIESMASTER);
          strRelPattern.append(",");
          strRelPattern.append(ProductLineConstants.RELATIONSHIP_PRODUCTS);
          
          try{
        	  verifyModelStructureLogFile = new FileWriter(documentDirectory + "verification.log", true);
        	  
        	  verifyModelStructureLogFile.write("Starting Verification for Model Id: "+modelId+" \n");
        	  
        	  DomainObject modelObject = new DomainObject(modelId);
        	  
        	  MapList mapModel = (MapList)modelObject.getRelatedObjects(context, strRelPattern.toString(), strTypePattern.toString(), slObjSelects, slRelSelects, getTo,
                		getFrom, iLevel, strObjWhere, strRelWhere, limit);
        	  
        	  verifyModelStructureLogFile.write("mapModel: "+mapModel + "\n");
          }
          catch(Exception e){
        	  
          }
          finally{
        	  verifyModelStructureLogFile.close();
          }
    	  
    	  
    	  
      }
      
      /**
       * Method to be called from migrateObjects method to migrate Manufacturing Plan Structure below the Model 
       * @param context the eMatrix <code>Context</code> object
       * @param MPMap Map containing information about all the Manufacturing Plan under Master Manufacturing Plan connected with the Model
       * 					 
       * @throws Exception
       */
      private void  migrateBranchedMPForNodeIndex(Context context, Map MPMap)throws Exception {

    	  try{
    		  StringList MPManagedSeriesBusIdList;    		  
    		  StringList MPManagedSeriesRevisionInfoList;
    		  StringList MPManagedSeriesTypeInfoList;
    		  StringList MPManagedSeriesNameInfoList;
    		  StringList MPManagedSeriesRevIndexInfoList;

    		  StringList MPManagedRootRelIdList; 
    		  StringList MPManagedRootBusIdList;
    		  StringList MPManagedRootRevisionInfoList;
    		  StringList MPManagedRootTypeInfoList;
    		  StringList MPManagedRootNameInfoList;
    		  StringList MPManagedRootRevIndexInfoList;
    		  StringList MPManagedRootAMPForList;

    		    		 
    		  String revIndex = "";    		  
    		  String mpmObjectId;
    		  DomainObject mpmObject;

    		  String currentId;
    		  String strRootNodeId;
    		  Object tempObj;

    		  DomainObject dummyRootNodeMP = new DomainObject();
    		  DomainObject rootNodeMP = new DomainObject();

    		  Map attributeMap = new HashMap();
    		  revIndexMap = new HashMap();

    		  if(MPMap.size() == 0){
    			  mqlLogRequiredInformationWriter("Model Does Not Have Any MP Connected To It \n");
    		  }
    		  else{
    			  mpmObjectId = (String)MPMap.get(ProductLineConstants.SELECT_ID);
    			  mpmObject = new DomainObject(mpmObjectId);

    			  tempObj = MPMap.get("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.id");
    			  MPManagedSeriesBusIdList = ProductLineUtil.convertObjToStringList(context, tempObj);

    			  tempObj = MPMap.get("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.revision");
    			  MPManagedSeriesRevisionInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);

    			  tempObj = MPMap.get("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.type");
    			  MPManagedSeriesTypeInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);

    			  tempObj = MPMap.get("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.name");
    			  MPManagedSeriesNameInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);

    			  tempObj = MPMap.get("from["+ProductLineConstants.RELATIONSHIP_MANAGED_REVISION+"].to.revindex");
    			  MPManagedSeriesRevIndexInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);

    			  tempObj = MPMap.get("from["+RELATIONSHIP_MANAGED_ROOT+"].id");
    			  MPManagedRootRelIdList = ProductLineUtil.convertObjToStringList(context, tempObj);
    			  MPManagedRootRelIdList = removeDuplicate(context, MPManagedRootRelIdList);

    			  tempObj = MPMap.get("from["+RELATIONSHIP_MANAGED_ROOT+"].to.id");
    			  MPManagedRootBusIdList = ProductLineUtil.convertObjToStringList(context, tempObj);
    			  MPManagedRootBusIdList = removeDuplicate(context, MPManagedRootBusIdList);

    			  tempObj = MPMap.get("from["+RELATIONSHIP_MANAGED_ROOT+"].to.revision");
    			  MPManagedRootRevisionInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);
    			  MPManagedRootRevisionInfoList = removeDuplicate(context, MPManagedRootRevisionInfoList);

    			  tempObj = MPMap.get("from["+RELATIONSHIP_MANAGED_ROOT+"].to.type");
    			  MPManagedRootTypeInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);
    			  MPManagedRootTypeInfoList = removeDuplicate(context, MPManagedRootTypeInfoList);

    			  tempObj = MPMap.get("from["+RELATIONSHIP_MANAGED_ROOT+"].to.name");
    			  MPManagedRootNameInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);
    			  MPManagedRootNameInfoList = removeDuplicate(context, MPManagedRootNameInfoList);

    			  tempObj = MPMap.get("from["+RELATIONSHIP_MANAGED_ROOT+"].to.revindex");
    			  MPManagedRootRevIndexInfoList = ProductLineUtil.convertObjToStringList(context, tempObj);
    			  MPManagedRootRevIndexInfoList = removeDuplicate(context, MPManagedRootRevIndexInfoList);

    			  tempObj = MPMap.get("from["+RELATIONSHIP_MANAGED_ROOT+"].to.to["+RELATIONSHIP_ASSOCIATE_MANUFACTURINGPLAN+"].from.id");
    			  MPManagedRootAMPForList = ProductLineUtil.convertObjToStringList(context, tempObj);
    			  MPManagedRootAMPForList = removeDuplicate(context, MPManagedRootAMPForList);

    			  if(MPManagedRootBusIdList.size() == 0 && MPManagedSeriesBusIdList.size() != 0){
    				  mqlLogRequiredInformationWriter("\nStructure under this model is not migrated in branching structure. Migration will skip this model \n");
    			  }
    			  else if(MPManagedRootBusIdList.size() > 1){
    				  mqlLogRequiredInformationWriter("\nStructure under this model is invalid, found two 'Managed Root Relationship' connected IDs: "+ MPManagedRootBusIdList +"\n");
    			  }
    			  else{  

    				  //storing old rev index values in a map of map
    				  for(int j=0; j<MPManagedSeriesBusIdList.size(); j++){    					  
    					  currentId = (String)MPManagedSeriesBusIdList.get(j);
    					  revIndex = (String)MPManagedSeriesRevIndexInfoList.get(j);   
    					  mqlLogRequiredInformationWriter("Id: "+ currentId +" Type: "+ MPManagedSeriesTypeInfoList.get(j) +" Name: "+ MPManagedSeriesNameInfoList.get(j) +" Revision: "+ MPManagedSeriesRevisionInfoList.get(j) +" \n");
    					  revIndexMap.put(currentId, revIndex);	  	              	                    		 
    				  }	    

    				  //storing revindex attribute on Object which was connect via Managed Root
    				  strRootNodeId = (String)MPManagedRootBusIdList.get(0);
    				  rootNodeMP.setId(strRootNodeId);
    				  revIndex = (String)MPManagedRootRevIndexInfoList.get(0);					  
    				  revIndexMap.put(strRootNodeId, revIndex);	 
    				  
    				  String rootNodeLevelValue = rootNodeMP.getInfo(context, "attribute["+ATTRIBUTE_DERIVATION_LEVEL+"].value");
    				  Map attributeToSetup = new HashMap();
    				  
    		    	  if(!ProductLineCommon.isNotNull(rootNodeLevelValue)){
    		    		  mqlLogRequiredInformationWriter("Setting Node Index on Root Node Type: "+ MPManagedRootTypeInfoList.get(0) +" Name: "+ MPManagedRootNameInfoList.get(0) +" Revision: "+ MPManagedRootRevisionInfoList.get(0) +" Id: "+ strRootNodeId +" \n");
    		    		  attributeToSetup = DerivationUtil.createDerivedNode(context, null, "Level0", MPManagedRootTypeInfoList.get(0).toString());
    		    	  }

    		    	  mqlLogRequiredInformationWriter("Setting Rev Index on Root Node Type: "+ MPManagedRootTypeInfoList.get(0) +" Name: "+ MPManagedRootNameInfoList.get(0) +" Revision: "+ MPManagedRootRevisionInfoList.get(0) +" Id: "+ strRootNodeId +" \n");
    		    	  attributeToSetup.put(ATTRIBUTE_REV_INDEX, revIndex);
    		    	  
    				  rootNodeMP.setAttributeValues(context, attributeToSetup);
    				  
    				  mqlLogRequiredInformationWriter("revindex property for all MP in system: "+ revIndexMap +" \n");
    					  


    				  mqlLogRequiredInformationWriter("Populating node/rev index on all other nodes in MP structure tree \n");
    				  setNodeIndexAttribute(context, strRootNodeId);

    			  }
    		  }
    	  }
    	  catch(Exception exception){
    		  mqlLogRequiredInformationWriter("\nException in method migrateBranchedMPForNodeIndex");
    		  mqlLogRequiredInformationWriter("\n\n Error Detials : \n");
    		  mqlLogRequiredInformationWriter(exception.getMessage());
    		  throw exception;
    	  }     
      }
      

      private StringList removeDuplicate(Context context, StringList listWithDuplicates){

    	  if(listWithDuplicates != null && listWithDuplicates.size() != 0){
    		  String[] strWithDuplicateArray = new String[listWithDuplicates.size()];
    		  strWithDuplicateArray = (String[])listWithDuplicates.toArray(strWithDuplicateArray);

    		  HashSet setWithoutDuplicate = new HashSet(Arrays.asList(strWithDuplicateArray));

    		  String[] strWithoutDuplicateArray = new String[setWithoutDuplicate.size()];
    		  strWithoutDuplicateArray = (String[])setWithoutDuplicate.toArray(strWithoutDuplicateArray);

    		  return new StringList(strWithoutDuplicateArray);
    	  }
    	  else{
    		  return new StringList();
    	  }    	  
      }
      private void setNodeIndexAttribute(Context context, String objectId) throws Exception{

     	 
    	  String revIndex;
    	  Map attributeToSetup =new HashMap();

    	  StringList attributeSelectable = new StringList();
    	  attributeSelectable.add("from["+RELATIONSHIP_DERIVED_ABSTRACT+"].to.id");
    	  attributeSelectable.add("from["+RELATIONSHIP_DERIVED_ABSTRACT+"].to.attribute["+ATTRIBUTE_DERIVATION_LEVEL+"].value");  
    	  attributeSelectable.add(DomainConstants.SELECT_TYPE);  

    	  String childId;
    	  String level;

    	  DomainObject currentObjMP = new DomainObject();
    	  DomainObject childObject = new DomainObject();

    	  currentObjMP.setId(objectId);
    	  DomainConstants.MULTI_VALUE_LIST.add("from["+RELATIONSHIP_DERIVED_ABSTRACT+"].to.id");
		  DomainConstants.MULTI_VALUE_LIST.add("from["+RELATIONSHIP_DERIVED_ABSTRACT+"].to.attribute["+ATTRIBUTE_DERIVATION_LEVEL+"].value");

    	  Map selectableMap = currentObjMP.getInfo(context, attributeSelectable);
    	  DomainConstants.MULTI_VALUE_LIST.clear();
    	  String strType = selectableMap.get(DomainConstants.SELECT_TYPE).toString();
    	  
		  String strMqlCmd = "print bus $1 select $2 dump $3";
		  String strTemp = MqlUtil.mqlCommand(context, strMqlCmd, true, objectId, "from["+ RELATIONSHIP_MAIN_DERIVED +"].to.id", DELIMITER_PIPE);
          mqlLogRequiredInformationWriter("Child found for MP Id: "+ objectId +" Are: \n"+strTemp);

    	  StringList firstLevelChildList = ProductLineUtil.convertObjToStringList(context, selectableMap.get("from["+RELATIONSHIP_MAIN_DERIVED+"].to.id"));

    	  if(ProductLineUtil.convertObjToStringList(context, selectableMap.get("from["+RELATIONSHIP_DERIVED+"].to.id")).size() > 0){
    		  firstLevelChildList.addAll(ProductLineUtil.convertObjToStringList(context, selectableMap.get("from["+RELATIONSHIP_DERIVED+"].to.id")));
    	  }

    	  StringList childDerivationLevelList = ProductLineUtil.convertObjToStringList(context, selectableMap.get("from["+RELATIONSHIP_MAIN_DERIVED+"].to.attribute["+ATTRIBUTE_DERIVATION_LEVEL+"].value"));

    	  if(ProductLineUtil.convertObjToStringList(context, selectableMap.get("from["+RELATIONSHIP_DERIVED+"].to.attribute["+ATTRIBUTE_DERIVATION_LEVEL+"].value")).size() > 0){
    		  childDerivationLevelList.addAll(ProductLineUtil.convertObjToStringList(context, selectableMap.get("from["+RELATIONSHIP_DERIVED+"].to.attribute["+ATTRIBUTE_DERIVATION_LEVEL+"].value")));
    	  }

    	  mqlLogRequiredInformationWriter("Child found for MP Id: "+ objectId +" Are: "+ firstLevelChildList +" with level: "+ childDerivationLevelList +"\n");

    	  if(firstLevelChildList.size() > 0){
    		  for(int i=0; i<firstLevelChildList.size(); i++){
    			  childId = (String)firstLevelChildList.get(i);
    			  level = (String)childDerivationLevelList.get(i);
    			  
    			  if(!ProductLineCommon.isNotNull(level)){
    				  level = "Level0"; //for the data coming from 2012x.HF4 migration
    				  mqlLogRequiredInformationWriter("Creating Node Index attribute for: "+ childId +" with parent id: "+ objectId +" with level: "+ level +"\n");
        			  attributeToSetup = DerivationUtil.createDerivedNode(context, objectId, level, strType);
    			  }
    			  else{
    				  mqlLogRequiredInformationWriter("Node Index attribute already present, skipping generation of Node Index Attribute for id: "+ childId +" with parent id: "+ objectId +" with level: "+ level +"\n");
    				  attributeToSetup = new HashMap();    				  
    			  }
    			  
    			  revIndex = (String)revIndexMap.get(childId);
    			  mqlLogRequiredInformationWriter("Setting revIndex attribute as the previous revindex property: "+ revIndex +"\n");
    			  attributeToSetup.put(ATTRIBUTE_REV_INDEX, revIndex);
    			  
    			  childObject.setId(childId);
    			  mqlLogRequiredInformationWriter("Setting attribute Map " + "for Product Id: "+ childId + "\n" + attributeToSetup  + "\n");
    			  
    			  childObject.setAttributeValues(context, attributeToSetup);
    			  setNodeIndexAttribute(context, childId);
    		  }
    	  }
    	  else{
    		  return;
    	  }                    
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
  		String cmd = "modify program $1 property $2 value $3";  		
  		MqlUtil.mqlCommand(context, cmd, "eServiceSystemInformation.tcl", "ManufacturingPlanDerivationMigration", strStatus);
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
			   "property[ManufacturingPlanDerivationMigration].value");
  	  
  	    if(result.equalsIgnoreCase("MigrationPreCheckCompleted"))
  		{
  			return 1;
  		}else if(result.equalsIgnoreCase("MigrationCompleted"))
  		{
  			return 2;
  		}else if(result.equalsIgnoreCase("MigrationPostCheckCompleted"))
  		{
  			return 3;
  		}
  	 
  	    return 0;
  	}

      /**
       * Outputs the help for this migration.
       * Note: This method can be run only after running PLCDerivationFindObjects
       * @param context the eMatrix <code>Context</code> object
       * @param args String[] containing the command line arguments
       * @throws Exception if the operation fails
       */
       public void help(Context context, String[] args) throws Exception
      {
    	   //statusJPO.help(context, args);

           if(!context.isConnected())
           {
               throw new FrameworkException("not supported on desktop client");
           }

           writer.write("================================================================================================\n");
           writer.write(" CFP Configuration MP Derivation Migration steps::-  \n");
           writer.write("\n");
           writer.write(" Step1: Create the directory hierarchy like:- \n");     
           writer.write(" C:/Migration/ManufacturingPlanDerivationMigration \n");
           writer.write("\n");
           writer.write(" Step2: Perform Manufacturing Plan Migration \n");
           writer.write(" \n");
           writer.write(" Step2.1: Find all Models which are the first point to get all the Objects those need to be migrated and save them into flat files \n");           
           writer.write(" Example: \n");
           writer.write(" execute program DerivationMigrationFindObjects  10 'C:/Migration/ManufacturingPlanDerivationMigration' ; \n");
           writer.write(" First parameter  = indicates number of object per file \n");
           writer.write(" Second Parameter  = the directory where files should be written \n");
           writer.write(" \n");
           writer.write(" Step2.2: Perform Object Migration \n");
           writer.write(" execute program ManufacturingPlanDerivationMigration  'C:/Migration/ManufacturingPlanDerivationMigration' 1 n ; \n");
           writer.write(" First parameter  = the directory to read the files from\n");
           writer.write(" Second Parameter = minimum range of file to start migrating  \n");
           writer.write(" Third Parameter  = maximum range of file to end migrating  \n");
           writer.write("        - value of 'n' means all the files starting from mimimum range\n");
           writer.write("================================================================================================\n");
           writer.close();       
      }
}
