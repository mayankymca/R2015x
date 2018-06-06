/*
**  MCADPurgeBase
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to Purge the versioned objects.
*/

import java.util.Hashtable;
import java.util.StringTokenizer;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.Context;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFBaselineHelper;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;

public class MCADPurgeBase_mxJPO extends IEFCommonUIActions_mxJPO
{
    public  MCADPurgeBase_mxJPO  ()
    {

    }
    
    public MCADPurgeBase_mxJPO (Context context, String[] args) throws Exception
    {
      if (!context.isConnected())
		MCADServerException.createException("not supported no desktop client", null);
    }
    
    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }

    protected void canPerformOperationCustom(Context _context, Hashtable resultDataTable) throws MCADException
    {
        try
        {
            if (_busObject == null)
            {
                MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.BusinessObjectNotFound"), null);
            }
            _busObject.open(_context);
	        //String sType		= _busObject.getTypeName();  //[NDM] OP6
			// [NDM] H68
			//boolean bFinalized  = _generalUtil.isBusObjectFinalized(_context, _busObject);
			
			//if(_util.isMajorObject(_context, _busObjectID)  && bFinalized)//_globalConfig.isMajorType(sType) //[NDM] OP6
			if(_util.isMajorObject(_context, _busObjectID))
			{
				//bShowButton = true;
			}
			else
			{
				MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.PurgeValidForFinalizedMajorTypes"), null);
			}
			_busObject.close(_context);
        }
        catch(Exception e)
        {
            MCADServerException.createException(_serverResourceBundle.getString("mcadIntegration.Server.Message.PurgeNotAllowed") + e.getMessage(), e);
        }
    }

	public void executeCustom(Context _context, Hashtable resultAndStatusTable)  throws MCADException
    {
        String minorBOIdList = (String)_argumentsTable.get(MCADServerSettings.MINOR_BUSID_LIST);
		IEFBaselineHelper  baselineHeper 	= new IEFBaselineHelper(_context,_globalConfig, _serverResourceBundle , new IEFGlobalCache());

        try
		{
			BusinessObjectList minorList	= _util.getMinorObjects(_context, _busObject);
			String relLatestVersion			= MCADMxUtil.getActualNameForAEFData(_context, "relationship_LatestVersion");
			BusinessObject minorObj			= (BusinessObject)minorList.elementAt(0);

			minorObj.open(_context);
			String minorObjId = minorObj.getObjectId();
			minorObj.close(_context);
			String lastRemainingRevId = _util.getLatestRevisionID(_context,minorObjId);

			while(lastRemainingRevId != null)
			{  
				if(minorBOIdList.indexOf(lastRemainingRevId) == -1)
				{
					if(!_util.doesRelationExist(_context,lastRemainingRevId, _busObjectID, relLatestVersion))				
					{
						_util.connectBusObjects(_context,_busObjectID, lastRemainingRevId, relLatestVersion, true, null);
					}

					break;
				}

				BusinessObject latestObj	= new BusinessObject(lastRemainingRevId);
				BusinessObject previousObj	= latestObj.getPreviousRevision(_context);

				previousObj.open(_context);
				String prevObjId       = previousObj.getObjectId();
				previousObj.close(_context);

				if(prevObjId == null || prevObjId.equals(lastRemainingRevId)|| prevObjId.equals(""))
					break;
				else
					lastRemainingRevId = prevObjId;
			}

			StringTokenizer st = new StringTokenizer(minorBOIdList,"|");
			while (st.hasMoreTokens())
			{
				String sThisObjId		= st.nextToken();
				BusinessObject thisBO	= new BusinessObject(sThisObjId);
				
				thisBO.open(_context);
				boolean isObjectConnectedToBaseline = baselineHeper.isBaselineRelationshipExistsForId(_context, sThisObjId);
				thisBO.close(_context);

				if(isObjectConnectedToBaseline)
				{						
						Hashtable tokensTable = new Hashtable(4);
						tokensTable.put("TYPE",thisBO.getTypeName() );
						tokensTable.put("NAME",thisBO.getName() );
						tokensTable.put("REVISION",thisBO.getRevision() );						
						String msg = _serverResourceBundle.getString("mcadIntegration.Server.Message.CanNotPurgeObjectWithBaselineAttached",tokensTable);
						MCADServerException.createException(msg, null);					
				}

				String resultMessage	= _generalUtil.purgeIndividual(_context, _busObject, thisBO);

				// send back the list of objects purged..
				resultAndStatusTable.put(MCADServerSettings.JPO_EXECUTION_RESULT,resultMessage);
			}
        }
        catch(Exception e)
        {
			MCADServerException.createException(e.getMessage(), e);
        }
    }
}

