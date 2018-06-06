/*
 * emxSpinnerPnOPlugin
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

import com.matrixone.apps.domain.util.MqlUtil;
import com.ds.dso.pno.PnOPlugin;

public class emxSpinnerPnOPlugin_mxJPO 
{ 
		public int mxMain(Context context, String[] args) throws Exception
		{		    	
			MatrixWriter writer = new MatrixWriter(context);
			
			try
			{	
				String installPath = getMatrixInstall(context,writer);
				String user = MqlUtil.mqlCommand(context, "get env 1");
				String password = MqlUtil.mqlCommand(context, "get env 2");
				String sPnoType = MqlUtil.mqlCommand(context, "get env 3");
				String sPnoValue = MqlUtil.mqlCommand(context, "get env 4");
				String sPnoLocation = MqlUtil.mqlCommand(context, "get env 5");
				String sContext = MqlUtil.mqlCommand(context, "get env SecurityContext");	
				String serverURL = MqlUtil.mqlCommand(context, "get env URL");
				String sPnOFilePath = MqlUtil.mqlCommand(context, "get env PnOPath");		
                String bLicenseSetting = MqlUtil.mqlCommand(context, "get env LicenseSetting");
				String sTransactionMode = MqlUtil.mqlCommand(context, "get env TransactionMode");	
				String sTransactionSize = MqlUtil.mqlCommand(context, "get env TransactionSize");				
				String command = MqlUtil.mqlCommand(context, "get env command");
				PnOPlugin plugin = new PnOPlugin();				
				switch(command){
				case "ExportPnO" :  if(user.length() == 0 || (sPnoType.length() > 0 && sPnoValue.length() == 0 ) ) {					
					DisplayErrorForExport();				
				} else {		
					plugin.setbLicenseSetting(bLicenseSetting);
					plugin.setMatrixInstallPath(installPath);
					plugin.setCommand(command);
					plugin.setUser(user);
					plugin.setPassword(password);
					plugin.setContext(sContext);
					plugin.setServerURL(serverURL);
					plugin.setsPnOFielsPath(sPnOFilePath);
					plugin.setPnoType(sPnoType);
					plugin.setPnoValue(sPnoValue);
					plugin.setsPnOLocation(sPnoLocation);
					plugin.executePlugIn(context);
					
				}
				break;
				
				case "ImportPnO" : String Path = sPnOFilePath+java.io.File.separator+"PnO";
					java.io.File f = new java.io.File(Path);					
					if(user.length() == 0 || ( ! f.exists() && ! f.isDirectory() )) {
					DisplayErrorForImport();	
				} else {					
					plugin.setbLicenseSetting(bLicenseSetting);
					plugin.setMatrixInstallPath(installPath);
					plugin.setCommand(command);
					plugin.setUser(user);
					plugin.setPassword(password);
					plugin.setContext(sContext);
					plugin.setServerURL(serverURL);
					plugin.setsPnOFielsPath(sPnOFilePath);
					plugin.setPnoType(sPnoType);
					plugin.setPnoValue(sPnoValue);
					plugin.setsPnOLocation(sPnoLocation);
					plugin.setsTransactionMode(sTransactionMode);
					plugin.setsTransactionSize(sTransactionSize);
					plugin.executePlugIn(context);
					
				}
				break;
			}
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			return 0;		
			
			
		}
			
		private void DisplayErrorForImport() {
			StringBuilder importHelp =new StringBuilder("");
			importHelp.append("Spinner P&O and Security import program \n \n");
			importHelp.append("usage: exec prog emxSpinnerPnOImport.tcl [username]  \n");
			importHelp.append("					 [password] \n");

			importHelp.append("Help options \n");
			importHelp.append("username \n");
			importHelp.append("	Connect to the VPLM server using the specified P&O user id; this \n");
			importHelp.append("	parameter doesn't need to be specified when invoking the program from JPO; \n");
			importHelp.append("	user must be either granted system privileges, or assigned command \n");
			importHelp.append("	vplm::Administration. \n");
			importHelp.append("password \n");
			importHelp.append("	Connect to the VPLM server using the specified password; \n");
			importHelp.append("	this parameter is useless when invoking the program from JPO. \n\n");     	
			importHelp.append("exec prog emxSpinnerPnOImport.tcl username password; \n");
			System.out.println(importHelp.toString());
		}
		
		private void DisplayErrorForExport(){
			StringBuilder exportHelp =new StringBuilder("");
			exportHelp.append("Spinner P&O and Security export program \n \n");
			exportHelp.append("usage: exec prog emxSpinnerPnOExport.tcl [username]  \n");
			exportHelp.append("					 [password] \n");
			exportHelp.append("					 {objecttype} \n");
			exportHelp.append("					 {objectid} \n");
			exportHelp.append("					 {locationtype/locationname}; \n");
			exportHelp.append("Help options \n");
			exportHelp.append("username \n");
			exportHelp.append("	Connect to the VPLM server using the specified P&O user id; this \n");
			exportHelp.append("	parameter doesn't need to be specified when invoking the program from JPO; \n");
			exportHelp.append("	user must be either granted system privileges, or assigned command \n");
			exportHelp.append("	vplm::Administration. \n");
			exportHelp.append("password \n");
			exportHelp.append("	Connect to the VPLM server using the specified password; \n");
			exportHelp.append("	this parameter is useless when invoking the program from JPO. \n");
			exportHelp.append("objecttype \n");
			exportHelp.append("	Set object to export, identified by its type and identifier attribute; \n");
			exportHelp.append("	it is possible to specify several objects at once. \n");
			exportHelp.append("	Available object types are as follows: \n");
			exportHelp.append("		businessunit \n");
			exportHelp.append("		company \n");
			exportHelp.append("		context \n");
			exportHelp.append("		department \n");
			exportHelp.append("		person \n");
			exportHelp.append("		project \n");
			exportHelp.append("		role \n");
			exportHelp.append("objectid \n");
			exportHelp.append("	Set of Objects or single object to be exported \n");
			exportHelp.append("locationtype/locationname \n");
			exportHelp.append("	Location of the user (for use with dedicated Central applications only) \n\n");
		
			exportHelp.append("exec prog emxSpinnerPnOExport.tcl username password objecttype objectid locationtype/locationname \n");
			System.out.println(exportHelp.toString());
		}
		
	  private String getMatrixInstall(Context context,MatrixWriter writer)
	  {
		String sMatrixInstall = "";

		// Get path of MATRIXINSTALL
		try
		{
		  sMatrixInstall = Environment.getValue(context, "MATRIXINSTALL");
		}
		catch (Exception e)
		{
		  e.printStackTrace();
		//  writer.write("Unable to read MATRIXINSTALL path");
		}

		return sMatrixInstall;
	  }
	  
}