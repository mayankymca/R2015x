/*
 **  MCADRenameBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to rename the MCAD objects.
 */


import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.ArrayList;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.FileItr;
import matrix.db.FileList;
import matrix.db.Relationship;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADIntegrationSessionData;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADNameValidationUtil;
import com.matrixone.MCADIntegration.utils.MCADUtil;

public class MCADRenameBase_mxJPO extends IEFCommonUIActions_mxJPO
{
	protected String _errStr			= "";
	protected String _integName			="";
	java.util.Vector renamedObjNames	= null;

	private Vector alreadyRenamedObjList = null;

	protected boolean isSystemCaseSensitive = true;
	protected MCADIntegrationSessionData integSessionData 	= null;

	public  MCADRenameBase_mxJPO  () {

	}
	public MCADRenameBase_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("not supported no desktop client", null);

	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	// Business Logic for implementing
	protected void canPerformOperationCustom(Context context, Hashtable resultDataTable) throws MCADException
	{
		System.out.println("[MCADRename.canPerformOperationCustom] : ");
		/*try
        {
            //
        }
        catch(Exception e)
        {
            throw new MCADException("Rename Operation Cannot be performed because of an error condition :" + e.getMessage());
        }*/
	}

	// Entry point
	public void executeCustom(Context _context, Hashtable resultAndStatusTable)  throws MCADException
	{
		String attrFileSouceName = MCADMxUtil.getActualNameForAEFData(_context,"attribute_IEF-FileSource");
		boolean isObjectAndFilenameDifferent	= _globalConfig.isObjectAndFileNameDifferent();

		String newName			= (String)_argumentsTable.get(MCADServerSettings.NEW_NAME);
		String instanceName		= (String)_argumentsTable.get(MCADServerSettings.INSTANCE_NAME);
		String priority			= (String)_argumentsTable.get(MCADServerSettings.MESSAGE_PRIORITY);
		isSystemCaseSensitive	= (Boolean.valueOf((String)_argumentsTable.get(MCADServerSettings.CASE_SENSITIVE_FLAG))).booleanValue();
		String paramsToReturn	= "";
		alreadyRenamedObjList = new Vector();
		

		try
		{
			_integName = (String)_argumentsTable.get(MCADAppletServletProtocol.INTEGRATION_NAME);

			_busObject.open(_context);

			String busType				= _busObject.getTypeName();

			String oldName              = (String)_argumentsTable.get(MCADServerSettings.OLD_NAME);

			if(oldName == null || oldName.equals(""))
				oldName = _busObject.getName();

			String cadType				= _util.getCADTypeForBO(_context, _busObject);

			_busObject.close(_context);

			resultAndStatusTable.put(MCADServerSettings.NEW_NAME, newName);

			StringBuffer renamedObjIds = new StringBuffer();
			// Renaming a ProE Object
			if (_integName.equalsIgnoreCase("MxPro"))
			{
				if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
				{
					String oldProeInstanceName  = _generalUtil.getIndivisualInstanceName(oldName);
					BusinessObject familyObject = null;

					if (_util.isMajorObject(_context, _busObjectID))//_globalConfig.isMajorType(busType)) [NDM]
					{
						BusinessObjectList minorObjects = _util.getMinorObjects(_context, _busObject);
						BusinessObject latestMinor = (BusinessObject)minorObjects.lastElement();

						latestMinor.open(_context);

						String latestMinorID = latestMinor.getObjectId(_context);

						latestMinor.close(_context);

						latestMinorID = _util.getLatestRevisionID(_context, latestMinorID);
						latestMinor   = new BusinessObject(latestMinorID);

						familyObject = _generalUtil.getFamilyObjectForInstance(_context, latestMinor);
					}
					else
					{
						familyObject = _generalUtil.getFamilyObjectForInstance(_context, _busObject);
					}

					familyObject.open(_context);
					String familyName = familyObject.getName();

					if (oldProeInstanceName.equalsIgnoreCase(familyName))
					{
						_busObject = familyObject;
						oldName	= familyName;
						cadType	= _util.getCADTypeForBO(_context, familyObject);
					}

					familyObject.close(_context);
				}
			}

			boolean instanceRename = false;

			if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
				instanceRename = true;

			// to get all list of all objects to process
			BusinessObjectList objectsList	= new BusinessObjectList();
			Vector objIDList				= new Vector();

			getAllObjectsAcrossStreamsToRename(_context, _busObject, objectsList, objIDList, instanceRename);

			String attrTitle  = MCADMxUtil.getActualNameForAEFData(_context, "attribute_Title");	
			String oldTitle = _busObject.getAttributeValues(_context,attrTitle).getValue();
			String newTitle = "";

			if(instanceRename)
			{
				if(isObjectAndFilenameDifferent)
				{
					String familyNameFromTitle = _generalUtil.getFamilyNameFromObjectName(oldTitle,true);
					newTitle = MCADUtil.getDisplayNameForInstance(familyNameFromTitle,  newName);
				}
				else
				{
				String familyNameFromTitle = _generalUtil.getFamilyNameFromObjectName(oldTitle);

				newTitle = MCADUtil.getNameForInstance(_globalConfig, familyNameFromTitle,  newName);
			}		
			}		

			//Assumption : For instanceRename, 'objectsList' will have only instance revisions stream.
			BusinessObjectItr objItr		= new BusinessObjectItr(objectsList);

			while(objItr.next())
			{
				BusinessObject busObject		= null;

				busObject = objItr.obj();

				if(!busObject.isOpen())
					busObject.open(_context);

				boolean isRenamed	     = false;
				String currentBusOldName = busObject.getName();
				String currentCadType    = _util.getCADTypeForBO(_context, busObject);
				String busID		     = busObject.getObjectId();
				busObject.close(_context);

				BusinessObject renamedObj = busObject;

				if(instanceRename)
				{
					Vector fileNames = new Vector();

					try
					{
						String cadType1   = _util.getCADTypeForBO(_context, busObject);
						String formatName = _generalUtil.getFormatsForType(_context, busObject.getTypeName(), cadType1);
						FileList fileList = busObject.getFiles(_context, formatName);
						FileItr itr = new FileItr(fileList);
						while(itr.next())
						{
							String fName = itr.obj().getName();
							fileNames.addElement(fName);
						}

						if(fileNames.size() == 0)
						{
							fileNames.addElement("");
						}

						if (!fileNames.isEmpty())
						{
							paramsToReturn = fileNames.toString();
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}

					String actualNewName	= getNewNameForInstanceRename(newName, currentBusOldName);
					String actualOldBusName	= _generalUtil.getIndivisualInstanceName(currentBusOldName);
					
					String newNameForFileOperation = actualNewName;

					checkForTitleUniqueness(_context,newTitle,cadType,busObject,newName);

					if(isObjectAndFilenameDifferent)
					{
						String instanceNameFromTitle = MCADUtil.getIndivisualInstanceName(oldTitle,true);
						
						String indivisualInstanceName = _generalUtil.getIndivisualInstanceName(currentBusOldName);
						
						actualNewName				 = indivisualInstanceName.replace(instanceNameFromTitle, newName);
						
						actualNewName				 = getNewNameForInstanceRename(actualNewName, currentBusOldName);
						
						actualOldBusName			 = instanceNameFromTitle;
						
						String familyTitle			 = _generalUtil.getFamilyNameFromObjectName(oldTitle,true);

						newNameForFileOperation 	 = MCADUtil.getDisplayNameForInstance(familyTitle, newName);
					}

					if(!newName.equals(actualOldBusName))
					{
						isRenamed	= true;
						//get name changed object
						if(!actualNewName.equals(currentBusOldName))
						{
							//do Name Validation
							validateBusObjectName(actualNewName, currentCadType);
						if(!alreadyRenamedObjList.contains(busObject.getObjectId()))
						{
						renamedObj = _util.renameObject(_context, busObject,actualNewName);
							alreadyRenamedObjList.add(busObject.getObjectId());
						}
						
							
							
						}
						
						//rename dependent docs if any
						renameDependentDocs(_context, renamedObj, currentBusOldName, actualNewName, oldName, newNameForFileOperation, instanceName.trim(), renamedObjIds);

						//set Parent Instance attribute on Instance Of relationship for nested instance if parent instance is renamed
						String renamedObjID		 = renamedObj.getObjectId(_context);
						String instanceOf		 = MCADMxUtil.getActualNameForAEFData(_context, "relationship_InstanceOf");
						String parentInstance	 = MCADMxUtil.getActualNameForAEFData(_context, "attribute_ParentInstance");
						String [] oids			 = new String[1];
						oids[0]					 = renamedObjID;
						StringList busSelectList = new StringList(5);

						String selectOnInst		 = "to[" + instanceOf + "].from.from[" + instanceOf + "].";

						busSelectList.add(selectOnInst + "id");
						busSelectList.add(selectOnInst + "attribute[" + parentInstance + "]");

						BusinessObjectWithSelectList busWithSelectList	= BusinessObjectWithSelect.getSelectBusinessObjectData(_context, oids, busSelectList);
						BusinessObjectWithSelect busWithSelect			= busWithSelectList.getElement(0);
						StringList relIdList							= (StringList)busWithSelect.getSelectDataList(selectOnInst + "id");
						StringList attrList								= (StringList)busWithSelect.getSelectDataList(selectOnInst + "attribute[" + parentInstance + "]");

						if(attrList!=null)
						{
							for(int b=0 ; b < attrList.size(); b++)
							{
								String attrParentInst = (String)attrList.elementAt(b);
								if(attrParentInst.equals(actualOldBusName))
								{
									String relId				= (String)relIdList.elementAt(b);
									Relationship nestedInstRel	= new Relationship(relId);
									String indivInstName		= _generalUtil.getIndivisualInstanceName(actualNewName);
									
									if(isObjectAndFilenameDifferent)
										indivInstName = newName;
									
									_util.setRelationshipAttributeValue(_context, nestedInstRel, parentInstance, indivInstName);
								}
							}
						}
					}

					//get actual server side operations for rename done
					// Set file source attribute for Rename operation
					_util.setAttributeValue(_context, renamedObj, attrFileSouceName, MCADAppletServletProtocol.FILESOURCE_RENAME);
					
					boolean renamedFromRqdOnFamilyLike	= true;
					
					_generalUtil.doActualRename(_context, renamedObj, actualNewName, currentBusOldName, newNameForFileOperation, actualOldBusName, renamedFromRqdOnFamilyLike,true);
				}
				else
				{
					String actualNewName = "";

					if(isObjectAndFilenameDifferent && oldTitle.contains(".") && newName.contains(".") && oldTitle.substring(0,oldTitle.lastIndexOf(".")).equals(currentBusOldName))
						actualNewName = newName.substring(0,newName.lastIndexOf("."));
					else
						actualNewName = _util.replace(currentBusOldName, oldName, newName);

					String actualOldBusName 			= currentBusOldName;

					boolean renamedFromRqdOnFamilyLike	= true;

					if(_globalConfig.isTypeOfClass(currentCadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
					{
						String oldFamilyNameFromInstanceName = _generalUtil.getFamilyNameFromObjectName(currentBusOldName);

						String newFamilyBusName = "";
						if(isObjectAndFilenameDifferent &&  oldTitle.contains(".") && newName.contains(".") && oldTitle.substring(0,oldTitle.lastIndexOf(".")).equals(oldFamilyNameFromInstanceName))
							newFamilyBusName = newName.substring(0,newName.lastIndexOf("."));
						else
							newFamilyBusName = _util.replace(oldFamilyNameFromInstanceName, oldName, newName);

						boolean isProeGenericInstance = false;
						renamedFromRqdOnFamilyLike	= false;
						
						if (_integName.equalsIgnoreCase("MxPro"))
						{
							String proeInstanceName = _generalUtil.getIndivisualInstanceName(currentBusOldName);
							if (proeInstanceName.equalsIgnoreCase(oldName))
							{
								isProeGenericInstance = true;
								renamedFromRqdOnFamilyLike = true;
							}
						}

						actualNewName				= getNewNameForFamilyRename(newFamilyBusName, currentBusOldName, isProeGenericInstance);


						isRenamed	= false;
						//get name changed object
						if(!actualNewName.equals(currentBusOldName))
						{
							//do Name Validation
							validateBusObjectName(actualNewName, currentCadType);
							
							if(!alreadyRenamedObjList.contains(busObject.getObjectId()))
						{
							renamedObj = _util.renameObject(_context, busObject, actualNewName);
							alreadyRenamedObjList.add(busObject.getObjectId());
						}
							
						
						
	
						}


						//rename dependent docs if any
						renameDependentDocs(_context, renamedObj, currentBusOldName, actualNewName, oldName, newName, instanceName.trim(), renamedObjIds);
					}
					else if(!oldName.equals(newName))
					{
						isRenamed	= true;

						//get name changed object
						if(!actualNewName.equals(currentBusOldName))
						{
							//do Name Validation
							validateBusObjectName(actualNewName, currentCadType);
							
							if(!alreadyRenamedObjList.contains(busObject.getObjectId()))
						{
							renamedObj = _util.renameObject(_context, busObject, actualNewName);
							alreadyRenamedObjList.add(busObject.getObjectId());
						}
							
						
								
						}
						
						
						//rename dependent docs if any
						renameDependentDocs(_context, renamedObj, currentBusOldName, actualNewName, oldName, newName, instanceName.trim(), renamedObjIds);

						_util.setAttributeValue(_context, renamedObj, attrFileSouceName, MCADAppletServletProtocol.FILESOURCE_RENAME);
					}
					else
					{
						_util.setAttributeValue(_context, renamedObj, attrFileSouceName, MCADAppletServletProtocol.FILESOURCE_RENAME);
					}

					//get actual server side operations for rename done
					// Set file source attribute for Rename operation
					_generalUtil.doActualRename(_context, renamedObj, actualNewName, actualOldBusName, newName, oldName, renamedFromRqdOnFamilyLike, true);
				}

				if(isRenamed)
				{
					renamedObjIds.append(busID);
					renamedObjIds.append(MCADAppletServletProtocol.IEF_SEPERATOR_ONE);
					renamedObjIds.append(oldName);
					renamedObjIds.append(MCADAppletServletProtocol.IEF_SEPERATOR_TWO);
				}
			}

			resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT, paramsToReturn + MCADAppletServletProtocol.IEF_SEPERATOR_TWO + renamedObjIds);
			resultAndStatusTable.put(MCADServerSettings.OBJECT_ID_LIST, objIDList);

			if(_globalConfig.isBatchProcessorForRenameEnabled())
			{
				resultAndStatusTable.put(MCADServerSettings.OBJECT_ID, _busObject.getObjectId(_context));
				resultAndStatusTable.put(MCADServerSettings.MESSAGE_PRIORITY, priority);
				resultAndStatusTable.put(MCADServerSettings.SELECTED_OBJECTID_LIST, objectsList);
			}
		}
		catch(Exception e)
		{
			String error = e.getMessage();
			System.out.println("[MCADRename.executeCustom] Exception occured- " + error);
			MCADServerException.createException(error, e);
		}
	}

	private void validateBusObjectName(String newName, String cadType) throws MCADServerException 
	{
		if(_globalConfig.getNonSupportedCharacters() == null || _globalConfig.getNonSupportedCharacters().equals(""))
		{
			boolean isValidFileName = MCADNameValidationUtil.isValidNameForCADType( newName,  cadType,  _globalConfig);
			if(!isValidFileName)
			{
				MCADServerException.createManagedException("IEF0292300325",_serverResourceBundle.getString("mcadIntegration.Server.Message.IEF0292300325"),null);
			}
		}
	}

	protected String getNewNameForInstanceRename(String newName, String oldInstanceName)
	{
		String familyName		= _generalUtil.getFamilyNameFromObjectName(oldInstanceName);
		String newInstanceName	= _generalUtil.getNameForInstance(familyName, newName);

		return newInstanceName;
	}

	protected String getNewNameForFamilyRename(String newFamilyBusName, String oldInstanceBusName, boolean isProeGenericInstance)
	{
		String individualInstName =_generalUtil.getIndivisualInstanceName(oldInstanceBusName);

		String newNameForInstance = _generalUtil.getNameForInstance(newFamilyBusName, individualInstName);

		if (isProeGenericInstance)
		{
			newNameForInstance = _generalUtil.getNameForInstance(newFamilyBusName, newFamilyBusName);
		}

		return newNameForInstance;
	}

	protected void getAllObjectsAcrossStreamsToRename(Context _context, BusinessObject busObj, BusinessObjectList ObjectsList, Vector objIDList, boolean instanceRename)
	{
		String busType					= busObj.getTypeName();
		boolean isMinorType				= !_util.isMajorObject(_context, busObj.getObjectId());//_globalConfig.isMajorType(busType); [NDM]

		try
		{
			BusinessObjectList newObjList	= _util.getRevisionBOsOfAllStreams(_context, busObj, isMinorType);
			BusinessObjectItr objItr		= new BusinessObjectItr(newObjList);
			while(objItr.next())
			{
				BusinessObject BusObject	= objItr.obj();
				BusObject.open(_context);
				String busid = BusObject.getObjectId();
				String CadType = _util.getCADTypeForBO(_context, BusObject);
				BusObject.close(_context);
				if (!instanceRename && (_globalConfig.isTypeOfClass(CadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE) || _globalConfig.isTypeOfClass(CadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE)))
				{
					Vector preInstanceList			= _generalUtil.getInstanceListForFamilyObject(_context, BusObject.getObjectId(_context));
					Enumeration keys				= preInstanceList.elements();
					while(keys.hasMoreElements())
					{
						BusinessObject instObj	= (BusinessObject)keys.nextElement();
						instObj.open(_context);
						String busObjType		= instObj.getTypeName();
						instObj.close(_context);

						boolean bIsMinorType			= !_util.isMajorObject(_context, busObj.getObjectId());//_globalConfig.isMajorType(busObjType); [NDM]
						BusinessObjectList intObjList	= _util.getRevisionBOsOfAllStreams(_context, instObj, bIsMinorType);

						BusinessObjectItr instObjItr	= new BusinessObjectItr(intObjList);
						while(instObjItr.next())
						{
							BusinessObject instObjFinal = instObjItr.obj();
							String instObjFinalID		= instObjFinal.getObjectId(_context);

							if(!objIDList.contains(instObjFinalID))
							{
								objIDList.addElement(instObjFinalID);
								getAllObjectsAcrossStreamsToRename(_context, instObjFinal, ObjectsList, objIDList, false);
								ObjectsList.addElement(instObjFinal);
							}
						}
					}
				}

				if(!objIDList.contains(busid))
				{
					objIDList.addElement(busid);
					ObjectsList.addElement(BusObject);
				}
			}
		}
		catch(Exception e)
		{

		}
	}

	/**
	 * This method renames dependent docs.
	 *  If oldInstanceName (arg 4) has length>0 it means that only instance is to be renamed.
	 * else, family is renamed, leading to renaming of family dependent doc as well
	 * as instance dependent doc.
	 */
	protected void renameDependentDocs(Context _context, BusinessObject bo, String oldBusName, String newBusName, String oldNameForFileOperation, String newNameForFileOperation, String oldInstanceName, StringBuffer renamedObjIds) throws Exception
	{
		try
		{
			String attribName			= MCADMxUtil.getActualNameForAEFData(_context,"attribute_CADObjectName");
			Hashtable relationsList		= _generalUtil.getAllWheareUsedRelationships(_context, bo, true, MCADServerSettings.DERIVEDOUTPUT_LIKE);
			Enumeration allRels			= relationsList.keys();
			String familyName			= "";
			boolean bRenameOnlyInstance = false;

			if(oldInstanceName.length() > 0)
			{
				bRenameOnlyInstance = true;
				bo.open(_context);
				familyName = bo.getName();
				bo.close(_context);
			}

			while(allRels.hasMoreElements())
			{
				Relationship rel	= (Relationship)allRels.nextElement();
				String end			= (String)relationsList.get(rel);

				BusinessObject ddBO = null;
				rel.open(_context);
				// The other object is at the other "end"
				if (end.equals("from"))
				{
					ddBO = rel.getTo();
				}
				else
				{
					ddBO = rel.getFrom();
				}

				ddBO.open(_context);
				String boName = ddBO.getName();
				if(boName.contains(newNameForFileOperation))
				{
					int indexOfDot = boName.indexOf(".");
					
					if(indexOfDot != -1)
					{
						
					if(newNameForFileOperation.equals(boName.substring(0,indexOfDot)))
					{
						continue;
					}
					}
					
				}
				String cadType	= _util.getCADTypeForBO(_context, ddBO);
				rel.close(_context);

				if(bRenameOnlyInstance)
				{
					//String tmpOldInstanceName = _generalUtil.getGeneratedInstanceName(familyName, oldInstanceName);
					String tmpOldInstanceName = familyName + "-" + oldInstanceName;
					if(!_integName.equalsIgnoreCase("MxPro") && MCADUtil.areStringsEqual(boName, tmpOldInstanceName, isSystemCaseSensitive))
					{
						//String changedName = _generalUtil.getGeneratedInstanceName(familyName, newName);
						String changedName = familyName + "-" + newBusName;

						_util.setRelationshipAttributeValue(_context, rel, attribName, changedName);

						validateBusObjectName(changedName, cadType);
					
					if(!alreadyRenamedObjList.contains(ddBO.getObjectId()))
						{
						_util.renameObject(_context, ddBO,changedName);
							alreadyRenamedObjList.add(ddBO.getObjectId());
						}

					}
					else if (_integName.equalsIgnoreCase("MxPro") && !MCADUtil.areStringsEqual(boName, tmpOldInstanceName, isSystemCaseSensitive))
					{
						_util.setRelationshipAttributeValue(_context, rel, attribName, newBusName);

						if(!alreadyRenamedObjList.contains(ddBO.getObjectId()))
						{
						_util.renameObject(_context, ddBO, newBusName);
							alreadyRenamedObjList.add(ddBO.getObjectId());
						}
					}
				}
				else
				{
					String changedName = _util.replace(boName, oldNameForFileOperation, newNameForFileOperation);

					if(changedName.equals(boName))
						changedName = MCADMxUtil.getUniqueObjectName(_integName);

					if(MCADUtil.areStringsEqual(boName, oldBusName, isSystemCaseSensitive))
						_util.setRelationshipAttributeValue(_context, rel, attribName, changedName);
					else if(boName.startsWith(oldBusName+"."))
						_util.setRelationshipAttributeValue(_context, rel, attribName, newBusName);

					validateBusObjectName(changedName, cadType);
				if(!alreadyRenamedObjList.contains(ddBO.getObjectId()))
						{
					_util.renameObject(_context, ddBO,changedName);
						alreadyRenamedObjList.add(ddBO.getObjectId());
						}
					
					
						
				}

				ddBO.close(_context);
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
	}

	protected void checkForTitleUniqueness(Context _context,String newTitle,String cadType,BusinessObject busObject,String newName) throws Exception
	{
		ArrayList alreadyProcessedFamily = new ArrayList();
		String attrTitle  = MCADMxUtil.getActualNameForAEFData(_context, "attribute_Title");

		if(_globalConfig.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_INSTANCE_LIKE))
		{
			String objectIds [] = new String [1];
			objectIds[0]		= busObject.getObjectId();

			String relInstanceOf	  =  MCADMxUtil.getActualNameForAEFData(_context,"relationship_InstanceOf");
			String familyIds		  = "to["+relInstanceOf+"].from.id";

			StringList busSelectList1 = new StringList();
			busSelectList1.add(familyIds);

			BusinessObjectWithSelectList busWithSelectList1	= BusinessObjectWithSelect.getSelectBusinessObjectData(_context, objectIds, busSelectList1);

			BusinessObjectWithSelect busWithSelect1 = busWithSelectList1.getElement(0);

			StringList familyID = busWithSelect1.getSelectDataList(familyIds);

			if(familyID != null && familyID.size() > 0)
			{
				for(int j=0;j<familyID.size();j++)
				{
					String familyId = (String)familyID.get(j);

					if(!alreadyProcessedFamily.contains(familyId))
					{
						alreadyProcessedFamily.add(familyId);

						String [] arrfamilyId = new String[1];
						arrfamilyId[0] = familyId;

						ArrayList instanceList = _generalUtil.getFamilyStructureRecursively(_context, arrfamilyId, new Hashtable(),null);
						String [] oids		   = new String[instanceList.size()];

						instanceList.toArray(oids);

						StringList busSelectList = new StringList();
						busSelectList.add("attribute[" + attrTitle + "]");

						BusinessObjectWithSelectList busWithSelectList	= BusinessObjectWithSelect.getSelectBusinessObjectData(_context, oids, busSelectList);

						for(int k=0;k<busWithSelectList.size();k++)
						{
							BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(k);

							String instancetitle = (String)busWithSelect.getSelectData("attribute[" + attrTitle + "]");

							if(instancetitle.equals(newTitle))
							{
								BusinessObject familyObject = new BusinessObject(familyId);
								familyObject.open(_context);
								Hashtable messageDetails = new Hashtable();
								messageDetails.put("NAME",newName);
								messageDetails.put("FAMILYNAME",familyObject.getName());
								familyObject.close(_context);
								MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.InstanceNameNotUniqueForFamily", messageDetails), null);
							}
						}
					}
				}
			}
		}
	}
}

