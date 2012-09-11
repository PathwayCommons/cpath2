Pathway Commons software
cPath2 (cPathSquared) Admin How To


GENERAL INFORMATION


Before running cPath2 from console or deploying the WAR on a Tomcat,
SET the system environment variable 'CPATH2_HOME' - to be a directory that 
contains all the cpath2 configuration files you are planning to use, such as: 
- cpath.properties (set db names, connection url prefix, username, password there); 
- hibernate.properties;
- log4j.properties;
- 'tmp' dir (it is where downloaded data and ontology files are stored, 
and cache directories and files get created!);
- obo.properties;
- validation.properties (BioPAX Validator rules tuning);
- ehcache-hibernate.xml (the 2nd-level cache configuration);
- ehcache-service.xml (the mid-tier, service layer, cache configuration);
- blacklist.txt (optional; BioPAX graph query performance tuning., usually - small 
molecules to ignore, like ubiquitous ATP...)
- security-config.xml (webapp security, admin login);
- webdoc.properties (for the cpath2 demo webapp to find a cpath2 webservice).

The cpath2 distribution directory can be cpath2 home as well (edit files there).
One can later switch between different cpath2 home directories 
(and therefore - cpath2 databases) by re-defining the CPATH2_HOME; 
one can also edit cpath.properties file to specify different DB login/password
and cpath2 databases. For a new database, the corresponding Lucene 
index directories will be created automatically (named after corresponding databases); 
for existing cpath2 databases, the "main" Lucene index directory is expected 
to be found in CPATH2_HOME; otherwise, full-text search won't work properly. 

Using the cPath2 shell script is more convenient than executing the 
jar or class directly. We provide several shell scripts for your convenience.

If you want run java or write own script, make sure to set JAVA_HOME and other options: 

1. ALWAYS add CPATH2_HOME JVM environment variable: -DCPATH2_HOME=$CPATH2_HOME
(provided that the system environment variable $CPATH2_HOME has been already set);

2. using the mysql client:
mysql> SET GLOBAL max_allowed_packet = 256000000;
(alternatively, set this in the mysql conf. file)

3. (in production) increase ulimits, e.g.: 
'ulimit -s unlimited' (Linux), or 'launchctl limit stack unlimited' (MacOSX)

4. consider setting 'java.io.tmpdir' as: -Djava.io.tmpdir=$CPATH2_HOME/tmp
(1..4 - all set for you in the shell scripts)

5. try md5hex.uri.enabled=true and other options in cpath.properties
(e.g., all "get" and "graph" queries will, along with RDFId, also accept the Primary Key values,
i.e., - MD5hex (32-byte) digest string calculated from elements's URIs!) 


Try the cpath-admin.sh (without arguments) to see what's there available.



DATA IMPORTING

cPath2 was planned to automatically download and process data from any URL,
but this not always works due to such issues as: "ftp://.." URL access fails 
from java on some servers with strict policy; problems with data archive structure;
cpath2 cannot read multiple files from gzip archives; etc.

The PREFERRED method is as follows
(when creating a new cpath2 instance DBs from scratch): 

1. Edit the cPath2 metadata.conf (see "metadata format" below)
Note: if possible, use a standard name in the 'NAME' (the second) field of pathway data entries (BIOPAX and PSI_MI type). 

2. Download warehouse data, ('wget') UniProt and ChEBI, into $CPATH2_HOME/tmp/:
- wget ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/taxonomic_divisions/uniprot_sprot_human.dat.gz
and rename it to, e.g., UNIPROT_HUMAN.YYYYMMDD.gz (name is to be constructed from 
the corresponding line in metadata.conf, i.e. IDENTIFIER.VERSION.EXT; 
.gz (single file), .zip (multi- or single-file) archives are currently supported, 
and no extension or other one means the data is not compressed, process as is)
- wget ftp://ftp.ebi.ac.uk/pub/databases/chebi/SDF/ChEBI_complete.sdf.gz, etc. 

Note: One day, you may want to import other data sources into cpath2 'warehouse', then 
there must be corresponding cpath.converter.Converter implementation (which 
is not required to be implemented in the cpath2 sources; no need to recompile 
cpath2 either; - just add your class/jar to cpath2 script's classpath).

3. Download and prepare pathway data (note: L1, L2 will be auto-upgraded to L3) 
or PSI-MI data $CPATH2_HOME/tmp/ as follows:
- download (wget) files or archive(s) from the pathway resource 
(e.g., wget http://www.reactome.org/download/current/biopax3.zip) 
- extract what you need (e.g. some species data only)
- create a simple zip/gzip archive, name it according to IDENTIFIER.VERSION.EXT schema
(alternatively, use, e.g., file:///full-path-to/smth.zip URL in the metadata.conf) 

4. Run import-data.sh script and follow the instructions/questions...


METADATA FORMAT

The cPath2 METADATA file is a plain text table, which can be placed anywhere
where accessible by the cpath2 admin script via URL. It has the following structure:
- one data source metadata definition per line (thus, EOLs/newline matter);
- lines that begin with "#" are remarks and will be ignored; blank lines - too;
- there are exactly nine columns, which are separated by "<br>" (is not html tag!);
- empty strings in the middle of a line (the result of using <br><br>) and the 
trailing empty string (after the last '<br>', i.e., Converter class name, if any), 
will be always added to the columns array.
- no column headers, but columns are, in order, the following: 
1) IDENTIFIER - unique, short (40), and simple; spaces or dashes are not allowed;

2) NAME - can contain one (must) or more (optional) data source names, separated 
by semicolons, as follows: [displayName;]standardName[;name1;name2;..].
for pathway type records (BIOPAX, PSI_MI), there must be at least one standard 
data source name, if possible (names will be used for filtering by data source 
in cpath2 full-text search);

3) VERSION - text or number; better keep it simple, because it becomes 
essential part of the corresponding local file (in $CPATH2_HOME/tmp/);

4) DESCRIPTION - free text: organization name, release date, web site, comments;

