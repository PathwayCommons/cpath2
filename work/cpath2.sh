#!/bin/sh
##
# cPath2 run script.
##

CPATH2_OPTS="-Dspring.profiles.active=prod -Dfile.encoding=UTF-8 -Xmx96g -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider"
CPATH2_TUNING_OPTS="-Xms48g -Xmn1g -Xss65536k -XX:SurvivorRatio=16 -Xincgc -Xnoclassgc -XX:CMSTriggerRatio=50 -XX:ParallelGCThreads=2 -XX:NewRatio=5"
CPATH2_DEBUG_OPTS="-Dlogback.configurationFile=logback.xml -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=12345"

$JAVA_HOME/bin/java $CPATH2_OPTS -jar ../cpath-cli/target/cpath2.jar "$1" "$2" "$3" "$4" "$5"
#$JAVA_HOME/bin/java  -server $CPATH2_OPTS -jar ../cpath-ws/target/cpath2.war --server.port=8280

## TEST and DEVELOP...
#cd ../cpath-ws
#mvn spring-boot:run -Drun.jvmArguments="-Dserver.port=8080 -Dfile.encoding=UTF-8 -Xss65536k -Xmx64g -DCPATH2_HOME=/home/igor/pc9 -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider -Dspring.profiles.active=prod"
