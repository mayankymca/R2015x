/*
 ** ${CLASSNAME}.java
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved.
 ** This program contains proprietary and trade secret information of
 ** Dassault Systemes.
 ** Copyright notice is precautionary only and does not evidence any actual
 ** or intended publication of such program
 */

  import matrix.db.*;
import matrix.util.*;

import java.util.*;
import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;
import com.matrixone.apps.common.*;

  public class emxPLCRDOMigrationBase_mxJPO  extends emxCommonMigration_mxJPO {

    /**
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @grade 0
     */

    public emxPLCRDOMigrationBase_mxJPO (Context context, String[] args) throws Exception {
       super(context, args);
    }
    
//    public boolean writeOID(Context context, String[] args) throws Exception
//    {
//    	String writeIdStr = writeObjectId(context, args); 
//    	if ( writeIdStr != null && !"".equals(writeIdStr) )
//        {
//        	fileWriter(writeIdStr);
//        }
//        return false;
//    }
    
    public String writeObjectId(Context context, String[] args) throws Exception
    {
    	StringList EXCLUDED_TYPES = new StringList(5);
    	String excludedTypes = "";
    	
    	try {
	    	EnoviaResourceBundle.getProperty(context, "emxCommonMigration.Exclude.Types");
	    	String[] excludedTypeArray = excludedTypes.split(",");
	    	EXCLUDED_TYPES = new StringList(excludedTypeArray.length);
	    	for (int i=0; i< excludedTypeArray.length; i++) {
	    		EXCLUDED_TYPES.addElement(PropertyUtil.getSchemaProperty(context,(String)excludedTypeArray[i]));
	        }
    	} catch(Exception ex) {
            EXCLUDED_TYPES = new StringList();
        }

    	String strObjectId = args[0];
    	String strType = args[1];

    	if (!EXCLUDED_TYPES.contains(strType)) {
    		// Assemble the correct RDO String to write to the file in this format:
    		// OBJECTID|PROJECT|ORGANIZATION
            String strProject = DomainAccess.getDefaultProject(context);
            String strOrgSelect = "to[" + DomainConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY + "].from.name";
            DomainObject domObject = new DomainObject(strObjectId);
            String strOrganization = (String)domObject.getInfo(context, strOrgSelect);

            // If RDO is blank
            //DomainObject domCompany = DomainObject.newInstance(context,Company.getHostCompany(context));
            //String strHostCompany = (String)domCompany.getInfo(context, SELECT_NAME);
            //if (strOrganization == null || "".equals(strOrganization)) {
            //	strOrganization = strHostCompany;
            //}
            
            // Assemble return string
            String returnString = strObjectId.trim() + "|" + strProject.trim() + "|" + strOrganization.trim();
            return returnString;
        } else {
        	return null;
        }
    }

    public void help(Context context, String[] args) throws Exception
    {
        if(!context.isConnected())
        {
            throw new Exception("not supported on desktop client");
        }

        writer.write("================================================================================================\n");
        writer.write(" RDO Migration is a two step process:\n");
        writer.write("================================================================================================\n");
        writer.write(" Step 1: Find the objects.\n");
        writer.write("     Find the objects based on the type (ex. Builds, Model, Products, Product Line, etc.) passed as\n");
        writer.write("     an input parameter and write them into flat files.\n\n");

        writer.write(" Example:\n");
        writer.write("     execute program emxPLCRDOMigrationFindObjects 1000 Model C:/Temp/oids/;\n\n");

        writer.write("     First parameter  = Indicates number of object per file.\n");
        writer.write("     Second Parameter = The parent type to search for.\n");
        writer.write("     Third Parameter  = The directory where files should be written.\n\n");

        writer.write(" Step 2: Migrate the objects.\n\n");

        writer.write(" Example:\n");
        writer.write("     execute program emxPLCRDOMigration 'C:/Temp/oids/' 1 n;\n\n");

        writer.write("     First parameter  = The directory to read the files from.\n");
        writer.write("     Second Parameter = Minimum range of file to start migrating.\n");
        writer.write("     Third Parameter  = Maximum range of file to end migrating.\n");
        writer.write("         (Note: Value of 'n' means all the files starting from mimimum range)\n");
        writer.write("================================================================================================\n");
        writer.close();
    }
}
