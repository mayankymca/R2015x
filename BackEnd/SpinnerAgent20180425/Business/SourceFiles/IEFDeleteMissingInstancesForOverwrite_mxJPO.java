/*
**  IEFDeleteMissingInstancesForOverwrite.java
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**	
**  This JPO provides hook for deletion of missing instances in case of overwrite
**  @since Sourcing R208
*/


import matrix.db.Context;

public class IEFDeleteMissingInstancesForOverwrite_mxJPO extends IEFDeleteMissingInstancesForOverwriteBase_mxJPO
{
	/**
	   * Constructor.
	   *
	   * @param context the eMatrix <code>Context</code> object
	   * @param args
	   * @throws Exception if the operation fails
	   * @since Sourcing R208
	*/

	public IEFDeleteMissingInstancesForOverwrite_mxJPO(Context context, String[] args) throws Exception
	{
		super(context, args);
	}	
}
