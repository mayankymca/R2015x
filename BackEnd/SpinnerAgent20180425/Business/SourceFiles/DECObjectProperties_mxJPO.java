/*
**  DECObjectProperties
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Class defining basic infrastructure, contains common data members required
**  for executing any IEF related actions.
*/

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.apps.domain.util.MapList;

public class DECObjectProperties_mxJPO
{
      protected MCADServerResourceBundle _serverResourceBundle  = null;
	  protected IEFGlobalCache _cache							= null;
	  protected MCADMxUtil _util								= null;
	 // HashMap selectDataMap								        = null;;
	 HashMap hmObjInfo = new HashMap();

    public  DECObjectProperties_mxJPO  ()
    {
    }

    public DECObjectProperties_mxJPO (Context context, String[] args) throws Exception
    {   
		if (!context.isConnected())
        {
            MCADServerException.createException("not supported no desktop client", null);
        }
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }
    public String getRevision(Context context, String[] args) throws Exception
    {
		String revision = null;
       
        try
        {
				HashMap params	   = (HashMap)JPO.unpackArgs(args);
			
				HashMap requestMap = (HashMap)params.get("requestMap");	
				String objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
		
		HashMap hmData = (HashMap)hmObjInfo.get(objectId);
		if(hmData!=null && hmData.size() > 0)
		{
			revision		   = (String)hmData.get("revision");
		
		} else {
						HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

hmObjInfo.put(objectId,dataMap);
revision		   = (String)dataMap.get("revision");
		}
		
			/*HashMap selectDataMap = null;		
			if(selectDataMap != null && selectDataMap.size() > 0)
			{
System.out.println("--inside getRev...");
				revision = (String)selectDataMap.get("revision");
			}else
			{
System.out.println("--inside getRev. ELSE..");

				HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 
System.out.println("--inside getRev. ELSE.."+dataMap);
				revision		   = (String)selectDataMap.get("revision");
			}*/
        }
        catch (Exception e)
        { 
			
        }
        return revision;
    }

	public String getPolicy(Context context, String[] args) throws Exception
    {
		String policy = null;
		String localeLanguage = null;

       
        try
        {

				HashMap params	   = (HashMap)JPO.unpackArgs(args);

				HashMap requestMap = (HashMap)params.get("requestMap");	
				String objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
		localeLanguage	   = requestMap.get("languageStr").toString();
		HashMap hmData = (HashMap)hmObjInfo.get(objectId);
		if(hmData!=null && hmData.size() > 0)
		{
			policy		   = (String)hmData.get("policy");
		
		} else {
						HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

hmObjInfo.put(objectId,dataMap);
policy		   = (String)dataMap.get("policy");
		}
policy			   =  MCADMxUtil.getNLSName(context, "Policy", policy, "", "" , localeLanguage);	


/*
		
			HashMap params	   = (HashMap)JPO.unpackArgs(args);
			System.out.println("--params."+params);
			HashMap requestMap = (HashMap)params.get("requestMap");
			localeLanguage	   = requestMap.get("languageStr").toString();
HashMap selectDataMap = null;		
			if(selectDataMap != null && selectDataMap.size() > 0)
			{
				
				policy = (String)selectDataMap.get("policy");
				policy	=  MCADMxUtil.getNLSName(context, "Policy", policy, "", "", localeLanguage);
			}
			else
			{
				
				String objectId	   = (String) requestMap.get("objectId");
				HashMap dataMap    = this.getSelectDataForTable(context,objectId,localeLanguage); 
				policy = (String)selectDataMap.get("policy");
				policy			   =  MCADMxUtil.getNLSName(context, "Policy", policy, "", "" , localeLanguage);

			}*/
        }
        catch (Exception e)
        {            
        }
        return policy;
    }
	
