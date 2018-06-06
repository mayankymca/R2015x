/*
 **  DSC_CommonUtilBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Class defining basic infrastructure, contains common data members required
 **  for executing any IEF related actions.
 */

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;

public class DSC_CommonUtilBase_mxJPO
{
	public static final String DELIMITER						= "${}$";
	public static final String PROP_SEP							= "~";
	public static int count										= 0; 

	public  DSC_CommonUtilBase_mxJPO ()
	{
	}

	public DSC_CommonUtilBase_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
		{
			MCADServerException.createException("not supported no desktop client", null);
		}
	}

	public int mxMain(Context context, String []args)  throws Exception
	{  
		return 0;
	}

	public Map parentRelationshipExists(Context context, String[] args)
	{	
		HashMap objectMap = new HashMap();
		try
		{	
			HashMap uploadParamsMap = (HashMap)JPO.unpackArgs(args);
			String objectId			= (String)uploadParamsMap.get("objectId");
			String parentId			= (String)uploadParamsMap.get("parentId");

			if (null == objectId || null == parentId || 0 == objectId.length() || 0 == parentId.length())
			{
				objectMap.put("result", "false");
				return objectMap;
			}

			String relName			= (String)uploadParamsMap.get("relName");

			// relationship: "Vaulted Objects"
			relName = PropertyUtil.getSchemaProperty(context, "relationship_VaultedDocuments");
			DomainObject object		= DomainObject.newInstance(context, objectId);
			String selectParentId	= "last.to[" + relName + "].from.id";
			StringList selects		= new StringList(2);

			selects.add(DomainConstants.SELECT_ID);
			selects.add(selectParentId);

			Map objectSelectMap		= object.getInfo(context,selects);
			String foundParentId	= (String)objectSelectMap.get(selectParentId);

			if (null != foundParentId && foundParentId.equals(parentId))
			{
				objectMap.put("result", "true");
				System.out.println("foundParentId = " + foundParentId);
			}
			else
			{
				objectMap.put("result", "false");
			}
		}
		catch (Exception e)
		{
			System.out.println(e.toString());
			objectMap.put("result", "false");
			e.printStackTrace();
		}

		return objectMap;
	}

	public String getPersonProperty(Context context, String propName)
	{	
		try
		{	
			matrix.db.MQLCommand mql = new matrix.db.MQLCommand();
			mql.open(context);
			String personName	= context.getUser();
			mql.executeCommand(context, "print Person $1 select $2",personName,"property[" + propName + "]");
			String result		= mql.getResult();  

			if (result != null && result.length() >= 0)
			{
				int pos = result.indexOf("value");
				if (pos < 0) return "";
				result	= result.substring(pos+5, result.length());
				result	= result.trim();
			}
			else
			{
				result = "";
			}
			mql.close(context);
			return result;
		}
		catch (Exception e)
		{
			System.out.println(e.toString());
			return "";
		}   
	}

	public Map getDefaultIntegrationAssignment(Context context, String[] args) throws Exception
	{
		HashMap integMap = new HashMap();
		try
		{
			HashMap paramMap = (HashMap)JPO.unpackArgs(args);
			if (null == paramMap)
			{
				return integMap;
			}
			MCADLocalConfigObject localConfigObject = (MCADLocalConfigObject)paramMap.get("LCO");
			if (null == localConfigObject)
			{
				return integMap;
			}
			// find the Integration Mapping in user's Local Config Object
			Hashtable integrationMapping = localConfigObject.getIntegrationNameGCONameMapping();

			Enumeration itr = integrationMapping.keys();
			while (itr.hasMoreElements())
			{       
				String sIntegName = (String)itr.nextElement();
				String sIntegArc     = (String)integrationMapping.get(sIntegName);
				integMap.put("integrationName", sIntegName);
				integMap.put("integrationArc", sIntegArc);
				break;
			}
		}
		catch (Exception e)
		{
			System.out.println("getDefaultIntegrationAssigment: Error " + e.toString());
		}
		return integMap;
	}

	public MapList getUserIntegrationAssignments(Context context, String[] args) throws Exception
	{
		HashMap integMap = null;
		MapList integrationList = new MapList();
		try
		{
			HashMap paramMap = (HashMap)JPO.unpackArgs(args);
			if (null == paramMap) 
			{
				return integrationList;
			}
			MCADLocalConfigObject localConfigObject = (MCADLocalConfigObject)paramMap.get("LCO");
			if (null == localConfigObject)
			{
				return integrationList;
			}
			// find the Integration Mapping in user's Local Config Object
			Hashtable integrationMapping = localConfigObject.getIntegrationNameGCONameMapping();

			Enumeration itr = integrationMapping.keys();
			while (itr.hasMoreElements())
			{       
				String sIntegName = (String)itr.nextElement();
				String sIntegArc     = (String)integrationMapping.get(sIntegName);
				integMap = new HashMap();
				integMap.put("integrationName", sIntegName);
				integMap.put("integrationArc", sIntegArc);
				integrationList.add(integMap);
			}
		}
		catch (Exception e)
		{
			System.out.println("getDefaultIntegrationAssigment: Error " + e.toString());
		}
		return integrationList;
	}

	public MapList getInstalledApplications(Context context, String[] args) throws Exception
	{
		MapList appList = new MapList();
		try
		{
			String Result = "";
			String sErrorCode = "";
			String prMQLString;
			MQLCommand prMQL  = new MQLCommand();
			prMQL.open(context);
			prMQL.executeCommand(context,"execute program $1","eServiceHelpAbout.tcl");
			Result = prMQL.getResult().trim();
			String error = prMQL.getError();
			StringBuffer strBuff  = new StringBuffer();

			StringTokenizer token = new StringTokenizer(Result, "|", false);
			sErrorCode = token.nextToken().trim();//first token
			if( sErrorCode.equals("1"))//internal failure of tcl program
			{
				token.nextToken().trim();//second token
			}

			while (token.hasMoreTokens())
			{
				HashMap appMap = new HashMap();

				appMap.put("name", token.nextToken().trim()); //will store name of application
				appMap.put("build", token.nextToken().trim()); //will have build number
				appList.add(appMap);
			}
		}
		catch(Exception e)
		{
			System.out.println("getInstalledApplications: " + e.toString());
		}
		return appList;
	}

	public HashMap getCommonDocumentAttributes(Context context, String objectId)
	{
		HashMap objectMap = new HashMap();       
		try
		{ 
			String attrMoveFilesToVersion		= MCADMxUtil.getActualNameForAEFData(context, "attribute_MoveFilesToVersion");
			String attrIsVersionObject			= MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
			String attrSource 				= MCADMxUtil.getActualNameForAEFData(context, "attribute_Source");
			matrix.db.MQLCommand mql = new matrix.db.MQLCommand();
			mql.open(context);            
			mql.executeCommand(context, "print bus $1 select $2 dump",objectId,"attribute[" + attrIsVersionObject + "]");
			String result = mql.getResult();  

			if (result != null && result.length() >= 0)
			{
				result	= result.trim();                
				objectMap.put(attrIsVersionObject, result);
			}

			mql.executeCommand(context, "print bus $1 select $2 dump",objectId,"attribute[" + attrMoveFilesToVersion + "]");

			result = mql.getResult();  
			if (result != null && result.length() >= 0)
			{
				result	= result.trim();
				objectMap.put(attrMoveFilesToVersion, result);
			}
			mql.executeCommand(context, "print bus $1 select $2 dump",objectId,"attribute[" + attrSource + "]");

			result = mql.getResult();  
			if (result != null && result.length() >= 0)
			{
				result	= result.trim();
				objectMap.put(attrSource, result);
			}
			mql.close(context);
		}
		catch (Exception e)
		{
			System.out.println("CDMSupport.getCommonDocumentAttributes: " + e.toString());
		}
		return objectMap;
	}
	
	public String getApplicationName(Context context, String[] args) throws Exception
	{
		String sValue = null;
		try
		{
			String sGivenProdName = args[0];
			if(sGivenProdName!= null && !"".equals(sGivenProdName)){
				sValue = EnoviaResourceBundle.getProperty(context,"emxSystem",context.getLocale(),"emxFramework.HelpAbout."+sGivenProdName);	
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw e;
		}

		return sValue;
	}	

}

