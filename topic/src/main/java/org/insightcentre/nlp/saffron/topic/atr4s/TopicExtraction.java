package org.insightcentre.nlp.saffron.topic.atr4s;

import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.SearchException;
import org.insightcentre.nlp.saffron.topic.tfidf.TFIDF;
import ru.ispras.atr.candidates.TCCConfig;
import ru.ispras.atr.candidates.TermCandidatesCollector;
import ru.ispras.atr.candidates.TermOccurrencesCollectorConfig;
import ru.ispras.atr.datamodel.DSDataset;
import ru.ispras.atr.datamodel.TermCandidate;
import ru.ispras.atr.datamodel.TermOccurrence;
import ru.ispras.atr.features.FeatureConfig;
import ru.ispras.atr.features.contexts.DomainCoherence;
import ru.ispras.atr.features.contexts.PostRankDC;
import ru.ispras.atr.features.keyrel.DocKeysExtractorConfig;
import ru.ispras.atr.features.keyrel.KeyConceptRelatedness;
import ru.ispras.atr.features.keyrel.NormWord2VecAdapterConfig;
import ru.ispras.atr.features.occurrences.AvgTermFrequency;
import ru.ispras.atr.features.occurrences.Basic;
import ru.ispras.atr.features.occurrences.CValue;
import ru.ispras.atr.features.occurrences.ComboBasic;
import ru.ispras.atr.features.occurrences.ResidualIDF;
import ru.ispras.atr.features.occurrences.TotalTFIDF;
import ru.ispras.atr.features.refcorpus.DomainPertinence;
import ru.ispras.atr.features.refcorpus.ReferenceCorpusConfig;
import ru.ispras.atr.features.refcorpus.Relevance;
import ru.ispras.atr.features.refcorpus.Weirdness;
import ru.ispras.atr.features.tm.NovelTopicModel;
import ru.ispras.atr.features.wiki.LinkProbability;
import ru.ispras.atr.preprocess.EmoryNLPPreprocessorConfig;
import ru.ispras.atr.preprocess.NLPPreprocessor;
import ru.ispras.atr.rank.OneFeatureTCWeighterConfig;
import ru.ispras.atr.rank.PUTCWeighterConfig;
import ru.ispras.atr.rank.TermCandidatesWeighter;
import ru.ispras.atr.rank.VotingTCWeighterConfig;
import ru.ispras.pu4spark.LogisticRegressionConfig;
import ru.ispras.pu4spark.TraditionalPULearnerConfig;
import scala.Tuple2;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration.Feature;

/**
 * Extract Topics using ATR4S
 * @author John McCrae <john@mccr.ae>
 */
public class TopicExtraction {
    private final TermExtractionConfiguration config;
    private final NLPPreprocessor nlpPreprocessor;
    private final TermCandidatesCollector candidatesCollector;
    private final TermCandidatesWeighter candidatesWeighter;
    private final double threshold;
    private final int maxTopics;

    public TopicExtraction(TermExtractionConfiguration config) {
        this.config = config;
        this.nlpPreprocessor =  EmoryNLPPreprocessorConfig.make().build();
        this.candidatesCollector = TCCConfig.make(range(config.ngramMin, config.ngramMax), 
            config.minTermFreq, TermOccurrencesCollectorConfig.make()).build();
        switch(config.method) {
            case one:
                this.candidatesWeighter = new OneFeatureTCWeighterConfig(mkFeat(config.baseFeature, config)).build();
                break;
            case voting:
                this.candidatesWeighter = VotingTCWeighterConfig.make(mkFeats(config)).build();
                break;
            case puatr:
                this.candidatesWeighter = PUTCWeighterConfig.make(mkFeat(config.baseFeature, config), 
                        100, mkFeats(config), new TraditionalPULearnerConfig(0.05, 1, new LogisticRegressionConfig(100, 1.0e-8, 0.0))).build();
            default:
                throw new IllegalArgumentException("Unknown method: " + config.method);
        }
        this.threshold = config.threshold;
        this.maxTopics = config.maxTopics;
    }
    
