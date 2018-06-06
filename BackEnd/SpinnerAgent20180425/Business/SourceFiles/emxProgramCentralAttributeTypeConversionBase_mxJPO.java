/*
**  emxProgramCentralAttributeTypeConversionBase
**
**  Copyright (c) 1992-2015 Dassault Systems, Inc.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systems,
**  Inc. Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
*/

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectItr;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.Relationship;
import matrix.db.RelationshipWithSelect;
import matrix.db.RelationshipWithSelectItr;
import matrix.db.RelationshipWithSelectList;
import matrix.util.MatrixException;
import matrix.util.StringItr;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.RegistrationUtil;

public class emxProgramCentralAttributeTypeConversionBase_mxJPO
{
    /**
     * Enables the debug logging
     */
    private boolean debug = false;

    private final int RETURN_SUCCESS = 0;

    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args the arguments
     * @throws Exception if the operation fails
     */

    public emxProgramCentralAttributeTypeConversionBase_mxJPO (Context context, String[] args) throws Exception {
        debug (context, "Constructor called");
    }

    /**
     * Runs the conversion routing for Lag Time attribute data type conversion
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args the arguments
     * @throws MatrixException if the operation fails
     */
    public int convertLagTimeAttribute (Context context, String[] args) throws MatrixException {
        try {
            info (context, "Conversion routine started");
            
            //
            // If the coversion was already run sucessfully, then return
            //
            
            StringBuffer strMQL = new StringBuffer("print program eServiceSystemInformation.tcl select property[LagTimeAttributeTypeConversion].value dump |");
            String  strResult = MqlUtil.mqlCommand(context, strMQL.toString());
            if("Executed".equals(strResult)) {
                info (context, "The coversion was previously run sucessfully");
                return RETURN_SUCCESS;
            }
            
            ContextUtil.startTransaction (context, true);
            
            //
            // Check if the Lag Time attribute is already of type real, if
            // so then skip the conversion.
            //
            
            String strSymbolicName          = "attribute_LagTime";
            final String ATTRIBUTE_LAG_TIME = emxAdminCache_mxJPO.getName (context, strSymbolicName);
            
            AttributeType attributeType = new AttributeType(ATTRIBUTE_LAG_TIME);
            String strAttributeDataType = attributeType.getDataType(context);
            debug (context, "AttributeDataType=" + strAttributeDataType);
            
            if ("integer".equals(strAttributeDataType)) 
            {
                    String strNewDataType     = "real";
                    String strDefaultValue    = "0";
                    String strApplicationName = "ENOVIAEngineering";
                    String strInstaller       = "Framework";
                    String strBusWhere        = "attribute[" + ATTRIBUTE_LAG_TIME + "]!=0";
                    String strRelWhere        = "attribute[" + ATTRIBUTE_LAG_TIME + "]!=0";
                    
                    info (context, ">>> Conversion started for " + strSymbolicName);
                    final String ATTRIBUTE_ORIGINAL_NAME = emxAdminCache_mxJPO.getName (context, strSymbolicName);
                    final String ATTRIBUTE_TEMPORARY_NAME = "Temporary_" + strSymbolicName;

                    // Get previous registration version of the attribute
                    String strVersion = "";
                    RegistrationUtil regUtil = new RegistrationUtil();
                    Map mapAdminProperties = regUtil.getAdminProperties(context, "attribute", ATTRIBUTE_ORIGINAL_NAME);
                    if (mapAdminProperties != null) {
                        strVersion = (String)mapAdminProperties.get("version"); 
                    }

                    //
                    // Create a temp attribute
                    //
                    
                    String strNewAttributeName = ATTRIBUTE_TEMPORARY_NAME;
                    String strOldAttributeName = ATTRIBUTE_ORIGINAL_NAME;
                    
                    debug (context, "Creating attribute '" + strNewAttributeName + "'");
                    String strDescription = MqlUtil.mqlCommand (context, "print attribute \"" + strOldAttributeName + "\" select description dump");

                    //
                    // This newly created attribute will be registered with some values
                    // These will be appropriately updated later down in the routine.
                    //
                    
                    strMQL = new StringBuffer (128);
                    strMQL.append ("add attribute \"").append(strNewAttributeName).append("\" ")
                            .append("type ").append(strNewDataType).append(" ")
                            .append("description \"").append(strDescription).append("\" ")
                            .append("default \"").append(strDefaultValue).append("\"")
                            .append(" property application value ENOVIAEngineering ")
                            .append(" property version value V6R2009x ")
                            .append(" property \"original name\" value \"" + strNewAttributeName + "\" ")
                            .append(" property installer value Framework ")
                            .append(" property \"installed date\" value \"" + new java.util.Date().toString() + "\"");
                    MqlUtil.mqlCommand (context, strMQL.toString());
                    
                    //
                    // Get all the types using this attribute and add temporary attribute to it
                    //
                    
                    StringList slTypes = getAdminObjectsUsingAttribute (context, "type", ATTRIBUTE_ORIGINAL_NAME);

                    for (StringItr stringItr = new StringItr (slTypes); stringItr.next(); ) {
                        String strType = stringItr.obj();
                        
                        strMQL = new StringBuffer(128);
                        strMQL.append("modify type \"").append(strType)
                                .append("\" add attribute \"").append(ATTRIBUTE_TEMPORARY_NAME).append("\"");
                        debug (context, "Adding attribute: " + strMQL);

                        strResult = MqlUtil.mqlCommand (context, strMQL.toString());

                        debug (context, "Result:" + strResult);
                        debug (context, "Modified type \"" + strType + "\" added attribute \"" + ATTRIBUTE_TEMPORARY_NAME + "\"");
                    }
                    
                    //
                    // Find all the business objects which are affected
                    //
                    
                    String strTypePattern = FrameworkUtil.join (slTypes, ",");
                    StringList slBusSelect = new StringList (DomainObject.SELECT_ID);
                    String strVaultPattern = "*";

                    MapList mlResults = DomainObject.findObjects(context,
                                                                strTypePattern,
                                                                strVaultPattern,
                                                                strBusWhere,
                                                                slBusSelect);

                    StringList slBusObjectIds = new StringList ();
                    Map mapObjInfo = null;
                    String strObjectId = null;
                    for (Iterator itrMap = mlResults.iterator(); itrMap.hasNext(); ) {
                        mapObjInfo = (Map)itrMap.next();

                        strObjectId = (String)mapObjInfo.get (DomainObject.SELECT_ID);

                        slBusObjectIds.add (strObjectId);
                    }
                    
                    if (slBusObjectIds.size() > 0) {
                        debug (context, "Copying attribute values from '" + ATTRIBUTE_ORIGINAL_NAME + "' to '" + ATTRIBUTE_TEMPORARY_NAME + "' for " + slBusObjectIds.size() + " business objects");

                        final String SELECT_SOURCE_ATTRIBUTE = "attribute[" + ATTRIBUTE_ORIGINAL_NAME + "]";

                        String[] strObjectIds = new String[slBusObjectIds.size()];
                        strObjectIds = (String[])slBusObjectIds.toArray(strObjectIds);

                        slBusSelect = new StringList (SELECT_SOURCE_ATTRIBUTE);
                        slBusSelect.add (DomainObject.SELECT_ID);

                        BusinessObjectWithSelectList busObjWithSelectList = BusinessObject.getSelectBusinessObjectData(context,
                                                                                                                        strObjectIds,
                                                                                                                        slBusSelect);
                        BusinessObjectWithSelect busObjWithSelect = null;
                        String strAttributeValue = null;
                        strObjectId = null;
                        DomainObject dmoObject = DomainObject.newInstance (context);
                        for (BusinessObjectWithSelectItr busObjWithSelectItr = new BusinessObjectWithSelectItr(busObjWithSelectList); busObjWithSelectItr.next();) {
                            busObjWithSelect = busObjWithSelectItr.obj();

                            strObjectId = busObjWithSelect.getSelectData (DomainObject.SELECT_ID);
                            strAttributeValue = busObjWithSelect.getSelectData (SELECT_SOURCE_ATTRIBUTE);

                            //
                            // Following code is specifically return to improve performance
                            // with an assumption that data type of 'Lag Time' attribute is being 
                            // changed to real and the default value of new attribute is 0.0
                            //
                            
                            try
                            {
                                int value = Integer.parseInt(strAttributeValue);
                                if (value == 0)
                                {
                                    continue;
                                }
                            }
                            catch (NumberFormatException nfe)
                            {
                                // Dont do any things, proceed with the copy operation
                            }


                            dmoObject.setId (strObjectId);
                            dmoObject.setAttributeValue (context, ATTRIBUTE_TEMPORARY_NAME, strAttributeValue);

                            //TODO This is to be optimized to set the values in one API call if possible!
                        }
                    }// if business objects found

                    //--

                    //
                    // Get relationships using this attribute
                    // Add the temporaray attribute to them then find the affected relation ids
                    //
                    
                    StringList slRelationships = getAdminObjectsUsingAttribute (context, "relationship", ATTRIBUTE_ORIGINAL_NAME);

                    String strRelationship = null;
                    for (StringItr stringItr = new StringItr (slRelationships); stringItr.next(); ) {
                        strRelationship = stringItr.obj();
                        
                        strMQL = new StringBuffer(128);
                        strMQL.append("modify relationship \"").append(strRelationship).append("\" add attribute \"").append(ATTRIBUTE_TEMPORARY_NAME).append("\"");
                        MqlUtil.mqlCommand (context, strMQL.toString());

                        debug (context, "Modified relationship \"" + strRelationship + "\" added attribute \"" + ATTRIBUTE_TEMPORARY_NAME + "\"");
                    }
                    
                    StringList slRelationIds = new StringList();
                    String strRelationshipId = null;
                    StringList slRelationshipIds = null;
                    String strRelInfo = null;
                    StringList slRelInfo = null;

                    for (StringItr stringItr = new StringItr(slRelationships); stringItr.next(); ) {
                        strRelationshipId = stringItr.obj();
                        
                        strMQL = new StringBuffer(128);
                        strMQL.append("query connection type \"").append(strRelationshipId).append("\" ")
                                .append(" where '").append(strRelWhere).append("' select id dump ,");
                        
                        strResult = MqlUtil.mqlCommand (context, strMQL.toString());

                        // TestRel1,35648.45145.23196.65170
                        // TestRel2,35648.45145.23196.65172
                        slRelationshipIds = FrameworkUtil.split (strResult, "\n");

                        for (StringItr itrRelInfo = new StringItr (slRelationshipIds); itrRelInfo.next();) {
                            strRelInfo = itrRelInfo.obj();

                            // TestRel1,35648.45145.23196.65170
                            slRelInfo = FrameworkUtil.split(strRelInfo, ",");

                            slRelationIds.add (slRelInfo.get(1));
                        }
                    }

                    debug (context, "Relationship objects found " + slRelationIds.size());
                    
                    //
                    // Copy the original attribute value to temporary attribute on each relation found
                    //
                    
                    if (slRelationIds.size() > 0) {
                        debug (context, "Copying attribute value from '" + ATTRIBUTE_ORIGINAL_NAME + "' to '" + ATTRIBUTE_TEMPORARY_NAME + "' for " + slRelationIds.size() + " relations");

                        final String SELECT_SOURCE_ATTRIBUTE = DomainRelationship.getAttributeSelect (ATTRIBUTE_ORIGINAL_NAME);

                        String[] strRelIds = new String[slRelationIds.size()];
                        strRelIds = (String[])slRelationIds.toArray (strRelIds);

                        StringList relationshipSelects = new StringList (DomainRelationship.SELECT_ID);
                        relationshipSelects.add (SELECT_SOURCE_ATTRIBUTE);

                        RelationshipWithSelectList relWithSelectList = Relationship.getSelectRelationshipData(context,
                                                                                                             strRelIds,
                                                                                                             relationshipSelects);
                        String strRelId = null;
                        String strAttributeValue = null;
                        RelationshipWithSelect relWithSelect = null;
                        for (RelationshipWithSelectItr  relWithSelectItr = new RelationshipWithSelectItr(relWithSelectList);
                           relWithSelectItr.next();) {
                           relWithSelect = relWithSelectItr.obj();

                           strRelId = relWithSelect.getSelectData (DomainRelationship.SELECT_ID);
                           strAttributeValue = relWithSelect.getSelectData (SELECT_SOURCE_ATTRIBUTE);
                            
                            //
                            // Following code is specifically return to improve performance
                            // with an assumption that data type of 'Lag Time' attribute is being 
                            // changed to real and the default value of new attribute is 0.0
                            //
                           
                            try
                            {
                                int value = Integer.parseInt(strAttributeValue);
                                if (value == 0)
                                {
                                    continue;
                                }
                            }
                            catch (NumberFormatException nfe)
                            {
                                // Dont do any things, proceed with the copy operation
                            }

                           DomainRelationship.setAttributeValue (context, strRelId, ATTRIBUTE_TEMPORARY_NAME, strAttributeValue);
                           //TODO Needs to check if we can improve performance here by setting all the attributes in one API call!
                        }
                    }//if relations found

                    // --
                    
                    //
                    // Final processing of the attribute
                    //

                    deleteRegistration (context, ATTRIBUTE_ORIGINAL_NAME);
                    
                    // Delete original
                    MqlUtil.mqlCommand (context, "delete attribute \"" + ATTRIBUTE_ORIGINAL_NAME + "\"");
                    
                    // Rename temp attribute
                    MqlUtil.mqlCommand (context, "modify attribute \"" + ATTRIBUTE_TEMPORARY_NAME + "\" name \"" + ATTRIBUTE_ORIGINAL_NAME + "\"");

                    updateRegistration (context, ATTRIBUTE_ORIGINAL_NAME, strSymbolicName, strApplicationName, strVersion, strInstaller);
            }//if integer
            
            //
            // Set status to note that the conversion has already been run
            //
            
            MqlUtil.mqlCommand(context, "modify program eServiceSystemInformation.tcl property LagTimeAttributeTypeConversion value Executed");

            info (context, "Conversion routine finished.");

            ContextUtil.commitTransaction (context);
            
            return RETURN_SUCCESS;
        }
        catch (Exception exp) {
            ContextUtil.abortTransaction (context);
            
            info (context, "Conversion routine FAILED: " + exp.getMessage());

            exp.printStackTrace();

            throw new MatrixException ("*** Conversion FAILED: " + exp.getMessage());
        }
    }



