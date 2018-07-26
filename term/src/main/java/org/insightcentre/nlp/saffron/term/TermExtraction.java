package org.insightcentre.nlp.saffron.term;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
    private final ThreadLocal<POSTagger> tagger;
    private final Tokenizer tokenizer;

    public TermExtraction(int nThreads, ThreadLocal<POSTagger> tagger, Tokenizer tokenizer) {
        this.nThreads = nThreads;
        this.tagger = tagger;
        this.tokenizer = tokenizer;
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
                } catch(IOException x) {
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
    }

    public FrequencyStats extractStats(DocumentSearcher searcher) throws SearchException, InterruptedException, ExecutionException {
        ExecutorService service = new ThreadPoolExecutor(nThreads, nThreads, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1000));
        final FrequencyStats summary = new FrequencyStats();

        for (Document doc : searcher.allDocuments()) {
            service.submit(new TermExtractionTask(doc, tagger, tokenizer, summary));
        }

        service.shutdown();
        service.awaitTermination(2, TimeUnit.DAYS);
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
