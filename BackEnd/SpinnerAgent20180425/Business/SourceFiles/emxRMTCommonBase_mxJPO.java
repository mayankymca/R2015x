/*
** emxRMTCommonBase
**
** Copyright (c) 2007 MatrixOne, Inc.
**
** All Rights Reserved.
** This program contains proprietary and trade secret information of
** MatrixOne, Inc.  Copyright notice is precautionary only and does
** not evidence any actual or intended publication of such program.
**
*/

/*

Change History:
Date       Change By  Release   Bug/Functionality        Details
-----------------------------------------------------------------------------------------------------------------------------
15-Apr-09  kyp        V6R2010   357905                   Added method triggerCheckReservedObject to implement check trigger
                                                         to check RMT object reservation before modifying any object.
21-Fev-13  jx5		  V6R2014   IR-218082V6R2014         Added methods getRequirementIcons and getRelIconProperty to handle
														 the display of Requirement icon depending on object's relationship
19-MAR-13  lx6		  V6R2014   UI enhancement           This delivery consists in UI enhancement on tables and forms

03-MAY-13  jx5		  V6R2014   IR-233755V6R2014		 Take custom Requirement types into account in getRequirementIcons

17-MAY-13  lx6		  V6R2014   IR-234604V6R2014 		 NHIV6R215-039037: Lifecycle states of RMT object should be supported
 														 with different icons. 
06-JUN-13  jx5		  V6R2013	IR-230904V6R2014		 STP: In CATIA, "Open In Table" command working is KO

18-JUl-13  lx6		  V6R2014x	IR-239404V6R2014x		 STP: Incorrect information is being displayed on Lock for Edit  
														 window when other User lock the requirement specification. 
13-MAR-14  zud	djh	 V6R2015x	HL Parameter under Requirement. Modified getAllHTMLSourceFromRTF() and added new functions 
														 
16-MAY-14  qyg        V6R2015x  IR-281776V6R2015x        Add method getRequirementIconsByDirection 

04-JUL-14  jx5		  V6R2015x  RMC Perfo				 Add method importContentTextFromExcel

23-JUL-14  hat1 djh   V6R2015x  Validation column added for Requirements and Test Cases in Structure Display view. Added NextTestExecutionScheduled() and LastCompletedTestExecution().  

13-AUG-14  hat1 zud   V6R2015x  Validation column added for Requirement Specification and Chapter in Structure Display view. Added percentagePassLastTE getTestCaseValidationCount(), getTestCaseValidationCountsDB(), percentagePassLastTE().  

17-SEP-14  hat1 zud   V6R2015x  IR-242335V6R2015             STP: Content in export to excel  requirement garbled in japanese language. 

08-OCT-14  hat1 zud   V6R2015x  IR-331758-3DEXPERIENCER2015x  STP: IE11 - Expand All on Req. Spec. Str. view gives XML Error.

10-OCT-14 ZUD IR-333259-3DEXPERIENCER2015x Parameter value displayed in "Content" cell contains unexpected "null" values 

16-OCT-14  hat1 zud   V6R2015x  IR-326368-3DEXPERIENCER2015x    NHIV6R2015x-45210: Test Case validation status is not accessible at specification structure display page. 

16-OCT-14  hat1 zud   V6R2015x  IR-326341-3DEXPERIENCER2015x  	STP:  Data of content field of custom requirement is displayed in Validation column. Modified percentagePassLastTE(). 
13-JAN-15  KIE1 ZUD   IR-333259-3DEXPERIENCER2015x Parameter value displayed in "Content" cell contains unexpected "null" values


13-JAN-15  KIE1 ZUD   IR-333259-3DEXPERIENCER2015x Parameter value displayed in "Content" cell contains unexpected "null" values
29-JAN-15  QYG       IR-421740-3DEXPERIENCER2015x: Saving RTF data corrupts "Content Data" attribute 
23-May-16  QYG       IR-441507-3DEXPERIENCER2015x Performance Issue in Requirement and Listing Test cases.
09-JUN-17  KIE1 ZUD  IR-517222-3DEXPERIENCER2015x: There is no Select All option in Requirement Specifications Structure Compare page
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import matrix.db.Attribute;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectItr;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Policy;
import matrix.db.Relationship;
import matrix.db.RelationshipWithSelectList;
import matrix.db.State;
import matrix.db.StateList;
import matrix.util.MatrixException;
import matrix.util.StringList;

import org.apache.axis.encoding.Base64;
import org.apache.poi.util.IOUtils;

import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MessageUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.StringUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.framework.lifecycle.LifeCyclePolicyDetails;
import com.matrixone.apps.framework.ui.UIComponent;
import com.matrixone.apps.framework.ui.UIMenu;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UIStructureCompare;
import com.matrixone.apps.framework.ui.UITableCommon;
import com.matrixone.apps.framework.ui.UIToolbar;
import com.matrixone.apps.productline.ProductLineUtil;
import com.matrixone.apps.requirements.RequirementsCommon;
import com.matrixone.apps.requirements.RequirementsConstants;
import com.matrixone.apps.requirements.RequirementsUtil;
import com.matrixone.apps.requirements.convertor.EConvertorSettings;
import com.matrixone.apps.requirements.convertor.EConvertorSettings.DefaultConvertorVersion;
import com.matrixone.apps.requirements.convertor.EConvertorSettings.SubConvertorSet;
import com.matrixone.apps.requirements.convertor.RMTConvertor;
import com.matrixone.apps.requirements.convertor.engine.util.ConvertedDataDecorator;
import com.matrixone.apps.requirements.convertor.engine.util.ImageUtil;
import com.matrixone.apps.requirements.convertor.engine.util.ReferenceDocumentUtil;
import com.matrixone.apps.requirements.ui.UITableRichText;


/**
 * This JPO class has some methods pertaining to the generic RMT usage
 * @author Brian Casto
 * @version RequirementManagement V6R2009x - Copyright (c) 2007, MatrixOne, Inc.
 */
public class emxRMTCommonBase_mxJPO extends emxDomainObject_mxJPO
{

//  Added:15-Apr-08:kyp:R207:RMT Bug 357905
   protected static final String SELECT_RESEVERED = "reserved";
   protected static final String SELECT_RESEVERED_BY = "reservedby";
// End:R207:RMT Bug 357905

   /**
    * Create a new emxRMTCommonBase object.
    *
    * @param context the eMatrix <code>Context</code> object
    * @param args holds no arguments
    * @return a emxRMTCommonBase object.
    * @throws Exception if the operation fails
    * @since RequirementManagement V6R2009x
    * @grade 0
    */
   public emxRMTCommonBase_mxJPO(Context context, String[] args) throws Exception
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
    * @since ProductCentral 10.0.0.0
    * @grade 0
    */
   public int mxMain (Context context, String[] args) throws Exception
   {
      if (!context.isConnected())
      {
         String language = context.getSession().getLanguage();
         String strContentLabel = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.Alert.FeaturesCheckFailed"); 
         throw  new Exception(strContentLabel);
      }
      return(0);
   }

   /**
    * Return the Object message in HTML format
    * @param context the eMatrix <code>Context</code> object
    * @param args JPO arguments
    * @return String HTML output
    * @throws Exception if operation fails
    */
	public String getObjectMessageHTML(Context context, String[] args) throws Exception
    {
        Map info = (Map)JPO.unpackArgs(args);
        info.put("messageType", "html");
        com.matrixone.jdom.Document doc = getObjectMailXML(context, info);
        return (emxSubscriptionUtil_mxJPO.getMessageBody(context, doc, "html"));

    }

    /**
     * Return the Object message in TEXT format
     * @param context the eMatrix <code>Context</code> object
     * @param args JPO arguments
     * @return String HTML output
     * @throws Exception if operation fails
     */
    public String getObjectMessageText(Context context, String[] args) throws Exception
    {
        Map info = (Map)JPO.unpackArgs(args);
        info.put("messageType", "text");
        com.matrixone.jdom.Document doc = getObjectMailXML(context, info);
        return (emxSubscriptionUtil_mxJPO.getMessageBody(context, doc, "text"));

    }

	/**
	 * Return the Object message in XML format
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param info Map information of xml attributes
	 * @return null
	 * @throws Exception if operation fails
	 */
    public static com.matrixone.jdom.Document getObjectMailXML(Context context, Map info) throws Exception
    {
        String baseURL = (String)info.get("baseURL");
        String bundleName = (String)info.get("bundleName");
        String locale = ((Locale)info.get("locale")).toString();
        String messageType = (String)info.get("messageType");
        String objectId = (String)info.get("id");
        String notificationName = (String)info.get("notificationName");

        HashMap eventCmdMap = UIMenu.getCommand(context, notificationName);
        String eventName = UIComponent.getSetting(eventCmdMap, "Event Type");
        String eventKey = "emxRequirements.Event." + eventName.replaceAll(" ", "") + ".Message";
        if(eventKey.equals("emxRequirements.Event.ObjectMajorRevised.Message")){
        	eventKey = "emxRequirements.Event.ObjectRevised.Message";
        }
        String i18NEvent = EnoviaResourceBundle.getProperty(context, bundleName, context.getLocale(), eventKey); 

        DomainObject object = DomainObject.newInstance(context, objectId);
        StringList selectList = new StringList(3);
        selectList.addElement(SELECT_TYPE);
        selectList.addElement(SELECT_NAME);
        selectList.addElement(SELECT_REVISION);
        Map objectInfo = object.getInfo(context, selectList);
        String objectType = (String)objectInfo.get(SELECT_TYPE);
        String i18NobjectType = UINavigatorUtil.getAdminI18NString("type", objectType, locale);
        String objectName = (String)objectInfo.get(SELECT_NAME);
        String objectRev = (String)objectInfo.get(SELECT_REVISION);

		String[] headerValues = new String[4];
		headerValues[0] = i18NobjectType;
		headerValues[1] = objectName;
		headerValues[2] = objectRev;
		headerValues[3] = i18NEvent;
		String header = MessageUtil.getMessage(context, null, "emxRequirements.Event.Mail.Header", headerValues, null, context.getLocale(), bundleName);
		HashMap headerInfo = new HashMap();
        headerInfo.put("header", header);

        ArrayList dataLineInfo = new ArrayList();
		if (messageType.equalsIgnoreCase("html"))
		{
			String[] messageValues = new String[4];
			messageValues[0] = baseURL + "?objectId=" + objectId;
			messageValues[1] = i18NobjectType;
			messageValues[2] = objectName;
			messageValues[3] = objectRev;
			String viewLink = MessageUtil.getMessage(context,null,
													 "emxRequirements.Event.Html.Mail.ViewLink",
													 messageValues,null,
													 context.getLocale(),bundleName);
			dataLineInfo.add(viewLink);

		}
		else
		{
			String[] messageValues = new String[3];
			messageValues[0] = i18NobjectType;
			messageValues[1] = objectName;
			messageValues[2] = objectRev;
			String viewLink = MessageUtil.getMessage(context,null,
													 "emxRequirements.Event.Text.Mail.ViewLink",
													 messageValues,null,
													 context.getLocale(),bundleName);

			dataLineInfo.add(viewLink);
			dataLineInfo.add(baseURL + "?objectId=" + objectId);
		}

		HashMap footerInfo = new HashMap();
        footerInfo.put("dataLines", dataLineInfo);

        return (emxSubscriptionUtil_mxJPO.prepareMailXML(context, headerInfo, null, footerInfo));
    }



	/** Trigger Method to fire a notification on a spec if the structure has been changed
	 * @param context the eMatrix <code>Context</code> object
	 * @param args holds the Hashmap containing the parent id.
	 * @returns void nothing
	 * @throws Exception if the operation fails
	 * @since RequirementsManagement V6R2009x
	 *
	 */
    public void specStructureModified(Context context, String[] args)
        throws Exception
    {
        String strObjectId = args[0];

        try
        {
			DomainObject domObj = DomainObject.newInstance(context,strObjectId);

			List lstSpecTypes = ProductLineUtil.getChildrenTypes(context, RequirementsUtil.getRequirementSpecificationType(context));
			lstSpecTypes.add(RequirementsUtil.getRequirementSpecificationType(context));

			//check if domObj is a spec
			String thisObjType = domObj.getType(context);
			if (lstSpecTypes.contains(thisObjType))
			{
				//${CLASS:emxNotificationUtil}.objectNotification(context, strObjectId, "RMTSpecStructureModifiedEvent", null);
				emxNotificationUtilBase_mxJPO.objectNotification(context, strObjectId, "RMTSpecStructureModifiedEvent", null);
				return;
			}

			String strRelPattern = RequirementsUtil.getSpecStructureRelationship(context);
			StringList lstObjSelects = new StringList(2);
			lstObjSelects.add(SELECT_ID);
			lstObjSelects.add(SELECT_TYPE);
			boolean bGetTo = true;
            boolean bGetFrom = false;
            short sRecursionLevel = -1;

            MapList mapParentObjects = domObj.getRelatedObjects(context, strRelPattern, QUERY_WILDCARD,
                  lstObjSelects, null, bGetTo, bGetFrom, sRecursionLevel, null, null);


			for(int i=0; i<mapParentObjects.size(); i++)
            {
                Map mapT=(Map)mapParentObjects.get(i);
                String strObjType = (String)mapT.get(SELECT_TYPE);

                 if (lstSpecTypes.contains(strObjType))
				 {
					String strObjId = (String)mapT.get(SELECT_ID);
					//${CLASS:emxNotificationUtil}.objectNotification(context, strObjId, "RMTSpecStructureModifiedEvent", null);
					emxNotificationUtilBase_mxJPO.objectNotification(context, strObjId, "RMTSpecStructureModifiedEvent", null);
				 }
            }

        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
            throw new FrameworkException(e.getMessage());
        }

    }




	/**  Method to handle import of Content Data from Excel
	 * @param context - the eMatrix <code>Context</code> object
	 * @param args - holds the Hashmap containing the parent id, attribute name, and attribute value
	 * @returns void nothing
	 * @throws Exception if the operation fails
	 * @since RequirementsManagement V6R2009x
	 */
    public boolean importContentDataFromExcel(Context context, String[] args)
        throws Exception
    {
        String strObjectId = args[0];
        String strContentData = args[1];
		String strContentText = args[2];
		boolean isContentProcessed = false;
        try
        {
        	if(strContentText.equalsIgnoreCase("") && !strContentData.equalsIgnoreCase(""))
        	{
			DomainObject domObj = DomainObject.newInstance(context,strObjectId);

        		domObj.setAttributeValue(context, "Content Text", strContentData);
			//call utility function to take make text ready to be content data
        		//String contentDataValue = UITableRichText.compressAndEncode(strContentData);
        		domObj.setAttributeValue(context, "Content Data", "<html>"+strContentData+"</html>");
        		domObj.setAttributeValue(context, "Content Type", "html");
        		isContentProcessed =  true;
        	}
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
            throw new FrameworkException(e.getMessage());
        }
        return isContentProcessed;

    }
    
    //JX5 : Added for rest web services
    /**  Method to handle import of Content Text from Excel
	 * @param context - the eMatrix <code>Context</code> object
	 * @param args - holds the Hashmap containing the parent id, attribute name, and attribute value
	 * @returns void nothing
	 * @throws Exception if the operation fails
	 * @since RequirementsManagement 3DExperience-R2015x
	 */
    public boolean importContentTextFromExcel(Context context, String[] args) throws Exception{
    	
    	String strObjectId = args[0];
    	String strContentData = args[1];
    	String strContentText = args[2];
    	boolean isContentProcessed = false;
    	
    	try{
    		
    		if(!strContentText.equalsIgnoreCase("") && strContentData.equalsIgnoreCase("")){
    			//If we only have content text, we need to convert it for content data attribute
    			DomainObject obj = DomainObject.newInstance(context,strObjectId);
    			
    			String contentDataValue = UITableRichText.compressAndEncode(strContentText);
    			
    			obj.setAttributeValue(context, "Content Text", strContentText);
    			obj.setAttributeValue(context, "Content Data", contentDataValue);
    			obj.setAttributeValue(context, "Content Type", "rtf.gz.b64");
    			
    			isContentProcessed = true;		
    		}
    			
    	}
    	catch(Exception e){
    		isContentProcessed = false;
    	}
    	
    	return isContentProcessed;
    }

//  Added:15-Apr-08:kyp:R207:RMT Bug 357905
    /**
     * Checks whether current object is reserved, and if reserved then is the context user the reserving user
     *
     * This is configured as check triggers for following event:
     * ChangeName   ChangeOwner ChangePolicy    ChangeVault Checkin Connect Delete  Disconnect  Modify Attribute    Modify Description  ChangeType  RemoveFile
     *
     * These triggers are configured on following types:
     * Requirement Specification, Requirement, Chapter, Comment
     *
     * @param context The Matrix Context object
     * @param args The trigger arguments. Following should be the contents of this array
     *          args[0] : The object id of the object being checked for reservation
     * @return 0: If the object is not reserved or it is reserved by the context user itself
     *         1: If the object is reseved by some other user.
     * @throws MatrixException if operation fails.
     */
	public int triggerCheckReservedObject(Context context, String[] args) throws MatrixException {
        final int ALLOW_MODIFICATION = 0;
        final int DENY_MODIFICATION = 1;
        try {
            // Argument check
            if (context == null) {
                throw new IllegalArgumentException("context");
            }
            if (args == null || args.length < 1) {
                throw new IllegalArgumentException("args");
            }

            //Get error message
            String strLanguage = context.getSession().getLanguage();
            String strErrorMessage = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.ReservedObject.CannotModifyThisObject"); 

            int nReturn = ALLOW_MODIFICATION;
            String strObjectId = args[0];

            // Find reservation information on this object
            StringList slBusSelect = new StringList();
            slBusSelect.add(SELECT_TYPE);
            slBusSelect.add(SELECT_NAME);
            slBusSelect.add(SELECT_REVISION);
            slBusSelect.add(SELECT_RESEVERED);
            slBusSelect.add(SELECT_RESEVERED_BY);

            DomainObject dmoObject = DomainObject.newInstance(context, strObjectId);
            Map mapObjectInfo = dmoObject.getInfo(context, slBusSelect);

            String strType = (String)mapObjectInfo.get(SELECT_TYPE);
            String strName = (String)mapObjectInfo.get(SELECT_NAME);
            String strRevision = (String)mapObjectInfo.get(SELECT_REVISION);
            boolean isReserved = "true".equalsIgnoreCase((String)mapObjectInfo.get(SELECT_RESEVERED));
            String strReservedBy = (String)mapObjectInfo.get(SELECT_RESEVERED_BY);

            //bug 375355: allow User Agent to pass through, for now.
            if (!isReserved || (isReserved && context.getUser().equals(strReservedBy)) || "User Agent".equals(context.getUser())) {
                return ALLOW_MODIFICATION;
            }

            // Process error message to fill in object details and reserving user name
            String strReservingUserName = PersonUtil.getFullName(context, strReservedBy);

            strErrorMessage = FrameworkUtil.findAndReplace(strErrorMessage, "$<type>", strType);
            strErrorMessage = FrameworkUtil.findAndReplace(strErrorMessage, "$<name>", strName);
            strErrorMessage = FrameworkUtil.findAndReplace(strErrorMessage, "$<revision>", strRevision);
            strErrorMessage = FrameworkUtil.findAndReplace(strErrorMessage, "$<username>", strReservingUserName);

            emxContextUtil_mxJPO.mqlError(context, strErrorMessage);

            return DENY_MODIFICATION;
        }
        catch (Exception exp) {
            exp.printStackTrace();
            throw new MatrixException(exp);
        }
    }
//  End:R207:RMT Bug 357905

	/**
	 * Method is to check is Program Central installed or not
	 * @param context the eMatrix <code>Context</code> object
	 * @param args JPO arguments
	 * @throws Exception if operation fails
	 * @return boolean value true or false
	 */
	public static boolean isPRGInstalled(Context context, String[] args) throws Exception
	{
		return FrameworkUtil.isSuiteRegistered(context, "appVersionProgramCentral", false, null, null);
	}

	/**
	 * Method is to check is VPLM installed or not
	 * @param context the eMatrix <code>Context</code> object
	 * @param args JPO arguments
	 * @throws Exception if operation fails
	 * @return boolean value true or false
	 */
	public static boolean isVPMInstalled(Context context, String[] args) throws Exception
	{
		return true;
	}

	/**
	 * Method is to check is Child Requirement is to create or not
	 * @param context the eMatrix <code>Context</code> object
	 * @param args JPO arguments
	 * @throws Exception if operation fails
	 * @return boolean value true or false
	 */
	public static boolean isChildRequirementCreation(Context context, String[] args) throws Exception
	{
		HashMap requestMap = (HashMap) JPO.unpackArgs(args);
		return "true".equalsIgnoreCase((String)requestMap.get("isChildCreation"));
	}

	/**
	 * Method is to get Hidden fields from JSP
	 * @param context the eMatrix <code>Context</code> object
	 * @param args JPO arguments
	 * @throws Exception if operation fails
	 * @return Object in xml format
	 */
	public static Object getHiddenField(Context context, String[] args) throws Exception
	{
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		//String objectId = (String)programMap.get("objectId");
		Map fieldMap = (Map)programMap.get("fieldMap");
		String fieldName = (String)fieldMap.get("name");
		return "<input type=\"hidden\" name=\"" + fieldName + "\" value=\"\" />";
	}

