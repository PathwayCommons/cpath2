<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html>
<head>
    <jsp:include page="head.jsp"/>
    <script src="<spring:url value='/scripts/help.js'/>"></script>
    <title>${cpath.name} home</title>
</head>
<body data-spy="scroll" data-target=".navbar">

<jsp:include page="header.jsp"/>

<div class="row" id="about">
    <div class="jumbotron">
      <h2>About <c:out value="${cpath.name} v${cpath.version}"/>:</h2>
      <p><c:out value="${cpath.description}"/></p>
      <p>Explore the <a href="swagger" target="_blank">Swagger API docs</a> and examples below.</p>
    </div>
</div>

<div class="row" id="notes">
    <h3><a id="about_uris"></a>URIs or IDs</h3>
    <p>
    Query parameters: <var>source, uri, target</var> accept an array of URIs of the BioPAX elements,
    which look like <var>bioregistry.io/&lt;prefix&gt;:&lt;identifier&gt;</var>
    (reference bio entities and vocabularies) or <var>${cpath.xmlBase}&lt;unique_id&gt;</var>
    (bio processes and participants). BioPAX URIs are not to guess; instead, they should be discovered with
    <code>/search</code> or <code>/top_pathways</code> queries. However, IDs from the following gene/protein or
    small molecule collections are also acceptable in place of URIs in <code>/get</code> and <code>/graph</code>
    queries:
    <ul title="These are" style="line-height:85%">
      <li>HUGO Gene Symbol</li>
      <li>NCBI Gene ID</li>
      <li>Uniprot AC</li>
      <li>RefSeq</li>
      <li>Ensembl</li>
      <li>ChEBI</li>
      <li>ChEMBL</li>
      <li>KEGG Compound</li>
      <li>DrugBank</li>
      <li>PharmGKB Drug</li>
      <li>PubChem Compound/Substance (must prefix with <var>CID:</var> or <var>SID:</var>, respectively,
      to distinguish from each other and Gene ID)</li>
    </ul>
    As a rule, using full URIs makes a precise query, whereas using the IDs makes a
    more exploratory one, which also depends on the full-text search (index) and id-mapping.
    </p>
</div>

