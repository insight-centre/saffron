package org.insightcentre.nlp.saffron.term.domain;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayPriorityQueue;
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
import opennlp.tools.tokenize.Tokenizer;
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

    public static DomainStats initialize(DocumentSearcher searcher, int nThreads,
            Tokenizer tokenizer, int maxLength, int maxDocs,
            FrequencyStats stats, InclusionStats incl) throws SearchException {
        Map<String, Object2IntMap<String>> totalFreqs = totalFreqs(searcher, nThreads, tokenizer, maxLength, maxDocs, stats, incl);
        Set<String> words = topWords(stats, totalFreqs);
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

    private static Map<String, Object2IntMap<String>> totalFreqs(DocumentSearcher searcher, int nThreads,
            Tokenizer tokenizer, int maxLength, int maxDocs,
            FrequencyStats stats, InclusionStats incl) throws SearchException {
        ExecutorService service = new ThreadPoolExecutor(nThreads, nThreads, 0,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());

        Set<String> topTerms = new HashSet<>(topTerms(stats, incl));
        Map<String, Object2IntMap<String>> totalFreqs = new HashMap<>();

        int docCount = 0;
        for (Document doc : searcher.allDocuments()) {
            service.submit(new TopWordsTask(doc, tokenizer, maxLength, topTerms, totalFreqs));
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

    private static Set<String> topWords(FrequencyStats stats, Map<String, Object2IntMap<String>> totalFreqs) {
        Object2DoubleMap<String> pmis = calcPMI(totalFreqs, stats);
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
                double pmi = (Math.log(ftw / ft / fw) + Math.log(N)) / (Math.log(ftw) - Math.log(N));
                pmis.put(word, pmis.getDouble(word) + pmi);
            }
        }

        // We should divide by the size of the term set, however this is not 
        // necessary to sort
        return pmis;
    }

    public double score(String term) {
        double pmi = 0.0;
        if (totalFreqs.containsKey(term)) {
            for (Object2IntMap.Entry<String> e2 : totalFreqs.get(term).object2IntEntrySet()) {
                String word = e2.getKey();
                double ftw = (double) e2.getIntValue();
                double fw = (double) wordFreq.getInt(word);
                double ft = (double) freqs.termFrequency.getInt(term);
                pmi += (Math.log(ftw / ft / fw) + Math.log(N)) / (Math.log(ftw) - Math.log(N));
            }
        }

        // We should divide by the size of the term set, however this is not 
        // necessary to sort
        return pmi;
    }

    private static Set<String> topN(final Object2DoubleMap<String> map, int n) {
        ObjectArrayPriorityQueue<String> queue = new ObjectArrayPriorityQueue<>(n, new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                int c = Double.compare(map.getDouble(o1), map.getDouble(o2));
                return c == 0 ? o1.compareTo(o2) : -c;
            }
        });
        for (String s : map.keySet()) {
            queue.enqueue(s);
            queue.trim();
        }
        Set<String> l = new HashSet<>();
        while (!queue.isEmpty()) {
            l.add(queue.dequeue());
        }
        return l;
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
        private final Tokenizer tokenizer;
        private final int maxLength;
        private final Set<String> topTerms;
        private final Map<String, Object2IntMap<String>> totalFreqs;

        public TopWordsTask(Document doc, Tokenizer tokenizer, int maxLength, Set<String> topTerms, Map<String, Object2IntMap<String>> totalFreqs) {
            this.doc = doc;
            this.tokenizer = tokenizer;
            this.maxLength = maxLength;
            this.topTerms = topTerms;
            this.totalFreqs = totalFreqs;
        }

        @Override
        public void run() {
            final Map<String, Object2IntMap<String>> freq = new HashMap<>();
            String contents = doc.contents();
            for (String sentence : contents.split("\n")) {
                String[] tokens = tokenizer.tokenize(sentence);
                if (tokens.length > 0) {
                    for (int i = 0; i <= tokens.length - maxLength; i++) {
                        for (int j = i + 1; j <= i + maxLength; j++) {
                            String term = join(tokens, i, j);
                            if (topTerms.contains(term)) {
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