    /**
     * Finds out the admin objects to which given attribute is immediately associated
     *
     * @param context The eMatrix Context object
     * @param strAdminObjectType This can be either 'type' or 'relationship'
     * @param strAttributeName The name of the attribute
     * @return The StringList containing the names of the admin objects found
     * @throws MatrixException if operation fails
     */
    protected StringList getAdminObjectsUsingAttribute (Context context, String strAdminObjectType, String strAttributeName) throws MatrixException {

        String strMQL = "list " + strAdminObjectType + " select name immediateattribute[" + strAttributeName + "] dump";
        String strResult = MqlUtil.mqlCommand (context, strMQL);

        StringList slResultLines = FrameworkUtil.split (strResult, "\n");
        String strResultLine = null;
        StringList slResult = null;
        String strAdminObjectName = null;
        String strIsAttributePresent = null;
        StringList slResultAdminObjects = new StringList ();

        for (StringItr stringItr = new StringItr (slResultLines); stringItr.next();) {
            strResultLine = stringItr.obj();

            slResult = FrameworkUtil.split (strResultLine, ",");

            if (slResult.size() != 2) {
                continue;
            }

            strAdminObjectName = (String)slResult.get (0);
            strIsAttributePresent = (String)slResult.get (1);

            if ("TRUE".equals (strIsAttributePresent)) {
                slResultAdminObjects.add (strAdminObjectName);
            }
        }

        return slResultAdminObjects;
    }
    