	private static String escape(String text)
	{
		if(text != null){
			text = text.replaceAll("\"", "\\\\\"");
			text = text.replaceAll("\n", "\\\\n");
			return text;
		}else{
			return "";
		}
	}
	/**
	 * Method is used to return default Sub and Derived Requirement of the selected objectId
	 *
	 * @param context  the eMatrix <code>Context</code> object
	 * @param args  JPO arguments
	 * @return Object in xml format
	 * @throws Exception if operation fails
	 */
	public static Object populateDefaultsForSubAndDerived(Context context, String[] args) throws Exception
	{
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		Map requestMap = (Map)programMap.get("requestMap");
		StringBuffer sb = new StringBuffer();

		String emxTableRowId = (String)requestMap.get("emxTableRowId");
		if(emxTableRowId != null){
			sb.append("<script type=\"text/javascript\">\n");
			//sb.append("//<![CDATA[\n"); //this doesn't help the problem with special characters

			sb.append("function selectOption(fieldName, fieldValue)\n");
			sb.append("{\n");
				sb.append("var field = document.getElementsByName(fieldName)[0];\n");
				sb.append("for(var i = 0; i != field.length; i++){\n"); //avoid to use any '<' , '>', or '&' character in the function
					sb.append("if(field.options[i].value == fieldValue){\n");
						sb.append("field.selectedIndex = i;\n");
						sb.append("break;\n");
					sb.append("}\n");
				sb.append("}\n");
			sb.append("}\n");

			String parentReqId = emxTableRowId.split("[|]", -1)[1];
			DomainObject domObj = new DomainObject(parentReqId);
			String sDefault = "";

			Map attrMap = (Map) domObj.getAttributeMap(context);

			sDefault = (String)attrMap.get(RequirementsUtil.getPriorityAttribute(context));
			sb.append("selectOption(\"" + "Priority" + "\", \"" + sDefault + "\");\n");

			sDefault = (String)attrMap.get(RequirementsUtil.getDifficultyAttribute(context));
			sb.append("selectOption(\"" + "Difficulty" + "\", \"" + sDefault + "\");\n");

			sDefault = (String)attrMap.get(RequirementsUtil.getRequirementClassificationAttribute(context));
			sb.append("selectOption(\"" + "Classification" + "\", \"" + sDefault + "\");\n");

			//sDefault = (String)attrMap.get(RequirementsUtil.getSynopsisAttribute(context));
			//sb.append("document.getElementsByName('" + "Synopsis" + "')[0].innerHTML = \"" + sDefault + "\";\n");

			//sDefault = (String)attrMap.get(RequirementsUtil.getNotesAttribute(context));
			//sb.append("document.getElementsByName('" + "Notes" + "')[0].innerHTML = \"" + sDefault + "\";\n");

			//sDefault = (String)attrMap.get(RequirementsUtil.getEstimatedCostAttribute(context));
			//sb.append("document.getElementsByName('" + "Cost" + "')[0].innerHTML = '" + sDefault + "';");

			//sDefault = (String)attrMap.get(RequirementsUtil.getSponsoringCustomerAttribute(context));
			//sb.append("document.getElementsByName('" + "Customer" + "')[0].innerHTML = '" + sDefault + "';");

			sDefault = (String)attrMap.get(RequirementsUtil.getRequirementCategoryAttribute(context));
			sb.append("document.getElementsByName('" + "Requirement Category" + "')[0].value = \"" + 
			XSSUtil.encodeForJavaScript(context, sDefault) + "\";\n");

			sDefault = (String)attrMap.get(RequirementsUtil.getTitleAttribute(context));
			sb.append("document.getElementsByName('" + "Title" + "')[0].value = \"" + 
			XSSUtil.encodeForJavaScript(context, sDefault) + "\";\n");

			//sb.append("//]]>\n");
			sb.append("</script>");

			//sDefault = (String)attrMap.get(RequirementsUtil.getUserRequirementImportanceAttribute(context));
			//sb.append("<input type=\"hidden\" name=\"" + RequirementsUtil.getUserRequirementImportanceAttribute(context) + "\" value=\"" + sDefault + "\" />");

			//sDefault = (String)attrMap.get(RequirementsUtil.getDesignatedUserAttribute(context));
			//sb.append("<input type=\"hidden\" name=\"" + RequirementsUtil.getDesignatedUserAttribute(context) + "\" value=\"" + sDefault + "\" />");
		}
		return sb.toString();
	}

    /**
     *  Get Maplist containing Revisions Info for Id passed In
     *  Used for Revision Summary Page in RMTRevisions command
     *  revision column
     *
     *  @param context the eMatrix <code>Context</code> object
     *  @param args an array of String arguments for this method
     *  @return MapList containing Revisions Info
     *  @throws Exception if the operation fails
     *
     * @since R2012.HF7
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getRevisions(Context context, String[] args) throws Exception
    {
    	 HashMap map = (HashMap) JPO.unpackArgs(args);
         String       objectId = (String) map.get("objectId");
         MapList revisionsList = null;

         StringList busSelects = new StringList(1);
         busSelects.add(DomainObject.SELECT_ID);

		 boolean isMajor = RequirementsCommon.isMajorPolicy(context, objectId);

		 if(isMajor)
		 {
			 // Major
			 revisionsList = getMajorRevisionsInfo(context,busSelects,new StringList(0), objectId);
		 }
		 else
		 {
			 // Minor
			 DomainObject busObj   = DomainObject.newInstance(context, objectId);
			 revisionsList = busObj.getRevisionsInfo(context,busSelects,new StringList(0));
		 }

    	 return revisionsList;
    }

    /**
     * Get the revisions for an object along with select data
     *
     * @param context the eMatrix <code>Context</code> object
     * @param singleValueSelects the eMatrix <code>StringList</code> object that holds the list of selects that return a single value, for example owner
     * @param multiValueSelects the eMatrix <code>StringList</code> object that holds the list of selects that return multiple values, for example revisions[]
     * @return a MapList
     * @throws FrameworkException if the operation fails
     * @since R2012.HF7
     */
    public MapList getMajorRevisionsInfo(Context context, StringList singleValueSelects, StringList multiValueSelects, String objectId)  throws FrameworkException
    {
    	MapList returnMapList = new MapList();

        int i;
        int singleSelectSize = singleValueSelects.size();
        int multiSelectSize = multiValueSelects.size();
        StringList selects = new StringList(1);

        // Add single select to main select list.
        for (i = 0;i < singleSelectSize;i++)
            selects.addElement(singleValueSelects.elementAt(i));

        // Add multiple select to main select list.
        for (i = 0;i < multiSelectSize;i++)
            selects.addElement(multiValueSelects.elementAt(i));

        try
        {

        	BusinessObjectList boList = getMajorRevisionList(context, objectId);
            BusinessObjectItr boItr = new BusinessObjectItr(boList);
            String objOids[] = new String[boList.size()];
            int oidCnt = 0;

            BusinessObject bo;
            while (boItr.next()) {
                bo = boItr.obj();
                objOids[oidCnt++] = bo.getObjectId(context);
            }
            boItr.reset();

            BusinessObjectWithSelectList selectObjList = BusinessObject.getSelectBusinessObjectData(context,objOids, selects);
            BusinessObjectWithSelectItr selectObjItr = new BusinessObjectWithSelectItr(selectObjList);

            // Iterate through select objects and 'plain' objects.
            // select objects have select data, 'plain' objects have TNR.
            BusinessObjectWithSelect selectBO;
            while (selectObjItr.next()) {
                boItr.next();
                selectBO = selectObjItr.obj();
                bo = boItr.obj();

                HashMap objMap = new HashMap();
                // put TNR
                objMap.put(SELECT_TYPE, bo.getTypeName());
                objMap.put(SELECT_NAME, bo.getName());
                objMap.put(SELECT_REVISION, bo.getRevision());
                objMap.put(SELECT_ID, bo.getObjectId());

                // Add single values to Map
                String key, value;
                for (i = 0;i < singleSelectSize;i++) {
                  key = (String)singleValueSelects.elementAt(i);
                  value = selectBO.getSelectData(key);
                  objMap.put(key, value);
                }

                // Add multiple values to Map
                StringList valueList;
                for (i = 0;i < multiSelectSize;i++) {
                  key = (String)multiValueSelects.elementAt(i);
                  valueList = selectBO.getSelectDataList(key);
                  objMap.put(key, valueList);
                }

                // Add object Map to MapList
                returnMapList.add(objMap);
            }
        }
        catch (Exception e)
        {
            throw (new FrameworkException(e));
        }

        return returnMapList;

    }

   /**
    * get list of major revisions.
    * @param context the eMatrix <code>Context</code> object
    * @param objectId object Id
    * @return list of major revision objects
    * @throws Exception if the operation fails
    */
    public BusinessObjectList getMajorRevisionList(Context context, String objectId) throws Exception
    {
    	  BusinessObjectList objects = new BusinessObjectList();
    	  MapList revisionsList = new MapList();
    	  String data = MqlUtil.mqlCommand(context, "PRINT BUS $1 SELECT $2 DUMP $3", objectId, "majorids[].bestsofar.id", "~");
    	  StringList rows = FrameworkUtil.split(data, "~");
    	    int numPairs = rows.size();
    	    for (int i= 0 ; i < numPairs; i++) {
    	        String objId =  (String) rows.get(i);
    	        DomainObject doSourceECR = new DomainObject(objId);
    	        objects.add(doSourceECR);
    	    }
    	  return objects;
    }


    /**
     * Method shows higher revision Icon if a higher major or minor revision of the object exists
     * based on its policy definition
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @return List - returns the program HTML output
     * @throws Exception if the operation fails
     * @since R2012.HF7
     */
     public List getHigherRevisionIcon(Context context, String[] args) throws Exception
     {
     String ICON_TOOLTIP_HIGHER_REVISION_EXISTS = "emxRequirements.Form.Label.HigherRev";
     String RESOURCE_BUNDLE_PRODUCTS_STR = "emxRequirementsStringResource";
     String OBJECT_LIST = "objectList";

     Map programMap = (HashMap) JPO.unpackArgs(args);
     MapList relBusObjPageList = (MapList) programMap.get(OBJECT_LIST);

     int iNumOfObjects = relBusObjPageList.size();
     // The List to be returned
     List lstHigherRevExists= new Vector(iNumOfObjects);
     String arrObjId[] = new String[iNumOfObjects];

     int iCount;
     //Getting the bus ids for objects in the table
     for (iCount = 0; iCount < iNumOfObjects; iCount++) {
         Object obj = relBusObjPageList.get(iCount);
         arrObjId[iCount] = (String)((Map)relBusObjPageList.get(iCount)).get(DomainConstants.SELECT_ID);
     }

     //Reading the tooltip from property file.
     String strTooltipHigherRevExists = EnoviaResourceBundle.getProperty(context, RESOURCE_BUNDLE_PRODUCTS_STR, context.getLocale(), ICON_TOOLTIP_HIGHER_REVISION_EXISTS);
         String strHigherRevisionIconTag= "";
         String strIcon = EnoviaResourceBundle.getProperty(context,
                         "emxComponents.HigherRevisionImage");
     //Iterating through the list of objects to generate the program HTML output for each object in the table
         for (iCount = 0; iCount < iNumOfObjects; iCount++) {
                 if(!isLastRevision(context, arrObjId[iCount])){
                 strHigherRevisionIconTag =
                         "<img src=\"../common/images/"
                             + strIcon
                             + "\" border=\"0\"  align=\"middle\" "
                             + "TITLE=\""
                             + " "
                             + strTooltipHigherRevExists
                             + "\""
                             + "/>";
                 }else{
                 strHigherRevisionIconTag = " ";
                 }
             lstHigherRevExists.add(strHigherRevisionIconTag);
         }
     return lstHigherRevExists;
     }

     
     
     
     /**
      * Method shows higher revision Icon in the object property page if a higher revision of the object exists
      * @param context the eMatrix <code>Context</code> object
      * @return String - returns the program HTML output
      * @throws Exception if the operation fails
      * @since R2012x.HF4
      */
      public String getHigherRevisionIconProperty(Context context, String[] args) throws Exception{

      String ICON_TOOLTIP_HIGHER_REVISION_EXISTS = "emxProduct.Revision.ToolTipHigherRevExists";
      String RESOURCE_BUNDLE_PRODUCTS_STR = "emxProductLineStringResource";
      Map programMap = (HashMap) JPO.unpackArgs(args);
      Map relBusObjPageList = (HashMap) programMap.get("paramMap");
      String strObjectId = (String)relBusObjPageList.get("objectId");

      //String Buffer to display the Higher revision field in Req property page.
      StringBuffer sbHigherRevisionExists = new StringBuffer(100);
      String strHigherRevisionExists = "";

      //Reading the tooltip from property file.
      String strTooltipHigherRevExists = EnoviaResourceBundle.getProperty(context, RESOURCE_BUNDLE_PRODUCTS_STR, context.getLocale(), ICON_TOOLTIP_HIGHER_REVISION_EXISTS);

          String strHigherRevisionIconTag= "";
          DomainObject domObj = DomainObject.newInstance(context, strObjectId);

      // Begin of Add by Enovia MatrixOne for Bug 300775 Date 03/25/2005
      String strNo  = EnoviaResourceBundle.getProperty(context, RESOURCE_BUNDLE_PRODUCTS_STR, context.getLocale(), "emxProduct.Label.No"); 
      String strYes = EnoviaResourceBundle.getProperty(context, RESOURCE_BUNDLE_PRODUCTS_STR, context.getLocale(), "emxProduct.Label.Yes"); 
      // End of Add by Enovia MatrixOne for Bug 300775 Date 03/25/2005

      //To generate the program HTML output for the context object

          if(!isLastRevision(context, strObjectId))
          {

                  strHigherRevisionIconTag =
                      "<a HREF=\"#\" TITLE=\""
                              + " "
                              + strTooltipHigherRevExists
                              + "\">"
                              + emxPLCCommonBase_mxJPO.HIGHER_REVISION_ICON
                              + "</a>";
                  sbHigherRevisionExists.append(strHigherRevisionIconTag);
                  // Modified by Enovia MatrixOne for Bug 300775 Date 03/25/2005
                  sbHigherRevisionExists.append(strYes);
                  strHigherRevisionExists = sbHigherRevisionExists.toString();

          }else{
                  // Modified by Enovia MatrixOne for Bug 300775 Date 03/25/2005
                  sbHigherRevisionExists.append(strNo);
                  strHigherRevisionExists = sbHigherRevisionExists.toString();

               }

       return strHigherRevisionExists;
      }

      /**
      * Check to see if this is the last revision of the object.
      *
      * @param context the eMatrix <code>Context</code> object
      * @return a boolean indicating whether it is true or false
      * @throws FrameworkException if the operation fails
      * @since R2012.HF7
      */
     public boolean isLastRevision(Context context, String objectId)
         throws FrameworkException
     {
    	 boolean isContextPushed = false;
         try
         {
             ContextUtil.pushContext(context);
             isContextPushed = true;
             DomainObject lastRevision = RequirementsCommon.getLastRevision(context, objectId);
             String lastObjectId = lastRevision.getId(context);
             return objectId.equals(lastObjectId) || objectId.equals(lastRevision.getInfo(context, "physicalid"));
         }
         catch (Exception e)
         {
             throw (new FrameworkException(e));
         }
         finally
         {
             if(isContextPushed)
             {
                ContextUtil.popContext(context);
             }
         }
     }
	 
// Start JX5 : IR-218082V6R2014 STP: RMT UI Changes for 3D Experience.
	 /**
    *  This function returns the Icon of RMT Objects
    *
    * @param context  the eMatrix <code>Context</code> object
    * @return         MapList <objectID, IconName>
    */
	public static MapList getRequirementIcons(Context context, String[] args) throws Exception
	{
//long start = System.currentTimeMillis();		
		MapList iconList = new MapList();
		try
		{
			//unpack the incoming arguments
			HashMap inputMap = (HashMap)JPO.unpackArgs(args);
			//get the objectList MapList from the tableData hashMap
			MapList objectList = (MapList) inputMap.get("objectList");
			String bArr[] = new String[objectList.size()];
			String rArr[] = new String[objectList.size()];
			StringList bSel = new StringList();
			RelationshipWithSelectList rwsl = null;
			String IconName = "";
			String relType = "";
			//to handle the case where there is no relationship
			Boolean emptyRelArray=false;
			
			// Creation of businessObjectId & relId Array
			for(int i=0;i<objectList.size();i++)
			{
				bArr[i] = (String)((Map)objectList.get(i)).get(DomainConstants.SELECT_ID);
				rArr[i] = (String)((Map)objectList.get(i)).get(DomainConstants.SELECT_RELATIONSHIP_ID);
				if(rArr[i] == null || rArr[i].equals("null") || rArr[i].equals(""))
				{
					emptyRelArray=true;
				}
				
			}
			
			// Creation of Selection StringList
			bSel.add(DomainConstants.SELECT_TYPE);
			bSel.add(DomainRelationship.SELECT_TYPE);
			
			//retrive bus with select
			// N.B.:It is impossible to have no business object ID
			BusinessObjectWithSelectList bwsl   = BusinessObject.getSelectBusinessObjectData(context,bArr,bSel);
			
			//retrive rel with select
			// N.B.: It is possible that an object has no relationship
			if(!emptyRelArray)
			{
				rwsl		= Relationship.getSelectRelationshipData(context,rArr,bSel);
			}
			
			//Processing all objects from objectList
			for(int i=0; i<objectList.size();i++)
			{
			
				// Retrieve the type of the object
				String busType = bwsl.getElement(i).getSelectData(DomainConstants.SELECT_TYPE);
				
				// Retrieve the type of the relationship
				if(!emptyRelArray)
				{
					relType  = rwsl.getElement(i).getSelectData(DomainRelationship.SELECT_TYPE);
				}
				else
				{
					relType = "null";
				}
				
				
				HashMap Map =  new HashMap();
				Map objectMap 				= (Map)objectList.get(i);
				String currentObjectId 		= (String)objectMap.get(DomainConstants.SELECT_ID);
				Boolean isKindOfRequirement = false;
				if((String)objectMap.get("kindof") != null) {
					isKindOfRequirement = RequirementsUtil.getRequirementType(context).equals(objectMap.get("kindof"));
				}
				else{
					//IR-233755V6R2014
					DomainObject obj			= DomainObject.newInstance(context,currentObjectId);
					isKindOfRequirement = obj.isKindOf(context, "Requirement");
					//
				}
				
				
				//In the case of a Requirement	and its derived types			
				if(isKindOfRequirement && relType!=null && !relType.equals("") &&!relType.equals("null"))
				{									
					IconName = getRelIconProperty(context, relType);
					Map.put(currentObjectId, IconName);	
				}
				else
				{//For all other objects we use the standar method
					IconName = UINavigatorUtil.getTypeIconProperty(context, busType);
					Map.put(currentObjectId, IconName);
				}
				iconList.add(Map);
			}
		}catch (Exception ex) {
            System.out.println(" Error while getting custom RMT icons : " + ex.toString());
        }	
//System.out.println("###icon data: " + (System.currentTimeMillis() - start));
		return iconList;
	}
	/**
	 * returns icons for source requirements column in traceability report
	 * @param context the eMatrix <code>Context</code> object
	 * @param args
	 * @return  MapList <objectID, IconName>
	 * @throws Exception
	 */
    public static MapList getSourceRequirementIconsByDirection(Context context, String[] args) throws Exception
    {
        return getRequirementIconsByDirection(context, args, true);
    }
    
    /**
     * returns icons for target requirements column in traceability report
     * @param context
     * @param args
     * @return  MapList <objectID, IconName>
     * @throws Exception
     */
    public static MapList getTargetRequirementIconsByDirection(Context context, String[] args) throws Exception
    {
        return getRequirementIconsByDirection(context, args, false);
    }
	
