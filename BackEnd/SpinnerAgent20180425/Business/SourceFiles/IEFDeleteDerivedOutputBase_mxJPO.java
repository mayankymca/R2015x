/*
**  IEFDeleteDerivedOutputBase
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to delete derived output objects
*/
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.DomainObject;

public class IEFDeleteDerivedOutputBase_mxJPO
{
	MatrixWriter _mxWriter = null;
	// Variable to denote whether Debug messages are to be put or not 
	static boolean bDebugOn = false;
	String _sObjectID = null;

	/**
	 * The no-argument constructor.
	 */
	public IEFDeleteDerivedOutputBase_mxJPO()
	{
	}

	/**
	 * Constructor wich accepts the Matrix context and an array of String
	 * arguments.
	 */
	public IEFDeleteDerivedOutputBase_mxJPO(Context context, String[] args) throws Exception
	{
		_mxWriter = new MatrixWriter(context);

		// Get the OBJECTID of the object in context
        _sObjectID =args[0];
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		String relDirectionName					= "";
		Hashtable derivedOutputLikeRelsAndEnds 	= null;

		try
		{
			MCADMxUtil util						= new MCADMxUtil(context, null, new IEFGlobalCache());
			
			String integrationName				= util.getIntegrationName(context, _sObjectID);
		
			String gcoRev     = MCADMxUtil.getConfigObjectRevision(context);

			StringBuffer sbKey = new StringBuffer(100);
			sbKey.append(integrationName);
			sbKey.append("|");
			sbKey.append(gcoRev);
			String sKey = sbKey.toString();	

			//MCADGlobalConfigObject globalConf	= getGlobalConfigObject(context, integrationName, util);
			MCADGlobalConfigObject globalConf = (MCADGlobalConfigObject)util.getUserSpecificFullGCODefinition(context,sKey);

			if(globalConf != null)
			{
			derivedOutputLikeRelsAndEnds 		= globalConf.getRelationshipsOfClass(MCADServerSettings.DERIVEDOUTPUT_LIKE);
			Enumeration allRels = derivedOutputLikeRelsAndEnds.keys();

            while(allRels.hasMoreElements())
            {
                String relName		= (String)allRels.nextElement();
				String relnDir		= (String)derivedOutputLikeRelsAndEnds.get(relName);
				relDirectionName	= relnDir + ", " + relName;
				deleteConnectedDerivedOutputs(context, relDirectionName);
			}
			}
			
		}
        catch(Exception me)
        {
			_mxWriter.write("Error occurred:" + me.getMessage());
			MCADServerException.createException(me.getMessage(), me);
		}
		return 0;
	}

	protected void deleteConnectedDerivedOutputs(Context context, String relDirectionName) throws Exception
	{
		int index				= relDirectionName.indexOf(",");
		String relDerivedOutput = relDirectionName.substring(index+1).trim();
		String relEnd			= relDirectionName.substring(0, index).trim();
		String relOppositeEnd	= relEnd.equals("to") ? "from" : "to";
		
		String derivedOutputIds		= MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3",_sObjectID,relOppositeEnd + "[" + relDerivedOutput + "]." + relEnd + ".id","|");
		StringTokenizer tokenizer	= new StringTokenizer(derivedOutputIds, "|");
		
		StringList slOid = new StringList();
		while(tokenizer.hasMoreElements())
		{
			String objId = (String) tokenizer.nextElement();
			//MqlUtil.mqlCommand(context, "delete bus $1",objId);
			slOid.addElement(objId);
		}
		String [] oidsTopLevel		  = new String [slOid.size()];
		slOid.toArray(oidsTopLevel);			
		
		DomainObject.deleteObjects(context, oidsTopLevel);		
	}

	protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String integrationName, MCADMxUtil mxUtil) throws Exception
    {
		MCADGlobalConfigObject gcoObject	= null;
		
		if(integrationName != null && integrationName.length() > 0)
		{
		IEFSimpleConfigObject simpleLCO = IEFSimpleConfigObject.getSimpleLCO(context);

		String gcoType  = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
		String attrName = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");

		Hashtable integNameGCOMapping		= simpleLCO.getAttributeAsHashtable(attrName, "\n", "|");
	    String gcoName						= (String)integNameGCOMapping.get(integrationName);

		MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(null);
		gcoObject							= configLoader.createGlobalConfigObject(context, mxUtil, gcoType, gcoName);
		}
		return gcoObject;
    }
}

