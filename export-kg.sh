#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Function to quit on error
die() { echo "$@" 1>&2 ; exit 1; }

cd $DIR/web
export MONGO_HOST=localhost
export MONGO_PORT=27017
export MONGO_DB_NAME=saffron_test
mvn -q exec:java@kg-extraction -f pom.xml -Dexec.args="$*"
