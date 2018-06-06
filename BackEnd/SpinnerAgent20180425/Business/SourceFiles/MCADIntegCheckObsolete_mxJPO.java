/*
 **  MCADIntegCheckObsolete 
 **
 **  Copyright Dassault Systemes, 1992-2010.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 ** JPO for checking if an object is obsolete
 ** find active version in the current stream 
 */

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.IEFServerHashCodeUtil;
import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADStringUtils;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;

public class MCADIntegCheckObsolete_mxJPO 
{
	private String SELECT_ON_ACTIVE_MINOR					= "";
	private String SELECT_ATTR_IEFFILEMESSAGEDIGEST			= "";
	private String integrationName							= "";

	private MCADServerResourceBundle serverResourceBundle = null;
	protected IEFGlobalCache	cache						= null;
	protected IEFIntegAccessUtil mxUtil						= null;
	protected MCADMxUtil util              					= null;


	public MCADIntegCheckObsolete_mxJPO ()
	{
	}

	public MCADIntegCheckObsolete_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("Not supported on desktop client!!!", null);

		String language	= args[0];
		integrationName	= args[1];

		init(context, language);
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	private void init(Context context, String language)  throws Exception
	{
		SELECT_ON_ACTIVE_MINOR	= "from[" + MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion") + "].to.";

