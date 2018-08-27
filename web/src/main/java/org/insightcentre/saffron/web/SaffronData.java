package org.insightcentre.saffron.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.IndexedCorpus;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.documentindex.DocumentSearcherFactory;

/**
 * All the data generated during the run of Saffron that is exposed by the Web
 * interface
 *
 * @author John McCrae <john@mccr.ae>
 */
public class SaffronData {

    private Taxonomy taxonomy;
    private List<AuthorAuthor> authorSim;
    private List<TopicTopic> topicSim;
    private List<AuthorTopic> authorTopics;
    private List<DocumentTopic> docTopics;
    private HashMap<String, Topic> topics;
    private HashMap<String, List<AuthorAuthor>> authorByAuthor1, authorByAuthor2;
    private HashMap<String, List<TopicTopic>> topicByTopic1, topicByTopic2;
    private HashMap<String, List<DocumentTopic>> docByTopic, topicByDoc;
    private HashMap<String, List<AuthorTopic>> authorByTopic, topicByAuthor;
    private List<String> topicsSorted;
    private HashMap<String, Document> corpus;
    private HashMap<String, List<Document>> corpusByAuthor;
    private HashMap<String, Author> authors;
    private HashMap<String, IntList> taxoMap;
    private DocumentSearcher searcher;

    public Taxonomy getTaxonomy() {
        return taxonomy;
    }

    public void setTaxonomy(Taxonomy taxonomy) {
        this.taxonomy = taxonomy;
        this.taxoMap = getTaxoLocations(taxonomy);
    }

    public List<AuthorAuthor> getAuthorSim() {
        return authorSim;
    }

    public void setAuthorSim(List<AuthorAuthor> authorSim) {
        authorByAuthor1 = new HashMap<>();
        authorByAuthor2 = new HashMap<>();
        for (AuthorAuthor aa : authorSim) {
            if (!authorByAuthor1.containsKey(aa.author1_id)) {
                authorByAuthor1.put(aa.author1_id, new ArrayList<AuthorAuthor>());
            }
            authorByAuthor1.get(aa.author1_id).add(aa);
            if (!authorByAuthor2.containsKey(aa.author2_id)) {
                authorByAuthor2.put(aa.author2_id, new ArrayList<AuthorAuthor>());
            }
            authorByAuthor2.get(aa.author2_id).add(aa);
        }

        this.authorSim = authorSim;
    }

    public List<AuthorAuthor> getAuthorSimByAuthor1(String author1) {
        List<AuthorAuthor> aas = authorByAuthor1.get(author1);
        return aas == null ? Collections.EMPTY_LIST : aas;
    }

    public List<AuthorAuthor> getAuthorSimByAuthor2(String author2) {
        List<AuthorAuthor> aas = authorByAuthor2.get(author2);
        return aas == null ? Collections.EMPTY_LIST : aas;
    }
    
    public List<Author> authorAuthorToAuthor1(List<AuthorAuthor> aas) {
        List<Author> as = new ArrayList<>();
        for(AuthorAuthor aa : aas) {
            Author a = getAuthor(aa.author1_id);
            if(a != null) {
                as.add(a);
            }
        }
        return as;
    }
    
    
    public List<Author> authorAuthorToAuthor2(List<AuthorAuthor> aas) {
        List<Author> as = new ArrayList<>();
        for(AuthorAuthor aa : aas) {
            Author a = getAuthor(aa.author2_id);
            if(a != null) {
                as.add(a);
            }
        }
        return as;
    }

    public List<AuthorTopic> getAuthorTopics() {
        return authorTopics;
    }

    public void setAuthorTopics(Collection<AuthorTopic> authorTopics) {
        authorByTopic = new HashMap<>();
        topicByAuthor = new HashMap<>();
        for (AuthorTopic at : authorTopics) {
            if (!authorByTopic.containsKey(at.topic_id)) {
                authorByTopic.put(at.topic_id, new ArrayList<AuthorTopic>());
            }
            authorByTopic.get(at.topic_id).add(at);
            if (!topicByAuthor.containsKey(at.author_id)) {
                topicByAuthor.put(at.author_id, new ArrayList<AuthorTopic>());
            }
            topicByAuthor.get(at.author_id).add(at);
        }
        this.authorTopics = new ArrayList<>(authorTopics);
    }

    public List<AuthorTopic> getAuthorByTopic(String topic) {
        List<AuthorTopic> ats = authorByTopic.get(topic);
        return ats == null ? Collections.EMPTY_LIST : ats;
    }

    public List<Author> authorTopicsToAuthors(List<AuthorTopic> ats) {
        List<Author> authors = new ArrayList<>();
        for (AuthorTopic at : ats) {
            Author a = getAuthor(at.author_id);
            if (a != null) {
                authors.add(a);
            }
        }
        return authors;
    }

