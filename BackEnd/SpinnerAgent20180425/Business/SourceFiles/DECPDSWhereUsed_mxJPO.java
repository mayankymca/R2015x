/*
 **  DECPDSWhereUsed
 **
 **  Copyright Dassault Systemes, 1992-2010.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  JPO to find where all this object is used in PDS Context
 **  Similar to DSCWhereUsed JPO, however returns only the latest among the connected parent.
 */

import java.util.HashMap;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADWhereusedHelper;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;
import com.matrixone.apps.domain.util.MapList;

public class DECPDSWhereUsed_mxJPO
{
	protected MCADWhereusedHelper whereusedHelper		= null;

	public DECPDSWhereUsed_mxJPO(Context context, String[] args) throws Exception
	{
	}

	private void init(Context context, String [] args) throws Exception
	{
		HashMap argumentMap		   = (HashMap)JPO.unpackArgs(args);

		String languageStr		   = (String)argumentMap.get("languageStr");
		MCADGlobalConfigObject gco = (MCADGlobalConfigObject)argumentMap.get("GCO");
		MCADLocalConfigObject lco  = (MCADLocalConfigObject)argumentMap.get("LCO");

		MCADServerResourceBundle serverResourceBundle = new MCADServerResourceBundle(languageStr);
		whereusedHelper								  = new MCADWhereusedHelper(context, gco, lco, serverResourceBundle, null);
	}

	public Object getList(Context context, String[] args) throws Exception
	{
		init(context, args);

		MapList returnBusObjectList	= new MapList();

		HashMap argumentMap		= (HashMap)JPO.unpackArgs(args);

		String objectId			= (String)argumentMap.get("objectId");

		try
		{
			returnBusObjectList = whereusedHelper.getWhereusedList(context, objectId, "1", "*", "*",  MCADWhereusedHelper.WHEREUSED_VIEW_LATEST_AMONG_CONNECTED, false, true);
		}
		catch( Exception ex )
		{
			System.out.println("DECPDSWhereUsed::getList] Exception : " + ex.getMessage());
			MCADServerException.createException(ex.getMessage(), ex);
		}

		return returnBusObjectList;
	}
}

