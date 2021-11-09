import json
import sys
import csv
import os
import uuid

#python3 parse_kibana_object.py $SUFFIX.ndjson  index_patterns.json $SUFFIX

def main(kibana_obj,indx_patterns_obj,indx_suffix,outfile_kibana_obj):

    vis_title_idref = {}
    vis_title_id_list = []
    id_set = set()
    vis_titles_ids = {}
    resp_json = []

    vis_titles = [
                "Total Terms",
                "Saffron Top Terms",
                "Occurrences Control",
                "Saffron Term-Doc Relation",
                "Saffron Term-Author Relation",
                "Total Authors",
                "Total Docs",
                "Saffron Term Similarity",
                "Saffron Graph",
                "Panel",
                "Saffron Term Synonyms",
                "Direct Parents",
                "Direct Children"]

    # parsing index patterns
    with open(indx_patterns_obj,'r') as f:
        indx_patterns = json.load(f)
        indx = indx_patterns['saved_objects']
        vis_title_id_list = [{x['attributes']['title']: x['id']} for x in indx if x['attributes']['title'].endswith(indx_suffix)]

    vis_title_id_dic = {k:v for element in vis_title_id_list for k,v in element.items()}

    for k,v in vis_title_id_dic.items():
        id_set.add(v)
        if k == "terms" + "_" + indx_suffix:
            vis_title_idref["Total Terms"] = v
            vis_title_idref["Saffron Top Terms"] = v
            # vis_title_idref["saffron_term_cloud"] = v
            vis_title_idref["Occurrences Control"] = v

        elif k == "term-sim0" + "_" + indx_suffix:
            vis_title_idref["Saffron Term Similarity"] = v

        elif k == "author-terms" + "_" + indx_suffix:
            vis_title_idref["Saffron Term-Author Relation"] = v
            vis_title_idref["Total Authors"] = v

        elif k == "doc-terms" + "_" + indx_suffix:
            vis_title_idref["Total Docs"] = v
            vis_title_idref["Saffron Term-Doc Relation"] = v

        elif k == "syns" + "_" + indx_suffix:
            vis_title_idref["Saffron Term Synonyms"] = v

        elif k == "parsed-kg" + "_" + indx_suffix:
            vis_title_idref["Saffron Graph"] = v

        elif k == "parsed-kg-tgt" + "_" + indx_suffix:
            vis_title_idref["Direct Children"] = v

        elif k == "parsed-kg-src" + "_" + indx_suffix:
            vis_title_idref["Direct Parents"] = v

        elif k == "corpus_embedded" + "_" + indx_suffix:
                vis_title_idref["Embedded Corpus"] = v
        vis_title_idref["Panel"] = ''

    # generate uuid for each visualization
    for i in vis_titles:
        random_uuid = str(uuid.uuid4())
        if random_uuid not in id_set:
            vis_titles_ids[i] = random_uuid
            id_set.add(random_uuid)
        else:
            random_uuid = str(uuid.uuid4())
            vis_titles_ids[i] = random_uuid
            id_set.add(random_uuid)


    ## changing referenced ID index patterns
    with open(kibana_obj,'r') as f:
        for i,line in enumerate(f):
            k_content = json.loads(line)
            resp_json.append(json.loads(line))
            title = resp_json[i]['attributes']['title']

            if title == "saffron_dashboard":
                    resp_json[i]['attributes']['title'] = "saffron_dashboard_" + indx_suffix
                    resp_json[i]['id'] = str(uuid.uuid4())
                    visualization_id = resp_json[i]['id']
                    resp_json[i]['references'][0]['id'] = vis_titles_ids['Total Terms'] #panel_0
                    resp_json[i]['references'][1]['id'] = vis_titles_ids['Saffron Top Terms'] #panel_1
                    resp_json[i]['references'][2]['id'] = vis_titles_ids['Panel'] #panel_2
                    resp_json[i]['references'][3]['id'] = vis_titles_ids['Occurrences Control'] #panel_3
                    resp_json[i]['references'][4]['id'] = vis_titles_ids['Saffron Term-Doc Relation'] #panel_4
                    resp_json[i]['references'][5]['id'] = vis_titles_ids['Saffron Term-Author Relation'] #panel_5
                    resp_json[i]['references'][6]['id'] = vis_titles_ids['Total Authors'] #panel_6
                    resp_json[i]['references'][7]['id'] = vis_titles_ids['Total Docs'] #panel_7
                    resp_json[i]['references'][8]['id'] = vis_titles_ids['Saffron Term Similarity'] #panel_8
                    resp_json[i]['references'][9]['id'] = vis_titles_ids['Saffron Graph'] #panel_9
                    resp_json[i]['references'][10]['id'] = vis_titles_ids['Saffron Term Synonyms'] #panel_10
                    resp_json[i]['references'][11]['id'] = vis_titles_ids['Direct Children'] #panel_11
                    resp_json[i]['references'][12]['id'] = vis_titles_ids['Direct Parents'] #panel_12

            else:
                    resp_json[i]['attributes']['title'] = resp_json[i]['attributes']['title'] + "_" + indx_suffix
                    resp_json[i]['id'] = vis_titles_ids[title]
                    visualization_id = resp_json[i]['id']

                    if resp_json[i]['references'] != [] :
                        resp_json[i]['references'][0]['id'] = vis_title_idref[title]
                        reference_id = resp_json[i]['references'][0]['id']
                    else:
                        resp_json[i]['attributes']['visState'] = "{\"title\":\"Panel\",\"type\":\"markdown\",\"params\":{\"markdown\":\"# Saffron Data\\nThis dashboard contains **Saffron Run for index: " \
                        + indx_suffix + "**.\",\"openLinksInNewTab\":false,\"fontSize\":10},\"aggs\":[]}"



    with open(outfile_kibana_obj, 'w') as outfile:
        for x in resp_json:
            json.dump(x, outfile)
            outfile.write('\n')

if __name__ == "__main__":
       main(sys.argv[1], sys.argv[2], sys.argv[3],sys.argv[4])
