Important tips.

Using the script (cpath-admin.sh) is more convenient that using the jar 
(cpath-admin.jar). When you run the jar, you have to set several 
Java options (see below). Run the cpath-admin.sh without arguments 
to see what's available.

Before running the cpath-admin.jar or deploying the WAR,
SET the system environment variable 'CPATH2_HOME' to be a directory that 
contains cpath2 configuration files, such as: cpath.properties, 
log4j.properties, and 'tmp' sub-directory. This is also - where lucene 
indexes and log files are created and used!

THIS (cpath2 distribution) directory CAN be used as cpath2 home as well.
One can switch between different cpath2 home directories (and cpath2 databases)
by re-defining the CPATH2_HOME variable (- is a preferred method); 
one can also edit cpath.properties file to specify a DB login or password,
or other cpath2 databases. For a new database, the corresponding lucene 
index directory will be created automatically (using the database name); 
for existing cpath2 databases, the lucene index directory is expected 
to be found in CPATH2_HOME; otherwise, full-text search won't work properly. 

Before the FIRST data import, 
create all the cpath2 databases that are listed 
in the $CPATH2_HOME/cpath2.properties file
using the admin command, e.g.:
-create-tables cpathmain,cpathproteins,cpathmolecules,cpathmetadata
(Warning: you can accidently destroy an existing cpath2 database!)

ALWAYS define CPATH2_HOME JVM environment variable:

java -DCPATH2_HOME=$CPATH2_HOME
(provided that the system variable $CPATH2_HOME has been already set)

ALSO - do (using a mysql client, or - via the mysql conf. file):

mysql> SET GLOBAL max_allowed_packet = 256000000;

ALSO (in production) - increase ulimits, e.g.: 

'ulimit -s unlimited' (Linux), or 'launchctl limit stack unlimited' (MacOSX)

ALSO (when several users run the cpath-admin.jar) specify 'java.io.tmpdir' dir, e.g.:

-Djava.io.tmpdir=$CPATH2_HOME/tmp


Example cpath2 admin actions:

java -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -jar cpath-admin.jar -create-tables mymetadata

java -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Xmx1024M -jar cpath-admin.jar -fetch-metadata "file://full-path/cpath2-metadata.html"

# http:// or ftp:// url can be used as well to kick off the cpath2 data import or update...
#$JAVA_HOME/bin/java -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Xmx1024M -jar cpath-admin.jar -fetch-data <URL-of-the-metadata-page>

$JAVA_HOME/bin/java -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Xmx2048M -jar cpath-admin.jar -fetch-data --all
or
$JAVA_HOME/bin/java -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Xmx2048M -jar cpath-admin.jar -fetch-data UNIPROT-HUMAN

$JAVA_HOME/bin/java -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Xmx1024M -Xss65536k -jar cpath-admin.jar -premerge

# also possible to add new pathway providers to the "metadata" html page 
# with corresponding cleaner/converter, if required, even after the cpath-admin.jar is compiled:
#$JAVA_HOME/bin/java -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -cp /path-to/DataCleaner1Impl.class;/path-to/DataCleaner2Impl.class -Xss65536k -Xmx2g -jar cpath-admin.jar -premerge

$JAVA_HOME/bin/java -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Xss65536k -Xmx2048M -jar cpath-admin.jar -merge

# create full-text index
$JAVA_HOME/bin/java -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Xss65536k -Xmx2g -jar cpath-admin.jar -create-index main

(NOTE: again, it's easier to use cpath-admin.sh script instead)

...
