package org.insightcentre.nlp.saffron.domain;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import static java.lang.Math.max;
import static java.lang.Math.min;
import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class OpenNLPPOSExtractor {
    private final POSTagger tagger;
    private final Tokenizer tokenizer;
    private final Chunker chunker;

    public OpenNLPPOSExtractor(File tokenizerModelFile, File posModelFile, File chunkModelFile) throws IOException {
        this.tagger = new POSTaggerME(new POSModel(posModelFile));
        this.tokenizer = new TokenizerME(new TokenizerModel(tokenizerModelFile));
        this.chunker = new ChunkerME(new ChunkerModel(chunkModelFile));
    }

    public OpenNLPPOSExtractor(POSTagger tagger, Tokenizer tokenizer, Chunker chunker) {
        this.tagger = tagger;
        this.tokenizer = tokenizer;
        this.chunker = chunker;
    }

    public void processFile(String documentText, Object2IntMap<String> wordFreq, 
        Object2IntMap<Keyphrase> phraseFreq, Object2IntMap<NearbyPair> pairs,
        int span) {
        String[] tokens = tokenizer.tokenize(documentText.toLowerCase());
        String[] tags = tagger.tag(tokens);
        String[] chunks = chunker.chunk(tokens, tags);

        assert(tags.length == tokens.length);
        
        for(int i = 0; i < tokens.length; i++) {
            if(tags[i].startsWith("N") ||
               tags[i].startsWith("V") ||
               tags[i].startsWith("J")) {
                wordFreq.put(tokens[i], wordFreq.getInt(tokens[i]) + 1);
            }
        }

        StringBuilder currentNP = new StringBuilder();
        int currentNPlen = 0;
        for(int i = 0; i < chunks.length; i++) {
            if(chunks[i].equals("B-NP")) {
                if(currentNPlen > 0) {
                    addNP(currentNP.toString(), i - currentNPlen, currentNPlen, tokens, tags, phraseFreq, pairs, span);
                    currentNP.delete(0, currentNP.length());
                }
                currentNP.append(tokens[i]);
                currentNPlen = 1;
            } else if(chunks[i].equals("I-NP")) {
                currentNP.append(" ").append(tokens[i]);
                currentNPlen++;
            } else if(currentNPlen > 0) {
                addNP(currentNP.toString(), i - currentNPlen, currentNPlen, tokens, tags, phraseFreq, pairs, span);
                currentNPlen = 0;
                currentNP.delete(0, currentNP.length());
            }
        }
        if(currentNPlen > 0) {
            addNP(currentNP.toString(), chunks.length - currentNPlen, currentNPlen, tokens, tags, phraseFreq, pairs, span);
        }
    }

    private void addNP(String currentNP, int i, int len, String[] tokens, String[] tags, Object2IntMap<Keyphrase> freq, 
        Object2IntMap<NearbyPair> pairFreq, int span) {
        Keyphrase kp = new Keyphrase(currentNP, len);
        freq.put(kp, freq.getInt(kp) + 1);
        for(int j = max(0, i - span); j < i; j++) {
            if(tags[j].startsWith("N") ||
               tags[j].startsWith("V") ||
               tags[j].startsWith("J")) {
                NearbyPair pair = new NearbyPair(tokens[j], kp);
                pairFreq.put(pair, pairFreq.getInt(pair) + 1);
            }
        }

        for(int j = i + len; j < min(tokens.length, i + len + span); j++) {
            if(tags[j].startsWith("N") ||
               tags[j].startsWith("V") ||
               tags[j].startsWith("J")) {
                NearbyPair pair = new NearbyPair(tokens[j], kp);
                pairFreq.put(pair, pairFreq.getInt(pair) + 1);
            }
 
        }
    }

}
