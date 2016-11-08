#!/bin/bash

command -v mvn >/dev/null 2>&1 || { echo >&2 "Please install maven2."; exit 1; }
command -v curl >/dev/null 2>&1 || { echo >&2 "Please install curl."; exit 1; }
command -v wget >/dev/null 2>&1 || { echo >&2 "Please install wget."; exit 1; }
command -v unzip >/dev/null 2>&1 || { echo >&2 "Please install unzip."; exit 1; }

mkdir -p models
if [ ! -f models/en-chunker.bin ]
then
    echo "Downloading OpenNLP models"
    curl http://opennlp.sourceforge.net/models-1.5/en-chunker.bin    -o models/en-chunker.bin
fi

if [ ! -f models/en-token.bin ]
then
    curl http://opennlp.sourceforge.net/models-1.5/en-token.bin      -o models/en-token.bin
fi

if [ ! -f models/en-pos-maxent.bin ]
then
    curl http://opennlp.sourceforge.net/models-1.5/en-pos-maxent.bin -o models/en-pos-maxent.bin
fi

if [ ! -f models/stopwords/english ]
then
    echo "Getting stopword lists"
    curl https://raw.githubusercontent.com/nltk/nltk_data/gh-pages/packages/corpora/stopwords.zip -o stopwords.zip
    unzip stopwords.zip -d models/
    rm stopwords.zip
fi

if [ ! -f gate ]
then
    echo "You need to install GATE... please do the following:"
    echo "Download GATE from http://downloads.sourceforge.net/project/gate/gate/8.2/gate-8.2-build5482-BIN.zip"
    echo "unzip gate-8.2-build5482-BIN.zip"
    echo "ln -s /path/to/gate `pwd`/gate"
    read -p "Continue when GATE is installed"
fi

if [ ! -f models/dbpedia.db ]
then
    echo "Building DBpedia"
    curl http://downloads.dbpedia.org/2015-10/core-i18n/en/redirects_en.ttl.bz2 -o redirects_en.ttl.bz2
    mvn -q exec:java -p topic/pom.xml -Dexec.mainClass="org.insightcentre.nlp.saffron.topic.dbpedia.ConstructDBpediaIndex" -Dexec.args="-d redirects_en.ttl.bz2 -o models/dbpedia.db"
    rm redirects_en.ttl.bz2
fi

#if [ ! -f models/ngrams.db ]
#then
#    echo "Building NGrams"
#    mkdir ngrams
#    cd ngrams
#    cat ../topic/src/main/resources/bigram_links.txt | xargs wget --limit-rate=10000k 
#    cd -
#    for f in `ls ngrams`
#    do
#        mvn -q exec:java -Dexec.mainClass="org.insightcentre.nlp.saffron.topics.ngrams.ConstructNGramIndex" -Dexec.args="-d $f -o models/ngrams.db"
#    done
#fi

echo "Building Saffron"
mvn install
