#!/bin/bash
## This script starts Saffron in server mode
## The folder where the data will be written should be given, and if there
## is already data there the browser interface will start, otherwise the 
## execution (Teanga) interface will be used

# Find the directory of this script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# Function to quit on error
die() { echo "$@" 1>&2 ; exit 1; }

if [ -z $1 ]
then
    die "Usage\n\t./saffron-web.sh data-dir/"
fi

cd $DIR/web
mvn -q exec:java -f pom.xml -Dexec.args="-d $1"
