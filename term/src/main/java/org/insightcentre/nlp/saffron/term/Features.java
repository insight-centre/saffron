package org.insightcentre.nlp.saffron.term;

import java.util.Set;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Features {
    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
    
    public static double weirdness(String term, FrequencyStats stats, FrequencyStats ref) {
        int i = stats.termFrequency.getInt(term);
        int j = ref.termFrequency.getInt(term);
        return ((double)i + 0.1) / ((double)j + 0.1);
    }
    
    public static double aveTermFreq(String term, FrequencyStats stats) {
        double i = stats.termFrequency.getInt(term);
        double j = stats.docFrequency.getInt(term);
        return j == 0 ? 0.0 : i / j;
    }
    
    public static double residualIDF(String term, FrequencyStats stats) {
        double tf = stats.termFrequency.getInt(term);
        double df = stats.docFrequency.getInt(term);
        assert(df <= tf);
        assert(df <= stats.documents);
        double theta = tf / stats.documents;
        return log2(1 - Math.exp(-theta)) - log2(df / stats.documents);
    }
    
    public static double totalTFIDF(String term, FrequencyStats stats) {
        double tf = stats.termFrequency.getInt(term);
        double df = stats.docFrequency.getInt(term);
        if(df == 0) return 0.0;
        return tf * Math.log((double)stats.documents / df);
    }
    
    public static double cValue(String term, FrequencyStats freq, InclusionStats incl) {
        double tf = freq.termFrequency.getInt(term);
        double t = term.split(" ").length;
        double subtf = 0.0;
        if(incl.subTerms.containsKey(term)) {
            final Set<String> subterms = incl.subTerms.get(term);
            for(String subterm : subterms) {
                subtf += freq.termFrequency.getInt(subterm);
            }
            subtf /= subterms.size();
        }
        return log2(t + 0.1) * (tf - subtf);
    }
    
    public static double basic(String term, double alpha, FrequencyStats freq, InclusionStats incl) {
        double tf = freq.termFrequency.getInt(term);
        double t = term.split(" ").length;
        double et = incl.subTerms.containsKey(term) ? incl.subTerms.get(term).size() : 0.0;
        return t * log2(tf) + alpha * et;
    }
    
    public static double basicCombo(String term, double alpha, double beta, FrequencyStats freq, InclusionStats incl) {
        double tf = freq.termFrequency.getInt(term);
        double t = term.split(" ").length;
        double et = incl.subTerms.containsKey(term) ? incl.subTerms.get(term).size() : 0.0;
        double et2 = incl.superTerms.getInt(term);
        return t * log2(tf) + alpha * et + beta * et2;
    }
    
    public static double relevance(String term, FrequencyStats freq, FrequencyStats ref) {
        double ntf1 = (double)freq.termFrequency.getInt(term) / freq.tokens;
        double df = (double)freq.docFrequency.getInt(term) / freq.documents;
        double ntf2 = (double)ref.termFrequency.getInt(term) / ref.tokens;
        
        return 1.0 - 1.0/(log2(2 + ntf1 * df / ntf2));
    }
}
