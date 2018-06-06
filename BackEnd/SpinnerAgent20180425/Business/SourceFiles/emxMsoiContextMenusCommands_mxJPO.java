import matrix.db.*;
import java.util.*;
import com.matrixone.apps.domain.util.*;
import com.matrixone.MCADIntegration.server.*;
import com.matrixone.MCADIntegration.server.beans.*;
import com.matrixone.MCADIntegration.utils.*;
import com.matrixone.apps.domain.util.i18nNow;

/**
 * @author SJ7
 */
public class emxMsoiContextMenusCommands_mxJPO
{
    
    public  emxMsoiContextMenusCommands_mxJPO ()
    {
    }

    public emxMsoiContextMenusCommands_mxJPO (Context context, String[] args) throws Exception
    {
        if (!context.isConnected())
        {
            MCADServerException.createException("not supported no desktop client", null);
        }
    }

    public int mxMain(Context context, String []args)  throws Exception
    {  
        return 0;
    }
    
    /**
     * @param context
     * @return
     */
     //SJ7+
    public String getContextMenu(Context context, String[] args)
    {
        String returnValue = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
        returnValue += "<CommandsConfiguration xmlns=\"http://SmarTeam.Std.InfoWorker.UILogic.Commands.Configuration\">";

        try
        {
            String command = "list menu ctxMsoi_*";
            String output = MqlUtil.mqlCommand(context, command);
            
            String ClientLanguage = "";
            
            if(args[0] != null && !args[0].isEmpty())
                ClientLanguage = args[0];
            else
                ClientLanguage = "en";
            
            ArrayList<String> navigationContext = new ArrayList<String>();
            ArrayList<String> searchResultsContext = new ArrayList<String>();
            ArrayList<String> revisionsContext = new ArrayList<String>();
			ArrayList<String> outlookContext = new ArrayList<String>();
            
            String[] outputTemp = output.split("ctxMsoi_");
            
            for (int i = 0; i < outputTemp.length; i++) {
                if (outputTemp[i].contains("Navigation")) {
                    navigationContext.add("ctxMsoi_"+outputTemp[i]);
                }
                else if(outputTemp[i].contains("SearchResults")) {
                    searchResultsContext.add("ctxMsoi_"+outputTemp[i]);                    
                }
                else if(outputTemp[i].contains("Revisions")) {
                    revisionsContext.add("ctxMsoi_"+outputTemp[i]);
                }
				else if(outputTemp[i].contains("Outlook")) {
                    outlookContext.add("ctxMsoi_"+outputTemp[i]);
                }                       
            }
            
            returnValue += "<Contexts>";
            returnValue += getAllMenuCommands(navigationContext, context, ClientLanguage);
            returnValue += getAllMenuCommands(searchResultsContext, context, ClientLanguage);
            returnValue += getAllMenuCommands(revisionsContext, context, ClientLanguage);
			returnValue += getAllMenuCommands(outlookContext, context, ClientLanguage);
            returnValue += "</Contexts>";            
            
        }
        catch (Exception e)
        {
            System.out.println("Inside getContextMenu - Exception : \r\n ");
            System.out.println(e);
        }

        returnValue += "</CommandsConfiguration>";
        
        return returnValue;
    }
    
    public String getAllMenuCommands(ArrayList<String> menuContext, Context context, String ClientLanguage)
    {        
        String returnValue = "";                           
        String[] tempSplit, menuSplit;
        menuSplit = menuContext.get(0).split("_");
        returnValue += "<Context Name=\""+menuSplit[1].trim() +"\"><Groups>";

        for (String str : menuContext) 
        {
            tempSplit = str.split("_");            
            returnValue += "<Group Name=\""+tempSplit[2].trim()+"\">";

            returnValue += "<Commands>";
            returnValue += getCommandFromMenu(context, str, ClientLanguage);
            returnValue += "</Commands></Group>";            
        }
        
        returnValue += "</Groups></Context>";        

        return returnValue;
    }
    
    public String getCommandFromMenu(Context context, String menuName, String ClientLanguage)
    {
        String returnValue = "";        
        try
        {
            String command = "print menu " + menuName + " select command dump";
            String output = MqlUtil.mqlCommand(context, command);
            
            String[] temp, settingtemp, commandtemp;
            commandtemp = null;
            String delimeter = ",";
            String commandPrefix="ctxMsoiCmd_";
            temp = output.split(delimeter);

            for(int i=0; i<= temp.length - 1; i++)
            {
                try
                { 
                    if(temp[i].trim().contains(commandPrefix))
                    {
                        commandtemp = temp[i].split(commandPrefix);
                        System.out.println("commandtemp : " + commandtemp);    
                    }
                    else
                    {     
                        commandtemp[0] = commandPrefix;
                        commandtemp[1] = temp[i];
                    }
                }
                catch(Exception Ex)
                {
                    System.out.println("Inside getCommandFromMenu - Exception : \r\n ");
                    System.out.println(Ex);
                }

                command = "print command "+temp[i]+" select setting.name setting.value dump";
               
                output = MqlUtil.mqlCommand(context, command);

                settingtemp = output.split(delimeter);                

                String TranslatedCommand = i18nNow.getI18nString("emxMSF.Command.ctxMsoiCmd_" + commandtemp[1].trim(),"emxMSFStringResource", ClientLanguage);
                returnValue += "<Command Name=\""+commandtemp[1].trim()+"\" DisplayName=\""+TranslatedCommand+"\" ";

                returnValue += settingtemp[2] +"=\""+ settingtemp[5] +"\" "+ settingtemp[0] +"=\""+ settingtemp[3] +"\" "+ settingtemp[1] +"=\""+ settingtemp[4] +"\">";
                
                if(settingtemp[4].contains("true"))
                {
                    returnValue += "<SubCommands>"+getCommandFromMenu(context, temp[i]+"Menu", ClientLanguage);
                    returnValue += "</SubCommands>";
                }
                returnValue += "</Command>";
            }
        }
        catch (Exception e)
        {
            System.out.println("Inside getCommandFromMenu - Exception : \r\n ");
            System.out.println(e);
        }
        
        return returnValue;
    }
} 
