#!/bin/bash
## Runs cpath2 web service or cli app (except for building a new data/model)

export CPATH2_HOME="."
export JDK_JAVA_OPTIONS="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED"

DEBUG_OPTS=""
#DEBUG_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=12345"
CPATH2_OPTS="-Dlogback.configurationFile=logback.xml -Dfile.encoding=UTF-8 -Xss32m -Xmx60g -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider -Dpaxtools.normalizer.use-latest-registry=true -Dpaxtools.core.use-latest-genenames=true"

for arg in "$@"; do
  if [ "$arg" = "--build" ] || [ "$arg" = "-b" ] ; then
    echo "unsupported option - to build a new data/model please use the other script or cpath2.war directly"
    exit 1
    break
  elif [ "$arg" = "--server" ] || [ "$arg" = "-s" ] ; then
    CPATH2_OPTS="$CPATH2_OPTS -server"
    break
  fi
done

$JAVA_HOME/bin/java $DEBUG_OPTS $CPATH2_OPTS -jar ../target/cpath2.war "$@"
