import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import matrix.db.Access;
import matrix.db.AccessList;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.Vault;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkUtil;


public class IEFCreateObjectUsingTemplate_mxJPO
{
    private MCADMxUtil util                           = null;
    private MCADServerGeneralUtil generalUtil         = null;
    private MCADServerResourceBundle resourceBundle           = null;
    private MCADGlobalConfigObject globalConfigObject = null;
	
    public  IEFCreateObjectUsingTemplate_mxJPO  ()
	{
		
    }

	public IEFCreateObjectUsingTemplate_mxJPO (Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            throw new Exception("not supported no desktop client");
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
            this.resourceBundle     = new MCADServerResourceBundle(languageName);
            this.util               = new MCADMxUtil(context, resourceBundle,new IEFGlobalCache());
            this.generalUtil        = new MCADServerGeneralUtil(context, globalConfigObject, resourceBundle,new IEFGlobalCache());
        }
		catch(Exception e)
        {
			System.out.println("[IEFCreateObjectUsingTemplate.initialize]: Exception while initializating JPO" + e.getMessage());
            //throw new MCADException(e.getMessage());
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
     * args[0] & args[1] = packed Global Config Object
     * args[2] = locale language name
     * args[3] = Template object ID
     * args[4] = Type of the design object to be created
     * args[5] = Name of the design object to be created. If AutoName is selected, it should
     *           be a blank string
     * args[6] = Revision of the design object to be created
     * args[7] = Policy for the design object to be created
     * args[8] = "true" if AutoName is selected; "false" otherwise
     * args[9] = autoname series
     * args[10] = "true" if the selected template object is a component or the topmost assembly;
     *           "false" otherwise
     * args[11] & args[12] = packed HashMap containing actual attribute names as key and values
     *                     as value
     * args[14] = parentType to which the major object will be connected
     * args[15] = parentName to which the major object will be connected
     * args[16] = parentRev to which the major object will be connected
     * args[17] = cadType
     *
     * @return
     * @throws Exception
     */
	public String createDesignObject(Context context, String[] args) throws MCADException
	{
        String retVal = "";

		try
		{
			String[] packedGCO = new String[2];
			packedGCO[0] = args[0];
			packedGCO[1] = args[1];
			java.util.Hashtable jpoargstable = (java.util.Hashtable) JPO.unpackArgs(packedGCO);
			//this.globalConfigObject = (MCADGlobalConfigObject) jpoargstable.get("GCO");
			//String languageName       = args[2];
			String languageName       = (String)jpoargstable.get("Language");
			//String templateObjID      = args[3];
			String templateObjID	  = (String)jpoargstable.get("TemplateID");
			//String designObjType      = args[4];
			String designObjType	  = (String)jpoargstable.get("Type");
			//String designObjName      = args[5];
			String designObjName	  = (String)jpoargstable.get("Name");
			//String designObjRevision  = args[6];
			String designObjRevision	  = (String)jpoargstable.get("Revision");
			//String designObjPolicy    = args[7];
			String designObjPolicy	  = (String)jpoargstable.get("Policy");
			//String isAutoNameSelected = args[8];
			String isAutoNameSelected = (String)jpoargstable.get("Autonameseries");
			//String autonameSeries     = args[9];
			String autonameSeries = (String)jpoargstable.get("Autoname");
			//String isRootObject       = args[10];
			String isRootObject = (String)jpoargstable.get("True");

			/*String[] packedAttrNameValMap = new String[2];
			packedAttrNameValMap[0] = args[11];
			packedAttrNameValMap[1] = args[12];*/
			//String parentType		= args[14];
			String parentType = (String)jpoargstable.get("Parenttype");
			//String parentName		= args[15];
			String parentName = (String)jpoargstable.get("Parentname");
			//String parentRev		= args[16];
			String parentRev = (String)jpoargstable.get("Parentrev");
			//String cadType			= args[17];
			String cadType = (String)jpoargstable.get("Cadtype");
			//String relationship		= args[18];
			String relationship = (String)jpoargstable.get("Relationship");

			HashMap _GcoTable  = (HashMap)jpoargstable.get("gcoTable");
			String integrationName=  (String)jpoargstable.get("integrationName");
			/*String[] packedAttrNameValMap = new String[2];
			packedAttrNameValMap[0] = args[11];
			packedAttrNameValMap[1] = args[12];
			String parentType		= args[14];
			String parentName		= args[15];
			String parentRev		= args[16];
			String cadType			= args[17];
			String relationship		= args[18];*/

			//HashMap attrNameValMap = (HashMap)JPO.unpackArgs(packedAttrNameValMap);

			HashMap attrNameValMap = (HashMap)jpoargstable.get("AttribMap");

			this.globalConfigObject = (MCADGlobalConfigObject) jpoargstable.get("GCO");
			initialize(context, packedGCO, languageName);

			Vault vault = context.getVault();
			String vaultName = vault.getName();

			String designObjectID = "";

			BusinessObject parentObject = null;
			String parentId         = null;

			boolean bParentExists = true;
			
			if("".equalsIgnoreCase(parentType) || parentType == null || "null".equalsIgnoreCase(parentType))
				bParentExists = false;

			if("collections".equalsIgnoreCase(parentType) && (parentRev != null && "".equalsIgnoreCase(parentRev.trim())))
				bParentExists = false;

			if(bParentExists)
			{
				parentObject = new BusinessObject(parentType, parentName, parentRev, null);
				if(parentObject.exists(context))
				{
					parentId = parentObject.getObjectId(context);
					parentObject.open(context);
					//Check if the user has checkin access            				
					AccessList list = parentObject.getAccessForGrantee(context, context.getUser());
					if(list != null && list.size() > 0)
					{
						Access access = (Access)list.get(0);
						if(access != null)
						{
							if(!access.hasCheckinAccess())
								MCADServerException.createException(resourceBundle.getString("mcadIntegration.Server.Message.NoCheckinAccessToFolder"), null);
						}
					}
					parentObject.close(context);
				}
			}

			DomainObject docHolder = null;

			if (bParentExists)
			{
				docHolder = DomainObject.newInstance(context, parentId);
					docHolder.open(context);
					//Check if the user has checkin access            				
					AccessList list = docHolder.getAccessForGrantee(context, context.getUser());
					if(list != null && list.size() > 0)
					{
						Access access = (Access)list.get(0);
						if(access != null)
						{
							if(!access.hasCheckinAccess())
								MCADServerException.createException(resourceBundle.getString("mcadIntegration.Server.Message.NoCheckinAccessToFolder"), null);
						}
					}
					docHolder.close(context);
				String symbolicTypeName = FrameworkUtil.getAliasForAdmin(context,"Type",parentType,true);

				if(symbolicTypeName == null || "null".equalsIgnoreCase(symbolicTypeName) || symbolicTypeName.length()==0)
					symbolicTypeName = parentType;

				//Read the relationship value from the GCO if it is not available from the request
				if(relationship == null || relationship.length() == 0)
				{
					HashMap jpoArgsMap = new HashMap();
					jpoArgsMap.put("type", symbolicTypeName);
					jpoArgsMap.put("gcoTable", _GcoTable);
					jpoArgsMap.put("integrationName", integrationName);
					if ( parentId != null && !"".equals(parentId) && !"null".equals(parentId) )
						jpoArgsMap.put("parentId", parentId);

					relationship = (String)JPO.invoke(context, "IEFCDMUtil", null, "getDocumentRelationship", JPO.packArgs(jpoArgsMap), String.class);
				}
                    
				if(relationship != null && relationship.startsWith("relationship_"))
					relationship = MCADMxUtil.getActualNameForAEFData(context,relationship);
			}

			if(designObjRevision!=null)
			{
				//if designObjRevision equal "next", get the first revision sequence from the policy
				if(designObjRevision.equalsIgnoreCase("next"))
				{
					matrix.db.Policy policy = new matrix.db.Policy(designObjPolicy);
					if(policy.hasSequence(context))
					{
						designObjRevision = policy.getFirstInSequence(context);
					}
					else
					{
						Hashtable table = new Hashtable();
						table.put("NAME", designObjPolicy);
						//throw new MCADException(resourceBundle.getString("mcadIntegration.Server.Message.NoSequenceFoundInPolicy",table));
						MCADServerException.createException(resourceBundle.getString("mcadIntegration.Server.Message.NoSequenceFoundInPolicy",table), null);
					}
				}
			}

			String attrTitle	= MCADMxUtil.getActualNameForAEFData(context, "attribute_Title");
			String strTitle 	= (String) attrNameValMap.get(attrTitle);
			//MSOI comment
			DomainObject object = DomainObject.newInstance(context, designObjType);
			//DomainObject object = objIEFCDMSupport.iefGetBusinessObject(context, designObjType);
			CommonDocument cmmObject = (CommonDocument)object;
			//Create a new business object and connect it with its parent
			
			HashMap argsTable = new HashMap(14);
	        argsTable.put("designObjType", designObjType);
	        argsTable.put("designObjName", designObjName);	        
	        argsTable.put("designObjRevision", designObjRevision);
	        argsTable.put("designObjPolicy", designObjPolicy);
	        argsTable.put("s1", "");
	        argsTable.put("vaultName", vaultName);
	        argsTable.put("s2", strTitle);
	        argsTable.put("s3", "");	        
	        argsTable.put("docHolder", docHolder);
	        argsTable.put("relationship", relationship);
	        argsTable.put("s4", "");
	        argsTable.put("attrNameValMap", attrNameValMap);
	        argsTable.put("domainObj", cmmObject);
	        argsTable.put("lang", languageName);
	       
	        object = (DomainObject) JPO.invoke(context, "IEFCDMSupport", null, "iefCreateAndConnect", JPO.packArgs(argsTable), DomainObject.class);
	        //object = objIEFCDMSupport.iefCreateAndConnect(context, designObjType, designObjName, designObjRevision, designObjPolicy, "", vaultName, strTitle, null, docHolder, relationship, null, (Map)attrNameValMap, cmmObject, languageName);

			StringList selects = new StringList(1);
			selects.add(DomainConstants.SELECT_ID);
			Map objectSelectMap = object.getInfo(context,selects);
			designObjectID = (String)objectSelectMap.get(DomainConstants.SELECT_ID);

			String minorObjectId = designObjectID;

			if(util.isVersionable(context, designObjectID))
			{
				Hashtable argsTable1 = new Hashtable(4);
				argsTable1.put("domainObject",object);
				argsTable1.put("s1","");
				argsTable1.put("s2","");
				argsTable1.put("attrMap",attrNameValMap);
				
				minorObjectId = (String) JPO.invoke(context, "IEFCDMSupport", null, "iefCreateVersion", JPO.packArgs(argsTable1), String.class);
			}

			if((parentType != null && "collections".equalsIgnoreCase(parentType)) 
				&& (parentRev == null || "null".equalsIgnoreCase(parentRev) || "".equalsIgnoreCase(parentRev)))
			{
				System.out.println("The container is a " + parentType + " and the name is " + parentName + ". Adding the object to the container " + parentName);
				boolean isAdded = addObjectToContainer(context, designObjectID, parentName);
				if(isAdded)
					System.out.println("Addition of the object " + designObjectID + " to the container " + parentName + " SUCCESSFUL.");
				else
					System.out.println("Addition of the object " + designObjectID + " to the container " + parentName + " FAILED.");
			}

			retVal = "true|" + designObjectID+";"+ minorObjectId + "|" + designObjName + "|" + cadType;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			retVal = "false|" + e.getMessage();
		}
		System.out.println("[IEFCreateObjectUsingTemplate.createDesignObject] retVal : " + retVal);
        return retVal;
	}

	private boolean addObjectToContainer(Context _context, String majorObjectId, String sCollectionName)
	{
        boolean retVal = false;
        
        try
        {
            String Args[] = new String[4];
            Args[0] = "set";
            Args[1] = sCollectionName;
            Args[2] = "businessobject";
            Args[3] = majorObjectId;
            
			String retString = executeThisMQL(_context, "modify $1 $2 add $3 $4",Args);
            if(retString != null && retString.startsWith("true|"))
                retVal = true;
        }
        catch(Exception e)
        {
            System.out.println("[IEFCreateObjectUsingTemplate] Exception: " + e.getMessage());
			retVal = false;
			e.printStackTrace();            
        }
        
        return retVal;
	}

    private String executeThisMQL(Context _context, String mqlCmd, String args[])
    {
        String MQLResult = "";
        try
        {
            if(_context!=  null)
            {
                MQLCommand mqlc = new MQLCommand();

                boolean bRet = mqlc.executeCommand(_context, mqlCmd, args);
                if (bRet)
                {
                    // ok
                    MQLResult = mqlc.getResult();
                    if (MQLResult == null || MQLResult.length() == 0)
                    {
                        // Handle if necessary
                    }
                    MQLResult = "true|" + MQLResult;
                }
                else
                {
                    // get error msg
                    MQLResult = mqlc.getError();
                    MQLResult = "false|" + MQLResult;
                }
                // remove extra new line character at the end of result, if any
                if(MQLResult.endsWith("\n"))
                {
                    MQLResult = MQLResult.substring(0, (MQLResult.lastIndexOf("\n")));
                }
            }
            else
            {
                MQLResult = "false|Matrix Context invalid";
            }
        }
        catch(MatrixException me)
        {
            MQLResult = "false|" + me.getMessage();
        }
        
        return MQLResult;
    }
}

