<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:attribute-set name="tableHeader">
        <xsl:attribute name="font-size">12pt</xsl:attribute>
        <xsl:attribute name="font-family">sans-serif</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="text-align">left</xsl:attribute>
        <xsl:attribute name="display-align">center</xsl:attribute>
        <xsl:attribute name="border-width">1pt</xsl:attribute>
        <xsl:attribute name="border-style">solid</xsl:attribute>
        <xsl:attribute name="border-color">#FFFFFF</xsl:attribute>
        <xsl:attribute name="color">#FFFFFF</xsl:attribute>
        <xsl:attribute name="padding-top">2pt</xsl:attribute>
        <xsl:attribute name="padding-bottom">1pt</xsl:attribute>
        <xsl:attribute name="padding-start">3pt</xsl:attribute>
        <xsl:attribute name="padding-after">3pt</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="tableCell">
        <xsl:attribute name="font-size">12pt</xsl:attribute>
        <xsl:attribute name="font-family">sans-serif</xsl:attribute>
        <xsl:attribute name="font-weight">normal</xsl:attribute>
        <xsl:attribute name="text-align">left</xsl:attribute>
        <xsl:attribute name="display-align">center</xsl:attribute>
        <xsl:attribute name="border-width">1pt</xsl:attribute>
        <xsl:attribute name="border-style">solid</xsl:attribute>
        <xsl:attribute name="border-color">#FFFFFF</xsl:attribute>
        <xsl:attribute name="color">#000000</xsl:attribute>
        <xsl:attribute name="padding-top">3pt</xsl:attribute>
        <xsl:attribute name="padding-bottom">2pt</xsl:attribute>
        <xsl:attribute name="padding-start">3pt</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="header1">
        <xsl:attribute name="font-size">10pt</xsl:attribute>
        <xsl:attribute name="font-family">sans-serif</xsl:attribute>
        <xsl:attribute name="font-weight">normal</xsl:attribute>
        <xsl:attribute name="display-align">center</xsl:attribute>
        <xsl:attribute name="border-width">1pt</xsl:attribute>
        <xsl:attribute name="padding-top">2pt</xsl:attribute>
        <xsl:attribute name="padding-bottom">1pt</xsl:attribute>
        <xsl:attribute name="padding-start">3pt</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="header2">
        <xsl:attribute name="font-size">13pt</xsl:attribute>
        <xsl:attribute name="font-family">sans-serif</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
        <xsl:attribute name="display-align">center</xsl:attribute>
        <xsl:attribute name="border-width">1pt</xsl:attribute>
        <xsl:attribute name="padding-top">2pt</xsl:attribute>
        <xsl:attribute name="padding-bottom">1pt</xsl:attribute>
        <xsl:attribute name="padding-start">3pt</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="footer1">
        <xsl:attribute name="font-size">10pt</xsl:attribute>
        <xsl:attribute name="font-family">sans-serif</xsl:attribute>
        <xsl:attribute name="font-weight">normal</xsl:attribute>
        <xsl:attribute name="display-align">center</xsl:attribute>
        <xsl:attribute name="border-width">1pt</xsl:attribute>
        <xsl:attribute name="padding-top">2pt</xsl:attribute>
        <xsl:attribute name="padding-bottom">1pt</xsl:attribute>
        <xsl:attribute name="padding-start">3pt</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="footer2">
        <xsl:attribute name="font-size">10pt</xsl:attribute>
        <xsl:attribute name="font-family">sans-serif</xsl:attribute>
        <xsl:attribute name="font-weight">normal</xsl:attribute>
        <xsl:attribute name="display-align">center</xsl:attribute>
        <xsl:attribute name="border-width">1pt</xsl:attribute>
        <xsl:attribute name="padding-top">2pt</xsl:attribute>
        <xsl:attribute name="padding-bottom">1pt</xsl:attribute>
        <xsl:attribute name="padding-start">3pt</xsl:attribute>
    </xsl:attribute-set>
</xsl:stylesheet>

