/*
**  MCADLocalConfigObjectActions
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program for adding Local Config Object
*/


import matrix.db.BusinessObject;
import matrix.db.Context;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;

public class MCADLocalConfigObjectActions_mxJPO
{
	public MCADLocalConfigObjectActions_mxJPO()
    {
    }
    
	public MCADLocalConfigObjectActions_mxJPO (Context context, String[] args) throws Exception
    {
      if (!context.isConnected())
		MCADServerException.createException("not supported no desktop client", null);
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }
	
	/**
	 * Entry Point
	 * This method adds the local config object with input TNR if it does not
	 * already exist in the database and returns "true".
	 * If the object with given TNR already exists, "true" is returned.
	 */
	public String addLocalConfigObject(Context context,String[] args)throws Exception
	{
		String lcoType   = args[0];
		String lcoName   = args[1];
		String lcoRev    = args[2];
		
		String retStr = "false|";

		BusinessObject localConfigObj = null;
		try
		{

			//String vault = context.getVault().toString();
			localConfigObj = new BusinessObject(lcoType,lcoName,lcoRev,"");

			if(!localConfigObj.exists(context))
			{
				// create the business object
				localConfigObj.create(context, MCADMxUtil.getActualNameForAEFData(context, "policy_MCADInteg-ConfigObjectPolicy"));
			}
			retStr = "true|";

		}
		catch(Exception ex)
		{
			String errorMessage	= "Failed in creating Local Config Object.";

			if(null != ex && !"null".equals(ex.getMessage()))
			{
				errorMessage = errorMessage + " " + ex.getMessage();
			}

			System.out.println(errorMessage);
			localConfigObj = null;
			retStr = "false|" + errorMessage;
		}
		return retStr;
	}

	public String deleteLocalConfigObject(Context context,String[] args)throws Exception
	{
		String lcoType   = args[0];
		String lcoName   = args[1];
		String lcoRev    = args[2];
		
		String retStr = "false|";

		BusinessObject localConfigObj = null;
		try
		{

			//String vault = context.getVault().toString();
			localConfigObj = new BusinessObject(lcoType,lcoName,lcoRev,"");

			if(localConfigObj.exists(context))
			{
				// delete the business object
				localConfigObj.remove(context);
			}
			retStr = "true|";

		}
		catch(Exception ex)
		{
			String errorMessage	= "Failed to delete Local Config Object.";

			if(null != ex && !"null".equals(ex.getMessage()))
			{
				errorMessage = errorMessage + " " + ex.getMessage();
			}
			
			System.out.println(errorMessage);
			localConfigObj = null;
			retStr = "false|" + errorMessage;
		}
		return retStr;
	}
}

