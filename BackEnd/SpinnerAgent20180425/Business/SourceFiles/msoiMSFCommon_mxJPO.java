/*   msoiMSFCommon
 **
 **   Copyright (c) 2003-2015 Dassault Systemes.
 **   All Rights Reserved.
 **   This program contains proprietary and trade secret information of MatrixOne,
 **   Inc.  Copyright notice is precautionary only
 **   and does not evidence any actual or intended publication of such program
 **
 **   This JPO contains the implementation of emxCommonPart
 **
 **  @quickReview 13:05:08 SJ7 Changes for Autoclau HL R2014x - Merging code changes from RP3
 **  @quickReview 13:06:14 SJ7 Changes for Adding a function to show the Version number in search results.
 **  @quickReview 13:09:06 NS7 IR-248641V6R2014x MS Office Connector Explorer: "Preview" brings up 3D Via Viewer for all files
 **  @quickReview 13:10:22 RZW IR-262298V6R2014x Hostname should just show the host name for a particular file
 **  @quickReview 15:11:26 RZW Active Version access passed in file list
 **  @quickReview 17:02:27 RZW Created MSFSearchTable
 **  @quickReview 17:04:28 ODW TSK3438445: Not able to launch MS project from with in Compass
 */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.jdom.Document;
import com.matrixone.jdom.Element;
import com.matrixone.jdom.output.XMLOutputter;

/**
 * The <code>msoiMSFCommon</code> class
 *
 */
public class msoiMSFCommon_mxJPO extends emxDomainObject_mxJPO
{
	private String currentLanguage = null;

	public msoiMSFCommon_mxJPO (Context context, String[] args) throws Exception
	{
		super(context, args);
	}
	
	public void AddMSFDocumentInterface(Context context, String[] args) throws Exception
	{
		try
		{
			ContextUtil.pushContext(context);
			MqlUtil.mqlCommand(context, "modify bus $1 add interface $2", args[0], "MSFDocumentInterface");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			System.out.println("exception" + ex.getMessage());
		}
		finally
		{
			ContextUtil.popContext(context);
		}
	}
	
	private Map getMasterObjectMap(Context context, DomainObject masterObject) throws Exception
	{
		//Added to make a single database call to
		StringList masterObjectSelectList = new StringList(9);
		masterObjectSelectList.add(CommonDocument.SELECT_ID);
		masterObjectSelectList.add(CommonDocument.SELECT_TYPE);
		masterObjectSelectList.add(CommonDocument.SELECT_NAME);
		masterObjectSelectList.add(CommonDocument.SELECT_REVISION);
		masterObjectSelectList.add(CommonDocument.SELECT_FILE_NAME);
		masterObjectSelectList.add(CommonDocument.SELECT_FILE_FORMAT);
		masterObjectSelectList.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
		masterObjectSelectList.add(CommonDocument.SELECT_IS_VERSION_OBJECT);
		masterObjectSelectList.add(CommonDocument.SELECT_TITLE);

		// get the Master Object data
		return masterObject.getInfo(context,masterObjectSelectList);
	}
	
	private MapList getVersionObjectList(Context context, DomainObject masterObject) throws Exception
	{
		// Version Object seletcs
		StringList versionSelectList = new StringList(13);
		versionSelectList.add(CommonDocument.SELECT_ID);
		versionSelectList.add(CommonDocument.SELECT_REVISION);
		versionSelectList.add(CommonDocument.SELECT_DESCRIPTION);
		versionSelectList.add(CommonDocument.SELECT_LOCKED);
		versionSelectList.add(CommonDocument.SELECT_LOCKER);
		versionSelectList.add(CommonDocument.SELECT_TITLE);
		versionSelectList.add(CommonDocument.SELECT_FILE_NAME);
		versionSelectList.add(CommonDocument.SELECT_FILE_FORMAT);
		versionSelectList.add(CommonDocument.SELECT_OWNER);
		versionSelectList.add(DomainConstants.SELECT_ORIGINATED);
		versionSelectList.add(DomainConstants.SELECT_TYPE);
		versionSelectList.add(CommonDocument.SELECT_IS_VERSION_OBJECT);
		versionSelectList.add("current.access");

		// get the file (Version Object) data
		return masterObject.getRelatedObjects(context,
				CommonDocument.RELATIONSHIP_ACTIVE_VERSION,
				CommonDocument.TYPE_DOCUMENTS,
				versionSelectList,
				null,
				false,
				true,
				(short)1,
				null,
				null,
				null,
				null,
				null);
	}

