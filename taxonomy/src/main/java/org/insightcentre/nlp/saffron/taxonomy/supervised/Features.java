package org.insightcentre.nlp.saffron.taxonomy.supervised;

import Jama.Matrix;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import static java.lang.Math.max;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provides supervised feature extraction for taxonomy learning
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Features {
    private Matrix svdMatrixAve, svdMatrixMinMax;
    private HashMap<String, IntSet> topicDocuments;


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
            for (int j = 0; j < tops.length; j++) {
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
        throw new UnsupportedOperationException();
    }

    private Matrix vectorByAve(String sent) {
        String[] sents = PrettyGoodTokenizer.tokenize(sent);
        if (sents.length == 0) {
            throw new RuntimeException("Empty sentence");
        }
        Matrix v = getVectorForWord(sents[0]);
        for (int i = 1; i < sents.length; i++) {
            v.plus(getVectorForWord(sents[i]));
        }
        return v;
    }

    private Matrix vectorMinMax(String sent) {
        String[] sents = PrettyGoodTokenizer.tokenize(sent);
        if (sents.length == 0) {
            throw new RuntimeException("Empty sentence");
        }
        Matrix v2 = null;
        for (int i = 0; i < sents.length; i++) {
            Matrix v = getVectorForWord(sents[0]);
            if (v2 == null) {
                v2 = new Matrix(v.getRowDimension() * 2, 1);
            }
            for (int j = 0; j < v.getRowDimension(); j++) {
                v2.set(j * 2, 0, Math.max(v2.get(j * 2, 0), v.get(j, 0)));
                v2.set(j * 2 + 1, 0, Math.min(v2.get(j * 2 + 1, 0), v.get(j, 0)));
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
        Matrix v2 = vectorByAve(bottom).transpose();
        Matrix v3 = svdMatrixAve.times(v1);
        return v2.times(v3).get(0, 0);
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
        Matrix v2 = vectorMinMax(bottom).transpose();
        Matrix v3 = svdMatrixMinMax.times(v1);
        return v2.times(v3).get(0, 0);
    }
    
    /**
     * Document topic complement difference. Defined as |A n B| / |A| - |A n B| / |B|
     * @param top
     * @param bottom
     * @return 
     */
    public double topicComplementDiff(String top, String bottom) {
        IntSet s1 = topicDocuments.get(top);
        IntSet s2 = topicDocuments.get(bottom);
        if(s1 != null && s2 != null && !s1.isEmpty() && !s2.isEmpty()) {
            s1 = new IntRBTreeSet(s1);
            int n1 = s1.size();
            s1.retainAll(s2);
            int m = s1.size();
            int n2 = s2.size();
            return (double)(n1 - m) / n1 - (double)(n2 - m) / n2;
        } else {
            return 0;
        }
    }
    
}
