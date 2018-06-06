/*
**  MCADGetTypesForTypeChooser
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  This jpo is for getting the types that are to be displayed in TypeChooser
*/

import java.util.Vector;

import matrix.db.BusinessType;
import matrix.db.BusinessTypeItr;
import matrix.db.BusinessTypeList;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class MCADGetTypesForTypeChooser_mxJPO
{
 private MCADGlobalConfigObject _globalConfig			= null;
    private MCADMxUtil _util								= null;
	private MCADServerResourceBundle _serverResourceBundle  = null;
	private IEFGlobalCache	_cache							= null;
	
	private Vector _rootTypes								= null;
	private Vector _mappedTypes								= null;

	public MCADGetTypesForTypeChooser_mxJPO()
    {
    }
    
	public MCADGetTypesForTypeChooser_mxJPO (Context context, String[] args) throws Exception
    {
      if (!context.isConnected())
	      MCADServerException.createException("not supported no desktop client", null);
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }
	
	//init method
	private void intialize(Context context, String[] packedGCO, String language)  throws Exception
    {
        _serverResourceBundle			= new MCADServerResourceBundle(language);
		_cache							= new IEFGlobalCache();
        _util							= new MCADMxUtil(context, _serverResourceBundle, _cache);
        _globalConfig				= (MCADGlobalConfigObject) JPO.unpackArgs(packedGCO);
		_rootTypes					= new Vector();

		//populate _rootTypes
		_rootTypes.addElement(MCADMxUtil.getActualNameForAEFData(context, "type_MCADModel"));
		_rootTypes.addElement(MCADMxUtil.getActualNameForAEFData(context, "type_MCADDrawing"));
		_rootTypes.addElement(MCADMxUtil.getActualNameForAEFData(context, "type_MCADVersionedDrawing"));
    }

	/**
	 * Entry Point
	 * This method returns a string containing the list of types to
	 * be displayed in TypeChooser
	 */
	public String getTypesForTypeChooser(Context context, String[] args)throws Exception
	{
		String typesList = "";
		Vector unfilteredTypeList = new Vector();

		String[] packedGCO = new String[2];
		packedGCO[0] = args[0];
		packedGCO[1] = args[1];
		String language = args[2];

		//initialize
		intialize(context, packedGCO, language);

		//get all the mapped types from bust-type mapping in GCO
		_mappedTypes = _globalConfig.getAllMappedTypes();
		
		if(_mappedTypes != null)
		{
			int size = _mappedTypes.size();
			for(int i = 0;i < size;i++)
			{
                //Unhide versioned types and dont show in type chooser
				String versionedType = "";
                try
                {
					//mapped types in GCO are major types
					String mappedMajorType = (String)_mappedTypes.elementAt(i);
					//versionedType = _util.getCorrespondingType(mappedMajorType);
					//boolean bVersionedTypeHidden = isVersionedTypeHidden(versionedType);
	
					unfilteredTypeList.addElement(mappedMajorType);
	
					//add the versioned types to the list if it is not "hidden"
					//if(!bVersionedTypeHidden)
					//{
					//	unfilteredTypeList.addElement(versionedType);
					//}
                }
                catch(Exception e)
                {
                	System.out.println("WARNING: Type '" + versionedType + "' is not registered");
                }

			}
			
			typesList = getFilteredTypeList(context, unfilteredTypeList);
		}

		return typesList;
	}

	private String getFilteredTypeList(Context context, Vector unfilteredTypeList)throws Exception
	{
		Vector duplicateList = new Vector();

		for(int i=0; i<unfilteredTypeList.size(); i++)
		{
			String typeName = (String)unfilteredTypeList.elementAt(i);
			BusinessType currBusType = new BusinessType(typeName, context.getVault());
			findDuplicateChildren(context, currBusType, unfilteredTypeList, duplicateList);
		}

		StringBuffer typesListBuffer = new StringBuffer();
		for(int i=0; i<unfilteredTypeList.size(); i++)
		{
			String typeName = (String)unfilteredTypeList.elementAt(i);
			if(!duplicateList.contains(typeName))
			{
				typesListBuffer.append(_util.getSymbolicName(context, "type", typeName));
				typesListBuffer.append(",");
			}
		}
		return typesListBuffer.toString();
	}

	private void findDuplicateChildren(Context context, BusinessType currType, Vector allTypeList, Vector duplicateList) throws Exception
	{
		BusinessTypeList childrenList = currType.getChildren(context);
		if(childrenList != null)
		{
			BusinessTypeItr itr = new BusinessTypeItr(childrenList);
			while(itr.next())
			{
				BusinessType childType = itr.obj();
				if(allTypeList.contains(childType.getName()))
					duplicateList.addElement(childType.getName());
				findDuplicateChildren(context, childType, allTypeList, duplicateList);
			}
		}
	}
}

