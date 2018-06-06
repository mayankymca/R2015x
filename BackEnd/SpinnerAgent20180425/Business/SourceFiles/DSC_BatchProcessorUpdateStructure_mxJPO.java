/*
 **  DSC_BatchProcessorUpdateStructure
 **
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 ** Program to create DSCMessage for Background Update Structure. 
 */

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.db.RelationshipItr;
import matrix.db.RelationshipList;
import matrix.db.RelationshipType;
import matrix.util.MatrixException;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCBackgroundProcessorUtil;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCMessage;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCQueue;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNodeImpl;



public class DSC_BatchProcessorUpdateStructure_mxJPO
{
	private MCADMxUtil _util								= null;
	private MCADServerResourceBundle _serverResourceBundle	= null;
	private IEFGlobalCache _cache							= null;
	private MCADGlobalConfigObject gco						= null;

	private String integrationName 							= null;
	private String operationName							= null;
	private String relCADSubComponent                       = null;

	
	
	public DSC_BatchProcessorUpdateStructure_mxJPO (Context context, String[] args) throws Exception
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

			String type                             = (String) hashMapArgs.get("type"); 
			String name                             = (String) hashMapArgs.get("name");
			String rev                              = (String) hashMapArgs.get("rev");
			String ver                              = (String) hashMapArgs.get("ver");
			String languageName						= (String) hashMapArgs.get("language");
			String activeMessageRelationship        = (String) hashMapArgs.get("activemessagerel");


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

			boolean canConnectMsg = true;

			BusinessObject busObj = new BusinessObject(type,name, rev, "");
			busObj.open(context);

			RelationshipList	relList = _util.getToRelationship(context, busObj, (short)0, false);

