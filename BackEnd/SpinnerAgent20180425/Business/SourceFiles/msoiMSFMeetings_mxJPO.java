/*   msoiMSFMeetings
*   This JPO contains the implementation of msoiMSFMeetings
*
* @quickreview 14:04:30 SJ7 HL for Outlook Meetings integrations. Changes for that
*
*/

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Set;
import java.lang.Object;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.db.RelationshipItr;
import matrix.db.RelationshipType;

import matrix.util.List;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;

import com.matrixone.apps.common.Company;
import com.matrixone.apps.common.Document;
import com.matrixone.apps.common.Meeting;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.common.Search;
import com.matrixone.apps.common.Message;
import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.DateUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UITableIndented;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.jdom.Element;

//includes for LCO Update
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;
import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerLogger;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;

public class msoiMSFMeetings_mxJPO extends emxDomainObject_mxJPO
{
	private static final String MQL_MEETING_AGENDA_ID = "print bus $1 select $2 dump";
	private static final String MQL_AGENDA_MODIFIED = "print connection $1 select modified dump";
	private static final String MQL_PERSON_DETAILS = "print person \"$1\" select \"$2\" dump";
	private String syncDateTime = null;
	private String[] initArgs = null;
	private String language = null;
	private String clientOffSet = "";
	private Date clientDate = null; 
	private SimpleDateFormat serverDateFormat = null;
	
	public msoiMSFMeetings_mxJPO (Context context, String[] args) throws Exception
	{
		super(context, args);
		initArgs = args;
	}
		
	//SJ7+ TODO : Modify this function to get a list of all Meetings from BPS based on date
	//Add conditions on expandTypeWhere and add an OR clause
	public MapList getMeetingSummaryListByDate(Context context,String[] args) throws Exception
	{
		//Change the where clauses for getting all the objects more than the sync date
		StringBuffer relPattern = new StringBuffer("");
		relPattern.append(DomainConstants.RELATIONSHIP_MEETING_CONTEXT);
		relPattern.append(",");
		relPattern.append(DomainConstants.RELATIONSHIP_ASSIGNED_MEETINGS);

		MapList MeetingList = new MapList();

		StringList select = new StringList();
		select.addElement(DomainConstants.SELECT_ID);
		select.addElement(DomainConstants.SELECT_POLICY);
		//Added:30-Apr-2010:di1:R210:PRG:Meeting Usability
		select.addElement(DomainConstants.SELECT_NAME);
		select.addElement(DomainConstants.SELECT_TYPE);
		select.addElement(DomainConstants.SELECT_OWNER);
		select.addElement(DomainConstants.SELECT_CURRENT);
		select.addElement(DomainConstants.SELECT_DESCRIPTION);
		select.addElement("attribute["+ DomainConstants.ATTRIBUTE_MEETING_DURATION + "]");
		select.addElement("attribute["+ DomainConstants.ATTRIBUTE_MEETING_START_DATETIME + "]");
		//Addition End:30-Apr-2010:di1:R210:PRG:Meeting Usability
		//Added:13-May-2014:SJ7:R417:MSF:Outlook Meeting Integration
		select.addElement("attribute["+ DomainConstants.ATTRIBUTE_MEETING_LOCATION + "]");
		select.addElement("attribute["+ DomainConstants.ATTRIBUTE_TITLE + "]");
		//Addition End:13-May-2014:SJ7:R417:MSF:Outlook Meeting Integration
		

		com.matrixone.apps.common.Person busPerson=com.matrixone.apps.common.Person.getPerson(context);
		busPerson.open(context);
		String personid = busPerson.getId();
		DomainObject personDom = DomainObject.newInstance(context, personid);

		StringBuffer expandTypeWhere = new StringBuffer();
		expandTypeWhere.append("('").append(DomainConstants.SELECT_OWNER).append("' == '").append(busPerson.getName());
		expandTypeWhere.append("' || '");
		expandTypeWhere.append(DomainObject.SELECT_CURRENT).append("' != 'Create')");

		MeetingList = personDom.getRelatedObjects(context,
			relPattern.toString(),               //String relPattern
			DomainConstants.TYPE_MEETING,              //String typePattern
			select,          //StringList objectSelects,
			null,                     //StringList relationshipSelects,
			false,                    //boolean getTo,
			true,                     //boolean getFrom,
			(short)0,                 //short recurseToLevel,
			expandTypeWhere.toString(),          //String objectWhere,
			"",                       //String relationshipWhere,
			0,
			null,        //Pattern includeType,
			null,                     //Pattern includeRelationship,
			null);                    //Map includeMap
		// MeetingList = DomainObject.findObjects(context,DomainConstants.TYPE_MEETING,"*","*","*","eService Production","",true,select);
		return MeetingList;

	}
	
