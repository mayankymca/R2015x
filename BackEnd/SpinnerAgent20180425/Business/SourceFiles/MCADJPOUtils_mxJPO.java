/*
 **  MCADJPOUtils
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  This program contains common utility functions
 */
import java.util.Hashtable;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.FileList;
import matrix.db.Format;
import matrix.db.FormatList;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class MCADJPOUtils_mxJPO extends IEFBaseJPO_mxJPO
{
	public  MCADJPOUtils_mxJPO  () {

	}
	public MCADJPOUtils_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("not supported no desktop client", null);

	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	/**
	 * Common Entry Point to JPO
	 * After initialization, the controll is given to the apropriate
	 * method depending on requested action
	 */
	public Hashtable execute(Context context,String[] args) throws MCADException
	{
		Hashtable res = null;
		try
		{
			Hashtable argsTable = (Hashtable)JPO.unpackArgs(args);
			res = execute(context, argsTable);
		}
		catch(Exception e)
		{
			System.out.println("Exception in MCADJPOUtils::execute: " + e.getMessage());
			MCADServerException.createException(e.getMessage(), e);
		}
		return res;
	}

	public Hashtable execute(Context context, Hashtable paramTable) throws MCADException
	{
		Hashtable resultDataTable = new Hashtable();
		resultDataTable.put(MCADServerSettings.JPO_EXECUTION_STATUS, "true");
		resultDataTable.put(MCADServerSettings.JPO_STATUS_MESSAGE, "");
		resultDataTable.put(MCADServerSettings.JPO_EXECUTION_RESULT, "");

		try
		{
			// Get all standard initialization,
			// including creation of GCO, logger, resource bundle etc.

			initialize(context,paramTable);
			String requestedMethod = (String)paramTable.get(MCADServerSettings.JPO_METHOD_NAME);

			if (requestedMethod.equals(MCADGlobalConfigObject.FEATURE_CHECKOUT))
			{
				getBoIdForCheckout(context, resultDataTable);
			}

			else if(requestedMethod.equals(MCADGlobalConfigObject.FEATURE_FINALIZE))
			{
				getBoIdToFinalize(context, resultDataTable);
			}
			else if(requestedMethod.equals(MCADGlobalConfigObject.FEATURE_EBOMSYNCHRONIZE))
			{
				getBoIdForEBOMSynch(context, resultDataTable);
			}
			else if(requestedMethod.equals(MCADGlobalConfigObject.FEATURE_BASELINE))
			{
				getBoIdForBaseline(context, resultDataTable);
			}
			else if(requestedMethod.equals(MCADGlobalConfigObject.DEFAULT_OPERATION))
			{
				getBoIdForDefaultOperations(context, resultDataTable);
			}
			else
			{
				MCADServerException.createException("CRITICAL ERROR, Unknown method," + requestedMethod, null);
			}
			resultDataTable.put(MCADServerSettings.JPO_EXECUTION_STATUS,"true");
		}
		catch(Exception e)
		{
			// Do not throw any exception back to JPO
			// For Any error/exception, send proper message back to the caller
			// using the resultDataTable
			String error = e.getMessage();
			System.out.println("[MCADJPOUtils.execute] :Exception occured" + error);
			resultDataTable.put(MCADServerSettings.JPO_EXECUTION_STATUS,"false");
			resultDataTable.put(MCADServerSettings.JPO_STATUS_MESSAGE,error);
		}
		return resultDataTable;
	}

	/**
	 * Description: This is a wrapper function around actual getBoIdForCheckout().
	 *
	 * @param   THE matrix context.
	 * @param   args[0] Global config object in serialized form.
	 * @param   args[1] business object id of the major component.
	 *
	 * @return  returns the business object id of the object to be checked out.
	 *
	 */
	public void getBoIdForCheckout(Context _context, Hashtable resultAndStatusTable) throws MCADException
	{
		//System.out.println("[MCADJPOUtil].getBoIdForCheckout");
		String sBusIdToCheckout = "";

		try
		{
			_busObject.open(_context);
			String sType = _busObject.getTypeName();
			String sName = _busObject.getName();
			String sRev = _busObject.getRevision();

			/* Rule 1: Object should be of MCAD relavant type. */
			if(_globalConfig.isMCADType(sType))
			{
				sBusIdToCheckout = getBoIdToCheckout(_context, _busObjectID);
			}

			_busObject.close(_context);

			if(sBusIdToCheckout.equals(""))
			{
				//failure message
				Hashtable msgTable = new Hashtable();
				msgTable.put("TYPE", sType);
				msgTable.put("NAME", sName);
				msgTable.put("REVISION", sRev);
				MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.NeitherFilesNorVersions", msgTable), null);
			}
			// [NDM : QWJ] START
			/*else if(sBusIdToCheckout.equals(_busObjectID))
			{
				resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT, sBusIdToCheckout);
			}*/
			else
			{
				/*//both ids are different. build the message.

				BusinessObject bo = new BusinessObject(sBusIdToCheckout);

				bo.open(_context);

				Hashtable msgTable = new Hashtable();
				msgTable.put("TYPE1", sType);
				msgTable.put("NAME1", sName);
				msgTable.put("REVISION1", sRev);
				msgTable.put("TYPE2", bo.getTypeName());
				msgTable.put("NAME2", bo.getName());
				msgTable.put("REVISION2", bo.getRevision());*/

				resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT, sBusIdToCheckout);

				//bo.close(_context);
				// [NDM : QWJ] END
			}
		}
		catch(Exception me)
		{
			MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.CannotCheckout")+me.getMessage(), me);
		}
	}

	/**
	 * Description: Decides whether the business object is newly created using "Generate New Structure".
							The logic is : if the value of attribute "Newly Created in Matrix" on CAD Model is "true",
							then it is created using "Generate New Structure".
	 *
	 * @param   Business object
	 *
	 * @return  boolean value which is the information about whether given business object is generated
	 *					by "Generate New Structure"
	 */
	boolean isBOCreatedByGenerateNewStructure(Context _context, BusinessObject bo) throws Exception
	{
		boolean result = false;
		bo.open(_context);
		String newlyCreatedInMatrixAttrActualName = _generalUtil.getActualNameForAEFData(_context,"attribute_NewlyCreatedinMatrix");
		String newlyCreatedInMatrix				  = _util.getAttributeForBO(_context, bo, newlyCreatedInMatrixAttrActualName);
		if(newlyCreatedInMatrix == null)
			newlyCreatedInMatrix = "";

		if(newlyCreatedInMatrix.equalsIgnoreCase("true") == true)
		{
			result = true;
		}
		return result;
	}


	// Get the id of the latest minor in the stream of input major
	public String getLatestMinorObjectId(Context _context, BusinessObject bo) throws MCADException
	{
		String latestMinorObjectId = "";
		try
		{
			BusinessObject minorObj = _util.getActiveMinor(_context, bo);
			if( minorObj == null)
			{
				latestMinorObjectId = getLatestBOIDWithFiles(_context, bo.getRevisions(_context));
			}
			else
			{
				latestMinorObjectId = minorObj.getObjectId();
			}
		}
		catch(Exception ex)
		{
			MCADServerException.createException(ex.getMessage(), ex);
		}
		return latestMinorObjectId;
	}

	/**
	 * Description: Returns the latest version of the Business object
	 *				which has files.
	 *
	 * @param   Business object list
	 *
	 * @return  ID of the versioned object. Blank string if none of
	 * 			the versions have files.
	 *
	 */
	private String getLatestBOIDWithFiles(Context _context, BusinessObjectList boList) throws Exception
	{
		String busID = "";

		if(boList.isEmpty())
			return busID;

		int index = boList.size()-1;
		while(index>-1)
		{
			BusinessObject thisBo = boList.getElement(index);
			if(hasFiles(_context, thisBo))
			{
				busID = thisBo.getObjectId();
				return busID;
			}
			else
			{
				BusinessObjectList minorsList = _util.getMinorObjects(_context, thisBo);
				int minorsListSize			  = minorsList.size();
				if( minorsListSize > 0)
				{
					int iCount = minorsList.size()-1;
					while(iCount > -1)
					{
						BusinessObject minorBO = minorsList.getElement(iCount);
						if (hasFiles(_context, minorBO))
						{
							busID = minorBO.getObjectId();
							return busID;
						}
						iCount--;
					}
				}
			}
			index--;
		}
		return busID;
	}

	/**
	 * Description: Looks for tthe existance of files in the business object.
	 *
	 * @param   Business object (in opened state) in question.
	 *
	 * @return  TRUE if object has files in MCAD relavent formats, FALSE otherwise.
	 *
	 */
	private boolean hasFiles(Context _context, BusinessObject bo)
	{
		boolean bHasFiles = false;
		try
		{
			bo.open(_context);
			//get all the formats.
			FormatList formats = bo.getFormats(_context);
			//Browse through all the formats
			for(int i=0; i<formats.size(); i++)
			{
				String thisFormatName = ((Format)formats.elementAt(i)).getName();
				if(_globalConfig.isMCADFormat(thisFormatName))
				{
					FileList files = bo.getFiles(_context, thisFormatName);
					if(files.size() > 0)
					{
						//Found files. No need to browse any more.
						bHasFiles = true;
						break;
					}
				}
			}
			bo.close(_context);
		}
		catch(Exception _ex)
		{
			//Do nothing
		}

		return bHasFiles;
	}

	public void getBoIdToFinalize(Context _context, Hashtable resultAndStatusTable) throws MCADException
	{
		//System.out.println("[MCADJPOUtil].getBoIdToFinalize");
		try
		{
			_busObject.open(_context);
			String sType = _busObject.getTypeName();

			/* Rule 1: Return the same ID back if the object is a Minor */
			if(!_util.isMajorObject(_context, _busObjectID))  // [NDM] is Major Object
			{
				BusinessObject majorBusObj	= null;
				String busIdToCheckout		= "";

				majorBusObj = _util.getMajorObject(_context, _busObject);

				if(majorBusObj != null)
				{
					majorBusObj.open(_context);

					//if(_generalUtil.isBusObjectFinalized(_context, majorBusObj))
					//{
						busIdToCheckout = majorBusObj.getObjectId(_context);
					//}

					majorBusObj.close(_context);
				}
				else
				{
					Hashtable messageDetails = new Hashtable(2);
					messageDetails.put("NAME", _busObject.getName());

					MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.CannotPerformOperationAsMajorAbsentOrNotAccessible", messageDetails), null);                    
				}

				if(busIdToCheckout.equals(""))
					busIdToCheckout = _busObjectID;

				//System.out.println("[MCADJPOUtil].getBoIdToFinalize, putting " + _busObjectID);
				resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT, busIdToCheckout);
			}
			else if (_util.isMajorObject(_context, _busObjectID))
			{
				String busIdToCheckout = null;
				busIdToCheckout = _busObjectID;
				resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT, busIdToCheckout);
			}
			/* Rule 2: Browse through the minors and send the ID of the latest */
			/* else if(_util.isMajorObject(_context, _busObjectID))  // [NDM] is Major Object
            {
				String busIdToCheckout = null;

				//if object is finalized, get its id
				// [NDM] Comment
				if(_generalUtil.isBusObjectFinalized(_context, _busObject))
				{
					busIdToCheckout = _busObjectID;
				}
				else
				{
					//Get the id of the latest minor.
					busIdToCheckout = getLatestMinorObjectId(_context, _busObject);

					if(busIdToCheckout.equals(""))
						busIdToCheckout = _busObjectID;
				}

				resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT, busIdToCheckout);
            }*/
			else
			{
				MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.ObjectNotReleventToIntegration"), null);
			}
		}
		catch(Exception me)
		{
			MCADServerException.createException(me.getMessage(), me);
		}
	}

	public void getBoIdForBaseline(Context _context, Hashtable resultAndStatusTable) throws MCADException
	{

		try
		{
			_busObject.open(_context);
			if(_util.isMajorObject(_context, _busObjectID))
			{
				BusinessObject activeMinorBusObject = _util.getActiveMinor(_context,_busObject);
				String activeMinorId				= activeMinorBusObject.getObjectId(_context);
				resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT,activeMinorId);
			}
			else
			{
					resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT,_busObjectID);
			}
		}
		catch(Exception me)
		{
			MCADServerException.createException(me.getMessage(), me);
		}
	}
	
	/**
	 * This method returns the id of the relevant object which has files
	 * If the input id is that of a versioned type or if the input object is finalized,
	 * the input id is returned back.Otherwise id of the latest version is returned.
	 * @param objId
	 * @return
	 * @throws Exception
	 */
	public void getBoIdForDefaultOperations(Context _context, Hashtable resultAndStatusTable) throws MCADException
	{
		//System.out.println("[MCADJPOUtil].getBoIdToSaveAs");
		try
		{
			_busObject.open(_context);
			//String sType = _busObject.getTypeName();  //[NDM] OP6

			/* Rule 1: Return the same ID back if the object is a Minor */
			if(!_util.isMajorObject(_context, _busObjectID))//_globalConfig.isMinorType(sType)) //[NDM] OP6
			{
				String activeMinorId = _util.getActiveVersionObjectFromMinor(_context, _busObjectID);
				//[NDM] H68
				//if(_generalUtil.isBusObjectFinalized(_context, _busObject))
				if(activeMinorId.equals(_busObjectID))
				{				  
					BusinessObject  finalBusObject = _util.getMajorObject(_context, _busObject);
					finalBusObject.open(_context);
					_busObjectID =  finalBusObject.getObjectId();
					finalBusObject.close(_context);

					resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT,_busObjectID);

				}else
				{
					//System.out.println("[MCADJPOUtil].getBoIdToSaveAs, putting " + _busObjectID);

					resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT,_busObjectID);
				}
			}
			/* Rule 2: Browse through the minors and send the ID of the latest */
			else if(_util.isMajorObject(_context, _busObject.getObjectId()))//_globalConfig.isMajorType(sType))// {NDM] OP6
			{
				//if(_generalUtil.isBusObjectFinalized(_context, _busObject))
				//{
					resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT,_busObjectID);
				//}
				//else
				//{
					//Get the id of the latest minor.
				//	String busIdToCheckout = getLatestMinorObjectId(_context, _busObject);
				//	resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT,busIdToCheckout);
				//}
			}
			else
			{
				MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.ObjectNotReleventToIntegration"), null);
			}
		}
		catch(Exception me)
		{
			MCADServerException.createException(me.getMessage(), me);
		}
	}

	// [NDM:QWJ} : Start
	/**
	 * Description: Returns the id of business object to be checked out given an id of
	 * any MCAD type object based on the following set of rules.
	 * Rules for NDM:
	 * 		1. If object is major object, it must have file, return its id.
	 *      2. If object is minor object and object has files (means older version), return its id.
	 *		3. If object is minor object and object has NO files (means latest minor), get major object and returns its id.
	 * @throws Exception 
	 */

	public String getBoIdToCheckout(Context _context, String sBusId)
	{
		String busIdToCheckout = "";

		try
		{
			String []oids = new String[] {sBusId};

			String attrIsVersionedObj 		= MCADMxUtil.getActualNameForAEFData(_context, "attribute_IsVersionObject");
			String sLatestVersionRelName  	= MCADMxUtil.getActualNameForAEFData(_context, "relationship_LatestVersion");
			String sVersionOfRelName  		= MCADMxUtil.getActualNameForAEFData(_context, "relationship_VersionOf");

			String SELECT_HAS_FILE     	= "format.hasfile";

			StringBuffer ATTR_IS_VERSIONED_OBJ	= new StringBuffer("attribute[");
			ATTR_IS_VERSIONED_OBJ.append(attrIsVersionedObj);
			ATTR_IS_VERSIONED_OBJ.append("]");

			StringBuffer SELECT_ON_LATEST_VERSION_ID = new StringBuffer("to[");
			SELECT_ON_LATEST_VERSION_ID.append(sLatestVersionRelName);
			SELECT_ON_LATEST_VERSION_ID.append("].from.id");

			StringBuffer SELECT_ON_VERSION_OF_ID = new StringBuffer("from[");
			SELECT_ON_VERSION_OF_ID.append(sVersionOfRelName);
			SELECT_ON_VERSION_OF_ID.append("].to.id");

			StringList selectlist = new StringList(4);
			selectlist.add(ATTR_IS_VERSIONED_OBJ.toString());
			selectlist.add(SELECT_ON_LATEST_VERSION_ID.toString());
			selectlist.add(SELECT_HAS_FILE);
			selectlist.add(SELECT_ON_VERSION_OF_ID.toString());

			BusinessObjectWithSelect busWithSelect = BusinessObject.getSelectBusinessObjectData(_context, oids, selectlist).getElement(0);

			boolean isMinorObject 		= new Boolean((String) busWithSelect.getSelectData(ATTR_IS_VERSIONED_OBJ.toString())).booleanValue();
			boolean isObjectHasFile 	= new Boolean((String) busWithSelect.getSelectDataList(SELECT_HAS_FILE).elementAt(0)).booleanValue();

			if(!isMinorObject)      // && isObjectHasFile)       //[NDM] : L86  in case of Instance, Instance may have no file
			{
				busIdToCheckout = sBusId;
			}
			else
			{
				if(_generalUtil.isObjectBulkLoaded(_context, sBusId))
				{
					busIdToCheckout = busWithSelect.getSelectData(SELECT_ON_LATEST_VERSION_ID.toString());
				}
				else
				{
				if(isObjectHasFile)
					busIdToCheckout = sBusId;
				else
				{
					busIdToCheckout = busWithSelect.getSelectData(SELECT_ON_VERSION_OF_ID.toString());
				}
			}		 
		}
		}
		catch(Exception _ex)
		{
			_ex.printStackTrace();
			busIdToCheckout = "false|" + _ex.getMessage();
		}

		return busIdToCheckout;
	}
	/**
	 * Description: Returns the id of business object to be checked out given an id of
	 * any MCAD type object based on the following set of rules.
	 * Rules:
	 *      1.If object is major type,is finalized and has files, return its id.If there are NO
	 *         files, return blank string
	 *      2.If object is major type, is NOT finalized and if the latest minor has files
	 *         return the lates minor object's id.If the latest minor object has NO files,
	 *         return blank string
	 *      3.If object is minor type and has files return its id.If it has NO files return blank string
	 */
	/*public String getBoIdToCheckout(Context _context, String sBusId)
    {
        String busIdToCheckout = "";

		try
        {
            MCADMxUtil mxUtil = new MCADMxUtil(_context, new MCADServerResourceBundle(""), new IEFGlobalCache());//[NDM]:for getting mcadmxutil
            BusinessObject bo = new BusinessObject(sBusId);
			bo.open(_context);
			String boType = bo.getTypeName();

			//check if the object is major type
			if(mxUtil.isMajorObject(_context, sBusId)) // [NDM] is Major Object
			{
				//if object is finalized, get its id				
				//if(_generalUtil.isBusObjectFinalized(_context, bo))
				//{
				//	busIdToCheckout = sBusId;
				//}
				//else
				//{
					//object is not finalized.Get the id of the latest minor.
					//busIdToCheckout = getLatestMinorObjectId(_context, bo);
				busIdToCheckout = sBusId;  // [NDM:QWJ] 
				//}
			}
			else
			{
				//object is a minor object.Get its id 
                //check if the object is with BulkLoading and return Major ID
                if(_generalUtil.isObjectBulkLoaded(_context, sBusId))
                {
                    busIdToCheckout = _util.getMajorIDByLatestVersion(_context, sBusId);
				}
			    else if(_generalUtil.isBusObjectFinalized(_context, bo))
				{
					//if object is finalized, get the id of the major object
					bo = _util.getMajorObject(_context, bo);
					if(bo!=null)
					{
						bo.open(_context);
						busIdToCheckout = bo.getObjectId();
						bo.close(_context);
					}
					else
					{
						System.out.println("No Business Object Found");
					}
				}
				else
				{
					//object is not finalized, get its id
					busIdToCheckout = sBusId;
				}
			}

			bo.close(_context);

			if(!busIdToCheckout.trim().equals(""))
			{
				BusinessObject boToCheckout = new BusinessObject(busIdToCheckout);
			}
        }
        catch(Exception _ex)
        {
			_ex.printStackTrace();
            busIdToCheckout = "false|" + _ex.getMessage();
        }

        return busIdToCheckout;
	}*/
	// [NDM : QWJ} : END

	public void getBoIdForEBOMSynch(Context _context, Hashtable resultAndStatusTable) throws MCADException
	{
		try
		{
			_busObject.open(_context);
			//String sType = _busObject.getTypeName();  //[NDM] OP6

			if(!_util.isMajorObject(_context, _busObjectID))//_globalConfig.isMinorType(sType))  //[NDM] OP6
			{
				BusinessObject majorBusObj	= null;
				String busIdForEBOMSynch	= "";
//[NDM] H68
				//if(_generalUtil.isBusObjectFinalized(_context, _busObject))
				//{
					majorBusObj = _util.getMajorObject(_context, _busObject);
					if(majorBusObj != null)
					{						
						busIdForEBOMSynch = majorBusObj.getObjectId(_context);						
					}
					else
					{
						Hashtable messageDetails = new Hashtable(2);
						messageDetails.put("NAME", _busObject.getName());
						MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.CannotPerformOperationAsMajorAbsentOrNotAccessible", messageDetails), null);    
					}
				//}				
				if(busIdForEBOMSynch.equals(""))
				{
					busIdForEBOMSynch = _busObjectID;
				}
				resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT, busIdForEBOMSynch);
			}
			else if(_util.isMajorObject(_context, _busObjectID))//_globalConfig.isMajorType(sType)) // {NDM] OP6
			{
				String busIdForEBOMSynch = null;

				if(_generalUtil.isBusObjectFinalized(_context, _busObject))
				{
					busIdForEBOMSynch = _busObjectID;
				}
				else
				{
					busIdForEBOMSynch = getLatestMinorObjectId(_context, _busObject);

					if(busIdForEBOMSynch.equals(""))
					{
						busIdForEBOMSynch = _busObjectID;
					}
				}

				resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT, busIdForEBOMSynch);
			}
			else
			{
				MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.ObjectNotReleventToIntegration"), null);
			}
		}
		catch(Exception me)
		{
			MCADServerException.createException(me.getMessage(), me);
		}
	}
}

