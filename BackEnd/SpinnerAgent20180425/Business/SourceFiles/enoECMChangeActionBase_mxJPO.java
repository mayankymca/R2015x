/*
 * * ${CLASS:enoECMChangeActionBase}** Copyright (c) 1993-2015 Dassault
 * Systemes. All Rights Reserved.
 */

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import matrix.db.AttributeTypeList;
import matrix.db.BusinessInterface;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.db.Vault;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.dassault_systemes.enovia.enterprisechangemgt.admin.ECMAdmin;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeAction;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeManagement;
import com.dassault_systemes.enovia.enterprisechangemgt.util.ChangeUtil;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.UOMUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UICache;
import com.matrixone.apps.framework.ui.UIUtil;

public class enoECMChangeActionBase_mxJPO extends emxDomainObject_mxJPO {
	/**
	 *
	 */
	private static final long		serialVersionUID				= 1L;
	public static final String		ATTR_VALUE_MANDATORY			= "Mandatory";
	protected static String			RESOURCE_BUNDLE_COMPONENTS_STR	= "emxEnterpriseChangeMgtStringResource";
	protected static final String	SELECT_NEW_VALUE				= "New Value";
	private ChangeManagement		changeManagement				= null;

	private static final String		FORMAT_DATE						= "date";
	private static final String		FORMAT_NUMERIC					= "numeric";
	private static final String		FORMAT_INTEGER					= "integer";
	private static final String		FORMAT_BOOLEAN					= "boolean";
	private static final String		FORMAT_REAL						= "real";
	private static final String		FORMAT_TIMESTAMP				= "timestamp";
	private static final String		FORMAT_STRING					= "string";
	protected static final String	INPUT_TYPE_TEXTBOX				= "textbox";
	private static final String		INPUT_TYPE_TEXTAREA				= "textarea";
	private static final String		INPUT_TYPE_COMBOBOX				= "combobox";
	protected static final String	SETTING_INPUT_TYPE				= "Input Type";
	private static final String		SETTING_FORMAT					= "format";
	private static final String		FORMAT_CHOICES					= "choices";

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param args
	 * @throws Exception
	 */
	public enoECMChangeActionBase_mxJPO(Context context, String[] args) throws Exception {
		super(context, args);
	}

	/**
	 * Method to return CA Affecetd items
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	@com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getAffectedItems(Context context, String[] args) throws Exception {
		Map programMap = (HashMap) JPO.unpackArgs(args);
		String changeObjId = (String) programMap.get("objectId");
		return new ChangeAction(changeObjId).getAffectedItems(context);
	}

	/**
	 * Method to return implemented items
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	@com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getImplementedItems(Context context, String[] args) throws Exception {
		Map programMap = (HashMap) JPO.unpackArgs(args);
		String changeObjId = (String) programMap.get("objectId");
		return new ChangeAction(changeObjId).getImplementedItems(context);
	}

	/**
	 * Method to return prerequisites
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	@com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getPrerequisites(Context context, String[] args) throws Exception {
		Map programMap = (HashMap) JPO.unpackArgs(args);
		String changeObjId = (String) programMap.get("objectId");
		return new ChangeManagement(changeObjId).getPrerequisites(context, ChangeConstants.TYPE_CHANGE_ACTION);
	}

	/**
	 * Method to return prerequisites
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	@com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getRelatedItems(Context context, String[] args) throws Exception {
		Map programMap = (HashMap) JPO.unpackArgs(args);
		String changeObjId = (String) programMap.get("objectId");
		return new ChangeAction(changeObjId).getRelatedItems(context);
	}

	/**
	 * Method to add Responsible Organizatoin for CA.
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void connectResponsibleOrganization(Context context, String[] args) throws Exception {
		try {
			Map programMap = (HashMap) JPO.unpackArgs(args);
			HashMap hmParamMap = (HashMap) programMap.get("paramMap");
			String strChangeObjId = (String) hmParamMap.get("objectId");
			String strNewResponsibleOrgName = (String) hmParamMap.get(ChangeConstants.NEW_VALUE);
			this.setId(strChangeObjId);
			if (UIUtil.isNotNullAndNotEmpty(strNewResponsibleOrgName)) {
				this.setPrimaryOwnership(context, ChangeUtil.getDefaultProject(context), strNewResponsibleOrgName);
			}
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
	}

	/**
	 * connectTechAssignee -
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void connectTechAssignee(Context context, String[] args) throws Exception {
		try {
			Map programMap = (HashMap) JPO.unpackArgs(args);
			HashMap hmParamMap = (HashMap) programMap.get("paramMap");
			String strChangeObjId = (String) hmParamMap.get("objectId");
			String strNewTechAssignee = (String) hmParamMap.get("New OID");
			String relTechAssignee = PropertyUtil.getSchemaProperty(context, "relationship_TechnicalAssignee");

			ChangeAction changeAction = new ChangeAction(strChangeObjId);
			changeAction.connectTechAssigneeToCA(context, strChangeObjId, strNewTechAssignee, relTechAssignee);
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
	}

	/**
	 * connectSeniorTechAssignee - Connect new Senior Tech Assignee -Update
	 * Program
	 * 
	 * @param context
	 * @param args
	 * @return
	 * @throws Exception
	 */
	public void connectSeniorTechAssignee(Context context, String[] args) throws Exception {
		try {
			Map programMap = (HashMap) JPO.unpackArgs(args);
			HashMap hmParamMap = (HashMap) programMap.get("paramMap");
			String strChangeObjId = (String) hmParamMap.get("objectId");
			String strNewSeniorTechAssig = (String) hmParamMap.get("New OID");
			String relSeniorTechAssignee = PropertyUtil.getSchemaProperty(context, "relationship_SeniorTechnicalAssignee");
			this.setId(strChangeObjId);
			String strSrTechAssigneeRelId = getInfo(context, "from[" + relSeniorTechAssignee + "].id");

			if (!ChangeUtil.isNullOrEmpty(strSrTechAssigneeRelId)) {
				DomainRelationship.disconnect(context, strSrTechAssigneeRelId);
			}
			if (!ChangeUtil.isNullOrEmpty(strNewSeniorTechAssig)) {
				DomainRelationship.connect(context, new DomainObject(strChangeObjId), new RelationshipType(relSeniorTechAssignee), new DomainObject(
						strNewSeniorTechAssig));
			}
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
	}

	/**
	 * This is a check trigger method on (Pending --> InWork) to validate
	 * Estimated Completion Date and Technical Assignee on the Change Action
	 * 
	 * @param context
	 * @param args
	 *            Change Action Id and Notice
	 * @return integer (0 = pass, 1= block with notice)
	 * @throws Exception
	 */
	public int validateCompletionDateAndTechAssignee(Context context, String args[]) throws Exception {
		int iReturn = 0;
		try {
			if (args == null || args.length < 1) {
				throw (new IllegalArgumentException());
			}
			String strChangeId = args[0];
			this.setId(strChangeId);

			// Getting the Tecchnical Assignee connected
			String strTechAssigneeId = getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.id");
			String strEstCompletionDateNotice = EnoviaResourceBundle.getProperty(context, args[3], context.getLocale(), args[1]);
			String strTechAssigneeNotice = EnoviaResourceBundle.getProperty(context, args[3], context.getLocale(), args[2]);
			// Getting the Estimated Completion Date Value
			String strCompletionDate = (String) getAttributeValue(context, ChangeConstants.ATTRIBUTE_ESTIMATED_COMPLETION_DATE);

			// Validating If both are not empty, if so accordingly sending the
			// notice.
			if (ChangeUtil.isNullOrEmpty(strCompletionDate)) {
				emxContextUtilBase_mxJPO.mqlNotice(context, strEstCompletionDateNotice);
				iReturn = 1;
			}

			if (ChangeUtil.isNullOrEmpty(strTechAssigneeId)) {
				emxContextUtilBase_mxJPO.mqlNotice(context, strTechAssigneeNotice);
				iReturn = 1;
			}
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw new FrameworkException(Ex.getMessage());
		}
		return iReturn;
	}

	/**
	 * This is a check trigger method on (Pending --> InWork) to validate
	 * whether the Impact Analysis connected to the Change Action are in
	 * Complete State
	 * 
	 * @param context
	 * @param args
	 *            (Change Action Id and Notice)
	 * @return integer (0 = pass, 1= block with notice)
	 * @throws Exception
	 */
	public int impactAnalysisCompletion(Context context, String args[]) throws Exception {
		int iReturn = 0;
		try {
			if (args == null || args.length < 1) {
				throw (new IllegalArgumentException());
			}

			String strIAName = "";
			Map tempMap = null;
			String strChangeId = args[0];
			this.setId(strChangeId);
			StringBuffer sbMessage = new StringBuffer();
			String STATE_IA_COMPLETE = PropertyUtil.getSchemaProperty(context, "policy", ChangeConstants.POLICY_IMPACT, args[3]);
			StringList objectSelects = new StringList(SELECT_ID);
			objectSelects.add(SELECT_CURRENT);
			objectSelects.add(SELECT_NAME);

			String strNotice = EnoviaResourceBundle.getProperty(context, args[2], context.getLocale(), args[1]);

			String whereStr = SELECT_CURRENT + "!=" + STATE_IA_COMPLETE;

			// Getting all the Impact Analysis which are not in complete state.
			MapList mlIAOutput = getRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPACT_ANALYSIS, TYPE_IMPACT_ANALYSIS, objectSelects, null,
					false, true, (short) 1, whereStr, EMPTY_STRING, 0);

			if (mlIAOutput != null && !mlIAOutput.isEmpty()) {
				for (Object var : mlIAOutput) {
					tempMap = (Map) var;
					strIAName = (String) tempMap.get(SELECT_NAME);
					sbMessage = addToStringBuffer(context, sbMessage, strIAName);
				}
			}

			// If Message is not empty, send the notice with Impact Analysis
			// name, which are not in complete state.
			if (sbMessage.length() != 0) {
				emxContextUtilBase_mxJPO.mqlNotice(context, strNotice + sbMessage.toString());
				iReturn = 1;
			}
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw new FrameworkException(Ex.getMessage());
		}
		return iReturn;
	}

