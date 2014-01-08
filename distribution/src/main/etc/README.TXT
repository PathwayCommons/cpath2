Pathway Commons software
cPath2 (cPathSquared) version ${version}

How To Create a new Instance


GENERAL INFORMATION

Note: since v4, cpath2 uses H2 database (file) instead of MySQL.

  Before running cPath2 from console or deploying the WAR on a Tomcat,
SET the System Environment variable 'CPATH2_HOME' - to be a directory that 
contains cpath2 configuration files, such as: 
- cpath2.properties (set db names, connection url prefix, username, password there); 
- hibernate.properties; these can be overwritten by the corresponding properties in the Spring (XML) configuration.
- logback.xml (optional; alternative logging can be enabled by -Dlogback.configurationFile=$CPATH2_HOME/logback.xml JVM option);
- 'data' dir (it is where downloaded original data files are stored);
- 'tmp' dir (it is where downloaded data and ontology files are stored, 
- 'cache' dir (it is where cache directories and files get created);
- validation.properties (BioPAX Validator rules tuning);
- blacklist.txt (optional; BioPAX graph query performance tuning., usually - small 
  molecules to ignore, like ubiquitous ATP...)

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

  cPath2 was planned to automatically download and process data from any URL,
but this not always works due to such issues as: "ftp://.." URL access fails 
from java on some servers with strict policy; problems with data archive structure;
cpath2 cannot read multiple files from gzip archives; etc.

  The PREFERRED method is as follows
(when creating a new cpath2 instance DBs from scratch): 

1. Edit the cPath2 metadata.conf (see "metadata format" below)
Note: if possible, use a standard name in the 'NAME' (the second) field of pathway data entries (BIOPAX and PSI_MI type). 

2. Download warehouse data, ('wget') UniProt and ChEBI, into $CPATH2_HOME/data/:
- wget ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/taxonomic_divisions/uniprot_sprot_human.dat.gz
  and rename it to, e.g., UNIPROT_HUMAN.gz (name is to be consistent with 
  the corresponding record in the metadata.conf, i.e. IDENTIFIER.EXT; 
  .gz (single file), .zip (multi- or single-file) archives are currently supported, 
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
or PSI-MI data $CPATH2_HOME/tmp/ as follows:
- download (wget) files or archive(s) from the pathway resource 
(e.g., wget http://www.reactome.org/download/current/biopax3.zip) 
- extract what you need (e.g. some species data only)
- create a simple zip/gzip archive, name it according to the cpath2 metadata "IDENTIFIER.EXT" convention
(alternatively, use, e.g., file:///full-path-to/smth.zip URL in the metadata.conf) 

4. set "cpath2.admin.enabled=true" in the cpath properties 
(or via JVM option: "-Dcpath2.admin.enabled=true").

5. Run import-data.sh script and follow the instructions/questions...


METADATA FORMAT

The cPath2 METADATA file is a plain text table, which can be placed anywhere
where accessible by the cpath2 admin script via URL. It has the following structure:
- one data source metadata definition per line (thus, EOLs/newline matter);
- lines that begin with "#" are remarks and will be ignored; blank lines - too;
- there are exactly 9 columns, which are tab-separated values (TSV);
- empty strings in the middle of a line (the result of using \t\t) and the 
trailing empty string (after the last '\t', i.e., Converter class name, if any), 
will be always added to the columns array.
- no column headers, but columns are, in order, the following: 
1) IDENTIFIER - unique, short (40), and simple; spaces or dashes are not allowed;

2) NAME - can contain one (must) or more (optional) data source names, separated 
by semicolons, as follows: [displayName;]standardName[;name1;name2;..].
for pathway type records (BIOPAX, PSI_MI), there must be at least one standard 
data source name, if possible (names will be used for filtering by data source 
in cpath2 full-text search);

3) DESCRIPTION - free text: organization name, release date, web site, comments;

4) URL to the Data (optional) - can be file://, http://, ftp://, classpath:;
- when no 'extension' or it is not '.zip' or '.gz', - means data to be saved as 
  $CPATH2_HOME/tmp/IDENTIFIER.EXT and processed "as is" (no unpacking);
- empty or a fake URL (e.g., "foo.zip" or ".gz", to force unzip) is accepted, because 
  cpath2 checks if IDENTIFIER ("foo.zip" -> IDENTIFIER.zip) 
  file exists in $CPATH2_HOME/tmp/, and if found, ignores whatever URL is there! 
 
So, how data is actually processed depends on the metadata TYPE 
(see the description of this column below), file extension, and
corresponding cleaner/converter implementations. For "pathway data" type 
(BIOPAX, PSI_MI), cpath2 fetcher expects either .zip (multiple file/dir entries are
allowed and processed separately!) or .gz (single data entry only!), other extensions
or none (means not archived data in the expected format). For "warehouse data"
type (PROTEIN, SMALL_MOLECULE, MAPPING), it expects: .zip (multiple file/dir
entries are merged and processed altogether!), .gz (single data entry only!),
other extensions or no extension (means not archived data).

So, as described above, a cPath2 Data Manager (a person) may proactively 
download, re-package, and either save the required data to $CPATH2_HOME/tmp/
(following the cpath2 importer naming convention), or specify a local URL, 
like file:///full/path/whatever.gz, instead.

5) URL to the Data Provider's Homepage (optional, good to have)

6) IMAGE URL (optional) - can be pointing to a resource logo;

7) TYPE - one of: BIOPAX, PSI_MI, PROTEIN, SMALL_MOLECULE;

8) CLEANER_CLASS - leave empty or use a specific cleaner class name (like cpath.cleaner.internal.UniProtCleanerImpl);

9) CONVERTER_CLASS - leave empty or use a specific converter class (like cpath.converter.internal.UniprotConverterImpl);


MORE DETAILS

About how to use the admin tool directly, i.e., not using import_data.sh  
(most commands work only if maintenance mode is enabled). 

Run cpath2-cli.sh w/o arguments to see the hint.

# fetch/update Metadata:

#sh cpath2-cli.sh -fetch-metadata <metadataURL>
sh cpath2-cli.sh -fetch-metadata "file:///full-path/metadata.conf"

#parse, clean, convert, and store data in the database

sh cpath2-cli.sh -create-warehouse
#sh cpath2-cli.sh -create-warehouse <IDENTIFIER>

sh cpath2-cli.sh -update-mapping

sh cpath2-cli.sh -premerge
#sh cpath2-cli.sh -premerge <IDENTIFIER>
#WARN: do not run multiple parallel '-premerge' processes that import WAREHOUSE data)

sh cpath2-cli.sh -merge
#sh cpath2-cli.sh -merge <IDENTIFIER> --force

# create full-text index:
sh cpath2-cli.sh -create-index

# updates counts/stats for no. pathways, interactions, molecules, etc.
sh cpath2-cli.sh -update-counts

# optional
sh cpath2-cli.sh -clear-cache

  Optionally (though highly recommended), generate a 'blacklist' 
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


