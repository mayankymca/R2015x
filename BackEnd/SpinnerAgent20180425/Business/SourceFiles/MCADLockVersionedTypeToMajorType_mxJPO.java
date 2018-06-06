/*
 **  MCADLockVersionedTypeToMajorType.java
 **
 **  Copyright Dassault Systemes, 1992-2010.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **	This program is used as a check trigger on the events on
 **	versioned types.The purpose of this program is to control
 **	access to the versioned type objects through the access on
 **	on the corresponding major type object.
 **
 **	Parameters - sEvent - Event name
 **	             sOID - versioned type object's id
 **	
 **	Returns - 0 - if access for the input event is found on
 **	               the corresponding major object
 **	          1 - if access for the input event is not found on
 **	               the corresponding major object  
 **	
 **
 */

import java.util.Iterator;
import java.util.Vector;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADUtil;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.util.StringList;

public class MCADLockVersionedTypeToMajorType_mxJPO
{
	private boolean isCheckinEx = false;
	private boolean isCheckin   = false;
	private boolean isAttributeSynch   = false;
	private boolean isLockUnlock   = false;
	private boolean isCheckout   = false;
	private boolean isCheckoutEx   = false;

	private String versionOf     = "";
	private String activeVersion = "";
	private String latestVersion = "";

	public MCADLockVersionedTypeToMajorType_mxJPO ()
	{
	}

	public MCADLockVersionedTypeToMajorType_mxJPO (Context context, String args[] ) throws Exception
	{
		String language   = "en-us";
		MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

		isCheckin   	  = getRPEValue(context, mxUtil, MCADServerSettings.IEF_CHECKIN, true).equalsIgnoreCase("true");
		isCheckinEx 	  = getRPEValue(context, mxUtil, MCADServerSettings.IEF_CHECKINEX, true).equalsIgnoreCase("true");
		isAttributeSynch  = getRPEValue(context, mxUtil, MCADServerSettings.IEF_ATTR_SYNC, true).equalsIgnoreCase("true");

		isLockUnlock = getRPEValue(context, mxUtil, MCADServerSettings.IEF_LOCK_UNLOCK, true).equalsIgnoreCase("true");

		isCheckout = getRPEValue(context, mxUtil, MCADServerSettings.IEF_CHECKOUT, true).equalsIgnoreCase("true");
		isCheckoutEx = getRPEValue(context, mxUtil, MCADServerSettings.IEF_CHECKOUTEX, true).equalsIgnoreCase("true");

		versionOf     = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		activeVersion = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
		latestVersion = MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion");
	}

	public int mxMain(Context context, String []args) throws Exception
	{
		return 0;
	}

	private String getRPEValue(Context context, MCADMxUtil mxUtil,  String variableName, boolean isGlobal)
	{
		String sResult = "";
		if(isGlobal)
		{
			String Args[] = new String[2];
			Args[0] = "global";
			Args[1] = variableName;
			sResult	= mxUtil.executeMQL(context, "get env $1 $2", Args);
		}
		else
		{
			String Args[] = new String[1];
			Args[0] = variableName;
			sResult	= mxUtil.executeMQL(context, "get env $1", Args);
		}
		String result	= "";

		if(sResult.startsWith("true"))
		{
			result = sResult.substring(sResult.indexOf("|")+1, sResult.length());
		}

		return result;
	}

	private String getAccessToCheck(Context context, String sEvent, MCADMxUtil mxUtil)
	{
		String returnAccess = "";

		if (sEvent.equalsIgnoreCase("modifyattribute")) 
		{
			returnAccess = "Modify";
		}
		else if (sEvent.equalsIgnoreCase("modifydescription")) 
		{
			returnAccess = "Modify";
		}
		else if (sEvent.equalsIgnoreCase("connect")) 
		{
			String sDirection  = this.getRPEValue(context, mxUtil, "CONNECTION", false);

			if (sDirection.equalsIgnoreCase("To")) 
			{
				returnAccess = "ToConnect";
			}
			else if (sDirection.equalsIgnoreCase("From")) 
			{
				returnAccess = "FromConnect";
			}
		}
		else if (sEvent.equalsIgnoreCase("disconnect")) 
		{
			String sDirection  = this.getRPEValue(context, mxUtil, "CONNECTION", false);

			if (sDirection.equalsIgnoreCase("To")) 
			{
				returnAccess = "ToDisconnect";
			}
			else if (sDirection.equalsIgnoreCase("From")) 
			{
				returnAccess = "FromDisconnect";
			}
		}
		else if (sEvent.equalsIgnoreCase("changename")) 
		{
			returnAccess = "ChangeName";
		}
		else if (sEvent.equalsIgnoreCase("changetype")) 
		{
			returnAccess = "ChangeType";
		}
		else if(sEvent.equalsIgnoreCase("changeowner"))
		{
			returnAccess = "ChangeOwner";
		}
		else if (sEvent.equalsIgnoreCase("changepolicy")) 
		{
			returnAccess = "ChangePolicy";
		}
		else if (sEvent.equalsIgnoreCase("changevault")) 
		{
			returnAccess = "ChangeVault";
		}
		else if (sEvent.equalsIgnoreCase("checkin")) 
		{
			returnAccess = "CheckIn";
		}
		else if (sEvent.equalsIgnoreCase("checkout")) 
		{
			returnAccess = "CheckOut";
		}
		else if (sEvent.equalsIgnoreCase("copy")) 
		{
			returnAccess = "Create";
		}
		else if (sEvent.equalsIgnoreCase("grant")) 
		{
			returnAccess = "Grant";
		}
		else if (sEvent.equalsIgnoreCase("lock")) 
		{
			returnAccess = "Lock";
		}
		else if (sEvent.equalsIgnoreCase("removefile")) 
		{
			returnAccess = "CheckIn";
		}
		else if (sEvent.equalsIgnoreCase("revoke")) 
		{
			returnAccess = "Revoke";
		}
		else if (sEvent.equalsIgnoreCase("unlock")) 
		{
			returnAccess = "UnLock";
		}

		return returnAccess;
	}

