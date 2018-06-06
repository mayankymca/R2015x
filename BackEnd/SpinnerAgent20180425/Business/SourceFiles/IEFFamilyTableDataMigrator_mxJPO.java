/*
 * IEFFamilyTableDataMigrator.java program to validate the IEF data model.
 *
 * Copyright (c) 1992-2008 MatrixOne, Inc.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.db.Query;
import matrix.db.QueryIterator;
import matrix.db.Relationship;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;

public class IEFFamilyTableDataMigrator_mxJPO
{
    private BufferedWriter writer                    = null;
    private FileWriter iefLog                        = null;
    private long startTime                           = 0L;

    private boolean isDebug                          = false;
    private MCADMxUtil _mxUtil                       = null;

    private String documentDirectory                 = "";
    private String  _sGCOType                        = "";
    private String  _sGCOName                        = "";

    private MCADServerGeneralUtil _serverGeneralUtil = null;
    private MCADGlobalConfigObject _gco              = null;
    private MCADServerResourceBundle _res            = null;
    private IEFGlobalCache _cache                    = null;

    public static final String ID                    = "id";
    public static final String NAME                  = "name";
    private String ATTR_CAD_TYPE                     = "";
    private String ATTR_IS_PRIMARY                   = "";
    private String ATTR_PARENT_INSTANCE              = "";
    private String SELECT_ATTR_CAD_TYPE              = "";
    private String SELECT_ATTR_IS_PRIMARY           = "";
    
    Hashtable familyLikeRelsAndEnds                  = new Hashtable();

    public IEFFamilyTableDataMigrator_mxJPO (Context context, String[] args)
        throws Exception
    {
        writer               = new BufferedWriter(new MatrixWriter(context));
        _res                 = new MCADServerResourceBundle("en-US");
        _cache               = new IEFGlobalCache();

        _mxUtil              = new MCADMxUtil(context, _res, _cache);
        
        ATTR_CAD_TYPE           = MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType");
        ATTR_IS_PRIMARY         = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IsPrimary");
        ATTR_PARENT_INSTANCE    = MCADMxUtil.getActualNameForAEFData(context, "attribute_ParentInstance");
        SELECT_ATTR_CAD_TYPE    =  "attribute[" + ATTR_CAD_TYPE + "]";
        SELECT_ATTR_IS_PRIMARY =  "attribute[" + ATTR_IS_PRIMARY + "]";
    }

    public int mxMain(Context context, String[] args)
        throws Exception
    {
        if(!context.isConnected())
        {
            throw new Exception("Not Supported on Desktop Client");
        }

        try
        {
            validateInputArguments(args);
        }
        catch (Exception iExp)
        {
            writeErrorToConsole(iExp.getMessage());
            writer.close();
            return 0;
        }

        try
        {
            startIEFLog();

            logTimeForEvent("IEF Family Data Migration START");
            startTime			  = System.currentTimeMillis();

            //Init GCO and Server Util
            _gco                  = getGlobalConfigObject(context, "");
            _serverGeneralUtil    = new MCADServerGeneralUtil(context, _gco, _res, _cache);
            familyLikeRelsAndEnds = _gco.getRelationshipsOfClass(MCADServerSettings.FAMILY_LIKE);
            
            doFamilyTableDataMigration(context);
            
            logTimeForEvent("IEF Family Data Migration COMPLETE");

            printMigrationReport();
            writeSuccessToConsole();
        }
        catch (FileNotFoundException fEx)
        {
            writeErrorToConsole("Directory does not exist or does not have access to the directory");
        }
        catch (Exception ex)
        {
            writeErrorToConsole("IEF Family Data Migration failed: " + ex.getMessage());
            ex.printStackTrace(new PrintWriter(iefLog, true));
        }

        endIEFLog();
        writer.flush();
        writer.close();
        return 0;
    }

    private void doFamilyTableDataMigration(Context context) throws Exception
    {
        HashSet inputMatrixTypes            = getRelevantMatrixTypesForQuery(context);
        StringList busSelects               = getBusSelectList();
        
        boolean isSecondLevelInstanceExists = true;
        
        String  matrixTypeListForQuery		= MCADUtil.getDelimitedStringFromCollection(inputMatrixTypes, ",");
        
        while(isSecondLevelInstanceExists)
        {
			if(isDebug)
				log("[IEFFamilyDataMigrator : doFamilyTableDataMigration] Starting Query for Processing");

            isSecondLevelInstanceExists = false;
            
            try
            {
                _mxUtil.startTransaction(context);  
                
                Query checkinQuery = new Query();
                checkinQuery.setBusinessObjectType(matrixTypeListForQuery);
                checkinQuery.setBusinessObjectName("*");
                checkinQuery.setBusinessObjectRevision("*");
                
                if(isDebug)
                    log("[IEFFamilyDataMigrator : doFamilyTableDataMigration] Matrix Type List For Query : " + matrixTypeListForQuery);

				QueryIterator queryIterator = checkinQuery.getIterator(context, busSelects, (short)1000);
				try
				{
                while(queryIterator.hasNext())
                {
                    BusinessObjectWithSelect busWithSelect = queryIterator.next();

					boolean isSecondLevelInstance		   = processResults(context, busWithSelect);
					
					if(isSecondLevelInstance)
						isSecondLevelInstanceExists = true;
                }
				}
				finally
				{
          queryIterator.close();
        }

				if(isDebug)
				{
					log("[IEFFamilyDataMigrator : doFamilyTableDataMigration] Is Second Level Instance Exists: " + isSecondLevelInstanceExists);
				}
                
				context.commit();
            }
            catch (Exception e)
            {
                if(context.isTransactionActive())
                    context.abort();
                
                if(isDebug)
                    log("[IEFFamilyDataMigrator : doFamilyTableDataMigration]Exception while querying for BusinessObjects : " + e.getMessage());
                
                MCADServerException.createException(e.getMessage(), e);
            }
        }
    }

	private boolean processResults(Context context, BusinessObjectWithSelect busWithSelect) throws Exception
	{
		boolean isSecondLevelInstance = false;

		String id   = busWithSelect.getSelectData(ID); 
		String name = busWithSelect.getSelectData(NAME); 
		
		name        = _serverGeneralUtil.getIndivisualInstanceName(name);
		
		if(isDebug)
		{
			log("[IEFFamilyDataMigrator : processResults] Instance Id	: " + id);
			log("[IEFFamilyDataMigrator : processResults] Instance name	: " + name);
		}

		Enumeration relList = familyLikeRelsAndEnds.keys();
                    
		while(relList.hasMoreElements())
		{
			String relName = (String)relList.nextElement();
			
			String relEnd = (String)familyLikeRelsAndEnds.get(relName);
			String expEnd = "";
			
			if(relEnd.equals("to"))
				expEnd = "from";
			else
				expEnd = "to";
			
			StringList childRelidList       = busWithSelect.getSelectDataList(expEnd + "[" + relName + "].id"); // child rel id
			StringList childidList          = busWithSelect.getSelectDataList(expEnd + "[" + relName + "]." + relEnd +".id"); // child instance ids
			StringList childIsPrimaryList   = busWithSelect.getSelectDataList(expEnd + "[" + relName + "]."+ SELECT_ATTR_IS_PRIMARY); // is primary on child relationship
			
			StringList familyIdList         = busWithSelect.getSelectDataList(relEnd + "[" + relName + "]." + expEnd +".id"); // parent ids
			StringList parentIsPrimaryList  = busWithSelect.getSelectDataList(relEnd + "[" + relName + "]."+ SELECT_ATTR_IS_PRIMARY); // is primary on parent relationship
			StringList familyCADTypeList    = busWithSelect.getSelectDataList(relEnd + "[" + relName + "]."+ expEnd + "." + SELECT_ATTR_CAD_TYPE); // is primary on parent relationship

			boolean isFirstLevelInstance	= true;
			
			if(familyCADTypeList != null)
			{
				isFirstLevelInstance = isFirstLevelInstance(familyCADTypeList);
				
				if(!isFirstLevelInstance)
					isSecondLevelInstance = true;
			}
			else
			{
				// might be a major instance object without family
				isFirstLevelInstance = false;
				
				if(isDebug)
					log("[IEFFamilyDataMigrator : processResults] Instance without family : " + id);
			}

			if(childRelidList != null && isFirstLevelInstance)
				processResultsForRelationship(context, name, childRelidList, childidList, childIsPrimaryList, familyIdList, parentIsPrimaryList, familyCADTypeList);
		}

		return isSecondLevelInstance;
	}

	private void processResultsForRelationship(Context context, String parentInstanceName, StringList childRelidList, StringList childidList, StringList childIsPrimaryList,StringList familyIdList, StringList parentIsPrimaryList, StringList familyCADTypeList) throws Exception
	{
		HashSet relationshipsToRemove = new HashSet(childRelidList);
                            
		for(int j = 0 ; j < childRelidList.size() ; j++)
		{
			String childrelid     = (String) childRelidList.get(j);
			String childId        = (String) childidList.get(j);
			String childIsPrimary = (String) childIsPrimaryList.get(j);

			if(isDebug)
			{
				log("[IEFFamilyDataMigrator : processResultsForRelationship] Child Instance Relationship id			: " + childrelid);
				log("[IEFFamilyDataMigrator : processResultsForRelationship] Child Instance id						: " + childId);
				log("[IEFFamilyDataMigrator : processResultsForRelationship] Child Instance Relationship is Primary : " + childIsPrimary);
			}
			
			if(familyIdList != null)
			{
				processChildInstance(context, childId, childrelid, childIsPrimary, parentInstanceName, familyIdList, parentIsPrimaryList, familyCADTypeList);
			}
			
		}

		removeAllUnwantedRelationships(context, relationshipsToRemove);
	}

	private void processChildInstance(Context context, String childInstanceId, String childInstanceRelId, String childIsPrimary, String parentInstanceName, StringList familyIdList, StringList parentIsPrimaryList, StringList familyCADTypeList) throws Exception
	{
		Relationship instanceOfRelation		 = new Relationship(childInstanceRelId);
        
		instanceOfRelation.open(context);
        
        String relationshipName				 = instanceOfRelation.getTypeName();

        String end							 = (String) familyLikeRelsAndEnds.get(relationshipName);
        
        AttributeList relationshipAttributes = instanceOfRelation.getAttributes(context);
        
	    instanceOfRelation.close(context);
    
		for(int i = 0 ; i < familyIdList.size() ; i++)
		{
			String familyID        = (String) familyIdList.get(i);
			String parentIsPrimary = (String) parentIsPrimaryList.get(i);
			String familyCADType   = (String) familyCADTypeList.get(i);
			
			if(isDebug)
			{
				log("[IEFFamilyDataMigrator : processChildInstance] Family id						: " + familyID);
				log("[IEFFamilyDataMigrator : processChildInstance] Family Relationship is Primary  : " + parentIsPrimary);
				log("[IEFFamilyDataMigrator : processChildInstance] Family CAD Type				    : " + familyCADType);
			}

			String isPrimary = "FALSE";
			
			if(_gco.isTypeOfClass(familyCADType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
			{
				if(parentIsPrimary.equalsIgnoreCase("true") && childIsPrimary.equalsIgnoreCase("true"))
					isPrimary = "TRUE";
				
				AttributeList newAttributeList       = new AttributeList();
		        newAttributeList.addAll(relationshipAttributes);
				newAttributeList.addElement(new Attribute(new AttributeType(ATTR_IS_PRIMARY), isPrimary));
		        newAttributeList.addElement(new Attribute(new AttributeType(ATTR_PARENT_INSTANCE), parentInstanceName));

				moveRelationshipAndUpdateAttributes(context, familyID, childInstanceId, relationshipName, end, newAttributeList);
    		}
		}
	}
    
	
    private void moveRelationshipAndUpdateAttributes(Context context, String familyId, String instanceId, String relationshipName, String end, AttributeList newAttributeList) throws Exception
    {
        Relationship newRelationship = _mxUtil.ConnectBusinessObjects(context, new BusinessObject(familyId), new BusinessObject(instanceId), relationshipName, end.equalsIgnoreCase("to"));
        newRelationship.setAttributes(context, newAttributeList);
        newRelationship.update(context);
    }

	private boolean isFirstLevelInstance(StringList familyCADTypeList)
	{
		boolean isFirstLevelInstance = true;
		
		for(int i = 0 ; i < familyCADTypeList.size() ; i++)
		{
			String familyCADType			= (String) familyCADTypeList.get(i);

			if(!_gco.isTypeOfClass(familyCADType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE))
			{
				isFirstLevelInstance		= false;
				
				// only if it is a pure first level instance go ahead and transfer relationships
				if(isDebug)
					log("[IEFFamilyDataMigrator : isFirstLevelInstance] Family CAD Type : " + familyCADType);

				break;
			}
		}
		
		return isFirstLevelInstance;
	}

	private void removeAllUnwantedRelationships(Context context, HashSet relationshipsToRemove) throws Exception
    {
        Iterator relItr = relationshipsToRemove.iterator();
        while(relItr.hasNext())
        {
            String relId = (String) relItr.next();
            
            log("[IEFFamilyDataMigrator : removeAllUnwantedRelationships] Removing relationship : " + relId);
            
            Relationship instanceOfRelation = new Relationship(relId);
            instanceOfRelation.open(context);
            
            instanceOfRelation.remove(context);
            
            instanceOfRelation.close(context);
        }
    }
    
    private StringList getBusSelectList()
    {
        StringList selectList = new StringList();
        selectList.add(ID);
        selectList.add(NAME);
        
        Enumeration relList       = familyLikeRelsAndEnds.keys();
        
        while(relList.hasMoreElements())
        {
            String relName = (String)relList.nextElement();
            
            if(isDebug)
                log("[IEFFamilyDataMigrator : getBusSelectList] Relationship name For select : " + relName);
            
            String relEnd = (String)familyLikeRelsAndEnds.get(relName);
            String expEnd = "";
            
            if(isDebug)
                log("[IEFFamilyDataMigrator : getBusSelectList] Relationship relEnd For select : " + relEnd);
            
            if(relEnd.equals("to"))
                expEnd = "from";
            else
                expEnd = "to";
            
            selectList.addElement(expEnd + "[" + relName + "].id"); // child rel id
            selectList.addElement(expEnd + "[" + relName + "]." + relEnd +".id"); // child object id
            selectList.addElement(relEnd + "[" + relName + "]." + expEnd +".id"); // family object id
            selectList.addElement(relEnd + "[" + relName + "]."+ SELECT_ATTR_IS_PRIMARY); // is primary on parent relationship
            selectList.addElement(expEnd + "[" + relName + "]."+ SELECT_ATTR_IS_PRIMARY); // is primary on child relationship
            selectList.addElement(relEnd + "[" + relName + "]."+ expEnd + "." + SELECT_ATTR_CAD_TYPE); // family cad type
        }
        
        return selectList;
    }
    
    private HashSet getRelevantMatrixTypesForQuery(Context context) throws MCADException
    {
        HashSet inputMatrixTypes = new HashSet(50);
        
        Vector instanceLikeClass   = _gco.getTypeListForClass(MCADAppletServletProtocol.TYPE_INSTANCE_LIKE);
        
        ArrayList inputCADTypeList = new ArrayList(instanceLikeClass);
        
        for(int i = 0; i < inputCADTypeList.size(); i++)
        {
            Vector matrixTypes = getMappedBusTypesForCADType((String)inputCADTypeList.get(i));
            
            for(int j = 0; j < matrixTypes.size(); j++)
            {
                String majorType = (String) matrixTypes.get(j);
                inputMatrixTypes.add(majorType);
                String minorType = _mxUtil.getCorrespondingType(context, majorType);
                
                if(minorType != null && !minorType.equals(""))
                    inputMatrixTypes.add(minorType);
            }
        }
        
        return inputMatrixTypes;
    }
    
    private Vector getMappedBusTypesForCADType(String cadType) throws MCADException
    {
        Vector rawMappedTypes = _gco.getMappedBusTypes(cadType);
        
        if(rawMappedTypes == null)
        {
            String errorMessage = _res.getString("mcadIntegration.Server.Message.ProblemsWithGlobalConfigObject");
            MCADServerException.createException(errorMessage, null);
        }

        Vector mappedBusTypes = MCADUtil.getListOfActualTypes(rawMappedTypes);


        return mappedBusTypes;
    }

    private void printMigrationReport() throws Exception
    {
        writer.flush();
    }

    private MCADGlobalConfigObject getGlobalConfigObject(Context context, String integrationName) throws Exception
    {
        if(_gco == null)
        {
            MCADConfigObjectLoader configLoader = new MCADConfigObjectLoader(null);
            _gco = configLoader.createGlobalConfigObject(context, _mxUtil, _sGCOType, _sGCOName);
        }

        return _gco;
    }

    private void validateInputArguments(String[] args) throws Exception
    {
        if (args.length < 3 )
            throw new IllegalArgumentException("Wrong number of arguments. Usage <IEFFamilDataMigrator GCOType GCOName LogDirectory>");

        _sGCOType = args[0];
        _sGCOName = args[1];
        documentDirectory = args[2];
        
        if (args.length == 4 )
        {
            String isDebugEnabled = args[3];
            isDebug               = isDebugEnabled.equalsIgnoreCase("true");
        }
        
        String fileSeparator = java.io.File.separator;
        if(documentDirectory != null && !documentDirectory.endsWith(fileSeparator))
            documentDirectory = documentDirectory + fileSeparator;
    }

    
    private void writeLineToConsole() throws Exception
    {
        writeMessageToConsole("=======================================================");
    }

    private void writeMessageToConsole(String message) throws Exception
    {
        writer.write(message + "\n");
    }

    private void writeErrorToConsole(String message) throws Exception
    {
        writeLineToConsole();
        writeMessageToConsole(message);
        writeMessageToConsole("IEF Family Data Migration     : FAILED");
        writeLineToConsole();
        writer.flush();
    }

    private void writeSuccessToConsole() throws Exception
    {
        writeLineToConsole();
        writeMessageToConsole("                IEF Family Data Migration is COMPLETE");
        writeMessageToConsole("                Time : "+ (System.currentTimeMillis() - startTime) + "ms ");
        writeMessageToConsole("                IEF Family Data Migration     : SUCCESS");
        writeLineToConsole();
        writer.flush();
    }

    private void startIEFLog() throws Exception
    {
        try
        {
            iefLog  = new FileWriter(documentDirectory + "iefFamilyTableDataMigratorFlat.log");
        }
        catch(Exception e)
        {
            writeMessageToConsole("ERROR: Can not create log file. " + e.getMessage());
        }
    }

    private void endIEFLog()
    {
        try
        {
            iefLog.write("\n\n");
            iefLog.flush();
            iefLog.close();
        }
        catch(Exception e)
        {
        }
    }

    private void log(String message)
    {
        try
        {
            if(isDebug)
                iefLog.write(message + "\n");
        }
        catch(Exception e)
        {
        }
    }

    private void logTimeForEvent(String event)
    {
        log("\n\n" + event + " Time: " + System.currentTimeMillis() + "\n\n");
    }
}

