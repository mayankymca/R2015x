/*
 * ProductsDerivationMigrationBase.java
 * program migrates the Products to Derivation Data Model
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import matrix.db.Context;
import matrix.util.StringList;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.productline.ProductLineCommon;
import com.matrixone.apps.productline.ProductLineConstants;
import com.matrixone.apps.productline.DerivationUtil;
import com.matrixone.apps.productline.ProductLineUtil;


/**
 * The <code>ProductsDerivationMigrationBase</code> class contains code to migrate Product structure
 * of the Models found in ProductsDerivationMigrationFindObjects.java. This needs to be done for supporting Derivation in 2014x
 */
  public class ProductsDerivationMigrationBase_mxJPO extends emxCommonMigration_mxJPO
  {   
	  public static final String RELATIONSHIP_MAIN_PRODUCT = PropertyUtil.getSchemaProperty("relationship_MainProduct");
	  public static final String RELATIONSHIP_MAIN_DERIVED = PropertyUtil.getSchemaProperty("relationship_MainDerived");
	  public static final String RELATIONSHIP_DERIVED                     = PropertyUtil.getSchemaProperty("relationship_Derived");
	  public static final String SELECT_REV_INDEX					= "revindex";
	  public static final String ATTRIBUTE_NODE_INDEX                     = PropertyUtil.getSchemaProperty("attribute_NodeIndex");
	  public static final String ATTRIBUTE_CHILD_NODE_AVAILABLE_INDEX                     = PropertyUtil.getSchemaProperty("attribute_ChildNodeAvailableIndex");
	  public static final String ATTRIBUTE_TITLE					= PropertyUtil.getSchemaProperty("attribute_Title");
	  public static final String ATTRIBUTE_DERIVED_CONTEXT = PropertyUtil.getSchemaProperty("attribute_DerivedContext");
	  public static Map revIndexMap;
	  public static final String SELECT_PREVIOUS_REVISION_ID					= "previous.id";
	  public static final String SELECT_NEXT_REVISION_ID					= "next.id";
	  public static final String SELECT_MINORORDER					= "minororder";
	  public static final String SELECT_MAJORID					= "majorid";
	  public static final String ATTRIBUTE_DERIVATION_LEVEL                    = PropertyUtil.getSchemaProperty("attribute_DerivationLevel");
	  public static final String RELATIONSHIP_DERIVED_ABSTRACT   = PropertyUtil.getSchemaProperty("relationship_DERIVED_ABSTRACT");
      public static final String ATTRIBUTE_REV_INDEX					= PropertyUtil.getSchemaProperty("attribute_RevIndex");

	  /**
      * Default constructor.
      * @param context the eMatrix <code>Context</code> object
      * @param args String array containing program arguments
      * @throws Exception if the operation fails
      */
      public ProductsDerivationMigrationBase_mxJPO (Context context, String[] args)
              throws Exception
      {
          super(context, args);          
          warningLog = new FileWriter(documentDirectory + "ProductDerivationMigration.log", true);
      }

      /**
       * This method is executed if a specific method is not specified.
       * This method checked for the status property and go ahead with  migration if Prechecks are  completed.
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
    		  int migrationStatus = getAdminMigrationStatus(context);
    		  String strMqlCommand = "trigger off";
    		  MqlUtil.mqlCommand(context,strMqlCommand,true);
    		  super.mxMain(context, args);
    		  
    		  /*mqlLogRequiredInformationWriter("Products Derivation Status is: "+ migrationStatus+" \n");
    		  
    		   This code needs to be uncommented after phase 2 implementation is done and Pre and Post checks are implemented 
              if(migrationStatus<2)
              {
            	  mqlLogRequiredInformationWriter("Products Derivation PreChecks not executed.Please complete PreChecks before migration. \n");
            	  bIsException=true;
            	  return -1;
              }*/
    		  strMqlCommand = "trigger on";
    		  MqlUtil.mqlCommand(context,strMqlCommand,true);
    	  }
    	  catch(Exception e){ 
    		  bIsException=true;
        	  mqlLogRequiredInformationWriter("\n");
        	  mqlLogRequiredInformationWriter("Products Derivation Failed :: "+e.getMessage());
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
       * Main migration method to handle migrating Product structure under found Models.
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
        	  Map prodMap;  
        	  Map mpMap;  
        	  
        	  String logString = "";       	          	  
          	  
          	  boolean isProdDerivation = false;

          	  
              String strObjectId = "";
              String strType = ""; 
              String strName = "";
              String strId = "";
              String strRev = "";
              String strRelType = "";
              
              String strPreviousRevisionID = "";    
              String strFirstRevisionID = "";
              
              String strMinororder = "";
              
              Map infoMap = new Hashtable();
              MapList mapModel;
              Iterator itr;
              short iLevel = 0;
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
			  slObjSelects.add(SELECT_MINORORDER);
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].to.id");
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].to.revision");
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].to.type");
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].to.name");
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].to.revindex");
              slObjSelects.add("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].id");
              slObjSelects.add(ProductLineConstants.SELECT_VAULT);
                   
              StringList slRelSelects = new StringList(ProductLineConstants.SELECT_RELATIONSHIP_ID);
              slRelSelects.add(ProductLineConstants.SELECT_RELATIONSHIP_TYPE);
              
              StringBuffer strTypePattern = new StringBuffer(ProductLineConstants.TYPE_PRODUCTS);
              strTypePattern.append(",");
              strTypePattern.append(ProductLineConstants.TYPE_PRODUCT_VERSION);
              
              StringBuffer strRelPattern = new StringBuffer(ProductLineConstants.RELATIONSHIP_MAIN_PRODUCT);
              strRelPattern.append(",");
              strRelPattern.append(ProductLineConstants.RELATIONSHIP_PRODUCTS);
                  
              DomainObject domContextBus = new DomainObject();
              Iterator iterator = objectIdList.iterator();
              
              mqlLogRequiredInformationWriter("Number Of Model(s) Found : "+ objectIdList.size()+" \n");
              
              //For each Model
              while (iterator.hasNext())
              {
            	  try{
            		  mqlLogRequiredInformationWriter("\n");
            		  isProdDerivation = false;
            		  
            		  prodMap = new HashMap();   
            		  
                      strObjectId = (String)iterator.next();
                      
                      mqlLogRequiredInformationWriter("Products Derivation Starts For Model Id= " + strObjectId +" \n");
                                            
                      domContextBus.setId(strObjectId);
                      
                      logString = "Model,"+strObjectId+",,";
                      
                      mapModel = (MapList)domContextBus.getRelatedObjects(context, strRelPattern.toString(), strTypePattern.toString(), slObjSelects, slRelSelects, getTo,
                      		getFrom, iLevel, strObjWhere, strRelWhere, limit);

                      if(mapModel.size() != 0){
                    	  itr = mapModel.iterator();
                          while(itr.hasNext()){
                        	  infoMap = (Hashtable)itr.next();                    	  
                        	  strType = (String)infoMap.get(ProductLineConstants.SELECT_TYPE);
                        	  strId = (String)infoMap.get(ProductLineConstants.SELECT_ID);  
                        	  strRelType = (String)infoMap.get(ProductLineConstants.SELECT_RELATIONSHIP_TYPE); 
                          	  
                        	  if(ProductLineCommon.isNotNull(strType)
                        		 && mxType.isOfParentType(context,strType,ProductLineConstants.TYPE_PRODUCTS)){
                        		  
                        		  strMinororder = (String)infoMap.get(SELECT_MINORORDER);
                        		 Integer intMinororder = new Integer(strMinororder);

                        		  prodMap.put(intMinororder, infoMap);

                        		  if(intMinororder == 0){
                        			  strFirstRevisionID = strId;
                        		  }
                        		  
                        		  if(strRelType.equalsIgnoreCase(ProductLineConstants.RELATIONSHIP_MAIN_PRODUCT)){
                        			  mqlLogRequiredInformationWriter("Product structure under this model is in derivation structure \n");
                        			  isProdDerivation = true;
                        		  }                        		  
                        	  }
                  	  
                          }
                      }
                                            
                	try{  
	                		if(prodMap.size()!=0 && !isProdDerivation){
	                			mqlLogRequiredInformationWriter("Starting the process for Product Derivation Migration and Node Index generation for id "+strId+" \n");
	                			migrateNonDerivationProductsForDerivationAndNodeIndex(context, prodMap, strFirstRevisionID,domContextBus);
	                			mqlLogRequiredInformationWriter("Completed the process for Product Derivation Migration and Node Index generation for id "+strId+" \n");
	                		}
	                		else if(prodMap.size()!=0 && isProdDerivation){
	                			mqlLogRequiredInformationWriter("Starting the process for Node Index generation for id "+strId+"\n");
	                			migrateDerivationProductsForRevIndex(context, prodMap); 
	                			mqlLogRequiredInformationWriter("Completed the process for Node Index generation for id "+strId+" \n");
	                		}
	                		else{
	                    		  mqlLogRequiredInformationWriter("This Model Does Not Have Any Product Connected To It \n");
	                    	  }
	              		  
                	}
                      catch(Exception exception){
                		  mqlLogRequiredInformationWriter("\nException in method migrateObjects");
                		  mqlLogRequiredInformationWriter("\n\n Error Detials : \n");
                		  mqlLogRequiredInformationWriter(exception.getMessage());
                		  throw exception;
                      }
                      
                      loadMigratedOids(strObjectId);                      
                      mqlLogRequiredInformationWriter("Derivation Migration Completed For Model id= " + strObjectId+ "\n");
                      
            	  }
            	  catch(Exception exception){ 
                	  mqlLogRequiredInformationWriter("\n\n!!!!!!!!!!!!!!!!!!!!!!!! Derivation Migration Failed For Model id= " + strObjectId + " !!!!!!!!!!!!!!!!!!!!!!!! \n\n");
            		  mqlLogRequiredInformationWriter("\nException in method migrateObjects");
            		  mqlLogRequiredInformationWriter("\n\n Error Detials : \n");
            		  mqlLogRequiredInformationWriter(exception.getMessage());
                	  exception.printStackTrace();
                	  writeUnconvertedOID(logString+errorMessage+"\n", strObjectId);  

                	  throw exception;
            	  }               
              }
          }
          catch(Exception e)
          {        	  
              throw e;
          }              
      }

      private void  migrateDerivationProductsForRevIndex(Context context, Map prodMap)throws Exception {

    	  try{
    		  String prodId;
    		  String revindex;
    		  DomainObject doProdObj = new DomainObject();
    		  Hashtable infoMap;
    		  Iterator itr = prodMap.keySet().iterator();
    		  
    		  while(itr.hasNext()){
    			  Integer minorOrder = (Integer)itr.next();
    			  infoMap = (Hashtable)prodMap.get(minorOrder);
    			  prodId = (String)infoMap.get(ProductLineConstants.SELECT_ID);
    			  revindex = (String)infoMap.get(SELECT_REV_INDEX);
    			  mqlLogRequiredInformationWriter("Setting attribute "+ ATTRIBUTE_REV_INDEX +" for Product Id: "+ prodId +" value: "+ revindex + "\n");
    			  doProdObj.setId(prodId);
    			  doProdObj.setAttributeValue(context, ATTRIBUTE_REV_INDEX, revindex);
    		  }
    	  }
    	  catch(Exception exception){
    		  mqlLogRequiredInformationWriter("\nException in method migrateDerivationProductsForRevIndex");
    		  mqlLogRequiredInformationWriter("\n\n Error Detials : \n");
    		  mqlLogRequiredInformationWriter(exception.getMessage());
    		  throw exception;
    	  }     
      }
      

      private void migrateNonDerivationProductsForDerivationAndNodeIndex(Context context, Map prodMap, String strFirstRevisionID,DomainObject objModel)throws Exception{

    	  try{

    		  Map attributeToSetup =new HashMap();
    		  String revIndex = "";

    		  if(!ProductLineCommon.isNotNull(strFirstRevisionID)){
    			  throw new FrameworkException("First Revision From Product Revision Chain Is Not Connected With Model");
    		  }
    		  Integer currentRev = 0;
    		  //for Main Product relationship
    		  Map firstProductMap = (Map)prodMap.get(currentRev);
    		  String connectionToConvert = (String)firstProductMap.get(ProductLineConstants.SELECT_RELATIONSHIP_ID);
    		  DomainRelationship.setType(context, connectionToConvert, RELATIONSHIP_MAIN_PRODUCT);

    		  String strIdCurrent =  (String)firstProductMap.get(ProductLineConstants.SELECT_ID);
    		  String strTypeCurrent = (String)firstProductMap.get(ProductLineConstants.SELECT_TYPE);
    		  
    		  Map currentMap = firstProductMap;
    		  Integer nextRev = currentRev + 1;    		
    		  
    		  String strNextRevisionID = "";
    		  Map nextMap;
    		  DomainObject currentObj = new DomainObject();
    		  DomainObject nextObj = new DomainObject();
    		  DomainObject derivationObj = new DomainObject();

    		  Object tempObj;

    		  StringList derivationTypeList; 
    		  StringList derivationIDList; 
    		  StringList derivationRevIndexList;
    		  StringList derivationConnectionList; 

    		  currentObj.setId(strFirstRevisionID);
    		  attributeToSetup = DerivationUtil.createDerivedNode(context, null, "Level0", strTypeCurrent);
    		  
    		  revIndex = (String)firstProductMap.get(SELECT_REV_INDEX);
    		 
    		  attributeToSetup.put(ATTRIBUTE_REV_INDEX, revIndex);
    		  mqlLogRequiredInformationWriter("Setting attribute Map " + "for Product Id: "+ strFirstRevisionID+ "\n" + attributeToSetup  + "\n");
    		  
    		  currentObj.setAttributeValues(context, attributeToSetup);
    		  
    		  while(prodMap.containsKey(nextRev)){
    			  strIdCurrent = (String)currentMap.get(ProductLineConstants.SELECT_ID);
    			  strTypeCurrent = (String)currentMap.get(ProductLineConstants.SELECT_TYPE);
    			  currentObj.setId(strIdCurrent);
    			  nextMap = (Map)prodMap.get(nextRev);
    			  
    			  if(nextMap == null){
    				  mqlLogRequiredInformationWriter("\n\n !!!!!! One or more revision from product revision chain is not connected with Model !!!!!! \n");
    				  throw new FrameworkException("\n\n !!!!!! One or more revision from product revision chain is not connected with Model !!!!!! \n");
    			  }
    			  
    			  strNextRevisionID = (String)nextMap.get(ProductLineConstants.SELECT_ID);
    			  nextObj.setId(strNextRevisionID);
    			  //for Main Derived relationship
    			  mqlLogRequiredInformationWriter("Connect " + strIdCurrent + "to" + strNextRevisionID + "with relationship 'Main Derived'" + "\n");
    			  DomainRelationship.connect(context,currentObj, RELATIONSHIP_MAIN_DERIVED, nextObj);

    			  //for setting NI and revindex attribute
    			  attributeToSetup = DerivationUtil.createDerivedNode(context, strIdCurrent, "Level0", strTypeCurrent);
       			  revIndex = (String)nextMap.get(SELECT_REV_INDEX);
    			  attributeToSetup.put(ATTRIBUTE_REV_INDEX, revIndex);
    			  			  
    			  mqlLogRequiredInformationWriter(" Setting attribute Map " + "for Product Id : "+ strNextRevisionID+ "\n" + attributeToSetup  + "\n");
    			  nextObj.setAttributeValues(context, attributeToSetup);

    			  //For Derived relationship     
    			  String versionType;
    			  tempObj = currentMap.get("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].to.type");
    			  derivationTypeList = ProductLineUtil.convertObjToStringList(context, tempObj);

    			  tempObj = currentMap.get("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].to.id");
    			  derivationIDList = ProductLineUtil.convertObjToStringList(context, tempObj);

    			  tempObj = currentMap.get("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].to.revindex");
    			  derivationRevIndexList = ProductLineUtil.convertObjToStringList(context, tempObj);

    			  tempObj = currentMap.get("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].id");
    			  derivationConnectionList = ProductLineUtil.convertObjToStringList(context, tempObj);

    			  if(derivationTypeList.size() > 0){        		
    				  for(int i=0; i<derivationTypeList.size(); i++){
    					  versionType = (String)derivationTypeList.get(i);
    					  if(!versionType.equals("") && !mxType.isOfParentType(context,versionType,ProductLineConstants.TYPE_PRODUCT_VARIANT)){
    						  DomainRelationship.setType(context, (String)derivationConnectionList.get(i), ProductLineConstants.RELATIONSHIP_DERIVED);
    						  derivationObj.setId((String)derivationIDList.get(i));
    						  attributeToSetup = DerivationUtil.createDerivedNode(context, strIdCurrent, "Level1", strTypeCurrent);
    						  revIndex = (String)derivationRevIndexList.get(i);
    				          attributeToSetup.put(ATTRIBUTE_REV_INDEX, revIndex);
    						  mqlLogRequiredInformationWriter("\nSetting attribute Map " + "for Product Id : "+ (String)derivationIDList.get(i) + " " + attributeToSetup  + "\n");
    						  derivationObj.setAttributeValues(context, attributeToSetup);
    						  DomainRelationship.setAttributeValue(context, (String)derivationConnectionList.get(i), ATTRIBUTE_DERIVED_CONTEXT, strIdCurrent);
    						  // Connects the Version with Products Rel to Model
    						  DomainRelationship.connect(context,objModel,ProductLineConstants.RELATIONSHIP_PRODUCTS, derivationObj);
    					  }
    				  }
    			  }

    			  //For Next Revision if available
    			  currentMap = nextMap;    			  
    			  nextRev++;
    		  }


    		  //For Derived relationship     
    		  String versionType;
    		  tempObj = currentMap.get("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].to.type");
    		  derivationTypeList = ProductLineUtil.convertObjToStringList(context, tempObj);

    		  tempObj = currentMap.get("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].to.id");
    		  derivationIDList = ProductLineUtil.convertObjToStringList(context, tempObj);

    		  tempObj = currentMap.get("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].to.revindex");
    		  derivationRevIndexList = ProductLineUtil.convertObjToStringList(context, tempObj);

    		  tempObj = currentMap.get("from["+ProductLineConstants.RELATIONSHIP_PRODUCT_VERSION+"].id");
    		  derivationConnectionList = ProductLineUtil.convertObjToStringList(context, tempObj);

    		  if(derivationTypeList.size() > 0){        		
    			  for(int i=0; i<derivationTypeList.size(); i++){
    				  versionType = (String)derivationTypeList.get(i);
    				  if(!versionType.equals("") && !mxType.isOfParentType(context,versionType,ProductLineConstants.TYPE_PRODUCT_VARIANT)){
    					  DomainRelationship.setType(context, (String)derivationConnectionList.get(i), ProductLineConstants.RELATIONSHIP_DERIVED);
    					  derivationObj.setId((String)derivationIDList.get(i));
    					  attributeToSetup = DerivationUtil.createDerivedNode(context, strIdCurrent, "Level1", strTypeCurrent);
    					  revIndex = (String)derivationRevIndexList.get(i);
    					  attributeToSetup.put(ATTRIBUTE_REV_INDEX, revIndex);
    					  mqlLogRequiredInformationWriter("\nSetting attribute Map " + "for Product Id: "+ (String)derivationIDList.get(i) + " " + attributeToSetup  + "\n");
    					  derivationObj.setAttributeValues(context, attributeToSetup);
    					  DomainRelationship.setAttributeValue(context, (String)derivationConnectionList.get(i), ATTRIBUTE_DERIVED_CONTEXT, strIdCurrent);
    					// Connects the Version with Products Rel to Model
						  DomainRelationship.connect(context,objModel,ProductLineConstants.RELATIONSHIP_PRODUCTS, derivationObj);    				  
    				  }
    			  }
    		  }
    	  }
    	  catch(Exception exception){
    		  mqlLogRequiredInformationWriter("\nException in method migrateNonDerivationProductsForDerivationAndNodeIndex");
    		  mqlLogRequiredInformationWriter("\n\n Error Detials : \n");
    		  mqlLogRequiredInformationWriter(exception.getMessage());
    		  throw exception;
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

    	  Map selectableMap = currentObjMP.getInfo(context, attributeSelectable);
    	  String strType = selectableMap.get(DomainConstants.SELECT_TYPE).toString();

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
  		String cmd = "modify program eServiceSystemInformation.tcl property ProductsDerivationMigration value "+strStatus;
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
  		String cmd = "print program eServiceSystemInformation.tcl select property[ProductsDerivationMigration].value dump";
  	    String result =	MqlUtil.mqlCommand(context, mqlCommand, cmd);
  	   
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
           writer.write(" PLC Product Derivation Node Index Migration steps::-  \n");
           writer.write("\n");
           writer.write(" Step1: Install the schema for the migration using the mql commands as below :");
           writer.write("\n\t set context user creator;");
           writer.write("\n\t set env REGISTRATIONOBJECT  \"eServiceSchemaVariableMapping.tcl\";");
           writer.write("\n\t run  <Server_Dir>/Apps/{Component Installing PLC}/<version>/Modules/ENOProductLine/AppInstall/Programs/PLCSchemaChangesForDerivationMigration.tcl;");
           writer.write("\n\t compile prog *DerivationMigration* force update;");
           writer.write("\n\n Step2: Create the directory hierarchy like:- \n");
           writer.write(" \tC:/Migration \n");           
           writer.write(" \tC:/Migration/ProductsDerivationMigration \n");
           writer.write("\n");
           writer.write(" Step3: Perform Product Node Index Migration \n");
           writer.write(" \n");
           writer.write(" \tStep3.1: Find all Models which are the first point to get all the Objects to generate Node Index on and save them into flat files \n");           
           writer.write(" \tExample: \n");
           writer.write(" \t\texecute program DerivationMigrationFindObjects 10 'C:/Migration/ProductsDerivationMigration' ; \n");
           writer.write(" \t\tFirst parameter  = indicates number of object per file \n");
           writer.write(" \t\tSecond Parameter  = the directory where files should be written \n");
           writer.write(" \n");
           writer.write(" \tStep3.2: Generate Node Index \n");
           writer.write(" \t\texecute program ProductsDerivationMigration 'C:/Migration/ProductsDerivationMigration' 1 n ; \n");
           writer.write(" \t\tFirst parameter  = the directory to read the files from\n");
           writer.write(" \t\tSecond Parameter = minimum range of file to start migrating  \n");
           writer.write(" \t\tThird Parameter  = maximum range of file to end migrating  \n");
           writer.write(" \t\t- value of 'n' means all the files starting from mimimum range\n");
           writer.write("================================================================================================\n");
           writer.close();        
      }
}
