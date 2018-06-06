/*
**  IEFEBOMSyncFindMatchingPartBase
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
*/

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Policy;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.common.Part;

public class IEFEBOMSyncFindMatchingPartBase_mxJPO
{
    protected String MATCHING_PART_RULE			= "LATEST_REV"; 
    protected String PART_RELEASE_STATE			= "Complete";
	protected final String MATCH_CADMODEL_REV	= "MATCH_CADMODEL_REV";
	protected boolean confAttrFailAtMissingPart	= false;
	String sVersionOfRelName					= "";
    String SELECT_ON_MAJOR						= "";
	
 MCADMxUtil					mxUtil					= null;
    MCADServerResourceBundle	serverResourceBundle	= null;
	IEFGlobalCache				cache					= null;
	protected String ATTR_PART_TYPE						= "";
	protected String LOCAL_CONFIG_TYPE					= "";

	IEFEBOMConfigObject ebomConfObject	= null;
	Hashtable			cadAttrTable	= null;
	HashMap policySequenceMap			= new HashMap();

	public IEFEBOMSyncFindMatchingPartBase_mxJPO()
    {
    }

    public IEFEBOMSyncFindMatchingPartBase_mxJPO(Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            MCADServerException.createException("not supported no desktop client", null);
		
		if(args.length == 4)
		{
			String ebomRegType	= args[0];
			String ebomRegName	= args[1];
			String ebomRegRev	= args[2];
			String language		= args[3];

			ebomConfObject			= new IEFEBOMConfigObject(context, ebomRegType, ebomRegName, ebomRegRev);
			serverResourceBundle	= new MCADServerResourceBundle(language);
			cache					= new IEFGlobalCache();
			mxUtil					= new MCADMxUtil(context, serverResourceBundle, cache);
			sVersionOfRelName		= MCADMxUtil.getActualNameForAEFData(context,"relationship_VersionOf");
            SELECT_ON_MAJOR			= "from[" + sVersionOfRelName + "].to.";

			confAttrFailAtMissingPart	= "true".equalsIgnoreCase(ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_FAIL_ON_NOT_FINDING_PART));
		}
    }

    public int mxMain(Context context, String[] args) throws Exception
    {
        return 0;
    }

    public BusinessObject findMatchingPart(Context context, String[] args) throws Exception
    {
		String cadObjectId	= args[0];
		String partObjName	= args[1];
		String instanceName = args[2];
		String ebomRegType	= args[3];
		String ebomRegName	= args[4];
		String ebomRegRev	= args[5];
		String language		= args[6];
		///String isMinorType  = args[7]; //[NDM]
		String famID		= args[7];
		

		ebomConfObject			 = new IEFEBOMConfigObject(context, ebomRegType, ebomRegName, ebomRegRev);
		serverResourceBundle	 = new MCADServerResourceBundle(language);
		cache					 = new IEFGlobalCache();
		mxUtil					 = new MCADMxUtil(context, serverResourceBundle, cache);

		sVersionOfRelName		= MCADMxUtil.getActualNameForAEFData(context,"relationship_VersionOf");
        SELECT_ON_MAJOR			= "from[" + sVersionOfRelName + "].to.";

		MATCHING_PART_RULE		 = ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_PART_MATCHING_RULE);		
		PART_RELEASE_STATE	     = ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_RELEASE_PART_STATE);
		
		ATTR_PART_TYPE			 = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMSync-PartTypeAttribute");		
		confAttrFailAtMissingPart	= "true".equalsIgnoreCase(ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_FAIL_ON_NOT_FINDING_PART));
		
		if(null != famID && !"".equals(famID))
		{
			String ebomExpositionMode 	= mxUtil.getAttributeForBO(context, famID, MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMExpositionMode"));
			if(null != ebomExpositionMode && "single".equalsIgnoreCase(ebomExpositionMode))
			{
				cadObjectId = famID;
			}
		}

		StringList busSelects = new StringList();
		busSelects.add("type");
		busSelects.add("name");
		busSelects.add("revision");
		busSelects.add("attribute[" + ATTR_PART_TYPE + "]");
		busSelects.add("policy");
		busSelects.add("policy.revision");
		
		//[NDM]		
		/*busSelects.add(SELECT_ON_MAJOR + "revision");
		busSelects.add(SELECT_ON_MAJOR + "policy");
		busSelects.add(SELECT_ON_MAJOR + "policy.revision");*/
		//[NDM]
		String [] cadids = new String[1];
		cadids[0] = cadObjectId;

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, cadids, busSelects);

		BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(0);

		BusinessObject cadObject = new BusinessObject(cadObjectId);
		cadObject.open(context);

		String cadType				= busObjectWithSelect.getSelectData("type");
		String cadModelRevision		= busObjectWithSelect.getSelectData("revision");
		String cadPolicyName		= busObjectWithSelect.getSelectData("policy");
		String cadModelPolicyRevSeq = busObjectWithSelect.getSelectData("policy.revision");
		String partType				= busObjectWithSelect.getSelectData("attribute[" + ATTR_PART_TYPE + "]");
