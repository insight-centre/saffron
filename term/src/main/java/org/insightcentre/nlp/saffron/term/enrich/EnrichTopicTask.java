package org.insightcentre.nlp.saffron.term.enrich;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import opennlp.tools.lemmatizer.Lemmatizer;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.tokenize.Tokenizer;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.term.FrequencyStats;

/**
 * A task to enrich topics based on a single document
 *
 * @author John McCrae
 */
public class EnrichTopicTask implements Runnable {

    private final Document doc;
    private final ThreadLocal<POSTagger> tagger;
    private final ThreadLocal<Lemmatizer> lemmatizer;
    private final ThreadLocal<Tokenizer> tokenizer;
    private final FrequencyStats stats = new FrequencyStats();
    private final FrequencyStats summary;
    private final Set<String> topicStrings;
    private final ConcurrentLinkedQueue<DocumentTopic> finalDocTopics;
    private final HashMap<String, DocumentTopic> docTopics = new HashMap<>();

    public EnrichTopicTask(Document doc, ThreadLocal<POSTagger> tagger, ThreadLocal<Lemmatizer> lemmatizer, ThreadLocal<Tokenizer> tokenizer, FrequencyStats summary, Set<String> topicStrings, ConcurrentLinkedQueue<DocumentTopic> docTopics) {
        this.doc = doc;
        this.tagger = tagger;
        this.lemmatizer = lemmatizer;
        this.tokenizer = tokenizer;
        this.summary = summary;
        this.topicStrings = topicStrings;
        this.finalDocTopics = docTopics;
    }

    @Override
    public void run() {
        try {
            String contents = doc.contents();
            System.err.println(doc.id);
            for (String sentence : contents.split("\n")) {
                String[] tokens;
                try {
                    tokens = tokenizer.get().tokenize(sentence);
                } catch (Exception x) {
                    System.err.println(sentence);
                    throw x;
                }
                if (tokens.length > 0) {
                    String[] tags = tagger == null ? null : tagger.get().tag(tokens);

                    if (tags != null && tags.length != tokens.length) {
                        throw new RuntimeException("Tagger did not return same number of tokens as tokenizer");
                    }
                    String[] lemmas = lemmatizer == null ? tokens : lemmatizer.get().lemmatize(tokens, tags);

                    for (int i = 0; i < tokens.length; i++) {
                        for (int j = i; j < tokens.length; j++) {
                            String topicCandidate = join(tokens, i, j);
                            if (topicStrings.contains(topicCandidate.toLowerCase())) {
                                processTopic(topicCandidate);
                            } else if(lemmatizer != null) {
                                topicCandidate = join(lemmas, i, j);
                                if (topicStrings.contains(topicCandidate.toLowerCase())) {
                                    processTopic(topicCandidate);
                                }
                            }
                        }
                    }
                }
                stats.tokens += tokens.length;
            }

            stats.documents = 1;

            synchronized (summary) {
                summary.add(stats);
            }
            for(DocumentTopic dt : docTopics.values()) {
                finalDocTopics.add(dt);
            }
        } catch (Exception x) {
            x.printStackTrace();
        } 
    }

    private void processTopic(String topicCandidate) {
        stats.termFrequency.put(topicCandidate, stats.termFrequency.getInt(topicCandidate) + 1);
        stats.docFrequency.put(topicCandidate, 1);
        docTopics.put(topicCandidate, new DocumentTopic(doc.id, topicCandidate, stats.docFrequency.getInt(topicCandidate), null, null, null));

    }

    public static String join(String[] tokens, int i, int j) {
        StringBuilder term = new StringBuilder();
        for (int x = i; x <= j; x++) {
            if (x != i) {
                term.append(" ");
            }
            term.append(tokens[x]);
        }
        return term.toString();

    }

}
