/*
**  IEFCheckBulkLoaderRoleAccess
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
import matrix.db.MatrixWriter;

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;

public class IEFCheckBulkLoaderRoleAccess_mxJPO
{
	String _sObjId;
	MatrixWriter _mxWriter = null;

	/**
	 * The no-argument constructor.
	 */
	public IEFCheckBulkLoaderRoleAccess_mxJPO()
	{
	}

	/**
	 * Constructor which accepts the Matrix context and an array of String
	 * arguments.
	 */
	public IEFCheckBulkLoaderRoleAccess_mxJPO(Context context, String[] args) throws Exception
	{
		_mxWriter = new MatrixWriter(context);
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		String roleBulkLoader = MCADMxUtil.getActualNameForAEFData(context, "role_IEFBulkLoader");

		if (context.isAssigned(roleBulkLoader) == true)
		{
			System.out.println("The present user has role" +  roleBulkLoader + " assigned\n");
			return 0;
		}
		else
		{
			System.out.println("The present user does not have role" +roleBulkLoader + " assigned\n");
			_mxWriter.write("The present user does not have role" + roleBulkLoader + " assigned\n");
			return 1;
		}

	}

}

