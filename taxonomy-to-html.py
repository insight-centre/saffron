import json
import sys
import re

def print_taxonomy(tax, topic2docs, docs, indent=2):
    ident = re.sub("\\W","_",tax["root"])
    print(("  " * indent) + """<div class="panel panel-primary">""")
    if tax["children"]:
        print(("  " * indent) + """  <div class="panel-heading" onclick="$('#""" + ident + """').slideToggle()">""" + tax["root"] + """ <span class="pull-right">+</span></div>""")
        print(("  " * indent) + """  <div class="panel-body" style="display:none;" id=""" + "\"" + ident + "\"" + """ ">""")
    else:
        print(("  " * indent) + """  <div class="panel-heading">""" + tax["root"] + """</div>""")
        print(("  " * indent) + """  <div class="panel-body" id=""" + "\"" + ident + "\"" + """ ">""")

    print(("  " * indent) + """  <ul>""")
    for doc in topic2docs[tax["root"]]:
        print("<li>" + docs[doc]["name"] + " - " + ",".join([a["name"] for a in docs[doc]["authors"]])+ "</li>")
    print(("  " * indent) + """  </ul>""")

    if tax["children"]:
        for child in tax["children"]:
            print_taxonomy(child, topic2docs, docs, indent + 1)
    print(("  " * indent) + """  </div>""")


    print(("  " * indent) + """</div>""")
    
    


def main(args):
    taxonomy = json.load(open(args[0]))
    doc_topics = json.load(open(args[1]))
    corpus = json.load(open(args[2]))
    topic2docs = {}
    for dt in doc_topics:
        if dt["topic_string"] in topic2docs:
            topic2docs[dt["topic_string"]].add(dt["document_id"])
        else:
            topic2docs[dt["topic_string"]] = set([dt["document_id"]])

    docs = {doc["id"]: doc for doc in corpus["documents"]}
    print("""<html>
  <head>
    <title>Saffron Taxonomy """ + args[3] + """</title>
    <script
      src="https://code.jquery.com/jquery-3.2.1.min.js"
        integrity="sha256-hwg4gsxgFZhOsEEamdOYGBf13FyQuiTwlAQgxVSNgt4="
          crossorigin="anonymous"></script>
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>
    <style>
        .panel-heading { text-transform: capitalize; }
    </style>
  </head>
  <body>
  <div class="container">
  <div class="row">
    <h1>""" + args[3] + """</h1>""")
    print_taxonomy(taxonomy, topic2docs, docs)
    print("""</div></div></body></html>""")
    

if __name__ == "__main__":
       main(sys.argv[1:])
