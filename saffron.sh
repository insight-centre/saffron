#!/bin/bash
## This is the generic run script for Saffron, that will apply all steps to
## a single corpus. It can be customized for particular runs

# Find the directory of this script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# Function to quit on error
die() { echo "$@" 1>&2 ; exit 1; }

if [ -z $1 ] || [ -z $2 ]
then
    die "Usage\n\t./saffron.sh corpus[.json] output/"
fi

OUTPUT=$2
mkdir -p $OUTPUT

# Step 0: Create configurations
if [ ! -f $DIR/models/stopwords/english ]
then
    die "Stopwords do not exist"
fi
if [ ! -f $DIR/models/en-chunker.bin ] || [ ! -f $DIR/models/en-pos-maxent.bin ] || [ ! -f $DIR/models/en-token.bin ]
then
    die "OpenNLP Models do not exist"
fi
if [ ! -d $DIR/gate ]
then
    die "GATE not installed"
fi
if [ ! -f $DIR/models/dbpedia.db ]
then
    die "DBpedia Index not built"
fi
cat > $OUTPUT/domain-model.config << DM_CONFIG
{
    "stopwords": "$DIR/models/stopwords/english",
    "tokenizerModel": "$DIR/models/en-token.bin",
    "posModel": "$DIR/models/en-pos-maxent.bin",
    "chunkModel": "$DIR/models/en-chunker.bin",
    "sentModel": "$DIR/models/en-sent.bin"
}
DM_CONFIG
cat > $OUTPUT/topic-extraction.config << TE_CONFIG
{
    "stopwords": "$DIR/models/stopwords/english",
    "gateHome": "$DIR/gate"
}
TE_CONFIG
cat > $OUTPUT/dbpedia.config << DBP_CONFIG
{
    "database": "$DIR/models/dbpedia.db"
}
DBP_CONFIG
cat > $OUTPUT/atr4s.config << ATR4S_CONFIG
{
    "corpus": "$DIR/models/COHA_term_cooccurrences.txt"
}
ATR4S_CONFIG

echo "########################################"
echo "## Step 1:Indexing corpus             ##"
echo "########################################"
if [[ "$1" == *.json ]]
then
    CORPUS=$1
    $DIR/index-corpus -c $CORPUS -i $OUTPUT/index || die "Indexing failed"
    cp $1 $OUTPUT/corpus-unconsolidated.json
elif [ -d $1 ] || [[ "$1" == *.zip ]] || [[ "$1" == *.tar.gz ]] || [[ "$1" == *.tgz ]]
then
    CORPUS=$OUTPUT/corpus-unconsolidated.json
    $DIR/index-corpus -c $1 -i $OUTPUT/index -o $CORPUS || die "Indexing failed"
else
    die "$1 should be Json or a Folder of text files"
fi

echo "########################################"
echo "## Step 2: Topic Extraction           ##"
echo "########################################"
$DIR/extract-topics -c $OUTPUT/atr4s.config \
    -x $CORPUS -t $OUTPUT/topics-extracted.json \
    -o $OUTPUT/doc-topics.json

echo "########################################"
echo "## Step 3: Author Consolidation       ##"
echo "########################################"
$DIR/consolidate-authors -t $CORPUS -o $OUTPUT/corpus.json
CORPUS=$OUTPUT/corpus.json

echo "########################################"
echo "## Step 4: DBpedia Lookup             ##"
echo "########################################"
$DIR/dbpedia-lookup -c $OUTPUT/dbpedia.config -t $OUTPUT/topics-extracted.json \
    -o $OUTPUT/topics.json

echo "########################################"
echo "## Step 5: Connect Authors            ##"
echo "########################################"
$DIR/connect-authors -t $CORPUS -p $OUTPUT/topics.json -d $OUTPUT/doc-topics.json -o $OUTPUT/author-topics.json

echo "########################################"
echo "## Step 6: Topic Similarity           ##"
echo "########################################"
$DIR/topic-sim -d $OUTPUT/doc-topics.json -o $OUTPUT/topic-sim.json

echo "########################################"
echo "## Step 7: Author Similarity          ##"
echo "########################################"
$DIR/author-sim -d $OUTPUT/author-topics.json -o $OUTPUT/author-sim.json

echo "########################################"
echo "## Step 8: Taxonomy Extraction       ##"
echo "########################################"
$DIR/taxonomy-extract -d $OUTPUT/doc-topics.json -t $OUTPUT/topics.json -o $OUTPUT/taxonomy.json

echo "Creating taxonomy at" $OUTPUT/taxonomy.html
python3 $DIR/taxonomy-to-html.py $OUTPUT/taxonomy.json $OUTPUT/doc-topics.json $OUTPUT/corpus.json > $OUTPUT/taxonomy.html Taxonomy
