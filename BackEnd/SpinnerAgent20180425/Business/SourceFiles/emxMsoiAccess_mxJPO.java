import matrix.db.*;

import java.util.*;

import com.matrixone.apps.domain.util.*;

import com.matrixone.MCADIntegration.server.*;
import com.matrixone.MCADIntegration.server.beans.*;
import com.matrixone.MCADIntegration.utils.*;

public class emxMsoiAccess_mxJPO
{
    
    public  emxMsoiAccess_mxJPO ()
	{
    }

    public emxMsoiAccess_mxJPO (Context context, String[] args) throws Exception
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
	    boolean retVal = false;
	    try
	    {
		  Vector userRoleList                       = PersonUtil.getAssignments(context);
		  final String ROLE_MICROSOFTOFFICE_USER = PropertyUtil.getSchemaProperty(context, "role_MicrosoftOfficeUser");
		  
		  //Changes SJ7+ 22-Aug-2012 Changes for R.A.C.E Environment.
		  //Changes SJ7+ 10/June/2014 Changes for Unified Login ... Removal of Team Role Check		  
		  final String ROLE_VPLMViewer = PropertyUtil.getSchemaProperty(context, "role_VPLMViewer");
		  
		  if(PersonUtil.hasAssignment(context, ROLE_MICROSOFTOFFICE_USER)) 
			 retVal = true;
		  else
			 retVal = false;
		}
		catch(Exception ex)
		{
			retVal = false;
			throw ex;
		}
		 return retVal;
	}

	public boolean isMSProjectIntegrationUser(Context context,String []args) throws Exception
	{
		Vector userRoleList 		= PersonUtil.getAssignments(context);
		final String ROLE_MICROSOFTPROJECT_USER = PropertyUtil.getSchemaProperty(context, "role_MicrosoftProjectUser");
		return userRoleList.contains(ROLE_MICROSOFTPROJECT_USER);
	}
	
	public boolean isMSOfficeOrProjectIntegrationUser(Context context,String []args) throws Exception
	{
		return isMSOfficeIntegrationUser(context, args) || isMSProjectIntegrationUser(context, args);
	}
}
