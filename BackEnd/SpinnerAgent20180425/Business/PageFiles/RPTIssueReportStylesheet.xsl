<?xml version="1.0" encoding="UTF-8"?>
<!--+
    | This stylesheet is a template. The purpose of it is to illustrate the
    | capabilities of the ENOVIA Report Generator. This stylesheet works
    | with Issue Lists.
    +-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" version="1.0">
    <!--+
        | Include dependencies
        +-->
    <xsl:include href="page:RPTGenericStyleAttributes.xsl"/>
    <xsl:include href="page:RPTGenericHeader.xsl"/>
    <xsl:include href="page:RPTGenericFooter.xsl"/>
    <xsl:include href="page:RPTGenericPieChart.xsl"/>
    <!--+
        | The parameters this stylesheet uses
        +-->
    <xsl:param name="paperSize" select="'A3'"/>
    <xsl:param name="appServer.URL"/>
    <!--+
        | Capitalizes the first character within a string
        +-->
    <xsl:template name="capitalizeFirstChar">
        <xsl:param name="aString" select="''"/>
        <xsl:if test="string-length($aString) &gt; 0">
            <xsl:variable name="firstChar" select="substring($aString, 1, 1)"/>
            <xsl:variable name="rest" select="substring($aString, 2)"/>
            <xsl:value-of select="translate($firstChar, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
            <xsl:value-of select="$rest"/>
        </xsl:if>
    </xsl:template>
    <!--+
        | Prints the localized name of the value
        +-->
    <xsl:template match="basic|attr" mode="localized-name">
        <xsl:choose>
            <xsl:when test="@localizedKey">
                <xsl:value-of select="@localizedKey"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="@key"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!--+
        | Defines the layout. This template is the first one called.
        +-->
    <xsl:template match="/">
        <!--+
            | Depending on the paper size parameter, heght/width are defined
            +-->
        <xsl:variable name="pageWidth">
            <xsl:choose>
                <xsl:when test="$paperSize = 'A4'">29.7cm</xsl:when>
                <xsl:when test="$paperSize = 'A3'">42cm</xsl:when>
                <xsl:when test="$paperSize = 'A2'">59.4cm</xsl:when>
                <xsl:when test="$paperSize = 'letter'">27.94cm</xsl:when>
                <xsl:when test="$paperSize = 'legal'">35.56cm</xsl:when>
                <xsl:when test="$paperSize = 'ledger'">43.18cm</xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="pageHeight">
            <xsl:choose>
                <xsl:when test="$paperSize = 'A4'">21cm</xsl:when>
                <xsl:when test="$paperSize = 'A3'">29.7cm</xsl:when>
                <xsl:when test="$paperSize = 'A2'">42cm</xsl:when>
                <xsl:when test="$paperSize = 'letter'">21.59cm</xsl:when>
                <xsl:when test="$paperSize = 'legal'">21.59cm</xsl:when>
                <xsl:when test="$paperSize = 'ledger'">27.94cm</xsl:when>
            </xsl:choose>
        </xsl:variable>

        <!--+
            | fo:root
            +-->
        <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
            <fo:layout-master-set>
                <fo:simple-page-master master-name="table-pages" margin-right="1.5cm" margin-left="1.5cm" margin-bottom="0.5cm" margin-top="1cm">
                    <xsl:attribute name="page-width"><xsl:value-of select="$pageWidth"/></xsl:attribute>
                    <xsl:attribute name="page-height"><xsl:value-of select="$pageHeight"/></xsl:attribute>
                    <fo:region-body margin-top="3cm" margin-bottom="1.8cm"/>
                    <fo:region-before extent="3cm"/>
                    <fo:region-after extent="1.5cm"/>
                </fo:simple-page-master>
                <fo:page-sequence-master master-name="page-sequence">
                    <fo:repeatable-page-master-reference master-reference="table-pages"/>
                </fo:page-sequence-master>
            </fo:layout-master-set>
            <fo:page-sequence master-reference="table-pages">
                <!--+
                    | Call the header template
                    +-->
                <xsl:call-template name="header">
                    <xsl:with-param name="creator">
                        <xsl:value-of select="/report/meta-data/rt-property[@key = 'fullname']"/>
                    </xsl:with-param>
                    <xsl:with-param name="createdDate">
                        <xsl:value-of select="/report/meta-data/rt-property[@key = 'date.localized']"/>
                    </xsl:with-param>
                    <xsl:with-param name="reportName">
                        <xsl:value-of select="'Issues Owned by '"/>
                        <xsl:value-of select="/report/meta-data/rt-property[@key = 'fullname']"/>
                    </xsl:with-param>
                    <xsl:with-param name="objectInfo">
                    </xsl:with-param>
                </xsl:call-template>
                <!--+
                    | Call the footer template
                    +-->
                <xsl:call-template name="footer">
                    <xsl:with-param name="footerText">
                    </xsl:with-param>
                </xsl:call-template>
                <!--+
                    | Call the table body template
                    +-->
                <xsl:call-template name="tableBody"/>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>
    <!--+
        | Generates the table. If any column within the used table has a setting called 'Column Width'
        | then we use the value from that setting to define the column width.
        |
        | The table is generated by iterating the rows, and for each row iterating over the cells.
        | The odd rows has a different background color than the even rows.
        +-->
    <xsl:template name="tableBody">
        <fo:flow flow-name="xsl-region-body">
        
           <fo:block text-align="right">
              <fo:instream-foreign-object>
                 <xsl:call-template name="pieChart">
                    <xsl:with-param name="chartComponents" select="/report/data-groups/stateData"/>
                    <xsl:with-param name="chartTitle" select="'Issue Status Distribution'"/>
                     <xsl:with-param name="chartWidth" select="'300'"/>
                     <xsl:with-param name="chartHeight" select="'200'"/>
                 </xsl:call-template>
               </fo:instream-foreign-object>
           </fo:block>

            <fo:table width="100%" border="0pt solid gray" padding="3pt" table-layout="fixed">
                
                <xsl:for-each select="/report/headers/header">
                    <fo:table-column column-width="proportional-column-width(1)">
                        <xsl:if test="setting[@name='Column Width']">
                            <xsl:attribute name="column-width"><xsl:value-of select="setting[@name='Column Width']"/></xsl:attribute>
                        </xsl:if>
                    </fo:table-column>
                </xsl:for-each>
                
                <fo:table-header>
                    <fo:table-row background-color="#999999">
                        <xsl:for-each select="/report/headers/header">
                            <fo:table-cell xsl:use-attribute-sets="tableHeader">
                                <fo:block>
                                    <xsl:value-of select="label"/>
                                </fo:block>
                            </fo:table-cell>
                        </xsl:for-each>
                    </fo:table-row>
                </fo:table-header>
                
                <fo:table-body>
                    <xsl:for-each select="//row">
                        <fo:table-row>
                        
                            <xsl:if test="position() mod 2 = 0">
                                <xsl:attribute name="background-color">#CCCCCC</xsl:attribute>
                            </xsl:if>
                            <xsl:for-each select="cell">
                               <xsl:if test="value">
                                   <fo:table-cell xsl:use-attribute-sets="tableCell">
                                       <xsl:for-each select="value">
                                           <fo:block>
                                               <xsl:value-of select="."/>
                                           </fo:block>
                                       </xsl:for-each>
                                   </fo:table-cell>
                               </xsl:if>
                            </xsl:for-each>
                            
                        </fo:table-row>
                    </xsl:for-each>
                </fo:table-body>
                
            </fo:table>
            <!--+
                | Empty final block to use when counting pages 
                +-->
            <fo:block id="endofdoc" font-size="11pt" font-family="sans-serif" line-height="18pt" space-after.optimum="7pt" text-align="start">
                <xsl:text/>
            </fo:block>
        </fo:flow>
    </xsl:template>
</xsl:stylesheet>

