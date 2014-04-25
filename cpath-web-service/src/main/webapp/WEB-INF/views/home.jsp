<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!-- get the root/base URL (e.g., depends on whether the WAR was deployed on a Tomcat 
or the fat JAR with embedded application server was started) -->
<c:set var="req" value="${pageContext.request}" />
<c:set var="uri" value="${req.requestURI}" />
<c:set var="base" value="${fn:replace(req.requestURL, fn:substring(uri, 1, fn:length(uri)), req.contextPath)}" />

<!DOCTYPE html>
<html>
<head>
	<jsp:include page="head.jsp" />
	<title>cPath2::Info</title>
</head>
<body data-spy="scroll" data-target=".navbar">
<jsp:include page="header.jsp"/>

  <h2>Description</h2>
	
  <div class="row nav-target" id="about">
  	<h3>${cpath.name}</h3>
  	<blockquote>
  	<!-- using c:out below to escape all internal html, if any (should not be) -->
	<p><c:out value="${cpath.description}"/></p>
	</blockquote>
  </div>
  	
  <div class="row">	
	<div class="jumbotron">
	<h3>Commands</h3>
	<blockquote><p>To query the integrated biological pathway database, 
	application developers can use the following commands:</p></blockquote>
	<ul id="commands" title="cPath2 Commands (API)">
		<li><a href="#search">SEARCH</a></li>
		<li><a href="#get">GET</a></li>
		<li><a href="#graph">GRAPH</a></li>
		<li><a href="#traverse">TRAVERSE</a></li>
		<li><a href="#top_pathways">TOP_PATHWAYS</a></li>
	</ul>
	<blockquote><p>Please check the availability terms of 
		<a href="datasources">contributing databases</a>.</p>
	</blockquote>
	</div>
  </div>
	
  <div class="row">	
	<h3>Other features</h3>
	<blockquote><p>Software developers who build a service or web site on top of a cPath2 instance 
	may also use the following queries, which return HTML, text, XML, or JSON metadata. 
	Fore more information, please contact us, and see the project's  
	<a href="http://cpath2-site.pathway-commons.googlecode.com/hg/index.html">documentation</a> and 
	<a href="http://cpath2-site.pathway-commons.googlecode.com/hg/cpath-web-service/xref/index.html">web 
	controllers' source code</a>:
	</p></blockquote>
	<ul>
		<li><a href="#idmapping">/idmapping</a> - maps some identifiers to primary UniProt IDs;</li>
		<li><em>/help</em> - returns a tree of Help objects describing the main commands, parameters, 
		BioPAX types, and properties, etc.; e.g., /help/schema, /help/commands, 
		/help/types (one can add .json/.xml extention or set 'Accept' HTTP header to request JSON/XML).</li>
		<li><em>/metadata/*</em> - /metadata/datasources*, /metadata/validations*</li>
		<li><em>/log*</em> - service access summary, such as /log/PROVIDER/geography/world, /log/timeline)</li>
	</ul>
	<p>Everything else, unless it maps to some controller, redirects to
		<a href="#get">GET</a> (by URI) query, e.g., ${base}pid is equivalent to ${base}get?uri=${cpath.xmlBase}pid  
		(along with configuring proper HTTP partial redirect for ${cpath.xmlBase}, 
		this helps making BioPAX URIs resolvable).
	</p>
  </div> <!-- about  -->
  
	<div class="row" id="notes">
	<h3>Notes</h3>
	  <div class="col-sm-6">
		<h4><a id="about_uris"></a>About URIs</h4>
	    <p>
	    Parameters: 'source', 'uri', and 'target' require URIs of existing BioPAX elements, which 
		are either standard <a href="http://identifiers.org" target="_blank">Identifiers.org</a>
		URLs (for most canonical biological entities and controlled vocabularies), or ${cpath.name} 
		generated ${cpath.xmlBase}<em>&lt;localID&gt;</em> URLs (for most Entities and Xrefs).
		Besides, BioPAX object URIs are not something to guess or hit by chance. 
		For example, despite knowing current URI namespace ${cpath.xmlBase} and actual service location ${base}, 
		one should not normally hit ${base}foo, ${cpath.xmlBase}foo, or ${base}get?uri=${cpath.xmlBase}foo 
		unless the corresponding BioPAX individual there exists, which 
		one can find out using <em>search, top_pathways</em>, and other queries (i.e., get some objects of interest first).
		However, HUGO gene symbols, SwissProt, RefSeq, Ensembl, and NCBI gene/protein <strong>identifiers
		are acceptable in place of full URIs</strong> in <em>get</em> and <em>graph</em> queries.
		As a rule of thumb, using full URIs makes a precise query, whereas with identifiers - more exploratory one 
		(which performs id-mapping to UniProt and search for the Xref's URIs).
		</p>
	  </div>
	  <div class="col-sm-6">
		<h4><a id="enco"></a>About Examples</h4>
	 	<p>Normally, instead of submitting a non-trivial URL query via browser's address line, 
	 	one should find or develop a convenient bioinformatic application, such as Cytoscape, PCViz, ChIBE,  
	 	or script that uses the web API and a standard client-side software library. Nevertheless, 
	 	this page intentionally includes web links one can simply click to submit an example query and view results.
		This works because examples are quite simple queries, and parameters, 
		such as long URI or Lucene query string, were properly (manually) URL-encoded. 
		Besides, <strong>consider always using HTTP POST method instead of GET</strong> 
		(to avoid caching, encoding, too long URL, etc. issues). Finally, 
		URIs are case-sensitive and contain no spaces.</p>
	  </div>
	</div>
<hr/>
<div class="row nav-target" id="search">
	<h3>SEARCH:</h3>
	<blockquote><p>
		A full-text search in the BioPAX database using the <a
			href="http://lucene.apache.org/core/3_6_2/queryparsersyntax.html"> Lucene query syntax</a>.
		Index fields (case-sensitive):<em>comment, ecnumber, keyword, name, pathway, term, xrefdb, xrefid, dataSource, organism</em> 
		(some of these are BioPAX properties, while others are composite relationships), can be optionally used in a query string.
		For example, <em>pathway</em> index field helps find pathway participants by keywords that match their parent pathway  
		names or identifiers; <em>xrefid</em> - find objects by matching its direct or attached to a child element Xrefs;
		<em>keyword</em>, the default search field, is a large aggregate that includes all BioPAX properties of an element 
		and nested elements' properties (e.g. a Complex can be found by one of its member's name or EC Number).
		Search results can be filtered by data provider (<em>datasource</em> parameter), <em>organism</em>, 
		and instantiable BioPAX class (<em>type</em>). Search can be used to select starting points for graph traversal queries
		(with '/graph', '/traverse', '/get' commands). Search strings are case insensitive unless put inside quotes.
	</p></blockquote>
	<h4>Returns:</h4> 	
	<p>The specified or first page of the ordered list of BioPAX individuals that match the search criteria
	(the results page size is configured on the server and returned with every result, as attribute). 
	The results (hits) are returned as <a href="help/schema">Search Response XML Schema</a> instance (XML document). 
	JSON format can be requested by using as '/search.json' or setting HTTP 'Accept' header. 
	</p>
	<h4 id="search_parameters">Parameters:</h4>
	<ul>
		<li><em>q=</em> [Required] a keyword, name, external identifier, or a Lucene query string.</li>
		<li><em>page=N</em> [Optional] (N&gt;=0, default is 0), search result page number.
		</li>
		<li><em>datasource=</em> [Optional] filter by data source (use names or URIs 
			of <a href="datasources">pathway data sources</a> or of any existing Provenance object). 
			If multiple data source values are specified, a union of hits from specified sources is returned. For example, 
			<em>datasource=reactome&amp;datasource=pid</em> returns hits associated with Reactome or PID.
		</li>
		<li><em>organism=</em> [Optional] organism filter. The organism can be specified either by official name, e.g.
			"homo sapiens" or by NCBI taxonomy id, e.g. "9606". Similar to data sources, if multiple organisms are
			declared a union of all hits from specified organisms is returned. For example
			'organism=9606&amp;organism=10016' returns results for both human and mice. 
			Note the <a href="#organisms">officially supported species</a>.
		</li>
		<li><em>type=</em> [Optional] BioPAX class filter (<a href="#biopax_types">values</a>)
		</li>
	</ul>
	<h4>Examples:</h4> <br/>
	<ol>
		<li><a rel="example" href="search.xml?q=Q06609">A basic text search. This query returns all entities
			that contain the "Q06609" keyword in XML</a></li>
		<li><a rel="example" href="search.json?q=Q06609"> Same query returned in JSON format</a></li>
		<li><a rel="example" href="search?q=xrefid:Q06609">This query returns entities
			"Q06609" only in the 'xrefid' index field in XML </a></li>
		<li><a rel="example" href="search.json?q=Q06609&type=pathway">Search for
			Pathways containing "Q06609" (search all fields), return JSON</a></li>
		<li><a rel="example"
		       href='search?q=brca2&type=proteinreference&organism=homo%20sapiens&datasource=pid'>Search
			for ProteinReference entries that contain "brca2" keyword in any indexed field, return only human
			proteins from NCI Pathway Interaction Database</a></li>
		<li><a rel="example"
		       href="search.xml?q=name:'col5a1'&type=proteinreference&organism=9606">Similar to search above,
			but searches specifically in the "name" field</a></li>
		<li><a rel="example"
		       href="search?q=brc*&type=control&organism=9606&datasource=reactome">This query
			uses wildcard notation to match any Control interactions that has a word that starts with brca in any of
			its indexed fields. The results are restricted to human interactions from the Reactome database.</a></li>
		<li><a rel="example" href="search?q=a*&page=3">An example use of pagination -- This query returns the
			the forth page (page=3) for all elements that has an indexed word that starts with "a"</a></li>
		<li><a rel="example"
		       href="search?q=+binding%20NOT%20transcription*&type=control&page=0">This query finds Control
			interactions that contain the word "binding" but not "transcription" in their indexed fields, explicitly
			request the first page.</a></li>
		<li><a rel="example" href="search?q=pathway:immune*&type=conversion">This query will find all
			interactions that directly or indirectly participate in a pathway that has a keyword match for "immune"
			. </a></li>
		<li><a rel="example" href="search?q=*&type=pathway&datasource=reactome">This query will return
			all Reactome pathways</a></li>
		<li><a rel="example" href="search?q=*&type=biosource">This query will list all organisms,
			including secondary organisms such as pathogens or model organisms listed in the evidence or
			interaction objects</a></li>
	</ol>
</div>
<hr/>
<div class="row nav-target" id="get">
	<h3>GET:</h3> 
	<blockquote><p>
	Retrieves a sub-model for one or several elements, such as pathway,
	interaction or physical entity, given their URIs. Get commands only retrieves the specified 
	and all the child BioPAX elements (one can use <a href="#traverse">"traverse </a>query 
	to obtain parent elements).</p></blockquote>
	<h4>Parameters:</h4>
	<ul>
		<li><em>uri=</em> [Required] valid/existing BioPAX element's URI 
			(RDF ID; for utility classes that were "normalized", such as entity
			refereneces and controlled vocabularies, it is usually a
			Idntifiers.org URL. Multiple IDs are allowed per query, for
			example, 'uri=http://identifiers.org/uniprot/Q06609&amp;uri=http://identifiers.org/uniprot/Q549Z0'
			<a href="#about_uris">See also</a> about MIRIAM and Identifiers.org.
		</li>
		<li><em>format=</em> [Optional] output format (<a
				href="#output_formats">values</a>)
		</li>
	</ul>
	<h4>Output:</h4> BioPAX (default) representation for the record(s) pointed to by given URI(s) is returned. 
	Other output formats are produced on demand by converting the BioPAX and can be specified by the optional format parameter. 
	Please be advised that with some output formats it might return "no result found" error if the conversion is not applicable 
	to the BioPAX result. For example, BINARY_SIF output makes sense if there are some interactions, complexes, or pathways 
	in the retrieved set.
	<h4>Query Examples:</h4>
	<ol>
		<li><a rel="example" href="get?uri=http://identifiers.org/uniprot/Q06609">
			This command returns the BioPAX representation of http://identifiers.org/uniprot/Q06609</a> 
			(<strong>ProteinReference</strong>)</li>
		<li><a rel="example" href="get?uri=COL5A1">
			This command returns Xref(s) in BioPAX format found by gene symbol COL5A1</a> 		
			<strong>Note:</strong> UniProt, RefSeq, NCBI Gene, and Ensemble identifiers ususally work here too 
			if these, or their corresponding primary UniProt accession, match at least one Xref.id BioPAX property value.</li>
	</ol>
</div>
<hr/>
<div class="row nav-target" id="graph">
	<h3>GRAPH:</h3> 
	<blockquote><p>
	Graph searches are useful for finding connections and neighborhoods of elements, such as the
	shortest path between two proteins or the neighborhood for a particular protein state or all states. Graph
	searches take detailed BioPAX semantics, such as generics or nested complexes, into account and traverse the graph
	accordingly. The starting points can be either physical entites, entity references, or xrefs. In the latter two cases,
	the graph search starts from ALL the physical entities that belong to that particular canonical references, i.e. from all
	the molecule states.
	Note that we integrate BioPAX data from multiple databases based on our proteins and small molecules data warehouse
	and consistently normalize UnificationXref, EntityReference, Provenance, BioSource, and ControlledVocabulary
	objects when we are absolutely sure that two objects of the same type are equivalent. We, however, do not merge
	physical entities and reactions from different sources, as matching and aligning pathways at that level is still an
	open research problem. As a result, graph searches can return several similar but disconnected sub-networks that 
	correspond to the pathway data from different providers (though some physical entities often refer to the 
	same small molecule or protein reference or controlled vocabulary).</p></blockquote>
	<h4>Parameters:</h4>
	<ul>
		<li><em>kind=</em> [Required] graph query (<a
				href="#graph_kinds">values</a>)
		</li>
		<li><em>source=</em> [Required] source object's URI/ID. Multiple source URIs/IDs are allowed per query, for example
			'source=http://identifiers.org/uniprot/Q06609&amp;source=http://identifiers.org/uniprot/Q549Z0'.
			See <a href="#about_uris">a note about URIs</a>.
		</li>
		<li><em>target=</em> [Required for PATHSFROMTO graph query]
			target URI/ID. Multiple target URIs are allowed per query; for
			example 'target=http://identifiers.org/uniprot/Q06609&amp;target=http://identifiers.org/uniprot/Q549Z0'.
			See <a href="#about_uris">a note about URIs</a>.
		</li>
		<li><em>direction=</em> [Optional, for NEIGHBORHOOD and COMMONSTREAM algorithms] - graph search direction (<a
				href="#graph_directions">values</a>).
		</li>
		<li><em>limit=</em> [Optional] graph query search distance limit (default = 1).
		</li>
		<li><em>format=</em> [Optional] output format (<a
				href="#graph_formats">values</a>)
		</li>
		<li><em>datasource=</em> [Optional] datasource filter (same as for <a href="#search_parameters">'search'</a>).
		</li>
		<li><em>organism=</em> [Optional] organism filter (same as for <a href="#search_parameters">'search'</a>).
		</li>
	</ul>
	<h4>Output:</h4> 
	By default, graph queries return a complete BioPAX representation of the
	subnetwork matched by the algorithm. Other output formats are available as specified
	by the optional format parameter. Please be advised that some output
	format choices might cause "no result found" error if the conversion is not applicable for the BioPAX result 
	(e.g., BINARY_SIF output fails if there are no interactions, complexes, nor pathways in the retrieved set).
	<h4>Query Examples:</h4> 
	Neighborhood of COL5A1 (P20908, CO5A1_HUMAN):
	<ol>
		<li><a rel="example" href="graph?source=http://identifiers.org/uniprot/P20908&kind=neighborhood&format=EXTENDED_BINARY_SIF">
			This query finds the BioPAX nearest neighborhood of the protein reference</a> http://identifiers.org/uniprot/P20908, i.e., 
			all reactions where the corresponding protein forms participate; returned in the Simple Interaction Format (SIF)</li>	
		<li><a rel="example" href="graph?source=P20908&kind=neighborhood">
			This query finds the 1 distance neighborhood of P20908</a> - starting from the corresponding Xref, 
			finds all reactions that its oners (e.g., a protein reference) and their states (protein forms) 
			participate in, and returns the BioPAX model.</li>		
		<li><a rel="example" href="graph?source=COL5A1&kind=neighborhood">
			A similar query using the gene symbol COL5A1 instead of URI or UniProt ID</a> 
			(this also implies internal id-mapping to primary UniProt IDs). Compared with above examples, 
			particularly the first one, a query like this potentially returns a larger subnetwork, for
			it possibly starts its graph traversing from several unification and relationship Xrefs 
			rather than from the ProteinReference (http://identifiers.org/uniprot/P20908).
			One can mix: submit URI along with UniProt accession, RefSeq ID, NCBI Gene ID and Ensemble IDs
			in a single /graph or /get query; other identifiers might also work, by chance (if present 
			in the db).
		</li>
	</ol>
</div>
<hr/>
<div class="row nav-target" id="traverse">
	<h3>TRAVERSE:</h3>
	<blockquote><p>
	Provides XPath-like access to the BioPAX database. With '/travers', users can
	explicitly state the paths they would like to access. The format of the path parameter value:
	<em>[Initial Class]/[property1]:[classRestriction(optional)]/[property2]...</em>
	A "*" sign after the property instructs path accessor to transitively traverse that property.
	For example, the following path accessor will traverse through all physical entity components within a complex:
	<em>Complex/component*/entityReference/xref:UnificationXref</em>. 
	The following will list the display names of all participants of interactions, 
	which are pathway components of a pathway: <em>Pathway/pathwayComponent:Interaction/participant*/displayName</em>.
	Optional <em>classRestriction</em> allows to limit the returned property values to a certain subclass of the property's range.
	In the first example above, this is used to get only the unification xrefs. 
	<a href="http://www.biopax.org/paxtools/apidocs/org/biopax/paxtools/controller/PathAccessor.html">
	Path accessors</a> can use all the official BioPAX properties as well as additional derived classes 
	and parameters, such as inverse parameters and interfaces that represent anonymous union classes in OWL. 
	(See <a href="http://www.biopax.org/paxtools/">Paxtools documentation</a> for more details).
	</p></blockquote>
	<h4>Parameters:</h4>
	<ul>
		<li><em>uri=</em> [Required] a BioPAX element URI - specified similarly to the
			<a href="#get">'GET' command above</a>). Multiple IDs are
			allowed (uri=...&amp;uri=...&amp;uri=...).
		</li>
		<li><em>path=</em> [Required] a BioPAX propery path in the form of
			property1[:type1]/property2[:type2];  see <a href="#biopax_properties">properties</a>,
			<a href="#biopax_inverse_properties">inverse properties</a>, <a href="http://www.biopax.org/paxtools">Paxtools</a>,
			<a href="http://www.biopax.org/paxtools/apidocs/org/biopax/paxtools/controller/PathAccessor.html">
			org.biopax.paxtools.controller.PathAccessor</a>.
		</li>
	</ul>
	<h4>Output:</h4>
	XML result according to the <a href="help/schema">Search Response XML
	Schema</a>&nbsp;(TraverseResponse type; pagination is disabled to return all values at once)<br/>
	<h4>Query Examples:</h4>
	<ol>
		<li><a rel="example"
		       href="traverse?uri=http://identifiers.org/uniprot/P38398&path=ProteinReference/organism/displayName">
			This query returns the display name of the organism of the ProteinReference specified by the URI.</a></li>
		<li><a rel="example"
		       href="traverse?uri=http://identifiers.org/uniprot/P38398&uri=http://identifiers.org/uniprot/Q06609&path=ProteinReference/organism">
			This query returns the URI of the organism for each of the Protein References</a></li>
		<li><a rel="example"
		       href="traverse?uri=http://identifiers.org/uniprot/Q06609&path=ProteinReference/entityReferenceOf:Protein/name">
			This query returns the names of all states of RAD51 protein (by its ProteinReference URI, using
			property path="ProteinReference/entityReferenceOf:Protein/name")</a></li>
		<li><a rel="example"
		       href="traverse?uri=http://identifiers.org/uniprot/P38398&path=ProteinReference/entityReferenceOf:Protein">
			This query returns the URIs of states of BRCA1_HUMAN</a></li>
		<li><a rel="example"
		       href="traverse?uri=http://identifiers.org/uniprot/P38398&uri=http://identifiers.org/taxonomy/9606&path=Named/name">
			This query returns the names of several different objects (using abstract type 'Named' from Paxtools
			API)</a></li>
	
	</ol>
