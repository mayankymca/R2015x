/*
 * ${CLASS:FTRGBOMEffectivityMigrationBase}.java
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;
import com.matrixone.apps.effectivity.EffectivityFramework;
import com.matrixone.apps.productline.ProductLineCommon;
import com.matrixone.apps.configuration.ConfigurationConstants;
import com.matrixone.apps.configuration.ConfigurationUtil;
import com.matrixone.apps.configuration.LogicalFeature;
import com.matrixone.apps.configuration.RuleProcess;
import com.matrixone.json.JSONObject;

/**
 * The <code>FTRGBOMEffectivityMigrationBase</code> class contains code to migrate Inclusion Rules to CFF Expression
 * of the Logical Features/Products found in Step 1.
 */
public class InclusionRuleMigrationForGBOMAndLFBase_mxJPO extends emxCommonMigration_mxJPO
{   
	private InclusionRuleMigrationForGBOMAndLFFindObjects_mxJPO statusJPO;
	private LogicalFeature domContextBus;
	private StringList lstEffectivityTypes = new StringList("FeatureOption");
	RuleProcess rp = new RuleProcess();

	/**
	 * Default constructor.
	 * @param context the eMatrix <code>Context</code> object
	 * @param args String array containing program arguments
	 * @throws Exception if the operation fails
	 */
	public InclusionRuleMigrationForGBOMAndLFBase_mxJPO (Context context, String[] args)
			throws Exception
			{
		super(context, args);          
		statusJPO = new InclusionRuleMigrationForGBOMAndLFFindObjects_mxJPO(context, new String[0]);
		warningLog = new FileWriter(documentDirectory + "migration.log", true);
		domContextBus = new LogicalFeature();
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
		try{ 	
			int migrationStatus = statusJPO.getAdminMigrationStatus(context);

			mqlLogRequiredInformationWriter("Migration status is: "+ migrationStatus+" \n");

			statusJPO.setAdminMigrationStatus(context,"MigrationInProgress");  
			mqlLogRequiredInformationWriter("Migration in progress .. \n \n");

			super.mxMain(context, args);
			//As log files are closing in end of emxCommonMigrationBase:mxMain method, we need to reopen them for final logging statement 
			writer     = new BufferedWriter(new MatrixWriter(context));
			warningLog = new FileWriter(documentDirectory + "migration.log", true);
			mqlLogRequiredInformationWriter("Migration completed successfully. \n \n");
			statusJPO.setAdminMigrationStatus(context,"MigrationCompleted");
		}
		catch(Exception e){    		
			statusJPO.setAdminMigrationStatus(context,"MigrationKO");
			mqlLogRequiredInformationWriter("\n");
			mqlLogRequiredInformationWriter("Migration failed :: "+e.getMessage());
			mqlLogRequiredInformationWriter("\n");
			e.printStackTrace();
			throw e;
		}

		return 0;
	}

