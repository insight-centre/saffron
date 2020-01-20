Saffron 3 - Text Analysis and Insight Tool
==========================================

Saffron is a tool for providing multi-stage analysis of text corpora by means 
of state-of-the-art natural language processing technology. Saffron consists of
a set of self-contained and independent modules that individually provide 
distinct analysis of text. These modules are as follows

1. *Corpus Indexing*: Analyses raw text documents in various formats and indexes
them for later components
2. *Term Extraction*: Extracts keyphrases that are the terms of each single
document in a collection
3. *Author Consolidation*: Detects and removes name variations from the list 
of authors of each document
4. *DBpedia Lookup*: Links terms extracted from a document to URLs on the 
Semantic Web
5. *Document-term Analysis*: Analyses the terms of a document and finds the relative
importance of these terms
6. *Author-Term Analysis*: Associates authors with particular documents and 
identifies the importance of the document to each author
7. *Term Similarity*: Measures the relevance of each term to each other term
8. *Author Similarity*: Measures the relevance of each author to each other
author
9. *Taxonomy Extraction*: Organizes the terms into a single hierarchical 
graph that allows for easy browsing of the corpus and deep insights.

![Saffron Service Workflow](https://gitlab.insight-centre.org/saffron/saffron/raw/master/docs/Saffron%20Services.png)

Installation
------------

Saffron requires the use of [Maven](https://maven.apache.org/) to install and 
can be built with the following command

    mvn install

Saffron depends on some number of resources and these can be obtained with the
following script

    ./install.sh

It also integrates on Mongo DB to store the data.


Install MongoDB with the defaults set and start up. Once started, open a Mongo 
session by typing 'mongo' on a terminal. 

A database "saffron-test" will automatically be created. To rename it or to store results in different databases, edit the following file: 

    ./saffron-web.sh

and change the following line to the name wanted

    export MONGO_DB_NAME=saffron_test


To change the Mongo HOST and PORT, simply edit the same file on the following:

    export MONGO_URL=localhost
    export MONGO_PORT=27017
    
By default all results will be stored in the Mongo database. However, you can generate the JSON files with all the results by setting the following line to **true**: 
    
    export STORE_LOCAL_COPY=false

Upgrading from version 3.3 to 3.4
------

Before starting Saffron, edit the following file:

	upgrade3.3To3.4.sh
	
and change the following configurations to reflect the database you want to upgrade:
  
	export MONGO_URL=localhost
    export MONGO_PORT=27017
    export MONGO_DB_NAME=saffron_test
    
Run the script by executing:

	./upgrade3.3To3.4.sh

Running
-------


Start the Saffron Web server simply choose a directory for Saffron to create
the models and run the command as follows

    ./saffron-web.sh

Then open the following url in a browser 

    http://localhost:8080/




Command Line Interface
======================

All steps of Saffron can be executed by running the `saffron.sh` script. This 
script takes thress arguments

1. The corpus, which may be 
    1. A folder containing files in TXT, DOC or PDF
    2. A zip file containing files in TXT, DOC or PDF
    3. A Json metadata file describing the corpus (see [Saffron Formats](FORMATS.md) for more details on the format of the file)
2. The output folder to which the results are written
3. The configuration file (as described in [Saffron Formats](FORMATS.md))

For example

    ./saffron.sh corpus.json output/ config.json

The following files are generated if STORE_LOCAL_COPY was originally set to true (see [Saffron Formats](FORMATS.md) for more details on the information contained in each file)

If using the Web interface, the files will be stored in /web/data/

* `terms.json`: The terms with weights
* `doc-terms.json`: The document term map with weights
* `author-terms.json`: The connection between authors and terms
* `author-sim.json`: The author-author similarity graph
* `term-sim.json`: The term-term similarity graph
* `taxonomy.json`: The final taxonomy over the corpus
* `config.json`: The configuration file for the run
 

To create a .dot file for the generated taxonomy, you can use the following command:

    python taxonomy-to-dot.py taxonomy.json > taxonomy.dot

Configuration
=============

Full details of the configuration can be seen from the [JavaDoc](https://saffron.pages.insight-centre.org/saffron/org/insightcentre/nlp/saffron/config/package-summary.html)

Documentation
=============

The JavaDoc is available at https://saffron.pages.insight-centre.org/saffron/

The Wiki describes the web interface of the review mode https://gitlab.insight-centre.org/saffron/saffron/wikis/Review-mode

API Documentation
=================

For full API documentation, see [Saffron API Documentation](https://gitlab.insight-centre.org/saffron/saffron/blob/master/web/README.md)
