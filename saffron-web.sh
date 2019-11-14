#!/bin/bash
## This script starts Saffron in server mode
## The folder where the data will be written should be given, and if there
## is already data there the browser interface will start, otherwise the 
## execution (Teanga) interface will be used

# Find the directory of this script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# Function to quit on error
die() { echo "$@" 1>&2 ; exit 1; }

cd $DIR/web
export SAFFRON_HOME=$DIR
export MONGO_HOST=localhost
export MONGO_PORT=27017
export MONGO_DB_NAME=saffron_test2
export STORE_LOCAL_COPY=false
mvn -q exec:java -f pom.xml -Dexec.args="$*"
