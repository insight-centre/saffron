package org.insightcentre.nlp.saffron.taxonomy.supervised;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
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
 * Develop a taxonomy by greedily splitting the set of topics
 * 
 * @author John McCrae
 */
public class HeadAndBag {
    private final SupervisedTaxo classifier;
    private final double splitPenalty;

    public HeadAndBag(SupervisedTaxo classifier, double splitPenalty) {
        this.classifier = classifier;
        this.splitPenalty = splitPenalty;
    }

    
    public Taxonomy extractTaxonomy(Set<String> topics) {
     
        HashMap<String, List<ScoredString>> scoresByChild = new HashMap<>();
        for(String t1 : topics) {
            List<ScoredString> list = new ArrayList<>();
            for(String t2 : topics) {
                if(!t1.equals(t2)) {
                    double score = classifier.predict(t2, t1);
                    if(score > 0)
                        list.add(new ScoredString(t2, score));
                }
            }
            list.sort(new Comparator<ScoredString>() {
                @Override
                public int compare(ScoredString o1, ScoredString o2) {
                    int i1 = Double.compare(o1.score, o2.score);
                    if(i1 != 0)
                        return -i1;
                    return o1.s.compareTo(o2.s);
                }
            });
            scoresByChild.put(t1, list);
        }
        
        Set<String> topics2 = new HashSet<>(topics);
        return buildTaxonomy(topics2, scoresByChild);
    }
    
    private Taxonomy buildTaxonomy(Set<String> topics, Map<String, List<ScoredString>> scoresByChild) {
        HeadResult head = findHead(topics, scoresByChild);
        topics.remove(head.head);
        List<Set<String>> bags = buildBags(topics, scoresByChild);
        Taxonomy taxonomy = new Taxonomy(head.head, head.score, new ArrayList<Taxonomy>());
        for(Set<String> bag : bags) {
            taxonomy.children.add(buildTaxonomy(bag, scoresByChild));
        }
        return taxonomy;
    }
    
    private List<Set<String>> buildBags(Set<String> topics, Map<String, List<ScoredString>> scoresByChild) {
        List<Set<String>> bags = new ArrayList<>();
        for(String t : topics) {
            List<ScoredString> scores = scoresByChild.get(t);
            double[] costs = new double[bags.size() + 1];
            costs[0] = bags.size() * splitPenalty;
            for(ScoredString ss : scores) {
                int i = 1;
                for(Set<String> bag : bags) {
                    if(bag.contains(ss.s)) {
                        costs[i] += ss.score;
                    }
                    i++;
                }
            }
            double costMax = Double.POSITIVE_INFINITY;
            int maxi = -1;
            for(int i = 0; i < costs.length; i++) {
                if(costs[i] < costMax) {
                    maxi = i;
                    costMax = costs[i];
                }
            }
            if(maxi == 0) {
                Set<String> bag = new HashSet<>();
                bag.add(t);
                bags.add(bag);
            } else {
                bags.get(maxi - 1).add(t);
            }
        }
        return bags;
    }
    
    private class HeadResult {
        String head;
        double score;

        public HeadResult(String head, double score) {
            this.head = head;
            this.score = score;
        }
        
    }
    
    private HeadResult findHead(Set<String> topics, Map<String, List<ScoredString>> scoresByChild) {
        Object2DoubleMap<String> scores = new Object2DoubleOpenHashMap<>();
        for(Map.Entry<String, List<ScoredString>> e : scoresByChild.entrySet()) {
            for(ScoredString ss : e.getValue()) {
                if(topics.contains(ss.s))
                    scores.put(ss.s, scores.getDouble(ss.s) + ss.score);
            }
        }
        double costMax = Double.NEGATIVE_INFINITY;
        String head = null;
        for(String t : topics) {
            double score = scores.getDouble(t);
            if(score > costMax) {
                costMax = score;
                head = t;
            }
        }
        return new HeadResult(head, costMax);
    }
    
    private static final class ScoredString {
        public final String s;
        public final double score;

        public ScoredString(String s, double score) {
            this.s = s;
            this.score = score;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 47 * hash + Objects.hashCode(this.s);
            hash = 47 * hash + (int) (Double.doubleToLongBits(this.score) ^ (Double.doubleToLongBits(this.score) >>> 32));
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
            final ScoredString other = (ScoredString) obj;
            if (Double.doubleToLongBits(this.score) != Double.doubleToLongBits(other.score)) {
                return false;
            }
            if (!Objects.equals(this.s, other.s)) {
                return false;
            }
            return true;
        }
        
        
    }
}
