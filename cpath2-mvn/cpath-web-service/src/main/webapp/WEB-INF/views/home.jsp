<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content='<fmt:message key="cpath2.provider"/>'/>
	<meta name="description" content="cPath2::Demo (version ${project.version})" />
	<meta name="keywords" content="<fmt:message key="cpath2.provider"/>, cPath2, cPathSquared webservice, help, demo, documentation" />
	<title>cPath2::Demo</title>
	<!-- JQuery plugins -->
	<script type="text/javascript" src="<c:url value="/resources/plugins/jquery-1.6.1.js" />"></script>
	<script type="text/javascript" src="<c:url value="/resources/plugins/jquery-ui-1.8.11.custom.min.js" />"></script>
	<link rel="stylesheet" href="<c:url value="/resources/plugins/jquery-ui-1.8.11.custom.css" />" type="text/css"/>
	<link rel="stylesheet" href="<c:url value="/resources/plugins/jquery.ui.override.css" />" type="text/css"/>
	<!-- other -->
	<script type="text/javascript" src="<c:url value="/resources/scripts/json.min.js" />"></script>
	<script type="text/javascript" src="<c:url value="/resources/scripts/help.js" />"></script>
	<link rel="stylesheet" href="<c:url value="/resources/css/andreas08.css" />" type="text/css" media="screen,projection" />
</head>

<body>

<span id="cpath2_endpoint_url" style="display:none"><c:url value="/"/></span>
  
<!-- place the content -->
  <div id="container">
   <jsp:include page="header.jsp" flush="true"/>
    <div id="content">
	  <!-- Name and description of this cpath2 instance is taken from the $CPATH2_HOME/webdoc.properties -->
      <h2><fmt:message key="cpath2.provider"/></h2>
      <p>
      <fmt:message key="cpath2.description"/>
      </p>
	  <p>Data is freely available, under the license terms of each contributing database.</p>
	  <!-- start of web service api documentation -->
      <h2>Web Service API:</h2>
      <p>You can programmatically access the data within Pathway Commons 
using the Pathway Commons Web Service Application Programming Interface (API).  
This page provides a reference guide to help you get started.</p>

	  <!-- list of web service commands -->
	  <ol>
		<li><a href="#search">Command: SEARCH</a></li>
		<li><a href="#get">Command: GET</a></li>
		<li><a href="#graph">Command: GRAPH</a></li>
		<li><a href="#traverse">Command: TRAVERSE</a></li>
		<li><a href="#top_pathways">Command: TOP_PATHWAYS</a></li>
		<li><a href="#help">Command: HELP</a></li>
	  </ol>
	  
	  <a href="#additional_parameters">Available Data and Parameter Values</a><br/>
	  <a href="#errors">Error Codes</a><br/>
	  
	  <br/>
	  
	  <!-- URIs -->
      <h3><a name="miriam"></a>Note about using URIs:</h3>
	  <p>Some of web service commands require a parameter value to be valid, 
existing URI; it is either an original data provider's URI (for most BioPAX 
Entities) or a <a href="http://identifiers.org">Identifiers.org</a> URL that 
we create for BioPAX UtilityClass objects, such as ProteinReference, 
whenever it (<a href="http://code.google.com/p/pathway-commons/wiki/cPath2PreMerge?ts=1346339836&updated=cPath2PreMerge#Normalization">normalization</a>) 
is possible (we do our best following <a href="http://www.ebi.ac.uk/miriam/main/">MIRIAM's</a> 
set of <a href="http://biomodels.net/miriam/">guidelines</a> for the 
annotation and curation of computational models.)</p>
	  <h3><a name="enco"></a>Note about URL-encoding (a browser issue):</h3>
	  <p>All HTTP GET query arguments, including URIs, must be URL-encoded, 
and this is, fortunately, done automatically when a standard web client 
library is used; but web browsers do not always do this for you. Usually, 
a web app takes care of building and sending proper GET or POST HTTP 
requests to the server, and users are not supposed to type a web service 
query and manually encode parameters in the browser's address line. 
For example, a web browser can replace spaces with "%20" (or '+'), but it will 
not encode '%' in "?uri=urn:biopax:RelationshipXref:HGNC_HGNC%3A13101", so
you must replace % with %25; same thing - about the sharp/pound sign ('#'- %23).
Also, URIs are case-sensitive.</p>

	  <!-- command bodies -->	 
	 <ol> 	  
	  <!-- search command -->
	  <li>
	  <h2><a name="search"></a>Command: SEARCH</h2>
	  <h3>Summary:</h3>
