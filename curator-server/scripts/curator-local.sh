#!/bin/sh
START=$PWD
DIRNAME=`dirname "$0"`
SCRIPTS_HOME=`cd "$DIRNAME" > /dev/null && pwd`
CURATOR_BASE=$SCRIPTS_HOME/..
CURATOR_BASE=`cd "$CURATOR_BASE" > /dev/null && pwd`
LIBDIR=$CURATOR_BASE/lib
COMPONENTDIR=$CURATOR_BASE/components

COMPONENT_CLASSPATH=$CURATOR_BASE:$COMPONENTDIR/curator-server.jar:$COMPONENTDIR/illinois-tokenizer-server.jar:$COMPONENTDIR/illinois-pos-server.jar:$COMPONENTDIR/illinois-chunker-server.jar:$COMPONENTDIR/illinois-coref-server.jar:$COMPONENTDIR/stanford-parser-server.jar:$COMPONENTDIR/curator-interfaces.jar

LIB_CLASSPATH=$LIBDIR/LBJ2Library.jar:$LIBDIR/LBJChunk.jar:$LIBDIR/LBJCoref.jar:$LIBDIR/LBJPOS.jar:$LIBDIR/commons-cli-1.2.jar:$LIBDIR/commons-collections-3.2.1.jar:$LIBDIR/commons-configuration-1.6.jar:$LIBDIR/commons-lang-2.5.jar:$LIBDIR/commons-logging-1.1.1.jar:$LIBDIR/h2-1.1.118.jar:$LIBDIR/junit-4.4.jar:$LIBDIR/libthrift.jar:$LIBDIR/logback-classic-0.9.17.jar:$LIBDIR/logback-core-0.9.17.jar:$LIBDIR/slf4j-api-1.5.8.jar:$LIBDIR/stanford-parser-2010-08-16.jar

CLASSPATH=$COMPONENT_CLASSPATH:$LIB_CLASSPATH

cd $CURATOR_BASE
echo java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx4000m edu.illinois.cs.cogcomp.curator.CuratorServer $@
exec java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx4000m edu.illinois.cs.cogcomp.curator.CuratorServer $@
cd $START