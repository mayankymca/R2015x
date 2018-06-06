/*
 ** ${CLASSNAME}
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved.
 ** This program contains proprietary and trade secret information of
 ** Dassault Systemes.
 ** Copyright notice is precautionary only and does not evidence any actual
 ** or intended publication of such program
 */

import matrix.db.*;
import matrix.util.*;
import java.util.*;
import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;

/**
 * The <code>emxUEBOMEditAllAccessBase</code> class contains implementation code for emxENCActionLinkAccess.
 * @version EC 10.5 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxUEBOMEditAllAccessBase_mxJPO extends emxDomainObject_mxJPO
{

    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object.
     * @param args holds no arguments.
     * @throws Exception if the operation fails.
     * @since x+4.
     *
     */
    public emxUEBOMEditAllAccessBase_mxJPO (Context context, String[] args)
      throws Exception
    {
        super(context, args);

    }

    /**
     * This method is executed if a specific method is not specified.
     *
     * @param context the eMatrix <code>Context</code> object.
     * @param args holds no arguments.
     * @return int.
     * @throws Exception if the operation fails.
     * @since x+4.
     */
    public int mxMain(Context context, String[] args)
      throws Exception
    {
      if (true)
      {
        throw new Exception("must specifyUEBOMActionLinkAccess invocation");
      }
      return 0;
    }

    

    /**
    * To Show Edit ALL link in UEBOM SB Indented Table.
    *
    * @param context the eMatrix <code>Context</code> object.
    * @param args holds objectId.
    * @return Boolean.
    * @throws Exception If the operation fails.
    * @since x+4.
    *
    */
    public Boolean showEBOMIndentedTableEditAll(Context context, String []args) throws Exception
    {
       boolean showCommand = false;
       if(isEBOMIndentedTable(context, args).booleanValue())
       {
            boolean isEBOMModificationAllowed = (isEBOMModificationAllowed(context,args).booleanValue());
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            String selectedProgram = (String)paramMap.get("selectedProgram");
            if(isEBOMModificationAllowed && "emxUnresolvedPart:getUEBOM".equals(selectedProgram))
            {
                showCommand = true;
            }
        }

        return Boolean.valueOf(showCommand);
    }

 

    /**
    * To Show Edit icon/ Edit ALL link in UEBOM emxTable/Indented  Table
    * @param context the eMatrix <code>Context</code> object.
    * @param args holds objectId.
    * @return Boolean.
    * @throws Exception If the operation fails.
    * @since x+4.
    *
    */

    public Boolean isEBOMModificationAllowed(Context context, String[] args)
        throws Exception
    {
         boolean allowChanges = true;
      try
        {
          HashMap paramMap = (HashMap)JPO.unpackArgs(args);
          String parentId      = (String) paramMap.get("objectId");
          //check the parent obj state
          StringList strList  = new StringList(2);
          strList.add(SELECT_CURRENT);
          strList.add("policy");

           DomainObject domObj = new DomainObject(parentId);
           Map map = domObj.getInfo(context,strList);

          String objState = (String)map.get(SELECT_CURRENT);
          String objPolicy = (String)map.get("policy");

          //String propAllowLevel = (String)FrameworkProperties.getProperty("emxUnresolvedEBOM.Part.RestrictPartModification");
          String propAllowLevel = (String)EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOM.Part.RestrictPartModification");
          StringList propAllowLevelList = new StringList();

          if(propAllowLevel != null && !"null".equals(propAllowLevel) && propAllowLevel.length() > 0)
          {
            StringTokenizer stateTok = new StringTokenizer(propAllowLevel, ",");
            while (stateTok.hasMoreTokens())
             {
                String tok = (String)stateTok.nextToken();
                propAllowLevelList.add(FrameworkUtil.lookupStateName(context, objPolicy, tok));
             }
          }
           allowChanges = (!propAllowLevelList.contains(objState));
        }catch (Exception e)
        {
           throw new Exception(e.toString());
        }

        return Boolean.valueOf(allowChanges);
    }
 /**
    * This method returns true if emxUnresolvedEBOM.DisplayEBOMIndentedTable = true.
    * Used to display the commands related to EBOM Indented Table
    *
    *
    * @param context the eMatrix <code>Context</code> object.
    * @param args
    * @return Boolean.
    * @throws Exception If the operation fails.
    * @since x+4.
    *
    */
    public Boolean isEBOMIndentedTable(Context context, String []args) throws Exception
    {
        boolean bShowIndentedTable = false;
        try
        {
            //String sShowIndentedTable = FrameworkProperties.getProperty("emxUnresolvedEBOM.DisplayEBOMIndentedTable");
        	String sShowIndentedTable = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOM.DisplayEBOMIndentedTable");
            if( sShowIndentedTable!=null && "true".equalsIgnoreCase(sShowIndentedTable.trim()) )
            {
                bShowIndentedTable = true;
            }
        }
        catch(Exception e)
        {
            bShowIndentedTable = false;
        }
        return Boolean.valueOf(bShowIndentedTable);
    }

}
