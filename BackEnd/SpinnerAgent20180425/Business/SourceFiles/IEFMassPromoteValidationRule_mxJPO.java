/*
**  IEFMassPromoteValidationRule
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
*/
import java.util.Hashtable;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class IEFMassPromoteValidationRule_mxJPO
{
	private MCADMxUtil util									= null;
	private MCADServerGeneralUtil generalUtil				= null;
	private MCADServerResourceBundle serverResourceBundle	= null;
	private ResourceBundle iefProperties					= null;

	private String rootNodeState							= null;
	private String isMultiPromote							= null;

	private MCADGlobalConfigObject globalConfigObject		= null;
	
	public IEFMassPromoteValidationRule_mxJPO () {
	}
	
	public IEFMassPromoteValidationRule_mxJPO(Context context, String[] args) throws Exception
	{				
		String [] packedGCO		= new String[2];
		packedGCO[0]			= args[0];
		packedGCO[1]			= args[1];
		this.globalConfigObject	= (MCADGlobalConfigObject) JPO.unpackArgs(packedGCO);
		this.rootNodeState		= args[2];
		String localeLanguage	= args[3];
		this.isMultiPromote		= args[4];

		serverResourceBundle	= new MCADServerResourceBundle(localeLanguage);
		this.iefProperties      = PropertyResourceBundle.getBundle("ief");
		this.util				= new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());
		this.generalUtil		= new MCADServerGeneralUtil(context, globalConfigObject, serverResourceBundle, new IEFGlobalCache());
	}

	public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }

	/** This method contains the implementation for getting the remark for a particular child node in the 
	 finalization page if it is selected for promotion. The method is passed a business object ID of the child 
	 node as argument. If the child object is not a major object, the corresponding major object and it's state is found.
	 
	 If the method returns a non-blank string starting with 'false|', the user is not allowed to select the node and 
	 the string that is returned is shown to the user as a pop-up dialog. If the selection is implicit (e.g. if "select all children"
	 flag is true in the Promote dialog) then no message is shown but the node will not be selected for promotion.
	 
	 @param context The user context
	 @param args A string array of arguments used. 
	 The first element of the array MUST be the objectId 
	 @returns String

	*/
	public String validateObjectForSelection(Context context, String[] args) throws Exception
	{
		String remark		= "";
		String objectId		= args[0];				
		
		return remark;
	}

	/**
	 This function contains the implementation for validating the promotion of the objects in the 
	 finalization page. The function is passed the following arguments - child object ID,Root Object Id,state
	 of this Child node,strings representing the its selection and that of any of its parents.
	 
	 If this method returns a non-blank String starting with 'false|', is is treated as an error condition and the promote operation is
	 aborted. In this case, the String that is returned is shown to the user as an error message.
	 
	 @param context The user context
	 @param args A string array of a packed Hashtable, which has the follwing keys and values:

	 key			         value
	 isAnyParentSelected	 "true" if any of the parents of this child object is select in the promotion page
	 rootBusObjId			 the object ID of the root node in the promotion page
	 childBusObjId		     the object Id of this child node
	 childNodeState          the state of this child node's major object
	 isChildSelected         "true" if this child has been selected

	 @returns String
	*/
	public String validateObjectForPromotion(Context context, String[] args) throws Exception
	{
		String	errorMessage = "";
				
		Hashtable inputArgs			= (Hashtable)JPO.unpackArgs(args);
		String isAnyParentSelected	= (String)inputArgs.get("isAnyParentSelected");
		String rootBusObjectId		= (String)inputArgs.get("rootBusObjId");
		String childBusObjectId		= (String)inputArgs.get("childBusObjId");
		String childNodeState		= (String)inputArgs.get("childNodeState");
		String isChildSelected		= (String)inputArgs.get("isChildSelected");				

		return errorMessage;
	}

}
