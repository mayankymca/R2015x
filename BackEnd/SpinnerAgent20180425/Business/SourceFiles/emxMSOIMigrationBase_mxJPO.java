/*
 * emxMSOIMigrationBase.java
 * program migrates the data into Common Document model i.e. to 10.5 schema.
 *
 * Copyright (c) 1992-2002 MatrixOne, Inc.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

  import matrix.db.*;
  import matrix.util.*;
  import java.io.*;
  import java.util.*;
  import java.text.*;
  import com.matrixone.apps.domain.*;
  import com.matrixone.apps.domain.util.*;
  import com.matrixone.apps.common.*;
  import com.matrixone.apps.common.util.*;

  import com.matrixone.MCADIntegration.server.*;
  import com.matrixone.MCADIntegration.server.beans.*;
  import com.matrixone.MCADIntegration.server.cache.*;

  public class emxMSOIMigrationBase_mxJPO
  {
      //<Change>SJ7 - 07-Nov-11 : Changes for Multitenant - Removing Static
	  public int _counter  = 0;
      public int _sequence  = 1;
      public java.io.File _oidsFile = null;
      public BufferedWriter _fileWriter = null;
      public StringList _objectidList = null;
      public int _chunk = 0;
      public String convertNonRevisionedDocument = "false";
	  // </Change> Complete

      BufferedWriter writer = null;
      FileWriter errorLog = null;
      FileWriter warningLog = null;
      FileWriter convertedOidsLog = null;

	  //<Change>SJ7 - 07-Nov-11 : Changes for Multitenant - Removing Static
	  int minRange = 0;
      int maxRange = 0;
	  	
      String documentDirectory = "";
	  //</Changes>
      /*static int minRange = 0;
      static int maxRange = 0;*/
      static String ATTRIBUTE_SUSPEND_VERSIONING                        = PropertyUtil.getSchemaProperty("attribute_SuspendVersioning");
      static String TYPE_DRAWING_PRINT                                  = PropertyUtil.getSchemaProperty("type_DrawingPrint");
      static String POLICY_VERSION                                      = PropertyUtil.getSchemaProperty("policy_Version");

      static String RELATIONSHIP_HAS_DOCUMENTS                          = PropertyUtil.getSchemaProperty("relationship_HasDocuments");
      static String RELATIONSHIP_VAULTED_OBJECTS_REV2                   = PropertyUtil.getSchemaProperty("relationship_VaultedDocumentsRev2");
      static String RELATIONSHIP_DOCUMENT_SHEETS                        = PropertyUtil.getSchemaProperty("relationship_DocumentSheets");
      static String RELATIONSHIP_REQUIREMENT_SPECIFICATION              = PropertyUtil.getSchemaProperty("relationship_RequirementSpecification");
      static String RELATIONSHIP_ORIGINATING_REQUIREMENT                = PropertyUtil.getSchemaProperty("relationship_Originating_Requirement");
      static String RELATIONSHIP_BUILD_SPECIFICATION                    = PropertyUtil.getSchemaProperty("relationship_BuildSpecification");
      static String RELATIONSHIP_PRODUCT_SPECIFICATION                  = PropertyUtil.getSchemaProperty("relationship_ProductSpecification");
      static String RELATIONSHIP_FEATURE_TEST_SPECIFICATION             = PropertyUtil.getSchemaProperty("relationship_FeatureTestSpecification");
      static String RELATIONSHIP_FEATURE_FUNCTIONAL_SPECIFICATION       = PropertyUtil.getSchemaProperty("relationship_FeatureFunctionalSpecification");
      static String RELATIONSHIP_FEATURE_DESIGN_SPECIFICATION           = PropertyUtil.getSchemaProperty("relationship_FeatureDesignSpecification");
      static String RELATIONSHIP_FEATURE_SPECIFICATION                  = PropertyUtil.getSchemaProperty("relationship_FeatureSpecification");
      public static final String FORMAT_JT = PropertyUtil.getSchemaProperty("format_JT");

      public static String TYPE_TECHNICAL_SPECIFICATION					= PropertyUtil.getSchemaProperty("type_TechnicalSpecification");
      public static String TYPE_ASSESSMENT								= PropertyUtil.getSchemaProperty("type_Assessment");
      public static String TYPE_FINANCIALS								= PropertyUtil.getSchemaProperty("type_Financials");
      public static String TYPE_RISK									= PropertyUtil.getSchemaProperty("type_Risk");
      public static String TYPE_BUSINESS_GOALS							= PropertyUtil.getSchemaProperty("type_BusinessGoal");
      public static String TYPE_QUALITY									= PropertyUtil.getSchemaProperty("type_Quality");
      public static String TYPE_GENERIC_DOCUMENT						= PropertyUtil.getSchemaProperty("type_GenericDocument");
      public static String TYPE_DOCUMENT_SHEET							= PropertyUtil.getSchemaProperty("type_DocumentSheet");
      public static final String TYPE_BUILDS                               = PropertyUtil.getSchemaProperty("type_Builds");
      public static final String TYPE_FEATURES                             = PropertyUtil.getSchemaProperty("type_Features");
      public static final String TYPE_PRODUCTS                             = PropertyUtil.getSchemaProperty("type_Products");
      public static final String TYPE_INCIDENT                             = PropertyUtil.getSchemaProperty("type_Incident");
      public static final String TYPE_REQUIREMENT                          = PropertyUtil.getSchemaProperty("type_Requirement");
      public static final String TYPE_TEST_CASE                            = PropertyUtil.getSchemaProperty("type_TestCase");
      public static final String TYPE_USE_CASE                             = PropertyUtil.getSchemaProperty("type_UseCase");
      public static final String TYPE_ISSUE                                = PropertyUtil.getSchemaProperty("type_Issue");
      public static final String TYPE_SPECIFICATION                        = PropertyUtil.getSchemaProperty("type_Specification");
	  public static final String RELATIONSHIP_ACTIVE_VERSION               = PropertyUtil.getSchemaProperty("relationship_ActiveVersion");
      public static final String RELATIONSHIP_LATEST_VERSION               = PropertyUtil.getSchemaProperty("relationship_LatestVersion");
      public static final String RELATIONSHIP_MEETING_ATTACHMENTS               = PropertyUtil.getSchemaProperty("relationship_MeetingAttachments");
      public static final String RELATIONSHIP_MESSAGE_ATTACHMENTS               = PropertyUtil.getSchemaProperty("relationship_MessageAttachments");
      public static final String RELATIONSHIP_PART_SPECIFICATION = PropertyUtil.getSchemaProperty("relationship_PartSpecification");
      public static final String RELATIONSHIP_PART_FAMILY_REFERENCE_DOCUMENT = PropertyUtil.getSchemaProperty("relationship_PartFamilyReferenceDocument");
      public static final String RELATIONSHIP_REFERENCE_DOCUMENT = PropertyUtil.getSchemaProperty("relationship_ReferenceDocument");
      public static final String RELATIONSHIP_TASK_DELIVERABLE = PropertyUtil.getSchemaProperty("relationship_TaskDeliverable");

      static String  SELECT_FROM_LATEST_VERSION_RELATIONSHIP_ID          = "from[" + RELATIONSHIP_LATEST_VERSION + "].id";
      static String  SELECT_TO_LATEST_VERSION_RELATIONSHIP_ID            = "to[" + RELATIONSHIP_LATEST_VERSION + "].id";
      static String  SELECT_VERSION_RELATIONSHIP_ID                      = "from[" + DomainObject.RELATIONSHIP_VERSION + "].id";
      static String  SELECT_VERSION_OBJECT_ID                            = "from[" + DomainObject.RELATIONSHIP_VERSION + "].to.id";
      static String  SELECT_VERSION_OBJECT_NAME                          = "from[" + DomainObject.RELATIONSHIP_VERSION + "].to." + DomainObject.SELECT_NAME;
      static String  SELECT_VERSION_OBJECT_REVISION                      = "from[" + DomainObject.RELATIONSHIP_VERSION + "].to." + DomainObject.SELECT_REVISION;
      static String  SELECT_VERSION_OBJECT_TITLE                         = "from[" + DomainObject.RELATIONSHIP_VERSION + "].to.attribute[" + DomainObject.ATTRIBUTE_TITLE + "]";
      static String  SELECT_VERSION_OBJECT_FILE_VERSION                  = "from[" + DomainObject.RELATIONSHIP_VERSION + "].to.attribute[" + DomainObject.ATTRIBUTE_FILE_VERSION + "]";
      static String  SELECT_VERSION_OBJECT_FILE_NAME                     = "from[" + DomainObject.RELATIONSHIP_VERSION + "].to." + DomainObject.SELECT_FILE_NAME;
      static String  SELECT_VERSION_OBJECT_FILE_FORMAT                   = "from[" + DomainObject.RELATIONSHIP_VERSION + "].to." + DomainObject.SELECT_FILE_FORMAT;
      static String  SELECT_VERSION_OBJECT_HASFILES                      = "from[" + DomainObject.RELATIONSHIP_VERSION + "].to." + DomainObject.SELECT_FORMAT_HASFILE;
      static String  SELECT_TEAM_FOLDER_ID                               = "to[" + DomainObject.RELATIONSHIP_VAULTED_OBJECTS + "].from.id";
      static String  SELECT_FROM_ID                                      = "to.from.id";
      static String  SELECT_FROM_RELATIONSHIP_NAME                       = "to.name";
      static String  SELECT_FROM_TYPE                                    = "to.from.type";
      static String  SELECT_FILE_VERSION                                 = "attribute[" + DomainObject.ATTRIBUTE_FILE_VERSION + "]";
      static String  SELECT_TITLE                                        = "attribute[" + DomainObject.ATTRIBUTE_TITLE + "]";

      static String SELECT_RELATIONSHIP_PART_SPECIFICATION_FROM_TYPE = "to[" + RELATIONSHIP_PART_SPECIFICATION + "].from.type";
      static String SELECT_RELATIONSHIP_PART_FAMILY_REFERENCE_DOCUMENT_FROM_TYPE = "to[" + RELATIONSHIP_PART_FAMILY_REFERENCE_DOCUMENT + "].from.type";
      static String SELECT_RELATIONSHIP_REFERENCE_DOCUMENT_FROM_TYPE = "to[" + RELATIONSHIP_REFERENCE_DOCUMENT + "].from.type";
      static String SELECT_RELATIONSHIP_REFERENCE_DOCUMENT_FROM_STATE = "to[" + RELATIONSHIP_REFERENCE_DOCUMENT + "].from.current";


      static String SELECT_RELATIONSHIP_VAULTED_OBJECTS_FROM_TYPE = "to[" + DomainObject.RELATIONSHIP_VAULTED_OBJECTS + "].from.type";
      static String SELECT_RELATIONSHIP_MESSAGE_ATTACHMENTS_FROM_TYPE = "to[" + RELATIONSHIP_MESSAGE_ATTACHMENTS + "].from.type";
      static String SELECT_RELATIONSHIP_MEETING_ATTACHMENTS_FROM_TYPE = "to[" + RELATIONSHIP_MEETING_ATTACHMENTS + "].from.type";
      static String SELECT_RELATIONSHIP_TASK_DELIVERABLE_FROM_TYPE = "to[" + RELATIONSHIP_TASK_DELIVERABLE + "].from.type";
      static String SELECT_RELATIONSHIP_HAS_DOCUMENTS_FROM_TYPE = "to[" + RELATIONSHIP_HAS_DOCUMENTS + "].from.type";
      static String SELECT_RELATIONSHIP_VAULTED_OBJECTS_REV2_FROM_TYPE = "to[" + RELATIONSHIP_VAULTED_OBJECTS_REV2 + "].from.type";
      static String SELECT_TYPE_DOCUMENT_SHEET_FROM_TYPE = "to[" + TYPE_DOCUMENT_SHEET + "].from.type";

      // For getting state of RFQ when Document is connected to Line Item
      static String SELECT_LINE_ITEM_RFQ_STATE = "to[" + DomainObject.RELATIONSHIP_REFERENCE_DOCUMENT + "].from.to["+ DomainObject.RELATIONSHIP_LINE_ITEM +"].from.current";
      // For getting state of RFQ when Document is connected to Line Item Split
      static String SELECT_LINE_ITEM_SPLIT_RFQ_STATE = "to[" + DomainObject.RELATIONSHIP_REFERENCE_DOCUMENT + "].from.to["+ DomainObject.RELATIONSHIP_LINE_ITEM_SPLIT +"].from.to["+ DomainObject.RELATIONSHIP_LINE_ITEM +"].from.current";
      // For getting state of RFQ Quotation when Document is connected to Supplier Line Item
      static String SELECT_SUPPLIER_LINE_ITEM_RFQ_QUOTATION_STATE = "to[" + DomainObject.RELATIONSHIP_REFERENCE_DOCUMENT + "].from.to["+ DomainObject.RELATIONSHIP_SUPPLIER_LINE_ITEM +"].from.current";
      // For getting state of RFQ Quotation when Document is connected to ECR
      static String SELECT_ECR_RFQ_QUOTATION_STATE = "to[" + DomainObject.RELATIONSHIP_REFERENCE_DOCUMENT + "].from.to["+ DomainObject.RELATIONSHIP_ECR_REFERENCE +"].from.to["+ DomainObject.RELATIONSHIP_SUPPLIER_LINE_ITEM +"].from.current";

      int TEAM_SOURCING = 1;
      int PMC = 2;
      int DOCUMENT_PRODUCT_SPEC = 3;
      int EC = 4;
      int IEF = 5;
      int UNKNOWN_MODEL = 99;
	  
	  //<Change>SJ7 - 07-Nov-11 : Changes for Multitenant - Removing Static
      StringList mxMainObjectSelects = new StringList(51);
	  //</Change>

	  public static final String TYPE_MCAD_GLOBAL_CONFIG		= PropertyUtil.getSchemaProperty("type_MCADInteg-GlobalConfig");
	  public static final String TYPE_MCAD_MODEL				= PropertyUtil.getSchemaProperty("type_MCADModel");
	  public static final String TYPE_ECAD_MODEL				= PropertyUtil.getSchemaProperty("type_ECADModel");
	  public static final String TYPE_MCAD_DRAWING				= PropertyUtil.getSchemaProperty("type_MCADDrawing");
	  public static final String TYPE_MCAD_VERSIONED_DRAWING	= PropertyUtil.getSchemaProperty("type_MCADVersionedDrawing");
	  public static final String TYPE_COMPOUND_DOCUMENT			= PropertyUtil.getSchemaProperty("type_CompoundDocument");
	  public static final String ATTR_MCAD_TYPE_FORMAT_MAPPING	= PropertyUtil.getSchemaProperty("attribute_MCADInteg-TypeFormatMapping");
	  public static final String ATTR_SOURCE					= PropertyUtil.getSchemaProperty("attribute_Source");
	  public static final String ATTR_ISFINALIZED				= PropertyUtil.getSchemaProperty("attribute_IsFinalized");
	  public static final String REL_VERSIONOF					= PropertyUtil.getSchemaProperty("relationship_VersionOf");
	  public static final String REL_FINALIZED					= PropertyUtil.getSchemaProperty("relationship_Finalized");
	  public static final String POLICY_CAD_MODEL_MINOR			= PropertyUtil.getSchemaProperty("policy_CADModelMinorPolicy");
      public static final String ATTRIBUTE_ISVERSIONOBJECT      = PropertyUtil.getSchemaProperty("attribute_IsVersionObject");
      public static final String ATTRIBUTE_TITLE				= PropertyUtil.getSchemaProperty("attribute_Title");
      public static final String ATTRIBUTE_MOVEFILESTOVERSION   = PropertyUtil.getSchemaProperty("attribute_MoveFilesToVersion");
      public static final String ATTRIBUTE_ORIGINATOR			= PropertyUtil.getSchemaProperty("attribute_Originator");
	  
	  public static final String IEF_NONE					= "NONE";
	  public static final String IEF_MAJOR_NORMAL			= "MAJOR";
	  public static final String IEF_MINOR_NORMAL			= "MINOR";
	  public static final String IEF_MAJOR_WITHOUT_MINOR	= "FINALIZED_MAJOR";
	  
	  public static final String SELECT_TO_VERSION_OF_RELATIONSHIP_ID		= "to[" + REL_VERSIONOF + "].from.id";
	  public static final String SELECT_FROM_VERSION_OF_RELATIONSHIP_ID		= "from[" + REL_VERSIONOF + "].to.id";
	  public static final String SELECT_ATTR_SOURCE							= "attribute[" + ATTR_SOURCE + "]";
	  public static final String SELECT_TO_VERSION_OF_RELATIONSHIP_ATTR_IS_FINALIZED	= "to[" + REL_VERSIONOF + "].attribute[" + ATTR_ISFINALIZED + "]";
	  public static final String SELECT_TO_FINALIZED_RELATIONSHIP_ID		= "to[" + REL_FINALIZED + "].from.id";
	  public static final String SELECT_TO_VERSION_OF_RELATIONSHIP_FILES	= "to[" + REL_VERSIONOF + "].from.format.file.name";
	  public static final String SELECT_TO_VERSION_OF_RELATIONSHIP_FORMATS	= "to[" + REL_VERSIONOF + "].from.format.file.format";

	  //<Change>SJ7 - 07-Nov-11 : Changes for Multitenant - Removing Static
	  private boolean isLogEnabled = true;
	  //</Change>

	  private MCADMxUtil	mxUtil				= null;
      private IEFCache  _GlobalCache                      = new IEFGlobalCache();
	  private FileWriter	iefLog				= null;
	  
	  //<Change>SJ7 - 07-Nov-11 : Changes for Multitenant - Removing Static
	  private HashMap typeFormatMapping		= null;
	  private Vector typeListIEFTypes		= new Vector();
	  private Vector typeListNonIEFTypes		= new Vector();
	  //</Change>

      static
      {
          DomainObject.MULTI_VALUE_LIST.add(SELECT_VERSION_RELATIONSHIP_ID);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_VERSION_OBJECT_ID);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_VERSION_OBJECT_NAME);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_VERSION_OBJECT_REVISION);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_VERSION_OBJECT_TITLE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_VERSION_OBJECT_FILE_VERSION);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_VERSION_OBJECT_FILE_NAME);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_VERSION_OBJECT_FILE_FORMAT);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_VERSION_OBJECT_HASFILES);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_FROM_ID);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_FROM_TYPE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_FROM_RELATIONSHIP_NAME);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_RELATIONSHIP_PART_SPECIFICATION_FROM_TYPE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_RELATIONSHIP_PART_FAMILY_REFERENCE_DOCUMENT_FROM_TYPE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_RELATIONSHIP_REFERENCE_DOCUMENT_FROM_TYPE);

          DomainObject.MULTI_VALUE_LIST.add(SELECT_RELATIONSHIP_VAULTED_OBJECTS_FROM_TYPE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_RELATIONSHIP_MESSAGE_ATTACHMENTS_FROM_TYPE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_RELATIONSHIP_MEETING_ATTACHMENTS_FROM_TYPE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_RELATIONSHIP_TASK_DELIVERABLE_FROM_TYPE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_RELATIONSHIP_HAS_DOCUMENTS_FROM_TYPE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_RELATIONSHIP_VAULTED_OBJECTS_REV2_FROM_TYPE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_TYPE_DOCUMENT_SHEET_FROM_TYPE);

          DomainObject.MULTI_VALUE_LIST.add(SELECT_LINE_ITEM_RFQ_STATE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_LINE_ITEM_SPLIT_RFQ_STATE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_SUPPLIER_LINE_ITEM_RFQ_QUOTATION_STATE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_ECR_RFQ_QUOTATION_STATE);
          DomainObject.MULTI_VALUE_LIST.add(SELECT_RELATIONSHIP_REFERENCE_DOCUMENT_FROM_STATE);

      }

	  //<Change>SJ7 - 07-Nov-11 : Changes for Multitenant - Removing Static
      public Map typeMapping = new HashMap();
	  //</Change>		

      long startTime = System.currentTimeMillis();
      long migrationStartTime = System.currentTimeMillis();

      boolean isConverted = false;
      boolean scan = false;
      String error = null;

      /**
      *
      * @param context the eMatrix <code>Context</code> object
      * @param args holds no arguments
      * @throws Exception if the operation fails
      * @grade 0
      */
      public emxMSOIMigrationBase_mxJPO (Context context, String[] args)
              throws Exception
      {
        writer     = new BufferedWriter(new MatrixWriter(context));
	    mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle("en-US"),_GlobalCache);
          mxMainObjectSelects.add(DomainObject.SELECT_FILE_FORMAT);
          mxMainObjectSelects.add(DomainObject.SELECT_FILE_NAME);
          mxMainObjectSelects.add(DomainObject.SELECT_FORMAT_HASFILE);
          mxMainObjectSelects.add(DomainObject.SELECT_TYPE);
          mxMainObjectSelects.add(DomainObject.SELECT_NAME);
          mxMainObjectSelects.add(DomainObject.SELECT_REVISION);
          mxMainObjectSelects.add(DomainObject.SELECT_VAULT);
          mxMainObjectSelects.add(DomainObject.SELECT_POLICY);
          mxMainObjectSelects.add(DomainObject.SELECT_OWNER);
          mxMainObjectSelects.add(DomainObject.SELECT_ID);
          mxMainObjectSelects.add(DomainObject.SELECT_LOCKED);
          mxMainObjectSelects.add(DomainObject.SELECT_LOCKER);
          mxMainObjectSelects.add(DomainObject.SELECT_ORIGINATED);
          mxMainObjectSelects.add("last.id");
          mxMainObjectSelects.add("previous.id");
          mxMainObjectSelects.add(SELECT_TITLE);

          mxMainObjectSelects.add(SELECT_FROM_RELATIONSHIP_NAME);
          mxMainObjectSelects.add(SELECT_FROM_ID);
          mxMainObjectSelects.add(SELECT_FROM_TYPE);

          mxMainObjectSelects.add(SELECT_VERSION_RELATIONSHIP_ID);
          mxMainObjectSelects.add(SELECT_VERSION_OBJECT_ID);
          mxMainObjectSelects.add(SELECT_VERSION_OBJECT_NAME);
          mxMainObjectSelects.add(SELECT_VERSION_OBJECT_REVISION);
          mxMainObjectSelects.add(SELECT_VERSION_OBJECT_TITLE);
          mxMainObjectSelects.add(SELECT_VERSION_OBJECT_FILE_VERSION);
          mxMainObjectSelects.add(SELECT_VERSION_OBJECT_FILE_NAME);
          mxMainObjectSelects.add(SELECT_VERSION_OBJECT_FILE_FORMAT);
          mxMainObjectSelects.add(SELECT_VERSION_OBJECT_HASFILES);

          mxMainObjectSelects.add(SELECT_RELATIONSHIP_PART_SPECIFICATION_FROM_TYPE);
          mxMainObjectSelects.add(SELECT_RELATIONSHIP_PART_FAMILY_REFERENCE_DOCUMENT_FROM_TYPE);
          mxMainObjectSelects.add(SELECT_RELATIONSHIP_REFERENCE_DOCUMENT_FROM_TYPE);
          mxMainObjectSelects.add(SELECT_RELATIONSHIP_VAULTED_OBJECTS_FROM_TYPE);
          mxMainObjectSelects.add(SELECT_RELATIONSHIP_MESSAGE_ATTACHMENTS_FROM_TYPE);
          mxMainObjectSelects.add(SELECT_RELATIONSHIP_MEETING_ATTACHMENTS_FROM_TYPE);
          mxMainObjectSelects.add(SELECT_RELATIONSHIP_TASK_DELIVERABLE_FROM_TYPE);
          mxMainObjectSelects.add(SELECT_RELATIONSHIP_HAS_DOCUMENTS_FROM_TYPE);
          mxMainObjectSelects.add(SELECT_RELATIONSHIP_VAULTED_OBJECTS_REV2_FROM_TYPE);
          mxMainObjectSelects.add(SELECT_TYPE_DOCUMENT_SHEET_FROM_TYPE);
          mxMainObjectSelects.add(SELECT_FROM_LATEST_VERSION_RELATIONSHIP_ID);
          mxMainObjectSelects.add(SELECT_TO_LATEST_VERSION_RELATIONSHIP_ID);

		  mxMainObjectSelects.add(DomainObject.SELECT_ID);
		  mxMainObjectSelects.add(DomainObject.SELECT_TYPE);
          mxMainObjectSelects.add(DomainObject.SELECT_NAME);
      }      

      /**
       * This method is executed if a specific method is not specified.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param args holds no arguments
       * @returns nothing
       * @throws Exception if the operation fails
       */
      public int mxMain(Context context, String[] args) throws Exception
      {
          if(!context.isConnected())
          {
              throw new Exception("not supported on desktop client");
          }
          int argsLength = args.length;
          error = "";

          String command = "print program eServiceSystemInformation.tcl select property[appVersionIEF] dump |";
          String result  = MqlUtil.mqlCommand(context, command);

          boolean iefVersion = false;
          boolean iefStatus = false;

          if( result == null || "null".equals(result) || "".equals(result))
          {
              iefStatus = true;
          }
          else if ( result.length() > 0)
          {
              result = result.substring(result.indexOf(" value ") + 7);

              if(result != null && "10-5".equalsIgnoreCase(result))
              {
                  iefStatus = true;
              }
          }

          if(!iefStatus)
          {
              writer.write("=================================================================\n");
              writer.write("IEF should be Upgraded to 10-5 before data migration\n");
			  writer.write("After that, IEF data should be migrated to 10-5 and then this MSOI migration program should be run\n");
              writer.write("Step 2 of Migration :     FAILED \n");
              writer.write("=================================================================\n");

              // if scan is true, writer will be closed by the caller
              if(!scan)
              {
                  writer.close();
              }
              return 0;
          }

          try
          {
              // writer     = new BufferedWriter(new MatrixWriter(context));
              if (args.length < 3 )
              {
                  error = "Wrong number of arguments";
                  throw new IllegalArgumentException();
              }
              documentDirectory = args[0];
              minRange = Integer.parseInt(args[1]);

              if ("n".equalsIgnoreCase(args[2]))
              {
                maxRange = getTotalFilesInDirectory();
              } else {
                maxRange = Integer.parseInt(args[2]);
              }

              if (minRange > maxRange)
              {
                error = "Invalid range for arguments, minimum is greater than maximum range value";
                throw new IllegalArgumentException();
              }

              if (minRange == 0 || minRange < 1 || maxRange == 0 || maxRange < 1)
              {
                error = "Invalid range for arguments, minimum/maximum range value is 0 or negative";
                throw new IllegalArgumentException();
              }
          }
          catch (IllegalArgumentException iExp)
          {
              writer.write("====================================================================\n");
              writer.write(error + " \n");
              writer.write("Step 2 of Migration :     FAILED \n");
              writer.write("====================================================================\n");
              // if scan is true, writer will be closed by the caller
              if(!scan)
              {
                  writer.close();
              }
              return 0;
          }

          try
          {
              //errorLog   = new FileWriter(documentDirectory + "unConvertedObjectIds.csv", true);
              //errorLog.write("MASTER OID,TYPE,NAME,REVISION,CLASSIFICATION,VERSION OID,LOCKER,COMMENTS\n");
              //errorLog.flush();
              //convertedOidsLog    = new FileWriter(documentDirectory + "convertedIds.txt", true);
              warningLog = new FileWriter(documentDirectory + "MSOIWarning.log", true);
          }
          catch(FileNotFoundException fExp)
          {
              // check if user has access to the directory
              // check if directory exists
              writer.write("=================================================================\n");
              writer.write("Directory does not exist or does not have access to the directory\n");
              writer.write("Step 2 of Migration :     FAILED \n");
              writer.write("=================================================================\n");
              // if scan is true, writer will be closed by the caller
              if(!scan)
              {
                  writer.close();
              }
              return 0;
          }

          int i = 0;
          try
          {
              ContextUtil.pushContext(context);
              String cmd = "trigger off";
              MqlUtil.mqlCommand(context, cmd);
              writer.write("=======================================================\n\n");
              writer.write("                Migrating Office Integration Objects...\n");
              writer.write("                File (" + minRange + ") to (" + maxRange + ")\n");
              writer.write("                Reading files from: " + documentDirectory + "\n");
              writer.write("                Objects which cannot be migrated will be written to:  MSOIUnConvertedObjectIds.log\n");
              writer.write("                Logging of this Migration will be written to: MSOImigration.log\n\n");
              writer.write("=======================================================\n\n");
              writer.flush();

              migrationStartTime = System.currentTimeMillis();
              for( i = minRange;i <= maxRange; i++)
              {
                  try
                  {
                      ContextUtil.startTransaction(context,true);
                      writer.write("Reading file: " + i + "\n");
                      StringList objectList = new StringList();
                      try
                      {
                          objectList = readFiles(i);
                      }
                      catch(FileNotFoundException fnfExp)
                      {
						  // throw exception if file does not exists
                          throw fnfExp;
						  
                      }

                      startTime = System.currentTimeMillis();
                      identifyModel(context,objectList);

                      logWarning("<<< Time taken for migration of objects in file in milliseconds :" + documentDirectory + "MSOIdocumentobjectids_" + i + ".txt"+ ":=" +(System.currentTimeMillis() - startTime) + ">>>\n");

                      ContextUtil.commitTransaction(context);

                      // write after completion of each file
                      writer.write("=================================================================\n");
                      writer.write("Migration of Documents in file MSOIdocumentobjectids_" + i + ".txt COMPLETE \n");
                      writer.write("=================================================================\n");
                      writer.flush();
                  }
                  catch(FileNotFoundException fnExp)
                  {
                      // log the error and proceed with migration for remaining files
                      writer.write("=================================================================\n");
                      writer.write("File MSOIdocumentobjectids_" + i + ".txt does not exist \n");
                      writer.write("=================================================================\n");
                      ContextUtil.abortTransaction(context);
                  }
                  catch (Exception exp)
                  {
                      // abort if identifyModel or migration fail for a specific file
                      // continue the migration process for the remaining files
                      writer.write("=======================================================\n");
                      writer.write("Migration of Documents in file MSOIdocumentobjectids_" + i + ".txt FAILED \n");
                      writer.write("=======================================================\n");
                      exp.printStackTrace();
                      ContextUtil.abortTransaction(context);

                  }
              }

              writer.write("=======================================================\n");
              writer.write("                Migrating Office Integration Objects  COMPLETE\n");
              writer.write("                Time: " + (System.currentTimeMillis() - migrationStartTime) + " ms\n");
              writer.write(" \n");
              writer.write("Step 2 of Migration :     SUCCESS \n");
              writer.write(" \n");
              writer.write("                Objects which cannot be migrated will be written to:  MSOIUnConvertedObjectIds.log\n");
              writer.write("                Logging of this Migration will be written to: MSOImigration.log\n\n");
              writer.write("=======================================================\n");
              writer.flush();
          }
          catch (FileNotFoundException fEx)
          {
              ContextUtil.abortTransaction(context);
          }
          catch (Exception ex)
          {
              // abort if identifyModel fail
              writer.write("=======================================================\n");
              writer.write("Migration of Documents in file MSOIdocumentobjectids_" + i + ".txt failed \n");
              writer.write("Step 2 of Migration     : FAILED \n");
              writer.write("=======================================================\n");

              ex.printStackTrace();
              ContextUtil.abortTransaction(context);
          }
          finally
          {
              logWarning("<<< Total time taken for migration in milliseconds :=" + (System.currentTimeMillis() - migrationStartTime) + ">>>\n");
              
              ContextUtil.popContext(context);
              // if scan is true, writer will be closed by the caller
              if(!scan)
              {
                  writer.close();
              }
              //errorLog.close();
              warningLog.close();
              //convertedOidsLog.close();
          }

          // always return 0, even this gives an impression as success
          // this way, matrixWriter writes to console
          // else writer.write statements do not show up in Application console
          // but it works in mql console
          return 0;
      }

      /**
       * This method goes thru all the Objects in files
       * but does NOT migrate any, but finds all the unConvertable Objects to the file
       * written to provide a way to see all the unConvertable Objects before
       * running the migration
       *
       * @param context the eMatrix <code>Context</code> object
       * @param writer - MatrixWriter object sent from calling JPO.
       * @param args - Context, directory name where files exist, Minimum range, Maximum range
       * @throws Exception if the operation fails
       */
      public void scanObjects(Context context, Map map) throws Exception
      {
          writer = (BufferedWriter)map.get("writer");
          String[] args = (String[])map.get("args");

          scan = true;
          mxMain(context, args);

          return;
      }

      /**
       * This method returns the total number of files in the directory.
       *
       * @returns int of total files present in the directory
       * @throws Exception if the operation fails
       */
      public int getTotalFilesInDirectory() throws Exception
      {
          int totalFiles = 0;
          try
          {
              String[] fileNames = null;
              java.io.File file = new java.io.File(documentDirectory);
              if(file.isDirectory())
              {
                  fileNames = file.list();
              } else {
                  throw new IllegalArgumentException();
              }
              for (int i=0; i<fileNames.length; i++)
              {
                  if(fileNames[i].startsWith("MSOIdocumentobjectids_"))
                  {
                      totalFiles = totalFiles + 1;
                  }
              }
          }
          catch(Exception fExp)
          {
              // check if user has access to the directory
              // check if directory exists
              error = "Directory does not exist or does not have access to the directory";
              throw fExp;
          }

          return totalFiles;
      }

      /**
       * This method reads the contents of the file and puts in Arraylist.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param args i holds the suffux of filename to identify the file.
       * @returns ArrayList of objectIds present in the file
       * @throws Exception if the operation fails
       */
      public StringList readFiles(int i) throws Exception
      {
          String objectId = "";
          StringList objectIds = new StringList();
          try
          {
              java.io.File file = new java.io.File(documentDirectory + "MSOIdocumentobjectids_" + i + ".txt");
              BufferedReader fileReader = new BufferedReader(new FileReader(file));
              while((objectId = fileReader.readLine()) != null)
              {
                objectIds.add(objectId);
              }
          }
          catch(FileNotFoundException fExp)
          {
              throw fExp;
          }
          return objectIds;
      }


      /**
       * This method reads the contents of the file and puts in Arraylist.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param args i holds the suffux of filename to identify the file.
       * @returns ArrayList of objectIds present in the file
       * @throws Exception if the operation fails
       */
      public StringList readFiles(String fileName) throws Exception
      {
          String objectId = "";
          StringList objectIds = new StringList();
          try
          {
              java.io.File file = new java.io.File(documentDirectory + fileName);
              BufferedReader fileReader = new BufferedReader(new FileReader(file));
              while((objectId = fileReader.readLine()) != null)
              {
                objectIds.add(objectId);
              }
          }
          catch(FileNotFoundException fExp)
          {
              throw fExp;
          }
          return objectIds;
      }


      /**
       * This method identifies the model and invokes the relevant module for migration
       *
       * @param context the eMatrix <code>Context</code> object
       * @param args objectIdList has the list of Object Ids
       * @returns nothing
       * @throws Exception if the operation fails
       */
      public void identifyModel(Context context,StringList objectIdList) throws Exception
      {

          String[] oidsArray = new String[objectIdList.size()];
          oidsArray = (String[])objectIdList.toArray(oidsArray);
          StringList selects = getIEFSelectables(context);
          mxMainObjectSelects.addAll(selects);
          MapList mapList = DomainObject.getInfo(context, oidsArray, mxMainObjectSelects);
          Iterator itr = mapList.iterator();
		  boolean isIEF_Doc = false;

          // These are the variable used in following while look
          // Declared here to avoid declaring multiple times in loops.
          // not sure any performance we get or not??
          Map map = new HashMap();

          int model = 0;
          String errorMessage = "";
          while( itr.hasNext())
          {
              isConverted = false;
              map = (Map) itr.next();                  

			  isIEF_Doc = validateIEFModel(context, map);

              logWarning("\n");
              logWarning("The Current Object "+ map.get(DomainObject.SELECT_ID) +" is in Model =" + model + "\n");
              logWarning("Object TNR :" + map.get(DomainObject.SELECT_TYPE) + " " +map.get(DomainObject.SELECT_NAME) + " " + map.get(DomainObject.SELECT_REVISION) + "\n");            
              logWarning("\n");

			  if(isIEF_Doc)
				  migrateIEFModel(context, map);

          }
      }

      private void logUnableToLockIds (String message) throws Exception
      {
          /*errorLog.write(message + "\n");
          errorLog.flush();*/
      }

      private void logMigratedOids (String objectId) throws Exception
      {
          /*convertedOidsLog.write( objectId + "\n");
          convertedOidsLog.flush();*/
      }

      private void logWarning (String message) throws Exception
      {
          warningLog.write( message );
          warningLog.flush();
      }

      private void logWarning (String message, Map map) throws Exception
      {
          writer.write("!!! WARNING !!! Object TNRV = " + map.get(DomainObject.SELECT_TYPE) +
                             " " + map.get(DomainObject.SELECT_NAME) +
                             " " + map.get(DomainObject.SELECT_REVISION) +
                             " " + map.get(DomainObject.SELECT_VAULT) + "\n" +
                             " Id:" + map.get(DomainObject.SELECT_ID) + "\n");
          writer.write("Above Object has Following warning \n" + message + "\n");
          writer.flush();

          warningLog.write("!!! WARNING !!! Object TNRV = " + map.get(DomainObject.SELECT_TYPE) +
                             " " + map.get(DomainObject.SELECT_NAME) +
                             " " + map.get(DomainObject.SELECT_REVISION) +
                             " " + map.get(DomainObject.SELECT_VAULT) + "\n" +
                             " Id:" + map.get(DomainObject.SELECT_ID) + "\n");
          warningLog.write("Above Object has Following warning \n" + message + "\n");
          warningLog.write("\n");
          warningLog.flush();
      }

	public StringList getIEFSelectables(Context context) throws Exception
    {
		StringList iefObjectSelects = new StringList();

		DomainObject.MULTI_VALUE_LIST.add(SELECT_TO_VERSION_OF_RELATIONSHIP_ID);
		DomainObject.MULTI_VALUE_LIST.add(SELECT_TO_VERSION_OF_RELATIONSHIP_ATTR_IS_FINALIZED);
		DomainObject.MULTI_VALUE_LIST.add(SELECT_TO_VERSION_OF_RELATIONSHIP_FILES);
		DomainObject.MULTI_VALUE_LIST.add(SELECT_TO_VERSION_OF_RELATIONSHIP_FORMATS);

		iefObjectSelects.add(SELECT_ATTR_SOURCE);
		iefObjectSelects.add(SELECT_TO_VERSION_OF_RELATIONSHIP_ID);
		iefObjectSelects.add(SELECT_FROM_VERSION_OF_RELATIONSHIP_ID);
		iefObjectSelects.add(SELECT_TO_VERSION_OF_RELATIONSHIP_ATTR_IS_FINALIZED);
		iefObjectSelects.add(SELECT_TO_FINALIZED_RELATIONSHIP_ID);
		iefObjectSelects.add(SELECT_TO_VERSION_OF_RELATIONSHIP_FILES);
		iefObjectSelects.add(SELECT_TO_VERSION_OF_RELATIONSHIP_FORMATS);

        return iefObjectSelects;
    }

	public boolean validateIEFModel(Context context, Map map) throws Exception
    {
		String iefType = IEF_NONE;
		startIEFLog();
        try
		{
			iefType = getIEFType(context, map);
		}
		catch(Exception e)
		{
			System.out.println("Exception occured in determining IEF Type. " + e.getMessage());
		}

		endIEFLog();

		if(iefType.equals(IEF_NONE))
			return false;
		else
			return true;
    }

	public boolean migrateIEFModel(Context context , Map map) throws Exception
    {
        boolean isConverted = false;
		startIEFLog();

		String objectId = (String)map.get(DomainObject.SELECT_ID);
		if(objectId == null || "".equals(objectId) || "null".equals(objectId))
		{
			log("ERROR - ID is null");
		}
		else
		{
			log("Migrating IEF model - ID: " + objectId);
		}

		try
		{
			String iefType = getIEFType(context, map);
			log("IEF Type = " + iefType);
			if(iefType.equals(IEF_MAJOR_NORMAL))
			{
				migrateMajor(context, map);
				isConverted = true;
				writeToConverted(objectId);
			}
			else if(iefType.equals(IEF_MINOR_NORMAL))
			{
				isConverted = true;
				writeToSkipped(objectId);
			}
			else if(iefType.equals(IEF_MAJOR_WITHOUT_MINOR))
			{
				migrateMajorWithoutMinor(context, map);
				isConverted = true;
				writeToConverted(objectId);
			}
			else
			{
				writeToUnconverted(objectId);
			}
		}
		catch(Exception e)
		{
			log("ERROR: " + e.getMessage());
			isConverted = false;
			writeToUnconverted((String)map.get(DomainObject.SELECT_ID));
		}

		log("Object isConverted " + isConverted);
		endIEFLog();
		return isConverted;
    }

	private String getIEFType(Context context, Map map) throws Exception
	{
		String sIEFType = IEF_NONE;

		String type = (String)map.get(DomainObject.SELECT_TYPE);
		if(isMcadEcadModel(context, type))
		{
			StringList versionOfToId   = (StringList)map.get(SELECT_TO_VERSION_OF_RELATIONSHIP_ID);
			String versionOfFromId = (String)map.get(SELECT_FROM_VERSION_OF_RELATIONSHIP_ID);

			if(versionOfToId != null)
			{
				sIEFType = IEF_MAJOR_NORMAL;
			}
			else if(versionOfFromId != null)
			{
				sIEFType = IEF_MINOR_NORMAL;
			}
			else if(getFilesOfReleventFormat(context, map) != null)
			{
				sIEFType = IEF_MAJOR_WITHOUT_MINOR;
			}
		}

		return sIEFType;
	}

	private boolean isMcadEcadModel(Context context, String type) throws Exception
	{
		if(typeListIEFTypes.contains(type))
			return true;
		else if(typeListNonIEFTypes.contains(type))
			return false;

		String parentType = type;
		while(parentType != null && !parentType.equals(""))
		{
			if(TYPE_COMPOUND_DOCUMENT.equals(parentType) )
			{
				typeListIEFTypes.addElement(type);
				return true;
			}

			BusinessType busType = new BusinessType(parentType, context.getVault());
			parentType = busType.getParent(context);
		}

		typeListNonIEFTypes.addElement(type);
		return false;
	}

	private void migrateMajor(Context context, Map map) throws Exception
	{
		String majorID				= (String)map.get(DomainObject.SELECT_ID);
		DomainObject majorObject	= DomainObject.newInstance(context, majorID);
		String majorType			= (String)map.get(DomainObject.SELECT_TYPE);
        String objName				= (String)map.get(DomainObject.SELECT_NAME);
		String moveFilesToVersion	= "False";

		log("Major Type = " + majorType);
		log("majorID = " + majorID);
		log("Name = " + objName);

		//StringList versionOfToId	= (StringList)map.get(SELECT_TO_VERSION_OF_RELATIONSHIP_ID);
		StringList versionOfToId	= new StringList();
		
		BusinessObjectList minorObjList = mxUtil.getMinorObjects(context, new BusinessObject(majorID));
		if(minorObjList != null)
		{			
			BusinessObjectItr minorObjItr = new BusinessObjectItr(minorObjList);

			while(minorObjItr.next())
			{
				BusinessObject minorObject = minorObjItr.obj();
				versionOfToId.addElement(minorObject.getObjectId());
			}
		}
		log("Minors = " + versionOfToId);
		StringList isFinalizedAttr	= (StringList)map.get(SELECT_TO_VERSION_OF_RELATIONSHIP_ATTR_IS_FINALIZED);
		log("isFinalized = " + isFinalizedAttr);
		String isFinalizedId		= (String)map.get(SELECT_TO_FINALIZED_RELATIONSHIP_ID);

		StringList versionFiles		= (StringList)map.get(SELECT_TO_VERSION_OF_RELATIONSHIP_FILES);
		StringList versionFormats	= (StringList)map.get(SELECT_TO_VERSION_OF_RELATIONSHIP_FORMATS);
		String releventFileName		= "";

		int numVersions				= versionOfToId.size();
		DomainObject activeMinor	= null;
		DomainObject latestMinor	= null;

		log("Number of Versions = " + numVersions);
		if(numVersions > 0)
		{
			if(typeFormatMapping == null)
				populateTypeFormatMapping(context);

			Vector releventFormats	= (Vector)typeFormatMapping.get(majorType);
			log("releventFormats:" + releventFormats);
			log("versionFormats:" + versionFormats);
			log("versionFiles:" + versionFiles);
			if(releventFormats != null)
			{
				for(int i=0; i<versionFormats.size(); i++)
				{
					String versionFormat = (String)versionFormats.elementAt(i);
					if(isRelevantFormat(releventFormats, versionFormat))
					{
						releventFileName = (String)versionFiles.elementAt(i);
					}
				}
			}
			
			latestMinor	= DomainObject.newInstance(context, (String)versionOfToId.elementAt(numVersions - 1));
			log("Latest Minor:" + (String)versionOfToId.elementAt(numVersions - 1));
			BusinessObject majorBusObj = new BusinessObject(majorID);
			BusinessObject minorBusObj = new BusinessObject(latestMinor.getId());

			if(releventFileName.equals(""))
			{
				log("Could not find any files in the minor objects. Using the major object file name for setting Title attribute");
				//String findFileCmd		= "print bus " + majorID + " select format[].file.name dump |";
				String findFileCmd		= "print bus $1 select format[].file.name dump |";
				String majorFileName    = MqlUtil.mqlCommand(context, findFileCmd, majorID);

				if(majorFileName.length() == 0)
					log("No files found on major object. Failed to set title attribute on minors");
				else if(majorFileName.indexOf("|") > 0)
					log("Multiple files found on major object. Failed to set title attribute on minors");
				else
					releventFileName = majorFileName;
			}

			if(isFinalizedId == null && !isFinalizedAttr.contains("TRUE"))
			{
				boolean status = mxUtil.moveFilesFcsSupported(context,minorBusObj, majorBusObj);
				if(status)
					log("File moved from object " + latestMinor.getId() + " to object " + majorID);
			}
		}

		HashMap attributes = new HashMap();
		attributes.put(ATTRIBUTE_MOVEFILESTOVERSION, moveFilesToVersion);
		majorObject.setAttributeValues(context, attributes);
		log("Major object's attributes are set");

		if(minorObjList != null)
		{
			
			BusinessObjectItr minorObjItr = new BusinessObjectItr(minorObjList);
			log("converting minors");

			Policy versionPolicy = new Policy(POLICY_VERSION);
			versionPolicy.open(context);

			String firstInSequence = "";
			String nextInSequence = "";

			if(versionPolicy.hasSequence())
				firstInSequence = versionPolicy.getFirstInSequence();
			else
				firstInSequence = "1";
			
			versionPolicy.close(context);
			
			nextInSequence	= firstInSequence;			
			String autoName = majorObject.getShortUniqueName("");
			log("autoName::" + autoName);
		
			log("firstInSequence::" + firstInSequence);

			while(minorObjItr.next())
			{				
				BusinessObject minorObject = minorObjItr.obj();				

				minorObject.change(context, majorType, autoName, nextInSequence, minorObject.getVault(), POLICY_VERSION);

				minorObject.open(context);
				
				AttributeType attTypeMoveFilesToVersion = new AttributeType(ATTRIBUTE_MOVEFILESTOVERSION);
				Attribute moveFilesToVersionAttr		= new Attribute(attTypeMoveFilesToVersion, "False");

				AttributeType attTypeTitle	= new AttributeType(ATTRIBUTE_TITLE);
				Attribute titleAttr			= new Attribute(attTypeTitle, releventFileName);

				AttributeList attributeList = new AttributeList();
				attributeList.addElement(moveFilesToVersionAttr);
				attributeList.addElement(titleAttr);

				minorObject.setAttributeValues(context, attributeList);

				nextInSequence = minorObject.getNextSequence(context);
				log("nextInSequence::" + nextInSequence);

				minorObject.close(context);
			}
		}

		log("Attributes set for all Minor objects");
	}

	private boolean isRelevantFormat(Vector releventFormats, String versionFormat)
	{
		boolean isRelevant = false;

		if(!versionFormat.equalsIgnoreCase("Image"))
		{
			if(releventFormats != null)
			{
				for(int i=0; i<releventFormats.size(); i++)
				{
					String releventFormat = (String)releventFormats.elementAt(i);
					if(releventFormat.trim().equals(versionFormat))
						isRelevant = true;
				}
			}
		}

		return isRelevant;
	}

	private void migrateMajorWithoutMinor(Context context, Map map) throws Exception
	{
		String majorID				= (String)map.get(DomainObject.SELECT_ID);
		DomainObject majorObject	= DomainObject.newInstance(context, majorID);
		String majorType			= (String)map.get(DomainObject.SELECT_TYPE);
        String objName				= (String)map.get(DomainObject.SELECT_NAME);
		String moveFilesToVersion	= "False";

		log("Major Type = " + majorType);
		log("majorID = " + majorID);
		log("Name = " + objName);
		
		BusinessObject majorBusObj		= new BusinessObject(majorID);

		BusinessObject dummyMinorObj	= mxUtil.getActiveMinor(context,majorBusObj);

		String autoName = majorObject.getShortUniqueName("");

		Policy versionPolicy = new Policy(POLICY_VERSION);
		versionPolicy.open(context);

		String firstInSequence = "";

		if(versionPolicy.hasSequence())
			firstInSequence = versionPolicy.getFirstInSequence();
		else
			firstInSequence = "1";
		
		versionPolicy.close(context);

		dummyMinorObj.change(context, dummyMinorObj.getTypeName(), autoName, firstInSequence, dummyMinorObj.getVault(), POLICY_VERSION);
	}

	private String getFilesOfReleventFormat(Context context, Map map) throws Exception
	{
		String type = (String)map.get(DomainObject.SELECT_TYPE);
		return getFilesOfReleventFormat(context, map, type);
	}

	private String getFilesOfReleventFormat(Context context, Map map, String type) throws Exception
	{
		String releventFiles = null;

		if(typeFormatMapping == null)
			populateTypeFormatMapping(context);

		if(typeFormatMapping != null)
		{
			Vector releventFormats	= (Vector)typeFormatMapping.get(type);
			StringList objFormats	= (StringList)map.get(DomainObject.SELECT_FILE_FORMAT);
			StringList objFiles		= (StringList)map.get(DomainObject.SELECT_FILE_NAME);
			if(releventFormats != null && objFormats != null)
			{
				for(int i=0; i<objFormats.size(); i++)
				{
					if(releventFormats.contains(objFormats.elementAt(i)))
					{						
						String fileName = (String) objFiles.elementAt(i);
						if(releventFiles == null)
							releventFiles = fileName;
						else
							releventFiles += ";" + fileName;

					}
				}
			}
		}

		return releventFiles;
	}

	private void populateTypeFormatMapping(Context context) throws Exception
	{
		try
		{
			log("##Populating Type Format Mapping");

			typeFormatMapping = new HashMap();
			
			//Change SJ7 10-Oct-11
			//String command = "temp query bus '" + TYPE_MCAD_GLOBAL_CONFIG + "' * * select id dump |";
			String command = "temp query bus $1 * * select id dump |";
			String gcoList = MqlUtil.mqlCommand(context, command, TYPE_MCAD_GLOBAL_CONFIG);
			StringTokenizer gcoTokenizer = new StringTokenizer(gcoList, "\n");
			while(gcoTokenizer.hasMoreElements())
			{
				String gcoDetails		= (String)gcoTokenizer.nextElement();
				String gcoId			= gcoDetails.substring(gcoDetails.lastIndexOf("|")+1);
				BusinessObject gcoBus	= new BusinessObject(gcoId);

				gcoBus.open(context);
				String gcoMapping		= gcoBus.getAttributeValues(context, ATTR_MCAD_TYPE_FORMAT_MAPPING).getValue();
				gcoBus.close(context);

				StringTokenizer mappingTokenizer = new StringTokenizer(gcoMapping, "\n");
				while(mappingTokenizer.hasMoreElements())
				{
					String mappingDetails = (String)mappingTokenizer.nextElement();
					int barIndex	= mappingDetails.indexOf("|");
					int commaIndex	= mappingDetails.indexOf(",");
					if(barIndex > 0 && commaIndex > barIndex)
					{
						String type		= mappingDetails.substring(barIndex+1, commaIndex);
						String format	= mappingDetails.substring(commaIndex+1);

						if(format.equalsIgnoreCase("Image"))
							continue;

						Vector formatList = (Vector)typeFormatMapping.get(type);
						if(formatList == null)
							formatList = new Vector();
						formatList.addElement(format);
						typeFormatMapping.put(type, formatList);
					}
				}
			}
		}
		catch(Exception e)
		{
		}
	}

	private void startIEFLog()
	{
		try
		{
			if(isLogEnabled)
			{
				iefLog	= new FileWriter(documentDirectory + "MSOIMigration.log", true);
			}
		}
		catch(Exception e)
		{
			isLogEnabled = false;
			System.out.println("ERROR: Can not create log file. " + e.getMessage());
		}
	}

	private void endIEFLog()
	{
		try
		{
			if(isLogEnabled)
			{
				iefLog.write("\n\n");
				iefLog.flush();
				iefLog.close();
			}
		}
		catch(Exception e)
		{
		}
	}

	private void log(String message)
	{
		try
		{
			if(isLogEnabled)
			{
				iefLog.write(message + "\n");
			}
		}
		catch(Exception e)
		{
		}
	}

	private void writeToConverted(String id)
	{
		writeIdToFile("MSOIConvertedObjectIds.log", id);
	}

	private void writeToUnconverted(String id)
	{
		writeIdToFile("MSOIUnConvertedObjectIds.log", id);
	}

	private void writeToSkipped(String id)
	{
		writeIdToFile("MSOISkippedMinorObjectIds.log", id);
	}

	private void writeIdToFile(String fileName, String id)
	{
		try
		{
			if(isLogEnabled)
			{
				FileWriter iefIdLog	= new FileWriter(documentDirectory + fileName, true);
				iefIdLog.write(id + "\n");
				iefIdLog.flush();
				iefIdLog.close();
			}
		}
		catch(Exception e)
		{
			System.out.println("ERROR: Failed to write " + id + " to log file " + fileName + ". " + e.getMessage());
		}
	}    
}
