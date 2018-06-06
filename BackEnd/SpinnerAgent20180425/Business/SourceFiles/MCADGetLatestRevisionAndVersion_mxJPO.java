/*
**  MCADGetLatestRevisionAndVersion
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program for identifying latest revision and version
*/
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
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
import com.matrixone.MCADIntegration.utils.MCADUtil;

public class MCADGetLatestRevisionAndVersion_mxJPO
{
    MCADServerResourceBundle _bundle	= null;
	IEFGlobalCache _cache				= null;
    MCADMxUtil _util					= null;
    MCADServerGeneralUtil _generalUtil	= null;
    MCADGlobalConfigObject _gco			= null;

    public  MCADGetLatestRevisionAndVersion_mxJPO  ()
    {
    }
    public MCADGetLatestRevisionAndVersion_mxJPO (Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            MCADServerException.createException("not supported no desktop client", null);
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }

    private void init(Context _context, MCADGlobalConfigObject GCO, String sLanguage)  throws Exception
    {
        _bundle			= new MCADServerResourceBundle(sLanguage);
		_cache			= new IEFGlobalCache();
        _util			= new MCADMxUtil(_context, _bundle, _cache);
        _gco			= GCO;
        _generalUtil	= new MCADServerGeneralUtil(_context,_gco, _bundle, _cache);
    }

    public String getRevisionAndVersion(Context _context, String[] packedArgs)throws Exception
    {
		String revisionVersionType		= null;
		String toEnforceTypeMatching	= null;

		Hashtable args = (Hashtable) JPO.unpackArgs(packedArgs);

		String nameCadType					= (String)args.get("NameAndCadType");
		MCADGlobalConfigObject packedGCO	= (MCADGlobalConfigObject)args.get(MCADServerSettings.GCO_OBJECT);
        String sLanguage					= (String)args.get(MCADServerSettings.LANGUAGE_NAME);
        
		toEnforceTypeMatching		= (String)args.get("EnforceTypeMatching");
		if(toEnforceTypeMatching == null)
			toEnforceTypeMatching = "FALSE";		

		init(_context, packedGCO, sLanguage);

		if(nameCadType.equals(""))
		{
			MCADServerException.createException("Document name and type supplied to JPO is incorrect", null);
		}

		StringTokenizer tokens = new StringTokenizer(nameCadType, "|");
		String name		= (String)tokens.nextElement();
		String cadType	= (String)tokens.nextElement();

		Vector mappedMatrixTypes = _gco.getMappedBusTypes(cadType);
		BusinessObject majorObj = null;
		String mxType			= "";
		String lastRev			= "";
		String newMxType = "";

		for(int i=0; i < mappedMatrixTypes.size(); i++)
		{
			mxType = (String)mappedMatrixTypes.elementAt(i);
			lastRev = _util.getLatestRevisionString(_context, mxType, name);
			majorObj = new BusinessObject(mxType, name, lastRev, "");
			if(majorObj.exists(_context))
			{
				break;
			}
			else
		{
			Vector changedTypeList 	= _gco.getTypeChangeMapList(cadType);
				for(int j=0; j<changedTypeList.size(); j++)
			{
				String newCadType = (String)changedTypeList.elementAt(i);
				mappedMatrixTypes = _gco.getMappedBusTypes(newCadType);
				newMxType		  = (String)mappedMatrixTypes.elementAt(0);
				lastRev			  = _util.getLatestRevisionString(_context, newMxType, name);
				if(!"TRUE".equalsIgnoreCase(toEnforceTypeMatching))
					mxType = newMxType;
				majorObj = new BusinessObject(newMxType, name, lastRev, "");
				if(majorObj.exists(_context))
					break;
			}
		}
		}

		majorObj.open(_context);
		BusinessObjectList majorObjList = majorObj.getRevisions(_context);

		//get the latest major object
		String latestRevisionAndVersion = this.getLatestRevisionAndVersion(_context, majorObjList, mxType, toEnforceTypeMatching);
		if(latestRevisionAndVersion.endsWith("|"))
		{
			latestRevisionAndVersion = latestRevisionAndVersion + " ";
		}

		revisionVersionType = latestRevisionAndVersion + "|" + mxType;
		majorObj.close(_context);
		return revisionVersionType;
    }