</div>
<hr/>
<div class="row nav-target" id="top_pathways">
	<h3>TOP_PATHWAYS:</h3>
	<blockquote><p>
	Returns all "top" pathways -- pathways that are neither
	'controlled' nor 'pathwayComponent' of another biological process.</p></blockquote>
	<h4>Parameters:</h4>
	<ul>
		<li><em>datasource=</em> [Optional] filter by data source (same as for <a href="#search_parameters">'search'</a>).
		</li>
		<li><em>organism=</em> [Optional] organism filter (same as for <a href="#search_parameters">'search'</a>).
		</li>
	</ul>	
	<h4>Output:</h4> 
	XML document described by <a href="help/schema">Search Response XML
	Schema</a>&nbsp;(SearchResponse type; pagination is disabled to returns all root pathways at once)<br/>
	<h4>Query Examples:</h4>
	<ol>
		<li><a href="top_pathways"> get top/root pathways (XML)</a></li>
		<li><a href="top_pathways.json"> get top/root pathways in JSON format</a></li>
	</ol>
</div>
<hr/>
<div class="row nav-target" id="idmapping">
	<h3>IDMAPPING:</h3>
	<blockquote><p>
	Unambiguously maps, e.g., HGNC gene symbols, NCBI Gene, RefSeq, ENS*, and
	secondary UniProt identifiers to the primary UniProt accessions, or -
	ChEBI and PubChem IDs to the primary ChEBI IDs. You can mix different standard ID types in one query.
	This is a specific id-mapping (not general-purpose) for canonical reference proteins and small molecules;
	it was first designed for internal use, to improve BioPAX data integration and allow graph
	queries accept not only URIs but also standard IDs. The mapping tables were derived
	exclusively from Swiss-Prot (DR fields) and ChEBI data (custom mapping tables can be added in the 
	future versions if necessary).</p></blockquote>
	<h4>Output:</h4> 
	Simple JSON (serialized Map) format.
	<h4>Examples:</h4> <br/>
	<ol>
		<li><a rel="example" href="idmapping?id=BRCA2&id=TP53">/idmapping?id=BRCA2&amp;id=TP53</a></li>
	</ol>
