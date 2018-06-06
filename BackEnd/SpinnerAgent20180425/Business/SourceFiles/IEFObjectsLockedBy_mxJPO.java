/*
**  IEFObjectsLockedBy
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  JPO to find which objects are locked by the user
*/

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.SelectList;

import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;


public class IEFObjectsLockedBy_mxJPO
{
    private String IEF_PROPERTIES_FILE		= "emxIEFDesignCenter";
    private final String IEF_QUERY_LIMIT	= "eServiceInfoCentral.QueryLimit";
	private String query_Obj_limit			= null;
	private String name_filter				= null;

    public IEFObjectsLockedBy_mxJPO(Context context, String[] args) throws Exception
    {
    }
    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getList(Context context, String[] args) throws Exception
    {
		HashMap hashMapArgs = (HashMap) JPO.unpackArgs(args);
		MCADGlobalConfigObject gco = (MCADGlobalConfigObject)hashMapArgs.get("GCO");

		if(hashMapArgs.containsKey("DSCMyLockedObjectLimit"))
			query_Obj_limit = (String)hashMapArgs.get("DSCMyLockedObjectLimit");
		
		if(hashMapArgs.containsKey("DSCMyLockedObjectsNameFilter"))
		{
			name_filter = (String)hashMapArgs.get("DSCMyLockedObjectsNameFilter");
		
			if(name_filter.trim().equals(""))
				name_filter = null;
		}
		
		return getListDetails(context, args, gco);
	}

	public Object getListForDesktopClient(Context context, String[] args) throws Exception
    {
		MapList businessObjectList  = new MapList();
		Hashtable hashtableArgs		= (Hashtable) JPO.unpackArgs(args);
		HashMap gcoTable			= (HashMap)hashtableArgs.get("gcoTable");
		Iterator itr				= gcoTable.keySet().iterator();

		while(itr.hasNext())
		{
			String key = (String) itr.next();
			if(!"MSOffice".equals(key))
			{
			  MCADGlobalConfigObject gco = (MCADGlobalConfigObject) gcoTable.get(key);
			  businessObjectList.addAll((MapList)getListDetails(context, args, gco));
		        }
		}
		
		return businessObjectList;
	}

    public Object getListDetails(Context context, String[] args, MCADGlobalConfigObject gco) throws Exception
    {
        MapList businessObjectList = new MapList();

        String type     		= null;
        String name             = name_filter;
        String revision         = null;
        String vault            = null;
        String owner            = null;
        String searchText       = null;
		boolean expandFlag		= false;

		//find only objects whose type is not hidden
		//mql and adk returns objects whose type is hidden, so set whereclause to filter the result
		//This will return only the major objects since in IEF, minor objects are never get locked
        String whereClause			= "(locked == TRUE) && (locker == '" + context.getUser() + "' && type.hidden == FALSE)";
        ResourceBundle iefProps		= ResourceBundle.getBundle(IEF_PROPERTIES_FILE);
        String queryLimit			= null;

		if(null != query_Obj_limit && !"".equals(query_Obj_limit))
			queryLimit	= query_Obj_limit;
        else
			queryLimit	= iefProps.getString(IEF_QUERY_LIMIT);

		if(args != null)
		{
			//Get only the integration related types from the GCO
			if(gco != null)
			{
			    Vector types = gco.getAllMappedTypes();
			    if(types != null)
			    {
			    	type = MCADUtil.getDelimitedStringFromCollection(types, ",");
                    if(type == null || type.length() == 0)
                    	type = null;
			    }
				if(types.isEmpty())
					{

							return businessObjectList;
					}
				
			}
			else
			{
				System.out.println("[IEFObjectsLockedBy:getList] Exception: Global Config Object not available");
				return businessObjectList;
			}
		}

        try
        {
            //Only the following attributes will be extracted from the query
            SelectList resultSelects = new SelectList(4);
            resultSelects.add(DomainObject.SELECT_ID);
            resultSelects.add(DomainObject.SELECT_NAME);
            resultSelects.add(DomainObject.SELECT_TYPE);
            resultSelects.add(DomainObject.SELECT_REVISION);

			//Find the objects now
			businessObjectList = DomainObject.findObjects(  context,
															type,
															name,
															revision,
															owner,
															vault,
															whereClause,
															"",
															expandFlag,
															resultSelects,
															Short.parseShort(queryLimit),
															null,
															searchText);

        }
        catch( Exception ex )
        {
            System.out.println("[IEFObjectsLockedBy:getList]Exception:" + ex.getMessage());
            businessObjectList = new MapList();
        }

        //emxInfoTable takes the MapList as the input parameter to display the output.
        return businessObjectList;
    }
}

