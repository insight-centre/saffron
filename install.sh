#!/bin/bash

command -v mvn >/dev/null 2>&1 || { echo >&2 "Please install maven2."; exit 1; }
command -v curl >/dev/null 2>&1 || { echo >&2 "Please install curl."; exit 1; }
command -v wget >/dev/null 2>&1 || { echo >&2 "Please install wget."; exit 1; }
command -v unzip >/dev/null 2>&1 || { echo >&2 "Please install unzip."; exit 1; }

mkdir -p models

if [ ! -f models/COHA_term_occurrences.txt ]
then
    curl https://at.ispras.ru/owncloud/index.php/s/0eUMJywO3AhXDHb/download -o models/COHA_term_cooccurrences.txt
fi

if [ ! -f models/dbpedia.db ]
then
    echo "Building DBpedia"
    curl http://downloads.dbpedia.org/2015-10/core-i18n/en/redirects_en.ttl.bz2 -o redirects_en.ttl.bz2
    mvn -q exec:java -f topic/pom.xml -Dexec.mainClass="org.insightcentre.nlp.saffron.topic.dbpedia.ConstructDBpediaIndex" -Dexec.args="-d redirects_en.ttl.bz2 -o models/dbpedia.db"
    rm redirects_en.ttl.bz2
fi

echo "Building Saffron"
mvn install
