package org.insightcentre.nlp.saffron.term.domain;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.tokenize.Tokenizer;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.SearchException;
import org.insightcentre.nlp.saffron.term.Features;
import org.insightcentre.nlp.saffron.term.FrequencyStats;
import org.insightcentre.nlp.saffron.term.InclusionStats;

/**
 * Extract the domain statistics and calculate the domain coherence statistic
 *
 * @author John McCrae
 */
public class DomainStats {

    public static final double alpha = 0.75;
    public static final int TERMS = 200;
    public static final int WORDS = 50;
    public static final int CONTEXT = 5;
    private final Map<String, Object2IntMap<String>> totalFreqs;
    private final FrequencyStats freqs;
    private final Object2IntMap<String> wordFreq;
    private final long N;

    public DomainStats(Map<String, Object2IntMap<String>> totalFreqs, FrequencyStats freqs, Object2IntMap<String> wordFreq, long N) {
        this.totalFreqs = totalFreqs;
        this.freqs = freqs;
        this.wordFreq = wordFreq;
        this.N = N;
    }

    public static DomainStats initialize(Corpus searcher, int nThreads,
            ThreadLocal<Tokenizer> tokenizer, int maxLength, int maxDocs,
            FrequencyStats stats, InclusionStats incl, Set<String> stopWords,
            ThreadLocal<POSTagger> tagger, Set<String> preceedingTokens, Set<String> middleTokens, Set<String> endTokens, boolean headTokenFinal) throws SearchException {
        Map<String, Object2IntMap<String>> totalFreqs = totalFreqs(searcher, nThreads, tokenizer, maxLength, maxDocs, stats, incl, tagger, preceedingTokens, middleTokens, endTokens, headTokenFinal);
        Set<String> words = topWords(stats, totalFreqs, stopWords);
        filterByWords(words, totalFreqs);
        Object2IntMap<String> wordFreq = new Object2IntLinkedOpenHashMap<>();
        long N = 0;
        for (Object2IntMap<String> freq2 : totalFreqs.values()) {
            for (Object2IntMap.Entry<String> e : freq2.object2IntEntrySet()) {
                wordFreq.put(e.getKey(), wordFreq.getInt(e.getKey()) + e.getIntValue());
                N += e.getIntValue();
            }
        }

        return new DomainStats(totalFreqs, stats, wordFreq, N);
    }

