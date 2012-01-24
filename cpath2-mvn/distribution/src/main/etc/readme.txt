Pathway Commons software
cPath2 (cPathSquared) Admin How To


GENERAL INFORMATION


Before running cPath2 from console or deploying the WAR on a Tomcat,
SET the system environment variable 'CPATH2_HOME' - to be a directory that 
contains all the cpath2 configuration files, such as: 
- cpath.properties (set db names, connection url prefix, username, password there); 
- hibernate.properties;
- log4j.properties;
- 'tmp' dir (it is where downloaded data and ontology files are stored, 
and cache directories and files get created!);
- obo.properties;
- validation.properties;
- ehcache-hibernate.xml (the 2nd-level cache configuration);
- ehcache-service.xml (the mid-tier, service layer, cache conf.);
- blacklist.txt (optional; BioPAX graph query performance tuning., usually - small 
molecules to ignore, like ubiquitous ATP...)
- security-config.xml (webapp security);
- webdoc.properties (for the cpath2 demo webapp to find a cpath2 webservice).

THIS (cpath2 distribution) directory CAN be used as cpath2 home as well.
One can switch between different cpath2 home directories (and cpath2 databases)
by re-defining the CPATH2_HOME variable (and this is the recommended method); 
one can also edit cpath.properties file to specify another DB login/password,
or cpath2 databases. For a new database, the corresponding Lucene 
index directories will be created automatically (using database names); 
for existing cpath2 databases, the Lucene index directory is expected 
to be found in CPATH2_HOME; otherwise, full-text search won't work properly. 

Using the cPath2 shell script is more convenient than using the corresponding 
jar directly, otherwise, make sure to set JAVA_HOME and additional options: 
1. ALWAYS add CPATH2_HOME JVM environment variable: -DCPATH2_HOME=$CPATH2_HOME
(provided that the system environment variable $CPATH2_HOME has been already set);
2. using the mysql client:
mysql> SET GLOBAL max_allowed_packet = 256000000;
(alternatively, set this in the mysql conf. file)
3. (in production) increase ulimits, e.g.: 
'ulimit -s unlimited' (Linux), or 'launchctl limit stack unlimited' (MacOSX)
4. consider setting 'java.io.tmpdir' as: -Djava.io.tmpdir=$CPATH2_HOME/tmp
(1..4 - all set for you in the shell scripts)

Try the cpath-admin.sh (without arguments) to see what's there available.


DATA IMPORTING

Configure the cPath2 metadata 
(e.g., see metadata.conf example in the cpath2 distribution dir).

The cPath2 metadata file is a plain text table, which can be placed anywhere
where accessible by the cpath2 admin script via URL. It has the following structure:
- one datasource per line (thus, EOLs/newline matter);
- columns are separated simply by "<br>" element (which does not make it HTML though);
- no column headers, but columns are, in order, the following: 
1) IDENTIFIER - unique, short, and simple; spaces or dashes are not allowed;
2) NAME - for pathway type records (BIOPAX, PSI_MI), must be official MIRIAM 
datasource name or synonym;
3) VERSION - text or number; better keep it simple, because it becomes 
essential part of local/downloaded data file or archive;
4) DATE - a description text field, release date (any date format);
5) URL to the data - can be file://, http://, ftp://, classpath:;
- when extension (EXT) is not '.zip' or '.gz' - means data to be saved as 
  $CPATH2_HOME/tmp/IDENTIFIER.VERSION.EXT and processes/parsed as is 
  (e.g, as BioPAX RDF+XML data);
- when no file extension available - means raw data to be saved as 
  $CPATH2_HOME/tmp/IDENTIFIER.VERSION (same as above);
- can be empty or fake URL (e.g., just "foo.zip"), which is OK, as cpath2 will
  look first for IDENTIFIER.VERSION (foo.zip -> IDENTIFIER.VERSION.zip) file 
  in $CPATH2_HOME/tmp!
  
NOTE: 
Anyway, what will actually happen to the data content also depends on the 
metadata TYPE (see the description of this column below), file extension, and
corresponding cleaner/converter implementation. For "pathway data" type (BIOPAX,
PSI_MI), cPath2 Fetcher expects either .zip (multiple file/dir entries are
allowed and processed separately!) or .gz (single data entry only!), other extensions
or none (means not archived data in the expected format). For "warehouse data"
type (PROTEIN, SMALL_MOLECULE, MAPPING), it expects: .zip (multiple file/dir
entries are merged and processed altogether!), .gz (single data entry only!),
other extensions or no extension (means not archived data).
Buy the way, one (a cpath2 data manager) may also proactively (e.g., using wget app)
download and save the desired data somewhere or even - directly to $CPATH2_HOME/tmp/
(following the above "IDENTIFIER.VERSION.EXT" naming convention, which is a tweak, 
however, and the proper way to run cpath2 importer off-line, well, almost off-line, 
would be by specifying local URLs like file:///full/path/whatever.gz instead of 
using empty URL and naming trick). This sometimes helps tackle various connection/firewall/data
issues, such as when FTP fails from java code, or - current cPath2 version cannot read
data from the remote location (e.g., it's a zip stream or "tarball" containing
multiple files and directories instead of one, etc.). In other words, so, when
one then runs the cPath2 --fetch-data command, and it finds everything it needs
under $CPATH2_HOME/tmp/, it skips downloading step entirely to quickly proceed
with converting/saving!

6) URL to the logo image - can be pointing to an image resource or empty;
7) TYPE - one of: BIOPAX, PSI_MI, PROTEIN, SMALL_MOLECULE, MAPPING;
8) CLEANER_CLASS - - use cpath.converter.internal.BaseCleanerImpl here if a 
more specific cleaner is not required/available (like cpath.cleaner.internal.UniProtCleanerImpl);
9) CONVERTER_CLASS - use cpath.converter.internal.BaseConverterImpl here if a 
more specific converter is not required/available (like converter.internal.UniprotConverterImpl);


Prepare MySQL Databases.

If required, generate (all or some of) the cpath2 database schemas using
the same db names as specified in the $CPATH2_HOME/cpath2.properties file:

sh cpath-admin.sh -create-tables cpathmain,cpathproteins,cpathmolecules,cpathmetadata
(WARNING: this destroys and re-creates the databases, if existed!)

Admin Actions (run cpath-admin.sh w/o arguments to see the hint).

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

# create full-text index (currently, it's required for the "main" cpath2 db only, only for the webservice app)
sh cpath-admin.sh -create-index main


