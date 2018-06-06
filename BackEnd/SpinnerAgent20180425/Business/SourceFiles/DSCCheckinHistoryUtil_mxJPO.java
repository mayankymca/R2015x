/*
**  DSC_CheckinHistoryUtil
**
**  Copyright Dassault Systemes, 1992-2007.
**  All Rights Reserved.
**  This program contains proprietary and trade secret information of Dassault Systemes and its 
**  subsidiaries, Copyright notice is precautionary only
**  and does not evidence any actual or intended publication of such program
**
**  Class defining basic infrastructure, contains common data members required
**  for executing any IEF related actions.
*/

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import matrix.db.Attribute;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.MapList;

public class DSCCheckinHistoryUtil_mxJPO
{
    public static  String ATTRIBUTE_RECENT_CHECKIN_FILES                 = null;
    public static  String  ATTRIBUTE_RECENT_CHECKIN_MAX_FILE_DISPLAY     = null;
    public static  String SELECT_RECENT_CHECKIN_FILES                    = null;
     
    private MCADServerResourceBundle serverResourceBundle                = null;
    private MCADMxUtil util                                              = null;
    private String localeLanguage                                        = null;
    private MCADLocalConfigObject lco                                    = null;
    private IEFGlobalCache cache                                         = null;
    private String[] initArgs                                            = null;

    public  DSCCheckinHistoryUtil_mxJPO  ()
    {
    }

    public DSCCheckinHistoryUtil_mxJPO (Context context, String[] args) throws Exception
    {
        initArgs = args;
        ATTRIBUTE_RECENT_CHECKIN_FILES            = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-RecentCheckinFiles");
        ATTRIBUTE_RECENT_CHECKIN_MAX_FILE_DISPLAY = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-RecentCheckinMaxFileDisplayed");
        SELECT_RECENT_CHECKIN_FILES               = "attribute[" + ATTRIBUTE_RECENT_CHECKIN_FILES + "]";
        
        if (!context.isConnected())
        {
            MCADServerException.createException("not supported no desktop client", null);
        }
    }

    public int mxMain(Context context, String []args)  throws Exception
    {
        return 0;
    }
   
    public Map setLCOAttribute(Context context, String [] args)
    {
       HashMap resultMap = new HashMap();
       try
       {
             HashMap uploadParamsMap = (HashMap)JPO.unpackArgs(args);
             MCADLocalConfigObject localConfigObject = (MCADLocalConfigObject)uploadParamsMap.get("LCO");
             String value        = (String)uploadParamsMap.get("value");
             if (null != value)
            	 util.setLCOAttribute(context, localConfigObject, ATTRIBUTE_RECENT_CHECKIN_FILES, value);
        }   
        catch (Exception e)
        {
            System.out.println(e.toString());
        }   
        return resultMap;
    }

    /**
     * getRecentCheckinFilesDetail
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds all arguments
     * @throws Exception if the operation fails
     * @since VCP 10.5.0.0
     * @grade 0
     */
     
