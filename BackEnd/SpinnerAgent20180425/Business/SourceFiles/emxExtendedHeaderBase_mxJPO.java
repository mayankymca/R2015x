/*
 *  ${CLASSNAME}.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */


import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIComponent;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UIRTEUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Access;
import matrix.db.BusinessObject;
import matrix.db.Command;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MenuObject;
import matrix.db.Policy;
import matrix.db.State;
import matrix.db.StateList;
import matrix.dbutil.SelectSetting;
import matrix.util.Pattern;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainSymbolicConstants;


public class emxExtendedHeaderBase_mxJPO {

    
    public static String sColorLink = emxUtil_mxJPO.sColorLink;

    public emxExtendedHeaderBase_mxJPO(Context context, String[] args) throws Exception {}

    public StringBuilder getHeaderContents(Context context, String[] args) throws Exception {

        StringBuilder sbResults = new StringBuilder();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sLanguage = (String)programMap.get("language");
        String sOID = (String) programMap.get("objectId");
        String documentDropRelationship	= (String) programMap.get("documentDropRelationship");
        String documentCommand		= (String) programMap.get("documentCommand");
        String showStatesInHeader	= (String) programMap.get("showStatesInHeader");
        String imageDropRelationship	= (String) programMap.get("imageDropRelationship");
        String timeZone	= (String) programMap.get("timezone");
        String isFDAEnabled 		 = EnoviaResourceBundle.getProperty(context,"emxFramework.Routes.EnableFDA");
		
		String strNoImages = EnoviaResourceBundle.getProperty(context, "emxFramework.DnD.NoImages");	
        String strNoUploadKind = EnoviaResourceBundle.getProperty(context, "emxFramework.DnD.NoUploadKind");	
        String strNoUploadType = EnoviaResourceBundle.getProperty(context, "emxFramework.DnD.NoUploadType");
        
        List<String> lNoImages=Arrays.asList(strNoImages.split(","));                
        List<String> lNoUploadKind =Arrays.asList(strNoUploadKind.split(","));       
        List<String> lNoUploadType = Arrays.asList(strNoUploadType.split(","));

        if(UIUtil.isNullOrEmpty(documentDropRelationship)){
        	documentDropRelationship = "Reference Document";
        }
        if( UIUtil.isNullOrEmpty(documentCommand)){
        	documentCommand = "APPReferenceDocumentsTreeCategory";
        }

        String sMCSURL              = (String) programMap.get("MCSURL");
        DomainObject dObject        = new DomainObject(sOID);
        StringList selBUS           = new StringList();

        StringBuilder sbContentImage        = new StringBuilder();
        StringBuilder sbContentName         = new StringBuilder();
        StringBuilder sbContentDescription  = new StringBuilder();
        StringBuilder sbContentDetails      = new StringBuilder();
        StringBuilder sbContentDocuments    = new StringBuilder();
        StringBuilder sbIcon                = new StringBuilder();
        StringBuilder sbRevision            = new StringBuilder();
        StringBuilder sbRevisionTitle       = new StringBuilder();
        StringBuilder sbContentLifeCycle    = new StringBuilder();

        String sLinkPrefix          = "onClick=\"showNonModalDialog('../common/emxTree.jsp?mode=insert";
        String sLinkSuffix          = "', '950', '680', true, 'Large');\"";

        String sLabelHigherRevision = EnoviaResourceBundle.getProperty(context, "Components", "emxComponents.EngineeringChange.HigherRevisionExists", sLanguage);
        String sLabelStatus         = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Basic.Current", sLanguage);
        String sLabelOwner          = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Basic.Owner", sLanguage);
        String sLabelModified       = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Basic.Modified", sLanguage);

        selBUS.add(DomainConstants.SELECT_TYPE);
        selBUS.add("type.kindof");
        selBUS.add("type.kindof["+DomainConstants.TYPE_TASK_MANAGEMENT+"]");
        selBUS.add(DomainConstants.SELECT_NAME);
        selBUS.add(DomainConstants.SELECT_REVISION);
        selBUS.add(DomainConstants.SELECT_CURRENT);
        selBUS.add(DomainConstants.SELECT_DESCRIPTION);
        selBUS.add(DomainConstants.SELECT_MODIFIED);
        selBUS.add(DomainConstants.SELECT_OWNER);
        selBUS.add("last");
        selBUS.add("attribute["+PropertyUtil.getSchemaProperty(context,DomainSymbolicConstants.SYMBOLIC_attribute_MarketingName)+"]");
        selBUS.add("attribute["+PropertyUtil.getSchemaProperty(context,DomainSymbolicConstants.SYMBOLIC_attribute_DisplayName)+"]");
        selBUS.add("attribute[" + DomainConstants.ATTRIBUTE_TITLE + "]");
        selBUS.add(DomainConstants.SELECT_HAS_FROMCONNECT_ACCESS);
        selBUS.add(DomainConstants.SELECT_HAS_CHECKIN_ACCESS);
        selBUS.add(DomainConstants.SELECT_HAS_MODIFY_ACCESS);

        Map mData 		= dObject.getInfo(context, selBUS);
        String sType 		= (String)mData.get(DomainConstants.SELECT_TYPE);
        String sKind 		= (String)mData.get("type.kindof");
        String sKindTask	= (String)mData.get("type.kindof["+DomainConstants.TYPE_TASK_MANAGEMENT+"]");
        String sName 		= (String)mData.get(DomainConstants.SELECT_NAME);
        String sRevision 	= (String)mData.get(DomainConstants.SELECT_REVISION);
        String sDescription     = (String)mData.get(DomainConstants.SELECT_DESCRIPTION);
        String sModified        = (String)mData.get(DomainConstants.SELECT_MODIFIED);
        String sOwner = PersonUtil.getFullName(context, (String)mData.get(DomainConstants.SELECT_OWNER));
        String sLast 		= (String)mData.get("last");
        String fromConnect 		= (String)mData.get(DomainConstants.SELECT_HAS_FROMCONNECT_ACCESS);
        String checkInAccess	= (String)mData.get(DomainConstants.SELECT_HAS_CHECKIN_ACCESS);
        String modifyAccess  = (String)mData.get(DomainConstants.SELECT_HAS_MODIFY_ACCESS);
        // we show any one of these attribute "Marketing Name", "Display Name" or "Title", which will not be null in the sequence.
        String sMarketingName 	= (String)mData.get("attribute["+PropertyUtil.getSchemaProperty(context,DomainSymbolicConstants.SYMBOLIC_attribute_MarketingName)+"]");
        if(UIUtil.isNullOrEmpty(sMarketingName)) {
        	sMarketingName	= (String)mData.get("attribute["+PropertyUtil.getSchemaProperty(context,DomainSymbolicConstants.SYMBOLIC_attribute_DisplayName)+"]");
        	if(UIUtil.isNullOrEmpty(sMarketingName)){
        		sMarketingName  = (String)mData.get("attribute[" + DomainConstants.ATTRIBUTE_TITLE + "]");
        	}
        }

        if(lNoImages.indexOf(sKind) == -1) {
    		sbContentImage.append("<div class=\"headerImage\" id=\"divExtendedHeaderImage\">");
    		sbContentImage.append("<span id=\"extendedHeaderImage\">").append(genHeaderImage(context, args, sOID, sLanguage, sMCSURL, imageDropRelationship, false, modifyAccess)).append("</span>");
    		sbContentImage.append("</div>");
        }

        // REVISION details
            if(!sRevision.equals("-")) {
                if(!sRevision.equals(" ")) {
                    if(!sRevision.equals("")) {
                        sbRevision.append(" (").append(sRevision);
                        sbRevisionTitle.append(" (").append(sRevision);
                        if(!sLast.equalsIgnoreCase(sRevision)) {
                            BusinessObject boLast   = dObject.getLastRevision(context);
                            String sOIDLast         = boLast.getObjectId();
                            sbRevision.append(" <img style='vertical-align:middle;height:12px;cursor:pointer;' src='../common/images/iconSmallStatusAlert.gif' ");
                            sbRevision.append(sLinkPrefix);
                            sbRevision.append("&objectId=").append(XSSUtil.encodeForURL(context, sOIDLast));
                            sbRevision.append(sLinkSuffix);
                            sbRevision.append(" title='").append(sLabelHigherRevision).append(" : ").append(XSSUtil.encodeForHTMLAttribute(context,sLast)).append("'> ");
                        }
                        sbRevision.append(")");
                        sbRevisionTitle.append(")");
                    }
                }
            }

        // NAME
        sbIcon.append("  <img class='typeIcon' ");
        sbIcon.append(" src='../common/images/").append(UINavigatorUtil.getTypeIconProperty(context, sType)).append("' />");
        
        if(DomainConstants.TYPE_CONTROLLED_FOLDER.equalsIgnoreCase(sType)){
        	
        	sName = sMarketingName;
        	sMarketingName = DomainConstants.EMPTY_STRING;
        }
        sbContentName.append("<span title='"+ XSSUtil.encodeForHTMLAttribute(context,sName)+"' class=\"extendedHeader name\">").append(XSSUtil.encodeForHTML(context,sName)).append("</span>");
        if(UIUtil.isNotNullAndNotEmpty(sMarketingName)) {
            sbContentName.append("<span title='"+XSSUtil.encodeForHTMLAttribute(context,sMarketingName)+"' class=\"extendedHeader marketing-name\">");
            sbContentName.append(XSSUtil.encodeForHTML(context,sMarketingName));
            sbContentName.append("</span>");

        }
        if(UINavigatorUtil.isMobile(context)){
        	sbContentName.append("<br>");
        }
        String typeName = EnoviaResourceBundle.getTypeI18NString(context, sType, sLanguage);
        String typeRevisionTooltip = typeName+ sbRevisionTitle.toString();
        sbContentName.append("<span class=\"extendedHeader\">").append(sbIcon);
        sbContentName.append("<span class=\"extendedHeader type-name\" title='"+XSSUtil.encodeForHTMLAttribute(context,typeRevisionTooltip) +"'>").append(typeName).append(sbRevision.toString());
        sbContentName.append("</span>");
        sbContentName.append(genHeaderAlerts(context, sOID, sLanguage, false));
		// to not show the lifecycle in header on the basis of setting
        if(!"hide".equalsIgnoreCase(showStatesInHeader) && !("true".equals(isFDAEnabled) && DomainConstants.TYPE_INBOX_TASK.equals(sKind))){
        sbContentLifeCycle.append("<span id=\"extendedHeaderStatus\" class=\"extendedHeader state\">");

        if(!UINavigatorUtil.isMobile(context)){
        	sbContentLifeCycle.append(sLabelStatus).append(" : " ).append(genHeaderStatus(context, sOID, sLanguage, false)).append("</span>");
        }else{
        	sbContentLifeCycle.append(genHeaderStatus(context, sOID, sLanguage, false)).append("</span>");
        }
        }
        // DESCRIPTION
        if(null != sDescription) { if(!"".equals(sDescription)) {
            sbContentDescription.append("<div class=\"headerContent\" id=\"divExtendedHeaderDescription\" ");
            String sText = sDescription.replaceAll("\n", "<br/>");

            int iLast   = 0;
            int iRows   = 0;
            int iEnd    = 0;
            while(iLast != -1){
                iLast = sText.indexOf("<br/>", iLast);
                if( iLast != -1){
                    iRows ++;
                    if(iRows == 3) {
                        iEnd = iLast;
                        iLast = -1;
                    } else {
                        iLast+="<br/>".length();
                    }
                }
            }

            if(iEnd > 0) {
                sText = sText.substring(0, iEnd);
                sbContentDescription.append(" title='").append(XSSUtil.encodeForHTMLAttribute(context,sDescription)).append("'");
            }

            sbContentDescription.append("><span class=\"extendedHeader content\">");
            String sEncodedStr = XSSUtil.encodeForHTML(context,sText);
            sEncodedStr = UIRTEUtil.containsRTETags(context, sText) ? UIRTEUtil.decodeRTESupportedTags(context, sEncodedStr) : sEncodedStr;
            sbContentDescription.append(sEncodedStr);
            sbContentDescription.append("</span></div>");
        }}

	if(!UINavigatorUtil.isMobile(context)){
        sbContentDetails.append(sbContentLifeCycle.toString());
        sbContentDetails.append("<span class=\"extendedHeader\">").append(sLabelOwner).append(" : " ).append(XSSUtil.encodeForHTML(context,sOwner)).append("</span>");
        sbContentDetails.append("<span class=\"extendedHeader\">").append(sLabelModified).append(" : " );
        sbContentDetails.append("<a onclick=\"javascript:showPageHeaderContent('");
        
        double clientTZOffset = (new Double(timeZone)).doubleValue();
        sModified = eMatrixDateFormat.getFormattedDisplayDateTime(context, sModified, true, Integer.parseInt(PersonUtil.getPreferenceDateFormatString(context)), Double.parseDouble(timeZone), context.getLocale());

        // when user click on last modified link, it will open OOTB history page by executing any one of below available command in
        // category menu.
        String historyCommands = "'AEFHistory','APPDocumentHistory','APPRouteHistory'";
        String strUrl = "../common/emxHistory.jsp?HistoryMode=CurrentRevision&Header=emxFramework.Common.History&objectId=" + sOID;
        sbContentDetails.append(strUrl +"','',new Array("+ historyCommands +"));\"");
        sbContentDetails.append(" >");
        sbContentDetails.append(XSSUtil.encodeForHTML(context,sModified));
        sbContentDetails.append("</a></span>");
        }

        if(lNoUploadKind.indexOf(sKind) == -1) {
            if(lNoUploadType.indexOf(sType) == -1) {
		        if(canAttachDocument(context, sOID, sType, documentDropRelationship, sKind, sKindTask)){
		        		sbContentDocuments.append(genHeaderDocuments(context, sOID, documentDropRelationship, documentCommand, sLanguage, false));
		        }
            }
        }

        // FINAL TABLE
        if(!UINavigatorUtil.isMobile(context)){
        sbResults.append("<div id=\"divExtendedHeaderContent\" o=\"").append(sOID).append("\" dr=\"").append(documentDropRelationship).append("\" dc=\"")
		.append(documentCommand).append("\" showStates=\"").append(showStatesInHeader).append("\" idr=\"").append(imageDropRelationship).append("\" mcs=\"").append(sMCSURL).append("\">");
        
        sbResults.append(sbContentImage.toString());
        sbResults.append("<div class=\"headerContent\" id=\"divExtendedHeaderName\">").append(sbContentName.toString()).append("</div>");
        sbResults.append(sbContentDescription.toString());
        sbResults.append("<div class=\"headerContent\" id=\"divExtendedHeaderDetails\">").append(sbContentDetails.toString()).append("</div>");
        if(sbContentDocuments.length() > 0) {
            sbResults.append("<div class=\"headerContent\" id=\"divExtendedHeaderDocuments\" >").append(sbContentDocuments.toString()).append("</div>");
        }
        }else{
        	sbResults.append("<div id=\"divExtendedHeaderContent\" o=\"").append(sOID).append("\" dr=\"").append(documentDropRelationship).append("\" dc=\"")
        					.append(documentCommand).append("\" showStates=\"").append(showStatesInHeader).append("\" idr=\"").append(imageDropRelationship).append("\" mcs=\"").append(sMCSURL).append("\">");
            sbResults.append(sbContentImage.toString());
            if(sbContentDocuments.length() > 0) {
            	sbResults.append("<div class=\"headerContent\" id=\"divExtendedHeaderName\">").append(sbContentName.toString()).append(sbContentDocuments.toString()).append(sbContentLifeCycle.toString()).append("</div>");
            }else{
            	sbResults.append("<div class=\"headerContent\" id=\"divExtendedHeaderName\">").append(sbContentName.toString()).append(sbContentLifeCycle.toString()).append("</div>");
            }
        }

        sbResults.append("</div>");

        sbResults.append(genHeaderNavigationImage(context, sLanguage, sOID, documentDropRelationship,documentCommand, showStatesInHeader, imageDropRelationship, sMCSURL));
        return sbResults;

    }

