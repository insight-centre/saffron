package org.insightcentre.nlp.saffron.taxonomy.search;

import org.insightcentre.nlp.saffron.taxonomy.metrics.TaxonomyScore;
import java.util.ArrayList;
import java.util.Map;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;

/**
 *
 * @author John McCrae
 */
public class BeamSearch {

    private final TaxonomyScore emptyScore;
    private final int beamSize;

    public BeamSearch(TaxonomyScore emptyScore, int beamSize) {
        this.emptyScore = emptyScore;
        this.beamSize = beamSize;
        assert(beamSize > 0);
    }


    public Taxonomy extractTaxonomy(Map<String, Topic> topicMap) {
        Beam<Soln> previous = new Beam<>(beamSize);
        previous.push(new Soln(Solution.empty(topicMap.keySet()), emptyScore, 0.0, false), 0.0);
        for (String t1 : topicMap.keySet()) {
            Beam<Soln> next = new Beam<>(beamSize);
            // We are looking for t1's parent
            for (String t2 : topicMap.keySet()) {
                if(!t1.equals(t2)) {
                    for(Soln prevSoln : previous) {
                        final double linkScore = prevSoln.score.deltaScore(new TaxoLink(t2, t1));
                        double totalScore = prevSoln.totalScore +
                                linkScore;
                        if(next.canPush(totalScore)) {
                            Solution s = prevSoln.soln.add(t2, t1, 
                                    topicMap.get(t2).score, 
                                    topicMap.get(t1).score,linkScore);
                            if(s != null) {
                                Soln candidate = new Soln(s, 
                                    prevSoln.score.next(t2, t1, s),
                                    totalScore,
                                    prevSoln.rooted);
                                next.push(candidate, totalScore);
                            }
                        }
                    }
                    // We may once in a search have a node with no parents, this
                    // is the root element and a flag is set to decide this
                    for(Soln prevSoln : previous) {
                        if(!prevSoln.rooted) {
                            next.push(new Soln(prevSoln.soln, prevSoln.score, 
                                    prevSoln.totalScore, true), 
                                    prevSoln.totalScore);
                        }
                    }
                }
            }
            previous = next;
        }
        return previous.pop().soln.toTaxonomy();
    }
    
    private static class Soln implements Comparable<Soln> {
        public final Solution soln;
        public final TaxonomyScore score;
        public final double totalScore;
        public final boolean rooted;

        public Soln(Solution soln, TaxonomyScore score, double totalScore, boolean rooted) {
            this.soln = soln;
            this.score = score;
            this.totalScore = totalScore;
            this.rooted = rooted;
        }


        @Override
        public int compareTo(Soln o) {
            int c = Integer.compare(this.hashCode(), o.hashCode());
            if(c != 0) return c;
            c = Integer.compare(this.soln.hashCode(), o.soln.hashCode());
            if(c != 0) return c;
            c = Integer.compare(this.score.hashCode(), o.score.hashCode());
            if(c != 0) return c;
            c = Double.compare(totalScore, o.totalScore);
            if(c != 0) return c;
            c = Boolean.compare(rooted, o.rooted);
            // This means there is a chance that some solutions may be lost
            return c;
        }
    }
}
