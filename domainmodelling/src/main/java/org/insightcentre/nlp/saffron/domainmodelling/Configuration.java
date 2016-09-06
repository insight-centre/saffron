package org.insightcentre.nlp.saffron.domainmodelling;

import ie.deri.unlp.javaservices.documentindex.DocumentSearcher;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.POSExtractor;

/**
 * The Configuration for running the domain modeling
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class Configuration {

    public static class NLPConfiguration {
        private int lengthThreshold = -1;
        private String kpInfoFileName = "output/kpInfo.json";
        private String nounsFileName = "output/kpNouns.json";
        private String verbsFileName = "output/kpVerbs.json";
        private String adjsFileName = "output/kpAdjs.json";
        private int docsCount;
        private int corpusFrequencyThreshold;

        public int getLengthThreshold() {
            return lengthThreshold;
        }

        public void setLengthThreshold(int lengthThreshold) {
            this.lengthThreshold = lengthThreshold;
        }

        public String getKpInfoFileName() {
            return kpInfoFileName;
        }

        public void setKpInfoFileName(String kpInfoFileName) {
            this.kpInfoFileName = kpInfoFileName;
        }

        public String getNounsFileName() {
            return nounsFileName;
        }

        public void setNounsFileName(String nounsFileName) {
            this.nounsFileName = nounsFileName;
        }

        public String getVerbsFileName() {
            return verbsFileName;
        }

        public void setVerbsFileName(String verbsFileName) {
            this.verbsFileName = verbsFileName;
        }

        public String getAdjsFileName() {
            return adjsFileName;
        }

        public void setAdjsFileName(String adjsFileName) {
            this.adjsFileName = adjsFileName;
        }

        public int getDocsCount() {
            return docsCount;
        }

        public void setDocsCount(int docsCount) {
            this.docsCount = docsCount;
        }

        public int getCorpusFrequencyThreshold() {
            return corpusFrequencyThreshold;
        }

        public void setCorpusFrequencyThreshold(int corpusFrequencyThreshold) {
            this.corpusFrequencyThreshold = corpusFrequencyThreshold;
        }
        

    }

    public NLPConfiguration nlp;
    public String stopwordsFile;

    public NLPConfiguration getNlp() {
        return nlp;
    }

    public void setNlp(NLPConfiguration nlp) {
        this.nlp = nlp;
    }

    /**
     * Load stop words
     * @throws IOException If the stop words file cannot be read
     * @return The list of stopwords
     */
    public Set<String> loadStopWords() throws IOException {
        Set<String> stopwords = new HashSet<>();
        try(BufferedReader br = new BufferedReader(new FileReader(stopwordsFile))) {
            String line;
            while((line = br.readLine()) != null) {
                stopwords.add(line);
            }
        }
        return stopwords;
    }
    
    public POSExtractor loadPosExtractor() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public DocumentSearcher loadSearcher() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Collection<File> loadCorpus() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}
