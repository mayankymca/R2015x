/*
 * ${CLASSNAME}.java
 * program migrates the data into Common Document model i.e. to 10.5 schema.
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import java.io.FileWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import matrix.db.Context;
import matrix.db.MQLCommand;
import matrix.db.RelationshipType;
import matrix.util.StringList;

import com.matrixone.apps.configuration.ConfigurationConstants;
import com.matrixone.apps.configuration.ConfigurationUtil;
import com.matrixone.apps.configuration.LogicalFeature;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.productline.ProductLineCommon;
import com.matrixone.jdom.Document;
import com.matrixone.jdom.input.SAXBuilder;
import com.matrixone.jdom.output.XMLOutputter;
import com.matrixone.util.MxXMLUtils;

/**
 * The <code>emxProductConfigurationMigrationBase</code> class contains the utilities
 * necessary to migrate the Variant Configuration schema to V6R2012x.  It must be run on all FTR data
 * created prior to V6R2012x.
 */
  public class emxProductConfigurationMigrationBase_mxJPO extends emxCommonMigration_mxJPO
  {
	public static final String SYMB_SPACE = " ";
	protected static final String OPEN_BRACE = "[";
	protected static final String CLOSE_BRACE = "]";
	public static String TYPE_CONFIGURATION_FEATURE = PropertyUtil
			.getSchemaProperty("type_ConfigurationFeature");
	protected final static String STR_EMPTY = "";
	protected final static String STR_ATTRIBUTE_OPEN_BRACE = "attribute[";
	protected final static String EXPRESSION_CLOSE = "]";
	protected final static String STR_TRUE = "TRUE";
	protected final static String STR_FALSE = "FALSE";
	protected static final String AND = "AND";
	protected static final String OR = "OR";
	protected static final String NOT = "NOT";
	private static final char OPEN_BRACE_CHAR = '(';
	private static final char CLOSE_BRACE_CHAR = ')';
	FileWriter conflictIDWriter = null;
	public boolean IS_CONFLICT = false;
	public final static String SELECT_CONFIGURATUION_STRUCTURE_OBJECTTYPE = "to["
			+ ConfigurationConstants.RELATIONSHIP_PRODUCT_CONFIGURATION
			+ "].from.from["
			+ ConfigurationConstants.RELATIONSHIP_CONFIGURATION_STRUCTURES
			+ "].to.type";
	public final static String SELECT_CONFIGURATION_STRUCTURE_OBJECTID = "to["
			+ ConfigurationConstants.RELATIONSHIP_PRODUCT_CONFIGURATION
			+ "].from.from["
			+ ConfigurationConstants.RELATIONSHIP_CONFIGURATION_STRUCTURES
			+ "].to.id";
	public final static String SELECT_CONFIGURATION_STRUCTURE_RELID = "to["
			+ ConfigurationConstants.RELATIONSHIP_PRODUCT_CONFIGURATION
			+ "].from.from["
			+ ConfigurationConstants.RELATIONSHIP_CONFIGURATION_STRUCTURES
			+ "].id";
	public final static String SELECT_SELECTED_OPTIONS_TO_OBJECTS = "from["
			+ ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS + "].to.id";
	public final static String SELECT_SELECTED_OPTIONS_RELID = "from["
			+ ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS + "].id";
	public final static String SELECT_PC_FROMTYPE = "to["
			+ ConfigurationConstants.RELATIONSHIP_PRODUCT_CONFIGURATION
			+ "].from.type";
	public final static String SELECT_PC_FROMID = "to["
			+ ConfigurationConstants.RELATIONSHIP_PRODUCT_CONFIGURATION
			+ "].from.id";
	public final static String SELECT_FPC_FROMTYPE = "to["
			+ ConfigurationConstants.RELATIONSHIP_FEATURE_PRODUCT_CONFIGURATION
			+ "].from.type";
	public final static String SELECT_FPC_FROMID = "to["
			+ ConfigurationConstants.RELATIONSHIP_FEATURE_PRODUCT_CONFIGURATION
			+ "].from.id";
	public final static String SELECT_SO_TO_LF_OBJECTID = "from["
			+ ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS + "].torel["
			+ ConfigurationConstants.RELATIONSHIP_LOGICAL_FEATURES + "].to.id";
	public final static String SELECT_SO_TO_LF_RELID = "from["
			+ ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS + "].torel["
			+ ConfigurationConstants.RELATIONSHIP_LOGICAL_FEATURES + "].id";

	public final static String SELECT_SO_TOREL_TOID = "from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].torel.to.id";
	public final static String SELECT_SO_TOREL_TOTYPE = "from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].torel.to.type";
	public final static String SELECT_SO_TOREL_TOREVISION = "from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].torel.to.revision";
	public final static String SELECT_SO_TOREL_TONAME = "from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].torel.to.name";
	public final static String SELECT_SO_TOREL_TOPHYID = "from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].torel.to.physicalid";
	public final static String SELECT_SO_ATTR_QUANTITY = "from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].attribute[" + ConfigurationConstants.ATTRIBUTE_QUANTITY + "]";
	public final static String SELECT_SO_ATTR_KEYINVAL = "from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].attribute[" + ConfigurationConstants.ATTRIBUTE_KEY_IN_VALUE+ "]";
	public final static String SELECT_SO_ATTR_FINDNUMBER = "from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].attribute[" + ConfigurationConstants.ATTRIBUTE_FIND_NUMBER + "]";
	public final static String SELECT_SO_ID = "from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"]."+DomainConstants.SELECT_RELATIONSHIP_ID;
	public final static String SELECT_SO_TOREL_ATTR_QUANTITY ="from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].torel.attribute[" + ConfigurationConstants.ATTRIBUTE_QUANTITY + "]";
	public final static String SELECT_SO_TOREL_ATTR_FINDNUMBER ="from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].torel.attribute[" + ConfigurationConstants.ATTRIBUTE_FIND_NUMBER + "]";
    public final static String SELECT_SO_TOREL_ATTR_FORCEPARTREUSE ="from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].torel.attribute[" + ConfigurationConstants.ATTRIBUTE_FORCE_PART_REUSE + "]";
    public final static String SELECT_SO_TOREL_ATTR_USAGE = "from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].torel.attribute[" + ConfigurationConstants.ATTRIBUTE_USAGE + "]";
    public final static String SELECT_SO_TOREL_ATTR_CONFIGURATIONSELECTIONCRITERIA = "from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].torel.attribute[" + ConfigurationConstants.ATTRIBUTE_CONFIGURATION_SELECTION_CRITERIA + "]";
    public final static String SELECT_SO_TOREL_ATTR_KEYINTYPE = "from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].torel.attribute[" + ConfigurationConstants.ATTRIBUTE_KEY_IN_TYPE + "]";
    public final static String SELECT_SO_TOREL_ID = "from["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"].torel.id";
	MapList pcSelectedOptionList =  null;
	  /**
      * Default constructor.
      * @param context the eMatrix <code>Context</code> object
      * @param args String array containing program arguments
      * @throws Exception if the operation fails
      */
      public emxProductConfigurationMigrationBase_mxJPO (Context context, String[] args)
              throws Exception
      {

          super(context, args);
          this._interOpCommand = "command_FTRInterOpIntermediateObjectMigration";
          this.warningLog = new FileWriter(documentDirectory + "migration.log", true);

      }
      /**
       * Main migration method to handle migrating Product Configuration.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param objectIdList StringList holds list objectids to migrate
       * @returns nothing
       * @throws Exception if the operation fails
       */
      public void  migrateObjects(Context context, StringList objectIdList)
                                                          throws Exception
      {

    	  mqlLogRequiredInformationWriter(getTimeStamp()+"In 'migrateObjects' method "+"\n");
    	  int migrationStatus = getAdminMigrationStatus(context);
    	  mqlLogRequiredInformationWriter(getTimeStamp()+"Migration Status value :: "+ migrationStatus +"\n");

          warningLog = new FileWriter(documentDirectory + "migrateObjects.log", true);
          try
          {
        	  loadResourceFile(context, "emxConfigurationMigration");
        	  mqlLogRequiredInformationWriter(getTimeStamp()+"Resource file loaded in 'migrateObjects' method ------> emxConfigurationMigration.properties"+"\n");
              StringList pcIds = new StringList(500);
              StringList objectSelects = new StringList(5);
          
              objectSelects.add(SELECT_TYPE);
              objectSelects.add(SELECT_NAME);
              objectSelects.add(SELECT_VAULT);
              objectSelects.add(SELECT_REVISION);
              objectSelects.add(SELECT_ID);

              String[] oidsArray = new String[objectIdList.size()];
              oidsArray = (String[])objectIdList.toArray(oidsArray);
              MapList mapList = DomainObject.getInfo(context, oidsArray, objectSelects);
              Iterator itr = mapList.iterator();
              Map map = new HashMap();
              String objectId = "";
              String strType = "";
              String strName = "";
              String strRev = "";
              while (itr.hasNext())
              {
                  map = (Map) itr.next();
                  objectId = (String) map.get(SELECT_ID);
                  strType = (String) map.get(SELECT_TYPE);
                  strName = (String) map.get(SELECT_NAME);
                  strRev = (String) map.get(SELECT_REVISION);
                  if (mxType.isOfParentType(context,strType, ConfigurationConstants.TYPE_PRODUCT_CONFIGURATION))
		          {
                	  pcIds.addElement(objectId);
		          }
                  else
                  {
                      //invalid type to migrate
                      mqlLogRequiredInformationWriter(getTimeStamp()+"Invalid migration object type for id= " + objectId+"\n");
                      String failureMessage = strType + "," + strName + "," + strRev + "," + "INVALID TYPE"+"\n";
                      writeUnconvertedOID(failureMessage,objectId);
                  }
              }

              //Now call the migration method based on type
              //It is not expected that all these will be run each time, most likely
              //only one of these list will be filled at a time.  The individual method checks
              //for empty list and just returns.

              	  mqlLogRequiredInformationWriter(getTimeStamp()+"Size of Product Configuration Ids  ---------------->"+ pcIds.size() + "\n\n");
            	  updatePCBOMXML(context, pcIds);
          }
          catch(Exception ex)
          {
              ex.printStackTrace();
              throw ex;
          }
      }
      /**
       * Migrates Rule related data.  Updates the cached attributes to reflect the migrated
       * Feature and GBOM schema.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param objectIdList StringList holds list objectids to migrate
       * @return nothing
       * @throws Exception if the operation fails
       */
      /**
       * Outputs the help for this migration.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param args String[] containing the command line arguments
       * @throws Exception if the operation fails
       */
       public void help(Context context, String[] args) throws Exception
      {
          if(!context.isConnected())
          {
              throw new Exception("not supported on desktop client");
          }

          writer.write("================================================================================================\n");
          writer.write(" Variant Configuration's Product Configuration Object Migration is a two step process  \n");
          writer.write(" Step1: Find all objects that need to be migrated and save them into flat files \n");
          writer.write(" Example: \n");
          writer.write(" execute program emxProductConfigurationMigrationFindObjects 1000 C:/Temp/oids/; \n");
          writer.write(" First parameter  = indicates number of object per file \n");
          writer.write(" Second Parameter  = the directory where files should be written \n");
          writer.write(" \n");
          writer.write(" Step2: Migrate the objects \n");
          writer.write(" Example: \n");
          writer.write(" execute program emxProductConfigurationMigration 'C:/Temp/oids/' 1 n ; \n");
          writer.write(" First parameter  = the directory to read the files from\n");
          writer.write(" Second Parameter = minimum range of file to start migrating  \n");
          writer.write(" Third Parameter  = maximum range of file to end migrating  \n");
          writer.write("        - value of 'n' means all the files starting from mimimum range\n");
          writer.write("================================================================================================\n");
          writer.close();
      }

	/**
	 * Sets the migration status as a property setting.
     * @param context the eMatrix <code>Context</code> object
 	 * @param strStatus String containing the status setting
	 * @throws Exception
	 */
	public void setAdminMigrationStatus(Context context,String strStatus) throws Exception
	{
		String cmd = "modify program eServiceSystemInformation.tcl property MigrationR212VariantConfiguration value "+strStatus;
		MqlUtil.mqlCommand(context, mqlCommand,  cmd);
	}

	/**
	 * Gets the migration status as an integer value.  Used to enforce an order of migration.
     * @param context the eMatrix <code>Context</code> object
 	 * @return integer representing the status
	 * @throws Exception
	 */
	public int getAdminMigrationStatus(Context context) throws Exception
	{

		String cmd = "print program eServiceSystemInformation.tcl select property[MigrationR212VariantConfiguration].value dump";
	    String result =	MqlUtil.mqlCommand(context, mqlCommand, cmd);
	    if(result.equalsIgnoreCase("UpdatePCBOMXMLInProcess"))
		{
			return 1;
		}else if(result.equalsIgnoreCase("UpdatePCBOMXMLComplete"))
		{
			return 2;
		}
	    return 0;

	}
    /**Need to call from DomainRelationship
     * Connects the specified relationship with the given relationship.
     * @param context
     * @param relationshipType
     * @param relId
     * @param strNewRelId
     * @param isFrom
     * @return
     * @throws FrameworkException
     */
    public String connectRelationship(Context context,
    		RelationshipType relationshipType,
    		java.lang.String relId, String strNewRelId,
    		boolean isFrom)throws Exception
    {
    	String fromRelId = null;
    	String toRelId = null;
    	String connId = null;
    	StringBuffer sbCmd = new StringBuffer();
    	StringBuffer sbCmd2 = new StringBuffer();
        try
        {
        	if(isFrom)
        	{
        		fromRelId = strNewRelId;
        		toRelId = relId;
        	}else
        	{
        		fromRelId = relId;
        		toRelId = strNewRelId;
        	}

        	sbCmd.append("add connection \"");
        	sbCmd.append(relationshipType);
        	sbCmd.append("\" fromrel \"");
        	sbCmd.append(fromRelId);
        	sbCmd.append("\" torel \"");
        	sbCmd.append(toRelId);
        	sbCmd.append("\" select id dump;");

        	sbCmd2.append("add connection ");
        	sbCmd2.append("$1");
        	sbCmd2.append(" fromrel ");
        	sbCmd2.append("$2");
        	sbCmd2.append(" torel ");
        	sbCmd2.append("$3");
        	sbCmd2.append(" select $4 dump");
        	
        	mqlLogRequiredInformationWriter(getTimeStamp()+"MQL command to be executed ::" + "\n" + sbCmd.toString()+ "\n");
        	//connId = MqlUtil.mqlCommand(context, sbCmd.toString(), true);
        	connId = MqlUtil.mqlCommand(context, sbCmd2.toString(), true,relationshipType.getName(),fromRelId,toRelId,"id");
        }
        catch (Exception e)
        {
        	mqlLogRequiredInformationWriter(getTimeStamp()+"MQL command execution failed in 'connectRelationship' API ::" + sbCmd.toString()+ "\n");
        	e.printStackTrace();
            throw new FrameworkException(e);
        }
		return connId;
    }


    /** Need to call from DomainRelationship
     * Connects the specified relationship with the given Object.
     * @param context
     * @param relationshipType
     * @param objectId
     * @param strNewRelId
     * @param isFrom
     * @return
     * @throws FrameworkException
     */
    public String connectObject(Context context,
    		RelationshipType relationshipType,
    		java.lang.String objectId,String strNewRelId,
    		boolean isFrom)throws Exception
    {
    	String fromId = null;
    	String toId = null;
    	String connId = null;
    	StringBuffer sbCmd = new StringBuffer();
    	StringBuffer sbCmd2 = new StringBuffer();
    	String strFrom = null;
    	String strTo = null;

        try
        {
        	if(!isFrom)
        	{
        		fromId = strNewRelId;
        		toId = objectId;
        		strFrom = "fromrel";
        		strTo = "to";
        	}else
        	{
        		fromId = objectId;
        		toId = strNewRelId;
        		strFrom = "from";
        		strTo = "torel";
        	}

        	sbCmd.append("add connection \"");
        	sbCmd.append(relationshipType);
        	sbCmd.append("\" ");
        	sbCmd.append(strFrom);
        	sbCmd.append(" \"");
        	sbCmd.append(fromId);
        	sbCmd.append("\" ");
        	sbCmd.append(strTo);
        	sbCmd.append(" \"");
        	sbCmd.append(toId);
        	sbCmd.append("\" select id dump;");
        	
        	sbCmd2.append("add connection ");
        	sbCmd2.append("$1");
        	sbCmd2.append(" ");
        	sbCmd2.append(strFrom);
        	sbCmd2.append(" ");
        	sbCmd2.append("$2");
        	sbCmd2.append(" ");
        	sbCmd2.append(strTo);
        	sbCmd2.append(" ");
        	sbCmd2.append("$3");
        	sbCmd2.append(" select $4 dump");

        	mqlLogRequiredInformationWriter(getTimeStamp()+"MQL command to be executed ::" + "\n" + sbCmd.toString()+ "\n");
        	//connId = MqlUtil.mqlCommand(context, sbCmd.toString(), true);
        	connId = MqlUtil.mqlCommand(context, sbCmd2.toString(), true,relationshipType.getName(),fromId,toId,"id");

        }
        catch (Exception e)
        {
        	mqlLogRequiredInformationWriter(getTimeStamp()+"MQL command execution failed in 'connectObject' API ::" + sbCmd.toString()+ "\n");
        	e.printStackTrace();
            throw new FrameworkException(e);
        }
		return connId;
    }
    private void  updatePCBOMXML(Context context, StringList objectIdList)
                                                        throws Exception
    {
       try
       {
    	  //set migration status
      		mqlLogRequiredInformationWriter(getTimeStamp()+"Update Product Configuration BOM XML In Process \n");

     		//STEP1 - turn triggers off
    		  String strMqlCommand = "trigger off";
    		  MqlUtil.mqlCommand(context,strMqlCommand,true);

    		  setAdminMigrationStatus(context,"updatePCBOMXMLInProcess");

           	  	 String[] oidsArray = new String[objectIdList.size()];
                 oidsArray = (String[])objectIdList.toArray(oidsArray);

                 StringList objectSelects = new StringList();
                 objectSelects.addElement(SELECT_TYPE);
                 objectSelects.addElement(SELECT_NAME);
                 objectSelects.addElement(SELECT_REVISION);
                 objectSelects.addElement(SELECT_ID);

                 objectSelects.addElement(SELECT_PC_FROMID);
                 objectSelects.addElement(SELECT_PC_FROMTYPE);

                 objectSelects.addElement(SELECT_CONFIGURATUION_STRUCTURE_OBJECTTYPE);
                 objectSelects.addElement(SELECT_CONFIGURATION_STRUCTURE_OBJECTID);
                 objectSelects.addElement(SELECT_CONFIGURATION_STRUCTURE_RELID);

                 objectSelects.addElement(SELECT_SELECTED_OPTIONS_TO_OBJECTS);
                 objectSelects.addElement(SELECT_SELECTED_OPTIONS_RELID);


                 DomainConstants.MULTI_VALUE_LIST.add(SELECT_CONFIGURATUION_STRUCTURE_OBJECTTYPE);
                 DomainConstants.MULTI_VALUE_LIST.add(SELECT_CONFIGURATION_STRUCTURE_OBJECTID);
                 DomainConstants.MULTI_VALUE_LIST.add(SELECT_CONFIGURATION_STRUCTURE_RELID);
                 DomainConstants.MULTI_VALUE_LIST.add(SELECT_SELECTED_OPTIONS_TO_OBJECTS);
                 DomainConstants.MULTI_VALUE_LIST.add(SELECT_SELECTED_OPTIONS_RELID);


                 MapList mapList = DomainObject.getInfo(context, oidsArray, objectSelects);


                 DomainConstants.MULTI_VALUE_LIST.remove(SELECT_CONFIGURATUION_STRUCTURE_OBJECTTYPE);
                 DomainConstants.MULTI_VALUE_LIST.remove(SELECT_CONFIGURATION_STRUCTURE_OBJECTID);
                 DomainConstants.MULTI_VALUE_LIST.remove(SELECT_CONFIGURATION_STRUCTURE_RELID);
                 DomainConstants.MULTI_VALUE_LIST.remove(SELECT_SELECTED_OPTIONS_TO_OBJECTS);
                 DomainConstants.MULTI_VALUE_LIST.remove(SELECT_SELECTED_OPTIONS_RELID);



                 Iterator itr = mapList.iterator();
                 Map pcDataMap = new HashMap();
                 String strPCId = "";
                 String strContextId = "";
                 String strContextType = "";

                 while (itr.hasNext())
                 {
                	 pcDataMap = (Map) itr.next();
                  strPCId =  (String)pcDataMap.get(SELECT_ID);

                  mqlLogRequiredInformationWriter(getTimeStamp()+"Product Configuration id in process :"+ strPCId + "\n");

                  clearSelectedOptions();
                  strContextId = (String)pcDataMap.get(SELECT_PC_FROMID);
                  strContextType = (String)pcDataMap.get(SELECT_PC_FROMTYPE);
                 
                  
                  //Need to create new "Selected Options" relationship between PC and "CONFIGURATON STRUCTURES" relationship for selected CF in PC
                  Object objCFIds =  pcDataMap.get(SELECT_CONFIGURATION_STRUCTURE_OBJECTID);
                  StringList sLCFIds = new StringList();
                  if (objCFIds instanceof StringList) {
                	  sLCFIds = (StringList)pcDataMap.get(SELECT_CONFIGURATION_STRUCTURE_OBJECTID);
					} else if (objCFIds instanceof String) {
					  sLCFIds.addElement((String)pcDataMap.get(SELECT_CONFIGURATION_STRUCTURE_OBJECTID));
					}


                  Object objConfigStructureRelIds =  pcDataMap.get(SELECT_CONFIGURATION_STRUCTURE_RELID);
                  StringList sLConfigStructureRelIds = new StringList();
                  if (objConfigStructureRelIds instanceof StringList) {
                	  sLConfigStructureRelIds = (StringList)pcDataMap.get(SELECT_CONFIGURATION_STRUCTURE_RELID);
					} else if (objConfigStructureRelIds instanceof String) {
						sLConfigStructureRelIds.addElement((String)pcDataMap.get(SELECT_CONFIGURATION_STRUCTURE_RELID));
					}


                  Object objSOCFds =  pcDataMap.get(SELECT_SELECTED_OPTIONS_TO_OBJECTS);
                  StringList sLSOCFds = new StringList();
                  if (objSOCFds instanceof StringList) {
                	  sLSOCFds = (StringList)pcDataMap.get(SELECT_SELECTED_OPTIONS_TO_OBJECTS);
					} else if (objSOCFds instanceof String) {
						sLSOCFds.addElement((String)pcDataMap.get(SELECT_SELECTED_OPTIONS_TO_OBJECTS));
					}


                  Object objSORelds =  pcDataMap.get(SELECT_SELECTED_OPTIONS_RELID);
                  StringList sLSORelds = new StringList();
                  if (objSORelds instanceof StringList) {
                	  sLSORelds = (StringList)pcDataMap.get(SELECT_SELECTED_OPTIONS_RELID);
					} else if (objSORelds instanceof String) {
					  sLSORelds.addElement((String)pcDataMap.get(SELECT_SELECTED_OPTIONS_RELID));
					}
                  StringList sLSOToDel = new StringList();
                  for(int i=0;i<sLCFIds.size();i++){

                	  String strCFId = (String)sLCFIds.get(i);
                	  if(sLSOCFds.contains(strCFId)){
                		  //CF is used in PC, so get the "Varies By" relid
                		  String strConfigStructureRelId = (String)sLConfigStructureRelIds.get(i);

                		  //Do connection between PC and Varies by rel id
                		  String strRel = ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS;
 						  RelationshipType strRelType = new RelationshipType(strRel);
                		  connectObject(context,
                				  		strRelType ,
	                		    		strPCId,
	                		    		strConfigStructureRelId,
	                		    		true);

                		  String strSOToSel = (String)sLSORelds.get(i);
                    	  sLSOToDel.add(strSOToSel);
                	  }
                  }
                      
                  
                  // For Assembly Config
                  if(mxType.isOfParentType(context, strContextType, ConfigurationConstants.TYPE_LOGICAL_FEATURE)){
                	 // String strVariesBy= MqlUtil.mqlCommand(context, "print bus "+strContextId+" select from["+ConfigurationConstants.RELATIONSHIP_VARIES_BY+"].id dump |");
                	  String strMQLCommand="print bus $1 select $2 dump $3";
                      String strVariesBy = MqlUtil.mqlCommand(context,strMQLCommand,strContextId,"from["+ ConfigurationConstants.RELATIONSHIP_VARIES_BY +"].id","|");
                	  StringTokenizer tokens= new StringTokenizer(strVariesBy,"|");
                	  while(tokens.hasMoreTokens()){
                		  String strVariesById=tokens.nextToken();
                		  // MqlUtil.mqlCommand(context, "add connection "+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+" from "+strPCId+" torel "+strVariesById);

                		  //Connect PC and Varies by rel id
                		  String strRel = ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS;
                		  RelationshipType strRelType = new RelationshipType(strRel);
                		  connectObject(context,
                				  strRelType ,
                				  strPCId,
                				  strVariesById,
                				  true);
                	  }
                	  // delete the selected option to CF objects
                	  StringList relSelect= new StringList("to.type");

                	  String[] strArr = new String[ sLSORelds.size()];
                	  strArr = (String []) sLSORelds.toArray(strArr);

                	  MapList mpLstSelectOpt=DomainRelationship.getInfo(context,strArr,relSelect);
                	  for(int j=0;j<mpLstSelectOpt.size();j++){
                		  Map temp = (Map)mpLstSelectOpt.get(j);
                		  if(temp.containsKey("to.type") && mxType.isOfParentType(context,(temp.get("to.type").toString()),ConfigurationConstants.TYPE_CONFIGURATION_FEATURE)){
                			  sLSOToDel.add(strArr[j]); 
                		  }
                	  }



                  }
                  //Need to delete the connection "Selected Options" present in R211 which is having "To Side" as Object(CF/CO)
                  if(!sLSOToDel.isEmpty()){
                	  String[] strArr = new String[sLSOToDel.size()];
                	  strArr = (String [])sLSOToDel.toArray(strArr);
                	  DomainRelationship.disconnect(context, strArr);
                	  mqlLogRequiredInformationWriter(getTimeStamp()+"Incorrect 'Selected Options' relationship from R211 data deleted. \n");
                  }

                  mqlLogRequiredInformationWriter(getTimeStamp()+"Update BOM XML For ProductConfiguration started."+"\n");
                  
                  //Note : The BOM XML format in V6R2014x in different and so the below code is changed
                  DomainObject domObject = DomainObject.newInstance(context, strPCId);
                  domObject.setAttributeValue(context,ConfigurationConstants.ATTRIBUTE_BOMXML,"");
                  //updateBOMXMLForProductConfiguration(context,strPCId);
                  
                  mqlLogRequiredInformationWriter(getTimeStamp()+"Update BOM XML For ProductConfiguration ended."+"\n");
                  mqlLogRequiredInformationWriter(getTimeStamp()+"Migration for Product Configuration id :"+ strPCId + " done \n\n\n");
                 }
                 setAdminMigrationStatus(context,"UpdatePCBOMXMLComplete");
                 mqlLogRequiredInformationWriter(getTimeStamp()+"Update Product Configuration BOM XML is completed. \n");
                 mqlLogRequiredInformationWriter("\n");

      	 }catch (Exception e) {
      		e.printStackTrace();
  			mqlLogRequiredInformationWriter(getTimeStamp()+"Update Product Configuration BOM XML failed. \n"+e.getMessage() + "\n");
		}
        return;
    }


    String getSchemaProperty(Context context,String sSymbolicName) throws Exception{

    	//String strResults = MqlUtil.mqlCommand(context,"print program eServiceSchemaVariableMapping.tcl select property["+ sSymbolicName +"] dump |;");
    	String strMQLCommand="print program $1 select $2 dump $3";
		String strResults = MqlUtil.mqlCommand(context,strMQLCommand,"eServiceSchemaVariableMapping.tcl","property["+ sSymbolicName +"]","|");

        StringTokenizer token = new StringTokenizer(strResults,"|");
        String val = null;
        while (token.hasMoreTokens()){
            String preParse = token.nextToken();

		    //property returned as 'relationsip_xyz to relationship xyz'
		    int toIndex = preParse.indexOf(" to ");
		    if (toIndex > -1){

			//split on " to "
			val = preParse.substring(toIndex+4,preParse.length());
			if (val != null){
			    val.trim();

			    //split on space and place result in hashtable
			    val = val.substring(val.indexOf(' ')+1,val.length());
			 }
		    }
		}
    	return val;
    }

    /**
     * This method prepares element related to Usage and Quantity for BOM XML
     * @param context
     * @param strFeatureId
     * @param strUsage
     * @param strQuantity
     * @return
     * @throws Exception
     */
    	private String getUsageQuantityElementBOMXML(Context context,Map usageQuantityElementMap,String objectId) throws Exception
        {
    		mqlLogRequiredInformationWriter(getTimeStamp()+"Inside Usage Quantity Element BOMXML update ::" + usageQuantityElementMap +"\n");
        	StringBuffer sbXML = new StringBuffer(100);
        	String strFeatureId = (String)usageQuantityElementMap.get("FeatureId");
        	mqlLogRequiredInformationWriter(getTimeStamp()+"Feature Id ::" + strFeatureId +"\n");
        	String strUsage = (String)usageQuantityElementMap.get("Usage");
        	mqlLogRequiredInformationWriter(getTimeStamp()+"Usage value ::" + strUsage +"\n");
        	String strQuantity = (String)usageQuantityElementMap.get("Quantity");
        	mqlLogRequiredInformationWriter(getTimeStamp()+"Quantity value ::" + strQuantity +"\n");

        	if (strUsage.equalsIgnoreCase(ConfigurationConstants.RANGE_VALUE_AS_REQUIRED))
            {
                MapList allEvaluatedQuantityRules = getEvaluatedQuantityRules(context, new DomainObject(strFeatureId),objectId);
                String finalQuantity = "";
                if (allEvaluatedQuantityRules != null && (allEvaluatedQuantityRules.size() > 0))
                {
                    double totalQuantity = 0.0;
                    for (int j = 0; j < allEvaluatedQuantityRules.size(); j++)
                    {
                        HashMap evaluatedQuantityRule = (HashMap) allEvaluatedQuantityRules.get(j);
                        String strQty = (String) evaluatedQuantityRule.get("quantity");
                        double quantity = Double.parseDouble(strQty);
                        totalQuantity = totalQuantity + quantity;
                        // if quantity is -ve then check from resolved parts
                        if (totalQuantity < 0)
                        {
                            finalQuantity = String.valueOf(totalQuantity);
                        }
                        // computed quantity is not -ve so display calculated quantity
                        else
                        {
                            finalQuantity = String.valueOf(totalQuantity);
                        }
                    }
                    strQuantity = finalQuantity;
                }
            }

        	mqlLogRequiredInformationWriter(getTimeStamp()+"Calculated Quantity value ::" + strQuantity +"\n");
        	sbXML.append("\t<Usage>" + strUsage + "</Usage>\n");
        	sbXML.append("\t<Quantity>" + strQuantity + "</Quantity>\n");
        	mqlLogRequiredInformationWriter(getTimeStamp()+"getUsageQuantityElementBOMXML() ends...\n");
        	return sbXML.toString();
        }
    	   /**
    	    * This method is used to get th value of Quantity attribute if the "Usage" value is "As Required"
    	    *
    	    * @param context the eMatrix Context object
    	    * @param featueDO the Feature Object
    	    * @param objProductConfiguration the Product Configuration Object
    	    * @throws Exception if the operation fails
    	    */
    	private MapList getEvaluatedQuantityRules(Context context,
    	           									 DomainObject featueDO,
    	           									 String pcid)throws Exception{

    		   mqlLogRequiredInformationWriter(getTimeStamp()+"Inside 'getEvaluatedQuantityRules' method ::" + "\n");

    	       MapList allEvaluatedQuantityRules = new MapList();
    	       StringList objectSelects = new StringList(1);
    	       objectSelects.addElement(ConfigurationConstants.SELECT_ID);
    	       objectSelects.addElement(ConfigurationConstants.ATTRIBUTE_QUANTITY);
    	       StringBuffer relWhere = new StringBuffer();
    	       relWhere.append("attribute[");
    	       relWhere.append(ConfigurationConstants.ATTRIBUTE_RULE_STATUS);
    	       relWhere.append("]==Active");
    	       String strExpression = "";
    	       MapList quantityRuleObjects = featueDO.getRelatedObjects(context,
    			    		   											ConfigurationConstants.RELATIONSHIP_QUANTITY_RULE,
    			    		   											ConfigurationConstants.TYPE_QUANTITY_RULE,
    			    		   											objectSelects,
    			    		   											new StringList(ConfigurationConstants.SELECT_RELATIONSHIP_ID),
    			    		   											false,
    			    		   											true,
    			    		   											(short) 1,
    			    		   											"",
    			    		   											relWhere.toString(),0);
    	       mqlLogRequiredInformationWriter(getTimeStamp()+"MapList of Quantity Rule Objects ::" +quantityRuleObjects+ "\n");

    	       Iterator itrQuantityRule = quantityRuleObjects.iterator();
    	       while (itrQuantityRule.hasNext()){
    	           HashMap evaluatedQuantityRules = new HashMap();
    	           Hashtable QuantityRule = (Hashtable) itrQuantityRule.next();
    	           String strQuantityRule = (String) QuantityRule.get(DomainConstants.SELECT_ID);
    	           DomainObject objQuantityRule = new DomainObject(strQuantityRule);
    	           StringList qrSelects = new StringList(2);
    	           qrSelects.addElement("attribute["+ ConfigurationConstants.ATTRIBUTE_QUANTITY + "]");
    	           qrSelects.addElement("attribute["+ ConfigurationConstants.ATTRIBUTE_RIGHT_EXPRESSION + "]");
    	           Map mapQuantity = objQuantityRule.getInfo(context, qrSelects);
    	           strExpression = (String) mapQuantity.get("attribute["+ ConfigurationConstants.ATTRIBUTE_RIGHT_EXPRESSION + "]");
    	           String strQuantity= (String) mapQuantity.get("attribute["+ ConfigurationConstants.ATTRIBUTE_QUANTITY + "]");
    	           Map mapTemp = new HashMap();
    	           mapTemp.put(DomainConstants.SELECT_ID, strQuantityRule.trim());
    	           //this.setId(objProductConfiguration.getInfo(context,ConfigurationConstants.SELECT_ID));
    	           //TODO
    	           String strExpResult = evaluateIRExpression(context, strQuantityRule,pcid, strExpression);
    	           if(strExpression.equals(""))
    	           {
    	        	   strExpResult = STR_TRUE;
    	           }
    	           if (strExpResult.equals(STR_TRUE)) {
    	               evaluatedQuantityRules.put(DomainConstants.SELECT_ID,strQuantityRule);
    	               evaluatedQuantityRules.put("quantity", strQuantity);
    	               allEvaluatedQuantityRules.add(evaluatedQuantityRules);
    	           }
    	       }
    	       mqlLogRequiredInformationWriter(getTimeStamp()+"getEvaluatedQuantityRules() ends..." + "\n");
    	       return allEvaluatedQuantityRules;
    	   }
    	    /**
    	     * This method is for Evaluating the Part Inclusion Rules
    	     * It gets the Inclusion Rule from the GBOM object and fetches the expression
    	     * for it. Then it evaluates the expression to true or false.
    	     *
    	     * @param context the eMatrix <code>Context</code> object
    	     * @param strObjectID - ObjectID of the GBOM object
    	     * @param strProductConfigurationID - ProductConfiguration ID of the ProductConfiguration for which the GBOM is generated.
    	     * @param errorHolder - the bean instance
    	     *
    	     * @return String "TRUE" or "FALSE"
    	     * @throws Exception if the operation fails
    	     */
    	private String evaluateIRExpression(Context context,
    	                                       String strGBOMRelID,
    	                                       String strProductConfigurationID,
    	                                       String strRightExpression)throws Exception {

    		    mqlLogRequiredInformationWriter(getTimeStamp()+"Inside 'evaluateIRExpression' method " + "\n");
    		    mqlLogRequiredInformationWriter(getTimeStamp()+"GBOM Rel ID              :: " + strGBOMRelID +"\n");
    		    mqlLogRequiredInformationWriter(getTimeStamp()+"Product Configuration ID :: " + strProductConfigurationID +"\n");
    		    mqlLogRequiredInformationWriter(getTimeStamp()+"Right Expression value   ::" + strRightExpression +"\n\n");

    	   	    String commonvaluesSelectable ="frommid["+ConfigurationConstants.RELATIONSHIP_COMMON_VALUES+"].torel.to.id";
    	   	    String selCOSel ="to.id";
    	   	    DomainObject.MULTI_VALUE_LIST.add(commonvaluesSelectable);
    	   	    String strResult = STR_FALSE;
    	   	    i18nNow i18nnow = new i18nNow();
    	   	    String strLanguage = context.getSession().getLanguage();
    	   	    String strIRErrorMsg = i18nnow.GetString("emxConfigurationStringResource",
    	 										  	     strLanguage,
    	 										  	     "emxProduct.Error.InlcusionRuleFailed");
    	   	    try {
    	             //getting the selected options on the product configuration and putting it in the map
    	             HashMap selectedOptions = new HashMap();
    	             MapList _SelectedOptions =getSelectedOptions( context,  strProductConfigurationID,true,true);
    	             Iterator it = _SelectedOptions.iterator();
    	             while (it.hasNext())
    	             {
    	             	Map so_Map = (Map) it.next();
    	                 selectedOptions.put(so_Map.get(DomainConstants.SELECT_ID),"");
    	             }
    	             DomainObject domPC = new DomainObject(strProductConfigurationID);
    	             String strContextId = domPC.getInfo(context, "to["+ ConfigurationConstants.RELATIONSHIP_PRODUCT_CONFIGURATION+ "].from.id");
    	             selectedOptions.put(strContextId, "");
    	             //getting the right expression attribute on inclusion rule which is connected to GBOM
    	             String boolleanExpression=strRightExpression;
    	   	        if (!(strRightExpression == null || ("".equals(strRightExpression)) || ("null".equals(strRightExpression))))
    	   	        {
    	   	            StringList relIDSList = new StringList(1);
    	   	            StringList tempList = FrameworkUtil.splitString(strRightExpression," ");
    	   	            String  b_rIDS="";
    	 	  	        for(int k=0;k<tempList.size();k++)
    	 	  	        {
    	 	  	            b_rIDS = ((String)tempList.get(k)).trim();
    	 		  	        if(b_rIDS.length()>0 && !(b_rIDS.equals(AND)||b_rIDS.equals(OR)||b_rIDS.equals(NOT)||b_rIDS.charAt(0)==OPEN_BRACE_CHAR||b_rIDS.charAt(0)==CLOSE_BRACE_CHAR))
    	 		  	        {
    	 		  	            //if it starts with r then it is common group relationship
    	 		  	            if(b_rIDS.startsWith("R"))
    	 		  	            {
    	 		  	            	StringList strRelSel = new StringList(commonvaluesSelectable);
    	 		  	            	strRelSel.add(selCOSel);
    	 		  	              MapList maplist  = DomainRelationship.getInfo(context,new String[]{b_rIDS.substring(1)},strRelSel);
    	 		  	              StringList CG_FL_ids = (StringList)((Map)maplist.get(0)).get(commonvaluesSelectable);
    	 		  	              String CO_REL_ids = (String)((Map)maplist.get(0)).get(selCOSel);
    	 		  	              if(CG_FL_ids!=null){
    	 		  	            	for(int j=0;j<CG_FL_ids.size();j++)
    	 			  	              {
    	 			  	                if(selectedOptions.keySet().contains(CG_FL_ids.get(j)))
    	 			  	                {
    	 			  	                   boolleanExpression=boolleanExpression.replace(b_rIDS, STR_TRUE);
    	 			  	                   break;
    	 			  	                }
    	 			  	              }
    	 		  	              }else if(selectedOptions.keySet().contains(CO_REL_ids))
    	 		  	              {
    	 		  	            	  boolleanExpression=boolleanExpression.replace(b_rIDS, STR_TRUE);
    	 		  	              }else{
    	 		  	            	boolleanExpression=boolleanExpression.replace(b_rIDS, STR_FALSE);
    	 		  	              }
    	 		  	            }else
    	 		  	            {
    	 		  	            	String strObjId = (String) ((HashMap)(DomainObject.getInfo(context,new String[]{b_rIDS.substring(1)}, new StringList(ConfigurationConstants.SELECT_ID)).get(0))).get(ConfigurationConstants.SELECT_ID);
    	 		  	            	// if BUS id  present in selected options replace it with TRUE else FALSE
    	 		  	            	if(selectedOptions.keySet().contains(strObjId))
    	 		  	            	{
    	 		  	            		boolleanExpression=boolleanExpression.replace(b_rIDS, STR_TRUE);
    	 		  	            	}else
    	 		  	            	{
    	 		  	            		boolleanExpression= boolleanExpression.replace(b_rIDS, STR_FALSE);
    	 		  	            	}
    	 		  	            }
    	 		  	       }
    	 	  	       }
    	                MQLCommand mqlCommand = new MQLCommand();
    	                StringBuffer strBuffer = new StringBuffer("");
    	                DomainObject domObj = new DomainObject(strGBOMRelID);
    	                if(domObj.exists(context)){
    	             	   strBuffer.append("evaluate expression \"")
    	                    .append(boolleanExpression)
    	                    .append("\" on bus ")
    	                    .append(strGBOMRelID);
    	                }else{
    	             	   strBuffer.append("evaluate expression \"")
    	                    .append(boolleanExpression)
    	                    .append("\" on relationship ")
    	                    .append(strGBOMRelID);
    	                }




    	                String strCommand = strBuffer.toString();
    	                boolean bMQLResult = false;
    	                bMQLResult =  mqlCommand.executeCommand(context, strCommand);
    	                if (bMQLResult == true)
    	                {
    	                  String strgetResult = mqlCommand.getResult();
    	                  if (strgetResult.trim().equalsIgnoreCase(STR_FALSE))
    	                  {
    	                      strResult = STR_FALSE;
    	                  } else if (strgetResult.trim().equalsIgnoreCase(STR_TRUE))
    	                  {
    	                      strResult = STR_TRUE;
    	                  }
    	                }else
    	                {
                          String strEvaluateExpressionFailure = i18nnow.GetString("emxConfigurationStringResource",
    	 													                     strLanguage,
    	 													                     "emxProduct.Error.EvaluateExpressionFailure");
    	 				 throw new Exception(strEvaluateExpressionFailure);
    	                }
    	   	          }
    	          }
    	          catch(Exception exp)
    	          {
    	              throw new FrameworkException(strIRErrorMsg);
    	          }
    	          mqlLogRequiredInformationWriter(getTimeStamp()+"evaluateIRExpression() ends..." + "\n");
    	          return strResult;
    	   	    }
    	private MapList getSelectedOptions(Context context, String objectId,boolean getLF,boolean getCF) throws Exception
    	     {
    			if(pcSelectedOptionList == null){
    				pcSelectedOptionList = getSelectedOptions(context,objectId,null,getLF,getCF);
    			}
    			return pcSelectedOptionList;
    	     }
    	     /**
    	      * This method gets the details of selected options
    	      * @param context
    	      * @param objectId
    	      * @param strLFObjIds
    	      * @param getLF
    	      * @param getCF
    	      * @return
    	      * @throws Exception
    	      */
    	private MapList getSelectedOptions(Context context,String objectId,StringList strLFObjIds,boolean getLF,boolean getCF) throws Exception
    	     {

    		   mqlLogRequiredInformationWriter(getTimeStamp()+"Inside 'getSelectedOptions' method " + "\n");
    		   
    	   	    ProductLineCommon pl = new ProductLineCommon();
    	   	    MapList selectedOptionList = new MapList();
				DomainObject domProductConfig = DomainObject.newInstance(context,
						objectId);
    	         //strWhereCondition = strWhereCondition + strLFCond + strCFCond;
    	         String strRelationPattern = ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS;
    	         StringList relationshipSelect = new StringList(SELECT_SO_ID);
    	         relationshipSelect.add(SELECT_SO_ATTR_QUANTITY);
    	         relationshipSelect.add(SELECT_SO_ATTR_KEYINVAL);
    	         relationshipSelect.add(SELECT_SO_TOREL_ATTR_CONFIGURATIONSELECTIONCRITERIA);
    	         relationshipSelect.add(SELECT_SO_ATTR_FINDNUMBER);
    	         relationshipSelect.add(SELECT_SO_TOREL_TONAME);
    	         relationshipSelect.add(SELECT_SO_TOREL_TOID);
    	         relationshipSelect.add(SELECT_SO_TOREL_ATTR_KEYINTYPE);
    	         relationshipSelect.add(SELECT_SO_TOREL_TOPHYID);
    	         relationshipSelect.add(SELECT_SO_TOREL_TOTYPE);
    	         relationshipSelect.add(SELECT_SO_TOREL_TOREVISION);
    	         relationshipSelect.add(SELECT_SO_TOREL_ID);
    	         relationshipSelect.add(SELECT_SO_TOREL_ATTR_QUANTITY);
    	         relationshipSelect.add(SELECT_SO_TOREL_ATTR_FINDNUMBER);
    	         relationshipSelect.add(SELECT_SO_TOREL_ATTR_FORCEPARTREUSE);
    	         relationshipSelect.add(SELECT_SO_TOREL_ATTR_USAGE);

      	        // MapList preciseBOMList = pl.queryConnection(context,strRelationPattern,relationshipSelect,strWhereCondition);

    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_ID);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_ATTR_QUANTITY);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_ATTR_KEYINVAL);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_TOREL_ATTR_CONFIGURATIONSELECTIONCRITERIA);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_ATTR_FINDNUMBER);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_TOREL_TONAME);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_TOREL_TOID);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_TOREL_ATTR_KEYINTYPE);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_TOREL_TOPHYID);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_TOREL_TOTYPE);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_TOREL_TOREVISION);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_TOREL_ID);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_TOREL_ATTR_QUANTITY);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_TOREL_ATTR_FINDNUMBER);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_TOREL_ATTR_FORCEPARTREUSE);
    	         DomainConstants.MULTI_VALUE_LIST.add(SELECT_SO_TOREL_ATTR_USAGE);

    	        Map selectedOptionsMap = domProductConfig.getInfo(context, relationshipSelect);
    	        DomainConstants.MULTI_VALUE_LIST.clear();
    	         Map infoMap = null;
    	         //for (int i = 0; i < preciseBOMList.size(); i++)
				   if(selectedOptionsMap != null &&
						   !selectedOptionsMap.isEmpty())
				   {
					   Set keys = selectedOptionsMap.keySet();
					   Map newLogicalFeatureMap = new HashMap();  
					   for(Object key : keys)
					   {
						   String newkey = "";
						   String sKey = (String)key;

						   if(SELECT_SO_ID.equalsIgnoreCase(sKey)){
							   newkey = DomainConstants.SELECT_ID+"["+ConfigurationConstants.RELATIONSHIP_SELECTED_OPTIONS+"]";
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList);
						   }else if(SELECT_SO_TOREL_TOID.equalsIgnoreCase(sKey))
						   {
							   newkey = DomainConstants.SELECT_ID;
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList);
						   }else if(SELECT_SO_TOREL_ATTR_KEYINTYPE.equalsIgnoreCase(sKey)){
							   newkey = ConfigurationConstants.ATTRIBUTE_KEY_IN_TYPE;
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList); 
						   }else if(SELECT_SO_TOREL_TOPHYID.equalsIgnoreCase(sKey)){
							   newkey = ConfigurationConstants.SELECT_PHYSICAL_ID;
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList); 
						   }else if(SELECT_SO_TOREL_TONAME.equalsIgnoreCase(sKey)){
							   newkey = DomainConstants.SELECT_NAME;
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList); 
						   }else if(SELECT_SO_TOREL_TOTYPE.equalsIgnoreCase(sKey)){
							   newkey = DomainConstants.SELECT_TYPE;
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList); 
						   }else if(SELECT_SO_TOREL_TOREVISION.equalsIgnoreCase(sKey)){
							   newkey = DomainConstants.SELECT_REVISION;
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList); 
						   }else if(SELECT_SO_ATTR_QUANTITY.equalsIgnoreCase(sKey)){
							   newkey = ConfigurationConstants.ATTRIBUTE_QUANTITY;
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList); 
						   }else if(SELECT_SO_ATTR_KEYINVAL.equalsIgnoreCase(sKey)){
							   newkey = ConfigurationConstants.ATTRIBUTE_KEY_IN_VALUE;
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList); 
						   }else if(SELECT_SO_ATTR_FINDNUMBER.equalsIgnoreCase(sKey)){
							   newkey = ConfigurationConstants.ATTRIBUTE_FIND_NUMBER;
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList); 
						   }else if(SELECT_SO_TOREL_ID.equalsIgnoreCase(sKey)){
							   newkey =DomainConstants.SELECT_RELATIONSHIP_ID;
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList); 
						   }else if(SELECT_SO_TOREL_ATTR_QUANTITY.equalsIgnoreCase(sKey)){
							   newkey ="torel.attribute[" + ConfigurationConstants.ATTRIBUTE_QUANTITY + "]";
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList); 
						   }else if(SELECT_SO_TOREL_ATTR_FINDNUMBER.equalsIgnoreCase(sKey)){
							   newkey ="torel.attribute[" + ConfigurationConstants.ATTRIBUTE_FIND_NUMBER + "]";
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList); 
						   }else if(SELECT_SO_TOREL_ATTR_FORCEPARTREUSE.equalsIgnoreCase(sKey)){
							   newkey ="torel.attribute[" + ConfigurationConstants.ATTRIBUTE_FORCE_PART_REUSE + "]";
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList); 
						   }else if(SELECT_SO_TOREL_ATTR_USAGE.equalsIgnoreCase(sKey)){
							   newkey ="torel.attribute[" + ConfigurationConstants.ATTRIBUTE_USAGE + "]";
							   StringList valueList = ConfigurationUtil.convertObjToStringList(context,selectedOptionsMap.get(sKey));
							   createSelectedOptionList(newkey,valueList,selectedOptionList); 
						   }
					   }
    	         }
				   System.out.println(getTimeStamp()+"Selected Options List " + selectedOptionList +"\n");
    	   	  	return selectedOptionList;
    	     }    	     
      /**
       * This method prepares element related to GBOM for BOM XML  
       * @param context
       * @param strFeatureId
       * @param strFeatureObjectType
       * @param strForcePartReuse
       * @param strForcePartReuseEnabled
       * @return
       * @throws Exception
       */
    	private String getGBOMElementBOMXML(Context context,Map gbomElementMap,String pcID) throws Exception
        {
    		mqlLogRequiredInformationWriter(getTimeStamp()+"Inside getGBOMElementBOMXML() ::" + gbomElementMap +"\n");
    		StringBuffer sbXML = new StringBuffer();
        	String strFeatureId = (String)gbomElementMap.get("FeatureId");
        	String strFeatureObjectType = (String)gbomElementMap.get("FeatureObjectType");
        	String strForcePartReuse = (String)gbomElementMap.get("ForcePartReuse");
        	String strPrdCtxtObjType = (String)gbomElementMap.get("ContextObjectType");
        	String strPrdCtxtObjId = (String)gbomElementMap.get("ContextObjectId");
        	//String strForcePartReuseEnabled = (String)gbomElementMap.get("ForcePartReuseEnabled");
        	boolean isVariantExists = false;
        	LogicalFeature lfbean = new LogicalFeature(strFeatureId);
        	StringList strListActiveDVs;
        	MapList listActiveDVs;
        	StringList objSelects = new StringList();
            objSelects.addElement(ConfigurationConstants.SELECT_PHYSICAL_ID);
            //Get Active Design Variant list        
            if(ProductLineCommon.isNotNull(strPrdCtxtObjType) && 
            		mxType.isOfParentType(context, strPrdCtxtObjType,ConfigurationConstants.TYPE_PRODUCT_VARIANT)){
            	listActiveDVs = lfbean.getValidDesignVariants(context, strFeatureId, strPrdCtxtObjId);
            }else{
            	 listActiveDVs = lfbean.getActiveDesignVariants(context, null, null, objSelects, null, false, true, 0, 0, "", "", (short)0, "");
            }
            strListActiveDVs = new StringList(listActiveDVs.size()); 
            for (Iterator actDVIttr = listActiveDVs.iterator(); actDVIttr
    				.hasNext();) {
    			Map dvMap = (Map) actDVIttr.next();
    			String strDVID = (String) dvMap.get(ConfigurationConstants.SELECT_PHYSICAL_ID);
    			strListActiveDVs.add(strDVID);
            }
            
            //Get the associated Parts/Part Family to the Logical Feature
            String strCustomPartMode = EnoviaResourceBundle.getProperty(context,
    		"emxConfiguration.PreviewBOM.EnableCustomPartMode");
            StringBuffer strRelSel = new StringBuffer(ConfigurationConstants.RELATIONSHIP_GBOM);
            if (ProductLineCommon.isNotNull(strCustomPartMode)
    				&& strCustomPartMode.equalsIgnoreCase("true")) {
            	 strRelSel.append(",");
                 strRelSel.append(ConfigurationConstants.RELATIONSHIP_CUSTOM_GBOM);
            }

            MapList partsToEvaluate = lfbean.getActiveGBOMStructure(context, "", strRelSel.toString(),objSelects, getRelSelectsForGBOM(), false, true, 1, 0, null, null, DomainObject.FILTER_ITEM, "");
            for (Iterator actDVIttr = partsToEvaluate.iterator(); actDVIttr
    			.hasNext();) {
         	    Map partMap = (Map) actDVIttr.next();
         	    partMap.put("listActiveDVs", strListActiveDVs);
            }
            //Get the Parts to evaluate for the Logical Feature Id
            if(mxType.isOfParentType(context, strFeatureObjectType,ConfigurationConstants.TYPE_LOGICAL_FEATURE))
            {
                String tempArgs[] = new String[]{strFeatureId,"from",ConfigurationConstants.RELATIONSHIP_VARIES_BY};
                isVariantExists = Boolean.parseBoolean(hasRelationship(context,tempArgs));
            }
            HashMap evaluatedParts = getEvaluatedListsIdBased(context,partsToEvaluate,isVariantExists,pcID);
            ArrayList parts = (ArrayList) evaluatedParts.get("partIds");
    		MapList mlPartInfo = (MapList) evaluatedParts.get("partInfo");
            ArrayList partFamily = (ArrayList) evaluatedParts.get("partFamilyIds");
            if (partFamily.size() > 0) 
            {
            	sbXML.append("\t<PartFamily>\n");
                for (int i = 0; i < partFamily.size(); i++) 
                {
                    String strPfId = (String) partFamily.get(i);
                    DomainObject domPf = new DomainObject(strPfId);	                       
                    List lstFeatureSelects = new StringList(DomainConstants.SELECT_TYPE);
                    lstFeatureSelects.add(DomainConstants.SELECT_NAME);
                    lstFeatureSelects.add(DomainConstants.SELECT_REVISION);
                    lstFeatureSelects.add(ConfigurationConstants.SELECT_PHYSICAL_ID);
                    Map mapInfo = domPf.getInfo(context,(StringList) lstFeatureSelects);
                    String strPfPhyId  = (String)mapInfo.get(ConfigurationConstants.SELECT_PHYSICAL_ID);
                    String strPfType   = (String)mapInfo.get(DomainConstants.SELECT_TYPE);
                    String strPfName   = (String)mapInfo.get(DomainConstants.SELECT_NAME);
                    String strPfRev    = (String)mapInfo.get(DomainConstants.SELECT_REVISION);
                    sbXML.append("\t\t<Part "+ConfigurationConstants.SELECT_ID+"=\"")
                      .append(strPfId)
    					 .append("\" "+ConfigurationConstants.SELECT_PHYSICAL_ID+"=\"")
                      .append(strPfPhyId)
                      .append("\" "+ConfigurationConstants.SELECT_TYPE+"=\"")
                      .append(strPfType)
                      .append("\" "+ConfigurationConstants.SELECT_NAME+"=\"")
                      .append(strPfName)
                      .append("\" rev=\"")
                      .append(strPfRev)
                      .append("\"/>\n");
                }
                sbXML.append("\t</PartFamily>\n");
            }
            if(strForcePartReuse.equalsIgnoreCase("Yes")) 
            {
                if(mlPartInfo.size() > 0)
                {
    				   MapList mlStdParts = new MapList();
    				   MapList mlCustParts = new MapList();
    				   MapList mlParts = new MapList();
                 //String strPartIdUsage = null;
                 for (int i = 0; i < mlPartInfo.size(); i++)
                 {
                	 Map partInfoMap = (Map)mlPartInfo.get(i);
                     String strPartUsage = (String) partInfoMap.get(ConfigurationConstants.PARTUSAGE);
                     mlParts.add(partInfoMap);
                     if (ConfigurationConstants.RANGE_VALUE_STANDARD.equals(strPartUsage))
                    	 mlStdParts.add(partInfoMap);
                     else
                    	 mlCustParts.add(partInfoMap);
                 }
                    if (mlStdParts.size() == 1 && mlParts.size() ==1) 
                    {
    					 Map partInfoMap = (Map) (mlStdParts.size() == 1 ? mlStdParts.get(0) : mlParts.get(0));
                         String strPartId = (String)partInfoMap.get("id");
                         boolean hasEBOM = true;
                         String [] args = new String[3];
        		         args[0] = strPartId;
        		         args[1] = "from" ;
        		         args[2] = ConfigurationConstants.RELATIONSHIP_EBOM;
        		         if((ProductLineCommon.hasRelationship(context, args)).equalsIgnoreCase("false")){
        		        	 hasEBOM = false;
        		         }
                         String strPartUsage = (String)partInfoMap.get(ConfigurationConstants.PARTUSAGE);
                         String strGBOMRelId = (String)partInfoMap.get(ConfigurationConstants.GBOMRELID);
                        DomainObject domPart = new DomainObject(strPartId);
                        List lstFeatureSelects = new StringList(DomainConstants.SELECT_TYPE);
                        lstFeatureSelects.add(DomainConstants.SELECT_NAME);
                        lstFeatureSelects.add(DomainConstants.SELECT_REVISION);
                        lstFeatureSelects.add(ConfigurationConstants.SELECT_PHYSICAL_ID);
                        //need to check if this returns multi values if Part is used as GBOM in different LFs
    					   String strPartCommitted = "to["
                             + ConfigurationConstants.RELATIONSHIP_GBOM
                             + "]."
                             + STR_ATTRIBUTE_OPEN_BRACE
                             + ConfigurationConstants.ATTRIBUTE_COMMITTED
                             + EXPRESSION_CLOSE;
                        lstFeatureSelects.add(strPartCommitted);
                        Map mapInfo = domPart.getInfo(context,(StringList) lstFeatureSelects); 
                        String strPfPhyId  = (String)mapInfo.get(ConfigurationConstants.SELECT_PHYSICAL_ID);
                        String strPartType = (String)mapInfo.get(DomainConstants.SELECT_TYPE);
                        String strPartName = (String)mapInfo.get(DomainConstants.SELECT_NAME);
                        
                        strPartName = stringDecode(context,strPartName);
      				   
                        String strPartRev  = (String)mapInfo.get(DomainConstants.SELECT_REVISION);
    					   strPartCommitted   = (String)mapInfo.get(strPartCommitted);
                        String strPartStatus = hasEBOM ? 
                                             "Resolved" : "Pending";
                        sbXML.append("\t<")
                          .append(strPartStatus)
                          .append(" "+ConfigurationConstants.SELECT_ID+"=\"")
                          .append(strPartId)
    						 .append("\" "+ConfigurationConstants.SELECT_PHYSICAL_ID+"=\"")
                          .append(strPfPhyId)
                          .append("\" "+ConfigurationConstants.SELECT_TYPE+"=\"")
                          .append(strPartType)
                          .append("\" "+ConfigurationConstants.SELECT_NAME+"=\"")
                          .append(strPartName)
                          .append("\" rev=\"")
                          .append(strPartRev)
                          .append("\" "+ConfigurationConstants.PARTUSAGE+"=\"")
                          .append(strPartUsage)
                          .append("\" "+ConfigurationConstants.GBOMRELID+"=\"")
                          .append(strGBOMRelId)
                          .append("\"/>\n");

                    }else
                    {
    					   //remove preselected STD part from duplicates
                     if (mlStdParts.size() == 1) {
                    	 Map partInfoMap = (Map) (mlStdParts.size() == 1 ? mlStdParts.get(0) : mlParts.get(0));
                         String strPartId = (String)partInfoMap.get("id");
                         boolean hasEBOM = true;
                         String [] args = new String[3];
        		         args[0] = strPartId;
        		         args[1] = "from" ;
        		         args[2] = ConfigurationConstants.RELATIONSHIP_EBOM;
        		         if((ProductLineCommon.hasRelationship(context, args)).equalsIgnoreCase("false")){
        		        	 hasEBOM = false;
        		         }
                         String strPartUsage = (String)partInfoMap.get(ConfigurationConstants.PARTUSAGE);
                         String strGBOMRelId = (String)partInfoMap.get(ConfigurationConstants.GBOMRELID);
                        DomainObject domPart = new DomainObject(strPartId);
                        List lstFeatureSelects = new StringList(DomainConstants.SELECT_TYPE);
                        lstFeatureSelects.add(DomainConstants.SELECT_NAME);
                        lstFeatureSelects.add(DomainConstants.SELECT_REVISION);
                        lstFeatureSelects.add(ConfigurationConstants.SELECT_PHYSICAL_ID);
                        //need to check if this returns multi values if Part is used as GBOM in different LFs
    					   String strPartCommitted = "to["
                             + ConfigurationConstants.RELATIONSHIP_GBOM
                             + "]."
                             + STR_ATTRIBUTE_OPEN_BRACE
                             + ConfigurationConstants.ATTRIBUTE_COMMITTED
                             + EXPRESSION_CLOSE;
                        lstFeatureSelects.add(strPartCommitted);
                        Map mapInfo = domPart.getInfo(context,(StringList) lstFeatureSelects); 
                        String strPfPhyId  = (String)mapInfo.get(ConfigurationConstants.SELECT_PHYSICAL_ID);
                        String strPartType = (String)mapInfo.get(DomainConstants.SELECT_TYPE);
                        String strPartName = (String)mapInfo.get(DomainConstants.SELECT_NAME);
                        String strPartRev  = (String)mapInfo.get(DomainConstants.SELECT_REVISION);
    					   strPartCommitted   = (String)mapInfo.get(strPartCommitted);
                        String strPartStatus = hasEBOM ? 
                                             "Resolved" : "Pending";
                        sbXML.append("\t<")
                          .append(strPartStatus)
                          .append(" "+ConfigurationConstants.SELECT_ID+"=\"")
                          .append(strPartId)
    						 .append("\" "+ConfigurationConstants.SELECT_PHYSICAL_ID+"=\"")
                          .append(strPfPhyId)
                          .append("\" "+ConfigurationConstants.SELECT_TYPE+"=\"")
                          .append(strPartType)
                          .append("\" "+ConfigurationConstants.SELECT_NAME+"=\"")
                          .append(strPartName)
                          .append("\" rev=\"")
                          .append(strPartRev)
                          .append("\" "+ConfigurationConstants.PARTUSAGE+"=\"")
                          .append(strPartUsage)
                          .append("\" "+ConfigurationConstants.GBOMRELID+"=\"")
                          .append(strGBOMRelId)
                          .append("\"/>\n");
                    	 mlParts.remove(mlStdParts.get(0));
                     }
                     sbXML.append("\t<Duplicate>\n");
                        for (int i = 0; i < mlParts.size(); i++) 
                        {
                        	Map partInfoMap = (Map) mlParts.get(i);
                            String strPartId = (String)partInfoMap.get("id");
                            String strPartUsage = (String)partInfoMap.get(ConfigurationConstants.PARTUSAGE);
                            String strGBOMRelId = (String)partInfoMap.get(ConfigurationConstants.GBOMRELID);
                            DomainObject domPart = new DomainObject(strPartId);	                               
                            List lstFeatureSelects = new StringList(DomainConstants.SELECT_TYPE);
                            lstFeatureSelects.add(DomainConstants.SELECT_NAME);
                            lstFeatureSelects.add(DomainConstants.SELECT_REVISION);
                            lstFeatureSelects.add(ConfigurationConstants.SELECT_PHYSICAL_ID);
                            Map mapInfo = domPart.getInfo(context,(StringList) lstFeatureSelects);
                            String strPfPhyId  = (String)mapInfo.get(ConfigurationConstants.SELECT_PHYSICAL_ID);
                            String strPartType = (String)mapInfo.get(DomainConstants.SELECT_TYPE);
                            String strPartName = (String)mapInfo.get(DomainConstants.SELECT_NAME);
                            String strPartRev = (String)mapInfo.get(DomainConstants.SELECT_REVISION);   
                            sbXML.append("\t\t<Part "+ConfigurationConstants.SELECT_ID+"=\"")
                              .append(strPartId)
    							   .append("\" "+ConfigurationConstants.SELECT_PHYSICAL_ID+"=\"")
                              .append(strPfPhyId)
                              .append("\" "+ConfigurationConstants.SELECT_TYPE+"=\"")
                              .append(strPartType)
                              .append("\" "+ConfigurationConstants.SELECT_NAME+"=\"")
                              .append(strPartName)
                              .append("\" rev=\"")
                              .append(strPartRev)
                              .append("\" "+ConfigurationConstants.PARTUSAGE+"=\"")
                              .append(strPartUsage)
                              .append("\" "+ConfigurationConstants.GBOMRELID+"=\"")
                              .append(strGBOMRelId)
                              .append("\"/>\n");

                        }
                        sbXML.append("\t</Duplicate>\n");
                    }
                }
            }
            mqlLogRequiredInformationWriter(getTimeStamp()+"getGBOMElementBOMXML() ends..." + "\n");
        	return sbXML.toString();
        }

        /**
         * Private method returns relationship selectable for GBOM relationship Expand.
         *
         */
    	private static StringList getRelSelectsForGBOM() 
    	  {
    		StringList relSelects = new StringList();
    		relSelects.add("tomid["
    				+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION
    				+ "].from.id");
    		relSelects.add("tomid["
    				+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION
    				+ "].from.attribute["
    				+ ConfigurationConstants.ATTRIBUTE_RULE_COMPLEXITY + "]");
    		relSelects.add("tomid["
    				+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION
    				+ "].from.attribute["
    				+ ConfigurationConstants.ATTRIBUTE_DESIGNVARIANTS + "]");
    		relSelects.add("tomid["
    				+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION
    				+ "].from.attribute["
    				+ ConfigurationConstants.ATTRIBUTE_RIGHT_EXPRESSION + "]");
    		relSelects.add("tomid["
    				+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION
    				+ "].from.attribute["
    				+ ConfigurationConstants.ATTRIBUTE_LEFT_EXPRESSION + "]"); 		
    		relSelects.add(STR_ATTRIBUTE_OPEN_BRACE
                   + ConfigurationConstants.ATTRIBUTE_RULE_TYPE
                   + EXPRESSION_CLOSE);
    		relSelects.add(DomainRelationship.SELECT_TO_ID);
    		relSelects.add(DomainRelationship.SELECT_TO_TYPE);
    		relSelects.add(DomainRelationship.SELECT_TO_NAME);
    		relSelects.add(DomainRelationship.SELECT_TO_REVISION);
    		relSelects.add(DomainRelationship.SELECT_ID);
    		return relSelects;
    	  }
    	    /**
    	     * This method is called from updateBomXmlMigration().
    	     * It returns the list of parts which evaluates to true as a result of the IR/ER evaluation
    	     * which is done based on Id comparison & not TNR against selected options
    	     * 
    	     * @param context
    	     * @param arrPartObjectIds array of parts to be evaluated
    	     * @param isVariantExists
    	     * @return Returns HshMap of parts which evaluates to true
    	     * @throws Exception
    	     */
    	private HashMap getEvaluatedListsIdBased(Context context,MapList arrPartObjectIds,boolean isVariantExists,String pcID)throws Exception 
    	     {        
    	        HashMap retMap = new HashMap();
    	        // ArrayList to store Parts connected to Product or feature
    	        ArrayList arrProductPartId = new ArrayList();
    	        // ArrayList to store Part Familys connected to Product or Feature
    	        ArrayList arrProductPartFamilyId = new ArrayList();
    	        // ArrayList to store part which are not evaluated. after evaluation
    	        // result will be stored in arrProductPartId
    	        ArrayList arrPartsToEvaluate = new ArrayList();
    	        // ArrayList to store PartFamily which are not evaluated. after
    	        // evaluation result will be stored in arrProductPartFamilyId
    	        ArrayList arrPartFamilysToEvaluate = new ArrayList();
    	        for (int l = 0; l < arrPartObjectIds.size(); l++) 
    	        {
    	      	    Map mapProductPartId = (Map) arrPartObjectIds.get(l);
    	            String strGBOMRelId  = (String) mapProductPartId.get(DomainConstants.SELECT_RELATIONSHIP_ID);
    	            String type 		 = (String) mapProductPartId.get(DomainConstants.SELECT_TYPE);
    	            String partId 	 	 = (String) mapProductPartId.get(DomainConstants.SELECT_ID);
    	            String ruleType 	 = (String) mapProductPartId.get(STR_ATTRIBUTE_OPEN_BRACE
    														                        + ConfigurationConstants.ATTRIBUTE_RULE_TYPE
    														                        + EXPRESSION_CLOSE);
    	            List listActiveDVs   = (StringList) mapProductPartId.get("listActiveDVs");
    	            //TODO getGBOMStructure should return listActiveDVs
    	            
    	            String strRuleExp    = (String) mapProductPartId.get("tomid["
    														         				+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION
    														         				+ "].from.attribute["
    														         				+ ConfigurationConstants.ATTRIBUTE_RIGHT_EXPRESSION + "]");
    	            String strDVs    	 = (String) mapProductPartId.get("tomid["
    														         				+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION
    														         				+ "].from.attribute["
    														         				+ ConfigurationConstants.ATTRIBUTE_DESIGNVARIANTS + "]");
    	            if(strRuleExp == null)
    	            {
    	                strRuleExp = "";
    	            }
    	            if(strDVs == null)
    	            {
    	            	strDVs = "";
    	            }
    	            Hashtable objectdetails = new Hashtable();
    	            objectdetails.put("ruleType", ruleType);
    	            objectdetails.put("expression", strRuleExp);
    	            objectdetails.put("partId", partId);
    	            objectdetails.put("DesignVariants", strDVs);
    	            objectdetails.put("GBOMRELID", strGBOMRelId);                
    	            if(listActiveDVs != null)
    	                objectdetails.put("listActiveDVs", listActiveDVs);                
    	            if (mxType.isOfParentType(context,type,ConfigurationConstants.TYPE_PARTFAMILY)) 
    	            {
    	                arrPartFamilysToEvaluate.add(objectdetails);
    	            }else if(mxType.isOfParentType(context,type,ConfigurationConstants.TYPE_PART))
    	            {
    	                arrPartsToEvaluate.add(objectdetails);
    	            }
    	        }
    	        //Get all the Part Ids
    	        arrProductPartId = gbomParseRulesIdBased(context, arrPartsToEvaluate,"Part", isVariantExists, true,pcID);
    	        if(isVariantExists) {
    	            ArrayList tempPFList = new ArrayList();
    	            for (int i = 0; i < arrPartFamilysToEvaluate.size(); i++) {
    	                Hashtable objectDetails = (Hashtable) arrPartFamilysToEvaluate.get(i);
    	                String partId = (String) objectDetails.get("partId");
    	                tempPFList.add(i, partId);
    	            }
    	            arrProductPartFamilyId = tempPFList;
    	        }else {
    	        	//Get all the Part Family Ids
    	            arrProductPartFamilyId = gbomParseRulesIdBased(context,arrPartFamilysToEvaluate, "Part",isVariantExists, false,pcID);
    	        }

    	        //arrProductPartId - This ArrayList contains the info of Part ID and PartUsage with pipe seperator
    	        //getting the respective PartUsage associated with Part
    	        MapList mlPartInfo = new MapList();
    			if(arrProductPartId.size()>0)
    	        {
    	            String partUsage = null;
    	            for(int i = 0; i < arrProductPartId.size(); i++)
    	            {
    	                String strEvaluatedPartId = (String)arrProductPartId.get(i);
    	                for (int j = 0; j < arrPartObjectIds.size(); j++) 
    	                {
    	                        Map mapProductPartId = (Map) arrPartObjectIds.get(j);
    	                        String strPartId = (String) mapProductPartId.get(DomainConstants.SELECT_ID);
    	                        String strGBOMRelId  = (String) mapProductPartId.get(DomainConstants.SELECT_RELATIONSHIP_ID);
    							if(strEvaluatedPartId.equals(strPartId))
    	                        {
    	                            String strRelationship = (String) mapProductPartId.get(KEY_RELATIONSHIP);
    	                            if(strRelationship.equalsIgnoreCase(ConfigurationConstants.RELATIONSHIP_GBOM))
    	                            {
    	                                partUsage = ConfigurationConstants.RANGE_VALUE_STANDARD;
    	                            }
    	                            else if(strRelationship.equalsIgnoreCase(ConfigurationConstants.RELATIONSHIP_CUSTOM_GBOM))
    	                            {
    	                                partUsage = ConfigurationConstants.RANGE_VALUE_CUSTOM;
    	                            }
    	                            Map partInfoMap = new HashMap();
    	                            partInfoMap.put(ConfigurationConstants.SELECT_ID, strEvaluatedPartId);
    	                            partInfoMap.put(ConfigurationConstants.PARTUSAGE, partUsage);
    	                            partInfoMap.put(ConfigurationConstants.GBOMRELID, strGBOMRelId);
    	                            mlPartInfo.add(partInfoMap);
    	                            break;
    	                        }
    	                }
    	           }
    			}

    	        retMap.put("partIds", arrProductPartId);
    			retMap.put("partInfo", mlPartInfo);
    	        retMap.put("partFamilyIds", arrProductPartFamilyId);
    	        return retMap;
    	    }
    	     
    	     /**
    	      * This method is called from getEvaluatedListsIdBased().It
    	      * takes all the parts to evaluate and returns final part object id's
    	      * 
    	      * @param context
    	      * @param objectsToEvaluate
    	      * @param calledFrom
    	      * @param isVariantExists
    	      * @param isForPart
    	      * @return ArrayList
    	      * @throws Exception
    	      */
    	 	  private ArrayList gbomParseRulesIdBased(Context context,
    									              ArrayList objectsToEvaluate,
    									              String calledFrom,
    									              boolean isVariantExists, 
    									              boolean isForPart,
    									              String pcID) throws Exception {
    	 		  
    	 		 loadResourceFile(context, "emxConfigurationMigration");
    	 		 mqlLogRequiredInformationWriter(getTimeStamp()+" Inside gbomParseRulesIdBased().."+"\n");
    	 		  ArrayList evaluatedObject = new ArrayList();
    	         ArrayList partsWithoutExpression = new ArrayList();
    	         boolean returned = false;
    	         boolean ruleEvaluationRequired = true;
    	         String strExpResult = "";
    	         ArrayList finalReturnParts = new ArrayList();
    	         ArrayList temp = new ArrayList();         
    	         String strProdContextId = "";
    	         String strProdContextType = "";
    	         DomainObject domPC = new DomainObject(pcID);
    	         List lstFeatureSelects = new StringList(DomainConstants.SELECT_ID);
    	         lstFeatureSelects.add("to["
    					                 + ConfigurationConstants.RELATIONSHIP_PRODUCT_CONFIGURATION
    					                 + "].from.id");
    	         lstFeatureSelects.add("to["
    					                 + ConfigurationConstants.RELATIONSHIP_PRODUCT_CONFIGURATION
    					                 + "].from.type");
    	         Map mapInfo = domPC.getInfo(context,(StringList) lstFeatureSelects); 
    	         String strContextId 	= (String)mapInfo.get("to["
    										                 + ConfigurationConstants.RELATIONSHIP_PRODUCT_CONFIGURATION
    										                 + "].from.id");
    	         String strContextType 	= (String)mapInfo.get("to["
    										                 + ConfigurationConstants.RELATIONSHIP_PRODUCT_CONFIGURATION
    										                 + "].from.type");         
    	         //Added to make id based
    	         HashMap selectedOptions = new HashMap();
    	         MapList _SelectedOptions = getSelectedOptions(context,pcID,true,true);
    	         Iterator it = _SelectedOptions.iterator();
    	         while (it.hasNext()) 
    	         {
    	             selectedOptions.put(((Map) it.next()).get(DomainConstants.SELECT_ID), "");
    	         }
    	         //need to add context object id in selected options list to evaluate rule 
    	         selectedOptions.put(strContextId, "");         
    	         if(mxType.isOfParentType(context, strContextType,ConfigurationConstants.TYPE_LOGICAL_FEATURE))
    	         {            
    	            strProdContextId = domPC.getInfo(context, "to["
    									                     + ConfigurationConstants.RELATIONSHIP_FEATURE_PRODUCT_CONFIGURATION
    									                     + "].from.id");
    	            
    	            //check if there is any prodcut context associated
    	            if(strProdContextId != null && ! "null".equalsIgnoreCase(strProdContextId) && ! "".equalsIgnoreCase(strProdContextId))             
    	                strProdContextType = new DomainObject(strProdContextId).getInfo(context, "type");
    	            
    	            //check if the context is a Product Variant
    	            if(mxType.isOfParentType(context, strProdContextType,ConfigurationConstants.TYPE_PRODUCT_VARIANT))
    	            {
    	                strContextId = strProdContextId;
    	                strContextType = strProdContextType;
    	            }             
    	         }         
    	         if(objectsToEvaluate != null && objectsToEvaluate.size() > 0) 
    	         {
    	             for (int count = 0; count < objectsToEvaluate.size(); count++) 
    	             {
    	                 strExpResult = "";
    	                 Hashtable objectDetails 	= (Hashtable) objectsToEvaluate.get(count);
    	                 String strGBOMRelID 		= (String) objectDetails.get("GBOMRELID");
    	                 String strRuleType 		= (String) objectDetails.get("ruleType");
    	                 String strExpression 		= (String) objectDetails.get("expression");
    	                 String partId 				= (String) objectDetails.get("partId");
    	                 List listActiveDVs 		= (StringList) objectDetails.get("listActiveDVs");                 
    	                 if (strExpression == null || strExpression.length() <= 0) 
    	                 {
    	                     partsWithoutExpression.add(partId);
    	                 }else 
    	                 {
    	                     //getting the design variants referred in the inclusion rule
    	                     int DVCount1 = 0;                     
    	                     String strDVs = (String) objectDetails.get("DesignVariants");
    	                     TreeSet DVSet  = new TreeSet();
    	                     if(strDVs != null && ! "null".equals(strDVs) && ! "".equals(strDVs))
    	                     {
    	                         StringTokenizer st = new StringTokenizer(strDVs,",");
    	                         while(st.hasMoreTokens())
    	                         {
    	                        	 DVSet.add(st.nextToken());
    	                         }
    	                     }

    	                     TreeSet activeDVSet = new TreeSet(listActiveDVs);
    	                     if(!DVSet.equals(activeDVSet) && DVSet.size()!=0)
    	                     {
    	                    	 ruleEvaluationRequired = false;
    	                     }
    	                     if(ruleEvaluationRequired)
    		                 {
    		                     if (strContextType.equalsIgnoreCase(ConfigurationConstants.TYPE_PRODUCT_VARIANT)
    		                             && isVariantExists
    		                             && isForPart
    		                             && calledFrom.equalsIgnoreCase("Part"))
    		                     {
    		                    	if (strExpResult.equalsIgnoreCase(STR_FALSE))
    		                         {
    		                             ruleEvaluationRequired = false;
    		                         }
    		                     }
    		                 }
    	                 }
    	                 //modified to make id based
    	                 if (ruleEvaluationRequired)
    	                 {
     	                 	String strUseDesignVariant = getResourceProperty(context,"emxConfiguration.Migration.EvaluateBlankRuleasValid");
    	                 	String strAttrRExp =  strExpression;
    	                 	if(strUseDesignVariant != null && !strUseDesignVariant.equals("true"))
    	                    {
    	                     	if(((strAttrRExp != null && strAttrRExp.equals(""))|| strAttrRExp == null) && !isVariantExists)
    	                     	{
    	                            strExpResult = STR_TRUE;
    	                     	}
    	                     	else if(isVariantExists)
    	                     	{
    	                     		if(strAttrRExp == null || strAttrRExp.equals("")){
    	                     			strExpResult = STR_FALSE;
    	                     		}else{
    	                     			strExpResult = evaluateIRExpression(context, strGBOMRelID,pcID,strExpression);
    	                     		}
    	                     	}
    	                     	else
    	                     	{
    	                     		strExpResult = evaluateIRExpression(context, strGBOMRelID,pcID,strExpression);
    	                     		partsWithoutExpression.remove(partId);
    	                     	}
    	                     }else
    	                     {
    	                     	if(strAttrRExp != null && strAttrRExp.equals(""))
    	                     	{
    	                     		strExpResult = STR_TRUE;
    	                     		partsWithoutExpression.remove(partId);
    	                     	}
    	                     	else if(strAttrRExp == null)
    	                     	{
    	                     		strExpResult = STR_TRUE;
    	                     		partsWithoutExpression.remove(partId);
    	                     	}else if(strAttrRExp != null && !strAttrRExp.equals(""))
    	                     	{
    	                             // Call the evaluate expression to find whether to Include or Exclude the
    	                             // part to the "Object"

    	                     		strExpResult = evaluateIRExpression(context, strGBOMRelID,pcID,strExpression);
    	                     	}
    	                     }
    	                 }
    	                 else
    	                 {
    	                 	strExpResult = STR_FALSE;
    	                 }
    	                 ruleEvaluationRequired = true;
    	                 if ((strExpResult.equals(STR_TRUE) && strRuleType.equals(ConfigurationConstants.RANGE_VALUE_INCLUSION))
    	                         || (strExpResult.equals(STR_FALSE) && strRuleType.equals(ConfigurationConstants.RANGE_VALUE_EXCLUSION))) 
    	                 {
    	                     temp.add(partId);
    	                     evaluatedObject.add(partId);
    	                 }
    	             }
    	         }
    	         temp.removeAll(partsWithoutExpression);
    	         if (calledFrom.equalsIgnoreCase("Part"))
    	         {
    	             if (temp.size() > 0)
    	             {
    	                 finalReturnParts = temp;
    	             } else
    	             {
    	                 finalReturnParts = evaluatedObject;
    	             }
    	         } else
    	         {
    	             finalReturnParts = evaluatedObject;
    	         }
    	         mqlLogRequiredInformationWriter(getTimeStamp()+" gbomParseRulesIdBased() ends..."+"\n");
    	         return finalReturnParts;
    	     }



    	 	 /**
    	      * This is used to encode selected string - Bug No. 361962
    	      * @param context
    	      * @param strObjId
    	      * @param strSelectedFeatures
    	      * @param strParams
    	      * @param featureType
    	      * @return String
    	      * @throws Exception
    	      */
    	     public static String stringDecode(Context context,String  strFeatureName)
    	             throws Exception {
    	       if(strFeatureName.indexOf("&")>-1){

    	    	     strFeatureName = strFeatureName.replaceAll("&","&amp;");

    	        }else if(strFeatureName.indexOf("<")>-1){

    	        	 strFeatureName = strFeatureName.replaceAll("<","&lt;");

    	        }else if(strFeatureName.indexOf(">")>-1){

    	        	strFeatureName = strFeatureName.replaceAll(">","&gt;");
    	        }

    	       return strFeatureName;
    	     }
    	     
    	    /**
    	     * This method is used for the updation of the BOM XML attribute on Product
    	     * Configuration
    	     *
    	     * @param context the eMatrix Context object
    	     * @param args holds the Product configuration id
    	     * @throws Exception if the operation fails
    	     */
    		private void updateBOMXMLForProductConfiguration(Context context,String pcObjectId) throws Exception
    		{
    	     	StringBuffer sbXml = new StringBuffer(200);
    	        try
    	        {
    	        	mqlLogRequiredInformationWriter(getTimeStamp()+"Update BOM XML For Id :: " +pcObjectId +"\n\n");
    	     	    DomainObject domObject = new DomainObject(pcObjectId);
    	            sbXml.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?> <EngineeringBOM>\n");
    	            StringList objSelects = new StringList(DomainConstants.SELECT_TYPE);
    	            objSelects.add(DomainConstants.SELECT_ID);
    	            objSelects.add(SELECT_PC_FROMID);
    	            objSelects.add(SELECT_PC_FROMTYPE);
    	            objSelects.add(SELECT_FPC_FROMID);
    	            objSelects.add(SELECT_FPC_FROMTYPE);
    	            objSelects.add(SELECT_SO_TO_LF_OBJECTID);
    	            objSelects.add(SELECT_SO_TO_LF_RELID);

    	            MULTI_VALUE_LIST.add(SELECT_SO_TO_LF_OBJECTID);
    	            MULTI_VALUE_LIST.add(SELECT_SO_TO_LF_RELID);

    	            Map pcInfoMap = domObject.getInfo(context, objSelects);

    	            MULTI_VALUE_LIST.remove(SELECT_SO_TO_LF_OBJECTID);
    	            MULTI_VALUE_LIST.remove(SELECT_SO_TO_LF_OBJECTID);
    	            mqlLogRequiredInformationWriter("Product Configuration Info Map :: " +pcInfoMap +"\n");
    	            String strContextObjId = (String)pcInfoMap.get(SELECT_PC_FROMID);

    	            //context object can be Logical Feature/Product Revision/Product Variant
    	            String strCtxtObjType = (String)pcInfoMap.get(SELECT_PC_FROMTYPE);
    	            String strPrdCtxtObjType = (String)pcInfoMap.get(SELECT_FPC_FROMTYPE);
    	            String strPrdCtxtObjId = (String)pcInfoMap.get(SELECT_FPC_FROMID);
    	            StringList listSelectedFeatureRelId = (StringList) pcInfoMap.get(SELECT_SO_TO_LF_RELID);

    	            MapList featureList = new MapList();
    	            //If context object is Product Variant
    	            if(strCtxtObjType!= null){
    	            	if(mxType.isOfParentType(context, strCtxtObjType,ConfigurationConstants.TYPE_PRODUCT_VARIANT))
        	            {
        	            	mqlLogRequiredInformationWriter(getTimeStamp()+"Context is Product Variant " +"\n");
        	            	strPrdCtxtObjType = strCtxtObjType;
        	                strPrdCtxtObjId = strContextObjId;
        	                mqlLogRequiredInformationWriter(getTimeStamp()+"Querying for Product Variant Logical Structure , Id:: "+strContextObjId+"\n");
        	                if(listSelectedFeatureRelId != null && !listSelectedFeatureRelId.isEmpty()) {
        	                	featureList = getPVLogicalStructure(context,strContextObjId,listSelectedFeatureRelId);
            	                mqlLogRequiredInformationWriter(getTimeStamp()+"Product Variant Logical Structure feature List Info" + featureList +"\n");
        	                }else {
        	                	mqlLogRequiredInformationWriter(getTimeStamp()+"No Logical Features, Products connected to PC "+"\n");
        	                }
        	            }//If context object is Logical Feature/Product Revision
        	            else
        	            {
        	            	mqlLogRequiredInformationWriter(getTimeStamp()+"Context is "+ strCtxtObjType +"\n");
        	            	mqlLogRequiredInformationWriter(getTimeStamp()+"Querying for "+ strCtxtObjType +" Logical Structure, Id:: "+strContextObjId +"\n");
        	            	 if(listSelectedFeatureRelId != null && !listSelectedFeatureRelId.isEmpty()) {
        	            		 featureList = getProductLogicalStructure(context, strContextObjId, listSelectedFeatureRelId);
        	            		 mqlLogRequiredInformationWriter(getTimeStamp()+"Context's Logical Structure feature List Info" + featureList +"\n");
        	            	 }else {
        	                	mqlLogRequiredInformationWriter(getTimeStamp()+"No Logical Features, Products connected to PC "+"\n");
        	                }
        	            }
    	            }

    	            Iterator itrFeatures = featureList.iterator();
    	            Map hsFeatures = null;
    	            StringList strFeatIdXML = new StringList();
    	            while (itrFeatures.hasNext())
    	            {
    	                hsFeatures = (Map) itrFeatures.next();
    	                String strFeatureId = (String) hsFeatures.get(ConfigurationConstants.SELECT_ID);
    	                String strFeatPhyId = (String) hsFeatures.get(ConfigurationConstants.SELECT_PHYSICAL_ID);
    	                //To check whether the XML for this Feature is already created or not.
    	                if(strFeatureId!= null && !strFeatIdXML.contains(strFeatureId))
    	 	           {
    	             	   // Also need to take care that the Logical Feature structure would consist of Product as well.
    	 	               String strObjectType 	 = (String) hsFeatures.get(ConfigurationConstants.SELECT_TYPE);
    	 	               String strFeatureName 	 = (String) hsFeatures.get(ConfigurationConstants.SELECT_NAME);
    	 	               String strFeatureRevision = (String) hsFeatures.get(ConfigurationConstants.SELECT_REVISION);
    	 	               String strRuleType 		 = (String) hsFeatures.get(STR_ATTRIBUTE_OPEN_BRACE
    	 									                               + ConfigurationConstants.ATTRIBUTE_RULE_TYPE
    	 									                               + EXPRESSION_CLOSE);
    	 	               String strForcePartReuse  = (String) hsFeatures.get(STR_ATTRIBUTE_OPEN_BRACE
    	 									                               + ConfigurationConstants.ATTRIBUTE_FORCE_PART_REUSE
    	 									                               + EXPRESSION_CLOSE);
    	 	               String strUsage 			 = (String) hsFeatures.get(STR_ATTRIBUTE_OPEN_BRACE
    	 									                               + ConfigurationConstants.ATTRIBUTE_USAGE
    	 									                               + EXPRESSION_CLOSE);
    	 	               String strQuantity 		 = (String) hsFeatures.get(STR_ATTRIBUTE_OPEN_BRACE
    	 									                               + ConfigurationConstants.ATTRIBUTE_QUANTITY
    	 									                               + EXPRESSION_CLOSE);


    	 	               strFeatIdXML.addElement(strFeatureId);

    	 	               strFeatureName = stringDecode(context,strFeatureName);

    	 				    sbXml.append("<Feature id=\"")
    	                      .append(strFeatureId)
    	                      .append("\" physicalid=\"")
    	                      .append(strFeatPhyId)
    	                      .append("\" type=\"")
    	                      .append(strObjectType)
    	                      .append("\" name=\"")
    	                      .append(strFeatureName)
    	                      .append("\" rev=\"")
    	                      .append(strFeatureRevision)
    	                      .append("\">\n");
    	 				    Map usageQuantityElementMap = new HashMap();
    	 				    usageQuantityElementMap.put("FeatureId",strFeatureId);
    	 				    usageQuantityElementMap.put("Usage",strUsage);
    	 				    usageQuantityElementMap.put("Quantity",strQuantity);
    	 				   sbXml.append(getUsageQuantityElementBOMXML(context,usageQuantityElementMap,pcObjectId));
    	 				    Map gbomElementMap = new HashMap();
    	 				    gbomElementMap.put("FeatureId",strFeatureId);
    	 				    gbomElementMap.put("ContextObjectType",strPrdCtxtObjType);
    	 				    gbomElementMap.put("ContextObjectId",strPrdCtxtObjId);
    	 				    gbomElementMap.put("FeatureObjectType",strObjectType);
    	 				    gbomElementMap.put("ForcePartReuse",strForcePartReuse);
    	 					//gbomElementMap.put("ForcePartReuseEnabled",strForcePartReuseEnabled);
    	 				   sbXml.append(getGBOMElementBOMXML(context,gbomElementMap,pcObjectId));
    	 	               sbXml.append("</Feature>\n");
    	 	          }
    	            }
    	            sbXml.append("</EngineeringBOM>");
    	            XMLOutputter outputter = MxXMLUtils.getOutputter(true);
    	            SAXBuilder saxb = new SAXBuilder();
	    			saxb.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
					saxb.setFeature("http://xml.org/sax/features/external-general-entities", false);
					saxb.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    	            Document document = saxb.build(new StringReader(sbXml.toString())); 
    	            //mqlLogRequiredInformationWriter("Attribute 'BOM XML' value to be set ::" + outputter.outputString(document) +"\n");
    	            domObject.setAttributeValue(context,ConfigurationConstants.ATTRIBUTE_BOMXML,outputter.outputString(document));

    	        }catch (Exception e) {
    	            throw new FrameworkException(e);
    	        }
    		}
    		/**
    		 * To the get the Product Variant Logical structure
    		 * 
    		 * @param context
    		 * @param prodVarId
    		 * @return
    		 * @throws Exception
    		 */
    		private MapList getPVLogicalStructure(Context context, String prodVarId,StringList selectedOptionRelIds) throws Exception
    		{
			
    		MapList pvFeaturesMapList = new MapList();
			try 
			{
				DomainObject domProdVarObj = DomainObject.newInstance(context,
						prodVarId);
				StringBuilder strTypePattern = new StringBuilder();
				String rlPattern = "from\\[.*\\]\\.torel\\..*";
				String ojPattern = "from\\[.*\\]\\.torel\\.to\\..*";
				String connPattern = "from\\[.*\\]\\..*";
				String selectLogicalFeatureId = "from["+ConfigurationConstants.RELATIONSHIP_PRODUCT_FEATURE_LIST+"].torel.to.id";
				
				StringList logicalStructureSelectList = new StringList();
		
				StringList objectSelects = getObjectSelects();
				StringList relSelects = getRelSelects();
	
			   for(int i = 0; i < objectSelects.size(); i++)
			   {
				   String objSelectable = "from["+ConfigurationConstants.RELATIONSHIP_PRODUCT_FEATURE_LIST+"].torel.to."+objectSelects.elementAt(i);
				   logicalStructureSelectList.add(objSelectable);
				   DomainConstants.MULTI_VALUE_LIST.add(objSelectable);
			   }
			   for(int i = 0; i < relSelects.size(); i++)
			   {
				   String relSelectable = "from["+ConfigurationConstants.RELATIONSHIP_PRODUCT_FEATURE_LIST+"].torel."+relSelects.elementAt(i);
				   logicalStructureSelectList.add(relSelectable);
				   DomainConstants.MULTI_VALUE_LIST.add(relSelectable);
			   }
			   	Map logicalFeatureDataMap = domProdVarObj.getInfo(context, logicalStructureSelectList);
			   	DomainConstants.MULTI_VALUE_LIST.clear();
				   if(logicalFeatureDataMap != null &&
						   !logicalFeatureDataMap.isEmpty())
				   {
					   Set keys = logicalFeatureDataMap.keySet();
					   Map newLogicalFeatureMap = new HashMap();  
					   int featureCount = (ConfigurationUtil.convertObjToStringList(context, logicalFeatureDataMap.get(selectLogicalFeatureId))).size();
					   for(Object key : keys)
					   {
						   String newkey = "";
						   if(((String)key).matches(ojPattern))
						   {
								   newkey = ((String)key).replaceFirst(ojPattern.substring(0, ojPattern.length()-2), "");
								   StringList valueList = ConfigurationUtil.convertObjToStringList(context, logicalFeatureDataMap.get(key));
								   createValueMapList(newkey,valueList,pvFeaturesMapList,featureCount);
						   } 
						   else if(((String)key).matches(rlPattern))
						   {
							   	   newkey = ((String)key).replaceFirst(rlPattern.substring(0, rlPattern.length()-2), "");
								   StringList valueList = ConfigurationUtil.convertObjToStringList(context,logicalFeatureDataMap.get(key));
								   createValueMapList(newkey,valueList,pvFeaturesMapList,featureCount);
						   }else if(((String)key).matches(connPattern))
						   {
							   	   newkey = ((String)key).replaceFirst(rlPattern.substring(0, connPattern.length()-2), "");
								   StringList valueList = ConfigurationUtil.convertObjToStringList(context,logicalFeatureDataMap.get(key));
								   createValueMapList(newkey,valueList,pvFeaturesMapList,featureCount);
						   }
					   }//END:for(Object key 
				   }//END:for..
					// Compare with the PC Selected List and remove the one which are not connected
					if(pvFeaturesMapList.size() > 0){
						for(Iterator listIterator = pvFeaturesMapList.iterator();listIterator.hasNext();)
						{
							Map temFeatureMap = (Map)listIterator.next();
							String featureRelId = (String)temFeatureMap.get(DomainRelationship.SELECT_ID);
							if(!(selectedOptionRelIds.contains(featureRelId)))
							{
								listIterator.remove();
							}
						}
					}
			}catch(Exception exp){
				
				throw (new FrameworkException(exp));
			}
			return pvFeaturesMapList;
    		}
    		/**
    		 * 
    		 * 
    		 * @param context
    		 * @param productId
    		 * @param selectedOptionRelIds
    		 * @return
    		 * @throws Exception
    		 */
       		private MapList getProductLogicalStructure(Context context, String productId,StringList selectedOptionRelIds) throws Exception
    		{
    		MapList prodLogicalFeatureList = new MapList();
			try 
			{
				StringBuilder strTypePattern = new StringBuilder();
				strTypePattern.append(ConfigurationConstants.TYPE_LOGICAL_STRUCTURES);
				strTypePattern.append(",");
				strTypePattern.append(ConfigurationConstants.TYPE_PRODUCTS);
				String strRelPattern = ConfigurationConstants.RELATIONSHIP_LOGICAL_FEATURES;
				
				StringList slObjSelects = getObjectSelects();
				StringList slRelSelects = getRelSelects();
                                String strLimit = EnoviaResourceBundle.getProperty(context,"emxConfiguration.Search.QueryLimit");
				 int limit = Integer.parseInt(strLimit);

				DomainObject domContextBus = new DomainObject(productId);
				prodLogicalFeatureList = domContextBus.getRelatedObjects(context,strRelPattern, strTypePattern.toString(),
									slObjSelects, slRelSelects,false, true, (short)0,
									DomainObject.EMPTY_STRING, DomainObject.EMPTY_STRING,(short)limit, DomainObject.CHECK_HIDDEN,
									DomainObject.PREVENT_DUPLICATES,(short) DomainObject.PAGE_SIZE, null, null,
				                    null, DomainObject.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short)0);

				// Compare with the PC Selected List and remove the one which are not connected
				if(prodLogicalFeatureList.size() > 0){
					for(Iterator listIterator = prodLogicalFeatureList.iterator();listIterator.hasNext();)
					{
						Map temFeatureMap = (Map)listIterator.next();
						String featureRelId = (String)temFeatureMap.get(DomainRelationship.SELECT_ID);
						if(!(selectedOptionRelIds.contains(featureRelId)))
						{
							listIterator.remove();
						}
					}
				}
			}catch(Exception exp){

				throw (new FrameworkException(exp));
			}

			return prodLogicalFeatureList;
    		}
    		/**
    		 * To get the object selectables
    		 * @return
    		 */
    		public StringList getObjectSelects(){

    			StringList slObjSelects =  new StringList();
    			slObjSelects.addElement("physicalid");
    			slObjSelects.addElement(ConfigurationConstants.SELECT_ID);
    			slObjSelects.addElement(ConfigurationConstants.SELECT_TYPE);
    			slObjSelects.addElement(ConfigurationConstants.SELECT_NAME);
    			slObjSelects.addElement(ConfigurationConstants.SELECT_REVISION);
    			slObjSelects.addElement(ConfigurationConstants.SELECT_CURRENT);
    			slObjSelects.addElement("to["
    					+ ConfigurationConstants.RELATIONSHIP_LOGICAL_FEATURES + "]");

    			slObjSelects.addElement("to["
    					+ ConfigurationConstants.RELATIONSHIP_LOGICAL_FEATURES + "]"
    					+ ".tomid["
    					+ ConfigurationConstants.RELATIONSHIP_PRODUCT_FEATURE_LIST
    					+ "].id");

    			slObjSelects.addElement("to["
    					+ ConfigurationConstants.RELATIONSHIP_LOGICAL_FEATURES + "]"
    					+ ".tomid["
    					+ ConfigurationConstants.RELATIONSHIP_PRODUCT_FEATURE_LIST
    					+ "].attribute["
    					+ ConfigurationConstants.ATTRIBUTE_FEATURE_ALLOCATION_TYPE
    					+ "]");
    			slObjSelects.addElement("from["
    					+ ConfigurationConstants.RELATIONSHIP_LOGICAL_FEATURES
    					+ "].to.id");

    			return slObjSelects;

    		}
    		/**
    		 * To get the relationship selectables
       		 * @return
    		 */
    		private StringList getRelSelects(){
    			StringList slRelSelects = new StringList();
    			slRelSelects.addElement(ConfigurationConstants.SELECT_RELATIONSHIP_ID);
    			slRelSelects
    					.addElement(ConfigurationConstants.SELECT_RELATIONSHIP_TYPE);
    			slRelSelects
    					.addElement(ConfigurationConstants.SELECT_RELATIONSHIP_NAME);
    			slRelSelects.addElement("tomid["
    					+ ConfigurationConstants.RELATIONSHIP_PRODUCT_FEATURE_LIST
    					+ "].from.id");
    			slRelSelects.addElement(SELECT_ATTRIBUTE_USAGE);
    			slRelSelects.addElement("from." + ConfigurationConstants.SELECT_ID);
    			slRelSelects.addElement("attribute["
    					+ ConfigurationConstants.ATTRIBUTE_RULE_TYPE + "]");
    			slRelSelects.addElement("attribute["
    					+ ConfigurationConstants.ATTRIBUTE_FORCE_PART_REUSE + "]");
    			slRelSelects.addElement("attribute[" + ConfigurationConstants.ATTRIBUTE_QUANTITY
    					+ "]");
    			return slRelSelects;

    		}
    	     private void createValueMapList(String key,StringList valueList,MapList mListToUpdate,int totalSize) throws FrameworkException
    	     {
    	    	 try {
    	    		 int listSize = valueList.size() < totalSize ? valueList.size():totalSize;

			 if(mListToUpdate.size() > 0)
        	    	 {
				 for(int index =0;index < listSize;index++)
        	    		 {
        	    			 Map map = (HashMap)mListToUpdate.get(index);
        	        		 String sValue = (String)valueList.get(index);
        	        		 map.put(key, sValue);
        	    		 }
        	    	 }else
        	    	 {
        	    		 for(int index =0;index < listSize;index++)
        	    		 {
        	    			 String sValue = (String)valueList.get(index);
        	    			 Map map = new HashMap();
        	    			 map.put(key, sValue);
        	    			 mListToUpdate.add(map);
        	    		 }

        	    	 }
    	    	 }catch(Exception ep){
    	    		 ep.getStackTrace();
    	    		 throw new FrameworkException(ep.getMessage());
    	    	 }

    	    }
    	     private void createSelectedOptionList(String key,StringList valueList,MapList mListToUpdate) throws FrameworkException
    	     {
    	    	 try {

    	    		 if(mListToUpdate.size() > 0)
        	    	 {
				 for(int index =0;index < valueList.size();index++)
        	    		 {
        	    			 Map map = (HashMap)mListToUpdate.get(index);
        	        		 String sValue = (String)valueList.get(index);
        	        		 map.put(key, sValue);
        	    		 }
        	    	 }else
        	    	 {
        	    		 for(int index =0;index < valueList.size();index++)
        	    		 {
        	    			 String sValue = (String)valueList.get(index);
        	    			 Map map = new HashMap();
        	    			 map.put(key, sValue);
        	    			 mListToUpdate.add(map);
        	    		 }

        	    	 }
    	    	 }catch(Exception ep){
    	    		 ep.getStackTrace();
    	    		 throw new FrameworkException(ep.getMessage());
    	    	 }

    	    }
		   private static String getTimeStamp() {
			   String timeStamp = "";
			   try{
				   SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss.SSS");
				   Date recordDate = new Date(System.currentTimeMillis());
				   timeStamp =  "["+dateFormat.format(recordDate)+"]: ";
			   }catch(Exception ep){
				   ep.printStackTrace();
			   }
			   return timeStamp;
			   }

		   /**
		    * To clear the Selected Option list
		    */
		   private void clearSelectedOptions()
			{
				if(pcSelectedOptionList != null)
					pcSelectedOptionList =  null;
			}
  }