	public void  migrateObjects(Context context, StringList objectIdList)throws Exception
	{
		try
		{	
			String logString = ""; 
			String strObjectId = "";			
			MapList infoMapList = null;		
			short iLevel = 1;
			int limit = 0;
			String strObjWhere = DomainConstants.EMPTY_STRING;
			String strRelWhere = DomainConstants.EMPTY_STRING;

			StringList slObjSelects = new StringList(ConfigurationConstants.SELECT_ID);
			slObjSelects.add(ConfigurationConstants.SELECT_TYPE);
			slObjSelects.add(ConfigurationConstants.SELECT_NAME);
			slObjSelects.add(ConfigurationConstants.SELECT_REVISION);
			slObjSelects.add("from["+ConfigurationConstants.RELATIONSHIP_VARIES_BY+"].to.id");
			slObjSelects.add("from["+ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY+"].to.id");
			//slObjSelects.add("from["+ConfigurationConstants.RELATIONSHIP_VARIES_BY+"].attribute["+ConfigurationConstants.ATTRIBUTE_EFFECTIVITY_CONDITION+"]");
			//slObjSelects.add("from["+ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY+"].attribute["+ConfigurationConstants.ATTRIBUTE_EFFECTIVITY_CONDITION+"]");

			StringList slRelSelects = new StringList(ConfigurationConstants.SELECT_RELATIONSHIP_ID); 
			slRelSelects.add(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE); 
			slRelSelects.add("attribute[" + ConfigurationConstants.SELECT_ATTRIBUTE_RULE_TYPE +"]"); 
			//slRelSelects.add("attribute[" + ConfigurationConstants.ATTRIBUTE_EFFECTIVITY_CONDITION +"]"); 
			slRelSelects.add(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE+".kindof["+ConfigurationConstants.RELATIONSHIP_LOGICAL_FEATURES+"]"); 
			slRelSelects.add(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE+".kindof["+ConfigurationConstants.RELATIONSHIP_VARIES_BY+"]");
			slRelSelects.add(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE+".kindof["+ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY+"]");
			slRelSelects.add(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE+".kindof["+EffectivityFramework.RELATIONSHIP_CONFIGURATION_CONTEXT+"]");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].id");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].type");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].name");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].revision");			
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].attribute["+ ConfigurationConstants.ATTRIBUTE_RIGHT_EXPRESSION +"].value");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.physicalid");  //connections to relationship
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.type");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.id"); 
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.type.kindof["+ ConfigurationConstants.RELATIONSHIP_CONFIGURATION_STRUCTURES +"]");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.type.kindof["+ ConfigurationConstants.RELATIONSHIP_COMMON_GROUP +"]");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.attribute["+ ConfigurationConstants.ATTRIBUTE_COMMON_GROUP_NAME +"]");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.from.type.kindof["+ ConfigurationConstants.TYPE_CONFIGURATION_FEATURES +"]");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.from.type");			
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.from.name");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.from.revision");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.from.id");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.from.physicalid");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.to.type.kindof["+ ConfigurationConstants.TYPE_CONFIGURATION_FEATURES +"]");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.to.type");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.to.name");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.to.revision");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.to.id");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.to.physicalid");
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].to.type"); //connections to Object
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].to.name"); 
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].to.revision"); 
			slRelSelects.add("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].to.id"); 

			boolean getFrom = true;
			boolean getTo = false;

			StringBuffer strTypePattern = new StringBuffer(ConfigurationConstants.TYPE_PART);
			strTypePattern.append(",");
			strTypePattern.append(ConfigurationConstants.TYPE_PART_FAMILY);
			strTypePattern.append(",");
			strTypePattern.append(ConfigurationConstants.TYPE_LOGICAL_FEATURE);
			strTypePattern.append(",");
			strTypePattern.append(ConfigurationConstants.TYPE_CONFIGURATION_FEATURE);
			strTypePattern.append(",");
			strTypePattern.append(ConfigurationConstants.TYPE_PRODUCTS);
			strTypePattern.append(",");
			strTypePattern.append(ConfigurationConstants.TYPE_MODEL);

			StringBuffer strRelPattern = new StringBuffer(ConfigurationConstants.RELATIONSHIP_GBOM);
			strRelPattern.append(",");
			strRelPattern.append(ConfigurationConstants.RELATIONSHIP_INACTIVE_GBOM);
			strRelPattern.append(",");
			strRelPattern.append(ConfigurationConstants.RELATIONSHIP_CUSTOM_GBOM);
			strRelPattern.append(",");
			strRelPattern.append(ConfigurationConstants.RELATIONSHIP_INACTIVE_CUSTOM_GBOM);
			strRelPattern.append(",");
			strRelPattern.append(ConfigurationConstants.RELATIONSHIP_LOGICAL_FEATURES);
			strRelPattern.append(",");
			strRelPattern.append(ConfigurationConstants.RELATIONSHIP_VARIES_BY);
			strRelPattern.append(",");
			strRelPattern.append(ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY);
			strRelPattern.append(",");
			strRelPattern.append(EffectivityFramework.RELATIONSHIP_CONFIGURATION_CONTEXT);

			Iterator iterator = objectIdList.iterator();

			//For each LF/Product Object found
			while (iterator.hasNext())
			{
				try{					
					mqlLogRequiredInformationWriter("\n");

					strObjectId = (String)iterator.next();

					mqlLogRequiredInformationWriter("############################## Migration starts for object id= " + strObjectId +" ############################## \n");

					domContextBus.setId(strObjectId);

					infoMapList = (MapList)domContextBus.getRelatedObjects(context, strRelPattern.toString(), strTypePattern.toString(), slObjSelects, slRelSelects, getTo,
							getFrom, iLevel, strObjWhere, strRelWhere, limit);

					processGBOMForEffectivity(context, strObjectId, infoMapList);

				}
				catch(Exception ex){
					mqlLogRequiredInformationWriter(ex.getMessage());
					mqlLogRequiredInformationWriter("\n");					

					for (StackTraceElement ste : ex.getStackTrace()) {
						mqlLogRequiredInformationWriter(ste+"\n");
					}

					mqlLogRequiredInformationWriter("\n");
					mqlLogRequiredInformationWriter("########################## Migration failed for object id= " + strObjectId + " ##########################");
					mqlLogRequiredInformationWriter("\n");
					ex.printStackTrace();
					writeUnconvertedOID(","+strObjectId+",,"+ex.getMessage()+"\n", strObjectId); 
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
	private void  processGBOMForEffectivity(Context context, String strObjectId, MapList infoMapList)throws Exception {    	  

		MapList part_MapList = new MapList();	
		MapList LF_MapList = new MapList();	
		StringList slDesignVariantsList = new StringList();
		StringList slInactiveDesignVariantsList = new StringList();
		StringList slConfigContextsList = new StringList();		
		//StringList slIRIDListToDelete = new StringList();
		java.util.Set setParentInvolved = new HashSet();
		Map IRInfoMap = null;
		Map structureInfoMap = null;
		boolean objectMigrated = true;
		boolean inclusionRuleMigrated = true;

		try{
			mqlLogRequiredInformationWriter("Fetching all Models connected \n");
			StringList slconnectedProductModels = domContextBus.getAllContextModels(context, false);
			mqlLogRequiredInformationWriter("Models to be processed for context: "+slconnectedProductModels+" \n");

			if(!slconnectedProductModels.isEmpty()){

				if(infoMapList.size() != 0){
					Iterator itrOut = infoMapList.iterator();

					mqlLogRequiredInformationWriter("Separating Design Variants, Configuration Context, Logical Feature, Part Info \n");

					while(itrOut.hasNext()){
						structureInfoMap = (Hashtable)itrOut.next();

						if(structureInfoMap.containsKey(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE+".kindof["+ConfigurationConstants.RELATIONSHIP_VARIES_BY+"]") && "TRUE".equalsIgnoreCase((String)structureInfoMap.get(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE+".kindof["+ConfigurationConstants.RELATIONSHIP_VARIES_BY+"]")) ){
							slDesignVariantsList.add(structureInfoMap.get(ConfigurationConstants.SELECT_ID));						
						}
						else if(structureInfoMap.containsKey(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE+".kindof["+ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY+"]") && "TRUE".equalsIgnoreCase((String)structureInfoMap.get(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE+".kindof["+ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY+"]")) ){
							slInactiveDesignVariantsList.add(structureInfoMap.get(ConfigurationConstants.SELECT_ID));					
						}
						else if(structureInfoMap.containsKey(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE+".kindof["+EffectivityFramework.RELATIONSHIP_CONFIGURATION_CONTEXT+"]") && "TRUE".equalsIgnoreCase((String)structureInfoMap.get(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE+".kindof["+EffectivityFramework.RELATIONSHIP_CONFIGURATION_CONTEXT+"]")) ){
							slConfigContextsList.add(structureInfoMap.get(ConfigurationConstants.SELECT_ID));
						}
						else if(structureInfoMap.containsKey(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE+".kindof["+ConfigurationConstants.RELATIONSHIP_LOGICAL_FEATURES+"]") && "TRUE".equalsIgnoreCase((String)structureInfoMap.get(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE+".kindof["+ConfigurationConstants.RELATIONSHIP_LOGICAL_FEATURES+"]")) ){
							LF_MapList.add(structureInfoMap);
						}					
						else{
							part_MapList.add(structureInfoMap);
						}
					}

					mqlLogRequiredInformationWriter("Design Variants already attached= " + slDesignVariantsList +"\n");				
					mqlLogRequiredInformationWriter("Inactive Design Variants attached= " + slInactiveDesignVariantsList +"\n");				
					mqlLogRequiredInformationWriter("Configuration Context already attached= "+ slConfigContextsList +"\n");

					mqlLogRequiredInformationWriter("\nStart processing Inclusion Rule on GBOM \n");

					if(part_MapList.size() != 0){

						Iterator itrPart = part_MapList.iterator();

						while(itrPart.hasNext()){
							IRInfoMap = (Hashtable)itrPart.next();

							inclusionRuleMigrated = processPartLF(context, IRInfoMap, slDesignVariantsList, slInactiveDesignVariantsList, 
									slConfigContextsList, slconnectedProductModels, setParentInvolved, false);

							if(!inclusionRuleMigrated){
								String toSideObjectID = (String)IRInfoMap.get(ConfigurationConstants.SELECT_ID);
								writeUnconvertedOID(","+strObjectId+",,"+"IR failed For GBOM Part/Product : "+toSideObjectID+". Refer migration.log for more details"+"\n", null);
							}


							objectMigrated = objectMigrated && inclusionRuleMigrated;
						} //End of loop to iterate Part		

						setActiveInactiveDesignVariant(context, strObjectId, setParentInvolved, new StringList(slDesignVariantsList), 
								new StringList(slInactiveDesignVariantsList));

						mqlLogRequiredInformationWriter("\nEnd processing Inclusion Rule on GBOM \n");
					}
					else{
						mqlLogRequiredInformationWriter("\nObject does not have any Inclusion Rule on its GBOM.. \n");
					}

					mqlLogRequiredInformationWriter("\nStart processing Inclusion Rule on Logical Features \n");

					if(LF_MapList.size() != 0){

						Iterator itrLF = LF_MapList.iterator();

						while(itrLF.hasNext()){
							IRInfoMap = (Hashtable)itrLF.next();

							setParentInvolved = new HashSet();

							inclusionRuleMigrated = processPartLF(context, IRInfoMap, slDesignVariantsList, slInactiveDesignVariantsList, 
									slConfigContextsList, slconnectedProductModels, setParentInvolved, true);

							if(!inclusionRuleMigrated){
								String toSideObjectID = (String)IRInfoMap.get(ConfigurationConstants.SELECT_ID);
								writeUnconvertedOID(","+strObjectId+",,"+"IR failed For LF : "+toSideObjectID+". Refer migration.log for more details"+"\n", null);
							}

							objectMigrated = objectMigrated && inclusionRuleMigrated;
						} //End of loop to iterate LF		

						mqlLogRequiredInformationWriter("\nEnd processing Inclusion Rule on Logical Features \n");
					}
					else{
						mqlLogRequiredInformationWriter("\nObject does not have any Inclusion Rule on its Logical Features.. \n");
					}

					/*
				slconnectedProductModels.removeAll(slConfigContextsList);
				mqlLogRequiredInformationWriter("Connecting :"+ strObjectId +" to  Models: "+ slconnectedProductModels +" using relationship "+ EffectivityFramework.RELATIONSHIP_CONFIGURATION_CONTEXT +" \n");
				if(slconnectedProductModels.size()>0){
					DomainRelationship.connect(context, domContextBus, EffectivityFramework.RELATIONSHIP_CONFIGURATION_CONTEXT,true,Arrays.copyOf(slconnectedProductModels.toArray(), slconnectedProductModels.size(), String[].class));
				}
					 */

				}
				else{
					mqlLogRequiredInformationWriter("\nObject does not have any Inclusion Rule on its GBOM or Logical Features... \n");
				}
			}
			else{
				mqlLogRequiredInformationWriter("\nObject does not have Model context associated with it. \n");
				objectMigrated = false;
			}


			if(!objectMigrated){				
				mqlLogRequiredInformationWriter("=========>>>>>> One or Many IRs under this object got failed to migrate, refer migration.log for more details");
				mqlLogRequiredInformationWriter("\n infoMapList of the object->"+infoMapList+"\n"); 
				mqlLogRequiredInformationWriter("########################## Migration failed for object id= " + strObjectId + " ##########################");
				mqlLogRequiredInformationWriter("\n\n\n\n\n");
				writeUnconvertedOID(","+strObjectId+",,"+"One or Many IRs under this object got failed to migrate. Refer migration.log for more details"+"\n", strObjectId);
			}
			else{
				loadMigratedOids(strObjectId); 
				mqlLogRequiredInformationWriter("Migration completed for object id= " + strObjectId);
				mqlLogRequiredInformationWriter("\n\n");
			}
		}
		catch(Exception e){
			mqlLogRequiredInformationWriter("infoMapList of the object->"+infoMapList+"\n"); 
			throw e;
		} 	  
	}

	private void setActiveInactiveDesignVariant(Context context, String strObjectId, java.util.Set setParentInvolved, StringList slDesignVariantsList, 
			StringList slInactiveDesignVariantsList) throws Exception {  

		slInactiveDesignVariantsList.retainAll(setParentInvolved);
		String CurrentId = domContextBus.getId(context);

		LogicalFeature domContextBustoactivate = new LogicalFeature();
		domContextBustoactivate.setId(CurrentId);
		mqlLogRequiredInformationWriter("Object: "+ strObjectId +" will have following Inactive Design Variant changed to Active: "+ slInactiveDesignVariantsList +"\n");
		if(slInactiveDesignVariantsList.size()>0){
			domContextBustoactivate.makeDesignVariantsActive(context, slInactiveDesignVariantsList, "");
		}		

		setParentInvolved.removeAll(slDesignVariantsList);	
		setParentInvolved.removeAll(slInactiveDesignVariantsList);	


		LogicalFeature domContextBustoadd = new LogicalFeature();
		domContextBustoadd.setId(CurrentId);

		mqlLogRequiredInformationWriter("Connecting :"+ strObjectId +" to  Configuration Feature: "+ setParentInvolved +" using relationship "+ ConfigurationConstants.RELATIONSHIP_VARIES_BY +" \n");
		if(setParentInvolved.size()>0){				
			domContextBustoadd.addDesignVariants(context, new StringList(new ArrayList(setParentInvolved)));						
		}
	}

	private boolean processPartLF(Context context, Map IRInfoMap, StringList slDesignVariantsList, StringList slInactiveDesignVariantsList, 
			StringList slConfigContextsList, StringList slconnectedProductModels, 
			java.util.Set setParentInvolved, boolean isLF) throws Exception {

		boolean inclusionRuleMigrated = true;


		String strReason = null;
		Object tempObj = null;	

		String IRid = (String)IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].id");
		String IRType = (String)IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].type");
		String IRName = (String)IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].name");
		String IRRevision = (String)IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].revision");
		String strRuleInfo = IRType+" "+IRName+" "+IRRevision+" ("+IRid+") ";

		String relIDForIR = (String)IRInfoMap.get(ConfigurationConstants.SELECT_RELATIONSHIP_ID);
		String relTypeForIR = (String)IRInfoMap.get(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE);	
		String toSideObjectID = (String)IRInfoMap.get(ConfigurationConstants.SELECT_ID);
		String toSideObjectType = (String)IRInfoMap.get(ConfigurationConstants.SELECT_TYPE);
		String toSideObjectName = (String)IRInfoMap.get(ConfigurationConstants.SELECT_NAME);
		String toSideObjectRevision = (String)IRInfoMap.get(ConfigurationConstants.SELECT_REVISION);
		String toSideObjectInfo = toSideObjectType+" "+toSideObjectName+" "+toSideObjectRevision+" ("+toSideObjectID+") "; 
	
		if(ProductLineCommon.isNotNull(IRid)){
			mqlLogRequiredInformationWriter("\nProcessing relationship: "+relTypeForIR+"("+relIDForIR+") connected with: "+ toSideObjectInfo +" for Inclusion Rule: "+strRuleInfo+"\n");

			//String attributeRightExpression = (String)IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].attribute["+ ConfigurationConstants.ATTRIBUTE_RIGHT_EXPRESSION +"].value"); 

			//mqlLogRequiredInformationWriter("Right Expression attribute:\""+attributeRightExpression+"\" \n");	

			//if(ProductLineCommon.isNotNull(attributeRightExpression)){

				strReason = isSupportedCase(context, IRInfoMap);

				if(ProductLineCommon.isNotNull(strReason)){ //These IRs are unsupported case, hence will not be logged as failed case
					inclusionRuleMigrated = false;
					mqlLogRequiredInformationWriter("Migration couldn't convert:"+strRuleInfo+" into effectivity \n");				
					mqlLogRequiredInformationWriter("Reason:"+strReason+" \n");
				}
				else{ 				
		
					mqlLogRequiredInformationWriter(strRuleInfo+"IR passed validation for migration, Effectivity conversion will start now..."+"\n");

					tempObj = IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.from.id");
					StringList slParentId = ConfigurationUtil.convertObjToStringList(context, tempObj);

					tempObj = IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.physicalid");
					StringList slRelCOPhysicalId = ConfigurationUtil.convertObjToStringList(context, tempObj);

					tempObj = IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.from.physicalid");
					StringList slParentPhysicalId = ConfigurationUtil.convertObjToStringList(context, tempObj);

					tempObj = IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.to.physicalid");
					StringList slChildPhysicalId = ConfigurationUtil.convertObjToStringList(context, tempObj);

					EffectivityFramework Eff = new com.matrixone.apps.effectivity.EffectivityFramework(); 
					JSONObject effObj = new com.matrixone.json.JSONObject();
					Map effMap = new HashMap(); 
					StringBuffer strActualExpression= new StringBuffer(100);

					for(int l=0;l<slconnectedProductModels.size();l++){

						for(int k = 0; k < slParentPhysicalId.size(); k++){

							if(slconnectedProductModels.size() > 1 && k == 0){
								strActualExpression.append("( ");
							}

							effObj.put("contextId", slconnectedProductModels.get(l)); //physicalid of the Model context
							effObj.put("parentId", slParentPhysicalId.get(k)); //physicalid of the CF 
							effObj.put("objId", slChildPhysicalId.get(k)); //physicalid of the CO
							effObj.put("relId", slRelCOPhysicalId.get(k)); //physicalid of the CO rel
							effObj.put("insertAsRange", false);

							String jsonString = effObj.toString();
							Map formatedExpr =Eff.formatExpression(context, "FeatureOption", jsonString);

							strActualExpression.append((String)formatedExpr.get(Eff.ACTUAL_VALUE));

							if(slParentPhysicalId.size() > 1 && k < slParentPhysicalId.size() - 1){
								strActualExpression.append(" AND ");
							}
							if(slconnectedProductModels.size() > 1 && k == slParentPhysicalId.size() - 1){
								strActualExpression.append(" )");
							}
						}

						if(slconnectedProductModels.size() > 1 && l < slconnectedProductModels.size() - 1){
							strActualExpression.append(" OR ");
						}						
					}

					com.matrixone.apps.effectivity.EffectivityFramework effectivity = new com.matrixone.apps.effectivity.EffectivityFramework();

					if(!isLF && (relTypeForIR.equals(ConfigurationConstants.RELATIONSHIP_INACTIVE_GBOM) || relTypeForIR.equals(ConfigurationConstants.RELATIONSHIP_INACTIVE_CUSTOM_GBOM))){
						//Inactive GBOM because of Inactive DV
						StringList copyOfInactiveDVList = new StringList(slInactiveDesignVariantsList);
						copyOfInactiveDVList.retainAll(slParentId);

						if(copyOfInactiveDVList.size() > 0 ){
							DomainRelationship.disconnect(context, relIDForIR);
							slParentId.clear();
						}
						else{
							DomainRelationship.setType(context, relIDForIR,ConfigurationConstants.RELATIONSHIP_GBOM);
							mqlLogRequiredInformationWriter("Effectivity Expression to be stored on relationship: "+ relIDForIR +" is: " + strActualExpression + "\n");
							effectivity.updateRelExpression(context, relIDForIR , strActualExpression.toString());
							DomainRelationship.setType(context, relIDForIR, relTypeForIR);
						}						
					}
					else{
						mqlLogRequiredInformationWriter("Effectivity Expression to be stored on relationship: "+ relIDForIR +" is: " + strActualExpression + "\n");
						effectivity.updateRelExpression(context, relIDForIR , strActualExpression.toString());						
					}

					mqlLogRequiredInformationWriter("Deleting Inclusion Rule object ID :: "+ IRid +" \n");
					DomainObject.deleteObjects(context, new String[]{IRid});
					setParentInvolved.addAll(slParentId);

					if(isLF){

						tempObj = IRInfoMap.get("from["+ConfigurationConstants.RELATIONSHIP_VARIES_BY+"].to.id");
						slDesignVariantsList = ConfigurationUtil.convertObjToStringList(context, tempObj);

						tempObj = IRInfoMap.get("from["+ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY+"].to.id");
						slInactiveDesignVariantsList = ConfigurationUtil.convertObjToStringList(context, tempObj);

						domContextBus.setId(toSideObjectID);

						setActiveInactiveDesignVariant(context, toSideObjectID, setParentInvolved, slDesignVariantsList, 
								slInactiveDesignVariantsList);
					}	
		
				}		//		
			/*}
			else{		//These IR has blank IR and hence will be deleted, no change in Effectivity
				mqlLogRequiredInformationWriter("Deleting Inclusion Rule object ID: "+ IRid +" \n");
				DomainObject.deleteObjects(context, new String[]{IRid});
			}*/
		}
		else{
			mqlLogRequiredInformationWriter("\nRelationship: "+relTypeForIR+"("+relIDForIR+") connected with: "+ toSideObjectInfo +" not having any IR connected with it \n");			
		} //

		return inclusionRuleMigrated;
	}


	private String isSupportedCase(Context context, Map IRInfoMap)throws Exception {  

		String strReason = null;
		Object tempObj = null;	
		//HashMap tempArgMap = new HashMap();
		//Map mapRightExpression = new HashMap();

		String ruleTypeForIR = (String)IRInfoMap.get("attribute[" + ConfigurationConstants.SELECT_ATTRIBUTE_RULE_TYPE +"]");
		String attributeRightExpression = (String)IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].attribute["+ ConfigurationConstants.ATTRIBUTE_RIGHT_EXPRESSION +"].value"); 
		attributeRightExpression = attributeRightExpression.trim();
		mqlLogRequiredInformationWriter("Right Expression attribute:\""+attributeRightExpression+"\" \n");
	
		
		//mapRightExpression.put("attribute[Right Expression]", attributeRightExpression);
		//MapList objectList = new MapList();
		//objectList.add(mapRightExpression);
		//tempArgMap.put("objectList", objectList);	            
		//String[] args = JPO.packArgs(tempArgMap);
		//java.util.List lsREExpression = rp.getExpressionForRuleDisplay(context, args, "Right Expression");				
		//String strIRDisplayExpression = (String)lsREExpression.get(0);
		//mqlLogRequiredInformationWriter("Display expression:\""+strIRDisplayExpression+"\" \n");	

		String toSideObjectType = (String)IRInfoMap.get(ConfigurationConstants.SELECT_TYPE);

		tempObj = IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.type");
		StringList rightExpressionToRel = ConfigurationUtil.convertObjToStringList(context, tempObj);

		tempObj = IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].to.type");
		StringList toObjectOnRE = ConfigurationUtil.convertObjToStringList(context, tempObj);
		
		tempObj = IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.from.type.kindof["+ ConfigurationConstants.TYPE_CONFIGURATION_FEATURES +"]");
		StringList isFromTypeCF = ConfigurationUtil.convertObjToStringList(context, tempObj);

		tempObj = IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.to.type.kindof["+ ConfigurationConstants.TYPE_CONFIGURATION_FEATURES +"]");
		StringList isToTypeCF = ConfigurationUtil.convertObjToStringList(context, tempObj);

		tempObj = IRInfoMap.get("tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from["+ConfigurationConstants.TYPE_INCLUSION_RULE+"].from["+ ConfigurationConstants.RELATIONSHIP_RIGHT_EXPRESSION +"].torel.from.physicalid");
		StringList slParentPhysicalId = ConfigurationUtil.convertObjToStringList(context, tempObj);
		java.util.Set toRemoveDuplicate = new HashSet(slParentPhysicalId);

		if(toSideObjectType != null && mxType.isOfParentType(context,toSideObjectType,ConfigurationConstants.TYPE_PRODUCTS)){
			strReason = "Unsupported Case: Product is present as GBOM";
		}
		else if(ProductLineCommon.isNotNull(ruleTypeForIR) && ruleTypeForIR.equals("Exclusion")){
			strReason = "Unsupported Case: IR Rule Type Exclusion in nature.";
		}
		else if(rightExpressionToRel.contains("Common Group")){
			strReason = "Unsupported Case: Right Expression connected to Common Group.";					
		}
		else if(rightExpressionToRel.contains("Logical Features")){
			strReason = "Unsupported Case: Right Expression connected to Logical Features.";					
		}
		else if(isFromTypeCF.contains("FALSE") || isToTypeCF.contains("FALSE")){
			strReason = "Unsupported Case: Right Expression connected to non-FO relationship.";					
		}
		else if(ProductLineCommon.isNotNull(attributeRightExpression) && attributeRightExpression.contains("NOT ") ) {	
			strReason = "Unsupported Case: Inclusion Rule having NOT in Expression.";					
		}
		else if(slParentPhysicalId.size() != toRemoveDuplicate.size()){
			strReason = "Unsupported Case: Right Expression connected to multiple options of same Feature.";	
		}
		else if(ProductLineCommon.isNotNull(attributeRightExpression) && attributeRightExpression.contains(" OR ")){
			strReason = "Unsupported Case: Inclusion Rule having OR in Expression.";					
		}
		else if(toObjectOnRE != null && toObjectOnRE.size() > 0 ){
			strReason = "Unsupported Case: Right Expression connected to object.";							
		}
		
		return strReason;
	}
}
