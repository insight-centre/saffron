Saffron 3 - Formats
===================

This document describes the formats used by Saffron 3. All formats are in Json
and we will describe only the properties each has

Corpus
------

A collection of documents. A corpus has the following properties

* `index`: A string representing the path of the index for this corpus
* `documents`: An array of documents in the corpus

### Document

A single document in a corpus. It has the following properties

* `file`: A string referring to the original version of this document on disk
* `id`: A unique string to identify the document
* `name`: The human readable name of the document
* `mime_type`: The MIME type of the document
* `authors`: An array of authors of this document
* `metadata`: An object containing any other properties

### Author

A single author of a document in a corpus.

* `id`: A unique string to identify the author (optional)
* `name`: The author's name (required)
* `variants`: An array of strings given other known variants of the name

Domain Model
------------

The set of keywords that define a domain. This object has only one property

* `terms`: An array of strings containing keywords

Topic
-----

A single topic in the corpus. Contains the following annotations

* `topic_string`: The string that names the topic (must be unique)
* `occurrences`: The total number of occurrences of a topic in the corpus
* `matches`: The number of documents in the corpus containing this topic
* `score`: The importance of the topic to this corpus (between 0 and 1)
* `mv_list`: A list of alternative (morphological variants) forms of this 
topic string
* `dbpedia_url`: The equivalent topic in DBpedia

### Morphological Variants

A variant form of a topic

* `string`: The form of this variant
* `occurrences`: The number of times this variant occurs
* `pattern`: The pattern this variant matches
* `acronym`: If this is an acronym the acronym form of the term
* `expanded_acronym`: The expanded version of this acronym

Taxonomy
--------

The topic taxonomy containing the following

* `root`: The topic string of this topic or `""` for no topic
* `children`: A list of children of this node (these are also Taxonomy objects)

Author-Author
-------------

An edge in the author-author graph

* `author1_id`: The ID of the first author
* `author2_id`: The ID of the second author
* `similarity`: The similarity score between these authors

Author-Topic
------------

An edge linking an author to a topic

* `author_id`: The ID of the author
* `topic_id`: The topic string of the topic
* `matches`: The number of times this topic is used in documents by this author
* `occurrences`: The number of documents by this author containing the topic
* `tfirf`: The Term Frequency-Inverse Research Frequency (See "Domain adaptive 
extraction of topical hierarchies for Expertise Mining" (Georgeta Bordea (2013)) 
for evaluations of different methods)
* `score`: The score of the this linking
* `researcher_score`: Score for author's ranking for this particular topic

Document-Topic
--------------

The connection between a document and a topic

* `document_id`: The ID of the document
* `topic_string`: The topic string of the topic
* `occurrences`: The number of times this topic occurs in this document
* `pattern`: The pattern used to match the topic
* `acronym`: The acronym form (if any)
* `score`: The weight of this link
* `tfidf`: Saffron internal value
* `unembedded_occ`: Saffron internal value

Topic-Topic
-----------

An edge in the topic-topic graph

* `topic1`: The first topic's topic string
* `topic2`: The second topic's topic string
* `similarity`: The similarity of the two topics


