package org.insightcentre.nlp.saffron.term.lda;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.IOException;
import opennlp.tools.tokenize.Tokenizer;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.SearchException;
import org.insightcentre.nlp.saffron.term.FrequencyStats;

/**
 * The novel topic model metric as defined by 
 * Li, S., Li, J., Song, T., Li, W., Chang, B.: A novel topic model for automatic term extraction.
 * In: Proceedings of the 36th international ACM SIGIR conference on Research
 * and development in information retrieval, pp. 885–888. ACM (2013)
 * 
 * @author John McCrae
 */
public class NovelTopicModel {
    public static final int K = 20;
    public static final double alpha = 0.1, beta = 0.1;
    public static final int iterations = 100;
    
    private final double[][] P_wk;
    private final Object2IntMap<String> dictionary;
    private double minTopicFreq;

    public NovelTopicModel(double[][] P_wk, Object2IntMap<String> dictionary, double maxTopicFreq) {
        this.P_wk = P_wk;
        this.dictionary = dictionary;
        this.minTopicFreq = maxTopicFreq;
    }
    
   public static NovelTopicModel initialize(DocumentSearcher searcher, Tokenizer tokenizer) throws IOException, SearchException {
       CorpusProcessor.Result r = CorpusProcessor.convert(searcher, tokenizer);
       LDA lda = new LDA(r.buffer, K, r.docCount, r.dictionary.size(), alpha, beta);
       lda.train(iterations);
       final double[][] P_wk = new double[r.dictionary.size()][K];
       for(int w = 0; w < r.dictionary.size(); w++) {
           for(int k = 0; k < K; k++) {
               P_wk[w][k] = ((double)lda.N_kw[k][w] + alpha) / ((double)lda.N_k[k] + K * alpha);
           }
       }
       int minTopicFreq = Integer.MAX_VALUE;
       for(int k = 0; k < K; k++) {
           minTopicFreq = Math.min(minTopicFreq, lda.N_k[k]);
       }
       return new NovelTopicModel(P_wk, r.dictionary, (double)minTopicFreq + K * alpha);
   } 
   
   public double novelTopicModel(String term, FrequencyStats stats) {
       String[] words = term.split(" ");
       double tf = (double)stats.termFrequency.getInt(term) + alpha;
       double score = 0.0;
       for(String word : words) {
           if(dictionary.containsKey(word)) {
               int w = dictionary.getInt(word);
               double maxScore = 0.0;
               for(int k = 0; k < K; k++) {
                   maxScore = Math.max(maxScore, P_wk[w][k]);
               }
               score += maxScore;
           } else {
               score += alpha / minTopicFreq;
           }
       }
       
       return tf * score;
   }
}
