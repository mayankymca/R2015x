/*
**  DECBaselineDetails.java
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**	
**  This  is a JPO which act as a data source for rendering data in to a custom table .
**	Using this JPO program  developer can  create their own column definitions and can return
**	tabledata in a  CustomMapList  which stores each row of table as Map objects.
*/
import matrix.db.Context;

 
public class DECBaselineDetails_mxJPO extends DECBaselineDetailsBase_mxJPO
{	

    /**
	   * Constructor.
	   *
	   * @param context the eMatrix <code>Context</code> object
	   * @param args holds no arguments
	   * @throws Exception if the operation fails
	   * @since Sourcing V6R2008-2
	   */
	public DECBaselineDetails_mxJPO (Context context, String[] args) throws Exception
	{
	  super(context, args);
	}	
}
