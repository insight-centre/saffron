package org.insightcentre.saffron.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.*;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.documentindex.DocumentSearcherFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * All the data generated during the run of Saffron that is exposed by the Web
 * interface
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class SaffronInMemoryDataSource implements SaffronDataSource {

    private final Map<String, SaffronDataImpl> data = new HashMap<>();

    public final String type = "file";

    private static class SaffronDataImpl {

        private Taxonomy taxonomy;
        private List<AuthorAuthor> authorSim;
        private List<TopicTopic> topicSim;
        private List<AuthorTerm> authorTopics;
        private List<DocumentTerm> docTopics;
        private HashMap<String, Term> topics;
        private HashMap<String, List<AuthorAuthor>> authorByAuthor1, authorByAuthor2;
        private HashMap<String, List<TopicTopic>> topicByTopic1, topicByTopic2;
        private HashMap<String, List<DocumentTerm>> docByTopic, topicByDoc;
        private HashMap<String, List<AuthorTerm>> authorByTopic, topicByAuthor;
        private List<String> topicsSorted;
        private HashMap<String, Document> corpus;
        private HashMap<String, List<Document>> corpusByAuthor;
        private HashMap<String, Author> authors;
        private HashMap<String, IntList> taxoMap;
        private DocumentSearcher searcher;
        private final String id;

        public SaffronDataImpl(String id) {
            this.id = id;
        }

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
            for (AuthorAuthor aa : aas) {
                Author a = getAuthor(aa.author1_id);
                if (a != null) {
                    as.add(a);
                }
            }
            return as;
        }

        public List<Author> authorAuthorToAuthor2(List<AuthorAuthor> aas) {
            List<Author> as = new ArrayList<>();
            for (AuthorAuthor aa : aas) {
                Author a = getAuthor(aa.author2_id);
                if (a != null) {
                    as.add(a);
                }
            }
            return as;
        }

        public List<AuthorTerm> getAuthorTopics() {
            return authorTopics;
        }

        public void setAuthorTopics(Collection<AuthorTerm> authorTopics) {
            authorByTopic = new HashMap<>();
            topicByAuthor = new HashMap<>();
            for (AuthorTerm at : authorTopics) {
                if (!authorByTopic.containsKey(at.getTermId())) {
                    authorByTopic.put(at.getTermId(), new ArrayList<AuthorTerm>());
                }
                authorByTopic.get(at.getTermId()).add(at);
                if (!topicByAuthor.containsKey(at.getAuthorId())) {
                    topicByAuthor.put(at.getAuthorId(), new ArrayList<AuthorTerm>());
                }
                topicByAuthor.get(at.getAuthorId()).add(at);
            }
            this.authorTopics = new ArrayList<>(authorTopics);
        }

        public List<AuthorTerm> getAuthorByTopic(String topic) {
            List<AuthorTerm> ats = authorByTopic.get(topic);
            return ats == null ? Collections.EMPTY_LIST : ats;
        }

        public List<Author> authorTopicsToAuthors(List<AuthorTerm> ats) {
            List<Author> authors = new ArrayList<>();
            for (AuthorTerm at : ats) {
                Author a = getAuthor(at.getAuthorId());
                if (a != null) {
                    authors.add(a);
                }
            }
            return authors;
        }

        public List<AuthorTerm> getTopicByAuthor(String author) {
            List<AuthorTerm> ats = topicByAuthor.get(author);
            return ats == null ? Collections.EMPTY_LIST : ats;
        }

        public List<DocumentTerm> getDocTopics() {
            return docTopics;
        }

        public void setDocTopics(List<DocumentTerm> docTopics) {
            docByTopic = new HashMap<>();
            topicByDoc = new HashMap<>();
            for (DocumentTerm dt : docTopics) {
                if (!docByTopic.containsKey(dt.getTermString())) {
                    docByTopic.put(dt.getTermString(), new ArrayList<DocumentTerm>());
                }
                docByTopic.get(dt.getTermString()).add(dt);
                if (!topicByDoc.containsKey(dt.getDocumentId())) {
                    topicByDoc.put(dt.getDocumentId(), new ArrayList<DocumentTerm>());
                }
                topicByDoc.get(dt.getDocumentId()).add(dt);
            }
            this.docTopics = docTopics;
        }

        public List<Document> getDocByTopic(String topic) {
            final List<DocumentTerm> dts = docByTopic.get(topic);
            if (dts == null) {
                return Collections.EMPTY_LIST;
            } else {
                final List<Document> docs = new ArrayList<>();
                for (DocumentTerm dt : dts) {
                    Document d = corpus.get(dt.getDocumentId());
                    if (d != null) {
                        docs.add(d);
                    }
                }
                return docs;
            }
        }

        public List<DocumentTerm> getTopicByDoc(String doc) {
            List<DocumentTerm> dts = topicByDoc.get(doc);
            if (dts == null) {
                return Collections.EMPTY_LIST;
            } else {
                return dts;
            }
        }

        public Collection<String> getTopTopics(int from, int to) {
            if (from < topicsSorted.size() && to <= topicsSorted.size()) {
                return topicsSorted.subList(from, to);
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        public Term getTopic(String topic) {
            return topics.get(topic);
        }

        public Collection<Term> getTopics() {
            return topics == null ? Collections.EMPTY_LIST : topics.values();
        }

        public void setTopics(Collection<Term> _topics) {
            this.topics = new HashMap<>();
            this.topicsSorted = new ArrayList<>();
            for (Term t : _topics) {
                this.topics.put(t.getString(), t);
                this.topicsSorted.add(t.getString());
            }
            this.topicsSorted.sort(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    if (topics.containsKey(o1) && topics.containsKey(o2)) {
                        double wt1 = topics.get(o1).getScore();
                        double wt2 = topics.get(o2).getScore();
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

        public List<TopicTopic> getTopicByTopic1(String topic1, List<String> _ignore) {
            Set<String> ignore = _ignore == null ? new HashSet<>() : new HashSet<>(_ignore);
            List<TopicTopic> tt = topicByTopic1.get(topic1);
            if (tt != null) {
                Iterator<TopicTopic> itt = tt.iterator();
                while (itt.hasNext()) {
                    if (ignore.contains(itt.next().topic2)) {
                        itt.remove();
                    }
                }
                return tt;
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        public List<TopicTopic> getTopicByTopic2(String topic2) {
            List<TopicTopic> tt = topicByTopic2.get(topic2);
            return tt == null ? Collections.EMPTY_LIST : tt;
        }

        /**
         * Is the Saffron data available. If this is false the getters of this
         * class may return null;
         *
         * @return true if the code is loaded
         */
        public boolean isLoaded() {
            return taxonomy != null && authorSim != null && topicSim != null
                    && authorTopics != null && docTopics != null && topics != null
                    && corpus != null;
        }

        public void setCorpus(Corpus corpus) {
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
            //this.searcher = corpus;
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

        public Collection<Author> getAuthors() {
            return authors.values();
        }

        public Document getDoc(String docId) {
            return corpus.get(docId);
        }

        public Collection<Document> getDocuments() {
            return corpus.values();
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

        public Taxonomy getTaxoDescendent(String topic_string) {

            Taxonomy t = taxonomy;
            return taxonomy.descendent(topic_string);
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

        public List<TopicAndScore> getTaxoChildrenScored(String topic_string) {
            IntList il = taxoMap.get(topic_string);
            if (il != null) {
                Taxonomy t = taxoNavigate(taxonomy, il);
                List<TopicAndScore> children = new ArrayList<>();
                for (Taxonomy t2 : t.children) {
                    children.add(new TopicAndScore(t2.root, t2.linkScore));
                }
                return children;
            } else {
                return Collections.EMPTY_LIST;
            }
        }

        private void updateTopicName(String topic, String newTopic, Status status) {
            Term t = topics.get(topic);
            if (t != null) {
                for (AuthorTerm at : authorTopics) {
                    if (at.getTermId().equals(topic)) {
                        at.setTermId(newTopic);
                    }
                }
                for (DocumentTerm dt : docTopics) {
                    if (dt.getTermString().equals(topic)) {
                        dt.setTermString(newTopic);
                    }
                }
                for (TopicTopic tt : topicSim) {
                    if (tt.topic1.equals(topic)) {
                        tt.topic1 = newTopic;
                    }
                    if (tt.topic2.equals(topic)) {
                        tt.topic2 = newTopic;
                    }
                }
                updateTopicNameInTaxonomy(taxonomy, topic, newTopic);
                t.setString(newTopic);
                t.setStatus(status);
            }
        }

        private void updateTopicNameInTaxonomy(Taxonomy taxo, String topic, String newTopic) {
            if(taxo.root.equals(topic)) {
                taxo.root = newTopic;
            } else {
                for(Taxonomy child : taxo.getChildren()) {
                    updateTopicNameInTaxonomy(child, topic, newTopic);
                }
            }
        }

		public void setSearcher(DocumentSearcher searcher) {
			this.searcher = searcher;			
		}
    }


    /**
     * Load the Saffron data from disk
     *
     * @param directory The directory containing the JSON files
     * @return An initializes object
     * @throws IOException
     */
    public void fromDirectory(File directory, String name) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final TypeFactory tf = mapper.getTypeFactory();

        File taxonomyFile = new File(directory, "taxonomy.json");
        if (!taxonomyFile.exists()) {
            throw new FileNotFoundException("Could not find taxonomy.json");
        }

        final SaffronDataImpl saffron = new SaffronDataImpl(name);

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

        saffron.setAuthorTopics((List<AuthorTerm>) mapper.readValue(authorTopicFile,
                tf.constructCollectionType(List.class, AuthorTerm.class)));

        File docTopicsFile = new File(directory, "doc-topics.json");
        if (!docTopicsFile.exists()) {
            throw new FileNotFoundException("Could not find doc-topics.json");
        }

        saffron.setDocTopics((List<DocumentTerm>) mapper.readValue(docTopicsFile,
                tf.constructCollectionType(List.class, DocumentTerm.class)));

        File topicsFile = new File(directory, "topics.json");
        if (!topicsFile.exists()) {
            throw new FileNotFoundException("Could not find topics.json");
        }

        saffron.setTopics((List<Term>) mapper.readValue(topicsFile,
                tf.constructCollectionType(List.class, Term.class)));

        File indexFile = new File(directory, "index");
        if (!indexFile.exists()) {
            throw new FileNotFoundException("Could not find index");
        }

        saffron.setCorpus(DocumentSearcherFactory.load(indexFile));

        this.data.put(name, saffron);
    }


    @Override
    public boolean addAuthorSimilarity(String id, Date date, List<AuthorAuthor> authorSim) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            return false;
        }
        authorSim.addAll(saffron.authorSim);
        saffron.setAuthorSim(authorSim);
        return true;
    }

    @Override
    public boolean addDocumentTopicCorrespondence(String id, Date date, List<DocumentTerm> topics) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            return false;
        }
        topics.addAll(saffron.docTopics);
        saffron.setDocTopics(topics);
        return true;
    }

    @Override
    public boolean addRun(String id, Date date, Configuration config) {
        if(!data.containsKey(id)) {
            data.put(id, new SaffronDataImpl(id));
        }
        return true;
    }

    @Override
    public boolean addTaxonomy(String id, Date date, Taxonomy graph) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            return false;
        }
        saffron.setTaxonomy(graph);
        return true;
    }

    @Override
    public boolean addTopicExtraction(String id, Date date, Set<Term> res) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            return false;
        }
        res.addAll(saffron.topics.values());
        saffron.setTopics(res);
        return true;
    }

    @Override
    public boolean addTopics(String id, Date date, List<Term> topics) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            return false;
        }
        topics.addAll(saffron.topics.values());
        saffron.setTopics(topics);
        return true;
    }

    @Override
    public boolean addTopicsSimilarity(String id, Date date, List<TopicTopic> topicSimilarity) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            return false;
        }
        topicSimilarity.addAll(saffron.topicSim);
        saffron.setTopicSim(topicSimilarity);
        return true;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void deleteRun(String runId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            return;
        }
        data.remove(runId);
    }

    @Override
    public void deleteTopic(String runId, String topic) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            return;
        }
        List<Term> topics = saffron.getTopics().stream().filter((Term t) -> !t.getString().equals(topic)).collect(Collectors.toList());
        saffron.setTopics(topics);
    }

    @Override
    public String getRun(String runId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            return "";
        }
        return saffron.id;
    }

    @Override
    public boolean updateTaxonomy(String runId, Taxonomy graph) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            return false;
        }
        saffron.setTaxonomy(graph);
        return true;
    }

    @Override
    public boolean updateTopic(String id, String topic, String status) {
        return false;
    }

    @Override
    public boolean updateTopicName(String id, String topic, String newTopic, String status) {
        return false;
    }


    @Override
    public Taxonomy getTaxonomy(String runId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxonomy();
    }

    @Override
    public List<DocumentTerm> getDocTopics(String runId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocTopics();
    }


    @Override
    public List<String> getTaxoParents(String runId, String topic_string) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoParents(topic_string);
    }

    @Override
    public List<TopicAndScore> getTaxoChildrenScored(String runId, String topic_string) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoChildrenScored(topic_string);
    }

    @Override
    public List<AuthorAuthor> getAuthorSimByAuthor1(String runId, String author1) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthorSimByAuthor1(author1);
    }

    @Override
    public List<AuthorAuthor> getAuthorSimByAuthor2(String runId, String author1) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthorSimByAuthor2(author1);
    }

    @Override
    public List<Author> authorAuthorToAuthor1(String runId, List<AuthorAuthor> aas) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.authorAuthorToAuthor1(aas);
    }

    @Override
    public List<Author> authorAuthorToAuthor2(String runId, List<AuthorAuthor> aas) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.authorAuthorToAuthor2(aas);
    }

    @Override
    public List<String> getTaxoChildren(String runId, String topic_string) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoChildren(topic_string);
    }

    @Override
    public List<TopicTopic> getTopicByTopic1(String runId, String topic1, List<String> _ignore) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopicByTopic1(topic1, _ignore);
    }

    @Override
    public List<TopicTopic> getTopicByTopic2(String runId, String topic2) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopicByTopic2(topic2);
    }

    @Override
    public List<AuthorTerm> getTopicByAuthor(String runId, String author) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopicByAuthor(author);
    }

    @Override
    public List<AuthorTerm> getAuthorByTopic(String runId, String topic) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthorByTopic(topic);
    }

    @Override
    public List<Author> authorTopicsToAuthors(String runId, List<AuthorTerm> ats) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.authorTopicsToAuthors(ats);
    }

    @Override
    public List<DocumentTerm> getTopicByDoc(String runId, String doc) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopicByDoc(doc);
    }

    @Override
    public List<Document> getDocByTopic(String runId, String topic) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocByTopic(topic);
    }

    @Override
    public Term getTopic(String runId, String topic) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopic(topic);
    }

    @Override
    public List<Document> getDocsByAuthor(String runId, String authorId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocsByAuthor(authorId);
    }

    @Override
    public Collection<String> getTopTopics(String runId, int from, int to) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopTopics(from, to);
    }

    @Override
    public Author getAuthor(String runId, String authorId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthor(authorId);
    }

    @Override
    public Document getDoc(String runId, String docId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDoc(docId);
    }

    @Override
    public DocumentSearcher getSearcher(String runId) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getSearcher();
    }

    @Override
    public void setDocTopics(String runId, List<DocumentTerm> docTopics) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setDocTopics(docTopics);
    }
    
    @Override
    public void setIndex(String runId, DocumentSearcher index) {
    	SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
    	saffron.setSearcher(index);
    }
    
    @Override
    public void setCorpus(String runId, Corpus corpus) {
    	SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setCorpus(corpus);
    }

    @Override
    public void setTopics(String runId, List<Term> _topics) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setTopics(_topics);
    }

    @Override
    public void setAuthorTopics(String runId, Collection<AuthorTerm> authorTopics) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setAuthorTopics(authorTopics);
    }

    @Override
    public void setTopicSim(String runId, List<TopicTopic> topicSim) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setTopicSim(topicSim);
    }

    @Override
    public void setAuthorSim(String runId, List<AuthorAuthor> authorSim) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setAuthorSim(authorSim);
    }

    @Override
    public void setTaxonomy(String runId, Taxonomy taxonomy) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        saffron.setTaxonomy(taxonomy);
    }

    @Override
    public void remove(String runId) {
        data.remove(runId);
    }

    @Override
    public boolean containsKey(String id) {
        return data.containsKey(id);
    }

    @Override
    public boolean isLoaded(String id) {
        SaffronDataImpl saffron = data.get(id);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.isLoaded();
    }

    @Override
    public Iterable<String> runs() {
        return data.keySet();
    }

    @Override
    public Taxonomy getTaxoDescendent(String runId, String topicString) {
        SaffronDataImpl saffron = data.get(runId);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTaxoDescendent(topicString);
    }

    @Override
    public Iterable<Document> getAllDocuments(String datasetName) {
        SaffronDataImpl saffron = data.get(datasetName);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocuments();
    }

    @Override
    public Iterable<Author> getAllAuthors(String datasetName) {
        SaffronDataImpl saffron = data.get(datasetName);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthors();
    }

    @Override
    public Iterable<Term> getAllTopics(String datasetName) {
        SaffronDataImpl saffron = data.get(datasetName);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopics();
    }

    @Override
    public Date getDate(String doc) {
        return new Date();
    }

    @Override
    public List<AuthorTerm> getAllAuthorTopics(String name) {
        SaffronDataImpl saffron = data.get(name);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getAuthorTopics();
    }

    @Override
    public Iterable<DocumentTerm> getDocTopicByTopic(String name, String topicId) {
        SaffronDataImpl saffron = data.get(name);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getDocTopics().stream().filter(dt -> dt.getTermString().equals(topicId)).collect(Collectors.toList());
    }

    @Override
    public Iterable<TopicTopic> getAllTopicSimilarities(String name) {
        SaffronDataImpl saffron = data.get(name);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopicSim();
    }

    @Override
    public Iterable<TopicTopic> getTopicByTopics(String name, String topic1, String topic2) {
        SaffronDataImpl saffron = data.get(name);
        if (saffron == null) {
            throw new NoSuchElementException("Saffron run does not exist");
        }
        return saffron.getTopicSim().stream().filter(tt -> tt.topic1.equals(topic1) && tt.topic2.equals(topic2)).collect(Collectors.toList());
    }

}
