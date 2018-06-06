/*
 * emxPreInstallChecks
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
import com.ds.dso.license.SpinnerLicenseCheck;
import com.ds.dso.license.ValidateLicenseKey;
import matrix.db.*;

 public class emxPreInstallChecks_mxJPO 
 {
	public int mxMain(Context context, String[] args) throws Exception
	{    
		return 0;
	}
		
    public boolean checkLicenseKey(Context context, String[] args) throws Exception
	{
 		MatrixWriter writer = new MatrixWriter(context);
		 
		try
		{	
			SpinnerLicenseCheck checkParams = new SpinnerLicenseCheck();			
			boolean isValid = checkParams.spinnerInstallationCheck(context);
			ValidateLicenseKey.setVerboOnOff(context, "verbose off");		
			if(isValid) { 
				writer.write("True");
				return true;
			} else {
				writer.write("False");
				return false;
			}
		}catch (Exception e) {
			System.out.println("Exception in checkLicenseKey:" + e.toString());
			return false;
		}
	}
  }