	/**
	 * Subsidiary method to add the new String to the StringBuffer
	 * 
	 * @param context
	 * @param sbOutput
	 *            - StringBuffer Output
	 * @param message
	 *            - String need to be added
	 * @return String Buffer
	 * @throws Exception
	 */
	private StringBuffer addToStringBuffer(Context context, StringBuffer sbOutput, String message) throws Exception {
		try {
			if (sbOutput != null && sbOutput.length() != 0) {
				sbOutput.append(", ");
				sbOutput.append(message);
			}
			else {
				sbOutput.append(message);
			}
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
		return sbOutput;
	}

	/**
	 * Check Trigger on (Pending --> InWork) to check whether Prerequisites
	 * Parent CA (Hard Dependency) are all in Complete State, Hard Dependency -
	 * Parent Change Action Id will be having attribute value
	 * "Type of Dependency" as "Hard".
	 * 
	 * @param context
	 * @param args
	 *            - Change Action Id
	 * @return (0 = pass, 1= block with notice)
	 * @throws Exception
	 */
	public int checkForDependency(Context context, String args[]) throws Exception {
		int iReturn = 0;
		try {
			if (args == null || args.length < 1) {
				throw (new IllegalArgumentException());
			}

			String strCAName = "";
			Map tempMap = null;
			String strChangeId = args[0];
			this.setId(strChangeId);
			StringBuffer sBusWhere = new StringBuffer();
			StringBuffer sRelWhere = new StringBuffer();
			StringBuffer sbMessage = new StringBuffer();

			String strNotice = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(),
					"EnterpriseChangeMgt.Notice.HardDependency");

			sBusWhere.append("(" + SELECT_CURRENT + " != \"");
			sBusWhere.append(ChangeConstants.STATE_CHANGE_ACTION_COMPLETE);
			sBusWhere.append("\" && ");
			sBusWhere.append(SELECT_CURRENT + " != \"");
			sBusWhere.append(ChangeConstants.STATE_CHANGE_ACTION_CANCEL);
			sBusWhere.append("\" && ");
			sBusWhere.append(SELECT_CURRENT + " != \"");
			sBusWhere.append(ChangeConstants.STATE_CHANGE_ACTION_HOLD);
			sBusWhere.append("\")");
			sRelWhere.append("attribute[");
			sRelWhere.append(ChangeConstants.ATTRIBUTE_PREREQUISITE_TYPE);
			sRelWhere.append("] == ");
			sRelWhere.append(ATTR_VALUE_MANDATORY);

			// Get all the Prerequisites which are not in complete state and
			// that are Hard Dependency.
			MapList mlPrerequisites = new ChangeAction(strChangeId).getPrerequisites(context, ChangeConstants.TYPE_CHANGE_ACTION,
					sBusWhere.toString(), sRelWhere.toString());

			if (mlPrerequisites != null && !mlPrerequisites.isEmpty()) {
				for (Object var : mlPrerequisites) {
					tempMap = (Map) var;
					strCAName = (String) tempMap.get(SELECT_NAME);
					sbMessage = addToStringBuffer(context, sbMessage, strCAName);
				}
			}

			// If Message is not empty, send the notice with Change Action which
			// are not completed.
			if (sbMessage.length() != 0) {
				emxContextUtilBase_mxJPO.mqlNotice(context, strNotice + "  " + sbMessage.toString());
				iReturn = 1;
			}
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw new FrameworkException(Ex.getMessage());
		}
		return iReturn;
	}

