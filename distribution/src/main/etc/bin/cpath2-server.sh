#!/bin/sh

##
# cPath2 ${version} web service start script.
#
# Please set the CPATH2_HOME environment variable first.
#
# Use parameters to specify HTTP port and AJP properties
# for the built-in Apache Tomcat 7 app server
# (run with --help to see the options list).
#
##

echo "CPATH2_HOME Directory: $CPATH2_HOME"

# get cpath2 properties
xmlbase=`sed '/^\#/d' $CPATH2_HOME/cpath2.properties | grep 'cpath2.xml.base'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
maindb=`sed '/^\#/d' $CPATH2_HOME/cpath2.properties | grep 'cpath2.db='  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`

echo "This cPath2 Instance Uses:"
echo "       main.db=$maindb"
echo "       xml.base=$xmlbase"

#to disable ehcache, add -Dnet.sf.ehcache.disabled=true
CPATH2_OPTS="-Dfile.encoding=UTF-8 -Xss65536k -Xmx10g -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider -Dspring.profiles.active=prod"

CPATH2_TUNING_OPTS="-Xms2g -Xmn1g -XX:PermSize=128m -XX:MaxPermSize=256m -XX:SurvivorRatio=16 -Xincgc -Xnoclassgc -XX:CMSTriggerRatio=50 -XX:ParallelGCThreads=2 -XX:NewRatio=5"

CPATH2_PROFILING_OPTS="-agentpath:/data/local/jprofiler6/bin/linux-x64/libjprofilerti.so=port=48000,wait"

JAVA_DEBUG_OPTS="-Dlogback.configurationFile=$CPATH2_HOME/logback.xml -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=12345"

if [ -z "$1" ]; then 
	$JAVA_HOME/bin/java -jar cpath2-server.jar --help
else
	$JAVA_HOME/bin/java  $CPATH2_TUNING_OPTS $CPATH2_OPTS -jar cpath2-server.jar $1 $2 $3 $4 $5 $6
fi
