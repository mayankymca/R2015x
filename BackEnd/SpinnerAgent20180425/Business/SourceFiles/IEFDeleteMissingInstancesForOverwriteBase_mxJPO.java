/*
**  IEFDeleteMissingInstancesForOverwriteBase.java
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**	
**  This is the base JPO to provide hook for deletion of missing instances in case of overwrite
**	DO NOT CUSTOMIZE this JPO, use IEFDeleteMissingInstancesForOverwrite for customization.
**	@since Sourcing R208
*/


import java.util.Hashtable;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;

public class IEFDeleteMissingInstancesForOverwriteBase_mxJPO
{
	private boolean isDebug 		= false;
	private String language	 		= "en";

	/**
	   * Constructor.
	   *
	   * @param context the eMatrix <code>Context</code> object
	   * @param args
	   * @throws Exception if the operation fails
	   * @since Sourcing R208
	   */

	public IEFDeleteMissingInstancesForOverwriteBase_mxJPO(Context context, String[] args) throws Exception
	{

	}	

	public String deleteInstance(Context context, String[] args) throws Exception
	{
		Vector busObjectInfoList				= new Vector();
		String isDeleteMissingInstancesForOverwrite = "true";
		Hashtable argumentsTable					= (Hashtable) JPO.unpackArgs(args);
		/**
			busObjectInfoList has 
			0 = fileAbsPath
			1 = busId;
			2 = format;
			3 = storeName;
			4 = choppedFileName;
		*/
		busObjectInfoList = (Vector) argumentsTable.get("busObjectInfoList");
		
		this.isDebug 	= ((String) argumentsTable.get("isDebugOn")).equalsIgnoreCase("true");
		this.language 	= (String) argumentsTable.get("language");
		
		isDeleteMissingInstancesForOverwrite = isDeleteInstances(context, args);
						
		return isDeleteMissingInstancesForOverwrite;
	}

	/**
		This method returns a flag which will ultimately decide whether to delete missing instances
		in object "overwrite" case.
		This method can be overridden in child JPO to add additional logic at customer end to contol
		above behavior.
	*/

	protected String isDeleteInstances(Context context, String[] args) throws Exception
	{
			
		//By Default this JPO returns "true", so the missing instances will be deleted in case of overwrite.
		//Return "false", if user does not want to delete missing instances for overwrite.
		
		return "true";
	}	
}
