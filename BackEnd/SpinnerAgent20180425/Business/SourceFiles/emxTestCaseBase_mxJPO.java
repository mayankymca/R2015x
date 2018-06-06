/*
 *  emxTestCaseBase.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 * static const char RCSID[] = $Id: /ENOProductLine/CNext/Modules/ENOProductLine/JPOsrc/base/${CLASSNAME}.java 1.3.2.1.1.1 Wed Oct 29 22:17:06 2008 GMT przemek Experimental$
 *
 * formatted with JxBeauty (c) johann.langhofer@nextra.at
 */

 /**
 * @quickreview JX5 QYG	13:06:19 : IR-231962V6R2014x It cannot edit Test Case when browser language is Japanese
 * @quickreview T25 DJH 13:09:24 : IR-255996V6R2014x "The estimated completion date in the Test Case is not taking into account." Modified validateEstimateDate Method.
 * @quickreview T25 DJH 14:03:18 : HL Parameter under Test Case.Add method getAssociatedParameters.
 * @quickreview ZUD DJH 14:07:21 : Modified method getAssociatedParameters for IR-298238-3DEXPERIENCER2015x and IR-298340-3DEXPERIENCER2015x.
 * @quickreview KIE1 ZUD 15:04:06 : IR-352105-3DEXPERIENCER2015x R417-STP: Infinite loop is displayed for single node of Testcase in tree structure.
 * @quickreview KIE1 ZUD 15:09:08 : IR-395903-3DEXPERIENCER2015x If Estimated Completion Date is previous date when edit properties of Test Case, warning message will display and cannot edit properties
 */


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import  com.matrixone.apps.domain.DomainConstants;
import  com.matrixone.apps.domain.DomainObject;
import  com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import  com.matrixone.apps.domain.util.MapList;
import  com.matrixone.apps.domain.util.PropertyUtil;
import  com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.mxType;

import com.matrixone.apps.productline.ProductLineCommon;
import com.matrixone.apps.productline.ProductLineConstants;
import com.matrixone.apps.productline.TestCase;


import  java.util.HashMap;
import  java.util.Hashtable;
import  java.util.Iterator;
import java.util.Locale;
import  java.util.Map;
import  java.util.Vector;
import  matrix.db.Context;
import  matrix.db.JPO;
import  matrix.util.StringList;
import  matrix.util.Pattern;

/**
 * The <code>emxTestCase</code> class contains methods related to Test Case admin type.
 * @author Enovia MatrixOne
 * @version ProductCentral 10.5 - Copyright (c) 2004, MatrixOne, Inc.
 */
public class emxTestCaseBase_mxJPO extends emxDomainObject_mxJPO {

    /** A string constant with the value ".". */
    public static final String SYMB_DOT                  = ".";

    /**
     *A string constant with the value emxFrameworkStringResource.
     */
    public static final String RESOURCE_BUNDLE_FRAMEWORK_STR = "emxFrameworkStringResource";

    /**
     * Default constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since AEF
     * @grade 0
     */
    public emxTestCaseBase_mxJPO (Context context, String[] args) throws Exception
    {
        super(context, args);
    }

    /**
     * Main entry point.
     *
     * @param context context for this request
     * @param args holds no arguments
     * @return an integer status code (0 = success)
     * @throws Exception if the operation fails
     * @since ProductCentral 10-0-0-0
     * @grade 0
     */
    public int mxMain (Context context, String[] args) throws Exception {
        if (!context.isConnected()) {
            String language = context.getSession().getLanguage();
            String strContentLabel = EnoviaResourceBundle.getProperty(context,"ProductLine",
                   "emxProduct.Alert.FeaturesCheckFailed",language);
            throw  new Exception(strContentLabel);
        }
        return  0;
    }

