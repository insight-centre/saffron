#!/bin/bash

ES=http://localhost:9200
KIBANA=http://localhost:5601
ESTYPE="doc"
SUFFIX=$1
DIR=$2
ELK_DIR="$DIR/elk_$SUFFIX"

PARSED_KG_FILE_ex="parsed-kg.json"
PARSED_KG_FILE="parsed-kg"
PARSED_SYNS="syns"
ELK_INDX_DIR="$ELK_DIR/index_dir_${SUFFIX}"
ELK_PRC_DIR="$ELK_DIR/processing_dir_${SUFFIX}"
ELK_DB_DIR="$ELK_DIR/db_dir_${SUFFIX}"


if [ ! -d "$ELK_DIR" ]
then mkdir "$ELK_DIR"
fi

if [ ! -d "$ELK_PRC_DIR" ]
then mkdir "$ELK_PRC_DIR"
fi

if [ ! -d "$ELK_INDX_DIR" ]
then mkdir "$ELK_INDX_DIR"
fi

if [ ! -d "$ELK_DB_DIR" ]
then mkdir "$ELK_DB_DIR"
fi

echo "#################################################"
echo "## Preprocessing files for indexing            ##"
echo "#################################################"

for file in $(ls $DIR/{author-terms,corpus,term-sim,corpus_embedded,terms,doc-terms,kg}.json); do
  cp $file $ELK_PRC_DIR
done

# formating author-terms
sed -i 's/termId/term_string/g' "$ELK_PRC_DIR/author-terms.json"

# formating corpus.json
sed -i 's/^{//;s/"documents" : //;s/}$//' "$ELK_PRC_DIR/corpus.json"


# formmating term-sim
cp "$ELK_PRC_DIR/term-sim.json" "$ELK_PRC_DIR/term-sim0.json"
cp "$ELK_PRC_DIR/term-sim.json" "$ELK_PRC_DIR/term-sim1.json"

comma_joint=","
echo "[" > "$ELK_PRC_DIR/term-sim2.json"
 for f in $(ls $ELK_PRC_DIR/{term-sim0,term-sim1}.json);
 do
   echo $f
   sed -i 's/term2_id/term_string/g' $f
   sed -i  's/\[//g;s/\]//g' $f
   cat $f >> "$ELK_PRC_DIR/term-sim2.json"
   echo $comma_joint >> "$ELK_PRC_DIR/term-sim2.json"
   comma_joint=''
 done;
 echo "]" >> "$ELK_PRC_DIR/term-sim2.json"

rm "$ELK_PRC_DIR/term-sim0.json" "$ELK_PRC_DIR/term-sim1.json"
mv "$ELK_PRC_DIR/term-sim2.json" "$ELK_PRC_DIR/term-sim0.json"

# parse saffron kg
python3 parse_kg_graph.py   "$ELK_PRC_DIR/kg.json" "$ELK_PRC_DIR/$PARSED_KG_FILE_ex" "$ELK_PRC_DIR/$PARSED_SYNS"
cp "$ELK_PRC_DIR/$PARSED_KG_FILE_ex" "$ELK_PRC_DIR/parsed-kg-src.json"
cp "$ELK_PRC_DIR/$PARSED_KG_FILE_ex" "$ELK_PRC_DIR/parsed-kg-tgt.json"

sed -i 's/source/term_string/g' "$ELK_PRC_DIR/parsed-kg-src.json"
sed -i 's/target/term_string/g' "$ELK_PRC_DIR/parsed-kg-tgt.json"

echo "\n#################################################"
echo "##    Indexing documents in Elasticsearch      ##"
echo "#################################################"

for file in $(ls $ELK_PRC_DIR/{author-terms,corpus,term-sim0,corpus_embedded,terms,doc-terms,parsed-kg-src,parsed-kg-tgt,${PARSED_SYNS},${PARSED_KG_FILE}}.json); do
  in_filename=`basename $file`
  in_basename=`basename -s .json $in_filename`
  es_index=${in_basename}_${SUFFIX}
  o_filename=${in_basename}_${SUFFIX}.json
  o_filepath="$ELK_INDX_DIR/$o_filename"
  python3  json_to_ndjson.py $file $es_index "$ELK_INDX_DIR/$o_filename"
  echo "ES Indexing $in_filename of $ESTYPE  to $es_index .\n"
    curl -H 'Content-Type: application/x-ndjson' \
        -XPOST 'localhost:9200/$es_index/doc/_bulk?pretty' \
        --data-binary @$o_filepath
done


echo "#################################################"
echo "##   Registering ES indices as index-patterns  ##"
echo "#################################################"

for index in $(curl -s -XGET ${ES}/_cat/indices?h=i)
do
    if [[ ! ${index:0:1} = "." ]] && [[ ${index#*_} = $SUFFIX ]]; then
        echo "\n"
        echo "Registering ES index $index."
        echo "\n"
        curl -XPOST ${KIBANA}/api/saved_objects/index-pattern \
        -H "kbn-xsrf: true" \
        -H 'Content-Type: application/json;charset=UTF-8' \
        --data-binary "{\"attributes\":{\"title\":\"${index}\",\"timeFieldName\":\"${date}\"}}"
      fi
done

# printf "\n"
echo "\n"
echo "#################################################"
echo "##      Importing Kibana Dashboard             ##"
echo "#################################################"

tmp_dashboard="$ELK_DB_DIR/saffron_dashboard_tmp_${SUFFIX}.ndjson"
index_patterns="$ELK_DB_DIR/index_patterns_${SUFFIX}.json"
custom_dashboard="$ELK_DB_DIR/saffron_dashboard_${SUFFIX}.ndjson"

# cp  saffron_so3_dashboard.ndjson  $tmp_dashboard
cp  saffron_dashboard.ndjson  $tmp_dashboard


curl -s "${KIBANA}/api/saved_objects/_find?fields=title&fields=type&per_page=10000&type=index-pattern" >  $index_patterns
python3 parse_kibana_object.py $tmp_dashboard  $index_patterns $SUFFIX $custom_dashboard
rm $tmp_dashboard

# upload kibana dashboard
curl -X POST ${KIBANA}/api/saved_objects/_import -H 'kbn-xsrf: true' --form file=@${custom_dashboard}