    protected void createRegistration (Context context,
                                            String strName,
                                            String strSymbolicName,
                                            String strApplicationName,
                                            String strVersion,
                                            String strInstaller) throws MatrixException {
        String strInstalledDate = new java.util.Date().toString();

        HashMap map = new HashMap();
        map.put("lstAdminType", "attribute");
        map.put("lstunregisteredadmins",strName);
        map.put("txtSymbolicName", strSymbolicName);
        map.put("txtApplication", strApplicationName);
        map.put("txtVersion", strVersion);
        map.put("txtInstaller", strInstaller);
        map.put("txtInstalledDate", strInstalledDate);
        map.put("txtOriginalName", strName);

        RegistrationUtil regUtil = new RegistrationUtil();
        regUtil.createRegistration (context, map);
        debug (context, "Created Registration for admin '" + strName + "'");
    }

    protected void updateRegistration (Context context,
                                            String strName,
                                            String strSymbolicName,
                                            String strApplicationName,
                                            String strVersion,
                                            String strInstaller) throws MatrixException {
        String strInstalledDate = new java.util.Date().toString();

        HashMap map = new HashMap();
        map.put("lstAdminType", "attribute");
        map.put("hdnregisteredadmins",strName);
        map.put("lstregisteredadmins",strName);
        map.put("txtSymbolicName", strSymbolicName);
        map.put("txtApplication", strApplicationName);
        map.put("txtVersion", strVersion);
        map.put("txtInstaller", strInstaller);
        map.put("txtInstalledDate", strInstalledDate);
        map.put("txtOriginalName", strName);

        RegistrationUtil regUtil = new RegistrationUtil();
        regUtil.updateRegistration (context, map);
        debug (context, "Updated Registration for admin '" + strName + "'");
    }

