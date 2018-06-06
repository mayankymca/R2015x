/*
 * emxTaskWBSSortBase.java
 *
 * Copyright (c) 2005-2015 Dassault Systemes.
 *
 * All Rights Reserved. This program contains proprietary and trade secret
 * information of MatrixOne, Inc. Copyright notice is precautionary only and
 * does not evidence any actual or intended publication of such program.
 *
 * static const char RCSID[] = $Id: /ENOProductLine/CNext/Modules/ENOProductLine/JPOsrc/base/${CLASSNAME}.java 1.3.2.1.1.1 Wed Oct 29 22:17:06 2008 GMT przemek Experimental$
 */

import java.io.*;
import java.util.*;
import matrix.db.*;
import com.matrixone.jsystem.util.MxPattern;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.jsystem.util.MxMatcher;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;

/**
 * The <code>emxRouteTaskSortBase</code> class contains methods for comparision.
 *
 * @author Wipro
 * @version PLC 10-6 - Copyright (c) 2004, MatrixOne, Inc.
 */

public class emxRouteTaskSortBase_mxJPO extends emxCommonBaseComparator_mxJPO
{

	private Context context;
	public static final String SELECT_ROUTE_SEQUENCE =
			DomainObject.getAttributeSelect(DomainConstants.ATTRIBUTE_ROUTE_SEQUENCE);
	public static final String SELECT_ROUTE_ACTION =
			DomainObject.getAttributeSelect(DomainConstants.ATTRIBUTE_ROUTE_ACTION);
    protected static final String EMPTY_STRING = "";

    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since PLC 10-6
     */

    public emxRouteTaskSortBase_mxJPO (Context context, String[] args)
        throws Exception
    {

		this.context = context;

    }

    /**
     * Default Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args    holds no arguments
     * @throws        Exception if the operation fails
     * @since         PLC 10-6
     *
     */

    public emxRouteTaskSortBase_mxJPO ()
    {
		try {
			this.context = ContextUtil.getAnonymousContext();
		} catch (Exception e) {}
    }

    /**
     * Compares two maps based on the key values of the MapComparator.
     * 
     *
     * <p><dl><dt><b>Example:</b><dd><tt>
     * <pre>
     * ArrayList sortKeys = new ArrayList(2);
     * sortKeys.add("name");
     * sortKeys.add("dir");
     * MapComparator mapCompare = new MultilevelMapComparator(sortKeys);
     * HashMap map1 = new HashMap(1);
     * map1.put("name", "1.1");
     * HashMap map2 = new HashMap(1);
     * map2.put("name", "1.2.1");
     * int diff = mapCompare.compare(map1, map2);
     * </pre>
     * </tt></dl>
     *
     * @param object1  The first map to compare.
     * @param object2  The second map to compare.
     * @return         A negative integer, zero, or a positive integer
     *                 as the first argument is less than, equal to,
     *                 or greater than the second.
     * @since PLC 10-6
     */

    public int compare(Object object1, Object object2) {
        int dirMult = 1;
		boolean isNumber1 = true;
		boolean isNumber2 = true;

        // Cast the objects to compare into maps.
        Map map1 = (Map) object1;
        Map map2 = (Map) object2;

        Map sortKeys = getSortKeys();

		String keyName = (String) sortKeys.get("name");
        String keyDir  = (String) sortKeys.get("dir");

        // If the direction is not ascending, then set the
        // multiplier to -1.  Otherwise, set the multiplier to 1.

        if (! "ascending".equals(keyDir)) {
            dirMult = -1;
        } else {
            dirMult = 1;
        }

		String key = EMPTY_STRING;
		if("Assignee".equalsIgnoreCase(keyName)){
			key ="owner";
		}else if("Action".equalsIgnoreCase(keyName)){
			key = SELECT_ROUTE_ACTION;
		}else if("Instructions".equalsIgnoreCase(keyName)){
			key = DomainConstants.SELECT_ROUTEINSTRUCTIONS;
		}else{
			key = SELECT_ROUTE_SEQUENCE;
		}

		int diff = 0; 

		if("DueDate".equalsIgnoreCase(keyName)){
			try{
				String taskDueDate1 = emxInboxTaskBase_mxJPO.getDueDate(context, map1);
				String taskDueDate2 = emxInboxTaskBase_mxJPO.getDueDate(context, map2);

				Date d1 = new Date(taskDueDate1);
				Date d2 = new Date(taskDueDate2);

				diff = UIUtil.isNullOrEmpty(d1.toString()) &&  UIUtil.isNullOrEmpty(d2.toString())  ?  0 : 
					UIUtil.isNullOrEmpty(d1.toString())  ? -1 :
						UIUtil.isNullOrEmpty(d2.toString())  ?  1 : 
							d1.compareTo(d2);
			}catch(Exception ex){}
		}else{
			String strTask1 = (String) map1.get(key);
			String strTask2 = (String) map2.get(key);

			boolean istaskEmpty1 = UIUtil.isNullOrEmpty(strTask1);
			boolean istaskEmpty2 = UIUtil.isNullOrEmpty(strTask2);
  
			isNumber1 = isNumber(strTask1);
			isNumber2 = isNumber(strTask2);

			if(isNumber1 && isNumber2)
			{
				int  firstToken = Integer.parseInt(strTask1);
				int  secondToken = Integer.parseInt(strTask2);

            if(firstToken == secondToken) {
					diff = 0;
            } else if (firstToken > secondToken) {
					diff = 1;
            } else {
					diff = -1;
				}
			}else{
				try {
					diff = istaskEmpty1 && istaskEmpty2 ?  0 :         
						istaskEmpty1 ? -1 :  
							istaskEmpty2 ?  1 :      
								strTask1.compareToIgnoreCase(strTask2);
				} catch (Exception e) {
					throw new RuntimeException(e.getMessage());
				} 
			}
            }
        
		return diff * dirMult;
    }

	private boolean isNumber(String task){    	   	
		MxPattern p = MxPattern.compile(".*\\D.*");
		p.matcher(task).matches();
		if(!p.matcher(task).matches()){
			return true;
		}else{
			return false;
		}
    }

}
