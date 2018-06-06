/*
**  emxProcessBase
**
**  Copyright (c) 1992-2015 Dassault Systemes.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of MatrixOne,
**  Inc.  Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
*/


import java.util.*;

import matrix.db.*;
import matrix.util.*;


import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.util.*;
import matrix.util.*;

import java.util.*;

import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;


/**
 * The <code>emxWorkflowBase</code> class contains methods for document.
 *
 *
 */

public class emxProcessBase_mxJPO
{




    /**
       * Constructor.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param args holds no arguments
       * @throws Exception if the operation fails
       * @since Common 10.0.0.0
       * @grade 0
       */
      public emxProcessBase_mxJPO (Context context, String[] args)
          throws Exception
      {

      }

      /**
       * This method is executed if a specific method is not specified.
       *
       * @param context the eMatrix <code>Context</code> object
       * @param args holds no arguments
       * @returns int
       * @throws Exception if the operation fails
       * @since Common 10.0.0.0
       */
      public int mxMain(Context context, String[] args)
          throws Exception
      {
          if (true)
          {
              throw new Exception(ComponentsUtil.i18nStringNow("emxComponents.Workflow.SpecifyMethodOnWorkflowInvocation", context.getLocale().getLanguage()));
          }
          return 0;
      }



      @com.matrixone.apps.framework.ui.ProgramCallable
      public MapList getProcesses(Context context, String[] args) throws Exception
      {
          MapList processList = new MapList();
          try
          {
              String cmd = "list process * select hidden name description property[MX_TASKMAIL_CLASS] dump |";
              HashMap map = processMql(context, cmd);
              String result = (String)map.get("error");
              if(result != null)
              {
                  throw new Exception(result);
              } else
              {
                  result = (String)map.get("result");
                  if(result != null)
                  {
                      HashMap hMap = null;
                      StringTokenizer tokens = new StringTokenizer(result.trim(), "\n");
                      StringTokenizer processData;
                      String tempToken;
                      while(tokens.hasMoreTokens())
                      {
                          //Get each token and replace '||' with '| |'
                    	  tempToken = tokens.nextToken();
                    	  StringBuffer sbuf = new StringBuffer(tempToken);
                          while (sbuf.toString().indexOf("||") > 0) {
                              sbuf.replace(sbuf.toString().indexOf("||"), (sbuf
                                      .toString().indexOf("||") + 2), "| |");
                          }
                          tempToken = sbuf.toString();
                    	  processData = new StringTokenizer(tempToken.toString(), "|");

                          if(processData.hasMoreTokens())
                          {
                              if(processData.nextToken().equalsIgnoreCase("FALSE"))
                              {
                                  String name = processData.nextToken();
                                  boolean validate = false;
                                  if(name != null && name.length() > 0)
                                  {
                                      if(tempToken.indexOf("MX_TASKMAIL_CLASS value") > 0)
                                      {
                                    	  validate = true;
                                      }
                                      else
                                      {
                                    	  validate = false;
                                      }
                                  }
                                  if(validate)
                                  {
                                      hMap = new HashMap();
                                      hMap.put("id", name);
                                      if(processData.hasMoreTokens())
                                      {
                                    	  hMap.put("id", name);
                                          hMap.put("description", (processData.nextToken()).trim());
                                      }
                                      else
                                      {
                                        hMap.put("description", "");
                                      }
                                      processList.add(hMap);
                                  }
                              }
                          }
                      }
                  }
              }
          }
          catch(Exception ex)
          {
              throw ex;
          }

          return processList;
      }

      public StringList getAttributesForProcess(Context context, String[] args) throws Exception
      {
          StringList list = new StringList();
          if(args.length == 0){
              throw new Exception("Invalid arguments");
          }
          StringBuffer buf = new StringBuffer("print process \"");
          buf.append(args[0]);
          buf.append("\" select attribute dump");
          HashMap result = processMql(context, buf.toString());
          if((String)result.get("error") != null)
          {
              throw new Exception((String)result.get("error"));
          }
          StringTokenizer tokens = new StringTokenizer((String)result.get("result"), ",");
          while(tokens.hasMoreTokens())
          {
              list.add(tokens.nextToken());
          }
          return list;
      }

