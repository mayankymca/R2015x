/*
 **  MCADIntegGetDrawing
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to get the related drawing object.
 */
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class MCADIntegGetDrawingExtended_mxJPO {

	private MCADGlobalConfigObject _globalConfig			= null;
	private MCADMxUtil _util								= null;
	private MCADServerGeneralUtil _generalUtil				= null;
	private MCADServerResourceBundle _serverResourceBundle	= null;
	private IEFGlobalCache	_cache							= null;

	/*** 
	 ***The value needs to be set in this way <relName1>,<relDirection1>,<type1@type2@type3>|<relName2>,<relDirection2>,<type2>

	 ** The direction specified is the direction on which the type specified is connected. for eg Cat Design Table is connected at "to" side with Cat Drawing in CatDesignTable relationship.

	 ** Only major types need to be specified here the code will look for the minor tpye also.
	 **/

	private String relationNameDirectionType		= "CatDesignTable,to,Cat Design Table|CatMML,to,Cat Part@Cat Product@Cat Drawing";


	public MCADIntegGetDrawingExtended_mxJPO()
	{
	}

	public MCADIntegGetDrawingExtended_mxJPO(Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("Not supported on desktop client!!!", null);
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	private void init(Context context, String[] packedGCO, String sLanguage)  throws Exception
	{
		_serverResourceBundle = new MCADServerResourceBundle(sLanguage);
		_cache				   = new IEFGlobalCache();
		_util = new MCADMxUtil(context, _serverResourceBundle, _cache);
		_globalConfig = (MCADGlobalConfigObject) JPO.unpackArgs(packedGCO);
		_generalUtil = new MCADServerGeneralUtil(context,_globalConfig, _serverResourceBundle, _cache);
	}

	/** This function contains the implementation for showing the vertical view.
 The function is passed a business object ID as argument. The vertical view
 is applied on the object of this busID, to show it's dependent objects on the
 checkout page.
 The implementation should be designed to use this business object ID and expand it,
 and create a hashtable which contains relationship IDs as "keys" and busIDs as "values".
				The keys in the hashtable should be the relationship IDs of those objects which are to be shown in the
				 vertical view , and the values should be the busIDs of the objects.
				This hashtable should be returned by the function. It is then used by IEF
 to show the related objects in the vertical view on the checkout page.

 @param context The user context
 @param args A string array of arguments used. The first element of the array MUST be the
 busID, the others are optional, depending on the implementation.
 @return a hashtable which contains relationship IDs of the objects which need to
 be shown as dependents in the vertical view  as "keys" and the objects corresponding
 busIDs as "values".
	 */
	public Hashtable getVerticalViewBOIDs(Context context, String []args) throws Exception
	{
		Hashtable verticalViewRelIDBusIDsTable = new Hashtable();

		// This is a MUST
		String busId = args[0];

		// These are optional, depending on the implementation.
		String [] packedGCO = new String[2];
		packedGCO[0] = args[1];
		packedGCO[1] = args[2];
		String sLanguage = args[3];

		init(context, packedGCO, sLanguage);

		try
		{
			String sRelName		= MCADMxUtil.getActualNameForAEFData(context,"relationship_AssociatedDrawing");	
			Hashtable relsAndEnds = _globalConfig.getRelationshipsOfClass(MCADServerSettings.ASSEMBLY_LIKE);
			// Get all drawing related this, drawings are "dependent"
			String queryResult	= _generalUtil.getFilteredFirstLevelChildAndRelIds(context, busId, false, relsAndEnds, null, true, null);

			Hashtable busNameRevisionSequenceMap = new Hashtable();
			Hashtable busNameLatestRevisionMap   = new Hashtable();
			Hashtable busNameRelationshipIDMap   = new Hashtable();
			Hashtable relationshipIDBusIDMap     = new Hashtable();

			StringTokenizer strTok = new StringTokenizer(queryResult,"\n");
			while(strTok.hasMoreTokens())
			{
				String row				   = strTok.nextToken();
				StringTokenizer rowElements = new StringTokenizer(row,"|");
				String level				   = rowElements.nextToken();
				String relName			   = rowElements.nextToken();
				String direction			   = rowElements.nextToken();
				String connectedObjId	   = rowElements.nextToken();
				String relId				   = rowElements.nextToken();

				if(relName.equals(sRelName))
				{
					relationshipIDBusIDMap.put(relId, connectedObjId);

					BusinessObject connectedBusObj = new BusinessObject(connectedObjId);
					connectedBusObj.open(context);

					String connectedObjName = connectedBusObj.getName();
					String currentRevision  = connectedBusObj.getRevision();
					if(!busNameLatestRevisionMap.containsKey(connectedObjName))
					{
						busNameLatestRevisionMap.put(connectedObjName, currentRevision);
						busNameRelationshipIDMap.put(connectedObjName, relId);
					}
					else
					{
						Vector revisionSequence = (Vector)busNameRevisionSequenceMap.get(connectedObjName);
						if(revisionSequence == null)
						{
							revisionSequence = generateRevisionSequenceData(context, connectedObjId);
							busNameRevisionSequenceMap.put(connectedObjName, revisionSequence);
						}

						String latestRevision = (String)busNameLatestRevisionMap.get(connectedObjName);
						if(revisionSequence.indexOf(currentRevision) > revisionSequence.indexOf(latestRevision))
						{
							busNameLatestRevisionMap.put(connectedObjName, currentRevision);
							busNameRelationshipIDMap.put(connectedObjName, relId);
						}
					}     
				}
			}

			Enumeration relationshipIDsElements = busNameRelationshipIDMap.elements();
			while(relationshipIDsElements.hasMoreElements())
			{
				String relationshipID = (String)relationshipIDsElements.nextElement();
				String busID		  = (String)relationshipIDBusIDMap.get(relationshipID);

				verticalViewRelIDBusIDsTable.put(relationshipID, busID);
			}
			getExtendedVerticalViewBOIDs(context, verticalViewRelIDBusIDsTable);
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

		// Return the hashtable, to be used by IEF for showing the vertical view
		return verticalViewRelIDBusIDsTable;
	}


	private Vector generateRevisionSequenceData(Context _context, String busID)
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

			BusinessObjectList majorBusObjectsList = connectedMajorBusObject.getRevisions(_context);
			BusinessObjectItr majorBusObjectsItr = new BusinessObjectItr(majorBusObjectsList);
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

	private void getExtendedVerticalViewBOIDs(Context _context, Hashtable verticalViewRelIDBusIDsTable)  throws Exception
	{
		Hashtable relNameTypeMap = populateRelNameTypeMap(_context);

		Enumeration relIdsList = verticalViewRelIDBusIDsTable.keys();
		int count = 0;
		while(relIdsList.hasMoreElements())
		{
			String relID = (String)relIdsList.nextElement();
			String busID = (String)verticalViewRelIDBusIDsTable.get(relID);

			// Get all object Related to this Drawing
			String Args[] = new String[2];
			Args[0] = busID;
			Args[1] = "|"; 
			String queryResult  = _util.executeMQL(_context ,"expand bus $1 dump $2", Args);

			if(queryResult.startsWith("true"))
				queryResult = queryResult.substring(5);
			else
				continue;

			StringTokenizer strTok = new StringTokenizer(queryResult,"\n");
			while(strTok.hasMoreTokens())
			{
				String row					= strTok.nextToken();
				StringTokenizer rowElements = new StringTokenizer(row,"|");

				String level				= rowElements.nextToken();
				String relName				= rowElements.nextToken();
				String direction			= rowElements.nextToken();
				String objType				= rowElements.nextToken();
				String objName				= rowElements.nextToken();
				String objRev				= rowElements.nextToken();

				relName = relName + "|" + direction;
				if(relNameTypeMap.containsKey(relName))
				{
					BusinessObject connectedBus = new BusinessObject(objType, objName, objRev, "");
					connectedBus.open(_context);
					String connectedObjId = connectedBus.getObjectId();
					connectedBus.close(_context);

					verticalViewRelIDBusIDsTable.put(("dummy"+ count++), connectedObjId);

				}
			}
		}
	}

	// The method returns as hashtable with relName as the key and a Vector containng all the types at the value. The first member of the vector will be the relation ship direction
	private Hashtable populateRelNameTypeMap(Context _context) throws Exception
	{
		Hashtable relNameTypeMap = new Hashtable();
		StringTokenizer relNameTypeRowTokens = new StringTokenizer(relationNameDirectionType, "|");
		while(relNameTypeRowTokens.hasMoreTokens())
		{
			String relNameTypeRow				= relNameTypeRowTokens.nextToken();
			StringTokenizer relNameTypeTokens	= new StringTokenizer(relNameTypeRow , ",");
			String relName						= relNameTypeTokens.nextToken();
			String direction					= relNameTypeTokens.nextToken();
			String types						= relNameTypeTokens.nextToken();

			String sRelName						= _util.getSymbolicName(_context, "relationship", relName);
			sRelName							= MCADMxUtil.getActualNameForAEFData(_context, sRelName);
			Vector typesList					= new Vector();

			StringTokenizer typesToken			= new StringTokenizer(types, "@");
			while(typesToken.hasMoreTokens())
			{
				String busType				= typesToken.nextToken();
				String correspondingType	=  _util.getCorrespondingType(_context, busType);

				typesList.add(busType);
				typesList.add(correspondingType);
			}
			sRelName = sRelName + "|" + direction;
			relNameTypeMap.put(sRelName, typesList);
		}

		return relNameTypeMap;
	}
}

