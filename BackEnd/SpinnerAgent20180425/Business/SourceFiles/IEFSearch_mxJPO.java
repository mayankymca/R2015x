//IEFSearch.java

//This program handles the MSOffice integration search specific commands that need to be handled
//in an office integration environment.

//Copyright (c) 2002 MatrixOne, Inc.
//All Rights Reserved
//This program contains proprietary and trade secret information of
//MatrixOne, Inc.  Copyright notice is precautionary only and does
//not evidence any actual or intended publication of such program.
/**
 * @quickReview 17:04:14 ACE2 TSK3438445: ENOVIA_BAA_MSF_2018x_Backport Microsoft Project Integration from 2017x to 2015x
 */

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessType;
import matrix.db.BusinessTypeList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.Query;
import matrix.db.Vault;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFCache;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNodeImpl;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.OrganizationUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UISearch;

/**
 * The <code>IEFSearch</code> class represents the JPO for
 * obtaining the search data for MS Office integration
 *
 * @version AEF 10.5.1.0 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class IEFSearch_mxJPO
{
    private boolean isNewQuery                                  = false;
    private Locale locale                                       = Locale.getDefault();
    private double timeZone                                     = 0.0;
    private String defaultDateFormat                            = "MEDIUM";
    private String showTime                                     = "false";
    private HashMap dateFormatMap                               = new HashMap();
	private HashMap _GCOTable									= new HashMap();
    private MCADGlobalConfigObject _GlobalConf                  = null;
    private MCADLocalConfigObject _LocalConf                    = null;
    private IEFCache  _GlobalCache                              = new IEFGlobalCache();;
    private MCADMxUtil _util                                    = null;
    private HashSet notAllowedAttributes                        = new HashSet();
    private String sDefaultQueryLimit                           = "100";
    private final String iefClientFinderQueryStr                = ".msoiFinder";
    private final String msoiHiddenQuery                        = ".msoiHiddenQuery";
    private String integratioName                               = "MSOffice"; //Default Value
    private String _source                                      = "";
    private String[] initArgs                                   = null;
    private String datetimepattern  							= null;
    /**
     * Constructs a new IEFSearch JPO object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args an array of String arguments for this method
     * @throws Exception if the operation fails
     * @since AEF 10.0.0.0
     */
	public IEFSearch_mxJPO (Context context)
    {
        init(context);
    }


    /**
     * Constructs a new IEFSearch JPO object.
     *
     * @param context the eMatrix <code>Context</code> object
     * @param args an array of String arguments for this method
     * @throws Exception if the operation fails
     * @since AEF 10.0.0.0
     */
	public IEFSearch_mxJPO (Context context, String[] args) throws Exception
    {
        try
        {
            if(args != null && args.length > 0)
            {
                initArgs = args;
                Hashtable initArgsTable = (Hashtable)JPO.unpackArgs(args);

                if(initArgsTable != null)
                {
                    if(initArgsTable.get("locale") != null)
                        locale = (Locale)initArgsTable.get("locale");

                    String time_Zone = (String)initArgsTable.get("timezone");
                    if(time_Zone != null)
                        timeZone = (Double.valueOf(time_Zone)).doubleValue();

                    _GCOTable = (HashMap)initArgsTable.get("gcoTable");
                    _LocalConf = (MCADLocalConfigObject )initArgsTable.get("lco");
                    integratioName = (String )initArgsTable.get("integrationName");
                    if(integratioName == null || "".equals(integratioName))
                        integratioName = "MSOffice";
					
					_GlobalConf = (MCADGlobalConfigObject)_GCOTable.get(integratioName);
                    
					_source = (String)initArgsTable.get("source");
                }
            }

			init(context);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.out.println("[IEFSearch.constructor] EXCEPTION : " + ex.getMessage());
        }
    }

	private void init(Context ctx) 
    {
        //Remove the attributes whcih are not required in CDM implementation
    	try
        {
    		notAllowedAttributes = (HashSet)JPO.invoke(ctx, "IEFUtil", null, "getAttributeSetToFilter", null, HashSet.class);
    		sDefaultQueryLimit = FrameworkProperties.getProperty("emxFramework.Search.QueryLimit");
            if( (sDefaultQueryLimit == null) || ("null".equals(sDefaultQueryLimit)) || ("".equals(sDefaultQueryLimit))) {
                sDefaultQueryLimit = "100";
            }

            defaultDateFormat = FrameworkProperties.getProperty("emxFramework.DateTime.DisplayFormat");
            if(defaultDateFormat == null || "null".equalsIgnoreCase(defaultDateFormat) || "".equals(defaultDateFormat))
                defaultDateFormat = "MEDIUM";

            showTime = FrameworkProperties.getProperty("emxFramework.DateTime.DisplayTime");
            if(showTime == null || "null".equalsIgnoreCase(showTime) || "".equals(showTime))
                showTime = "false";

            dateFormatMap.put("SHORT","3");
            dateFormatMap.put("MEDIUM","2");
            dateFormatMap.put("LONG","1");
            dateFormatMap.put("FULL","0");
        }
        catch(Exception ex)
        {
            System.out.println("[IEFSearch.init] EXCEPTION : " + ex.getMessage());
        }

        if(ctx != null)
            _util = new MCADMxUtil(ctx, null,_GlobalCache);
    }

    /**
     * getSearchResults
     *
     * @param query criteria
     * @throws Exception if the operation fails
     */
    public String getSearchResults(Context context, String[] args) throws MatrixException
    {
        String xmlOutput = "";
        isNewQuery = true;
        try
        {
            //Read the parameters. Last parameter is false because the query name is NOT available in this request
            HashMap paramMap = readParameters(context, args, false);

            boolean retVal = saveQueryInMx(context, paramMap);

            if(retVal)
            {
                String txtQueryName = (String)paramMap.get("queryname");
                String[] queryArgs = new String[4];
                queryArgs[0] = (String)paramMap.get("language");
                queryArgs[1] = "command_MsoiEvaluateSavedQueries";
                queryArgs[2] = "";
                queryArgs[3] = txtQueryName;

                //Update the .msoiHiddenFinder query
                updateQuery(context, paramMap);

               //Save the query as iefClientFinderQuery
                paramMap.put("queryname", iefClientFinderQueryStr);
                retVal = saveQueryInMx(context, paramMap);

                //Update the query and set the description as an encoded xml string. This will be used during the query criteria retrieval
                if(retVal)
                    updateQuery(context, paramMap);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw new MatrixException(e.getMessage());
        }

        return xmlOutput;
    }

    /*
     * refreshSearchResults -- This is called when the 'General Search' folder is double clicked
     *
     * @throws Exception if the operation fails
     */
    public MapList refreshSearchResults(Context context, String[] args)
    {
        MapList maplist = new MapList();
        try
        {
            if(doesQueryExist(context,iefClientFinderQueryStr))
            {
                StringList busSelects = new StringList(1);
                busSelects.add(DomainConstants.SELECT_ID);
                busSelects.add(DomainConstants.SELECT_TYPE);
                busSelects.add(DomainConstants.SELECT_REVISION);
                busSelects.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
                busSelects.add(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
                busSelects.add(DomainConstants.SELECT_FILE_NAME);
                busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
                busSelects.add(CommonDocument.SELECT_TITLE);
                busSelects.add(DomainConstants.SELECT_NAME);
                busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION);
                busSelects.add(CommonDocument.SELECT_HAS_ROUTE);
                busSelects.add(CommonDocument.SELECT_SUSPEND_VERSIONING);
                busSelects.add(CommonDocument.SELECT_HAS_CHECKOUT_ACCESS);
                busSelects.add(CommonDocument.SELECT_HAS_CHECKIN_ACCESS);

                int querylimit = getQueryLimit(context, iefClientFinderQueryStr);
                Query iefClientFinderQuery = new Query(iefClientFinderQueryStr);
                iefClientFinderQuery.open(context);
                if(querylimit > 0)
                    iefClientFinderQuery.setObjectLimit(Short.parseShort("" + querylimit));
                maplist = FrameworkUtil.toMapList(iefClientFinderQuery.select(context, busSelects));
                iefClientFinderQuery.close(context);
            }
        }
        catch(Exception e)
        {
            System.out.println("[IEFSearch.refreshGeneralSearch] EXCEPTION : " + e.getMessage());
        }
        return maplist;
    }

    /**
     * Get the query criteria
     * Generates XML string representing the query criteria.
     *
     * @param 'query name' String value
     * @throws Exception if the operation fails
     */
    public String getQueryDetails(Context context, String[] args) throws MatrixException
    {
        String xmlOutput = "";
        String queryName = null;
        String lang = "en";
        try
        {
            if(args != null && args.length != 0)
            {
                lang = args[0];
                queryName = args[1];

				if(queryName != null && queryName.length() != 0)
                {
                    ContextUtil.startTransaction(context, false);
                    String searchData = UISearch.getSearchData(context, queryName);
                    if(searchData != null)
                    {
                        xmlOutput = decodeURL(searchData, "UTF-8");
                        if(xmlOutput != null && xmlOutput.length() > 0)
                        {
							IEFXmlNodeImpl xmlPacket = getCommandPacket(context, xmlOutput);
                            xmlOutput = checkForNewAttributesToAdd(context, xmlPacket, queryName, lang);
                        }
                    }
                    else
                    {
                        xmlOutput = getDefaultQueryDetails(context, lang);
                    }
                    ContextUtil.commitTransaction(context);
                }
                else
                {
                    xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.QueryNameIsEmpty",  "iefStringResource", lang);
                    throw new MatrixException(xmlOutput);
                }
            }
        }
        catch(Exception e)
        {
            ContextUtil.abortTransaction(context);
            if(iefClientFinderQueryStr.equalsIgnoreCase(queryName))
            {
                xmlOutput = getDefaultQueryDetails(context, lang);
            }
            else
            {
                throw new MatrixException(e.getMessage());
            }
        }

        return xmlOutput;
    }

    /**
     * Update existing query
     *
     * @param 'query name' String value
     * @param query criteria
     * @throws Exception if the operation fails
     */
    public String saveQuery(Context context, String[] args) throws MatrixException
    {
        String xmlOutput = "";
        isNewQuery = false;
        try
        {
            saveQueryInMatrix(context, args);

            if(isNewQuery)
                xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.QuerySavedSuccessfully",  "iefStringResource", args[0]);
            else
                xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.QueryUpdatedSuccessfully",  "iefStringResource", args[0]);
        }
        catch(Exception e)
        {
            throw new MatrixException(e.getMessage());
        }

        return xmlOutput;
    }

    /**
     * Save as new query
     *
     * @param 'query name' String value
     * @param query criteria
     * @throws Exception if the operation fails
     */
    public String saveNewQuery(Context context, String[] args) throws MatrixException
    {
        String xmlOutput = "";
        isNewQuery = true;
        try
        {
            saveQueryInMatrix(context, args);

            if(isNewQuery)
                xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.QuerySavedSuccessfully",  "iefStringResource", args[0]);
            else
                xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.QueryUpdatedSuccessfully",  "iefStringResource", args[0]);
        }
        catch(Exception e)
        {
            throw new MatrixException(e.getMessage());
        }

        return xmlOutput;
    }

    /**
     * saveDotFinderAs
     *
     * @param 'query name' String value
     * @param query criteria
     * @throws Exception if the operation fails
     */
    public String saveDotFinderAs(Context context, String[] args) throws MatrixException
    {
        String xmlOutput = "";
        try
        {
            String languageStr = "en";
            String sNewQueryName = "";
            if(args != null)
            {
                languageStr = args[0];
                if(languageStr == null) languageStr = "en";
                sNewQueryName = args[1];
            }
            if(sNewQueryName == null || "".equals(sNewQueryName))
            {
                throw new Exception(i18nNow.getI18nString("mcadIntegration.Server.Message.QueryNameIsEmpty",  "iefStringResource", languageStr));
            }
            else
            {
                sNewQueryName = sNewQueryName.trim();
                ContextUtil.startTransaction(context, true);
                MQLCommand mqlcommand = new MQLCommand();
                mqlcommand.executeCommand(context, "copy query $1 $2 $3", iefClientFinderQueryStr,sNewQueryName, "overwrite");
                ContextUtil.commitTransaction(context);
                xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.QuerySavedSuccessfully",  "iefStringResource", languageStr);
            }
        }
        catch(Exception e)
        {
            throw new MatrixException(e.getMessage());
        }

        return xmlOutput;
    }
    /**
     * Check whether the query exists in Matrix
     *
     * @param 'query name' String value
     * return boolean
     */
    public boolean doesQueryExist(Context context, String txtQueryName)
    {
        boolean doesQueryExist = false;
        try
        {
            ContextUtil.startTransaction(context, false);
            MQLCommand mqlcommand = new MQLCommand();
            mqlcommand.executeCommand(context, "list query $1", txtQueryName);
            String result = mqlcommand.getResult();
            if(result != null && result.length() != 0)
            {
                if(txtQueryName.equals(result.trim()))
                    doesQueryExist = true;
            }
            ContextUtil.commitTransaction(context);
        }
        catch(Exception e)
        {
            doesQueryExist = false;
            ContextUtil.abortTransaction(context);
        }
        return doesQueryExist;
    }

    /**
     * Delete the saved query
     *
     * @param 'query name' String value
     * @throws Exception if the operation fails
     */
    public String deleteQuery(Context context, String[] args) throws MatrixException
    {
        String xmlOutput = "";
        String lang = args[0];
        try
        {
            //Read the parameters
            String txtQueryName = args[1];
            //Delete the query
			if(txtQueryName != null && txtQueryName.length() != 0)
            {
                ContextUtil.startTransaction(context, true);
                UISearch.deleteSearch(context, txtQueryName);
                ContextUtil.commitTransaction(context);
                String[] listQueriesArgs = new String[5];
                listQueriesArgs[0] = lang;
                listQueriesArgs[1] = "command_MsoiSavedQueries";
                listQueriesArgs[2] = "Command";
                listQueriesArgs[3] = "Command";
                listQueriesArgs[4] = "Command";

                //${CLASS:IEFClientBuildFolderStructure} folderStructure =  new ${CLASS:IEFBuildFolderStructure}(context, initArgs);

                xmlOutput = (String) JPO.invoke(context, "IEFBuildFolderStructure", initArgs, "evaluateCommand", listQueriesArgs, String.class);
                //xmlOutput = folderStructure.evaluateCommand(context, listQueriesArgs);
            }
            else
            {
                xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.UnableToDeleteQuery",  "iefStringResource", lang) + " " + i18nNow.getI18nString("mcadIntegration.Server.Message.UnableToDeleteQuery",  "iefStringResource", lang);
                throw new MatrixException(xmlOutput);
            }
        }
        catch (Exception ex)
        {
            ContextUtil.abortTransaction(context);
            throw new MatrixException(xmlOutput);
        }

        return xmlOutput;

    }

    /**
     * Save query for Matrix
     *
     * @param query criteria
     * @throws Exception if the operation fails
     */
    private void saveQueryInMatrix(Context context, String[] args) throws MatrixException
    {
        try
        {
            //Read the parameters. Last parameter is false because the query name is available in this request
            HashMap paramMap = readParameters(context, args, true);

            boolean retVal = saveQueryInMx(context, paramMap);

            //Update the query and set the description as an encoded xml string. This will be used during the query criteria retrieval
            if(retVal)
                updateQuery(context, paramMap);
        }
        catch(Exception e)
        {
            throw new MatrixException(e.getMessage());
        }
    }


    /* To get the advanced search where expression
     *
     */
    public String getAdvanceSearchWhereExpression(Context context, String sTypeName, HashMap hashmap, String dateCreatedAfter, String dateCreatedBefore, String latestRevOnly, String strTitle, String lang) throws FrameworkException
    {
        String returnString = "";
        Locale localeLocal=new Locale("en");    
        StringBuffer stringbuffer = new StringBuffer(256);
        boolean bThrowException = false;
        try
        {
            StringList stringlist = new StringList();
            String sAttrList = (String)hashmap.get("attrList");
            if(sAttrList != null && !"".equals(sAttrList))
            {
                if(sAttrList.endsWith(","))
                    sAttrList = sAttrList.substring(0, sAttrList.length() - 1);
                stringlist = FrameworkUtil.split(sAttrList, ",");
            }

            String sType = "Type";
            String sName = "Name";
            String sRevision = "Revision";
            String sOwner = "Owner";
            String sVault = "Vault";
            String sDescription = "Description";
            String sCurrent = "Current";
            String sModified = "Modified";
            String sOriginated = "Originated";
            String sGrantor = "Grantor";
            String sGrantee = "Grantee";
            String sPolicy = "Policy";
            String sOr = " || ";
            String sAnd = " && ";
            String sEqualEqualConst = " == const";
            String sNotEqualConst = " != const";
            String sGreaterThanConst = " > const";
            String sLessThanConst = " < const";
            String sGreaterThanOrEqualConst = " >= const";
            String sLessThanOrEqualConst = " <= const";
            String sTildaEqualConst = " ~= const";
            String sBackSlash = "\"";
            String sAsterisk  = "*";
            String sOpenParenthesize = "(";
            String sCloseParenthesize = ")";
            String sAttribute = "attribute";
            String sOpenSquareBracket = "[";
            String sCloseSquareBracket = "]";
            String sIncludes = "Includes";
            String sIsExactly = "Is Exactly";
            String sIsNot = "Is not";
            String sMatches = "Matches";
            String sBeginsWith = "Begins With";
            String sEndsWith = "Ends With";
            String sEquals = "Equals";
            String sDoesNotEqual = "Does Not Equal";
            String sIsBetween = "Is Between";
            String sIsAtMost = "Is At Most";
            String sIsAtLeast = "Is At Least";
            String sIsMoreThan = "Is More Than";
            String sIsLessThan = "Is Less Than";
            String sIsOn = "Is On";
            String sIsOnOrBefore = "Is On Or Before";
            String sIsOnOrAfter = "Is On Or After";
            String sAndOrField = (String)hashmap.get("andOrField");
            String receivedDateFormat = (String)hashmap.get("defaultdateformat");

            if(stringlist.size() == 0)
                sAndOrField = sAnd;

            if("and".equals(sAndOrField))
                sAndOrField = sAnd;
            else
                sAndOrField = sOr;

            if(hashmap.size() < 3)
                sAndOrField = sAnd;

            if(receivedDateFormat != null)
                defaultDateFormat = receivedDateFormat;

            Integer IntDateFormat = new Integer((String)dateFormatMap.get(defaultDateFormat));
            int iDateFormat = 2;

            if(IntDateFormat != null)
                iDateFormat = IntDateFormat.intValue();

            try
            {
             
            	java.text.DateFormat dateformat = java.text.DateFormat.getDateInstance(iDateFormat, localeLocal);
                java.util.Date dtCreatedAfter = null;
                java.util.Date dtCreatedBefore = null;
                if(!"".equals(dateCreatedAfter))
                    dtCreatedAfter = dateformat.parse(dateCreatedAfter);

                if(!"".equals(dateCreatedBefore))
                    dtCreatedBefore = dateformat.parse(dateCreatedBefore);

                if(dtCreatedAfter != null && dtCreatedBefore != null)
                {
                    int a = dtCreatedAfter.compareTo(dtCreatedBefore);
                    if(a > 0)
                    {
                        bThrowException = true;
                        String msg = i18nNow.getI18nString("mcadIntegration.Server.Message.AfterDateGreaterThanBeforeDate",  "iefStringResource", lang);
                        throw new FrameworkException(msg);
                    }
                }
            }
            catch(Exception emx)
            {
                if(bThrowException)
                    throw new FrameworkException(emx);
            }

            if(latestRevOnly != null && "true".equalsIgnoreCase(latestRevOnly))
            {
                stringbuffer.append(sOpenParenthesize);
                stringbuffer.append("revision");
                stringbuffer.append(" == ");
                stringbuffer.append("last");
                stringbuffer.append(sCloseParenthesize);
            }

            for(int i = 0; i < stringlist.size(); i++)
            {
                try
                {
                    String sString3 = "";
                    String sString4 = "";
                    String sString5 = "";
                    String sString6 = (String)stringlist.elementAt(i);
                    Pattern pattern1 = new Pattern(sString6);
                    Pattern pattern3 = new Pattern((String)hashmap.get("comboDescriptor_" + sString6));
                    sString3 = (String)hashmap.get("txt_" + sString6);
                    sString4 = (String)hashmap.get(sString6);

                    if(sString3 == null || sString3.equals("") || sString3.equals("*"))
                    {
                        if(sString4 != null)
                            sString5 = sString4;
                    } else
                    {
                        sString5 = sString3.trim();
                    }

                    boolean canAppend = checkForBooleanValue(context, sString6, sString5);
                    if(!canAppend)
                        continue;
                    StringBuffer stringbuffer1 = new StringBuffer(256);
                    if(!sString5.equals("") && !sString5.equals("*"))
                    {
                        if(pattern1.match(sType) || pattern1.match(sName) || pattern1.match(sRevision) || pattern1.match(sOwner) || pattern1.match(sVault) || pattern1.match(sDescription) || pattern1.match(sCurrent) || pattern1.match(sModified) || pattern1.match(sOriginated) || pattern1.match(sGrantor) || pattern1.match(sGrantee) || pattern1.match(sPolicy) || sString6.equals("State"))
                        {
                            stringbuffer1.append(sString6);
                        } else
                        {
                            stringbuffer1.append(sAttribute);
                            stringbuffer1.append(sOpenSquareBracket);
                            stringbuffer1.append(sString6);
                            stringbuffer1.append(sCloseSquareBracket);
                        }
                        if(pattern3.match(sIncludes))
                        {
                            stringbuffer1.append(sTildaEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sAsterisk );
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sAsterisk );
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sIsExactly))
                        {
                            stringbuffer1.append(sEqualEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sIsNot))
                        {
                            stringbuffer1.append(sNotEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sMatches))
                        {
                            stringbuffer1.append(sTildaEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sBeginsWith))
                        {
                            stringbuffer1.append(sTildaEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sAsterisk );
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sEndsWith))
                        {
                            stringbuffer1.append(sTildaEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sAsterisk );
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sEquals))
                        {
                            stringbuffer1.append(sEqualEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sDoesNotEqual))
                        {
                            stringbuffer1.append(sNotEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sIsBetween))
                        {
                            sString5 = sString5.trim();
                            int j = sString5.indexOf(" ");
                            String sString7 = "";
                            String sString8 = "";
                            if(j == -1)
                            {
                                sString7 = sString5;
                                sString8 = sString5;
                            } else
                            {
                                sString7 = sString5.substring(0, j);
                                sString8 = sString5.substring(sString7.length() + 1, sString5.length());
                                j = sString8.indexOf(" ");
                                if(j != -1)
                                    sString8 = sString8.substring(0, j);
                            }
                            stringbuffer1.append(sGreaterThanOrEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString7);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sCloseParenthesize);
                            stringbuffer1.append(sAnd);
                            stringbuffer1.append(sOpenParenthesize);
                            if(pattern1.match(sDescription) || pattern1.match(sCurrent) || pattern1.match(sModified) || pattern1.match(sOriginated) || pattern1.match(sGrantor) || pattern1.match(sGrantee) || pattern1.match(sPolicy))
                            {
                                stringbuffer1.append(sString6);
                            } else
                            {
                                stringbuffer1.append(sAttribute);
                                stringbuffer1.append(sOpenSquareBracket);
                                stringbuffer1.append(sString6);
                                stringbuffer1.append(sCloseSquareBracket);
                            }
                            stringbuffer1.append(sLessThanOrEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString8);
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sIsAtMost))
                        {
                            stringbuffer1.append(sLessThanOrEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sIsAtLeast))
                        {
                            stringbuffer1.append(sGreaterThanOrEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sIsMoreThan))
                        {
                            stringbuffer1.append(sGreaterThanConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sIsLessThan))
                        {
                            stringbuffer1.append(sLessThanConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sIsOn))
                        {
                            String s52 = eMatrixDateFormat.getFormattedInputDateTime(context, sString5, "12:00:00 AM", iDateFormat, timeZone, localeLocal);
                            String s54 = eMatrixDateFormat.getFormattedInputDateTime(context, sString5, "11:59:59 PM", iDateFormat, timeZone, localeLocal);
                            stringbuffer1.append(sLessThanOrEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(s54);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sAnd + " ");
                            if(pattern1.match(sType) || pattern1.match(sName) || pattern1.match(sRevision) || pattern1.match(sOwner) || pattern1.match(sVault) || pattern1.match(sDescription) || pattern1.match(sCurrent) || pattern1.match(sModified) || pattern1.match(sOriginated) || pattern1.match(sGrantor) || pattern1.match(sGrantee) || pattern1.match(sPolicy) || sString6.equals("State"))
                            {
                                stringbuffer1.append(sString6);
                            } else
                            {
                                stringbuffer1.append(sAttribute);
                                stringbuffer1.append(sOpenSquareBracket);
                                stringbuffer1.append(sString6);
                                stringbuffer1.append(sCloseSquareBracket);
                            }
                            stringbuffer1.append(sGreaterThanOrEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(s52);
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sIsOnOrBefore))
                        {
                            sString5 = eMatrixDateFormat.getFormattedInputDateTime(context, sString5, "11:59:59 PM", iDateFormat, timeZone, localeLocal);
                            stringbuffer1.append(sLessThanOrEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sBackSlash);
                        } else if(pattern3.match(sIsOnOrAfter))
                        {
                            sString5 = eMatrixDateFormat.getFormattedInputDateTime(context, sString5, "12:00:00 AM", iDateFormat, timeZone, localeLocal);
                            stringbuffer1.append(sGreaterThanOrEqualConst);
                            stringbuffer1.append(sBackSlash);
                            stringbuffer1.append(sString5);
                            stringbuffer1.append(sBackSlash);
                        }

                        if((stringbuffer1.toString()).length() > 0)
                        {
                            if(stringbuffer.length() > 0)
                                stringbuffer.append(sAndOrField);

                            stringbuffer.append(sOpenParenthesize);
                            stringbuffer.append(stringbuffer1.toString());
                            stringbuffer.append(sCloseParenthesize);
                        }
                    }
                }
                catch(Exception exp)
                {
                    MCADServerException.createException(exp.getMessage(), exp);
                }
            }

            //Following code is added for the search criteria 'Created After' and 'Created Before' fields on the MSOI general search dialog
            boolean isCreateAfterDateSet = false;
            boolean isCreateBeforeDateSet = false;
            String dateString = "";
            StringBuffer dateCreatedBeforeAfter = new StringBuffer();
            try
            {
                if(dateCreatedAfter != null && dateCreatedAfter.length() != 0)
                {
                	if(datetimepattern == null)
                	{
                    dateString = eMatrixDateFormat.getFormattedInputDateTime(context, dateCreatedAfter, "12:00:00 AM", iDateFormat, timeZone, localeLocal);
                	}
                	else
                	{
                		dateString = dateCreatedAfter;
                	}
                    dateCreatedAfter = "originated " + sGreaterThanConst + sBackSlash + dateString + sBackSlash;
                    isCreateAfterDateSet = true;
                    dateString = "";
                }
            }
            catch(Exception dateExp1)
            {
                MCADServerException.createException(dateExp1.getMessage(), dateExp1);
            }
            try
            {
                if(dateCreatedBefore != null && dateCreatedBefore.length() != 0)
                {
                	if(datetimepattern == null)
                	{
                    dateString = eMatrixDateFormat.getFormattedInputDateTime(context, dateCreatedBefore, "12:00:00 AM", iDateFormat, timeZone, localeLocal);
                	}
                	else
                	{
                		dateString = dateCreatedBefore;
                	}
                    dateCreatedBefore = "originated " + sLessThanConst + sBackSlash + dateString + sBackSlash;
                    isCreateBeforeDateSet = true;
                    dateString = "";
                }
            }
            catch(Exception dateExp2)
            {
                MCADServerException.createException(dateExp2.getMessage(), dateExp2);
            }

            if(isCreateAfterDateSet)
                dateCreatedBeforeAfter.append(dateCreatedAfter);
            if(isCreateBeforeDateSet && dateCreatedBeforeAfter.length() > 0)
            {
                dateCreatedBeforeAfter.append(sAnd);
                dateCreatedBeforeAfter.append(dateCreatedBefore);
            }
            else if(isCreateBeforeDateSet && dateCreatedBeforeAfter.length() == 0)
                dateCreatedBeforeAfter.append(dateCreatedBefore);

            if(dateCreatedBeforeAfter.length() > 0)
            {
                dateCreatedBeforeAfter.insert(0, " " + sOpenParenthesize);
                dateCreatedBeforeAfter.append(sCloseParenthesize);

                if((stringbuffer.toString()).length() > 0)
                {
                    dateCreatedBeforeAfter.insert(0, " "+sAndOrField+" ");
                }
                stringbuffer.append(dateCreatedBeforeAfter);
            }

            
            boolean isEscapingRequired = MCADUtil.isEscapingRequiredForMQL(strTitle);
            if(isEscapingRequired)
            	strTitle   = MCADUtil.escapeStringForMQL(strTitle);
            
            //Above code is added for the search criteria 'Created After' and 'Created Before' fields on the MSOI general search dialog
            if(!"".equals(stringbuffer.toString()))
            {
                if(strTitle != null && !("*".equals(strTitle)) && strTitle.length() != 0)
                {
					stringbuffer.append(sAndOrField + sOpenParenthesize + sBackSlash + "attribute" + sOpenSquareBracket 
							+ MCADMxUtil.getActualNameForAEFData(context, "attribute_Title") + sCloseSquareBracket 
							+ sBackSlash + sTildaEqualConst + sBackSlash + strTitle + sBackSlash + sCloseParenthesize);
                }
            }
            else
            {
                if(strTitle != null && !("*".equals(strTitle)) && strTitle.length() != 0)
                {
					stringbuffer.append(sOpenParenthesize + sBackSlash + "attribute" + sOpenSquareBracket 
							+ MCADMxUtil.getActualNameForAEFData(context, "attribute_Title") + sCloseSquareBracket 
							+ sBackSlash + sTildaEqualConst + sBackSlash + strTitle + sBackSlash + sCloseParenthesize);
                }
            }

            String stypeDOCUMENTS = CommonDocument.TYPE_DOCUMENTS;  //[SUPPORT]
            boolean isDocumentsType = false;
            
            Hashtable argumentsTable = new Hashtable(2);
            argumentsTable.put("Type",sTypeName);
            argumentsTable.put("RootType",stypeDOCUMENTS);
            Boolean isTypeOf = (Boolean)JPO.invoke(context, "IEFUtil", null, "isTypeOf", JPO.packArgs(argumentsTable), Boolean.class);
            isDocumentsType  = isTypeOf.booleanValue();
            
            if(!"".equals(stringbuffer.toString()))
            {
				if(isDocumentsType && iefAppendIsVersionObject(context))
                {
                    stringbuffer.insert(0, sOpenParenthesize);
                    stringbuffer.append(sCloseParenthesize);
					stringbuffer.append(sAnd + sOpenParenthesize + sBackSlash + "attribute" + sOpenSquareBracket 
							+ MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject") + sCloseSquareBracket 
							+ sBackSlash + sEqualEqualConst + sBackSlash + "False" + sBackSlash + sCloseParenthesize);
                }
            }
            else
            {
				if(isDocumentsType && iefAppendIsVersionObject(context))
                {
					stringbuffer.append(sOpenParenthesize + sBackSlash + "attribute" + sOpenSquareBracket 
							+ MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject") + sCloseSquareBracket 
							+ sBackSlash + sEqualEqualConst + sBackSlash + "False" + sBackSlash + sCloseParenthesize);
                }
            }

            if(isEscapingRequired && stringbuffer.toString().trim().length() != 0)
            	stringbuffer.insert(0, "escape ");
            
            returnString = stringbuffer.toString();
        }
        catch(Exception e)
        {
            try
            {
	        	MCADServerException.createException(e.getMessage(), e);
            }
	        catch(Exception exc){}
            returnString = "";
            if(bThrowException)
                throw new FrameworkException(e);
        }

        return returnString;
    }
    
	private boolean iefAppendIsVersionObject(Context context) throws MatrixException
    {
		Boolean isVersionObject = (Boolean) JPO.invoke(context, "IEFCDMSupport", null, "iefAppendIsVersionObject", null, Boolean.class); 
    	return isVersionObject.booleanValue();
    }
    
	private IEFXmlNodeImpl getCommandPacket(Context context, String commandString) throws Exception
    {
        IEFXmlNodeImpl xmlNode = null;

        try
        {
        	Hashtable argumentsTable  = new Hashtable(1);
        	argumentsTable.put("commandString", commandString);
			IEFXmlNode commandPacket = (IEFXmlNode)JPO.invoke(context, "IEFUtil", null, "getCommandPacket", JPO.packArgs(argumentsTable), IEFXmlNode.class);
            
            if(commandPacket != null)
            {
                xmlNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
                IEFXmlNodeImpl AttributeListNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
                AttributeListNode.addNode((IEFXmlNodeImpl)commandPacket);
                xmlNode.addNode(AttributeListNode);
            }
        }
        catch(Exception e)
        {
            MCADServerException.createException(e.getMessage(), e);
            xmlNode = null;
        }

        return xmlNode;
    }

    private HashMap createAttributesMap(IEFXmlNode xmlContent)
    {
        HashMap attrMap = new HashMap();
        StringBuffer sAttrList = new StringBuffer();
        IEFXmlNodeImpl xmlNode = (IEFXmlNodeImpl)xmlContent;

        try
        {
            if(xmlNode != null)
            {
                Enumeration nodeEnum = xmlNode.elements();
                while(nodeEnum.hasMoreElements())
                {
                    IEFXmlNodeImpl rootNode = (IEFXmlNodeImpl)nodeEnum.nextElement();
                    if(rootNode != null)
                    {
                        IEFXmlNodeImpl attributeListNode = rootNode.getChildByName("attributelist");
                        if(attributeListNode != null)
                        {
                            Enumeration childElements = attributeListNode.elements();
                            while(childElements.hasMoreElements())
                            {
                                IEFXmlNode attributeNode = (IEFXmlNode)childElements.nextElement();
                                if(attributeNode != null)
                                {
                                    String nodeName = attributeNode.getName();
                                    if("attribute".equals(nodeName))
                                    {
                                        String name = attributeNode.getAttribute("name");
                                        String operatorValue = attributeNode.getAttribute("seloperatorvalue");
                                        String enteredValue = attributeNode.getAttribute("enteredvalue");
                                        String selectedValue = attributeNode.getAttribute("selselectedvalue");

                                        if(name != null)
                                            name = name.trim();

                                        if(operatorValue == null || operatorValue.length() == 0)
                                            operatorValue = "*";
                                        else
                                        {
                                            int i = operatorValue.indexOf(";");
                                            if(i > 0)
                                            {
                                                operatorValue = operatorValue.substring(0, i);
                                            }
                                        }

                                        if(enteredValue == null || (enteredValue.trim()).length() == 0)
                                            enteredValue = "*";

                                        if("*".equals(enteredValue))
                                        {
                                            if(selectedValue != null && selectedValue.length() != 0)
                                            {
                                                int j = selectedValue.indexOf(";");
                                                if(j > 0)
                                                {
                                                    selectedValue = selectedValue.substring(0, j);
                                                }
                                                enteredValue = selectedValue;
                                            }
                                        }

                                        if(enteredValue == null || (enteredValue.trim()).length() == 0)
                                            continue;

                                        if(name != null && name.length() > 0)
                                        {
                                            attrMap.put("comboDescriptor_"+name,  operatorValue);
                                            attrMap.put("txt_"+name,  enteredValue);
                                            attrMap.put(name,  enteredValue);
                                            sAttrList.append(name + ",");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if(attrMap.size() > 0)
                attrMap.put("attrList", sAttrList.toString());
        }
        catch(Exception e)
        {
        	try
            {
        		MCADServerException.createException(e.getMessage(), e);
            }
	        catch(Exception exc){}
        }

        return attrMap;
    }

	private String checkForNewAttributesToAdd(Context context, IEFXmlNodeImpl xmlPacket, String queryName, String languageStr) throws Exception
    {
        String returnXMLOutput = "";
        if(xmlPacket != null)
            xmlPacket.getXmlString();

        String type = null;
        IEFXmlNode attributeListNode = null;
        HashMap attributesMap = new HashMap();
        HashSet attributesSet = new HashSet();
        boolean bProjectTypeFound = false;

        if(xmlPacket != null)
        {
            Enumeration nodeEnum = xmlPacket.elements();
            while(nodeEnum.hasMoreElements())
            {
                IEFXmlNodeImpl rootNode = (IEFXmlNodeImpl)nodeEnum.nextElement();
                if(rootNode != null)
                {
                    IEFXmlNodeImpl searchdetailsNode = rootNode.getChildByName("searchdetails");
                    if(searchdetailsNode != null)
                    {
                        Enumeration childNodes = searchdetailsNode.elements();
                        while(childNodes.hasMoreElements())
                        {
                            IEFXmlNode node = (IEFXmlNode)childNodes.nextElement();
                            if("basics".equalsIgnoreCase(node.getName()))
                            {
                                type = node.getAttribute("type");
                                //For MSPI
                                if(_source != null && _source.length() > 0)
                                {
                                    type = getMSPIType(context, _util, type);
                                    if(type != null && type.length() > 0)
                                    {
                                        node.setAttributeValue("type", type);
                                        
										String displayAttributeName = getDisplayValue(context, "Type", type, languageStr);
                                        node.setAttributeValue("displaytypevalue", displayAttributeName);
                                        node.setAttributeValue("name", "*");
                                        node.setAttributeValue("revision", "*");
                                        node.setAttributeValue("latestRevFlag", "false");
                                        try
                                        {
                                            String vaultDisplay = i18nNow.getI18nString("emxComponents.Preferences.DefaultVault",  "emxFrameworkStringResource", languageStr);
                                            if(vaultDisplay == null) vaultDisplay = "*";
                                            node.setAttributeValue("vault", vaultDisplay);
                                        }
                                        catch (Exception e)
                                        {
                                            node.setAttributeValue("vault", "*");
                                        }
                                        node.setAttributeValue("owner", "*");
                                        node.setAttributeValue("title", "*");
                                        node.setAttributeValue("datecreatedafter", "");
                                        node.setAttributeValue("datecreatedbefore", "");
                                        node.setAttributeValue("andoroption", "or");
                                        node.setAttributeValue("querylimit", sDefaultQueryLimit);
                                        node.setAttributeValue("defaultdateformat", defaultDateFormat);
                                        node.setAttributeValue("showtime", showTime);
                                        break;
                                    }
								} 
								else
                                {
									if(MCADMxUtil.getActualNameForAEFData(context, "type_ProjectSpace").equalsIgnoreCase(type))
                                    {
                                        bProjectTypeFound = true;
                                        break;
                                    }
                                }

                                node.setAttributeValue("defaultdateformat", defaultDateFormat);
                                node.setAttributeValue("showtime", showTime);
                                try
                                {
                                    Query newQuery = new Query(queryName);
                                    newQuery.open(context);
                                    type = newQuery.getBusinessObjectType();
                                    newQuery.close(context);
                                }
                                catch(Exception ex)
                                {
                                }
                            }
                            else if("attributelist".equalsIgnoreCase(node.getName()))
                            {
                                attributeListNode = node;
                                IEFXmlNodeImpl attrListNode = (IEFXmlNodeImpl)attributeListNode;
                                if(attrListNode != null)
                                {

                                    Enumeration cElements = attrListNode.elements();
                                    //For MSPI
                                    if(_source != null && _source.length() > 0)
                                    {
                                        type = getMSPIType(context, _util, type);
									} 
									else
                                    {
										if(MCADMxUtil.getActualNameForAEFData(context, "type_ProjectSpace").equalsIgnoreCase(type))
                                        {
                                            bProjectTypeFound = true;
                                            break;
                                        }
                                    }

                                    if(cElements.hasMoreElements())
                                    {
                                        if(type != null && type.length() > 0)
                                        {
                                            node.setAttributeValue("type", type);
                                            
											String displayAttributeName = getDisplayValue(context, "Type", type, languageStr);
                                            node.setAttributeValue("displaytypevalue", displayAttributeName);
                                            //Get the list of attributes associated with the type.
                                            try
                                            {
                                                BusinessType objectType = new BusinessType(type,context.getVault());
                                                objectType.open(context, false);
                                                matrix.db.FindLikeInfo likeObjects = objectType.getFindLikeInfo(context);
                                                objectType.close(context);
                                                AttributeList attributeList    = likeObjects.getAttributes();
                                                AttributeItr  attributeListItr = new AttributeItr(attributeList);
                                                while (attributeListItr.next())
                                                {
                                                    Attribute attributeObject = attributeListItr.obj();
                                                    String attributeName = attributeObject.getName();

                                                    if(!notAllowedAttributes.contains(attributeName))
                                                        attributesSet.add(attributeName);
                                                }

                                                Enumeration attributeNodes = node.elements();

                                                while(attributeNodes.hasMoreElements())
                                                {
                                                    IEFXmlNode attributeNode = (IEFXmlNode)attributeNodes.nextElement();
                                                    String nodeName = attributeNode.getName();
                                                    String sOperatorvalue = "";
                                                    String sSelectedvalue = "";
                                                    String sEnteredvalue = "";
                                                    String sSelselectedvalue = "";
                                                    String sSeloperatorvalue = "";
                                                    if(nodeName.equals("attribute"))
                                                    {
                                                        String sAttrName = attributeNode.getAttribute("name");
                                                        AttributeType attrTypeObj = new AttributeType(sAttrName);
                                                        String sOperator = attrTypeObj.getDataType(context);

                                                        if(attributesSet.contains(sAttrName))
                                                        {
                                                            attributesSet.remove(sAttrName);
                                                        }
                                                        else
                                                        {
                                                            //Attribute either deleted or it is not in the type. So
                                                            node.deleteChild(attributeNode);
                                                            continue;
                                                        }
                                                        sOperatorvalue = attributeNode.getAttribute("operatorvalue");
                                                        sSelectedvalue = attributeNode.getAttribute("selectedvalue");
                                                        sEnteredvalue = attributeNode.getAttribute("enteredvalue");
                                                        sSelselectedvalue = attributeNode.getAttribute("selselectedvalue");
                                                        sSeloperatorvalue = attributeNode.getAttribute("seloperatorvalue");

														HashMap operatorMap = getOperatorvalues(context, sOperator, languageStr);
                                                        
                                                        sOperator = (String)operatorMap.get("operator");
                                                        sOperatorvalue = (String)operatorMap.get("operatorvalues");
                                                        if(sOperatorvalue == null || sOperatorvalue.length() == 0)
                                                            continue;

														String sRangevalues = getSelectedValues(context, attrTypeObj, languageStr);
                                                        
                                                        if(sEnteredvalue == null) sEnteredvalue = "";
                                                        if(sSelselectedvalue == null) sSelselectedvalue = "";
                                                        if(sSeloperatorvalue == null) sSeloperatorvalue = "";

                                                        attributeNode.setAttributeValue("operator", sOperator);
                                                        attributeNode.setAttributeValue("operatorvalue", sOperatorvalue);
                                                        attributeNode.setAttributeValue("seloperatorvalue", sSeloperatorvalue);
                                                        attributeNode.setAttributeValue("selectedvalue", sSelectedvalue);
                                                        attributeNode.setAttributeValue("selselectedvalue", sSelselectedvalue);
                                                        attributeNode.setAttributeValue("enteredvalue", sEnteredvalue);
                                                    }
                                                }

                                                if(!attributesSet.isEmpty())
                                                {
                                                    Iterator itr = attributesSet.iterator();
                                                    while(itr.hasNext())
                                                    {
                                                        String sOperatorvalue = "";
                                                        String sSelectedvalue = "";

                                                        String attr = (String)itr.next();
                                                        AttributeType attrTypeObj = new AttributeType(attr);
                                                        String sOperator = attrTypeObj.getDataType(context);

														HashMap operatorMap = getOperatorvalues(context, sOperator, languageStr);
                                                        
                                                        sOperator = (String)operatorMap.get("operator");
                                                        sOperatorvalue = (String)operatorMap.get("operatorvalues");

                                                        if(sOperatorvalue == null || sOperatorvalue.length() == 0)
                                                            continue;

														String sRangevalues = getSelectedValues(context, attrTypeObj, languageStr);
                                                        
                                                        IEFXmlNodeImpl attributeNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
                                                        attributeNode.setName("attribute");
                                                        Hashtable attributesTable = new Hashtable();
                                                        attributesTable.put("name", attr);
                                                        attributesTable.put("operator", sOperator);
                                                        attributesTable.put("operatorvalue", sOperatorvalue);
                                                        attributesTable.put("selectedvalue", sSelectedvalue);
                                                        attributesTable.put("seloperatorvalue", "");
                                                        attributesTable.put("selselectedvalue", "");
                                                        attributesTable.put("enteredvalue", "");
                                                        attributeNode.setAttributes(attributesTable);
                                                        attributesMap.put(attr, attributeNode);
                                                    }
                                                    break;
                                                }

                                            }
                                            catch(Exception emx)
                                            {
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if(bProjectTypeFound)
                            attributesSet = new HashSet(); // To clear it
                    }
                }
            }

            //Update the attribute list node with the attributes tag
            Iterator itr1 = attributesSet.iterator();
            while(itr1.hasNext())
            {
                IEFXmlNodeImpl attributeNode = (IEFXmlNodeImpl)attributesMap.get((String)itr1.next());
                if(attributeListNode != null && attributeNode != null)
                    ((IEFXmlNodeImpl)attributeListNode).addNode(attributeNode);
            }

            if(bProjectTypeFound)
                returnXMLOutput = getDefaultQueryDetails(context, languageStr);
            else
                returnXMLOutput = xmlPacket.getXmlString();
        }
        return returnXMLOutput;
    }

    /**
     * read parameters
     * @param query criteria
     */
    private HashMap readParameters(Context context, String[] args, boolean isQueryNameAvailable) throws Exception
    {
        HashMap parametersMap = new HashMap();
        int index = 0;
        String lang = "en";
        String txtQueryName = "";

        if(args != null && args.length != 0)
        {
            //Read the parameters
            lang                = args[index++];
            if(lang != null) lang = lang.trim();

            if(isQueryNameAvailable)
            {
                txtQueryName    = args[index++];
                if(txtQueryName != null) txtQueryName = txtQueryName.trim();
            }
            else
                txtQueryName    = msoiHiddenQuery;

	    String sAttributeList = null;
            String txtType          = args[index++];
            String txtName          = args[index++];
            String txtRev           = args[index++];
            String latestRevOnly    = args[index++];
            String txtVault         = args[index++];
            String txtOwner         = args[index++];
            String txtTitle         = args[index++];
            String dateCreatedAfter = args[index++];
            String dateCreatedBefore= args[index++];
            String sAndOrOption     = args[index++];
            String sQueryLimit      = args[index++];
            String strDefdateformat = args[index++];
            String strShowTime      = args[index++];
            String locale			= null;
    	    String timezoneoffset   = null;
    	    
    	    MCADServerGeneralUtil generalUtil = new MCADServerGeneralUtil();

	    if(index < args.length)
		sAttributeList   = args[index++];

            if(txtType != null) txtType = txtType.trim();
            if(txtName != null) txtName = txtName.trim();
            if(txtRev != null) txtRev = txtRev.trim();
            if(latestRevOnly != null) latestRevOnly = latestRevOnly.trim();
            if(txtVault != null) txtVault = txtVault.trim();
            if(txtOwner != null) txtOwner = txtOwner.trim();
            if(txtTitle != null) txtTitle = txtTitle.trim();
            
            if(dateCreatedAfter != null)
            {
            	dateCreatedAfter = dateCreatedAfter.trim();
            	 if(index < args.length && !dateCreatedAfter.trim().equals(""))
            	 {
            		 datetimepattern = args[index++];
            		 locale          = args[index++];
            		 timezoneoffset  = args[index++];
            		 
                	 dateCreatedAfter = generalUtil.getAttributeValueForDateTimeForJPO(context,null,  dateCreatedAfter, "timestamp", true, datetimepattern, locale, timezoneoffset);

            	 }
                 
                 
            }
            if(dateCreatedBefore != null)
            {
            	dateCreatedBefore = dateCreatedBefore.trim();
            	if(!dateCreatedBefore.trim().equals(""))
            	{
	            	if(datetimepattern==null && index < args.length)
	            	{
	           		 	 datetimepattern = args[index++];
	           		     locale          = args[index++];
		           		 timezoneoffset  = args[index++];
	           	 	}
            	dateCreatedBefore = generalUtil.getAttributeValueForDateTimeForJPO(context,null,  dateCreatedBefore,  "timestamp",true , datetimepattern, locale, timezoneoffset);
            	}
            }
            
            if(sAndOrOption != null) sAndOrOption = sAndOrOption.trim();
            if(sQueryLimit != null) sQueryLimit = sQueryLimit.trim();
            if(strDefdateformat != null) strDefdateformat = strDefdateformat.trim();
            if(strShowTime != null) strShowTime = strShowTime.trim();
            if(sAttributeList != null) sAttributeList = sAttributeList.trim();

            String sWhereExp    = "";
            String sSearchFormat= "*";
            String sSearchText  = "";

            String sValidateQuery = "false";
            String sExpandType  = "true";
            String sUpperQueryLimit = "";
            int iQueryLimit = 0;
            int iUpperQueryLimit = 0;
            String txtVaultToSave   = txtVault;
            //Verify the parameters
            if(txtQueryName == null || txtQueryName.length() == 0)
            {
                throw new Exception(i18nNow.getI18nString("mcadIntegration.Server.Message.QueryNameIsEmpty",  "iefStringResource", lang));
            }

            if(txtType == null || txtType.length() == 0)
            {
                throw new Exception(i18nNow.getI18nString("mcadIntegration.Server.Message.TypeNameIsEmpty",  "iefStringResource", lang));
            }

            if(txtName == null || txtName.length() == 0)
                txtName = "*";

            if(txtRev == null || txtRev.length() == 0)
                txtRev = "*";

            if(txtOwner == null || txtOwner.length() == 0)
                txtOwner = "*";

            if(txtTitle == null)
                txtTitle = "";

            if(latestRevOnly == null || latestRevOnly.length() == 0)
                latestRevOnly = "false";

            if(strDefdateformat == null || strDefdateformat.length() == 0)
                strDefdateformat = defaultDateFormat;

            if(strShowTime == null || strShowTime.length() == 0)
                strShowTime = showTime;

            if(txtVault == null || txtVault.length() == 0)
            {
                txtVault = "*";
            }
            else
            {
                HashMap vaultsMap = new HashMap();
                addVaultToList(context, vaultsMap, lang);
                if(txtVault.equals(PersonUtil.SEARCH_ALL_VAULTS) || txtVault.equals(PersonUtil.SEARCH_LOCAL_VAULTS) || txtVault.equals(PersonUtil.SEARCH_DEFAULT_VAULT))
                {
                    txtVaultToSave = (String)vaultsMap.get(txtVault);
                    txtVault = getSearchVaults(context, false, txtVault);
                }
                else
                {
                    if(vaultsMap.containsValue(txtVault))
                    {
                        Iterator itr = vaultsMap.keySet().iterator();
                        while(itr.hasNext())
                        {
                            String vaultKey = (String)itr.next();
                            if(txtVault.equalsIgnoreCase((String)vaultsMap.get(vaultKey)))
                            {
                                txtVaultToSave = (String)vaultsMap.get(vaultKey);
                                txtVault = getSearchVaults(context, false, vaultKey);
                                break;
                            }
                        }
                    }
                }

                if(txtVault == null || txtVault.length() == 0)
                    txtVault = "*";
            }

            if(sAndOrOption == null)
                sAndOrOption = "or";

            if(sQueryLimit == null)
            {
                sQueryLimit = sDefaultQueryLimit;
                if( (sQueryLimit == null) || ("null".equals(sQueryLimit)) || ("".equals(sQueryLimit))) {
                    sQueryLimit = "100";
                }

                sUpperQueryLimit = FrameworkProperties.getProperty("emxFramework.Search.UpperQueryLimit");
                if( (sUpperQueryLimit == null) || ("null".equals(sUpperQueryLimit)) || ("".equals(sUpperQueryLimit))) {
                    //this number must not excede 32767
                    sUpperQueryLimit="32767";
                    iUpperQueryLimit= 32767;
                }

                try{
                    iUpperQueryLimit = Integer.parseInt(sUpperQueryLimit, 10);
                }
                catch(NumberFormatException nfe)
                {
                    iUpperQueryLimit = 32767;
                }

                String QueryLimitNumberFormat = FrameworkProperties.getProperty("emxFramework.GlobalSearch.ErrorMsg.QueryLimitNumberFormat");
                String QueryLimitMaxAllowed = FrameworkProperties.getProperty("emxFramework.GlobalSearch.ErrorMsg.QueryLimitMaxAllowed");


                if(sQueryLimit.length() == 0)
                    throw new Exception(QueryLimitNumberFormat);

                //does sQueryLimit contain a decimal?
                if(sQueryLimit.indexOf(".") != -1)
                    throw new Exception(QueryLimitNumberFormat);

                //iQueryLimit must be at least 1
                try{
                    iQueryLimit = Integer.parseInt(sQueryLimit, 10);
                }
                catch(NumberFormatException nfe)
                {
                    throw new Exception(QueryLimitNumberFormat);
                }
                if(iQueryLimit < 1)
                    throw new Exception(QueryLimitNumberFormat);

                //sQueryLimit must not be greater than 32767
                if(iQueryLimit > iUpperQueryLimit)
                    throw new Exception(QueryLimitMaxAllowed + "" + iUpperQueryLimit);
            }

            if(sAttributeList == null)
                sAttributeList = "";

            IEFXmlNodeImpl attributeList = null;
            HashMap attributesMap = new HashMap();

            if(sAttributeList != null && sAttributeList.length() != 0)
				attributeList = getCommandPacket(context, sAttributeList);

            if(attributeList != null)
                attributesMap = createAttributesMap(attributeList);

            if(attributesMap != null)
            {
                attributesMap.put("andOrField", sAndOrOption);
                attributesMap.put("defaultdateformat", strDefdateformat);
            }
            try
            {
                sWhereExp = getAdvanceSearchWhereExpression(context, txtType, attributesMap, dateCreatedAfter, dateCreatedBefore, latestRevOnly, txtTitle,lang);
            }catch(Exception ex)
            {
                throw ex;
            }
            if(doesQueryExist(context, txtQueryName))
                isNewQuery = false;
            else
                isNewQuery = true;

            parametersMap.put("language", lang);
            parametersMap.put("queryname", txtQueryName);
            parametersMap.put("type", txtType);
            
			String displayAttributeName = getDisplayValue(context, "Type", txtType, lang);
            
            parametersMap.put("displaytypevalue", displayAttributeName);
            parametersMap.put("name", txtName);
            parametersMap.put("revision", txtRev);
            parametersMap.put("latestrevisionflag", latestRevOnly);
            parametersMap.put("vault", txtVault);
            parametersMap.put("owner", txtOwner);
            parametersMap.put("title", txtTitle);
            parametersMap.put("datecreatedafter", dateCreatedAfter);
            parametersMap.put("datecreatedbefore", dateCreatedBefore);
            parametersMap.put("andor", sAndOrOption);
            parametersMap.put("querylimit", sQueryLimit);
            parametersMap.put("whereexp", sWhereExp);
            parametersMap.put("searchformat", sSearchFormat);
            parametersMap.put("searchtext", sSearchText);
            parametersMap.put("validatequery", sValidateQuery);
            parametersMap.put("expandtype", sExpandType);

            if(attributeList != null)
                parametersMap.put("attributelist", attributeList);

            Hashtable basicAttributes = new Hashtable();
            basicAttributes.put("type", txtType);
            
			String displayAttrName = getDisplayValue(context, "Type", txtType, lang);
            
            basicAttributes.put("displaytypevalue", displayAttrName);
            basicAttributes.put("name", txtName);
            basicAttributes.put("revision", txtRev);
            basicAttributes.put("latestRevFlag", latestRevOnly);
            basicAttributes.put("vault", txtVaultToSave);
            basicAttributes.put("owner", txtOwner);
            basicAttributes.put("title", txtTitle);
            basicAttributes.put("datecreatedafter", dateCreatedAfter);
            basicAttributes.put("datecreatedbefore", dateCreatedBefore);
            basicAttributes.put("andoroption", sAndOrOption);
            basicAttributes.put("querylimit", sQueryLimit);
            basicAttributes.put("defaultdateformat", strDefdateformat);
            basicAttributes.put("showtime", strShowTime);

            parametersMap.put("basicAttributes", basicAttributes);
        }
        else
        {
            throw new Exception("Parameters NOT received in IEFSearch JPO");
        }
        //System.out.println(" parametersMap : " + parametersMap);
        return parametersMap;
    }

    private boolean saveQueryInMx(Context context, HashMap paramMap) throws Exception
    {
        boolean retVal = true;
        String txtQueryName = "";
        try
        {
			String lang             = (String)paramMap.get("language");
            txtQueryName            = (String)paramMap.get("queryname");
            String txtType          = (String)paramMap.get("type");
            String txtName          = (String)paramMap.get("name");
            String txtRev           = (String)paramMap.get("revision");
            String latestRevOnly    = (String)paramMap.get("latestrevisionflag");
            String txtVault         = (String)paramMap.get("vault");
            String txtOwner         = (String)paramMap.get("owner");
            String txtTitle         = (String)paramMap.get("title");
            String dateCreatedAfter = (String)paramMap.get("datecreatedafter");
            String dateCreatedBefore= (String)paramMap.get("datecreatedbefore");
            String sAndOrOption     = (String)paramMap.get("andor");
            String sQueryLimit      = (String)paramMap.get("querylimit");
            String sWhereExp        = (String)paramMap.get("whereexp");
            String sSearchFormat    = (String)paramMap.get("searchformat");
            String sSearchText      = (String)paramMap.get("searchtext");
            String sValidateQuery   = (String)paramMap.get("validatequery");
            String sExpandType      = (String)paramMap.get("expandtype");

            boolean bValidateQuery = (new Boolean(sValidateQuery)).booleanValue();
            boolean bExpandType = (new Boolean(sExpandType)).booleanValue();

            //set the current query
            Query newQuery = new Query(txtQueryName);
            newQuery.open(context);
            newQuery.setBusinessObjectType(txtType);
            newQuery.setBusinessObjectName(txtName);
            newQuery.setBusinessObjectRevision(txtRev);
            newQuery.setExpandType(bExpandType);
            newQuery.setOwnerPattern(txtOwner);
            newQuery.setSearchText(sSearchText);
            newQuery.setSearchFormat(sSearchFormat);
            newQuery.setVaultPattern(txtVault);
            newQuery.setWhereExpression(sWhereExp);
            newQuery.setObjectLimit(Short.parseShort(sQueryLimit));
            newQuery.setQueryTrigger(bValidateQuery);
            newQuery.update(context);
            newQuery.close(context);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            retVal = false;
            throw ex;
        }

        return retVal;
    }

    /**
     * Update existing query criteria
     *
     * @param 'query name' String value
     * @param query criteria
     * @throws Exception if the operation fails
     */
    public void updateQuery(Context context, HashMap paramMap) throws MatrixException
    {
        String xmlOutput = "";
        String lang = (String)paramMap.get("language");

        try
        {
            Hashtable basicAttributes = (Hashtable)paramMap.get("basicAttributes");
            String txtQueryName = (String)paramMap.get("queryname");

            IEFXmlNodeImpl attributeList = (IEFXmlNodeImpl)paramMap.get("attributelist");
            if(attributeList == null)
            {
                attributeList = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
                attributeList.setName("attributelist");
            }

            //Update the query and set the description
            //Create the XML string to set the description.
            IEFXmlNodeImpl searchDetailsNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
            searchDetailsNode.setName("searchdetails");

            IEFXmlNodeImpl basicsNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
            basicsNode.setName("basics");
            basicsNode.setAttributes(basicAttributes);

            searchDetailsNode.addNode(basicsNode);

            if(attributeList != null)
                searchDetailsNode.addNode(attributeList);

            //Update the query
            ContextUtil.startTransaction(context, true);
            UISearch.updateSearch(context, txtQueryName, encodeURL(searchDetailsNode.getXmlString(), "UTF-8"));
            ContextUtil.commitTransaction(context);
        } 
        catch (Exception ex)
        {
            ContextUtil.abortTransaction(context);
		ex.printStackTrace();
            String emxExceptionString = (ex.toString()).trim();
            // set the error string in the Error object
            if(xmlOutput.length() == 0)
            {
                try
                {
                    if(isNewQuery)
                        xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.UnableToSaveQuery",  "iefStringResource", lang);
                    else
                        xmlOutput = i18nNow.getI18nString("mcadIntegration.Server.Message.UnableToUpdateQuery",  "iefStringResource", lang);
                }
                catch(Exception e)
                {
                    throw new MatrixException(ex.getMessage());
                }
                throw new MatrixException(xmlOutput);
            }
            
            throw new MatrixException(emxExceptionString);
        }
    }


    /**
     * checkForBooleanValue : checkForBooleanValue.
     *
     * @param attribute name
     * @param attribute value
     */
    private boolean checkForBooleanValue(Context context, String sAttributeName, String sAttributeValue)
    {
        boolean bRetVal = true;
        try
        {
            AttributeType attrType = new AttributeType(sAttributeName);
            String dataType = attrType.getDataType(context);
            if("boolean".equalsIgnoreCase(dataType))
            {
                StringList choices = attrType.getChoices(context);
                if(choices != null)
                {
                    if(choices.contains(sAttributeValue))
                        bRetVal = true;
                    else
                        bRetVal = false;
                }
            }
        }
        catch(Exception ex)
        {
            bRetVal = false;
        }

        return bRetVal;
    }
    /**
     * getSubTypes : Get the sub types of a given type.
     *
     * @param type name
     * @throws Exception if the operation fails
     */
	public String getTypes(Context context, String[] args) throws Exception
    {
        String xmlOutput = "";
        String languageStr = "en";
        String sType = "";
        HashMap typeMap = new HashMap();
		String sDefaultRootTypeListFromGCO = MCADMxUtil.getActualNameForAEFData(context, "type_DOCUMENTS");

        try
        {
            //Get the list of root level types from the GCO.
            String attrName = null;
            if(_util != null)
				attrName = MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-Pref-IEF-MSOIGeneralSearchTypes");          

            if(attrName != null && attrName.length() != 0)
            {
                sDefaultRootTypeListFromGCO = _GlobalConf.getPreferenceValue(attrName);
                if(sDefaultRootTypeListFromGCO == null || sDefaultRootTypeListFromGCO.length() == 0)
					sDefaultRootTypeListFromGCO = MCADMxUtil.getActualNameForAEFData(context, "type_DOCUMENTS");
            }

            if(args != null && args.length > 0)
            {
                languageStr = args[0];
                if(languageStr == null || languageStr.length() == 0)
                    languageStr = "en";
                sType = args[1];
                if(sType == null)
                    sType = "";
            }

            //To list the root (top) level types
            if(sType != null && sType.length() == 0)
            {
                //For MSPI
                if(_source != null && _source.length() > 0)
                {
                    //Replace sDefaultRootTypeListFromGCO by the type of MSPI
                    sDefaultRootTypeListFromGCO = getMSPIType(context, _util, "");
                }

                TreeMap treemapTypeList = new TreeMap();
                StringTokenizer typeListTokenizer = new StringTokenizer(sDefaultRootTypeListFromGCO, ",");
                while (typeListTokenizer.hasMoreTokens())
                {
                    String eachType=typeListTokenizer.nextToken().trim();
                    if(!isHidden(context, "type", eachType))
                        treemapTypeList.put(eachType, eachType);
                }

				getTypeMap(context, treemapTypeList, typeMap, languageStr);
            }

            //To list the sub types
            if(sType != null && sType.length() > 0)
            {
                BusinessType busType = new BusinessType(sType, context.getVault());
                busType.open(context, false);
                if(busType.hasChildren(context))
                {
                    BusinessTypeList lstChildren = busType.getChildren(context);

                    Iterator iterator = lstChildren.iterator();
                    if (iterator != null)
                    {
                        TreeMap treemapTypeList = new TreeMap();
                        while(iterator.hasNext())
                        {
                            BusinessType busTypeName = (BusinessType) iterator.next();
                            busTypeName.open(context, false);
                            String sTypeName = busTypeName.getName();
                            if(!isHidden(context, "type", sTypeName))
                                treemapTypeList.put(sTypeName, sTypeName);
                            busTypeName.close(context);
                        }
						getTypeMap(context, treemapTypeList, typeMap, languageStr);
                    }
                }
                busType.close(context);
            }

            if(typeMap.size() > 0)
            {
                //Create the XML string.
                IEFXmlNodeImpl typeDetailsNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
                typeDetailsNode.setName("details");

                java.util.Set types = typeMap.keySet();
                Iterator iterator = types.iterator();
                while(iterator.hasNext())
                {
                    String sTypeName = (String)iterator.next();
                    String displayValue = (String)typeMap.get(sTypeName);
                    Hashtable typeAttributes = new Hashtable();
                    typeAttributes.put("name", sTypeName);
                    typeAttributes.put("displayvalue", displayValue);

                    IEFXmlNodeImpl typeNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
                    typeNode.setName("type");
                    typeNode.setAttributes(typeAttributes);

                    typeDetailsNode.addNode(typeNode);
                }

                xmlOutput = typeDetailsNode.getXmlString();
            }
        }
        catch(Exception e)
        {
        }

        return xmlOutput;
    }

    /**
     * getSubTypes : Get the sub types of a given type.
     *
     * @param type name
     * @throws Exception if the operation fails
     */
	public String getAllTypes(Context context, String[] args) throws Exception
    {
        String xmlOutput = "";
        String languageStr = "en";
        String rootTypeList = null;
        String rootType = "type_DOCUMENTS";

        try
        {
            if(null != args && 0 != args.length)
            {
                if (null != args[0] && 0 != args[0].length())
                {
			languageStr = args[0];
                }
				
                if (1 < args.length && null != args[1] && 0 != args[1].length())
                {
			rootType = args[1];
                }
            }

            if(_util != null)
            {
                rootTypeList = _util.isCDMInstalled(context) 
			? MCADMxUtil.getActualNameForAEFData(context, rootType)
			: "Office Document";
            }
            else
            {
                rootTypeList = MCADMxUtil.getActualNameForAEFData(context, "type_DOCUMENTS");
            }

            // rootTypeList --> BusinessTypeList
            BusinessTypeList rootLevelTypes = new BusinessTypeList();
			StringTokenizer typeListTokenizer = new StringTokenizer(rootTypeList, ",");
            while (typeListTokenizer.hasMoreTokens())
            {
                rootLevelTypes.addElement(new BusinessType(typeListTokenizer.nextToken().trim(), context.getVault()));
            }

            //Create the XML string.
            IEFXmlNodeImpl typeDetailsNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
            typeDetailsNode.setName("typelist");

            if(rootLevelTypes.size() > 0)
            {
                getAllSubTypes(context, rootLevelTypes, languageStr, typeDetailsNode, "");
            }

            xmlOutput = typeDetailsNode.getXmlString();
        }
        catch(Exception e)
        {
        }

        return xmlOutput;
    }

    /**
     * getAllSubTypes : Get the sub types of a given type.
     */

    private void getAllSubTypes(Context context, BusinessTypeList typesList, String languageStr, IEFXmlNodeImpl typeDetailsNode, String parentTypeName)
    {
        Iterator iterator = typesList.iterator();
        if (iterator != null)
        {
            while(iterator.hasNext())
            {
                try
                {
                    BusinessType busType = (BusinessType)iterator.next();
                    busType.open(context, false);
                    String sTypeName = busType.getName();
                    if(!isHidden(context, "type", sTypeName))
                    {
                        IEFXmlNodeImpl typeNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
                        typeNode.setName("type");
                        typeDetailsNode.addNode(typeNode);
                        Hashtable typeAttributes = new Hashtable();
                        typeAttributes.put("name", sTypeName);

                        if(parentTypeName == null)
                            parentTypeName = "";

                        typeAttributes.put("parenttype", parentTypeName);

						String tempDisplayTypeName = getDisplayValue(context, "Type", sTypeName, languageStr);
                       
                        if(sTypeName != null && sTypeName.length() > 0)
                        {
                            if(tempDisplayTypeName == null || "".equals(tempDisplayTypeName))
                                typeAttributes.put("displayvalue", sTypeName);
                            else
                                typeAttributes.put("displayvalue", tempDisplayTypeName);
                        }


                        if(busType.hasChildren(context))
                        {
                            typeAttributes.put("haschildren", "true");
                            typeNode.setAttributes(typeAttributes);
                            BusinessTypeList lstChildren = busType.getChildren(context);
                            busType.close(context);
                            getAllSubTypes(context, lstChildren, languageStr, typeNode, sTypeName);
                        }
                        else
                        {
                            typeAttributes.put("haschildren", "false");
                            typeNode.setAttributes(typeAttributes);
                            busType.close(context);
                        }
                    }
                }
                catch(Exception ex)
                {
                }
            }
        }
    }

    /**
     * getVaults : Get the list of vaults.
     *
     * @throws Exception if the operation fails
     */
    public String getVaults(Context context, String[] args) throws MatrixException
    {
        String xmlOutput = "";
        MapList vaultsList = new MapList();
        HashMap vaultsMap = new HashMap();
        String languageStr = "en";
        try
        {
            if(args != null && args.length >= 0)
                languageStr = args[0];
            //Add to vault map list the following : User default vault, Local vault, All vaults
            addVaultToList(context, vaultsMap, languageStr);

            //Selected Vaults - START
            //To support FrameworkLite
            String organizationId = "";
            boolean bPersonObjectSchemaExist = false;
            if(PersonUtil.isPersonObjectSchemaExist(context))
            {
                organizationId = PersonUtil.getUserCompanyId(context);
                bPersonObjectSchemaExist = true;
            }
            //
            boolean incCollaborationPartners = false;
            String incCollPartners = "false";
            if("true".equalsIgnoreCase(incCollPartners))
            {
                incCollaborationPartners = true;
            }

            vaultsList = getAllVaultsDetails(context, incCollaborationPartners);
            Iterator itr = vaultsList.iterator();
            while(itr.hasNext())
            {
                Map vaultMap = (Map) itr.next();
                String vault = (String) vaultMap.get(DomainConstants.SELECT_NAME);
                String vaultDisplay = i18nNow.getAdminI18NString("Vault", vault, languageStr);

                if(vault != null && vault.length() > 0)
                {
                    if(vaultDisplay == null || "".equals(vaultDisplay))
                        vaultsMap.put(vault, vault);
                    else
                        vaultsMap.put(vault, vaultDisplay);
                }
            }
            //Selected Vaults - END

            //Sort the list
            TreeMap sortedVaultsMap = new TreeMap(vaultsMap);

            //Create the XML string.
            IEFXmlNodeImpl vaultDetailsNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
            vaultDetailsNode.setName("details");

            java.util.Set vaults = sortedVaultsMap.keySet();
            Iterator iterator = vaults.iterator();
            while(iterator.hasNext())
            {
                String sVaultName = (String)iterator.next();
                String displayValue = (String)sortedVaultsMap.get(sVaultName);

                Hashtable vaultAttributes = new Hashtable();
                vaultAttributes.put("name", sVaultName);
                vaultAttributes.put("displayvalue", displayValue);

                IEFXmlNodeImpl vaultNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
                vaultNode.setName("vault");
                vaultNode.setAttributes(vaultAttributes);

                vaultDetailsNode.addNode(vaultNode);
            }

            xmlOutput = vaultDetailsNode.getXmlString();

        }
        catch(Exception e)
        {
            
        }
        return xmlOutput;
    }

    private void addVaultToList(Context context,HashMap vaultsMap, String lang) throws Exception
    {
        String vault = PersonUtil.SEARCH_ALL_VAULTS;
        String vaultDisplay = i18nNow.getI18nString("emxFramework.GlobalSearch.All",  "emxFrameworkStringResource", lang);
        vaultsMap.put(vault, vaultDisplay);

        vault = PersonUtil.SEARCH_LOCAL_VAULTS;
        vaultDisplay = i18nNow.getI18nString("emxFramework.GlobalSearch.LocalVaults",  "emxFrameworkStringResource", lang);
        vaultsMap.put(vault, vaultDisplay);

        vault = PersonUtil.SEARCH_DEFAULT_VAULT;
        vaultDisplay = i18nNow.getI18nString("emxComponents.Preferences.DefaultVault",  "emxFrameworkStringResource", lang);
        vaultsMap.put(vault, vaultDisplay);
    }

	private void getTypeMap(Context context, TreeMap treeMap, HashMap typeMap, String languageStr)
    {
        if(treeMap != null)
        {
            java.util.Set exportSet = treeMap.keySet();
            Iterator exportIterator = exportSet.iterator();

            while(exportIterator.hasNext())
            {
                String tempTypeName = (String)exportIterator.next();
                
				String tempDisplayTypeName = getDisplayValue(context, "Type", tempTypeName, languageStr);
                
                if(tempTypeName != null && tempTypeName.length() > 0)
                {
                    if(tempDisplayTypeName == null || "".equals(tempDisplayTypeName))
                        typeMap.put(tempTypeName, tempTypeName);
                    else
                        typeMap.put(tempTypeName, tempDisplayTypeName);
                }
            }
        }
    }

    private boolean isHidden(Context context, String adminValue, String typeName)
    {
        boolean isHidden = false;
        try
        {
            MQLCommand mqlCmd = new MQLCommand();
            mqlCmd.open(context);
            mqlCmd.executeCommand(context, "list $1 $2 select $3 dump",adminValue,typeName, "hidden");
            String sResult = mqlCmd.getResult();
            mqlCmd.close(context);

            if(sResult != null)
            {
                StringTokenizer strTkParent = new StringTokenizer(sResult,"\n");
                while(strTkParent.hasMoreTokens()){
                    sResult = strTkParent.nextToken();
                }
            }

            if(sResult != null && "TRUE".equalsIgnoreCase(sResult))
                isHidden = true;
        }
        catch(Exception me)
        {
        }
        return isHidden;
    }

    private String getDefaultQueryDetails(Context context, String languageStr)
    {
        try
        {
            String sType = "";
            String attrName = null;

            if(_LocalConf != null)
            {
                if(_util != null)
					attrName = MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-MSOIDefaultSearchType"); 

                //For MSPI
                if(_source != null && _source.length() > 0)
                {
                    sType = getMSPIType(context, _util, "");
                }
                else
                {
                    if(attrName != null && attrName.length() != 0)
                        sType = _LocalConf.getPreferenceValueForIntegration(integratioName, attrName);

                    if(_GlobalConf != null)
                    {
                        //Get the value from the GCO first. If it is ENFORCED in GCO then use that value from GCO.
                        attrName = null;
                        if(_util != null)
							attrName = MCADMxUtil.getActualNameForAEFData(context,"attribute_IEF-Pref-IEF-MSOIDefaultSearchType");

                        if(attrName != null && attrName.length() != 0)
                        {
                            String isAttrEnforced = _GlobalConf.getPreferenceType(attrName);
                            if((MCADAppletServletProtocol.ENFORCED_PREFERENCE_TYPE).equalsIgnoreCase(isAttrEnforced) || "HIDDEN".equalsIgnoreCase(isAttrEnforced))
                            {
                                //get the value from GCO instead of LCO
                                String tempAttrVal = _GlobalConf.getPreferenceValue(attrName);
                                if(tempAttrVal != null && tempAttrVal.length() != 0)
                                    sType = tempAttrVal;
                            }
                        }
                    }
                }

                if(sType == null)
                    sType = "";
            }

            //Create the XML string.
            IEFXmlNodeImpl searchDetailsNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
            searchDetailsNode.setName("searchdetails");

            IEFXmlNodeImpl basicsNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
            basicsNode.setName("basics");
            Hashtable basicAttributes = new Hashtable();
            basicAttributes.put("type", sType);
            
			String displayAttributeName = getDisplayValue(context, "Type", sType, languageStr);
            basicAttributes.put("displaytypevalue", displayAttributeName);
            basicAttributes.put("name", "*");
            basicAttributes.put("revision", "*");
            basicAttributes.put("latestRevFlag", "false");
            String vaultDisplay = i18nNow.getI18nString("emxComponents.Preferences.DefaultVault",  "emxFrameworkStringResource", languageStr);
            if(vaultDisplay == null) vaultDisplay = "*";
            basicAttributes.put("vault", vaultDisplay);
            basicAttributes.put("owner", "*");
            basicAttributes.put("title", "*");
            basicAttributes.put("datecreatedafter", "");
            basicAttributes.put("datecreatedbefore", "");
            basicAttributes.put("andoroption", "or");
            basicAttributes.put("querylimit", sDefaultQueryLimit);
            basicAttributes.put("defaultdateformat", defaultDateFormat);
            basicAttributes.put("showtime", showTime);
            basicsNode.setAttributes(basicAttributes);

            IEFXmlNodeImpl attributeListNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
            attributeListNode.setName("attributeList");

            searchDetailsNode.addNode(basicsNode);
            searchDetailsNode.addNode(attributeListNode);

            return searchDetailsNode.getXmlString();
        }
        catch(Exception e)
        {
            return "";
        }
    }

    public String getAttributeList(Context context, String[] args) throws MatrixException
    {
        String xmlOutput = "";
        String languageStr = "en";
        if(args != null && args.length > 0)
        {
            try
            {
                //Read the parameters
                languageStr = args[0];
                String searchType = args[1];
                if(searchType != null && searchType.length() > 0)
                {
                    searchType = searchType.trim();
                    //Create business object of objectType passed in and open it
                    BusinessType objectType = new BusinessType(searchType,context.getVault());
                    objectType.open(context, false);

                    //Get attributes and close object
                    matrix.db.FindLikeInfo likeObjects = objectType.getFindLikeInfo(context);
                    objectType.close(context);
                    //Put attributes into a usable list
                    AttributeList attributeList    = likeObjects.getAttributes();
                    AttributeItr  attributeListItr = new AttributeItr(attributeList);

                    //While there are more attributes on the list
                    //Create a vector that holds the information for each attribute being displayed

                    //Create the XML string.
                    IEFXmlNodeImpl attributeListNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
                    attributeListNode.setName("attributelist");

                    while (attributeListItr.next())
                    {
                        Attribute attributeObject = attributeListItr.obj();
                        String attributeName = attributeObject.getName();
                        if(!isHidden(context, "attribute", attributeName))
                        {
                            if(!notAllowedAttributes.contains(attributeName))
                            {
                                AttributeType attrTypeObj = new AttributeType(attributeName);
                                String sOperator1 = attrTypeObj.getDataType(context);               //IR-548306-3DEXPERIENCER2015x : L86

                                //IR-542275-3DEXPERIENCER2015x : Custom attributes of type string, date etc are not displayed in the document attributes,if custom binary attribute is added to type Document
							    String sOperator = _util.getTypeForAttribute(context, attributeName);                                
								HashMap operatorMap = getOperatorvalues(context, sOperator, languageStr);
                                
                                sOperator = (String)operatorMap.get("operator");
                                String sOperatorvalue = (String)operatorMap.get("operatorvalues");

                                if(sOperatorvalue == null || sOperatorvalue.length() == 0)
                                    continue;
                                
								String sSelectedValue = getSelectedValues(context, attrTypeObj, languageStr);
                                
                                IEFXmlNodeImpl attributeNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
                                attributeNode.setName("attribute");
                                Hashtable attributesTable = new Hashtable();
                                attributesTable.put("name", attributeName);
                                attributesTable.put("operator", sOperator);
                                attributesTable.put("operatorvalue", sOperatorvalue);
                                attributesTable.put("selectedvalue", sSelectedValue);
                                attributeNode.setAttributes(attributesTable);
                                attributeListNode.addNode(attributeNode);
                            }
                        }
                    }
                    xmlOutput = attributeListNode.getXmlString();
                }
            }
            catch(Exception e)
            {
            }
        }
        return xmlOutput;
    }
    
	private HashMap getOperatorvalues(Context context, String sOperator,String languageStr) throws Exception
    {
    	HashMap operatorMap = null;
    	try
    	{
	    	HashMap argsMap = new HashMap(2);
	        argsMap.put("Operator", sOperator);
	        argsMap.put("language", languageStr);
			operatorMap = (HashMap)JPO.invoke(context, "IEFUtil", null, "getOperatorvalues", JPO.packArgs(argsMap), HashMap.class);
    	}
    	catch(MatrixException e)
    	{
    		try
            {
        		MCADServerException.createException(e.getMessage(), e);
            }
	        catch(Exception exc){}
    	}
        return operatorMap;
    }
    
	private String getSelectedValues(Context context, AttributeType attrTypeObj,String languageStr) throws Exception
    {
    	String sSelectedValue = null;
    	try
    	{
	    	Hashtable argumentsTable  = new Hashtable(2);
	        argumentsTable.put("AttributeType", attrTypeObj);
	        argumentsTable.put("language", languageStr);
			sSelectedValue = (String)JPO.invoke(context, "IEFUtil", null, "getSelectedValues", JPO.packArgs(argumentsTable), String.class);
    	}
    	catch(MatrixException e)
    	{
    		try
            {
        		MCADServerException.createException(e.getMessage(), e);
            }
	        catch(Exception exc){}
    	}
    	return sSelectedValue;
    }
    
	private String getDisplayValue(Context context, String adminType, String sTypeName, String languageStr) 
    {
    	String tempDisplayTypeName = null;
    	try
    	{
	    	Hashtable argumentsTable  = new Hashtable(3);
	        argumentsTable.put("adminType", adminType);
		argumentsTable.put("adminName", sTypeName);
	        argumentsTable.put("language", languageStr);

			tempDisplayTypeName = (String)JPO.invoke(context, "IEFUtil", null, "getDisplayValue", JPO.packArgs(argumentsTable), String.class);
    	}
    	catch(Exception e)
    	{
    		try
            {
        		MCADServerException.createException(e.getMessage(), e);
            }
	        catch(Exception exc){}
    	}
        return tempDisplayTypeName;
    }
    
    public String getQueryLimit(Context context, String [] args) throws Exception
    {
        Hashtable argTable = (Hashtable) JPO.unpackArgs(args);
        String sQueryName  = (String) argTable.get("Query");
	int k = getQueryLimit(context, sQueryName);
	Integer queryLimitValue = new Integer(k + "");
        return (k + "");
    }
    
    public int getQueryLimit(Context context, String sQueryName)
    {
        int iQueryLimit = -1;
        IEFXmlNodeImpl xmlPacket = null;
        if(sQueryName != null)
        {
            try
            {
                String searchData = UISearch.getSearchData(context, sQueryName);
                if(searchData != null)
                {
                    String xmlOutput = decodeURL(searchData, "UTF-8");
                    if(xmlOutput != null && xmlOutput.length() > 0)
                    {
						xmlPacket = getCommandPacket(context, xmlOutput);
                    }
                }

                if(xmlPacket != null)
                {
                    Enumeration nodeEnum = xmlPacket.elements();
                    while(nodeEnum.hasMoreElements())
                    {
                        IEFXmlNodeImpl rootNode = (IEFXmlNodeImpl)nodeEnum.nextElement();
                        if(rootNode != null)
                        {
                            IEFXmlNodeImpl searchdetailsNode = rootNode.getChildByName("searchdetails");
                            if(searchdetailsNode != null)
                            {
                                Enumeration childNodes = searchdetailsNode.elements();
                                while(childNodes.hasMoreElements())
                                {
                                    IEFXmlNode node = (IEFXmlNode)childNodes.nextElement();
                                    if("basics".equalsIgnoreCase(node.getName()))
                                    {
                                        String sQueryLimit = node.getAttribute("querylimit");
                                        iQueryLimit = new Integer(sQueryLimit).intValue();
                                        break;
                                    }
                                }
                            }
                        }
                        if(iQueryLimit != -1)
                            break;
                    }
                }
            }
            catch(Exception ex)
            {
                iQueryLimit = -1;
            }
        }

        return iQueryLimit;
    }

	private String  getMSPIType(Context context, MCADMxUtil _util, String _type) throws MCADException
    {
		String sType = MCADMxUtil.getActualNameForAEFData(context, "type_ProjectSpace");

        if(sType.equalsIgnoreCase(_type))
        {
            sType = "";
        }
        else if(_util != null)
        {
            try
            {
				String sTempType = MCADMxUtil.getActualNameForAEFData(context, "type_ProjectSpace"); 
                if(sTempType != null && (sTempType.trim()).length() != 0)
                    sType = sTempType;
            }
            catch (MCADException e)
            {
            }
        }

        return sType;
    }
    
    //Following functions are added to support the versions before 10.5 SP1
    private String getSearchVaults(Context context, boolean flag, String s)
    throws FrameworkException
    {
        String s1 = "";

        try
        {
            boolean flag1 = PersonUtil.isPersonObjectSchemaExist(context);
            String s2 = "";
            if(flag1)
                s2 = PersonUtil.getUserCompanyId(context);
            if("ALL_VAULTS".equals(s))
            {
                if(flag1)
                {
                    s1 = OrganizationUtil.getAllVaults(context, s2, flag);
				}
				else
                {
                    String s3 = MqlUtil.mqlCommand(context, "list $1 ", "vault");
                    StringTokenizer stringtokenizer = new StringTokenizer(s3, "\n");

                    while(stringtokenizer.hasMoreTokens())
                    {
                        String s5 = stringtokenizer.nextToken();
                        if(!s5.equals(PropertyUtil.getSchemaProperty(context, "vault_eServiceAdministration")))
                        {
                            s1 = s1 + s5;
                            if(stringtokenizer.hasMoreTokens())
                                s1 = s1 + ",";
                        }
                    }
                }
			} 
			else if("LOCAL_VAULTS".equals(s))
                {
                    if(flag1)
                        s1 = OrganizationUtil.getLocalVaults(context, s2);
                    else
                        s1 = context.getVault().getName();
                } else
                    if("DEFAULT_VAULT".equals(s))
                        s1 = context.getVault().getName();
                    else
                        if(s != null && !"".equals(s) && !"null".equals(s))
                            s1 = s;
            return s1;
        }
        catch(Exception exception)
        {
        }
        return s1;
    }

    private MapList getAllVaultsDetails(Context context, boolean flag) throws FrameworkException
    {
        MapList maplist = new MapList();
        try
        {
            String s1 = MqlUtil.mqlCommand(context, "list $1", "vault");
            StringTokenizer stringtokenizer = new StringTokenizer(s1, "\n");

            while(stringtokenizer.hasMoreTokens())
            {
                String s3 = "";
                s3 = stringtokenizer.nextToken();
                if(!s3.equals(PropertyUtil.getSchemaProperty(context, "vault_eServiceAdministration")))
                {
                    Vault vault = new Vault(s3);
                    vault.open(context);
                    HashMap hashmap1 = new HashMap();
                    hashmap1.put("name", vault.getName());
                    hashmap1.put("description", vault.getDescription(context));
                    vault.close(context);
                    maplist.add(hashmap1);
                }
            }
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
        }
        return maplist;
    }

    private String encodeURL(String s, String s1)
    {
        StringBuffer stringbuffer = new StringBuffer();
        if(s != null)
        {
            int i = s.length();
            for(int l = 0; l < i; l++)
            {
                char c1 = s.charAt(l);
                if((c1 < 'a' || c1 > 'z') && (c1 < 'A' || c1 > 'Z') && (c1 < '0' || c1 > '9') && c1 != '/' && c1 != '?' && c1 != '&' && c1 != '=' && c1 != ':' && c1 != '.' && c1 != ';' && c1 != '@' && c1 != '$' && c1 != ',' && c1 != '+' && c1 != '-' && c1 != '_' && c1 != '*')
                {
                    Character character = new Character(c1);
                    String s2 = character.toString();
                    byte abyte0[];
                    try
                    {
                        abyte0 = s2.getBytes(s1);
                    }
                    catch(UnsupportedEncodingException unsupportedencodingexception)
                    {
                        abyte0 = new byte[0];
                    }
                    for(int i1 = 0; i1 < abyte0.length; i1++)
                    {
                        char c2 = (char)abyte0[i1];
                        char c = c2;
                        stringbuffer.append('%');
                        int j = c >> 4 & 0xf;
                        int k = c & 0xf;
                        stringbuffer.append(Integer.toHexString(j));
                        stringbuffer.append(Integer.toHexString(k));
                    }

                } else
                {
                    stringbuffer.append(s.charAt(l));
                }
            }
        }
        return stringbuffer.length() != 0 ? stringbuffer.toString() : null;
    }

    private String decodeURL(String s, String s1)
    throws FrameworkException
    {
        int i = 0;
        int j = s == null ? 0 : s.length();
        byte abyte0[] = new byte[j];
        String s2 = null;
        try
        {
            for(int k = 0; k < j; k++)
                abyte0[k] = 0;

            if(s != null)
            {
                for(int l = 0; l < j; l++)
                    if(s.charAt(l) == '%' && l + 2 < j)
                    {
                        int i1 = Character.digit(s.charAt(l + 1), 16);
                        int j1 = Character.digit(s.charAt(l + 2), 16);
                        if(i1 != -1 && j1 != -1)
                        {
                            abyte0[i] = (byte)((i1 << 4) + j1);
                            i++;
                        }
                        l += 2;
                    }
                    else if(s.charAt(l) == '+')
                    {
                        abyte0[i] = 32;
                        i++;
                    } 
                    else
                    {
                        abyte0[i] = (byte)s.charAt(l);
                        i++;
                    }

            }
            if(i > 0)
                s2 = new String(abyte0, 0, i, s1);
        }
        catch(Exception exception)
        {
        }
        return s2;
    }

    //Above functions are added to support the versions before 10.5 SP1
}

