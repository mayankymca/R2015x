
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import com.matrixone.apps.domain.util.eMatrixDateFormat;

public class emxDashboardChangesBase_mxJPO {

    String sColorState1 = "6699BB";
    String sColorState2 = "cc0000";
    String sColorState4 = "009c00";
    String sColorState3 = "ff7f00";
    String sColorState5 = "aab8be";

    SimpleDateFormat sdf    = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat());

    public emxDashboardChangesBase_mxJPO(Context context, String[] args) throws Exception {}

    // Changes Dashboard
    public String[] getChangesDashboardData(Context context, String[] args) throws Exception {


        String[] sColors        = {"329cee","f6bd0f","8BBA00","ec0c41","752fc3","AFD8F8","fad46c","c9ff0d","F984A1","A66EDD"};
        String[] aResults       = new String[50];
        HashMap paramMap        = (HashMap) JPO.unpackArgs(args);
        String sLanguage        = (String)paramMap.get("languageStr");
        String sFilterGlobal    = (String)paramMap.get("filterGlobal");
        String sChangeTypes     = (String) paramMap.get("changeTypes");
        Calendar cNow           = Calendar.getInstance();
        Calendar cRecent        = Calendar.getInstance();

        cRecent.add(java.util.GregorianCalendar.DAY_OF_YEAR,-30);
        int iYearNow    = cNow.get(Calendar.YEAR);
        int iMonthNow   = cNow.get(Calendar.MONTH);
        int iWeekNow    = cNow.get(Calendar.WEEK_OF_YEAR);
        int iDayNow     = cNow.get(Calendar.DAY_OF_YEAR);

        String sLabelState1         = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.Evaluation" , sLanguage);
        String sLabelState2         = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.Implementation" , sLanguage);
        String sLabelState3         = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.Validation" , sLanguage);
        String sLabelState4         = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.State.Engineering_Change_(Standard).Complete" , sLanguage);
        String sLabelState5         = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.State.Issue.Closed" , sLanguage);
        String sLabelPriorityLow    = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Range.Severity.Low" , sLanguage);
        String sLabelPriorityMedium = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Range.Severity.Medium" , sLanguage);
        String sLabelPriorityHigh   = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Range.Severity.High" , sLanguage);

        StringBuilder sbChanges         = new StringBuilder();
        StringBuilder sbDataCategories  = new StringBuilder();
        StringBuilder sbAxisCategories  = new StringBuilder();
        StringBuilder sbDataStatus      = new StringBuilder();
        StringBuilder sbDataSeverity    = new StringBuilder();
        StringBuilder sbUpdates         = new StringBuilder();
        StringBuilder sbAxisDuration    = new StringBuilder();
        StringBuilder sbDataDuration    = new StringBuilder();
        StringBuilder sbAxisTimeline    = new StringBuilder();
        StringBuilder sbDataTimeline    = new StringBuilder();

        java.util.List<String> lCategories      = new ArrayList<String>();
        java.util.List<String> lTimeline        = new ArrayList<String>();
        java.util.List<Integer> lWeeksMonths    = new ArrayList<Integer>();
        java.util.List<Integer> lYears          = new ArrayList<Integer>();

        MapList mlCategories    = new MapList();
        int[] iCountStatus      = new int[5];
        int[] iCountSeverity    = new int[5];
        int[] iCountUpdates     = new int[4];

        StringList busSelects = new StringList();
        busSelects.add(DomainConstants.SELECT_ID);
        busSelects.add(DomainConstants.SELECT_TYPE);
        busSelects.add(DomainConstants.SELECT_ORIGINATED);
        busSelects.add(DomainConstants.SELECT_CURRENT);
        busSelects.add(DomainConstants.SELECT_MODIFIED);
        busSelects.add("attribute["+ DomainConstants.ATTRIBUTE_CATEGORY_OF_CHANGE+ "]");
        busSelects.add("attribute["+ DomainConstants.ATTRIBUTE_SEVERITY  + "]");
        busSelects.add("state[Submit].duration");
        busSelects.add("state[Evaluate].duration");
        busSelects.add("state[Review].duration");
        busSelects.add("state[Approved].duration");
        busSelects.add("state[Implement].duration");
        busSelects.add("state[Validate].duration");
        busSelects.add("state[Formal Approval].duration");
        busSelects.add("state[Submit].start");
        busSelects.add("state[Approved].start");
        busSelects.add("state[Validate].start");
        busSelects.add("state[Complete].start");
        busSelects.add("state[Close].start");

        // ECO states
        busSelects.add("state[Define Components].duration");
        busSelects.add("state[Design Work].duration");
        busSelects.add("state[Review].duration");
        busSelects.add("state[Release].duration");
        busSelects.add("state[Implemented].duration");
        busSelects.add("state[Define Components].start");
        busSelects.add("state[Design Work].start");
        busSelects.add("state[Review].start");
        busSelects.add("state[Release].start");
        busSelects.add("state[Implemented].start");

        MapList mlChanges = retrievChangesPending(context, args, busSelects, sFilterGlobal, "", sChangeTypes);

        if(mlChanges.size() > 0) {


            // Panel Header
            String sPrefix = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.ThereAre" , sLanguage);
            String sSuffix = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.ChangesMatching" , sLanguage);
            if(mlChanges.size() == 1) { sPrefix = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.ThereIs" , sLanguage); }
            if(mlChanges.size() == 1) { sSuffix = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.ChangeMatching" , sLanguage); }

            sbChanges.append(sPrefix);
            sbChanges.append(" <span style='font-weight:bold;color:#000;'>").append(mlChanges.size()).append("</span> ");
            sbChanges.append(sSuffix);

            BigDecimal[][] bdDurations = new BigDecimal[3][3];
            for(int i = 0; i < 3; i++) { for(int j = 0; j < 3; j++) { bdDurations[i][j] = new BigDecimal("0"); }}


            // Get Timeline Information
            mlChanges.sort("originated", "ascending", "date");
            Map mFirst                  = (Map)mlChanges.get(0);
            Boolean bUseMonths          = false;
            String sDateStart           = (String)mFirst.get(DomainConstants.SELECT_ORIGINATED);
            java.util.Calendar cWeeks   = java.util.Calendar.getInstance();
            java.util.Calendar cStart   = java.util.Calendar.getInstance();
            java.util.Calendar cDate    = java.util.Calendar.getInstance();
            java.util.Calendar cStart1  = java.util.Calendar.getInstance();
            java.util.Calendar cStart2  = java.util.Calendar.getInstance();
            java.util.Calendar cStart3  = java.util.Calendar.getInstance();
            java.util.Calendar cStart4  = java.util.Calendar.getInstance();
            java.util.Calendar cStart5  = java.util.Calendar.getInstance();

            cDate.setTime(sdf.parse(sDateStart));
            cStart.setTime(sdf.parse(sDateStart));
            cWeeks.add(java.util.Calendar.DAY_OF_YEAR, -110);

            if(cWeeks.after(cStart)) {
                bUseMonths = true;
                cDate.setTime(sdf.parse(sDateStart));
                do  {
                    int iMonth = cDate.get(java.util.Calendar.MONTH) + 1;
                    int iYear = cDate.get(java.util.Calendar.YEAR);
                    String sTemp = iMonth + "/" + iYear;
                    lTimeline.add(sTemp);
                    lWeeksMonths.add(iMonth);
                    lYears.add(iYear);
                    cDate.add(java.util.Calendar.MONTH, 1);
                } while (cDate.before(cNow));
            } else {
                do  {
                    int iWeek = cDate.get(java.util.Calendar.WEEK_OF_YEAR);
                    int iYear = cDate.get(java.util.Calendar.YEAR);
                    lWeeksMonths.add(iWeek);
                    lYears.add(iYear);//
                    String sTemp = iWeek + "/" + iYear;
                    lTimeline.add(sTemp);
                    cDate.add(java.util.Calendar.DAY_OF_YEAR, 7);
                } while (cDate.before(cNow));
            }

            int[][] iCountersTimeline = new int[lWeeksMonths.size()][5];
            for(int i = 0; i < lWeeksMonths.size(); i++) { for(int j = 0; j < 5; j++) { iCountersTimeline[i][j] = 0; }}

            for(int i = 0; i < mlChanges.size(); i++) {

                Map mChange         = (Map)mlChanges.get(i);
                String sType        = (String)mChange.get(DomainConstants.SELECT_TYPE);
                String sCategory    = (String)mChange.get("attribute["+ DomainConstants.ATTRIBUTE_CATEGORY_OF_CHANGE+ "]");
                String sCurrent     = (String)mChange.get(DomainConstants.SELECT_CURRENT);
                String sSeverity    = (String)mChange.get("attribute["+ DomainConstants.ATTRIBUTE_SEVERITY  + "]");
                String sModified    = (String)mChange.get(DomainConstants.SELECT_MODIFIED);
                String sDuration1   = (String)mChange.get("state[Submit].duration");
                String sDuration2   = (String)mChange.get("state[Evaluate].duration");
                String sDuration3   = (String)mChange.get("state[Review].duration");
                String sDuration4   = (String)mChange.get("state[Approved].duration");
                String sDuration5   = (String)mChange.get("state[Implement].duration");
                String sDuration6   = (String)mChange.get("state[Validate].duration");
                String sDuration7   = (String)mChange.get("state[Formal Approval].duration");
                String sStart1      = (String)mChange.get("state[Submit].start");
                String sStart2      = (String)mChange.get("state[Approved].start");
                String sStart3      = (String)mChange.get("state[Validate].start");
                String sStart4      = (String)mChange.get("state[Complete].start");
                String sStart5      = (String)mChange.get("state[Close].start");
                int iSeverity       = 0;

                if(lCategories.indexOf(sCategory) == -1) {
                    lCategories.add(sCategory);
                    String sLabel = EnoviaResourceBundle.getRangeI18NString(context, "Category of Change", sCategory, sLanguage);
                    Map mCategory = new HashMap();
                    mCategory.put("name", sCategory);
                    mCategory.put("label", sLabel);
                    mCategory.put("count", "1");
                    mlCategories.add(mCategory);

                } else {
                    Map mCategory = (Map)mlCategories.get(lCategories.indexOf(sCategory));
                    Integer iCount = Integer.parseInt((String)mCategory.get("count"));
                    iCount ++;
                    mCategory.put("count", String.valueOf(iCount));
                }

                if(sCurrent.equals("Submit"))               { iCountStatus[0]++; }
                else if(sCurrent.equals("Evaluate"))        { iCountStatus[0]++; }
                else if(sCurrent.equals("Review"))          { iCountStatus[0]++; }
                else if(sCurrent.equals("Approved"))        { iCountStatus[1]++; }
                else if(sCurrent.equals("Implement"))       { iCountStatus[1]++; }
                else if(sCurrent.equals("Validate"))        { iCountStatus[2]++; }
                else if(sCurrent.equals("Formal Approval")) { iCountStatus[2]++; }
                else if(sCurrent.equals("Complete"))        { iCountStatus[3]++; }
                else if(sCurrent.equals("Close"))           { iCountStatus[4]++; }

                // ECO states
                else if(sCurrent.equals("Define Components"))   { iCountStatus[0]++; }
                else if(sCurrent.equals("Design Work"))         { iCountStatus[1]++; }
                else if(sCurrent.equals("Review"))              { iCountStatus[2]++; }
                else if(sCurrent.equals("Released"))            { iCountStatus[3]++; }
                else if(sCurrent.equals("Implemented"))         { iCountStatus[4]++; }

                if(sSeverity.equals("Low"))         { iCountSeverity[0]++; iSeverity = 0; }
                else if(sSeverity.equals("Medium")) { iCountSeverity[1]++; iSeverity = 1; }
                else if(sSeverity.equals("High"))   { iCountSeverity[2]++; iSeverity = 2; }

                Calendar cUpdate = Calendar.getInstance();
                cUpdate.setTime(sdf.parse(sModified));

                if(cUpdate.after(cRecent)) {

                    iCountUpdates[3]++;

                    int iYear    = cUpdate.get(Calendar.YEAR);
                    int iMonth   = cUpdate.get(Calendar.MONTH);
                    int iWeek    = cUpdate.get(Calendar.WEEK_OF_YEAR);
                    int iDay     = cUpdate.get(Calendar.DAY_OF_YEAR);

                    if(iYear == iYearNow) {
                        if(iMonth == iMonthNow) { iCountUpdates[2]++; }
                        if(iWeek == iWeekNow)   { iCountUpdates[1]++; }
                        if(iDay == iDayNow)     { iCountUpdates[0]++; }
                    }

                }

                BigDecimal bdDuration1  = new BigDecimal("0");
                BigDecimal bdDuration2  = new BigDecimal("0");
                BigDecimal bdDuration3  = new BigDecimal("0");
                BigDecimal bdDuration4  = new BigDecimal("0");
                BigDecimal bdDuration5  = new BigDecimal("0");
                BigDecimal bdDuration6  = new BigDecimal("0");
                BigDecimal bdDuration7  = new BigDecimal("0");

                if(sType.equals("ECO")) {
                    sDuration1 = (String)mChange.get("state[Define Components].duration");
                    sDuration2 = "0";
                    sDuration3 = "0";
                    sDuration4 = (String)mChange.get("state[Design Work].duration");
                    sDuration5 = "0";
                    sDuration6 = (String)mChange.get("state[Review].duration");
                    sDuration7 = "0";
                    sStart1    = (String)mChange.get("state[Define Components].start");
                    sStart2    = (String)mChange.get("state[Design Work].start");
                    sStart3    = (String)mChange.get("state[Review].start");
                    sStart4    = (String)mChange.get("state[Release].start");
                    sStart5    = (String)mChange.get("state[Implemented].start");
                }

                if(!"".equals(sDuration1)) { bdDuration1 = new BigDecimal(sDuration1); }
                if(!"".equals(sDuration2)) { bdDuration2 = new BigDecimal(sDuration2); }
                if(!"".equals(sDuration3)) { bdDuration3 = new BigDecimal(sDuration3); }
                if(!"".equals(sDuration4)) { bdDuration4 = new BigDecimal(sDuration4); }
                if(!"".equals(sDuration5)) { bdDuration5 = new BigDecimal(sDuration5); }
                if(!"".equals(sDuration6)) { bdDuration6 = new BigDecimal(sDuration6); }
                if(!"".equals(sDuration7)) { bdDuration7 = new BigDecimal(sDuration7); }

                bdDurations[0][iSeverity] = bdDurations[0][iSeverity].add(bdDuration1).add(bdDuration2).add(bdDuration3);
                bdDurations[1][iSeverity] = bdDurations[1][iSeverity].add(bdDuration4).add(bdDuration5);
                bdDurations[2][iSeverity] = bdDurations[2][iSeverity].add(bdDuration6).add(bdDuration7);

                for(int j = 0; j < lTimeline.size(); j++) {

                    int iYearReference          = lYears.get(j);
                    int iWeekMonthReference     = lWeeksMonths.get(j);
                    boolean bFound              = false;

                    if(!sStart5.equals("")) {
                        cStart5.setTime(sdf.parse(sStart5));
                        int iYearStart = cStart5.get(java.util.Calendar.YEAR);
                        int iMonthStart = cStart5.get(java.util.Calendar.WEEK_OF_YEAR);
                        if(bUseMonths) { iMonthStart = cStart5.get(java.util.Calendar.MONTH) + 1; }
                        if(iYearStart == iYearReference) {
                            if(iMonthStart <= iWeekMonthReference) {
                                bFound = true;
                                iCountersTimeline[j][4]++;
                            }
                        } else if(iYearStart <= iYearReference) {
                            bFound = true;
                            iCountersTimeline[j][4]++;
                        }
                    }

                    if(!sStart4.equals("") && (bFound == false)) {
                        cStart4.setTime(sdf.parse(sStart4));
                        int iYearStart = cStart4.get(java.util.Calendar.YEAR);
                        int iMonthStart = cStart4.get(java.util.Calendar.WEEK_OF_YEAR);
                        if(bUseMonths) { iMonthStart = cStart4.get(java.util.Calendar.MONTH) + 1; }
                        if(iYearStart == iYearReference) {
                            if(iMonthStart <= iWeekMonthReference) {
                                bFound = true;
                                iCountersTimeline[j][3]++;
                            }
                        } else if(iYearStart <= iYearReference) {
                            bFound = true;
                            iCountersTimeline[j][3]++;
                        }
                    }

                    if(!sStart3.equals("") && (bFound == false)) {
                        cStart3.setTime(sdf.parse(sStart3));
                        int iYearStart = cStart3.get(java.util.Calendar.YEAR);
                        int iMonthStart = cStart3.get(java.util.Calendar.WEEK_OF_YEAR);
                        if(bUseMonths) { iMonthStart = cStart3.get(java.util.Calendar.MONTH) + 1; }
                        if(iYearStart == iYearReference) {
                            if(iMonthStart <= iWeekMonthReference) {
                                bFound = true;
                                iCountersTimeline[j][2]++;
                            }
                        } else if(iYearStart <= iYearReference) {
                            bFound = true;
                            iCountersTimeline[j][2]++;
                        }
                    }

                    if(!sStart2.equals("") && (bFound == false)) {
                        cStart2.setTime(sdf.parse(sStart2));
                        int iYearStart = cStart2.get(java.util.Calendar.YEAR);
                        int iMonthStart = cStart2.get(java.util.Calendar.WEEK_OF_YEAR);
                        if(bUseMonths) { iMonthStart = cStart2.get(java.util.Calendar.MONTH) + 1; }
                        if(iYearStart == iYearReference) {
                            if(iMonthStart <= iWeekMonthReference) {
                                bFound = true;
                                iCountersTimeline[j][1]++;
                            }
                        } else if(iYearStart <= iYearReference) {
                            bFound = true;
                            iCountersTimeline[j][1]++;
                        }
                    }

                    if(!sStart1.equals("") && (bFound == false)) {
                        cStart1.setTime(sdf.parse(sStart1));
                        int iYearStart = cStart1.get(java.util.Calendar.YEAR);
                        int iMonthStart = cStart1.get(java.util.Calendar.WEEK_OF_YEAR);
                        if(bUseMonths) { iMonthStart = cStart1.get(java.util.Calendar.MONTH) + 1; }
                        if(iYearStart == iYearReference) {
                            if(iMonthStart <= iWeekMonthReference) {
                                iCountersTimeline[j][0]++;
                            }
                        } else if(iYearStart <= iYearReference) {
                            iCountersTimeline[j][0]++;
                        }
                    }

                }


            }

            BigDecimal bdDiv = new BigDecimal("86400");

            for(int i = 0; i < 3; i++) {
                for(int j = 0; j < 3; j++) {
                    if(bdDurations[i][j] != new BigDecimal("0")) {
                        if(iCountSeverity[j] != 0) {
                            bdDurations[i][j] = bdDurations[i][j].divide(new BigDecimal(iCountSeverity[j]), 0, RoundingMode.UP);
                            bdDurations[i][j] = bdDurations[i][j].divide(bdDiv, 0, RoundingMode.UP);
                        }
                    }
                }
            }


            sbDataDuration.append("{name : \"").append(sLabelState1).append("\", data : [").append(bdDurations[0][2]).append(",").append(bdDurations[0][1]).append(",").append(bdDurations[0][0]).append("], color: '#").append(sColorState3).append("'},");
            sbDataDuration.append("{name : \"").append(sLabelState2).append("\", data : [").append(bdDurations[1][2]).append(",").append(bdDurations[1][1]).append(",").append(bdDurations[1][0]).append("], color: '#").append(sColorState2).append("'},");
            sbDataDuration.append("{name : \"").append(sLabelState3).append("\", data : [").append(bdDurations[2][2]).append(",").append(bdDurations[2][1]).append(",").append(bdDurations[2][0]).append("], color: '#").append(sColorState1).append("'}");

            sbAxisDuration.append("'").append(sLabelPriorityHigh).append("',");
            sbAxisDuration.append("'").append(sLabelPriorityMedium).append("',");
            sbAxisDuration.append("'").append(sLabelPriorityLow).append("'");

            for (int i = 0; i < lTimeline.size(); i++) { sbAxisTimeline.append("'").append(lTimeline.get(i)).append("',"); }

            StringBuilder sbDataTimeline1 = new StringBuilder();
            StringBuilder sbDataTimeline2 = new StringBuilder();
            StringBuilder sbDataTimeline3 = new StringBuilder();
            StringBuilder sbDataTimeline4 = new StringBuilder();
            StringBuilder sbDataTimeline5 = new StringBuilder();


                for (int i = 0; i < lTimeline.size(); i++) {
                    sbDataTimeline1.append(iCountersTimeline[i][0]).append(",");
                    sbDataTimeline2.append(iCountersTimeline[i][1]).append(",");
                    sbDataTimeline3.append(iCountersTimeline[i][2]).append(",");
                    sbDataTimeline4.append(iCountersTimeline[i][3]).append(",");
                    sbDataTimeline5.append(iCountersTimeline[i][4]).append(",");
                }
            

            if (sbDataTimeline1.length() > 0 ) { sbDataTimeline1.setLength(sbDataTimeline1.length() - 1); }
            if (sbDataTimeline2.length() > 0 ) { sbDataTimeline2.setLength(sbDataTimeline2.length() - 1); }
            if (sbDataTimeline3.length() > 0 ) { sbDataTimeline3.setLength(sbDataTimeline3.length() - 1); }
            if (sbDataTimeline4.length() > 0 ) { sbDataTimeline4.setLength(sbDataTimeline4.length() - 1); }
            if (sbDataTimeline5.length() > 0 ) { sbDataTimeline5.setLength(sbDataTimeline5.length() - 1); }

            sbDataTimeline.append("{name : \"").append(sLabelState1).append("\", data : [").append(sbDataTimeline1.toString()).append("], color: '#").append(sColorState1).append("'},");
            sbDataTimeline.append("{name : \"").append(sLabelState2).append("\", data : [").append(sbDataTimeline2.toString()).append("], color: '#").append(sColorState2).append("'},");
            sbDataTimeline.append("{name : \"").append(sLabelState3).append("\", data : [").append(sbDataTimeline3.toString()).append("], color: '#").append(sColorState3).append("'},");
            sbDataTimeline.append("{name : \"").append(sLabelState4).append("\", data : [").append(sbDataTimeline4.toString()).append("], color: '#").append(sColorState4).append("'},");
            sbDataTimeline.append("{name : \"").append(sLabelState5).append("\", data : [").append(sbDataTimeline5.toString()).append("], color: '#").append(sColorState5).append("'}");

        }

        mlCategories.sort("count", "descending", "integer");

        for(int i = 0; i < mlCategories.size(); i++) {
            Map mCategory = (Map)mlCategories.get(i);
            sbAxisCategories.append("\"").append((String)mCategory.get("label")).append("\",");
            sbDataCategories.append("{ name:\"").append((String)mCategory.get("name")).append("\", color:'#").append(sColors[i%sColors.length]).append("', y:").append((String)mCategory.get("count")).append("},");
        }

        sbDataStatus.append("{ name:\"").append(sLabelState1).append("\", color:'#").append(sColorState1).append("', y:").append(iCountStatus[0]).append(", filter:'Submit,Evaluate,Review,Define Components'},");
        sbDataStatus.append("{ name:\"").append(sLabelState2).append("\", color:'#").append(sColorState2).append("', y:").append(iCountStatus[1]).append(", filter:'Approved,Implement,Design Work'},");
        sbDataStatus.append("{ name:\"").append(sLabelState3).append("\", color:'#").append(sColorState3).append("', y:").append(iCountStatus[2]).append(", filter:'Validate,Formal Approval,Review'}");
        if(iCountStatus[3] > 0) {
            sbDataStatus.append(",{ name:\"").append(sLabelState4).append("\", color:'#").append(sColorState4).append("', y:").append(iCountStatus[3]).append(", filter:'Complete,Release'}");
        }

        sbDataSeverity.append("{ name:\"").append(sLabelPriorityLow).append("\", color:'#").append(sColorState4).append("', y:").append(iCountSeverity[0]).append(", filter:'Low'},");
        sbDataSeverity.append("{ name:\"").append(sLabelPriorityMedium).append("\", color:'#").append(sColorState3).append("', y:").append(iCountSeverity[1]).append(", filter:'Medium'},");
        sbDataSeverity.append("{ name:\"").append(sLabelPriorityHigh).append("\", color:'#").append(sColorState2).append("', y:").append(iCountSeverity[2]).append(", filter:'High'}");


        String sURLPrefix = "onclick='openURLInDetails(\"../common/emxIndentedTable.jsp?changeTypes=" + sChangeTypes + "&filterGlobal=" + sFilterGlobal + "&table=APPDashboardEC&program=emxDashboardChanges:getChangesDashboardItems&&freezePane=Name,NewWindow&suiteKey=Framework&filterRange=";

        String sLabelLatestUpdate   = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.LatestUpdates" , sLanguage);
        String sLabelToday          = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.Today" , sLanguage);
        String sLabelThisWeek       = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.ThisWeek" , sLanguage);
        String sLabelThisMonth      = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.ThisMonth" , sLanguage);
        String sLabelLast30Days     = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.Last30Days" , sLanguage);

        sbUpdates.append("<table style='width:98%;'><tr>");
        sbUpdates.append("<td class='counterCell red'    style='width:25%;'").append(sURLPrefix).append("1&header=").append(sLabelLatestUpdate).append(" : ").append(sLabelToday).append("\")'").append("><span class=\"counterText red\">").append(iCountUpdates[0]).append("</span><br/>").append(sLabelToday).append("</td>");
        sbUpdates.append("<td class='counterCell yellow' style='width:25%;'").append(sURLPrefix).append("2&header=").append(sLabelLatestUpdate).append(" : ").append(sLabelThisWeek).append("\")'").append("><span class=\"counterText yellow\">").append(iCountUpdates[1]).append("</span><br/>").append(sLabelThisWeek).append("</td>");
        sbUpdates.append("<td class='counterCell green'  style='width:25%;'").append(sURLPrefix).append("3&header=").append(sLabelLatestUpdate).append(" : ").append(sLabelThisMonth).append("\")'").append("><span class=\"counterText green\">").append(iCountUpdates[2]).append("</span><br/>").append(sLabelThisMonth).append("</td>");
        sbUpdates.append("<td class='counterCell gray'   style='width:25%;'").append(sURLPrefix).append("4&header=").append(sLabelLatestUpdate).append(" : ").append(sLabelLast30Days).append("\")'").append("><span class=\"counterText gray\">").append(iCountUpdates[3]).append("</span><br/>").append(sLabelLast30Days).append("</td>");
        sbUpdates.append("</tr></table>");


        // Remove trailing commas
        if(sbAxisCategories.length() > 0) { sbAxisCategories.setLength(sbAxisCategories.length() - 1); }
        if(sbDataCategories.length() > 0) { sbDataCategories.setLength(sbDataCategories.length() - 1); }
        if(sbAxisTimeline.length() > 0) { sbAxisTimeline.setLength(sbAxisTimeline.length() - 1); }



        // Output
        aResults[0]  = sbChanges.toString();
        aResults[1]  = sbAxisCategories.toString();
        aResults[2]  = sbDataCategories.toString();
        aResults[3]  = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Attribute.Category_of_Change" , sLanguage);
        aResults[4]  = String.valueOf(25 + (mlCategories.size() * 25));
        aResults[5]  = sbDataStatus.toString();
        aResults[6]  = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Basic.Current" , sLanguage);
        aResults[7]  = sbDataSeverity.toString();
        aResults[8]  = EnoviaResourceBundle.getProperty(context, "Components", "emxFramework.Attribute.Severity" , sLanguage);
        aResults[9]  = sbUpdates.toString();
        aResults[10] = sLabelLatestUpdate;
        aResults[11] = sbAxisDuration.toString();
        aResults[12] = sbDataDuration.toString();
        aResults[13] = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.DurationByState" , sLanguage);
        aResults[14] = sbAxisTimeline.toString();
        aResults[15] = sbDataTimeline.toString();
        aResults[16] = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.Timeline" , sLanguage);
        aResults[40] = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.Filter" , sLanguage);

        return aResults;

    }
    public MapList retrievChangesPending(Context context, String[] args, StringList busSelects, String sFilterGlobal, String sFilter, String sTypes) throws FrameworkException {

        String sWhere = "(current != 'Close') && (current != 'Reject') && (current != 'Cancelled')";

        if(sFilterGlobal.equals("MineInWork"))      { sWhere += " && (current != 'Complete') && (owner =='" + context.getUser() + "')"; }
        else if(sFilterGlobal.equals("AllInWork"))  { sWhere += " && (current != 'Complete')"; }
        else if(sFilterGlobal.equals("MineAll"))    { sWhere += " && (owner =='" + context.getUser() + "')"; }

        if(!sFilter.equals("")) { sWhere += " && " + sFilter; }

        return DomainObject.findObjects(context, sTypes, context.getVault().getName(), sWhere, busSelects);

    }
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getChangesDashboardItems(Context context, String[] args) throws Exception {

        HashMap paramMap        = (HashMap) JPO.unpackArgs(args);
        String sChangeTypes     = (String) paramMap.get("changeTypes");
        String sFilterGlobal    = (String) paramMap.get("filterGlobal");
        String sFilterRange     = (String) paramMap.get("filterRange");
        String sFilterCategory  = (String) paramMap.get("filterCategory");
        String sFilterStatus    = (String) paramMap.get("filterStatus");
        String sFilterSeverity  = (String) paramMap.get("filterSeverity");
        String sFilter          = "";

        StringList busSelects = new StringList();
        busSelects.add("id");

        if(null != sFilterRange) {

            busSelects.add("modified");

            Calendar cNow = Calendar.getInstance();

            if(sFilterRange.equals("2")) {
                cNow.set(Calendar.DAY_OF_WEEK, cNow.getFirstDayOfWeek());
            } else if(sFilterRange.equals("3")) {
                cNow.add(java.util.GregorianCalendar.DAY_OF_YEAR, - cNow.get(Calendar.DAY_OF_MONTH) + 1);
            } else if(sFilterRange.equals("4")) {
                cNow.add(java.util.GregorianCalendar.DAY_OF_YEAR, -30);
            }

            sFilter = "modified >= \"" + (cNow.get(Calendar.MONTH) + 1) + "/" + cNow.get(Calendar.DAY_OF_MONTH) + "/" + cNow.get(Calendar.YEAR) + "\"";

        }

        if(null != sFilterCategory) {
            busSelects.add("attribute["+ DomainConstants.ATTRIBUTE_CATEGORY_OF_CHANGE+ "]");
            sFilter = "attribute["+ DomainConstants.ATTRIBUTE_CATEGORY_OF_CHANGE+ "] == '" + sFilterCategory + "'";
        }

        if(null != sFilterStatus) {
            sFilter = "(current smatchlist '" + sFilterStatus + "' ',')";
        }

        if(null != sFilterSeverity) {
            sFilter = "(attribute["+ DomainConstants.ATTRIBUTE_SEVERITY  + "] == '" + sFilterSeverity + "')";
        }

        return retrievChangesPending(context, args, busSelects, sFilterGlobal, sFilter, sChangeTypes);

    }


    // My Dashboard
    public String[] getUserDashboardData(Context context, String[] args) throws Exception {

        String[] sColors        = {"329cee","f6bd0f","8BBA00","ec0c41","752fc3","AFD8F8","fad46c","c9ff0d","F984A1","A66EDD"};
        String[] aResults       = new String[10];
        HashMap paramMap        = (HashMap) JPO.unpackArgs(args);
        String sOID             = (String)paramMap.get("objectId");
        String sLanguage        = (String)paramMap.get("languageStr");
        Integer[][] iCounters   = new Integer[3][7];
        int iCountTotal         = 0;
        int iCountMRU           = 0;
        Calendar cMRU           = Calendar.getInstance();

        cMRU.add(java.util.GregorianCalendar.DAY_OF_YEAR, -1);

        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 7; j++) {
                iCounters[i][j] = 0;
            }
        }

        StringBuilder sbAxisChanges = new StringBuilder();
        StringBuilder sbDataChanges = new StringBuilder();

        sbAxisChanges.append("'").append(EnoviaResourceBundle.getStateI18NString(context, "Engineering Change (Standard)", "Submit"           , sLanguage)).append("',");
        sbAxisChanges.append("'").append(EnoviaResourceBundle.getStateI18NString(context, "Engineering Change (Standard)", "Evaluate"         , sLanguage)).append("',");
        sbAxisChanges.append("'").append(EnoviaResourceBundle.getStateI18NString(context, "Engineering Change (Standard)", "Review"           , sLanguage)).append("',");
        sbAxisChanges.append("'").append(EnoviaResourceBundle.getStateI18NString(context, "Engineering Change (Standard)", "Approved"         , sLanguage)).append("',");
        sbAxisChanges.append("'").append(EnoviaResourceBundle.getStateI18NString(context, "Engineering Change (Standard)", "Implement"        , sLanguage)).append("',");
        sbAxisChanges.append("'").append(EnoviaResourceBundle.getStateI18NString(context, "Engineering Change (Standard)", "Validate"         , sLanguage)).append("',");
        sbAxisChanges.append("'").append(EnoviaResourceBundle.getStateI18NString(context, "Engineering Change (Standard)", "Formal Approval"  , sLanguage)).append("' ");

        com.matrixone.apps.common.Person pUser = com.matrixone.apps.common.Person.getPerson( context );

        StringList busSelects = new StringList();
        busSelects.add(DomainConstants.SELECT_ID);
        busSelects.add(DomainConstants.SELECT_NAME);
        busSelects.add(DomainConstants.SELECT_CURRENT);
        busSelects.add(DomainConstants.SELECT_MODIFIED);
        busSelects.add("attribute["+ DomainConstants.ATTRIBUTE_SEVERITY  + "]");

        MapList mlChanges = pUser.getRelatedObjects(context, DomainConstants.RELATIONSHIP_ASSIGNED_EC, DomainConstants.TYPE_ENGINEERING_CHANGE, busSelects, null, false, true, (short)1, "(current != 'Complete') && (current != 'Close') && (current != 'Reject')", "", 0);

        for(int i = 0; i < mlChanges.size(); i++) {

            Map mChange         = (Map)mlChanges.get(i);
            String sCurrent     = (String)mChange.get(DomainConstants.SELECT_CURRENT);
            String sModified    = (String)mChange.get(DomainConstants.SELECT_MODIFIED);
            String sSeverity    = (String)mChange.get("attribute["+ DomainConstants.ATTRIBUTE_SEVERITY  + "]");
            int iStatus         = 0;
            int iSeverity       = 0;

            Calendar cModified    = Calendar.getInstance();
            cModified.setTime(sdf.parse(sModified));
            if(cModified.after(cMRU)) { iCountMRU++; }

            if(sSeverity.equals("Medium"))      { iSeverity = 1; }
            else if(sSeverity.equals("High"))   { iSeverity = 2; }

            if(sCurrent.equals("Evaluate"))             { iStatus = 1; }
            else if(sCurrent.equals("Review"))          { iStatus = 2; }
            else if(sCurrent.equals("Approved"))        { iStatus = 3; }
            else if(sCurrent.equals("Implement"))       { iStatus = 4; }
            else if(sCurrent.equals("Validate"))        { iStatus = 5; }
            else if(sCurrent.equals("Formal Approval")) { iStatus = 6; }

            iCounters[iSeverity][iStatus]++;

        }

        iCountTotal = mlChanges.size();

 

        sbDataChanges.append("{ color:'#009c00', name: \"").append(EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Range.Severity.Low", sLanguage)).append("\", ");
        sbDataChanges.append(" data: [").append(iCounters[0][0]).append(",").append(iCounters[0][1]).append(",").append(iCounters[0][2]).append(",").append(iCounters[0][3]).append(",").append(iCounters[0][4]).append(",").append(iCounters[0][5]).append(",").append(iCounters[0][6]).append("] },");
        sbDataChanges.append("{ color:'#ff7f00', name: \"").append(EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Range.Severity.Medium", sLanguage)).append("\", ");
        sbDataChanges.append(" data: [").append(iCounters[1][0]).append(",").append(iCounters[1][1]).append(",").append(iCounters[1][2]).append(",").append(iCounters[1][3]).append(",").append(iCounters[1][4]).append(",").append(iCounters[1][5]).append(",").append(iCounters[1][6]).append("] },");
        sbDataChanges.append("{ color:'#cc0000', name: \"").append(EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Range.Severity.High", sLanguage)).append("\", ");
        sbDataChanges.append(" data: [").append(iCounters[2][0]).append(",").append(iCounters[2][1]).append(",").append(iCounters[2][2]).append(",").append(iCounters[2][3]).append(",").append(iCounters[2][4]).append(",").append(iCounters[2][5]).append(",").append(iCounters[2][6]).append("] }");

        StringBuilder sbCounter = new StringBuilder();
        sbCounter.append("<td onclick='openURLInDetails(\"../common/emxIndentedTable.jsp?table=APPDashboardEC&program=emxDashboardChanges:getChangesAssignedPending&header=emxFramework.String.AssignedChangesPending&freezePane=Name,NewWindow&suiteKey=Framework\")'");
        sbCounter.append(" class='counterCell ");
        if(iCountTotal == 0)  { sbCounter.append("grayBright"); }
        else                       { sbCounter.append("' style='color:#00B2A9;");  }
        sbCounter.append("'><span class='counterText blue'>").append(iCountTotal).append("</span><br/>");
        sbCounter.append(EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.Changes", sLanguage)).append("</td>");

        StringBuilder sbUpdates = new StringBuilder();
        sbUpdates.append("<td ");
        if(iCountMRU > 0) {
            sbUpdates.append(" onclick='openURLInDetails(\"../common/emxIndentedTable.jsp?table=APPECList&program=emxDashboardChanges:getChangesAssignedPending&mode=MRU&header=emxFramework.String.MRUChanges&freezePane=Name,NewWindow&suiteKey=Framework\")' ");
            sbUpdates.append(" class='mruCell'><span style='color:#000000;font-weight:bold;'>").append(iCountMRU).append("</span> <span class='counterTextMRU'>");
            if(iCountMRU == 1) { sbUpdates.append(EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.MostRecentUpdate"  , sLanguage)); }
            else               { sbUpdates.append(EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.MostRecentUpdates" , sLanguage)); }
            sbUpdates.append("</span>");
        } else {
            sbUpdates.append(">");
        }
        sbUpdates.append("</td>");


        aResults[0] = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.String.AssignedChangesPending", sLanguage);
        aResults[1] = sbAxisChanges.toString();
        aResults[2] = sbDataChanges.toString();
        aResults[3] = sbCounter.toString();
        aResults[4] = sbUpdates.toString();

        return aResults;

    }
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getChangesAssignedPending(Context context, String[] args) throws Exception {

        Map programMap          = (Map) JPO.unpackArgs(args);
        String sMode            = (String) programMap.get("mode");
        StringBuilder sbWhere   = new StringBuilder();

        if(null == sMode) { sMode = ""; }
        com.matrixone.apps.common.Person pUser = com.matrixone.apps.common.Person.getPerson( context );

        StringList busSelects = new StringList();
        busSelects.add(DomainConstants.SELECT_ID);
        busSelects.add(DomainConstants.SELECT_MODIFIED);

        sbWhere.append("(current != 'Complete') && (current != 'Close') && (current != 'Reject')");

        if(sMode.equals("MRU")) {

            Calendar cal = Calendar.getInstance();
            cal.add(java.util.GregorianCalendar.DAY_OF_YEAR, -1);

            String sMinute = String.valueOf(cal.get(Calendar.MINUTE));
            String sSecond = String.valueOf(cal.get(Calendar.SECOND));
            String sAMPM = (cal.get(Calendar.AM_PM) == 0 ) ? "AM" : "PM";

            if(sSecond.length() == 1) { sSecond = "0" + sSecond; }
            if(sMinute.length() == 1) { sMinute = "0" + sMinute; }


            sbWhere.append(" && (modified >= \"");
            sbWhere.append(cal.get(Calendar.MONTH) + 1).append("/").append(cal.get(Calendar.DAY_OF_MONTH)).append("/").append(cal.get(Calendar.YEAR));
            sbWhere.append(" ").append(cal.get(Calendar.HOUR) + 1).append(":").append(sMinute).append(":").append(sSecond).append(" ").append(sAMPM);
            sbWhere.append("\")");

        } else if(sMode.equals("By Status")) {

            String sStatus      = (String) programMap.get("filterStatus");

            if(sStatus.equals("0"))      { sStatus = "Submit";          }
            else if(sStatus.equals("1")) { sStatus = "Evaluate";        }
            else if(sStatus.equals("2")) { sStatus = "Review";          }
            else if(sStatus.equals("3")) { sStatus = "Approved";        }
            else if(sStatus.equals("4")) { sStatus = "Implement";       }
            else if(sStatus.equals("5")) { sStatus = "Validate";        }
            else if(sStatus.equals("6")) { sStatus = "Formal Approval"; }
            else if(sStatus.equals("7")) { sStatus = "Complete";        }
            else if(sStatus.equals("8")) { sStatus = "Close";           }
            else if(sStatus.equals("9")) { sStatus = "Reject";          }

            sbWhere = new StringBuilder();
            sbWhere.append("(current == '").append(sStatus).append("')");

        }

        return pUser.getRelatedObjects(context, DomainConstants.RELATIONSHIP_ASSIGNED_EC, DomainConstants.TYPE_ENGINEERING_CHANGE, busSelects, null, false, true, (short)1, sbWhere.toString(), "", 0);

    }

}
