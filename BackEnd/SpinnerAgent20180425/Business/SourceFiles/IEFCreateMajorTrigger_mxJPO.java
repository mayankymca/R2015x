/*
**  IEFCreateMajorTrigger
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to use as trigger to create event on Major Objects
*/
import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.util.MatrixException;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class IEFCreateMajorTrigger_mxJPO
{

	MatrixWriter	_mxWriter	= null;
	String			_sObjectID	= null;

	/**
	 * The no-argument constructor.
	 */
	public IEFCreateMajorTrigger_mxJPO()
	{
	}

	/**
	 * Constructor which accepts the Matrix context and an array of String
	 * arguments.
	 */
	public IEFCreateMajorTrigger_mxJPO(Context context, String[] args) throws Exception
	{
		_mxWriter = new MatrixWriter(context);

		// Get the OBJECTID of the object in context
		_sObjectID = args[0];
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		 String attrMoveFileToVersion	= MCADMxUtil.getActualNameForAEFData(context, "attribute_MoveFilesToVersion");
		 String attrTitle				= MCADMxUtil.getActualNameForAEFData(context, "attribute_Title");

		try
		{
			MCADMxUtil util = new MCADMxUtil(context, null, new IEFGlobalCache());
			if(util.isCDMInstalled(context))
			{
				BusinessObject busObj = new BusinessObject(_sObjectID);
				busObj.open(context);
				String objName = busObj.getName();
				busObj.close(context);
				AttributeList attributelist = new AttributeList(1);
				attributelist.addElement(new Attribute(new AttributeType(attrMoveFileToVersion), "True"));
				attributelist.addElement(new Attribute(new AttributeType(attrTitle), objName));
				busObj.setAttributes(context, attributelist);
				busObj.update(context);
			}
		}
		catch(MatrixException me)
        {
	    	_mxWriter.write("Matrix Error occurred:" + me.getMessage());
			MCADServerException.createException(me.getMessage(), me);
		}
        catch(Exception me)
        {
	    	_mxWriter.write("Error occurred:" + me.getMessage());
			MCADServerException.createException(me.getMessage(), me);
		}
		return 0;
	}
}

