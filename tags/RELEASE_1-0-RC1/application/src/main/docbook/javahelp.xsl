<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:d="http://docbook.org/ns/docbook"
                xmlns:doc="http://nwalsh.com/xsl/documentation/1.0" 
                xmlns:ng="http://docbook.org/docbook-ng" 
                xmlns:db="http://docbook.org/ns/docbook" 
                xmlns:exsl="http://exslt.org/common" 
                xmlns:exslt="http://exslt.org/common"
                xmlns:suwl="http://nwalsh.com/xslt/ext/com.nwalsh.saxon.UnwrapLinks"
                xmlns:xlink='http://www.w3.org/1999/xlink'
                exslt:dummy="dummy" ng:dummy="dummy" db:dummy="dummy" 
                extension-element-prefixes="exslt" version="1.0" 
                exclude-result-prefixes="doc suwl xlink ng db exsl exslt d">

    <xsl:import href="urn:docbkx:stylesheet/profile-javahelp.xsl"/>
    <!--<xsl:import href="http://docbook.sourceforge.net/release/xsl/current/javahelp/profile-javahelp.xsl"/>-->
    <xsl:import href="titlepage.html.templates.xsl"/>

    <xsl:attribute-set name="monospace.verbatim.properties">
        <xsl:attribute name="wrap-option">
            <xsl:text>wrap</xsl:text>
        </xsl:attribute>
    </xsl:attribute-set>

    <xsl:template  match="emphasis[@role='bold']">
        <xsl:call-template  name="inline.boldseq"/>
    </xsl:template>

    <xsl:template match="d:cover" mode="titlepage.mode">
        <div>
            <xsl:apply-templates select="." mode="class.cover"/>
            <xsl:apply-templates mode="titlepage.mode"/>
        </div>
    </xsl:template>
    
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
                <xsl:copy-of select="concat($object-id, '.png')" />
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

<xsl:template name="helpmap.content">
  <map version="1.0">
    <xsl:choose>
      <xsl:when test="$rootid != ''">
        <xsl:apply-templates select="key('id',$rootid)//set                                      | key('id',$rootid)//book                                      | key('id',$rootid)//part                                      | key('id',$rootid)//reference                                      | key('id',$rootid)//preface                                      | key('id',$rootid)//chapter                                      | key('id',$rootid)//appendix                                      | key('id',$rootid)//article                                      | key('id',$rootid)//colophon                                      | key('id',$rootid)//refentry                                      | key('id',$rootid)//section                                      | key('id',$rootid)//sect1                                      | key('id',$rootid)//sect2                                      | key('id',$rootid)//sect3                                      | key('id',$rootid)//sect4                                      | key('id',$rootid)//sect5                                      | key('id',$rootid)//indexterm                                       | key('id',$rootid)//glossary                                      | key('id',$rootid)//bibliography          | key('id',$rootid)//*[@xml:id]" mode="map"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="//set                                      | //book                                      | //part                                      | //reference                                      | //preface                                      | //chapter                                      | //appendix                                      | //article                                      | //colophon                                      | //refentry                                      | //section                                      | //sect1                                      | //sect2                                      | //sect3                                      | //sect4                                      | //sect5                                      | //indexterm                                      | //glossary                                      | //bibliography          | //*[@xml:id]" mode="map"/>
      </xsl:otherwise>
    </xsl:choose>
  </map>
</xsl:template>

<xsl:template match="*[@xml:id]" mode="map">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <mapID target="{$id}">
    <xsl:attribute name="url">
      <xsl:call-template name="href.target.uri"/>
    </xsl:attribute>
  </mapID>
