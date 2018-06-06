/*
**	emxCommonValuesBase
**
**	Copyright (c) 1993-2015 Dassault Systemes.
**	All Rights Reserved.
**  This program contains proprietary and trade secret information of
**  Dassault Systemes.
**  Copyright notice is precautionary only and does not evidence any actual
**  or intended publication of such program
*/

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.apps.configuration.CommonValues;
import com.matrixone.apps.configuration.ConfigurationConstants;
import com.matrixone.apps.configuration.ConfigurationUtil;
import com.matrixone.apps.configuration.LogicalFeature;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.productline.ProductLineUtil;

public class emxCommonValuesBase_mxJPO extends emxDomainObject_mxJPO
{

	String strCommonGroupIcon = "iconSmallCommonGroup.gif";
	String strConfigurableFeatureIcon = "iconSmallConfigurableFeature.gif";
	String strConfigurationOptionsIcon = "iconSmallConfigurationOption.gif";
	String strConfigurationFeatureIcon = "iconSmallConfigurationfeature.gif";
	String strProductIcon = "iconSmallProduct.gif";
	// The operator symbols
	/** A string constant with the value &&. */
	protected static final String SYMB_AND = " && ";
	/** A string constant with the value ||. */
	protected static final String SYMB_OR = " || ";
	/** A string constant with the value ==. */
	protected static final String SYMB_EQUAL = " == ";
	/** A string constant with the value !=. */
	protected static final String SYMB_NOT_EQUAL = " != ";
	/** A string constant with the value >. */
	protected static final String SYMB_GREATER_THAN = " > ";
	/** A string constant with the value <. */
	protected static final String SYMB_LESS_THAN = " < ";
	/** A string constant with the value >=. */
	protected static final String SYMB_GREATER_THAN_EQUAL = " >= ";
	/** A string constant with the value <=. */
	protected static final String SYMB_LESS_THAN_EQUAL = " <= ";
	/** A string constant with the value ~~. */
	protected static final String SYMB_MATCH = " ~~ ";  // Short term fix for Bug #243366, was " ~~ "
	/** A string constant with the value '. */
	protected static final String SYMB_QUOTE = "'";
	/** A string constant with the value *. */
	protected static final String SYMB_WILD = "*";
	/** A string constant with the value (. */
	protected static final String SYMB_OPEN_PARAN = "(";
	/** A string constant with the value ). */
	protected static final String SYMB_CLOSE_PARAN = ")";
	/** A string constant with the value attribute. */
	protected static final String SYMB_ATTRIBUTE = "attribute";
	/** A string constant with the value [. */
	protected static final String SYMB_OPEN_BRACKET = "[";
	/** A string constant with the value ]. */
	protected static final String SYMB_CLOSE_BRACKET = "]";
	/** A string constant with the value to. */
	protected static final String SYMB_TO = "to";
	/** A string constant with the value from. */
	protected static final String SYMB_FROM = "from";
	/** A string constant with the value ".". */
	protected static final String SYMB_DOT = ".";
	/** A string constant with the value "null". */
	protected static final String SYMB_NULL = "null";

	protected static final String FIELD_DISPLAY_CHOICES = "field_display_choices";
     /** A string constant with the value field_choices. */
    protected static final String FIELD_CHOICES = "field_choices";
    
    public static final String SELECT_PHYSICALID ="physicalid";
    public static final String SUITE_KEY ="Configuration";

    /**
    * Default Constructor.
    *
  * @param context the eMatrix <code>Context</code> object
  * @param args holds no arguments
    * @throws Exception if the operation fails
    * @since ProductCentral 10.0.0.0
    */
    public emxCommonValuesBase_mxJPO(Context context, String[] args) throws Exception
    {
        super(context, args);
    }

    /**
    * Main entry point into the JPO class. This is the default method that will be excuted for this class.
    *
    * @param context the eMatrix <code>Context</code> object
    * @param args holds no arguments
    * @return int - An integer status code (0 = success)
    * @throws Exception if the operation fails
    * @since ProductCentral 10.0.0.0
    */
    public int mxMain(Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
        {
           //  throw new Exception(strContentLabel);
		           return 1;

        }
        return 0;
    }

private String executeMqlCommand(Context context  , String strMqlCommand ) throws Exception{

	String strResult = MqlUtil.mqlCommand(context , strMqlCommand ,true );
	if (strResult == null)
	{
		strResult = "";
	}
	return strResult ; 
}

