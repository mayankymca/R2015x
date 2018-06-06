/*
 **  DECNameGeneratorBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to generate autonames .
 */
import java.util.*;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;

import matrix.db.Context;
import matrix.db.JPO;

public class DECNameGeneratorBase_mxJPO
{
	public DECNameGeneratorBase_mxJPO(Context context, String[] args) throws Exception
	{		

	}

	/**
	 * This method generates the autonames.
	 * 
	 *  @param context
	 *  	   ENOVIA DB Context
	 *  @param args  
	 *  	   A packed Hashtable which contains the following keys and values:
	 *  
	 *  <p>			 <em>key</em>   - Name and Revision of 'eService Object Generator' business object separated by "|". For Example. 'type_CADModel|A Size'.
	 *  
	 *  <p>			 <em>value</em> - Number of autonames to be generated.
	 *  										
	 *  @return		A Hashtable containing the following keys and values
	 *  
	 *  <p> 		<em>key</em>    - Name and Revision of 'eService Object Generator' business object separated by "|".For Example. 'type_CADModel|A Size'.
	 *  <p>			<em>value</em>  - Vector containing list of auto generated names.
	 *  
	 *  @throws		Exception
	 *  
	 * */
	public Hashtable getNames(Context context, String[] args) throws Exception
	{
		Hashtable returnTable	= new Hashtable();
		
		Hashtable paramJPO 		= (Hashtable) JPO.unpackArgs(args);
		
		Enumeration typeSeriesNames	= paramJPO.keys();
		while(typeSeriesNames.hasMoreElements())
		{
			String sTypeSeriesName		= (String) typeSeriesNames.nextElement();
			StringTokenizer tokenize	= new StringTokenizer(sTypeSeriesName,"|");
			String sType				= tokenize.nextToken();
			String seriesName			= tokenize.nextToken();
			int _count					= ((Integer) paramJPO.get(sTypeSeriesName)).intValue();
		
			Vector autoNames		= getAutoNamesForCADTypes(context, seriesName, sType, _count);
			returnTable.put(sTypeSeriesName, autoNames);
		}

		return returnTable;
	}
	
	protected Vector getAutoNamesForCADTypesOLD(Context context, String seriesName, String sTypeName ,  int _count) throws Exception
	{
		String Args[] = new String[9];
		Args[0] = "eServicecommonNumberGenerator.tcl";
		Args[1] = sTypeName;
		Args[2] = seriesName;
		Args[3] = "";
		Args[4] = "NULL";
		Args[5] = "";
		Args[6] = "_";
		Args[7] = "";
		Args[8] = "Yes";
	
		Vector	autoNameList	= new Vector(_count);
		
		for(int i = 0 ; i < _count ; i++)
		{
			String mqlResult = MCADMxUtil.executeMQL("execute program $1 $2 $3 $4 $5 $6 $7 $8 $9 ", context, Args);
		

			if(mqlResult.startsWith("true|"))
			{
				mqlResult = mqlResult.substring(5);
			}
			else
			{
				MCADServerException.createException(mqlResult.substring(6), null);
			}

			StringTokenizer tokenizer = new StringTokenizer(mqlResult, "|");
			String exitCode			  = (String) tokenizer.nextElement();
			
			if(exitCode.equals("1"))
			{
				String error = (String)tokenizer.nextElement();
				MCADServerException.createException(error, null);
			}
			
			String busName = (String)tokenizer.nextElement();
			autoNameList.add(busName);
		}
		
		return autoNameList;
	}
	
	protected Vector getAutoNamesForCADTypes(Context context, String seriesName, String sTypeName ,  int _count) throws Exception
	{
	
		Vector	autoNameList	= new Vector(_count);
		if(_count>0){
		
		String Args[] = new String[9];
		Args[0] = "DECCommonNumberGenerator.tcl";
		Args[1] = sTypeName;
		Args[2] = seriesName;
		Args[3] = "";
		Args[4] = "NULL";
		Args[5] = "";
		Args[6] = "_";
		Args[7] = "";
		Args[8] = "Yes|"+_count;
	
		
		
		//for(int i = 0 ; i < _count ; i++)
		//{
			String mqlResult = MCADMxUtil.executeMQL("execute program $1 $2 $3 $4 $5 $6 $7 $8 $9 ", context, Args);
		
			if(mqlResult.startsWith("true|"))
			{
				mqlResult = mqlResult.substring(5);
			}
			else
			{
				MCADServerException.createException(mqlResult.substring(6), null);
			}

			StringTokenizer tokenizer = new StringTokenizer(mqlResult, "|");
			String exitCode			  = (String) tokenizer.nextElement();
			
			if(exitCode.equals("1"))
			{
				String error = (String)tokenizer.nextElement();
				MCADServerException.createException(error, null);
			} else {
				while(tokenizer.hasMoreElements()){
					String busName = (String)tokenizer.nextElement();
					autoNameList.add(busName);
				}
			}


		//}
		}
		
		return autoNameList;
	}
	
}

