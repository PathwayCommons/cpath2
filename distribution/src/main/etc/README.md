Pathway Commons software
cPath2 (cPathSquared) version ${version}

# How To Create a new Instance

## General information

Note: since cpath2 v4, it used H2 database (file, embedded mode) instead of MySQL;
after v5 and v6, cpath2 does not persist the BioPAX model in the H2 database, 
and does not use Hibernate Search (but uses Lucene API directly); so H2/JPA is used
only for the metadata, id-mapping and log statistics.

  Before running cPath2 from console or deploying the WAR on a Tomcat,
SET the System Environment variable 'CPATH2_HOME' - a directory for 
the cpath2 configuration and data files: 
- cpath2.properties (to configure modes, physical location, admin credentials, instance name, version, description, etc.);
- data/ directory (where original and intermediate data are stored);
- tmp/ (where ontology, temporary and some test files are stored); 
- index/ (Lucene index directory for the BioPAX model);
- downloads/ (where blacklist.txt and batch data download archives are created);
- cache/ (where cache directories and files get created, if cache is enabled);
- validation.properties (BioPAX Validator rules tuning, usually defines a less strict profile than the Validator's default);
- hibernate.properties (can be overwritten by the corresponding properties in the Spring configuration);
- logback.xml (optional; alternative logging can be enabled by -Dlogback.configurationFile=$CPATH2_HOME/logback.xml JVM option);

Run the cpath2-cli.sh script, where specific system and JVM options are defined, 
to execute a series of commands to create a new cpath2 instance (import metadata, 
clean/convert/normalize input data, build the warehouse, build the main BioPAX model, Lucene index, and downloads).
Try the sh cpath2-cli.sh (without arguments) to see what's available.

Once a new instance is configured and created, one can also run the service using cpath2-server.sh script that kicks the executable cpath2-server.jar, instead of deploying the cpath2.war on a Tomcat, e.g., as follows:

    nohup sh cpath2-server.sh -httpPort 8080 -ajpPort 8009 2>&1 &

(And then check the nohup.out and cpath2 logs periodically; port numbers are just an example.)

## Preparing and importing data
(To do from console until we provide full-featured cPath2 WebAdmin app.) 

cPath2 was originally supposed to automatically download and process data from any URL.
Unfortunately, this did not work well for multiple reasons, such as:
FTP URL fails when accessed from the java program on some servers with a strict policy; 
problems with original data archives; or if we do not want to use all files in some archive
or want to collect files from different locations and have to re-pack anyway; etc. 
So, one (data manager) usually has to find, download,
re-pack as ZIP and place original data into $CPATH2_HOME/data/ folder.

### Configure Metadata

A cPath2 metadata file is a plain text file (the default one is $CPATH2_HOME/metadata.conf) 
that has the following format:
 - one data source definition per line;
 - blank lines and lines that begin with "#" are ignored (remarks);
 - there are exactly 11 columns; values are separated with tab (so each line has exactly 10 '\t' chars; 
   empty values (\t\t) are sometimes ok, e.g., when there is no Converter/Cleaner class.
 
The metadata columns are, in order: 
 1. IDENTIFIER - unique, short (40), and simple; spaces or dashes are not allowed;
 2. NAME - can contain one (must) or multiple provider names, separated 
 by semicolon, i.e.: [displayName;]standardName[;name1;name2;..];
 BIOPAX, PSI_MI, PSI_MITAB metadata entries should have at least the standard 
 official name, if possible (it is used for filtering in search/graph queries and by the data exporter);
 3. DESCRIPTION - free text: organization name, release date, web site, comments;
 4. URL to the Data - can be any URI (file://, http://, ftp://, classpath:). 
 It's just a memo, because original data usually needs re-packing.
 The cpath2 data fetcher looks for the $CPATH2_HOME/data/IDENTIFIER.zip 
 input files (multi-entry zip archives are ok). How the data are then processed depends 
 on its TYPE (see below) and cleaner/converter implementations (if specified).
 So, as described above, a cPath2 Data Manager (a person) should  
 download, re-package, and save the required archives to $CPATH2_HOME/data/
 (following the IDENTIFIER.zip naming convention) in advance.
 5. URL to the Data Provider's Homepage (optional, good to have)
 6. IMAGE URL (optional) - can be pointing to a resource logo;
 7. TYPE - one of: BIOPAX, PSI_MI, PSI_MITAB, WAREHOUSE, MAPPING;
 8. CLEANER_CLASS - leave empty or use a specific cleaner class name (like cpath.cleaner.internal.UniProtCleanerImpl);
 9. CONVERTER_CLASS - leave empty or use a specific converter class, 
 e.g., cpath.converter.internal.UniprotConverterImpl, cpath.converter.internal.PsimiConverterImpl;
 10. PUBMED ID - PubMed record ID (only number) of the main publication
 11. AVAILABILITY - values: 'free', 'academic', 'purchase'

A Converter or Cleaner implementation is not required to be implemented in the main cpath2 project sources. 
It's also possible to configure (metadata.conf) and plug into --premerge stage external 
cleaner/converter classes after the cpath2-cli.jar is released:
simply include to the cpath2-cli.sh Java options like: "-cp /path-to/MyDataCleanerImpl.class;/path-to/MyDataConverterImpl.class" 

### Prepare warehouse data

Download UniProt, ChEBI, id-mapping tables into $CPATH2_HOME/data/, e.g.:
 - `wget ftp://ftp.uniprot.org/...`
 - `wget ftp://ftp.ebi.ac.uk/pub/databases/chebi/ontology/...`
 - etc...

Re-pack/rename the data, i.e., make uniprot_human.zip, chebi.zip, etc.  
Filenames must be the corresponding metadata identifier, as in metadata.conf; 
only ZIP archive (can contain multiple files) is currently supported by cPath2 data importer 
(not .zip or no extension means the data will be read as is).

### Prepare pathway data 
(note: BioPAX L1, L2 will be auto-converted to BioPAX L3) 

Download and arrange the original BioPAX and PSI-MI/PSI-MITAB data in $CPATH2_HOME/data/ as follows:
 - download (wget) original files or archives from the pathway resource (e.g., `wget http://www.reactome.org/download/current/...`) 
 - extract what you need (e.g. some species data only)
 - create a new zip archive using name like "IDENTIFIER.zip" (datasource identifier, e.g., reactome_human.zip).

### Run cpath2-cli.sh script 
(See the available commands and the order below; 
long-going commands, especially premerge, create-downloads, 
are beter start with nohup in a separate process.)

Set "cpath2.admin.enabled=true" in the cpath properties (or via JVM option: "-Dcpath2.admin.enabled=true").
Run cpath2-cli.sh without arguments to see about the commands and parameters.

The following sequence of cpath2 commands is normally required 
to create a new cPAth2 instance from scratch: 
 - -fetch-metadata (sh cpath2-cli.sh -fetch-metadata)
 - -premerge 
 - -create-warehouse
 - -merge
 - -index
 - -export

Extras/other steps (optional):
 - -run-analysis (to execute a class that implements cpath.dao.Analysis interface, 
  e.g., to post-fix something in the merged biopax model/db or to produce some output; 
  if it does modify the model though, i.e. not a read-only analysis, 
  you are to run -dbindex and following steps again.)
 - -export (if parameters are provided, gets a sub-model, using absolute URIs, e.g.,
  to upload to a Virtuoso SPARQL server)
 - -log --import (or --export)

Highly recommended is to generate the 'blacklist' (soon after the -merge stage)
The graph queries and format converter algorithms will not
traverse through the entities in this list and the entity references in the
list will be eliminated from the SIF exports. The blacklist is generated 
solely based on the number of degrees of an entity (number of interactions 
and complexes an entity (grouped by entity reference) participates). 
A high-degree entity causes unnecessary traversing during the graph queries 
-- hence not wanted. However, the following command will keep the entities 
that control more than a threshold number of reactions out of the blacklist 
since this type of entities are often biologically relevant -- for more, see 
the blacklist.* options in the cpath2.properties:

## Backup
To backup, simply archive the configuration and data files, index directory, and cpath2.h2.db files.
To backup/move the important Web Service Access Log DB, use: -export-log / -import-log commands.

## Contact
Feel free to contact the developers, or email pc-info AT pathwaycommons.org if you need help.