</div>

<div class="row nav-target" id="parameter_values">
	<h2>Parameter Values</h2>
	<div class="parameters row" id="organisms">
		<h3>Officially supported organisms</h3>
		<p>We intended to integrate the pathway data only for the following species:</p>
		<ul>
			<c:forEach var="org" items="${cpath.organisms}">
				<em><strong><c:out value="${org}"/></strong></em>
			</c:forEach>
		</ul>
		<p>But there are still other organisms associated with some BioPAX elements,
			because original pathway data might contain disease pathways, experiment details,
			generics (i.e., wildcard proteins), etc. We did not specially clean or convert 
			such data due to high risk of introducing errors and artifacts.
			All BioSource objects can be found by using <a href="search?q=*&type=biosource">this search query</a>.
		</p>
	</div>
	<div class="row">
	<div class="parameters col-sm-6" id="output_formats">
		<h3>Output Format ('format'):</h3>
		<p>
			See also <a href="/metadata/formats">output format description.</a>
		</p>
		<!-- items are to be added here by a javascript -->
		<ul id="formats"></ul>
	</div>
	<div class="parameters col-sm-6" id="graph_kinds">
		<h3>Graph Type ('kind'):</h3>
		<!-- items are to be added here by a javascript -->
		<ul id="kinds"></ul>
	</div>
	</div>
	<div class="row">
	<div class="parameters col-sm-6" id="graph_directions">
		<h3>Graph Directions ('direction'):</h3>
		<!-- items are to be added here by a javascript -->
		<ul id="directions"></ul>
	</div>
	<div class="parameters col-sm-6" id="biopax_types">
		<h3>BioPAX class ('type'):</h3>
		<p>	<a href="javascript:switchit('types')">Click here</a> to show/hide the list  
			(see also: <a href="http://www.biopax.org/webprotege/">BioPAX Classes</a>).
		</p>
		<!-- items are to be added here by a javascript -->
		<ul class="dropdown" id="types" style="display: none;"></ul>
		<br/>
	</div>
	</div>
	<div class="row">
	<div class="parameters col-sm-6" id="biopax_properties">
		<h3>BioPAX Properties and Restrictions:</h3>
		<p>Listed below are BioPAX	properties' summary as defined 
			in the Paxtools model: domain, property name, range and restrictions (if any). 
			For example, <em>XReferrable xref Xref D:ControlledVocabulary=UnificationXref
			D:Provenance=UnificationXref,PublicationXref</em> means that 
			values of ControlledVocabulary.xref can only be of <em>UnificationXref</em> type.</p>
		<p><a href="javascript:switchit('properties')">Click here</a>
			to show/hide the list of properties</p>
		<!-- items are to be added here by a javascript -->
		<ul id="properties" style="display: none;"></ul>
	</div>
	<div class="parameters col-sm-6" id="biopax_inverse_properties">
		<h3>Inverse BioPAX Object Properties (a feature of Paxtools API):</h3>
		<p>Some of the object BioPAX properties can be traversed in the
			inverse direction as well, e.g, 'xref' - 'xrefOf'. 
			Unlike for the standard <em>xref</em> property, e.g., the restriction <em>XReferrable xref
			Xref D:ControlledVocabulary=UnificationXref	D:Provenance=UnificationXref,PublicationXref</em> 
			below must be read <em>right-to-left</em> as it is actually about Xref.xrefOf: 
			RelationshipXref.xrefOf cannot contain neither <em>ControlledVocabulary</em> 
			(any sub-class) nor <em>Provenance</em> objects 
			(in other words, vocabularies and provenance may not have any relationship xrefs).</p>
		<p><a href="javascript:switchit('inverse_properties')">Click here</a>
			to show/hide the list of properties</p>
		<!-- items are to be added here by a javascript -->
		<ul id="inverse_properties" style="display: none;"></ul>
	</div>
	</div>
</div>
	
	<div class="row nav-target" id="errors">
	<h2>Error Response</h2>
	<p>
		If an error or no results happens while processing a user's request,
		the client will receive an error HTTP response with corresponding status code and message
		(then browsers usually display a error page sent by the server; other clients normally
		check the status before further processing the results.)
	</p>
	</div>

<jsp:include page="footer.jsp"/>
<script src="<c:url value="/resources/scripts/help.js"/>"></script>
</body>
</html>