</xsl:template>

    <xsl:template name="simple.xlink">
        <xsl:param name="node" select="."/>
        <xsl:param name="content">
            <xsl:apply-templates/>
        </xsl:param>
        <xsl:param name="linkend" select="$node/@linkend"/>
        <xsl:param name="xhref" select="$node/@xlink:href"/>

  <!-- Support for @xlink:show -->
        <xsl:variable name="target.show">
            <xsl:choose>
                <xsl:when test="$node/@xlink:show = 'new'">_blank</xsl:when>
                <xsl:when test="$node/@xlink:show = 'replace'">_top</xsl:when>
                <xsl:otherwise></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="link">
            <xsl:choose>
                <xsl:when test="$xhref and 
                      (not($node/@xlink:type) or 
                           $node/@xlink:type='simple')">

        <!-- Is it a local idref or a uri? -->
                    <xsl:variable name="is.idref">
                        <xsl:choose>
            <!-- if the href starts with # and does not contain an "(" -->
            <!-- or if the href starts with #xpointer(id(, it's just an ID -->
                            <xsl:when test="starts-with($xhref,'#')
                            and (not(contains($xhref,'&#40;'))
                            or starts-with($xhref,
                                       '#xpointer&#40;id&#40;'))">1
                            </xsl:when>
                            <xsl:otherwise>0</xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>

        <!-- Is it an olink ? -->
                    <xsl:variable name="is.olink">
                        <xsl:choose>
            <!-- If xlink:role="http://docbook.org/xlink/role/olink" -->
            <!-- and if the href contains # -->
                            <xsl:when test="contains($xhref,'#') and
                 @xlink:role = $xolink.role">1
                            </xsl:when>
                            <xsl:otherwise>0</xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>

                    <xsl:choose>
                        <xsl:when test="$is.olink = 1">
                            <xsl:call-template name="olink">
                                <xsl:with-param name="content" select="$content"/>
                            </xsl:call-template>
                        </xsl:when>

                        <xsl:when test="$is.idref = 1">

                            <xsl:variable name="idref">
                                <xsl:call-template name="xpointer.idref">
                                    <xsl:with-param name="xpointer" select="$xhref"/>
                                </xsl:call-template>
                            </xsl:variable>

                            <xsl:variable name="targets" select="key('id',$idref)"/>
                            <xsl:variable name="target" select="$targets[1]"/>

                            <xsl:call-template name="check.id.unique">
                                <xsl:with-param name="linkend" select="$idref"/>
                            </xsl:call-template>

                            <xsl:choose>
                                <xsl:when test="count($target) = 0">
                                    <xsl:message>
                                        <xsl:text>XLink to nonexistent id: </xsl:text>
                                        <xsl:value-of select="$idref"/>
                                    </xsl:message>
                                    <xsl:copy-of select="$content"/>
                                </xsl:when>

                                <xsl:otherwise>
                                    <a>
                                        <xsl:apply-templates select="." mode="common.html.attributes"/>

                                        <xsl:attribute name="href">
                                            <xsl:call-template name="href.target">
                                                <xsl:with-param name="object" select="$target"/>
                                            </xsl:call-template>
                                        </xsl:attribute>

                                        <xsl:choose>
                                            <xsl:when test="$node/@xlink:title">
                                                <xsl:attribute name="title">
                                                    <xsl:value-of select="$node/@xlink:title"/>
                                                </xsl:attribute>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:apply-templates select="$target"
                                           mode="html.title.attribute"/>
                                            </xsl:otherwise>
                                        </xsl:choose>

                                        <xsl:if test="$target.show !=''">
                                            <xsl:attribute name="target">
                                                <xsl:value-of select="$target.show"/>
                                            </xsl:attribute>
                                        </xsl:if>

                                        <xsl:copy-of select="$content"/>

                                    </a>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>

          <!-- otherwise it's a URI -->
                        <xsl:otherwise>
                            <object>
                                <xsl:attribute name="classid">
                                    <xsl:text>java:org.netbeans.modules.javahelp.BrowserDisplayer</xsl:text>
                                </xsl:attribute>
                                <param>
                                    <xsl:attribute name="name">
                                        <xsl:text>content</xsl:text>
                                    </xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="$xhref"/>
                                    </xsl:attribute>
                                </param>
                                <param>
                                    <xsl:attribute name="name">
                                        <xsl:text>text</xsl:text>
                                    </xsl:attribute>
                                    <xsl:attribute name="value">
                                                <xsl:value-of select="$content"/>
                                    </xsl:attribute>
                                </param>
                                <param>
                                    <xsl:attribute name="name">
                                        <xsl:text>textColor</xsl:text>
                                    </xsl:attribute>
                                    <xsl:attribute name="value">
                                        <xsl:text>blue</xsl:text>
                                    </xsl:attribute>
                                </param>
                            </object>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>

                <xsl:when test="$linkend">
                    <xsl:variable name="targets" select="key('id',$linkend)"/>
                    <xsl:variable name="target" select="$targets[1]"/>

                    <xsl:call-template name="check.id.unique">
                        <xsl:with-param name="linkend" select="$linkend"/>
                    </xsl:call-template>

                    <a>
                        <xsl:apply-templates select="." mode="common.html.attributes"/>
                        <xsl:attribute name="href">
                            <xsl:call-template name="href.target">
                                <xsl:with-param name="object" select="$target"/>
                            </xsl:call-template>
                        </xsl:attribute>

                        <xsl:apply-templates select="$target" mode="html.title.attribute"/>

                        <xsl:copy-of select="$content"/>
          
                    </a>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$content"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="function-available('suwl:unwrapLinks')">
                <xsl:copy-of select="suwl:unwrapLinks($link)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$link"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="d:ulink" name="ulink">
        <xsl:param name="url" select="@url"/>
        <xsl:variable name="link">
            <object>
                <xsl:attribute name="classid">
                    <xsl:text>java:org.netbeans.modules.javahelp.BrowserDisplayer</xsl:text>
                </xsl:attribute>
                <param>
                    <xsl:attribute name="name">
                        <xsl:text>content</xsl:text>
                    </xsl:attribute>
                    <xsl:attribute name="value">
                        <xsl:value-of select="$url"/>
                    </xsl:attribute>
                </param>
                <param>
                    <xsl:attribute name="name">
                        <xsl:text>text</xsl:text>
                    </xsl:attribute>
                    <xsl:attribute name="value">
                        <xsl:choose>
                            <xsl:when test="count(child::node())=0">
                                <xsl:value-of select="$url"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:apply-templates/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                </param>
                <param>
                    <xsl:attribute name="name">
                        <xsl:text>textColor</xsl:text>
                    </xsl:attribute>
                    <xsl:attribute name="value">
                        <xsl:text>blue</xsl:text>
                    </xsl:attribute>
                </param>
            </object>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="function-available('suwl:unwrapLinks')">
                <xsl:copy-of select="suwl:unwrapLinks($link)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$link"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="longdesc.uri"> 
        <xsl:param name="mediaobject" select="."/> 
        <xsl:if test="$html.longdesc"> 
            <xsl:if test="$mediaobject/d:textobject[not(d:phrase)]"> 
                <xsl:variable name="dbhtml.dir"> 
                    <xsl:call-template name="dbhtml-dir"/> 
                </xsl:variable> 
                <xsl:variable name="filename"> 
                    <xsl:choose> 
                        <xsl:when test=" 
                $mediaobject/@*[local-name() = 'id']
                and not($use.id.as.filename = 0)"> 
                <!-- * if this mediaobject has an ID, then we use the --> 
                <!-- * value of that ID as basename for the "longdesc" --> 
                <!-- * file (that is, without prepending an "ld-" too it) --> 
                            <xsl:value-of select="$mediaobject/@*[local-name() = 'id']"/> 
                            <xsl:value-of select="$html.ext"/> 
                        </xsl:when> 
                        <xsl:otherwise> 
                <!-- * otherwise, if this mediaobject does not have an --> 
                <!-- * ID, then we generate an ID... --> 
                            <xsl:variable name="image-id"> 
                                <xsl:call-template name="object.id"> 
                                    <xsl:with-param name="object" select="$mediaobject"/> 
                                </xsl:call-template> 
                            </xsl:variable> 
                <!-- * ...and then we take that generated ID, prepend an --> 
                <!-- * "ld-" to it, and use that as the basename for the file --> 
                            <xsl:value-of select="concat('ld-',$image-id,$html.ext)"/> 
                        </xsl:otherwise> 
                    </xsl:choose> 
                        <!--</xsl:with-param> 
                    </xsl:call-template>-->
                </xsl:variable> 
 
                <xsl:value-of select="$filename"/> 
            </xsl:if> 
        </xsl:if> 
    </xsl:template>

    <xsl:template name="navig.content">
        <xsl:param name="direction" select="next"/>
        <xsl:variable name="navtext">
            <xsl:choose>
                <xsl:when test="$direction = 'prev'">
                    <xsl:call-template name="gentext.nav.prev"/>
                </xsl:when>
                <xsl:when test="$direction = 'next'">
                    <xsl:call-template name="gentext.nav.next"/>
                </xsl:when>
                <xsl:when test="$direction = 'up'">
                    <xsl:call-template name="gentext.nav.up"/>
                </xsl:when>
                <xsl:when test="$direction = 'home'">
                    <xsl:call-template name="gentext.nav.home"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>xxx</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="$navig.graphics != 0">
                <img>
                    <xsl:attribute name="src">
                        <xsl:value-of select="$navig.graphics.path"/>
                        <xsl:value-of select="$direction"/>
                        <xsl:value-of select="$navig.graphics.extension"/>
                    </xsl:attribute>
                    <xsl:attribute name="alt">
                        <xsl:value-of select="$navtext"/>
                    </xsl:attribute>
                    <xsl:attribute name="border">
                        <xsl:value-of select="0"/>
                    </xsl:attribute>
                </img>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$navtext"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="write.longdesc"> 
        <xsl:param name="mediaobject" select="."/> 
        <xsl:if test="$html.longdesc != 0 and $mediaobject/d:textobject[not(d:phrase)]"> 
            <xsl:variable name="filename"> 
                <xsl:call-template name="longdesc.uri"> 
                    <xsl:with-param name="mediaobject" select="$mediaobject"/> 
                </xsl:call-template> 
            </xsl:variable> 
 
            <xsl:value-of select="$filename"/> 
 
            <xsl:call-template name="write.chunk"> 
                <xsl:with-param name="filename" select="concat($base.dir,$filename)"/> 
                <xsl:with-param name="quiet" select="$chunk.quietly"/> 
                <xsl:with-param name="content"> 
                    <xsl:call-template name="user.preroot"/> 
                    <html> 
                        <head> 
                            <xsl:call-template name="system.head.content"/> 
                            <xsl:call-template name="head.content"> 
                                <xsl:with-param name="title" select="'Long Description'"/> 
                            </xsl:call-template> 
                            <xsl:call-template name="user.head.content"/> 
                        </head> 
                        <body> 
                            <xsl:call-template name="body.attributes"/> 
                            <xsl:for-each select="$mediaobject/d:textobject[not(d:phrase)]"> 
                                <xsl:apply-templates select="./*"/> 
                            </xsl:for-each> 
                        </body> 
                    </html> 
                    <xsl:value-of select="$chunk.append"/> 
                </xsl:with-param> 
            </xsl:call-template> 
        </xsl:if> 
    </xsl:template> 
 
    <xsl:template match="d:book" mode="jhtoc">
        <xsl:variable name="id">
            <xsl:call-template name="object.id"/>
        </xsl:variable>
        <xsl:variable name="title">
            <xsl:apply-templates select="." mode="title.markup"/>
        </xsl:variable>

        <tocitem target="{$id}">
            <xsl:attribute name="text">
                <xsl:value-of select="normalize-space($title)"/>
            </xsl:attribute>
            <xsl:apply-templates select="d:part|d:reference|d:preface|d:chapter|d:appendix|d:colophon|d:glossary|d:bibliography"
                         mode="jhtoc"/>
        </tocitem>
    </xsl:template>

    <xsl:template match="d:part|d:reference|d:preface|d:chapter|d:appendix"
              mode="jhtoc">
        <xsl:variable name="id">
            <xsl:call-template name="object.id"/>
        </xsl:variable>
        <xsl:variable name="title">
            <xsl:apply-templates select="." mode="title.markup"/>
        </xsl:variable>

        <tocitem target="{$id}">
            <xsl:attribute name="text">
                <xsl:value-of select="normalize-space($title)"/>
            </xsl:attribute>
            <xsl:apply-templates
      select="d:preface|d:chapter|d:appendix|d:refentry|d:section|d:sect1|d:glossary|d:bibliography"
      mode="jhtoc"/>
        </tocitem>
    </xsl:template>
    
    <xsl:template name="division.toc">
        <xsl:param name="toc-context" select="."/>

        <xsl:call-template name="make.toc">
            <xsl:with-param name="toc-context" select="$toc-context"/>
            <xsl:with-param name="nodes" select="d:part|d:reference
                                         |d:preface|d:chapter|d:appendix
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
                                         |.//bridgehead[@renderas='sect1'
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
                                        <a>
                                            <xsl:attribute name="accesskey">
                                                <xsl:text>p</xsl:text>
                                            </xsl:attribute>
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
                                            <a>
                                                <xsl:attribute name="accesskey">
                                                    <xsl:text>u</xsl:text>
                                                </xsl:attribute>
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
                                        <a>
                                            <xsl:attribute name="accesskey">
                                                <xsl:text>n</xsl:text>
                                            </xsl:attribute>
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
                                            <a>
                                                <xsl:attribute name="accesskey">
                                                    <xsl:text>h</xsl:text>
                                                </xsl:attribute>
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
                                        <a>
                                            <xsl:attribute name="accesskey">
                                                <xsl:text>t</xsl:text>
                                            </xsl:attribute>
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

    <xsl:template match="d:programlisting|d:screen|d:synopsis">
        <xsl:param name="suppress-numbers" select="'0'"/>
        <xsl:variable name="id">
            <xsl:call-template name="object.id"/>
        </xsl:variable>

        <xsl:call-template name="anchor"/>

        <xsl:variable name="div.element">
            <xsl:choose>
                <xsl:when test="$make.clean.html != 0"><xsl:text>div</xsl:text></xsl:when>
                <xsl:otherwise><xsl:text>textarea</xsl:text></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:if test="$shade.verbatim != 0">
            <xsl:message>
                <xsl:text>The shade.verbatim parameter is deprecated. </xsl:text>
                <xsl:text>Use CSS instead,</xsl:text>
            </xsl:message>
            <xsl:message>
                <xsl:text>for example: pre.</xsl:text>
                <xsl:value-of select="local-name(.)"/>
                <xsl:text> { background-color: #E0E0E0; }</xsl:text>
            </xsl:message>
        </xsl:if>

        <xsl:choose>
            <xsl:when test="$suppress-numbers = '0'
                    and @linenumbering = 'numbered'
                    and $use.extensions != '0'
                    and $linenumbering.extension != '0'">
                <xsl:variable name="rtf">
                    <xsl:choose>
                        <xsl:when test="$highlight.source != 0">
                            <xsl:call-template name="apply-highlighting"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:apply-templates/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:element name="{$div.element}">
                    <xsl:apply-templates select="." mode="common.html.attributes"/>
                    <xsl:if test="@width != ''">
                        <xsl:attribute name="width">
                            <xsl:value-of select="@width"/>
                        </xsl:attribute>
                    </xsl:if>
                    <xsl:call-template name="number.rtf.lines">
                        <xsl:with-param name="rtf" select="$rtf"/>
                    </xsl:call-template>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="{$div.element}">
                    <xsl:apply-templates select="." mode="common.html.attributes"/>
                    <xsl:if test="@width != ''">
                        <xsl:attribute name="width">
                            <xsl:value-of select="@width"/>
                        </xsl:attribute>
                    </xsl:if>
                    <xsl:attribute name="cols">
                        <xsl:value-of select="60"/>
                    </xsl:attribute>
                    <xsl:attribute name="rows">
                        <xsl:value-of select="6"/>
                    </xsl:attribute>
                    <xsl:attribute name="readonly">
                        <xsl:text>true</xsl:text>
                    </xsl:attribute>
                    <xsl:attribute name="wrap">
                        <xsl:text>off</xsl:text>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="$highlight.source != 0">
                            <xsl:call-template name="apply-highlighting"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:apply-templates/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
