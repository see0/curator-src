#!/bin/sh
START=$PWD
DIRNAME=`dirname "$0"`
CURATOR_BASE=`cd "$DIRNAME" > /dev/null && pwd`
LIBDIR=$CURATOR_BASE/lib
DATADIR=$CURATOR_BASE/data
TMPDIR=$CURATOR_BASE/tmp

echo "About to download required libraries and data files to run the Curator and included annotators"

mkdir -p $TMPDIR
cd $TMPDIR
echo "Downloading component dependencies"
curl -O http://l2r.cs.uiuc.edu/~cogcomp/Software/LBJ2Library.jar
curl -O http://l2r.cs.uiuc.edu/~cogcomp/Software/LBJPOS.jar
curl -O http://l2r.cs.uiuc.edu/~cogcomp/Software/LBJChunk.jar
curl -O http://l2r.cs.uiuc.edu/~cogcomp/Software/LBJCoref.jar
curl -O http://l2r.cs.uiuc.edu/~cogcomp/Software/illinois-ner-server-dependencies.tar.gz
curl -O http://nlp.stanford.edu/software/stanford-parser-2010-07-09.tgz

echo "Unpacking dependencies"
tar xzf illinois-ner-server-dependencies.tar.gz
tar xzf stanford-parser-2010-07-09.tgz

echo "Moving dependencies"
mv LBJ2Library.jar $LIBDIR
mv LBJPOS.jar $LIBDIR
mv LBJChunk.jar $LIBDIR
mv LBJCoref.jar $LIBDIR
mv illinois-ner-server-dependencies/LBJ2Library-2.2.2.jar $LIBDIR
mv illinois-ner-server-dependencies/LbjNerTagger.jar $LIBDIR
mv stanford-parser-2010-07-09/stanford-parser-2010-07-09.jar $LIBDIR

echo "Moving model files and data"
mkdir -p $DATADIR
mv illinois-ner-server-dependencies/ner $DATADIR
mv stanford-parser-2010-07-09/englishPCFG.ser.gz $DATADIR

echo "Cleaning up"
cd $CURATOR_BASE
rm -rf $TMPDIR

echo "Done"
