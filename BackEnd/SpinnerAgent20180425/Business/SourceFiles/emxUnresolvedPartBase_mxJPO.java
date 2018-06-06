/*
 ** ${CLASSNAME}
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved.
 ** This program contains proprietary and trade secret information of
 ** Dassault Systemes.
 ** Copyright notice is precautionary only and does not evidence any actual
 ** or intended publication of such program
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import matrix.db.BusinessType;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Policy;
import matrix.db.PolicyItr;
import matrix.db.PolicyList;
import matrix.util.StringList;

import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PolicyUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.effectivity.EffectivityFramework;
import com.matrixone.apps.engineering.EngineeringConstants;
import com.matrixone.apps.engineering.EngineeringUtil;
import com.matrixone.apps.engineering.Part;
import com.matrixone.apps.framework.ui.UIUtil;

import com.matrixone.apps.unresolvedebom.MODStacks;
import com.matrixone.apps.unresolvedebom.UnresolvedEBOM;
import com.matrixone.apps.unresolvedebom.UnresolvedEBOMConstants;
import com.matrixone.apps.unresolvedebom.UnresolvedPart;

import com.matrixone.jdom.Attribute;
import com.matrixone.jdom.Document;
import com.matrixone.jdom.Element;
import com.matrixone.jsystem.util.StringUtils;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;

/**
 * The <code>emxUnresolvedPartBase</code> class contains implementation code for emxUnresolvedPart.
 *
 * @version X+4 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxUnresolvedPartBase_mxJPO extends emxPart_mxJPO {

	//2011x - Starts
	public static final String OPERARTION_ADD		=	"Add";
	public static final String OPERARTION_CUT		=	"Cut";
	private static final String MARKUP_NEW = "new";
	EffectivityFramework effectivity				= new EffectivityFramework();
	//2011x - Ends

    //TOTO add to constants and fetch from symbolic name
    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object.
     * @param args holds no arguments.
     * @throws Exception if the operation fails.
     * @since X+4
     */
    public emxUnresolvedPartBase_mxJPO(Context context, String[] args) throws Exception{
        super(context, args);
    }

    /**
     * This method is executed if a specific method is not specified.
     *
     * @param context the eMatrix <code>Context</code> object.
     * @param args holds no arguments.
     * @return int.
     * @throws Exception if the operation fails.
     * @since EC 9.5.JCI.0.
     */
    public int mxMain(Context context, String[] args)
    throws Exception
    {
        if (true)
        {
            throw new Exception("must specify method on emxUnresolvedPart invocation");
        }
        return 0;
    }

//PUE BOM Modification code Starts

    /**
     * @param context
     * @param args
     * @return HashMap
     * @throws Exception
     */
    public HashMap updateUEBOM(Context context, String args[]) throws Exception {
        return null;
    }

   /**
     * Method to remove the part from the Unresolved BOM
     *
     * @param context
     * @param relId
     * @param ecoId
     * @throws Exception
     */

    private void removePartFromUEBOM(Context context, String relId, String ecoId,String selectedpart,boolean sameECO, StringList cutRelIDList)
            throws Exception {
try {
	String operation = "'"+UnresolvedEBOMConstants.ATTRIBUTE_BOM_OPERATION+"' Remove";
        if (sameECO) {

        	StringList relIDsToDisconnect = new StringList();
     	    String parentId	= MqlUtil.mqlCommand(context, "print connection $1 select $2 dump",relId,"from.id");

        	/*Check for whether selected CHILD part and CORRESPONDING PARENT was added in multiple assemblies with same context change, if exists do not remove Affected Item relationship
        	of those if not exists means if existed in only bom assembly then remove affected Item relationship of corresponding */

     	    //expanding selected CHILD object in TO and FROM side and getting parent ,child list
     	    StringList parentObjListOfChild = getFromOrToIdsBasedOnIntermediateRel(context,ecoId,selectedpart,false,UnresolvedEBOMConstants.RELATIONSHIP_EBOM_PENDING,UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE);
        	StringList childObjListOfChild  = getFromOrToIdsBasedOnIntermediateRel(context,ecoId,selectedpart,true,UnresolvedEBOMConstants.RELATIONSHIP_EBOM_PENDING,UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE);

        	parentObjListOfChild.addAll(childObjListOfChild);

        	//expanding PARENT object in TO and FROM side and getting parent ,child list
        	StringList parentObjListOfParent = getFromOrToIdsBasedOnIntermediateRel(context,ecoId,parentId,false,UnresolvedEBOMConstants.RELATIONSHIP_EBOM_PENDING,UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE);
        	StringList childObjListOfParent  = getFromOrToIdsBasedOnIntermediateRel(context,ecoId,parentId,true,UnresolvedEBOMConstants.RELATIONSHIP_EBOM_PENDING,UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE);

        	parentObjListOfParent.addAll(childObjListOfParent);

            relIDsToDisconnect.addElement(relId);//EBOM Pending Rel Id

        	// if selected CHILD part exists in only one assembly with the given context change, then add child affected rel id into disconnect rel id list
        	if (parentObjListOfChild.size() == 1) {
        		String affectedItemRelIdForChild = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump",selectedpart,"to[" + RELATIONSHIP_AFFECTED_ITEM + "|from.id=="+ecoId+"].id");
        		if(!isNullOrEmpty(affectedItemRelIdForChild))
        			relIDsToDisconnect.addElement(affectedItemRelIdForChild);//Child affected Item rel id
        	}
        	// if PARENT for the selected CHILD part exists in only one assembly with the given context change, then add parent affected rel into disconnect rel id list
    		if (parentObjListOfParent.size() == 1) {
        		String affectedItemRelIdForParent = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump",parentId,"to[" + RELATIONSHIP_AFFECTED_ITEM + "|from.id=="+ecoId+"].id");
        		if(!isNullOrEmpty(affectedItemRelIdForParent))
        			relIDsToDisconnect.addElement(affectedItemRelIdForParent);//Parent affected Item rel id
    		}

    		ContextUtil.pushContext(context);
    		try {
    			DomainRelationship.disconnect(context, (String[]) relIDsToDisconnect.toArray(new String[relIDsToDisconnect.size()]));
    		} catch (Exception ex) {
    			throw ex;
    		} finally {
    			ContextUtil.popContext(context);
    		}
        } else {
        		cutRelIDList.addElement(relId);
        		UnresolvedEBOM.connectObjToRel(context, ecoId, relId, operation,UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE);
        		new UnresolvedEBOM().addPrerequisite(context, relId, ecoId);
        	}
	}
	catch (Exception e)
	{
		throw e;
	}
 }
/**
 * Returns StringList of expanded Objects FROM/TO side based on Intermediate Relationship
 * A			Add
 * |
 * |___B		ECO-1
 *
 * Here in above example, if we pass B as object id and ECO-1 to be checked on intermediate rel and returns A object finally.
 * @param context the eMatrix code context object
 * @param ecoId the eco Id to be checked on other side of intermediate relationship.
 * @param objID the selected part object id
 * @param isFromExpand a boolean value which decides whether fromside expand Or tosideexpand.
 * @param expandRel expand relationship
 * @param intermediateRel Intemediate relationship from Rel to Object, here its EBOM Pending--EBOM Change--->ECO ID
 * @return StringList of objects based on where condition passed
 * @throws Exception if the operation fails
 */

   public StringList getFromOrToIdsBasedOnIntermediateRel(Context context,String ecoId, String objID, boolean isFromExpand,String expandRel, String intermediateRel) throws Exception {
	   String selectable;
	   if(isFromExpand) {
		   selectable =  "from[" + expandRel + "|tomid[" + intermediateRel + "].from.id=="+ecoId+"].to.id";
	   }else {
		   selectable =  "to[" + expandRel + "|tomid[" + intermediateRel + "].from.id=="+ecoId+"].from.id";
	   }
	   String objIDsInfo = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump",objID,selectable);
	   StringList objIDList = FrameworkUtil.splitString(objIDsInfo, ",");
	   return objIDList;
	}

