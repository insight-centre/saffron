package org.insightcentre.nlp.saffron.run;

import java.io.File;
import java.net.URL;
import org.insightcentre.nlp.saffron.data.Corpus;

/**
 * Configuration of a single run.
 * 
 * @author John McCrae
 */
public class RunConfiguration {
    public final File corpusFile;
    public final URL crawlURL;
    public final Corpus providedCorpus;
    public final CorpusMethod corpusMethod;
    public final InclusionList inclusionList;
    public final boolean isInitialRun;
    public final KGMethod kgMethod;
    public final int maxPages;
    public final boolean domain;
    public final boolean extractDomainModel;
    public final File domainModelFile;

    /**
     * Create a configuration for a single run over a file
     * @param corpusFile The file to analyse
     * @param corpusMethod Information about the type of corpus
     * @param inclusionList The inclusion list (or null if not used)
     * @param isInitialRun If this is an initial run
     * @param kgMethod The knowledge graph extraction method to use
     * @param extractDomainModel If a domain model should be extracted
     * @param domainModelFile A file with the domain model, if not extracted
     */
    public RunConfiguration(File corpusFile, CorpusMethod corpusMethod, 
            InclusionList inclusionList, boolean isInitialRun, 
            KGMethod kgMethod, boolean extractDomainModel,
            File domainModelFile) {
        this.corpusFile = corpusFile;
        this.crawlURL = null;
        this.providedCorpus = null;
        this.corpusMethod = corpusMethod;
        this.inclusionList = inclusionList;
        this.isInitialRun = isInitialRun;
        this.kgMethod = kgMethod;
        this.maxPages = 0;
        this.domain = false;
        this.domainModelFile = domainModelFile;
        if (domainModelFile != null)
        	this.extractDomainModel = false;
        else
        	this.extractDomainModel = extractDomainModel;
    }

    /**
     * Create a run starting with crawling a website
     * @param crawlURL The URL to crawl
     * @param inclusionList The inclusion (or null if not used)
     * @param isInitialRun If this is an initial run
     * @param kgMethod The knowledge graph extraction method to use
     * @param maxPages The maximum number of pages to show in a run
     * @param domain Whether to limit the crawl to a single domain
     */
    public RunConfiguration(URL crawlURL, InclusionList inclusionList, boolean isInitialRun, KGMethod kgMethod, int maxPages, 
    		boolean domain, boolean extractDomainModel) {
        this.crawlURL = crawlURL;
        this.inclusionList = inclusionList;
        this.isInitialRun = isInitialRun;
        this.kgMethod = kgMethod;
        this.maxPages = maxPages;
        this.domain = domain;
        this.corpusFile = null;
        this.providedCorpus = null;
        this.corpusMethod = CorpusMethod.CRAWL;
        this.extractDomainModel = extractDomainModel;
        this.domainModelFile = null;
    }

    /**
     * Use a corpus already loaded from another source
     * @param providedCorpus The corpus object
     * @param inclusionList The inclusion (or null if not used)
     * @param isInitialRun If this is an initial run
     * @param kgMethod The knowledge graph extraction method to use
     * @param extractDomainModel If a domain model should be extracted
     * @param domainModelFile A file with the domain model, if not extracted
     */
    public RunConfiguration(Corpus providedCorpus, InclusionList inclusionList, boolean isInitialRun, KGMethod kgMethod,
    		boolean extractDomainModel, File domainModelFile) {
        this.providedCorpus = providedCorpus;
        this.inclusionList = inclusionList;
        this.isInitialRun = isInitialRun;
        this.kgMethod = kgMethod;
        this.maxPages = 0;
        this.domain = false;
        this.corpusFile = null;
        this.crawlURL = null;
        this.corpusMethod = CorpusMethod.PROVIDED;
        this.domainModelFile = domainModelFile;
        if (domainModelFile != null)
        	this.extractDomainModel = false;
        else
        	this.extractDomainModel = extractDomainModel;
    }
    
    /**
     * Where to load the corpus from
     */
    public static enum CorpusMethod {
        CRAWL, INFER, JSON, ZIP, PROVIDED
    }
    
    /**
     * Which knowledge graph extraction method to use
     */
    public static enum KGMethod {
        KG, TAXO
    }
}
