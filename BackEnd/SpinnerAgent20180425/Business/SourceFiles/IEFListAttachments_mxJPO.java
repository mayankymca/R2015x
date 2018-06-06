/**
 * IEFListAttachments.java
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 * This JPO returns list of all files attached to a BusinessObject
 * $Archive: $
 * $Revision: 1.3$
 * $Author: ds-kmahajan$ rahulp
 * @since AEF 9.5.2.0
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Map;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.FileList;
import matrix.db.Format;
import matrix.db.FormatList;
import matrix.db.JPO;
import matrix.db.MQLCommand;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.uicomponents.beans.IEF_ColumnDefinition;
import com.matrixone.MCADIntegration.uicomponents.util.IEF_CustomMapList;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.i18nNow;

public class IEFListAttachments_mxJPO
{
	private ArrayList columnDefs							= null;
	private MCADMxUtil util									= null;
	private IEFGlobalCache cache							= null;
	private MCADServerResourceBundle serverResourceBundle	= null;
	private String localeLanguage							= null;
	private String objectId = null;
	private String langStr  = null;

	public IEFListAttachments_mxJPO(Context context, String[] args) throws Exception
	{
		columnDefs = new ArrayList();
		//Creating 3 columns : FileName, Format, Download link
		IEF_ColumnDefinition column1 = new IEF_ColumnDefinition();
		IEF_ColumnDefinition column2 = new IEF_ColumnDefinition();
		IEF_ColumnDefinition column3 = new IEF_ColumnDefinition();

		//Initializing column for FileName
		column1.setColumnTitle("emxIEFDesignCenter.Common.FileName");
		column1.setColumnKey("FileName");
		column1.setColumnDataType("string");
		column1.setColumnType("href");
		column1.setColumnTarget("popup");

		//Initializing column for Format
		column2.setColumnTitle("emxIEFDesignCenter.Common.Format");
		column2.setColumnKey("Format");
		column2.setColumnDataType("string");
		column2.setColumnType("text");

		//Initializing column for Download link
		column3.setColumnKey("Image");
		column3.setColumnType("icon");
		column3.setColumnTarget("popup");
		column3.setColumnIsSortable(false);
		//multiple image possible with viewer integration
		column3.setIsMultipleImages(true);

		// add all the column in the list
		columnDefs.add(column1);
		columnDefs.add(column2);
		columnDefs.add(column3);
	}

	public Object getColumnDefinitions(Context context,String [] args) throws Exception
	{
		return columnDefs;
	}

	private void init(Context context, String []args)  throws Exception
	{
		HashMap paramMap =  (HashMap)JPO.unpackArgs(args);
		objectId		 =  (String)paramMap.get("objectId");
		langStr			 =  (String)paramMap.get("languageStr");

		HashMap paramList = (HashMap)paramMap.get("paramList");			
		localeLanguage				= paramMap.get("localeObj").toString();
		if(paramList != null && langStr == null)
			langStr					= (String)paramList.get("languageStr");

		serverResourceBundle		 = new MCADServerResourceBundle(langStr);
		cache						 = new IEFGlobalCache();
		util						 = new MCADMxUtil(context, serverResourceBundle, cache);
	}
@com.matrixone.apps.framework.ui.ProgramCallable
	public Object getTableData(Context context, String[] args) throws Exception
	{
		//First of all initialize the class variables.
		init(context, args);

		BusinessObject busObj = new BusinessObject(objectId);
		busObj.open(context);
		MapList attachmentList = new MapList();

		try
		{
			// get all formats
			FormatList formats = busObj.getFormats(context);
			for(int i = 0 ; i<formats.size(); i++)
			{
				String format = ((Format)formats.get(i)).getName();
				// for each formats get all files attached  each attachments represents
				// one single row in the table
				FileList list = busObj.getFiles(context,format);
				for ( int j =0; j< list.size(); j++)
				{
					matrix.db.File file = (matrix.db.File)list.get(j);
					String fileName		=file.getName();

					HashMap map = new HashMap();
					map.put("FileName", fileName);
					map.put("Format",format);
					map.put("id", objectId);
					attachmentList.add(map);
				}
			}
			// return row values each Hashtable in the list represent single row
		}
		catch( Exception ex )
		{
			attachmentList = new IEF_CustomMapList();
		}
		finally
		{
			busObj.close(context);			
		}
		return attachmentList;
	}

	public Vector getFormats(Context context, String[] args) throws Exception
	{
		Vector columnCellContentList = new Vector();

		HashMap paramMap			= (HashMap)JPO.unpackArgs(args);  
		MapList relBusObjPageList	= (MapList)paramMap.get("objectList");

		for(int i =0 ; i<relBusObjPageList.size(); i++)
		{
			String format = null;

			try
			{
				HashMap objDetails		= (HashMap)relBusObjPageList.get(i);
				String objectId			= (String)objDetails.get("id");
				format   				= (String)objDetails.get("Format");
				format					=  MCADMxUtil.getNLSName(context, "Format", format, "", "", localeLanguage);
			} 
			catch(Exception e) 
			{
				e.printStackTrace();
			}

			columnCellContentList.add(format);
		}

		return columnCellContentList;
	}

	public Vector getFileNames(Context context, String[] args) throws Exception
	{
		Vector columnCellContentList	= new Vector();
		StringBuffer htmlBuffer			= new StringBuffer();
		HashMap paramMap				= (HashMap)JPO.unpackArgs(args);  
		Map paramList					= (Map)paramMap.get("paramList");
		String reportFormat                        = (String) paramList.get("reportFormat");
		paramMap.put("displayCheckout", "true");
		paramMap.put("displayViewer", "true");

		MapList relBusObjPageList	= (MapList)paramMap.get("objectList");

		for(int i =0 ; i<relBusObjPageList.size(); i++)
		{
			String fileName = null;

			try
			{
				HashMap objDetails		= (HashMap)relBusObjPageList.get(i);
				String objectId			= (String)objDetails.get("id");
				htmlBuffer				= new StringBuffer();
				fileName				= (String) objDetails.get("FileName");

				String format			= (String)objDetails.get("format");
				String hexfileName		= MCADUrlUtil.hexEncode(fileName);

				String checkoutHref		= "../iefdesigncenter/DSCComponentCheckoutWrapper.jsp?"+ "objectId=" + objectId + "&amp;action=download" + "&amp;format=" + format + "&amp;fileName=" + hexfileName + "&amp;refresh=false&amp;";

				String checkoutToolTip	= null;
				checkoutHref			= "javascript:openWindow('"+ checkoutHref + "')";

				htmlBuffer.append(getFeatureIconContent(checkoutHref+"--","../../common/images/iconSmallAttachment.gif", "_"+fileName, checkoutToolTip));
				htmlBuffer.append(getViewerURL(context, objectId, format, fileName));
			} 
			catch(Exception e) 
			{
				e.printStackTrace();
			}
			if("CSV".equalsIgnoreCase(reportFormat)){ 
				columnCellContentList.add(fileName);
				}else{ 
			columnCellContentList.add(htmlBuffer.toString());
		}
		//	columnCellContentList.add(htmlBuffer.toString());
		}

		return columnCellContentList;
	}

	public Object getActionLinks(Context context, String[] args) throws Exception
	{
		Vector columnCellContentList	= new Vector();
		StringBuffer htmlBuffer			= new StringBuffer();
		HashMap paramMap				= (HashMap)JPO.unpackArgs(args);  

		HashMap paramList = (HashMap)paramMap.get("paramList");			

		if(paramList != null && langStr == null)
			langStr					= (String)paramList.get("languageStr");

		paramMap.put("displayCheckout", "true");
		paramMap.put("displayViewer", "true");

		MapList relBusObjPageList	= (MapList)paramMap.get("objectList");

		serverResourceBundle		= new MCADServerResourceBundle(langStr);
		cache						= new IEFGlobalCache();
		util						= new MCADMxUtil(context, serverResourceBundle, cache);

		for(int i =0 ; i<relBusObjPageList.size(); i++)
		{
			String fileName = null;

			try
			{
				HashMap objDetails		= (HashMap)relBusObjPageList.get(i);
				String objectId			= (String)objDetails.get("id");
				htmlBuffer				= new StringBuffer();
				fileName				= (String) objDetails.get("FileName");

				String format			= (String)objDetails.get("Format");
				String hexFileName		= MCADUrlUtil.hexEncode(fileName);

				String checkoutHref		= "../iefdesigncenter/DSCComponentCheckoutWrapper.jsp?"+ "objectId=" + objectId + "&amp;action=download" + "&amp;format=" + format + "&amp;fileName=" + hexFileName + "&amp;refresh=false&amp;";

				String checkoutToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.Download");
				checkoutHref			= "javascript:openWindow('"+ checkoutHref + "')";

				htmlBuffer.append(getFeatureIconContent(checkoutHref, "../../common/images/iconActionDownload.gif", "", checkoutToolTip));
				htmlBuffer.append(getViewerURL(context, objectId, format, fileName));
			} 
			catch(Exception e) 
			{
				e.printStackTrace();
			}

			columnCellContentList.add(htmlBuffer.toString());
		}

		return columnCellContentList;
	}

	protected String getFeatureIconContent(String href, String featureImage, String title, String toolTop)
	{
		StringBuffer featureIconContent = new StringBuffer();

		featureIconContent.append("<a href=\"");
		featureIconContent.append(href);
		featureIconContent.append("\" ><img src=\"images/");
		featureIconContent.append(featureImage);
		featureIconContent.append("\" border=\"0\" title=\"");
		featureIconContent.append(toolTop);
		featureIconContent.append("\"/>");
		featureIconContent.append(title);
		featureIconContent.append("</a>");


		return featureIconContent.toString();
	}

	protected String getViewerURL(Context context, String objectId, String format, String fileName)
	{
		try
		{	
			String lang				= (String)context.getSession().getLanguage();
			String sTipView			= i18nNow.getI18nString("emxTeamCentral.ContentSummary.ToolTipView", "emxTeamCentralStringResource", lang);
			StringBuffer htmlBuffer = new StringBuffer();

			// format and store all of them  in a String seperated by comma
			MQLCommand prMQL  = new MQLCommand();
			prMQL.open(context);
			prMQL.executeCommand(context,"execute program $1 $2","eServicecommonGetViewers.tcl", format);

			String sResult				= prMQL.getResult().trim();
			String error				= prMQL.getError();
			String sViewerServletName	= "";

			if( null != sResult && sResult.length() > 0)
			{
				StringTokenizer viewerTokenizer = new StringTokenizer(sResult, "|", false);	
				String sErrorCode				= "";

				if (viewerTokenizer.hasMoreTokens()) 
					sErrorCode = viewerTokenizer.nextToken();

				if (sErrorCode.equals("0"))
				{
					if (viewerTokenizer.hasMoreTokens()) 
						sViewerServletName = viewerTokenizer.nextToken();  	
					if (viewerTokenizer.hasMoreTokens()) 
						sTipView = viewerTokenizer.nextToken();  
				}
				if (sViewerServletName == null || sViewerServletName.length() == 0)
					return "";

				String sFileViewerLink	= "/servlet/" + sViewerServletName;
				String  viewerURL		= "../iefdesigncenter/emxInfoViewer.jsp?url=" +sFileViewerLink+ "&amp;id=" + objectId + "&amp;format=" + format + "&amp;file=" + MCADUrlUtil.hexEncode(fileName) + "&amp;";
				String viewerHref		= viewerURL;

				viewerHref				="javascript:openWindow('"+ viewerURL + "')";
				String url				= getFeatureIconContent(viewerHref,  "../../iefdesigncenter/images/iconActionViewer.gif",  "", sTipView+" ("+format + ")");

				htmlBuffer.append(url);
			}
			return htmlBuffer.toString();
		}
		catch (Exception e)
		{
			System.out.println("IEFListAttachments_mxJPO.getViewerURL() :" +e.getMessage());
		}

		return "";
	}
}
