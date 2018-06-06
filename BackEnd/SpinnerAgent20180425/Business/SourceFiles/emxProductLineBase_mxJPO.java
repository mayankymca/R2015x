/*
 ** emxProductLineBase
 **
 ** Copyright (c) 1992-2015 Dassault Systemes.
 **
 ** All Rights Reserved.
 ** This program contains proprietary and trade secret information of
 ** MatrixOne, Inc.  Copyright notice is precautionary only and does
 ** not evidence any actual or intended publication of such program.
 **
 ** static const char RCSID[] = $Id: /java/JPOsrc/base/emxProductLineBase.java 1.3.2.1.1.1.1.1 Wed Dec 17 11:04:07 2008 GMT ds-dpathak Experimental$
 *
 * formatted with JxBeauty (c) johann.langhofer@nextra.at
 */



import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.productline.DerivationUtil;
import com.matrixone.apps.productline.Model;
import com.matrixone.apps.productline.Product;
import com.matrixone.apps.productline.ProductLineCommon;
import com.matrixone.apps.productline.ProductLineConstants;
import com.matrixone.apps.productline.ProductLineUtil;
/**
 * This JPO class has some methods pertaining to Product Line type.
 * @author Enovia MatrixOne
 * @version ProductCentral 10.5 - Copyright (c) 2004, MatrixOne, Inc.
 */
public class emxProductLineBase_mxJPO extends emxDomainObject_mxJPO {

/** Alias for string current. */
 protected static final String CURRENT = DomainConstants.SELECT_CURRENT;
/** Alias for key emxProduct.ModelAndSubProductLineCreateAddExisting. */
 protected static final String KEY_FIRST_PART = "emxProduct.ModelAndSubProductLineCreateAddExisting";
/** Alias for string NotAllowedStates. */
 protected static final String KEY_LAST_PART = "NotAllowedStates";
/** Alias for budle string. */
 protected static final String BUNDLE_STR = "emxProduct";
/** Alias for dot. */
 protected static final String DOT = ".";

    /**
     * Create a new emxProductLine object from a given id.
     *
   * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments.
     * @return a emxProductLine object
     * @throws Exception if operation fails
     * @since ProductCentral 10.0.0.0
     * @grade 0
     */
    public emxProductLineBase_mxJPO (Context context, String[] args) throws Exception
    {
        super(context, args);
    }

    /**
     * Main entry point.
     *
   * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @return an integer status code (0 = success)
     * @throws Exception if operation fails
     * @since ProductCentral 10.0.0.0
     * @grade 0
     */
    public int mxMain (Context context, String[] args) throws Exception {
        if (!context.isConnected()) {
            String language = context.getSession().getLanguage();
            String strContentLabel = EnoviaResourceBundle.getProperty(context, "ProductLine","emxProduct.Alert.FeaturesCheckFailed", language);
            throw  new Exception(strContentLabel);
        }
        return  0;
    }

    /**
     * Method call to get all the Product Lines in the data base.
     *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the following input arguments:
     *        0 - HashMap containing one String entry for key "objectId"
   * @return Object - MapList containing the id of all Product Line objects related to context User's Company.
     * @throws Exception if operation fails
     * @since ProductCentral 10.0.0.0
     * @grade 0
     */
    @Deprecated
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getAllProductLines (Context context, String[] args) throws Exception {
        //Instantiating a StringList for fetching value of Company Id
        StringList strCompany = ProductLineUtil.getUserCompanyIdName(context);
        //The id of the company
        String strCompanyId = (String)strCompany.get(0);
        // forming the where clause
        String strRelationship = ProductLineConstants.RELATIONSHIP_COMPANY_PRODUCT_LINES;
        StringBuffer strWhereExpression = new StringBuffer(85);
        strWhereExpression.append("to[").append(strRelationship).append("].from.").append(DomainConstants.SELECT_ID).append("=='").append(strCompanyId).append("'");
        //Calls the protected method to retrieve the data
        MapList mapBusIds = getDesktopProductLines(context, strWhereExpression.toString(),null);
        return  mapBusIds;
    }

