/*
**  DSCBackgroundDerivedOutputPostDelete
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
** Program to unlock the locked objects for backgroundprocess
*/

import matrix.db.Context;

public class DSCBackgroundDerivedOutputPostDelete_mxJPO
{

	String messageBodyAttrValue	= "";

	public DSCBackgroundDerivedOutputPostDelete_mxJPO (Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            throw new Exception("not supported no desktop client");
		messageBodyAttrValue = args[0].trim();
    }

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}
		
	/**
	*
	*/
	public String execute(Context context, String[] args) throws Exception
	{
		String[] jpoArgs	= new String[1];
		jpoArgs[0]			= messageBodyAttrValue;

		DSCBackgroundDerivedOutputPostProcess_mxJPO postProcessJPO	= new DSCBackgroundDerivedOutputPostProcess_mxJPO(context, jpoArgs);
		
		String sResult	= postProcessJPO.execute(context, args);
		
		return sResult;
	}
}