    private static String genHeaderNavigationImage(Context context, String sLanguage, String sOID, String documentDropRelationship,
    		String documentCommand, String showStatesInHeader, String imageRelationship, String sMCSURL) throws Exception {

    	StringBuffer strNavImages = new StringBuffer();
    	String sPrevious = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.BackToolbarMenu.label",sLanguage);
    	String sNext = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.ForwardToolbarMenu.label",sLanguage);
    	String sRefresh = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.History.Refresh",sLanguage);
		String refreshPage = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.String.RefreshPage", new Locale(sLanguage));
		String resizeHeader = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.ResizeHeader.label",sLanguage);

    	strNavImages.append("<div id=\"divExtendedHeaderNavigation\" style='margin-right: 0px; right: 1px; position: absolute;'>");
    	strNavImages.append("<table><tr>");
    	strNavImages.append("<td><div class='field home button'><a title='' href='javascript:launchHomePage();' ><button class='home'></button></a></div></td>");
    	strNavImages.append("<td><div class='field previous button'><a title='"+sPrevious+"' href='javascript:getTopWindow().bclist.goBack();' >");
    	strNavImages.append("<button class='previous'></button></a></div></td><td><div class='field next button'><a title='"+sNext+"' href='javascript:getTopWindow().bclist.goForward();'><button class='next'></button></a></div></td>");
    	strNavImages.append("<td><div class='field refresh button'><a title='"+sRefresh+"' onclick='javascript:refreshWholeTree(event,\""+sOID+"\",\""+documentDropRelationship+"\",\""+documentCommand+"\",\""+showStatesInHeader+"\",\""+sMCSURL +"\");'>");
    	strNavImages.append("<button text='"+refreshPage+"' class='refresh'></button></a></div></td>");
    	strNavImages.append("<td><div class='field resize-Xheader button'><a id='resize-Xheader-Link' title='"+resizeHeader+"' onclick='toggleExtendedPageHeader();'><button class='resize-Xheader'></button></a></div></td>");
    	strNavImages.append("</tr></table>");
    	strNavImages.append("</div>");
    	return strNavImages.toString();
    }