//[NDM]
	/*	if(MATCHING_PART_RULE.equals(MATCH_CADMODEL_REV) && isMinorType.equalsIgnoreCase("true"))
		{
			cadModelRevision	 = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revision");
			cadPolicyName		 = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "policy");
		    cadModelPolicyRevSeq = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "policy.revision");
		}*/
//[NDM]
		if(partType.equals(""))
			partType = ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_DEFAULT_NEW_PART_TYPE);

		if(partType == null || partType.equals(""))
			MCADServerException.createException(serverResourceBundle.getString("mcadIntegration.Server.Message.partTypeNotSpecified"), null);

		BusinessObject partObject = getPartRevisionForRule(context, partType, partObjName, MATCHING_PART_RULE, cadPolicyName, cadModelPolicyRevSeq, cadModelRevision, cadType);

		if(partObject != null && !isPartReleased(context, partObject.getObjectId(context)))
		{
			copyAttribsFromCadObjToPart(context, partObject, cadObject, instanceName, cadType, partType);
		}

		cadObject.close(context);

		return partObject;
    }

	public void transferCadAttribsToPart(Context context, String[] args) throws Exception
	{
		String cadObjectId	= args[0];
		String partObjId	= args[1];
		String instanceName = args[2];
		
		BusinessObject cadObject = new BusinessObject(cadObjectId);
		cadObject.open(context);
		String cadType		= cadObject.getTypeName();
		
		if(args.length == 5)
		{
			MCADServerGeneralUtil serverGeneralUtil		= null;
			MCADGlobalConfigObject globalConfigObject 	= null;
			BusinessObject activeMinorObject			= null;
			String famID 								= null;
			String ebomExpositionMode					= null;
			String cadObjectType 						= mxUtil.getCADTypeForBO(context, cadObject);
			
			String[] packedGCO = new String[2];

			packedGCO[0] 	= args[3];
			packedGCO[1] 	= args[4];

			globalConfigObject 	= (MCADGlobalConfigObject) JPO.unpackArgs(packedGCO);

			serverGeneralUtil	= new MCADServerGeneralUtil(context,globalConfigObject, serverResourceBundle, cache);

			if(globalConfigObject != null && globalConfigObject.isTypeOfClass(cadObjectType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
			{
			//[NDM] H68
				//if(serverGeneralUtil.isBusObjectFinalized(context, cadObject))
				//{
					famID	= serverGeneralUtil.getTopLevelFamilyObjectForInstance(context,cadObject.getObjectId());
				//}
				//else
				//{
					//activeMinorObject = mxUtil.getActiveMinor(context, cadObject);
					//famID	= serverGeneralUtil.getTopLevelFamilyObjectForInstance(context,activeMinorObject.getObjectId());
				//}

				if(null != famID)
				{
					ebomExpositionMode 	= mxUtil.getAttributeForBO(context, famID, MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMExpositionMode"));

					if("single".equalsIgnoreCase(ebomExpositionMode))
					{
						cadObject.close(context);
						cadObject = new BusinessObject(famID);
						cadObject.open(context);
						cadType = cadObject.getTypeName();
					}
				}
			}
		}
		
		BusinessObject partObject = new BusinessObject(partObjId);
		partObject.open(context);
		String partType		= partObject.getTypeName();
		partObject.close(context);

		copyAttribsFromCadObjToPart(context, partObject, cadObject, instanceName, cadType, partType);

		cadObject.close(context);
	}

	public void copyAttribsFromCadObjToPart(Context context, BusinessObject partObject, BusinessObject cadObject, 
										String instanceName, String cadType, String partType) throws Exception
	{		
		Hashtable	cadAttrTable = getAttributeMap(context,ebomConfObject, cadObject, instanceName, cadType, partType);
		Part partObj = new Part(partObject);
		partObj.openObject(context);
		cadAttrTable = setSystemAttributeValues(context, partObj, cadAttrTable);
		partObj.setAttributeValues(context, cadAttrTable);
		partObj.closeObject(context, true);
	}

    protected BusinessObject getPartRevisionForRule(Context context, String partTypeName, String partName, String matchingRule, String cadPolicyName, String cadModelPolicyRevSeq, String cadModelRevision, String cadType) throws MCADException
    {
        String Args[] = new String[5];
		Args[0] = partTypeName;
		Args[1] = partName;
		Args[2] = "*";
		Args[3] = "revisions";
		Args[4] = "|";

        BusinessObject retBus = null;
        
        try
        {
            String sResult = mxUtil.executeMQL(context,"temp query bus $1 $2 $3 select $4 dump $5", Args);
            if (sResult.startsWith("true"))
            {
                sResult = sResult.substring(5);
                StringTokenizer strtok1 = new StringTokenizer(sResult, "\n");
                if(strtok1.hasMoreTokens())
                {
                    StringTokenizer strtok2 = new StringTokenizer(strtok1.nextToken(), "|");
                    String sType			= strtok2.nextToken();
                    String sName			= strtok2.nextToken();
                    String sRev				= "";
					
					Vector revList	= new Vector();
					while (strtok2.hasMoreTokens())
					{
						revList.addElement(strtok2.nextToken());
					}

					String latestRevision = "";
					for(int i=revList.size()-1; i>0; i--)
					{
						String currRev		= (String) revList.elementAt(i);
						Args= new String[6];
						Args[0] = sType;
						Args[1] = sName;
						Args[2] = currRev;
						Args[3] = "current";
						Args[4] = "policy.property[PolicyClassification].value";
						Args[5] = "|";
						String indvlResult	= mxUtil.executeMQL(context,"print bus $1 $2 $3 select $4 $5 dump $6", Args);

						if (indvlResult.startsWith("true"))
						{
							StringTokenizer indvlTok = new StringTokenizer(indvlResult.substring(5), "|");
							String classification = "";
							String state = "";
							if(indvlTok.hasMoreTokens())
								state = indvlTok.nextToken();
							if(indvlTok.hasMoreTokens())
								classification = indvlTok.nextToken();

							if(!"Equivalent".equals(classification))
							{
								if(latestRevision.equals(""))
									latestRevision = currRev;

								if(matchingRule.equals("LATEST_REV"))
								{
									sRev = currRev;
									break;
								}								
								else if(matchingRule.equals(MATCH_CADMODEL_REV) && currRev.equals(cadModelRevision))
								{
									sRev = currRev;
									break;
								}	
							}

						}
					}

                    if(!"".equals(sRev))
					{
						retBus = new BusinessObject(sType, sName, sRev, "");
                        retBus.open(context);
                        Policy partPolicyObject  = retBus.getPolicy(context);
						partPolicyObject.open(context);
						String partPolicyName  = partPolicyObject.getName();
						String sPolicyConfiguredPart = MCADMxUtil.getActualNameForAEFData(context, "policy_ConfiguredPart");
						
						if(sPolicyConfiguredPart !=null && sPolicyConfiguredPart.equals(partPolicyName))
						{
							partPolicyObject.close(context);
							retBus.close(context);
							
							Hashtable messageDetails = new Hashtable(4);
							messageDetails.put("TYPE", sType);
							messageDetails.put("NAME", sName);
							messageDetails.put("REV", sRev);
							messageDetails.put("PARTPOLICY", partPolicyName);
							String message = serverResourceBundle.getString("mcadIntegration.Server.Message.EBOMSyncNotSupportedForPolicy", messageDetails);
							
							MCADServerException.createException(message , null);
						}

						if(matchingRule.equals(MATCH_CADMODEL_REV))
						{
							String partRevSequence = "";
							if(policySequenceMap.containsKey(partPolicyName))
							{
								partRevSequence = (String)policySequenceMap.get(partPolicyName);
							}
							else
							{
								partRevSequence = partPolicyObject.getSequence();
								policySequenceMap.put(partPolicyName, partRevSequence);
							}
							
							partPolicyObject.close(context);

							if(!cadModelPolicyRevSeq.equals(partRevSequence))
							{
								Hashtable messageDetails = new Hashtable(3);
								messageDetails.put("CADPOLICY", cadPolicyName);
								messageDetails.put("PARTPOLICY", partPolicyName);
							
								String message = serverResourceBundle.getString("mcadIntegration.Server.Message.RevisionSequenceCADandDevMisMatch", messageDetails);
							
								MCADServerException.createException(message , null);
							}

							if(isPartReleased(context, retBus.getObjectId(context)))
							{
								Hashtable messageDetails = new Hashtable(3);
								messageDetails.put("NAME", sName);
								messageDetails.put("CADMODELTYPE", sType);
				
								String message = serverResourceBundle.getString("mcadIntegration.Server.Message.partWithMatchRevAlreadyReleased", messageDetails);

								MCADServerException.createException(message, null);
							}

						}
						retBus.close(context);
					}
					else if(matchingRule.equals(MATCH_CADMODEL_REV) && !"".equals(latestRevision))
					{
						BusinessObject partObject = new BusinessObject(sType, sName, latestRevision, "");
						//move logic for throwing error outside this method
						retBus = getPartRevisionIfReleased(context, partObject.getObjectId(context), cadPolicyName, cadModelPolicyRevSeq, cadModelRevision, cadType);
					}
                }
            }
        }
        catch (Exception ex)
        {
			MCADServerException.createException(ex.getMessage(), ex);
        }

        return retBus;
    }

	protected BusinessObject getPartRevisionIfReleased(Context context, String partObjectId, String cadPolicyName, String cadModelPolicyRevSeq, String targetRevision, String sType) throws Exception
	{
	   String revisedPartId = partObjectId;
	   BusinessObject partObject = new BusinessObject(partObjectId);
	   partObject.open(context);
	  
	   String sName = partObject.getName();
	   if(isPartReleased(context, partObjectId))
	   {
			BusinessObject revisedPartObject	= partObject.revise(context, targetRevision, partObject.getVault());
			revisedPartId						= revisedPartObject.getObjectId(context);

			partObject.close(context);
			return new BusinessObject(revisedPartId);
	   }
	   else if(confAttrFailAtMissingPart)
	   {
			Policy partPolicyObject  = partObject.getPolicy(context);
		    partPolicyObject.open(context);
			String partPolicyName  = partPolicyObject.getName();
			String partRevSequence = "";

			if(policySequenceMap.containsKey(partPolicyName))
			{
				partRevSequence = (String)policySequenceMap.get(partPolicyName);
			}
			else
			{
				partRevSequence = partPolicyObject.getSequence();
				policySequenceMap.put(partPolicyName, partRevSequence);
			}
			
			partPolicyObject.close(context);

			if(!cadModelPolicyRevSeq.equals(partRevSequence))
			{
				Hashtable messageDetails = new Hashtable(3);
				messageDetails.put("CADPOLICY", cadPolicyName);
				messageDetails.put("PARTPOLICY", partPolicyName);

				String message = serverResourceBundle.getString("mcadIntegration.Server.Message.RevisionSequenceCADandDevMisMatch", messageDetails);

				MCADServerException.createException(message , null);
			}

			//Latest revision without matching revision
			Hashtable messageDetails = new Hashtable(3);
			messageDetails.put("NAME", sName);
			messageDetails.put("CADMODELTYPE", sType);
			
			String message = serverResourceBundle.getString("mcadIntegration.Server.Message.partRevAndCADModelRevMismatch", messageDetails);

			MCADServerException.createException(message,null);

			partObject.close(context);
			return new BusinessObject(revisedPartId);
		}
		else
		{
			partObject.close(context);
			return null;
		}
	}

	protected boolean isPartReleased(Context context, String partObjectId)
	{
		boolean bReleased	= false;
		String Args[] = new String[3];
		Args[0] = partObjectId;
		Args[1] = "current";
		Args[2] = "|";
		String mqlCmdResult		= mxUtil.executeMQL(context,"print bus $1 select $2 dump $3", Args);

		if (mqlCmdResult.startsWith("true"))
		{
			String currentState = mqlCmdResult.substring(5);

			String PART_RELEASE_STATE = ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_RELEASE_PART_STATE);

			//If part is released, then revise the part
			if(PART_RELEASE_STATE.equals(currentState))
			{
				bReleased = true;
			}
		}
	
		return bReleased;
	}

	protected Hashtable getAttributeMap(Context context, IEFEBOMConfigObject ebomConfObj, BusinessObject cadObject, 
										String instanceName, String cadType, String partType) throws Exception
	{
		Hashtable attrValueHash = new Hashtable();
		Hashtable mandAttrNamehash = new Hashtable();
		//For Mandatory Object Attribute
		mandAttrNamehash = ebomConfObj.getMandTypeAttributeMapping(context, cadType, partType);
		
		getAttrHashtable(context, attrValueHash, cadObject, mandAttrNamehash,partType,true);
		
		LOCAL_CONFIG_TYPE = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-LocalConfig");
		
		//Taking Pref Value From LCO
		String userName		= context.getUser();
		String prefColElement = "";
		
		Hashtable localhash			= new Hashtable();
		Hashtable localattrHashName = new Hashtable();
		
		String localConfigObjRev    =  MCADMxUtil.getConfigObjectRevision(context);
		BusinessObject localObj		= new BusinessObject(LOCAL_CONFIG_TYPE, userName, localConfigObjRev, "");
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
		//Formating Value for the EBOM SYNC
		String cadTypeName		= "";
		String cadAttrName		= "";
		String partTypeName		= "";
		String partAttrName		= "";
		Enumeration localEnum = localhash.keys();
		while(localEnum.hasMoreElements())
		{
			String name		= (String)localEnum.nextElement();				
			String value	= (String)localhash.get(name);
            StringTokenizer token = new StringTokenizer(value, "|");
			cadTypeName  = (String)token.nextElement();
			cadAttrName  = (String)token.nextElement();
            partTypeName = (String)token.nextElement();
            partAttrName = (String)token.nextElement();
			
			if(cadTypeName.equals(cadType))
			        localattrHashName.put(cadAttrName,partAttrName);
		}
		
		//For Attribute Mapping From Local object
		getAttrHashtable(context, attrValueHash, cadObject, localattrHashName,partType,false);
		
		return attrValueHash;
	}

	private void getAttrHashtable(Context context, Hashtable attrValueHash, BusinessObject cadObject, Hashtable testHash, String partType, boolean checkIfAttributeExistsOnPart) throws Exception
	{
		Hashtable sysAttrValueHash = new Hashtable();
		
		String cadAttrName		= "";
		String partAttrName		= "";
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
				{
					Attribute attrib = cadObject.getAttributeValues(context, cadAttrName);
					if(attrib != null)
					{
						attrValue = attrib.getValue();
					}
				}

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

	protected String getSystemAttributeValues(Context context, BusinessObject cadObj, String attrName) throws Exception
	{
		if("$$Owner$$".equals(attrName))
			return cadObj.getOwner(context).getName();
		else if("$$Description$$".equals(attrName))
			return cadObj.getDescription(context);
		else
			return "";
	}
	
	protected Hashtable setSystemAttributeValues(Context context, Part partObj, Hashtable attrMap) throws Exception
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


