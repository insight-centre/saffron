package org.insightcentre.nlp.saffron.domainmodelling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.lucene.store.Directory;
import org.insightcenter.nlp.saffron.documentindex.DocumentIndexFactory;
import org.insightcenter.nlp.saffron.documentindex.DocumentIndexer;
import org.insightcenter.nlp.saffron.documentindex.DocumentSearcher;
import org.insightcenter.nlp.saffron.documentindex.IndexingException;
import org.insightcentre.nlp.saffron.documentindex.lucene.LuceneException;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.POSExtractor;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.openlp.OpenNLPPOSExtractor;

/**
 * The Configuration for running the domain modeling
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class Configuration {

    public static class NLPConfiguration {
        public int lengthThreshold = 5;
        public String kpInfoFileName = "output/kpInfo.json";
        public String nounsFileName = "output/kpNouns.json";
        public String verbsFileName = "output/kpVerbs.json";
        public String adjsFileName = "output/kpAdjs.json";
        public int corpusFrequencyThreshold = 4;
    }

    public static class KeyPhraseConfiguration {
        public double alpha = 3.5;
        public double beta = 3.5;
        public int lengthThreshold = 5;
        public double rankThreshold = 0.0;
        public boolean stemKeyphrases = false;
        public String docsFileName = "output/docs.json";
        public String ranksFileName = "output/ranks.json";
        public int performanceUpto = 30;
    }

    public static class KeyphraseSimConfiguration {
        public String taxonsFileName = "output/taxons.json";
        public int taxSimLengthThreshold = 0;
        public int corpusFreqThresholdTaxonSim = 10;
        public int containsTaxonBoost = 50;
        public int taxonSimilaritySlopSize = 5;
        public long tokensInCorpus = 1000000l;
        public String tokensSimFileName = "output/tokens.json";
    }
    
    public NLPConfiguration nlp;
    public KeyPhraseConfiguration keyphrase;
    public KeyphraseSimConfiguration kpSim;
    public String stopwordsFile = "src/main/resources/stopwords/english";
    public String indexPath = "index";
    /** Create the Lucene index */
    public boolean reuseIndex = false;
    public String infoFileName = "output/info.json";
    public File corpusPath, 
        tokenizerModel = new File("models/en-token.bin"), 
        posModel = new File("models/en-pos-maxent.bin"),  
        chunkModel = new File("models/en-chunker.bin");

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
    
    private POSExtractor posExtractor = null;
    public POSExtractor loadPosExtractor() {
        if(posExtractor != null) { 
            return posExtractor;
        }
        try {
            return posExtractor = new OpenNLPPOSExtractor(tokenizerModel, posModel, chunkModel);
        } catch(IOException x) {
            throw new RuntimeException(x);
        }
    }

    private DocumentSearcher searcher = null;
    public DocumentSearcher loadSearcher() throws IOException, LuceneException, IndexingException {
        if(searcher != null) {
            return searcher;
        }
        Directory directory;
        File indexFile = new File(indexPath);

        if(reuseIndex) {
            directory = DocumentIndexFactory.luceneFileDirectory(indexFile, false);
        } else {
            directory = DocumentIndexFactory.luceneFileDirectory(indexFile, true);
            indexDocs(directory);
        }

        return searcher = DocumentIndexFactory.luceneSearcher(directory, DocumentIndexFactory.LuceneAnalyzer.LOWERCASE_STEMMER);
    }

    private void indexDocs(Directory directory) throws IndexingException, IOException  {
        Collection<File> corpus = loadCorpus();
        try(DocumentIndexer indexer = DocumentIndexFactory.luceneIndexer(directory, DocumentIndexFactory.LuceneAnalyzer.LOWERCASE_STEMMER)) {
            for (File f : corpus) {
                String content = new String(Files.readAllBytes(f.toPath()));
                indexer.indexDoc(f.getName(), content);
            }
        }
    }

    public Collection<File> loadCorpus() {
        return Arrays.asList(corpusPath.listFiles());
    }

    public int docsCount() {
        return loadCorpus().size();
    }
}
