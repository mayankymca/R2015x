/**
 * IEFReplaceObjectsOptionDialog.java
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 */
 
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class IEFReplaceObjectsOptionDialog_mxJPO
{
    private MCADMxUtil util                           = null;
    private MCADServerResourceBundle resourceBundle   = null;
	private IEFGlobalCache cache					  = null;
    private MCADGlobalConfigObject globalConfigObject = null;

    public  IEFReplaceObjectsOptionDialog_mxJPO  ()
	{
    }

	public IEFReplaceObjectsOptionDialog_mxJPO (Context context, String[] args) throws Exception
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

	public Hashtable getStructureCreationData(Context context, String[] args) throws Exception
	{
		Hashtable structureCreationData = new Hashtable();

		String[] packedGCO = new String[2];
        packedGCO[0] = args[0];
        packedGCO[1] = args[1];

        String languageName					= args[2];
        String integrationName				= args[3];
		String selectedRelID				= args[4];
        String selectedObjectID				= args[5];
		String templateLabelForComponent	= args[6];
		String templateLabelForAssembly		= args[7];

		initialize(context, packedGCO, languageName);

		BusinessObject selectedBusObject = new BusinessObject(selectedObjectID);
		selectedBusObject.open(context);

		String cadType			= util.getCADTypeForBO(context, selectedBusObject);
		
		Hashtable templateObjectsTable	= null;
		String templateLabel			= "";

		if("assembly".equalsIgnoreCase(cadType))
		{
			templateObjectsTable	= getTemplateListDataForCadType(context, "component", globalConfigObject, util);
			//templateLabel			= templateLabelForAssembly;
			templateLabel			= templateLabelForComponent;
		}
		else
		{
			templateObjectsTable	= getTemplateListDataForCadType(context, "assembly", globalConfigObject, util);
			//templateLabel			= templateLabelForComponent;
			templateLabel			= templateLabelForAssembly;
		}

		String templatesComboControlString = getSelectControlString("template", templateObjectsTable);

		selectedBusObject.close(context);

		structureCreationData.put("templatesComboControlString",templatesComboControlString);
		structureCreationData.put("templateLabel",templateLabel);

		return structureCreationData;
	}

	private Hashtable getTemplateListDataForCadType(Context context, String cadType, MCADGlobalConfigObject globalConfigObject, MCADMxUtil util) throws Exception
    {
		Hashtable templateObjectsTable = new Hashtable();

        Vector mappedTemplateTypes = globalConfigObject.getTemplateTypesForCADType(cadType);
        if(mappedTemplateTypes != null)
        {
            Vector templateObjectsList = doQuery(context, cadType, mappedTemplateTypes, util);
            for(int i=0; i<templateObjectsList.size(); i++)
            {
                String objectDetails	= (String)templateObjectsList.elementAt(i);
                StringTokenizer tokens	= new StringTokenizer(objectDetails, "|");

                if(tokens.hasMoreTokens())
                {
                    String type         = "";
                    String name         = "";
                    String revision     = "";
                    String objectID     = "";
                    String description  = "";
                    String cueClassName = "";
                    String filesList  	= "";

                    if(tokens.hasMoreTokens())
                        type = tokens.nextToken();
                    if(tokens.hasMoreTokens())
                        name = tokens.nextToken();
                    if(tokens.hasMoreTokens())
                        revision = tokens.nextToken();
                    if(tokens.hasMoreTokens())
                        objectID = tokens.nextToken();
                    if(tokens.hasMoreTokens())
                        description = tokens.nextToken();

					templateObjectsTable.put(objectID, name);
                }
            }
        }

		return templateObjectsTable;
    }

	private Vector doQuery(Context context, String cadType, Vector mappedTemplateTypes, MCADMxUtil util) throws MCADException
    {
        Vector templateObjectsList = new Vector();

        for(int i=0; i<mappedTemplateTypes.size(); i++)
        {
            String templateType = (String)mappedTemplateTypes.elementAt(i);

            String Args[] = new String[6];
            Args[0] = templateType;
            Args[1] = "*";
            Args[2] = "*";
            Args[3] = "id";
            Args[4] = "description";
            Args[5] = "|";
            String queryResult = util.executeMQL(context,"temp query bus $1 $2 $3 select $4 $5 dump $6", Args);
            if(queryResult.startsWith("true|"))
            {
                StringTokenizer tokens = new StringTokenizer(queryResult.substring(5), "\n");
                while(tokens.hasMoreTokens())
                {
                    String objectDetails = tokens.nextToken();
                    templateObjectsList.addElement(objectDetails);
                }
            }
            else if(queryResult.startsWith("false|"))
            {
                MCADServerException.createException(queryResult.substring(6), null);
            }
        }

        return templateObjectsList;
    }

	private String getSelectControlString(String controlName, Hashtable optionsTable)
    {
        String returnString = "";

        if(optionsTable != null && optionsTable.size()>0)
        {
			StringBuffer selectControlBuffer	= new StringBuffer(" <select name=\"" + controlName + "\">\n");
            Enumeration optionsElements			= optionsTable.keys();

            while(optionsElements.hasMoreElements())
            {
                String optionName	= (String)optionsElements.nextElement();
				String optionValue	= (String)optionsTable.get(optionName);

                selectControlBuffer.append(" <option value=\"" + optionName + "\">" + optionValue + "</option>\n");
            }

			selectControlBuffer.append(" </select>\n");

			returnString  = selectControlBuffer.toString();
        }

        return returnString;
    }
}
