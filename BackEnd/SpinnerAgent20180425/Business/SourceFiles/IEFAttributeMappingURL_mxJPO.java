/***
IEFAttributeMappingURL.java
****/

import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;

public class IEFAttributeMappingURL_mxJPO
{
	public IEFAttributeMappingURL_mxJPO ()
	{
	}
	public IEFAttributeMappingURL_mxJPO (Context context, String args[] ) throws Exception
	{
	}
	public int mxMain(Context context, String []args) throws Exception
	{
		return 0;
	}

	public String getEBOMObjectMappingURL(Context context, String args[])
	{
		String mandAttrName			= "";
		String attrName				= "";
		String prefConfigObjectID	= "";
		String mandConfigObjectID	= "";
		String integrationName		= "";
		
		String heading	= "mcadIntegration.Server.Heading.AttributeTransfer";
		String url		= "IEFAttributeTransferDialogFS.jsp?pageHeading=" + heading + "&integrationName=";
		
		MCADGlobalConfigObject globalConfigObject = null;
		MCADLocalConfigObject localConfigObject   = null;

		try
		{
			Hashtable gcoTable	= (Hashtable)JPO.unpackArgs(args);
			
			integrationName		= (String)gcoTable.get("integrationName");
			HashMap GCOMap		= (HashMap)gcoTable.get("GCOTable");
			localConfigObject	= (MCADLocalConfigObject)gcoTable.get("lco");

			attrName = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMSync-ObjectAttrMapping");
			
			prefConfigObjectID = localConfigObject.getObjId();
			globalConfigObject = (MCADGlobalConfigObject)GCOMap.get(integrationName);
				
			String sEBOMRegistryTNR = globalConfigObject.getEBOMRegistryTNR();
			
			StringTokenizer token = new StringTokenizer(sEBOMRegistryTNR, "|");
			if(token.countTokens() >= 3)
			{
				String sEBOMRConfigObjType			= (String) token.nextElement();
				String sEBOMRConfigObjName			= (String) token.nextElement();
				String sEBOMRConfigObjRev			= (String) token.nextElement();
				IEFEBOMConfigObject ebomConfig		= new IEFEBOMConfigObject(context,sEBOMRConfigObjType, sEBOMRConfigObjName, sEBOMRConfigObjRev);
				BusinessObject busObj = new BusinessObject(sEBOMRConfigObjType, sEBOMRConfigObjName, sEBOMRConfigObjRev, "");
				mandConfigObjectID	  = busObj.getObjectId(context);
			} 			
			
		} 
		catch(Exception e)
		{
			
		}
		
		StringBuffer retValue 	= new StringBuffer();		
		mandAttrName		 	= IEFEBOMConfigObject.ATTR_MAND_OBJECT_ATTR_MAPPING;
		
		String mandAttrInfo 	= mandConfigObjectID + ":" + mandAttrName;
		String prefAttrInfo 	= prefConfigObjectID + ":" + attrName;
		
		retValue.append(url);
		retValue.append(integrationName);
		retValue.append("&prefAttrInfo=");
		retValue.append(prefAttrInfo);
		retValue.append("&mandAttrInfo=");
		retValue.append(mandAttrInfo);
		retValue.append("&toType=type_Part");
		retValue.append("&submitUrl=IEFAttributeTransferUpdate.jsp&filterProgram=MCADTransferAttributesForEBOM:getValidObjectAttributeMapping&isBusMapping=true&validationURL=IEFAttributeMappingValidate.jsp");

		return retValue.toString();
	}

	public String getEBOMRelMappingURL(Context context, String args[])
	{
		String mandAttrName			= "";
		String attrName				= "";
		String prefConfigObjectID	= "";
		String mandConfigObjectID	= "";
		String integrationName		= "";
		String heading				= "mcadIntegration.Server.Heading.AttributeTransfer";
		String url					= "IEFAttributeTransferDialogFS.jsp?pageHeading=" + heading + "&integrationName=";
		
		MCADGlobalConfigObject globalConfigObject	= null;
		MCADLocalConfigObject localConfigObject		= null;
				
		try
		{
			Hashtable gcoTable			= (Hashtable)JPO.unpackArgs(args);

			integrationName		= (String)gcoTable.get("integrationName");
			HashMap GCOMap		= (HashMap)gcoTable.get("GCOTable");
			localConfigObject	= (MCADLocalConfigObject)gcoTable.get("lco");
			
			attrName			  = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-EBOMSync-RelAttrMapping");

			prefConfigObjectID = localConfigObject.getObjId();
			globalConfigObject = (MCADGlobalConfigObject)GCOMap.get(integrationName);
			
			String sEBOMRegistryTNR = globalConfigObject.getEBOMRegistryTNR();
			
			StringTokenizer token = new StringTokenizer(sEBOMRegistryTNR, "|");
			if(token.countTokens() >= 3)
			{
				String sEBOMRConfigObjType	= (String) token.nextElement();
				String sEBOMRConfigObjName	= (String) token.nextElement();
				String sEBOMRConfigObjRev	= (String) token.nextElement();
				IEFEBOMConfigObject ebomConfig		= new IEFEBOMConfigObject(context,sEBOMRConfigObjType, sEBOMRConfigObjName, sEBOMRConfigObjRev);
				BusinessObject busObj	= new BusinessObject(sEBOMRConfigObjType, sEBOMRConfigObjName, sEBOMRConfigObjRev, "");
				mandConfigObjectID		= busObj.getObjectId(context);
			}
		} 
		catch(Exception e)
		{
			
		}
		
		StringBuffer retValue = new StringBuffer();		
		mandAttrName		  = IEFEBOMConfigObject.ATTR_MAND_REL_ATTR_MAPPING;

		String mandAttrInfo = mandConfigObjectID + ":" + mandAttrName;
		String prefAttrInfo = prefConfigObjectID + ":" + attrName;
		
		retValue.append(url);
		retValue.append(integrationName);
		retValue.append("&prefAttrInfo=");
		retValue.append(prefAttrInfo);
		retValue.append("&mandAttrInfo=");
		retValue.append(mandAttrInfo);
		retValue.append("&submitUrl=IEFAttributeTransferUpdate.jsp&filterProgram=MCADTransferAttributesForEBOM:getValidRelAttributeMapping&isBusMapping=false&validationURL=IEFAttributeMappingValidate.jsp");

		return retValue.toString();
	}
}