    public static String genHeaderImage(Context context, String[] args, String sOID, String sLanguage, String sMCSURL, String relationship, Boolean bFromJSP, String modifyAccess) throws Exception {

    	StringBuilder sbResult  = new StringBuilder();
        StringBuilder sbImage   = new StringBuilder();
        String sURLPrimaryImage = "" ;	
		try {
			HashMap programMap = new HashMap();
			HashMap objectMap = new HashMap();
    		objectMap.put("id", sOID);
    		MapList objectList = new MapList();
    		objectList.add(objectMap);
    		programMap.put("objectList", objectList);    		
    		programMap.put("objectId", sOID);    		
    		
    		Map Imagedata = new HashMap();
    		Imagedata.put("MCSURL", sMCSURL);
    		Map requestMap = new HashMap();
    		requestMap.put("ImageData", Imagedata);    		
    		
    		programMap.put("paramList", requestMap);
    		programMap.put("format", "format_mxThumbnailImage");    		
    		programMap.put("href", sMCSURL);
			
			programMap.put(UIComponent.IMAGE_MANAGER_GENERATE_HTML_FLAG, UIComponent.FALSE);
			Vector vImageURLS = (Vector) JPO.invoke(context, "emxImageManager", null, "getImageURLs",
													JPO.packArgs(programMap), Vector.class);
			if(vImageURLS.size()>0){
				sURLPrimaryImage =(String)((Map)vImageURLS.get(0)).get("ImageURL");
				if(UIUtil.isNullOrEmpty(sURLPrimaryImage)) {
					sURLPrimaryImage = "../common/images/icon48x48ImageNotFound.gif";
				}
			}else{
				sURLPrimaryImage = "../common/images/icon48x48ImageNotFound.gif" ;
			}
		}catch(Exception e){
			//sURLPrimaryImage = ${CLASS:emxUtil}.getPrimaryImageURL(context, args, sOID, "mxThumbnail Image", sMCSURL, "");
			sURLPrimaryImage = "../common/images/icon48x48ImageNotFound.gif" ;
		}
        
        String sLabelDropImages = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.DropImagesHere", sLanguage);

        if (!sURLPrimaryImage.equals("../common/images/icon48x48ImageNotFound.gif")) {
			boolean useInPlaceManager = false;
			try{
				useInPlaceManager = "true".equalsIgnoreCase(EnoviaResourceBundle.getProperty(context, "emxFramework.InPlaceImageManager")); 
			}catch(Exception e){
			}
			if(useInPlaceManager){
	        	sbImage.append("<a href='#' onClick=\"require(['../components/emxUIImageManagerInPlace'], function(ImageManager){ new ImageManager('" +  sOID + "'); } );return false;\" >");
			}else{
	            sbImage.append("<a href='#' onClick=\"var posLeft=(screen.width/2)-(900/2);var posTop = (screen.height/2)-(650/2);window.open('");
	            sbImage.append("../components/emxImageManager.jsp?isPopup=false&toolbar=APPImageManagerToolBar&header=emxComponents.Image.ImageManagerHeading&HelpMarker=emxhelpimagesview&");
	            sbImage.append("objectId=").append(XSSUtil.encodeForURL(context,sOID));
	            sbImage.append("',  '', 'height=650,width=900,top=' + posTop + ',left=' + posLeft + ',toolbar=no,directories=no,status=no,menubar=no;return false;')\">");
			}
            sbImage.append("<img id='divDropPrimaryImage' src='").append(sURLPrimaryImage).append("' border='1' style='vertical-align:middle;border: 1px solid #bababa;box-shadow:1px 1px 2px #ccc;' height='42'></a>");
            if(UINavigatorUtil.isMobile(context)){
            	sbResult.append(sbImage.toString());
            	
            }else if("false".equalsIgnoreCase(modifyAccess)) {
            	sbResult.append(sbImage.toString());
            }
        
        }
        if((!UINavigatorUtil.isMobile(context))  && ("true".equalsIgnoreCase(modifyAccess))){
        	sbResult.append("<form id='imageUpload' action='../common/emxExtendedPageHeaderFileUploadImage.jsp?objectId=").append(sOID).append("&relationship=").append(XSSUtil.encodeForURL(context, relationship)).append("'  method='post'  enctype='multipart/form-data'>");
	        if(sbImage.length() == 0) {
	            sbResult.append("   <div id='divDropImages' class='dropArea'");
	            sbResult.append("      ondrop='ImageDrop(event, \"imageUpload\", \"divDropImages\")' ");
	            sbResult.append("  ondragover='ImageDragHover(event, \"divDropImages\")' ");
	            sbResult.append(" ondragleave='ImageDragHover(event, \"divDropImages\")' >");
	            sbResult.append(sLabelDropImages);
	        } else {
	            sbResult.append("   <div id='divDropImages' class='dropAreaWithImage'");
	            sbResult.append("      ondrop='ImageDropOnImage(event, \"imageUpload\", \"divDropImages\", \"divDropPrimaryImage\")' ");
	            sbResult.append("  ondragover='ImageDragHoverWithImage(event, \"divDropImages\", \"divDropPrimaryImage\")' ");
	            sbResult.append(" ondragleave='ImageDragHoverWithImage(event, \"divDropImages\", \"divDropPrimaryImage\")' >");
	            sbResult.append(sbImage.toString());
	        }
	        sbResult.append("   </div>");
	        sbResult.append("</form>");
        }

       return sbResult.toString();

    }
    private String genHeaderAlerts(Context context, String sOID, String sLanguage, Boolean bFromJSP) throws Exception {

        StringBuilder sbResult      = new StringBuilder();
        StringList selAlerts        = new StringList();
        DomainObject dObject        = new DomainObject(sOID);

        String sStyleHighlightIcon  = UINavigatorUtil.isMobile(context) ? "style='height:16px;'" : "style='height:11px;'";

        selAlerts.add(DomainConstants.SELECT_ID);

        String relIssue = PropertyUtil.getSchemaProperty(context,DomainSymbolicConstants.SYMBOLIC_relationship_Issue);
        String typeIssue = PropertyUtil.getSchemaProperty(context,DomainSymbolicConstants.SYMBOLIC_type_Issue);
        // Route can be connected to object with multiple relationships.
        Pattern relPattern         = null;
        relPattern  = new Pattern(DomainConstants.RELATIONSHIP_ROUTE_SCOPE);
        relPattern.addPattern(DomainObject.RELATIONSHIP_OBJECT_ROUTE);
        relPattern.addPattern(DomainObject.RELATIONSHIP_ROUTE_TASK);

        String mlPendingRoutesWhere = "";
        String  activeFilter = EnoviaResourceBundle.getProperty(context,"emxComponentsRoutes.Filter.Active");
        
        
        if(UIUtil.isNotNullAndNotEmpty(activeFilter))
        {
            StringTokenizer tokenizer=new StringTokenizer(activeFilter,",");
            while(tokenizer.hasMoreTokens())
            {
                String nextFilter=tokenizer.nextToken();
                if(!"".equals(mlPendingRoutesWhere))
                {
                	mlPendingRoutesWhere += "||";
                }
                mlPendingRoutesWhere += "attribute[Route Status] == \"" + nextFilter + "\"";
            }
        }
        
        MapList mlPendingIssues     = dObject.getRelatedObjects(context, relIssue, typeIssue, selAlerts, null, true,  false, (short)1, "current != 'Closed'", "", 0);
        MapList mlPendingChanges    = dObject.getRelatedObjects(context, DomainConstants.RELATIONSHIP_EC_AFFECTED_ITEM, "Change", selAlerts, null, true,  false, (short)1, "(current != 'Complete') && (current != 'Close') && (current != 'Reject')", "", 0);
        MapList mlPendingRoutes     = dObject.getRelatedObjects(context, relPattern.getPattern(), DomainConstants.TYPE_ROUTE,  selAlerts, null, false, true,  (short)1, mlPendingRoutesWhere, "", 0);
        
        Iterator routeObjItr = mlPendingRoutes.iterator();
        String routeId = "";
        Set pendingRouteIds = new HashSet();
        while(routeObjItr.hasNext())
        {
        	Map routeInfoMap = (Map)routeObjItr.next();
        	routeId = (String)routeInfoMap.get(DomainConstants.SELECT_ID);
        	pendingRouteIds.add(routeId);
        }
        
        // this string will contain OOTB categories command for Issues,Changes,Routes
        String categoryCommands = "";

            if(mlPendingIssues.size() > 0) {
            	categoryCommands = "'ContextIssueListPage'";
            	sbResult.append("<span id='spanExtendedHeaderAlerts' class='extendedHeader counter'>");
                String sAlertIssues = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.ExistingIssue", sLanguage);
                sbResult.append("<a ").append("title='").append(sAlertIssues).append("'");

                String sOIDIssue = "";
                if(mlPendingIssues.size() == 1) {
                    Map mIssue = (Map)mlPendingIssues.get(0);
                    sOIDIssue = (String)mIssue.get("id");
                }
                String strUrl = "../common/emxIndentedTable.jsp?selection=multiple&table=IssueList&toolbar=ContextIssueToolBar&program=emxCommonIssue:getActiveIssues&objectId="+ XSSUtil.encodeForURL(context,sOID);
                sbResult.append(" onclick=\"javascript:showPageHeaderContent('");
                sbResult.append(strUrl +"', '");
                sbResult.append(XSSUtil.encodeForJavaScript(context,sOIDIssue) +"',new Array("+categoryCommands+"));\"");
                sbResult.append(">").append(mlPendingIssues.size());
                sbResult.append("<img ").append(sStyleHighlightIcon).append(" src='../common/images/iconSmallIssue.gif'/></a>");
                sbResult.append("</span>");
            }

            if(mlPendingChanges.size() > 0) {
            	categoryCommands = "'CommonEngineeringChangeTreeCategory'";
            	sbResult.append("<span id='spanExtendedHeaderAlerts' class='extendedHeader counter'>");
                String sAlertChanges = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.ExistingChange", sLanguage);
                sbResult.append(" <a ").append("title='").append(sAlertChanges).append("'");
                String sOIDChange = "";
                if(mlPendingChanges.size() == 1) {
                    Map mIssue = (Map)mlPendingChanges.get(0);
                    sOIDChange = (String)mIssue.get("id");
                }
                String strUrl = "../common/emxIndentedTable.jsp?table=APPDashboardEC&toolbar=APPObjectECListToolBar&program=emxCommonEngineeringChange:getObjectECList&objectId="+ XSSUtil.encodeForURL(context,sOID);
                sbResult.append(" onclick=\"javascript:showPageHeaderContent('");
                sbResult.append(strUrl +"', '");
                sbResult.append(XSSUtil.encodeForJavaScript(context,sOIDChange) +"',new Array("+categoryCommands+"));\"");

                sbResult.append(">").append(mlPendingChanges.size());
                sbResult.append("<img ").append(sStyleHighlightIcon).append(" src='../common/images/iconSmallECR.gif'/></a>");
                sbResult.append("</span>");
            }

            if(mlPendingRoutes.size() > 0) {
            	categoryCommands = "'APPRoutes','TMCRoute','APPDocumentRoutes'";
            	sbResult.append("<span id='spanExtendedHeaderAlerts' class='extendedHeader counter'>");
                String sAlertRoutes = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.ExistingRoute", sLanguage);
                sbResult.append(" <a ").append("title='").append(sAlertRoutes).append("'");
                String sOIDRoute = "";
                if(pendingRouteIds.size() == 1) {
                    Map mRoute = (Map)mlPendingRoutes.get(0);
                    sOIDRoute = (String)mRoute.get("id");
                }
                String strUrl = "../common/emxIndentedTable.jsp?table=APPRouteSummary&toolbar=APPRouteSummaryToolBar&program=emxRoute:getActiveRoutes" +
                		"&suiteKey=Components&StringResourceFileId=emxComponentsStringResource&SuiteDirectory=components&selection=multiple&objectId="+XSSUtil.encodeForURL(context, sOID);
                sbResult.append(" onclick=\"javascript:showPageHeaderContent('");
                sbResult.append(strUrl +"', '");
                sbResult.append(XSSUtil.encodeForJavaScript(context, sOIDRoute) +"',new Array("+categoryCommands+"));\"");

                sbResult.append(">").append(pendingRouteIds.size());
                sbResult.append("<img ").append(sStyleHighlightIcon).append(" src='../common/images/iconSmallRoute.gif'/></a>");
                sbResult.append("</span>");
            }

        return sbResult.toString();

    }
    public String genHeaderStatus(Context context, String sOID, String sLanguage, Boolean bFromJSP) throws Exception {

        StringBuilder sbResult  = new StringBuilder();
        DomainObject dObject    = new DomainObject(sOID);
        StateList sList 	= dObject.getStates(context);
        Access access 		= dObject.getAccessMask(context);
        Boolean bAccessPromote 	= access.hasPromoteAccess();
        Boolean bAccessDemote 	= access.hasDemoteAccess();
        StringList selBUS       = new StringList();

        String sLabelPromote    = EnoviaResourceBundle.getProperty(context, "Components", "emxComponents.Button.Promote", sLanguage);
        String sLabelDemote 	= EnoviaResourceBundle.getProperty(context, "Components", "emxComponents.Button.Demote", sLanguage);

        selBUS.add("type.kindof");
        selBUS.add(DomainConstants.SELECT_POLICY);
        selBUS.add(DomainConstants.SELECT_CURRENT);

        Map mData = dObject.getInfo(context, selBUS);

        String sKind    = (String)mData.get("type.kindof");
        String sPolicy  = (String)mData.get(DomainConstants.SELECT_POLICY);
        String sCurrent = (String)mData.get(DomainConstants.SELECT_CURRENT);


        if(sKind.equals("Route")) { bAccessPromote = false; bAccessDemote = false; }

        int iCurrent = 0;
        for (int i = 0; i < sList.size(); i++) {
            State state = (State)sList.get(i);
            String sStateName = state.getName();
            if(sStateName.equals(sCurrent)) {
                iCurrent = i;
                break;
            }
        }

        if(bAccessPromote) {
            Policy policy           = dObject.getPolicy(context);
            String sSymbolicState   = FrameworkUtil.reverseLookupStateName(context, policy.getName(), sCurrent);
            MapList mlStateBlocks = dObject.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, new StringList(new String[] {"id"}), new StringList(new String[] {"attribute["+DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE+"]"}), false, true, (short)1, "(current != 'Complete') && (current != 'Archive')", "attribute["+DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE+"] == '" + sSymbolicState + "'", 1);
            if(mlStateBlocks.size() > 0) { bAccessPromote = false; }
        }