/**
     * Method to check whether part is been added in the same context
     *
     * @param context
     * @param relId
     * @param ecoId
     * @return
     * @throws Exception
     */
    private boolean isPartAddedInECO(Context context, String relId, String ecoId, boolean boolThrowException) throws Exception {
    	String ecoNameADDEC	   = "tomid[" + UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE + "|" + UnresolvedEBOMConstants.SELECT_ATTRIBUTE_BOM_OPERATION + "== Add].from.id";

        String strPUEECOAddID = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump $3", relId, ecoNameADDEC, "|");
        StringList listPUEECOAddID    = FrameworkUtil.split(strPUEECOAddID, "|");
        Locale Local = context.getLocale();

        if (boolThrowException) {
        	String ecoNameRemoveEC = "tomid[" + UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE + "|" + UnresolvedEBOMConstants.SELECT_ATTRIBUTE_BOM_OPERATION + "== Remove].from.id";
	        String strPUEECORemID = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump $3", relId, ecoNameRemoveEC, "|");

	        StringList listPUEECORemoveID = FrameworkUtil.split(strPUEECORemID, "|");

	        if (listPUEECOAddID != null && listPUEECOAddID.contains(ecoId) && !listPUEECORemoveID.isEmpty()) {
	        	String strMessage = EnoviaResourceBundle.getProperty(context, "emxUnresolvedEBOMStringResource", Local,"emxUnresolvedEBOM.ContextChange.RemoveOperationFailed");
	        	emxContextUtilBase_mxJPO.mqlNotice(context, strMessage);
	        	throw new Exception (strMessage);
	        }

	        if (listPUEECORemoveID != null && listPUEECORemoveID.contains(ecoId)) {
	        	String strMessage = EnoviaResourceBundle.getProperty(context, "emxUnresolvedEBOMStringResource", Local,"emxUnresolvedEBOM.ContextChange.AddOperationFailed");
	        	emxContextUtilBase_mxJPO.mqlNotice(context, strMessage);
	        	throw new Exception (strMessage);
	        }
        }

        return listPUEECOAddID.contains(ecoId);
    }

    /**
     * Method to add part to unresolved Part.
     *
     * @param context
     * @param parentPartId
     * @param childPartId
     * @param contextECOId
     * @param relAttributes
     * @return
     * @throws Exception
     */
    private String addPartToUEBOM(Context context, String parentPartId, String childPartId, String contextECOId, HashMap relAttributes,StringList addRelIDList) throws Exception {
    	DomainRelationship dr 	= null;
    	try
    	{
    		ContextUtil.startTransaction(context, true);
    		//2011x - Ends
    		ContextUtil.pushContext(context);
    		DomainObject parentObj = new DomainObject(parentPartId);
    		DomainObject childObj = new DomainObject(childPartId);
    		//connect the parent part and child part
    		dr = DomainRelationship.connect(context, parentObj, UnresolvedEBOMConstants.RELATIONSHIP_EBOM_PENDING, childObj);
    		String connectionId = dr.getName();

    		UnresolvedEBOM.connectObjToRel(context, contextECOId, connectionId, null, UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE);
    		//set attributes to EBOM relationship
    		dr.setAttributeValues(context, (Map) relAttributes);
    		MapList mapList = new MapList();
    		mapList.add(relAttributes);

    		addAffectedItem(context, parentPartId, childPartId, contextECOId);
    		if (addRelIDList != null)
    		   addRelIDList.addElement(connectionId);

    		ContextUtil.popContext(context);
    		ContextUtil.commitTransaction(context);
    	} catch (Exception exp) {
    		ContextUtil.abortTransaction(context);
    		throw exp;
    	}
        return dr.toString();
    }

    /**
     * This is to add the affected Items
     * @param context
     * @param selectedObjectId
     * @param contextECOId
     * @throws Exception
     */
    private void addAffectedItem(Context context, String selectedObjectId,
            String childPartId, String contextECOId) throws Exception {
        boolean connected = false;
        DomainObject parentObj = new DomainObject(selectedObjectId);
        DomainObject childObj = new DomainObject(contextECOId);
        String sRelType = (String) PropertyUtil.getSchemaProperty(context,"relationship_AffectedItem");
        StringList affectedItems = childObj.getInfoList(context, "relationship["+sRelType+"].to.id");
        DomainObject childPart = new DomainObject(childPartId);
        String childState           = childPart.getInfo(context, DomainConstants.SELECT_CURRENT);
        String aliasStateName       = PropertyUtil.getSchemaProperty(context,"policy",
                                                UnresolvedEBOMConstants.POLICY_CONFIGURED_PART,
                                                "state_Preliminary");
        String policyClassification = "policy.property[PolicyClassification].value";
        String sParentPolicyClass = childPart.getInfo(context,policyClassification);
        if (aliasStateName.equals(childState)
                && "Unresolved".equalsIgnoreCase(sParentPolicyClass)
                && !affectedItems.contains(childPartId)) {
			DomainRelationship.connect(context, childObj, sRelType, childPart);
        }
        if (affectedItems.contains(selectedObjectId)) {
            connected = true;
        }
        if (!connected) {
            DomainRelationship.connect(context, childObj, sRelType, parentObj);

        }
    }

    /**
     * Method to add the rows to the return xml file after the process.
     * @param context the eMatrix <code>Context</code> object.
     * @param args holds objectId.
     * @return HashMap.
     * @throws Exception If the operation fails.
     * @since X+4.
     *
     */
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public HashMap updateUnresolvedEBOM(Context context, String[] args) throws Exception {

    	HashMap<Object,Object> retMap = new HashMap<Object,Object>();
        Locale Local = context.getLocale();
        String supersededPartAlert = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource",Local, "emxUnresolvedEBOM.Alert.SupersededPart");
        retMap.put("Action", "execScript");
        StringBuffer updateoXMLBuffer = new StringBuffer();String rowFormat = "";
        StringBuffer returnMsgBuffer  = new StringBuffer("{ main:function() { ");
        boolean isENGSMBInstalled = EngineeringUtil.isENGSMBInstalled(context, false); //Added for IR-232706

        try {

            //ComponentsUtil.checkLicenseReserved(context, "ENO_ENG_TP");
			ComponentsUtil.checkLicenseReserved(context, "ENO_XCE_TP");

        	HashMap programMap  = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap  = (HashMap) programMap.get("requestMap");
            Document doc 	    = (Document) requestMap.get("XMLDoc");
            String contextECOId = (String) requestMap.get("PUEUEBOMContextChangeFilter_actualValue");

	        //2012x--Starts
	        contextECOId 			 = (!isNullOrEmpty(contextECOId))?contextECOId:"";
	        String  isWipBomAllowed  = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOM.WIPBOM.Allowed");

	        StringList cutRelIDList  = new StringList();
	        StringList addRelIDList  = new StringList();
	        HashMap<Object,Object> inlineMap = new HashMap<Object,Object>();
	        boolean isInWipMode		 = false;
	        //2012x--Ends

            Element  rootElement = doc.getRootElement();
            List     objList     = rootElement.getChildren("object");
            Iterator objItr      = objList.iterator();

            HashMap  relAttrMap;String operation = "";

            String[] attributeKeys = {  DomainConstants.ATTRIBUTE_FIND_NUMBER,
					        			DomainConstants.ATTRIBUTE_REFERENCE_DESIGNATOR,
					        			DomainConstants.ATTRIBUTE_COMPONENT_LOCATION,
					        			DomainConstants.ATTRIBUTE_QUANTITY,
					        			DomainConstants.ATTRIBUTE_USAGE,
					        			DomainConstants.ATTRIBUTE_NOTES };

            String[] objSizeArr    = {  "emxEngineeringCentral.Common.ASize",
					        			"emxEngineeringCentral.Common.BSize",
					        			"emxEngineeringCentral.Common.CSize",
					        			"emxEngineeringCentral.Common.DSize",
					        			"emxEngineeringCentral.Common.GeneralSize"};

            StringList objAutoNameList = getObjSizeList(context,objSizeArr, "emxEngineeringCentralStringResource", "en");


            while (objItr.hasNext()) {
            	String parentPartId;
				String childPartId;
				String rowId;
				String rowInfo;
            	String strExpression;

                Element eleChild = (Element) objItr.next();
                String  relId    = eleChild.getAttribute("relId") != null ? eleChild.getAttribute("relId").getValue(): "";

            	StringList selectables  = new StringList(DomainConstants.SELECT_CURRENT);
            	selectables.addElement("policy.property[PolicyClassification].value");
            	Attribute attrOperation = eleChild.getAttribute("markup");

                String strObjectId      = eleChild.getAttribute("objectId").getValue();

                // In case of Edit ,parent Id will be explicitly comes in the postDataXML where as in other operations it will comes as objectId.

           	 	String parentObjId  	= (attrOperation != null)?eleChild.getAttribute("parentId").getValue():eleChild.getAttribute("objectId").getValue();
            	Map parentPartInfo      = (Map)DomainObject.newInstance(context, parentObjId).getInfo(context, selectables);
            	String parentObjPolicy  = (String)parentPartInfo.get("policy.property[PolicyClassification].value");
            	String parentObjState   = (String)parentPartInfo.get(DomainConstants.SELECT_CURRENT);

            	if (!"Unresolved".equalsIgnoreCase(parentObjPolicy)) {
            		retMap.put("Action", "ERROR");
            		String invalidSelectionMsg = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource", Local,"emxUnresolvedEBOM.Alert.DoNotAllowUnUnresolvedWithinResolved");
            		retMap.put("Message", invalidSelectionMsg);
            		return retMap;

            	}
                isInWipMode = ("true".equalsIgnoreCase(isWipBomAllowed) && "Preliminary".equals(parentObjState))?true:false;
	            String existingRelIdActExpr = "";//2012x


	            //attrOperation is not null only when markup is changed
	            if (attrOperation != null) {
	                operation 	 = eleChild.getAttribute("markup").getValue();
	                parentPartId = eleChild.getAttribute("parentId").getValue();

                    //102871 starts
                    if (isParentInSupersededState(context,parentPartId)) {
                        retMap.put("Action","Error");
                        retMap.put("Message",supersededPartAlert);
                        return retMap;
                    }
                    //102871 ends


	                rowId = eleChild.getAttribute("rowId").getValue();
	                rowFormat = "[rowId:" + rowId + "]";
	                rowInfo = getParentRowIdInfo(rowId);
	                HashMap oldRelColumnMap = (HashMap) new DomainRelationship(relId).getAttributeMap(context);
					//Need to remove Applicability attributes
					oldRelColumnMap.remove("Applicability Status");
	                oldRelColumnMap.remove("Applicability Details");
	                oldRelColumnMap.remove("Applicability Transaction");
					//end
	                HashMap chgColumnMap 	= getChangedColumnMapFromElement(context, eleChild);
	                //2012x--Starts-if contextECO is empty the edit the WIP BOM

	              //Added for IR-232706 start
	                if (isENGSMBInstalled) {
                    	String mqlQuery = new StringBuffer(100).append("print bus $1 select $2 dump").toString();
                    	String vpmControlState = MqlUtil.mqlCommand(context, mqlQuery,parentPartId,"from["+RELATIONSHIP_PART_SPECIFICATION+"|to.type.kindof["+EngineeringConstants.TYPE_VPLM_CORE_REF+"]].to.attribute["+EngineeringConstants.ATTRIBUTE_VPM_CONTROLLED+"]");

                    	String	vpmVisible = getStringValue(chgColumnMap, "VPMVisible");
                    	chgColumnMap.remove("VPMVisible");
    					 //If part is not in VPM Control set the isVPMVisible value according to user selection.
						if (isValidData(vpmVisible) && !"true".equalsIgnoreCase(vpmControlState))
							chgColumnMap.put(EngineeringConstants.ATTRIBUTE_VPM_VISIBLE, vpmVisible);
            		}
	              //Added for IR-232706 end

	                if(isInWipMode)
	                {
	                   //if user not edited the current effectivity,the no need to update rel expression
	                	String currEff = (String)chgColumnMap.get("CurrentEffectivity");

	                	//in case of mass update user can give empty expression also
	                	if ("".equals(currEff) || " ".equals(currEff)) {
							retMap.put("Action", "ERROR");
							String effMesssage = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource", Local,"emxUnresolvedEBOM.CurrentEffectivity.alert");
			 				retMap.put("Message", effMesssage);
					    	return retMap;
	                	}
	                	if (!isNullOrEmpty(currEff)) {
	                		effectivity.updateRelExpression(context, relId,currEff);
	                    	chgColumnMap.remove("CurrentEffectivity");
	                   }
	                	//set the newly edited attributes on existing rel id
	                	new DomainRelationship(relId).setAttributeValues(context, chgColumnMap);
	                }//2012x--Ends

                  //2012x--Added condition check for to proceed with normal change process
	              else {
	                	boolean sameECO = isPartAddedInECO(context, relId, contextECOId, false);
		                if (!sameECO) {
		                    // add with new attributes
		                    String newRelId = addPartToUEBOM(context, parentPartId,strObjectId, contextECOId, oldRelColumnMap,addRelIDList);
		                    new DomainRelationship(newRelId).setAttributeValues(context, chgColumnMap);
	                        removePartFromUEBOM(context, relId, contextECOId,"",false,cutRelIDList);
	                        returnMsgBuffer.append(" FreezePaneregister(\"")
	                        			   .append(rowInfo)
	                        			   .append("\"); rebuildView();")
	                        			   .append(" emxEditableTable.addToSelected('<mxRoot><action>add</action><data status=\"commited\">")
	                        			   .append(" <item oid=\""+strObjectId+"\" relType=\"relationship_EBOMPending\" relId=\""+newRelId+"\" pid=\""+parentPartId+"\" direction=\"\"></item>")
	                        			   .append(" </data></mxRoot>');")
	                        			   .append(" FreezePaneunregister(\"")
	                        			   .append(rowInfo)
	                        			   .append("\");");

		                    } else {
		                        new DomainRelationship(relId).setAttributeValues(context, chgColumnMap);
		                    }
	                   }

	              }
            //2012x-Starts-- Code to start for addExisting and remove operations for WIP BOM
            else if (isInWipMode) {
            	List objectList    = eleChild.getChildren("object");
                Iterator objectItr = objectList.iterator();

                while (objectItr.hasNext()){
                	Element element    = (Element) objectItr.next();
                	HashMap colMap     = getChangedColumnMapFromElement(context, element);
					String currEffExpr = (String) colMap.get("CurrentEffectivity");

                    parentPartId     = eleChild.getAttribute("objectId").getValue();
                    if (isParentInSupersededState(context,parentPartId)) {
                        retMap.put("Action","Error");
                        retMap.put("Message",supersededPartAlert);
                        return retMap;
                    }
                    //102871 ends
                    operation        = element.getAttribute("markup").getValue();
                    relId            = element.getAttribute("relId").getValue();
                    childPartId      = element.getAttribute("objectId").getValue();
					rowId 			 = element.getAttribute("rowId").getValue();
					rowFormat 		 = "[rowId:" + rowId + "]";
					strExpression	 = (String)colMap.get("CurrentEffectivity");
					//get the Actual value from the bean if the expression comes as displayValue.
					strExpression    = (strExpression != null && !"null".equals(strExpression))?strExpression.indexOf(EffectivityFramework.KEYWORD_PHYSID_PREFIX)!=-1?strExpression:existingRelIdActExpr:"";

					if (isNullOrEmpty(currEffExpr) && !"cut".equalsIgnoreCase(operation)) {
						retMap.put("Action", "ERROR");
						String effMesssage = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource", Local,"emxUnresolvedEBOM.CurrentEffectivity.alert");
						retMap.put("Message", effMesssage);

				    	return retMap;
					}
					//code to execute new operation for WIP BOM
					if (operation  != null && "new".equals(operation)) {
						inlineMap.put("parentOID", parentPartId);
						inlineMap.put("contextECOId", contextECOId);
						inlineMap.put("updateOXMLBuffer", updateoXMLBuffer);
						inlineMap.put("childObjectId", childPartId);
						inlineMap.put("rowId", rowId);
						inlineMap.put("markup", operation);
						inlineMap.put("columns", colMap);

						HashMap inlineRetMap = inlineCreateNewForConfiguredBOM(context,inlineMap,attributeKeys,objAutoNameList,isInWipMode);
				        String action = (String)inlineRetMap.get("Action");
				        if ("Error".equalsIgnoreCase(action)) {
	                        retMap.put("Action","Error");
	                        retMap.put("Message",(String)inlineRetMap.get("Message"));
	                        return retMap;
				        }
	                    updateoXMLBuffer = (StringBuffer)inlineRetMap.get("updateOXMLBuffer");
	                 } //code to execute add operation for WIP BOM
                    if (operation  != null && "add".equals(operation)) {

                    	relAttrMap = (HashMap)getAttributes(colMap, attributeKeys);

                    	//Added for IR-232706 start
                    	if (isENGSMBInstalled) {
                        	String mqlQuery = new StringBuffer(100).append("print bus $1 select $2 dump").toString();
                        	String vpmControlState = MqlUtil.mqlCommand(context, mqlQuery,parentPartId,"from["+RELATIONSHIP_PART_SPECIFICATION+"|to.type.kindof["+EngineeringConstants.TYPE_VPLM_CORE_REF+"]].to.attribute["+EngineeringConstants.ATTRIBUTE_VPM_CONTROLLED+"]");

                        	String	vpmVisible = getStringValue(colMap, "VPMVisible");
        					 //If part is not in VPM Control set the isVPMVisible value according to user selection.
    						if (isValidData(vpmVisible) && !"true".equalsIgnoreCase(vpmControlState))
    							relAttrMap.put(EngineeringConstants.ATTRIBUTE_VPM_VISIBLE, vpmVisible);
                		}
                    	//Added for IR-232706 end

	                    String newRelId  = createWIPBOM(context, parentPartId, childPartId,relAttrMap, strExpression,operation);
	                    updateoXMLBuffer = constructUpdateOXMLStr(updateoXMLBuffer,childPartId,rowId,newRelId,"add");

	                    //code to execute remove operation for WIP BOM
	                 } else if (operation != null && "cut".equals(operation)) {

                    	existingRelIdActExpr = UnresolvedPart.getEffectivityValueForRelId(context, relId, EffectivityFramework.ACTUAL_VALUE);
		                DomainRelationship.disconnect(context, relId);
	                    updateoXMLBuffer = constructUpdateOXMLStr(updateoXMLBuffer,childPartId,rowId,relId,"cut");
                    }
                }
            } //2012x-Ends

            else {
                //Add and Cut functionalities through Change Process
                List objectList = eleChild.getChildren("object");
                Iterator objectItr = objectList.iterator();
                while (objectItr.hasNext()) {
                    Element element = (Element) objectItr.next();
                    parentPartId    = eleChild.getAttribute("objectId").getValue();

                    if (isParentInSupersededState(context,parentPartId)) {
                        retMap.put("Action","Error");
                        retMap.put("Message",supersededPartAlert);
                        return retMap;
                    }

					if ("".equals(contextECOId)) {
						retMap.put("Action", "ERROR");
						String ecoMesssage = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource", Local,"emxUnresolvedEBOM.CommonView.Alert.ContextECOSelection");
		 				retMap.put("Message", ecoMesssage);

				    	return retMap;
					}

                    operation       = element.getAttribute("markup").getValue();
                    relId 			= element.getAttribute("relId").getValue();
                    childPartId 	= element.getAttribute("objectId").getValue();
                    rowId 			= element.getAttribute("rowId").getValue();
                    rowFormat 		= "[rowId:" + rowId + "]";

                    rowInfo = getParentRowIdInfo(rowId);
                    HashMap hmpAttributes = getChangedColumnMapFromElement(context, element);
					//code to execute new operation for WIP BOM
					if (operation  != null && "new".equals(operation)) {
						inlineMap.put("parentOID", parentPartId);
						inlineMap.put("contextECOId", contextECOId);
						inlineMap.put("updateOXMLBuffer", updateoXMLBuffer);
						inlineMap.put("childObjectId", childPartId);
						inlineMap.put("rowId", rowId);
						inlineMap.put("markup", operation);
						inlineMap.put("columns", hmpAttributes);

						HashMap inlineRetMap = inlineCreateNewForConfiguredBOM(context,inlineMap,attributeKeys,objAutoNameList,isInWipMode);
				        String action = (String)inlineRetMap.get("Action");
				        if ("Error".equalsIgnoreCase(action)) {
	                        retMap.put("Action","Error");
	                        retMap.put("Message",(String)inlineRetMap.get("Message"));
	                        return retMap;
				        }
	                    updateoXMLBuffer = (StringBuffer)inlineRetMap.get("updateOXMLBuffer");
	                 } //code to execute add operation for WIP BOM

                    if (operation != null && "add".equals(operation)) {
                        //2012x--Invoked method to get column Map to avoid duplication of code

                    	relAttrMap = (HashMap)getAttributes(hmpAttributes, attributeKeys);
                        String newRelId  = addPartToUEBOM(context, parentPartId, childPartId, contextECOId,relAttrMap,addRelIDList);
                        updateoXMLBuffer = constructUpdateOXMLStr(updateoXMLBuffer,childPartId,rowId,newRelId,"add");

                        } else if (operation != null && "cut".equals(operation)) {

                        	boolean sameECO = isPartAddedInECO(context, relId, contextECOId, true);
                            removePartFromUEBOM(context, relId, contextECOId,childPartId,sameECO,cutRelIDList);
                            if (sameECO) {
                                //return msg to remove the row from the view
                                updateoXMLBuffer = constructUpdateOXMLStr(updateoXMLBuffer,childPartId,rowId,relId,"cut");
                            } else {
                                addAffectedItem(context, parentPartId, childPartId, contextECOId);
                                String strXML= "/mxRoot/rows//r[@status = 'cut'and @id='"+rowId+"']";
                                returnMsgBuffer.append(" emxUICore.selectSingleNode(oXML.documentElement,\""+strXML+"\").removeAttribute(\"status\");");
                            }
                        }
                    }
                }
            }
            if (addRelIDList.size() > 0) {
            	String[] addRelIdArr = java.util.Arrays.copyOf(addRelIDList.toArray(), addRelIDList.size(), String[].class);
            	effectivity.updateRelProposedExpression(context, addRelIdArr, contextECOId, OPERARTION_ADD);
            }
            if (cutRelIDList.size() > 0) {
            	String[] cutRelIdArr = java.util.Arrays.copyOf(cutRelIDList.toArray(), cutRelIDList.size(), String[].class);
            	effectivity.updateRelProposedExpression(context, cutRelIdArr, contextECOId, OPERARTION_CUT);
            }
            if(!"".equals(updateoXMLBuffer.toString())) {
            	String updateoXMLStr = StringUtils.replaceAll(updateoXMLBuffer.toString(), "&", "&amp;");
            	returnMsgBuffer.append("var objDOM = emxUICore.createXMLDOM();")
            				   .append("objDOM.loadXML('<mxRoot><action>success</action><message></message><data status=\"commited\">"+updateoXMLStr+"</data></mxRoot>');")
            				   .append("emxUICore.checkDOMError(objDOM);updateoXML(objDOM);refreshStructureWithOutSort();")
            				   .append("arrUndoRows = new Object();").append("postDataXML.loadXML(\"<mxRoot/>\");");
            }
            else {
	               //In case of edit/cut operations
	               if ("changed".equalsIgnoreCase(operation) || "cut".equalsIgnoreCase(operation))
	              {
	            	if (isInWipMode) { //in case of WIP Mode edit just need to refresh the SB.
	    	            retMap.put("Action", "refresh");
	    	            returnMsgBuffer = new StringBuffer();
	            	}
	            	else { //in case of NON WIP Mode need to refresh whole structure to add/cut information
	            		returnMsgBuffer.append("refreshRows();arrUndoRows = new Object();postDataXML.loadXML(\"<mxRoot/>\");");
	            	}
		        }
          }
            //if buffer is not empty then only append braces
            if (returnMsgBuffer.length() > 0) {
            		returnMsgBuffer.append("}}");
            }


          retMap.put("Message", returnMsgBuffer.toString());

        } catch (Exception e) {
        	e.printStackTrace();
        	retMap.put("Action", "ERROR"); // If any exception is there send "Action" as "ERROR"

 			if (e.toString().indexOf("license") > -1) { // If any License Issue throw the exception to user.
 				retMap.put("Message", rowFormat);
 				throw e;
 			}
 			else if ((e.toString().indexOf("recursion")) != -1) {
 				String recursionMesssage = EnoviaResourceBundle.getProperty(context,
						   "emxEngineeringCentralStringResource", Local,"emxEngineeringCentral.RecursionError.Message");
 				retMap.put("Message", rowFormat + recursionMesssage);
 			}
 			else if ((e.toString().indexOf("recursion")) == -1 && ((e.toString().indexOf("Check trigger")) != -1)) {
 				String tnrMesssage = EnoviaResourceBundle.getProperty(context,
 	 					"emxEngineeringCentralStringResource", Local,"emxEngineeringCentral.TNRError.Message");
 				retMap.put("Message", rowFormat + tnrMesssage);
 			}
 			else {
 				String strExcpn = e.toString();
 				int j = strExcpn.indexOf(']');
 			    strExcpn = strExcpn.substring(j + 1, strExcpn.length());
 			    retMap.put("Message", rowFormat + strExcpn);
 			}

		}

        return retMap;
    }
     /**
      * Method to return the parent rowId info
      * @param rowId
      * @return
      */
     private String getParentRowIdInfo(String rowId) {
         String[] emxTableRowId = StringUtils.split(rowId,",");
         String newRowId = "";
         for(int i=0; i<emxTableRowId.length-1; i++) {
             if(i == 0){
                 newRowId = emxTableRowId[i];
             } else {
                 newRowId = newRowId+","+emxTableRowId[i];
             }
         }
         return "|||"+newRowId;
     }

    /**
     * Method to get the chnaged columns as HashMap
     * @param context
     * @param elm
     * @return
     */
     //public static HashMap getChangedColumnMapFromElement(Context context,Element elm,boolean editMode) {
     public static HashMap getChangedColumnMapFromElement(Context context,Element elm) {

	 HashMap<String,String> changedColumnsMap = new HashMap<String,String>();
     Element clm 		       = null;
     String  name 			   = "";
     String  value   		   = "";

     //List     colList = editMode?elm.getChildren("column"):elm.getChild("object").getChildren("column");
     List     colList = elm.getChildren("column");
     Iterator colItr  = colList.iterator();
     while (colItr.hasNext()) {
		         clm   = (Element) colItr.next();
		         name  = clm.getAttributeValue("name");
		         value = clm.getText();
		         changedColumnsMap.put(name, value);
	         }

     	return changedColumnsMap;
   }

