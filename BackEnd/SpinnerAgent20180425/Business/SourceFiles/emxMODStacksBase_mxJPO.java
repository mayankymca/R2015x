/*
 ** ${CLASSNAME}
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved.
 ** This program contains proprietary and trade secret information of
 ** Dassault Systemes.
 ** Copyright notice is precautionary only and does not evidence any actual
 ** or intended publication of such program
 */
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;

import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.*;
import com.matrixone.apps.effectivity.EffectivityFramework;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.unresolvedebom.*;
import com.matrixone.apps.domain.util.XSSUtil;

/**
 * The <code>emxMODStackBase</code> class contains code for the "MOD Satck"
 *
 *
 */
  /**
 * @author QWM
 *
 * The emxMODStacksBase class contains ...
 *
 */
public class emxMODStacksBase_mxJPO extends emxAEFFullSearch_mxJPO
  {
      /**
       * Constructor.
       *
       * @param context the eMatrix <code>Context</code> object.
       * @param args holds no arguments.
       * @throws Exception if the operation fails.
       *
       */
	//2011x - Starts
	private static final String ATTRIBUTE_MOD_STACKS =
        									PropertyUtil.getSchemaProperty("attribute_MODStacks");
	//Start:0668852011x
	private EffectivityFramework effectivity  = new EffectivityFramework();

	private static final String MODSTACK_ID = "modId";
	private static final String APPLICABILITY_ID = "applicabilityId";

	//End:0668852011x
	//2011x - Ends

      public emxMODStacksBase_mxJPO (Context context, String[] args)
          throws Exception
      {
         super(context,args);
      }

      /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getMODStacks(Context context, String[] args) throws Exception {

          HashMap programMap = (HashMap) JPO.unpackArgs(args);
          String strObjectId = (String)  programMap.get("objectId");
          String sPUEECOType =
              PropertyUtil.getSchemaProperty(context,"type_PUEECO");
          MODStacks modStack = new MODStacks(context, DomainObject.newInstance(context, strObjectId).getInfo(context, "physicalid"), sPUEECOType);
          return modStack.calculateMODStacks();
      }


      /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getMODStackIds(Context context, String[] args) throws Exception{

          StringList result = new StringList();

          Map paramMap = (HashMap) JPO.unpackArgs(args);
          MapList objectList = (MapList)paramMap.get("objectList");
          String modStackId = "";
          if(objectList != null && objectList.size()>0) {
              for(int i=0;i<objectList.size();i++) {
                  Map mapMod = (HashMap)objectList.get(i);
                  modStackId = (String)mapMod.get("id");
                  modStackId = modStackId.substring(0,modStackId.indexOf('|'));
                  result.addElement(modStackId);
              }
          }
          return result;
      }

      /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getMODStackApplicability (Context context, String[] args) throws Exception{

          StringList result   = new StringList();
          //Start:IR-070522V62011x
          //String INF          = FrameworkProperties.getProperty("emxUnresolvedEBOM.UnitRangeNotation.Subsequent.Value");
          String INF          = EnoviaResourceBundle.getProperty(context,"emxUnresolvedEBOM.UnitRangeNotation.Subsequent.Value");
          //String displayINF   = FrameworkProperties.getProperty("emxEffectivity.Display.Infinity");
          String displayINF   = EnoviaResourceBundle.getProperty(context,"emxEffectivity.Display.Infinity");
          //Ends-070522V62011x
          Map paramMap = (HashMap) JPO.unpackArgs(args);
          MapList objectList = (MapList)paramMap.get("objectList");
          if(objectList != null && objectList.size()>0) {
              for(int i=0;i<objectList.size();i++) {
            	  Map mapMod  = (HashMap)objectList.get(i);
            	 //Start:IR-070522V62011x
            	  String EFF  = (String)mapMod.get("applicability");
            	  		 EFF  =  EFF.indexOf(INF)!=-1?FrameworkUtil.findAndReplace(EFF,INF,displayINF):EFF;
            	  //End:IR-070522V62011x
            	  result.addElement(EFF);
              }
          }
          return result;

      }

      /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getMODStackPUEChanges (Context context, String[] args) throws Exception{

          StringList result = new StringList();

          Map paramMap = (HashMap) JPO.unpackArgs(args);
          MapList objectList = (MapList)paramMap.get("objectList");
          StringBuffer sbChange = new StringBuffer();
          if(objectList != null && objectList.size()>0) {
              String pueName = "";
              for(int i=0;i<objectList.size();i++) {
                  Map mapMod = (HashMap)objectList.get(i);
                  MapList mlChanges = (MapList)(mapMod.get("effectedPUEChanges"));

                  for(int j=0;j<mlChanges.size();j++)
                  {
                      Map change = (Map)mlChanges.get(j);
                      pueName = (String)change.get("name");
                      if("NEW_CCA".equals(pueName))
                      {
                          sbChange.append(pueName);
                          sbChange.append("&nbsp;");
	                   } else if (change.containsKey("mode"))
	                      {
	                    	  sbChange.append("<B><a href=\"JavaScript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId="+ change.get("id") +"', '700', '600', 'false', 'popup', '')\">"+XSSUtil.encodeForHTMLAttribute(context, pueName)+"</a> <B>&nbsp;");
	                      } else
		                      {
		                        sbChange.append("<a href=\"JavaScript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId="+ change.get("id") +"', '700', '600', 'false', 'popup', '')\">"+XSSUtil.encodeForHTMLAttribute(context,pueName)+"</a>&nbsp;");
		                      }
                   }
                  result.addElement(sbChange.toString());
                  sbChange = new StringBuffer();
              }
          }
          return result;
      }

      /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getMODStackStatus (Context context, String[] args) throws Exception{

          StringList result = new StringList();

          Map paramMap = (HashMap) JPO.unpackArgs(args);
          MapList objectList = (MapList)paramMap.get("objectList");
          if(objectList != null && objectList.size()>0) {
              for(int i=0;i<objectList.size();i++) {
                  Map mapMod = (HashMap)objectList.get(i);
                  result.addElement((String)mapMod.get("status"));
              }
          }
          return result;
      }

/**
 * This method is invoked to get the names of the MOD Stacks for the PUE ECOs in the Unit Changes tab of Engineering Changes PowerView
 * @author KQD
 * @param context
 * @param args
 * @return StringList
 * @throws Exception
 */
    public StringList getPUEECORelatedMODStacks (Context context, String[] args) throws Exception{

		  StringList result  = new StringList();
		  HashMap programMap = (HashMap)JPO.unpackArgs(args);
		  MapList objectList = (MapList) programMap.get("objectList");
		  HashMap paramList  = (HashMap) programMap.get("paramList");
		  String productID   = (String)paramList.get("objectId");
		  String strType = PropertyUtil.getSchemaProperty(context,"type_PUEECO");
		  MODStacks modStack = new MODStacks(context,DomainObject.newInstance(context, productID).getInfo(context, "physicalid"),strType);
        String sChangeId;
        String strTemp;

        try{
            HashMap hmMODStackName        = modStack.getPUEChangeRelatedMODStacks();
            for(int j=0; j < objectList.size();j++)
              {
               strTemp = " ";
               sChangeId = ( String )((Map)objectList.get(j)).get("id");
               if(hmMODStackName.get(sChangeId) != null)
                 {
                       strTemp = (String)hmMODStackName.get(sChangeId);
                 }
              result.add(strTemp );
             }
        }catch(Exception e){}

        return result;

    }
/**
 * Update method to calculate modstacks for change id.
 * @param context the eMatrix code context object
 * @param args packed hashMap of request parameters contains objectId ,old and new expression values.
 * @throws FrameworkException if the operation fails
 * @throws Exception  if the operation fails
 */
        public void updateMODStacksforPUE(Context context, String[] args) throws FrameworkException , Exception {
        	try {
				//ComponentsUtil.checkLicenseReserved(context, "ENO_ENG_TP");
				ComponentsUtil.checkLicenseReserved(context, "ENO_XCE_TP");
        	} catch (Exception e) {
                throw new FrameworkException(e.getMessage());
            }

			new emxEffectivityFramework_mxJPO(context, null).updateEffectivityOnChange(context, args);
        }

	     public MapList getMODStackApplicabilityforPUE (Context context, String[] args) throws Exception{

          MapList  result = new MapList ();
          HashMap paramMap = (HashMap) JPO.unpackArgs(args);
		  HashMap RequestValuesMap = (HashMap)paramMap.get("RequestValuesMap");
		  String[] appArray = (String[])RequestValuesMap.get("keyInApp");
          String[] modeArray = (String[])RequestValuesMap.get("mode");
		  String[] ProductOIDArray = (String[])RequestValuesMap.get("ProductOID");
          String[] changeIdArray = (String[])RequestValuesMap.get("changeId");
		  String sPUEECO =PropertyUtil.getSchemaProperty(context,"type_PUEECO");
		  String appliValue = appArray[0];
		  String sproductOID = ProductOIDArray[0];
          String mode = modeArray[0];
          String changeId = changeIdArray[0];
		  MODStacks modStack = new MODStacks(context,sproductOID, sPUEECO);
          if("create".equals(mode))
              result = modStack.previewMODStacks(appliValue);
          else
              result = modStack.previewMODStacksForEdit(appliValue,changeId);
          return result;

      }
	     /** Method to Load MODStacks for preview
		   *
		   * @param context the eMatrix code context object
		   * @param args contains packed hashMap of parameters
		   * @return MapList of values
		   * @throws Exception if the operation fails.
		   */

	  public HashMap LoadMODStacksForPreview (Context context, String[] args)throws Exception{
		  HashMap resultMap 					 = new HashMap();
		  MapList result  						 = new MapList();

		  MODStacks modStack  					 =  null;
		  HashMap paramMap   					 = (HashMap) JPO.unpackArgs(args);
		  String actualExpr 				     = MODStacks.getOnlyUnitEffectivityExpr((String) paramMap.get("actualExpression"));		  
		  String sPUEECO 			             = PropertyUtil.getSchemaProperty(context,"type_PUEECO");
	      String changeId 						 = (String)paramMap.get("changeId");
	      String modeType   					 = (String)paramMap.get("modeType");
	      Map applMap				    	 	 = effectivity.getExpressionSequence(context, actualExpr);
	        if (!applMap.keySet().isEmpty()){
			Iterator prodIdItr  				 = applMap.keySet().iterator();
			 while (prodIdItr.hasNext())
		    {
		    	String sProdObjId 	= (String)prodIdItr.next();
		    	String applValue    = (String)applMap.get(sProdObjId);
		    	modStack  = new MODStacks(context, sProdObjId, sPUEECO);
		    	if("create".equals(modeType)) {
		              result = modStack.previewMODStacks(applValue);
		    	} else {
		    		String SELECT_CCA_ID = "from[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].to[" + ChangeConstants.TYPE_CCA + "].id";
		        	StringList objectSelect = new StringList(2);
		        	objectSelect.addElement(DomainConstants.SELECT_TYPE);
		        	objectSelect.addElement(SELECT_CCA_ID);

		    		Map infoMap = DomainObject.newInstance(context, changeId).getInfo(context, objectSelect);

		    		String type = (String) infoMap.get(DomainConstants.SELECT_TYPE);

		    		if (ChangeConstants.TYPE_CHANGE_ORDER.equals(type) && UIUtil.isNullOrEmpty((String) infoMap.get(SELECT_CCA_ID))) {
		    			result = modStack.previewMODStacks(applValue);
		    		} else {
		    			result = modStack.previewMODStacksForEdit(applValue,changeId);
		    		}
		    	}

		    	resultMap.put(sProdObjId, result);
		    }
	     }
		    	return resultMap;
	  }

         /**
          * Returns a MapList of the ECO ids when ECO filter is selected
          * for a given context.
          * @param context the eMatrix <code>Context</code> object.
          * @param args contains a packed HashMap containing objectId of object
          * @return MapList.
          * @since  X+4.
          * @throws Exception if the operation fails.
         */
         @com.matrixone.apps.framework.ui.ProgramCallable
         public MapList getContextChangeForECO(Context context, String args[])throws Exception
         {
             MapList ecoList=new MapList();
             StringList finalECOs=new StringList();
             HashMap paramMap = (HashMap) JPO.unpackArgs(args);
             String  stringECOObjectId =(String)paramMap.get("cmdECOFilterInfo");
             finalECOs = FrameworkUtil.split(stringECOObjectId, ",");
             for(int i=1;i<=finalECOs.size();i++) {
                Map map = new HashMap();
                map.put("id",finalECOs.get(i-1));
                ecoList.add(map);
             }
             ecoList.add(0,new Integer(finalECOs.size()+1));
             return ecoList;
         }
		 //IR-017080 fix ends

         //2011x - Starts
         @com.matrixone.apps.framework.ui.ProgramCallable
         public MapList getMODStackNames(Context context, String args[]) throws Exception {
        	 MapList mlModStackInfo	= null;
        	 HashMap paramMap 		= (HashMap) JPO.unpackArgs(args);
        	 String objectId		= (String) paramMap.get("objectId");
        	 String level			= (String) paramMap.get("level");
        	 if (!"null".equals(level) && !"".equals(level)
             		&& level != null && level.startsWith("0")){
        		 mlModStackInfo			= new MapList();
        		 if (level.length() <= 3) { //Added for :0656172011x
        			 String sPUEECOType =
        	              PropertyUtil.getSchemaProperty(context,"type_PUEECO");
        	          MODStacks modStack = new MODStacks(context, DomainObject.newInstance(context, objectId).getInfo(context, "physicalid"), sPUEECOType);
        			 MapList modStackList = modStack.calculateMODStacks();

        			 Map mapModStack;
        			 Map tempMap;

        			 String modId;

        			 for (int i = 0, size = modStackList.size(); i < size; i++) {
        				 tempMap = (Map) modStackList.get(i);
        				 modId = (String) tempMap.get("id");
        				 mapModStack = new HashMap();

        				 mapModStack.put("id", objectId);
        				 mapModStack.put(MODSTACK_ID, FrameworkUtil.split(modId, "|").get(0));
        				 mapModStack.put(APPLICABILITY_ID, tempMap.get("applicability"));
                         mapModStack.put("level", "1");
                         mlModStackInfo.add(mapModStack);
        			 }
                 }
             }

        	 return mlModStackInfo;
         }

         public Vector displayModNames(Context context, String args[]) throws Exception {
        	 HashMap paramMap 		= (HashMap) JPO.unpackArgs(args);
        	 MapList objectList     = (MapList) paramMap.get("objectList");
        	 Vector modNameVector   = new Vector();
        	 if (objectList != null && objectList.size() > 0 && !objectList.isEmpty()){ //Added for Fix-IR-0609472011x
        	 Map mapModStack		= (Map) objectList.get(0);
        	 String level			= (String) mapModStack.get("level");

        	 if ("1".equals(level)) {
        		 Map map;
        		 for (int i = 0; i < objectList.size(); i++) {
        			 map = (Map) objectList.get(i);
        			 modNameVector.addElement(map.get(MODSTACK_ID));
        		 }

        	 } else if ("0".equals(level)) {

	        		 for (int i=0;i<objectList.size();i++){
	        			 modNameVector.addElement("");
	        		 }
        	}
        }
        	 return modNameVector;
    }


         public Vector displayEffectivity(Context context, String args[]) throws Exception {
        	 HashMap paramMap 		= (HashMap) JPO.unpackArgs(args);
        	 MapList objectList     = (MapList) paramMap.get("objectList");
        	 Vector modEffVector    = new Vector(objectList.size());
        	 if (objectList != null && objectList.size() > 0 && !objectList.isEmpty()){//Added for Fix-IR-0609472011x
        	 Map mapModStack		= (Map) objectList.get(0);
        	 String level			= (String) mapModStack.get("level");
        	 if ("1".equals(level)) {
        		 Map map;
        		 for (int i = 0; i < objectList.size(); i++) {
        			 map = (Map) objectList.get(i);
        			 modEffVector.addElement(map.get(APPLICABILITY_ID));
        		 }

        	 }else if ("0".equals(level)) {
        		 	for (int i=0;i<objectList.size();i++){
        			 modEffVector.addElement("");
        		 }
        	  }
           }
        	 return modEffVector;
        }

	/**
	 * This method will be invoked when the product got revised and it will set
	 * the MODStacks attribute empty for the newly revised product.
	 *
	 * @param context
	 *            the eMatrix code context object
	 * @param args
	 *            contains product object id
	 * @return void
	 * @throws FrameworkException
	 *             if the operation fails
	 */
	public void resetModStacks(Context context, String[] args) throws FrameworkException {
		String prodId 	 = args[0];
		//take the physical id of latest revision
		String prodPhyId = DomainObject.newInstance(context, prodId).getInfo(context, "next.physicalid");
		//set modstacks attribute to empty
		DomainObject.newInstance(context, prodPhyId).setAttributeValue(context,ATTRIBUTE_MOD_STACKS, DomainConstants.EMPTY_STRING);

	}

 }
