/*
**  IEF-GetAutoNameSeries
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to determine integration context
*/
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Query;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.domain.util.PropertyUtil;

public class IEFGetAutoNameSeries_mxJPO
{

	public  IEFGetAutoNameSeries_mxJPO  ()
    {

    }
    
	public IEFGetAutoNameSeries_mxJPO (Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            MCADServerException.createException("not supported no desktop client", null);
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }
    
	public Hashtable getTypeNameAutoNameSeriesTable(Context context, String[] args) throws Exception
    {			
		String[] typesList			= new String[2];
		typesList[0]				= args[0];
		typesList[1]				= args[1];
		Vector typeNameList			= (Vector)JPO.unpackArgs(typesList);
		String language				= args[3];
		IEFGlobalCache cache							= new IEFGlobalCache();
		MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle(language);
		MCADMxUtil util									= new MCADMxUtil(context, serverResourceBundle, cache);
		String objectGeneratorType	= args[2];

		Hashtable autoNameSeriesTable = new Hashtable();

		for(int i=0;i< typeNameList.size();++i)
		{
			String typeName = (String)typeNameList.elementAt(i);

			Query queryObjType = new Query("");
			queryObjType.open(context);
			queryObjType.setBusinessObjectType(objectGeneratorType);
			queryObjType.setBusinessObjectName(typeName);
			queryObjType.setBusinessObjectRevision("*");
			queryObjType.setVaultPattern("*");
			queryObjType.setOwnerPattern("*");
			queryObjType.setWhereExpression("");
			queryObjType.setExpandType(false);

			BusinessObjectList typeObjectsList	= queryObjType.evaluate(context);
			BusinessObjectItr typeObjectsItr	= new BusinessObjectItr(typeObjectsList);
				
			Hashtable autoSeriesTableForType = new Hashtable(10);

			String rev = "";
			while(typeObjectsItr.next())
			{
				BusinessObject objGenObj = typeObjectsItr.obj();
				rev = objGenObj.getRevision();
				String actualTypeName = (String)PropertyUtil.getSchemaProperty(context,typeName);
				String translatedString = util.localizeAutoNameSeries(context,rev,actualTypeName,language);

				autoSeriesTableForType.put(rev,translatedString);
			}
			queryObjType.close(context);
			autoNameSeriesTable.put(typeName, autoSeriesTableForType);
		}

		return autoNameSeriesTable;
	}
}
