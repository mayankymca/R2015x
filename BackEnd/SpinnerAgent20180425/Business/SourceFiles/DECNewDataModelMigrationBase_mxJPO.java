import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import matrix.db.AttributeList;
import matrix.db.BusinessInterfaceList;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.MQLCommand;
import matrix.db.MatrixWriter;
import matrix.db.Relationship;
import matrix.db.RelationshipType;
import matrix.db.RelationshipWithSelect;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleObjectExpander;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADStringUtils;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MqlUtil;

public class DECNewDataModelMigrationBase_mxJPO
{
	BufferedWriter consoleWriter						= null;
	BufferedWriter logWriter							= null;
	BufferedWriter objIdWriter							= null;
	PrintStream errorStream								= null;

	public static int _counter  						= 0;
	public static int _sequence  						= 1;
	public static int _chunk 							= 1000;

	public static StringList _objectidList 				= null;
	public static BufferedWriter _fileWriter 			= null;
	public static java.io.File _oidsFile 				= null;

	public static String inputDirectory 				= "";
	public static String pageObjectName 				= "";
	public static final String fileSeparator			= java.io.File.separator;

	private String outputDirectory 						= "";
	private String failedIdsLogsDirectory 				= "";
	private String gcoBusId								= "";
	private BufferedWriter successObjectidWriter 		= null;
	private BufferedWriter failedObjectidWriter 		= null;
	private HashSet<String> failedObjectIdsList			= null;
	private Vector<String> alreadyProcessedRelIds = null;

	private File successLogFile  						= null;
	private File failedLogFile  						= null;
	private boolean isFailure							= false;
	private MCADGlobalConfigObject 	gcoObject			= null;
	private MCADMxUtil util 							= null; 
	private MQLCommand mqlc 							= null; 

	private String ATTR_IS_VERSIONED_OBJ 				= "";
	private String ATTR_CAD_TYPE		 				= "";
	private String SELECT_ON_ACTIVE_MINOR   			= "";
	private String TO_ACTIVE_VERSION					= "";
	private String SELECT_ON_MAJOR   					= "";

	public DECNewDataModelMigrationBase_mxJPO (Context context, String[] args)throws Exception
	{
	}

	/**
	 * Second Step of DEC New Data Model Migration.
	 * This method changes the type of object in data base according to types mapped in Page Object.  
	 * @param context		Matrix context object
	 * @param args			Command line arguments provided by user
	 * 						arg[0] is a input directory
	 *  					arg[1] is a Page Object Name
	 * @throws Exception
	 */
	public void changeTypes(Context context, String[] args) throws Exception
	{
		String logFileName = "ChangeTypes";

		initialize(context, args, 2, logFileName);

		writeMessageToConsole("====================================================================================");
		writeMessageToConsole("Changing types for Objects...\n");
		writeMessageToConsole("		Reading input log file from : "+inputDirectory);
		writeMessageToConsole("		Writing Log files to: " + outputDirectory );
		writeMessageToConsole("====================================================================================\n");

		try
		{
			// Read files, store oid in set
			File inputDirectoryFile 	= new File(inputDirectory);
			File[] listOfFiles 			= inputDirectoryFile.listFiles();

			HashMap<String, String> oldTypeToNewTypeMap = getOldTypeToNewTypeMappingFromPageObj(context);

			String mqlCmdOIDExists 		= "print bus $1 select exists dump";
			String mqlCmdToGetType 		= "print bus $1 select type dump";
			String mqlCmdToChangeType 	= "modify bus $1 type $2";

			for (File inputObjectIdLogFile : listOfFiles)
			{
				BufferedReader inputFile = new BufferedReader(new FileReader(inputObjectIdLogFile));

				String objectId 	= "";
				String oIdTNR	 	= "";
				String existingType	= "";
				String isObjExists	= "";
				String mqlArgs1[] 	= new String[1];
				String mqlArgs2[] 	= new String[2];

				while ((oIdTNR = inputFile.readLine()) != null)
				{
					objectId		= oIdTNR.substring(0, oIdTNR.indexOf("|"));					
					mqlArgs1[0]		= objectId;

					try 
					{
						isObjExists  	= MqlUtil.mqlCommand(context, mqlCmdOIDExists, mqlArgs1);
					}
					catch (Exception mqlException1) 
					{
						mqlException1.printStackTrace(errorStream);
					}

					if(MCADStringUtils.isNullOrEmpty(isObjExists) || "false".equalsIgnoreCase(isObjExists))
					{
						isFailure = true;
						String msg = objectId + " does not exists in ENOVIA";
						writeMessageToConsole(msg);
						writeErrorToFile("[DECNewDataModelMigrationBase:moveFilesToRevisionObj] Exception occured : "+ msg);

						addObjectIdToFailedObjIdList(oIdTNR, logFileName);

						continue;
					}

					try 
					{
						existingType  	= MqlUtil.mqlCommand(context, mqlCmdToGetType, mqlArgs1);
					}
					catch (Exception mqlException1) 
					{
						mqlException1.printStackTrace(errorStream);
					}

					if(oldTypeToNewTypeMap.containsKey(existingType))
					{
						String newType = oldTypeToNewTypeMap.get(existingType);

						mqlArgs2[0]		= objectId;
						mqlArgs2[1]		= newType;

						try 
						{
							String Args[] = new String[1];
							Args[0] = "off";

							mqlc.executeCommand(context, "trigger $1", Args);

							if(!context.isTransactionActive())
								context.start(true);

							boolean result = executeMqlCommand(context, mqlCmdToChangeType, mqlArgs2, objectId, logFileName);

							if (result)
							{
								successObjectidWriter.write(oIdTNR);
								successObjectidWriter.newLine();
								successObjectidWriter.flush();
							}
							else
								addObjectIdToFailedObjIdList(oIdTNR, logFileName);
						} 
						finally
						{
							// Commit Transaction
							if(context.isTransactionActive())
								context.commit();

							String Args[] = new String[1];
							Args[0] = "on";

							mqlc.executeCommand(context, "trigger $1", Args);
						}
					}
				}

				inputFile.close();
			}

			writeFailedIdsToFile();
		}
		catch(Exception exc)
		{
			writeMessageToConsole(" Exception in changing type : " + exc.getMessage());
			writeErrorToFile("[DECNewDataModelMigrationBase:changeTypes] Exception in changing type : " + exc.getMessage());
			exc.printStackTrace(errorStream);

			closeLogStream();
		}

		writeMessageToConsole("====================================================================================");
		writeMessageToConsole("Changing types for Objects COMPLETED ...");

		if(isFailure)
		{
			writeMessageToConsole("		Change Type Operation FAILED for some object ids.");
			writeMessageToConsole("		Failed object ids list is saved at : " + failedIdsLogsDirectory);
			writeMessageToConsole("		Successful object ids list is saved at : " + successLogFile.getAbsolutePath());
			writeMessageToConsole("		Please find other log files at : " + outputDirectory );
			writeMessageToConsole("			Step 2 of Migration         :  FAILURE ");
		}
		else
		{
			writeMessageToConsole("		Successful object ids list is saved at : " + successLogFile.getAbsolutePath());
			writeMessageToConsole("		Please find other log files at : " + outputDirectory );
			writeMessageToConsole("			Step 2 of Migration         :  SUCCESS ");
		}

		writeMessageToConsole("====================================================================================\n");

		closeLogStream();
	}