			if (relList != null)
			{
				RelationshipItr relItr = new RelationshipItr(relList);
				while (relItr.next())
				{
					Relationship returnRel = relItr.obj();
					if(returnRel.getTypeName().equals(activeMessageRelationship))
					{
						returnRel.open(context);
						BusinessObject msgObj =	returnRel.getFrom();
						if(_util.getCurrentState(context, msgObj.getObjectId(context)).equals("Submitted"))
						{

							canConnectMsg =false;
						}
					}
				}

				if(canConnectMsg)
				{
					DSCQueue queue = new DSCQueue(context, MCADMxUtil.getActualNameForAEFData(context, "type_DSCQueue"), queueName);
					
					if(!queue.exists(context))
					{
						queue.create(context, MCADMxUtil.getActualNameForAEFData(context, "policy_DSCQueuePolicy"));
					}

					queue.open(context);

					String uniqueName 	= getUniqueNameForMessage();	
					String messageBody 	= createMessageBody(context, uniqueName, type, name, ver);

					DSCMessage messageObject	= new DSCMessage(context, uniqueName, messageBody);

					if(!messageObject.exists(context))
					{
						messageObject.create(context, MCADMxUtil.getActualNameForAEFData(context,"policy_DSCMessagePolicy"));
					}

					_util.setAttributeOnBusObject(context, messageObject, MCADMxUtil.getActualNameForAEFData(context, "attribute_Title"), operationName);
					
					messageObject.open(context);
					messageObject.setBody(context, messageBody);
					messageObject.setPriority(context, "1");
					messageObject.close(context);

					queue.addPendingMessage(context, messageObject);

					connectMessageAndBusObj(context, busObj, messageObject, activeMessageRelationship, false);

					queue.close(context);
				}
			}
		}
		catch(Exception e)
		{
			MCADServerException.createManagedException("IEF0133200103", _serverResourceBundle.getString("mcadIntegration.Server.Message.IEF0133200103"), e);
		}
	}
	
	
	private void connectMessageAndBusObj(Context context, BusinessObject busObj, DSCMessage messageObj, String relationshipName, boolean isFrom) throws Exception
	{ 		
		RelationshipType relType = new RelationshipType(relationshipName);
		relType.open(context);	
		Relationship activeMessage = busObj.connect(context, relType, isFrom, messageObj);
		relType.close(context);		
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

	private String createMessageBody(Context context, String messageName, String type, String name,  String ver) throws Exception
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

		IEFXmlNodeImpl ignoreLockNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		ignoreLockNode.setName("ignoreLock");
		ignoreLockNode.setContent("false");
		messageNode.addNode(ignoreLockNode);

		IEFXmlNodeImpl messageBodyNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		messageBodyNode.setName("messagebody");
		messageNode.addNode(messageBodyNode);

		IEFXmlNodeImpl cadObjectListNode	= new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		cadObjectListNode.setName("cadobjectlist");
		messageBodyNode.addNode(cadObjectListNode);

		IEFXmlNodeImpl cadObjectNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		cadObjectNode.setName("cadobject");
		cadObjectListNode.addNode(cadObjectNode);


		Hashtable cadObjectAttributes = new Hashtable();			
		cadObjectAttributes.put("type", type);
		cadObjectAttributes.put("name", name);
		cadObjectAttributes.put("revision", ver);
		cadObjectNode.setAttributes(cadObjectAttributes);


		return messageNode.getXmlString();
	}
	
	public void deleteMessageObject(Context context, String []args)  throws Exception
	{
		try 
		{
			HashMap hashMapArgs						= (HashMap) JPO.unpackArgs(args);
			String type                             = (String) hashMapArgs.get("type"); 
			String name                             = (String) hashMapArgs.get("name");
			String rev                              = (String) hashMapArgs.get("rev");
			String languageName						= (String) hashMapArgs.get("language");
			this._serverResourceBundle				= new MCADServerResourceBundle(languageName);
			this._util								= new MCADMxUtil(context, _serverResourceBundle, _cache);
			

			String cadSubcomponentRelationship      = (String) hashMapArgs.get("cadsubcomprel");
			String activeMessageRelationship        = (String) hashMapArgs.get("activemessagerel");

			BusinessObject busChildObj = new BusinessObject(type,name, rev, "");
			busChildObj.open(context);
			RelationshipList relChildList = _util.getToRelationship(context, busChildObj, (short)0, false);
			if (relChildList != null)
			{	
				RelationshipItr relItr = new RelationshipItr(relChildList);
				while (relItr.next())
				{
					Relationship returnRel = relItr.obj();
					if(returnRel.getTypeName().equals(cadSubcomponentRelationship))
					{
						String relChildId = returnRel.getName();						
						returnRel.open(context);

						BusinessObject parentObj =	returnRel.getFrom();

						RelationshipList relParentList = _util.getToRelationship(context, parentObj, (short)0, false);
						if (relParentList != null)
						{	
							RelationshipItr relParentItr = new RelationshipItr(relParentList);
							while (relParentItr.next())
							{
								Relationship returnParentRel = relParentItr.obj();
								if(returnParentRel.getTypeName().equals(activeMessageRelationship))
								{
									returnParentRel.open(context);
									BusinessObject msgObj =	returnParentRel.getFrom();
									//check state of message object
									if(_util.getCurrentState(context, msgObj.getObjectId(context)).equals("Submitted"))
									{
										RelationshipList relAllChildList = _util.getFromRelationship(context, parentObj, (short)0, false);
										if (relAllChildList != null)
										{
											RelationshipItr relAllChildListItr = new RelationshipItr(relAllChildList);
											boolean deleteMsgObj = false;
											
											int counter	= 0;
											while (relAllChildListItr.next())
											{
												Relationship returnAllChildRel = relAllChildListItr.obj();
												String returnRelID = returnAllChildRel.getName();
						
												if(!relChildId.equals(returnRelID))
												{
													if(returnAllChildRel.getTypeName().equals(cadSubcomponentRelationship))
													{
														counter++;

														returnAllChildRel.open(context);
														String attrValue = _util.getRelationshipAttributeValue(context,returnAllChildRel,MCADMxUtil.getActualNameForAEFData(context, "attribute_RelationshipModificationStatusinMatrix"));
														
														if(attrValue == null || attrValue.length() == 0)
														{
															deleteMsgObj = true;
														}														
													}
												}
											}
											
											if(deleteMsgObj || (counter < 1))
											{
												msgObj.remove(context);	
											}
										}
									}
								}
							}
						}	
					}
				}
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.FailedWhileDeletingDSCMessage"),e);
		}
		
	}
	
	
	
	
}
