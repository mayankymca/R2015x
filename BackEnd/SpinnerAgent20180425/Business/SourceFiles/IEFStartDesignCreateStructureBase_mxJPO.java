/*
 **  IEFStartDesignCreateStructureBase
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 **  JPO for creating startdesign structure
 */

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map;

import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Policy;
import matrix.db.Query;
import matrix.db.Relationship;
import matrix.util.MatrixException;
import matrix.util.StringList;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADFolderUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;

public class IEFStartDesignCreateStructureBase_mxJPO
{
	protected MCADMxUtil util												= null;
	protected MCADServerGeneralUtil generalUtil					= null;
	protected MCADServerResourceBundle resourceBundle	= null;
	protected IEFGlobalCache cache										= null;
	protected MCADGlobalConfigObject globalConfigObject		= null;
	protected MCADLocalConfigObject localConfigObject		= null;

	protected IEFEBOMConfigObject _ebomConfigObj			= null;
	protected MCADFolderUtil folderUtil									= null;

	protected String iefStartDesignJPOAttrName					= "";
	protected String iefStartDesignFormAttrName					= "";
	protected String quantityAttrName									= "";
	protected String renamedFromAttrName							= "";
	protected String source													= "";
	protected String startDesignSiteNameAttribute					= "";
	protected Hashtable templateObjIDDesignObjIDMap			= null;
	protected Hashtable templateObjTNRBlankAttrList			= null;	

	protected boolean FAIL_AT_FIRST_ERROR						= false;
	protected String DELETED			= "deleted";
	protected String NEW				= "new";
	
	public  IEFStartDesignCreateStructureBase_mxJPO  ()
	{
	}

	public IEFStartDesignCreateStructureBase_mxJPO (Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			MCADServerException.createException("not supported no desktop client", null);
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		return 0;
	}

	/**
	 * This method initializes all the class members useful in the JPO operations
	 */
	public void initialize(Context context,Hashtable argumentsTable, String languageName) throws MCADException
	{
		try
		{
			this.globalConfigObject = (MCADGlobalConfigObject) argumentsTable.get("GCO");
			this.localConfigObject  = (MCADLocalConfigObject) argumentsTable.get("LCO");

			this.resourceBundle	= new MCADServerResourceBundle(languageName);
			this.cache					= new IEFGlobalCache();
			this.util						= new MCADMxUtil(context, resourceBundle, cache);
			this.generalUtil			= new MCADServerGeneralUtil(context, globalConfigObject, resourceBundle, cache);
			this.folderUtil				= new MCADFolderUtil(context, resourceBundle, cache);

			this.iefStartDesignJPOAttrName		= MCADMxUtil.getActualNameForAEFData(context,"attribute_IEFStartDesignJPO");
			this.iefStartDesignFormAttrName		= MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-StartDesignForm");
			this.quantityAttrName						= MCADMxUtil.getActualNameForAEFData(context,"attribute_Quantity");
			this.renamedFromAttrName			= MCADMxUtil.getActualNameForAEFData(context,"attribute_RenamedFrom");
			this.startDesignSiteNameAttribute	= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-StartDesignTemplateSites");

			String confObjTNR			= globalConfigObject.getEBOMRegistryTNR();
			StringTokenizer token		= new StringTokenizer(confObjTNR, "|");
			if(token.countTokens() < 3)
			{
				String errorMessage = resourceBundle.getString("mcadIntegration.Server.Message.EBOMRegistryNotDefined");
				MCADServerException.createException(errorMessage, null);
			}				

			String confObjType				= (String)token.nextElement();
			String confObjName			= (String)token.nextElement();
			String confObjRev				= (String)token.nextElement();
			_ebomConfigObj					= new IEFEBOMConfigObject(context, confObjType, confObjName, confObjRev);

			this.templateObjIDDesignObjIDMap	= new Hashtable();
			this.templateObjTNRBlankAttrList	= new Hashtable();
		}
		catch(Exception e)
		{
			System.out.println("[initialize]: Exception while initializating JPO" + e.getMessage());
			MCADServerException.createException(e.getMessage(), e);
		}
	}

