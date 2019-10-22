package org.insightcentre.nlp.saffron.taxonomy.supervised;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.insightcentre.nlp.saffron.data.Status;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;

/**
 * Greedily construct a taxonomy
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class GreedyTaxoExtract {

    private final SupervisedTaxo classifier;
    private final int maxChildren;

    public GreedyTaxoExtract(SupervisedTaxo classifier, int maxChildren) {
        this.classifier = classifier;
        this.maxChildren = maxChildren;
    }

    
    public Taxonomy extractTaxonomy(List<DocumentTerm> docTerms, Map<String, Term> termMap) {
        HashMap<String, List<ScoredString>> scoresByChild = new HashMap<>();
        for(String t1 : termMap.keySet()) {
            List<ScoredString> list = new ArrayList<>();
            for(String t2 : termMap.keySet()) {
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
        
        ArrayList<String> orphans = new ArrayList<>();
        {
            Iterator<Map.Entry<String, List<ScoredString>>> iter = scoresByChild.entrySet().iterator();
            while(iter.hasNext()) {
                Map.Entry<String, List<ScoredString>> e = iter.next();
                String s1 = e.getKey();
                List<ScoredString> ss = e.getValue();
                if(ss.isEmpty()) {
                    iter.remove();
                    orphans.add(s1);
                }
            }
        }
        
        HashMap<String, Taxonomy> taxos = new HashMap<>();
        HashMap<String, Taxonomy> topTaxos = new HashMap<>();
        HashSet<String> children = new HashSet<>();
        while(!scoresByChild.isEmpty()) {
            String parent = null, child = null;
            double hpScore = Double.NEGATIVE_INFINITY;
            Iterator<Map.Entry<String, List<ScoredString>>> iter = scoresByChild.entrySet().iterator();
            while(iter.hasNext()) {
                Map.Entry<String, List<ScoredString>> e = iter.next();
                String s1 = e.getKey();
                List<ScoredString> ss = e.getValue();
                if(ss.isEmpty()) {
                    iter.remove();
                } else {
                    double score = ss.get(0).score;
                    if(score > hpScore) {
                        child = s1;
                        parent = ss.get(0).s;
                        hpScore = score;
                    }
                }       
            }
            //System.err.println(child + " => " + parent);
            if(parent == null || child == null)
                break;
            Taxonomy pTaxo = taxos.get(parent);
            Taxonomy cTaxo = taxos.get(child);
            if(pTaxo == null && cTaxo == null) {
                pTaxo = new Taxonomy(parent, termMap.get(parent).getScore(), Double.NaN,  "", "", new ArrayList<Taxonomy>(), Status.none);
                cTaxo = new Taxonomy(child, termMap.get(child).getScore(), hpScore, "", "", new ArrayList<Taxonomy>(), Status.none);
                pTaxo.children.add(cTaxo);
                taxos.put(parent, pTaxo);
                taxos.put(child, cTaxo);
            } else if(pTaxo == null) {
                pTaxo = new Taxonomy(parent, termMap.get(parent).getScore(), Double.NaN, "", "", new ArrayList<Taxonomy>(), Status.none);
                pTaxo.children.add(cTaxo);
                taxos.put(parent, pTaxo);
            } else if(cTaxo == null && pTaxo.children.size() < maxChildren) {
                cTaxo = new Taxonomy(child, termMap.get(child).getScore(), hpScore, "", "", new ArrayList<Taxonomy>(), Status.none);
                pTaxo.children.add(cTaxo);
                taxos.put(child, cTaxo);
            } else if(pTaxo.children.size() < maxChildren) {
                if (pTaxo.hasDescendent(child) || cTaxo.hasDescendent(parent)) {
                    scoresByChild.get(child).remove(0);
                    continue; // This is a loop
                }
                pTaxo.children.add(cTaxo);
            } else {
                // pTaxo.children.size() > maxChildren
                scoresByChild.get(child).remove(0);
                continue;
            }
            topTaxos.remove(child);
            children.add(child);
            if(!children.contains(parent))
                topTaxos.put(parent, pTaxo);
            
            if(topTaxos.isEmpty()) {
                throw new RuntimeException("Error adding " + child + " => " + parent);
            }
            scoresByChild.remove(child);
        }
        
        Iterator<String> orphanIter = orphans.iterator();
        while(orphanIter.hasNext()) {
            if(topTaxos.containsKey(orphanIter.next()))
                orphanIter.remove();
        }
        
        return addOrphans(mergeTaxos(topTaxos, termMap), orphans, termMap);
    }

    private Taxonomy addOrphans(Taxonomy t, List<String> orphans, Map<String, Term> termMap) {
        for(String orphan : orphans) {
            t.children.add(new Taxonomy(orphan, termMap.get(orphan).getScore(), Double.NaN, "", "", new ArrayList<Taxonomy>(), Status.none));
        }
        return t;
    }
    
    private Taxonomy mergeTaxos(HashMap<String, Taxonomy> topTaxos,
            Map<String, Term> terms) {
        if(topTaxos.isEmpty()) {
            throw new RuntimeException("Did not extract any taxonomies (no terms?)");
        } else if(topTaxos.size() == 1) {
            return topTaxos.values().iterator().next();
        } else {
            String topTerm = null;
            double topScore = Double.NEGATIVE_INFINITY;
            int topOcc = Integer.MIN_VALUE;
            for(String termString : topTaxos.keySet()) {
                Term t = terms.get(termString);
                if(t != null) {
                    if(t.getScore() > topScore || 
                            (t.getScore() == topScore && t.getOccurrences() > topOcc)) {
                        topScore = t.getScore();
                        topOcc = t.getOccurrences();
                        topTerm = termString;
                    }
                }
            }
            if(topTerm == null)
                throw new RuntimeException("Unreachable");
            Taxonomy t = topTaxos.remove(topTerm);
            t.children.addAll(topTaxos.values());
            if(!t.verifyTree())
                throw new RuntimeException("loops!");
            return t;
        }
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

   /* public Taxonomy extractTaxonomy(List<DocumentTopic> docTopics, Map<String, Topic> topicMap) {
        final Object2DoubleMap<StringPair> scores = new Object2DoubleOpenHashMap<>();
        for (String s : topicMap.keySet()) {
            for (String t : topicMap.keySet()) {
                if (!s.equals(t)) {
                    double score = classifier.predict(s, t);
                    final StringPair sp;
                    if (score > 0) {
                        sp = new StringPair(s, t);
                    } else {
                        sp = new StringPair(t, s);
                        score = -score;
                    }
                    double score2 = scores.getDouble(sp);
                    scores.put(sp, Math.max(score, score2));
                }
            }
        }
        ArrayList<StringPair> pairs = new ArrayList<>(scores.keySet());
        pairs.sort(new Comparator<StringPair>() {
            @Override
            public int compare(StringPair o1, StringPair o2) {
                double s1 = scores.getDouble(o1);
                double s2 = scores.getDouble(o2);
                int i1 = Double.compare(Math.abs(s1), Math.abs(s2));
                if (i1 != 0) {
                    return -i1;
                }
                int i2 = Double.compare(s1, s2);
                if (i2 != 0) {
                    return -i2;
                }
                int i3 = o1._1.compareTo(o2._1);
                if (i3 != 0) {
                    return i3;
                }
                int i4 = o2._2.compareTo(o2._2);
                return i4;
            }
        });
        return buildTaxo(pairs, topicMap.size());
    }

    private Taxonomy buildTaxo(ArrayList<StringPair> pairs, int completeSize) {
        HashMap<String, Taxonomy> taxos = new HashMap<>();
        while (!pairs.isEmpty()) {
            StringPair sp = pairs.remove(0);
            Taxonomy t1 = taxos.get(sp._1);
            Taxonomy t2 = taxos.get(sp._2);
            System.err.printf("%s <-> %s\n", sp._1, sp._2);
            if (t1 == null && t2 == null) { // Completely safe
                t1 = new Taxonomy(sp._1, new ArrayList<Taxonomy>());
                t2 = new Taxonomy(sp._2, new ArrayList<Taxonomy>());
                t2.children.add(t1);
                taxos.put(sp._1, t1);
                taxos.put(sp._2, t2);
            } else if (t1 == null) {
                    t1 = new Taxonomy(sp._1, new ArrayList<Taxonomy>());
                    t2.children.add(t1);
                    taxos.put(sp._1, t1);
            } else if (t2 == null) { // The child is new
                t2 = new Taxonomy(sp._2, new ArrayList<Taxonomy>());
                t2.children.add(t1);
                taxos.put(sp._2, t2);
            } else { // Join two existing taxonomies
                if (t1.hasDescendent(sp._2) || t2.hasDescendent(sp._1)) {
                    continue; // This is a loop
                }
                t2.children.add(t1);
            }
            // Remove any other case where _1 could occur in the first position
            removeByChild(pairs, sp._1);
        }
        List<Taxonomy> roots = findRoots(taxos);
        if (roots.isEmpty()) {
            throw new RuntimeException("Constructed no taxonomy... Maybe no topics?");
        } else if (roots.size() == 1) {
            return roots.get(0);
        } else {
            return new Taxonomy("Everything", roots);
        }
    }

    private List<Taxonomy> findRoots(HashMap<String, Taxonomy> taxos) {
        List<Taxonomy> roots = new ArrayList<>(taxos.values());
        for (Taxonomy t : taxos.values()) {
            for (Taxonomy c : t.getChildren()) {
                Taxonomy t2 = taxos.get(c.root);
                if (t2 != null) {
                    roots.remove(t2);
                }
            }
        }
        return roots;
    }

    private void removeByChild(ArrayList<StringPair> pairs, String s) {
        ListIterator<StringPair> iter = pairs.listIterator();
        while(iter.hasNext()) {
            if(iter.next()._1.equals(s))
                iter.remove();
        }
    }*/

}
