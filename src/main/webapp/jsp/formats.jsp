<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>

<!DOCTYPE html>
<html>
<head>
<jsp:include page="head.jsp" />
<title>${cpath.name} formats</title>
</head>
<body data-spy="scroll" data-target=".navbar">
<jsp:include page="header.jsp" />

<div class="jumbotron">
<h2>Output Formats</h2>
<p>
The GET and GRAPH web service commands return data in several formats explained below.
</p>
</div>

<h3>BioPAX (RDF/XML)</h3>
<p>
<a href="https://www.biopax.org">BioPAX</a> is the default
and most complete output format of ${cpath.name} that offers
access to all the details of the biological network model stored in the system. 
This format is ideal for users wishing to to access specific data not
available in simple formats. Since BioPAX is defined using the standard
OWL (RDF/XML) syntax, this format can also be used with RDF/OWL tools such as
reasoners or triplestores.  All pathways and interactions within
the database are available in BioPAX Level 3.  Due to the richness 
of representation in BioPAX, reading and using such a large BioPAX
document requires knowledge of the format and software development
tools available for processing it, such as 
<a href="https://www.biopax.org/paxtools">Paxtools</a>,
a Java library for working with BioPAX as object model, or Jena, SPARQL.
</p>

<h3 id="jsonld">JSON-LD</h3>
<p>
    <a href="https://json-ld.org/">JSON-LD</a> is a lightweight Linked Data format.
    It is easy for humans to read and write. It is based on the already successful JSON format and provides a way
    to help JSON data interoperate at Web-scale. JSON-LD is an ideal data format for programming environments,
    REST Web services, and unstructured databases such as CouchDB and MongoDB.
    Paxtools' json-converter module, based on the Apache Jena libraries, helps convert a BioPAX model or element
    to JSON-LD format.
</p>

