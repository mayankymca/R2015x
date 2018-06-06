import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;

import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectAttributes;
import matrix.db.Context;
import matrix.db.MQLCommand;
import matrix.util.MatrixException;
import matrix.util.MatrixService;

import com.matrixone.apps.domain.util.PropertyUtil;

/**
 * @author vadikv
 *
 * The <code>emxWdsServer_mxJPO</code> class contains implementation of web
 * service used by ENOVIA provider for Microsoft Desktop Search
 *
 * @version AEF 11.0.0.0 - Copyright (c) 2007, SmarTeam, Inc.
 */
public class emxWdsServer_mxJPO implements MatrixService {

    private final String PersistencyObject_Type = "stConfig_WdsSelectedTypes";
    private final String PersistencyObject_Name = "stConfig_WdsSelectedTypesObj";
    private final String PersistencyObject_Rev = "Selected types for WDS indexing";
    private final String PersistencyObject_Attribute = "stConfig_WdsService_Types";
    private final String xmlPrefix = "<?xml version=\"1.0\" encoding=\"utf-8\"?><items>";
    private final String xmlSuffix = "</items>";
    private final String itemPrefix = "<item>";
    private final String itemSuffix = "</item>";
    private final String KeywordsConfig_Attribute = "stConfig_WdsService_KeywordAttributes";
    private final String KeywordsConfig_Type = "stConfig_WdsService_Keywords";
    private final String KeywordsConfig_Name = "Wds Keywords";
    private final String Vault = ""; // "eService Administration"
    private Hashtable m_hash;

    /**
     * Returns business types names which mapped to be indexed by WDS
     */
    public String getItemsIdentifiers(Context context) throws Exception
    {
        StringBuffer resultBuilder = new StringBuffer(xmlPrefix);

        BusinessObject configBo = new BusinessObject(PersistencyObject_Type, PersistencyObject_Name, PersistencyObject_Rev, Vault);

        if (!configBo.exists(context))
        {
            throw new Exception("No configuration object found");
        }

        String selectedTypes = configBo.getAttributeValues(context,PersistencyObject_Attribute).getValue();

        String[] selectedTypesArray = selectedTypes.split(";");

        for (int i = 0; i < selectedTypesArray.length; i++)
        {
            resultBuilder.append(itemPrefix);
            resultBuilder.append(selectedTypesArray[i]);
            resultBuilder.append(itemSuffix);
        }

        resultBuilder.append(xmlSuffix);
        return resultBuilder.toString();
    }

    /**
     * Returns actual items according to provided business type for indexing.
     */
    public String getItemContentByIdentifier(Context context, String identifier)throws Exception
    {
        final String dumpSeparator = "~,~";
        final String recordsSeparator = ";~~~;";

        final int TYPE_IDX = 0;
        final int NAME_IDX = 1;
        final int REVISION_IDX = 2;
        final int ID_IDX = 3;
        final int AUTHOR_IDX = 4;
        final int DATE_IDX = 5;
        final int KEYWORDS_IDX = 6;

        StringBuilder resultBuilder = new StringBuilder(xmlPrefix);

        String realType = PropertyUtil.getSchemaProperty(identifier);

        String keywords = "";
        BusinessObject configBo = new BusinessObject(KeywordsConfig_Type,KeywordsConfig_Name, identifier, Vault);

        if (configBo.exists(context))
        {
            keywords = configBo.getAttributeValues(context,KeywordsConfig_Attribute).getValue();
        }

        StringBuilder mqlQeryBuilder = new StringBuilder("temp query bus \"");
        mqlQeryBuilder.append(realType);
        mqlQeryBuilder.append("\" * *  select id owner originated ");

        if (keywords != "")
        {
            mqlQeryBuilder.append(keywords);
        }
//Change by SJ7 05-Oct-11
        mqlQeryBuilder.append(" dump \"");
        //mqlQeryBuilder.append(dumpSeparator);
		mqlQeryBuilder.append(" $1 ");
        mqlQeryBuilder.append("\" recordseparator \"");
        //mqlQeryBuilder.append(recordsSeparator);
		mqlQeryBuilder.append(" $2 ");
        mqlQeryBuilder.append("\";");

        String classId = getClassId(identifier);

        MQLCommand mqlCommand = new MQLCommand();
        if (mqlCommand.executeCommand(context, mqlQeryBuilder.toString(), dumpSeparator, recordsSeparator))
        {
            String mqlResult = mqlCommand.getResult().trim();
            if (mqlResult.length() > 0)
            {
                String[] recordsArray = mqlResult.split(recordsSeparator);

                for (int i = 0; i < recordsArray.length; i++)
                {
                    String[] recordArray = recordsArray[i].split(dumpSeparator);
                    /*
                     * Fixed attribute mappings: 1. Title 2. Author 3. Date 4.
                     * Type (optional) 5. Folder (optional) 6. ID - ObjectID
                     */
                    resultBuilder.append(itemPrefix);
                    resultBuilder.append("<Title>");
                    resultBuilder.append(recordArray[TYPE_IDX]);
                    resultBuilder.append(" ");
                    resultBuilder.append(recordArray[NAME_IDX]);

                    if (recordArray[REVISION_IDX] != "")
                    {
                        resultBuilder.append(" rev ");
                        resultBuilder.append(recordArray[REVISION_IDX]);
                    }

                    resultBuilder.append("</Title>");
                    resultBuilder.append("<ID>");
                    resultBuilder.append(recordArray[ID_IDX]);
                    resultBuilder.append("</ID>");
                    resultBuilder.append("<ClassID>");
                    resultBuilder.append(classId);
                    resultBuilder.append("</ClassID>");
                    resultBuilder.append("<Author>");
                    resultBuilder.append(recordArray[AUTHOR_IDX]);
                    resultBuilder.append("</Author>");
                    resultBuilder.append("<Date>");
                    resultBuilder.append(recordArray[DATE_IDX]);
                    resultBuilder.append("</Date>");
                    resultBuilder.append("<Type>");
                    resultBuilder.append(recordArray[TYPE_IDX]);
                    resultBuilder.append("</Type>");

                    // Additional keywords:
                    if (recordArray.length > KEYWORDS_IDX)
                    {
                        resultBuilder.append("<keywords>");
                        resultBuilder.append(recordArray[KEYWORDS_IDX]);
                        int j = KEYWORDS_IDX + 1;

                        while (j < recordArray.length)
                        {
                            resultBuilder.append(",");
                            resultBuilder.append(recordArray[j]);
                            j++;
                        }
                        resultBuilder.append("</keywords>");
                    }
                    resultBuilder.append(itemSuffix);
                }
            }
        }
        resultBuilder.append(xmlSuffix);
        return resultBuilder.toString();
    }

