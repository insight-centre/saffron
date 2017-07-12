import sys
import json

def gen_label(taxo, topic_wts):
    if "root" in taxo:
        print("  \"%s\" [ weight=%.4f ];" % (taxo["root"], topic_wts[taxo["root"]]))
    if "children" in taxo:
        for child in taxo["children"]:
            gen_label(child, topic_wts)

def gen_link(taxo):
    if "root" in taxo and "children" in taxo:
        src = taxo["root"]
        for child in taxo["children"]:
            trg = child["root"]
            print("  \"" + src + "\" -> \"" + trg + "\";")
            gen_link(child)

def main(args):
    taxonomy = json.load(open(args[0]))
    topics = json.load(open(args[1]))
    topic_wts = { x["topic_string"]: x["score"] for x in topics }
    
    print("digraph G {")
    
    gen_label(taxonomy, topic_wts)

    gen_link(taxonomy)


    print("}")

if __name__ == "__main__":
       main(sys.argv[1:])

