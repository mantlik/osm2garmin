<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:exsl="http://exslt.org/common" xmlns:fo="http://www.w3.org/1999/XSL/Format" 
    xmlns:ng="http://docbook.org/docbook-ng" xmlns:db="http://docbook.org/ns/docbook" 
    xmlns:exslt="http://exslt.org/common" exslt:dummy="dummy" ng:dummy="dummy" db:dummy="dummy" 
    extension-element-prefixes="exslt" exclude-result-prefixes="db ng exsl exslt" version="1.0">
        
    <xsl:import href="urn:docbkx:stylesheet/profile-docbook.xsl"/>
    <!--<xsl:import href="http://docbook.sourceforge.net/release/xsl/current/fo/profile-docbook.xsl"/>-->
    <xsl:import href="titlepage.fo.templates.xsl"/>

    <xsl:param name="base.dir">
        <xsl:value-of select="substring-before($targets.filename,'targets.db')"/>
    </xsl:param>
    <xsl:param name="admon.graphics.path">
        <xsl:value-of select="$base.dir"/>
        <xsl:value-of select="'../../../Osm2garminGUI/src/main/javahelp/org/mantlik/osm2garminspi/docs/icons/'"/>
    </xsl:param>
    <xsl:param name="navig.graphics.path">
        <xsl:value-of select="$base.dir"/>
        <xsl:value-of select="'../../../Osm2garminGUI/src/main/javahelp/org/mantlik/osm2garminspi/docs/icons/'"/>
    </xsl:param>
    <xsl:param name="img.src.path">
        <xsl:value-of select="$base.dir"/>
        <xsl:value-of select="'../../../Osm2garminGUI/src/main/javahelp/org/mantlik/osm2garminspi/docs/'"/>
    </xsl:param>

    <xsl:attribute-set name="monospace.verbatim.properties" use-attribute-sets="verbatim.properties monospace.properties">
        <xsl:attribute name="wrap-option">wrap</xsl:attribute>
        <xsl:attribute name="hyphenation-character">&lt;</xsl:attribute>
        <xsl:attribute name="font-size">7</xsl:attribute>
    </xsl:attribute-set>
    
    <xsl:attribute-set name="shade.verbatim.style">
        <xsl:attribute name="background-color">#FFFFCC</xsl:attribute>
    </xsl:attribute-set>

    <xsl:param name="shade.verbatim" select="1"/>
    <xsl:param name="keep.relative.image.uris" select="1"/>

    <xsl:template  match="emphasis[@role='bold']">
        <xsl:call-template  name="inline.boldseq"/>
    </xsl:template>

    <xsl:template match="db:cover" mode="titlepage.mode">
        <fo:block>
            <xsl:apply-templates mode="titlepage.mode"/>
        </fo:block>
    </xsl:template>

    <xsl:template match="equation">
        <fo:table width="100%" table-layout="fixed">
            <xsl:attribute name="id">
                <xsl:call-template name="object.id"/>
            </xsl:attribute>
            <fo:table-column column-width="90%"/>
            <fo:table-column column-width="10%"/>
            <fo:table-body start-indent="0pt" end-indent="0pt">
                <fo:table-row>
                    <fo:table-cell text-align="center">
                        <fo:block>
                            <xsl:apply-templates/>
                        </fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="right" display-align="center">
                        <fo:block>
                            <xsl:text>(</xsl:text>
                            <xsl:apply-templates select="." mode="label.markup"/>
                            <xsl:text>)</xsl:text>
                        </fo:block>
                    </fo:table-cell>
                </fo:table-row>
            </fo:table-body>
        </fo:table>
    </xsl:template>

</xsl:stylesheet>
