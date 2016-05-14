#!/bin/bash

set -e

VERSION=1
CACHE_ROOT=~/.cache/mongoschema/$VERSION

mkdir -p $CACHE_ROOT

ARCHIVE_FILE=$CACHE_ROOT/mongoschema.tar.bz2
RUN_FILE=$CACHE_ROOT/bin/app

if [ ! -e "$RUN_FILE" ]; then
    echo "Downloading binaries..."
    curl -L https://dl.bintray.com/scf37/generic/mongoschema-$VERSION.tar.bz2 -o $ARCHIVE_FILE
    tar -xf $ARCHIVE_FILE -C $CACHE_ROOT
fi

export WORKDIR=$(pwd)

exec $RUN_FILE $*
