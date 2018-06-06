/*
**  IEFUnSignTrigger
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to use as trigger to unsign all signatures in the current state
*/
import matrix.db.Context;
import matrix.db.MatrixWriter;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class IEFUnSignTrigger_mxJPO
{
	MatrixWriter	_mxWriter	= null;
	String			_sObjectID	= null;

	/**
	 * The no-argument constructor.
	 */
	public IEFUnSignTrigger_mxJPO()
	{
	}

	/**
	 * Constructor wich accepts the Matrix context and an array of String
	 * arguments.
	 */
	public IEFUnSignTrigger_mxJPO(Context context, String[] args) throws Exception
	{
		_mxWriter = new MatrixWriter(context);

		// Get the OBJECTID of the object in context
		_sObjectID = args[0];
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	public int execute(Context context, String []args)  throws Exception
	{
        int returnValue =0;

		try
		{
			MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle("");
			MCADMxUtil util         = new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());
			String Args[] = new String[3];
			Args[0] = _sObjectID;
			Args[1] = "signature";
			Args[2] = "all";
	        String sResult      = util.executeMQL(context,"unsign bus $1 $2 $3", Args);
			if(sResult.startsWith("false"))
			{
				//TODO : I18N
			   MCADServerException.createException("Failed to unsign : "+ sResult.substring(sResult.indexOf("|")+1, sResult.length()), null);
				returnValue = -1;
			}

		}
        catch(Exception me)
        {
	    	_mxWriter.write("Error occurred:" + me.getMessage());
			MCADServerException.createException(me.getMessage(), me);
		}

		return returnValue;
	}
}
