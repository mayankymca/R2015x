/*
**  MCADDerivedOutputsTableData
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**	
**  This  is a JPO which act as a data source for rendering data in to a custom table .
**	Using this JPO program  developer can  create their own column definitions and can return
**	tabledata in a  CustomMapList  which stores each row of table as Map objects.
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.FileList;
import matrix.db.Format;
import matrix.db.FormatList;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.db.RelationshipItr;
import matrix.db.RelationshipList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.MCADIntegration.utils.customTable.CellData;
import com.matrixone.MCADIntegration.utils.customTable.ColumnDefinition;
import com.matrixone.MCADIntegration.utils.customTable.CustomMapList;
import com.matrixone.apps.domain.util.PropertyUtil;

public class MCADDerivedOutputsTableData_mxJPO
{
    MCADGlobalConfigObject _gco = null;
    MCADMxUtil _util = null;
    MCADServerGeneralUtil _generalUtil = null;
    String sIDDepDoc = "";
    MCADServerResourceBundle serverResourceBundle = null;
	IEFGlobalCache	cache = null;
    
    
    /**
    * This is constructor which intializes variable declared
    * @author GauravG
    * @since AEF 9.5.2.0
    */
    
    public MCADDerivedOutputsTableData_mxJPO(Context context, String[] args) throws Exception
    {
    }
    
    private void intializeClassMembers(Context context, MCADGlobalConfigObject gcoObject, String sLanguage)
    {
        serverResourceBundle = new MCADServerResourceBundle(sLanguage);
		cache				 = new IEFGlobalCache();
        _gco = gcoObject;
        _util = new MCADMxUtil(context, serverResourceBundle, cache);
        _generalUtil = new MCADServerGeneralUtil(context,_gco, serverResourceBundle, cache);
        
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
        ArrayList  columnDefs = new ArrayList();
        //Creating 3 columns : FileName, Format, Download
        ColumnDefinition column1 = new ColumnDefinition();
        ColumnDefinition column2 = new ColumnDefinition();
        ColumnDefinition column3 = new ColumnDefinition();
        ColumnDefinition column4 = new ColumnDefinition();
        ColumnDefinition column5 = new ColumnDefinition();
        
        //Initializing column for Component(program / method / wizard) name
        column1.setColumnTitle("mcadIntegration.Server.ColumnName.Name");
        column1.setColumnKey("ObjectName");
        column1.setColumnDataType("string");
        column1.setColumnType("href");
        column1.setColumnTarget("content");
        
        column2.setColumnTitle("mcadIntegration.Server.ColumnName.Revision");
        column2.setColumnKey("Revision");
        column2.setColumnDataType("string");
        column2.setColumnType("text");
        
        column3.setColumnTitle("mcadIntegration.Server.ColumnName.Type");
        column3.setColumnKey("Type");
        column3.setColumnDataType("string");
        column3.setColumnType("text");
        
        column4.setColumnTitle("mcadIntegration.Server.ColumnName.Details");
        column4.setColumnKey("ObjectDetails");
        column4.setColumnDataType("string");
        column4.setColumnTarget("popup");
        column4.setColumnType("icon");
        column4.setColumnIsSortable(false);
        
        
        column5.setColumnTitle("mcadIntegration.Server.ColumnName.FileName");
        column5.setColumnKey("FileName");
        column5.setColumnDataType("string");
        column5.setColumnType("text");
        
        columnDefs.add(column1);
        columnDefs.add(column2);
        columnDefs.add(column3);
        columnDefs.add(column4);
        columnDefs.add(column5);
        
        return columnDefs;
    }
    
    /**
    * This method returns Dependent documents table data using a CustomMapList.
    * Each row data of table is stored in Map object which in turn is stored in CustomMaplist.
    * Data Picking logic -
    * Get CAD object connected with selected PART object using "Part Specification"
    * relationship. Store the "CAD Object Name" attribute value on this relationship.
    * Now Find Dependent Document
    * object(s) connected to above CAD object using "Dependent Document Like" relationship
    * such that attribute "CAD Object Name" on this relationship matched with that on
    * Part Specification relationship between PART and CAD object. Show files in
    * picked Dependent Document objects.
    *
    * Limitations: Following method does not use icons specific toolsets but hardcodes a
    *              fixed image instead.
    *
    *             Following method does not display method-components belonging to a toolset.
    *
    * @param Context  context for user logged in
    * @param String array
    *
    * This method expects following parameters to be packed in string array
    *    sBusId : busId of PART type object
    *    langStr : language info for resource bundle
    *    jsTreeID : Tree node ID of above object
    *    gcoString : GCO object string
    *
    * @return Object as CustomMapList
    */
    
    public Object getTableData(Context context, String[] args) throws Exception
    {
        String sType = "";
        String sName = "";
        String sRev = "";
        String checkOutUrl = "";
        CustomMapList attachmentList= new CustomMapList();
        
        HashMap paramMap		= (HashMap)JPO.unpackArgs(args);
        String sBusId			= (String)paramMap.get("objectId");
        String langStr			= (String)paramMap.get("languageStr");
        String jsTreeID			= (String)paramMap.get("jsTreeID");
        String sInstanceName	= (String)paramMap.get("instanceName");
		String suiteDir			= (String)paramMap.get("emxSuiteDirectory");

		MCADGlobalConfigObject gcoObject = (MCADGlobalConfigObject)paramMap.get("GCOObject");
		if(gcoObject == null)
			return attachmentList;

        intializeClassMembers(context, gcoObject, langStr);

		String sObjectDetailsPage = getObjectDetailsPageName(paramMap);

		sBusId = _generalUtil.getValidObjctId(context, sBusId);
		if(sBusId == null || sBusId.length()==0)
			return attachmentList;
        
        BusinessObject partbus = new BusinessObject (sBusId);
        partbus.open(context);
        String type = partbus.getTypeName();
        String name = partbus.getName();
        String rev = partbus.getRevision();
        String vault = context.getVault().toString();
        String policy = partbus.getPolicy().getName();
        partbus.close(context);
        
        String detailsIconUrl		= "images/iconActionNewWindow.gif";
        String detailsToolTip		= serverResourceBundle.getString("mcadIntegration.Server.AltText.Details");
        
        String objectName = name;
		if(sInstanceName != null && !sInstanceName.trim().equals(""))
			objectName = MCADUrlUtil.hexDecode(sInstanceName);

        BusinessObjectList depDocList = null;
		Vector irrelevantFormats	  = new Vector();
		
		if(gcoObject.isCreateDependentDocObj())
		{
			depDocList = _generalUtil.getDependentDocObjects(context, partbus, MCADMxUtil.getActualNameForAEFData(context, "attribute_CADObjectName"), objectName);
		}
		else
		{
			depDocList = new BusinessObjectList(1);
			depDocList.addElement(partbus);
			
			String cadType   = _util.getAttributeForBO(context, sBusId, MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType"));
			String cadFormat = _generalUtil.getFormatsForType(context, type, cadType);
			irrelevantFormats.addElement(cadFormat);
		}

        BusinessObjectItr depDocItr = new BusinessObjectItr(depDocList);
        while(depDocItr.next())
        {
            BusinessObject busDepDoc = depDocItr.obj();
            sIDDepDoc = busDepDoc.getObjectId();
            
            FormatList formatList = busDepDoc.getFormats(context)  ;
            for(int j=0; j<formatList.size(); j++)
            {
                Format  format = (Format)formatList.elementAt(j);
                String formatName = format.getName();
                
				//if format is irrelevant, i.e., it is a cadFormat, skip it as we want
				//only those formats which have derived output files
				if(irrelevantFormats.contains(formatName))
				{
					continue;
				}

				FileList depdocfileList =  busDepDoc.getFiles(context, formatName)  ;
                for(int k=0; k<depdocfileList.size(); k++)
                {
                    matrix.db.File  depdocfile = (matrix.db.File)depdocfileList.elementAt(k);
                    String fileName = depdocfile.getName();

                    //================================================================
                    //putting collected data in attachmentList to be returned, in format
                    //expected by CustomTable.
                    sType = busDepDoc.getTypeName();
                    sName = busDepDoc.getName();
                    sRev = busDepDoc.getRevision();
                    
                    
                    Map map = new Hashtable();
                    CellData typeCellData  = new CellData();
                    typeCellData .setCellText(sType);
                    map.put("Type",typeCellData);
                    
                    checkOutUrl =  sObjectDetailsPage + "?objectId=" + sIDDepDoc +"&mode=insert&jsTreeID=" + jsTreeID + "&emxSuiteDirectory=" + suiteDir;
                    CellData nameCellData  = new CellData();
                    nameCellData .setCellText(sName);
                    nameCellData.setHrefCellData(checkOutUrl);
                    map.put("ObjectName",nameCellData);
                    
                    
                    CellData revCellData  = new CellData();
                    revCellData .setCellText(sRev);
                    map.put("Revision",revCellData);
                    
                    CellData objectDetailsCellData = new CellData();
                    objectDetailsCellData.setIconUrl(detailsIconUrl);
                    objectDetailsCellData.setIconToolTip(detailsToolTip);
                    objectDetailsCellData.setHrefCellData(checkOutUrl);
                    map.put("ObjectDetails", objectDetailsCellData);
                    
                    CellData fileCellData = new CellData();
                    fileCellData.setCellText(fileName);
                    map.put("FileName", fileCellData);
                    
                    attachmentList.add(map);
                    //================================================================
                }
            }
        }
        //}
        return attachmentList;
      }
      
      /**
      * This method finds out objects connected with "partbus" using "Part Specification
      * relationship. Also find value of attribute "CAD Object Name" on this relationship.
      * Places object and attribute value in hashmap.
      */
      public Hashtable getPartSpecsRelatedObjects(Context context, BusinessObject partbus) throws Exception
      {
          Hashtable hashCadObjectParentName = new Hashtable();
          String sPartSpecsRel = MCADMxUtil.getActualNameForAEFData(context, "policy_PartSpecification");
          //TBD - change it to CAD Object Name
          String attName = PropertyUtil.getSchemaProperty(context, "attribute_CADObjectName");
          
          try
          {
              RelationshipList relList  = _util.getFromRelationship(context, partbus, (short)0, false);
              RelationshipItr Itr		= new RelationshipItr(relList);
              while (Itr.next())
              {
                  Relationship rel = Itr.obj();
                  BusinessObject  cadObject = rel.getTo();
                  
                  String thisRelName = rel.getTypeName();
                  if(thisRelName.equals(sPartSpecsRel))
                  {
                      String attParentNameVal = _util.getRelationshipAttributeValue(context, rel , attName);
                      hashCadObjectParentName.put(cadObject, attParentNameVal);
                  }
              }
          }
          catch(Exception e)
          {
          }
          return hashCadObjectParentName;
      }

	  private String getObjectDetailsPageName(HashMap argumentsTable)
      {
          String objectDetailsPageName = "../common/emxTree.jsp";
          String sSuiteDirectory = (String) argumentsTable.get("emxSuiteDirectory");

          if(sSuiteDirectory != null && sSuiteDirectory.indexOf("infocentral") > -1)
          {
              objectDetailsPageName = "../../infocentral/emxInfoManagedMenuEmxTree.jsp";
          }
		  else if(sSuiteDirectory != null && sSuiteDirectory.indexOf("iefdesigncenter") > -1)
		  {
              objectDetailsPageName = "../common/emxTree.jsp";
		  }

          return objectDetailsPageName;
      }
}

