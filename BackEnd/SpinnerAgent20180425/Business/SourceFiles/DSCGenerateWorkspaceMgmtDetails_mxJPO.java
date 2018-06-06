/*
**  DSCGenerateWorkspaceMgmtDetails
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Populates the rows and columns of the DSCLogFileList table
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.StringTokenizer;

import java.util.Locale;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;

import com.matrixone.apps.framework.ui.UIComponent;

public class DSCGenerateWorkspaceMgmtDetails_mxJPO
{
	/**
	*
	* @param context the Matrix <code>Context</code> object
	* @param args holds no arguments
	* @throws Exception if the operation fails
	*/
	public DSCGenerateWorkspaceMgmtDetails_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
		{
			MCADServerException.createException("not supported on desktop client", null);
		}
	}
	/**
	* This method is executed if a specific method is not specified.
	*
	* @param context the Matrix <code>Context</code> object
	* @param args holds no arguments
	* @returns nothing
	* @throws Exception if the operation fails
	*/
	private HashMap integrationNameGCOTable					= null;
	private MCADServerResourceBundle serverResourceBundle	= null;
	private MCADMxUtil util									= null;
	private String localeLanguage							= null;
	private IEFGlobalCache	cache							= null;
	private MCADGlobalConfigObject globalConfigObject		= null;
	HashMap selectDataMap								        = new HashMap();
	public int mxMain(Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
		{
			MCADServerException.createException("not supported on desktop client", null);
		}
		return 0;
	}

	public MapList getFileListForIEFClient(Context context, String [] args) throws Exception
	{
		HashMap paramMap			= (HashMap) JPO.unpackArgs(args);
		HashMap fileDetails			= (HashMap) paramMap.get("fileDetails");
		Hashtable dirListTable		= (Hashtable) paramMap.get("dirDetails");
		//command attributes like fetcontent cmd for parent directory
		Hashtable commandAttributes = (Hashtable) paramMap.get("commandAttributes");
		String directoryPath		= (String) commandAttributes.get("directoryPath");
		if(directoryPath == null)
			directoryPath = "";
		
		Iterator fileIter			= fileDetails.keySet().iterator();
		Iterator dirIter			= dirListTable.keySet().iterator();
		MapList idList				= new MapList();

		while (fileIter.hasNext())
		{
			HashMap item = new HashMap();
			String fileKey = (String) fileIter.next();
			
			HashMap hMap = (HashMap) fileDetails.get(fileKey);
			item.put("id", hMap.get("id"));
			item.put("name", hMap.get("name"));
			item.put("mxstatus", hMap.get("mxstatus"));
			item.put("cadType", hMap.get("cadType"));
			item.put("directoryPath", directoryPath);
			item.put("nodeType", "file");
			item.put("fileKey", fileKey);

			// Store the details for the current file for column retrieval
			// Note use of "curFileDetails" indicates the details for a single file.

			item.put("curFileDetails", fileDetails.get(fileKey));
			idList.add(item);
		}
		
		while (dirIter.hasNext())
		{
			HashMap item				= new HashMap();
			String dirKey    			= (String) dirIter.next();

			item.putAll(commandAttributes);
			item.put("id", "");
			item.put("nodeType", "folder");
			item.put("directoryPath", dirKey);
			item.put("name", dirListTable.get(dirKey));
			
			HashMap dirDetails = new HashMap();
			dirDetails.put("name", dirListTable.get(dirKey));
			item.put("curFileDetails", dirDetails);

			idList.add(item);
		}
		
		return idList;
	}

   /**
   * Get the list of entries to display.
   *
   * @param context the Matrix <code>Context</code> object
   * @param args holds the following input arguments:
   * 0 - objectList MapList
   * @returns Object of type MapList
   * @throws Exception if the operation fails
   */
	@com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getFileList(Context context, String [] args) throws Exception
	{
		HashMap paramMap		= (HashMap)JPO.unpackArgs(args);
		HashMap fileDetails		= this.unpackFileDetails(paramMap);

		Iterator fileIter		= fileDetails.keySet().iterator();
		MapList idList			= new MapList();

		while (fileIter.hasNext())
		{
			HashMap item = new HashMap();
			String fileKey = (String) fileIter.next();
			item.put("id", fileKey);
			// Store the details for the current file for column retrieval
			// Note use of "curFileDetails" indicates the details for a single file.
			item.put("curFileDetails", fileDetails.get(fileKey));
			idList.add(item);
		}

		return idList;
	}

   /**
   * Get the list of filenames to display in the filename column.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args contains a Map with input args (see getColumn doc)
   * @return Vector contains list of column values
   * @throws Exception if the operation fails
   */

	public Vector getFileName(Context context,String[] args) throws Exception
	{
           return this.getColumn(context,"filename", args);
	}

	/**
	* Get the list of revs to display in the rev column.
	*
	* @param context the eMatrix <code>Context</code> object
	* @param args contains a Map with input args (see getColumn doc)
	* @return Vector contains list of column values
	* @throws Exception if the operation fails
	*/

	public Vector getRev(Context context,String[] args) throws Exception
	{
		return this.getColumn(context,"revision", args);
	}

        /**
	* Get state to display in the rev column.
	*
	* @param context the eMatrix <code>Context</code> object
	* @param args contains a Map with input args (see getColumn doc)
	* @return Vector contains list of column values
	* @throws Exception if the operation fails
	*/

	public Vector getState(Context context,String[] args) throws Exception
	{
		Vector vReadAcces=this.getColumn(context,"readAcces", args);
		Vector objectId=this.getColumn(context,"objectId", args);
		Vector returnValue=this.getColumn(context,"state", args);
		Vector vctNewReturnValue = new Vector(returnValue.size());
		Vector localeLanguage=this.getColumn(context,"localeLanguage", args);

		for(int k=0;k<returnValue.size();k++){
			String sReturnVal = (String)returnValue.get(k);
			String sEachOid = (String)objectId.get(k);
		
			if(sEachOid != null )
			{
				if(!"".equals(sEachOid) )
				{
					String sReadAccess = (String) vReadAcces.get(k);
						if(!"".equals(sReadAccess) && !sReadAccess.equalsIgnoreCase("TRUE")){
								//String noAccess=UIComponent.getI18nNoAccessString(context,localeLanguage.toString());
								String noAccess="No Access";
									sReturnVal = noAccess;//"No Access";
						}
				}
			}
			vctNewReturnValue.addElement(sReturnVal);
		}
       return vctNewReturnValue;
	}

	/**
   * Get the list of revs to display in the vercolumn.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args contains a Map with input args (see getColumn doc)
   * @return Vector contains list of column values
   * @throws Exception if the operation fails
   */

   public Vector getVer(Context context,String[] args)
     throws Exception
   {
      return this.getColumn(context,"version", args);
   }

	/**
	* Get the list of names to display in the type column.
	*
	* @param context the eMatrix <code>Context</code> object
	* @param args contains a Map with input args (see getColumn doc)
	* @return Vector contains list of column values
	* @throws Exception if the operation fails
	*/

	public Vector getNameHrefLink(Context context,String[] args) throws Exception
	{
		Vector vReadAcces=this.getColumn(context,"readAcces", args);
		Vector objectId=this.getColumn(context,"objectId", args);
		Vector returnValue=this.getColumn(context,"namehrefLink", args);
		Vector vName=this.getColumn(context,"name", args);

		Vector vctNewReturnValue = new Vector(returnValue.size());
		
		for(int k=0;k<returnValue.size();k++){
			String sReturnVal = (String)returnValue.get(k);
			String sEachOid = (String)objectId.get(k);

			if(sEachOid != null )
			{
				if(!"".equals(sEachOid) )
				{
					String sReadAccess = (String) vReadAcces.get(k);
					if(!"".equals(sReadAccess) && !sReadAccess.equalsIgnoreCase("TRUE")){
						sReturnVal = (String) vName.get(k);
					}
				}
			}
			vctNewReturnValue.addElement(sReturnVal);
		}
        return vctNewReturnValue;	
	}

	public Vector getName(Context context,String[] args) throws Exception
	{
	   return this.getColumn(context,"name", args);
	}

	public Vector getTitle(Context context,String[] args) throws Exception
	{
	   return this.getColumn(context,"title", args);
	}
	/**
	* Get the list of sizes to display in the size column.
	*
	* @param context the eMatrix <code>Context</code> object
	* @param args contains a Map with input args (see getColumn doc)
	* @return Vector contains list of column values
	* @throws Exception if the operation fails
	*/

	public Vector getSize(Context context,String[] args) throws Exception
	{
	   return this.getColumn(context,"size", args);
	}

	/**
	* Get the list of types to display in the type column.
	*
	* @param context the eMatrix <code>Context</code> object
	* @param args contains a Map with input args (see getColumn doc)
	* @return Vector contains list of column values
	* @throws Exception if the operation fails
	*/

	public Vector getType(Context context,String[] args) throws Exception
	{
		return this.getColumn(context,"type",args);
	}

	/**
   * Get the list of status values to display in the status column.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args contains a Map with input args (see getColumn doc)
   * @return Vector contains list of column values
   * @throws Exception if the operation fails
   */

   public Vector getStatus(Context context,String[] args)
     throws Exception
   {
		Vector vReadAcces=this.getColumn(context,"readAcces", args);
		Vector objectId=this.getColumn(context,"objectId", args);
		Vector returnValue=this.getColumn(context,"status", args);
		Vector localeLanguage=this.getColumn(context,"localeLanguage", args);
	
		Vector vctNewReturnValue = new Vector(returnValue.size());
		for(int k=0;k<returnValue.size();k++){
			String sReturnVal = (String)returnValue.get(k);
			String sEachOid = (String)objectId.get(k);
		
			if(sEachOid != null )
			{
				if(!"".equals(sEachOid) )
				{
					String sReadAccess = (String) vReadAcces.get(k);
					
						if(!"".equals(sReadAccess) && !sReadAccess.equalsIgnoreCase("TRUE")){
							//String noAccess=UIComponent.getI18nNoAccessString(context,localeLanguage.toString());
							String noAccess="No Access";
							sReturnVal = noAccess;//"No Access";
						}
				}
			}
			vctNewReturnValue.addElement(sReturnVal);
		}
        return vctNewReturnValue; 
   }

   /**
   * Get the list of lockedby values to display in the lockedBy column.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args contains a Map with input args (see getColumn doc)
   * @return Vector contains list of column values
   * @throws Exception if the operation fails
   */

   public Vector getLockedBy(Context context,String[] args)
     throws Exception
   {
		Vector vReadAcces=this.getColumn(context,"readAcces", args);
		Vector objectId=this.getColumn(context,"objectId", args);
		Vector returnValue=this.getColumn(context,"lockedBy", args);
		Vector vctNewReturnValue = new Vector(returnValue.size());
		Vector localeLanguage=this.getColumn(context,"localeLanguage", args);
		
		for(int k=0;k<returnValue.size();k++){
			String sReturnVal = (String)returnValue.get(k);
			String sEachOid = (String)objectId.get(k);
			if(sEachOid != null )
			{
				if(!"".equals(sEachOid) )
				{
					String sReadAccess = (String) vReadAcces.get(k);
						if(!"".equals(sReadAccess) && !sReadAccess.equalsIgnoreCase("TRUE")){
							//String noAccess=UIComponent.getI18nNoAccessString(context,localeLanguage.toString());
							String noAccess="No Access";
							sReturnVal = noAccess;
						//	sReturnVal = "No Access";
						}
				}
			}
			vctNewReturnValue.addElement(sReturnVal);
		}
        return vctNewReturnValue;  

   }

   /**
   * Get the list of insession values to display in the inSession column.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args contains a Map with input args (see getColumn doc)
   * @return Vector contains list of column values
   * @throws Exception if the operation fails
   */

   public Vector getInSession(Context context,String[] args)
     throws Exception
   {
		Vector vReadAcces=this.getColumn(context,"readAcces", args);
		Vector objectId=this.getColumn(context,"objectId", args);
		Vector returnValue=this.getColumn(context,"inSession", args);
		Vector vctNewReturnValue = new Vector(returnValue.size());
		Vector localeLanguage=this.getColumn(context,"localeLanguage", args);

		for(int k=0;k<returnValue.size();k++){
			String sReturnVal = (String)returnValue.get(k);
			String sEachOid = (String)objectId.get(k);
			if(sEachOid != null )
			{
				if(!"".equals(sEachOid) )
				{
					String sReadAccess = (String) vReadAcces.get(k);
						if(!"".equals(sReadAccess) && !sReadAccess.equalsIgnoreCase("TRUE")){
							//String noAccess=UIComponent.getI18nNoAccessString(context,localeLanguage.toString());
							String noAccess="No Access";
							sReturnVal = noAccess;//"No Access";
					
						}
				}
			}
			vctNewReturnValue.addElement(sReturnVal);
		}
        return vctNewReturnValue;	   
   }

   /**
    * Get the list of values to display selectable or non-selectable checkboxes
    *
    * @param context the eMatrix <code>Context</code> object
    * @param args contains a Map with input args (see getColumn doc)
    * @return Vector contains list of column values
    * @throws Exception if the operation fails
    */

    public Vector getSelectable(Context context,String[] args) throws Exception
    {
	   	Vector vReadAcces=this.getColumn(context,"readAcces", args);
	   	Vector objectId=this.getColumn(context,"objectId", args);
		Vector inSessionObjects = this.getColumn(context,"inSession", args);
		Vector selectable = new Vector();
		for(int i=0; i<inSessionObjects.size(); i++) 
		{
			String inSession = (String) inSessionObjects.elementAt(i);
            if(inSession.equalsIgnoreCase("yes")) 
			{
				selectable.addElement("false");
            }
			else 
			{
				String sReturnVal ="true";
				String sEachOid = (String)objectId.get(i);
				String sReadAccess = (String) vReadAcces.get(i);

				if(sEachOid != null )
				{
					if(!"".equals(sEachOid) )
					{
						if(!"".equals(sReadAccess) && !sReadAccess.equalsIgnoreCase("TRUE")){
							sReturnVal = "false";
						}
					}
				}
				selectable.addElement(sReturnVal);
            }
		}
		return selectable;
    }

   /**
	* Get the list of access values to display in the Access column.
	*
	* @param context the eMatrix <code>Context</code> object
	* @param args contains a Map with input args (see getColumn doc)
	* @return Vector contains list of column values
	* @throws Exception if the operation fails
	*/

	public Vector getAccess(Context context,String[] args) throws Exception
	{
		Vector vReadAcces=this.getColumn(context,"readAcces", args);
		Vector objectId=this.getColumn(context,"objectId", args);
		Vector returnValue=this.getColumn(context,"access", args);
		Vector vctNewReturnValue = new Vector(returnValue.size());
		Vector localeLanguage=this.getColumn(context,"localeLanguage", args);
		
		for(int k=0;k<returnValue.size();k++){
			String sReturnVal = (String)returnValue.get(k);
			String sEachOid = (String)objectId.get(k);
			if(sEachOid != null )
			{
				if(!"".equals(sEachOid) )
				{
					String sReadAccess = (String) vReadAcces.get(k);
						if(!"".equals(sReadAccess) && !sReadAccess.equalsIgnoreCase("TRUE")){
							//String noAccess=UIComponent.getI18nNoAccessString(context,localeLanguage.toString());
							String noAccess="No Access";
							sReturnVal = noAccess;//"No Access";
					
						}
				}
			}
			vctNewReturnValue.addElement(sReturnVal);
		}
        return vctNewReturnValue;			
	}

	public Vector getHtmlForPopupLink(Context context,String[] args) throws Exception
	{
		Vector vReadAcces=this.getColumn(context,"readAcces", args);
		Vector objectId=this.getColumn(context,"objectId", args);
		Vector returnValue=this.getColumn(context,"htmlForPopupLink", args);
		Vector localeLanguage=this.getColumn(context,"localeLanguage", args);

		Vector vctNewReturnValue = new Vector(returnValue.size());
		
		for(int k=0;k<returnValue.size();k++){
			String sReturnVal = (String)returnValue.get(k);
			String sEachOid = (String)objectId.get(k);
			if(sEachOid != null )
			{
				if(!"".equals(sEachOid) )
				{
					String sReadAccess = (String) vReadAcces.get(k);
						if(!"".equals(sReadAccess) && !sReadAccess.equalsIgnoreCase("TRUE")){
							//String noAccess=UIComponent.getI18nNoAccessString(context,localeLanguage.toString());
							String noAccess="No Access";
							sReturnVal = noAccess;//"No Access";

						}
				}
			}
			vctNewReturnValue.addElement(sReturnVal);
		}
        return vctNewReturnValue;	
	}

	public Vector getCurrentVersionForPLMTree(Context context, String[] args) throws Exception
	{
		HashMap programMap 				= (HashMap)JPO.unpackArgs(args);
		HashMap paramMap				= (HashMap)programMap.get("paramList");
		HashMap integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");
		String localeLanguage			= (String)paramMap.get("LocaleLanguage");
		MapList relBusObjPageList		= (MapList)programMap.get("objectList");
		Vector currentVersionList		= new Vector(relBusObjPageList.size());
		String[] objIds					= new String[relBusObjPageList.size()];
		
		for(int i = 0; i < relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");	
		}
		
		Hashtable curVersionList = getCurrentVersion(context, objIds, localeLanguage, integrationNameGCOTable,false);
		
		for (int index = 0; index < objIds.length; index++) 
		{
			String currentVersion = (String) curVersionList.get(objIds[index]);
			currentVersionList.add(currentVersion);
		}
		return currentVersionList;
	}
	
	/**
	 * 
	 * This method used in DECPLMTreeDetails Table for getting current version
	 * getCurrentVersion() should pass true in last parameter to support above mention behavior.. 
	 * if the busid is minor & in release state then method should return major's current version.   
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public Vector getCurrentVersionForPLMTreeDetails(Context context, String[] args) throws Exception
	{
		HashMap programMap 				= (HashMap)JPO.unpackArgs(args);
		HashMap paramMap				= (HashMap)programMap.get("paramList");
		HashMap integrationNameGCOTable	= (HashMap)paramMap.get("GCOTable");
		String localeLanguage			= (String)paramMap.get("LocaleLanguage");
		MapList relBusObjPageList		= (MapList)programMap.get("objectList");
		Vector currentVersionList		= new Vector(relBusObjPageList.size());
		String[] objIds					= new String[relBusObjPageList.size()];
		
		for(int i = 0; i < relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");	
		}
		
		Hashtable curVersionList = getCurrentVersion(context, objIds, localeLanguage, integrationNameGCOTable,true);
		
		for (int index = 0; index < objIds.length; index++) 
		{
			String currentVersion = (String) curVersionList.get(objIds[index]);
			currentVersionList.add(currentVersion);
		}
		return currentVersionList;
	}
	
	public Vector getCurrentVersionForLWM(Context context, String[] args) throws Exception
	{
		Vector allObjectIdList			= this.getColumn(context,"id", args);
		Vector currentVersionList		= new Vector(allObjectIdList.size());
		ArrayList objectIds				= new ArrayList();
		HashMap programMap				= (HashMap) JPO.unpackArgs(args);
		HashMap paramMap				= (HashMap)programMap.get("paramList");
		HashMap integrationNameGCOTable	= (HashMap)paramMap.get("gcoTable");
		String localeLanguage			= (String)paramMap.get("languageStr");

		for(int i=0; i < allObjectIdList.size(); i++)
		{
			String objectId = (String)allObjectIdList.elementAt(i);

			if(! "".equals(objectId.trim()) && !objectIds.contains(objectId))
			{
				objectIds.add(objectId);
			}

			// Initializing vector elements to "".
			currentVersionList.add("");
		}

        String[] objIds = new String[objectIds.size()];
		objectIds.toArray(objIds);
		
		Hashtable curVersionList = getCurrentVersion(context, objIds, localeLanguage, integrationNameGCOTable,false);
		
		for (int index = 0; index < allObjectIdList.size(); index++) 
		{
			String busId = (String) allObjectIdList.get(index);
			
			if(! "".equals(busId.trim()) )
			{
				String currentVersion = (String) curVersionList.get(busId);
				currentVersionList.insertElementAt(currentVersion, index);
			}
		}
		
		return currentVersionList;
	}
	public Vector getCurrentVersionForWebLWM(Context context, String[] args) throws Exception
	{
		return this.getColumn(context,"activeversion", args);
	}
	private Hashtable getCurrentVersion(Context context, String[] objIds, String localeLanguage, HashMap integrationNameGCOTable ,boolean bFinalizeFlag) throws Exception
	{
		Hashtable currentVersionList 			= new Hashtable();
		
		MCADServerResourceBundle resourceBundle	= new MCADServerResourceBundle(localeLanguage);
		IEFGlobalCache _cache					= new IEFGlobalCache();
		IEFIntegAccessUtil _util				= new IEFIntegAccessUtil(context, resourceBundle, _cache);
		
		
		String REL_VERSION_OF 	= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		String sFinalizedRelName 			= MCADMxUtil.getActualNameForAEFData(context, "relationship_Finalized");
		String sFinalizedAttribName 		= MCADMxUtil.getActualNameForAEFData(context, "attribute_IsFinalized");
		
		String SELECT_SOURCE_ATTR			= "attribute[" + (String)PropertyUtil.getSchemaProperty(context, "attribute_Source") + "]";
		
		String SELECT_ON_MAJOR	= "from[" + REL_VERSION_OF + "].to.";
		
		String MAJOR_THRU_FINALIZED  		= "from[" + sFinalizedRelName + "].to.id";
		String FINALIZED_ATTR_ON_VERSION_OF = "from[" + REL_VERSION_OF + "].attribute[" + sFinalizedAttribName + "]";
		
		Vector assignedIntegrations = _util.getAssignedIntegrations(context);
		
		StringList busSelectionList = new StringList();
		busSelectionList.addElement("id");
		busSelectionList.addElement("type");
		busSelectionList.addElement("revision");
		busSelectionList.addElement(SELECT_SOURCE_ATTR);
		busSelectionList.addElement(SELECT_ON_MAJOR + "revision");
		busSelectionList.add(MAJOR_THRU_FINALIZED);
		busSelectionList.add(FINALIZED_ATTR_ON_VERSION_OF);

		BusinessObjectWithSelectList buslWithSelectionList	= BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);
		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			MCADGlobalConfigObject globalConfigObj	= null;
			
			BusinessObjectWithSelect busObjectWithSelect 	= (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
			String busId								 	= busObjectWithSelect.getSelectData("id");
			
			String integrationSource					    = busObjectWithSelect.getSelectData(SELECT_SOURCE_ATTR);
			
			String integrationName = null;
			
			StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");				

			if(integrationSourceTokens.hasMoreTokens())
				integrationName = integrationSourceTokens.nextToken();

			if(integrationName != null && integrationNameGCOTable != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName))
			{
				globalConfigObj	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
			}

			if(globalConfigObj != null)
			{
				String currentRevision		= "";
				//String busType				= busObjectWithSelect.getSelectData("type"); //[NDM] OP6
				boolean isCreateVersionObjEnabled = globalConfigObj.isCreateVersionObjectsEnabled();
				
				if(_util.isMajorObject(context, busId))//globalConfigObj.isMajorType(busType)) //[NDM] OP6
				{
					currentRevision = busObjectWithSelect.getSelectData("revision");
				}
				else
				{
					boolean isFinalized = false;
					
					if(bFinalizeFlag)
					{
						String majorIdThroughFinalizedRelationship = busObjectWithSelect.getSelectData(MAJOR_THRU_FINALIZED);

						if(majorIdThroughFinalizedRelationship != null && !majorIdThroughFinalizedRelationship.equals(""))
						{
							// The object is in either finalized state or a higher state
							isFinalized = true;
						}
						else
						{
							String finalizedAttributeOnVersionOf = busObjectWithSelect.getSelectData(FINALIZED_ATTR_ON_VERSION_OF);

							if(finalizedAttributeOnVersionOf != null && finalizedAttributeOnVersionOf.equalsIgnoreCase("true"))
							{
								isFinalized = true;
							}
						}
					}
					
					if(!isCreateVersionObjEnabled || isFinalized)
					{
						currentRevision = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revision");
					}
					else
					{
						currentRevision = busObjectWithSelect.getSelectData("revision");
					}
				}				
					
				if(currentRevision != null && ! "".equals(currentRevision))
				{
					currentVersionList.put(busId, currentRevision);
				}
			}
		}

		return currentVersionList;
	}

	public Vector getLatestVersionForPLMTree(Context context, String[] args) throws Exception
	{
		HashMap programMap 				= (HashMap)JPO.unpackArgs(args);
		HashMap paramMap				= (HashMap)programMap.get("paramList");
		
		HashMap integrationNameGCOTable	 = (HashMap)programMap.get("GCOTable");
		if(integrationNameGCOTable == null)
			integrationNameGCOTable =  (HashMap)paramMap.get("GCOTable");
			
		String localeLanguage	    = (String) programMap.get("LocaleLanguage");
		if(localeLanguage == null)
			localeLanguage          = (String) paramMap.get("LocaleLanguage");
		
		MapList relBusObjPageList		= (MapList)programMap.get("objectList");
		Vector latestVersionList		= new Vector(relBusObjPageList.size());
		String[] objIds					= new String[relBusObjPageList.size()];
		
		for(int i = 0; i < relBusObjPageList.size(); i++)
		{
			Map objDetails	= (Map)relBusObjPageList.get(i);
			objIds[i]		= (String)objDetails.get("id");	
		}
		
		Hashtable latestVerList = getLatestVersion(context, objIds, localeLanguage, integrationNameGCOTable);
		
		for (int index = 0; index < objIds.length; index++) 
		{
			String currentVersion = (String) latestVerList.get(objIds[index]);
			latestVersionList.add(currentVersion);
		}
		
		return latestVersionList;
	}
	
	public Vector getLatestVersionForLWM(Context context, String[] args) throws Exception
	{
		HashMap programMap				= (HashMap) JPO.unpackArgs(args);
		HashMap paramMap				= (HashMap)programMap.get("paramList");
		HashMap integrationNameGCOTable	= (HashMap)paramMap.get("gcoTable");
		String localeLanguage			= (String)paramMap.get("languageStr");
		
		Vector allObjectIdList			= this.getColumn(context,"id", args);
		Vector latestVersionList		= new Vector(allObjectIdList.size());
		ArrayList objectIds				= new ArrayList();

		for(int i=0; i < allObjectIdList.size(); i++)
		{
			String objectId = (String)allObjectIdList.elementAt(i);

			if(! "".equals(objectId.trim()) && !objectIds.contains(objectId))
			{
				objectIds.add(objectId);
			}

			// Initializing vector elements to "".
			latestVersionList.add("");
		}

		String[] objIds = new String[objectIds.size()];
		objectIds.toArray(objIds);

		Hashtable latestVerList = getLatestVersion(context, objIds, localeLanguage, integrationNameGCOTable);
		
		for (int index = 0; index < allObjectIdList.size(); index++) 
		{
			String busId = (String) allObjectIdList.get(index);
			
			if(! "".equals(busId.trim()) )
			{
				String latestVersion = (String) latestVerList.get(busId);
				latestVersionList.insertElementAt(latestVersion, index);
			}
		}
		
		return latestVersionList;
	}
	public Vector getLatestVersionForWebLWM(Context context, String[] args) throws Exception
	{
		return this.getColumn(context,"latestversion", args);
	}
	private Hashtable getLatestVersion(Context context, String[] objIds, String localeLanguage, HashMap integrationNameGCOTable) throws Exception
	{
		Hashtable latestVersionList = new Hashtable();
		
		MCADServerResourceBundle resourceBundle	= new MCADServerResourceBundle(localeLanguage);
		IEFGlobalCache _cache					= new IEFGlobalCache();
		IEFIntegAccessUtil _util			= new IEFIntegAccessUtil(context, resourceBundle, _cache);
		MCADGlobalConfigObject globalConfigObj	= null;
		
		String REL_VERSION_OF 		= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		String REL_ACTIVE_VERSION 	= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
		
		String SELECT_ON_MAJOR			= "from[" + REL_VERSION_OF + "].to.";
		String SELECT_ON_ACTIVE_MINOR	= "from[" + REL_ACTIVE_VERSION + "].to.";

		String SELECT_SOURCE_ATTR			= "attribute[" + (String)PropertyUtil.getSchemaProperty(context, "attribute_Source") + "]";

		StringList busSelectionList = new StringList();
		busSelectionList.addElement("id");
		busSelectionList.addElement("type");
		busSelectionList.addElement("revision");
		busSelectionList.addElement("last.policy");
		busSelectionList.addElement("last.current");
		busSelectionList.addElement("last.state");
		busSelectionList.addElement("last.revision");
		busSelectionList.addElement("last." + SELECT_ON_ACTIVE_MINOR + "revision");		
		busSelectionList.addElement(SELECT_ON_MAJOR + "last.policy");
		busSelectionList.addElement(SELECT_ON_MAJOR + "last.current");
		busSelectionList.addElement(SELECT_ON_MAJOR + "last.state");
		busSelectionList.addElement(SELECT_ON_MAJOR + "last.revision");
		busSelectionList.addElement(SELECT_ON_MAJOR + "last." + SELECT_ON_ACTIVE_MINOR + "revision");
busSelectionList.addElement(SELECT_SOURCE_ATTR);

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			String latestRevision							= "";
			BusinessObjectWithSelect busObjectWithSelect	= (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
			String busId								 	= busObjectWithSelect.getSelectData("id");
			String integrationName							= ""; //_util.getIntegrationName(context, busId);

			String integrationSource = busObjectWithSelect.getSelectData(SELECT_SOURCE_ATTR);
			StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");				

			if(integrationSourceTokens.hasMoreTokens())
				integrationName = integrationSourceTokens.nextToken();

			if(integrationName != null && integrationNameGCOTable != null && integrationNameGCOTable.containsKey(integrationName) && _util.getAssignedIntegrations(context).contains(integrationName))
			{
				globalConfigObj	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
			}

			
			if(globalConfigObj != null)
			{
				boolean isCreateVersionObjEnabled = globalConfigObj.isCreateVersionObjectsEnabled();
				//String busType	= busObjectWithSelect.getSelectData("type"); //{NDM] OP6
				
				if(_util.isMajorObject(context, busId))//globalConfigObj.isMajorType(busType)) //{NDM] OP6
				{
					String latestMajorPolicy		= busObjectWithSelect.getSelectData("last.policy");
					String latestMajorState			= busObjectWithSelect.getSelectData("last.current");
					StringList latestMajorStateList	= busObjectWithSelect.getSelectDataList("last.state");
					String finalizationState		= globalConfigObj.getFinalizationState(latestMajorPolicy);
					
					if(!isCreateVersionObjEnabled || latestMajorStateList.lastIndexOf(latestMajorState) >= latestMajorStateList.lastIndexOf(finalizationState))
					{
						latestRevision = busObjectWithSelect.getSelectData("last.revision");
					}
					else
					{
						latestRevision = busObjectWithSelect.getSelectData("last." + SELECT_ON_ACTIVE_MINOR + "revision");
					}					
				}
				else
				{
					String latestMinorPolicy		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "last.policy");
					String latestMinorState			= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "last.current");
					StringList latestMinorStateList	= busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR + "last.state");
					String finalizationState		= globalConfigObj.getFinalizationState(latestMinorPolicy);

					if(!isCreateVersionObjEnabled || latestMinorStateList.lastIndexOf(latestMinorState) >= latestMinorStateList.lastIndexOf(finalizationState))
					{
						latestRevision = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "last.revision");
					}
					else
					{
						latestRevision = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "last." + SELECT_ON_ACTIVE_MINOR + "revision");
					}
				}
				
				if(null == latestRevision || "".equals(latestRevision))
				{
					Hashtable JPOArgsTable = new Hashtable();
					JPOArgsTable.put(MCADServerSettings.GCO_OBJECT, globalConfigObj);
					JPOArgsTable.put(MCADServerSettings.LANGUAGE_NAME, resourceBundle.getLanguageName());
					JPOArgsTable.put(MCADServerSettings.OBJECT_ID, busId);
					JPOArgsTable.put(MCADServerSettings.JPO_METHOD_NAME, MCADGlobalConfigObject.FEATURE_CHECKOUT);

					String [] packedArgumentsTable = JPO.packArgs(JPOArgsTable);
					String [] args = new String[2];
					args[0] = packedArgumentsTable[0];
					args[1] = packedArgumentsTable[1];

					String jpoName   = "MCADJPOUtils";
					String jpoMethod = "execute";
					String[] init = new String[] {};
					
					Hashtable resultDataTable = (Hashtable)JPO.invoke(context, jpoName, init, jpoMethod, args, Hashtable.class);
					
					String validObjectId = (String) resultDataTable.get(MCADServerSettings.JPO_EXECUTION_RESULT);
					
					BusinessObject validBusObject = new BusinessObject(validObjectId);
					validBusObject.open(context);
					latestRevision 	= validBusObject.getRevision();
					validBusObject.close(context);
				}

				if(latestRevision != null && ! "".equals(latestRevision))
				{
					latestVersionList.put(busId, latestRevision);
				}
			}
		}

		return latestVersionList;
	}

   /**
	* Unpack the fileDetails TreeMap from the parameter map.
	*
	* @param paramMap holds a JPO.packArgs packed TreeMap of file details (see emxDSCWorkspaceMgmtDetails2.jsp for details):
	* @returns Object of type TreeMap containing file details
	* @throws Exception if the operation fails
	*/
	private HashMap unpackFileDetails(HashMap paramMap) throws Exception
	{
		String jpoPackedArgs	= (String)paramMap.get("jpoPackedArgsFileDetails");
		jpoPackedArgs			= MCADUrlUtil.hexDecode(jpoPackedArgs);
		HashMap fileDetails		= (HashMap)MCADUtil.covertToObject(jpoPackedArgs, true);

        return  fileDetails;
	}

	/**
	* Get the list of column values to display for a given column
	*
	* @param args contains a Map with the following entries:
	*    paramList contains a HashMap that contains the file details (see unpackFileDetails)
	*    objectList MapList
	* @return Vector contains list of column values
	* @throws Exception if the operation fails
	*/