<div class="row nav-target" id="search">
    <h2>/search</h2>
    <blockquote><p>
      A full-text search in the BioPAX database with <a href="http://lucene.apache.org">Lucene syntax</a>.
      The index field names are: <var>uri, keyword, name, pathway, xrefid, datasource, organism</var>.
      E.g., <var>keyword</var> is the default search field that includes most of BioPAX element's properties
      and nested properties (e.g. a Complex can be found by one of its member's names or ECNumber).
      Search results, specifically the URIs, can be starting point for the graph, get, traverse queries.
      Search strings are case insensitive, except for <var>xrefid, uri</var>, or when it's enclosed in quotes.
    </p></blockquote>
    <h3 id="search_parameters">input:</h3>
    <ul>
        <li><code>q=</code> [Required] a keyword, name, identifier, or a Lucene query string.</li>
        <li><code>page=N</code>  the search result page number (N&gt;=0, default is 0).</li>
        <li><code>datasource=</code>  filter by data source (an array of names, URIs
            of the <a href="datasources" target="_blank">data sources</a> or any <var>Provenance</var>).
            If multiple data source values are specified, a union of hits from specified sources is returned.
            For example, <var>datasource=reactome&amp;datasource=pid</var>.
        </li>
        <li><code>organism=</code>  organism filter; can be either the canonical name, e.g.
            <var>homo sapiens</var> or NCBI Taxon ID, <var>9606</var>. If multiple values
            are provided, then the union of hits is returned; e.g.,
            <var>organism=9606&amp;organism=10016</var> results in both human and mouse related hits.
            See also: <a href="#organisms">supported species</a> (other organisms data,
            such as viruses and model organisms, can go together with e.g. human models that we integrated).
        </li>
        <li><code>type=</code> BioPAX class filter (<a href="#biopax_types">values</a>; case-insensitive).
            Note that some query filters, such as <var>&amp;type=biosource</var>
            (for most BioPAX UtilityClass, such as Score, Evidence), will not return any hits.
            So, use Entity (e.g., Pathway, Control, Protein) or EntityReference types
            (e.g. ProteinReference, SmallMoleculeReference) instead.
        </li>
    </ul>
    <h3>output:</h3>
    <p>An ordered list of hits (the maximum number of hits per page is <var><c:out value="${cpath.maxHitsPerPage}"/></var>)
       as JSON or <a href="/help/schema" target="_blank">XML</a> depending on 'Accept: application/json'
       or 'Accept: application/xml' request header.</p>
    <h3>examples:</h3> <br/>
    <ol>
        <li><a rel="nofollow" href="search?q=FGFR2" >Find things by keyword:"FGFR2"</a></li>
        <li><a rel="nofollow" href='search?q=FGFR2&type=pathway'>Find pathways by keyword:"FGFR2"</a></li>
        <li><a rel="nofollow" href="search?q=xrefid:FGFR2&type=proteinreference">Find protein references by xref id</a></li>
        <li><a rel="nofollow" href="search?q=*&page=1">Pagination: find everything, the second result page</a></li>
        <li><a rel="nofollow" href="search?q=+binding%20NOT%20transcription*&type=control">Find Control interactions
          that contain "binding" but not "transcription" word</a></li>
        <li><a rel="nofollow" href="search?q=pathway:immune*&type=conversion">Find the interactions
          which pathway contains a keyword:"immune*" ('*' means any ending)</a></li>
        <li><a rel="nofollow" href="search?q=*&type=pathway&datasource=reactome&datasource=pid">Find all Reactome or PID
          pathways</a></li>
    </ol>
</div>
<div class="row"><a href="#content" class="top-scroll">^top</a></div>

<div class="row nav-target" id="get">
    <h2>/get</h2>
    <blockquote><p>
        Retrieves a BioPAX sub-model (one or several pathways, entities), the requested and child/nested elements,
        given the source URIs (one can use a /search or /traverse query first to find the input URIs for this query).</p>
    </blockquote>
    <h3>input:</h3>
    <ul>
        <li><code>uri=</code> [Required] valid/existing BioPAX element's absolute URI
            (for utility classes that were "normalized", such as entity
            references and controlled vocabularies, it is usually an
            Identifiers.org URL. Multiple identifiers are allowed per query, for
            example, 'uri=http://identifiers.org/uniprot/Q06609&amp;uri=http://identifiers.org/uniprot/Q549Z0'
            <a href="#about_uris">See also</a> note about URIs and IDs.
        </li>
        <li><code>format=</code>  output format (<a
                href="#output_formats">values</a>)
        </li>
        <li><code>pattern=</code>  array of built-in BioPAX patterns to apply (SIF types - inference rule names;
            see <a href="formats#sif_relations">output format description</a>) when format=SIF or TXT is used;
            by default, all the pre-defined patterns but <i>neighbor-of</i> apply.
        </li>
        <li>
            <code>subpw=</code>  'true' or 'false' (default) - whether to include or skip sub-pathways when we
            auto-complete and clone the requested BioPAX element(s) into a reasonable sub-model.
        </li>
    </ul>
    <h3>output:</h3>
    BioPAX (default) representation for the record(s) pointed to by the given URI(s) is returned.
    Other output formats are produced on demand by converting from the BioPAX and can be specified using the optional
    format parameter.
    With some output formats, it might return no data (empty result) if the conversion is not applicable
    to the BioPAX model. For example, SIF output is only possible if there are some interactions, complexes, or pathways
    in the retrieved set.
    <h3>examples:</h3>
    <ol>
        <li><a rel="nofollow" href="get?uri=http://identifiers.org/uniprot/Q06609&format=JSONLD">
            Gets the JSON-LD representation of Q06609</a> ProteinReference.
        </li>
        <li><a rel="nofollow" href="get?uri=FGFR2&format=gsea">This /get query</a>,
            unlike previous one, first performs full-text search by 'xrefid:FGFR2',
            and then converts result physical entities and genes (BioPAX sub-model) to GSEA GMT format.
        </li>
        <li><a rel="nofollow" href="get?uri=http://identifiers.org/reactome/R-HSA-201451">
            Get the 'Signaling by BMP' Pathway </a>
            (<a rel="nofollow" href="http://identifiers.org/reactome/R-HSA-201451">R-HSA-201451</a>,
            format: BioPAX, source: Reactome, human).
        </li>
    </ol>
</div>
<div class="row"><a href="#content" class="top-scroll">^top</a></div>

<div class="row nav-target" id="graph">
    <h2>/graph</h2>
    <blockquote><p>
        Graph searches are useful for finding connections and neighborhoods of elements, such as the
        shortest path between two proteins or the neighborhood for a particular protein state or all states. Graph
        searches consider detailed BioPAX semantics, such as generics or nested complexes, and traverse the graph
        accordingly. Note that we integrate data from multiple databases and consistently normalize UnificationXref,
        EntityReference, Provenance, BioSource, and ControlledVocabulary objects when we are absolutely sure that
        two objects of the same type are equivalent. We, however, do not merge physical entities and processes
        from different sources, as accurately matching and aligning pathways at that level is still an
        open research problem.</p></blockquote>
    <h3>input:</h3>
    <ul>
        <li><code>kind=</code> [Required] graph query (<a
                href="#graph_kinds">values</a>)
        </li>
        <li><code>source=</code> [Required] source object's URI/ID. Multiple source URIs/IDs are allowed per query, for
            example
            'source=http://identifiers.org/uniprot/Q06609&amp;source=http://identifiers.org/uniprot/Q549Z0'.
            See <a href="#about_uris">note about URIs and IDs</a>.
        </li>
        <li><code>target=</code> [Required for PATHSFROMTO graph query]
            target URI/ID. Multiple target URIs are allowed per query; for
            example 'target=http://identifiers.org/uniprot/Q06609&amp;target=http://identifiers.org/uniprot/Q549Z0'.
            See <a href="#about_uris">note about URIs and IDs</a>.
        </li>
        <li><code>direction=</code> [Optional, for NEIGHBORHOOD and COMMONSTREAM algorithms] - graph search direction (<a
                href="#graph_directions">values</a>).
        </li>
        <li><code>limit=</code>  graph query search distance limit (default = 1).
        </li>
        <li><code>format=</code>  output format (<a
                href="#output_formats">values</a>)
        </li>
        <li><code>pattern=</code>  array of built-in BioPAX patterns to apply (SIF types - inference rule names;
            see <a href="formats#sif_relations">output format description</a>) when format=SIF or TXT is used;
            by default, all the pre-defined patterns but <i>neighbor-of</i> apply.
        </li>
        <li><code>datasource=</code>  datasource filter (same as for <a href="#search_parameters">'search'</a>).
        </li>
        <li><code>organism=</code>  organism filter (same as for <a href="#search_parameters">'search'</a>).
        </li>
        <li>
            <code>subpw=</code>  'true' or 'false' (default) - whether to include or skip sub-pathways;
            it does not affect the graph search algorithm, but - only how we auto-complete and clone BioPAX elements
            to make a reasonable sub-model from the result set.
        </li>
    </ul>
    <h3>output:</h3>
    By default, it returns a BioPAX representation of the sub-network matched by the algorithm.
    Other output formats are available as specified by the optional format parameter.
    Some output format choices result in no data if the conversion is not applicable to the result BioPAX model
    (e.g., BINARY_SIF output fails if there are no interactions, complexes, nor pathways in the retrieved set).
    <h3>examples:</h3>
    Neighborhood of COL5A1 (P20908):
    <ol>
        <li><a rel="nofollow" href="graph?source=http://identifiers.org/uniprot/P20908&kind=neighborhood&format=SIF">
            BioPAX nearest neighborhood of the protein reference</a> http://identifiers.org/uniprot/P20908, i.e.,
            all reactions where the corresponding protein forms participate; returned as SIF
        </li>
        <li><a rel="nofollow" href="graph?source=P20908&kind=neighborhood">
            Nearest neighborhood of P20908</a> - starting from the corresponding Xref,
            finds all reactions that its owners (e.g., a protein reference) and their states (protein forms)
            participate in, and returns the BioPAX model.
        </li>
        <li><a rel="nofollow" href="graph?source=COL5A1&kind=neighborhood">
            A similar query using the gene symbol COL5A1 instead of URI or UniProt ID</a>
            (performs full-text search and id-mapping internally). Compared with other examples,
            a query like this potentially returns a larger sub-network, as
            it possibly starts graph traversing from multiple matching entities (seeds)
            rather than from a single ProteinReference.
            One can mix URIs along with UniProt, NCBI Gene, ChEBI IDs in a single /graph or /get query;
            other identifier types may also work. See: <a href="#about_uris">about URIs and IDs</a>.
        </li>
    </ol>
</div>
<div class="row"><a href="#content" class="top-scroll">^top</a></div>

<div class="row nav-target" id="traverse">
    <h2>/traverse</h2>
    <blockquote><p>
        XPath-like access to our BioPAX db. With '/traverse', users can
        explicitly state the paths they would like to access. The format of the path parameter value:
        <var>[Initial Class]/[property1]:[classRestriction(optional)]/[property2]...</var>
        A "*" sign after the property instructs the path accessor to transitively traverse that property.
        For example, the following path accessor will traverse through all physical entity components a complex,
        including components of nested complexes, if any:
        <var>Complex/component*/entityReference/xref:UnificationXref</var>.
        The following will list the display names of all participants of interactions,
        which are pathway components of a pathway:
        <var>Pathway/pathwayComponent:Interaction/participant*/displayName</var>.
        Optional <var>classRestriction</var> allows to limit the returned property values to a certain subclass of the
        property's range.
        In the first example above, this is used to get only the unification xrefs.
        <a href="https://www.biopax.org/paxtools/apidocs/org/biopax/paxtools/controller/PathAccessor.html">
            Path accessors</a> can use all the official BioPAX properties as well as additional derived classes
        and parameters, such as inverse parameters and interfaces that represent anonymous union classes in BioPAX OWL.
        (See <a href="https://www.biopax.org/paxtools/">Paxtools documentation</a> for more details).
    </p></blockquote>
    <h3>input:</h3>
    <ul>
        <li><code>path=</code> [Required] a BioPAX property path in the form of
            type0/property1[:type1]/property2[:type2]; see BioPAX
            <a href="#biopax_types">types</a>,
            <a href="#biopax_properties">properties</a>,
            <a href="#biopax_inverse_properties">inverse properties</a>,
            <a href="https://www.biopax.org/paxtools">Paxtools</a>,
            <a href="https://www.biopax.org/paxtools/apidocs/org/biopax/paxtools/controller/PathAccessor.html">
                org.biopax.paxtools.controller.PathAccessor</a>.
        </li>
        <li><code>uri=</code> [Required] a BioPAX element URI - specified similarly to the
            <a href="#get">'GET' command above</a>). Multiple URIs are
            allowed (uri=...&amp;uri=...&amp;uri=...). Standard gene/chemical IDs can now be used along with absolute
            URIs,
            which makes such request equivalent to two queries combined: 1) <i>search</i> for the specified BioPAX type
            objects
            by IDs in the 'xrefid' index field; 2) <i>traverse</i> - using URIs of objects found in the first step and
            the path.
        </li>
    </ul>
    <h3>output:</h3>
    XML result according to the <a href="help/schema">Search Response XML
    Schema</a>&nbsp;(TraverseResponse type; pagination is disabled to return all values at once)<br/>
    <h3>Examples (using human data):</h3>
    <ol>
        <li><a rel="nofollow"
               href="traverse?uri=http://identifiers.org/uniprot/P38398&path=ProteinReference/organism/displayName">
            This query returns the display name of the organism of the ProteinReference specified by the URI.</a></li>
        <li><a rel="nofollow"
               href="traverse?uri=http://identifiers.org/uniprot/P38398&uri=http://identifiers.org/uniprot/Q06609&path=ProteinReference/organism">
            This query returns the URI of the organism for each of the Protein References</a></li>
        <li><a rel="nofollow"
               href="traverse?uri=http://identifiers.org/uniprot/Q06609&path=ProteinReference/entityReferenceOf:Protein/name">
            This query returns the names of all states of RAD51 protein (by its ProteinReference URI, using
            property path="ProteinReference/entityReferenceOf:Protein/name")</a></li>
        <li><a rel="nofollow"
               href="traverse?uri=http://identifiers.org/uniprot/P38398&path=ProteinReference/entityReferenceOf:Protein">
            This query returns the URIs of states of BRCA1_HUMAN</a></li>
        <li><a rel="nofollow"
               href="traverse?uri=http://identifiers.org/uniprot/P38398&uri=http://identifiers.org/taxonomy/9606&path=Named/name">
            This query returns the names of several different objects (using abstract type 'Named' from Paxtools
            API)</a></li>
    </ol>
