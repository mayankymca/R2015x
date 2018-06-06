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
import java.util.Map;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.List;
import matrix.util.StringList;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.engineering.EngineeringConstants;
import com.matrixone.apps.engineering.EngineeringUtil;
import com.matrixone.apps.engineering.TeamECO;

public class emxTeamECOBase_mxJPO extends emxECO_mxJPO {

	/**
	 * Constructor
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public emxTeamECOBase_mxJPO(Context context, String[] args)
			throws Exception {
		super(context, args);
	}

	/**
	 * To display the Policy field information in create Team ECO page.
	 * @param context
	 * @param args
	 * @return String
	 * @throws Exception
	 */
	public String getTeamECOPolicy(Context context, String[] args) throws Exception {

		String policyName = PropertyUtil.getSchemaProperty(context, "policy_DECO");
		String htmlStr = "<input type=\"hidden\" name=\"Policy\" value=\""+ policyName +"\"/>";
		return htmlStr + i18nNow.getAdminI18NString("Policy", policyName ,context.getSession().getLanguage());

    }

	/**
	 * Check trigger method on VPLM_SMB_Definition policy to check for the higher rev existence,
	 * Release of product revision will be blocked, if there is higher rev exists of the same
	 * @param context
	 * @param args
	 * @return int
	 * @throws Exception
	 */
    public int checkForProductLatestRevision(Context context, String args[]) throws Exception {

        String prodId = args[0];
        String prodNextState = args[2];

        DomainObject prodObj = new DomainObject(prodId);
        String type = PropertyUtil.getSchemaProperty(context,"type_ENOSTProductReference");
        if(prodObj.isKindOf(context, type)) {
              //check the latest product revision to be released in development mode (Product is connected development part)
              //If Product is not connected to any part, continue with the release process with out any check
              if(EngineeringConstants.STATE_VPLM_SMB_RELEASE.equals(prodNextState)) {
                    //Check for Development BOM existence
                  String connectedDevPartId = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump",prodId,
                                                                 "to[" + EngineeringConstants.RELATIONSHIP_PART_SPECIFICATION + "|from.policy==\"" +
                                                                 EngineeringConstants.POLICY_DEVELOPMENT_PART + "\"].from.id");

                    if(connectedDevPartId != null && !"".equals(connectedDevPartId)) {
                          if(!prodObj.isLastRevision(context)) {
                                throw new FrameworkException(EngineeringUtil.i18nStringNow(context,"emxEngineeringCentral.VPLMProduct.NotLatestProductRevision",
                                      context.getSession().getLanguage()));
                          }
                    }
              }
        }
        return 0;
    }


    /**
     * Method for the mass Go To Production on BOM
     * @param context
     * @param args
     * @return Strnig
     * @throws Exception
     */
    public String bomGotoProduction(Context context, String[] args) throws Exception {

		HashMap programMap = (HashMap)JPO.unpackArgs(args);
		HashMap paramMap   = (HashMap)programMap.get("paramMap");
		HashMap requestMap= (HashMap)programMap.get("requestMap");

		String strECOId = programMap.containsKey("changeId") ?
							(String) programMap.get("changeId") :
								(String)paramMap.get("objectId");

		// Selected Affected Item Ids
		String[] strObjIdAr = programMap.containsKey("selectedPartId") ?
							(String[]) programMap.get("selectedPartId") :
								(String[]) requestMap.get("selectedPartId");

		if(strObjIdAr == null) {
			return "Create Team ECO from Global Action";
		}

        String strObjId = strObjIdAr[0];

        return new TeamECO(strECOId).bomGotoProduction(context, strObjId);

    }
    
    private int lLength(List list) {
    	return list == null ? 0 : list.size();
    }
    
    /**This method is called from check trigger when promoting Development Part to Complete state.
     * This method checks wheather the connected Team Change object is in Release state.
     * If Team Change object is in Relese state it returns 0 else it returns 1.
     * @param context ematrix context.
     * @param args packed arguments.
     * @throws Exception if any operation fails.
     */
    public int checkConnectedTeamChangeIsInReleaseState(Context context, String[] args) throws Exception {
    	int iCheckTrigPass = 0;
    	
    	// If this trigger is called when promoting from Team Change the value will be TRUE else the value will be null
    	String strCalledFromTCpromotion = context.getCustomData("fromTCPromoteAction");
    	
    	// This check is required to avoid multiple check while promoting from Team Change.Below code should be executed only if it directly promoting from Part lifecycle.
    	if (!"TRUE".equals(strCalledFromTCpromotion)) {
	    	String partId = args[0];
	    	String teamChangePolicy = PropertyUtil.getSchemaProperty(context, "policy_TeamChange");
	    	String stateRelease = PropertyUtil.getSchemaProperty(context, "policy", teamChangePolicy, "state_Release");
	    	String relPattern = EngineeringConstants.RELATIONSHIP_AFFECTED_ITEM+ "," + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM+ "," + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM;
            String typePattern = DomainConstants.TYPE_ECO+ ","+ChangeConstants.TYPE_CHANGE_ACTION ;
	    	
	    	StringList objectSelect = new StringList(3);
	    	objectSelect.add(DomainConstants.SELECT_NAME);
	    	objectSelect.add(DomainConstants.SELECT_CURRENT);
	    	objectSelect.add(DomainConstants.SELECT_REVISION);
	    	objectSelect.add(DomainConstants.SELECT_TYPE);
	    	
	    	DomainObject domPartObj = DomainObject.newInstance(context, partId);
	    	
	    	MapList affectedItemList = domPartObj.getRelatedObjects(context, 
	    			                            relPattern, 
	    			                            typePattern , 
	    										objectSelect, 
	    										null, true, false, (short) 1, null, null, null, null, null);	    	
	    	
	    	if (affectedItemList.isEmpty()) {
	    		String strTeamChangeMand = EnoviaResourceBundle.getProperty(context, "emxTeamEngineering.TeamChange.Mandatory");
				
				if ("true".equalsIgnoreCase(strTeamChangeMand)) {
					iCheckTrigPass = 1;
					String strMessage = EngineeringUtil.i18nStringNow(context,"emxTeamEngineering.TBE.TeamChangeRequired", context.getSession().getLanguage());
					emxContextUtil_mxJPO.mqlNotice(context, strMessage);
				} else {
//					String SELECT_PREVIOUS_TC_INFO = "previous.to[" + EngineeringConstants.RELATIONSHIP_AFFECTED_ITEM + "]";
//					String strPrevObjectConnectedToTC = domPartObj.getInfo(context, SELECT_PREVIOUS_TC_INFO);
					String SELECT_PREVIOUS_TC_INFO = "previous.to[" + relPattern + "]";
					String strPrevObjectConnectedToTC = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", partId, SELECT_PREVIOUS_TC_INFO);
					
					if ("True".equals(strPrevObjectConnectedToTC)) {
						iCheckTrigPass = 1;
						String strMessage = EngineeringUtil.i18nStringNow(context,"emxTeamEngineering.TBE.TeamChangeRequired", context.getSession().getLanguage());
						emxContextUtil_mxJPO.mqlNotice(context, strMessage);
					}
				}
	    	} else {
	    		Map map = (Map) affectedItemList.get(0);
	    		String objectCurrentState = (String) map.get(DomainConstants.SELECT_CURRENT);
	    		String stype = (String) map.get(DomainConstants.SELECT_TYPE);
	    		if(ChangeConstants.TYPE_CHANGE_ACTION.equals(stype)){
	    			if (!ChangeConstants.STATE_CHANGE_ACTION_COMPLETE.equals(objectCurrentState)) {
			    		iCheckTrigPass = 1;
			    		String strMessage = EngineeringUtil.i18nStringNow(context,"emxEngineeringCentral.ECO.AutoCheckChangedItemToReleaseStateWarning", context.getSession().getLanguage());
			    		strMessage += " " + map.get(DomainConstants.SELECT_NAME) + " " + map.get(DomainConstants.SELECT_REVISION);   
			    		emxContextUtil_mxJPO.mqlNotice(context, strMessage);
		    		}
	    		}else{
	    			if (!stateRelease.equals(objectCurrentState)) {
			    		iCheckTrigPass = 1;
			    		String strMessage = EngineeringUtil.i18nStringNow(context,"emxTeamEngineering.TBE.AutoCheckTeamChangeItemToReleaseStateWarning", context.getSession().getLanguage());
			    		strMessage += " " + map.get(DomainConstants.SELECT_NAME) + " " + map.get(DomainConstants.SELECT_REVISION);   
			    		emxContextUtil_mxJPO.mqlNotice(context, strMessage);
		    		}
	    		}
	    	}
    	}
    	
    	return iCheckTrigPass;
    }
    
    /**This method is called from check trigger when promoting Team change to Release state.
     * This method checks wheather all the affected items connected to it is in Peer Review state.
     * If all the Affected Items are in Peer Review state it returns 0 else it return 1.
     * @param context ematrix context.
     * @param args packed arguments.
     * @throws Exception if any operation fails.
     */
    public int checkAllTheAffectedItemsAreInPeerReviewState(Context context, String[] args) throws Exception {
    	int iCheckTrigPass = 0;
    	
    	String teamChangeId = args[0];
    	String statePeerReview = PropertyUtil.getSchemaProperty(context, "policy", DomainConstants.POLICY_DEVELOPMENT_PART, "state_PeerReview"); 
    	String objectWhere = "current != '" + statePeerReview + "'";
    	
    	DomainObject domTeamObj = DomainObject.newInstance(context, teamChangeId);
    	
    	MapList affectedItemList = domTeamObj.getRelatedObjects(context, 
    										EngineeringConstants.RELATIONSHIP_AFFECTED_ITEM, DomainConstants.TYPE_PART, 
    										null, null, false, true, (short) 1, objectWhere, null, null, null, null);
    	
    	if (!affectedItemList.isEmpty()) {
    		iCheckTrigPass = 1;
    		String strMessage = EngineeringUtil.i18nStringNow(context,"emxTeamEngineering.TBE.AffectedItemsInPeerReviewState", context.getSession().getLanguage());
    		emxContextUtil_mxJPO.mqlNotice(context, strMessage);
    	}
    	
    	return iCheckTrigPass;    	
    }
    
    /** This method will be called as an action trigger of Team Change Policy when promoting to Release state.
     * This method releases the affected item connected to it.
     * @param context ematrix context.
     * @param args packed arguments.
     * @throws Exception if any operation fails.
     */
    public void setAffectedItemsToCompleteState(Context context, String[] args) throws Exception {
    	String teamChangeId = args[0];    	
    	String affectedItemId;
    	String stateComplete = PropertyUtil.getSchemaProperty(context, "policy", DomainConstants.POLICY_DEVELOPMENT_PART, "state_Complete");
    	String SELECT_AFFECTED_ITEM_IDS = "from[" + EngineeringConstants.RELATIONSHIP_AFFECTED_ITEM + "].to.id";
    	
    	DomainObject domTeamObj = DomainObject.newInstance(context, teamChangeId);
    	
    	StringList affectedItemIdList = domTeamObj.getInfoList(context, SELECT_AFFECTED_ITEM_IDS);    	
    	context.setCustomData("fromTCPromoteAction", "TRUE"); // To avoid DB hit done in development Part checkTrigger.  
    	
    	try {
	    	for (int i = 0, size = lLength(affectedItemIdList); i < size; i++) {
	    		affectedItemId = (String) affectedItemIdList.get(i);    		
	    		DomainObject.newInstance(context, affectedItemId).setState(context, stateComplete);
	    	}
    	} catch (Exception e) {
    		throw e;
    	} finally {
    		context.removeFromCustomData("fromTCPromoteAction");
    	}
    }
}

