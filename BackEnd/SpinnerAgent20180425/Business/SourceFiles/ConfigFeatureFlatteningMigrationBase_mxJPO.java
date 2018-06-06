import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import com.matrixone.apps.configuration.ConfigurationConstants;
import com.matrixone.apps.configuration.InclusionRule;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.productline.ProductLineCommon;
import com.matrixone.apps.productline.ProductLineConstants;

import matrix.db.Context;
import matrix.db.RelationshipType;
import matrix.util.StringList;


public class ConfigFeatureFlatteningMigrationBase_mxJPO extends emxCommonMigration_mxJPO {


	static String RE_RULE_PRODUCT_ID = "to[Configuration Features].tomid[Right Expression].from.to.from.id";
	static String RE_MPR_PRODUCT_ID = "to[Configuration Features].tomid[Right Expression].from.to[Marketing Preference].from.id";
	static String RE_BCR_PRODUCT_ID = "to[Configuration Features].tomid[Right Expression].from.to[Boolean Compatibility Rule].from.id";
	static String RE_ID = "to[Configuration Features].tomid[Right Expression].id";
	static String LE_RULE_PRODUCT_ID = "to[Configuration Features].tomid[Left Expression].from.to.from.id";
	static String LE_MPR_PRODUCT_ID = "to[Configuration Features].tomid[Left Expression].from.to[Marketing Preference].from.id";
	static String LE_BCR_PRODUCT_ID = "to[Configuration Features].tomid[Left Expression].from.to[Boolean Compatibility Rule].from.id";
	static String LE_ID = "to[Configuration Features].tomid[Left Expression].id";

	public ConfigFeatureFlatteningMigrationBase_mxJPO(Context context,
			String[] args) throws Exception {
		super(context, args);
		// TODO Auto-generated constructor stub
	}

