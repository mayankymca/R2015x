import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import matrix.db.Context;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.apps.common.Person;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.program.ProgramCentralConstants;
import com.matrixone.apps.program.Task;

public class PRGSupportBase_mxJPO extends emxDomainObject_mxJPO {
	
    public PRGSupportBase_mxJPO (Context context, String[] args) throws Exception {
      super(context, args);
    }
	
    public MapList getTaskRelatedInfo(Context context, String[] argumentStringArray) throws FrameworkException, ParseException {
    	
		String clientOffSet     = argumentStringArray[1];
		String clientdateFormat = argumentStringArray[2]; 
		
		SimpleDateFormat format = new SimpleDateFormat(clientdateFormat);
		TimeZone zone =TimeZone.getTimeZone("GMT");
		zone.setRawOffset(Integer.parseInt(clientOffSet));
		format.setTimeZone(zone);
		MapList outputMapList = new MapList();
    	
		StringList objSelects = new StringList(14);
		objSelects.addElement(SELECT_ID);
		objSelects.addElement(SELECT_CURRENT);
		objSelects.addElement(SELECT_MODIFIED);
		objSelects.addElement(SELECT_NAME);
		objSelects.addElement(SELECT_TYPE);
		objSelects.addElement(Task.SELECT_PREDECESSOR_IDS);
		objSelects.addElement(Task.SELECT_PREDECESSOR_TYPES);
		objSelects.addElement(Task.SELECT_TASK_ESTIMATED_START_DATE);
		objSelects.addElement(Task.SELECT_TASK_ESTIMATED_FINISH_DATE);
		objSelects.addElement(Task.SELECT_TASK_ESTIMATED_DURATION);		
		objSelects.addElement(Task.SELECT_TASK_ACTUAL_START_DATE);
		objSelects.addElement(Task.SELECT_OWNER);
		objSelects.addElement(ProgramCentralConstants.SELECT_KINDOF_TASK);
		
		//where condn is removed to return all state's task. IR-376471-3DEXPERIENCER2016x
		StringBuilder whereClause = new StringBuilder("");
		/*StringBuilder whereClause = new StringBuilder(Task.SELECT_CURRENT);
		whereClause.append("=='");
		whereClause.append(ProgramCentralConstants.STATE_PROJECT_TASK_CREATE);
		whereClause.append("' ||");
		whereClause.append(Task.SELECT_CURRENT);
		whereClause.append("=='");
		whereClause.append(ProgramCentralConstants.STATE_PROJECT_TASK_ASSIGN);
		whereClause.append("' ||");
		whereClause.append(Task.SELECT_CURRENT);
		whereClause.append("=='");
		whereClause.append(ProgramCentralConstants.STATE_PROJECT_TASK_ACTIVE);
		whereClause.append("'");
		*/
		Person person = Person.getPerson(context);
		MapList tasklist = person.getAssignments(context,objSelects,whereClause.toString());
		
		Iterator mapIterator = tasklist.iterator();
		while(mapIterator.hasNext()) {
			
			Map<String, String> taskMap = (Map)mapIterator.next();
			
			String objectType = (String)taskMap.get(ProgramCentralConstants.SELECT_KINDOF_TASK);
			
			if (!"TRUE".equalsIgnoreCase(objectType)) {
				continue;
			}
			/*String estStartDateString = (String)taskMap.get(Task.SELECT_TASK_ESTIMATED_START_DATE);
			String estEndDateString   = (String)taskMap.get(Task.SELECT_TASK_ESTIMATED_FINISH_DATE);
			String estConstraintDateString = (String)taskMap.get(Task.SELECT_TASK_CONSTRAINT_DATE);
			
			if(UIUtil.isNotNullAndNotEmpty(estConstraintDateString)) {
				estConstraintDate=dateFormat.parse(estConstraintDateString);
				taskMap.put(Task.SELECT_TASK_CONSTRAINT_DATE, format.format(estConstraintDate));
				
			}
			Date estStartDate = dateFormat.parse(estStartDateString);
			Date estEndDate   = dateFormat.parse(estEndDateString);
			taskMap.put(Task.SELECT_TASK_ESTIMATED_START_DATE, format.format(estStartDate));
			taskMap.put(Task.SELECT_TASK_ESTIMATED_FINISH_DATE, format.format(estEndDate));*/
			
			outputMapList.add(taskMap);
		}
		return outputMapList;
	}