Full text search in Pathway Commons using keywords, phrases, a Lucene query. 
For example, find records that contain the keyword "BRCA2" (in any index 
field), or, more specifically, - "BRCA2" in a Protein's 'name' (index) field. 
BioPAX entity or utility class hits that match the search criteria and pass 
filters are ranked and returned. Search (index) field names are 
(case-sensitive): <em>comment, ecnumber, keyword, name, pathway, term, xrefdb, 
xrefid, dataSource, and organism</em>. The latter two were introduced mainly 
for filtering as un_tockenized, no wildcard/fuzzy support. (Note: index fields 
are not exactly BioPAX properties!) To search within a specific BioPAX class 
only, use the 'type' parameter (filter). Full-text search is a very powerful 
feature and can save a lot of time, but it's not a replacement for a more 
accurate BioPAX graph analysis, i.e., - using other web service commands (see 
below), following official BioPAX semantics. (The more "advanced" search query 
is the more cool but uncertain/error-prone result is.) 
	  <h3>Parameters:</h3>
	  <ul>
		<li><em>q=</em> [Required] a keyword, name, external identifier, or a search string (Lucene syntax).</li>
		<li><em>page=N</em> [Optional] (N&gt;=0, default is 0), search result page number.  See below (&quote;Output&quote; section) for details.</li>
		<li><em>datasource=</em> [Optional] data source filter (<a href="#available_datasource_parameter">values</a>). Multiple data source values are allowed per query; for example, <em>datasource=reactome&amp;datasource=pid</em> means: we want data from Reactome OR NCI_Nature (PID)</li>
		<li><em>organism=</em> [Optional] organism filter. Multiple organisms are allowed per query; for example 'organism=9606&amp;organism=10016' (which means either 9606 or 10016; can also use "homo sapiens", "mus musculus" instead).</li>
		<li><em>type=</em> [Optional] BioPAX class filter (<a href="#available_biopax_parameter">values</a>)</li>
	  </ul>
	  <h3>Output:</h3>
XML result that follows the <a href="resources/schemas/cpath2.xsd.txt">Search Response XML Schema</a>.<br/> 
JSON is returned by appending '.json' to the query URL.  The server returns up to 
'numHitsPerPage' search hits per request (configured on the server).  
But you can request hits beyond the first N, if any, using the 'page' 
parameter.  The 'numHits' attribute in the returned XML or JSON contains the 
total number of search results that matched the query and filters. If a page 
is requested beyond the total number of results, "empty result" error xml is 
returned. In other words, when numHits &gt; numHitsPerPage hits, 'page=n' 
(n&gt;=0) parameter is to get hits ranked from numHitsPerPage*n to numHitsPerPage*(n+1)-1. 
Total no. pages can be also calculated as<br/> INT[(numHits-1)/numHitsPerPage+1].
	  <h3>Example Queries:</h3>
	  <br/>
	  <ol>
	  <li><a href="search.xml?q=Q06609">search for "Q06609" keyword, no filters, return XML result</a></li>
	  <li><a href="search?q=xrefid:Q06609">specific search for "Q06609" in the 'xrefid' index field, no filters, return XML (default)</a></li>
	  <li><a href="search.json?q=Q06609">search for "Q06609" keyword, no filters, return JSON result</a></li>
	  <li><a href="search.json?q=Q06609&type=pathway">search for Pathways containing "Q06609" (search all fields), return JSON</a></li>
	  <li><a href="search?q=brca2&type=proteinreference&organism=homo%20sapiens&datasource=pid">search for ProteinReference containing "brca2" (case-insensitive) keyword (in any field), filter by organism (human) and datasource (NCI_Nature actually)</a></li>
	  <li><a href="search?q=brc*&type=control&organism=9606&datasource=reactome">search for Control interactions matching "brca*" (wildcard, case-insensitive, in any field), originated from Reactome, Human</a></li>
	  <li><a href="search?q=a*&page=3">use of pagination: get the forth page (page=3) hits</a></li>
	  <li><a href="search?q=+binding%20NOT%20transcription*&type=control&page=0">search for Control interactions having something to do with "binding" but not "transcription" (gets the first page hits)</a></li>
	  <li><a href="search?q=pathway:immune&type=conversion">search for Conversion interactions that are direct or indirect participants of a "immune" (part of its name) pathway (cool!)</a></li>
	  <li><a href="search?q=*&type=pathway&datasource=panther">find all Panther pathways</a></li>
	  <li><a href="search?q=*&type=biosource">all organisms (i.e., including BioSource objects referenced from evidence/infection data there)</a></li>
	  </ol>
	  </li>
	  <!-- get command -->
	  <li>
	  <h2><a name="get"></a>Command: GET</h2>
	  <h3>Summary:</h3>
