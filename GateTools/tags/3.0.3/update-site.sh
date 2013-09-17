#!/bin/bash
set -eu
VERSION=`cat VERSION`
cd target
unzip GrAF-dist-$VERSION.zip
anc-put GrAF-dist-$VERSION.zip /home/www/anc/tools/gate
anc-put GrAF/ /home/www/anc/tools/gate/GrAF-GATE-$VERSION