//Context change code starts
/**
 * Returns a MapList of the ECO ids when Modstack filter is selected
 * for a given context.
 * @param context the eMatrix <code>Context</code> object.
 * @param args contains a packed HashMap containing objectId of object
 * @return MapList.
 * @since EngineeringCentral X3
 * @throws Exception if the operation fails.
*/
public MapList getContextChange(Context context, String args[])throws Exception
{
    MapList ecoList=new MapList();
    HashMap paraMap = (HashMap) JPO.unpackArgs(args);
    String  ProductObjectId =(String)paraMap.get("objectId");
    //get Modstack Filter Info
    String  modStackObjectName=(String)paraMap.get("cmdModStackECOInfo");
    if(modStackObjectName!=null&&!modStackObjectName.equals(""))
    {
		MODStacks modObject=new MODStacks(context,ProductObjectId,UnresolvedEBOMConstants.TYPE_PUE_ECO);

    //get the pending ECOs which are connected to the MOD stack
        ecoList=modObject.getPUEChanges(modStackObjectName,modObject.PENDING_STATUS);
    }
    ecoList.add(0,new Integer(ecoList.size()));
    return ecoList;
}

/**
 * lookupEntries method checks the object entered manually is exists or not in the database
 * Method to Inline create & connect new Part objects in EBOM powerview IndentedTable
 * This method is invoked on clicking on Apply button in EBOM
  * @param args String array having the object Id(s) of the part.
 * @throws FrameworkException if creation of the Part Master object fails.
 */

private HashMap inlineCreateNewForConfiguredBOM(Context context,HashMap changedRowMap,String[] attributeKeys,StringList objAutoNameList,boolean isInWipMode)throws Exception {
	HashMap doc = new HashMap();
	HashMap hmRelAttributesMap;
    HashMap columnsMap;
	Map smbAttribMap;
    String relId;

	String rowFormat = "";
    String sUser     = context.getUser();
    Locale Local = context.getLocale();

    String parentObjectId = (String) changedRowMap.get("parentOID");
    String contextECOId   = (String) changedRowMap.get("contextECOId");
    StringBuffer updateOXMLBuffer = (StringBuffer)changedRowMap.get("updateOXMLBuffer");
    ContextUtil.pushContext(context);

	try
	{
	DomainObject childObj;

			try {
				relId = "";
				String childObjectId = (String) changedRowMap.get("childObjectId");
				String sRowId = (String) changedRowMap.get("rowId");
				rowFormat = "[rowId:" + sRowId + "]";
				String markup = (String) changedRowMap.get("markup");
				columnsMap = (HashMap) changedRowMap.get("columns");
				String strUOM = (String) columnsMap.get("UOM");
				String desc = (String) columnsMap.get("Description");
				hmRelAttributesMap = getAttributes(columnsMap, attributeKeys);

				 if (MARKUP_NEW.equals(markup)) {

					String objectName = (String) columnsMap.get("Name");
					String objectType = (String) columnsMap.get("Type");
					String objectRev = (String) columnsMap.get("Revision");
					String objectPolicy = (String) columnsMap.get("Policy");
					String objectVault = (String) columnsMap.get("Vault");
					String currEffExpr = (String) columnsMap.get("CurrentEffectivity");

					if (isInWipMode && "".equals(currEffExpr)) {

						doc.put("Action", "ERROR");
						String effMesssage = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource", Local,"emxUnresolvedEBOM.CurrentEffectivity.alert");
		 				doc.put("Message", effMesssage);
				    	return doc;
					}

					smbAttribMap = new HashMap();
					smbAttribMap.put(DomainConstants.ATTRIBUTE_UNIT_OF_MEASURE, strUOM);

					childObj = DomainObject.newInstance(context);
					childObj = createchildObj(context, objectType, objectName, objectRev, objectPolicy, objectVault, childObj, objAutoNameList.contains(objectName));
					childObj.setOwner(context, sUser);

					if (isValidData(desc)) {
						childObj.setDescription(context, desc);
					}
					childObj.setAttributeValues(context, smbAttribMap);

                   changedRowMap.put("childObj", childObj);
                   childObjectId = childObj.getId();
                   if (isInWipMode) {
                       relId = createWIPBOM(context, parentObjectId, childObjectId,hmRelAttributesMap, currEffExpr,null);
                   }
                   else {
                	   relId = addPartToUEBOM(context, parentObjectId, childObjectId, contextECOId,hmRelAttributesMap,null);
                	   effectivity.updateRelProposedExpression(context, relId, contextECOId, OPERARTION_ADD);

                   }
				}
				    updateOXMLBuffer = constructUpdateOXMLStr(updateOXMLBuffer,childObjectId,sRowId,relId,"new");

			} catch (Exception e) {
				if (e.toString().indexOf("license") > -1) {
					throw e;
				}
				throw new Exception(rowFormat + e);
			}

		doc.put("Action", "success"); // Here the action can be "Success" or
		// "refresh"
		doc.put("updateOXMLBuffer", updateOXMLBuffer);// Adding the key "updateOXMLString" with changed content
	} catch (Exception e) {
		doc.put("Action", "ERROR"); // If any exception is there send "Action" as "ERROR"

		if (e.toString().indexOf("license") > -1) { // If any License Issue throw the exception to user.
			doc.put("Message", rowFormat + e.toString());
			throw e;
		}

		if ((e.toString().indexOf("recursion")) != -1) {
			String recursionMesssage = EnoviaResourceBundle.getProperty(context,
					"emxEngineeringCentralStringResource", Local,"emxEngineeringCentral.RecursionError.Message");
			doc.put("Message", rowFormat + recursionMesssage);
		}

		else if ((e.toString().indexOf("recursion")) == -1 && ((e.toString().indexOf("Check trigger")) != -1)) {
			String tnrMesssage = EnoviaResourceBundle.getProperty(context,
 					"emxEngineeringCentralStringResource", Local,"emxEngineeringCentral.TNRError.Message");
			doc.put("Message", rowFormat + tnrMesssage);
		}

		else {
			String strExcpn = e.toString();
			int j = strExcpn.indexOf(']');
		    strExcpn = strExcpn.substring(j + 1, strExcpn.length());
			doc.put("Message", rowFormat + strExcpn);
		}

	} finally {
		ContextUtil.popContext(context);
	}

	return doc;

}

private StringList getObjSizeList(Context context,String[] objSizeArr, String resource, String languageStr) throws Exception {
	 int length = length (objSizeArr);
	 StringList list = new StringList(length);
	 String temp;
	 Locale Local = context.getLocale();
	 for (int i = 0; i < length; i++) {
		// temp = UINavigatorUtil.getI18nString(objSizeArr[i], resource, languageStr);
		 temp = EnoviaResourceBundle.getProperty(context, resource, Local,objSizeArr[i]);
		 list.add(temp);
	 }
	 return list;
}
private HashMap getAttributes(HashMap map, String[] keys) throws Exception {
	 int length = length (keys);
	 HashMap mapReturn = new HashMap(length);
	 String data;
	 for (int i = 0; i < length; i++) {
		 data = getStringValue(map, keys[i]);
		 if (isValidData(data)) {
			 mapReturn.put(keys[i], data);
		 }
	 }
	 return mapReturn;
}
private String getStringValue(Map map, String key) {
	return (String) map.get(key);
}
private int length(Object[] array) {
	return array == null ? 0 : array.length;
}
private boolean isValidData(String data) {
	return ((data == null || "null".equals(data)) ? 0 : data.trim().length()) > 0;
}



/**
 * Returns a StringList of Prerequisite ECOs
 * for a given context.
 * @param context the eMatrix <code>Context</code> object.
 * @param args contains a packed HashMap containing the eco ids which are connected to a product
 * @return StringList.
 * @since EngineeringCentral X3
 * @throws Exception if the operation fails.
*/

