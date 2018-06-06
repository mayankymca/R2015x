/*
**  MCADTransferAttributesForEBOM
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  This JPO is for transferring mapped attributes between CAD object and related Part object
*/

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class MCADTransferAttributesForEBOM_mxJPO {

    MCADMxUtil _util = null;
    MCADServerGeneralUtil _generalUtil = null;
    MCADGlobalConfigObject _globalConfig = null;
    MCADServerResourceBundle _serverResourceBundle = null;
	IEFGlobalCache _cache	= null;

    //Table containing attribute Mapping for copy between Part object and CAD Model
    private Hashtable _typeAttributeMapTable = null;

    //End at which the part object lies in attribute transfer
    private final String  PART_END_FOR_ATTRIBUTE_TRANSFER = "to";

    public  MCADTransferAttributesForEBOM_mxJPO  () {

    }
    public MCADTransferAttributesForEBOM_mxJPO (Context context, String[] args) throws Exception
    {
      if (!context.isConnected())
		MCADServerException.createException("not supported no desktop client", null);

    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }

    private void init(Context context, String [] packedGCO, String sLanguage)  throws Exception
    {
        _serverResourceBundle = new MCADServerResourceBundle(sLanguage);
		_cache				  = new IEFGlobalCache();
        _util = new MCADMxUtil(context, _serverResourceBundle, _cache);
        _globalConfig = (MCADGlobalConfigObject) JPO.unpackArgs(packedGCO);
        _generalUtil = new MCADServerGeneralUtil(context,_globalConfig, _serverResourceBundle, _cache);
        String attributeOriginator = MCADMxUtil.getActualNameForAEFData(context, "attribute_Originator");

         // Initialize the attribute map table in following format
         // Key = From Type Attr Name
         // Value = Corresponding To Type Attr name
         _typeAttributeMapTable =  new java.util.Hashtable();
         _typeAttributeMapTable.put("Originator",attributeOriginator);

    }

    //entry point
    public void runTransferAttributesForEBOM(Context context,String[] args)throws Exception
    {
        String [] packedGCO    = new String[2];
        packedGCO[0] = args[0];
        packedGCO[1] = args[1];
        String partObjId = args[2];
        String busObjId = args[3];
        String instanceName = args[4];
        String sLanguage = args[5];

        init(context, packedGCO, sLanguage);

        //transfer attributes between Part and Cad object as per the specified direction
        if (PART_END_FOR_ATTRIBUTE_TRANSFER.equalsIgnoreCase("from"))
        {
            transferAttrToCADObject(context, partObjId,busObjId,false,instanceName,_typeAttributeMapTable);
        }
        else
        {
            transferAttrToCADObject(context, partObjId,busObjId,true,instanceName,_typeAttributeMapTable);
        }

    }


    //This method transfers the values of attributes between matrix object and CAD object for the passed
	//list of attributes. Direction for transfer is determined by boolean flag isBusOnFromSide. When it is true,
	//transfer is from cad object to part object and vice versa when it is false
	private void transferAttrToCADObject(Context _context, String partObjId, String busObjId,boolean isBusOnFromSide,
	                                     String instanceName,Hashtable atrMappingTable)throws Exception
    {
        String atrVal=null;
        String fromAtrName;
        String toAtrName="";
		String fromAtrDataType ="";
		String toAtrDataType ="";
		MQLCommand mql = new MQLCommand();

        try
        {
            if(instanceName == null)
                instanceName = "";

            BusinessObject fromObj = null;
            BusinessObject toObj = null;

            if(!isBusOnFromSide)
            {
                fromObj = new BusinessObject(partObjId);
                toObj	= new BusinessObject(busObjId);
            }
            else
            {
                fromObj = new BusinessObject(busObjId);
                toObj	= new BusinessObject(partObjId);
            }

			fromObj.open(_context);
			toObj.open(_context);

			//navigate the hashtable for the attributes list
			Enumeration atrEnum = atrMappingTable.keys();
			AttributeList attributelist = new AttributeList();
            while(atrEnum.hasMoreElements())
            {
				fromAtrName  = (String)atrEnum.nextElement();
				toAtrName    = (String)atrMappingTable.get(fromAtrName);

				if (fromAtrName.length()==0 ||  toAtrName.length()==0 )
					continue;
                //if from/to side attr are system attributes
                if (fromAtrName.startsWith("$$") && fromAtrName.endsWith("$$"))
					fromAtrName = fromAtrName.substring(2,fromAtrName.length()-2).trim();

                if (toAtrName.startsWith("$$") && toAtrName.endsWith("$$"))
					toAtrName = toAtrName.substring(2,toAtrName.length()-2).trim();

				//if it is Description or Owner, get its value
				if (fromAtrName.equals("Description"))
				{
					atrVal = fromObj.getDescription(_context);
				}
				else if (fromAtrName.equals("Owner"))
				{
					atrVal = (fromObj.getOwner(_context)).toString();
				}
				else
				{
					//check if the attribute exist and get the data type if it exists
					fromAtrDataType = getDatatypeOfAtrIfExist(_context,fromObj, fromAtrName);

					//if the attribute exist in from object, get its value
					if (fromAtrDataType.length() >0)
					{
						String id = fromObj.getObjectId();
						boolean bRet = mql.executeCommand(_context, "print bus $1 select $2 dump",id,"attribute["+fromAtrName+"].value");
						if (bRet)
						{
							atrVal = mql.getResult().trim();
						}
					}
                }

				// if attribute value is extracted successfully
				if (atrVal!=null)
				{
				    //if it is Description or Owner, get its value
					if (toAtrName.equals("Description"))
					{
						toObj.setDescription(atrVal);
						toObj.update(_context);
					}
					else if (toAtrName.equals("Owner"))
					{
						toObj.setOwner(atrVal);
						toObj.update(_context);
					} else {
						//check if the attribute exist and get the data type if it exists
						toAtrDataType = getDatatypeOfAtrIfExist(_context,toObj,toAtrName);

						//if data type is non empty, attribute exist
						if (toAtrDataType.length() >0 && (checkIfAtrCompatibleForTransfer(fromAtrDataType,toAtrDataType)) && (checkIfValueExistInRange(_context, toAtrName, atrVal)) )
						{
								attributelist.addElement(new Attribute(new AttributeType(toAtrName), atrVal));
						}
					}
				}
			}

			toObj.setAttributes(_context, attributelist);

			fromObj.close(_context);
			toObj.close(_context);
        }
        catch(Exception _ex)
        {
			//don't throw any exception....
        }
		return;
    }

    private String getDatatypeOfAtrIfExist(Context context,BusinessObject busObj, String atrName)throws Exception
    {
		String busType= "";
		String result ="";
		try
		{
			//get the type of the business object
			busType = busObj.getTypeName();
			MQLCommand mql = new MQLCommand();
			boolean bRet = mql.executeCommand(context,"print $1 $2 select $3 dump", "type", busType, "attribute["+atrName+"].type");
            if(bRet)
            {
                result = mql.getResult().trim();

            }
            else
            {
                MCADServerException.createException(mql.getError(), null);
            }
		}
		catch (Exception e)
		{
            //don't throw any exception
		}
		return result;
	}

	private boolean checkIfAtrCompatibleForTransfer(String fromDataType, String toDataType)throws Exception
	{
		//if the to type is string, it is compatible
		if (toDataType.equals("string"))
		{
			return true;
		}
		else if (toDataType.equals(fromDataType))// else if self it is compatible
		{
			return true;
		}
		else if (fromDataType.equals("integer")&&toDataType.equals("real") )// else if from int and to real it is compatible
		{
			return true;
		}
		else if (fromDataType.equals("real")&&toDataType.equals("integer"))//else if from int and to real it is compatible
		{
			return true;
		}
		else // else not compatible
		{
			return false;
		}
	}

	private boolean checkIfValueExistInRange(Context context, String attributeName, String atrVal)throws Exception
	{
		boolean ret = true ;
		matrix.util.StringList rangelist = null;
		
		//check if the attribnute has ranges,
		try
		{
			AttributeType attributeType = new AttributeType(attributeName);
			attributeType.open(context);
			rangelist = attributeType.getChoices();
			attributeType.close(context);
			if (rangelist!=null && rangelist.size()>0 && !(rangelist.contains(atrVal)) )//if there are ranges, check if the value passed in included
			{
				ret = false;
			}
		}
		catch (Exception e)
		{
           ret = false;

		}
		
		return ret;
	}

	public Hashtable getValidObjectAttributeMapping(Context context, String [] args) throws Exception
	{
		Hashtable returnTable				 = new Hashtable();
		Map inputArgs						 = (Map) JPO.unpackArgs(args);
		MCADGlobalConfigObject gco			 = (MCADGlobalConfigObject) inputArgs.get("GCO");
		Vector mappingList	     			 = (Vector) inputArgs.get("Mapping");
		Enumeration attributeMappingElements = mappingList.elements();

		Vector mappedTypes					 = gco.getAllMappedTypes();
		
		while(attributeMappingElements.hasMoreElements())
		{
			String mapping				   = (String) attributeMappingElements.nextElement();
			StringTokenizer mappingDetails = new StringTokenizer(mapping, "|");
			String mxCADType			   = (String)mappingDetails.nextElement();
		
			if(mappedTypes.contains(mxCADType) || !gco.getCorrespondingType(mxCADType).equals("") )
				returnTable.put(mapping, mapping);
		}

		return returnTable;
	}

	public Hashtable getValidRelAttributeMapping(Context context, String [] args) throws Exception
	{
		Hashtable returnTable				 = new Hashtable();
		Map inputArgs						 = (Map) JPO.unpackArgs(args);
		MCADGlobalConfigObject gco			 = (MCADGlobalConfigObject) inputArgs.get("GCO");
		Vector mappingList	     			 = (Vector) inputArgs.get("Mapping");
		Enumeration attributeMappingElements = mappingList.elements();
		
		Hashtable relsAndEnds =  gco.getRelationshipsOfClass(MCADAppletServletProtocol.ASSEMBLY_LIKE);
	        
		while(attributeMappingElements.hasMoreElements())
		{
			String mapping					= (String) attributeMappingElements.nextElement();
			
			StringTokenizer mappingDetails  = new StringTokenizer(mapping, "|");
			String mxRel					= (String)mappingDetails.nextElement();

			if(relsAndEnds.containsKey(mxRel))
				returnTable.put(mapping, mapping);
		}

		return returnTable;
	}

}
