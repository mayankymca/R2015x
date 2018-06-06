/**
 * IEFCheckObject.java
 *
 *  Copyright Dassault Systemes, 1992-2007.
 *  All Rights Reserved.
 *  This program contains proprietary and trade secret information of Dassault Systemes and its 
 *  subsidiaries, Copyright notice is precautionary only
 *  and does not evidence any actual or intended publication of such program
 *
 * This JPO performs Pre-Checkin Evaluation of the MCAD model.
 */

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;

public class IEFCheckObject_mxJPO
{
	public IEFCheckObject_mxJPO(Context context, String[] args) throws Exception
	{
	}

	public String checkObject(Context context, String[] args) throws Exception
	{
		String returnStr = "";

		 String name		 = args[0];
		 String type		 = args[1];
		 String debug		 = args[2];
		 String language	 = args[3];
		 String objectStatus = args[4];
		 String rev			 = args[5];

		MCADServerResourceBundle serverResourceBundle			= new MCADServerResourceBundle(language);

		try
		{
			Vector inputArgs			= new Vector();
			Hashtable inputIdsInfoTable = new Hashtable(1);
			Hashtable inputArgsTable	= new Hashtable(1);
			
			inputArgs.addElement(name);
			inputArgs.addElement(type);
			inputArgs.addElement(rev);
			inputArgs.addElement(objectStatus);

			inputIdsInfoTable.put("dummyID", inputArgs);

			inputArgsTable.put("inputTable", inputIdsInfoTable);
			inputArgsTable.put("isDebugOn", debug);
			inputArgsTable.put("language", language);

			String [] newArgs		= JPO.packArgs(inputArgsTable);
			Hashtable returnTable	= evaluateObject(context, newArgs);
			returnStr				= (String) returnTable.get("dummyID");

			//return "false|message for user" if evaluation fails
			//return "true|message for user" if evaluation succeeds
		}
		catch(Exception ex)
		{
			MCADServerException.createManagedException("IEF0033200100", serverResourceBundle.getString("mcadIntegration.Server.Message.IEF0033200100"),ex);
		}

		return  returnStr;
	}

	public Hashtable evaluateObject(Context context, String[] args) throws Exception
	{

		Hashtable returnTable	= new Hashtable();

		

		Hashtable argumentsTable	    = (Hashtable) JPO.unpackArgs(args);
		Hashtable inputTypeNameDetails	= (Hashtable) argumentsTable.get("inputTable"); // key -> cadID / value -> type,name,objStatus (the same key will be used in return table)
		String debug					= (String) argumentsTable.get("isDebugOn");
		String language					= (String) argumentsTable.get("language");
		
		MCADServerResourceBundle serverResourceBundle			= new MCADServerResourceBundle(language);

		try
		{
		
			

			Enumeration typeName			= inputTypeNameDetails.keys();
			while(typeName.hasMoreElements())
			{
				String cadID 			= (String)typeName.nextElement();
				Vector typeNameDetails 	= (Vector)inputTypeNameDetails.get(cadID);
				String name 			= (String)typeNameDetails.elementAt(0);
				String type 			= (String)typeNameDetails.elementAt(1);
				String rev	 			= (String)typeNameDetails.elementAt(2);
				String objStatus		= (String)typeNameDetails.elementAt(3);
			
				String result 			= "true| "+ type + ",  " + name + ",  " + rev + ",  " + debug;
				returnTable.put(cadID, result);
			}
		}
		catch(Exception ex)
		{
			MCADServerException.createManagedException("IEF0033200100", serverResourceBundle.getString("mcadIntegration.Server.Message.IEF0033200100"),ex);
		}

		return returnTable;
	}
}
