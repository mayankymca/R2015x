/*
 * ${CLASSNAME}
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Map;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.configuration.ConfigurationConstants;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;


/**
 * The <code>FTRGBOMEffectivityMigrationFindObjectsBase</code> class contains implementation code for list out all the Models in the system.
 * This is the very first step of Migration.
 */
public class InclusionRuleMigrationForGBOMAndLFFindObjectsBase_mxJPO extends emxCommonFindObjects_mxJPO
{	

	/**
	 * Constructor.
	 *
	 * @param context the eMatrix <code>Context</code> object
	 * @param args holds no arguments
	 * @throws Exception if the operation fails
	 */
	public InclusionRuleMigrationForGBOMAndLFFindObjectsBase_mxJPO (Context context, String[] args)
	throws Exception
	{
		super(context, args);
	}

	/**
	 * This method is executed if a specific method is not specified.
	 * This is the actual method to get all the Model ids in system and write them in file name objectids_xx.txt
	 * 
	 * @param context the eMatrix <code>Context</code> object
	 * @param args holds no arguments
	 * @returns nothing
	 * @throws Exception if the operation fails
	 */
	public int mxMain(Context context, String[] args)
	throws Exception
	{
		setAdminMigrationStatus(context,"FindObjectInProgress");

		StringBuffer typeList = new StringBuffer();
		typeList.append(ConfigurationConstants.TYPE_LOGICAL_FEATURE);
		typeList.append(",");
		typeList.append(ConfigurationConstants.TYPE_PRODUCTS);
		String strTypePattern = typeList.toString();

		StringBuffer strRelPattern = new StringBuffer(ConfigurationConstants.RELATIONSHIP_GBOM);
		strRelPattern.append(",");
		strRelPattern.append(ConfigurationConstants.RELATIONSHIP_INACTIVE_GBOM);
		strRelPattern.append(",");
		strRelPattern.append(ConfigurationConstants.RELATIONSHIP_CUSTOM_GBOM);
		strRelPattern.append(",");
		strRelPattern.append(ConfigurationConstants.RELATIONSHIP_INACTIVE_CUSTOM_GBOM);
		strRelPattern.append(",");
		strRelPattern.append(ConfigurationConstants.RELATIONSHIP_LOGICAL_FEATURES);

		String sbObjWhere = "from["+ strRelPattern +"].tomid["+ ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION +"].from.type == \""+ ConfigurationConstants.TYPE_INCLUSION_RULE +"\"";

		try
		{
			if (args.length < 2 )
			{
				throw new IllegalArgumentException();
			}

			// documentDirectory does not ends with "/" add it
			documentDirectory = args[1];
			String fileSeparator = java.io.File.separator;
			if(documentDirectory != null && !documentDirectory.endsWith(fileSeparator))
			{
				documentDirectory = documentDirectory + fileSeparator;
			}


			String[] newArgs = new String[3];
			newArgs[0] = args[0];
			String str = newArgs[0].toString();
			int chunkSize = Integer.parseInt(str);

			StringList objSelects = new StringList();
			objSelects.addElement(ConfigurationConstants.SELECT_ID);
			objSelects.addElement(ConfigurationConstants.SELECT_TYPE);
			objSelects.addElement(ConfigurationConstants.SELECT_NAME);

			MapList topLevelObjects = DomainObject.findObjects(context, // context
					strTypePattern, // typePattern
					"*", // vaultPattern
					sbObjWhere, // whereExpression
					objSelects); // objectSelects

			//reset/set static variables back to original values every time this JPO is run
			emxCommonMigration_mxJPO._counter  = 0;
			emxCommonMigration_mxJPO._sequence  = 1;
			emxCommonMigration_mxJPO._oidsFile = null;
			emxCommonMigration_mxJPO._fileWriter = null;
			emxCommonMigration_mxJPO._objectidList = null;

			//set statics
			//create BW and file first time
			if (emxCommonMigration_mxJPO._fileWriter == null)
			{	
				try
				{
					emxCommonMigration_mxJPO.documentDirectory = documentDirectory;
					emxCommonMigration_mxJPO._oidsFile = new java.io.File(documentDirectory + "objectids_1.txt");
					emxCommonMigration_mxJPO._fileWriter = new BufferedWriter(new FileWriter(emxCommonMigration_mxJPO._oidsFile));
					emxCommonMigration_mxJPO._chunk = chunkSize;
					emxCommonMigration_mxJPO._objectidList = new StringList(chunkSize);
				}
				catch(FileNotFoundException eee)
				{
					throw eee;
				}
			}

			for (int i = 0; i < topLevelObjects.size(); i++) {
				Map tmpMap = (Map) topLevelObjects.get(i);
				String topLevelObjectId = (String) tmpMap.get(ConfigurationConstants.SELECT_ID);
				String topLevelObjectType = (String) tmpMap.get(ConfigurationConstants.SELECT_TYPE);

				String[] args1 = new String [2];
				args1[0] = topLevelObjectId;  //Path of Output File
				args1[1] = topLevelObjectType;  //Path of Output File

				JPO.invoke(context, "emxCommonMigrationBase", null,"writeOID", args1, null);
			}

			return 0;
		}
		catch (IllegalArgumentException iExp)
		{
			writer.write("=================================================================\n");
			writer.write("Wrong number of arguments Or Invalid number of Oids per file\n");
			writer.write("Step 2.1 of Migration ${CLASS:FTRGBOMEffectivityMigrationFindObjectsBase} :   " + iExp.toString() + "   : FAILED!!!! \n");
			writer.write("=================================================================\n");
			writer.close();
			return 0;
		}    
		finally{
			setAdminMigrationStatus(context,"FindObjectCompleted");
			// call cleanup to write the left over oids to a file
			emxCommonMigration_mxJPO.cleanup();
		}
	}

	/**
	 * Sets the migration status as a property setting.
	 * Status could be :-
	 * FindObjectInProgress
	 * FindObjectCompleted
	 * MigrationInProgress
	 * MigrationCompleted
	 * MigrationKO
	 * 
	 * @param context the eMatrix <code>Context</code> object
	 * @param strStatus String containing the status setting
	 * @throws Exception
	 */
	public void setAdminMigrationStatus(Context context,String strStatus) throws Exception
	{
		String cmd = "modify program $1 property $2 value $3 ";
		MqlUtil.mqlCommand(context, cmd, true, "eServiceSystemInformation.tcl", "InclusionRuleMigrationForGBOMAndLF", strStatus);
	}

	/**
	 * Gets the migration status as an integer value.  Used to enforce an order of migration.
	 * @param context the eMatrix <code>Context</code> object
	 * @return integer representing the status
	 * @throws Exception
	 */
	public int getAdminMigrationStatus(Context context) throws Exception
	{		
		String cmd = "print program $1 select $2 dump";
		String result =	MqlUtil.mqlCommand(context, cmd, true, "eServiceSystemInformation.tcl", "property[InclusionRuleMigrationForGBOMAndLF].value");

		if(result.equalsIgnoreCase("FindObjectInProgress"))
		{
			return 1;
		}
		else if(result.equalsIgnoreCase("FindObjectCompleted"))
		{
			return 2;
		}
		else if(result.equalsIgnoreCase("MigrationInProgress"))
		{
			return 3;
		}
		else if(result.equalsIgnoreCase("MigrationCompleted"))
		{
			return 4;
		}
		else
		{
			return -1;
		}
	}

}