public StringList getPrerequisiteECO(Context context,String args[])throws Exception
{
    HashMap programMap = (HashMap)JPO.unpackArgs(args);
    MapList objList = (MapList)programMap.get("objectList");
    StringBuffer columnVals = new StringBuffer();
    StringList result = new StringList();
    //if object List not empty then iterate through the object list and get all the ECO ids
    if(objList != null && objList.size()>0)
    {
        Iterator i = objList.iterator();
        while (i.hasNext())
        {
            Map map = (Map) i.next();
            String strId = (String)map.get("id");
            DomainObject domainObjectECO=new DomainObject(strId);

			StringBuffer sbTypePattern = new StringBuffer(UnresolvedEBOMConstants.TYPE_PUE_ECO);

            String relToExpand = PropertyUtil.getSchemaProperty(context,"relationship_Prerequisite");
            StringList selectStmts  = new StringList(1);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            selectStmts.addElement(DomainConstants.SELECT_NAME);
            //get all the prerequisite ECOS
            MapList mapList = domainObjectECO.getRelatedObjects(context,
                                                  relToExpand,                // relationship pattern
                                                  sbTypePattern.toString(),   // object pattern
                                                  selectStmts,                // object selects
                                                  null,                       // relationship selects
                                                  false,                       // to direction
                                                  true,                      // from direction
                                                 (short)1,                   // recursion level
                                                  null,                      // object where clause
                                                  null);
            for(int j=0;j<mapList.size();j++)
            {
                Map change = (Map)mapList.get(j);
                columnVals.append("<a href=\"javascript:showDialog('../common/emxTree.jsp?objectId=");
                columnVals.append(change.get("id"));
                columnVals.append("')\">");
                columnVals.append(change.get("name"));
                columnVals.append("</a>&#160;");
            }
            result.addElement(columnVals.toString());
            columnVals=new StringBuffer();
        }
    }
    return result;
}

/**
The  method that builds the return map for Table and Indented Table
pre processing methods.

@param actionValue     the calling method's Action string (Continue or Stop).

@param messageValue    the calling method's Message string (normally built by
a previous call to the tableBuildMessageString method).

@param objectList      the MapList of objects (normally built by a previous call
to the tableBuildObjectList method).

@return returns to the calling processing method a HashMap that contains the
incoming actionValue, messageValue, and objectList.

@since XBOM
*/

public HashMap tableBuildReturnMap (String actionValue, String messageValue, MapList objectList) throws Exception
{

HashMap returnMap = new HashMap(3);
String actionKey = "Action";
String messageKey = "Message";
String objectListKey = "ObjectList";

returnMap.put(actionKey,actionValue);
returnMap.put(messageKey,messageValue);
returnMap.put(objectListKey,objectList);
return returnMap;

}

/**
This method will check whether the context product has been selected or not.
If user tries to edit any information with out selecting context product then
it will alert the user to select context product

@param args - a packed HashMap containing toolbar related information.

@return returns to the calling processing method a MapList that contains the
'modified' object list.

@since XBOM
*/

@com.matrixone.apps.framework.ui.PreProcessCallable
public HashMap hasProductSelected(Context context, String[] args) throws Exception
{
    HashMap inputMap = (HashMap)JPO.unpackArgs(args);
    HashMap requestMap = (HashMap) inputMap.get("requestMap");
    HashMap tableData = (HashMap) inputMap.get("tableData");
    MapList objectList = (MapList) tableData.get("ObjectList");
    String actionValue = "continue";
    Locale Local = context.getLocale();
    //------ Fix for BUG - 359207 starts ----
    //Desc: Change view value in reequest map is in English, hence compare the same with english string.
    String sChangeViewCurrent = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource",new Locale("en"),"emxUnresolvedEBOM.BOMPowerView.ChangeView.Current");
    //------ Fix Ends.. --------
    String changeViewMessageValue =EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource",Local,"emxUnresolvedEBOM.ChangeViewFilter.AlertMessage");
    HashMap returnMap = new HashMap();
    //if Product filter value is empty then alert the user
	if(sChangeViewCurrent.equals(requestMap.get("PUEUEBOMChangeViewFilter")))
    {
        actionValue="Stop";
        returnMap = (HashMap) tableBuildReturnMap (actionValue,changeViewMessageValue,objectList);
    }

return returnMap;

}
//Context change code ends

     //PUE BOM Modification ends

