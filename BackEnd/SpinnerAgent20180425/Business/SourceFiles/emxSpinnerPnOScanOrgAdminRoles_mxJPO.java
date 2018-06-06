/*
 * emxSpinnerPnOScanOrgAdminRoles
 *
 * Copyright (c) 1992-2010 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 *
 */

import matrix.db.*;
import matrix.util.*;
import java.util.*;
import java.io.*;
import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;
import com.matrixone.apps.framework.ui.UIUtil;
import java.text.SimpleDateFormat;

public class emxSpinnerPnOScanOrgAdminRoles_mxJPO extends emxDomainObject_mxJPO
{
	public static  String RELATIONSHIP_COMPANY_DEPARTMENT = PropertyUtil.getSchemaProperty("relationship_CompanyDepartment");
	private static String RELATIONSHIP_SUBSIDIARY = PropertyUtil.getSchemaProperty("relationship_Subsidiary");
	
	static FileWriter statusLog = null;
	static PrintWriter pwStatusLog = null;
		
	public emxSpinnerPnOScanOrgAdminRoles_mxJPO (Context context, String[] args) throws Exception
	{
		super(context, args);
	}
	
	public int mxMain(Context context, String[] args) throws Exception
	{
		String DATE_FORMAT = "MM/dd/yyyy hh:mm:ss a";
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		Date sToday = sdf.parse(sdf.format(new Date()));
				
		long lStartTime = System.currentTimeMillis();		
	
		createLogs(context);
		
		try {
			
			statusLog.write("Started at = "+ sToday +" \n");
			scanOrganizationAdminRole(context);
			
		} catch (Exception ex) {
			
			ex.printStackTrace(pwStatusLog);
			
		} finally {	
		
			Date eToday = sdf.parse(sdf.format(new Date()));
			statusLog.write("Finished at = "+ eToday +" \n");
			
			long lEndTime = System.currentTimeMillis();		
			long diff = (lEndTime - lStartTime);
			
			diff = diff/1000;
			statusLog.write("Time Taken = "+diff +" \n");			
			
			statusLog.close();
			pwStatusLog.close();
		}
		return 0;
	}
	
	private static void scanOrganizationAdminRole(Context context) throws Exception
	{
		StringList selectList = new StringList();
		selectList.addElement(DomainConstants.SELECT_ID);
		selectList.addElement(DomainConstants.SELECT_NAME);
		selectList.addElement(SELECT_TYPE);
		
		MapList mapList = DomainObject.findObjects(context, DomainObject.TYPE_ORGANIZATION, DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD, "", true, selectList);
		
		Iterator itr = mapList.iterator();
		Map map = null;
		
		while(itr.hasNext())
		{
			map = (Map)itr.next();
			String sOrgId = (String)map.get(DomainConstants.SELECT_ID);
			String sOrgName = (String)map.get(DomainConstants.SELECT_NAME);
			String sOrgType = (String)map.get(DomainConstants.SELECT_TYPE);
			
			String sAdminRole = MqlUtil.mqlCommand(context, "list role $1", sOrgName);
			
			if (!sAdminRole.equals(sOrgName)) {
				
				statusLog.write("## Admin Role does not exist for " + sOrgType + " "+ sOrgName + "\n");
				
				if (sOrgType.equals(DomainObject.TYPE_BUSINESS_UNIT)){
					
					DomainObject objOrg = new DomainObject(sOrgId);
					String sParentName = objOrg.getInfo(context, "to[" +DomainObject.RELATIONSHIP_DIVISION+ "].from.name");
					
					addAdminRole (context,sOrgName, sParentName);
					
				} else if (sOrgType.equals(DomainObject.TYPE_DEPARTMENT)){
					
					DomainObject objOrg = new DomainObject(sOrgId);
					
					String sParentName = objOrg.getInfo(context, "to["+ RELATIONSHIP_COMPANY_DEPARTMENT +"].from.name");
					
					addAdminRole (context, sOrgName, sParentName);
					
				} else if (sOrgType.equals(DomainObject.TYPE_COMPANY)){
					
					DomainObject objOrg = new DomainObject(sOrgId);
					String sParentName = objOrg.getInfo(context, "to["+ RELATIONSHIP_SUBSIDIARY +"].from.name");
					
					addAdminRole (context, sOrgName, sParentName);
					
				} else {
					
					addAdminRole (context,sOrgName, "");
				}
			} else {
				String isOrgRole = MqlUtil.mqlCommand(context, "print role $1 select $2 dump", sOrgName, "isanorg");
				
				if (!isOrgRole.equalsIgnoreCase("TRUE")) {
					
					statusLog.write("## Error: Admin Role "+ sOrgName + " exist but not as an Organization role \n");
				} else {
					
					statusLog.write("## Admin Role exist for "+ sOrgType + " "+ sOrgName + " \n");
				}
			}
		}
	}
	
	private static void addAdminRole (Context context, String sOrgName, String sParentName) throws Exception
	{
		if (UIUtil.isNotNullAndNotEmpty(sParentName)) {
						
			MqlUtil.mqlCommand(context, "add role $1 asanorg parent $2", sOrgName, sParentName);
			statusLog.write("## Added Admin Role " + sOrgName + " with parent "+ sParentName + "\n");

		} else {
			
			MqlUtil.mqlCommand(context, "add role $1 asanorg", sOrgName);
			statusLog.write("## Added Admin Role " + sOrgName + " without parent \n");
		}
		
	}
	
	public void createLogs(Context context) throws Exception
	{
		String sLOGPATH = Environment.getValue(context, "MX_TRACE_FILE_PATH");
        statusLog = new FileWriter(sLOGPATH + java.io.File.separator + "scanOrganizationAdminRoles.log", false);
		pwStatusLog = new PrintWriter(statusLog, true);
	}
}