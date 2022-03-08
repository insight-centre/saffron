#!/bin/bash
## This is the generic run script for Saffron, that will apply all steps to
## a single corpus. It can be customized for particular runs

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

mvn -q exec:java -f $DIR/run/pom.xml -Dexec.mainClass="org.insightcentre.nlp.saffron.run.SaffronPipeline" -Dexec.args="$*"
