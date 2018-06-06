/*
**  MCADIntegGetLatestRevisionBase
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to get latest revision
*/
import java.util.Hashtable;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class MCADIntegGetLatestRevisionBase_mxJPO 
{
    protected MCADGlobalConfigObject _globalConfig			= null;
    protected MCADMxUtil _util								= null;
    protected MCADServerGeneralUtil _generalUtil				= null;
    protected MCADServerResourceBundle _serverResourceBundle  = null;
	protected IEFGlobalCache _cache							= null;

	protected String REL_VERSION_OF							 = "";
	protected String SELECT_ON_MAJOR						 = "";

	protected String REL_ACTIVE_VERSION						 = "";
	protected String SELECT_ON_ACTIVE_MINOR_ID	             = "";

    public MCADIntegGetLatestRevisionBase_mxJPO ()
    {
    }

    public MCADIntegGetLatestRevisionBase_mxJPO (Context context, String[] args) throws Exception
    {
		if (!context.isConnected())
			MCADServerException.createException("Not supported on desktop client!!!", null);

        String [] packedGCO = new String[2];
        packedGCO[0] = args[0];
        packedGCO[1] = args[1];

        String sLanguage = args[2];

        init(context, packedGCO, sLanguage);
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }

    protected void init(Context context, String[] packedGCO, String sLanguage) throws Exception
    {
        _serverResourceBundle = new MCADServerResourceBundle(sLanguage);
		_cache				  = new IEFGlobalCache();
        _globalConfig = (MCADGlobalConfigObject) JPO.unpackArgs(packedGCO);
        _util = new MCADMxUtil(context, _serverResourceBundle, _cache);
        _generalUtil = new MCADServerGeneralUtil(context,_globalConfig, _serverResourceBundle, _cache);

		REL_VERSION_OF		  = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		SELECT_ON_MAJOR		  = "from[" + REL_VERSION_OF + "].to.";
   }

    /** This function contains the implementation for returning the latest revision of a business object
         The function is passed the business object ID as argument. The implementation should return
         the bus ID of the latest revision, i.e. the latest finalized major object.
		- Additionally a check has been made in case a finalized instance comes into a root node. If it is 
			caught then the connected family is called.
         @param context The user context
         @param args A string array of arguments used. The first element of the array MUST be the
         busID, the others are optional, depending on the implementation.
         @return  A String object containing the bus ID of the latest revision.
    */
	public String getLatest(Context context, String[] args) throws Exception
	{
		// This is a MUST
		String busId			= args[0];

		Hashtable returnTable	= getLatestForObjectIds(context, args);
		
	   return (String)returnTable.get(busId);
	}

    // [NDM] QWJ : Start
	public Hashtable getLatestForObjectIds(Context context, String[] args) throws Exception
    {
		Hashtable retTable = new Hashtable(args.length);
		StringList busSelectionList = new StringList(4);
		String IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context,"attribute_IsVersionObject");
		String SELECT_ISVERSIONOBJ = "attribute["+IS_VERSION_OBJ+"]";
		busSelectionList.addElement("id");
		busSelectionList.addElement("last.id"); // latest major id from major
		busSelectionList.addElement(SELECT_ISVERSIONOBJ);
		
		busSelectionList.addElement(SELECT_ON_MAJOR + "last.id"); // latest major id from minor

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, args, busSelectionList);

		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			String outputId			 = null;

			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
			String sIsVersion= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
			Boolean bIsVersion = Boolean.valueOf(sIsVersion);
			   
			String busID			 = busObjectWithSelect.getSelectData("id");
		
			if(!bIsVersion)
				outputId = busObjectWithSelect.getSelectData("last.id");
			else
				outputId = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "last.id");
				
			if(outputId == null || outputId.equals(""))
				outputId = busID;
			
			retTable.put(busID, outputId);
		}

		return	retTable;
    }
	

}

