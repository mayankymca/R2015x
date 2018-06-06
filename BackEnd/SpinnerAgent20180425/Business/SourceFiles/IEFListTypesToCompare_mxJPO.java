/**
 * IEFListTypesToCompare.java
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 * list all assembly types to compare
 */

import java.util.HashMap;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

public class IEFListTypesToCompare_mxJPO
{

	public IEFListTypesToCompare_mxJPO(Context context, String[] args) throws Exception
	{
	}

	public Object getList(Context context, String[] args) throws Exception
	{
		StringList objectTypesList = new StringList();

		HashMap paramMap = (HashMap)JPO.unpackArgs(args);
				
		try
		{		
			objectTypesList.addElement("MCAD Assembly,MCAD Versioned Assembly,MCAD Drawing,MCAD Versioned Drawing");
		}
		catch(Exception exception)
		{

		}

		return objectTypesList;
	}
}
