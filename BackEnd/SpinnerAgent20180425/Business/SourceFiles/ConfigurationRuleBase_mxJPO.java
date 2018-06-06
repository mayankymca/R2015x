/*
 ** emxConfigurationRuleBase
 **
 ** Copyright (c) 1992-2016 Dassault Systemes.
 **
 ** All Rights Reserved.
 ** This program contains proprietary and trade secret information of
 ** MatrixOne, Inc.  Copyright notice is precautionary only and does
 ** not evidence any actual or intended publication of such program.
 **
 ** static const char RCSID[] = $Id: /ENOVariantConfigurationBase/CNext/Modules/ENOVariantConfigurationBase/JPOsrc/base/${CLASSNAME}.java 1.6.2.1.1.1 Wed Oct 29 22:27:16 2008 GMT przemek Experimental$
 */

import java.util.HashMap;
import java.util.Map;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.dassault_systemes.enovia.configuration.modeler.Model;
import com.matrixone.apps.configuration.ConfigurableRulesUtil;
import com.matrixone.apps.configuration.ConfigurationConstants;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.effectivity.EffectivityFramework;
import com.matrixone.apps.productline.Product;
import com.matrixone.json.JSONObject;

/**
 * This JPO class has some method pertaining to Configuration Rule type.
 * @author Enovia MatrixOne
 * @version ProductCentral 10.5 - Copyright (c) 2004, MatrixOne, Inc.
 */
public class ConfigurationRuleBase_mxJPO extends emxDomainObject_mxJPO
{

/**
  * Default Constructor.
  * @param context the eMatrix <code>Context</code> object
  * @param args holds no arguments
  * @throws Exception if the operation fails
  * @since R418
  * @grade 0
  */
  ConfigurationRuleBase_mxJPO (Context context, String[] args) throws Exception
  {
    super(context, args);
  }

 /**
  * Main entry point.
  *
  * @param context the eMatrix <code>Context</code> object
  * @param args holds no arguments
  * @return an integer status code (0 = success)
  * @throws Exception if the operation fails
  * @since R418
  * @grade 0
  */
  public int mxMain(Context context, String[] args) throws Exception
  {
    if (!context.isConnected()){
         String strContentLabel =EnoviaResourceBundle.getProperty(context,
        	        "Configuration","emxProduct.Error.UnsupportedClient",context.getSession().getLanguage());
         throw  new Exception(strContentLabel);
        }
    return 0;
  }

  
    
    /**
     * Method call to get all the configuration Rules in the data base in given context.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - HashMap containing one String entry for key "objectId"
     * @return Object - MapList containing the id of Configuration Rule objects
     * @throws Exception if the operation fails
     * @since R418
     * @grade 0
     */
     @com.matrixone.apps.framework.ui.ProgramCallable
     public MapList getAllConfigurationRuleObjects(Context context, String[] args) throws Exception
     {
    	 MapList mlExpressionRules = new MapList();

    	 try {
    		 HashMap reqMap = (HashMap) JPO.unpackArgs(args);
    		 String strProductId = (String)reqMap.get("objectId");
    		 strProductId =  strProductId.trim();//Context Product ID
    		 String strActualExpression="";
    		 strActualExpression=getProductRevisionExpression(context,strProductId);
    		 EffectivityFramework eff = new EffectivityFramework();
    		 Map effMap = eff.getFilterCompiledBinary(context, strActualExpression, EffectivityFramework.QUERY_MODE_STRICT);
    		 String strFilterBinary = (String)effMap.get(EffectivityFramework.COMPILED_BINARY_EXPR);

    		 String strModelId="";
    		 Product productBean = new Product(strProductId);
    		 strModelId = productBean.getModelId(context);
	        
       	StringList objSelects =new StringList(DomainConstants.SELECT_ID);
       	objSelects.add(DomainConstants.SELECT_TYPE);
       	StringList relSelects = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
       	relSelects.addElement(ConfigurationConstants.SELECT_ATTRIBUTE_MANDATORYRULE);
    		 Model mdoelBean = new Model(strModelId);
    		 mlExpressionRules = mdoelBean.getCfgRules(context, objSelects, relSelects, strFilterBinary);
    	 } catch (Exception e) {
    		 throw new FrameworkException(e.getMessage());
    	 }     	

    	 return mlExpressionRules;
       }   
       	
     /**
 	 * get formatted expression for Product 
 	 * @param context
 	 * @param contextId
 	 * @return
 	 * @throws Exception
 	 * @since R417.HF12
 	 */
  	private static String getProductRevisionExpression(Context context,String contextId) throws Exception
  	{
  		String modelId = "";
  		String modelPhysicalId = "";
  		String productPhysicalId = "";
  		StringList selList = new StringList();
  		selList.add(DomainObject.SELECT_ID);
  		selList.add(ConfigurationConstants.SELECT_PHYSICAL_ID);
  		selList.add("to[" + ConfigurationConstants.RELATIONSHIP_PRODUCTS + "].from.id");
  		selList.add("to[" + ConfigurationConstants.RELATIONSHIP_PRODUCTS + "].from.physicalid");
        
  		try
  		{		
  			DomainObject domProductBus = new DomainObject(contextId);
  			Map productInfo = domProductBus.getInfo(context, selList);
       	
  			if(productInfo != null && productInfo.size() > 0) {
  				productPhysicalId = (String)productInfo.get(ConfigurationConstants.SELECT_PHYSICAL_ID);
  				Object objModelId = productInfo.get("to[" + ConfigurationConstants.RELATIONSHIP_PRODUCTS + "].from.id");
  				if(objModelId != null)
  				{
  					modelId = (String)objModelId;
  					modelPhysicalId = (String)productInfo.get("to[" + ConfigurationConstants.RELATIONSHIP_PRODUCTS + "].from.physicalid");
  				}else{
  					modelId = (String)productInfo.get("to[" + ConfigurationConstants.RELATIONSHIP_MAIN_PRODUCT + "].from.id");
  					modelPhysicalId = (String)productInfo.get("to[" + ConfigurationConstants.RELATIONSHIP_MAIN_PRODUCT + "].from.physicalid");
  				}
  			}
  		}
  		catch(Exception e)
  		{
  			throw new FrameworkException(e.getMessage());
  		}
       	
  		JSONObject jsonObj = new JSONObject();
  		jsonObj.put("parentId", modelPhysicalId); 
  		jsonObj.put("insertAsRange", false);
  		com.matrixone.json.JSONArray valuesArray = new com.matrixone.json.JSONArray();
  		valuesArray.put(productPhysicalId);  
  		jsonObj.put("values", valuesArray);
  		String jsonString = jsonObj.toString();
  		EffectivityFramework effInstance = new EffectivityFramework();
  		Map formatedExpr = effInstance.formatExpression(context, "ProductRevision", jsonString);
  		String strProductRevisionEffExpr = formatedExpr.get(EffectivityFramework.ACTUAL_VALUE).toString();
  		return strProductRevisionEffExpr;
       }

}
