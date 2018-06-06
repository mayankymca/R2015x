/*
 **  DECCaptureModifiedAttributes.java
 **
 **  Copyright Dassault Systemes, 1992-2007.
 **  All Rights Reserved.
 **  This program contains proprietary and trade secret information of Dassault Systemes and its 
 **  subsidiaries, Copyright notice is precautionary only
 **  and does not evidence any actual or intended publication of such program
 **
 */

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADXMLUtils;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;
import com.matrixone.apps.domain.DomainConstants;

import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;

public class DECCaptureModifiedAttributes_mxJPO 
{
	private boolean isCheckinEx      = false;
	private boolean isCheckin        = false;
	private boolean isAttributeSynch = false;
	private boolean isRename		 = false;

	private boolean isLockUnlock	 = false;
	private boolean isCheckout	 	 = false;
	private boolean isCheckoutEx	 = false;

	public DECCaptureModifiedAttributes_mxJPO ()
	{
	}

	public DECCaptureModifiedAttributes_mxJPO (Context context, String args[] ) throws Exception
	{
		String language   = "en-us";
		MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

		isCheckin   	  = getRPEforOperation(context, mxUtil, MCADServerSettings.IEF_CHECKIN).equalsIgnoreCase("true");
		isCheckinEx 	  = getRPEforOperation(context, mxUtil, MCADServerSettings.IEF_CHECKINEX).equalsIgnoreCase("true");
		isAttributeSynch  = getRPEforOperation(context, mxUtil, MCADServerSettings.IEF_ATTR_SYNC).equalsIgnoreCase("true");

		isLockUnlock = getRPEforOperation(context, mxUtil, MCADServerSettings.IEF_LOCK_UNLOCK).equalsIgnoreCase("true");

		isCheckout = getRPEforOperation(context, mxUtil, MCADServerSettings.IEF_CHECKOUT).equalsIgnoreCase("true");
		isCheckoutEx = getRPEforOperation(context, mxUtil, MCADServerSettings.IEF_CHECKOUTEX).equalsIgnoreCase("true");


		isRename  = getRPEforOperation(context, mxUtil, MCADGlobalConfigObject.FEATURE_RENAME).equalsIgnoreCase("true");
	}

	public int mxMain(Context context, String []args) throws Exception
	{
		return 0;
	}

	public void captureModifiedAttributes(matrix.db.Context context, String[] args) throws Exception
	{		
		String language 		= "en-us";
		MCADMxUtil mxUtil		= new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
		String modifiedAttribNm	= MCADMxUtil.getActualNameForAEFData(context, "attribute_ModifiedCADAttributes");

		try
		{
			String objectId = args[0];
			String attrNm 	= args[1];
			String attrVal	= args[2];

			String attrIsVersionObject	  = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
			String attrMoveFilesToVersion = MCADMxUtil.getActualNameForAEFData(context, "attribute_MoveFilesToVersion");
			String attrSuspendVersioning  = MCADMxUtil.getActualNameForAEFData(context, "attribute_SuspendVersioning");
			String fileMsgDigestAttrName  = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-FileMessageDigest");

			if(attrNm.equals(attrIsVersionObject) || attrNm.equals(attrMoveFilesToVersion) || attrNm.equals(attrSuspendVersioning))
				return;

			if(isRename && attrNm.equals(fileMsgDigestAttrName))
				return;

			String   lockInfoAttributeName = MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-LockInformation");

			if((isLockUnlock || isCheckout || isCheckoutEx) && attrNm.equals(lockInfoAttributeName))
				return; 

			if(!attrNm.equals("") && !attrNm.equals(modifiedAttribNm) 
					&& !isCheckin 
					&& !isAttributeSynch
					&& !isCheckinEx )
			{
				BusinessObject bo = new BusinessObject(objectId);
				bo.open(context);			

				String modifiedAttrib = getModifiedAttributeList(context, attrNm, bo, mxUtil, attrVal);

				mxUtil.setAttributeValue(context, bo, modifiedAttribNm, modifiedAttrib);

				String val		= bo.getAttributeValues(context, modifiedAttribNm).getValue();
				String checkVal = objectId + "," + val;

				String Args[] = new String[3];
                                Args[0] = "global";
				Args[1] = "DEC_ATTR_VALUES";
				Args[2] = checkVal;
				mxUtil.executeMQL(context, "set env $1 $2 $3 ", Args);				

				bo.close(context);
			}
			else if(attrNm.equals(modifiedAttribNm) && getRPEforOperation(context, mxUtil, "DEC_ATTR_OPERATION").equals(""))
			{
				BusinessObject bo 		= new BusinessObject(objectId);
				bo.open(context);	

				String objIdFromRPE		= "";
				String attrValFromRPE	= "";
				String rpeValue			= getRPEforOperation(context, mxUtil, "DEC_ATTR_VALUES");

				StringTokenizer tokens = new StringTokenizer(rpeValue, ",");
				if(tokens.countTokens() >= 2)
				{
					objIdFromRPE	= (String)tokens.nextElement();
					attrValFromRPE	= (String)tokens.nextElement();
				}

				if(objectId.equals(objIdFromRPE))
				{
					if(!attrVal.equals(attrValFromRPE))
					{
						mxUtil.setAttributeValue(context, bo, modifiedAttribNm, attrValFromRPE);
						mxUtil.setRPEVariablesForIEFOperation(context, "DEC_ATTR_OPERATION");
						mxUtil.unsetRPEVariablesForIEFOperation(context, "DEC_ATTR_VALUES");
					}
				}

				bo.close(context);
			}
		}
		catch(Exception e)
		{
			mxUtil.unsetRPEVariablesForIEFOperation(context, "DEC_ATTR_VALUES");
			mxUtil.unsetRPEVariablesForIEFOperation(context,"DEC_ATTR_OPERATION");	
		}
	}

