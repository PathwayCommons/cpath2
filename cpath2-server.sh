#!/bin/sh

##
# cPath2 web service start script.
#
# Please set the CPATH2_HOME environment variable first.
#
# Use parameters to specify HTTP port and AJP properties
# for the built-in Apache Tomcat 7 app server
# (run with --help to see the options list).
#
##

export CPATH2_HOME=/data/cpath2

echo "CPATH2_HOME Directory: $CPATH2_HOME"

# get cpath2 properties
xmlbase=`sed '/^\#/d' $CPATH2_HOME/cpath2.properties | grep 'cpath2.xml.base'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`

echo "$0 $1 $2 $3 $4 $5 xml.base=$xmlbase"

#to disable ehcache, add -Dnet.sf.ehcache.disabled=true
CPATH2_OPTS="-server -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Xss65536k -Xmx48g -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider -Dspring.profiles.active=prod"

#do not use this settings when debugging/profiling
CPATH2_TUNING_OPTS="-Xms24g -Xmn1g -XX:SurvivorRatio=16 -Xincgc -Xnoclassgc -XX:CMSTriggerRatio=50 -XX:ParallelGCThreads=2 -XX:NewRatio=5"

CPATH2_PROFILING_OPTS="-agentpath:/data/local/jprofiler6/bin/linux-x64/libjprofilerti.so=port=48000,wait"

CPATH2_DEBUG_OPTS="-Dlogback.configurationFile=$CPATH2_HOME/logback.xml -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=12345"

if [ -z "$1" ]; then 
	$JAVA_HOME/bin/java -jar cpath2-server.jar --help
else
	$JAVA_HOME/bin/java $CPATH2_OPTS $CPATH2_TUNING_OPTS -jar cpath2-server.jar $1 $2 $3 $4 $5 $6
fi
