/*
**  MCADUpdateNextInRevision
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program for updating "next" in revision and blank in version
*/
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class MCADUpdateNextInRevision_mxJPO
{
    MCADServerResourceBundle _bundle	= null;
	IEFGlobalCache	_cache				= null;
    MCADMxUtil _util					= null;
    MCADServerGeneralUtil _generalUtil	= null;
    MCADGlobalConfigObject _gco			= null;

    String labelNext = null;
    
    public  MCADUpdateNextInRevision_mxJPO  ()
    {
    }
    public MCADUpdateNextInRevision_mxJPO (Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
            MCADServerException.createException("not supported no desktop client", null);
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }
   
	private void init(Context context, MCADGlobalConfigObject GCO, String sLanguage)  throws Exception
    {
        _bundle			= new MCADServerResourceBundle(sLanguage);
		_cache			= new IEFGlobalCache();
        _util			= new MCADMxUtil(context, _bundle, _cache);        
		_gco			= GCO;		
        _generalUtil	= new MCADServerGeneralUtil(context,_gco, _bundle, _cache);

        labelNext  =  _bundle.getString("mcadIntegration.Server.FieldName.Next");
    }

    public String getRevisionAndVersion(Context context, String[] packedArgs)throws Exception
    {
		String revisionVersionType	= null;
		Hashtable args				= (Hashtable) JPO.unpackArgs(packedArgs);

		String nameCadType			= (String)args.get("NameAndCadType");
		MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)args.get(MCADServerSettings.GCO_OBJECT);
        String sLanguage			= (String)args.get(MCADServerSettings.LANGUAGE_NAME);        		

		init(context, gco, sLanguage);

		if(nameCadType.equals(""))
		{
			//TODO : I18N
			MCADServerException.createException("Document name and type supplied to JPO is incorrect", null);
		}
        
		StringTokenizer tokens = new StringTokenizer(nameCadType, "|");
		String name		= (String)tokens.nextElement();
		String cadType	= (String)tokens.nextElement();

		//get the matrix type from cad type.In case multiple matrix types are mapped to
		//the same cad type, get the first mapped matrix type
		
		Vector mappedMatrixTypes = _gco.getMappedBusTypes(cadType);		
		String mxType = (String)mappedMatrixTypes.elementAt(0);
        
		revisionVersionType = labelNext+"|" + "|" + mxType;
		return revisionVersionType;
    }
}
