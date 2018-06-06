/*
**   emxProgramCentralUtilBase
**
**   Copyright (c) 1992-2015 Dassault Systemes.
**   All Rights Reserved.
**   This program contains proprietary and trade secret information of MatrixOne,
**   Inc.  Copyright notice is precautionary only
**   and does not evidence any actual or intended publication of such program
**
**   static const char RCSID[] = $Id: ${CLASSNAME}.java.rca 1.1.2.1.2.2 Thu Dec  4 07:55:10 2008 ds-ss Experimental ${CLASSNAME}.java.rca 1.1.2.1.2.1 Thu Dec  4 01:53:17 2008 ds-ss Experimental ${CLASSNAME}.java.rca 1.1.2.1 Wed Oct 22 15:50:25 2008 przemek Experimental przemek $
*/

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.IconMail;
import matrix.db.JPO;
import matrix.db.MatrixWriter;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;
import matrix.util.StringResource;

import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.common.Company;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.common.util.SubscriptionUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainAccess;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.program.Assessment;
import com.matrixone.apps.program.ProgramCentralConstants;
import com.matrixone.apps.program.ProgramCentralUtil;
import com.matrixone.apps.program.ProjectSpace;
import com.matrixone.apps.program.Task;

/**
 * The <code>emxProgramCentralUtil</code> class contains static methods for sending mail.
 *
 * @version AEF 9.5.0.0 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxProgramCentralUtilBase_mxJPO
{
    /** Holds the base URL for notification messages. */
    protected static String _baseURL = "";

    /** Holds the agent name for notification messages. */
    protected static String _agentName = "";

    /** Holds the languages for notification messages. */
    protected static String _languages = "";

    /** Name of the bundle to use. */
    static final String _bundleName = "emxProgramCentralStringResource";

    /** Cache of the loaded properties. */
    static Map _bundles = new Hashtable();

    /** Directory name to store the migration log file */
    private String logDirectory = "";

    /**
     * Constructor.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @throws Exception if the operation fails
     * @since AEF 9.5.0.0
     */
    public emxProgramCentralUtilBase_mxJPO (Context context, String[] args)
        throws Exception
    {
        /*if (!context.isConnected())
            throw new Exception("not supported on desktop client");
        */
    }

    /**
     * This method is executed if a specific method is not specified.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @return int 0 for success and non-zero for failure
     * @throws Exception if the operation fails
     * @since AEF 9.5.0.0
     */
    public int mxMain(Context context, String[] args)
        throws Exception
    {
        if (true)
        {
            throw new Exception("must specify method on emxMailUtil invocation");
        }
        return 0;
    }

    /**
     * Set base URL string used in notifications.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - url string
     * @return int 0 for success and non-zero for failure
     * @throws Exception if the operation fails
     * @since AEF 9.5.0.0
     */
    public static int setBaseURL(Context context, String[] args)
        throws Exception
    {
        if (args != null && args.length > 0)
        {
            _baseURL = args[0];
        }
        return 0;
    }

    /**
     * Get base URL string used in notifications.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @return String containing url
     * @throws Exception if the operation fails
     * @since AEF 9.5.0.0
     */
    public static String getBaseURL(Context context, String[] args)
        throws Exception
    {
        return _baseURL;
    }

    /**
     * Get base URL string used in notifications.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @return int 0 for success and non-zero for failure
     * @throws Exception if the operation fails
     * @since AEF 9.5.0.0
     */
    public static int getStreamBaseURL(Context context, String[] args)
        throws Exception
    {
        BufferedWriter writer = new BufferedWriter(new MatrixWriter(context));
        writer.write(_baseURL);
        writer.flush();
        return 0;
    }

    /**
     * Set agent name used in notifications.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - name string
     * @return int 0 for success and non-zero for failure
     * @throws Exception if the operation fails
     * @since AEF 9.5.1.0
     */
    public static int setAgentName(Context context, String[] args)
        throws Exception
    {
        if (args != null && args.length > 0)
        {
            _agentName = args[0];
        }
        return 0;
    }

    /**
     * Get agent name used in notifications.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @return String the name of the person to use in "from" field
     * @throws Exception if the operation fails
     * @since AEF 9.5.0.0
     */
    public static String getAgentName(Context context, String[] args)
        throws Exception
    {
        return _agentName;
    }

    /**
     * Set languages used in notifications.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - space-delimited list of locals
     * @return int 0 for success and non-zero for failure
     * @throws Exception if the operation fails
     * @since AEF 9.5.1.0
     */
    public static int setLanguages(Context context, String[] args)
        throws Exception
    {
        if (args != null && args.length > 0)
        {
            _languages = args[0];
        }
        return 0;
    }

    /**
     * Get languages used in notifications.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds no arguments
     * @return String a list of space-delimited locales
     * @throws Exception if the operation fails
     * @since AEF 9.5.0.0
     */
    public static String getLanguages(Context context, String[] args)
        throws Exception
    {
        return _languages;
    }

    /**
     * Sends an icon mail notification to a single specified user.
     * Appends a url to the message if an objectId is given.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - toUser - the user to notify
     *        1 - subject - the notification subject
     *        2 - message - the notification message
     *        3 - objectId - the id of the object to include in the notification url
     * @return int 0 for success and non-zero for failure
     * @throws Exception if the operation fails
     * @deprecated use sendNotificationToUser()
     * @since AEF 9.5.0.0
     */
    public static int sendMail(Context context, String[] args)
        throws Exception
    {
        if (args == null || args.length < 4)
        {
            throw (new IllegalArgumentException());
        }
        String toUser = args[0];
        String subject = args[1];
        String message = args[2];
        String objectId = args[3];
        String url = _baseURL;
        StringList toList = new StringList(1);
        toList.addElement(toUser);

        if (objectId != null && url.length() > 0)
        {
            url += "?objectId=";
            url += objectId;
            message += "\n\n";
            message += url;
        }

        sendMessage(context,
                    toList,
                    null,
                    null,
                    subject,
                    message,
                    null);
        return 0;
    }

    /**
     * This method sends an icon mail notification to the specified users.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args contains a Map with the following entries:
     *        toList - the list of users to notify
     *        ccList - the list of users to cc
     *        bccList - the list of users to bcc
     *        subject - the notification subject
     *        message - the notification message
     *        objectIdList - the ids of objects to send with the notification
     * @return int 0 for success and non-zero for failure
     * @throws Exception if the operation fails
     * @since AEF 9.5.0.0
     */
    public static int sendMessage(Context context, String[] args)
        throws Exception
    {
        if (args == null || args.length < 1)
        {
            throw (new IllegalArgumentException());
        }
        Map map = (Map) JPO.unpackArgs(args);

        sendMessage(context,
                    (StringList) map.get("toList"),
                    (StringList) map.get("ccList"),
                    (StringList) map.get("bccList"),
                    (String) map.get("subject"),
                    (String) map.get("message"),
                    (StringList) map.get("objectIdList"));

        return 0;
    }

    /**
     * Sends an icon mail notification to the specified users.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args contains a Map with the following entries:
     *        toList - the list of users to notify
     *        ccList - the list of users to cc
     *        bccList - the list of users to bcc
     *        subjectKey - the notification subject key
     *        subjectKeys - an array of subject place holder keys
     *        subjectValues - an array of subject place holder values
     *        messageKey - the notification message key
     *        messageKeys - an array of message place holder keys
     *        messageValues - an array of message place holder values
     *        objectIdList - the ids of objects to send with the notification
     *        companyName - used for company-based messages
     * @return int 0 for success and non-zero for failure
     * @throws Exception if the operation fails
     * @since AEF 9.5.1.0
     */
    public static int sendNotification(Context context, String[] args)
        throws Exception
    {
        if (args == null || args.length < 1)
        {
            throw (new IllegalArgumentException());
        }
        Map map = (Map) JPO.unpackArgs(args);

        sendNotification(context,
                    (StringList) map.get("toList"),
                    (StringList) map.get("ccList"),
                    (StringList) map.get("bccList"),
                    (String) map.get("subjectKey"),
                    (String[]) map.get("subjectKeys"),
                    (String[]) map.get("subjectValues"),
                    (String) map.get("messageKey"),
                    (String[]) map.get("messageKeys"),
                    (String[]) map.get("messageValues"),
                    (StringList) map.get("objectIdList"),
                    (String) map.get("companyName"));

        return 0;
    }

    /**
     * Sends an icon mail notification to a single specified user.
     * Appends a url to the message if an objectId is given.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        userList - a comma separated list of users to notify
     *        subjectKey - the notification subject key
     *        subjectSubCount - the number of key/value pairs for subject substitution
     *        subjectKey1 - the first subject key
     *        subjectValue1 - the first subject value
     *        messageKey - the notification message key
     *        messageSubCount - the number of key/value pairs for message substitution
     *        messageKey1 - the first message key
     *        messageValue1 - the first message value
     *        objectIdList - a comma separated list of objectids to include in the notification url
     *        companyName - used for company-based messages
     * @return int 0 for success and non-zero for failure
     * @throws Exception if the operation fails
     * @since AEF 9.5.1.0
     */
    public static int sendNotificationToUser(Context context, String[] args)
        throws Exception
    {
        if (args == null || args.length < 3)
        {
            throw (new IllegalArgumentException());
        }
        int index = 0;
        StringTokenizer tokens = new StringTokenizer(args[index++], ",");
        StringList toList = new StringList();
        while (tokens.hasMoreTokens())
        {
            toList.addElement(tokens.nextToken().trim());
        }

        String subjectKey = args[index++];
        int subCount = Integer.parseInt(args[index++]);
        String[] subjectKeys = new String[subCount];
        String[] subjectValues = new String[subCount];
        if (args.length < 3+(subCount*2))
        {
            throw (new IllegalArgumentException());
        }
        for (int i=0; i < subCount ;i++)
        {
            subjectKeys[i] = args[index++];
            subjectValues[i] = args[index++];
        }

        String messageKey = args[index++];
        subCount = Integer.parseInt(args[index++]);
        String[] messageKeys = new String[subCount];
        String[] messageValues = new String[subCount];
        for (int i=0; i < subCount ;i++)
        {
            messageKeys[i] = args[index++];
            messageValues[i] = args[index++];
        }

        StringList objectIdList = null;
        if (args.length > index)
        {
            tokens = new StringTokenizer(args[index++], ",");
            objectIdList = new StringList();
            while (tokens.hasMoreTokens())
            {
                objectIdList.addElement(tokens.nextToken().trim());
            }
        }

        String companyName = null;
        if (args.length > index)
        {
            companyName = args[index];
        }
        String basePropFile ="emxProgramCentralStringResource";
        emxMailUtilBase_mxJPO.sendNotification(context,null,
                         toList,
                         null,
                         null,
                         subjectKey,
                         subjectKeys,
                         subjectValues,
                         messageKey,
                         messageKeys,
                         messageValues,
                         objectIdList,
                         "",
                         basePropFile);
        return 0;
    }

    /**
     * Returns a processed and translated message for a given key.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        messageKey - the message key
     *        messageSubCount - the number of key/value pairs for message substitution
     *        messageKey1 - the first message key
     *        messageValue1 - the first message value
     *        companyName - used for company-based messages
     * @return String the message
     * @throws Exception if the operation fails
     * @since AEF 9.5.1.0
     */
    public static String getMessage(Context context, String[] args)
        throws Exception
    {
        if (args == null || args.length < 2)
        {
            throw (new IllegalArgumentException());
        }
        int index = 0;
        String messageKey = args[index++];
        int subCount = Integer.parseInt(args[index++]);
        String[] messageKeys = new String[subCount];
        String[] messageValues = new String[subCount];
        if (args.length < 2+(subCount*2))
        {
            throw (new IllegalArgumentException());
        }
        for (int i=0; i < subCount ;i++)
        {
            messageKeys[i] = args[index++];
            messageValues[i] = args[index++];
        }
        String companyName = null;
        if (args.length > index)
        {
            companyName = args[index];
        }

        String message = getMessage(context,
                         messageKey,
                         messageKeys,
                         messageValues,
                         companyName);

        BufferedWriter writer = new BufferedWriter(new MatrixWriter(context));
        writer.write(message);
        writer.flush();
        return message;
    }

    /**
     * Returns a processed and translated message for a given key.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param messageKey the notification message key
     * @param messageKeys an array of message place holder keys
     * @param messageValues an array of message place holder values
     * @param companyName used for company-based messages
     * @return String the message
     * @throws Exception if the operation fails
     * @since AEF 9.5.1.0
     */
    public static String getMessage(Context context,
                                     String messageKey,
                                     String[] messageKeys,
                                     String[] messageValues,
                                     String companyName)
        throws Exception
    {

        // Define the message from the given key.
        String messageValue = getString(
                messageKey,
                companyName,
                getLocale(context));
        String message = messageValue;

        // Substitute in values for any placeholders.
        if (messageKeys != null && messageKeys.length > 0 && messageValue != null)
        {
            message = StringResource.format(messageValue, messageKeys, messageValues);
        }
        return message;
    }

    /**
     * Sends an icon mail notification to the specified users.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param toList the list of users to notify
     * @param ccList the list of users to cc
     * @param bccList the list of users to bcc
     * @param subject the notification subject
     * @param message the notification message
     * @param objectIdList the ids of objects to send with the notification
     * @throws Exception if the operation fails
     * @since AEF 9.5.0.0
     */
    protected static void sendMessage(Context context,
                                    StringList toList,
                                    StringList ccList,
                                    StringList bccList,
                                    String subject,
                                    String message,
                                    StringList objectIdList)
        throws Exception
    {
        // If there is no subject, then return without sending the notification.
        if (subject == null || "".equals(subject))
        {
            return;
        }

        // If the base URL and object id list are available,
        // then add urls to the end of the message.
        if ( (_baseURL != null && ! "".equals(_baseURL)) &&
             (objectIdList != null && objectIdList.size() != 0) )
        {
            // Prepare the message for adding urls.
            message += "\n";

            Iterator i = objectIdList.iterator();
            while (i.hasNext())
            {
                // Add the url to the end of the message.
                message += "\n" + _baseURL + "?objectId=" + (String) i.next();
            }
        }

        // Send the mail message.
        sendMail(context,
                 toList,
                 ccList,
                 bccList,
                 subject,
                 message,
                 objectIdList);
    }

    /**
     * Sends an icon mail notification to the specified users.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param toList the list of users to notify
     * @param ccList the list of users to cc
     * @param bccList the list of users to bcc
     * @param subjectKey the notification subject key
     * @param subjectKeys an array of subject place holder keys
     * @param subjectValues an array of subject place holder values
     * @param messageKey the notification message key
     * @param messageKeys an array of message place holder keys
     * @param messageValues an array of message place holder values
     * @param objectIdList the ids of objects to send with the notification
     * @param companyName used for company-based messages
     * @throws Exception if the operation fails
     * @since AEF 9.5.1.0
     */
    protected static void sendNotification(Context context,
                                    StringList toList,
                                    StringList ccList,
                                    StringList bccList,
                                    String subjectKey,
                                    String[] subjectKeys,
                                    String[] subjectValues,
                                    String messageKey,
                                    String[] messageKeys,
                                    String[] messageValues,
                                    StringList objectIdList,
                                    String companyName)
        throws Exception
    {
        // Define the mail subject.
        String subject = getMessage(context,
                         subjectKey,
                         subjectKeys,
                         subjectValues,
                         companyName);

        // Define the mail message.
        String message = getMessage(context,
                         messageKey,
                         messageKeys,
                         messageValues,
                         companyName);

        // If the base URL and object id list are available,
        // then add urls to the end of the message.
        if ( (_baseURL != null && ! "".equals(_baseURL)) &&
             (objectIdList != null && objectIdList.size() != 0) )
        {
            // Prepare the message for adding urls.
            message += "\n";

            Iterator i = objectIdList.iterator();
            while (i.hasNext())
            {
                // Add the url to the end of the message.
                message += "\n" + _baseURL + "?objectId=" + (String) i.next();
            }
        }

        // Send the mail message.
        sendMail(context,
                 toList,
                 ccList,
                 bccList,
                 subject,
                 message,
                 objectIdList);
    }

    /**
     * Sends an icon mail notification to the specified users.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param toList the list of users to notify
     * @param ccList the list of users to cc
     * @param bccList the list of users to bcc
     * @param subject the notification subject
     * @param message the notification message
     * @param objectIdList the ids of objects to send with the notification
     * @throws Exception if the operation fails
     * @since AEF 9.5.0.0
     */
    protected static void sendMail(Context context,
                                 StringList toList,
                                 StringList ccList,
                                 StringList bccList,
                                 String subject,
                                 String message,
                                 StringList objectIdList)
        throws Exception
    {
        // Create iconmail object.
        IconMail mail = new IconMail();
        mail.create(context);

        // Set the "to" list.
        mail.setToList(toList);

        // Set the "cc" list.
        if (ccList != null)
        {
            mail.setCcList(ccList);
        }

        // Set the "bcc" list.
        if (bccList != null)
        {
            mail.setBccList(bccList);
        }

        // Set the object list.  If the object id list is available,
        // then send the objects along with the notification.
        if (objectIdList != null && objectIdList.size() != 0)
        {
            BusinessObjectList bol =
                    new BusinessObjectList(objectIdList.size());

            Iterator i = objectIdList.iterator();
            while (i.hasNext())
            {
                String id = (String) i.next();
                BusinessObject bo = new BusinessObject(id);
                bo.open(context);
                bol.addElement(bo);
            }

            mail.setObjects(bol);
        }

        // Set the message.
        mail.setMessage(message);

        boolean isContextPushed = false;
        emxContextUtil_mxJPO utilityClass = new emxContextUtil_mxJPO(context, null);
        // Test if spoofing should be performed on the "from" field.
        if (_agentName != null && ! "".equals(_agentName))
        {
            try
            {
                // Push Notification Agent
                String[] pushArgs = {_agentName};
                utilityClass.pushContext(context, pushArgs);
                isContextPushed = true;
            }
            catch (Exception ex)
            {
            }
        }

        // Set the subject and send the iconmail.
        mail.send(context, subject);

        if (isContextPushed == true)
        {
            // Pop Notification Agent
            utilityClass.popContext(context, null);
        }
    }

    /**
     * Get a string for the specified key and company.
     *
     * @param key the key to use in the search
     * @param companyName the company to get the key for
     * @param locale used to identify the bundle to use
     * @return String matching the key
     * @since AEF 9.5.1.0
     */
    static public String getString(String key, String companyName, Locale locale)
    {
        // Get the appropriate resource bundle based on the locale.
        ResourceBundle bundle = (locale == null) ?
                                    (ResourceBundle) _bundles.get("default") :
                                    (ResourceBundle) _bundles.get(locale);

        // Create the bundle if necessary and store it in the bundle map.
        if (bundle == null)
        {
            //bundle = PropertyResourceBundle.getBundle(_bundleName, locale);
            bundle = ResourceBundle.getBundle(_bundleName, locale);

            if (locale == null)
            {
                _bundles.put("default", bundle);
            }
            else
            {
                _bundles.put(locale, bundle);
            }
        }

        // Get the string value from the bundle using the key.
        String value = null;
        try
        {
            if (companyName == null || companyName.length() == 0)
            {
                value = bundle.getString(key);
            }
            else
            {
                try
                {
                    value = bundle.getString(key + "." + companyName.replace(' ', '_'));
                }
                catch (Exception e)
                {
                    value = bundle.getString(key);
                }
            }
        }
        catch (Exception e)
        {
            value = key;
        }

        return value;
    }


    /**
     * Get locale for given context.
     *
     * @param context the eMatrix <code>Context</code> object
     * @return the locale object
     * @since AEF 9.5.1.1
     */
    public static Locale getLocale(Context context)
    {
        // this is the country
        String strContry = "";
        // this is the string id of the localize tag, if there is one
        String strLanguage = "";

        // Get locale
        try
        {
            String result = context.getSession().getLanguage();

            StringTokenizer st1 = new StringTokenizer(result, "\n");
            String locale = st1.nextToken();

            int idxDash = locale.indexOf('-');
            int idxComma = locale.indexOf(',');
            int idxSemiColumn = locale.indexOf(';');
            if ((idxComma == -1) && (idxSemiColumn == -1) && (idxDash == -1))
            {
                strLanguage = locale;
            }
            else if (idxDash != -1)
            {
                boolean cont = true;
                if ((idxComma < idxDash) && (idxComma != -1))
                {
                    if ((idxSemiColumn == -1) || (idxComma < idxSemiColumn))
                    {
                        strLanguage = locale.substring(0, idxComma);
                        cont = false;
                    }
                }
                if ((cont) && (idxSemiColumn < idxDash) && (idxSemiColumn != -1) )
                {
                    if ((idxComma == -1) || (idxComma > idxSemiColumn))
                    {
                        strLanguage = locale.substring(0, idxSemiColumn);
                        cont = false;
                    }
                }
                else if (cont)
                {
                    boolean sec2 = true;
                    StringTokenizer st = new StringTokenizer(locale, "-");
                    if (st.hasMoreTokens()) {
                        strLanguage = st.nextToken();
                        if (st.hasMoreTokens()) {
                            strContry = st.nextToken();
                        } else {
                            sec2 = false;
                        }
                    } else {
                        sec2 = false;
                    }
                    int idx = strContry.indexOf(',');
                    if (idx != -1)
                    {
                        strContry = strContry.substring(0,idx);
                    }
                    idx = strContry.indexOf(';');
                    if (idx != -1)
                    {
                        strContry = strContry.substring(0,idx);
                    }
                    //if (!sec2) {
                        //System.out.println("MATRIX ERROR - LOCAL INFO CONTAINS WRONG DATA");
                    //}
                }
            }
            else
            {
                if ((idxComma != -1) && ((idxComma < idxSemiColumn) || (idxSemiColumn == -1)))
                {
                    strLanguage = locale.substring(0, idxComma);
                }
                else
                {
                    strLanguage = locale.substring(0, idxSemiColumn);
                }
            }
        }
        catch (Exception e)
        {
            strLanguage = "en";
            strContry = "US";
        }

        // Get Resource bundle.
        Locale loc = new Locale(strLanguage, strContry);
        return loc;
    }

    /**
     * Conversion routine for dashboards from pre 10.5 PMC
     * @param context the eMatrix <code>Context</code> object
     * @param args holds the following input arguments:
     *        0 - String Log Directory
     * @throws Exception if the operation fails.
     */
    public void migrateDashboards(Context context, String[] args)
        throws Exception
    {
        if (args.length == 0 )
        {
            throw new IllegalArgumentException();
        }

        logDirectory = args[0];

        try
        {
            ContextUtil.pushContext(context);
            ArrayList convertedDashboardList = new ArrayList();

            StringList objectSelects = new StringList(1);
            objectSelects.add(DomainObject.SELECT_NAME);

            //Get the list of Person objects in the database
            MapList mapList = DomainObject.findObjects(context, DomainObject.TYPE_PERSON, DomainObject.QUERY_WILDCARD, null, objectSelects);

            Iterator iterator = mapList.iterator();
            Map map = new HashMap();
            String dashBoardName = "";
            String userName = "";
            String cmd = "";
            String result = "";
            while(iterator.hasNext())
            {
                map = (Map)iterator.next();
                userName = (String)map.get(DomainObject.SELECT_NAME);
                boolean hasAdminObject    = true;

                try
                {

                    matrix.db.Person adminPerson = new matrix.db.Person(userName);
                    //Opening the context will throw error in case Admin Object does not exist
                    adminPerson.open(context);
                    adminPerson.close(context);
                }
                catch(Exception e)
                {
                    hasAdminObject = false;
                    convertedDashboardList.add("Conversion failed : " + userName + "'s Adminstration Object does not exist.");
                }

                if(hasAdminObject)
                {
                    //Get the list of Sets for this user
                    //PRG:RG6:R213:Mql Injection:parameterized Mql:18-Oct-2011:start
                    String sCommandStatement = "list Set user $1";
                    result =  MqlUtil.mqlCommand(context, sCommandStatement,userName);
                    //PRG:RG6:R213:Mql Injection:parameterized Mql:18-Oct-2011:End

                    StringTokenizer st = new StringTokenizer(result, "\n");

                    //PRG:RG6:R213:Mql Injection:parameterized Mql:18-Oct-2011:start
                    sCommandStatement = "set workspace user $1";
                    result =  MqlUtil.mqlCommand(context, sCommandStatement,userName);
                    //PRG:RG6:R213:Mql Injection:parameterized Mql:18-Oct-2011:End

                    //Prefix Sets beginning with "dashboard-" with a "."
                    while(st.hasMoreTokens())
                    {
                        dashBoardName = st.nextToken();
                        if(dashBoardName.startsWith("dashboard-"))
                        {
                            try
                            {
                              //PRG:RG6:R213:Mql Injection:parameterized Mql:18-Oct-2011:start
                                sCommandStatement = "copy set $1";
                                String sParam = dashBoardName + "."+ dashBoardName;
                                MqlUtil.mqlCommand(context, sCommandStatement,sParam);
                             //PRG:RG6:R213:Mql Injection:parameterized Mql:18-Oct-2011:End

                              //PRG:RG6:R213:Mql Injection:parameterized Mql:18-Oct-2011:start
                                sCommandStatement = "delete set $1";
                                MqlUtil.mqlCommand(context, sCommandStatement,dashBoardName);
                             //PRG:RG6:R213:Mql Injection:parameterized Mql:18-Oct-2011:End

                                convertedDashboardList.add("Conversion passed : " + userName + " : " + dashBoardName );
                            }
                            catch(Exception e)
                            {
                                convertedDashboardList.add("Conversion failed : " + userName + " : Could can not copy Dashboard \"" + dashBoardName + "\" as hidden, a set with the same name might exists, Or not able to delete the original dashboard after conversion.");
                            }
                        }
                    }
                 }
            }

            // check for all the Admin persons
            result = MqlUtil.mqlCommand(context, "list person"); //PRG:RG6:R213:Mql Injection:Static Mql:18-Oct-2011
            StringTokenizer stUser = new StringTokenizer(result, "\n");
            while(stUser.hasMoreTokens())
            {
                userName = stUser.nextToken();
                try
                {
                    //Creating person bean itself will throw error if Business Object does not exist
                    Person persObj = Person.getPerson(context, userName);
                }
                catch(Exception e)
                {
                    convertedDashboardList.add("Conversion failed : " + userName + "'s Business Object does not exist.");
                }
            }
            writeFile(convertedDashboardList);
        }
        catch(Exception e)
        {
          throw new FrameworkException(e);
        }
        finally
        {
          ContextUtil.popContext(context);
        }
    }


    /**
     * Writes the dashboard names which have been renamed to a log file
     * @param dashBoardNames the list of dashboards in old format
     * @throws Exception if the operation fails.
     */

    protected void writeFile(ArrayList dashBoardNames)
        throws Exception
    {
        java.io.File file = new java.io.File(logDirectory + "convertedDashboardList.log");
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(file));
        for (int i=0; i < dashBoardNames.size(); i++)
        {
            fileWriter.write((String)dashBoardNames.get(i));
            fileWriter.newLine();
        }
        fileWriter.close();
    }

    /**
     * This method is used to Exclude the person having role Project User & External Project User
     * @param context the eMatrix <code>Context</code> object
     * @param args holds follwing arguments
     *            objectId- Object id of the document
     * @return StringList
     * @throws Exception if the operation fails
     */

    public StringList getExcludeOIDForTaskAssignee(Context context, String args[]) throws Exception
    {

        //get All Users
        MapList mapUserList = null;
        StringList select = new StringList();
        select.add(DomainConstants.SELECT_ID);
        select.add(DomainConstants.SELECT_NAME);
        mapUserList =DomainObject.findObjects(context,PropertyUtil.getSchemaProperty(context,"type_Person"),"*",null,select);

        HashMap programMap        = (HashMap) JPO.unpackArgs(args);
        // Modified:12-May-09:nzf:R207:PRG:Bug:375205
        // Added additional check and logic for Risk aswell
        // Added:7-May-09:nzf:R207:PRG:Bug:372833
        HashMap requestMap        = (HashMap)programMap.get("RequestValuesMap");
        String  objectId          = (String) programMap.get("objectId");

        DomainObject dmoTask = DomainObject.newInstance(context,objectId);

        boolean isKindOfTask = dmoTask.isKindOf(context,DomainConstants.TYPE_TASK_MANAGEMENT);

        StringList slProjMembers = new StringList();
        StringList slProjMembersNames = new StringList();
        String strProjectId = "";
        if(isKindOfTask){
            strProjectId = Task.getParentProject(context,dmoTask);
        }else{
            StringList slBusSelects = new StringList(2);
            slBusSelects.add(DomainConstants.SELECT_ID);

            MapList mlProjectList = dmoTask.getRelatedObjects(context,
                    DomainConstants.RELATIONSHIP_RISK,
                    DomainConstants.TYPE_PROJECT_MANAGEMENT,
                    slBusSelects,
                    null,       // relationshipSelects
                    true,      // getTo
                    false,       // getFrom
                    (short) 1,  // recurseToLevel
                    null,       // objectWhere
                    null);      // relationshipWhere


            Map mpProjectInfo = null;
            for (Iterator itrFolderStructure = mlProjectList.iterator(); itrFolderStructure.hasNext();) {

                mpProjectInfo = (Map)itrFolderStructure.next();
                strProjectId = (String)mpProjectInfo.get(DomainConstants.SELECT_ID);

            }
            //Added:22-Mar-10:S3L:R2011:PRG:Bug:041299
            dmoTask = DomainObject.newInstance(context,strProjectId);
            isKindOfTask=dmoTask.isKindOf(context,DomainConstants.TYPE_TASK_MANAGEMENT);
            if(isKindOfTask)
            {
                 strProjectId = Task.getParentProject(context,dmoTask);
            }
            // End:R2011:PRG:Bug:041299
        }

        ProjectSpace psProject = new ProjectSpace();
        psProject.newInstance(context);
        psProject.setId(strProjectId);

        //User Object selectables
        StringList objectSelects = new StringList(1);
        objectSelects.add(DomainConstants.SELECT_ID);
        objectSelects.add(DomainConstants.SELECT_NAME);

        MapList mapProjectMembers = psProject.getMembers(
                context,        //Context
                objectSelects,  //Person selectables
                null,           //MemberRelationship selectable
                null,           // business object where clause
                null);          // relationship where clause

        Iterator itr1 = mapProjectMembers.iterator();

        while (itr1.hasNext()){
            Map map1 = (Map) itr1.next();
            slProjMembers.add((String) map1.get(DomainConstants.SELECT_ID));
            slProjMembersNames.add((String) map1.get(DomainConstants.SELECT_NAME));
        }
        // End:R207:PRG:Bug:372833

        StringList  slAllUserIds = new StringList();
        StringList  slAllUserNames = new StringList();

        for(int i=0;i<mapUserList.size();i++){

            Map mapUser = (Map) mapUserList.get(i);

            String strUserId = (String) mapUser.get(DomainConstants.SELECT_ID);
            String strUserName= (String) mapUser.get(DomainConstants.SELECT_NAME);

            slAllUserIds.add(strUserId);
            slAllUserNames.add(strUserName);

        }

        // Added:7-May-09:nzf:R207:PRG:Bug:372833
        String strProjectMemberId = "";
        for(int x=0;x<slProjMembers.size();x++){
            strProjectMemberId = (String)slProjMembers.get(x);
            if(slAllUserIds.contains(strProjectMemberId)){
                slAllUserIds.remove(strProjectMemberId);
            }
        }// End:R207:PRG:Bug:372833

        // End:R207:PRG:Bug:375205

        return slAllUserIds;
    }
