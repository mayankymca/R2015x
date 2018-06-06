/*
**  DSCMessageDeleteAction
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to use as trigger invoke the post process jpo's execute method
*/

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MatrixWriter;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class DSCMessageDeleteAction_mxJPO
{
	MatrixWriter	_mxWriter	= null;

	/**
	 * The no-argument constructor.
	 */
	public DSCMessageDeleteAction_mxJPO()
	{

	}

	/**
	 * Constructor which accepts the Matrix context and an array of String
	 * arguments.
	 */
	public DSCMessageDeleteAction_mxJPO (Context context, String[] args) throws Exception
    {
        _mxWriter	= new MatrixWriter(context);
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		int returnValue = 0;

		String []constArgs	= new String[1];
		
		MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle("");
		MCADMxUtil mxUtil								= new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());	

		try
		{
			String Args[] = new String[2];
			Args[0] = "global";
			Args[1] = "DSC_MESSAGE_DELETE_JPO";
			String postProcessJPOAttr		= mxUtil.executeMQL(context, "get env $1 $2",Args);
			Args[1] = "DSC_MESSAGE_BODY";
			String messageBody				= mxUtil.executeMQL(context, "get env $1 $2",Args);
			String messageBodyAttrValue		= "";
			String postProcessJPOAttrValue	= "";

			if(messageBody.startsWith("true|"))
			{
				messageBodyAttrValue = messageBody.substring(messageBody.indexOf("|")+1, messageBody.length());
				
				if(!messageBodyAttrValue.trim().equals(""))
					constArgs[0] = messageBodyAttrValue;
			}

			if(postProcessJPOAttr.startsWith("true|"))
			{
				postProcessJPOAttrValue = postProcessJPOAttr.substring(postProcessJPOAttr.indexOf("|")+1, postProcessJPOAttr.length());
				
				if(!postProcessJPOAttrValue.equals(""))
				{
					String sResult	= (String)JPO.invoke(context,postProcessJPOAttrValue,constArgs,"execute",args,String.class);
					if(sResult.startsWith("false"))
					{
						//TODO : I18N
					   MCADServerException.createException("Failed while post processing Message : " + sResult.substring(sResult.indexOf("|")+1, sResult.length()), null);
					   returnValue = -1;
					}
				}
			}
		}
        
		catch(Exception me)
        {
	    	_mxWriter.write("Error occurred:" + me.getMessage());
			MCADServerException.createException(me.getMessage(), me);

			returnValue = -1;
		}

		finally
		{
			String Args[] = new String[2];
			Args[0] = "global";
			Args[1] = "DSC_MESSAGE_DELETE_JPO";
			mxUtil.executeMQL(context, "unset env $1 $2",Args);
			Args[1] = "DSC_MESSAGE_BODY";
			mxUtil.executeMQL(context, "unset env $1 $2",Args);
		}

		return returnValue;
	}
}