	public void migrateObjects(Context context, StringList objectList) throws Exception
	{
		String strObjectId = null ;
		DomainObject domContextBus = new DomainObject();
		Iterator iterator = objectList.iterator();
		MapList ParentObj ;
		MapList childCFs ;

		short iLevel = 0;
		int limit = 0;
		boolean getTo = true;            
		boolean getFrom = true;
		String strObjWhere = DomainConstants.EMPTY_STRING;
		String strRelWhere = DomainConstants.EMPTY_STRING;

		StringList slObjSelects = new StringList(ProductLineConstants.SELECT_ID);
		slObjSelects.add(ProductLineConstants.SELECT_TYPE);
		slObjSelects.add(ProductLineConstants.SELECT_NAME);
		slObjSelects.add("to[Configuration Features].id");
		slObjSelects.add("to[Configuration Features].physicalid");
		//rules
		slObjSelects.add("to[Configuration Features].tomid[Right Expression].from.id");
		slObjSelects.add("to[Configuration Features].tomid[Right Expression].from.type");
		slObjSelects.add("to[Configuration Features].tomid[Right Expression].from.attribute[Right Expression].value");
		slObjSelects.add(RE_MPR_PRODUCT_ID);
		slObjSelects.add(RE_BCR_PRODUCT_ID);
		slObjSelects.add(RE_ID);
		//rules
		slObjSelects.add("to[Configuration Features].tomid[Left Expression].from.id");
		slObjSelects.add("to[Configuration Features].tomid[Left Expression].from.type");
		slObjSelects.add("to[Configuration Features].tomid[Left Expression].from.attribute[Left Expression].value");
		slObjSelects.add(LE_MPR_PRODUCT_ID);
		slObjSelects.add(LE_BCR_PRODUCT_ID);
		slObjSelects.add(LE_ID);

		StringList slRelSelects = new StringList("");

		StringBuffer strTypePattern = new StringBuffer(ProductLineConstants.TYPE_CONFIGURATION_FEATURES);
		strTypePattern.append(",");
		strTypePattern.append(ProductLineConstants.TYPE_PRODUCTS);
		strTypePattern.append(",");
		strTypePattern.append(ProductLineConstants.TYPE_PRODUCT_LINE);
		strTypePattern.append(",");
		strTypePattern.append(ProductLineConstants.TYPE_MODEL);


		StringBuffer strRelPattern = new StringBuffer(ProductLineConstants.RELATIONSHIP_CONFIGURATION_FEATURES);
		strRelPattern.append(",");
		strRelPattern.append(ProductLineConstants.RELATIONSHIP_VARIES_BY);
		strRelPattern.append(",");
		strRelPattern.append(ProductLineConstants.RELATIONSHIP_INACTIVE_VARIES_BY);

		//mqlLogRequiredInformationWriter("Number Of toplevel CF(s) Found : "+ objectList.size()+" \n");

		//For each CF
		while (iterator.hasNext())
		{
			try{
				mqlLogRequiredInformationWriter("\n TOP CF _+_+_ "); 


				strObjectId = (String)iterator.next();
				mqlLogRequiredInformationWriter(" ID : " + strObjectId);
				Map childMap , parentMap ;
				MapList newCFList = new MapList();
				domContextBus.setId(strObjectId);

				getTo = true; getFrom = false;
				ParentObj = (MapList)domContextBus.getRelatedObjects(context, strRelPattern.toString(), strTypePattern.toString(), slObjSelects, slRelSelects, getTo,
						getFrom, iLevel, strObjWhere, strRelWhere, limit);

				getTo = false; getFrom = true;
				childCFs = (MapList)domContextBus.getRelatedObjects(context, strRelPattern.toString(), strTypePattern.toString(), slObjSelects, slRelSelects, getTo,
						getFrom, iLevel, strObjWhere, strRelWhere, limit);

				/*	if(ParentObj.size() != 0){
					System.out.println("Parents : " + ParentObj.size());
					Iterator itr = ParentObj.iterator();
					while(itr.hasNext()){
						parentMap = (Hashtable)itr.next();                    	  
						String strType = (String) parentMap.get(ProductLineConstants.SELECT_TYPE);
						String strId = (String) parentMap.get(ProductLineConstants.SELECT_ID); 
						String name = (String) parentMap.get(ProductLineConstants.SELECT_NAME);

						mqlLogRequiredInformationWriter("\nParent : "+ strType + " - " + name + " - "+ strId);

					}
				}*/

				if(childCFs.size() != 0){

					//System.out.println("child cf : " + ParentObj.size());


					Iterator itr = childCFs.iterator();
					while(itr.hasNext()){

						childMap = (Hashtable)itr.next(); 
						Hashtable<String, StringList> newCFdetails = new Hashtable<>() ;

						String strType = (String) childMap.get(ProductLineConstants.SELECT_TYPE);
						String strId = (String) childMap.get("to[Configuration Features].id");
						String name = (String) childMap.get(ProductLineConstants.SELECT_NAME);

						String strCFId = (String)childMap.get(ProductLineConstants.SELECT_ID);
						String strPID = (String) childMap.get("to[Configuration Features].physicalid");

						StringList RulesRE=new StringList() , typeRuleRE= new StringList() , REexpression = new StringList() , 
								REMPRProductID = new StringList() , REBCRProductID = new StringList() ,REIDs = new StringList();
						StringList RulesLE=new StringList() , typeRuleLE= new StringList() , LEexpression = new StringList(), 
								LEMPRProductID = new StringList() , LEBCRProductID = new StringList() , LEIDs = new StringList();

						mqlLogRequiredInformationWriter("\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ \nchild START : "+
								strType + " - " + name + " - "+ strId);

						if ( childMap.get("to[Configuration Features].tomid[Right Expression].from.id") != null ){
							if (childMap.get("to[Configuration Features].tomid[Right Expression].from.id") instanceof StringList){
								RulesRE = (StringList) childMap.get("to[Configuration Features].tomid[Right Expression].from.id");
								typeRuleRE = (StringList) childMap.get("to[Configuration Features].tomid[Right Expression].from.type");
								REexpression = (StringList) childMap.get("to[Configuration Features].tomid[Right Expression].from.attribute[Right Expression].value");
								REIDs = (StringList) childMap.get(RE_ID);
							}else{
								RulesRE.add(childMap.get("to[Configuration Features].tomid[Right Expression].from.id"));
								typeRuleRE.add(childMap.get("to[Configuration Features].tomid[Right Expression].from.type"));
								REexpression.add(childMap.get("to[Configuration Features].tomid[Right Expression].from.attribute[Right Expression].value"));
								REIDs.add(childMap.get(RE_ID));
							}		
							//mqlLogRequiredInformationWriter("\nRulesRE : "+ RulesRE.size() +" - " + RulesRE + " -Type- " + typeRuleRE) ;
						}

						if ( childMap.get("to[Configuration Features].tomid[Left Expression].from.id") != null ){
							if (childMap.get("to[Configuration Features].tomid[Left Expression].from.id") instanceof StringList){
								RulesLE = (StringList) childMap.get("to[Configuration Features].tomid[Left Expression].from.id");
								typeRuleLE = (StringList) childMap.get("to[Configuration Features].tomid[Left Expression].from.type");
								LEexpression = (StringList) childMap.get("to[Configuration Features].tomid[Left Expression].from.attribute[Left Expression].value");
								LEIDs = (StringList) childMap.get(LE_ID);
							}else{
								RulesLE.add( childMap.get("to[Configuration Features].tomid[Left Expression].from.id"));
								typeRuleLE.add(childMap.get("to[Configuration Features].tomid[Left Expression].from.type"));
								LEexpression.add(childMap.get("to[Configuration Features].tomid[Left Expression].from.attribute[Left Expression].value"));
								LEIDs.add(childMap.get(LE_ID));
							}	
							//mqlLogRequiredInformationWriter("\nRulesLE : "+ RulesLE.size() +" - " + RulesLE + " -Type- " + typeRuleLE) ;
						}

						if( childMap.get(RE_MPR_PRODUCT_ID) != null){
							if (childMap.get(RE_MPR_PRODUCT_ID) instanceof StringList){
								REMPRProductID = (StringList) childMap.get(RE_MPR_PRODUCT_ID);
							}else{
								REMPRProductID.add(childMap.get(RE_MPR_PRODUCT_ID));
							}	
						}
						if( childMap.get(RE_BCR_PRODUCT_ID) != null){
							if (childMap.get(RE_BCR_PRODUCT_ID) instanceof StringList){
								REBCRProductID = (StringList) childMap.get(RE_BCR_PRODUCT_ID);
							}else{
								REBCRProductID.add(childMap.get(RE_BCR_PRODUCT_ID));
							}	
						}
						if( childMap.get(LE_MPR_PRODUCT_ID) != null){
							if (childMap.get(LE_MPR_PRODUCT_ID) instanceof StringList){
								LEMPRProductID = (StringList) childMap.get(LE_MPR_PRODUCT_ID);
							}else{
								LEMPRProductID.add(childMap.get(LE_MPR_PRODUCT_ID));
							}	
						}
						if( childMap.get(LE_BCR_PRODUCT_ID) != null){
							if (childMap.get(LE_BCR_PRODUCT_ID) instanceof StringList){
								LEBCRProductID = (StringList) childMap.get(LE_BCR_PRODUCT_ID);
							}else{
								LEBCRProductID.add(childMap.get(LE_BCR_PRODUCT_ID));
							}	
						}


						//1. Port Child CF(s) to Parent ( PRD - PL - MOD )
						if(ParentObj.size() != 0){
							System.out.println("\nTOTAL PARENTS : " + ParentObj.size());
							Iterator prntItr = ParentObj.iterator();
							int parsecount = 0 ;
							//for each parent
							while(prntItr.hasNext()){

								parsecount++;
								parentMap = (Hashtable)prntItr.next();                    	  
								String parentType = (String)parentMap.get(ProductLineConstants.SELECT_TYPE);
								String parentId = (String)parentMap.get(ProductLineConstants.SELECT_ID); 
								String parentname = (String) parentMap.get(ProductLineConstants.SELECT_NAME);

								mqlLogRequiredInformationWriter("\n--------------------------------------------------\nParent START  :"+ 
										parentType + " - " + parentname + " - "+ parentId );

								DomainObject parentObj = DomainObject.newInstance(context); parentObj.setId(parentId);

								if( !parentType.equalsIgnoreCase("Model")){
									if(parsecount == 1){
										//float
										mqlLogRequiredInformationWriter("\nFirst parent : floating CFS to parent ");
										DomainRelationship.setFromObject(context,strId, parentObj);
									}else{
										//clone
										mqlLogRequiredInformationWriter("\nOther parent : creating new connection to parent and copying Rules");
										DomainRelationship CFRel = DomainRelationship.connect(context, parentObj, "Configuration Features", new DomainObject(strCFId));
										mqlLogRequiredInformationWriter("\nNew CFS id : " + CFRel.getName());

										String mqlCommand = "print connection $1 select $2 dump $3";
										String newCFsPID = MqlUtil.mqlCommand(context, mqlCommand, CFRel.getName(), "physicalid", ConfigurationConstants.DELIMITER_PIPE);
										// migrate Rules oldCFSid , newCFSid , type
										mqlLogRequiredInformationWriter("\nTotal no of RE on CFs :" + RulesRE.size());
										if(RulesRE.size() > 0){
											int counterMPR=0;
											int counterBCR=0;
											for (int i=0 ; i < RulesRE.size() ; i++) {
												mqlLogRequiredInformationWriter("\nProcessing RE  : "+ i + " : " +RulesRE.get(i).toString() + " : "+typeRuleRE.get(i).toString());
												if(typeRuleRE.get(i).toString().equals("Inclusion Rule")){
													//mqlLogRequiredInformationWriter("\nIn IR");
													migrateREofIR(context, strPID, newCFsPID, RulesRE.get(i).toString());
												}
												if(typeRuleRE.get(i).toString().equals("Boolean Compatibility Rule")){
													//mqlLogRequiredInformationWriter("\nIn BCR");
													if(parentId.equals(REBCRProductID.get(counterBCR).toString())){
														migrateREofBCR(context, strPID, newCFsPID.toString(), RulesRE.get(i).toString(), REIDs.get(i).toString());		
													}
													counterBCR++;
												}
												if(typeRuleRE.get(i).toString().equals("Marketing Preference")){
													//mqlLogRequiredInformationWriter("\nIn MPR");
													if(parentId.equals(REMPRProductID.get(counterMPR).toString())){
														migrateREofMPR(context, strPID, newCFsPID, RulesRE.get(i).toString(), REIDs.get(i).toString());
													}
													counterMPR++;
												}
											}
										}
										mqlLogRequiredInformationWriter("\nTotal no of LE on CFs :" + RulesLE.size());
										if(RulesLE.size() > 0){
											int counterMPR=0;
											int counterBCR=0;
											for(int i=0 ; i < RulesLE.size() ; i++){
												mqlLogRequiredInformationWriter("\n Processing LE : "+ i + " : " +RulesLE.get(i).toString() + " : "+typeRuleLE.get(i).toString());
												if(typeRuleLE.get(i).toString().equals("Inclusion Rule")){
													//mqlLogRequiredInformationWriter("\nIn IR");
													migrateLEofIR(context, strPID, newCFsPID, RulesLE.get(i).toString());
												}
												if(typeRuleLE.get(i).toString().equals("Boolean Compatibility Rule")){
													//mqlLogRequiredInformationWriter("\nIn BCR");
													if(parentId.equals(LEBCRProductID.get(counterBCR).toString())){
														migrateLEofBCR(context, strPID, newCFsPID.toString(), RulesLE.get(i).toString(), LEIDs.get(i).toString());		
													}
													counterBCR++;
												}
												if(typeRuleLE.get(i).toString().equals("Marketing Preference")){
													//mqlLogRequiredInformationWriter("\nIn MPR");
													if(parentId.equals(LEMPRProductID.get(counterMPR).toString())){
														migrateREofMPR(context, strPID, newCFsPID, RulesLE.get(i).toString(), LEIDs.get(i).toString());
													}
													counterMPR++;
												}
											}
										}
									}
								}else{
									if(ParentObj.size() == 1){
										//change type to candidate config feature
										mqlLogRequiredInformationWriter("\nFirst parent : floating CFS to parent model ");
										DomainRelationship.setType(context, strId, "Candidate Configuration Feature");
										DomainRelationship.setFromObject(context, strId, parentObj);

									}else{
										//create clone as candidate config feature
										mqlLogRequiredInformationWriter("\n12352");
										DomainRelationship CFRel = DomainRelationship.connect(context, parentObj, "Candidate Configuration Features", new DomainObject(strCFId));
										mqlLogRequiredInformationWriter("new CCF id : " + CFRel.getName());
										// migrate Rules oldCFSid , newCFSid , type
										/*	if(RulesRE.size() > 0){
											for (int i=0 ; i < RulesRE.size() ; i++) {
												migrateRuleRightExpression(strId, newCFsPID , typeRuleRE.get(i).toString() , RulesRE.get(i).toString());
											}
										}
										if(RulesLE.size() > 0){
											for(int i=0 ; i < RulesLE.size() ; i++){
												migrateRuleLeftExpression(strId, newCFsPID, typeRuleLE.get(i).toString(), RulesRE.get(i).toString());
											}
										}*/
									}

								}

								//newCFdetails.put("oldId", new StringList(strId));
								//newCFList.add(newCFdetails);
								mqlLogRequiredInformationWriter("\nEND processing :"+ 
										parentType + " - " + parentname + " - "+ parentId +
										"\n------------------------------------------------------------");
							}
						}else{
							System.out.println("No parent of CF : Delete all relations between CF and CF");
							//to be implemented
						} 

						mqlLogRequiredInformationWriter("\ncCHILD END : "+
								strType + " - " + name + " - "+ strId
								+"\n++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

					}
				}




			}catch( Exception e){
				mqlLogRequiredInformationWriter(e.toString());
				e.printStackTrace();
				mqlLogRequiredInformationWriter(e.getLocalizedMessage());

			}
		}
	}






	/*IR - Branch RE and update expression
	 * -create new RE from RuleID to newCFsID
	 * -update rule.attribute[RE] to have "( oldCFsPID and newCFsPID )"
	 */
	private void migrateREofIR(Context context, String oldCFsPID , String newCFsPID , String RuleID)
			throws Exception{


		//connect relationship 
		ProductLineCommon relFeature = new ProductLineCommon(newCFsPID);
		RelationshipType reltype = new RelationshipType(ProductLineConstants.RELATIONSHIP_RIGHT_EXPRESSION);
		relFeature.connectObject(context, reltype, RuleID, true);

		DomainObject ruleObj = DomainObject.newInstance(context);
		ruleObj.setId(RuleID);
		String strRightExpression = ruleObj.getAttributeValue(context,ProductLineConstants.RELATIONSHIP_RIGHT_EXPRESSION );
		mqlLogRequiredInformationWriter("\n  RE IR    :" + strRightExpression  );
		strRightExpression = strRightExpression.replaceAll("R"+oldCFsPID, "R"+oldCFsPID + " OR R"+newCFsPID);
		mqlLogRequiredInformationWriter("\n  newRE IR :" + strRightExpression);
		ruleObj.setAttributeValue(context, ProductLineConstants.RELATIONSHIP_RIGHT_EXPRESSION, strRightExpression);
		mqlLogRequiredInformationWriter("\n  IR on RightExpression processed ");

	}

	/*BCR - move RE to appropriate product and replace in expression
	 *  -if parent product is equal to product with rule - modify REID to point to newCFsID
	 * 	-change attribute[RE] - replace oldCFsPID with newCFsPID
	 */
	private void migrateREofBCR(Context context, String oldCFsPID , String newCFsPID ,  String RulePID , String REID)
			throws Exception{

		//dis-connect relationship 
		DomainRelationship.disconnect(context, REID);	
		//connect relationship
		ProductLineCommon relFeature = new ProductLineCommon(newCFsPID);
		RelationshipType reltype = new RelationshipType(ProductLineConstants.RELATIONSHIP_RIGHT_EXPRESSION);
		relFeature.connectObject(context, reltype, RulePID, true);

		//Replace oldCFsPId with newCFsPID
		DomainObject ruleObj = DomainObject.newInstance(context);
		ruleObj.setId(RulePID);
		String strRightExpression = ruleObj.getAttributeValue(context,ProductLineConstants.RELATIONSHIP_RIGHT_EXPRESSION );
		mqlLogRequiredInformationWriter("\n  RE BCR    :" + strRightExpression );
		strRightExpression = strRightExpression.replaceAll("R"+oldCFsPID, "R"+newCFsPID);
		mqlLogRequiredInformationWriter("\n  newRE BCR :" + strRightExpression);
		ruleObj.setAttributeValue(context, ProductLineConstants.RELATIONSHIP_RIGHT_EXPRESSION, strRightExpression);

	}

	/*MPR - move the RE to appropriate product and replace expression
	 * -if parent product is equal to product with rule - modify REID to point to newCFsID
	 * -change attribute[RE] - replace oldCFsPID with newCFsPID
	 */
	private void migrateREofMPR(Context context, String oldCFsPID , String newCFsPID ,  String RulePID , String REID)
			throws Exception{

		//dis-connect relationship 
		DomainRelationship.disconnect(context, REID);
		//connect relationship
		ProductLineCommon relFeature = new ProductLineCommon(newCFsPID);
		RelationshipType reltype = new RelationshipType(ProductLineConstants.RELATIONSHIP_RIGHT_EXPRESSION);
		relFeature.connectObject(context, reltype, RulePID, true);
		//Replace oldCFsPId with newCFsPID
		DomainObject ruleObj = DomainObject.newInstance(context);
		ruleObj.setId(RulePID);
		String strRightExpression = ruleObj.getAttributeValue(context,ProductLineConstants.RELATIONSHIP_RIGHT_EXPRESSION );
		mqlLogRequiredInformationWriter("\n  RE MPR    :" + strRightExpression  );
		strRightExpression = strRightExpression.replaceAll("R"+oldCFsPID, "R"+newCFsPID);
		mqlLogRequiredInformationWriter("\n  newRE MPR :" + strRightExpression);
		ruleObj.setAttributeValue(context, ProductLineConstants.RELATIONSHIP_RIGHT_EXPRESSION, strRightExpression);
		mqlLogRequiredInformationWriter("\n------------------Need Review "+RulePID + "--------------------------");
	}


	private void migrateLEofIR(Context context, String oldCFsPID , String newCFsPID , String RuleID)
			throws Exception{

		//Clone IR
		DomainObject Obj =  DomainObject.newInstance(context);
		Obj.setId(RuleID);
		InclusionRule IRObj = new InclusionRule(Obj);
		IRObj.copyInclusionRules(context, newCFsPID);
		mqlLogRequiredInformationWriter("\n  IR on LE processed ");
	}

	private void migrateLEofBCR(Context context, String oldCFsPID , String newCFsPID ,  String RulePID , String REID)
			throws Exception{

		//dis-connect relationship 
		DomainRelationship.disconnect(context, REID);	
		//connect relationship
		ProductLineCommon relFeature = new ProductLineCommon(newCFsPID);
		RelationshipType reltype = new RelationshipType(ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION);
		relFeature.connectObject(context, reltype, RulePID, true);

		//Replace oldCFsPId with newCFsPID
		DomainObject ruleObj = DomainObject.newInstance(context);
		ruleObj.setId(RulePID);
		String strLeftExpression = ruleObj.getAttributeValue(context,ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION );
		mqlLogRequiredInformationWriter("\n  LE BCR    :" + strLeftExpression );
		strLeftExpression = strLeftExpression.replaceAll("R"+oldCFsPID, "R"+newCFsPID);
		mqlLogRequiredInformationWriter("\nnew  LE BCR :" + strLeftExpression);
		ruleObj.setAttributeValue(context, ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION, strLeftExpression);

	}

	/*MPR - move the LE to appropriate product and replace expression
	 * -if parent product is equal to product with rule - modify LEID to point to newCFsID
	 * -change attribute[LE] - replace oldCFsPID with newCFsPID
	 */
	private void migrateLEofMPR(Context context, String oldCFsPID , String newCFsPID ,  String RulePID , String REID)
			throws Exception{

		//dis-connect relationship 
		DomainRelationship.disconnect(context, REID);
		//connect relationship
		ProductLineCommon relFeature = new ProductLineCommon(newCFsPID);
		RelationshipType reltype = new RelationshipType(ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION);
		relFeature.connectObject(context, reltype, RulePID, true);
		//Replace oldCFsPId with newCFsPID
		DomainObject ruleObj = DomainObject.newInstance(context);
		ruleObj.setId(RulePID);
		String strLeftExpression = ruleObj.getAttributeValue(context,ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION );
		mqlLogRequiredInformationWriter("\n  LE MPR    :" + strLeftExpression );
		strLeftExpression = strLeftExpression.replaceAll("R"+oldCFsPID, "R"+newCFsPID);
		mqlLogRequiredInformationWriter("\n  newLE MPR :" + strLeftExpression);
		ruleObj.setAttributeValue(context, ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION, strLeftExpression);

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
              throw new Exception("not supported on desktop client");
          }

          writer.write("================================================================================================\n");
          writer.write(" Variant Configuration Feature Flattening Migration is a two step process  \n");
          writer.write(" Step1: Find all objects that need to be migrated and save them into flat files \n");
          writer.write(" Example: \n");
          writer.write(" execute program ConfigFeatureFlatteningFindObjects 1000 C:/Temp/oids/; \n");
          writer.write(" First parameter  = indicates number of object per file \n");
          writer.write(" Second Parameter  = the directory where files should be written \n");
          writer.write(" \n");
          writer.write(" Step2: Migrate the objects \n");
          writer.write(" Example: \n");
          writer.write(" execute program ConfigFeatureFlatteningMigration 'C:/Temp/oids/' 1 n ; \n");
          writer.write(" First parameter  = the directory to read the files from\n");
          writer.write(" Second Parameter = minimum range of file to start migrating  \n");
          writer.write(" Third Parameter  = maximum range of file to end migrating  \n");
          writer.write("        - value of 'n' means all the files starting from mimimum range\n");
          writer.write("================================================================================================\n");
          writer.close();
      }
}