</div>
<div class="row"><a href="#content" class="top-scroll">^top</a></div>

<div class="row nav-target" id="top_pathways">
    <h2>/top_pathways</h2>
    <blockquote><p>
        Finds root pathways - that are neither 'controlled' nor a 'pathwayComponent' of another biological process,
        excluding trivial ones.</p></blockquote>
    <h3>input:</h3>
    <ul>
        <li><code>q=</code> [Required] a keyword, name, external identifier, or a Lucene query string,
            like in <a href="#search_parameters">'search'</a>, but the default is '*' (match all).
        </li>
        <li><code>datasource=</code> filter by data source (same as for <a href="#search_parameters">'search'</a>).
        </li>
        <li><code>organism=</code> organism filter (same as for <a href="#search_parameters">'search'</a>).
        </li>
    </ul>
    <h3>output:</h3>
    XML document described by <a href="help/schema">Search Response XML
    Schema</a>&nbsp;(SearchResponse type; pagination is disabled to return all top pathways at once)<br/>
    <h3>examples:</h3>
    <ol>
        <li><a rel="nofollow" href="top_pathways?q=TP53">
            get top pathways related to 'TP53'</a></li>
        <li><a rel="nofollow" href="top_pathways?q=insulin&datasource=reactome">
            get top pathways from Reactome, matching 'insulin'; request JSON format</a></li>
    </ol>
