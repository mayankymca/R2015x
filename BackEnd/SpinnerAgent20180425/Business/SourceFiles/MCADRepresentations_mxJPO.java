/*
**  MCADRepresentations
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**	This  is a JPO which act as a data source for rendering data in to a custom table .
**	Using this JPO program  developer can  create their own column definitions and can return
**	tabledata in a  CustomMapList  which stores each row of table as Map objects. 
**	The Table data returned is the list of Representations supported by the input business object. 
**	Value of "ProE Simplified Reps" attribute on business object is the representations supported.
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

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

public class MCADRepresentations_mxJPO
{
    MCADGlobalConfigObject _gco = null;
    MCADMxUtil _util = null;
    MCADServerGeneralUtil _generalUtil = null;
    MCADServerResourceBundle serverResourceBundle = null;
	IEFGlobalCache	cache = null;

	/**
	 * Constructor which accepts the Matrix context and an array of String
	 * arguments.
     *
 	 * @param Context context - context for user logged in
     * @param String array
	 */
	public MCADRepresentations_mxJPO(Context context, String[] args) throws Exception
    {
    }

	/**
	 * This method initialized class members
	 * 
	 * @param Context context - context for user logged in
	 * @param String gcoString - global confg object in string format
	 * @param String sLanguage - language specified for resource bundle
	 */
	private void intializeClassMembers(Context _context, MCADGlobalConfigObject gcoObject, String sLanguage)
    {
        serverResourceBundle = new MCADServerResourceBundle(sLanguage);
		cache				 = new IEFGlobalCache();
        _gco = gcoObject;
        _util = new MCADMxUtil(_context, serverResourceBundle, cache);
        _generalUtil = new MCADServerGeneralUtil(_context,_gco, serverResourceBundle, cache);

    }

    /**
    * This method retutns list of column definitions by setting their column properties.
    * These columns definitions control the look & feel of the target table.
    * Following methos controls look & feel of table displayong toolsets.
    *
    * @param Context  context for user logged in
    * @param String array
    * @return Object as ArrayList
    */
	public Object getColumnDefinitions(Context context,String [] args) throws Exception
    {
	
	 HashMap paramMap =  (HashMap)JPO.unpackArgs(args);
        String sBusId			= (String)paramMap.get("objectId");
        String langStr			= (String)paramMap.get("languageStr");
        String sInstanceName	= (String)paramMap.get("instanceName");

		if(sInstanceName == null)
			sInstanceName = "";

		MCADGlobalConfigObject gcoObject = (MCADGlobalConfigObject)paramMap.get("GCOObject");

        intializeClassMembers(context, gcoObject, langStr);
	
	
        ArrayList  columnDefs = new ArrayList();
        ColumnDefinition column1 = new ColumnDefinition();
        ColumnDefinition column2 = new ColumnDefinition();

        String ColumnTitle = serverResourceBundle.getString("mcadIntegration.Server.ColumnName.RepresentationName");
        column1.setColumnTitle(ColumnTitle);
        column1.setColumnKey("SIMPLIFIEDREPS");
        column1.setColumnDataType("string");
        column1.setColumnType("text");
        column1.setColumnIsSortable(false);

        ColumnTitle = serverResourceBundle.getString("mcadIntegration.Server.ColumnName.Checkout");
        column2.setColumnTitle(ColumnTitle);
        column2.setColumnKey("CHECKOUT");
        column2.setColumnDataType("string");
        column2.setColumnTarget("hiddenFrame");
        column2.setColumnType("icon");
        column2.setColumnIsSortable(false);

        columnDefs.add(column1);
        columnDefs.add(column2);

        return columnDefs;
    }

    /**
    * This method returns Representations table data using a CustomMapList.
    * Each row data of table is stored in Map object which in turn is stored in CustomMaplist.
    * Data Picking logic -
	* Value of "ProE Simplified Reps" attribute on business object (sBusId) is the Representations in
	* the format Representation1,Representation2 etc is parsed and shown in table form.
    *
    * @param Context  context for user logged in
    * @param String array
    *
    * This method expects following parameters to be packed in string array
    *    sBusId : busId of PART type object
    *    langStr : language info for resource bundle
    *    jsTreeID : Tree node ID of above object
    *    gcoString : GCO object string
	*    instanceName : instance selected
    *
    * @return Object as CustomMapList
    */
    public Object getTableData(Context context, String[] args) throws Exception
    {
        String sSimRep = "";
        CustomMapList attachmentList= new CustomMapList();

        HashMap paramMap =  (HashMap)JPO.unpackArgs(args);
        String sBusId			= (String)paramMap.get("objectId");
        String langStr			= (String)paramMap.get("languageStr");
        String sInstanceName	= (String)paramMap.get("instanceName");

		if(sInstanceName == null)
			sInstanceName = "";

		MCADGlobalConfigObject gcoObject = (MCADGlobalConfigObject)paramMap.get("GCOObject");

        intializeClassMembers(context, gcoObject, langStr);

		sBusId = _generalUtil.getValidObjctId(context, sBusId);

        BusinessObject bus = new BusinessObject (sBusId);
        bus.open(context);
        String aefattrSimplifiedRep = MCADMxUtil.getActualNameForAEFData(context, "attribute_ProESimplifiedReps");
        String attrSimplifiedRep	= _util.getAttributeForBO(context, sBusId, aefattrSimplifiedRep);
        bus.close(context);

        String checkoutIconUrl		= "images/iconActionCheckOut.gif";
        String checkoutToolTip		= serverResourceBundle.getString("mcadIntegration.Server.AltText.Checkout");

        StringTokenizer strOneRep = new StringTokenizer(attrSimplifiedRep, ",");
        while (strOneRep.hasMoreTokens())
        {
            sSimRep = strOneRep.nextToken();			

			String encodedSimRep = MCADUrlUtil.hexEncode(sSimRep);
            StringBuffer actionHref = new StringBuffer("MCADInstanceCheckout.jsp?");
            actionHref.append("objectId=" + sBusId);
            actionHref.append("&representationName=" + encodedSimRep);

			if(null != sSimRep && !"".equals(sSimRep) && (sSimRep.indexOf(":") > -1))
			{
				sSimRep = sSimRep.substring(0, sSimRep.lastIndexOf(":"));
			}

            Map map = new Hashtable();
            CellData simRepCellData  = new CellData();
            simRepCellData .setCellText(sSimRep);
            map.put("SIMPLIFIEDREPS",simRepCellData);

            CellData checkoutCellData = new CellData();
            checkoutCellData.setIconUrl(checkoutIconUrl);
            checkoutCellData.setIconToolTip(checkoutToolTip);
            checkoutCellData.setHrefCellData(actionHref.toString());
            map.put("CHECKOUT", checkoutCellData);

            attachmentList.add(map);
        }

       return attachmentList;
      }

}

