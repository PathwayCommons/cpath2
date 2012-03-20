#!/bin/sh

#set the environment variable CPATH2_HOME first.

$JAVA_HOME/bin/java -ea -Dfile.encoding=UTF-8 -Xss65536k -Xmx8g -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -jar cpath-admin.jar $1 $2 $3 $4 $5

# for jprofiling
#$JAVA_HOME/bin/java -agentpath:/data/local/jprofiler6/bin/linux-x64/libjprofilerti.so=port=48000,wait -Dfile.encoding=UTF-8 -Xss65536k -Xmx8g -DCPATH2_HOME=$CPATH2_HOME -Djava.io.tmpdir=$CPATH2_HOME/tmp -jar cpath-admin.jar $1 $2 $3 $4 $5

#$JAVA_HOME/bin/java -Dfile.encoding=UTF-8 -Xms4g -Xmx8g -Xmn1g -XX:PermSize=128m -XX:MaxPermSize=256m -XX:SurvivorRatio=16 -Xincgc -Xnoclassgc -XX:CMSTriggerRatio=50 -XX:ParallelGCThreads=2 -XX:NewRatio=5 -Xss2048k -DCPATH2_HOME=$CPATH2_HOME -jar cpath-admin.jar $1 $2 $3 $4 $5
