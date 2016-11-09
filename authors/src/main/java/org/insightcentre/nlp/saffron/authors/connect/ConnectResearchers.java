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
import java.util.TreeSet;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class ConnectResearchers {
    private final int top_n;

    public ConnectResearchers(int top_n) {
        this.top_n = top_n;
    }
    
    public Collection<AuthorTopic> connectResearchers(List<Topic> topics, List<DocumentTopic> documentTopics,
        List<Document> documents) {

        Map<String, Document>     docById      = buildDocById(documents);
        Map<Author, List<String>> author2Topic = buildAuthor2Topic(documentTopics, docById);
        //Map<Author, List<String>> author2Doc   = buildAuthor2Doc(documentTopics, docById);
        Map<String, Topic>        topicById    = buildTopicById(topics);

        Object2IntMap<Author>     occurrences = new Object2IntOpenHashMap<>();
        Object2IntMap<Author>     matches     = new Object2IntOpenHashMap<>();
        Object2IntMap<Author>     paper_count = new Object2IntOpenHashMap<>();
        Object2DoubleMap<Author>  tfirf       = new Object2DoubleOpenHashMap<>();
        countOccurrence(author2Topic, topicById, occurrences, matches);
        countTfirf(documentTopics, docById, paper_count, tfirf);

        TreeSet<AuthorTopic> topN = new TreeSet<>(new Comparator<AuthorTopic>() {

            @Override
            public int compare(AuthorTopic arg0, AuthorTopic arg1) {
                int i1 = Double.compare(arg0.score, arg1.score);
                if(i1 == 0) {
                    int i2 = arg0.researcher_id.compareTo(arg1.researcher_id);
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
        
        for(Map.Entry<Author, List<String>> e : author2Topic.entrySet()) {
            System.err.println(e.getKey());
            for(String topicString : e.getValue()) {
                System.err.println(topicString);
                AuthorTopic at = new AuthorTopic();
                at.researcher_id = e.getKey().name;
                at.topic_id      = topicString;
                at.tfirf         = tfirf.getDouble(e.getKey());
                at.matches       = matches.getInt(e.getKey());
                at.occurrences   = occurrences.getInt(e.getKey());
                at.paper_count   = paper_count.getInt(e.getKey());
                at.score         = at.tfirf * at.paper_count;
                if(topN.size() < top_n) {
                    topN.add(at);
                } else if(topN.size() >= top_n && at.score > topN.first().score) {
                    topN.pollFirst();
                    topN.add(at);
                }
            }
        }

        return topN;
        
    }

    private Map<Author, List<String>> buildAuthor2Topic(List<DocumentTopic> documentTopics, Map<String, Document> docById) {
        Map<Author, List<String>> author2Topic = new HashMap<>();
        for(DocumentTopic dt : documentTopics) {
            Document doc = docById.get(dt.document_id);
            if(doc == null) {
                System.err.println("Document missing: " + dt.document_id);
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

    private Map<String, Topic> buildTopicById(List<Topic> topics) {
        Map<String, Topic> topicById = new HashMap<>();
        for(Topic topic : topics)
            topicById.put(topic.topicString, topic);
        return topicById;
    }

    private Map<String, Document> buildDocById(List<Document> documents) {
        Map<String, Document> docById = new HashMap<>();
        for(Document document : documents)
            docById.put(document.id, document);
        return docById;
    }

    private void countOccurrence(Map<Author, List<String>> author2Topic, Map<String, Topic> topics, Object2IntMap<Author> occurrences, Object2IntMap<Author> matches) {
        for(Map.Entry<Author, List<String>> e : author2Topic.entrySet()) {
            Author a = e.getKey();
            for(String topic_string : e.getValue()) {
                Topic t = topics.get(topic_string);
                if(t == null) {
                    System.err.println("Topic missing: " + topic_string);
                    continue;
                }
                occurrences.put(a, occurrences.getInt(a) + t.occurrences);
                matches.put(a, matches.getInt(a) + t.matches);
            }
        }
    }

    private void countTfirf(List<DocumentTopic> docTopics, Map<String, Document> docById, Object2IntMap<Author> paper_count, Object2DoubleMap<Author> tfirf) {
        for(DocumentTopic dt : docTopics) {
            Document doc = docById.get(dt.document_id);
            for(Author a : doc.authors) {
                paper_count.put(a, paper_count.getInt(a) + 1);
                if(dt.tfidf != null)
                    tfirf.put(a, tfirf.getDouble(a) + dt.tfidf);
            }
        }
    }


}
