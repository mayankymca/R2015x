<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:aef="http://www.matrixone.com/aef">
	<xsl:output method="html" version="1.0" encoding="UTF-8" indent="no"/>
	<xsl:template match="aef:mxRoot">
		<html>
			<head>
				<title><xsl:value-of select="aef:headerData/aef:header"/></title>
				<style TYPE='text/css'>
					body,
					table,
					td {
						font-family: Verdana, Arial, Helvetica;
						font-size: 9pt;
						line-height: 12pt;
						}
					
					body {
						background-color: rgb(247,247,247);
						}
					
					
					table.emailTable {
						border-top: solid 1px #c6c6c6;
						border-right: solid 1px #7e7e7e;
						border-bottom: solid 1px #7e7e7e;
						border-left: solid 1px #c6c6c6;
						}
						
						td.message {
						background-color: #eeeeee;
						font-size: 13pt;
						font-weight: bold;
						padding: 12px;
						color: #990000;
						line-height: 15pt;
						border-top: solid 2px #fefefe;
						border-bottom: solid 2px #c6c6c6;
						}
					
					td.separator {
						background-color: #7A95B9;
						padding: 6px 6px 6px 12px;
						font-size: 10pt;
						font-weight: bold;
						color: #ffffff;
						border-bottom: 1px solid #5f738e;
						}
					
					td.prompt {
						background-color: #d8d8d8;
						font-weight: bold;
						color: black;
						width: 150px;
						border-bottom: 1px solid white;
						padding: 6px 6px 6px 12px;
						}
					
					td.data {
						color: black;
						border-bottom: 1px solid white;
						padding: 5px 5px 5px 12px;
						border-left: 2px solid white;
						background-color: #eeeeee;
						}
					
					td.data table tr td {
						padding: 2px;
						
						}
					
					td.data table tr td.highlight {
						background-color: #efffff;
						border: 1px dashed #b4b4b4;
						font-weight: bold;
						padding: 2px 10px 2px 6px;
						}
					
					span.emailNotify {
						font-size: 8pt;
						font-style: italic;
						color: rgb(150,150,150);
						}
					
					a {
						font-weight: bold;
						color: rgb(34,118,141);
						}
					
					span.appendedBy {
						background-color: #ffffee;
						color: black;
						display: block;
						width: auto;
						padding: 2px 6px 2px 6px;
						margin: 0;
						border: 1px dashed #b4b4b4;
						}
					
					span.modifiedBy {
						font-weight: bold;
						}
						
							
					span.attribution {
						font: verdana;
						color: black;
						font-size: 8pt;
						font-weight: normal;
						}
					#wrapper {
						border-top: solid 1px #7e7e7e;
						border-right: solid 1px #7e7e7e;
						border-bottom: solid 1px #7e7e7e;
						border-left: solid 1px #7e7e7e;
						}
				</style>
			</head>
			<div id="wrapper" width="60%">
				<body class="email">
					<table class="emailTable" border="0" width="100%" cellspacing="0" cellpadding="0">
					  <tr>
						  <td colspan="2" class="message">
							  <xsl:copy-of select="aef:headerData/aef:header/node()"/> <span class="attribution"><br><xsl:copy-of select="aef:headerData/aef:creatorText/node()"/></br></span>
						  </td>
					  </tr>
					  <xsl:apply-templates select="aef:bodyData/aef:sections/aef:section"></xsl:apply-templates>
					</table>
				</body>
			</div><br></br>
		    <xsl:apply-templates select="aef:footerData/aef:dataLines/aef:dataLine"></xsl:apply-templates>
		    <p><span class="emailNotify"><xsl:copy-of select="aef:footerData/aef:signature/node()"/></span></p>
		</html>
	</xsl:template>

	<xsl:template match="aef:bodyData/aef:sections/aef:section">
		<tr><td  class="separator" colspan="2"><xsl:copy-of select="aef:sectionHeader/node()"/></td></tr>
	    <xsl:apply-templates select="aef:fields/aef:field"></xsl:apply-templates>
	</xsl:template>

	<xsl:template match="aef:fields/aef:field">
	  <tr >
		<td class="prompt" width="40%" nowrap="nowrap"><xsl:copy-of select="aef:label/node()"/></td>
		<xsl:choose>		
			<xsl:when test="aef:oldValue">
				<td class="data" width="30%">
					<table border="0" cellspacing="0" cellpadding="0">
						<tr>
							<td><xsl:copy-of select="aef:oldValue/node()"/></td>
							<td>--></td>
							<td class="highlight"><xsl:copy-of select="aef:value/node()"/></td>
						</tr>
					</table>			
				</td>
			</xsl:when>
			<xsl:otherwise>
				<td class="data" width="70%"><xsl:copy-of select="aef:value/node()"/> </td>
			</xsl:otherwise>
		</xsl:choose>
	  </tr>
	</xsl:template>						
                
	<xsl:template match="aef:footerData/aef:dataLines/aef:dataLine/node()">
		<xsl:copy-of select="."/>
	</xsl:template>

</xsl:stylesheet>