		SELECT_ATTR_IEFFILEMESSAGEDIGEST		= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-FileMessageDigest") + "]";

		serverResourceBundle = new MCADServerResourceBundle(language);
		cache				 = new IEFGlobalCache();
		mxUtil				 = new IEFIntegAccessUtil(context, serverResourceBundle, cache);

	}

	public Hashtable returnObsoleteInfoForObjectIds(Context context, String[] args) throws Exception
	{
		Hashtable returnTable				 = new Hashtable();
		try
		{
			Hashtable argumentsTable			 = (Hashtable) JPO.unpackArgs(args);

			String isCheckObsoleteAcrossRevision = (String) argumentsTable.get("isCheckObsoleteAcrossRevision");
			Hashtable inputObjectDetailsTable	 = (Hashtable) argumentsTable.get("inputTable"); // key -> id / value -> all other info (the same key will be used in return table)
			Collection inputInfoValue			 = inputObjectDetailsTable.values();
			Iterator inputInfoItr 				 = inputInfoValue.iterator();
			Set majorIds						 = new HashSet(inputInfoValue.size());

			while(inputInfoItr.hasNext())
			{
				Vector inputInfo 		= (Vector) inputInfoItr.next();
				String inputMajorBusID	= (String)inputInfo.elementAt(1);
				majorIds.add(inputMajorBusID);
			}

			Hashtable majorIdInfoTable = getInfoFromMajorids(context, majorIds, isCheckObsoleteAcrossRevision);

			Enumeration inputIds = inputObjectDetailsTable.keys();
			while(inputIds.hasMoreElements())
			{
				String inputid				 = (String) inputIds.nextElement();

				updateObjectObsoleteDetails(context, inputid, inputObjectDetailsTable, majorIdInfoTable, returnTable);
			}
		}
		catch(Exception ex)
		{
			MCADServerException.createManagedException("IEF0033200102", serverResourceBundle.getString("mcadIntegration.Server.Message.IEF0033200102"), ex);
		}

		return returnTable;
	}

	private void updateObjectObsoleteDetails(Context context, String inputid, Hashtable inputObjectDetailsTable, Hashtable majorIdInfoTable, Hashtable returnTable) throws Exception
	{
		if(!returnTable.containsKey(inputid))
		{
			Vector obsoleteObjectDetails			 = new Vector(4);

			Vector inputInfoForId					 = (Vector) inputObjectDetailsTable.get(inputid);

			String minorBusRevision					 = (String) inputInfoForId.elementAt(0);
			String inputMajorBusID					 = (String) inputInfoForId.elementAt(1);
			String matrixHashcode					 = (String) inputInfoForId.elementAt(2);
			String workingSetHashcode				 = (String) inputInfoForId.elementAt(3);
			String finalizationStatusForAllRevisions = (String) inputInfoForId.elementAt(4);
			String fileList							 = (String) inputInfoForId.elementAt(5);

			// In Virtual finalization case , the stamping on the file will be a older version while one on the working set will be the finalized major
			//E.g A.1 is Virtual finalized to create A.3. WS will have entry as A and file will have entry as A.1. This is not obsolete.
			boolean isRevisionInWorkingSetMatched	 = true; 

			String familyObseleteStatus				 = "true";

			if(inputInfoForId.size() > 6)
			{
				String familyid = (String) inputInfoForId.elementAt(6);

				if(!familyid.equals(""))
				{
					updateObjectObsoleteDetails(context, familyid, inputObjectDetailsTable, majorIdInfoTable, returnTable);

					Vector familyObsoleteInfo = (Vector) returnTable.get(familyid);

					familyObseleteStatus	  = (String) familyObsoleteInfo.elementAt(0);
				}

				if(inputInfoForId.size() > 7)
				{
					String sIsRevisionInWorkingSetMatched = (String) inputInfoForId.elementAt(7);

					if(sIsRevisionInWorkingSetMatched != null && sIsRevisionInWorkingSetMatched.equals(MCADAppletServletProtocol.FALSE))
						isRevisionInWorkingSetMatched = false;
				}
			}

			Vector majorIdInfo						 = (Vector) majorIdInfoTable.get(inputMajorBusID);

			String mxMajorRevision					 = (String) majorIdInfo.elementAt(0);
			String majorBusID						 = (String) majorIdInfo.elementAt(1);
			String activeBusID						 = (String) majorIdInfo.elementAt(2);
			String mxMinorRevision					 = (String) majorIdInfo.elementAt(3);
			String mxMinorFileDigestValue			 = (String) majorIdInfo.elementAt(4);

			String mxMinorHascode					 = getMinorHascode(context, majorBusID, fileList, mxMinorFileDigestValue);

			boolean isHashCodeMatchesWithActiveMinor = (!mxMinorHascode.equals("") && !matrixHashcode.equals("") && mxMinorHascode.equals(matrixHashcode));

			if(inputMajorBusID.equals(majorBusID)
					&& (mxMinorRevision.equalsIgnoreCase(minorBusRevision) || (!isRevisionInWorkingSetMatched && (isHashCodeMatchesWithActiveMinor || familyObseleteStatus.equalsIgnoreCase("false")))) 
					&& (workingSetHashcode == null || "".equals(workingSetHashcode) || workingSetHashcode.equals(matrixHashcode)))
			{
				obsoleteObjectDetails.addElement("false");

				//Virtual Finalization usecase
				if(!mxMinorRevision.equalsIgnoreCase(minorBusRevision) && (isHashCodeMatchesWithActiveMinor || familyObseleteStatus.equalsIgnoreCase("false")))
				{
					if(isRevisionFinalized(mxMajorRevision, finalizationStatusForAllRevisions))
					{
						obsoleteObjectDetails.addElement(majorBusID);
						obsoleteObjectDetails.addElement(mxMajorRevision);
						obsoleteObjectDetails.addElement(" ");
					}
					else
					{
						obsoleteObjectDetails.addElement(activeBusID);
						obsoleteObjectDetails.addElement(mxMajorRevision);

						String activeVersion = MCADUtil.getVersionFromMinorRevision(mxMajorRevision, mxMinorRevision);
						obsoleteObjectDetails.addElement(activeVersion);
					}
				}
			}
			else
			{
				obsoleteObjectDetails.addElement("true");
				if(isRevisionFinalized(mxMajorRevision, finalizationStatusForAllRevisions))
				{
					obsoleteObjectDetails.addElement(majorBusID);
					obsoleteObjectDetails.addElement(mxMajorRevision);
					obsoleteObjectDetails.addElement(" ");
				}
				else
				{
					obsoleteObjectDetails.addElement(activeBusID);
					obsoleteObjectDetails.addElement(mxMajorRevision);

					String activeVersion = MCADUtil.getVersionFromMinorRevision(mxMajorRevision, mxMinorRevision);
					obsoleteObjectDetails.addElement(activeVersion);
				}
			}

			returnTable.put(inputid, obsoleteObjectDetails);
		}
	}

	private String getMinorHascode(Context context, String majorBusID, String fileList, String mxMinorFileDigestValue)throws Exception 
	{
		String mxMinorHascode	= "";

		Hashtable hashFileTable = (Hashtable)IEFServerHashCodeUtil.getIEFMessageDigestTableForBO(mxMinorFileDigestValue);

		if(hashFileTable == null)
		{
			mxMinorHascode = "";
		}
		else if((mxMinorHascode = (String)hashFileTable.get(fileList)) == null)
		{
			String ATTRIBUTE_RENAMED_FROM			= "attribute["+ MCADMxUtil.getActualNameForAEFData(context, "attribute_RenamedFrom") + "]";
			String ATTRIBUTE_TITLE					= "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Title") + "]";

			String integrationName					= mxUtil.getIntegrationName(context, majorBusID);

			if( MCADStringUtils.isNullOrEmpty(integrationName) )
				integrationName 	= this.integrationName;

			boolean isObjectAndFileNameDifferent	= false;

			if(!MCADStringUtils.isNullOrEmpty(integrationName))
			{
				IEFSimpleConfigObject simpleGCO		= IEFSimpleConfigObject.getSimpleGCO(context, integrationName);

				if(simpleGCO != null)
				{
					String objectAndFileNameDifferent 	= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-ObjectAndFileNameDifferent");
					String attributeValue				= simpleGCO.getConfigAttributeValue(objectAndFileNameDifferent);
					isObjectAndFileNameDifferent		= MCADUtil.getBoolean(attributeValue);
				}
			}

			util               = new MCADMxUtil(context, serverResourceBundle, cache);

			String [] objectId	= new String[1];
			objectId[0]			= majorBusID;

			StringList busSelectList = new StringList(3, 1);
			busSelectList.add(ATTRIBUTE_RENAMED_FROM);
			busSelectList.add("name");

			if(isObjectAndFileNameDifferent)
				busSelectList.add(ATTRIBUTE_TITLE);

			BusinessObjectWithSelectList busWithSelectListRename = BusinessObjectWithSelect.getSelectBusinessObjectData(context, objectId, busSelectList);
			BusinessObjectWithSelect busWithSelectRename 		 = busWithSelectListRename.getElement(0);
			String renamedFrom 		 							 = busWithSelectRename.getSelectData(ATTRIBUTE_RENAMED_FROM);

			if(renamedFrom.equals(null) || renamedFrom.equals(""))
			{
				mxMinorHascode = "";
			}
			else
			{
				String	objName = busWithSelectRename.getSelectData("name");

				if(isObjectAndFileNameDifferent)
					objName		= busWithSelectRename.getSelectData(ATTRIBUTE_TITLE);

				fileList		   = util.getTargetFileName(fileList, objName, renamedFrom);

				if((mxMinorHascode = (String)hashFileTable.get(fileList)) == null)
				{
					mxMinorHascode = "";
				}
			}
		}
		return mxMinorHascode;
	}

	private Hashtable getInfoFromMajorids(Context context, Set majorIds, String isCheckObsoleteAcrossRevision) throws Exception
	{
		Hashtable returnTable = new Hashtable(majorIds.size());

		String [] oids = new String[majorIds.size()];
		majorIds.toArray(oids);

		StringList busSelectionList = new StringList(4);

		busSelectionList.addElement("id");

		if("true".equalsIgnoreCase(isCheckObsoleteAcrossRevision))
		{
			busSelectionList.addElement("revisions");
			busSelectionList.addElement("revisions.id");

			busSelectionList.addElement("revisions." + SELECT_ON_ACTIVE_MINOR + "id");
			busSelectionList.addElement("revisions." + SELECT_ON_ACTIVE_MINOR + "revision");
			busSelectionList.addElement("revisions." + SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
		}
		else
		{
			busSelectionList.addElement("revision");

			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + "id");
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + "revision");
			busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
		}

		BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, oids, busSelectionList);

		for(int j = 0; j < buslWithSelectionList.size(); j++)
		{
			Vector infoVector = new Vector(3);
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(j);
			String busId								 = busObjectWithSelect.getSelectData("id");

			String majorBusId							 = busId;
			String majorObjectRevision					 = "";
			String activeVersionId						 = "";
			String activeVersion						 = "";
			String activeVersionfileMessageDigestValue	 = "";

			if("true".equalsIgnoreCase(isCheckObsoleteAcrossRevision))
			{
				StringList majorObjectRevisions				 = busObjectWithSelect.getSelectDataList("revisions");


				for(int i = (majorObjectRevisions.size() - 1); i >=0 ; i--) // get valid object (created by cad tool and not by matrix)
				{
					majorObjectRevision					= (String)majorObjectRevisions.elementAt(i);
					majorBusId							= busObjectWithSelect.getSelectData("revisions[" + majorObjectRevision + "].id");
					activeVersionId						= busObjectWithSelect.getSelectData("revisions[" + majorObjectRevision + "]." + SELECT_ON_ACTIVE_MINOR + "id");
					activeVersion						= busObjectWithSelect.getSelectData("revisions[" + majorObjectRevision + "]." + SELECT_ON_ACTIVE_MINOR + "revision");
					activeVersionfileMessageDigestValue = busObjectWithSelect.getSelectData("revisions[" + majorObjectRevision + "]." + SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST);

					if(activeVersionId != null && !activeVersionId.equals(""))
					{
						break;
					}
				}
			}
			else
			{
				majorObjectRevision = busObjectWithSelect.getSelectData("revision");		
				activeVersionId	    = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + "id");
				activeVersion	    = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + "revision");
				activeVersionfileMessageDigestValue = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + SELECT_ATTR_IEFFILEMESSAGEDIGEST);
			}

			infoVector.addElement(majorObjectRevision);
			infoVector.addElement(majorBusId);
			infoVector.addElement(activeVersionId);
			infoVector.addElement(activeVersion);
			infoVector.addElement(activeVersionfileMessageDigestValue);

			returnTable.put(busId, infoVector);
		}

		return	returnTable;
	}

	// Returns true|{Latest Revision} OR error|{Error String} OR false if not obsolete
	public Vector returnObsoleteInfo(Context context, String[] args)  throws Exception
	{
		Vector obsoleteObjectDetails			 = new Vector(1);

		String inputMajorBusID					 = args[0];
		String minorBusRevision					 = args[1];
		String matrixHashcode					 = args[2];
		String workingSetHashcode				 = args[3];
		String finalizationStatusForAllRevisions = args[4];
		String isCheckObsoleteAcrossRevision	 = args[5];
		String fileList							 = args[6];

		try
		{
			Hashtable inputArgsTable    = new Hashtable(2);
			Hashtable inputIdsInfoTable = new Hashtable(1);
			Vector inputArgs			= new Vector();

			inputArgs.addElement(minorBusRevision);
			inputArgs.addElement(inputMajorBusID);
			inputArgs.addElement(matrixHashcode);
			inputArgs.addElement(workingSetHashcode);
			inputArgs.addElement(finalizationStatusForAllRevisions);
			inputArgs.addElement(fileList);

			inputArgsTable.put("isCheckObsoleteAcrossRevision", isCheckObsoleteAcrossRevision);
			inputArgsTable.put("inputTable", inputIdsInfoTable);

			inputIdsInfoTable.put("dummyID", inputArgs);

			String [] newArgs		  = JPO.packArgs(inputArgsTable);
			Hashtable returnTable	  = returnObsoleteInfoForObjectIds(context, newArgs);
			obsoleteObjectDetails	  = (Vector) returnTable.get("dummyID");
		}
		catch(Exception e)
		{
			obsoleteObjectDetails.addElement("error");
			obsoleteObjectDetails.addElement(e.getMessage());
		}

		return obsoleteObjectDetails; 
	}

	private boolean isRevisionFinalized(String inputRevision, String finalizationStatusForAllRevisions)
	{
		boolean isFinalized				= false; 
		StringTokenizer revisionTokens	= new StringTokenizer(finalizationStatusForAllRevisions, "|");

		while(revisionTokens.hasMoreTokens())
		{
			String revision = "";
			String status	= "";

			String revisionStatus = revisionTokens.nextToken();

			StringTokenizer statusTokens = new StringTokenizer(revisionStatus, "=");

			if(statusTokens.hasMoreTokens())
				revision = statusTokens.nextToken();
			if(statusTokens.hasMoreTokens())
				status = statusTokens.nextToken();

			if(revision.equalsIgnoreCase(inputRevision))
			{
				if(status.equalsIgnoreCase("true"))
					isFinalized = true;
				else
					isFinalized = false;

				break;
			}
		}

		return isFinalized;
	}
}
