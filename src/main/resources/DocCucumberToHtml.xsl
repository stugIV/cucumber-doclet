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
						
						<xsl:apply-templates select="CLASS"  mode="script" />
				
						function viewPhrase(result, annotation) {
							result.empty();
							newH3 = document.createElement( "h3" );
							if (annotation.deprecated) {
								newS = document.createElement( "s" );
								newS.append(annotation.phrases[0]);
								newH3.append(newS);
							} else {
								newH3.append(annotation.phrases[0]);
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
							
							newP = document.createElement( "p" );
							newU = document.createElement( "u" );
							newB = document.createElement( "b" );
							newB.append("List of possible phrases :");
							newU.append(newB);
							newP.append(newU)				
							result.append(newP);
							
							newUL = document.createElement( "ul" );
							annotation.phrases.forEach(function(element) {
								newLI = document.createElement( "li" );
								newLI.append(element);
								newUL.append(newLI);
							});
							result.append(newUL);
							
							newP = document.createElement( "p" );
							newU = document.createElement( "u" );
							newB = document.createElement( "b" );
							newB.append("Example :");
							newU.append(newB);
							newP.append(newU)
							result.append(newP);
							
							newDIV = document.createElement( "div" );
							newDIV.id = "divExample";
							newP = document.createElement( "p" );
							annotation.examples.forEach(function(example) {
								newBR = document.createElement( "br" );
								newP.append(example.replace(/ /g, "&#160;"));
								newP.append(newBR);
							});
							newDIV.append(newP);
							result.append(newDIV);			
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
						
					} );
				</script>
				<style>
					.ui-menu { width: 50em; }
					.ui-widget-header { padding: 0.2em; }
					#phrases { width:80em; }
					#tabs { height:46em; }
					#divMenu { float:left; height: 37em; overflow-y:auto;overflow-x:hidden; }
					#divDetail { float:left; border: 1px black solid; margin: 0 1em 0 1em; padding:0 0.5em 0 0.5em;width:41em;overflow-x:hidden; }
					#divDetailResearch { border: 1px black solid; margin: 1em 1em 0 1em; padding:0 0.5em 0 0.5em;overflow-x:hidden; }
					#divExample { font-family:Courier New; font-size:small; }
				</style>
			</head>
			<body>
				<div id="tabs">
					<ul>
						<li><a href="#tabs-1">Search</a></li>
						<li><a href="#tabs-2">List</a></li>
					</ul>
					<div id="tabs-1" class="ui-widget">
						<label for="phrases">Search a sentence : </label>
  						<input id="phrases" />
						<div id="divDetailResearch" />
					</div>
					<div id="tabs-2">
						<h1>List of executable sentences</h1>
						<div id="divMenu">
							<ul id="menu">
								<xsl:apply-templates select="CLASS"  mode="html" />
							</ul>
						</div>
						<div id="divDetail" />
					</div>
				</div>
			</body>
		</html>
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
	<xsl:param name="classPosition" />
		<li><xsl:attribute name="id">idAnnotation-<xsl:value-of select="$classPosition" />-<xsl:value-of select="position()" /></xsl:attribute>
			<xsl:apply-templates select="ANNOTATION"  mode="html" />
		</li>
	</xsl:template>
	<xsl:template match="ANNOTATION" mode="html">
		<xsl:variable name= "phraseName">
			<b><xsl:value-of select="@name" /></b>
			&#160;
			<i><xsl:value-of select="translate(PHRASE,'\','')" /></i>
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

	<xsl:template match="CLASS" mode="script">
		<xsl:apply-templates select="FUNCTION" mode="script">
			<xsl:with-param name="classPosition" select="position()" />
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template match="FUNCTION" mode="script">
	<xsl:param name="classPosition" />
		<xsl:apply-templates select="ANNOTATION" mode="script">
			<xsl:with-param name="idAnnotation" select="concat('idAnnotation-',$classPosition,'-',position())" />
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
		<xsl:apply-templates select="../TAG[@name='example']" mode="script" />
		<xsl:apply-templates select="PARAM" mode="script" />
		annotation.label = "<xsl:value-of select="concat(@name,' ',@phrase)" />";
		annotation.id = "<xsl:value-of select="$idAnnotation" />";
		annotation.label = "<xsl:value-of select="concat(@name,' ',PHRASE)" />";
		annotations[annotation.id] = annotation;
		<xsl:apply-templates select="PHRASE" mode="script" />		
	</xsl:template>
	<xsl:template match="COMMENT" mode="script">
		<xsl:for-each select="LINE">
			annotation.comments.push("<xsl:value-of select="." />");
		</xsl:for-each> 
	</xsl:template>
	<xsl:template match="TAG" mode="script">
		<xsl:for-each select="LINE">
			annotation.examples.push("<xsl:value-of select="." />");
		</xsl:for-each> 
	</xsl:template>
	<xsl:template match="PARAM" mode="script">
		var param = new Param;
		param.label = "<xsl:value-of select="@name" />;
		param.type = "<xsl:value-of select="@type" />;
		annotation.params.push(param);
	</xsl:template>
	<xsl:template match="PHRASE" mode="script">
		annotation.phrases.push("<xsl:value-of select="concat(../@name,' ', .)" />");
	</xsl:template>

</xsl:stylesheet>