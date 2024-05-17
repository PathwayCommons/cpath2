#!/bin/bash
## Build a new PC model, full-text index, etc., using ./metadata.json and the datafile in the ./data dir.
# optional arg "from-stage" can be: PREMERGE, MERGE, POSTMERGE
export CPATH2_HOME="."
export JDK_JAVA_OPTIONS="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED"
CPATH2_OPTS="-Dlogback.configurationFile=logback.xml -Dfile.encoding=UTF-8 -Xss32m -Xmx64g -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider -Dpaxtools.normalizer.use-latest-registry=true -Dpaxtools.core.use-latest-genenames=true"
#run with load time weaving (LTW) agent (this is for biopax validator, during data 'premerge' phase)
$JAVA_HOME/bin/java $CPATH2_OPTS -javaagent:../target/spring-instrument.jar -jar ../target/cpath2.war -b "$@"
