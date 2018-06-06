/**
 * DECBaselineDetails.java
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 * This JPO gets all revisions for a BusinessObject
 * Project. Infocentral Migration to UI level 3
 * $Archive: $
 * $Revision: 1.2$
 * $Author: 
 * @since AEF 9.5.2.0
 */

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import java.util.StringTokenizer;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.util.StringList;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFBaselineHelper;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

public class DECBaselineDetailsBase_mxJPO
{
	MapList objectList					= new MapList();
	HashMap baselineDetailsMap			= new HashMap();
	IEFBaselineHelper baselineHelper	= null;

	public DECBaselineDetailsBase_mxJPO(Context context, String[] args) throws Exception
	{		
	}

	@com.matrixone.apps.framework.ui.ProgramCallable
	public Object getList(Context context, String[] args) throws Exception
	{		
		HashMap paramMap								= (HashMap)JPO.unpackArgs(args);				
		String objectId									= (String)paramMap.get("objectId");
		String localeLanguage							= (String)paramMap.get("languageStr");
		boolean isInFinalizationState	= false;
		
		StringList selectables = new StringList();
		selectables.add(DomainObject.SELECT_ID);
		selectables.add("physicalid");
		DomainObject doObj = DomainObject.newInstance(context,objectId);
		Map objInfo = doObj.getInfo(context, selectables);			
		String sPhyId = (String) objInfo.get("physicalid");
		if(sPhyId.equals(objectId)){
			objectId = (String) objInfo.get(DomainObject.SELECT_ID);
		}
		MCADServerResourceBundle resourceBundle			= new MCADServerResourceBundle(localeLanguage);
		IEFGlobalCache _cache							= new IEFGlobalCache();
		IEFIntegAccessUtil _util					= new IEFIntegAccessUtil(context, resourceBundle, _cache);		
		String integrationName							= _util.getIntegrationName(context, objectId);		
		
		MCADGlobalConfigObject globalConfigObject     	= null;
		HashMap integrationNameGCOTable					=(HashMap)paramMap.get("GCOTable");

		MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle(localeLanguage), new IEFGlobalCache());

		if(integrationNameGCOTable == null)
			integrationNameGCOTable = (HashMap)paramMap.get("GCOTable");

		if(integrationName != null && integrationNameGCOTable != null && integrationNameGCOTable.containsKey(integrationName) && _util.getAssignedIntegrations(context).contains(integrationName))
		{					
			globalConfigObject	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
		}

		MCADServerGeneralUtil _generalUtil = new MCADServerGeneralUtil(context,globalConfigObject, resourceBundle, _cache);

		baselineHelper 						= new IEFBaselineHelper(context, globalConfigObject, resourceBundle, _cache);

		BusinessObject busObject			= new BusinessObject(objectId);
		busObject.open(context);
        String busType						= busObject.getTypeName();

		isInFinalizationState =  _generalUtil.isBusObjectFinalized(context,busObject);
		boolean isMajorType					= mxUtil.isMajorObject(context,objectId);		
			if(isMajorType)
			{
				BusinessObjectList minorsList = _util.getMinorObjects(context, busObject);
				busObject.close(context);
				if(isInFinalizationState)
				{						
					String[] oids	= new String[1];
					oids[0]			= objectId;
					getBaselineDetails(context, oids, objectId);				
				}
	
				BusinessObjectItr minorBusObjectsItr = new BusinessObjectItr(minorsList);
				
				while(minorBusObjectsItr.next())
				{
					BusinessObject minorBusObject = minorBusObjectsItr.obj();

					String busID	= minorBusObject.getObjectId(context);
					String[] oids	= new String[1];
					oids[0]			= busID;
					getBaselineDetails(context, oids, busID);
				}
			}
			else
			{
				busObject.close(context);
		String[] oids	= new String[1];
        oids[0]			= objectId;
				getBaselineDetails(context, oids, objectId);						
			}
	
		return objectList;
	}

	private void getBaselineDetails(Context context, String[] oids, String objectId) throws Exception
	{
		baselineDetailsMap	= baselineHelper.getBaselineDetailsMap(context, oids);
		if(null != baselineDetailsMap)
		{
			Vector baselineDetailsList = (Vector)baselineDetailsMap.get(objectId);
			Enumeration baselineDetailsEnum = baselineDetailsList.elements();
			while(baselineDetailsEnum.hasMoreElements())
			{
				HashMap baselineDetails = (HashMap)baselineDetailsEnum.nextElement();
				if(null != baselineDetails)
				{
					objectList.add(baselineDetails);
				}
			}
		}		
	}

	public String getNLSTypeInRootNodeDetailsAttributeValue(Context context, String[] args) throws Exception
    {

		String type				= null;
		String localeLanguage	= null;
		String objectId			= null;
		String RootNodeDetails	     = MCADMxUtil.getActualNameForAEFData(context, "attribute_RootNodeDetails");
		Vector mxTypes = new Vector();
        try
        {						
			HashMap params				= (HashMap)JPO.unpackArgs(args);
			HashMap requestMap			= (HashMap)params.get("requestMap");	
			localeLanguage				= requestMap.get("languageStr").toString();
			objectId					= (String) requestMap.get("objectId");
			String[] objIds				=	new String[1];
			objIds[0]					= objectId;
			StringList busSelectionList	= new StringList();	
			
			busSelectionList.add("attribute[" + RootNodeDetails + "]");
			BusinessObjectWithSelectList busWithSelectionList 	= BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);
			BusinessObjectWithSelect busObjectWithSelect 		= (BusinessObjectWithSelect)busWithSelectionList.elementAt(0);
			type 		=  busObjectWithSelect.getSelectData("attribute[" + RootNodeDetails + "]") ; 

				StringTokenizer tokenizer = new StringTokenizer(type, "|");
				while(tokenizer.hasMoreElements())
				{
				   String sType   = (String) tokenizer.nextElement();
				   mxTypes.add(sType);
}

			type 		=  MCADMxUtil.getNLSName(context, "Type", mxTypes.get(0).toString(), "", "" , localeLanguage);

        }
        catch (Exception e)
        {    
			e.printStackTrace();
        }
        return type+"|"+mxTypes.get(1)+"|"+mxTypes.get(2);
    }


}

