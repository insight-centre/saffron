package org.insightcentre.nlp.saffron.term;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.lemmatizer.Lemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.insightcentre.nlp.saffron.DefaultSaffronListener;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration.Feature;
import org.insightcentre.nlp.saffron.data.*;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.index.SearchException;
import org.insightcentre.nlp.saffron.documentindex.CorpusTools;
import org.insightcentre.nlp.saffron.term.domain.DomainStats;
import org.insightcentre.nlp.saffron.term.lda.NovelTopicModel;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class TermExtraction {

    private final int nThreads;
    private final ThreadLocal<POSTagger> tagger;
    private final ThreadLocal<Tokenizer> tokenizer;
    private final int maxDocs;
    private final int minTermFreq;
    private final ThreadLocal<Lemmatizer> lemmatizer;
    private final Set<String> stopWords, preceedingsTokens, endTokens, middleTokens;
    private final int ngramMin, ngramMax;
    private final boolean headTokenFinal;
    private final TermExtractionConfiguration.WeightingMethod method;
    private final List<TermExtractionConfiguration.Feature> features;
    private final File refFile;
    private final int maxTerms;
    private final Feature keyFeature;
    private final Set<String> configBlacklist;
    private final boolean oneTermPerDoc;

    public TermExtraction(int nThreads, ThreadLocal<POSTagger> tagger, ThreadLocal<Tokenizer> tokenizer) {
        this.nThreads = nThreads;
        this.tagger = tagger;
        this.tokenizer = tokenizer;
        TermExtractionConfiguration config = new TermExtractionConfiguration();
        this.stopWords = new HashSet<>(Arrays.asList(TermExtractionConfiguration.ENGLISH_STOPWORDS));
        this.maxDocs = config.maxDocs;
        this.minTermFreq = config.minTermFreq;
        this.lemmatizer = null;
        this.preceedingsTokens = config.preceedingTokens;
        this.middleTokens = config.middleTokens;
        this.endTokens = config.headTokens;
        this.ngramMax = config.ngramMax;
        this.ngramMin = config.ngramMin;
        this.headTokenFinal = config.headTokenFinal;
        this.method = TermExtractionConfiguration.WeightingMethod.one;
        this.features = Arrays.asList(TermExtractionConfiguration.Feature.weirdness);
        this.refFile = null;
        this.maxTerms = 100;
        this.keyFeature = Feature.comboBasic;
        this.configBlacklist = Collections.EMPTY_SET;
        this.oneTermPerDoc = false;
    }

    public TermExtraction(int nThreads, ThreadLocal<POSTagger> tagger,
            ThreadLocal<Tokenizer> tokenizer, int maxDocs, int minTermFreq,
            ThreadLocal<Lemmatizer> lemmatizer, Set<String> stopWords,
            Set<String> preceedingsTokens, Set<String> endTokens, Set<String> middleTokens,
            int ngramMin, int ngramMax, boolean headTokenFinal,
            TermExtractionConfiguration.WeightingMethod method, List<Feature> features,
            File refFile, int maxTerms, Feature keyFeature, Set<String> blacklist,
            boolean oneTermPerDoc) {
        this.nThreads = nThreads;
        this.tagger = tagger;
        this.tokenizer = tokenizer;
        this.maxDocs = maxDocs;
        this.minTermFreq = minTermFreq;
        this.lemmatizer = lemmatizer;
        this.stopWords = stopWords;
        this.preceedingsTokens = preceedingsTokens;
        this.endTokens = endTokens;
        this.middleTokens = middleTokens;
        this.ngramMin = ngramMin;
        this.ngramMax = ngramMax;
        this.headTokenFinal = headTokenFinal;
        this.method = method;
        this.features = features;
        this.refFile = refFile;
        this.maxTerms = maxTerms;
        this.keyFeature = keyFeature;
        this.configBlacklist = blacklist;
        this.oneTermPerDoc = oneTermPerDoc;
    }

    public TermExtraction(final TermExtractionConfiguration config) throws IOException {
        this.nThreads = config.numThreads <= 0 ? 10 : config.numThreads;
        if (config.posModel == null) {
            throw new RuntimeException("Tagger must be set");
        }
        final POSModel posModel = new POSModel(config.posModel.toFile());
        this.tagger = new ThreadLocal<POSTagger>() {
            @Override
            protected POSTagger initialValue() {
                return new POSTaggerME(posModel);
            }
        };
        final TokenizerModel tokenizerModel;
        if (config.tokenizerModel == null) {
            tokenizerModel = null;
        } else {
            tokenizerModel = new TokenizerModel(config.tokenizerModel.toFile());
        }
        this.tokenizer = new ThreadLocal<Tokenizer>() {
            @Override
            protected Tokenizer initialValue() {

                if (config.tokenizerModel == null) {
                    return SimpleTokenizer.INSTANCE;
                } else {
                    return new TokenizerME(tokenizerModel);
                }
            }
        };
        this.maxDocs = config.maxDocs;
        this.minTermFreq = config.minTermFreq;
        if (config.lemmatizerModel == null) {
            this.lemmatizer = null;
        } else {
            final DictionaryLemmatizer dictLemmatizer = new DictionaryLemmatizer(config.lemmatizerModel.toFile());
            this.lemmatizer = new ThreadLocal<Lemmatizer>() {
                @Override
                protected Lemmatizer initialValue() {
                    return dictLemmatizer;
                }
            };
        }
        this.stopWords = config.stopWords == null
                ? new HashSet<>(Arrays.asList(TermExtractionConfiguration.ENGLISH_STOPWORDS))
                : readLineByLine(config.stopWords);
        this.preceedingsTokens = config.preceedingTokens;
        this.middleTokens = config.middleTokens;
        this.endTokens = config.headTokens;
        this.ngramMax = config.ngramMax;
        this.ngramMin = config.ngramMin;
        this.headTokenFinal = config.headTokenFinal;
        this.method = config.method;
        this.features = config.features;
        assert (!this.features.isEmpty());
        this.refFile = config.corpus == null ? null : config.corpus.toFile();
        this.maxTerms = config.maxTerms;
        this.keyFeature = config.baseFeature;
        this.configBlacklist = config.blacklist == null ? new HashSet<>() : config.blacklist;
        if(config.blacklistFile != null) {
            loadBlacklistFromFile (this.configBlacklist, config.blacklistFile);
        }
        this.oneTermPerDoc = config.oneTermPerDoc;
    }

    private static HashSet<String> readLineByLine(SaffronPath p) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(p.toFile()));
        HashSet<String> set = new HashSet<>();
        String s;
        while ((s = r.readLine()) != null) {
            set.add(s);
        }
        return set;
    }

    public FrequencyStats extractStats(Corpus searcher,
            ConcurrentLinkedQueue<DocumentTerm> docTerms,
            CasingStats casing, Set<String> blackList)
            throws SearchException, InterruptedException, ExecutionException {
        ExecutorService service = new ThreadPoolExecutor(nThreads, nThreads, 0,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());
        final FrequencyStats summary = new FrequencyStats();

        int docCount = 0;
        for (Document doc : searcher.getDocuments()) {
            service.submit(new TermExtractionTask(doc, tagger, lemmatizer, tokenizer,
                    stopWords, ngramMin, ngramMax, preceedingsTokens, middleTokens, endTokens,
                    headTokenFinal,
                    summary, docTerms, casing, lowercaseAll(blackList)));
            if (docCount++ > maxDocs) {
                break;
            }
        }

        service.shutdown();
        service.awaitTermination(2, TimeUnit.DAYS);
        summary.filter(minTermFreq);
        return summary;
    }

    private Object2DoubleMap<String> scoreByFeat(List<String> terms, final TermExtractionConfiguration.Feature feature,
            final FrequencyStats stats, final Lazy<FrequencyStats> ref,
            final Lazy<InclusionStats> incl, final Lazy<NovelTopicModel> ntm,
            final Lazy<DomainStats> domain, final Set<String> whiteList) {
        final Object2DoubleMap<String> scores = new Object2DoubleOpenHashMap<>();
        for (String term : terms) {
            if (whiteList.contains(term)) {
                scores.put(term, Double.POSITIVE_INFINITY);
            } else {
                scores.put(term,
                        Features.calcFeature(feature, term, stats, ref, incl, ntm, domain));
            }
        }
        return scores;

    }

    private void rankTermsByFeat(List<String> terms, final Object2DoubleMap<String> scores,
            Set<String> whiteList, Set<String> blackList) {
        terms.removeIf((String t) -> blackList.contains(t));

        terms.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (whiteList.contains(o1)) {
                    if (whiteList.contains(o2)) {
                        return -Double.compare(scores.getDouble(o1), scores.getDouble(o2));
                    } else {
                        return -1;
                    }
                } else {
                    if (whiteList.contains(o2)) {
                        return +1;
                    } else {
                        boolean proper1 = isProperTerm(o1, stopWords);
                        boolean proper2 = isProperTerm(o2, stopWords);
                        if (proper1 == proper2) {
                            return -Double.compare(scores.getDouble(o1), scores.getDouble(o2));
                        } else if (proper1) {
                            return -1;
                        } else {
                            return +1;
                        }
                    }
                }
            }
        });
    }

    public Result extractTerms(final Corpus searcher) {
        return extractTerms(searcher, new DefaultSaffronListener());
    }

    public Result extractTerms(final Corpus searcher, SaffronListener log) {
        return extractTerms(searcher, new HashSet<>(), new HashSet<>(), log);

    }

    public Result extractTerms(final Corpus searcher, final Set<String> whiteList, final Set<String> blackList, SaffronListener log) {
        blackList.addAll(configBlacklist);
        try {
            final ConcurrentLinkedQueue<DocumentTerm> dts = new ConcurrentLinkedQueue<>();
            final CasingStats casing = new CasingStats();
            final FrequencyStats freqs = extractStats(searcher, dts, casing, blackList);
            Lazy<FrequencyStats> ref = new Lazy<FrequencyStats>() {
                @Override
                protected FrequencyStats init() {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        if (refFile.getName().endsWith("json.gz")) {
                            return mapper.readValue(
                                    new GZIPInputStream(new FileInputStream(refFile)),
                                    FrequencyStats.class);
                        } else if (refFile.getName().endsWith(".json")) {
                            return mapper.readValue(refFile, FrequencyStats.class);
                        } else if (refFile.getName().endsWith(".zip")) {
                            return extractStats(CorpusTools.fromZIP(refFile), null, null, blackList);
                        } else if (refFile.getName().endsWith(".tar.gz")) {
                            return extractStats(CorpusTools.fromTarball(refFile), null, null, blackList);
                        } else {
                            throw new IllegalArgumentException("Could not deduce type of background corpus");
                        }
                    } catch (IOException | SearchException | InterruptedException | ExecutionException x) {
                        x.printStackTrace();
                        return null;
                    }
                }
            };
            final Lazy<InclusionStats> incl = new Lazy<InclusionStats>() {
                @Override
                protected InclusionStats init() {
                    return new InclusionStats(freqs.docFrequency);
                }
            };
            Lazy<NovelTopicModel> ntm = new Lazy<NovelTopicModel>() {

                @Override
                protected NovelTopicModel init() {
                    try {
                        return NovelTopicModel.initialize(searcher, tokenizer);
                    } catch (IOException | SearchException x) {
                        x.printStackTrace();
                        return null;
                    }
                }
            };
            Lazy<DomainStats> domain = new Lazy<DomainStats>() {

                @Override
                protected DomainStats init() {
                    try {
                        return DomainStats.initialize(searcher, nThreads, tokenizer, ngramMax, maxDocs, freqs, incl.get(), stopWords, tagger, preceedingsTokens, middleTokens, endTokens, headTokenFinal);
                    } catch (SearchException x) {
                        x.printStackTrace();
                        return null;
                    }
                }
            };
            List<String> terms = new ArrayList<>(freqs.docFrequency.keySet());
            for (String whiteListTerm : whiteList) {
                if (!freqs.docFrequency.containsKey(whiteListTerm)) {
                    terms.add(whiteListTerm);
                }
            }
            switch (method) {
                case one:
                    Object2DoubleMap<String> scores = scoreByFeat(terms, keyFeature,
                            freqs, ref, incl, ntm, domain, whiteList);
                    rankTermsByFeat(terms, scores, whiteList, blackList);
                    if (terms.size() > maxTerms) {
                        if (oneTermPerDoc) {
                            terms = getTopTerms(terms, maxTerms, dts);
                        } else {
                            terms = terms.subList(0, maxTerms);
                        }
                    }
                    return new Result(convertToTerms(terms, freqs, scores, casing, whiteList, stopWords),
                            filterTerms(terms, dts, casing, stopWords));
                case voting:
                    Object2DoubleMap<String> voting = new Object2DoubleOpenHashMap<>();
                    for (Feature feat : features) {
                        Object2DoubleMap<String> scores2 = scoreByFeat(terms, feat,
                                freqs, ref, incl, ntm, domain, whiteList);
                        rankTermsByFeat(terms, scores2, whiteList, blackList);
                        int i = 1;
                        for (String term : terms) {
                            voting.put(term, voting.getDouble(term) + 1.0 / i++);
                        }
                    }
                    rankTermsByFeat(terms, voting, whiteList, blackList);
                    if (terms.size() > maxTerms) {
                        if (oneTermPerDoc) {
                            terms = getTopTerms(terms, maxTerms, dts);
                        } else {
                            terms = terms.subList(0, maxTerms);
                        }
                    }
                    return new Result(convertToTerms(terms, freqs, voting, casing, whiteList, stopWords),
                            filterTerms(terms, dts, casing, stopWords));
                default:
                    throw new UnsupportedOperationException("TODO");
            }
        } catch (SearchException | ExecutionException | InterruptedException x) {
            throw new RuntimeException(x);
        }
    }

    private static List<DocumentTerm> filterTerms(List<String> ts,
            ConcurrentLinkedQueue<DocumentTerm> dts,
            CasingStats casing, Set<String> stopWords) {
        Set<String> ts2 = new HashSet<>(ts);
        Set<DocumentTerm> rval = new HashSet<>();
        for (DocumentTerm dt : dts) {
            if (ts2.contains(dt.getTermString())) { // && isProperTerm(dt.term_string, stopWords)) {
                rval.add(new DocumentTerm(dt.getDocumentId(),
                        casing.trueCase(dt.getTermString()),
                        dt.getOccurrences(), dt.getPattern(), dt.getAcronym(), dt.getTfIdf()));
            }
        }
        return new ArrayList<DocumentTerm>(rval);
    }

    private static boolean isProperTerm(String rootSequence, Set<String> stopWords) {
        String s = rootSequence;

        if (s.length() < 2) {
            return false;
        }
        // all words need to have at least 2 characters
        String[] words = s.split(" ");
        if (minimumWordLength(words) < 2) {
            return false;
        }

        if (s.contains("- ") || s.contains(" -")) {
            return false;
        }
        final char[] chars = s.toCharArray();

        // first character must be alphabetic
        if (!isAlpha(chars[0])) {
            return false;
        }
        if (!isAlphaNumeric(chars[chars.length - 1])) {
            return false;
        }

        // first or last word not in stopwords
        String firstWord = decapitalize(words[0]);
        String lastWord = decapitalize(words[words.length - 1]);
        if (stopWords.contains(firstWord) || stopWords.contains(lastWord)) {
            return false;
        }

        // is alpha numeric
        for (int x = 0; x < chars.length; x++) {
            final char c = chars[x];
            if (!isAlphaNumeric(c) && c != '-' && c != ' ') {
                return false;
            }
        }
        return true;
    }

    private static int minimumWordLength(String[] words) {
        Integer minLength = Integer.MAX_VALUE;

        for (String word : words) {
            if (minLength > word.length()) {
                minLength = word.length();
            }
        }
        return minLength;
    }

    private static boolean isAlpha(final char c) {
        return Character.isLetter(c);
    }

    private static boolean isAlphaNumeric(final char c) {
        return Character.isLetterOrDigit(c);
    }

    private static String decapitalize(String s) {
        if (s.length() <= 1) {
            return s.toLowerCase();
        } else {
            if (Character.isUpperCase(s.charAt(0)) && Character.isUpperCase(s.charAt(1))) {
                return s;
            } else {
                char[] c = s.toCharArray();
                c[0] = Character.toLowerCase(c[0]);
                return new String(c);
            }
        }
    }

    private static Set<Term> convertToTerms(List<String> ts, FrequencyStats stats,
            Object2DoubleMap<String> scores, CasingStats casing, Set<String> whiteList,
            Set<String> stopWords) {
        Set<Term> terms = new HashSet<>();
        for (String t : ts) {
            final Term term = new Term(casing.trueCase(t),
                    stats.termFrequency.getInt(t),
                    stats.docFrequency.getInt(t), scores.getDouble(t),
                    Collections.EMPTY_LIST,
                    Status.none.toString());
            terms.add(term);
            if (whiteList.contains(t)) {
                term.setStatus(Status.accepted);
            }
        }
        return terms;
    }

    private static Set<String> lowercaseAll(Set<String> blacklist) {
        Set<String> ss = new HashSet<>();
        for (String s : blacklist) {
            ss.add(s.toLowerCase());
        }
        return ss;
    }

    private List<String> getTopTerms(List<String> terms, int maxTerms, ConcurrentLinkedQueue<DocumentTerm> dts) {
        Set<String> docs = new HashSet<>();
        Map<String, Set<String>> term2doc = new HashMap<>();
        for (DocumentTerm dt : dts) {
            docs.add(dt.getDocumentId());
            if (!term2doc.containsKey(dt.getTermString())) {
                term2doc.put(dt.getTermString(), new HashSet<>());
            }
            term2doc.get(dt.getTermString()).add(dt.getDocumentId());
        }
        List<String> acceptedTerms = new ArrayList<>();
        for (String term : terms) {
            Set<String> d = term2doc.get(term);
            if (acceptedTerms.size() < maxTerms) {
                if (d != null) {
                    docs.removeAll(d);
                }
                acceptedTerms.add(term);
            } else if (docs.isEmpty()) {
                return acceptedTerms;
            } else {
                if (d != null) {
                    d.retainAll(docs);
                    if (!d.isEmpty()) {
                        docs.removeAll(d);
                        acceptedTerms.add(term);
                    }
                }
            }
        }
        return acceptedTerms;
    }

    private void loadBlacklistFromFile(Set<String> configBlacklist, SaffronPath blacklistFile) {
        try(BufferedReader r = new BufferedReader(new FileReader(blacklistFile.toFile()))) {
            String line;
            while((line = r.readLine()) != null) {
                configBlacklist.add(line);
            }
        } catch(IOException x) {
            System.err.println("Could not load black list file");
            throw new RuntimeException(x);
        }
    }

    public static class Result {

        public Set<Term> terms;
        public List<DocumentTerm> docTerms;

        public Result(Set<Term> terms, List<DocumentTerm> docTerms) {
            this.terms = terms;
            this.docTerms = docTerms;
        }

        /**
         * Update the result to give a more normal distribution of term scores.
         */
        public void normalize() {
            double[] values = new double[terms.size()];
            int i = 0;
            for (Term t : terms) {
                values[i++] = t.getScore();
            }
            LogGap normalizer = LogGap.makeModel(values);
            for (Term t : terms) {
                t.setScore(normalizer.normalize(t.getScore()));
            }
        }

        @Override
        public String toString() {
            return "Result{" + "terms=" + terms + ", docTerms=" + docTerms + '}';
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.terms);
            hash = 53 * hash + Objects.hashCode(this.docTerms);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Result other = (Result) obj;
            if (!Objects.equals(this.terms, other.terms)) {
                return false;
            }
            if (!Objects.equals(this.docTerms, other.docTerms)) {
                return false;
            }
            return true;
        }

    }

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("c", "The configuration to use").withRequiredArg().ofType(File.class);
                    accepts("x", "The corpus index to read").withRequiredArg().ofType(File.class);
                    accepts("t", "The terms to write").withRequiredArg().ofType(File.class);
                    accepts("o", "The doc-term corespondences to write").withRequiredArg().ofType(File.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }

            ObjectMapper mapper = new ObjectMapper();

            if (os.valueOf("c") == null) {
                badOptions(p, "Configuration is required");
                return;
            }
            if (os.valueOf("x") == null) {
                badOptions(p, "Corpus is required");
                return;
            }
            if (os.valueOf("t") == null) {
                badOptions(p, "Output for Terms is required");
                return;
            }
            if (os.valueOf("o") == null) {
                badOptions(p, "Output for Doc-Terms is required");
                return;
            }
            Configuration c = mapper.readValue((File) os.valueOf("c"), Configuration.class);
            File corpusFile = (File) os.valueOf("x");
            final Corpus searcher = CorpusTools.readFile(corpusFile);

            final TermExtraction te = new TermExtraction(c.termExtraction);

            final Result r = te.extractTerms(searcher);
            r.normalize();

            mapper.writeValue((File) os.valueOf("t"), r.terms);
            mapper.writeValue((File) os.valueOf("o"), r.docTerms);

        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }

    }
}
