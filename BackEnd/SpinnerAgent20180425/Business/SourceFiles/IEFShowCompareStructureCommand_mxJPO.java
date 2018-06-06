/*
**  IEF-ShowVersions
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to check if versioning flag in GCO is enabled.
*/

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADUtil;

public class IEFShowCompareStructureCommand_mxJPO
{
    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object.
     * @param args holds no arguments.
     * @throws Exception if the operation fails.
     */
    public IEFShowCompareStructureCommand_mxJPO (Context context, String[] args)
        throws Exception
    {
    }

	public Boolean isObjectAssemblyType(Context context,String[] args) throws Exception
	{
		boolean isObjectAssembly = false;
		
		try
		{
			HashMap paramMap	= (HashMap)JPO.unpackArgs(args);
			String objectId		= (String) paramMap.get("objectId");
			String language		= (String) paramMap.get("languageStr");
			
			MCADMxUtil util = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
			String typeClassMappingAttrName = MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-TypeClassMapping");
			String cadTypeAttrName			= MCADMxUtil.getActualNameForAEFData(context,"attribute_CADType");
			String integrationName			= util.getIntegrationName(context, objectId);
			String cadType					= util.getAttributeForBO(context, objectId, cadTypeAttrName);

			IEFSimpleConfigObject simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, integrationName);
			Hashtable typeClassMappingTable = simpleGCO.getAttributeAsHashtable(typeClassMappingAttrName, "\n", "|");

			String assemblyLikeMap		= (String)typeClassMappingTable.get("TYPE_ASSEMBLY_LIKE");
			Vector asseblyLikeMapList	= MCADUtil.getVectorFromString(assemblyLikeMap, ",");
		
			if(asseblyLikeMapList.contains(cadType))
				isObjectAssembly = true;
			else
				isObjectAssembly = false;
		}
		catch(Throwable e)
		{
		}
		
		return new Boolean(isObjectAssembly);		
	}
}

