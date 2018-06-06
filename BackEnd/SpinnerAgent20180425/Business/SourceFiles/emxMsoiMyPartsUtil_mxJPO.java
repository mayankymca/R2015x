// emxMsoiMyPartsUtil.java
//
// 
// Copyright (c) 2002 MatrixOne, Inc.
// All Rights Reserved
// This program contains proprietary and trade secret information of
// MatrixOne, Inc.  Copyright notice is precautionary only and does
// not evidence any actual or intended publication of such program.
//
// $Log: emxMsoiMyPartsUtil.java,v $
// Office Integration Menus

import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import matrix.db.*;
import matrix.util.*;

import com.matrixone.apps.framework.ui.*;
import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;
import com.matrixone.apps.common.*;

/**
 * The <code>emxMsoiMyPartsUtil</code> class represents the JPO for
 * obtaining the MS Office integration menus
 *
 * @version AEF 10.5 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxMsoiMyPartsUtil_mxJPO 
{

/**
   * Constructs a new emxMsoiMyPartsUtil JPO object.
   *
   * @param context the eMatrix <code>Context</code> object
   * @param args an array of String arguments for this method
   * @throws Exception if the operation fails
   * @since AEF 10.5
   */
  public emxMsoiMyPartsUtil_mxJPO (Context context, String[] args)
      throws Exception
  {
    // Call the super constructor
    super();
  }
   
/**
   * Get Parts List
   * 
   * @param context
   * @return a MapList of WBS Tasks
   * @throws Exception if the operation fails
   */
  public MapList getParts(Context context, String[] args) throws MatrixException
  { 
		MapList partsList = new MapList();
		//Get the Parts list 
		short sQueryLimit = (short)(java.lang.Integer.parseInt("100"));
		String strType = "Part";
		String strName = "*";
		String strRevision = "*";
		String strVault = "*";;
		StringList select = new StringList(1);
		select.addElement(DomainConstants.SELECT_ID);
		String sbWhereExp = "";

		partsList = DomainObject.findObjects(context, strType,strName, strRevision, "*", strVault, sbWhereExp, "", true, select, sQueryLimit);
	return partsList;
  }
}
