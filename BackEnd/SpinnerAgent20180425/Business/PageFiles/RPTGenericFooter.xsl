<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format">

    <xsl:template name="footer">
        <xsl:param name="footerText"/>

        <fo:static-content flow-name="xsl-region-after">
            <fo:table width="100%" table-layout="fixed">
                <fo:table-column column-width="proportional-column-width(1)"/>
                <fo:table-column column-width="proportional-column-width(1)"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="20pt" font-size="10pt" font-family="Times" font-style="italic" color="#606060">
                                <xsl:value-of select="$footerText"/>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="20pt" font-size="10pt" text-align="end" font-family="Times" font-style="italic" color="#606060">
                                Page <fo:page-number/> of <fo:page-number-citation ref-id="endofdoc"/>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
        </fo:static-content>
    </xsl:template>
</xsl:stylesheet>

