package org.insightcentre.nlp.saffron.taxonomy.supervised;

import Jama.Matrix;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.insightcentre.nlp.saffron.data.Topic;

/**
 * Provides supervised feature extraction for taxonomy learning
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Features {

    private final Matrix svdMatrixAve, svdMatrixMinMax;
    private final Map<String, IntSet> topicDocuments;
    private final Map<String, double[]> vectors;
    private final Train.FeatureSelection selection;
    private final Map<String, Topic> topicMap;

    public Features(Matrix svdMatrixAve, Matrix svdMatrixMinMax, 
            Map<String, IntSet> topicDocuments, 
            Map<String, double[]> vectors,
            Map<String, Topic> topicMap,
            Train.FeatureSelection selection) {
        this.svdMatrixAve = svdMatrixAve;
        this.svdMatrixMinMax = svdMatrixMinMax;
        this.topicDocuments = topicDocuments;
        this.vectors = vectors;
        this.topicMap = topicMap;
        this.selection = selection;
    }

    /**
     * Returns +1 if bottom includes top, -1 if top includes bottom or 0
     * otherwise. A string is said to include another string if it starts with
     * or ends with that string respecting word boundaries
     *
     * @param top A lowercase string
     * @param bottom A lowercase string
     * @return
     */
    public double inclusion(String top, String bottom) {
        if (bottom.matches("\\Q" + top + "\\E\\b.*")
                || bottom.matches(".*\\b\\Q" + top + "\\E")) {
            return +1;
        } else if (top.matches("\\Q" + bottom + "\\E\\b.*")
                || top.matches(".*\\b\\Q" + bottom + "\\E")) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * The number of words that are in both strings divided by the length of top
     *
     * @param top A lowercase string
     * @param bottom A lowercase string
     * @return
     */
    public double overlap(String top, String bottom) {
        Set<String> tops = new TreeSet<>(Arrays.asList(PrettyGoodTokenizer.tokenize(top)));
        int n = tops.size();
        tops.retainAll(Arrays.asList(PrettyGoodTokenizer.tokenize(bottom)));
        return (double) tops.size() / n;
    }

    /**
     * The longest common subsequence of words divided by the length of top
     *
     * @param top A lowercase string
     * @param bottom A lowercase string
     * @return
     */
    public double longestCommonSubseq(String top, String bottom) {
        String[] tops = PrettyGoodTokenizer.tokenize(top);
        String[] bottoms = PrettyGoodTokenizer.tokenize(bottom);
        int[][] lcs = new int[tops.length][bottoms.length];
        int maxLcs = 0;
        for (int i = 0; i < tops.length; i++) {
            for (int j = 0; j < bottoms.length; j++) {
                if (tops[i].equals(bottoms[j])) {
                    if (i == 0 || j == 0) {
                        lcs[i][j] = 1;
                    } else {
                        lcs[i][j] = lcs[i - 1][j - 1] + 1;
                        maxLcs = max(maxLcs, lcs[i][j]);
                    }
                }
            }
        }
        return (double) maxLcs / tops.length;
    }

    private Matrix getVectorForWord(String word) {
        double[] v = vectors.get(word);
        return v == null ? null : new Matrix(v, v.length);
    }

    private Matrix vectorByAve(String sent) {
        String[] sents = PrettyGoodTokenizer.tokenize(sent);
        if (sents.length == 0) {
            throw new RuntimeException("Empty sentence");
        }
        int m = 0;
        int n = 1;
        Matrix v = null;
        while (v == null && m < sents.length) {
            v = getVectorForWord(sents[m++]);
        }
        for (int i = m; i < sents.length; i++) {
            Matrix v2 = getVectorForWord(sents[i]);
            if (v2 != null) {
                v.plus(v2);
                n++;
            }
        }
        return v == null ? null : v.times(1.0 / n);
    }

    private Matrix vectorMinMax(String sent) {
        String[] sents = PrettyGoodTokenizer.tokenize(sent);
        if (sents.length == 0) {
            throw new RuntimeException("Empty sentence");
        }
        Matrix v2 = null;
        for (int i = 0; i < sents.length; i++) {
            Matrix v = getVectorForWord(sents[0]);
            if (v != null) {
                if (v2 == null) {
                    v2 = new Matrix(v.getRowDimension() * 2, 1);
                }
                for (int j = 0; j < v.getRowDimension(); j++) {
                    v2.set(j * 2, 0, Math.max(v2.get(j * 2, 0), v.get(j, 0)));
                    v2.set(j * 2 + 1, 0, Math.min(v2.get(j * 2 + 1, 0), v.get(j, 0)));
                }
            }
        }
        return v2;

    }

    /**
     * Get the similarity of these vectors by using an inverse learned relation
     * over average vectors
     *
     * @param top A lowercase string
     * @param bottom A lowercase string
     * @return
     */
    public double svdSimAve(String top, String bottom) {
        Matrix v1 = vectorByAve(top);
        Matrix v2 = vectorByAve(bottom);
        if(v1 != null & v2 != null) {
            v2 = v2.transpose();
            Matrix v3 = svdMatrixAve.times(v1);
            return v2.times(v3).get(0, 0);
        } else {
            return 0.0;
        }
    }

    /**
     * Get the similarity of these vectors by using an inverse learned relation
     * over min-max vectors
     *
     * @param top A lowercase string
     * @param bottom A lowercase string
     * @return
     */
    public double svdSimMixMax(String top, String bottom) {
        Matrix v1 = vectorMinMax(top);
        Matrix v2 = vectorMinMax(bottom);
        if(v1 != null && v2 != null) {
            v2 = v2.transpose();
            Matrix v3 = svdMatrixMinMax.times(v1);
            return v2.times(v3).get(0, 0);
        } else {
            return 0.0;
        }
    }

    /**
     * Document topic complement difference. Defined as |A n B| / |A| - |A n B|
     * / |B|
     *
     * @param top
     * @param bottom
     * @return
     */
    public double topicComplementDiff(String top, String bottom) {
        IntSet s1 = topicDocuments.get(top);
        IntSet s2 = topicDocuments.get(bottom);
        if (s1 != null && s2 != null && !s1.isEmpty() && !s2.isEmpty()) {
            s1 = new IntRBTreeSet(s1);
            int n1 = s1.size();
            s1.retainAll(s2);
            int m = s1.size();
            int n2 = s2.size();
            return (double) (n1 - m) / n1 - (double) (n2 - m) / n2;
        } else {
            return 0;
        }
    }
    
    /**
     * The relative frequency of the terms given as log(freq(top)/freq(bottom))
     * @param top
     * @param bottom
     * @return 
     */
    public double relFreq(String top, String bottom) {
        if(topicMap != null) {
            Topic t1 = topicMap.get(top);
            Topic t2 = topicMap.get(bottom);
            if(t1 != null && t2 != null && t1.occurrences > 0 && t2.occurrences > 0) {
                return Math.log((double)t1.occurrences) - Math.log((double)t2.occurrences);
            }
        }
        return 0;
    }
    

    public double[] buildFeatures(String top, String bottom) {
        DoubleList v = new DoubleArrayList();
        if(selection == null || selection.inclusion)
            v.add(inclusion(top, bottom));
        if(selection == null || selection.overlap)
            v.add(overlap(top, bottom));
        if(selection == null || selection.lcs)
            v.add(longestCommonSubseq(top, bottom));
        if((selection == null || selection.svdSimAve) && svdMatrixAve != null) 
            v.add(svdSimAve(top, bottom));
        if((selection == null || selection.svdSimMax) && svdMatrixMinMax != null)
            v.add(svdSimMixMax(top, bottom));
        if((selection == null || selection.topicDiff) && topicDocuments != null)
            v.add(topicComplementDiff(top, bottom));
        if((selection == null || selection.relFreq) && topicMap != null)
            v.add(relFreq(top, bottom));
        return v.toDoubleArray();
    }
    
    public String[] featureNames() {
        ArrayList<String> v = new ArrayList<>();
        if(selection == null || selection.inclusion)
            v.add("inclusion");
        if(selection == null || selection.overlap)
            v.add("overlap");
        if(selection == null || selection.lcs)
            v.add("longestCommonSubseq");
        if((selection == null || selection.svdSimAve) && svdMatrixAve != null) 
            v.add("svdSimAve");
        if((selection == null || selection.svdSimMax) && svdMatrixMinMax != null)
            v.add("svdSimMixMax");
        if((selection == null || selection.topicDiff) && topicDocuments != null)
            v.add("topicComplementDiff");
        if((selection == null || selection.relFreq) && topicMap != null)
            v.add("relFreq");
        return v.toArray(new String[v.size()]);
    }
}
