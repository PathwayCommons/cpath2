#!/bin/bash

export CPATH2_HOME="."

JDK_JAVA_OPTIONS="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED"
DEBUG_OPTS=""
#DEBUG_OPTS="-Dlogback.configurationFile=logback.xml -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=12345"
BASE_OPTS="-Dlogback.configurationFile=logback.xml -Dfile.encoding=UTF-8 -Xss32m -Xmx60g -Dpaxtools.normalizer.use-latest-registry=true -Dpaxtools.core.use-latest-genenames=true"
#use List instead Map collections for read-only BioPAX model if we start the Web app but not if building the instance/data
EXTRA_OPTS="$BASE_OPTS -Dpaxtools.model.safeset=list -server"

for arg in "$@"; do
  if [ "$arg" = "--build" ] || [ "$arg" = "-b" ] ; then
    echo "Enabled load time weaving (LTW)"
    EXTRA_OPTS="$BASE_OPTS -Dpaxtools.model.safeset=map -javaagent:../target/spring-instrument.jar"
    break
  fi
done

$JAVA_HOME/bin/java $DEBUG_OPTS $EXTRA_OPTS -jar ../target/cpath2.war "$@"
