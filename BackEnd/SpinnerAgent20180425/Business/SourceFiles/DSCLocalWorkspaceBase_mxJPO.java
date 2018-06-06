/*
 **  DSCLocalWorkspaceBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Get all the required information about the local workspace objects
 */

import java.util.Hashtable;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;

public class DSCLocalWorkspaceBase_mxJPO
{
	private MCADServerResourceBundle _resourceBundle	= null;
	private IEFGlobalCache _cache						= null;
	private MCADMxUtil _util							= null;
	private Hashtable _argumentsTable					= null;

	public  DSCLocalWorkspaceBase_mxJPO  ()
	{
	}

	public DSCLocalWorkspaceBase_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("not supported no desktop client", null);

		initialize(context, args);
	}

	public int mxMain(Context context, String []args) throws Exception
	{
		return 0;
	}

	/**
	 * This method initializes all the class members useful in the JPO operations
	 */
	public void initialize(Context context, String[] args) throws MCADException
	{
		try
		{
			_argumentsTable		= (Hashtable)JPO.unpackArgs(args);
			String languageName	= (String)_argumentsTable.get(MCADServerSettings.LANGUAGE_NAME);
			_resourceBundle     = new MCADServerResourceBundle(languageName);
			_cache				= new IEFGlobalCache();
			_util               = new MCADMxUtil(context, _resourceBundle, _cache);
		}
		catch(Exception e)
		{
			System.out.println("[initialize]: Exception while initializating JPO" + e.getMessage());
			MCADServerException.createException(e.getMessage(), e);
		}
	}

	/*
	 * Get all the required information about the object by selectList.
	 */
	public Hashtable getLocalWorkspaceObjectsDetails(Context context, String[] args) throws Exception 
	{
		Hashtable argumentsTable		= (Hashtable)JPO.unpackArgs(args);
		Vector tnrList					= (Vector)argumentsTable.get("tnrList");

		String REL_VERSION_OF			= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		String REL_ACTIVE_VERSION		= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");

		String SELECT_ON_MAJOR			= "from[" + REL_VERSION_OF + "].to.";
		String SELECT_ON_ACTIVE_MINOR	= "from[" + REL_ACTIVE_VERSION + "].to.";

		String ATTR_CAD_TYPE			= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType") + "]";
		String ATTR_MESSAGE_DIGEST		= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-FileMessageDigest") + "]";		
		String ATTR_SOURCE				= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";            
		String ATTR_TITLE				= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Title") + "]";            

		Hashtable tnrObjectInfoTable	= null;
		if(tnrList.size() > 0)
		{
			try
			{
				StringList busSelectionList	= new StringList();

				busSelectionList.addElement("id");
				busSelectionList.addElement("type");
				busSelectionList.addElement("name");
				busSelectionList.addElement("revision");
				busSelectionList.addElement("revisions");
				busSelectionList.addElement("locker");
				busSelectionList.addElement("current.access");
				busSelectionList.addElement(ATTR_CAD_TYPE);				
				busSelectionList.addElement(ATTR_SOURCE);
				busSelectionList.addElement(ATTR_MESSAGE_DIGEST);
				busSelectionList.addElement("current"); 
	                        busSelectionList.addElement(ATTR_TITLE);
				busSelectionList.addElement("current.access[read]");
				busSelectionList.addElement(SELECT_ON_MAJOR + "current");
				busSelectionList.addElement(SELECT_ON_MAJOR + "current.access");
				busSelectionList.addElement(SELECT_ON_MAJOR + "locker");
				busSelectionList.addElement(SELECT_ON_MAJOR + "id"); 
				busSelectionList.addElement(SELECT_ON_MAJOR + "type"); 
				busSelectionList.addElement(SELECT_ON_MAJOR + "name");
				busSelectionList.addElement(SELECT_ON_MAJOR + ATTR_MESSAGE_DIGEST); 
				busSelectionList.addElement(SELECT_ON_MAJOR + ATTR_CAD_TYPE);
				busSelectionList.addElement(SELECT_ON_MAJOR + "revisions");
				busSelectionList.addElement(SELECT_ON_MAJOR + "revisions." + SELECT_ON_ACTIVE_MINOR + "revision");
				busSelectionList.addElement("revisions." + SELECT_ON_ACTIVE_MINOR + "revision");
				busSelectionList.addElement(SELECT_ON_MAJOR + "current.access[read]");//a3h

				tnrObjectInfoTable = (Hashtable)_util.getTNRObjectInfo(context, tnrList, busSelectionList);	 
			}
			catch (Exception e)
			{
				tnrObjectInfoTable = null;
			}
		}

		return tnrObjectInfoTable;
	}
}