    /**
     * Get the list of all owned ProductLines.
     *
   * @param context the eMatrix <code>Context</code> object
     * @param args - Holds the parameters passed from the calling method.
     * @return Object - MapList containing the id of all owned Product Line objects by the context User.
     * @throws Exception if operation fails
     * @since ProductCentral 10.0.0.0
     * @grade 0
     */
    @Deprecated
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getOwnedProductLines (Context context, String[] args) throws Exception {
        //Instantiating a StringList for fetching value of Company Id
        StringList strCompany = ProductLineUtil.getUserCompanyIdName(context);
        //The id of the company
        String strCompanyId = (String)strCompany.get(0);
        // forming the where clause
        String strRelationship = ProductLineConstants.RELATIONSHIP_COMPANY_PRODUCT_LINES;
        StringBuffer strWhereExpression = new StringBuffer(85);
        strWhereExpression.append("to[").append(strRelationship).append("].from.").append(DomainConstants.SELECT_ID).append("=='").append(strCompanyId).append("'");
        // forming the Owner Pattern clause
        String strOwnerCondition = context.getUser();
        //Calls the protected method to retrieve the data
        MapList mapBusIds = getDesktopProductLines(context, strWhereExpression.toString(),strOwnerCondition);
        return  mapBusIds;
    }

    /**
     * Get the list of Desktop Product Lines.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param strWhereCondition string containing the where condition for selection.
     * @param strOwnerCondition string containing the owner condition for selection.
     * @return MapList - MapList containing the id of Desktop Requirement objects based on whereCondition .
     * @throws Exception if the operation fails
     * @since ProductCentral 10.0.0.0
     * @grade 0
     */
    protected MapList getDesktopProductLines (Context context, String strWhereCondition, String strOwnerCondition) throws Exception {
        //String list initialized to retrieve data for the Product Lines
        StringList objectSelects = new StringList(DomainConstants.SELECT_ID);
        String strType = ProductLineConstants.TYPE_PRODUCT_LINE;
        //The findobjects method is invoked to get the list of products
        //modified findobjecs to expand parameter form false to true for IR-093222V6R2012
        MapList mapBusIds = findObjects(context, strType, null,null,strOwnerCondition,DomainConstants.QUERY_WILDCARD,strWhereCondition,true, objectSelects);
        return  mapBusIds;
    }

    /**
     * Get the list of All owned Sub Product Lines related to a Product line.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following arguments
      0 - Hashmap containing the object id
     * @return MapList - MapList containing the id of all Sub Product Lines connecting to that Product Line.
     * @throws Exception if the operation fails
     * @since ProductCentral 10.0.0.0
     * @grade 0
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getOwnedRelatedProductLines (Context context, String[] args) throws Exception {
        //Unpacks the argument for processing
        HashMap programMap = (HashMap)JPO.unpackArgs(args);
        //Gets the objectId in context
        String strObjectId = (String)programMap.get("objectId");
        //Instantiating a StringList for fetching value of Company Id
        StringList strCompany = ProductLineUtil.getUserCompanyIdName(context);
        //The id of the company
        String strCompanyId = (String)strCompany.get(0);
        // forming the objectWhere clause
        String strRelationship = ProductLineConstants.RELATIONSHIP_COMPANY_PRODUCT_LINES;
        StringBuffer objectWhere = new StringBuffer(85);
        objectWhere.append("to[").append(strRelationship).append("].from.").append(DomainConstants.SELECT_ID).append("=='").append(strCompanyId).append("'");
        objectWhere.append("&& owner== \"").append(context.getUser()).append("\"");

        //Calls the protected method to retrieve the data
        MapList mapBusIds = getRelatedProductLines(context, objectWhere.toString(), strObjectId);
        return  mapBusIds;
    }

    /**
     * Get the list of All  Sub Product Lines related to a Product line.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following arguments
      0 - Hashmap containing the object id
     * @return MapList - MapList containing the id of all Sub Product Lines connecting to that Product Line.
     * @throws Exception if the operation fails
     * @since ProductCentral 10.0.0.0
     * @grade 0
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getAllRelatedProductLines (Context context, String[] args) throws Exception {
        //Unpacks the argument for processing
        HashMap programMap = (HashMap)JPO.unpackArgs(args);
        //Gets the objectId in context
        String strObjectId = (String)programMap.get("objectId");
        String expandLevel = (String) programMap.get("expandLevel");
		// If the ExpandLevel is all then set the recurse level to 0
		if(expandLevel == null){
			expandLevel = "1";
		} else if(ProductLineConstants.RANGE_VALUE_ALL.equalsIgnoreCase(expandLevel)){
			expandLevel = "0";
		}
        //Instantiating a StringList for fetching value of Company Id
        StringList strCompany = ProductLineUtil.getUserCompanyIdName(context);
        //The id of the company
        String strCompanyId = (String)strCompany.get(0);
        // forming the objectWhere clause
        String strRelationship = ProductLineConstants.RELATIONSHIP_COMPANY_PRODUCT_LINES;
        StringBuffer objectWhere = new StringBuffer(86);
        objectWhere.append("to[").append(strRelationship).append("].from.").append(DomainConstants.SELECT_ID).append("=='").append(strCompanyId).append("'");

        //Calls the protected method to retrieve the data
        MapList mapBusIds = getRelatedProductLines(context, objectWhere.toString(), strObjectId,expandLevel);
        return  mapBusIds;
    }

    /**
     * Get the list of Related Product Lines.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param objectWhere string containg the where condition
   * @param strObjectId string containing the object id
     * @return MapList - MapList containing the id of Desktop Requirement objects based on whereCondition .
     * @throws Exception if the operation fails
     * @since ProductCentral 10.0.0.0
     * @grade 0
     */
    protected MapList getRelatedProductLines (Context context, String objectWhere,String strObjectId) throws Exception {
        //OOTB it was level 1
    	String expandLevel = "1";
        //The getRelatedObjects method is invoked to get the list of Sub Product Lines.
        MapList relBusObjPageList =getRelatedProductLines (context, objectWhere,strObjectId,expandLevel) ;
        return  relBusObjPageList;
    }

