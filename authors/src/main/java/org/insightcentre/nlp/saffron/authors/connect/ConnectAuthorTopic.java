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
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;

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
    
    public Collection<AuthorTerm> connectResearchers(List<Term> topics, List<DocumentTerm> documentTopics,
        Iterable<Document> documents) {
        return connectResearchers(topics, documentTopics, documents, new DefaultSaffronListener());
    }
    
    public Collection<AuthorTerm> connectResearchers(List<Term> topics, List<DocumentTerm> documentTopics,
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

        List<AuthorTerm> ats = new ArrayList<>();
        for(Map.Entry<Author, List<String>> e : author2Topic.entrySet()) {
            TreeSet<AuthorTerm> topN = new TreeSet<>(new Comparator<AuthorTerm>() {

                @Override
                public int compare(AuthorTerm arg0, AuthorTerm arg1) {
                    int i1 = Double.compare(arg0.getScore(), arg1.getScore());
                    if(i1 == 0) {
                        int i2 = arg0.getAuthorId().compareTo(arg1.getAuthorId());
                        if(i2 == 0) {
                            int i3 = arg0.getTermId().compareTo(arg1.getTermId());
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
                AuthorTerm at = new AuthorTerm();
                at.setAuthorId(e.getKey().id);
                at.setTermId(topicString);
                at.setTfIrf(tfirf.getDouble(atKey));
                at.setMatches(matches.getInt(atKey));
                at.setOccurrences(occurrences.getInt(atKey));
                at.setPaperCount(paper_count.getInt(atKey));
                at.setScore(at.getTfIrf() * at.getPaperCount());
                if(topN.size() < top_n) {
                    topN.add(at);
                } else if(topN.size() >= top_n && at.getScore() > topN.first().getScore()) {
                    topN.pollFirst();
                    topN.add(at);
                }
            }
            ats.addAll(topN);
        }

        return ats;
        
    }

    private Map<Author, List<String>> buildAuthor2Topic(List<DocumentTerm> documentTopics, Map<String, Document> docById,
            SaffronListener log) {
        Map<Author, List<String>> author2Topic = new HashMap<>();
        for(DocumentTerm dt : documentTopics) {
            Document doc = docById.get(dt.getDocumentId());
            if(doc == null) {
                log.log("Document missing: " + dt.getDocumentId());
                continue;
            }
            for(Author a : doc.authors) {
                if(!author2Topic.containsKey(a)) 
                    author2Topic.put(a, new ArrayList<String>());
                author2Topic.get(a).add(dt.getTermString());
            }
        }
        return author2Topic;
    }

    private Map<String, Term> buildTopicById(List<Term> topics) {
        Map<String, Term> topicById = new HashMap<>();
        for(Term topic : topics)
            topicById.put(topic.getString(), topic);
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
            for(String termString : e.getValue()) {
                Term t = topics.get(termString);
                if(t == null) {
                    log.log("Topic missing: " + termString);
                    continue;
                }
                AT at = new AT(a.id, termString);
                occurrences.put(at, occurrences.getInt(at) + t.getOccurrences());
                matches.put(at, matches.getInt(at) + t.getMatches());
            }
        }
    }

    private void countTfirf(List<DocumentTerm> docTopics, Map<String, Document> docById, Object2IntMap<AT> paper_count, Object2DoubleMap<AT> tfirf) {
        for(DocumentTerm dt : docTopics) {
            Document doc = docById.get(dt.getDocumentId());
            for(Author a : doc.authors) {
                AT at = new AT(a.id, dt.getTermString());
                paper_count.put(at, paper_count.getInt(at) + 1);
                if(dt.getTfIdf() != null)
                    tfirf.put(at, tfirf.getDouble(at) + dt.getTfIdf());
            }
        }
    }


}