	public String getPolicyForObject(Context context, String[] args) throws Exception
    {
		String policy			= null;
		String localeLanguage	= null;
		String objectId			= null;
		try
        {  		
        	HashMap params				= (HashMap)JPO.unpackArgs(args);

			HashMap requestMap			= (HashMap)params.get("requestMap");	
			localeLanguage				= requestMap.get("languageStr").toString();
			objectId					= (String) requestMap.get("objectId");
			String[] objIds				=	new String[1];
			objIds[0]					= objectId;
			StringList busSelectionList	= new StringList();		
			
			busSelectionList.add("policy");
			BusinessObjectWithSelectList busWithSelectionList 	= BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);
			BusinessObjectWithSelect busObjectWithSelect 		= (BusinessObjectWithSelect)busWithSelectionList.elementAt(0);
			policy 	=  busObjectWithSelect.getSelectData("policy") ;                             
			policy 	=  MCADMxUtil.getNLSName(context, "Policy", policy, "", "", localeLanguage);
        }
        catch (Exception e)
        {   
			e.printStackTrace();
        }
        return policy;
    }

	public String getOwner(Context context, String[] args) throws Exception
    {
		String owner = null;
		String localeLanguage = null;
       
        try
        { 
				HashMap params	   = (HashMap)JPO.unpackArgs(args);

				HashMap requestMap = (HashMap)params.get("requestMap");	
				String objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
		localeLanguage	   = requestMap.get("languageStr").toString();
		HashMap hmData = (HashMap)hmObjInfo.get(objectId);
		if(hmData!=null && hmData.size() > 0)
		{
			owner		   = (String)hmData.get("owner");
		
		} else {
						HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

hmObjInfo.put(objectId,dataMap);
owner		   = (String)dataMap.get("owner");
		}
				owner			   =  MCADMxUtil.getNLSName(context, MCADMxUtil.getActualNameForAEFData(context, "type_Person"), owner, "", "" , localeLanguage);
		
/*			HashMap params	   = (HashMap)JPO.unpackArgs(args);
			System.out.println("--params."+params);
			HashMap requestMap = (HashMap)params.get("requestMap");
			localeLanguage	   = requestMap.get("languageStr").toString();
HashMap selectDataMap = null;		
			if(selectDataMap != null && selectDataMap.size() > 0)
			{
				owner = (String)selectDataMap.get("owner");
				owner =  MCADMxUtil.getNLSName(context, MCADMxUtil.getActualNameForAEFData(context, "type_Person"), owner, "", "", localeLanguage);

			}else
			{
				String objectId	   = (String) requestMap.get("objectId");
				HashMap dataMap    = this.getSelectDataForTable(context,objectId,localeLanguage); 
				owner			   = (String)selectDataMap.get("owner");
				owner			   =  MCADMxUtil.getNLSName(context, MCADMxUtil.getActualNameForAEFData(context, "type_Person"), owner, "", "" , localeLanguage);

			}*/
        }
        catch (Exception e)
        {            
        }
        return owner;
    }

	public String getTitle(Context context, String[] args) throws Exception
    {
		String title = null;
       
        try
        {  

				HashMap params	   = (HashMap)JPO.unpackArgs(args);

				HashMap requestMap = (HashMap)params.get("requestMap");	
				String objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
		//localeLanguage	   = requestMap.get("languageStr").toString();
		HashMap hmData = (HashMap)hmObjInfo.get(objectId);
		if(hmData!=null && hmData.size() > 0)
		{
			title		   = (String)hmData.get("title");
		
		} else {
						HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

hmObjInfo.put(objectId,dataMap);
title		   = (String)dataMap.get("title");
		}

		
/*HashMap selectDataMap = null;				
			if(selectDataMap != null && selectDataMap.size() > 0)
			{
				title = (String)selectDataMap.get("title");				
			}
			else
			{
				HashMap params	   = (HashMap)JPO.unpackArgs(args);
				System.out.println("--params."+params);
				HashMap requestMap = (HashMap)params.get("requestMap");	
				String objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
				HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 
				title			   = (String)selectDataMap.get("title");
			}*/
        }
        catch (Exception e)
        {            
        }
        return title;
    }

	public String getRenamedFrom(Context context, String[] args) throws Exception
    {
		String renamedFrom = null;
       
        try
        {

		
				HashMap params	   = (HashMap)JPO.unpackArgs(args);

				HashMap requestMap = (HashMap)params.get("requestMap");	
				String objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
		//localeLanguage	   = requestMap.get("languageStr").toString();
		HashMap hmData = (HashMap)hmObjInfo.get(objectId);
		if(hmData!=null && hmData.size() > 0)
		{
			renamedFrom		   = (String)hmData.get("renamedFrom");
		
		} else {
						HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

hmObjInfo.put(objectId,dataMap);
renamedFrom		   = (String)dataMap.get("renamedFrom");
		}

/*  		
HashMap selectDataMap = null;				
			if(selectDataMap != null && selectDataMap.size() > 0)
			{
				renamedFrom = (String)selectDataMap.get("renamedFrom");
			}else
			{
				HashMap params	   = (HashMap)JPO.unpackArgs(args);
				System.out.println("--params."+params);
				HashMap requestMap = (HashMap)params.get("requestMap");	
				String objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
				HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 
				renamedFrom		   = (String)selectDataMap.get("renamedFrom");
			}*/
        }
        catch (Exception e)
        {            
        }
        return renamedFrom;
    }

	public String getLocker(Context context, String[] args) throws Exception
    {
		String locker = null;
		String localeLanguage = null;

       
        try
        {  	

				HashMap params	   = (HashMap)JPO.unpackArgs(args);

				HashMap requestMap = (HashMap)params.get("requestMap");	
				String objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
		localeLanguage	   = requestMap.get("languageStr").toString();
		HashMap hmData = (HashMap)hmObjInfo.get(objectId);
		if(hmData!=null && hmData.size() > 0)
		{
			locker		   = (String)hmData.get("locker");
		
		} else {
						HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

hmObjInfo.put(objectId,dataMap);
locker		   = (String)dataMap.get("locker");
		}
				locker =  MCADMxUtil.getNLSName(context, MCADMxUtil.getActualNameForAEFData(context, "type_Person"), locker, "", "" , localeLanguage);
		
			/*HashMap params	   = (HashMap)JPO.unpackArgs(args);
			System.out.println("--params."+params);
			HashMap requestMap = (HashMap)params.get("requestMap");
			localeLanguage	   = requestMap.get("languageStr").toString();
			HashMap selectDataMap = null;		
			if(selectDataMap != null && selectDataMap.size() > 0)
			{
				locker = (String)selectDataMap.get("locker");
				locker =  MCADMxUtil.getNLSName(context, MCADMxUtil.getActualNameForAEFData(context, "type_Person"), locker, "", "" , localeLanguage);

				
			}else
			{
				String objectId	   = (String) requestMap.get("objectId");
				HashMap dataMap    = this.getSelectDataForTable(context,objectId,localeLanguage); 
				locker			   = (String)selectDataMap.get("locker");
				locker =  MCADMxUtil.getNLSName(context, MCADMxUtil.getActualNameForAEFData(context, "type_Person"), locker, "", "", localeLanguage);


			}*/
        }
        catch (Exception e)
        {            
        }
        return locker;
    }

	public String getCurrent(Context context, String[] args) throws Exception
    {
		String state = null;
		String policy= null;
		String localeLanguage = null;
       
        try
        {  		

				HashMap params	   = (HashMap)JPO.unpackArgs(args);

				HashMap requestMap = (HashMap)params.get("requestMap");	
				String objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
		localeLanguage	   = requestMap.get("languageStr").toString();
		HashMap hmData = (HashMap)hmObjInfo.get(objectId);
		if(hmData!=null && hmData.size() > 0)
		{
			state		   = (String)hmData.get("state");
			policy	= (String)hmData.get("policy");
		} else {
						HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

hmObjInfo.put(objectId,dataMap);
state		   = (String)dataMap.get("state");
policy	= (String)dataMap.get("policy");
		}

state	=  MCADMxUtil.getNLSName(context, "State", state, "Policy", policy , localeLanguage);   
		
			/*HashMap params	    = (HashMap)JPO.unpackArgs(args);
			System.out.println("--params."+params);
			HashMap requestMap	= (HashMap)params.get("requestMap");
			localeLanguage		= requestMap.get("languageStr").toString();
			HashMap selectDataMap = null;		
			if(selectDataMap != null && selectDataMap.size() > 0)
			{				
				policy	= (String)selectDataMap.get("policy");
				state = (String)selectDataMap.get("state");
				state	=  MCADMxUtil.getNLSName(context, "State", state, "Policy", policy , localeLanguage);   
			}
			else
			{
				String objectId	   = (String) requestMap.get("objectId");
				HashMap dataMap    = this.getSelectDataForTable(context,objectId,localeLanguage); 
				policy			= (String)dataMap.get("policy");
				state			= (String)dataMap.get("state");
				state			=  MCADMxUtil.getNLSName(context, "State", state, "Policy", policy , localeLanguage);
			}*/
        }
        catch (Exception e)
        {            
        }
        return state;
    }