    protected MapList getRelatedProductLines (Context context, String objectWhere,String strObjectId, String strExpandLevel) throws Exception {
        //String List initialized to retrieve back the data
        StringList relSelects = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
        StringList objectSelects = new StringList(DomainConstants.SELECT_ID);
        //Sets the relationship name to the one connecting Sub ProductLine.
        String strSubProductLineReln = ProductLineConstants.RELATIONSHIP_SUB_PRODUCT_LINES;
        String strRelationship = strSubProductLineReln;
        //Domain Object initialized with the object id.
        setId(strObjectId);
        short recurseLevel = Short.parseShort(strExpandLevel);
        String strType = ProductLineConstants.TYPE_PRODUCT_LINE;

        //The getRelatedObjects method is invoked to get the list of Sub Product Lines.
        MapList relBusObjPageList = getRelatedObjects(context, strRelationship,
                strType, objectSelects, relSelects, false, true, recurseLevel, objectWhere, DomainConstants.EMPTY_STRING,0);
        return  relBusObjPageList;
    }    


 /* Modified by <Tanmoy Chatterjee> On <26th May 2003>
    For the following Bug
    <Description- Sub ProductLine/Models can be added to Product line in Active/Inactive State..>
    Fixed as Follows:
    <added the following functions and updated the tool.xls>
*/