    /**
     * returns icons for requirements column in traceability report
     * @param context
     * @param args
     * @param isSourceObject whether it's source requirement column or not
     * @return MapList <objectID, IconName>
     * @throws Exception
     */
    protected static MapList getRequirementIconsByDirection(Context context, String[] args , boolean isSourceObject) throws Exception
    {
        MapList iconList = new MapList();
        try
        {
            //unpack the incoming arguments
            HashMap inputMap = (HashMap)JPO.unpackArgs(args);
            //get the objectList MapList from the tableData hashMap
            MapList objectList = (MapList) inputMap.get("objectList");
            String bArr[] = new String[objectList.size()];
            String rArr[] = new String[objectList.size()];
            StringList bSel = new StringList();
            RelationshipWithSelectList rwsl = null;
            String IconName = "";
            String relType = "";
            
            // Creation of businessObjectId & relId Array
            for(int i=0;i<objectList.size();i++)
            {
                bArr[i] = (String)((Map)objectList.get(i)).get(DomainConstants.SELECT_ID);
                rArr[i] = (String)((Map)objectList.get(i)).get(DomainConstants.SELECT_RELATIONSHIP_ID);
                
            }
            
            // Creation of Selection StringList
            bSel.add(DomainConstants.SELECT_TYPE);
            bSel.add(DomainRelationship.SELECT_TYPE);
            
            //retrive bus with select
            // N.B.:It is impossible to have no business object ID
            BusinessObjectWithSelectList bwsl   = BusinessObject.getSelectBusinessObjectData(context,bArr,bSel);
            
            //retrive rel with select
            // N.B.: It is possible that an object has no relationship
            rwsl        = Relationship.getSelectRelationshipData(context,rArr,bSel);
            
            //Processing all objects from objectList
            for(int i=0; i<objectList.size();i++)
            {
                
            
                // Retrieve the type of the object
                String busType = bwsl.getElement(i).getSelectData(DomainConstants.SELECT_TYPE);
                
                // Retrieve the type of the relationship
                relType  = rwsl.getElement(i).getSelectData(DomainRelationship.SELECT_TYPE);
                
                HashMap Map =  new HashMap();
                Map objectMap               = (Map)objectList.get(i);
                String currentObjectId      = (String)objectMap.get(DomainConstants.SELECT_ID);
                String direction            = (String)objectMap.get("direction");
                //
                boolean isFromTarget = "<--".equals(direction);
                //assume all objects in the list are Requirements
                if (!isFromTarget && isSourceObject || isFromTarget && !isSourceObject)
                {
                    IconName = UINavigatorUtil.getTypeIconProperty(context, busType);
                }
                else
                {
                    IconName = getRelIconProperty(context, relType);
                }
                
                Map.put(currentObjectId, IconName);
                iconList.add(Map);
            }
        }catch (Exception ex) {
            System.out.println(" Error while getting custom RMT icons : " + ex.toString());
        }   
        return iconList;
    }
    	/**
    *  This function gets the Icon file name for any given relationship
    *  from the emxRequirements.properties file
    *
    * @param context  the eMatrix <code>Context</code> object
    * @param rel     object relationship name
    * @return         String - icon name
    */
    public static String getRelIconProperty(Context context, String rel) throws Exception
    {
        String icon = "";
        String relRegistered = "";

        try {
            if (rel != null && rel.length() > 0 )
            {
                String propertyKey = "";
                String propertyKeyPrefix = "emxRequirements.SmallIcon.";
				String defaultPropertyKey = "emxRequirements.SmallIcon.relationship_SpecificationStructure";//"emxFramework.smallIcon.defaultType";

                // Get the symbolic name for the relationship passed in
                relRegistered = FrameworkUtil.getAliasForAdmin(context, "relationship", rel, true);
			
                if (relRegistered != null && relRegistered.length() > 0 )
                {
                    propertyKey = propertyKeyPrefix + relRegistered.trim();

                    try {
                        //icon = EnoviaResourceBundle.getProperty(context, propertyKey);
                    	icon = EnoviaResourceBundle.getProperty(context,"emxRequirements",context.getLocale(),propertyKey);
                    	//EnoviaResourceBundle.getProperty
                    } catch (Exception e1) {
                        icon = "";
                    }
                    if( icon == null || icon.length() == 0 ||icon.equalsIgnoreCase(propertyKey))
                    {
                        // If no icons found, return a default icon for propery file.
                        icon = EnoviaResourceBundle.getProperty(context, defaultPropertyKey);
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println(" Error getting relationship icon name : " + ex.toString());
        }

        return icon;
    }
	
// End JX5
//START LX6    
    /**
     * Display the lifeCycle in form.
     *
     * @param context the eMatrix <code>Context</code> object
     * @return a string which represent the url of the lifeCycle Dialog 
     * @throws Exception if the operation fails
     * @since R2014
     */
    public static String fieldLifecycle(Context context, String[] args) throws Exception {

    	HashMap programMap = (HashMap) JPO.unpackArgs(args);
    	HashMap paramMap = (HashMap) programMap.get("paramMap");
    	HashMap fieldMap = (HashMap) programMap.get("fieldMap");
    	HashMap settingsMap = (HashMap) fieldMap.get("settings");

    	String sHeight = (String) settingsMap.get("height");
    	if (null == sHeight || "".equals(sHeight)) {
    		sHeight = "60";
    	}
    	String sOID = (String) paramMap.get("objectId");

    	String sResult = "<object id='gnvLifecycle' type='text/html'";
    	sResult += "data='../common/emxLifecycleDialog.jsp?export=false&toolbar=AEFLifecycleMenuToolBar&objectId=" + sOID + "&header=emxFramework.Lifecycle.LifeCyclePageHeading&mode=basic'";
    	sResult += "width='100%' height='" + sHeight + "' style='overflow:none;padding:0px;margin:0px'></object>";

    	return sResult;

    }

    /**
     * Check if the form is on Edit Mode
     *
     * @param context the eMatrix <code>Context</code> object
     * @return boolean to display or not the lifeCyle 
     * @throws Exception if the operation fails
     * @since R2014
     */
    public static Boolean checkViewMode(Context context, String[] args) throws Exception {

    	Boolean bViewMode = new Boolean(true);
    	HashMap requestMap = (HashMap) JPO.unpackArgs(args);
    	bViewMode = true;

    	try {
    		String sMode = (String) requestMap.get("mode");
    		if (sMode.equals("edit")) {
    			bViewMode = false;
    		}
    		if (sMode.equals("")) {
    			sMode = (String) requestMap.get("editLink");
    			if (!sMode.equals("")) {
    				bViewMode = true;
    			}
    		}
    	} catch (Exception e) {
    	}

    	return bViewMode;
    } 
    
    /**
     * get the appropriate icon for the lock field
     *
     * @param context the eMatrix <code>Context</code> object
     * @return a string which represent the html code for an Image
     * @throws Exception if the operation fails
     * @since R2014
     */
    public static String getLockIcon(Context context, String[] args) throws Exception
    {
    	Map programMap = (HashMap) JPO.unpackArgs(args);
	    Map paramMap = (HashMap)programMap.get("paramMap");
	    String strObjectId = (String)paramMap.get("objectId");
	    StringList selectStmts = new StringList("reserved");
	    selectStmts.addElement("reservedby");
	    selectStmts.addElement("reservedcomment");
	    selectStmts.addElement("reservedstart");
	    DomainObject domObj = DomainObject.newInstance(context, strObjectId);
	    Map ReservedInfo = domObj.getInfo(context,selectStmts);
	    String reserved = (String)ReservedInfo.get("reserved");
	    String strImage="";
	    String strDifficultyIconTag = "";
	    String User = context.getUser();
	    if(reserved.equalsIgnoreCase("true"))
	    {
	    	String reservedby = (String)ReservedInfo.get("reservedby");
	    	if(User.equalsIgnoreCase(reservedby))
	        {
	    		strImage= EnoviaResourceBundle.getProperty(context,"emxRequirements.Icon.padLockReservedByMySelf");
	        }
        	else
        	{
        		strImage= EnoviaResourceBundle.getProperty(context,"emxRequirements.Icon.padLockReservedByOther");
        	}    	
	    	String strLockedBy = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxFramework.Basic.ReservedBy");
	    	String toolTip = strLockedBy + " " +reservedby+ " \n";
	    	strDifficultyIconTag = 
	        	"<img src=\"" + strImage + "\""
	            + " border=\"0\"  align=\"middle\" "
	            + "title=\""
	            + " "
	            + toolTip
	            + "\""
	            + "/>";
	    }
	    else
	    { 

	    	strDifficultyIconTag = "";
	    }
        return strDifficultyIconTag;
    }
    

    /**
     * get the column html data for an icon column or styled column
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @return a List of string which represent the column html data
     * @throws Exception if the operation fails
     * @since R2014
     */
    public List getColumnHTML(Context context, String[] args) throws Exception {
        String RESOURCE_BUNDLE_PRODUCTS_STR = "emxRequirementsStringResource";
        String OBJECT_LIST = "objectList";
        Map programMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) programMap.get(OBJECT_LIST);
        
        HashMap paramList = (HashMap) programMap.get("paramList");
        String strExport = (String) paramList.get("exportFormat");
        boolean toExport = false;
        if (strExport != null) 
                toExport = true;
        
        int iNumOfObjects = relBusObjPageList.size();
        String arrObjId[] = new String[iNumOfObjects];
        List columnTags = new Vector(iNumOfObjects);
        int iCount;
        // Getting the bus ids for objects in the table
        for (iCount = 0; iCount < iNumOfObjects; iCount++) {
            arrObjId[iCount] = (String) ((Map) relBusObjPageList.get(iCount)).get(DomainConstants.SELECT_ID);
        }
        
        HashMap columnMap = (HashMap) programMap.get("columnMap");
        Map Settings = (Map)columnMap.get("settings");
        boolean useStyle = "true".equalsIgnoreCase((String)Settings.get("Use Style"));
        String select = (String) (columnMap.containsKey("expression_businessobject")?
                columnMap.get("expression_businessobject"):
                columnMap.get("expression_relationship"));
        StringList selects = new StringList();
        selects.addElement(select);   

        MapList columnData = DomainObject.getInfo(context, arrObjId, selects);
        String attributeName = select;
        int attrind = select.indexOf("attribute[");
        if(attrind >=0)
        {
        	attributeName = select.substring(attrind + 10, select.indexOf("]", attrind));
        	attributeName = attributeName.replaceAll(" ", "_");
        }
        
        // Iterating through the list of objects to generate the program HTML
        // output for each object in the table
        String denied = EnoviaResourceBundle.getProperty(context, RequirementsConstants.BUNDLE_FRAMEWORK , context.getLocale(), "emxFramework.Basic.DENIED");
        for (iCount = 0; iCount < iNumOfObjects; iCount++) {
            Map cell = (Map)columnData.get(iCount);
            String strValue = (String)cell.get(select);
            String strHTMLTag = "";
            if (strValue != null && strValue.length() > 0) {
                if(RequirementsConstants.DENIED.equalsIgnoreCase(strValue)){
                    strHTMLTag = denied;
                }else{
                    
                    String i18nProperty = "emxFramework.Range." + attributeName + "." + strValue;
                    String strDisplayValue = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource",
                            context.getLocale(), i18nProperty);
                    if(toExport){
                    	strHTMLTag = strDisplayValue;
                    }
                    else{
                    	if(useStyle){
                            String propertyKey = "emxRequirements.TextStyle." + attributeName + "." + strValue;
                            String Style = EnoviaResourceBundle.getProperty(context, propertyKey);
                            strHTMLTag = "<span style=\"" + Style + "\">" + strDisplayValue + "</span>"; 
                    	}
                    	else{
    		                String propertyKey = "emxRequirements.Icon." + attributeName + "." + strValue;
    		                String strIcon = EnoviaResourceBundle.getProperty(context, propertyKey);
    		                strHTMLTag = "<img src=\"" + strIcon + "\"" + "  border=\"0\"  align=\"middle\" " + "title=\""
    		                        + strDisplayValue + "\"" + "/>" + "<span style=\"display:none\">" + strDisplayValue + "</span>";
                    	}
                    }
                }
            }
            columnTags.add(strHTMLTag);
        }
        return columnTags;
    }
    
    /**
     * get the filed html data for a styled form field
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @return a string which represent the field html data
     * @throws Exception if the operation fails
     * @since R2014
     */
    public String getFieldHTML(Context context, String[] args) throws Exception
    {
	    Map programMap = (HashMap) JPO.unpackArgs(args);
	    Map paramMap = (HashMap)programMap.get("paramMap");
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        String strMode =  (String) requestMap.get("mode");
    	String strObjectId =  (String)paramMap.get("objectId");
        HashMap fieldMap = (HashMap) programMap.get("fieldMap");
        Map Settings = (Map)fieldMap.get("settings");
        boolean useStyle = "true".equalsIgnoreCase((String)Settings.get("Use Style"));
        String select = (String)fieldMap.get("expression_businessobject");
        if(select == null){
        	select = (String)fieldMap.get("expression_relationship");
        }

        DomainObject domObj = DomainObject.newInstance(context, strObjectId);
        String strValue = domObj.getInfo(context, select.trim());
    	
        String attributeName = select;
        int attrind = select.indexOf("attribute[");
        if(attrind >=0)
        {
        	attributeName = select.substring(attrind + 10, select.indexOf("]", attrind));
        	attributeName = attributeName.replaceAll(" ", "_");
        }
        String i18nProperty = "emxFramework.Range." + attributeName + "." + strValue;
        String strDisplayValue = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource",
                context.getLocale(), i18nProperty);
    	if(strMode != null && strMode.equalsIgnoreCase("Edit"))
    	{
    		//
    	}
    	else
    	{
            
            if(useStyle){
                String propertyKey = "emxRequirements.TextStyle." + attributeName + "." + strValue;
	    		String strStyle = EnoviaResourceBundle.getProperty(context, propertyKey);
	    		strDisplayValue = "<span style=\""+strStyle+"\">"+strDisplayValue+"</span>";
            }
    	}
        return strDisplayValue;
	   
    }
    
    /**
     * get the range value for a Program or ProgramHTMLOutput form field
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @return a Map which represent the internal and translated range values
     * @throws Exception if the operation fails
     * @since R2014
     */
    public Object getFieldRangeValues(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        HashMap fieldMap = (HashMap) programMap.get("fieldMap");
        String select = (String)fieldMap.get("expression_businessobject");
        if(select == null){
        	select = (String)fieldMap.get("expression_relationship");
        }
        String attributeName = select;
        int attrind = select.indexOf("attribute[");
        if(attrind >=0)
        {
        	attributeName = select.substring(attrind + 10, select.indexOf("]", attrind));
        }

        StringList fieldRangeValues = new StringList();
        StringList fieldRangeValuesDisplay = new StringList();
        StringList Ranges = RequirementsCommon.getAttributeRange(context, attributeName);
    	attributeName = attributeName.replaceAll(" ", "_");
        Iterator itr = Ranges.iterator();
        while (itr.hasNext()) {
            String rangeValue = (String) itr.next();
            String i18nProperty = "emxFramework.Range." + attributeName + "." + rangeValue;
            String strDisplayValue = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource",
                    context.getLocale(), i18nProperty);

            fieldRangeValues.addElement(rangeValue);
            fieldRangeValuesDisplay.addElement(strDisplayValue);
        }
        HashMap<String, StringList> mapRangeValues = new HashMap<String, StringList>();

