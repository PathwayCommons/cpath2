#!/bin/sh

#set the environment variable CPATH2_HOME first.

$JAVA_HOME/bin/java -Dfile.encoding=UTF-8 -Xmx4096M -XX:PermSize=128m -XX:MaxPermSize=256m -DCPATH2_HOME=$CPATH2_HOME -jar cpath-service.jar $1 $2 $3
