/*
**  IEFGeneralSearchDeleteCheck.java
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**	*/

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.server.MCADServerSettings;

import matrix.db.BusinessObject;
import matrix.db.Context;
import java.util.StringTokenizer;
import java.util.Hashtable;

public class IEFGeneralSearchDeleteCheck_mxJPO 
{	
	private MCADServerResourceBundle serverResourceBundle;
	private MCADMxUtil util;

	public IEFGeneralSearchDeleteCheck_mxJPO (Context context, String[] args) throws Exception
	{
		serverResourceBundle 	=  new MCADServerResourceBundle("");
		util					= new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());

	}	

	public void checkGeneralSearchDelete(Context context, String[] args) throws Exception
	{
	 		String isDECDelete = util.getRPEforOperation(context,MCADServerSettings.ISDECDelete);
			
			if(!isDECDelete.equals("true"))
				MCADServerException.createException(serverResourceBundle.getString("mcadIntegration.Server.Message.CantDeleteFromGeneralSearch"), null);
	}

}
