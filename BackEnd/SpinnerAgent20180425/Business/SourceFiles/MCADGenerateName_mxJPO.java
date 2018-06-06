/*
**  MCADGenerateName
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to generate a unique name
*/
import matrix.db.Context;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;

public class MCADGenerateName_mxJPO
{
    /** Resource bundle **/
    MCADServerResourceBundle serverResourceBundle = null;

    public  MCADGenerateName_mxJPO  () {

    }
    public MCADGenerateName_mxJPO (Context context, String[] args) throws Exception
    {
		if (!context.isConnected())
			MCADServerException.createException("not supported no desktop client", null);
    }
    public int mxMain(Context context, String []args)  throws Exception
    {
		return 0;
    }

    private void intializeClassMembers(Context context, String sLanguage)  throws Exception
    {
		serverResourceBundle = new MCADServerResourceBundle(sLanguage);
    }

    // entry point
	public String runGenerateName(Context context, String[] args)  throws Exception
	{
		String instanceName = args[0];
		String familyName = args[1];
		String sLanguage = args[2];

		//initialize the member variables of this class
		//which may be used anywhere later
		intializeClassMembers(context, sLanguage);

		String retStr = familyName + "-" + instanceName;

		return retStr;
	}
}
