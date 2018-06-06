/*
**  IEFEBOMSyncNewPartCreation
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program for creating new Part objects.
*/

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Policy;
import com.matrixone.apps.domain.DomainObject;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.common.Part;


public class IEFEBOMSyncNewPartCreation_mxJPO
{
    private String INITIAL_PART_REVISION	= "A";
	protected String MATCHING_PART_RULE		  = "LATEST_REV";
	protected final String MATCH_CADMODEL_REV = "MATCH_CADMODEL_REV";

  MCADServerResourceBundle	serverResourceBundle	= null;
	MCADMxUtil mxUtil									= null;
	private String ATTR_PART_TYPE						= "";	
    public IEFEBOMSyncNewPartCreation_mxJPO()
    {
    }

    public IEFEBOMSyncNewPartCreation_mxJPO(Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            MCADServerException.createException("not supported no desktop client", null);

    }

    public int mxMain(Context context, String[] args) throws Exception
    {
        return 0;
    }

    public BusinessObject createPart(Context context, String[] args) throws Exception
    {
		String cadObjectId	= args[0];
		String partObjName	= args[1];
		String instanceName	= args[2];
		String ebomRegType	= args[3];
		String ebomRegName	= args[4];
		String ebomRegRev	= args[5];
		String language		= args[6];
		String isMinorType  = args[7];
	String famID		= args[8];

		serverResourceBundle			= new MCADServerResourceBundle(language);
		IEFEBOMConfigObject ebomConfObj	= new IEFEBOMConfigObject(context, ebomRegType, ebomRegName, ebomRegRev);
		mxUtil							= new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());

