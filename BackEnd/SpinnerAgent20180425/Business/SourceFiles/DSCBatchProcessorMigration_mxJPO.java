/*
 * DSCBatchProcessorMigration.java program to migrate the old messages after upgrade to V6R2008-1
 *
 * Copyright Dassault Systemes, 1992-2007. All rights
 * reserved.

 * This program contains proprietary and trade secret information of
 * Dassault Systemes and its subsidiaries.
 * Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.ExpansionWithSelect;
import matrix.db.MatrixWriter;
import matrix.db.RelationshipWithSelect;
import matrix.db.State;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCBackgroundProcessorUtil;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCMessage;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCQueue;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class DSCBatchProcessorMigration_mxJPO
{
    private BufferedWriter writer		= null;
	private FileWriter iefLog			= null;

	private MCADMxUtil mxUtil			= null;
    private String documentDirectory	= "";

	private String QUEUE_NAMES			= "";

	private String DSCMessageErrorRelName	= null;
	private String DSCMessageSuccessRelName	= null;
	private String DSCMessagePendingRelName	= null;
	private Vector pendingMessageIds		= new Vector();
	private Vector failedMessageIds			= new Vector();
	private Vector successfulMessageIds		= new Vector();
	

    public DSCBatchProcessorMigration_mxJPO (Context context, String[] args) throws Exception
    {
		writer	= new BufferedWriter(new MatrixWriter(context));
		mxUtil	= new MCADMxUtil(context, new MCADServerResourceBundle("en-US"), new IEFGlobalCache());
		
		this.DSCMessageSuccessRelName	= DSCBackgroundProcessorUtil.getActualNameForAEFData(context, "relationship_DSCMessageSuccess");
		this.DSCMessageErrorRelName		= DSCBackgroundProcessorUtil.getActualNameForAEFData(context, "relationship_DSCMessageError");
		this.DSCMessagePendingRelName	= DSCBackgroundProcessorUtil.getActualNameForAEFData(context, "relationship_PendingMessage");
    } 

    public int mxMain(Context context, String[] args) throws Exception
    {
		if(!context.isConnected())
		{
			throw new Exception("not supported on desktop client");
		}

		try
		{
			validateInputArguments(args);
		}
		catch (Exception ex)
		{
			writeErrorToConsole(ex.getMessage());
			writer.close();
			return 0;
		}

		try
		{
			startIEFLog();
			String Args[] = new String[1];
			Args[0] = "off";
			mxUtil.executeMQL(context,"trigger $1",Args);

			Vector queueList 			= DSCBackgroundProcessorUtil.getVectorFromString(QUEUE_NAMES, ",");
			Iterator queueNamesElements	= queueList.iterator();
			DSCMessage message			= null;
			
			while(queueNamesElements.hasNext())
			{
				try
				{
					DSCQueue queue = new DSCQueue(context, DSCBackgroundProcessorUtil.getActualNameForAEFData(context, "type_DSCQueue"), queueNamesElements.next().toString());
					
					if(queue.exists(context))
					{
						queue.open(context);

						pendingMessageIds			= getMessages(context, queue, this.DSCMessagePendingRelName);
						promoteMessages(context, pendingMessageIds, "Submitted");
						
						failedMessageIds			= getMessages(context, queue, this.DSCMessageErrorRelName);
						promoteMessages(context, failedMessageIds, "Completed");

						successfulMessageIds		= getMessages(context, queue, this.DSCMessageSuccessRelName);
						Enumeration successMsgEnum	= successfulMessageIds.elements();
						while(successMsgEnum.hasMoreElements())
						{
        					String messageId	= (String)successMsgEnum.nextElement();
							message				= new DSCMessage(context, messageId);
							message.open(context);
							log("Message deleted : Type -> " + message.getTypeName() + ", Name -> " + message.getName() + ", Revision -> " + message.getRevision());
							message.remove(context);
							message = null;
						}
					}

				}
				catch (Exception ex)
				{
					if(message != null)
						log("Failed to process message: Type -> " + message.getTypeName() + ", Name -> " + message.getName() + ", Revision -> " + message.getRevision() + " Error: " + ex.getMessage());
				}
			}
                        Args[0] = "on";
			mxUtil.executeMQL(context,"trigger $1", Args);
			
			writeSuccessToConsole();
		}
		catch (FileNotFoundException fEx)
		{
			writeErrorToConsole("Directory does not exist or does not have access to the directory");
		}
		catch (Exception ex)
		{
			writeErrorToConsole("Migration of old messages failed: " + ex.getMessage());
			ex.printStackTrace(new PrintWriter(iefLog, true));
		}

		endIEFLog();
		writer.flush();
		writer.close();
        return 0;
    }

	/**
	* Get the list of messages connected to the queue with the relationship name passed as
	* an argument to the method.
	*/
	private Vector getMessages(Context context, DSCQueue queue, String relationName) throws Exception
	{
		Vector messagesList		= new Vector();
		StringList relSelect	= new StringList();
		relSelect.addElement("to.id");
		ExpansionWithSelect expansionWithSelect = queue.expandSelect(context, relationName, DSCBackgroundProcessorUtil.getActualNameForAEFData(context, "type_DSCMessage"), new StringList(), relSelect, true, true, (short)1, "", "", true);
		Enumeration relationsList				= expansionWithSelect.getRelationships().elements();
		ArrayList validMessageIds				= new ArrayList();
		while(relationsList.hasMoreElements())
		{
			RelationshipWithSelect relationship = (RelationshipWithSelect) relationsList.nextElement();
		    relationship.open(context);
		    
		    String messageId = relationship.getSelectData("to.id");
		    relationship.close(context);
			messagesList.add(messageId);
		}

		return messagesList;
	}

	private void promoteMessages(Context context, Vector messageIds, String targetState)
	{
		DSCMessage message	= null;
		try
		{
			Enumeration msgEnum	= messageIds.elements();
			while(msgEnum.hasMoreElements())
			{
				String messageId	= (String)msgEnum.nextElement();
				message				= new DSCMessage(context, messageId);
				message.open(context);
				promoteToTargetState(context, message, targetState);
				message.close(context);
				message = null;
			}
		}
		catch (Exception ex)
		{
			if(message != null)
				log("Failed to process message: Type -> " + message.getTypeName() + ", Name -> " + message.getName() + ", Revision -> " + message.getRevision() + " Error: " + ex.getMessage());
		}
	}

	/**
	* Promote either
	*	1) Failed messages to the completed state. OR
	*	2) Created messages to the submitted state depending on the State passed as an argument.
	*/
	private void promoteToTargetState(Context context, DSCMessage message, String targetState) throws Exception
	{
		State currentState	= mxUtil.getCurrentState(context, message);
		
		while(!currentState.getName().equals(targetState))
		{
			message.promote(context);
			currentState = mxUtil.getCurrentState(context, message);
		}
		log("Message promoted to " + targetState + " state : Type -> " + message.getTypeName() + ", Name -> " + message.getName() + ", Revision -> " + message.getRevision());
	}


	/**
	* Validate the input arguments. Two arguments have to be passed
	* First argument - the queue names as a comma-seperated list. e.g. "Queue1,Queue2".
	* Second argument - the path of the directory where the logs are to be genereted is needed.
	*/
	private void validateInputArguments(String[] args) throws Exception
	{
		if (args.length != 2 )
			throw new IllegalArgumentException("Wrong number of arguments");

		QUEUE_NAMES			= args[0];
		documentDirectory	= args[1];

		String fileSeparator = java.io.File.separator;
		if(documentDirectory != null && !documentDirectory.endsWith(fileSeparator))
			documentDirectory = documentDirectory + fileSeparator;
	}

	///////////////////////////////////////////////////////////////////////////////////

	private void writeLineToConsole() throws Exception
	{
		writeMessageToConsole("=======================================================");
	}

	private void writeMessageToConsole(String message) throws Exception
	{
		writer.write(message + "\n");
	}

	private void writeErrorToConsole(String message) throws Exception
	{
		writeLineToConsole();
		writeMessageToConsole(message);
		writeMessageToConsole("		Migration	: FAILED");
		writeLineToConsole();
		writer.flush();
	}

	private void writeSuccessToConsole() throws Exception
	{
		writeLineToConsole();
		writeMessageToConsole("		Migration of old messages : COMPLETED");
		writeLineToConsole();
		writer.flush();
	}

	private void startIEFLog() throws Exception
	{
		try
		{
			iefLog	= new FileWriter(documentDirectory + "DSCBatchProcessorMigration.log");
		}
		catch(Exception e)
		{
			writeMessageToConsole("ERROR: Can not create log file. " + e.getMessage());
		}
	}

	private void endIEFLog()
	{
		try
		{
			iefLog.write("\n\n");
			iefLog.flush();
			iefLog.close();
		}
		catch(Exception e)
		{
		}
	}

	private void log(String message)
	{
		try
		{
			iefLog.write(message + "\n");
		}
		catch(Exception e)
		{
		}
	}
}
