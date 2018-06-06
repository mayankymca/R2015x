/*
**  MCADInstances
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to display details of instances for attribute based configurations
*/
import java.util.ArrayList;

import matrix.db.Context;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.customTable.ColumnDefinition;

/**
 * This JPO is used to generate data for displaying details
 * of instances (based on config) in a custom table. It provides the
 * table column definitions and the actual cell data.
 */
public class MCADInstances_mxJPO
{
	MCADServerResourceBundle serverResourceBundle = null;
	IEFGlobalCache cache	= null;
	MCADGlobalConfigObject _gco = null;
	MCADMxUtil _util = null;
	MCADServerGeneralUtil _generalUtil = null;

	/**
	 * The no-argument constructor.
	 */
	public MCADInstances_mxJPO()
	{
	}

	/**
	 * Constructor wich accepts the Matrix context and an array of String
	 * arguments.
	 */
	public MCADInstances_mxJPO(Context context, String[] args) throws Exception
	{
	}

	/**
	 * This method generates column definitions for the table.
	 *
	 * @param context The Matrix Context.
	 * @param args[] arguments - not used.
	 * @throws Exception.
	 */
	public Object getColumnDefinitions(Context context, String [] args) throws Exception
	{
		//HashMap paramMap	=  (HashMap)JPO.unpackArgs(args);
		//String langStr		=  (String)paramMap.get("languageStr");

		//intializeClassMembers(context, langStr);

		ArrayList  columnDefs = new ArrayList();

		ColumnDefinition parentInstanceNamesColumn		= new ColumnDefinition();
		ColumnDefinition instanceNamesColumn			= new ColumnDefinition();
		ColumnDefinition actionsColumn					= new ColumnDefinition();
		ColumnDefinition instanceWhereUsedColumn		= new ColumnDefinition();
		ColumnDefinition relatedDrawingColumn			= new ColumnDefinition();
		ColumnDefinition instanceDetailsColumn			= new ColumnDefinition();
		ColumnDefinition instanceNavigateColumn			= new ColumnDefinition();
		ColumnDefinition dependentDocColumn				= new ColumnDefinition();
		ColumnDefinition instanceRepresentationsColumn	= new ColumnDefinition();

		//Initializing column for Instance Name
		//String instanceColumnName = serverResourceBundle.getString("mcadIntegration.Server.ColumnName.InstanceName");
		instanceNamesColumn.setColumnTitle("mcadIntegration.Server.ColumnName.InstanceName");
		instanceNamesColumn.setColumnKey("INSTANCENAME");
		instanceNamesColumn.setColumnDataType("string");
		instanceNamesColumn.setColumnType("label");
		instanceNamesColumn.setColumnIsSortable(false);

		//Initializing column for Parent Instance Name
		parentInstanceNamesColumn.setColumnTitle("mcadIntegration.Server.ColumnName.ParentInstanceName");
		parentInstanceNamesColumn.setColumnKey("PARENTINSTANCENAME");
		parentInstanceNamesColumn.setColumnDataType("string");
		parentInstanceNamesColumn.setColumnType("label");
		parentInstanceNamesColumn.setColumnIsSortable(false);

		//Initializing column for Action
		//String actionColumnName = serverResourceBundle.getString("mcadIntegration.Server.ColumnName.Action");
		actionsColumn.setColumnTitle("mcadIntegration.Server.ColumnName.Checkout");
		actionsColumn.setColumnKey("CHECKOUT");
		actionsColumn.setColumnDataType("string");
		actionsColumn.setColumnTarget("hiddenFrame");
		actionsColumn.setColumnType("icon");
		actionsColumn.setColumnIsSortable(false);

		//Initializing column for Where Used action
		instanceWhereUsedColumn.setColumnTitle("mcadIntegration.Server.ColumnName.WhereUsed");
		instanceWhereUsedColumn.setColumnKey("WHEREUSED");
		instanceWhereUsedColumn.setColumnDataType("string");
		instanceWhereUsedColumn.setColumnTarget("popup");
		instanceWhereUsedColumn.setColumnType("icon");
		instanceWhereUsedColumn.setColumnIsSortable(false);

		//Initializing column for Related Drawing action
		relatedDrawingColumn.setColumnTitle("mcadIntegration.Server.ColumnName.RelatedDrawing");
		relatedDrawingColumn.setColumnKey("RELATEDDRAWING");
		relatedDrawingColumn.setColumnDataType("string");
		relatedDrawingColumn.setColumnTarget("popup");
		relatedDrawingColumn.setColumnType("icon");
		relatedDrawingColumn.setColumnIsSortable(false);

		instanceDetailsColumn.setColumnTitle("mcadIntegration.Server.ColumnName.Details");
		instanceDetailsColumn.setColumnKey("INSTANCEDETAILS");
		instanceDetailsColumn.setColumnDataType("string");
		instanceDetailsColumn.setColumnTarget("popup");
		instanceDetailsColumn.setColumnType("icon");
		instanceDetailsColumn.setColumnIsSortable(false);

		instanceNavigateColumn.setColumnTitle("mcadIntegration.Server.ColumnName.Navigate");
		instanceNavigateColumn.setColumnKey("INSTANCENAVIGATE");
		instanceNavigateColumn.setColumnDataType("string");
		instanceNavigateColumn.setColumnTarget("popup");
		instanceNavigateColumn.setColumnType("icon");
		instanceNavigateColumn.setColumnIsSortable(false);

		dependentDocColumn.setColumnTitle("mcadIntegration.Server.ColumnName.DependentDocuments");
		dependentDocColumn.setColumnKey("DEPENDENTDOCS");
		dependentDocColumn.setColumnDataType("string");
		dependentDocColumn.setColumnTarget("popup");
		dependentDocColumn.setColumnType("icon");
		dependentDocColumn.setColumnIsSortable(false);

		instanceRepresentationsColumn.setColumnTitle("mcadIntegration.Server.ColumnName.Representations");
		instanceRepresentationsColumn.setColumnKey("INSTANCEREPRESENTATIONS");
		instanceRepresentationsColumn.setColumnDataType("string");
		instanceRepresentationsColumn.setColumnTarget("popup");
		instanceRepresentationsColumn.setColumnType("icon");
		instanceRepresentationsColumn.setColumnIsSortable(false);

		columnDefs.add(instanceNamesColumn);
		columnDefs.add(parentInstanceNamesColumn);
		columnDefs.add(actionsColumn);
		columnDefs.add(instanceWhereUsedColumn);
		columnDefs.add(relatedDrawingColumn);
		columnDefs.add(instanceDetailsColumn);
		columnDefs.add(instanceNavigateColumn);
		columnDefs.add(dependentDocColumn);
		columnDefs.add(instanceRepresentationsColumn);

		return columnDefs;
	}
}
