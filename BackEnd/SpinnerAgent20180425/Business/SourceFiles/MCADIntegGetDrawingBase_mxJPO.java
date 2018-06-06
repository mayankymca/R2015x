/*
 **  MCADIntegGetDrawingBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to get the related drawing object.
 */
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.DSCExpandObjectWithSelect;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleObjectExpander;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNodeImpl;
import com.matrixone.apps.domain.DomainObject;


public class MCADIntegGetDrawingBase_mxJPO 
{
	protected MCADGlobalConfigObject _globalConfig			 = null;
	protected MCADMxUtil _util								 = null;
	protected MCADServerGeneralUtil _generalUtil			 = null;
	protected MCADServerResourceBundle _serverResourceBundle = null;
	protected IEFGlobalCache	_cache						 = null;	
	protected DSCExpandObjectWithSelect _objectExpander	     = null;

	protected boolean isSequenceAvailable    				 = false;
	protected Vector revisionSequenceList    				 = null;

	public MCADIntegGetDrawingBase_mxJPO()
	{
	}

	public MCADIntegGetDrawingBase_mxJPO(Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("Not supported on desktop client!!!", null);

		Hashtable argsTable	= (Hashtable)JPO.unpackArgs(args);
		String sLanguage		= (String) argsTable.get("language");

		init(context, argsTable, sLanguage);
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	protected void init(Context context, Hashtable argsTable, String sLanguage)  throws Exception
	{
		_serverResourceBundle	= new MCADServerResourceBundle(sLanguage);
		_cache					= new IEFGlobalCache();
		_util					= new MCADMxUtil(context, _serverResourceBundle, _cache);
		_globalConfig			= (MCADGlobalConfigObject) argsTable.get("GCO");
		_generalUtil			= new MCADServerGeneralUtil(context,_globalConfig, _serverResourceBundle, _cache);
		_objectExpander			= new DSCExpandObjectWithSelect(context, _generalUtil , _util, _globalConfig);
	}

	/** This function contains the implementation for showing the vertical view.
	 The function is passed a business object ID as argument. The vertical view
	 is applied on the object of this busID, to show it's dependent objects on the
	 checkout page.
	 The implementation should be designed to use this business object ID and expand it,
	 and create a structure which contains different nodes with corresponding relationship IDs and busIDs.  
	 This XML structure should be returned by the function. It is then used by IEF
	 to show the related objects in the vertical view on the checkout page.

	 @param context The user context
	 @param args A string array of arguments used. The first element of the array MUST be the
	 busID, the others are optional, depending on the implementation.
	 @return a ParsedXML which contains nodes strcuture to be shown in checkout page. XML structure should be as shown below.
	 <viewdetails>
		<node busid="" relid="" isfloated="">
			<node busid="" relid="" isfloated=""/>
		</node>
		<node busid="" relid="" isfloated=""/>
	 </viewdetails>
	 */
	public IEFXmlNode getVerticalViewBOIDs(Context context, String []args) throws Exception
	{
		String busId				= args[0];
		String expandedObjectIds	= args[1];
		String expandDrawing		= args[2];
		Hashtable requestTable 		= new Hashtable(1);
		StringTokenizer objIDs		= new StringTokenizer(expandedObjectIds, ",");
		HashSet objIDSet			= new HashSet();

		while(objIDs.hasMoreTokens())
		{
			objIDSet.add((String)objIDs.nextToken().trim());
		}

		requestTable.put(busId, objIDSet);

		Vector argsVector = new Vector(2);
		argsVector.addElement(requestTable);
		argsVector.addElement(expandDrawing);

		String []requestInfo = JPO.packArgs(argsVector);

		Hashtable resultTable = getVerticalViewBOIDsForObjectIds(context, requestInfo);
		return (IEFXmlNode)resultTable.get(busId);
	}

	public Hashtable getVerticalViewBOIDsForObjectIds(Context context, String []args) throws Exception
	{
		Vector arguments	   = (Vector) JPO.unpackArgs(args);
		Hashtable requestTable = (Hashtable) arguments.elementAt(0);
		String expandDrawing   = (String) arguments.elementAt(1);

		Hashtable returnTable  = new Hashtable(requestTable.size());
		try
		{
			Hashtable assemblyRelsAndEnds  = _globalConfig.getRelationshipsOfClass(MCADServerSettings.ASSEMBLY_LIKE);

			Hashtable relsAndEnds = new Hashtable();

			String associatedDrawing = MCADMxUtil.getActualNameForAEFData(context,"relationship_AssociatedDrawing"); 

			relsAndEnds.put(associatedDrawing, assemblyRelsAndEnds.get(associatedDrawing));
			relsAndEnds.put("CatDesignTable","to");
			relsAndEnds.put("CatMML","to");

			relsAndEnds							= MCADUtil.changeEndsForRelationshipMap(relsAndEnds);

			Hashtable busIdFloatedRelIdsMap = new Hashtable();

			Hashtable busIdDependentRelIdTable = floatDrawingOnDesignVersion(context, requestTable, relsAndEnds, busIdFloatedRelIdsMap);

			Enumeration busids 	   = requestTable.keys();

			while(busids.hasMoreElements())
			{
				String busId				 = (String) busids.nextElement();
				HashMap relIdParentDetails = (HashMap) busIdDependentRelIdTable.get(busId);
				HashSet expandedObjectIds	 = (HashSet)requestTable.get(busId);

				returnTable.put(busId, getVerticalViewBOIDs(context, busId, (HashSet)expandedObjectIds.clone(), expandDrawing, relIdParentDetails, busIdFloatedRelIdsMap));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return returnTable;
	}

	private IEFXmlNode getVerticalViewBOIDs(Context context, String busId, HashSet expandedObjectIds, String expandDrawing, HashMap relIdParentDetails, Hashtable busIdFloatedRelIdsMap) throws Exception
	{
		IEFXmlNode viewDetailsNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
		viewDetailsNode.setName("viewdetails");

		try
		{
			ArrayList floatedRelList = (ArrayList) busIdFloatedRelIdsMap.get(busId);

			Vector relNameList = new Vector();	
			relNameList.addElement(MCADMxUtil.getActualNameForAEFData(context,"relationship_AssociatedDrawing"));
			relNameList.addElement(MCADMxUtil.getActualNameForAEFData(context,"relationship_CatDesignTable"));
			relNameList.addElement(MCADMxUtil.getActualNameForAEFData(context,"relationship_CatMML"));

			String key							= "";
			Hashtable keyRevisionSequenceMap	= new Hashtable();
			Hashtable keyLatestRevisionMap		= new Hashtable();
			Hashtable keyRelationshipIDMap		= new Hashtable();
			Hashtable relationshipIDBusIDMap    = new Hashtable();
			Hashtable alreadyExpandedItems      = new Hashtable();
			Hashtable typeIsKindOfCADDrgMap     = new Hashtable();

			java.util.Set set = relIdParentDetails.entrySet();
			Iterator itr = set.iterator();
			while(itr.hasNext())
			{
				Map.Entry entry =  (Map.Entry)itr.next();  
				String relId = (String)entry.getKey();
				String connectedObjId = (String)entry.getValue(); 

				Relationship rel = new Relationship(relId);
				rel.open(context);
				String relName  = rel.getTypeName();
				rel.close(context);

				if(relNameList.contains(relName))
				{
					relationshipIDBusIDMap.put(relId, connectedObjId);

					BusinessObject connectedBusObj = new BusinessObject(connectedObjId);
					connectedBusObj.open(context);

					String connectedObjName = connectedBusObj.getName();
					String currentRevision  = connectedBusObj.getRevision();
					String connectedObjType = connectedBusObj.getTypeName();

					//if(!_globalConfig.isMajorType(connectedObjType))
					//{
					//	connectedObjType = _util.getCorrespondingType(context, connectedObjType);
					//} [NDM] QWJ Comment

					if(!typeIsKindOfCADDrgMap.containsKey(connectedObjType))
					{
						String Args[] = new String[3];
						Args[0] = "type";
						Args[1] = connectedObjType;
						Args[2] = "kindof["+MCADMxUtil.getActualNameForAEFData(context, "type_CADDrawing")+"]";
						String mqlResult = _util.executeMQL(context, "print $1 $2 select $3", Args);

						if(mqlResult.startsWith("true"))
						{
							mqlResult = mqlResult.substring(mqlResult.indexOf("=")+2);
							typeIsKindOfCADDrgMap.put(connectedObjType, mqlResult);
						}
					}

					if("TRUE".equalsIgnoreCase((String)typeIsKindOfCADDrgMap.get(connectedObjType)))// Drawing check is introduced for preventing showing other type in structure for CATMML relation.
					{
						key = connectedObjName + "|" + connectedObjType;
						if(expandedObjectIds.contains(connectedObjId))
						{
							alreadyExpandedItems.put(key, relId);								
						}

						if(!keyLatestRevisionMap.containsKey(key))
						{
							keyLatestRevisionMap.put(key, currentRevision);
							keyRelationshipIDMap.put(key, relId);
						}
						else
						{
							Vector revisionSequence = (Vector)keyRevisionSequenceMap.get(key);
							if(revisionSequence == null)
							{
								revisionSequence = generateRevisionSequenceData(context, connectedObjId);
								keyRevisionSequenceMap.put(key, revisionSequence);
							}

							String latestRevision = (String)keyLatestRevisionMap.get(key);
							if(revisionSequence.indexOf(currentRevision) > revisionSequence.indexOf(latestRevision))
							{
								keyLatestRevisionMap.put(key, currentRevision);
								keyRelationshipIDMap.put(key, relId);
							}
						}
					}
				}
			}

			Enumeration expandedIDsElements = alreadyExpandedItems.keys();
			while(expandedIDsElements.hasMoreElements())
			{
				String expandedObjectKey   = (String)expandedIDsElements.nextElement();
				String relatID = (String)alreadyExpandedItems.get(expandedObjectKey);				
				keyRelationshipIDMap.put(expandedObjectKey, relatID);

			}

			//Add selected node to the already expanded nodes list to avoid repetition when drawing object is expanded
			if(!expandedObjectIds.contains(busId))
				expandedObjectIds.add(busId);

			Enumeration relationshipIDsElements = keyRelationshipIDMap.elements();
			while(relationshipIDsElements.hasMoreElements())
			{

				String relationshipID = (String)relationshipIDsElements.nextElement();
				String busID   = (String)relationshipIDBusIDMap.get(relationshipID);

				if(!expandedObjectIds.contains(busID))
				{
					IEFXmlNode viewNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
					viewNode.setName("node");

					Hashtable attributesTable = new Hashtable();
					attributesTable.put("busid", busID);
					attributesTable.put("relid", relationshipID);

					attributesTable.put("isfloated", new Boolean(floatedRelList.contains(relationshipID)).toString());

					viewNode.setAttributes(attributesTable);

					viewDetailsNode.addNode(viewNode);

					//Get dependent nodes for drawing object
					if(expandDrawing.equals("true"))
						addChildNodes(context, busID, viewNode, expandedObjectIds);
				}
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

		return viewDetailsNode;
	}

	protected Vector generateRevisionSequenceData(Context _context, String busID)
	{
		Vector sortedRevisionsList = new Vector();

		try
		{
			BusinessObject busObject = new BusinessObject(busID);
			busObject.open(_context);

			BusinessObject connectedMajorBusObject = null;   
			if(_util.isMajorObject(_context, busID))//_globalConfig.isMajorType(busObject.getTypeName())) // {NDM] OP6
			{
				connectedMajorBusObject = busObject;
			}
			else
			{
				connectedMajorBusObject = _util.getMajorObject(_context, busObject);
				connectedMajorBusObject.open(_context);
			}

			BusinessObjectList majorBusObjectsList	= connectedMajorBusObject.getRevisions(_context);
			BusinessObjectItr majorBusObjectsItr	= new BusinessObjectItr(majorBusObjectsList);
			while(majorBusObjectsItr.next())
			{
				BusinessObject majorBusObject = majorBusObjectsItr.obj();
				majorBusObject.open(_context);

				String majorRevision = majorBusObject.getRevision();

				BusinessObjectList minorBusObjectsList = _util.getMinorObjects(_context, majorBusObject);
				if(minorBusObjectsList.size() > 0)
				{
					BusinessObject connectedMinorBusObject = minorBusObjectsList.getElement(0);

					minorBusObjectsList = connectedMinorBusObject.getRevisions(_context);

					BusinessObjectItr minorBusObjectsItr = new BusinessObjectItr(minorBusObjectsList);        
					while(minorBusObjectsItr.next())
					{
						BusinessObject minorBusObject = minorBusObjectsItr.obj();
						minorBusObject.open(_context);
						String minorRevision = minorBusObject.getRevision();
						minorBusObject.close(_context);

						sortedRevisionsList.addElement(minorRevision);
					}
				}
				//[NDM] H68 : Needs to remove finalization logic... 

				//boolean isFinalized = _generalUtil.isBusObjectFinalized(_context, majorBusObject);
				//if(isFinalized)
					sortedRevisionsList.addElement(majorRevision);

				majorBusObject.close(_context);
			}
		}
		catch (Exception ex)
		{

		}

		return sortedRevisionsList;  
	}

	protected void addChildNodes(Context _context, String parentObjectId, IEFXmlNode parentNode, HashSet alreadyExpandedObjectIDs) throws MCADException
	{
		Hashtable relsAndEnds	= _globalConfig.getRelationshipsOfClass(MCADServerSettings.ASSEMBLY_LIKE);
		String queryResult		= _generalUtil.getFilteredFirstLevelChildAndRelIds(_context, parentObjectId, true, relsAndEnds, null, true, null);

		StringTokenizer childObjectsTokens = new StringTokenizer(queryResult,"\n");
		while(childObjectsTokens.hasMoreTokens())
		{
			String childObjectDetails = childObjectsTokens.nextToken();

			StringTokenizer childObjectElements = new StringTokenizer(childObjectDetails, "|");

			String level		= childObjectElements.nextToken();
			String relName		= childObjectElements.nextToken();
			String direction	= childObjectElements.nextToken();
			String objectId		= childObjectElements.nextToken();
			String relId		= childObjectElements.nextToken();

			if(!alreadyExpandedObjectIDs.contains(objectId))
			{
				IEFXmlNode viewNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
				viewNode.setName("node");

				Hashtable attributesTable = new Hashtable();
				attributesTable.put("busid", objectId);
				attributesTable.put("relid", relId);
				viewNode.setAttributes(attributesTable);

				parentNode.addNode(viewNode);
			}
		}
	}

	// [NDM] QWJ Start
        /*private Hashtable getIdPreviousVersionsIdtable(Context context, ArrayList objectIds) throws Exception
	{
		String REL_ACTIVE_VERSION		= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");

		String SELECT_ON_ACTIVE_MINOR	= "from[" + REL_ACTIVE_VERSION + "].to.";

		Hashtable returnTable = new Hashtable();

		String [] oidList = new String [objectIds.size()];
		objectIds.toArray(oidList);

		StringList  busSelectList = new StringList();

		busSelectList.add("id");
		busSelectList.add("type");
		busSelectList.add("revision");
		busSelectList.add("revisions");
		busSelectList.add("revisions.id");

		busSelectList.add(SELECT_ON_ACTIVE_MINOR + "revision");
		busSelectList.add(SELECT_ON_ACTIVE_MINOR + "revisions");
		busSelectList.add(SELECT_ON_ACTIVE_MINOR + "revisions.id");

		BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, oidList, busSelectList);

		for(int i = 0 ; i < busWithSelectList.size(); i++)
		{
			ArrayList versionsList = new ArrayList();

			BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(i);

			// [NDM] : QWJ
			String busId	= busWithSelect.getSelectData("id");
			returnTable.put(busId, versionsList);

			//String typeName = busWithSelect.getSelectData("type");
			String revision = busWithSelect.getSelectData("revision");
			StringList versions = busWithSelect.getSelectDataList("revisions");

			// [NDM] : QWJ
			if(_util.isMajorObject(context, busId))
			{
				//If Input is major the add all versions older than the active minor (major means it is finalized)
				revision = busWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + "revision");
				versions = busWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + "revisions");

				if(versions != null)
				{
					for(int j = 0; j < versions.size();j++)
					{
						String currentVersion = (String)versions.elementAt(j);
						if(currentVersion.equals(revision))
							break;
						else
						{
							versionsList.add(busWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + "revisions[" + currentVersion + "].id"));
						}
					}
				}
			}
			else
			{
				if(versions != null)
				{
					for(int j = 0; j < versions.size();j++)
					{
						String currentVersion = (String)versions.elementAt(j);
						if(currentVersion.equals(revision))
							break;
						else
						{
							versionsList.add(busWithSelect.getSelectData("revisions[" + currentVersion + "].id"));
						}
					}
				}
			}
		}

		return returnTable;
	}
[NDM] QWJ end */

	private Hashtable getTypeAndNameTableForAllChildIds(Context context,Hashtable busIdDependentRelIdTable) throws Exception
	{
		Hashtable returnTable = new Hashtable();

		ArrayList inputIds = new ArrayList();

		Enumeration expandedIds = busIdDependentRelIdTable.keys();
		while(expandedIds.hasMoreElements())
		{
			String expandedid = (String) expandedIds.nextElement();
			HashMap relIdParentDetails = (HashMap) busIdDependentRelIdTable.get(expandedid);

			if(relIdParentDetails != null && !relIdParentDetails.isEmpty())
			{
				java.util.Set set = relIdParentDetails.entrySet();
				Iterator itr = set.iterator();
				while(itr.hasNext())
				{        
					Map.Entry entry =  (Map.Entry)itr.next();  
					Object connectedObjId = entry.getValue();

					inputIds.add(connectedObjId);
				}

			}
		}

		String [] oidList = new String [inputIds.size()];
		inputIds.toArray(oidList);

		StringList  busSelectList = new StringList();

		busSelectList.add("id");
		busSelectList.add("type");
		busSelectList.add("name");

		BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, oidList, busSelectList);

		for(int i = 0 ; i < busWithSelectList.size(); i++)
		{
			BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(i);

			String id = busWithSelect.getSelectData("id");

			String type = busWithSelect.getSelectData("type");
			String name = busWithSelect.getSelectData("name");

			String typeNameKey = new StringBuffer(type).append("|").append(name).toString();

			returnTable.put(id, typeNameKey);
		}

		return returnTable;
	}

	private ArrayList getChildIdsTypeAndName(HashMap relIdParentDetails, Hashtable childidTypeAndNameTable)
	{
		ArrayList returnList = new  ArrayList();

		if(relIdParentDetails != null && !relIdParentDetails.isEmpty())
		{
			java.util.Set set = relIdParentDetails.entrySet();
			Iterator itr = set.iterator();
			while(itr.hasNext())
			{
				Map.Entry entry =  (Map.Entry)itr.next();  
				String connectedObjId = (String)entry.getValue();         

				returnList.add(childidTypeAndNameTable.get(connectedObjId));
			}
		}

		return returnList;
	}

	private void getModifiedRelidsTableBasedOnChildTypeAndName(Context context,ArrayList childIdsTypeAndName, ArrayList previousVersionsList, 	Hashtable busIdDependentRelIdTable, HashMap relIdParentDetails, Hashtable childidTypeAndNameTable, HashSet expandedObjectIds, ArrayList floatedRelList) throws Exception
	{
		for(int i = previousVersionsList.size()-1; i >= 0; --i)
		{
			String previousVersionId = (String) previousVersionsList.get(i);

			HashMap ChildRelIdParentDetails = (HashMap) busIdDependentRelIdTable.get(previousVersionId);

			if(ChildRelIdParentDetails != null && !ChildRelIdParentDetails.isEmpty())
			{
				java.util.Set set = ChildRelIdParentDetails.entrySet();
				Iterator itr = set.iterator();
				while(itr.hasNext())
				{
					Map.Entry entry =  (Map.Entry)itr.next();  
					String relId = (String)entry.getKey();          
					String connectedObjId = (String)entry.getValue();         

					String childTypeAndName = (String) childidTypeAndNameTable.get(connectedObjId);

					String correspondingTypeAndName = _globalConfig.getCorrespondingType(childTypeAndName.substring(0, childTypeAndName.indexOf("|"))) + childTypeAndName.substring(childTypeAndName.indexOf("|"));

					if(!childIdsTypeAndName.contains(childTypeAndName) && !childIdsTypeAndName.contains(correspondingTypeAndName))
					{
						relIdParentDetails.put(relId, connectedObjId);

						childIdsTypeAndName.add(childTypeAndName);
						childIdsTypeAndName.add(correspondingTypeAndName);

						expandedObjectIds.add(previousVersionId);

						floatedRelList.add(relId);
					}
					Relationship rel = new Relationship(relId);
					rel.open(context);
					
					if(_generalUtil.isDrawingIterationFloated(context, previousVersionId, connectedObjId, rel.getTypeName()))
					{
						floatedRelList.add(relId);
					}
					rel.close(context);
				}
			}
		}
	}

	private Hashtable floatDrawingOnDesignVersion(Context context,Hashtable requestTable, Hashtable relsAndEnds, Hashtable busIdFloatedRelIdsMap)throws Exception
	{
		HashSet idForExpansion = new HashSet();
// [NDM] QWJ
		Hashtable objectIdPreviousVersionsIdTable   = getPreviousRevisionVersionIds(context, new ArrayList(requestTable.keySet())); 

		Enumeration inputIds =  requestTable.keys();

		while(inputIds.hasMoreElements())
		{
			String inputid = (String) inputIds.nextElement();

			ArrayList previousVersionsList =  (ArrayList)objectIdPreviousVersionsIdTable.get(inputid);

			if(previousVersionsList != null && previousVersionsList.size() > 0)
			{
				idForExpansion.addAll(previousVersionsList);
			}

			idForExpansion.add(inputid);
		}

		String [] oidList = new String [idForExpansion.size()];
		idForExpansion.toArray(oidList);

		IEFSimpleObjectExpander expander = new IEFSimpleObjectExpander(oidList, relsAndEnds,  null, null, (short)1);
		expander.expandInputObjects(context);

		HashMap busIdParentIdMap =  expander.getBusIdRelidChildBusIdMap();
		Hashtable busIdDependentRelIdTable = new Hashtable();

		java.util.Set set = busIdParentIdMap.entrySet();
		Iterator itr = set.iterator();
		while(itr.hasNext())
		{        
			Map.Entry entry =  (Map.Entry)itr.next();  
			Object key = entry.getKey();          
			Object val = entry.getValue();         
			if(key==null)
			{            
				key = ""; 
			}if(val==null)
			{             
				val = ""; 
			}
			busIdDependentRelIdTable.put(key,val);
		}

		/*Hashtable busIdDependentRelIdTable	= _objectExpander.getRelationshipAndChildObjectInfoForParent(context, objectIdExpLevelMap, relsAndEnds, new HashMap(), new Hashtable(), false,MCADAppletServletProtocol.VIEW_AS_STORED,null,null);*/

		Hashtable busidTypeAndNameTable = getTypeAndNameTableForAllChildIds(context, busIdDependentRelIdTable);

		inputIds =  requestTable.keys();

		while(inputIds.hasMoreElements())
		{
			String inputid = (String) inputIds.nextElement();

			ArrayList floatedRelList = new  ArrayList();

			HashSet expandedObjectIds  = (HashSet) requestTable.get(inputid);

			HashMap relIdParentDetails = (HashMap) busIdDependentRelIdTable.get(inputid);

			ArrayList childIdsTypeAndName = getChildIdsTypeAndName(relIdParentDetails, busidTypeAndNameTable);

			ArrayList previousVersionsList =  (ArrayList)objectIdPreviousVersionsIdTable.get(inputid);

			getModifiedRelidsTableBasedOnChildTypeAndName(context,childIdsTypeAndName, previousVersionsList, busIdDependentRelIdTable, relIdParentDetails, busidTypeAndNameTable, expandedObjectIds, floatedRelList);

			busIdFloatedRelIdsMap.put(inputid, floatedRelList);
		}

		return busIdDependentRelIdTable;
	}
// [NDM] QWJ
	private Hashtable getPreviousRevisionVersionIds(Context context, ArrayList objectIds) throws Exception 
	{
		Hashtable returnTable 			= new Hashtable();

		String[] busIds					= new String[objectIds.size()];
		objectIds.toArray(busIds);
		
		String REL_VERSION_OF 			= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		String REL_ACTIVE_VERSION		= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");

		String IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context,"attribute_IsVersionObject");
		String SELECT_ISVERSIONOBJ = "attribute["+IS_VERSION_OBJ+"]";
		
		String SELECT_ON_MAJOR 			= "from[" + REL_VERSION_OF + "].to.";
		String SELECT_ON_ACTIVE_MINOR	= "from[" + REL_ACTIVE_VERSION + "].to.";

		String REVISIONS 				= "revisions";
		String ID_ON_REVISIONS 			= "revisions.id";
		String REVINDEX_ON_REVISIONS 	= "revisions.revindex";

		StringBuffer REV_ON_ACTIVE_MINOR = new StringBuffer("revisions.");
		REV_ON_ACTIVE_MINOR.append(SELECT_ON_ACTIVE_MINOR);
		REV_ON_ACTIVE_MINOR.append(REVISIONS);

		StringList busSelectionList = new StringList(14);
		busSelectionList.add(DomainObject.SELECT_ID);
		busSelectionList.add(DomainObject.SELECT_REVISION);
		busSelectionList.add(SELECT_ON_MAJOR+DomainObject.SELECT_REVISION);
		busSelectionList.add(REVISIONS);
		busSelectionList.add(SELECT_ISVERSIONOBJ);
		busSelectionList.add(ID_ON_REVISIONS);
		busSelectionList.add(REV_ON_ACTIVE_MINOR.toString());
		busSelectionList.add(REV_ON_ACTIVE_MINOR.toString() + ".id");

		busSelectionList.add(SELECT_ON_MAJOR + REVISIONS);

		busSelectionList.add(SELECT_ON_MAJOR + ID_ON_REVISIONS);
		busSelectionList.add(SELECT_ON_MAJOR + REV_ON_ACTIVE_MINOR.toString());
		busSelectionList.add(SELECT_ON_MAJOR + REV_ON_ACTIVE_MINOR.toString() + ".id");

		busSelectionList.add(REVINDEX_ON_REVISIONS);
		busSelectionList.add(SELECT_ON_MAJOR + REVINDEX_ON_REVISIONS);

		BusinessObjectWithSelectList busWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, busIds, busSelectionList);

		for(int i=0; i<busWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect	= (BusinessObjectWithSelect)busWithSelectionList.elementAt(i);
			String objId  									= busObjectWithSelect.getSelectData(DomainObject.SELECT_ID);
			String revision  								= busObjectWithSelect.getSelectData(DomainObject.SELECT_REVISION);
			String sIsVersion= busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);

			boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
			StringList majorRevisions 						= null;
	
			//return only previous revisions 
			int currentRevIndex								= 0;
			int revIndex									= 0;		
			ArrayList allIdList	= new ArrayList();
			
			if(!isVersion)
			{
				String sCurrentRevIndex							= busObjectWithSelect.getSelectData("revisions[" + revision+ "].revindex");
				if(sCurrentRevIndex != null && !"".equals(sCurrentRevIndex));
				currentRevIndex	 = Integer.valueOf(sCurrentRevIndex).intValue();

				majorRevisions = busObjectWithSelect.getSelectDataList(REVISIONS);
				for(int j = (majorRevisions.size() - 1 ); j > -1 ; j--)
				{
					String majorId 			= busObjectWithSelect.getSelectData("revisions[" + majorRevisions.get(j) + "].id");
					String sRevIndex 		= busObjectWithSelect.getSelectData("revisions[" + majorRevisions.get(j) + "].revindex");

					if(sRevIndex != null && !"".equals(sRevIndex));
					revIndex	 = Integer.valueOf(sRevIndex).intValue();

					if(revIndex<=currentRevIndex)
					{
						if(null != majorId && !"".equals(majorId))
							allIdList.add(majorId);

						StringList minorVersions = busObjectWithSelect.getSelectDataList("revisions[" + majorRevisions.get(j) + "]." + SELECT_ON_ACTIVE_MINOR + REVISIONS);
						for(int k = (minorVersions.size() - 1 ); k > -1 ; k--)
						{
							String minorId	= busObjectWithSelect.getSelectData(
									"revisions[" + majorRevisions.get(j) + "]." + SELECT_ON_ACTIVE_MINOR + 
									"revisions[" + minorVersions.get(k) + "].id");

							if(null != minorId && !"".equals(minorId))
								allIdList.add(minorId);
						}
					}

					revIndex =0;
				}
			}
			else
			{
				revision  				= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + DomainObject.SELECT_REVISION);
				String sCurrentRevIndex	= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + revision+ "].revindex");
				if(sCurrentRevIndex != null && !"".equals(sCurrentRevIndex));
				currentRevIndex	 = Integer.valueOf(sCurrentRevIndex).intValue();

				majorRevisions  = busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR + REVISIONS);

				for(int j = (majorRevisions.size() - 1 ); j > -1 ; j--)
				{
					String majorId 			= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + majorRevisions.get(j) + "].id");
					String sRevIndex 		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + majorRevisions.get(j) + "].revindex");

					if(sRevIndex != null && !"".equals(sRevIndex));
					revIndex	 = Integer.valueOf(sRevIndex).intValue();

					if(revIndex<=currentRevIndex)
					{
						if(null != majorId && !"".equals(majorId))
							allIdList.add(majorId);

						StringList minorVersions = busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR + "revisions[" + majorRevisions.get(j) + "]." + SELECT_ON_ACTIVE_MINOR + REVISIONS);

						for(int k = (minorVersions.size() - 1 ); k > -1 ; k--)
						{
							String minorId	= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + 
									"revisions[" + majorRevisions.get(j) + "]." + SELECT_ON_ACTIVE_MINOR + 
									"revisions[" + minorVersions.get(k) + "].id");

							if(null != minorId && !"".equals(minorId))
								allIdList.add(minorId);
						}
					}

					revIndex = 0;
				}
			}
			
			returnTable.put(objId, allIdList);
		}

		return returnTable;
	}
}