    public List<AuthorTopic> getTopicByAuthor(String author) {
        List<AuthorTopic> ats = topicByAuthor.get(author);
        return ats == null ? Collections.EMPTY_LIST : ats;
    }

    public List<DocumentTopic> getDocTopics() {
        return docTopics;
    }

    public void setDocTopics(List<DocumentTopic> docTopics) {
        docByTopic = new HashMap<>();
        topicByDoc = new HashMap<>();
        for (DocumentTopic dt : docTopics) {
            if (!docByTopic.containsKey(dt.topic_string)) {
                docByTopic.put(dt.topic_string, new ArrayList<DocumentTopic>());
            }
            docByTopic.get(dt.topic_string).add(dt);
            if (!topicByDoc.containsKey(dt.document_id)) {
                topicByDoc.put(dt.document_id, new ArrayList<DocumentTopic>());
            }
            topicByDoc.get(dt.document_id).add(dt);
        }
        this.docTopics = docTopics;
    }

    public List<Document> getDocByTopic(String topic) {
        final List<DocumentTopic> dts = docByTopic.get(topic);
        if (dts == null) {
            return Collections.EMPTY_LIST;
        } else {
            final List<Document> docs = new ArrayList<>();
            for (DocumentTopic dt : dts) {
                Document d = corpus.get(dt.document_id);
                if (d != null) {
                    docs.add(d);
                }
            }
            return docs;
        }
    }

    public List<DocumentTopic> getTopicByDoc(String doc) {
        List<DocumentTopic> dts = topicByDoc.get(doc);
        if (dts == null) {
            return Collections.EMPTY_LIST;
        } else {
            return dts;
        }
    }

