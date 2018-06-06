/*
**  IEFEBOMSyncFindMatchingPart
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
*/
import matrix.db.Context;

 
public class IEFEBOMSyncFindMatchingPart_mxJPO extends IEFEBOMSyncFindMatchingPartBase_mxJPO
{
	/**
	   * Constructor.
	   *
	   * @since Sourcing V6R2008-2
	   */
	public IEFEBOMSyncFindMatchingPart_mxJPO ()
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
	public IEFEBOMSyncFindMatchingPart_mxJPO (Context context, String[] args) throws Exception
	{
	  super(context, args);
	}	
}
