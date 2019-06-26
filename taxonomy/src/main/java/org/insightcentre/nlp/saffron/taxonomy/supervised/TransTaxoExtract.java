package org.insightcentre.nlp.saffron.taxonomy.supervised;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Taxonomy;

/**
 *
 * @author John McCrae
 */
public class TransTaxoExtract {

    private final SupervisedTaxo classifier;
    private final double discount;

    public TransTaxoExtract(SupervisedTaxo classifier, double discount) {
        this.classifier = classifier;
        this.discount = discount;
    }

    public Taxonomy extractTaxonomy(Set<String> topics) {
        Object2DoubleMap<StringPair> scores = new Object2DoubleOpenHashMap<>();
        for (String topic1 : topics) {
            for (String topic2 : topics) {
                if (!topic1.equals(topic2)) {
                    double score = classifier.predict(topic1, topic2) - discount;
                    scores.put(new StringPair(topic1, topic2), score);

                }
            }
        }

        // Loop is to find the highest score
        TaxoBuilder taxo = new TaxoBuilder();
        while (true) {
            StringPair best = findBest(scores);
            if (best == null || Double.isInfinite(scores.getDouble(best))) {
                break;
            }
            if (taxo.add(best)) {
                rescore(scores, best, taxo);
            } 
            scores.remove(best);
        }
        return taxo.toTaxo(scores);
    }

    private StringPair findBest(Object2DoubleMap<StringPair> scores) {
        double bestScore = Double.NEGATIVE_INFINITY;
        StringPair best = null;
        for (Object2DoubleMap.Entry<StringPair> score : scores.object2DoubleEntrySet()) {
            if (score.getDoubleValue() > bestScore) {
                bestScore = score.getDoubleValue();
                best = score.getKey();
            }
        }
        return best;
    }

    private void rescore(Object2DoubleMap<StringPair> scores, StringPair best, TaxoBuilder taxo) {
        //double parentCost = taxo.parentCost(best.top, scores);
        //double thisCost = scores.getDouble(best);
        //double childCost = taxo.childCost(best.bottom, scores);
        Set<String> parents = new HashSet<>();
        Set<String> children = new HashSet<>();
        taxo.parents(best.top, parents);
        taxo.children(best.bottom, children);

        for (Object2DoubleMap.Entry<StringPair> s : scores.object2DoubleEntrySet()) {
            if (Double.isFinite(s.getDoubleValue())) {
                if (parents.contains(s.getKey().bottom) && children.contains(s.getKey().top)) {
                    s.setValue(Double.NEGATIVE_INFINITY);
                } else if (parents.contains(s.getKey().bottom)) {
                    double d = s.getDoubleValue();
                    d += scores.getDouble(new StringPair(s.getKey().top, best.bottom));
                    s.setValue(d);
                } else if (children.contains(s.getKey().top)) {
                    double d = s.getDoubleValue();
                    d += scores.getDouble(new StringPair(best.top, s.getKey().bottom));
                    s.setValue(d);
                }
            }
        }
    }

    private static class TaxoBuilder {

        Map<String, String> parents = new HashMap<>();
        Map<String, Set<String>> children = new HashMap<>();
        List<Set<String>> cliques = new ArrayList<>();

        public boolean add(StringPair s) {
            if(parents.containsKey(s.bottom))
                return false;
            Set<String> bottomClique = null;
            Set<String> topClique = null;
            for (Set<String> clique : cliques) {
                if (clique.contains(s.bottom)) {
                    if (clique.contains(s.top)) {
                        return false;
                    } else {
                        bottomClique = clique;
                    }
                }
                if (clique.contains(s.top)) {
                    topClique = clique;
                }
            }
            if (bottomClique != null) {
                if (topClique != null) {
                    cliques.remove(topClique);
                    bottomClique.addAll(topClique);
                } else {
                    bottomClique.add(s.top);
                }
            } else if (topClique != null) {
                topClique.add(s.bottom);
            } else {
                HashSet<String> newClique = new HashSet<>();
                newClique.add(s.top);
                newClique.add(s.bottom);
                cliques.add(newClique);
            }
            parents.put(s.bottom, s.top);
            if (!children.containsKey(s.top)) {
                children.put(s.top, new HashSet<String>());
            }
            children.get(s.top).add(s.bottom);
            return true;
        }

        public double parentCost(String top, Object2DoubleMap<StringPair> scores) {
            String parent = parents.get(top);
            if (parent != null) {
                return scores.getDouble(new StringPair(parent, top))
                        + parentCost(parent, scores);
            } else {
                return 0.0;
            }
        }

        public void parents(String top, Set<String> result) {
            String parent = parents.get(top);
            if (parent != null) {
                result.add(parent);
                parents(parent, result);
            }

        }

        public double childCost(String bottom, Object2DoubleMap<StringPair> scores) {
            Set<String> child = children.get(bottom);
            if (child != null) {
                double score = 0.0;
                for (String c : child) {
                    score += scores.getDouble(new StringPair(bottom, c));
                    score += childCost(c, scores);
                }
                return score;
            } else {
                return 0.0;
            }
        }

        public void children(String bottom, Set<String> result) {
            Set<String> child = children.get(bottom);
            if (child != null) {
                for (String c : child) {
                    result.add(c);
                    children(c, result);
                }
            }
        }

        public Taxonomy toTaxo(Object2DoubleMap<StringPair> scores) {
            String root = parents.keySet().iterator().next();
            while (parents.containsKey(root)) {
                root = parents.get(root);
            }
            return _buildTaxonomy(root, null, scores);
        }

        private Taxonomy _buildTaxonomy(String root, String parent, Object2DoubleMap<StringPair> scores) {
            List<Taxonomy> childrenTaxos = new ArrayList<>();
            Set<String> childTopics = children.get(root);
            if (childTopics != null) {
                for (String childTopic : childTopics) {
                    childrenTaxos.add(_buildTaxonomy(childTopic, parent, scores));
                }
            }
            double linkScore = parent == null ? Double.NaN : scores.getDouble(new StringPair(parent, root));
            return new Taxonomy(root, 0.0, linkScore, "", "", childrenTaxos, "none");
        }
    }

    private static class StringPair {

        String top, bottom;

        public StringPair(String top, String bottom) {
            this.top = top;
            this.bottom = bottom;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + Objects.hashCode(this.top);
            hash = 67 * hash + Objects.hashCode(this.bottom);
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
            if (!Objects.equals(this.top, other.top)) {
                return false;
            }
            if (!Objects.equals(this.bottom, other.bottom)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "StringPair{" + "top=" + top + ", bottom=" + bottom + '}';
        }
    }
}
