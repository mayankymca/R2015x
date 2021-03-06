/*
 ** ${CLASS:MarketingFeature}
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved.
 ** This program contains proprietary and trade secret information of
 ** Dassault Systemes.
 ** Copyright notice is precautionary only and does not evidence any actual
 ** or intended publication of such program
 */

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.MQLCommand;
import matrix.db.RelationshipType;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.DebugUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PolicyUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.framework.ui.UIUtil;
/**
 * The <code>emxBOMPartManagementBase</code> class contains implementation code for emxBOMPartManagement.
 *
 *@version EC 9.5.JCI.0 - Copyright (c) 2002, MatrixOne, Inc.
 */

public class emxBOMPartManagementBase_mxJPO extends emxCommonPart_mxJPO {

	public static String EBOM_FROZEN_STATES = null;

	/** emxContextUtil for push/pop context */
	static protected emxContextUtil_mxJPO contextUtil = null;

	/** relationship "Manufacturer Equivalent". */
	public static final String RELATIONSHIP_MANUFACTURER_EQUIVALENT_HISTORY = PropertyUtil
			.getSchemaProperty("relationship_ManufacturerEquivalentHistory");

	public static final String RELATIONSHIP_GBOM = PropertyUtil
			.getSchemaProperty("relationship_GBOM");
	public static final String RELATIONSHIP_CHANGE_AFFECTED_ITEM = PropertyUtil.getSchemaProperty("relationship_ChangeAffectedItem");
	public static final String RELATIONSHIP_IMPLEMENTED_ITEM = PropertyUtil.getSchemaProperty("relationship_ImplementedItem");
	public static final String RELATIONSHIP_CHANGE_ACTION = PropertyUtil.getSchemaProperty("relationship_ChangeAction");
	public static final String TYPE_CHANGE_ACTION = PropertyUtil.getSchemaProperty("type_ChangeAction");
	public static final String TYPE_CHANGE_ORDER = PropertyUtil.getSchemaProperty("type_ChangeOrder");
	public static final String POLICY_CHANGE_ACTION = PropertyUtil.getSchemaProperty("policy_ChangeAction");
	public static final String POLICY_CHANGE_ORDER = PropertyUtil.getSchemaProperty("policy_ChangeOrder");
	
	public static final String RELATIONSHIP_ASSIGNED_AFFECTED_ITEM =
    	PropertyUtil.getSchemaProperty("relationship_AssignedAffectedItem");
	
	/** The Manufacturer Equivalent History Relationship Object. */
	static protected final RelationshipType _mepHistory = new RelationshipType(
			RELATIONSHIP_MANUFACTURER_EQUIVALENT_HISTORY);

	/** The EBOM History Relationship Object. */
	static protected final RelationshipType _ebomHistory = new RelationshipType(
			DomainConstants.RELATIONSHIP_EBOM_HISTORY);

	static final int LT = 0;
    static final int GT = 1;
    static final int EQ = 2;
    static final int LE = 3;
    static final int GE = 4;
    static final int NE = 5;

    
     /**
       * Constructor.
       *
       * @param context the eMatrix <code>Context</code> object.
       * @param args holds no arguments.
       * @throws Exception if the operation fails.
       * @since EC 9.5.JCI.0.
       */

	public emxBOMPartManagementBase_mxJPO(Context context, String[] args)
			throws Exception {

		super(context, args);
		contextUtil = new emxContextUtil_mxJPO(context, null);

	}


	/**
	 * Floats the EBOM connections from previously released Part to the newly
	 * released Part. The following steps are performed: - Creates an
	 * "EBOM History" connection between this child and its released parent
	 * assemblies. - Copies attributes from the "EBOM" connection to the
	 * "EBOM History" connection. - Floats the "EBOM" connections from parent
	 * assemblies to the new released child component. - Sets the End
	 * Effectivity date on the "EBOM History" connection to the date the child
	 * is released (-1 second). - Sets the Start Effectivity date on "EBOM"
	 * connection to the date the child is released.
	 * 
	 * @param context
	 *            the eMatrix <code>Context</code> object.
	 * @param args
	 *            holds the following input arguments: 0 - the symbolic name of
	 *            the relationship to float, either
	 *            relationship_ManufacturerEquivalent or relationship_EBOM
	 *            (default if none specified).
	 * @throws Exception
	 *             if the operation fails.
	 * @since EC 9.5.JCI.0.
	 * @trigger PolicyECPartStateReviewPromoteAction.
	 */
    public void floatEBOMToEnd(Context context, String[] args) throws Exception
    {
    
        SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.SECOND, -1);
    Date date = cal.getTime();
    String endEffectivityDate = _mxDateFormat.format(date);
	String startEffectivityDate = _mxDateFormat.format(new Date());
	Pattern relPattern        = new Pattern("");
	String  RELATIONSHIP_GBOM_TO     = PropertyUtil.getSchemaProperty(context,"relationship_GBOMTo");
	String sLatestRev = getInfo(context,"last.id");
	//checking float to older revision key is disable or not 
	String sFloatToOlderRevision= FrameworkProperties.getProperty("eServiceEngineeringCentral.ManagePartFloatwhenRevisedoneSameChange");
	String completeState = PropertyUtil.getSchemaProperty(context, "policy",POLICY_CHANGE_ORDER, "state_Complete");
	String sImplementedItemCO="to["+RELATIONSHIP_IMPLEMENTED_ITEM+"].from.to["+RELATIONSHIP_CHANGE_ACTION+"|from.current != \""+completeState+"\"&& from.type == '"+TYPE_CHANGE_ORDER+"'].from.id";;
	String sAffectedItemCO ="to["+RELATIONSHIP_CHANGE_AFFECTED_ITEM+"].from.to["+RELATIONSHIP_CHANGE_ACTION+"|from.current != \""+completeState+"\"&& from.type == '"+TYPE_CHANGE_ORDER+"'].from.id";
	String sNextRevConnectedImplementedCO=null;
	String sNextRevConnectedAffectedItemCO =null;
	String sImplCO ="to["+RELATIONSHIP_IMPLEMENTED_ITEM+"].from.to["+RELATIONSHIP_CHANGE_ACTION+"].from.id";
	String sAICO ="to["+RELATIONSHIP_CHANGE_AFFECTED_ITEM+"].from.to["+RELATIONSHIP_CHANGE_ACTION+"].from.id";
	String sNextRevConnectedImplCO =null;
	String sNextRevConnectedAICO =null;
		
	if("True".equalsIgnoreCase(sFloatToOlderRevision)){		
		sNextRevConnectedImplementedCO="next.to["+RELATIONSHIP_IMPLEMENTED_ITEM+"].from.to["+RELATIONSHIP_CHANGE_ACTION+"|from.current != \""+completeState+"\"].from.id";
	    sNextRevConnectedAffectedItemCO="next.to["+RELATIONSHIP_CHANGE_AFFECTED_ITEM+"].from.to["+RELATIONSHIP_CHANGE_ACTION+"|from.current != \""+completeState+"\"].from.id";
	    sNextRevConnectedImplCO="next.to["+RELATIONSHIP_IMPLEMENTED_ITEM+"].from.to["+RELATIONSHIP_CHANGE_ACTION+"].from.id";
	    sNextRevConnectedAICO="next.to["+RELATIONSHIP_CHANGE_AFFECTED_ITEM+"].from.to["+RELATIONSHIP_CHANGE_ACTION+"].from.id";
	}
	String objectId=this.getId(context);
	StringList slConnectedCO=new StringList(2);
    slConnectedCO.add(sImplementedItemCO);
    slConnectedCO.add(sAffectedItemCO);
    DomainObject dObj=new DomainObject(objectId);
    Map mCO=dObj.getInfo(context, slConnectedCO);
      
