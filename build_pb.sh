#!/bin/bash
#
# build the protobuf classes from the data.proto. Note tested with 
# protobuf 2.4.1. Current version is 2.5.0.
#
# Building: 
# 
# Running this script is only needed when the protobuf structures 
# have change.
#

project_base="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# which protoc that you built
PROTOC_HOME=/usr/local/protobuf-2.4.1/

rm -r ${project_base}/generated/*
$PROTOC_HOME/bin/protoc --proto_path=${project_base}/resources --java_out=${project_base}/generated ${project_base}/resources/comm.proto
