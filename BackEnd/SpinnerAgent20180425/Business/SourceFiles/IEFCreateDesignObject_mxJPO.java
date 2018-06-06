/*
**  IEFCreateDesignObject
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  JPO for creating design objects
*/
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Vault;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.MQLCommand;

import matrix.util.StringList;


import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADFolderUtil;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

public class IEFCreateDesignObject_mxJPO
{
    private MCADMxUtil util                           = null;
    private MCADServerGeneralUtil generalUtil         = null;
    private MCADServerResourceBundle resourceBundle   = null;
	private IEFGlobalCache cache					  =	null;
    private MCADGlobalConfigObject globalConfigObject = null;

    public  IEFCreateDesignObject_mxJPO  ()
	{

    }

	public IEFCreateDesignObject_mxJPO (Context context, String[] args) throws Exception
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
    public void initialize(Context context, String languageName) throws MCADException
    {
        try
        {
            this.resourceBundle				= new MCADServerResourceBundle(languageName);
			this.cache						= new IEFGlobalCache();
            this.util						= new MCADMxUtil(context, resourceBundle, cache);
            this.generalUtil				= new MCADServerGeneralUtil(context, globalConfigObject, resourceBundle, cache);
		}
		catch(Exception e)
        {
			System.out.println("[initialize]: Exception while initializating JPO" + e.getMessage());
            MCADServerException.createException(e.getMessage(), e);
        }
    }

