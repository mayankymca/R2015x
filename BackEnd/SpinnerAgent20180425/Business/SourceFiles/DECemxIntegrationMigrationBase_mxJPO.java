/*
**  emxIntegrationMigrationBase
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
*/

  import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessType;
import matrix.db.Context;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

  public class DECemxIntegrationMigrationBase_mxJPO
  {

	  private String TYPE_MCAD_GLOBAL_CONFIG		= "";
	  private String TYPE_MCAD_MODEL				= "";
	  private String TYPE_ECAD_MODEL				= "";
	  private String TYPE_MCAD_DRAWING				= "";
	  private String TYPE_MCAD_VERSIONED_DRAWING	= "";
	  private String TYPE_COMPOUND_DOCUMENT			= "";
	  private String ATTR_MCAD_TYPE_FORMAT_MAPPING	= "";
	  private String ATTR_SOURCE					= "";
	  private String ATTR_ISFINALIZED				= "";
	  private String REL_VERSIONOF					= "";
	  private String REL_FINALIZED					= "";
	  private String POLICY_CAD_MODEL_MINOR			= "";
	  private String POLICY_DOCUMENT_MINOR			= "";
      private String ATTRIBUTE_ISVERSIONOBJECT      = "";
      private String ATTRIBUTE_TITLE				= "";
      private String ATTRIBUTE_MOVEFILESTOVERSION   = "";
      private String ATTRIBUTE_ORIGINATOR			= "";
      private String ATTRIBUTE_CAD_TYPE				= "";
      
	  public static final String IEF_NONE					= "NONE";
	  public static final String IEF_MAJOR_NORMAL			= "MAJOR";
	  public static final String IEF_MINOR_NORMAL			= "MINOR";
	  public static final String IEF_MAJOR_WITHOUT_MINOR	= "FINALIZED_MAJOR";
	  
	  private String SELECT_TO_VERSION_OF_RELATIONSHIP_ID		= "";
	  private String SELECT_FROM_VERSION_OF_RELATIONSHIP_ID		= "";
	  private String SELECT_ATTR_SOURCE							= "";
	  private String SELECT_ATTR_CAD_TYPE						= "";
	  private String SELECT_MINOR_ATTR_CAD_TYPE					= "";
	  private String SELECT_TO_VERSION_OF_RELATIONSHIP_ATTR_IS_FINALIZED	= "";
	  private String SELECT_TO_FINALIZED_RELATIONSHIP_ID		= "";
	  private String SELECT_TO_VERSION_OF_RELATIONSHIP_FILES	= "";
	  private String SELECT_TO_VERSION_OF_RELATIONSHIP_FORMATS	= "";

	  private static boolean isLogEnabled = false;
	  private static final String	documentDirectory	= "D:/Temp/Migration/";

	  private MCADMxUtil	mxUtil				= null;
	  private FileWriter	iefLog				= null;
	  
	  private static HashMap typeFormatMapping		= null;
	  private static Vector typeListIEFTypes		= new Vector();
	  private static Vector typeListNonIEFTypes		= new Vector();
	  private static Vector typeListOfficeTypes		= new Vector();
    /**
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public DECemxIntegrationMigrationBase_mxJPO (Context context, String[] args)
        throws Exception
    {
		mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle("en-US"), new IEFGlobalCache());
		
		  TYPE_MCAD_GLOBAL_CONFIG		= PropertyUtil.getSchemaProperty(context,"type_MCADInteg-GlobalConfig");
		  TYPE_MCAD_MODEL				= PropertyUtil.getSchemaProperty(context,"type_MCADModel");
		  TYPE_ECAD_MODEL				= PropertyUtil.getSchemaProperty(context,"type_ECADModel");
		  TYPE_MCAD_DRAWING				= PropertyUtil.getSchemaProperty(context,"type_MCADDrawing");
		  TYPE_MCAD_VERSIONED_DRAWING	= PropertyUtil.getSchemaProperty(context,"type_MCADVersionedDrawing");
		  TYPE_COMPOUND_DOCUMENT		= PropertyUtil.getSchemaProperty(context,"type_CompoundDocument");
		  ATTR_MCAD_TYPE_FORMAT_MAPPING	= PropertyUtil.getSchemaProperty(context,"attribute_MCADInteg-TypeFormatMapping");
		  ATTR_SOURCE					= PropertyUtil.getSchemaProperty(context,"attribute_Source");
		  ATTR_ISFINALIZED				= PropertyUtil.getSchemaProperty(context,"attribute_IsFinalized");
		  REL_VERSIONOF					= PropertyUtil.getSchemaProperty(context,"relationship_VersionOf");
		  REL_FINALIZED					= PropertyUtil.getSchemaProperty(context,"relationship_Finalized");
		  POLICY_CAD_MODEL_MINOR		= PropertyUtil.getSchemaProperty(context,"policy_CADModelMinorPolicy");
		  POLICY_DOCUMENT_MINOR			= PropertyUtil.getSchemaProperty(context,"policy_DocumentMinorPolicy");
	      ATTRIBUTE_ISVERSIONOBJECT     = PropertyUtil.getSchemaProperty(context,"attribute_IsVersionObject");
	      ATTRIBUTE_TITLE				= PropertyUtil.getSchemaProperty(context,"attribute_Title");
	      ATTRIBUTE_MOVEFILESTOVERSION  = PropertyUtil.getSchemaProperty(context,"attribute_MoveFilesToVersion");
	      ATTRIBUTE_ORIGINATOR			= PropertyUtil.getSchemaProperty(context,"attribute_Originator");
	      ATTRIBUTE_CAD_TYPE			= PropertyUtil.getSchemaProperty(context,"attribute_CADType");
		  
		  
		  SELECT_TO_VERSION_OF_RELATIONSHIP_ID		= "to[" + REL_VERSIONOF + "].from.id";
		  SELECT_FROM_VERSION_OF_RELATIONSHIP_ID	= "from[" + REL_VERSIONOF + "].to.id";
		  SELECT_ATTR_SOURCE						= "attribute[" + ATTR_SOURCE + "]";
		  SELECT_ATTR_CAD_TYPE						= "attribute[" + ATTRIBUTE_CAD_TYPE + "]";
		  SELECT_MINOR_ATTR_CAD_TYPE				= "to[" + REL_VERSIONOF + "].from.attribute[" + ATTRIBUTE_CAD_TYPE + "]";
		  SELECT_TO_VERSION_OF_RELATIONSHIP_ATTR_IS_FINALIZED	= "to[" + REL_VERSIONOF + "].attribute[" + ATTR_ISFINALIZED + "]";
		  SELECT_TO_FINALIZED_RELATIONSHIP_ID		= "to[" + REL_FINALIZED + "].from.id";
		  SELECT_TO_VERSION_OF_RELATIONSHIP_FILES	= "to[" + REL_VERSIONOF + "].from.format.file.name";
		  SELECT_TO_VERSION_OF_RELATIONSHIP_FORMATS	= "to[" + REL_VERSIONOF + "].from.format.file.format";
	}

    /**
     * This method is executed if a specific method is not specified.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @returns nothing
     * @throws Exception if the operation fails
     * @since VCP 10.5.0.0
     */
    public int mxMain(Context context, String[] args)
        throws Exception
    {
        if (true)
        {
            //throw new Exception("must specify method on emxCommonFile invocation");
        }
        return 0;
    }

    public StringList getIEFSelectables(Context context) throws Exception
    {
		StringList iefObjectSelects = new StringList();

		DomainObject.MULTI_VALUE_LIST.add(SELECT_TO_VERSION_OF_RELATIONSHIP_ID);
		DomainObject.MULTI_VALUE_LIST.add(SELECT_TO_VERSION_OF_RELATIONSHIP_ATTR_IS_FINALIZED);
		DomainObject.MULTI_VALUE_LIST.add(SELECT_TO_VERSION_OF_RELATIONSHIP_FILES);
		DomainObject.MULTI_VALUE_LIST.add(SELECT_TO_VERSION_OF_RELATIONSHIP_FORMATS);
		DomainObject.MULTI_VALUE_LIST.add(SELECT_MINOR_ATTR_CAD_TYPE);

		iefObjectSelects.add(SELECT_ATTR_SOURCE);
		iefObjectSelects.add(SELECT_ATTR_CAD_TYPE);
		iefObjectSelects.add(SELECT_MINOR_ATTR_CAD_TYPE);
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
				createDummyMinor(context, map);
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
			if(parentType.equals(TYPE_MCAD_MODEL) 
				|| parentType.equals(TYPE_ECAD_MODEL) 
				|| parentType.equals(TYPE_MCAD_DRAWING) 
				|| parentType.equals(TYPE_MCAD_VERSIONED_DRAWING)
				|| parentType.equals(TYPE_COMPOUND_DOCUMENT) )
			{
				typeListIEFTypes.addElement(type);
				if(parentType.equals(TYPE_COMPOUND_DOCUMENT))
					typeListOfficeTypes.addElement(type);
				return true;
			}

			BusinessType busType = new BusinessType(parentType, context.getVault());
			parentType = busType.getParent(context);
		}

		typeListNonIEFTypes.addElement(type);
		return false;
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
					{						String fileName = (String) objFiles.elementAt(i);
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
			String gcoList = MqlUtil.mqlCommand(context, "temp query bus $1 $2 $3 select $4 dump $5", TYPE_MCAD_GLOBAL_CONFIG,"*","*","id","|");
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

	private void migrateMajor(Context context, Map map) throws Exception
	{
		String majorID				= (String)map.get(DomainObject.SELECT_ID);
		DomainObject majorObject	= DomainObject.newInstance(context, majorID);
		String majorType			= (String)map.get(DomainObject.SELECT_TYPE);
        String objName				= (String)map.get(DomainObject.SELECT_NAME);
		String moveFilesToVersion	= "True";

		log("Major Type = " + majorType);
		log("Name = " + objName);

		StringList versionOfToId	= (StringList)map.get(SELECT_TO_VERSION_OF_RELATIONSHIP_ID);
		StringList isFinalizedAttr	= (StringList)map.get(SELECT_TO_VERSION_OF_RELATIONSHIP_ATTR_IS_FINALIZED);
		String isFinalizedId		= (String)map.get(SELECT_TO_FINALIZED_RELATIONSHIP_ID);

		StringList versionFiles		= (StringList)map.get(SELECT_TO_VERSION_OF_RELATIONSHIP_FILES);
		StringList versionFormats	= (StringList)map.get(SELECT_TO_VERSION_OF_RELATIONSHIP_FORMATS);
		String releventFileName		= "";
		String majorTitle			= objName;

		int numVersions				= versionOfToId.size();
		DomainObject activeMinor	= null;
		DomainObject latestMinor	= null;

		log("Number of Versions = " + numVersions);
		if(numVersions > 0)
		{
			log("Version Formats = " + versionFormats);
			log("Version Files = " + versionFiles);

			if(typeFormatMapping == null)
				populateTypeFormatMapping(context);
			Vector releventFormats	= (Vector)typeFormatMapping.get(majorType);
			if(releventFormats != null)
			{
				for(int i=0; i<releventFormats.size(); i++)
				{
					int formatIndex		 = versionFormats.indexOf(releventFormats.elementAt(i));
					if(formatIndex > -1)
					{
						releventFileName = (String)versionFiles.elementAt(i);
						break;
					}
				}
			}
			log("Relevent Formats: " + releventFormats);

			latestMinor	= DomainObject.newInstance(context, (String)versionOfToId.elementAt(numVersions - 1));
			if(numVersions > 1)
			{
				if(isFinalizedId != null)
				{
					activeMinor			= DomainObject.newInstance(context, isFinalizedId);
					majorTitle			= releventFileName;
					moveFilesToVersion	= "False";
				}
				else if(isFinalizedAttr.contains("TRUE"))
				{
					activeMinor			= DomainObject.newInstance(context, (String)versionOfToId.elementAt(isFinalizedAttr.indexOf("TRUE")));
					majorTitle			= releventFileName;
					moveFilesToVersion	= "False";
				}
				else
				{
					activeMinor = latestMinor;
				}
			}
			else
			{
				if(isFinalizedId != null || isFinalizedAttr.contains("TRUE"))
					moveFilesToVersion	= "False";

				activeMinor = latestMinor;
			}


			log("Found Active and Latest minors");
			DomainRelationship.connect(context, majorObject, MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion"), activeMinor);
			DomainRelationship.connect(context, majorObject, MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion"), latestMinor);
			log("Connected Active and Latest minors");
		}

		String cadType = (String)((Vector)map.get(SELECT_MINOR_ATTR_CAD_TYPE)).elementAt(0);

		HashMap attributes = new HashMap();
		attributes.put(ATTRIBUTE_TITLE, majorTitle);
		attributes.put(ATTRIBUTE_MOVEFILESTOVERSION, moveFilesToVersion);
		attributes.put(ATTRIBUTE_CAD_TYPE, cadType);
		majorObject.setAttributeValues(context, attributes);
		log("Major object's attributes are set");

		for(int i=0; i<versionOfToId.size(); i++)
		{
			String minorId				= (String)versionOfToId.elementAt(i);
			DomainObject minorObject	= DomainObject.newInstance(context, minorId);

			attributes = new HashMap();
			attributes.put(ATTRIBUTE_TITLE, releventFileName);
			attributes.put(ATTRIBUTE_ISVERSIONOBJECT, "True");
			minorObject.setAttributeValues(context, attributes);
		}
		log("Attributes set for all Minor objects");
	}

	private void createDummyMinor(Context context, Map map) throws Exception
	{
        String majorID		= (String)map.get(DomainObject.SELECT_ID);
        String majorType    = (String)map.get(DomainObject.SELECT_TYPE);
        String objName      = (String)map.get(DomainObject.SELECT_NAME);
        String majorRev     = (String)map.get(DomainObject.SELECT_REVISION);
        String majorVault   = (String)map.get(DomainObject.SELECT_VAULT);
        String majorOwner   = (String)map.get(DomainObject.SELECT_OWNER);
        String source		= (String)map.get(SELECT_ATTR_SOURCE);
		String cadType		= (String)map.get(SELECT_ATTR_CAD_TYPE);

		String minorType	= mxUtil.getCorrespondingType(context, majorType);
		String minorRev		= majorRev + ".0";
		String fileName		= getFilesOfReleventFormat(context, map);

		String policy		= POLICY_CAD_MODEL_MINOR;
		if(typeListOfficeTypes.contains(minorType))
			policy			= POLICY_DOCUMENT_MINOR;

		log("Major Type = " + majorType);
		log("Name = " + objName);
		log("Minor Type = " + minorType);
		log("File Name = " + fileName);
		log("Minor Policy = " + fileName);

		DomainObject majorObject	= DomainObject.newInstance(context, majorID);
		DomainObject dummyMinor		= DomainObject.newInstance(context, minorType);
        dummyMinor.createObject(context, minorType, objName, minorRev, policy, majorVault);

		HashMap attributes = new HashMap();
		attributes.put(ATTRIBUTE_TITLE, fileName);
		attributes.put(ATTRIBUTE_ISVERSIONOBJECT, "True");
		attributes.put(ATTR_SOURCE, source);
		attributes.put(ATTRIBUTE_ORIGINATOR, majorOwner);
		attributes.put(ATTRIBUTE_CAD_TYPE, cadType);
		dummyMinor.setAttributeValues(context, attributes);
		log("Attributes set for Minor object");

		HashMap majorAttributes = new HashMap();
		majorAttributes.put(ATTRIBUTE_TITLE, fileName);
		majorAttributes.put(ATTRIBUTE_MOVEFILESTOVERSION, "False");
		majorObject.setAttributeValues(context, majorAttributes);
		log("Attributes set for Major object");

		StringList objectSelects = new StringList(3);
		objectSelects.add(DomainObject.SELECT_ID);
		Map idMap		= dummyMinor.getInfo(context, objectSelects);
		String minorID	= (String)idMap.get(DomainObject.SELECT_ID);

		MqlUtil.mqlCommand(context, "modify bus $1 owner $2",minorID,majorOwner);
		log("Owner set for Minor object");

		DomainRelationship.connect(context, majorObject, MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion"), dummyMinor);
		DomainRelationship.connect(context, majorObject, MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion"), dummyMinor);
		log("Connected Active and Latest minors");
	}

	private void startIEFLog()
	{
		try
		{
			if(isLogEnabled)
			{
				iefLog	= new FileWriter(documentDirectory + "iefMigration.log", true);
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
		writeIdToFile("iefConvertedObjectIds.log", id);
	}

	private void writeToUnconverted(String id)
	{
		writeIdToFile("iefUnConvertedObjectIds.log", id);
	}

	private void writeToSkipped(String id)
	{
		writeIdToFile("iefSkippedObjectIds.log", id);
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