      public Vector getName(Context context, String[] args) throws Exception
      {
          Vector list = new Vector();
          try
          {
             HashMap programMap = (HashMap)JPO.unpackArgs(args);
             MapList objList = (MapList)programMap.get("objectList");

             Iterator itr = objList.iterator();
             HashMap obj;
             while(itr.hasNext())
             {
                 obj = (HashMap)itr.next();
                 list.add("<img border=0 src=../common/images/iconSmallBusinessProcess.gif></img> &nbsp;&nbsp;"+XSSUtil.encodeForHTML(context, (String)obj.get("id")));//list.add((String)obj.get("id"));
             }

          }catch(Exception ex)
          {
              throw ex;
          }
          return list;
      }
      public Vector getDescription(Context context, String[] args) throws Exception
      {
          Vector list = new Vector();
          try
          {
             HashMap programMap = (HashMap)JPO.unpackArgs(args);
             MapList objList = (MapList)programMap.get("objectList");

             Iterator itr = objList.iterator();
             HashMap obj;
             while(itr.hasNext())
             {
                 obj = (HashMap)itr.next();
                 list.add((String)obj.get("description"));
             }

          }catch(Exception ex)
          {
              throw ex;
          }
          return list;
      }

      public HashMap getProcessInfo(Context context, String[] args) throws Exception
      {
          HashMap returnMap = new HashMap();
          if(args.length == 0){
              throw new Exception("Invalid arguments");
          }
          StringBuffer cmd = new StringBuffer("print process \"");
          cmd.append(args[0]);
          cmd.append("\" select name description attribute dump |");
          HashMap map = processMql(context, cmd.toString());

          String result = (String)map.get("error");
          if(result != null)
          {
              throw new Exception(result);
          } else
          {
              result = (String)map.get("result");
              if(result != null)
              {
                  StringBuffer sbuf = new StringBuffer(result.trim());
                   while (sbuf.toString().indexOf("||") > 0)
                   {
                      sbuf.replace(sbuf.toString().indexOf("||"), (sbuf
                              .toString().indexOf("||") + 2), "| |");
                  }
                  result = sbuf.toString();

                  StringTokenizer tokens = new StringTokenizer(result, "|");
                  StringList attributes = null;
                  if(tokens.hasMoreTokens())
                  {
                      returnMap.put("name", tokens.nextToken());
                      if(tokens.hasMoreTokens())
                       {
                            String description = tokens.nextToken().trim();
                            returnMap.put("description", description);
                       }
                      //returnMap.put("description", tokens.nextToken().trim());
                      attributes = new StringList();
                      while(tokens.hasMoreTokens())
                      {
                         // attributes = new StringList();

                          attributes.add(tokens.nextToken());
                      }
                      returnMap.put("attributes", attributes);
                  }
              }
          }


          return returnMap;
      }

      private static HashMap processMql(Context context, String cmd) throws Exception
      {
         MQLCommand mql = new MQLCommand();
         HashMap returnMap = new HashMap();
           boolean bResult = mql.executeCommand(context, cmd);
           if(bResult)
           {
               returnMap.put("result", mql.getResult().trim());
             return returnMap;
           }
           else
           {
             returnMap.put("error", mql.getError());
             return returnMap;
           }
      }



