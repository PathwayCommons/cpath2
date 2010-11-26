#!/bin/sh

#set the environment variable CPATH2_HOME first.

$JAVA_HOME/bin/java -Dfile.encoding=UTF-8 -Xms4g -Xmx8g -Xmn1g -XX:PermSize=128m -XX:MaxPermSize=256m -XX:SurvivorRatio=16 -Xincgc -Xnoclassgc -XX:CMSTriggerRatio=50 -XX:ParallelGCThreads=2 -XX:NewRatio=5 -DCPATH2_HOME=$CPATH2_HOME -jar cpath-admin.jar $1 $2 $3 $4

#$JAVA_HOME/bin/java -Dfile.encoding=UTF-8 -Xms4g -Xmx8g -Xmn1g -XX:PermSize=128m -XX:MaxPermSize=256m -XX:SurvivorRatio=16 -Xincgc -Xnoclassgc -XX:CMSTriggerRatio=50 -XX:ParallelGCThreads=2 -XX:NewRatio=5 -DCPATH2_HOME=$CPATH2_HOME -jar cpath-admin.jar $1 $2 $3 $4