</div>
<div class="row"><a href="#content" class="top-scroll">^top</a></div>

<div class="row nav-target" id="parameter_values">
    <h2>Parameter Values</h2>
    <div class="parameters" id="organisms">
        <h3>Organisms</h3>
        <p>We intend to integrate pathway data only for the following species:</p>
        <ul>
            <c:forEach var="org" items="${cpath.organisms}">
                <em><strong><c:out value="${org}"/></strong></em>
            </c:forEach>
        </ul>
        <p>Additional organisms may be pulled in due to interactions with entities
            from any of the above organisms, but are not otherwise supported.
            This means that we don’t comprehensively collect information for
            unsupported organisms and we have not cleaned or converted
            such data due to the high risk of introducing errors and artifacts.
            All BioSource objects can be found by using
            <a rel="nofollow" href="search?q=*&type=biosource">this search query</a>.
        </p>
    </div>
        <div class="parameters" id="output_formats">
            <h3>Output Format ('format'):</h3>
            <p>
                For detailed descriptions of these formats, see <a href="formats">output format description.</a>
            </p>
            <!-- items are to be added here by a javascript -->
            <ul id="formats"></ul>
        </div>
        <div class="parameters" id="graph_kinds">
            <h3>Graph Type ('kind'):</h3>
            <!-- items are to be added here by a javascript -->
            <ul id="kinds"></ul>
        </div>
        <div class="parameters" id="graph_directions">
            <h3>Graph Directions ('direction'):</h3>
            <!-- items are to be added here by a javascript -->
            <ul id="directions"></ul>
        </div>
        <div class="parameters" id="biopax_types">
            <h3>BioPAX class ('type'):</h3>
            <p><a href="#" class="hider" hide-id="types">Click here</a> to show/hide the list
                (see also: <a href="https://www.biopax.org/owldoc/Level3/" target="_blank">BioPAX Classes</a>).
            </p>
            <!-- items are to be added here by a javascript -->
            <ul class="dropdown hidden" id="types"></ul>
            <br/>
        </div>
        <div class="parameters" id="biopax_properties">
            <h3>BioPAX Properties and Restrictions:</h3>
            <p>Listed below are BioPAX properties' summary as defined
                in the Paxtools model: domain, property name, range and restrictions (if any).
                For example, <em>XReferrable xref Xref D:ControlledVocabulary=UnificationXref
                    D:Provenance=UnificationXref,PublicationXref</em> means that
                values of ControlledVocabulary.xref can only be of <em>UnificationXref</em> type.</p>
            <p><a href="#" class="hider" hide-id="properties">Click here</a>
                to show/hide the list of properties</p>
            <!-- items will be added here by the javascript -->
            <ul id="properties" class="hidden"></ul>
        </div>
        <div class="parameters" id="biopax_inverse_properties">
            <h3>Inverse BioPAX Object Properties (a feature of the <a href="http://biopax.org/paxtools">Paxtools
                library</a>):</h3>
            <p>Some of the BioPAX object properties can be traversed in the inverse direction, e.g, 'xref' - 'xrefOf'.
                Unlike for the standard <em>xref</em> property, e.g., the restriction <em>XReferrable xref
                    Xref D:ControlledVocabulary=UnificationXref D:Provenance=UnificationXref,PublicationXref</em>
                below must be read <em>right-to-left</em> as it is actually about Xref.xrefOf:
                RelationshipXref.xrefOf cannot contain neither <em>ControlledVocabulary</em>
                (any sub-class) nor <em>Provenance</em> objects
                (in other words, vocabularies and provenance may not have any relationship xrefs).</p>
            <p><a href="#" class="hider" hide-id="inverse_properties">Click here</a>
                to show/hide the list of properties</p>
            <!-- items are to be added here by a javascript -->
            <ul id="inverse_properties" class="hidden"></ul>
        </div>
</div>
<div class="row"><a href="#content" class="top-scroll">^top</a></div>
<jsp:include page="footer.jsp"/>
</body>
</html>
