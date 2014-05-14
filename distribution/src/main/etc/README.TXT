Pathway Commons software
cPath2 (cPathSquared) version ${version}

How To Create a new Instance


GENERAL INFORMATION

Note: since v4, cpath2 uses H2 database (file) instead of MySQL.

  Before running cPath2 from console or deploying the WAR on a Tomcat,
SET the System Environment variable 'CPATH2_HOME' - a directory that 
contains cpath2 configuration and data files, such as: 
- cpath2.properties (set db names, connection url prefix, username, password there); 
- hibernate.properties; these can be overwritten by the corresponding properties in the Spring (XML) configuration.
- logback.xml (optional; alternative logging can be enabled by -Dlogback.configurationFile=$CPATH2_HOME/logback.xml JVM option);
- data/ directory (it is where downloaded original data files are stored);
- tmp/ dir (where ontology, temporary and some test files are stored), 
- downloads/ dir (where batch download archives are created),
- cache/ dir (where cache directories and files get created);
- validation.properties (BioPAX Validator rules tuning);
- blacklist.txt (optional; BioPAX graph query performance tuning., usually - small  
  molecules to ignore, like ubiquitous ATP...);

  The cpath2 distribution contains example configuration files.
One can edit cpath2.properties file, e.g., to specify different DB connection URL. 
For each database, the corresponding Lucene index directories will be created 
automatically (named after corresponding databases); for existing cpath2 databases, 
the "main" Lucene index directory is expected to be found in CPATH2_HOME dir.; 
otherwise, full-text search won't work properly (until re-indexed).

  Using the cPath2 shell script is more convenient than executing the 
jar or class directly. We provide several shell scripts for your convenience.

  If you want run java or write own script, make sure to set JAVA_HOME and other options: 

1. ALWAYS add CPATH2_HOME JVM environment variable: -DCPATH2_HOME=$CPATH2_HOME
(provided that the system environment variable $CPATH2_HOME has been already set);

2. Review and edit if needed cpath2.properties, and other configuration files and metadata:

3. (in production) increase ulimits, e.g.: 
'ulimit -s unlimited' (Linux), or 'launchctl limit stack unlimited' (MacOSX)

4. consider setting 'java.io.tmpdir' as: -Djava.io.tmpdir=$CPATH2_HOME/tmp
(it's already set in the shell scripts)

5. Normally, use -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider JVM option 
(it's already set as default in the cpath2 shell scripts; make sure it's also enabled for the Tomcat  
where cpath2.war is to be deployed, for this improves memory usage and performance)

Try cpath2.md5hex.uri.enabled=true and other debug options in cpath2.properties
(e.g., all "get" and "graph" queries will, along with RDFId, also accept the Primary Key values,
i.e., - MD5hex (32-byte) digest string calculated from elements's URIs!) 

  Try the cpath2-cli.sh (without arguments) to see what's there available.


DATA IMPORTING from console

  cPath2 was planned to automatically download and process data from any URL.
Unfortunately, this did not work well: "ftp://.." URL access fails if called 
from java on some servers with strict policy; problems with data archive structure;
do not want all files in an original archive, etc.

  When creating a new cpath2 instance from scratch,
  the preferred data uploading method is as follows
  (TODO: until we develop cPath2 WebAdmin feature): 

1. Edit the cPath2 metadata.conf (see "metadata format" below)
Note: if possible, use a standard name in the 'NAME' (the second) field 
of pathway data entries (of BIOPAX, PSI_MI and PSI_MITAB type). 

2. Download warehouse data, ('wget') UniProt and ChEBI, into $CPATH2_HOME/data/:
- wget ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/taxonomic_divisions/uniprot_sprot_human.dat.gz
  and re-pack/rename it to e.g. UNIPROT_HUMAN.zip (name is to be consistent with 
  the corresponding record in the metadata.conf, i.e. IDENTIFIER.zip; 
  (multi- or single-file) only zip archives are supported, 
  and no extension or other one means the data is not compressed, process as is)
- wget ftp://ftp.ebi.ac.uk/pub/databases/chebi/SDF/ChEBI_complete.sdf.gz, etc. 

NOTE: 
  A cpath.converter.Converter or Cleaner implementation is not required 
to be implemented in the main cpath2 project sources. It's possible 
to configure (metadata.conf) and plug into --premerge an external 
cleaner/converter classes even after the cpath2-cli.jar is compiled and shipped:
simply include to cpath2-cli.sh (admin shell script) Java options something like:
"-cp /path-to/MyDataCleanerImpl.class;/path-to/MyDataConverterImpl.class" 


3. Download and prepare pathway data (note: L1, L2 will be auto-upgraded to L3) 
or PSI-MI/PSI-MITAB data $CPATH2_HOME/tmp/ as follows:
- download (wget) files or archive(s) from the pathway resource 
(e.g., wget http://www.reactome.org/download/current/biopax3.zip) 
- extract what you need (e.g. some species data only)
- create a zip archive, name it as "IDENTIFIER.zip" 
(datasource identifier).

4. set "cpath2.admin.enabled=true" in the cpath properties 
(or via JVM option: "-Dcpath2.admin.enabled=true").

5. Run cpath2-cli.sh script... (see the available commands and their order below)


METADATA FORMAT

The cPath2 METADATA file is a plain text table, which can be placed anywhere
where accessible by the cpath2 admin script via URL. It has the following structure:
- one data source metadata definition per line (thus, EOLs/newline matter);
- lines that begin with "#" are remarks and will be ignored; blank lines - too;
- there are exactly 11 columns, which are tab-separated values (TSV);
- empty strings in the middle of a line (the result of using \t\t) and the 
trailing empty string (after the last '\t', i.e., Converter class name, if any), 
will be always added to the columns array.
- no column headers, but columns are, in order, the following: 
1) IDENTIFIER - unique, short (40), and simple; spaces or dashes are not allowed;

2) NAME - can contain one (must) or more (optional) data source names, separated 
by semicolons, as follows: [displayName;]standardName[;name1;name2;..].
for pathway type records (BIOPAX, PSI_MI, PSI_MITAB), there must be at least one standard 
data source name, if possible (names will be used for filtering by data source 
in cpath2 full-text search);

3) DESCRIPTION - free text: organization name, release date, web site, comments;

