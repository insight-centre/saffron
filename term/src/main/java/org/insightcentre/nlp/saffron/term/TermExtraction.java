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
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
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
    private final int maxTopics;
    private final Feature keyFeature;
    private final Set<String> blacklist;
    private final boolean oneTopicPerDoc;

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
        this.maxTopics = 100;
        this.keyFeature = Feature.comboBasic;
        this.blacklist = Collections.EMPTY_SET;
        this.oneTopicPerDoc = false;
    }

    public TermExtraction(int nThreads, ThreadLocal<POSTagger> tagger, 
            ThreadLocal<Tokenizer> tokenizer, int maxDocs, int minTermFreq, 
            ThreadLocal<Lemmatizer> lemmatizer, Set<String> stopWords, 
            Set<String> preceedingsTokens, Set<String> endTokens, Set<String> middleTokens, 
            int ngramMin, int ngramMax, boolean headTokenFinal, 
            TermExtractionConfiguration.WeightingMethod method, List<Feature> features, 
            File refFile, int maxTopics, Feature keyFeature, Set<String> blacklist, 
            boolean oneTopicPerDoc) {
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
        this.maxTopics = maxTopics;
        this.keyFeature = keyFeature;
        this.blacklist = blacklist;
        this.oneTopicPerDoc = oneTopicPerDoc;
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
        this.maxTopics = config.maxTopics;
        this.keyFeature = config.baseFeature;
        this.blacklist = config.blacklist;
        this.oneTopicPerDoc = config.oneTopicPerDoc;
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
            ConcurrentLinkedQueue<DocumentTopic> docTopics,
            CasingStats casing)
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
                    summary, docTopics, casing, lowercaseAll(blacklist)));
            if (docCount++ > maxDocs) {
                break;
            }
        }

        service.shutdown();
        service.awaitTermination(2, TimeUnit.DAYS);
        summary.filter(minTermFreq);
        return summary;
    }

    private Object2DoubleMap<String> scoreByFeat(List<String> topics, final TermExtractionConfiguration.Feature feature,
            final FrequencyStats stats, final Lazy<FrequencyStats> ref,
            final Lazy<InclusionStats> incl, final Lazy<NovelTopicModel> ntm,
            final Lazy<DomainStats> domain, final Set<String> whiteList) {
        final Object2DoubleMap<String> scores = new Object2DoubleOpenHashMap<>();
        for (String topic : topics) {
            if(whiteList.contains(topic)) {
                scores.put(topic, Double.POSITIVE_INFINITY);
            } else {
                scores.put(topic,
                    Features.calcFeature(feature, topic, stats, ref, incl, ntm, domain));
            }
        }
        return scores;

    }

    private void rankTopicsByFeat(List<String> topics, final Object2DoubleMap<String> scores,
            Set<String> whiteList, Set<String> blackList) {
        topics.removeIf((String t) -> blackList.contains(t));

        topics.sort(new Comparator<String>() {
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
                        return -Double.compare(scores.getDouble(o1), scores.getDouble(o2));
                    }
                }
            }
        });
    }

    public Result extractTopics(final Corpus searcher) {
        return extractTopics(searcher, new DefaultSaffronListener());
    }

    public Result extractTopics(final Corpus searcher, SaffronListener log) {
        return extractTopics(searcher, Collections.EMPTY_SET, Collections.EMPTY_SET, log);

    }

    public Result extractTopics(final Corpus searcher, final Set<String> whiteList, final Set<String> blackList, SaffronListener log) {
        try {
            final ConcurrentLinkedQueue<DocumentTopic> dts = new ConcurrentLinkedQueue<>();
            final CasingStats casing = new CasingStats();
            final FrequencyStats freqs = extractStats(searcher, dts, casing);
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
                            return extractStats(CorpusTools.fromZIP(refFile), null, null);
                        } else if (refFile.getName().endsWith(".tar.gz")) {
                            return extractStats(CorpusTools.fromTarball(refFile), null, null);
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
            List<String> topics = new ArrayList<>(freqs.docFrequency.keySet());
            for(String whiteListTopic : whiteList) {
                if(!freqs.docFrequency.containsKey(whiteListTopic)) {
                    topics.add(whiteListTopic);
                }
            }
            switch (method) {
                case one:
                    Object2DoubleMap<String> scores = scoreByFeat(topics, keyFeature,
                            freqs, ref, incl, ntm, domain, whiteList);
                    rankTopicsByFeat(topics, scores, whiteList, blackList);
                    if (topics.size() > maxTopics) {
                        if (oneTopicPerDoc) {
                            topics = getTopTopics(topics, maxTopics, dts);
                        } else {
                            topics = topics.subList(0, maxTopics);
                        }
                    }
                    return new Result(convertToTopics(topics, freqs, scores, casing, whiteList),
                            filterTopics(topics, dts, casing, stopWords));
                case voting:
                    Object2DoubleMap<String> voting = new Object2DoubleOpenHashMap<>();
                    for (Feature feat : features) {
                        Object2DoubleMap<String> scores2 = scoreByFeat(topics, feat,
                                freqs, ref, incl, ntm, domain, whiteList);
                        rankTopicsByFeat(topics, scores2, whiteList, blackList);
                        int i = 1;
                        for (String topic : topics) {
                            voting.put(topic, voting.getDouble(topic) + 1.0 / i++);
                        }
                    }
                    rankTopicsByFeat(topics, voting, whiteList, blackList);
                    if (topics.size() > maxTopics) {
                        if (oneTopicPerDoc) {
                            topics = getTopTopics(topics, maxTopics, dts);
                        } else {
                            topics = topics.subList(0, maxTopics);
                        }
                    }
                    return new Result(convertToTopics(topics, freqs, voting, casing, whiteList),
                            filterTopics(topics, dts, casing, stopWords));
                default:
                    throw new UnsupportedOperationException("TODO");
            }
        } catch (SearchException | ExecutionException | InterruptedException x) {
            throw new RuntimeException(x);
        }
    }

    private static List<DocumentTopic> filterTopics(List<String> ts,
            ConcurrentLinkedQueue<DocumentTopic> dts,
            CasingStats casing, Set<String> stopWords) {
        Set<String> ts2 = new HashSet<>(ts);
        List<DocumentTopic> rval = new ArrayList<>();
        for (DocumentTopic dt : dts) {
            if (ts2.contains(dt.topic_string) && isProperTopic(dt.topic_string, stopWords)) {
                rval.add(new DocumentTopic(dt.document_id,
                        casing.trueCase(dt.topic_string),
                        dt.occurrences, dt.pattern, dt.acronym, dt.tfidf));
            }
        }
        return rval;
    }

    private static boolean isProperTopic(String rootSequence, Set<String> stopWords) {
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

    private static Set<Topic> convertToTopics(List<String> ts, FrequencyStats stats,
            Object2DoubleMap<String> scores, CasingStats casing, Set<String> whiteList) {
        Set<Topic> topics = new HashSet<>();
        for (String t : ts) {
            final Topic topic = new Topic(casing.trueCase(t),
                    stats.termFrequency.getInt(t),
                    stats.docFrequency.getInt(t), scores.getDouble(t),
                    Collections.EMPTY_LIST);
            topics.add(topic);
            if(whiteList.contains(t)) {
                topic.status = Status.accepted;
            }
        }
        return topics;
    }

    private static Set<String> lowercaseAll(Set<String> blacklist) {
        Set<String> ss = new HashSet<>();
        for (String s : blacklist) {
            ss.add(s.toLowerCase());
        }
        return ss;
    }

    private List<String> getTopTopics(List<String> topics, int maxTopics, ConcurrentLinkedQueue<DocumentTopic> dts) {
        Set<String> docs = new HashSet<>();
        Map<String, Set<String>> topic2doc = new HashMap<>();
        for (DocumentTopic dt : dts) {
            docs.add(dt.document_id);
            if (!topic2doc.containsKey(dt.topic_string)) {
                topic2doc.put(dt.topic_string, new HashSet<>());
            }
            topic2doc.get(dt.topic_string).add(dt.document_id);
        }
        List<String> acceptedTopics = new ArrayList<>();
        for (String topic : topics) {
            Set<String> d = topic2doc.get(topic);
            if (acceptedTopics.size() < maxTopics) {
                if (d != null) {
                    docs.removeAll(d);
                }
                acceptedTopics.add(topic);
            } else if (docs.isEmpty()) {
                return acceptedTopics;
            } else {
                if (d != null) {
                    d.retainAll(docs);
                    if (!d.isEmpty()) {
                        docs.removeAll(d);
                        acceptedTopics.add(topic);
                    }
                }
            }
        }
        return acceptedTopics;
    }

    public static class Result {

        public Set<Topic> topics;
        public List<DocumentTopic> docTopics;

        public Result(Set<Topic> topics, List<DocumentTopic> docTopics) {
            this.topics = topics;
            this.docTopics = docTopics;
        }

        /**
         * Update the result to give a more normal distribution of topic scores.
         */
        public void normalize() {
            double[] values = new double[topics.size()];
            int i = 0;
            for(Topic t : topics) {
                values[i++] = t.score;
            }
            LogGap normalizer = LogGap.makeModel(values);
            for(Topic t : topics) {
                t.score = normalizer.normalize(t.score);
            }
        }

        @Override
        public String toString() {
            return "Result{" + "topics=" + topics + ", docTopics=" + docTopics + '}';
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.topics);
            hash = 53 * hash + Objects.hashCode(this.docTopics);
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
            if (!Objects.equals(this.topics, other.topics)) {
                return false;
            }
            if (!Objects.equals(this.docTopics, other.docTopics)) {
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
                    accepts("t", "The topics to write").withRequiredArg().ofType(File.class);
                    accepts("o", "The doc-topic corespondences to write").withRequiredArg().ofType(File.class);
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
                badOptions(p, "Output for Topics is required");
                return;
            }
            if (os.valueOf("o") == null) {
                badOptions(p, "Output for Doc-Topics is required");
                return;
            }
            Configuration c = mapper.readValue((File) os.valueOf("c"), Configuration.class);
            File corpusFile = (File) os.valueOf("x");
            final Corpus searcher = CorpusTools.readFile(corpusFile);

            final TermExtraction te = new TermExtraction(c.termExtraction);

            final Result r = te.extractTopics(searcher);
            r.normalize();

            mapper.writeValue((File) os.valueOf("t"), r.topics);
            mapper.writeValue((File) os.valueOf("o"), r.docTopics);

        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
        }

    }
}
