/*   emxWorkCalendarBase
 **
 **   Copyright (c) 2003-2015 Dassault Systemes.
 **   All Rights Reserved.
 **   This program contains proprietary and trade secret information of MatrixOne,
 **   Inc.  Copyright notice is precautionary only
 **   and does not evidence any actual or intended publication of such program
 **
 **   This JPO contains the implementation of emxWorkCalendar
 **
 **   static const char RCSID[] = $Id: ${CLASSNAME}.java.rca 1.9.2.2 Thu Dec  4 07:55:10 2008 ds-ss Experimental ${CLASSNAME}.java.rca 1.9.2.1 Thu Dec  4 01:53:19 2008 ds-ss Experimental ${CLASSNAME}.java.rca 1.9 Wed Oct 22 15:49:37 2008 przemek Experimental przemek $
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.apps.common.Company;
import com.matrixone.apps.common.WorkCalendar;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.program.ProgramCentralConstants;

/**
 * The <code>emxWorkCalendarBase</code> class contains methods for emxWorkCalendar.
 *
 * @version PMC 10.5.1.2 - Copyright(c) 2004, MatrixOne, Inc.
 */

public class emxWorkCalendarBase_mxJPO extends emxDomainObject_mxJPO
{

	/**
	 * Constructor.
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args holds no arguments
	 * @throws Exception if the operation fails
	 * @since PMC 10.5.1.2
	 */

	public emxWorkCalendarBase_mxJPO (Context context, String[] args)
	throws Exception
	{
		super(context, args);
	}

	/**
	 * This method is executed if a specific method is not specified.
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args holds no arguments
	 * @return an integer: 0 for success and non-zero for failure
	 * @throws Exception if the operation fails
	 * @since PMC 10.5.1.2
	 */

	public int mxMain(Context context, String[] args)
	throws Exception
	{
		if (true)
		{
			throw new Exception("must specify method on emxWorkCalendar invocation");
		}
		return 0;
	}


	/**
	 * Return the locations/business units either associated or not
	 * associated to the calendar
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args contains a Map with the following entries:
	 *    objectId - the context calendar object
	 * @return MapList containing objects for search result
	 * @throws Exception if the operation fails
	 * @since PMC 10.5.1.2
	 */

	@com.matrixone.apps.framework.ui.ProgramCallable
	public MapList getAvailableLocations(Context context , String[] args)
	throws Exception
	{

		StringList busSelects = new StringList( 3 );
		busSelects.add( SELECT_ID );
		busSelects.add( SELECT_TYPE );
		busSelects.add( SELECT_NAME );

		StringList relSelects = new StringList( 1 );
		relSelects.add( "id[connection]" );

		Map programMap = (Map) JPO.unpackArgs(args);
		String objectId = (String)programMap.get( "objectId" );

		this.setId( objectId );

		// Get locations already connected to the context calendar
		MapList ml = this.getRelatedObjects(context,
				PropertyUtil.getSchemaProperty( context, "relationship_Calendar" ),
				PropertyUtil.getSchemaProperty( context, "type_Location" ),
				busSelects,
				relSelects,
				true,
				false,
				(short) 1,
				null,
				null);
		ml.addSortKey(SELECT_TYPE, "ascending", "string");
		ml.addSortKey(SELECT_NAME, "ascending", "string");
		ml.sort();


		//Retrieve locations not connected to the calendar
		Company company = (Company) DomainObject.newInstance( context, PropertyUtil.getSchemaProperty( context, "type_Company" ), DomainConstants.PROGRAM );
		company.setId( getInfo( context, "to[" + PropertyUtil.getSchemaProperty( context, "relationship_CompanyCalendar" ) + "].businessobject.id" ) );

		MapList retml = new MapList();

		Iterator itr;
		Map map;
		HashSet set = new HashSet();
		for (itr = ml.iterator(); itr.hasNext(); )
		{
			map = (Map) itr.next();
			set.add(map.get(SELECT_ID));
		}

		ml = company.getLocations( context, busSelects, null );
		for (itr = ml.iterator(); itr.hasNext(); )
		{
			map = (Map) itr.next();
			if (!set.contains(map.get(SELECT_ID)))
			{
				retml.add(map);
			}
		}

		retml.addSortKey(SELECT_TYPE, "ascending", "string");
		retml.addSortKey(SELECT_NAME, "ascending", "string");
		retml.sort();
		return retml;
	}

