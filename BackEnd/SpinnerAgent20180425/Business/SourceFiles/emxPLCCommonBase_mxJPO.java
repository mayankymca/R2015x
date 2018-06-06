/*
 ** emxPLCCommonBase
 **
 ** Copyright (c) 1992-2015 Dassault Systemes.
 **
 ** All Rights Reserved.
 ** This program contains proprietary and trade secret information of
 ** MatrixOne, Inc.  Copyright notice is precautionary only and does
 ** not evidence any actual or intended publication of such program.
 **
 ** static const char RCSID[] = $Id: /java/JPOsrc/base/${CLASSNAME}.java 1.22.2.6.1.1.1.2.1.1 Fri Jan 16 14:09:32 2009 GMT ds-shbehera Experimental$
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessType;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.Person;
import matrix.util.StringList;


import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkLicenseUtil;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.framework.ui.UIMenu;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.productline.DerivationUtil;
import com.matrixone.apps.productline.ProductLineConstants;
import com.matrixone.apps.productline.ProductLineCommon;
import com.matrixone.apps.productline.ProductLineUtil;


    /**
     * The <code>emxPLCCommonBase</code> class contains common utility methods for Product Central application
     * @author Wipro,Enovia MatrixOne
     * @version ProductCentral 10.6 - Copyright (c) 2004, MatrixOne, Inc.
     *
     */
    public class emxPLCCommonBase_mxJPO extends emxDomainObject_mxJPO
    {
        /** A string constant with the value objectId. */
        public static final String STR_OBJECT_ID = "objectId";
        /** A string constant with the value objectList. */
        public static final String STR_OBJECT_LIST = "objectList";
        /** A string constant with the value COMMA:",". */
        public static final String STR_COMMA = ",";
        /** A string constant with the value paramList*/
        public static final String STR_DEFAULT_INTERMEDIATE_TYPES =
                         ProductLineConstants.TYPE_GBOM
                            +STR_COMMA+ProductLineConstants.TYPE_FEATURE_LIST;
        /** A string constant with the value paramList*/
        public static final String STR_PARAM_LIST = "paramList";
        /** A string constant with the value  ExpandFilterTypes. */
        public static final String STR_FILTER_TYPE_NAMES = "ExpandFilterTypes";
        /** A string constant with the value  ExpandFilterRelationships. */
        public static final String STR_FILTER_REL_NAMES = "ExpandFilterRelationships";
        /** A string constant with the value  IntermediateFilterTypes. */
        public static final String STR_INTERMEDIATE_FILTER_TYPE_NAMES =
                                                   "IntermediateFilterTypes";
        /** A string constant with the value settings */
        public static final String STR_SETTINGS = "settings";
        /** A string constant with the value "command". */
        public static final String STR_COMMAND = "command";
        /** A string constant with the value "null". */
        public static final String STR_NULL = "null";
        /** A string constant with the value "". */
        public static final String STR_BLANK = "";
        /** A string constant with the value "emxProductLineStringResource". */
        public static final String STR_BUNDLE = "emxProductLineStringResource";
        /** A string constant with the value "relationship". */
        public static final String STR_RELATIONSHIP = "relationship";
        /** A string constant with the value Boolean Compatibility Rule */
        public static final String REL_BCR = ProductLineConstants.RELATIONSHIP_BOOLEAN_COMPATIBILITY_RULE;
        /** A string constant with the value Left Expression */
        public static final String REL_LE = ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION;
        /** A string constant with the value Right Expression */
        public static final String REL_RE = ProductLineConstants.RELATIONSHIP_RIGHT_EXPRESSION;
        /*Begin of add:Ramandeep,Enovia MatrixOne Bug#300035 4/6/2005*/
        /** A string constant with the value Rule */
        public static final String REL_RULE = ProductLineConstants.RELATIONSHIP_LOCAL_RULE;
        /*end of add:Ramandeep,Enovia MatrixOne Bug#300035 4/6/2005*/
        /** A string constant with the value emxComponentsStringResource. */
        public static final String RESOURCE_BUNDLE_COMPONENTS_STR =
                "emxComponentsStringResource";
        /** A string constant for Higher Revision icon path*/
        public static final String HIGHER_REVISION_ICON =
                "<img src=\"../common/images/iconSmallHigherRevision.gif\" border=\"0\"  align=\"middle\">";
        /** A string constant for New Derivation icon path*/
        public static final String NEW_DERIVATION_ICON =
                "<img src=\"../common/images/iconSmallHigherRevision.gif\" border=\"0\"  align=\"middle\">";
        /** A string constant with the value objectList. */
        public static final String OBJECT_LIST = "objectList";
        /** A string constant with the value objectId. */
        public static final String OBJECT_ID = "objectId";
        /** A string constant for Tool Tip on Higher Revision Icon. */
        public static final String ICON_TOOLTIP_HIGHER_REVISION_EXISTS = "emxProduct.Revision.ToolTipHigherRevExists";
        /** A string constant for Tool Tip on New Derivation Icon. */
        public static final String ICON_TOOLTIP_NEW_DERIVATION_EXISTS = "emxProduct.Revision.ToolTipNewDerivationExists";
        /** A string constant for symbolic name of Relationship Affected Item */
        public static final String SYMBOLIC_relationship_ECAffectedItem = "relationship_ECAffectedItem";
        /** A string constant for symbolic name of Type EC */
        public static final String SYMBOLIC_policy_EngineeringChange = "policy_EngineeringChangeStandard";

        /** A string constant for symbolic name of state of EC */
        public static final String SYMB_state_close = "state_Close";
        /** A string constant for symbolic name of state of EC */
        public static final String SYMB_state_Reject = "state_Reject";
        /** A string constant for symbolic name of state of EC */
        public static final String SYMB_state_Complete = "state_Complete";

        /** A string constant with the value "to[Boolean Compatibility Rule].from.id"*/
        public static final String BCR_FROM_OID = "to["+REL_BCR+"]."+DomainConstants.SELECT_FROM_ID;

//Begin of Add by Enovia MatrixOne for EC Lifecycle Bug on 18-Mar-05
        /** A string constant with the value [. */
        public static final String SYMB_OPEN_BRACKET         = "[";
        /** A string constant with the value ]. */
        public static final String SYMB_CLOSE_BRACKET        = "]";
        /** A string constant with the value ==. */
        public static final String SYMB_EQUAL                = " == ";
        /** A string constant with the value attribute. */
        public static final String SYMB_ATTRIBUTE            = "attribute";
        /** A string constant with the value "'". */
        public static final String SYMB_QUOTE                = "'";
//End of Add by Enovia MatrixOne for EC Lifecycle Bug on 18-Mar-05
//Begin of add by Rashmi, Enovia MatrixOne for bug 301411 Date: 4/13/2005
         /** A string constant with the value " ". */
        public static final String SYMB_SPACE               = " ";
//End of add for bug 301411
//Begin of Add by Vibhu, Enovia MatrixOne for Bug 303269 on 28 April 05
                public static final String SYMB_OR                  ="OR";
//End of Add by Vibhu, Enovia MatrixOne for Bug 303269 on 28 April 05
        public static final String SYMBOLIC_policy_Product = "policy_Product";
        public static final String SYMB_state_Review = "state_Review";
        public static final String SYMB_state_Release = "state_Release";
        public static final String SYMB_state_Obsolete = "state_Obsolete";
        public static final String SUITE_KEY = "ProductLine";
    /**
     * Create a new emxPLCCommonBase object from a given id.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments.
     * @return a emxPLCCommonBase Object
     * @throws Exception if the operation fails
     * @since ProductCentral 10.6
     */
       public emxPLCCommonBase_mxJPO (Context context, String[] args) throws Exception
        {
            super(context, args);
        }

        /**
         * Main entry point.
         *
         * @param context the eMatrix <code>Context</code> object
         * @param args holds no arguments
         * @return an integer status code (0 = success)
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
         */
        public int mxMain (Context context, String[] args) throws Exception {
            if (!context.isConnected()) {
                String strContentLabel = EnoviaResourceBundle.getProperty(context, SUITE_KEY,
                        "emxProduct.Error.UnsupportedClient",context.getSession().getLanguage());
                throw  new Exception(strContentLabel);
            }
            return  0;
        }
    /**
     * Get the list of component objects for the object type passed in to be shown in
     * Structured Navigator expansion.The returned MapList will have component objects
     * of the object type corresponding to the node expanded in the Structure Navigator.
     * This utility method will get invoked by getStructureList method in Product Central type specific
     * JPOs when the Structured Navigator is expanding a node of object type other than the
     * type represented by the JPO.
     * Product Central type JPOs should invoke this method only when they have the method 'getStructureList'.
     * Only Product Central types whose tree menus have Structure Menu setting will invoke this method
     * For e.g. emxProductBase JPO's getStructureList method handles type Product Structure Navigator
     * expansion when a Product object is opened from Product Summary page. In the same Product
     * Structure Navigator, if object node of different type, e.g. Product Configuration is expanded,
     * the getStructureList method in emxProductBase JPO redirects the call to this method. The
     * MapList returned by this method is used to display Product Configuration object expansion in
     * the Structure Navigator.
     * @param context the eMatrix Context object.
     * @param args contains a Map with the following entries:
     *      paramMap   - Map having object Id String
     *      requestMap - Map having request parameter values
     * @return MapList with component objects of type expanded in Structure Navigator
     * @throws Exception if the operation fails
     */

    public static MapList getStructureListForType (Context context, String[] args)
        throws Exception{


        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap   = (HashMap)programMap.get("paramMap");

        String objectId    = (String)paramMap.get("objectId");

        DomainObject domainObject = DomainObject.newInstance(context, objectId);
        String objectType         = domainObject.getInfo(context, DomainObject.SELECT_TYPE);
        String objectParentType   = getParentType(context, objectType);

        // based on type of object, invoke getStructureList method of corresponding JPO
        MapList structList        = new MapList();

        try{
            String jpoClass = getJPOForType(context, objectType);

            // if JPO mapping for type does not exist, and top level parent type is different from the
            // type, try getting JPO mapping for the parent type
            if(jpoClass == null && !objectType.equals(objectParentType)){
                jpoClass = getJPOForType(context, objectParentType);
            }

            // if valid JPO obtained from Properties mapping, invoke the JPO
            if(jpoClass != null) {
                //invoke the getStructureList method on Product Central type specific JPO
                structList = (MapList)JPO.invoke(context,           // matrix Context object
                                                 jpoClass,          // the JPO
                                                 null,              // constructor arguments
                                                 "getStructureList",// method to return Structure List
                                                 args,              // args containing param map and object id
                                                 MapList.class);    // return class type
            } else {
                // else return empty list. This happens when no JPO mapping exists for the type and also its top level parent
                structList = new MapList();
            }

        }catch(Exception ex){
            throw new FrameworkException(ex);
        }

        return structList;
    }

    /**
     * This method returns the Product Central JPO corresponding to the PLC admin type passed in.
     * Every top level Product Central admin type has a JPO Base Wrapper pair like emxProduct, emxProductBase for type Products
     * The Product Central  type JPO mapping is retrieved from Properties file
     * e.g. emxProduct is returned for passed in objectType 'Products'
     * @param context     the eMatrix Context object.
     * @param objectType  the Product Central admin type for which JPO program name to be returned
     * @return String     the JPO program name corresponding to passed in Product Central admin type
     * @throws Exception  if the operation fails
     */
    public static String getJPOForType(Context context, String objectType){

        String prcJPOForType  = null;
        boolean bMissRsrcKey  = false;

        try{
            if(objectType != null && objectType.length() > 0){
                StringBuffer prcJPOTypeKey = new StringBuffer("emxProduct.JPO.");
                // lookup symbolic name for this admin type
                String symTypeName = FrameworkUtil.getAliasForAdmin(context,DomainConstants.SELECT_TYPE,objectType,true);
                prcJPOTypeKey.append(symTypeName);

                // get the corresponding Product Central type JPO from resource bundle
                prcJPOForType = EnoviaResourceBundle.getProperty(context,prcJPOTypeKey.toString());
            }
        } catch(Exception ex){
            // set true if JPO mapping for this type does not exist
            bMissRsrcKey = true;
        }
        // if no properties mapping exist or is empty
        if(bMissRsrcKey || !(prcJPOForType.length() >0)){
            prcJPOForType = null;
        }
        return prcJPOForType;
    }

    /**
     * The method returns the toplevel Parent type for passed in type
     * @param context    the eMatrix Context object.
     * @param type       the admin type for which top level parent is being queried
     * @return String    the top level parent type
     * @throws Exception if the operation fails
     */
     public static String getParentType(Context context, String type)
         throws Exception{
         String result = "";
         if(type != null){
			String strMqlCmd1 = "print type $1 select $2 dump";
			String strkindof = "kindof";
			result = MqlUtil.mqlCommand(context, strMqlCmd1, true,type,strkindof);
         }
         return result.trim();
    }
        /**
         * Get the list of the Parent objects under a context.
         *
         * @param context the eMatrix <code>Context</code> object
         * @param args is a string array containing a HashMap which
            in turn holds the following objects:
             programMap - HashMap containing the Object Id
                          and symbolicname of the command which invoked the method
         * @return Object of type MapList containing the parent objects
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
         */
        @com.matrixone.apps.framework.ui.ProgramCallable
        public static MapList getWhereUsed(Context context,String[] args)
            throws Exception {
            //Unpacking the arguments
            HashMap programMap = (HashMap)JPO.unpackArgs(args);

            //Obtaining the object id
            String strObjectId = (String)programMap.get(STR_OBJECT_ID);
            String strRelPattern=STR_BLANK;
            String strTypePattern=STR_BLANK;
            String strTypeNamesKey=STR_BLANK;
            String strRelNamesKey=STR_BLANK;
            String strIntermediateTypeNamesKey=STR_BLANK;
            String strIntermediateTypes = STR_BLANK;
            String strCommandName=STR_BLANK;
            String strDisplayRelPattern=STR_BLANK;
            StringBuffer sbTypeNames=new StringBuffer();
            StringBuffer sbRelNames=new StringBuffer();
            StringBuffer sbRelPattern=new StringBuffer();
            StringBuffer sbTypePattern=new StringBuffer();
            StringBuffer sbIntermediateTypeNames=new StringBuffer();


            //Instantiating a Maplist object that will contain the object ids
            List relObjIdList = new MapList();

            //Instantiating a Maplist object that will contain the filtered objects' ids
            List filteredMapList = new MapList();

            //get the symbolic name from the command
            String strCommandSymbolicName = (String)programMap.get(STR_COMMAND);
            //if strCommandSymbolicName is not null get the corresponding command name
            if(!( (strCommandSymbolicName == null) ||
                 (STR_BLANK.equals(strCommandSymbolicName))||
                 (STR_NULL.equals(strCommandSymbolicName))
                 )
              )
            {
              strCommandName = PropertyUtil.getSchemaProperty(context,strCommandSymbolicName);
            }

            /* if strCommandName is not null get the command settings  */
            if(! ( (strCommandName == null) ||
                  (STR_BLANK.equals(strCommandName))||
                  (STR_NULL.equals(strCommandName))
              )  )
            {
                UIMenu menu = new UIMenu();
                HashMap commandMap = menu.getCommand(context,strCommandName);
                HashMap settingsMap =(HashMap)commandMap.get(STR_SETTINGS);
                strTypeNamesKey=(String)settingsMap.get(STR_FILTER_TYPE_NAMES);
                strRelNamesKey = (String)settingsMap.get(STR_FILTER_REL_NAMES);
                strIntermediateTypeNamesKey =
                       (String)settingsMap.get(STR_INTERMEDIATE_FILTER_TYPE_NAMES);
            }
          /* if strTypeNamesKey is not null get the type pattern
             otherwise set it to a wildcard value*/
            if((strTypeNamesKey != null) &&
               (!STR_BLANK.equals(strTypeNamesKey)&&
               (!STR_NULL.equals(strTypeNamesKey))
               ))
             strTypePattern = getPattern(context,
                                         strTypeNamesKey,
                                         DomainConstants.QUERY_WILDCARD);
            else
              strTypePattern=DomainConstants.QUERY_WILDCARD;

            /* if strIntermediateTypeNamesKey is not null get
             the comma separated string containing intermediate type names
             otherwise set it to a Default value*/

            if((strIntermediateTypeNamesKey != null) &&
               (!STR_BLANK.equals(strIntermediateTypeNamesKey)&&
               (!STR_NULL.equals(strIntermediateTypeNamesKey))
               ))
                strIntermediateTypes = getPattern(context,
                                              strIntermediateTypeNamesKey,
                                              STR_DEFAULT_INTERMEDIATE_TYPES);
            else
              strIntermediateTypes = STR_DEFAULT_INTERMEDIATE_TYPES;
            /* if strRelNamesKey is not null get the relationship pattern
             otherwise set it to a wildcard value*/

             if((strRelNamesKey != null) &&
               (!STR_BLANK.equals(strRelNamesKey)&&
               (!STR_NULL.equals(strRelNamesKey))
               ))
            {
             strRelPattern = getPattern(context,
                                       strRelNamesKey,
                                       DomainConstants.QUERY_WILDCARD);
            strDisplayRelPattern =strRelPattern;
            }

            else
              strRelPattern = DomainConstants.QUERY_WILDCARD;

              /*Add the names of relationship for Intermediate type
                to the strRelPattern */
              if(!DomainConstants.QUERY_WILDCARD.equalsIgnoreCase(strRelPattern))
              {
              sbRelPattern.append(strRelPattern);
              sbRelPattern.append(STR_COMMA);
              sbRelPattern.append
                  (getIntermediateRelationships(context,strIntermediateTypes));
              strRelPattern = sbRelPattern.toString();
              }
              if(!DomainConstants.QUERY_WILDCARD.equalsIgnoreCase(strTypePattern))
              {
              sbTypePattern.append(strTypePattern);
              sbTypePattern.append(STR_COMMA);
              sbTypePattern.append(strIntermediateTypes);
              strTypePattern = sbTypePattern.toString();
              }
            //Stringlists containing the objectSelects & relationshipSelects parameters
            StringList ObjectSelectsList = new StringList();
            ObjectSelectsList.add(SELECT_ID);
            ObjectSelectsList.add(SELECT_TYPE);
            ObjectSelectsList.add(SELECT_NAME);
            ObjectSelectsList.add(BCR_FROM_OID);
                        //Begin of add by Vibhu, Enovia MatrixOne for OOC: Issue no.974 on 26 April,05
            ObjectSelectsList.add(SELECT_CURRENT);
                        //End of add by Vibhu, Enovia MatrixOne for OOC: Issue no. 974 on 26 April,05

            StringList RelSelectsList =
                new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
            RelSelectsList.add(DomainConstants.SELECT_FROM_ID);
            RelSelectsList.add(DomainConstants.SELECT_TO_ID);


   //Recurse level is set to 1
            short sRecurseLevel = 0;

//Added by Enovia MatrixOne for Performance Bug- Where used, on 23 Jan 06
    strRelPattern = removeDuplicateInString(strRelPattern, STR_COMMA);

    //Instantiating DomainObject
    DomainObject domainObject = newInstance(context,strObjectId);
             relObjIdList = domainObject.getRelatedObjects(context,
                                                          strRelPattern,
                                                          strTypePattern,
                                                          ObjectSelectsList,
                                                          RelSelectsList,
                                                          true,
                                                          false,
                                                          sRecurseLevel,
                                                          DomainConstants.EMPTY_STRING,
                                                          DomainConstants.EMPTY_STRING);


            if (relObjIdList!=null && relObjIdList.size()!=0)

            {
             /* Calling the setLevelsInMapList() method which modifies the maplist
             (relObjIdList) returned by the method getRelatedObjects().It loops
             through the maplist and searches for the intermediate objects ,if it
             finds one it decrement the "level" of the corresponding object(one
             connected through the intermediate object)by 1.

             This manipulation is needed becuase intermediate objects are supposedly
             transparent to the user.*/

            relObjIdList = setLevelsInMapList(relObjIdList,strIntermediateTypes);

            /*Filter the MapList to remove the Intermediate objects */
            relObjIdList = removeIntermediateObjects(relObjIdList,strIntermediateTypes);
            }
           /*The code below fetches the to side objects(till level 1) which are
             connected to the context object through intermediate objects */

            String strInterRel = getIntermediateRelationships(context,strIntermediateTypes);
            Map mpObjSelectMap = (HashMap)getObjectsSelect(strDisplayRelPattern);
            StringList slObjSelList = (StringList)mpObjSelectMap.get("objselects");
            /*Begin of Modify:Ramandeep,Enovia MatrixOne Bug#300035 3/11/2005*/
            StringBuffer sbBCRParentIDFromLE = new StringBuffer("to[");
            sbBCRParentIDFromLE.append(REL_LE);
            sbBCRParentIDFromLE.append("].from.to[");
            sbBCRParentIDFromLE.append(REL_BCR);
            sbBCRParentIDFromLE.append("].");
            sbBCRParentIDFromLE.append(DomainConstants.SELECT_FROM_ID);

            StringBuffer sbBCRParentIDFromRE = new StringBuffer("to[");
            sbBCRParentIDFromRE.append(REL_RE);
            sbBCRParentIDFromRE.append("].from.to[");
            sbBCRParentIDFromRE.append(REL_BCR);
            sbBCRParentIDFromRE.append("].");
            sbBCRParentIDFromRE.append(DomainConstants.SELECT_FROM_ID);


            slObjSelList.add(sbBCRParentIDFromLE.toString());
            slObjSelList.add(sbBCRParentIDFromRE.toString());
            Map mpRelName = (HashMap)mpObjSelectMap.get("relName");

            mpRelName.put(sbBCRParentIDFromLE.toString(),REL_LE);
            mpRelName.put(sbBCRParentIDFromRE.toString(),REL_RE);
            //Making another copy of Obj select list so that we keep our original selectlist with us.
            List slObjSelListCopy = new StringList();
            for(int i=0;i<slObjSelList.size();i++){
                slObjSelListCopy.add(slObjSelList.get(i));
                        }

            // Start - Modification for bug 373864
            String strBlockeTypes = EnoviaResourceBundle.getProperty(context,"emxProduct.WhereUsed.Restricted.IntermediateType");
            StringTokenizer strTypeTokenizer = new StringTokenizer(strBlockeTypes,",");
            StringList strRestrictedTypeList = new StringList(strTypeTokenizer.countTokens());

            while(strTypeTokenizer.hasMoreTokens())
            {
            	String strSymbName = (String) strTypeTokenizer.nextToken();
            	strRestrictedTypeList.add(PropertyUtil.getSchemaProperty(context,strSymbName.trim()));
            }

            MapList mlToSideObjsList = new MapList();
            String strContextObjType = domainObject.getInfo(context, ProductLineConstants.SELECT_TYPE);

            if(!strRestrictedTypeList.contains(strContextObjType))
            {
                /*Calling the getRelatedObjects() method of DomainObject
                which Returns the Maplist of Object Ids */
            	// Added if condition with logic, call domainObject.getRelatedObjects when strInterRel is not blank/null for IR-155574V6R2013 
            	if(!(strInterRel==null || strInterRel.equalsIgnoreCase("null") || strInterRel.equals("")))
            	{
                mlToSideObjsList = domainObject.getRelatedObjects(context,
                                                              strInterRel,
                                                              strTypePattern,
                                                              (StringList)slObjSelListCopy,
                                                              RelSelectsList,
                                                              false,
                                                              true,
                                                              (short)1,
                                                              DomainConstants.EMPTY_STRING,
                                                              DomainConstants.EMPTY_STRING);
            	}
            }
            // End - Modification for bug 373864

            Map mpToSideObj = new HashMap();
            Map mpToCopy = new HashMap();
            StringList slObjIdList = new StringList();
            Object objBCRParentIDFromLE = STR_BLANK;
            Object objBCRParentIDFromRE = STR_BLANK;
            for(int i=0;i<mlToSideObjsList.size();i++)
            {
                    mpToSideObj = (Map)mlToSideObjsList.get(i);
                    for(int j=0;j<slObjSelList.size();j++)
                    {
                        if (
                            ((String)slObjSelList.get(j)).equalsIgnoreCase(sbBCRParentIDFromLE.toString())||
                            ((String)slObjSelList.get(j)).equalsIgnoreCase(sbBCRParentIDFromRE.toString())
                            )
                                {
                                 continue;
                                }
                        Object objIdList = (Object)mpToSideObj.get(slObjSelList.get(j));
                        if(objIdList != null)
                        {
                           if(objIdList instanceof List)
                            {
                             slObjIdList = (StringList)objIdList;
                            }
                           else if(objIdList instanceof String)
                            {
                             slObjIdList.addElement((String)objIdList);
                            }
                            for(int k = 0;k < slObjIdList.size();k++)
                            {
                                mpToCopy = new HashMap();
                                //create a map and copy it to the maplist to be returned
                                mpToCopy.put(SELECT_ID,slObjIdList.get(k));
                                mpToCopy.put(DomainConstants.SELECT_FROM_ID,slObjIdList.get(k));
                                mpToCopy.put(DomainConstants.SELECT_TO_ID,strObjectId);
                                mpToCopy.put(DomainConstants.KEY_LEVEL,(String)mpToSideObj.get(DomainConstants.KEY_LEVEL));
                                mpToCopy.put(STR_RELATIONSHIP,(String)mpRelName.get(slObjSelList.get(j)));

                                /*Begin of add:Vibhu,Enovia MatrixOne Bug#300051 3/29/2005*/
                                DomainObject objTemp = DomainObject.newInstance(context,(String)slObjIdList.get(k));
                                String strType = objTemp.getInfo(context,DomainConstants.SELECT_TYPE);
                                mpToCopy.put(SELECT_TYPE,strType);
                                
                                String strName = objTemp.getInfo(context, DomainConstants.SELECT_NAME);
                                mpToCopy.put(SELECT_NAME, strName);
                                
                                /*End of add:Vibhu,Enovia MatrixOne Bug#300051 3/29/2005*/

                                                                //Begin of add by Vibhu, Enovia MatrixOne for Issue no.974 on 26 April,05
                                String strStateCurrent = objTemp.getInfo(context,SELECT_CURRENT);
                                                                mpToCopy.put(SELECT_CURRENT,strStateCurrent);
                                                                //End of add by Vibhu, Enovia MatrixOne for Issue no. 974 on 26 April,05

                                objBCRParentIDFromLE = (Object)mpToSideObj.get(sbBCRParentIDFromLE.toString());
                                 if (objBCRParentIDFromLE!=null)
                                  {

                                      if(objBCRParentIDFromLE instanceof List)
                                        {
                                         mpToCopy.put(BCR_FROM_OID,((StringList)objBCRParentIDFromLE).get(0));
                                        }
                                       else if(objBCRParentIDFromLE instanceof String)


                                        {
                                         mpToCopy.put(BCR_FROM_OID,(String)objBCRParentIDFromLE);
                                        }
                                  }

                                objBCRParentIDFromRE = (Object)mpToSideObj.get(sbBCRParentIDFromRE.toString());
                                  if (objBCRParentIDFromRE!=null)
                                  {

                                      if(objBCRParentIDFromRE instanceof List)
                                        {
                                         mpToCopy.put(BCR_FROM_OID,((StringList)objBCRParentIDFromRE).get(0));
                                        }
                                       else if(objBCRParentIDFromRE instanceof String)


                                        {
                                         mpToCopy.put(BCR_FROM_OID,(String)objBCRParentIDFromRE);
                                        }
                         /*End of Modify:Ramandeep,Enovia MatrixOne Bug#300035 3/11/2005*/
                                  }
                                  relObjIdList.add(mpToCopy);
                             }//end for
                        }//end if
                    }//end for
            }//end for
        if (relObjIdList!=null && relObjIdList.size()!=0)
        {
            /*Filter the MapList to remove the Duplicate objects */
             relObjIdList = removeDuplicateObjects(context,relObjIdList);
        }
	Iterator objectListItr = relObjIdList.iterator();
        Map objectMap = new HashMap();
        //loop through all the records
        while(objectListItr.hasNext())
        {
            objectMap = (Map) objectListItr.next();
             String strLevel = (String)objectMap.get(DomainConstants.KEY_LEVEL);
             objectMap.put("sLevel", strLevel);
        } //End of while loop
 return  (MapList)relObjIdList ;
}//End of the method

        /**
         * This method creates the HTML to display the Edit Icon
         * @param context the eMatrix <code>Context</code> object
         * @param args - Holds the parameters passed from the calling method
         * @return Vector
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
         **/
    public static Vector getEditIcon(Context context,String[] args) throws Exception
        {
    	        //XSSOK- Deprecated
                /*Begin of add:Ramandeep,Enovia MatrixOne Bug#300035 3/11/2005*/
                StringBuffer sbRuleTargetId = new StringBuffer("from[");
                sbRuleTargetId.append(REL_LE);
                sbRuleTargetId.append("].to.from[");
                sbRuleTargetId.append(ProductLineConstants.RELATIONSHIP_FEATURE_LIST_TO);
                sbRuleTargetId.append("].");
                sbRuleTargetId.append(DomainConstants.SELECT_ID);

                StringBuffer sbRuleLERelId = new StringBuffer("from[");
                sbRuleLERelId.append(REL_LE);
                sbRuleLERelId.append("].to.from[");
                sbRuleLERelId.append(ProductLineConstants.RELATIONSHIP_FEATURE_LIST_TO);
                sbRuleLERelId.append("].");
                sbRuleLERelId.append(DomainConstants.SELECT_TO_ID);

                StringBuffer sbIRProductId = new StringBuffer("to[");
                sbIRProductId.append(REL_RULE);
                sbIRProductId.append("].");
                sbIRProductId.append(DomainConstants.SELECT_FROM_ID);

                StringBuffer sbIRParentId = new StringBuffer("from[");
                sbIRParentId.append(REL_LE);
                sbIRParentId.append("].to.to[");
                sbIRParentId.append(ProductLineConstants.RELATIONSHIP_FEATURE_LIST_FROM);
                sbIRParentId.append("].");
                sbIRParentId.append(DomainConstants.SELECT_FROM_ID);

                StringBuffer sbGBOMRuleLERelId = new StringBuffer("from[");
                sbGBOMRuleLERelId.append(REL_LE);
                sbGBOMRuleLERelId.append("].to.from[");
                sbGBOMRuleLERelId.append(ProductLineConstants.RELATIONSHIP_GBOM_TO);
                sbGBOMRuleLERelId.append("].");
                sbGBOMRuleLERelId.append(DomainConstants.SELECT_TO_ID);

                StringBuffer sbGBOMIRParentId = new StringBuffer("from[");
                sbGBOMIRParentId.append(REL_LE);
                sbGBOMIRParentId.append("].to.to[");
                sbGBOMIRParentId.append(ProductLineConstants.RELATIONSHIP_GBOM_FROM);
                sbGBOMIRParentId.append("].");
                sbGBOMIRParentId.append(DomainConstants.SELECT_FROM_ID);

                StringBuffer sbGBOMRuleTargetId = new StringBuffer("from[");
                sbGBOMRuleTargetId.append(REL_LE);
                sbGBOMRuleTargetId.append("].to.from[");
                sbGBOMRuleTargetId.append(ProductLineConstants.RELATIONSHIP_GBOM_TO);
                sbGBOMRuleTargetId.append("].");
                sbGBOMRuleTargetId.append(DomainConstants.SELECT_ID);

                /*end of add:Ramandeep,Enovia MatrixOne Bug#300035 3/11/2005*/

                Vector editIconVector = new Vector();
                Map programMap = (Map) JPO.unpackArgs(args);
                List objectList = (MapList)programMap.get(STR_OBJECT_LIST);
                Map paramMap = (Map) programMap.get(STR_PARAM_LIST);

                //strReportFormat indicates whether method is called from table or report
                String strReportFormat=(String)paramMap.get("reportFormat");
                Iterator objectListItr = objectList.iterator();
                String strType = STR_BLANK;
                String strBaseType = STR_BLANK;
                String strSymbolicTypeName = STR_BLANK;
                String strId = STR_BLANK;
                String strRelId = STR_BLANK;
                /*Begin of Modify:Ramandeep,Enovia MatrixOne Bug#300035 3/11/2005*/
                String strBCRParentId = STR_BLANK;
                /*End of Modify:Ramandeep,Enovia MatrixOne Bug#300035 3/11/2005*/
                String strWebForm = STR_BLANK;
                String strFormName = STR_BLANK;
                String strCommand = STR_BLANK;
                String strImageSource = "<img border='0' src='../common/images/iconActionEdit.gif'>";
                String strIssueType = PropertyUtil.getSchemaProperty
                                             (context,"type_Issue");
                String strTypeBCR = PropertyUtil.getSchemaProperty
                                             (context,"type_BooleanCompatibilityRule");
                String strTypePCR = PropertyUtil.getSchemaProperty
                                             (context,"type_ProductCompatibilityRule");
                String strTypeInclusionRule = PropertyUtil.getSchemaProperty
                                              (context,"type_InclusionRule");
                String strTypePart = PropertyUtil.getSchemaProperty
                                             (context,"type_Part");
                String strHrefPart = "../components/emxCommonFS.jsp?functionality=PartEditFSInstance&PRCFSParam1=Part&suiteKey=ProductLine";
                String strHrefFeatureBCR = "../components/emxCommonFS.jsp?functionality=BooleanCompatibilityEditFSInstance&PRCFSParam1=BooleanCompatibilityRule&suiteKey=Configuration&emxSuiteDirectory=configuration";
                String strHrefProductBCR = "../components/emxCommonFS.jsp?functionality=ProductCompatibilityEditFSInstance&PRCFSParam1=ProductCompatibilityRule&suiteKey=Configuration&emxSuiteDirectory=configuration";
                //Begin of modify by Enovia MatrixOne for Inc Rule tracking bug dated 29-Apr-2005
                String strHrefInclusionRule = "../components/emxCommonFS.jsp?functionality=InclusionRuleEditFSInstance&PRCFSParam1=InclusionRule&contextPage=WhereUsed&suiteKey=Configuration&emxSuiteDirectory=configuration";
                //End of modify by Enovia MatrixOne for Inc Rule tracking bug dated 29-Apr-2005
                String strHrefWebForm = "../common/emxForm.jsp?mode=Edit&editLink=false";
                String strHrefDefault = "../common/emxDynamicAttributes.jsp";
                StringBuffer sbHref = new StringBuffer();
                StringBuffer sbEditIcon = new StringBuffer();
                Map objectMap = new HashMap();
                String strFeatureObjId = STR_BLANK;
                String strGBOMObjId = STR_BLANK;
                String strFeatureIRParentId = STR_BLANK;
                String strGBOMIRParentId = STR_BLANK;
                String strFeatureRuleTargetId = STR_BLANK;
                String strGBOMRuleTargetId = STR_BLANK;

                //loop through all the records
                while(objectListItr.hasNext())
                {
                    //Clear the buffers
                    sbHref.delete(0,sbHref.length());
                    sbEditIcon.delete(0,sbEditIcon.length());
                    objectMap = (Map) objectListItr.next();
                    strId = (String) objectMap.get(SELECT_ID);
                    strBCRParentId = (String) objectMap.get(BCR_FROM_OID);
                    strRelId = (String) objectMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                    DomainObject dObj = new DomainObject(strId);
                    strType = (String) dObj.getInfo(context,SELECT_TYPE);
                    //get the top most parent of current type
                    strBaseType = FrameworkUtil.getBaseType
                                                      (context,strType,
                                                       context.getVault());

                    /*checks whether the context user has remove access for
                      each of the selected */
                    if (FrameworkUtil.hasAccess(context, dObj, "modify"))
                    {
                      //if called from normal mode(not report)
                      if(!(strReportFormat!=null&&
                         strReportFormat.equals("null")==false&&
                          strReportFormat.equals("")==false))
                        {
                              if (strType.equalsIgnoreCase(strTypeBCR)||
                                  strType.equalsIgnoreCase(strTypePCR))
                              {
                             /*Begin of Modify:Ramandeep,Enovia MatrixOne Bug#300035 3/11/2005*/
                                   if (strBCRParentId==null||
                                       strBCRParentId.equalsIgnoreCase(STR_BLANK)||
                                       strBCRParentId.equalsIgnoreCase(STR_NULL))
                             /*End of Modify:Ramandeep,Enovia MatrixOne Bug#300035 3/11/2005*/
                                      {
                                         sbHref.append(strHrefProductBCR);
                                      }
                                   else
                                      {
                                         sbHref.append(strHrefFeatureBCR);
                                      }
                                  sbHref.append("&objectId=");
                                  sbHref.append(strId);
                                  sbHref.append("&parentOID=");
                                  sbHref.append(strBCRParentId);
                              }
                              else if (strType.equalsIgnoreCase(strTypeInclusionRule))
                              {
                               /*Begin of Add:Ramandeep,Enovia MatrixOne Bug#300035 3/11/2005*/
                               DomainObject domRule = DomainObject.newInstance(context,strId);
                               List lstRuleSelects = new StringList();
                               lstRuleSelects.add(sbRuleTargetId.toString());
                               lstRuleSelects.add(sbRuleLERelId.toString());
                               lstRuleSelects.add(sbIRProductId.toString());
                               lstRuleSelects.add(sbIRParentId.toString());
                               lstRuleSelects.add(sbGBOMRuleLERelId.toString());
                               lstRuleSelects.add(sbGBOMIRParentId.toString());
                               lstRuleSelects.add(sbGBOMRuleTargetId.toString());

                               Map mapRule = domRule.getInfo(context,(StringList)lstRuleSelects);
                               sbHref.delete(0,sbHref.length());
                               sbHref.append(strHrefInclusionRule);

                               strFeatureObjId = (String)mapRule.get(sbRuleLERelId.toString());
                               strGBOMObjId = (String)mapRule.get(sbGBOMRuleLERelId.toString());
                               if (strFeatureObjId!=null&&!strFeatureObjId.equals(STR_BLANK)
                                   &&!strFeatureObjId.equalsIgnoreCase(STR_NULL)){
                                   sbHref.append("&objectId=");
                                   sbHref.append(strFeatureObjId);
                                }else if (strGBOMObjId!=null&&!strGBOMObjId.equals(STR_BLANK)
                                   &&!strGBOMObjId.equalsIgnoreCase(STR_NULL)){
                                   sbHref.append("&objectId=");
                                   sbHref.append(strGBOMObjId);
                                }

                               strFeatureIRParentId= (String)mapRule.get(sbIRParentId.toString());
                               strGBOMIRParentId= (String)mapRule.get(sbGBOMIRParentId.toString());
                               if (strFeatureIRParentId!=null&&!strFeatureIRParentId.equals(STR_BLANK)
                                   &&!strFeatureIRParentId.equalsIgnoreCase(STR_NULL)){
                                   sbHref.append("&parentOID=");
                                   sbHref.append(strFeatureIRParentId);
                                }else if (strGBOMIRParentId!=null&&!strGBOMIRParentId.equals(STR_BLANK)
                                   &&!strGBOMIRParentId.equalsIgnoreCase(STR_NULL)){
                                   sbHref.append("&parentOID=");
                                   sbHref.append(strGBOMIRParentId);
                                }

                               strFeatureRuleTargetId = (String)mapRule.get(sbRuleTargetId.toString());
                               strGBOMRuleTargetId = (String)mapRule.get(sbGBOMRuleTargetId.toString());
                               if (strFeatureRuleTargetId!=null&&!strFeatureRuleTargetId.equals(STR_BLANK)
                                   &&!strFeatureRuleTargetId.equalsIgnoreCase(STR_NULL)){
                                   sbHref.append("&relId=");
                                   sbHref.append(strFeatureRuleTargetId);
                                }else if (strGBOMRuleTargetId!=null&&!strGBOMRuleTargetId.equals(STR_BLANK)
                                   &&!strGBOMRuleTargetId.equalsIgnoreCase(STR_NULL)){
                                   sbHref.append("&relId=");
                                   sbHref.append(strGBOMRuleTargetId);
                                }

                               sbHref.append("&productID=");
                               sbHref.append((String)mapRule.get(sbIRProductId.toString()));

                              /*End of add:Ramandeep,Enovia MatrixOne Bug#300035 3/11/2005*/
                              }
                              else if (strBaseType.equalsIgnoreCase(strTypePart))
                              {
                               sbHref.delete(0,sbHref.length());
                               sbHref.append(strHrefPart);
                               sbHref.append("&relId=");
                               sbHref.append(strRelId);
                               sbHref.append("&objectId=");
                               sbHref.append(strId);
                               sbHref.append("&parentOID=");
                               sbHref.append(strId);
                              }
                              else{

                              //get the symbolic name for the BaseType
                              strSymbolicTypeName = FrameworkUtil.getAliasForAdmin
                                                                   (context,SELECT_TYPE,
                                                                    strBaseType,true);

                             /*Here it is assumed that name of the webform for a type
                               is same as type's symbolic name eg type_Products */

                                 //check whether there is an associated webform for this type

                               strCommand = "list form \"" + strSymbolicTypeName + "\" ";
                               strFormName = MqlUtil.mqlCommand(context, strCommand);

                               if (strFormName!=null&&!strFormName.equalsIgnoreCase(STR_BLANK)
                                   &&!strFormName.equalsIgnoreCase(STR_NULL))
                                      {
                                       sbHref.delete(0,sbHref.length());
                                       sbHref.append(strHrefWebForm);
                                       if(strType.equals(strIssueType))
                                          {
                                           sbHref.append("&formHeader=emxComponents.Heading.Edit");
                                           sbHref.append("&suiteKey=Components");
                                          }
                                       else
                                          {
                                          sbHref.append("&formHeader=emxProduct.Heading.Edit");
                                          sbHref.append("&suiteKey=ProductLine");
                                          }
                                       sbHref.append("&form=");
                                       sbHref.append(strSymbolicTypeName);
                                       sbHref.append("&objectId=");
                                       sbHref.append(strId);
                                       }
                                 //otherwise re-direct to default jsp
                                 else {
                                       sbHref.delete(0,sbHref.length());
                                       sbHref.append(strHrefDefault);
                                       sbHref.append("?objectId=");
                                       sbHref.append(strId);
                                      }
                              }

                              sbEditIcon.append("<a href=\"javascript:showModalDialog('");
                              String sHref = sbHref.toString();
                              sHref = sHref.replace("&", "&amp;");
                              sbEditIcon.append(sHref);
                              sbEditIcon.append("', '570', '520')\"><img border='0' src='../common/images/iconActionEdit.gif'></img></a>");
                              editIconVector.add(sbEditIcon.toString());
                              //clear the buffer
                              sbEditIcon.delete(0,sbEditIcon.length());
                        }
                       else//if called from report show only the image
                        {
                         editIconVector.add(strImageSource);
                        }

                  }//end if
                  else//if the user does not have modify access
                    {
                      editIconVector.add(STR_BLANK);
                    }
                } //End of while loop
                return editIconVector;

        } //End of the method

       /**
         * This method is used to get the level for all the objects
           which are associated with context object.
         * @param context the eMatrix <code>Context</code> object
         * @param args - Holds the parameters passed from the calling method
            When this array is unpacked, arguments corresponding to the following
            String keys are found:-
            objectList- MapList Containing the objectIds.
         * @return Vector
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
         **/

    public static Vector getLevel(Context context,String[] args) throws Exception
        {
            Vector levelVector = new Vector();

                Map programMap = (Map) JPO.unpackArgs(args);
                List objectList = (MapList)programMap.get(STR_OBJECT_LIST);
                Iterator objectListItr = objectList.iterator();
                String strLevel = STR_BLANK;
                String sLevel = STR_BLANK;
                Map objectMap = new HashMap();
                //loop through all the records
                while(objectListItr.hasNext())
                {
                    objectMap = (Map) objectListItr.next();
                    strLevel = (String)objectMap.get(DomainConstants.KEY_LEVEL);
                    sLevel=(String)objectMap.get("sLevel");
                    if(UIUtil.isNotNullAndNotEmpty(sLevel))
					 strLevel = sLevel;
                    //XSSOK
                    levelVector.add(strLevel);
                } //End of while loop

                return levelVector;

        } //End of the method


        /**
         * This method is used to get the string of admin names
           from a property file corresponding to the key passed.
         * @param context the eMatrix <code>Context</code> object
         * @param String strKey - The key to be Searched
         * @param String strDefault - Value to be returned if key is missing
         * @return String
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
         **/
    public static String getPattern(Context context,String strKey,String strDefault)
        throws Exception
        {
         StringBuffer sbTypeNames=new StringBuffer(30);
         String strPattern = STR_BLANK;
         String strSymbolicNames = STR_BLANK;

         try{
             strSymbolicNames = EnoviaResourceBundle.getProperty(context,strKey);
             }
           catch(Exception e)
             {
             strSymbolicNames = strKey;
             }
         if ((strSymbolicNames != null)&&
               (!STR_BLANK.equals(strSymbolicNames)&&
               (!STR_NULL.equals(strSymbolicNames))
               )
              )
            strPattern =
                 getActualNamesFromSymbolicNames(context,strSymbolicNames);

         if ((strPattern == null) ||
               (STR_BLANK.equals(strPattern)||
               (STR_NULL.equals(strPattern))
               )
              )
            strPattern = strDefault;
           return strPattern;

        }//End of the method

        /**
         * This method is used to get a comma separated string of the
           actual names for admin objects.
         * @param context the eMatrix <code>Context</code> object
         * @param String strSymbolicNamesList - a comma separated string of
            symbolic names
         * @return String
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
         **/
    public static String getActualNamesFromSymbolicNames(Context context,
                                                     String strSymbolicNamesList)
        throws Exception
        {
            int i=0;
            String strActualNames = STR_BLANK;
            String strToken = STR_BLANK;
            StringBuffer sbActualNames = new StringBuffer();
            StringTokenizer stActualNames =
            new StringTokenizer(strSymbolicNamesList,STR_COMMA);
            while(stActualNames.hasMoreTokens())
             {

                    strToken = PropertyUtil.getSchemaProperty(context,
                                                   stActualNames.nextToken());
                    if(i>0)
                    sbActualNames.append(STR_COMMA);
                    if(strToken!=null)
                       {
                        sbActualNames.append(strToken);
                        i++;
                       }
             }//end while loop
            strActualNames = sbActualNames.toString();

          return strActualNames;

        }//End of the method

        /**
         * This method is used to get the string of Relationship
           for Intermediate types in the database.
         * @param context the eMatrix <code>Context</code> object
         * @param String strIntermediateTypePattern
         * @return String
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
         **/
    public static String getIntermediateRelationships(Context context,
                                                  String strIntermediateTypePattern)
        throws Exception
        {
         int i = 0;
         StringBuffer sbIntermediateRelPattern = new StringBuffer();
         StringTokenizer stIntermediateTypePattern =
               new StringTokenizer(strIntermediateTypePattern,STR_COMMA);
         String strIntermediateRelPattern = STR_BLANK;
         String strCurrentToken = STR_BLANK;
         List lstIntermediateRelationshipList = new StringList();

            while(stIntermediateTypePattern.hasMoreTokens())
            {
                strCurrentToken = stIntermediateTypePattern.nextToken();

                //create a new BusinessType object
                BusinessType busType =
                   new BusinessType(strCurrentToken,context.getVault());

                /*calling the method getRelationshipTypes(of class BusinessType)
                  to get the list of all Relationship for the type*/
                lstIntermediateRelationshipList =
                             busType.getRelationshipTypes(context,true,true,false);
                //get the string from the list and append to stringbuffer
                if(i > 0)
                sbIntermediateRelPattern.append(STR_COMMA);
                sbIntermediateRelPattern.append
                              (getStringFromList(lstIntermediateRelationshipList));
                i++;
            }//end while loop

        return sbIntermediateRelPattern.toString();
       }//End Method

        /**
         * This method is used to get the string from a List
         * @param List lstToBeConverted - list to be converted
         * @return String
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
         **/
    public static String getStringFromList(List lstToBeConverted)
        throws Exception
        {

         StringBuffer sbConverted = new StringBuffer();
         for (int i = 0;i < lstToBeConverted.size() ;i++ )
         {
          if(i>0)
          sbConverted.append(STR_COMMA);
          sbConverted.append(lstToBeConverted.get(i));
         }
        return sbConverted.toString();
        }//End Method

        /**
         * This method is used check whether a Particular exists within
           a string.
         * @param String strParent - The source string
         * @param String strPattern -Pattern to be searched
         * @return boolean
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
         **/
    public static boolean boolContains(String strParent,String strPattern)
        throws Exception
        {
            boolean bContains = false;
            String strCurrentToken = STR_BLANK;
            StringTokenizer stParent =
            new StringTokenizer(strParent,STR_COMMA);
            while(stParent.hasMoreTokens())
            {

                strCurrentToken = stParent.nextToken();
                if (strCurrentToken.equals(strPattern))
                {
                 bContains = true;
                 break;
                }
            }//end while loop
            return bContains;


        }//End of method

        /**
         * This method is used to set the level for all the objects in a
           MapList as seen by the user by ignoring the Intermediate Objects.
         * @param List relObjIdList - contains the parent MapList
         * @param String strIntermediateTypes - comma seperated string of
           intermediate types
         * @return List- The modified MapList
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
         **/
     public static List setLevelsInMapList(List relObjIdList,
                                          String strIntermediateTypes)
         throws Exception
         {

             /* This processing is dependent upon the structure of the MapList
                returned by the method getRelatedObjects() which searches for
                the connected objects in a depth first manner. */

              int iLevelCurrent = 0;
              int iLevelNext = 0;
              String strType = STR_BLANK;
              String strLevel = STR_BLANK;
              Map mpMapCurrent= null;
              Map mpMapNext= null;
              //Loop through the whole Maplist
                for (int index = 0;index<relObjIdList.size();index++ )
                 {
                    mpMapCurrent = (Map)relObjIdList.get(index);
                    strType=(String)mpMapCurrent.get(SELECT_TYPE);
                    iLevelCurrent = Integer.parseInt(
                                      (String)mpMapCurrent.get(DomainConstants.KEY_LEVEL));

                     /* if an intermediate object is encountered loop through the maplist
                       and decrement by 1 ,the level in all the Maps having level
                       greater than it */
                    if (boolContains(strIntermediateTypes,strType))
                    {
                        for (int i = index ;i < relObjIdList.size()-1;i++ )
                        {


                          mpMapNext = (Map)relObjIdList.get(i+1);
                          iLevelNext = Integer.parseInt((String)mpMapNext.get
                                                             (DomainConstants.KEY_LEVEL));


                          if (iLevelNext > iLevelCurrent)
                           {
                                iLevelNext--;
                                Integer intObjLevel=new Integer(iLevelNext);
                                strLevel = intObjLevel.toString();
                                mpMapNext.put(DomainConstants.KEY_LEVEL,strLevel);
                           }//end inner if
                          else
                            break;
                        }//end inner for loop
                    }//end outer if
                 }//end outermost for loop
         return relObjIdList;
         }//end of the method

        /**
         * This method is used to remove the Intermediate objects
           from the MapList.
         * @param List objMapList - contains the MapList
         * @param String strIntermediateTypes - comma seperated string of
           intermediate types
         * @return List- The modified MapList
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
         **/
     public static List removeIntermediateObjects(List objMapList,
                                                 String strIntermediateTypes)
         throws Exception
         {
                 Map mpMapCurrent = null;
                 String strType = STR_BLANK;
                 for (int i = 0;i < objMapList.size();i++ )
                 {
                    mpMapCurrent = (Map)objMapList.get(i);
                    strType=(String)mpMapCurrent.get(SELECT_TYPE);
                    if (boolContains(strIntermediateTypes,strType))
                       {
                        objMapList.remove(mpMapCurrent);
                        i--;
                       }
                 }
            return objMapList;
          }//end of the method

        /**
         * This method forms a Map containing stringlist "objectselects"
           based on Display Relationships and a Map "relName" containing
           corresponding objectselects as key and Relationship name as value
         * @param String strDisplayRelPattern - display Relationships
         * @return Map
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
         **/
     public static  Map getObjectsSelect(String strDisplayRelPattern)
       throws Exception
         {

         List slRelSelect = new StringList();
         Map tmpmap = new HashMap();
         StringBuffer sbTmp = new StringBuffer();
         StringTokenizer stRelPattern= new StringTokenizer
                                         (strDisplayRelPattern,STR_COMMA);
         String strNextToken = "";
         while(stRelPattern.hasMoreTokens())
             {

              strNextToken = stRelPattern.nextToken();
              sbTmp.append("to[");
              sbTmp.append(strNextToken);
              sbTmp.append("].");
              sbTmp.append(DomainConstants.SELECT_FROM_ID);

              slRelSelect.add(sbTmp.toString());
              tmpmap.put(sbTmp.toString(),strNextToken);
              sbTmp.delete(0,sbTmp.length());
             }
             Map returnMap = new HashMap();
             returnMap.put("objselects",slRelSelect);
             returnMap.put("relName",tmpmap);

     return returnMap;
         }
        /**
         * This method is used to remove the duplicate objects
           from the MapList.
         * @param context the eMatrix <code>Context</code> object
         * @param List objMapList - contains the MapList
         * @return List- The modified MapList
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
         **/
     public static List removeDuplicateObjects(Context context,List objMapList)
         throws Exception
         {
                 List filteredMapList = new MapList(objMapList);
                 Map mpMapCurrent = null;
                 Map mpMapNext = null;
                 int iLevelCurrent = 0;
                 int iLevelNext = 0;
                 int iNoOfOccurence = 0;
                 String strRelNameCurrent = STR_BLANK;
                 String strRelNameNext = STR_BLANK;
                 String strFromIdCurrent = STR_BLANK;
                 String strFromIdNext = STR_BLANK;
                 String strToIdCurrent = STR_BLANK;
                 String strToIdNext = STR_BLANK;
                 /*Begin of add:Vibhu,Enovia MatrixOne Bug#300051 3/29/2005*/
                 String strTypeNext = STR_BLANK;
                 String strTypeCurrent = STR_BLANK;
                 String strNameCurrent = STR_BLANK;
                 String strNameNext = STR_BLANK;
                 /*End of add:Vibhu,Enovia MatrixOne Bug#300051 3/29/2005*/

                 for (int i = 0;i < objMapList.size();i++ )
                 {
                    iNoOfOccurence = 0;

                    mpMapCurrent = (Map)objMapList.get(i);
                    strRelNameCurrent = (String)mpMapCurrent.get(STR_RELATIONSHIP);
                    strFromIdCurrent = (String)mpMapCurrent.get(DomainConstants.SELECT_FROM_ID);
                    strToIdCurrent = (String)mpMapCurrent.get(DomainConstants.SELECT_TO_ID);
                    strTypeCurrent = (String)mpMapCurrent.get(SELECT_TYPE);
                    strNameCurrent =(String)mpMapCurrent.get(DomainConstants.SELECT_NAME);

            for (int j = i+1;j < objMapList.size();j++ )
            {

                        mpMapNext = (Map)objMapList.get(j);
                        strRelNameNext = (String)mpMapNext.get(STR_RELATIONSHIP);
                        strFromIdNext = (String)mpMapNext.get(DomainConstants.SELECT_FROM_ID);
                        strToIdNext = (String)mpMapNext.get(DomainConstants.SELECT_TO_ID);
                        /*Begin of add:Vibhu,Enovia MatrixOne Bug#300051 3/29/2005*/
                        strTypeNext = (String)mpMapNext.get(SELECT_TYPE);
                        strNameNext =(String)mpMapNext.get(DomainConstants.SELECT_NAME);
                        /*End of add:Vibhu,Enovia MatrixOne Bug#300051 3/29/2005*/

                         /*Remove duplicate objects if from
                           side object ,to side object is same and one of the
                           following condition is met:
                           1.relationship is same
                           2.relationship is LE or RE
                           The second condition takes care of the scenerio when
                           a Feature is used in rule in left exp as well as
                           Right exp.
                           */

                 if ( strFromIdNext.equalsIgnoreCase(strFromIdCurrent)
                     &&
                      strTypeNext.equalsIgnoreCase(strTypeCurrent) && strNameCurrent.equals(strNameNext))
                            {
                                    filteredMapList.remove(mpMapNext);

                                }//end if


                     }//End of the outer for loop

               }
            return filteredMapList;

          }//End of the method



/**
    * Method retrieves All revisions of the Context Object
    * @param context the eMatrix <code>Context</code> object
    * @param args holds arguments
    * @return MapList - returns the Maplist of revisions
    * @throws Exception if the operation fails
    * @since ProductCentral 10.6
    */

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getAllRevisions(Context context, String[] args) throws Exception{

    Map programMap = (HashMap) JPO.unpackArgs(args);
    String strObjectId = (String)  programMap.get(OBJECT_ID);
    setId(strObjectId);
    StringList objectList= new StringList(DomainConstants.SELECT_ID);
    StringList multivalueList= new StringList();
    //Function call to retrive the information of the revisions of context object, based on the object selects.
    MapList relBusObjList = this.getRevisionsInfo(context,objectList,multivalueList);
    return relBusObjList;
    }

   /**
    * Method shows higher revision Icon if a higher revision of the object exists
    * @param context the eMatrix <code>Context</code> object
    * @param args holds arguments
    * @return List - returns the program HTML output
    * @throws Exception if the operation fails
    * @since ProductCentral 10.6
    */
    public List getHigherRevisionIcon(Context context, String[] args) throws Exception{

	    Map programMap = (HashMap) JPO.unpackArgs(args);
	    MapList relBusObjPageList = (MapList) programMap.get(OBJECT_LIST);
	    Map paramList = (HashMap)programMap.get("paramList");
	    String reportFormat = (String)paramList.get("reportFormat");
	
	    int iNumOfObjects = relBusObjPageList.size();
	    // The List to be returned
	    List lstHigherRevExists= new Vector(iNumOfObjects);
	    String arrObjId[] = new String[iNumOfObjects];
	
	    int iCount;
	    //Getting the bus ids for objects in the table
	    for (iCount = 0; iCount < iNumOfObjects; iCount++) {
	        Object obj = relBusObjPageList.get(iCount);
	        if (obj instanceof HashMap) {
	            arrObjId[iCount] = (String)((HashMap)relBusObjPageList.get(iCount)).get(DomainConstants.SELECT_ID);
	        }
	        else if (obj instanceof Hashtable)
	        {
	            arrObjId[iCount] = (String)((Hashtable)relBusObjPageList.get(iCount)).get(DomainConstants.SELECT_ID);
	        }
	    }

	    //Reading the tooltip from property file.
	    String strTooltipHigherRevExists =
        EnoviaResourceBundle.getProperty(context, SUITE_KEY,ICON_TOOLTIP_HIGHER_REVISION_EXISTS,context.getSession().getLanguage());

        String strHigherRevisionIconTag= "";
        String strIcon = EnoviaResourceBundle.getProperty(context,"emxComponents.HigherRevisionImage");
        DomainObject domObj = new DomainObject();

        //Iterating through the list of objects to generate the program HTML output for each object in the table
        for (int jCount = 0; jCount < iNumOfObjects; jCount++) {
        	String str = "";
        	boolean hasHigherRevision = DerivationUtil.higherRevisionExists(context, arrObjId[jCount]);
        	if (hasHigherRevision) {
        		if (reportFormat != null && "CSV".equalsIgnoreCase(reportFormat)){
        			strHigherRevisionIconTag = strTooltipHigherRevExists;
	            } else {
	            	strHigherRevisionIconTag =
                        "<img src=\"../common/images/"
                            + XSSUtil.encodeForHTMLAttribute(context,strIcon)
                            + "\" border=\"0\"  align=\"middle\" "
                            + "TITLE=\""
                            + " "
                            + XSSUtil.encodeForHTMLAttribute(context,strTooltipHigherRevExists)
                            + "\""
                            + "/>";
	            }
            } else {
               	strHigherRevisionIconTag = " ";
            }
            lstHigherRevExists.add(strHigherRevisionIconTag);
        }
        return lstHigherRevExists;
    }


    /**
    * Method shows higher revision Icon in the object property page if a higher revision of the object exists
    * @param context the eMatrix <code>Context</code> object
    * @return String - returns the program HTML output
    * @throws Exception if the operation fails
    * @since ProductCentral 10.6
    */
    public String getHigherRevisionIconProperty(Context context, String[] args) throws Exception {

    	Map programMap = (HashMap) JPO.unpackArgs(args);
    	Map relBusObjPageList = (HashMap) programMap.get("paramMap");
    	String strObjectId = (String)relBusObjPageList.get("objectId");
    	
    	Map requestMap = (HashMap)programMap.get("requestMap");
    	String reportFormat = (String)requestMap.get("reportFormat");
    	
    	//String Buffer to display the Higher revision field in Req property page.
    	StringBuffer sbHigherRevisionExists = new StringBuffer(100);
    	String strHigherRevisionExists = "";

    	//Reading the tooltip from property file.
    	String strTooltipHigherRevExists =
    	EnoviaResourceBundle.getProperty(context, SUITE_KEY,ICON_TOOLTIP_HIGHER_REVISION_EXISTS,context.getSession().getLanguage());

        String strHigherRevisionIconTag= "";
        DomainObject domObj = DomainObject.newInstance(context, strObjectId);

        // Begin of Add by Enovia MatrixOne for Bug 300775 Date 03/25/2005
        String strNo                  = EnoviaResourceBundle.getProperty(context, SUITE_KEY,
                                            "emxProduct.Label.No",
                                            context.getSession().getLanguage());
        String strYes                 = EnoviaResourceBundle.getProperty(context, SUITE_KEY,
                                            "emxProduct.Label.Yes",
                                            context.getSession().getLanguage());
        // End of Add by Enovia MatrixOne for Bug 300775 Date 03/25/2005

        //To generate the program HTML output for the context object

        if (DerivationUtil.higherRevisionExists(context, strObjectId)) {
        	//check for reportFormat
        	if(ProductLineCommon.isNotNull(reportFormat))
        	{
        		strHigherRevisionIconTag = strYes;
        	}
        	else
        	{
/*        		strHigherRevisionIconTag =
                    "<a HREF=\"#\" TITLE=\""
                            + " "
                            + XSSUtil.encodeForHTMLAttribute(context,strTooltipHigherRevExists)
                            + "\">"
                            + HIGHER_REVISION_ICON
                            + XSSUtil.encodeForXML(context,strYes)
                            + "</a>";*/
        		strHigherRevisionIconTag = HIGHER_REVISION_ICON + XSSUtil.encodeForXML(context,strYes);
        	}
        	sbHigherRevisionExists.append(strHigherRevisionIconTag);
            strHigherRevisionExists = sbHigherRevisionExists.toString();

        } else {
            sbHigherRevisionExists.append(strNo);
            strHigherRevisionExists = sbHigherRevisionExists.toString();

        }

        return strHigherRevisionExists;
    }


    /**
     * Method shows higher revision Icon if a higher revision of the object exists
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @return List - returns the program HTML output
     * @throws Exception if the operation fails
     * @since ProductCentral 10.6
     */
     public List getNewDerivationIcon(Context context, String[] args) throws Exception{

 	     Map programMap = (HashMap) JPO.unpackArgs(args);
 	     MapList relBusObjPageList = (MapList) programMap.get(OBJECT_LIST);
 	     Map paramList = (HashMap)programMap.get("paramList");
 	     String reportFormat = (String)paramList.get("reportFormat");
 	
 	     int iNumOfObjects = relBusObjPageList.size();
 	     // The List to be returned
 	     List lstNewDerivationExists = new Vector(iNumOfObjects);
 	     String arrObjId[] = new String[iNumOfObjects];
 	
 	     int iCount;
 	     //Getting the bus ids for objects in the table
 	     for (iCount = 0; iCount < iNumOfObjects; iCount++) {
 	         Object obj = relBusObjPageList.get(iCount);
 	         if (obj instanceof HashMap) {
 	             arrObjId[iCount] = (String)((HashMap)relBusObjPageList.get(iCount)).get(DomainConstants.SELECT_ID);
 	         }
 	         else if (obj instanceof Hashtable)
 	         {
 	             arrObjId[iCount] = (String)((Hashtable)relBusObjPageList.get(iCount)).get(DomainConstants.SELECT_ID);
 	         }
 	     }

 	     //Reading the tooltip from property file.
   	     String strTooltipNewDerivationExists =
   	     EnoviaResourceBundle.getProperty(context, SUITE_KEY,ICON_TOOLTIP_NEW_DERIVATION_EXISTS,context.getSession().getLanguage());

   	     String strNewDerivationIconTag= "";

         //Iterating through the list of objects to generate the program HTML output for each object in the table
         for (int jCount = 0; jCount < iNumOfObjects; jCount++) {
             String str = "";
         	 boolean newDerivationExists = DerivationUtil.newDerivationExists(context, arrObjId[jCount]);
         	if (newDerivationExists) {
         		if (reportFormat != null && "CSV".equalsIgnoreCase(reportFormat)){
         			strNewDerivationIconTag = strTooltipNewDerivationExists;
 	            } else {
 	               strNewDerivationIconTag =
    	                      "<a HREF=\"#\" TITLE=\""
    	                              + " "
    	                              + strTooltipNewDerivationExists
    	                              + "\">"
    	                              + NEW_DERIVATION_ICON
    	                              + "</a>";
 	            }
             } else {
            	 strNewDerivationIconTag = " ";
             }
         	lstNewDerivationExists.add(strNewDerivationIconTag);
         }
         return lstNewDerivationExists;
     }

     /**
     * This FORM JPO method is used to get Derivation Type readonly value.
     * @param context The ematrix context of the request.
     * @param args string array containing packed arguments.
     * @return String containing HTML for combo box
     * @throws FrameworkException
     */
     
     public String getNewDerivationExistsProperty(Context context, String[] args) throws Exception {
    	 String derivationExistsValue = "";

  		// Get the required parameter values from the REQUEST map
    	HashMap programMap = (HashMap) JPO.unpackArgs(args);
		HashMap requestMap = (HashMap) programMap.get("requestMap");
		String objectId = (String) requestMap.get("objectId");
   	  		
		String reportFormat = (String)requestMap.get("reportFormat"); 
		
        //String Buffer to display the Higher revision field in Req property page.
   	    StringBuffer sbNewDerivationExists = new StringBuffer(100);
   	    String strNewDerivationExists = "";

        //Reading the tooltip from property file.
   	    String strTooltipNewDerivationExists =
   	    	EnoviaResourceBundle.getProperty(context, SUITE_KEY,ICON_TOOLTIP_NEW_DERIVATION_EXISTS,context.getSession().getLanguage());

   	    String strNewDerivationIconTag= "";
        String strNo  =EnoviaResourceBundle.getProperty(context, SUITE_KEY,"emxProduct.Label.No",context.getSession().getLanguage());
        String strYes =EnoviaResourceBundle.getProperty(context, SUITE_KEY,"emxProduct.Label.Yes",context.getSession().getLanguage());

        //To generate the program HTML output for the context object

        if (DerivationUtil.newDerivationExists(context, objectId)) {
        	//check for reportFormat
        	if(ProductLineCommon.isNotNull(reportFormat))
        	{
        		strNewDerivationIconTag = strYes;
        	}
        	else
        	{
        		strNewDerivationIconTag =
	                      "<a HREF=\"#\" TITLE=\""
	                              + " "
	                              + XSSUtil.encodeForHTMLAttribute(context,strTooltipNewDerivationExists)
	                              + "\">"
	                              + NEW_DERIVATION_ICON
	                              + XSSUtil.encodeForXML(context,strYes)
	                              + "</a>";
        	}
   	        sbNewDerivationExists.append(strNewDerivationIconTag);
   	        strNewDerivationExists = sbNewDerivationExists.toString();

   	    } else {
   	        sbNewDerivationExists.append(strNo);
   	        strNewDerivationExists = sbNewDerivationExists.toString();
   	    }
   	    return strNewDerivationExists;
   	}
   	  		
    
   /** This column JPO method is used to get the RDO for a object. If the context
    * user has read access on the RDO object then it is hyperlinked to the
    * properties page otherwise only name is returned. Also if the context user
    * doesn't have the show access on the RDo object then context is changed
    * to super user to retrieve the RDO name.
    *
    * @param context the eMatrix <code>Context</code> object
    * @param args - String array containing following packed HashMap
    *                       with following elements:
    *                       paramMap - The HashMap containig the object id.
    * @return String - The program HTML output containing the RDO name.
    * @throws Exception if the operation fails
    * @since ProductCentral 10.6
    */
    public String getDesignResponsibility(Context context, String[] args) throws Exception{
            String strPolicyProduct = PropertyUtil.getSchemaProperty(context,SYMBOLIC_policy_Product);

            String strStateReview = FrameworkUtil.lookupStateName(      context,
                                                                        strPolicyProduct,
                                                                        SYMB_state_Review
                                                                  );
            String strStateRelease = FrameworkUtil.lookupStateName(     context,
                                                                        strPolicyProduct,
                                                                        SYMB_state_Release
                                                                  );
            String strStateObsolete= FrameworkUtil.lookupStateName(     context,
                                                                        strPolicyProduct,
                                                                        SYMB_state_Obsolete
                                                                  );
        //Get the object id of the context object
        Map programMap = (HashMap) JPO.unpackArgs(args);
                Map relBusObjPageList = (HashMap) programMap.get("paramMap");
                Map mpRequest = (HashMap) programMap.get("requestMap");
        String strObjectId = (String)relBusObjPageList.get("objectId");
                String strMode = (String)mpRequest.get("mode");
                String strPFMode=(String)mpRequest.get("PFmode");

        //Begin of add by Enovia MatrixOne on 18-Apr-05 for Bug#300548
        Map fieldMap = (HashMap) programMap.get("fieldMap");
        String strFieldName = (String)fieldMap.get("name");
        //End of add by Enovia MatrixOne on 18-Apr-05 for Bug#300548

        //Form the select expressions for getting the RDO name and RDO id.
        StringBuffer sbRDONameSelect  = new StringBuffer("to[");
        sbRDONameSelect.append(ProductLineConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY);
        sbRDONameSelect.append("].from.");
        sbRDONameSelect.append(DomainConstants.SELECT_NAME);

        StringBuffer sbRDOIdSelect  = new StringBuffer("to[");
        sbRDOIdSelect.append(ProductLineConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY);
        sbRDOIdSelect.append("].from.");
        sbRDOIdSelect.append(DomainConstants.SELECT_ID);

                //Begin of Add by Rashmi, Enovia MatrixOne for bug 301411 Date:4/13/2005
                StringBuffer sbRDOTypeSelect  = new StringBuffer("to[");
        sbRDOTypeSelect.append(ProductLineConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY);
        sbRDOTypeSelect.append("].from.");
        sbRDOTypeSelect.append(DomainConstants.SELECT_TYPE);
                //End of add for bug 301411

        //CODE CHANGES
        String exportFormat = "";
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        if(requestMap!=null && requestMap.containsKey("reportFormat")){
        	exportFormat = (String)requestMap.get("reportFormat");
        }

        StringList lstObjSelects = new StringList();
        lstObjSelects.add(sbRDONameSelect.toString());
        lstObjSelects.add(sbRDOIdSelect.toString());
                //Begin of Add by Rashmi, Enovia MatrixOne for bug 301411 Date:4/13/2005
                lstObjSelects.add(sbRDOTypeSelect.toString());
                //End of add for bug 301411

        String strRDOId = "";
        String strRDOName = "";
        StringBuffer sbHref  = new StringBuffer();
                StringBuffer sbBuffer  = new StringBuffer();
                //Begin of Add by Rashmi, Enovia MatrixOne for bug 301411 Date:4/13/2005
                String strTempIcon = DomainConstants.EMPTY_STRING;
                String strRDOType =  DomainConstants.EMPTY_STRING;
                String strTypeIcon = DomainConstants.EMPTY_STRING;



                //End of add for bug 301411

        //Get the RDO id and name by changing the context to super user
        DomainObject domObj = DomainObject.newInstance(context, strObjectId);
        ContextUtil.pushContext(context);

        Map mapRDO = (Map)domObj.getInfo(context,lstObjSelects);

        ContextUtil.popContext(context);

        //If RDO is set for this object then check whether the context user has read
        //access on the RDO object. If yes then hyperlink the RDO name to its
        //properties page otherwise return the RDO name.
                // If the mode is edit , display the design responsibility field as textbox with a chooser.

        if(strMode!=null && !strMode.equals("") &&
            !strMode.equalsIgnoreCase("null") && strMode.equalsIgnoreCase("edit"))
        {

            if(mapRDO!=null&&mapRDO.size()>0){
                strRDOName = (String) mapRDO.get(sbRDONameSelect.toString());
                //Begin of modify by Enovia MatrixOne on 1-June-05 for bug 304576 reopened
                if (mapRDO.get(sbRDOIdSelect.toString()) instanceof StringList){
                    StringList strRDOListId = (StringList) mapRDO.get(sbRDOIdSelect.toString());
                    strRDOId =  (String)strRDOListId.get(0);
                } else {
                    strRDOId = (String) mapRDO.get(sbRDOIdSelect.toString());
                }
                //End of modify by Enovia MatrixOne on 1-June-05 for bug 304576 reopened

                                //Begin of Add by Rashmi, Enovia MatrixOne for bug 301411 Date:4/13/2005
                                strRDOType = (String) mapRDO.get(sbRDOTypeSelect.toString());
                                //End of Add for bug 301411
            }

            if(strRDOName==null || strRDOName.equalsIgnoreCase("null") || strRDOName.equals("")){
                strRDOName = "";
                strRDOId = "";
                                //Begin of Add by Rashmi, Enovia MatrixOne for bug 301411 Date:4/13/2005
                                strRDOType = "";
                                //End of add for bug 301411
            }
            //Begin of Add by Vibhu,Enovia MatrixOne for Bug 311803 on 11/22/2005
            boolean bHasReadAccess =false;

            /* Start - Added  by Amarpreet Singh 3dPLM for Checking the access to Edit Design Responsibility*/          String strCtxUser = context.getUser();
            String strOwner = (String)domObj.getInfo(context,DomainConstants.SELECT_OWNER);
            boolean hasRoleProductManager  = false;
            boolean hasRoleSystemEngineer  = false;
            boolean bIsOwner               = false;

            Person ctxPerson = new Person (strCtxUser);

            hasRoleProductManager = ctxPerson.isAssigned(context,"Product Manager");
            hasRoleSystemEngineer = ctxPerson.isAssigned(context,"System Engineer");

            if ( strCtxUser != null && !"".equals(strCtxUser))
            {
                if (strOwner!= null && !"".equals(strOwner))
                {
                    if (strOwner.equals(strCtxUser))
                    {
                        bIsOwner = true;
                    }
                }
            }
          /* End - Added  by Amarpreet Singh 3dPLM for Checking the access to Edit Design Responsibility*/
          /* Start  - Added  by Amarpreet Singh 3dPLM for Checking the access to Edit Design Responsibility*/
            try
            {
                if ( (strRDOId==null || strRDOId.equals("")) &&
                    ( bIsOwner || hasRoleProductManager || hasRoleSystemEngineer )){
                    bHasReadAccess = true;
                }
                else {
                    boolean hasAccessOnProject = emxProduct_mxJPO.hasAccessOnProject(context,strRDOId);
                    if (hasAccessOnProject && ( bIsOwner || hasRoleProductManager || hasRoleSystemEngineer ))
                    {
                        bHasReadAccess = true;
                    }
                }
            } catch (Exception e) {
                bHasReadAccess = false;
            }

          /* End - Added  by Amarpreet Singh 3dPLM for Checking the access to Edit Design Responsibility*/

            if (!bHasReadAccess)
            {
                if (strRDOType!=null && !strRDOType.equals("") && !strRDOType.equalsIgnoreCase("null")){
                    strTypeIcon = UINavigatorUtil.getTypeIconFromCache(strRDOType);
                    strTypeIcon = "images/"+strTypeIcon;
                }
                if ( strTypeIcon == null || ("").equals(strTypeIcon))
                {
                    strTypeIcon = "images/iconSmallCompany.gif";
                }
                sbBuffer.delete(0, sbBuffer.length());
                sbBuffer.append("<img border=\"0\" src=\"");
                sbBuffer.append(strTypeIcon);
                sbBuffer.append("\"</img>");
                sbBuffer.append(SYMB_SPACE);
                sbBuffer.append(strRDOName);
            } else {
            //End of Add by Vibhu,Enovia MatrixOne for Bug 311803 on 11/22/2005
                //Begin of modify by Enovia MatrixOne for Bug# 300548 on 18-Apr-05
                sbBuffer.append("<input type=\"text\"");
                sbBuffer.append("name=\"");
                sbBuffer.append(strFieldName);
                sbBuffer.append("Display\" id=\"\" value=\"");
                sbBuffer.append(strRDOName);
                sbBuffer.append("\">");
                sbBuffer.append("<input type=\"hidden\" name=\"");
                sbBuffer.append(strFieldName);
                sbBuffer.append("\" value=\"");
                sbBuffer.append(strRDOId);
                sbBuffer.append("\">");
                sbBuffer.append("<input type=\"hidden\" name=\"");
                sbBuffer.append(strFieldName);
                sbBuffer.append("OID\" value=\"");
                sbBuffer.append(strRDOId);
                sbBuffer.append("\">");
                sbBuffer.append("<input ");
                sbBuffer.append("type=\"button\" name=\"btnDesignResponsibility\"");
                sbBuffer.append("size=\"200\" value=\"...\" alt=\"\" enabled=\"true\" ");
                sbBuffer.append("onClick=\"javascript:showChooser('../common/emxFullSearch.jsp?field=TYPES=type_Organization,type_ProjectSpace&table=PLCDesignResponsibilitySearchTable&selection=single&formName=editDataForm&submitAction=refreshCaller&hideHeader=true&typeAheadTable=PLCTypeAheadTable&submitURL=../productline/SearchUtil.jsp?&mode=Chooser&chooserType=FormChooser");
                sbBuffer.append("&frameName=formEditDisplay");
                sbBuffer.append("&fieldNameActual=");
                sbBuffer.append(strFieldName);
                //Modified for Bug: 372104
                sbBuffer.append("OID");
                sbBuffer.append("&fieldNameDisplay=");
                sbBuffer.append(strFieldName);
                sbBuffer.append("Display");
               // Commenting for Bug: 372104--
//                sbBuffer.append("&fieldNameOID=");
//                sbBuffer.append(strFieldName);
//                sbBuffer.append("OID");
                sbBuffer.append("&searchmode=chooser");
                sbBuffer.append("&suiteKey=Configuration");
                sbBuffer.append("&searchmenu=SearchAddExistingChooserMenu");
                // Begin of Modify by Praveen, Enovia MatrixOne for Bug #300094 03/15/2005
                sbBuffer.append("&searchcommand=PLCSearchCompanyCommand,PLCSearchProjectsCommand");
                // End of Modify by Praveen, Enovia MatrixOne for Bug #300094 03/15/2005
                sbBuffer.append("&PRCParam1=DesignResponsibility");
                sbBuffer.append("&objectId=");
                sbBuffer.append(strObjectId);
                sbBuffer.append("&HelpMarker=emxhelpfullsearch','850','630')\">");
                sbBuffer.append("&nbsp;&nbsp;");
                sbBuffer.append("<a href=\"javascript:ClearDesignResponsibility('");
                sbBuffer.append(strFieldName);
                sbBuffer.append("')\">");
                //End of modify by Enovia MatrixOne for Bug# 300548 on 18-Apr-05

                String strClear =
                EnoviaResourceBundle.getProperty(context, SUITE_KEY,"emxProduct.Button.Clear",context.getSession().getLanguage());
                sbBuffer.append(strClear);
                sbBuffer.append("</a>");
            }
            return sbBuffer.toString();
        }else{

            if(mapRDO!=null&&mapRDO.size()>0){

                strRDOName = (String) mapRDO.get(sbRDONameSelect.toString());

                if (mapRDO.get(sbRDOIdSelect.toString()) instanceof StringList)
                {

                 StringList strRDOListId = (StringList) mapRDO.get(sbRDOIdSelect.toString());
                 strRDOId =  (String)strRDOListId.get(0);
                } else {
                 strRDOId = (String) mapRDO.get(sbRDOIdSelect.toString());

                }

                strRDOType = (String) mapRDO.get(sbRDOTypeSelect.toString());

            }else{
                strRDOName = "";
                strRDOId = "";
                                //Begin of Add by Rashmi, Enovia MatrixOne for bug 301411 date: 4/13/2005
                                strRDOType = "";
                                //End of add for bug 301411
            }

                        //Begin of Add by Rashmi, Enovia MatrixOne for bug 301411 date: 4/13/2005
            //Begin of Modify by Enovia MatrixOne for bug 301411 (reopened) on 24-May-05
            if (strRDOType!=null && !strRDOType.equals("") && !strRDOType.equalsIgnoreCase("null")){
               strTypeIcon = UINavigatorUtil.getTypeIconFromCache(strRDOType);
               strTypeIcon = "images/"+strTypeIcon;
                         }
             //End of Modify by Enovia MatrixOne for bug 301411(reopened) on 24-May-05
                          //End of add for bug 301411
            if(strRDOName!=null && !strRDOName.equals("") && !strRDOName.equalsIgnoreCase("null")){

                boolean bHasReadAccess;
                try
                {
                    //Modified by Vibhu,Enovia MatrixOne for Bug 311803 on 11/22/2005
                    bHasReadAccess = emxProduct_mxJPO.hasAccessOnProject(context,strRDOId);
                } catch (Exception e) {
                    bHasReadAccess = false;
                }

                //Begin of Modify by Rashmi, Enovia MatrixOne for bug 301411 date: 4/13/2005
				if (bHasReadAccess) {
					// CODE changes
					if ("CSV".equalsIgnoreCase(exportFormat)) {
						sbHref.append(strRDOName);
					} else if ("true".equalsIgnoreCase(strPFMode)) {
						sbHref.append("<img border=\"0\" src=\"");
						sbHref.append(strTypeIcon);
						sbHref.append("\"</img>");
						sbHref.append(strRDOName);
					} else {
						sbHref
								.append("<A HREF=\"JavaScript:showDetailsPopup('../common/emxTree.jsp?objectId=");
						sbHref.append(strRDOId);
						sbHref.append("&mode=replace");
						sbHref.append("&AppendParameters=true");
						sbHref.append("&reloadAfterChange=true");
						sbHref.append("')\"class=\"object\">");
						sbHref.append("<img border=\"0\" src=\"");
						sbHref.append(strTypeIcon);
						sbHref.append("\"</img>");
						sbHref.append("</A>");
						sbHref.append("&nbsp");
						sbHref
								.append("<A HREF=\"javascript:showDetailsPopup('../common/emxTree.jsp?objectId=");
						sbHref.append(strRDOId);
						sbHref.append("&mode=replace");
						sbHref.append("&AppendParameters=true");
						sbHref.append("&reloadAfterChange=true");
						sbHref.append("')\"class=\"object\">");
						sbHref.append(strRDOName);
						sbHref.append("</A>");
					}

					return sbHref.toString();
				}else{
                                        sbBuffer.delete(0, sbBuffer.length());
                                      //CODE changes
                                        if("CSV".equalsIgnoreCase(exportFormat)){
                                        	sbBuffer.append(strRDOName);
                                        }else{
                                        	sbBuffer.append("<img border=\"0\" src=\"");
                                            sbBuffer.append(strTypeIcon);
                                            sbBuffer.append("\"</img>");
                                            sbBuffer.append(SYMB_SPACE);
                                            sbBuffer.append(strRDOName);
                                        }

                    return sbBuffer.toString();
                                        //End of modify for bug 301411
                }
            }else{
                return "";
            }
        }
 }

    /**
     * Connects the design responsibility organization to a feature.
     * @param context the eMatrix <code>Context</code> object
     * @param args
     *        0 - HashMap containing one Map entry for the key PARAM_MAP
     *          This Map contains the arguments passed to the jsp which called this method.
     * @return int - an integer (0) if the operation is successful
     * @throws Exception if operation fails
     * @since ProductCentral 10-6
     */
	public int updateDesignResponsibility(Context context, String[] args)
			throws Exception {
		Map programMap = (HashMap) JPO.unpackArgs(args);
		Map paramMap = (HashMap) programMap.get("paramMap");
		String strObjectId = (String) paramMap.get(OBJECT_ID);

		String strNewOrganizationOID = (String) paramMap.get("New Value");
		if (strNewOrganizationOID == null) {
			strNewOrganizationOID = "";
		}
		String strObjID = (String) paramMap.get("objectId");
		// Object for which PrimaryOwnership is to set
		DomainObject domObj = new DomainObject(strObjID);
		String defaultProj=PersonUtil.getDefaultProject(context, context.getUser());
		if (!strNewOrganizationOID.equals("")) {
			DomainObject domObjOrgnization = new DomainObject(
					strNewOrganizationOID);
			// TODO- Assumption Oranization Name is same as Role name
			String strNewOrganizationName = domObjOrgnization.getInfo(context,
					DomainObject.SELECT_NAME);
			domObj.setPrimaryOwnership(context,
					defaultProj,
					strNewOrganizationName);
		}else{
			String defaultOrg=PersonUtil.getDefaultOrganization(context, context.getUser());
			domObj.setPrimaryOwnership(context,defaultProj,defaultOrg);
		}
		return 0;

	}

//Begin of Add by Enovia MatrixOne for EC bug on 18 Mar 2005
    /**
     * This trigger method is used to check whether a valid Engineering Change
     * object in complete state is connected to the object by Implemented or
     * Affected Item relationship when it is promoted to release or obsolete state.
     *.
     * @param context the eMatrix <code>Context</code> object
     * @param args
     *        0 - The id of the object.
     *        1 -  The next state of the object (Release/Obsolete).
     * @return int - an integer (0) if the operation is successful
     * @throws Exception if operation fails
     * @since ProductCentral 10-6
     */
    public int checkConnectedEC (Context context, String[] args)
                                                                            throws Exception{
        //Get the object id and next state from the args
        String strObjectId = args[0];
        String strNextState = args[1];

        //Get the EC Object in Complete state connected to the context object
        //by Implemented Item or Affected Item Relationship depending upon the
        //next state
        DomainObject domItem = DomainObject.newInstance(context,
                                                                                       strObjectId);

        String strStateRelease = FrameworkUtil.lookupStateName(
                                                                context,
                                                                domItem.getPolicy(context).getName(),
                                                                "state_Release");

         List lstObjectSelects = new StringList(DomainConstants.SELECT_ID);

        String strPolicyEC = PropertyUtil.getSchemaProperty(context,
                                                            SYMBOLIC_policy_EngineeringChange);
        String strStateComplete = FrameworkUtil.lookupStateName(
                                                                context,
                                                                strPolicyEC,
                                                                SYMB_state_Complete);

        StringBuffer sbObjWhereExpression = new StringBuffer();
        sbObjWhereExpression.append(DomainConstants.SELECT_CURRENT);
        sbObjWhereExpression.append(SYMB_EQUAL);
        sbObjWhereExpression.append(SYMB_QUOTE);
        sbObjWhereExpression.append(strStateComplete);
        sbObjWhereExpression.append(SYMB_QUOTE);

        StringBuffer sbRelWhereExpression = new StringBuffer();
        sbRelWhereExpression.append(SYMB_ATTRIBUTE);
        sbRelWhereExpression.append(SYMB_OPEN_BRACKET);
        sbRelWhereExpression.append(DomainConstants.ATTRIBUTE_REQUESTED_CHANGE);
        sbRelWhereExpression.append(SYMB_CLOSE_BRACKET);
        sbRelWhereExpression.append(SYMB_EQUAL);

        List lstECList = new MapList();

         //If the item is being to promoted to Release state then
         //Get the EC in complete state connected to this object by Implemented
        //Item Relationship or Affected Item Relationship for which Requested Change
        //attribute is For Release
         if(strNextState.equals(strStateRelease)){
             lstECList = (MapList) domItem.getRelatedObjects(
                                                                context,
                                                                DomainConstants.RELATIONSHIP_EC_IMPLEMENTED_ITEM,
                                                                DomainConstants.QUERY_WILDCARD,
                                                                (StringList) lstObjectSelects,
                                                                null,
                                                                true,
                                                                false,
                                                                (short) 1,
                                                                sbObjWhereExpression.toString(),
                                                                null);
            if(lstECList!=null && !lstECList.isEmpty()){
                return 0;
            }else{
                //Get the EC connected to this object by Affected Item relationship
                //with proper Requested Change attribute
                sbRelWhereExpression.append("\"For Release\"");
                                //Begin of Add by Vibhu,Enovia MatrixOne for Bug 303269 on 28 April 05
                                sbRelWhereExpression.append(SYMB_SPACE);
                sbRelWhereExpression.append(SYMB_OR);
                sbRelWhereExpression.append(SYMB_SPACE);
                sbRelWhereExpression.append(SYMB_ATTRIBUTE);
                sbRelWhereExpression.append(SYMB_OPEN_BRACKET);
                sbRelWhereExpression.append(DomainConstants.ATTRIBUTE_REQUESTED_CHANGE);
                sbRelWhereExpression.append(SYMB_CLOSE_BRACKET);
                sbRelWhereExpression.append(SYMB_EQUAL);
                sbRelWhereExpression.append("\"For Obsolescence\"");
                                //End of Add by Vibhu,Enovia MatrixOne for Bug 303269 on 28 April 05
                lstECList = (MapList) domItem.getRelatedObjects(
                                                                context,
                                                                DomainConstants.RELATIONSHIP_EC_AFFECTED_ITEM,
                                                                DomainConstants.QUERY_WILDCARD,
                                                                (StringList) lstObjectSelects,
                                                                null,
                                                                true,
                                                                false,
                                                                (short) 1,
                                                                sbObjWhereExpression.toString(),
                                                                sbRelWhereExpression.toString());
                if(lstECList!=null && !lstECList.isEmpty()){
                    return 0;
                 }else{
                     String strErrorMsg = EnoviaResourceBundle.getProperty(context, SUITE_KEY,"emxProduct.Alert.NoCompleteECForRelease",context.getSession().getLanguage());
                    emxContextUtil_mxJPO.mqlNotice(context,strErrorMsg);
                    return 1;
                }
            }
        }
        //If the item is being to promoted to Obsolete state then
         //Get the EC in complete state connected to this object Affected Item
         //Relationship for which Requested Change attribute is For Obsolescence
        else{
               sbRelWhereExpression.append("\"For Obsolescence\"");
               lstECList = (MapList) domItem.getRelatedObjects(
                                                                context,
                                                                DomainConstants.RELATIONSHIP_EC_AFFECTED_ITEM,
                                                                DomainConstants.QUERY_WILDCARD,
                                                                (StringList) lstObjectSelects,
                                                                null,
                                                                true,
                                                                false,
                                                                (short) 1,
                                                                sbObjWhereExpression.toString(),
                                                                sbRelWhereExpression.toString());
               if(lstECList!=null && !lstECList.isEmpty()){
                   return 0;
                }else{
                    String strErrorMsg = EnoviaResourceBundle.getProperty(context, SUITE_KEY,"emxProduct.Alert.NoCompleteECForObsolescence",context.getSession().getLanguage());
                    emxContextUtil_mxJPO.mqlNotice(context,strErrorMsg);
                    return 1;
                }
        }
    }
//End of Add by Enovia MatrixOne for EC bug on 18 Mar 2005

        //Begin of Add by Vibhu,Enovia MatrixOne for Bug#300051 on 29 Mar 2005

        /**
         * This method is used to remove a particular branch of maps which have
           levels higher than specified Index object. The method removed the map
           until it encounters a map of lower or equal level than the indexed Object.
         * @param MapList - filteredMapList in which the maps are removed.
         * @param Integer - Index of the base object which is taken as reference, all
                            objects are compared with the indexed object.
         * @return integer- index of the map which has to be searched next.
         * @throws Exception if the operation fails
         * @since ProductCentral 10.6
    **/

        public static int removeBranch(List filteredMapList,int iCurrentIndex) throws Exception
    {
        Map mpMap = null;
        int iNextLevel = 0;
        int iBaseLevel = 0;
        mpMap = (Map)filteredMapList.get(iCurrentIndex);
        iBaseLevel = Integer.parseInt((String)mpMap.get(DomainConstants.KEY_LEVEL));


        //This do while removes those objects whose level is higher then the base level
        do
        {
                if(iCurrentIndex == filteredMapList.size()-1)
                {
                        filteredMapList.remove(mpMap);
                        iCurrentIndex--;
                        break;
                }
                else
                {
                        filteredMapList.remove(mpMap);
                        iCurrentIndex--;
                        mpMap = (Map)filteredMapList.get(iCurrentIndex);
                        iNextLevel = Integer.parseInt((String)mpMap.get(DomainConstants.KEY_LEVEL));
                }
        }while(iNextLevel > iBaseLevel);

        return iCurrentIndex;
    }
         //End of Add by Vibhu,Enovia MatrixOne for Bug#300051 on 29 Mar 2005
    /**
     * This method is used to return the Name and Revision of an object
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @return List- the List of Strings in the form of 'Name Revision'
     * @throws Exception if the operation fails
     * @since ProductCentral 10.6SP1
    **/

    public List getNameRev (Context context, String[] args) throws Exception{
        //unpack the arguments
        Map programMap = (HashMap) JPO.unpackArgs(args);
        List lstobjectList = (MapList) programMap.get(STR_OBJECT_LIST);
        Iterator objectListItr = lstobjectList.iterator();
        //initialise the local variables
            String oidsArray[] = new String[lstobjectList.size()];
            int i = 0;
        Map objectMap = new HashMap();
            String strObjIdtemp =DomainConstants.EMPTY_STRING;
            while(objectListItr.hasNext())
            {
                objectMap = (Map) objectListItr.next();
                strObjIdtemp = (String)objectMap.get(DomainConstants.SELECT_ID);
                oidsArray[i] = strObjIdtemp;
                i++;
            }
            StringList selects = new StringList(3);
            selects.add(DomainConstants.SELECT_TYPE);
            selects.add(DomainConstants.SELECT_NAME);
            selects.add(DomainConstants.SELECT_REVISION);
            MapList list = DomainObject.getInfo(context, oidsArray, selects);
        String strObjId = DomainConstants.EMPTY_STRING;
            Map objecttempMap = new HashMap();
        List lstNameRev = new StringList();
            StringBuffer stbNameRev;
        String strType = null;
            String strName = null;
            String strRev = null;

            objectListItr = list.iterator();
        while(objectListItr.hasNext())
        {
                objecttempMap = (Map) objectListItr.next();
                strName = (String)objecttempMap.get(DomainConstants.SELECT_NAME);
                strType = (String)objecttempMap.get(DomainConstants.SELECT_TYPE);
                strRev = (String)objecttempMap.get(DomainConstants.SELECT_REVISION);

                stbNameRev = new StringBuffer(100);
                stbNameRev = stbNameRev.append(strName);
                if(strType.equalsIgnoreCase(ProductLineConstants.TYPE_PRODUCT_VARIANT)){
                    stbNameRev.append(SYMB_SPACE );
                    stbNameRev.append(strRev.substring(0,1));
                } else {
                    stbNameRev.append(SYMB_SPACE );
                    stbNameRev.append(strRev);
            }
            lstNameRev.add(stbNameRev.toString());
        }
        return lstNameRev;
    }

    /**
     * This method is used to return the status icon of an object
     * @param context the eMatrix <code>Context</code> object
     * @param args holds arguments
     * @return List- the List of Strings in the form of 'Name Revision'
     * @throws Exception if the operation fails
     * @since ProductCentral 10.6SP1
    **/

    public List getStatusIcon (Context context, String[] args) throws Exception{
        //unpack the arguments
        Map programMap = (HashMap) JPO.unpackArgs(args);
        List lstobjectList = (MapList) programMap.get(STR_OBJECT_LIST);
        Iterator objectListItr = lstobjectList.iterator();
        
        // For Fixing IR-228607V6R2014x Start
        Map paramMap = (HashMap) programMap.get("paramList");
        String strReportFormat = (String)paramMap.get("reportFormat");
        // For Fixing IR-228607V6R2014x End
        
        //initialise the local variables
        Map objectMap = new HashMap();
        String strObjId = DomainConstants.EMPTY_STRING;
        String strObjState = DomainConstants.EMPTY_STRING;
        String strIcon = DomainConstants.EMPTY_STRING;
        // Begin of Add by Enovia MatrixOne for Bug # 312021 Date Nov 16, 2005
        String strObjPolicy = DomainConstants.EMPTY_STRING;
        String strObjPolicySymb = DomainConstants.EMPTY_STRING;
        String strObjStateSymb = DomainConstants.EMPTY_STRING;
        StringBuffer sbStatePolicyKey = new StringBuffer();
        boolean flag = false;
        // End of Add by Enovia MatrixOne for Bug # 312021 Date Nov 16, 2005
        List lstNameRev = new StringList();
        StringBuffer stbNameRev = new StringBuffer(100);
        DomainObject domObj = null;
        //loop through all the records
        while(objectListItr.hasNext())
        {
            objectMap = (Map) objectListItr.next();
            strObjId = (String)objectMap.get(DomainConstants.SELECT_ID);
            domObj = DomainObject.newInstance(context, strObjId);
            strObjState = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            // Begin of Add by Enovia MatrixOne for Bug # 312021 Date Nov 16, 2005
            strObjPolicy = domObj.getInfo(context, DomainConstants.SELECT_POLICY);

            // Getting symbolic names for both policy & state
            strObjPolicySymb = FrameworkUtil.getAliasForAdmin(context,DomainConstants.SELECT_POLICY,strObjPolicy,true);
            strObjStateSymb = FrameworkUtil.reverseLookupStateName(context, strObjPolicy, strObjState);

            // Forming the key which is to be looked up
            sbStatePolicyKey = new StringBuffer("emxProduct.LCStatusImage.");
            sbStatePolicyKey.append(strObjPolicySymb)
                            .append(".")
                            .append(strObjStateSymb);

            // Geeting the value for the corresponding key, if not catching it to set flag = false
            try{
                strIcon = EnoviaResourceBundle.getProperty(context,sbStatePolicyKey.toString());
                flag = true;
            }
            catch(Exception ex)
            {
                flag = false;
            }
            // End of Add by Enovia MatrixOne for Bug # 312021 Date Nov 16, 2005

            //Begin of Add by Vibhu,Enovia MatrixOne for Bug 310473 on 10/11/2005
            // Begin of Add by Enovia MatrixOne for Bug # 312021 Date Nov 16, 2005
            if(flag) 
            {
                strObjState = FrameworkUtil.findAndReplace(strObjState," ", "");
                StringBuffer sbStateKey = new StringBuffer("emxFramework.State.");
                sbStateKey.append(strObjState);
                strObjState = EnoviaResourceBundle.getProperty(context, SUITE_KEY,sbStateKey.toString(),context.getSession().getLanguage());
                //End of Add by Vibhu,Enovia MatrixOne for Bug 310473 on 10/11/2005
                
                if(strReportFormat == null || (strReportFormat!=null && strReportFormat.equalsIgnoreCase(""))) // if(strReportFormat == null || strReportFormat == "") else , Added For Fixing IR-228607V6R2014x
                {
                	stbNameRev.delete(0, stbNameRev.length());
                    stbNameRev = stbNameRev.append("<img src=\"../common/images/")
                                    .append(strIcon)
                                    .append("\" border=\"0\"  align=\"middle\" ")
                                    .append("TITLE=\"")
                                    .append(" ")
                                    .append(strObjState)
                                    .append("\"")
                                    .append("/>");
                    lstNameRev.add(stbNameRev.toString());
                }
                else
                {
                	lstNameRev.add(strObjState);  
                }
                
            }
            else
            {
                lstNameRev.add(DomainConstants.EMPTY_STRING);
            }
            // End of Add by Enovia MatrixOne for Bug # 312021 Date Nov 16, 2005
        }
        return lstNameRev;
    }

//Start of Add By Enovia MatrixOne, for Performance Bug - Where Used on 23 Jan 06.
/**
     * This method is used to remove the duplicate values in a String having
     * various elements separated by some delimiting character
     * @param String to be investigated for duplicate entries
     * @param delimiting character
     * @return String having unique elements separated by the delimiting character
     * @throws Exception if the operation fails
     * @since ProductCentral 10.6SP2
    **/

    public static String removeDuplicateInString (String str, String strDelimiting) throws Exception
    {
        StringTokenizer st = new StringTokenizer(str, strDelimiting);
        HashSet hashset = new HashSet();
        StringBuffer sb = new StringBuffer(300);
        while(st.hasMoreTokens())
            {
                hashset.add(st.nextElement());
            }

        Iterator lstItr = hashset.iterator();
        while (lstItr.hasNext())
            {
                sb.append(lstItr.next().toString());
                sb.append(STR_COMMA);
            }

        sb.deleteCharAt(sb.length()-1);

        return sb.toString();
    }
//End of Add By Enovia MatrixOne, for Performance Bug - Where Used on 23 Jan 06.
    /**
     * To obtain the list of Object IDs to be excluded from the search for Add Existing Actions
     *
     * @param context- the eMatrix <code>Context</code> object
     * @param args- holds the HashMap containing the following arguments
     * @return  StringList- consisting of the object ids to be excluded from the Search Results
     * @throws Exception if the operation fails
     * @author Sandeep Kathe(klw)
     */

    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList excludeConnected(Context context, String[] args) throws Exception
    {
        Map programMap = (Map) JPO.unpackArgs(args);
        String strObjectIds = (String)programMap.get("objectId");
        String strRelationship=(String)programMap.get("relName");
        StringList excludeList= new StringList();
        StringTokenizer objIDs = new StringTokenizer(strObjectIds,",");
        String toType=null;
        String fromType=null;
        boolean bisTo=false;
        boolean bisFrom=false;
        DomainObject domObjFeature = new DomainObject(strObjectIds);
        toType=domObjFeature.getInfo(context,"to["+PropertyUtil.getSchemaProperty(context,strRelationship)+"].from.type");
        fromType=domObjFeature.getInfo(context,"from["+PropertyUtil.getSchemaProperty(context,strRelationship)+"].to.type");

        if(toType!=null){
            bisTo=true;
        }
        else{
            bisFrom=true;
        }
        MapList childObjects=domObjFeature.getRelatedObjects(context,
                PropertyUtil.getSchemaProperty(context,strRelationship),
                toType==null?fromType:toType,
                new StringList(DomainConstants.SELECT_ID),
                null,
                bisTo,
                bisFrom,
               (short) 1,
                DomainConstants.EMPTY_STRING,
                DomainConstants.EMPTY_STRING);
        for(int i=0;i<childObjects.size();i++){
            Map tempMap=(Map)childObjects.get(i);
            excludeList.add(tempMap.get(DomainConstants.SELECT_ID));
        }
        excludeList.add(strObjectIds);
        return excludeList;
    }

    /**
     * Method Check if Variant Configuration Application is Installed.
     *
     * @param context The ematrix context object.
     * @param String[] The args .
     * @return Boolean Object - Returns true if Variant Configuration is Installed.
     *                        - Returns false if Variant Configuration is not Installed.
     * @since ProductLine X4
     */
     public Object isVariantConfigurationInstalled(Context context, String[] args) throws Exception
     {
           boolean isConfigurationInstall = FrameworkUtil.isSuiteRegistered(context,"appVersionVariantConfiguration",false,null,null);
           if(isConfigurationInstall)
           {
               return true;
           }
           else{
               return false;
           }
     }

     /**
      * Display the LifeCycle page of the Build in the Properties Page
      *
      * @param context the eMatrix <code>Context</code> object
      * @param args - Holds the HashMap containing the following arguments
      *          paramMap - contains ObjectId, Old Value for product name and new value
      * @return String - returns HTML to display the LifeCycle page
      * @throws Exception if the operation fails
      * @since ProductLine X+4
      */

     public String getLifecycleStates(Context context,String[] args) throws Exception
     {
         String STR_BLANK = "";

         String retString =STR_BLANK;
         String objID =STR_BLANK;
         StringBuffer output = new StringBuffer(" ");
         HashMap programMap = (HashMap) JPO.unpackArgs(args);
         HashMap paramMap = (HashMap)programMap.get("paramMap");
         objID = (String)paramMap.get("objectId");

         DomainObject PartDom = new DomainObject(objID);
         PartDom.setId(objID);

         output.append("<HTML><body bgcolor=\"gray\">");

         output.append("<span name=\"States\" id=\"States\">");

         output.append("<script language='JavaScript'>");
         output.append("var iFrameSrc=\' <iframe name=\"Lifecycle\" src=\"emxLifecycleDialog.jsp?objectId="+objID+"\" width=\"100%\" height=\"100\" marginHeight=\"0\" scrolling=\"auto\" frameborder=\"0\"> </iframe>\';");
         output.append("var vSPAN=document.getElementById('States');");
         output.append("vSPAN.innerHTML =iFrameSrc;");
         output.append("</script>");
         output.append("</span>");
         output.append("</body></HTML>");
         retString = output.toString();

         return retString;
     }

     /**
      * To obtain the list of Object IDs to be excluded from the search for Add Existing of Builds
      * under Product Configuration context
      * @param context- the eMatrix <code>Context</code> object
      * @param args- holds the HashMap containing the following arguments
      * @return  StringList- consisting of the object ids to be excluded from the Search Results
      * @throws Exception if the operation fails
      */

     @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
     public StringList filterRelatedBuilds(Context context, String[] args)
            throws Exception {
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            StringList tempStrList = new StringList();

            StringList tempStrList2 = new StringList();
            StringList tempStrList3 = new StringList();
            //Added for IR-032978
            StringList tempStrProdBuildList = new StringList();
            //End
            String strBuildProductId = "";

            String strParentId = "";

            DomainObject domObject = new DomainObject(strObjectId);
            String strTypeOfObject = domObject.getInfo(context,
                    DomainConstants.SELECT_TYPE);

            // get all the Orphan builds from data base
            StringList objSelect = new StringList(2);
            objSelect.addElement(DomainConstants.SELECT_ID);
            //Added for IR-032978
            String strProdConfIdSelectable  = "to["+ ProductLineConstants.RELATIONSHIP_PRODUCT_CONFIGURATION_BUILD+ "].from.id";
            String strProdIdSelectable = "to["+ ProductLineConstants.RELATIONSHIP_PRODUCT_BUILD+ "].from.id";
            objSelect.addElement(strProdConfIdSelectable);
            objSelect.addElement(strProdIdSelectable);
            //End
            // Get all the Builds from the database
            MapList lstBuildList = DomainObject.findObjects(context,
                    ProductLineConstants.TYPE_BUILDS,
                    DomainConstants.QUERY_WILDCARD, "", objSelect);

            String strBuildType = "";
            String strBuildId = "";
            String strBuildParentId = "";

            if (!(mxType.isOfParentType(context, strTypeOfObject,ProductLineConstants.TYPE_PRODUCTS))) {
                // If context is PC
                strParentId = domObject.getInfo(context,
                                "to["+ ProductLineConstants.RELATIONSHIP_PRODUCT_CONFIGURATION
                                     + "].from.id");

                if(EnoviaResourceBundle.getProperty(context,"eServiceSuiteProductLine.ProductConfigurationBuildAddExisting.DisplayAllProductRevBuilds").equals("true")){
                    return new StringList();
                }
                DomainObject domParentProduct = new DomainObject(strParentId);
                String strParentModelID = domParentProduct.getInfo(context,
                        "to[" + ProductLineConstants.RELATIONSHIP_PRODUCTS
                                + "].from.id");

                if(strParentModelID == null || "".equals(strParentModelID)){
                                strParentModelID = domParentProduct
                                .getInfo(context,
                                        "to[" + ProductLineConstants.RELATIONSHIP_MAIN_PRODUCT
                                        + "].from.id");
                }

                for (int i = 0; i < lstBuildList.size(); i++) {
                    Map productMap = (Map) lstBuildList.get(i);
                    strBuildId = (String) productMap.get("id");

                    // Get the Builds from the database which are not connected
                    // to PC i.e. get all orphan builds and builds under Product
                    //Modified for IR-032978 - Start
//                    DomainObject domBuildId = new DomainObject(strBuildId);
//
//                    strBuildParentId = domBuildId.getInfo(context,"to["
//                                            + ProductLineConstants.RELATIONSHIP_PRODUCT_CONFIGURATION_BUILD
//                                            + "].from.id");
                    strBuildParentId =(String) productMap.get(strProdConfIdSelectable);
                    strBuildProductId = (String) productMap.get(strProdIdSelectable);

                    if (strBuildParentId == null || strBuildParentId.length() == 0) {
                        tempStrList2.add(strBuildId);
                        if(strBuildProductId != null && strBuildProductId.equalsIgnoreCase(strParentId)){
                            tempStrProdBuildList.add(strBuildId);
                        }
                        //Modifications for IR-032978 - End
                    } else {
                        // Builds connected to PC added to the list of objects
                        // which is to be removed from global list of builds
                        tempStrList3.add(strBuildId);

                    }

                }
                // remove the bulids which are already present to other product
                // context,we should have builds only under given product
                // context and orphan
                String strBuildId1 = "";
                for (int i = 0; i < tempStrList2.size(); i++) {
                    strBuildId1 = (String) tempStrList2.get(i);
                    if (!showForProdConfig(context, strBuildId1,
                            strParentModelID, strParentId)) {
                        tempStrList3.addElement(strBuildId1);
                    }
                }
                tempStrList3.addAll(getContextIPUBuilds(context,strParentId));
                //Added for IR-032978
                tempStrList3.removeAll(tempStrProdBuildList);
                //End
                return tempStrList3;

            } else {
                // In product Context..

                // Check for the setting in property file if true return blank
                // to display all the builds..
                if (EnoviaResourceBundle.getProperty(context,"eServiceSuiteProductLine.ProductBuildAddExisting.DisplayOrphanBuilds")
                        .equals("false")) {
                    return new StringList();
                } else {
                    String strParentModelID = domObject.getInfo(context, "to["
                            + ProductLineConstants.RELATIONSHIP_PRODUCTS
                            + "].from.id");

					if(strParentModelID == null || "".equals(strParentModelID)){
                                strParentModelID = domObject
                                .getInfo(context,
                                        "to[" + ProductLineConstants.RELATIONSHIP_MAIN_PRODUCT
                                        + "].from.id");
                    }

                    for (int i = 0; i < lstBuildList.size(); i++) {
                        Map productMap = (Map) lstBuildList.get(i);
                        strBuildId = (String) productMap.get("id");

                        if (!showBuildsInProductContext(context, strParentModelID, strBuildId)) {
                            tempStrList.add(strBuildId);
                        }
                    }
                    tempStrList.addAll(getContextIPUBuilds(context,strObjectId));
                    return tempStrList;
                }

            }

        } catch (Exception e) {
            throw new FrameworkException(e);
        }

    }
    /**
     * This method is added as part of fix for BUG: 370415 -- This will return the ID's of all product context IPUs
     * @param context - Matrix context
     * @param strProdId - Product ID
     * @return Stringlist
     * @throws Exception
     */
     public static StringList getContextIPUBuilds(Context context,  String strProdId) throws Exception {
        StringList strList = new StringList();
        DomainObject objProd = new DomainObject(strProdId);
        String selectables = "from["+ProductLineConstants.RELATIONSHIP_PRODUCT_BUILD+"].to.id";
        StringList objList = objProd.getInfoList(context,selectables);
        strList = filterIPUAndUBOMBuilds(context,objList);
        return strList;
    }
     //Below two methods added as part of fix for IR-032817
     private static StringList filterIPUAndUBOMBuilds(Context context, StringList objList)throws Exception {
         StringList lst = new StringList();
         for(int j=0;j<objList.size();j++) {
         String strBuildId = (String) objList.get(j);
             lst.addAll(expandAndGetBuilds(context,strBuildId,new StringList()));
         }
        return lst;
    }

    private static StringList expandAndGetBuilds(Context context, String strBuildId, StringList processedList) throws Exception{
       StringList lst = new StringList();
       DomainObject domBuild = new DomainObject(strBuildId);
       processedList.add(strBuildId);
       StringList selectStmts = new StringList();
       selectStmts.addElement(DomainConstants.SELECT_ID);
       MapList list = domBuild.getRelatedObjects(context,
               ProductLineConstants.RELATIONSHIP_INTENDED_PRODUCT_UNIT+","+ProductLineConstants.RELATIONSHIP_UNITBOM,  // relationship pattern
               ProductLineConstants.TYPE_HARDWARE_BUILD,                  // object pattern
                                        selectStmts  ,                 // object selects
                                        null,              // relationship selects
                                        true,                        // to direction
                                        true,                       // from direction
                                        (short)1,                    // recursion level
                                        null,                        // object where clause
                                        null);
       for (int i = 0; i < list.size(); i++) {
           Map map = (Map)list.get(i);
           String strTemp = (String) map.get(ProductLineConstants.SELECT_ID);
           if(!processedList.contains(strTemp)){
               expandAndGetBuilds(context,strTemp,processedList);
           }
           lst.addElement(strTemp);

       }
        return lst;
    }

    /**
      * Returns true if build needs to be shown in product configuration context - Add existing operation.
      * @param context the eMatrix <code>Context</code> object.
      * @param strProdModelInfo contains model Id of the selected product.
      * @param strBuildId contains Build Id
      * @param strProductId contains product id of the product configuration.
      * @return boolean.
      * @since ProductLine X5
      * @throws Exception if the operation fails.
     */
    private boolean showForProdConfig(Context context, String strBuildId,
            String strProdModelInfo, String strProductId) {

        try {
            DomainObject objBuild = new DomainObject(strBuildId);
            StringList selectables = new StringList(2);
            String strBuildProdIdSelectable = "to["
                + ProductLineConstants.RELATIONSHIP_PRODUCT_BUILD
                + "].from.id";
            String strBuildModelIdSelectable = "to["
                + ProductLineConstants.RELATIONSHIP_MODEL_BUILD
                + "].from.id";
            selectables.add(strBuildProdIdSelectable);
            selectables.add(strBuildModelIdSelectable);
            Map map = objBuild.getInfo(context, selectables);
            String strBuildProdId = (String)map.get(strBuildProdIdSelectable);
            String strBuildModelInfo = (String) map.get(strBuildModelIdSelectable);
            if(!isUNTInstalled(context,new String[2])){
                if(strBuildProdId == null || strBuildProdId.length() == 0){
                    return true;
                }else if((strBuildProdId != null && strBuildProdId.length()>0)&&strBuildProdId.equalsIgnoreCase(strProductId)){
                    return true;
                }else {
                    return false;
                }
            }
            if (strBuildModelInfo == null && strBuildProdId == null) {
                return true;
            } else if (strBuildModelInfo.equals(strProdModelInfo)
                    && (strBuildProdId == null || strBuildProdId
                            .equals(strProductId))) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if build needs to be shown in product context - Add existing operation.
     * @param context the eMatrix <code>Context</code> object.
     * @param strProdModelInfo contains model Id of the selected product.
     * @param strBuildId contains Build Id
     * @return boolean.
     * @since ProductLine X5
     * @throws Exception if the operation fails.
    */
    private boolean showBuildsInProductContext(Context context, String strProdModelInfo,
            String strBuildId) {
        try {
            DomainObject objBuild = new DomainObject(strBuildId);
            StringList selectables = new StringList(2);
            selectables.add("to["
                    + ProductLineConstants.RELATIONSHIP_PRODUCT_BUILD
                    + "].from.id");
            selectables.add("to["
                    + ProductLineConstants.RELATIONSHIP_MODEL_BUILD
                    + "].from.id");
            Map map = objBuild.getInfo(context, selectables);
            String strProdInfo = (String)map.get("to["
                    + ProductLineConstants.RELATIONSHIP_PRODUCT_BUILD
                    + "].from.id");
            String strBuildModelInfo =(String) map.get("to["
                    + ProductLineConstants.RELATIONSHIP_MODEL_BUILD
                    + "].from.id");

            if ((strProdInfo == null || strProdInfo.length() == 0)
                    && (strBuildModelInfo == null || strBuildModelInfo.length() == 0)) {
                return true;
            } else if (strProdModelInfo.equals(strBuildModelInfo)
                    && (strProdInfo == null || strProdInfo.length() == 0)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }

    }

    /**
      * Returns Type of build - Top level if Unit number exists for a build or Orphan build which is "Others"
      * @param context the eMatrix <code>Context</code> object.
      * @param args contains a packed HashMap containing objectId of object
      * @return String.
      * @since ProductLine X5
      * @throws Exception if the operation fails.
     */
     public String getBuilds(Context context, String args[]) throws Exception {
         String finalReturn   = " ";
         String buildId = args[0];
         String strType = args[1];
         DomainObject domPart = new DomainObject(buildId);
         String strBuildProductId = domPart.getInfo(context,"to["+ProductLineConstants.RELATIONSHIP_PRODUCT_BUILD+"].from.id");
         if(strBuildProductId != null && !strBuildProductId.equalsIgnoreCase("null") && strBuildProductId.length()>0){
               return "Top Level";
          }
           return "Others";
      }

     /**
      * To obtain the list of Object IDs to be excluded from the search for Add Existing Product Actions
      *
      * @param context- the eMatrix <code>Context</code> object
      * @param args- holds the HashMap containing the following arguments
      * @return  StringList- consisting of the object ids to be excluded from the Search Results
      * @throws Exception if the operation fails
      * @author 3DPLM
      */

     public StringList excludeProductRevisionConnected(Context context, String[] args) throws Exception
     {
         Map programMap = (Map) JPO.unpackArgs(args);
         String strObjectIds = (String)programMap.get("objectId");
         //String strRelationship=(String)programMap.get("relName");
         String result=null;
         String strRelationship= "relationship_Products";
         String  strManagedRelationhip = "relationship_Ma";
         StringList excludeList= new StringList();
         StringTokenizer objIDs = new StringTokenizer(strObjectIds,",");
         String toType=null;
         String fromType=null;
         boolean bisTo=false;
         boolean bisFrom=false;

         StringList objSelect = new StringList(2);
         objSelect.addElement(DomainConstants.SELECT_ID);
         DomainObject domObjFeature = new DomainObject(strObjectIds);
         String strModelID = domObjFeature.getInfo(context,"from["+ProductLineConstants.RELATIONSHIP_MANAGED_MODEL+"].to.id");
         DomainObject domObjModel = new DomainObject(strModelID);

         toType=domObjModel.getInfo(context,"to["+PropertyUtil.getSchemaProperty(context,strRelationship)+"].from.type");
         fromType=domObjModel.getInfo(context,"from["+PropertyUtil.getSchemaProperty(context,strRelationship)+"].to.type");

         if(toType!=null){
             bisTo=true;
         }
         else{
             bisFrom=true;
         }
         MapList childObjects=domObjModel.getRelatedObjects(context,
                 PropertyUtil.getSchemaProperty(context,strRelationship),
                 toType==null?fromType:toType,
                 new StringList(DomainConstants.SELECT_ID),
                 null,
                 bisTo,
                 bisFrom,
                (short) 1,
                 DomainConstants.EMPTY_STRING,
                 DomainConstants.EMPTY_STRING);
         //Get all the Products from the database
         MapList lstProductsList = DomainObject.findObjects(context,
                                                    ProductLineConstants.TYPE_PRODUCTS,
                                                    DomainConstants.QUERY_WILDCARD,"",
                                                    objSelect);

        /* for(int cnt=0;cnt<lstProductsList.size();cnt++){

             Map prodMap = (Map)lstProductsList.get(cnt);
             String prodId = (String)prodMap.get(DomainConstants.SELECT_ID);

             for(int i=0;i<childObjects.size();i++){

                 Map tempMap=(Map)childObjects.get(i);
                 String tempID = (String)tempMap.get(DomainConstants.SELECT_ID);
                 if(!tempID.equals(prodId)){
                     excludeList.add(prodId);
                 }

             }
         }*/

         StringList childList= new StringList();
         for(int iCount=0;iCount<childObjects.size();iCount++)
         {

             Map tempMap=(Map)childObjects.get(iCount);
             String tempID = (String)tempMap.get(DomainConstants.SELECT_ID);
             childList.add(tempID);

         }

         for(int icnt=0;icnt<lstProductsList.size();icnt++)
         {

             Map prodMap = (Map)lstProductsList.get(icnt);
             String prodId = (String)prodMap.get(DomainConstants.SELECT_ID);

             if(!childList.contains(prodId))
             {
                 excludeList.add(prodId);
             }

        }


         excludeList.add(strObjectIds);
         return excludeList;
     }


     /**
      * To obtain the list of Object IDs to be excluded from the search for Models
      *
      * @param context- the eMatrix <code>Context</code> object
      * @param args- holds the HashMap containing the following arguments
      * @return  StringList- consisting of the object ids to be excluded from the Search Results
      * @throws Exception if the operation fails
      * @author 3DPLM
      */

     public StringList excludeManagedModels(Context context, String[] args) throws Exception
     {
         StringList excludeList= new StringList();
         StringList objSelect = new StringList(2);
         objSelect.addElement(DomainConstants.SELECT_ID);
         Map programMap = (Map) JPO.unpackArgs(args);
         String strObjectIds = (String)programMap.get("ObjectId");


         //Get all the Products from the database
         MapList lstProductsList = DomainObject.findObjects(context,
                                                    ProductLineConstants.TYPE_MODEL,
                                                    DomainConstants.QUERY_WILDCARD,"",
                                                    objSelect);
         for(int cnt=0;cnt<lstProductsList.size();cnt++){

             Map modelMap = (Map)lstProductsList.get(cnt);
             String modelId = (String)modelMap.get(DomainConstants.SELECT_ID);

             DomainObject domModel = new DomainObject(modelId);
             String productPlatformId = domModel.getInfo(context,"to["+ProductLineConstants.RELATIONSHIP_MANAGED_MODEL+"].from.id");
             if(productPlatformId!=null){
                 excludeList.add(modelId);
             }

         }


         if(strObjectIds!=null && !strObjectIds.equals("") && !strObjectIds.equals("null"))
         {
             excludeList.add(strObjectIds);
         }

         //excludeList.add(strObjectIds);
        return excludeList;
     }


     public boolean isUNTInstalled(Context context,String args[])
     {
         return  FrameworkUtil.isSuiteRegistered(context,"appInstallTypeUnitTracking",false,null,null);
     }

     /**
      * To exclude the Models that are connected to Product Lines
      * @param context
      * @param args
      * @return
      * @throws Exception
      */
     @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
     public StringList excludeConnectedModels(Context context, String[] args) throws Exception
     {
    	 StringList excludeList  = new StringList();
     	 StringList objSelect = new StringList(2);
         objSelect.addElement(DomainConstants.SELECT_ID);
         String strWhereExp = "to["+ProductLineConstants.RELATIONSHIP_PRODUCTLINE_MODELS+"].from.type == '"+ProductLineConstants.TYPE_PRODUCT_LINE+"'";

         MapList modelExcludeList = DomainObject.findObjects(context,ProductLineConstants.TYPE_MODEL,DomainConstants.QUERY_WILDCARD,strWhereExp,objSelect);
         for(int i=0;i<modelExcludeList.size();i++){
             Map tempMap=(Map)modelExcludeList.get(i);
             excludeList.add(tempMap.get(DomainConstants.SELECT_ID));
         }
         return excludeList;
     }

     /**
	     *
		 *
	     * @param context the eMatrix <code>Context</code> object
	     * @param args holds the following input arguments
	     *        0 - id of the business object
	     *        1 - Expression
	     * @return String
	     * @throws Exception if the operation fails		     *
	     */

	    public String getSelecatableVal(Context context, String args[]) throws Exception
	    {
	    	//return MqlUtil.mqlCommand(context, "print bus "+ args[0] +" select "+args[1]+" dump |");
	    	return MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3",args[0],args[1],ProductLineConstants.DELIMITER_PIPE);
	    }

		/**
		* Use as include context program for MP and Products Effectivity commands
		*/
	    @com.matrixone.apps.framework.ui.ProgramCallable
        public MapList getModelContexts(Context context, String[] args) throws Exception {
            try {
                MapList returnMapList = new MapList();
                HashMap programMap = (HashMap) JPO.unpackArgs(args);
                String objectId = (String) programMap.get("objectId");
                if (objectId!=null && !objectId.isEmpty()) {
                    Map<String,String> tempMap = new HashMap<String,String>();
                    tempMap.put(DomainConstants.SELECT_ID, objectId);
                    returnMapList.add(tempMap);
                }
                return returnMapList;
			} catch (Exception e) {
                throw e;
            }
        }


        /**
	     * Method for coonet the company to PL
	     * @param context
	     * @param args
	     * @return
	     * @throws Exception
	     */
	    public int connectCompanyName(Context context, String[] args)
        throws Exception {
		Map programMap = (HashMap) JPO.unpackArgs(args);
		Map paramMap = (HashMap) programMap.get("paramMap");
		Map requestMap = (HashMap) programMap.get("requestMap");
		String strFeatureId = (String) paramMap.get(OBJECT_ID);

		String strOldCompanyName = (String) paramMap.get("Old value");
		String strNewCompanyName = (String) paramMap.get("New Value");
		String strNewCompanyId = (String) paramMap.get("New OID");
		String strOldCompanyOID = (String) paramMap.get("Old OID");
		// Added this for Bug 371941 to get the count of all the Design
		// Responsibilty Rows
		String count = (String) paramMap.get("count");

		if (strOldCompanyName == null) {
			strOldCompanyName = "";
		}
		if (strNewCompanyName == null) {
			strNewCompanyName = "";
		}
		if (strNewCompanyId == null) {
			strNewCompanyId = "";
		}
		if (strOldCompanyOID == null) {
			strOldCompanyOID = "";
		}
		if (strNewCompanyId == null || strNewCompanyId.length() == 0) {
			//strNewCompanyId = (String) paramMap.get("New Value");
			com.matrixone.apps.common.Person person = com.matrixone.apps.common.Person
			.getPerson(context);
	        String companyId = person.getCompanyId(context);
	        if(strNewCompanyName.equals(person.getCompany(context).getName())){
			// setId(companyId);
	        	strNewCompanyId=companyId;
		     }
	        }

		if (strOldCompanyOID.equals(strNewCompanyId)) {
			strNewCompanyId = (String) paramMap.get("New Value");
		}

		if (strNewCompanyId == null) {
			strNewCompanyId = "";
		}

		// Added this condition for Bug 371941 to get the New Organization id of
		// each row based on the count
		if (strNewCompanyId.equals("") && count != null
				&& !count.equalsIgnoreCase("-1")) {
			String[] nCompanyOID = (String[]) requestMap
					.get("Company" + count);
			// Mx377923WIM -added null and empty check for nOrganizationOID
			if ((nCompanyOID != null) && (nCompanyOID.length > 0)) {
				strNewCompanyId = nCompanyOID[0];
			}
		}
		// Start of Add by Enovia MatrixOne for Bug # 311803 on 09-Dec-05
		String strOldCompanyId = "";
		String strObjID = (String) paramMap.get("objectId");
		DomainObject domObj = DomainObject.newInstance(context, strObjID);
		StringList slBusTypes = new StringList();
		slBusTypes.addElement(DomainConstants.SELECT_ID);
		ContextUtil.pushContext(context);

		Map mDesignResponsibility = domObj.getRelatedObject(context,
				ProductLineConstants.RELATIONSHIP_COMPANY_PRODUCT_LINES, false,
				slBusTypes, null);

		if (!((mDesignResponsibility == null) || (mDesignResponsibility
				.equals(null)))) {
			String strDesRespObj = (mDesignResponsibility
					.get(DomainConstants.SELECT_ID)).toString();
			strOldCompanyId = strDesRespObj;
		} else {
			strOldCompanyId = (String) paramMap.get("Old OID");
		}
		ContextUtil.popContext(context);

		boolean bHasReadAccess;

		try {
			if (strOldCompanyId == null
					|| strOldCompanyId.equals("null")
					|| strOldCompanyId.equals(""))
				bHasReadAccess = true;
			else
				bHasReadAccess = emxProduct_mxJPO.hasAccessOnProject(context,
						strOldCompanyId);
		} catch (Exception e) {
			bHasReadAccess = false;
		}

		if (bHasReadAccess) {
			// End of Add by Enovia MatrixOne for Bug # 311803 on 09-Dec-05

			// fix issue with strOldOrganizationId being null and throwing null
			// ptr errors on check for empty string
			if (strOldCompanyId == null)
				strOldCompanyId = "";

			// Begin of add by Enovia MatrixOne on 18-Apr-05 for Bug# 300548
			if (!strNewCompanyId.equals("")
					&& !strOldCompanyId.equals("")
					&& strOldCompanyId.equals(strNewCompanyId)) {
				return 0;
			} else {
				// End of add by Enovia MatrixOne on 18-Apr-05 for Bug# 300548
				String strDesignRespRelationship = ProductLineConstants.RELATIONSHIP_COMPANY_PRODUCT_LINES;

				setId(strFeatureId);
				List organizationList = new MapList();
				if (strOldCompanyName == null
						|| "null".equals(strOldCompanyName))
					strOldCompanyName = "";

				List lstObjectSelects = new StringList(
						DomainConstants.SELECT_ID);
				List lstRelSelects = new StringList(
						DomainConstants.SELECT_RELATIONSHIP_ID);

				// Begin of Modify by Praveen, Enovia MatrixOne for Bug #300094
				// 03/15/2005
				String strOrganizationType = DomainConstants.QUERY_WILDCARD;
				// End of Modify by Praveen, Enovia MatrixOne for Bug #300094
				// 03/15/2005

				// Modified by Enovia MatrixOne on 18-Apr-05 for Bug# 300548
				StringBuffer sbWhereCondition = new StringBuffer(25);
				if (strOldCompanyId != null
						&& !strOldCompanyId.equals("")) {
					sbWhereCondition = sbWhereCondition
							.append(DomainConstants.SELECT_ID);
					sbWhereCondition = sbWhereCondition.append("==");
					sbWhereCondition = sbWhereCondition.append("\"");
					sbWhereCondition = sbWhereCondition
							.append(strOldCompanyId);
					sbWhereCondition = sbWhereCondition.append("\"");
				}

				// Added for RDO Fix
				// Changing the context to super user
				ContextUtil.pushContext(context);

				organizationList = getRelatedObjects(context,
						strDesignRespRelationship, strOrganizationType,
						(StringList) lstObjectSelects,
						(StringList) lstRelSelects, true, true, (short) 1,
						sbWhereCondition.toString(),
						DomainConstants.EMPTY_STRING);

				if (organizationList != null && !organizationList.isEmpty()) {
					String strRelId = (String) ((Map) organizationList.get(0))
							.get(DomainConstants.SELECT_RELATIONSHIP_ID);
					// Begin of add by Yukthesh, Enovia MatrixOne for Bug
					// #311540 on Nov 10,2005
					// Turn off the matrix triggers
					MqlUtil.mqlCommand(context, "trigger off", true);

					try {
						// Disconnecting the existing relationship
						DomainRelationship.disconnect(context, strRelId);
					} finally {
						// Turn on the matrix triggers
						MqlUtil.mqlCommand(context, "trigger on", true);
					}
					// End of add by Yukthesh, Enovia MatrixOne for Bug #311540
					// on Nov 10,2005
				}

				// Added for RDO Fix
				// Changing the context back to the context user
				ContextUtil.popContext(context);

				if (strNewCompanyId == null
						|| "null".equals(strNewCompanyId))
					strNewCompanyId = "";

				if (!strNewCompanyId.equals("")) {
			        setId(strNewCompanyId);
					DomainObject domainObjectToType = newInstance(context,
							strFeatureId);

					// Added for RDO Fix
					// Changing the context to super user
					ContextUtil.pushContext(context);

					if(mxType.isOfParentType(context,domainObjectToType.getInfo(context,DomainObject.SELECT_TYPE),
							ProductLineConstants.TYPE_PRODUCTS)){
						strDesignRespRelationship = ProductLineConstants.RELATIONSHIP_COMPANY_PRODUCT;
					}

					DomainRelationship.connect(context, this,
							strDesignRespRelationship, domainObjectToType);


					// Added for RDO Fix
					// Changing the context back to the context user
					ContextUtil.popContext(context);
				}

				// Added by Enovia MatrixOne on 18-Apr-05 for Bug# 300548
			}
			return 0;

			// Start of Add by Enovia MatrixOne for Bug # 311803 on 09-Dec-05
		}// end of if for check of read access
		else {
			return 0;
		}
		// End of Add by Enovia MatrixOne for Bug # 311803 on 09-Dec-05

}
    /**
	     * method to exclude  the productline which are already connected
	     * @param context
	     * @param args
	     * @return
	     * @throws Exception
	     */
	    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
	    public StringList excludeAvailableProductLine(Context context, String [] args)
		   throws FrameworkException
		   {
	    	   StringList finalList=new StringList();
			   StringList strList=new StringList();

			   try{
				   Map programMap = (Map) JPO.unpackArgs(args);
			       String strSourceObjectId = (String) programMap.get("objectId");
			       DomainObject domContextObj = new DomainObject(strSourceObjectId);
			       String txtType = domContextObj.getInfo(context,DomainConstants.SELECT_TYPE);
			       String strWhereExp = DomainConstants.EMPTY_STRING;
			       String strRelPattern = "";
			       short level = 1;

			      String strObjectPattern = ProductLineConstants.TYPE_PRODUCT_LINE;
			      strRelPattern = ProductLineConstants.RELATIONSHIP_SUB_PRODUCT_LINES;


			       StringList objectSelects = new StringList(DomainObject.SELECT_ID);
			       StringList relSelects = new StringList(DomainRelationship.SELECT_ID);

			       short limit = 0;
			       MapList relatedFromPlList = new MapList();

			       relatedFromPlList = domContextObj.getRelatedObjects(context,
															               strRelPattern,
															               strObjectPattern,
															               objectSelects,
															               relSelects,
															               false,
															               true,
															               level,
															               strWhereExp,
															               strWhereExp,
															               limit);

			       //add the context PL
			       finalList.add(strSourceObjectId);

			       for(int i=0;i<relatedFromPlList.size();i++)
			       {
			           Map mapFeatureObj = (Map) relatedFromPlList.get(i);
			           if(mapFeatureObj.containsKey(objectSelects.get(0)))
			           {
			               Object idsObject = mapFeatureObj.get(objectSelects.get(0));
			               strList=ProductLineCommon.convertObjToStringList(context,idsObject);
			               finalList.addAll(strList);
			           }
			       }
			   }
			   catch (Exception e) {
					throw new FrameworkException(e);
			   }

		       return finalList;
		   }
	 /**
	  * Method to check if ECHproduct is installed
	  *
	  * @param context
	  * @param args
	  * @return	true - if ECH is installed
	  * 		false - if ECH is not Installed
	  * @exception throws FrameworkException
	  * @since R213
	  */
     public boolean isECHInstalled(Context context,String args[]) throws FrameworkException
     {
         return  FrameworkUtil.isSuiteRegistered(context,"appVersionEnterpriseChange",false,null,null);
     }
     /**
    This column JPO method is used to get the RDO for a object. If the context
    * user has read access on the RDO object then it is hyperlinked
    * on SB page otherwise only name is returned. Also if the context user
    * doesn't have the show access on the RDo object then context is changed
    * to super user to retrieve the RDO name.    *
    * @param context the eMatrix <code>Context</code> object
    * @param args - String array containing following packed HashMap
    *                       with following elements:
    *                       lstobjectList -  list containig the object id.
    * @return String - The program HTML output containing the RDO name and Link.
    * @throws Exception if the operation fails
    * @since R213
      */
     public Vector getDesignResponsibilitySB(Context context, String[] args) throws Exception{
    	 String strPolicyProduct = PropertyUtil.getSchemaProperty(context,SYMBOLIC_policy_Product);

    	 String strStateReview = FrameworkUtil.lookupStateName(      context,
    			 strPolicyProduct,
    			 SYMB_state_Review
    			 );
    	 String strStateRelease = FrameworkUtil.lookupStateName(     context,
    			 strPolicyProduct,
    			 SYMB_state_Release
    			 );
    	 String strStateObsolete= FrameworkUtil.lookupStateName(     context,
    			 strPolicyProduct,
    			 SYMB_state_Obsolete
    			 );
    	 //Get the object id of the context object
    	 Map programMap = (HashMap) JPO.unpackArgs(args);
    	 Map paramMap = (Map) programMap.get("paramList");
    	 List lstobjectList = (MapList) programMap.get("objectList");
    	 String strMode = (String)paramMap.get("mode");
    	 String suiteDir = (String) paramMap.get("SuiteDirectory");
    	 String suiteKey = (String) paramMap.get("suiteKey");
    	 Map objectMap = null;
    	 Vector result=new  Vector();
    	 Map fieldMap = (HashMap) programMap.get("columnMap");
    	 String strFieldName = (String)fieldMap.get("name");
    	 //Form the select expressions for getting the RDO name and RDO id.
    	 StringBuffer sbRDONameSelect  = new StringBuffer("to[");
    	 sbRDONameSelect.append(ProductLineConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY);
    	 sbRDONameSelect.append("].from.");
    	 sbRDONameSelect.append(DomainConstants.SELECT_NAME);
    	 StringBuffer sbRDOIdSelect  = new StringBuffer("to[");
    	 sbRDOIdSelect.append(ProductLineConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY);
    	 sbRDOIdSelect.append("].from.");
    	 sbRDOIdSelect.append(DomainConstants.SELECT_ID);
    	 StringBuffer sbRDOTypeSelect  = new StringBuffer("to[");
    	 sbRDOTypeSelect.append(ProductLineConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY);
    	 sbRDOTypeSelect.append("].from.");
    	 sbRDOTypeSelect.append(DomainConstants.SELECT_TYPE);
    	 //CODE CHANGES
    	 String exportFormat = "";
    	 HashMap requestMap = (HashMap) programMap.get("requestMap");
    	 if(requestMap!=null && requestMap.containsKey("reportFormat")){
    		 exportFormat = (String)requestMap.get("reportFormat");
    	 } else if (paramMap!=null && paramMap.containsKey("reportFormat")){
    		 exportFormat = (String)paramMap.get("reportFormat");
    	 }
    	 StringList lstObjSelects = new StringList();
    	 lstObjSelects.add(sbRDONameSelect.toString());
    	 lstObjSelects.add(sbRDOIdSelect.toString());
    	 lstObjSelects.add(sbRDOTypeSelect.toString());
    	 String strRDOId = "";
    	 String strRDOName = "";
    	 StringBuffer sbBuffer  = new StringBuffer();
    	 //Begin of Add by Rashmi, Enovia MatrixOne for bug 301411 Date:4/13/2005
    	 String strTempIcon = DomainConstants.EMPTY_STRING;
    	 String strRDOType =  DomainConstants.EMPTY_STRING;
    	 String strTypeIcon = DomainConstants.EMPTY_STRING;
    	 for (int j=0; j<lstobjectList.size(); j++)
    	 {
    		 objectMap = (Map) lstobjectList.get(j);
    		 String strObjectId=(String)objectMap.get(DomainConstants.SELECT_ID);
    		 //Get the RDO id and name by changing the context to super user
    		 DomainObject domObj = DomainObject.newInstance(context, strObjectId);
    		 ContextUtil.pushContext(context);
    		 Map mapRDO = (Map)domObj.getInfo(context,lstObjSelects);
    		 ContextUtil.popContext(context);
    		 //If RDO is set for this object then check whether the context user has read
    		 //access on the RDO object. If yes then hyperlink the RDO name to its
    		 if(mapRDO!=null&&mapRDO.size()>0){
    			 strRDOName = (String) mapRDO.get(sbRDONameSelect.toString());
    			 if (mapRDO.get(sbRDOIdSelect.toString()) instanceof StringList)
    			 {
    				 StringList strRDOListId = (StringList) mapRDO.get(sbRDOIdSelect.toString());
    				 strRDOId =  (String)strRDOListId.get(0);
    			 } else {
    				 strRDOId = (String) mapRDO.get(sbRDOIdSelect.toString());

    			 }
    			 strRDOType = (String) mapRDO.get(sbRDOTypeSelect.toString());
    		 }else{
    			 strRDOName = "";
    			 strRDOId = "";
    			 strRDOType = "";
    		 }
    		 if (strRDOType!=null && !strRDOType.equals("") && !strRDOType.equalsIgnoreCase("null")){
    			 strTypeIcon = UINavigatorUtil.getTypeIconFromCache(strRDOType);
    			 strTypeIcon = "images/"+strTypeIcon;            
    	     }
    		 if(strRDOName!=null && !strRDOName.equals("") && !strRDOName.equalsIgnoreCase("null")){
    			 boolean bHasReadAccess =false;
    			 //Checking the access to Edit Design Responsibility
    			 String strCtxUser = context.getUser();
    			 String strOwner = (String)domObj.getInfo(context,DomainConstants.SELECT_OWNER);
    			 boolean hasRoleProductManager  = false;
    			 boolean hasRoleSystemEngineer  = false;
    			 boolean bIsOwner               = false;
    			 Person ctxPerson = new Person (strCtxUser);
    			 hasRoleProductManager = ctxPerson.isAssigned(context,"Product Manager");
    			 hasRoleSystemEngineer = ctxPerson.isAssigned(context,"System Engineer");
    			 if ( strCtxUser != null && !"".equals(strCtxUser)) {
    				 if (strOwner!= null && !"".equals(strOwner)) {
    					 if (strOwner.equals(strCtxUser)) {
    						 bIsOwner = true;
    					 }
    				 }
    			 }
    			 
    			 try {
    				 if ( (strRDOId==null || strRDOId.equals("")) &&
    						 ( bIsOwner || hasRoleProductManager || hasRoleSystemEngineer )){
    					 bHasReadAccess = true;
    				 }
    				 else {
    					 boolean hasAccessOnProject = emxProduct_mxJPO.hasAccessOnProject(context,strRDOId);
    					 if (hasAccessOnProject && ( bIsOwner || hasRoleProductManager || hasRoleSystemEngineer ))
    					 {
    						 bHasReadAccess = true;
    					 }
    				 }
    			 } catch (Exception e) {
    				 bHasReadAccess = false;
    			 }
    			 
    			 if(bHasReadAccess){
    				 //CODE changes
    				 StringBuffer sbHref  = new StringBuffer();
    				 if("CSV".equalsIgnoreCase(exportFormat)){
    					 sbHref.append(strRDOName);
    				 }else{
    					 //XSSOK- Deprecated
    					 sbHref.append("<a href=\"JavaScript:showDetailsPopup('../common/emxTree.jsp?emxSuiteDirectory=");
    					 sbHref.append(suiteDir);
    					 sbHref.append("&amp;suiteKey=");
    					 sbHref.append(suiteKey);
    					 sbHref.append("&amp;objectId=");
    					 sbHref.append(strRDOId);
    					 sbHref.append("', '450', '300', 'true', 'popup')\">");
    					 sbHref.append(" <img border=\"0\" src=\"");
    					 sbHref.append(strTypeIcon);
    					 sbHref.append("\" /> ");
    					 sbHref.append(XSSUtil.encodeForHTML(context,strRDOName));
    					 sbHref.append("</a>");
    				 }
					 result.add(sbHref.toString());
    			 }else{
    				 sbBuffer.delete(0, sbBuffer.length());
    				 //CODE changes
    				 if("CSV".equalsIgnoreCase(exportFormat)){
    					 sbBuffer.append(strRDOName);
    				 }else{
    					 //XSSOK- Deprecated
    					 sbBuffer.append("<img border=\"0\" src=\"");
    					 sbBuffer.append(strTypeIcon);
    					 sbBuffer.append("\"></img>");
    					 sbBuffer.append(SYMB_SPACE);
    					 sbBuffer.append(strRDOName);
    				 }
    				 result.add(sbBuffer.toString());
    				 //End of modify for bug 301411
    			 }
    		 }
    		 else{
    			 result.add("");

    		 }
    	 }
    	 return result;
     }
     
     /**
      * This method used to enable the particular RDO column cell for edit on the basis of access of user
      * @param context
      * @param args
      * @return
      * @throws Exception
      */
public StringList editAccess(Context context, String[] args)throws Exception{
	String strPolicyProduct = PropertyUtil.getSchemaProperty(context,SYMBOLIC_policy_Product);

    String strStateReview = FrameworkUtil.lookupStateName(      context,
                                                                strPolicyProduct,
                                                                SYMB_state_Review
                                                          );
    String strStateRelease = FrameworkUtil.lookupStateName(     context,
                                                                strPolicyProduct,
                                                                SYMB_state_Release
                                                          );
    String strStateObsolete= FrameworkUtil.lookupStateName(     context,
                                                                strPolicyProduct,
                                                                SYMB_state_Obsolete
                                             );
    //Get the object id of the context object
         Map programMap = (HashMap) JPO.unpackArgs(args);
         Map paramMap = (Map) programMap.get("paramList");
         List lstobjectList = (MapList) programMap.get("objectList");
        Map objectMap = null;
        StringList result=new  StringList();
//Form the select expressions for getting the RDO name and RDO id.
StringBuffer sbRDONameSelect  = new StringBuffer("to[");
sbRDONameSelect.append(ProductLineConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY);
sbRDONameSelect.append("].from.");
sbRDONameSelect.append(DomainConstants.SELECT_NAME);

StringBuffer sbRDOIdSelect  = new StringBuffer("to[");
sbRDOIdSelect.append(ProductLineConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY);
sbRDOIdSelect.append("].from.");
sbRDOIdSelect.append(DomainConstants.SELECT_ID);
StringBuffer sbRDOTypeSelect  = new StringBuffer("to[");
sbRDOTypeSelect.append(ProductLineConstants.RELATIONSHIP_DESIGN_RESPONSIBILITY);
sbRDOTypeSelect.append("].from.");
sbRDOTypeSelect.append(DomainConstants.SELECT_TYPE);
String exportFormat = "";
HashMap requestMap = (HashMap) programMap.get("requestMap");
if(requestMap!=null && requestMap.containsKey("reportFormat")){
	exportFormat = (String)requestMap.get("reportFormat");
}

StringList lstObjSelects = new StringList();
lstObjSelects.add(sbRDONameSelect.toString());
lstObjSelects.add(sbRDOIdSelect.toString());
        lstObjSelects.add(sbRDOTypeSelect.toString());
String strRDOId = "";
String strRDOName = "";
        StringBuffer sbBuffer  = new StringBuffer();
        //Begin of Add by Rashmi, Enovia MatrixOne for bug 301411 Date:4/13/2005
        String strTempIcon = DomainConstants.EMPTY_STRING;
        String strRDOType =  DomainConstants.EMPTY_STRING;
        String strTypeIcon = DomainConstants.EMPTY_STRING;
for (int j=0; j<lstobjectList.size(); j++)
{
objectMap = (Map) lstobjectList.get(j);
String strObjectId=(String)objectMap.get(DomainConstants.SELECT_ID);
//Get the RDO id and name by changing the context to super user
DomainObject domObj = DomainObject.newInstance(context, strObjectId);
ContextUtil.pushContext(context);
Map mapRDO = (Map)domObj.getInfo(context,lstObjSelects);
ContextUtil.popContext(context);
//If RDO is set for this object then check whether the context user has read
//access on the RDO object. If yes then hyperlink the RDO name to its
    if(mapRDO!=null&&mapRDO.size()>0){
        strRDOName = (String) mapRDO.get(sbRDONameSelect.toString());
        //Begin of modify by Enovia MatrixOne on 1-June-05 for bug 304576 reopened
        if (mapRDO.get(sbRDOIdSelect.toString()) instanceof StringList){
            StringList strRDOListId = (StringList) mapRDO.get(sbRDOIdSelect.toString());
            strRDOId =  (String)strRDOListId.get(0);
        } else {
            strRDOId = (String) mapRDO.get(sbRDOIdSelect.toString());
        }
      strRDOType = (String) mapRDO.get(sbRDOTypeSelect.toString());
    }
    if(strRDOName==null || strRDOName.equalsIgnoreCase("null") || strRDOName.equals("")){
        strRDOName = "";
        strRDOId = "";
        strRDOType = "";
    }
    boolean bHasReadAccess =false;
   // Checking the access to Edit Design Responsibility
    String strCtxUser = context.getUser();
    String strOwner = (String)domObj.getInfo(context,DomainConstants.SELECT_OWNER);
    boolean hasRoleProductManager  = false;
    boolean hasRoleSystemEngineer  = false;
    boolean bIsOwner               = false;
    Person ctxPerson = new Person (strCtxUser);
    hasRoleProductManager = ctxPerson.isAssigned(context,"Product Manager");
    hasRoleSystemEngineer = ctxPerson.isAssigned(context,"System Engineer");
    if ( strCtxUser != null && !"".equals(strCtxUser))
    {
        if (strOwner!= null && !"".equals(strOwner))
        {
            if (strOwner.equals(strCtxUser))
            {
                bIsOwner = true;
            }
        }
    }
    try
    {

        if ( (strRDOId==null || strRDOId.equals("")) &&
            ( bIsOwner || hasRoleProductManager || hasRoleSystemEngineer )){
            bHasReadAccess = true;
        }
        else {
            boolean hasAccessOnProject = emxProduct_mxJPO.hasAccessOnProject(context,strRDOId);
            if (hasAccessOnProject && ( bIsOwner || hasRoleProductManager || hasRoleSystemEngineer ))
            {
                bHasReadAccess = true;
            }
        }
    } catch (Exception e) {
        bHasReadAccess = false;
    }
    // for making StringList for edit access
    if (!bHasReadAccess)
    {
       result.add(false);
    } else {
    	result.add(true);
    }
}
	return result;
}

/**
 * Returns true if the PRG is installed otherwise false.
 * @mx.whereUsed This method will be called from part property pages
 * @mx.summary   This method check whether PRG is installed or not, this method can be used as access program to show/hide the columns from Product summary page
 * @param context the eMatrix <code>Context</code> object.
 * @return boolean true or false based condition.
 * @throws Exception if the operation fails.
 * @since R215
 */
	public boolean isPRGInstalled(Context context,String[] args) throws Exception
	{     
		boolean isPRGInstalled = ProductLineUtil.isPRGInstalled(context);
        return  isPRGInstalled;
	}

	public List getHigherRevisionIconForProductContext(Context context,
			String[] args) throws Exception {

		Map programMap = (HashMap) JPO.unpackArgs(args);
		MapList relBusObjPageList = (MapList) programMap.get(OBJECT_LIST);
		Map paramList = (HashMap) programMap.get("paramList");
		String reportFormat = (String) paramList.get("reportFormat");
		String strLanguage = context.getSession().getLanguage();
		int iNumOfObjects = relBusObjPageList.size();
		// The List to be returned
		List lstHigherRevExists = new Vector(iNumOfObjects);
		Map objectMap = new HashMap();
		Iterator objectListItr = relBusObjPageList.iterator();
		int iCount;
		// Reading the tooltip from property file.
		String strTooltipHigherRevExists =	EnoviaResourceBundle.getProperty(context, SUITE_KEY,ICON_TOOLTIP_HIGHER_REVISION_EXISTS,context.getSession().getLanguage());
		String strHigherRevisionIconTag = "";
		String strIcon = EnoviaResourceBundle.getProperty(context,"emxComponents.HigherRevisionImage");
		// Iterating through the list of objects to generate the program HTML
		// output for each object in the table
		for (iCount = 0; iCount < iNumOfObjects; iCount++) {		
			while (objectListItr.hasNext()) {
				objectMap = (Map) objectListItr.next();
				String nextRevExist = (String) objectMap.get("next");

				if (nextRevExist!=null
						&& !("".equals(nextRevExist))) {
					if (reportFormat != null
							&& !("null".equalsIgnoreCase(reportFormat))
							&& reportFormat.length() > 0) {
						lstHigherRevExists.add(strTooltipHigherRevExists);
					} else {
						strHigherRevisionIconTag = "<img src=\"../common/images/"
								+ strIcon
								+ "\" border=\"0\"  align=\"middle\" "
								+ "TITLE=\""
								+ " "
								+ XSSUtil.encodeForHTMLAttribute(context,strTooltipHigherRevExists)
								+ "\"" + "/>";
					}
				}else if(objectMap.containsKey("parentLevel")){
				String  id=(String)	objectMap.get(DomainConstants.SELECT_ID);
					DomainObject domObj = new DomainObject(id);
					if(!domObj.isLastRevision(context)){					
							if (reportFormat != null
									&& !("null".equalsIgnoreCase(reportFormat))
									&& reportFormat.length() > 0) {
								lstHigherRevExists.add(strTooltipHigherRevExists);
							} else {
								strHigherRevisionIconTag = "<img src=\"../common/images/"
										+ strIcon
										+ "\" border=\"0\"  align=\"middle\" "
										+ "TITLE=\""
										+ " "
										+ XSSUtil.encodeForHTMLAttribute(context,strTooltipHigherRevExists)
										+ "\"" + "/>";
							}	
					}
					
				} else {
					strHigherRevisionIconTag = " ";
				}
				lstHigherRevExists.add(strHigherRevisionIconTag);
			}

		}
		return lstHigherRevExists;
	}	
	
	public boolean hideEditLink (Context context,String[] args) throws FrameworkException { 
  		boolean isFTRUser=false;
  		String Licenses[] = {"ENO_FTR_TP","ENO_PLC_TP"};	    
  		try {
  			FrameworkLicenseUtil.checkLicenseReserved(context,Licenses);
		    isFTRUser = true;
		}catch (Exception e){
			isFTRUser = false;
		}		
  		if(!isFTRUser){
  			return true;
  		}
  		return false;	 
  	 }
	
	public boolean showEditLink (Context context,String[] args) throws FrameworkException { 
  		boolean isFTRUser=false;
  		String Licenses[] = {"ENO_FTR_TP","ENO_PLC_TP"};
  		try {
  			FrameworkLicenseUtil.checkLicenseReserved(context,Licenses);
		    isFTRUser = true;
		}catch (Exception e){
			isFTRUser = false;
		}
  		if(isFTRUser){
  			return true;
  		}
  		return false;
  	 }
	
	public boolean isPRGInstalledHideEditLink(Context context,String[] args) throws Exception
	{     
		boolean isPRGInstalled = ProductLineUtil.isPRGInstalled(context);
		boolean isFTRUser=false;
	  		String Licenses[] = {"ENO_FTR_TP","ENO_PLC_TP"};   		    
	  		try {
	  			FrameworkLicenseUtil.checkLicenseReserved(context,Licenses);
			    isFTRUser = true;
			}catch (Exception e){
				isFTRUser = false;
			}
	  		if(isPRGInstalled &&!isFTRUser){
	  			return true;
	  		}
	    return  false;
	}
	
	public boolean isPRGInstalledShowEditLink(Context context,String[] args) throws Exception
	{     
		boolean isPRGInstalled = ProductLineUtil.isPRGInstalled(context);
		boolean isFTRUser=false;
  		String Licenses[] = {"ENO_FTR_TP","ENO_PLC_TP"};   		    
  		try {
  			FrameworkLicenseUtil.checkLicenseReserved(context,Licenses);
		    isFTRUser = true;
		}catch (Exception e){
			isFTRUser = false;
		}
  		if(isPRGInstalled && isFTRUser){
  			return true;
  		}
        return  false;
	}

	
	/**
	 * The Utility method return true if context user has "ENO_FTR_TP" License,
	 * other wise return false
	 * 
	 * @param context
	 * @return true if context user has "ENO_FTR_TP" License,else return false
	 * 
	 */
	public static boolean isFTRUser(Context context,String[] args) {
		try {
			String[] arrTPsName = { "ENO_FTR_TP" };
			FrameworkLicenseUtil.checkLicenseReserved(context, arrTPsName);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * The Utility method return true if context user has "ENO_FTR_TP,ENO_CFE_TP" License,
	 * other wise return false
	 * 
	 * @param context
	 * @return true if context user has "ENO_FTR_TP,ENO_CFE_TP" License,else return false
	 * 
	 */
	public static boolean isFTRCFEUser(Context context,String[] args) {
		try {
			String[] arrTPsName = { "ENO_FTR_TP","ENO_CFE_TP" };
			FrameworkLicenseUtil.checkLicenseReserved(context, arrTPsName);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	* This method is used to refresh the Structure Tree for object add and remove
	* 
	* @param context
	* @param String[]
	* @return Map
	* @throws FrameworkException
	*/
	@com.matrixone.apps.framework.ui.PostProcessCallable
	public static Map refreshTree(Context context, String[] args) throws Exception{
		Map returnMap = new HashMap();
		returnMap.put("Action", "execScript");
		returnMap.put("Message","{ main:function __main(){refreshTreeForAddRemove(xmlResponse)}}");
		return returnMap;
	}		
	}//End of class

