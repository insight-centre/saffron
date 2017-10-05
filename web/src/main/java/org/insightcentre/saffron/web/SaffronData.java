package org.insightcentre.saffron.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.IndexedCorpus;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.connections.TopicTopic;

/**
 * All the data generated during the run of Saffron that is exposed by the
 * Web interface
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class SaffronData {
    private Taxonomy taxonomy;
    private List<AuthorAuthor> authorSim;
    private List<TopicTopic> topicSim;
    private List<AuthorTopic> authorTopics;
    private List<DocumentTopic> docTopics;
    private HashMap<String,Topic> topics;
    private HashMap<String,List<AuthorAuthor>> authorByAuthor1, authorByAuthor2;
    private HashMap<String,List<TopicTopic>> topicByTopic1, topicByTopic2;
    private HashMap<String,List<DocumentTopic>> docByTopic, topicByDoc;
    private HashMap<String,List<AuthorTopic>> authorByTopic, topicByAuthor;
    private List<String> topicsSorted;
    private HashMap<String,Document> corpus;

    public Taxonomy getTaxonomy() {
        return taxonomy;
    }

    public void setTaxonomy(Taxonomy taxonomy) {
        this.taxonomy = taxonomy;
    }

    public List<AuthorAuthor> getAuthorSim() {
        return authorSim;
    }

    public void setAuthorSim(List<AuthorAuthor> authorSim) {
        authorByAuthor1 = new HashMap<>();
        authorByAuthor1 = new HashMap<>();
        for(AuthorAuthor aa : authorSim) {
            if(!authorByAuthor1.containsKey(aa.author1_id))
                authorByAuthor1.put(aa.author1_id, new ArrayList<AuthorAuthor>());
            authorByAuthor1.get(aa.author1_id).add(aa);
            if(!authorByAuthor2.containsKey(aa.author2_id))
                authorByAuthor2.put(aa.author2_id, new ArrayList<AuthorAuthor>());
            authorByAuthor2.get(aa.author2_id).add(aa);
        }
        
        
        this.authorSim = authorSim;
    }
    
    public List<AuthorAuthor> getAuthorSimByAuthor1(String author1) {
        return authorByAuthor1.get(author1);
    }
    
    public List<AuthorAuthor> getAuthorSimByAuthor2(String author2) {
        return authorByAuthor2.get(author2);
    }

    public List<AuthorTopic> getAuthorTopics() {
        return authorTopics;
    }

    public void setAuthorTopics(List<AuthorTopic> authorTopics) {
        authorByTopic = new HashMap<>();
        topicByAuthor = new HashMap<>();
        for(AuthorTopic at : authorTopics) {
            if(!authorByTopic.containsKey(at.topic_id))
                authorByTopic.put(at.topic_id, new ArrayList<AuthorTopic>());
            authorByTopic.get(at.topic_id).add(at);
            if(!topicByAuthor.containsKey(at.author_id))
                topicByAuthor.put(at.author_id, new ArrayList<AuthorTopic>());
            topicByAuthor.get(at.author_id).add(at);
        }
        this.authorTopics = authorTopics;
    }
    
    public List<AuthorTopic> getAuthorByTopic(String topic) {
        return authorByTopic.get(topic);
    }
    
    public List<AuthorTopic> getTopicByAuthor(String author) {
        return topicByAuthor.get(author);
    }

    public List<DocumentTopic> getDocTopics() {
        return docTopics;
    }

    public void setDocTopics(List<DocumentTopic> docTopics) {
        docByTopic = new HashMap<>();
        topicByDoc = new HashMap<>();
        for(DocumentTopic dt : docTopics) {
            if(!docByTopic.containsKey(dt.topic_string))
                docByTopic.put(dt.topic_string, new ArrayList<DocumentTopic>());
            docByTopic.get(dt.topic_string).add(dt);
            if(!topicByDoc.containsKey(dt.document_id))
                topicByDoc.put(dt.document_id, new ArrayList<DocumentTopic>());
            topicByDoc.get(dt.document_id).add(dt);
        }
        this.docTopics = docTopics;
    }
    
    public List<Document> getDocByTopic(String topic) {
        final List<DocumentTopic> dts = docByTopic.get(topic);
        if(dts == null) {
            return Collections.EMPTY_LIST;
        } else {
            final List<Document> docs = new ArrayList<>();
            for(DocumentTopic dt : dts) {
                Document d = corpus.get(dt.document_id);
                if(d != null) {
                    docs.add(d);
                }
            }
            return docs;
        }
    }
    
    public List<DocumentTopic> getTopicByDoc(String doc) {
        return topicByDoc.get(doc);
    }

    public Collection<String> getTopTopics(int from, int to) {
        return topicsSorted.subList(from, to);
    }
    
    public Topic getTopic(String topic) {
        return topics.get(topic);
    }

    public void setTopics(Collection<Topic> _topics) {
        this.topics = new HashMap<>();
        this.topicsSorted = new ArrayList<>();
        for(Topic t : _topics) {
            this.topics.put(t.topicString, t);
            this.topicsSorted.add(t.topicString);
        }
        this.topicsSorted.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if(topics.containsKey(o1) && topics.containsKey(o2)) {
                    double wt1 = topics.get(o1).score;
                    double wt2 = topics.get(o2).score;
                    if(wt1 > wt2) {
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
        for(TopicTopic tt : topicSim) {
            if(!topicByTopic1.containsKey(tt.topic1))
                topicByTopic1.put(tt.topic1, new ArrayList<TopicTopic>());
            topicByTopic1.get(tt.topic1).add(tt);
            if(!topicByTopic2.containsKey(tt.topic2))
                topicByTopic2.put(tt.topic2, new ArrayList<TopicTopic>());
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
     * @return true if the code is loaded
     */
    public boolean isLoaded() {
        return taxonomy != null && authorSim != null && topicSim != null
                && authorTopics != null && docTopics != null && topics != null
                && corpus != null;
    }
    
    /**
     * Load the Saffron data from disk
     * @param directory The directory containing the JSON files
     * @return An initializes object
     * @throws IOException 
     */
    public static SaffronData fromDirectory(File directory) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final TypeFactory tf = mapper.getTypeFactory();
        
        File taxonomyFile = new File(directory, "taxonomy.json");
        if(!taxonomyFile.exists()) {
            throw new FileNotFoundException("Could not find taxonomy.json");
        }
        
        final SaffronData saffron = new SaffronData();
        
        saffron.setTaxonomy(mapper.readValue(taxonomyFile, Taxonomy.class));
        
        File authorSimFile = new File(directory, "author-sim.json");
        if(!authorSimFile.exists()) 
            throw new FileNotFoundException("Could not find author-sim.json");
        
        saffron.setAuthorSim((List<AuthorAuthor>)mapper.readValue(authorSimFile, 
                tf.constructCollectionType(List.class, AuthorAuthor.class)));
        
        File topicSimFile = new File(directory, "topic-sim.json");
        if(!topicSimFile.exists()) 
            throw new FileNotFoundException("Could not find topic-sim.json");
        
        saffron.setTopicSim((List<TopicTopic>)mapper.readValue(topicSimFile, 
                tf.constructCollectionType(List.class, TopicTopic.class)));
        
        File authorTopicFile = new File(directory, "author-topics.json");
        if(!authorTopicFile.exists()) 
            throw new FileNotFoundException("Could not find author-topics.json");
        
        saffron.setAuthorTopics((List<AuthorTopic>)mapper.readValue(authorTopicFile, 
                tf.constructCollectionType(List.class, AuthorTopic.class)));
        
        File docTopicsFile = new File(directory, "doc-topics.json");
        if(!docTopicsFile.exists()) 
            throw new FileNotFoundException("Could not find doc-topics.json");
        
        saffron.setDocTopics((List<DocumentTopic>)mapper.readValue(docTopicsFile, 
                tf.constructCollectionType(List.class, DocumentTopic.class)));
                
        File topicsFile = new File(directory, "topics.json");
        if(!topicsFile.exists()) 
            throw new FileNotFoundException("Could not find topics.json");
        
        saffron.setTopics((List<Topic>)mapper.readValue(topicsFile, 
                tf.constructCollectionType(List.class, Topic.class)));
        
        File corpusFile = new File(directory, "corpus.json");
        if(!corpusFile.exists())
            throw new FileNotFoundException("Could not find corpus.json");
        
        saffron.setCorpus((Corpus)mapper.readValue(corpusFile, IndexedCorpus.class));
        
        return saffron;
    }

    private void setCorpus(Corpus corpus) {
        this.corpus = new HashMap<>();
        for(Document d : corpus.getDocuments()) {
            this.corpus.put(d.id, d);
        }
    }
    
}
