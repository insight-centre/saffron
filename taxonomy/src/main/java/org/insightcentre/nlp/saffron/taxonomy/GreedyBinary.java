package org.insightcentre.nlp.saffron.taxonomy;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class GreedyBinary {
    private final static class StringPair {
        public final String s1, s2;

        public StringPair(String s1, String s2) {
            this.s1 = s1;
            this.s2 = s2;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + Objects.hashCode(this.s1);
            hash = 67 * hash + Objects.hashCode(this.s2);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StringPair other = (StringPair) obj;
            if (!Objects.equals(this.s1, other.s1)) {
                return false;
            }
            if (!Objects.equals(this.s2, other.s2)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "StringPair{" + "s1=" + s1 + ", s2=" + s2 + '}';
        }
        
    }
    
    private final double alpha, beta, epsilon;

    public GreedyBinary(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
        this.epsilon = 1e-5;
    }

    public GreedyBinary(double alpha, double beta, double epsilon) {
        this.alpha = alpha;
        this.beta = beta;
        this.epsilon = epsilon;
    }
    
    

    private static final class Precomputed {
        public Object2IntMap<String> occ = new Object2IntOpenHashMap<>();
        public Object2IntMap<StringPair> coocc = new Object2IntOpenHashMap<>();
        int totalDocs;
    }
    
    public Taxonomy optimisedSimilarityGraph(List<DocumentTopic> docTopics, Map<String, org.insightcentre.nlp.saffron.data.Topic> topics) {
        if (topics.isEmpty()) {
            throw new IllegalArgumentException("No topics!");
        }
        final Precomputed precomputed = precompute(docTopics);

        final Topic rootTopic = getRootTopic(topics);
        final Set<Topic> topics2 = new HashSet<>(topics.values());
        topics2.remove(rootTopic);

        return split(rootTopic, topics2, precomputed);
    }

    // Very simple strategy: just use the highest score from term extraction
    private Topic getRootTopic(Map<String, Topic> topics) {
        double best = -Double.MAX_VALUE;
        Topic bestTopic = null;
        for (Topic t : topics.values()) {
            if (t.score > best) {
                best = t.score;
                bestTopic = t;
            }
        }
        return bestTopic;
    }

    private static Precomputed precompute(List<DocumentTopic> docTopics) {
        Map<String, Set<String>> docs = new HashMap<>();
        Precomputed result = new Precomputed();
        for(DocumentTopic dt : docTopics) {
            if(docs.containsKey(dt.document_id)) {
                if(!docs.get(dt.document_id).contains(dt.topic_string)) {
                    result.occ.put(dt.topic_string, result.occ.getInt(dt.topic_string) + 1);
                    docs.get(dt.document_id).add(dt.topic_string);
                }
            } else {
                result.totalDocs++;
                docs.put(dt.document_id, new HashSet<String>());
                docs.get(dt.document_id).add(dt.topic_string);
                result.occ.put(dt.topic_string, result.occ.getInt(dt.topic_string) + 1);
            }
        }
        for(Map.Entry<String, Set<String>> e : docs.entrySet()) {
            for(String t1 : e.getValue()) {
                for(String t2: e.getValue()) {
                    if(!t1.endsWith(t2)) {
                        StringPair sp = new StringPair(t1, t2);
                        result.coocc.put(sp, result.coocc.getInt(sp) + 1);
                    }
                }
            }
        }
        
        return result;
        
    }
    
    private Taxonomy split(Topic rootTopic, Set<Topic> candidates, Precomputed precomputed) {
        if (candidates.isEmpty()) {
            return new Taxonomy(rootTopic.topicString, Collections.EMPTY_LIST);
        }
        if (candidates.size() == 1) {
            return new Taxonomy(rootTopic.topicString, Arrays.asList(new Taxonomy(candidates.iterator().next().topicString, Collections.EMPTY_LIST)));
        }
        Topic bestLeft = null, bestRight = null;
        double bestScore = -Double.MAX_VALUE;
        for (Topic t1 : candidates) {
            for (Topic t2 : candidates) {
                if (t1.topicString.compareTo(t2.topicString) > 0) {
                    final double score = 0.5 * alpha * pmi(rootTopic, t1, precomputed)
                            + 0.5 * alpha * pmi(rootTopic, t2, precomputed)
                            - beta * pmi(t1, t2, precomputed);
                    if (score > bestScore) {
                        bestScore = score;
                        bestLeft = t1;
                        bestRight = t2;
                    }
                }
            }
        }
        if(bestLeft == null) {
            throw new RuntimeException("Should not occur");
        }
        Set<Topic> leftTopics = new HashSet<>();
        Set<Topic> rightTopics = new HashSet<>();
        candidates.remove(bestLeft);
        candidates.remove(bestRight);
        for(Topic t : candidates) {
            double leftPmi = pmi(t, bestLeft, precomputed);
            double rightPmi = pmi(t, bestRight, precomputed);
            if(leftPmi > rightPmi) {
                leftTopics.add(t);                        
            } else {
                rightTopics.add(t);
            }
        }
        //System.err.printf("[%s] -> [%s] [%s]\n", rootTopic.topicString, bestLeft.topicString, bestRight.topicString);
        return new Taxonomy(rootTopic.topicString, Arrays.asList(
                split(bestLeft, leftTopics, precomputed),
                split(bestRight, rightTopics, precomputed)
        ));

    }
    
    private double pmi(Topic t1, Topic t2, Precomputed precomputed) {
        double pxy = ((double)precomputed.coocc.getInt(new StringPair(t1.topicString, t2.topicString)) + epsilon) / (precomputed.totalDocs + epsilon);
        double px = ((double)precomputed.occ.getInt(t1.topicString) + epsilon) / (precomputed.totalDocs + epsilon);
        double py = ((double)precomputed.occ.getInt(t2.topicString) + epsilon) / (precomputed.totalDocs + epsilon);
        
        return pxy * Math.log(pxy / px / py);
    }
    
}
