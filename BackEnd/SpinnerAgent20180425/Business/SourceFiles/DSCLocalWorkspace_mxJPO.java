/*
**  DSCLocalWorkspace
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Get all the required information about the local workspace objects
*/
import matrix.db.Context;
 
public class DSCLocalWorkspace_mxJPO extends DSCLocalWorkspaceBase_mxJPO
{
	/**
	   * Constructor.
	   *
	   * @since Sourcing V6R2008-2
	   */
	public DSCLocalWorkspace_mxJPO ()
	{
	  super();
	}

    /**
	   * Constructor.
	   *
	   * @param context the eMatrix <code>Context</code> object
	   * @param args holds no arguments
	   * @throws Exception if the operation fails
	   * @since Sourcing V6R2008-2
	   */
	public DSCLocalWorkspace_mxJPO (Context context, String[] args) throws Exception
	{
	  super(context, args);
	}	
}
