#!/bin/bash

if  [ "$HOSTNAME" = "picard" ] ; then
	DEST=/Users/suderman/Documents/GatePlugins/GrAF
else
	echo "No configuration specified."
	exit 1
fi

cp target/GrAF*.jar $DEST
cp src/main/resources/creole.xml $DEST

echo "ANC Plugin has been copied to Gate."
exit 0