public String getCurrentForObject(Context context, String[] args) throws Exception
    {
		String state			= null;
		String policy			= null;
		String localeLanguage	= null;
		String objectId			= null;
		try
        {  		
        	HashMap params				= (HashMap)JPO.unpackArgs(args);

			HashMap requestMap			= (HashMap)params.get("requestMap");	
			localeLanguage				= requestMap.get("languageStr").toString();
			objectId					= (String) requestMap.get("objectId");
			String[] objIds				=	new String[1];
			objIds[0]					= objectId;
			StringList busSelectionList	= new StringList();		
			
			busSelectionList.add("policy");
			busSelectionList.add("current");

			BusinessObjectWithSelectList busWithSelectionList 	= BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);
			BusinessObjectWithSelect busObjectWithSelect 		= (BusinessObjectWithSelect)busWithSelectionList.elementAt(0);
			policy 	=  busObjectWithSelect.getSelectData("policy") ;  
			state 	=  busObjectWithSelect.getSelectData("current") ; 
			state	=  MCADMxUtil.getNLSName(context, "State", state, "Policy", policy , localeLanguage);
			
        }
        catch (Exception e)
        {   
			e.printStackTrace();
        }
        return state;
    }
	public String getType(Context context, String[] args) throws Exception
    {
		String type = null;
		String localeLanguage	= null;
		String objectId		= null;
       
        try
        {

				HashMap params	   = (HashMap)JPO.unpackArgs(args);

				HashMap requestMap = (HashMap)params.get("requestMap");	
				objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
		localeLanguage	   = requestMap.get("languageStr").toString();
		HashMap hmData = (HashMap)hmObjInfo.get(objectId);
		if(hmData!=null && hmData.size() > 0)
		{
			type		   = (String)hmData.get("type");
		
		} else {
						HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

hmObjInfo.put(objectId,dataMap);
type		   = (String)dataMap.get("type");
		}
type =  MCADMxUtil.getNLSName(context, "Type", type, "", "" , localeLanguage);	

	/*	
			 
			HashMap params	   = (HashMap)JPO.unpackArgs(args);
			System.out.println("--params."+params);
			HashMap requestMap = (HashMap)params.get("requestMap");	
			localeLanguage	   = requestMap.get("languageStr").toString();
HashMap selectDataMap = null;		
System.out.println("---selectDataMap--"+selectDataMap);			

			if(selectDataMap != null && selectDataMap.size() > 0)
			{	
                                type = (String)selectDataMap.get("type");
                                type =  MCADMxUtil.getNLSName(context, "Type", type, "", "" , localeLanguage);
			}else
			{
                                objectId	   = (String) requestMap.get("objectId");
				HashMap dataMap    = this.getSelectDataForTable(context,objectId,localeLanguage); 
				type = (String)selectDataMap.get("type");
				type =  MCADMxUtil.getNLSName(context, "Type", type, "", "" , localeLanguage);
			}*/
        }
        catch (Exception e)
        {            
        }
        return type;
    }
	
	public String getTypeForObject(Context context, String[] args) throws Exception
    {
		String type				= null;
		String localeLanguage	= null;
		String objectId			= null;
       
        try
        {						
			HashMap params				= (HashMap)JPO.unpackArgs(args);

			HashMap requestMap			= (HashMap)params.get("requestMap");	
			localeLanguage				= requestMap.get("languageStr").toString();
			objectId					= (String) requestMap.get("objectId");
			String[] objIds				=	new String[1];
			objIds[0]					= objectId;
			StringList busSelectionList	= new StringList();	
			
			busSelectionList.add("type");
			BusinessObjectWithSelectList busWithSelectionList 	= BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);
			BusinessObjectWithSelect busObjectWithSelect 		= (BusinessObjectWithSelect)busWithSelectionList.elementAt(0);
			type 		=  busObjectWithSelect.getSelectData("type") ;                             
			type 		=  MCADMxUtil.getNLSName(context, "Type", type, "", "" , localeLanguage);
        }
        catch (Exception e)
        {    
			e.printStackTrace();
        }
        return type;
    }

	public String getCheckinComment(Context context, String[] args) throws Exception
    {
		String checkInComment = null;
		String objectId = null;
        try
        {
  				HashMap params	   = (HashMap)JPO.unpackArgs(args);

				HashMap requestMap = (HashMap)params.get("requestMap");	
				objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
		//localeLanguage	   = requestMap.get("languageStr").toString();
		HashMap hmData = (HashMap)hmObjInfo.get(objectId);
		if(hmData!=null && hmData.size() > 0)
		{
			checkInComment		   = (String)hmData.get("checkInComment");
		
		} else {
						HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

hmObjInfo.put(objectId,dataMap);
checkInComment		   = (String)dataMap.get("checkInComment");
		}

/*HashMap selectDataMap = null;				
			if(selectDataMap != null && selectDataMap.size() > 0)
			{
				checkInComment = (String)selectDataMap.get("checkInComment");
			}
			else
			{
				HashMap params	   = (HashMap)JPO.unpackArgs(args);
				System.out.println("--params."+params);
				HashMap requestMap = (HashMap)params.get("requestMap");	
				String objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
				HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

				checkInComment = (String)selectDataMap.get("checkInComment");
			}*/
        }
        catch (Exception e)
        {
        }
        return checkInComment;
    }

	public String getDescription(Context context, String[] args) throws Exception
    {
		String description = null;
		String objectId = null;
        try
        {

		
  				HashMap params	   = (HashMap)JPO.unpackArgs(args);

				HashMap requestMap = (HashMap)params.get("requestMap");	
				objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
		//localeLanguage	   = requestMap.get("languageStr").toString();
		HashMap hmData = (HashMap)hmObjInfo.get(objectId);
		if(hmData!=null && hmData.size() > 0)
		{
			description		   = (String)hmData.get("description");
		
		} else {
						HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

hmObjInfo.put(objectId,dataMap);
description		   = (String)dataMap.get("description");
		}
/*		
HashMap selectDataMap = null;				
			if(selectDataMap != null && selectDataMap.size() > 0)
			{
				description = (String)selectDataMap.get("description");
			}
			else
			{
				HashMap params	   = (HashMap)JPO.unpackArgs(args);
				System.out.println("--params."+params);
				HashMap requestMap = (HashMap)params.get("requestMap");	
				String objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
				HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

				description = (String)selectDataMap.get("description");
			}*/
        }
        catch (Exception e)
        {            
        }
        return description;
    }

	public String getName(Context context, String[] args) throws Exception
    {
		String name = null;
 		String objectId = null;      
        try
        {  

		  				HashMap params	   = (HashMap)JPO.unpackArgs(args);

				HashMap requestMap = (HashMap)params.get("requestMap");	
				objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
		//localeLanguage	   = requestMap.get("languageStr").toString();
		HashMap hmData = (HashMap)hmObjInfo.get(objectId);
		if(hmData!=null && hmData.size() > 0)
		{
			name		   = (String)hmData.get("name");
		
		} else {
						HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

hmObjInfo.put(objectId,dataMap);
name		   = (String)dataMap.get("name");
		}
		
/*
		
HashMap selectDataMap = null;				
			if(selectDataMap != null && selectDataMap.size() > 0)
			{
				name = (String)selectDataMap.get("name");
			}
			else
			{
				HashMap params	   = (HashMap)JPO.unpackArgs(args);
				System.out.println("--params."+params);
				HashMap requestMap = (HashMap)params.get("requestMap");	
				String objectId	   = (String) requestMap.get("objectId");
				String language	   = (String) requestMap.get("languageStr");
				HashMap dataMap    = this.getSelectDataForTable(context,objectId,language); 

				name = (String)dataMap.get("name");
			}*/
        }
        catch (Exception e)
        {
			e.printStackTrace();
        }
        return name;
    }


	
	private HashMap getSelectDataForTable(Context context , String objectId, String language) throws Exception
	{		
        _serverResourceBundle = new MCADServerResourceBundle(language);
		_cache				  = new IEFGlobalCache();
		_util				  = new MCADMxUtil(context, _serverResourceBundle, _cache);

		String REL_VERSION_OF	 = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		String renamedFrom	     = MCADMxUtil.getActualNameForAEFData(context, "attribute_RenamedFrom");
		String title			 = MCADMxUtil.getActualNameForAEFData(context, "attribute_Title");		
		String checkInComment	 = MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-Comment");
		String SELECT_ON_MAJOR	 = "from[" + REL_VERSION_OF + "].to.";
		String[] objIds			 =	new String[1];
		objIds[0]				 = objectId;

		StringList busSelectionList		= new StringList();				
		busSelectionList.addElement("id");		
		busSelectionList.addElement(SELECT_ON_MAJOR + "revision");
		busSelectionList.addElement(SELECT_ON_MAJOR + "revisions.policy");
		busSelectionList.addElement(SELECT_ON_MAJOR + "revisions.current");
		busSelectionList.addElement(SELECT_ON_MAJOR + "locker");
		busSelectionList.addElement(SELECT_ON_MAJOR + "owner");
		busSelectionList.add(SELECT_ON_MAJOR + "attribute[" + renamedFrom + "]");
		busSelectionList.add(SELECT_ON_MAJOR + "attribute[" + title + "]");
		busSelectionList.add(SELECT_ON_MAJOR + "attribute[" + checkInComment + "]");
		busSelectionList.add(SELECT_ON_MAJOR+"id");
		busSelectionList.add(SELECT_ON_MAJOR + "type");
		busSelectionList.add(SELECT_ON_MAJOR + "description");
		busSelectionList.add(SELECT_ON_MAJOR + "name");

		busSelectionList.addElement("revision");
		busSelectionList.addElement("revisions.policy"); 
		busSelectionList.addElement("revisions.current");
		busSelectionList.addElement("locker");
		busSelectionList.addElement("owner");
		busSelectionList.add("attribute[" + renamedFrom + "]");
		busSelectionList.add("attribute[" + title + "]");
		busSelectionList.add("attribute[" + checkInComment + "]");
		busSelectionList.add("type");
		busSelectionList.add("description");
		busSelectionList.add("name");
		
			
		BusinessObjectWithSelectList busWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);
