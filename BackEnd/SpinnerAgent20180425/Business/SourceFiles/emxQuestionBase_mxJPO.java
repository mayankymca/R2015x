/* emxQuestionBase.java
 *
 * Copyright (c) 2002-2015 Dassault Systemes.
 * All Rights Reserved
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 * static const char RCSID[] = $Id: ${CLASSNAME}.java.rca 1.9.2.1 Thu Dec  4 07:55:03 2008 ds-ss Experimental ${CLASSNAME}.java.rca 1.9 Wed Oct 22 15:49:11 2008 przemek Experimental przemek $
 */

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import matrix.db.AccessConstants;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.RelationshipType;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.program.ProgramCentralUtil;
import com.matrixone.apps.program.Question;
import com.matrixone.apps.program.QuestionRelationship;
import com.matrixone.apps.program.Task;

/**
 * The <code>emxQuestionBase</code> class represents the Question JPO
 * functionality for the AEF type.
 * 
 * @version AEF 9.5.1.1 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxQuestionBase_mxJPO extends com.matrixone.apps.program.Question {
    
    /** The parent/holder object of this question. */
    protected DomainObject _parentObject = null;

    /** The project access list id relative to project. */
    static protected final String SELECT_PARENT_ID = "to["
            + RELATIONSHIP_PROJECT_QUESTION + "].from.id";

    /**
     * Constructs a new emxQuestion JPO object.
     * 
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: 0 - String containing the
     *            id
     * @throws Exception
     *             if the operation fails
     * @since AEF 9.5.1.1
     */
    public emxQuestionBase_mxJPO(Context context, String[] args)
            throws Exception {
        // Call the super constructor
        super();
        if (args != null && args.length > 0) {
            setId(args[0]);
        }
    }

    /**
     * Get the parent/holder object.
     * 
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return DomainObject parent/holder object
     * @throws Exception
     *             if the operation fails
     * @since AEF 9.5.1.2
     */
    protected DomainObject getParentObject(Context context) throws Exception {
        if (_parentObject == null) {
            // System.out.println("Retrieving project security ID..." +
            // (new Date().getTime()));
            String parentId = getInfo(context, SELECT_PARENT_ID);
            if (parentId != null && !"".equals(parentId)) {
                _parentObject = DomainObject.newInstance(context, parentId);
            }
        }
        return _parentObject;
    }

    /**
     * This function verifies the user's permission for the given Question.
     * 
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: PARENT_MODIFY to see if
     *            the context user has modify access to the parent object, <BR>
     * @return boolean true or false
     * @throws Exception
     *             if the operation fails
     * @since AEF 9.5.1.0
     */
    public boolean hasAccess(Context context, String args[]) throws Exception {
        // System.out.println("Start Question - " + (new Date().getTime()));
        // program[emxQuestion PARENT_MODIFY -method hasAccess
        // -construct ${OBJECTID}] == true
        boolean access = false;
        String accessType = args[0];

        if ("PARENT_MODIFY".equals(accessType)) {
            DomainObject parentObject = getParentObject(context);

            if (parentObject != null) {
                int iAccess = AccessConstants.cModify;
                // System.out.println("Checking access..." +
                // (new Date().getTime()));
                if (parentObject.checkAccess(context, (short) iAccess)) {
                    access = true;
                }
            }
			else{
			    access = true;
			}

        }
        // System.out.println(new Date().getTime());
        // System.out.println("End ProjectSpace - " + context.getUser() +
        // " : " + getId() + " : " + access);

        return access;
    }

    /**
     * getQuestions - This method gets the List the Question added to the
     * ProjectTemplate Task. Used for PMCTaskQuestionSummary table.
     * 
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @return MapList contains list of project members
     * @throws Exception
     *             if the operation fails
     * @since PMC X+2
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getQuestions(Context context, String[] args)
            throws Exception {

        com.matrixone.apps.program.Task task = (com.matrixone.apps.program.Task) DomainObject
                .newInstance(context, DomainConstants.TYPE_TASK, "PROGRAM");
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) programMap.get("objectId");
        MapList questionList = new MapList();
        String taskQuestionId = "";
        if (objectId != null && !"null".equals(objectId)
                && !"".equals(objectId)) {
            task.setId(objectId);
            taskQuestionId = task.getInfo(context,
                    Task.SELECT_QUESTION_CONNECTION_ID);
        }
        // Only access this part of the page if the task has a question assigned
        // to it
        if (taskQuestionId != null && !"null".equals(taskQuestionId)
                && !"".equals(taskQuestionId)) {
            Map questionMap = null;
            // Get questionId
            StringList taskSelects = new StringList(4);
            taskSelects.add(DomainConstants.SELECT_NAME);
            taskSelects.add(DomainConstants.SELECT_DESCRIPTION);
            taskSelects.add(DomainConstants.SELECT_ID);

            StringList relSelects = new StringList(2);
            relSelects.add(QuestionRelationship.SELECT_TASK_TRANSFER);
            relSelects.add(DomainRelationship.SELECT_ID);
            questionMap = Question.getQuestion(context, task, taskSelects,
                    relSelects);
            questionList.add(questionMap);
        }

        return questionList;
    }
    
    
//Added:nr2:Bug#376468:23/06/09
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public HashMap changeRevision(Context context, String[] args)
    throws Exception {
        HashMap returnResult = new HashMap();
        try{
            //Unpack Incoming Arguments
            HashMap programMap = (HashMap)JPO.unpackArgs(args);

            //Get ParamMap
            HashMap paramMap = (HashMap) programMap.get("paramMap");

           //Get Object Id
            String objectId = (String)paramMap.get("newObjectId");
            
            //Define Variables
            String result = "";
            String origName = "";
            String origRev = "";
            String tempName = "tempQuesName";
            
            //Get name,revision of the Question
            MQLCommand mqlcmd = new MQLCommand();
            String cmd = "print bus " + objectId + " select name revision dump |;";
            boolean res = mqlcmd.executeCommand(context,cmd);

            if(!res)
            {
                returnResult.put("Action","CONTINUE");
                returnResult.put("Message","Failure: Object Not Found"); 
                return returnResult;
            }                
            
            result = mqlcmd.getResult();
            StringTokenizer resultToken = new StringTokenizer(result,"|");

            if(resultToken.countTokens() == 2){
                origName = resultToken.nextToken();
                origRev = resultToken.nextToken();
            }
            
            origRev = DomainObject.newInstance(context).getUniqueName("QR",12); 
            cmd = "modify bus " + objectId +" name " + tempName + " revision " + origRev + ";";
            res = mqlcmd.executeCommand(context,cmd);
            //Modify the Object's name again
            
            if(!res){
                returnResult.put("Action","CONTINUE");
                returnResult.put("Message","Failure : Can not modify Question " + origName + " Quesion Already Exist"); 
                return returnResult;
            }
			//Modifed for IR-026261 by ixe
            cmd = "modify bus Question " + tempName + " " + origRev + " name \"" + origName +"\"";
			//Modification for IR-026261 end
            res = mqlcmd.executeCommand(context,cmd);

            if(res){
                returnResult.put("Action","CONTINUE");
                //returnResult.put("Message","Success");
            }else{
                returnResult.put("Action","CONTINUE");
                returnResult.put("Message","Failure : can not modify Question " + tempName + " " + origRev);  
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return returnResult;
    }
//End:Bug#376468   
    
    /**
     * This method returns Maplist which holds either question information related
     * to given project template or task information related to given question.
     * 
     * @param	context 
     * 			The ENOVIA <code>Context</code> object.
     * @param 	argumentArray
     * 			String array which holds either project template or question id.
     * @return	infoMapList
     * 			Maplist which holds question or question task information.
     * 
     * @throws 	FrameworkException		
	 * 			FrameworkException can be thrown in case of method fail to execute. 
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getQuestionORQuestionTaskList(Context context, String[] argumentArray) throws MatrixException {
    	
    	MapList infoMapList = null;
    	
    	StringList busSelectList = new StringList (4);
    	busSelectList.add (SELECT_ID);
    	busSelectList.add (SELECT_NAME);
    	busSelectList.add (SELECT_CURRENT);
    	busSelectList.add (SELECT_POLICY);
    	busSelectList.add (SELECT_DESCRIPTION);
    	
    	StringList relSelectList = new StringList();
    	String busWhere = EMPTY_STRING;
    	String relWhere = EMPTY_STRING;
    	
    	String strRelPattern = DomainConstants.RELATIONSHIP_PROJECT_QUESTION + "," + 
				   			   DomainConstants.RELATIONSHIP_QUESTION;
    	String strTypePattern = DomainConstants.TYPE_QUESTION + "," +
    							DomainConstants.TYPE_TASK_MANAGEMENT;
    	
    	try {
	    	Map<String,String> programMap = JPO.unpackArgs(argumentArray);
	        String objectId = programMap.get("objectId");
	        
	        String strExpandLevel = (String) programMap.get("expandLevel");
	    	short recurseToLevel = ProgramCentralUtil.getExpandLevel(strExpandLevel);
	        
	    	Question question = new Question(objectId);
	    	infoMapList = question.getRelatedObjects(context, strRelPattern, strTypePattern, busSelectList,
													 relSelectList, false, true, recurseToLevel, busWhere, relWhere, 0);	        
    	} catch (Exception exception) {
    		throw new MatrixException(exception);
    	}
        return infoMapList;
    }
    
    
    /**
     * This method populates and returns dropdown which holds 'True' and 'False'.
     * User will select either value while inline editing question.
     *  
     * @param	context 
     * 			The ENOVIA <code>Context</code> object.
     * @param 	argumentArray
	 *			Argument String Array.
     * @return	Map which has dropdown which holds 'True' and 'False'. User will 
     * 			select either value while inline editing question.
     * @throws 	MatrixException		
	 * 			MatrixException can be thrown in case of method fail to execute.
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public Map getQuestionTaskResponseRangeValues(Context context, String[] argumentArray) throws MatrixException {
    	
    	String clientLanguage = context.getLocale().getLanguage();
        Map<String,StringList> returnMap = new HashMap<String,StringList>(2);
        StringList responseActualValueList = new StringList(2);
        StringList responseDisplayValueList = new StringList(2);
    	
    	String actualTrue = ProgramCentralUtil.getPMCI18nString(context,"emxProgramCentral.QuestionResponseRange.TRUE", Locale.US.getLanguage());
    	String actualFalse = ProgramCentralUtil.getPMCI18nString(context,"emxProgramCentral.QuestionResponseRange.FALSE",Locale.US.getLanguage());
    	responseActualValueList.add(actualTrue);
    	responseActualValueList.add(actualFalse);
    	
    	String i18True = ProgramCentralUtil.getPMCI18nString(context,"emxProgramCentral.QuestionResponseRange.TRUE", clientLanguage);
    	String i18False = ProgramCentralUtil.getPMCI18nString(context,"emxProgramCentral.QuestionResponseRange.FALSE", clientLanguage);
    	responseDisplayValueList.add(i18True);
    	responseDisplayValueList.add(i18False);
    	
        returnMap.put("field_choices", responseActualValueList);
        returnMap.put("field_display_choices", responseDisplayValueList);
        
        return returnMap;
    }
    /**
     * This method populates and returns dropdown which holds 'True' and 'False'.
     * User will select either value while connecting tasks to the question.
     *  
     * @param	context 
     * 			The ENOVIA <code>Context</code> object.
     * @param 	argumentArray
	 *			Argument String Array.
     * @return	Map which has dropdown which holds 'True' and 'False'. User will 
     * 			select either value while connecting tasks to the question.
     * @throws 	MatrixException		
	 * 			MatrixException can be thrown in case of method fail to execute.
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public StringList getQuestionResponseOptionList(Context context, String[] args) throws Exception {
    	
    	Map programMap = JPO.unpackArgs(args);
    	MapList objectList = (MapList) programMap.get("objectList");
    	int objectListSize = objectList.size(); 
    	
    	StringList programHTMLStringList = new StringList();
    	
    	String clientLanguage = context.getLocale().getLanguage();
    	String i18True = ProgramCentralUtil.getPMCI18nString(context,"emxProgramCentral.QuestionResponseRange.TRUE", clientLanguage);
    	String i18False = ProgramCentralUtil.getPMCI18nString(context,"emxProgramCentral.QuestionResponseRange.FALSE", clientLanguage);
    	String programHTMLString = "<select name=\"questionResponse\">" +
    							   		"<option value=\"True\">"+i18True+"</option>" +
    							   		"<option value=\"False\">"+i18False+"</option>" +
    							   "</select>";
	    
    	for( int i= 0; i<objectListSize;i++) {
    		programHTMLStringList.add(programHTMLString);
    	}
    	
	    return programHTMLStringList;
    }
    /**
     * This method checks if inline object is of Question, it makes that row
     * editable for the user, else makes that row read-only.
     * 
     * @param	context 
     * 			The ENOVIA <code>Context</code> object.
     * @param 	argumentArray
	 *			Argument String Array which holds id for objects to be rendered.
     * @return	editAccessList
     * 			It holds 'true' as value for question objects for other value ill be false.
     * @throws 	MatrixException		
	 * 			MatrixException can be thrown in case of method fail to execute.
     */
    public StringList makeQuestionCellEditable(Context context, String[] argumentArray) throws MatrixException {

    	try {
            Map programMap = JPO.unpackArgs(argumentArray);
            List<Map<String,String>> objectList = (List<Map<String,String>>) programMap.get("objectList");
            StringList editAccessList = new StringList(objectList.size());
            
        	for(int i=0;i<objectList.size();i++) {
        		String level = objectList.get(i).get("level");
        		editAccessList.add("0".equalsIgnoreCase(level) || "1".equalsIgnoreCase(level));
            } 
            return editAccessList;
            
        } catch (Exception exception) {
            throw new MatrixException(exception);
        }
    }
    /**
     * This method checks if inline object is of Task Management, it makes that row
     * editable for the user, else makes that row read-only.
     * 
     * @param	context 
     * 			The ENOVIA <code>Context</code> object.
     * @param 	argumentArray
	 *			Argument String Array which holds id for objects to be rendered.
     * @return	editAccessList
     * 			It holds 'true' as value for Task Management objects for other value ill be false.
     * @throws 	MatrixException		
	 * 			MatrixException can be thrown in case of method fail to execute.
     */
    public StringList makeTaskCellEditable(Context context, String[] argumentArray) throws MatrixException {
    	
        try {
            Map programMap = JPO.unpackArgs(argumentArray);
            List<Map<String,String>> objectList = (List<Map<String,String>>) programMap.get("objectList");
            StringList editAccessList = new StringList(objectList.size());
            
        	for(int i=0;i<objectList.size();i++) {
        		String level = objectList.get(i).get("level");
        		editAccessList.add("2".equalsIgnoreCase(level));
            } 
            return editAccessList;
            
        } catch (Exception exception) {
            throw new MatrixException(exception);
        }
    }
    /**
     * This method updates attribute of 'Question' relationship when user changes
     * Question response value.
     * 
     * @param	context 
     * 			The ENOVIA <code>Context</code> object.
     * @param 	argumentArray
	 *			Argument String Array which holds id for objects to be rendered.
     * @throws 	MatrixException		
	 * 			MatrixException can be thrown in case of method fail to execute.
     */
    public void updateQuestionResponse(Context context, String[] argumentArray) throws Exception {
    	
    	try {	
    		Map programMap    			= JPO.unpackArgs(argumentArray);
    		Map<String,String> paramMap = (Map)programMap.get("paramMap");
    		String taskId 	= paramMap.get("objectId");
    		String newResponseValue = paramMap.get("New Value");
    			
    		Map<String,String> attributeValueMap = new HashMap<String,String>(1);
    		attributeValueMap.put(ATTRIBUTE_TASK_TRANSFER,newResponseValue);
    		
    		String SELECT_QUESTION_REL_ID = "to["+RELATIONSHIP_QUESTION+"].id";
    		
    		DomainObject taskObject = DomainObject.newInstance(context,TYPE_TASK_MANAGEMENT,PROGRAM);
    		taskObject.setId(taskId);
    		String connectionId = taskObject.getInfo(context,SELECT_QUESTION_REL_ID);
    		
    		DomainRelationship relObject  = new DomainRelationship(connectionId);
    		relObject.setAttributeValues(context, attributeValueMap);
    		
    	} catch (Exception exception) {
    		throw new MatrixException(exception);
    	}
    }
    /**
     * This method connects newly created question with project template and
     * selected tasks of it.
     * 
     * @param	context 
     * 			The ENOVIA <code>Context</code> object.
     * @param 	argumentArray
	 *			Argument String Array which holds id of question,tasks and project template.
     * @throws 	MatrixException		
	 * 			MatrixException can be thrown in case of method fail to execute.
     */
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public void connectQuestionToTask(Context context, String[] argumentArray) throws Exception {
    	
    	try {
    		Map parameterMap = JPO.unpackArgs(argumentArray);
    		Map paramMap = (Map) parameterMap.get("paramMap");
    		String questionId = (String) paramMap.get("objectId");
    		
    		Map<String,String> requestMap = (Map<String,String>)parameterMap.get("requestMap");
        	
    		String projectTemplateId = requestMap.get("projectTemplateId");
        	String questionResponse  = requestMap.get("QuestionResponse");
        	String taskIdString 	 = requestMap.get("taskIdString");        	
        	String[] taskIdArray = taskIdString.split("_"); 
        	String[] questionResponseArray = new String[taskIdArray.length];
        	
        	for(int i=0;i<taskIdArray.length;i++) {
        		questionResponseArray[i]=questionResponse;
        	}

        	RelationshipType projectQuestionRel = new RelationshipType(RELATIONSHIP_PROJECT_QUESTION);
        	
        	Question question = new Question(questionId);
        	question.connectTaskArray(context, taskIdArray, questionResponseArray);//connect Task(s) to Question.
    		question.addFromObject(context,projectQuestionRel,projectTemplateId);//connect Question to Template
    		
    	} catch (Exception exception) {
    		throw new MatrixException(exception);
    	}
    }
    /**
     * This method returns true if user selects project template task/tasks and
     * create new question to assign it.
     * 
     * @param	context 
     * 			The ENOVIA <code>Context</code> object.
     * @param 	argumentArray
	 *			Argument String Array which holds "showQuestionResponse" in it.
	 *
     * @return  returns true if user selects project template task/tasks and
     * 			create new question to assign it.
     * 
     * @throws 	MatrixException		
	 * 			MatrixException can be thrown in case of method fail to execute.
     */
    public boolean isShowQuestionResponse(Context context, String[] argumentArray) throws MatrixException {
    	
        try {
            Map<String,String> programMap = JPO.unpackArgs(argumentArray);
            String showQuestionResponse= programMap.get("showQuestionResponseDD");
            
            return "true".equalsIgnoreCase(showQuestionResponse);
            
        } catch (Exception exception) {
            throw new MatrixException(exception);
        }
    }
}
