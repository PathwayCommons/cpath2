#!/bin/sh
##
# cPath2 run script.
##

CPATH2_OPTS="-Dfile.encoding=UTF-8 -Xss32m -Xms16g -Xmx99g -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider"
CPATH2_DEBUG_OPTS="-Dlogback.configurationFile=logback.xml -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=12345"
CPATH2_TUNING_OPTS="-Xmn1g -Xss64m -XX:SurvivorRatio=16 -Xincgc -Xnoclassgc -XX:CMSTriggerRatio=50 -XX:ParallelGCThreads=2 -XX:NewRatio=5"

if [ "$1" = "server" ] ; then
  # 'work' directory for the production mode
  export CPATH2_HOME="."
  $JAVA_HOME/bin/java -server $CPATH2_OPTS $CPATH2_TUNING_OPTS -jar ../cpath-ws/target/cpath2.war --spring.profiles.active=prod --server.port=8280
elif [ "$1" = "console" ] ; then
  export CPATH2_HOME="."
  $JAVA_HOME/bin/java $CPATH2_OPTS -Dspring.profiles.active=prod -jar ../cpath-cli/target/cpath2.jar "$2" "$3" "$4" "$5"
else
  #start the web app using the default (dev) profile, small test data in the temp. dir, etc.
  #(optionally, include $CPATH2_DEBUG_OPTS)
  $JAVA_HOME/bin/java -Dfile.encoding=UTF-8 -Xmx4g -jar ../cpath-ws/target/cpath2.war --server.port=8280
fi