	/**
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 * 
	 */
 public Map getRangeValuesForGlobalCommonGroup(Context context, String[] args) throws Exception
{
	
	String strAttributeName = ConfigurationConstants.ATTRIBUTE_GLOBAL_COMMON_GROUP;
	HashMap rangeMap = new HashMap();
	matrix.db.AttributeType attribName = new matrix.db.AttributeType(strAttributeName);
	attribName.open(context);

	List attributeRange = attribName.getChoices();
	List attributeDisplayRange = i18nNow.getAttrRangeI18NStringList(ConfigurationConstants.ATTRIBUTE_GLOBAL_COMMON_GROUP,
					   (StringList)attributeRange,context.getSession().getLanguage());


	rangeMap.put(FIELD_CHOICES , attributeRange);
	rangeMap.put(FIELD_DISPLAY_CHOICES , attributeDisplayRange);
	return  rangeMap;
}
/**
 * 
 * @param context
 * @param args
 * @return
 * @throws Exception
 * 
 */
public StringList isColumnEditable(Context context, String[] args )throws Exception
{
    try {
        HashMap inputMap = (HashMap)JPO.unpackArgs(args);
        MapList objectMap = (MapList) inputMap.get("objectList");
        StringList returnStringList = new StringList (objectMap.size());
        
        for (int i = 0; i < objectMap.size(); i++) {
            Map table = (Map) objectMap.get(i);
            String strRelId = (String)table.get("id[connection]");
            
        	if (strRelId != null && !"".equals(strRelId)) {
        		String strMqlCommand = "print connection \""+strRelId+"\" select frommid["+ConfigurationConstants.RELATIONSHIP_COMMON_GROUP+"].id dump | "; 
        		String strCommonValues = executeMqlCommand(context , strMqlCommand) ;
        		if (strCommonValues != null && "".equals(strCommonValues)) {
                	returnStringList.add(Boolean.valueOf(true));
        		} else {
                	returnStringList.add(Boolean.valueOf(false));
        		}
        	} else {
            	returnStringList.add(Boolean.valueOf(true));
        	}
        }

        return returnStringList;

        
    } catch(Exception e) {
        e.printStackTrace();
        throw new FrameworkException(e.getMessage());
    }
}
 /**
  * 
  * @param context
  * @param args
  * @return
  * @throws Exception
  * 
  */
public Boolean updateGlobalCommonGroupValue(Context context,String[] args) throws Exception
{

	HashMap programMap = (HashMap)JPO.unpackArgs(args);
	HashMap paramMap = (HashMap)programMap.get("paramMap");

	String strRelId = (String)paramMap.get("relId");
	String strNewAttribval  = (String)paramMap.get("New Value");

    String language = context.getSession().getLanguage();
    String strAlertMessage =EnoviaResourceBundle.getProperty(context,SUITE_KEY,language,"emxProduct.Alert.GlobalCommonGroupModify");

	
	if (strRelId != null && !"".equals(strRelId))
	{
		String strMqlCommand = "print connection \""+strRelId+"\" select frommid["+ConfigurationConstants.RELATIONSHIP_COMMON_GROUP+"].id dump | "; 

		String strCommonValues = executeMqlCommand(context , strMqlCommand) ;

		DomainRelationship domRel = new DomainRelationship(strRelId);

		if (strCommonValues != null && "".equals(strCommonValues)){
			domRel.setAttributeValue(context ,ConfigurationConstants.ATTRIBUTE_GLOBAL_COMMON_GROUP , strNewAttribval  );	
		}else{
            emxContextUtilBase_mxJPO contextUtil = new  emxContextUtilBase_mxJPO(context,null) ; 
			contextUtil.mqlNotice(context , strAlertMessage);
		}
	}

	return Boolean.valueOf(true);
  }


////////////////////////////////////////////////////////////////////R212 Code//////////////////////////////////////////////////
/**
* This method is used to get Common Group values attached to the Common Values
* To display the "Common Group" column values in Config Option Table 
* @param context     the eMatrix <code>Context</code> object
* @param args 		 Contains String array containing all the required parameters 
* @return			 Vector containing the Common Groups 
* @exception		 Exception if operation fails
* @category 
*/
public Vector getCommonGroupColumnValues(Context context, String[] args) throws Exception
{
	HashMap programMap = (HashMap)JPO.unpackArgs(args);
	MapList objectList =  (MapList)programMap.get("objectList");
	int iNumOfObjects = objectList.size() ; 
	String strMqlCmd = "" ;
	String strCGRelId = "";
	String strAttribName = "";
	Vector vecCGNames = new Vector(iNumOfObjects);

	HashMap paramList = (HashMap)programMap.get("paramList");

	String strRelId = (String)paramList.get("relId");
	String strDVId = (String)paramList.get("objectId");
	CommonValues cgBean = new CommonValues();
	String strGlobalCG = "";
	if(  null!= strRelId && !"null".equals(strRelId) && !"".equals(strRelId)){
		strGlobalCG = cgBean.getGlobalCommonGroupValue(context , strRelId);
	}
	
	String strPdtId = "" ; 
	String strTechFId = "" ; 
	MapList mlCtxCGs = new MapList();
				
	String strParentOID = (String)paramList.get("parentOID");
	if( null != strParentOID && !"null".equals(strParentOID) && !"".equals(strParentOID)){
	StringTokenizer stringTokenizer = new StringTokenizer(strParentOID ,",");
	strTechFId = stringTokenizer.nextToken();
	if(stringTokenizer.hasMoreTokens()){
		strPdtId = stringTokenizer.nextToken();
	}
	 mlCtxCGs = cgBean.getCommonGroups(context ,strTechFId , strDVId ) ;
	}


	Vector vCGIds = new Vector(mlCtxCGs.size());		
	String strCtxCommonGroupId = "";
	HashMap map = new HashMap();
	HashMap PdtCGInfo1 = new HashMap();
	String strCtxId = "" ; 
	if (strGlobalCG.equalsIgnoreCase("No")){
		
		for (int iCnt = 0 ;iCnt < mlCtxCGs.size() ;iCnt++ ) {
	
			PdtCGInfo1 = (HashMap)mlCtxCGs.get(iCnt);
			strCtxId =(String)PdtCGInfo1.get("Context Prod Id");

			if (strCtxId != null && !"".equals(strCtxId)&& strCtxId.equals(strPdtId)){
				vCGIds.add((String)PdtCGInfo1.get("Common Group Id") );	
			}
		}
	}else{
			for (int i = 0 ;i < mlCtxCGs.size() ;i++ ){
					
				map =(HashMap) mlCtxCGs.get(i);
				strCtxCommonGroupId  = (String)map.get("Common Group Id");
				vCGIds.add(strCtxCommonGroupId );	
			}
	}


	for (int iCnt = 0; iCnt < iNumOfObjects ; iCnt++ ){

		
		Hashtable objInfoTable = (Hashtable)objectList.get(iCnt);
		String strCORelId = (String)objInfoTable.get("id[connection]");
		
		strMqlCmd = "print connection $1 select $2 dump $3;" ;
		StringBuffer selectable = new StringBuffer();
		selectable.append("tomid[")
				.append(ConfigurationConstants.RELATIONSHIP_COMMON_VALUES)
				.append("].fromrel.id")	;
		
		strCGRelId = MqlUtil.mqlCommand(context , strMqlCmd ,true, strCORelId,selectable.toString(),ConfigurationConstants.DELIMITER_PIPE);

		
		if (strCGRelId != null && !"".equals(strCGRelId)){
		
			if (strGlobalCG.equalsIgnoreCase("No")){

				if (!strCGRelId.contains("|") ){
					
					strMqlCmd = "print connection $1 select $2 dump $3" ;
					StringBuffer selectable2 = new StringBuffer();
					selectable2.append("tomid[")
							.append(ConfigurationConstants.RELATIONSHIP_CONTEXT_COMMON_GROUP)
							.append("].from.id")	;
					strCtxId  = MqlUtil.mqlCommand(context , strMqlCmd ,true,strCGRelId ,selectable2.toString(),ConfigurationConstants.DELIMITER_PIPE);
					if(strPdtId.equals(strCtxId) && vCGIds.contains(strCGRelId)){
						DomainRelationship domCGRelObj = new DomainRelationship(strCGRelId);
						strAttribName = domCGRelObj.getAttributeValue(context , ConfigurationConstants.ATTRIBUTE_COMMON_GROUP_NAME);
						if (strAttribName == null ){
							strAttribName  = "";
						}					
					}else{
						strAttribName  = "";
					}
					
				}else {
						String strCGId = "" ; 
						StringTokenizer strTokens = new StringTokenizer(strCGRelId,"|");
						while(strTokens.hasMoreTokens()){
							strAttribName = "" ; 
							strCGId = strTokens.nextToken();
							strMqlCmd = "print connection $1 select $2 dump $3 " ;
							StringBuffer selectable3 = new StringBuffer();
							selectable3.append("tomid[")
									.append(ConfigurationConstants.RELATIONSHIP_CONTEXT_COMMON_GROUP)
									.append("].from.id")	;
							strCtxId  = MqlUtil.mqlCommand(context , strMqlCmd ,true,strCGId,selectable3.toString(),ConfigurationConstants.DELIMITER_PIPE );								

							if(!"".equals(strCtxId) && strPdtId.equals(strCtxId) && vCGIds.contains(strCGId)){
								DomainRelationship domCGRelObj = new DomainRelationship(strCGId);
								strAttribName = domCGRelObj.getAttributeValue(context , ConfigurationConstants.ATTRIBUTE_COMMON_GROUP_NAME);
								if (strAttribName == null ){
									strAttribName  = "";
								}	
								break ; 
							}else{
								strAttribName  = "";
							}
						}
					}
			}else{

						if (!strCGRelId.contains("|") ){
							if (vCGIds.contains(strCGRelId)){									
								DomainRelationship domCGRelObj = new DomainRelationship(strCGRelId);
								strAttribName = domCGRelObj.getAttributeValue(context , ConfigurationConstants.ATTRIBUTE_COMMON_GROUP_NAME);
								if (strAttribName == null ){
									strAttribName  = "";
								}
							}else{
								strAttribName  = "";
							}						
						}else{
								StringTokenizer strTokens = new StringTokenizer(strCGRelId,"|");
								String strCGId = "" ; 
								while(strTokens.hasMoreTokens()){
									strAttribName = "" ; 
									strCGId = strTokens.nextToken();

									if (vCGIds.contains(strCGId)){									
										DomainRelationship domCGRelObj = new DomainRelationship(strCGId);	
										strAttribName = domCGRelObj.getAttributeValue(context , ConfigurationConstants.ATTRIBUTE_COMMON_GROUP_NAME);
										break;
									}
								}
								if (strAttribName == null ){
									strAttribName  = "";
								}

							}
					}
			}else {
				strAttribName  = "";
			}

		vecCGNames.add(strAttribName);

	}

	return vecCGNames;

} //end of getCommonGroup


/**
* This method is used to get the Common Values attached to the Common Group
* @param context     the eMatrix <code>Context</code> object
* @param strCGId			Contains the Rel id of the Common Group
* @return					 Vector containing the Common Values 
* @exception				 Exception if operation fails
* @since					 Feature Configuration V6R2008-1
*/
private Vector getCommonValuesDataForCG(Context context , String strCGRelId) throws Exception
{
	//Use getRelationshipData and get the CG Rel Ids
	Vector vecCommonValues = new Vector();
	DomainRelationship domCGRelId = new DomainRelationship(strCGRelId);
	StringList sLRelSelect = new StringList("frommid["+ConfigurationConstants.RELATIONSHIP_COMMON_VALUES+"].torel."+DomainConstants.SELECT_ID+"");
	sLRelSelect.addElement("frommid["+ConfigurationConstants.RELATIONSHIP_COMMON_VALUES+"].torel.to."+DomainConstants.SELECT_ID+"");
	sLRelSelect.addElement("frommid["+ConfigurationConstants.RELATIONSHIP_COMMON_VALUES+"].torel.to."+DomainConstants.SELECT_NAME+"");
	
	Hashtable htCGInfo = domCGRelId.getRelationshipData(context,sLRelSelect);
        
	StringList slConfigOptRelIds = (StringList)htCGInfo.get("frommid["+ConfigurationConstants.RELATIONSHIP_COMMON_VALUES+"].torel."+DomainConstants.SELECT_ID+"");
	StringList slConfigOptIds = (StringList)htCGInfo.get("frommid["+ConfigurationConstants.RELATIONSHIP_COMMON_VALUES+"].torel.to."+DomainConstants.SELECT_ID+"");
	StringList slConfigOptNames = (StringList)htCGInfo.get("frommid["+ConfigurationConstants.RELATIONSHIP_COMMON_VALUES+"].torel.to."+DomainConstants.SELECT_NAME+"");
	
	HashMap hsCGMap =  new HashMap();	
	hsCGMap.put("Common Group Id" ,strCGRelId);
	hsCGMap.put("Config Options RelIds" ,slConfigOptRelIds);
	hsCGMap.put("Config Options Ids" ,slConfigOptIds);
	hsCGMap.put("Config Options Names" ,slConfigOptNames);
	vecCommonValues.add(hsCGMap);	
	
	return vecCommonValues ; 
	
}	
	/*
	String strMqlCommand = "print connection \""+strCGId+"\" select frommid["+ConfigurationConstants.RELATIONSHIP_COMMON_VALUES+"].to."+DomainConstants.SELECT_ID+"  dump |";
	String strCommonValues = executeMqlCommand(context , strMqlCommand ) ;
	StringTokenizer strTokens = new StringTokenizer(strCommonValues,"|");
	
	while(strTokens.hasMoreTokens()){
		vecCommonValues.add((String)strTokens.nextToken());	
	}
	return vecCommonValues ; 
}*/

public Vector populateCommonGroupValuesInViewCGListPage(Context context , String[] args) throws Exception{
	
	HashMap programMap = (HashMap)JPO.unpackArgs(args);
	MapList objectList =  (MapList)programMap.get("objectList");
    String languageStr = context.getSession().getLanguage();
	String i18CG=EnoviaResourceBundle.getProperty(context,SUITE_KEY, languageStr,"emxProduct.Tooltip.CommonGroup");
	String i18CV=EnoviaResourceBundle.getProperty(context,SUITE_KEY, languageStr,"emxProduct.Tooltip.CommonValues");
	String i18CGContext=EnoviaResourceBundle.getProperty(context,SUITE_KEY, languageStr,"emxProduct.Tooltip.CGContext");
	Vector vNames = new Vector(objectList.size());
	String strName = ""; 
	HashMap ctxCGInfo = null ;
	StringBuffer sbBuffer  = new StringBuffer(400);
	String strType  = "" ;
	String exportFormat = null;
	boolean exportToExcel = false;     
	HashMap requestMap = (HashMap)programMap.get("paramList");
	if(requestMap!=null && requestMap.containsKey("reportFormat")){
		exportFormat = (String)requestMap.get("reportFormat");
	}
	if("CSV".equals(exportFormat)){
		exportToExcel = true;
	}
	
	for ( int i = 0; i <  objectList.size() ; i++ )
	{
		sbBuffer = sbBuffer.delete(0,sbBuffer.length());
		ctxCGInfo = (HashMap)objectList.get(i) ; 
		strName = (String)ctxCGInfo.get("name") ; 
		String tempStrName = strName;
		strType = (String)ctxCGInfo.get("type") ; 
		if (strName == null){		
			strName = "" ;
		}
		 // Start - Specia Character - Added HTML equivalent code for &,<," & > - Bug No. 361962
		if(strName.indexOf("&") != -1 || strName.indexOf("<") != -1 || strName.indexOf(">") != -1 || strName.indexOf("\"") != -1 ){
			strName = FrameworkUtil.findAndReplace(strName , "&","&amp;");	
			strName = FrameworkUtil.findAndReplace(strName , "<", "&lt;");	
			strName = FrameworkUtil.findAndReplace(strName , ">", "&gt;");	
			strName = FrameworkUtil.findAndReplace(strName , "\"", "&quot;");	
		}
		 // End - Bug No. 361962
		if(exportToExcel && strName != null){
			vNames.add(tempStrName);			
		}
		else{
		if (strType != null && !"".equals(strType) && strType.equals("CommonGroup")){
				sbBuffer = sbBuffer.append("<img src=\"../common/images/")
                                .append(strCommonGroupIcon)
                                .append("\" border=\"0\"  align=\"middle\" ")
                                .append("TITLE=\"")
                                .append(" ")
                                .append(i18CG)
                                .append("\"")
                                .append("/><B>")
								.append(strName)
								.append("</B>") ; 
				strName = sbBuffer.toString();

			} else if (strType != null
					&& !"".equals(strType)
					&& (strType.equals("CommonValues") || mxType
							.isOfParentType(
									context,
									strType,
									ConfigurationConstants.TYPE_LOGICAL_STRUCTURES))) {
				//TODO- check when type will be logical Feature
				String strObjId = (String) ctxCGInfo.get("id");
				String i18Title=i18CGContext;
				String strConfigurableFeatureIcon = "";
				if (strType
						.equalsIgnoreCase(ConfigurationConstants.TYPE_SOFTWARE_FEATURE)
						|| ProductLineUtil.getChildrenTypes(context,
								ConfigurationConstants.TYPE_SOFTWARE_FEATURE)
								.contains(strType)) {
					strConfigurableFeatureIcon = "iconSmallSoftwareFeature.gif";
				} else {
					strConfigurableFeatureIcon = "iconSmallLogicalFeature.gif";
				}
				
				if (!strObjId.contains("*") && !strObjId.contains("&")) {
					DomainRelationship domRelId = new DomainRelationship(
							strObjId);
					StringList sLRelSelect = new StringList(
							ConfigurationConstants.SELECT_TO_TYPE);
					Hashtable htCGInfo = domRelId.getRelationshipData(context,
							sLRelSelect);
					StringList toType = (StringList) htCGInfo
							.get(ConfigurationConstants.SELECT_TO_TYPE);
					strType = (String) toType.get(0);
					if (mxType.isOfParentType( context , strType ,ConfigurationConstants.TYPE_CONFIGURATION_FEATURE))
						strConfigurableFeatureIcon = strConfigurationFeatureIcon;
					else
						strConfigurableFeatureIcon = strConfigurationOptionsIcon;
					i18Title=i18CV;
				}
				sbBuffer = sbBuffer.append("<img src=\"../common/images/")
						.append(strConfigurableFeatureIcon).append(
								"\" border=\"0\"  align=\"middle\" ").append(
								"TITLE=\"").append(" ").append(i18Title)
						.append("\"").append("/><B>").append(strName).append(
								"</B>");
				strName = sbBuffer.toString();
			}else if (strType != null && !"".equals(strType) && mxType.isOfParentType( context , strType ,ConfigurationConstants.TYPE_PRODUCTS) ){
			
				sbBuffer = sbBuffer.append("<img src=\"../common/images/")
                                .append(strProductIcon)
                                .append("\" border=\"0\"  align=\"middle\" ")
                                .append("TITLE=\"")
                                .append(" ")
                                .append(i18CGContext)
                                .append("\"")
                                .append("/><B>")
								.append(strName)
								.append("</B>");
			strName = sbBuffer.toString();
		}
		
		vNames.add(strName) ; 		
	}
	}
	 return vNames ; 
}

/**
* This method is to display "Type" Column value in View Common Group List Page Table 
* @param context     the eMatrix <code>Context</code> object
* @param args 		 Contains String array containing all the required parameters 
* @return			 Vector containing values of "Type"
* @throws FrameworkException
* @since R212
* @author IXH
* @category NON API
*/

public Vector getTypeInViewCGListPage(Context context , String[] args) throws Exception{


	HashMap programMap = (HashMap)JPO.unpackArgs(args);
	MapList objectList =  (MapList)programMap.get("objectList");
	Vector vTypes = new Vector(objectList.size());
	String strObjId = ""; 
	HashMap ctxCGInfo = null ;

	StringBuffer sbBuffer  = new StringBuffer(400);
	String strType = "" ;
	String strI18Type = "" ;
	for ( int i = 0; i <  objectList.size() ; i++ )
	{
		sbBuffer = sbBuffer.delete(0,sbBuffer.length());
		strType = "" ; 
		strI18Type = "" ; 
		ctxCGInfo = (HashMap)objectList.get(i) ; 

		strObjId = (String)ctxCGInfo.get("id") ; 
		if (!strObjId.contains("*") && !strObjId.contains("&") ){ 
			
			DomainRelationship domRelId = new DomainRelationship(strObjId);
			StringList sLRelSelect = new StringList("to."+ConfigurationConstants.SELECT_TYPE+"");
			Hashtable htCGInfo = domRelId.getRelationshipData(context,sLRelSelect);
			
			StringList slType = (StringList) htCGInfo.get("to."+ConfigurationConstants.SELECT_TYPE+"");
			strType = (String)slType.get(0);

			if (strType == null ){
				strType = "";				
				}
			if(!strType.isEmpty())
			 strI18Type = ConfigurationUtil.geti18FrameworkString(
					context, strType);
			}
		vTypes.add(strI18Type) ; 
	}
	return vTypes ; 
}

/**
* This method is to display "State" Column value in View Common Group List Page Table 
* @param context     the eMatrix <code>Context</code> object
* @param args 		 Contains String array containing all the required parameters 
* @return			 Vector containing values of "State"
* @throws FrameworkException
* @since R212
* @author IXH
* @category NON API
*/
public Vector getStateInViewCGListPage(Context context , String[] args) throws Exception{


	HashMap programMap = (HashMap)JPO.unpackArgs(args);
	MapList objectList =  (MapList)programMap.get("objectList");
	Vector vStates = new Vector(objectList.size());
	String strObjId = ""; 
	HashMap ctxCGInfo = null ;

	StringBuffer sbBuffer  = new StringBuffer(400);
	String strI18State = "" ;
	String strState = "" ;
	String strPolicy = "" ;
	String strLanguage = context.getSession().getLanguage();
	for ( int i = 0; i <  objectList.size() ; i++ )
	{
		sbBuffer = sbBuffer.delete(0,sbBuffer.length());
		strState = "" ;
		strPolicy = "" ;
		strI18State = "" ;
		ctxCGInfo = (HashMap)objectList.get(i) ; 

		strObjId = (String)ctxCGInfo.get("id") ; 
		if (!strObjId.contains("*") && !strObjId.contains("&") ){ 

			DomainRelationship domRelId = new DomainRelationship(strObjId);
			StringList sLRelSelect = new StringList("to."+ConfigurationConstants.SELECT_CURRENT);
			sLRelSelect.addElement("to."+DomainConstants.SELECT_POLICY);
			Hashtable htCGInfo = domRelId.getRelationshipData(context,sLRelSelect);

			StringList slState = (StringList)htCGInfo.get("to."+ConfigurationConstants.SELECT_CURRENT);
			StringList slPolicy = (StringList)htCGInfo.get("to."+ConfigurationConstants.SELECT_POLICY);
			strState = (String)slState.get(0);
			strPolicy = (String)slPolicy.get(0);
            String policy_Name= strPolicy.replace(' ', '_');
            String state_Name= strState.replace(' ', '_');
			String  stateKey = "emxFramework.State."+policy_Name+"."+state_Name;
			strI18State=EnoviaResourceBundle.getProperty(context,"Framework",stateKey,strLanguage);
			if (strI18State == null ){
				strI18State = "";				
			}
		}
		vStates.add(strI18State) ; 
	}
	return vStates ; 
}


/**
* This method is to display "Name" Column value in View Common Group List Page Table 
* @param context     the eMatrix <code>Context</code> object
* @param args 		 Contains String array containing all the required parameters 
* @return			 Vector containing values of "Name"
* @throws FrameworkException
* @since R212
* @author IXH
* @category NON API
*/

public Vector getNameInViewCGListPage(Context context , String[] args) throws Exception{


	HashMap programMap = (HashMap)JPO.unpackArgs(args);
	MapList objectList =  (MapList)programMap.get("objectList");
	Vector vNames = new Vector(objectList.size());
	String strObjId = ""; 
	HashMap ctxCGInfo = null ;

	StringBuffer sbBuffer  = new StringBuffer(400);
	String strName = "" ;

	for ( int i = 0; i <  objectList.size() ; i++ )
	{
		sbBuffer = sbBuffer.delete(0,sbBuffer.length());
		strName = "" ; 
		ctxCGInfo = (HashMap)objectList.get(i) ; 

		strObjId = (String)ctxCGInfo.get("id") ; 
		if (!strObjId.contains("*") && !strObjId.contains("&") ){ 

			DomainRelationship domRelId = new DomainRelationship(strObjId);
			StringList sLRelSelect = new StringList("to."+ConfigurationConstants.SELECT_NAME+"");
			Hashtable htCGInfo = domRelId.getRelationshipData(context,sLRelSelect);
		        
			StringList slName = (StringList)htCGInfo.get("to."+ConfigurationConstants.SELECT_NAME+"");
			strName = (String)slName.get(0);

			if (strName == null ){
				strName = "";				
				}
			}
		vNames.add(strName) ; 
	}
	return vNames ; 
}


/**
* This method is to display "Default Selection" attribute value in View Common Group List Page Table 
* @param context     the eMatrix <code>Context</code> object
* @param args 		 Contains String array containing all the required parameters 
* @return			 Vector containing values of "Default Selection" Yes /No
* @throws FrameworkException
* @since R212
* @author IXH
* @category NON API
*/

public Vector getDefaultSelectionInViewCGListPage(Context context , String[] args) throws Exception{

	HashMap programMap = (HashMap)JPO.unpackArgs(args);
	MapList objectList =  (MapList)programMap.get("objectList");
	Vector vDefaultSels = new Vector(objectList.size());
	String strObjId = ""; 
	HashMap ctxCGInfo = null ;

	StringBuffer sbBuffer  = new StringBuffer(400);
	String strDefaultSel = "" ;
	String i18StrDefaultSel = "" ;
	String strLanguage = context.getSession().getLanguage();

	for ( int i = 0; i <  objectList.size() ; i++ )	{
		sbBuffer = sbBuffer.delete(0,sbBuffer.length());
		strDefaultSel = "" ; 
		i18StrDefaultSel="";
		ctxCGInfo = (HashMap)objectList.get(i) ; 

		strObjId = (String)ctxCGInfo.get("id") ; 
		if (!strObjId.contains("*") && !strObjId.contains("&") ){ 

			DomainRelationship domRelConfOpt =  new DomainRelationship(strObjId);
			strDefaultSel= domRelConfOpt.getAttributeValue(context , ConfigurationConstants.ATTRIBUTE_DEFAULT_SELECTION) ; 
			if (strDefaultSel == null ){
				strDefaultSel = "";				
			}
			if(!strDefaultSel.isEmpty()){
				String attributeName= ConfigurationConstants.ATTRIBUTE_DEFAULT_SELECTION.replace(' ', '_');
				String  rangeKey = "emxFramework.Range."+attributeName+"."+strDefaultSel;
				i18StrDefaultSel=EnoviaResourceBundle.getProperty(context,"Framework",rangeKey,strLanguage);
			}
		}
		vDefaultSels.add(i18StrDefaultSel) ; 
	}
	return vDefaultSels ; 
}

/**
* This method is to get "Variant Option" Column value in View Common Group List Page Table 
* @param context     the eMatrix <code>Context</code> object
* @param args 		 Contains String array containing all the required parameters 
* @return			 Vector containing values of Variant option
* @throws FrameworkException
* @since R212
* @author IXH
* @category NON API
*/
public Vector getVariantOptionInViewCGListPage(Context context , String[] args) throws Exception{

	//XSSOK Deprecated 
	HashMap programMap = (HashMap)JPO.unpackArgs(args);
	HashMap requestMap = (HashMap) programMap.get("paramList");
	String strRprtFrmt = (String) requestMap.get("reportFormat");
	 String exportFormat = null;
     boolean exportToExcel = false;
     if(requestMap!=null && requestMap.containsKey("reportFormat")){
    	 exportFormat = (String)requestMap.get("reportFormat");
     }
     if("CSV".equals(exportFormat)){
    	 exportToExcel = true;
     }
	String strstart = "";
	String strEnd = "";
	if(strRprtFrmt != null){
		strstart = "<a>";
		strEnd = "</a>";
	}
	MapList objectList =  (MapList)programMap.get("objectList");
	Vector vDVNames = new Vector(objectList.size());
	String strConfRelId = ""; 
	HashMap ctxCGInfo = null ;

	StringBuffer sbBuffer  = new StringBuffer(400);
	String strDVName = "" ;
    String languageStr = context.getSession().getLanguage();
	String i18VariantOption=EnoviaResourceBundle.getProperty(context,SUITE_KEY,"emxProduct.Tooltip.VariantOption",languageStr);
	
	for ( int i = 0; i <  objectList.size() ; i++ )
	{
		sbBuffer = sbBuffer.delete(0,sbBuffer.length());
		strDVName = "" ; 
		ctxCGInfo = (HashMap)objectList.get(i) ; 

		strConfRelId = (String)ctxCGInfo.get("id") ; 
		if (!strConfRelId.contains("*") && !strConfRelId.contains("&") ){ 

			DomainRelationship domRelId = new DomainRelationship(strConfRelId);
			StringList sLRelSelect = new StringList("from."+ConfigurationConstants.SELECT_ATTRIBUTE_DISPLAY_NAME+"");
			sLRelSelect.addElement(DomainObject.SELECT_FROM_TYPE);
			Hashtable htCGInfo = domRelId.getRelationshipData(context,sLRelSelect);
		        
			StringList slVariantOption = (StringList)htCGInfo.get("from."+ConfigurationConstants.SELECT_ATTRIBUTE_DISPLAY_NAME+"");
			strDVName = (String)slVariantOption.get(0);
			
			String strFromType="";
			if (!((StringList) htCGInfo.get(DomainObject.SELECT_FROM_TYPE)).isEmpty())
				strFromType = (String) ((StringList) htCGInfo.get(DomainObject.SELECT_FROM_TYPE)).get(0);
			
            String strConfigurableFeatureIcon="";						
			if (mxType.isOfParentType( context , strFromType ,ConfigurationConstants.TYPE_CONFIGURATION_FEATURE))
				strConfigurableFeatureIcon = strConfigurationFeatureIcon;
			else
				strConfigurableFeatureIcon = strConfigurationOptionsIcon;
			
		    if (strDVName!= null && !"".equals(strDVName)){
		    if(exportToExcel){
		    	sbBuffer.append(strDVName);
		    }else{
		    	sbBuffer.append(strstart);
		    	sbBuffer = sbBuffer.append("<img src=\"../common/images/")
		    	.append(strConfigurableFeatureIcon)
		    	.append("\" border=\"0\"  align=\"middle\" ")
		    	.append("TITLE=\"")
		    	.append(" ")
		    	.append(i18VariantOption)
		    	.append(" ")
		    	.append("\"")
		    	.append("/>")
		    	.append(strDVName);
		    	sbBuffer.append(strEnd);
		    }
			strDVName = sbBuffer.toString();
		}
		}
        if(strDVName == null)strDVName="";
        vDVNames.add(strDVName) ; 
	}
	return vDVNames ; 
}

/**
 * This method is used to Get Common Group Visual Cue , 
 * It compares the Common Groups for the selected Design Variant & its immediate sub Technical Feature
 * 
 * @param context  - Matrix Context Object
 * @param sLstDVIds - StringList of Design Variant IDs to be removed from the Context Logical Feature 
 * @return bResult - boolean true  - if the operation is  Successful
 * 					 		 false - if the operation Fails
 * @throws FrameworkException
 * @since R212
 * @author IXH
 * @category NON API
 */
public Vector getCommonGroupVisualCueInViewDV(Context context, String[] args) throws Exception
{		
	//XSSOK Deprecated 
	HashMap programMap = (HashMap)JPO.unpackArgs(args);
	MapList objectList =  (MapList)programMap.get("objectList");
	
	String strLogicalFeatureID = ""; 
	String strDVID = ""; 
	String strCtxPdtId = ""; 
	StringTokenizer strTokens = null ; 
	String strRelId = ""  ; 
	
	MapList mapList = new MapList();
	CommonValues cgBean = new CommonValues(); 
	HashMap mpParentCGInfo = new HashMap();
	HashMap mpChildCGInfo = new HashMap();
	Hashtable map = null ; 
	Vector vec = new Vector();

	String strGlobalCG = "" ; 
	String strContext = "";
	HashMap parentCtxmap = new HashMap(objectList.size());

	StringBuffer sbBuffer = new StringBuffer(400);
	String strLanguage = context.getSession().getLanguage();
	String strToolTipNoSubFCG = EnoviaResourceBundle.getProperty(context,SUITE_KEY,"emxConfiguration.ToolTip.CommonGroupVisualCue1",strLanguage);
	String strToolTipCommonSubFCG = EnoviaResourceBundle.getProperty(context,SUITE_KEY,"emxConfiguration.ToolTip.CommonGroupVisualCue2",strLanguage);
	String strToolTipExactSubFCG = EnoviaResourceBundle.getProperty(context,SUITE_KEY,"emxConfiguration.ToolTip.CommonGroupVisualCue3",strLanguage);

	sbBuffer = sbBuffer.append("<img src=\"../common/images/")
                            .append("iconStatusRed.gif")
                            .append("\" border=\"0\"  align=\"middle\" ")
                            .append("TITLE=\"")
                            .append(" ")
                            .append(strToolTipNoSubFCG)
                            .append("\"")
                            .append("/>");

	String strREDVisualCue = sbBuffer.toString();
	sbBuffer.delete(0,sbBuffer.length());

	sbBuffer = sbBuffer.append("<img src=\"../common/images/")
                            .append("iconStatusYellow.gif")
                            .append("\"  border=\"0\"  align=\"middle\" ")
                            .append("TITLE=\"")
                            .append(" ")
                            .append(strToolTipCommonSubFCG)
                            .append("\"")
                            .append("/>");
	String strYELLOWVisualCue = sbBuffer.toString();
	
	sbBuffer.delete(0,sbBuffer.length());
	sbBuffer = sbBuffer.append("<img src=\"../common/images/")
                            .append("iconStatusGreen.gif")
                            .append("\" border=\"0\"  align=\"middle\" ")
                            .append("TITLE=\"")
                            .append(" ")
                            .append(strToolTipExactSubFCG)
                            .append("\"")
                            .append("/>");
	String strGREENVisualCue = sbBuffer.toString();

	for (int iCnt = 0 ; iCnt < objectList.size() ; iCnt++ ){

		map				 = (Hashtable )objectList.get(iCnt);
		strDVID			 = (String)map.get(DomainConstants.SELECT_ID);
		strTokens		 = new StringTokenizer(((String)map.get("id[parent]")),",");
		strLogicalFeatureID = strTokens.nextToken();
		
		if(strTokens.hasMoreTokens()){
			strCtxPdtId		 = strTokens.nextToken();
		}else{
			strCtxPdtId = (String)map.get("contextProductID");
		}

		
		strRelId		 = (String)map.get(DomainRelationship.SELECT_ID) ; 
		strGlobalCG		 = cgBean.getGlobalCommonGroupValue(context , strRelId);
		parentCtxmap.put(strDVID,strGlobalCG);
		mapList			 = cgBean.getCommonGroups(context ,strLogicalFeatureID , strDVID ) ;
		
		if(strGlobalCG.equalsIgnoreCase("No")){
			for (int cnt = 0 ; cnt < mapList.size() ;cnt++ ){
				HashMap mp = (HashMap) mapList.get(cnt);
				strContext = (String)mp.get("Context Prod Id");
				if(strContext.equals(strCtxPdtId)){
					mpParentCGInfo.put( strDVID , mapList );	
				}
			}
		}else{
			mpParentCGInfo.put( strDVID , mapList );
		}
	}

	DomainObject domCtxObject =  new DomainObject(strCtxPdtId);
	String strCtxPdtType = domCtxObject.getInfo(context, DomainConstants.SELECT_TYPE);
	
	if(strCtxPdtType.equals(ConfigurationConstants.TYPE_PRODUCT_VARIANT)){
		StringBuffer sbWhereCondition=new StringBuffer(100);
		sbWhereCondition.append("to[").append(ConfigurationConstants.RELATIONSHIP_PRODUCT_FEATURE_LIST).append("].from.id == \"").append(strCtxPdtId).append("\"");
		//whereCondition = "to["+ConfigurationConstants.RELATIONSHIP_PRODUCT_FEATURE_LIST+"].from.id == \""+strCtxPdtId+"\"";
	}
			
	StringList busSelects = new StringList();
	StringList relSelects = new StringList();

	StringBuffer relPattern = new StringBuffer();
	StringBuffer typePattern = new StringBuffer();

	MapList	mLDesignVariants = new MapList();
	
	StringList slObjSelects = new StringList();

	// adding the rel pattern
	StringBuffer sBufRelPattern = new StringBuffer();
	sBufRelPattern.append(ConfigurationConstants.RELATIONSHIP_LOGICAL_STRUCTURES);
	
	// adding the type pattern
	StringBuffer sBufTypePattern = new StringBuffer();
	sBufTypePattern.append(ConfigurationConstants.TYPE_LOGICAL_STRUCTURES);
	sBufTypePattern.append(",");
	sBufTypePattern.append(ConfigurationConstants.TYPE_PRODUCTS);

	LogicalFeature logicalFeature = new LogicalFeature(strLogicalFeatureID);
	
	MapList subFeaturesMapList = logicalFeature.getLogicalFeatureStructure(context, 
															   sBufTypePattern.toString(), 
															   sBufRelPattern.toString(),
															   slObjSelects,
															   new StringList(), 
															   false,true,
															   (int)0,
															   (int)0, 
															   DomainObject.EMPTY_STRING,
															   DomainObject.EMPTY_STRING, 
															   (short) 0,
															   DomainObject.EMPTY_STRING);
	
	busSelects.clear();
	busSelects.add(DomainConstants.SELECT_ID); 	
	busSelects.add(DomainConstants.SELECT_NAME);
	busSelects.add(DomainConstants.SELECT_TYPE);
	
	relSelects.clear();
	relSelects.add(DomainRelationship.SELECT_ID);
	
	
		if (subFeaturesMapList.size() != 0 )
		{
			for (int iCnt = 0 ; iCnt < subFeaturesMapList.size() ; iCnt++ ){
				
				Hashtable subFeatureMap = (Hashtable)subFeaturesMapList.get(iCnt);
				String strSubLogicalFeatureID  = (String)subFeatureMap.get("id");
				String strType  = (String)subFeatureMap.get("type");
				
				if(!mxType.isOfParentType(context,strType,ConfigurationConstants.TYPE_LOGICAL_STRUCTURES)){
					continue ; 
				}
				
				if (strSubLogicalFeatureID != null && !"".equals(strSubLogicalFeatureID)){
										
					String relWhere = DomainObject.EMPTY_STRING;
					String objWhere = DomainObject.EMPTY_STRING;
					
					StringBuffer relPattern1 = new StringBuffer();
					relPattern1.append(ConfigurationConstants.RELATIONSHIP_VARIES_BY);
					relPattern1.append(",");
					relPattern1.append(ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY);
					
					// Obj and Rel Selects
					StringList objSelects = new StringList();
					relSelects.clear();

					int iLevel = 1;
					String filterExpression = (String) programMap.get("CFFExpressionFilterInput_OID");

					// retrieve Active DV list
					
					LogicalFeature logicalFeat = new LogicalFeature(strSubLogicalFeatureID);
					mLDesignVariants = logicalFeat.getDesignVariants(context, typePattern.toString(),
												   relPattern.toString(), objSelects, relSelects, false, true, iLevel, 0,
												   objWhere, relWhere, DomainObject.FILTER_STR_AND_ITEM,
												   filterExpression);
					
					
					if (mLDesignVariants.size() !=0 ){

						for (int iDVCnt = 0 ; iDVCnt < mLDesignVariants.size() ; iDVCnt++ ){
									
							Hashtable designVariantMap = (Hashtable)mLDesignVariants.get(iDVCnt);
							String strDesignVariantID = (String)designVariantMap.get(DomainConstants.SELECT_ID);
							MapList CGList = cgBean.getCommonGroups(context , strSubLogicalFeatureID , strDesignVariantID );

							if(mpChildCGInfo.size() == 0 ){
								mpChildCGInfo.put(strDesignVariantID , CGList );
							}else {
									if (mpChildCGInfo.containsKey(strDesignVariantID)){
										MapList list = (MapList)mpChildCGInfo.get(strDesignVariantID);
										list.addAll(CGList);
										mpChildCGInfo.remove(strDesignVariantID);
										mpChildCGInfo.put(strDesignVariantID , list );
									}else{
											mpChildCGInfo.put(strDesignVariantID , CGList );
									}
								}
							}
						}
				  }
			   }
			}
			
			MapList mapListParentCGs = new MapList();
			MapList mapListChildCGs = new MapList();
			String strParentCGName = "" ;
			StringList strParentCVals = new StringList() ;
			String strChildCGName = "" ;
			StringList strChildCVals = new StringList() ;
			boolean bMatched = false ; 
			boolean bPartialMatch = false ; 
			int iNoOfMatches = 0 ; 
			int iNoOfPartialMatches= 0 ; 
			String strVCue = "" ; 
			
				for (int iCnt = 0 ; iCnt < objectList.size() ; iCnt++ ){
					Hashtable table		= (Hashtable )objectList.get(iCnt);
					strRelId		 = (String)table.get(DomainRelationship.SELECT_ID) ; 
								
					String strRelType =(String) table.get(DomainRelationship.SELECT_NAME); 
					
					if (!"".equals(strRelType) && strRelType != null && strRelType.equalsIgnoreCase(ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY))
					{
							strVCue = "" ; 
					}else{
						boolean isInvalid = false ; 
						String strCtxName = domCtxObject.getInfo(context , DomainConstants.SELECT_NAME);

						if(!"".equals(strRelType) && strRelType != null && strRelType.equalsIgnoreCase(ConfigurationConstants.RELATIONSHIP_VARIES_BY)){
							StringBuffer sbMqlCommand=new StringBuffer(200);
							sbMqlCommand
							.append("print connection \"")
							.append(strRelId)
							.append(" select tomid[")
							.append(
									ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY)
							.append(".fromrel[")
							.append(
									ConfigurationConstants.RELATIONSHIP_PRODUCT_FEATURE_LIST)
							.append(".from.name dump |");
							//String strMqlCommand = "print connection \""+strRelId+"\" select tomid["+ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY+"].fromrel["+ConfigurationConstants.RELATIONSHIP_PRODUCT_FEATURE_LIST+"].from.name dump |" ;
							String strRelIds  = executeMqlCommand(context , sbMqlCommand.toString() ) ;
							if(strRelIds != null && !"".equals(strRelIds)){
								strTokens = new StringTokenizer(strRelIds,"|");
								String strToken = ""  ;
								while (strTokens.hasMoreTokens())
								{
									strToken = strTokens.nextToken();
									if (strToken  !=  null && !"".equals(strToken) && strToken.equalsIgnoreCase(strCtxName) )
									{
										isInvalid = true ;
									}
								}
							}
						}
					
					if (!isInvalid)
					{
					
					iNoOfMatches = 0 ;
					iNoOfPartialMatches = 0 ; 
					mapListParentCGs =  new MapList();
					mapListChildCGs = new MapList();
					
					MapList mapListParentCGsActual =  new MapList();
					MapList mapListChildCGsActual = new MapList();
					Set parentDVSet = new HashSet();
                    Set childDVSet = new HashSet();
					
					strDVID				= (String)table.get(DomainConstants.SELECT_ID);

					strGlobalCG	=(String) parentCtxmap.get(strDVID);

					if(mpParentCGInfo.size() != 0 && mpParentCGInfo.containsKey(strDVID)){
						mapListParentCGs = (MapList)mpParentCGInfo.get(strDVID);
						
						for(int k=0;k<mapListParentCGs.size();k++)
						{
							Map tempMap = (Map)mapListParentCGs.get(k);
							
							//if(strContextId!=null && !strContextId.equals("")&& !strContextId.equals("null") && strCtxPdtId.equals(strContextId))
									mapListParentCGsActual.add(tempMap);
						}
					}
					
					if(mpChildCGInfo.size() != 0 && mpChildCGInfo.containsKey(strDVID))
                    {
						mapListChildCGs  = (MapList)mpChildCGInfo.get(strDVID);
						
						for(int k=0;k<mapListChildCGs.size();k++)
						{
							Map tempMap = (Map)mapListChildCGs.get(k);
							//if(strContextId!=null && !strContextId.equals("")&& !strContextId.equals("null") && strCtxPdtId.equals(strContextId))
									mapListChildCGsActual.add(tempMap);
						}
					}

					if(mapListChildCGsActual.size() == 0 ){ 
						strVCue = "" ; 
					}else if(mapListParentCGsActual.size()== 0 && mapListChildCGsActual.size() >0){
						strVCue = strREDVisualCue;
					}else {
						
						   for (int count = 0 ; count < mapListParentCGsActual.size() ;count++ ){
							   
								HashMap map1 = (HashMap) mapListParentCGsActual.get(count);
								strParentCGName = (String)map1.get("Common Group Name") ; 
								strParentCVals = (StringList) map1.get("Config Options RelIds") ;
                                parentDVSet.add(strParentCGName);

								for (int count1 = 0 ; count1 < mapListChildCGsActual.size() ;count1++ ){
										bMatched = false ;
										bPartialMatch = false; 
										HashMap map2 = (HashMap) mapListChildCGsActual.get(count1);
										strChildCGName = (String)map2.get("Common Group Name") ; 
										strChildCVals = (StringList) map2.get("Config Options RelIds") ;
                                        childDVSet.add(strChildCGName);

										if(strParentCVals.containsAll(strChildCVals) || strChildCVals.containsAll(strParentCVals)){
											bPartialMatch = true ;
											if(strParentCGName.equals(strChildCGName)){
                                            
												if(strParentCVals.size() == strChildCVals.size()){
													bMatched  = true ;
												}
											 }
										}else if(strParentCGName.equals(strChildCGName)){
											bPartialMatch = true ;
										}
								

										 if(bMatched){
											iNoOfMatches++;
										 }else if(bPartialMatch){
												iNoOfPartialMatches++;
										 }
							    }
							}

							if( iNoOfMatches > 0 &&  iNoOfPartialMatches == 0) {
								if(iNoOfMatches == mapListChildCGsActual.size()
										&& parentDVSet.containsAll(childDVSet) 
                                        && childDVSet.containsAll(parentDVSet) )
									
								{
									strVCue  = strGREENVisualCue;
								}else{
									strVCue  = strYELLOWVisualCue;
								}
							}else if(iNoOfPartialMatches > 0){
								strVCue  = strYELLOWVisualCue;
							}else{
									strVCue  = strREDVisualCue;
							}
						}
					}else{
 						strVCue = "" ; 
					}
				}
						vec.add(strVCue);
			}
			
	return vec ; 
}
/**
* This method is used to get value of attribute 'Global Common Group'
* @param context     the eMatrix <code>Context</code> object
* @param args 		 Contains String array containing all the required parameters 
* @return			 value of attribute 'Global Common Group'
* @exception		 Exception if operation fails
*/
public Vector getGlobalCommonGroupAttribValue(Context context, String[] args) throws Exception
 {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		Hashtable childMap = null;
		String strVariesByRelId = "";
		StringList attributeRange = new StringList(1);
		Vector attributeValuesVec = new Vector();
		StringList attributeDisplayRange = new StringList(1);
		String strGlobalCommonGroupVal = "";

		if (objectList.size() != 0) {
			for (int iCnt = 0; iCnt < objectList.size(); iCnt++) {
				attributeRange = new StringList(1);
				attributeDisplayRange = new StringList(1);
				childMap = (Hashtable) objectList.get(iCnt);
				strVariesByRelId = (String) childMap.get("id[connection]");
				if (strVariesByRelId != null && !"".equals(strVariesByRelId)) {
					CommonValues cgBean = new CommonValues();
					strGlobalCommonGroupVal = cgBean
							.getGlobalCommonGroupAttributeValue(context,
									strVariesByRelId);

					if (strGlobalCommonGroupVal != null
							&& !("".equals(strGlobalCommonGroupVal))) {
						attributeRange.addElement(strGlobalCommonGroupVal);
						attributeDisplayRange = i18nNow
								.getAttrRangeI18NStringList(
										ConfigurationConstants.ATTRIBUTE_GLOBAL_COMMON_GROUP,
										attributeRange, context.getSession()
												.getLanguage());
						attributeValuesVec.add(attributeDisplayRange.get(0));
					} else {
						attributeValuesVec.add("");

					}
				} else {
					attributeValuesVec.add("");
				}
			}
		}
		return attributeValuesVec;
	}

