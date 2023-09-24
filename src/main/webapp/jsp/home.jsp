<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
      <p><em><c:out value="${cpath.description}"/></em></p>
      <blockquote>
        <h2>Service endpoints:</h2>
        <ul id="commands" title="Service endpoints">
          <li><a href="#search"><code>/search</code></a></li>
          <li><a href="#get"><code>/get</code></a></li>
          <li><a href="#graph"><code>/graph</code></a></li>
          <li><a href="#traverse"><code>/traverse</code></a></li>
          <li><a href="#top_pathways"><code>/top_pathways</code></a></li>
        </ul>
      </blockquote>
      <blockquote>
        <p>See also:</p>
        <ul title="See also">
          <li><a href="swagger">API (Swagger)</a></li>
        </ul>
      </blockquote>
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
        A full-text search in this BioPAX database using <a href="http://lucene.apache.org">Lucene query syntax</a>.
        Index fields (case-sensitive): <em>uri, keyword, name, pathway, xrefid, datasource, organism</em>
        (some of these are BioPAX properties, while others are composite relationships), can be optionally used in a
        query string.
        For example, the <em>pathway</em> index field helps find pathway participants by keywords that match their
        parent pathway
        names or identifiers; <em>xrefid</em> finds objects by matching its direct or 'attached to a child element'
        Xrefs;
        <em>keyword</em>, the default search field, is a large aggregate that includes all BioPAX properties of an
        element
        and nested elements' properties (e.g. a Complex can be found by one of its member's name or EC Number).
        Search results can be filtered by data provider (<em>datasource</em> parameter), <em>organism</em>,
        and instantiable BioPAX class (<em>type</em>). Search can be used to select starting points for graph traversal
        queries
        (with '/graph', '/traverse', '/get' commands). Search strings are case insensitive unless put inside quotes.
    </p></blockquote>
    <h3>Returns:</h3>
    <p>ordered list of BioPAX individuals that match the search criteria
        (the page size, 'maxHitsPerpage' is configured on the server).
        The results (hits) are returned either as JSON or XML document (<a href="help/schema">Search Response XML Schema</a>),
        requested by using HTTP request header, either 'Accept: application/json' or 'Accept: application/xml'.
    </p>
    <h3 id="search_parameters">Parameters:</h3>
    <ul>
        <li><em>q=</em> [Required] a keyword, name, external identifier, or a Lucene query string.</li>
        <li><em>page=N</em> [Optional] (N&gt;=0, default is 0). Search results are paginated to avoid
            overloading the search response. This sets the search result page number.
        </li>
        <li><em>datasource=</em> [Optional] filjsonter by data source (use names or URIs
            of <a href="datasources">pathway data sources</a> or of any existing Provenance object).
            If multiple data source values are specified, a union of hits from specified sources is returned. For
            example,
            <em>datasource=reactome&amp;datasource=pid</em> returns hits associated with Reactome or PID.
        </li>
        <li><em>organism=</em> [Optional] organism filter. The organism can be specified either by official name, e.g.
            "homo sapiens" or by NCBI taxonomy identifier, e.g. "9606". Similar to data sources, if multiple organisms
            are
            declared, a union of all hits from specified organisms is returned. For example
            'organism=9606&amp;organism=10016' returns results for both human and mouse.
            Note the <a href="#organisms">officially supported species</a>.
        </li>
        <li><em>type=</em> [Optional] BioPAX class filter (<a href="#biopax_types">values</a>).
            NOTE: queries using &amp;type=biosource (or any BioPAX UtilityClass, such as Score, Evidence)
            filter won't not return any hits; use Entity (e.g., Pathway, Control,
            Protein) or EntityReference type (e.g., ProteinReference) instead.
        </li>
    </ul>
    <h3>Examples:</h3> <br/>
    <ol>
        <li><a rel="nofollow" href="search?q=FGFR2" >Find things that contain "FGFR2" keyword</a>
        </li>
        <li><a rel="nofollow"
               href='search?q=FGFR2&type=pathway'>Find pathways by FGFR2 keyword in any index field</a></li>
        <li><a rel="nofollow"
               href="search?q=xrefid:FGFR2&type=proteinreference">Search in 'xrefid' index, filter by protein reference
            type</a></li>
        <li><a rel="nofollow" href="search?q=*&page=2">Pagination example: get the third page of all indexed
            elements</a></li>
        <li><a rel="nofollow"
               href="search?q=+binding%20NOT%20transcription*&type=control">Finds Control
            interactions that contain the word "binding" but not "transcription" in their indexed fields</a></li>
        <li><a rel="nofollow" href="search?q=pathway:immune*&type=conversion">Find all
            interactions that directly or indirectly participate in a pathway that has a keyword match for "immune"</a>
        </li>
        <li><a rel="nofollow" href="search?q=*&type=pathway&datasource=reactome">All Reactome pathways</a></li>
    </ol>