		ATTR_PART_TYPE			 = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMSync-PartTypeAttribute");		
		INITIAL_PART_REVISION	= ebomConfObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_NEW_PART_REVISION);
		MATCHING_PART_RULE		= ebomConfObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_PART_MATCHING_RULE);
		String PART_POLICY_NAME		= ebomConfObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_NEW_PART_POLICY);		

		if(null != famID && !"".equals(famID))
		{
			String ebomExpositionMode 	= mxUtil.getAttributeForBO(context, famID, MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMExpositionMode"));
			if(null != ebomExpositionMode && "single".equalsIgnoreCase(ebomExpositionMode))
			{
				cadObjectId = famID;
			}
		}

		BusinessObject cadObject = new BusinessObject(cadObjectId);
		cadObject.open(context);
		String cadType		= cadObject.getTypeName();
	
		String cadModelRevision	= cadObject.getRevision();
		String partType		= cadObject.getAttributeValues(context, ATTR_PART_TYPE).getValue();

		if(partType == null || partType.equals(""))
			partType = ebomConfObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_DEFAULT_NEW_PART_TYPE);

		if(partType == null || partType.equals(""))
			MCADServerException.createException(serverResourceBundle.getString("mcadIntegration.Server.Message.PartTypeNotSpecifiedInEBOMRegistry"), null);

		Hashtable	cadAttrTable = getAttributeMap(context, ebomConfObj, cadObject, instanceName, cadType, partType);

		if(MATCHING_PART_RULE.equals(MATCH_CADMODEL_REV))
		{
			/*[NDM]if(isMinorType.equalsIgnoreCase("true"))
			{
				BusinessObject majorObject = mxUtil.getMajorObject(context, cadObject);
				majorObject.open(context);
				cadModelRevision		   = majorObject.getRevision();
				majorObject.close(context);
			}*/

			INITIAL_PART_REVISION = cadModelRevision;
		}

		cadObject.close(context);

		BusinessObject partBusObject = new BusinessObject(partType, partObjName, INITIAL_PART_REVISION, "");
		Part partObj = new Part(partBusObject);
		if(partBusObject.exists(context))
		{
			partBusObject.open(context);
			BusinessObjectList revisionList		= partBusObject.getRevisions(context);
			BusinessObjectItr revisionListItr	=new BusinessObjectItr(revisionList);
			BusinessObject latestBusObj			= null;
			
			while(revisionListItr.next())
			{
				latestBusObj = revisionListItr.obj();
			}

			String latestRev = latestBusObj.getNextSequence(context);
			String vaultName = partBusObject.getVault();
			partBusObject.close(context);

			partBusObject = partBusObject.revise(context, latestRev, vaultName);

			partObj = new Part(partBusObject);
			
			cadAttrTable = setSystemAttributeValues(context, partObj, cadAttrTable);
			partObj.setAttributeValues(context, cadAttrTable);
		}
		else
		{			
			partObj.create(context, PART_POLICY_NAME);			
			cadAttrTable = setSystemAttributeValues(context, partObj, cadAttrTable);
			partObj.setAttributeValues(context, cadAttrTable);

			DomainObject cadObj = new DomainObject(cadObjectId);
			String proj = cadObj.getAltOwner1(context).toString();
			String org	= cadObj.getAltOwner2(context).toString();

			if(proj != null && !"".equals(proj) &&  org != null && !"".equals(org))
			{
				partObj.setPrimaryOwnership(context, org ,proj);
			}

		
			String iSVPMVisibleAttrActualName   = MCADMxUtil.getActualNameForAEFData(context, "attribute_isVPMVisible");
			Vector partAttributeList 	= mxUtil.getAllAttributeNamesOnType(context, partType);
			

			if(partAttributeList != null &&  partAttributeList.contains(iSVPMVisibleAttrActualName))
			{
				Attribute attr_isVPMVisible = new Attribute(new AttributeType(iSVPMVisibleAttrActualName), "False");

				AttributeList attrLst = new AttributeList(1);
				attrLst.add(attr_isVPMVisible);
				
				partObj.setAttributes(context, attrLst);
                	}

		}		

		Policy partPolicyObject  = partBusObject.getPolicy(context);
		partPolicyObject.open(context);
		String partPolicyName  = partPolicyObject.getName();
		
		partPolicyObject.close(context);
		String sPolicyConfiguredPart = MCADMxUtil.getActualNameForAEFData(context, "policy_ConfiguredPart");
		if(sPolicyConfiguredPart !=null && sPolicyConfiguredPart.equals(partPolicyName))
		{
			Hashtable messageDetails = new Hashtable(4);
			messageDetails.put("TYPE", partBusObject.getTypeName());
			messageDetails.put("NAME", partBusObject.getName());
			messageDetails.put("REV", partBusObject.getRevision());
			messageDetails.put("PARTPOLICY", partPolicyName);
			String message = serverResourceBundle.getString("mcadIntegration.Server.Message.EBOMSyncNotSupportedForPolicy", messageDetails);

			MCADServerException.createException(message , null);

		}
		return partBusObject;
    }

	private Hashtable getAttributeMap(Context context, IEFEBOMConfigObject ebomConfObj, BusinessObject cadObject, 
										String instanceName, String busType, String partType) throws Exception
	{
		Hashtable attrValueHash				= new Hashtable();
		Hashtable mandAttrNamehash = new Hashtable();
		//For Mandatory Object Attribute
	        mandAttrNamehash = ebomConfObj.getMandTypeAttributeMapping(context, busType, partType);
	 	getAttrHashtable(context, attrValueHash, cadObject, mandAttrNamehash,partType,true);
	 	String typeLocalConfig	= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-LocalConfig");
				
		//Retrieving Object mapping form the LCO.
		String userName		= context.getUser();
		String prefColElement = "";
		
		Hashtable localhash			= new Hashtable();
		Hashtable localattrHashName = new Hashtable();
		String localConfigObjRev    = MCADMxUtil.getConfigObjectRevision(context);
		BusinessObject localObj		= new BusinessObject(typeLocalConfig, userName, localConfigObjRev, "");
		Attribute prefObjectValue	= localObj.getAttributeValues(context, IEFEBOMConfigObject.ATTR_OBJECT_ATTR_MAPPING);
		String prefObjectMapping	= prefObjectValue.getValue();
		
		String objectId  =  cadObject.getObjectId();
		String integrationName = mxUtil.getIntegrationName(context, objectId);
		
		StringTokenizer prefObjectMappingToken		= new StringTokenizer(prefObjectMapping, "\n");
		while(prefObjectMappingToken.hasMoreElements())
		{
			prefColElement		= (String)prefObjectMappingToken.nextElement();
			int firstIndex		= prefColElement.indexOf("|");
			String integName	= prefColElement.substring(0,firstIndex);
			String prefValue	= prefColElement.substring(firstIndex+1,prefColElement.length());
			if(integName.equals(integrationName))
			{
				if(prefValue!=null && !prefValue.trim().equals(""))
				{
					Enumeration objectStringValue1 = MCADUtil.getTokensFromString(prefValue.trim(), "@");
					while (objectStringValue1.hasMoreElements())
					{
						String obj1 = (String)objectStringValue1.nextElement();
						localhash.put(obj1, obj1);
					}
				}
			}
		}
		//Formatiing Value for the EBOM SYNC
		Enumeration localEnum = localhash.keys();
		String cadTypeName		= "";
		String cadAttrName		= "";
		String partTypeName		= "";
		String partAttrName		= "";
		while(localEnum.hasMoreElements())
		{
			String name		= (String)localEnum.nextElement();				
			String value	= (String)localhash.get(name);
            StringTokenizer token = new StringTokenizer(value, "|");
			cadTypeName  = (String)token.nextElement();
			cadAttrName  = (String)token.nextElement();
            partTypeName = (String)token.nextElement();
            partAttrName = (String)token.nextElement();
			
			if (cadTypeName.equals(busType))
			{
			localattrHashName.put(cadAttrName,partAttrName);
		}
		}

		//For Attribute Mapping From Local object
		getAttrHashtable(context, attrValueHash, cadObject, localattrHashName,partType,false);
	
		return attrValueHash;
	}

	private void getAttrHashtable(Context context, Hashtable attrValueHash, BusinessObject cadObject, Hashtable testHash, String partType, boolean checkIfAttributeExistsOnPart) throws Exception
	{
		Hashtable sysAttrValueHash = new Hashtable();
		String cadAttrName			= "";
		String partAttrName			="";

		Enumeration cadAttrEnum = testHash.keys();
		while(cadAttrEnum.hasMoreElements())
		{
			try
			{
				cadAttrName			= (String) cadAttrEnum.nextElement();
				partAttrName		= (String) testHash.get(cadAttrName);
				String attrValue	= "";

				if(cadAttrName.startsWith("$$"))
					attrValue = getSystemAttributeValues(context, cadObject, cadAttrName);
				else
					attrValue = cadObject.getAttributeValues(context, cadAttrName).getValue();

				if(partAttrName.startsWith("$$"))
					sysAttrValueHash.put(partAttrName, attrValue);
				else
				{
				
					if(checkIfAttributeExistsOnPart)
					{
						Vector	attributeList = mxUtil.getAllAttributeNamesOnType(context, partType);
						
						if(!attributeList.contains(partAttrName))
						{
							Hashtable messageDetails = new Hashtable(2);
							messageDetails.put("TYPE", partType);
							messageDetails.put("ATTRNAME", partAttrName);
	
							String errorMessage = serverResourceBundle.getString("mcadIntegration.Server.Message.AttributeDoesNotExistsOnType", messageDetails);
							MCADServerException.createException(errorMessage, null);
						}
					}
				
					attrValueHash.put(partAttrName, attrValue);
			}
			}
			catch(Exception e)
			{
				MCADServerException.createException(e.getMessage(), e);
			}
		}
		if(sysAttrValueHash != null && sysAttrValueHash.size() > 0)
		{
			attrValueHash.put("System Attributes", sysAttrValueHash);		
		}
	}

	private String getSystemAttributeValues(Context context, BusinessObject cadObj, String attrName) throws Exception
	{
		if("$$Owner$$".equals(attrName))
			return cadObj.getOwner(context).getName();
		else if("$$Description$$".equals(attrName))
			return cadObj.getDescription(context);
		else
			return "";
	}
	
	private Hashtable setSystemAttributeValues(Context context, Part partObj, Hashtable attrMap) throws Exception
	{
		Hashtable sysAttr = (Hashtable) attrMap.get("System Attributes");
		attrMap.remove("System Attributes");

		if(sysAttr != null && sysAttr.containsKey("$$Description$$"))
		{
	          String descAttr = (String) sysAttr.get("$$Description$$");
	          if(descAttr != null && descAttr.trim().length() > 0)
	          {
		          partObj.setDescription(context, descAttr);
		          partObj.update(context);
	          }
		}

		if(sysAttr != null && sysAttr.containsKey("$$Owner$$"))
		{
		  String ownerAttr = (String) sysAttr.get("$$Owner$$");
		  if(ownerAttr != null && ownerAttr.trim().length() > 0)
		  {
			  partObj.setOwner(context, ownerAttr);
			  partObj.update(context);
		  }
		}
		return attrMap;
	}
}

