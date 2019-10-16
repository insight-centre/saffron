package org.insightcentre.nlp.saffron.authors.connect;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.AuthorTopicConfiguration;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class ConnectAuthorTopic {
    private static class AT {
        public final String author;
        public final String topic;

        public AT(String author, String topic) {
            this.author = author;
            this.topic = topic;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 17 * hash + Objects.hashCode(this.author);
            hash = 17 * hash + Objects.hashCode(this.topic);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final AT other = (AT) obj;
            if (!Objects.equals(this.author, other.author)) {
                return false;
            }
            if (!Objects.equals(this.topic, other.topic)) {
                return false;
            }
            return true;
        }
        
    }
    private final int top_n;

    public ConnectAuthorTopic(AuthorTopicConfiguration config) {
        this.top_n = config.topN;
    }
    
    public Collection<AuthorTopic> connectResearchers(List<Term> topics, List<DocumentTopic> documentTopics,
        Iterable<Document> documents) {
        return connectResearchers(topics, documentTopics, documents, new DefaultSaffronListener());
    }
    
    public Collection<AuthorTopic> connectResearchers(List<Term> topics, List<DocumentTopic> documentTopics,
        Iterable<Document> documents, SaffronListener log) {

        Map<String, Document>     docById      = buildDocById(documents);
        Map<Author, List<String>> author2Topic = buildAuthor2Topic(documentTopics, docById, log);
        //Map<Author, List<String>> author2Doc   = buildAuthor2Doc(documentTopics, docById);
        Map<String, Term>        topicById    = buildTopicById(topics);

        Object2IntMap<AT>     occurrences = new Object2IntOpenHashMap<>();
        Object2IntMap<AT>     matches     = new Object2IntOpenHashMap<>();
        Object2IntMap<AT>     paper_count = new Object2IntOpenHashMap<>();
        Object2DoubleMap<AT>  tfirf       = new Object2DoubleOpenHashMap<>();
        countOccurrence(author2Topic, topicById, occurrences, matches, log);
        countTfirf(documentTopics, docById, paper_count, tfirf);

        List<AuthorTopic> ats = new ArrayList<>();
        for(Map.Entry<Author, List<String>> e : author2Topic.entrySet()) {
            TreeSet<AuthorTopic> topN = new TreeSet<>(new Comparator<AuthorTopic>() {

                @Override
                public int compare(AuthorTopic arg0, AuthorTopic arg1) {
                    int i1 = Double.compare(arg0.score, arg1.score);
                    if(i1 == 0) {
                        int i2 = arg0.author_id.compareTo(arg1.author_id);
                        if(i2 == 0) {
                            int i3 = arg0.topic_id.compareTo(arg1.topic_id);
                            if(i3 == 0) {
                                return arg0.hashCode() - arg1.hashCode();
                            }
                            return i3;
                        }
                        return i2;
                    }
                    return i1;
                }
            });
        
            for(String topicString : e.getValue()) {
                AT atKey = new AT(e.getKey().id, topicString);
                AuthorTopic at = new AuthorTopic();
                at.author_id     = e.getKey().id;
                at.topic_id      = topicString;
                at.tfirf         = tfirf.getDouble(atKey);
                at.matches       = matches.getInt(atKey);
                at.occurrences   = occurrences.getInt(atKey);
                at.paper_count   = paper_count.getInt(atKey);
                at.score         = at.tfirf * at.paper_count;
                if(topN.size() < top_n) {
                    topN.add(at);
                } else if(topN.size() >= top_n && at.score > topN.first().score) {
                    topN.pollFirst();
                    topN.add(at);
                }
            }
            ats.addAll(topN);
        }

        return ats;
        
    }

    private Map<Author, List<String>> buildAuthor2Topic(List<DocumentTopic> documentTopics, Map<String, Document> docById,
            SaffronListener log) {
        Map<Author, List<String>> author2Topic = new HashMap<>();
        for(DocumentTopic dt : documentTopics) {
            Document doc = docById.get(dt.document_id);
            if(doc == null) {
                log.log("Document missing: " + dt.document_id);
                continue;
            }
            for(Author a : doc.authors) {
                if(!author2Topic.containsKey(a)) 
                    author2Topic.put(a, new ArrayList<String>());
                author2Topic.get(a).add(dt.topic_string);
            }
        }
        return author2Topic;
    }

    private Map<String, Term> buildTopicById(List<Term> topics) {
        Map<String, Term> topicById = new HashMap<>();
        for(Term topic : topics)
            topicById.put(topic.topicString, topic);
        return topicById;
    }

    private Map<String, Document> buildDocById(Iterable<Document> documents) {
        Map<String, Document> docById = new HashMap<>();
        for(Document document : documents)
            docById.put(document.id, document);
        return docById;
    }

    private void countOccurrence(Map<Author, List<String>> author2Topic, Map<String, Term> topics, Object2IntMap<AT> occurrences, 
            Object2IntMap<AT> matches, SaffronListener log) {
        for(Map.Entry<Author, List<String>> e : author2Topic.entrySet()) {
            Author a = e.getKey();
            for(String topic_string : e.getValue()) {
                Term t = topics.get(topic_string);
                if(t == null) {
                    log.log("Topic missing: " + topic_string);
                    continue;
                }
                AT at = new AT(a.id, topic_string);
                occurrences.put(at, occurrences.getInt(at) + t.occurrences);
                matches.put(at, matches.getInt(at) + t.matches);
            }
        }
    }

    private void countTfirf(List<DocumentTopic> docTopics, Map<String, Document> docById, Object2IntMap<AT> paper_count, Object2DoubleMap<AT> tfirf) {
        for(DocumentTopic dt : docTopics) {
            Document doc = docById.get(dt.document_id);
            for(Author a : doc.authors) {
                AT at = new AT(a.id, dt.topic_string);
                paper_count.put(at, paper_count.getInt(at) + 1);
                if(dt.tfidf != null)
                    tfirf.put(at, tfirf.getDouble(at) + dt.tfidf);
            }
        }
    }


}
