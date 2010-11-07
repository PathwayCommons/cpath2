#!/bin/sh

#set the environment variable CPATH2_HOME first.

$JAVA_HOME/bin/java -Dfile.encoding=UTF-8 -Xmx2048M -DCPATH2_HOME=$CPATH2_HOME -jar cpath-service.jar $1 $2 $3