	/**
	 * expand program called on expand of CG context, which will render the CG +
	 * CG's Options
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	@com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getCommonGroupListInSB(Context context, String[] args)
			throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String strLevel = (String) programMap.get("level");
		if (strLevel != null) {
			StringTokenizer strTokens = new StringTokenizer(strLevel, ",");
			if (strTokens.countTokens() >= 4) {
				return new MapList();
			}
		}
		CommonValues cgBean = new CommonValues();
		MapList mapList = new MapList();
		HashMap map = new HashMap();
		String strObjId = (String) programMap.get("objectId");
		String strParentObjId = (String) programMap.get("parentOID");
		String strCGId = "";
		String strCGName = "";
		HashMap ctxCGInfo = null;
		String strRelId = (String) programMap.get("relId");
		String strPdtId = "";

		HashMap PdtCGInfo1 = new HashMap();
		strCGId = "";
		String strCtxId = "";
		if (strObjId.contains("&") || (strObjId.contains("*"))) {
			if (strObjId.contains("*")) {

				StringTokenizer strTokenizer = new StringTokenizer(strObjId,
						"*");
				strPdtId = strTokenizer.nextToken();
				strObjId = strTokenizer.nextToken();
				strRelId = cgBean.getVariesByRelId(context, strParentObjId,
						strObjId);

				String strGlobalCG = cgBean.getGlobalCommonGroupValue(context,
						strRelId);
				MapList mlCtxCGs = cgBean.getCommonGroups(context,
						strParentObjId, strObjId);

				if (strGlobalCG != null && !"".equals(strGlobalCG)
						&& strGlobalCG.equalsIgnoreCase("No")) {
					for (int iCnt = 0; iCnt < mlCtxCGs.size(); iCnt++) {
						PdtCGInfo1 = (HashMap) mlCtxCGs.get(iCnt);
						strCtxId = (String) PdtCGInfo1.get("Context Prod Id");

						if (strCtxId != null && !"".equals(strCtxId)
								&& strCtxId.equals(strPdtId)) {

							map = new HashMap();
							strCGId = (String) PdtCGInfo1
									.get("Common Group Id");
							strCGName = (String) PdtCGInfo1
									.get("Common Group Name");
							map.put("relId", strCGId);
							strCGId = strCGId + "&" + strCGId;
							map.put("id", strCGId);
							map.put("name", strCGName);
							map.put("type", "CommonGroup");
							map.put("level", "2");
							mapList.add(map);

							Vector vecParentCVs = getCommonValuesDataForCG(
									context, strCGId);
							for (int i = 0; i < vecParentCVs.size(); i++) {
								strCGId = (String) ((Map) (vecParentCVs.get(i)))
										.get("Common Group Id");
								if ((StringList) ((Map) (vecParentCVs.get(i)))
										.get("Config Options RelIds") != null) {
									StringList ConfigOptionsRelId = (StringList) ((Map) (vecParentCVs
											.get(i)))
											.get("Config Options RelIds");

									StringList ConfigOptionNames = new StringList();
									if ((StringList) ((Map) (vecParentCVs
											.get(i)))
											.get("Config Options Names") != null) {
										ConfigOptionNames = (StringList) ((Map) (vecParentCVs
												.get(i)))
												.get("Config Options Names");
									}

									for (int i1 = 0; i1 < ConfigOptionsRelId
											.size(); i1++) {
										map = new HashMap();
										map.put("id", ConfigOptionsRelId
												.get(i1));
										map.put("name", ConfigOptionNames
												.get(i1));
										map.put("type", "CommonValues");
										map.put("level", "3");
										mapList.add(map);
									}
								}
							}
						}
					}
				} else {
					for (int m = 0; m < mlCtxCGs.size(); m++) {
						map = new HashMap();
						ctxCGInfo = (HashMap) mlCtxCGs.get(m);

						strCGId = (String) ctxCGInfo.get("Common Group Id");
						strCGName = (String) ctxCGInfo.get("Common Group Name");

						map.put("relId", strCGId);
						strCGId = strCGId + "&" + strCGId;
						map.put("id", strCGId);
						map.put("name", strCGName);
						map.put("type", "CommonGroup");
						map.put("level", "2");
						mapList.add(map);

						Vector vecParentCVs = getCommonValuesDataForCG(context,
								strCGId);
						for (int i = 0; i < vecParentCVs.size(); i++) {
							strCGId = (String) ((Map) (vecParentCVs.get(i)))
									.get("Common Group Id");
							if ((StringList) ((Map) (vecParentCVs.get(i)))
									.get("Config Options RelIds") != null) {
								StringList ConfigOptionsRelId = (StringList) ((Map) (vecParentCVs
										.get(i))).get("Config Options RelIds");

								StringList ConfigOptionNames = new StringList();
								if ((StringList) ((Map) (vecParentCVs.get(i)))
										.get("Config Options Names") != null) {
									ConfigOptionNames = (StringList) ((Map) (vecParentCVs
											.get(i)))
											.get("Config Options Names");
								}

								for (int i1 = 0; i1 < ConfigOptionsRelId.size(); i1++) {
									map = new HashMap();
									map.put("id", ConfigOptionsRelId.get(i1));
									map.put("name", ConfigOptionNames.get(i1));
									map.put("type", "CommonValues");
									map.put("level", "3");
									mapList.add(map);
								}
							}
						}
					}
				}
			}else if (strObjId.contains("&")){

				StringTokenizer strTokenizer = new StringTokenizer(strObjId , "|"); 
				strRelId = strTokenizer.nextToken();
				
				StringTokenizer strTokenizer0 = new StringTokenizer(strRelId , "&"); 
				String strCGRelId = strTokenizer0.nextToken();
				
				//Get the Config Option ID and Name
				Vector vecParentCVs = getCommonValuesDataForCG(context ,strCGRelId);

				for ( int i = 0 ;i < vecParentCVs.size() ;i++ ) { 
					strCGRelId    = (String)((Map)(vecParentCVs.get(i))).get("Common Group Id"); 
					
					if((StringList)((Map)(vecParentCVs.get(i))).get("Config Options RelIds")!=null)
					{
						StringList ConfigOptionNames = new StringList();
						
						StringList ConfigOptionsRelId = (StringList)((Map)(vecParentCVs.get(i))).get("Config Options RelIds");
						if((StringList)((Map)(vecParentCVs.get(i))).get("Config Options Names")!=null){
							ConfigOptionNames = (StringList)((Map)(vecParentCVs.get(i))).get("Config Options Names");
						}
						
						for(int i1=0;i1<ConfigOptionsRelId.size();i1++){
								map = new HashMap();
								map.put("id" ,ConfigOptionsRelId.get(i1));
								map.put("name" ,ConfigOptionNames.get(i1));
								map.put("type" ,"CommonValues");
								mapList.add(map);
					     }
					} 
				}
		}
		}
		return mapList;
	}

	/**
	 * program which will render CG context, which will be root node in common
	 * group list page
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws FrameworkException
	 */
	@com.matrixone.apps.framework.ui.ProgramCallable
	public List getCommonGroupContextInCGListPage(Context context, String[] args)
			throws FrameworkException {
		MapList mapList = new MapList();

		try {
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			CommonValues cgBean = new CommonValues();

			HashMap map = new HashMap();
			String strObjId = (String) programMap.get("objectId");
			
			String strParentObjId = (String) programMap.get("parentOID");
			
			//get Name and type of the parentOID
			DomainObject domObjParent = new DomainObject(strParentObjId);
			StringList slSelect= new StringList(DomainObject.SELECT_TYPE);
			slSelect.addElement(DomainObject.SELECT_NAME);
			slSelect.addElement(DomainObject.SELECT_ID);
			Map mpParentInfo=domObjParent.getInfo(context, slSelect);
			
			String strParentType=(String)mpParentInfo.get(DomainObject.SELECT_TYPE);
			String strParentName=(String)mpParentInfo.get(DomainObject.SELECT_NAME);
			
			
			String strProductId = (String) programMap.get("ProductId");

			String strRelId = (String) programMap.get("relId");
			String strPdtId = "";


			String strGlobalCG = cgBean.getGlobalCommonGroupValue(context,
					strRelId);
			
			if (strGlobalCG.equalsIgnoreCase("No")) {
//				MapList mlCtxCGs = cgBean.getCommonGroups(context,
//						strParentObjId, strObjId);
				LogicalFeature logicalftr = new LogicalFeature(strParentObjId);
				List slContextIds= logicalftr.getLogicalFeatureContextIds(context);

				if (!slContextIds.isEmpty()) {
					String[] cgIDArray = new String[slContextIds.size()];
					MapList cgMapList = DomainObject.getInfo(context,
							(String[]) slContextIds.toArray(cgIDArray),
							slSelect);
					Map mpofCG= null;
					for (int iCnt = 0; iCnt < cgMapList.size(); iCnt++) {
						mpofCG=(Map)cgMapList.get(iCnt);
						strPdtId=(String)mpofCG.get(DomainObject.SELECT_ID);
						String strParentType1=(String)mpofCG.get(DomainObject.SELECT_TYPE);
						String strParentName1=(String)mpofCG.get(DomainObject.SELECT_NAME);
						map = new HashMap();
						String strObjId1 = strPdtId + "*" + strObjId;

						map.put("id", strObjId1);
						map.put("relId", strRelId);
						map.put("name", strParentName1);
						map.put("type", strParentType1);
						map.put("level", "1");
						mapList.add(map);
					}
				} else { // If we are adding CG for 1st time where GCG="No"
					map = new HashMap();
					String strObjId1 = strProductId + "*" + strObjId;
					map.put("id", strObjId1);
					map.put("relId", strRelId);
					map.put("name", strParentName);
					map.put("type", strParentType);
					map.put("level", "1");
					mapList.add(map);
				}
			} else {
				map = new HashMap();
				String strObjId1 = strParentObjId + "*" + strObjId;
				map.put("id", strObjId1);
				map.put("relId", strRelId);
				map.put("name", strParentName);
				map.put("type",strParentType);
				map.put("level", "1");
				mapList.add(map);
			}
		} catch (Exception e) {
			throw new FrameworkException(e.getMessage());
		}
		return mapList;
	}

}//end of class



