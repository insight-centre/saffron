package org.insightcentre.nlp.saffron.term.enrich;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import opennlp.tools.lemmatizer.Lemmatizer;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.tokenize.Tokenizer;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.term.FrequencyStats;
import org.insightcentre.nlp.saffron.term.enrich.EnrichTopics.WordTrie;

/**
 * A task to enrich terms based on a single document
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
    private final WordTrie termStrings;
    private final ConcurrentLinkedQueue<DocumentTerm> finalDocTerms;
    private final HashMap<String, DocumentTerm> docTerms = new HashMap<>();

    public EnrichTopicTask(Document doc, ThreadLocal<POSTagger> tagger, ThreadLocal<Lemmatizer> lemmatizer, ThreadLocal<Tokenizer> tokenizer, FrequencyStats summary, WordTrie termStrings, ConcurrentLinkedQueue<DocumentTerm> docTerms) {
        this.doc = doc;
        this.tagger = tagger;
        this.lemmatizer = lemmatizer;
        this.tokenizer = tokenizer;
        this.summary = summary;
        this.termStrings = termStrings;
        this.finalDocTerms = docTerms;
    }

    @Override
    public void run() {
        List<WordTrie> tries = new ArrayList<>();
        try {
            String contents = doc.contents().toLowerCase();
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
                        List<WordTrie> tries2 = updateTries(tries, tokens, i);
                        if(lemmatizer != null) {
                            tries2.addAll(updateTries(tries, lemmas, i));
                        }
                        for(WordTrie t : tries2) {
                            if(t.present) {
                                processTerm(t.word);
                            }
                        }
                        tries = tries2;
                    }
                }
                stats.tokens += tokens.length;
            }

            stats.documents = 1;

            synchronized (summary) {
                summary.add(stats);
            }
            for(DocumentTerm dt : docTerms.values()) {
                finalDocTerms.add(dt);
            }
        } catch (Exception x) {
            x.printStackTrace();
        } 
    }

    private List<WordTrie> updateTries(final List<WordTrie> tries, final String[] tokens, final int i) {
        List<WordTrie> tries2 = new ArrayList<>();
        for(WordTrie trie : tries) {
            WordTrie t = trie.get(tokens[i]);
            if(t != null) {
                tries2.add(t);
            }
        }
        if(termStrings.containsKey(tokens[i])) {
            tries2.add(termStrings.get(tokens[i]));
        }
        return tries2;
    }

    private void processTerm(String termCandidate) {
        stats.termFrequency.put(termCandidate, stats.termFrequency.getInt(termCandidate) + 1);
        stats.docFrequency.put(termCandidate, 1);
        docTerms.put(termCandidate, new DocumentTerm(doc.id, termCandidate, stats.termFrequency.getInt(termCandidate), null, null, null));

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
