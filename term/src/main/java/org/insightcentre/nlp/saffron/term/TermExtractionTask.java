package org.insightcentre.nlp.saffron.term;

import static java.lang.Integer.min;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import opennlp.tools.lemmatizer.Lemmatizer;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.tokenize.Tokenizer;
import org.insightcentre.nlp.saffron.data.Document;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class TermExtractionTask implements Runnable {

    private final Document doc;
    private final ThreadLocal<POSTagger> tagger;
    private final ThreadLocal<Lemmatizer> lemmatizer;
    private final Tokenizer tokenizer;
    private final Set<String> stopWords;
    private final int ngramMin;
    private final int ngramMax;
    private final FrequencyStats stats = new FrequencyStats();
    private final Set<String> preceedingTokens;
    private final Set<String> endTokens;
    private final FrequencyStats summary;
    private final boolean headTokenFinal;

    public TermExtractionTask(Document doc, ThreadLocal<POSTagger> tagger, 
            ThreadLocal<Lemmatizer> lemmatizer,
            Tokenizer tokenizer,
            Set<String> stopWords, int ngramMin, int ngramMax,
            Set<String> preceedingTokens, Set<String> endTokens,
            boolean headTokenFinal,
            FrequencyStats summary) {
        this.doc = doc;
        this.tagger = tagger;
        this.lemmatizer = lemmatizer;
        this.tokenizer = tokenizer;
        this.stopWords = stopWords;
        this.ngramMin = ngramMin;
        this.ngramMax = ngramMax;
        this.preceedingTokens = preceedingTokens;
        this.endTokens = endTokens;
        this.summary = summary;
        this.headTokenFinal = headTokenFinal;
    }
    
    @Override
    public void run() {
        try {
            String contents = doc.contents();
            System.err.println(doc.id);
            for (String sentence : contents.split("\n")) {
                String[] tokens = tokenizer.tokenize(sentence);
                if (tokens.length > 0) {
                    String[] tags = new String[0];
                    tags = tagger.get().tag(tokens);

                    if (tags.length != tokens.length) {
                        throw new RuntimeException("Tagger did not return same number of tokens as tokenizer");
                    }

                    for (int i = 0; i < tokens.length; i++) {
                        boolean nonStop = false;
                        boolean headSeen = false;
                        for (int j = i + ngramMin - 1; j < min(i + ngramMax, tokens.length); j++) {
                            if (!stopWords.contains(tokens[j])) {
                                nonStop = true;
                            }
                            if(headTokenFinal) {
                                if (endTokens.contains(tags[j]) && nonStop) {
                                    if(lemmatizer != null && lemmatizer.get() != null) {
                                        String[] lemmas = lemmatizer.get().lemmatize(tokens, tags);
                                        String[] tokens2 = Arrays.copyOfRange(tokens, i, j+1);
                                        tokens2[tokens2.length-1] = lemmas[j];
                                        processTerm(tokens2, 0, j - i);
                                    } else {
                                        processTerm(tokens, i, j);
                                    }
                                }
                                if (!preceedingTokens.contains(tags[j])) {
                                    break;
                                }
                            } else {
                                headSeen = headSeen || endTokens.contains(tags[j]);
                                if (preceedingTokens.contains(tags[j]) && nonStop && headSeen) {
                                    if(lemmatizer != null && lemmatizer.get() != null) {
                                        String[] lemmas = lemmatizer.get().lemmatize(tokens, tags);
                                        String[] tokens2 = Arrays.copyOfRange(tokens, i, j+1);
                                        tokens2[0] = lemmas[i];
                                        processTerm(tokens2, 0, j - i);
                                    } else {
                                        processTerm(tokens, i, j);
                                    }
                                }
                                if(!endTokens.contains(tags[j])) {
                                    break;
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
        } catch (Exception x) {
            x.printStackTrace();
        }
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

    private void processTerm(String[] tokens, int i, int j) {
        String termStr = join(tokens, i, j);
        stats.docFrequency.put(termStr, 1);
        stats.termFrequency.put(termStr, 1 + stats.termFrequency.getInt(termStr));
    }

}
