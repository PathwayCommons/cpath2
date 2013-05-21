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

echo "This cPath2 Instance Uses:"
echo "       main.db=$maindb"
echo "       xml.base=$xmlbase"


CPATH2_OPTS="-Dfile.encoding=UTF-8 -Xss65536k -Xmx6g -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Dnet.sf.ehcache.skipUpdateCheck=true -Dbiopax.normalizer.uri.strategy=simple"

CPATH2_PROFILING_OPTS="-agentpath:/data/local/jprofiler6/bin/linux-x64/libjprofilerti.so=port=48000,wait"

$JAVA_HOME/bin/java $CPATH2_OPTS -jar cpath2-cli.jar "$1" "$2" "$3" "$4" "$5"
