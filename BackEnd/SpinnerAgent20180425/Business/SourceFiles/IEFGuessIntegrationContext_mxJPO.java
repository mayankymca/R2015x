/*
**  IEFGuessIntegrationContext
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to determine integration context
*/
import java.util.Hashtable;
import java.util.StringTokenizer;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.apps.domain.util.PropertyUtil;

public class IEFGuessIntegrationContext_mxJPO
{

	public  IEFGuessIntegrationContext_mxJPO  ()
    {

    }
    
	public IEFGuessIntegrationContext_mxJPO (Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            MCADServerException.createException("not supported no desktop client", null);
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }
    
	public Hashtable getIntegrationNameForBusIds(Context context, String[] args)
    {
		Hashtable busIdIntegrationNameTable = new Hashtable(args.length);

		String SELECT_SOURCE_ATTR			= "attribute[" + (String)PropertyUtil.getSchemaProperty(context, "attribute_Source") + "]";
		StringList busSelects				= new StringList(1);

		busSelects.addElement("id");
		busSelects.addElement(SELECT_SOURCE_ATTR);
		
		try
		{
			BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, args, busSelects);

			for(int i = 0; i < busWithSelectList.size(); i++)
			{
				BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(i);
				
				String busID			 = busWithSelect.getSelectData("id");
				String integrationSource = busWithSelect.getSelectData(SELECT_SOURCE_ATTR);

				StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");
				
				String integrationName = "";

				if(integrationSourceTokens.hasMoreTokens())
					integrationName = integrationSourceTokens.nextToken();
				       busIdIntegrationNameTable.put(args[i], integrationName);
			}
		}
		catch(Exception ex)
		{
		}

		return busIdIntegrationNameTable;
	}

	public String getIntegrationName(Context context, String[] args)
    {
		String busID						= args[0];

		Hashtable busIdIntegrationNameTable = getIntegrationNameForBusIds(context, args);
		
		String integrationName = (String) busIdIntegrationNameTable.get(busID);

		return integrationName;
	}
}
