#!/bin/sh
START=$PWD
DIRNAME=`dirname "$0"`
SCRIPTS_HOME=`cd "$DIRNAME" > /dev/null && pwd`
CURATOR_BASE=$SCRIPTS_HOME/../..
CURATOR_BASE=`cd "$CURATOR_BASE" > /dev/null && pwd`
LIBDIR=$CURATOR_BASE/lib

CLASSPATH=${CURATOR_BASE}/curator-server/bin:$CURATOR_BASE/curator-server/configs:$LIBDIR/LBJ2Library.jar:$LIBDIR/LBJChunk.jar:$LIBDIR/LBJCoref.jar:$LIBDIR/LBJPOS.jar:$LIBDIR/commons-cli-1.2.jar:$LIBDIR/commons-collections-3.2.1.jar:$LIBDIR/commons-configuration-1.6.jar:$LIBDIR/commons-lang-2.4.jar:$LIBDIR/commons-logging-1.1.1.jar:$LIBDIR/curator-interfaces.jar:$LIBDIR/h2-1.1.118.jar:$LIBDIR/junit-4.4.jar:$LIBDIR/libthrift.jar:$LIBDIR/logback-classic-0.9.17.jar:$LIBDIR/logback-core-0.9.17.jar:$LIBDIR/slf4j-api-1.5.8.jar:$LIBDIR/stanford-parser-2010-07-09.jar:$CURATOR_BASE/curator-annotators/illinois-chunker/bin:$CURATOR_BASE/curator-annotators/illinois-coref/bin:$CURATOR_BASE/curator-annotators/illinois-pos/bin:$CURATOR_BASE/curator-annotators/illinois-tokenizer/bin:$CURATOR_BASE/curator-annotators/stanford-parser/bin


cd $CURATOR_BASE/curator-server
java -cp $CLASSPATH -Dhome=$CURATOR_BASE/curator-server -Xmx 4000m edu.illinois.cs.cogcomp.curator.CuratorServer -config configs/curator.properties -annotators configs/annotators-local.xml -port 9090
cd $PWD