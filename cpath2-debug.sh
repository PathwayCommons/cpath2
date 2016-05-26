#!/bin/sh

##
# cPath2 4.1.0-SNAPSHOT web service start script.
#
# Please set the CPATH2_HOME environment variable first.
#
# Use parameters to specify HTTP port and AJP properties
# for the built-in Apache Tomcat 7 app server
# (run with --help to see the options list).
#
##

export CPATH2_HOME=/Users/rodche/cpath2_home

echo "CPATH2_HOME Directory: $CPATH2_HOME"

# get cpath2 properties
xmlbase=`sed '/^\#/d' $CPATH2_HOME/cpath2.properties | grep 'cpath2.xml.base'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`

echo "xml.base=$xmlbase"

#to disable ehcache, add -Dnet.sf.ehcache.disabled=true
OPTS="-Dfile.encoding=UTF-8 -Xss65536k -Xmx32g -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider -Dspring.profiles.active=dev"

PROFILING_OPTS="-agentpath:/data/local/jprofiler6/bin/linux-x64/libjprofilerti.so=port=48000,wait"

DEBUG_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=12345"

$JAVA_HOME/bin/java $OPTS -Dlogback.configurationFile=$CPATH2_HOME/logback.xml -jar cpath2-server.jar -httpPort 8080