    /**
     * Get the list of all TestCases on the context.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - HashMap containing the object id.
     * @return bus ids  and rel ids of Test Cases
     * @throws Exception if the operation fails
     * @since ProductCentral 10-0-0-0
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getRelatedTestCases (Context context, String[] args) throws Exception {
        MapList relBusObjPageList = new MapList();
        StringList objectSelects = new StringList(DomainConstants.SELECT_ID);
        StringList relSelects = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
        HashMap programMap = (HashMap)JPO.unpackArgs(args);
        String strObjectId = (String)programMap.get("objectId");
        setId(strObjectId);
        String strRel = (String)programMap.get("rel");
        short sRecursionLevel = 1;
        String strType = ProductLineConstants.TYPE_TEST_CASE;
        String strRelName = PropertyUtil.getSchemaProperty(context,strRel);
        relBusObjPageList = getRelatedObjects(context, strRelName, strType,
                objectSelects, relSelects, false, true, sRecursionLevel, "",
                "");
        return  relBusObjPageList;
    }

    /**
     * Get the list of all parent objects of the contextTestCase context.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - HashMap containing the object id.
     * @return bus ids  of parent objectsand rel ids of Test Cases
     * @throws Exception if the operation fails
     * @since ProductCentral 10-0-0-0
     */
    public MapList getTestCasesWhereUsed (Context context, String[] args) throws Exception {
        MapList relBusObjPageList = new MapList();
        StringList objectSelects = new StringList(DomainConstants.SELECT_ID);
        StringList relSelects = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
        HashMap programMap = (HashMap)JPO.unpackArgs(args);
        String strObjectId = (String)programMap.get("objectId");
        setId(strObjectId);
        String strTestCaseSubTestCaseReln = ProductLineConstants.RELATIONSHIP_SUB_TEST_CASE;
        String strTestCaseUseCaseReln = ProductLineConstants.RELATIONSHIP_USE_CASE_VALIDATION;
        String strTestCaseIncidentReln = ProductLineConstants.RELATIONSHIP_INCIDENT_VALIDATION;
        String strTestCaseRequirementReln = ProductLineConstants.RELATIONSHIP_REQUIREMENT_VALIDATION;
        String strTestCaseFeatureReln = ProductLineConstants.RELATIONSHIP_FEATURE_TEST_CASE;
        String strComma = ",";
        String strRelationshipPattern = strTestCaseSubTestCaseReln + strComma
                + strTestCaseUseCaseReln + strComma + strTestCaseIncidentReln
                + strComma + strTestCaseRequirementReln + strComma + strTestCaseFeatureReln;
        short sRecursionLevel = 1;
        relBusObjPageList = getRelatedObjects(context, strRelationshipPattern,
                "*", objectSelects, relSelects, true, false, sRecursionLevel,
                "", "");
        return  relBusObjPageList;
    }

