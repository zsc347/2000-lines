#!/bin/bash

if [ $# -ne 1 ]; then
    echo "useage: ./build.sh <folder>"
    exit 1
fi

pushd $1 > /dev/null
echo building `pwd`

pushd client > /dev/null
go build
mv client ../../
popd  > /dev/null

pushd server  > /dev/null
go build
mv server ../../
popd  > /dev/null


popd > /dev/null