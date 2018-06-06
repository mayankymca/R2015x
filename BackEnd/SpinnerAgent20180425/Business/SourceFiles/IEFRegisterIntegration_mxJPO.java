/*
**  IEFRegisterIntegration
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  JPO to register integration to IEF
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

public class IEFRegisterIntegration_mxJPO
{
	public IEFRegisterIntegration_mxJPO(Context context, String[] args) throws Exception
	{

	}

	public void registerIntegrationToIEF(Context context, String[] args) throws Exception
	{
		try
		{
			String integrationName		= args[0];
			String integrationVersion	= args[1];
			String installedDate		= args[2];
			String defaultGCOName		= args[3];

			String typeIEFGlobalRegistry = MCADMxUtil.getActualNameForAEFData(context, "type_IEF-GlobalRegistry");
			String attrIEFRegistryData 	 = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-RegistryData");

			MCADMxUtil util = new MCADMxUtil(context, new MCADServerResourceBundle(""), new IEFGlobalCache());

			BusinessObject inputObject	= new BusinessObject(typeIEFGlobalRegistry, typeIEFGlobalRegistry, "-", "");
			inputObject.open(context);

			String registryDataAttrValue = util.getAttributeForBO(context, inputObject.getObjectId(), attrIEFRegistryData);

			IEFXmlNodeImpl integrationsRegistryNode = null;
			if(registryDataAttrValue == null || registryDataAttrValue.trim().equals(""))
			{
				integrationsRegistryNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
				integrationsRegistryNode.setName("integrationregistry");
			}
			else
			{
				integrationsRegistryNode = (IEFXmlNodeImpl)MCADXMLUtils.parse(registryDataAttrValue, "UTF8");

				IEFXmlNodeImpl registeredIntegrationNode = getIntegrationNode(integrationsRegistryNode, integrationName);
				if(registeredIntegrationNode != null)
				{
					integrationsRegistryNode.deleteChild(registeredIntegrationNode);
				}
			}

			IEFXmlNodeImpl integrationNode = createIntegrationNode(integrationName, integrationVersion, installedDate, defaultGCOName);
			integrationsRegistryNode.addNode(integrationNode);

			String updatedRegistryDataString = integrationsRegistryNode.getXmlString();
			updatedRegistryDataString = updatedRegistryDataString.replace('\"', '\'');

			inputObject.open(context);
			util.setAttributeOnBusObject(context, inputObject, attrIEFRegistryData, updatedRegistryDataString);

			inputObject.close(context);
		}
		catch( Exception ex )
        {
			System.out.println("[IEFRegisterIntegration:registerIntegrationToIEF]Exception:"+ex.getMessage());
		}
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

	private IEFXmlNodeImpl createIntegrationNode(String integrationName, String integrationVersion, String installedDate, String defaultGCOName) throws Exception
	{
		// create node to hold this attr name value pair
        IEFXmlNodeImpl integrationNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
        integrationNode.setName("integration");

        IEFXmlNodeImpl nameNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
        nameNode.setName("name");
        IEFXmlNodeImpl nameContentNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.CDSECTION);
        nameContentNode.setContent(integrationName);
        nameNode.addNode(nameContentNode);
        integrationNode.addNode(nameNode);

        IEFXmlNodeImpl versionNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
        versionNode.setName("version");
        IEFXmlNodeImpl versionContentNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.CDSECTION);
        versionContentNode.setContent(integrationVersion);
        versionNode.addNode(versionContentNode);
        integrationNode.addNode(versionNode);

		IEFXmlNodeImpl dateNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
        dateNode.setName("date");
        IEFXmlNodeImpl dateContentNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.CDSECTION);
        dateContentNode.setContent(installedDate);
        dateNode.addNode(dateContentNode);
        integrationNode.addNode(dateNode);

		IEFXmlNodeImpl defaultGCONode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
        defaultGCONode.setName("defaultGCOName");
        IEFXmlNodeImpl defaultGCOContentNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.CDSECTION);
        defaultGCOContentNode.setContent(defaultGCOName);
        defaultGCONode.addNode(defaultGCOContentNode);
        integrationNode.addNode(defaultGCONode);

        return integrationNode;
	}



	public void registerRoleWithIntegration( Context context,  String[] args) throws Exception
	{
                                                String integrationName		  = args[0];
			String defaultRoleName	                  = args[1];
			String defaultGCOName		  = args[2];

		MCADMxUtil util = new MCADMxUtil(context, new MCADServerResourceBundle(""), new IEFGlobalCache());

		String Args[] = new String[5];
		Args[0] = defaultRoleName;
		Args[1] = "property";
		Args[2] = "IEF-IntegrationAssignments";
		Args[3] = "value";
		Args[4] = integrationName+"~"+defaultGCOName;

		String result = util.executeMQL(context,"modify role $1 $2 $3 $4 $5", Args);

		return;
	}


}

