/*
**  IEFGetRegistrationDetails
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  JPO to get registered integration details
*/
import java.util.Enumeration;

import matrix.db.BusinessObject;
import matrix.db.Context;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADXMLUtils;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNodeImpl;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;

public class IEFGetRegistrationDetails_mxJPO
{
	public IEFGetRegistrationDetails_mxJPO(Context context, String[] args) throws Exception
	{

	}

	public String getRegistrationDetails(Context context, String[] args)
	{
		StringBuffer registrationDetails = new StringBuffer();

		try
		{
			String integrationName = args[0];
			String typeIEFGlobalRegistry = MCADMxUtil.getActualNameForAEFData(context, "type_IEF-GlobalRegistry");
			String attrIEFRegistryData 	 = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-RegistryData");

			BusinessObject inputObject	= new BusinessObject(typeIEFGlobalRegistry, typeIEFGlobalRegistry, "-", "");
			inputObject.open(context);

			MCADMxUtil util				 = new MCADMxUtil(context, new MCADServerResourceBundle(""), new IEFGlobalCache());
			String registryDataAttrValue = util.getAttributeForBO(context,inputObject.getObjectId(), attrIEFRegistryData);

			if(registryDataAttrValue != null && !registryDataAttrValue.trim().equals(""))
			{
				IEFXmlNode integrationsRegistryNode	= MCADXMLUtils.parse(registryDataAttrValue, "UTF8");
				IEFXmlNodeImpl registeredIntegrationNode = getIntegrationNode(integrationsRegistryNode, integrationName);

				if(registeredIntegrationNode != null)
				{
					String registeredIntegrationName	= registeredIntegrationNode.getChildByName("name").getFirstChild().getContent();
					registrationDetails.append(registeredIntegrationName + "|");

					String registeredIntegrationVersion	= registeredIntegrationNode.getChildByName("version").getFirstChild().getContent();
					registrationDetails.append(registeredIntegrationVersion + "|");

					String registeredIntegrationDate	= registeredIntegrationNode.getChildByName("date").getFirstChild().getContent();
					registrationDetails.append(registeredIntegrationDate + "|");

					String registeredDefaultGCOName 	= registeredIntegrationNode.getChildByName("defaultGCOName").getFirstChild().getContent();
					registrationDetails.append(registeredDefaultGCOName);

				}
			}

			inputObject.close(context);
		}
		catch( Exception ex )
        {
			System.out.println("[IEFGetRegistrationDetails:getRegistrationDetails] Exception : " + ex.getMessage());
		}

		return registrationDetails.toString();
	}

	private IEFXmlNodeImpl getIntegrationNode(IEFXmlNode integrationsRegistryNode, String integrationName) throws Exception
	{
		IEFXmlNodeImpl integrationNode = null;

		Enumeration registeredIntegrationsList = integrationsRegistryNode.elements();
		while(registeredIntegrationsList.hasMoreElements())
		{
			IEFXmlNodeImpl registeredIntegrationNode	= (IEFXmlNodeImpl)registeredIntegrationsList.nextElement();

			String registeredIntegrationName	= registeredIntegrationNode.getChildByName("name").getFirstChild().getContent();
			if(integrationName.equalsIgnoreCase(registeredIntegrationName))
			{
				integrationNode = registeredIntegrationNode;
				break;
			}
		}

		return integrationNode;
	}
}

