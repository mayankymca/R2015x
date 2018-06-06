/*
**  DSCBackgroundDerivedOuputPostProcess
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**	Program to unlock the locked objects for backgroundprocess
*/

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.MatrixWriter;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADBrowserLockUnlockHandler;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADXMLUtils;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNodeImpl;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;

public class DSCBackgroundDerivedOutputPostProcess_mxJPO
{
	MatrixWriter _mxWriter		= null;
	String messageBodyAttrValue	= "";

	/**
	 * The no-argument constructor.
	 */
	public DSCBackgroundDerivedOutputPostProcess_mxJPO()
	{

	}

	/**
	 * Constructor which accepts the Matrix context and an array of String
	 * arguments.
	 */
	public DSCBackgroundDerivedOutputPostProcess_mxJPO (Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            throw new Exception("not supported no desktop client");
		_mxWriter	= new MatrixWriter(context);
		messageBodyAttrValue = args[0].trim();
    }

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}
		
	/**
	*
	*/
	public String execute(Context context, String[] args) throws Exception
	{
		String result			= "true";
		boolean isContextPushed = false;
		String retainlock        = null;
		String username			= null;
		List objectIdList	    = new ArrayList();
		String integrationName	= null;

		MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle("");
		MCADMxUtil mxUtil								= new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());	
		
		try
		{
			if(!messageBodyAttrValue.equals(""))
			{
				IEFXmlNode messageBodyPacket = MCADXMLUtils.parse(messageBodyAttrValue, "UTF8");

				 integrationName	= messageBodyPacket.getChildByName("integrationname").getFirstChild().getContent();
				 username			= messageBodyPacket.getChildByName("username").getFirstChild().getContent();
				
				MCADGlobalConfigObject gcoObject	= getGlobalConfigObject(context, integrationName, mxUtil);
				MCADBrowserLockUnlockHandler lockUnlockHandler  = new MCADBrowserLockUnlockHandler(context, gcoObject, mxUtil, integrationName, false);
				
				String[] backgroundUserDetails 	= mxUtil.getBackgroundProcessUserDetails(context, "BackgroundDerivedOutput");
				String backgroundUserName		= backgroundUserDetails[0];
				String backgroundUserPassword	= backgroundUserDetails[1];
			
				if(backgroundUserName != null && !backgroundUserName.equals("") && !backgroundUserName.equals(context.getUser()))
				{
					com.matrixone.apps.domain.util.ContextUtil.pushContext(context, backgroundUserName, backgroundUserPassword, null);
					isContextPushed = true;		            
				}
				
				Enumeration cadObjectList = messageBodyPacket.getChildByName("messagebody").getChildByName("derivedoutputlist").elements();

				while(cadObjectList.hasMoreElements())
				{
					IEFXmlNodeImpl cadObjectNode	 = (IEFXmlNodeImpl)cadObjectList.nextElement();
            
					String mxType		   = "";            
					String cadType           = (String)cadObjectNode.getAttribute("type");
					String name              = (String)cadObjectNode.getAttribute("name");
					String revision          = (String)cadObjectNode.getAttribute("revision");
						  retainlock       = (String)cadObjectNode.getAttribute("retainlock");

					BusinessObject busObject = null;

					Vector mappedMxType		 = gcoObject.getMappedBusTypes(cadType);

					if(mappedMxType != null && mappedMxType.size() > 0)
					{
						mxType	  = mappedMxType.get(0).toString();
						busObject = new BusinessObject(mxType,name,revision, "");

						if(!busObject.exists(context))
						{
							mxType	  = mxUtil.getCorrespondingType(context, mxType);
							busObject = new BusinessObject(mxType,name,revision, "");
						}
					}

					if(busObject != null && busObject.exists(context))
					{
						busObject.open(context);
					
						String busObjectId  = busObject.getObjectId(context);
						
						String isObjLocked	= mxUtil.getMajorObjectLockStatusOfContextUser(context, busObject, gcoObject);
						
						busObject.close(context);
						
						if(isObjLocked.equalsIgnoreCase("true"))
							lockUnlockHandler.doLockUnlock(context, busObjectId, false);

						objectIdList.add(busObjectId);
					}
				}				
			}
		
		}
		catch(Exception me)
        {
			result = "false|"+me.getMessage();
		}
		finally
		{
			if(isContextPushed)
			{ 
				try
				{
					com.matrixone.apps.domain.util.ContextUtil.popContext(context);
				}
				catch(Throwable ex)
				{
					result = "false|"+ex.getMessage();									        					        			
				}
			}
		}

		if(retainlock != null && retainlock.equalsIgnoreCase("true"))
		{
			retainLockForMessageCreator(context, username, objectIdList, integrationName);
		}

		return result;
	}

	private MCADGlobalConfigObject getGlobalConfigObject(Context context, String integrationName, MCADMxUtil mxUtil) throws Exception
    {
		MCADGlobalConfigObject gcoObject	= null;
		
		IEFSimpleConfigObject simpleLCO = IEFSimpleConfigObject.getSimpleLCO(context);

		String gcoType  = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
		String attrName = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");

		Hashtable integNameGCOMapping		= simpleLCO.getAttributeAsHashtable(attrName, "\n", "|");
	    String gcoName						= (String)integNameGCOMapping.get(integrationName);

		MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(null);
		gcoObject							= configLoader.createGlobalConfigObject(context, mxUtil, gcoType, gcoName);

		return gcoObject;
    }

	private void retainLockForMessageCreator(Context context, String username,List objectIdList,String integrationName) throws Exception
    {
		MCADServerResourceBundle serverResourceBundle	= new MCADServerResourceBundle("");
		MCADMxUtil mxUtil								= new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());	
		boolean isContextPushed							= false;

		try
		{
			MCADGlobalConfigObject gcoObject				= getGlobalConfigObject(context, integrationName, mxUtil);				
			MCADBrowserLockUnlockHandler lockUnlockHandler  = new MCADBrowserLockUnlockHandler(context, gcoObject, mxUtil, integrationName, false);

			if(username != null)
			{
				com.matrixone.apps.domain.util.ContextUtil.pushContext(context, username, null, null);
				isContextPushed = true;
			}
			for(int i = 0; i < objectIdList.size(); i++)
			{
				String objId = (String)objectIdList.get(i);
				lockUnlockHandler.doLockUnlock(context, objId, true);
			}			
		}catch(Exception e)
		{
		}
		finally
		{
			if(isContextPushed)
			{
				try
				{
					com.matrixone.apps.domain.util.ContextUtil.popContext(context);
}
				catch(Throwable ex)
				{

				}
			}
		}
    }
}