        mapRangeValues.put("field_choices", fieldRangeValues);
        mapRangeValues.put("field_display_choices", fieldRangeValuesDisplay);
        return mapRangeValues;
    }

    
    /**
     * Update the object attribute value for a Program or ProgramHTMLOutput column cell. 
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @return 0 for success
     * @throws Exception if the operation fails
     * @since R2014
     */
    public int updateProgramObjectAttribute(Context context, String[] args) throws Exception
    {
    	return updateProgramAttribute(context, args, true, true);
    }

    /**
     * Update the object attribute value for a Program or ProgramHTMLOutput form field. 
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @return 0 for success
     * @throws Exception if the operation fails
     * @since R2014
     */
    public int updateProgramObjectField(Context context, String[] args) throws Exception
    {
    	return updateProgramAttribute(context, args, true, false);
    }

    /**
     * Update the relationship attribute value for a Program or ProgramHTMLOutput column cell. 
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @return 0 for success
     * @throws Exception if the operation fails
     * @since R2014
     */
    public int updateProgramRelAttribute(Context context, String[] args) throws Exception
    {
    	return updateProgramAttribute(context, args, false, true);
    }
    
    /**
     * Update the attribute value for a Program or ProgramHTMLOutput column cell or form field. 
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @param args isBus true if the attribute is for business object; false for relationship
     * @param args isColumn true if it's for a column cell; false for a form field
     * @return 0 for success
     * @throws Exception if the operation fails
     * @since R2014
     */
    public int updateProgramAttribute(Context context, String[] args, boolean isBus, boolean isColumn) throws Exception
    {
	    Map programMap = (HashMap) JPO.unpackArgs(args);
	    Map paramMap = (HashMap)programMap.get("paramMap");
        HashMap columnMap = (HashMap) programMap.get(isColumn ? "columnMap" : "fieldMap");
        //boolean boColumn = columnMap.containsKey("expression_businessobject"); //bps bug
        String select = (String)columnMap.get("expression_businessobject");
        if(select == null){
        	select = (String)columnMap.get("expression_relationship");
        }

        String attributeName = select;
        int attrind = select.indexOf("attribute[");
        if(attrind >=0)
        {
        	attributeName = select.substring(attrind + 10, select.indexOf("]", attrind));
        }

	    String newValue = (String)paramMap.get("New Value");
	    if(isBus){
	        String strObjectId = (String)paramMap.get("objectId");
	        DomainObject domObj = DomainObject.newInstance(context, strObjectId);
	        domObj.setAttributeValue(context, attributeName, newValue);
	    }else{
			String strRelId = (String)paramMap.get("relId");
	    	DomainRelationship.newInstance(context, strRelId).setAttributeValue(context, attributeName, newValue);
	    }
	   
	    return 0;
    }
    

    /**
     * Check if the Object has a next state or not
     *
     * @param context the eMatrix <code>Context</code> object
     * @return a boolean 
     * @throws Exception if the operation fails
     * @since R2014
     */
    public boolean hasNextState(Context context, String[] args) throws Exception 
    {

    	HashMap programMap = (HashMap) JPO.unpackArgs(args);	 
    	String objectId =  (String)programMap.get("objectId");	
    	boolean isEnabled = false;
    	if(objectId != null && objectId.length() > 0)
    	{
    		DomainObject busObject = new DomainObject(objectId);
    		String policyName = busObject.getPolicy(context).getName();
    		StateList statesList  = LifeCyclePolicyDetails.getStateList(context, busObject, policyName);
    		String currentState = busObject.getInfo(context, DomainObject.SELECT_CURRENT);
    		int lastIndx=(statesList.size()) - 1;
    		isEnabled =  !((State)statesList.get(lastIndx)).isCurrent();
    	}
    	return isEnabled;   	

    }
    
    
    
    /**
     * Check if the Object has a previous state or not
     *
     * @param context the eMatrix <code>Context</code> object
     * @return a boolean 
     * @throws Exception if the operation fails
     * @since R2014
     */
    public boolean hasPreviousState(Context context, String[] args) throws Exception 
    {
	   	 HashMap programMap = (HashMap) JPO.unpackArgs(args);	 
	   	 String objectId =  (String)programMap.get("objectId");	 
	   	boolean isEnabled = false;
        if(objectId != null && objectId.length() > 0)
        {
        	DomainObject busObject = new DomainObject(objectId);
    		String policyName = busObject.getPolicy(context).getName();
    		StateList statesList  = LifeCyclePolicyDetails.getStateList(context, busObject, policyName);
    		String currentState = busObject.getInfo(context, DomainObject.SELECT_CURRENT);
    		isEnabled = !((State)statesList.get(0)).isCurrent();
       }
        return isEnabled;   	
    }
    
    /**
     * Check if the Policy field has to be displayed or hidden
     *
     * @param context the eMatrix <code>Context</code> object
     * @return a boolean 
     * @throws Exception if the operation fails
     * @since R2014
     */
    public static boolean showPolicyField(Context context, String[] args) throws Exception {
    	String isSimplified = EnoviaResourceBundle.getProperty(context, "emxRequirements.Preferences.Creation.isSimplifiedCreationForm");
    	Boolean displayField;
    	if(isSimplified == null || "".equals(isSimplified)|| "false".equals(isSimplified))
    	{
    		displayField = true;
    	}
    	else
    	{
    		displayField = false;
    	}
    	return displayField;
    	
    }
    
    private String getSequenceOrderList(Context context, String oid,String level, MapList ObjectlistMap) throws MatrixException
    {
    	int iCount;
    	int iNumOfObjects = ObjectlistMap.size();
    	String seqOrder = "";
    	for (iCount = 0; iCount < iNumOfObjects; iCount++) {
	        Object obj = ObjectlistMap.get(iCount);
	        String ObjectId = (String)((Map)ObjectlistMap.get(iCount)).get(DomainConstants.SELECT_ID);
	        String Objectlevel = (String)((Map)ObjectlistMap.get(iCount)).get("level");
	        if(Objectlevel==null)
	        {
	        	String relId = (String)((Map)ObjectlistMap.get(iCount)).get("id[connection]");
	        	Relationship rel = new DomainRelationship(relId);
	        	Objectlevel = String.valueOf(rel.getLevel());
	        }
	        if(Objectlevel.equalsIgnoreCase("0"))
	        {
	        	seqOrder = ""; 
	        }
	        else if(oid.equalsIgnoreCase(ObjectId)&&level.equalsIgnoreCase(Objectlevel))
	        {
	        	String previousLevel = Integer.toString((Integer.parseInt(Objectlevel)-1));
	        	String parentId = (String)((Map)ObjectlistMap.get(iCount)).get("from.id[connection]");
	        	seqOrder = (String)((Map)ObjectlistMap.get(iCount)).get("attribute[Sequence Order]");
	        	if(seqOrder!=null)
	        	{
	        		seqOrder = seqOrder+".";
	        		seqOrder = getSequenceOrderList(context, parentId,previousLevel, ObjectlistMap) + seqOrder;
	        	}
	        	else
	        	{
	        		String relId = (String)((Map)ObjectlistMap.get(iCount)).get("id[connection]");
	        		if(relId!=null&&relId.length()>0)
	        		{
		        		Relationship rel = new DomainRelationship(relId) ;
		        		Attribute seqAttrib = rel.getAttributeValues(context, RequirementsUtil.getSequenceOrderAttribute(context));
		        		seqOrder = seqAttrib.getValue();
	        		}
	        		else
	        		{
	        			seqOrder = "";
	        		}
	        	}
	        }
	        
	    }
    	return seqOrder;
    }
        
    public List getHigherAndActualRevisionIcon(Context context, String[] args) throws Exception
    {
   	 String ICON_TOOLTIP_HIGHER_REVISION_EXISTS = "emxRequirements.Form.Label.HigherRev";
        String RESOURCE_BUNDLE_PRODUCTS_STR = "emxRequirementsStringResource";
        String OBJECT_LIST = "objectList";
        String PARAM_LIST = "paramList";

        Map programMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) programMap.get(OBJECT_LIST);
        
        HashMap paramList = (HashMap) programMap.get(PARAM_LIST);
        String exportFormat = (String)paramList.get("exportFormat");
        int iNumOfObjects = relBusObjPageList.size();
        // The List to be returned
        List lstHigherRevExists= new Vector(iNumOfObjects);
        String arrObjId[] = new String[iNumOfObjects];

        int iCount;
        //Getting the bus ids for objects in the table
        for (iCount = 0; iCount < iNumOfObjects; iCount++) {
            Object obj = relBusObjPageList.get(iCount);
            arrObjId[iCount] = (String)((Map)relBusObjPageList.get(iCount)).get(DomainConstants.SELECT_ID);
        }

        StringList selects = new StringList();
        selects.addElement("majorid");
        selects.addElement("majorid.lastmajorid");
        selects.addElement(DomainConstants.SELECT_REVISION);
        MapList revData = DomainObject.getInfo(context, arrObjId, selects);
        
        //Reading the tooltip from property file.
        String strTooltipHigherRevExists = EnoviaResourceBundle.getProperty(context, RESOURCE_BUNDLE_PRODUCTS_STR, context.getLocale(), ICON_TOOLTIP_HIGHER_REVISION_EXISTS);
        String strIcon = EnoviaResourceBundle.getProperty(context,
                        "emxComponents.HigherRevisionImage");
        String strHigherRevisionIconTag =
      		 	 "&#160;"
      			 +"<img src=\"../common/images/"
                   + strIcon
                   + "\" border=\"0\"  align=\"baseline\" "
                   + "TITLE=\""
                   + " "
                   + strTooltipHigherRevExists
                   + "\""
                   + "/>";
        //Iterating through the list of objects to generate the program HTML output for each object in the table
            for (iCount = 0; iCount < iNumOfObjects; iCount++) {
            	Map m = (Map)revData.get(iCount);
           	 	String Revision = (String)m.get(DomainConstants.SELECT_REVISION);
           	 	String id = (String)m.get("majorid");
                    if(!id.equals(m.get("majorid.lastmajorid")) && exportFormat==null){
                    	lstHigherRevExists.add(Revision + strHigherRevisionIconTag);
                    }else{
                    	lstHigherRevExists.add(Revision);
                    }
            }
        return lstHigherRevExists;
    }
    public static HashMap getCustomCommands(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        String strlanguage = (String)requestMap.get("languageStr");
        
        HashMap hmpDynMenu = (HashMap)JPO.invoke(context, "emxCustomTableDynamicMenu", null, "getCustomCommands",
        		args, HashMap.class);
        
        MapList dynamicCmdOnMenu = UIToolbar.getChildren(hmpDynMenu);
        
    	HashMap SettingMap = new HashMap();
    	SettingMap.put("Image", "");
    	SettingMap.put("Registered Suite","Requirements");
    	
    	HashMap CheckedSettingMap = new HashMap();
    	CheckedSettingMap.put("Image", "../common/images/iconActionChecked.gif");
    	CheckedSettingMap.put("Registered Suite","Requirements");
       

        HashMap separatorMap = new HashMap();
        HashMap separatorSettingMap = new HashMap();
        separatorSettingMap.put("Registered Suite", "Framework");
        separatorSettingMap.put("Action Type", "Separator");
        separatorMap.put("type", "command");
        separatorMap.put("Name", "AEFSeparator");
        separatorMap.put("label", "AEFSeparator");
        separatorMap.put("description", "Use as separator for toolbar buttons");
        separatorMap.put("alt", "Separator");
        separatorMap.put("settings", separatorSettingMap);
        dynamicCmdOnMenu.add(separatorMap);
    	
		boolean tabularView = true; //UITableRichText.TABULAR_STYLE.equals(UITableRichText.getSCEDefaultViewStyle(session, context));
		String indentation = (String)requestMap.get("indent");
		if(indentation != null){
			tabularView = "tabular".equalsIgnoreCase(indentation);
		}

		String strCmdLabel = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", new Locale(strlanguage), "emxRequirements.SCE.Settings.Label.Tabular");
    	HashMap indentMap = new HashMap();
		indentMap.put("type", "command");
		indentMap.put("label", strCmdLabel);
		indentMap.put("description", "Indentations");
		indentMap.put("href", "javascript:indentTo('tabular')");
		//if(tabularView){
		//	indentMap.put("settings",CheckedSettingMap);
		//}else{
			indentMap.put("settings",SettingMap);
		//}
		indentMap.put("roles",new StringList("all"));
		dynamicCmdOnMenu.add(indentMap);
		
		for(int i = 0; i<50; i+=40){ 
			indentMap = new HashMap();
			indentMap.put("type", "command");
			indentMap.put("label", i + "px");
			indentMap.put("description", "Indentations");
			indentMap.put("href", "javascript:indentTo('"+ i +"')");
			indentMap.put("settings",SettingMap);
			indentMap.put("roles",new StringList("all"));
			
			dynamicCmdOnMenu.add(indentMap);
		}
		
		
		if(!tabularView){
			dynamicCmdOnMenu.add(separatorMap);
			strCmdLabel = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", new Locale(strlanguage), "emxRequirements.SCE.Label.NoAttributes");
			indentMap = new HashMap();
			indentMap.put("type", "command");
			indentMap.put("label", strCmdLabel);
			indentMap.put("description", "simple view");
			indentMap.put("href", "javascript:showSimpleView()");
			indentMap.put("settings",SettingMap);
			indentMap.put("roles",new StringList("all"));
			
			dynamicCmdOnMenu.add(indentMap);
		}

		return hmpDynMenu;
        

    }
    
    //END LX6    
    // ZUD Parameter Under Req Support
    /**
     * Returns the decorated HTML to be displayed in content cell of Parameter Node of Structure Browser
     * @param context - the eMatrix <code>Context</code> object
     * @param args - the inputMap who contains the table definition
     * @return a String containing HTML 
     */

    public String getParameterContentValue(Context context, String[] args ) throws Exception
    {
    	//String objectID = (String) JPO.unpackArgs(args);
    	// ++ HAT1 : ZUD IR-242335V6R2015 fix
    	String strObjectIDtoExport = (String) JPO.unpackArgs(args);
    	String objectID = "";
    	String toExport = "";
    	if(strObjectIDtoExport.contains("|"))
    	{
    		String[] objectIDtoExport = strObjectIDtoExport.split("[|]", 2);
    	    objectID            = objectIDtoExport[0];
    	    toExport            = objectIDtoExport[1];
    	}
    	else
    	{
    		objectID = strObjectIDtoExport;
    	}
    	// -- HAT1 : ZUD IR-242335V6R2015 fix
    	
    	String strParameterText = "";
       
    	// Retrieve Parameter Minimum Value
    	MapList paramMapList = new MapList();
    	Map<String, String> ParamMap = new HashMap<String, String>();
    	ParamMap.put("id", objectID);

    	paramMapList.add(ParamMap);

    	HashMap argsParamValue = new HashMap();
        argsParamValue.put("objectList", paramMapList);
    	StringList ParameterReturn = new StringList();
    	
    	strParameterText +=" "; 
    	//ParameterReturn= (StringList)${CLASS:emxParameterEdit}.getParameterMinValue(context, JPO.packArgs(argsParamValue));
    	ParameterReturn = (StringList) JPO.invoke(context,
	            "emxParameterEdit", null, "getParameterMinValue", JPO.packArgs(argsParamValue),
	            StringList.class);
    	strParameterText += (String) ParameterReturn.firstElement();
    	    	
    	// Retrieve Parameter Value
    	// ++ HAT1 : ZUD IR-242335V6R2015 fix
    	if(toExport.equalsIgnoreCase("true"))
    	{
        	strParameterText += " < ";
    	}
    	else
    	{
    	strParameterText += " &lt; ";
    	}
    	// -- HAT1 : ZUD IR-242335V6R2015 fix
    	
    	ParameterReturn.removeAllElements();
		
    	ParameterReturn = (StringList) JPO.invoke(context,
	            "emxParameterEdit", null, "getParameterValue", JPO.packArgs(argsParamValue),
	            StringList.class);
    	strParameterText += (String) ParameterReturn.firstElement();
    	    	
    	// Retrieve PArameter Maximum Value
    	// ++ HAT1 : ZUD IR-242335V6R2015 fix
    	if(toExport.equalsIgnoreCase("true"))
    	{
        	strParameterText += " < ";
    	}
    	else
    	{
    	strParameterText += " &lt; ";
    	}
    	// -- HAT1 : ZUD IR-242335V6R2015 fix
    	
    	ParameterReturn.removeAllElements();

    	ParameterReturn = (StringList) JPO.invoke(context,
	            "emxParameterEdit", null, "getParameterMaxValue", JPO.packArgs(argsParamValue),
	            StringList.class);
    	strParameterText += (String) ParameterReturn.firstElement(); 
    	    	
    	// ++ HAT1 : ZUD IR-242335V6R2015 fix
    	if(toExport.equalsIgnoreCase("true"))
    	{
    		return strParameterText;
    	}
    	// -- HAT1 : ZUD IR-242335V6R2015 fix
    	
		String decoratedParamHTML =  getParameterDivDecorator(context,objectID,strParameterText);
		return decoratedParamHTML;		
    	
    }
   
    /**
     * This function checks if the parameter values in the table are correct (min, max and value with respective units)
     * @param context - the eMatrix <code>Context</code> object
     * @param args - the inputMap which contains object id and parameter values
     * @return a String containing error message. If check is OK returns null
     */
    public String CheckParameterContentValue(Context context, String[] args ) throws Exception
    {
      //To check the values, all the values are submitted to the database.
      //    If the values are incorrect, database returns Map containing Action (Stop) and Message (Error Message)
      //    If the values are correct, database returns Map containing Action (Continue) and Message (Empty)
      //    In case of error we display the message
      //    In case of success, we revert back to the original values and return. We do it as actual value change will happen when user clicks Save button
    	try
      {
    		String[] Param_val = (String[]) JPO.unpackArgs(args);
    		String objectID = Param_val[0];
    		String strParameterText = "";
        String retType = "";
    		
        //get object id to retrieve actual param values from database
    		MapList paramMapList = new MapList();
    		Map<String, String> ParamMap = new HashMap<String, String>();
    		ParamMap.put("id", objectID);
    		paramMapList.add(ParamMap);
    		HashMap argsParamValue = new HashMap();
    		argsParamValue.put("objectList", paramMapList);
    		StringList ParameterReturn = new StringList();
        
        // Retrieve Parameter minimum value
    		strParameterText +=" ";
    		ParameterReturn = (StringList) JPO.invoke(context,
		            "emxParameterEdit", null, "getParameterMinValue", JPO.packArgs(argsParamValue),
		            StringList.class);
    		
    		strParameterText += (String) ParameterReturn.firstElement();
    		
    		// Retrieve Parameter value
    		strParameterText += "<";
    		ParameterReturn.removeAllElements();
    		
    		ParameterReturn = (StringList) JPO.invoke(context,
    		            "emxParameterEdit", null, "getParameterValue", JPO.packArgs(argsParamValue),
    		            StringList.class);
    		 
    	
    		strParameterText += (String) ParameterReturn.firstElement();
    		
    		// Retrieve Parameter Maximum value
    		strParameterText += "<";
    		ParameterReturn.removeAllElements();
    		ParameterReturn = (StringList) JPO.invoke(context,
		            "emxParameterEdit", null, "getParameterMaxValue", JPO.packArgs(argsParamValue),
		            StringList.class);
    		
    		strParameterText += (String) ParameterReturn.firstElement();
    		
        //try to set value in database
    		retType = setParameterString(context,args);
    		if("OK".equalsIgnoreCase(retType))
    		{
    			HashMap ProgramCheckMap = new HashMap();
    			argsParamValue.remove("objectList");
    			argsParamValue.put("ObjectList", paramMapList);
    			ProgramCheckMap.put("tableData",argsParamValue);
    			
    			HashMap  ReturnError = (HashMap ) JPO.invoke(context,
    		            "emxParameterEdit", null, "updateTableValues", JPO.packArgs(ProgramCheckMap),
    		            HashMap.class);
    			
          //Work done. Now reset original values
    			String [] Param_values ={objectID, strParameterText};
    			setParameterString(context,JPO.packArgs(Param_values));
    			
    			if("STOP".equalsIgnoreCase((String)ReturnError.get("Action")))
          {
    				retType = (String)ReturnError.get("Message");
          }
    			else
          {
    				retType = null; //success
          }
    		}
        else
        {
    		  String [] Param_values ={objectID, strParameterText};
    		  setParameterString(context,JPO.packArgs(Param_values));
        }

         return retType;
    	}
    	catch(Exception ex)
    	{
    		String error = ex.toString();
    		return error;
    	}
   }
    /**
     * Sets the parameter Values edited by user.
     * @param context - the eMatrix <code>Context</code> object
     * @param args - the inputMap who contains the table definition
     * @return int value for Sucsess or failure 
     */
    public String setParameterString(Context context, String[] args ) throws Exception
    { 
    	try
    	{
    		String[] Param_val = (String[]) JPO.unpackArgs(args);
    	
    		// Gets the value of content cell and splits it to get Max, Min and actual Param values.
    		// Trims spaces from Parameter values.
    		String[] Param_items = Param_val[1].split("[<]");
    		if(Param_items.length< 2 )   
    			return "Invalid Parameter Values";
        
    		Param_items[0] = Param_items[0].trim();
    		String[] ParamMinValue = Param_items[0].split("[ ]");
    		Param_items[1] = Param_items[1].trim();
    		String[] ParamValue = Param_items[1].split("[ ]");
    		Param_items[2] = Param_items[2].trim();
    		String[] ParamMaxValue = Param_items[2].split("[ ]");
    
    		String prmUnit = "";
    
    		
    		HashMap ProgramMap =  new HashMap<String, HashMap>();
    		HashMap ParamMap   =  new HashMap<String, String>();
    		HashMap ReuestMap  =  new HashMap<String, String[]>();             
  
    		// Sets Parameter Value
    		int retType = 0;
    		ParamMap.put("objectId", Param_val[0]);
    		// Don't try to Set Parameter Values if they are NULL
			//Fix For IR-333259-3DEXPERIENCER2015x
    		if(ParamValue[0].equals("") || ParamValue[0].length() > 0)
    		{
    			if(ParamValue.length >1)
    				ParamMap.put("New Value",ParamValue[0]+" "+ParamValue[1]);
    			else
    		               ParamMap.put("New Value",ParamValue[0]);
        
    			String[] ParamValueUnit ={prmUnit};
    		        ReuestMap.put("ValueUnit",ParamValueUnit);
        
    		        ProgramMap.put("paramMap",ParamMap);
    		        ProgramMap.put("requestMap",ReuestMap);
        
    			retType = (int) JPO.invoke(context,
    	            "emxParameterEdit", null, "setParameterValue", JPO.packArgs(ProgramMap));
    		}
        
    		// Sets Parameter Minimum Value
			//Fix For IR-333259-3DEXPERIENCER2015x
    		if(ParamMinValue[0].equals("") || ParamMinValue[0].length() > 0)
    		{
    		ParamMap.remove("New Value");
    			
    			if(ParamMinValue.length >1)
    			      ParamMap.put("New Value",ParamMinValue[0]+" "+ParamMinValue[1]);
    			else
    		              ParamMap.put("New Value",ParamMinValue[0]);
        
    			String[] ParamMinUnit ={prmUnit};
    		        ReuestMap.put("MinUnit",ParamMinUnit);
        
    		        ProgramMap.remove("paramMap");
    		        ProgramMap.remove("requestMap");
    	                ProgramMap.put("paramMap",ParamMap);
        
    		        ProgramMap.put("requestMap",ReuestMap);  				
    			
    		        retType = (int) JPO.invoke(context,
    	                    "emxParameterEdit", null, "setParameterValueMin", JPO.packArgs(ProgramMap));
    			}
        
    		// Sets Parameter MAximum Value
			//Fix For IR-333259-3DEXPERIENCER2015x
    			if(ParamMaxValue[0].equals("") || ParamMaxValue[0].length() > 0)
    			{
    				
    				ParamMap.remove("New Value");    			
    				if(ParamMaxValue.length >1)
    					ParamMap.put("New Value",ParamMaxValue[0]+" "+ParamMaxValue[1]);
    				else    	
    		                        ParamMap.put("New Value",ParamMaxValue[0]);
        
    				String[] ParamMAxUnit ={prmUnit};
    		                ReuestMap.put("MaxUnit",ParamMAxUnit);
                        
    		                ProgramMap.remove("paramMap");
    		                ProgramMap.remove("requestMap");
    		                ProgramMap.put("paramMap",ParamMap);
    		                ProgramMap.put("requestMap",ReuestMap);
        
    		                retType = (int) JPO.invoke(context,
    	                      "emxParameterEdit", null, "setParameterValueMax", JPO.packArgs(ProgramMap));
    			}
             
    		return "OK";
    	}
    	catch(Exception ex)
    	{ 
    		String error = ex.toString();
    		return error;
    	}
    	
    }
    
    /**
     * Modifies Parameter values to HTML which is then displayed in Content cell of Structure Browser
     * @param context - the eMatrix <code>Context</code> object
     * @param objectID - to be included in HTML
     * @param Value - Parameter Content cell value
     * @param  ParameterReturn
     * @return String  containing HTML 
     */
    public String getParameterDivDecorator(Context context, String objectID , String Value) throws Exception
    {
    	String DivDecoratedText = "";
    	String objectIdForDiv = "objectInfo_"+objectID.replace(".", "");
    	DivDecoratedText = "<div style=\"display:none;\" id= \""+ objectIdForDiv + "\">";
    	DivDecoratedText += "<div id='objectID'>" + objectID + "</div>";
    	DivDecoratedText+="<div id='objectType'>PlmParameter</div>";
    	DivDecoratedText+= "<div id='convertorVersion'>None</div>";
    	String Table_ID = "Table_"+objectID.replace(".", "");
    	
    	DomainObject dmoObj = DomainObject.newInstance(context, objectID);
        String strParameterTitile = dmoObj.getAttributeValue(context, "Title");
            	DivDecoratedText += "<div title='Parameter Edit View : " + strParameterTitile +"| PlmParameter' style='display:none;' id= 'ParameterEditor_" + objectID.replace(".", "") + "'> " ;
    
    	DivDecoratedText += "</div>";
    	DivDecoratedText +="<div id='Values_"+ objectID.replace(".", "") +"'>"+Value+"</div>";
    	DivDecoratedText += "</div>";
    	
    	// ++ Fix For IR-333259-3DEXPERIENCER2015x
    	String displayValue = "";
    	if(Value.contains("TRUE")) // For Boolean TRUE display null
    		displayValue = "<b>TRUE</b>";
    	else if(Value.contains("FALSE"))// For Boolean FALSE dnt display
    		displayValue = "";
    	else if(Value.contains("null")) // For All null values make cell blank
    	{
    		Value = Value.replaceAll("null", "...");
    		String[] splitValue = Value.split("&lt;");
    		
    		if(!splitValue[0].trim().equals("..."))
    		{
    			displayValue = splitValue[0] +"&lt;";
    		}else{
    			displayValue += " ";
    		}
    		
    		displayValue +="<b>"+splitValue[1]+"</b>";
    		
    		if(!splitValue[2].trim().equals("..."))
    		{
    			displayValue += " &lt;" +splitValue[2] ;
    		}else{
    			displayValue += " ";
    		}
    		
    		if(displayValue.equals(" ... "))
    			displayValue = "";
    	}else{
    		
    		String[] splitValue = Value.split("&lt;");
    		displayValue +=splitValue[0]+" < ";
    		displayValue +="<b>"+splitValue[1]+"</b>";
    		displayValue +=" < "+splitValue[2];
    	}
    	// -- Fix For IR-333259-3DEXPERIENCER2015x
    	DivDecoratedText +="<div id='contentCell_"+ objectID.replace(".", "") +"' class='cke_contents cke_reset'>"+displayValue+"</div>";
		return DivDecoratedText;
    	
    }
    /**
     * Returns the place holder to show the HTML from RTF
     * @param context - the eMatrix <code>Context</code> object
     * @param args - the inputMap who contains the table definition
     * @return a vector containaing the place holder
     * @throws Exception
     */
    public Vector getAllHTMLSourceFromRTF(Context context, String[] args) throws Exception {

        Map inputMap = (Map) JPO.unpackArgs(args);
        Vector returnVector = new Vector();

        MapList objectMap = (MapList) inputMap.get("objectList");
        HashMap paramList = (HashMap) inputMap.get("paramList");
        HashMap columnMap = (HashMap) inputMap.get("columnMap");
        HashMap columnMapSettings = (HashMap) columnMap.get("settings");
        
        String strExport = (String) paramList.get("exportFormat");
        boolean toExport = false;
        if (strExport != null) 
            toExport = true;
        
        boolean isEditable = true; // FIXME columnMapSettings.get("Editable").equals("true");
        boolean isSCE = (paramList.get("isFromSCE") != null && ((String) paramList.get("isFromSCE")).equals("true")) ? true : false;
        boolean isFromDloatingDiv = (paramList.get("fromFloatingDiv") != null && ((String) paramList.get("fromFloatingDiv")).equals("true")) ? true : false;
        
        String nameKey = null;
        nameKey = isFromDloatingDiv==true?"target.name":"name";
        String idKey = null;
        idKey = isFromDloatingDiv==true?"target.id":"id";
        
        updateTimestamp(context, objectMap);
        
        HashMap<String, String> returnMap = new HashMap<String, String>(objectMap.size());
        Iterator objectItr = objectMap.iterator();

        String denied = EnoviaResourceBundle.getProperty(context, RequirementsConstants.BUNDLE_FRAMEWORK , context.getLocale(), "emxFramework.Basic.DENIED");
        String reqSpecType = RequirementsUtil.getRequirementSpecificationType(context),
        		parameterType = RequirementsUtil.getPARParameterType(context),
        		chapterType = RequirementsUtil.getChapterType(context);
        MapList returnList = new MapList();
        while (objectItr.hasNext()) {
            try {
                // Get the information about the current row
                Map<String, String> curObjectMap = (Map) objectItr.next();
                String objectName = (String) curObjectMap.get(nameKey);
                String objectID = (String) curObjectMap.get(idKey);
                
                String kind = (String) curObjectMap.get("kindof");
   
                if(kind == null) {
                    DomainObject dmoObj = DomainObject.newInstance(context, objectID);
                	kind = dmoObj.getInfo(context, "type.kindof");
                	if("DOCUMENTS".equals(kind)) {
                		kind = RequirementsUtil.getRequirementSpecificationType(context);
                	}
                }
                
                // Special handler for Requirement Specification, we want to throw an event for the RichText
                if (reqSpecType.equals(kind)) {
                    returnVector.add(toExport != true ? "<div style='text-align:center;'>" +
                            "<img style='display: none; margin-left: auto; margin-right: auto;' "
                            + "src='images/loading.gif' /> - </div>" : " "); 
                    continue;
                }
                
                // We can process the RichText only if we have a Requirement or a Comment or a Parameter or a Test Case
                if (chapterType.equals(kind)) 
                {
                    returnVector.add(toExport != true ? "<div style='text-align:center;'> - </div>" : " "); 
                    continue;
                }

                String readAccess = (String)curObjectMap.get(RequirementsConstants.SELECT_READ_ACCESS);
                if(RequirementsConstants.DENIED.equals(readAccess)){
                    returnVector.add(toExport != true ? "<div style='text-align:center;'>" + denied + "</div>" : " "); 
                    continue;
                }

                // We get the timeStamp
                Long timeStamp;
                try {
                    timeStamp = eMatrixDateFormat.getJavaDate(curObjectMap.get(DomainConstants.SELECT_MODIFIED)).getTime();
                } catch (Exception ex) {
                	timeStamp = new Random().nextLong();
                }
                
                // If we are in the SCE, we use a different place holder
                if (isSCE) {
                    StringBuffer stringBuffer = new StringBuffer();
                    renderRichtextField(context, timeStamp.toString(), objectID, "", stringBuffer, isEditable);
                    returnVector.add(stringBuffer.toString());
                    continue;
                }
                boolean isParameter = parameterType.equals(kind);
                if(toExport)
                {
                	// ++ HAT1 : ZUD IR-242335V6R2015 fix                    
                	if(isParameter)
                	{
                    	// ++ HAT1 : ZUD IR-242335V6R2015 fix                    
                    	String objectIDtoExport = objectID + "|" + "true";
     		            // HAT1 : ZUD IR-331758-3DEXPERIENCER2015x fix                                       
                    	String parameterContentColumn = getParameterContentValue(context, JPO.packArgs(objectIDtoExport));
                    	// -- HAT1 : ZUD IR-242335V6R2015 fix                    
                        returnVector.add(parameterContentColumn);
                	}
                	else
                	{
                        DomainObject dmoObj = DomainObject.newInstance(context, objectID);
                        String strContentText = dmoObj.getAttributeValue(context, "Content Text");
                		returnVector.add(strContentText);
                	}
                	// -- HAT1 : ZUD IR-242335V6R2015 fix                    
                }
                else 
                {
                	if(isParameter){
                        //following code is used to get the parent Requirement of current parameter. In case we want to display a table for all the parameters together
                		String strParentId = (String)curObjectMap.get("$PID");
                    	returnVector.add("<img style='display: block; margin-left: auto; margin-right: auto;' "
                                + "src='images/loading.gif' onload='getParameterContent(this, \"" + strParentId + "\", \"" + objectID + "\", \"" + 
                                    timeStamp + "\", \"" + DefaultConvertorVersion.VERSION_ONE.toString() + "\")' />");
                	}
                	else{
                        returnVector.add("<img style='display: block; margin-left: auto; margin-right: auto;' "
                                + "src='images/loading.gif' class='richTextPlaceHolder' data-objectId='" + objectID + "'"
                                + " data-timeStamp='" + timeStamp + "'" + " data-convertor='" + 
                                DefaultConvertorVersion.VERSION_ONE.toString() + "' />");
                	}
                }
                
            } catch (Exception ex) {
                String strError = "<img style='display: block; margin-left: auto; margin-right: auto;' "
                        + "alt='Error' src='data:image;base64," + ImageUtil.DEFAULT_PICTURE_ERROR + "' />";
                returnVector.add(toExport != true ? strError : " ");
                continue;
            }
        }
        return returnVector;
    }
    
    private void updateTimestamp(Context context, MapList objectList) throws Exception
    {
    	if(objectList.size() == 0) return;
        int iCount;
        String[] arrObjId = new String[objectList.size()];
        // Getting the bus ids for objects in the table
        for (iCount = 0; iCount < objectList.size(); iCount++) {
            arrObjId[iCount] = (String) ((Map) objectList.get(iCount)).get(DomainConstants.SELECT_ID);
        }
        StringList selects = new StringList();
        selects.addElement(DomainConstants.SELECT_MODIFIED);
        MapList columnData = DomainObject.getInfo(context, arrObjId, selects);
        for (iCount = 0; iCount < objectList.size(); iCount++) {
        	String t = (String)((Map)columnData.get(iCount)).get(DomainConstants.SELECT_MODIFIED);
        	((Map) objectList.get(iCount)).put(DomainConstants.SELECT_MODIFIED, t);
        }
    }

    /**
     * Returns the place holder to show the HTML from RTF
     * @param context - the eMatrix <code>Context</code> object
     * @param args - the inputMap who contains the table definition for Validation Column under Requirements & Requirements Specification.
     * @return a vector containaing the place holder
     * @throws Exception
     */ 
 
