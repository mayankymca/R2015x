/*
 **   emxMSPIGetSyncUIColumnDetails
 **   To get column details to show in Sync UI dialog in MS Project application
 */

import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;
import java.util.Map;
import java.util.ListIterator;
import java.util.StringTokenizer;

import matrix.db.AttributeType;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringItr;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.framework.ui.UITable;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.program.ProjectSpace;
import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.jdom.Document;
import com.matrixone.jdom.Element;
import com.matrixone.jdom.output.XMLOutputter;
import com.matrixone.apps.common.ProjectManagement;


/**
 * The <code>emxMSPIGetSyncUIColumnDetails</code> class contains methods for UI Table Component.
 */

public class emxMSPIGetSyncUIColumnDetails_mxJPO
{
    //cache the type and parent type mapping
    private Context context                                 = null;
	private	i18nNow i18n	= new i18nNow();
    
   /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     */

    public emxMSPIGetSyncUIColumnDetails_mxJPO (Context context, String[] args) throws Exception
    {
        this.context = context;
        init(context);
    }

    private void init(Context context) throws Exception
    {
        String language         = Locale.getDefault().getLanguage();
    }

    public String renderTable(Context context, String[] args) throws Exception
    {
		String xmlOutput = null;
        String tableName        = args[1];
        String language         = args[0];
        MapList retMapList = renderTable(context, tableName, language);
        Map stateMap = renderState(context,language);
        Map constraintTypeMap = renderConstraintType(context,language);
        Map taskReqMap = renderTaskReq(context,language);        
		xmlOutput = buildXmlOutput(context, retMapList,stateMap,constraintTypeMap,taskReqMap);	
        return xmlOutput;
    }    

	private Map renderTaskReq(Context context, String language) throws Exception
    {
    	Map taskReqLbl = new HashMap();
    	String label = new String();
    	
    	AttributeType atrTaskRequirement = new AttributeType(DomainConstants.ATTRIBUTE_TASK_REQUIREMENT);
		atrTaskRequirement.open(context);
		StringList strList = atrTaskRequirement.getChoices(context);
		atrTaskRequirement.close(context);
		StringItr  strTaskReqItr = new StringItr(strList);
		
		while (strTaskReqItr.next())
        {
			label = i18nNow.getRangeI18NString(DomainConstants.ATTRIBUTE_TASK_REQUIREMENT,(String) strTaskReqItr.obj(), language);        	
			taskReqLbl.put(strTaskReqItr.obj(), label);            
        }
		return taskReqLbl;
	}

	private Map renderConstraintType(Context context2, String language) throws Exception
    {
    	Map constraintLbl = new HashMap();
    	String label = new String();
    	
    	AttributeType atrDefaultConstraint = new AttributeType(ProjectManagement.ATTRIBUTE_TASK_CONSTRAINT_TYPE);
		atrDefaultConstraint.open(context);
		StringList strList = atrDefaultConstraint.getChoices(context);
		atrDefaultConstraint.close(context);
		StringItr  strConstrItr = new StringItr(strList);
		while (strConstrItr.next())
        {
			label = i18nNow.getRangeI18NString(ProjectManagement.ATTRIBUTE_TASK_CONSTRAINT_TYPE,(String) strConstrItr.obj(), language);        	
			constraintLbl.put(strConstrItr.obj(), label);            
        }
		return constraintLbl;
	}

	private Map renderState(Context context, String language) throws Exception
    {    	
    	MapList outputMapList    = new MapList();
    	Map stateLabel = new HashMap();
    	Map stateLbl = new HashMap();
    	
    	String label = new String();
    	String sPolicy = (String)PropertyUtil.getSchemaProperty("policy_ProjectTask");
    	StringList stateList = ProjectSpace.getStates(context, sPolicy);
    	StringItr  strStateItr = new StringItr(stateList);
    	while (strStateItr.next())
        {
        	label = i18nNow.getStateI18NString(sPolicy,strStateItr.obj(), language);
        	stateLabel.put(strStateItr.obj(), label);            
        }
    	
    	stateLbl.put("StateLabel", stateLabel);    	
    	
		return stateLabel;
	}

    /**
     * Method for rendering a table
     * This is an extract of the emxTable.jsp from AEF
     * The extract of the JSP is made into a JPO since the Office Integration does not go through the jsp layer
     *
     * @param context the eMatrix <code>Context</code> object
     * @param tableName the name of the table to be used for rendering
     * @param totalResult the list of ids that needs to be rendered
     *                     this list is obtained by running the JPO or inquiry
     *                     if id is absent "nodeType" key must be present with value 
     *                          "file"   - denoting file like node
     *                          "folder" - denoting folder like node
     * @return a MapList object
     * @throws Exception if the operation fails
     */

