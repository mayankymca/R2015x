/*
 * DECProESRepAttrbMigration.java program.
 *
**  Copyright Dassault Systemes, 1992-2009.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
 *
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.MQLCommand;
import matrix.db.MatrixWriter;
import matrix.db.Query;
import matrix.db.QueryIterator;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.PolicyUtil;
import com.matrixone.apps.domain.util.PropertyUtil;


public class DECProESRepAttrbMigration_mxJPO {

	private BufferedWriter writer				= null;
	private FileWriter iefLog					= null;
	private FileWriter migrationCommandsFile 	= null;
	private FileWriter failedIDsFile			= null;
	
	private MCADMxUtil mxUtil					= null;
	private String documentDirectory			= "";

	private String attrProERelFeatureId			= "";
	private String attrProESRepExcludedRelList	= "";
	private String simpRepAttrName				= "";
	private HashMap policyFinStateTable			= null;

	private String gcoNames				 		= "";


	public DECProESRepAttrbMigration_mxJPO (Context context, String[] args)throws Exception
	{
		writer = new BufferedWriter(new MatrixWriter(context));		

		mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle("en-US"), new IEFGlobalCache());
		
		policyFinStateTable = new HashMap();

		attrProERelFeatureId		= (String)PropertyUtil.getSchemaProperty(context, "attribute_ProERelFeatureID");
		attrProESRepExcludedRelList = (String)PropertyUtil.getSchemaProperty(context, "attribute_ProES-RepExcludedRelList");
		simpRepAttrName				= (String)PropertyUtil.getSchemaProperty(context, "attribute_ProESimplifiedReps");
	}

	public int mxMain(Context context, String[] args)
	throws Exception
	{		
		if(!context.isConnected())
		{
			throw new Exception("not supported on desktop client");
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
			if(args[0].equalsIgnoreCase("writeCommand"))
				writeMigrationCommands(context, args);
			else if(args[0].equalsIgnoreCase("migrate"))
				migrateSRepData(context, args);
		}
		catch (FileNotFoundException fEx)
		{			
			String message = "Directory '" + documentDirectory + "' does not exist or user does not have access permission to the directory";
			writeErrorToConsole(message);			
			context.abort();
		}
		catch (Exception ex)
		{
			writeErrorToConsole("ProE S-Rep Attribute Migration failed : " + ex.getMessage());
			ex.printStackTrace(new PrintWriter(iefLog, true));
			context.abort();
		}

		endIEFLog();		
		writer.flush();
		writer.close();
		return 0;
	}

	private void validateInputArguments(String[] args) throws IllegalArgumentException
	{
		String expectedUsageStep1 = "\n For Step1 : writeCommand <document directory> <GCO Name>"; 
		String expectedUsageStep2 = "\n For Step2 : migrate <same document directory> ProE_SRep_Migration_Commands.txt <transaction option true or false>";
		
		if (args.length < 3 && args[0].startsWith("write"))
		{
			throw new IllegalArgumentException("Wrong number of arguments. Expected usage is " + expectedUsageStep1);
		}
		else if ((args[0].startsWith("migrate") && args.length <= 3))
		{		
			throw new IllegalArgumentException("Wrong number of arguments. Expected usage is " + expectedUsageStep2);
		}

		documentDirectory = args[1];
		// documentDirectory does not ends with "/" add it
		String fileSeparator = java.io.File.separator;
		if(documentDirectory != null && !documentDirectory.endsWith(fileSeparator))
		{
			documentDirectory = documentDirectory + fileSeparator;
		}
	}

	private void writeMigrationCommands(Context context, String[] args) throws Exception
	{
		startIEFLog("decSRepMigration_V6R2011_STEP1.log");
		long startTime = System.currentTimeMillis();

		writeLineToConsole();
		writeMessageToConsole("                Writing Migration Commands for ProE Objects STARTS");
		writeMessageToConsole("                Writing files to: " + documentDirectory);
		writeLineToConsole();

		log("@@@@@@@@@@@   Writing commands to file started : STEP1   @@@@@@@@@@@@@@@");
		context.start(false);		

		createCommandsFile();

		writeCommandToFile(context, args);

		closeCommandsFile();

		context.commit();
		log("@@@@@@@@@@@   Writing commands to file finished : STEP1   @@@@@@@@@@@@@@@");
		writeLineToConsole();
		writeMessageToConsole("                Writing Migration Commands for ProE Objects  COMPLETE");
		writeMessageToConsole("                Time:"+ (System.currentTimeMillis() - startTime) + "ms ");
		writeMessageToConsole("                Step 1 of Migration         :  SUCCESS");
		writeLineToConsole();
	}


	private void migrateSRepData(Context context, String[] args)throws Exception
	{
		startIEFLog("decSRepMigration_V6R2011_STEP2.log");
		startFailedIDsFile("failedIDs.log");
		String fileName = args[2];
		boolean transactionMode = true;
		try
		{
			String tansaction = args[3];
			if(tansaction.equalsIgnoreCase("false"))
			{
				transactionMode = false;
			}
		}
		catch (Exception e)
		{
			transactionMode = true;
		}
		
		long startTime = System.currentTimeMillis();

		writeLineToConsole();
		writeMessageToConsole("                Updating objects and relationships...");
		writeMessageToConsole("                Writing log files to: " + documentDirectory);
		writeLineToConsole();
		writer.write("Reading command file: " + fileName + "\n");
		log("@@@@@@@@@@@   Executing commands from " + fileName + " started : STEP2   @@@@@@@@@@@@@@@");
		
		if(transactionMode)
			context.start(true);

		String fileNameWithPath		= documentDirectory + fileName;
		BufferedReader iefIDsReader = new BufferedReader(new FileReader(fileNameWithPath));
		
		ArrayList lines = null; //
		
		boolean gotErrors = false;
		String line = "";
		while((line = iefIDsReader.readLine()) != null)
		{
			try
			{
				if(transactionMode)
				{
					if(line.length() <3 || line.startsWith("#"))
						continue;
					log("Executing: " + line);
					runModifyMQL(context, line);
				}
				else
				{
					/* if transaction is false then read lines between ### and then add it to a list.
					// And limit the transaction to that list. Lines in the list are interdependendent hence some line is failed then
					// we have roll back transation for that list only. If it fails then list would be written into failedIDs file
					// so that user can execute that file later. */
					
					if(line.length() <3  || line.startsWith("#"))
						continue;
					
					try
					{
						if(line.startsWith("###"))
						{
							lines = new ArrayList();
							while(!(line = iefIDsReader.readLine()).startsWith("###"))
							{
								//read file between ### and then add it to a list
								lines.add(line);
							}
							
							context.start(true);

							String tempLine = "";
							boolean hasErrors = false;
							for(int i=0; i < lines.size(); i++)
							{
								tempLine = (String) lines.get(i);
								log("Executing: " + tempLine);
								try
								{
									runModifyMQL(context, tempLine);									
								}
								catch (Exception e)
								{
									log("Error occured at line : " + tempLine);									
									log("ERROR : " + e.getMessage());
									e.printStackTrace(new PrintWriter(iefLog, true));
									
									hasErrors = true;
									
									failedIDsFile.write("##################");
									failedIDsFile.write("\n");
									for(int j=0; j < lines.size(); j++)
									{
										failedIDsFile.write((String) lines.get(j));
										failedIDsFile.write("\n");
									}
									failedIDsFile.write("##################");
									failedIDsFile.write("\n");
									break;
								}
							}
							
							if(hasErrors)
							{
								context.abort();
							}
							else
							{
								context.commit();
							}							
						}
					}
					catch(Exception e)
					{
						context.abort();
					}
				}
			}
			catch(Exception e)
			{
				gotErrors = true;
				log("ERROR : " + e.getMessage());
			}
		}

		if(gotErrors)
			throw new Exception("Error in Migration. Review log file for details.");
		
		if(transactionMode)
			context.commit();
		
		log("@@@@@@@@@@@   Executing commands from " + fileName + " finished : STEP2   @@@@@@@@@@@@@@@");
		writeLineToConsole();
		writeMessageToConsole("                Object and Relationship migration  COMPLETE");
		writeMessageToConsole("                Time:"+ (System.currentTimeMillis() - startTime) + "ms ");
		writeMessageToConsole("                Step 2 of Migration         :  SUCCESS");
		writeMessageToConsole(" ");
		writeLineToConsole();

	}

	private void runModifyMQL(Context context, String command) throws Exception
	{
		MQLCommand mqlCommand = new MQLCommand();
		boolean success = mqlCommand.executeCommand(context, command);
		if(!success)
			throw new Exception(mqlCommand.getError());
	}

	private void writeCommandToFile(Context context, String[] args)throws Exception
	{		
		gcoNames = args[2];
		
		String assemblyTypes = getTypeNames(context);

		ArrayList majorIdsList = new ArrayList();

		log("Searching bus ids for assemblyTypes : " + assemblyTypes);
		
		Query query = new Query();
		query.setBusinessObjectType(assemblyTypes);
		query.setBusinessObjectName("*");
		query.setBusinessObjectRevision("*");
		
		StringList busSelects = new StringList();
		busSelects.add(DomainConstants.SELECT_ID);
		busSelects.add("attribute[" + attrProESRepExcludedRelList + "].value");
		busSelects.add("attribute[" + simpRepAttrName + "].value");
		
		QueryIterator queryIterator = query.getIterator(context, busSelects, (short)1000);
		
		try
		{	
			while(queryIterator.hasNext()) 
			{
				BusinessObjectWithSelect busWithSelect = queryIterator.next();

				String busId 		 					= busWithSelect.getSelectData(DomainConstants.SELECT_ID); 
				String attrProESRepExcludedRelListValue = busWithSelect.getSelectData("attribute[" + attrProESRepExcludedRelList + "].value");
				String attrProESimpRepValue 			= busWithSelect.getSelectData("attribute[" + simpRepAttrName + "].value");
				
				//Filter on already migrated objects or objets which do not require migration
				if(null != attrProESimpRepValue && !"".equalsIgnoreCase(attrProESimpRepValue)
						&& (null == attrProESRepExcludedRelListValue || "".equalsIgnoreCase(attrProESRepExcludedRelListValue)))
				{
					majorIdsList.add(busId);
				}
			}
		}
		catch(Exception e)
		{
			log("ERROR : Error in querying the database for objects");
		}
		finally
		{
			queryIterator.close();
		}
		
		if(!majorIdsList.isEmpty())
		{
			//Get all minor objects corrresponds to this major id
			HashMap majorIdVersionObjectDetailsMap = getVersionObjectInformation(context, majorIdsList);
			
			Set keySet = majorIdVersionObjectDetailsMap.keySet();
			Iterator keys = keySet.iterator();
			while(keys.hasNext())
			{
				writeToCommandsFile("###################");
				Hashtable repNameExcludedRelMap = new Hashtable();
				String majorID = (String)keys.next();
				HashMap versionObjectDetails = (HashMap)majorIdVersionObjectDetailsMap.get(majorID);
				
				boolean isFinalized = isFinalized(context, majorID);
				
				if(null != versionObjectDetails && !versionObjectDetails.isEmpty())
				{
					log("!!!~~~ =============================== ~~~!!!");					
					String attrExcludedRelListValueOnMajor = "";
					
					
					Set minorIdsSet = versionObjectDetails.keySet();
					Iterator minorIds = minorIdsSet.iterator();
					while(minorIds.hasNext())
					{
						String busID = (String)minorIds.next();					
						log("Writing command for bus : " + busID);
						String typeName = (String)versionObjectDetails.get(busID);
						String attrExcludedRelListValue = "";
						String boAttrSRepValue = mxUtil.getAttributeForBO(context, busID, simpRepAttrName);
						log("Simplified Rep Value on bus : " + boAttrSRepValue);			
						Vector attrSRepValueList = getList(typeName, boAttrSRepValue, ",");
			
						String Args[] = new String[4];
						Args[0] = busID;
						Args[1] = "id";
						Args[2] = "((name == const\"CAD SubComponent\") && (from.id == " + busID + "))";
						Args[3]= "|";
						String expandResult = executeMQL(context, "expand bus $1 terse select rel $2 where $3 dump $4", Args);
			
						StringTokenizer expandToken = new StringTokenizer(expandResult, "\n");			
						while(expandToken.hasMoreElements())
						{
							String row = expandToken.nextToken();
			
							StringTokenizer rowElements = new StringTokenizer(row,"|");
							String level			= rowElements.nextToken();
							String relName			= rowElements.nextToken();
							String direction		= rowElements.nextToken();
							String childObjectId	= rowElements.nextToken();
							String relId			= rowElements.nextToken();
			
							log("===========================");
							log("Writing command for relationship : " + relId);
			
							String uuid = getUUID(context);
							log("Feature ID Value to be updated on relationship : " + uuid);
			
							String modifyFeatureIDCommand = "modify connection " + relId + " \"" + attrProERelFeatureId + "\" " + "\"" + uuid + "\"";
							writeToCommandsFile(modifyFeatureIDCommand);
							
							
							if(isFinalized)
							{
								//if parentMajor is finalized then update same uuid on its relationship with child
								String Args1[] = new String[4];
								Args1[0] = majorID;
								Args1[1] = "id";
								Args1[2] = "((name == const\"CAD SubComponent\") && (to.id == " + childObjectId + "))";
								Args1[3]= "|";
								String mExpandResult = executeMQL(context, "expand bus $1 terse select rel $2 where $3 dump $4", Args1);
								
								StringTokenizer mExpandToken = new StringTokenizer(mExpandResult, "\n");			
								while(mExpandToken.hasMoreElements())
								{
									String mRow = mExpandToken.nextToken();
					
									StringTokenizer mRowElements = new StringTokenizer(mRow,"|");
									mRowElements.nextToken();
									mRowElements.nextToken();
									mRowElements.nextToken();
									mRowElements.nextToken();
									String relIdWithMajor = mRowElements.nextToken();
									
									String modifyFeatureIDCommandOnMajor = "modify connection " + relIdWithMajor + " \"" + attrProERelFeatureId + "\" " + "\"" + uuid + "\"";
									writeToCommandsFile(modifyFeatureIDCommandOnMajor);
								}
							}
			
							String Args1[] = new String[2];
							Args1[0] = relId;
							Args1[1] = "attribute[" + simpRepAttrName + "].value";
							String relAttrValue = executeMQL(context, "print connection $1 select $2 dump", Args1);
							Vector relAttrValueList = getList(null, relAttrValue, ",");
							
							for (int k=0; k < relAttrValueList.size(); k++)
							{
								String sRepValueOnRel = (String)relAttrValueList.get(k);
								StringTokenizer tokens = new StringTokenizer(sRepValueOnRel, "|");
								int noOfTokens = tokens.countTokens();
								if(noOfTokens != 3)
								{
									log("$$ Currupted Value '" + sRepValueOnRel + "' of attribute '" + simpRepAttrName + "' on connection '" + relId + " $$");
								}									
							}
							
							log("Simplified Rep Value on relationship : " + relAttrValue);
			
							for (int i=0; i < attrSRepValueList.size(); i++)
							{
								//value consists on type|name|repName derived from object.
								String value 	= (String)attrSRepValueList.get(i);
								String repName 	= value.substring(value.lastIndexOf("|") + 1);
			
								if(relAttrValueList.contains(value))
								{
									if(!repNameExcludedRelMap.containsKey(repName))
									{
										repNameExcludedRelMap.put(repName, "");
									}
								}
								else
								{
									String excludedRelList = uuid + ",";                		
									if(repNameExcludedRelMap.containsKey(repName))
									{
										String excludedRelListOld = (String)repNameExcludedRelMap.get(repName);
			
										if(null != excludedRelList && !"".equals(excludedRelList))
										{
											excludedRelList += excludedRelListOld;
										}                			
									}
			
									repNameExcludedRelMap.put(repName, excludedRelList);
								}
							}
							log("===========================");                
						}
			
						attrExcludedRelListValue = getStringFromMap(repNameExcludedRelMap, "|", "@");
						log("ProE S-Rep Excluded Rel List Value to be updated on bus : " + attrExcludedRelListValue);
			
						String modifyExcludedRelAttr = "modify bus " + busID + " \"" + attrProESRepExcludedRelList + "\" \"" + attrExcludedRelListValue + "\"";
						writeToCommandsFile(modifyExcludedRelAttr);
						
//						now copy "ProE S-Rep Excluded Rel" from active minor to major
						String Args1[] = new String[2];
						Args1[0] = majorID;
						Args1[1] = "from[Active Version].to.id";
						String result = executeMQL(context, "print bus $1 select $2 dump", Args1);
						
						if(null != result && busID.equals(result))
						{
							attrExcludedRelListValueOnMajor = attrExcludedRelListValue;
						}
						
					}
					
					String modifyExcludedRelAttrOnMajor = "modify bus " + majorID + " \"" + attrProESRepExcludedRelList + "\" \"" + attrExcludedRelListValueOnMajor + "\"";
					writeToCommandsFile(modifyExcludedRelAttrOnMajor);
					
					writeToCommandsFile("###################");					
					log("!!!~~~ =============================== ~~~!!!");
					log("\n");
				}
			}
		}
		else
		{
			log("!!!~~~ =============================== ~~~!!!");
			log("All Objects are already migrated !");
			log("!!!~~~ =============================== ~~~!!!");
			
			writeMessageToConsole("                All Objects are already migrated !");
		}
	}
	
	private HashMap getVersionObjectInformation(Context context, ArrayList majorIdsList) throws Exception
	{
		HashMap minorObjectDetailsMap = new HashMap();

		String[] oids = new String[majorIdsList.size()];
		for (int i = 0; i < majorIdsList.size(); i++)
		{
			oids[i] = (String)majorIdsList.get(i);
		}
		
		String actualVersionOfRelName  = MCADMxUtil.getActualNameForAEFData(context,"relationship_VersionOf");

		String SELECT_EXPRESSION_VERSION_ID 	= "to[" + actualVersionOfRelName + "].from.id";
		String SELECT_EXPRESSION_VERSION_TYPE 	= "to[" + actualVersionOfRelName + "].from.type";
		String SELECT_EXPRESSION_VERSION_NAME 	= "to[" + actualVersionOfRelName + "].from.name";
		String SELECT_EXPRESSION_VERSION_REV 	= "to[" + actualVersionOfRelName + "].from.revision";

		StringList busSelectionList = new StringList();
		busSelectionList.add(DomainConstants.SELECT_ID);
		busSelectionList.add(SELECT_EXPRESSION_VERSION_ID);
		busSelectionList.add(SELECT_EXPRESSION_VERSION_TYPE);
		busSelectionList.add(SELECT_EXPRESSION_VERSION_NAME);
		busSelectionList.add(SELECT_EXPRESSION_VERSION_REV);

		BusinessObjectWithSelectList busWithSelectionList 	= 	BusinessObject.getSelectBusinessObjectData(context, oids, busSelectionList);
		for(int i=0; i<busWithSelectionList.size(); i++)
		{
			BusinessObjectWithSelect busObjectWithSelect  =  (BusinessObjectWithSelect)busWithSelectionList.get(i);

			String majorID =  busObjectWithSelect.getSelectData(DomainConstants.SELECT_ID);

			StringList versionObjectIds 	 =	busObjectWithSelect.getSelectDataList(SELECT_EXPRESSION_VERSION_ID);
			StringList versionObjectTypes 	 =	busObjectWithSelect.getSelectDataList(SELECT_EXPRESSION_VERSION_TYPE);
			StringList versionObjectNames 	 =	busObjectWithSelect.getSelectDataList(SELECT_EXPRESSION_VERSION_NAME);

			for(int k=versionObjectIds.size()-1; k>=0; k--)
			{
				HashMap objBasicDetailsMap	= new HashMap();
				
				String versionId = (String)versionObjectIds.elementAt(k);				
				String minorType = (String)versionObjectTypes.elementAt(k);
				String minorName = (String)versionObjectNames.elementAt(k);
				
				String typeName = minorType + "|" + minorName;

				objBasicDetailsMap.put(versionId, typeName);
				minorObjectDetailsMap.put(majorID, objBasicDetailsMap);
			}
		}		
		
		return minorObjectDetailsMap;
	}
	
	private boolean isFinalized(Context context, String busId) throws Exception
	{
		boolean isFinalized = false;
		
		String Args[] = new String[1];
		Args[0] = busId;
		String policy = executeMQL(context, "print bus $1 select policy dump", Args);
		
		String finalizationState = (String)policyFinStateTable.get(policy);
		
		if(null != finalizationState && !"".equals(finalizationState))
		{		
			isFinalized = PolicyUtil.checkState(context, busId, finalizationState, PolicyUtil.GE);
		}
		
		return isFinalized;
	}

	private String getTypeNames(Context context) throws Exception
	{
		String typeList = "";
		
		if(null == gcoNames || "".equals(gcoNames.trim()))
		{
			writeMessageToConsole("No GCO found with name: " + gcoNames);
			throw new Exception("No GCO found with name: " + gcoNames);
		}

		StringTokenizer gcoToken = new StringTokenizer(gcoNames, ",");
		mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle("en-US"), new IEFGlobalCache());
		while(gcoToken.hasMoreElements())
		{
			String gcoName = (String) gcoToken.nextElement();
			BusinessObject gcoObject = getGCOObjectFromName(context, gcoName);
			if(gcoObject != null)
			{
				String typeClassMapping	= gcoObject.getAttributeValues(context, MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-TypeClassMapping")).getValue();
				String busTypeMapping	= gcoObject.getAttributeValues(context, MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-BusTypeMapping")).getValue();
				String finState			= gcoObject.getAttributeValues(context, MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-FinalizationState")).getValue();
				
				if(finState.indexOf("|") > -1)
				{
					String policy 	= finState.substring(0, finState.indexOf("|"));
					String state 	= finState.substring(finState.indexOf("|") + 1);
					policyFinStateTable.put(policy, state);
				}

				HashSet assemblyClassList = getValueFromMapping(typeClassMapping, "TYPE_ASSEMBLY_LIKE");
				Iterator itr1 = assemblyClassList.iterator();
				while(itr1.hasNext())
				{
					String cadType = (String) itr1.next();
					//HashSet is used to avoid duplicate types across GCOs.
					HashSet mappedTypes = getValueFromMapping(busTypeMapping, cadType);
					Iterator itr2 = mappedTypes.iterator();					
					while(itr2.hasNext())
					{
						String mxType	 = (String) itr2.next();
						//String minorType = mxUtil.getCorrespondingType(context, mxType);						
						//typeList += mxType + "," + minorType + ",";
						//work only on Majors
						typeList += mxType + ",";
					}
				}
				typeList = typeList.substring(0, typeList.lastIndexOf(","));
			}
			else
			{
				writeMessageToConsole("No GCO found with name: " + gcoName);
				throw new Exception("No GCO found with name: " + gcoName);
			}
		}
		return typeList;
	}

	public String getUUID(Context context) throws Exception
	{
		String UUID = null;
		String mqlResult 	= "";

		String Args[] = new String[9];
		Args[0] = "eServicecommonNumberGenerator.tcl";
		Args[1] = "type_CADModel";
		Args[2] = "A Size";
		Args[3] = "";
		Args[4] = "NULL";
		Args[5] = "";
		Args[6] = "_";
		Args[7] = "";
		Args[8] = "Yes";

		mqlResult = mxUtil.executeMQL(context,"execute program $1 $2 $3 $4 $5 $6 $7 $8 $9" , Args);

		if(mqlResult.startsWith("true"))
		{				
			String result = mqlResult.substring(5);

			StringTokenizer tokenizer = new StringTokenizer(result, "|");
			tokenizer.nextElement(); 				
			UUID = (String)tokenizer.nextElement();
		}
		else
		{
			UUID = new Double(Math.random()).toString();
		}

		return UUID;
	}

	private String getStringFromMap(Map map, String delim1, String delim2)
	{
		String value = "";

		Set keySet = map.keySet();
		Iterator keys = keySet.iterator();
		while(keys.hasNext())
		{
			String repName = (String)keys.next();
			String excludedRels = (String)map.get(repName);

			if(excludedRels.indexOf(",") > -1)
				excludedRels = excludedRels.substring(0, excludedRels.lastIndexOf(","));

			value += repName + delim1 + excludedRels + delim2;
		}

		//if(value.indexOf(delim2) > -1)
			//value = value.substring(0, value.lastIndexOf(delim2));

		return value;
	}

	private Vector getList(String typeName, String attrValue, String delim)
	{
		Vector attrValueList = new Vector();
		StringTokenizer tokens = new StringTokenizer(attrValue, delim);
		while(tokens.hasMoreElements())
		{
			String value = (String)tokens.nextElement();
			if(null != typeName && !"".equals(typeName))
			{
				value = typeName + "|" + value;
			}
			attrValueList.add(value);
		}
		return attrValueList;
	}

	private HashSet getValueFromMapping(String mapping, String key)
	{
		HashSet valueList = new HashSet();
		StringTokenizer tokenizer = new StringTokenizer(mapping, "\n");
		while(tokenizer.hasMoreElements())
		{
			String row = (String) tokenizer.nextElement();
			if(row.startsWith(key+"|"))
			{
				String mappedValue = row.substring(row.indexOf("|")+1);
				StringTokenizer valTok = new StringTokenizer(mappedValue, ",");
				while(valTok.hasMoreElements())
				{
					valueList.add(valTok.nextElement());
				}
			}
		}
		return valueList;
	}

	private BusinessObject getGCOObjectFromName(Context context, String gcoName) throws Exception
	{
		BusinessObject gcoObject = null;
		MQLCommand mqlCommand = new MQLCommand();
		mqlCommand.executeCommand(context, "temp query bus $1 $2 $3 select $4 dump $5","MCADInteg-GlobalConfig",gcoName, "1","id","|");
		String result = mqlCommand.getResult();
		if(result != null && result.length() > 0)
		{
			String gcoId = result.substring(result.lastIndexOf("|")+1);
			gcoObject = new BusinessObject(gcoId);
		}
		return gcoObject;
	}

	private String executeMQL(Context context,String cmd, String args[]) throws Exception
	{
		//log("Executing MQL Command: " + cmd);
		MQLCommand mqlCommand = new MQLCommand();
		boolean bRet = mqlCommand.executeCommand(context, cmd, args);
		String MQLResult = "";
		if (bRet)
			MQLResult = mqlCommand.getResult();
		else
			throw new Exception(mqlCommand.getError());

		if(MQLResult.endsWith("\n"))
		{
			MQLResult = MQLResult.substring(0, (MQLResult.lastIndexOf("\n")));
		}

		return MQLResult;
	}


	private void createCommandsFile() throws Exception
	{
		String fileNameWithPath = documentDirectory + "ProE_SRep_Migration_Commands.txt";
		migrationCommandsFile				= new FileWriter(fileNameWithPath);
		writer.write("Created file to write commands : " + fileNameWithPath + "\n");
	}

	private void writeToCommandsFile(String command) throws Exception
	{
		try
		{
			migrationCommandsFile.write(command + "\n");
		}
		catch(Exception e)
		{
			writer.write("ERROR in writeing command " + command + "   -   " + e.getMessage() + "\n");
		}
	}

	private void closeCommandsFile() throws Exception
	{
		if(migrationCommandsFile != null)
		{
			migrationCommandsFile.flush();
			migrationCommandsFile.close();
			//migrationCommandsFile = null;
		}
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
		writeMessageToConsole("Step 2 of Migration     : FAILED");
		writeLineToConsole();
		writer.flush();
	}

	private void startIEFLog(String fileName) throws Exception
	{
		try
		{
			iefLog		= new FileWriter(documentDirectory + fileName);
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

			if(failedIDsFile != null)
			{
				failedIDsFile.flush();
				failedIDsFile.close();
			}
		}
		catch(Exception e)
		{
		}
	}
	
	private void startFailedIDsFile(String fileName) throws Exception
	{
		try
		{
			failedIDsFile		= new FileWriter(documentDirectory + fileName);
		}
		catch(Exception e)
		{
			writeMessageToConsole("ERROR: Can not create log file. " + e.getMessage());
		}
	}

	private void log(String message)
	{
		try
		{
			iefLog.write(message + "\n");
		}
		catch(Exception e)
		{
		}
	}
}