	/**
	 * Return the Frequency of the Event
	 * associated to the calendar
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args contains a Map with the following entries:
	 *    objectId - the context calendar object
	 * @return Vector Containing Frequency as string
	 * @throws Exception if the operation fails
	 * @since PMC 10.5.1.2
	 */
	public Vector getFrequency(Context context, String args[])
	throws Exception
	{
		HashMap paramMap = (HashMap)JPO.unpackArgs(args);
		String objectId = (String) paramMap.get("objectId");

		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList relBusObjPageList =(MapList)programMap.get("objectList");
		String attribute_Frequency=(String)PropertyUtil.getSchemaProperty(context,"attribute_Frequency");

		Vector columnValues = new Vector(relBusObjPageList.size());
		String id = "";
		String sRetValue = "";

		Map paramList = (Map)programMap.get("paramList");
		String strLanguage = (String)paramList.get("languageStr");
		i18nNow i18nnow = new i18nNow();

		for (int i = 0; i < relBusObjPageList.size(); i++)
		{
			try{
				id =(String)((HashMap)relBusObjPageList.get(i)).get("id[connection]");
			}catch (Exception ex) {
				id =(String)((Hashtable)relBusObjPageList.get(i)).get("id[connection]");
			}
			if (id != null && id.trim().length() > 0 ){
				DomainRelationship bus  = new DomainRelationship(id);
				try{
					bus.open(context);
				}catch (FrameworkException Ex) {
					throw Ex;
				}
				String sFreq = bus.getAttributeValue(context,attribute_Frequency);
				if(sFreq != null && sFreq.equalsIgnoreCase("0")){
					String NonRecurrence = EnoviaResourceBundle.getProperty(context, "ProgramCentral", 
							"emxProgramCentral.Calendar.Frequency_NonRecurrence", strLanguage);
					columnValues.add(NonRecurrence);
				}
				else if(sFreq != null && sFreq.equalsIgnoreCase("1")){

					String Weekly = EnoviaResourceBundle.getProperty(context, "ProgramCentral", 
							"emxProgramCentral.Calendar.Frequency_Weekly", strLanguage);
					columnValues.add(Weekly);
				}
				else if(sFreq != null && sFreq.equalsIgnoreCase("2")){
					String Monthly = EnoviaResourceBundle.getProperty(context, "ProgramCentral", 
							"emxProgramCentral.Calendar.Frequency_Monthly", strLanguage);
					columnValues.add(Monthly);
				}
				else if(sFreq != null && sFreq.equalsIgnoreCase("3")){
					String Yearly = EnoviaResourceBundle.getProperty(context, "ProgramCentral", 
							"emxProgramCentral.Calendar.Frequency_Yearly", strLanguage);
					columnValues.add(Yearly);
				}
			}
		}
		return columnValues;
	}

