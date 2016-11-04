#!/bin/bash

command -v mvn >/dev/null 2>&1 || { echo >&2 "Please install maven2."; exit 1; }
command -v curl >/dev/null 2>&1 || { echo >&2 "Please install curl."; exit 1; }
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

echo "Building Saffron"
mvn install
