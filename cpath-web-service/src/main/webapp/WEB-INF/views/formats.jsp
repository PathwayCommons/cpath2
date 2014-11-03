<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>

<!DOCTYPE html>
<html>
<head>
<jsp:include page="head.jsp" />
<title>cPath2::Formats</title>
</head>
<body data-spy="scroll" data-target=".navbar">
<jsp:include page="header.jsp" />

<div class="jumbotron">
<h2>Output Formats</h2>
<p>
The GET and GRAPH web service commands, as well as 
<a href="<c:url value="/downloads"/>">batch downloads</a>, 
provide output data in several formats explained below.
</p>
</div>

<h3>BioPAX (RDF/XML)</h3>
<p>
<a target="_blank" href="http://www.biopax.org">BioPAX</a> is the default 
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
<a target="_blank" href="http://www.biopax.org/paxtools">Paxtools</a>, 
a Java library for working with BioPAX as object model, or Jena, SPARQL.
</p>

<h3>Gene Set Enrichment Format (GSEA - MSigDB GMT)</h3>
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
<a target="_blank" href="http://www.broad.mit.edu/gsea/msigdb/">Broad Molecular Signature Database</a>.
</p><p>
In this output format, all participants in the pathway are specified with
a UniProt Accession.  All participants for a pathway must come from
the same species as the pathway.  Therefore some participants from
cross-species pathways are removed.  Exporting to the MSigDB format
will enable computational biologists to use pathway commons data
within gene set enrichment algorithms, such as GSEA. Available for all
pathways within Pathway Commons (only from pathway database sources,
not interaction database sources). Full data format details are available at 
<a target="_blank" href="http://www.broad.mit.edu/cancer/software/gsea/wiki/index.php/Data_formats">Broad GSEA Wiki</a>.
We used the <strong>normalized and merged</strong> BioPAX L3 data and the 
<a target="_blank" href="http://www.biopax.org/paxtools/gsea-converter/apidocs/org/biopax/paxtools/io/gsea/GSEAConverter.html">
simple GSEA converter from the Paxtools library</a> to generate the corresponding GSEA archives.
It creates one or many GSEA entries (rows) in each output file from ProteinReferences's 
UniProt Xrefs - one line (UniProt id-list) per pathway per organism, 
one UniProt accession per ProteinReference. If there are no pathways, then simply - per organism.  
(Note: the converter does not do any additional id-mapping; thus a protein without
a UniProt identifier in its URI or Xref will be ignored; also, to effectively enforce cross-species 
check, BioSources must have a UnificationXref with "taxonomy" database name
and id, and Pathways, ProteinReferences - not empty 'organism' property value).
</p>

<h3>Simple Interaction Format (SIF)</h3>
<h4>BINARY_SIF</h4>
<p>
Many network analysis algorithms require pairwise interaction networks 
as input.  A BioPAX network often contains more complex relationships
with multiple participants, such as biochemical reactions.  To make it
easier to use all of the pathway information in Pathway Commons with
typical network analysis tools, we developed a set of rules to reduce
BioPAX interactions to pairwise (or binary) relationships.  Since SIF interactions
are always binary it is not possible to fully represent all of BioPAX,
thus this translation is lossy in general.  Nonetheless, the SIF
network is useful for those applications that require pairwise
interaction input.  SIF format can be easily imported into popular network analysis tools, 
like <a target="_blank" href="http://wiki.cytoscape.org/Cytoscape_User_Manual/Network_Formats">Cytoscape</a>.
</p><p>
In this output format, all participants will be specified as chemical or gene names or IDs.
This format does not contain any cross-species
interactions and is available for all pathways and interactions within
this database.
</p>

<h4>EXTENDED_BINARY_SIF</h4>
<p>
Similar to the basic SIF output format, except that this output format is
specified in two sections. Each section starts with one row of column
headings.  The two sections are separated by a single blank line. 
Each entry is multi-column, tab-delimited. The first section is BINARY_SIF (edges) 
as describe above. Current edge attributes include the interaction data source and PubMed ID.  
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

