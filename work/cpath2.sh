#!/bin/sh

CPATH2_HOME="."

CPATH2_OPTS="-Dfile.encoding=UTF-8 -Xss32m -Xmx60g -Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider"
# CPATH2_DEBUG_OPTS="-Dlogback.configurationFile=logback.xml -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=12345"

if [ "-premerge" in @ ] ; then
    # enable load time weaving
    CPATH2_OPTS="$CPATH2_OPTS -javaagent:../target/spring-instrument.jar"
fi

$JAVA_HOME/bin/java $CPATH2_OPTS -jar ../target/cpath2.war "$1" "$2" "$3" "$4" "$5"
