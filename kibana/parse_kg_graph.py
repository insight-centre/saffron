import json
import sys
import csv
import os


def write_file(data,output_file):
    # output_file = output_file + '.json'
    if os.path.exists(output_file):
        os.remove(output_file)
    else:
        print("Creating {}".format(output_file))

    with open(output_file, 'w', encoding="utf-8") as file:
        json.dump(data,file,ensure_ascii=False, indent=4)



def process_children(data,entries,unique_terms,parent,relation_type,syns_d):
    for i in range(0,len(data)):
        group=0
        current_term = data[i];
        current_term_text = current_term['root']
        t_link_score = current_term['linkScore']
        t_score  = current_term['score']
        search_term_sys =  [a for a, b in syns_d.items() if current_term_text in b]
        if search_term_sys == []:
            group=0
        else:
            group = search_term_sys[0]
        entry = {"source":current_term_text,"target":parent,"saffron_score":t_score,\
                "relation_type":relation_type,"link_score":t_link_score,"group":str(group)}
        entries.append(entry)
        unique_terms.append([current_term_text,""])
        if "children" in current_term and len(current_term['children']) > 0:
            process_children(current_term['children'], entries, unique_terms, current_term_text,relation_type,syns_d)



def  parse_taxonomy(taxonomy,synonyms):
    entries = []
    unique_terms = []
    relation_type = "is_a"
    syns_d = dict(enumerate(synonyms,start=1))
    process_children(taxonomy,entries,unique_terms,None,relation_type,syns_d)
    return entries
    # write_file(entries)



def parse_partonomy(partonomy,synonymys):
    entries = []
    unique_terms = []
    relation_type = "part_of"
    syns_d = dict(enumerate(synonymys,start=1))
    process_children(partonomy,entries,unique_terms,None,relation_type,syns_d)
    # write_file(entries)
    return entries

def parse_syns(synonymy_clusters,syns_output_file):
    syns_list = []
    for term_list in synonymy_clusters:
        for element in term_list:
            term_string = element
            syns = [t for t in term_list if t != term_string]
            for syn in syns:
                entry = {"term_string": term_string, "synonymy": syn}
                syns_list.append(entry)
    syns_output_file = syns_output_file + ".json"
    write_file(syns_list,syns_output_file)

def main(input_file,output_file, syns_output_file):
    kg_json = json.load(open(input_file,'r'))
    kg_json = json.dumps(kg_json).replace('null', '"null"')
    kg_json = json.loads(kg_json)

    taxonomy = []
    taxonomy.append(kg_json['taxonomy'].copy())
    partonomy = kg_json['partonomy']['components']
    synonymy  = kg_json['synonymyClusters']

    is_a_entries = parse_taxonomy(taxonomy,synonymy)
    part_of_entries = parse_partonomy(partonomy,synonymy)
    data = is_a_entries
    data.extend(part_of_entries)
    write_file(data,output_file)
    parse_syns(synonymy,syns_output_file)


# run python3  main.py kg.json output.json
if __name__ == "__main__":
       input_file = sys.argv[1]
       output_file = sys.argv[2]
       syns_output_file = sys.argv[3]
       main(input_file, output_file,syns_output_file)