	public void captureModifiedDescription (matrix.db.Context context, String[] args) throws Exception
	{		
		try
		{
			String language 	= "en-us";

			String objectId		= args[0];

			MCADMxUtil mxUtil	= new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

			if(!isCheckin && !isAttributeSynch && !isCheckinEx)
			{
				String attrNm 		= "$$description$$";
				BusinessObject bo 	= new BusinessObject(objectId);
				bo.open(context);

				String attrVal 		= bo.getDescription(context);

				String modifiedAttribNm	= MCADMxUtil.getActualNameForAEFData(context, "attribute_ModifiedCADAttributes");

				String modifiedAttrib 	= getModifiedAttributeList(context, attrNm, bo, mxUtil, attrVal);
				mxUtil.setAttributeValue(context, bo, modifiedAttribNm, modifiedAttrib);

				String checkVal = objectId + "," + modifiedAttrib;

				String Args[] = new String[2];
				Args[0] = "DEC_ATTR_VALUES";
				Args[1] = checkVal;
				mxUtil.executeMQL(context, "set env global $1 $2 ", Args);	
				bo.close(context);
			}
		}
		catch(Exception e)
		{

		}
	}	

	private String getModifiedAttributeList(matrix.db.Context context, String attrNm, BusinessObject bo, MCADMxUtil mxUtil, String attrVal) throws Exception
	{

		String gcoType 		  			=  MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
		String gcoName		  			= "";
		String globalRegistryType  		= MCADMxUtil.getActualNameForAEFData(context, "type_IEF-GlobalRegistry");
		String globalRegistryName  		= "IEF-GlobalRegistry";
		String globalRegistryRevision  	= "-";
		String charSet					= "UTF-8";
		String result					= "";
		BusinessObject majorBus			= null;
		BusinessObject minorBus			= null;
		boolean isMajorSync				= false;
		boolean isMinorSync				= false;
		try 
		{
			String modifiedAttribNm			= MCADMxUtil.getActualNameForAEFData(context, "attribute_ModifiedCADAttributes");
			String registryDataAtrNm		= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-RegistryData");
			String mxToCADMapAttrNm			= MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-MxToCADAttribMapping");
			String attributeTitle			= MCADMxUtil.getActualNameForAEFData(context, "attribute_Title");
            String attributeSource                  = MCADMxUtil.getActualNameForAEFData(context,"attribute_Source");
			
			String IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context,"attribute_IsVersionObject");
			String SELECT_ISVERSIONOBJ = "attribute["+IS_VERSION_OBJ+"]";
			String SELECT_SOURCE = "attribute["+attributeSource+"]";				
			String SELECT_MODIFIEDCADATTRS = "attribute["+modifiedAttribNm+"]";		
					
			StringList slSelectsForInputID = new StringList(5);
			slSelectsForInputID.addElement(DomainConstants.SELECT_ID);
			slSelectsForInputID.addElement(DomainConstants.SELECT_TYPE);
			slSelectsForInputID.addElement(SELECT_ISVERSIONOBJ);
			slSelectsForInputID.addElement(SELECT_SOURCE);
			slSelectsForInputID.addElement(SELECT_MODIFIEDCADATTRS);
					
			String sThisBOID = bo.getObjectId(context);
			StringList slOid = new StringList(1);
			slOid.addElement(sThisBOID);
			
			String [] oidsTopLevel		  = new String [slOid.size()];
			slOid.toArray(oidsTopLevel);			
			
			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, oidsTopLevel, slSelectsForInputID);
			BusinessObjectWithSelect busObjectWithSelect 		= (BusinessObjectWithSelect)buslWithSelectionList.elementAt(0);
			
