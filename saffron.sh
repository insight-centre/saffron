#!/bin/bash
## This is the generic run script for Saffron, that will apply all steps to
## a single corpus. It can be customized for particular runs

# Find the directory of this script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# Function to quit on error
die() { echo "$@" 1>&2 ; exit 1; }

if [ -z $1 ] || [ -z $2 ]
then
    die "Usage\n\t./saffron.sh corpus[.json] output/ config.json"
fi

if [ -z $3 ]
then
    CONFIG=models/config.json
else
    CONFIG=$3
fi

OUTPUT=$2
mkdir -p $OUTPUT

# Step 0: Create configurations
if [ -f $DIR/models/dbpedia.db ]
then
    cat > $OUTPUT/dbpedia.config << DBP_CONFIG
{
    "database": "$DIR/models/dbpedia.db"
}
DBP_CONFIG
fi

echo "########################################"
echo "## Step 1:Indexing corpus             ##"
echo "########################################"
if [[ "$1" == *.json ]] || [ -d $1 ] || [[ "$1" == *.zip ]] || [[ "$1" == *.tar.gz ]] || [[ "$1" == *.tgz ]]
then
    $DIR/index-corpus -c $1 -i $OUTPUT/index || die "Indexing failed"
    CORPUS=$OUTPUT/index
else
    die "$1 should be Json document, archive or a folder of text files"
fi

echo "########################################"
echo "## Step 2: Term Extraction            ##"
echo "########################################"
$DIR/extract-terms -c $CONFIG \
    -x $CORPUS -t $OUTPUT/terms-extracted.json \
    -o $OUTPUT/doc-terms.json

echo "########################################"
echo "## Step 3: Concept Consolidation      ##"
echo "########################################"
#$DIR/consolidate-concepts -c $CONFIG \
#	-t $OUTPUT/terms-extracted.json \
#	-o $OUTPUT/concepts.json 

echo "########################################"
echo "## Step 4: Author Consolidation       ##"
echo "########################################"
$DIR/consolidate-authors -t $CORPUS 

echo "########################################"
echo "## Step 5: DBpedia Lookup             ##"
echo "########################################"
if [ -z $DBP_CONFIG ]
then
    echo "Skipping"
    mv $OUTPUT/terms-extracted.json $OUTPUT/terms.json
else
$DIR/dbpedia-lookup -c $DBP_CONFIG -t $OUTPUT/terms-extracted.json \
    -o $OUTPUT/terms.json
fi

echo "########################################"
echo "## Step 6: Connect Authors            ##"
echo "########################################"
$DIR/connect-authors -c $CONFIG -t $CORPUS -p $OUTPUT/terms.json -d $OUTPUT/doc-terms.json -o $OUTPUT/author-terms.json

echo "########################################"
echo "## Step 7: Term Similarity            ##"
echo "########################################"
$DIR/term-sim -d $OUTPUT/doc-terms.json -o $OUTPUT/term-sim.json

echo "########################################"
echo "## Step 8: Author Similarity          ##"
echo "########################################"
$DIR/author-sim -d $OUTPUT/author-terms.json -o $OUTPUT/author-sim.json

echo "########################################"
echo "## Step 9: Taxonomy Extraction        ##"
echo "########################################"
$DIR/taxonomy-extract -d $OUTPUT/doc-terms.json -t $OUTPUT/terms.json -o $OUTPUT/taxonomy.json -c $CONFIG
$DIR/kg-extract -d $OUTPUT/doc-terms.json -t $OUTPUT/terms.json -o $OUTPUT/kg.json -c $CONFIG

echo "########################################"
echo "## Step 10: RDF Extraction            ##"
echo "########################################"
$DIR/export-kg.sh -b http://saffron.insight-centre.org -o $OUTPUT/kg.rdf -t kg -d $OUTPUT

#echo "Creating taxonomy at" $OUTPUT/taxonomy.html
#python3 $DIR/taxonomy-to-html.py $OUTPUT/taxonomy.json $OUTPUT/doc-terms.json $OUTPUT/corpus.json > $OUTPUT/taxonomy.html Taxonomy