	public MapList getTaskRelatedInfoAfterDate(Context context,String[] argumentStringArray) throws Exception {

		MapList taskInfoMapList = new MapList();
		
		if (argumentStringArray == null || argumentStringArray.length != 4) {
			throw new MatrixException("Please send correct arguments");
		}
			
		String clientLanguage   = argumentStringArray[0];
		String clientOffSet     = argumentStringArray[1];
		String clientdateFormat = argumentStringArray[2]; 
		String dateTime 		= argumentStringArray[3];
		
		if(UIUtil.isNullOrEmpty(clientLanguage)|| UIUtil.isNullOrEmpty(clientOffSet)
				|| UIUtil.isNullOrEmpty(clientdateFormat)||UIUtil.isNullOrEmpty(dateTime)) {
			
			throw new MatrixException("Please send correct arguments");
		}
		
		MapList mapList = getTaskRelatedInfo(context,argumentStringArray);
		SimpleDateFormat clientDateFormat=new SimpleDateFormat(clientdateFormat);
		TimeZone clientTimeZone = TimeZone.getTimeZone("UTC");
                clientTimeZone.setRawOffset(Integer.parseInt(clientOffSet));
                clientDateFormat.setTimeZone(clientTimeZone);
		Date datePassed = clientDateFormat.parse(dateTime);
		
		SimpleDateFormat format = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat());
		TimeZone serverTimeZone = TimeZone.getTimeZone("UTC");
                int serverTimeZoneOffset = TimeZone.getDefault().getOffset(new Date().getTime());
                serverTimeZone.setRawOffset(serverTimeZoneOffset);
                format.setTimeZone(serverTimeZone);
		Iterator iterator = mapList.iterator();

		while (iterator.hasNext()) {
			Map taskInfo = (Map) iterator.next();
			String modifiedDate = (String) taskInfo.get(SELECT_MODIFIED);
			Date dateModified = format.parse(modifiedDate);
			if (dateModified.compareTo(datePassed) >= 0) {
				taskInfoMapList.add(taskInfo);
			}
		}
		return taskInfoMapList;
	}
	
    public static String updateTaskPercentComplete(Context context, String[] argumentStringArray) throws MatrixException {
    	
    	if (argumentStringArray == null || argumentStringArray.length != 2) {
			throw new MatrixException("Please send correct arguments");
		}
    	
   		String message = DomainConstants.EMPTY_STRING;
   		String taskId  = argumentStringArray[0];
   		String percentComplete = argumentStringArray[1];
   		Map objectList = new HashMap(1);
   		Map objectValues = new HashMap(1);

   		try {
   			Task task = (Task) DomainObject.newInstance(context,TYPE_TASK, "PROGRAM");
   			task.setId(taskId);
   			objectValues.put("percentComplete", percentComplete);
   			objectList.put(taskId, objectValues);
   			String errmessage =ProgramCentralConstants.EMPTY_STRING;
   			if(percentComplete.equalsIgnoreCase("100"))
   			{
   			task.setState(context, "Complete");
   			}
   			else
   			{
   				 errmessage = Task.updateDates(context, objectList, true,false);
   			}
   			task.rollupAndSave(context);
   			
   			if (errmessage.contains("System Error")) {
   				message = "failed";
   			} else {
   				message = "success";
   			}
   			
   		} catch (FrameworkException e) {
   			message = "failed";
   		} catch (Exception e) {
   			message = "failed";
   		}
   		return message;
   	}
}