    public MapList renderTable(Context context, String tableName, String language) throws Exception
    {
		MapList outputMapList    = new MapList();
		try
		{
			Vector userRoleList       = PersonUtil.getAssignments(context);  
			
			if(tableName != null && tableName.length() > 0)
				tableName = tableName.trim();

			HashMap tableMap        = UITable.getTable(context, tableName);
			
			if (tableMap == null)
			{
				MCADException.createException("Unable to get Table configuration for " + tableName, null);
			}
			else
			{
				MapList columns = UITable.getColumns(context, tableName, userRoleList);
				if (columns == null || columns.size() == 0 )
				{
				}
				else
				{
					int noOfColumns = columns.size();

					String columnName[]         = new String[noOfColumns];
					String columnHeaders[]      = new String[noOfColumns];
					String columnHeaderOrder	= "";
					
					//initialize the variables.
					HashMap columnMap       = new HashMap();                
						
					for (int i=0; i < noOfColumns; i++)
					{
						columnMap           = (HashMap)columns.get(i);
						// Get column details
						String colName      = UITable.getName(columnMap);
						String columnLabel  = UITable.getLabel(columnMap);                    
						
						String registeredSuite  = UITable.getSetting(columnMap, "Registered Suite");
						String StringResFileId  = "";

						if (registeredSuite != null && registeredSuite.length() != 0)
						{
							StringResFileId = UINavigatorUtil.getStringResourceFileId(registeredSuite);
						}
						
						columnHeaderOrder = columnHeaderOrder +  colName + ",";

						if (columnLabel != null && columnLabel.trim().length() > 0 )
						{
							//columnHeaders[i] = UINavigatorUtil.getI18nString(columnLabel.trim(), StringResFileId , language);
							columnHeaders[i] = i18n.GetString(StringResFileId,language,columnLabel.trim());
						}

						if (columnHeaders[i] == null || columnHeaders[i].trim().length() == 0 || columnHeaders[i].equals("null"))
							columnHeaders[i] = "";  
											
						columnName[i]           = colName;					
					}//end of column iteration
					
					Map columnHeaderMap                 = new HashMap();
					for (int i = 0; i < noOfColumns; i++)
					{					
						//create a map of column name and header value
						columnHeaderMap.put(columnName[i], columnHeaders[i]);                        
					} 
					Map columnHeaderOrderMap = new HashMap();
					columnHeaderMap.put("columnHeaderOrder",columnHeaderOrder);
					outputMapList.add(columnHeaderMap);	
				} // end of columns null check
			} // end of tableMap null check			
		}
		catch(Exception e)
		{
			System.out.println(e.toString());
		}
		return outputMapList;		
	}
	

    public String buildXmlOutput(Context context, MapList detailsMapList, Map stateLabelMap,Map ConstrainTypeMap, Map taskReqMap) throws Exception	
	{
		String xmlStringValue = null;
		try 
		{
			// create the response placeholder (root node) 
			Element elMainNode = new Element("LabelInformation");
			
			Element elDetailsNode = new Element("Table");

			elMainNode.addContent(elDetailsNode);
			//create headerList and objectList node
			Element elObjectListNode = new Element("Columnlist");

			//add create headerList and objectList node to the Details node
			elDetailsNode.addContent(elObjectListNode);

			//Iterate through the list of menu contents and get the required info
			ListIterator detailsMapItr = detailsMapList.listIterator();

			while (detailsMapItr.hasNext())
			{
				Map detailsMap = (Map) detailsMapItr.next();
				 			    
				String columnHeaderOrder = (String) detailsMap.get("columnHeaderOrder");
				StringTokenizer parser = new StringTokenizer(columnHeaderOrder,",");
				
				while (parser.hasMoreTokens())
				{
					//add the object column details
					Element elObjectNode = new Element("Column");
					elObjectListNode.addContent(elObjectNode);					
					//remove space in the name
					String sClmnHeader = parser.nextToken();
					String MapKeyName = MCADUtil.removeSpace(sClmnHeader); 
					try
					{
						String MapValue = (String) detailsMap.get(MapKeyName);
						if(MapValue != null)
						{
							elObjectNode.setAttribute("Name", MapKeyName);							
							elObjectNode.setAttribute("Label", MapValue);
						}
						else
						{
							elObjectNode.setAttribute("Name", "");
							elObjectNode.setAttribute("Label", "");
						}
					}
					catch (ClassCastException ex)
					{
					}
				}				
			}

			Element elLabelNode = new Element("Label");
			Element elStateNode = new Element("StateList");
			
			elLabelNode.addContent(elStateNode);
			elMainNode.addContent(elLabelNode);
			
			Iterator kState = stateLabelMap.keySet().iterator();
			while (kState.hasNext()) 
			{	
				Element elObjectNode = new Element("State");
				elStateNode.addContent(elObjectNode);
				String key = (String) kState.next();
				String value = (String)stateLabelMap.get(key);
				elObjectNode.setAttribute("Name", key);
				elObjectNode.setAttribute("Label", value);
			}		
			
			Element elConstraintNode = new Element("ConstraintList");
			elLabelNode.addContent(elConstraintNode);
			
			Iterator kConstraint = ConstrainTypeMap.keySet().iterator();
			while (kConstraint.hasNext()) 
			{	
				Element elObjectNode = new Element("Constraint");
				elConstraintNode.addContent(elObjectNode);
				String key = (String) kConstraint.next();
				String value = (String)ConstrainTypeMap.get(key);
				elObjectNode.setAttribute("Name", key);
				elObjectNode.setAttribute("Label", value);
			}
			
			Element elTaskReqNode = new Element("TaskRequirementList");
			elLabelNode.addContent(elTaskReqNode);
			
			Iterator kTaskReq = taskReqMap.keySet().iterator();
			while (kTaskReq.hasNext()) 
			{	
				Element elObjectNode = new Element("TaskReq");
				elTaskReqNode.addContent(elObjectNode);
				String key = (String) kTaskReq.next();
				String value = (String)taskReqMap.get(key);
				elObjectNode.setAttribute("Name", key);
				elObjectNode.setAttribute("Label", value);
			}		

			Document docResponse = new Document(elMainNode);
			docResponse.setDocType(null);

			XMLOutputter xmloResponse 	= new XMLOutputter();
			xmlStringValue = xmloResponse.outputString(docResponse);

			ContextUtil.commitTransaction(context);
		} 
		catch (Exception ex)
		{
			ContextUtil.abortTransaction(context);
			String message = ex.getMessage();
			MCADServerException.createException(message, ex);
		}
		return xmlStringValue;
	}	
}
