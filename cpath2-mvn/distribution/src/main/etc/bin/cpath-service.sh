#!/bin/sh

#set the environment variable CPATH2_HOME first.

$JAVA_HOME/bin/java -Dfile.encoding=UTF-8 -Xms6g -Xmx6g -XX:PermSize=512m -XX:MaxPermSize=512m -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -jar cpath-service.jar $1 $2 $3 $4
