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

CPATH2_OPTS="-Dfile.encoding=UTF-8 -Xss65536k -Xmx8g -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -Dnet.sf.ehcache.skipUpdateCheck=true"

CPATH2_TUNING_OPTS="-Xms2g -Xmn1g -XX:PermSize=128m -XX:MaxPermSize=256m -XX:SurvivorRatio=16 -Xincgc -Xnoclassgc -XX:CMSTriggerRatio=50 -XX:ParallelGCThreads=2 -XX:NewRatio=5"

CPATH2_PROFILING_OPTS="-agentpath:/data/local/jprofiler6/bin/linux-x64/libjprofilerti.so=port=48000,wait"

if [ -z "$1" ]; then 
	$JAVA_HOME/bin/java -jar cpath2-server.jar --help
else
	$JAVA_HOME/bin/java  $CPATH2_TUNING_OPTS $CPATH2_OPTS -jar cpath2-server.jar $1 $2 $3 $4 $5 $6
fi
