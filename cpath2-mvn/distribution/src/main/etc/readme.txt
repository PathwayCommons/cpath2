Important tips.

Before running the cpath-admin.jar or deploying the WAR,
DEFINE the environment variable 'CPATH2_HOME' as a directory that 
contain at least the cpath2.properties and log4j.properties. 
Pre-merge pathway databases, all lucene indexes,
and log files will be created/updated/used in the home directory!

THIS (distribution) directory CAN be used as cpath2 home as well.
One can switch home directories (and cpath2 system databases)
by re-defining the CPATH2_HOME variable (this is preferable method); 
one can also edit cpath2.properties file, e.g., to replace a cpath2 database,
but for the new db, if existed, the corresponding lucene index directory 
must be found in this home directory (otherwise fulltext search won't work properly!)  

Before the FIRST data import, 
create all the cpath2 databases that are listed 
in the $CPATH2_HOME/cpath2.properties file
using the admin command, e.g.:
-create-tables cpathmain,cpathproteins,cpathmolecules,cpathmetadata
(you can also drop/create any of cpath2 databases at any time; be warned!)

ALWAYS and also define CPATH2_HOME JVM variable:

java -DCPATH2_HOME=$CPATH2_HOME
(where environment variable $CPATH2_HOME has been already set)


Example admin actions:

java -DCPATH2_HOME=$CPATH2_HOME -jar cpath-admin.jar -create-tables mymetadata

java -DCPATH2_HOME=$CPATH2_HOME -Xmx1024M -jar cpath-admin.jar -fetch-metadata "file://full-path/cpath2-metadata.html"

# http:// or ftp:// url can be used as well to kick off the cpath2 data import or update...
#java -DCPATH2_HOME=$CPATH2_HOME -Xmx1024M -jar cpath-admin.jar -fetch-metadata <URL-of-the-metadata-page>

java -DCPATH2_HOME=$CPATH2_HOME -Xmx2048M -jar cpath-admin.jar -fetch-pathwaydata CELLMAP

java -DCPATH2_HOME=$CPATH2_HOME -Xmx2048M -jar cpath-admin.jar -fetch-proteindata UNIPROT-HUMAN

java -DCPATH2_HOME=$CPATH2_HOME -Xmx1024M -jar cpath-admin.jar -premerge

# also possible to add new pathway providers to the "metadata" html page 
# with corresponding cleaner/converter, if required, even after the cpath-admin.jar is compiled:
#java -DCPATH2_HOME=$CPATH2_HOME -cp /path-to/DataCleaner1Impl.class;/path-to/DataCleaner2Impl.class  -Xmx1024M -jar cpath-admin.jar -premerge

java -DCPATH2_HOME=$CPATH2_HOME -Xmx2048M -jar cpath-admin.jar -merge

...