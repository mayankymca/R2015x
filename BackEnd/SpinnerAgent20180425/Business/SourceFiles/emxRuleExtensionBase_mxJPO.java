/*
 ** emxRuleExtensionBase
 **
 ** Copyright (c) 1992-2015 Dassault Systemes.
 **
 ** All Rights Reserved.
 ** This program contains proprietary and trade secret information of
 ** MatrixOne, Inc.  Copyright notice is precautionary only and does
 ** not evidence any actual or intended publication of such program.
 **
 ** static const char RCSID[] = $Id: /ENOVariantConfigurationBase/CNext/Modules/ENOVariantConfigurationBase/JPOsrc/base/${CLASSNAME}.java 1.5.2.1.1.1 Wed Oct 29 22:27:16 2008 GMT przemek Experimental$
 */

import java.util.HashMap;
import java.util.Map;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.apps.configuration.ConfigurationConstants;
import com.matrixone.apps.configuration.ConfigurationUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.productline.ProductLineConstants;

/**
 * This JPO class has some methods pertaining to Rule Extension.
 * @author Enovia MatrixOne
 * @version ProductCentral 10.5 - Copyright (c) 2004, MatrixOne, Inc.
 */
public class emxRuleExtensionBase_mxJPO
{


   /**
     * Default Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @return noting,constructor
     * @throws Exception if the operation fails
     * @since Product Central 10-0-0-0
     */
 public emxRuleExtensionBase_mxJPO (Context context, String[] args)
 throws Exception
  {
  }


 /**
   * Main entry point.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds no arguments
   * @return an integer status code (0 = success)
   * @throws Exception if the operation fails
   * @since Product Central 10-0-0-0
   */
 public int mxMain(Context context, String[] args)
 throws Exception
  {
    if (!context.isConnected())
      throw new Exception("Not supported on desktop client");
    return 0;
  }

 /**
  * Method call to get all the Rule Extensions in the data base.
  *
  * @param context the eMatrix <code>Context</code> object
  * @param args - Holds the following arguments
  *   0 - HashMap containing the following arguments
  * @return Object - MapList containing the id of Rule Extension objects
  * @throws Exception if the operation fails
  * @since Product Central 10-0-0-0
  * @grade 0
  */
  @com.matrixone.apps.framework.ui.ProgramCallable
  public MapList getAllRuleExtensions(Context context, String[] args) throws Exception
      {
      MapList ruleExtensionMapList = new MapList();
        /* Method to retrive the Rule Extentions, ids connected to a Product*/
        HashMap ruleExtensionMap = (HashMap) JPO.unpackArgs(args);
        /* To obtain the Product ID */
        String strObjectId = (String)ruleExtensionMap.get("objectId");
        strObjectId =  strObjectId.trim();
        StringList objectSelects =new StringList(1);
        objectSelects.add(DomainConstants.SELECT_ID);
        
        StringList relationSelects =new StringList(1);
        relationSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
        DomainObject contextObject = new DomainObject(strObjectId);
        ruleExtensionMapList = contextObject.getRelatedObjects(context,ProductLineConstants.RELATIONSHIP_RULE_EXTENSION,ProductLineConstants.TYPE_RULE_EXTENSION,objectSelects,relationSelects,false,true,(short)0,null,null, 0);
  

       return ruleExtensionMapList;
  }
  /**
   * Method call to get all the Rule Extensions in the data base connected to context.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args - Holds the following arguments
   *   0 - HashMap containing the following arguments
   * @return Object - MapList containing the id of Rule Extension objects
   * @throws Exception if the operation fails
   * @since Product Central 10-0-0-0
   * @grade 0
   */
   public MapList getAllRuleExtensionsSearch(Context context, String[] args) throws Exception
       {

      
        HashMap resourceRule = (HashMap) JPO.unpackArgs(args); 
        
        String contextId = (String)resourceRule.get("hdnType");       
        DomainObject domainObject = new DomainObject(contextId);       
        StringList objSelects =new StringList(DomainConstants.SELECT_ID);   
        StringList relSelects = new StringList();
        short sRecurse = 1;
        String strObjPattern = ProductLineConstants.TYPE_RULE_EXTENSION;
        String strRelPattern = ProductLineConstants.RELATIONSHIP_RULE_EXTENSION;    
        MapList ruleExtensionList = new MapList();
        ruleExtensionList = domainObject.getRelatedObjects(context, strRelPattern,
                        strObjPattern, objSelects, relSelects, true, true, sRecurse,
                        "", "", 0);  
        return ruleExtensionList;
        
   }
   
   /**
    * Method call to get all the Rule Extensions in the data base.
    *
    * @param context the eMatrix <code>Context</code> object
    * @param args - Holds the following arguments
    *   0 - HashMap containing the following arguments
    * @return Object - MapList containing the id of Rule Extension objects
    * @throws Exception if the operation fails
    * @since R212
    * @grade 0
    */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getAllRuleExtensionsObjects(Context context, String[] args) throws Exception
        {
          MapList ruleExtensionMapList = new MapList();
         
          /* Method to retrieve the Rule Extention Ids connected to context*/
          HashMap ruleExtensionMap = (HashMap) JPO.unpackArgs(args);
          
          /* To obtain the Context ID */
          String strObjectId = (String)ruleExtensionMap.get("objectId");
          strObjectId =  strObjectId.trim();
          StringList objectSelects =new StringList(1);
          objectSelects.add(DomainConstants.SELECT_ID);
          objectSelects.add(DomainConstants.SELECT_TYPE);
          
          StringList relationSelects =new StringList(1);
          relationSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
          DomainObject contextObject = new DomainObject(strObjectId);
          ruleExtensionMapList = contextObject.getRelatedObjects(context,
        		  											      ProductLineConstants.RELATIONSHIP_RULE_EXTENSION,
												        		  ProductLineConstants.TYPE_RULE_EXTENSION,
												        		  objectSelects,
												        		  relationSelects,
												        		  false,
												        		  true,
												        		  (short)0,
												        		  null,
												        		  null,
												        		  0);

         return ruleExtensionMapList;
    }
   
   
	/**
	 * Exclude program for add existing Rule Extension
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	@com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
	public StringList excludeConnectedRuleExtension(Context context, String[] args) throws Exception
	{
		Map programMap = (Map) JPO.unpackArgs(args);
		
		// Logical Feature ID
		String relWhere = DomainObject.EMPTY_STRING;
		String objWhere = DomainObject.EMPTY_STRING;
				
		// Obj and Rel Selects
		StringList objSelects = new StringList();
		StringList relSelects = new StringList();

		String filterExpression = DomainObject.EMPTY_STRING;
		String objectId = (String) programMap.get("objectId");

		ConfigurationUtil confUtil = new ConfigurationUtil(objectId);
		MapList objectList = confUtil.getObjectStructure(context,
														ConfigurationConstants.TYPE_RULE_EXTENSION,
														ConfigurationConstants.RELATIONSHIP_RULE_EXTENSION,
														objSelects, relSelects, false, true, (short) 1, 0, objWhere,
														relWhere, DomainObject.FILTER_ITEM, filterExpression);
		
		StringList RuleExtensionToExclude = new StringList();
		for (int i = 0; i < objectList.size(); i++) {
			Map mapREObj = (Map) objectList.get(i);
			if (mapREObj.containsKey(DomainObject.SELECT_ID)) {
				String REIDToExclude = (String) mapREObj
						.get(DomainObject.SELECT_ID);
				RuleExtensionToExclude.add(REIDToExclude);
			}
		}
		return RuleExtensionToExclude;
}

   
   
   
}
