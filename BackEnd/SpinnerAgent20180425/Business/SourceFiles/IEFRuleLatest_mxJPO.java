/*
**  IEFRuleLatest
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to get the latest version of the given object.
*/
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class IEFRuleLatest_mxJPO
{
	private MCADGlobalConfigObject globalConfig					= null;
	private MCADServerResourceBundle	serverResourceBundle	= null;
	private IEFGlobalCache cache								= null;	
	private MCADServerGeneralUtil generalUtil					= null;
	private MCADMxUtil util										= null;

	private String REL_VERSION_OF								= "";
	private String REL_ACTIVE_VERSION							= "";
	
	private String SELECT_ON_MAJOR								= "";
	private String SELECT_ON_ACTIVE_MINOR						= "";
	
    public  IEFRuleLatest_mxJPO  ()
    {
    }

    public IEFRuleLatest_mxJPO (Context context, String[] args) throws Exception
    {

         Hashtable argument	= (Hashtable) JPO.unpackArgs(args);

         if (!context.isConnected())
            MCADServerException.createException("not supported no desktop client", null);

		String language				=  (String)argument.get("language");
		MCADGlobalConfigObject gco	= (MCADGlobalConfigObject)argument.get("GCO");

		util			= new MCADMxUtil(context, new MCADServerResourceBundle(language), new IEFGlobalCache());

		init(context, gco, language);
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }

    private void init(Context context, MCADGlobalConfigObject gco, String language)  throws Exception
    {
        this.serverResourceBundle	= new MCADServerResourceBundle(language);
		this.cache					= new IEFGlobalCache();
        this.globalConfig			= gco;
        this.generalUtil			= new MCADServerGeneralUtil(context,globalConfig, serverResourceBundle, cache);

		REL_VERSION_OF				= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
		REL_ACTIVE_VERSION			= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
		
		SELECT_ON_MAJOR				= "from[" + REL_VERSION_OF + "].to.";
		SELECT_ON_ACTIVE_MINOR		= "from[" + REL_ACTIVE_VERSION + "].to.";
    }

    public HashMap applyRule(Context context, String[] oids) throws Exception
    {
		HashMap returnTable				= new HashMap(oids.length);
		HashSet revisionCreatedManually = new HashSet();
		try
		{
			StringList busSelectionList = new StringList();
				
			busSelectionList.addElement("id");
			busSelectionList.addElement("type");
			
			busSelectionList.addElement(SELECT_ON_MAJOR + "last." + SELECT_ON_ACTIVE_MINOR + "id"); // latest minor from minor
			busSelectionList.addElement(SELECT_ON_MAJOR + "last.policy"); // major policy from minor
			busSelectionList.addElement(SELECT_ON_MAJOR + "last.current"); // major state from minor
			busSelectionList.addElement(SELECT_ON_MAJOR + "last.id"); // latest major id from minor
			busSelectionList.addElement(SELECT_ON_MAJOR + "last.state"); // latest major statelist from minor


			busSelectionList.addElement("last." + SELECT_ON_ACTIVE_MINOR + "id"); // latest minor from major
			busSelectionList.addElement("last.policy"); // major policy from minor
			busSelectionList.addElement("last.current"); // major state from minor
			busSelectionList.addElement("last.id"); // latest major id from major
			busSelectionList.addElement("last.state"); // latest major list from major

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, oids, busSelectionList);

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{
				String outputId			 = null;

				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);
				   
				String busID			 = busObjectWithSelect.getSelectData("id");
				
				//String busType			 = busObjectWithSelect.getSelectData("type"); //[NDM] OP6

				if(util.isMajorObject(context, busID))//globalConfig.isMajorType(busType)) //[NDM] OP6
				{
					//handle major inbus
					String latestMajorPolicy		= busObjectWithSelect.getSelectData("last.policy");
					String latestMajorState			= busObjectWithSelect.getSelectData("last.current");
					StringList latestMajorStateList = busObjectWithSelect.getSelectDataList("last.state");

					String finalizationState		= globalConfig.getFinalizationState(latestMajorPolicy);

					if(latestMajorStateList.lastIndexOf(latestMajorState) >= latestMajorStateList.lastIndexOf(finalizationState))
						outputId = busObjectWithSelect.getSelectData("last.id"); // major id 
					else
						outputId = busObjectWithSelect.getSelectData("last." + SELECT_ON_ACTIVE_MINOR + "id"); // minor id 

					if(outputId == null || outputId.equals(""))
						revisionCreatedManually.add(busID);
				}
				else
				{
					//handle minor inbus
					String latestMajorPolicy		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "last.policy");
					String latestMajorState			= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "last.current");
					StringList latestMajorStateList = busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR + "last.state");

					String finalizationState		= globalConfig.getFinalizationState(latestMajorPolicy);

					if(latestMajorStateList.lastIndexOf(latestMajorState) >= latestMajorStateList.lastIndexOf(finalizationState))
						outputId = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "last.id"); // major id 
					else
						outputId = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "last." + SELECT_ON_ACTIVE_MINOR + "id"); // minor id 

					if(outputId == null || outputId.equals(""))
						revisionCreatedManually.add(busID);
				}
					
				if(outputId == null || outputId.equals(""))
					outputId = busID;

				returnTable.put(busID, outputId);
			}

			if(revisionCreatedManually.size() > 0)
			{
				Hashtable newReturnTable =	processManuallyCreatedRevisions(context, revisionCreatedManually);
				returnTable.putAll(newReturnTable);
			}
			
		}
		catch(Exception e)
		{
			System.out.println("[IEFRuleLatest.applyRule]: Error - " + e.getMessage());
		}

		return returnTable;
	}

	private Hashtable processManuallyCreatedRevisions(Context context, HashSet revisionCreatedManually) throws Exception
	{
		Hashtable returnTable = new Hashtable(revisionCreatedManually.size());
		
		try
		{
			String [] oids		  = new String [revisionCreatedManually.size()];
			revisionCreatedManually.toArray(oids);
			
			StringList busSelectionList = new StringList();
					
			busSelectionList.addElement("id");
			busSelectionList.addElement("type");
			
			busSelectionList.addElement(SELECT_ON_MAJOR + "revisions");
			busSelectionList.addElement(SELECT_ON_MAJOR + "revisions." + SELECT_ON_ACTIVE_MINOR + "id"); 
			busSelectionList.addElement(SELECT_ON_MAJOR + "revisions.policy");
			busSelectionList.addElement(SELECT_ON_MAJOR + "revisions.current");
			busSelectionList.addElement(SELECT_ON_MAJOR + "revisions.id");
			busSelectionList.addElement(SELECT_ON_MAJOR + "revisions.state");

			busSelectionList.addElement("revisions"); 
			busSelectionList.addElement("revisions." + SELECT_ON_ACTIVE_MINOR + "id");
			busSelectionList.addElement("revisions.policy");
			busSelectionList.addElement("revisions.current");
			busSelectionList.addElement("revisions.id");
			busSelectionList.addElement("revisions.state");

			BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, oids, busSelectionList);

			for(int i = 0; i < buslWithSelectionList.size(); i++)
			{
				String outputId								 = null;

				BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect)buslWithSelectionList.elementAt(i);

				String busID								 = busObjectWithSelect.getSelectData("id");
					
				//String busType								 = busObjectWithSelect.getSelectData("type");  //[NDM] OP6

				if(util.isMajorObject(context, busID))//globalConfig.isMajorType(busType)) //[NDM] OP6
				{
					StringList majorRevisions				 = busObjectWithSelect.getSelectDataList("revisions");
					for(int j = (majorRevisions.size() - 1); j >=0 ;j--)
					{
						String latestMajorPolicy			 = busObjectWithSelect.getSelectData("revisions[" + majorRevisions.elementAt(j) + "].policy");
						String latestMajorState				 = busObjectWithSelect.getSelectData("revisions[" + majorRevisions.elementAt(j) + "].current");
						StringList latestMajorStateList		 = busObjectWithSelect.getSelectDataList("revisions[" + majorRevisions.elementAt(j) + "].state");

						String finalizationState			 = globalConfig.getFinalizationState(latestMajorPolicy);

						if(latestMajorStateList.lastIndexOf(latestMajorState) >= latestMajorStateList.lastIndexOf(finalizationState))
							outputId = busObjectWithSelect.getSelectData("revisions[" + majorRevisions.elementAt(j) + "].id"); // major id 
						else
							outputId = busObjectWithSelect.getSelectData("revisions[" + majorRevisions.elementAt(j) + "]." + SELECT_ON_ACTIVE_MINOR + "id"); // minor id

						if(outputId != null && !outputId.equals(""))
							break;
					}
				}
				else
				{
					StringList majorRevisions			= busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR + "revisions");
					for(int j = (majorRevisions.size() - 1); j >=0 ;j--)
					{
						String latestMajorPolicy		= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + majorRevisions.elementAt(j) + "].policy");
						String latestMajorState			= busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + majorRevisions.elementAt(j) + "].current");
						StringList latestMajorStateList = busObjectWithSelect.getSelectDataList(SELECT_ON_MAJOR + "revisions[" + majorRevisions.elementAt(j) + "].state");

						String finalizationState		= globalConfig.getFinalizationState(latestMajorPolicy);

						if(latestMajorStateList.lastIndexOf(latestMajorState) >= latestMajorStateList.lastIndexOf(finalizationState))
							outputId = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + majorRevisions.elementAt(j) + "].id"); // major id 
						else
							outputId = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "revisions[" + majorRevisions.elementAt(j) + "]." + SELECT_ON_ACTIVE_MINOR + "id"); // minor id

						if(outputId != null && !outputId.equals(""))
							break;
					}
				}

				if(outputId == null || outputId.equals(""))
						outputId = busID;

				returnTable.put(busID, outputId);
			}
		}
		catch(Exception e)
		{
			System.out.println("[MCADIntegGetLatestVersion.processManuallyCreatedRevisions]: Error - " + e.getMessage());
		}

		return returnTable;
	}
}

