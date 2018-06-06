/**
 * IEFReplaceObjectsOption.java
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 */
 
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.FileItr;
import matrix.db.FileList;
import matrix.db.FormatItr;
import matrix.db.FormatList;
import matrix.db.JPO;
import matrix.db.Policy;
import matrix.db.Relationship;
import matrix.util.MatrixException;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class IEFReplaceObjectsOption_mxJPO
{
    private MCADMxUtil util                           = null;
    private MCADServerResourceBundle resourceBundle   = null;
	private IEFGlobalCache cache					  = null;
    private MCADGlobalConfigObject globalConfigObject = null;
	private String languageName						  = "";
	private String workingDir						  = "";

    public  IEFReplaceObjectsOption_mxJPO  ()
	{
    }

	public IEFReplaceObjectsOption_mxJPO (Context context, String[] args) throws Exception
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
    public void initialize(Context context,String[] packedGCO, String languageName) throws MCADException
    {
        try
        {
            this.globalConfigObject = (MCADGlobalConfigObject) JPO.unpackArgs(packedGCO);
            this.resourceBundle     = new MCADServerResourceBundle(languageName);
			this.cache				= new IEFGlobalCache();
            this.util               = new MCADMxUtil(context, resourceBundle, cache);
        }
		catch(Exception e)
        {
			System.out.println("[initialize]: Exception while initializating JPO" + e.getMessage());
            MCADServerException.createException(e.getMessage(), e);
        }
    }

	public void getReplaceObjectData(Context context, String[] args) throws Exception
	{
		Hashtable replaceObjectData = new Hashtable();

		String[] packedGCO = new String[2];
        packedGCO[0] = args[0];
        packedGCO[1] = args[1];

        languageName				= args[2];
        String integrationName		= args[3];
		String relID				= args[4];
        String selectedObjectID		= args[5];
		String templateObjectID     = args[6];
		workingDir					= args[7];

		initialize(context, packedGCO, languageName);
		String relationshipModificationStatusAttributeName = MCADMxUtil.getActualNameForAEFData(context,"attribute_RelationshipModificationStatusinMatrix");

		BusinessObject selectedBusObject = new BusinessObject(selectedObjectID);
		selectedBusObject.open(context);
		String selectedObjectName = selectedBusObject.getName();
		selectedBusObject.close(context);

		Relationship relObject = new Relationship(relID);

		relObject.open(context);
		util.setRelationshipAttributeValue(context, relObject, relationshipModificationStatusAttributeName, "deleted");

		BusinessObject templateObject = new BusinessObject(templateObjectID);
		templateObject.open(context);

		String designObjectID = createDesignObject(selectedObjectName, templateObject, relObject, integrationName, context, util, globalConfigObject);

		BusinessObject designObject = new BusinessObject(designObjectID);
		designObject.open(context);
		copyFileFromTemplateObject(designObject, templateObject, context, util, globalConfigObject);
		designObject.close(context);

		templateObject.close(context);
		relObject.close(context);

		replaceObjectData.put("selectedObjectName", selectedObjectName);
		replaceObjectData.put("designObjectID", designObjectID);
		replaceObjectData.put("selectedObjectName", selectedObjectName);
	}

	private String createDesignObject(String designName, BusinessObject templateObject, Relationship relObject, String integrationName, Context context, MCADMxUtil util, MCADGlobalConfigObject globalConfigObject) throws Exception
    {
        String designObjID		= "";

        String cadSubComponentRelationshipName	= MCADMxUtil.getActualNameForAEFData(context,"relationship_CADSubComponent");
		String spatialLocationAttName			= MCADMxUtil.getActualNameForAEFData(context,"attribute_SpatialLocation");
		String newTransformationMatrixAttName	= MCADMxUtil.getActualNameForAEFData(context,"attribute_NewTransformationMatrix");
		String relModificationStatusAttName		= MCADMxUtil.getActualNameForAEFData(context,"attribute_RelationshipModificationStatusinMatrix");
		String iefStartDesignJPOAttrName		= MCADMxUtil.getActualNameForAEFData(context,"attribute_IEFStartDesignJPO");

		String templateObjId	= templateObject.getObjectId();
		String cadType			= globalConfigObject.getCADTypeForTemplateType(templateObject.getTypeName());

		//get the name of the JPO for creating design object
		String startDesignJPOName = getStartDesignJPOName(templateObject, context, iefStartDesignJPOAttrName);

		//get the type, policy and revision for the design object to be created
		String designObjType     = getDesignObjectType(cadType, globalConfigObject);
		String designObjPolicy   = getDesignObjectPolicy(designObjType, globalConfigObject);
		String designObjRevision = getDesignObjectRevision(designObjPolicy, context);

		String[] packedGCO = new String[2];
		packedGCO = JPO.packArgs(globalConfigObject);

		//create the design object corresponding to this childTemplateObject
		String[] args = new String[13];
		args[0]  = packedGCO[0];
		args[1]  = packedGCO[1];
		args[2]  = languageName;
		args[3]  = templateObjId;
		args[4]  = designObjType;
		args[5]  = designName;
		args[6]  = designObjRevision;
		args[7]  = designObjPolicy;
		args[8]  = "false";
		args[9]  = "";
		args[10] = "false";

		HashMap attrNameValMap = getPartAttributeNameValueMap(templateObject, context);
		//add "CAD Type" and "Source" attribute to this list
		attrNameValMap.put("CAD Type", cadType);
		attrNameValMap.put("Source", integrationName);
		// Add generate new str schema
		 attrNameValMap.put("Newly Created in Matrix", "TRUE");
		 //String modifiedInMatrixAttrName          	= "Modified in Matrix";

		String[] packedAttrNameValMap = JPO.packArgs(attrNameValMap);
		args[11] = packedAttrNameValMap[0];
		args[12] = packedAttrNameValMap[1];

		String designObjDetails = (String)JPO.invoke(context, startDesignJPOName, null, "createDesignObject", args, String.class);
		if(designObjDetails.startsWith("true|"))
		{
			designObjDetails = designObjDetails.substring(5);
		}
		else if(designObjDetails.startsWith("false|"))
		{
			MCADServerException.createException(designObjDetails.substring(6), null);
		}

		designObjID = designObjDetails.substring(0, designObjDetails.indexOf("|"));

		BusinessObject parentDesignObject = relObject.getFrom();
		String parentDesignObjID = parentDesignObject.getObjectId();

		Hashtable relAttrNameValMap = new Hashtable();
		relAttrNameValMap.put(relModificationStatusAttName, "new");

		String spatialLocation = util.getRelationshipAttributeValue(context, relObject, spatialLocationAttName);
		relAttrNameValMap.put(newTransformationMatrixAttName, spatialLocation);

		//connect parent design object to child design object
		util.connectBusObjects(context, parentDesignObjID, designObjID, cadSubComponentRelationshipName , true, relAttrNameValMap);

        return designObjID;
    }

	private String getDesignObjectType(String cadType, MCADGlobalConfigObject globalConfigObject)
    {
        String designObjType = "";
        Vector mappedMxTypesList = globalConfigObject.getMappedBusTypes(cadType);

        if(mappedMxTypesList != null && mappedMxTypesList.size() > 0)
        {
            designObjType = (String)mappedMxTypesList.elementAt(0);
        }

        return designObjType;
    }

    private String getDesignObjectPolicy(String designObjType, MCADGlobalConfigObject globalConfigObject)
    {
        String designObjPolicy = "";
        Vector policiesList = globalConfigObject.getPolicyListForType(designObjType);

        if(policiesList != null && policiesList.size() > 0)
        {
            designObjPolicy = (String)policiesList.elementAt(0);
        }

        return designObjPolicy;
    }

    private String getDesignObjectRevision(String designObjPolicy, Context context) throws MCADException
    {
        String designObjRevision = "";

        try
        {
            Policy policy = new Policy(designObjPolicy);
            policy.open(context);
            designObjRevision = policy.getFirstInSequence();
            policy.close(context);
        }
        catch(MatrixException e)
        {
            MCADServerException.createException(e.getMessage(), e);
        }

        return designObjRevision;
    }

	private String getStartDesignJPOName(BusinessObject templateObject, Context context, String iefStartDesignJPOAttrName) throws MCADException
    {
        String startDesignJPOName = "";
        try
        {
            Attribute startDesignJPOAttr		= templateObject.getAttributeValues(context, iefStartDesignJPOAttrName);

            if(startDesignJPOAttr != null)
            {
                startDesignJPOName = startDesignJPOAttr.getValue();
            }

            if(startDesignJPOName.equals(""))
            {
                Hashtable exceptionDetails = new Hashtable(1);
                exceptionDetails.put("NAME", templateObject.getName());
                //MCADServerException.createException(integSessionData.getStringResource("mcadIntegration.Server.Message.CouldNotGetStartDesignJPOName", exceptionDetails), null);
				System.out.println("Error : CouldNotGetStartDesignJPOName");
            }
        }
        catch(Exception e)
        {
            MCADServerException.createException(e.getMessage(), e);
        }

        return startDesignJPOName;
    }

	private HashMap getPartAttributeNameValueMap(BusinessObject partObject, Context context) throws MCADException
	{
		HashMap attrNameValMap = new HashMap(10);

		try
		{
			AttributeList attributeList = partObject.getAttributeValues(context);
			AttributeItr attributeItr = new AttributeItr(attributeList);
			while(attributeItr.next())
			{
				Attribute attribute = attributeItr.obj();

				attrNameValMap.put(attribute.getName(), attribute.getValue());
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

		return attrNameValMap;
	}

	private void copyFileFromTemplateObject(BusinessObject designObj, BusinessObject templateObj, Context context, MCADMxUtil util, MCADGlobalConfigObject globalConfigObject) throws Exception
	{
		String designObjName	= designObj.getName();
		String designObjCADType	= util.getCADTypeForBO(context, designObj);
		
		//get the mapped format for the template Type from GCO	
		String mappedFormat = globalConfigObject.getFormatsForType(templateObj.getTypeName(), designObjCADType);
		//String workingDir	= integSessionData.getUserWorkingDirectory().getName();
		String attrTitle	= MCADMxUtil.getActualNameForAEFData(context,"attribute_Title");
		
		//Copy files from template Object to minor Object
		FormatList formatList = templateObj.getFormats(context);
		if (formatList != null && formatList.size() > 0)
		{	
			FormatItr formatItr = new FormatItr(formatList);
			while(formatItr.next())
			{
				String format = formatItr.obj().getName();

				if(!format.equals(mappedFormat))
				{
					continue;
				}

				//need to set the Title attribute with value as the file names
				String titleValue = "";
				FileList fileList = templateObj.getFiles(context, format);
				if (fileList.size() == 0)
				{
					//String error = integSessionData.getStringResource("mcadIntegration.Server.Message.NoFileInTemplateObject");
					String error = "NoFileInTemplateObject";
					MCADServerException.createException(error,null);
				}

				FileItr  fileItr  = new FileItr(fileList);
				while (fileItr.next())
				{
					String fileName	 = fileItr.obj().getName();
					String newFileName = "";

					//Checkout file from template object
					templateObj.checkoutFile(context,false, format, fileName, workingDir);

					java.io.File originalFile = new java.io.File(workingDir + java.io.File.separator + fileName);
														
					if(globalConfigObject.isFileRenameOnServerSide())
					{
						String actualFileExtn  = fileName.substring(fileName.lastIndexOf('.'));
						newFileName = designObjName + actualFileExtn;
					}
					else
					{
						newFileName = fileName;
					}

					java.io.File renamedFile = new java.io.File(workingDir + java.io.File.separator + newFileName);

					originalFile.renameTo(renamedFile);

					//Checkin the file into the design object
					designObj.checkinFile(context, false, true, "", format, newFileName, workingDir);

					//delete the renamed file
					renamedFile.delete();

					if(!"".equals(titleValue))
					{
						titleValue += ";";
					}

					titleValue += newFileName;
				}

				util.setAttributeOnBusObject(context, designObj, attrTitle, titleValue);
			}
		}
	}
}
