/*
 *  DSCProjectFolderContent.java
 *
 * Copyright (c) 1992-2012 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 * @author Manoj Pathak 
 * */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;


public class DSCProjectFolderContent_mxJPO extends DomainObject
{
	String objectId                              = null;

	public DSCProjectFolderContent_mxJPO(Context context, String[] args) throws Exception
	{
		super();
		if(args != null && args.length > 0)
			setId(args[0]);
	}

	public int mxMain(Context context, String[] args) throws Exception
	{
		if (!context.isConnected())
			throw new Exception(ComponentsUtil.i18nStringNow("emxTeamCentral.Generic.NotSupportedOnDesktopClient", context.getLocale().getLanguage()));
		return 0;
	}
	   @com.matrixone.apps.framework.ui.ProgramCallable 
	public MapList getFolderContent(Context context, String[] args) throws Exception
	{
		MapList contentMapList 			= null;
		
		try
		{
			HashMap programMap        	= (HashMap) JPO.unpackArgs(args);
			objectId 					= (String)programMap.get("objectId");

			DomainObject domainObject  	= DomainObject.newInstance(context,objectId);
			String sTypeName            = domainObject.getInfo(context, "type");

			boolean bIsTypeWorkspaceVault = sTypeName.equals(DomainObject.TYPE_WORKSPACE_VAULT);

			StringList selectTypeStmts = new StringList(10);
			selectTypeStmts.add(DomainConstants.SELECT_ID);
			selectTypeStmts.add(DomainConstants.SELECT_NAME);
			selectTypeStmts.add(DomainConstants.SELECT_TYPE);
			selectTypeStmts.add(DomainConstants.SELECT_REVISION);
			selectTypeStmts.add(DomainConstants.SELECT_ORIGINATED);
			selectTypeStmts.add(DomainConstants.SELECT_DESCRIPTION);
			selectTypeStmts.add(DomainConstants.SELECT_OWNER);
			selectTypeStmts.add(CommonDocument.SELECT_TITLE);
			selectTypeStmts.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION);

			String sObjWhere = "current.access[read] == TRUE";
			if (bIsTypeWorkspaceVault)
			{
				contentMapList = domainObject.getRelatedObjects(context, DomainObject.RELATIONSHIP_VAULTED_OBJECTS_REV2, "*", selectTypeStmts,
						null,false, true, (short)1, sObjWhere, null, null, null, null);
			}

			Iterator contentListItr = contentMapList.iterator();
			Map contentMap          = null;
			String sType            = DomainConstants.TYPE_DOCUMENT;
			String contentName      = null;
			String parentType 	  	= null;

			while(contentListItr.hasNext())
			{

				contentMap   = (Map)contentListItr.next();
				sType        = (String) contentMap.get(DomainConstants.SELECT_TYPE);
				parentType 	 = CommonDocument.getParentType(context, sType);

				contentMap.put("bIsTypeWorkspaceVault",Boolean.valueOf(bIsTypeWorkspaceVault));
				contentMap.put("objectId", objectId);
				if (!parentType.equals(CommonDocument.TYPE_DOCUMENTS))
				{
					contentName = (String)contentMap.get(DomainConstants.SELECT_NAME);
					contentMap.put(CommonDocument.SELECT_TITLE, contentName);
				} 
			}
		}
		catch (Exception ex)
		{
			System.out.println("Error in getFolderContent = " + ex.getMessage());
			ex.printStackTrace(System.out);
			throw ex;
		}
		finally
		{
			return contentMapList;
		}
	}
}
