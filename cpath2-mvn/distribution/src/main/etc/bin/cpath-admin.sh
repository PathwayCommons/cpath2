#!/bin/sh

#set the environment variable CPATH2_HOME first.

$JAVA_HOME/bin/java -Dfile.encoding=UTF-8 -Xms4096M -Xmx4096M -XX:PermSize=64m -XX:MaxPermSize=128m -DCPATH2_HOME=$CPATH2_HOME -jar cpath-admin.jar $1 $2 $3 $4