//PUE Filtering code Starts

     /**
     * Method to get the Unresolved and Resolved BOM Structure of an Unresolved Part
     *
     * @param context the eMatrix <code> Context </code> object
     * @param args holds objectId as first argument
     * @return MapList of the BOM Strucutre of the Part
     * @throws Exception
     * @since X+4
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getUEBOM (Context context, String[] args) throws Exception
    {
		MapList uebomList = new MapList();

		uebomList=getFilteredUEBOM(context, args);
		Iterator itr = uebomList.iterator();
		MapList tList = new MapList();
		while(itr.hasNext())
		{
		    HashMap newMap = new HashMap((Map)itr.next());
		    newMap.put("selection", "multiple");
		    tList.add (newMap);
		}
		uebomList.clear();
		uebomList.addAll(tList);

		HashMap hmTemp = new HashMap();
		hmTemp.put("expandMultiLevelsJPO","true");
		uebomList.add(hmTemp);

		return uebomList;
    }

    /**
     * Method to get an Unresolved BOM structure based on the applied filters.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds objectId as first argument
     * @return MapList containing BOM
     * @throws Exception if the operation fails
     * @since  X+4
     */
    public MapList getFilteredUEBOM (Context context, String[] args)throws Exception
    {
        HashMap paramMap = (HashMap)JPO.unpackArgs(args);
        MapList tempList = new MapList();

        String partId = (String) paramMap.get("objectId");
        int nExpandLevel;
        String sChangeView = (String) paramMap.get("PUEUEBOMChangeViewFilter");
       // String strChangeView = FrameworkProperties.getProperty("emxUnresolvedEBOM.BOMPowerView.ChangeView");
        String strChangeView = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOM.BOMPowerView.ChangeView");
        StringList strChangeViewList = FrameworkUtil.split(strChangeView,",");
        String sChangeViewCurrent = (String)strChangeViewList.get(0);
        String sECOFilterID = (String) paramMap.get("PUEUEBOMECOFilter_actualValue");
        String sPCFilterId = (String) paramMap.get("PUEUEBOMProductConfigurationFilter_actualValue");

        String sCopyFrom = (String) paramMap.get("CopyFrom");
        String sWhere = "";
        StringList selectStmts = new StringList(2);
        StringList selectRelStmts = new StringList(1);
        String filterActualExpression = "";//2011x
        String filterExpression = "";
        String view = EffectivityFramework.PROPOSED_VIEW;
        String sExpandLevels = (String)paramMap.get("emxExpandFilter");
        if(sExpandLevels==null || sExpandLevels.length()==0)
    	{
    		nExpandLevel = 1;
    	} else if("All".equalsIgnoreCase(sExpandLevels)) {
	            nExpandLevel = 0;
    	} else {
    		nExpandLevel = Integer.parseInt(sExpandLevels);
    	}


        sECOFilterID = sECOFilterID!=null ? sECOFilterID : "";

        HashMap hmEffectivityCriteria = new HashMap();
        hmEffectivityCriteria.putAll(paramMap);

        try {

            Part partObj = new Part(partId);
            selectStmts.addElement(SELECT_ID);
            selectStmts.addElement("physicalid");

            selectRelStmts.addElement(SELECT_RELATIONSHIP_ID);
            selectRelStmts.addElement(SELECT_FROM_ID); //Added For Copy From

            String sRelName = DomainConstants.RELATIONSHIP_EBOM+","+UnresolvedEBOMConstants.RELATIONSHIP_EBOM_PENDING;
            String sTypeName = TYPE_PART;

			// If operation is Copy From, recurse to all levels
			if(sCopyFrom !=null && !"".equals(sCopyFrom)){
				nExpandLevel = 0;
			}

            StringList ecoIdList = new StringList();
            if (isValidData(sECOFilterID)) {
            	ecoIdList = getPreRequisiteECOIds(context, FrameworkUtil.split(sECOFilterID, ","));
            }

            // Set where clause based on change view filter
            if (sChangeView!=null && sChangeView.equals(sChangeViewCurrent))
            {
            	//2011x - Starts
            	sRelName	= DomainConstants.RELATIONSHIP_EBOM;
            	view		= EffectivityFramework.CURRENT_VIEW;
            	//2011x - Ends
            }

            // if ECO filter not empty, pass view as current for VPM to return proper results.
            if (!ecoIdList.isEmpty())
            	view = EffectivityFramework.CURRENT_VIEW;

            filterActualExpression = (String) paramMap.get("CFFExpressionFilterInput_actualValue");

            if (UIUtil.isNotNullAndNotEmpty(sPCFilterId)) {
            	String sPCFilterExpr = UnresolvedPart.getExpressionForPC(context, FrameworkUtil.split(sPCFilterId, ","));

            	if (UIUtil.isNotNullAndNotEmpty(sPCFilterExpr)) {
            		filterActualExpression = UIUtil.isNullOrEmpty(filterActualExpression)
				            									? sPCFilterExpr
				            									: "(" + sPCFilterExpr + ") OR (" + filterActualExpression + ")";
            	}
            }

            // pass " " to CFF for empty expressions
            if (UIUtil.isNullOrEmpty(filterActualExpression)) { filterActualExpression = " "; }

            // if both empty don't call getCompiledBinary
            if (!(ecoIdList.isEmpty() && " ".equals(filterActualExpression))) {
            	String queryMode = effectivity.QUERY_MODE_150;
	            Map filterbinaryMap = (Map)effectivity.getFilterCompiledBinary(context, filterActualExpression, ecoIdList, view,queryMode);
	            filterExpression = (String)filterbinaryMap.get("compiledBinary");
            }
            tempList = partObj.getRelatedObjects(context, sRelName,
            										sTypeName, selectStmts, selectRelStmts, false, true, (short)
            										nExpandLevel, null, sWhere, (short)0, DomainObject.CHECK_HIDDEN,
            										DomainObject.PREVENT_DUPLICATES, (short)DomainObject.PAGE_SIZE, null, null,
            										null, (String)null, filterExpression,DomainObject.FILTER_STRUCTURE);

            return tempList;
        }
        catch (FrameworkException Ex)
        {
            throw Ex;
        }
    }

    /**
     * @param context ematrix context
     * @param ecoIdList contains ecoIds selected by user to filter.
     * @return StringList with PreRequisite ECOs.
     * @throws Exception if any error occurs.
     */
    private StringList getPreRequisiteECOIds(Context context, StringList ecoIdList) throws Exception {
    	StringList sListReturn = new StringList();

    	MapList prerequisitesList;
    	DomainObject doECO;

    	String ecoId;

    	for (int i = 0, size = ecoIdList.size(); i < size; i++) {
    		ecoId = (String) ecoIdList.get(i);

	        doECO = DomainObject.newInstance(context, ecoId);

	        prerequisitesList = doECO.getRelatedObjects(context,
	        							 UnresolvedEBOMConstants.RELATIONSHIP_PREREQUISITE,
	        							 UnresolvedEBOMConstants.TYPE_PUE_ECO,
	                                     new StringList(SELECT_ID), null, false, true, (short)0, null, null);

	        for (int j = 0, jSize = prerequisitesList.size(); j < jSize; j++) {
	            sListReturn.addElement((String) ((Map) prerequisitesList.get(j)).get(SELECT_ID));
	        }

	        sListReturn.addElement(ecoId);
	    }

	    return sListReturn;
    }

    /**
     * Method to reformat MapList - add extra UEBOM columns
     * @param Map
     * @param String
     * @param String
     * @param String
     * @param String
     * @return Map
     */
    public Map reformatMap(Map map, String applicability,String effectivity, String add, String remove, String operation)
    {
        map.put("Applicability",applicability);
        map.put("Effectivity",effectivity);
        map.put("Add",add);
        map.put("Remove",remove);
        map.put("Operation",operation);

        /*// Do not allow edit for removed columns or resolved bom
        if (!"".equals(remove) || ("".equals(add)) && "".equals(remove)) {
            map.put("RowEditable", "readonly");
        }else{
            map.put("RowEditable", "show");
        }*/
        return map;
    }

    /**
     * Method to populate Change View filter
     * @param context
     * @param String[]
     * @return HashMap
     */
    public HashMap getChangeViewFilters(Context context, String args[]) throws Exception
    {
        HashMap mapReturn = new HashMap();
        StringList strFilterDisplayList = new StringList();
        StringList strFilterActualList = new StringList();
        String strChangeView = PropertyUtil.getAdminProperty(context,"person",context.getUser(),"preference_ChangeView");
        //String strChangeViewFilter = FrameworkProperties.getProperty("emxUnresolvedEBOM.BOMPowerView.ChangeView");
        String strChangeViewFilter = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOM.BOMPowerView.ChangeView");
        StringList strChangeViewList = FrameworkUtil.split(strChangeViewFilter,",");
        HashMap paraMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) paraMap.get("requestMap");
        Locale Local = context.getLocale();
        //2011x
        boolean copyFrom	= false;
        if ("CopyFrom".equals(requestMap.get("From"))) {
        	copyFrom 		= true;
        }
        //2011x
        String strChangeViewCurrent = (String) requestMap.get("PUEUEBOMChangeViewFilter");
        String strTemp = "";
        if(strChangeViewCurrent!=null && !"".equals(strChangeViewCurrent))
        {
           // strTemp = i18nNow.getI18nString("emxUnresolvedEBOM.BOMPowerView.ChangeView."+strChangeViewCurrent,"emxUnresolvedEBOMStringResource",context.getSession().getLanguage());
        	 strTemp = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource",Local,"emxUnresolvedEBOM.BOMPowerView.ChangeView."+strChangeViewCurrent);
            strFilterDisplayList.add(strTemp);
            strFilterActualList.add(strChangeViewCurrent);
            strChangeView = strChangeViewCurrent;
        }
        else if(strChangeView!=null && !"".equals(strChangeView))
        {
           // strTemp = i18nNow.getI18nString("emxUnresolvedEBOM.BOMPowerView.ChangeView."+strChangeView,"emxUnresolvedEBOMStringResource",context.getSession().getLanguage());
        	 strTemp = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource",Local,"emxUnresolvedEBOM.BOMPowerView.ChangeView."+strChangeView);
            strFilterDisplayList.add(strTemp);
            strFilterActualList.add(strChangeView);
        }
        for(int i=0;i<strChangeViewList.size();i++)
        {
            strChangeViewFilter = (String) strChangeViewList.get(i);
            //strTemp = i18nNow.getI18nString("emxUnresolvedEBOM.BOMPowerView.ChangeView."+strChangeViewFilter,"emxUnresolvedEBOMStringResource",context.getSession().getLanguage());
            strTemp = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource",Local,"emxUnresolvedEBOM.BOMPowerView.ChangeView."+strChangeViewFilter);
            if(!strChangeViewFilter.equals(strChangeView) && strChangeViewFilter!=null)
            {
            	//2011x
            	if (copyFrom) {
            		if (DomainConstants.SELECT_CURRENT.equalsIgnoreCase(strChangeViewFilter)) {
            			strFilterDisplayList.add(strTemp);
            			strFilterActualList.add(strChangeViewFilter);
            		}
            	} else
            	{
            		strFilterDisplayList.add(strTemp);
            		strFilterActualList.add(strChangeViewFilter);
            	}
        	}//2011x
        }
        mapReturn.put("field_choices", strFilterActualList);
        mapReturn.put("field_display_choices", strFilterDisplayList);
        return mapReturn;
    }

   /**
    * Method to check if part is a top level part.
    * @param context
    * @param String[]
    * @return boolean
    */
   public boolean checkForTopLevelPart(Context context,String objectID)
   throws Exception
   {
   boolean chkTopLevelPart = false;
   String  parentObjectId = objectID;
   DomainObject domObj = new DomainObject(parentObjectId);
   MapList mapList = domObj.getRelatedObjects(context,
           UnresolvedEBOMConstants.RELATIONSHIP_ASSIGNED_PART,
           UnresolvedEBOMConstants.TYPE_PRODUCTS,
                                              null,
                                              null,
                                              true,
                                              false,
                                              (short) 1,
                                              null,
                                              null);

   if(mapList.size()>0){
       chkTopLevelPart = true;
   }
   else{
       chkTopLevelPart = false;
   }
   return chkTopLevelPart;
}

   /**
    * Method to display table data for UEBOM columns
    * @param context
    * @param String[]
    * @return MapList
    */
    public Vector getUEBOMColumnValues(Context context,String args[])throws Exception
    {
         HashMap programMap = (HashMap)JPO.unpackArgs(args);
         MapList objList = (MapList)programMap.get("objectList");
         Map columnMap = (Map)programMap.get("columnMap");
         String columnName = (String)columnMap.get("name");
         Vector columnVals = new Vector(objList.size());
         Iterator itr = objList.iterator();
         while(itr.hasNext())
         {
             Map m=(Map)itr.next();
             if("Add".equals(columnName))
                 columnVals.add(m.get("Add"));
             else if("Remove".equals(columnName))
                 columnVals.add(m.get("Remove"));
             else if("Applicability".equals(columnName))
             {
                 String applicability = (String)m.get("Applicability");
                 if(applicability!=null)
                     applicability = UnresolvedPart.excludeNOTOperator(context,applicability);

                 columnVals.add(applicability);
             }
             else if("Effectivity".equals(columnName))
             {
                 String Effectivity = (String)m.get("Effectivity");
                 if(Effectivity!=null)
                     Effectivity = UnresolvedPart.excludeNOTOperator(context,Effectivity);
                 columnVals.add(m.get("Effectivity"));
             }
         }
         return columnVals;
    }

  /**
   * Returns a StringList of the parent object ids and select Object Id which are connected using UEBOM Relationship
   * for a given context.
   * @param context the eMatrix <code>Context</code> object.
   * @param args contains a packed HashMap containing objectId of object
   * @return StringList.
 * @since EngineeringCentral X3
   * @throws Exception if the operation fails.
  */
  @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
  public StringList excludeRecursiveOIDAddExisting(Context context, String args[])    throws Exception
  {
    HashMap programMap = (HashMap) JPO.unpackArgs(args);
      String selPartObjectId = (String) programMap.get("selPartObjectId");
      StringList result = new StringList();
      if (selPartObjectId == null) {
          return (result);
      }
      DomainObject domObj = new DomainObject(selPartObjectId);
      String strTypePart = PropertyUtil.getSchemaProperty(context,"type_Part");
      StringBuffer sbTypePattern = new StringBuffer(strTypePart);
      //Start for the IR-072947V6R2011x
      //String relToExpand = PropertyUtil.getSchemaProperty(context,"relationship_EBOMPending");
      StringBuffer sbRelPattern = new StringBuffer();
      sbRelPattern.append(DomainConstants.RELATIONSHIP_EBOM).append(',').append(UnresolvedEBOMConstants.RELATIONSHIP_EBOM_PENDING);
      //End for the IR-072947V6R2011x
      StringList selectStmts = new StringList(1);
      selectStmts.addElement(DomainConstants.SELECT_ID);
      MapList mapList = domObj.getRelatedObjects(context, sbRelPattern.toString(), // relationship pattern
              sbTypePattern.toString(), // object pattern
              selectStmts, // object selects
              null, // relationship selects
              true, // to direction
              false, // from direction
              (short) 0, // recursion level
              null, // object where clause
              null); // relationship where clause

      Iterator i1 = mapList.iterator();
      while (i1.hasNext()) {
          Map m1 = (Map) i1.next();
          String strId = (String) m1.get(DomainConstants.SELECT_ID);
          result.addElement(strId);
      }

      result.add(selPartObjectId);

      return result;
}


  /**
   * This method is check if the Object is Reserved
   * @param context the eMatrix <code>Context</code> object
   * @param strObjId : objectId to check for Reserved
   * @return boolean : return the boolean based on Reserve
   * @throws FrameworkException  if the operation fails
   * @author Praveen Voggu
   * @since X3 HF0.6
   */
 public static boolean isConnectionReserved(Context context, String strRelId) throws FrameworkException
 {
     String mqlCommand ="";
     String strReserved = "";
     if(strRelId != null){
     mqlCommand = "print connection $1 select $2";
     strReserved = MqlUtil.mqlCommand(context, mqlCommand,strRelId,"reserved");
     }

     boolean isConnReserved = false;

     if(strReserved.indexOf("TRUE") != -1)
     {
         isConnReserved = true;
     }
     return isConnReserved;

 }

 /**
      * @param context
      * @param args
      * @return true/false
 * @throws Exception
      * @throws Exception
      */
     public boolean sendMailForBGProcess(Context context,String objectId,boolean success) throws Exception{

         String loggedInUser = com.matrixone.apps.common.Person.getPerson(context).getName(context);
         String strSubject="";
         String strBody = "";
         StringList toList = new StringList(loggedInUser);
         Locale Local = context.getLocale();
         if(success){
        	 strSubject = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource",Local,"emxUnresolvedEBOM.Notification.Subject");
        	 strBody = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource",Local,"emxUnresolvedEBOM.Notification.Body");
         }
         else{
        	 strSubject = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOMStringResource",Local,"emxUnresolvedEBOM.FailNotification.Subject");
         }
         // Send notification
         MailUtil.sendMessage(context,
                            toList,//toList
                            null,//ccList
                            null,//bccList
                            strSubject,//subject
                            strBody,//message
                            new StringList(objectId));//objectIdList


         return true;
    }
 //Added for UEBOM Copy From operation-Ends

     /**
      * Returns a MapList of the ECO ids when ECO filter is selected
      * for a given context.
      * @param context the eMatrix <code>Context</code> object.
      * @param args contains a packed HashMap containing objectId of object
      * @return MapList.
      * @since  X+4.
      * @throws Exception if the operation fails.
     */
     public MapList getContextChangeForECO(Context context, String args[])throws Exception
     {
         MapList ecoList=new MapList();
         StringList finalECOs=new StringList();
         HashMap paramMap = (HashMap) JPO.unpackArgs(args);
         String  stringECOObjectId =(String)paramMap.get("cmdECOFilterInfo");
         finalECOs = FrameworkUtil.split(stringECOObjectId, ",");
         for(int i=1;i<=finalECOs.size();i++) {
            Map map = new HashMap();
            map.put("id",finalECOs.get(i-1));
            ecoList.add(map);
         }
         ecoList.add(0,new Integer(finalECOs.size()+1));
         return ecoList;
     }

     /**
     * Method to check if BOMMode is Resolved for Assign Top Level Part command.
     *
     * @param context
     * @param String[]
     * @return boolean
     */

    public boolean checkForBOMMode(Context context, String args[])
            throws Exception {
        return true;
    }

    /**
     * update UEBOM fields
     */
    public Boolean updatePUEFields(Context context, String[] args) throws Exception {
        return Boolean.TRUE;
    }
    //Added for X7 - Starts
    /**
    * Method to include parts which satisy the given condition
    * @param context
    * @param String[]
    * @return StringList
    */
    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList includeUnresolvedParts(Context context, String[] args) throws Exception {
        HashMap programMap 				= (HashMap) JPO.unpackArgs(args);
        String productId 				= (String) programMap.get("objectId");
        StringList slUnresolvedParts 	= new StringList();
        DomainObject doObject 			= DomainObject.newInstance(context);
        doObject.setId(productId);
        String physicalId				= doObject.getInfo(context, "physicalid");
        StringList slBusSelectList 		= new StringList(1);
        slBusSelectList.add(UnresolvedEBOMConstants.SELECT_ID);
        String STATE_CONPART_SUPERSEDED = PropertyUtil.getSchemaProperty(context,"policy",
                                           UnresolvedEBOMConstants.POLICY_CONFIGURED_PART,
                                           "state_Superseded");
        String sVault = context.getVault().toString();
        StringBuffer sbWhere = new StringBuffer(128);
        sbWhere.append('(').append("policy == \"").append(UnresolvedEBOMConstants.POLICY_CONFIGURED_PART)
            .append("\" && ").append(DomainConstants.SELECT_REVISION).append(" == \"").append("last\"")
            .append(" && relationship[").append(UnresolvedEBOMConstants.RELATIONSHIP_ASSIGNED_PART)
            .append("] == \"False\" && ").append("to[").append(UnresolvedEBOMConstants.RELATIONSHIP_EBOM_PENDING)
            .append("] == \"False\" && ").append("to[").append(UnresolvedEBOMConstants.RELATIONSHIP_EBOM)
            .append("] == \"False\" && ").append(UnresolvedEBOMConstants.SELECT_CURRENT).append(" != \"")
            .append(STATE_CONPART_SUPERSEDED).append("\")");
        MapList mlUnresolvedParts = DomainObject.findObjects(context, UnresolvedEBOMConstants.TYPE_PART,
                                        						sVault, sbWhere.toString(), slBusSelectList);
        if (mlUnresolvedParts != null && !mlUnresolvedParts.isEmpty()) {
            Iterator itrUnresolvedParts = mlUnresolvedParts.iterator();
            while (itrUnresolvedParts.hasNext()) {
                Map mapParts = (Map) itrUnresolvedParts.next();
                String sUnresolvedPartId = (String) mapParts.get(DomainConstants.SELECT_ID);
                doObject.setId(sUnresolvedPartId);
                StringList slConnectionIds	=	doObject.getInfoList(context, "from["+DomainConstants.RELATIONSHIP_EBOM+"].id");
                slConnectionIds.addAll(doObject.getInfoList(context, "from["+UnresolvedEBOMConstants.RELATIONSHIP_EBOM_PENDING+"].id"));
                if (slConnectionIds != null && !slConnectionIds.isEmpty()) {
                	Iterator itrRelIds		=	slConnectionIds.iterator();
                	while (itrRelIds.hasNext()) {
                		String sRelId				= (String) itrRelIds.next();
                		StringList slProductList 	= effectivity.getRelEffectivityUsage(context, sRelId);
                		if (slProductList.contains(physicalId) && slProductList.size() == 1) {
                			slUnresolvedParts.add(sUnresolvedPartId);
                		}
                	}
            	} else {
            		slUnresolvedParts.add(sUnresolvedPartId);
            	}

            }
        }
        return slUnresolvedParts;
    }