    private static FeatureConfig mkFeat(Feature name, TermExtractionConfiguration config) {
        switch(name) {
                case weirdness: 
                    return new Weirdness(ReferenceCorpusConfig.apply(config.corpus, 1e-3));
                case avgTermFreq:
                    return new AvgTermFrequency();
                case residualIdf:
                    return new ResidualIDF();
                case totalTfIdf:
                    return new TotalTFIDF();
                case cValue:
                    return CValue.make();
                case basic:
                    return Basic.make();
                case comboBasic:
                    return new ComboBasic(0.75, 0.1);
                case postRankDC:
                    return PostRankDC.make();
                case relevance:
                    return new Relevance(ReferenceCorpusConfig.apply(config.corpus, 1e-3));
                case domainPertinence:
                    return new DomainPertinence(ReferenceCorpusConfig.apply(config.corpus, 1e-3), 0.1);
                case domainCoherence:
                    return DomainCoherence.make();
                case novelTopicModel:
                    return NovelTopicModel.make();
                case linkProbability:
                    return new LinkProbability(0.018, config.infoMeasure);
                case keyConceptRelatedness:
                    //return KeyConceptRelatedness.make();
                    
                    return new KeyConceptRelatedness(DocKeysExtractorConfig.make(), 
                            500, 2, 1, 15, new NormWord2VecAdapterConfig(config.w2vmodelPath, true, 0)
                    );
                default:
                    throw new IllegalArgumentException("Bad feature name for ATR4S: " + name);
            }
    }
    
    private static List<FeatureConfig> mkFeats(TermExtractionConfiguration config) {
        List<FeatureConfig> feats = new ArrayList<>();
        for(Feature name : config.features) {
            feats.add(mkFeat(name, config));
        }
        return feats;
        
    
    }
    
    public static class Result {
        public Set<Topic> topics;
        public List<DocumentTopic> docTopics;      

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
    
    public Result extractTopics(DocumentSearcher searcher) {
        Result result = new Result();
        result.topics = new HashSet<>();
        result.docTopics = new ArrayList<>();
        try {
            List<Tuple2<String, String>> docs = new ArrayList<>();
            for(Document doc : searcher.allDocuments()) {
                docs.add(new Tuple2<>(doc.getId(), doc.contents()));
            }
            DSDataset dataset = nlpPreprocessor.preprocess(JavaConversions.iterableAsScalaIterable(docs).toSeq());
            Seq<TermCandidate> candidates = candidatesCollector.collect(dataset);
            Map<String,TermCandidate> candmap = new HashMap<>();
            for(TermCandidate tc : JavaConversions.asJavaIterable(candidates)) {
                candmap.put(tc.canonicalRepr(), tc);
            }
            Iterable<Tuple2<String,Object>> sortedTerms = JavaConversions.asJavaIterable(
                    candidatesWeighter.weightAndSort(candidates, dataset));
            int i = 0;
            System.err.println("Top 10 terms");
            for(Tuple2<String,Object> term : sortedTerms) {
                final Double score = (Double)term._2;
                if(i < 10)
                    System.err.printf("%s %.4f\n", term._1, score);
                if(score > threshold && i++ < maxTopics) {
                    TermCandidate tc = candmap.get(term._1);
                    
                    //Seq<String> lemmas = tc.lemmas();
                    Set<String> lemmas = new HashSet<>();
                    Map<String,Integer> docOccurs = new HashMap<>();
                    int occurrences = 0;
                    for(TermOccurrence to : JavaConversions.asJavaIterable(tc.occurrences())) {
                        StringBuilder sb = new StringBuilder();
                        for(String l : JavaConversions.asJavaIterable(to.lemmas())) {
                            if(sb.length() != 0)
                                sb.append(" ");
                            sb.append(l);
                        }
                        lemmas.add(sb.toString());
                        if(docOccurs.containsKey(to.docName()))
                            docOccurs.put(to.docName(), docOccurs.get(to.docName()) + 1);
                        else
                            docOccurs.put(to.docName(), 1);
                        occurrences++;
                    }
                    
                    String lemma1 = tc.canonicalRepr().replaceAll("_", " ");
                    List<Topic.MorphologicalVariation> morphVars = new ArrayList<>();
                    for(String lemma : lemmas) {
                        morphVars.add(new Topic.MorphologicalVariation(lemma));
                    }
                    morphVars.add(new Topic.MorphologicalVariation(lemma1));
                    
                    result.topics.add(new Topic(lemma1, occurrences, docOccurs.size(), score, morphVars));
                    for(Map.Entry<String, Integer> docOcc : docOccurs.entrySet()) {
                        result.docTopics.add(new DocumentTopic(docOcc.getKey(), lemma1, docOcc.getValue(), null, null, null));
                    }
                } /*else {
                    System.err.println("Ignoring " + term._1);
                }*/
            }
            
        } catch(SearchException x) {
            throw new RuntimeException(x);
        }
        TFIDF.addTfidf(result.docTopics);
        return result;
    }
    
    private List<Object> range(int i, int j) {
        List<Object> ints = new ArrayList<>();
        for(int k = i; k < j; k++) {
            ints.add(k);
        }
        return ints;
    }
    
}
