/*
 **  DSC_BackgroundDepDocMessageCreation
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 ** Program to create DSCMessage for Background Derived Output Operation.
 */

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCMessage;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCQueue;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNodeImpl;

public class DSC_BackgroundDepDocMessageCreation_mxJPO
{
	private MCADMxUtil _util								= null;
	private MCADServerResourceBundle _serverResourceBundle	= null;
	private IEFGlobalCache _cache							= null;
	private MCADGlobalConfigObject gco						= null;

	private HashMap objIDSelBackgroundDepDocTable 			= new HashMap();
	private HashMap objIDBackgroundCheckoutDetailsMap		= new HashMap();
	private String integrationName 							= null;
	private String operationName							= null;
	private String retainLock								= null;

	public DSC_BackgroundDepDocMessageCreation_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			throw new Exception("not supported no desktop client");
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	public void createMessageObject(Context context, String []args)  throws Exception
	{
		try
		{
			HashMap hashMapArgs						= (HashMap) JPO.unpackArgs(args);

			this.integrationName					= (String)hashMapArgs.get("integrationName");
			this.operationName						= (String)hashMapArgs.get("operationName");
			this.retainLock							= (String)hashMapArgs.get("retainlock");			
			this.objIDSelBackgroundDepDocTable		= (HashMap)hashMapArgs.get("objIDSelBackgroundDepDocTable");
			this.objIDBackgroundCheckoutDetailsMap	= (HashMap)hashMapArgs.get("objIDBackgroundCheckoutDetailsMap");
			this.gco								= (MCADGlobalConfigObject)hashMapArgs.get("GCO");

			String languageName						= (String) hashMapArgs.get("language");

			this._serverResourceBundle				= new MCADServerResourceBundle(languageName);

			this._cache								= new IEFGlobalCache();
			this._util								= new MCADMxUtil(context, _serverResourceBundle, _cache);

			String personSiteName					= getPersonSiteName(context);
			String queueName 						= null;

			DSC_GetBatchProcessorDetails_mxJPO batchProcessorDetails = new DSC_GetBatchProcessorDetails_mxJPO();

			if(personSiteName == null || personSiteName.trim().equals(""))
			{
				String[] args1	= new String[1];
				args1[0]		= operationName;

				queueName		= batchProcessorDetails.getDefaultSiteBatchOperationQueueName(context, args1);
			}
			else
			{
				try
				{
					String[] args2 	= new String[2];
					args2[0]		= operationName;
					args2[1]		= personSiteName;

					queueName = batchProcessorDetails.getBatchOperationQueueNameForSite(context, args2);
				}
				catch(Exception exception)
				{
					Hashtable exceptiontable = new Hashtable(1);
					exceptiontable.put("OPERATION",operationName);
					MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToGetBatchOperationQueueName", exceptiontable), exception);
				}
			}

			DSCQueue queue = new DSCQueue(context, MCADMxUtil.getActualNameForAEFData(context, "type_DSCQueue"), queueName);

			if(!queue.exists(context))
			{
				queue.create(context, MCADMxUtil.getActualNameForAEFData(context, "policy_DSCQueuePolicy"));
			}

			queue.open(context);

			String uniqueName 	= getUniqueNameForMessage();	
			String messageBody 	= createMessageBody(context, uniqueName);

			DSCMessage messageObject	= new DSCMessage(context, uniqueName, messageBody);

			if(!messageObject.exists(context))
			{
				messageObject.create(context, MCADMxUtil.getActualNameForAEFData(context,"policy_DSCMessagePolicy"));
			}

			_util.setAttributeOnBusObject(context, messageObject, MCADMxUtil.getActualNameForAEFData(context, "attribute_Title"), operationName);
			_util.setAttributeOnBusObject(context, messageObject, MCADMxUtil.getActualNameForAEFData(context, "attribute_DSCPostProcessAction"), "DSCBackgroundDerivedOutputPostProcess");
			_util.setAttributeOnBusObject(context, messageObject, MCADMxUtil.getActualNameForAEFData(context, "attribute_DSCPostDeleteAction"), "DSCBackgroundDerivedOutputPostDelete");

			messageObject.open(context);
			messageObject.setBody(context, messageBody);
			messageObject.setPriority(context, "1");
			messageObject.close(context);

			queue.addPendingMessage(context, messageObject);
			queue.close(context);
		}
		catch(Exception e)
		{
			MCADServerException.createManagedException("IEF0133200103", _serverResourceBundle.getString("mcadIntegration.Server.Message.IEF0133200103"), e);
		}
	}

