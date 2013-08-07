#!/bin/bash
set -eu
VERSION=`cat VERSION`
anc-put target/GrAF-dist-$VERSION.zip /home/www/anc/tools/gate