public Vector getValidationColumnRTF(Context context, String[] args) throws Exception 
{
	final String validationStatusKey = "attribute[" + RequirementsUtil.getValidationStatusAttribute(context) + "]";

        Map inputMap = (Map) JPO.unpackArgs(args);
        Vector returnVector = new Vector();

        MapList objectMapList = (MapList) inputMap.get("objectList");
        HashMap paramList = (HashMap) inputMap.get("paramList");
        HashMap columnMap = (HashMap) inputMap.get("columnMap");
        HashMap columnMapSettings = (HashMap) columnMap.get("settings");
        
        String strExport = (String) paramList.get("exportFormat");
        boolean toExport = false;
        if (strExport != null) 
            toExport = true;                

        boolean isEditable = true; // FIXME columnMapSettings.get("Editable").equals("true");
        boolean isSCE = (paramList.get("isFromSCE") != null && ((String) paramList.get("isFromSCE")).equals("true")) ? true : false;
        
        if(objectMapList.size() == 0){
        	return returnVector;
        }
        
        String denied = EnoviaResourceBundle.getProperty(context, RequirementsConstants.BUNDLE_FRAMEWORK , context.getLocale(), "emxFramework.Basic.DENIED");
        String REQ_TC_NOTPLAYED = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.ReqValidationColumn.ToolTip.TestCaseNotPlayed"); 
        String REQ_TC_PARTIALLYPASSED = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.ReqValidationColumn.ToolTip.PartiallyPassed"); 
        String REQ_TC_FAILED = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.ReqValidationColumn.ToolTip.ValidationFailed"); 
        String REQ_TC_PASSED = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.ReqValidationColumn.ToolTip.ValidationPassed");
        String TC_TE_PASSED         = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.TestCaseValidationColumn.ToolTip.Passed"); 
        String TC_TE_FAILED         = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.TestCaseValidationColumn.ToolTip.Failed"); 
        String TC_NEXT_TE_SCHEDULED = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.TestCaseValidationColumn.ToolTip.NextTEScheduled");
        String TC_NO_TE_SCHEDULED   = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.TestCaseValidationColumn.ToolTip.NoTEScheduled"); 
        String TC_NO_TE_REPLAYED    = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.TestCaseValidationColumn.ToolTip.NoTEReplayed"); 
    	String REQ_NO_TC_ASSOCIATED = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.ReqValidationColumn.ToolTip.NoTCAssociated"); 
        String[] NLS = new String[]{REQ_TC_NOTPLAYED, REQ_TC_PARTIALLYPASSED, REQ_TC_FAILED, REQ_TC_PASSED, REQ_NO_TC_ASSOCIATED };
        
        final String relIdKey = "to[" + RequirementsUtil.getTestExecutionTestCaseRelationship(context) +  "].id";
        final String idKey = "to[" + RequirementsUtil.getTestExecutionTestCaseRelationship(context) +  "].from.id";
        final String percentageKey = "to[" + RequirementsUtil.getTestExecutionTestCaseRelationship(context) +  "].from.attribute[" + 
				RequirementsUtil.getPercentagePassedAttribute(context) + "]";
        final String actualEndDateKey = "to[" + RequirementsUtil.getTestExecutionTestCaseRelationship(context) +  "].from.attribute[" + 
				RequirementsUtil.getActualEndDateAttribute(context) + "]";
        final String estStartDateKey = "to[" + RequirementsUtil.getTestExecutionTestCaseRelationship(context) +  "].from.attribute[" + 
					RequirementsUtil.getEstimatedStartDateAttribute(context) + "]";
        
        final String percentageKey2 = "attribute[" + RequirementsUtil.getPercentagePassedAttribute(context) + "]";
        final String actualEndDateKey2 = "attribute[" + RequirementsUtil.getActualEndDateAttribute(context) + "]";
        final String estStartDateKey2 = "attribute[" + RequirementsUtil.getEstimatedStartDateAttribute(context) + "]";

        boolean doExpand = true;

    	String expandFrom = null;
		Map o =  (Map)objectMapList.get(0);
		String relId = (String)o.get(DomainConstants.SELECT_RELATIONSHIP_ID);
		if(relId == null || relId.length() == 0) {
			expandFrom = (String)o.get(DomainConstants.SELECT_ID);
		}
		else{
			expandFrom =(String)((StringList)DomainRelationship.newInstance(context, relId).getRelationshipData(context, new StringList("from.id")).get("from.id")).get(0);
		}
		
        Map<String, Map[]> TestCaseTestExecMap = new HashMap<>();
        Map<String, Float> TestCasePercentageMap = new HashMap<>();
        Map<String, Integer> TestCaseTECountMap = new HashMap<>();
    	Map<String, int[]> countsMap = new HashMap<>();
    	Map<String, List[]> namesMap = new HashMap<>();
        
    	DomainObject expandFromObj = DomainObject.newInstance(context, expandFrom);
    	
    	if(relId == null && objectMapList.size() == 1 && !toExport){ //could be a structure with just the root, or a separate call for root of one level expand
    		String kind = expandFromObj.getInfo(context, "type.kindof");
    		if("DOCUMENTS".equals(kind) || kind.equals(RequirementsUtil.getChapterType(context)) ) {
        		doExpand = false;
    		}
    	}
    	
        if(doExpand){
        	StringList sel  = new StringList(4);
        	sel.add(DomainConstants.SELECT_ID);  //Test Case objects ID. 
        	sel.add(DomainConstants.SELECT_TYPE); // Test Case objects name. 
        	sel.add(DomainConstants.SELECT_NAME); // Test Case objects name. 
        	sel.add(validationStatusKey);  

            MapList structure = expandFromObj.getRelatedObjects(context,
    					RequirementsUtil.getSpecStructureRelationship(context) + "," + RequirementsUtil.getSubRequirementRelationship(context) + "," + RequirementsUtil.getRequirementValidationRelationship(context), 
    					"*",   
    					sel,    // Object selects - information related to test case objects. 
                        null,       // relationship selects
                        false,      // from
                        true,       // to
                        (short)0,   //expand level
                        null,       // object where
                        null,       // relationship where
                        0);         // limit
            
            if(relId == null && objectMapList.size() == 1){ //add root to get the total counts
            	structure.add(0, objectMapList.get(0));
            }
            
            RequirementsUtil.fillTypeInfo(context, structure);
            List<String> testCaseIds = new ArrayList<>();
            
            for(int i = 0; i < structure.size(); i++){
            	Map m = (Map)structure.get(i);
            	String kind = (String)m.get("kindof");
                if(kind.equals(RequirementsUtil.getTestCaseType(context))){
                	testCaseIds.add((String)m.get(DomainConstants.SELECT_ID));
                }
            }
            
            sel  = new StringList(6);
            sel.add(DomainConstants.SELECT_ID);
            sel.add(idKey);
            sel.add(relIdKey);
            sel.add(percentageKey);
            sel.add(actualEndDateKey);
            sel.add(estStartDateKey);
            MapList execList = DomainObject.getInfo(context, testCaseIds.toArray(new String[0]), sel);
            
            for (int ii = 0; ii < execList.size(); ii++) {
            	Map<String, String> m = (Map<String, String>) execList.get(ii);
            	String TCId = m.get(DomainConstants.SELECT_ID);
            	MapList TEs; 
            	if(m.get(idKey) == null) {
            		TEs = new MapList(0);
            	}
            	else{
                	String[] TEIds = m.get(idKey).split("\7", -1);
                	String[] TETCIds = m.get(relIdKey).split("\7", -1);
                	String[] percentages = m.get(percentageKey).split("\7", -1);
                	String[] actualEndDates = m.get(actualEndDateKey).split("\7", -1);
                	String[] estStartDates = m.get(estStartDateKey).split("\7", -1);
                	TEs = new MapList(TEIds.length);
                	for(int j = 0; j < TEIds.length; j++) {
                		Map<String, String > entry = new HashMap<>();
                		entry.put(DomainConstants.SELECT_ID, TEIds[j]);
                		entry.put(DomainConstants.SELECT_RELATIONSHIP_ID, TETCIds[j]);
                		entry.put(percentageKey2, percentages[j]);
                		entry.put(actualEndDateKey2, actualEndDates[j]);
                		entry.put(estStartDateKey2, estStartDates[j]);
                		TEs.add(entry);
                	}
            	}
            	Map nextTEScheduled = NextTestExecutionScheduled(context, TEs);
            	Map lastCompletedTE = LastCompletedTestExecution(context, TEs);
        		TestCaseTestExecMap.put(TCId, new Map[]{lastCompletedTE, nextTEScheduled});
        		float percentagePassed = percentagePassLastTE(context, lastCompletedTE);
        		TestCasePercentageMap.put(TCId, percentagePassed);
        		TestCaseTECountMap.put(TCId, TEs.size());
            }
            
            
        	int indent = -1;
        	Stack path = new Stack();
        	for (int ii = 0; ii < structure.size(); ii++) {
        		Map objMap = (Map) structure.get(ii);
        		String relLevel = (String) objMap.get(SELECT_LEVEL);
        		int level = Integer.parseInt(relLevel);
        		if(level > indent){
        			indent = level;
        		}else if(level == indent){
        			Map m = (Map)path.pop();
        			int[] v = (int[])m.get("Validation");
        			if(path.size() > 0) {
            			int[] pv = (int[])((Map)path.peek()).get("Validation");
            			List[] pvn = (List[])((Map)path.peek()).get("ValidationNames");
            			for(int j = 0; j < pv.length; j++) {
            				pv[j] += v[j];
            				pvn[j].addAll(((List<String>[])m.get("ValidationNames"))[j]);
            			}
        			}
        		}else{
        			do{
        				Map m = (Map)path.pop();
            			int[] v = (int[])m.get("Validation");
            			
            			if(path.size() > 0) {
                			int[] pv = (int[])((Map)path.peek()).get("Validation");
                			List[] pvn = (List[])((Map)path.peek()).get("ValidationNames");
                			for(int j = 0; j < pv.length; j++) {
                				pv[j] += v[j];
                				pvn[j].addAll(((List[])m.get("ValidationNames"))[j]);
                			}
            			}
        				indent--;
        			}while(level < indent);
        			Map m = (Map)path.pop();
        			int[] v = (int[])m.get("Validation");
        			if(path.size() > 0) {
            			int[] pv = (int[])((Map)path.peek()).get("Validation");
            			List[] pvn = (List[])((Map)path.peek()).get("ValidationNames");
            			for(int j = 0; j < pv.length; j++) {
            				pv[j] += v[j];
            				pvn[j].addAll(((List<String>[])m.get("ValidationNames"))[j]);
            			}
        			}
        		}
        		List[] names = new ArrayList[4];
        		names[0] = new ArrayList();
        		names[1] = new ArrayList();
        		names[2] = new ArrayList();
        		names[3] = new ArrayList();
    			int[] counts = new int[]{0,0,0,0};

        		if(RequirementsUtil.getTestCaseType(context).equals(objMap.get("kindof"))){
        			String strValidationStatus = (String)objMap.get(validationStatusKey);
        			
        			if(strValidationStatus.equals("Not Validated"))
        			{
        				counts[0] += 1;
        				names[0].add((String)objMap.get(DomainConstants.SELECT_NAME));
        			}
        			else if(strValidationStatus.equals("Validation Passed"))
        			{
        				counts[1] += 1;
        				names[1].add((String)objMap.get(DomainConstants.SELECT_NAME));
        			}
        			else
        			{
        			    // HAT1 : ZUD IR-331758-3DEXPERIENCER2015x fix                                       
            			Map lastCompletedTE = TestCaseTestExecMap.get((String)objMap.get(DomainConstants.SELECT_ID))[0];
            			float percentagePassed = percentagePassLastTE(context, lastCompletedTE);
        				if(percentagePassed > 0.0)
        				{
        					counts[2] += 1;
        					names[2].add((String)objMap.get(DomainConstants.SELECT_NAME));
        				}
        				else
        				{
        					counts[3] += 1;	
        					names[3].add((String)objMap.get(DomainConstants.SELECT_NAME));
        				}
        			}
        			
        		}
        		objMap.put("Validation", counts); //not validation, passed, not full validated, failed
        		objMap.put("ValidationNames", names);
    			path.push(objMap);
        	}
        	
        	while(path.size() > 1) {
    			Map m = (Map)path.pop();
    			int[] v = (int[])m.get("Validation");
    			if(path.size() > 0) {
        			int[] pv = (int[])((Map)path.peek()).get("Validation");
        			List[] pvn = (List[])((Map)path.peek()).get("ValidationNames");
        			for(int j = 0; j < pv.length; j++) {
        				pv[j] += v[j];
        				pvn[j].addAll(((List<String>[])m.get("ValidationNames"))[j]);
        			}
    			}
        	}

        	for (int ii = 0; ii < structure.size(); ii++) {
        		Map objMap = (Map) structure.get(ii);
        		countsMap.put((String)objMap.get(DomainConstants.SELECT_ID), (int[])objMap.get("Validation"));
        	}

        	for (int ii = 0; ii < structure.size(); ii++) {
        		Map objMap = (Map) structure.get(ii);
        		namesMap.put((String)objMap.get(DomainConstants.SELECT_ID), (List[])objMap.get("ValidationNames"));
        	}
    	}
    	
        
        Iterator objectItr = objectMapList.iterator();

        while (objectItr.hasNext()) {
            try {

                // Get the information about the current row
                Map<String, String> curObjectMap = (Map) objectItr.next();
                String objectName = (String) curObjectMap.get("name");
                String objectID = (String) curObjectMap.get("id");

                String kind = (String) curObjectMap.get("kindof");
                
                if(kind == null) {
                    DomainObject dmoObj = DomainObject.newInstance(context, objectID);
                	kind = dmoObj.getInfo(context, "type.kindof");
                	if("DOCUMENTS".equals(kind)) {
                		kind = RequirementsUtil.getRequirementSpecificationType(context);
                	}
                }
                
                if (kind.equals(RequirementsUtil.getPARParameterType(context)) ||
                		kind.equals(RequirementsUtil.getCommentType(context))
                	) 
                {
                    returnVector.add(toExport != true ? "<div style='text-align:center;'> - </div>" : " "); 
                    continue;
                }

                String readAccess = (String)curObjectMap.get(RequirementsConstants.SELECT_READ_ACCESS);
                if(RequirementsConstants.DENIED.equals(readAccess)){
                    returnVector.add(toExport != true ? "<div style='text-align:center;'>" + denied + "</div>" : " "); 
                    continue;
                }

                // If we are in the SCE, we use a different place holder
                if (isSCE) {
                    // We get the timeStamp
                    Long timeStamp = new Random().nextLong();
                    try {
                        timeStamp = eMatrixDateFormat.getJavaDate(curObjectMap.get(DomainConstants.SELECT_MODIFIED)).getTime();
                    } catch (Exception ex) {
                        // NOP
                    }
                    
                    StringBuffer stringBuffer = new StringBuffer();
                    renderRichtextField(context, timeStamp.toString(), objectID, "", stringBuffer, isEditable);
                    returnVector.add(stringBuffer.toString());
                    continue;
                }
                
	            if(kind.equals(RequirementsUtil.getChapterType(context)) || kind.equals(RequirementsUtil.getRequirementSpecificationType(context)))
	            {
                	// ++ HAT1 : ZUD IR-242335V6R2015 fix                    
	            	if(toExport || doExpand)
	            	{
	            		returnVector.add(getTestCaseValidationCount(context, countsMap.get(objectID) , null, NLS, false, toExport)); 
		            }
	            	else{
	                	returnVector.add("<div id = 'ValidationColumn_"+ objectID +"'><img style='display: block; margin-left: auto; margin-right: auto;' "
	                            + "src='images/loading.gif' onload='getRootTestCaseCounts(this, \"" + objectID + "\")' /></div>");

	            	}
	            	continue;
                	// -- HAT1 : ZUD IR-242335V6R2015 fix                    
	            }
                
        	    //++ HAT1 : ZUD - HL (Validation Column) To print the count of  Relevant Test Cases for Requirements under Validation Column.                                
            	//hat1 : zud IR-326341-3DEXPERIENCER2015x fix
            	if(kind.equals(RequirementsUtil.getRequirementType(context)))
                {
                	returnVector.add(getTestCaseValidationCount(context, countsMap.get(objectID) , namesMap.get(objectID), NLS, true, toExport));  
                	continue;
				}
        		//-- HAT1 : ZUD - HL (Validation Column) To print the count of  Relevant Test Cases for Requirements under Validation Column.                

                // ++ HAT1 : ZUD - HL (Validation Column). To print the Status & Schedule for the Last performed and Next Scheduled Test Execution respectively for any Test Case.
                
                if(kind.equals(RequirementsUtil.getTestCaseType(context)))
                {
                    String strTestCaseValidationColumnStatus = " --- ",strTestCaseValidationColumnSchedule  = " --- ";
                    
                    String TestCaseValidationColumn = "";
    				String toExportTestCaseValidationColumn = "";
                	
                    if(TestCaseTECountMap.get(objectID) != 0)
                    {
                    	// ++ Last completed Test Execution.
                    	Map lastTestExecutionObjMap = TestCaseTestExecMap.get(objectID)[0];

                    	if(lastTestExecutionObjMap != null) // && isActualEndDateTE == -1
                    	{
	                    	String lastTestExecutionObjID = (String) lastTestExecutionObjMap.get("id");
	                    	String lastTestExecutionObjName = (String) lastTestExecutionObjMap.get("name");
	                    	String lastTestExecutionObjRelID = (String) lastTestExecutionObjMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
							
							float PercentagePassed = TestCasePercentageMap.get(objectID); // Float.parseFloat(strPercentagePassed); // Converting it into Float.
							
							Date dateLastActualEndDateTE	= eMatrixDateFormat.getJavaDate((String)lastTestExecutionObjMap.get(actualEndDateKey2));

							SimpleDateFormat strDate = new SimpleDateFormat ("yyyy/MM/dd hh:mm a");      //Required format for date display.
							String strLastActualEndDateTE = (String) strDate.format(dateLastActualEndDateTE);   // Converting default Date object format to Required String format. 
							
		                    String TC_TE_PARTIALLYPASSED = MessageUtil.getMessage(context, null, "emxRequirements.TestCaseValidationColumn.ToolTip.Partially", new String[]{PercentagePassed + ""}, null, context.getLocale(), "emxRequirementsStringResource");
							if(PercentagePassed > 0.00 && PercentagePassed < 100.00)
							{
								strTestCaseValidationColumnStatus = "Partial";
								TestCaseValidationColumn ="<table><tr>" 
								  +"<td width = '45px' style = 'background-color:yellow;' title = '" + TC_TE_PARTIALLYPASSED +" \u00A0\u00A0 "+ strLastActualEndDateTE +"'>" 
                    					+ "<a href='#'  onClick ='testExecutionPopUp(\"" + lastTestExecutionObjID + "\", \"" + lastTestExecutionObjRelID + "\",\"" + objectID + "\")' >" + strTestCaseValidationColumnStatus + "</a>" 
										+ "</td>";
								
			                	// HAT1 : ZUD IR-242335V6R2015 fix                    
								toExportTestCaseValidationColumn = strTestCaseValidationColumnStatus;
							}
							
							else if(PercentagePassed == 0.00)
							{
								strTestCaseValidationColumnStatus = "Failed";
								TestCaseValidationColumn ="<table><tr>" 
								  +"<td width = '45px' style = 'background-color:red;' title = '" + TC_TE_FAILED + "\u00A0\u00A0" + strLastActualEndDateTE+"'>"
                    					+ "<a href='#'  onClick ='testExecutionPopUp(\"" + lastTestExecutionObjID + "\", \"" + lastTestExecutionObjRelID + "\",\"" + objectID + "\")' >" + strTestCaseValidationColumnStatus + "</a>" 										
									+ "</td>";
			                	// HAT1 : ZUD IR-242335V6R2015 fix  
								toExportTestCaseValidationColumn = strTestCaseValidationColumnStatus;

							}
							
							else // (PercentagePassed == 100.00)
							{
								strTestCaseValidationColumnStatus = "Passed";
								TestCaseValidationColumn ="<table><tr>" 
                     					+"<td width = '45px' style = 'background-color:#39FF14;' title = '" + TC_TE_PASSED + "\u00A0\u00A0 "+ strLastActualEndDateTE+"'>" 
      								
                    					+ "<a href='#'  onClick ='testExecutionPopUp(\"" + lastTestExecutionObjID + "\", \"" + lastTestExecutionObjRelID + "\",\"" + objectID + "\")' >" + strTestCaseValidationColumnStatus + "</a>" 										
										+ "</td>";
			                	// HAT1 : ZUD IR-242335V6R2015 fix  
								toExportTestCaseValidationColumn = strTestCaseValidationColumnStatus;
							}
						}
                    	else
						{
							TestCaseValidationColumn ="<table><tr>" 
                					+"<td style = 'background-color:aqua;' title = '" + TC_NO_TE_REPLAYED +"'>" + strTestCaseValidationColumnStatus + "</td>";
							
		                	// HAT1 : ZUD IR-242335V6R2015 fix  
							toExportTestCaseValidationColumn = TC_NO_TE_REPLAYED;

						}
                    	//-- Last completed Test Execution.
                    	
                    	// ++ Next Test Execution Scheduled Start. 
                    	
                    	Map nextScheduledTestExecutionObjMap = TestCaseTestExecMap.get(objectID)[1];
                    	String strEarliestEstimatedStartDateTE = "";
                    	
                    	if(nextScheduledTestExecutionObjMap != null)
                    	{
                    		String nextTestExecutionObjID = (String) nextScheduledTestExecutionObjMap.get("id");
	                    	String nextTestExecutionObjName = (String) nextScheduledTestExecutionObjMap.get("name");
	                    	String nextTestExecutionObjRelID = (String) nextScheduledTestExecutionObjMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
	                    	//DomainObject nextTestExecutionDmoObj = DomainObject.newInstance(context, nextTestExecutionObjID);

	                    	Date earliestEstimatedStartDateTE  = eMatrixDateFormat.getJavaDate((String)nextScheduledTestExecutionObjMap.get(estStartDateKey2));

							SimpleDateFormat strDate = new SimpleDateFormat ("yyyy/MM/dd hh:mm a");      //Required format for date display.
							strEarliestEstimatedStartDateTE = (String) strDate.format(earliestEstimatedStartDateTE);   // Converting default Date object format to Required String format. 

							TestCaseValidationColumn += "<td>" + " / "+ "</td>"
                					+"<td style = 'background-color:#39FF14;' title = '" + TC_NEXT_TE_SCHEDULED + " \u00A0\u00A0 "+ strEarliestEstimatedStartDateTE +"'>"          						
                					+ "<a href='#'  onClick ='testExecutionPopUp(\"" + nextTestExecutionObjID + "\", \"" + nextTestExecutionObjRelID + "\",\"" + objectID + "\")' >" + strEarliestEstimatedStartDateTE + "</a>" 										
                					+   "</td>"
                							+ "</tr></table>";
		                	// HAT1 : ZUD IR-242335V6R2015 fix  
							toExportTestCaseValidationColumn += "/" + strEarliestEstimatedStartDateTE;

                    	}
                    	else
                    	{
							TestCaseValidationColumn += "<td>" + " / "+ "</td>"
										+"<td style = 'background-color:aqua;' title = '" + TC_NO_TE_SCHEDULED +"'>" + strTestCaseValidationColumnSchedule + "</td>"
                							+ "</tr></table>";
		                	// HAT1 : ZUD IR-242335V6R2015 fix  
							toExportTestCaseValidationColumn += "/" + TC_NO_TE_SCHEDULED;
                    	}
                    	// -- Next Test Execution Scheduled End.
                    }
                    else
                    {
                    	TestCaseValidationColumn ="<table><tr>" 
                    					+"<td style = 'background-color:aqua;' title = '" + TC_NO_TE_REPLAYED +"'>" + strTestCaseValidationColumnStatus + "</td>"
                    							+"<td>" + " / "+ "</td>"
                    					+"<td style = 'background-color:aqua;' title = '" + TC_NO_TE_SCHEDULED +"'>" +strTestCaseValidationColumnSchedule  + "</td>"
                    							+ "</tr></table>";
	                	// HAT1 : ZUD IR-242335V6R2015 fix  
						toExportTestCaseValidationColumn = TC_NO_TE_REPLAYED + "/" + TC_NO_TE_SCHEDULED;
                    }
                    if(!toExport)
                    {
                    	returnVector.add(TestCaseValidationColumn);
                    }
                    else
                    {
                        returnVector.add(toExportTestCaseValidationColumn);
                    }
                }
                // -- HAT1 : ZUD - HL (Validation Column). To print the Status & Schedule for the Last performed and Next Scheduled Test Execution respectively for any Test Case.
                
            } catch (Exception ex) {
                String strError = "<img style='display: block; margin-left: auto; margin-right: auto;' "
                        + "alt='Error' src='data:image;base64," + ImageUtil.DEFAULT_PICTURE_ERROR + "' />";
                returnVector.add(toExport != true ? strError : " ");
                continue;
            }
        }
        return returnVector;
    }      

    /**
     * Finds the next scheduled Test Execution for a Test Case.
     * @param context - the eMatrix <code>Context</code> object
     * @param args - All the Test Executions created for a Test Case
     * @return the next scheduled Test Execution, null if no such TE.
     * @throws Exception
     */    

