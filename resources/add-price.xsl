<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:param name="claimCheckId">unknown</xsl:param>
  <xsl:template match="@* | node()">
    <xsl:copy>
      <xsl:apply-templates select="@* | node()"/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="order">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:attribute name="id"><xsl:value-of select="$claimCheckId"/></xsl:attribute>
      <xsl:apply-templates select="node()"/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="item">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:attribute name="price"><xsl:value-of select="5 * @quantity"/></xsl:attribute>
      <xsl:apply-templates select="node()"/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
