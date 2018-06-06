/*
 **  IEFInstallationUtil
 **
 **  Copyright Dassault Systemes, 1992-2011.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  JPO to register integration to IEF
 */

import java.util.StringTokenizer;

import matrix.db.Context;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class IEFInstallationUtil_mxJPO
{	
	public IEFInstallationUtil_mxJPO(Context context, String[] args) throws Exception
	{

	}

	public void appendRoleToParent( Context context,  String[] args) throws Exception
	{
		String inputRoleToModify = MCADMxUtil.getActualNameForAEFData(context, args[0]);
		String parentRoleToAdd	 = MCADMxUtil.getActualNameForAEFData(context, args[1]);

		String parentString		 = "";
		String result 			 = "";

		MCADMxUtil util			 = new MCADMxUtil(context, new MCADServerResourceBundle(""), new IEFGlobalCache());

		String Args[] = new String[3];
		Args[0] = inputRoleToModify;
		Args[1] = "parent";
		Args[2] = "|";
		String sResult 			 = util.executeMQL(context,"print role $1 select $2 dump $3", Args);
		java.util.List<String> args1 = new java.util.ArrayList<String>();
		StringBuffer cmd = new StringBuffer("modify role");
		int c=3;
		if(sResult.startsWith("true"))
		{
			result 				 = sResult.substring(5);
			StringTokenizer	stk	 = new StringTokenizer(result, "|");
			cmd.append(" $1 $2");

			args1.add(inputRoleToModify);
			args1.add("parent");
			while(stk.hasMoreElements())
			{
				cmd.append(" $");
				cmd.append(Integer.toString(c++));
				args1.add(stk.nextToken());
				cmd.append(",");
			
			}
		}
		else 
		{
			result = sResult.substring(6);
			MCADServerException.createException(result, null);
		}

		cmd.append(" $");
		cmd.append(Integer.toString(c++));
		args1.add(parentRoleToAdd);
  
		String result2  = util.executeMQL(context, cmd.toString(), (String[])args1.toArray(new String[0]));
		
		if(result2.startsWith("false"))
		{
			result = sResult.substring(6);
			MCADServerException.createException(result, null);			
		}
		
		return;
	}	
}
