/*
**  MCADInstanceRepresentations
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  This JPO is used to generate data for displaying details
**  of instances (based on config) in a custom table. It provides the 
**  table column definitions and the actual cell data.
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.MCADIntegration.utils.customTable.CellData;
import com.matrixone.MCADIntegration.utils.customTable.ColumnDefinition;
import com.matrixone.MCADIntegration.utils.customTable.CustomMapList;

public class MCADInstanceRepresentations_mxJPO
{
	MCADServerResourceBundle serverResourceBundle = null;
	IEFGlobalCache cache = null;
	MCADGlobalConfigObject _gco = null;
	MCADMxUtil _util = null;
	MCADServerGeneralUtil _generalUtil = null;

	/**
	 * The no-argument constructor.
	 */
	public MCADInstanceRepresentations_mxJPO() 
	{
	}

	/**
	 * Constructor wich accepts the Matrix context and an array of String
	 * arguments.
     *
 	 * @param Context context - context for user logged in
     * @param String array
	 */
	public MCADInstanceRepresentations_mxJPO(Context context, String[] args) throws Exception
	{
	}

	/**
	 * This method generates column definitions for the table.
	 * 
	 * @param context The Matrix Context.
	 * @param args[] arguments - not used.
	 * @throws Exception.
	 */
	public Object getColumnDefinitions(Context context, String [] args) throws Exception
	{
		HashMap paramMap			=  (HashMap)JPO.unpackArgs(args);
		String langStr				=  (String)paramMap.get("languageStr");

		MCADGlobalConfigObject gcoObject = (MCADGlobalConfigObject)paramMap.get("GCOObject");

		intializeClassMembers(context, gcoObject, langStr);
		
		ArrayList  columnDefs = new ArrayList();

		ColumnDefinition representationNameColumn		= new ColumnDefinition();
		ColumnDefinition checkoutColumn					= new ColumnDefinition();

		String sMsg1	= serverResourceBundle.getString("mcadIntegration.Server.ColumnName.RepresentationName");
		
		//Initializing column for Instance Name
		representationNameColumn.setColumnTitle(sMsg1);
		representationNameColumn.setColumnKey("REPRESENTATIONNAME");
		representationNameColumn.setColumnDataType("string");
		representationNameColumn.setColumnType("label");
		representationNameColumn.setColumnIsSortable(false);

		String sMsg2	= serverResourceBundle.getString("mcadIntegration.Server.ColumnName.Checkout");
		//Initializing column for Action
		checkoutColumn.setColumnTitle(sMsg2);
		checkoutColumn.setColumnKey("CHECKOUT");
		checkoutColumn.setColumnDataType("string");
		checkoutColumn.setColumnTarget("hiddenFrame"); 
		checkoutColumn.setColumnType("icon");  
		checkoutColumn.setColumnIsSortable(false);

		columnDefs.add(representationNameColumn);
		columnDefs.add(checkoutColumn);

		return columnDefs;
	}

	/**
	 * This method generates the actual cell data for all the cells
	 * to be displayed for the instances.
     *
     * @param Context  context for user logged in
     * @param String array
     * @return Object as ArrayList
	 *
	 * This method expects following parameters to be packed in string array
     *    sBusId : busId of PART type object
     *    langStr : language info for resource bundle
     *    jsTreeID : Tree node ID of above object
	 *    instanceName : instance selected
     */
	public Object getTableData(Context context, String[] args) throws Exception
	{
		HashMap paramMap			=  (HashMap)JPO.unpackArgs(args);
		String sBusId				= (String)paramMap.get("objectId");
		String langStr				=  (String)paramMap.get("languageStr");
		String jsTreeID				=  (String)paramMap.get("jsTreeID");
		String encodedInstanceName	=  (String)paramMap.get("instanceName");
		String instanceName			= MCADUrlUtil.hexDecode(encodedInstanceName);

		MCADGlobalConfigObject gcoObject = (MCADGlobalConfigObject)paramMap.get("GCOObject");

		intializeClassMembers(context, gcoObject, langStr);

		BusinessObject bus = new BusinessObject(sBusId);
		bus.open(context);
		Vector instanceRepresentations = _generalUtil.getInstanceRepresentations(context, bus, instanceName);
		bus.close(context);

		String checkoutIconUrl	= "images/iconActionCheckOut.gif";
		String checkoutToolTip	= serverResourceBundle.getString("mcadIntegration.Server.AltText.Checkout");

		CellData cellData = null;

		CustomMapList collectionList = new CustomMapList();

		for (int i=0; i<instanceRepresentations.size(); i++)
		{
			String representationName			= (String)instanceRepresentations.elementAt(i);
			String encodedRepresentationName	= MCADUrlUtil.hexEncode(representationName);
			Map rowData = new Hashtable();

			StringBuffer checkoutHref = new StringBuffer("MCADInstanceCheckout.jsp?");
			checkoutHref.append("objectId=" + sBusId);
			checkoutHref.append("&instanceName=" + encodedInstanceName);
			checkoutHref.append("&representationName=" + encodedRepresentationName);

			if(null != representationName && !"".equals(representationName) && (representationName.indexOf(":") > -1))
			{
				representationName = representationName.substring(0, representationName.lastIndexOf(":"));
			}

			cellData = new CellData();
			cellData.setCellText(representationName);
			rowData.put("REPRESENTATIONNAME", cellData);

			cellData = new CellData();
			cellData.setIconUrl(checkoutIconUrl);
			cellData.setIconToolTip(checkoutToolTip);
			cellData.setHrefCellData(checkoutHref.toString());
			rowData.put("CHECKOUT", cellData);

			collectionList.add(rowData);
		}
		
		return collectionList;
	}

	/**
	 * This method initialized class members
	 * 
	 * @param Context context - context for user logged in
	 * @param String gcoString - global confg object in string format
	 * @param String sLanguage - language specified for resource bundle
	 */
	private void intializeClassMembers(Context context, MCADGlobalConfigObject gcoObject, String sLanguage)
	{
		serverResourceBundle = new MCADServerResourceBundle(sLanguage);
		cache				 = new IEFGlobalCache();
		_gco = gcoObject;
		_util = new MCADMxUtil(context, serverResourceBundle, cache);
		_generalUtil = new MCADServerGeneralUtil(context, _gco, serverResourceBundle, cache);
	}
}
