#!/bin/bash

export CPATH2_HOME="."

CPATH2_OPTS="-Dfile.encoding=UTF-8 -Xss32m -Xmx60g --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED"
# CPATH2_DEBUG_OPTS="-Dlogback.configurationFile=logback.xml -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=12345"

for arg in "$@"; do
  if [ "$arg" = "--build" ] || [ "$arg" = "-b" ] ; then
    echo "enabled load time weaving"
    CPATH2_OPTS="$CPATH2_OPTS -server -javaagent:../target/spring-instrument.jar"
    break
  fi
done

$JAVA_HOME/bin/java $CPATH2_OPTS -jar ../target/cpath2.war "$@"
