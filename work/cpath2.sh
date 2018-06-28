#!/bin/sh

##
# Running cPath2 either as web services or admin console:
##

CPATH2_OPTS="-Dfile.encoding=UTF-8 -Xss32m -Xms16g -Xmx80g -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider"

#CPATH2_DEBUG_OPTS="-Dlogback.configurationFile=logback.xml -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=12345"
#CPATH2_TUNING_OPTS="-Xmn1g -Xss64m -XX:SurvivorRatio=16 -Xincgc -Xnoclassgc -XX:CMSTriggerRatio=50 -XX:ParallelGCThreads=2 -XX:NewRatio=5"

CPATH2_HOME="."
$JAVA_HOME/bin/java $CPATH2_OPTS -jar ../target/cpath2.war "$1" "$2" "$3" "$4" "$5"