        if(bAccessDemote) {
            if(iCurrent > 0) {
                State statePrev = (State)sList.get(iCurrent - 1);
                String sStatePrev = EnoviaResourceBundle.getStateI18NString(context, sPolicy, statePrev.getName(), sLanguage);
				sbResult.append("<a href=\"javascript:lockPromoteOrDemote('../common/emxExtendedPageHeaderAction.jsp?action=demote&objectId=").append(XSSUtil.encodeForURL(context, sOID)).append("', 'hiddenFrame')\" title='").append(sLabelDemote).append("'>");
                sbResult.append("<button class='previous' type ='button'>"+ sStatePrev+" ");
                sbResult.append("<img class='lc-button-arrow' src='../common/images/utilArrowLeft.png'/>");
                sbResult.append("</button>");
                sbResult.append("</a>");
            }
        }

        sbResult.append("<span style='color:").append(sColorLink).append(";cursor:pointer;' ");
        sbResult.append(" onClick=");
        sbResult.append("\"");
        sbResult.append("var posLeft=(screen.width/2)-(475);var posTop = (screen.height/2)-(225);");
        sbResult.append(" javascript:window.open('");
        sbResult.append("../common/emxLifecycle.jsp?toolbar=AEFLifecycleMenuToolBar&header=emxFramework.Lifecycle.LifeCyclePageHeading&export=false&mode=basic&objectId=").append(XSSUtil.encodeForURL(context,sOID));
        sbResult.append("', '', 'height=450,width=950,top=' + posTop + ',left=' + posLeft + ',toolbar=no,directories=no,status=no,menubar=no;return false;')");
        sbResult.append("\"");
        sbResult.append(">");
        sbResult.append("<button class='status' type ='button'>"+EnoviaResourceBundle.getStateI18NString(context, sPolicy, sCurrent, sLanguage)+"</button>");
        sbResult.append("</span>");

