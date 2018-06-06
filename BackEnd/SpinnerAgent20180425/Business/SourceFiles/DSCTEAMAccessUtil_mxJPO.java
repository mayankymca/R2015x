/**
 * ${CLASSNAME}
 *
 *  Copyright Dassault Systemes, 1992-2011.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 * This JPO assigns integrations to given person and updates values
 * of attributes in the corresponding local config object for assigned
 * integrations.
 */

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.ResourceBundle;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.domain.util.PersonUtil;

public class  DSCTEAMAccessUtil_mxJPO
{
	protected IEFIntegAccessUtil util       		= null;

	protected String harnessUserRoleName			= "";
	protected String integrationAdminRoleName		= "";
	protected String exchangeUserRoleName			= "";
	protected String vplmViewerRoleName				= "";
	protected String vplmAdminRoleName				= "";

	protected static final String MXCATIAV4			= "MxCATIAV4";
	protected static final String MXCATIAV5			= "MxCATIAV5";

	public  DSCTEAMAccessUtil_mxJPO()
	{
	}

	public  DSCTEAMAccessUtil_mxJPO(Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("not supported no desktop client", null);

	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	protected void init (Context context, String[] args)throws Exception
	{
		HashMap paramMap			= (HashMap)JPO.unpackArgs(args);
		String language				= (String) paramMap.get("languageStr");

		this.util				 	= new IEFIntegAccessUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
		harnessUserRoleName			= MCADMxUtil.getActualNameForAEFData(context, "role_HarnessUser");
		integrationAdminRoleName	= MCADMxUtil.getActualNameForAEFData(context, "role_IEFAdmin");
		exchangeUserRoleName		= MCADMxUtil.getActualNameForAEFData(context, "role_ExchangeUser");
		vplmViewerRoleName		 	= MCADMxUtil.getActualNameForAEFData(context, "role_VPLMViewer");
		vplmAdminRoleName		 	= MCADMxUtil.getActualNameForAEFData(context, "role_VPLMAdmin");
	}

	public Boolean checkAccessForTEAMUser(Context context, String[] args) throws Exception
	{
		boolean result	= false;

		init(context, args);

		if(isTEAMSolution(context, args) )
		{
			if(MCADMxUtil.isRoleAssignedToUser(context, harnessUserRoleName)) 
				result	= true;
		}
		else if(PersonUtil.hasAssignment(context, harnessUserRoleName))			
			result 	= true;

		return new Boolean(result);
	}
	
	public Boolean checkSearchAccessForTEAMUser(Context context, String[] args) throws Exception
	{
		boolean result	= false;

		init(context, args);

		if(isTEAMSolution(context, args) )
		{
			if(MCADMxUtil.isRoleAssignedToUser(context, harnessUserRoleName) || MCADMxUtil.isRoleAssignedToUser(context, vplmAdminRoleName))
				result	= true;
		}
		else if(PersonUtil.hasAssignment(context, harnessUserRoleName))			
			result 	= true;

		return new Boolean(result);
	}

	public Boolean checkAccessForTEAMLeader(Context context, String[] args) throws Exception
	{
		boolean result	= false;

		init(context, args);

		if(isTEAMSolution(context, args))
		{
			if(MCADMxUtil.isRoleAssignedToUser(context, integrationAdminRoleName))
				result	= true;			
		}
		else if(PersonUtil.hasAssignment(context, integrationAdminRoleName))			
		{
			result 	= true;
		}

		return new Boolean(result);
	}
	
	public Boolean checkAccessForVPLMAdmin(Context context, String[] args) throws Exception
	{
		boolean result	= false;

		init(context, args);

		if(isTEAMSolution(context, args))
		{
			if(MCADMxUtil.isRoleAssignedToUser(context, vplmAdminRoleName))
				result	= true;
		}
		else if(PersonUtil.hasAssignment(context, integrationAdminRoleName))			
		{
			result 		= true;
		}

		return new Boolean(result);
	}
public Boolean checkAccessForVPLMAdminAcrossAllProjects(Context context, String[] args) throws Exception
	{
		boolean result	= false;

		init(context, args);

		if(isTEAMSolution(context, args))
		{
			if(MCADMxUtil.isRoleAssignedToUser(context, vplmAdminRoleName))
				result	= true;
			else if(MCADMxUtil.isRoleAssignedToUserAcrossAllProjects(context, vplmAdminRoleName))
				result	= true;	
		}
		else if(PersonUtil.hasAssignment(context, integrationAdminRoleName))			
		{
			result 		= true;
		}

		return new Boolean(result);
	}
	public Boolean checkStartDesignAccessforTEAM(Context context, String[] args) throws Exception
	{
		boolean result				= false;
		init(context, args);

		if(isTEAMSolution(context, args))
		{
			if(MCADMxUtil.isRoleAssignedToUser(context, harnessUserRoleName))
				result = isUserNotVPLMViewer(context,vplmViewerRoleName);
		}
		else if(PersonUtil.hasAssignment(context, harnessUserRoleName))			
			result = true;

		if(result)
		{
			String supportCreateCADStructureForFamily = "false";
			try
			{
				ResourceBundle mcadIntegrationBundle = ResourceBundle.getBundle("ief");
	            supportCreateCADStructureForFamily = mcadIntegrationBundle.getString("mcadIntegration.SupportFamilyInStartDesign");	
			}
			catch (Exception e)
			{
				supportCreateCADStructureForFamily = "false";
			}
			if(supportCreateCADStructureForFamily.equalsIgnoreCase("true"))
			{
				result = true;
			}
			else
			{			
			result = IEFActionAccess_mxJPO.isNonSLWIntegPresent(context, args);
			}
		}

		return new Boolean(result);
	}

	public Boolean checkCreateDesignAccess_WSFolderforTEAM(Context context, String[] args) throws Exception
	{
		boolean result = false;	
		init(context, args);

		if(isTEAMSolution(context, args))
		{
			if(MCADMxUtil.isRoleAssignedToUser(context, harnessUserRoleName) && isCatiaUser(context))				
				result  = isUserNotVPLMViewer(context, vplmViewerRoleName);
		}
		else if( isCatiaUser(context) && PersonUtil.hasAssignment(context, harnessUserRoleName))			
			result = true;

		return new Boolean(result);
	}

	public Boolean isCatiaAssignedToTEAM(Context context, String[] args) throws Exception
	{
		boolean result	= false;

		init(context, args);

		if(isTEAMSolution(context, args))
		{
			if(MCADMxUtil.isRoleAssignedToUser(context, harnessUserRoleName) && isCatiaUser(context))
				result	= true;
		}
		else if( isCatiaUser(context) && PersonUtil.hasAssignment(context, harnessUserRoleName))			
			result 	= true;

		return new Boolean(result);
	}

	public Boolean checkExcangeUserRoleForTEAM(Context context, String[] args) throws Exception 
	{
		boolean result 	= false;

		init(context, args);

		if(isTEAMSolution(context, args))
		{
			if(MCADMxUtil.isRoleAssignedToUser(context, harnessUserRoleName))
				result	= true;
		}
		else if(PersonUtil.hasAssignment(context, exchangeUserRoleName))		
			result 	= true;

		return  new Boolean(result);
	}

	public Boolean checkMeetingAccessforTEAM(Context context, String[] args) throws Exception 
	{
		boolean result 	= false;

		init(context, args);

		if(isTEAMSolution(context, args))
		{
			if(MCADMxUtil.isRoleAssignedToUser(context, harnessUserRoleName))
				result	= true;
		}
		else
			result 		= true;

		return  new Boolean(result);
	}

	private boolean isCatiaUser(Context context) throws Exception 
	{
		boolean isCatiaUser 	= false;
		Vector integrationList	= util.getAssignedIntegrations(context);

		if(integrationList.contains(MXCATIAV5) || integrationList.contains(MXCATIAV4))
			isCatiaUser 		= true;

		return isCatiaUser;
	}

	private boolean isUserNotVPLMViewer(Context context, String inputRole) throws Exception 
	{
		boolean retResult				= false; 

		String role	= context.getRole();
		if(null != role && !"".equals(role))
		{
			String loginContextRoleName = "";
			StringTokenizer roleTokens  = new StringTokenizer(role, ".");				
			String roleString		 	= roleTokens.nextToken();

			if(roleString.startsWith("ctx::"))
				loginContextRoleName	= roleString.substring(5);

			if(!"".equals(loginContextRoleName) && !loginContextRoleName.equals(inputRole))
				retResult	= true;
		}

		return retResult ;
	}

	private boolean isTEAMSolution(Context context, String[] args) throws Exception
	{
		IEFSecurityContextUtil_mxJPO seceurityContext = new IEFSecurityContextUtil_mxJPO(context, args); 
		return 	seceurityContext.isSolutionBasedEnvironment(context, args);		
	}
}

