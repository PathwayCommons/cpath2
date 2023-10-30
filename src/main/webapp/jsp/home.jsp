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
      <p>Explore the new <a href="swagger">Swagger API docs:</a></p>
      <blockquote>
      <ul>
      <li><a href="swagger#/biopax-model-controller/searchQuery"><code>/search</code></a></li>
      <li><a href="swagger#/biopax-model-controller/topPathwaysQuery"><code>/top_pathways</code></a></li>
      <li><a href="swagger#/biopax-model-controller/traverseQuery"><code>/traverse</code></a></li>
      <li><a href="swagger#/biopax-model-controller/neighborhoodQuery"><code>/neighborhood</code></a></li>
      <li><a href="swagger#/biopax-model-controller/pathsbetweenQuery"><code>/pathsbetween</code></a></li>
      <li><a href="swagger#/biopax-model-controller/pathsfromtoQuery"><code>/pathsfromto</code></a></li>
      <li><a href="swagger#/biopax-model-controller/commonstreamQuery"><code>/commonstream</code></a></li>
      </ul>
      </blockquote>
    </div>
</div>

<div class="row nav-target" id="examples">
    <h2>Some Examples</h2>
    <p>
     Links below use the API v1 (deprecated) and HTTP GET (query parameters are URL-encoded)
    </p>
    <ol>
        <li><a rel="nofollow" href="search?q=FGFR2" target="_blank">
            Find something by keyword:"FGFR2"</a></li>
        <li><a rel="nofollow" href='search?q=CALCRL&type=pathway' target="_blank">
            Find pathways by keyword:"CALCRL"</a></li>
        <li><a rel="nofollow" href="search?q=xrefid:FGFR2&type=proteinreference" target="_blank">
            Find protein references by xref id</a></li>
        <li><a rel="nofollow" href="search?q=*&page=1" target="_blank">
            Pagination: find everything, the second result page</a></li>
        <li><a rel="nofollow" href="search?q=+binding%20NOT%20transcription*&type=control" target="_blank">
            Find Control interactions that contain "binding" but not "transcription" word</a></li>
        <li><a rel="nofollow" href="search?q=pathway:immune*&type=conversion" target="_blank">
            Find the interactions which pathway contains a keyword:"immune*" ('*' means any ending)</a></li>
        <li><a rel="nofollow" href="search?q=*&type=pathway&datasource=reactome&datasource=pid" target="_blank">
            Find all Reactome or PID pathways</a></li>
        <li><a rel="nofollow" href="get?uri=bioregistry.io/uniprot:Q06609&format=jsonld" target="_blank">
            Get a ProteinReference (Q06609) by absolute URI and convert to JSON-LD format</a>
        </li>
        <li><a rel="nofollow" href="get?uri=FGFR2&format=gsea" target="_blank">
            Find, fetch by bio identifier (FGFR2) and convert to GSEA format</a>
            (it first performs a full-text search 'xrefid:FGFR2' to find bioentity URIs, then extracts the
            sub-model and converts to GSEA GMT format
        </li>
        <li><a rel="nofollow" href="get?uri=bioregistry.io/reactome:R-HSA-201451" target="_blank">
            Get 'Signaling by BMP' Pathway</a> (format: BioPAX;
            see also: <a rel="nofollow" href="https://bioregistry.io/reactome:R-HSA-201451">R-HSA-201451</a>)
        </li>
        <li><a rel="nofollow" href="graph?source=bioregistry.io/uniprot:P20908&kind=neighborhood&format=SIF" target="_blank">
            Nearest neighborhood of the protein reference bioregistry.io/uniprot:P20908 (COL5A1)</a>,
            all reactions where the corresponding protein forms/states take part, converted to SIF format
        </li>
        <li><a rel="nofollow" href="graph?source=COL5A1&kind=neighborhood" target="_blank">
            Nearest neighborhood query using the gene symbol COL5A1 (P20908) instead of absolute URI</a>
            (performs full-text search and id-mapping internally; such query potentially returns a larger sub-network
            as it might start from more matching entities rather than from one ProteinReference;
            One can mix URIs along with UniProt, NCBI Gene, ChEBI IDs in a single graph or get/fetch query;
            other identifiers might also work; see: <a href="#about_uris">about URIs and IDs)</a>
        </li>
        <li><a rel="nofollow" target="_blank"
               href="traverse?uri=bioregistry.io/uniprot:P38398&path=ProteinReference/organism/displayName">
            Traverse to find the display name of the organism of the ProteinReference specified by the URI</a></li>
        <li><a rel="nofollow" target="_blank"
               href="traverse?uri=bioregistry.io/uniprot:P38398&uri=bioregistry.io/uniprot:Q06609&path=ProteinReference/organism">
            Traverse to find the URI of the organism for each of the Protein References</a></li>
        <li><a rel="nofollow" target="_blank"
               href="traverse?uri=bioregistry.io/uniprot:Q06609&path=ProteinReference/entityReferenceOf:Protein/name">
            Traverse to list the names of all states of RAD51 protein (by its ProteinReference URI, using
            property path="ProteinReference/entityReferenceOf:Protein/name")</a></li>
        <li><a rel="nofollow" target="_blank"
               href="traverse?uri=bioregistry.io/uniprot:P38398&path=ProteinReference/entityReferenceOf:Protein">
            Traverse to list the URIs of states of BRCA1_HUMAN</a></li>
        <li><a rel="nofollow" target="_blank"
               href="traverse?uri=bioregistry.io/uniprot:P38398&uri=bioregistry.io/ncbitaxon:9606&path=Named/name">
            Traverse to list the names of several different objects (using abstract type 'Named' from Paxtools
            API)</a></li>
        <li><a rel="nofollow" target="_blank" href="top_pathways?q=TP53">
            Find the top-level pathways related to 'TP53'</a></li>
        <li><a rel="nofollow" target="_blank" href="top_pathways?q=insulin&datasource=reactome">
            Find the top pathways from Reactome, matching 'insulin'; request JSON format</a></li>
    </ol>
</div>

<div class="row nav-target" id="parameter_values">
    <h2>Parameter Values</h2>
    <h3><a id="about_uris"></a>About URIs or IDs</h3>
    <p>
        Query parameters: <var>source, uri, target</var> accept an array of URIs of the BioPAX elements,
        which look like <var>bioregistry.io/&lt;prefix&gt;:&lt;identifier&gt;</var>
        (reference bio entities and vocabularies) or <var>${cpath.xmlBase}&lt;unique_id&gt;</var>
        (bio processes and participants). BioPAX URIs are not to guess; instead, they should be discovered with
        <code>/search</code> or <code>/top_pathways</code> queries. However, IDs from the following gene/protein or
        small molecule collections are also acceptable in place of URIs in the fetch and graph queries:
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
    <p>
        We integrated multiple <a href='datasources'>data sources</a> and consistently normalized Xref,
        EntityReference, Provenance, BioSource, and ControlledVocabulary objects. We did not merge physical entities (states)
        and processes from different sources automatically, as accurately matching and aligning pathways at that level
        is still an open research problem.
    </p>
    <h3><a id="about_enum"></a>About Enumerations</h3>
    <p>
    Note that enumeration parameter values, such as those for Type, Format, Direction, Limit, Pattern,
    are case-insensitive and can also use e.g. "interacts-with" instead of "INTERACTS_WITH" (both '-' and '_'
    are acceptable), "jsonld" instead "JSONLD", "neighborhood" instead of "NEIGHBORHOOD", etc.
    </p>

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
            All BioSource objects can be found by using a <code>/search</code> query
            with parameters: <var>{"q":"*", "type":"biosource"}</var>.
        </p>
    </div>
        <div class="parameters" id="output_formats">
            <h3>Output Format ('format'):</h3>
            <p>
                For detailed descriptions of these formats, see <a href="formats">output format description.</a>
            </p>
            <%-- items are to be added here by a javascript --%>
            <ul id="formats"></ul>
        </div>
        <div class="parameters" id="graph_kinds">
            <h3>Graph Type ('kind'):</h3>
            <%-- items are to be added here by a javascript --%>
            <ul id="kinds"></ul>
        </div>
        <div class="parameters" id="graph_limits">
            <h3>Graph Limit Types ('limitType'):</h3>
            <%-- items are to be added here by a javascript --%>
            <ul id="limits"></ul>
        </div>
        <div class="parameters" id="graph_directions">
            <h3>Graph Directions ('direction'):</h3>
            <%-- items are to be added here by a javascript --%>
            <ul id="directions"></ul>
        </div>
        <div class="parameters" id="biopax_types">
            <h3>BioPAX class ('type'):</h3>
            <p><a href="#" class="hider" hide-id="types">Click here</a> to show/hide the list
                (see also: <a href="https://www.biopax.org/owldoc/Level3/">BioPAX Classes</a>).
            </p>
            <%-- items are to be added here by a javascript --%>
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
            <h3>Inverse BioPAX Object Properties:</h3>
            <p>Thanks to <a href="http://biopax.org/paxtools">Paxtools</a>, some of the BioPAX object properties
            can be traversed in the inverse direction, e.g, 'xref' - 'xrefOf'.
            Unlike for the standard <em>xref</em> property, e.g., the restriction <em>XReferrable xref
            Xref D:ControlledVocabulary=UnificationXref D:Provenance=UnificationXref,PublicationXref</em>
            below must be read <em>right-to-left</em> as it is actually about Xref.xrefOf:
            RelationshipXref.xrefOf cannot contain neither <em>ControlledVocabulary</em>
            (any sub-class) nor <em>Provenance</em> objects
            (in other words, vocabularies and provenance may not have any relationship xrefs).</p>
            <p><a href="#" class="hider" hide-id="inverse_properties">Click here</a>
                to show/hide the list of properties</p>
            <%-- items are to be added here by a javascript --%>
            <ul id="inverse_properties" class="hidden"></ul>
        </div>
</div>
<div class="row"><a href="#content" class="top-scroll">^top</a></div>

<jsp:include page="footer.jsp"/>
</body>

</html>
