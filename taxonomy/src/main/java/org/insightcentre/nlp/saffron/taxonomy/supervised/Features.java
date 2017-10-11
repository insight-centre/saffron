package org.insightcentre.nlp.saffron.taxonomy.supervised;

import Jama.Matrix;
import static java.lang.Math.max;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provides supervised feature extraction for taxonomy learning
 * @author John McCrae <john@mccr.ae>
 */
public class Features {
    /**
     * Returns +1 if bottom includes top, -1 if top includes bottom or 0 otherwise.
     * A string is said to include another string if it starts with or ends with
     * that string respecting word boundaries
     * @param top A lowercase string
     * @param bottom A lowercase string
     * @return 
     */
    public double inclusion(String top, String bottom) {
        if(bottom.matches("\\Q" + top + "\\E\\b.*") ||
                bottom.matches(".*\\b\\Q" + top + "\\E")) {
            return +1;
        } else if (top.matches("\\Q" + bottom + "\\E\\b.*") ||
                top.matches(".*\\b\\Q" + bottom + "\\E")) {
            return -1;
        } else {
            return 0;
        }
    }
    
    /**
     * The number of words that are in both strings divided by the length of top
     * @param top A lowercase string
     * @param bottom A lowercase string
     * @return 
     */
    public double overlap(String top, String bottom) {
        Set<String> tops = new TreeSet<>(Arrays.asList(PrettyGoodTokenizer.tokenize(top)));
        int n = tops.size();
        tops.retainAll(Arrays.asList(PrettyGoodTokenizer.tokenize(bottom)));
        return (double)tops.size() / n;
    }
    
    /**
     * The longest common subsequence of words divided by the length of top
     * @param top A lowercase string
     * @param bottom A lowercase string
     * @return 
     */
    public double longestCommonSubseq(String top, String bottom) {
        String[] tops = PrettyGoodTokenizer.tokenize(top);
        String[] bottoms = PrettyGoodTokenizer.tokenize(bottom);
        int[][] lcs = new int[tops.length][bottoms.length];
        int maxLcs = 0;
        for(int i = 0; i < tops.length; i++) {
            for(int j = 0; j < tops.length; j++) {
                if(tops[i].equals(bottoms[j])) {
                    if(i == 0 || j == 0) {
                        lcs[i][j] = 1;
                    } else {
                        lcs[i][j] = lcs[i - 1][j - 1] + 1;
                        maxLcs = max(maxLcs, lcs[i][j]);
                    }
                }
            }
        }
        return (double)maxLcs / tops.length;
    }
    
    private Matrix svdMatrix;
    
    private Matrix getVectorForWord(String word) {
        throw new UnsupportedOperationException();
    }
    
    private Matrix vectorByAve(String sent) {
        String[] sents = PrettyGoodTokenizer.tokenize(sent);
        if(sents.length == 0) {
            throw new RuntimeException("Empty sentence");
        }
        Matrix v = getVectorForWord(sents[0]);
        for(int i = 1; i < sents.length; i++) {
            v.plus(getVectorForWord(sents[i]));
        }
        return v;
    }
    
    /**
     * Get the similarity of these vectors by using an inverse learned relation
     * over average vectors
     * @param top A lowercase string
     * @param bottom A lowercase string
     * @return 
     */
    public double svdSimAve(String top, String bottom) {
        Matrix v1 = vectorByAve(top);
        Matrix v2 = vectorByAve(bottom);
        Matrix v3 = svdMatrix.times(v1);
        return v2.times(v3).get(0, 0);
    }
}
