package org.insightcentre.nlp.saffron.authors.sim;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.*;

import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.AuthorSimilarityConfiguration;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class AuthorSimilarity {

    private final double threshold;
    private final int top_n;

    public AuthorSimilarity(AuthorSimilarityConfiguration config) {
        this.threshold = config.threshold;
        this.top_n = config.topN;
    }

    public List<AuthorAuthor> authorSimilarity(Collection<AuthorTerm> ats, String saffronDatasetName) {
        return authorSimilarity(ats, saffronDatasetName, new DefaultSaffronListener());
    }
       
    public List<AuthorAuthor> authorSimilarity(Collection<AuthorTerm> ats, String saffronDatasetName, SaffronListener log) {
        List<AuthorAuthor> termAuthors = new ArrayList<>();
        Map<String, Object2DoubleMap<String>> vectors = new HashMap<>();
        Object2DoubleMap<String> authorNorms = new Object2DoubleOpenHashMap<>();
        for (AuthorTerm at : ats) {
            if (!vectors.containsKey(at.getAuthorId())) {
                vectors.put(at.getAuthorId(), new Object2DoubleOpenHashMap<String>());
            }
            vectors.get(at.getAuthorId()).put(at.getTermId(), at.getScore());
            authorNorms.put(at.getAuthorId(), at.getScore()*at.getScore() + authorNorms.getDouble(at.getAuthorId()));
        }
        for(Object2DoubleMap.Entry<String> e : authorNorms.object2DoubleEntrySet()) {
            e.setValue(Math.sqrt(e.getDoubleValue()));
        }
        Map<String, TopNList<String>> authorByTopic = new HashMap<>();
        for(AuthorTerm at : ats) {
            if(!authorByTopic.containsKey(at.getTermId())) {
                authorByTopic.put(at.getTermId(), new TopNList<>(top_n * 2));
            }
            authorByTopic.get(at.getTermId()).offer(at.getAuthorId(), Math.abs(at.getScore()) / authorNorms.getDouble(at.getAuthorId()));
        }
        int i = 0;
        for (String t1 : vectors.keySet()) {
            TreeSet<AuthorAuthor> topN = new TreeSet<>(new Comparator<AuthorAuthor>() {

                @Override
                public int compare(AuthorAuthor arg0, AuthorAuthor arg1) {
                    int i1 = Double.compare(arg0.similarity, arg1.similarity);
                    if (i1 == 0) {
                        int i2 = arg0.author1_id.compareTo(arg1.author1_id);
                        if (i2 == 0) {
                            return arg0.author2_id.compareTo(arg1.author2_id);
                        }
                        return i2;
                    }
                    return i1;
                }
            });
            TreeSet<String> authors2 = new TreeSet<>();
            for(String term : vectors.get(t1).keySet()) {
                authors2.addAll(authorByTopic.getOrDefault(term, new TopNList<>(0)));
            }
            //System.err.print(".");
            for (String t2 : authors2) {
                if (!t1.equals(t2)) {
                    double s = sim(vectors.get(t1), vectors.get(t2));
                    if (s > threshold) {
                        topN.add(new AuthorAuthor(t1, t2, s, saffronDatasetName, new HashMap<String, String>(), saffronDatasetName ));
                    }
                }
            }
            while (topN.size() > top_n) {
                topN.pollFirst();
            }
            termAuthors.addAll(topN);
            //if(++i % 1000 == 0) {
            //    log.tick();
            //}
        }

        return termAuthors;
    }

    private double sim(Object2DoubleMap<String> v1, Object2DoubleMap<String> v2) {
        double aa = 0, bb = 0, ab = 0;
        for (String s : v1.keySet()) {
            double a = v1.get(s);
            aa += a * a;
            if (v2.containsKey(s)) {
                ab += a * v2.get(s);
            }
        }
        for (String s : v2.keySet()) {
            double b = v2.get(s);
            bb += b * b;
        }
        if (aa == 0 || bb == 0) {
            return 0;
        }
        return ab / Math.sqrt(aa * bb);
    }

}
