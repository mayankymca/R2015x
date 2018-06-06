/*
**   ${CLASSNAME}
**
**  Copyright (c) 1992-2003 MatrixOne, Inc.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of MatrixOne,
**  Inc.  Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Returns Preferences page layout
*/
/**
 *  ${CLASSNAME} jpo
 * This jpo returns a string containing xml for generating Preferences page
 */

import matrix.db.Context;
import matrix.db.MatrixWriter;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.server.cache.IEFCache;
import java.io.BufferedWriter;


public class emxMSPIPreferencesPageLayout_mxJPO
{
	MCADMxUtil util						= null;

	private final String COMBO_BOX		= "ComboBox";
	private final String EDIT_BOX		= "EditBox";
	private final String RADIO_BUTTON	= "Radio";
	private final String POPUP_LIST		= "PopupList";
	private final String CHECK_BOX		= "CheckBox";
    private IEFCache  _GlobalCache       = new IEFGlobalCache();
	private BufferedWriter writer		= null;


	public  emxMSPIPreferencesPageLayout_mxJPO()
    {
    }
    
	public  emxMSPIPreferencesPageLayout_mxJPO (Context context, String[] args) throws Exception
    {
 	  writer = new BufferedWriter(new MatrixWriter(context));

      if (!context.isConnected())
      throw new Exception("not supported no desktop client");

    }

    public int mxMain(Context context, String []args)  throws Exception
    {	
        return 0;
    }
	
	private void init(Context context, String language)
	{
		MCADServerResourceBundle serverResourceBundle = new MCADServerResourceBundle(language);
		util = new  MCADMxUtil(context, null,_GlobalCache); 
	}

	/**
	 * Entry Point
	 * This method returns a string containing xml which is used for generating Preferences page
	 *
	 */
	public String getPreferencesPageLayout(Context context, String[] args)throws Exception
	{   
		StringBuffer preferencesPageLayout = null;

		try{
		String language = args[0];
		init(context, language);

  	    preferencesPageLayout = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

		preferencesPageLayout.append("<preferences>");
		
		preferencesPageLayout.append("</preferences>");
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return preferencesPageLayout.toString();
	}
}
