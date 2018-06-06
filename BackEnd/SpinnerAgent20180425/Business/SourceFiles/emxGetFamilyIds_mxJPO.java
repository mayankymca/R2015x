/*
**  ${CLASSNAME}
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  
*/
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

 
public class emxGetFamilyIds_mxJPO 
{
	private MCADMxUtil	util								= null;
	private MCADServerResourceBundle serverResourceBundle	= null;
	private IEFGlobalCache globalcache						= null;
	private HashMap integrationNameGCOTable					= null;
	private Hashtable integrationNameInstanceIdsTable				= null;
	
	public emxGetFamilyIds_mxJPO ()
	{
	  
	}

	public emxGetFamilyIds_mxJPO (Context context, String[] args) throws Exception
	{
	}

public Hashtable getFamilyIds(Context context,String[] args) throws Exception
	{
		Hashtable instIdFamIdTable = null;
		HashMap map = (HashMap)JPO.unpackArgs(args);
		Set objIds = (Set)map.get("objectIdSet");
		globalcache				= new IEFGlobalCache();
		serverResourceBundle 	= new MCADServerResourceBundle(context.getSession().getLanguage());
		util					= new MCADMxUtil(context, serverResourceBundle, globalcache);

		String SEL_ATTR_SOURCE	   		  = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";
		String SEL_ATTR_CADTYPE	   		  = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType") + "]";
		String typeGlobalConfig		  = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");

		StringList busSelectionList = new StringList();

		busSelectionList.addElement("id");
		busSelectionList.addElement(SEL_ATTR_SOURCE); 
		busSelectionList.addElement(SEL_ATTR_CADTYPE);

