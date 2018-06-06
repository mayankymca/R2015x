/**
 * IEFCompareStructureComparisonReport.java
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 */
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;

public class IEFCompareStructureComparisonReport_mxJPO
{
	private MCADServerResourceBundle resourceBundle   = null;
	private IEFGlobalCache cache					  = null;
	private MCADGlobalConfigObject globalConfigObject = null;
	private MCADServerGeneralUtil serverGeneralUtil   = null;

	private Hashtable structureInfoTable			  = null;	
	private Hashtable structure1UniqueInfoTable		  = null;
	private Hashtable structure2UniqueInfoTable		  = null;
	private Hashtable structureCommonInfoTable		  = null;

	private Vector relAttrNames						  = null;

	private String quantityAttrActualName			  = "";
	private String titleAttrActualName				  = "";

	public  IEFCompareStructureComparisonReport_mxJPO  ()
	{
	}

	public IEFCompareStructureComparisonReport_mxJPO (Context context, String[] args) throws Exception
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
			this.globalConfigObject				= (MCADGlobalConfigObject) JPO.unpackArgs(packedGCO);
			this.resourceBundle					= new MCADServerResourceBundle(languageName);
			this.cache							= new IEFGlobalCache();
			this.serverGeneralUtil				= new MCADServerGeneralUtil(context, globalConfigObject, resourceBundle, cache);

			this.structureInfoTable				= new Hashtable();
			this.structure1UniqueInfoTable		= new Hashtable();
			this.structure2UniqueInfoTable		= new Hashtable();
			this.structureCommonInfoTable		= new Hashtable();
			this.relAttrNames					= new Vector(2);