// End:V6R2010:PRG Autonomy search

    /**
     * This method is executed to check Access Node visibility.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args holds follwing arguments
     *            objectId- Object id of the document
     *
     * @return boolean
     * @throws Exception if the operation fails
     * @since PC10.5.SP1
     */

    public boolean checkAccessNode(Context context, String[] args) throws Exception
    {
        boolean retVal=false;
        try
        {
            HashMap programMap        = (HashMap) JPO.unpackArgs(args);
            String  objectId          = (String) programMap.get("objectId");

            DomainObject domObject = DomainObject.newInstance(context, objectId);

            StringList busSelects = new StringList(1);
            busSelects.add(DomainConstants.SELECT_ID);

            Pattern typePattern = new Pattern (DomainConstants.TYPE_BUSINESS_GOAL);
            typePattern.addPattern(DomainConstants.TYPE_WORKSPACE_VAULT);
            typePattern.addPattern(DomainConstants.TYPE_TASK_MANAGEMENT);
            typePattern.addPattern(DomainConstants.TYPE_RISK);
            typePattern.addPattern(DomainConstants.TYPE_QUALITY);
            typePattern.addPattern(DomainConstants.TYPE_FINANCIAL_ITEM);
            typePattern.addPattern(Assessment.TYPE_ASSESSMENT);
            typePattern.addPattern(DomainConstants.TYPE_ROUTE);

            MapList objectList = domObject.getRelatedObjects(context,
                                  DomainConstants.QUERY_WILDCARD,
                                  typePattern.getPattern(),
                                  busSelects,
                                  null,
                                  true,
                                  true,
                                  (short) 1,
                                  null,
                                  null);

            if(objectList.size() > 0)
           {
               retVal = true;
           }

        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
        finally
        {
            return retVal;
        }
    }

//  Added:R207:PRG Integration of Common Meetings and Decisions

    /**
     * This Method is used to get Decision object from meeting of a selected object.
     * This method is used for table "PMCDecisionsRelatedDecisions"
     *
     * @param context The Matrix Context object
     * @param args Packed program and request maps for the table
     * @return MapList containing all table data
     * @throws MatrixException if operation fails
     *
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getTablePMCDecisionsRelatedDecisionsData(Context context, String[] args) throws MatrixException
    {
        try {
            final String SELECT_RELATED_OBJECT_ID = "from["+DomainConstants.RELATIONSHIP_DECISION+"].to.id";
            final String SELECT_RELATED_OBJECT_NAME = "from["+DomainConstants.RELATIONSHIP_DECISION+"].to.name";
            final String SELECT_RELATED_OBJECT_TYPE = "from["+DomainConstants.RELATIONSHIP_DECISION+"].to.type";

            Map programMap      = (Map) JPO.unpackArgs(args);
            String  strParentId = (String) programMap.get("objectId");

            DomainObject dmoObject = DomainObject.newInstance(context, strParentId);

            String strRelationshipPattern = DomainConstants.RELATIONSHIP_MEETING_CONTEXT+","+DomainConstants.RELATIONSHIP_DECISION;
            String strTypePattern = DomainConstants.TYPE_MEETING+","+DomainConstants.TYPE_DECISION;

            StringList slBusSelect = new StringList();
            slBusSelect.add(DomainConstants.SELECT_ID);
            slBusSelect.add(DomainConstants.SELECT_TYPE);
            slBusSelect.add(SELECT_RELATED_OBJECT_ID);
            slBusSelect.add(SELECT_RELATED_OBJECT_NAME);
            slBusSelect.add(SELECT_RELATED_OBJECT_TYPE);

            StringList slRelSelect = new StringList();

            boolean getTo = true;
            boolean getFrom = true;
            short recurseToLevel = 2;
            String strBusWhere = "";
            String strRelWhere = "";

            MapList mlRelatedObjects = dmoObject.getRelatedObjects(context,
                    strRelationshipPattern, //pattern to match relationships
                    strTypePattern, //pattern to match types
                    slBusSelect, //the eMatrix StringList object that holds the list of select statement pertaining to Business Obejcts.
                    slRelSelect, //the eMatrix StringList object that holds the list of select statement pertaining to Relationships.
                    getTo, //get To relationships
                    getFrom, //get From relationships
                    recurseToLevel, //the number of levels to expand, 0 equals expand all.
                    strBusWhere, //where clause to apply to objects, can be empty ""
                    strRelWhere); //where clause to apply to relationship, can be empty ""

            Map mapRelatedObjectInfo = null;
            String strType = "";
            String strDecisionId = "";
            Object objRelatedParentId = null;
            StringList slUniqueDecisionList = new StringList();
            MapList mlDecisionObjectList = new MapList();

            for (Iterator itrRelatedObjects = mlRelatedObjects.iterator(); itrRelatedObjects.hasNext();)
            {
                mapRelatedObjectInfo = (Map) itrRelatedObjects.next();
                strType = (String)mapRelatedObjectInfo.get(DomainConstants.SELECT_TYPE);
                //if it is Meeting then remove from the list
                if(DomainConstants.TYPE_MEETING.equals(strType))
                {
                    continue;
                }

                //if decision directly attached to parent then remove from the list
                objRelatedParentId = mapRelatedObjectInfo.get(SELECT_RELATED_OBJECT_ID);
                if (objRelatedParentId == null || strParentId.equals(objRelatedParentId))
                {
                    continue;
                }

                //if decision is already existing then remove from the list
                strDecisionId = (String)mapRelatedObjectInfo.get(DomainConstants.SELECT_ID);
                if (slUniqueDecisionList.contains(strDecisionId))
                {
                    continue;
                }
                slUniqueDecisionList.add(strDecisionId);

                mlDecisionObjectList.add(mapRelatedObjectInfo);
            }
            return mlDecisionObjectList;
        }
        catch (FrameworkException fwe)
        {
            fwe.printStackTrace();
            throw new MatrixException(fwe);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new MatrixException(e);
        }
    }

    /**
     * This method is used to show the parent object column data in Decision related object table
     * This is used for column "ParentObjects" in table "PMCDecisionsRelatedDecisions"
     *
     * @param context The Matrix Context object
     * @param args The arguments, it contains objectList and paramList maps
     * @return The Vector object containing
     * @throws MatrixException if operation fails
     */
    public Vector getColumnParentObjectData (Context context, String[] args) throws MatrixException
    {
        try
        {
            final String SELECT_RELATED_OBJECT_ID   = "from["+DomainConstants.RELATIONSHIP_DECISION+"].to.id";
            final String SELECT_RELATED_OBJECT_NAME = "from["+DomainConstants.RELATIONSHIP_DECISION+"].to.name";
            final String SELECT_RELATED_OBJECT_TYPE = "from["+DomainConstants.RELATIONSHIP_DECISION+"].to.type";

            Map programMap = (Map) JPO.unpackArgs(args);
            MapList mlObjectList = (MapList) programMap.get("objectList");

            Vector vecColumnValues = new Vector(mlObjectList.size());

            Object objRelatedObjectIds = null;
            Object objRelatedObjectName = null;
            Object objRelatedObjectType = null;
            String strRelatedObjectId = "";
            String strRelatedObjectName = "";
            String strRelatedObjectType = "";
            StringList slRelatedObjectIdList = null;
            StringList slRelatedObjectNameList = null;
            StringList slRelatedObjectTypeList = null;
            StringList slHTMLAchors = null;
            String strHTMLAnchor = "";
            String strTypeSymName = "";
            String strCommonDirectory = EnoviaResourceBundle.getProperty(context, "eServiceSuiteFramework.CommonDirectory");
            String strTypeIcon = "";

            int nTotalRelatedObjects = 0;

            final String HTML_TEMPLATE = "<a href='javascript:showModalDialog(\"../common/emxTree.jsp?objectId=${OBJECTID}\", \"875\", \"550\", \"false\", \"popup\")' title=\"${NAME}\"><img src=\"../" + strCommonDirectory + "/images/${IMAGE}\" border=\"0\"/>${NAME}</a>";

            Map mapObjectInfo = null;
            for (Iterator itrObjectList = mlObjectList.iterator();itrObjectList.hasNext();)
            {
                mapObjectInfo = (Map) itrObjectList.next();

                slRelatedObjectIdList = new StringList();

                objRelatedObjectIds = mapObjectInfo.get(SELECT_RELATED_OBJECT_ID);
                objRelatedObjectName = mapObjectInfo.get(SELECT_RELATED_OBJECT_NAME);
                objRelatedObjectType = mapObjectInfo.get(SELECT_RELATED_OBJECT_TYPE);

                if (objRelatedObjectIds instanceof String)
                {
                    strRelatedObjectId = (String)objRelatedObjectIds;
                    strRelatedObjectName = (String)objRelatedObjectName;
                    strRelatedObjectType = (String)objRelatedObjectType;

                    // Find type icon
                    strTypeSymName = FrameworkUtil.getAliasForAdmin(context, "Type", strRelatedObjectType, true);
                    try{
                        strTypeIcon = EnoviaResourceBundle.getProperty(context, "emxFramework.smallIcon." + strTypeSymName);
                    }catch(Exception e){
                        strTypeIcon  = EnoviaResourceBundle.getProperty(context, "emxFramework.smallIcon.defaultType");
                    }

                    strHTMLAnchor = FrameworkUtil.findAndReplace(HTML_TEMPLATE, "${OBJECTID}", strRelatedObjectId);
                    strHTMLAnchor = FrameworkUtil.findAndReplace(strHTMLAnchor, "${NAME}", strRelatedObjectName);
                    strHTMLAnchor = FrameworkUtil.findAndReplace(strHTMLAnchor, "${IMAGE}", strTypeIcon);

                    vecColumnValues.add(strHTMLAnchor);
                }
                else if (objRelatedObjectIds instanceof StringList)
                {
                    slRelatedObjectIdList = (StringList)objRelatedObjectIds;
                    slRelatedObjectNameList = (StringList)objRelatedObjectName;
                    slRelatedObjectTypeList = (StringList)objRelatedObjectType;

                    nTotalRelatedObjects = slRelatedObjectIdList.size();
                    slHTMLAchors = new StringList(nTotalRelatedObjects);
                    for (int i = 0; i < nTotalRelatedObjects; i++) {
                        strRelatedObjectId = (String)slRelatedObjectIdList.get(i);
                        strRelatedObjectName =XSSUtil.encodeForHTML(context, (String)slRelatedObjectNameList.get(i));
                        strRelatedObjectType = (String)slRelatedObjectTypeList.get(i);

                        // Find type icon
                        strTypeSymName = FrameworkUtil.getAliasForAdmin(context, "Type", strRelatedObjectType, true);
                        try{
                            strTypeIcon = EnoviaResourceBundle.getProperty(context, "emxFramework.smallIcon." + strTypeSymName);
                        }catch(Exception e){
                            strTypeIcon  = EnoviaResourceBundle.getProperty(context, "emxFramework.smallIcon.defaultType");
                        }

                        strHTMLAnchor = FrameworkUtil.findAndReplace(HTML_TEMPLATE, "${OBJECTID}", strRelatedObjectId);
                        strHTMLAnchor = FrameworkUtil.findAndReplace(strHTMLAnchor, "${NAME}", strRelatedObjectName);
                        strHTMLAnchor = FrameworkUtil.findAndReplace(strHTMLAnchor, "${IMAGE}", strTypeIcon);

                        slHTMLAchors.add(strHTMLAnchor);
                    }

                    vecColumnValues.add(FrameworkUtil.join(slHTMLAchors, ","));
                }
                else {
                    vecColumnValues.add(DomainObject.EMPTY_STRING);
                }
            }
            return vecColumnValues;
        }
        catch (FrameworkException fwe)
        {
            fwe.printStackTrace();
            throw new MatrixException(fwe);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new MatrixException(e);
        }
    }

//  End:R207:PRG Integration of Common Meetings and Decisions
  // start: Added for Task calender feature
    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList getExcludeOIDForCalendar(Context context, String []args)throws Exception {
        //get All Calender
        MapList mapCalendarList = null;
        StringList select = new StringList();
        select.add(DomainConstants.SELECT_ID);
        select.add(DomainConstants.SELECT_NAME);
        mapCalendarList =DomainObject.findObjects(context,PropertyUtil.getSchemaProperty(context,"type_WorkCalendar"),"*",null,select);
        StringList slExcludeId = new StringList();
        Iterator itrCalender = mapCalendarList.iterator();
        while(itrCalender.hasNext()){
            Map mapCalendar = (Map)itrCalender.next();
            slExcludeId.add(mapCalendar.get(DomainConstants.SELECT_ID));
        }

        com.matrixone.apps.common.Person person = com.matrixone.apps.common.Person.getPerson(context);
        Company company = person.getCompany(context);
        String strCompanyCalendar = PropertyUtil.getSchemaProperty(context,"relationship_CompanyCalendar");
        StringList objectSelects = new StringList();
        objectSelects.add(DomainConstants.SELECT_NAME);
        objectSelects.add(DomainConstants.SELECT_ID);
        objectSelects.add(DomainConstants.SELECT_CURRENT);


        MapList companyCalenders =  company.getRelatedObjects(
                                        context,                    // eMatrix context
                                        strCompanyCalendar,    // relationship pattern
                                        DomainConstants.QUERY_WILDCARD,             // object pattern
                                        objectSelects,              // object selects
                                        null,                       // relationship selects
                                        false,                      // to direction
                                        true,                       // from direction
                                        (short) 1,                  // recursion level
                                        null,                // object where clause
                                        DomainConstants.EMPTY_STRING,0);              // relationship where clause

       Iterator itrCalenderCompany = companyCalenders.iterator();
       while(itrCalenderCompany.hasNext()){
           Map mapCalendar = (Map)itrCalenderCompany.next();
           if(mapCalendar.get(DomainConstants.SELECT_CURRENT).equals("Active")){
               slExcludeId.remove(mapCalendar.get(DomainConstants.SELECT_ID));
           }
       }
       return slExcludeId;
    }
    // End: Added for Task calender feature

    /**
     *
     * This API to be used for table column to display linked person object with image icon.
     * It returns the Full name of Owner/Assignee/or any Person Object which can be selected on business object or relationship.
     * String provided in Expression field of Column will be used as a Key to fetch Person value which had been stored in object map against this key.
     * Value of Person Object must be stored in object map against this key only.(In Table or Expand Table method.)
     *
     *
     * @param context The Matrix Context object
     * @param args Packed program and request maps for the table
     * @return Vector of owners full name
     * @throws Exception if operation fails
     */

    public StringList getPersonFullName(Context context,String[] args) throws Exception
    {
    	StringList vcPersonFullName = new StringList();
    	try
    	{
    		HashMap programMap         = (HashMap) JPO.unpackArgs(args);
    		MapList objectList = (MapList)programMap.get("objectList");
    		Map mapColumnMap = (Map)programMap.get("columnMap");

    		//Code for Export bug fix for Owner Column IR-205151V6R2014
    		Map paramListMap = (Map)programMap.get("paramList");
    		String reportFormat = (String) paramListMap.get("reportFormat");
    		String exportFormat = (String) paramListMap.get("exportFormat");

    		String strBusExpression=(String)mapColumnMap.get("expression_businessobject");
    		String strRelExpression=(String)mapColumnMap.get("expression_relationship");
                String strExpressionData= ProgramCentralConstants.EMPTY_STRING;

    		if(null != strBusExpression && !"".equals(strBusExpression))
    		{
    			strExpressionData= strBusExpression;
    		}
    		else if(null != strRelExpression && !"".equals(strRelExpression))
    		{
    			strExpressionData =strRelExpression;
    		}
    		else if((null == strBusExpression && null == strRelExpression)|| ("" .equals(strBusExpression) && "".equals(strRelExpression)))
    		{
    			throw new MatrixException("Invalid Key in Expression  "+strBusExpression +" : "+strRelExpression);
    		}

    		Map mapObjects = null;
    		String strObjectId = DomainConstants.EMPTY_STRING;
    		DomainObject dmoObject = null;
    		String  strOwnerLoginName = DomainConstants.EMPTY_STRING;
    		String strLanguage = ((Locale)ProgramCentralUtil.getLocale(context)).getLanguage();
    		String strPersonFullName = DomainConstants.EMPTY_STRING;
    		String strPersonId = DomainConstants.EMPTY_STRING;
    		boolean isRoleType = false;
    		boolean isGroupType = false;
    		String imgRole = "<img src=\"../common/images/iconSmallRole.gif\" border=\"0\" id=\"\" title=\"\"></img>";
    		String imgPerson ="<img src=\"../common/images/iconSmallPerson.gif\" border=\"0\" id=\"\" title=\"\"></img>";
    		String imgGroup = "<img src=\"../common/images/iconSmallGroup.gif\" border=\"0\" id=\"\" title=\"\"></img>";

    		for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();)
    		{
    			StringBuffer strPersonFullNameBuffer = new StringBuffer();
    			mapObjects = (Map) itrObjects.next();

    			strOwnerLoginName = (String)mapObjects.get(strExpressionData);
    			isRoleType = "true".equalsIgnoreCase((String)mapObjects.get(strExpressionData+".isarole"))?true:false;
    			isGroupType = "true".equalsIgnoreCase((String)mapObjects.get(strExpressionData+".isagroup"))?true:false;

    			if(ProgramCentralUtil.isNullString(strOwnerLoginName))
    			{
    				strObjectId = (String)mapObjects.get(DomainConstants.SELECT_ID);
    				StringList selectList = new StringList();
    				selectList.add(strExpressionData);
    				selectList.add(strExpressionData+".isaperson");
    				selectList.add(strExpressionData+".isagroup");
    				selectList.add(strExpressionData+".isarole");
    				Map objectMap = dmoObject.newInstance(context,strObjectId).getInfo(context, selectList);
    				strOwnerLoginName = (String)objectMap.get(strExpressionData);
    				isRoleType = "true".equalsIgnoreCase((String)mapObjects.get(strExpressionData+".isarole"))?true:false;
    				isGroupType = "true".equalsIgnoreCase((String)mapObjects.get(strExpressionData+".isagroup"))?true:false;
    			}

    			if(isRoleType)
    			{
    				strPersonFullName = i18nNow.getRoleI18NString(strOwnerLoginName,strLanguage);
    				strPersonFullNameBuffer.append(imgRole+" ");
    				strPersonFullNameBuffer.append(XSSUtil.encodeForHTML(context,strPersonFullName));
    			}
    			else if(isGroupType)
    			{
    				strPersonFullName = i18nNow.getMXI18NString(strOwnerLoginName, "", strLanguage, "Group");
    				strPersonFullNameBuffer.append(imgGroup+" ");
    				strPersonFullNameBuffer.append(XSSUtil.encodeForHTML(context,strPersonFullName));
    			}

    			//Code for Export bug fix for Owner Column
    			else if("CSV".equalsIgnoreCase(reportFormat) && "CSV".equalsIgnoreCase(exportFormat)){
    				strPersonFullName = PersonUtil.getFullName(context, strOwnerLoginName);
    				strPersonFullNameBuffer.append(XSSUtil.encodeForHTML(context,strPersonFullName));
    			}                
    			else
    			{
    				//This code is written to check for person object having admin object but no business object.
    				//PersonUtil.getPersonObjectID() is only giving value if business object exist.
    				String sCommandStatement = "temp query bus $1 $2 $3 select $4 dump $5";
    				String result =  MqlUtil.mqlCommand(context, sCommandStatement,DomainConstants.TYPE_PERSON,strOwnerLoginName,"*","id","|");
    				StringList slResult = FrameworkUtil.splitString(result, "|");
    				if(ProgramCentralUtil.isNotNullString(result) && slResult.size()>0)
    				{
    					strPersonId = (String)slResult.get(3);
    					strPersonFullName = PersonUtil.getFullName(context, strOwnerLoginName);
    					strPersonFullNameBuffer.append(imgPerson);
    					strPersonFullNameBuffer.append("<a href='../common/emxTree.jsp?objectId=");
    					strPersonFullNameBuffer.append(strPersonId).append("'>");
    					strPersonFullNameBuffer.append(XSSUtil.encodeForXML(context,strPersonFullName));
    					strPersonFullNameBuffer.append("</a>");
    				}
    				else
    				{
    					strPersonFullNameBuffer.append(XSSUtil.encodeForHTML(context,strOwnerLoginName));
    				}
    			}
    			vcPersonFullName.add(strPersonFullNameBuffer.toString());
    		}
    	}
    	catch (Exception ex)
    	{
    		throw new MatrixException(ex);
    	}
    	return vcPersonFullName;
    }

    /**
     * This trigger get the old object from that it get associated publish subscribe object
     * then get the associated Event objects from which it fetch the person objects and finally
     * get the revised object's id and push the old object's subscription events to the revised
     * object for those person objects.
     *
     * @author RG6
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            : object id - Old ObjectId before revising the Object
     *            : newRev    - Latest revision of the object which is created
     * @return int : int based on success or failure of the trigger
     *              0 - Success
     *              1 - Failure
     * @throws MatrixException
     *             if the operation fails
     * @since PRG R210
     */
    public static int triggerLinkSubscriptionsToLatestRevision(Context context,
            String[] args) throws MatrixException {
        try {
            ContextUtil.startTransaction(context, true);

            // Check method arguments
            if (context == null || args == null) {
                throw new IllegalArgumentException();
            }

            String strObjectId = args[0]; // object id of previous revision
            String strNewRev = args[1];

            if (strObjectId == null || "".equalsIgnoreCase(strObjectId)) {
                throw new IllegalArgumentException();
            }

            if (strNewRev == null || "".equalsIgnoreCase(strNewRev)) {
                throw new IllegalArgumentException();
            }

            SubscriptionUtil sbUtilObj = new SubscriptionUtil();
            String strLanguage = context.getSession().getLanguage();
            String strLatestRevObjId = ""; // new object id of revised object
            int count = 0;
            String SELECT_NEXT_ID = EnoviaResourceBundle.getProperty(context, "emxProgramCentral.Subscriptions.Select.Next.Id");
            StringList slBusSel = new StringList();
            slBusSel.add(SELECT_NEXT_ID);
            DomainObject dmoOldObject = DomainObject.newInstance(context,strObjectId);

            Map dObjInfoMap = dmoOldObject.getInfo(context, slBusSel);
            // get the new revision's object id
            String nextId = (String)dObjInfoMap.get(SELECT_NEXT_ID);

            strLatestRevObjId = nextId;  // new object ID

            //get subscribed events list for the old object
            Map objEventMap = SubscriptionUtil.getObjectSubscribedEventsMap(context, strObjectId);
            Map subscriptionMap = new HashMap();  // subscription map containing both subscribed and pushed events
            if(objEventMap != null && objEventMap.size()>0){
                Map SubscribedMap = (Map)objEventMap.get("Subscription Map");
                Map pushedSubMap =  (Map)objEventMap.get("Pushed Subscription Map");
                subscriptionMap.putAll(SubscribedMap);
                subscriptionMap.putAll(pushedSubMap);
            }

            Set eventKeySet = subscriptionMap.keySet();  // iterate over the events
            StringList slPersonNotificationList = new StringList();
            StringList slPersonlist = new StringList();
            for(Iterator itEventNames = eventKeySet.iterator();itEventNames.hasNext();){
                String eventName = (String)itEventNames.next();
                //get associated person object list for the subscribed event.
                slPersonlist =sbUtilObj.getSubscribers(context, strObjectId, (HashMap)subscriptionMap.get(eventName),"object",true);
                // array containing person names
                String[] strArrayObjPerson = new String[slPersonlist.size()];
                count = 0;  //reset the counter
                while(count<slPersonlist.size()){
                    String strPersonName = (String)slPersonlist.get(count); // list content both name and notification type separated by "|"
                    slPersonNotificationList.clear();
                    slPersonNotificationList = FrameworkUtil.split(strPersonName, "|");
                    String strActualPerosnName = (String)slPersonNotificationList.get(0);
                    String strPersonObjID = PersonUtil.getPersonObjectID(context, strActualPerosnName); // find the person object id
                    strArrayObjPerson[count] = strPersonObjID;
                    count++;
                }
                // push the subscription from old object,to the revised object for each event
                SubscriptionUtil.createPushSubscription(context, strLatestRevObjId, eventName, strArrayObjPerson, strLanguage);
            }

        } catch (Exception e) {
            ContextUtil.abortTransaction(context);
            throw new MatrixException(e);
        }
        ContextUtil.commitTransaction(context);

        return 0;
    }

    /**
     * This method is used get formatted value of currency as per context locale and display currency symbol or ISO4217 code as per locale input
     * @param context the eMatrix <code>Context</code> object
     * @param strCurrency
     *                  Currency value such as Dollar, Yuan Renimimbi, Yen, Euro, Pound.
     *                  If the currency value is different than system defined then need
     *                  to add ISO 4217 value in the properties file.
     * @param nStdCost
     *                  Cost value which needs to be formatted.
     * @return String
     *                  which contains formatted value of currency as per locale.
     *
     * @throws MatrixException
     */
    public static String getFormattedCurrencyValue(Context context, String strCurrency,double nStdCost) throws MatrixException
    {
        return getFormattedCurrencyValue(context, context.getLocale(),strCurrency, nStdCost);
    }

    /**
     * This method is used get formatted value of currency as per given locale and display currency symbol or ISO4217 code as per locale input
     * @param context the eMatrix <code>Context</code> object
     * @param localeObj
     *                  LocaleObj of Browser or defined
     * @param strCurrency
     *                  Currency value such as Dollar, Yuan Renimimbi, Yen, Euro, Pound.
     *                  If the currency value is different than system defined then need
     *                  to add ISO 4217 value in the properties file.
     * @param nStdCost
     *                  Cost value which needs to be formatted.
     * @return String
     *                  which contains formatted value of currency as per locale.
     *
     * @throws MatrixException
     */
    public static String getFormattedCurrencyValue(Context context, Locale localeObj, String strCurrency,double nStdCost) throws MatrixException
    {
        try {
            if(null==localeObj)
            {
                localeObj = context.getLocale();
            }
        NumberFormat format = NumberFormat.getCurrencyInstance(localeObj);
        String strDispCurrency = strCurrency;
        if(strCurrency.trim().contains(" "))
        {
            strDispCurrency = strCurrency.replace(" ", "_");
        }
        String strCurrencyCode = EnoviaResourceBundle.getProperty(context,"emxProgramCentral.CurrencyCode.ISO4217."+strDispCurrency);
        Currency currency = Currency.getInstance(strCurrencyCode);
        format.setCurrency(currency);
            //Currently fractional setting are according to java default.
            //if someone wants to change fractional setting to user default then following needs to be uncomment.
            //format.setMaximumFractionDigits(5);
        if (format instanceof DecimalFormat) {
            DecimalFormat decimalFormat = (DecimalFormat) format;
            decimalFormat.setNegativePrefix("(" + decimalFormat.getPositivePrefix());
            decimalFormat.setNegativeSuffix(")");
        }
        String strFormattedCost = format.format(nStdCost);
        return strFormattedCost;
    }
        catch (FrameworkException e)
    {
            throw new MatrixException(e);
    }
    }

    /**
     * This method is used get formatted value of currency as per context locale into number.
     * @param context the eMatrix <code>Context</code> object
     * @param strCostValue
     *                  Cost value which needs to be formatted.
     * @return BigDecimal
     *                  which contains formatted value of currency in number as per locale.
     *
     * @throws MatrixException
     */
    public static BigDecimal getNormalizedCurrencyValue(Context context,String strCostValue) throws MatrixException
    {
        return ProgramCentralUtil.getNormalizedCurrencyValue(context,context.getLocale(), strCostValue);
    }
    /**
     * This method is used get formatted value of currency as per given locale into number.
     * @param context the eMatrix <code>Context</code> object
     * @param localeObj
     *                  LocaleObj of Browser or defined
     * @param strCostValue
     *                  Cost value which needs to be formatted.
     * @return BigDecimal
     *                  which contains formatted value of currency in number as per locale.
     *
     * @throws MatrixException
     */
    public static BigDecimal getNormalizedCurrencyValue(Context context, Locale locale, String strCostValue) throws MatrixException
    {
        return ProgramCentralUtil.getNormalizedCurrencyValue(context,locale, strCostValue);
    }

    /**
     * This method is used get formatted value of number as per context locale.
     * @param context the eMatrix <code>Context</code> object
     * @param number
     *                  Number which needs to be formatted.
     * @return String
     *                  which contains formatted value of number as per locale.
     *
     * @throws MatrixException
     */
    public static String getFormattedNumberValue(Context context,double number) throws MatrixException
    {
        return getFormattedNumberValue(context, context.getLocale(), number);
    }
    /**
     * This method is used get formatted value of number as per given locale.
     * @param context the eMatrix <code>Context</code> object
     * @param localeObj
     *                  LocaleObj of Browser or defined
     * @param number
     *                  Number which needs to be formatted.
     * @return String
     *                  which contains formatted value of number as per locale.
     *
     * @throws MatrixException
     */
    public static String getFormattedNumberValue(Context context, Locale localeObj, double number) throws MatrixException
    {
        try {
            if(null==localeObj)
            {
                localeObj = context.getLocale();
            }
            NumberFormat format = NumberFormat.getNumberInstance(localeObj);
            String strFormattedCost = format.format(number);
            return strFormattedCost;
        }
        catch (Exception e)
        {
            throw new MatrixException(e);
        }
    }
    /**
     * This method is used to get currency symbol of given currency
     * @param context the eMatrix <code>Context</code> object
     * @param strCurrency
     *                  Currency value which needs to be formatted.
     * @return String
     *                  which contains formatted value of currency as per locale.
     *
     * @throws MatrixException
     */
    public static String getCurrencySymbol(Context context,String strCurrency) throws MatrixException
    {
        return getCurrencySymbol(context, context.getLocale(), strCurrency);
    }
    /**
     * This method is used to get currency symbol of given currency
     * @param context the eMatrix <code>Context</code> object
     * @param localeObj
     *                  LocaleObj of Browser or defined
     * @param nStdCost
     *                  Cost value which needs to be formatted.
     * @return String
     *                  which contains formatted value of currency as per locale.
     *
     * @throws MatrixException
     */
    public static String getCurrencySymbol(Context context, Locale localeObj,String strCurrency) throws MatrixException
    {
        try
        {
            String strDispCurrency = strCurrency;
            NumberFormat format = NumberFormat.getCurrencyInstance(localeObj);
            if(strCurrency.trim().contains(" "))
            {
                strDispCurrency = strCurrency.replace(" ", "_");
            }
            String strCurrencyCode = EnoviaResourceBundle.getProperty(context,"emxProgramCentral.CurrencyCode.ISO4217."+strDispCurrency);
            Currency currency = Currency.getInstance(strCurrencyCode);
            format.setCurrency(currency);
            String strCurrencySymbol = format.getCurrency().getSymbol(localeObj);
            return strCurrencySymbol;
    }
        catch (Exception e)
    {
        throw new MatrixException(e);
    }
    }
    /**
     * Returns the user's preferred currency. Incase no currency is set then an error message will be shown.
     * @param context the ENOVIA <code>Context</code> user.
     * @param strNoticeKey the string error message key.
     * @return the currency set by user in preferences page.
     * @throws MatrixException if operation fails.
     */
    public static String getUserPreferenceCurrency(Context context, String strNoticeKey)
    throws MatrixException{
        try
        {
            String sLanguage = context.getSession().getLanguage();
            PersonUtil contextUser =  new PersonUtil();
            String strPreferredCurrency = contextUser.getCurrency(context);
            if(ProgramCentralUtil.isNotNullString(strPreferredCurrency) &&
                    !strPreferredCurrency.equals("As Entered")
                    && !strPreferredCurrency.equals("Unassigned")){
                return strPreferredCurrency;
            }
            else{
                String strTotalLabel = EnoviaResourceBundle.getProperty(context, "ProgramCentral", 
        				strNoticeKey,sLanguage);
                throw new MatrixException(strTotalLabel);
            }
        }
        catch (MatrixException e){
            throw e;
        }
        catch (Exception e){
            throw new MatrixException(e);
        }
    }

    public static String getParentProject(Context context,String[] args) throws Exception{
        String prjId = "";
        try{
            String taskId = args[0];
            if(ProgramCentralUtil.isNullString(taskId)){
                throw new Exception();
            }

            DomainObject domObj = DomainObject.newInstance(context);
            domObj.setId(taskId);
            prjId = Task.getParentProject(context, domObj);
        }
        catch(Exception e){
            e.printStackTrace();
            throw new MatrixException(e);
        }
        BufferedWriter writer = new BufferedWriter(new MatrixWriter(context));
        writer.write(prjId);
        writer.flush();
        return prjId;
    }


    //Added : H1A : IR-148090V6R2013x : 08/06/2012
    /**
     * This method is used to Exclude Document objects which have the is Versioned Object attribute set to true.
     * @param context the eMatrix <code>Context</code> object
     * @param args  arguments
     * @return StringList  List of document ids to be excluded
     * @throws Exception if the operation fails
     */
    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList getExcludeOIDForVersionedDocuments(Context context, String args[]) throws Exception
    {
        MapList mapDocumentList = null;
        StringList select = new StringList();
        select.add(DomainConstants.SELECT_ID);
        select.add(DomainConstants.SELECT_NAME);
        select.add(CommonDocument.SELECT_IS_VERSION_OBJECT);
        String whereExp = "attribute["+CommonDocument.ATTRIBUTE_IS_VERSION_OBJECT+"] == True";

        //String strTypePattern =CommonDocument.TYPE_DOCUMENTS+","+PropertyUtil.getSchemaProperty("type_DOCUMENTCLASSIFICATION")+","+DomainConstants.TYPE_ECO+","+DomainConstants.TYPE_ECR+","+DomainConstants.TYPE_PART+","+DomainConstants.TYPE_PRODUCTLINE+","+PropertyUtil.getSchemaProperty("type_ExternalDeliverable");
        String strTypePattern =CommonDocument.TYPE_DOCUMENTS;
        mapDocumentList =DomainObject.findObjects(context,strTypePattern,"*",whereExp,select);
        StringList  slVersionedDocumentIds = new StringList();
        int size = mapDocumentList.size();
        for(int i=0; i<size; i++)
        {
            Map mapDocumentObj = (Map) mapDocumentList.get(i);
            String strId = (String) mapDocumentObj.get(CommonDocument.SELECT_ID);
            slVersionedDocumentIds.add(strId);
        }

        return slVersionedDocumentIds;
    }

    /**
     * Exclude OID happens depending upon user role pass to emxFullSearch parameter arguments "field=TYPES=Person:USERROLE="
     * @param context The matrix context object
     * @param returns StringList containing Person whicn not in any Organization
     * @param args The arguments, it contains programMap
     * @throws Exception if operation fails
     */
    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList getexcludeOIDforPersonSearch(Context context, String[] args)throws Exception
    {
        try
        {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strRoles = (String)programMap.get("field");
            strRoles = strRoles.substring(22, strRoles.length());//"TYPES=Person:USERROLE="
            String[] saRoles = strRoles.split(",");
            StringList slPersonNames = new StringList();
            String strPersonRole = "";
            StringList slPersonIds = new StringList();
            StringList slPersonRoles = new StringList();
            slPersonRoles.add(ProgramCentralConstants.ROLE_PROJECT_LEAD);
            slPersonRoles.add(ProgramCentralConstants.ROLE_EXTERNAL_PROJECT_LEAD);
            slPersonRoles.add(ProgramCentralConstants.ROLE_PROJECT_USER);
            slPersonRoles.add(ProgramCentralConstants.ROLE_EXTERNAL_PROJECT_USER);
            slPersonRoles.add(ProgramCentralConstants.ROLE_VPLM_VIEWER);
            slPersonRoles.add(ProgramCentralConstants.ROLE_VPLM_PROJECT_LEADER);
            slPersonRoles.add(ProgramCentralConstants.ROLE_VPLM_ADMIN);
            slPersonRoles.add(ProgramCentralConstants.ROLE_VPLM_PROJECT_OWNER);

            for(int i = 0; i<saRoles.length;i++)
            {
                if(slPersonRoles.contains(saRoles[i]))
                {
                    strPersonRole = saRoles[i];
                    String strPersonName ="";
                    String strPersonId ="";
                    StringList slpersonOrg = new StringList();
                    int nLength;
                    slPersonNames = PersonUtil.getPersonFromRole(context, strPersonRole);
                    for(int nCount=0;nCount<slPersonNames.size();nCount++)
                    {
                        strPersonName=(String)slPersonNames.get(nCount);
                        //This method "PersonUtil.getMemberOrganizations()" is used to check if the person is associated with any organisation/company
                        //If the person gets deleted then the object remains as an Admin object which we can see in "Business Modeler",
                        //So it will give exception for method PersonUtil.getPersonObjectID()
                        strPersonId = PersonUtil.getPersonObjectID(context, strPersonName);
                        DomainObject domPerson = new DomainObject(strPersonId);
                        slpersonOrg = domPerson.getInfoList(context, "to["+DomainConstants.RELATIONSHIP_EMPLOYEE+"].from.id");
                        nLength = slpersonOrg.size();
                        if(nLength==0)
                        {
                              slPersonIds.add(strPersonId);
                            }
                }
            }
            }
            return slPersonIds;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Method to check if LBC (Library Central) is installed
     *
     * @param context
     * @param args
     * @return true - if LBC is installed
     *         false - if LBC is not Installed
     * @exception throws FrameworkException
     * @since R215
     */
     public boolean isLBCInstalled(Context context,String args[]) throws FrameworkException
     {
         return  FrameworkUtil.isSuiteRegistered(context,"appVersionLibraryCentral",false,null,null);
     }
     
     /**Gets i18n value for passed key along with placeholders in keys.
  	  * @param context the eMatrix <code>Context</code> object
  	  * @param args holds the following input arguments:
  	  *       msgKey: key to translate
  	  *       Key   : Array of placeholder keys
  	  *       Value : Array of placeholder values 
  	  * @throws MatrixException if operation fails
  	  */
     public String geti18nString(Context context, String[] args) throws MatrixException
     {
    	 String sReturnValue = "";

    	 try {
    		 Map mParamMap = (Map) JPO.unpackArgs(args);
    		 String sPromoteMsg = (String) mParamMap.get("msgKey");
    		 String sKey[] = (String[]) mParamMap.get("placeHolderKeys");
    		 String sValue[] = (String[]) mParamMap.get("placeHolderValues");

    		 sReturnValue = getMessage(context, sPromoteMsg, sKey, sValue, null);
    	 } catch (Exception e) {
    		 throw new MatrixException(e);
    	 }
    	 return sReturnValue;
     }

/**
     * This method is called to get "Previous Project State" attribute of "Project Space". 
     * @param context
     *        context object which is used while fetching data related application.
     *        
     * @param args
     *        Holds input argument.
     *        
     * @throws Exception
     *         Exception can be thrown in case of method fails to execute.
     */
	public String getPreviousState(Context context,String[] args) throws Exception {
		String previousState	=	DomainConstants.EMPTY_STRING;
    	 Map programMap =  JPO.unpackArgs(args);
	  	String projectId = (String)programMap.get(ProgramCentralConstants.SELECT_ID);
    	 ProjectSpace projectObj = (ProjectSpace)DomainObject.newInstance(context,
    			 DomainConstants.TYPE_PROJECT_SPACE,DomainConstants.TYPE_PROGRAM);
	 	projectObj.setId(projectId);
	   	 
    	 previousState	= projectObj.getAttributeValue(context,ProgramCentralConstants.ATTRIBUTE_PREVIOUS_STATE);
    	 if (previousState.contains(ProgramCentralConstants.COMMA)) {
    		 String[] stateArray	=	previousState.split(ProgramCentralConstants.COMMA);
	   		 previousState	=	stateArray[0];
	   	 }
	   	 return previousState;
	}
	
	/**
     * This method is called to get Previous policy of the ProjectSpace. 
     * @param context
     *        context object which is used while fetching data related application.
     *        
     * @param args
     *        Holds input argument.
     *        
     * @throws Exception
     *         Exception can be thrown in case of method fails to execute.
     */
	    
    public String getPreviousPolicy(Context context,String[] args) throws Exception {
      	String previousPolicy	=	DomainConstants.EMPTY_STRING;
    	 Map programMap = JPO.unpackArgs(args);
	   	String projectId = (String)programMap.get(ProgramCentralConstants.SELECT_ID);
      	
    	 ProjectSpace projectObj = (ProjectSpace)DomainObject.newInstance(context,
    			 DomainConstants.TYPE_PROJECT_SPACE,	DomainConstants.TYPE_PROGRAM);
	 	projectObj.setId(projectId);
      	 
    	 previousPolicy = projectObj.getAttributeValue(context,ProgramCentralConstants.ATTRIBUTE_PREVIOUS_STATE);
    	 if (previousPolicy.contains(ProgramCentralConstants.COMMA)) {
    		 String[] stateArray	=	previousPolicy.split(ProgramCentralConstants.COMMA);
      		previousPolicy	=	stateArray[1];
      	 } else {
      		 previousPolicy	=	DomainConstants.POLICY_PROJECT_SPACE;
      	 }
      	 return previousPolicy;
       }
	    
    /**
     * This method sets the value "Previous Project State" attribute of "Project Space" to "Previous state","Previous policy".
     * @param context
     *        context object which is used while fetching data related application.
     *        
     * @param args
     *        Holds input argument.
     *        
     * @throws Exception
     *         Exception can be thrown in case of method fails to execute.
     */ 
    public void setPreviousState(Context context,String[] args) throws Exception {
    	 Map programMap = JPO.unpackArgs(args);
		 String projectId = (String)programMap.get(ProgramCentralConstants.SELECT_ID);
		 String state = (String)programMap.get(ProgramCentralConstants.SELECT_CURRENT);
		 String policy = (String)programMap.get(ProgramCentralConstants.SELECT_POLICY);
		
		 ProjectSpace projectObj = (ProjectSpace)
						DomainObject.newInstance(context,DomainConstants.TYPE_PROJECT_SPACE,DomainConstants.TYPE_PROGRAM);
		
		 projectObj.setId(projectId);
   	 
	   	 String previousPolicy	=	DomainConstants.EMPTY_STRING;
	   	 String previousState	=	projectObj.getAttributeValue(context,ProgramCentralConstants.ATTRIBUTE_PREVIOUS_STATE);
	   	 String previousStateAndPolicy	=	projectObj.EMPTY_STRING;
	   	 
    	 if (previousState.contains(ProgramCentralConstants.COMMA)) {
    		 String[] stateArray	=	previousState.split(ProgramCentralConstants.COMMA);
	   		 previousPolicy	=	stateArray[1];
	   	 } else {
	   		 previousPolicy	=	DomainConstants.POLICY_PROJECT_SPACE;
	   	 }
	   	 
    	 if (ProgramCentralUtil.isNotNullString(policy)) {
    		 previousStateAndPolicy	=	state + ProgramCentralConstants.COMMA + policy;
	   	 } else {
    		 previousStateAndPolicy	=	state + ProgramCentralConstants.COMMA + previousPolicy;
	   	 }
   	 	projectObj.setAttributeValue(context,ProgramCentralConstants.ATTRIBUTE_PREVIOUS_STATE,previousStateAndPolicy);
    }

    
    /**
	 * 
	 * It reloads the SB for portal command with updated values and mainntains the edit mode.  
	 * 
	 * @param context The ENOVIA <code>Context</code> object.
	 * @param args holds information about objects.
	 * @return map contains script with refresh logic.
	 * @throws Exception
	 */
	@com.matrixone.apps.framework.ui.PostProcessCallable
    public Map postProcessPortalCmdSBRefresh(Context context, String[] args) throws Exception {
    	
    	HashMap hashMap = new HashMap();

    	Map programMap = (Map) JPO.unpackArgs(args);
    	Map requestMap = (Map) programMap.get("requestMap");
    	String currentFrameName = XSSUtil.encodeForJavaScript(context, (String)requestMap.get("portalCmdName"));

		
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		sb.append("main:function() {");
		sb.append("var topFrame = findFrame(getTopWindow(),\"").append(currentFrameName).append("\");");
		sb.append("var cmdURL = topFrame.location.href;");
		sb.append("cmdURL += \"&mode=edit\";");
		sb.append("topFrame.location.href = cmdURL;");
		sb.append("}}"); 
		
		hashMap.put("Action","execScript");
		hashMap.put("Message", sb.toString());
		
		return hashMap;
    }

    /**
	 * 
	 * Inherit POV and SOV access for any object when the relationship is being created.  
	 * 
	 * @param context The ENOVIA <code>Context</code> object.
	 * @param args holds information about objects.
	 * @throws Exception
	 */
    public boolean PRGCreateAccessInheritance(Context context, String[] args) throws Exception  
    {
       String fromId = args[0];
       String toId = args[1];
       String command = "";
       boolean contextPushed = false;

       try{
           ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"),DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
           contextPushed = true;
           DomainObject domChildObj = DomainObject.newInstance(context,fromId);
           if(domChildObj.isKindOf(context,DomainConstants.TYPE_PROJECT_SPACE))
           {
        	   command = "modify bus " + toId + " add access bus " + fromId;
           }
           else if(domChildObj.isKindOf(context,DomainConstants.TYPE_WEEKLY_TIMESHEET))
           {
        	   command = "modify bus " + fromId + " add access bus " + toId;
           }
    	   
           MqlUtil.mqlCommand(context, command);

          return true;
       }catch(Exception e){
          throw e;
       }
       finally
       {
           if(contextPushed)
           {
               ContextUtil.popContext(context);
           }
       }

    }

    /**
	 * 
	 * Inherit POV and SOV access for any object when the relationship is being created.  
	 * 
	 * @param context The ENOVIA <code>Context</code> object.
	 * @param args holds information about objects.
	 * @throws Exception
	 */
    public boolean setAccessInheritance(Context context, String[] args) throws Exception
    {
       String fromId = args[0];  // id of from object
       String toId = args[1];  // id of to object
       String onlyIncludeToType = args[2]; // only inherit access to this Type on the To side of the relationship

    	//We are doing through create ownership 
    	//  String accessBits = args[3];  // access bits we want to inherit from the parent

       boolean contextPushed = false;

       try{
    		boolean toInheritAccess = false;
    		String TYPE_THREAD = PropertyUtil.getSchemaProperty(context,"type_Thread" );
           String SELECT_KINDOF_THREAD = "type.kindof[" + TYPE_THREAD+ "]";
    		String TYPE_ISSUE = PropertyUtil.getSchemaProperty(context,"type_Issue" );
           String SELECT_KINDOF_ISSUE = "type.kindof[" + TYPE_ISSUE+ "]";

    		StringList selectables = new StringList(2);
    		selectables.addElement(ProgramCentralConstants.SELECT_ATTRIBUTE_ACCESS_TYPE);
    		selectables.addElement("current.access");

    		// if the To side is a Document and it has Specific Access Type then return
    		DomainObject toObj 	= DomainObject.newInstance(context, toId);
    		String sAccessType 	= toObj.getInfo(context, ProgramCentralConstants.SELECT_ATTRIBUTE_ACCESS_TYPE);

    		if (sAccessType.equalsIgnoreCase("Specific")) {
               return true;
           }

            // Get information about the From side object
    		StringList strFromList = new StringList(5);
           strFromList.add(ProgramCentralConstants.SELECT_KINDOF_PROJECT_SPACE);
           strFromList.add(ProgramCentralConstants.SELECT_KINDOF_PROJECT_CONCEPT);
           strFromList.add(ProgramCentralConstants.SELECT_KINDOF_TASKMANAGEMENT);
           strFromList.add(SELECT_KINDOF_ISSUE);
           strFromList.add(SELECT_KINDOF_THREAD);

           DomainObject fromObj = DomainObject.newInstance(context,fromId);
           Map mpObjectInfo = fromObj.getInfo(context,strFromList);
    		String isProjectSpace 	= (String)mpObjectInfo.get(ProgramCentralConstants.SELECT_KINDOF_PROJECT_SPACE);
    		String isProjectConcept = (String)mpObjectInfo.get(ProgramCentralConstants.SELECT_KINDOF_PROJECT_CONCEPT);
    		String isTaskManagement = (String)mpObjectInfo.get(ProgramCentralConstants.SELECT_KINDOF_TASKMANAGEMENT);
    		String isIssue 			= (String)mpObjectInfo.get(SELECT_KINDOF_ISSUE);
    		String isThread 		= (String)mpObjectInfo.get(SELECT_KINDOF_THREAD);


    		if("true".equalsIgnoreCase(isProjectSpace) || "true".equalsIgnoreCase(isProjectConcept) 
    				||"true".equalsIgnoreCase(isTaskManagement) || "true".equalsIgnoreCase(isIssue)){

    			toInheritAccess = true;

    		}else if ("true".equalsIgnoreCase(isThread)) {

    			StringList slBusSelect = new StringList(3);
    			strFromList.add(ProgramCentralConstants.SELECT_KINDOF_PROJECT_SPACE);
    			strFromList.add(ProgramCentralConstants.SELECT_KINDOF_PROJECT_CONCEPT);
    			strFromList.add(ProgramCentralConstants.SELECT_KINDOF_PROJECT_TEMPLATE);

               String strRelatioshipPattern = DomainConstants.RELATIONSHIP_THREAD + "," + DomainConstants.RELATIONSHIP_SUBTASK;

               MapList mlParent = fromObj.getRelatedObjects(context,     // context.
                        strRelatioshipPattern,                              // rel filter.
                        DomainConstants.QUERY_WILDCARD,                     // type filter
                        slBusSelect,                                        // business object selectables.
                        null,                                               // relationship selectables.
                        true,                                               // expand to direction.
                        false,                                              // expand from direction.
                        (short) 0,                                          // level
                        null,                                               // object where clause
                        null,                                               // relationship where clause
                        0);                                                 //limit

    			for(int i=0,size = mlParent.size(); i<size; i++){
    				Map parentMap 		= (Map)mlParent.get(i);
    				String isProject 	= (String)parentMap.get(ProgramCentralConstants.SELECT_KINDOF_PROJECT_SPACE);
    				String isConcept 	= (String)parentMap.get(ProgramCentralConstants.SELECT_KINDOF_PROJECT_CONCEPT);
    				String isTemplate 	= (String)parentMap.get(ProgramCentralConstants.SELECT_KINDOF_PROJECT_TEMPLATE);

    				if("true".equalsIgnoreCase(isProject) || "true".equalsIgnoreCase(isConcept)|| "true".equalsIgnoreCase(isTemplate)){
    					toInheritAccess = true;
                            break;
                       }
                    }
               }

    		if (toInheritAccess) {
    			ProgramCentralUtil.pushUserContext(context);
              contextPushed = true;

    			if (onlyIncludeToType != null && !onlyIncludeToType.isEmpty()){
                  String typeToInclude = PropertyUtil.getSchemaProperty(context, onlyIncludeToType);

    				if(toObj.isKindOf(context, typeToInclude)) {
    					//Added for bug-424323
    					DomainAccess.createObjectOwnership(context, toId, fromId, DomainAccess.COMMENT_MULTIPLE_OWNERSHIP);
                  }
              }
              }

           return true;

       }catch(Exception e){
    		e.printStackTrace();
          throw e;
    	} finally	{
    		if(contextPushed) {
               ContextUtil.popContext(context);
           }
       }
    }

    
    /**
    *
    * Change the POV of the child to the POV of the parent.
    *
    * @param context The ENOVIA <code>Context</code> object.
    * @param args holds information about objects.
    * @throws Exception
    */
   public boolean SetPOVToParentPOV(Context context, String[] args) throws Exception
   {
      String fromId = args[0];  // id of from object

      // Had to make this variable final in order for it to be accessed in the inner Callable class (see below)
      //
      final String toId = args[1];  // id of to object
      final String relType =args[2]; //relationship 

      String command = "";
      String personId = "";
      try{
         
    	 try {
    	  personId = com.matrixone.apps.domain.util.PersonUtil.getPersonObjectID(context);
    	 }
    	 catch (Exception e){
    		// we fail the above call to getPersonObjectID becasue meetings already set context to UserAgent
    		// we only need the personID for Documents so just ignore the error 
    	 }
    	 
         String RELATIONSHIP_PROJECT_ASSESSMENT = PropertyUtil.getSchemaProperty("relationship_ProjectAssessment");
         String RELATIONSHIP_REFERENCE_DOCUMENT = PropertyUtil.getSchemaProperty("relationship_ReferenceDocument");
         
         String SELECT_PROJECT = "project";
         Map mapParent = null;
         String strParentid = DomainConstants.EMPTY_STRING;
         DomainObject domParentObject = DomainObject.newInstance(context);
		 StringList slBusSelect = new StringList();
		 slBusSelect.add(DomainConstants.SELECT_ID);
		 slBusSelect.add(DomainConstants.SELECT_TYPE);
		 MapList mlParent = new MapList();
		 
         String strFromOrganization = DomainConstants.EMPTY_STRING;
         String strFromProject = DomainConstants.EMPTY_STRING;
         String strToOrganization = DomainConstants.EMPTY_STRING;
         String strToProject = DomainConstants.EMPTY_STRING;
         String bisFromProjectSpaceType = DomainConstants.EMPTY_STRING;
         String bisFromProjectConceptType = DomainConstants.EMPTY_STRING;
         String bisFromTaskManagementType = DomainConstants.EMPTY_STRING;
         String bisToIssueType = DomainConstants.EMPTY_STRING;
         String bisToMeetingType = DomainConstants.EMPTY_STRING;
         String bisToDecisionType = DomainConstants.EMPTY_STRING;
         String bisToDiscussionType = DomainConstants.EMPTY_STRING;
         String bisToMessageType = DomainConstants.EMPTY_STRING;
         String bisToDocumentType = DomainConstants.EMPTY_STRING;
         String bisToWorkspaceVaultType = DomainConstants.EMPTY_STRING;
         String TYPE_ISSUE = PropertyUtil.getSchemaProperty("type_Issue" );
         String SELECT_KINDOF_ISSUE = "type.kindof[" + TYPE_ISSUE+ "]";
         String TYPE_MEETING = PropertyUtil.getSchemaProperty("type_Meeting" );
         String TYPE_DECISION = PropertyUtil.getSchemaProperty("type_Decision" );
         String TYPE_DISCUSSION = PropertyUtil.getSchemaProperty("type_Thread" );
         String TYPE_MESSAGE = PropertyUtil.getSchemaProperty("type_Message" );
         String SELECT_KINDOF_MEETING = "type.kindof[" + TYPE_MEETING+ "]";
         String SELECT_KINDOF_WORKSPACE_VAULT  =  "type.kindof["+DomainConstants.TYPE_WORKSPACE_VAULT+"]";
         String SELECT_KINDOF_DECISION = "type.kindof[" + TYPE_DECISION+ "]";
         String SELECT_KINDOF_DISCUSSION = "type.kindof[" + TYPE_DISCUSSION+ "]";
         String SELECT_KINDOF_MESSAGE = "type.kindof[" + TYPE_MESSAGE+ "]";
         
         String TYPE_DOCUMENTS = PropertyUtil.getSchemaProperty("type_DOCUMENTS");
         String SELECT_KINDOF_DOCUMENTS = "type.kindof[" + TYPE_DOCUMENTS+ "]";

         // Get information about the From side object
         StringList strFromList = new StringList();
         strFromList.add(DomainConstants.SELECT_ORGANIZATION);
         strFromList.add(SELECT_PROJECT);
         strFromList.add(ProgramCentralConstants.SELECT_KINDOF_PROJECT_SPACE);
         strFromList.add(ProgramCentralConstants.SELECT_KINDOF_PROJECT_CONCEPT);
         strFromList.add(ProgramCentralConstants.SELECT_KINDOF_TASKMANAGEMENT);

         DomainObject fromObj = DomainObject.newInstance(context,fromId);
         Map mpObjectInfo = fromObj.getInfo(context,strFromList);

         strFromOrganization = (String)mpObjectInfo.get(DomainConstants.SELECT_ORGANIZATION);
         strFromProject = (String)mpObjectInfo.get(SELECT_PROJECT);
         bisFromProjectSpaceType = (String)mpObjectInfo.get(ProgramCentralConstants.SELECT_KINDOF_PROJECT_SPACE);
         bisFromProjectConceptType = (String)mpObjectInfo.get(ProgramCentralConstants.SELECT_KINDOF_PROJECT_CONCEPT);
         bisFromTaskManagementType = (String)mpObjectInfo.get(ProgramCentralConstants.SELECT_KINDOF_TASKMANAGEMENT);

         // Get information about the To side object
         StringList strToList = new StringList();
         strToList.add(DomainConstants.SELECT_ORGANIZATION);
         strToList.add(SELECT_PROJECT);
         strToList.add(SELECT_KINDOF_ISSUE);
         strToList.add(SELECT_KINDOF_MEETING);
         strToList.add(SELECT_KINDOF_WORKSPACE_VAULT);
         strToList.add(SELECT_KINDOF_DECISION);
         strToList.add(SELECT_KINDOF_DISCUSSION);
         strToList.add(SELECT_KINDOF_DOCUMENTS);
         strToList.add(SELECT_KINDOF_MESSAGE);
         
         DomainObject toObj = DomainObject.newInstance(context,toId);
         mpObjectInfo = toObj.getInfo(context,strToList);
         strToOrganization = (String)mpObjectInfo.get(DomainConstants.SELECT_ORGANIZATION);
         strToProject = (String)mpObjectInfo.get(SELECT_PROJECT);
         bisToIssueType = (String)mpObjectInfo.get(SELECT_KINDOF_ISSUE);
         bisToMeetingType = (String)mpObjectInfo.get(SELECT_KINDOF_MEETING);
         bisToWorkspaceVaultType = (String)mpObjectInfo.get(SELECT_KINDOF_WORKSPACE_VAULT);
         bisToDecisionType = (String)mpObjectInfo.get(SELECT_KINDOF_DECISION);
         bisToDiscussionType = (String)mpObjectInfo.get(SELECT_KINDOF_DISCUSSION);
         bisToMessageType = (String)mpObjectInfo.get(SELECT_KINDOF_MESSAGE);

         bisToDocumentType = (String)mpObjectInfo.get(SELECT_KINDOF_DOCUMENTS);

         // if project or org are blank or project is GLOBAL we don't want to change POV
         if((strFromOrganization.equals("")) || (strFromProject.equals("")) || (strFromProject.equals("GLOBAL")))
        	 return true;
         
         // check to see if the Project or Organization of the child object is different than its parent object
         if((!strFromOrganization.equals(strToOrganization)) || (!strFromProject.equals(strToProject)))
         {
            if (bisToDocumentType.equalsIgnoreCase("true"))  // if To object isKindof Document object
            {
                // check RPE variable to see if we are coming from a create and connect and not just a connect
            	String allowPOVStamping = PropertyUtil.getRPEValue(context, "MX_ALLOW_POV_STAMPING", false);
                if(allowPOVStamping!= null && "true".equals(allowPOVStamping)) // if we are creating this new Document restamp
                {
                    String strRelatioshipPattern = DomainConstants.RELATIONSHIP_TASK_DELIVERABLE + "," + DomainConstants.RELATIONSHIP_SUBTASK + "," + DomainConstants.RELATIONSHIP_PROJECT_VAULTS + "," + DomainConstants.RELATIONSHIP_SUB_VAULTS + "," + DomainConstants.RELATIONSHIP_VAULTED_OBJECTS_REV2 + "," + RELATIONSHIP_PROJECT_ASSESSMENT + "," + RELATIONSHIP_REFERENCE_DOCUMENT + "," + DomainConstants.RELATIONSHIP_RISK;

                    // let only restanp if this Document is getting created in the context of a PS, PC or PT
                	mlParent = toObj.getRelatedObjects(context,                 // context.
    						strRelatioshipPattern,   							// rel filter.
    						DomainConstants.QUERY_WILDCARD,						// type filter
    						slBusSelect,  										// business object selectables.
    						null,           								    // relationship selectables.
    						true,          										// expand to direction.
    						false,           									// expand from direction.
    						(short) 0,  										// level
    						null,           									// object where clause
    						null,												// relationship where clause
    						0);          										//limit

    				for (Iterator itrParent = mlParent.iterator(); itrParent.hasNext();) 
    				{
    					mapParent = (Map) itrParent.next();
    					if(mapParent != null) 
    					{
    						strParentid = (String)mapParent.get(DomainConstants.SELECT_ID);
    				    	domParentObject.setId(strParentid);
    					
    			     		// if in the context of PS, PC or PT then restamp
    				    	if(domParentObject.isKindOf(context, DomainConstants.TYPE_PROJECT_SPACE) || domParentObject.isKindOf(context, DomainConstants.TYPE_PROJECT_TEMPLATE)
    							|| domParentObject.isKindOf(context, DomainConstants.TYPE_PROJECT_CONCEPT)) 
    			    		{
    		                    // give the owner of the Document full access before restamping it
    				    		DomainAccess.createObjectOwnership(context, toId, personId, "Full", DomainAccess.COMMENT_MULTIPLE_OWNERSHIP);
                        	    toObj.setPrimaryOwnership(context, strFromProject,strFromOrganization);
                        	    break;
                            }
    					}
                    }
                }
            }
			
			 else if(DomainConstants.RELATIONSHIP_VAULTED_OBJECTS_REV2.equalsIgnoreCase(relType) ||         DomainConstants.RELATIONSHIP_TASK_DELIVERABLE.equalsIgnoreCase(relType) || RELATIONSHIP_REFERENCE_DOCUMENT.equalsIgnoreCase(relType)){ 
            	//Do Nothing. No need to re-stamp.
            }
			
            // if an Issue or a Meeting or a Discussion make sure we only blank if created in Project Space or Project Concept of Task Management
            else if (bisToIssueType.equalsIgnoreCase("true") || bisToMeetingType.equalsIgnoreCase("true"))
            {
               if (bisFromProjectSpaceType.equalsIgnoreCase("true") || bisFromProjectConceptType.equalsIgnoreCase("true") || bisFromTaskManagementType.equalsIgnoreCase("true"))
               {
                   toObj.setPrimaryOwnership(context, strFromProject,strFromOrganization);
               }
            }
            // only change POV if Folder is in context of PS or PC or PT
            else if (bisToWorkspaceVaultType.equalsIgnoreCase("true"))
            {
                String strRelatioshipPattern = DomainConstants.RELATIONSHIP_PROJECT_VAULTS + "," + DomainConstants.RELATIONSHIP_SUB_VAULTS;

                // let only restanp if this Document is getting created in the context of a PS, PC or PT
    			mlParent = toObj.getRelatedObjects(context,     // context.
						strRelatioshipPattern,   							// rel filter.
						DomainConstants.QUERY_WILDCARD,						// type filter
						slBusSelect,  										// business object selectables.
						null,           								    // relationship selectables.
						true,          										// expand to direction.
						false,           									// expand from direction.
						(short) 0,  										// level
						null,           									// object where clause
						null,												// relationship where clause
						0);          										//limit

				for (Iterator itrParent = mlParent.iterator(); itrParent.hasNext();) 
				{
					mapParent = (Map) itrParent.next();
					if(mapParent != null) 
					{
						strParentid = (String)mapParent.get(DomainConstants.SELECT_ID);
				    	domParentObject.setId(strParentid);
					
			     		// if in the context of PS, PC or PT then restamp
			     		if(domParentObject.isKindOf(context, DomainConstants.TYPE_PROJECT_SPACE) || domParentObject.isKindOf(context, DomainConstants.TYPE_PROJECT_TEMPLATE)
							|| domParentObject.isKindOf(context, DomainConstants.TYPE_PROJECT_CONCEPT)) 
			    		{
                    	    toObj.setPrimaryOwnership(context, strFromProject,strFromOrganization);
                    	    break;
                        }
					}
                }
            }
            // if an Decision make sure we only blank if created in Project Space or Project Concept of Task Management
            else if (bisToDecisionType.equalsIgnoreCase("true"))
            {
               if (bisFromProjectSpaceType.equalsIgnoreCase("true") || bisFromProjectConceptType.equalsIgnoreCase("true") || bisFromTaskManagementType.equalsIgnoreCase("true"))
               {
                   // check RPE variable to see if we are coming from a create and connect and not just a connect
               	   String allowPOVStamping = PropertyUtil.getRPEValue(context, "MX_ALLOW_POV_STAMPING", false);
                   if(allowPOVStamping!= null && "true".equals(allowPOVStamping)) // if we are creating this new Decision restamp
                   {
                       toObj.setPrimaryOwnership(context, strFromProject,strFromOrganization);
                   }
               }
            }
            // if a Discussion make sure we only blank if created in Project Space or Project Concept of Task Management
            else if (bisToDiscussionType.equalsIgnoreCase("true"))
            {
               if (bisFromProjectSpaceType.equalsIgnoreCase("true") || bisFromProjectConceptType.equalsIgnoreCase("true") || bisFromTaskManagementType.equalsIgnoreCase("true"))
               {
		    		toObj.setPrimaryOwnership(context, strFromProject,strFromOrganization);
               }
            }
            else if (bisToMessageType.equalsIgnoreCase("true"))
            {
                String strRelatioshipPattern = DomainConstants.RELATIONSHIP_MESSAGE + "," + DomainConstants.RELATIONSHIP_THREAD;

                // let only restanp if this Document is getting created in the context of a PS, PC or PT
    			mlParent = toObj.getRelatedObjects(context,     // context.
						strRelatioshipPattern,   							// rel filter.
						DomainConstants.QUERY_WILDCARD,						// type filter
						slBusSelect,  										// business object selectables.
						null,           								    // relationship selectables.
						true,          										// expand to direction.
						false,           									// expand from direction.
						(short) 0,  										// level
						null,           									// object where clause
						null,												// relationship where clause
						0);          										//limit

				for (Iterator itrParent = mlParent.iterator(); itrParent.hasNext();) 
				{
					mapParent = (Map) itrParent.next();
					if(mapParent != null) 
					{
						strParentid = (String)mapParent.get(DomainConstants.SELECT_ID);
				    	domParentObject.setId(strParentid);
					
			     		// if in the context of PS, PC or PT then restamp
			     		if(domParentObject.isKindOf(context, DomainConstants.TYPE_PROJECT_SPACE) || domParentObject.isKindOf(context, DomainConstants.TYPE_PROJECT_TEMPLATE)
							|| domParentObject.isKindOf(context, DomainConstants.TYPE_PROJECT_CONCEPT)) 
			    		{
                            // give the owner of the Message full access before restamping it
				    		DomainAccess.createObjectOwnership(context, toId, personId, "Full", DomainAccess.COMMENT_MULTIPLE_OWNERSHIP);
                    	    toObj.setPrimaryOwnership(context, strFromProject,strFromOrganization);
                    	    break;
                        }
					}
                }
            }
            else
            {
                toObj.setPrimaryOwnership(context, strFromProject,strFromOrganization);
            }
         }

         return true;
      }catch(Exception e){
         throw e;
      }
      finally
      {
          PropertyUtil.setRPEValue(context, "MX_ALLOW_POV_STAMPING", "false", true); // clear out the RPE variable
      }

   }
   /**
    * Remove inherited access for any object when the relationship is being deleted.
    * Also removes ownership is there exist.
    * @param context the eMatrix <code>Context</code> object
    * @param args - contains trigger parameters.
    * @return 
    * @throws Exception 
    */
   public static boolean clearInheritedAccess(Context context, String[] args) throws Exception {

	   String fromId = args[0];  // id of from object
	   String toId = args[1];  // id of to object
	   String onlyIncludeToType = args[2]; // only remove access to this Type on the To side of the relationship
	   String accessBits = args[3];  // access bits we want to remove from the parent

	   String command = DomainConstants.EMPTY_STRING;
	   boolean contextPushed = false;
	   try{
		   boolean oKToRemoveAccess = false;
		   DomainObject toObj = DomainObject.newInstance(context, toId);

		   // Get information about the From side object
		   StringList strFromList = new StringList();
		   strFromList.add(ProgramCentralConstants.SELECT_KINDOF_PROJECT_SPACE);
		   strFromList.add(ProgramCentralConstants.SELECT_KINDOF_PROJECT_CONCEPT);
		   strFromList.add(ProgramCentralConstants.SELECT_KINDOF_TASKMANAGEMENT);

		   DomainObject fromObj = DomainObject.newInstance(context,fromId);
		   Map mpObjectInfo = fromObj.getInfo(context,strFromList);
		   String bisFromProjectSpaceType = (String)mpObjectInfo.get(ProgramCentralConstants.SELECT_KINDOF_PROJECT_SPACE);
		   String bisFromProjectConceptType = (String)mpObjectInfo.get(ProgramCentralConstants.SELECT_KINDOF_PROJECT_CONCEPT);
		   String bisFromTaskManagementType = (String)mpObjectInfo.get(ProgramCentralConstants.SELECT_KINDOF_TASKMANAGEMENT);

		   // if in the context of PS, PC or PT
		   if (bisFromProjectSpaceType.equalsIgnoreCase("true") || bisFromProjectConceptType.equalsIgnoreCase("true") || bisFromTaskManagementType.equalsIgnoreCase("true"))
		   {
			   oKToRemoveAccess = true;
		   }
		   if (oKToRemoveAccess) {

			   if (onlyIncludeToType != null && (! "".equalsIgnoreCase(onlyIncludeToType))){

				   String typeToInclude = PropertyUtil.getSchemaProperty(context, onlyIncludeToType);

				   //Check to see if it is kind of type_DOCUMENTS
				   if(toObj.isKindOf(context, typeToInclude)){
					   
					   String comment = DomainAccess.COMMENT_MULTIPLE_OWNERSHIP;
					   if("TRUE".equalsIgnoreCase(bisFromTaskManagementType)){
						   //while deleting ownership, need to pass same comment as used in creating ownership
						   comment = "Adding ownership for TaskDeliverable relationship";
					   }
					   
					   //delete Object Ownership if exist.
					   DomainAccess.deleteObjectOwnership(context, toId, fromId, comment);

					   //check if there is inherited access is present or not...
					   String sResult = MqlUtil.mqlCommand(context,"print bus $1 select $2 dump",toId,"access["+fromId+"|Inherited Access]");

					   if("TRUE".equalsIgnoreCase(sResult)){
						   command = "modify bus " + toId + " remove access bus " + fromId + " for 'Inherited Access' as " +  accessBits;
					   }else{
						   return true;
					   }
				   }
			   }
			   else if(toObj.isKindOf(context,ProgramCentralConstants.TYPE_ISSUE)){   //Need to remove ownership from Issue..
					   DomainAccess.deleteObjectOwnership(context, toId, fromId, "");
				   }
		
			   if(!command.isEmpty())
				   MqlUtil.mqlCommand(context, command);
		   }

		   return true;
	   }catch(Exception e){
		   throw e;
	   }
   }
}