</div>
<div class="row"><a href="#content" class="top-scroll">^top</a></div>

<div class="row nav-target" id="get">
    <h2>/get</h2>
    <blockquote><p>
        Retrieves an object model for one or several BioPAX elements, such as pathway,
        interaction or physical entity, given their URIs. Get commands only retrieve the specified
        and all the child BioPAX elements (one can use the <a href="#traverse">traverse</a> query
        to obtain parent elements).</p></blockquote>
    <h3>Parameters:</h3>
    <ul>
        <li><em>uri=</em> [Required] valid/existing BioPAX element's absolute URI
            (for utility classes that were "normalized", such as entity
            references and controlled vocabularies, it is usually an
            Identifiers.org URL. Multiple identifiers are allowed per query, for
            example, 'uri=http://identifiers.org/uniprot/Q06609&amp;uri=http://identifiers.org/uniprot/Q549Z0'
            <a href="#about_uris">See also</a> note about URIs and IDs.
        </li>
        <li><em>format=</em> [Optional] output format (<a
                href="#output_formats">values</a>)
        </li>
        <li><em>pattern=</em> [Optional] array of built-in BioPAX patterns to apply (SIF types - inference rule names;
            see <a href="formats#sif_relations">output format description</a>) when format=SIF or TXT is used;
            by default, all the pre-defined patterns but <i>neighbor-of</i> apply.
        </li>
        <li>
            <em>subpw=</em> [Optional] 'true' or 'false' (default) - whether to include or skip sub-pathways when we
            auto-complete and clone the requested BioPAX element(s) into a reasonable sub-model.
        </li>
    </ul>
    <h3>Output:</h3>
    BioPAX (default) representation for the record(s) pointed to by the given URI(s) is returned.
    Other output formats are produced on demand by converting from the BioPAX and can be specified using the optional
    format parameter.
    With some output formats, it might return no data (empty result) if the conversion is not applicable
    to the BioPAX model. For example, SIF output is only possible if there are some interactions, complexes, or pathways
    in the retrieved set.
    <h3>Examples:</h3>
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
    <h3>Parameters:</h3>
    <ul>
        <li><em>kind=</em> [Required] graph query (<a
                href="#graph_kinds">values</a>)
        </li>
        <li><em>source=</em> [Required] source object's URI/ID. Multiple source URIs/IDs are allowed per query, for
            example
            'source=http://identifiers.org/uniprot/Q06609&amp;source=http://identifiers.org/uniprot/Q549Z0'.
            See <a href="#about_uris">note about URIs and IDs</a>.
        </li>
        <li><em>target=</em> [Required for PATHSFROMTO graph query]
            target URI/ID. Multiple target URIs are allowed per query; for
            example 'target=http://identifiers.org/uniprot/Q06609&amp;target=http://identifiers.org/uniprot/Q549Z0'.
            See <a href="#about_uris">note about URIs and IDs</a>.
        </li>
        <li><em>direction=</em> [Optional, for NEIGHBORHOOD and COMMONSTREAM algorithms] - graph search direction (<a
                href="#graph_directions">values</a>).
        </li>
        <li><em>limit=</em> [Optional] graph query search distance limit (default = 1).
        </li>
        <li><em>format=</em> [Optional] output format (<a
                href="#output_formats">values</a>)
        </li>
        <li><em>pattern=</em> [Optional] array of built-in BioPAX patterns to apply (SIF types - inference rule names;
            see <a href="formats#sif_relations">output format description</a>) when format=SIF or TXT is used;
            by default, all the pre-defined patterns but <i>neighbor-of</i> apply.
        </li>
        <li><em>datasource=</em> [Optional] datasource filter (same as for <a href="#search_parameters">'search'</a>).
        </li>
        <li><em>organism=</em> [Optional] organism filter (same as for <a href="#search_parameters">'search'</a>).
        </li>
        <li>
            <em>subpw=</em> [Optional] 'true' or 'false' (default) - whether to include or skip sub-pathways;
            it does not affect the graph search algorithm, but - only how we auto-complete and clone BioPAX elements
            to make a reasonable sub-model from the result set.
        </li>
    </ul>
    <h3>Output:</h3>
    By default, it returns a BioPAX representation of the sub-network matched by the algorithm.
    Other output formats are available as specified by the optional format parameter.
    Some output format choices result in no data if the conversion is not applicable to the result BioPAX model
    (e.g., BINARY_SIF output fails if there are no interactions, complexes, nor pathways in the retrieved set).
    <h3>Examples:</h3>
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
        <em>[Initial Class]/[property1]:[classRestriction(optional)]/[property2]...</em>
        A "*" sign after the property instructs the path accessor to transitively traverse that property.
        For example, the following path accessor will traverse through all physical entity components a complex,
        including components of nested complexes, if any:
        <em>Complex/component*/entityReference/xref:UnificationXref</em>.
        The following will list the display names of all participants of interactions,
        which are pathway components of a pathway:
        <em>Pathway/pathwayComponent:Interaction/participant*/displayName</em>.
        Optional <em>classRestriction</em> allows to limit the returned property values to a certain subclass of the
        property's range.
        In the first example above, this is used to get only the unification xrefs.
        <a href="https://www.biopax.org/paxtools/apidocs/org/biopax/paxtools/controller/PathAccessor.html">
            Path accessors</a> can use all the official BioPAX properties as well as additional derived classes
        and parameters, such as inverse parameters and interfaces that represent anonymous union classes in BioPAX OWL.
        (See <a href="https://www.biopax.org/paxtools/">Paxtools documentation</a> for more details).
    </p></blockquote>
    <h3>Parameters:</h3>
    <ul>
        <li><em>path=</em> [Required] a BioPAX property path in the form of
            type0/property1[:type1]/property2[:type2]; see BioPAX
            <a href="#biopax_types">types</a>,
            <a href="#biopax_properties">properties</a>,
            <a href="#biopax_inverse_properties">inverse properties</a>,
            <a href="https://www.biopax.org/paxtools">Paxtools</a>,
            <a href="https://www.biopax.org/paxtools/apidocs/org/biopax/paxtools/controller/PathAccessor.html">
                org.biopax.paxtools.controller.PathAccessor</a>.
        </li>
        <li><em>uri=</em> [Required] a BioPAX element URI - specified similarly to the
            <a href="#get">'GET' command above</a>). Multiple URIs are
            allowed (uri=...&amp;uri=...&amp;uri=...). Standard gene/chemical IDs can now be used along with absolute
            URIs,
            which makes such request equivalent to two queries combined: 1) <i>search</i> for the specified BioPAX type
            objects
            by IDs in the 'xrefid' index field; 2) <i>traverse</i> - using URIs of objects found in the first step and
            the path.
        </li>
    </ul>
    <h3>Output:</h3>
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
    <h3>Parameters:</h3>
    <ul>
        <li><em>q=</em> [Required] a keyword, name, external identifier, or a Lucene query string,
            like in <a href="#search_parameters">'search'</a>, but the default is '*' (match all).
        </li>
        <li><em>datasource=</em> [Optional] filter by data source (same as for <a href="#search_parameters">'search'</a>).
        </li>
        <li><em>organism=</em> [Optional] organism filter (same as for <a href="#search_parameters">'search'</a>).
        </li>
    </ul>
    <h3>Output:</h3>
    XML document described by <a href="help/schema">Search Response XML
    Schema</a>&nbsp;(SearchResponse type; pagination is disabled to return all top pathways at once)<br/>
    <h3>Examples:</h3>
    <ol>
        <li><a rel="nofollow" href="top_pathways?q=TP53">
            get top pathways related to 'TP53'</a></li>
        <li><a rel="nofollow" href="top_pathways?q=insulin&datasource=reactome">
            get top pathways from Reactome, matching 'insulin'; request JSON format</a></li>
    </ol>
</div>
<div class="row"><a href="#content" class="top-scroll">^top</a></div>

<div class="row nav-target" id="parameter_values">
    <h2>Parameters</h2>
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
