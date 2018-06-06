/*
**  IEFGetRegisteredIntegrations
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  JPO to get integrations registered to IEF
*/
import java.util.Enumeration;

import matrix.db.BusinessObject;
import matrix.db.Context;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADXMLUtils;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;

public class IEFGetRegisteredIntegrations_mxJPO
{
	public IEFGetRegisteredIntegrations_mxJPO(Context context, String[] args) throws Exception
	{

	}

	public String getRegisteredIntegrations(Context context, String[] args)
	{
		StringBuffer registeredIntegrations = new StringBuffer();

		try
		{
			String typeIEFGlobalRegistry = MCADMxUtil.getActualNameForAEFData(context, "type_IEF-GlobalRegistry");
			String attrIEFRegistryData 	 = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-RegistryData");
			BusinessObject inputObject	= new BusinessObject(typeIEFGlobalRegistry, typeIEFGlobalRegistry, "-", "");
			inputObject.open(context);

			MCADMxUtil util				 = new MCADMxUtil(context, new MCADServerResourceBundle(""), new IEFGlobalCache());
			String registryDataAttrValue = util.getAttributeForBO(context,inputObject.getObjectId(), attrIEFRegistryData);

			if(registryDataAttrValue != null && !registryDataAttrValue.trim().equals(""))
			{
				IEFXmlNode integrationsRegistryNode	= MCADXMLUtils.parse(registryDataAttrValue, "UTF8");

				Enumeration registeredIntegrationList = integrationsRegistryNode.elements();
				while(registeredIntegrationList.hasMoreElements())
				{
					IEFXmlNode registeredIntegrationNode	= (IEFXmlNode)registeredIntegrationList.nextElement();
					String registeredIntegrationName	= registeredIntegrationNode.getChildByName("name").getFirstChild().getContent();

					registeredIntegrations.append(registeredIntegrationName + "|");
				}
			}

			inputObject.close(context);
		}
		catch( Exception ex )
        {
			System.out.println("[IEFGetRegisteredIntegrations:getRegisteredIntegrations]Exception:"+ex.getMessage());
		}

		return registeredIntegrations.toString();
	}

}

