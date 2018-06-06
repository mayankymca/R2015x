/*
**  IEFCommonUIActions
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
*/
import java.util.Hashtable;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFProgressCounter;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

abstract public class IEFCommonUIActions_mxJPO extends IEFBaseJPO_mxJPO
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

	//Operation UID
	protected String _operationUID		= "";

    public  IEFCommonUIActions_mxJPO  () {

    }
    public IEFCommonUIActions_mxJPO (Context context, String[] args) throws Exception
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
            _argumentsTable = (java.util.Hashtable) JPO.unpackArgs(args);

            _globalConfig   = (MCADGlobalConfigObject)_argumentsTable.get(MCADServerSettings.GCO_OBJECT);


			_operationUID = (String)_argumentsTable.get(MCADServerSettings.OPERATION_UID);
			if(_operationUID != null)
				IEFProgressCounter.addCounter(_operationUID, 0);

			String languageName  = (String)_argumentsTable.get(MCADServerSettings.LANGUAGE_NAME);
			_serverResourceBundle = new MCADServerResourceBundle(languageName);
			_cache				  =	new IEFGlobalCache();

			_busObjectID = (String)_argumentsTable.get(MCADServerSettings.OBJECT_ID);
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

	/**
	 * Common Entry Point to JPO
	 * This method decided if a perticular link can be displayed
	 * in the dashboard UI, based on whether the opration can be done on the
	 * selected businessobject
	 * After initialization, the controll is given to the derived class
	 * for checkin if operation can be performed or not
	 * Default implementation is to allow the operation
     */
	public Hashtable canShowButton(Context context,String[] args) throws MCADException
    {
		Hashtable resultDataTable = new Hashtable();
		try
		{
			// Get all standard initialization,
			// including creation of GCO, logger, resource bundle etc.
			initialize(context,args);
			canPerformOperation(context, resultDataTable);
		}
        catch(Exception e)
        {
			// Do not throw any exception back to JPO
			// For Any error/exception, send proper message back to the caller
			// using the resultDataTable
			String error = e.getMessage();
			e.printStackTrace();
			resultDataTable.put(MCADServerSettings.JPO_EXECUTION_STATUS,"false");
			resultDataTable.put(MCADServerSettings.JPO_STATUS_MESSAGE,error);
        }
		return resultDataTable;
	}

	/**
	 * Common Entry Point to JPO
	 * After initialization, the control is given to the derived class
	 * for executing custom logic
	 * A Pre-execution-check is also made, whether the operation can be performed
	 * The logic for this pre-execution check can be implemented in the
	 * Derived class. Default implementation is to allow the operation
     */
	public Hashtable execute(Context context,String[] args) throws MCADException
    {
		Hashtable resultDataTable = new Hashtable();
		try
		{
			// Get all standard initialization,
			// including creation of GCO, logger, resource bundle etc.
			initialize(context,args);
			canPerformOperation(context, resultDataTable);
			String result = (String)resultDataTable.get(MCADServerSettings.JPO_EXECUTION_STATUS);
			if (result.equalsIgnoreCase("true"))
			{
				executeCustom(context, resultDataTable);
			}
			else
			{
				//System.out.println("[MCAD-BaseJPO.execute] : Exit out of JPO, as the operation cannot be performed");
				//System.out.println("resultDataTable.toString()");
			}
		}
        catch(Exception e)
        {
			// Do not throw any exception back to JPO
			// For Any error/exception, send proper message back to the caller
			// using the resultDataTable
			String error = e.getMessage();
			e.printStackTrace();
			System.out.println("[MCAD-BaseJPO.execute] :Exception occured" + error);
			resultDataTable.put(MCADServerSettings.JPO_EXECUTION_STATUS,"false");
			resultDataTable.put(MCADServerSettings.JPO_STATUS_MESSAGE,error);
        }
		finally
		{
			IEFProgressCounter.removeCounter(_operationUID);
			IEFProgressCounter.removeCancelStatus(_operationUID);
		}

		return resultDataTable;
	}

    /**
	 * All derived Classes to impelment this method
	 * All return parameters like files list for file operations etc. to be
	 * returned using Hashtable "resultDataTable". For this hastable,
	 * use following String as key
	 * MCADServerSettings.JPO_EXECUTION_RESULT
	 * for any error condition encountered, the method must throw an MCADException
	 */
	abstract protected void executeCustom(Context _context, Hashtable resultDataTable) throws MCADException;

    //
	private void canPerformOperation(Context _context, Hashtable resultDataTable) throws MCADException
	{
		try
		{
			canPerformOperationCustom(_context, resultDataTable);
			// Operation can be performed,
			// if return successfully from the custom implementation.
			resultDataTable.put(MCADServerSettings.JPO_EXECUTION_STATUS,"true");
		}
		catch(Exception e)
        {
			resultDataTable.put(MCADServerSettings.JPO_EXECUTION_STATUS,"false");
			resultDataTable.put(MCADServerSettings.JPO_STATUS_MESSAGE,e.getMessage());
        }
	}

    /**
	 * Checks if the operation can be performed on the selected BusinessObject.
	 * All derived Classes to impelment this method, if required.
	 * for any error condition encountered, or for a valid reason for which
	 * the operation cannot be performed (any custom business logic)
	 * the method must throw an MCADException
	 * Default implementation - do nothing, i.e. operation is allowed
	 */
	protected void canPerformOperationCustom(Context _context, Hashtable resultDataTable) throws MCADException
	{
	}

	protected void incrementMetaCurrentCount()
    {
		IEFProgressCounter.incrementCounter(_operationUID);
    }

	protected boolean isOperationCancelled()
    {
		boolean isOperationCancelled = false;

		String cancelStatus = IEFProgressCounter.getCancelStatus(_operationUID);
		if("true".equalsIgnoreCase(cancelStatus))
			isOperationCancelled = true;

		return isOperationCancelled;
    }
}