    private static List<String> topTerms(final FrequencyStats stats, final InclusionStats incl) {
        List<String> topTerms = new ArrayList<>(stats.termFrequency.keySet());
        topTerms.sort(new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                double s1 = Features.basic(o1, alpha, stats, incl);
                double s2 = Features.basic(o1, alpha, stats, incl);
                int c = Double.compare(s1, s2);
                return c == 0 ? o1.compareTo(o2) : -c;
            }
        });
        topTerms.subList(0, Math.min(TERMS, topTerms.size()));
        return topTerms;
    }

    private static Map<String, Object2IntMap<String>> totalFreqs(Corpus searcher, int nThreads,
            ThreadLocal<Tokenizer> tokenizer, int maxLength, int maxDocs,
            FrequencyStats stats, InclusionStats incl, 
            ThreadLocal<POSTagger> tagger, Set<String> preceedingTokens, Set<String> middleTokens, Set<String> endTokens, boolean headTokenFinal) throws SearchException {
        ExecutorService service = new ThreadPoolExecutor(nThreads, nThreads, 0,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());

        Set<String> topTerms = new HashSet<>(topTerms(stats, incl));
        Map<String, Object2IntMap<String>> totalFreqs = new HashMap<>();

        int docCount = 0;
        for (Document doc : searcher.getDocuments()) {
            service.submit(new TopWordsTask(doc, tokenizer, maxLength, topTerms, totalFreqs, tagger, preceedingTokens, middleTokens, endTokens, headTokenFinal));
            if (docCount++ > maxDocs) {
                break;
            }
        }

        service.shutdown();
        try {
            service.awaitTermination(2, TimeUnit.DAYS);
            return totalFreqs;

        } catch (InterruptedException x) {
            x.printStackTrace();
            throw new RuntimeException(x);
        }
    }

    private static Set<String> topWords(FrequencyStats stats, Map<String, Object2IntMap<String>> totalFreqs, Set<String> stopWords) {
        Object2DoubleMap<String> pmis = calcPMI(totalFreqs, stats);
        final ObjectIterator<Map.Entry<String, Double>> iterator = pmis.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Double> e = iterator.next();
            if (stopWords.contains(e.getKey()) || e.getKey().matches("\\W*")) {
                iterator.remove();
            }
        }
        return topN(pmis, WORDS);
    }

    private static Object2DoubleMap<String> calcPMI(Map<String, Object2IntMap<String>> totalFreqs, FrequencyStats freqs) {
        Object2IntMap<String> wordFreq = new Object2IntLinkedOpenHashMap<>();
        long N = 0;
        for (Object2IntMap<String> freq2 : totalFreqs.values()) {
            for (Object2IntMap.Entry<String> e : freq2.object2IntEntrySet()) {
                wordFreq.put(e.getKey(), wordFreq.getInt(e.getKey()) + e.getIntValue());
                N += e.getIntValue();
            }
        }

        Object2DoubleMap<String> pmis = new Object2DoubleOpenHashMap<>();
        for (Map.Entry<String, Object2IntMap<String>> e : totalFreqs.entrySet()) {
            String term = e.getKey();
            for (Object2IntMap.Entry<String> e2 : e.getValue().object2IntEntrySet()) {
                String word = e2.getKey();
                double ftw = (double) e2.getIntValue();
                double fw = (double) wordFreq.getInt(word);
                double ft = (double) freqs.termFrequency.getInt(term);
                double pmi = (Math.log(ftw / ft / fw) + Math.log(N));// / (Math.log(ftw) - Math.log(N));
                pmis.put(word, pmis.getDouble(word) + pmi);
            }
        }

        // We should divide by the size of the term set, however this is not 
        // necessary to sort
        return pmis;
    }

    public double score(String _term) {
        String term = _term.toLowerCase();
        double pmi = 0.0;
        if (totalFreqs.containsKey(term)) {
            for (Object2IntMap.Entry<String> e2 : totalFreqs.get(term).object2IntEntrySet()) {
                String word = e2.getKey();
                double ftw = (double) e2.getIntValue();
                double fw = (double) wordFreq.getInt(word);
                double ft = (double) freqs.termFrequency.getInt(term);
                pmi += (Math.log(ftw / ft / fw) + Math.log(N));// / (Math.log(ftw) - Math.log(N));
                //System.err.println(String.format("%s (near %s) %.4f %.4f %.4f", word, term, ftw, fw, ft));
            }
        } else {
            //System.err.println("Term not found: " + term);
        }

        // We should divide by the size of the term set, however this is not 
        // necessary to sort
        return pmi;
    }

    private static Set<String> topN(final Object2DoubleMap<String> map, int n) {
        ArrayList<String> values = new ArrayList<>(map.keySet());
        values.sort(new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                int c = Double.compare(map.getDouble(o1), map.getDouble(o2));
                return c == 0 ? o1.compareTo(o2) : -c;
            }
        });
        //for (int i = 0; i < n && i < values.size(); i++) {
        //    System.err.println(String.format("%s %.4f", values.get(i), map.getDouble(values.get(i))));
        //}
        return new HashSet<>(values.subList(0, Math.min(n, values.size())));
    }

    private static void filterByWords(Set<String> words, Map<String, Object2IntMap<String>> totalFreqs) {
        for (Object2IntMap<String> m : totalFreqs.values()) {
            Iterator<Object2IntMap.Entry<String>> iter = m.object2IntEntrySet().iterator();
            while (iter.hasNext()) {
                if (!words.contains(iter.next().getKey())) {
                    iter.remove();
                }
            }
        }
    }

    private static class TopWordsTask implements Runnable {

        private final Document doc;
        private final ThreadLocal<Tokenizer> tokenizer;
        private final int maxLength;
        private final Set<String> topTerms;
        private final Map<String, Object2IntMap<String>> totalFreqs;
        private final ThreadLocal<POSTagger> tagger;
        private final Set<String> preceedingTokens;
        private final Set<String> middleTokens;
        private final Set<String> endTokens;
        private final boolean headTokenFinal;

        public TopWordsTask(Document doc, ThreadLocal<Tokenizer> tokenizer, int maxLength, Set<String> topTerms, Map<String, Object2IntMap<String>> totalFreqs, ThreadLocal<POSTagger> tagger, Set<String> preceedingTokens, Set<String> middleTokens, Set<String> endTokens, boolean headTokenFinal) {
            this.doc = doc;
            this.tokenizer = tokenizer;
            this.maxLength = maxLength;
            this.topTerms = topTerms;
            this.totalFreqs = totalFreqs;
            this.tagger = tagger;
            this.preceedingTokens = preceedingTokens;
            this.middleTokens = middleTokens;
            this.endTokens = endTokens;
            this.headTokenFinal = headTokenFinal;
        }

        @Override
        public void run() {
            final Map<String, Object2IntMap<String>> freq = new HashMap<>();
            String contents = doc.contents();
            for (String sentence : contents.split("\n")) {
                String[] tokens = tokenizer.get().tokenize(sentence.toLowerCase());
                String[] tags = tagger.get().tag(tokens);
                if (tokens.length > 0) {
                    for (int i = 0; i <= tokens.length - maxLength; i++) {
                        for (int j = i + 1; j <= i + maxLength; j++) {
                            String term = join(tokens, i, j);
                            if (topTerms.contains(term) && isTerm(tags, i, j)) {
                                if (!freq.containsKey(term)) {
                                    freq.put(term, new Object2IntOpenHashMap<String>());
                                }
                                Object2IntMap<String> freq2 = freq.get(term);
                                for (int c = Math.max(0, i - CONTEXT); c < Math.min(tokens.length, j + CONTEXT); c++) {
                                    if (c < i || c > j) {
                                        freq2.put(tokens[c], freq2.getInt(tokens[c]) + 1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            synchronized (totalFreqs) {
                for (Map.Entry<String, Object2IntMap<String>> e2 : freq.entrySet()) {
                    if (!totalFreqs.containsKey(e2.getKey())) {
                        totalFreqs.put(e2.getKey(), e2.getValue());
                    } else {
                        Object2IntMap<String> freq2 = totalFreqs.get(e2.getKey());
                        for (Object2IntMap.Entry<String> e : e2.getValue().object2IntEntrySet()) {
                            freq2.put(e.getKey(), freq2.getInt(e.getKey()) + e.getIntValue());
                        }
                    }
                }
            }
        }

        private boolean isTerm(String[] tags, int i, int j) {
            if (headTokenFinal) {
                if(!preceedingTokens.contains(tags[i]) && i - j > 1) return false;
                for(int k = i + 1; k < j-1; k++) {
                    if(!preceedingTokens.contains(tags[k]) && !middleTokens.contains(tags[k])) return false;
                }
                if(!endTokens.contains(tags[j-1])) return false;
                return true;
            } else {
                if(!endTokens.contains(tags[i])) return false;
                for(int k = i + 1; k < j-1; k++) {
                    if(!preceedingTokens.contains(tags[k]) && !middleTokens.contains(tags[k])) return false;
                }
                if(!preceedingTokens.contains(tags[j-1]) && i - j > 1) return false;
                return true;
                
            }
        }
    }

    private static String join(String[] s, int i, int j) {
        StringBuilder sb = new StringBuilder();
        for (int k = i; k < j; k++) {
            if (k != i) {
                sb.append(" ");
            }
            sb.append(s[k]);
        }
        return sb.toString();
    }
}
