<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:d="http://docbook.org/ns/docbook"
                xmlns:exsl="http://exslt.org/common"
		version="1.0"
                exclude-result-prefixes="exsl d">

    <xsl:import href="urn:docbkx:stylesheet/profile-chunk.xsl"/>
    <!--<xsl:import href="http://docbook.sourceforge.net/release/xsl/current/html/profile-chunk.xsl"/>-->
    <xsl:import href="titlepage.html.templates.xsl"/>

    <xsl:attribute-set name="monospace.verbatim.properties">
        <xsl:attribute name="wrap-option">wrap</xsl:attribute>
    </xsl:attribute-set>
    <xsl:param name="shade.verbatim" select="1"/>
    <xsl:param name="admon.graphics.path">./icons/</xsl:param>
    <xsl:param name="navig.graphics.path">./icons/</xsl:param>
    <xsl:param name="img.src.path">./</xsl:param>

<!-- "Support" for MathML -->
    <xsl:template match="mml:math[node()]" xmlns:mml="http://www.w3.org/1998/Math/MathML">
        <xsl:variable name="object-id"> 
            <xsl:call-template name="object.id"> 
                <xsl:with-param name="object" select="."/> 
            </xsl:call-template> 
        </xsl:variable> 
        <xsl:call-template name="write.chunk">
            <xsl:with-param name="filename" select="concat($base.dir, $object-id, '.mml')"/>
            <xsl:with-param name="content">
                <xsl:copy>
                    <xsl:copy-of select="@*"/>
                    <xsl:apply-templates/>
                </xsl:copy>
            </xsl:with-param>
        </xsl:call-template>
        <img>
            <xsl:attribute name="src">
                <xsl:copy-of select="concat($object-id, '.svg')" />
            </xsl:attribute>
        </img>
    </xsl:template>

    <xsl:template match="d:equation">
        <table>
            <xsl:attribute name="border">
                <xsl:text>0</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="id">
                <xsl:call-template name="object.id"/>
            </xsl:attribute>
            <xsl:attribute name="width">
                <xsl:text>100%</xsl:text>
            </xsl:attribute>
            <tr>
                <td>
                    <xsl:attribute name="width">
                        <xsl:text>90%</xsl:text>
                    </xsl:attribute>
                    <xsl:apply-templates/>
                </td>
                <td>
                    <xsl:attribute name="width">
                        <xsl:text>10%</xsl:text>
                    </xsl:attribute>
                    <xsl:text>(</xsl:text>
                    <xsl:apply-templates select="." mode="label.markup"/>
                    <xsl:text>)</xsl:text>
                </td>
            </tr>
        </table>
    </xsl:template>

    <xsl:template  match="emphasis[@role='bold']">
        <xsl:call-template  name="inline.boldseq"/>
    </xsl:template>

    <xsl:template match="d:cover" mode="titlepage.mode">
        <div>
            <xsl:apply-templates select="." mode="class.cover"/>
            <xsl:apply-templates mode="titlepage.mode"/>
        </div>
    </xsl:template>

    <xsl:template name="division.toc">
        <xsl:param name="toc-context" select="."/>

        <xsl:call-template name="make.toc">
            <xsl:with-param name="toc-context" select="$toc-context"/>
            <xsl:with-param name="nodes" select="d:part|d:reference
                                         |d:preface|d:chapter|d:appendix
                                         |d:article
                                         |d:bibliography|d:glossary|d:index
                                         |d:refentry
                                         |d:bridgehead[$bridgehead.in.toc != 0]"/>

        </xsl:call-template>
    </xsl:template>

    <xsl:template name="component.toc">
        <xsl:param name="toc-context" select="."/>
        <xsl:param name="toc.title.p" select="true()"/>

        <xsl:call-template name="make.toc">
            <xsl:with-param name="toc-context" select="$toc-context"/>
            <xsl:with-param name="toc.title.p" select="$toc.title.p"/>
            <xsl:with-param name="nodes" select="d:section|d:sect1|d:refentry
                                         |d:bibliography|d:glossary
                                         |d:appendix
                                         |d:bridgehead[not(@renderas)
                                                     and $bridgehead.in.toc != 0]
                                         |.//d:bridgehead[@renderas='sect1'
                                                        and $bridgehead.in.toc != 0]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="d:book" mode="toc">
        <xsl:param name="toc-context" select="."/>

        <xsl:call-template name="subtoc">
            <xsl:with-param name="toc-context" select="$toc-context"/>
            <xsl:with-param name="nodes" select="d:part|d:reference
                                         |d:preface|d:chapter|d:appendix
                                         |d:bibliography|d:glossary|d:index
                                         |d:refentry
                                         |d:bridgehead[$bridgehead.in.toc != 0]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="d:part|d:reference" mode="toc">
        <xsl:param name="toc-context" select="."/>

        <xsl:call-template name="subtoc">
            <xsl:with-param name="toc-context" select="$toc-context"/>
            <xsl:with-param name="nodes" select="d:appendix|d:chapter
                                         |d:index|d:glossary|d:bibliography
                                         |d:preface|d:reference|d:refentry
                                         |d:bridgehead[$bridgehead.in.toc != 0]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="d:preface|d:chapter|d:appendix" mode="toc">
        <xsl:param name="toc-context" select="."/>

        <xsl:call-template name="subtoc">
            <xsl:with-param name="toc-context" select="$toc-context"/>
            <xsl:with-param name="nodes" select="d:section|d:sect1|d:glossary|d:bibliography|d:index
                                         |d:bridgehead[$bridgehead.in.toc != 0]"/>
        </xsl:call-template>
    </xsl:template>


    <xsl:template name="footer.navigation">
        <xsl:param name="prev" select="/foo"/>
        <xsl:param name="next" select="/foo"/>
        <xsl:param name="nav.context"/>
        <xsl:param name="home" select="/*[1]"/>
        <xsl:variable name="up" select="parent::*"/>

        <xsl:variable name="row1" select="count($prev) &gt; 0
                                    or count($up) &gt; 0
                                    or count($next) &gt; 0"/>

        <xsl:variable name="row2" select="($prev and $navig.showtitles != 0)
                                    or (generate-id($home) != generate-id(.)
                                        or $nav.context = 'toc')
                                    or ($chunk.tocs.and.lots != 0
                                        and $nav.context != 'toc')
                                    or ($next and $navig.showtitles != 0)"/>

        <xsl:if test="$suppress.navigation = '0' and $suppress.footer.navigation = '0'">
            <div class="navfooter">
                <xsl:if test="$footer.rule != 0">
                    <hr/>
                </xsl:if>

                <xsl:if test="$row1 or $row2">
                    <table width="100%" summary="Navigation footer">
                        <xsl:if test="$row1">
                            <tr>
                                <td width="40%" align="left">
                                    <xsl:if test="count($prev)>0  and local-name($prev) != 'article'">
                                        <a accesskey="p">
                                            <xsl:attribute name="href">
                                                <xsl:call-template name="href.target">
                                                    <xsl:with-param name="object" select="$prev"/>
                                                </xsl:call-template>
                                            </xsl:attribute>
                                            <xsl:call-template name="navig.content">
                                                <xsl:with-param name="direction" select="'prev'"/>
                                            </xsl:call-template>
                                        </a>
                                    </xsl:if>
                                    <xsl:text>&#160;</xsl:text>
                                </td>
                                <td width="20%" align="center">
                                    <xsl:choose>
                                        <xsl:when test="count($up)>0">
                                            <a accesskey="u">
                                                <xsl:attribute name="href">
                                                    <xsl:call-template name="href.target">
                                                        <xsl:with-param name="object" select="$up"/>
                                                    </xsl:call-template>
                                                </xsl:attribute>
                                                <xsl:call-template name="navig.content">
                                                    <xsl:with-param name="direction" select="'up'"/>
                                                </xsl:call-template>
                                            </a>
                                        </xsl:when>
                                        <xsl:otherwise>&#160;</xsl:otherwise>
                                    </xsl:choose>
                                </td>
                                <td width="40%" align="right">
                                    <xsl:text>&#160;</xsl:text>
                                    <xsl:if test="count($next)>0  and local-name($next) != 'article'">
                                        <a accesskey="n">
                                            <xsl:attribute name="href">
                                                <xsl:call-template name="href.target">
                                                    <xsl:with-param name="object" select="$next"/>
                                                </xsl:call-template>
                                            </xsl:attribute>
                                            <xsl:call-template name="navig.content">
                                                <xsl:with-param name="direction" select="'next'"/>
                                            </xsl:call-template>
                                        </a>
                                    </xsl:if>
                                </td>
                            </tr>
                        </xsl:if>

                        <xsl:if test="$row2">
                            <tr>
                                <td width="40%" align="left" valign="top">
                                    <xsl:if test="$navig.showtitles != 0  and local-name($prev) != 'article'">
                                        <xsl:apply-templates select="$prev" mode="object.title.markup"/>
                                    </xsl:if>
                                    <xsl:text>&#160;</xsl:text>
                                </td>
                                <td width="20%" align="center">
                                    <xsl:choose>
                                        <xsl:when test="$home != . or $nav.context = 'toc'">
                                            <a accesskey="h">
                                                <xsl:attribute name="href">
                                                    <xsl:call-template name="href.target">
                                                        <xsl:with-param name="object" select="$home"/>
                                                    </xsl:call-template>
                                                </xsl:attribute>
                                                <xsl:call-template name="navig.content">
                                                    <xsl:with-param name="direction" select="'home'"/>
                                                </xsl:call-template>
                                            </a>
                                            <xsl:if test="$chunk.tocs.and.lots != 0 and $nav.context != 'toc'">
                                                <xsl:text>&#160;|&#160;</xsl:text>
                                            </xsl:if>
                                        </xsl:when>
                                        <xsl:otherwise>&#160;</xsl:otherwise>
                                    </xsl:choose>

                                    <xsl:if test="$chunk.tocs.and.lots != 0 and $nav.context != 'toc'">
                                        <a accesskey="t">
                                            <xsl:attribute name="href">
                                                <xsl:apply-templates select="/*[1]"
                                           mode="recursive-chunk-filename"/>
                                                <xsl:text>-toc</xsl:text>
                                                <xsl:value-of select="$html.ext"/>
                                            </xsl:attribute>
                                            <xsl:call-template name="gentext">
                                                <xsl:with-param name="key" select="'nav-toc'"/>
                                            </xsl:call-template>
                                        </a>
                                    </xsl:if>
                                </td>
                                <td width="40%" align="right" valign="top">
                                    <xsl:text>&#160;</xsl:text>
                                    <xsl:if test="$navig.showtitles != 0 and local-name($next) != 'article'">
                                        <xsl:apply-templates select="$next" mode="object.title.markup"/>
                                    </xsl:if>
                                </td>
                            </tr>
                        </xsl:if>
                    </table>
                </xsl:if>
            </div>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