    public MapList getRecentCheckinFilesDetail(Context context, String[] args)
    {     
        MapList fileList    = new MapList();
        try
        {   
            
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            HashMap paramList = (HashMap)paramMap.get("paramList");
            lco = (MCADLocalConfigObject)paramMap.get("LCO");
            if (null ==  lco && null != paramList)
            {
                    lco = (MCADLocalConfigObject)paramList.get("LCO");
            }

            if (null == lco)
            {
                return fileList;
            }

            String fileString = (String)util.getLCOAttributeValue(context,  lco, ATTRIBUTE_RECENT_CHECKIN_FILES);

            if(fileString == null)
            {
                fileString = "";
            }

            
            Enumeration  tokens      = MCADUtil.getTokensFromString(fileString, MCADAppletServletProtocol.DELIMITER);

            while (tokens.hasMoreElements())
            {
              String token  = (String)tokens.nextElement();
              HashMap map   = new HashMap();

              map.put("id", token);

              fileList.add(map);
            }
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        
        return fileList;
    }

    private MapList getRecentCheckinFileData(Context context)
    {
        MapList fileList    = new MapList();
        try
        {   
            serverResourceBundle        = new MCADServerResourceBundle(localeLanguage);
            cache                       = new IEFGlobalCache();
            util                        = new MCADMxUtil(context, serverResourceBundle, cache);

            String fileString = (String)util.getLCOAttributeValue(context,  lco, ATTRIBUTE_RECENT_CHECKIN_FILES);

            if (fileString == null)
            {
               fileString = "";
            }
           
            Enumeration  tokens      = MCADUtil.getTokensFromString(fileString, MCADAppletServletProtocol.DELIMITER);
            boolean needsUpdate         = false;
            StringBuffer buffer         = new StringBuffer();
	         ArrayList majorObjList		= new ArrayList();
            while (tokens.hasMoreElements())
            {
                String token    = (String)tokens.nextElement();
                String name     = "";
                String type     = "";
                String revision = "";
                String checkinDate = "";

                
                StringTokenizer tnr = new StringTokenizer(token, MCADAppletServletProtocol.TNR_SEPARATOR);

                if (tnr.hasMoreTokens()) 
                {
                  type = tnr.nextToken();
                }

                if (tnr.hasMoreTokens()) 
                {
                  name = tnr.nextToken();
                }

                if (tnr.hasMoreTokens()) 
                {
                  revision = tnr.nextToken();
                }
                                                                
                if (tnr.hasMoreTokens()) 
                {
                  checkinDate = tnr.nextToken();
                }

                BusinessObject bus      = new BusinessObject(type, name, revision, context.getVault().getName());
                boolean isObjectExists  = true;

                try
                {
                     isObjectExists = bus.exists(context);
                }
                catch (Exception e)
                {
                	e.printStackTrace();
                	System.out.println("type=" +type+ " name=" +name+" revision="+revision);
                     isObjectExists = false;
                }

                if (isObjectExists)
                {
                   if (buffer.length() > 0)
                   {
                        buffer.append(MCADAppletServletProtocol.DELIMITER);
                   }

                   bus.open(context);
                   String id    = bus.getObjectId();
                   HashMap map  = new HashMap();
				   BusinessObject majorBusObject = null;
                                                                   
				   Attribute attrSource =   bus.getAttributeValues(context, MCADMxUtil.getActualNameForAEFData(context,"attribute_Source"));
				   String objIntegrationName = attrSource.getValue();
				   
					boolean isCDMMinorObject = util.isCDMMinorObject(context, id);

					if(isCDMMinorObject && objIntegrationName !=null && objIntegrationName.contains(MCADAppletServletProtocol.MS_OFFICE_INTEGRATION_NAME))
					{
						majorBusObject = util.getMajorObjectForCDM(context, bus);
					}
					else
					{
						majorBusObject = util.getMajorObject(context, bus);
					}
                   String majorId	= "";
                                                                   
				   if(majorBusObject != null)
						majorId = majorBusObject.getObjectId(context);
				if(majorBusObject != null && !majorId.equals("") && !majorObjList.contains(majorId))
                   {
                       majorBusObject.open(context);
                       map.put(DomainConstants.SELECT_ID, majorId);
                       // Designer Central 10.6.0.1
                       map.put("UseMinor", "true");
                       map.put("MinorObjectId", id);
                       // Designer Central 10.6.0.1
						
		       majorObjList.add(majorId);

                   map.put("objectList", id);
                   map.put("Checkin Date", checkinDate);
						if(isCDMMinorObject)
					   {
							map.put("Revision", revision);
							map.put("Version", "");
					   }
					   else
					   {
                   map.put("Revision", revision.substring(0,revision.indexOf(".")));
                   map.put("Version", revision.substring(revision.indexOf(".")+1));
					   }

                   fileList.add(map);
                   buffer.append(token);
						majorBusObject.close(context);
                   bus.close(context);
                                       
                   }
                                   
                }
                else
                {
                   if(false == needsUpdate)
                   {
                        needsUpdate = true;
                   }
                }
            }

           // if some business object has been removed, update the new file list in the person attribute to reflect 
           // some business objects are being deleted.
            if (needsUpdate)
            {
                HashMap params = new HashMap();
                params.put("attrName",  ATTRIBUTE_RECENT_CHECKIN_FILES);
                params.put("value", buffer.toString());
                params.put("LCO", lco);
                setLCOAttribute(context,  JPO.packArgs(params));
            }
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        return fileList;
    }

    /**
      * getRecentlyCheckedInFiles
      * Desktop Integration uses this function
      *
      */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getRecentlyCheckedInFiles(Context context, String[] args)
    {     
        MapList fileList    = new MapList();
        try
        {   
            Hashtable paramMap = (Hashtable)JPO.unpackArgs(initArgs);
            lco = (MCADLocalConfigObject)paramMap.get("lco");
            fileList = getRecentCheckinFileData(context);
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        
        return fileList;
    }

   /**
     * getRecentCheckinFiles
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds all arguments
     * @throws Exception if the operation fails
     * @since VCP 10.5.0.0
     * @grade 0
     */
     @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getRecentCheckinFiles(Context context, String[] args)
    {   
        MapList fileList    = new MapList();
        try
        {   
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            lco = (MCADLocalConfigObject)paramMap.get("LCO");
            fileList = getRecentCheckinFileData(context);
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        return fileList;
    }

    public MapList getRecentCheckinFilesMajorObjects(Context context, String[] args)
    {
        MapList fileList    = new MapList();
        try
        {
            HashMap paramMap = (HashMap)JPO.unpackArgs(args);
            lco = (MCADLocalConfigObject)paramMap.get("LCO");
            String fileString = (String)util.getLCOAttributeValue(context,  lco, ATTRIBUTE_RECENT_CHECKIN_FILES);
            if(fileString == null)
            {
                fileString = "";
            }

            Enumeration tokenizer   = MCADUtil.getTokensFromString(fileString,  MCADAppletServletProtocol.DELIMITER);
            boolean needsUpdate         = false;
            StringBuffer buffer         = new StringBuffer();
            int i                       = 0;

            while (tokenizer.hasMoreElements())
            {
                String token    = (String)tokenizer.nextElement();
                String name     = "";
                String type     = "";
                String revision = "";

                if (0 != i)
                {
                     buffer.append(MCADAppletServletProtocol.DELIMITER);
                }

                StringTokenizer tnr = new StringTokenizer(token,  MCADAppletServletProtocol.TNR_SEPARATOR);
                if (tnr.hasMoreTokens()) 
                {
                    type = tnr.nextToken();
                }

                if (tnr.hasMoreTokens()) 
                {
                    name = tnr.nextToken();
                }

                if (tnr.hasMoreTokens()) 
                {
                    revision = tnr.nextToken();
                }

                BusinessObject bus = new BusinessObject(type, name, revision, context.getVault().getName());
                bus.open(context);

                boolean isObjectExists = bus.exists(context);
            
                if (isObjectExists)
                {
                    String id   = bus.getObjectId();
                    HashMap map = new HashMap();

                    map.put(DomainConstants.SELECT_ID,  id);
                    map.put("objectList",  id);
                    fileList.add(map);
                    if (i > 0)
                    {
                       buffer.append(MCADAppletServletProtocol.DELIMITER);
                    }
                    buffer.append(token);
                    bus.close(context);
                    i++;
                }
                else
                {
                    if (false == needsUpdate)
                    {
                        needsUpdate = true;
                    }
                }
            }
            // if some business object has been removed, update the new file list in the person attribute to reflect 
            // some business objects are being deleted.
            if (needsUpdate)
            {
                HashMap params = new HashMap();
                params.put("attrName",  ATTRIBUTE_RECENT_CHECKIN_FILES);
                params.put("value", buffer.toString());
                params.put("LCO", lco);
                setLCOAttribute(context,  JPO.packArgs(params));
            }
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        
        return fileList;
    }
     
    /**
     * addFile
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds all arguments
     * @throws Exception if the operation fails
     * @since VCP 10.5.0.0
     * @grade 0
     */
    public Map addFile(Context context, String[] args)
    {
        Map objectMap = new HashMap();

        try
        {
            HashMap uploadParamsMap         = (HashMap)JPO.unpackArgs(args);
            String type                     = (String)uploadParamsMap.get("Type");
            String name                     = (String)uploadParamsMap.get("Name");
            String revision                 = (String)uploadParamsMap.get("Revision");
            lco                             = (MCADLocalConfigObject)uploadParamsMap.get("LCO");
            Calendar calendar               = Calendar.getInstance();
            String timePattern              = "MM/dd/yyyy hh:mm:ss a";
            SimpleDateFormat formatter      = (SimpleDateFormat)DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.US);

            formatter.applyPattern(timePattern);
            String currentTime              = formatter.format(calendar.getTime());

            // number of last n files to be displayed
            String countStr     = (String)util.getLCOAttributeValue(context, lco, ATTRIBUTE_RECENT_CHECKIN_MAX_FILE_DISPLAY);
            int nCount          = 50;

            if (null == countStr || 0 == countStr.length())
            {
                nCount = 50;
            }
            else
            {
                nCount = Integer.parseInt(countStr);
            }

            // get attribute values of ATTRIBUTE_RECENT_CHECKIN_FILES
            MapList fileList    = getRecentCheckinFilesDetail(context, args);
            Map map             = new HashMap();

            // returns only the last n files stored in the attribute in FIFO fashion
            

            String files = "";

            // converts the last n files to a string
            StringBuffer buffer = new StringBuffer();
            buffer.append(type + MCADAppletServletProtocol.TNR_SEPARATOR + name + MCADAppletServletProtocol.TNR_SEPARATOR + revision + MCADAppletServletProtocol.TNR_SEPARATOR + currentTime);
            for(int i = 0; i < fileList.size() && i < nCount-1; i++)
            {
                if(buffer.length() > 0)
                {
                   buffer.append(MCADAppletServletProtocol.DELIMITER); 
                }

                map = (Map)fileList.get(i);
                buffer.append((String)map.get("id"));
            }

            files = buffer.toString();

            // update the attribute value stored in Person object
            HashMap params                          = new HashMap();
            params.put("LCO", lco);
            params.put("attrName",  ATTRIBUTE_RECENT_CHECKIN_FILES);
            params.put("value", files);
            setLCOAttribute(context,  JPO.packArgs(params));
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        return objectMap;
    }

     /**
     * clearAll
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds all arguments
     * @throws Exception if the operation fails
     * @since VCP 10.5.0.0
     * @grade 0
     */
    public Map clearAll(Context context, String[] args)
    {
        Map objectMap = new HashMap();
       
        try
        {
            HashMap params = (HashMap)JPO.unpackArgs(args);
           
            params.put("attrName",  ATTRIBUTE_RECENT_CHECKIN_FILES);
            params.put("value", "");
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        return objectMap;
    }
}

