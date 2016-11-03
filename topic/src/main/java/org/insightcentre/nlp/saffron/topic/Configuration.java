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
     * The folder containing all the files in the corpus
     */
    public File corpus;
    /**
     * The domain model
     */
    public File domainModelFile;
    /**
     * The file containing the stopwords
     */
    public File stopwordFile = new File("src/main/resources/stopwords/english");
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

    public File getCorpus() {
        return corpus;
    }

    public void setCorpus(File corpus) {
        this.corpus = corpus;
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

    @JsonProperty("domainModel")
    public File getDomainModelFile() {
        return domainModelFile;
    }

    @JsonProperty("domainModel")
    public void setDomainModelFile(File domainModelFile) {
        this.domainModelFile = domainModelFile;
    }

    public File getStopwordFile() {
        return stopwordFile;
    }

    public void setStopwordFile(File stopwordFile) {
        this.stopwordFile = stopwordFile;
    }

    public Collection<File> loadCorpus() {
        return Arrays.asList(corpus.listFiles());
    }

    private Set<String> sws = null;
    public Set<String> getStopWords() throws IOException {
        if(sws != null) {
            return sws;
        }
        Set<String> s = new HashSet<>();
        try(BufferedReader br = new BufferedReader(new FileReader(stopwordFile))) {
            String line;
            while((line = br.readLine()) != null) {
                s.add(line);
            }
        }
        return sws = s;
    }

    private List<String> dm = null;
    public List<String> getDomainModel() throws IOException {
        if(dm != null) {
            return dm;
        }
        List<String> s = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new FileReader(domainModelFile))) {
            String line;
            while((line = br.readLine()) != null) {
                s.add(line);
            }
        }
        return dm = s;
    }
}
