<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
   <xsl:template name="header">
      <xsl:param name="creator" select="''"/>
      <xsl:param name="createdDate" select="''"/>
      <xsl:param name="reportName" select="''"/>
      <xsl:param name="objectInfo" select="''"/>
      
      <fo:static-content flow-name="xsl-region-before">
         <fo:table width="100%" table-layout="fixed">
            <fo:table-column column-width="proportional-column-width(1)"/>
            <fo:table-body>
            
               <fo:table-row>
                  <fo:table-cell background-color="#3399CC">
                        <fo:block display-align="center" font-size="14pt" font-weight="bolder" text-indent="5pt" line-height="40px" color="#FFFFFF" >
                           <xsl:value-of select="$reportName"/>
                        </fo:block>
                  </fo:table-cell>
               </fo:table-row>
               
               <fo:table-row>
                  <fo:table-cell text-align="end" padding-top="9pt">
                     <fo:block>
                        <xsl:value-of select="'Report Date: '"/><xsl:value-of select="$createdDate"/>
                     </fo:block>
                     <fo:block>
                        <xsl:value-of select="'Created by: '"/><xsl:value-of select="$creator"/>
                     </fo:block>
                  </fo:table-cell>
               </fo:table-row>

            </fo:table-body>
         </fo:table>
      </fo:static-content>
   </xsl:template>
</xsl:stylesheet>