    private String getLatestRevisionAndVersion(Context _context, BusinessObjectList busObjList, String mxType, String toEnforceTypeMatching)throws Exception
    {
		String latestRevisionAndVersion = "";

        int size = busObjList.size();

        for(int ind = size; ind > 0; ind--)
        {
            BusinessObject busObj = (BusinessObject)busObjList.get(ind - 1);
		busObj.open(_context);
		String majorType = busObj.getTypeName();
		String latestRevision	= busObj.getRevision();
		busObj.close(_context);

		if(_generalUtil.isBusObjectFinalized(_context, busObj))
		{
			
			String latestVersion	= "";
			latestRevisionAndVersion = latestRevision + "|" + latestVersion;

			if(!"TRUE".equalsIgnoreCase(toEnforceTypeMatching) || mxType.equalsIgnoreCase(majorType))
			{
				break;
			}
		}
		else
		{

			String latestVersion = "";
			BusinessObject activeMinorObject = _util.getActiveMinor(_context, busObj);
			if(activeMinorObject!=null)
			{

				activeMinorObject.open(_context);
				String latestType		= activeMinorObject.getTypeName();
				latestVersion	= activeMinorObject.getRevision();
				activeMinorObject.close(_context);
				String correspondinglatestType	= _util.getCorrespondingType(_context, latestType);
				
				if("TRUE".equalsIgnoreCase(toEnforceTypeMatching) && correspondinglatestType.equalsIgnoreCase(mxType))
				{

					latestVersion				= MCADUtil.getVersionFromMinorRevision(latestRevision, latestVersion);
					latestRevisionAndVersion	= latestRevision + "|" + latestVersion;

					break;
				}
			}
			
			if(latestRevisionAndVersion.equals(""))
			{
				String truncatedLatestVersion	= "";
				String truncatedLatestRevision	= "";
				
				String latestMinorId = _util.getLatestMinorID(_context, busObj);

				if(latestMinorId != null && !latestMinorId.equals(""))
				{
					BusinessObject latestMinor = new BusinessObject(latestMinorId);
					
					while(latestMinor != null)
					{
						latestMinor.open(_context);
						String correspondingType	= _util.getCorrespondingType(_context, latestMinor.getTypeName());

						latestMinorId = latestMinor.getObjectId(); 
						latestVersion = latestMinor.getRevision();
						latestMinor.close(_context);

						truncatedLatestVersion	= MCADUtil.getVersionFromMinorRevision(latestRevision, latestVersion);

						if("TRUE".equalsIgnoreCase(toEnforceTypeMatching) && mxType.equalsIgnoreCase(correspondingType))
						{

							truncatedLatestRevision = _util.getRevisionStringForMinorRev(latestVersion);

							break;
						}
						else if(!"TRUE".equalsIgnoreCase(toEnforceTypeMatching))
						{

							break;
						}

						latestMinor = latestMinor.getPreviousRevision(_context);

						if(!latestMinor.exists(_context))
						{
							latestMinor = null;
						}
					}
				}
				if (!truncatedLatestRevision.equalsIgnoreCase(""))
				{

					latestRevisionAndVersion = truncatedLatestRevision + "|" + truncatedLatestVersion;
					break;
				}
				else if(!"TRUE".equalsIgnoreCase(toEnforceTypeMatching))
				{

					latestRevisionAndVersion = latestRevision + "|" + truncatedLatestVersion;

					break;
				}
			}
		}
        }

	return latestRevisionAndVersion;
    }

}
