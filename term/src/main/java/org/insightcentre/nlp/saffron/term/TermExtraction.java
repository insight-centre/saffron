package org.insightcentre.nlp.saffron.term;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.SearchException;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class TermExtraction {
    private final int nThreads;
    private final POSTagger tagger;
    private final Tokenizer tokenizer;

    public TermExtraction(int nThreads, POSTagger tagger, Tokenizer tokenizer) {
        this.nThreads = nThreads;
        this.tagger = tagger;
        this.tokenizer = tokenizer;
    }
    
    public TermExtraction(TermExtractionConfiguration config) throws IOException {
        this.nThreads = config.numThreads <= 0 ? 10 : config.numThreads;
        if(config.posModel == null)
                throw new RuntimeException("Tagger must be set");
        this.tagger = new POSTaggerME(new POSModel(config.posModel.toFile()));
        if(config.tokenizerModel == null) {
            this.tokenizer = SimpleTokenizer.INSTANCE;
        } else {
            this.tokenizer = new TokenizerME(new TokenizerModel(config.tokenizerModel.toFile()));
        }
    }

    public FrequencyStats extractStats(DocumentSearcher searcher) throws SearchException, InterruptedException, ExecutionException {
        ExecutorService service = Executors.newFixedThreadPool(nThreads);
        ArrayList<Future<FrequencyStats>> stats = new ArrayList<>();
        for(Document doc : searcher.allDocuments()) {
            stats.add(service.submit(new TermExtractionTask(doc, tagger, tokenizer)));
        }
        FrequencyStats summary = new FrequencyStats();
        while(!stats.isEmpty()) {
            Iterator<Future<FrequencyStats>> i = stats.iterator();
            while(i.hasNext()) {
                Future<FrequencyStats> s = i.next();
                if(s.isDone()) {
                    summary.add(s.get());
                    i.remove();
                }
            }
        }
        service.shutdown();
        return summary;
    }
    
    public Result extractTopics(DocumentSearcher searcher) throws SearchException, InterruptedException, ExecutionException {
        
        throw new UnsupportedOperationException("TODO");

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

}
