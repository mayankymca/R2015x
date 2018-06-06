/*
**  IEFCADStructureCreatorBase
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  JPO for creating CAD structure from EBOM
*/

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.BusinessObject;
import matrix.db.BusinessType;
import matrix.db.Context;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.JPO;
import matrix.db.Policy;
import matrix.db.Relationship;
import matrix.db.RelationshipWithSelect;
import matrix.db.Vault;
import matrix.util.MatrixException;
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
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;
import com.matrixone.apps.domain.util.PropertyUtil;

public class IEFCADStructureCreatorBase_mxJPO
{
    protected MCADMxUtil util                           = null;
    protected MCADServerGeneralUtil generalUtil         = null;
    protected MCADServerResourceBundle resourceBundle   = null;
	protected IEFGlobalCache cache						= null;
    protected MCADGlobalConfigObject globalConfigObject = null;
    protected MCADLocalConfigObject localConfigObject	= null;

	protected BusinessObject assemblyTemplateObject		= null;
	protected BusinessObject componentTemplateObject	= null;

    protected String iefStartDesignJPOAttrName          = "";
	protected String iefStartDesignFormAttrName			= "";
    protected String quantityAttrName                   = "";
	protected String source								= "";
	protected String ebomRelationshipName				= "";
	protected String cadSubComponentRelationshipName	= "";
	protected String partSpecificationRelationshipName  = "";	
	protected String matchingPartString					= "";

	protected Hashtable designObjIDTemplateObjIDMap     = null;
	protected Hashtable attrRelTable					= null;

	protected final String MATCH_CADMODEL_REV			= "MATCH_CADMODEL_REV";
	protected boolean FAIL_AT_FIRST_ERROR				= false;

	protected IEFEBOMConfigObject ebomConfObject		= null;

	protected IEFCreateDesignObject_mxJPO iefCreateDesignObjectJPO = null;

	protected HashMap policySequenceMap					= null;
	
    public  IEFCADStructureCreatorBase_mxJPO  ()
	{

    }

	public IEFCADStructureCreatorBase_mxJPO (Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            MCADServerException.createException("not supported no desktop client", null);
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }

    /**
	 * This method initializes all the class members useful in the JPO operations
     */
    public void initialize(Context context,MCADGlobalConfigObject inputGCO, MCADLocalConfigObject inputLCO, String languageName) throws MCADException
    {
        try
        {
            this.globalConfigObject = inputGCO;
            this.localConfigObject =  inputLCO;
            	
            this.resourceBundle     = new MCADServerResourceBundle(languageName);
			this.cache				= new IEFGlobalCache();
            this.util               = new MCADMxUtil(context, resourceBundle, cache);
            this.generalUtil        = new MCADServerGeneralUtil(context, globalConfigObject, resourceBundle, cache);

            this.iefStartDesignJPOAttrName	= MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-StartDesignJPO");
			this.iefStartDesignFormAttrName = MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-StartDesignForm");
            this.quantityAttrName			= MCADMxUtil.getActualNameForAEFData(context,"attribute_Quantity");
			
			this.ebomRelationshipName				= MCADMxUtil.getActualNameForAEFData(context,"relationship_EBOM");
			this.cadSubComponentRelationshipName	= MCADMxUtil.getActualNameForAEFData(context,"relationship_CADSubComponent");

			Hashtable assemblyLikeRelsTable = globalConfigObject.getRelationshipsOfClass(MCADAppletServletProtocol.ASSEMBLY_LIKE);
			
			if(!assemblyLikeRelsTable.containsKey(cadSubComponentRelationshipName))
			{
				Enumeration assemblyLikeRelList = assemblyLikeRelsTable.keys();
				if(assemblyLikeRelList.hasMoreElements())
				{
					this.cadSubComponentRelationshipName = (String) assemblyLikeRelList.nextElement();
				}
			}

			this.partSpecificationRelationshipName	= MCADMxUtil.getActualNameForAEFData(context,"relationship_PartSpecification");

			this.designObjIDTemplateObjIDMap	= new Hashtable();
			this.attrRelTable					= new Hashtable();
			this.policySequenceMap				= new HashMap();
			
			this.iefCreateDesignObjectJPO		= new IEFCreateDesignObject_mxJPO(context, null);
			
			String sEBOMRegistryTNR				= globalConfigObject.getEBOMRegistryTNR();
			
			StringTokenizer token				= new StringTokenizer(sEBOMRegistryTNR, "|");
			
			if(token.countTokens() >= 3)
			{
				String sEBOMRConfigObjType			= (String) token.nextElement();
				String sEBOMRConfigObjName			= (String) token.nextElement();
				String sEBOMRConfigObjRev			= (String) token.nextElement();
				
				ebomConfObject	= new IEFEBOMConfigObject(context, sEBOMRConfigObjType, sEBOMRConfigObjName, sEBOMRConfigObjRev);
			}

			this.matchingPartString = ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_PART_MATCHING_RULE);

        }
		catch(Exception e)
        {
			System.out.println("[initialize]: Exception while initializating JPO" + e.getMessage());
            MCADServerException.createException(e.getMessage(), e);
        }
    }

    /**
     * Entry Point
     * This method is to be called for creating the structure for Create CAD Structure
     * functionality.It takes as an input the object ID of the template object which
     * is selected in the UI.Other inputs are the type, name, revision and policy
     * for the design object to be created and a map of the attribute name and values
     * which are to be set on the design object.A flag indicating whether AutoName is
     * selected for naming of object, is also passed as one of the input parameters.
     *
     * Based on the template object ID and other parameters, this object creates the
     * design object. If the template object is of assembly type,
     * structure below this template object is traversed and design objects are created
     * for each of the template objects in the structure. These design objects are then
     * connected to build the structure.
     *
     * @param context
     * @param args - a packed HashMap containing the following keys and values
     * "GCO" 				 = Global Config Object
     * "LCO" 				 = Local Config Object
     * "language" 			 = locale language name
     * "partID" 			 = Part object's ID
     * "integrationName" 	 = Integration name
     * "assemblyTemplateID"  = The assembly template's object id
     * "componentTemplateID" = The component template's object id
	 *
     * @return
     * @throws Exception
     */
	public Hashtable createStructure(Context context, String[] args) throws Exception
	{
		HashMap argumentsMap = (HashMap) JPO.unpackArgs(args);
		
		MCADGlobalConfigObject inputGCO = (MCADGlobalConfigObject) argumentsMap.get("GCO");
		MCADLocalConfigObject inputLCO = (MCADLocalConfigObject) argumentsMap.get("LCO");

        String languageName			= (String) argumentsMap.get("language");
        String partID				= (String) argumentsMap.get("partID");
		String integrationName		= (String) argumentsMap.get("integrationName");
		String assemblyTemplateID	= (String) argumentsMap.get("assemblyTemplateID");
		String componentTemplateID	= (String) argumentsMap.get("componentTemplateID");
		String folderId				= (String) argumentsMap.get("folderId");
		this.source = integrationName;		

		try
		{
			initialize(context, inputGCO, inputLCO, languageName);

			assemblyTemplateObject	= new BusinessObject(assemblyTemplateID);
			componentTemplateObject	= new BusinessObject(componentTemplateID);

			assemblyTemplateObject.open(context);
			componentTemplateObject.open(context);

			boolean isStructureCreated = traversePartStructure(context, partID, null, null, args,folderId);

			assemblyTemplateObject.close(context);
			componentTemplateObject.close(context);

			if(isStructureCreated)
				designObjIDTemplateObjIDMap.put("OPERATION_STATUS", "true");
			else
			{
				designObjIDTemplateObjIDMap.put("OPERATION_STATUS", "false");
				designObjIDTemplateObjIDMap.put("ERROR_MESSAGE", resourceBundle.getString("mcadIntegration.Server.Message.CADStructureAlreadyCreated"));
			}
		}
		catch(Exception e)
		{
			String errorMessage = e.getMessage();
			designObjIDTemplateObjIDMap.put("OPERATION_STATUS", "false");
			designObjIDTemplateObjIDMap.put("ERROR_MESSAGE", errorMessage);
		}

		return designObjIDTemplateObjIDMap;
	}

    protected boolean traversePartStructure(Context context, String partID, String parentDesignObjID, String relId, String[] arguments, String folderId) throws MCADException
    {
        boolean isStructureCreated = false;
		
		try
        {	
			BusinessObject partObject				= new BusinessObject(partID);
			partObject.open(context);
			Policy partPolicyObject  = partObject.getPolicy(context);
			partPolicyObject.open(context);
			String partPolicyName  = partPolicyObject.getName();
			String sPolicyConfiguredPart = MCADMxUtil.getActualNameForAEFData(context, "policy_ConfiguredPart");
			if(sPolicyConfiguredPart !=null && sPolicyConfiguredPart.equals(partPolicyName))
			{
				partPolicyObject.close(context);
				partObject.close(context);
				
				Hashtable messageDetails = new Hashtable(4);
				messageDetails.put("TYPE", partObject.getTypeName());
				messageDetails.put("NAME", partObject.getName());
				messageDetails.put("REV", partObject.getRevision());
				messageDetails.put("PARTPOLICY", partPolicyName);
				String message = resourceBundle.getString("mcadIntegration.Server.Message.CreateCADStructNotSupportedForPolicy", messageDetails);
				
				MCADServerException.createException(message , null);
			}
			
			Vector relIDs							= new Vector();
			Hashtable relsAndEnds					= new Hashtable();
			Hashtable relAttrNameValMap				= new Hashtable();
			relsAndEnds.put(ebomRelationshipName,"to");

			String ChildAndRelIdsString				= generalUtil.getFilteredFirstLevelChildAndRelIds(context, partID,true,relsAndEnds,null,true,null);
			BusinessObject templateObject			= componentTemplateObject;

			if(!ChildAndRelIdsString.equals(""))
				templateObject = assemblyTemplateObject;

				String cadType			= globalConfigObject.getCADTypeForTemplateType(templateObject.getTypeName());
				
			// User Parent Part's Spec, if it has one.
			String associatedDesignObjID			= getAssociatedSpecId(context,cadType, partID);

			if (associatedDesignObjID.trim().equals(""))
			{
				isStructureCreated		= true;
				associatedDesignObjID	= createDesignObject(context, arguments, partObject, templateObject, folderId);
			}
			else
			{				
				Vector majorInstanceIds  = generalUtil.getInstanceListForFamilyObject(context, associatedDesignObjID);
				if(majorInstanceIds != null && majorInstanceIds.size() > 0)
				{

					BusinessObject majorInstObject = (BusinessObject)majorInstanceIds.get(0);
					String majorInsObjID = majorInstObject.getObjectId(context);
					if(majorInsObjID != null && majorInsObjID.length() > 0)
					{
							associatedDesignObjID = majorInsObjID;
					}
				}

				relIDs		= util.findRelationShip(context, associatedDesignObjID, parentDesignObjID, true, "1", cadSubComponentRelationshipName);
			}				
			if(parentDesignObjID != null)
			{
				double partQuantity					= 1.0;
				String relModificationStatusAttName = MCADMxUtil.getActualNameForAEFData(context,"attribute_RelationshipModificationStatusinMatrix");

				relAttrNameValMap.put(relModificationStatusAttName, "new");

				Hashtable relAttrMapping		= getRelationAttributeMapping(cadSubComponentRelationshipName,ebomRelationshipName); 
				Hashtable relPartCADAttrTable	= getRelPartCADAttributeTable(context, relAttrMapping, relId);

				relAttrNameValMap.putAll(relPartCADAttrTable);

				Relationship ebomRel		= new Relationship(relId);
				ebomRel.open(context);
				Attribute ebomRelAttr		= ebomRel.getAttributeValues(context, quantityAttrName);
				String ebomQuantityStr		= ebomRelAttr.getValue();
				partQuantity				= Double.parseDouble(ebomQuantityStr);				

				BusinessObject parentPartObj = ebomRel.getFrom();
				parentPartObj.open(context);

				Vector ebomRelIDs = util.findRelationShip(context, partID, parentPartObj.getObjectId(), true, "1", ebomRelationshipName);

					if(relIDs.size() < ebomRelIDs.size() || relIDs.size() < partQuantity)
					{	
						for(double j=0.0; j<partQuantity; j++)
						{	
							isStructureCreated      = true;
							Vector parentInstanceId = generalUtil.getInstanceListForFamilyObject(context, parentDesignObjID);
							Vector childInstanceId  = generalUtil.getInstanceListForFamilyObject(context, associatedDesignObjID);

							if(parentInstanceId != null  && parentInstanceId.size() > 0)
							{
								BusinessObject parentDesignObject = (BusinessObject)parentInstanceId.get(0);
								parentDesignObjID = parentDesignObject.getObjectId(context);
							}							
							if(childInstanceId != null  && childInstanceId.size() > 0)
							{	
								BusinessObject associatedDesignObject = (BusinessObject)childInstanceId.get(0);							
								associatedDesignObjID = associatedDesignObject.getObjectId(context);
							}
							util.connectBusObjects(context, parentDesignObjID, associatedDesignObjID, cadSubComponentRelationshipName , true, relAttrNameValMap);

							String activeParentDesignId		= util.getActiveVersionObject(context, parentDesignObjID);
							String activeAssociatedDesignId = util.getActiveVersionObject(context, associatedDesignObjID);
							util.connectBusObjects(context, activeParentDesignId, activeAssociatedDesignId, cadSubComponentRelationshipName , true, relAttrNameValMap);
						}
					}

					parentPartObj.close(context);
				}

			partObject.close(context);

			StringTokenizer strTok			= new StringTokenizer(ChildAndRelIdsString,"\n");
			while(strTok.hasMoreTokens())
			{
				String row 					= strTok.nextToken();
				StringTokenizer rowElements = new StringTokenizer(row,"|");
				String level 				= rowElements.nextToken();
				String relName 				= rowElements.nextToken();
				String direction 			= rowElements.nextToken();
				String childObjectId 		= rowElements.nextToken();
				String relationshipId 		= rowElements.nextToken();
				
				traversePartStructure(context, childObjectId, associatedDesignObjID, relationshipId, arguments,folderId);
			}
        }
        catch(Exception e)
        {
            MCADServerException.createException(e.getMessage(), e);
        }

		return isStructureCreated;
    }

    protected String getAssociatedSpecId(Context context, String cadType, String PartId) throws Exception
    {
    	String specId = "";

		String rel				= MCADMxUtil.getActualNameForAEFData(context, "relationship_PartSpecification");

		String [] oids = new String[]{PartId};
		StringList selectlist = new StringList(3);
		
		selectlist.add("relationship["+rel+"].to.id");
		BusinessObjectWithSelect busWithSelect = BusinessObject.getSelectBusinessObjectData(context, oids, selectlist).getElement(0);

		StringList partSpecIdList 	= busWithSelect.getSelectDataList("relationship["+rel+"].to.id");
		
		String integObjectId					= null;
		if(partSpecIdList != null && !partSpecIdList.isEmpty())
		{
			for(int i=0 ; i<partSpecIdList.size(); i++)
				integObjectId = (String) partSpecIdList.elementAt(i);
		}

       if(globalConfigObject.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE) && integObjectId != null && !integObjectId.equals(""))
		{
		     BusinessObject busObject	 = new BusinessObject(integObjectId);
			 BusinessObject familyObject = generalUtil.getFamilyObjectForInstance(context, busObject);
			 
			 integObjectId = familyObject.getObjectId();

		}
		if(integObjectId != null && !integObjectId.equals(""))
			specId = generalUtil.getValidObjctId(context, integObjectId);		

        return specId;
    }
    
    protected String getMajorId(Context context, String minorId) throws Exception
    {
    	String majObjId = "";

		String relVersionOf = (String)PropertyUtil.getSchemaProperty(context, "relationship_VersionOf");
		String Args[] = new String[2];
		Args[0] = minorId;
		Args[1] = "from[" + relVersionOf + "].to.id";		
		String sResult	= util.executeMQL(context,"print bus $1 select $2 dump", Args);
		
		if(sResult.startsWith("true"))
			majObjId = sResult.substring(5);
                
        return majObjId;
    }

    protected String createDesignObject(Context context, String[] argumentString, BusinessObject partObject, BusinessObject templateObject,String folderId) throws MCADException
    {
        String designObjID		= "";
		String designObjId     = "";

        try
        {
			String partObjId				= partObject.getObjectId();
			String templateObjId			= templateObject.getObjectId();
			String busName					= partObject.getName();
			String cadType					= globalConfigObject.getCADTypeForTemplateType(templateObject.getTypeName());

            //get the type, policy and revision for the design object to be created
            String designObjType			= getDesignObjectType(cadType);
            String designObjPolicy			= getDesignObjectPolicy(designObjType);
            String designObjRevision		= getDesignObjectRevision(context, designObjPolicy, partObjId);			

			Hashtable typeAttrMapping		= getTypeAttributeMapping(context, designObjType, partObject.getTypeName()); 
			Hashtable typePartCADAttrTable	= getTypePartCADAttributeTable(context, typeAttrMapping, partObject);
			
			if(source.equalsIgnoreCase("MxPRO") || source.equalsIgnoreCase("MxCATIAV4"))
			{
				busName = busName.toUpperCase();
			}
			
            //create the design object corresponding to this childTemplateObject
			
			HashMap attrNameValMap = getPartAttributeNameValueMap(context, templateObject);
            
			//add "CAD Type", "Source" and FileSource attribute to this list
            attrNameValMap.put("CAD Type", cadType);
            attrNameValMap.put("Source", source);
			attrNameValMap.put("IEF-FileSource", MCADAppletServletProtocol.FILESOURCE_TEMPLATE);
            
			// Add generate new str schema
            attrNameValMap.put("Newly Created in Matrix", "TRUE");

			String mappedFormat = globalConfigObject.getFormatsForType(designObjType, cadType);
			attrNameValMap.put("Renamed From",  util.getFileNameWithoutExtnForBusID(context, templateObjId, mappedFormat));

			attrNameValMap.putAll(typePartCADAttrTable);

			Hashtable jpoArgsTable = new Hashtable();
			jpoArgsTable.put("GCO", this.globalConfigObject);
			jpoArgsTable.put("languageName", resourceBundle.getLanguageName());
			jpoArgsTable.put("templateObjID", templateObjId);
			jpoArgsTable.put("type", designObjType);
			jpoArgsTable.put("name", busName);
			jpoArgsTable.put("customRevision", designObjRevision);
			jpoArgsTable.put("policy", designObjPolicy);
			jpoArgsTable.put("autoName", "false");
			jpoArgsTable.put("autoNameSeries", "");
			jpoArgsTable.put("isRootObject", "false");
            jpoArgsTable.put("attributesMap", attrNameValMap);
            jpoArgsTable.put("folderId", folderId);
			
           // String assignPartToMajor = "true";//ebomConfObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_ASSIGN_PART_TO_MAJOR); [NDM]
			String[] args = JPO.packArgs(jpoArgsTable);
            
			String designObjDetails = iefCreateDesignObjectJPO.createDesignObject(context, args);

            if(designObjDetails.startsWith("true|"))
            {
                designObjDetails = designObjDetails.substring(5);
                designObjIDTemplateObjIDMap.put(designObjDetails, templateObjId);
            }
            else if(designObjDetails.startsWith("false|"))
            {
                MCADServerException.createException(designObjDetails.substring(6), null);
            }
			designObjId = designObjDetails.substring(0, designObjDetails.indexOf("|"));
			designObjID = designObjId;


			if(globalConfigObject.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
			{
  			    designObjID = designObjDetails.substring(designObjDetails.lastIndexOf("|")+1, designObjDetails.length());
			}
            
			String cadObjectName		= MCADMxUtil.getActualNameForAEFData(context,"attribute_CADObjectName");
			BusinessObject busObject	= new BusinessObject(partObjId);

			busObject.open(context);
			String busObjectName = busObject.getName();
			busObject.close(context);

			attrRelTable.put(cadObjectName, busObjectName);
			
				BusinessObject designObject	= new BusinessObject(designObjID);
				BusinessObject designMajorObject = util.getMajorObject(context, designObject);
				String majDesignObjID = "";
				if(null != designMajorObject){
					majDesignObjID = designMajorObject.getObjectId();
				}else{
					majDesignObjID = designObjID;
				}
				util.connectBusObjects(context, partObjId, majDesignObjID, partSpecificationRelationshipName , true, attrRelTable);

        }
        catch (Exception e)
        {
            MCADServerException.createException(e.getMessage(), e);			
        }

        return designObjId;
    }

    protected String getDesignObjectType(String cadType)
    {
        String designObjType		= "";
        Vector mappedMxTypesList	= globalConfigObject.getMappedBusTypes(cadType);

        if(mappedMxTypesList != null && mappedMxTypesList.size() > 0)
        {
            designObjType = (String)mappedMxTypesList.elementAt(0);
        }

        return designObjType;
    }

    protected String getDesignObjectPolicy(String designObjType)
    {
        String designObjPolicy	= "";
        
        Hashtable matrixTypesPrefTypesMap   = globalConfigObject.getMxTypesPrefTypesMapFromDefaultTypePolicySettingsAttr();
		String isMatrixTypeEnforced         = (String)matrixTypesPrefTypesMap.get(designObjType);
		
		Hashtable matrixTypesDefPoliciesMap = null;

		if(isMatrixTypeEnforced.equalsIgnoreCase(MCADAppletServletProtocol.ENFORCED_PREFERENCE_TYPE) || isMatrixTypeEnforced.equalsIgnoreCase(MCADAppletServletProtocol.HIDDEN_PREFERENCE_TYPE))
		{
			//Get the value from GCO 
			matrixTypesDefPoliciesMap = globalConfigObject.getMxTypesDefPolsMapFromDefaultTypePolicySettingsAttr();
		}
		else
		{
			//Get the value from LCO 
			matrixTypesDefPoliciesMap = localConfigObject.getDefaultTypePolicySettings(source);
		}

		Vector defaultPolicies = (Vector)matrixTypesDefPoliciesMap.get(designObjType);
		
		designObjPolicy = (String) defaultPolicies.elementAt(0);

        return designObjPolicy;
    }

    protected String getDesignObjectRevision(Context context, String designObjPolicy, String partObjId) throws Exception
    {
        String designObjRevision = "";

        try
        {           
			//If the revision sequence of Part and CAD Model matches, then CAD Models should be created with the revision matching as that of Part revisions.Else error will be thrown.
			if(this.matchingPartString.equals(MATCH_CADMODEL_REV))
			{
				matchRevisionSequence(context, designObjPolicy,partObjId);
	
				BusinessObject  partBusObject	= new BusinessObject(partObjId);
				partBusObject.open(context);
				
				designObjRevision = partBusObject.getRevision();
				partBusObject.close(context);
			}
			else
			{
				Policy policy = new Policy(designObjPolicy);
				policy.open(context);
				designObjRevision = policy.getFirstInSequence();
	            policy.close(context);
			}
        }
        catch(MatrixException e)
        {
            MCADServerException.createException(e.getMessage(), e);
        }

        return designObjRevision;
    }

	protected HashMap getPartAttributeNameValueMap(Context context, BusinessObject partObject) throws MCADException
	{
		HashMap attrNameValMap = new HashMap(10);

		try
		{
			AttributeList attributeList = partObject.getAttributeValues(context);
			AttributeItr attributeItr	= new AttributeItr(attributeList);
			while(attributeItr.next())
			{
				Attribute attribute		= attributeItr.obj();
				
				attrNameValMap.put(attribute.getName(), attribute.getValue());
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

		return attrNameValMap;
	}

	protected Hashtable getTypeAttributeMapping(Context context, String cadType, String partType)
    {
		Hashtable objAttrMap	= globalConfigObject.getIEFPartCADObjectAttrMapping();
        Vector cadTypesList		= getBaseTypesList(context, cadType);
        Vector partTypesList	= getBaseTypesList(context, partType);

        return getAttributeMapping(objAttrMap, cadTypesList, partTypesList);
    }
    
    protected Hashtable getRelationAttributeMapping(String cadRel, String partRel)
    {
		Hashtable relAttrMap	= globalConfigObject.getIEFPartCADRelAttrMapping();
        Vector cadRelList		= getBaseRelList(cadRel);
        Vector partRelList		= getBaseRelList(partRel);

        return getAttributeMapping(relAttrMap, cadRelList, partRelList);
    }

	protected Hashtable getAttributeMapping(Hashtable typeMapTable, Vector cadTypes, Vector partTypes)
    {
        Hashtable mappingTable = new Hashtable();
        try
        {
            Vector mappingList = new Vector();
            for(int i=0; i<partTypes.size(); i++)
            {
                String partType			= (String) partTypes.elementAt(i);
                Vector mapListForType	= (Vector)typeMapTable.get(partType);

                if(mapListForType != null)
                    mappingList.addAll(mapListForType);
            }
            
            if(mappingList != null)
            {
                for(int i=0; i<mappingList.size(); i++)
                {
                    String mappedValue		= (String) mappingList.elementAt(i);
					boolean istypeIncluded	= isTypeIncluded(cadTypes, mappedValue);

                    if(istypeIncluded)
                    {
                        StringTokenizer token	= new StringTokenizer(mappedValue, "|");
                        String partAttrName		= (String)token.nextElement();
                        String cadTypeName		= (String)token.nextElement();
                        String cadAttrName		= (String)token.nextElement();
                        
                        mappingTable.put(partAttrName, cadAttrName);
                    }
                }
            }
        }
        catch(Exception e)
        {
            System.out.println("[IEFEBOMConfigObject:getTypeAttributeMapping] Exception - " + e.getMessage());
        }
        return mappingTable;
    }

	protected Vector getBaseTypesList(Context context, String typeName)
    {
        Vector typesList = new Vector();
        try
        {
            String currTypeName = typeName;
            while(currTypeName != null && !currTypeName.trim().equals(""))
            {
                typesList.addElement(currTypeName);

                BusinessType busType	= new BusinessType(currTypeName, new Vault(""));
                currTypeName			= busType.getParent(context);
            }
        }
        catch(Exception e)
        {
        }
        return typesList;
    }
    
    protected Vector getBaseRelList(String relName)
    {
        Vector relList = new Vector();
        relList.addElement(relName);
        return relList;
    }

	protected boolean isTypeIncluded(Vector typeList, String mappingValue)
    {
        for(int i=0; i<typeList.size(); i++)
        {
            String cadType = (String) typeList.elementAt(i);
            if(mappingValue.indexOf("|"+cadType+"|") > 0)
                return true;
        }
        return false;
    }

	protected String getSystemAttributeValues(Context context, BusinessObject partObj, String attrName) throws Exception
	{
		if("$$Owner$$".equals(attrName))
			return partObj.getOwner(context).getName();
		else if("$$Description$$".equals(attrName))
			return partObj.getDescription(context);
		else
			return "";
	}

	protected Hashtable getTypePartCADAttributeTable(Context context, Hashtable partCADAttrTable, BusinessObject partObject) throws MCADException
	{
		Hashtable attrValueHash = new Hashtable();
		Enumeration partAttrEnum = partCADAttrTable.keys();
		while(partAttrEnum.hasMoreElements())
		{
			try
			{
				String partAttrName	= (String) partAttrEnum.nextElement();
				String cadAttrName	= (String) partCADAttrTable.get(partAttrName);
				String attrValue	= "";

				if(partAttrName.startsWith("$$"))
					attrValue = getSystemAttributeValues(context, partObject, partAttrName);
				//else if(attrValuesForInstance != null && attrValuesForInstance.containsKey(cadAttrName))
					//attrValue = (String) attrValuesForInstance.get(cadAttrName);
				else
					attrValue = partObject.getAttributeValues(context, partAttrName).getValue();

				attrValueHash.put(cadAttrName, attrValue);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				MCADServerException.createException(e.getMessage(), e);
			}
		}
		return attrValueHash;
	}

	protected Hashtable getRelPartCADAttributeTable(Context context, Hashtable partCADAttrTable, String relId) throws MCADException
	{
		Hashtable attrValueHash			= new Hashtable();

		if(!relId.equals(""))
		{
			Relationship EBOMRel		= new Relationship(relId);
			
			Enumeration partAttrEnum	= partCADAttrTable.keys();
			while(partAttrEnum.hasMoreElements())
			{
				try
				{
					String partAttrName	= (String) partAttrEnum.nextElement();
					String cadAttrName	= (String) partCADAttrTable.get(partAttrName);
					String attrValue	= EBOMRel.getAttributeValues(context, partAttrName).getValue();

					attrValueHash.put(cadAttrName, attrValue);
				}
				catch(Exception e)
				{
					e.printStackTrace();
					MCADServerException.createException(e.getMessage(), e);
				}
			}
		}

		return attrValueHash;
	}


	protected void matchRevisionSequence(Context context, String designObjPolicy, String partObjId) throws Exception
	{
		try
		{				
			String designObjSeq           = getPolicyRevisionSequence(context, designObjPolicy);
			
			BusinessObject  partBusObject = new BusinessObject(partObjId);
			partBusObject.open(context);
			
			Policy policy				  = partBusObject.getPolicy(context);
			policy.open(context);
	
			String partPolicyName		  = policy.getName();
			String partObjSeq             = policy.getSequence();
			
			policy.close(context);

			if(!designObjSeq.equals(partObjSeq))
			{
				Hashtable messageDetails = new Hashtable(3);
				messageDetails.put("CADPOLICY", designObjPolicy);
				messageDetails.put("PARTPOLICY", partPolicyName);

				MCADServerException.createException(resourceBundle.getString("mcadIntegration.Server.Message.RevisionSequenceCADandDevMisMatch", messageDetails) , null);
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
	}

	private String getPolicyRevisionSequence(Context context, String policyName) throws Exception
	{
		String policySequence       = "";

		if(policySequenceMap.containsKey(policyName))
		{
			policySequence          = (String)policySequenceMap.get(policyName);
		}
		else
		{
			Policy policy          = new Policy(policyName);
			policy.open(context);
			policySequence         = policy.getSequence();
			policy.close(context);
			policySequenceMap.put(policyName, policySequence);
		}

		return policySequence;
	}
}