	/**
	 * Entry Point
	 * This method is to be called for creating the structure for StartDesign
	 * functionality.It takes as an input the object ID of the template object which
	 * is selected in the UI.Other inputs are the type, name, revision and policy
	 * for the design object to be created and a map of the attribute name and values
	 * which are to be set on the design object.A flag indicating whether AutoName is
	 * selected for naming of object, is also passed as one of the input parameters.
	 *
	 * Based on the template object ID and other parameters, this object creates the
	 * design object (by calling the JPO name of which is got from IEFStartDesignJPO
	 * attribute on the template object). If the template object is of assembly type,
	 * structure below this template object is traversed and design objects are created
	 * for each of the template objects in the structure. These design objects are then
	 * connected to build the structure.
	 *
	 * @param context
	 * @param args
	 * Key and value in the packed Hastable 
	 * "GCO"            =  Global Config Object
	 * "languageName"   =  locale language name
	 * "templateObjID"  =  Template object ID
	 * "type"           =  Type of the design object to be created
	 * "name"           =  Name of the design object to be created. If AutoName is selected, it should
	 *                     be a blank string
	 * "customRevision" =  Revision of the design object to be created
	 * "policy"         =  Policy for the design object to be created
	 * "autoName"       = "true" if AutoName is selected; "false" otherwise
	 * "autoNameSeries" =  selected autoname series if AutoName is selected, blank string otherwise
	 * "isRootObject"   = "true" if the selected template object is a component or the topmost assembly;
	 *                    "false" otherwise
	 * "attributesMap"  =  hashmap containing actual attribute names as key and values
	 *                     as value
	 * "source"         =  Source details (i.e., value that is to be set on Source attribute)
	 *
	 * @return
	 * @throws Exception
	 */
	public Hashtable createStructure(Context context, String[] args) throws Exception
	{
		Hashtable argumentsTable = (Hashtable)JPO.unpackArgs(args);

		String languageName  = (String)argumentsTable.get("languageName");
		String templateObjID	= (String)argumentsTable.get("templateObjID");
		String folderId				= (String)argumentsTable.get("folderId");

		String partId				= (String)argumentsTable.get("partId");
		this.source				= (String)argumentsTable.get("source");

		try
		{
			initialize(context, argumentsTable, languageName);
			BusinessObject templateObject = new BusinessObject(templateObjID);

			templateObject.open(context);

			//get the name of the JPO for creating design object
			String startDesignJPOName = getStartDesignJPOName(context, templateObject);

			templateObject.close(context);
			//now invoke JPO to create design object
			String designObjDetails = (String)JPO.invoke(context, startDesignJPOName, null, "createDesignObject", args, String.class);

			if(designObjDetails.startsWith("true|"))
			{
				designObjDetails = designObjDetails.substring(5);
				templateObjIDDesignObjIDMap.put(templateObjID, designObjDetails);
			}
			else if(designObjDetails.startsWith("false|"))
			{
				MCADServerException.createException(designObjDetails.substring(6), null);
			}

			String rootDesignObjectID = designObjDetails.substring(0, designObjDetails.indexOf("|"));

			//connectPartWithCADObject(context, partId,rootDesignObjectID);
			if(folderId!=null)
			connectToFolder(context, folderId, rootDesignObjectID);

			Hashtable relsAndEnds = globalConfigObject.getRelationshipsOfClass(MCADServerSettings.ASSEMBLY_LIKE);
			//traverseTemplateStructure(context, templateObjID, rootDesignObjectID, relsAndEnds, argumentsTable, true);

			if(templateObjTNRBlankAttrList.size() > 0)
			{
				String errMsg			= resourceBundle.getString("mcadIntegration.Server.Message.FoundBlankValuesForMandatoryAttr") + "\\n";
				Enumeration keys	= templateObjTNRBlankAttrList.keys();
				while(keys.hasMoreElements())
				{
					String templateObjTNR	= (String)keys.nextElement();
					Vector blankAttrList		= (Vector)templateObjTNRBlankAttrList.get(templateObjTNR);

					errMsg += "\\n" + "Template Object: " + templateObjTNR + "\\n";
					for(int i=0; i<blankAttrList.size(); i++)
					{
						String attrName = (String)blankAttrList.elementAt(i);
						errMsg += "Attribute: " + attrName + "\\n";
					}
				}

				errMsg += "\\n" + resourceBundle.getString("mcadIntegration.Server.Message.SetValidValuesForMandatoryAttr");
				templateObjIDDesignObjIDMap.put("OPERATION_STATUS", "false");
				templateObjIDDesignObjIDMap.put("ERROR_MESSAGE", errMsg);
			}
			else
			{
				templateObjIDDesignObjIDMap.put("OPERATION_STATUS", "true");
			}
		}
		catch(Exception e)
		{
			String errorMessage = e.getMessage();
			
			if(errorMessage == null)
			{
				e.printStackTrace();
				errorMessage = "";
			}

			templateObjIDDesignObjIDMap.put("OPERATION_STATUS", "false");
			templateObjIDDesignObjIDMap.put("ERROR_MESSAGE", errorMessage);
		}
		return templateObjIDDesignObjIDMap;
	}

