/*
**  DSC_BackgroundScheduler
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**
*/

import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.State;

import com.matrixone.MCADIntegration.server.batchprocessor.DSCBackgroundProcessorUtil;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCPeriodicMessage;
import com.matrixone.MCADIntegration.server.batchprocessor.DSCQueue;

public class DSC_BackgroundScheduler_mxJPO
{

	//The queue names need to be specified here as a comma-seperated list. e.g. "Queue1, Queue2".
	private final String queueNames = "";

	public DSC_BackgroundScheduler_mxJPO (Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            throw new Exception("not supported no desktop client");
    }

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}
		
	/**
	*
	*/
	public void runScheduler(Context context, String[] args) throws Exception
	{

		Vector queueList 				= DSCBackgroundProcessorUtil.getVectorFromString(queueNames,",");
		Iterator queueNamesElements		= queueList.iterator();
		
		while(queueNamesElements.hasNext())
		{
			try
			{
				DSCQueue queue = new DSCQueue(context, DSCBackgroundProcessorUtil.getActualNameForAEFData(context, "type_DSCQueue"), queueNamesElements.next().toString());
					
				if(queue.exists(context))
			    {
					queue.open(context);
			        Vector periodicMessageList  = queue.getPeriodicMessages(context);
			        Enumeration periodicMsgEnum = periodicMessageList.elements();
			        while(periodicMsgEnum.hasMoreElements())
					{
						String messageId				   = (String)periodicMsgEnum.nextElement();
						DSCPeriodicMessage periodicMessage = new DSCPeriodicMessage(context, messageId);

						long periodicIntervalInMilliSec	   = periodicMessage.getPeriodicInterval(context)*60*1000;
						Date lastRunDate 				   = periodicMessage.getLastRun(context);
						Date startDate					   = periodicMessage.getStartDate(context);
						int periodicRunLimit		  	   = periodicMessage.getPeriodicRunLimit(context);
						int periodicRunCount			   = periodicMessage.getPeriodicRunCount(context);
						State currentState				   = DSCBackgroundProcessorUtil.getCurrentState(context, periodicMessage);
					 
						Date currentDate				   = new Date();
						
						if(currentState.getName().equals("Created") && (lastRunDate == null) && (startDate.compareTo(currentDate) < 0))
						{
							queue.addPendingMessage(context,periodicMessage);
						}
						else if(currentState.getName().equals("Completed") && lastRunDate != null)
						{
							long lastRunIntervalTime	= lastRunDate.getTime();
							long currentTime			= currentDate.getTime();

							if(((currentTime - lastRunIntervalTime) > periodicIntervalInMilliSec) && (periodicRunCount <= periodicRunLimit))
								queue.addPendingMessage(context,periodicMessage);
						}
					}
				}
			}
			catch (Exception ex)
			{
				throw new Exception(ex.getMessage());
			}	
		}
	}	
}