			String sIsVersionObj         = (String)busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
			boolean isVersionObject = Boolean.valueOf(sIsVersionObj).booleanValue();
			String sourceAttribValue         = (String)busObjectWithSelect.getSelectData(SELECT_SOURCE);	
			String objType         = (String)busObjectWithSelect.getSelectData(DomainConstants.SELECT_TYPE);	
			String modifiedAttrib         = (String)busObjectWithSelect.getSelectData(SELECT_MODIFIEDCADATTRS);			
			
			//String sourceAttribValue		= (bo.getAttributeValues(context,attributeSource)).getValue();
			//System.out.println("--inside getModifiedAttributeList()-new object->"+bo.getObjectId(context));
			//String objType 					=  bo.getTypeName();

			String integName				= getIntegrationName(sourceAttribValue);
			//String sUser = context.getUser();
			String gcoRev     = MCADMxUtil.getConfigObjectRevision(context);

			StringBuffer sbKey = new StringBuffer(100);
			sbKey.append(integName);
			sbKey.append("|");
			sbKey.append(gcoRev);
			String sKey = sbKey.toString();		
			
			/*System.out.println("--inside getModifiedAttributeList()-->"+integName+"--->"+context.getUser());
			IEFSimpleConfigObject simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, integName);
			
			

			if (null == simpleGCO)
			{
				BusinessObject registryObj	= new BusinessObject(globalRegistryType,globalRegistryName,globalRegistryRevision , "");
				String registryXmlStr		= (registryObj.getAttributeValues(context, registryDataAtrNm)).getValue();
				IEFXmlNode  registryXml		=  MCADXMLUtils.parse(registryXmlStr, charSet);

				Enumeration registoryList 	= registryXml.elements();
				while(registoryList.hasMoreElements())
				{
					IEFXmlNode integNode		= (IEFXmlNode)registoryList.nextElement();
					IEFXmlNode defaultGCONode	= integNode.getChildByName("defaultGCOName");;
					String  defaultGCONm 		= defaultGCONode.getFirstChild().getContent();
					IEFXmlNode nameNode			= integNode.getChildByName("name");
					String  integNmXML 			= nameNode.getFirstChild().getContent();

					if(integName.equals(integNmXML))
					{
						gcoName = defaultGCONm;
						break;
					}
				}
				simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, gcoType, gcoName);
			}

			System.out.println("--old approach-->"+simpleGCO);*/
			IEFSimpleConfigObject simpleGCO = (IEFSimpleConfigObject)mxUtil.getUserSpecificGCODefinition(context,sKey);

			
			
			//boolean  isVersionObject =  !mxUtil.isMajorObject(context, bo.getObjectId(context));//isMinorType(context, objType, simpleGCO, mxUtil); // [NDM] OP6
			
			if (getRPEforOperation(context, mxUtil, "Finalize").equals("") && getRPEforOperation(context, mxUtil, "UndoFinalize").equals(""))
			{
				String Arguments[] = new String[2];
				Arguments[0] = "global";
				Arguments[1] = "PRSD_Operation";
				String isPRSDOperation		= mxUtil.executeMQL(context, "get env $1 $2",Arguments);
				if (!isVersionObject)
				{
					String majorSync = getRPEforOperation(context, mxUtil, MCADServerSettings.IEF_ATTR_SYNC_MAJOR);
					// Do nothing
					if(majorSync.equals(""))
					{
						mxUtil.setRPEVariablesForIEFOperation(context, MCADServerSettings.IEF_ATTR_SYNC_MINOR);
						isMinorSync	 = true;
						minorBus	 = mxUtil.getActiveMinor(context, bo);
						
							if(!attributeTitle.equals(attrNm))
							{							
								mxUtil.setAttributeValue(context, minorBus, attrNm, attrVal);
							}
							if(attributeTitle.equals(attrNm) && isPRSDOperation.startsWith("true"))
							{
								mxUtil.setAttributeValue(context, minorBus, attrNm, attrVal);
							}
						
					}
				}
				else
				{
					String minorSync				= getRPEforOperation(context, mxUtil, MCADServerSettings.IEF_ATTR_SYNC_MINOR);
					majorBus						= mxUtil.getMajorObject(context,bo);
					BusinessObject activeMinorBus	= mxUtil.getActiveMinor(context, majorBus);
					if(minorSync.equals("") && bo.equals(activeMinorBus))
					{
						mxUtil.setRPEVariablesForIEFOperation(context,MCADServerSettings.IEF_ATTR_SYNC_MAJOR);
						isMajorSync = true;
						
							if(!attributeTitle.equals(attrNm))
								mxUtil.setAttributeValue(context, majorBus, attrNm, attrVal);
													
							if(attributeTitle.equals(attrNm) && isPRSDOperation.startsWith("true"))
							{
								mxUtil.setAttributeValue(context, majorBus, attrNm, attrVal);
							}
													
					}
				}
			}

