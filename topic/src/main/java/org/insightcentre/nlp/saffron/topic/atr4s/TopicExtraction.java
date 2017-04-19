package org.insightcentre.nlp.saffron.topic.atr4s;

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
import ru.ispras.atr.candidates.TCCConfig;
import ru.ispras.atr.candidates.TermCandidatesCollector;
import ru.ispras.atr.candidates.TermOccurrencesCollectorConfig;
import ru.ispras.atr.datamodel.DSDataset;
import ru.ispras.atr.datamodel.TermCandidate;
import ru.ispras.atr.datamodel.TermOccurrence;
import ru.ispras.atr.features.FeatureConfig;
import ru.ispras.atr.features.refcorpus.ReferenceCorpusConfig;
import ru.ispras.atr.features.refcorpus.Weirdness;
import ru.ispras.atr.preprocess.EmoryNLPPreprocessorConfig;
import ru.ispras.atr.preprocess.NLPPreprocessor;
import ru.ispras.atr.rank.OneFeatureTCWeighterConfig;
import ru.ispras.atr.rank.TermCandidatesWeighter;
import scala.Tuple2;
import scala.collection.JavaConversions;
import scala.collection.Seq;

/**
 * Extract Topics using ATR4S
 * @author John McCrae <john@mccr.ae>
 */
public class TopicExtraction {
    private final Main.Configuration config;
    private final NLPPreprocessor nlpPreprocessor;
    private final TermCandidatesCollector candidatesCollector;
    private final TermCandidatesWeighter candidatesWeighter;
    private final double threshold;
    private final int maxTopics;

    public TopicExtraction(Main.Configuration config) {
        this.config = config;
        this.nlpPreprocessor =  EmoryNLPPreprocessorConfig.make().build();
        this.candidatesCollector = TCCConfig.make(range(config.ngramMin, config.ngramMax), 
            config.minTermFreq, TermOccurrencesCollectorConfig.make()).build();
        if(config.method.equals("one")) {
            this.candidatesWeighter = new OneFeatureTCWeighterConfig(mkFeats(config).get(0)).build();
        } else {
            throw new IllegalArgumentException("Unknown method: " + config.method);
        }
        this.threshold = config.threshold;
        this.maxTopics = config.maxTopics;
    }
    
    private static List<FeatureConfig> mkFeats(Main.Configuration config) {
        List<FeatureConfig> feats = new ArrayList<>();
        for(String name : config.features) {
            if(name.equals("weirdness")) {
                feats.add(new Weirdness(ReferenceCorpusConfig.apply(config.corpus, 1e-3)));
            } else {
                throw new IllegalArgumentException();
            }
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
                docs.add(new Tuple2<>(doc.getId(), doc.getContents()));
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
                    Seq<String> lemmas = tc.lemmas();
                    Map<String,Integer> docOccurs = new HashMap<>();
                    int occurrences = 0;
                    for(TermOccurrence to : JavaConversions.asJavaIterable(tc.occurrences())) {
                        if(docOccurs.containsKey(to.docName()))
                            docOccurs.put(to.docName(), docOccurs.get(to.docName()) + 1);
                        else
                            docOccurs.put(to.docName(), 1);
                        occurrences++;
                    }
                    String lemma1 = lemmas.apply(0).toLowerCase();
                    List<Topic.MorphologicalVariation> morphVars = new ArrayList<>();
                    for(String lemma : JavaConversions.asJavaIterable(lemmas)) {
                        morphVars.add(new Topic.MorphologicalVariation(lemma));
                    }
                    
                    result.topics.add(new Topic(lemma1, occurrences, docOccurs.size(), score, morphVars));
                    for(Map.Entry<String, Integer> docOcc : docOccurs.entrySet()) {
                        result.docTopics.add(new DocumentTopic(docOcc.getKey(), lemma1, docOcc.getValue(), null, null));
                    }
                }
            }
            
        } catch(SearchException x) {
            throw new RuntimeException(x);
        }
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