    String sChangeOrder = UIUtil.isNotNullAndNotEmpty((String)mCO.get(sImplCO))?(String)mCO.get(sImplCO):(String)mCO.get(sAICO);
	contextUtil.pushContext(context, null);
    try
    {
        Map attributeMap = null;
        Map m = null;
        DomainRelationship historyRel = null;
        String rel_SymbolicName = args[0];
	if (!(rel_SymbolicName != null && rel_SymbolicName.equals("relationship_ManufacturerEquivalent"))){
         relPattern.addPattern(DomainConstants.RELATIONSHIP_EBOM);
         relPattern.addPattern(RELATIONSHIP_GBOM_TO);
		 relPattern.addPattern(RELATIONSHIP_GBOM);
      }
        String strPreviousId = "";
        //Multitenant
        String checkPartVersion= EnoviaResourceBundle.getProperty(context, "emxBOMPartManagement.Check.PartVersion");
        if (checkPartVersion != null && "TRUE".equalsIgnoreCase(checkPartVersion))
        {
            String sRelPartVersion = PropertyUtil.getSchemaProperty(context,"relationship_PartVersion");
	     	String objectSelect="relationship["+sRelPartVersion+"].from.id";
	     	strPreviousId = getInfo(context,objectSelect);
        }
        else
        {
            strPreviousId = getInfo(context,"previous.id");
        }
        if ( strPreviousId == null || "".equals(strPreviousId)){
            return;
         }

			DomainObject prevRevPart = new DomainObject(strPreviousId);
			StringList selectStmts = new StringList(8);
			selectStmts.addElement(DomainConstants.SELECT_ID);
			selectStmts.addElement(DomainConstants.SELECT_CURRENT);
			selectStmts.addElement(sImplementedItemCO);
			selectStmts.addElement(sAffectedItemCO);
			if("True".equalsIgnoreCase(sFloatToOlderRevision)){				
				selectStmts.addElement(DomainConstants.SELECT_REVISION);
				selectStmts.addElement(DomainConstants.SELECT_NAME);				
				selectStmts.addElement(sNextRevConnectedImplementedCO);
				selectStmts.addElement(sNextRevConnectedAffectedItemCO);
			}
			String stateSymbolicName = "state_Release";
			StringList selectRelStmts = new StringList(1);
			selectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
			String sRevQuery = "program[emxServiceUtils -method checkRevisions ${OBJECTID} "
					+ stateSymbolicName
					+ " HIGHEST_AND_PRESTATE_REVS] == true";

        MapList mapList = prevRevPart.getRelatedObjects(context,
        		                     relPattern.getPattern(),
                                     "*",
                                     selectStmts,
                                     selectRelStmts,
                                     true,
                                     false,
                                     (short) 1,
                                     sRevQuery,
                                     null);

        boolean isGBOM = false;
        String strRelationName;
        
        String sPartId;
        String sCurrentState;
        String sAssociatedCO;
        String sNextRevAssociatedCO;
        //String obsoleteState = PropertyUtil.getSchemaProperty(context, "policy",DomainConstants.POLICY_EC_PART, "state_Obsolete");
        String releaseState  = PropertyUtil.getSchemaProperty(context, "policy",DomainConstants.POLICY_EC_PART, "state_Release");
        
        Iterator i = mapList.iterator();
        ContextUtil.startTransaction(context, true);
        while (i.hasNext())
        {            m = (Map) i.next();
        strRelationName = (String)m.get("relationship");
        if (strRelationName.equals(RELATIONSHIP_GBOM_TO) || strRelationName.equals(RELATIONSHIP_GBOM)) {
            isGBOM = true;
		}
        attributeMap = new DomainRelationship((String)m.get(DomainConstants.SELECT_RELATIONSHIP_ID)).getAttributeMap(context, true);
        
        sCurrentState = (String)m.get(DomainConstants.SELECT_CURRENT);
        sAssociatedCO = UIUtil.isNotNullAndNotEmpty((String)m.get(sImplCO))?(String)m.get(sImplCO):(String)m.get(sAICO);
       
        if( !isGBOM && UIUtil.isNullOrEmpty(sChangeOrder)){
	    	
            historyRel = prevRevPart.addRelatedObject(context, _ebomHistory, true, (String)m.get(DomainConstants.SELECT_ID));
            attributeMap.put(DomainConstants.ATTRIBUTE_END_EFFECTIVITY_DATE, endEffectivityDate);
			 attributeMap.remove("Effectivity Types");
            attributeMap.remove("Effectivity Expression");
            attributeMap.remove("Effectivity Expression Binary");
            attributeMap.remove("Effectivity Variable Indexes");
            attributeMap.remove("Effectivity Compiled Form");
            attributeMap.remove("Effectivity Proposed Expression");
            attributeMap.remove("Effectivity Ordered Criteria");
            attributeMap.remove("Effectivity Ordered Criteria Dictionary");
            attributeMap.remove("Effectivity Ordered Impacting Criteria");
            historyRel.setAttributeValues(context, attributeMap);
           }   
        else if( !isGBOM && releaseState.equalsIgnoreCase(sCurrentState) && !(sChangeOrder.equalsIgnoreCase(sAssociatedCO))){
		    		    	
                 historyRel = prevRevPart.addRelatedObject(context, _ebomHistory, true, (String)m.get(DomainConstants.SELECT_ID));
                 attributeMap.put(DomainConstants.ATTRIBUTE_END_EFFECTIVITY_DATE, endEffectivityDate);
				 attributeMap.remove("Effectivity Types");
                 attributeMap.remove("Effectivity Expression");
                 attributeMap.remove("Effectivity Expression Binary");
                 attributeMap.remove("Effectivity Variable Indexes");
                 attributeMap.remove("Effectivity Compiled Form");
                 attributeMap.remove("Effectivity Proposed Expression");
                 attributeMap.remove("Effectivity Ordered Criteria");
                 attributeMap.remove("Effectivity Ordered Criteria Dictionary");
                 attributeMap.remove("Effectivity Ordered Impacting Criteria");
                 historyRel.setAttributeValues(context, attributeMap);
                }
		    
		 
		    	if("True".equalsIgnoreCase(sFloatToOlderRevision)){
			    	sPartId=(String)m.get(DomainConstants.SELECT_ID);			    	
			    	sNextRevAssociatedCO = UIUtil.isNotNullAndNotEmpty((String)m.get(sNextRevConnectedImplCO))?(String)m.get(sNextRevConnectedImplCO):(String)m.get(sNextRevConnectedAICO);
			    			    	
			    	//if parent (old/new) revision is in release state and sNextRevAssociatedCO(CO connected to parent new rev) is not same as sChangeOrder(CO connected to new rev of child) then connect the child new revision to old latest release revision of parent
			    	
			    	if((releaseState.equalsIgnoreCase(sCurrentState) && (!(sChangeOrder.equalsIgnoreCase(sNextRevAssociatedCO)) )) || PolicyUtil.checkState(context, sPartId, releaseState,PolicyUtil.LT)){
			    		DomainRelationship.setToObject(context, (String)m.get(DomainConstants.SELECT_RELATIONSHIP_ID), this);
			    		DomainRelationship.setAttributeValue(context, (String)m.get(DomainConstants.SELECT_RELATIONSHIP_ID), DomainConstants.ATTRIBUTE_START_EFFECTIVITY_DATE, startEffectivityDate);
			    	}
		    	}
		    	else{
		    		DomainRelationship.setToObject(context, (String)m.get(DomainConstants.SELECT_RELATIONSHIP_ID), this);
		    		if(!isGBOM)
		    			DomainRelationship.setAttributeValue(context, (String)m.get(DomainConstants.SELECT_RELATIONSHIP_ID), DomainConstants.ATTRIBUTE_START_EFFECTIVITY_DATE, startEffectivityDate);
		    		
		    	}
		    
		}

        ContextUtil.commitTransaction(context);
    }
    catch (Exception ex)
    {
        DebugUtil.debug("Got exception:  " + ex.toString());
        ContextUtil.abortTransaction(context);
        ex.printStackTrace();
        throw ex;
    }
    finally
    {
      contextUtil.popContext(context, null);
    }
    }

    
	
	/**
	 * Sets the Start Effectivity date on the "EBOM" or
	 * "Manufacturer Equivalent" connection between this assembly and any child
	 * components to the released date of the assembly.
	 * 
	 * @param context
	 *            the eMatrix <code>Context</code> object.
	 * @param args
	 *            holds the following input arguments: 0 - the symbolic name of
	 *            the relationship to float, either
	 *            relationship_ManufacturerEquivalent or relationship_EBOM
	 *            (default if none specified)
	 * @throws Exception
	 *             if the operation fails.
	 * @since EC 9.5.JCI.0.
	 * @trigger PolicyECPartStateReviewPromoteAction.
	 */
	public void setEBOMStartEffectivity(Context context, String[] args)
			throws Exception
	 {
		SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);

		DebugUtil.debug("PolicyECPartStateReviewPromoteAction:setEBOMStartEffectivity");

        // Need shadow agent to modify attributes
        contextUtil.pushContext(context, null);

        try
        {
            // This method is now also used to set the Manufacturer Equivalent Rel attributes
            // First get the symbolic name of the relationship to expand on
            String rel_SymbolicName = args[0];
            String relToExpand = "";

            // If there is no relationship name passed in then default to EBOM
            if (rel_SymbolicName != null &&
                rel_SymbolicName.equals("relationship_ManufacturerEquivalent"))
            {
                relToExpand = DomainConstants.RELATIONSHIP_MANUFACTURER_EQUIVALENT;
            }
            else
            {
                relToExpand = DomainConstants.RELATIONSHIP_EBOM;
            }
            // Rel selects
            StringList selectRelStmts = new StringList();
            selectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            // Get the child objects of this assembly connected via
            // the "EBOM" relationship
            MapList mapList = getRelatedObjects(context,
                                relToExpand,                        // relationship pattern
                                "*",                                // object pattern
                                null,                               // object selects
                                selectRelStmts,                     // relationship selects
                                false,                              // to direction
                                true,                               // from direction
                                (short) 1,                          // recursion level
                                null,                               // object where clause
                                null);                              // relationship where clause

            Iterator i = mapList.iterator();
            while (i.hasNext())
            {
                Map m = (Map) i.next();

                // Set start effectivity date to current date for any EBOM
                // connections to this released assembly
                DomainRelationship ebomRel = new DomainRelationship((String)m.get(DomainConstants.SELECT_RELATIONSHIP_ID));
                String startEffectivityDate = _mxDateFormat.format(new Date());
                ebomRel.setAttributeValue(context, DomainConstants.ATTRIBUTE_START_EFFECTIVITY_DATE, startEffectivityDate);
                DebugUtil.debug("New Attributes: " + ebomRel.getAttributeMap(context, true));
            }
        }
        catch (Exception ex)
        {
            throw ex;
        }
        finally
        {
          contextUtil.popContext(context, null);
        }
    }


	/**
	 * This method checks if the children objects related to the parent with the
	 * specified relationships with the "to" direction have reached the target
	 * state given.
	 * 
	 * The intent of this program is to provide a function which checks the
	 * state of all objects of a named object type related to a parent object.
	 * The returned value will inform the parent if all the requested related
	 * objects are at a given state so that the parent can be promoted to the
	 * next state.
	 * 
	 * @param context
	 *            the eMatrix <code>Context</code> object
	 * @param args
	 *            hold the following input arguments:
	 * 
	 * 
	 *            args[0] - sRelationship -args[0] - Relationship to expand
	 *            from, mutltiple relationships can be entered as a string
	 *            delimited with spaces(" "), "~" or ",". (Optional) (default
	 *            "*") Ex.
	 *            relationship_PartSpecification,relationship_DrawingSpecification
	 *            Passing in one of the following will expand on all
	 *            relationships: * or "" (NULL).
	 * 
	 *            args[1] - sTargetObject - args[1] - Object to expand on,
	 *            multiple objects can be entered as a string, delimited with
	 *            spaces(" "), "~" or ",". (Optional) (default "*") Ex.
	 *            type_Part,type_DrawingPrint Passing in one of the following
	 *            will expand on all objects: * or "" (NULL).
	 * 
	 *            args[2] - sTargetStateProp - The state being checked for.
	 *            Symbolic name must be used. (Optional) args[3] - sDirection -
	 *            The direction to expand. Valid entries are "from" or "to".
	 *            (Optional) (default both to and from).
	 * 
	 *            args[4] - sComparisonOperator - Operator to check state with.
	 *            Valid entries are LT, GT, EQ, LE, GE, and NE. (Optional)
	 *            (default - "EQ") args[5] - sObjectRequired - Set "required"
	 *            flag if an object should be connected. Valid entries are
	 *            Required and Optional. (Optional) (default - "Optional").
	 *            args[6] - sStateRequired - Set "required" flag if target state
	 *            should be present. Valid entries are Required and Optional.
	 *            (Optional) (default - "Required")
	 * 
	 * @return 0 if all children are in a valid state. 1 if any child is in an
	 *         invalid state.
	 * @throws Exception
	 *             if the operation fails
	 * @since EC 10.6.SP2
	 */
      public int checkRelatedObjectState(Context context, String []args)  throws Exception
      {
          String strOutput = "";
          int intOutput = 0;

          // Create an instant of emxUtil JPO
          emxUtil_mxJPO utilityClass = new emxUtil_mxJPO(context, null);

          // Get Required Environment Variables
          String arguments[] = new String[1];
          arguments[0] = "get env OBJECTID";

          ArrayList cmdResults = utilityClass.executeMQLCommands(context, arguments);

          String sObjectId = (String)cmdResults.get(0);
          StringBuffer sBuffer = new StringBuffer();
          String sRel = "";
          String sTargObject = "";

          String sRelationship    = args[0];
          String sTargetObject    = args[1];
          String sTargetStateProp = args[2];
          String sDirection       = args[3];
          String sComparisonOperator  = args[4];
          String sObjectRequired      = args[5];
          String sStateRequired       = args[6];

          // If no value for operator set it to EQ
          if("".equals(sComparisonOperator))
          {
              sComparisonOperator = "EQ";
          }

          // If value for Object Required in not Required set it to Optional
          if(!"required".equalsIgnoreCase(sObjectRequired))
          {
              sObjectRequired = "Optional";
          }

          // If value for State Required in not Required set it to Optional
          if(!"optional".equalsIgnoreCase(sStateRequired))
          {
              sStateRequired = "Required";
          }

          StringTokenizer strToken = new StringTokenizer(sRelationship, " ,~");
          String strRel = "";
          String strRelRealName = "";
          while(strToken.hasMoreTokens())
          {
              strRel = strToken.nextToken().trim();
              strRelRealName = PropertyUtil.getSchemaProperty(context,strRel);

              if("".equals(strRelRealName))
              {
                   // Error out if not registered
                  arguments = new String[5];
                  arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState_if.InvalidRel";
                  arguments[1] = "1";
                  arguments[2] = "Rel";
                  arguments[3] = strRel;
                  arguments[4] = "";

                  emxMailUtil_mxJPO mailUtil = new emxMailUtil_mxJPO(context, null);
                  strOutput = mailUtil.getMessage(context,arguments);
                  intOutput = 1;
                  break;
              }
              else
              {
                  if(sBuffer.length() > 0)
                  {
                      sBuffer.append(',');
                  }
                  sBuffer.append(strRelRealName);
              }
          }

          if(sBuffer.length() > 0)
          {
              sRel = sBuffer.toString();
          }
          else
          {
              // Set Relationship to * if one is not entered
              sRel = "*";
          }

          if(intOutput == 0)
          {
              sBuffer = new StringBuffer();
              strToken = new StringTokenizer(sTargetObject, " ,~");
              String sTypeResult = "";
              String sTypeRealName = "";
              while(strToken.hasMoreTokens())
              {
                  sTypeResult = strToken.nextToken().trim();
                  sTypeRealName = PropertyUtil.getSchemaProperty(context,sTypeResult);
                  if("".equals(sTypeRealName))
                  {
                      // Error out if not registered
                      arguments = new String[5];
                      arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState_if.InvalidType";
                      arguments[1] = "1";
                      arguments[2] = "Type";
                      arguments[3] = sTypeResult;
                      arguments[4] = "";

                      emxMailUtil_mxJPO mailUtil = new emxMailUtil_mxJPO(context, null);
                      strOutput = mailUtil.getMessage(context,arguments);
                      intOutput = 1;
                      break;
                  }
                  else
                  {
                      if(sBuffer.length() > 0)
                      {
                          sBuffer.append(',');
                      }
                      sBuffer.append(sTypeRealName);
                  }
              }

              if(sBuffer.length() > 0)
              {
                  sTargObject = sBuffer.toString();
              }
              else
              {
                  // Set Target Object to * if one is not entered
                  sTargObject = "*";
              }
          }

          if(intOutput == 0)
          {
              String sTargetState = "";

              emxMailUtil_mxJPO mailUtil = new emxMailUtil_mxJPO(context, null);

              DomainObject domObj = new DomainObject(sObjectId);
              boolean bParentState = true;
              // If no Target state is defined use current state of object
              if(sTargetStateProp == null || "".equals(sTargetStateProp))
              {
                  sTargetState = domObj.getInfo(context,DomainConstants.SELECT_CURRENT);
                  bParentState = false;
              }

              // prepare getRelatedObjects parameters
              boolean getToRelationships = true;
              boolean getFromRelationships = true;

              if("to".equalsIgnoreCase(sDirection))
              {
                  getFromRelationships = false;
              }
              else if("from".equalsIgnoreCase(sDirection))
              {
                  getToRelationships = false;
              }

              // Only get the children that are not on the same ECO
              String ecoId = "";
              StringList selectRelStmts = new StringList();
              StringList selectStmts  = new StringList(1);
              selectStmts.addElement(SELECT_ID);
              MapList ecoMapList = getECO(context,selectStmts,selectRelStmts);
              if (ecoMapList.size() > 0)
              {
                  Map ecoMap = (Map)ecoMapList.get(0);
                  ecoId = (String)ecoMap.get(SELECT_ID);
              }

              String whereClause = "";
              if (ecoId != null && ecoId.length() > 0)
              {
                 // whereClause = "to["+DomainObject.RELATIONSHIP_NEW_PART_PART_REVISION+"].from.id!=\""+ecoId+"\"";
                  //whereClause = whereClause + " || to["+RELATIONSHIP_AFFECTED_ITEM+"].from.id!=\""+ecoId+"\"";
                  whereClause = "to["+RELATIONSHIP_AFFECTED_ITEM+"] == False || to["+RELATIONSHIP_AFFECTED_ITEM+"].from.id!=\""+ecoId+"\"";
              }
              StringList strListObj = new StringList(6);
              strListObj.add(DomainConstants.SELECT_ID);
              strListObj.add(DomainConstants.SELECT_TYPE);
              strListObj.add(DomainConstants.SELECT_NAME);
              strListObj.add(DomainConstants.SELECT_REVISION);
              strListObj.add(DomainConstants.SELECT_CURRENT);
              strListObj.add(DomainConstants.SELECT_POLICY);

              MapList mapList = domObj.getRelatedObjects(context,
                                                         sRel,
                                                         sTargObject,
                                                         strListObj,
                                                         null,
                                                         getToRelationships, // getTo relationships
                                                         getFromRelationships, // getFrom relationships
                                                         (short)1,
                                                         whereClause,
                                                         "");

              int size = 0;
              if(mapList != null && (size = mapList.size()) > 0)
              {
                  // Create a list of all matching objects and check their state
                  Map map = null;
                  String sChildID = "";
                  String sChildType = "";
                  String sChildName = "";
                  String sChildRev = "";
                  String sChildCurrent = "";
                  String sChildPolicy = "";

                  StringList strListChildStates = null;

                  String sDvlpPartPolicy = DomainConstants.POLICY_DEVELOPMENT_PART;

                  for(int i = 0 ; i < size ; i++)
                  {
                      map = (Map)mapList.get(i);
                      sChildID = (String)map.get(DomainConstants.SELECT_ID);
                      sChildType = (String)map.get(DomainConstants.SELECT_TYPE);
                      sChildName = (String)map.get(DomainConstants.SELECT_NAME);
                      sChildRev = (String)map.get(DomainConstants.SELECT_REVISION);
                      sChildCurrent = (String)map.get(DomainConstants.SELECT_CURRENT);
                      sChildPolicy = (String)map.get(DomainConstants.SELECT_POLICY);

                      /*
                      *If a dvlp part, then we need to equate "Approved" (Common Part State) to
                      * "Complete" (Dvlp part state) and "Review" (Common Part State)
                      * to "Peer Review" (Dvlp Part State)
                      */
                      if(sChildPolicy.equals(sDvlpPartPolicy))
                      {
                          if("state_Approved".equals(sTargetStateProp))
                          {
                              sTargetStateProp = "state_Complete";
                          }
                          else if("state_Review".equals(sTargetStateProp))
                          {
                              sTargetStateProp = "state_PeerReview";
                          }
                      }

                      if(bParentState)
                      {
                          sTargetState = PropertyUtil.getSchemaProperty(context,"policy",sChildPolicy,sTargetStateProp);
                          if(sTargetState == null || "".equals(sTargetState))
                          {
                              if("required".equalsIgnoreCase(sStateRequired))
                              {
                                  // Error out if not registered
                                  arguments = new String[7];
                                  arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.InvalidState";
                                  arguments[1] = "2";
                                  arguments[2] = "State";
                                  arguments[3] = sTargetStateProp;
                                  arguments[4] = "Policy";
                                  arguments[5] = sChildPolicy;
                                  arguments[6] = "";

                                  intOutput = 1;
                                  strOutput = strOutput + mailUtil.getMessage(context,arguments);
                                  break;
                              }
                              else
                              {
                                  continue;
                              }
                          }
                      }

                      // Get all states for object
                      domObj = new DomainObject(sChildID);

                      strListChildStates = domObj.getInfoList(context,DomainConstants.SELECT_STATES);
                      int indexTargetState = strListChildStates.indexOf(sTargetState);

                      // check if target state is in object's policy
                      if(indexTargetState < 0)
                      {
                          if("required".equalsIgnoreCase(sStateRequired))
                          {
                              arguments = new String[13];
                              arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.InvalidTargetState";
                              arguments[1] = "5";
                              arguments[2] = "Type";
                              arguments[3] = sChildType;
                              arguments[4] = "Name";
                              arguments[5] = sChildName;
                              arguments[6] = "Rev";
                              arguments[7] = sChildRev;
                              arguments[8] = "Policy";
                              arguments[9] = sChildPolicy;
                              arguments[10] = "State";
                              arguments[11] = sTargetState;
                              arguments[12] = "";

                              intOutput = 1;
                              strOutput = strOutput + mailUtil.getMessage(context,arguments);
                          }

                          continue;
                      }

                      // Get index location for object
                      int index = strListChildStates.indexOf(sChildCurrent);

                      // Check Target State index with object index location
                      if("LT".equals(sComparisonOperator))
                      {
                          if(index >= indexTargetState)
                          {
                              arguments = new String[13];
                              arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.EqualOrAfter";
                              arguments[1] = "5";
                              arguments[2] = "Type";
                              arguments[3] = sChildType;
                              arguments[4] = "Name";
                              arguments[5] = sChildName;
                              arguments[6] = "Rev";
                              arguments[7] = sChildRev;
                              arguments[8] = "State";
                              arguments[9] = sChildCurrent;
                              arguments[10] = "TargetState";
                              arguments[11] = sTargetState;
                              arguments[12] = "";

                              intOutput = 1;
                              strOutput = strOutput + mailUtil.getMessage(context,arguments);
                          }
                      }
                      else if("GT".equals(sComparisonOperator))
                      {
                          if(index <= indexTargetState)
                          {
                              arguments = new String[13];
                              arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.EqualOrBefore";
                              arguments[1] = "5";
                              arguments[2] = "Type";
                              arguments[3] = sChildType;
                              arguments[4] = "Name";
                              arguments[5] = sChildName;
                              arguments[6] = "Rev";
                              arguments[7] = sChildRev;
                              arguments[8] = "State";
                              arguments[9] = sChildCurrent;
                              arguments[10] = "TargetState";
                              arguments[11] = sTargetState;
                              arguments[12] = "";

                              intOutput = 1;
                              strOutput = strOutput + mailUtil.getMessage(context,arguments);
                          }
                      }
                      else if("EQ".equals(sComparisonOperator))
                      {
                          if(index != indexTargetState)
                          {
                              arguments = new String[11];
                              arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.NotIn";
                              arguments[1] = "4";
                              arguments[2] = "Type";
                              arguments[3] = sChildType;
                              arguments[4] = "Name";
                              arguments[5] = sChildName;
                              arguments[6] = "Rev";
                              arguments[7] = sChildRev;
                              arguments[8] = "State";
                              arguments[9] = sTargetState;
                              arguments[10] = "";

                              intOutput = 1;
                              strOutput = strOutput + mailUtil.getMessage(context,arguments);
                          }
                      }
                      else if("LE".equals(sComparisonOperator))
                      {
                          if(index > indexTargetState)
                          {
                              arguments = new String[13];
                              arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.After";
                              arguments[1] = "5";
                              arguments[2] = "Type";
                              arguments[3] = sChildType;
                              arguments[4] = "Name";
                              arguments[5] = sChildName;
                              arguments[6] = "Rev";
                              arguments[7] = sChildRev;
                              arguments[8] = "State";
                              arguments[9] = sChildCurrent;
                              arguments[10] = "TargetState";
                              arguments[11] = sTargetState;
                              arguments[12] = "";

                              intOutput = 1;
                              strOutput = strOutput + mailUtil.getMessage(context,arguments);
                          }
                      }
                      else if("GE".equals(sComparisonOperator))
                      {
                          if(index < indexTargetState)
                          {
                              arguments = new String[13];
                              arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.Before";
                              arguments[1] = "5";
                              arguments[2] = "Type";
                              arguments[3] = sChildType;
                              arguments[4] = "Name";
                              arguments[5] = sChildName;
                              arguments[6] = "Rev";
                              arguments[7] = sChildRev;
                              arguments[8] = "State";
                              arguments[9] = sChildCurrent;
                              arguments[10] = "TargetState";
                              arguments[11] = sTargetState;
                              arguments[12] = "";

                              intOutput = 1;
                              strOutput = strOutput + mailUtil.getMessage(context,arguments);
                          }
                      }
                      else if("NE".equals(sComparisonOperator))
                      {
                          if(index == indexTargetState)
                          {
                              arguments = new String[11];
                              arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.Equal";
                              arguments[1] = "4";
                              arguments[2] = "Type";
                              arguments[3] = sChildType;
                              arguments[4] = "Name";
                              arguments[5] = sChildName;
                              arguments[6] = "Rev";
                              arguments[7] = sChildRev;
                              arguments[8] = "State";
                              arguments[9] = sChildCurrent;
                              arguments[10] = "";

                              intOutput = 1;
                              strOutput = strOutput + mailUtil.getMessage(context,arguments);
                          }
                      }
                      else
                      {
                          arguments = new String[5];
                          arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.InvalidOperator";
                          arguments[1] = "1";
                          arguments[2] = "Operation";
                          arguments[3] = sComparisonOperator;
                          arguments[4] = "";

                          intOutput = 1;
                          strOutput = strOutput + mailUtil.getMessage(context,arguments);
                          break;
                      }
                  }

              }
              else if("required".equalsIgnoreCase(sObjectRequired))
              {
                  arguments = new String[7];
                  arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.NoObject";
                  arguments[1] = "2";
                  arguments[2] = "Rel";
                  arguments[3] = sRel;
                  arguments[4] = "Object";
                  arguments[5] = sTargObject;
                  arguments[6] = "";

                  intOutput = 1;
                  strOutput = mailUtil.getMessage(context,arguments);
              }
          }

          if(intOutput != 0)
          {
              emxContextUtil_mxJPO.mqlNotice(context,strOutput);
          }
          return intOutput;
      }

	/**
	 * Gets the select information for the ECO for the Part.
	 * 
	 * @param context
	 *            the eMatrix <code>Context</code> object.
	 * @param selectStmts
	 *            The list of selects.
	 * @param selectRelStmts
	 *            The list of relationship selects.
	 * @return MapList of the ECO.
	 * @throws FrameworkException
	 *             if the operation fails.
	 * @since AEF 9.5.0.0.
	 */
    public MapList getECO(Context context,
                          StringList selectStmts,
                          StringList selectRelStmts)
        throws FrameworkException
    {
        try
        {
            Pattern relPattern = new Pattern(RELATIONSHIP_AFFECTED_ITEM);

            return getRelatedObjects(context,
                                         relPattern.getPattern(),     // relationship pattern
                                         DomainConstants.TYPE_ECO,                         //Sharath modifed from * // object pattern
                                         selectStmts,                 // object selects
                                         selectRelStmts,              // relationship selects
                                         true,                        // to direction
                                         false,                       // from direction
                                         (short)1,                    // recursion level
                                         null,                        // object where clause
                                         null);                       // relationship where clause
        }
        catch (Exception e)
        {
            throw new FrameworkException(e);
        }
    }


	/**
	 * Sets the End Effectivity date between this part and its parent assemblies
	 * to the date this part is made obsolete.
	 * 
	 * @param context
	 *            the eMatrix <code>Context</code> object.
	 * @param args
	 *            holds the following input arguments: 0 - the symbolic name of
	 *            the relationship to float, either
	 *            relationship_ManufacturerEquivalent or relationship_EBOM
	 *            (default if none specified)
	 * @throws Exception
	 *             if the operation fails.
	 * @since EC 9.5.5.0.
	 * @trigger PolicyECPartStateReleasePromoteAction.
	 */
    public void setEBOMEndEffectivity(Context context, String[] args)
                    throws Exception
    {
		SimpleDateFormat _mxDateFormat =
			new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);

        DebugUtil.debug("PolicyECPartStateReleasePromoteAction:setEBOMEndEffectivity");

        // Need shadow agent to modify attributes
        contextUtil.pushContext(context, null);

        try
        {
            // This method is now also used to set the Manufacturer Equivalent Rel attributes
            // First get the symbolic name of the relationship to expand on
            String rel_SymbolicName = args[0];
            String relToExpand = "";

            // If there is no relationship name passed in then default to EBOM
            if (rel_SymbolicName != null &&
                rel_SymbolicName.equals("relationship_ManufacturerEquivalent"))
            {
                relToExpand = DomainConstants.RELATIONSHIP_MANUFACTURER_EQUIVALENT;
            }
            else
            {
                relToExpand = DomainConstants.RELATIONSHIP_EBOM;
            }
            // Rel selects
            //
            StringList selectRelStmts = new StringList();
            selectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            // Get the parent assemblies connected to this child (which is being
            // made obsolete) via the "EBOM" relationship
            // the "EBOM" relationship
            MapList mapList = getRelatedObjects(context,
                                relToExpand,                        // relationship pattern
                                "*",                                // object pattern
                                null,                               // object selects
                                selectRelStmts,                     // relationship selects
                                true,                               // to direction
                                false,                              // from direction
                                (short) 1,                          // recursion level
                                null,                               // object where clause
                                null);                              // relationship where clause

            Iterator i = mapList.iterator();
            while (i.hasNext())
            {
                Map m = (Map) i.next();

                // Set start effectivity date to current date for any EBOM
                // connections to this released assembly
                DomainRelationship ebomRel = new DomainRelationship((String)m.get(DomainConstants.SELECT_RELATIONSHIP_ID));
                String endEffectivityDate = _mxDateFormat.format(new Date());
                ebomRel.setAttributeValue(context, DomainConstants.ATTRIBUTE_END_EFFECTIVITY_DATE, endEffectivityDate);
                DebugUtil.debug("New Attributes: " + ebomRel.getAttributeMap(context, true));
            }
        }
        catch (Exception ex)
        {
            throw ex;
        }
        finally
        {
          contextUtil.popContext(context, null);
        }
    }

	/**
	 * Checks if an object can be connected as an EBOM. If the system property
	 * emxBOMPartManagement.AllowUnReleasedRevs is false, then it makes the
	 * following checks: - makes sure the part is not in one of the frozen
	 * states as specified in emxBOMPartManagement.EBOMFrozenStates. - checks
	 * that only the latest revision can be connected.
	 * 
	 * @param context
	 *            the eMatrix <code>Context</code> object.
	 * @param args
	 *            holds no arguments.
	 * @return int. 0 - for success, 1 - for Exception/failure.
	 * @throws Exception
	 *             if the operation fails.
	 * @since EC 9.5.2.1.
	 */
	
    public int CheckForEBOMConnection(Context context, String[] args) throws Exception
    {
      // ObjectId at the from end of the EBOM relationship

		// Allow all revisions to connect irrespective of state based on a
		// property entry.
		String strAllowUnreleasedRevs = FrameworkProperties.getProperty(
				context, "emxBOMPartManagement.AllowUnReleasedRevs");
		// Allow all revisions to connect irrespective of state if property
		// entry is true.
		if (strAllowUnreleasedRevs != null
				&& strAllowUnreleasedRevs.equalsIgnoreCase("true")) {
			return 0;
		}

		EBOM_FROZEN_STATES = FrameworkProperties.getProperty(
				context, "emxBOMPartManagement.EBOMFrozenStates");
		StringTokenizer sTok = new StringTokenizer(EBOM_FROZEN_STATES, ",");

		// Added for IR-021267
		if (EBOM_FROZEN_STATES == null
				|| EBOM_FROZEN_STATES.trim().length() == 0
				|| (sTok.countTokens() % 2) != 0) {
			 String sErrorMsg=EnoviaResourceBundle.getProperty(context,"emxBOMPartManagementStringResource",context.getLocale(),"emxBOMPartManagement.EBOM.EBOMFrozenStatesPopropertyError");
			emxContextUtil_mxJPO.mqlError(context, sErrorMsg);
			return 1;
		}

		StringList stateList = new StringList();
		while (sTok.hasMoreTokens()) {
			String tokenPolicy = sTok.nextToken();
			String tokenState = sTok.nextToken();
			String sPolicyRealName = PropertyUtil.getSchemaProperty(context,
					tokenPolicy);
			if (sPolicyRealName == null || "".equals(sPolicyRealName.trim())) {
				String sErrorMsg=EnoviaResourceBundle.getProperty(context,"emxBOMPartManagementStringResource",context.getLocale(),"emxBOMPartManagement.Common.SymbolicNameMapError");
				emxContextUtil_mxJPO.mqlError(context, tokenPolicy + " "
						+ sErrorMsg);
				return 1;
			}
			String sStateRealName = PropertyUtil.getSchemaProperty(context,
					"policy", sPolicyRealName, tokenState);
			if (sStateRealName == null || "".equals(sStateRealName.trim())) {
				
				String sErrorMsg=EnoviaResourceBundle.getProperty(context,"emxBOMPartManagementStringResource",context.getLocale(),"emxBOMPartManagement.Common.SymbolicNameMapError");
				emxContextUtil_mxJPO.mqlError(context, tokenState + " "
						+ sErrorMsg);
				return 1;
			}
			stateList.addElement(sStateRealName);
		}

      // get the type & name of part connecting as EBOM to check whether any its rev is already connected.
      String sType = getInfo(context, SELECT_TYPE);
      String sName = getInfo(context, SELECT_NAME);
      String sRev = getInfo(context, SELECT_REVISION);

      StringList selectStmts = new StringList();

      selectStmts.addElement(SELECT_TYPE);
      selectStmts.addElement(SELECT_NAME);
      selectStmts.addElement(SELECT_REVISION);
      selectStmts.addElement(SELECT_CURRENT);

      // get all the revisions of the currnt object
      MapList mapList = getRevisions(context, selectStmts, false);

      int mapCount = mapList.size() - 1 ;
      Map map = null;
      // Loop thru the revision chain back from latest. Check any of the revision is in the state mentioned
      // as frozen in the above property if yes check the currect connecting object revision is same as of this rev
      // if Yes make connection otherwise display error message

		for (int loopCount = 0; loopCount < mapList.size(); loopCount++) {
			map = (Map) mapList.get(mapCount--);
			String sCurrent = (String) map.get(SELECT_CURRENT);
			String sRevision = (String) map.get(SELECT_REVISION);
			if (stateList.contains(sCurrent)) {
				if (sRevision.equals(sRev)) {
					return 0;
				} else {
					
				  String sErrorMsg= sType + " " + sName+ " " + sRev + " " +
				  EnoviaResourceBundle.getProperty(context,"emxBOMPartManagementStringResource",context.getLocale(),"emxBOMPartManagement.EBOM.NotLatestRevisionError1") + sCurrent + " " +
				  EnoviaResourceBundle.getProperty(context,"emxBOMPartManagementStringResource",context.getLocale(),"emxBOMPartManagement.EBOM.NotLatestRevisionError2");
				  emxContextUtil_mxJPO.mqlError(context, sErrorMsg);
					return 1;
				}
			}
		}

		// If no revisions are in the the above property mentioned frozen
		// states, then check whether the
		// currect connecting revision is the first revision, if not display
		// error message.
		if (((String) map.get(SELECT_REVISION)).equals(sRev)) {
			return 0;
		} else {
			
			 String sErrorMsg = sType + " " + sName+ " " + sRev + " "+EnoviaResourceBundle.getProperty(context, "emxBOMPartManagement.EBOM.NotFirstRevisionError");
	    		emxContextUtil_mxJPO.mqlError(context, sErrorMsg);
			return 1;
		}
	}

	/**
	 * Checks if an object can be connected as an EBOM. - checks if the Parent
	 * is not in any state as indicated by the property key -
	 * emxBOMPartManagement.ECPart.AllowApply and
	 * emxBOMPartManagement.DevelopmentPart.AllowApply - If the Parent part is
	 * in any of the above states then allows to be connected as EBOM else not.
	 * *
	 * 
	 * @param context
	 *            the eMatrix <code>Context</code> object.
	 * @param args
	 *            holds no arguments.
	 * @return int. 0 - for success, 1 - for Exception/failure.
	 * @throws Exception
	 *             if the operation fails.
	 * @since EC V6R2011
	 */
	public int CheckForParentInRelease(Context context, String[] args)
			throws Exception {
		String sType = getInfo(context, SELECT_TYPE);
		String sName = getInfo(context, SELECT_NAME);
		String sRev = getInfo(context, SELECT_REVISION);
		String sPolicy = getInfo(context, SELECT_POLICY);
		String sCurrent = getInfo(context, DomainConstants.SELECT_CURRENT);
		String langStr = context.getSession().getLanguage();
		String policyClassification = getPolicyClassification(context, sPolicy);
		String propAllowLevel = "";

		if ("Production".equalsIgnoreCase(policyClassification)) {
			propAllowLevel = DomainObject.STATE_PART_RELEASE;
		}
		// IR-049181 - Starts
		else if ("Unresolved".equalsIgnoreCase(policyClassification)) {
			return 0;
		}
		// IR-049181 - Ends

		if (sCurrent == null && "".equals(sCurrent)) {
			return 1;
		}

		if (!sCurrent.equals(propAllowLevel)) {
			return 0;
		} else {
			
			  String sErrorMsg= sType + " " + sName+ " " + sRev + " " +
			  EnoviaResourceBundle.getProperty(context,"emxBOMPartManagementStringResource",context.getLocale(),"emxBOMPartManagement.EBOM.ParentInReleaseError1") +
			  sCurrent + ". " + EnoviaResourceBundle.getProperty(context,"emxBOMPartManagementStringResource",
			  langStr,"emxBOMPartManagement.EBOM.ParentInReleaseError2");
    			emxContextUtil_mxJPO.mqlNotice(context, sErrorMsg);
			return 1;
		}
	}
	
	public int multiLevelRecursionCheck(Context context, String[] args)
			throws Exception {
		boolean recursionFlag = false; // assume recursion not exists initially
		String fromObjectId = args[0];
		String toObjectId = args[1];
		String relType = args[2];
		String includeRevisions = args[3];

		boolean includeRevisionCheck = "true"
				.equalsIgnoreCase(includeRevisions) ? true : false;

		StringList relTypeList = (StringList) FrameworkUtil.split(relType, ",");
		StringBuffer relTypeBuffer = new StringBuffer();
		Iterator relItr = relTypeList.iterator();
		while (relItr.hasNext()) {
			relType = PropertyUtil.getSchemaProperty(context, relItr.next()
					.toString());
			relTypeBuffer = (relTypeBuffer.length() > 0) ? relTypeBuffer
					.append(",").append(relType) : relTypeBuffer
					.append(relType);
		}

		try {
			// this api returns true if recursion exists
			ContextUtil.pushContext(context);
			recursionFlag = (Boolean) DomainObject.multiLevelRecursionCheck(
					context, fromObjectId, toObjectId,
					relTypeBuffer.toString(), includeRevisionCheck);
			ContextUtil.popContext(context);
		}

		catch (Exception e) {
			e.printStackTrace();
			throw new FrameworkException(e.getMessage());
		}

		// If recursion exists, return recursion exists message to the User
		if (recursionFlag) {
			// Multitenant
			 String recursionMesssage =EnoviaResourceBundle.getProperty(context,"emxBOMPartManagementStringResource",context.getLocale(),"emxBOMPartManagement.Alert.RecursionError");
			throw new FrameworkException(recursionMesssage);
		}

		return 0;
	}


	/**
	 * Gets the Policy Classification for a policy The value will be either
	 * 'Development', 'Production', 'Equivalent',or 'Other'
	 * 
	 * @param context
	 *            the eMatrix <code>Context</code> object
	 * @param policyName
	 *            the name of the policy
	 * @return the Policy Classification value
	 * @throws FrameworkException
	 *             if the operation fails
	 */
	static public String getPolicyClassification(Context context,
			String policyName) throws FrameworkException {
		MQLCommand command = new MQLCommand();
		String sPolicyName = policyName;

		try {
			command.executeCommand(context, "print policy $1 select $2 dump",
					sPolicyName, "property[PolicyClassification].value");
		} catch (MatrixException e) {
			throw new FrameworkException(e);
		}

		return command.getResult().trim();

	}

	 /**
     * Removes the Substitutes for an EBOM relationship that is being deleted.
     *      When a part is disconnected from the assembly, all subsitutes
     *      for that part must also be deleted.  The context of this Part should
     *      be the Assembly Part.
     *
     * @param context the eMatrix <code>Context</code> object.
     * @param args holds the following input arguments:
     *        0 - the EBOM relationship id that is being deleted
     * @throws Exception if the operation fails.
     * @since X3.
     * @trigger RelationshipEBOMDeleteOverride.
     */

    public void removeSubstitutes(Context context, String[] args)
        throws Exception
    {
        DebugUtil.debug("RelationshipEBOMDeleteOverride:removeSubstitutes");

        // args[] parameters
            //Getting EBOM id from argument
        String ebomRelId = args[0];
           String  RELATIONSHIP_EBOM_SUBSTITUTE  = PropertyUtil.getSchemaProperty(context,"relationship_EBOMSubstitute");

        try
        {
                //Getting connected EBOM Substiute connection Id to remove the Substitute Part
                String strCommand     = "print connection $1 select $2 dump $3";
                String strMessage     = MqlUtil.mqlCommand(context,strCommand,ebomRelId,"frommid["+RELATIONSHIP_EBOM_SUBSTITUTE+"].id","|");
                //creating a list of connected EBOM Substiute connection Id to remove the Substitute Part
                StringList slEBOMSubs = FrameworkUtil.split(strMessage,"|");
                Iterator ebomsubsItr  = slEBOMSubs.iterator();
                String sEBOMSubstituteRelid    = "";
                while(ebomsubsItr.hasNext())
                            {
                    //EBOM Substitute Id
                    sEBOMSubstituteRelid = (String) ebomsubsItr.next();
                    //Removing the EBOM Substitute connection to remove the Substitute Part.
                    String strdelCommand = "delete connection $1";
                    MqlUtil.mqlCommand(context,strdelCommand,sEBOMSubstituteRelid);
            }

        }
        catch (Exception e)
        {
            DebugUtil.debug("RelationshipEBOMDeleteOverride--Exception=", e.toString());
            throw (e);
        }
        finally
        {
        }
}


	
	
	/**
	    * Checks that if there are Equivalent Parts connected to the context part
	    * at least one of the Equivalent Parts must be Released.
	    * If none of the attached Equivalent Parts are Released, the error message
	    * "The Part has 1 or more Equivalent Parts connected.One or more of the Equivalent Parts must be in the Release state"
	    * is thrown.
	    *
	    * @param context the eMatrix <code>Context</code> object.
	    * @param args holds no arguments.
	    * @return void.
	    * @throws Exception if the operation fails.
	    * @since EC 9.5.ROSSINI.0.
	    */
	    public void checkEquivalentPartState(matrix.db.Context context,String[] args) throws Exception
	    {
	      try
	      {
	        // Get the list of Equivalent Parts
	        boolean flag=false;

	        StringList objectSelects = new StringList(1);
	        objectSelects.addElement(SELECT_ID);

	        String partId = (String) args[0];
	        if(partId!=null)
	        {
	        	  ContextUtil.startTransaction(context, true);
	        	  DomainObject expandPartObj = new DomainObject(partId);
	              expandPartObj.open(context);

	              StringList relSelects = new StringList();
	              ExpansionIterator iter = expandPartObj.getExpansionIterator(context, RELATIONSHIP_MANUFACTURER_EQUIVALENT, TYPE_PART,
	                      objectSelects, relSelects, false, true, (short)1,
	                      null, null, (short)0,
	                      false, false, (short)1, false);

	              MapList objList = FrameworkUtil.toMapList(iter,
	                      (short)0, null, null, null, null);

	              // Check any Equivalent Part is below Release, if yes thow exception for not ot promote Part.
	              if(objList.size() > 0)
	              {
	                  Iterator objItr = (Iterator) objList.iterator();

	                  Map objMap = null;
	                  while (objItr.hasNext())
	                  {
	                      objMap  = (Map)objItr.next();
	                      if(checkObjState(context, (String)objMap.get(SELECT_ID), STATE_PART_RELEASE, GE) == 0 )
	                      {
	                          flag=true;
	                          break;
	                      }
	                  }
	              } else {
	                flag=true;
	              }

	              expandPartObj.close(context);
	              ContextUtil.commitTransaction(context);
	        }

	        if(!flag) {
	        	ContextUtil.abortTransaction(context);
	        	
	        	String msg=EnoviaResourceBundle.getProperty(context,"emxBOMPartManagementStringResource",context.getLocale(),"emxBOMPartManagement.EquivalentParts.Message");
	          throw new FrameworkException(msg);
	        }
	      }
	      catch(Exception e)
	      {
	    	  ContextUtil.abortTransaction(context);

	        throw e;
	      }
	    //Modified for IR-054051V6R2011x -Ends
	    }
	    
	    /**
	     * Checks the current state of the object with the target state, using the comparison operator
	     * and returns the result.
	     * @return int representing the following values.
	     *               0 if object state logic satisfies Comparison Operator.
	     *               1 if object state logic didn't satisfies Comparison Operator.
	     *               1 if a program error is encountered.
	     *               1 if state in state argument does not exist in objects policy.
	     *               1 if an invalid comparison operator is passed in.
	     * @param context the eMatrix <code>Context</code> object.
	     * @param id  the id of the object whose state to be checked.
	     * @param targetState the target state against which the current state of the object is compared.
	     * @param comparisonOperator the operator used for comparison LT, GT, EQ, LE, GE, NE.
	     * @since EC 9.5.ROSSINI.0.
	     */

	     protected int checkObjState(matrix.db.Context context, String id, String targetState, int comparisonOperator)
	     {
	         try
	         {
	         	String sResult = MqlUtil.mqlCommand(context,"print bus $1 select $2 $3 dump $4",id,"current","state","|");
	             StringTokenizer tokens = new StringTokenizer(sResult, "|");
	             int targetIndex = sResult.lastIndexOf(targetState);
	             int stateIndex  = sResult.lastIndexOf(tokens.nextToken());

	             // Check if State exist in Policy
	             if (targetIndex < 0) {
	                 return 1; // State doesn't exist in the policy
	             }

	             // check Target State index with object Current state index
	             switch (comparisonOperator) {
	                 case LT :
	                     if ( stateIndex < targetIndex ) {
	                         return 0;
	                     }
	                     break;

	                 case GT :
	                     if ( stateIndex > targetIndex ) {
	                         return 0;
	                     }
	                     break;

	                 case EQ :
	                     if ( stateIndex == targetIndex ) {
	                          return 0;
	                     }
	                     break;

	                 case LE :
	                     if ( stateIndex <= targetIndex ) {
	                          return 0;
	                     }
	                     break;

	                 case GE :
	                     if ( stateIndex >= targetIndex ) {
	                          return 0;
	                     }
	                     break;

	                 case NE :
	                     if ( stateIndex != targetIndex ) {
	                         return 0;
	                     }
	                     break;

	                 default :
	                     return 1;

	             }
	             return 1;
	         } catch (Exception ex) {
	             return 1;
	         }
	     }
		 

}