4) URL to the Data - can be any URI (file://, http://, ftp://, classpath:); it's just a memo.
The cpath2 data fetcher looks for the $CPATH2_HOME/data/IDENTIFIER.zip 
input files (multi-entry zip archives are ok). How the data are then processed depends 
on its TYPE (see below) and cleaner/converter implementations (if specified).

So, as described above, a cPath2 Data Manager (a person) should  
download, re-package, and save the required archives to $CPATH2_HOME/data/
(following the IDENTIFIER.zip naming convention) in advance.

5) URL to the Data Provider's Homepage (optional, good to have)

6) IMAGE URL (optional) - can be pointing to a resource logo;

7) TYPE - one of: BIOPAX, PSI_MI, PSI_MITAB, WAREHOUSE, MAPPING;

8) CLEANER_CLASS - leave empty or use a specific cleaner class name (like cpath.cleaner.internal.UniProtCleanerImpl);

9) CONVERTER_CLASS - leave empty or use a specific converter class, 
e.g., cpath.converter.internal.UniprotConverterImpl, cpath.converter.internal.PsimiConverterImpl;

10) PUBMED ID - PubMed record ID (only number) of the main publication

11) AVAILABILITY - values: 'free', 'academic', 'purchase'

MORE DETAILS

Run cpath2-cli.sh without arguments to see about the commands and parameters.

The following sequence of cpath2 commands is normally required to create a
new instance from scratch: 
-init (also destroys existing db)
-fetch-metadata
-premerge 
-create-warehouse
-merge --force (unfortunately, --force is usually required...)
-dbindex
-update-counts
-create-blacklist
-create-downloads
-clear-cache
-run-analysis (to execute a class that implements cpath.dao.Analysis interface, 
e.g., to post-fix something in the merged biopax model/db or to produce some output; 
if it does modify the model though, i.e. not a read-only analysis, 
you are to run -dbindex and following steps again.)


Example. 
Init and fetch:

sh cpath2-cli.sh -init
sh cpath2-cli.sh -fetch-metadata "file:///full-path/metadata.conf"

Etc., i.e., go over all the stages...

  Optionally (though highly recommended), do generate the 'blacklist' 
for the graph queries and SIF format converter. The algorithms will not
traverse through the entities in this list and the entity references in the
list will be eliminated from the SIF exports. The blacklist is generated 
solely based on the number of degrees of an entity (number of interactions 
and complexes an entity (grouped by entity reference) participates). 
A high-degree entity causes unnecessary traversing during the graph queries 
-- hence not wanted. However, the following command will keep the entities 
that control more than a threshold number of reactions out of the blacklist 
since this type of entities are often biologically relevant -- for more, see 
the blacklist.* options in the cpath2.properties:

sh cpath2-cli.sh -create-blacklist

Next (do not forget setting in cpath2.properties,  cpath2.provider.organisms=homo sapiens,mus musculus,..)
generate data archives:

sh cpath2-cli.sh -create-downloads 
(this might take many hours)


DB DUMPS

To backup, simply archive the configuration files, index directory, and *.h2.db files.

To backup/move the important Web Service Access Log DB, use: -export-log / -import-log commands.

