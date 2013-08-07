#!/bin/bash
set -eu

if  [ "$HOSTNAME" = "picard" ] ; then
	DEST=/Users/suderman/Documents/GatePlugins/GrAF
else
	echo "No configuration specified."
	exit 1
fi

if [ ! -e VERSION ] ; then
	echo "VERSION file not found."
	exit 2
fi

VERSION=`cat VERSION`

cd target
tar -xzf GrAF-dist-$VERSION.tar.gz
cd GrAF
cp GrAF-GATE-$VERSION.jar $DEST
cp creole.xml $DEST
cp LICENSE* $DEST
cd ..
rm -rf GrAF

echo "ANC Plugin has been copied to Gate."
exit 0