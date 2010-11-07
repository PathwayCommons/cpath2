#!/bin/sh

#set the environment variable CPATH2_HOME first.

$JAVA_HOME/bin/java -Xmx1536M -DCPATH2_HOME=$CPATH2_HOME -jar cpath-admin.jar $1 $2 $3
