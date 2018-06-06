/**
 * IEFSecurityContextUtil
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 * This JPO gets all revisions for a BusinessObject
 * Project. Infocentral Migration to UI level 3
 * $Archive: $
 * $Revision: 1.2$
 * $Author: 
 * @since AEF 9.5.2.0
 */

import matrix.db.Context;

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADStringUtils;
import com.matrixone.apps.domain.util.PropertyUtil;


public class IEFSecurityContextUtil_mxJPO
{

	public IEFSecurityContextUtil_mxJPO(Context context, String[] args) throws Exception
	{		

	}

	public Boolean isEnvironmentCentricSolution(Context context, String[] args) throws Exception
	{
		String solution	= args[0];
		Boolean checkSolutionBased	= new Boolean(false);

		if("TEAM".equalsIgnoreCase(solution))
		{
			checkSolutionBased	= Boolean.TRUE;
		}

		return checkSolutionBased;

	}

	public String getSolution(Context context, String[] args) throws Exception
	{
		String solution	= "";

		String role	= context.getRole();

		if(null != role && !"".equals(role))
		{
			String projectAssigned	= "";

			StringBuffer roleString	= new StringBuffer(role);
			int index	= roleString.lastIndexOf(".");
			projectAssigned = roleString.substring(index + 1);

			solution	= PropertyUtil.getAdminProperty(context, "role", projectAssigned, "SOLUTION");

			if((MCADStringUtils.isNullOrEmpty(solution) || !solution.equalsIgnoreCase("TEAM")) && isUserVPLMAdminAndTeamGCOExist(context))
			{
				solution = "TEAM";
			}
		}

		return solution;
	}

	private boolean isUserVPLMAdminAndTeamGCOExist(Context context) throws Exception
	{
		boolean result 			 = false;
		String vplmAdminRoleName = MCADMxUtil.getActualNameForAEFData(context, "role_VPLMAdmin");

		if( MCADMxUtil.isRoleAssignedToUser(context, vplmAdminRoleName))
		{
			result 				 = MCADMxUtil.isGCOExistForRevision(context, "TEAM");
		}

		return  result;	
	}

	public Boolean isSolutionBasedEnvironment(Context context, String[] args) throws Exception
	{
		String solution = getSolution(context, args);

		String [] arg = new String[1];
		arg[0]			= solution;
		Boolean isSolutionBased	= isEnvironmentCentricSolution(context,arg);

		return isSolutionBased;
	}
}