	/**
	 * Check Trigger on (Pending --> InWork) to check whether parent CO is in In
	 * Work state,
	 * 
	 * @param context
	 * @param args
	 *            - Change Action Id
	 * @return (0 = pass, 1= block with notice)
	 * @throws Exception
	 */
	public int checkCOState(Context context, String args[]) throws Exception {
		String strFunc = null;
		String strNotice = null;

		String strChangeId = args[0];
		String nextState = args[9]; // for custom change on cancel the check
									// trigger will be fired on promting to
									// cancel state.
		String type = args[8]; // for custom change on cancel the check trigger
								// will be fired on promting to cancel state.

		if (ChangeConstants.TYPE_CCA.equals(type)) {
			String policyConfiguredPart = PropertyUtil.getSchemaProperty(context, "policy_PUEECO");
			String stateCancelled = PropertyUtil.getSchemaProperty(context, "policy", policyConfiguredPart, "state_Cancelled");

			if (stateCancelled.equals(nextState)) {
				return 0;
			}
		}
		if (UIUtil.isNotNullAndNotEmpty(strChangeId)) {
			this.setId(strChangeId);

			String coObjIdSelect = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from[" + ChangeConstants.TYPE_CHANGE_ORDER + "].id";
			String coPolicySelect = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from[" + ChangeConstants.TYPE_CHANGE_ORDER + "].policy";
			String coCurrentState = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from[" + ChangeConstants.TYPE_CHANGE_ORDER + "].current";
			StringList select = new StringList();
			select.add(coObjIdSelect);
			select.add(coPolicySelect);
			select.add(coCurrentState);

			Map resultList = getInfo(context, select);
			String coObjId = (String) resultList.get(coObjIdSelect);
			String copolicy = (String) resultList.get(coPolicySelect);

			if (UIUtil.isNullOrEmpty(coObjId)) {
				String crObjectId = getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from["
						+ ChangeConstants.TYPE_CHANGE_REQUEST + "].id");

				if (UIUtil.isNotNullAndNotEmpty(crObjectId)) {
					strNotice = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
							"EnterpriseChangeMgt.Notice.COIsNotConnectedToCA");
				}
			}
			else {
				strFunc = context.getCustomData("massFunctionality");

				if (ChangeConstants.POLICY_FASTTRACK_CHANGE.equals(copolicy)) {
					if (ChangeConstants.TYPE_CCA.equals(type)) {
						String coState = (String) resultList.get(coCurrentState);
						String STATE_CO_ON_HOLD = PropertyUtil.getSchemaProperty(context, "policy", ChangeConstants.POLICY_FORMAL_CHANGE,
								"state_OnHold");
						if (coState.equals(STATE_CO_ON_HOLD)) {
							strNotice = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
									"EnterpriseChangeMgt.Notice.ConnectedCOInOnHoldState");
						}
					}
				}
				else {
					String STATE_CO_INWORK = PropertyUtil.getSchemaProperty(context, "policy", ChangeConstants.POLICY_FORMAL_CHANGE, "state_InWork");
					if ((new ChangeUtil().checkObjState(context, coObjId, STATE_CO_INWORK, ChangeConstants.NE) == 0)
							&& !(ChangeConstants.FOR_RELEASE.equals(strFunc) || ChangeConstants.FOR_OBSOLETE.equals(strFunc))) {
						strNotice = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
								"EnterpriseChangeMgt.Notice.ConnectedCONotInInWorkState");
					}
				}
			}
		}

		if (strNotice != null) {
			emxContextUtilBase_mxJPO.mqlNotice(context, strNotice);
		}

		if (strFunc != null) {
			context.clearCustomData();
		}

		return (strNotice == null) ? 0 : 1;
	}

	/**
	 * The Action trigger method on (Pending --> In Work) to Revise and Connect
	 * object to implemented items of CA. This can be used for generic purpose.
	 * 
	 * @param context
	 * @param args
	 * @return void
	 * @throws Exception
	 */

	public int reviseAndConnectToImplementedItems(Context context, String[] args) throws Exception {
		try {
			if (args == null || args.length < 1) {
				throw (new IllegalArgumentException());
			}
			String strId = "";
			String strType = "";
			String strName = "";
			String strCurrent = "";
			String strRevision = "";
			String strAttrRC = "";
			String strWhere = "";
			String strPolicy = "";
			String strLatestRevCurrent = "";
			String strLatestRevision = "";
			String strLatestRevisionId = "";
			String strObjectStateName = "";
			RelationshipType relType = null;
			Map tempMap = null;
			Map mapTemp = null;
			boolean bSpec = true;
			String nonCDMTypes = EnoviaResourceBundle.getProperty(context, "EnterpriseChangeMgt.Integration.NonCDMTypes");
			StringList typeList = new StringList();
			String typeSpec = "";
			String LastestRevId = "";

			HashMap latestRevisionMap = new HashMap();
			String strObjId = args[0];
			DomainObject dObj = new DomainObject(strObjId);

			ChangeUtil changeUtil = new ChangeUtil();
			StringList slNameList = new StringList();
			StringList slBusSelect = new StringList(4);
			slBusSelect.addElement(SELECT_ID);
			slBusSelect.addElement(SELECT_TYPE);
			slBusSelect.addElement(SELECT_NAME);
			slBusSelect.addElement(SELECT_CURRENT);
			slBusSelect.addElement(SELECT_REVISION);
			slBusSelect.addElement(SELECT_POLICY);

			ChangeAction changeAction = new ChangeAction(strObjId);

			StringList slRelSelect = new StringList();
			String strRequestedChange = ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE;
			slRelSelect.addElement(strRequestedChange);

			// Getting all the connected objects of context object
			MapList mlRelatedObjects = dObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, "*", slBusSelect,
					slRelSelect, false, true, (short) 1, "", "", 0);

			Iterator i = mlRelatedObjects.iterator();

			DomainObject domObj = new DomainObject();

			// Iterating all the Affected Items Objects
			while (i.hasNext()) {
				mapTemp = (Map) i.next();
				// Fetching all the values
				strId = (String) mapTemp.get(SELECT_ID);
				strType = (String) mapTemp.get(SELECT_TYPE);
				strName = (String) mapTemp.get(SELECT_NAME);
				strCurrent = (String) mapTemp.get(SELECT_CURRENT);
				strRevision = (String) mapTemp.get(SELECT_REVISION);
				strAttrRC = (String) mapTemp.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
				strPolicy = (String) mapTemp.get(SELECT_POLICY);

				strObjectStateName = ECMAdmin.getReleaseStateValue(context, strType, strPolicy);

				// Checking if the object is "For Revise" and state is Release
				if (strAttrRC.equalsIgnoreCase(ChangeConstants.FOR_REVISE) && strCurrent.equalsIgnoreCase(strObjectStateName)) {
					strWhere = "name == '" + strName + "' && revision == last";
					setId(strId);

					// Considering only one Latest Revision with that Name
					LastestRevId = getInfo(context, "last.id");

					setId(LastestRevId);

					tempMap = getInfo(context, slBusSelect);
					// Checking for the current state and revision of the object
					strLatestRevCurrent = (String) tempMap.get(SELECT_CURRENT);
					strLatestRevision = (String) tempMap.get(SELECT_REVISION);

					// Check if latest revision exists and which is not released
					// in the system
					if (!strRevision.equalsIgnoreCase(strLatestRevision)
							&& changeUtil.checkObjState(context, LastestRevId, strObjectStateName, ChangeConstants.LT) == 0) {
						// Getting the latest revision of object, if already
						// connected in Implemented Items
						MapList mlImplementedItems = dObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, "*", slBusSelect,
								null, false, true, (short) 1, strWhere, EMPTY_STRING, 0);

						if (mlImplementedItems.size() == 0) {
							// Connecting to Change Action object if latest
							// revision is not connected as Implemented Item
							changeAction.connectImplementedItems(context, new StringList(LastestRevId));
						}
					}
					else {
						domObj.setId(strId);

						if (UIUtil.isNotNullAndNotEmpty(nonCDMTypes))
							typeList = FrameworkUtil.split(nonCDMTypes, ",");
						// Modified for IR-264331 start
						if (domObj.isKindOf(context, CommonDocument.TYPE_DOCUMENTS)) {
							Iterator itr = typeList.iterator();
							while (itr.hasNext()) {
								typeSpec = (String) itr.next();
								typeSpec = PropertyUtil.getSchemaProperty(context, typeSpec);
								if (domObj.isKindOf(context, typeSpec)) {
									bSpec = false;
									break;
								}
							}
						}

						if (domObj.isKindOf(context, CommonDocument.TYPE_DOCUMENTS) && bSpec) {
							CommonDocument docItem = new CommonDocument(domObj);
							docItem.revise(context, true);
						}
						else {
							domObj.reviseObject(context, false);
						}

						// Modified for IR-264331 end

						// Selecting the latest revision business id
						strLatestRevisionId = (String) domObj.getInfo(context, "last.id");
						relType = new RelationshipType(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);
						// Adding the above latest revision object to
						// Implemented Item of CA
						changeAction.connectImplementedItems(context, new StringList(strLatestRevisionId));
						latestRevisionMap.put(strId, strLatestRevisionId);
					}
				}
			}

		}
		catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return 0;
	}

	/**
	 * The Action trigger method on (Pending --> In Work) to set current date as
	 * the Actual Start Date of Change Action
	 * 
	 * @param context
	 * @param args
	 *            (Change Action Id)
	 * @throws Exception
	 */
	public void setActualStartDate(Context context, String[] args) throws Exception {
		try {
			if (args == null || args.length < 1) {
				throw (new IllegalArgumentException());
			}
			String strObjId = args[0];
			this.setId(strObjId);
			SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
			String strActualStartDate = _mxDateFormat.format(new Date());

			// Setting the Current Date to the Actual Start Date.
			setAttributeValue(context, ATTRIBUTE_ACTUAL_START_DATE, strActualStartDate);
			// Below code is added to address use case where in CO has more than
			// one CAs and one of the CA is promoted to In Work state. In this
			// case the only Tech. Assginee of first CA is made owner of CA and
			// than the other CAs are promoted.
			// In this use case even after CA promotion to In Work the Tech.
			// Asignee is not made owner. IR-346050-3DEXPERIENCER2015x
			String strTechAssignee = getInfo(context, "from[" + PropertyUtil.getSchemaProperty(context, "relationship_TechnicalAssignee")
					+ "].to.name");
			if (getOwner(context).toString().compareToIgnoreCase(strTechAssignee) != 0) {
				setOwner(context, strTechAssignee);
			}

		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
	}

	/**
	 * Check Trigger on (Pending --> In Work) to check whether the context
	 * change Action's implemented items are not connected to the other Change
	 * Actions which are in "In Work" State.
	 * 
	 * @param context
	 * @param args
	 *            (Change Action Id and Notice)
	 * @return (0 = pass, 1= block with notice)
	 * @throws Exception
	 */
	public int validateImplementedItems(Context context, String[] args) throws Exception {
		int iReturn = 0;
		try {
			if (args == null || args.length < 1) {
				throw (new IllegalArgumentException());
			}
			Map tempMap = null;
			String strImplementedObjId = "";
			String strObjId = args[0];
			String strMessage = "";

			String strNotice = EnoviaResourceBundle.getProperty(context, args[2], context.getLocale(), args[1]);

			// Get All Implemented items from the Chnage Action
			MapList mlImplementedItems = new ChangeAction(strObjId).getImplementedItems(context);

			for (Object var : mlImplementedItems) {
				tempMap = (Map) var;
				strImplementedObjId = (String) tempMap.get(SELECT_ID);
				// Get Change Action which are in In Work State
				strMessage = getChangeAction(context, strImplementedObjId, strObjId);
			}

			// If Message Is not empty, send send a notice and block the
			// Promotion.
			if (strMessage != null && !strMessage.isEmpty()) {
				emxContextUtilBase_mxJPO.mqlNotice(context, strNotice + strMessage);
				iReturn = 1;
			}
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
		return iReturn;
	}

	/**
	 * Subsidiary method to get the change action connected to the Item, which
	 * are "In Work" state.
	 * 
	 * @param context
	 * @param strImplementedObjId
	 * @param strObjId
	 * @return String name of Change Actions which are in InWork State
	 * @throws Exception
	 */
	public String getChangeAction(Context context, String strImplementedObjId, String strObjId) throws Exception {
		StringBuffer sbOutput = new StringBuffer();
		try {
			String strChangeActionName = "";
			String strChangeActionId = "";
			Map tempMap = null;
			String STATE_CA_IN_WORK = PropertyUtil.getSchemaProperty(context, "policy", ChangeConstants.POLICY_CHANGE_ACTION, "state_InWork");
			this.setId(strImplementedObjId);
			StringList slObjectSelects = new StringList();
			slObjectSelects.add(SELECT_NAME);
			slObjectSelects.add(SELECT_ID);
			slObjectSelects.add(SELECT_CURRENT);

			String strWhere = SELECT_CURRENT + " == \"" + STATE_CA_IN_WORK + "\"";

			MapList mlChangeActionList = getRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, // relationship
																													// pattern
					ChangeConstants.TYPE_CHANGE_ACTION, // object pattern
					slObjectSelects, // object selects
					new StringList(DomainRelationship.SELECT_ID), // relationship
																	// selects
					true, // to direction
					false, // from direction
					(short) 1, // recursion level
					strWhere, // object where clause
					"", 0); // relationship where clause

			for (Object var : mlChangeActionList) {
				tempMap = (Map) var;
				strChangeActionName = (String) tempMap.get(SELECT_NAME);
				strChangeActionId = (String) tempMap.get(SELECT_ID);

				if (strChangeActionId != null && !strChangeActionId.equals(strObjId)) {
					sbOutput = addToStringBuffer(context, sbOutput, strChangeActionName);
				}
			}
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
		return sbOutput.toString();
	}

	/**
	 * Subsidiary method to get the Ids as per the selects and convrting to
	 * array.
	 * 
	 * @param context
	 * @param strSelect
	 * @param ObjectId
	 * @return String array
	 * @throws Exception
	 */
	public String[] getConnectedObjects(Context context, String strSelect, String ObjectId) throws Exception {
		StringList slObjectRel = new StringList();
		String[] ObjArr = new String[slObjectRel.size()];
		try {
			setId(ObjectId);
			slObjectRel = getInfoList(context, strSelect);
			ObjArr = (String[]) slObjectRel.toArray(new String[slObjectRel.size()]);
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
		return ObjArr;
	}

	/**
	 * Check Trigger on (In Work -->> In Approval) to check whether the Route
	 * Template or the Senior technical Assignee is connected to Change Action.
	 * 
	 * @param context
	 * @param args
	 *            (Change Action ID and Notice)
	 * @return (0 = pass, 1 = block the promotion)
	 * @throws Exception
	 */
	public int checkForSrTechnicalAssigneeAndRouteTemplate(Context context, String[] args) throws Exception {
		int iReturn = 0;
		if (args == null || args.length < 1) {
			throw (new IllegalArgumentException());
		}
		String objectId = args[0];// Change Object Id
		String strReviewerRouteTemplate = args[1];
		MapList mapRouteTemplate = new MapList();

		try {
			// create change object with the context Object Id
			setId(objectId);
			StringList selectStmts = new StringList(1);
			selectStmts.addElement("attribute[" + ATTRIBUTE_ROUTE_BASE_PURPOSE + "]");

			String whrClause = "attribute[" + ATTRIBUTE_ROUTE_BASE_PURPOSE + "] match '" + strReviewerRouteTemplate + "' && current == Active";

			// get route template objects from change object
			mapRouteTemplate = getRelatedObjects(context, RELATIONSHIP_OBJECT_ROUTE, TYPE_ROUTE_TEMPLATE, selectStmts, null, false, true, (short) 1,
					whrClause, null, 0);

			// get the Senior Technical Assignee connected
			String strResponsibleOrgRelId = getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_SENIOR_TECHNICAL_ASSIGNEE + "].id");

			// Send notice and block promotion if both are not connected.
			if ((mapRouteTemplate == null || mapRouteTemplate.isEmpty()) && ChangeUtil.isNullOrEmpty(strResponsibleOrgRelId)) {
				emxContextUtil_mxJPO.mqlNotice(context, EnoviaResourceBundle.getProperty(context, args[3], context.getLocale(), args[2]));
				iReturn = 1;
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return iReturn;
	}

	/**
	 * Action trigger on (InWork--> In Approval) to Promote all the
	 * Implemented/Affected Items connected to the Change Action to Approved
	 * State.
	 * 
	 * @param context
	 * @param args
	 *            (Change Action Id)
	 * @throws Exception
	 */
	public void promoteItemsToApproved(Context context, String[] args) throws Exception {
		try {
			String strItem = "";
			String strItemType = "";
			String strItemPolicy = "";
			String strChangeActionId = args[0];
			String strCurrentState = args[1];
			String strTargetState = args[2];
			setId(strChangeActionId);
			String STATE_CA_INAPPROVAL = PropertyUtil.getSchemaProperty(context, "policy", ChangeConstants.POLICY_CHANGE_ACTION, "state_InApproval");
			StringList objSelects = new StringList(SELECT_ID);
			objSelects.addElement(SELECT_TYPE);
			objSelects.addElement(SELECT_POLICY);

			MapList ImplementedItems = null;
			Map<String, String> implementedItemMap;
			String relWhereClause = "attribute[" + ATTRIBUTE_REQUESTED_CHANGE + "] == '" + ChangeConstants.FOR_RELEASE + "'";
			String stateApprovedMapping = "";
			boolean strAutoApproveValue = false;

			// if(!ChangeUtil.isNullOrEmpty(strTargetState) &&
			// STATE_CA_INAPPROVAL.equalsIgnoreCase(strTargetState))
			if (!ChangeUtil.isNullOrEmpty(strTargetState) && "state_InApproval".equalsIgnoreCase(strTargetState)) {
				// Get the Implemented Items connected.
				ImplementedItems = getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + ","
						+ ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, // relationship
																			// pattern
						"*", // object pattern
						objSelects, // object selects
						new StringList(DomainRelationship.SELECT_ID), // relationship
																		// selects
						false, // to direction
						true, // from direction
						(short) 1, // recursion level
						EMPTY_STRING, // object where clause
						relWhereClause, (short) 0); // relationship where clause

				Map relItemTypPolicyDtls = new HashMap();
				// Set the Approved State on the Implemented Items
				for (Object var : ImplementedItems) {
					implementedItemMap = (Map<String, String>) var;
					strItem = implementedItemMap.get(SELECT_ID);
					strItemType = implementedItemMap.get(SELECT_TYPE);
					strItemPolicy = implementedItemMap.get(SELECT_POLICY);
					stateApprovedMapping = ECMAdmin.getApproveStateValue(context, strItemType, strItemPolicy);
					strAutoApproveValue = ECMAdmin.getAutoApprovalValue(context, strItemType, strItemPolicy);
					if (strAutoApproveValue && !ChangeUtil.isNullOrEmpty(stateApprovedMapping)) {
						relItemTypPolicyDtls.put(strItem, strItemPolicy + "|" + strItemType);
					}
				}

				if (!relItemTypPolicyDtls.isEmpty())
					ECMAdmin.enforceApproveOrder(context, relItemTypPolicyDtls);
			}
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw new FrameworkException(Ex.getMessage());
		}
	}

	/**
	 * Action Trigger on (InApproval-- > Approved) to Set the current date as
	 * the Actual Completion Date
	 * 
	 * @param context
	 * @param args
	 *            (Cahnge Action Id)
	 * @throws Exception
	 */
	public void setActualCompletionDate(Context context, String[] args) throws Exception {
		try {
			if (args == null || args.length < 1) {
				throw (new IllegalArgumentException());
			}
			String strObjId = args[0];
			this.setId(strObjId);
			SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
			String strActualCompletionDate = _mxDateFormat.format(new Date());
			// Set the Actual Completion Date
			setAttributeValue(context, ATTRIBUTE_ACTUAL_COMPLETION_DATE, strActualCompletionDate);
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
	}

	/**
	 * Action trigger on (Approved --> Complete) to Promote the Implemented
	 * Items as per the Requested Change attribute on the relationship.
	 * 
	 * @param context
	 * @param args
	 *            (Change Action Id)
	 * @throws Exception
	 */
	public void promoteImplementedItemsAsRequestedChange(Context context, String[] args) throws Exception {
		try {
			Map tempMap = null;
			String strRequestedChange = "";
			String strItem = "";
			String strItemPolicy = "";
			String strItemType = "";
			String targetStateName = "";
			String strRelType = "";
			String strChangeActionId = args[0];
			setId(strChangeActionId);
			StringList busSelects = new StringList(SELECT_ID);
			busSelects.add(SELECT_POLICY);
			busSelects.add(SELECT_TYPE);
			StringList relSelects = new StringList(SELECT_RELATIONSHIP_ID);
			relSelects.add(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
			relSelects.add(SELECT_RELATIONSHIP_TYPE);
			StringList obsoleteItems = new StringList();
			StringList releasedItems = new StringList();
			StringList updatedItems = new StringList();

			// Get the implemented items & Affected Items connected.
			MapList listItems = getRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + ","
					+ ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, "*", busSelects, relSelects, false, true, (short) 1, EMPTY_STRING,
					EMPTY_STRING, (short) 0);

			Set allReleaseItems = new HashSet();
			Map relItemTypPolicyDtls = new HashMap();

			// promote Implemented items as per the Requested change attribute
			for (Object var : listItems) {
				targetStateName = null;
				tempMap = (Map) var;
				strRequestedChange = (String) tempMap.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
				strItem = (String) tempMap.get(SELECT_ID);
				strItemPolicy = (String) tempMap.get(SELECT_POLICY);
				strItemType = (String) tempMap.get(SELECT_TYPE);
				strRelType = (String) tempMap.get(SELECT_RELATIONSHIP_TYPE);

				if (ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase(strRequestedChange)) {
					targetStateName = ECMAdmin.getObsoleteStateValue(context, strItemType, strItemPolicy);

					// Obsoleting items
					setId(strItem);
					setState(context, targetStateName);

					if (ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM.equalsIgnoreCase(strRelType))
						obsoleteItems.addElement(strItem);
				}

				if (ChangeConstants.FOR_RELEASE.equalsIgnoreCase(strRequestedChange)) {
					allReleaseItems.add(strItem);
					relItemTypPolicyDtls.put(strItem, strItemPolicy + "|" + strItemType);

					targetStateName = ECMAdmin.getReleaseStateValue(context, strItemType, strItemPolicy);
					if (ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM.equalsIgnoreCase(strRelType))
						releasedItems.addElement(strItem);
				}

				if (ChangeConstants.FOR_UPDATE.equalsIgnoreCase(strRequestedChange)
						&& ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM.equalsIgnoreCase(strRelType)) {
					updatedItems.addElement(strItem);
				}

				/*
				 * if(!ChangeUtil.isNullOrEmpty(targetStateName)) {
				 * setId(strItem); setState(context, targetStateName); }
				 */
			}
			PropertyUtil.setRPEValue(context, "MX_SKIP_PART_PROMOTE_CHECK", "true", false);
			// Logic to RELEASE affecited/Implemented items in order as per the
			// admin settings
			ECMAdmin.enforceReleaseOrder(context, relItemTypPolicyDtls);

			ChangeAction changeAction = new ChangeAction(strChangeActionId);
			// Connects all the affected items
			changeAction.connectImplementedItems(context, obsoleteItems, ChangeConstants.FOR_OBSOLESCENCE);
			changeAction.connectImplementedItems(context, releasedItems, ChangeConstants.FOR_RELEASE);
			changeAction.connectImplementedItems(context, updatedItems, ChangeConstants.FOR_UPDATE);

		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
	}

	/**
	 * Action Trigger (InWork --> In Approval) to check whether Route Template
	 * is connected to the Change Action, If not get the Senior Technical
	 * Assignee and set as the Owner.
	 * 
	 * @param context
	 * @param args
	 *            (Change Action Id)
	 * @throws Exception
	 */
	public void setOwner(Context context, String args[]) throws Exception {
		if (args == null || args.length < 1) {
			throw (new IllegalArgumentException());
		}
		try {
			String objectId = args[0];// Change Object Id
			String strReviewerRouteTemplate = args[1];
			MapList mapRouteTemplate = new MapList();

			setId(objectId);
			StringList selectStmts = new StringList(1);
			selectStmts.addElement("attribute[" + ATTRIBUTE_ROUTE_BASE_PURPOSE + "]");

			String whrClause = "attribute[" + ATTRIBUTE_ROUTE_BASE_PURPOSE + "] match '" + strReviewerRouteTemplate + "' && current == Active";

			// get route template objects from change object
			mapRouteTemplate = getRelatedObjects(context, RELATIONSHIP_OBJECT_ROUTE, TYPE_ROUTE_TEMPLATE, selectStmts, null, false, true, (short) 1,
					whrClause, null, 0);

			// If not Route template is connected to the Change Action, the get
			// the Senior Technical Assignee and set as change Action Owner.
			if (mapRouteTemplate == null || mapRouteTemplate.isEmpty()) {

				String strSeniorTechAssignee = getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_SENIOR_TECHNICAL_ASSIGNEE + "].to.name");
				setOwner(context, strSeniorTechAssignee);
			}
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
	}

	/**
	 * Action Trigger on (InApproval --> Approved) to check whether the context
	 * Change Action is the ast Change Action to be Approved. If so then Promote
	 * the Change Order to "In Approval" state and notify CO Owner.
	 * 
	 * @param context
	 * @param args
	 *            (Change Action Id and Notice)
	 * @throws Exception
	 */
	public void checkForLastCA(Context context, String args[]) throws Exception {
		try {
			String strCAId;
			String strCAState;
			String strCAPolicy;
			String strChangeOrderId = null;
			String strChangeOrderPolicy = null;
			StringList strRouteList = new StringList();
			String strRoutetemplate = null;
			String strCCAId = null;
			Map tempMap = null;

			StringList listChangeActionAllStates;
			boolean pendingChangeExists = false;
			String objectId = args[0];// Change Object Id
			setId(objectId);

			StringList slObjectSelect = new StringList(4);
			slObjectSelect.add(SELECT_ID);
			slObjectSelect.add(SELECT_POLICY);
			slObjectSelect.add("from[" + RELATIONSHIP_OBJECT_ROUTE + "|to.type=='" + TYPE_ROUTE_TEMPLATE
					+ "' && to.revision == to.last &&  attribute[" + ATTRIBUTE_ROUTE_BASE_PURPOSE + "]==Approval].to.name");
			slObjectSelect.add("from[" + RELATIONSHIP_OBJECT_ROUTE + "|to.type=='" + TYPE_ROUTE + "' &&  attribute[" + ATTRIBUTE_ROUTE_BASE_STATE
					+ "]=='state_InApproval'].to.name");

			MapList resultList = getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ORDER,
					slObjectSelect, null, true, false, (short) 1, "", EMPTY_STRING, 0);

			if (resultList != null && !resultList.isEmpty()) {
				for (Object var : resultList) {
					tempMap = (Map) var;
					strChangeOrderId = (String) tempMap.get(SELECT_ID);
					strChangeOrderPolicy = (String) tempMap.get(SELECT_POLICY);
					if (tempMap.get("from[" + RELATIONSHIP_OBJECT_ROUTE + "].to.name") instanceof StringList) {
						strRouteList = (StringList) tempMap.get("from[" + RELATIONSHIP_OBJECT_ROUTE + "].to.name");
						strRoutetemplate = (String) strRouteList.get(0);
					}
					else {
						strRoutetemplate = (String) tempMap.get("from[" + RELATIONSHIP_OBJECT_ROUTE + "].to.name");
					}

				}
			}

			if (UIUtil.isNotNullAndNotEmpty(strChangeOrderId)) {
				// Get Change Actions connected to Change Order
				MapList mlChangeActions = getChangeActions(context, strChangeOrderId);
				HashMap releaseStateMap = ChangeUtil.getReleasePolicyStates(context);

				Map mapTemp;
				for (Object var : mlChangeActions) {
					mapTemp = (Map) var;
					strCAId = (String) mapTemp.get(SELECT_ID);
					if (!strCAId.equals(objectId)) {
						strCAState = (String) mapTemp.get(SELECT_CURRENT);
						strCAPolicy = (String) mapTemp.get(SELECT_POLICY);
						listChangeActionAllStates = ChangeUtil.getAllStates(context, strCAPolicy);
						if (new ChangeUtil().checkObjState(context, listChangeActionAllStates, strCAState, (String) releaseStateMap.get(strCAPolicy),
								ChangeConstants.LT) == 0) {
							if (ChangeConstants.TYPE_CCA.equals((String) mapTemp.get(SELECT_TYPE))) {
								String affectedItemExits = DomainObject.newInstance(context, strCAId).getInfo(context,
										"from[" + DomainConstants.RELATIONSHIP_AFFECTED_ITEM + "]");
								if ("True".equalsIgnoreCase(affectedItemExits)) {
									pendingChangeExists = true;
									break;
								}
								else {
									strCCAId = strCAId;
								}
							}
							else {
								pendingChangeExists = true;
								break;
							}
						}
					}
				}

				// If flag is empty, then set the CO state and notify the owner.
				if (!pendingChangeExists) {
					setId(strChangeOrderId);
					if (UIUtil.isNotNullAndNotEmpty(strRoutetemplate)) {
						setState(context, PropertyUtil.getSchemaProperty(context, "policy", strChangeOrderPolicy, "state_InApproval"));
					}
					else {
						setState(context, PropertyUtil.getSchemaProperty(context, "policy", strChangeOrderPolicy, "state_Complete"));
					}

					emxNotificationUtilBase_mxJPO.sendNotification(context, strChangeOrderId, new StringList(getOwner(context).getName()),
							new StringList(), new StringList(), args[1], args[2], new StringList(), args[3], null, null, null);
					if (strCCAId != null) {
						DomainObject.deleteObjects(context, new String[] { strCCAId });
					}
				}
			}
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
	}

	/**
	 * Subsidiary method to get Change Actions connected to the Change Order
	 * 
	 * @param context
	 * @param strChangeOrderId
	 * @return
	 * @throws Exception
	 */
	public MapList getChangeActions(Context context, String strChangeOrderId) throws Exception {
		StringList slObjectSelect = new StringList(4);
		slObjectSelect.add(SELECT_ID);
		slObjectSelect.add(SELECT_NAME);
		slObjectSelect.add(SELECT_CURRENT);
		slObjectSelect.add(SELECT_TYPE);
		slObjectSelect.add(SELECT_POLICY);
		StringList slRelSelect = new StringList(SELECT_RELATIONSHIP_ID);
		setId(strChangeOrderId);
		return getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, DomainConstants.QUERY_WILDCARD, slObjectSelect, slRelSelect,
				false, true, (short) 1, "", EMPTY_STRING, 0);
	}

	/**
	 * Method is called from TransferOwnerShip commands in Dashboard. It will
	 * identify the Person objects with specific roles from the XML depending on
	 * the affected Item's Type and Policy. It will identify the person from
	 * Responsible Organization of the CA. If not present, it will fetch the RO
	 * from Change Order. Based on the functionality it will either be
	 * TechAssignee or SrTechAssignee
	 * 
	 * @param context
	 * @param args
	 * @return String - representing the Org ID and Role
	 * @throws Exception
	 */
	public String checkAssigneeRole(Context context, String[] args) throws Exception {
		try {
			// unpacking the Arguments from variable args
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);
			String strObjectId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
			boolean isTechAssignee = true;
			boolean isSrTechAssignee = false;
			String sfunctionality = (String) requestMap.get("sfunctionality");
			if ("transferOwnershipToSrTechnicalAssignee".equals(sfunctionality)) {
				isTechAssignee = false;
				isSrTechAssignee = true;
			}
			return getRoleDynamicSearchQuery(context, strObjectId, isTechAssignee, isSrTechAssignee);
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
	}

	/**
	 * Method is called from CATransferOwnerShip commands in Properties page. It
	 * will identify the Person objects with specific roles from the XML
	 * depending on the affected Item's Type and Policy. It will identify the
	 * person from Responsible Organization of the CA. If not present, it will
	 * fetch the RO from Change Order.
	 * 
	 * @param context
	 * @param args
	 * @return String - representing the Org ID and Role
	 * @throws Exception
	 */
	public String getTechAssigneeandSrTechAssigneeRoleDynamicSearchQuery(Context context, String[] args) throws Exception {
		try {
			// unpacking the Arguments from variable args
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);
			String strObjectId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
			return getRoleDynamicSearchQuery(context, strObjectId, true, true);
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
	}

	/**
	 * Method is called from Href of the Technical Assignee on the CA Property
	 * field. It will identify the Person objects with specific roles from the
	 * XML depending on the affected Item's Type and Policy. It will identify
	 * the person from Responsible Organization of the CA. If not present, it
	 * will fetch the RO from Change Order.
	 * 
	 * @param context
	 * @param args
	 * @return String - representing the Org ID and Role
	 * @throws Exception
	 */
	public String getTechAssigneeRoleDynamicSearchQuery(Context context, String[] args) throws Exception {
		try {
			// unpacking the Arguments from variable args
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);
			HashMap fieldMap = (HashMap) programMap.get(ChangeConstants.FIELD_VALUES);
			HashMap typeAheadMap = (HashMap) programMap.get("typeAheadMap");
			String strObjectId = fieldMap != null ? (String) fieldMap.get(ChangeConstants.ROW_OBJECT_ID) : "";
			if (UIUtil.isNullOrEmpty(strObjectId) && typeAheadMap != null) {
				strObjectId = (String) typeAheadMap.get(ChangeConstants.ROW_OBJECT_ID);
			}
			if (UIUtil.isNullOrEmpty(strObjectId)) {
				strObjectId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
			}
			return getRoleDynamicSearchQuery(context, strObjectId, true, false);
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
	}

	/**
	 * Method is called from Href of the Senior Technical Assignee on the CA
	 * Property field. It will identify the Person objects with specific roles
	 * from the XML depending on the affected Item's Type and Policy. It will
	 * identify the person from Responsible Organization of the CA. If not
	 * present, it will fetch the RO from Change Order.
	 * 
	 * @param context
	 * @param args
	 * @return String - representing the Org ID and Role
	 * @throws Exception
	 */
	public String getSrTechAssigneeRoleDynamicSearchQuery(Context context, String[] args) throws Exception {
		try {
			// unpacking the Arguments from variable args
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);
			HashMap fieldMap = (HashMap) programMap.get(ChangeConstants.FIELD_VALUES);
			HashMap typeAheadMap = (HashMap) programMap.get("typeAheadMap");
			String strObjectId = fieldMap != null ? (String) fieldMap.get(ChangeConstants.ROW_OBJECT_ID) : "";
			if (UIUtil.isNullOrEmpty(strObjectId) && typeAheadMap != null) {
				strObjectId = (String) typeAheadMap.get(ChangeConstants.ROW_OBJECT_ID);
			}
			if (UIUtil.isNullOrEmpty(strObjectId)) {
				strObjectId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
			}
			return getRoleDynamicSearchQuery(context, strObjectId, false, true);
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
	}

	/**
	 * Subsidiary method for the getTechAssigneeRoleDynamicSearchQuery &
	 * getSrTechAssigneeRoleDynamicSearchQuery
	 * 
	 * @param context
	 * @param strObjectId
	 *            - Change Action Id
	 * @param isTechRole
	 *            - boolean for TechAssignee or Senior TechAssignee
	 * @return String
	 * @throws Exception
	 */
	public String getRoleDynamicSearchQuery(Context context, String strObjectId, boolean isTechRole, boolean isSrTechRole) throws Exception {
		try {
			setId(strObjectId);
			String strTechRole = "";
			String strSrTechRole = "";
			String strResponsibleOrg = "";
			String strIndiviAffectedItemType = "";
			String strIndiviAffectedItemPolicy = "";
			ChangeAction changeActionInstance = new ChangeAction();
			String strAffectedItemType = "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.type";
			String strAffectedItemPolicy = "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.policy";
			StringList slSelects = new StringList();
			slSelects.add(strAffectedItemType);
			slSelects.add(strAffectedItemPolicy);

			StringBuffer strRole = new StringBuffer();
			strResponsibleOrg = changeActionInstance.getResponsibleOrganization(context, strObjectId);

			// Get the Affected item connected to the Change Action.
			Map mapAffectedItemDetails = getInfo(context, slSelects);

			if (mapAffectedItemDetails != null && !mapAffectedItemDetails.isEmpty()) {
				strIndiviAffectedItemType = (String) mapAffectedItemDetails.get(strAffectedItemType);
				strIndiviAffectedItemPolicy = (String) mapAffectedItemDetails.get(strAffectedItemPolicy);

				// Get the Role from XML with the Type and Policy of Affected
				// Item.
				if (isTechRole) {
					strTechRole = ECMAdmin.getTechAssigneeRole(context, strIndiviAffectedItemType, strIndiviAffectedItemPolicy);
				}
				if (isSrTechRole) {
					strSrTechRole = ECMAdmin.getSrTechAssigneeRole(context, strIndiviAffectedItemType, strIndiviAffectedItemPolicy);
				}

			}
			if (UIUtil.isNotNullAndNotEmpty(strTechRole)) {
				strRole.append(strTechRole);
			}
			if (UIUtil.isNotNullAndNotEmpty(strSrTechRole)) {
				if (strRole.length() > 0) {
					strRole.append(",");
				}
				strRole.append(strSrTechRole);
			}

			return "MEMBER_ID=" + strResponsibleOrg + ":USERROLE=" + strRole.toString();
		}
		catch (Exception Ex) {
			Ex.printStackTrace();
			throw Ex;
		}
	}

	/**
	 * Displays the Range Values on Edit for Attribute Requested Change at
	 * COAffectedItemsTable/CAAffectedItemsTable..
	 * 
	 * @param context
	 *            the eMatrix <code>Context</code> object
	 * @param args
	 *            holds a HashMap containing the following entries:
	 * @param HashMap
	 *            containing the following keys, "objectId"
	 * @return HashMap contains actual and display values
	 * @throws Exception
	 *             if operation fails
	 * @since ECM R211
	 */
	public HashMap displayRequestedChangeRangeValues(Context context, String[] args) throws Exception {
		String strLanguage = context.getSession().getLanguage();
		StringList requestedChange = new StringList();
		StringList strListRequestedChange = FrameworkUtil.getRanges(context, ATTRIBUTE_REQUESTED_CHANGE);
		HashMap rangeMap = new HashMap();

		StringList listChoices = new StringList();
		StringList listDispChoices = new StringList();

		String attrValue = "";
		String dispValue = "";

		for (int i = 0; i < strListRequestedChange.size(); i++) {
			attrValue = (String) strListRequestedChange.get(i);
			dispValue = i18nNow.getRangeI18NString(ATTRIBUTE_REQUESTED_CHANGE, attrValue, strLanguage);
			listDispChoices.add(dispValue);
			listChoices.add(attrValue);
		}

		rangeMap.put("field_choices", listChoices);
		rangeMap.put("field_display_choices", listDispChoices);

		return rangeMap;
	}

	/**
	 * excludeAffectedItems() method returns OIDs of Affect Items which are
	 * already connected to context change object
	 * 
	 * @param context
	 *            Context : User's Context.
	 * @param args
	 *            String array
	 * @return The StringList value of OIDs
	 * @throws Exception
	 *             if searching Parts object fails.
	 */
	@com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
	public StringList excludeAffectedItems(Context context, String args[]) throws Exception {
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		String strChangeId = (String) programMap.get("objectId");
		StringList strlAffItemList = new StringList();

		if (ChangeUtil.isNullOrEmpty(strChangeId))
			return strlAffItemList;

		try {
			setId(strChangeId);
			strlAffItemList.addAll(getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id"));

		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return strlAffItemList;
	}

	/**
	 * Updates the Range Values for Attribute RequestedChange Based on User
	 * Selection
	 * 
	 * @param context
	 *            the eMatrix <code>Context</code> object
	 * @param args
	 *            holds a HashMap containing the following entries: paramMap - a
	 *            HashMap containing the following keys,
	 *            "relId","RequestedChange"
	 * @return int
	 * @throws Exception
	 *             if operation fails
	 * @since
	 **/
	public int updateRequestedChangeValues(Context context, String[] args) throws Exception {
		try {
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			HashMap paramMap = (HashMap) programMap.get(ChangeConstants.PARAM_MAP);
			HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);

			String changeActionId = (String) paramMap.get(ChangeConstants.OBJECT_ID);
			String changeObjId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
			String affectedItemRelId = (String) paramMap.get(ChangeConstants.SELECT_REL_ID);
			String strNewRequestedChangeValue = (String) paramMap.get(ChangeConstants.NEW_VALUE);
			changeManagement = new ChangeManagement(changeActionId);
			String affectedItemObjId = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", affectedItemRelId, "to.id");
			String message = changeManagement.updateRequestedChangeValues(context, affectedItemObjId, affectedItemRelId, strNewRequestedChangeValue);
			if ("".equals(message)) {
				return 0;// operation success
			}
			else {
				emxContextUtil_mxJPO.mqlNotice(context, message);
				return 1;// for failure
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw new FrameworkException(ex.getMessage());
		}
	}

	/**
	 * 
	 * @return
	 */
	public MapList getCustomAttributes(Context context, String[] args) throws Exception {

		Map programMap = (HashMap) JPO.unpackArgs(args);
		Map requestMap = (Map) programMap.get("requestMap");

		MapList mlColumns = new MapList();
		Map mapColumn = null;
		Map mapSettings = null;
		StringList attributeList = null;
		Iterator<String> attrItr = null;
		String interfaceName = "";
		String attrName = "";
		String sGroupHeader = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(),
				"EnterpriseChangeMgt.Label.CustomAttributes");
		// Find object's type
		String strObjectId = (String) requestMap.get("objectId");
		DomainObject dmoObject = new DomainObject(strObjectId);
		Map objAttributeMap = null;

		StringList interfaceList = FrameworkUtil.split(
				MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", strObjectId, "from["
						+ ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].interface", "|"), "|");
		Set interfaceSet = new HashSet(interfaceList);
		Iterator<String> interfaceItr = interfaceSet.iterator();
		while (interfaceItr.hasNext()) {
			interfaceName = interfaceItr.next();
			objAttributeMap = new HashMap();
			try {
				BusinessInterface intface = new BusinessInterface(interfaceName, new Vault(""));
				AttributeTypeList attributeTypeList = intface.getAttributeTypes(context);
				if (attributeTypeList != null) {
					objAttributeMap = FrameworkUtil.toMap(context, attributeTypeList);
					Iterator<String> keyItr = objAttributeMap.keySet().iterator();
					while (keyItr.hasNext()) {
						attrName = keyItr.next();
						mapColumn = new HashMap();
						mapSettings = new HashMap();
						mapColumn.put("settings", mapSettings);
						mapColumn.put("name", attrName);
						mapColumn.put("expression_relationship", "attribute[" + attrName + "].value");
						mapColumn.put("label", "emxFramework.Attribute." + attrName.replaceAll(" ", "_"));
						mapSettings.put("Group Header", sGroupHeader);
						mapSettings.put("Field Type", "attribute");
						mapSettings.put("Admin Type", PropertyUtil.getAliasForAdmin(context, "attribute", attrName, true));
						mapSettings.put("Editable", "true");
						mapSettings.put("Registered Suite", "Framework");

						HashMap attrMap = (HashMap) objAttributeMap.get(attrName);
						setColumnSettings(context, mapSettings, (String) attrMap.get(UICache.TYPE), (StringList) attrMap.get(FORMAT_CHOICES),
								(String) attrMap.get("multiline"));
						mapColumn.put(UICache.UOM_ASSOCIATEDWITHUOM, UOMUtil.isAssociatedWithDimension(context, attrName) + "");
						mapColumn.put(UICache.DB_UNIT, UOMUtil.getSystemunit(context, null, attrName, null));
						mapColumn.put(UICache.UOM_UNIT_LIST, UOMUtil.getDimensionUnits(context, attrName));
						if (UOMUtil.isAssociatedWithDimension(context, attrName)) {
							mapSettings.remove(SETTING_FORMAT);
						}
						mlColumns.add(mapColumn);
					}
				}

			}
			catch (Exception ex) {
				ex.printStackTrace();
			}

		}

		return mlColumns;
	}

	/**
	 * Method to get the proper Input Type/Format settings for interface
	 * attributes
	 * 
	 * @param context
	 * @param columnMap
	 * @param attrType
	 * @param choicesList
	 * @param sMultiLine
	 * @throws MatrixException
	 */
	private void setColumnSettings(Context context, Map columnMap, String attrType, StringList choicesList, String sMultiLine) throws MatrixException {
		String strFieldFormat = "";
		String strFieldIPType = INPUT_TYPE_TEXTBOX;

		if (FORMAT_STRING.equalsIgnoreCase(attrType)) {
			strFieldIPType = INPUT_TYPE_TEXTBOX;
			if (choicesList != null && choicesList.size() > 0) {
				strFieldIPType = INPUT_TYPE_COMBOBOX;
			}
			else if ("true".equalsIgnoreCase(sMultiLine)) {
				strFieldIPType = INPUT_TYPE_TEXTAREA;
			}
		}
		else if (FORMAT_BOOLEAN.equalsIgnoreCase(attrType)) {
			strFieldIPType = INPUT_TYPE_COMBOBOX;
		}
		else if (FORMAT_REAL.equalsIgnoreCase(attrType)) {
			if (choicesList != null && choicesList.size() > 0) {
				strFieldIPType = INPUT_TYPE_COMBOBOX;
			}
			strFieldFormat = FORMAT_NUMERIC;
		}
		else if (FORMAT_TIMESTAMP.equalsIgnoreCase(attrType)) {
			strFieldFormat = FORMAT_DATE;
		}
		else if (FORMAT_INTEGER.equalsIgnoreCase(attrType)) {
			if (choicesList != null && choicesList.size() > 0) {
				strFieldIPType = INPUT_TYPE_COMBOBOX;
			}
			strFieldFormat = FORMAT_INTEGER;
		}

		columnMap.put(SETTING_INPUT_TYPE, strFieldIPType);
		if (strFieldFormat.length() > 0)
			columnMap.put(SETTING_FORMAT, strFieldFormat);
	}

	/**
	 * The Action trigger method on (Pending --> In Work) to Promote Connected
	 * CO to In Work State
	 * 
	 * @param context
	 * @param args
	 *            (Change Action Id)
	 * @throws Exception
	 */
	public void promoteConnectedCO(Context context, String[] args) throws Exception {
		new ChangeAction().promoteConnectedCO(context, args);
	}

	/**
	 * Reset Owner on demote of ChangeAction
	 * 
	 * @param context
	 *            the eMatrix <code>Context</code> object
	 * @param args
	 *            holds the following input arguments: 0 - String holding the
	 *            object id. 1 - String to hold state.
	 * @returns void.
	 * @throws Exception
	 *             if the operation fails
	 * @since ECM R417
	 */
	public void resetOwner(Context context, String[] args) throws Exception {
		try {
			String objectId = args[0]; // changeObject ID
			setObjectId(objectId);
			String strCurrentState = args[1]; // current state of ChangeObject

			StringList select = new StringList(SELECT_OWNER);
			select.add(SELECT_ORIGINATOR);
			select.add(SELECT_POLICY);
			select.add(ChangeConstants.SELECT_TECHNICAL_ASSIGNEE_NAME);
			Map resultList = getInfo(context, select);
			String currentOwner = (String) resultList.get(SELECT_OWNER);
			String sOriginator = (String) resultList.get(SELECT_ORIGINATOR);
			String sPolicy = (String) resultList.get(SELECT_POLICY);
			String previousOwner = (String) resultList.get(ChangeConstants.SELECT_TECHNICAL_ASSIGNEE_NAME);

			if (ChangeConstants.POLICY_CHANGE_ACTION.equalsIgnoreCase(sPolicy)
					&& ChangeConstants.STATE_CHANGE_ACTION_INAPPROVAL.equalsIgnoreCase(strCurrentState) && !ChangeUtil.isNullOrEmpty(previousOwner)
					&& !currentOwner.equalsIgnoreCase(previousOwner)) {
				setOwner(context, previousOwner);

			}

		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw new FrameworkException(ex.getMessage());
		}
	}

}