        if(bAccessPromote) {
            if(iCurrent < sList.size() - 1) {
                State stateNext = (State)sList.get(iCurrent + 1);
                String sStateNext = EnoviaResourceBundle.getStateI18NString(context, sPolicy, stateNext.getName(), sLanguage);
				sbResult.append("<a href=\"javascript:lockPromoteOrDemote('../common/emxExtendedPageHeaderAction.jsp?action=promote&objectId=").append(XSSUtil.encodeForURL(context,sOID)).append("', 'hiddenFrame')\" title='").append(sLabelPromote).append("'>");
                sbResult.append("<button  class='next' type ='button'> ");
                sbResult.append("<img class='lc-button-arrow' src='../common/images/utilArrowRight.png'/>");
                sbResult.append(" " + sStateNext);
                sbResult.append("</button>");
                sbResult.append("</a>");
            }
        }
        return  sbResult.toString();

    }
    public static String genHeaderDocuments(Context context, String sOID, String sRelationship, String sCommand,  String sLanguage, Boolean bFromJSP) throws Exception {

    	StringBuilder sbResult  = new StringBuilder();
        StringList selDocuments = new StringList();
        DomainObject dObject    = new DomainObject(sOID);
        StringList selBUS       = new StringList();
        String className = UINavigatorUtil.isMobile(context) ? " document-alert" : " document-count";
        String sProjectVault     = PropertyUtil.getSchemaProperty(context,DomainObject.SYMBOLIC_type_ProjectVault);

        selBUS.add(DomainConstants.SELECT_CURRENT);
        selBUS.add(DomainConstants.SELECT_TYPE);
        selBUS.add("type.kindof");
        selBUS.add("type.kindof["+DomainConstants.TYPE_TASK_MANAGEMENT+"]");
        selBUS.add(DomainConstants.SELECT_HAS_CHECKIN_ACCESS);
        selBUS.add(DomainConstants.SELECT_HAS_FROMCONNECT_ACCESS);
        Map mData = dObject.getInfo(context, selBUS);

        String fromConnect 		= (String)mData.get(DomainConstants.SELECT_HAS_FROMCONNECT_ACCESS);
        String sCurrent         = (String) mData.get(DomainConstants.SELECT_CURRENT);
        String sKind            = (String) mData.get("type.kindof");
        String checkInAccess	= (String)mData.get(DomainConstants.SELECT_HAS_CHECKIN_ACCESS);
        String sObjType         = (String) mData.get(DomainConstants.SELECT_TYPE);
        String sHref            = "";
        String sSuite           = "Components";
        String sLabel           = "emxComponents.Command.Documents";
        String sMenu            = "";

        if(sCommand.startsWith("type_")){
        	sMenu = sCommand;
        	sCommand = "";
        	sLabel = "emxTeamCentral.DocumentSummary.Document";
        }

        if(sKind.equals("DOCUMENTS")){
        	sCommand  = "APPDocumentFiles";
        	sRelationship = PropertyUtil.getSchemaProperty(context, DomainSymbolicConstants.SYMBOLIC_relationship_ActiveVersion);
        }
        /*
        else if(sKindTask.equalsIgnoreCase("TRUE")) {
        	sCommand  = "PMCDeliverableCommandPowerView";
        	sRelationship = DomainConstants.RELATIONSHIP_TASK_DELIVERABLE;
        }*/

        // Get command or menu details
        if(!sCommand.equals("")) {
            Command command = new Command(context, sCommand);
            if(null != command) {

                sHref                   = command.getHref();
                sLabel                  = command.getLabel();
                SelectSetting setting   = command.getSettings();
                String sSuiteCommand    = setting.getValue("Registered Suite");
                String sSuiteDirTemp    = "";

                if(!sSuiteCommand.equals(""))  {
                    sSuite          = sSuiteCommand;
                    sSuiteDirTemp   = FrameworkProperties.getProperty(context, "eServiceSuite" + sSuiteCommand + ".Directory");
                }

                if(sHref.contains("${COMMON_DIR}/"))    { sHref = sHref.replace("${COMMON_DIR}/", ""); }
                if(sHref.contains("${ROOT_DIR}/"))      { sHref = sHref.replace("${ROOT_DIR}/", "../"); }
                if(sHref.contains("${SUITE_DIR}/"))     { sHref = sHref.replace("${SUITE_DIR}/", "../" + sSuiteDirTemp + "/"); }
                if(sHref.contains("${COMPONENT_DIR}/")) { sHref = sHref.replace("${COMPONENT_DIR}/", "../components/"); }

                sLabel = EnoviaResourceBundle.getProperty(context, sSuite, sLabel, sLanguage);

            }
        } else if(!sMenu.equals("")) {
            MenuObject menu = new MenuObject(context, sMenu);
            if(null != menu) {

                sHref                   = menu.getHref();
                SelectSetting setting   = menu.getSettings();
                String sSuiteCommand    = setting.getValue("Registered Suite");
                String sSuiteDirTemp    = "";

                if(!sSuiteCommand.equals(""))  {
                    sSuite          = sSuiteCommand;
                    sSuiteDirTemp   = FrameworkProperties.getProperty(context, "eServiceSuite" + sSuiteCommand + ".Directory");
                }

                if(sHref.contains("${COMMON_DIR}/"))    { sHref = sHref.replace("${COMMON_DIR}/", ""); }
                if(sHref.contains("${ROOT_DIR}/"))      { sHref = sHref.replace("${ROOT_DIR}/", "../"); }
                if(sHref.contains("${SUITE_DIR}/"))     { sHref = sHref.replace("${SUITE_DIR}/", "../" + sSuiteDirTemp + "/"); }
                if(sHref.contains("${COMPONENT_DIR}/")) { sHref = sHref.replace("${COMPONENT_DIR}/", "../components/"); }

                sLabel = EnoviaResourceBundle.getProperty(context, sSuite, sLabel, sLanguage);

            }
            sCommand = sMenu;
        }

        selDocuments.add(DomainConstants.SELECT_ID);
        selDocuments.add(DomainConstants.SELECT_TYPE);
        selDocuments.add(DomainConstants.SELECT_MODIFIED);
        selDocuments.add("attribute[" + DomainConstants.ATTRIBUTE_TITLE + "]");

        String selType     = PropertyUtil.getSchemaProperty(context,DomainObject.SYMBOLIC_type_DOCUMENTS);
        MapList mlDocuments = dObject.getRelatedObjects(context, sRelationship, selType, selDocuments, null, false, true, (short)0, "", "", 2);
		String strCount = MqlUtil.mqlCommand(context, "eval expr $1 on expand bus $2 type $3 rel $4 dump","Count TRUE",sOID,selType,sRelationship);
        int iDocsCount = Integer.parseInt(strCount);


        String sDisplay = "block";

        if(sCurrent.equals("Complete")) { sDisplay = "none"; }
        else if(sCurrent.equals("Archive")) { sDisplay = "none"; }
        else if(sCurrent.equals("Inactive")) { sDisplay = "none"; }
        if(UINavigatorUtil.isMobile(context)){
        	sDisplay = "none";
        }
        
        
        
        if("true".equalsIgnoreCase(checkInAccess)&& "true".equalsIgnoreCase(fromConnect) ){
        sbResult.append("<div id='headerDropZone' style='float:left;padding-right:5px;display:").append(sDisplay).append(";'>");
        sbResult.append("<form id='formDrag' action='../common/emxFileUpload.jsp?relationship=").append(XSSUtil.encodeForURL(context, sRelationship)).append("&documentCommand=").append(XSSUtil.encodeForURL(context, sCommand)).append("&objectId=").append(XSSUtil.encodeForURL(context, sOID)).append("'  method='post'  enctype='multipart/form-data'>\n");
        sbResult.append("   <div id='divDrag' class='dropArea' ");
        sbResult.append("      ondrop=\"FileSelectHandlerHeader(event, '" + sOID + "', 'formDrag', 'divDrag', 'divExtendedHeaderDocuments', '").append(sRelationship).append("')\" ");
        sbResult.append("  ondragover=\"FileDragHover(event, 'divDrag')\" ");
        sbResult.append(" ondragleave=\"FileDragHover(event, 'divDrag')\">\n");
        sbResult.append(EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.DropFilesHere", sLanguage));
        sbResult.append("   </div>");
        sbResult.append("</form></div>");
        }
        if(mlDocuments.size() > 0) {

            int iNumberDocuments = mlDocuments.size() > 2 ? 2 : mlDocuments.size();
            mlDocuments.sort("modified", "descending", "date");
            if(!UINavigatorUtil.isMobile(context)){
            for(int i = 0; i < iNumberDocuments; i++) {

                Map mDocument 		= (Map)mlDocuments.get(i);
                String sDocumentId 	= (String)mDocument.get(DomainConstants.SELECT_ID);
                String sDocumentType 	= (String)mDocument.get(DomainConstants.SELECT_TYPE);
                String sDocumentFile 	= (String)mDocument.get("attribute[" + DomainConstants.ATTRIBUTE_TITLE + "]");
                String sDocumentDate 	= (String)mDocument.get(DomainConstants.SELECT_MODIFIED);
                StringBuilder documentTitleAndDate  = new StringBuilder();
                documentTitleAndDate.append(sDocumentFile).append("(").append(sDocumentDate).append(")");

                sbResult.append("<span class=\"extendedHeader\" onmouseover='this.style.color=\"").append(sColorLink).append("\";' onmouseout='this.style.color=\"#000\";' ");
                sbResult.append(" onClick=\"javascript:callCheckout('").append(XSSUtil.encodeForJavaScript(context, sDocumentId)).append("',");
                sbResult.append("'download', '', '', 'null', 'null', 'structureBrowser', 'APPDocumentSummary', 'null')\">");
                sbResult.append("<img src='../common/images/").append(UINavigatorUtil.getTypeIconProperty(context, sDocumentType)).append("' />");
                sbResult.append("<span title='"+ XSSUtil.encodeForHTMLAttribute(context, documentTitleAndDate.toString()) +"' class=\"extendedHeader document-name\">"+XSSUtil.encodeForHTML(context, documentTitleAndDate.toString())+"</span>");
                sbResult.append("</span>");
            }
            }
            if(iDocsCount > 2){
            	sbResult.append("<span class=\"extendedHeader\">");
	            sbResult.append("<span title='"+XSSUtil.encodeForHTMLAttribute(context, sLabel)+"' class=\"extendedHeader" +className+"\" style='cursor:pointer;color:").append(sColorLink).append("' ");
	            sbResult.append("onclick=\"javascript:showRefDocs('").append(sCommand).append("','").append(sHref);
	            sbResult.append("&objectId=").append(XSSUtil.encodeForJavaScript(context, sOID)).append("&parentOID=").append(XSSUtil.encodeForJavaScript(context, sOID)).append("');");

	            sbResult.append("\" >");
	            sbResult.append(iDocsCount).append(" ").append(XSSUtil.encodeForHTML(context, sLabel));
	            sbResult.append("</span>");
	            sbResult.append("</span>");
		     if(sProjectVault.equals(sObjType)){
		     	     sbResult.append("<input type=\"hidden\" id=\"ext-doc-count\" name=\"ext-doc-count\" value=\""+ iDocsCount + "\"></input>");
		     }
            }else{
				if(sProjectVault.equals(sObjType)){
            	     sbResult.append("<input type=\"hidden\" id=\"ext-doc-count\" name=\"ext-doc-count\" value=\""+ iNumberDocuments + "\"></input>");
				}
            }
        }

        return sbResult.toString();

    }
    public String[] genHeaderFromJSP(Context context, String[] args) throws Exception {

        HashMap programMap  = (HashMap) JPO.unpackArgs(args);
        String sOID         = (String) programMap.get("objectId");
        String sLanguage    = (String) programMap.get("language");
        String sContents    = (String) programMap.get("content");
        String[] aResult    = new String[4];
		
		String sbContentLifeCycle="";
        if(sContents.equals("status")) {

			 String sLabelStatus  = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Basic.Current", sLanguage);
			 
			sbContentLifeCycle="<span id=\"extendedHeaderStatus\" class=\"extendedHeader state\">"+sLabelStatus+" : "+genHeaderStatus(context, sOID, sLanguage, true)+"</span>";
		
			aResult[0]              = sbContentLifeCycle;
            aResult[2]              = genHeaderAlerts(context, sOID, sLanguage, true);

            DomainObject dObject    = new DomainObject(sOID);
            String sCurrent         = dObject.getInfo(context, "current");
            String sDisplay         = "block";

            if(sCurrent.equals("Complete")) { sDisplay = "none"; }
            else if(sCurrent.equals("Archive")) {sDisplay = "none";}
            else if(sCurrent.equals("Inactive")) {sDisplay = "none";}
            else if(sCurrent.equals("Closed")) {sDisplay = "none";}

            aResult[1] = sDisplay;
            aResult[3] = aResult[2].length() > 0 ? "block" : "none";

        }

        return aResult;
    }

    private boolean canAttachDocument(Context context, String sOID, String sType, String sRelationship, String sKind, String sKindTask) throws Exception {

    	String command = "print relationship $1 select fromtype dump $2";
    	String fromTypes = MqlUtil.mqlCommand(context, command, true, sRelationship,",");
        StringList relFromTypes = FrameworkUtil.split(fromTypes, ",");

        if (relFromTypes.size() == 0){
            return false;
        }
        if( relFromTypes.indexOf(sType) > -1 || relFromTypes.indexOf(sKind) > -1 || "true".equalsIgnoreCase(sKindTask)){
        	return true;
        }
	    return false;
    }
}