 /**
  * Method call to check whether the product/feature is in design engineering state.
  *
  * @param context the eMatrix <code>Context</code> object
  * @param args holds the following arguments
    0 - HashMap containing the object id.
  * @return Object - A Boolean object
  * @throws Exception if operation fails
  * @since ProductCentral 10-0-0-0
  * @grade 0
  */
  public Boolean linkDisplay(Context context, String[] args) throws Exception{
    HashMap argumentMap = (HashMap) JPO.unpackArgs(args);
      String strObjectId = (String)argumentMap.get("objectId");
      strObjectId = strObjectId.trim();
      setId(strObjectId);
      String strType = getInfo(context,"type");
      //System.out.println("strType->"+strType);

      //get the current state of the object
      String strCurrentState = getInfo(context,CURRENT);
      strCurrentState = removeSpaces(strCurrentState);
      //get the default policy of the type->product or features
      String strPolicy = getDefaultPolicy(context,strType);
      strPolicy = removeSpaces(strPolicy);
    //System.out.println("strCurrentState->"+strCurrentState+"strPolicy->"+strPolicy);
    //forming the key which will be present in the emxProduct.properties
    StringBuffer sKey = new StringBuffer(85);
      sKey  = sKey.append(KEY_FIRST_PART).append(DOT).append(strPolicy).append(DOT).append(KEY_LAST_PART);
      //System.out.println("KEY->"+sKey);
      String strKey = sKey.toString();
    //get the Locale in the present context
    String strLocale = context.getSession().getLanguage();
    //get the value corresponding to the key from the emxProductcentral.properties file
    String strNotAllowedStates =  EnoviaResourceBundle.getProperty(context, "ProductLine",strKey,strLocale);
    //System.out.println("strNotAllowedStates->"+strNotAllowedStates);
    //check if the current state of the object is amongst the not allowed states
    if((strNotAllowedStates.indexOf(strCurrentState))!=-1){
      //the current state is not amongst the allowed state. in that case return false
      //this false will be used by the emxTable.jsp and the create new link will not be displayed
      Boolean b = new Boolean(false);
      return b;
    }else{
      //the link will not be displayed by the emxTable.jsp
      Boolean b = new Boolean(true);
      return b;
    }

  }



/**
  * Method call to remove spaces fmor a string.
  *
  * @param strInput the input string
  * @return string after removing the spaces
  * @throws Exception if operation fails
  * @since ProductCentral 10-0-0-0
  * @grade 0
  */
  protected String removeSpaces(String strInput) throws Exception{

    StringBuffer sOutput = new StringBuffer(20);
    StringTokenizer st = new StringTokenizer(strInput);
    while (st.hasMoreTokens()) {
             sOutput = sOutput.append(st.nextToken());
    }
    return   sOutput.toString();

  }

   /** This method gets the object Structure List for the context Product Line object.This method gets invoked
     * by settings in the command which displays the Structure Navigator for Product Line type objects
     *  @param context the eMatrix <code>Context</code> object
     *  @param args    holds the following input arguments:
     *      paramMap   - Map having object Id String
     *  @return MapList containing the object list to display in Product Line structure navigator
     *  @throws Exception if the operation fails
     *  @since Product Central 10-6
     */

    public static MapList getStructureList(Context context, String[] args)
        throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap   = (HashMap)programMap.get("paramMap");
        String objectId    = (String)paramMap.get("objectId");

        MapList productlineStructList = new MapList();

        Pattern relPattern = new Pattern(ProductLineConstants.RELATIONSHIP_SUB_PRODUCT_LINES);
        relPattern.addPattern(ProductLineConstants.RELATIONSHIP_PRODUCTLINE_MODELS);

        // include type 'Product Line, Model' in Product Line structure navigation list
        Pattern typePattern     = new Pattern("*");
        DomainObject productLineObj = DomainObject.newInstance(context, objectId);
        String objectType     = productLineObj.getInfo(context, DomainConstants.SELECT_TYPE);        
        String strSymbolicName = FrameworkUtil.getAliasForAdmin(context,DomainConstants.SELECT_TYPE, objectType, true);
        String strAllowedSTRel = "";        
        try{
        	strAllowedSTRel = EnoviaResourceBundle.getProperty(context,"emxConfiguration.StructureTree.SelectedRel."+strSymbolicName);
        }catch (Exception e) {
		}
        if(strAllowedSTRel.equals("")){
        	strAllowedSTRel = EnoviaResourceBundle.getProperty(context, "emxConfiguration.StructureTree.SelectedRel.type_ProductLine");
        }          
        String[] arrRel = null;
        String strRelPattern = "";
	    if(strAllowedSTRel!=null && !strAllowedSTRel.equals("")){
	    	arrRel = strAllowedSTRel.split(",");	    
		    for(int i=0; i< arrRel.length; i++){
		    	strRelPattern = strRelPattern + "," + PropertyUtil.getSchemaProperty(context,arrRel[i]);
		    }
		    strRelPattern = strRelPattern.replaceFirst(",", "");
	    }    

