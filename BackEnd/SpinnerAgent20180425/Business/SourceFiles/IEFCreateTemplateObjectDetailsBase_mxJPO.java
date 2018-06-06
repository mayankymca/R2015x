/**
 ** IEFCreateTemplateObjectDetailsBase.java
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such programimport java.util.*;
 **
 **/

import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
 
public class IEFCreateTemplateObjectDetailsBase_mxJPO
{
    protected MCADMxUtil util                           = null;
    protected MCADServerResourceBundle resourceBundle   = null;
	protected IEFGlobalCache cache						= null;
    protected MCADGlobalConfigObject globalConfigObject = null;
 
    public  IEFCreateTemplateObjectDetailsBase_mxJPO  ()
 {
    }
 
 public IEFCreateTemplateObjectDetailsBase_mxJPO (Context context, String[] args) throws Exception
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
   this.cache    = new IEFGlobalCache();
            this.util               = new MCADMxUtil(context, resourceBundle, cache);
        }
  catch(Exception e)
        {
   System.out.println("[initialize]: Exception while initializating JPO" + e.getMessage());
            MCADServerException.createException(e.getMessage(), e);
        }
    }
 
 public Hashtable getTemplateObjectDetails(Context context, String[] args) throws Exception
 {
  Hashtable structureCreationData = new Hashtable();
 
  String[] packedGCO = new String[2];
        packedGCO[0] = args[0];
        packedGCO[1] = args[1];
 
        String languageName      = args[2];
        String integrationName     = args[3];
 
  initialize(context, packedGCO, languageName);
 
  if(!integrationName.equalsIgnoreCase("Microstation"))
  {
  
   Hashtable assemblyTemplateObjectsTable  = getTemplateListDataForCadType(context, "assembly", globalConfigObject, util);
   Hashtable componentTemplateObjectsTable  = getTemplateListDataForCadType(context, "component", globalConfigObject, util);
 
   Hashtable assemblyFamilyTemplateObjectsTable  = getTemplateListDataForCadType(context, "assemblyFamily", globalConfigObject, util);
   Hashtable componentFamilyTemplateObjectsTable  = getTemplateListDataForCadType(context, "componentFamily", globalConfigObject, util);

 
   assemblyTemplateObjectsTable.putAll(assemblyFamilyTemplateObjectsTable);
   componentTemplateObjectsTable.putAll(componentFamilyTemplateObjectsTable);

   structureCreationData.put("assemblyTemplateObjectsTable", assemblyTemplateObjectsTable);
   structureCreationData.put("componentTemplateObjectsTable", componentTemplateObjectsTable);
  }
  else
  {
   Hashtable assemblyTemplateObjectsTable  = getTemplateListDataForCadType(context, "design", globalConfigObject, util);
   structureCreationData.put("assemblyTemplateObjectsTable", assemblyTemplateObjectsTable);
   
   Hashtable componentTemplateObjectsTable  = getTemplateListDataForCadType(context, "design", globalConfigObject, util);
   structureCreationData.put("componentTemplateObjectsTable", componentTemplateObjectsTable);
  }
 
  return structureCreationData;
  
 }
 
 protected Hashtable getTemplateListDataForCadType(Context context, String cadType, MCADGlobalConfigObject globalConfigObject, MCADMxUtil util) throws Exception
    {
 
  Hashtable templateObjectsTable = new Hashtable();
 
        Vector mappedTemplateTypes  = globalConfigObject.getTemplateTypesForCADType(cadType);
  
        if(mappedTemplateTypes != null)
        {
            Vector templateObjectsList = doQuery(context, cadType, mappedTemplateTypes, util);
            for(int i=0; i<templateObjectsList.size(); i++)
            {
                String objectDetails = (String)templateObjectsList.elementAt(i);
                StringTokenizer tokens = new StringTokenizer(objectDetails, "|");
 
                if(tokens.hasMoreTokens())
                {
                    String type         = "";
                    String name         = "";
                    String revision     = "";
                    String objectID     = "";
                    String description  = "";
                    String cueClassName = "";
                    String filesList   = "";
 
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
 
 protected Vector doQuery(Context context, String cadType, Vector mappedTemplateTypes, MCADMxUtil util) throws MCADException
    {
        Vector templateObjectsList = new Vector();
 
        for(int i=0; i<mappedTemplateTypes.size(); i++)
        {
            String templateType = (String)mappedTemplateTypes.elementAt(i);
            String Args[] = new String[5];
            Args[0] = templateType;
            Args[1] = "*";
            Args[2] = "*";
            Args[3] = "id";
            Args[4] = "|";
            String queryResult = util.executeMQL(context,"temp query bus $1 $2 $3 select $4 description dump $5", Args);
   
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
}
