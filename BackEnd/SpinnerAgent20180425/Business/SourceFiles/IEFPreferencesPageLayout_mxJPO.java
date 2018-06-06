/*
**  IEFPreferencesPageLayout
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Returns Preferences page layout
*/
/**
 * IEFPreferencesPageLayout jpo
 * This jpo returns a string containing xml for generating Preferences page
 */

import matrix.db.Context;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class IEFPreferencesPageLayout_mxJPO
{

	private final String COMBO_BOX		= "ComboBox";
	private final String EDIT_BOX		= "EditBox";
	private final String RADIO_BUTTON	= "Radio";
	private final String POPUP_LIST		= "PopupList";
	private final String CHECK_BOX		= "CheckBox";
	private final String SEARCH_BOX     = "SearchBox";
	private final String HYPER_LINK		= "HyperLink";

	public IEFPreferencesPageLayout_mxJPO()
    {
    }
    
	public IEFPreferencesPageLayout_mxJPO (Context context, String[] args) throws Exception
    {
      if (!context.isConnected())
      MCADServerException.createException("not supported no desktop client", null);

    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }
	
	private void init(Context context, String language)
	{
		IEFGlobalCache cache = new IEFGlobalCache();
		MCADServerResourceBundle serverResourceBundle = new MCADServerResourceBundle(language);
	}

	/**
	 * Entry Point
	 * This method returns a string containing xml which is used for generating Preferences page
	 *
	 */
	public String getPreferencesPageLayout(Context context, String[] args)throws Exception
	{   
		String language			= args[0];
		String versioningFlag	= args[1];

		init(context, language);

		StringBuffer preferencesPageLayout = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

		preferencesPageLayout.append("<preferences>");

		//Category "Checkin" starts
		preferencesPageLayout.append("<category name=\"Checkin\">");
		
		//Preference - MCADInteg-DeleteLocalFileOnCheckin 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-DeleteLocalFileOnCheckin") + "\">");
		preferencesPageLayout.append("<label>DeleteLocalFilesOnCheckin</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - MCADInteg-LockObjectOnCheckin
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-LockObjectOnCheckin") + "\">");
		preferencesPageLayout.append("<label>RetainLockOnCheckin</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - MCADInteg-CopyRelAttribOnCheckin 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-CopyRelAttribOnCheckin") + "\">");
		preferencesPageLayout.append("<label>CopyRelAttributesOnCheckin</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		if(versioningFlag.equalsIgnoreCase("true"))
		{
			//Preference - MCADInteg-CreateVersionOnCheckin 
			preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-CreateVersionOnCheckin") + "\">");
			preferencesPageLayout.append("<label>CreateVersionOnCheckin</label>");
			preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
			preferencesPageLayout.append("</preference>");
		}

		//Preference - MCADInteg-ApplyToChildren 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-ApplyToChildren") + "\">");
		preferencesPageLayout.append("<label>ApplyToChildrenOnCheckin</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - IEF-UseBulkLoading 
		//preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-UseBulkLoading") + "\">");
		//preferencesPageLayout.append("<label>UseBulkLoading</label>");
		//preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		//preferencesPageLayout.append("</preference>");


		//Preference - MCADInteg-BackgroundCheckin 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-BackgroundCheckin") + "\">");
		preferencesPageLayout.append("<label>BackgroundCheckin</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");
		
		//Preference - IEF-DeleteFilesBehaviourOnCheckin 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-DeleteFilesBehaviourOnCheckin") + "\">");
		preferencesPageLayout.append("<label>DeleteFilesBehaviourOnCheckin</label>");
		preferencesPageLayout.append("<uitype>" + COMBO_BOX + "</uitype>");
		preferencesPageLayout.append("<options>DeleteOnlySelectedFiles|DeleteAllLocalFiles</options>");
		preferencesPageLayout.append("</preference>");

                //Preference - IEF-BackgroundCheckinDirectory 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-BackgroundCheckinDirectory")+ "\">");
		preferencesPageLayout.append("<label>BackgroundCheckinDirectory</label>");
		preferencesPageLayout.append("<uitype>" + POPUP_LIST + "</uitype>");
		preferencesPageLayout.append("</preference>");	 
 	
		//Preference - MCADInteg-PreCheckInEvaluation 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-PreCheckInEvaluation") + "\">");
		preferencesPageLayout.append("<label>PreCheckinEvaluation</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - IEF-AllowAutoGenerationForManualDerivedOutput 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-AllowAutoGenerationForManualDerivedOutput") + "\">");
		preferencesPageLayout.append("<label>AutoGenerationForManualDerivedOutput</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		preferencesPageLayout.append("</category>");
		//Category "Checkin" ends
		
		//Category "Checkout" starts
		preferencesPageLayout.append("<category name=\"CheckoutOrDownload\">");


		//Preference - MCADInteg-LockObjectOnCheckout 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-LockObjectOnCheckout") + "\">");
		preferencesPageLayout.append("<label>LockObjectsOnCheckout</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - IEF-LocalCheckout 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-LocalCheckout") + "\">");
		preferencesPageLayout.append("<label>LocalCheckout</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");
		
		//Preference - MCADInteg-SelectFirstLevelChildren 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-SelectFirstLevelChildren") + "\">");
		preferencesPageLayout.append("<label>SelectFirstLevelChildren</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

                //Preference - MCADInteg-SelectRequiredChildren 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-SelectRequiredChildren") + "\">");
		preferencesPageLayout.append("<label>SelectRequiredChildren</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - IEF-ApplyViewToChildrenOnly 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-ApplyViewToChildrenOnly") + "\">");
		preferencesPageLayout.append("<label>ApplyViewToChildrenOnly</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - MCADInteg-CopyRelAttribOnCheckout 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-CopyRelAttribOnCheckout") + "\">");
		preferencesPageLayout.append("<label>CopyRelAttributesOnCheckout</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - MCADInteg-WarnForFileOverwrite 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-WarnForFileOverwrite") + "\">");
		preferencesPageLayout.append("<label>WarnForFileOverwrite</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - IEF-UserDirectoryAlias-Mode 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-UserDirectoryAlias-Mode") + "\">");
		preferencesPageLayout.append("<label>UserDirectoryAliasMode</label>");
		preferencesPageLayout.append("<uitype>" + COMBO_BOX + "</uitype>");
		preferencesPageLayout.append("<options>Current|Allowed|NotAllowed</options>");
		preferencesPageLayout.append("</preference>");

		//Preference - MCADInteg-CheckOutDirectory 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-CheckOutDirectory") + "\">");
		preferencesPageLayout.append("<label>DefaultCheckoutDirectory</label>");
		preferencesPageLayout.append("<uitype>" + POPUP_LIST + "</uitype>");
		preferencesPageLayout.append("</preference>");     
		
		//Preference - MCADInteg-PreCheckOutEvaluation 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-PreCheckOutEvaluation") + "\">");
		preferencesPageLayout.append("<label>PreCheckoutEvaluation</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - IEF-ImplicitlyIncludeUnrequiredChildren
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-ImplicitlyIncludeUnrequiredChildren") + "\">");
		preferencesPageLayout.append("<label>ImplicitlyIncludeUnrequiredChildren</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		preferencesPageLayout.append("</category>");
		//Category "Checkout" ends

		//Category "Miscellaneous" starts
		preferencesPageLayout.append("<category name=\"Miscellaneous\">");

		//Preference - MCADInteg-SelectChildItems 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-SelectChildItems") + "\">");
		preferencesPageLayout.append("<label>SelectChildrenIfParentIsSelected</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - MCADInteg-UseZipInFileOperations 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-UseZipInFileOperations") + "\">");
		preferencesPageLayout.append("<label>UseZipInFileOperations</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - IEF-EnableProgressiveLoading 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-EnableProgressiveLoading") + "\">");
		preferencesPageLayout.append("<label>EnableProgressiveLoading</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");
 
		//Preference - IEF-Pref-AllowPromoteFloatingDrawing 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-AllowPromoteFloatingDrawing") + "\">");
		preferencesPageLayout.append("<label>AllowPromoteFloatingDrawing</label>");
		preferencesPageLayout.append("<uitype>" + CHECK_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");
 
		//Preference - IEF-DefaultExpandLevel 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-DefaultExpandLevel") + "\">");
		preferencesPageLayout.append("<label>DefaultExpandLevel</label>");
		preferencesPageLayout.append("<uitype>" + EDIT_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

                //Preference - IEF-DefaultFolder 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-DefaultFolder") + "\">");
		preferencesPageLayout.append("<label>DefaultFolder</label>");
		preferencesPageLayout.append("<uitype>" + SEARCH_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - MCADInteg-ViewRegistryName 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_MCADInteg-ViewRegistryName") + "\">");
		preferencesPageLayout.append("<label>RelatedViewRegistry</label>");
		preferencesPageLayout.append("<uitype>" + COMBO_BOX + "</uitype>");
		preferencesPageLayout.append("</preference>");

		//Preference - IEF-DefaultVerticalView 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-DefaultVerticalView") + "\">");
		preferencesPageLayout.append("<label>RelatedVerticalViews</label>");
		preferencesPageLayout.append("<uitype>" + COMBO_BOX  + "</uitype>");		
		preferencesPageLayout.append("</preference>");

		//Preference - IEF-DefaultLateralView 
		preferencesPageLayout.append("<preference name=\"" + MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-DefaultLateralView") + "\">");
		preferencesPageLayout.append("<label>RelatedLateralViews</label>");
		preferencesPageLayout.append("<uitype>" + COMBO_BOX  + "</uitype>");
		preferencesPageLayout.append("</preference>");      

		//Preference - IEF-EBOMSync-ObjectAttrMapping
		preferencesPageLayout.append("<preference name=\"blank\">");
		preferencesPageLayout.append("<label>ObjectAttributeTransferForEBOM</label>");
		preferencesPageLayout.append("<uitype>" + HYPER_LINK + "</uitype>");
		preferencesPageLayout.append("<jponame>IEFAttributeMappingURL</jponame>");
		preferencesPageLayout.append("<jpomethod>getEBOMObjectMappingURL</jpomethod>");
		preferencesPageLayout.append("</preference>");

		//Preference - IEF-EBOMSync-RelAttrMapping
		preferencesPageLayout.append("<preference name=\"blank\">");
		preferencesPageLayout.append("<label>RelationshipAttributeTransferForEBOM</label>");
		preferencesPageLayout.append("<uitype>" + HYPER_LINK + "</uitype>");
		preferencesPageLayout.append("<jponame>IEFAttributeMappingURL</jponame>");
		preferencesPageLayout.append("<jpomethod>getEBOMRelMappingURL</jpomethod>");
		preferencesPageLayout.append("</preference>");

		preferencesPageLayout.append("</category>");
		//Category "Miscellaneous" ends

		preferencesPageLayout.append("</preferences>");

		return preferencesPageLayout.toString();
	}
}