public Map NextTestExecutionScheduled(Context context, MapList TestCaseTestExecutionList)
{
    final String estStartDateKey = "attribute[" + RequirementsUtil.getEstimatedStartDateAttribute(context) + "]";
    final String actualEndDateKey = "attribute[" + RequirementsUtil.getActualEndDateAttribute(context) + "]";

	Map<String, String> currTestExecutionObjMap = null;
	Map<String, String> nextTestExecutionObjMap = null;    // Map for Next Scheduled Test Execution under Test Case.
	
	Date currentDate = new Date();                     // currentDate object of type Date.
	Date dateNextEstimatedStartDateTE = currentDate;     // Populating 'dateLastActualEndDateTE' with currentDate which will hold attribute 'Actual End Date' of Last connected Test Execution.
	Date currEstimatedStartDateTE = null;

    if(TestCaseTestExecutionList.size() != 0)
    {
        String strTestCaseTestExecutionListCount = String.valueOf(TestCaseTestExecutionList.size());
    	Iterator testExecutionItr = TestCaseTestExecutionList.iterator();
    	int isAnyNextTEfound = 0;
		while(testExecutionItr.hasNext())
		{
			currTestExecutionObjMap = (Map) testExecutionItr.next();
			
			if(!((String)currTestExecutionObjMap.get(actualEndDateKey)).equals(""))
			{
				continue;  //Test Execution is already started.
			}
			try 
			{
				currEstimatedStartDateTE = eMatrixDateFormat.getJavaDate((String)currTestExecutionObjMap.get(estStartDateKey));
			} 
			catch (Exception e) 
			{
				continue;     // Test Execution have no "Estimated Start Date".
			}
			
			int isBeforeCurrEstimatedStartDate = currEstimatedStartDateTE.compareTo(dateNextEstimatedStartDateTE);
			
			int isAfterCurrentDate = currEstimatedStartDateTE.compareTo(currentDate);
			
			if(isAfterCurrentDate == 1 && isAnyNextTEfound == 0 && isBeforeCurrEstimatedStartDate == 1)
			{
				isAnyNextTEfound = 1;
				nextTestExecutionObjMap = currTestExecutionObjMap;
				dateNextEstimatedStartDateTE = currEstimatedStartDateTE;
			}
			
			if(isAfterCurrentDate == 1 && isAnyNextTEfound == 1 && isBeforeCurrEstimatedStartDate == -1)
			{
				nextTestExecutionObjMap = currTestExecutionObjMap;
				dateNextEstimatedStartDateTE = currEstimatedStartDateTE;
			}
		}
  }
  return nextTestExecutionObjMap;
}
 
    /**
     * Finds the Last completed Test Execution under Test Case.
     * @param context - the eMatrix <code>Context</code> object
     * @param args - All the Test Executions created for a Test Case
     * @return the last completed Test Execution, null if no such TE.
     * @throws Exception
     */
    public Map LastCompletedTestExecution(Context context, MapList TestCaseTestExecutionList)
    {
        final String actualEndDateKey = "attribute[" + RequirementsUtil.getActualEndDateAttribute(context) + "]";

    	Map<String, String> currTestExecutionObjMap = null;
    	Map<String, String> lastTestExecutionObjMap = null;            // Map for Last connected Test Execution to Test Case.
    	
    	Date currentDate = new Date();                                 // currentDate object of type Date.
    	Date dateLastActualEndDateTE = currentDate;                    // Populating 'dateLastActualEndDateTE' with currentDate which will hold attribute 'Actual End Date' of Last connected Test Execution.
    	Date currActualEndDateTE = null;
    	    	
    	if(TestCaseTestExecutionList.size() != 0)
        {
	    	Iterator testExecutionItr = TestCaseTestExecutionList.iterator();
	    	int isAnyLastTEfound = 0;
			while(testExecutionItr.hasNext())
			{
				currTestExecutionObjMap = (Map) testExecutionItr.next();
				
				try
				{
					currActualEndDateTE	= eMatrixDateFormat.getJavaDate((String)currTestExecutionObjMap.get(actualEndDateKey));
				} 
				catch (Exception e) 
				{
					continue;
				}
				
				int flag = currActualEndDateTE.compareTo(dateLastActualEndDateTE);
				
				if(isAnyLastTEfound == 0 && (flag == -1))
				{
					isAnyLastTEfound = 1;
					lastTestExecutionObjMap = currTestExecutionObjMap;
					dateLastActualEndDateTE = currActualEndDateTE;
				}
				
				if(isAnyLastTEfound == 1 && flag == 1)
				{
					lastTestExecutionObjMap = currTestExecutionObjMap;
					dateLastActualEndDateTE = currActualEndDateTE;
				}
				
			}
        }
		return lastTestExecutionObjMap;
    }
    

/**
 * This function checks status of all the test cases which comes under any object and count them separately.
 * @param context - the eMatrix <code>Context</code> object
 * @param args - the inputMap which contains List object Test Cases ids.
 * @return a String containing counts of Test Cases . If check is OK returns null
 */

public String getTestCaseValidationCount(Context context, int[] counts, List[] names, String[] NLS, boolean witLink, boolean isExport ) throws Exception
{
	int notValidatedCount = counts[0], validationPassedCount = counts[1], notFulValidatedCount = counts[2], validationFailedCount = counts[3];
	String strReqSpecTestCasesStatusCount = "";
	
	if(notValidatedCount + validationPassedCount + notFulValidatedCount + validationFailedCount != 0)
	{
    	if(isExport){
       		// HAT1 : ZUD IR-242335V6R2015 fix                    
    	       strReqSpecTestCasesStatusCount = notValidatedCount + " / " + notFulValidatedCount + "/ " + validationFailedCount + " /" + validationPassedCount;
    	}
    	else{
    	       strReqSpecTestCasesStatusCount ="<table>"
    					+                         "<tr>"
    					+                             "<td  style = 'background-color:aqua;' title = '" + notValidatedCount + "\u00A0\u00A0" + NLS[0] + "'>"
    					+ 									(witLink ? "<a href='#' onClick = 'testCaseNotValidatedList(this, \"" + names[0] + "\")'> "  : "<b>") + notValidatedCount 
    					+ 									(witLink ? "</a>" : "</b>") + "</td>" 
    					                                  + "<td>"+ "  /  " + "</td>"
    				    +                             "<td  style = 'background-color:yellow;'  title = '" + notFulValidatedCount + "\u00A0\u00A0" + NLS[1] + "'>" 
    					+ 									(witLink ? "<a href='#' onClick ='testCaseNotFulValidatedList(this, \"" + names[1] + "\")' >" : "<b>") + notFulValidatedCount 
    					+ 									(witLink ? "</a>" : "</b>") + "</td>"
    													  + "<td>"+ "  /  " + "</td>"
    					+                             "<td  style = 'background-color:red;' title = '" + validationFailedCount +  "\u00A0\u00A0" + NLS[2] + "'>" 
    					+ 									(witLink ? "<a href='#' onClick ='testCaseValidationFailedList(this, \"" + names[2] + "\")' >" : "<b>") + validationFailedCount 
    					+ 									(witLink ? "</a>" : "</b>") + "</td>" 
    	                								  + "<td>"+ "  /  " + "</td>"
    					+                             "<td  style = 'background-color:#39FF14;'  title = '" + validationPassedCount + "\u00A0\u00A0" + NLS[3] + "'>" 
    	                + 									(witLink ? "<a href='#' onClick ='testCaseValidationPassedList(this, \"" + names[3] + "\")' >" : "<b>") + validationPassedCount 
    	                + 									(witLink ? "</a>" : "</b>") + "</td>" 
    					+                         "</tr>"
    					+                    "</table>";
    	}
	}
	else
	{
		if(isExport){
			// HAT1 : ZUD IR-242335V6R2015 fix                    
			strReqSpecTestCasesStatusCount = "0";
		}
		else{
			strReqSpecTestCasesStatusCount = "<table>" +"<tr>"
					+"<td title = '"+ NLS[4] +"'>"
						+ "<h2><font color='red'>0</font></h2>"
					+"</td>"
						+ "</tr></table>";
		}
	}
	
	return strReqSpecTestCasesStatusCount;		
}   

//-- HAT1 : ZUD 12-AUG-14 Validation Column under Requirement Specification and Chapter.

//HAT1 : ZUD 13-AUG-14 Validation Column under Requirement Specification and Chapter.
/**
 * This function takes DomainObject of Test Case and return percentage pass of 'Last Test Execution' replayed.
 * @param context - the eMatrix <code>Context</code> object
 * @param args - the input which contains  Test Cases DomainObject.
 * @return a String containing Percentage Passed of the last Test Execution.
 */

