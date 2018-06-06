/*
 *  ${CLASSNAME}.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */

import matrix.db.Context;
import java.util.*;

public class emxLibraryCentralClassificationAttributes_mxJPO extends emxLibraryCentralClassificationAttributesBase_mxJPO{
    public emxLibraryCentralClassificationAttributes_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }
	
/*
 *  User will customize list according to his usage.
 *  And will return the customized arrayList.
 */
   public ArrayList<String> customizeAttributeList(Context context, ArrayList<String> completeListOfAttributes){
	   
	/* Put your code here*/
	
	return completeListOfAttributes;
   }	
}
