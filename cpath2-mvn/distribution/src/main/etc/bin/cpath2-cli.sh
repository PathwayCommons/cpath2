#!/bin/sh

##
# cPath2 ${version} admin/service console app.
#
# Please set the CPATH2_HOME environment variable first.
#
##

CPATH2_OPTS="-Dfile.encoding=UTF-8 -Xss65536k -Xmx6g -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Dnet.sf.ehcache.skipUpdateCheck=true"

CPATH2_PROFILING_OPTS="-agentpath:/data/local/jprofiler6/bin/linux-x64/libjprofilerti.so=port=48000,wait"

$JAVA_HOME/bin/java $CPATH2_OPTS -jar cpath2-cli.jar "$1" "$2" "$3" "$4" "$5"
