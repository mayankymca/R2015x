/*
 *  emxReadExtractSchema
 *
 * Copyright (c) 1992-2012 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import matrix.db.*;
import com.ds.dso.license.emxReadSchemaFile;
import com.ds.dso.license.SpinnerLicenseCheck;
import java.util.HashMap;

public class emxReadExtractSchema_mxJPO extends emxDomainObject_mxJPO
{
    public emxReadExtractSchema_mxJPO (Context context, String[] args)
        throws Exception
    {
      super(context, args);
    }

    public int mxMain(Context context, String[] args)
        throws Exception
    {
		MatrixWriter writer = new MatrixWriter(context);
                
		try {
			emxReadSchemaFile  readSchemaObj = new emxReadSchemaFile();
			
			boolean isValid = readSchemaObj.readExtractSchema(context);
    		if (isValid == false)
    		{
    		  writer.write("License is invalid. Please refer logs.");
    		}
		} catch (Exception e) {
			e.printStackTrace();
		}	 
        return 0;
    }
 
}