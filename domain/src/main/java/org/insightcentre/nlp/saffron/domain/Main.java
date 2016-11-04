package org.insightcentre.nlp.saffron.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.documentindex.DocumentSearcher;
import org.insightcentre.nlp.saffron.documentindex.DocumentSearcherFactory;
import org.insightcentre.nlp.saffron.documentindex.SearchException;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Main {
    public static List<String> extractDomainModel(Configuration configuration, DocumentSearcher searcher) throws IOException, SearchException {
        Object2IntMap<String> wordFreq = new Object2IntOpenHashMap<>();
        Object2IntMap<Keyphrase> phraseFreq = new Object2IntOpenHashMap<>();

        Object2IntMap<NearbyPair> pairs = new Object2IntOpenHashMap<>();
        
        OpenNLPPOSExtractor posExtractor = configuration.loadPosExtractor();

        for(String s : searcher.allDocuments()) {
            posExtractor.processFile(s, wordFreq, phraseFreq, pairs, configuration.span);
        }
        
        Set<Keyphrase> keyphrases = filterPhrases(phraseFreq, configuration.minFreq,
            configuration.maxLength, configuration.loadStopWords());

        System.err.printf("Extracted %d keyphrases\n", keyphrases.size());

        Object2DoubleMap<String> rank = rankWords(wordFreq, keyphrases, phraseFreq, pairs, configuration.epsilon);

        return filterTopN(rank, configuration.n);
    }
   
   
   private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: "  + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }
  
    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {{
                accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                accepts("t", "The text corpus to load").withRequiredArg().ofType(File.class);
                accepts("o", "Where to write the domain model").withRequiredArg().ofType(File.class);
            }};
            final OptionSet os;
            
            try {
                os = p.parse(args);
            } catch(Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
 
            final File configuration = (File)os.valueOf("c");
            if(configuration == null || !configuration.exists()) {
                badOptions(p, "Configuration does not exist");
            }
            final File corpusFile  = (File)os.valueOf("t");
            if(corpusFile == null || !corpusFile.exists()) {

            }
            final File output = (File)os.valueOf("o");
            if(output == null) {
                badOptions(p, "Output not specified");
            }
            
            ObjectMapper mapper = new ObjectMapper();
            // Read configuration
            Configuration config = mapper.readValue(configuration, Configuration.class);
            Corpus corpus        = mapper.readValue(corpusFile, Corpus.class);
    
            DocumentSearcher searcher = DocumentSearcherFactory.loadSearcher(corpus);
            
            List<String> result = extractDomainModel(config, searcher);
            
            try(PrintWriter out = new PrintWriter(output)) {
                for(String r : result) {
                    out.println(r);
                }
            }
        } catch(Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    static Set<Keyphrase> filterPhrases(Object2IntMap<Keyphrase> phraseFreq, 
            int minFreq, int lengthMax, 
            Set<String> stopWords) {
        Set<Keyphrase> rval = new HashSet<>();
        for(Object2IntMap.Entry<Keyphrase> s : phraseFreq.object2IntEntrySet()) {
            if(s.getIntValue() > minFreq &&
                isProperTopic(s.getKey().phrase, stopWords) &&
                s.getKey().length > 1 && s.getKey().length <= lengthMax) {
                rval.add(s.getKey());
            }
        }
        return rval;
    }

    static Object2DoubleMap<String> rankWords(Object2IntMap<String> wordFreq, Set<Keyphrase> keyphrases, 
        Object2IntMap<Keyphrase> kpFreq, Object2IntMap<NearbyPair> pairs, double epsilon) {
        Object2DoubleMap<String> ranks = new Object2DoubleOpenHashMap<>();
        for(Object2IntMap.Entry<String> e : wordFreq.object2IntEntrySet()) {
            for(Keyphrase kp : keyphrases) {
                NearbyPair np = new NearbyPair(e.getKey(), kp);
                int npFreq = pairs.getInt(np);

                ranks.put(e.getKey(), ranks.getDouble(e.getKey()) +
                    pmi(npFreq, wordFreq.getInt(e.getKey()),
                        kpFreq.getInt(kp), epsilon));
            }
        }
        return ranks;
    }

    private static class ScoredString implements Comparable<ScoredString> {
        public final String s;
        public final double d;

        public ScoredString(String s, double d) {
            this.s = s;
            this.d = d;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + Objects.hashCode(this.s);
            hash = 97 * hash + (int) (Double.doubleToLongBits(this.d) ^ (Double.doubleToLongBits(this.d) >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ScoredString other = (ScoredString) obj;
            if (!Objects.equals(this.s, other.s)) {
                return false;
            }
            if (Double.doubleToLongBits(this.d) != Double.doubleToLongBits(other.d)) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(ScoredString o) {
            int i = Double.compare(d, o.d);
            if(i == 0) {
                return s.compareTo(o.s);
            }
            return i;
        }
       
    }
    
    private static List<String> filterTopN(Object2DoubleMap<String> rank, int n) {
        TreeSet<ScoredString> ts = new TreeSet<>();
        double min = Double.NEGATIVE_INFINITY;
        for(Object2DoubleMap.Entry<String> e : rank.object2DoubleEntrySet()) {
            if(ts.size() < n) {
                ts.add(new ScoredString(e.getKey(), e.getDoubleValue()));
            } else if(e.getDoubleValue() > min) {
                ts.pollFirst();
                ts.add(new ScoredString(e.getKey(), e.getDoubleValue()));
                min = ts.first().d;
            }
        }
        ArrayList<String> result = new ArrayList<>();
        for(ScoredString t : ts) {
            result.add(t.s);
        }
        return result;
    }


    public static boolean isProperTopic(String rootSequence, Set<String> stopWords) {
        String s = rootSequence;

        if (s.length() < 2) {
            return false;
        }
        // all words need to have at least 2 characters
        String[] words = s.split(" ");
        for(String word : words) {
            if(word.length() < 2) {
                return false;
            }
        }

        if (s.contains("- ") || s.contains(" -")) {
            return false;
        }
        final char[] chars = s.toCharArray();

        // first character must be alphabetic
        if (!Character.isAlphabetic(chars[0])) {
            return false;
        }
        if (!Character.isLetterOrDigit(chars[chars.length - 1])) {
            return false;
        }

        // first or last word not in stopwords
    	String firstWord = words[0].toLowerCase();
    	String lastWord = words[words.length-1].toLowerCase();
    	if (stopWords.contains(firstWord) || stopWords.contains(lastWord)) {
            return false;
        }

        // is alpha numeric
        for (int x = 0; x < chars.length; x++) {
            final char c = chars[x];
            if (!Character.isLetterOrDigit(c) && c != '-' && c != ' ') {
                return false;
            }
        }
        return true;
    }

    private static double pmi(int xy, int x, int y, double epsilon) {
        /// Note we don't need the exact value as this is only used for ranking
        /// therefore we don't try to count how often these could co=occur
        return (epsilon + xy) * Math.log((epsilon + xy) / (epsilon + x) / (epsilon + y));
    }

}
