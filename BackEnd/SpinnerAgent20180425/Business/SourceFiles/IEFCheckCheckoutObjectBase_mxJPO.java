/*
**  IEFCheckCheckoutObjectBase
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to use as trigger to modify event on attribute IEF-UserBulkLoading
*/

import matrix.db.Context;

public class IEFCheckCheckoutObjectBase_mxJPO
{
	public IEFCheckCheckoutObjectBase_mxJPO(Context context, String[] args) throws Exception
	{
	}

	public String checkCheckoutObject(Context context, String[] args) throws Exception
	{
	//System.out.println("--------------------------IEFCheckCheckoutObject:checkObject--------------------");
             

			String returnValue = getReturnValue(context, args);
			return  returnValue;
	}

	protected String getReturnValue(Context context, String[] args) 
	{
			 String name			= args[0];
             String type			= args[1];
             String rev			    = args[2];
             String debug		    = args[3];
             String language		= args[4];
             String isSilentMode	= args[5];
             String relId			= args[6];
             String parentID        = args[7];

			//return "false|false|message for user" if selection is not allowed
			//return "true|false|message for user" if selection is allowed but lock is not allowed
			//return "true|true|message for user" if both selection and lock is allowed
			return  "true|true| "+ name + ",  " + type + ",  " + rev;
	}
}