/**
 * This method retrieves values for Initial Release, Add, Remove Columns in unresolved part bom powerview
 * @param context the eMatrix code context object
 * @param args packed hashMap of request parameters
 * @return StringList of column values
 * @throws Exception if the operation fails
 */
    public StringList getUEBOMAddorRemoveColumnValues(Context context,String args[]) throws Exception {
    	HashMap programMap 			= (HashMap)JPO.unpackArgs(args);
        MapList objList 			= (MapList)programMap.get("objectList");
        Map columnMap 				= (Map)programMap.get("columnMap");
        String columnName 			= (String)columnMap.get("name");
        StringList columnVals 		= null;
        int index					= 0;
        String objIds[]             = null;
        Iterator itrObj 			= objList.iterator();
        String delimiter 			= matrix.db.SelectConstants.cSelectDelimiter;
        String strAttrAffectedItemCategory = PropertyUtil.getSchemaProperty(context,"attribute_AffectedItemCategory");

        StringBuffer colBuffer;

        String ecoNameECSelectable	= "tomid["+UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE+"].from.name";
        String ecoNameECHSelectable	= "tomid["+UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE_HISTORY+"].from.name";


         if ("InitialRelease".equalsIgnoreCase(columnName)){
        	objIds 				   = new String[objList.size()];
			 while (itrObj.hasNext()) {
		     	Map mapObj   = (Map) itrObj.next();
		     	String objId = (String) mapObj.get(DomainConstants.SELECT_ID);
		     	if (objId != null && objId.length() > 0)
		     		objIds[index++]	= objId;
		     }
			 String ecoName = "";
			 if (index > 0){
				 	columnVals 				 = new StringList();
					String  select      	 = "to[" + RELATIONSHIP_AFFECTED_ITEM + "].from["+UnresolvedEBOMConstants.TYPE_PUE_ECO+"]";
					String  selectCCA      	 = "to[" + RELATIONSHIP_AFFECTED_ITEM + "].from["+ChangeConstants.TYPE_CCA+"]";
				 	String  ecoSelect        = "to[" + RELATIONSHIP_AFFECTED_ITEM + "|attribute[" + strAttrAffectedItemCategory + "] == Indirect].from["+UnresolvedEBOMConstants.TYPE_PUE_ECO+"]";
				    StringList slObjSelect 	 = new StringList(ecoSelect);
		        	MapList mlColumnValues   = DomainObject.getInfo(context, objIds, slObjSelect);
		        	Iterator itrColumnValues = mlColumnValues.iterator();
		        	while (itrColumnValues.hasNext()) {
		        		StringList ecoList 	=  new StringList();
		        		Map mapColumn       = (Map) itrColumnValues.next();
		        		String changeName = (String) mapColumn.get(select);

		        		if (UIUtil.isNullOrEmpty(changeName)) {
		        			changeName = (String) mapColumn.get(selectCCA);
		        		}
		        		ecoList.addAll(FrameworkUtil.split(changeName, delimiter));
		        		ecoName = ecoList.size() > 0?(String)ecoList.get(0):"";
		        		ecoName = ecoName != null && !"null".equalsIgnoreCase(ecoName)?ecoName:"";
		        		columnVals.add(ecoName);
		        	}
			  }
			 return columnVals;
        }
         else if("Add".equalsIgnoreCase(columnName)) {
 	        String relIds[]		= new String[objList.size()];
	        String ecoNameEC	= "tomid["+UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE+"|"+UnresolvedEBOMConstants.SELECT_ATTRIBUTE_BOM_OPERATION+ "== Add].from.name";
	        String ecoNameECH	= "tomid["+UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE_HISTORY+"|"+UnresolvedEBOMConstants.SELECT_ATTRIBUTE_BOM_OPERATION+ "== Add].from.name";


	        StringList slRelSelect 		= new StringList(2);
	        slRelSelect.add(ecoNameEC);
	        slRelSelect.add(ecoNameECH);

	        while (itrObj.hasNext()) {
	        	Map mapObj = (Map) itrObj.next();
	        	String relId = (String) mapObj.get(DomainConstants.SELECT_RELATIONSHIP_ID);
	        	if (relId != null && relId.length() > 0)
	        		relIds[index++]	= relId;
	        }

	        if (index > 0) {
	        	columnVals 			   = new StringList();
	        	MapList mlColumnValues = DomainRelationship.getInfo(context, relIds, slRelSelect);
	        	Iterator itrColumnValues = mlColumnValues.iterator();
	        	while (itrColumnValues.hasNext()) {
	        		Map mapColumn = (Map) itrColumnValues.next();
	        		String ecValues = (String) mapColumn.get(ecoNameECSelectable);
	        		String echValues 	= (String) mapColumn.get(ecoNameECHSelectable);

	        		if (isNullOrEmpty(ecValues) && isNullOrEmpty(echValues)) {
	        			columnVals.add("");
	        		} else {
		        		if(!isNullOrEmpty(ecValues)) {
		        			columnVals.add(ecValues);
		        		}
		        		if(!isNullOrEmpty(echValues)) {
		        			columnVals.add(echValues);
		        		}
	        		}
	        	}
	        }
         }
	        else {

	 	        String relIds[]		= new String[objList.size()];
		        String ecoNameEC	= "tomid["+UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE+"|"+UnresolvedEBOMConstants.SELECT_ATTRIBUTE_BOM_OPERATION+ "== Remove].from.name";
		        String ecoNameECH	= "tomid["+UnresolvedEBOMConstants.RELATIONSHIP_EBOM_CHANGE_HISTORY+"|"+UnresolvedEBOMConstants.SELECT_ATTRIBUTE_BOM_OPERATION+ "== Remove].from.name";

		        StringList slRelSelect 		= new StringList(2);
		        slRelSelect.add(ecoNameEC);
		        slRelSelect.add(ecoNameECH);

		        while (itrObj.hasNext()) {
		        	Map mapObj = (Map) itrObj.next();
		        	String relId = (String) mapObj.get(DomainConstants.SELECT_RELATIONSHIP_ID);
		        	if (relId != null && relId.length() > 0)
		        		relIds[index++]	= relId;
		        }


	        if (index > 0) {
	        	columnVals 			   = new StringList();
	        	MapList mlColumnValues = DomainRelationship.getInfo(context, relIds, slRelSelect);
	        	Iterator itrColumnValues = mlColumnValues.iterator();
	        	while (itrColumnValues.hasNext()) {
		        	colBuffer = new StringBuffer();
	        		Map mapColumn = (Map) itrColumnValues.next();
	        		Object ecValues 	= (Object) mapColumn.get(ecoNameECSelectable);
	        		Object echValues 	= (Object) mapColumn.get(ecoNameECHSelectable);

	        		if(ecValues != null) {
	        			getColumnStringFromObject(ecValues,colBuffer);
	        		}
	        		if(echValues != null) {
	        			getColumnStringFromObject(echValues,colBuffer);
	        		}
	        		columnVals.addElement(colBuffer.toString());
	        		}
	        	}
	        }
    	return columnVals;
    }

/**
 * This Method prepares a StringBuffer with values retrieved from object
 * @param val Object where values gets retrieved from
 * @param colBuffer prepared StringBuffer for a particular column
 */
private void getColumnStringFromObject(Object val, StringBuffer colBuffer) {

	if (val instanceof String) {
		colBuffer = colBuffer.length() > 0 ? colBuffer.append(", ").append(val.toString()) : colBuffer.append(val.toString());
	}
	else {
		StringList valList = (StringList)val;
		for (int i= 0;i<valList.size();i++) {
			colBuffer = colBuffer.length() > 0 ? colBuffer.append(", ").append((String)valList.get(i)) : colBuffer.append((String)valList.get(i));
		}
	}
  }