<h3 id="gsea">Gene Set Enrichment Format (GSEA - MSigDB GMT)</h3>
<p>
Over-representation analysis (ORA) is frequently used to assess the
statistical enrichment of known gene sets (e.g. pathways) in a
discrete or ranked list of genes.  This type of analysis is useful for
summarizing large gene lists and is commonly applied to
genomics data sets.  One popular software for analyzing ranked gene
lists is Gene Set Enrichment Analysis (GSEA).  The Gene sets used by
GSEA are stored for convenience in the Molecular Signature Database
(MSigDB) in the Gene Matrix Transposed file format (*.gmt).  This is
the main tab-delimited file format specified by the 
<a href="http://www.broad.mit.edu/gsea/msigdb/">Broad Molecular Signature Database</a>.
</p><p>Each gene set is described by a name, a description, and the genes in the gene set: 
participants in a pathway are specified with one or several HGNC symbols 
(we can also provide another file using UniProt accession numbers instead).
All participants (corresponding BioPAX EntityReferences) for a pathway 
must come from the same species as the pathway. Participants from cross-species pathways, 
as well as those for which no identifier is found (i.e., when there're no Xrefs of given type), are removed.  
Exporting to the MSigDB format will enable computational biologists to use pathway commons data
within gene set enrichment algorithms, such as GSEA. Available for all
pathways within Pathway Commons (only from pathway database sources,
not interaction database sources). Full data format details are available at 
<a href="http://www.broadinstitute.org/cancer/software/gsea/wiki/index.php/Data_formats#GMT:_Gene_Matrix_Transposed_file_format_.28.2A.gmt.29">Broad GSEA Wiki</a>.
We used the <strong>normalized and merged</strong> BioPAX Level3 model and our 
simple GSEA converter from the Paxtools library to generate the GSEA (.gmt) archives.
(Note: to effectively enforce cross-species check, BioSources must have 
a UnificationXref with "taxonomy" database name and id, and Pathways, 
ProteinReferences - not empty 'organism' property value).
</p>

<h3 id="sif">Simple Interaction Format (SIF)</h3>
<h4>SIF (or BINARY_SIF)</h4>
<p>
Many network analysis algorithms require pairwise interaction networks 
as input. A BioPAX network often contains more complex relationships
with multiple participants, such as biochemical reactions. To make it
easier to use all of the pathway information in Pathway Commons with
typical network analysis tools, we developed a set of rules to reduce
BioPAX interactions to pairwise (or binary) relationships.  
Since SIF interactions are always binary it is not possible to fully 
represent all of BioPAX, thus this translation is lossy in general. 
Nonetheless, the SIF network is useful for those applications that require pairwise
interaction input.  SIF format can be easily imported into popular network analysis tools, 
such as <a href="http://wiki.cytoscape.org/Cytoscape_User_Manual/Network_Formats#SIF_Format">Cytoscape</a>.
</p><p>
In this output format, all participants are specified as chemical or gene 
names or identifiers. This format does not contain any cross-species
interactions and is available for all pathways and interactions within
this database.</p><p>
A note about identifiers: We uniquely mapped selected protein and gene identifiers, such as
HGNC symbols, NCBI Gene, UniProt Isoform, Ensembl and RefSeq, 
to primary UniProt accession numbers, where possible, 
and then normalized and merged original 
protein types to canonical UniProt ones, 
thus building a larger BioPAX network of all pathways, interactions and participants 
from different data sources. In some cases, mappings between identifiers cannot be made, 
so it is possible to lost some information in this process. Also, in cases where 
the SIF format contains a non-UniProt identifier (e.g. HGNC Symbol), it is possible
that more than one identifier maps to a UniProt identifier. In this case, a duplicate SIF 
interaction is created for each additional non-UniProt identifier.
</p>

<h4 id="esif">TXT (or EXTENDED_BINARY_SIF)</h4>
<p>
Similar to the basic SIF output format, except that this output format is
specified in two sections. Each section starts with one row of column
headings.  The two sections are separated by a single blank line. 
Each entry is multi-column, tab-delimited. The first section is SIF (edges)
as describe above, plus PATHWAYS column. 
Current edge attributes include the interaction data source and PubMed ID.  
The second section contains participant (molecule or gene) name followed by several node attributes.  
Current node attributes include PARTICIPANT_TYPE, PARTICIPANT_NAME(s), UNIFICATION_XREF(s)
(e.g., one or more UniProt IDs in the case of a protein reference, or a
ChEBI ID in the case of a Small Molecule reference), and RELATIONSHIP_XREF(s) 
(including RefSeq, Entrez Gene, and Gene Symbol).
Xrefs are represented as a NAME:VALUE pair; for example PubMed:9136927.  
Multiple names or xrefs will be separated by a semicolon ';'.  
This output format is suitable for Cytoscape - Attribute Table import and loading into
Excel.  To prevent an unsuccessful import into Cytoscape due to
missing attribute values, users should specify during import that all
columns are strings.  This format is available for all pathways and
interactions within Pathway Commons.
</p>

<h4 id="sif_relations">Types of Binary Relations</h4>
<table class="table table-striped table-bordered">
<thead>
<tr>
<th>Name</th>
<th>Description</th>
<th>Sample BioPAX Structure</th>
<th>Inferred Binary Relation(s)</th>
</tr>
</thead>
<tbody>
<tr>
<td><b>controls-state-change-of</b></td>
<td>First protein controls a reaction that changes the state of the second protein.</td>
<td><img src="<spring:url value='/img/sif/controls-state-change-of-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/B-to-A.svg'/>"/></td>
</tr>
<tr>
<td><b>controls-transport-of</b></td> 
<td>First protein controls a reaction that changes the cellular location of the second protein.</td>
<td><img src="<spring:url value='/img/sif/controls-transport-of-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/B-to-A.svg'/>"/></td>
</tr>
<tr>
<td><b>controls-phosphorylation-of</b></td> 
<td>First protein controls a reaction that changes the phosphorylation status of the second protein.</td>
<td><img src="<spring:url value='/img/sif/controls-phosphorylation-of-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/B-to-A.svg'/>"/></td>
</tr>
<tr>
<td><b>controls-expression-of</b></td> 
<td>First protein controls a conversion or a template reaction that changes expression of the second protein.</td>
<td><img src="<spring:url value='/img/sif/controls-expression-of-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/B-to-A.svg'/>"/></td>
</tr>
<tr>
<td><b>catalysis-precedes</b></td>
<td>First protein controls a reaction whose output molecule is input to another reaction controled by the second protein.</td>
<td><img src="<spring:url value='/img/sif/catalysis-precedes-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/A-to-B.svg'/>"/></td>
</tr>
<tr>
<td><b>in-complex-with</b></td> 
<td>Proteins are members of the same complex.</td>
<td><img src="<spring:url value='/img/sif/in-complex-with-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/triangle.svg'/>"/></td>
</tr>
<tr>
<td><b>interacts-with</b></td> 
<td>Proteins are participants of the same MolecularInteraction.</td>
<td><img src="<spring:url value='/img/sif/interacts-with-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/interacts-with-sif.svg'/>"/></td>
</tr>
<tr>
<td><b>neighbor-of</b></td> 
<td>Proteins are participants or controlers of the same interaction.</td>
<td><img src="<spring:url value='/img/sif/neighbor-of-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/neighbor-of-sif.svg'/>"/></td>
</tr>
<tr>
<td><b>consumption-controled-by</b></td>
<td>The small molecule is consumed by a reaction that is controled by a protein</td>
<td><img src="<spring:url value='/img/sif/metabolic1-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/X-to-A.svg'/>"/></td>
</tr>
<tr>
<td><b>controls-production-of</b></td>
<td>The protein controls a reaction of which the small molecule is an output.</td>
<td><img src="<spring:url value='/img/sif/metabolic1-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/controls-production-of-sif.svg'/>"/></td>
</tr>
<tr>
<td><b>controls-transport-of-chemical</b></td>
<td>The protein controls a reaction that changes cellular location of the small molecule.</td>
<td><img src="<spring:url value='/img/sif/controls-transport-of-chemical-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/controls-transport-of-chemical-sif.svg'/>"/></td>
</tr>
<tr>
<td><b>chemical-affects</b></td>
<td>A small molecule has an effect on the protein state.</td>
<td><img src="<spring:url value='/img/sif/chemical-affects-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/X-to-A.svg'/>"/></td>
</tr>
<tr>
<td><b>reacts-with</b></td>
<td>Small molecules are input to a biochemical reaction.</td>
<td><img src="<spring:url value='/img/sif/metabolic2-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/reacts-with-sif.svg'/>"/></td>
</tr>
<tr>
<td><b>used-to-produce</b></td>
<td>A reaction consumes a small molecule to produce another small molecule.</td>
<td><img src="<spring:url value='/img/sif/metabolic2-biopax.svg'/>"/></td>
<td><img src="<spring:url value='/img/sif/used-to-produce-sif.svg'/>"/></td>
</tr>
</tbody>
</table>
<p></p>
<table class="table"><tr><td>Legend:</td><td><img src="<spring:url value='/img/sif/legend.svg'/>"/></td></tr></table>

<h3 id="sbgn">SBGN</h3>
<p>
The Systems Biology Graphical Notation (<a href="http://www.sbgn.org">SBGN</a>)
is a standard visual notation for network diagrams in biology. 
SBGN markup language (SBGN-ML) is an associated standard XML format that can be loaded into available 
software to visualize a diagram of a pathway. BioPAX can be converted to SBGN-ML format, following the 
process diagram paradigm, one of three paradigms (activity flow, process and entity relationship) available in SBGN.
</p>

<jsp:include page="footer.jsp" />
</body>
</html>
