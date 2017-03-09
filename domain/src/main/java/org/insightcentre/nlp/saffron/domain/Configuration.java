package org.insightcentre.nlp.saffron.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Configuration {
    /**
     * The tokenizer model for OpenNLP
     */
    public File tokenizerModel = new File("models/en-token.bin");
    /**
     * The POS model for OpenNLP
     */
    public File posModel = new File("models/en-pos-maxent.bin");
    /**
     * The chunk model for OpenNLP
     */
    public File chunkModel = new File("models/en-chunker.bin");
    /**
     * The sentence model for OpenNLP
     */
    public File sentModel = new File("models/en-sent.bin");
    /**
     * The maximum span to consider for nearby keyphrases
     */
    public int span = 5;
    /**
     * The minimum frequency for a keyphrase in the corpus
     */
    public int minFreq = 0;
    /**
     * The maximum length for a keyphrase in the corpus
     */
    public int maxLength = 5;
    /**
     * The file containing the stopwords
     */
    public final File stopwords = new File("models/stopwords/english");
    /**
     * The number of words to use for the domain model
     */
    public int n = 200;
    /**
     * Smoothing factor for PMI
     */
    public double epsilon = 0.1;

    private OpenNLPPOSExtractor posExtractor = null;
    public OpenNLPPOSExtractor loadPosExtractor() {
        if(posExtractor != null) { 
            return posExtractor;
        }
        try {
            return posExtractor = new OpenNLPPOSExtractor(tokenizerModel, posModel, chunkModel, sentModel);
        } catch(IOException x) {
            throw new RuntimeException(x);
        }
    }

    private Set<String> sws = null;
    public Set<String> loadStopWords() throws IOException {
        if(sws != null) {
            return sws;
        }
        Set<String> s = new HashSet<String>();
        try(BufferedReader br = new BufferedReader(new FileReader(stopwords))) {
            String line;
            while((line = br.readLine()) != null) {
                s.add(line);
            }
        }
        return sws = s;
    }
  
    
}
