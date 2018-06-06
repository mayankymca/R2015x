/*
** DSCListDateSelection.java
** Created on May 15, 2008
** Dassault Systemes, 1993  2007. All rights reserved.
** All Rights Reserved
** This program contains proprietary and trade secret information of
** Dassault Systemes.  Copyright notice is precautionary only and does
** not evidence any actual or intended publication of such program.
*/

import java.util.HashMap;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADRecentlyAccessedPartsHelper;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.domain.util.i18nNow;

public class DSCListDateSelection_mxJPO
{
	private MCADServerResourceBundle	serverResourceBundle	= null;
	private IEFGlobalCache		cache			= null;
	private MCADMxUtil			mxUtil			= null;

	/**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails     
     */
	public DSCListDateSelection_mxJPO(Context context, String[] args)	throws Exception
	{		
	}

    /**
     * This method is executed if a specific method is not specified.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @return int 0, status code.
     * @throws Exception if the operation fails     
     */
    public int mxMain(Context context, String[] args)
        throws Exception
    {
        if (!context.isConnected())
            throw new Exception("not supported on desktop client");
        return 0;
    }

	public HashMap getListOfDates(Context context, String[] args) throws Exception
	{
		HashMap dateSelectionMap = new HashMap();

		HashMap programMap	= (HashMap) JPO.unpackArgs(args);
		HashMap requestMap	= (HashMap) programMap.get("requestMap");
		HashMap paramMap	= (HashMap) programMap.get("paramMap");

		String dateSelected		= (String) requestMap.get("dateSelected");
		String languageStr		= (String) paramMap.get("languageStr");
		   
		StringList fieldRangeValues			= new StringList();
		StringList fieldDisplayRangeValues	= new StringList();

		HashMap tempMap = new HashMap(6);
		tempMap.put(MCADRecentlyAccessedPartsHelper.TODAY, i18nNow.getI18nString("emxIEFDesignCenter.Common.Today", "emxIEFDesignCenterStringResource", languageStr));
		tempMap.put(MCADRecentlyAccessedPartsHelper.LAST_SEVEN_DAYS, i18nNow.getI18nString("emxIEFDesignCenter.Common.LastSevenDays", "emxIEFDesignCenterStringResource", languageStr));
		tempMap.put(MCADRecentlyAccessedPartsHelper.LAST_FOURTEEN_DAYS, i18nNow.getI18nString("emxIEFDesignCenter.Common.LastFourteenDays", "emxIEFDesignCenterStringResource", languageStr));
		tempMap.put(MCADRecentlyAccessedPartsHelper.LAST_THIRTY_DAYS, i18nNow.getI18nString("emxIEFDesignCenter.Common.LastThirtyDays", "emxIEFDesignCenterStringResource", languageStr));
		tempMap.put(MCADRecentlyAccessedPartsHelper.LAST_SIXTY_DAYS, i18nNow.getI18nString("emxIEFDesignCenter.Common.LastSixtyDays", "emxIEFDesignCenterStringResource", languageStr));
		tempMap.put(MCADRecentlyAccessedPartsHelper.ALL, i18nNow.getI18nString("emxIEFDesignCenter.Common.All", "emxIEFDesignCenterStringResource", languageStr));

		Vector dateList = new Vector(); 

		dateList.addElement(MCADRecentlyAccessedPartsHelper.TODAY);
		dateList.addElement(MCADRecentlyAccessedPartsHelper.LAST_SEVEN_DAYS);
		dateList.addElement(MCADRecentlyAccessedPartsHelper.LAST_FOURTEEN_DAYS);
		dateList.addElement(MCADRecentlyAccessedPartsHelper.LAST_THIRTY_DAYS);
		dateList.addElement(MCADRecentlyAccessedPartsHelper.LAST_SIXTY_DAYS);
		dateList.addElement(MCADRecentlyAccessedPartsHelper.ALL);

		for(int i = 0 ; i < dateList.size(); i++)
		{	
			String time        = (String) dateList.elementAt(i);			 
			String displaytime = (String) tempMap.get(time);
				
			if(dateSelected != null && !"".equals(dateSelected) && dateSelected.equals(time))
				{
				fieldRangeValues.add(0, time);
				fieldDisplayRangeValues.add(0, displaytime);
				}
				else
				{
				fieldRangeValues.addElement(time);
				fieldDisplayRangeValues.addElement(displaytime);
				}
		}
		
		dateSelectionMap.put("field_choices", fieldRangeValues);
		dateSelectionMap.put("field_display_choices", fieldDisplayRangeValues);
		return dateSelectionMap;
	}
}
