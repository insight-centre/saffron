#!/bin/bash

command -v mvn >/dev/null 2>&1 || { echo >&2 "Please install maven2."; exit 1; }
command -v curl >/dev/null 2>&1 || { echo >&2 "Please install curl."; exit 1; }
command -v tar >/dev/null 2>&1 || { echo >&2 "Please install tar."; exit 1; }
command -v bzip2 >/dev/null 2>&1 || { echo >&2 "Please install bzip2."; exit 1; }

mkdir -p models

if [ ! -f models/config.json ] || [ ! -f dbpedia.db ] || [ ! -f en-lemmatizer.dict.txt ] || [ ! -f en-pos-maxent.bin ] || [ ! -f glove.6B.50d.txt ] || [ ! -f svd.ave ] || [ ! -f svd.minMax ] || [ ! -f svm ] || [ ! -f wiki-terms.json.gz ] || [ ! -f wn-hyps.json.gz ]
then
    curl http://server1.nlp.insight-centre.org/saffron-datasets/models.tar.bz2 -o models.tar.bz2
    tar xjf models.tar.bz2 && rm models.tar.bz2
fi

mvn -q install