    /**
     * Entry Point
     * This method is to be called for creating the structure for StartDesign
     * functionality.It takes as an input the object ID of the template object which
     * is selected in the UI.Other inputs are the type, name, revision and policy
     * for the design object to be created and a map of the attribute name and values
     * which are to be set on the design object.A flag indicating whether AutoName is
     * selected for naming of object, is also passed as one of the input parameters.
     *
     * Based on the template object ID and other parameters, this object creates the
     * design object (by calling the JPO name of which is got from IEF-StartDesignJPO
     * attribute on the template object). If the template object is of assembly type,
     * structure below this template object is traversed and design objects are created
     * for each of the template objects in the structure. These design objects are then
     * connected to build the structure.
     *
     * @param context
     * @param args
	 * Key and value in the packed Hastable 
     * "GCO"			= packed Global Config Object
     * "languageName"	= locale language name
     * "templateObjID"	= Template object ID
     * "type"			= Type of the design object to be created
     * "name"			= Name of the design object to be created. If AutoName is selected, it should
     *					  be a blank string
     * "customRevision" = Revision of the design object to be created
     * "policy"			= Policy for the design object to be created
     * "autoName"		= "true" if AutoName is selected; "false" otherwise
     * "autoNameSeries" = autoname series
     * "isRootObject"	= "true" if the selected template object is a component or the topmost assembly;
     *					  "false" otherwise
     * "attributesMap"	= HashMap containing actual attribute names as key and values
     *                    as value
     *
     * @return
     * @throws Exception
     */
	public String createDesignObject(Context context, String[] args) throws MCADException
	{
        String retVal = "";

		try
		{
			Hashtable argumentsTable  = (Hashtable)JPO.unpackArgs(args);
			
			globalConfigObject		  = (MCADGlobalConfigObject)argumentsTable.get("GCO");

			String languageName       = (String)argumentsTable.get("languageName");
			String designObjType      = (String)argumentsTable.get("type");
			String designObjName      = (String)argumentsTable.get("name");
			String designObjRevision  = (String)argumentsTable.get("customRevision");
			String designObjPolicy    = (String)argumentsTable.get("policy");
			String isAutoNameSelected = (String)argumentsTable.get("autoName");
			String autonameSeries     = (String)argumentsTable.get("autoNameSeries");
			String isRootObject       = (String)argumentsTable.get("isRootObject");
			String templateObjID      = (String)argumentsTable.get("templateObjID");
			String folderId			  = (String)argumentsTable.get("folderId");
			HashMap attrNameValMap	  = (HashMap)argumentsTable.get("attributesMap");
			HashMap returnPolicyValueMap = new HashMap();
			initialize(context, languageName);
			
			String attrCADType = MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType");
			
			//get CAD Type value
			String cadType = (String)attrNameValMap.get(attrCADType);
			String instanceCADType	="";
			String integrationName  = (String)attrNameValMap.get("Source");

			//get minor type and corresponding policy
			String minorType     = designObjType;//getMinorType(context, designObjType); // [NDM] OP6
			String minorPolicy   =  util.getRelatedPolicy(context, designObjPolicy);//getMinorPolicy(minorType);  // [NDM] OP6
			String minorRevision = util.getFirstVersionStringForStream(designObjRevision);
			
			Vault vault = context.getVault();
			String vaultName = vault.getName();

			String designObjectID = "";
			BusinessObject designObject = null;
			BusinessObject instanceDesignObject = null;
			BusinessObject majorInstanceBusObj = null;

			//now based on whether AutoName is selected or not create design object in the database
			if(isAutoNameSelected.equalsIgnoreCase("true"))
			{
				String baseType             = util.getBaseType(context, designObjType);
				String symbolicBaseTypeName = util.getSymbolicName(context, "type", baseType);
				String symbolicPolicyName   = util.getSymbolicName(context, "policy", designObjPolicy);

				designObjectID = autoName(context, symbolicBaseTypeName, autonameSeries, symbolicPolicyName, vaultName, minorRevision);

				//change the type and policy to the minorType and minorPolicy
				designObject = new BusinessObject(designObjectID);
				designObject.open(context);

				String busName  = designObject.getName();
				String busRev   = designObject.getRevision();
				String busVault = designObject.getVault();

				designObject.change(context, minorType, busName, busRev, busVault, minorPolicy);
				designObjName = busName;

				designObject.close(context);
			}
			else
			{
				designObject = new BusinessObject(minorType, designObjName, minorRevision, "");

				/*Code to connect instance with family*/
				if(globalConfigObject.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
				{
					String [] oids			 = new String[1];
					oids[0]					 = templateObjID;
					String instanceOf		 = MCADMxUtil.getActualNameForAEFData(context, "relationship_InstanceOf");
					String attrTitle		 = MCADMxUtil.getActualNameForAEFData(context,"attribute_Title");
					String selectOnInstOf	 = "from[" + instanceOf + "].to";
					StringList busSelectList = new StringList(3);
		
					String selectInstOfRelId   = selectOnInstOf + ".id";
					String selectInstOfRelName = selectOnInstOf + ".name";
					String selectAttrTitle = selectOnInstOf + ".attribute[" + attrTitle + "]";

					busSelectList.add(selectInstOfRelId);
					busSelectList.add(selectInstOfRelName);
					busSelectList.add(selectAttrTitle);

					BusinessObjectWithSelectList busWithSelectList		  = BusinessObjectWithSelect.getSelectBusinessObjectData(context, oids, busSelectList);					
					BusinessObjectWithSelect busWithSelectOnRenamedObject = busWithSelectList.getElement(0);

					String clonedRelId				= (String)busWithSelectOnRenamedObject.getSelectData(selectInstOfRelId);
					String titleAttrValueonTemplate = (String)busWithSelectOnRenamedObject.getSelectData(selectAttrTitle);
					String instanceNameOfTemplate	= (String)busWithSelectOnRenamedObject.getSelectData(selectInstOfRelName);					

					//get a instance type from Family-type|Instance type mapping in GCO.
					String instanceType		   = "";				
					String cadTypeValue		   = "";	
					List instanceCadMxTypeList = getInstanceTypeFromFamily(minorType);
					instanceType			   = (String)instanceCadMxTypeList.get(0);
					cadTypeValue			   = (String)instanceCadMxTypeList.get(1);

					String baseInstanceType             = util.getBaseType(context, instanceType);
					String symbolicBaseInstanceTypeName = util.getSymbolicName(context, "type", baseInstanceType);
					String symbolicPolicyName			= util.getSymbolicName(context, "policy", minorPolicy);					

					String autonameSeriesForInstance = "B Size";					
					if(instanceNameOfTemplate != null && instanceNameOfTemplate.contains("-"))
					{
						String autoNameSeries = null;
						int indexForAutoName  = instanceNameOfTemplate.indexOf("-");
						if(indexForAutoName > 0)
						{
							autoNameSeries = instanceNameOfTemplate.substring(0,indexForAutoName);						
						}
						if(autoNameSeries != null && autoNameSeries.length() > 0)
						{
							autonameSeriesForInstance = autoNameSeries+" Size";
						}
					}


					String instanceObjectID			 = autoName(context, symbolicBaseInstanceTypeName, autonameSeriesForInstance, symbolicPolicyName, vaultName, minorRevision);
					instanceDesignObject			 = new BusinessObject(instanceObjectID);
					instanceDesignObject.open(context);

					String busInstanceName  = instanceDesignObject.getName();
					String busInstanceRev   = instanceDesignObject.getRevision();
					String busInstanceVault = instanceDesignObject.getVault();

					String minorInstancePolicy   = util.getRelatedPolicy(context, designObjPolicy);//getMinorPolicy(instanceType); //[NDM] OP6

					instanceDesignObject.change(context, instanceType, busInstanceName, busInstanceRev, busInstanceVault, minorInstancePolicy);

					String fileSourceAttrValue  = (String)attrNameValMap.get("IEF-FileSource");
					int index					= titleAttrValueonTemplate.indexOf(MCADServerSettings.INSTANCE_FAMILY_OPEN_BRACE);
					String titleAttribute		= titleAttrValueonTemplate;
					if(index > 0)
					{
						titleAttribute = titleAttrValueonTemplate.substring(0,index+1);					
						titleAttribute = titleAttribute+designObjName+MCADServerSettings.INSTANCE_FAMILY_CLOSE_BRACE;
					}

					String attrNameTitle = MCADMxUtil.getActualNameForAEFData(context, "attribute_Title");
					util.setAttributeValue(context, instanceDesignObject, attrNameTitle, titleAttribute);
					util.setAttributeValue(context, instanceDesignObject, attrCADType, cadTypeValue);

					String attrNameSource		= MCADMxUtil.getActualNameForAEFData(context, "attribute_Source");
					String sourceAttributeValue = integrationName+"|R417";
					util.setAttributeValue(context, instanceDesignObject, attrNameSource, sourceAttributeValue);

					String attrNameFilesSource		= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-FileSource");					
					util.setAttributeValue(context, instanceDesignObject, attrNameFilesSource, fileSourceAttrValue);

					String attrNameIsVersionObject		= MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
					sourceAttributeValue = "True";
					util.setAttributeValue(context, instanceDesignObject, attrNameIsVersionObject, sourceAttributeValue);
				

					util.executeMQL(context, "set env global MCADINTEGRATION_CONTEXT true");

					String majorInstanceId = generalUtil.createAndConnectToMajorObject(context, designObjRevision, true, instanceDesignObject, designObjPolicy, true, false);

					util.executeMQL(context, "unset env global MCADINTEGRATION_CONTEXT");
					instanceDesignObject.close(context);
					majorInstanceBusObj = new BusinessObject(majorInstanceId);
					
					sourceAttributeValue = "False";
					util.setAttributeValue(context, majorInstanceBusObj, attrNameIsVersionObject, sourceAttributeValue);
					util.setAttributeValue(context, majorInstanceBusObj, attrNameTitle, titleAttribute);

					if(integrationName  != null && !integrationName.equalsIgnoreCase("solidworks"))
					{
						if(folderId != null && folderId.length() > 0)
						{
							MCADFolderUtil folderUtil	= new MCADFolderUtil(context, resourceBundle, cache);
							util.setAttributeValue(context, majorInstanceBusObj, attrCADType, cadTypeValue);
							folderUtil.assignToFolder(context, majorInstanceBusObj, folderId, "false");
						}
					}
				}
				/*Code ends to connect instance with family*/

				//if(!doesObjectExist(context, designObjType, designObjName))
				if(!designObject.exists(context))
				{
					designObject.create(context, minorPolicy);
					designObject.setVault(context, vault);
				}
				else
				{
					Hashtable exceptionDetails = new Hashtable(1);
					exceptionDetails.put("NAME", designObjName);
					MCADServerException.createException(resourceBundle.getString("mcadIntegration.Server.Message.ObjectWithNameAlreadyExists", exceptionDetails), null);
				}
				designObjectID = designObject.getObjectId();
			}
			if(!designObject.isOpen())
			{
				designObject.open(context);
			}

			setAttributes(context, designObject, isRootObject, attrNameValMap);
			String attrNameSource		= MCADMxUtil.getActualNameForAEFData(context, "attribute_Source");
			String sourceAttributeValue = integrationName+"|R417";
			util.setAttributeValue(context, designObject, attrNameSource, sourceAttributeValue);


			String attrNameIsVersionObject		= MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
			String isVersionObjectAttributeValue = "True";
		
			util.setAttributeValue(context, designObject, attrNameIsVersionObject, isVersionObjectAttributeValue);
		
			//create major object and connect to minor object
		
			util.executeMQL(context, "set env global MCADINTEGRATION_CONTEXT true");			
			
			String majorDesignId  = generalUtil.createAndConnectToMajorObject(context, designObjRevision, true, designObject, designObjPolicy, true, false);
			util.executeMQL(context, "unset env global MCADINTEGRATION_CONTEXT");
		
	        BusinessObject majorDesignBusObj = new BusinessObject(majorDesignId);

			isVersionObjectAttributeValue = "False";
			util.setAttributeValue(context, majorDesignBusObj, attrNameIsVersionObject, isVersionObjectAttributeValue);
			if(folderId != null && folderId.length() > 0)
			{
				MCADFolderUtil folderUtil		 = new MCADFolderUtil(context, resourceBundle, cache);

				util.setAttributeValue(context, majorDesignBusObj, attrNameSource, sourceAttributeValue);
				folderUtil.assignToFolder(context, majorDesignBusObj, folderId, "false");
			}

			if(globalConfigObject.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
			{
				String instanceOfRelName		= MCADMxUtil.getActualNameForAEFData(context, "relationship_InstanceOf");
				String activeInstanceRelName    = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveInstance");

				//connect instance of major to family of major
				util.ConnectBusinessObjects(context, majorDesignBusObj, majorInstanceBusObj, instanceOfRelName, true);

				util.ConnectBusinessObjects(context, designObject, instanceDesignObject, instanceOfRelName, true);

				if(integrationName  != null && integrationName.equalsIgnoreCase("solidworks"))
				{
					util.ConnectBusinessObjects(context, designObject, instanceDesignObject, activeInstanceRelName, true);
				}
				instanceDesignObject.open(context);

				String instanceObjId = instanceDesignObject.getObjectId();
				String instanceName  = instanceDesignObject.getName();
				String instanceType  = instanceDesignObject.getTypeName();							
				instanceDesignObject.close(context);			

//				retVal = "true|" + instanceObjId + "|" + instanceName + "|" + instanceCADType;
				//retVal = "true|" + designObjectID + "|" + designObjName + "|" + cadType;
				retVal = "true|" + majorDesignId + "|" + designObjName + "|" + cadType + "|" + instanceObjId;
			}
			else
			{
//				retVal = "true|" + designObjectID + "|" + designObjName + "|" + cadType;
				retVal = "true|" + majorDesignId + "|" + designObjName + "|" + cadType;
			}
			if(designObject.isOpen())
			{
				designObject.close(context);
			}		
		}
		catch(Exception e)
		{
			e.printStackTrace();
			retVal = "false|" + e.getMessage();
		}		
        return retVal;
	}
	
	private boolean doesObjectExist(Context context, String designObjType, String designObjName) throws MCADException
	{
		boolean objectExists = false;
		try
		{
			String Args[] = new String[3];
			Args[0] = designObjType;
			Args[1] = designObjName;
			Args[2] = "*";
			String result = util.executeMQL(context,"temp query bus $1 $2 $3", Args);

			if(result.startsWith("true|"))
			{
				result = result.substring(5);
				if(!result.equals(""))
				{
					objectExists = true;
				}
			}
			else
			{
				MCADServerException.createException(result.substring(6), null);
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
		return objectExists;
	}

	//[NDM] OP6 .... Removed mothods
	   // private String getMinorType(Context context, String designObjType)
	  //    private String getMinorPolicy(String minorType)

    private void setAttributes(Context context, BusinessObject designObject, String isRootObject, HashMap attrNameValMap) throws MCADException
    {
		try
		{
		    String designObjectType	= designObject.getTypeName();		
			if(attrNameValMap != null && attrNameValMap.size() > 0 )
			{
				String attrTitle = MCADMxUtil.getActualNameForAEFData(context,"attribute_Title");
				AttributeList attributeList = new AttributeList();

				Iterator itr = attrNameValMap.keySet().iterator();
				while(itr.hasNext())
				{
					String attrName  = (String)itr.next();
					String attrValue = (String)attrNameValMap.get(attrName);

					//set the title attribute as blank now.It will be set to a valid value after files
					//are renamed.
					if(attrName.equals(attrTitle))
					{
						attrValue = "";
					}

					if(attrName.equals("$$Description$$"))
					{
						designObject.setDescription(attrValue);
						designObject.update(context);
					}
					else if(attrName.equals("$$Owner$$"))
					{
						designObject.setOwner(attrValue);
						designObject.update(context);
					}
					else if(attrName.equals("Description"))
					{
						//Description is a system attribute so handle it separately
						designObject.setDescription(attrValue);
						designObject.update(context);
					}
					else if(generalUtil.doesAttributeExistsOnType(context, designObjectType, attrName))
					{
						Attribute attribute = new Attribute(new AttributeType(attrName), attrValue);
						attributeList.addElement(attribute);
					}
				}
				
				//set the attributes on the object
				designObject.setAttributes(context, attributeList);
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
    }

    private String autoName(Context context, String name, String autonameSeries, String policy, String vault, String customRevision) throws MCADException
    {
        String busObjID = "";

        try
        {
            String Args[] = new String[7];
            Args[0] = "eServicecommonNumberGenerator.tcl";
            Args[1] = name;
            Args[2] = autonameSeries;
            Args[3] = policy;
            Args[4] = "Null";
            Args[5] = vault;
            Args[6] = customRevision;
            String result = util.executeMQL(context,"execute program $1 $2 $3 $4 $5 $6 $7", Args);

	  PropertyUtil.setGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME, context.getUser());

            if(result.startsWith("true|"))
            {
                result = result.substring(5);
            }
            else
            {
                MCADServerException.createException(result.substring(6), null);
            }

            if(result.length() == 0)
            {
                MCADServerException.createException(resourceBundle.getString("mcadIntegration.Server.Message.autonameGenerationFailed"), null);
            }

            StringTokenizer tokens = new StringTokenizer(result, "|", false);
            String exitCode = tokens.nextToken().trim();
            if(exitCode.equals("1"))
            {
				Hashtable messageDetails = new Hashtable();
				messageDetails.put("BUSTYPE", name);

                MCADServerException.createException(resourceBundle.getString("mcadIntegration.Server.Message.ObjectCreationError", messageDetails), null);
            }

            busObjID = tokens.nextToken().trim();
        }
        catch(Exception matrixexception)
        {
            MCADServerException.createException(matrixexception.getMessage(), matrixexception);
        }
        return busObjID;
     }

	private List getInstanceTypeFromFamily(String minorType)
	{
		List instanceCadTypeMxTypeList = new ArrayList();
		String instanceType    = "";
		String instanceCADType = "";

		if(minorType.equals("SW Component Family"))
		{
			instanceType="SW Component Instance";
			instanceCADType = "componentInstance";
		}
		if(minorType.equals("INV Part Factory"))
		{
			instanceType="INV iPart";
			instanceCADType = "componentInstance";
		}
		if(minorType.equals("INV Assembly Factory"))
		{
			instanceType="INV iAssembly";
			instanceCADType = "assemblyInstance";
		}
		if(minorType.equals("SW Assembly Family"))
		{
			instanceType="SW Assembly Instance";
			instanceCADType = "assemblyInstance";
		}
		if(minorType.equals("SE Assembly Family"))
		{
			instanceType="SE Assembly Instance";
			instanceCADType = "assemblyInstance";
		}
		if(minorType.equals("SW Assembly Family For Team"))
		{
			instanceType="SW Assembly Instance For Team";
			instanceCADType = "assemblyInstance";
		}
		if(minorType.equals("SW Component Family For Team"))
		{
			instanceType="SW Component Instance For Team";
			instanceCADType = "componentInstance";
		}
		instanceCadTypeMxTypeList.add(instanceType);
		instanceCadTypeMxTypeList.add(instanceCADType);
		return instanceCadTypeMxTypeList;
	}

}
