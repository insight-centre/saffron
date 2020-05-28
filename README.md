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

<img src="https://gitlab.insight-centre.org/saffron/saffron/raw/master/docs/Saffron%20Services.png" alt="Saffron Service Workflow" width="400"/>

Installation
------------

Saffron requires the use of [Apache Maven](https://maven.apache.org/) to run.
If using the Web Interface [MongoDB](https://docs.mongodb.com/manual/) will also be needed to store the data.
Both need to be installed before trying to run Saffron:
* [Install Maven](https://maven.apache.org/install.html)
* [Install MongoDb](https://docs.mongodb.com/manual/administration/install-community/) (use the default settings)


1.  Run the following script to obtain the resources on which Saffron depends:

    ./install.sh


1.  To build the dependencies Saffron requires, use the following command:

    mvn clean install


Running
-------


### Using the Web Interface


1.  Start a MongoDB session by typing 'mongod' on a terminal. MongoDB has to be running for Saffron to operate.

2.  The file saffron-web.sh contains some information, such as the name given to the database, the host and port it will run on.
    If you need to change the database name (default to saffron_test) edit the file saffron-web.sh and change the line:
        export MONGO_DB_NAME=saffron_test

    To change the Mongo HOST and PORT, simply edit the same file on the following:

        export MONGO_URL=localhost
        export MONGO_PORT=27017

    By default all results will be stored in the Mongo database, and the JSON files will be generated in /web/data/. However, you can change it to store in in the Mondo database only by setting the following line to **false**:

        export STORE_LOCAL_COPY=true



1.  To start the Saffron Web server, simply choose a directory for Saffron to create
the models and run the command as follows

    ./saffron-web.sh


1.  Then open the following url in a browser to access the Web Interface

    http://localhost:8080/

See the [Wiki](https://gitlab.insight-centre.org/saffron/saffron/-/wikis/WEB-INTERFACE-USER-MANUAL) for more details on how to use the Web Interface

### Using the Command Line


All steps of Saffron can be executed by running the `saffron.sh` script, without using the Web Interface. This
script takes three arguments

1. The corpus, which may be
    1. A folder containing files in TXT, DOC or PDF
    2. A zip file containing files in TXT, DOC or PDF
    3. A Json metadata file describing the corpus (see [Saffron Formats](FORMATS.md) for more details on the format of the file)
2. The output folder to which the results are written
3. The configuration file (as described in [Saffron Formats](FORMATS.md))

For example

    ./saffron.sh corpus.json output/ config.json


Results
-------

If the Web Interface is used and STORE_LOCAL_COPY was set to true, or Saffron was used with the command line, the following files are generated and stored in /web/data/.
(see [Saffron Formats](FORMATS.md) for more details on each file)

* `terms.json`: The terms with weights
* `doc-terms.json`: The document term map with weights
* `author-terms.json`: The connection between authors and terms
* `author-sim.json`: The author-author similarity graph
* `term-sim.json`: The term-term similarity graph
* `taxonomy.json`: The final taxonomy over the corpus
* `config.json`: The configuration file for the run


To create a .dot file for the generated taxonomy, you can use the following command:

    python taxonomy-to-dot.py taxonomy.json > taxonomy.dot


Upgrading from version 3.3 to 3.4
------
If you have results fron using Saffron version 3.3, you will need to do the following to make it compatible with the version 3.4

Before starting Saffron, edit the following file:

	upgrade3.3To3.4.sh

and change the following configurations to reflect the database you want to upgrade:

	export MONGO_URL=localhost
    export MONGO_PORT=27017
    export MONGO_DB_NAME=saffron_test

Run the script by executing:

	./upgrade3.3To3.4.sh



Configuration
=============

Full details of the configuration can be seen from the [JavaDoc](https://saffron.pages.insight-centre.org/saffron/org/insightcentre/nlp/saffron/config/package-summary.html)

Documentation
=============

The JavaDoc is available at (https://saffron.pages.insight-centre.org/saffron/)

The Wiki gives more details on how the approach of Saffronto use the web interface  (https://gitlab.insight-centre.org/saffron/saffron/-/wikis/home)

FORMATS.md gives an exhaustive description of the input and output generated by Saffron

API Documentation
=================

For full API documentation, see [Saffron API Documentation](https://gitlab.insight-centre.org/saffron/saffron/blob/master/web/README.md)