			// [NDM] OP6
		/*	if (isVersionObject)
			{
				objType 	= mxUtil.getCorrespondingType(context, objType);
			}*/

			//String modifiedAttrib		= (bo.getAttributeValues(context, modifiedAttribNm)).getValue();
			boolean isAttributePresent	= isAttributePresentInList(modifiedAttrib, attrNm);
			String attribMappingVal		= simpleGCO.getConfigAttributeValue(mxToCADMapAttrNm);

			HashSet mxAttrSet                   = new HashSet();
			StringTokenizer attribMappingTokens = new StringTokenizer(attribMappingVal, "\n");				
			while(attribMappingTokens.hasMoreElements())
			{				
				String attribMapping			= (String)attribMappingTokens.nextElement();
				StringTokenizer mappingToken	= new StringTokenizer(attribMapping, "|");
				if(mappingToken.countTokens() >= 2)
				{
					String mxSideAttrib				= (String)mappingToken.nextElement();
					StringTokenizer mxSideAtrToken	= new StringTokenizer(mxSideAttrib, ",");
					if(mxSideAtrToken.countTokens() >= 2)
					{

						String mxType = (String)mxSideAtrToken.nextElement();
						String mxAttr = (String)mxSideAtrToken.nextElement();

						if(mxAttr.equals(attrNm) && ("all".equalsIgnoreCase(mxType) || objType.equals(mxType)) && !mxAttrSet.contains(mxAttr))
						{
							mxAttrSet.add(mxAttr);
							if(attrNm.startsWith("$$") && attrNm.endsWith("$$"))
							{	
								attrNm = attrNm.substring(2,attrNm.length()-2);
							}

							String modifiedEventsActualName	= MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-ModificationEvents");

							Vector modificationEvents = simpleGCO.getAttributeAsVector(modifiedEventsActualName, ",");

							if(modificationEvents.contains(MCADAppletServletProtocol.UPDATESTAMP_EVENT_ATTRIBUTE_MODIFICATION))
								mxUtil.modifyUpdateStamp(context, bo.getObjectId(context));

							if(modifiedAttrib.equals(""))
							{
								modifiedAttrib = attrNm;
							}
							else if(!isAttributePresent)
							{
								modifiedAttrib = modifiedAttrib+"|"+attrNm;
							}
						}
					}

					result = modifiedAttrib;
				}
			}
		}
		catch (Exception e)
		{

		}
		finally
		{
			if(isMinorSync)
			{
				mxUtil.unsetRPEVariablesForIEFOperation(context, MCADServerSettings.IEF_ATTR_SYNC_MINOR);
			}			
			if(isMajorSync)
			{
				mxUtil.unsetRPEVariablesForIEFOperation(context,MCADServerSettings.IEF_ATTR_SYNC_MAJOR );	
			}

			mxUtil.unsetRPEVariablesForIEFOperation(context,"DEC_ATTR_OPERATION");
		}

		return result;
	}

	private String getRPEforOperation(matrix.db.Context context,MCADMxUtil mxUtil,  String operationName)
	{
		

		String Args[] = new String[2];
		Args[0] = "global";
		Args[1] = operationName;
		String sResult = mxUtil.executeMQL(context, "get env $1 $2", Args);
		
		String result	= "";
		if(sResult.startsWith("true"))
		{
			result = sResult.substring(sResult.indexOf("|")+1, sResult.length());
		}

		return result;
	}

	// [NDM] OP6  isMinorType Method removed

	private boolean isAttributePresentInList(String modifiedAttrib, String attrNm)
	{
		boolean isAttibPresentFlag = false;

		if(attrNm.startsWith("$$") && attrNm.endsWith("$$"))
		{	
			attrNm = attrNm.substring(2,attrNm.length()-2);

		}
		StringTokenizer modifiedAttribToken = new StringTokenizer(modifiedAttrib,"|");
		while(modifiedAttribToken.hasMoreElements())
		{
			String modifiedAttribVal = (String)modifiedAttribToken.nextToken();
			if(modifiedAttribVal.equals(attrNm))
			{
				isAttibPresentFlag = true ;
				break;
			}

		}
		return isAttibPresentFlag;
	}

	private String getIntegrationName(String sourceAttrib)
	{
		String result = sourceAttrib;
		if (result == null || result.length() < 0)
			return "";

		int pos = result.indexOf("|");
		if (pos > 0)
		{
			return result.substring(0, pos);
		}

		if(result.length() > 0 )
			return result;
		else
			return "";
	}

} 

