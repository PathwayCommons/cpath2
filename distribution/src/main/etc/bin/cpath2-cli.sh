#!/bin/sh

##
# cPath2 ${version} admin/service console app.
#
# Please set the CPATH2_HOME environment variable first.
#
##


echo "CPATH2_HOME Directory: $CPATH2_HOME"

# get cpath2 properties
xmlbase=`sed '/^\#/d' $CPATH2_HOME/cpath2.properties | grep 'cpath2.xml.base'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
maindb=`sed '/^\#/d' $CPATH2_HOME/cpath2.properties | grep 'cpath2.db='  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`

echo "$0 $1 $2 $3 $4 $5 using: main.db=$maindb; xml.base=$xmlbase"


CPATH2_OPTS="-Dfile.encoding=UTF-8 -Xss65536k -Xmx6g -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Dnet.sf.ehcache.skipUpdateCheck=true"

CPATH2_PROFILING_OPTS="-agentpath:/data/local/jprofiler6/bin/linux-x64/libjprofilerti.so=port=48000,wait"

JAVA_DEBUG_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=12345"

$JAVA_HOME/bin/java $CPATH2_OPTS -jar cpath2-cli.jar "$1" "$2" "$3" "$4" "$5"
