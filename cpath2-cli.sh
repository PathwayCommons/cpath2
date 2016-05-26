#!/bin/sh

##
# cPath2 admin/service console app.
#
# Please set the CPATH2_HOME environment variable first.
#
##

export CPATH2_HOME=/data/cpath2
echo "CPATH2_HOME Directory: $CPATH2_HOME"

# get cpath2 properties
xmlbase=`sed '/^\#/d' $CPATH2_HOME/cpath2.properties | grep 'cpath2.xml.base'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`

echo "$0 $1 $2 $3 $4 $5 using: xml.base=$xmlbase"


CPATH2_OPTS="-Djava.awt.headless=true -Dfile.encoding=UTF-8 -Xss65536k -Xmx32g -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider -Dspring.profiles.active=prod"
#-Dcpath.idsSummary.verbose

CPATH2_PROFILING_OPTS="-agentpath:/data/local/jprofiler6/bin/linux-x64/libjprofilerti.so=port=48000,wait"

JAVA_DEBUG_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=12345"

$JAVA_HOME/bin/java $CPATH2_OPTS -jar cpath2-cli.jar "$1" "$2" "$3" "$4" "$5"
