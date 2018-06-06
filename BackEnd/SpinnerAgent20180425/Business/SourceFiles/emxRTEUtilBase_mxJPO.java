/*
 *  emxRTEUtilBase
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved.
 * This program contains proprietary and trade secret information of
 * MatrixOne, Inc.  Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program.
 *
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import matrix.db.Context;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.StringUtil;
import com.matrixone.apps.framework.ui.UIRTEUtil;

/**
 * The <code>emxRTEUtil</code> class contains RTE utility methods.
 * @author sgudlavalleti
 */
public class emxRTEUtilBase_mxJPO  {
    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     */
    public emxRTEUtilBase_mxJPO (Context context, String[] args)
            throws Exception
    {
        //super( context, args);
    }
    static String ATTRIBUTE_ALL_MODIFY_ACTION = "AttributeAllModifyAction";
    static String ATTRIBUTE_ALL_RTE_MODIFY_ACTION = "AttributeAllRTEModifyAction";

    /**
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static void enableRTE(Context context, String[] args) throws Exception
    {
        Map RTEData =  getEnableRTEData(context, args);
        Iterator itr = RTEData.keySet().iterator();
        while(itr.hasNext())
        {
            try
            {
                ContextUtil.startTransaction(context, true);
                String typeName = (String)itr.next();
                String attributes = (String)RTEData.get(typeName);
                StringList attributeList = StringUtil.split(attributes, ",");
                Iterator attrItr = attributeList.iterator();
                StringList addAttrCmds = new StringList(attributeList.size());
                String newRTEEnabledAttributes = "";
                while(attrItr.hasNext())
                {
                    String attrName = (String)attrItr.next();
                    String attrCommand = "print type '"+ typeName +"' select attribute["+ attrName +"] dump";
                    String result = MqlUtil.mqlCommand(context, attrCommand, true);

                    if( "true".equalsIgnoreCase(result) || "description".equals(attrName))
                    {
                        String RTEAttrName = attrName + "_RTE";
                        attrCommand = "print type '"+ typeName +"' select attribute["+ RTEAttrName +"] dump";
                        result = MqlUtil.mqlCommand(context, attrCommand, true);
                        if( "false".equalsIgnoreCase(result))
                        {
                            attrCommand = "list attribute '"+ RTEAttrName + "'";
                            result = MqlUtil.mqlCommand(context, attrCommand, true);
                            addAttrCmds.addElement("add attribute '" + RTEAttrName +"' ");
                            newRTEEnabledAttributes += attrName +"|";

                            if(!(result != null && result.contains(RTEAttrName)))
                            {
                                attrCommand = "add attribute '" + attrName +"_RTE' type string multiline hidden "+
                                                    "trigger Modify action emxTriggerManager input " + ATTRIBUTE_ALL_RTE_MODIFY_ACTION;
                                MqlUtil.mqlCommand(context, attrCommand, true);
                            }
                            String triggerInput = ATTRIBUTE_ALL_MODIFY_ACTION + " ";
                            if("description".equals(attrName))
                            {
                                attrCommand = "print type '" + typeName +"' select trigger dump";
                                result = MqlUtil.mqlCommand(context, attrCommand, true);
                                if(result != null && result.indexOf(ATTRIBUTE_ALL_MODIFY_ACTION) == -1 )
                                {
                                    if(result.indexOf("ModifyDescriptionAction:")>=0 )
                                    {
                                        int index = result.indexOf("ModifyDescriptionAction:emxTriggerManager(");
                                        index = index + 42;
                                        int lastIndex = result.indexOf(")", index);
                                        triggerInput += result.substring(index, lastIndex);
                                    }
                                    attrCommand = "modify type '" + typeName +"' "+
                                                    "add trigger ModifyDescription action emxTriggerManager input '" + triggerInput.trim() +"'";
                                    MqlUtil.mqlCommand(context, attrCommand, true);
                                }

                            } else {
                                attrCommand = "print attribute '" + attrName +"' select trigger dump";
                                result = MqlUtil.mqlCommand(context, attrCommand, true);
                                if(result != null && result.indexOf(ATTRIBUTE_ALL_MODIFY_ACTION) == -1 )
                                {
                                    if(result.indexOf("ModifyAction:")>=0 )
                                    {
                                        int index = result.indexOf("ModifyAction:emxTriggerManager(");
                                        index = index + 31;
                                        triggerInput += result.substring(index, (result.length()-1));
                                    }
                                    attrCommand = "modify attribute '" + attrName +"' "+
                                                "add trigger Modify action emxTriggerManager input '" + triggerInput.trim() +"'";
                                    MqlUtil.mqlCommand(context, attrCommand, true);
                                }
                            }
                        }
                    }
                }
                String typeCommand = "modify type '" + typeName +"' sparse true";
                MqlUtil.mqlCommand(context, typeCommand, true);

                typeCommand = "modify type '" + typeName +"' " + StringUtil.join(addAttrCmds, "");
                MqlUtil.mqlCommand(context, typeCommand, true);
                typeCommand = "print type '" + typeName +"' select property[" + UIRTEUtil.RTE_ENABLED_ATTRIBUTES + "] dump";
                String rteEnabledAttributes = MqlUtil.mqlCommand(context, typeCommand, true);
                //String newRTEEnabledAttributes = StringUtil.join(addAttrCmds, "");
                if(rteEnabledAttributes == null || "".equals(rteEnabledAttributes) )
                {
                	newRTEEnabledAttributes = newRTEEnabledAttributes.substring(0, newRTEEnabledAttributes.length()-1);
                } else {
                	newRTEEnabledAttributes += rteEnabledAttributes;
                }
                //mod type Document add property xyz value abc;
                typeCommand = "modify type '" + typeName +"' add property '" + UIRTEUtil.RTE_ENABLED_ATTRIBUTES + "' value '"+ newRTEEnabledAttributes +"'";
                MqlUtil.mqlCommand(context, typeCommand, true);
                typeCommand = "modify type '" + typeName +"' sparse false";
                MqlUtil.mqlCommand(context, typeCommand, true);
                ContextUtil.commitTransaction(context);
            } catch (Exception ex)
            {
                ContextUtil.abortTransaction(context);
            }
        }
        /*
        modify type Document add property RTE_ATTRIBUTES value comments;
        print type Document select property[RTE_ATTRIBUTES].value dump; //comments
        modify type Issue add Trigger ModifyDescription action emxTriggerManager input abc;
        print attribute Comments select trigger dump
        add attribute Comments_RTE type string multiline  hidden  trigger Modify action emxTriggerManager input AttributeAllStripRTETags;
        modify attribute Comments add trigger Modify action emxTriggerManager input AttributeAllResetRTEText;
        */
    }

    
    /**
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static void disableRTE(Context context, String[] args) throws Exception
    {
        Map RTEData =  getEnableRTEData(context, args);
        Iterator itr = RTEData.keySet().iterator();
        while(itr.hasNext())
        {
            try
            {
                ContextUtil.startTransaction(context, true);
                String typeName = (String)itr.next();
                String attributes = (String)RTEData.get(typeName);
                StringList attributeList = StringUtil.split(attributes, ",");
                Iterator attrItr = attributeList.iterator();
                StringList addAttrCmds = new StringList(attributeList.size());                
                String typeCommand = "print type '" + typeName +"' select property[" + UIRTEUtil.RTE_ENABLED_ATTRIBUTES + "] dump";
                String rteEnabledAttributes = MqlUtil.mqlCommand(context, typeCommand, true);

                while(attrItr.hasNext())
                {
                    String attrName = (String)attrItr.next();
                    String RTEAttrName = attrName + "_RTE";
                    String attrCommand = "print type '"+ typeName +"' select attribute["+ RTEAttrName +"] dump";
                    String result = MqlUtil.mqlCommand(context, attrCommand, true);

                    if( "true".equalsIgnoreCase(result))
                    {                        
                        addAttrCmds.addElement("remove attribute '" + RTEAttrName +"' ");
                        rteEnabledAttributes = rteEnabledAttributes.replace(attrName+"|", "");

                        String triggerInput = ATTRIBUTE_ALL_MODIFY_ACTION + " ";
                        if("description".equals(attrName))
                        {
                            attrCommand = "print type '" + typeName +"' select trigger dump";
                            result = MqlUtil.mqlCommand(context, attrCommand, true);
                            if(result != null && result.indexOf(ATTRIBUTE_ALL_MODIFY_ACTION) > -1 )
                            {
                                if(result.indexOf("ModifyDescriptionAction:")>=0 )
                                {
                                    int index = result.indexOf("ModifyDescriptionAction:emxTriggerManager(");
                                    index = index + 42;
                                    int lastIndex = result.indexOf(")", index);
                                    triggerInput = result.substring(index, lastIndex);
                                    if( triggerInput.trim().equals(ATTRIBUTE_ALL_MODIFY_ACTION))
                                    {
                                    	attrCommand = "modify type '" + typeName +"' "+ "remove trigger ModifyDescription action";
                                    	MqlUtil.mqlCommand(context, attrCommand, true);                                    	
                                    } else {
                                    	triggerInput = triggerInput.replace(ATTRIBUTE_ALL_MODIFY_ACTION, "");
                                        attrCommand = "modify type '" + typeName +"' "+
                                        				"add trigger ModifyDescription action emxTriggerManager input '" + triggerInput.trim() +"'";
                                        MqlUtil.mqlCommand(context, attrCommand, true);
                                    }
                                }
                            }

                        } else {
                            attrCommand = "print attribute '" + attrName +"' select trigger dump";
                            result = MqlUtil.mqlCommand(context, attrCommand, true);
                            if(result != null && result.indexOf(ATTRIBUTE_ALL_MODIFY_ACTION) > -1 )
                            {
                                if(result.indexOf("ModifyAction:")>=0 )
                                {
                                    int index = result.indexOf("ModifyAction:emxTriggerManager(");
                                    index = index + 31;
                                    triggerInput = result.substring(index, (result.length()-1));
                                    if( triggerInput.trim().equals(ATTRIBUTE_ALL_MODIFY_ACTION))
                                    {
                                    	attrCommand = "modify attribute '" + attrName +"' "+ "remove trigger Modify action";
                                    	MqlUtil.mqlCommand(context, attrCommand, true);                                    	
                                    } else {
                                    	triggerInput = triggerInput.replace(ATTRIBUTE_ALL_MODIFY_ACTION, "");
                                        attrCommand = "modify attribute '" + attrName +"' "+
                                        				"add trigger Modify action emxTriggerManager input '" + triggerInput.trim() +"'";
                                        MqlUtil.mqlCommand(context, attrCommand, true);
                                    }                                    
                                }
                            }
                        }               
                    }
                }
                typeCommand = "modify type '" + typeName +"' sparse true";
                MqlUtil.mqlCommand(context, typeCommand, true);

                typeCommand = "modify type '" + typeName +"' " + StringUtil.join(addAttrCmds, "");
                MqlUtil.mqlCommand(context, typeCommand, true);

                typeCommand = "modify type '" + typeName +"' add property '" + UIRTEUtil.RTE_ENABLED_ATTRIBUTES + "' value '"+ rteEnabledAttributes +"'";
                MqlUtil.mqlCommand(context, typeCommand, true);
                typeCommand = "modify type '" + typeName +"' sparse false";
                MqlUtil.mqlCommand(context, typeCommand, true);
                ContextUtil.commitTransaction(context);
            } catch (Exception ex)
            {
                ContextUtil.abortTransaction(context);
            }
        }
        /*
        modify type Document add property RTE_ATTRIBUTES value comments;
        print type Document select property[RTE_ATTRIBUTES].value dump; //comments
        modify type Issue add Trigger ModifyDescription action emxTriggerManager input abc;
        print attribute Comments select trigger dump
        add attribute Comments_RTE type string multiline  hidden  trigger Modify action emxTriggerManager input AttributeAllStripRTETags;
        modify attribute Comments add trigger Modify action emxTriggerManager input AttributeAllResetRTEText;
        */
    }
    

    static String GLOBAL_RPE_RTE_TEXT_EDITED = "GLOBAL_RPE_RTE_TEXT_EDITED";
    static String GLOBAL_RPE_NON_RTE_TEXT_EDITED = "GLOBAL_RPE_NON_RTE_TEXT_EDITED";
    /**
     * @param context
     * @param args
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map getEnableRTEData(Context context, String[] args) {
        Map rteMap = new HashMap();
        String fileName = args[0];
        boolean isFile = false;
        if( fileName != null)
        {
            File file = new File(fileName);
            isFile = file.exists();
        }
        if( isFile)
        {
            try {
                rteMap = getEnableRTEDataFromFile(context, args);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
        int length = args.length;
        for(int i=0; i<length; i++)
        {
            String typeAttrs = args[i];
            String type = typeAttrs.substring(0,  typeAttrs.indexOf("|"));
            String attrs = typeAttrs.substring(typeAttrs.indexOf("|")+1, typeAttrs.length());
            rteMap.put(type, attrs);
        }
        }
        return rteMap;
    }
    /**
     * @param context
     * @param args
     * @return
     * @throws FileNotFoundException
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map getEnableRTEDataFromFile(Context context, String[] args) throws FileNotFoundException {
        Map rteMap = new HashMap();
        String fileName = args[0];
        File file = new File(fileName);
        Scanner fileScanner = null;
        try {
            fileScanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        while(fileScanner.hasNext())
        {
            String typeAttrs = fileScanner.nextLine();
            String type = typeAttrs.substring(0,  typeAttrs.indexOf("|"));
            String attrs = typeAttrs.substring(typeAttrs.indexOf("|")+1, typeAttrs.length());
            rteMap.put(type, attrs);
        }
        return rteMap;
    }

    /**
     *
     * @param context
     * @param args {OBJECTID} {ATTRNAME} {NEWATTRVALUE} {TYPE}
     * @throws Exception
     */
    public static void updateNonRTEValue(Context context, String[] args) throws Exception
    {
        try
        {
            String rpeValue = PropertyUtil.getGlobalRPEValue(context, GLOBAL_RPE_NON_RTE_TEXT_EDITED);
            if(!"true".equals(rpeValue))
            {
                PropertyUtil.setGlobalRPEValue(context, GLOBAL_RPE_RTE_TEXT_EDITED , "true");
                String oid = args[0];
                String attrName = args[1];
                String newAttrValue = args[2];
                String type = args[3];
                if( UIRTEUtil.isRTEEnabled(context, type, attrName))
                {
                    if( attrName.endsWith("_RTE"))
                    {
                        String newAttrValueWithoutRTETags = UIRTEUtil.getNonRTEString(context, newAttrValue);
                        DomainObject obj = DomainObject.newInstance(context, oid);
                        String nonRTEAttrName = attrName.substring(0, attrName.lastIndexOf("_RTE"));
                        if(attrName.equalsIgnoreCase("description_RTE"))
                        {
                            obj.setDescription(context, newAttrValueWithoutRTETags);
                        } else {
                            obj.setAttributeValue(context, nonRTEAttrName, newAttrValueWithoutRTETags);
                        }
                    }
                }
            }

        } catch (Exception ex)
        {
            ex.printStackTrace();
            throw ex;
        }
    }

    /**
     *
     * @param context
     * @param args {OBJECTID} {ATTRNAME} {NEWATTRVALUE} {TYPE}
     * @throws Exception
     */
    public static void resetRTEValue(Context context, String[] args) throws Exception
    {
        try
        {
            String rpeValue = PropertyUtil.getGlobalRPEValue(context, GLOBAL_RPE_RTE_TEXT_EDITED);
            if(!"true".equals(rpeValue))
            {
                String oid = args[0];
                String attrName = args[1];
                String newAttrValue = args[2];
                String type = args[3];
                String description = "";
                if( args.length>3)
                {
                    description = args[4];
                }
                if(attrName == null || "".equals(attrName))
                {
                    attrName = "description";
                    newAttrValue = description;
                }
                if( UIRTEUtil.isRTEEnabled(context, type, attrName))
                {
                    if( !attrName.endsWith("_RTE"))
                    {
                        PropertyUtil.setGlobalRPEValue(context, GLOBAL_RPE_NON_RTE_TEXT_EDITED , "true");
                        String RTEAttrName = attrName + "_RTE";
                        DomainObject obj = DomainObject.newInstance(context, oid);
                        String RTEAttributeValue = obj.getInfo(context, "attribute[" + RTEAttrName + "]");
                        String attrValueWithoutRTETags = UIRTEUtil.getNonRTEString(context, RTEAttributeValue);
                        if(!newAttrValue.equals(attrValueWithoutRTETags))
                        {
                            obj.setAttributeValue(context, RTEAttrName, newAttrValue);
                        }
                    }
                }
            }

        } catch (Exception ex)
        {
            ex.printStackTrace();
            throw ex;
        }
    }


}