    // new a real JPO entry point to use from Matrix (non ws layer)
    public String generateHtmlPreviewForObject(Context context, String[] args) throws Exception
    {
        return generateHtmlPreviewForObject(context, args[0]);
    }

    public String generateHtmlPreviewForObject(Context context, String objectIdentifier) throws Exception
    {
        final String htmlHeader = "<html>\r\n<head>"
                + "<meta http-equiv=\"Content-Language\" content=\"en-us\">\r\n"
                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">\r\n"
                + "<title>Attributes</title>\r\n"
                + getStyle() +"\r\n"
                + "</head>\r\n"
                + "<body>\r\n"
                + "<table border=\"0\" cellspacing=\"1\" cellpadding=\"0\" width=\"100%\">\r\n"
                + "    <tr>\r\n"
                + "        <td colspan=\"2\" class=\"heading1\" align=\"center\">Attributes</td>\r\n"
                + "    </tr>\r\n"
                + "    <tr>\r\n"
                + "        <td class=\"heading1\" align=\"center\">Name</td>\r\n"
                + "        <td class=\"heading1\" align=\"center\">Value</td>\r\n"
                + "    </tr>";

        final String htmlFooter = "</table></body></html>";

        StringBuffer resultBuilder = new StringBuffer(htmlHeader);

        try
        {
            BusinessObject previewedBo = new BusinessObject(objectIdentifier);
            previewedBo.open(context);

            resultBuilder.append(getAttributePreview("Description", previewedBo.getDescription(context)));
            resultBuilder.append(getAttributePreview("Vault", previewedBo.getVault()));
            BusinessObjectAttributes attributes = previewedBo.getAttributes(context);
            AttributeItr attributesItr = new AttributeItr(attributes.getAttributes());

            while (attributesItr.next())
            {
                Attribute attribute = attributesItr.obj();
                resultBuilder.append(getAttributePreview(attribute.getName(), attribute.getValue()));
            }
        }
        catch (MatrixException e)
        {
            resultBuilder.append("<tr><td>" + e.getMessage() + "</td></tr>");
        }

        resultBuilder.append(htmlFooter);

        return resultBuilder.toString();
    }

    private String getAttributePreview(String attributeName, String attributeValue)
    {
        if (attributeValue == "" || attributeValue == null)
        {
            attributeValue = "&nbsp;";
        }

        final String attributeNameHeader = "<tr><td class=\"label\" nowrap=\"nowrap\" width=\"275px\">";

        final String attributeNameFooter = "</td><td class=\"inputField\" wrap >";
        final String attributeValueFooter = "</td></tr>";

        StringBuilder resultBuilder = new StringBuilder(attributeNameHeader);
        resultBuilder.append(attributeName);
        resultBuilder.append(attributeNameFooter);
        resultBuilder.append(attributeValue);
        resultBuilder.append(attributeValueFooter);
        return resultBuilder.toString();
    }

    private String getHashName(String name) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte mdbytes[] = md.digest(name.getBytes());
        StringBuffer hashString = new StringBuffer(40);

        for (int i = 0; i < mdbytes.length; i++)
        {
            int b = 0xFF & mdbytes[i];
            // there must be a better way to format as %02x but this works
            if (b < 0x10)
            {
                hashString.append('0').append(Integer.toHexString(b));
            }
            else
            {
                hashString.append(Integer.toHexString(b));
            }
        }

        return (hashString.toString());
    }

    private String getClassId(String name)
    {
        String classId = "";
        Hashtable hash = getHashtable();

        if (!hash.containsKey(name))
        {
            try
            {
                classId = getHashName(name);
                hash.put(name, classId);
            }
            catch (Exception ex) {}
        }
        else
        {
            classId = hash.get(name).toString();
        }

        return classId;
    }

    private Hashtable getHashtable()
    {
        if (m_hash == null)
        {
            m_hash = new Hashtable();
        }

        return m_hash;
    }

    private String getStyle()
    {
        final String style =  "<style>" +
                                   "body { background-color: white; }" +
                                   "td.label { background-color: #dadada; color: black; font-weight: bold; height: 24px; }" +
                                   "td.labelRequired { background-color: #dadada; color: #990000; font-weight: bold; height: 24px; ; font-style: italic }" +
                                   "td.inputField { background-color: #eeeeee; font-size: 12px; }" +
                                   "td.requiredNotice {  font-family: Arial, Helvetica, sans-serif; color: #990000}" +
                                   "td.heading1 { font-size: 10pt; font-weight: bold; border-top: 1px solid #003366;  height: 24px;}" +
                                   "td.heading2 { font-size: 8pt; font-weight: bold; background-color: #dddddd;  height: 24px;}" +
                                   "</style>";

        return style;
    }
}