5) URL to the data (optional) - can be file://, http://, ftp://, classpath:;
- when no 'extension' or it is not '.zip' or '.gz', - means data to be saved as 
  $CPATH2_HOME/tmp/IDENTIFIER.VERSION.EXT and processed "as is" (no unpacking);
- empty or a fake URL (e.g., "foo.zip" or ".gz", to force unzip) is accepted, because 
  cpath2 checks if IDENTIFIER.VERSION ("foo.zip" -> IDENTIFIER.VERSION.zip) 
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

6) IMAGE URL (optional) - can be pointing to a resource logo;

7) TYPE - one of: BIOPAX, PSI_MI, PROTEIN, SMALL_MOLECULE, MAPPING;

Note: MAPPING (id-mapping data) can be fetched and stored locally, but 
is not actually used anywhere (cpath2 v3.x). We planned to eventually 
use BridgeDB. However, cpath2, in fact, does alternative "id-mapping", - during 
"premerge" (in normalization) and "merge" stages (matching/replacing original 
small molecule and protein references with ones found in the cpath2 data 
warehouse, either by URI or by unification xrefs). This is possible, because
UniProt and ChEBI data contain cross-references to other databases, which cpath2
used to generate BioPAX xrefs and then search them. 

8) CLEANER_CLASS - leave empty or use a specific cleaner class name (like cpath.cleaner.internal.UniProtCleanerImpl);

9) CONVERTER_CLASS - leave empty or use a specific converter class (like cpath.converter.internal.UniprotConverterImpl);



MORE DETAILS

One can also use cpath-admin.sh or import-data.sh scripts to execute 
data import commands (as above), export some data or generate reports (e.g., validation).


(run cpath-admin.sh w/o arguments to see the hint).

Prepare MySQL Databases. If required, generate (all or some of) the cpath2 database schemas using
the same db names as specified in the $CPATH2_HOME/cpath.properties file:

sh cpath-admin.sh -create-tables cpathmain,cpathproteins,cpathmolecules,cpathmetadata
(WARNING: this destroys and re-creates the databases, if existed!)

1. Fetch/Update Instance Metadata:
#sh cpath-admin.sh -fetch-metadata <metadataURL>
sh cpath-admin.sh -fetch-metadata "file:///full-path/metadata.conf"

2. Fetch Data (download, parse, clean, convert, and store data locally)
sh cpath-admin.sh -fetch-data --all
- gets all warehouse and pathway data sequentially; one can also import, e.g., CHEBI only: 
sh cpath-admin.sh -fetch-data CHEBI
(WARNING: do not run multiple parallel '-fetch-data' processes that import the same TYPE of data)

sh cpath-admin.sh -premerge
#sh cpath-admin.sh -premerge <IDENTIFIER>

# also possible to configure (in a "metadata.conf") and use an external cleaner/converter, i.e., after the cpath-admin.jar was compiled:
#$JAVA_HOME/bin/java -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -cp /path-to/MyDataCleanerImpl.class;/path-to/MyDataConverterImpl.class -Xss65536k -Xmx2g -jar cpath-admin.jar -premerge

sh cpath-admin.sh -merge
#sh cpath-admin.sh -merge <IDENTIFIER>

# create full-text index (currently, it's required for the "main" cpath2 db only, only for the webservice app)
sh cpath-admin.sh -create-index main

Optionally, you can also create a 'blacklist' (i.e. ignore list) for the graph queries. The graph queries
will simply not traverse through the entities in this list and the entity references in the list will be eliminated
from the SIF exports. The blacklist is generated solely based on the number of degrees of an entity
(number of interactions and complexes an entity (grouped by entity reference) participates). A high-degree entity causes
unnecessary traversing during the graph queries -- hence not wanted. However, the following command will keep
the entities that control more than a threshold number of reactions out of the blacklist since this type of entities
are often biologically relevant -- for more, see the blacklist.* options in the cpath.properties:

sh cpath-admin.sh -create-blacklist <main_database_name> $CPATH2_HOME/blacklist.txt



DB DUMPS

To backup, use mysqldump:

mysqldump --max_allowed_packet=256M -u <user> -p cpath2_meta > cpath2_meta.sql

To restore (or move to another machine), create a new database there first; then:

mysql --max_allowed_packet=256M mysql -u <user> -p <new_database> < cpath2_meta.sql


