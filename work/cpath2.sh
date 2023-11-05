#!/bin/bash

export CPATH2_HOME="."

JDK_JAVA_OPTIONS="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED"

OPTS="-server -Dfile.encoding=UTF-8 -Xss32m -Xmx60g -Dpaxtools.normalizer.use-latest-registry=true"
DEBUG_OPTS=""
#DEBUG_OPTS="-Dlogback.configurationFile=logback.xml -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=12345"

#BioPAX model is read-only (ok to use List instead Map for collections) when we run as Web app but not if building the instance/data
export CPATH2_OPTS="$DEBUG_OPTS $OPTS -Dpaxtools.model.safeset=list"

for arg in "$@"; do
  if [ "$arg" = "--build" ] || [ "$arg" = "-b" ] ; then
    echo "Enabled load time weaving (LTW)"
    export CPATH2_OPTS="$DEBUG_OPTS $OPTS -Dpaxtools.model.safeset=map -javaagent:../target/spring-instrument.jar"
    break
  fi
done

$JAVA_HOME/bin/java $CPATH2_OPTS -jar ../target/cpath2.war "$@"
