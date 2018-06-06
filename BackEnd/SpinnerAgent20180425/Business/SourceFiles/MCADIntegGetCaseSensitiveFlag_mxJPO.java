/*
**  MCADIntegGetCaseSensitiveFlag
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to get the Casesensitive Flag.
*/
import matrix.db.Context;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
 
public class MCADIntegGetCaseSensitiveFlag_mxJPO {
 
	private MCADMxUtil _util					           = null;
	private MCADServerResourceBundle _serverResourceBundle = null;
	private IEFGlobalCache	_cache						   = null;

	public MCADIntegGetCaseSensitiveFlag_mxJPO()
	{
	}
 
	public MCADIntegGetCaseSensitiveFlag_mxJPO(Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("Not supported on desktop client!!!", null);
	}
 
	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}
 
 	private void init(Context context, String sLanguage)  throws Exception
	{
	 	 _serverResourceBundle = new MCADServerResourceBundle(sLanguage);
		 _cache				   = new IEFGlobalCache();
		 _util				   = new MCADMxUtil(context, _serverResourceBundle, _cache);
	}

	public Boolean getCaseSensitiveFlag(Context context, String[] args)
	{
		boolean isSystemCaseSensitive = true;

		try
		{
			String sLanguage = args[0];			
			init(context, sLanguage);

			String Args[] = new String[2];
			Args[0] = "system";
			Args[1] = "casesensitive";
	        String result		= _util.executeMQL(context, "print $1 $2", Args);
		    if(result.startsWith("true|"))
			{
				result		= result.substring(5);
	            int index	= result.indexOf("=");
		        if(index > -1)
			    {
				    String caseSensitiveFlag = result.substring(index + 1);
					if(caseSensitiveFlag.equalsIgnoreCase("Off"))
					{
						isSystemCaseSensitive = false;
					}
				}
			}
		}
		catch(Exception ex)
		{
			System.out.println("Error occured in MCADIntegGetCasesensitiveFlag:" + ex.getMessage());
		}

		return new Boolean(isSystemCaseSensitive);
	}
}
