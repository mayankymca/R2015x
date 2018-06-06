/**
 * IEFGetTemplateObjectData
 */

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADException;

public class IEFGetTemplateObjectData_mxJPO
{
    private MCADServerResourceBundle resourceBundle           = null;

	private Hashtable templateObjIDDesignObjIDMap     = null;
    private Hashtable templateObjTNRBlankAttrList	  = null;
    private java.util.Hashtable jpoargstable					  = null;	

    public  IEFGetTemplateObjectData_mxJPO  ()
	{

    }

	public IEFGetTemplateObjectData_mxJPO (Context context, String[] args) throws Exception
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
        		this.jpoargstable = (Hashtable) JPO.unpackArgs(packedGCO);
  	this.resourceBundle     = new MCADServerResourceBundle(languageName);
        
			this.templateObjIDDesignObjIDMap	= new Hashtable();
			this.templateObjTNRBlankAttrList	= new Hashtable();
        }
		catch(Exception e)
        {
			System.out.println("[IEFGetTemplateObjectData.initialize]: Exception while initializating JPO" + e.getMessage());
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
     * args[9] = selected autoname series if AutoName is selected, blank string otherwise
     * args[10] = "true" if the selected template object is a component or the topmost assembly;
     *           "false" otherwise
     * args[11] & args[12] = packed hashtable containing actual attribute names as key and values
     *                     as value
	 * args[13] = Source details (i.e., value that is to be set on Source attribute)
	 *
     * @return
     * @throws Exception
     */
	public Hashtable createStructure(Context context, String[] args) throws Exception
	{
        String[] packedGCO = new String[2];
        packedGCO[0] = args[0];
        packedGCO[1] = args[1];

		 this.jpoargstable = (java.util.Hashtable) JPO.unpackArgs(packedGCO);
		 
		String languageName  = (String)jpoargstable.get(MCADMxUtil.getActualNameForAEFData(context, "attribute_Language"));
		
        String templateObjID = (String)jpoargstable.get("TemplateID");

		try
		{
			initialize(context, packedGCO, languageName);
			BusinessObject templateObject = new BusinessObject(templateObjID);

			templateObject.open(context);

			String startDesignJPOName = "IEFCreateObjectUsingTemplate"; //getStartDesignJPOName(templateObject);

			templateObject.close(context);

			//now invoke JPO to create design object
			String designObjDetails = (String)JPO.invoke(context, startDesignJPOName, null, "createDesignObject", args, String.class);
			if(designObjDetails.startsWith("true|"))
			{
				designObjDetails = designObjDetails.substring(5);
				templateObjIDDesignObjIDMap.put(templateObjID, designObjDetails);
			}
			else if(designObjDetails.startsWith("false|"))
			{
				//throw new MCADException(designObjDetails.substring(6));
				MCADServerException.createException(designObjDetails.substring(6), null);
			}

			if(templateObjTNRBlankAttrList.size() > 0)
			{
				String errMsg = resourceBundle.getString("mcadIntegration.Server.Message.FoundBlankValuesForMandatoryAttr") + "\\n";
				Enumeration keys = templateObjTNRBlankAttrList.keys();
				while(keys.hasMoreElements())
				{
					String templateObjTNR = (String)keys.nextElement();
					Vector blankAttrList  = (Vector)templateObjTNRBlankAttrList.get(templateObjTNR);

					errMsg += "\\n" + "Template Object: " + templateObjTNR + "\\n";
					for(int i=0; i<blankAttrList.size(); i++)
					{
						String attrName = (String)blankAttrList.elementAt(i);
						errMsg += "Attribute: " + attrName + "\\n";
					}
				}

				errMsg += "\\n" + resourceBundle.getString("mcadIntegration.Server.Message.SetValidValuesForMandatoryAttr");
				templateObjIDDesignObjIDMap.put("OPERATION_STATUS", "false");
				templateObjIDDesignObjIDMap.put("ERROR_MESSAGE", errMsg);
			}
			else
			{
				templateObjIDDesignObjIDMap.put("OPERATION_STATUS", "true");
			}
		}
		catch(Exception e)
		{
			String errorMessage = e.getMessage();
			templateObjIDDesignObjIDMap.put("OPERATION_STATUS", "false");
			templateObjIDDesignObjIDMap.put("ERROR_MESSAGE", errorMessage);
		}

		return templateObjIDDesignObjIDMap;
	}    
}

