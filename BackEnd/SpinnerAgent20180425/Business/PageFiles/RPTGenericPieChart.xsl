<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:java="http://xml.apache.org/xslt/java" xmlns:svg="http://www.w3.org/2000/svg">
      
   <xsl:template name="pieChart">
   
      <xsl:param name="chartComponents"/>
      <xsl:param name="chartTitle"/>
      <xsl:param name="chartWidth" select="300"/>
      <xsl:param name="chartHeight" select="200"/>

      <svg:svg width="{$chartWidth}" height="{$chartHeight}" viewBox="0 0 {$chartWidth} {$chartHeight}">
         <xsl:variable name="total" select="sum($chartComponents/chartComponent/@count)"/>
         <xsl:for-each select="$chartComponents/chartComponent">
            <xsl:variable name="color">
               <xsl:choose>
                  <xsl:when test="(position() = 1)">
                     <xsl:text>lightSalmon</xsl:text>
                  </xsl:when>
                  <xsl:when test="(position() = 2)">
                     <xsl:text>lightCyan</xsl:text>
                  </xsl:when>
                  <xsl:when test="(position() = 3 or position() = 8 or position() = 13 or position() = 18)">
                     <xsl:text>mistyRose</xsl:text>
                  </xsl:when>
                  <xsl:when test="(position() = 4 or position() = 9 or position() = 14 or position() = 19)">
                     <xsl:text>gold</xsl:text>
                  </xsl:when>
                  <xsl:when test="(position() = 5 or position() = 10 or position() = 15 or position() = 20)">
                     <xsl:text>tan</xsl:text>
                  </xsl:when>
                  <xsl:when test="(position() = 6 or position() = 11 or position() = 16)">
                     <xsl:text>lightGreen</xsl:text>
                  </xsl:when>
                  <xsl:otherwise>
                     <xsl:text>lightSteelBlue</xsl:text>
                  </xsl:otherwise>
               </xsl:choose>
            </xsl:variable>
            
            <xsl:variable name="position" select="position()"/>
            
            <xsl:apply-templates select="." mode="pie">
               <xsl:with-param name="color" select="$color"/>
               <xsl:with-param name="total" select="$total"/>
               <xsl:with-param name="chartHeight" select="$chartHeight"/>
               <xsl:with-param name="runningTotal" select="sum(preceding-sibling::chartComponent/@count)"/>
            </xsl:apply-templates>
            
            <xsl:apply-templates select="." mode="pieLegend">
               <xsl:with-param name="color" select="$color"/>
               <xsl:with-param name="offset" select="40 + ($position * 15)"/>
            </xsl:apply-templates>
         </xsl:for-each>

         <svg:text>
            <xsl:attribute name="style"><xsl:text>font-size:12; text-anchor:middle</xsl:text></xsl:attribute>
            <xsl:attribute name="font-weight"><xsl:text>bold</xsl:text></xsl:attribute>
            <xsl:attribute name="x"><xsl:text>150</xsl:text></xsl:attribute>
            <xsl:attribute name="y"><xsl:text>175</xsl:text></xsl:attribute>
            <xsl:value-of select="$chartTitle"/>
         </svg:text>
         
      </svg:svg>
   </xsl:template>
   
   <xsl:template match="chartComponent" mode="pie">
      
      <xsl:param name="color" select="'indianRed'"/>
      <xsl:param name="chartHeight"/>
      <xsl:param name="total" select="'0'"/>
      <xsl:param name="runningTotal" select="'0'"/>
      
      <xsl:variable name="number" select="@count"/>
      <xsl:variable name="currentAngle" select="java:java.lang.Math.toRadians(($number div $total) * 360.0)"/>
      <xsl:variable name="halfAngle" select="java:java.lang.Math.toRadians((($number div 2) div $total) * 360.0)"/>
      <xsl:variable name="rotation" select="270 + (360.0 * ($runningTotal div $total))"/>
      <xsl:variable name="x1" select="java:java.lang.Math.cos($halfAngle) * 60"/>
      <xsl:variable name="y1" select="java:java.lang.Math.sin($halfAngle) * 60"/>
      <xsl:variable name="cosTheta" select="java:java.lang.Math.cos(java:java.lang.Math.toRadians($rotation))"/>
      <xsl:variable name="sinTheta" select="java:java.lang.Math.sin(java:java.lang.Math.toRadians($rotation))"/>
      
      <svg:path style="fill:{$color};stroke:black;stroke-width:1;fillrule:evenodd;stroke-linejoin:bevel;">
         <xsl:attribute name="transform">
            <xsl:text>translate(80,</xsl:text>
            <xsl:value-of select="$chartHeight div 2"/>
            <xsl:text>) </xsl:text>
            <!--centering the circle in the non-legend space-->
            <xsl:text>rotate(</xsl:text>
            <xsl:value-of select="$rotation"/>
            <xsl:text>)</xsl:text>
         </xsl:attribute>

         <xsl:attribute name="d">
            <xsl:text>M 50 0 A 50 50 0 </xsl:text>
            <xsl:choose>
               <xsl:when test="$currentAngle > 3.14">
                  <xsl:text>1 </xsl:text>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:text>0 </xsl:text>
               </xsl:otherwise>
            </xsl:choose>
            <xsl:text>1 </xsl:text>
            <xsl:value-of select="java:java.lang.Math.cos($currentAngle) * 50"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="java:java.lang.Math.sin($currentAngle) * 50"/>
            <xsl:text> L 0 0 Z</xsl:text>
         </xsl:attribute>

      </svg:path>
      
      <svg:text style="text-anchor:middle">
         <xsl:attribute name="transform">
            <xsl:text>translate(80,</xsl:text>
            <xsl:value-of select="$chartHeight div 2"/>
            <xsl:text>) </xsl:text>
         </xsl:attribute>
         <xsl:attribute name="x"><xsl:value-of select="($x1 * $cosTheta) - ($y1 * $sinTheta)"/></xsl:attribute>
         <xsl:attribute name="y"><xsl:value-of select="($x1 * $sinTheta) + ($y1 * $cosTheta)"/></xsl:attribute>
         <xsl:value-of select="round(100 * ($number div $total))"/>
         <xsl:text>%</xsl:text>
      </svg:text>
   </xsl:template>

   <xsl:template match="chartComponent" mode="pieLegend">
   
      <xsl:param name="color" select="'indianRed'"/>
      <xsl:param name="offset" select="'0'"/>
   
      <svg:text>
         <xsl:attribute name="style"><xsl:text>font-size:10; text-anchor:start</xsl:text></xsl:attribute>
         <xsl:attribute name="x"><xsl:text>160</xsl:text></xsl:attribute>
         <xsl:attribute name="y"><xsl:value-of select="$offset"/></xsl:attribute>
         <xsl:value-of select="@gid"/>
         <xsl:text> (</xsl:text>
         <xsl:value-of select="@count"/>
         <xsl:text>) </xsl:text>
      </svg:text>
   
      <svg:path>
         <xsl:attribute name="style">
            <xsl:text>stroke:black; stroke-width:1; fill:</xsl:text>
            <xsl:value-of select="$color"/>
         </xsl:attribute>
         <xsl:attribute name="d">
            <xsl:text>M 250 </xsl:text>
            <xsl:value-of select="$offset - 10"/>
            <xsl:text> L 250 </xsl:text>
            <xsl:value-of select="$offset"/>
            <xsl:text> L 260 </xsl:text>
            <xsl:value-of select="$offset"/>
            <xsl:text> L 260 </xsl:text>
            <xsl:value-of select="$offset - 10"/>
            <xsl:text> Z</xsl:text>
         </xsl:attribute>
      </svg:path>
      
   </xsl:template>

</xsl:stylesheet>