	public MapList getFiles(Context context, String[] args) throws Exception
	{
		MapList fileMapList = new MapList();
		String fileFormat = "";

		String  masterObjectId     = args[0];
		DomainObject masterObject  = DomainObject.newInstance(context, masterObjectId);
		Map masterObjectMap = getMasterObjectMap(context, masterObject);
		
		//Check if the object is a major or a minor object
		String isVersionObjectString    = (String) masterObjectMap.get(CommonDocument.SELECT_IS_VERSION_OBJECT);
		boolean isVersionObject = "true".equalsIgnoreCase(isVersionObjectString);

		// get all the files in the Master Object
		StringList fileList = (StringList) masterObjectMap.get(CommonDocument.SELECT_FILE_NAME);
		StringList fileFormatList = (StringList) masterObjectMap.get(CommonDocument.SELECT_FILE_FORMAT);

		// get the Master Object meta data
		String masterId    = (String) masterObjectMap.get(CommonDocument.SELECT_ID);
		boolean moveFilesToVersion = (Boolean.valueOf((String) masterObjectMap.get(CommonDocument.SELECT_MOVE_FILES_TO_VERSION))).booleanValue();

		if (isVersionObject)	//Only in case of launch from compass this is possible
		{
			String versionFileName = (String)masterObjectMap.get(CommonDocument.SELECT_TITLE);
			String versionFileRevision = (String)masterObjectMap.get(CommonDocument.SELECT_REVISION);
			int index = fileList.indexOf(versionFileName);

			// get the File Format
			if (index != -1 && fileFormatList != null && fileFormatList.size() >= index )
			{
				fileFormat = (String)fileFormatList.get(index);
			}
			
			masterObjectMap.put(CommonDocument.SELECT_FILE_FORMAT, fileFormat);
			masterObjectMap.put(CommonDocument.SELECT_FILE_NAME, versionFileName);
			masterObjectMap.put(CommonDocument.SELECT_REVISION, versionFileRevision);
			masterObjectMap.put(CommonDocument.SELECT_LOCKER, "");
			masterObjectMap.put("_objectAccessActiveMinor", "");
			fileMapList.add(masterObjectMap);
		}
		else
		{
			MapList versionList = getVersionObjectList(context, masterObject);

			// loop thru each file to build MapList, each Map corresponds to one file
			Iterator versionItr  = versionList.iterator();
			while(versionItr.hasNext())
			{
				Map fileVersionMap     = (Map)versionItr.next();
				String versionFileName = (String)fileVersionMap.get(CommonDocument.SELECT_TITLE);
				String locker = (String)fileVersionMap.get(CommonDocument.SELECT_LOCKER);
				String versionFileRevision = (String)fileVersionMap.get(CommonDocument.SELECT_REVISION);
				String activeVersionAccess = (String)fileVersionMap.get("current.access");
				fileFormat = CommonDocument.FORMAT_GENERIC;
				if( moveFilesToVersion )
				{
					try
					{
						String versionFiles = (String)fileVersionMap.get(CommonDocument.SELECT_FILE_NAME);
						fileFormat = (String) fileVersionMap.get(CommonDocument.SELECT_FILE_FORMAT);
					} catch (ClassCastException cex) {
						StringList versionFilesList = (StringList)fileVersionMap.get(CommonDocument.SELECT_FILE_NAME);
						StringList versionFileFormat = (StringList)fileVersionMap.get(CommonDocument.SELECT_FILE_FORMAT);

						// get the file corresponding to this Version by filtering the above fileList
						int index = versionFilesList.indexOf(versionFileName);

						// get the File Format
						if (index != -1 && versionFileFormat != null && versionFileFormat.size() >= index )
						{
							fileFormat = (String)versionFileFormat.get(index);
						}
					}
				} else {
					// get the file corresponding to this Version by filtering the above fileList
					int index = fileList.indexOf(versionFileName);

					// get the File Format
					if (index != -1 && fileFormatList != null && fileFormatList.size() >= index )
					{
						fileFormat = (String)fileFormatList.get(index);
					}
				}
				fileVersionMap.put(CommonDocument.SELECT_FILE_FORMAT, fileFormat);
				fileVersionMap.put(CommonDocument.SELECT_FILE_NAME, versionFileName);
				fileVersionMap.put(CommonDocument.SELECT_REVISION, versionFileRevision);
				fileVersionMap.put(CommonDocument.SELECT_LOCKER, locker);
				fileVersionMap.put("_objectAccessActiveMinor", activeVersionAccess);
				fileMapList.add(fileVersionMap);
			}
		}
		return fileMapList;
	}

