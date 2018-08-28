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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.lemmatizer.Lemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration.Feature;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.index.CorpusAsDocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.SearchException;
import org.insightcentre.nlp.saffron.documentindex.CorpusTools;
import org.insightcentre.nlp.saffron.term.domain.DomainStats;
import org.insightcentre.nlp.saffron.term.lda.NovelTopicModel;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class TermExtraction {

    private final int nThreads;
    private final ThreadLocal<POSTagger> tagger;
    private final Tokenizer tokenizer;
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

    public TermExtraction(int nThreads, ThreadLocal<POSTagger> tagger, Tokenizer tokenizer) {
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
    }

    public TermExtraction(final TermExtractionConfiguration config) throws IOException {
        this.nThreads = config.numThreads <= 0 ? 10 : config.numThreads;
        if (config.posModel == null) {
            throw new RuntimeException("Tagger must be set");
        }
        this.tagger = new ThreadLocal<POSTagger>() {
            @Override
            protected POSTagger initialValue() {
                try {
                    return new POSTaggerME(new POSModel(config.posModel.toFile()));
                } catch (IOException x) {
                    x.printStackTrace();
                    return null;
                }
            }
        };
        if (config.tokenizerModel == null) {
            this.tokenizer = SimpleTokenizer.INSTANCE;
        } else {
            this.tokenizer = new TokenizerME(new TokenizerModel(config.tokenizerModel.toFile()));
        }
        this.maxDocs = config.maxDocs;
        this.minTermFreq = config.minTermFreq;
        if (config.lemmatizerModel == null) {
            this.lemmatizer = null;
        } else {
            this.lemmatizer = new ThreadLocal<Lemmatizer>() {
                @Override
                protected Lemmatizer initialValue() {
                    try {
                        return new DictionaryLemmatizer(config.lemmatizerModel.toFile());
                    } catch (IOException x) {
                        x.printStackTrace();
                        return null;
                    }
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

    public FrequencyStats extractStats(DocumentSearcher searcher,
            ConcurrentLinkedQueue<DocumentTopic> docTopics,
            CasingStats casing)
            throws SearchException, InterruptedException, ExecutionException {
        ExecutorService service = new ThreadPoolExecutor(nThreads, nThreads, 0,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());
        final FrequencyStats summary = new FrequencyStats();

        int docCount = 0;
        for (Document doc : searcher.allDocuments()) {
            service.submit(new TermExtractionTask(doc, tagger, lemmatizer, tokenizer,
                    stopWords, ngramMin, ngramMax, preceedingsTokens, middleTokens, endTokens,
                    headTokenFinal,
                    summary, docTopics, casing));
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
            final Lazy<DomainStats> domain) {
        final Object2DoubleMap<String> scores = new Object2DoubleOpenHashMap<>();
        for (String topic : topics) {
            scores.put(topic,
                    Features.calcFeature(feature, topic, stats, ref, incl, ntm, domain));
        }
        return scores;

    }

    private void rankTopicsByFeat(List<String> topics, final Object2DoubleMap<String> scores) {
        topics.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return -Double.compare(scores.getDouble(o1), scores.getDouble(o2));
            }
        });
    }

    public Result extractTopics(final DocumentSearcher searcher) {
        try {
            final ConcurrentLinkedQueue<DocumentTopic> dts = new ConcurrentLinkedQueue<>();
            final CasingStats casing = new CasingStats();
            final FrequencyStats freqs = extractStats(searcher, dts, casing);
            Lazy<FrequencyStats> ref = new Lazy<FrequencyStats>() {
                @Override
                protected FrequencyStats init() {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        if(refFile.getName().endsWith("json.gz")) {
                            return mapper.readValue(
                                    new GZIPInputStream(new FileInputStream(refFile)), 
                                    FrequencyStats.class);
                        } else if(refFile.getName().endsWith(".json")) {
                            return mapper.readValue(refFile, FrequencyStats.class);
                        } else if(refFile.getName().endsWith(".zip")) {
                            return extractStats(new CorpusAsDocumentSearcher(CorpusTools.fromZIP(refFile)), null, null);
                        } else if(refFile.getName().endsWith(".tar.gz")) {
                            return extractStats(new CorpusAsDocumentSearcher(CorpusTools.fromTarball(refFile)), null, null);
                        } else {
                            throw new IllegalArgumentException("Could not deduce type of background corpus");
                        }
                    } catch (IOException|SearchException|InterruptedException|ExecutionException x) {
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
                    } catch(IOException|SearchException x) {
                        x.printStackTrace();
                        return null;
                    }
                }
            };
            Lazy<DomainStats> domain = new Lazy<DomainStats>() {

                @Override
                protected DomainStats init() {
                    try {
                        return DomainStats.initialize(searcher, nThreads, tokenizer, ngramMax, maxDocs, freqs, incl.get());
                    } catch(SearchException x) {
                        x.printStackTrace();
                        return null;
                    }
                }
            };
            List<String> topics = new ArrayList<>(freqs.docFrequency.keySet());
            switch (method) {
                case one:
                    Object2DoubleMap<String> scores = scoreByFeat(topics, keyFeature,
                            freqs, ref, incl, ntm, domain);
                    rankTopicsByFeat(topics, scores);
                    if (topics.size() > maxTopics) {
                        topics = topics.subList(0, maxTopics);
                    }
                    return new Result(convertToTopics(topics, freqs, scores, casing),
                            filterTopics(topics, dts, casing));
                case voting:
                    Object2DoubleMap<String> voting = new Object2DoubleOpenHashMap<>();
                    for (Feature feat : features) {
                        Object2DoubleMap<String> scores2 = scoreByFeat(topics, feat,
                                freqs, ref, incl, ntm, domain);
                        rankTopicsByFeat(topics, scores2);
                        int i = 1;
                        for (String topic : topics) {
                            voting.put(topic, voting.getDouble(topic) + 1.0 / i++);
                        }
                    }
                    rankTopicsByFeat(topics, voting);
                    if (topics.size() > maxTopics) {
                        topics = topics.subList(0, maxTopics);
                    }
                    return new Result(convertToTopics(topics, freqs, voting, casing),
                            filterTopics(topics, dts, casing));
                default:
                    throw new UnsupportedOperationException("TODO");
            }
        } catch (SearchException | ExecutionException | InterruptedException x) {
            throw new RuntimeException(x);
        }
    }

    private static List<DocumentTopic> filterTopics(List<String> ts, 
            ConcurrentLinkedQueue<DocumentTopic> dts,
            CasingStats casing) {
        Set<String> ts2 = new HashSet<>(ts);
        List<DocumentTopic> rval = new ArrayList<>();
        for(DocumentTopic dt : dts) {
            if(ts2.contains(dt.topic_string)) {
                rval.add(new DocumentTopic(dt.document_id, 
                        casing.trueCase(dt.topic_string),
                        dt.occurrences, dt.pattern, dt.acronym, dt.tfidf));
            }
        }
        return rval;
    }
    
    private static Set<Topic> convertToTopics(List<String> ts, FrequencyStats stats,
            Object2DoubleMap<String> scores, CasingStats casing) {
        Set<Topic> topics = new HashSet<>();
        for (String t : ts) {
            topics.add(new Topic(casing.trueCase(t), 
                    stats.termFrequency.getInt(t),
                    stats.docFrequency.getInt(t), scores.getDouble(t),
                    Collections.EMPTY_LIST));
        }
        return topics;
    }

    public static class Result {

        public Set<Topic> topics;
        public List<DocumentTopic> docTopics;

        public Result(Set<Topic> topics, List<DocumentTopic> docTopics) {
            this.topics = topics;
            this.docTopics = docTopics;
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

}
