/*
**  DSCMessagePostProcess
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to set Global RPEs for message body and JPOName to be executed
*/

import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MatrixWriter;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class DSCMessagePostProcess_mxJPO
{
	MatrixWriter	_mxWriter	= null;
	String			_sObjectID	= null;
	
	/**
	 * The no-argument constructor.
	 */
	public DSCMessagePostProcess_mxJPO()
	{

	}

	/**
	 * Constructor which accepts the Matrix context and an array of String
	 * arguments.
	 */
	public DSCMessagePostProcess_mxJPO(Context context, String[] args) throws Exception
	{
		_mxWriter	= new MatrixWriter(context);
		_sObjectID	= args[0];
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		int returnValue		= 0;
		
		String []constArgs	= new String[1];
		constArgs[0]		= "";
		
		MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle("");
		MCADMxUtil util									= new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());

		String	jpoAttrName = util.getSymbolicName(context, "attribute", args[0].trim());

		BusinessObject busObject = new BusinessObject(_sObjectID);
		busObject.open(context);

		String postProcessJPOAttrName		= MCADMxUtil.getActualNameForAEFData(context, jpoAttrName);
		String messageCompletionStatusName  = MCADMxUtil.getActualNameForAEFData(context, "attribute_CompletionStatus");
		String messageBodyAttrName			= MCADMxUtil.getActualNameForAEFData(context, "attribute_DSCMessageBody");
		
		StringList attribNames				= new StringList();

		attribNames.addElement(postProcessJPOAttrName);
		attribNames.addElement(messageCompletionStatusName);
		attribNames.addElement(messageBodyAttrName);

		AttributeList attrList = busObject.getAttributeValues(context, attribNames);
		AttributeItr attrItr   = new AttributeItr(attrList);
		
		String messageCompletionStatus = "";
		String postProcessJPOAttrValue = "";

		while (attrItr.next())
		{
			Attribute attribute = attrItr.obj();
			String attriName	= attribute.getName().trim();

			if(attriName.equals(postProcessJPOAttrName))
				postProcessJPOAttrValue = attribute.getValue().trim();
			else if(attriName.equals(messageCompletionStatusName))
				messageCompletionStatus = attribute.getValue().trim();
			else if(attriName.equals(messageBodyAttrName))
				constArgs[0] = attribute.getValue().trim();
		}

		busObject.close(context);

		try
		{
			if(!postProcessJPOAttrValue.equals(""))
			{
				if(messageCompletionStatus.equalsIgnoreCase("Succeeded"))
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

		return returnValue;
	}

	public int executePostDeleteJPO(Context context, String []args)  throws Exception
	{
		int returnValue				= 0;
		boolean isJPONameSet		= false;
		boolean isMessageBodySet	= false;
		

		MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle("");
		MCADMxUtil util									= new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());

		String	jpoAttrName = util.getSymbolicName(context, "attribute", args[0].trim());

		BusinessObject busObject = new BusinessObject(_sObjectID);
		busObject.open(context);

		String postProcessJPOAttrName		= MCADMxUtil.getActualNameForAEFData(context, jpoAttrName);
		String messageCompletionStatusName  = MCADMxUtil.getActualNameForAEFData(context, "attribute_CompletionStatus");
		String messageBodyAttrName			= MCADMxUtil.getActualNameForAEFData(context, "attribute_DSCMessageBody");

		StringList attribNames				= new StringList();

		attribNames.addElement(postProcessJPOAttrName);
		attribNames.addElement(messageCompletionStatusName);
		attribNames.addElement(messageBodyAttrName);

		AttributeList attrList = busObject.getAttributeValues(context, attribNames);
		AttributeItr attrItr   = new AttributeItr(attrList);
		
		String messageCompletionStatus	= "";
		String postProcessJPOAttrValue	= "";
		String messageBodyAttrValue		= "";

		while (attrItr.next())
		{
			Attribute attribute = attrItr.obj();
			String attriName	= attribute.getName().trim();

			if(attriName.equals(postProcessJPOAttrName))
				postProcessJPOAttrValue = attribute.getValue().trim();
			else if(attriName.equals(messageCompletionStatusName))
				messageCompletionStatus = attribute.getValue().trim();
			else if(attriName.equals(messageBodyAttrName))
				messageBodyAttrValue = attribute.getValue().trim();
		}

		busObject.close(context);

		try
		{
			if(!postProcessJPOAttrValue.equals(""))
			{
				String Args[] = new String[3];
				Args[0] = "global";
				Args[1] = "DSC_MESSAGE_DELETE_JPO";
				Args[2] = postProcessJPOAttrValue;
				String setJPOName = util.executeMQL(context, "set env $1 $2 $3",Args);
				if(setJPOName.startsWith("true|"))
					isJPONameSet = true;
				
				Args[1] = "DSC_MESSAGE_BODY";
				Args[2] = messageBodyAttrValue;
				String setMsgBody = util.executeMQL(context, "set env $1 $2 $3",Args);
				if(setMsgBody.startsWith("true|"))
					isMessageBodySet = true;
			}
		}
        catch(Exception me)
        {
	    	_mxWriter.write("Error occurred:" + me.getMessage());
			MCADServerException.createException(me.getMessage(), me);

			String Args[] = new String[2];
			Args[0] = "global";
			Args[1] = "DSC_MESSAGE_DELETE_JPO";
			if(isJPONameSet)
				util.executeMQL(context, "unset env $1 $2",Args);

			Args[1] = "DSC_MESSAGE_BODY";
			if(isMessageBodySet)
				util.executeMQL(context, "unset env $1 $2",Args);

			returnValue = -1;
		}

		return returnValue;
	}
}

