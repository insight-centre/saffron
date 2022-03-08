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
3. *Concept Consolidation*: Detects and removes variations from the list
of terms of each document
4. *Author Consolidation*: Detects and removes name variations from the list
of authors of each document
5. *DBpedia Lookup*: Links terms extracted from a document to URLs on the
Semantic Web
6. *Author Connection*: Associates authors with terms from the documents and
identifies the importance of the term to each author
7. *Term Similarity*: Measures the relevance of each term to each other term
8. *Author Similarity*: Measures the relevance of each author to each other
author
9. *Taxonomy Extraction*: Organizes the terms into a single hierarchical
graph that allows for easy browsing of the corpus and deep insights.
10. *RDF Extraction*: Creates a knowledge graph (note that this process can take some time)


<img src="https://github.com/insight-centre/saffron/blob/master/docs/Saffron%20Services.png" alt="Saffron Service Workflow" width="400"/>

More detailed information on the configuration of Saffron can be found [here](https://github.com/insight-centre/saffron/wiki).

Prerequisites
------------

### Java JDK 1.7 or above
Make sure you have [Java](https://www.oracle.com/java/technologies/downloads/)
```
java -version
```

#### Maven
Saffron uses [Apache Maven](https://maven.apache.org/) to run, it should therefore be installed (the recommended version is [Maven 3.5.4](https://maven.apache.org/docs/3.5.4/release-notes.html)).

Maven can be obtained through package managers such as APT or may be installed
 as follows:

1. Download Maven
```
wget -O- https://archive.apache.org/dist/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz | sudo tar -xzv
```
2. Locate and add Maven's bin directory path to the PATH variable in your ~/.bash_profile

```
export PATH="$HOME/apache-maven-3.5.4/bin:$PATH"

source ~/.bash_profile
```

3. Check that Maven is installed
```
mvn -version
```

#### MongoDB (optional)

 If using the Web Interface [MongoDB](https://docs.mongodb.com/manual/) can be used to store the data.
 If so, install [MongoDb](https://docs.mongodb.com/manual/administration/install-community/) using the  using the default settings.

#### 3GB Memory

 Saffron use deep learning models for some of its modules, and these files can be quite big.  You will need about **3 GB** of free hard disk memory to install Saffron and its models.

Installation
------------

To install Saffron:

1. clone the gitlab repository
```
git clone https://gitlab.insight-centre.org/saffron/saffron-os.git ~/saffron-os
```

2. Move to the project directory, and install the maven dependencies
```
cd ~/saffron-os
mvn clean install
```

3. Run the whole pipeline of Saffron using the Command Line method below.

**Note1**: Running the pipeline the first time will download all the models needed by Saffron to work, so the first time it will take longer 

Running
-------

### Using the Command Line


All steps of Saffron can be executed by running the `saffron.sh` script, without using the Web Interface. This
script takes three arguments

1. The corpus, which may be
    1. A folder containing files in TXT, DOC or PDF
    2. A zip, tar.gz or .tgz file containing files in TXT, DOC or PDF
    3. A Json metadata file describing the corpus (see [Saffron Formats](FORMATS.md) for more details on the format of the file)
    4. A Url (to crawl the corpus from) 
2. The output folder to which the results are written
3. The configuration file (as described in [Saffron Formats](FORMATS.md)). 

In addition, some optional arguments can be specified:

                          
  `-c <RunConfiguration$CorpusMethod>`:  The type of corpus to be used. One of _CRAWL_, _JSON_, _ZIP_ (for the corpus as a zip, tar.gz or .tgz file containing files in TXT, DOC or PDF ). _Default to JSON_                         

  `-i <File>     `                    :  The inclusion list of terms and relations (in JSON)                                
  `-k <RunConfiguration$KGMethod>`    :  The method for knowledge graph construction: ie. whether to generate a taxonomy or a knowledge graph. Choose between TAXO and KG. _Default to KG_         
  `--domain`                          :  Limit the crawl to the domain of the seed URL (if using the CRAWL option for the corpus)  

  `--max-pages <Integer> `            :  The maximum number of pages to extract when crawling (if using the CRAWL option for the corpus)                                 
  `--name <String>`                   :  The name of the run 



For example

    ./saffron.sh ~/corpus.zip ~/output/ config.json -k TAXO -c ZIP


**More detail on Saffron**, ie. how to install it, how to configure the different features, and the approaches it is based on can be found in the Wiki (https://github.com/insight-centre/saffron/wiki)



### Using the Web Interface


1. (optional) If you choose to use Mongo, install [MongoDb](https://docs.mongodb.com/manual/administration/install-community/) (use the default settings)

    And start a session by typing 'mongod' on a terminal. MongoDB has to be running.

    The file saffron-web.sh contains some information, such as the name given to the database, the host and port it will run on. If using Mongo, you need to change the database name (default to saffron_test) edit the file saffron-web.sh and change the line:
        export MONGO_DB_NAME=saffron_test

    To change the Mongo HOST and PORT, simply edit the same file on the following:

        export MONGO_URL=localhost
        export MONGO_PORT=27017

3.  **All results (output JSON files) will be generated in `./web/data/`**. However, you can change it to store in in the Mondo database only by setting the following line to **false**:

        export STORE_LOCAL_COPY=true



1.  To start the Saffron Web server, simply choose a directory for Saffron to create
the models and run the command as follows

    ./saffron-web.sh


1.  Then open the following url in a browser to access the Web Interface

    http://localhost:8080/

See the [Wiki](https://github.com/insight-centre/saffron/wiki/2.1.-Web-Interface) for more details on how to use the Web Interface



**FORMATS.md** gives the description of the input files needed to run Saffron and output files generated by Saffron



### Using Docker (one module at a time or as a pipeline)

It is possible to run each module of Saffron using Docker (note that some modules depend on other modules).

A comprehensive documentation on how to do this is available in `./docs/Saffron_Docker_Documentation.pdf`


Results
-------

If the Web Interface is used and STORE_LOCAL_COPY set to true, the output files are generated and stored in **./web/data/**.
Saffron  generates the following files
(see [Saffron Formats](FORMATS.md) for more details on each file)

* `terms.json`: The terms with weights
* `doc-terms.json`: The document term map with weights
* `author-terms.json`: The connection between authors and terms
* `author-sim.json`: The author-author similarity graph
* `term-sim.json`: The term-term similarity graph
* `taxonomy.json`: The final taxonomy over the corpus as JSON (if option chosen)
* `taxonomy.json`: The final taxonomy over the corpus as RDF (if option chosen)
* `rdf.json`: The final knowledge graph over the corpus as JSON (if option chosen)
* `rdf.json`: The final knowledge graph over the corpus as RDF (if option chosen)
* `config.json`: The configuration file for the run


To create a .dot file for the generated taxonomy, you can use the following command:

    python taxonomy-to-dot.py taxonomy.json > taxonomy.dot

Developer Guide
---------------

Check [here](https://docs.google.com/document/d/1ebyiSYCL9mG31MUnMGXGiCfaUgLiw39ButFjpho_LXA/edit#heading=h.l0sbpcm9d6qq) to see how you can contribute to Saffron

**Important**:

If making any change that impact either the format of **input files**, the format of the **output files**, the format of the **configuration file**, or the **command** to run Saffron, please update the following files accordingly:
- `README.md`
- Files within the `examples` folder (and sub-folders)
- `FORMAT.md`

and inform the development team of Saffron.


Java configuration
=================

The Java classes describing the configuration can be found here [JavaDoc](https://saffron.pages.insight-centre.org/saffron-os/org/insightcentre/nlp/saffron/config/package-summary.html)


API Documentation
=================

For the API documentation, see [Saffron API Documentation](https://github.com/insight-centre/saffron/blob/master/web/README.md)
