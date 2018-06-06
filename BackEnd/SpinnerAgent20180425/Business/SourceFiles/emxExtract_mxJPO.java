import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ds.dso.license.SpinnerLicenseCheck;
import com.ds.dso.exportfiles.emxExtractSchema;

import matrix.db.Context;
import com.matrixone.apps.domain.util.MqlUtil;
public class emxExtract_mxJPO
{
	public int mxMain(Context context, String[] args) throws Exception
	{
		emxExtractSchema emxExtractSchemaObj = new emxExtractSchema();
		
		String mCommandEnv = "get env 1";
		String mCommandEnv2 = "get env 2";
		String mCommandEnv3 = "get env 3";
		String mCommandEnv4 = "get env 4";
		String mCommandEnv5 = "get env 5";
		
		String bJPOExtraction = "get env JPOEXTRACTION";

		SpinnerLicenseCheck spinnerLicenseCheckObj = new SpinnerLicenseCheck();
		String sSchemaType =  MqlUtil.mqlCommand(context, mCommandEnv);
		String sSchemaName = MqlUtil.mqlCommand(context, mCommandEnv2);
		
		String sParameter3 = MqlUtil.mqlCommand(context, mCommandEnv3);
		String sParameter4 = MqlUtil.mqlCommand(context, mCommandEnv4);
		String sParameter5 = MqlUtil.mqlCommand(context, mCommandEnv5);
		
		
		String bJPO = MqlUtil.mqlCommand(context, bJPOExtraction);
		boolean isValid = spinnerLicenseCheckObj.SpinnerRunTimeCheck(context);
		if (isValid)
		{
		  if(sSchemaName.equalsIgnoreCase("template")){
			  emxExtractSchemaObj.setExtractTemplate(true);  
		  }
		  
		  if(sParameter3.equalsIgnoreCase("trigger")){
			  emxExtractSchemaObj.setExtractTriggerFilter(true);  
		  }
		  
		  if(sParameter4.equalsIgnoreCase("spinner")){
			  emxExtractSchemaObj.setExportSpinnerFilter(true);  
		  }else{
			  if(sParameter4 !=null && sParameter4.length()>0)
			  {
				  Date modFromDate = parseDate(sParameter4) ;
				  if(modFromDate!=null)
				  {
					  emxExtractSchemaObj.setSpinnerModFilterFromDate(modFromDate);
					  emxExtractSchemaObj.setExportDateFilter(true);
				  } else{
					  return 0;
				  }
			  }
		  }
		    
		  if(sParameter5 !=null && sParameter5.length()>0)
		  {
			  Date modToDate = parseDate(sParameter5) ;
			  if(modToDate!=null)
			  {
				   emxExtractSchemaObj.setSpinnerModFilterToDate(modToDate);
			  } else{
				  return 0;
			  }
		  }
			emxExtractSchemaObj.setbJPO(bJPO);
			emxExtractSchemaObj.extractFiles(context,sSchemaType,sSchemaName,bJPO);
		} 
		return 0;
	}
	
	private Date parseDate(String sDate)
	{
		 try {
			 SimpleDateFormat sDateFormat = new SimpleDateFormat("MM/dd/yyyy");
			 Date date = sDateFormat.parse(sDate);
			 return date;
			 
		 }catch(ParseException exception)
		 {
			 System.out.println("Filter parameter ModifiedFromDate or ModifiedToDate is not correct DateFormat(MM/dd/yyyy)   "+sDate);
			 return null;
		 }
	}
}
