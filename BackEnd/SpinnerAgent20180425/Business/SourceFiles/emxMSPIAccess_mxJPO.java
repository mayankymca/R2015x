// emxMSPIAccess.java
//
// Copyright (c) 2005 MatrixOne, Inc.
// All Rights Reserved
// This program contains proprietary and trade secret information of
// MatrixOne, Inc.  Copyright notice is precautionary only and does
// not evidence any actual or intended publication of such program.
//
// Access program for MSPI commands

import matrix.db.Context;
import java.util.Vector;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.server.MCADServerException;

public class emxMSPIAccess_mxJPO
{
    final String MSOfficeIntegrationName = "MSOffice";
    final String MSProjectIntegrationName ="MSProject";
    public  emxMSPIAccess_mxJPO()
	{
    }

    public emxMSPIAccess_mxJPO(Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
		{
            MCADServerException.createException("not supported no desktop client", null);
		}
    }

    public int mxMain(Context context, String []args)  throws Exception
    {  
        return 0;
    }

    public boolean isMSOfficeIntegrationUser(Context context,String []args) throws Exception

    {
        //for PLM Express support begin ODW: 01-08-11
    	/*Vector userRoleList = PersonUtil.getAssignments(context);
        final String ROLE_MICROSOFTOFFICE_USER = PropertyUtil.getSchemaProperty(context, "role_MicrosoftOfficeUser");
    	if(ROLE_MICROSOFTOFFICE_USER == null || "".equals(ROLE_MICROSOFTOFFICE_USER)){
    		return false;
    	}
        return userRoleList.contains(ROLE_MICROSOFTOFFICE_USER);*/
        
		IEFIntegAccessUtil mxUtil = new IEFIntegAccessUtil(context, null,new IEFGlobalCache());
    	Vector assignedIntegrations = mxUtil.getAssignedIntegrations(context);
		return assignedIntegrations.contains(MSOfficeIntegrationName);
		//for PLM Express support end ODW: 01-08-11
    }
    
    
    public boolean isMSProjectIntegrationUser(Context context,String []args) throws Exception
    {
		//for PLM Express support begin ODW: 01-08-11
    	/*Vector userRoleList = PersonUtil.getAssignments(context);
    	final String ROLE_MICROSOFTPROJECT_USER = PropertyUtil.getSchemaProperty(context, "role_MicrosoftProjectUser");
    	if(ROLE_MICROSOFTPROJECT_USER == null || "".equals(ROLE_MICROSOFTPROJECT_USER)){
    		return false;
    	}
    	return userRoleList.contains(ROLE_MICROSOFTPROJECT_USER);*/
        IEFIntegAccessUtil mxUtil = new IEFIntegAccessUtil(context, null,new IEFGlobalCache());
    	Vector assignedIntegrations = mxUtil.getAssignedIntegrations(context);
		return assignedIntegrations.contains(MSProjectIntegrationName);
		//for PLM Express support end ODW: 01-08-11
    }


//    public boolean isMSProjectIntegrationUser(Context context,String []args) throws Exception
//	{
//		Vector userRoleList 		= PersonUtil.getAssignments(context);
//		//return userRoleList.contains("MicrosoftProject User");
//		//Added:nr2:MPI:20-Mar-2010
//		//For removing the non-availability of project in MSF if MPI is installed over MSF
//		return (userRoleList.contains("MicrosoftOffice User")||userRoleList.contains("MicrosoftProject User"));
//	}
    
    public boolean isMSOfficeOrProjectIntegrationUser(Context context,String []args) throws Exception
    {
    	return isMSOfficeIntegrationUser(context, args) || isMSProjectIntegrationUser(context, args);
    }
}
