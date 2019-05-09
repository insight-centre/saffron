Internationalization Guide - How to make Saffron work for more than English
===========================================================================


Step 1 - Collect a POS tagged corpus
------------------------------------

    apache-opennlp-1.9.1/bin/opennlp POSTaggerTrainer -lang ga -model ga-pos-merged.bin -data merged-pos.txt

Step 2 - Build a lemmatizer
---------------------------

Step 3 - Write a configuration
------------------------------

Step 4 - Build a background corpus
----------------------------------

    mvn -f term/pom.xml exec:java -Dexec.mainClass="org.insightcentre.nlp.saffron.term.tools.ProcessWikipedia" -Dexec.args="/home/jmccrae/data/wiki/gawiki-latest-pages-articles.xml.bz2 models/config-ga.json models/wiki-ga-terms.json"