	/**
	 * Third and last step of DEC New Data Model migration
	 * This method move file inside Active Versioned object to Revision object 
	 * and Copies Active Versioned Object's relationship to Revision object
	 * @param context		Matrix context object
	 * @param args			Command line arguments provided by user
	 * 						arg[0] is a input directory
	 *  					arg[1] is a Page Object Name
	 *  					arg[2] is a GlobalConfigObject Bus Id
	 * @throws Exception
	 */
	public void fileMovementAndRelModification (Context context, String[] args) throws Exception
	{
		String logFileName = "FileMovementAndRelModification";

		initialize(context, args, 3, logFileName);

		writeMessageToConsole("====================================================================================");
		writeMessageToConsole("Moving files from Iteration objects to Revision Objects ...");
		writeMessageToConsole("And updating Relationships similar to Active Iteration on Revision Objects ...\n");
		writeMessageToConsole("		Reading input log file from : "+inputDirectory);
		writeMessageToConsole("		Writing Log files to: " + outputDirectory );
		writeMessageToConsole("====================================================================================\n");

		try
		{
			// Read files, store oid in set
			File inputDirectoryFile 	= new File(inputDirectory);
			File[] listOfFiles 			= inputDirectoryFile.listFiles();

			String relActiveVersion 	= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
			String relVersionOf		 	= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
			String attrIsVersionedObj	= MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
			String attrCADType			= MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType");

			ATTR_IS_VERSIONED_OBJ 		= "attribute[" + attrIsVersionedObj + "]";
			ATTR_CAD_TYPE		 		= "attribute[" + attrCADType + "]";
			SELECT_ON_ACTIVE_MINOR   	= "from[" + relActiveVersion + "].to.";
			TO_ACTIVE_VERSION			= "to[" + relActiveVersion + "]";
			SELECT_ON_MAJOR   			= "from[" + relVersionOf + "].to.";

			alreadyProcessedRelIds = new Vector<String>();

			StringList selectStmts = new StringList();
			selectStmts.addElement("exists");
			selectStmts.addElement("id");
			selectStmts.addElement("name");
			selectStmts.addElement("type");
			selectStmts.addElement("revision");
			selectStmts.addElement("vault");
			selectStmts.addElement(ATTR_IS_VERSIONED_OBJ);
			selectStmts.addElement(SELECT_ON_MAJOR + "id");
			selectStmts.addElement(SELECT_ON_ACTIVE_MINOR + "id");
			selectStmts.addElement(SELECT_ON_ACTIVE_MINOR + "revision");
			selectStmts.addElement(ATTR_CAD_TYPE);
			selectStmts.addElement("format.file.name");
			selectStmts.addElement(SELECT_ON_ACTIVE_MINOR + "format.file.name");
			selectStmts.addElement(SELECT_ON_MAJOR + "format.file.name");
			selectStmts.addElement("format.file.format");
			selectStmts.addElement(SELECT_ON_ACTIVE_MINOR + "format.file.format");
			selectStmts.addElement(SELECT_ON_MAJOR + "format.file.format");
			selectStmts.addElement(TO_ACTIVE_VERSION);

			Hashtable relsAndEndsForExpansion = gcoObject.getRelationshipsOfClass(MCADServerSettings.ASSEMBLY_LIKE);
			relsAndEndsForExpansion.putAll(gcoObject.getRelationshipsOfClass(MCADServerSettings.FAMILY_LIKE));
			relsAndEndsForExpansion.putAll(gcoObject.getRelationshipsOfClass(MCADServerSettings.CIRCULAR_EXTERNAL_REFERENCE_LIKE));
			relsAndEndsForExpansion.putAll(gcoObject.getRelationshipsOfClass(MCADServerSettings.EXTERNAL_REFERENCE_LIKE));
			relsAndEndsForExpansion.putAll(gcoObject.getRelationshipsOfClass(MCADServerSettings.DERIVEDOUTPUT_LIKE));

			relsAndEndsForExpansion.put(MCADMxUtil.getActualNameForAEFData(context, "relationship_PartSpecification"), MCADAppletServletProtocol.STR_FROM_END);
			relsAndEndsForExpansion.put(MCADMxUtil.getActualNameForAEFData(context, "relationship_AssociatedDrawing"), MCADAppletServletProtocol.STR_TO_END);
			relsAndEndsForExpansion.put(MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveInstance"), MCADAppletServletProtocol.STR_TO_END);

			for (File inputObjectIdLogFile : listOfFiles)
			{
				BufferedReader inputFile 	= new BufferedReader(new FileReader(inputObjectIdLogFile));

				String objectId 	= "";
				String oIdTNR 		= "";

				while ((oIdTNR = inputFile.readLine()) != null)
				{
					objectId		= oIdTNR.substring(0, oIdTNR.indexOf("|"));					

					String oids[] = new String[1];
					oids[0] 	  = objectId;

					BusinessObjectWithSelectList busWithSelectList 	= BusinessObject.getSelectBusinessObjectData(context, oids, selectStmts);
					BusinessObjectWithSelect busWithSelect 			= (BusinessObjectWithSelect) busWithSelectList.elementAt(0);

					try
					{
						String isObjExists = busWithSelect.getSelectData("exists");

						if(MCADStringUtils.isNullOrEmpty(isObjExists) || "false".equalsIgnoreCase(isObjExists))
						{
							isFailure = true;
							String msg = objectId + " does not exists in ENOVIA";
							writeMessageToConsole(msg);
							writeErrorToFile("[DECNewDataModelMigrationBase:moveFilesToRevisionObj] Exception occured : "+ msg);

							addObjectIdToFailedObjIdList(oIdTNR, logFileName);

							continue;
						}

						String Args[] = new String[1];
						Args[0] = "off";

						mqlc.executeCommand(context, "trigger $1", Args);

						// Derived Output
						boolean createDOResult = createDOIfNecessary(context, busWithSelect, objectId, logFileName);

						// File Movement
						boolean moveFileResult = moveFilesToRevisionObj(context, busWithSelect, objectId, logFileName);

						// Relationship Modification
						boolean relModificationResult = relationshipModification(context, busWithSelect, objectId, logFileName, relsAndEndsForExpansion);

						if (createDOResult && moveFileResult && relModificationResult)
						{
							successObjectidWriter.write(oIdTNR);
							successObjectidWriter.newLine();
							successObjectidWriter.flush();
						}
						else
							addObjectIdToFailedObjIdList(oIdTNR, logFileName);
					} 
					catch(Exception exc)
					{
						writeMessageToConsole(" Exception occured : "+exc.getMessage());
						writeErrorToFile("[DECNewDataModelMigrationBase:fileMovementAndRelModification] Exception occured : "+exc.getMessage());
						exc.printStackTrace(errorStream);
					}
					finally
					{
						String Args[] = new String[1];
						Args[0] = "on";

						mqlc.executeCommand(context, "trigger $1", Args);
					}
				}

				inputFile.close();
			}

			writeFailedIdsToFile();

			writeMessageToConsole("====================================================================================");
			writeMessageToConsole("File movement from Iteration objects to Revision Objects & Relationships modification COMPLETED ...\n");

			if(isFailure)
			{
				writeMessageToConsole("			But the Operation FAILED for some object ids.\n");
				writeMessageToConsole("		Failed object ids list is saved at : " + failedIdsLogsDirectory);
				writeMessageToConsole("		Successful object ids list is saved at : " + successLogFile.getAbsolutePath());
				writeMessageToConsole("		Please find other log files at : " + outputDirectory );
				writeMessageToConsole("			Step 3 of Migration         :  FAILURE ");
			}
			else
			{
				writeMessageToConsole("		Successful object ids list is saved at : " + successLogFile.getAbsolutePath());
				writeMessageToConsole("		Please find other log files at : " + outputDirectory );
				writeMessageToConsole("			Step 3 of Migration         :  SUCCESS ");
			}

			writeMessageToConsole("====================================================================================\n");

			closeLogStream();
		}
		catch(Exception exception)
		{
			writeMessageToConsole(" Exception occured : "+exception.getMessage());
			writeErrorToFile("[DECNewDataModelMigrationBase:fileMovementAndRelModification] Exception occured : "+exception.getMessage());
			exception.printStackTrace(errorStream);
			closeLogStream();
		}
	}

	private boolean moveFilesToRevisionObj(Context context, BusinessObjectWithSelect busWithSelect, String objectId, String logFileName) throws Exception
	{
		boolean moveFileResult = true;
		String mqlCommand = "modify bus $1 move from $2 format $3";

		try
		{
			if(!context.isTransactionActive())
				context.start(true);

			String isVersionedObj 	= busWithSelect.getSelectData(ATTR_IS_VERSIONED_OBJ);

			if("false".equalsIgnoreCase(isVersionedObj))
			{
				String mxType					= busWithSelect.getSelectData("type");
				String cadType					= busWithSelect.getSelectData(ATTR_CAD_TYPE);
				String activeMinorId			= busWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR +  "id");

				String formatName				= gcoObject.getFormatsForType(mxType, cadType);

				StringList revisionFiles		= busWithSelect.getSelectDataList("format.file.name");
				StringList revisionFormats		= busWithSelect.getSelectDataList("format.file.format");

				StringList activeMinorFiles		= busWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + "format.file.name");
				StringList activeMinorFormats	= busWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + "format.file.format");

				String fileNameAtRevision		= getFileNameFromFormat(revisionFormats, revisionFiles, formatName);
				String fileNameAtActiveVersion	= getFileNameFromFormat(activeMinorFormats, activeMinorFiles, formatName);

				if("".equals(fileNameAtRevision) && ! "".equals(fileNameAtActiveVersion))
				{
					// File present at Active Minor 
					//util.copyFilesFcsSupported(context, new BusinessObject(activeMinorId), new BusinessObject(objectId), formatName);
					//writeMessageToConsole(objectId + " : Files copied from Active Iteration object");
					Vector formatNameList = new Vector();
					formatNameList.addElement(formatName);
					util.moveFilesFcsSupported(context, new BusinessObject(activeMinorId), new BusinessObject(objectId), formatNameList);
					writeMessageToConsole(objectId + " : Files moved from Active Iteration object to Revision object");
				}
				else if(!"".equals(fileNameAtRevision) && "".equals(fileNameAtActiveVersion))
				{
					// File present at Major	
					//util.copyFilesFcsSupported(context, new BusinessObject(objectId), new BusinessObject(activeMinorId), formatName);
					//writeMessageToConsole(objectId + " : Files copied to Active Iteration object");
				}
				else
					writeMessageToConsole(objectId + " : No file movement required for this object, file already in Revision object for Active Iteration object");
			}
		}
		catch (Exception exception) 
		{
			// Abort Transaction
			if(context.isTransactionActive())
				context.abort();

			moveFileResult = false;
			isFailure = true;
			writeMessageToConsole(objectId + " Exception occured : "+exception.getMessage());
			writeErrorToFile("[DECNewDataModelMigrationBase:moveFilesToRevisionObj] Exception occured : "+exception.getMessage());
			exception.printStackTrace(errorStream);
		}
		finally
		{
			// Commit Transaction
			if(context.isTransactionActive())
				context.commit();
		}

		return moveFileResult;
	}

	private String getFileNameFromFormat(StringList formatList, StringList fileList, String formatName) 
	{
		String fileName	= "";

		int formatIndex	= formatList.indexOf(formatName);
		if(formatIndex > -1)
		{
			fileName 	= (String)fileList.elementAt(formatIndex);

			if(null == fileName)
				fileName = "";
		}

		return fileName;
	}

	private boolean createDOIfNecessary(Context context, BusinessObjectWithSelect busWithSelect, String objectId, String logFileName) throws Exception 
	{
		boolean createDOResult = true;
		Vector<String> DOFormatsProcessed = new Vector<String>();
		try
		{
			if(!context.isTransactionActive())
				context.start(true);

			String isVersionedObj 		= busWithSelect.getSelectData(ATTR_IS_VERSIONED_OBJ);
			String designType			= busWithSelect.getSelectData("type");
			StringList formats			= busWithSelect.getSelectDataList("format.file.format");
			StringList files			= busWithSelect.getSelectDataList("format.file.name");
			String cadType				= busWithSelect.getSelectData(ATTR_CAD_TYPE);
			String isActiveVersionedObj = busWithSelect.getSelectData(TO_ACTIVE_VERSION);

			String formatName			= gcoObject.getFormatsForType(designType, cadType);
			String doBusName			= getFileNameFromFormat(formats, files, formatName);

			if(MCADStringUtils.isNullOrEmpty(doBusName))
				doBusName = busWithSelect.getSelectData("name");

			Vector<String> DOLikeCadTypes = gcoObject.getTypeListForClass(MCADAppletServletProtocol.TYPE_DERIVEDOUTPUT_LIKE);

			if(formats.contains(formatName))
				formats.remove(formatName);

			for (int formatCnt = 0; formatCnt < formats.size(); formatCnt++) 
			{
				String DOFormat 				= (String) formats.get(formatCnt);
				Vector<String> cadTypeMxType	= gcoObject.getCADMxTypeForFormat(DOFormat);

				if(!DOFormatsProcessed.contains(DOFormat))
				{
					for (int cadMxTypeCnt = 0; cadMxTypeCnt < cadTypeMxType.size(); cadMxTypeCnt++) 
					{
						String cadMxType = (String) cadTypeMxType.get(cadMxTypeCnt);

						if(cadMxType.contains("|"+designType))
						{
							String doCadType = cadMxType.substring(0, cadMxType.indexOf("|"+designType));

							if(DOLikeCadTypes.contains(doCadType))
							{
								String doBusType		= getDepDocBOType(doCadType);
								String doBusRevision	= "";

								if("false".equalsIgnoreCase(isVersionedObj))
									doBusRevision = busWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + "revision");								
								else
									doBusRevision = busWithSelect.getSelectData("revision");

								if(!MCADStringUtils.isNullOrEmpty(doBusType) && !MCADStringUtils.isNullOrEmpty(doBusName) 
										&& !MCADStringUtils.isNullOrEmpty(doBusRevision))
								{
									try 
									{
										String vaultName		= busWithSelect.getSelectData("vault");
										String doPolicy 		= gcoObject.getDefaultPolicyForType(doBusType);

										BusinessObject DOBusObj = new BusinessObject(doBusType, doBusName, doBusRevision, vaultName);

										boolean isDOExists 		= DOBusObj.exists(context);

										if(!isDOExists)
											DOBusObj.create(context, doPolicy, null, new AttributeList(), null, null, "", "", new BusinessInterfaceList());

										DOBusObj.open(context);

										String DOBusId = DOBusObj.getObjectId();

										DOBusObj.close(context);

										// Move file 
										String mqlCommand 	= "modify bus $1 move format $2 from $3 format $4";
										String DerOutFormat = gcoObject.getFormatsForType(doBusType, doCadType);

										String mqlArgs[] 	= new String[4];
										mqlArgs[0]			= DOBusId;
										mqlArgs[1]			= DerOutFormat;
										mqlArgs[2]			= objectId;
										mqlArgs[3]			= DOFormat;

										executeMqlCommand(context, mqlCommand, mqlArgs, objectId, logFileName);

										if(!isDOExists)
										{
											String sRelName 		= "";									
											boolean isViewableType 	= util.isTypeDerivedFromBaseType(context, doBusType, new String[] {"type_Viewable"} );

											// TODO: Remove Hardcoding of Relationship
											if(isViewableType)
												sRelName = "Viewable";
											else
												sRelName = "Derived Output";

											String majorId = "";
											String minorId = "";

											if("false".equalsIgnoreCase(isVersionedObj))
											{
												majorId = objectId;
												minorId = busWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + "id");
											}
											else
											{
												majorId = busWithSelect.getSelectData(SELECT_ON_MAJOR + "id");
												minorId = objectId;
											}

											Hashtable relAttrNameVal 			= new Hashtable();
											Hashtable basicToSetForConnection 	= new Hashtable();

											String busName = busWithSelect.getSelectData("name");

											relAttrNameVal.put(MCADMxUtil.getActualNameForAEFData(context, "attribute_CADObjectName"), busName);

											// If Major object or Active Version object then only connect DO with Major object
											if((!MCADStringUtils.isNullOrEmpty(isActiveVersionedObj) && isActiveVersionedObj.equalsIgnoreCase(MCADAppletServletProtocol.TRUE))
													|| "false".equalsIgnoreCase(isVersionedObj))
											{
												util.connectBusObjects(context, majorId, DOBusId, sRelName, true, relAttrNameVal, basicToSetForConnection);
											}

											util.connectBusObjects(context, minorId, DOBusId, sRelName, true, relAttrNameVal, basicToSetForConnection);
										}

										DOFormatsProcessed.addElement(DOFormat);
										writeMessageToConsole(objectId + " : " + doBusType + " object created with format "+DerOutFormat);
									}
									catch (Exception exc)
									{
										createDOResult = false;
										isFailure = true;
										writeMessageToConsole("Exception : " + exc.getMessage());
										writeErrorToFile("[DECNewDataModelMigrationBase:createDOIfNecessary] Exception : " + exc.getMessage());
										exc.printStackTrace(errorStream);
									}
								}
								else
								{
									createDOResult = false;
									isFailure = true;
									writeMessageToConsole("Derived output creation failed for objectid "+objectId);
									writeErrorToFile("[DECNewDataModelMigrationBase:createDOIfNecessary] Derived output creation failed for objectid "+objectId);
									writeMessageToConsole("Derived output Type : " + doBusType + " Name : " + doBusName + " Revision : " + doBusRevision);
									writeErrorToFile("[DECNewDataModelMigrationBase:createDOIfNecessary] Derived output Type : " + doBusType + " Name : " + doBusName + " Revision : " + doBusRevision);
								}
							}
							break;
						}
					}
				}
			}
		}
		catch (Exception exception) 
		{
			// Abort Transaction
			if(context.isTransactionActive())
				context.abort();

			createDOResult = false;
			writeMessageToConsole(" Exception occured : "+exception.getMessage());
			writeErrorToFile("[DECNewDataModelMigrationBase:createDOIfNecessary] Exception occured : "+exception.getMessage());
			exception.printStackTrace(errorStream);
		}
		finally
		{
			// Commit Transaction
			if(context.isTransactionActive())
				context.commit();
		}

		return createDOResult;
	}

	private String getDepDocBOType(String cad_type)
	{
		Vector rawMappedTypes = gcoObject.getMappedBusTypes(cad_type);

		Vector mappedTypes 	= MCADUtil.getListOfActualTypes(rawMappedTypes);
		String doBusType 	= (String)mappedTypes.elementAt(0);

		return doBusType;
	}

	private boolean relationshipModification(Context context, BusinessObjectWithSelect busWithSelect, String objectId,	String logFileName, Hashtable relsAndEndsForExpansion) throws Exception 
	{
		boolean relModificationResult = true;		
		String mqlCommand = "modify connection $1 $2 $3";

		try 
		{
			if(!context.isTransactionActive())
				context.start(true);

			boolean isActiveVersionedObj 	= new Boolean(busWithSelect.getSelectData(TO_ACTIVE_VERSION)).booleanValue();
			boolean isVersionObject		 	= new Boolean(busWithSelect.getSelectData(ATTR_IS_VERSIONED_OBJ)).booleanValue();
			String majorObjId 				= busWithSelect.getSelectData(SELECT_ON_MAJOR + "id");
			String activeMinorObjId			= busWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + "id");

			String ParentMajorId 			= "";
			String ParentMinorId 			= "";

			String[] oidList = new String[1];
			oidList[0] = objectId;

			StringList relSelects = new StringList();
			relSelects.addElement(DomainObject.SELECT_NAME);

			// Expand the object for 1 level on to and from side.
			IEFSimpleObjectExpander simpleObjectExpander = new IEFSimpleObjectExpander(oidList, relsAndEndsForExpansion, new StringList(), relSelects, (short)1);
			simpleObjectExpander.expandInputObjects(context);

			HashMap relIdChildidsMap = simpleObjectExpander.getRelidChildBusIdList(objectId);

			if(relIdChildidsMap != null)
			{
				Iterator relIdsItr = relIdChildidsMap.keySet().iterator();
				while(relIdsItr.hasNext())
				{
					boolean result = true;

					String relId	= (String)relIdsItr.next();
					String childId	= (String)relIdChildidsMap.get(relId);

					RelationshipWithSelect relWithSelect = simpleObjectExpander.getRelationshipWithSelect(relId);
					String relationshipName 			 = relWithSelect.getSelectData(DomainObject.SELECT_NAME);

					try 
					{
						if(isVersionObject &&
								relationshipName.equalsIgnoreCase(MCADMxUtil.getActualNameForAEFData(context, "relationship_PartSpecification")))
						{
							// TODO : move relationship to Revision object always
							String mqlArgs[] 	= new String[3];
							mqlArgs[0]			= relId;
							mqlArgs[1]			= MCADAppletServletProtocol.STR_TO_END;
							mqlArgs[2]			= majorObjId;

							result = executeMqlCommand(context, mqlCommand, mqlArgs, objectId, logFileName);

							if(result)
								writeMessageToLogFile(majorObjId + " : Please do re EBOM-Sync on the object to sync attributes");
						}
						else if(!isVersionObject
								&& relationshipName.equalsIgnoreCase(MCADMxUtil.getActualNameForAEFData(context, "relationship_DesignBaseline")))
						{
							// TODO : move relationship to Active Minor object always
							String mqlArgs[] 	= new String[3];
							mqlArgs[0]			= relId;
							mqlArgs[1]			= MCADAppletServletProtocol.STR_FROM_END;
							mqlArgs[2]			= activeMinorObjId;

							result = executeMqlCommand(context, mqlCommand, mqlArgs, objectId, logFileName);
						}
						else if(gcoObject.isRelationshipOfClass(relationshipName, MCADAppletServletProtocol.DERIVEDOUTPUT_LIKE))
						{
							String newRelId = "";
							if(!isVersionObject) // DO connected to Major
							{
								// If relationship not available between Active Iteration and DO, then create it
								if(!util.doesRelationExist(context, activeMinorObjId, childId, relationshipName, true))
								{
									newRelId = copyRelnBetweenObj(context, activeMinorObjId, childId, relationshipName, relId);
								}
							}
							else if(isActiveVersionedObj)// DO is connected to Active Iteration object
							{
								// If relationship not available between Major Revision and DO, then create it
								if(!util.doesRelationExist(context, majorObjId, childId, relationshipName, true))
								{
									newRelId = copyRelnBetweenObj(context, majorObjId, childId, relationshipName, relId);
								}
							}
							
							if(!MCADStringUtils.isNullOrEmpty(newRelId))
								result = true;
						}
						else if((isActiveVersionedObj || !isVersionObject)
								&& 
								(gcoObject.isRelationshipOfClass(relationshipName, MCADAppletServletProtocol.ASSEMBLY_LIKE) ||
								gcoObject.isRelationshipOfClass(relationshipName, MCADAppletServletProtocol.FAMILY_LIKE) ||
								relationshipName.equalsIgnoreCase(MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveInstance"))))
						{
							/**
							 * Three possible Cases here
							 * Case 1. Parent is not finalized and child is not finalized
							 * 		AssemblyLike relationship between Active iteration of Parent and Child
							 * 		Soln : Copy relationship on Revision of parent and child 
							 * 				(Rel exists check not necessary, multiple relationship of same type can be present betn Parent and child)
							 * 
							 * Case 2. Parent is not finalized and child is finalized
							 * 		AssemblyLike relationship between Active iteration of Parent and Revision obj of Child
							 * 		Soln : Move Relationship's TO end to child Active Iteration,
							 * 			   Copy relationship on Revision of parent and child
							 * 
							 * Case 3. Parent is finalized and child is finalized
							 * 		AssemblyLike relationship between Active iteration of Parent and Revision obj of Child
							 * 										&
							 * 		AssemblyLike relationship between Revision of Parent and Child
							 * 		Soln : Move Relationship's FROM end to child Active Iteration
							 */
							
							if(!alreadyProcessedRelIds.contains(relId))
							{
								if(isActiveVersionedObj)
								{
									ParentMajorId = majorObjId;
									ParentMinorId = objectId;
								}
								else
								{
									ParentMajorId = objectId;
									ParentMinorId = activeMinorObjId;
								}

								String childMajorId = "";
								String childMinorId = "";

								boolean isChildMajor = util.isMajorObject(context, childId);
								if(isChildMajor)
								{
									childMajorId 	= childId;
									childMinorId	= util.getActiveVersionObject(context, childId);
								}
								else
								{
									childMajorId	= util.getMajorObject(context, new BusinessObject(childId)).getObjectId(context);
									childMinorId	= childId;
								}

								boolean isParentFinalized	= isObjectFinalized(context, ParentMajorId);
								boolean isChildFinalized 	= isObjectFinalized(context, childMajorId);

								String relEnd = (String) relsAndEndsForExpansion.get(relationshipName);
								String objectIdForModification = "";

								if(relationshipName.equalsIgnoreCase(MCADMxUtil.getActualNameForAEFData(context, "relationship_AssociatedDrawing")))
								{
									objectIdForModification = ParentMinorId;
									relEnd = MCADAppletServletProtocol.STR_FROM_END;
								}
								else
								{
									objectIdForModification = childMinorId;
									relEnd = MCADAppletServletProtocol.STR_TO_END;
								}

								// Case 2 OR Case 3 (if Parent and Child are not major)
								if((isVersionObject || !isChildMajor) 
									&& ((!isParentFinalized && isChildFinalized) || (isParentFinalized && isChildFinalized)))
								{
									// Move Relationship's FROM end to child Active Iteration
									String mqlArgs[] 	= new String[3];
									mqlArgs[0]			= relId;
									mqlArgs[1]			= relEnd;
									mqlArgs[2]			= objectIdForModification;

									result = executeMqlCommand(context, mqlCommand, mqlArgs, objectId, logFileName);
								}

								// Case 1 OR Case 2
								if(isActiveVersionedObj && ((!isParentFinalized && !isChildFinalized) || (!isParentFinalized && isChildFinalized) ))
								{
									// Copy relationship on Revision of parent and child
									String newRelId = copyRelnBetweenObj(context, ParentMajorId, childMajorId, relationshipName, relId);
									alreadyProcessedRelIds.add(newRelId);
								}
								
								alreadyProcessedRelIds.add(relId);
							}
						}

						if(!result)
						{
							relModificationResult = false;
							isFailure = true;

							writeErrorToFile(objectId + " : " + " Failure in correcting relationship "+ relationshipName );		
							writeErrorToFile("====================================================================================" + "\n");
						}
					}
					catch (Exception exc)
					{
						relModificationResult = false;
						isFailure = true;

						writeErrorToFile(objectId + " : " + exc.getMessage());		
						writeErrorToFile("====================================================================================" + "\n");

						writeMessageToConsole(" Exception : " + exc.getMessage());
						writeErrorToFile("[DECNewDataModelMigrationBase:relationshipModification] Exception : " + exc.getMessage());
						exc.printStackTrace(errorStream);
					}
				}
			}
		}
		catch (Exception e)
		{
			// Abort Transaction
			if(context.isTransactionActive())
				context.abort();

			relModificationResult = false;
			isFailure = true;
			writeErrorToFile("[DECNewDataModelMigrationBase:relationshipModification] Exception : " + e.getMessage());
			e.printStackTrace(errorStream);
		}
		finally
		{
			// Commit Transaction
			if(context.isTransactionActive())
				context.commit();
		}

		return relModificationResult;
	}

	private boolean isObjectFinalized(Context context, String busid) throws Exception
	{
		boolean isObjectFinalized = false;

		String [] oids = new String[]{busid};

		StringList selectlist = new StringList(4);
		selectlist.add("type");
		selectlist.add("current");
		selectlist.add("policy");
		selectlist.add("state");

		BusinessObjectWithSelect busWithSelect 	= BusinessObject.getSelectBusinessObjectData(context, oids, selectlist).getElement(0);

		String finalizationState 	= gcoObject.getFinalizationState(busWithSelect.getSelectData("policy"));
		String currentState			= busWithSelect.getSelectData("current");
		StringList majorStateList 	= busWithSelect.getSelectDataList("state");

		if(MCADStringUtils.isNullOrEmpty(finalizationState))
		{
			String msg = "Finalization state not found for Policy : "+busWithSelect.getSelectData("policy");
			throw new Exception(msg);
		}

		if(majorStateList.lastIndexOf(currentState) >= majorStateList.lastIndexOf(finalizationState))
			isObjectFinalized = true;

		return isObjectFinalized;
	}

	private String copyRelnBetweenObj(Context context, String fromId, String toId, String relationshipName, String relationshipId) throws Exception
	{
		String relId = "";
		BusinessObject fromBO	= null;
		BusinessObject toBO		= null;

		try 
		{
			RelationshipType relType 		= new RelationshipType(relationshipName);
			
			fromBO	= new BusinessObject(fromId);
			toBO	= new BusinessObject(toId);
			
			fromBO.open(context);
			toBO.open(context);
			
			Relationship Rel = fromBO.connect(context, null, relType, true, toBO, new Relationship(relationshipId).getAttributeValues(context, true));
			
			relId = Rel.getName();
			//result = util.connectBusObjects(context, fromId, toId, relationshipName, true, util.getRelationshipAttrNameValMap(context, new Relationship(relationshipId)));
		}
		catch (Exception relConnectException)
		{
			writeErrorToFile("Error while connecting " + fromId + " to " + toId + " with relationship : " + relationshipName);
			relConnectException.printStackTrace(errorStream);
		}
		finally
		{
			fromBO.close(context);
			toBO.close(context);
		}

		return relId;
	}

	private boolean executeMqlCommand(Context context, String mqlCmdToChangeType, String[] mqlArgs2, String objectId, String logFileName) throws Exception 
	{
		boolean bRet = false;

		try 
		{
			bRet = mqlc.executeCommand(context, mqlCmdToChangeType, mqlArgs2);

			if (!bRet)
			{
				isFailure = true;

				writeMessageToConsole(objectId + " : " + mqlc.getError());

				writeErrorToFile(objectId + " : " + mqlc.getError());		
				writeErrorToFile("====================================================================================" + "\n");

				// Abort Transaction
				if(context.isTransactionActive())
					context.abort();
			}
		}
		catch(Exception exc)
		{
			writeMessageToConsole(" Exception : " + exc.getMessage());
			writeErrorToFile("[DECNewDataModelMigrationBase:executeMqlCommand] Exception : " + exc.getMessage());
			exc.printStackTrace(errorStream);
		}

		return bRet;
	}

	private void addObjectIdToFailedObjIdList(String objectId, String logFileName) throws Exception
	{
		try
		{
			failedObjectIdsList.add(objectId);

			if (failedObjectIdsList.size() == _chunk)
			{
				_sequence++;

				writeFailedIdsToFile();

				//create new file
				failedLogFile  			= new File(failedIdsLogsDirectory + logFileName + "_FailedObjectids_" + _sequence + ".log");
				failedObjectidWriter 	= new BufferedWriter(new FileWriter(failedLogFile));
			}
		}
		catch(Exception exc)
		{
			writeErrorToFile("[DECNewDataModelMigrationBase:addObjectIdToFailedObjIdList] Exception : " + exc.getMessage());
			exc.printStackTrace(errorStream);
		}
	}

	private void writeFailedIdsToFile() throws Exception
	{
		try
		{
			if(failedObjectIdsList != null && failedObjectIdsList.size() > 0)
			{
				Iterator failedIdItr = failedObjectIdsList.iterator();

				while (failedIdItr.hasNext())
				{
					failedObjectidWriter.write((String) failedIdItr.next());
					failedObjectidWriter.newLine();
					failedObjectidWriter.flush();
				}

				failedObjectidWriter.close();

				failedObjectIdsList		= new HashSet<String>();
			}
			else
			{
				// delete the empty file created
				failedObjectidWriter.close();
				failedLogFile.delete();
			}
		}
		catch(Exception Exp)
		{
			throw Exp;
		}
	}

	private HashMap<String, String> getOldTypeToNewTypeMappingFromPageObj(Context context) throws Exception 
	{
		HashMap<String, String> typeListMap = new HashMap<String, String>();
		String MQLResult 			= "";

		try 
		{
			// Get Types from Page object
			String args[] 		= new String[1];
			args[0] 			= pageObjectName;

			MQLResult  			= MqlUtil.mqlCommand(context, "print page $1 select content dump", args);

			if (MQLResult == null || MQLResult.length() == 0)
			{
				writeMessageToConsole(" Page Object is blank. Please update Page Object with Type information");
				writeErrorToFile("[DECNewDataModelMigrationBase:getoldTypeToNewTypeMappingFrompageObj] Page Object is blank. Please update Page Object with Type information");
				throw new Exception("Page Object is blank. Please update Page Object with Type information");
			}

		} 
		catch (Exception exception) 
		{
			writeMessageToConsole(" Failure in getting type details fro type conversion from Page obejct : "+exception.getMessage());
			writeErrorToFile("[DECNewDataModelMigrationBase:getoldTypeToNewTypeMappingFrompageObj] Failure in getting type details fro type conversion from Page obejct : "+exception.getMessage());
			exception.printStackTrace(errorStream);
			throw exception;
		}

		byte[] bytes 			= MQLResult.getBytes("UTF-8");
		InputStream input 		= new ByteArrayInputStream(bytes);

		Properties properties	= new Properties();

		properties.load(input);

		if(properties.keySet() != null)
		{
			Iterator keyTypeSymbolicNames  = properties.keySet().iterator();
			while(keyTypeSymbolicNames.hasNext())
			{
				String keyType	= (String) keyTypeSymbolicNames.next();
				String keyvalue = (String) properties.get(keyType);

				if(keyType.endsWith(".changeTo"))
				{
					keyType		= MCADMxUtil.getActualNameForAEFData(context, keyType.substring(0, keyType.indexOf(".changeTo")));
					keyvalue	= MCADMxUtil.getActualNameForAEFData(context, keyvalue);

					typeListMap.put(keyType, keyvalue);
				}
			}
		}

		return typeListMap;
	}

	/**
	 * This method initializes the parameters required for the migration task
	 * @param context		Matrix context object
	 * @param args			Command line arguments provided by user
	 * 						arg[0] is a input directory
	 *  					arg[1] is a Page Object Name
	 *  					arg[2] is a GlobalConfigObject Bus Id
	 * @param cmdArgsCnt	no of command line arguments
	 * @param logFileName	Log file name which will be created inside input directory
	 * @throws Exception
	 */
	private void initialize(Context context, String[] args, int cmdArgsCnt, String logFileName) throws Exception
	{
		try
		{
			consoleWriter 		= new BufferedWriter(new MatrixWriter(context));
			failedObjectIdsList = new HashSet<String>();
			_sequence			= 1;
			_chunk				= 1000;
			isFailure			= false;

			if (args.length != cmdArgsCnt )
			{
				throw new IllegalArgumentException("Wrong number of arguments or arguments with wrong values!");
			}

			mqlc = new MQLCommand();

			inputDirectory = args[0];

			// documentDirectory does not ends with "/" add it
			if(inputDirectory != null && !inputDirectory.endsWith(fileSeparator))
			{
				inputDirectory = inputDirectory + fileSeparator;
			}

			// create a directory to add debug and error logs
			outputDirectory = new File(inputDirectory).getParentFile().getParent() + fileSeparator +  getTimeStamp() + fileSeparator;

			File debugLogFile = new File(outputDirectory + logFileName + "_DebugLog.log");
			File errorLogFile = new File(outputDirectory + logFileName + "_ErrorLog.log");

			debugLogFile.getParentFile().mkdirs();

			logWriter 			 	= new BufferedWriter(new FileWriter(debugLogFile));
			errorStream 			= new PrintStream(new FileOutputStream(errorLogFile));

			successLogFile  		= new File(outputDirectory + logFileName + "_SuccessObjectids.log");
			successObjectidWriter 	= new BufferedWriter(new FileWriter(successLogFile));

			// Create Results directory inside output directory to log failed object ids.
			failedIdsLogsDirectory 	= outputDirectory + "Results" + fileSeparator;
			failedLogFile  			= new File(failedIdsLogsDirectory + logFileName + "_FailedObjectids_" + _sequence + ".log");

			failedLogFile.getParentFile().mkdirs();

			failedObjectidWriter 	= new BufferedWriter(new FileWriter(failedLogFile));

			// Page object name
			pageObjectName = args[1];

			// get GCO id 
			if(args.length == 3)
			{
				String gcoBusId	= args[2];

				// Get Types from GCO BusTypeMapping
				MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(null);
				util 								= new MCADMxUtil(context, new MCADServerResourceBundle(""), new IEFGlobalCache());
				gcoObject							= configLoader.createGlobalConfigObject(context, util, gcoBusId);
			}
		}
		catch (Exception iExp)
		{
			if(null != consoleWriter)
				consoleWriter.write("[DECNewDataModelMigrationBase:initialize] Exception in initialization : "+iExp.getMessage());
			else
				System.out.println("[DECNewDataModelMigrationBase:initialize] Exception in initialization : "+iExp.getMessage());

			if(null != errorStream)
				iExp.printStackTrace(errorStream);

			closeLogStream();

			return;
		}
	}

	private String getTimeStamp()
	{
		Date date = new Date();
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd").format(date)+"T"+ new SimpleDateFormat("HH:mm:ss").format(date);

		timeStamp = timeStamp.replace('-', '_');
		timeStamp = timeStamp.replace(':', '_');

		return timeStamp;
	}

	private void closeLogStream() throws IOException
	{
		try 
		{
			if(null != consoleWriter)
				consoleWriter.close();

			if(null != logWriter)
				logWriter.close();

			if(null != errorStream)
				errorStream.close();

			if(null != successObjectidWriter)
				successObjectidWriter.close();

			if(null != failedObjectidWriter)
				failedObjectidWriter.close();
		} 
		catch (IOException e) 
		{
			System.out.println("Exception while closing log stream "+e.getMessage());
		}
	}

	private void writeMessageToConsole(String message) throws Exception
	{
		consoleWriter.write(message + "\n");
		consoleWriter.flush();
		writeMessageToLogFile(message);
	}

	private void writeMessageToLogFile(String message) throws Exception
	{
		logWriter.write(MCADUtil.getCurrentTimeForLog() + message + "\n");
	}

	private void writeErrorToFile(String message) throws Exception
	{
		errorStream.write(message.getBytes("UTF-8"));
		errorStream.write("\n".getBytes("UTF-8"));
	}

	public boolean writeOID(Context context, String[] args) throws Exception
	{
		try
		{
			StringBuffer oIdTNR = new StringBuffer(args[0]);
			oIdTNR.append("|");
			oIdTNR.append(args[1]);
			oIdTNR.append("|");
			oIdTNR.append(args[2]);
			oIdTNR.append("|");
			oIdTNR.append(args[3]);

			_objectidList.add(oIdTNR.toString());

			_counter++;

			if (_counter == _chunk)
			{
				_counter=0;
				_sequence++;

				//write oid from _objectidList
				for (int s=0;s<_objectidList.size();s++)
				{
					_fileWriter.write((String)_objectidList.elementAt(s));
					_fileWriter.newLine();
					_fileWriter.flush();
				}

				_objectidList=new StringList();
				_fileWriter.close();

				//create new file
				_oidsFile = new java.io.File(inputDirectory + pageObjectName + "_Objectids_" + _sequence + ".log");
				_fileWriter = new BufferedWriter(new FileWriter(_oidsFile));
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * This method takes care of leftover objectIds which do add up to the limit specified
	 *
	 * @param none
	 * @returns none
	 * @throws Exception if the operation fails
	 */
	public static void cleanup() throws Exception
	{
		try
		{
			if(_objectidList != null && _objectidList.size() > 0)
			{
				for (int s=0;s<_objectidList.size();s++)
				{
					_fileWriter.write((String)_objectidList.elementAt(s));
					_fileWriter.newLine();
				}
				_fileWriter.close();
			}
			else
			{
				// delete the empty file created
				_fileWriter.close();
				_oidsFile.delete();
			}
		}
		catch(Exception Exp)
		{
			throw Exp;
		}
	}
}