	/**
	 * Return the Day of the Week
	 * according to attribute Frequency value
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args contains a Map with the following entries:
	 *    objectId - the context calendar object
	 * @return Vector Containing Day as string
	 * @throws Exception if the operation fails
	 * @since PMC 10.5.1.2
	 */
	public Vector getDayNumber(Context context, String args[])throws Exception
	{
		HashMap paramMap = (HashMap)JPO.unpackArgs(args);
		String objectId = (String) paramMap.get("objectId");
		HashMap programMap = (HashMap) JPO.unpackArgs(args);
		MapList relBusObjPageList =(MapList)programMap.get("objectList");
		String attribute_DayNumber=(String)PropertyUtil.getSchemaProperty(context,"attribute_DayNumber");
		String attribute_Frequency=(String)PropertyUtil.getSchemaProperty(context,"attribute_Frequency");
		Vector columnValues = new Vector(relBusObjPageList.size());
		String id = "";
		String sRetValue = "";

		for (int i = 0; i < relBusObjPageList.size(); i++)
		{
			try
			{
				id =(String)((HashMap)relBusObjPageList.get(i)).get("id[connection]");
			}
			catch (Exception ex) 
			{
				id =(String)((Hashtable)relBusObjPageList.get(i)).get("id[connection]");
			}

			if (id != null && id.trim().length() > 0 )
			{
				DomainRelationship bus  = new DomainRelationship(id);
				try{
					bus.open(context);
				}catch (FrameworkException Ex) {
					throw Ex;
				}
				String d_number = bus.getAttributeValue(context,attribute_DayNumber);
				String d_freq = bus.getAttributeValue(context,attribute_Frequency);

				//Modified:15-Feb-2011:hp5:R211:PRG:IR-093751V6R2012
				String sLanguage = context.getSession().getLanguage();
				String dayNoKey = "emxProgramCentral.Calendar.";
				String convertedDay = "";

				if(d_freq != null && d_freq.equals("1"))
				{
					if(d_number != null && d_number.equalsIgnoreCase("1"))
					{
						convertedDay = EnoviaResourceBundle.getProperty(context, "ProgramCentral", 
								dayNoKey+ProgramCentralConstants.ATTRIBUTE_SUNDAY, sLanguage);
						columnValues.add(convertedDay);
					}
					else if(d_number != null && d_number.equalsIgnoreCase("2"))
					{
						convertedDay = EnoviaResourceBundle.getProperty(context, "ProgramCentral", 
								dayNoKey+ProgramCentralConstants.ATTRIBUTE_MONDAY, sLanguage);
						columnValues.add(convertedDay);
					}
					else if(d_number != null && d_number.equalsIgnoreCase("3"))
					{
						convertedDay = EnoviaResourceBundle.getProperty(context, "ProgramCentral", 
								dayNoKey+ProgramCentralConstants.ATTRIBUTE_TUESDAY, sLanguage);
						columnValues.add(convertedDay);
					}
					else if(d_number != null && d_number.equalsIgnoreCase("4"))
					{
						convertedDay = EnoviaResourceBundle.getProperty(context, "ProgramCentral", 
								dayNoKey+ProgramCentralConstants.ATTRIBUTE_WEDNESDAY, sLanguage);
						columnValues.add(convertedDay);
					}
					else if(d_number != null && d_number.equalsIgnoreCase("5"))
					{
						convertedDay = EnoviaResourceBundle.getProperty(context, "ProgramCentral", 
								dayNoKey+ProgramCentralConstants.ATTRIBUTE_THURSDAY, sLanguage);
						columnValues.add(convertedDay);
					}
					else if(d_number != null && d_number.equalsIgnoreCase("6"))
					{
						convertedDay = EnoviaResourceBundle.getProperty(context, "ProgramCentral", 
								dayNoKey+ProgramCentralConstants.ATTRIBUTE_FRIDAY, sLanguage);
						columnValues.add(convertedDay);
					}
					else if(d_number != null && d_number.equalsIgnoreCase("7"))
					{
						convertedDay = EnoviaResourceBundle.getProperty(context, "ProgramCentral", 
								dayNoKey+ProgramCentralConstants.ATTRIBUTE_SATURDAY, sLanguage);
						columnValues.add(convertedDay);
					}
				}else{
					columnValues.add(d_number);
				}
			}
		}
		return columnValues;
	}

	/**
	 * Return the calendars associated to a company
	 *
	 * @param Context the eMatrix <code>Context</code> object
	 * @param args contains a Map with the following entries:
	 *    objectId - the context company object
	 * @return MapList containing the id of calendar objects
	 * @throws Exception if operation fails
	 * @since PMC 10.6
	 */
	@com.matrixone.apps.framework.ui.ProgramCallable
	public static MapList getCalendar(Context context,String[] args) throws Exception
	{
		HashMap paramMap = (HashMap)JPO.unpackArgs(args);
		MapList objectList = new MapList();

		String objectId = (String)paramMap.get("objectId");

		StringList busSelects = new StringList(1);
		busSelects.add(DomainConstants.SELECT_ID);

		com.matrixone.apps.common.Company company =
			(com.matrixone.apps.common.Company) DomainObject.newInstance( context, objectId);
		objectList = WorkCalendar.getCalendars(context, company, busSelects);

		return objectList;
	}

	/**
	 * Return the events associated to a calendar
	 *
	 * @param Context the eMatrix <code>Context</code> object
	 * @param args contains a Map with the following entries:
	 *    objectId - the context calendar object
	 * @return MapList containing the id of event objects
	 * @throws Exception if operation fails
	 * @since PMC 10.6
	 */
	@com.matrixone.apps.framework.ui.ProgramCallable
	public static MapList getEvents(Context context,String[] args) throws Exception
	{
		HashMap paramMap = (HashMap)JPO.unpackArgs(args);
		MapList objectList = new MapList();
		String objectId = (String)paramMap.get("objectId");

		String RELATIONSHIP_CALENDAR_EVENT = PropertyUtil.getSchemaProperty(context,"relationship_CalendarEvent");

		StringList busSelects = new StringList(1);
		busSelects.add(DomainConstants.SELECT_ID);

		StringList relSelects = new StringList(1);
		relSelects.add(DomainRelationship.SELECT_ID);

		com.matrixone.apps.common.WorkCalendar workcalendar =
			(com.matrixone.apps.common.WorkCalendar) DomainObject.newInstance( context, objectId);
		objectList = workcalendar.getRelatedObjects(context,RELATIONSHIP_CALENDAR_EVENT, "*", busSelects, relSelects, false, true, (short)1, "", "", null, null, null);

		return objectList;
	}

}
