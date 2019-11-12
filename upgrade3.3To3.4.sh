#!/bin/bash
## This script performs database migration in order to upgrade Saffron.
## The MONGO configurations below should be the same from the installation
## being upgraded. 
##
## ALWAYS Make sure you have a backup of the database before performing an 
## upgrade.

# Find the directory of this script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# Function to quit on error
die() { echo "$@" 1>&2 ; exit 1; }

cd $DIR/web
export MONGO_HOST=localhost
export MONGO_PORT=27017
export MONGO_DB_NAME=saffron_test
mvn -q exec:java@upgrade -f pom.xml -Dexec.args="$*"
