import sys
import json

def gen_label(taxo):
    if "root" in taxo:
        print("  \"" + taxo["root"] + "\";")
    if "children" in taxo:
        for child in taxo["children"]:
            gen_label(child)

def gen_link(taxo):
    if "root" in taxo and "children" in taxo:
        src = taxo["root"]
        for child in taxo["children"]:
            trg = child["root"]
            print("  \"" + src + "\" -> \"" + trg + "\";")
            gen_link(child)

def main(args):
    taxonomy = json.load(open(args[0]))
    
    print("digraph G {")
    
    gen_label(taxonomy)

    gen_link(taxonomy)


    print("}")

if __name__ == "__main__":
       main(sys.argv[1:])

