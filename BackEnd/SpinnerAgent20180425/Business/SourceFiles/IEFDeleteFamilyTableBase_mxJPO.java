/*
 **  IEFDeleteFamilyTableBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  Program to delete Family Table when Family Object is deleted
 */
import java.util.Enumeration;
import java.util.Hashtable;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class IEFDeleteFamilyTableBase_mxJPO
{
	protected MCADMxUtil _util								= null;
	protected MCADServerGeneralUtil serverGeneralUtil		= null;
	protected String _sObjectID								= null;
	protected MCADServerResourceBundle serverResourceBundle	= null;

	/**
	 * The no-argument constructor.
	 */
	public IEFDeleteFamilyTableBase_mxJPO()
	{
	}

	/**
	 * Constructor which accepts the Matrix context and an array of String
	 * arguments.
	 */
	public IEFDeleteFamilyTableBase_mxJPO(Context context, String[] args) throws Exception
	{
		_util				 = new MCADMxUtil(context, null, new IEFGlobalCache());		
		// Get the OBJECTID of the object in context
		_sObjectID 			 = args[0];
		serverResourceBundle = new MCADServerResourceBundle("en-US"); 
	}

	public int mxMain(Context _context, String []args)  throws Exception
	{
		try
		{
			String reln = "";
			String end	= "";

			String Args[] = new String[3];
			Args[0] = "global";
			Args[1] = "IsFamilyDeleted";
			Args[2] = "true";
			_util.executeMQL(_context, "set env $1 $2 $3", Args);
			
			BusinessObject busObj = new BusinessObject(_sObjectID);
			busObj.open(_context);
			String integrationName				= _util.getIntegrationName(_context, _sObjectID);

			String gcoRev     = MCADMxUtil.getConfigObjectRevision(_context);

			StringBuffer sbKey = new StringBuffer(100);
			sbKey.append(integrationName);
			sbKey.append("|");
			sbKey.append(gcoRev);
			String sKey = sbKey.toString();	
			

			//MCADGlobalConfigObject globalConfig = getGlobalConfigObject(_context, _sObjectID);
			MCADGlobalConfigObject globalConfig = (MCADGlobalConfigObject)_util.getUserSpecificFullGCODefinition(_context,sKey);

			serverGeneralUtil = new MCADServerGeneralUtil(_context, globalConfig, serverResourceBundle, new IEFGlobalCache());

			// get the relationship name from gco
			Hashtable relsAndEnds =  globalConfig.getRelationshipsOfClass("FamilyLike");
			Enumeration allRels = relsAndEnds.keys();
			while(allRels.hasMoreElements())
			{
				reln = (String)allRels.nextElement();
				end = (String)relsAndEnds.get(reln);
			}

			// to delete all instances related to the family object
			deleteAllRelatedInstanceObjects(_context, busObj, reln, end, _sObjectID, globalConfig);
		}
		catch(Exception e)
		{			
			MCADServerException.createException(e.getMessage(), e);
		}
		return 0;
	}

	protected void deleteAllRelatedInstanceObjects(Context _context, BusinessObject busObj, String relName, String end, String busObjectID, MCADGlobalConfigObject globalConfig) throws Exception
	{
		try
		{
			String childEnd = end;
			String parentEnd = "from";

			if(end.equalsIgnoreCase("from"))
				parentEnd = "to";

			String SELECT_INSTANCE_IDS    = new StringBuffer(parentEnd).append("[").append(relName).append("].").append(childEnd).append(".id").toString();
			String SELECT_FAMILY_IDS      =  new StringBuffer(childEnd).append("[").append(relName).append("].").append(parentEnd).append(".id").toString();
			String actualBaselineRelName  =  MCADMxUtil.getActualNameForAEFData(_context,"relationship_DesignBaseline");

			String SELECT_EXPRESSION_BASELINE_ID = "from[" + actualBaselineRelName + "].to.id";

			StringList familySelectList = new StringList();
			familySelectList.add(SELECT_INSTANCE_IDS);

			BusinessObjectWithSelect familySelectData = BusinessObject.getSelectBusinessObjectData(_context, new String[]{busObj.getObjectId(_context)}, familySelectList).getElement(0);

			StringList instanceList = familySelectData.getSelectDataList(SELECT_INSTANCE_IDS);

			if(instanceList != null && instanceList.size() > 0)
			{
				String [] instanceIds = new String[instanceList.size()];
				instanceList.toArray(instanceIds);

				StringList instanceSelectList = new StringList();

				instanceSelectList.add(SELECT_FAMILY_IDS);
				instanceSelectList.add(SELECT_EXPRESSION_BASELINE_ID);
				instanceSelectList.add("id");

				BusinessObjectWithSelectList instanceSelectDataList = BusinessObject.getSelectBusinessObjectData(_context, instanceIds, instanceSelectList);

				for(int i = 0 ; i < instanceSelectDataList.size();i++)
				{
					BusinessObjectWithSelect instanceSelectData = instanceSelectDataList.getElement(i);

					StringList connectedFamilyIds = instanceSelectData.getSelectDataList(SELECT_FAMILY_IDS);
					StringList baselineIds		  = instanceSelectData.getSelectDataList(SELECT_EXPRESSION_BASELINE_ID);

					if(baselineIds != null && !baselineIds.isEmpty())
					{	
						MCADServerException.createException(serverResourceBundle.getString("mcadIntegration.Server.Message.CantPerformOperationAsObjectConnectedToBaseline"),null);
					}

					if(connectedFamilyIds != null && connectedFamilyIds.size() == 1)
					{
						String instanceId = instanceSelectData.getSelectData("id");

						BusinessObject instanceObject = new BusinessObject(instanceId);

						String instanceObjectId = instanceObject.getObjectId(_context);

						if(!instanceObjectId.equals(busObjectID))
						{
							BusinessObject instanceMajorObject = _util.getMajorObject(_context, instanceObject);						

							if(instanceMajorObject == null)
							{
								instanceObject.remove(_context);
							}
							else
							{
								instanceMajorObject.open(_context);
								BusinessObjectList minorObjectList 	= _util.getMinorObjects(_context, instanceMajorObject);

								if(minorObjectList.size() == 1)
								{
									instanceMajorObject.close(_context);
									instanceMajorObject.remove(_context);
								}
								else
								{
									//Delete the derived output of the minor before deleting the minor
									String[] init = new String[1];
									init[0] =  instanceObjectId;
									String[] jpoArgs = new String[] {};
									JPO.invoke(_context ,"IEFDeleteDerivedOutput", init, "mxMain", jpoArgs);

									boolean resetActiveMinor	= false;
									boolean resetLatestMinor	= false;
									String newLatestMinorId		= "";								

									BusinessObject activeMinor	= _util.getActiveMinor(_context, instanceMajorObject);
									String latestMinorId		= _util.getLatestMinorID(_context, instanceMajorObject);

									String activeObjectId = activeMinor.getObjectId(_context);

									if(instanceObjectId.equals(activeObjectId))
										resetActiveMinor = true;

									if(instanceObjectId.equals(latestMinorId))
										resetLatestMinor = true;

									instanceObject.remove(_context);

									if(resetActiveMinor)
									{
										newLatestMinorId = getLatestMinorId(_context, instanceMajorObject);
										if(newLatestMinorId != null && !newLatestMinorId.equals(""))
										{
											BusinessObject latestMinor	= new BusinessObject(newLatestMinorId);
											latestMinor.open(_context);

											_util.resetActiveVersion(_context, latestMinor, globalConfig);

											latestMinor.close(_context);
										}
									}

									if(resetLatestMinor)
										_util.resetLatestVersion(_context, instanceMajorObject);
								}
							}

							if(instanceMajorObject != null && instanceMajorObject.isOpen())
								instanceMajorObject.close(_context);
						}
					}
				}
			}
		}
		catch(MatrixException me)
		{
			MCADServerException.createException(me.getMessage(), me);
		}
		catch(Exception me)
		{		
			MCADServerException.createException(me.getMessage(), me);
		}
	}

	protected String getLatestMinorId(Context context, BusinessObject majorObject) throws Exception
	{
		String latestMinorID = "";

		BusinessObjectList minorList = _util.getMinorObjects(context, majorObject);

		if(minorList != null && minorList.size() > 0)
		{
			BusinessObject latestMinor	= (BusinessObject)minorList.elementAt(minorList.size()-1);
			String busID				= latestMinor.getObjectId(context);
			latestMinorID				= _util.getLatestRevisionID(context, busID);
		}

		return latestMinorID;
	}



	protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String busId) throws Exception
	{
		// Get the IntegrationName
		IEFGuessIntegrationContext_mxJPO guessIntegration = new IEFGuessIntegrationContext_mxJPO(context, null);
		String jpoArgs[] = new String[1];
		jpoArgs[0] = busId;
		String integrationName = guessIntegration.getIntegrationName(context, jpoArgs);

		// Get the relevant GCO Name 
		IEFGetRegistrationDetails_mxJPO registrationDetailsReader = new IEFGetRegistrationDetails_mxJPO(context, null);
		String args[] = new String[1];
		args[0] = integrationName;
		String registrationDetails = registrationDetailsReader.getRegistrationDetails(context, args);
		String gcoName = registrationDetails.substring(registrationDetails.lastIndexOf("|")+1);

		String typeGolbalConfig	= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
		// Now Create the GCO Object
		MCADConfigObjectLoader configLoader = new MCADConfigObjectLoader(null);
		MCADGlobalConfigObject gco = configLoader.createGlobalConfigObject(context, _util, typeGolbalConfig, gcoName);		
		return gco;
	}
}