	    String derivationType = null;
	    if (objectType != null && strRelPattern.equals(ProductLineConstants.RELATIONSHIP_PRODUCTS)) {
    		// Get the root product of the model
   	     	Model objModel = new Model();
    	    String strRootId = objModel.getDerivationRoot(context, objectId);
    	    if(ProductLineCommon.isNotNull(strRootId)){
    	    	// Get the info for the Root Product
    	    	StringList slObjSelects = new StringList(DomainObject.SELECT_ID);
    	    	slObjSelects.add(DomainObject.SELECT_NAME);
    	    	slObjSelects.add(DomainObject.SELECT_REVISION);
    	    	slObjSelects.add(DomainObject.SELECT_RELATIONSHIP_ID);
    	    	slObjSelects.add(DomainObject.SELECT_RELATIONSHIP_NAME);

    	    	// Get the information for the root object.
    	    	DomainObject domObject = new DomainObject(strRootId);
    	    	Map mpSingleObject = domObject.getInfo(context, slObjSelects);
    	    	derivationType = domObject.getInfo(context, DomainObject.SELECT_TYPE);

    	    	// Get the rest of the Derivations from the Root Product
    	    	productlineStructList = DerivationUtil.getAllDerivations(context, strRootId);
    	    	if (productlineStructList != null && mpSingleObject != null) {
    	    		productlineStructList.add(mpSingleObject);
    	    		// Sort the List
    	    		productlineStructList.addSortKey(DomainObject.SELECT_NAME, "ascending", "String");
    	    		productlineStructList.addSortKey(DomainObject.SELECT_REVISION, "ascending", "String");
    	    		productlineStructList.sort();
    	    	}
    	    }
    	} else if(objectType != null &&  mxType.isOfParentType(context, objectType,ProductLineConstants.TYPE_PRODUCT_LINE)) {
            try {
                productlineStructList = ProductLineCommon.getObjectStructureList(context, objectId, relPattern, typePattern);             
            } catch(Exception ex) {
               throw new FrameworkException(ex);
            }
        } else {
            productlineStructList = (MapList) emxPLCCommon_mxJPO.getStructureListForType(context, args);
        }

