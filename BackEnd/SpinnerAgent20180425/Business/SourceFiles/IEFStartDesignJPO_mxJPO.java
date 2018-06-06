/*
**  IEFStartDesignJPO
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  JPO for creating startdesign objects
*/
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Vault;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class IEFStartDesignJPO_mxJPO
{
    private MCADMxUtil util                           = null;
    private MCADServerGeneralUtil generalUtil         = null;
    private MCADServerResourceBundle resourceBundle   = null;
	private IEFGlobalCache cache					  =	null;
    private MCADGlobalConfigObject globalConfigObject = null;

    public  IEFStartDesignJPO_mxJPO  ()
	{

    }

	public IEFStartDesignJPO_mxJPO (Context context, String[] args) throws Exception
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
    public void initialize(Context context,Hashtable argumentsTable, String languageName) throws MCADException
    {
        try
        {
            this.globalConfigObject = (MCADGlobalConfigObject) argumentsTable.get("GCO");
            this.resourceBundle     = new MCADServerResourceBundle(languageName);
			this.cache				= new IEFGlobalCache();
            this.util               = new MCADMxUtil(context, resourceBundle, cache);
            this.generalUtil        = new MCADServerGeneralUtil(context, globalConfigObject, resourceBundle, cache);
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
     * design object (by calling the JPO name of which is got from IEFStartDesignJPO
     * attribute on the template object). If the template object is of assembly type,
     * structure below this template object is traversed and design objects are created
     * for each of the template objects in the structure. These design objects are then
     * connected to build the structure.
     *
     * @param context
     * @param args
     * Key and value in the packed Hastable 
     * "GCO"            =  Global Config Object
     * "languageName"   =  locale language name
     * "templateObjID"  =  Template object ID
     * "type"           =  Type of the design object to be created
     * "name"           =  Name of the design object to be created. If AutoName is selected, it should
     *                     be a blank string
     * "customRevision" =  Revision of the design object to be created
     * "policy"         =  Policy for the design object to be created
     * "autoName"       = "true" if AutoName is selected; "false" otherwise
     * "autoNameSeries" =  selected autoname series if AutoName is selected, blank string otherwise
     * "isRootObject"   = "true" if the selected template object is a component or the topmost assembly;
     *                    "false" otherwise
     * "attributesMap"  =  hashmap containing actual attribute names as key and values
     *                     as value
	 * "source"         =  Source details (i.e., value that is to be set on Source attribute)
     *
     * @return
     * @throws Exception
     */
	public String createDesignObject(Context context, String[] args) throws MCADException
	{
        String retVal = "";

		try
		{
			Hashtable argumentsTable = (Hashtable)JPO.unpackArgs(args);

			String languageName       = (String)argumentsTable.get("languageName");
			String designObjType      = (String)argumentsTable.get("type");
			String designObjName      = (String)argumentsTable.get("name");
			String designObjRevision  = (String)argumentsTable.get("customRevision");
			String designObjPolicy    = (String)argumentsTable.get("policy");
			String isAutoNameSelected = (String)argumentsTable.get("autoName");
			String autonameSeries     = (String)argumentsTable.get("autoNameSeries");
			String isRootObject       = (String)argumentsTable.get("isRootObject");

			HashMap attrNameValMap = (HashMap)argumentsTable.get("attributesMap");
			
			initialize(context, argumentsTable, languageName);
			
			//get CAD Type value
			String cadType = (String)attrNameValMap.get("CAD Type");

			//If the design object to be created is a Family type, throw error as creation of
			//family type object through templates is not allowed
			if(globalConfigObject.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
			{
				MCADServerException.createException(resourceBundle.getString("mcadIntegration.Server.Message.CreationOfFamilyTypeObjNotAllowed"), null);
			}

			//get minor type and corresponding policy
			String minorPolicy   = util.getRelatedPolicy(context, designObjPolicy);//getMinorPolicy(minorType);  [NDM] OP6 
			String minorRevision = util.getFirstVersionStringForStream(designObjRevision);
			
			Vault vault = context.getVault();
			String vaultName = vault.getName();
			String designObjectID = "";
			BusinessObject designObject = null;

			String moveFilesToVersion = MCADMxUtil.getActualNameForAEFData(context,"attribute_MoveFilesToVersion");
			AttributeList attributeList = new AttributeList();
			attributeList.addElement(new Attribute(new AttributeType(moveFilesToVersion), "True"));
			
			//now based on whether AutoName is selected or not create design object in the database
			if(isAutoNameSelected.equalsIgnoreCase("true"))
			{
				String baseType             = util.getBaseType(context, designObjType);
				String symbolicBaseTypeName = util.getSymbolicName(context, "type", baseType);
                                Vector vctAutonames = util.getAutoNamesForCADTypes(context, autonameSeries, "type_CADModel", 1);
                                String sAutonameValue = (String)vctAutonames.firstElement();
                                BusinessObject majorObject = new BusinessObject(designObjType, sAutonameValue, designObjRevision, "");
                                majorObject.create(context, designObjPolicy);
                                majorObject.open(context);
                                majorObject.setAttributes(context, attributeList);
                                String busVault = majorObject.getVault();
                                majorObject.close(context);

				designObjName = sAutonameValue;
					util.executeMQL(context, "set env global MCADINTEGRATION_CONTEXT true");
				designObject = new BusinessObject(designObjType, designObjName, minorRevision, "");
				designObject.create(context, minorPolicy);
				util.executeMQL(context, "unset env global MCADINTEGRATION_CONTEXT");		
				designObject.setVault(context, new Vault(busVault));
				designObjectID = designObject.getObjectId(context);
			}
			else
			{			    
				designObject = new BusinessObject(designObjType, designObjName, minorRevision, "");

				if(!doesObjectExist(context, designObjType, designObjName))
				{
				    BusinessObject majorObject = new BusinessObject(designObjType, designObjName, designObjRevision, "");
				    
				    majorObject.create(context, designObjPolicy);
				    majorObject.setVault(context, vault);

				    majorObject.open(context);
					majorObject.setAttributes(context, attributeList);
					majorObject.close(context);
					
					try
					{
						util.executeMQL(context, "set env global MCADINTEGRATION_CONTEXT true");
				    designObject.create(context, minorPolicy);
					}finally
					{
						util.executeMQL(context, "unset env global MCADINTEGRATION_CONTEXT");
					}

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

			//now transfer attributes to the design object
			setAttributes(context, designObject, isRootObject, attrNameValMap);

			//Settings Value of the 'Is Versioned Object' on Minor object - put it in setAttribbutes method
			if(util.isCDMInstalled(context))
			{
				String attrIsVersionObj	 	= generalUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
				util.setAttributeValue(context, designObject, attrIsVersionObj, "True");

			}
			String modifiedAttrib	= MCADMxUtil.getActualNameForAEFData(context, "attribute_ModifiedCADAttributes");			
			//create major object and connect to minor object
			String majorBusId = generalUtil.createAndConnectToMajorObject(context, designObjRevision, true, designObject, designObjPolicy, true, false);

			BusinessObject majorObject = new BusinessObject(majorBusId);
			majorObject.open(context);
			String cadAttribValue		= (majorObject.getAttributeValues(context, "Modified CAD Attributes")).getValue();

			majorObject.close(context);
	
			AttributeList modAttrList = new AttributeList();
			modAttrList.addElement(new Attribute(new AttributeType(modifiedAttrib), cadAttribValue));
			designObject.setAttributes(context, modAttrList);			

			/*
			BusinessObject templateObject = new BusinessObject(templateObjID);
			templateObject.open(context);

			templateObject.close(context);
			*/

			if(designObject.isOpen())
			{
				designObject.close(context);
			}
			
			retVal = "true|" + designObjectID + "|" + designObjName + "|" + cadType+ "|" +majorBusId;
		}
		catch(Exception e)
		{
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
			String result = util.executeMQL(context ,"temp query bus $1 $2 $3", Args);

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
			if(attrNameValMap != null && attrNameValMap.size() > 0 )	
			{   
				String attrTitle			= MCADMxUtil.getActualNameForAEFData(context,"attribute_Title");
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

					//System.out.println("AttrName - " + attrName + " | AttrVal - " + attrValue);
					if(attrName.equals("Description"))
					{
						//Description is a system attribute so handle it separately
						designObject.setDescription(attrValue);
						designObject.update(context);
					}
					else
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
            Args[4] = "NULL";
            Args[5] = vault;
            Args[6] = customRevision;
            String result = util.executeMQL(context ,"execute program $1 $2 $3 $4 $5 $6 $7 ", Args);

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
}