    public Collection<String> getTopTopics(int from, int to) {
        if(from < topicsSorted.size() && to <= topicsSorted.size()) {
            return topicsSorted.subList(from, to);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public Topic getTopic(String topic) {
        return topics.get(topic);
    }

    public void setTopics(Collection<Topic> _topics) {
        this.topics = new HashMap<>();
        this.topicsSorted = new ArrayList<>();
        for (Topic t : _topics) {
            this.topics.put(t.topicString, t);
            this.topicsSorted.add(t.topicString);
        }
        this.topicsSorted.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (topics.containsKey(o1) && topics.containsKey(o2)) {
                    double wt1 = topics.get(o1).score;
                    double wt2 = topics.get(o2).score;
                    if (wt1 > wt2) {
                        return -1;
                    } else if (wt2 > wt1) {
                        return +1;
                    }
                }
                return o1.compareTo(o2);
            }
        });
    }

    public List<TopicTopic> getTopicSim() {
        return topicSim;
    }

    public void setTopicSim(List<TopicTopic> topicSim) {
        topicByTopic1 = new HashMap<>();
        topicByTopic2 = new HashMap<>();
        for (TopicTopic tt : topicSim) {
            if (!topicByTopic1.containsKey(tt.topic1)) {
                topicByTopic1.put(tt.topic1, new ArrayList<TopicTopic>());
            }
            topicByTopic1.get(tt.topic1).add(tt);
            if (!topicByTopic2.containsKey(tt.topic2)) {
                topicByTopic2.put(tt.topic2, new ArrayList<TopicTopic>());
            }
            topicByTopic2.get(tt.topic2).add(tt);
        }
        this.topicSim = topicSim;
    }

    public List<TopicTopic> getTopicByTopic1(String topic1) {
        List<TopicTopic> tt = topicByTopic1.get(topic1);
        return tt == null ? Collections.EMPTY_LIST : tt;
    }

    public List<TopicTopic> getTopicByTopic2(String topic2) {
        List<TopicTopic> tt = topicByTopic2.get(topic2);
        return tt == null ? Collections.EMPTY_LIST : tt;
    }

    /**
     * Is the Saffron data available. If this is false the getters of this class
     * may return null;
     *
     * @return true if the code is loaded
     */
    public boolean isLoaded() {
        return taxonomy != null && authorSim != null && topicSim != null
                && authorTopics != null && docTopics != null && topics != null
                && corpus != null;
    }

    /**
     * Load the Saffron data from disk
     *
     * @param directory The directory containing the JSON files
     * @return An initializes object
     * @throws IOException
     */
    public static SaffronData fromDirectory(File directory) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final TypeFactory tf = mapper.getTypeFactory();

        File taxonomyFile = new File(directory, "taxonomy.json");
        if (!taxonomyFile.exists()) {
            throw new FileNotFoundException("Could not find taxonomy.json");
        }

        final SaffronData saffron = new SaffronData();

        saffron.setTaxonomy(mapper.readValue(taxonomyFile, Taxonomy.class));

        File authorSimFile = new File(directory, "author-sim.json");
        if (!authorSimFile.exists()) {
            throw new FileNotFoundException("Could not find author-sim.json");
        }

        saffron.setAuthorSim((List<AuthorAuthor>) mapper.readValue(authorSimFile,
                tf.constructCollectionType(List.class, AuthorAuthor.class)));

        File topicSimFile = new File(directory, "topic-sim.json");
        if (!topicSimFile.exists()) {
            throw new FileNotFoundException("Could not find topic-sim.json");
        }

        saffron.setTopicSim((List<TopicTopic>) mapper.readValue(topicSimFile,
                tf.constructCollectionType(List.class, TopicTopic.class)));

        File authorTopicFile = new File(directory, "author-topics.json");
        if (!authorTopicFile.exists()) {
            throw new FileNotFoundException("Could not find author-topics.json");
        }

        saffron.setAuthorTopics((List<AuthorTopic>) mapper.readValue(authorTopicFile,
                tf.constructCollectionType(List.class, AuthorTopic.class)));

        File docTopicsFile = new File(directory, "doc-topics.json");
        if (!docTopicsFile.exists()) {
            throw new FileNotFoundException("Could not find doc-topics.json");
        }

        saffron.setDocTopics((List<DocumentTopic>) mapper.readValue(docTopicsFile,
                tf.constructCollectionType(List.class, DocumentTopic.class)));

        File topicsFile = new File(directory, "topics.json");
        if (!topicsFile.exists()) {
            throw new FileNotFoundException("Could not find topics.json");
        }

        saffron.setTopics((List<Topic>) mapper.readValue(topicsFile,
                tf.constructCollectionType(List.class, Topic.class)));

        File corpusFile = new File(directory, "corpus.json");
        if (!corpusFile.exists()) {
            throw new FileNotFoundException("Could not find corpus.json");
        }

        saffron.setCorpus(mapper.readValue(corpusFile, IndexedCorpus.class));

        return saffron;
    }

    public void setCorpus(IndexedCorpus corpus) {
        this.corpus = new HashMap<>();
        this.corpusByAuthor = new HashMap<>();
        this.authors = new HashMap<>();
        for (Document d : corpus.getDocuments()) {
            this.corpus.put(d.id, d);
            for (Author a : d.getAuthors()) {
                if (!corpusByAuthor.containsKey(a.id)) {
                    corpusByAuthor.put(a.id, new ArrayList<Document>());
                }
                corpusByAuthor.get(a.id).add(d);
                if (!authors.containsKey(a.id)) {
                    authors.put(a.id, a);
                }
            }
        }
        try {
            this.searcher = DocumentSearcherFactory.loadSearcher(corpus, corpus.getIndex());
        } catch(IOException x) {
            System.err.println("Failed to load Lucene interface: (" + x.getClass().getName() + ") " + x.getMessage());
            x.printStackTrace();
        }
    }

    public DocumentSearcher getSearcher() {
        return searcher;
    }
    
    

    public List<Document> getDocsByAuthor(String authorId) {
        List<Document> docs = corpusByAuthor.get(authorId);
        return docs == null ? Collections.EMPTY_LIST : docs;
    }

    public Author getAuthor(String authorId) {
        return authors.get(authorId);
    }

    public Document getDoc(String docId) {
        return corpus.get(docId);
    }

    private HashMap<String, IntList> getTaxoLocations(Taxonomy t) {
        IntList il = new IntArrayList();
        HashMap<String, IntList> map = new HashMap<>();
        _getTaxoLocations(t, il, map);
        return map;
    }

    private void _getTaxoLocations(Taxonomy t, IntList il, HashMap<String, IntList> map) {
        map.put(t.root, il);
        for (int i = 0; i < t.children.size(); i++) {
            IntList il2 = new IntArrayList(il);
            il2.add(i);
            _getTaxoLocations(t.children.get(i), il2, map);
        }
    }

    private Taxonomy taxoNavigate(Taxonomy t, IntList il) {
        for (int i : il) {
            t = t.children.get(i);
        }
        return t;
    }

    public List<String> getTaxoParents(String topic_string) {
        IntList il = taxoMap.get(topic_string);
        if (il != null) {
            Taxonomy t = taxonomy;
            List<String> route = new ArrayList<>();
            for (int i : il) {
                route.add(t.root);
                t = t.children.get(i);
            }
            return route;
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public List<String> getTaxoChildren(String topic_string) {
        IntList il = taxoMap.get(topic_string);
        if (il != null) {
            Taxonomy t = taxoNavigate(taxonomy, il);
            List<String> children = new ArrayList<>();
            for (Taxonomy t2 : t.children) {
                children.add(t2.root);
            }
            return children;
        } else {
            return Collections.EMPTY_LIST;
        }
    }
}
