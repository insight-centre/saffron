package org.insightcentre.nlp.saffron.benchmarks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Taxonomy;

/**
 *
 * @author John McCrae
 */
public class FowlkesMallows {

    public static List<Set<String>> clusteringAtLevel(Taxonomy t, int level) {
        if(level == 0) {
            Set<String> s = new HashSet<>();
            flattenTaxonomy(t, s);
            return Collections.singletonList(s);
        } else if(level == 1) {
            List<Set<String>> clustering = new ArrayList<>();
            for(Taxonomy t2 : t.children) {
                Set<String> s = new HashSet<>();
                for(Set<String> s2 : clusteringAtLevel(t2, level - 1)) {
                    s.addAll(s2);
                }
                s.add(t2.root);
                clustering.add(s);
            }
            return clustering;
        } else {
            List<Set<String>> clustering = new ArrayList<>();
            for(Taxonomy t2 : t.children) {
                clustering.addAll(clusteringAtLevel(t2, level -1));
            }
            return clustering;
        }
    }

    public static double fowlkesMallows(Taxonomy t1, Taxonomy t2) {
        int depth = Math.max(t1.depth(), t2.depth());
        double score = 0.0;
        for(int level = 1; level <= depth; level++) {
            score += compareClustering(clusteringAtLevel(t1, level),
                            clusteringAtLevel(t2, level)) * (level);
        }
        return score * 2.0 / depth / (depth - 1);
    }
    
    private static void flattenTaxonomy(Taxonomy t, Set<String> s) {
        s.add(t.root);
        for(Taxonomy t2 : t.children) {
            flattenTaxonomy(t2, s);
        }
    }
    
    private static double compareClustering(List<Set<String>> c1, List<Set<String>> c2) {
        int n11 = 0; // the number of object pairs that are in the same cluster in both
        //int total = 0;
        int c1pairs = 0;
        for(Set<String> s1 : c1) {
            c1pairs += s1.size() * (s1.size() - 1) / 2;
            //total += s1.size();
        }
        //total = total * (total - 1) / 2;
        int c2pairs = 0;
        for(Set<String> s2 : c2) {
            c2pairs += s2.size() * (s2.size() - 1) / 2;
        }
        for(Set<String> s1 : c1) {
            for(Set<String> s2: c2) {
                Set<String> s3 = new HashSet<>(s1);
                s3.retainAll(s2);
                if(s3.size() > 1)
                    n11 += s3.size() * (s3.size() - 1) / 2;
            }
        }
        //int n10 = c1pairs - n11; // the number of object pairs that are in the same cluster in c1 but not c2
        //int n01 = c2pairs - n11; // the number of object pairs that are in the same cluster in c2 but not c1
        //int n00 = total - n10 - n01 - n11; // the number of object pairs that are in different clusters in both
        if(c1pairs > 0 && c2pairs > 0)
            return (double)n11 / Math.sqrt((double)c1pairs * c2pairs);
        else 
            return 0.0;
    }
}
