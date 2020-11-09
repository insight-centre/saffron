#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

export SAFFRON_HOME=$DIR
export MONGO_HOST=localhost
export MONGO_PORT=27017
export MONGO_DB_NAME=saffron_test
export STORE_LOCAL_COPY=true
mvn -q exec:java -f $DIR/web/pom-no-web.xml -Dexec.mainClass="org.insightcentre.saffron.web.rdf.RDFConversion" -Dexec.args="$*"
