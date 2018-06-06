/*
**  IEFDeleteMinor
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Program to delete minor objects when major objects are deleted.
*/
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpandRelationship;
import matrix.db.ExpandRelationshipItr;
import matrix.db.ExpandRelationshipList;
import matrix.db.Expansion;
import matrix.db.JPO;
import matrix.db.MatrixWriter;
import matrix.db.Visuals;
import matrix.util.MatrixException;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class IEFDeleteMinor_mxJPO
{
	private MatrixWriter _mxWriter = null;

	private String _sObjectID = null;

	private MCADMxUtil _util = null;


	/**
	 * The no-argument constructor.
	 */
	public IEFDeleteMinor_mxJPO()
	{
	}

	/**
	 * Constructor which accepts the Matrix context and an array of String
	 * arguments.
	 */
	public IEFDeleteMinor_mxJPO(Context _context, String[] args) throws Exception
	{
		_mxWriter = new MatrixWriter(_context);
		
		// Create with blank resource bundle, which will get the default locale language.
		_util = new MCADMxUtil(_context, new MCADServerResourceBundle(""), new IEFGlobalCache());

		// Get the OBJECTID of the object in context
        _sObjectID = args[0];
	}

	public int mxMain(Context context, String []args)  throws Exception
	{
		String relVersionOf 		= MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");;
		String relActiveVersion 	= MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");

		try
		{
			BusinessObject busObj = new BusinessObject(_sObjectID);

			//delete minor objects
			deleteRelatedObjects(context, busObj, relVersionOf, "from");

			//delete dummy object for this major object
			deleteRelatedObjects(context, busObj, relActiveVersion, "to");
		}
		catch(Exception e)
		{
			MCADServerException.createException(e.getMessage(), e);
		}
		return 0;
	}

	private void deleteRelatedObjects(Context context, BusinessObject busObj, String relName, String end) throws Exception
	{
		try
		{
			//Define the relationship where clause
			StringBuffer relationshipWhereClause = new StringBuffer();
            relationshipWhereClause.append("(name == const\"");
            relationshipWhereClause.append(relName);
			relationshipWhereClause.append("\"");
			relationshipWhereClause.append(")");
			
			short level = 1;
            Visuals vis = new Visuals();
			
			// Get all the expand relationships of the obejct of type "VersionOf"			
			Expansion expansion = _util.expandBusObject(context, busObj, level, "", relationshipWhereClause.toString(), (short)0, false, vis);

			ExpandRelationshipList filteredRelationshipList = expansion.getRelationships();
            ExpandRelationshipItr expandItr = new ExpandRelationshipItr(filteredRelationshipList);
			
			while (expandItr.next())
            {
                ExpandRelationship expandRel = expandItr.obj();
	            BusinessObject objToDelete = null;
				
				if(end.equals("from"))
				{
					objToDelete = expandRel.getFrom();
				}
				else
				{
					objToDelete = expandRel.getTo();
				}

				// Delete related object 
				if(objToDelete != null)
				{
					//Delete the derived output of the minor before deleting the minor
					String[] init = new String[1];
					init[0] =  objToDelete.getObjectId();
					String[] jpoArgs = new String[] {};

					JPO.invoke(context ,"IEFDeleteDerivedOutput", init, "mxMain", jpoArgs);

					//Now delete the minor
					objToDelete.remove(context);
				}

				//Delete the derived output of the major object
				String[] init		= new String[1];
				init[0]				= _sObjectID;
				String[] jpoArgs	= new String[] {};

				JPO.invoke(context ,"IEFDeleteDerivedOutput", init, "mxMain", jpoArgs);
            }
		}
		catch(MatrixException me)
        {
	    	_mxWriter.write("Matrix Error occurred:" + me.getMessage());
			MCADServerException.createException(me.getMessage(), me);
		}
        catch(Exception me)
        {
	    	_mxWriter.write("Error occurred:" + me.getMessage());
			MCADServerException.createException(me.getMessage(), me);
		}
	}
}