private Vector getColumn(Context context,String columnKey, String[] args) throws Exception
	{
		HashMap programMap	= (HashMap) JPO.unpackArgs(args);
		MapList objectList	= (MapList)programMap.get("objectList");
		Vector vec			= new Vector(objectList.size());


		String policyName				= null;
		String localeLanguage="";


		
		HashMap paramList 			= (HashMap)programMap.get("paramList");
		
		HashMap integrationNameGCOTable	=new HashMap();

		if(paramList != null)
		{
			integrationNameGCOTable	= (HashMap)paramList.get("GCOTable");
						
			if(paramList.containsKey("languageStr"))
				localeLanguage	   	= paramList.get("languageStr").toString();
			else
			{
				localeLanguage		= (String)paramList.get("LocaleLanguage");
				
				if(localeLanguage == null || localeLanguage.equals(""))
				{
						Locale LocaleObj	= (Locale)paramList.get("localeObj");

						if(null != LocaleObj)
						{
							localeLanguage = LocaleObj.toString();
						}
				}
			}
		}


		try
		{
			for(int i=0; i < objectList.size(); i++)
			{
				HashMap collMap = (HashMap) objectList.get(i);

				HashMap curFileDetails = (HashMap) collMap.get("curFileDetails");
				String value = (String) curFileDetails.get(columnKey);

				if(columnKey.equals("type"))
				{

				value=MCADMxUtil.getNLSName(context, "Type", value, "", "" , localeLanguage);
		
				}
				
				if(columnKey.equals("state"))
				{

		                ArrayList al= new ArrayList();

				StringTokenizer integrationSourceTokens = new StringTokenizer((String)collMap.get("id"), "|");				
				int j=0;
				while(integrationSourceTokens.hasMoreTokens())
				{
				String sType   = (String) integrationSourceTokens.nextElement();
				al.add(j, sType);
				j++;
			    }
		
				if(al.size()>1){
				BusinessObject validBusObject = new BusinessObject(al.get(1).toString());
				validBusObject.open(context);
				value	=  MCADMxUtil.getNLSName(context, "State", value, "Policy",validBusObject.getPolicy().toString(), localeLanguage); 
				validBusObject.close(context);
				}
		
				}
				if(value == null)
				value = (String) curFileDetails.get(columnKey);
				if(value == null)
					value = "";
				
				vec.addElement(value);
			}
		}
		catch (Exception e)
		{
			MCADServerException.createException(e.toString(), e);
		}

		return vec;
	}
}

