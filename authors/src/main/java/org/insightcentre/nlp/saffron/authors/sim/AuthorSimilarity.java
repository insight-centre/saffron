package org.insightcentre.nlp.saffron.authors.sim;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTopic;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class AuthorSimilarity {
    private final double threshold;
    private final int top_n;

    public AuthorSimilarity(double threshold, int top_n) {
        this.threshold = threshold;
        this.top_n = top_n;
    }

    public List<AuthorAuthor> topicSimilarity(List<AuthorTopic> ats) {
        List<AuthorAuthor> topicAuthors = new ArrayList<>();
        Map<String, Object2DoubleMap<String>> vectors = new HashMap<>();
        for(AuthorTopic at : ats) {
            if(!vectors.containsKey(at.researcher_id))
                vectors.put(at.researcher_id, new Object2DoubleOpenHashMap<String>());
            vectors.get(at.researcher_id).put(at.topic_id, at.score);
        }
        for(String t1 : vectors.keySet()) {
            TreeSet<AuthorAuthor> topN = new TreeSet<>(new Comparator<AuthorAuthor>() {

                @Override
                public int compare(AuthorAuthor arg0, AuthorAuthor arg1) {
                    int i1 = Double.compare(arg0.similarity, arg1.similarity);
                    if(i1 == 0) {
                        int i2 = arg0.author1_id.compareTo(arg1.author1_id);
                        if(i2 == 0) {
                            return arg0.author2_id.compareTo(arg1.author2_id);
                        }
                        return i2;
                    }
                    return i1;
                }
            });
            for(String t2 : vectors.keySet()) {
                if(!t1.equals(t2)) {
                    double s= sim(vectors.get(t1), vectors.get(t2));
                    if(s > threshold)
                        topN.add(new AuthorAuthor(t1, t2, s));
                }
                while(topN.size() > top_n)
                    topN.pollFirst();
                topicAuthors.addAll(topN);
            }
        }

        return topicAuthors;
    }

    private double sim(Object2DoubleMap<String> v1, Object2DoubleMap<String> v2) {
        double aa = 0, bb = 0, ab = 0;
        for(String s : v1.keySet()) {
            double a = v1.get(s);
            aa += a * a;
            if(v2.containsKey(s)) 
                ab += a * v2.get(s);
        }
        for(String s : v2.keySet()) {
            double b = v2.get(s);
            bb += b * b;
        }
        return ab / Math.sqrt(aa * bb);
    }

}