Retrieves details regarding one or more records, such as pathway, interaction 
or physical entity.  For example, get the complete Apoptosis pathway from Reactome.
	  <h3>Parameters:</h3>
	  <ul>
		<li><em>uri=</em> [Required]  a BioPAX element ID (RDF ID; for utility 
classes that have been "normalized", such as xrefs, entity refereneces and 
controlled vocabularies, it is usually a Idntifiers.org URL.  Multiple IDs are 
allowed per query, for example 'uri=http://identifiers.org/uniprot/Q06609&amp;uri=http://identifiers.org/uniprot/Q549Z0'. 
See <a href="#miriam">a note about MIRIAM and Identifiers.org</a>.</li>
		<li><em>format=</em> [Optional] output format (<a href="#available_output_parameter">values</a>)</li>
	  </ul>
	  <h3>Output:</h3>
By default, a complete BioPAX representation for the 
record pointed to by the given uri.  Other output formats are available as 
specified by the optional format parameter.  Please be advised that not all 
output formats are relevant for this web service.  For example, it would not 
make sense to request BINARY_SIF output when the given URI points to a protein.
	  <h3>Example Queries:</h3><br/>
	  <ol>
	  <li><a href="get?uri=http://identifiers.org/uniprot/Q06609">
		get a self-consistent BioPAX sub-model using URI=http://identifiers.org/uniprot/Q06609 (the ProteinReference and dependent objects)</a>
	  </li>
	  <li><a href="get?uri=http://www.reactome.org/biopax/48887Pathway137">
		get a Reactome "pathway" (normalized by the server, as usual)</a>
	  </li>
	  <li><a href="get?uri=http://pid.nci.nih.gov/biopaxpid_74716&format=BINARY_SIF">
		get the NCI-Nature Curated BMP signaling pathway in SIF format</a>
	  </li>
	 </ol>
	 </li>
	  
	  <!-- graph command -->
	  <li>
	  <h2><a name="graph"></a>Command: GRAPH</h2>
	  <h3>Summary:</h3>
We implemented several graph theoretic algorithms that take BioPAX data model 
into account, such as shortest path between two proteins. This command 
executes one of graph queries based on the integrated BioPAX network stored 
on our server. For example, get the neighborhood for a particular BRCA1 
protein state or all states. As the original BioPAX data and our import 
pipeline quality improves, graph query results also become more interesting 
and connected. We merge BioPAX data based on our proteins and small molecules 
data warehouse and consistently normalized UnificationXref, EntityReference, 
Provenance, BioSource, and ControlledVocabulary objects when we are absolutely 
sure two objects of the same type are equivalent. By favoring to store 
unmodified original data when in doubt, we try NOT to accidently introduce any 
artifacts or noise to this process and thus limit our users's options in the 
future. Having said that, the really good thing is that anyone (including our 
team) is free and encouraged to independently develop a dedicated web service 
client app, which, e.g., would use a combination of an advanced id-mapping 
tool, our basic search and query services, and any other advanced network 
merge algorithm of ones choice.
	  <h3>Parameters:</h3>
	  <ul>
		<li><em>kind=</em> [Required] graph query (<a href="#available_graph_parameter">values</a>)</li>
		<li><em>source=</em> [Required] source object's URI. Multiple source URIs are allowed per query, for example 'source=uri=http://identifiers.org/uniprot/Q06609&amp;source=uri=http://identifiers.org/uniprot/Q549Z0'. See <a href="#miriam">a note about MIRIAM and Identifiers.org</a>.</li>
		<li><em>target=</em> [Required for PATHSFROMTO graph query] target URI.  Multiple target URIs are allowed per query; for example  'target=uri=http://identifiers.org/uniprot/Q06609&amp;target=uri=http://identifiers.org/uniprot/Q549Z0'. See <a href="#miriam">a note about MIRIAM and Identifiers.org</a>.</li>
        <li><em>direction=</em> [Optional, for NEIGHBORHOOD and COMMONSTREAM] - graph search direction (<a href="#available_direction_parameter">values</a>).</li>
        <li><em>limit=</em> [Optional] graph query search distance limit (default = 1).</li>
        <li><em>format=</em> [Optional] output format (<a href="#available_output_parameter">values</a>)</li>
	  </ul>
	  <h3>Output:</h3>
By default, a complete BioPAX representation of the desired graph query.  
Other output formats are available as specified by the optional format 
parameter.  Please be advised that not all output formats are relevant for 
this web service.  For example, it would not make sense to request BINARY_SIF 
output when the given URI points to a protein.
	  <h3>Example Queries:</h3>
Neighborhood of Col5a1 (O88207, CO5A1_MOUSE): <br/>
	  <ol>
	  <li><a href="graph?source=http://www.reactome.org/biopax/48892Protein3105&kind=neighborhood">
		from the protein's state</a>
	  </li>
	  <li><a href="graph?source=http://identifiers.org/uniprot/O88207&kind=neighborhood">
		from the protein reference, i.e., all its states (found in the BioPAX network(s) on the server)</a>
	  </li>
	  	  <li><a href="graph?source=http://identifiers.org/uniprot/O88207&kind=neighborhood&format=EXTENDED_BINARY_SIF">
		from the same protein reference but using a different output format</a>
	  </li>
	  </ol>
	  </li>
	  
	  <!-- traverse command -->
	  <li>
	  <h2><a name="traverse"></a>Command: TRAVERSE</h2>
	  <h3>Summary:</h3>
Get BioPAX data property values or objects (URIs) using a XPath-like property 
path expression. This command has two parameters. 
	  <h3>Parameters:</h3>
	  <ul>
		<li><em>uri=</em> [Required] a BioPAX element ID (it's like for <a href="#get">'GET' command above</a>). Multiple IDs are allowed (uri=...&amp;uri=...&amp;uri=...).</li>
		<li><em>path=</em> [Required] a BioPAX propery path: like property1[:type1]/property2[:type2]; can also include convenient (unofficial) inverse BioPAX properties, such as xrefOf, componentOf, etc., and abstract types, such as Named, XReferrable, Process, etc.; see <a href="#available_properties_parameter">properties</a>, <a href="#available_inverse_properties_parameter">inverse properties</a>, <a href="http://www.biopax.org/paxtools">Paxtools</a>, org.biopax.paxtools.controller.PathAccessor.</li>
	  </ul>
	  <h3>Output:</h3>
	  XML result that follows the <a href="resources/schemas/cpath2.xsd.txt">Search Response XML Schema</a>&nbsp;(TraverseResponse type; pagination is disabled: returns all values at once)<br/>
	  <h3>Example Queries:</h3>
	  <ol>
	  <li><a href="traverse?uri=http://identifiers.org/uniprot/P38398&path=ProteinReference/organism/displayName">
		for a URI (of a ProteinReference), get the organism's display name</a>
	  </li>
	  <li><a href="traverse?uri=http://identifiers.org/uniprot/P38398&uri=http://identifiers.org/uniprot/Q06609&path=ProteinReference/organism">
		for each URI, get the organism (URI)</a>
	  </li>
	  <li><a href="traverse?uri=http://identifiers.org/uniprot/Q06609&path=ProteinReference/entityReferenceOf:Protein/name">
		get names of all states of RAD51 protein (by its ProteinReference URI, using property path="ProteinReference/entityReferenceOf:Protein/name")</a>
	  </li>
	  <li><a href="traverse?uri=http://identifiers.org/uniprot/P38398&path=ProteinReference/entityReferenceOf:Protein">
		get URIs of states of BRCA1_HUMAN (path="ProteinReference/entityReferenceOf:Protein")</a>
	  </li>	
	  <li><a href="traverse?uri=http://identifiers.org/uniprot/P38398&uri=http://www.reactome.org/biopax/48887Protein2992&uri=http://identifiers.org/taxonomy/9606&path=Named/name">
		get names of several different objects (using abstract type 'Named' from Paxtools API)</a>
	  </li>		  
	  <li><a href="traverse?uri=http://pid.nci.nih.gov/biopaxpid_74716&path=Pathway/pathwayComponent:Interaction/participant/displayName">
		get BMP pathway participants's names (cool, but be careful and not too much excited!)</a>
	  </li>	
	  </ol>	
	  </li>

	  <!-- top_pathways command -->
	  <li>
	  <h2><a name="top_pathways"></a>Command: TOP_PATHWAYS</h2>
	  <h3>Summary:</h3>
Retrieves all "top" pathways (- not exactly in the graph-theoretic sense, but 
- all such pathways that are neither 'controlled' nor 'pathwayComponent' of other processes)
	  <h3>Parameters:</h3>
		no parameters
	  <h3>Output:</h3>
	  XML result that follows the <a href="resources/schemas/cpath2.xsd.txt">
	  Search Response XML Schema</a>&nbsp;(SearchResponse type; pagination is disabled: returns all pathways at once)<br/>
	  <h3>Example Queries:</h3>
	  <ol>
	  <li><a href="top_pathways">
		get top/root pathways (XML)</a>
	  </li>
	  <li><a href="top_pathways.json">
		get top/root pathways (JSON)</a>
	  </li>
	  </ol>
	  </li>	

	  <!-- help command -->
	  <li>
	  <h2><a name="help"></a>Command: HELP</h2>
	  <h3>Summary:</h3>
Finally, this is a RESTful web service, which returns both static and 
instance-specific information, some of which helped create this demo page 
(- the list of types, constants, organisms, and data sources below)
	  <h3>Output:</h3>
	  XML/JSON (if '.json' suffix used) element 'Help' (nested tree); see: <a href="resources/schemas/cpath2.xsd.txt">Search Response XML Schema</a><br/>
	  <h3>Example Queries:</h3>
	  <br/>
	  <ol>
	  <li><a href="help/commands">/help/commands</a></li>
	  <li><a href="help/commands.json">/help/commands.json</a></li>
	  <li><a href="help/commands/search">/help/commands/search</a></li>
	  <li><a href="help/types">/help/types</a></li>
	  <li><a href="help/kinds">/help/kinds</a></li>
	  <li><a href="help/organisms">/help/organisms (note: some of species there listed are only due to they were mentioned in human, mouse, etc., pathway data)</a></li>
	  <li><a href="help/datasources">/help/datasources (data sources loaded into the system)</a></li>
	  <li><a href="help/directions">/help/directions</a></li>
	  <li><a href="help/types/properties">/help/types/properties</a></li>
	  <li><a href="help/types/provenance/properties">/help/types/provenance/properties</a></li>
	  <li><a href="help/types/inverse_properties">/help/types/inverse_properties</a></li>
	  <li><a href="help">/help</a></li>
	  </ol>
	  </li>	  
	  </ol>
	<br/>
	
	  <!-- additional parameter details -->
      <h2 id="additional_parameters">Available Data and Parameter Values</h2>
      
	  <div class="parameters">
	  	<h3>Loaded Data Sources ('datasource'):</h3>
	  	<p>Listed below are URIs of the pathway data archives that we processed and merged into this system 
and their corresponding names. The names are recommended values 
for the filter by data source (used with 'search' command). 
For example, 'NCI_Nature', 'reactome' can be used (case insensitive) there. 
Using URIs for filter values is also possible and allows for more specific filters. 
For example, if a provider's pathway data were actually imported from several physical locations and/or data formats, 
there several URIs below map to the same name, and you may want to search only in some of them. 
Other data source (Provenance) URIs and names "work" not as good as the recommended filter values, 
because those are, in fact, not guaranteed to be associated with all BioPAX entities from the same source. 
		</p>
		<!-- items are to be added here by a javascript -->
	  	<table id="datasources" class="datasource_homepage_table"></table>
	  	<br/>
	  	<p>In order to normalize and merge above data, we also imported and used following warehouse data:</p>
	  	<table id="warehouse" class="datasource_homepage_table"></table>
	  	<br/>
	  </div>      
        
	  <div class="parameters">
	  	<h3>Output formats ('format'):</h3>
	  	<p>See <a href="resources/docs/README.txt">README.txt</a> for more information regarding these format parameters.</p>
		<!-- items are to be added here by a javascript -->
	  	<ul id="output_parameter"></ul>
	  	<br/>
	  </div> 
	  
	  <div class="parameters">
	  	<h3>Built-in graph queries ('kind'):</h3>
		<!-- items are to be added here by a javascript -->
	  	<ul id="graph_parameter"></ul>
	  	<br/>
	  </div> 
	  
	  <div class="parameters">
	  	<h3>Graph traversal directions ('direction'):</h3>
		<!-- items are to be added here by a javascript -->
	  	<ul id="direction_parameter"></ul>
	  	<br/>
	  </div> 
	  
	  <div class="parameters">
	  	<h3>BioPAX classes ('type'):</h3>
		<!-- items are to be added here by a javascript -->
	  	<ul id="biopax_parameter"></ul>
	  	<br/>
	  </div> 
	  
	  <div class="parameters">
	  	<h3>Official BioPAX Properties and Domain/Range Restrictions:</h3>
	  	<p>Note: "XReferrable xref Xref D:ControlledVocabulary=UnificationXref 
	  	D:Provenance=UnificationXref,PublicationXref" means XReferrable.xref, 
	  	and that, for a ControlledVocabulary.xref, the value can only be of 
	  	UnificationXref type, etc.</p>
		<!-- items are to be added here by a javascript -->
	  	<ul id="properties_parameter"></ul>
	  	<br/>
	  </div> 
	  
	  <div class="parameters">
	  	<h3>Inverse BioPAX Object Properties and Domain/Range Restrictions (useful feature of Paxtools API):</h3>
	  	<p>Note: Some of object range BioPAX properties can be reversed/inversed 
	  	(and also used in the '/traverse?path=' context), e.g, 'xref' - 'xrefOf'. 
	  	These are listed below. But, e.g., unlike the normal xref property, 
	  	the same restriction ("XReferrable xref Xref D:ControlledVocabulary=UnificationXref 
	  	D:Provenance=UnificationXref,PublicationXref") must read/comprehend differently: 
	  	it's actually now means Xref.xrefOf, and that RelationshipXref.xrefOf 
	  	cannot contain a ControlledVocabulary (or its sub-class) values, etc.</p>
		<!-- items are to be added here by a javascript -->
	  	<ul id="inverse_properties_parameter"></ul>
	  	<br/>
	  </div>
	  
	  <div class="parameters">
	  	<h3>Official BioPAX Properties and Domain/Range Restrictions:</h3>
	  	<p>Note: "XReferrable xref Xref D:ControlledVocabulary=UnificationXref 
	  	D:Provenance=UnificationXref,PublicationXref" means XReferrable.xref, 
	  	and that, for a ControlledVocabulary.xref, the value can only be of 
	  	UnificationXref type, etc.</p>
		<!-- items are to be added here by a javascript -->
	  	<ul id="properties_parameter"></ul>
	  	<br/>
	  </div> 
	  
	  <!-- error codes -->
	  <h2 id="errors">Error Codes:</h2>
	  <p>An error while processing a request is reported as an XML document (ErrorResponse) 
	  with information about the error cause as defined in the<a href="resources/schemas/cpath2.xsd.txt">
	  Search Response XML Schema</a></p>
	  <p>Only the first error encountered is reported. The table below provides a list of error codes, with their descriptions.</p>
	  <div>
		<table>
		  <tr><th>Error Code</th><th>Error Description</th></tr>
		  <tr><td>450</td><td>Bad Command (command not recognized)</td></tr>
		  <tr><td>452</td><td>Bad Request (missing arguments)</td></tr>
		  <tr><td>460</td><td>No Results Found</td></tr>
		  <tr><td>500</td><td>Internal Server Error</td></tr>
		</table>		
	  </div>
	  <br/>
</div>

   <jsp:include page="footer.jsp" flush="true"/>
  </div>
</body>

</html>