	protected String getStartDesignJPOName(Context context, BusinessObject templateObject) throws MCADException
	{
		String startDesignJPOName = "";
		try
		{
			Attribute startDesignJPOAttr = templateObject.getAttributeValues(context, iefStartDesignJPOAttrName);

			if(startDesignJPOAttr != null)
			{
				startDesignJPOName = startDesignJPOAttr.getValue();
			}

			if(startDesignJPOName.equals(""))
			{
				Hashtable exceptionDetails = new Hashtable(1);
				exceptionDetails.put("NAME", templateObject.getName());
				MCADServerException.createException(resourceBundle.getString("mcadIntegration.Server.Message.CouldNotGetStartDesignJPOName", exceptionDetails), null);
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

		return startDesignJPOName;
	}

	protected void traverseTemplateStructure(Context context, String parentTemplateObjID, String parentDesignObjID, Hashtable relsAndEnds, Hashtable argumentsTable, boolean isParentObjATemplate) throws MCADException
	{
		try
		{
			//get the first level children template objects
			Hashtable childAndRelIds = generalUtil.getFirstLevelChildAndRelIds(context, parentTemplateObjID, true,relsAndEnds,null);

			Enumeration childIds = childAndRelIds.keys();
			while(childIds.hasMoreElements())
			{
				String childObjID = (String)childIds.nextElement();

				java.util.Vector relIdsVector = (Vector)childAndRelIds.get(childObjID);
				if(relIdsVector != null && relIdsVector.size() > 0)
				{
					BusinessObject childObject = new BusinessObject(childObjID);
					boolean isChildObjATemplate = true;
					String startDesignJPOName = "";

					childObject.open(context);

					String childType  = childObject.getTypeName();
					String cadType = globalConfigObject.getCADTypeForTemplateType(childType);
					if(cadType.equals(""))
					{
						//childType does not exist in IEF-TypeTemplateMapping in GCO.
						//Therefore child object is not of template type (here assumption is
						//is that all the template types are mapped in IEF-TypeTemplateMapping
						//in GCO).Hence no need to create design object. Just connect this object
						//to the parent design object.
						isChildObjATemplate = false;
					}
					else
					{
						//get the startdesign jpo name
						startDesignJPOName = getStartDesignJPOName(context, childObject);
					}

					childObject.close(context);

					String childDesignObjID = "";
					String templateFileNameWoExtn = "";

					if(isChildObjATemplate)
					{
						//get the mapped format for the template Type from GCO
						String mappedFormat		= globalConfigObject.getFormatsForType(childType, cadType);
						templateFileNameWoExtn	= util.getFileNameWithoutExtnForBusID(context, childObjID, mappedFormat);

						//create design object
						childDesignObjID		= createDesignObject(context, argumentsTable, cadType, childObjID, startDesignJPOName, childObject, templateFileNameWoExtn);

					}
					else
					{
						String[] validObjectIDDetails = generalUtil.getValidObjctIdForCheckout(context, childObjID);
						childDesignObjID			  = validObjectIDDetails[1];
					}

					//Do the connection only when at least one of the parent or child is a template object.
					//If both the parent and child are real objects then there is no need to connect them
					if(isParentObjATemplate || isChildObjATemplate)
					{
						for(int i=0;i<relIdsVector.size();i++)
						{
							double quantity	= 1.0;
							//get the relId
							String relationshipID = (String)relIdsVector.elementAt(i);

							Relationship relationship =  new Relationship(relationshipID);
							relationship.open(context);

							String relationshipName = relationship.getTypeName();

							Hashtable relAttrNameValMap = util.getRelationshipAttrNameValMap(context, relationship);

							//set the name of the template file (without extension) as the value for Renamed From attribute
							relAttrNameValMap.put(renamedFromAttrName, "[" + templateFileNameWoExtn + "]");

							
							String sQuantity = (String)relAttrNameValMap.get(quantityAttrName);
							if(sQuantity != null && !sQuantity.trim().equals(""))
							{
								quantity = Double.parseDouble(sQuantity);
							}
							
							relationship.close(context);

							String childEnd = (String)relsAndEnds.get(relationshipName);

							boolean isFrom = true;
							if(childEnd.equals("to"))
							{
								isFrom = true;
							}
							else
							{
								isFrom = false;
							}

							
								//if relationship have quantity attribute then only put it into relation-ship map 
								if(relAttrNameValMap.containsKey(quantityAttrName))
								{
								relAttrNameValMap.put(quantityAttrName, "1.0");
								}

								
								for(int j = 0;j<quantity;j++)
								{
									//connect parent design object to child design object
									util.connectBusObjects(context, parentDesignObjID, childDesignObjID, relationshipName , isFrom, relAttrNameValMap);
								}

						}
						// connect child object to folder 
						String folderId			 = (String)argumentsTable.get("folderId");	

						connectToFolder(context, folderId, childDesignObjID);
					}


					traverseTemplateStructure(context, childObjID, childDesignObjID, relsAndEnds, argumentsTable, isChildObjATemplate);
				}
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
	}

	protected String createDesignObject(Context context, Hashtable argTable, String cadType, String templateObjId, String startDesignJPOName, BusinessObject templateObject, String oldFileNameWithoutExtension) throws MCADException
	{
		String designObjID = "";
		String designObjDetails = "";

		try
		{
			//get the type, policy and revision for the design object to be created
			String designObjType     = getDesignObjectType(cadType);
			String designObjPolicy   = getDesignObjectPolicy(designObjType);
			String designObjRevision = getDesignObjectRevision(context, designObjPolicy);

			String currentAutoNameSeries = (String) argTable.get("autoNameSeries");

			//create the design object corresponding to this childTemplateObject
			argTable.put("templateObjID",templateObjId);
			argTable.put("type",designObjType);
			argTable.put("name","");
			argTable.put("customRevision",designObjRevision);
			argTable.put("policy",designObjPolicy);
			argTable.put("autoName","true");
			argTable.put("autoNameSeries", getAutonameSeries(context, designObjType, currentAutoNameSeries));
			argTable.put("isRootObject","false");

			HashMap attrNameValMap = getAttrNameValMap(context, templateObject);
			//add "CAD Type" , "Source" and FileSource attribute to this list
			attrNameValMap.put("CAD Type", cadType);
			attrNameValMap.put("Source", source);
			attrNameValMap.put("IEF-FileSource", MCADAppletServletProtocol.FILESOURCE_TEMPLATE);
			attrNameValMap.put("Renamed From", oldFileNameWithoutExtension);
			attrNameValMap.put("Description",templateObject.getDescription(context));

			argTable.put("attributesMap",attrNameValMap);

			String[] args = JPO.packArgs(argTable);

			designObjDetails = (String)JPO.invoke(context, startDesignJPOName, null, "createDesignObject", args, String.class);

			if(designObjDetails.startsWith("true|"))
			{
				designObjDetails = designObjDetails.substring(5);
				templateObjIDDesignObjIDMap.put(templateObjId, designObjDetails);
			}
			else if(designObjDetails.startsWith("false|"))
			{
				MCADServerException.createException(designObjDetails.substring(6), null);
			}

			designObjID = designObjDetails.substring(0, designObjDetails.indexOf("|"));
		}
		catch (Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

		return designObjID;
	}

	protected HashMap getAttrNameValMap(Context context, BusinessObject templateObject) throws MCADException
	{
		HashMap attrNameValMap = new HashMap(10);

		try
		{
			String templateObjType = templateObject.getTypeName();
			String templateObjName = templateObject.getName();
			String templateObjRev  = templateObject.getRevision();
			String templateObjTNR  = templateObjType + " " + templateObjName + " " + templateObjRev;

			AttributeList attributeList = templateObject.getAttributeValues(context);
			AttributeItr attributeItr = new AttributeItr(attributeList);
			while(attributeItr.next())
			{
				Attribute attribute = attributeItr.obj();
				String attributeName  = attribute.getName();
				String attributeValue = attribute.getValue();

				if(attributeName.equals(iefStartDesignFormAttrName) || attributeName.equals(iefStartDesignJPOAttrName) || attributeName.equals(startDesignSiteNameAttribute))
				{
					continue;
				}

				if(util.isPropertyExistsOnAttribute(context, attributeName, "mandatory", templateObjType))
				{
					if(attributeValue.equals(""))
					{

						//if fail-fast flag is true, throw error and stop further processing.
						//Otherwise continue processing and generate consolidated list of
						//attributes which are mandatory and have blank value for all the template
						//objects in the structure
						if(FAIL_AT_FIRST_ERROR)
						{
							Hashtable exceptionDetails = new Hashtable(2);
							exceptionDetails.put("TNR", templateObjTNR);
							exceptionDetails.put("ATTRNAME", attributeName);
							MCADServerException.createException(resourceBundle.getString("mcadIntegration.Server.Message.FoundMandatoryAttrWithBlankValue", exceptionDetails), null);
						}
						else
						{
							Vector mandatoryAttrWithBlankValuesList = null;
							mandatoryAttrWithBlankValuesList = (Vector)templateObjTNRBlankAttrList.get(templateObjName);

							if(mandatoryAttrWithBlankValuesList == null)
							{
								mandatoryAttrWithBlankValuesList = new Vector();
							}
							mandatoryAttrWithBlankValuesList.addElement(attributeName);
							templateObjTNRBlankAttrList.put(templateObjTNR, mandatoryAttrWithBlankValuesList);
						}
					}
				}

				attrNameValMap.put(attribute.getName(), attribute.getValue());
			}
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

		return attrNameValMap;
	}

	protected String getDesignObjectType(String cadType)
	{
		String designObjType = "";
		Vector mappedMxTypesList = globalConfigObject.getMappedBusTypes(cadType);

		if(mappedMxTypesList != null && mappedMxTypesList.size() > 0)
		{
			designObjType = (String)mappedMxTypesList.elementAt(0);
		}

		return designObjType;
	}

	protected String getDesignObjectPolicy(String designObjType)
	{
		String designObjPolicy	= "";
	        
        Hashtable matrixTypesPrefTypesMap   = globalConfigObject.getMxTypesPrefTypesMapFromDefaultTypePolicySettingsAttr();
		String isMatrixTypeEnforced         = (String)matrixTypesPrefTypesMap.get(designObjType);
		
		Hashtable matrixTypesDefPoliciesMap = null;
		
		if(source != null && source.length() > 0)
		{
			if(source.contains("|"))
			{
				StringTokenizer token = new StringTokenizer(source, "|");
				source				  = (String)token.nextElement();
			}
		}
		if(isMatrixTypeEnforced.equalsIgnoreCase(MCADAppletServletProtocol.ENFORCED_PREFERENCE_TYPE) || isMatrixTypeEnforced.equalsIgnoreCase(MCADAppletServletProtocol.HIDDEN_PREFERENCE_TYPE))
		{
			//Get the value from GCO 
			matrixTypesDefPoliciesMap = globalConfigObject.getMxTypesDefPolsMapFromDefaultTypePolicySettingsAttr();
		}
		else
		{
			//Get the value from LCO 
			matrixTypesDefPoliciesMap = localConfigObject.getDefaultTypePolicySettings(source);
		}

		Vector defaultPolicies = (Vector)matrixTypesDefPoliciesMap.get(designObjType);
		
		designObjPolicy = (String) defaultPolicies.elementAt(0);

        return designObjPolicy;
	}

	protected String getDesignObjectRevision(Context context, String designObjPolicy) throws MCADException
	{
		String designObjRevision = "";

		try
		{
			Policy policy = new Policy(designObjPolicy);
			policy.open(context);
			designObjRevision = policy.getFirstInSequence();
			policy.close(context);
		}
		catch(MatrixException e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

		return designObjRevision;
	}

	protected String getAutonameSeries(Context context, String designObjType, String inputSeries) throws MCADException
	{
		String autonameSeries = "";
		try
		{
			String baseType		= util.getBaseType(context, designObjType);
			String sNamePattern = util.getSymbolicName(context, "type", baseType);

			//Object Generator Type
			String sObjGeneratorType = MCADMxUtil.getActualNameForAEFData(context, "type_eServiceObjectGenerator");

			String sRevisionPattern ="*";
			String sVaultPattern ="*";
			String sOwnerPattern ="*";
			String sWhereExp = "";

			Query queryObj = new Query("");

			queryObj.open(context);
			queryObj.setBusinessObjectType(sObjGeneratorType);
			queryObj.setBusinessObjectName(sNamePattern);
			queryObj.setBusinessObjectRevision(sRevisionPattern);
			queryObj.setVaultPattern(sVaultPattern);
			queryObj.setOwnerPattern(sOwnerPattern);
			queryObj.setWhereExpression(sWhereExp);
			queryObj.setExpandType(false);

			BusinessObjectList busObjList = queryObj.evaluate(context);

			BusinessObjectItr autoNamerItr = new BusinessObjectItr(busObjList);
			BusinessObject objGenObj = null;

			Vector sortedRevList = new Vector(6);
			while(autoNamerItr.next())
			{
				objGenObj = autoNamerItr.obj();
				String rev = objGenObj.getRevision();
				sortedRevList.addElement(rev);
			}

			if(sortedRevList.contains(inputSeries))
			{
				autonameSeries = inputSeries;
			}
			else
			{
			Collections.sort(sortedRevList);
			autonameSeries = (String)sortedRevList.elementAt(0);
			}
		}
		catch(MCADException e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
		catch (MatrixException e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}

		return autonameSeries;
	}
	/**
	 * This method is used to connect the EC Part with cad Object during SaveAs operation
	 * This method will use the connection JPO used in EBOM Synchronization.
	 */
	protected void connectPartWithCADObject(Context context, String partID, String busObjectID) throws MCADException
	{
		try
		{
			if(partID != null && !"".equals(partID) && !"null".equals(partID))
			{

				BusinessObject busObject = new BusinessObject(busObjectID);
				busObject.open(context);
				busObjectID	= busObject.getObjectId();				
				String sourceName			= generalUtil.getCSENameForBusObject(context, busObject);				
				String busObjectName		= busObject.getName();
				String busObjectTypeName	= busObject.getTypeName();
				busObject.close(context);

				String [] init = new String[] {}; 
				String [] args = new String[11];

				args[0] = partID;
				args[1] = busObjectID;
				args[2] = busObjectName;
				args[3] = "" + !util.isMajorObject(context, busObjectID);// globalConfigObject.isMinorType(busObjectTypeName); // [NDM] OP6
				args[4] = _ebomConfigObj.getTypeName();
				args[5] = _ebomConfigObj.getName();
				args[6] = _ebomConfigObj.getRevision();
				args[7] = resourceBundle.getLanguageName();
				args[8] = sourceName;

				String[] packedGCO = new String[2];
				packedGCO = JPO.packArgs(globalConfigObject);
				args[9]  = packedGCO[0];
				args[10] = packedGCO[1];

				String jpoName 		= _ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_CONNECT_PARTS_JPO);
				String jpoMethod	= "connectPartWithCADObject";
				Hashtable createdPartIdMessageDetails = (Hashtable) JPO.invoke(context, jpoName, init, jpoMethod, args, Hashtable.class);	
			}
		}
		catch (Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
	}

	/**
	 * This method is used to connect the newly created Object with the selected folder durign Create Object 
	 * operation
	 */
	protected void connectToFolder(Context context, String folderID, String busObjectID) throws MCADException
	{
		try
		{
			if(folderID != null && !"".equals(folderID) && !"null".equals(folderID))
			{				
				String applyToChild	= "false";				

				BusinessObject busObject = new BusinessObject(busObjectID);
				busObject.open(context);
				BusinessObject majorBusObject = util.getMajorObject(context,busObject);
				busObject.close(context);

				majorBusObject.open(context);				
				String busName		= majorBusObject.getName();
				String busType		= majorBusObject.getTypeName();
				String busRevision	= majorBusObject.getRevision();
				majorBusObject.close(context);

				boolean hasAssignedFolders = folderUtil.hasAssignedFolders(context, folderID , busType, busName, busRevision);

				if(!hasAssignedFolders)
				{
					folderUtil.assignToFolder(context, majorBusObject, folderID, applyToChild);
				}
			}
		}
		catch (Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
	}
	
	/**
	 * This method is used to connect newly created Object to the selected object during insert new Object operation
	 * operation
	 * @param context
	 * @param args: Key and value in the packed Hashtable 
	 * @return
	 * @throws Exception
	 */
	public Hashtable insertMethod(Context context,String[] args) throws Exception
	{
	Hashtable templateObjIDMap       = new Hashtable();
	try
		{
		StringBuffer sBuff  		    = new StringBuffer(200);
	    Hashtable argumentsTable        = (Hashtable)JPO.unpackArgs(args);
		String majorobjid				= (String)argumentsTable.get("majorobjid");
		String selectedmajorid		    = (String)argumentsTable.get("selectedmajorid");
		String minorObjID	            = (String)argumentsTable.get("minorObjID");
		String sRelModificationvalue        = NEW;	
		
		String sSpatiallocationvalue        = "1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1";//Default spatial location for center positioning		 
                String ATTR_REL_MODIFICATION_STATUS_IN_MATRIX = util.getActualNameForAEFData(context,"attribute_RelationshipModificationStatusinMatrix");
		String ATTR_SPATIAL_LOCATION        = util.getActualNameForAEFData(context,"attribute_SpatialLocation");
		
		this.util						 = new MCADMxUtil(context, resourceBundle, cache);
		this.globalConfigObject 		 = (MCADGlobalConfigObject) argumentsTable.get("GCO");
		BusinessObject CurrbusObj		 = new BusinessObject(selectedmajorid);

		Map mpActMinorData 				= util.getActiveMinorUpdated(context, CurrbusObj, null);
		String selectedlatestMinorId 	= (String)mpActMinorData.get("KEY_ACTIVEMINORID");
		
		String relDir 					= "";
		String relName	        		 = "";
		
		Hashtable relClassMapTable       =  globalConfigObject.getRelationshipsOfClass(MCADServerSettings.CAD_SUBCOMPONENT_LIKE);
		Enumeration relClassEnums   	 = relClassMapTable.keys();
			if(relClassEnums.hasMoreElements()){
				relName 				 = (String)relClassEnums.nextElement();
				relDir   		        = (String)relClassMapTable.get(relName);
			}
				
		StringBuffer sBuffer  				        = new StringBuffer(300);
				sBuffer.append("from["); //TODO hard-coding of direction to be removed
				sBuffer.append(relName);
				sBuffer.append("|(to.id==");
				sBuffer.append(majorobjid);
				sBuffer.append(")].id");
				
				StringBuffer sBufferminor  				        = new StringBuffer(300);
				sBufferminor.append("from["); //TODO hard-coding of direction to be removed
				sBufferminor.append(relName);
				sBufferminor.append("|(to.id==");
				sBufferminor.append(minorObjID);
				sBufferminor.append(")].id");
				
				String sWhExp = sBuffer.toString();
				String sWhExpminor = sBufferminor.toString();
				MqlUtil.mqlCommand( context, "connect bus " + selectedmajorid + " rel \"" + relName + "\" " + relDir + " \"" + majorobjid);
				MqlUtil.mqlCommand( context, "connect bus " + selectedlatestMinorId + " rel \"" + relName + "\" " + relDir + " \"" + minorObjID);	
				String sRes = MqlUtil.mqlCommand( context, "print bus " + selectedmajorid + " select "+ sWhExp + " dump");
				String sResminor = MqlUtil.mqlCommand( context, "print bus " + selectedlatestMinorId + " select "+ sWhExpminor + " dump");
				String sFrmId  = selectedmajorid;
			   
			   	String newTaskAction = "add";
				BusinessObject boNewObj 	= new BusinessObject(majorobjid);
				boNewObj.open(context);
				String sMajorobjType = boNewObj.getTypeName();
				boNewObj.close(context);
				
				sBuff.append("<mxRoot>");
				sBuff.append("<action><![CDATA["+newTaskAction+"]]></action>");
				sBuff.append("<data fromRMB=\"true\" status=\"committed\">");
				//sBuff.append("<item oid=\""+majorobjid+"\" relId=\""+sRes+"\" pid=\""+sFrmId+"\" type=\""+sMajorobjType+"\"   />");
    			sBuff.append("<item oid=\"" + majorobjid + "\" relId=\"" + sRes + "\" pid=\"" + sFrmId + "\" >")
    			.append("<column name=\"Type\" edited=\"true\" a=\""+ sMajorobjType +"\">"+sMajorobjType+"</column>")
    			.append("</item>");						
				sBuff.append("</data>");
				sBuff.append("</mxRoot>"); 
			   
	                HashMap RelAttributes = new HashMap();
	         	RelAttributes.put(ATTR_REL_MODIFICATION_STATUS_IN_MATRIX,sRelModificationvalue);
	        	RelAttributes.put(ATTR_SPATIAL_LOCATION, sSpatiallocationvalue);
			   
		        DomainRelationship correspondingRel = new DomainRelationship(sRes);
			correspondingRel.setAttributeValues(context,RelAttributes);

			DomainRelationship correspondingRelminor = new DomainRelationship(sResminor);
			correspondingRelminor.setAttributeValues(context,RelAttributes);

			StringBuffer sbNameVal = new StringBuffer(200);
			sbNameVal.append("Type");
			sbNameVal.append("|");
			sbNameVal.append(sMajorobjType);
			
			templateObjIDMap.put("Insertdata",sBuff.toString());	
			templateObjIDMap.put("OPERATION_STATUS", "true");
			templateObjIDMap.put("OID", majorobjid);
			templateObjIDMap.put("RELID", sRes);
			templateObjIDMap.put("PID", sFrmId);
			templateObjIDMap.put("COL_NAMEVAL", sbNameVal.toString());
			
					
			BusinessObject majorParentObj = new BusinessObject(selectedmajorid);
			BusinessObject latestMinorParentObj = new BusinessObject(selectedlatestMinorId);
			
			String attrModifiedinMatrix = util.getActualNameForAEFData(context, "attribute_ModifiedinMatrix");
			util.setAttributeOnBusObject(context, majorParentObj, attrModifiedinMatrix, "true");
			util.setAttributeOnBusObject(context, latestMinorParentObj, attrModifiedinMatrix, "true");
		
	     }
		catch(Exception e)
		{
			String errorMessage = e.getMessage();
			
			if(errorMessage == null)
			{
				e.printStackTrace();
				errorMessage = "";
}

			templateObjIDMap.put("OPERATION_STATUS", "false");
			templateObjIDMap.put("ERROR_MESSAGE", errorMessage);
		}
		return templateObjIDMap;
	}
	
	/**
	 * This method is used to remove connection between selected object and its immediate parent during remove instance operation
	 * @param context The user context
	 * @param args Key and value in the packed Hastable 
	 * @return
	 * @throws Exception
	 */
	public Hashtable removeMethod(Context context,String[] args) throws Exception
	{
	Hashtable templateObjIDMap      = new Hashtable();
	try
		{
	   Hashtable argumentsTable         = (Hashtable)JPO.unpackArgs(args);
		String sImmedParentMajor		= (String)argumentsTable.get("sImmedParentMajor");
		String selectedRelId			= (String)argumentsTable.get("selectedRelId");
        String selectedmajorid		= (String)argumentsTable.get("selectedmajorid");

		this.util						= new MCADMxUtil(context, resourceBundle, cache);
		this.globalConfigObject 		= (MCADGlobalConfigObject) argumentsTable.get("GCO");
		BusinessObject CurrbusObj		= new BusinessObject(selectedmajorid);
        
		String ATTR_REL_MODIFICATION_STATUS_IN_MATRIX = util.getActualNameForAEFData(context,"attribute_RelationshipModificationStatusinMatrix");
		String[] arrMajors = new String[1];
		arrMajors[0] = selectedmajorid;
		String[] allMinorsOfGivenRow = util.getAllVersionObjects(context,arrMajors, false);
		String reltnName	        		= "";
		String relDir                   = "";
		boolean bFromDir                = false;
		boolean btoDir            		= false;

             	 DomainRelationship  domRelObj=DomainRelationship.newInstance(context, selectedRelId);
             	 
             	 StringList selectList = new StringList(2);
             	 selectList.addElement(DomainRelationship.SELECT_NAME);
              	 selectList.addElement(DomainRelationship.SELECT_FROM_ID);
							 

             	 java.util.Hashtable tempMap =      domRelObj.getRelationshipData(context, selectList);

				StringList slConnectedRelName =(StringList) tempMap.get(DomainRelationship.SELECT_NAME);
				StringList slConnectedFromId =(StringList) tempMap.get(DomainRelationship.SELECT_FROM_ID);

				String sConnectedRelName = (String) slConnectedRelName.get(0);
				String sConnectedFromId = (String) slConnectedFromId.get(0);

				
			if(sConnectedFromId.equalsIgnoreCase(sImmedParentMajor)){
				btoDir = false;
				bFromDir = true;
			} else if(sConnectedFromId.equalsIgnoreCase(selectedmajorid))
			{
				btoDir = true;
				bFromDir = false;
			}
			
				BusinessObject objImmedParMaj  = new BusinessObject(sImmedParentMajor);
			
				
		Map mpActMinorDataForParent = util.getActiveMinorUpdated(context, objImmedParMaj, null);
		String sImmedParentMinor = (String)mpActMinorDataForParent.get("KEY_ACTIVEMINORID");


		StringList busSelects = new StringList(1);
		busSelects.add( DomainObject.SELECT_ID );

			StringList relSelects = new StringList(1);
			relSelects.addElement(DomainRelationship.SELECT_ID);

	DomainObject doObj = DomainObject.newInstance(context,sImmedParentMinor);
				//Getting all components connected to the object
				MapList mlInfo = doObj.getRelatedObjects( 	context,
															sConnectedRelName,
															"*",
															busSelects,
															relSelects,
															btoDir, //toDir
															bFromDir, //fromDir
															(short)1,
															null,
															null,
															0 );
															
	DomainObject doObjNewMajor = DomainObject.newInstance(context,sImmedParentMajor);
				//Getting all components connected to the object
				MapList mlInfoOfMajor = doObjNewMajor.getRelatedObjects( 	context,
															sConnectedRelName,
															"*",
															busSelects,
															relSelects,
															btoDir, //toDir
															bFromDir, //fromDir
															(short)1,
															null,
															null,
															0 );
			int iPosOfMajorToMajorConnection = -1;															
															
				
								
			for (int i = 0; i < mlInfoOfMajor.size(); i++) {
				Map mItem = (Map)mlInfoOfMajor.get( i );
				String sMajToMajRelId = (String) mItem.get( DomainRelationship.SELECT_ID ) ;
				if(sMajToMajRelId.equals(selectedRelId)){
					iPosOfMajorToMajorConnection = i;
					break;
				}
			}
			
			String sRelIdConnectedToSelectedMinor = null;

			Map mItem = (Map)mlInfo.get(iPosOfMajorToMajorConnection);
				
					String sConnectedChildMinorId = (String) mItem.get( DomainObject.SELECT_ID ) ;
					for(int j=0;j<allMinorsOfGivenRow.length;j++){
						String sEachMinor = (String)allMinorsOfGivenRow[j];
						if(sConnectedChildMinorId.equals(sEachMinor)){
							sRelIdConnectedToSelectedMinor = (String) mItem.get( DomainRelationship.SELECT_ID ) ;
							break;
						}
					}
			
			   
	    HashMap RelAttributes = new HashMap();
		RelAttributes.put(ATTR_REL_MODIFICATION_STATUS_IN_MATRIX,DELETED);

			   
				if(selectedRelId!=null){
		                DomainRelationship correspondingRel = new DomainRelationship(selectedRelId);

			String attrValue = util.getRelationshipAttributeValue(context,correspondingRel,MCADMxUtil.getActualNameForAEFData(context, "attribute_RelationshipModificationStatusinMatrix"));

			if(attrValue != null && attrValue.length() != 0 && attrValue.equalsIgnoreCase("new"))
			{	
				String Args[] = new String[] {selectedRelId};				
				MCADMxUtil.executeMQL("delete connection $1", context, Args);
			}
			else {
		
				correspondingRel.setAttributeValues(context,RelAttributes);					

				String sFMAJPOName = "enoFolderManagementFolder";
				String argsToMQL[] = new String[] {sFMAJPOName};
				String sJPOExists = MCADMxUtil.executeMQL("list program $1", context, argsToMQL);

				if(sJPOExists.startsWith("true"))
				{
					sJPOExists = sJPOExists.substring(5);
					if(sFMAJPOName.equals(sJPOExists)){
						String[] argsToTrigger = new String[3];
						argsToTrigger[0]  = sImmedParentMajor;
						argsToTrigger[1] = selectedmajorid;		
						argsToTrigger[2] = correspondingRel.getName();		
										
						JPO.invoke(context, "enoFolderManagementFolder", null, "updateDisplayOnFilterOnPrimaryRelDisconnect", argsToTrigger, String.class);			
					}

				}			

				}
				
				}
				if(sRelIdConnectedToSelectedMinor!=null){
		                DomainRelationship correspondingRel = new DomainRelationship(sRelIdConnectedToSelectedMinor);

			String attrValue = util.getRelationshipAttributeValue(context,correspondingRel,MCADMxUtil.getActualNameForAEFData(context, "attribute_RelationshipModificationStatusinMatrix"));

			if(attrValue != null && attrValue.length() != 0 && attrValue.equalsIgnoreCase("new"))
			{
				String Args[] = new String[] {sRelIdConnectedToSelectedMinor};
				MCADMxUtil.executeMQL("delete connection $1", context, Args);
			}
			else
				correspondingRel.setAttributeValues(context,RelAttributes);					
						}
				
			templateObjIDMap.put("OPERATION_STATUS", "true");
			
			BusinessObject majorParentObj = new BusinessObject(sImmedParentMajor);
			BusinessObject latestMinorParentObj = new BusinessObject(sImmedParentMinor);
			
			String attrModifiedinMatrix = util.getActualNameForAEFData(context, "attribute_ModifiedinMatrix");
			util.setAttributeOnBusObject(context, majorParentObj, attrModifiedinMatrix, "true");
			util.setAttributeOnBusObject(context, latestMinorParentObj, attrModifiedinMatrix, "true");
			
	     }
		catch(Exception e)
		{
			String errorMessage = e.getMessage();
			
			if(errorMessage == null)
			{
				e.printStackTrace();
				errorMessage = "";
			}

			templateObjIDMap.put("OPERATION_STATUS", "false");
			templateObjIDMap.put("ERROR_MESSAGE", errorMessage);
		}
		return templateObjIDMap;
	}
}