        //Begin of Add by Enovia MatrixOne for Bug 301624 on 5/5/2005
/*        Map tempMap;
        String strType ="";
        for(int i=0;i<productlineStructList.size();i++)
        {
            tempMap = (Map)productlineStructList.get(i);
            strType = (String)tempMap.get(DomainConstants.SELECT_TYPE);
            if(mxType.isOfParentType(context,strType,ProductLineConstants.TYPE_PRODUCT_LINE) )
            {
                tempMap.put("treeMenu","type_PLCProductLine");
            }

        }*/
        //End of Add by Enovia MatrixOne for Bug 301624 on 5/5/2005
        return productlineStructList;
    }

    /* ADDED FOR Version R207 */

    /**
     * This method is used to restrict from displaying the row
     * in Products and Features Property page
     * if Library Central is not installed
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args:
     *          args for the method
     *
     * @throws Exception if the operation fails
     * @return boolean
     * @since R207
     */
    public boolean showClassificationPath(Context context,String[] args) throws Exception
    {
            boolean isLCInstalled = false;
            boolean flDisplayField = false;
            isLCInstalled = FrameworkUtil.isSuiteRegistered(context,"appVersionLibraryCentral", false, null, null);
            if(isLCInstalled)
            {
                flDisplayField = true;
            }
        return flDisplayField;
    }

    /**
     * This method is used to restrict from displaying the Requirement Added/Removed event from the subscription panel if Requirements Management Central is not installed.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args not used for the method
     *
     * @throws Exception if the operation fails
     * @return boolean
     * @since R207
     */
    public boolean showRequirementAddedRemovedEvent(Context context,String[] args) throws Exception
    {//added for bug 368884
       boolean isREQInstalled = FrameworkUtil.isSuiteRegistered(context, "appVersionRequirementsManagement", false, null, null);
        return isREQInstalled;
    }
    
    /**
	 * Label program for  Tree structure
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
     * @since R213
     */
    public String getDisplayNameForNavigator(Context context,String[] args) throws Exception
    {	 		
		String strTreeName = ProductLineUtil.getDisplayNameForFeatureNavigator(context, args);

		return strTreeName;
	 }
    /**
     * Method call to get all the Product Lines in the data base.
     *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the following input arguments:
     *        0 - HashMap containing one String entry for key "objectId"
   * @return Object - MapList containing the id of all Product Line objects related to context User's Company.
     * @throws Exception if operation fails
     * @since R213    
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getAllTopLevelProductLines (Context context, String[] args) throws Exception {
        //Instantiating a StringList for fetching value of Company Id
        StringList strCompany = ProductLineUtil.getUserCompanyIdName(context);
        //The id of the company
        String strCompanyId = (String)strCompany.get(0);
        // forming the where claus
        String strRelationship = ProductLineConstants.RELATIONSHIP_COMPANY_PRODUCT_LINES;
        StringBuffer strWhereExpression = new StringBuffer(85);
        strWhereExpression.append("to[").append(strRelationship).append("].from.").append(DomainConstants.SELECT_ID).append("=='").append(strCompanyId).append("'");
        strWhereExpression.append(" && ");
        strWhereExpression.append("to["+ProductLineConstants.RELATIONSHIP_SUB_PRODUCT_LINES+"]==False");
        //Calls the protected method to retrieve the data
        MapList mapBusIds = getDesktopProductLines(context, strWhereExpression.toString(),null);
        return  mapBusIds;
    }
    /**
     * Method call to get all the top level Product Lines owned by the context user.
     *
   * @param context the eMatrix <code>Context</code> object
   * @param args holds the following input arguments:
     *        0 - HashMap containing one String entry for key "objectId"
   * @return Object - MapList containing the id of all Product Line objects related to context User's Company.
     * @throws Exception if operation fails
     * @since R213    
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getAllTopLevelOwnedProductLines (Context context, String[] args) throws Exception {
        //Instantiating a StringList for fetching value of Company Id
        StringList strCompany = ProductLineUtil.getUserCompanyIdName(context);
        //The id of the company
        String strCompanyId = (String)strCompany.get(0);
        // forming the where claus
        String strRelationship = ProductLineConstants.RELATIONSHIP_COMPANY_PRODUCT_LINES;
        StringBuffer strWhereExpression = new StringBuffer(85);
        strWhereExpression.append("to[").append(strRelationship).append("].from.").append(DomainConstants.SELECT_ID).append("=='").append(strCompanyId).append("'");
        strWhereExpression.append(" && ");
        strWhereExpression.append("to["+ProductLineConstants.RELATIONSHIP_SUB_PRODUCT_LINES+"]==False");
        
        //Calls the protected method to retrieve the data
        MapList mapBusIds = getDesktopProductLines(context, strWhereExpression.toString(),context.getUser());
        return  mapBusIds;
    }
    /**
     * for getting the company name
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public static StringList getCompany(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);	
		StringList vaultList = new StringList();	
	    //Instantiating a StringList for fetching value of Company Id and Company name
	    StringList strCompany = (StringList)ProductLineUtil.getUserCompanyIdName(context);
	    //The id of the company
	    String strCompanyId = (String)strCompany.get(0);
	    //The name of the company
	    String strCompanyName = (String)strCompany.get(1);
		vaultList.add(strCompanyName);		
		return vaultList;
	}
    
   	/**
	 * This method connects the newly created model to the context Product Line or the product line chosen
	 *
	 * @param context
	 *            the eMatrix <code>Context</code> object
	 * @param args -
	 *            Holds the following arguments 0 - HashMap containing the
	 *            following arguments
	 * @return void
	 * @throws Exception
	 *             if the operation fails
	 */
   
    public void updateProductLineProgram(Context context, String[] args)
    throws Exception{
    	
     		HashMap programMap = (HashMap)JPO.unpackArgs(args);
    		HashMap paramMap   = (HashMap)programMap.get("paramMap");
    		HashMap requestMap   = (HashMap)programMap.get("requestMap");
    		String strParentID = null;
    		
    		// below code is applicable when the Product Line is created from Global Actions
    		String[] strarrayPL = (String[])requestMap.get("Program");
    		if(strarrayPL != null){
    			strParentID = strarrayPL[0];
    		}
     		// Below code connects the newly created Product Line to the  Program
    		if(strParentID!=null && !strParentID.equals("")){
    			
    			DomainObject parentObj = new DomainObject(strParentID);
    			String newObjID = (String)paramMap.get("objectId");
    			// Connect the Model to the Context Product Line
    			DomainRelationship.connect(context, newObjID, 
    					PropertyUtil.getSchemaProperty(context,"relationship_ProductLineProgram"), strParentID,false);
    			
    			
    		}
    }
    
}