	public StringList GetFilesListXML(Context context, String[] args) throws MatrixException
	{ 
		StringList stringList = new StringList();        

		try 
		{
			String hostName = "";
			String fileName = "";
			String format = "";
			Map objectMap = null;
			MapList fileMapList = new MapList();        
			HashMap programmap  = (HashMap)JPO.unpackArgs(args);
			MapList objectList = (MapList)programmap.get("objectList");
			String[] methodargs = {""};
			for ( int i = 0; i < objectList.size(); i++ )
			{
				String xmlOutput = "<FileList>";
				String objId =null;
				try
				{
					objId = (String)((Hashtable)objectList.get(i)).get("id");
				}
				catch(ClassCastException ex)
				{
					objId = (String)((HashMap)objectList.get(i)).get("id");
				}

				if(objId.endsWith("\n"))
				{
					objId = objId.substring(0,objId.length()-1);
				}

				String[] myArgs = {objId};
				try
				{
					fileMapList = getFiles(context, myArgs);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					continue;
				}

				for ( int j = 0; j < fileMapList.size(); j++ )
				{
					Map fileVersionMap = (Map)fileMapList.get(j);

					format = "" + fileVersionMap.get(CommonDocument.SELECT_FILE_FORMAT);
					xmlOutput = xmlOutput + "<MSFFileInfo Format=" + "\"" + format + "\"";

					fileName = "" + fileVersionMap.get(CommonDocument.SELECT_FILE_NAME);
					xmlOutput = xmlOutput + " FileName=" + "\"" + fileName + "\"";

					xmlOutput = xmlOutput + " Version=" + "\"" + fileVersionMap.get(CommonDocument.SELECT_REVISION) + "\"";

					String mqlHostNameArg = "format[" + format + "].file[" + fileName + "].store.host";
					hostName = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", objId, mqlHostNameArg);
					xmlOutput = xmlOutput + " HostName=" + "\"" + hostName + "\"";

					xmlOutput = xmlOutput + " _objectAccessActiveMinor=" + "\"" + fileVersionMap.get("_objectAccessActiveMinor") + "\"";

					xmlOutput = xmlOutput + " LockedBy=" + "\"" + fileVersionMap.get(CommonDocument.SELECT_LOCKER) + "\"" + "/>";
				}

				xmlOutput = xmlOutput + "</FileList>";
				stringList.add(xmlOutput);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		return stringList;
	}

	//18-Jun-2013 : Function added to show versions in Search and navigation.
	public Vector getVersions(Context context, String[] args) throws Exception
	{
		Vector vecVersions = new Vector();
		try
		{
			boolean bActivateDSFA= FrameworkUtil.isSuiteRegistered(context,"ActivateDSFA",false,null,null);
			StringList selects = new StringList(5);
			selects.add(CommonDocument.SELECT_TYPE);
			selects.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION);
			if (bActivateDSFA )
			{
				selects.add(CommonDocument.SELECT_VCFILE_EXISTS);
				selects.add(CommonDocument.SELECT_VCFOLDER_EXISTS);
				selects.add(CommonDocument.SELECT_VCMODULE_EXISTS);
			}

			MapList mapList = getObjectInfoMapList(context, args, selects);

			for (Iterator itrSelectables = mapList.iterator(); itrSelectables.hasNext();)
			{
				Map objectMap = (Map) itrSelectables.next();

				String objectType = (String)objectMap.get(CommonDocument.SELECT_TYPE);
				String parentType = CommonDocument.getParentType(context, objectType);
				String vcFileType = (String)objectMap.get(CommonDocument.SELECT_VCFILE_EXISTS);
				String vcFolderType = (String)objectMap.get(CommonDocument.SELECT_VCFOLDER_EXISTS);
				String vcModuleType = (String)objectMap.get(CommonDocument.SELECT_VCMODULE_EXISTS);

				boolean vcDocumentType = (vcFileType != null && vcFileType.equalsIgnoreCase("true")) ||
					(vcFolderType != null && vcFolderType.equalsIgnoreCase("true")) ||
					(vcModuleType != null && vcModuleType.equalsIgnoreCase("true"));
				
				if(!vcDocumentType && parentType.equals(CommonDocument.TYPE_DOCUMENTS))
				{
					StringList versions = (StringList)objectMap.get(CommonDocument.SELECT_ACTIVE_FILE_VERSION);

					if ( versions == null || versions.size() == 0)
					{
						vecVersions.add("");
					} 
					else if ( versions.size() == 1 ) 
					{
						vecVersions.add((String)versions.get(0));
					} 
					else 
					{
						vecVersions.add("--");
					}
				}
				else 
				{
					vecVersions.add("");
				}
			}
			return vecVersions;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw ex;
		}
	}

	//NS7+ IR-248641V6R2014x
	public String getPreviewTypes(Context context, String[] args) throws Exception
	{
		String PreviewTypes =  "";

		try
		{        
			HashMap argsMap = new HashMap(1);
			argsMap.put("language",  "en");

			PreviewTypes = (String)JPO.invoke(context, "jpo.plmprovider.MetaDataBase", null , "getConfiguredTypes", JPO.packArgs(argsMap), String.class); 
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw ex;
		}

		return PreviewTypes;
	}

	public Vector GetSymbolicName(Context context, String[] args) throws Exception
	{
		Vector vecSymbolicNames  = new Vector();
		try
		{
			StringList selects = new StringList(1);
			selects.add(DomainConstants.SELECT_TYPE);

			MapList mapList = getObjectInfoMapList(context, args, selects);

			for (Iterator itrSelectables = mapList.iterator(); itrSelectables.hasNext();)
			{
				Map objectMap = (Map) itrSelectables.next();
				String objType = (String)objectMap.get(DomainConstants.SELECT_TYPE);
				vecSymbolicNames.add(FrameworkUtil.getAliasForAdmin(context, "type", objType, true));
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw ex;
		}

		return vecSymbolicNames;
	}

	/**
	 * Where : In Search Object result details, to hide 'Attach to existing' and revise menu item for older 
	 *         revision documents
	 * How : Check the current object revision and its last revision. If its not the latest then remove 
	 *       revise, checkin
	 * @param context the eMatrix <code>Context</code> object
	 * @param args holds the following input arguments:
	 *        0 - String containing the "programMap"
	 *        programMap holds the following input arguments:
	 *          0 - String containing "objectList"
	 * @returns MapList
	 * @throws Exception if operation fails
	 * @since MSF V6R2016x
	 */
	public Vector getCurrentUserAccess(Context context, String[] args) throws Exception
	{
		Vector vecAccess = new Vector();
		try
		{
			StringList selects = new StringList(3);
			selects.add("current.access");
			selects.add("revision");
			selects.add("last.revision");

			MapList mapList = getObjectInfoMapList(context, args, selects);

			for (Iterator itrSelectables = mapList.iterator(); itrSelectables.hasNext();)
			{
				Map objectMap = (Map) itrSelectables.next();
				String sLastRev = (String) objectMap.get("last.revision");
				String sRev = (String) objectMap.get("revision");
				String sAccess = (String) objectMap.get("current.access");

				vecAccess.add(!sLastRev.equals(sRev) ? sAccess.replace("revise", "").replace("checkin", "") : sAccess);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw ex;
		}

		return vecAccess;
	}

	/**
	* Method to get the translated type values
	*
	**/
	public Vector GetType(Context context, String[] args) throws Exception
	{
		Vector vecType  = new Vector();
		try
		{
			StringList selects = new StringList(1);
			selects.add(DomainConstants.SELECT_TYPE);

			MapList mapList = getObjectInfoMapList(context, args, selects);

			for (Iterator itrSelectables = mapList.iterator(); itrSelectables.hasNext();)
			{
				Map objectMap = (Map) itrSelectables.next();
				String objType = (String)objectMap.get(DomainConstants.SELECT_TYPE);
				vecType.add(i18nNow.getTypeI18NString(objType, currentLanguage));
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw ex;
		}

		return vecType;
	}

	/**
	* Method to get the translated policy values
	*
	**/
	public Vector GetPolicy(Context context, String[] args) throws Exception
	{
		Vector vecPolicy  = new Vector();
		try
		{
			StringList selects = new StringList(1);
			selects.add(DomainConstants.SELECT_POLICY);

			MapList mapList = getObjectInfoMapList(context, args, selects);

			for (Iterator itrSelectables = mapList.iterator(); itrSelectables.hasNext();)
			{
				Map objectMap = (Map) itrSelectables.next();
				String objPolicy = (String)objectMap.get(DomainConstants.SELECT_POLICY);
				vecPolicy.add(i18nNow.getAdminI18NString("Policy", objPolicy, currentLanguage));
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw ex;
		}

		return vecPolicy;
	}

	/**
	* Method to get the translated state values
	*
	**/
	public Vector GetState(Context context, String[] args) throws Exception
	{
		Vector vecState  = new Vector();
		try
		{
			StringList selects = new StringList(2);
			selects.add(DomainConstants.SELECT_CURRENT);
			selects.add(DomainConstants.SELECT_POLICY);

			MapList mapList = getObjectInfoMapList(context, args, selects);

			for (Iterator itrSelectables = mapList.iterator(); itrSelectables.hasNext();)
			{
				Map objectMap = (Map) itrSelectables.next();
				String objState = (String)objectMap.get(DomainConstants.SELECT_CURRENT);
				String objPolicy = (String)objectMap.get(DomainConstants.SELECT_POLICY);
				vecState.add(i18nNow.getStateI18NString(objPolicy, objState, currentLanguage));
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw ex;
		}

		return vecState;
	}

	/**
	* Method to unpack JPO args and get the details of the objects based on a selects list
	*
	**/
	private MapList getObjectInfoMapList(Context context, String[] args, StringList selects) throws Exception
	{
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList objectList = (MapList) programMap.get("objectList");
		HashMap paramList = (HashMap) programMap.get("paramList");

		if(paramList.containsKey("languageStr"))
		{
			currentLanguage = paramList.get("languageStr").toString();
		}
		else
		{
			currentLanguage = (String)paramList.get("LocaleLanguage");
			if(currentLanguage == null || currentLanguage.equals(""))
			{
				Locale LocaleObj = (Locale)paramList.get("localeObj");
				if(null != LocaleObj)
				{
					currentLanguage = LocaleObj.toString();
				}
			}
		}

		String[] oidsArray = new String[objectList.size()];
		int iItr = 0;
		for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) 
		{
			Map mapObjectInfo = (Map) itrObjects.next();
			oidsArray[iItr++] = (String) mapObjectInfo.get(DomainConstants.SELECT_ID);
		}

		return DomainObject.getInfo(context, oidsArray, selects);
	}

	public String GetDetailsForLaunchApplication(Context context, String[] args) throws Exception
	{
		Map outputMap = new HashMap();

		try
		{
			StringList selects = new StringList();

			selects.add(SELECT_TYPE);
			selects.add(SELECT_NAME);
			selects.add(SELECT_REVISION);
			selects.add("current.access");
			selects.add(CommonDocument.SELECT_FILE_NAME);
			selects.add(CommonDocument.SELECT_IS_KIND_OF_DOCUMENTS);
			selects.add(CommonDocument.SELECT_IS_VERSION_OBJECT);
			selects.add(CommonDocument.SELECT_TITLE);

			DomainObject domObject = DomainObject.newInstance(context, args[0]);
			outputMap = domObject.getInfo(context, selects);

			Element responseElement = new Element("ObjectInfo");
			addElement(responseElement, "Type", (String)outputMap.get(SELECT_TYPE));

			addElement(responseElement, "Name", (String)outputMap.get(SELECT_NAME));

			addElement(responseElement, "Revision", (String)outputMap.get(SELECT_REVISION));

			addElement(responseElement, "CurrentAccess", (String)outputMap.get("current.access"));

			Element FileNames = new Element("FileNames");

			List<String> sFileList = (List<String>)outputMap.get(CommonDocument.SELECT_FILE_NAME);

			String isVersionObjectString    = (String) outputMap.get(CommonDocument.SELECT_IS_VERSION_OBJECT);
			boolean isVersionObject = "true".equalsIgnoreCase(isVersionObjectString);

			String sKindOfDocument = (String) outputMap.get(CommonDocument.SELECT_IS_KIND_OF_DOCUMENTS);
			boolean bKindOfDocument = "true".equalsIgnoreCase(sKindOfDocument);

			if(bKindOfDocument) { //Document object and not for workspace folder

				if(!isVersionObject) {

					for(String sFileName : sFileList) {

						if(sFileName != null && !sFileName.isEmpty()) {
							addElement(FileNames, "FileName", (String)sFileName);
						}
						else { //document object with no files
							addElement(FileNames, "FileName", "");
						}
					}
				}
				else {
					String versionFileName = (String)outputMap.get(CommonDocument.SELECT_TITLE);
					addElement(FileNames, "FileName", versionFileName);					
				}

				responseElement.addContent(FileNames);
			}

			addElement(responseElement, "SymbolicName", FrameworkUtil.getAliasForAdmin(context, "type", (String)outputMap.get(SELECT_TYPE), true));

			Document document = new Document(responseElement);
			return new XMLOutputter().outputString(document);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw ex;
		}
	}

	private void addElement(Element parentElement, String name, String value) 
	{
		Element element = new Element(name);
		element.addContent(value);
		parentElement.addContent(element);
	}
}
