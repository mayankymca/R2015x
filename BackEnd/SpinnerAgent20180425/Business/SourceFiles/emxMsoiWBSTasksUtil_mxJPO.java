// emxMsoiWBSTasksUtil.java
//
// 
// Copyright (c) 2002 MatrixOne, Inc.
// All Rights Reserved
// This program contains proprietary and trade secret information of
// MatrixOne, Inc.  Copyright notice is precautionary only and does
// not evidence any actual or intended publication of such program.
//
// $Log: emxMsoiWBSTasksUtil.java,v $
// Office Integration Menus

import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import matrix.db.*;
import matrix.util.*;

import com.matrixone.apps.framework.ui.*;
import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;
import com.matrixone.apps.common.*;

/**
 * The <code>emxMsoiWBSTasksUtil</code> class represents the JPO for
 * obtaining the MS Office integration menus
 *
 * @version AEF 10.5 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxMsoiWBSTasksUtil_mxJPO 
{

/**
   * Constructs a new emxMsoiWBSTasksUtil JPO object.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args an array of String arguments for this method
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */
  public emxMsoiWBSTasksUtil_mxJPO (Context context, String[] args)
      throws Exception
  {
    // Call the super constructor
    super();
  }
   
/**
   * Get current user's WBS Tasks
   * 
   * @param context
   * @return a MapList of WBS Tasks
   * @throws Exception if the operation fails
   */
  public MapList getCurrentUserWBSTasks(Context context, String[] args) throws MatrixException
  { 
		MapList wbsTaskList = new MapList();
		com.matrixone.apps.common.Task task = (com.matrixone.apps.common.Task) DomainObject.newInstance(context,DomainConstants.TYPE_TASK);

		String personId = PersonUtil.getPersonObjectID(context);
		com.matrixone.apps.common.Person person = (com.matrixone.apps.common.Person)DomainObject.newInstance(context,personId);

		StringList busSelects = new StringList(9);
		busSelects.add(task.SELECT_ID);
		busSelects.add(task.SELECT_NAME);
		busSelects.add(task.SELECT_CURRENT);
		busSelects.add(task.SELECT_TYPE);	  
		busSelects.add(task.SELECT_PERCENT_COMPLETE);
		busSelects.add(task.SELECT_TASK_ESTIMATED_START_DATE);
		busSelects.add(task.SELECT_TASK_ESTIMATED_FINISH_DATE);
		busSelects.add(task.SELECT_TASK_ACTUAL_FINISH_DATE);
		busSelects.add(task.SELECT_TITLE);

		Map hrefMap = new HashMap();

		try
		{
			hrefMap = (HashMap) JPO.unpackArgs(args);
		} catch(Exception e)
		{
			System.out.println("[emxMsoiWBSTasksUtil.getCurrentUserWBSTasks] EXCEPTION : " + e.getMessage());
		}
		Map href = (Map) hrefMap.get("hRef");
		String tasks = (String) href.get("tasks");

		// GLOBAL VARIABLES
		String policyName = task.getDefaultPolicy(context);
		//Change SJ7+ for removing JPO Compilation issues.
		//String completeStateName = task.getSchemaProperty(context,"policy",policyName,"state_Complete");
		//String createStateName = task.getSchemaProperty(context,"policy",policyName,"state_Create");
		
		String completeStateName = PropertyUtil.getSchemaProperty(context,"policy",policyName,"state_Complete");
		String createStateName = PropertyUtil.getSchemaProperty(context,"policy",policyName,"state_Create");

		String busWhere = "";

		if(tasks != null && "complete".equalsIgnoreCase(tasks))
			busWhere = task.SELECT_CURRENT + "=='" + completeStateName + "'";
		else if(tasks != null && "".equals(tasks))
			busWhere = task.SELECT_CURRENT + "!='" + completeStateName + "'";
		else
			return wbsTaskList;

		wbsTaskList = person.getAssignments(context, busSelects, busWhere,true);
		return wbsTaskList;

		/*StringList aList = person.getInfoList(context, "to[" + DomainObject.RELATIONSHIP_MEMBER + "].from.id");

		Iterator aListItr = aList.iterator();
		while (aListItr.hasNext())
		{
			String Id = (String) aListItr.next();

			DomainObject Obj = DomainObject.newInstance(context, Id);
			MapList aMapList = Obj.getRelatedObjects(context,
								 DomainObject.RELATIONSHIP_SUBTASK,
								 DomainObject.TYPE_TASK,
								 false,
								 true,
								 (short) 0,
								 busSelects,
								 null,
								 busWhere,
								 null,
								 null,
								 DomainObject.TYPE_TASK,
								 null);

			wbsTaskList.addAll(aMapList);

		}
		return wbsTaskList;*/

  }
}