    /** This method gets the object Structure List for the context Test Case object.This method gets invoked
      * by settings in the command which displays the Structure Navigator for Test Case type objects
      * @param context the eMatrix <code>Context</code> object
      * @param args    holds the following input arguments:
      *      paramMap   - Map having object Id String
      * @return MapList containing the object list to display in Test Case structure navigator
      * @throws Exception if the operation fails
      * @since Product Central 10-6
      */
    public static MapList getStructureList(Context context, String[] args) throws Exception{
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap   = (HashMap)programMap.get("paramMap");
        String objectId    = (String)paramMap.get("objectId");

        MapList testCaseStructList = new MapList();

        Pattern tcTypePattern      = new Pattern(ProductLineConstants.TYPE_TEST_CASE);
        Pattern subTCRelPattern    = new Pattern(ProductLineConstants.RELATIONSHIP_SUB_TEST_CASE);
        //Parameter under Test Case HL: Added plm parameter type and relationship 
        tcTypePattern.addPattern(PropertyUtil.getSchemaProperty(context, "type_PlmParameter"));
        subTCRelPattern.addPattern(PropertyUtil.getSchemaProperty(context, "relationship_ParameterAggregation"));
        DomainObject testCaseObj   = DomainObject.newInstance(context, objectId);
        String objectType          = testCaseObj.getInfo(context, DomainConstants.SELECT_TYPE);
      //Added mxType for IR-092856V6R2012 
        if(objectType != null && mxType.isOfParentType(context, objectType,ProductLineConstants.TYPE_TEST_CASE)){
            try {
                //START:LX6 JX5 14:06:06 : IR-302553-3DEXPERIENCER2015x
                // expand for all connected Sub Test Cases, Parameters and Test Execution
                DomainObject domObj = DomainObject.newInstance(context, objectId);

                StringList objectSelects = new StringList(3);
                objectSelects.add(DomainConstants.SELECT_ID);
                objectSelects.add(DomainConstants.SELECT_TYPE);
                objectSelects.add(DomainConstants.SELECT_NAME);
                StringList relSelects = new StringList(1);
                relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);

                //expand on the relationships in the passed relPattern object
				//fix for IR-352105-3DEXPERIENCER2015x
                testCaseStructList = domObj.getRelatedObjects(context,                  // matrix context
                											  subTCRelPattern.getPattern(),  // all relationships to expand
                											  tcTypePattern.getPattern(), // all types required from the expand
                                                              objectSelects,            // object selects
                                                              relSelects,               // relationship selects
                                                              false,                    // to direction
                                                              true,                     // from direction
                                                              (short) 1,                // recursion level
                                                              "",                       // object where clause
                                                              "");                      // relationship where clause
                // return expanded object connections
                //END : LX6 JX5 14:06:06 : IR-302553-3DEXPERIENCER2015x
            }
            catch(Exception ex){
                throw new FrameworkException(ex);
            }
        } else {
            testCaseStructList = (MapList) emxPLCCommon_mxJPO.getStructureListForType(context, args);
        }
        return testCaseStructList;
    }

    /**
     * Gets the list of all TestCases on the context Test Execution that have the
     * 'Validation Status' attribute value on the connecting relationship as 'Validation Passed'. This
     *  method will be able to show flat view of all such Test Cases connected to Test Execution
     * as during Test Execution create all Test Cases from parent object are collected into a flat structure
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args    holds the following input arguments:
     *            0 - HashMap containing the object id.
     * @return        a <code>MapList</code> object having the list of all Test Cases connected
     *                to the context Test Execution with 'Validation Passed' value for 'Validation Status'
     * @throws        Exception if the operation fails
     * @since         ProductCentral 10-6
     */

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getPassedTestExecutionTestCases (Context context, String[] args)
        throws Exception {

        MapList relBusObjPageList = new MapList();

        HashMap programMap = (HashMap)JPO.unpackArgs(args);
        String strObjectId = (String)programMap.get("objectId");
        String strRel      = (String)programMap.get("rel");
        String strRelName  = PropertyUtil.getSchemaProperty(context,strRel);

        StringBuffer sbRelWhere  = new StringBuffer(getAttributeSelect(DomainConstants.ATTRIBUTE_VALIDATION_STATUS));
        sbRelWhere.append("== \"");
        sbRelWhere.append(getAttributeRangeValue(context,DomainConstants.ATTRIBUTE_VALIDATION_STATUS, TestCase.TEST_CASE_VALIDATION_PASSED));
        sbRelWhere.append("\"");

        relBusObjPageList = getTestExecutionTestCases(context,
                                                      strObjectId,
                                                      strRelName,
                                                      EMPTY_STRING,
                                                      sbRelWhere.toString());
        return  relBusObjPageList;
    }

    /**
     * Gets the list of all TestCases on the context Test Execution that have the
     * 'Validation Status' attribute value on the connecting relationship as 'Validation Failed'. This
     *  method will be able to show flat view of all such Test Cases connected to Test Execution
     * as during Test Execution create all Test Cases from parent object are collected into a flat structure
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args    holds the following input arguments:
     *            0 - HashMap containing the object id.
     * @return        a <code>MapList</code> object having the list of all Test Cases connected
     *                to the context Test Execution with 'Validation Failed' value for 'Validation Status'
     * @throws        Exception if the operation fails
     * @since         ProductCentral 10-6
     */

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getFailedTestExecutionTestCases (Context context, String[] args)
        throws Exception {

        MapList relBusObjPageList = new MapList();

        HashMap programMap = (HashMap)JPO.unpackArgs(args);
        String strObjectId = (String)programMap.get("objectId");
        String strRel      = (String)programMap.get("rel");

        String strRelName   = PropertyUtil.getSchemaProperty(context,strRel);

        StringBuffer sbRelWhere = new StringBuffer(getAttributeSelect(DomainConstants.ATTRIBUTE_VALIDATION_STATUS));
        sbRelWhere.append("== \"");
        sbRelWhere.append(getAttributeRangeValue(context,DomainConstants.ATTRIBUTE_VALIDATION_STATUS, TestCase.TEST_CASE_VALIDATION_FAILED));
        sbRelWhere.append("\"");

        relBusObjPageList = getTestExecutionTestCases(context,
                                                      strObjectId,
                                                      strRelName,
                                                      EMPTY_STRING,
                                                      sbRelWhere.toString());
        return  relBusObjPageList;
    }

    /**
     * Gets the list of all TestCases on the context Test Execution that have the
     * 'Validation Status' attribute value on the connecting relationship as 'Not Validated'. This
     * method will be able to show flat view of all such Test Cases connected to Test Execution
     * as during Test Execution create all Test Cases from parent object are collected into a flat structure
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args    holds the following input arguments:
     *            0 - HashMap containing the object id.
     * @return        a <code>MapList</code> object having the list of all Test Cases connected
     *                to the context Test Execution with 'Not Validated' value for 'Validation Status'
     * @throws        Exception if the operation fails
     * @since         ProductCentral 10-6
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getNotValidatedTestExecutionTestCases (Context context, String[] args)
        throws Exception {

        MapList relBusObjPageList = new MapList();

        HashMap programMap = (HashMap)JPO.unpackArgs(args);
        String strObjectId = (String)programMap.get("objectId");
        String strRel      = (String)programMap.get("rel");

        String strRelName        = PropertyUtil.getSchemaProperty(context,strRel);

        StringBuffer sbRelWhere  = new StringBuffer(getAttributeSelect(DomainConstants.ATTRIBUTE_VALIDATION_STATUS));
        sbRelWhere.append("== \"");
        sbRelWhere.append(getAttributeRangeValue(context,DomainConstants.ATTRIBUTE_VALIDATION_STATUS, TestCase.TEST_CASE_NOT_VALIDATED));
        sbRelWhere.append("\"");

        relBusObjPageList = getTestExecutionTestCases(context,
                                                      strObjectId,
                                                      strRelName,
                                                      EMPTY_STRING,
                                                      sbRelWhere.toString());
        return  relBusObjPageList;
    }

    /**
     * Gets the list of all TestCases on the context Test Execution based on the 'bus where'
     * and relationship 'where' search clauses passed in as parameters for the object
     *
     * @param context              the eMatrix <code>Context</code> object
     * @param strParentId          id of the object for which Test Case relationship expansion is done
     * @param strRelName           the relationship that connects the parent object and child Test Cases
     * @param strBusWhereCondition the 'business object where' clause to be applied while expanding for 'Test Case' connections
     * @param strRelWhereCondition the 'relationship where' clause to be applied while expanding for 'Test Case' connections
     * @return                     a <code>MapList</code> object having the list of all connected Test Cases meeting search criteria
     * @throws                     Exception if the operation fails
     * @since                      ProductCentral 10-6
     */

    protected MapList getTestExecutionTestCases (Context context, String strParentId,
                                                 String strRelName, String strBusWhereCondition,
                                                 String strRelWhereCondition)
        throws Exception {

        MapList relBusObjPageList = new MapList();

        StringList objectSelects = new StringList(DomainConstants.SELECT_ID);
        StringList relSelects = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);

        setId(strParentId);

        short sRecursionLevel = 1;
        String strType        = ProductLineConstants.TYPE_TEST_CASE;

        relBusObjPageList = getRelatedObjects(context,
                                              strRelName,
                                              strType,
                                              objectSelects,
                                              relSelects,
                                              false,
                                              true,
                                              sRecursionLevel,
                                              strBusWhereCondition,
                                              strRelWhereCondition);
        return  relBusObjPageList;
    }

    /**
     * The method gets the Status Column in Test Execution Test Case Summary Table.
     * Returns the Status icon gif depending on the relationship attribute 'Validation Status'.
     * @param context the eMatrix <code>Context</code> object
     * @param args    holds the following input arguments:
     *          0 -   MapList containing Relationship id list
     * @return        a <code>Vector</code> object to return Status gif in Test Execution Test Case Summary.
     * @throws        Exception if the operation fails
     * @since         Product Central 10-6
     **
     */
    public Vector showTestCaseStatusGif(Context context, String[] args) throws Exception {
    	//XSSOK
        Vector returnVector   = new Vector();
        HashMap programMap    = (HashMap)JPO.unpackArgs(args);
        MapList objectList    = (MapList)programMap.get("objectList");
        int objectListSize    = 0 ;

        if(objectList != null) {
            objectListSize = objectList.size();
        }

        //Constructing the Object ids String []
        String stridsArray[]    = new String[objectListSize];
        for (int i = 0; i < objectListSize; i++) {
            try {
                   stridsArray[i] = (String)((HashMap)objectList.get(i)).get(DomainConstants.SELECT_RELATIONSHIP_ID);
            } catch (Exception ex) {
                   stridsArray[i] = (String)((Hashtable)objectList.get(i)).get(DomainConstants.SELECT_RELATIONSHIP_ID);
            }
        }

        //Getting the Relationship attribute, Validation Status
        String selectAttrValidationStatus = "attribute["+DomainConstants.ATTRIBUTE_VALIDATION_STATUS+"]";
        StringList listSelect = new StringList(selectAttrValidationStatus);
        MapList attributeMapList = DomainRelationship.getInfo(context,stridsArray,listSelect);
        Iterator attLstItr = attributeMapList.iterator();
        //Iterating through the attribute maplist and constructing the return vector.
        while(attLstItr.hasNext()) {
            Map tmpAtrMap = (Map)attLstItr.next();
            String AttrValidationStatus = (String)( tmpAtrMap.get(selectAttrValidationStatus) );
            if(AttrValidationStatus != null && AttrValidationStatus.equals(TestCase.TEST_CASE_VALIDATION_PASSED)) {
                returnVector.add("<img border='0' src='../common/images/iconStatusGreen.gif' />");
            } else if(AttrValidationStatus != null && AttrValidationStatus.equals(TestCase.TEST_CASE_VALIDATION_FAILED)) {
                returnVector.add("<img border='0' src='../common/images/iconStatusRed.gif' />");
            } else if(AttrValidationStatus != null && AttrValidationStatus.equals(TestCase.TEST_CASE_NOT_VALIDATED)) {
            	returnVector.add("&#160;");
            } else {
            	returnVector.add("&#160;");
            }
        } //end of while
        return returnVector;
    }

    /**
     * This method returns the value for a perticular range of a attribute
     * reading from emxFrameworkStringResource.propeties file. The key value is
     * dynamically generated using the passed attribute name and range
     * "name.emxFramework.Range. <Attribute Name>. <Range Name>". This value can
     * not be used for display purpose as the language string in GetString
     * method is passed as blank and so always engish string will be returned.
     *
     * @param strAttributeName String holding the actual name of the attribute.
     * @param strRange String holding the range of the attribbute.
     * @return        String The property file value of the range.
     * @throws        Exception if the operation fails
     * @since         Product Central 10-6
     */
    protected String getAttributeRangeValue(Context context,
            String strAttributeName, String strRange) throws Exception {

        //Form the property file key using the passed attribute name and range
        // name.The white spaces in the attribute and range name will be
        // replaced by underscore(_).
        StringBuffer sbKey = new StringBuffer();
        sbKey.append("emxFramework.Range.");
        sbKey.append(strAttributeName.replace(
                ' ', '_'));
        sbKey.append(SYMB_DOT);
        sbKey.append(strRange.replace(
                ' ', '_'));

        //Read the property file value by passing the generated key and
        // language string as blank.
        String strRangeValue = EnoviaResourceBundle.getProperty(context, "Framework",
                sbKey.toString(),DomainConstants.EMPTY_STRING);

        return strRangeValue;
    }
    
    /**
     * To obtain the list of Object IDs to be excluded from the search for Add Existing Actions
     *
     * @param context- the eMatrix <code>Context</code> object
     * @param args- holds the HashMap containing the following arguments
     * @return  StringList- consisting of the object ids to be excluded from the Search Results
     * @throws Exception if the operation fails
     * @author OEP:R208:Bug 370645
     */
    
    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList excludeSubTestCases(Context context, String[] args) throws Exception
    {
	        Map programMap = (Map) JPO.unpackArgs(args);    
	        String strObjectIds = (String)programMap.get("objectId");
	        String strRelationship=(String)programMap.get("relName");
	        StringList excludeList= new StringList();  
	        DomainObject domObjTestCase  = new DomainObject(strObjectIds);
	        

	        // Code for removing the Parent Id's
	        MapList parentObjects=domObjTestCase.getRelatedObjects(context, 
	                PropertyUtil.getSchemaProperty(context,strRelationship),
	                "*",
	                new StringList(DomainConstants.SELECT_ID), 
	                null, 
	                true, 
	                false, 
	               (short) 0,
	                DomainConstants.EMPTY_STRING, 
	                DomainConstants.EMPTY_STRING);
	         
	        for(int i=0;i<parentObjects.size();i++){
	            Map tempMap=(Map)parentObjects.get(i);
	            excludeList.add(tempMap.get(DomainConstants.SELECT_ID));
	        }
	        
	        // Code use to remove those objects which are already added in list.
	       MapList currentObjectIDs=domObjTestCase.getRelatedObjects(context, 
	        	PropertyUtil.getSchemaProperty(context,strRelationship),
	        	"*",
	        	new StringList(DomainConstants.SELECT_ID), 
	                null, 
	                false, true,
	                (short)1, 
	                null, 
	                null,0);
	        
	        for(int iCount=0;iCount<currentObjectIDs.size();iCount++)
	        {
	            Map tempMap=(Map)currentObjectIDs.get(iCount);
	            String tempID = (String)tempMap.get(DomainConstants.SELECT_ID);
	            excludeList.add(tempID);
	        }
	        
	        excludeList.add(strObjectIds);
	        return excludeList;
    }
    
    /**
     * Estimated Completion Date field make mandatory or not based on a configuration property entry.
     * Called from WebForm type_TestCase
     * @param context- the eMatrix <code>Context</code> object
     * @param args- holds the HashMap containing the following arguments
     */
    
    public static boolean isEstimatedDateRequired(Context context, String[] args) throws Exception
	{
    	try
    	{
		String strEnforceMandatoryEstimatedCompletionDate = EnoviaResourceBundle 
					.getProperty(context,
							"emxProduct.TestCase.EnforceEstimatedCompletionDate");

			if ("true".equals(strEnforceMandatoryEstimatedCompletionDate)) {
				return true;
			} else {
				return false;
			}
    	}  catch(Exception ex){
            throw new FrameworkException(ex);
        }
	}
    
    /**
     * Estimated Completion Date field make mandatory or not based on a configuration property entry
     * Called from WebForm type_TestCase
     * @param context- the eMatrix <code>Context</code> object
     * @param args- holds the HashMap containing the following arguments
     */
    
    public static boolean isEstimatedDateNotRequired(Context context, String[] args) throws Exception
	 	{
    	try {
			String strEnforceMandatoryEstimatedCompletionDate = EnoviaResourceBundle
					.getProperty(context,
							"emxProduct.TestCase.EnforceEstimatedCompletionDate");
			if ("false".equals(strEnforceMandatoryEstimatedCompletionDate)) {
				return true;
			} else {
				return false;
			}
    	}  catch(Exception ex){
            throw new FrameworkException(ex);
        }
	}
    
    /**
     * Method is used to validate the Estimated Completion Date field
     * 
     * @param context the eMatrix <code>Context</code> object 
     * @param args hold the hashMap containing the following argument
     * @returns void 
     * @throws Exception if operation fails.
     * @since R212
     */
    public void validateEstimateDate(Context context, String[] args) throws Exception
    {
    	try{
    	  HashMap hashMap = (HashMap)JPO.unpackArgs(args);
          HashMap paramMap = (HashMap) hashMap.get("paramMap");
          String strObjectId = (String) paramMap.get("objectId");
          String strNewValue = (String) paramMap.get("New Value");
          //JX5 IR-231962V6R2014x
          HashMap requestMap = (HashMap) hashMap.get("requestMap");
          DateFormat df = new SimpleDateFormat();
          Calendar calendar =  Calendar.getInstance();
          
          java.util.Date strEstimatedCompletionDate =null;
          
		 //START: T25 DJH 2013:09:24:IR IR-255996V6R2014x .Correction done by JX5 for IR 231962V6R2014x is taken out of else block.
    	  double iClientTimeOffset = (new Double((String) requestMap.get("timeZone"))).doubleValue();
    	  Locale locale = (Locale)requestMap.get("localeObj");
    	  strNewValue = eMatrixDateFormat.getFormattedInputDate(context, strNewValue,
                  iClientTimeOffset,
                  locale);
    	  //END T25 DJH
    	  
          strEstimatedCompletionDate = com.matrixone.apps.domain.util.eMatrixDateFormat.getJavaDate(strNewValue);
          
          //Start KIE1 ZUD : IR-395903-3DEXPERIENCER2015x
          java.util.Date strOldEstimatedCompletionDate =null;
          String strOldValue = (String) paramMap.get("Old value");
          strOldEstimatedCompletionDate = com.matrixone.apps.domain.util.eMatrixDateFormat.getJavaDate(strOldValue);
          
          if(!strOldEstimatedCompletionDate.equals(strEstimatedCompletionDate))
          {
        	//Start KIE1 ZUD : IR-395903-3DEXPERIENCER2015x
	          calendar.setTime(strEstimatedCompletionDate);
	          Date newCompletionDate = calendar.getTime();
	          int day_of_month = calendar.get(Calendar.DAY_OF_MONTH);
	          
	          Date dtTodaysDate = df.parse(df.format(new Date()));
	          calendar.setTime(dtTodaysDate);
	          int tday_of_month = calendar.get(Calendar.DAY_OF_MONTH);
	          
	          if(newCompletionDate.before(dtTodaysDate) && day_of_month != tday_of_month)
	          {
	              String language = context.getSession().getLanguage();
	              String strInvalidEstimateDate = EnoviaResourceBundle.getProperty(context,"ProductLine",
	            		  "emxProduct.TestCase.Alert.InvalidEstimatedDate",language);
	            
	              throw new Exception(strInvalidEstimateDate);
	          }
	          else
	          {
	        	  DomainObject dom = new DomainObject(strObjectId);
	        	  dom.setAttributeValue(context, ProductLineConstants.ATTRIBUTE_ESTIMATED_COMPLETION_DATE , strNewValue);
	          }
          }
         
    	}catch(Exception ex)
    	{
    		throw new FrameworkException(ex.getMessage());
    	}
    }
    
    //getAssociatedParameters() added for Parameter under Test Case HL: 
    @com.matrixone.apps.framework.ui.ProgramCallable
 	  public MapList getAssociatedParameters(Context context, String args[]) throws Exception
    {
 		  try
      {
 
 			HashMap programMap = (HashMap) JPO.unpackArgs(args);
 			String objectId = (String)programMap.get("objectId");
 			
 			String toTypeName = PropertyUtil.getSchemaProperty(context, "type_PlmParameter");
			  String relationships = PropertyUtil.getSchemaProperty(context, "relationship_ParameterAggregation");
            
           DomainObject dom = new DomainObject(objectId);
            int sRecurse = 0;
            
            StringList objSelects = new StringList(1);
            objSelects.addElement(DomainConstants.SELECT_LEVEL);
            objSelects.addElement("id[connection]");
            objSelects.addElement(DomainConstants.SELECT_ID);
            objSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_NAME);
            
            StringList relSelects = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
            
            MapList relBusObjPageList = new MapList();
            
			//Modified method getAssociatedParameters for IR-298238-3DEXPERIENCER2015x and IR-298340-3DEXPERIENCER2015x.
            relBusObjPageList = dom.getRelatedObjects(context,
            		relationships,
            		toTypeName,
            		objSelects,
            		relSelects,
                    false,
                    true,
                    (short)0,
                    null,
                    null);

           
           return relBusObjPageList;
 			
 		}
 		catch (Exception ex){
  			System.out.println("getAssociatedParameters - exception " + ex.getMessage());
   			throw ex;
 		} 
 	}
}


