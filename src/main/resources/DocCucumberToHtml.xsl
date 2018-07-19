<?xml version="1.0" encoding="UTF-8" ?>

<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="html" version="5.0" encoding="utf-8"
		indent="yes" omit-xml-declaration="yes" doctype-system="about:legacy-compat" />

	<xsl:template match="/">
		<xsl:apply-templates select="JAVADOC" />
	</xsl:template>

	<xsl:template match="JAVADOC">
		<html lang="en">
			<head>
				<meta charset="utf-8" />
				<meta name="viewport" content="width=device-width, initial-scale=1" />
				<title>List of all executable sentences</title>
				<link rel="stylesheet"
					href="https://code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css" />
				<link rel="stylesheet" href="/resources/demos/style.css" />
				<script src="https://code.jquery.com/jquery-1.12.4.js"></script>
				<script src="https://code.jquery.com/ui/1.12.1/jquery-ui.js"></script>
				<script>
					$( function() {
						function Annotation () {
							this.id = "";
							this.comments = new Array();
							this.examples = new Array();
							this.class = "";
							this.function = "";
							this.params = new Array();
							this.label = "";
  							this.phrases = new Array();
  							this.deprecated = false;
						}
						function Param () {
							this.label = "";
							this.type = "";
						}

						var annotations = new Array();
						
						<xsl:apply-templates select="GROUP"  mode="script" />
				
						function viewPhrase(result, annotation) {
							result.empty();
							newH3 = document.createElement( "h3" );
							if (annotation.deprecated) {
								newS = document.createElement( "s" );
								newS.append(annotation.phrases[0]);
								newH3.append(newS);
							} else {
								newH3.append(annotation.label);
							}
							result.append(newH3);

							newP = document.createElement( "p" );
							annotation.comments.forEach(function(comment) {
								newI = document.createElement( "i" );
								newBR = document.createElement( "br" );
								newI.append(comment);
								newP.append(newI);
								newP.append(newBR);
							});
							result.append(newP);

							paramsP = document.createElement( "p" );
							paramsU = document.createElement( "u" );
							paramsB = document.createElement( "b" );
							paramsB.append("Params :");
							paramsU.append(paramsB);
							paramsP.append(paramsU)
							result.append(paramsP);
							
							paramsDIV = document.createElement( "div" );
							paramsDIV.id = "divParams";
							paramP = document.createElement( "p" );
							annotation.params.forEach(function(param) {
								paramBR = document.createElement( "br" );
								paramB = document.createElement("b")
								paramB.append(param.label);
								paramP.append(paramB)
								paramP.append(" - ");
								paramP.append(param.description);
								paramP.append(paramBR);
							});
							paramsDIV.append(paramP);
							result.append(paramsDIV);

							exampleP = document.createElement( "p" );
							exampleU = document.createElement( "u" );
							exampleB = document.createElement( "b" );
							exampleB.append("Example :");
							exampleU.append(exampleB);
							exampleP.append(exampleU)
							result.append(exampleP);
							
							exampleDIV = document.createElement( "div" );
							exampleDIV.id = "divExample";
							newP = document.createElement( "p" );
							annotation.examples.forEach(function(example) {
								newBR = document.createElement( "br" );
								newP.append(example.replace(/ /g, "&#160;"));
								newP.append(newBR);
							});
							exampleDIV.append(newP);
							result.append(exampleDIV);
						}
				
						$( "#tabs" ).tabs();
						
					    $( "#phrases" ).autocomplete({
						      source: Object.values(annotations),
      						      select: function( event, ui ) {
											viewPhrase($( "#divDetailResearch" ), annotations[ui.item.id]);
										$(this).val(''); return false;
								 }
						})
						
						$( "#menu" ).menu(
							{ items: "> :not(.ui-widget-header)"},
							{ select: function( event, ui ) {
								viewPhrase($( "#divDetail" ), annotations[ui.item.context.id]); }
							}
						);
						$(".ui-menu-item-wrapper").on('click', function(e) {
							$('#divDetail').remove();
							popup = $('<div id="divDetail"></div>');
							target = this.id;
							$(this).append(popup);
						});
					} );
				</script>
				<style>
					#menu .ui-widget-header,
					#menu .ui-menu-item-wrapper {
						padding: 5px 20px;
						font-size: 100%;
					}
					.ui-menu li.ui-menu-item:nth-child(2n+1) {
						background-color: rgba(0,0,0,.03);
					}
					#divDetail:not(:empty) {
						border: 1px solid #dbd3b8;
						background: #fff7db;
						color: #000;
						padding: 0 20px;
						margin: 5px 0 20px;
						border-radius: 3px;
						box-shadow: 0 2px 3px rgba(0, 0, 0, .2);
					}
				</style>
			</head>
			<body>
				<div id="tabs">
					<ul>
						<li><a href="#tabs-1">List</a></li>
						<li><a href="#tabs-2">Search</a></li>
                        <!--<li><a href="#tabs-Person">Person</a></li>-->
						<xsl:apply-templates select="GROUP" mode="tabs"/>
					</ul>
					<div id="tabs-1" class="ui-widget">
						<label for="phrases">Search a step : </label>
  						<input id="phrases" />
						<div id="divDetailResearch" />
					</div>
					<div id="tabs-2">
						<h1>List of executable sentences</h1>
						<div id="divMenu">
							<ul id="menu">
								<xsl:apply-templates select="GROUP" mode="html" />
							</ul>
						</div>
						<div id="divDetail" />
					</div>
                    <xsl:apply-templates select="GROUP" mode="desc"/>
				</div>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="GROUP" mode="tabs">
		<xsl:if test="@name='Other'">
			<xsl:apply-templates select="CLASS" mode="tabs"/>
		</xsl:if>
	</xsl:template>
	<xsl:template match="CLASS" mode="tabs">
		<li><a><xsl:attribute name="href">#tabs-<xsl:value-of select="@name" /></xsl:attribute>
			<xsl:value-of select="@name"/>
		</a></li>
	</xsl:template>
    <xsl:template match="GROUP" mode="desc">
		<xsl:if test="@name='Other'">
			<xsl:apply-templates select="CLASS" mode="desc"/>
		</xsl:if>
    </xsl:template>
	<xsl:template match="CLASS" mode="desc">
		<div><xsl:attribute name="id">tabs-<xsl:value-of select="@name" /></xsl:attribute>
			<div>
				<h3>Class <xsl:value-of select="@name" /></h3>
				<h4>Description</h4>
				<xsl:value-of select="@description"/>
			</div>
			<br/>
			<xsl:apply-templates select="ENUM" mode="desc"/>
		</div>
	</xsl:template>
	<xsl:template match="ENUM" mode="desc">
		<h4>Possible values</h4>
		<xsl:apply-templates select="FIELD" mode="desc"/>
	</xsl:template>
	<xsl:template match="FIELD" mode="desc">
		<b><xsl:value-of select="@name"/></b> - <xsl:value-of select="@description"/><br/>
	</xsl:template>


	<xsl:template match="GROUP" mode="html">
		<li class="ui-widget-header">
			<div>
				<xsl:value-of select="@name" />
			</div>
		</li>
		<xsl:apply-templates select="CLASS" mode="group">
			<xsl:with-param name="groupPosition" select="position()" />
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template match="CLASS" mode="group">
		<xsl:param name="groupPosition"/>
		<xsl:apply-templates select="FUNCTION" mode="html">
			<xsl:with-param name="groupPosition" select="$groupPosition" />
			<xsl:with-param name="classPosition" select="position()" />
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template match="CLASS" mode="html">
		<li class="ui-widget-header">
			<div>
				<xsl:value-of select="@name" />
			</div>
		</li>
		<xsl:apply-templates select="FUNCTION" mode="html">
			<xsl:with-param name="classPosition" select="position()" />
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template match="FUNCTION" mode="html">
	<xsl:param name="groupPosition" />
	<xsl:param name="classPosition" />
		<li><xsl:attribute name="id">idAnnotation-<xsl:value-of select="$groupPosition" />-<xsl:value-of select="$classPosition" />-<xsl:value-of select="position()" /></xsl:attribute>
			<xsl:apply-templates select="ANNOTATION"  mode="html" />
		</li>
	</xsl:template>
	<xsl:template match="ANNOTATION" mode="html">
		<xsl:variable name= "phraseName">
			<b><xsl:value-of select="@name" /></b>
			&#160;
			<i><xsl:value-of select="@phrase" /></i>
		</xsl:variable>
		<div>
			<xsl:choose>
			  <xsl:when test="../@Deprecated">
			    <s><xsl:copy-of select="$phraseName" /></s>
			  </xsl:when>
			  <xsl:otherwise>
			    <xsl:copy-of select="$phraseName" />
			  </xsl:otherwise>
			</xsl:choose> 
		</div>
	</xsl:template>

	<xsl:template match="GROUP" mode="script">
		<xsl:apply-templates select="CLASS" mode="script">
			<xsl:with-param name="groupPosition" select="position()" />
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template match="CLASS" mode="script">
		<xsl:param name="groupPosition"/>
		<xsl:apply-templates select="FUNCTION" mode="script">
			<xsl:with-param name="groupPosition" select="$groupPosition" />
			<xsl:with-param name="classPosition" select="position()" />
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template match="FUNCTION" mode="script">
	<xsl:param name="groupPosition" />
	<xsl:param name="classPosition" />
		<xsl:apply-templates select="ANNOTATION" mode="script">
			<xsl:with-param name="idAnnotation" select="concat('idAnnotation-',$groupPosition,'-',$classPosition,'-',position())" />
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template match="ANNOTATION" mode="script">
	<xsl:param name="idAnnotation" />
		var annotation = new Annotation;
		annotation.class = "<xsl:value-of select="../../@name" />";
		annotation.function = "<xsl:value-of select="../@name" />";

	    <xsl:if test="../@Deprecated">
	    	annotation.deprecated = true;
	    </xsl:if>
		
		<xsl:apply-templates select="../COMMENT" mode="script" />
		<xsl:apply-templates select="../TAG" mode="script" />
		<xsl:apply-templates select="PARAM" mode="script" />
		annotation.id = "<xsl:value-of select="$idAnnotation" />";
		annotation.label = "<xsl:value-of select="@phrase" />";
		annotations[annotation.id] = annotation;
		<xsl:apply-templates select="PHRASE" mode="script" />		
	</xsl:template>
	<xsl:template match="COMMENT" mode="script">
		<xsl:for-each select="LINE">
			annotation.comments.push("<xsl:value-of select="." />");
		</xsl:for-each> 
	</xsl:template>
	<xsl:template match="TAG" mode="script">
		<xsl:choose>
			<xsl:when test="@name='param'">
				var param = new Param;
				param.label="<xsl:value-of select="@nameParameter" />";
				<xsl:for-each select="LINE">
					param.description="<xsl:value-of select="." />";
				</xsl:for-each>
				annotation.params.push(param);
			</xsl:when>
			<xsl:otherwise>
				<xsl:for-each select="LINE">
					annotation.examples.push("<xsl:value-of select="." />");
				</xsl:for-each>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template match="PARAM" mode="script">
		var param = new Param;
		param.label = "<xsl:value-of select="@name" />;
		param.type = "<xsl:value-of select="@type" />;
		annotation.params.push(param);
	</xsl:template>
	<xsl:template match="PHRASE" mode="script">
		annotation.phrases.push("<xsl:value-of select="concat(../@phrase,' ', .)" />");
	</xsl:template>

</xsl:stylesheet>