			this.quantityAttrActualName			= MCADMxUtil.getActualNameForAEFData(context,"attribute_Quantity");
			this.titleAttrActualName			= MCADMxUtil.getActualNameForAEFData(context,"attribute_Title");
		}
		catch(Exception e)
		{
			System.out.println("[initialize]: Exception while initializating JPO" + e.getMessage());
			MCADServerException.createException(e.getMessage(), e);
		}
	}

	public Hashtable getStructureComparisionReportData(Context context, String[] args) throws Exception
	{
		String[] packedGCO = new String[2];
		packedGCO[0] = args[0];
		packedGCO[1] = args[1];

		String languageName						= args[2];
		String integrationName					= args[3];
		String structure1ID						= args[4];
		String structure2ID						= args[5];

		initialize(context, packedGCO, languageName);

		try
		{
			String[] validObjectInfoForStructure1	= serverGeneralUtil.getValidObjctIdForCheckout(context, structure1ID);
			String[] validObjectInfoForStructure2	= serverGeneralUtil.getValidObjctIdForCheckout(context, structure2ID);

			structure1ID = validObjectInfoForStructure1[1];
			structure2ID = validObjectInfoForStructure2[1];		

			relAttrNames.addElement(quantityAttrActualName);

			//Collect unique info for Structure1
			String childBusAndRelationshipInfoForStructure1 = serverGeneralUtil.getChildRelsAndIdsForCheckout(context, structure1ID, true, MCADAppletServletProtocol.ASSEMBLY_LIKE, "", "", "", relAttrNames);

			if(!childBusAndRelationshipInfoForStructure1.equals(""))
			{
				Enumeration childDetailsElementsForStructure1	= MCADUtil.getTokensFromString(childBusAndRelationshipInfoForStructure1, "\n");

				while(childDetailsElementsForStructure1.hasMoreElements())
				{
					String individualChildDetails = (String)childDetailsElementsForStructure1.nextElement();

					Enumeration childElements = MCADUtil.getTokensFromString(individualChildDetails, "|");

					String level			= (String)childElements.nextElement();
					String relName			= (String)childElements.nextElement();
					String direction		= (String)childElements.nextElement();
					String childObjectID	= (String)childElements.nextElement();
					String relationshipID	= (String)childElements.nextElement();
					String quantity			= (String)childElements.nextElement();

					BusinessObject busObject = new BusinessObject(childObjectID);
					busObject.open(context);

					String busType          = busObject.getTypeName();
					String busName          = busObject.getName();
					String busRev           = busObject.getRevision();
					String description		= busObject.getDescription(context);
					String title			= busObject.getAttributeValues(context, titleAttrActualName).getValue();
					busObject.close(context);

					if(quantity.equals(""))
						quantity = "1";

					if(description.equals(""))
						description = " ";

					String uniqueKey	= new StringBuffer(busType.trim()).append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE).append(busName.trim()).append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE).append(busRev.trim()).toString();
					String keyValue		= new StringBuffer(description).append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE).append(quantity).append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE).append(title).toString();
					if(!structure1UniqueInfoTable.containsKey(uniqueKey))
					{
						structure1UniqueInfoTable.put(uniqueKey, keyValue);
					}
					else
					{
						//If the current component already exists then add the current qty to the existing qty
						keyValue = (String)structure1UniqueInfoTable.get(uniqueKey);
						StringTokenizer valueTokens = new StringTokenizer(keyValue, MCADAppletServletProtocol.IEF_SEPERATOR_ONE , false);

						//Extract info from the previous component
						String previousDescription	= valueTokens.nextToken();
						Double previousQuantity		= new Double(valueTokens.nextToken());

						String latestQuantity = new Double(new Double(quantity).doubleValue() + previousQuantity.doubleValue()).toString();

						keyValue = new StringBuffer(description).append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE).append(latestQuantity).append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE).append(title).toString();
						structure1UniqueInfoTable.put(uniqueKey, keyValue);
					}
				}
			}

			//Collect unique info for Structure2
			String childBusAndRelationshipInfoForStructure2 = serverGeneralUtil.getChildRelsAndIdsForCheckout(context, structure2ID, true, MCADAppletServletProtocol.ASSEMBLY_LIKE, "", "", "", relAttrNames);
			if(!childBusAndRelationshipInfoForStructure2.equals(""))
			{
				Enumeration childDetailsElementsForStructure2	= MCADUtil.getTokensFromString(childBusAndRelationshipInfoForStructure2, "\n");
				while(childDetailsElementsForStructure2.hasMoreElements())
				{
					String individualChildDetails = (String)childDetailsElementsForStructure2.nextElement();

					Enumeration childElements = MCADUtil.getTokensFromString(individualChildDetails, "|");
					String level			= (String)childElements.nextElement();
					String relName			= (String)childElements.nextElement();
					String direction		= (String)childElements.nextElement();
					String childObjectID	= (String)childElements.nextElement();
					String relationshipID	= (String)childElements.nextElement();
					String quantity			= (String)childElements.nextElement();

					BusinessObject busObject = new BusinessObject(childObjectID);
					busObject.open(context);

					String busType          = busObject.getTypeName();
					String busName          = busObject.getName();
					String busRev           = busObject.getRevision();
					String description		= busObject.getDescription(context);
					String title			= busObject.getAttributeValues(context, titleAttrActualName).getValue();	
					busObject.close(context);

					if(quantity.equals(""))
						quantity = "1";

					if(description.equals(""))
						description = " ";

					String uniqueKey	= new StringBuffer(busType.trim()).append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE).append(busName.trim()).append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE).append(busRev.trim()).toString();
					String keyValue		= new StringBuffer(description).append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE).append(quantity).append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE).append(title).toString(); 
						
					if(!structure2UniqueInfoTable.containsKey(uniqueKey))
					{
						structure2UniqueInfoTable.put(uniqueKey, keyValue);
					}
					else
					{
						//If the current component already exists then add the current qty to the existing qty
						keyValue = (String)structure2UniqueInfoTable.get(uniqueKey);
						StringTokenizer valueTokens = new StringTokenizer(keyValue, MCADAppletServletProtocol.IEF_SEPERATOR_ONE , false);

						//Extract info from the previous component
						String previousDescription	= valueTokens.nextToken();
						Double previousQuantity		= new Double(valueTokens.nextToken());

						String latestQuantity = new Double(new Double(quantity).doubleValue() + previousQuantity.doubleValue()).toString();

						keyValue = new StringBuffer(description).append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE).append(latestQuantity).append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE).append(title).toString(); 
						structure2UniqueInfoTable.put(uniqueKey, keyValue);
					}
				}
			}

			//Collect common info for both the Structures
			if(!structure1UniqueInfoTable.isEmpty())
			{
				Enumeration structure1Keys = structure1UniqueInfoTable.keys();
				while(structure1Keys.hasMoreElements())
				{
					String structure1Key = (String)structure1Keys.nextElement();
					if (structure2UniqueInfoTable.containsKey(structure1Key))
					{
						String structure1Value = (String)structure1UniqueInfoTable.get(structure1Key);

						String structure2Value = (String)structure2UniqueInfoTable.get(structure1Key);
						StringTokenizer structure2ValueTokens	= new StringTokenizer(structure2Value, MCADAppletServletProtocol.IEF_SEPERATOR_ONE, false);
						String structure2ComponentDescription	= structure2ValueTokens.nextToken();
						String structure2ComponentQuantity		= structure2ValueTokens.nextToken();
						String structure2ComponentTitle			= structure2ValueTokens.nextToken();

						String commonTableValue = structure1Value + MCADAppletServletProtocol.IEF_SEPERATOR_ONE + structure2ComponentQuantity;
						structureCommonInfoTable.put(structure1Key, commonTableValue);

						structure1UniqueInfoTable.remove(structure1Key);
						structure2UniqueInfoTable.remove(structure1Key);
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("[initialize]: Exception while initializating JPO IEFCompareStructureComparisonReport" + e.getMessage());
			MCADServerException.createException(e.getMessage(), e);
		}

		structureInfoTable.put("structure1UniqueInfoTable", structure1UniqueInfoTable);
		structureInfoTable.put("structure2UniqueInfoTable", structure2UniqueInfoTable);
		structureInfoTable.put("structureCommonInfoTable", structureCommonInfoTable);

		return structureInfoTable;
	}
}