HashMap selectDataMap =  new HashMap();
		if(busWithSelectionList != null && busWithSelectionList.size() > 0)
		{
			BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)busWithSelectionList.elementAt(0);

			if(isMinorType(context,objectId,language))
			{
				selectDataMap.put("revision",busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revision"));
				String revision = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revision");	
				selectDataMap.put("policy",busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + revision + "].policy"));
				selectDataMap.put("owner",busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "owner"));
				selectDataMap.put("locker",busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "locker"));
				selectDataMap.put("renamedFrom",busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "attribute[" + renamedFrom + "]"));
				selectDataMap.put("title",busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "attribute[" + title + "]"));				
				selectDataMap.put("checkInComment",busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "attribute[" + checkInComment + "]"));
				selectDataMap.put("state",busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + revision + "].current"));
				selectDataMap.put("type",busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "type"));
				selectDataMap.put("description",busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "description"));
				selectDataMap.put("name",busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "name"));				
			}else
			{
				selectDataMap.put("revision",busObjectWithSelect.getSelectData("revision"));
				String revision = busObjectWithSelect.getSelectData("revision");	
				selectDataMap.put("policy",busObjectWithSelect.getSelectData("revisions[" + revision + "].policy"));
				selectDataMap.put("owner",busObjectWithSelect.getSelectData("owner"));
				selectDataMap.put("locker",busObjectWithSelect.getSelectData("locker"));
				selectDataMap.put("renamedFrom",busObjectWithSelect.getSelectData("attribute[" + renamedFrom + "]"));
				selectDataMap.put("title",busObjectWithSelect.getSelectData("attribute[" + title + "]"));				
				selectDataMap.put("checkInComment",busObjectWithSelect.getSelectData("attribute[" + checkInComment + "]"));
				selectDataMap.put("state",busObjectWithSelect.getSelectData("revisions[" + revision + "].current"));
				selectDataMap.put("type",busObjectWithSelect.getSelectData("type"));
				selectDataMap.put("description",busObjectWithSelect.getSelectData("description"));
				selectDataMap.put("name",busObjectWithSelect.getSelectData("name"));
			}
		}

		return selectDataMap;

	}

	private boolean isMinorType(Context context, String objectId, String language)
	{
		boolean isMinor = false;
		String type		= null;
		try
		{
			MCADMxUtil util			= new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());
			
			// [NDM] Start OP6
			/*String integrationName  = util.getIntegrationName(context, objectId);
			
			IEFSimpleConfigObject simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, integrationName);
			Hashtable typeMapping			= simpleGCO.getAttributeAsHashtable(MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-BusTypeMapping"), "\n", "|");

			Vector mxTypes = new Vector();
			
			Enumeration enumTypeMapping = typeMapping.elements() ;
			while(enumTypeMapping.hasMoreElements())
			{
				String sTypes = (String)enumTypeMapping.nextElement();
				StringTokenizer tokenizer = new StringTokenizer(sTypes, ",");
				while(tokenizer.hasMoreElements())
				{
				   String sType   = (String) tokenizer.nextElement();
				   mxTypes.addElement(sType.trim());
				}			
			}
			BusinessObject busObj = new BusinessObject(objectId);
			busObj.open(context);
			type = busObj.getTypeName();
			busObj.close(context);

			if(!mxTypes.contains(type))
				isMinor = true;*/
			
			if(!util.isMajorObject(context, objectId))
				isMinor = true;
			
			// [NDM] End OP6
		}
		catch(Throwable e)
		{
			e.printStackTrace();
		}
		return isMinor;
	}

    public String getVault(Context context, String[] args) throws Exception
    {
		String vault = null;
		String localeLanguage = null;
       
        try
        {						
				
				HashMap params	   = (HashMap)JPO.unpackArgs(args);

				HashMap requestMap = (HashMap)params.get("requestMap");
			localeLanguage			= requestMap.get("languageStr").toString();
				String objectId = requestMap.get("objectId").toString();
				BusinessObject busObj		= new BusinessObject(objectId);
				busObj.open(context);
				vault = busObj.getVault().toString();
				vault =  MCADMxUtil.getNLSName(context, "Vault", vault, "", "" , localeLanguage);
				busObj.close(context);
			
        }
        catch (Exception e)
        {       
                 e.printStackTrace();
        }
        return vault;
    }

	public Object getFileModifiedDate(Context context, String[] args) throws Exception
	{
		Vector retunValues = new Vector();

		HashMap params	   	= (HashMap)JPO.unpackArgs(args);

		HashMap paramList 	= (HashMap) params.get("paramList");

		List objectList   	= (List) params.get("objectList"); 
		String[] objIds		=	new String[objectList.size()];

		for(int i=0; i<objectList.size();i++)
		{
			Map idsMap   	= (Map) objectList.get(i);
			String objectId = idsMap.get("id").toString();
			objIds[i]		= objectId;
		}		

		String localeLanguage	= (String)paramList.get("LocaleLanguage");

		_util				  	= new MCADMxUtil(context,new MCADServerResourceBundle(localeLanguage), new IEFGlobalCache());
		
		String SELECT_ON_MODIFIED_DATE  = "format.file.modified.generic";
		String HAS_FILE 				= "format.hasfile";
		
		String REL_ACTIVE_VERSION   = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
		
		StringBuffer SELECT_ON_ACTIVE_MINOR = new StringBuffer("from[");
		SELECT_ON_ACTIVE_MINOR.append(REL_ACTIVE_VERSION);
		SELECT_ON_ACTIVE_MINOR.append("].to.");
		
		String strSELECT_ON_ACTIVE_MINOR = SELECT_ON_ACTIVE_MINOR.toString();
		
		StringBuffer MINOR_HAS_FILE = new StringBuffer(strSELECT_ON_ACTIVE_MINOR);
		MINOR_HAS_FILE.append(HAS_FILE);		
		StringBuffer SELECT_ON_MODIFIED_DATE_MINOR = new StringBuffer(strSELECT_ON_ACTIVE_MINOR); 
		SELECT_ON_MODIFIED_DATE_MINOR.append(SELECT_ON_MODIFIED_DATE);
		
		String REL_VERSION_OF = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		StringBuffer SELECT_ON_MAJOR = new StringBuffer("from[");
		SELECT_ON_MAJOR.append(REL_VERSION_OF);
		SELECT_ON_MAJOR.append("].to.");
		StringBuffer MAJOR_HAS_FILE = new StringBuffer(SELECT_ON_MAJOR.toString());
		MAJOR_HAS_FILE.append(HAS_FILE);
		StringBuffer SELECT_ON_MODIFIED_DATE_MAJOR = new StringBuffer(SELECT_ON_MAJOR.toString());
		SELECT_ON_MODIFIED_DATE_MAJOR.append(SELECT_ON_MODIFIED_DATE);
		
		String REL_LATEST_VERSION = MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion");
		StringBuffer SELECT_ON_LATEST_VERSION = new StringBuffer("to[");
		SELECT_ON_LATEST_VERSION.append(REL_LATEST_VERSION);
		SELECT_ON_LATEST_VERSION.append("].from.");
		StringBuffer LATEST_VERSION_HAS_FILE = new StringBuffer(SELECT_ON_LATEST_VERSION.toString());
		LATEST_VERSION_HAS_FILE.append(HAS_FILE);
		StringBuffer SELECT_ON_MODIFIED_DATE_LATEST_VERSION = new StringBuffer(SELECT_ON_LATEST_VERSION.toString());
		SELECT_ON_MODIFIED_DATE_LATEST_VERSION.append(SELECT_ON_MODIFIED_DATE);
		
		StringList  busSelectList   = new StringList();

		busSelectList.add("id");
		busSelectList.add("type");

		busSelectList.add(HAS_FILE);
		busSelectList.add(SELECT_ON_MODIFIED_DATE);
		
		busSelectList.add(MINOR_HAS_FILE.toString());
		busSelectList.add(SELECT_ON_MODIFIED_DATE_MINOR.toString());
						
		busSelectList.add(MAJOR_HAS_FILE.toString());
		busSelectList.add(SELECT_ON_MODIFIED_DATE_MAJOR.toString());
		
		busSelectList.add(LATEST_VERSION_HAS_FILE.toString());
		busSelectList.add(SELECT_ON_MODIFIED_DATE_LATEST_VERSION.toString());
		
		BusinessObjectWithSelectList busWithSelectionList 	= BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectList);

		for(int i = 0; i < busWithSelectionList.size(); i++)
		{
			String modifiedDate = "";
			BusinessObjectWithSelect busObjectWithSelect	= (BusinessObjectWithSelect)busWithSelectionList.elementAt(i);

			String hasFile 			= busObjectWithSelect.getSelectData(HAS_FILE);

			if(hasFile.equalsIgnoreCase(MCADAppletServletProtocol.TRUE))
			{
				modifiedDate = busObjectWithSelect.getSelectData(SELECT_ON_MODIFIED_DATE);
			}
			else
			{
				//String busType 			= busObjectWithSelect.getSelectData("type");
				String objectId 		= busObjectWithSelect.getSelectData("id");
				
				MCADGlobalConfigObject gco 	= this.getGlobalConfigObject(context, objectId, _util);
				MCADServerGeneralUtil serverGeneralUtil = new MCADServerGeneralUtil(context, gco, new MCADServerResourceBundle(localeLanguage), new IEFGlobalCache());
				
				if(_util.isMajorObject(context, objectId))//gco.isMajorType(busType)) // [NDM] OP6
				{
					String minorHasFile = busObjectWithSelect.getSelectData(MINOR_HAS_FILE.toString());
										
					if(minorHasFile.equalsIgnoreCase(MCADAppletServletProtocol.TRUE))
						modifiedDate = busObjectWithSelect.getSelectData(SELECT_ON_MODIFIED_DATE_MINOR.toString());					
				}
				else
				{
					boolean isObjBulkLoaded = serverGeneralUtil.isObjectBulkLoaded(context, objectId);
					if(isObjBulkLoaded)
					{
						String bulkObjHasFile = busObjectWithSelect.getSelectData(LATEST_VERSION_HAS_FILE.toString());
						
						if(bulkObjHasFile.equalsIgnoreCase(MCADAppletServletProtocol.TRUE))
							modifiedDate = busObjectWithSelect.getSelectData(SELECT_ON_MODIFIED_DATE_LATEST_VERSION.toString());
					}
					else
					{	
						String majorHasFile = busObjectWithSelect.getSelectData(MAJOR_HAS_FILE.toString());
					
						if(majorHasFile.equalsIgnoreCase(MCADAppletServletProtocol.TRUE))
							modifiedDate = busObjectWithSelect.getSelectData(SELECT_ON_MODIFIED_DATE_MAJOR.toString());
					}
				}

			}

			retunValues.add(modifiedDate);
		}

		return retunValues;
	}

	private String getGlobalConfigObjectName(Context context, String busId) throws Exception
	{
		// Get the IntegrationName
		IEFGuessIntegrationContext_mxJPO guessIntegration = new IEFGuessIntegrationContext_mxJPO(context, null);
		String jpoArgs[] = new String[1];
		jpoArgs[0] = busId;
		String integrationName = guessIntegration.getIntegrationName(context, jpoArgs);

		// Get the relevant GCO Name 

		String gcoName = null;

		IEFSimpleConfigObject simpleLCO = IEFSimpleConfigObject.getSimpleLCO(context);

		String gcoType  = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
		String attrName = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");
		if(simpleLCO.isObjectExists())
		{
			Hashtable integNameGcoMapping = simpleLCO.getAttributeAsHashtable(attrName, "\n", "|");
			gcoName = (String)integNameGcoMapping.get(integrationName);	        
		}
		else
		{
			IEFGetRegistrationDetails_mxJPO registrationDetailsReader = new IEFGetRegistrationDetails_mxJPO(context, null);
			String args[] = new String[1];
			args[0] = integrationName;
			String registrationDetails = registrationDetailsReader.getRegistrationDetails(context, args);
			gcoName 	           = registrationDetails.substring(registrationDetails.lastIndexOf("|")+1);
		}

		return gcoName;
	}

	protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String objectId, MCADMxUtil mxUtil) throws Exception
	{
		String typeGlobalConfig							= MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
		String gcoName									= this.getGlobalConfigObjectName(context, objectId);

		MCADGlobalConfigObject gcoObject	= null;

		if(gcoName != null && gcoName.length() > 0)
		{
			MCADConfigObjectLoader configLoader	= new MCADConfigObjectLoader(null);
			gcoObject							= configLoader.createGlobalConfigObject(context, mxUtil, typeGlobalConfig, gcoName);
		}
		return gcoObject;
	}

}