		String []  oids = new String[objIds.size()];
		objIds.toArray(oids);

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, oids, busSelectionList);		
		for(int i = 0; i < buslWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);	

			String integrationName	 = null;

			String integrationSource = busObjectWithSelect.getSelectData(SEL_ATTR_SOURCE);
			if(integrationSource != null)
			{
				StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

				if(integrationSourceTokens.hasMoreTokens())
					integrationName  = integrationSourceTokens.nextToken();
			}

			String id                  = busObjectWithSelect.getSelectData("id");;
			MCADGlobalConfigObject gco = null;
			if(integrationName != null && integrationNameGCOTable != null && integrationNameGCOTable.containsKey(integrationName))
			{					
				gco	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
			}
			else if(integrationName != null)
			{
				String gcoName	= this.getGlobalConfigObjectName(context, id);
				gco				= getGlobalConfigObject(context, gcoName, typeGlobalConfig ,util);
				if(null == integrationNameGCOTable)
				{
					integrationNameGCOTable = new HashMap();
					integrationNameGCOTable.put(integrationName, gco);
				}
				else if(!integrationNameGCOTable.containsKey(integrationName))
					integrationNameGCOTable.put(integrationName, gco);
			}

			String cadType = busObjectWithSelect.getSelectData(SEL_ATTR_CADTYPE);

			if(gco != null)
			{
				
				IEFEBOMConfigObject ebomConfigObj = null;
				
				String sEBOMRegistryTNR = gco.getEBOMRegistryTNR();
				StringTokenizer token 	= new StringTokenizer(sEBOMRegistryTNR, "|");
				if(token.countTokens() >= 3)
				{
					String sEBOMConfigObjType			= (String) token.nextElement();
					String sEBOMConfigObjName			= (String) token.nextElement();
					String sEBOMConfigObjRev			= (String) token.nextElement();

					ebomConfigObj	= new IEFEBOMConfigObject(context, sEBOMConfigObjType, sEBOMConfigObjName, sEBOMConfigObjRev);
				}

				String attrAssignPartToMajorValue = "";

				if(null!= ebomConfigObj)
				{
					attrAssignPartToMajorValue = "true";//ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_ASSIGN_PART_TO_MAJOR);
				}
				
				boolean isAssignPartToMajor = attrAssignPartToMajorValue.equalsIgnoreCase("true");
				
				String[]ids = null;
				String validId = id;
				
				boolean isTypeInstanceLike = gco.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE);
				
				if(isAssignPartToMajor)
				{
					MCADServerGeneralUtil serverGeneralUtil		= new MCADServerGeneralUtil(context, gco, serverResourceBundle, globalcache);
					ids = serverGeneralUtil.getValidObjctIdForCheckout(context, id);
					validId = ids[1];
					//[NDM] H68
					//if(!serverGeneralUtil.isBusObjectFinalized(context, id))
					//{
						if(instIdFamIdTable == null)
						{
							instIdFamIdTable = new Hashtable();
						}
						//instIdFamIdTable.put(id, validId); for issue  IR-411971-3DEXPERIENCER2015x
					//}
				}
				
				if(isTypeInstanceLike)
				{
					Set inputInstanceIDS = null;
					if(integrationNameInstanceIdsTable == null)
					{
						integrationNameInstanceIdsTable = new Hashtable();
						inputInstanceIDS = new HashSet();
					}
					else if(integrationNameInstanceIdsTable.containsKey(integrationName))
					{
						inputInstanceIDS = (Set)integrationNameInstanceIdsTable.get(integrationName);
					}
					else
					{
						inputInstanceIDS = new HashSet();
					}

					inputInstanceIDS.add(validId);
					integrationNameInstanceIdsTable.put(integrationName, inputInstanceIDS);
				}
			}
		}
		if(null != integrationNameInstanceIdsTable && integrationNameInstanceIdsTable.size() > 0)
		{
			Iterator iterator =  integrationNameInstanceIdsTable.keySet().iterator();
			while(iterator.hasNext())
			{
				String integName 		= (String)iterator.next();
				Set inputInstanceIDS 	= (Set)integrationNameInstanceIdsTable.get(integName);
				MCADGlobalConfigObject globalConfigObj	= (MCADGlobalConfigObject)integrationNameGCOTable.get(integName);
				MCADServerGeneralUtil serverGeneralUtil		= new MCADServerGeneralUtil(context, globalConfigObj, serverResourceBundle, globalcache);

				if(instIdFamIdTable == null)
				{
					instIdFamIdTable	= serverGeneralUtil.getTopLevelFamilyObjectForInstanceIDs(context, inputInstanceIDS, false);
				}
				else
				{
					Hashtable returnTable = serverGeneralUtil.getTopLevelFamilyObjectForInstanceIDs(context, inputInstanceIDS, false);
					instIdFamIdTable.putAll(returnTable);
				}
			}
		}
		return instIdFamIdTable;
	}

	private String getGlobalConfigObjectName(Context context, String busId) throws Exception
	{
		// Get the IntegrationName
		IEFGuessIntegrationContext_mxJPO guessIntegration = new IEFGuessIntegrationContext_mxJPO(context, null);
		String jpoArgs[] = new String[1];
		jpoArgs[0] = busId;
		String integrationName = guessIntegration.getIntegrationName(context, jpoArgs);
		// Get the relevant GCO Name 
		String gcoName = null;

		IEFSimpleConfigObject simpleLCO = IEFSimpleConfigObject.getSimpleLCO(context);
		String gcoType  = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
		String attrName = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");
		if(simpleLCO.isObjectExists())
		{
			Hashtable integNameGcoMapping = simpleLCO.getAttributeAsHashtable(attrName, "\n", "|");
			gcoName = (String)integNameGcoMapping.get(integrationName);	
			if(null == gcoName)
			{
				IEFGetRegistrationDetails_mxJPO registrationDetailsReader = new IEFGetRegistrationDetails_mxJPO(context, null);
				String args[] = new String[1];
				args[0] = integrationName;
				String registrationDetails = registrationDetailsReader.getRegistrationDetails(context, args);
				
				gcoName 	           = registrationDetails.substring(registrationDetails.lastIndexOf("|")+1);
			}

		}
		else
		{
			IEFGetRegistrationDetails_mxJPO registrationDetailsReader = new IEFGetRegistrationDetails_mxJPO(context, null);
			String args[] = new String[1];
			args[0] = integrationName;
			String registrationDetails = registrationDetailsReader.getRegistrationDetails(context, args);
			gcoName 	           = registrationDetails.substring(registrationDetails.lastIndexOf("|")+1);
		}

		return gcoName;
	}

	protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String gcoName, String gcoType, MCADMxUtil mxUtil) throws Exception
	{
		MCADGlobalConfigObject gcoObject	= null;

		if(gcoName != null && gcoName.length() > 0)
		{
			MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(null);
			gcoObject							= configLoader.createGlobalConfigObject(context, mxUtil, gcoType, gcoName);
		}
		return gcoObject;
	}
}
