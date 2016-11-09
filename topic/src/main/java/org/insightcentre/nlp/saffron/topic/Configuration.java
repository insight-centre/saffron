package org.insightcentre.nlp.saffron.topic;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The configuration for the topic extractor
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class Configuration {
    /**
     * The file containing the stopwords
     */
    public File stopwords = new File("models/stopwords/english");
    /**
     * The maximum number of topics to generate per document
     */
    public int maxTopics = Integer.MAX_VALUE;
    /**
     * The minimum number of tokens for a topic
     */
    public int minTokens = 1;
    /**
     * The maximum number of tokens for a topic
     */
    public int maxTokens = 5;
    /**
     * Where gate is installed to
     */
    public File gateHome;

    public File getGateHome() {
        return gateHome;
    }

    public void setGateHome(File gateHome) {
        this.gateHome = gateHome;
    }

    public int getMaxTopics() {
        return maxTopics;
    }

    public void setMaxTopics(int maxTopics) {
        this.maxTopics = maxTopics;
    }

    public int getMinTokens() {
        return minTokens;
    }

    public void setMinTokens(int minTokens) {
        this.minTokens = minTokens;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public File getStopwordFile() {
        return stopwords;
    }

    public void setStopwordFile(File stopwordFile) {
        this.stopwords = stopwordFile;
    }

    private Set<String> sws = null;
    public Set<String> getStopWords() throws IOException {
        if(sws != null) {
            return sws;
        }
        Set<String> s = new HashSet<>();
        try(BufferedReader br = new BufferedReader(new FileReader(stopwords))) {
            String line;
            while((line = br.readLine()) != null) {
                s.add(line);
            }
        }
        return sws = s;
    }
}
