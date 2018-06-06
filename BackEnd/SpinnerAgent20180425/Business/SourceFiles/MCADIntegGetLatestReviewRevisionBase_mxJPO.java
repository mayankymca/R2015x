/*
 **  MCADIntegGetLatestReviewRevisionBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to get latest version object
 */
/*
**  MCADIntegGetLatestInWorkRevisionBase
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to get latest revision
*/
import java.util.HashMap;
import java.util.Hashtable;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class MCADIntegGetLatestReviewRevisionBase_mxJPO
{
 
	protected String REL_VERSION_OF							 = "";
	protected String SELECT_ON_MAJOR						 = "";
	MCADMxUtil util											 = null;


    public MCADIntegGetLatestReviewRevisionBase_mxJPO ()
    {
    }

    public MCADIntegGetLatestReviewRevisionBase_mxJPO (Context context, String[] args) throws Exception
    {
      if (!context.isConnected())
		MCADServerException.createException("Not supported on desktop client!!!", null);

		String [] packedGCO = new String[2];
        packedGCO[0] = args[0];
        packedGCO[1] = args[1];

        String sLanguage = args[2];

        init(context, packedGCO, sLanguage);

		util 						= new MCADMxUtil(context, null, new IEFGlobalCache());
    }

	public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }

    protected void init(Context context, String[] packedGCO, String sLanguage)  throws Exception
    {
		REL_VERSION_OF		  	= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		SELECT_ON_MAJOR		  	= "from[" + REL_VERSION_OF + "].to.";
    }

	public String getLatest(Context context, String[] args) throws Exception
	{
		String busid			= args[0];
		// This is a MUST
		Hashtable returnTable	= getLatestForObjectIds(context, args);

		return (String)returnTable.get(busid);
	}

    public Hashtable getLatestForObjectIds(Context context, String[] args) throws Exception
    {
	    Hashtable retTable = new Hashtable();

		retTable = util.getObjectIdForParticularView(context,"state_Review",args);

		return retTable;
    }
}