	public int checkMajorAccess(Context context, String[] args) throws Exception
	{
		int returnValue   = 1;
		String language   = "en-us";
		MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

		String sEvent 	  = args[0];
		String sObjectId  = args[1];

		String sAccess  = getAccessToCheck(context, sEvent, mxUtil);

		String relName = "";

		boolean isDataModelRelationshipsConnnectOrDisconnect = false;

		if(sEvent.equalsIgnoreCase("connect") || sEvent.equalsIgnoreCase("disconnect"))
		{
			relName  = this.getRPEValue(context, mxUtil, "RELTYPE", false);

			if(relName.equals(versionOf) || relName.equals(activeVersion) || relName.equals(latestVersion))
				isDataModelRelationshipsConnnectOrDisconnect = true;
		}

		if ((sAccess.equals("Modify") || isDataModelRelationshipsConnnectOrDisconnect)&& ( isCheckin || isCheckinEx))
		{
			returnValue = 0;
		}
		else
		{
			String SELECT_ON_MAJOR_ID = "from[" + versionOf + "].to.id";
			String SELECT_MAJOR_EXISTS = "from[" + versionOf + "]";

			String [] oids  = new String [1];
			oids[0]         = sObjectId;

			StringList selectStmtsForMinor = new StringList(1, 1);
			selectStmtsForMinor.add(SELECT_ON_MAJOR_ID);
			selectStmtsForMinor.add(SELECT_MAJOR_EXISTS);

			BusinessObjectWithSelectList busWithSelectListForInputMinor = BusinessObject.getSelectBusinessObjectData(context, oids, selectStmtsForMinor);

			BusinessObjectWithSelect busWithSelectForMinor  = (BusinessObjectWithSelect) busWithSelectListForInputMinor.elementAt(0);
			String majorObjId                       		= busWithSelectForMinor.getSelectData(SELECT_ON_MAJOR_ID);
			String majorExist								= busWithSelectForMinor.getSelectData(SELECT_MAJOR_EXISTS);
			if((majorObjId.equals("") || majorExist.equals("false") || majorExist.equals("FALSE")) && (sAccess.equals("ChangeOwner") || sAccess.equals("CheckIn") || sAccess.equals("Modify") || sAccess.equals("ToConnect") || sAccess.equals("FromConnect") || sAccess.equals("ToDisconnect") || sAccess.equals("FromDisconnect") || sAccess.equals("Delete") || sAccess.equals("ChangeType") || sAccess.equals("ChangePolicy") || sAccess.equals("ChangeName")))
			{
				returnValue = 0;
			}
			else
			{
				StringList selectStmtsForMajor = new StringList(4, 1);

				selectStmtsForMajor.add("type");
				selectStmtsForMajor.add("name");
				selectStmtsForMajor.add("current.access");
				selectStmtsForMajor.add("revision");

				if (sEvent.equalsIgnoreCase("copy"))
				{
					selectStmtsForMajor.add("state");
					selectStmtsForMajor.add("state.access");
				}

				BusinessObjectWithSelectList busWithSelectListForMajor = BusinessObject.getSelectBusinessObjectData(context, new String[]{majorObjId}, selectStmtsForMajor);

				BusinessObjectWithSelect busWithSelectForMajor  = (BusinessObjectWithSelect) busWithSelectListForMajor.elementAt(0);

				String sAccessList = "";
				// get access mask for the major
				if (sEvent.equalsIgnoreCase("copy"))
				{
					StringList stateAccessList = new StringList(); 
					StringList stateList   	   = busWithSelectForMajor.getSelectDataList("state");

					for (int i = 0; i < stateList.size(); i++)
					{
						String stateName       = (String)stateList.get(i);
						String stateAccess = busWithSelectForMajor.getSelectData("state[" + stateName + "].access");

						stateAccessList.add(stateAccess);

						if(i == 0 && stateAccess.equals("all"))
							break;
					}

					sAccessList =  MCADUtil.getStringFromCollection(stateAccessList, ",");
				} 
				else
				{
					sAccessList =  busWithSelectForMajor.getSelectData("current.access");
				}

				if (sAccessList.equals("all"))
				{
					returnValue = 0;
				}  
				else
				{
					Vector accesList = MCADUtil.getVectorFromString(sAccessList, ",");

					for (Iterator iterator = accesList.iterator(); iterator.hasNext();)
					{
						String access = (String) iterator.next();

						if(access.equalsIgnoreCase(sAccess))
						{
							returnValue = 0;
							break;
						}
					}

					if (returnValue != 0)
					{
						//throw error message
						String busType = busWithSelectForMajor.getSelectData("type");
						String busName = busWithSelectForMajor.getSelectData("name");
						String majorBusRev = busWithSelectForMajor.getSelectData("revision");

						String message = "No " + sAccess + " access to business object " + busType + " " + busName + " " + majorBusRev;

						emxContextUtil_mxJPO.mqlError(context,message);
					} 
				}
			}
		}

		if(returnValue == 0 && !isCheckin && !isCheckinEx && 
				!isAttributeSynch && !isLockUnlock && !isCheckout && !isCheckoutEx)
		{
			if(sEvent.equalsIgnoreCase("checkin") || sEvent.equalsIgnoreCase("removefile"))
			{	
				DECModifyUpdateStamp_mxJPO object = new DECModifyUpdateStamp_mxJPO(context, args);

				object.modifyUpdateStamp(context, args);
			}
		}

		return returnValue;
	}
}
