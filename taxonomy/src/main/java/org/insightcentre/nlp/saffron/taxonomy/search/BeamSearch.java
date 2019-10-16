package org.insightcentre.nlp.saffron.taxonomy.search;

import java.util.HashSet;
import java.util.Iterator;
import org.insightcentre.nlp.saffron.taxonomy.metrics.TaxonomyScore;
import java.util.Map;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;

/**
 *
 * @author John McCrae
 */
public class BeamSearch implements TaxonomySearch {

    private final TaxonomyScore emptyScore;
    private final int beamSize;

    public BeamSearch(TaxonomyScore emptyScore, int beamSize) {
        this.emptyScore = emptyScore;
        this.beamSize = beamSize;
        assert (beamSize > 0);
    }

    @Override
    public Taxonomy extractTaxonomyWithBlackWhiteList(Map<String, Term> topicMap,
            Set<TaxoLink> whiteList, Set<TaxoLink> blackList) {
        Beam<Soln> previous = new Beam<>(beamSize);
        Beam<Soln> complete = new Beam<>(beamSize);
        TaxonomyScore score = emptyScore;
        Solution soln = Solution.empty(topicMap.keySet());
        double s2 = 0.0;
        Set<String> whiteHeads = new HashSet<>();

        for (TaxoLink sp : whiteList) {
            soln = soln.add(sp.top, sp.bottom,
                    topicMap.get(sp.top).score,
                    topicMap.get(sp.bottom).score,
                    score.deltaScore(sp), true);
            s2 += score.deltaScore(sp);
            score = score.next(sp.top, sp.bottom, soln);
            whiteHeads.add(sp.bottom);
        }
        previous.push(new Soln(soln, score, s2, false), s2);
        for (String t1 : topicMap.keySet()) {
            if(whiteHeads.contains(t1)) 
                continue;
            Beam<Soln> next = new Beam<>(beamSize);
            // We are looking for t1's parent
            for (String t2 : topicMap.keySet()) {
                if (!t1.equals(t2)) {
                    final TaxoLink taxoLink = new TaxoLink(t2, t1);
                    if (blackList.contains(taxoLink)) {
                        continue;
                    }
                    for (Soln prevSoln : previous) {
                        final double linkScore = prevSoln.score.deltaScore(taxoLink);
                        double totalScore = prevSoln.totalScore
                                + linkScore;
                        if (next.canPush(totalScore)) {
                            Solution s = prevSoln.soln.add(t2, t1,
                                    topicMap.get(t2).score,
                                    topicMap.get(t1).score, linkScore, false);
                            if (s != null) {
                                Soln candidate = new Soln(s,
                                        prevSoln.score.next(t2, t1, s),
                                        totalScore,
                                        prevSoln.rooted);
                                next.push(candidate, totalScore);
                                if (candidate.soln.isComplete()) {
                                    complete.push(candidate, totalScore);
                                }
                            }
                        }
                    }
                    // We may once in a search have a node with no parents, this
                    // is the root element and a flag is set to decide this
                    for (Soln prevSoln : previous) {
                        if (!prevSoln.rooted) {
                            next.push(new Soln(prevSoln.soln, prevSoln.score,
                                    prevSoln.totalScore, true),
                                    prevSoln.totalScore);
                        }
                    }
                }
            }
            previous = next;
        }
        return complete.pop().soln.toTaxonomy();
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
            int c = Double.compare(totalScore, o.totalScore);
            if(c != 0) { return -c; }
            c = Boolean.compare(rooted, o.rooted);
            if(c != 0) { return c; }
            c = Integer.compare(soln.size, o.soln.size);
            if(c != 0) { return -c; }
            Iterator<String> i1 = soln.heads.keySet().iterator();
            Iterator<String> i2 = o.soln.heads.keySet().iterator();
            while(i1.hasNext() && i2.hasNext()) {
                c = i1.next().compareTo(i2.next());
                if(c != 0) { return c; }
            }
            if(i1.hasNext()) {
                return -1;
            } else if(i2.hasNext()) {
                return +1;
            }
            c = Integer.compare(this.hashCode(), o.hashCode());
            if (c != 0) {
                return c;
            }
            c = Integer.compare(this.soln.hashCode(), o.soln.hashCode());
            if (c != 0) {
                return c;
            }
            c = Integer.compare(this.score.hashCode(), o.score.hashCode());
            if (c != 0) {
                return c;
            }
            c = Double.compare(totalScore, o.totalScore);
            if (c != 0) {
                return c;
            }
            c = Boolean.compare(rooted, o.rooted);
            // This means there is a chance that some solutions may be lost
            return c;
        }
    }
}
