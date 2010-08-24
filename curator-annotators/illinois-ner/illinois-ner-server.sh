#!/bin/sh
START=$PWD
DIRNAME=`dirname "$0"`
SCRIPTS_HOME=`cd "$DIRNAME" > /dev/null && pwd`
CURATOR_BASE=$SCRIPTS_HOME/..
CURATOR_BASE=`cd "$CURATOR_BASE" > /dev/null && pwd`
LIBDIR=$CURATOR_BASE/lib
COMPONENTDIR=$CURATOR_BASE/components

COMPONENT_CLASSPATH=$CURATOR_BASE:$COMPONENTDIR/illinois-ner-server.jar:$COMPONENTDIR/curator-interfaces.jar

# ner has a dependency on an older LBJ2Library (2.2.2)

LIB_CLASSPATH=$LIBDIR/LbjNerTagger.jar:$LIBDIR/LBJ2Library-2.2.2.jar:$LIBDIR/commons-cli-1.2.jar:/commons-lang-2.5.jar:$LIBDIR/commons-logging-1.1.1.jar::$LIBDIR/h2-1.1.118.jar:$LIBDIR/libthrift.jar:$LIBDIR/logback-classic-0.9.17.jar:$LIBDIR/logback-core-0.9.17.jar:$LIBDIR/slf4j-api-1.5.8.jar

CLASSPATH=$COMPONENT_CLASSPATH:$LIB_CLASSPATH:$NER_CLASSPATH

cd $CURATOR_BASE
echo java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx2G edu.illinois.cs.cogcomp.annotation.server.IllinoisNERServer $@
exec java -cp $CLASSPATH -Dhome=$CURATOR_BASE -Xmx2G edu.illinois.cs.cogcomp.annotation.server.IllinoisNERServer $@
cd $START