<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:aef="http://www.matrixone.com/aef">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="no"/>
	<xsl:template match="aef:mxRoot">
		<aef:mxRoot>
			<xsl:copy-of select="aef:headerData/aef:header/node()"/>
			<xsl:text disable-output-escaping="no">&#10;</xsl:text>
			<xsl:copy-of select="aef:headerData/aef:creatorText/node()"/>
			<xsl:text disable-output-escaping="no">&#10;</xsl:text>
			<xsl:text disable-output-escaping="no">&#10;</xsl:text>
			<xsl:apply-templates select="aef:bodyData/aef:sections/aef:section"/>
			<xsl:text disable-output-escaping="no">&#10;</xsl:text>
			<xsl:text disable-output-escaping="no">&#10;</xsl:text>
			<xsl:apply-templates select="aef:footerData/aef:dataLines/aef:dataLine"/>
			<xsl:copy-of select="aef:footerData/aef:signature/node()"/>
		</aef:mxRoot>
	</xsl:template>
	<xsl:template match="aef:bodyData/aef:sections/aef:section">
		<xsl:copy-of select="aef:sectionHeader/node()"/>
		<xsl:text disable-output-escaping="no">:&#10;</xsl:text>
		<xsl:apply-templates select="aef:fields/aef:field"/>
	</xsl:template>
	<xsl:template match="aef:fields/aef:field">
		<xsl:copy-of select="aef:label/node()"/>
		<xsl:text disable-output-escaping="no"> = </xsl:text>
		<xsl:choose>
			<xsl:when test="aef:oldValue">
				<xsl:copy-of select="aef:oldValue/node()"/>
				<xsl:text disable-output-escaping="no"> &#187; </xsl:text>
				<xsl:copy-of select="aef:value/node()"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:copy-of select="aef:value/node()"/>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:text disable-output-escaping="no">&#10;</xsl:text>
	</xsl:template>
	<xsl:template match="aef:footerData/aef:dataLines/aef:dataLine/node()">
		<xsl:copy-of select="."/>
		<xsl:text disable-output-escaping="no">&#10;</xsl:text>
	</xsl:template>
</xsl:stylesheet>

