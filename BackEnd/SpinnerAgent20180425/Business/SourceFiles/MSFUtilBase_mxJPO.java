/*
**   MSFUtilBase.java
**
**   Copyright (c) 1992-2015 Dassault Systemes.
**   All Rights Reserved.
**   This program contains proprietary and trade secret information of MatrixOne,
**   Inc.  Copyright notice is precautionary only
**   and does not evidence any actual or intended publication of such program
**   
** @quickReview 17:03:16 ACE2 TSK3438445: ENOVIA_BAA_MSF_2018x_Backport Microsoft Project Integration from 2017x to 2015x
** @quickreview 17:04:10 ACE2 Code Clean-up: Change inclusive mapping filter to exclusive
** 
**
*/

import matrix.db.Context;
import matrix.util.StringList;

import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;


public class MSFUtilBase_mxJPO {

	private StringList ExperimentSpecificAttrList = new StringList(new String[] { "PercentComplete", "ActualDuration", "ActualStart", "ActualFinish" });

	public MSFUtilBase_mxJPO (Context context, String[] args)
			throws Exception {
		super();
	}

	/**
	 * This method is executed if a specific method is not specified.
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args holds no arguments
	 * @return int
	 * @throws Exception if the operation fails
	 * @since AEF Rossini
	 */
	public int mxMain(Context context, String[] args)
			throws FrameworkException {
		if (!context.isConnected())
			throw new FrameworkException(ComponentsUtil.i18nStringNow("emxTeamCentral.Generic.NotSupportedOnDesktopClient", context.getLocale().getLanguage()));
		return 0;
	}

	public String getInstalledProducts(Context context,String[] args)
	{
		String retVal = "command_IEFDesktopCollections;command_IEFDesktopMyLockedObjects;command_IEFDesktopTMCAllWorskpace;command_MsoiTMCFolders;command_IEFDesktopRecentlyCheckInFiles;command_MsoiGeneralSearch;";

		try {
			// Product names are not localized.
			String strCommand = "print prog $1 select property dump $2";

			String strOutput = MqlUtil.mqlCommand(context, strCommand,
					"eServiceSystemInformation.tcl", ",");

			if (strOutput.contains("appVersionLibraryCentral"))
				retVal += "command_MsoiLibraryCentralMyLibraries;";

			if (strOutput.contains("appVersionProgramCentral"))
				retVal += "command_MsoiPMCAllProjectsMyDesk;command_MsoiPMCFolders;command_MsoiWBSTasks;command_MsoiOpenWBSTasks;command_MsoiClosedWBSTasks;";

			if (strOutput.contains("appVersionProductLine"))
				retVal += "command_MsoiProductCentralMyBuilds;command_MsoiProductCentralMyProducts;";

			if (strOutput.contains("appVersionSupplierCentral"))
				retVal += "command_MsoiWBSTasks;command_MsoiOpenWBSTasks;command_MsoiClosedWBSTasks;";

			if (strOutput.contains("appVersionVariantConfiguration"))
				retVal += "command_MsoiProductCentralMyFeatures;";

			if (strOutput.contains("appVersionRequirementsManagement"))
				retVal += "command_MsoiProductCentralMyRequirements;command_MsoiProductCentralSubRequirements;";

			if (strOutput.contains("appVersionIntegrationFramework"))
				retVal += "command_MsoiProjectFolderContent;";

			if (strOutput.contains("appVersionX-BOMEngineering"))
				retVal += "command_MsoiMyParts;command_MsoiReferenceDocuments;command_MsoiSpecifications;command_MsoiEBOM;";

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return retVal;
	}
	
	public StringList GetTypeSpecificAttributeList(Context context, String[] args)
	{
		String type = args[0];
		String isExperiment = args[1];
		StringList attrList = GetExcludedAttributeMappings(type);
		StringList refList = "true".equalsIgnoreCase(isExperiment) ? ExperimentSpecificAttrList : new StringList();

		if(attrList != null && attrList.size() > 0)
		{
			refList.addAll(attrList);
			return refList;
		}
		else
		{
			return "true".equalsIgnoreCase(isExperiment) ? ExperimentSpecificAttrList : null;
		}
	}
	
	/**
	 * To add specific filter on a projectType admin needs to override this method and write similar code
	 * tip: Copy paste this function in MSFUtil JPO, uncomment the sample code and add your required selectable for the required type 
	 * 
	 * Added selectable will be suppressed from Synchronization
	 *
	 */
	public StringList GetExcludedAttributeMappings(String type)
	{
		StringList attrList = new StringList();
		
		/*switch(type)
		{				
		case "CustomProjectSpace1":
			attrList.addElement("CustomAttribute1");
			attrList.addElement("CustomAttribute2");
			break;

		case "CustomProjectSpace2":	
			attrList.addElement("CustomAttribute3");
			attrList.addElement("CustomAttribute4");
			break;
		}*/

		return attrList;
	}

	public String GetTypeSpecificAttributeListWrapper(Context context, String[] args)
	{
		StringList typeSpecificList = GetTypeSpecificAttributeList(context,args);

		String ret = "";

		if(typeSpecificList != null)
		{
			for (String str : typeSpecificList.toList()) 
			{
				ret += str + ",";
			}
		}

		return ret;	
	}

	public boolean checkUserIsAdmin(Context context) {

		String accessUsers = "role_AdministrationManager,role_VPLMAdmin";

		boolean bUserAdmin = false;
		try {
			bUserAdmin = PersonUtil.hasAnyAssignment(context, accessUsers);
		} catch (FrameworkException e) {
			e.printStackTrace();
		}
		return bUserAdmin;
	}
}
