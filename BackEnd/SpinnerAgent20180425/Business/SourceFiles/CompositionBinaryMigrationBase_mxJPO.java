/*
 * ${CLASS:CompositionBinaryMigrationBase}.java
 * program to generate Composition Binary Data.
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
import matrix.db.MatrixWriter;
import matrix.util.StringList;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.effectivity.CompositionBinary;
import com.matrixone.apps.framework.ui.UIMenu;
import com.matrixone.apps.productline.ProductLineCommon;

import com.matrixone.apps.configuration.ConfigurationConstants;

/**
 * The <code>CompositionBinaryMigrationBase</code> class contains code to do the Composition Binary Migration
 * 
 */
  public class CompositionBinaryMigrationBase_mxJPO extends emxCommonMigration_mxJPO
  {   
	  private CompositionBinaryMigrationFindObjects_mxJPO statusJPO;
	  private static final String TYPE_MANUFACTURING_PLAN                      = PropertyUtil.getSchemaProperty("type_ManufacturingPlan");

	  private Map _typeRelMapping = new HashMap();	    
	  
	  public static final String TYPE_E_SERVICE_PROGRAM_PARAMETERS = PropertyUtil.getSchemaProperty("type_eServiceTriggerProgramParameters");
		
	  /**
      * Default constructor.
      * @param context the eMatrix <code>Context</code> object
      * @param args String array containing program arguments
      * @throws Exception if the operation fails
      */
      public CompositionBinaryMigrationBase_mxJPO (Context context, String[] args)
              throws Exception
      {    	  
          super(context, args);          
          statusJPO = new CompositionBinaryMigrationFindObjects_mxJPO(context, new String[0]); 
          warningLog = new FileWriter(documentDirectory + "CompositionBinaryMigration.log", true);          
      }

      /**
       * This method is executed if a specific method is not specified.
       * This method checked for the status property and go ahead with Product migration if find object step is completed.
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
    		  int migrationStatus = statusJPO.getAdminMigrationStatus(context);

    		  //verifying trigger status to check if Binary Composition is enable
    		//  String mqlCmd = "print bus \""+TYPE_E_SERVICE_PROGRAM_PARAMETERS+"\" RelationshipAllCompositionBinaryCreateAction updateCompositionBinary select current dump |";
    		  
    		//  String sResult = MqlUtil.mqlCommand(context, mqlCmd.toString());
    		    String smqlCmd = "print bus $1 $2 $3 select $4 dump $5";
	    	    String sResult = MqlUtil.mqlCommand(context, smqlCmd, TYPE_E_SERVICE_PROGRAM_PARAMETERS, "RelationshipAllCompositionBinaryCreateAction", "updateCompositionBinary","current", "|" );
    	    	
    	      if(sResult == null || !("Active".equalsIgnoreCase(sResult)) ){
            	  mqlLogRequiredInformationWriter("Composition Binary Is Not Enabled, Migration Will Not Be Continued. \n");
            	  return -1;
              }
    	      else{
    	    	  mqlLogRequiredInformationWriter("Composition Binary Is Enabled, Migration Will Continue. \n");
    	      }
    	      /*
    	       * Below Lines need to be uncommented after the Pre-Checks are Implemented
              if(migrationStatus != 1)
              {
            	  mqlLogRequiredInformationWriter("Migration pre-checks not complete. Please complete migration pre-checks before running Composition Binary Migration. \n");
            	  bIsException=true;
            	  return -1;
              }
              
              else if(migrationStatus == 2){*/
            	  MqlUtil.mqlCommand(context, "trigger off", true);
            	         
            	//TODO Need get strType map from _typeRelMapping and fill in the arguments based on the MapList
            	  mqlLogRequiredInformationWriter("Constructing Rel Type Mapping. \n");
    			  constructTypeRelMapping(context);
    			  mqlLogRequiredInformationWriter("Completed Rel Type Mapping: "+ _typeRelMapping +" \n");
    			  
    			  if(!ProductLineCommon.isNotNull(TYPE_MANUFACTURING_PLAN)){
    				  mqlLogRequiredInformationWriter("Type Manufacturing Plan Not Found, Check If CFP Is Installed. \n");
    			  }
    			  
            	  super.mxMain(context, args);
            	  //As log files are closing in end of emxCommonMigrationBase:mxMain method, we need to reopen them for final logging statement 
            	  writer     = new BufferedWriter(new MatrixWriter(context));
            	  warningLog = new FileWriter(documentDirectory + "CompositionBinaryMigration.log", true);
            	  mqlLogRequiredInformationWriter("Composition Binary Migration End \n \n");
            //  }      
    	  }
    	  catch(Exception e){
    		  bIsException=true;
    		  mqlLogRequiredInformationWriter("\n");
	    	  mqlLogRequiredInformationWriter("Composition Binary Migration Failed :: "+e.getMessage());
	    	  mqlLogRequiredInformationWriter("\n");
	          e.printStackTrace();
	          throw e;
	          }
    	  finally{
    		  if(!bIsException){
    		  mqlLogRequiredInformationWriter("Setting MigrationCompleted !! \n \n");
    		  statusJPO.setAdminMigrationStatus(context,"MigrationCompleted");
    		  }
    		  MqlUtil.mqlCommand(context, "trigger on", true);
    		  writer.close();
    		  warningLog.close();
    	  }
    	  
    	  return 0;
      }
      
      /**
       * Main migration method to handle migration.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param objectIdList StringList holds list objectids to migrate
       * @returns nothing
       * @throws Exception if the operation fails
       */
      public void  migrateObjects(Context context, StringList objectIdList)throws Exception
      {      	 
          try
          {  
        	  createCompositionBinary(context, objectIdList);              
          }
          catch(Exception e)
          {        	  
              throw e;
          }              
      }
      
      /**
       * Method to be called from migrateObjects method to generate Composition Binary Data on supported Object
       * @param context the eMatrix <code>Context</code> object
       * @param prodMap Map containing information about the Product revisions under the Model
       * 					-	Key: Product Revision
       * 					-	Value: Map containing various info
       * @param strFirstRevision the very first revision of the Product revision chain
       * @throws Exception
       */
      private void createCompositionBinary(Context context, StringList objectIdList)throws Exception{
    	  
    	  try{
    		  String[] strArrObjIds = new String[objectIdList.size()];
  			  strArrObjIds = (String[])objectIdList.toArray(strArrObjIds);
    		  MapList typeMapList = DomainObject.getInfo(context, strArrObjIds, new StringList(DomainConstants.SELECT_TYPE));
    		  String strType = null;
    		  String logString = null;
    		  String[] args = new String[6];
    		  String strId = null;
    		  for(int i=0; i < strArrObjIds.length; i++){
    			  
    			  try{
    				  strId = strArrObjIds[i];
    				  strType = (String)((Map)typeMapList.get(i)).get("type");
        			  mqlLogRequiredInformationWriter("\nCreating Composition Binary For Id: "+strId+" Type: "+strType+" \n");
        			  
        			  logString = strType+","+strId+",,";
        			  
        			  if(mxType.isOfParentType(context, strType, ConfigurationConstants.TYPE_BUILDS)){
        				  
        				  String sType = FrameworkUtil.getAliasForAdmin(context,
        						  										 ConfigurationConstants.SELECT_TYPE,
        						  										 ConfigurationConstants.TYPE_BUILDS,
        						  										 true);
        				  
        				  mqlLogRequiredInformationWriter("Found Type: " + sType + "\n");
        				  
        				  MapList argMapList = (MapList)_typeRelMapping.get(sType);
        				  StringList sLRelName = new StringList();
        				  
        				  for(int mLCnt=0;mLCnt<argMapList.size();mLCnt++){
        					  
        					  Map mRelType = (Map)argMapList.get(mLCnt);
        					  String sRel = (String) mRelType.get("relType");
        					  
        					  //Check if already this RelName is present in list
        					  if( sLRelName.isEmpty() || (!sLRelName.isEmpty()&& !sLRelName.contains(sRel))){
        						  
        						  //Add to the list of Rel
            					  sLRelName.add(sRel);
            					  
            					  //Set the arg values
            					  args[1] = sRel;
            					  args[2] = strId;
                        		  args[3] = strId;
                        		  args[4] = sType;
                        		  args[5] = null;
                        		  mqlLogRequiredInformationWriter("Creating Composition Binary for " + sRel + "\n");
                        		  CompositionBinary.updateCompositionBinary(context, args);
            					  
        					  }
        				  }
                		 
            		  }
        			  else if(mxType.isOfParentType(context, strType, ConfigurationConstants.TYPE_PRODUCTS)){
        				  
        				    String sType = FrameworkUtil.getAliasForAdmin(context,
																		 ConfigurationConstants.SELECT_TYPE,
																		 ConfigurationConstants.TYPE_PRODUCTS,
																		 true);
        				  
        				    mqlLogRequiredInformationWriter("Found Type: " + sType + "\n");
        				    
							MapList argMapList = (MapList)_typeRelMapping.get(sType);
							StringList sLRelName = new StringList();
							
							for(int mLCnt=0;mLCnt<argMapList.size();mLCnt++){
							
								Map mRelType = (Map)argMapList.get(mLCnt);
								String sRel = (String) mRelType.get("relType");
								
								//Check if already this RelName is present in list
								if( sLRelName.isEmpty() || (!sLRelName.isEmpty()&& !sLRelName.contains(sRel))){
								
									//Add to the list of Rel
									sLRelName.add(sRel);
									
									//Set the arg values
									args[1] = sRel;
									args[2] = strId;
									args[3] = strId;
									args[4] = null;
								/*	String sArg5 = FrameworkUtil.getAliasForAdmin(context,
											 									  ConfigurationConstants.SELECT_TYPE,
											 									  (String) mRelType.get("arg5"),
											 									  true);
									args[5] = sArg5; */
									args[5] = (String) mRelType.get("arg5");
									mqlLogRequiredInformationWriter("Creating Composition Binary for" + sRel + "\n");
									CompositionBinary.updateCompositionBinary(context, args);
							    }
							}
        			  }
        			  else if(ProductLineCommon.isNotNull(TYPE_MANUFACTURING_PLAN) && mxType.isOfParentType(context, strType, TYPE_MANUFACTURING_PLAN)){
        				          				  
        				    String sType = FrameworkUtil.getAliasForAdmin(context,
																		  ConfigurationConstants.SELECT_TYPE,
																		  TYPE_MANUFACTURING_PLAN,
																		  true);
        				    
        				    mqlLogRequiredInformationWriter("Found Type: " + sType + "\n");
        				    
							MapList argMapList = (MapList)_typeRelMapping.get(sType);
							StringList sLRelName = new StringList();
							
							for(int mLCnt=0;mLCnt<argMapList.size();mLCnt++){
							
								Map mRelType = (Map)argMapList.get(mLCnt);
								String sRel = (String) mRelType.get("relType");
								
								//Check if already this RelName is present in list
								if( sLRelName.isEmpty() || (!sLRelName.isEmpty()&& !sLRelName.contains(sRel))){
								
									//Add to the list of Rel
									sLRelName.add(sRel);
									
									//Set the arg values
									args[1] = sRel;
									args[2] = strId;
									args[3] = strId;
									args[4] = null;
									args[5] = sType;
								
									mqlLogRequiredInformationWriter("Creating Composition Binary for" + sRel + "\n");
									CompositionBinary.updateCompositionBinary(context, args);
								}
							}
        				  
        			  }
        			  else{
        				  mqlLogRequiredInformationWriter("Type:: "+ strType + " Is Not Supported, Migration Will Skip For This Object Id: "+ strId +" \n");
        			  } 
        			  
        			  mqlLogRequiredInformationWriter("Completed Composition Binary For Id: "+strId+" Type:"+strType+" \n");
        			  loadMigratedOids(strId);    
    			  }
    			  catch(FrameworkException ex){
    				  mqlLogRequiredInformationWriter("\n::::: Failed Completed Composition Binary For Id: "+strId+" Type:"+strType+" ::::: \n");
    				  ex.printStackTrace();
    				  writeUnconvertedOID(logString+ex.getMessage()+"\n", strId);  
    			  }    			  
    		  }
    	  }
    	  catch(Exception e){
    		  throw e;
    	  } 
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
           writer.write(" Variant Configuration Composition Binary Migration steps::-  \n");
           writer.write("\n");
           writer.write(" Step1: Create the directory hierarchy like:- \n");
           writer.write(" C:/Migration \n");
           writer.write(" C:/Migration/CompositionBinary \n");           
           writer.write("\n");
           writer.write(" Step2: Perform Composition Binary Migration \n");    
           writer.write(" \n");
           writer.write(" Step2.1: Find all objects that need composition binary generated and save the ids to a list of files \n");
           writer.write(" Example: \n");
           writer.write(" execute program CompositionBinaryMigrationFindObjects 10 'C:/Migration/CompositionBinary' ; \n");
           writer.write(" First parameter  = indicates number of object per file \n");
           writer.write(" Second Parameter  = the directory where files should be written \n");
           writer.write(" \n");
           writer.write(" Step2.2: Generate Composition Binary \n");
           writer.write(" Example: \n");
           writer.write(" execute program CompositionBinaryMigration 'C:/Migration/CompositionBinary' 1 n ; \n");
           writer.write(" First parameter  = the directory to read the files from\n");
           writer.write(" Second Parameter = minimum range of file to start migrating  \n");
           writer.write(" Third Parameter  = maximum range of file to end migrating  \n");
           writer.write("        - value of 'n' means all the files starting from mimimum range\n");           
           writer.write("================================================================================================\n");
           writer.close();
       }   
       
       /**
        * Handles querying the CFFComposition commands and reading the settings.
        * Constructs MapLists for both parent and child composition binary configurations.
        * Stores the MapLists in local class variables to be used by createCompositionBinary as input parameters
        * for updateCompositionBinary.
        *
        * @param context the eMatrix <code>Context</code> object
        * @throws Exception if the operation fails
        */
       private void constructTypeRelMapping(Context context)throws Exception
       {
    	   Vector userRoleList = PersonUtil.getAssignments(context);
    	   String language = context.getSession().getLanguage();    	       	  
    	   MapList childCompositionCommands = UIMenu.getOnlyCommands(context, "CFFChildComposition", userRoleList, language);
    	   MapList parentCompositionCommands = UIMenu.getOnlyCommands(context, "CFFParentComposition", userRoleList, language);
    	   processCompositionCommand(context, childCompositionCommands, true); 	   
    	   processCompositionCommand(context, parentCompositionCommands, false); 	   
    	   
       }
       /**
        * Handles querying the CFFComposition commands and reading the settings.
        * Constructs MapLists for both parent and child composition binary configurations.
        * Stores the MapLists in local class variables to be used by createCompositionBinary as input parameters
        * for updateCompositionBinary.
        *
        * @param context the eMatrix <code>Context</code> object
        * @param compositionCommands MapList of the composition commands
        * @param isChild boolean true if this is a child composition else false
        * @throws Exception if the operation fails
        */
       private void processCompositionCommand(Context context, MapList compositionCommands, boolean isChild)throws Exception
       {
    	   String strCommandName;
    	   String strAliasRelType;
    	   String strBinaryKey; 
    	   String strRelType;
    	   
    	   for(int i = 0; i < compositionCommands.size(); i++)
    	   {
    		   Map effCmdMap = (Map)compositionCommands.get(i);
    		   
    		   if (effCmdMap == null || effCmdMap.size() <= 0)
    		   {
    			   continue; //if no commands nothing to do, just continue
    		   }
    		   strCommandName = (String)effCmdMap.get("name");
    		   Map effSettingsMap = (Map)effCmdMap.get("settings");
    		   
    		   if (effSettingsMap == null || effSettingsMap.size() <= 0)
    		   {
    			   //we should not get here
    			   mqlLogRequiredInformationWriter("Composition Binary not setup correctly.  Contact system administrator. \n");
    		   }
    		   strAliasRelType = (String)effSettingsMap.get("relationshipType");
    		   
    		   if (strAliasRelType == null || "".equals(strAliasRelType))
    		   {
    			   //we should not get here
    			   mqlLogRequiredInformationWriter("Composition Binary not setup correctly.  Contact system administrator. \n");
    		   }
    		   strRelType = PropertyUtil.getSchemaProperty(strAliasRelType);
    		   
    		   boolean toSide = true;
    		   if (isChild)
    		   {
    			   toSide = false;    			   
    		   }
    		   //get the allowed types for the relationship
    		   Map allowedTypes = DomainRelationship.getAllowedTypes(context, strRelType, toSide, true);
    		       		   
    		   //construct a map which will be keyed on Type with a MapList of data for the updateCompositionBinary parameters
    		   Map argMap = new HashMap();
    		   argMap.put("relType", strRelType);
    		   
    		   //TODO Need to get these arguments from the triggers
    		   //To form the Trigger Name
    		   //query for Relationship<relType>CompositionBinaryCreate* and get attribute argument 5 and 6 which maps to arg4 and arg5
    		   String strRelTypeMod = strRelType.replaceAll(" ", "");
    		   StringBuffer strBuffer = new StringBuffer();
    		   strBuffer.append("Relationship");
    		   strBuffer.append(strRelTypeMod);
    		   strBuffer.append("CompositionBinaryCreateAction");
    		  
    		    //temp query bus * RelationshipLogicalFeaturesCompositionBinaryCreate* * select attribute[eService Program Argument 6].value;
    		    //String strMqlCmd = "temp query bus '"+TYPE_E_SERVICE_PROGRAM_PARAMETERS+"' '" + strBuffer.toString() + "' * select attribute[eService Program Argument 6].value dump |";
    		   String strMqlCmd = "temp query bus $1 $2 $3 select $4 dump $5";
    		    
    		    ArrayList viewableObjects=new ArrayList();
    			StringList templist;

    			//String strRelName = MqlUtil.mqlCommand(context ,smqlCmd ,true) ;
    			String strRelName = MqlUtil.mqlCommand(context ,strMqlCmd, TYPE_E_SERVICE_PROGRAM_PARAMETERS , strBuffer.toString(), "*", "attribute[eService Program Argument 6].value", "|" );
    			
    			//arg5 should only get populated for LogicalFeatures
    		    if(strRelName!=null && !strRelName.equals("") && strRelType!=null 
    		      && strRelType.equalsIgnoreCase(ConfigurationConstants.RELATIONSHIP_LOGICAL_FEATURES)){
        		  
    		    	 String sProgArg6  = "";
        			 StringTokenizer st = new StringTokenizer(strRelName, "|");
                     while (st.hasMoreTokens()) {
                          String sTrigerType = st.nextToken();
                          String sTrigerName = st.nextToken();
                          String sTrigerRev = st.nextToken();
                          sProgArg6 = st.nextToken();                          
                     }
                     argMap.put("arg4", "");
        		     argMap.put("arg5", sProgArg6);
    		    }else{
    		       //if attriute arg is blank or the trigger does not exist, then arg4 and arg5 are blank
    			   argMap.put("arg4", "");
        		   argMap.put("arg5", "");
    		   }
    		       		   
    		   String strAllowedTypes = (String)allowedTypes.get(DomainRelationship.KEY_INCLUDE);
    		   
    		   StringTokenizer stAlldTypes = new StringTokenizer(strAllowedTypes, ",");
    		   while (stAlldTypes.hasMoreTokens())
    		   {
    			   String strType = stAlldTypes.nextToken();
    			   if (!_typeRelMapping.containsKey(strType))
    			   {
    				   MapList argMapList = new MapList();
    				   argMapList.add(argMap);
    				   _typeRelMapping.put(strType, argMapList);
    			   }
    			   else
    			   {
    				   //if the Type is already in the list, then just add the current Map to the List
    				   MapList argMapList = (MapList)_typeRelMapping.get(strType);
    				   argMapList.add(argMap);
    			   }    			  
    		   }
    	   }
      }
}