    protected void deleteRegistration (Context context, String strName) throws MatrixException {

        HashMap map = new HashMap();
        map.put("lstAdminType", "attribute");
        map.put("lstregisteredadmins",strName);
        map.put("hdnregisteredadmins",strName);

        RegistrationUtil regUtil = new RegistrationUtil();
        regUtil.deleteRegistration (context, map);
        debug (context, "Deleted Registration for admin '" + strName + "'");
    }

    /************************
     * Logging mechanism
     ************************/
    
    /**
     * Prints information message
     */
    protected void info(Context context, String strMessage) throws MatrixException {
        strMessage = new java.util.Date() + " : emxProgramCentralAttributeTypeConversion: " + strMessage;
        log (context, strMessage);
    }

    /**
     * Prints debugging message
     */ 
    protected void debug(Context context, String strMessage) throws MatrixException {
        if (debug) {
            strMessage = new java.util.Date() + " : emxProgramCentralAttributeTypeConversion: [DEBUG] " + strMessage;
            log (context, strMessage);
        }
    }
       
    /**
     * Prints raw log message
     */
    protected void log(Context context, String strMessage) throws MatrixException {
        try {
            emxInstallUtil_mxJPO.println(context, strMessage);
        }
        catch (Exception exp) {
            throw new MatrixException (exp);
        }
    }
}