	//SJ7+ Changes for Outlook Meetings Integration
	private MapList invokeJPO(Context context, String[] args, String programName, String functionName, String tableName) throws Exception
	{
		
		String hyperLink = "";
		MapList returnList = new MapList();
		
		try{
			//Call the JPO program and its function
			returnList = (MapList)JPO.invoke(context, programName, null, functionName, args, MapList.class);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw (new MatrixException(programName + ":" + functionName + "=>" + ex.toString()) );
		}

		return returnList;
	}
	
	
	//SJ7+ Check Modified Date Time
	private boolean checkIfDateGreater(Context context, String serverDateString) throws Exception
	{
		try{
			Date serverDate = serverDateFormat.parse(serverDateString);
			
			if(serverDate.getTime() > clientDate.getTime())
			{
				return true;
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return false;
	}
	
	//SJ7+ Checks if the meeting is modified
	private boolean checkMeetingModifiedDateTime(Context context, String meetingObjectId) throws Exception
	{
		try{
			//Get the dates
			Meeting busMeeting = new Meeting(meetingObjectId);
			busMeeting.open(context);
			
			String modifiedDateTime = (String)busMeeting.getInfo(context, DomainConstants.SELECT_MODIFIED);
			
			return checkIfDateGreater(context, modifiedDateTime);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return false;
	}
	
	//SJ7+ UPdated Meetings will be computed in this function and those will be returned
	//TODO : Kill the 2 FOR Loops ... not Good!!
	//TODO : Client and server Date time issues (both should be of the same base) This will be resolved with the timeZone offset issue. Need to check with IEF on the same.
	public MapList getUpdatedMeetings(Context context, String[] args) throws Exception
	{
		//define the output MapList
		MapList updatedMeetings = new MapList();
		
		try{
			HashMap initTable = (HashMap)JPO.unpackArgs(args);
			String[] clientArgs = (String[])initTable.get("args");
			language = (String)initTable.get("languageStr");

			String syncDateTime = (String)clientArgs[4];
			clientOffSet = (String)clientArgs[5];
			
			String dateFormatString = (String)clientArgs[8];
			//Create the Client date format
			SimpleDateFormat clientDateFormat = new SimpleDateFormat(dateFormatString);
			TimeZone clientTimeZone = TimeZone.getTimeZone("UTC");
			clientTimeZone.setRawOffset(Integer.parseInt(clientOffSet));
			clientDateFormat.setTimeZone(clientTimeZone);
			
			clientDate = clientDateFormat.parse(syncDateTime);
			
			//Create the server Date format
			serverDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat());
			TimeZone serverTimeZone = TimeZone.getTimeZone("UTC");
			int serverTimeZoneOffset = TimeZone.getDefault().getOffset(new Date().getTime());
			serverTimeZone.setRawOffset(serverTimeZoneOffset);
			serverDateFormat.setTimeZone(serverTimeZone);
			
			MapList meetingList = getMeetingSummaryList(context, args);
			
			String meetingObjectId = "";
			String[] agendaIds = null;
			boolean isMeetingUpdated = false;
			Map meetingMap = null;
			
			for(Iterator iter = meetingList.iterator(); iter.hasNext();) {
				meetingMap = (Map) iter.next();
				meetingObjectId = (String) meetingMap.get("id");
				
				isMeetingUpdated = checkMeetingModifiedDateTime(context, meetingObjectId);
				
				if(isMeetingUpdated == true)
				{
					updatedMeetings.add(meetingMap);
				}
				else
				{
					//build the mql command for getting the agenda items
					String agendaIdString = MqlUtil.mqlCommand(context, MQL_MEETING_AGENDA_ID, meetingObjectId, "from[Agenda Item].id").trim();
					
					if((!agendaIdString.isEmpty()) || (agendaIdString != null))
					{
						if(agendaIdString.contains(","))
						{
							agendaIds = agendaIdString.split(",");
							isMeetingUpdated = getIfAgendaUpdated(context, agendaIds);
						}
						else if(!agendaIdString.isEmpty())
						{
							agendaIds = new String[1];
							agendaIds[0] = agendaIdString;
							isMeetingUpdated = getIfAgendaUpdated(context, agendaIds);
						}
					}
					
					if(isMeetingUpdated == true)
						updatedMeetings.add(meetingMap);
				}
			}
			
			//Update LCO
			updateLCOValue(context, initArgs, "MSF-MeetingsLastSyncDateTime", syncDateTime);

			return updatedMeetings;
			//print bus Meeting M-0000100 "" select from[Agenda Item].id dump;
			//print connection 54352.46979.41472.45860 select modified dump;
			
			//1. Get All Agenda IDs from the Meeting ID (This would be for every meeting)
			//2. For every Agenda ID, get the modified date time
			//3. Check if modified date time is greater than today
			//4. if greater than today, get the basic meeting details (a la getMeetingSummaryList
			//5. Return list of all Meeting IDs and send it back
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return updatedMeetings;
	}
	
	//return true if the meeting agenda modified date is greater than the passed date
	//TODO : Get rid of the MQL statement and try to get the data from the Relationship map of the Meeting object. R & D of the same is pending
	private boolean getIfAgendaUpdated(Context context, String[] agendaIds) throws Exception
	{
		try{
			String mqlString = "";
			String modifiedDateTime = "";

			if(agendaIds.length > 0){
				for(int i=0; i<agendaIds.length; i++){
					modifiedDateTime = MqlUtil.mqlCommand(context, MQL_AGENDA_MODIFIED, agendaIds[i]);

					if(checkIfDateGreater(context, modifiedDateTime) == true){
						return true;
					}
				}
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return false;
	}
	
	//SJ7+ Get a list of all Meetings
	public MapList getMeetingSummaryList(Context context, String[] args) throws Exception
	{
		return invokeJPO(context, args, "emxMeeting", "getMeetingSummaryList", "APPMeetingSummary");
	}
	
	//SJ7+ Get the List of all Meeting Agendas
	public MapList getMeetingAgendaItems(Context context, String[] args) throws Exception 
	{
		return invokeJPO(context, args, "emxMeeting", "getMeetingAgendas", "APPMeetingAgendaSummary");
	}
	
	//SJ7+ Gets the Meeting Attendee for meeting object provided
	public MapList getMeetingAttendee(Context context, String[] args)throws Exception
	{
		return invokeJPO(context, args, "emxMeeting", "getMeetingttendee", "APPMeetingAttendee");
	}
	
	//SJ7+ Get Decisions for a particular meeting
	public MapList getRelatedDecisions(Context context, String[] args) throws Exception
	{
		return invokeJPO(context, args, "emxDecision", "getRelatedDecisions", "APPDecisionsList");
	}
	
	//SJ7+ Get Discussions for Every Decision
	public MapList getDiscussions(Context context, String[] args) throws Exception
	{
		return invokeJPO(context, args, "emxDiscussion", "getDiscussionList", "APPDiscussionsList");
	}
	
	//SJ7+ Promote the meeting object
	private int PromoteMeeting(Context context, String[] args) throws Exception
	{
		try
		{
			//Since the function being called is void, this should call the invokePrivate and not <T> T invoke
			//args should have a meetingId in it as "objectId".
			return JPO.invoke(context, "emxMeeting", null, "promoteMeeting", args);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		return -1;
	}
	
	//SJ7+ Demote the meeting object
	private int DemoteMeeting(Context context, String[] args) throws Exception
	{
		try
		{
			//Since the function being called is void, this should call the invokePrivate and not <T> T invoke
			//args should have a meetingId in it as "objectId".
			return JPO.invoke(context, "emxMeeting", null, "demoteMeeting", args);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		return -1;
	}
	
	//SJ7+ Get Attribute for a meeting
	public String getAttribute(Context context, String[] args, String attributeName) throws Exception 
	{
		String attributeValue = "";
		try 
		{
			HashMap requestMap = (HashMap) JPO.unpackArgs(args);
			HashMap requestMap2 = (HashMap)requestMap.get("requestMap");
			String strMeetingId = (String)requestMap2.get("objectId");
			
			DomainObject MeetingObj = DomainObject.newInstance(context);
			MeetingObj.setId(strMeetingId);
			attributeValue = MeetingObj.getInfo(context, "attribute["+ attributeName +"]");
		}
		catch (Exception ex) 
		{
			ex.printStackTrace();
			throw new MatrixException(ex);
		}
		return attributeValue;
	}
	
	//SJ7+ Get Location for the meeting
	public String getLocation(Context context, String[] args) throws Exception
	{
		return getAttribute(context, args, "DomainConstants.ATTRIBUTE_MEETING_LOCATION");
	}
	
	//SJ7++ Get Owner's email address
	public Vector getMeetingOwnerEmailAddress(Context context, String[] args) throws Exception
	{
		try{
			// Get object list information from packed arguments
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			MapList objectList = (MapList) programMap.get("objectList");
			
			return getOwnerDetails(context, objectList, "email");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new MatrixException(ex);
		}
	}
	
	//SJ7++ Get Owner's Full Name
	public Vector getMeetingOwnerFullName(Context context, String[] args) throws Exception
	{
		try{
			// Get object list information from packed arguments
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			MapList objectList = (MapList) programMap.get("objectList");

			return getOwnerDetails(context, objectList, "fullname");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new MatrixException(ex);
		}
	}
	
	//SJ7+ Get Details of Owner
	private Vector getOwnerDetails(Context context, MapList objectList, String property) throws Exception
	{
		Vector vecResult = new Vector();
		Map mapRowData = null;
		String owner = null;
		String mqlString = null;
		String columnData = null;
		
		for (Iterator itrObjects = objectList.iterator(); itrObjects
		  .hasNext();) {
			mapRowData = (Map) itrObjects.next();
			try{
				owner = (String) mapRowData.get(SELECT_OWNER);
				if (null != owner && "" != owner)
				{
					columnData = MqlUtil.mqlCommand(context, MQL_PERSON_DETAILS, owner, property);
				}
				else
					columnData = "";
			}
			catch(Exception ex)
			{
				columnData = "";
			}
			vecResult.add(columnData);
		}
		
		return vecResult;
	}
	
	//SJ7+ Update the LCO
	private void updateLCOValue(Context context, String[] args, String lcoAttribute, String lcoAttributeValue) throws Exception
	{
		try{
			MCADMxUtil util = null;
			Hashtable initArgsTable = (Hashtable)JPO.unpackArgs(args);
			MCADLocalConfigObject localConfigObject = (MCADLocalConfigObject)initArgsTable.get("lco");
			//TODO: language is coming as empty. check that
			String localeLanguage = language;
			IEFGlobalCache cache = new IEFGlobalCache();
			MCADServerResourceBundle serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
			util = new MCADMxUtil(context, serverResourceBundle, cache);
			
			util.setLCOAttribute(context, localConfigObject, lcoAttribute, lcoAttributeValue);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	* Returns the Start Date value for Meeting and its related object in
	* APPMeetingSummary
	*
	* @param context
	* Matrix Context object
	* @param args
	* String array
	* @return vector holding mentioned values
	* @throws Exception
	* if operation fails
	*/
	public Vector getColumnMeetingDateData(Context context, String[] args) throws Exception 
	{
		try {
			// Create result vector
			Vector vecResult = new Vector();
			// Get object list information from packed arguments
			HashMap programMap = (HashMap) JPO.unpackArgs(args);
			MapList objectList = (MapList) programMap.get("objectList");
			double clientTZOffset = Double
				.parseDouble((String) ((Map) programMap.get("paramList"))
					.get("timeZone"));

			Map mapRowData = null;
			String strObjectId = null;
			String strColumnValues = null;
			for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) 
			{
				mapRowData = (Map) itrObjects.next();
				String strObjectType = (String) mapRowData.get(SELECT_TYPE);

				if (strObjectType.equals(TYPE_MEETING)) 
				{
					strObjectId = (String) mapRowData.get(SELECT_ID);
					vecResult.add(getFormattedMeetingStartDate(context, new Meeting(strObjectId), clientTZOffset));
				} 
				else 
				{
					vecResult.add("");
				}
			}
			return vecResult;
		} 
		catch (Exception ex) 
		{
			ex.printStackTrace();
			throw new MatrixException(ex);
		}
	}

	/**
	 * This will return the meeting start date in the user preference time zone.
	 * @param context
	 * @return meeting start date in the user preference time zone.
	 * @throws FrameworkException
	 */
	private String getFormattedMeetingStartDate(Context context, Meeting meetingObj, double clientTZOffset) throws FrameworkException 
	{
		StringList formattedDisplayDateTimeList = FrameworkUtil.split(getFormattedDisplayMeetingStartDateTime(context, meetingObj, clientTZOffset), " ");
		return (String)formattedDisplayDateTimeList.get(0) + " " + 
		(String)formattedDisplayDateTimeList.get(1)+ " " + 
		(String)formattedDisplayDateTimeList.get(2);
	}

	/**
	* This method returns the meeting date and time in 'MMM DD, YYYY HH:MM:SS a' from
	* e.g. 5/8/2009 18:30:00 will be return as 'May 8, 2009 6:30:00 PM'
	* @param context
	* @param meetingObj
	* @param clientTZOffset
	* @return
	* @throws FrameworkException
	*/
	private String getFormattedDisplayMeetingStartDateTime(Context context, Meeting meetingObj, double clientTZOffset) throws FrameworkException 
	{
		String strDate = meetingObj.getInfo(context, "attribute["+DomainConstants.ATTRIBUTE_MEETING_START_DATETIME+"]");
		//Here we are passing locale=Locale.ENGLISH and dateformat = DateFormat.MEDIUM
		//This is to get the date time string allways in same format. i.e. in  'MMM DD, YYYY HH:MM:SS a' format.
		//Otherwise it will return different format for each locale.
		String strFormattedDisplayDateTime = eMatrixDateFormat.getFormattedDisplayDateTime(context, strDate, true, DateFormat.MEDIUM, clientTZOffset, Locale.ENGLISH);
		//Formatted Meeting date will be in the form of 'MMM DD, YYYY HH:MM:SS a' take only time part from this
		//e.g if the date is 'May 8, 2009 6:30:00 PM', take only 6:30:00 PM from this.
		return strFormattedDisplayDateTime;
	}

	public String getMeetingStartTime(Context context,String[] args) throws Exception 
	{
		  HashMap programMap = (HashMap)JPO.unpackArgs(args);
		  HashMap requestMap = (HashMap) programMap.get("requestMap");
		  String strMeetingId = (String)requestMap.get("objectId");
		  String strMode = (String)requestMap.get("Mode");
		  
		  StringBuffer strMeetingStartTime = new StringBuffer();
		  
		  if(!UIUtil.isNullOrEmpty(strMode) && strMode.equals("create")){
				strMeetingStartTime.append("8:30:00 AM");
		  } else if(!UIUtil.isNullOrEmpty(strMeetingId)){			  
			  double clientTZOffset  = Double.parseDouble("0.0");		
			  String strClientTZone = Double.toString(clientTZOffset);			  		  
			  strMeetingStartTime.append(getFormattedMeetingStartTime(context, new Meeting(strMeetingId), clientTZOffset, false));
		  }		  
		  return strMeetingStartTime.toString();
    }

	private String getFormattedMeetingStartTime(Context context, Meeting meetingObj, double clientTZOffset, boolean trimSeconds) throws FrameworkException 
	{
        StringList formattedDisplayDateTimeList = FrameworkUtil.split(getFormattedDisplayMeetingStartDateTime(context, meetingObj, clientTZOffset), " ");
        String time = (String)formattedDisplayDateTimeList.get(3);
        String aa = (String)formattedDisplayDateTimeList.get(4);
        time = trimSeconds ? time.substring(0, time.lastIndexOf(':')) : time;
        return  time + " " + aa;
    }
}