    public Vector getRadioButtons(Context context, String[] args)
        throws Exception
    {
        try
        {
            HashMap programMap        = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap          = (HashMap) programMap.get("paramList");
            MapList workflowList      = (MapList)programMap.get("objectList");

            Vector StatusIcons        = new Vector();

            int index                 = 0;
            int size                  = workflowList.size();
            Map workflowMap           = null;
            //Bug No 318351 - Start
            /*
            Read the PrinterFriendly parameter from paramMap.
            In fowwing while loop, checked whether it is PrinterFriendly page.
            If it is PrinterFriendly an empty space is shown, else radio button is shown as usual.
            */
            String isPrinterFriendly = (String)paramMap.get("PrinterFriendly");
            while(index<size)
            {
                if (isPrinterFriendly != null && "true".equalsIgnoreCase(isPrinterFriendly))
                {
                    StatusIcons.add("&nbsp;");
                }
                else
                {
                    workflowMap    = (Map) workflowList.get(index);
                    String objectId = (String) workflowMap.get("id");
                    String message = "HI"+objectId;
                    if(index==0)
                        StatusIcons.add("<input type=\"radio\" name=\"process\" value=\""+XSSUtil.encodeForHTMLAttribute(context, objectId)+"\" onclick=\"javascript:parent.self.changePage('"+XSSUtil.encodeForURL(context,objectId)+"')\"/>");
                    else
                    StatusIcons.add("<input type=\"radio\" name=\"process\" value=\""+XSSUtil.encodeForHTMLAttribute(context, objectId)+"\" onclick=\"javascript:parent.self.changePage('"+XSSUtil.encodeForURL(context,objectId)+"')\"/>");
                }
                //Bug No 318351 - end
                index++;
            }
          return StatusIcons;
        }
        catch (Exception ex)
        {
            throw ex;
        }
    }

    public String isProcessAutostart(Context context, String [] args) throws Exception
    {
        String autoStart = "false";
        try
        {
            String  processName = args[0];
            if(processName != null && processName.length() > 0)
            {
                StringBuffer cmd = new StringBuffer("print process \"");
                cmd.append(processName);
                cmd.append("\" select autostart dump |");
                autoStart = (MqlUtil.mqlCommand(context, cmd.toString(), true)).trim();
            }
        }
        catch(Exception ex)
        {
            throw ex;
        }

        return autoStart;
    }

    public boolean validateProcess(Context context, String processName) throws Exception
    {
        boolean validate = true;

        try
        {
            ContextUtil.pushContext(context);
            StringBuffer validateCommand = new StringBuffer(128);
            validateCommand.append("validate process '");
            validateCommand.append(processName);
            validateCommand.append("'");

            HashMap returnMap = processMql(context, validateCommand.toString());
            String error = (String)returnMap.get("error");
            if(error != null && error.length() > 0)
            {
                validate = false;
            }

            validateCommand = new StringBuffer(128);
            validateCommand.append("print process '");
            validateCommand.append(processName);
            validateCommand.append("' select property[MX_TASKMAIL_CLASS].value dump");

            returnMap = processMql(context, validateCommand.toString());

            error = (String)returnMap.get("error");
            if(error != null && error.length() > 0)
            {
                throw new Exception(error);
            }
            else
            {
                String result = ((String)returnMap.get("result")).trim();
                if(result != null && result.length() == 0)
                {
                    validate = false;
                }

            }
            ContextUtil.popContext(context);
        }
        catch(Exception ex)
        {
            throw ex;
        }




        return validate;
    }

    public int getAllActivitiesDuration(Context context, String[] args) throws Exception
    {
    	int totalDuration = 0;
    	String  processName = args[0];
    	
    	StringBuffer cmd = new StringBuffer(128);
    	cmd.append("print process '");
    	cmd.append(processName);
    	cmd.append("' select interactive.numdays dump");
    	try {   
    		String result = (MqlUtil.mqlCommand(context, cmd.toString(), true)).trim();
    		StringList resultList = FrameworkUtil.split(result, ",");
    		for(int i=0; i < resultList.size(); i++)
    		{
    			String duration = (String)resultList.get(i);
    			int temp = Integer.parseInt(duration);
    			totalDuration = totalDuration + temp;
    		}
    	}
    	catch(Exception ex)
    	{
    		throw ex;
    	}
    	return totalDuration;
    }
}
