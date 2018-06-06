/*
 * emxSpinnerPnOLegacyLicense
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

import matrix.db.Context;
import matrix.db.Environment;
import matrix.db.*;

import com.matrixone.apps.domain.util.MqlUtil;

import com.ds.dso.pno.UpgradeLicenseMapping;

public class emxSpinnerPnOLegacyLicense_mxJPO 
{
		public int mxMain(Context context, String[] args) throws Exception
		{		    	
			MatrixWriter writer = new MatrixWriter(context);

			try
			{	
				UpgradeLicenseMapping plugin = new UpgradeLicenseMapping();
				plugin.upgradeLicense(context);
										
			}catch (Exception e) {
				e.printStackTrace();
			}
			return 0;							
		}
	  
}