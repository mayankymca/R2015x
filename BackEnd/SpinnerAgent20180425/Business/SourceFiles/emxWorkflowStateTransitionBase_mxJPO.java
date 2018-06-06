/*
**  emxWorkflowStateTransitionBase.java
**
**  Copyright (c) 1992-2015 Dassault Systemes.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of MatrixOne,
**  Inc.  Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**
*/

import java.util.*;

import matrix.db.*;
import matrix.util.*;

import com.matrixone.apps.common.Person;
import com.matrixone.apps.common.Workflow;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.*;




/**
 * The <code>emxWorkflowEngineBase</code> class contains methods for document.
 *
 *
 */

public class emxWorkflowStateTransitionBase_mxJPO
{
	
	//Constructor
	public emxWorkflowStateTransitionBase_mxJPO (Context context, String[] args)throws Exception
	{
    }
	
	public int checkWorkflowsCompleted(Context context, String[] args)throws Exception 
	{
		int returnValue = 0;
		//Get Type, Name, Revision objectId from arguments
		String sType = args[0];
		String sName = args[1];
		String sRev = args[2];
		String objectId = args[3];
		String sState = args[4];
		String sPolicy = args[5];
		
		String sRelWorkflowContent = PropertyUtil.getSchemaProperty(context, "relationship_WorkflowContent");
		String sAttrRouteBaseState = PropertyUtil.getSchemaProperty(context, "attribute_RouteBaseState");
		String sAttrRouteBasePolicy = PropertyUtil.getSchemaProperty(context, "attribute_RouteBasePolicy");
		String sStateComplete = PropertyUtil.getSchemaProperty(context, "policy", 
															PropertyUtil.getSchemaProperty(context, "policy_Workflow"), "state_Completed");
				
		StringBuffer notCompletedWorkflows = new StringBuffer(256);			
		boolean doNotPromote = false;
		
		MapList wfList = null;
		//Expand the object to get the connected workflow
		DomainObject dObject = null;
		if(objectId != null && objectId.length() > 0)
		{
			dObject = new DomainObject(objectId);
		}
		if(dObject != null)
		{
			StringList objectSelects = new StringList(2);
			StringList relSelects = new StringList(2);
			objectSelects.add(DomainObject.SELECT_NAME);
			objectSelects.add(DomainObject.SELECT_CURRENT);
			relSelects.add("attribute[" + sAttrRouteBaseState + "]");
			relSelects.add("attribute[" + sAttrRouteBasePolicy + "]");
			
			Pattern relPattern        = new Pattern("");
			relPattern.addPattern(sRelWorkflowContent);
			
			wfList = dObject.getRelatedObjects(context,
	                 relPattern.getPattern(),
	                 "*",
	                 objectSelects,
	                 relSelects,
	                 true,
	                 false,
	                 (short)0,
					  null, //objectWhere
	                 null, //relWhere,
					  null,
	                 null,
	                 null);		
		}		
		//Check connected workflow are completed based on Route Base State and Route Base Policy attributes.
		if(wfList != null && wfList.size() > 0)
		{
			String adHocProperty = EnoviaResourceBundle.getProperty(context,"emxComponents.Workflow.AdHocWorkflowBlockLifecycle");
			boolean adHocWorkflowBlock = false;
			if(adHocProperty != null && adHocProperty.equalsIgnoreCase("true"))
			{
				adHocWorkflowBlock = true;
			}
			
			Iterator itr = wfList.iterator();
			String wfName = "";
			String currState = "";
			String rBaseState = "";
			String rBasePolicy = "";
			
			
			while (itr.hasNext() )
			{
				Map item = (Map)itr.next();
				wfName = (String)item.get(DomainObject.SELECT_NAME);
				currState = (String)item.get(DomainObject.SELECT_CURRENT);
				rBaseState = (String)item.get("attribute[" + sAttrRouteBaseState + "]");
				rBasePolicy = (String)item.get("attribute[" + sAttrRouteBasePolicy + "]");
				
				if((adHocWorkflowBlock && rBaseState.equals("Ad Hoc")) 
						|| (sState.equals(rBaseState) && sPolicy.equals(rBasePolicy)))
				{
					if(!currState.equals(sStateComplete))
					{
						
						doNotPromote = true;
						if(notCompletedWorkflows.length() > 0)
						{
							notCompletedWorkflows.append(" ");
						}
						notCompletedWorkflows.append(wfName);
					}
				}
			}			
		}
		
		//If all workflows connected are not completed show mql notice
		if(doNotPromote)
		{
			returnValue = 1;
			try{
			String[] messageKeys = {"Type", "Name", "Rev", "Workflows"};
			String[] messageValues = {sType, sName, sRev, notCompletedWorkflows.toString()};
			String message = emxMailUtil_mxJPO.getMessage(context,
												"emxComponents.Workflow.WorkflowNotComplete",
												messageKeys,
												messageValues,
												"",
												"emxComponentsStringResource");
			
			String cmd = "notice '" + message + "'";
			MqlUtil.mqlCommand(context, cmd);	
			}
			catch(Exception ex)
			{
				
			}
		}
		
		
		return returnValue;
	}
		
}
