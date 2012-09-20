Pathway Commons Format Parameter
-----------------------------

The GET and GRAPH commands from the Pathway Commons Web Service
Application Programming Interface (API), return output in a variety of
formats.  Below is a description of each output format available.

If you have any questions, please contact us at:

pc-info@pathwaycommons.org

BioPAX OWL (RDF/XML)
--------------------

BioPAX is the default output format of Pathway Commons and offers
complete access to all the details that can be stored in the system.  This
format is ideal for users wishing to to access specific data not
available in other formats.  Since BioPAX is defined using the standard
OWL XML language, this format can be used with RDF / OWL tools such as
reasoners or triplestores.  All pathways and interactions within
Pathway Commons are available in BioPAX Level 3.  Due to the richness 
of representation in BioPAX, reading and using such a large BioPAX
document requires knowledge of the format and software development
tools available for processing it, such as Paxtools, a Java library
for working with BioPAX (http://www.biopax.org/paxtools.php).

Gene Set Enrichment Formats (GSEA - MSigDB GMT)
-----------------------------------------------

Over-representation analysis (ORA) is frequently used to assess the
statistical enrichment of known gene sets (e.g. pathways) in a
discrete or ranked list of genes.  This type of analysis is useful for
summarizing large gene lists and is commonly applied to
genomics data sets.  One popular software for analyzing ranked gene
lists is Gene Set Enrichment Analysis (GSEA).  The Gene sets used by
GSEA are stored for convenience in the Molecular Signature Database
(MSigDB) in the Gene Matrix Transposed file format (*.gmt).  This is
the main tab-delimited file format specified by the Broad Molecular
Signature Database (http://www.broad.mit.edu/gsea/msigdb/).

In this output format, all participants in the pathway are specified with
a UniProt Accession.  All participants for a pathway must come from
the same species as the pathway.  Therefore some participants from
cross-species pathways are removed.  Exporting to the MSigDB format
will enable computational biologists to use pathway commons data
within gene set enrichment algorithms, such as GSEA. Available for all
pathways within Pathway Commons (only from pathway database sources,
not interaction database sources). Full data format details are
available at: Broad GSEA Wiki, 
http://www.broad.mit.edu/cancer/software/gsea/wiki/index.php/Data_formats.

Simple Interaction Format (BINARY_SIF)
-------------------------------

Many network analysis algorithms require pairwise interaction networks 
as input.  A BioPAX network often contains more complex relationships
with multiple participants, such as biochemical reactions.  To make it
easier to use all of the pathway information in Pathway Commons with
typical network analysis tools, we developed a set of rules to reduce
BioPAX interactions to pairwise relationships.  Since SIF interactions
are always binary it is not possible to fully represent all of BioPAX,
thus this translation is lossy in general.  Nonetheless, the SIF
network is useful for those applications that require pairwise
interaction input.  SIF format can be easily imported into popular
network analysis tools, like Cytoscape
(http://cytoscape.org/cgi-bin/moin.cgi/Cytoscape_User_Manual/Network_Formats).

In this output format, all participants will be specified as
MIRIAM URIs.  This format does not contain any cross-species
interactions and is available for all pathways and interactions within
Pathway Commons.

Extended Simple Interaction Format (EXTENDED_BINARY_SIF)
-----------------------------------------------

Similar to the basic SIF output format, except that this output format is
specified in two sections.  Each section starts with one row of column
headings.  Each entry is multi-column, tab-delimited.  The sections
are separated by one blank line.  The first section is BINARY_SIF as
describe above, followed by edge attributes.  Current edge attributes
include the interaction data source and PubMed ID.  The second section
contains participant MIRIAM URI followed by node attributes.  Current
node attributes include entity type, entity name, UNIFICATION_XREF
(one or more UniProt IDs in the case of a protein reference, or a
ChEBI ID in the case of a Small Molecule reference), and
RELATIONSHIP_XREF (including RefSeq, Entrez Gene, and Gene Symbol).
If an attribute cannot be determined, "NOT_SPECIFIED" will be used.
All attributes except entity name are represented as a NAME:VALUE
pair; for example PubMed:9136927.  Multiple entity names or NAME:VALUE
pairs will be separated by a semicolon ';'.  This output format is
suitable for Cytoscape - Attribute Table import and loading into
Excel.  To prevent an unsuccessful import into Cytoscape due to
missing attribute values, users should specify during import that all
columns are strings.  This format is available for all pathways and
interactions within Pathway Commons.

Availability
------------

Pathway Commons redistributes data from primary databases.  Please make
sure to cite all primary sources you use to support the curation teams
that make this data available.  All data is made available under
original license terms of the primary databases.