/**
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds a packed hashmap with the following arguments
     *        paramMap HashMap of parameter values - fieldMap
     *        requestMap HashMap of request values - objectId, mode, form
     *
     * @throws Exception if the operation fails
     * @since CFF R210 for ECC Reports
     */
   public String getEffectivityExpressionDisplayBOM1(Context context, String[] args)
        throws Exception

   {
	   HashMap programMap = (HashMap)JPO.unpackArgs(args);
	   HashMap requestMap= (HashMap)programMap.get("requestMap");
	   String objectId = (String)requestMap.get("objectId");
	   double timezone = (new Double((String)requestMap.get("timeZone"))).doubleValue();
	   String mode = (String)requestMap.get("mode");
	   String formName = (String)requestMap.get("form");
	   EffectivityFramework EFF = new EffectivityFramework();
	   StringBuffer sb = new StringBuffer(100);
	   String actualValue = "";
	   String displayValue = "";
	   String effTypes1 = "";
	   Map mapExpression = null;
	   //TODO remove temp code of hardcoded form name - should be passed in by BPS edit form
	   formName = "editDataForm";
	   StringList listValue = new StringList();
	   StringList listValueActual = new StringList();
	   StringBuffer sbListValue = new StringBuffer(32);

	   MapList mlObjectExpression = EFF.getObjectExpression(context, objectId, timezone, true);
	   mapExpression = (Map)mlObjectExpression.get(0);
	   actualValue = (String)mapExpression.get(EffectivityFramework.ACTUAL_VALUE);
	   displayValue = (String)mapExpression.get(EffectivityFramework.DISPLAY_VALUE);
	   listValue = (StringList)mapExpression.get("listValue");
	   for(int i=0;i<listValue.size();i++)
	   {
	     sbListValue.append(listValue.get(i));
	     sbListValue.append("@delimitter@");
	   }
	   String strListValue = sbListValue.toString();
	   sbListValue.delete(0, sbListValue.length());
	   listValueActual = (StringList)mapExpression.get("listValueActual");
	   for(int i=0;i<listValueActual.size();i++)
	   {
	     sbListValue.append(listValueActual.get(i));
	     sbListValue.append("@delimitter@");
	   }
	   String quoteSeparatedIds = strListValue.substring(0, strListValue.length());
	   String strListValueAc = sbListValue.toString();
	   String quoteSeparatedIdsAc = strListValueAc.substring(0, strListValueAc.length());
	   HashMap effectivityFrameworkMap = new HashMap();
	   HashMap effTypes = new HashMap();
	   effTypes.put(EffectivityFramework.DISPLAY_VALUE, displayValue);
	   effTypes.put(EffectivityFramework.ACTUAL_VALUE, actualValue);
	   effectivityFrameworkMap.put("effTypes", effTypes);
	   HashMap effExpr = new HashMap();
	   effExpr.put(EffectivityFramework.DISPLAY_VALUE, displayValue);
	   effExpr.put(EffectivityFramework.ACTUAL_VALUE, actualValue);
	   effectivityFrameworkMap.put("effExpr", effExpr);

	    String editEffectivityURL = "../effectivity/EffectivityDefinitionDialog.jsp?modetype=filter&invockedFrom=fromForm&formName="+formName+"&parentOID=&" +
		"fieldNameEffExprDisplay=EffectivityExpression&" +
		"fieldNameEffExprActual=EffectivityExpressionActual&" +
		"fieldNameEffExprActualList=EffectivityExpressionOIDList&" +
		"fieldNameEffExprActualListAc=EffectivityExpressionOIDListAc&" +
		"fieldNameEffExprOID=EffectivityExpressionOID";
	    editEffectivityURL+="&objectId=";
	    editEffectivityURL+=objectId;

	    if(UIUtil.isNullOrEmpty(objectId)) {
		    effTypes1 = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgt.Effectivity.EffectivityTypes");
	    	editEffectivityURL+="&fieldNameEffTypes=effTypes1";
	    }
	    DomainObject doPart;
	   if (mode != null && mode.equalsIgnoreCase("edit"))
	   {
		   String policyClassification = "policy.property[PolicyClassification].value";
		   String SELECT_PART_POLICYCLASS = "from["+EngineeringConstants.RELATIONSHIP_PART_REVISION+"].to.policy.property[PolicyClassification].value";

		   StringList objectSelect = new StringList(3);
		   objectSelect.addElement(policyClassification);
		   objectSelect.addElement(DomainConstants.SELECT_TYPE);
		   objectSelect.addElement(SELECT_PART_POLICYCLASS);

		   if(!UIUtil.isNullOrEmpty(objectId)) {
		   doPart = new DomainObject(objectId);
		   Map dataMap = doPart.getInfo(context, objectSelect);
		   String type = (String) dataMap.get(DomainConstants.SELECT_TYPE);
		   if( EngineeringConstants.TYPE_PART_MASTER.equals(type))
		   {
			   policyClassification = (String) dataMap.get(SELECT_PART_POLICYCLASS);
		   }
		   else
		   {
		       policyClassification = (String) dataMap.get(policyClassification);
		   }
		   }
	       sb.append(" <script src='../common/scripts/emxUIModal.js'> </script> ");
	       sb.append(" <script src='../emxUIPageUtility.js'> </script> ");
	       sb.append("<script> ");
	       sb.append("function showEffectivityExpressionDialog() { ");
	       sb.append(" emxShowModalDialog(\"");
	       sb.append(XSSUtil.encodeForJavaScript(context, editEffectivityURL));
	       sb.append("\",700,500);");
	       sb.append('}');
	       sb.append("</script>");
			   if (policyClassification.equals("Unresolved") || null == objectId || "null".equals(objectId)) {
				   sb.append("<div id=\"editeffectivity1\"style=\"visibility: visible;\">");
			   } else {
				   sb.append("<div id=\"editeffectivity1\" style=\"visibility: hidden;\">");
			   }
	       sb.append("<input type=\"text\" name=\"EffectivityExpression\" size=\"20");
	       sb.append("\" readonly=\"readonly\" >");
	       sb.append(XSSUtil.encodeForHTML(context, displayValue));
	       sb.append("</textarea>");
	       sb.append("<a href=\"javascript:showEffectivityExpressionDialog()\">");
	       sb.append("<img src=\"../common/images/iconActionEdit.gif\" border=\"0\"/></a>");
	       sb.append("&nbsp<a href=\"javascript:basicClear('EffectivityExpression');basicClear('EffectivityExpressionActual');basicClear('EffectivityExpressionOIDList');basicClear('EffectivityExpressionOIDListAc');basicClear('EffectivityExpressionOID') \">").append(EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.Common.Clear", context.getSession().getLanguage())).append("</a>");
	       		sb.append("</div>");
	       sb.append("<input type=\"hidden\" name=\"EffectivityExpressionActual\" value=\"");
	       sb.append(XSSUtil.encodeForHTMLAttribute(context,actualValue));
	       sb.append("\" />");
	       sb.append("<input type=\"hidden\" name=\"EffectivityExpressionOIDList\" value=\"");
	       sb.append(XSSUtil.encodeForHTMLAttribute(context,quoteSeparatedIds));
	       sb.append("\" />");
	       sb.append("<input type=\"hidden\" name=\"EffectivityExpressionOIDListAc\" value=\"");
	       sb.append(XSSUtil.encodeForHTMLAttribute(context,quoteSeparatedIdsAc));
	       sb.append("\" />");
	       sb.append("<input type=\"hidden\" name=\"EffectivityExpressionOID\" value=\"");
	       sb.append(XSSUtil.encodeForHTMLAttribute(context,actualValue));
	       sb.append("\" />");
	      if(UIUtil.isNullOrEmpty(objectId)) {
	    	Map effType = null;
	  		MapList mlEffectivityTypes = EFF.getEffectivityTypeData(context, effTypes1);
	      if (!mlEffectivityTypes.isEmpty())
	       {
		   for (int i=0; i < mlEffectivityTypes.size(); i++)
		   {
		       effType = (Map)mlEffectivityTypes.get(i);
		       actualValue = (String)effType.get(EffectivityFramework.ACTUAL_VALUE);
		       displayValue = (String)effType.get(EffectivityFramework.DISPLAY_VALUE);
		       sb.append("<input type=\"checkbox\" name=\"effTypes1\" style=\"display:none;\" checked=\"checked\" value=\"" );
		       sb.append(XSSUtil.encodeForHTMLAttribute(context, actualValue));
		       sb.append("@displayactual@");
		       //XSS OK
		       sb.append(displayValue);
		       sb.append("\"/>");
		   }
	       }
	       }
	   }
	   else //view mode only display expression
	   {
	       sb.append(XSSUtil.encodeForHTML(context,displayValue));
	   }

	   return sb.toString();
	}

        /**
	     *
	     * @param context the eMatrix <code>Context</code> object
	     * @param args holds a packed hashmap with the following arguments
	     *        paramMap HashMap of parameter values - fieldMap
	     *        requestMap HashMap of request values - objectId, mode, form
	     *
	     * @throws Exception if the operation fails
             * @since CFF R210 for ECC Reports
	     */
	    public String getEffectivityExpressionDisplayBOM2(Context context, String[] args)
	        throws Exception
	    {
	    	 HashMap programMap = (HashMap)JPO.unpackArgs(args);
	  	   HashMap requestMap= (HashMap)programMap.get("requestMap");
	  	   String objectId = (String)requestMap.get("objectId2");
	  	   double timezone = (new Double((String)requestMap.get("timeZone"))).doubleValue();
	  	   String mode = (String)requestMap.get("mode");
	  	   String formName = (String)requestMap.get("form");
	  	   EffectivityFramework EFF = new EffectivityFramework();
	  	   StringBuffer sb = new StringBuffer(128);
	  	   String actualValue = "";
	  	   String displayValue = "";
	  	   String effTypes2 = "";
	  	   Map mapExpression = null;
	  	   //TODO remove temp code of hardcoded form name - should be passed in by BPS edit form
	  	   formName = "editDataForm";
	  	   StringList listValue = new StringList();
	  	   StringList listValueActual = new StringList();
	  	   StringBuffer sbListValue = new StringBuffer(128);

	  	   MapList mlObjectExpression = EFF.getObjectExpression(context, objectId, timezone, true);
	  	   mapExpression = (Map)mlObjectExpression.get(0);
	  	   actualValue = (String)mapExpression.get(EffectivityFramework.ACTUAL_VALUE);
	  	   displayValue = (String)mapExpression.get(EffectivityFramework.DISPLAY_VALUE);
	  	   listValue = (StringList)mapExpression.get("listValue");
	  	   for(int i=0;i<listValue.size();i++)
	  	   {
	  	     sbListValue.append(listValue.get(i));
	  	     sbListValue.append("@delimitter@");
	  	   }
	  	   String strListValue = sbListValue.toString();
	  	   sbListValue.delete(0, sbListValue.length());
	  	   listValueActual = (StringList)mapExpression.get("listValueActual");
	  	   for(int i=0;i<listValueActual.size();i++)
	  	   {
	  	     sbListValue.append(listValueActual.get(i));
	  	     sbListValue.append("@delimitter@");
	  	   }
	  	   String quoteSeparatedIds = strListValue.substring(0, strListValue.length());
	  	   String strListValueAc = sbListValue.toString();
	  	   String quoteSeparatedIdsAc = strListValueAc.substring(0, strListValueAc.length());
	  	   HashMap effectivityFrameworkMap = new HashMap();
	  	   HashMap effTypes = new HashMap();
	  	   effTypes.put(EffectivityFramework.DISPLAY_VALUE, displayValue);
	  	   effTypes.put(EffectivityFramework.ACTUAL_VALUE, actualValue);
	  	   effectivityFrameworkMap.put("effTypes", effTypes);
	  	   HashMap effExpr = new HashMap();
	  	   effExpr.put(EffectivityFramework.DISPLAY_VALUE, displayValue);
	  	   effExpr.put(EffectivityFramework.ACTUAL_VALUE, actualValue);
	  	   effectivityFrameworkMap.put("effExpr", effExpr);

	  	   String editEffectivityURL2 = "../effectivity/EffectivityDefinitionDialog.jsp?modetype=filter&invockedFrom=fromForm&formName="+formName+"&parentOID=&" +
	   		"fieldNameEffExprDisplay=EffectivityExpression1&" +
	     		"fieldNameEffExprActual=EffectivityExpressionActual1&" +
	     		"fieldNameEffExprActualList=EffectivityExpressionOIDList1&" +
	     		"fieldNameEffExprActualListAc=EffectivityExpressionOIDListAc1&" +
	    		"fieldNameEffExprOID=EffectivityExpressionOID1";
	  	    editEffectivityURL2+="&objectId=";
	  	    editEffectivityURL2+= (UIUtil.isNullOrEmpty(objectId) ? (String) requestMap.get("objectId") : objectId);

	  	    if(UIUtil.isNullOrEmpty(objectId)) {
			    effTypes2 = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgt.Effectivity.EffectivityTypes");
		    	editEffectivityURL2+="&fieldNameEffTypes=effTypes2";
		    }
	  	   if (mode != null && mode.equalsIgnoreCase("edit"))
	  	   {

	  		   String policyClassification = "policy.property[PolicyClassification].value";
	  	       String SELECT_PART_POLICYCLASS = "from["+EngineeringConstants.RELATIONSHIP_PART_REVISION+"].to.policy.property[PolicyClassification].value";

	  	       StringList objectSelect = new StringList(3);
	  	       objectSelect.addElement(policyClassification);
	  	       objectSelect.addElement(DomainConstants.SELECT_TYPE);
	  	       objectSelect.addElement(SELECT_PART_POLICYCLASS);

	  		   if(!UIUtil.isNullOrEmpty(objectId)) {
	  			 DomainObject doPart = new DomainObject(objectId);
	  		   Map dataMap = doPart.getInfo(context, objectSelect);
	  		   String type = (String) dataMap.get(DomainConstants.SELECT_TYPE);
	  		   if( EngineeringConstants.TYPE_PART_MASTER.equals(type))
	  		   {
	  			   policyClassification = (String) dataMap.get(SELECT_PART_POLICYCLASS);
	  		   }
	  		   else
	  		   {
	  		       policyClassification = (String) dataMap.get(policyClassification);
	  		   }
	  		   }
	  	       sb.append(" <script src='../common/scripts/emxUIModal.js'> </script> ");
	  	       sb.append(" <script src='../emxUIPageUtility.js'> </script> ");
	  	       sb.append("<script> ");
	  	       sb.append("function showEffectivityExpressionDialog2() { ");
	  	       sb.append(" emxShowModalDialog(\"");
	  	       sb.append(XSSUtil.encodeForJavaScript(context, editEffectivityURL2));
	  	       sb.append("\",700,500);");
	  	       sb.append('}');
	  	       sb.append("</script>");
	  		   if (policyClassification.equals("Unresolved") || null == objectId || "null".equals(objectId)) {
	  			   sb.append("<div id=\"editeffectivity\"style=\"visibility: visible;\">");
	  		   } else {
	  			   sb.append("<div id=\"editeffectivity\" style=\"visibility: hidden;\">");
	  		   }
	  			   sb.append("<input type=\"text\" name=\"EffectivityExpression1\" size=\"20");
	  			   sb.append("\" readonly=\"readonly\" >");
	  			   sb.append(XSSUtil.encodeForHTML(context, displayValue));
	  			   sb.append("</textarea>");

	  			   sb.append("<a href=\"javascript:showEffectivityExpressionDialog2()\">");
	  			   sb.append("<img src=\"../common/images/iconActionEdit.gif\"  border=\"0\"/></a>");
	  			   sb.append("&nbsp<a href=\"javascript:basicClear('EffectivityExpression1');basicClear('EffectivityExpressionActual1');basicClear('EffectivityExpressionOIDList1');basicClear('EffectivityExpressionOIDListAc1');basicClear('EffectivityExpressionOID1') \">").append(EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.Common.Clear", context.getSession().getLanguage())).append("</a>");
	  			   sb.append("</div>");
	  		   sb.append("<input type=\"hidden\" name=\"EffectivityExpressionActual1\" value=\"");
	  		   sb.append(XSSUtil.encodeForHTMLAttribute(context,actualValue));
	  	       sb.append("\" />");
	  	       sb.append("<input type=\"hidden\" name=\"EffectivityExpressionOIDList1\" value=\"");
	  	       sb.append(XSSUtil.encodeForHTMLAttribute(context,quoteSeparatedIds));
	  	       sb.append("\" />");
	  	       sb.append("<input type=\"hidden\" name=\"EffectivityExpressionOIDListAc1\" value=\"");
	  	       sb.append(XSSUtil.encodeForHTMLAttribute(context,quoteSeparatedIdsAc));
	  	       sb.append("\" />");
	  	       sb.append("<input type=\"hidden\" name=\"EffectivityExpressionOID1\" value=\"");
	  	       sb.append(XSSUtil.encodeForHTMLAttribute(context,actualValue));
	  	       sb.append("\" />");
	  	       /*if(UIUtil.isNullOrEmpty(objectId)) {
	  	    	Map effType = null;
	 			MapList mlEffectivityTypes = EFF.getEffectivityTypeData(context, effTypes2);
	  	       if (!mlEffectivityTypes.isEmpty())
		       {
			   for (int i=0; i < mlEffectivityTypes.size(); i++)
			   {
			       effType = (Map)mlEffectivityTypes.get(i);
			       actualValue = (String)effType.get(EffectivityFramework.ACTUAL_VALUE);
			       displayValue = (String)effType.get(EffectivityFramework.DISPLAY_VALUE);
			       sb.append("<input type=\"checkbox\" name=\"effTypes2\" style=\"display:none;\" checked=\"checked\" value=\"" );
			       sb.append(XSSUtil.encodeForHTMLAttribute(context, actualValue));
			       sb.append("@displayactual@");
			       //XSS OK
			       sb.append(displayValue);
			       sb.append("\"/>");
			   }
		       }
	  	       }*/
	  	   }
	  	   else //view mode only display expression
	  	   {
	  	       sb.append(XSSUtil.encodeForHTML(context, displayValue));
	  	   }

	  	   return sb.toString();
}

   /**
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds a packed hashmap with the following arguments
     *        paramMap String of parameter values - args
     *
     * @throws Exception if the operation fails
     * @since CFF R210 for ECC Reports
     */

    public Vector getProposedEffectivity(Context context, String[]args)
        throws Exception
    {
        HashMap programMap = (HashMap)JPO.unpackArgs(args);
	MapList objectList = (MapList)programMap.get("objectList");
	HashMap params = (HashMap)programMap.get("paramList");
	double timezone = (new Double((String)params.get("timeZone"))).doubleValue();
        Vector exprVector = new Vector(objectList.size());

	EffectivityFramework ef = new EffectivityFramework();
	String displayValue = "";

	MapList expressionMap =  ef.getRelExpression(context, objectList, timezone, true);
	for (int idx = 0; idx < expressionMap.size(); idx++)
	{
	    Map exprMap = (Map)expressionMap.get(idx);
	    displayValue = (String)exprMap.get(EffectivityFramework.DISPLAY_VALUE);
            exprVector.addElement(displayValue);
	}
	return exprVector;
    }

//2011x - Ends
    /**
     *
     * Method to Create WIP BOM.
     * @author YOQ
     * @param context the eMatrix code context object
     * @param parentPartId the parent part object id
     * @param childPartId the child part object id.
     * @param relAttributes hashMap of ebom relationship attributes
     * @param strExpression the effectivity expression to be applied on the EBOM relid.
     * @param operation the operation to be called with CFF api.
     * @throws Exception if the operation fails
     * @return StringList of true and false values for each column.
     *
     */
    private String createWIPBOM(Context context,String parentPartId, String childPartId,HashMap relAttributes, String strExpression,String operation) throws Exception
	{
	 DomainRelationship dr 	= null;
	 	try
	 	{
	 		ContextUtil.startTransaction(context, true);
	 		ContextUtil.pushContext(context);
	 		//connect the parent part and child part
	 		dr = DomainRelationship.connect(context, new DomainObject(parentPartId), UnresolvedEBOMConstants.RELATIONSHIP_EBOM, new DomainObject(childPartId));
	 		String connectionId = dr.getName();
	 		dr.setAttributeValues(context, (Map) relAttributes);
	 		effectivity.updateRelExpression(context, connectionId, strExpression);
		}
	 	catch (Exception exp) {
    		ContextUtil.abortTransaction(context);
    		throw exp;
    	}
	 	finally {
	 		ContextUtil.popContext(context);
	 	}
	 	return dr.toString();
    }
    /**
     * For rendering and displaying the Part mode field in the part create page
     * @param context the eMatrix <code>Context</code> object
     * @param args holds a packed hashmap with the following arguments
     *        paramMap String of parameter values - args
     *
     * @throws Exception if the operation fails
     * @since R211 for Part Creation Page
     */
	public HashMap getPartMode(Context context, String[] args) throws Exception
	{
	  HashMap programMap = (HashMap) JPO.unpackArgs(args);
	  HashMap requestMap = (HashMap) programMap.get ("requestMap");
	  String createMode = (String)requestMap.get("createMode");
	  String objectId = (String) requestMap.get("objectId");
	  HashMap hmPartModeMap = new HashMap();
	  StringList display 	= new StringList();
	  StringList actualVal	= new StringList();
	  Locale Local = context.getLocale();

	  String strUnConfigured = EnoviaResourceBundle.getProperty(context, "emxUnresolvedEBOMStringResource",Local,"emxUnresolvedEBOM.Common.Resolved");
	  String strConfigured = EnoviaResourceBundle.getProperty(context, "emxUnresolvedEBOMStringResource",Local,"emxUnresolvedEBOM.Common.Unresolved");


	  if ("EBOM".equals(createMode) || "EBOMReplaceNew".equals(createMode)){
		  display.addElement(strUnConfigured);
		  actualVal.addElement("Resolved");
	  }
	  else if ("assignTopLevelPart".equals(createMode)){
		  display.addElement(strConfigured);
		  actualVal.addElement("Unresolved");
	  }
	  else {
		// IR-082946V6R2012 : Start
		  String strDefaultPartPolicy = null;
		  if (objectId != null && !"".equals(objectId)) {
			  DomainObject domObj = DomainObject.newInstance(context, objectId);
			  String SELECT_ATTRIBUTE_DEFAULT_PART_POLICY = "attribute[" + PropertyUtil.getSchemaProperty(context,"attribute_DefaultPartPolicy") + "]";
			  strDefaultPartPolicy = domObj.getInfo(context, SELECT_ATTRIBUTE_DEFAULT_PART_POLICY);
		  }

		  if ("policy_ConfiguredPart".equals(strDefaultPartPolicy)) {
			  display.addElement(strConfigured);
			  actualVal.addElement("Unresolved");
			  display.addElement(strUnConfigured);
			  actualVal.addElement("Resolved");
		  } else {
			  display.addElement(strUnConfigured);
			  actualVal.addElement("Resolved");
			  display.addElement(strConfigured);
			  actualVal.addElement("Unresolved");
		  }
			// IR-082946V6R2012 : End
	  }
	  hmPartModeMap.put("field_choices",actualVal);
	  hmPartModeMap.put("field_display_choices",display);

	return hmPartModeMap;
	}
    /**
     *
     * Method to display the Current Effectivity column editable or not based on the parent state.
     * @author YOQ
     * @param context the eMatrix code context object
     * @param String[] packed hashMap of request parameters
     * @throws Exception if the operation fails
     * @return StringList of true and false values for each column.
     *
     */
   public static StringList isCurrentEffectivityEditable(Context context,String []args) throws Exception
    {
    	StringList columnVals  = new StringList();
    	StringList parentList  = new StringList();
    	MapList    stateList   = null;
    	String  []parentIds    = null;

    	HashMap programMap     = (HashMap)JPO.unpackArgs(args);
    	MapList objectList     = (MapList)programMap.get("objectList");
    	StringList slObjSelect = new StringList(3);
    	String currSelect      = DomainConstants.SELECT_CURRENT;
    	String objSelect       = DomainConstants.SELECT_ID;
    	// Physicalid selectable added for IR-286015
    	String phySelect       = "physicalid";
    	slObjSelect.addElement(currSelect);
    	slObjSelect.addElement(objSelect);
    	slObjSelect.addElement(phySelect);

    	String  objParentState = "";
    	int index			   = 0;
    	String parentId        = "";

    	Map<String,String> 	   objMap    = null;
    	int objListSize = objectList.size();

    	String isWipBomAllowed  = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOM.WIPBOM.Allowed");
    	if ("false".equalsIgnoreCase(isWipBomAllowed)) {
    		for (int i=0;i<objectList.size();i++){
    			columnVals.addElement(false);
    		}
    	}


		for (;index<objListSize;index++){
		objMap  = (Map)objectList.get(index);
		parentId = objMap.get("id[parent]") != null && !"null".equals(objMap.get("id[parent]"))?objMap.get("id[parent]"):objMap.get("id");
		    if (!parentList.contains(parentId)){
		    	parentList.add(parentId);
		    }
		    //in order to identify lookup add existing use case for release/preliminary ,add below if case.
		    if (objMap.size() < 5){
		    	columnVals.addElement(true);
		    }
		}
		if (columnVals.size() > 0) {
			return columnVals;
		}
		if (parentList.size() > 0){
			parentIds = (String[])parentList.toArray(new String[parentList.size()]);
			stateList = (MapList)DomainObject.getInfo(context, parentIds,slObjSelect);

			for (int i=0;i<objectList.size();i++){
			objMap  = (Map)objectList.get(i);
			parentId = objMap.get("id[parent]") != null && !"null".equals(objMap.get("id[parent]"))?objMap.get("id[parent]"):objMap.get("id");
			Iterator itrStateValues = stateList.iterator();

				while (itrStateValues.hasNext()) {
	        		Map mapState   = (Map) itrStateValues.next();
	        		if (mapState.containsValue(parentId)){
	        			objParentState = (String)mapState.get(currSelect);
	        		}else{
	        			continue;
	        		}
			    		if (!"".equals(objParentState) && "Preliminary".equalsIgnoreCase(objParentState)){
			  			    columnVals.addElement(true);
			  			    break;
			    		}else{
			    			columnVals.addElement(false);
			    			break;
			    		}

		          }
	          }
		}

    	return columnVals;
    }

   /**
    *
    * Access method to display Assign to PUEECO menu based on WIPBOM mode property entry.
    * @author YOQ
    * @param context the eMatrix code context object
    * @param String[] packed hashMap of request parameters
    * @throws Exception if the operation fails
    * @return boolean true if the WIPBOM entry is true else false to hide.
    *
    */

   public boolean isWipBomAllowed(Context context, String args[])
           throws Exception {
       boolean isWipBomAllowed = false;
       //String  isWipBomAllowd  = FrameworkProperties.getProperty("emxUnresolvedEBOM.WIPBOM.Allowed");
       String  isWipBomAllowd  = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOM.WIPBOM.Allowed");

       if (isWipBomAllowd.equalsIgnoreCase("true")) {
    	   isWipBomAllowed = true;
       }

       return isWipBomAllowed;
   }
   /**
    *
    * Method to get symbolic names of policies and their 1st revision .
    * @param context the eMatrix code context object
    * @param String[] packed hashMap of request parameters
    * @throws Exception if the operation fails
    * @return MapList.
    *
    */
	public MapList getPolicyRevision(Context context, String[] args) throws Exception {

		HashMap hmPolicyRev = new HashMap();
		MapList mPolicyName = new MapList();
		MapList mlResult = new MapList();
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String typeString = (String) programMap.get("type");

		try {
			BusinessType partBusinessType = new BusinessType(typeString, context.getVault());

			PolicyList allPartPolicyList = partBusinessType
					.getPoliciesForPerson(context, false);
			PolicyItr partPolicyItr = new PolicyItr(allPartPolicyList);
			Policy policyValue = null;
			String policyName = "";
			String symbolicName = "";
			String sRev;

				while (partPolicyItr.next()) {
					policyValue = (Policy) partPolicyItr.obj();
					policyName = policyValue.getName();
					sRev = policyValue.getFirstInSequence(context);
					symbolicName = PropertyUtil.getAliasForAdmin(context, "policy", policyName, true);
					hmPolicyRev.put(symbolicName, sRev);
					mPolicyName.add(symbolicName);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		mlResult.add(hmPolicyRev);
		mlResult.add(mPolicyName);

		return mlResult;
	}


   /* Range function to display policies in dropdown box */
   public HashMap getPolicies (Context context, String[] args) throws Exception {

	 HashMap paramMap   = (HashMap)JPO.unpackArgs(args);
	 HashMap requestMap = (HashMap)paramMap.get("requestMap");
	 String parentid    = (String)requestMap.get("objectId");

	 String parentType         = new DomainObject(parentid).getInfo(context, DomainConstants.SELECT_TYPE);
	 BusinessType partBusType  = new BusinessType(parentType, context.getVault());
	 PolicyList partPolicyList = partBusType.getPoliciesForPerson(context,false);
	 PolicyItr  partPolicyItr  = new PolicyItr(partPolicyList);
	 Locale Local = context.getLocale();
	 boolean isMBOMInstalled = EngineeringUtil.isMBOMInstalled(context);
	 String POLICY_STANDARD_PART = PropertyUtil.getSchemaProperty(context,"policy_StandardPart");
	 Policy partPolicy = null;
	 String policyName = "";
	 String policyAdminName = "";
	 String policyClassification = "";

	 HashMap rangeMap = new HashMap();
	 StringList columnVals = new StringList();
	 StringList columnVals_Choices = new StringList();

	 while(partPolicyItr.next())
	 {
		partPolicy = partPolicyItr.obj();
		policyName = partPolicy.getName();
		policyClassification = EngineeringUtil.getPolicyClassification(context, policyName);

		if(!isMBOMInstalled)
        {
        	if(policyName.equals(POLICY_STANDARD_PART))
        	{
        		continue;
        	}
        }
		if("Equivalent".equals(policyClassification) || "Manufacturing".equals(policyClassification))
		{
			continue;
		}
		policyAdminName = FrameworkUtil.getAliasForAdmin(context, "Policy", policyName, true);
		String tempPolicyName = replaceFirst(policyName.trim()," ", "_");
        //columnVals.add(UINavigatorUtil.getI18nString("emxFramework.Policy."+tempPolicyName, "emxFrameworkStringResource", languageStr));
		columnVals.add(EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", Local,"emxFramework.Policy."+tempPolicyName));
		columnVals_Choices.add(policyAdminName);
	 }
	 rangeMap.put("field_choices",columnVals_Choices);
	 rangeMap.put("field_display_choices", columnVals );

	 return rangeMap;
  }
   private boolean isNullOrEmpty(String testString)
	{
		return testString == null || testString.trim().length() == 0 || "null".equalsIgnoreCase(testString.trim());
	}
  private boolean isParentInSupersededState(Context context,String parentObjId) throws Exception
  {
	   String STATESUPERSEDED = PropertyUtil.getSchemaProperty(context,"policy",UnresolvedEBOMConstants.POLICY_CONFIGURED_PART, "state_Superseded");
	   String parentObjState  = DomainObject.newInstance(context, parentObjId).getInfo(context, DomainConstants.SELECT_CURRENT);
	   return STATESUPERSEDED.equalsIgnoreCase(parentObjState);
  }
  private StringBuffer constructUpdateOXMLStr(StringBuffer updateOXML,String childPartId,String rowId,String relId,String markup) throws Exception
  {

      return updateOXML.append("<item oId=\"")
		       .append(childPartId)
		       .append("\" rowId=\"")
		       .append(rowId)
		       .append("\" pId=\"null\" relId=\"")
		       .append(relId)
		       .append("\" markup=\"")
		       .append(markup)
			   .append("\"></item>");
  }
  /**
   * lookupEntries method checks the object entered manually is exists or not in the database
   * Method to Inline create & connect new Part objects in EBOM powerview IndentedTable
   * This method is invoked on clicking on Apply button in EBOM
    * @param args String array having the object Id(s) of the part.
   * @throws FrameworkException if creation of the Part Master object fails.
   */


  @com.matrixone.apps.framework.ui.ConnectionProgramCallable
  public  HashMap inlineCreateForConfiguredBOM(Context context, String[] args) throws Exception {

  	return null;
  }

  /**
   * Trigger method to check if the VPLM product is in Released state.
   * Part promotion is possible if product is in Released or Obsolete state, if VPM Visible = true.
   * @param context
   * @param args
   * @return
   * @throws Exception
   */
  public int checkVPMProductInReleasedState(Context context, String []args)  throws Exception {
      String partId = args[0];
      String targetState = PropertyUtil.getSchemaProperty(context, "policy",
    		  PropertyUtil.getSchemaProperty(context, "policy_VPLM_SMB") , args[1]);
      DomainObject partObj = DomainObject.newInstance(context, partId);
      String vplmVisible = partObj.getInfo(context, "attribute["+EngineeringConstants.ATTRIBUTE_VPM_VISIBLE+"].value");

      if("true".equalsIgnoreCase(vplmVisible)) {
          String productId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump",partId,"from["+RELATIONSHIP_PART_SPECIFICATION+"|to.type.kindof["+EngineeringConstants.TYPE_VPLM_CORE_REF+"]]" + ".to.id");

          if(productId != null && !"".equals(productId)){
              if (!PolicyUtil.checkState(context, productId, targetState, PolicyUtil.GE)) {
                  String strMessage = EngineeringUtil.i18nStringNow("emxEngineeringCentral.alert.releaseVPMProduct",
                      context.getSession().getLanguage());
                  emxContextUtil_mxJPO.mqlNotice(context,strMessage);
                  return 1;
              }
          }
      }
      return 0;
  }

  public Boolean displayProductConfigurationFilter(Context context, String[] args)
  throws Exception
  {
	  boolean isFTRInstalled = FrameworkUtil.isSuiteRegistered(context,"appVersionVariantConfiguration",false,null,null);
	  return Boolean.valueOf(isFTRInstalled);
  }

  public String getPCFilter1(Context context, String[] args) throws Exception {
	  String objectId= null;
	  HashMap programMap = (HashMap)JPO.unpackArgs(args);
	  HashMap requestMap= (HashMap)programMap.get("requestMap");
	  objectId = (String)requestMap.get("objectId1");
	  return getPCFilterExpressionDisplayBOM(context,"editPCFilter", "PUEUEBOMProductConfigurationFilter1OID","PCFilterId1",objectId,"showPCFilterDialog1");
  }

  public String getPCFilter2(Context context, String[] args) throws Exception {
	  String objectId= null;
	  HashMap programMap = (HashMap)JPO.unpackArgs(args);
	  HashMap requestMap= (HashMap)programMap.get("requestMap");
	  objectId = (String)requestMap.get("objectId2");
	  return getPCFilterExpressionDisplayBOM(context,"editPCFilter12", "PUEUEBOMProductConfigurationFilter2OID","PCFilterId2",objectId,"showPCFilterDialog2");

  }

  public String getPCFilterExpressionDisplayBOM(Context context,String divName, String textField, String hiddenField, String objectId, String javascriptFun)throws Exception
  {
	  String policyClassification = "policy.property[PolicyClassification].value";
      String SELECT_PART_POLICYCLASS = "from["+EngineeringConstants.RELATIONSHIP_PART_REVISION+"].to.policy.property[PolicyClassification].value";

      StringList objectSelect = new StringList(3);
      objectSelect.addElement(policyClassification);
      objectSelect.addElement(DomainConstants.SELECT_TYPE);
      objectSelect.addElement(SELECT_PART_POLICYCLASS);
      if(null!=objectId && !"null".equals(objectId)) {
		   DomainObject doPart = new DomainObject(objectId);
		   Map dataMap = doPart.getInfo(context, objectSelect);
		   String type = (String) dataMap.get(DomainConstants.SELECT_TYPE);

		   policyClassification =  EngineeringConstants.TYPE_PART_MASTER.equals(type)
		   							? (String) dataMap.get(SELECT_PART_POLICYCLASS)
   									: (String) dataMap.get(policyClassification);
	   }
       return getPCFilterExpressionDisplayBOM(context,divName,textField,hiddenField,policyClassification,objectId,javascriptFun);
   }

  public String getPCFilterExpressionDisplayBOM(Context context, String divName, String textField, String hiddenField,String policy, String objectId, String javascriptFun)throws Exception
  {
		StringBuffer sb = new StringBuffer(256);
		if (policy.equals("Unresolved") || null == objectId || "null".equals(objectId)) {
			sb.append("<div id=\"");
			sb.append(divName);
			sb.append("\" style=\"visibility: visible;\">");
		} else {
			sb.append("<div id=\"");
		   sb.append(divName);
		   sb.append("\" style=\"visibility: hidden;\">");
		}
	   sb.append("<input type=\"text\" id=\"" );
	   sb.append(textField);
	   sb.append("\" name=\"");
	   sb.append(textField);
	   sb.append("\" size=\"20\" readonly=\"readonly\" />");
	   sb.append("<input type=\"hidden\" id=\"" );
	   sb.append(hiddenField);
	   sb.append("\" name=\"");
	   sb.append(hiddenField);
	   sb.append("\" />");
	   sb.append("<a href=\"javascript:");
	   sb.append(javascriptFun);
	   sb.append("()\">");
	   sb.append("<img src=\"../common/images/iconActionEdit.gif\"  border=\"0\"/></a>");
	   if(divName.equalsIgnoreCase("editPCFilter"))
		   sb.append("&nbsp<a href=\"javascript:basicClear('").append(textField).append("');basicClear('PCFilterId1') \">").append(EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.Common.Clear", context.getSession().getLanguage())).append("</a>");
	   else
		   sb.append("&nbsp<a href=\"javascript:basicClear('").append(textField).append("');basicClear('PCFilterId2') \">").append(EngineeringUtil.i18nStringNow(context,"emxEngineeringCentral.Common.Clear", context.getSession().getLanguage())).append("</a>");
	   sb.append("</div>");
	   return sb.toString();
  	}

@com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
  public StringList getConfigurationContextPCIds(Context context, String[] args) throws Exception {
	  HashMap paramMap = JPO.unpackArgs(args);
	  String contextObjectId = (String) paramMap.get("objectId");

	  if (UIUtil.isNullOrEmpty(contextObjectId)) { return new StringList(); }

	  String SELECT_PC_ID = "from[" + PropertyUtil.getSchemaProperty(context, "relationship_ConfigurationContext") +
			  				"].to.from[" + PropertyUtil.getSchemaProperty(context, "relationship_MainProduct") +
			  				"].to.from[" + PropertyUtil.getSchemaProperty(context, "relationship_ProductConfiguration") + "].to.id";

	  return DomainObject.newInstance(context, contextObjectId).getInfoList(context, SELECT_PC_ID);
  }
}
