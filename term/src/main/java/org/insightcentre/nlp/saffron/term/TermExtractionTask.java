package org.insightcentre.nlp.saffron.term;

import static java.lang.Integer.min;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.tokenize.Tokenizer;
import org.insightcentre.nlp.saffron.data.Document;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class TermExtractionTask implements Callable<FrequencyStats> {

    private final Document doc;
    private final POSTagger tagger;
    private final Tokenizer tokenizer;
    private final Set<String> stopWords;
    private final int ngramMin;
    private final int ngramMax;
    private final FrequencyStats stats = new FrequencyStats();
    private final Set<String> preceedingTokens;
    private final Set<String> endTokens;

    private static final String[] ENGLISH_STOPWORDS = new String[]{"i",
        "me",
        "my",
        "myself",
        "we",
        "our",
        "ours",
        "ourselves",
        "you",
        "your",
        "yours",
        "yourself",
        "yourselves",
        "he",
        "him",
        "his",
        "himself",
        "she",
        "her",
        "hers",
        "herself",
        "it",
        "its",
        "itself",
        "they",
        "them",
        "their",
        "theirs",
        "themselves",
        "what",
        "which",
        "who",
        "whom",
        "this",
        "that",
        "these",
        "those",
        "am",
        "is",
        "are",
        "was",
        "were",
        "be",
        "been",
        "being",
        "have",
        "has",
        "had",
        "having",
        "do",
        "does",
        "did",
        "doing",
        "a",
        "an",
        "the",
        "and",
        "but",
        "if",
        "or",
        "because",
        "as",
        "until",
        "while",
        "of",
        "at",
        "by",
        "for",
        "with",
        "about",
        "against",
        "between",
        "into",
        "through",
        "during",
        "before",
        "after",
        "above",
        "below",
        "to",
        "from",
        "up",
        "down",
        "in",
        "out",
        "on",
        "off",
        "over",
        "under",
        "again",
        "further",
        "then",
        "once",
        "here",
        "there",
        "when",
        "where",
        "why",
        "how",
        "all",
        "any",
        "both",
        "each",
        "few",
        "more",
        "most",
        "other",
        "some",
        "such",
        "no",
        "nor",
        "not",
        "only",
        "own",
        "same",
        "so",
        "than",
        "too",
        "very",
        "s",
        "t",
        "can",
        "will",
        "just",
        "don",
        "should",
        "now",
        "d",
        "ll",
        "m",
        "o",
        "re",
        "ve",
        "y",
        "ain",
        "aren",
        "couldn",
        "didn",
        "doesn",
        "hadn",
        "hasn",
        "haven",
        "isn",
        "ma",
        "mightn",
        "mustn",
        "needn",
        "shan",
        "shouldn",
        "wasn",
        "weren",
        "won",
        "wouldn"
    };

    public TermExtractionTask(Document doc, POSTagger tagger, Tokenizer tokenizer, Set<String> stopWords, int ngramMin, int ngramMax, Set<String> preceedingTokens, Set<String> endTokens) {
        this.doc = doc;
        this.tagger = tagger;
        this.tokenizer = tokenizer;
        this.stopWords = stopWords;
        this.ngramMin = ngramMin;
        this.ngramMax = ngramMax;
        this.preceedingTokens = preceedingTokens;
        this.endTokens = endTokens;
    }

    public TermExtractionTask(Document doc, POSTagger tagger, Tokenizer tokenizer) {
        this.doc = doc;
        this.tagger = tagger;
        this.tokenizer = tokenizer;
        this.ngramMin = 1;
        this.ngramMax = 3;
        this.preceedingTokens = new HashSet<>(Arrays.asList("NN", "NNS", "JJ", "NNP", "IN"));
        this.endTokens = new HashSet<>(Arrays.asList("NN", "NNS"));
        this.stopWords =  new HashSet<>(Arrays.asList(ENGLISH_STOPWORDS));
    }

    @Override
    public FrequencyStats call() throws Exception {
        String[] tokens = tokenizer.tokenize(doc.contents());
        String[] tags = tagger.tag(tokens);

        if (tags.length != tokens.length) {
            throw new RuntimeException("Tagger did not return same number of tokens as tokenizer");
        }

        for (int i = 0; i < tokens.length; i++) {
            boolean nonStop = false;
            for (int j = i + ngramMin - 1; j < min(i + ngramMax, tokens.length); j++) {
                if (!stopWords.contains(tokens[j])) {
                    nonStop = true;
                }
                if (endTokens.contains(tags[j]) && nonStop) {
                    processTerm(tokens, i, j);
                }
                if (!preceedingTokens.contains(tags[j])) {
                    break;
                }
            }
        }

        stats.tokens = tokens.length;
        stats.documents = 1;

        return stats;
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
