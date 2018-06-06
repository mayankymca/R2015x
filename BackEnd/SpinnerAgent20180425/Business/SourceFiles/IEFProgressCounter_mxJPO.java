/*
**  IEFProgressCounter
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to get progress counter
*/

import matrix.db.Context;

import com.matrixone.MCADIntegration.server.beans.IEFProgressCounter;

public class IEFProgressCounter_mxJPO 
{
    public IEFProgressCounter_mxJPO ()
    {
    }

    public IEFProgressCounter_mxJPO (Context context, String[] args) throws Exception
    {
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }

    public String getProgressCounter(Context context, String[] args) throws Exception
    {
        String operationUID		= args[0];

		String progressCounter	= (String)IEFProgressCounter.getCounter(operationUID);

		return progressCounter;
    }

	public String setOperationCancelled(Context context, String[] args) throws Exception
    {
		String status			= "true";
        String operationUID		= args[0];

		IEFProgressCounter.setCancelStatus(operationUID);

		return status;
    }
}