<h4>Types of Binary Relations</h4>
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
<td><img src='/resources/img/sif/controls-state-change-of-biopax.svg'/></td>
<td><img src='/resources/img/sif/B-to-A.svg'/></td>
</tr>
<tr>
<td><b>controls-transport-of</b></td> 
<td>First protein controls a reaction that changes the cellular location of the second protein.</td>
<td><img src='/resources/img/sif/controls-transport-of-biopax.svg'/></td>
<td><img src='/resources/img/sif/B-to-A.svg'/></td>
</tr>
<tr>
<td><b>controls-phosphorylation-of</b></td> 
<td>First protein controls a reaction that changes the phosphorylation status of the second protein.</td>
<td><img src='/resources/img/sif/controls-phosphorylation-of-biopax.svg'/></td>
<td><img src='/resources/img/sif/B-to-A.svg'/></td>
</tr>
<tr>
<td><b>controls-expression-of</b></td> 
<td>First protein controls a conversion or a template reaction that changes expression of the second protein.</td>
<td><img src='/resources/img/sif/controls-expression-of-biopax.svg'/></td>
<td><img src='/resources/img/sif/B-to-A.svg'/></td>
</tr>
<tr>
<td><b>catalysis-precedes</b></td>
<td>First protein controls a reaction whose output molecule is input to another reaction controled by the second protein.</td>
<td><img src='/resources/img/sif/catalysis-precedes-biopax.svg'/></td>
<td><img src='/resources/img/sif/A-to-B.svg'/></td>
</tr>
<tr>
<td><b>in-complex-with</b></td> 
<td>Proteins are members of the same complex.</td>
<td><img src='/resources/img/sif/in-complex-with-biopax.svg'/></td>
<td><img src='/resources/img/sif/triangle.svg'/></td>
</tr>
<tr>
<td><b>interacts-with</b></td> 
<td>Proteins are participants of the same MolecularInteraction.</td>
<td><img src='/resources/img/sif/interacts-with-biopax.svg'/></td>
<td><img src='/resources/img/sif/interacts-with-sif.svg'/></td>
</tr>
<tr>
<td><b>neighbor-of</b></td> 
<td>Proteins are participants or controlers of the same interaction.</td>
<td><img src='/resources/img/sif/neighbor-of-biopax.svg'/></td>
<td><img src='/resources/img/sif/neighbor-of-sif.svg'/></td>
</tr>
<tr>
<td><b>consumption-controled-by</b></td>
<td>The small molecule is consumed by a reaction that is controled by a protein</td>
<td><img src='/resources/img/sif/metabolic1-biopax.svg'/></td>
<td><img src='/resources/img/sif/X-to-A.svg'/></td>
</tr>
<tr>
<td><b>controls-production-of</b></td>
<td>The protein controls a reaction of which the small molecule is an output.</td>
<td><img src='/resources/img/sif/metabolic1-biopax.svg'/></td>
<td><img src='/resources/img/sif/controls-production-of-sif.svg'/></td>
</tr>
<tr>
<td><b>controls-transport-of-chemical</b></td>
<td>The protein controls a reaction that changes cellular location of the small molecule.</td>
<td><img src='/resources/img/sif/controls-transport-of-chemical-biopax.svg'/></td>
<td><img src='/resources/img/sif/controls-transport-of-chemical-sif.svg'/></td>
</tr>
<tr>
<td><b>chemical-affects</b></td>
<td>A small molecule has an effect on the protein state.</td>
<td><img src='/resources/img/sif/chemical-affects-biopax.svg'/></td>
<td><img src='/resources/img/sif/X-to-A.svg'/></td>
</tr>
<tr>
<td><b>reacts-with</b></td>
<td>Small molecules are input to a biochemical reaction.</td>
<td><img src='/resources/img/sif/metabolic2-biopax.svg'/></td>
<td><img src='/resources/img/sif/reacts-with-sif.svg'/></td>
</tr>
<tr>
<td><b>used-to-produce</b></td>
<td>A reaction consumes a small molecule to produce another small molecule.</td>
<td><img src='/resources/img/sif/metabolic2-biopax.svg'/></td>
<td><img src='/resources/img/sif/used-to-produce-sif.svg'/></td>
</tr>
</tbody>
</table>
<p/>
<table class="table"><tr><td>Legend:</td><td><img src='/resources/img/sif/legend.svg'/></td></tr></table>

<h3>SBGN</h3>
<p>
The Systems Biology Graphical Notation (<a target="_blank" href="http://www.sbgn.org">SBGN</a>) 
is a standard visual notation for network diagrams in biology. 
SBGN markup language (SBGN-ML) is an associated standard XML format that can be loaded into available 
software to visualize a diagram of a pathway. BioPAX can be converted to SBGN-ML format, following the 
process diagram paradigm, one of three paradigms (activity flow, process and entity relationship) available in SBGN.
</p>

<jsp:include page="footer.jsp" />
</body>
</html>