// ++ HAT1 : ZUD IR-331758-3DEXPERIENCER2015x fix                                       
public float percentagePassLastTE(Context context, Map lastTestExecutionObjMap) throws Exception
{
    final String percentageKey = "attribute[" + RequirementsUtil.getPercentagePassedAttribute(context) + "]";
    
	String strPercentagePassed = "-1";

	if(lastTestExecutionObjMap != null) //When the TC have Last Completed Test Execution.
	{
		strPercentagePassed = (String)lastTestExecutionObjMap.get(percentageKey);
	}

	return Float.parseFloat(strPercentagePassed);
}


    /* The place holder for the SCE */
    private void renderRichtextField(Context context, String sTimeStamp, String objId, String relId,
            StringBuffer buffer, boolean isEditable) {
        String MSG_LOADING = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.SCE.Message.Loading"); 
        buffer.append("<div class=\"rtccontainer\" editable=\""
                + isEditable
                + "\">"
                + MSG_LOADING + "</div>");
    }

    /**
     * Converts RTF to HTML for one object
     * @param context - the eMatrix <code>Context</code> object
     * @param args - the object ID
     * @return the HTML source from the RTF
     * @throws Exception
     */
    public String getHTMLSourceFromRTF(Context context, String[] args) throws Exception {

        Map argsMap = (HashMap) JPO.unpackArgs(args);
        String objectID = (String) argsMap.get("objectId");
        String htmlPreview = (String) argsMap.get("htmlPreview");
        
        boolean USE_RTF_CONVERTOR = true;
        
        try {
            DomainObject dmoObj = DomainObject.newInstance(context, objectID);
            String strContentType = dmoObj.getAttributeValue(context, "Content Type");
            String strContentData = dmoObj.getAttributeValue(context, "Content Data");
            String strContentText = dmoObj.getAttributeValue(context, "Content Text");

            String modifed = dmoObj.getInfo(context, DomainConstants.SELECT_MODIFIED);
            String lud = eMatrixDateFormat.getJavaDate(modifed).getTime() + "";        

            if (!USE_RTF_CONVERTOR) {
                StringBuilder plainText = new StringBuilder(strContentText);
                return plainText.toString();
            }
            
            // If the Content Data is empty
            if (strContentData.isEmpty()) {
                return ConvertedDataDecorator.putRichTextEditorDiv(context, objectID);
            }
            
            // If we have RTF data
            if (strContentType.equals("rtf.gz.b64")) {
                if ("false".equalsIgnoreCase(htmlPreview)) { // If we want to show the Content Text instead of the HTML Preview
                    return ConvertedDataDecorator.putRichTextEditorDivForRTF(context, objectID, strContentText, "Aspose");
                }
                
                ByteArrayInputStream bInputStream = new ByteArrayInputStream(Base64.decode(strContentData));
                GZIPInputStream gInputStream = new GZIPInputStream(bInputStream);
                StringBuilder stringBuilder = new StringBuilder();
                byte[] data = IOUtils.toByteArray(gInputStream);
                bInputStream.close();
                gInputStream.close();

                String strData = new String(data);
                int sizePureData = strData.length();
                StringBuilder sb = new StringBuilder(strData);

                RMTConvertor convertor = RMTConvertor.Factory.create(context, objectID, EConvertorSettings.RTF2HTML, sb);
                String objectIdForDiv = objectID.replace(".", ""); // TODO put a if HTML/.NET AND replace convertor ID hardcoded
                return ConvertedDataDecorator.putRichTextEditorDivForRTF(context, objectID, convertor.convert(SubConvertorSet.ASPOSE), "Aspose");
            } else if (strContentType.equals("html")) {
                return ConvertedDataDecorator.putRichTextEditorDivForRTF(context, objectID, strContentData, "None");
            }
        } catch (Exception ex) {
            String strError = "<img style='display: block; margin-left: auto; margin-right: auto;' "
                    + "alt='Error' src='data:image;base64," + ImageUtil.DEFAULT_PICTURE_ERROR + "' />";
            return strError;
        }
        return "<div style='text-align:center;'> - </div>";
    }

    /**
     * Updates the RTF for on object, INPUT: Format | EncodedRichText | ContentText OR Format | HTML | ContentText
     * @param context - the eMatrix <code>Context</code> object
     * @param args - the object ID, the New Value who contains the RTF
     * @return nothing
     * @throws Exception
     */
    public String updateRichTextContent(Context context, String[] args) throws Exception {
        
        Map programMap = (HashMap) JPO.unpackArgs(args);
        Map paramMap = (HashMap) programMap.get("paramMap");
        String newValue = (String) paramMap.get("New Value");
        String strObjectId = (String) paramMap.get("objectId");
        //String Param_Val = (String) paramMap.get("Param_Val");
        // Fix for IR-333259-3DEXPERIENCER2015x
        newValue += " ";
        String defaultStringRTF = "H4sIAAAAAAAEAO29B2AcSZYlJi9tynt/SvVK1+B0oQiAYBMk2JBAEOzB" +
	        "iM3mkuwdaUcjKasqgcplVmVdZhZAzO2dvPfee++999577733ujudTif33/8/XGZkAWz2zkrayZ4hgKrIHz" + 
	        "9+fB8/In7x71u357u/b7ZsCv5nurrY3bu/9/vO8vPzHfxbZsuL3Z17937x73teLdt2UtIvO7/v+bIof9/z" + 
	        "6Tyrm7zdSb8opnXVVOdt+pqApK/zujg//CW/5DdOfvHv+63f9yJf5nXWVnX6RXPe5rOiTe+P93fHe/S/+7s7h" + 
	        "7/k970s8qu3xXK2//uup7u/7yqrZ9xHs3uAP37j5Jf8xsn/A8I8crOoAAAA";
        
        String defaultStringWordRTF = "H4sIAAAAAAAEAO29B2AcSZYlJi9tynt/SvVK1+B0oQiAYBMk2JBAEO" + 
            "zBiM3mkuwdaUcjKasqgcplVmVdZhZAzO2dvPfee++999577733ujudTif33/8/XGZkAWz2zkrayZ4hgKrIH" + 
            "z9+fB8/In7x71u357u/b7ZsCv5nurrY3bu/9/vO8vPzHfxbZsuL3Z1798zv5zn++sW/73m1bNtJSb/s/L7n" +
            "y6L8fc+n86xu8nYn/aKY1lVTnbfpawKZvs7r4vzwl/yS3zj5xb/vt37fi3yZ11lb1ekXzXmbz4o2vT/e3x3v" +
            "0f/u7+4c/pLf97LIr94Wy9n+77ue7v6+q6yecR/N7gH++I2TX/IbJ/8PHvxIx7YAAAA=";
        
        // This method is called when Save button in Structure Browser page is clicked
        // Zud Check if it is a parameter
        DomainObject dmoObj = DomainObject.newInstance(context, strObjectId);
        if(dmoObj.getInfo(context, DomainConstants.SELECT_TYPE).equals(RequirementsUtil.getParameterType(context)))
        {
        	String [] Param_values ={strObjectId,newValue};
        	
        	setParameterString(context,JPO.packArgs(Param_values));
        	return "";
        	
        }
        else
        {
        // Get the server URL to create a document/manage OLE objects
        int limitURL = newValue.indexOf("|");
        if (limitURL < 0) return "";
        
        String serverURL = newValue.substring(0, limitURL);
        
        // Get the format to know how to save the data (RTF or HTML)
        int limitFormat = newValue.indexOf("|", limitURL + 1);
        if (limitFormat < 0) return "";
        
        String formatToSave = newValue.substring(limitURL + 1, limitFormat);
        
        // We can save the data in the database
        DomainObject dmoObject = DomainObject.newInstance(context, strObjectId);
        ContextUtil.startTransaction(context, true);
        
        // We get the timeStamp
        Long timeStamp = new Random().nextLong();
        try {
            timeStamp = eMatrixDateFormat.getJavaDate(dmoObj.getInfo(context, DomainConstants.SELECT_MODIFIED)).getTime();
        } catch (Exception ex) {
            // NOP
        }
        
        // NOT USE TODAY. Will be use when Aspose will be available
        // WARNING: the user is going to erase data with another format, we can save the previous content
        // with a reference document
        if (!dmoObject.getAttributeValue(context, "Content Type").equals(formatToSave) && RequirementsUtil.isRequirement(context, strObjectId)) {
            // RTF -> HTML
            if (formatToSave.equals("html") && paramMap.get("isNewObject") == null) {
            	
            	String reqContentData = dmoObject.getAttributeValue(context, "Content Data");
            	
            	// If the old content is not empty
            	if (reqContentData.length() != 0) {
	                ReferenceDocumentUtil refDoUtil = new ReferenceDocumentUtil();
	                
	                // Doc name
	                String refDocName = "RTF-Backup-" + timeStamp; 
	                String refDocTitle = "RTF-Backup";
	                String refDocDescription = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.RichTextEditor.BackUp.RefDocDescription");
	                CommonDocument refDoc = refDoUtil.createAndConnectRefDocument(context, strObjectId, refDocName, refDocTitle, refDocDescription);
	                
	                // Check-in
	                ByteArrayInputStream bInputStream = new ByteArrayInputStream(Base64.decode(dmoObject.getAttributeValue(context, "Content Data")));
	                GZIPInputStream gInputStream = new GZIPInputStream(bInputStream);
	                byte[] data = IOUtils.toByteArray(gInputStream);
	                bInputStream.close();
	                gInputStream.close();
	
	                refDoUtil.checkInDocument(context, serverURL, refDoc, refDocName, data);
            	}
            }
        }

        // Save the content data
        int limitDataRichText = newValue.lastIndexOf("|");
        String richTextData = newValue.substring(limitFormat + 1, limitDataRichText);
        
        // If the RTF is empty, it will be html
        if (defaultStringRTF.equals(richTextData) || defaultStringWordRTF.equals(richTextData)) {
        	formatToSave = "html";
        	richTextData = "";
        }
        
        // Set the content type
        dmoObject.setAttributeValue(context, "Content Type", formatToSave);

        // Decode from base64
        if(!"rtf.gz.b64".equals(formatToSave)) {
        	richTextData = new String(Base64.decode(richTextData),"UTF-8");
        }
        
        // Check bad patterns
        dmoObject.setAttributeValue(context, "Content Data", inputFilterBadPatterns(context, richTextData));
        
        // Set the plain text
        dmoObject.setAttributeValue(context, "Content Text", new String(Base64.decode(newValue.substring(limitDataRichText + 1)), "UTF-8"));
       
        ContextUtil.commitTransaction(context);
        return "";
    }
}
    
    /**
     * Get the emxFramework.InputFilter.BadRegExp property and remove the occurences from the string.
     * @param context - the context.
     * @param str - the string to treat.
     * @return a new string without the bad patterns.
     */
    private String inputFilterBadPatterns(Context context, String str) {

        String returnStr = str;

        // Get the patterns from the properties file
        StringList inputFilterBadPatterns = new StringList();
        try {
            String strInputFilterBasPatterns = EnoviaResourceBundle.getProperty(context, "emxFramework.InputFilter.BadRegExp");
            inputFilterBadPatterns = StringUtil.split(strInputFilterBasPatterns, "|");
        } catch (Exception e) {
            inputFilterBadPatterns = new StringList();
        }

        // Creates the iterators to get the pattern one by one
        Iterator itr = inputFilterBadPatterns.iterator();

        // Remove the bad patterns from our original string
        while (itr.hasNext()) {
            String strBadPattern = (String) itr.next();
            Pattern pattern = Pattern.compile(strBadPattern);
            Matcher matcher = pattern.matcher(str);
            while (matcher.find()) {
                returnStr = returnStr.replace(matcher.group(), "");
            }
        }

        return returnStr;
    }
    
    /**
     * Method to set the richtext from a direct call;
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
	public String setRichTextContentFromDirectCall(Context context,
			String[] args) {
		// We use a different path in the logic if it's a new object
		Map programMap = null;
		
		try {
			programMap = (HashMap) JPO.unpackArgs(args);
			String type = (String) programMap.get("type");
			String objectId = (String) programMap.get("objectId");
			String contentData = (String) programMap.get("contentData");
			String contentText = (String) programMap.get("contentText");

			// We can save the data in the database
			DomainObject dmoObject = DomainObject
					.newInstance(context, objectId);
			ContextUtil.startTransaction(context, true);

			// Set the content type
			dmoObject.setAttributeValue(context, "Content Type", type);

			// Decode from base64
			contentData = new String(Base64.decode(contentData));

			dmoObject.setAttributeValue(context, "Content Data", inputFilterBadPatterns(context, contentData));

			// Set the plain text
			dmoObject.setAttributeValue(context, "Content Text", new String(
					Base64.decode(contentText)));

			ContextUtil.commitTransaction(context);
		} catch (Exception e) {
			return "false";
		}
		
		return "true";
	}
    
    public String createTempFileForRefDocument(Context context, String[] args) throws Exception {
        return ReferenceDocumentUtil.createTemporaryFile((byte []) JPO.unpackArgs(args));
    }
    
    public String attachRefDocumentFromTempFile(Context context, String[] args) throws Exception {
        Map programMap = (HashMap) JPO.unpackArgs(args);
        String strObjectId = (String) programMap.get("objectId");
        String filePath = (String) programMap.get("filePath");
        String serverURL = (String) programMap.get("serverURL");
        
        String refDocName = (String) programMap.get("refDocName");
        String refDocTitle = (String) programMap.get("refDocTitle");
        String refDocDescription = (String) programMap.get("refDocDescription");

        ContextUtil.startTransaction(context, true);
        try{
	        ReferenceDocumentUtil refDoUtil = new ReferenceDocumentUtil();
	        
	        CommonDocument refDoc = refDoUtil.createAndConnectRefDocument(context, strObjectId, refDocName, refDocTitle, refDocDescription);
	
	        FileInputStream inputStream = new FileInputStream(filePath);
	        refDoUtil.checkInDocument(context, serverURL, refDoc, refDocName, IOUtils.toByteArray(inputStream));
	        inputStream.close();
	        
	        // Delete the temp file
	        new File(filePath).delete();
	        
	        // Refresh the content data with the reference document
	        DomainObject dmoObject = DomainObject.newInstance(context, strObjectId);
	        StringBuilder contentData = new StringBuilder(dmoObject.getAttributeValue(context, "Content Data"));
	        
			int start = 0;
			do{
				int figureStart = contentData.indexOf("<figure", start),
						rcoDataStart = contentData.indexOf("data-cke-saved-rco=", start),
						docIdStart = contentData.indexOf("rcoReferenceDocFileId", rcoDataStart),
						docIdEnd = contentData.indexOf(",", docIdStart),
						fileNameStart = contentData.indexOf("'rcoFileName':'", figureStart),
						fileNameEnd = contentData.indexOf(",", fileNameStart);
				if(figureStart == -1 || rcoDataStart == -1 || docIdStart == -1 || docIdEnd == -1 || fileNameStart == -1 || fileNameEnd == -1 ) {
					break;
				}
				
				String fileName = contentData.substring(fileNameStart + "'rcoFileName':'".length(), fileNameEnd -1 );
				
				if(fileName.equalsIgnoreCase(refDocName)) {
					contentData.replace(docIdStart, docIdEnd - 1, "rcoReferenceDocFileId':'" + refDoc.getObjectId());
					
					String isSaved = "'isSaved':";
					int isSavedStart = contentData.indexOf(isSaved, fileNameEnd),
						isSavedFalseStart = contentData.indexOf("'isSaved':false", isSavedStart);
					
					if(isSavedStart == isSavedFalseStart) {
						contentData.replace(isSavedStart, isSavedStart + "'isSaved':false".length(), "'isSaved':true");
					}
					break;
				}
				else {
					start = fileNameEnd;
					continue;
				}
			}while(start != -1 && contentData.indexOf("<figure", start) != -1);
			
	
	        dmoObject.setAttributeValue(context, "Content Data", contentData.toString());
	        ContextUtil.commitTransaction(context);
        }catch(Exception e) {
        	ContextUtil.abortTransaction(context);
        	throw e;
        }
        
        return strObjectId;
    }
    
    public String runJavascriptLoader(Context context, String[] args) throws Exception {
        
        // To switch between view mode and edit mode
        return "<script type=\"text/javascript\"> " + 
          "function RMTDblClickToEdit(){" + 
          "var urlToLoad = referer; " +
          "if (referer.indexOf('mode=view') > 0) " +
          "    urlToLoad = referer.replace('mode=view', 'mode=Edit'); " +
          "if (urlToLoad.indexOf('mode=Edit') < 1) " +
          "    urlToLoad += '&mode=Edit'; " +
          "toggleMode(urlToLoad); " +
          "}" + 
            " $(document).ready(function() { " +
                "$.getScript(\"../requirements/RequirementAddExisting.jsp\", function () { " +
                "$('.field').dblclick(function() { " +
                  "RMTDblClickToEdit();" + 
                    "}" + 
                 "); }); }); </script>";
    }
    
    /**
     * Returns a simple link to edit the RTF content.
     * @param context - the eMatrix <code>Context</code> object
     * @param args - None
     * @return the link
     * @throws Exception
     */
    public String getRichTextControl(Context context, String[] args) throws Exception {
        Map programMap = (HashMap) JPO.unpackArgs(args);
        
        Map requestMap = (HashMap) programMap.get("requestMap");
        Map paramMap = (HashMap) programMap.get("paramMap");
        
        // To know if we have to face IE or Firefox
        String userAgent = (String) requestMap.get("User-Agent");
        boolean isIE = false;
        if (userAgent.contains("Windows") && userAgent.contains("MSIE") && userAgent.contains("Trident"))
            isIE = true;
        
        String mode = (String) requestMap.get("mode");
        String objectId = (String) paramMap.get("objectId");
        String relId = (String) paramMap.get("relId");
        String form = (String) requestMap.get("form");
        String lud = "";
        String contentType = "";
       
        if(objectId != null) {
            String modifed = DomainObject.newInstance(context, objectId).getInfo(context, DomainConstants.SELECT_MODIFIED);
            lud = eMatrixDateFormat.getJavaDate(modifed).getTime() + "";     
            
            DomainObject dmoObject = DomainObject.newInstance(context, objectId);
            contentType = dmoObject.getAttributeValue(context, "Content Type");
        }
        
        if (objectId != null && !"Edit".equalsIgnoreCase(mode)) { // Form view
            return "<div id=\"NewRichTextEditorRMT\" style=\"max-height:170px; height:150px; min-height:20px; overflow-y: auto; overflow-x: auto;\"></div>" + "<script type=\"text/javascript\"> $.getScript(\"../requirements/RequirementAddExisting.jsp\", function () { " +
                        "$.getScript(\"../requirements/scripts/RichTextEditorCommon.js\", function () { " +
                            "$.getScript(\"../requirements/scripts/RichTextEditorForm.js\", function () { " +
                                "getRichTextEditor(\"View\", \"" + (objectId != null ? objectId : "") + "\", \"" + (relId != null ? relId : "") + "\"," + lud + ", \"" + (contentType != null ? contentType : "") + "\");" +
                             "});" +
                        "});" +
                    "}); </script>";
        }
        
        // Create or edit
        return "<div id=\"NewRichTextEditorRMT\" style=\"max-height:170px; height:150px; min-height:20px; text-align: center;\">" +
                "<img src='images/loading.gif' id='loadingGifFormRMT' onload='getRichTextEditor(\"Edit\", \"" + (objectId != null ? objectId : "") + "\", \"" + (relId != null ? relId : "") + "\",\"" + lud + "\", \"" + (contentType != null ? contentType : "") + "\")' />" +
                "</div>";
    }
    
    public String setRichTextContentNewObject(Context context, String[] args) throws Exception {
        // We use a different path in the logic if it's a new object
        Map programMap = (HashMap) JPO.unpackArgs(args);
        Map paramMap = (HashMap) programMap.get("paramMap");
        paramMap.put("isNewObject", "true");
        JPO.packArgs(programMap);
        return updateRichTextContent(context, args);
    } 
    
    public String getHTMLFromDirectRichText(Context context, String[] args) throws Exception {
        Map argsMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) argsMap.get("objectId");
        String richTextContent = (String) argsMap.get("RTFContent");
        String htmlPreview = (String) argsMap.get("htmlPreview");

        if ("false".equalsIgnoreCase(htmlPreview))
            return "";
        
        // Convert the RTF data to HTML
        ByteArrayInputStream bInputStream = new ByteArrayInputStream(Base64.decode(richTextContent));
        GZIPInputStream gInputStream = new GZIPInputStream(bInputStream);
        StringBuilder stringBuilder = new StringBuilder();
        byte[] data = IOUtils.toByteArray(gInputStream);
        bInputStream.close();
        gInputStream.close();

        String strData = new String(data);
        int sizePureData = strData.length();
        StringBuilder sb = new StringBuilder(strData);

        RMTConvertor convertor = RMTConvertor.Factory.create(context, objectId, EConvertorSettings.RTF2HTML, sb);
        return convertor.convert(SubConvertorSet.ASPOSE);
    }
    
    public String getHTMLFromDirectHTML(Context context, String[] args) throws Exception {
        Map argsMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) argsMap.get("objectId");
        if (objectId == null || objectId.isEmpty())
            return "";
        
        DomainObject dmoObject = DomainObject.newInstance(context, objectId);
        return /* URLDecoder.decode( */dmoObject.getAttributeValue(context, "Content Data") /*, "UTF-8")*/;
    }
    
    public Vector getValueContent(Context context, String[] args) throws Exception {
        Map inputMap = (Map) JPO.unpackArgs(args);
        Vector returnVector = new Vector();

        MapList objectMap = (MapList) inputMap.get("objectList");

        Iterator objectItr = objectMap.iterator();
        ArrayList<String> objectIdsForParam = new ArrayList<String>();
        while (objectItr.hasNext()) {
            Map<String, String> curObjectMap = (Map) objectItr.next();
            objectIdsForParam.add((String) curObjectMap.get("id"));
        }

        for (MapList paramMapList : (List<MapList>) getAssociatedObjectsForRequirement(context, objectIdsForParam)) {
            String concatParam = "";
            for (int i = 0; i < paramMapList.size(); i++) {
                Hashtable<String, String> currentParam = (Hashtable<String, String>) paramMapList.get(i);
                String currentObjectId = currentParam.get(SELECT_ID);
                
                DomainObject dmoParam = DomainObject.newInstance(context, currentObjectId);
                String paramName = dmoParam.getInfo(context, SELECT_NAME);
                String paramTitle = dmoParam.getAttributeValue(context, "Title");
                
                HashMap argsParamValue = new HashMap();
                argsParamValue.put("objectList", paramMapList);
                
                /* concatParam += "<a href=\"javascript:link('" + currentParam.get(SELECT_LEVEL) + "','"
                        + currentObjectId + "','" + currentParam.get("id[connection]") + "', '" + paramName
                        + "')\" class=\"\">" + paramTitle + "</a>" + 
                        " | " + ${CLASS:emxParameterEdit}.getParameterValue(context, JPO.packArgs(argsParamValue)).get(i) + " | " +
                        ${CLASS:emxParameterEdit}.getParameterMinValue(context, JPO.packArgs(argsParamValue)).get(i) + " | " +
                        ${CLASS:emxParameterEdit}.getParameterMaxValue(context, JPO.packArgs(argsParamValue)).get(i) +
                        "<br />";
                */
            }
            returnVector.add(concatParam);
        }
        return returnVector;
    }
    
    private List<MapList> getAssociatedObjectsForRequirement(Context context, List<String> objectIds) throws Exception {
        ArrayList<MapList> paramsList = new ArrayList();

        for (String objectId : objectIds) {
            /* HashMap<String, String> argsParam = new HashMap<String, String>();
            argsParam.put("objectId", objectId);
            String toTypeName = PlmParameterUtil.TYPE_PLMPARAMETER; 
            StringList relationships = new StringList();
            relationships.add(PlmParameterUtil.RELATIONSHIP_PARAMETER_USAGE); */
            /* relationships.add(PlmParameterUtil.RELATIONSHIP_PARAMETER_AGGREGATION);
            paramsList.add((MapList) PlmParameterConnectUtil.getConnectedObjects(context, objectId, toTypeName, relationships));
            */ 
            paramsList.add(new MapList());
        }
        return paramsList;
    }
    
    // START:lx6:IR-234604V6R2014
    public List getStatusIcon(Context context, String[] args) throws Exception {
        // unpack the arguments
        Map programMap = (HashMap) JPO.unpackArgs(args);
        List lstobjectList = (MapList) programMap.get("objectList");
        Iterator objectListItr = lstobjectList.iterator();
        // initialise the local variables
        Map objectMap = new HashMap();
        String strObjId = DomainConstants.EMPTY_STRING;
        String strObjState = DomainConstants.EMPTY_STRING;
        String strIcon = DomainConstants.EMPTY_STRING;
        // Begin of Add by Enovia MatrixOne for Bug # 312021 Date Nov 16, 2005
        String strObjPolicy = DomainConstants.EMPTY_STRING;
        String strObjPolicySymb = DomainConstants.EMPTY_STRING;
        String strObjStateSymb = DomainConstants.EMPTY_STRING;
        StringBuffer sbStatePolicyKey = new StringBuffer();
        boolean flag = false;
        // End of Add by Enovia MatrixOne for Bug # 312021 Date Nov 16, 2005
        List lstNameRev = new StringList();
        StringBuffer stbNameRev = new StringBuffer(100);
        DomainObject domObj = null;
        
        HashMap paramList = (HashMap) programMap.get("paramList");
        String strExport = (String) paramList.get("exportFormat");
        boolean toExport = false;
        if (strExport != null) 
                toExport = true;
        int iNumOfObjects = lstobjectList.size();
        String arrObjId[] = new String[iNumOfObjects];
        List columnTags = new Vector(iNumOfObjects);
        int iCount;
        // Getting the bus ids for objects in the table
        for (iCount = 0; iCount < iNumOfObjects; iCount++) {
            arrObjId[iCount] = (String) ((Map) lstobjectList.get(iCount)).get(DomainConstants.SELECT_ID);
        }
        StringList selects = new StringList();
        selects.addElement(DomainConstants.SELECT_POLICY);
        selects.addElement(DomainConstants.SELECT_CURRENT);
        MapList columnData = DomainObject.getInfo(context, arrObjId, selects);
        String denied = EnoviaResourceBundle.getProperty(context, RequirementsConstants.BUNDLE_FRAMEWORK , context.getLocale(), "emxFramework.Basic.DENIED");
        // loop through all the records
        for(int i=0;i<columnData.size();i++){
        	Map values = (Map)columnData.get(i);
            String readAccess = (String)((Map)lstobjectList.get(i)).get(RequirementsConstants.SELECT_READ_ACCESS);
            if(RequirementsConstants.DENIED.equals(readAccess)){
                lstNameRev.add(denied);
                continue;
            }
        	strObjPolicy = (String)values.get(DomainConstants.SELECT_POLICY);
        	strObjState = (String)values.get(DomainConstants.SELECT_CURRENT);
        	strObjPolicySymb = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_POLICY, strObjPolicy,
                    true);
            strObjStateSymb = FrameworkUtil.reverseLookupStateName(context, strObjPolicy, strObjState);

            // Forming the key which is to be looked up
            sbStatePolicyKey = new StringBuffer("emxRequirements.LCStatusImage.");
            sbStatePolicyKey.append(strObjPolicySymb).append(".").append(strObjStateSymb);

            // Geeting the value for the corresponding key, if not catching it
            // to set flag = false
            try {
                strIcon = EnoviaResourceBundle.getProperty(context, sbStatePolicyKey.toString());
                flag = true;
            } catch (Exception ex) {
                flag = false;
            }

            if (flag) {
                strObjState = FrameworkUtil.findAndReplace(strObjState, " ", "");
                StringBuffer sbStateKey = new StringBuffer("emxFramework.State.");
                sbStateKey.append(strObjPolicy.replaceAll(" ", "_")).append(".").append(strObjState);
                strObjState = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource",
                        context.getLocale(), sbStateKey.toString());
                
                
                if(toExport) {
                    lstNameRev.add(strObjState);
                } else {
                    stbNameRev.delete(0, stbNameRev.length());
                    stbNameRev = stbNameRev.append("<img src=\"" + strIcon).append("\" border=\"0\"  align=\"middle\" ")
                            .append("TITLE=\"").append(" ").append(strObjState).append("\"").append("/>").append("<span style=\"display:none\">" + strObjState + "</span>");
                    lstNameRev.add(stbNameRev.toString());
                }
            } else {
                lstNameRev.add(DomainConstants.EMPTY_STRING);
            }
        }
        return lstNameRev;
    }
    // END:lx6:IR-234604V6R2014
    
    /**
     * Edit access function for program or programHTMLOutput columns. 
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @return List of true/false values
     * @throws Exception if the operation fails
     * @since R2014
     */
    public List isEditableColumn(Context context, String[] args) throws Exception {
        Map programMap = (HashMap) JPO.unpackArgs(args);
        Map columnMap = (HashMap<String, String>) programMap.get("columnMap");
        MapList objectList = (MapList) programMap.get("objectList");

        boolean isBusinessColumn = columnMap.get("expression_businessobject") != null ? true : false;
        boolean isRSColumn = columnMap.get("expression_relationship") != null ? true : false;


        int iNumOfObjects = objectList.size();
        StringList editableValues = new StringList();
        for (int i = 0; i < iNumOfObjects; i++) {
            editableValues.add(true + ""); //cell editability already controlled by RowEditable flag
        }
        String currentColumnName = (String)columnMap.get("name");
        Map Settings = (Map)columnMap.get("settings");
        String columnType = (String)Settings.get("Column Type");
        //String fieldType = (String)Settings.get("Field Type");
        StringList attrTypeList = null;
        String select = null;
        if("programHTMLOutput".equalsIgnoreCase(columnType) || "program".equalsIgnoreCase(columnType)){
        	attrTypeList = getColumnAttributeTypes(context, objectList, columnMap);
        }
        // BUS object
        if (isBusinessColumn) {
            for (int i = 0; i < iNumOfObjects; i++) {
                Map<String, String> currentObjectMap = (Map<String, String>) objectList.get(i);

                if ("readonly".equalsIgnoreCase(currentObjectMap.get("ObjEditable"))) {
                	editableValues.set(i, false + ""); //regular attribute columns
                	continue;
                }
                
                if("programHTMLOutput".equalsIgnoreCase(columnType) || "program".equalsIgnoreCase(columnType)){
                	String attrType = (String)attrTypeList.get(i);
                    if (attrType == null || EMPTY_STRING.equals(attrType.trim())) {
                    	editableValues.set(i, false + "");
                    }
                }
            }
            
        // RS object
        } else if (isRSColumn) {
            for (int i = 0; i < iNumOfObjects; i++) {
                Map<String, String> currentObjectMap = (Map<String, String>) objectList.get(i);

                if("programHTMLOutput".equalsIgnoreCase(columnType) || "program".equalsIgnoreCase(columnType)){
                	String attrType = (String)attrTypeList.get(i);
                    if (attrType == null || EMPTY_STRING.equals(attrType.trim())) {
                    	editableValues.set(i, false + "");
                    }
                }
            }
        }
        return (StringList) editableValues;
    }
    
    /**
     * Check whether an attribute column is applicable to each object in the list . 
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @return List of String values; blank value indicating the attribute not applicable to that particular object
     * @throws Exception if the operation fails
     * @since R2014
     */
    StringList getColumnAttributeTypes(Context context, MapList objectList, Map columnMap) throws Exception {
        int iNumOfObjects = objectList.size();
        if (iNumOfObjects == 0) {
            return new StringList(0);
        }
        String[] ids = new String[iNumOfObjects];
        boolean boColumn = columnMap.containsKey("expression_businessobject");
        String idSelect = boColumn ? DomainConstants.SELECT_ID : DomainConstants.SELECT_RELATIONSHIP_ID;
        for (int i = 0; i < iNumOfObjects; i++) {
            ids[i] = (String) ((Map) objectList.get(i)).get(idSelect);
        }

        if (ids[0] == null || ids[0].equals("")) {
            if (iNumOfObjects == 1) {
                return new StringList("");
            } else {
                ids[0] = ids[1]; // root relId could be null;
            }
        }

        StringList attrTypeList = new StringList(iNumOfObjects);
        String select = boColumn ? (String) columnMap.get("expression_businessobject") : (String) columnMap
                .get("expression_relationship");
        StringList selects = new StringList();
        int attrind = select.indexOf("attribute[");
        if (attrind >= 0) {
        	select = select.substring(attrind, select.indexOf("]", attrind) + 1) + ".type.name";
        	selects.addElement(select);
            MapList attrTypes = null;
            if (boColumn) {
                attrTypes = DomainObject.getInfo(context, ids, selects);
            } else {
                attrTypes = DomainRelationship.getInfo(context, ids, selects);
            }
            for (int i = 0; i < iNumOfObjects; i++) {
                attrTypeList.add(((Map) attrTypes.get(i)).get(select));
            }
        } else {// assuming it's basic attribute
            for (int i = 0; i < iNumOfObjects; i++) {
                attrTypeList.add("basic");
            }
        }
        if (ids[0] == null) {
            attrTypeList.set(0, "");
        }
        return attrTypeList;
    }
    
    //START lx6 IR-239404V6R2014x STP: Incorrect information is being displayed on Lock for Edit     
    public String getUser(Context context, String[] args) throws Exception
        {
    		return context.getUser();
        }
    //END lx6 IR-239404V6R2014x STP: Incorrect information is being displayed on Lock for Edit    
    //END lx6 IR-239404V6R2014x STP: Incorrect information is being displayed on Lock for Edit
    
    
    public static boolean isGraphAvailable(Context context, String[] args) throws FrameworkException{
    	boolean isGraphAvailable = false;
    	String value = EnoviaResourceBundle.getProperty(context,"emxRequirements.solidWorksGraph.isGraphAvailable");
    	if(value != null && value.equalsIgnoreCase("true")){
    		isGraphAvailable  = true;
    	}
    	return isGraphAvailable;
    }
    
    public List isAllocStatusColumnEditable(Context context, String[] args) throws Exception {
    	Map programMap = (HashMap) JPO.unpackArgs(args);
        Map columnMap = (HashMap<String, String>) programMap.get("columnMap");
        MapList objectList = (MapList) programMap.get("objectList");
        int iNumOfObjects = objectList.size();
        StringList editableValues = new StringList();
        for (int i = 0; i < iNumOfObjects; i++) {
        	if(((String)((Map)objectList.get(i)).get("level")).equalsIgnoreCase("0")){
        		editableValues.add(false); 
        	}else{
        		editableValues.add(true);
        	}
        }
        return (StringList)editableValues;
    }
    
    public static boolean isSCEUsed(Context context, String[] args) throws Exception {
	    return RequirementsUtil.isSCEUsed(context, args);
    }
    
    public static boolean isRTFControlUsed(Context context, String[] args) throws Exception {
    	return RequirementsUtil.isRTFControlUsed(context, args);
    }
    
    public Object getFirstRevision(Context context, String[] args) throws Exception {
    	Policy policyObj = null;
    	String strPolicy = "";  
    	Map programMap = (HashMap) JPO.unpackArgs(args);
    	Map requestMap = (Map)programMap.get("requestMap");
    	String strSymbolicPolicy = (String)requestMap.get("policy");
    	strPolicy = PropertyUtil.getSchemaProperty(context, strSymbolicPolicy);
    	if(strPolicy==null||strPolicy.isEmpty()){
    		String strSymbolicType = (String)requestMap.get("type");
    		String[] parsedString = strSymbolicType.split("[,]");
    		if(parsedString.length>0){
    			strSymbolicType = parsedString[parsedString.length-1];
    		}
    		String strType = PropertyUtil.getSchemaProperty(context, strSymbolicType);  
    		strType = strType.replace(" ", "");
    		String property = "emxRequirements.Default.Creation.Default" + strType + "Policy";
    		strSymbolicPolicy = EnoviaResourceBundle.getProperty(context, property);
    		strPolicy = PropertyUtil.getSchemaProperty(context, strSymbolicPolicy);
    		if(strPolicy == null||strPolicy.isEmpty()){
    			String noPolicyProperty = "emxRequirements.Alert.NoPolicyAssociated";
    			String noPolicy = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(), "emxRequirements.Alert.NoPolicyAssociated");
    			throw new Exception(noPolicy);
    		}
    	}
    	policyObj = new Policy (strPolicy);
    	policyObj.open(context);
    	String revision = "";
    	if(policyObj.hasMajorSequence(context)&&policyObj.hasMinorSequence(context)){
    		String firstMajSeq = policyObj.getFirstInMajorSequence(context);
    		String firstMinSeq = policyObj.getFirstInMinorSequence(context);
    		String delimiter = policyObj.delimiter(context)==null?"-":policyObj.delimiter(context);
    		revision = firstMajSeq + delimiter + firstMinSeq;
    	}else if(policyObj.hasMajorSequence(context)){
    		revision = policyObj.getFirstInMajorSequence(context);
    	}else{
    		revision = policyObj.getFirstInMinorSequence(context);
    	}
    	policyObj.close(context);
	    return revision;
    }
    
    public Object getExpandLevel(Context context, String[] args) throws Exception
    {
    	HashMap programMap = (HashMap) JPO.unpackArgs(args);
    	HashMap fieldMap = (HashMap) programMap.get("fieldMap");
    	HashMap requestMap = (HashMap) programMap.get("requestMap");
		HashMap requestValuesMap = (HashMap)requestMap.get("RequestValuesMap");
		StringBuffer strBuf = new StringBuffer(256);
		// the Expand level drop down
        strBuf.append("<table><td>");
        strBuf.append("<SELECT name=\"ExpandLevel\" > <OPTION value=\"1\" SELECTED >1 <OPTION value=\"2\">2 <OPTION value=\"3\">3 <OPTION value=\"4\">4 <OPTION value=\"5\">5 <OPTION value=\"0\">"+EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(),"emxRequirements.label.ExpandAll")+" </SELECT>");
        strBuf.append("</td><td><div>&nbsp;&nbsp;&nbsp</div></td><td>");
        strBuf.append("</td></table>");
		
		String sobjectId =(String)requestMap.get("objectId1");
         if(null == sobjectId)
         {
        	 sobjectId =(String)requestMap.get("objectId");

        	 if(null == sobjectId && null!=requestValuesMap && !"null".equals(requestValuesMap))
        	 {
        		 String[] objectId1 = (String[])requestValuesMap.get("objectId");
        		 sobjectId = objectId1[0];
        	 }
         }
         
         if (null != sobjectId && !"null".equals(sobjectId))
         {
        		DomainObject domObj = DomainObject.newInstance(context, sobjectId);
                String strName =  domObj.getInfo(context,DomainObject.SELECT_NAME);
                
                strBuf.append("<input type=\"hidden\" name=\"RSP1PreloadName\" value=\""+XSSUtil.encodeForHTMLAttribute(context,strName)+"\">");
  		        strBuf.append("<input type=\"hidden\" name=\"RSP1PreloadID\" value=\""+XSSUtil.encodeForHTMLAttribute(context,sobjectId)+"\">");
 				strBuf.append("<input type=\"hidden\" name=\"RSP1NameDispOID\" value=\""+XSSUtil.encodeForHTMLAttribute(context,sobjectId)+"\">");
         }else{
        	 	strBuf.append("<input type=\"hidden\" name=\"RSP1PreloadName\" value=\""+XSSUtil.encodeForHTMLAttribute(context,"")+"\">");
		        strBuf.append("<input type=\"hidden\" name=\"RSP1PreloadID\" value=\""+XSSUtil.encodeForHTMLAttribute(context,"")+"\">");
				strBuf.append("<input type=\"hidden\" name=\"RSP1NameDispOID\" value=\""+XSSUtil.encodeForHTMLAttribute(context,"")+"\">");
         }
         
         sobjectId =(String)requestMap.get("objectId2");
         		
         if(null == sobjectId && null!=requestValuesMap && !"null".equals(requestValuesMap))
         {
              	String[] objectId2 = (String[])requestValuesMap.get("objectId2");
               	sobjectId = objectId2[0];
         }
         if(null == sobjectId && null!=requestValuesMap && !"null".equals(requestValuesMap))
         {
            	String[] objectId2 = (String[])requestValuesMap.get("objectId2");
               	sobjectId = objectId2[0];
         }
         
         if (null != sobjectId && !"null".equals(sobjectId))
         {
        		DomainObject domObj = DomainObject.newInstance(context, sobjectId);
                String strName =  domObj.getInfo(context,DomainObject.SELECT_NAME);
                
                strBuf.append("<input type=\"hidden\" name=\"RSP2PreloadName\" value=\""+XSSUtil.encodeForHTMLAttribute(context,strName)+"\">");
  		        strBuf.append("<input type=\"hidden\" name=\"RSP2PreloadID\" value=\""+XSSUtil.encodeForHTMLAttribute(context,sobjectId)+"\">");
 				strBuf.append("<input type=\"hidden\" name=\"RSP2NameDispOID\" value=\""+XSSUtil.encodeForHTMLAttribute(context,sobjectId)+"\">");
         }else{
        	 	strBuf.append("<input type=\"hidden\" name=\"RSP2PreloadName\" value=\""+XSSUtil.encodeForHTMLAttribute(context,"")+"\">");
		        strBuf.append("<input type=\"hidden\" name=\"RSP2PreloadID\" value=\""+XSSUtil.encodeForHTMLAttribute(context,"")+"\">");
				strBuf.append("<input type=\"hidden\" name=\"RSP2NameDispOID\" value=\""+XSSUtil.encodeForHTMLAttribute(context,"")+"\">");
         }
    	         
         return strBuf.toString();
    }
    
    public Object getMatchBasedOn (Context context,String[] args)throws Exception
    {
		MapList columns = null;
		Map TableMap = null;
		Map TableSettingMap = null;
		String strMBO = null;
		String sLabel = null;
		String sStrResourceValue = null;
		StringBuffer strBuf = new StringBuffer();
		StringBuffer strBuf1 = new StringBuffer();
		StringBuffer strBuf2 = new StringBuffer();
		UITableCommon uiTable = new UITableCommon();
		// to get all the table columns and assign it to map
		String sIgnoredColumnTypes = UIStructureCompare.getIgnoredColumnTypes();
		columns = uiTable.getColumns(context, PropertyUtil.getSchemaProperty(
				context, "table_RMTSpecStructureCompare"), null);
		int MapListSize = columns.size();

		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap requestMap = (HashMap) programMap.get("requestMap");

		String sobjectId = (String) requestMap.get("objectId");

		StringList strlNotCompare= FrameworkUtil.split(sIgnoredColumnTypes, ",");
		strBuf.append("<SELECT name=\"MatchBasedOn\" onchange=\"javascript:onChangeMatchBasedOn(\'MatchBasedOn\');\">");
		
		strBuf1.append("<SELECT name=\"MatchBasedOn1\" onchange=\"javascript:onChangeMatchBasedOn(\'MatchBasedOn1\');\">");
		strBuf1.append("<OPTION value=None");
		strBuf1.append(">");
		strBuf1.append("None");
		strBuf1.append("</OPTION>");
		
		strBuf2.append("<SELECT name=\"MatchBasedOn2\" disabled onchange=\"javascript:onChangeMatchBasedOn(\'MatchBasedOn2\');\">");
		strBuf2.append("<OPTION value=None");
		strBuf2.append(">");
		strBuf2.append("None");
		strBuf2.append("</OPTION>");
		
		StringList strColmJSLabelList=new StringList(MapListSize);
		StringList strColmLabelList = new StringList(MapListSize);
		for(int i=0;i<MapListSize;i++)
		{
		    Map mtemp=(Map)columns.get(i);
		    Map mSetting=(Map)mtemp.get("settings");
		    String columnName = (String) mtemp.get("label");
		    sLabel = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(),columnName);
		    String strColumnType=(String)mSetting.get("Column Type");
		    String strComparable=(String)mSetting.get("Comparable");
			if(strColumnType!=null || "".equals(strColumnType)){
				if(!strlNotCompare.contains(strColumnType)){
					if( (strComparable==null) || "".equals(strComparable) || !"false".equals(strComparable)){
						strBuf.append("<OPTION value=\"");
						// Add the display value to the string buffer
						strBuf.append(XSSUtil.encodeForHTMLAttribute(context,
								sLabel));
						strBuf.append("\">");
						strBuf.append(XSSUtil.encodeForHTMLAttribute(context,
								sLabel));
						
						strBuf.append("</OPTION>");
						
						strBuf1.append("<OPTION value=\"");
						// Add the display value to the string buffer
						strBuf1.append(XSSUtil.encodeForHTMLAttribute(context,
								sLabel));
						strBuf1.append("\">");
						strBuf1.append(XSSUtil.encodeForHTMLAttribute(context,
								sLabel));
						
						strBuf1.append("</OPTION>");
						
						strBuf2.append("<OPTION value=\"");
						// Add the display value to the string buffer
						strBuf2.append(XSSUtil.encodeForHTMLAttribute(context,
								sLabel));
						strBuf2.append("\">");
						strBuf2.append(XSSUtil.encodeForHTMLAttribute(context,
								sLabel));
						
						strBuf2.append("</OPTION>");
					}
				}
			}
			else{
				strBuf.append("<OPTION value=\"");
				// Add the display value to the string buffer
				strBuf.append(XSSUtil.encodeForHTMLAttribute(context,
						sLabel));
				strBuf.append("\">");
				strBuf.append(XSSUtil.encodeForHTMLAttribute(context,
						sLabel));
				  strBuf.append("</OPTION>");
				  
				  strBuf1.append("<OPTION value=\"");
					// Add the display value to the string buffer
					strBuf1.append(XSSUtil.encodeForHTMLAttribute(context,
							sLabel));
					strBuf1.append("\">");
					strBuf1.append(XSSUtil.encodeForHTMLAttribute(context,
							sLabel));
					
					strBuf1.append("</OPTION>");
				  
				  strBuf2.append("<OPTION value=\"");
					// Add the display value to the string buffer
					strBuf2.append(XSSUtil.encodeForHTMLAttribute(context,
							sLabel));
					strBuf2.append("\">");
					strBuf2.append(XSSUtil.encodeForHTMLAttribute(context,
							sLabel));
					
					strBuf2.append("</OPTION>");
			}
		}
		  strBuf.append("</SELECT>");
		  strBuf1.append("</SELECT>");
		  strBuf.append(strBuf1);
		  strBuf2.append("</SELECT>");
		  strBuf.append(strBuf2);
		return strBuf.toString();
	}
    
    /**
     * Gives the list of Report Differences on criteria for comparison.
     * @param context the eMatrix <code>Context</code> object.
     * @param args holds no arguments.
     * @returns an Object containing HTML output for Report Differences for comparison.
     * @throws Exception if the operation fails.
     */
 	public Object getReportDifferences(Context context, String[] args)
 			throws Exception {
 		MapList columns = null;
 		Map TableMap = null;
 		Map TableSettingMap = null;
 		String strComparable = null;
 		String strFieldLabel = null;
 		String strFieldValue = null;
 		String sVal = null;

 		StringBuilder strBuf = new StringBuilder(512);
 		UITableCommon uiTable = new UITableCommon();
         String strRMTVisualCompareTable = PropertyUtil
 		.getSchemaProperty(context,"table_RMTSpecStructureCompare");
 		columns = uiTable.getColumns(context, strRMTVisualCompareTable,
 				null);
 		int MapListSize = columns.size();
 		int cnt = 0;
 		boolean isField = false;
 	
 		String sSelectAll = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(),"emxRequirements.label.SelectAll");
        
 		if (MapListSize > 0)
 		{
 			String strFieldNameForCtrl = "";
 			strBuf.append("<table>");
 			for (int i = 0; i < MapListSize; i++)
 			{
 				isField = false;
 				TableMap = (Map) columns.get(i);
 				TableSettingMap = (Map) TableMap.get("settings");
 				strComparable = (String) TableSettingMap.get("Comparable");
 				strFieldLabel = (String) TableMap.get("label");
 				if (strFieldLabel != null && !"null".equals(strFieldLabel))
 				{
 						strFieldValue = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", context.getLocale(),strFieldLabel);
 						strFieldNameForCtrl = EnoviaResourceBundle.getProperty(context, "emxRequirementsStringResource", new Locale("en"),strFieldLabel);
 						sVal = strFieldNameForCtrl.replace(" ", "_");
 					
 						if( (strComparable==null) || "".equals(strComparable) || !"false".equals(strComparable))
	 					{
	 						cnt++;
	 						isField = true;
	 						if( i == 0)
	 						{
	 							strBuf.append("<td>&nbsp;<input type=\"checkbox\" id=\"repDiffChk\" name=\"" + sVal
		 								+ "\" value=\"true\" disabled/>"+"   " + strFieldValue+ "&nbsp;</td>");
	 						}else{
		 						strBuf.append("<td>&nbsp;<input type=\"checkbox\" id=\"repDiffChk\" name=\"" + sVal
		 								+ "\" value=\"true\"/>"+"   " + strFieldValue+ "&nbsp;</td>");
	 						}
	 					}
 				}
 				else
 				{
 					continue;
 				}

 				if(cnt >= 3 && cnt % 3 == 0 && isField)
 				{
 					strBuf.append("<tr></tr>");
 				}
 			}
 			strBuf.append("</table>");
 		}
 		strBuf.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
 		strBuf.append("<br>");

 		strBuf.append("&nbsp;<input type =\"checkbox\" name = selectAll onclick=\"javascript:selectAllOptions(\'repDiffChk\');\"> ");
 		strBuf.append(sSelectAll);
 		return strBuf.toString();
 }
}




