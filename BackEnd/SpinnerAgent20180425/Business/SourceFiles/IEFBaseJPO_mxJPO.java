/*
**  IEFBaseJPO
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Class defining basic infrastructure, contains common data members required
**  for executing any IEF related actions.
*/

import java.util.Hashtable;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

abstract public class IEFBaseJPO_mxJPO
{
    protected MCADGlobalConfigObject _globalConfig = null;
    protected MCADServerResourceBundle _serverResourceBundle = null;
	protected IEFGlobalCache _cache					= null;

    /** Util class*/
    protected MCADMxUtil _util                     = null;
    protected MCADServerGeneralUtil _generalUtil   = null;

	// Parameters table sent by the Caller
	Hashtable _argumentsTable = null;

    // Businessobject on which the operation is invoked
    protected String _busObjectID       = "";
	protected BusinessObject _busObject = null;

    public  IEFBaseJPO_mxJPO  () {

    }
    public IEFBaseJPO_mxJPO (Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            MCADServerException.createException("not supported no desktop client", null);

    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }

    /**
	 * This method initializes all the class members useful in the JPO operations
     */
    public void initialize(Context context,String[] args) throws MCADException
    {
        try
        {
    		Hashtable argsTable = (java.util.Hashtable) JPO.unpackArgs(args);
	        initialize( context, argsTable);
        }
		catch(Exception e)
        {
			System.out.println("[initialize]: Exception while initializating JPO" + e.getMessage());
            MCADServerException.createException(e.getMessage(), e);
        }
    }

    public void initialize(Context context, Hashtable argsTable) throws MCADException
    {
        try
        {
			_argumentsTable = argsTable;
			_globalConfig = (MCADGlobalConfigObject) argsTable.get(MCADServerSettings.GCO_OBJECT);
			String languageName		= (String)argsTable.get(MCADServerSettings.LANGUAGE_NAME);
			_serverResourceBundle	= new MCADServerResourceBundle(languageName);
			_cache					= new IEFGlobalCache();
			_busObjectID = (String)argsTable.get(MCADServerSettings.OBJECT_ID);
			_busObject   = new BusinessObject(_busObjectID);

			_util           = new MCADMxUtil(context, _serverResourceBundle, _cache);
	        _generalUtil    = new MCADServerGeneralUtil(context,_globalConfig, _serverResourceBundle, _cache);
        }
		catch(Exception e)
        {
			System.out.println("[initialize]: Exception while initializating JPO" + e.getMessage());
            MCADServerException.createException(e.getMessage(), e);
        }
    }

}
