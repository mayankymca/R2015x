/**
 ** DECSignUpdateOnBusApproveActionTrigger.java
 **
 **  Copyright Dassault Systemes, 1992-2010.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such programimport java.util.*;
 **
 **  This trigger program updates Approver and Approved Date attributes on bus when signature is done
 **
 **/

import java.util.Vector;
import java.util.ArrayList;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.utils.MCADStringUtils;

import com.matrixone.apps.domain.util.PropertyUtil;

public class DECSignUpdateOnBusApproveActionTrigger_mxJPO
{
	/**
	 * The no-argument constructor.
	 */
    public  DECSignUpdateOnBusApproveActionTrigger_mxJPO  ()
    {
    }

    public DECSignUpdateOnBusApproveActionTrigger_mxJPO (Context context, String[] args) throws Exception
    {   
		if (!context.isConnected())
        {
            MCADServerException.createException("not supported no desktop client", null);
        }
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }

    public int updateApproverAttrib(Context context, String []args)  throws Exception
	{
		String result							= "";

		String actualName_Approver		= (String)PropertyUtil.getSchemaProperty(context, "attribute_Approver");
		String actualName_ApprovedDate	= (String)PropertyUtil.getSchemaProperty(context, "attribute_ApprovedDate");

		MCADServerResourceBundle serverResourceBundle 	= new MCADServerResourceBundle("");
		MCADMxUtil util									= new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());
		

		String objectId		= (String)args[0];
		String signName		= (String)args[1];
		String signer		= (String)args[2];
		String ISAPPROVED	= (String)args[3];
		String timeStamp	= (String)args[4];
		String objectType	= (String)args[5];

		int returnValue = 0;

		Vector busIds = new Vector(2);
		busIds.add(objectId + "-" + objectType);

		try
		{
			String activeMinorId = util.getActiveVersionObject(context, objectId);
			if(null != activeMinorId && !"".equals(activeMinorId))
			{				
				String SELECT_TYPE   = "type";

				String [] oids  = new String [1];
				oids[0]         = activeMinorId;

				StringList selectStmts = new StringList(1);
				selectStmts.add(SELECT_TYPE);

				BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, oids, selectStmts);

				BusinessObjectWithSelect busWithSelect  = (BusinessObjectWithSelect) busWithSelectList.elementAt(0);

				String type	  = busWithSelect.getSelectData(SELECT_TYPE);

				busIds.add(activeMinorId + "-" + type);
			}
		}
		catch (Exception e)
		{
			//No active minor found.
		}

		try
		{	
			for(int i = 0; i < busIds.size(); i++)
			{
				String busIdTypeName = (String)busIds.elementAt(i);
				
				String results[] = MCADStringUtils.split(busIdTypeName, "-");

				String busId     = results[0];
				String busType   = results[1];			

				if(null != ISAPPROVED && ISAPPROVED.equalsIgnoreCase("true"))
				{
					BusinessObject bus = new BusinessObject(busId);

					Vector allAttributesOnType = util.getAllAttributeNamesOnType(context, busType);
					if(allAttributesOnType.contains(actualName_Approver))
					{
						util.setAttributeValue(context, bus, actualName_Approver, signer);
					}

					if(allAttributesOnType.contains(actualName_ApprovedDate))
					{
						util.setAttributeValue(context, bus, actualName_ApprovedDate, timeStamp);
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Error while updating attributes on business object : " + e.getMessage());
		}

		return returnValue;
	}

}
