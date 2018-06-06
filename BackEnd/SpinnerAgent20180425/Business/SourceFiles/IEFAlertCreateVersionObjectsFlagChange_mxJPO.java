/*
**  IEF-VersionAccess
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to check if versioning flag in GCO is enabled.
*/

import matrix.db.Context;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class IEFAlertCreateVersionObjectsFlagChange_mxJPO
{

	String _sObjectID = null;
	/**
	 * The no-argument constructor.
	 */
	public IEFAlertCreateVersionObjectsFlagChange_mxJPO()
	{
	}

	/**
	 * Constructor wich accepts the Matrix context and an array of String
	 * arguments.
	 */
	public IEFAlertCreateVersionObjectsFlagChange_mxJPO(Context context, String[] args) throws Exception
	{
        _sObjectID =args[0];	// Get the OBJECTID of the object in context 
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle("");
		MCADMxUtil util			= new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());		
		String Args[] = new String[1];
		Args[0] = serverResourceBundle.getString("mcadIntegration.Server.Message.WarnChangeInCreateVersionObjectsFlag");
		String result			= util.executeMQL(context, "notice $1", Args);
		
		return 0;		
	}	
}
