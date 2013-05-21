<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="author" content="${cpath.name}" />
<meta name="description" content="cPath2 Service Description" />
<meta name="keywords" content="${cpath.name}, cPath2, cPathSquared, webservice, help, documentation" />
<script  src="<c:url value="/resources/scripts/jquery-1.9.1.min.js"/>"></script>
<script  src="<c:url value="/resources/scripts/json.min.js"/>"></script>
<script  src="<c:url value="/resources/scripts/help.js"/>"></script>
<link rel="stylesheet" href="<c:url value="/resources/css/cpath2.css"/>"  media="screen" />

<title>cPath2::Info</title>
</head>
<body>

	<!-- store actual server URL value and use in scripts -->
	<p>
		<span id="cpath2_endpoint_url" style="display: none"><c:url value="/" /></span>
	</p>

	<jsp:include page="header.jsp" />
	
	<div id="content">
	
	<section id="description_section">
	
	<h2>Resource Description</h2>
		<p>${cpath.description}</p>		
		<p>Data is freely available, under the license terms of each
			contributing database.</p>
		<!-- start of web service api documentation -->
		<h2>Web Service API:</h2>
		<p>You can programmatically access the data within Pathway Commons
			using the Pathway Commons Web Service Application Programming
			Interface (API). This page provides a reference guide to help you get
			started.</p>

		<h3 id="commands">Commands:</h3>
		<ol>
			<li><a href="#search">Command: SEARCH</a></li>
			<li><a href="#get">Command: GET</a></li>
			<li><a href="#graph">Command: GRAPH</a></li>
			<li><a href="#traverse">Command: TRAVERSE</a></li>
			<li><a href="#top_pathways">Command: TOP_PATHWAYS</a></li>
			<li><a href="#help">Command: HELP</a></li>
		</ol>

		<!-- URIs -->
		<h3>
			<a id="miriam"></a>Note about using URIs:
		</h3>
		<p>
			Some of web service commands require a parameter value to be valid,
			existing URI; it is either an original data provider's URI (for most
			BioPAX Entities) or a <a href="http://identifiers.org" rel="external">Identifiers.org</a>
			URL that we create for BioPAX UtilityClass objects, such as
			ProteinReference, whenever it (<a rel="external"
				href="http://code.google.com/p/pathway-commons/wiki/cPath2PreMerge?ts=1346339836&updated=cPath2PreMerge#Normalization">normalization</a>)
			is possible (we do our best following <a rel="external"
				href="http://www.ebi.ac.uk/miriam/main/">MIRIAM's</a> set of <a rel="external"
				href="http://biomodels.net/miriam/">guidelines</a> for the
			annotation and curation of computational models.)
		</p>
		<h3><a id="enco"></a>About this page example links and URL-encoding</h3>
		<p>cPath2 is a web service. All query parameters, including URIs, must be
			URL-encoded, and this is normally done automatically when a 
			web client application rests on a standard HTTP client library 
			to send well-formed GET or POST HTTP requests to the server.
			Developers and web users are not supposed to manually encode
			query parameters nor type long queries in a browser's address line.
			So do not do this. However, as you can see, this page contains manually 
			formed and encoded example query links one can click or copy to the browser's 
			address line to try. And the URIs you can see there in queries like "?uri=..." 
			are already encoded (- a BioPAX URI in the database can be different).
			Despite the above, if we still want to use a web browser to quickly 
			get an idea about what the web service is about, remember that web browsers 
			do only basic encoding for you; they can replace spaces with "%20" (or '+') 
			but it will not encode '%', '#', '+' if it is already part of the query (URI parameter); 
			so you must replace '%' with %25, '#' - %23, '+' - %2B, etc. To summarize,
			the local part of URI is URL-encoded, but when you need to use the URI as parameter in 
			a URL (query), it has to be encoded again (and decoded on the server side), which is 
			normally not a user's business. By the way, 
			URIs are case-sensitive and cannot contain spaces.</p>

		</section>
		<section id="commands_description_section">

		<!-- command bodies -->
		<ol>
			<!-- search command -->
			<li>
				<h2>
					<a id="search"></a>Command: SEARCH
				</h2>
				<h3>Summary:</h3> 
				Full text search in Pathway Commons using
				keywords, phrases, a Lucene query. For example, find records that
				contain the keyword "BRCA2" (in any index field), or, more
				specifically, - "BRCA2" in a Protein's 'name' (index) field. BioPAX
				entity or utility class hits that match the search criteria and pass
				filters are ranked and returned. Search (index) field names are
				(case-sensitive): <em>comment, ecnumber, keyword, name,
					pathway, term, xrefdb, xrefid, dataSource, and organism</em>. The latter
				two were introduced mainly for filtering as un_tockenized, no
				wildcard/fuzzy support. (Note: index fields are not exactly BioPAX
				properties!) To search within a specific BioPAX class only, use the
				'type' parameter (filter). Full-text search is a very powerful
				feature and can save a lot of time, but it's not a replacement for a
				more accurate BioPAX graph analysis, i.e., - using other web service
				commands (see below), following official BioPAX semantics. (The more
				"advanced" search query is the more cool but uncertain/error-prone
				result is.)
				<h3>Parameters:</h3>
				<ul>
					<li><em>q=</em> [Required] a keyword, name, external
						identifier, or a search string (Lucene syntax).</li>
					<li><em>page=N</em> [Optional] (N&gt;=0, default is 0), search
						result page number. See below ("Output" section) for
						details.</li>
					<li><em>datasource=</em> [Optional] data source filter (<a
						href="#available_datasource_parameter">values</a>). Multiple data
						source values are allowed per query; for example, <em>datasource=reactome&amp;datasource=pid</em>
						means: we want data from Reactome OR NCI_Nature (PID)</li>
					<li><em>organism=</em> [Optional] organism filter. Multiple
						organisms are allowed per query; for example
						'organism=9606&amp;organism=10016' (which means either 9606 or
						10016; can also use "homo sapiens", "mus musculus" instead).</li>
					<li><em>type=</em> [Optional] BioPAX class filter (<a
						href="#available_biopax_parameter">values</a>)</li>
				</ul>
				<h3>Output:</h3> XML result that follows the <a
				href="help/schema">Search Response XML
					Schema</a>.<br /> JSON is returned by appending '.json' to the query
				URL. The server returns up to 'numHitsPerPage' search hits per
				request (configured on the server). But you can request hits beyond
				the first N, if any, using the 'page' parameter. The 'numHits'
				attribute in the returned XML or JSON contains the total number of
				search results that matched the query and filters. If a page is
				requested beyond the total number of results, "no result" http
				response (460) is returned. In other words, when numHits &gt; numHitsPerPage
				hits, 'page=n' (n&gt;=0) parameter is to get hits ranked from
				numHitsPerPage*n to numHitsPerPage*(n+1)-1. Total no. pages can be
				also calculated as<br /> INT[(numHits-1)/numHitsPerPage+1].
				<h4>Query Examples:</h4> <br />
				<ol>
					<li><a rel="example" href="search.xml?q=Q06609">search for "Q06609"
							keyword, no filters, return XML result</a></li>
					<li><a rel="example" href="search?q=xrefid:Q06609">specific search for
							"Q06609" in the 'xrefid' index field, no filters, return XML
							(default)</a></li>
					<li><a rel="example" href="search.json?q=Q06609">search for "Q06609"
							keyword, no filters, return JSON result</a></li>
					<li><a rel="example" href="search.json?q=Q06609&type=pathway">search for
							Pathways containing "Q06609" (search all fields), return JSON</a></li>
					<li><a rel="example" 
						href='search?q=brca2&type=proteinreference&organism=homo%20sapiens&datasource=pid'>search
							for ProteinReference containing "brca2" (case-insensitive)
							keyword (in any field), filter by organism (human) and datasource
							(NCI_Nature actually)</a></li>
					<li><a rel="example" 
						href="search.xml?q=name:'col5a1'&type=proteinreference&organism=9606">more
							precise search for a human ProteinReference containing "col5a1"
							among its names (case-insensitive)</a></li>
					<li><a rel="example" 
						href="search?q=brc*&type=control&organism=9606&datasource=reactome">search
							for Control interactions matching "brca*" (wildcard,
							case-insensitive, in any field), originated from Reactome, Human</a></li>
					<li><a  rel="example" href="search?q=a*&page=3">use of pagination: get
							the forth page (page=3) hits</a></li>
					<li><a rel="example" 
						href="search?q=+binding%20NOT%20transcription*&type=control&page=0">search
							for Control interactions having something to do with "binding"
							but not "transcription" (gets the first page hits)</a></li>
					<li><a  rel="example" href="search?q=pathway:immune*&type=conversion">search
							for Conversion interactions that are direct or indirect
							participants of a "immune" (part of its name) pathway (cool!)</a></li>
					<li><a  rel="example" href="search?q=*&type=pathway&datasource=reactome">find
							all Reactome pathways</a></li>
					<li><a  rel="example" href="search?q=*&type=biosource">all organisms
							(i.e., including BioSource objects referenced from
							evidence/infection data there)</a></li>
				</ol>
			</li>
			<!-- get command -->
			<li>
				<h2>
					<a id="get"></a>Command: GET
				</h2>
				<h3>Summary:</h3> Retrieves details regarding one or more records,
				such as pathway, interaction or physical entity. For example, get
				the complete Apoptosis pathway from Reactome.
				<h3>Parameters:</h3>
				<ul>
					<li><em>uri=</em> [Required] a BioPAX element ID (RDF ID; for
						utility classes that have been "normalized", such as xrefs, entity
						refereneces and controlled vocabularies, it is usually a
						Idntifiers.org URL. Multiple IDs are allowed per query, for
						example 'uri=http://identifiers.org/uniprot/Q06609&amp;uri=http://identifiers.org/uniprot/Q549Z0'
						('uri=Q06609&amp;uri=Q549Z0' - also works but will return Xrefs instead of ProteinReferences).
						<a href="#miriam">See also</a> about MIRIAM and Identifiers.org.</li>
					<li><em>format=</em> [Optional] output format (<a
						href="#available_output_parameter">values</a>)</li>
				</ul>
				<h3>Output:</h3> By default, a complete BioPAX representation for
				the record pointed to by the given uri. Other output formats are
				available as specified by the optional format parameter. Please be
				advised that not all output formats are relevant for this web
				service. For example, it would not make sense to request BINARY_SIF
				output when the given URI points to a protein.
				<h4>Query Examples:</h4>
				<br />
				<ol>
					<li><a rel="example" href="get?uri=http://identifiers.org/uniprot/Q06609">
							get a self-consistent BioPAX sub-model using
							URI=http://identifiers.org/uniprot/Q06609 (the <strong>ProteinReference</strong>
							and dependent objects)</a></li>
					<li><a rel="example" href="get?uri=COL5A1">
							get the <strong>Xref</strong> by gene symbol (COL5A1)</a> 
							there might be no such objects in the database, but it works because 
							it actually maps (using internal id-mapping) to the xref P20908 
							(URI="${cpath.xmlBase}UnificationXrefUniProt+KnowledgebaseP20908"); 
							one can also try: UniProt accession, RefSeq ID, NCBI Gene ID and Ensemble IDs 
							(other identifiers may also work); however, using IDs instead URIs works 
							even better in <a href="#graph">"graph"</a> queries, e.g., to get 
							the neighborhood network of a thing. 
							You can then find all Xref's owners of P20908 xref using 
							the following <a href="#traverse">"traverse"</a> query (see below): 
							<a rel="example" href="traverse?uri=http://purl.org/pc2/UnificationXrefUniProt%2BKnowledgebaseP20908&path=Xref/xrefOf">
							get parents</a></li>	
					<li><a rel="example" 
						href="get?uri=http://www.reactome.org/biopax/48887%23Pathway468">
						a Reactome v44 "Signaling by BMP" pathway</a>(if loaded here; normalized, as usual)</li>
					<li><a rel="example" 
						href="get?uri=http://pid.nci.nih.gov/biopaxpid_74716&format=BINARY_SIF">
							get the NCI-Nature Curated BMP signaling pathway in SIF format</a></li>				
				</ol>
			</li>

			<!-- graph command -->
			<li>
				<h2>
					<a id="graph"></a>Command: GRAPH
				</h2>
				<h3>Summary:</h3> We implemented several graph theoretic algorithms
				that take BioPAX data model into account, such as shortest path
				between two proteins. This command executes one of graph queries
				based on the integrated BioPAX network stored on our server. For
				example, get the neighborhood for a particular BRCA1 protein state
				or all states. As the original BioPAX data and our import pipeline
				quality improves, graph query results also become more interesting
				and connected. We merge BioPAX data based on our proteins and small
				molecules data warehouse and consistently normalized
				UnificationXref, EntityReference, Provenance, BioSource, and
				ControlledVocabulary objects when we are absolutely sure two objects
				of the same type are equivalent. By favoring to store unmodified
				original data when in doubt, we try NOT to accidently introduce any
				artifacts or noise to this process and thus limit our users's
				options in the future. Having said that, the really good thing is
				that anyone (including our team) is free and encouraged to
				independently develop a dedicated web service client app, which,
				e.g., would use a combination of an advanced id-mapping tool, our
				basic search and query services, and any other advanced network
				merge algorithm of ones choice.
				<h3>Parameters:</h3>
				<ul>
					<li><em>kind=</em> [Required] graph query (<a
						href="#available_graph_parameter">values</a>)</li>
					<li><em>source=</em> [Required] source object's URI. Multiple
						source URIs are allowed per query, for example
						'source=uri=http://identifiers.org/uniprot/Q06609&amp;source=uri=http://identifiers.org/uniprot/Q549Z0'.
						See <a href="#miriam">a note about MIRIAM and Identifiers.org</a>.</li>
					<li><em>target=</em> [Required for PATHSFROMTO graph query]
						target URI. Multiple target URIs are allowed per query; for
						example
						'target=uri=http://identifiers.org/uniprot/Q06609&amp;target=uri=http://identifiers.org/uniprot/Q549Z0'.
						See <a href="#miriam">a note about MIRIAM and Identifiers.org</a>.</li>
					<li><em>direction=</em> [Optional, for NEIGHBORHOOD and
						COMMONSTREAM] - graph search direction (<a
						href="#available_direction_parameter">values</a>).</li>
					<li><em>limit=</em> [Optional] graph query search distance
						limit (default = 1).</li>
					<li><em>format=</em> [Optional] output format (<a
						href="#available_output_parameter">values</a>)</li>
					<li><em>datasource=</em> [Optional] data source filter (<a
						href="#available_datasource_parameter">values</a>). Multiple data
						source values are allowed per query; for example, <em>datasource=reactome&amp;datasource=pid</em>
						means: we want data from Reactome OR NCI_Nature (PID)</li>
					<li><em>organism=</em> [Optional] organism filter. Multiple
						organisms are allowed per query; for example
						'organism=9606&amp;organism=10016' (which means either 9606 or
						10016; can also use "Homo sapiens", "mus musculus" instead).</li>						
				</ul>
				<h3>Output:</h3> By default, a complete BioPAX representation of the
				desired graph query. Other output formats are available as specified
				by the optional format parameter. Please be advised that not all
				output formats are relevant for this web service. For example, it
				would not make sense to request BINARY_SIF output when the given URI
				points to a protein.
				<h4>Query Examples:</h4> Neighborhood of COL5A1 (P20908,
				CO5A1_HUMAN): <br />
				<ol>
					<li><a rel="example"
						href="graph?source=http://www.reactome.org/biopax/48887%23Protein2044&kind=neighborhood">
							from the protein's state</a></li>
					<li><a rel="example" href="graph?source=COL5A1&kind=neighborhood">
						the neghborhood of all owners of the Unification Xref found by gene symbol COL5A1</a> - a popular query,
						but it is less specific (implies internal id-mapping to UniProt IDs) compared to using exact URIs (if you know/found any);
						one can try and mix: UniProt accession, RefSeq ID, NCBI Gene ID and Ensemble IDs (other identifiers may also work by chance, 
						if present in the original data, though we did not specifically map them to UniProt's)</li>					
					<li><a rel="example"
						href="graph?source=P20908&kind=neighborhood">
							from the protein reference P20908, i.e., all its states (found in the
							BioPAX network(s) on the server)</a></li>
					<li><a rel="example"
						href="graph?source=http://identifiers.org/uniprot/P20908&kind=neighborhood&format=EXTENDED_BINARY_SIF">
							from the same protein reference but using a different output
							format</a></li>
				</ol>
			</li>

			<!-- traverse command -->
			<li>
				<h2>
					<a id="traverse"></a>Command: TRAVERSE
				</h2>
				<h3>Summary:</h3> Get BioPAX data property values or objects (URIs)
				using a XPath-like property path expression. This command has two
				parameters.
				<h3>Parameters:</h3>
				<ul>
					<li><em>uri=</em> [Required] a BioPAX element ID (it's like
						for <a href="#get">'GET' command above</a>). Multiple IDs are
						allowed (uri=...&amp;uri=...&amp;uri=...).</li>
					<li><em>path=</em> [Required] a BioPAX propery path: like
						property1[:type1]/property2[:type2]; can also include convenient
						(unofficial) inverse BioPAX properties, such as xrefOf,
						componentOf, etc., and abstract types, such as Named, XReferrable,
						Process, etc.; see <a href="#properties_parameter">properties</a>,
						<a href="#inverse_properties_parameter">inverse
							properties</a>, <a href="http://www.biopax.org/paxtools">Paxtools</a>,
						org.biopax.paxtools.controller.PathAccessor.</li>
				</ul>
				<h3>Output:</h3> XML result that follows the <a
				href="help/schema">Search Response XML
					Schema</a>&nbsp;(TraverseResponse type; pagination is disabled: returns
				all values at once)<br />
				<h4>Query Examples:</h4>
				<ol>
					<li><a rel="example"
						href="traverse?uri=http://identifiers.org/uniprot/P38398&path=ProteinReference/organism/displayName">
							for a URI (of a ProteinReference), get the organism's display
							name</a></li>
					<li><a rel="example"
						href="traverse?uri=http://identifiers.org/uniprot/P38398&uri=http://identifiers.org/uniprot/Q06609&path=ProteinReference/organism">
							for each URI, get the organism (URI)</a></li>
					<li><a rel="example"
						href="traverse?uri=http://identifiers.org/uniprot/Q06609&path=ProteinReference/entityReferenceOf:Protein/name">
							get names of all states of RAD51 protein (by its ProteinReference
							URI, using property
							path="ProteinReference/entityReferenceOf:Protein/name")</a></li>
					<li><a rel="example"
						href="traverse?uri=http://identifiers.org/uniprot/P38398&path=ProteinReference/entityReferenceOf:Protein">
							get URIs of states of BRCA1_HUMAN
							(path="ProteinReference/entityReferenceOf:Protein")</a></li>
					<li><a rel="example"
						href="traverse?uri=http://identifiers.org/uniprot/P38398&uri=http://www.reactome.org/biopax/48887%23Protein2992&uri=http://identifiers.org/taxonomy/9606&path=Named/name">
							get names of several different objects (using abstract type
							'Named' from Paxtools API)</a></li>
					<li><a rel="example"
						href="traverse?uri=http://pid.nci.nih.gov/biopaxpid_74716&path=Pathway/pathwayComponent:Interaction/participant/displayName">
							get BMP pathway participants's names (cool, but be careful and
							not too much excited!)</a></li>
				</ol>
			</li>

			<!-- top_pathways command -->
			<li>
				<h2>
					<a id="top_pathways"></a>Command: TOP_PATHWAYS
				</h2>
				<h3>Summary:</h3> Retrieves all "top" pathways (- not exactly in the
				graph-theoretic sense, but - all such pathways that are neither
				'controlled' nor 'pathwayComponent' of other processes)
				<h3>Parameters:</h3> no parameters
				<h3>Output:</h3> XML result that follows the <a
				href="help/schema"> Search Response XML
					Schema</a>&nbsp;(SearchResponse type; pagination is disabled: returns
				all pathways at once)<br />
				<h4>Query Examples:</h4>
				<ol>
					<li><a href="top_pathways"> get top/root pathways (XML)</a></li>
					<li><a href="top_pathways.json"> get top/root pathways (JSON)</a></li>
				</ol>
			</li>

			<!-- help command -->
			<li>
				<h2>
					<a id="help"></a>Command: HELP
				</h2>
				<h3>Summary:</h3> Finally, this is a RESTful web service that
				returns the information about web service commands, parameters,
				and BioPAX properties.
				<h3>Output:</h3> XML/JSON (if '.json' suffix used) element 'Help'
				(nested tree); see: <a href="help/schema">Search
					Response XML Schema</a><br />
				<h4>Query Examples:</h4> <br />
				<ol>
					<li><a rel="example" href="help/commands">/help/commands</a></li>
					<li><a rel="example" href="help/commands.json">/help/commands.json</a></li>
					<li><a rel="example" href="help/commands/search">/help/commands/search</a></li>
					<li><a rel="example" href="help/types">/help/types</a></li>
					<li><a rel="example" href="help/kinds">/help/kinds</a></li>
					<li><a rel="example" href="help/directions">/help/directions</a></li>
					<li><a rel="example" href="help/types/properties">/help/types/properties</a></li>
					<li><a rel="example" href="help/types/provenance/properties">/help/types/provenance/properties</a></li>
					<li><a rel="example" href="help/types/inverse_properties">/help/types/inverse_properties</a></li>
					<li><a rel="example" href="help">/help</a></li>
				</ol>
			</li>
		</ol>
		<br />
	</section>
	<section id="parameters_description_section">
		<!-- additional parameter details -->
		<h2>Pathway and Interaction Data Sources</h2>
		<p>Imported pathway data with corresponding logo and names.
		These names are recommended to use for the 'datasource' filter 
		parameter (see about <a href="#search">'/search'</a> command). 
		For example, 'NCI_Nature', 'reactome' can be successfully 
		used (case insensitive) there. Using URIs instead of names is also
		possible. If there are several items having a name in commom,
		e.g. 'Reactome', that means we imported the provider's data
		from defferent locations or archives. Not all data are ready to be
		imported into the cPath2 system right away; so one has to unpack, 
		add/remove/edit, pack originsl BioPAX or PSI-MI files. Data location
		URLs are shown for information and copyright purpose only.
		One can find all BioPAX Provenance objects in teh system by using
		<a  href="search?q=*&type=provenance">search for all Provenance objects</a>.	
		</p>
		<div id="data_sources" class="parameters">
			<!-- items are to be added here by a javascript -->
			<dl id="pathway_datasources" class="datasources"></dl>	
		</div>
		<br />
		<h2>Warehouse Data Sources</h2>
		<div class="parameters">
			<!-- items are to be added here by a javascript -->
			<p>In order to consistently normalize and merge all pathways and interactions, 
			   we created a BioPAX warehouse using the following data:</p>
			<dl id="warehouse_datasources" class="datasources"></dl>
		</div>
		<br />
		<h2 id="organisms">Organisms</h2>
		<div class="parameters">
			<h3>Officially supported organisms</h3>
			<p>Having the above data sources, we chose to integrate 
			all the pathway data files only for the following species:</p>
			<ul>
				<c:forEach var="org" items="${cpath.organisms}">
					<em><strong><c:out value="${org}" /></strong></em>
				</c:forEach>
			</ul>
			<p>There are still other organisms associated with some BioPAX elements too, 
			because original pathway data might contain disease pathways, other lab experiment details,
			use generics (i.e., wildcard proteins), etc. We did not specially clean or convert such data. 
			You can find all organisms by using <a  href="search?q=*&type=biosource">search for all BioSource objects</a>.</p>
		</div>

		<!-- additional parameter details -->
		<h2 id="additional_parameters">Query Parameter Values</h2>

		<div class="parameters">
			<h3>Output Formats ('format'):</h3>
			<p>
				See also <a href="help/formats.html">output formats.</a>
			</p>
			<!-- items are to be added here by a javascript -->
			<ul id="output_parameter"></ul>
			<br />
		</div>

		<div class="parameters">
			<h3>Built-in graph queries ('kind'):</h3>
			<!-- items are to be added here by a javascript -->
			<ul id="graph_parameter"></ul>
			<br />
		</div>

		<div class="parameters">
			<h3>Graph traversal directions ('direction'):</h3>
			<!-- items are to be added here by a javascript -->
			<ul id="direction_parameter"></ul>
			<br />
		</div>

		<div class="parameters">
			<h3>BioPAX classes ('type'):</h3>
			<p><a href="javascript:switchit('biopax_parameter')">Click here</a>
				to show/hide the list</p>
			<!-- items are to be added here by a javascript -->
			<ul id="biopax_parameter" style="display: none;"></ul>
			<br />
		</div>

		<div class="parameters">
			<h3>Official BioPAX Properties and Domain/Range Restrictions:</h3>
			<p>Note: "XReferrable xref Xref
				D:ControlledVocabulary=UnificationXref
				D:Provenance=UnificationXref,PublicationXref" means
				XReferrable.xref, and that, for a ControlledVocabulary.xref, the
				value can only be of UnificationXref type, etc.</p>
				<p><a href="javascript:switchit('properties_parameter')">Click here</a>
				to show/hide the list of properties</p>
			<!-- items are to be added here by a javascript -->
			<ul id="properties_parameter" style="display: none;"></ul>
			<br />
		</div>

		<div class="parameters">
			<h3>Inverse BioPAX Object Properties and Domain/Range
				Restrictions (useful feature of Paxtools API):</h3>
			<p>Note: Some of object range BioPAX properties can be
				reversed/inversed (and also used in the '/traverse?path=' context),
				e.g, 'xref' - 'xrefOf'. These are listed below. But, e.g., unlike
				the normal xref property, the same restriction ("XReferrable xref
				Xref D:ControlledVocabulary=UnificationXref
				D:Provenance=UnificationXref,PublicationXref") must read/comprehend
				differently: it's actually now means Xref.xrefOf, and that
				RelationshipXref.xrefOf cannot contain a ControlledVocabulary (or
				its sub-class) values, etc.</p>
				<p><a href="javascript:switchit('inverse_properties_parameter')">Click here</a>
				to show/hide the list of properties</p>
			<!-- items are to be added here by a javascript -->
			<ul id="inverse_properties_parameter" style="display: none;"></ul>
			<br />
		</div>

		<!-- error codes -->
		<h2 id="errors">Errors:</h2>
		<p>
			If an error or no results happens while processing a user's request, 
			the client will receive a HTTP response with error status code and message
			(then browsers usually display a error page sent by the server; clients normally
			check the status before further processing the results, if any.)
		</p>
		<br />
	  </section>
	</div>
	<jsp:include page="footer.jsp" />

</body>
</html>
