/*
 * emxRebuildLicense
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

import matrix.db.*;

public class emxRebuildLicense_mxJPO 
{
	public int mxMain(Context context, String[] args) throws Exception
	{		    	
		MatrixWriter writer = new MatrixWriter(context);

		try
		{	
			SpinnerLicenseCheck sLicenseCheck = new SpinnerLicenseCheck ();
			boolean isValid = sLicenseCheck.spinnerLicenseRehost(context);
			if(isValid) { 
				writer.write("License key is valid, rebuild the key successfully.\n");
			} else {
				writer.write("License key is not valid, please refer logs.\n");
			}
		}catch (Exception e) {
			System.out.println("Exception while rehosting the license.");			
			e.printStackTrace();
		}
		return 0;							
	}
}