	private String getUniqueNameForMessage()
	{		
		//Format the date time stamp
		Calendar calendar 			= Calendar.getInstance();
		String timePattern 			= "yyyy.MM.dd_HH.mm.ss";
		SimpleDateFormat formatter 	= (SimpleDateFormat)DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
		formatter.applyPattern(timePattern);

		String timeStamp 	= formatter.format(calendar.getTime());
		String uniqueName 	= timeStamp;

		return uniqueName;
	}

	private String createMessageBody(Context context, String messageName) throws Exception
	{
		IEFXmlNodeImpl messageNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		messageNode.setName("message");

		IEFXmlNodeImpl messageNameNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		messageNameNode.setName("messagename");
		messageNameNode.setContent(messageName);
		messageNode.addNode(messageNameNode);

		IEFXmlNodeImpl operationNameNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		operationNameNode.setName("operationname");
		operationNameNode.setContent(operationName);
		messageNode.addNode(operationNameNode);

		IEFXmlNodeImpl integrationNameNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		integrationNameNode.setName("integrationname");
		integrationNameNode.setContent(integrationName);
		messageNode.addNode(integrationNameNode);

		IEFXmlNodeImpl userNameNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		userNameNode.setName("username");
		userNameNode.setContent(context.getUser());
		messageNode.addNode(userNameNode);

		IEFXmlNodeImpl messageBodyNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		messageBodyNode.setName("messagebody");
		messageNode.addNode(messageBodyNode);

		IEFXmlNodeImpl checkoutListNode	= new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		checkoutListNode.setName("checkoutlist");
		messageBodyNode.addNode(checkoutListNode);

		IEFXmlNodeImpl derivedOutputListNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		derivedOutputListNode.setName("derivedoutputlist");
		messageBodyNode.addNode(derivedOutputListNode);

		//Populate CheckoutList Node
		java.util.Set checkoutListKeys = objIDBackgroundCheckoutDetailsMap.keySet();
		Iterator checkoutListIter = checkoutListKeys.iterator();
		while(checkoutListIter.hasNext())
		{
			IEFXmlNodeImpl cadObjectNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
			cadObjectNode.setName("cadobject");
			checkoutListNode.addNode(cadObjectNode);

			String objectId 			= checkoutListIter.next().toString();
			String[] checkoutObjDetails = (String[])objIDBackgroundCheckoutDetailsMap.get(objectId);

			Hashtable cadObjectAttributes = new Hashtable();			
			cadObjectAttributes.put("type", checkoutObjDetails[0]);
			cadObjectAttributes.put("name", checkoutObjDetails[1]);
			cadObjectAttributes.put("revision", checkoutObjDetails[2]);
			cadObjectAttributes.put("revisedfrom", checkoutObjDetails[3]);
			cadObjectNode.setAttributes(cadObjectAttributes);
		}

		//Populate DerivedOutputList Node
		java.util.Set derivedOutputListKeys 	= objIDSelBackgroundDepDocTable.keySet();
		Iterator derivedOutputListIter 			= derivedOutputListKeys.iterator();
		while(derivedOutputListIter.hasNext())
		{
			IEFXmlNodeImpl cadObjectNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
			cadObjectNode.setName("cadobject");
			derivedOutputListNode.addNode(cadObjectNode);

			String objectId 				= derivedOutputListIter.next().toString();
			String[] bgDepDocDetails 	= (String[])objIDSelBackgroundDepDocTable.get(objectId);

			Hashtable cadObjectAttributes = new Hashtable();
			cadObjectAttributes.put("type", bgDepDocDetails[0]);
			cadObjectAttributes.put("name", bgDepDocDetails[1]);
			cadObjectAttributes.put("revision", bgDepDocDetails[2]);
			cadObjectAttributes.put("seldepdoc", bgDepDocDetails[3]);

			if(!bgDepDocDetails[4].equals("") && !gco.isUniqueInstanceNameInDBOn())
				cadObjectAttributes.put("familyName", bgDepDocDetails[4]);

			cadObjectAttributes.put("retainlock", retainLock);	

			cadObjectNode.setAttributes(cadObjectAttributes);
		}

		return messageNode.getXmlString();
	}

	private String getPersonSiteName(Context context) throws MCADException
	{
		String personSite	= null;
		String Args[] = new String[1];
		Args[0] = context.getUser();
		String mqlResult	= _util.executeMQL(context ,"print person $1 select site dump", Args);

		if(mqlResult.startsWith("true|"))
			personSite = mqlResult.substring(mqlResult.indexOf("|")+1, mqlResult.length());
		else
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FailedToGetSiteName") + mqlResult, null);

		return personSite;
	}

}

