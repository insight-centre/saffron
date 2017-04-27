#!/bin/bash

command -v mvn >/dev/null 2>&1 || { echo >&2 "Please install maven2."; exit 1; }
command -v curl >/dev/null 2>&1 || { echo >&2 "Please install curl."; exit 1; }
command -v tar >/dev/null 2>&1 || { echo >&2 "Please install tar."; exit 1; }

mkdir -p models

#if [ ! -f models/COHA_term_occurrences.txt ] || [ ! -f models/info_measure.txt ] || [ ! -f models/w2vConcepts ]
#then
#    curl https://at.ispras.ru/owncloud/index.php/s/0eUMJywO3AhXDHb/download -o models/COHA_term_cooccurrences.txt
#    curl https://at.ispras.ru/owncloud/index.php/s/MzVm6GVOQ4eTJyR/download -o models/info_measure.txt
#    curl https://at.ispras.ru/owncloud/index.php/s/SWP1YiISQPQCqTj/download -o models/w2vConcepts.model
#fi
#
#if [ ! -f models/dbpedia.db ]
#then
#    echo "Building DBpedia"
#    curl http://downloads.dbpedia.org/2015-10/core-i18n/en/redirects_en.ttl.bz2 -o redirects_en.ttl.bz2
#    mvn -q exec:java -f topic/pom.xml -Dexec.mainClass="org.insightcentre.nlp.saffron.topic.dbpedia.ConstructDBpediaIndex" -Dexec.args="-d redirects_en.ttl.bz2 -o models/dbpedia.db"
#    rm redirects_en.ttl.bz2
#fi

if [ ! -f models/COHA_term_occurrences.txt ] || [ ! -f models/info_measure.txt ] || [ ! -f models/w2vConcepts ] || [ ! -f models/dbpedia.db ]
then
    curl http://server1.nlp.insight-centre.org/saffron-data/models.tar.bz2 -o models.tar.bz2
    tar xjf models.tar.bz2 && rm models.tar.bz2
fi

mvn -q install
