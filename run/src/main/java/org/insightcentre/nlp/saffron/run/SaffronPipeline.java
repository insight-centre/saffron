package org.insightcentre.nlp.saffron.run;

import static org.insightcentre.nlp.saffron.authors.Consolidate.applyConsolidation;
import static org.insightcentre.nlp.saffron.taxonomy.supervised.Main.loadMap;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.insightcentre.nlp.saffron.authors.Consolidate;
import org.insightcentre.nlp.saffron.authors.ConsolidateAuthors;
import org.insightcentre.nlp.saffron.authors.connect.ConnectAuthorTerm;
import org.insightcentre.nlp.saffron.authors.sim.AuthorSimilarity;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.crawler.SaffronCrawler;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Model;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;
import org.insightcentre.nlp.saffron.documentindex.CorpusTools;
import org.insightcentre.nlp.saffron.taxonomy.classifiers.BERTBasedRelationClassifier;
import org.insightcentre.nlp.saffron.taxonomy.extract.ConvertKGToRDF;
import org.insightcentre.nlp.saffron.taxonomy.search.KGSearch;
import org.insightcentre.nlp.saffron.taxonomy.search.TaxonomySearch;
import org.insightcentre.nlp.saffron.taxonomy.supervised.SupervisedTaxo;
import org.insightcentre.nlp.saffron.term.TermExtraction;
import org.insightcentre.nlp.saffron.term.TermExtraction.Result;
import org.insightcentre.nlp.saffron.term.domain.DomainTermExtraction;
import org.insightcentre.nlp.saffron.topic.tfidf.TFIDF;
import org.insightcentre.nlp.saffron.topic.topicsim.TermSimilarity;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.Files;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * The main entry point to Saffron. Co-ordinates a whole run
 *
 * @author John McCrae
 */
public class SaffronPipeline {

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("c", "The type of corpus to be used. One of CRAWL, JSON, ZIP").withRequiredArg().ofType(RunConfiguration.CorpusMethod.class);
                    accepts("k", "The method for knowledge graph construction. One of TAXO or KG").withRequiredArg().ofType(RunConfiguration.KGMethod.class);
                    accepts("i", "The inclusion list of terms and relations (in JSON)").withRequiredArg().ofType(File.class);
                    accepts("max-pages", "The maximum number of pages to extract when crawling").withRequiredArg().ofType(Integer.class);
                    accepts("domain", "Limit the crawl to the domain of the seed URL");
                    accepts("name", "The name of the run").withRequiredArg().ofType(String.class);
                    accepts("d", "If a domain model should be extracted");
                    accepts("domain-model", "The domain model to be used for term extraction").withRequiredArg().ofType(File.class);
                    nonOptions("corpus[.json] output/ config.json \n    corpus     : The corpus file (or initial URL for crawling)\n    output     : The output folder\n    config.json: The configuration JSON file").ofType(String.class);
                }
            };
            final OptionSet os;
            final ObjectMapper mapper = new ObjectMapper();

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }

            if (os.nonOptionArguments().size() != 3) {
                badOptions(p, "Wrong number of arguments, expected 3 got " + os.nonOptionArguments().size());
            }
            RunConfiguration.CorpusMethod corpusMethod = (RunConfiguration.CorpusMethod) os.valueOf("c");
            if (corpusMethod == null) {
                corpusMethod = RunConfiguration.CorpusMethod.INFER;
            }
            RunConfiguration.KGMethod kgMethod = (RunConfiguration.KGMethod) os.valueOf("k");
            if (kgMethod == null) {
                kgMethod = RunConfiguration.KGMethod.KG;
            }
            File inclusionListFile = (File) os.valueOf("i");
            final InclusionList inclusionList;
            if (inclusionListFile != null) {
                if (!inclusionListFile.exists()) {
                    badOptions(p, "Inclusion file does not exist");
                    return;
                }
                inclusionList = mapper.readValue(inclusionListFile, InclusionList.class);
            } else {
                inclusionList = new InclusionList();
            }
            final String saffronDatasetName = os.has("name") ? (String) os.valueOf("name") : "saffron";

            int maxpages = os.has("max-pages") ? (Integer) os.valueOf("i") : 100;
            boolean domain = os.has("domain");

            boolean extractDomainModel = os.has("d");
            File domainModelFile = (File) os.valueOf("domain-model");

            URL corpusURL;
            File corpusFile;
            if (corpusMethod == RunConfiguration.CorpusMethod.CRAWL) {
                corpusURL = new URL((String) os.nonOptionArguments().get(0));
                corpusFile = null;
            } else {
                corpusFile = new File((String) os.nonOptionArguments().get(0));
                if (!corpusFile.exists()) {
                    badOptions(p, "Corpus file does not exist");
                    return;
                }
                corpusURL = null;
            }
            File outputFolder = new File((String) os.nonOptionArguments().get(1));
            File configurationFile = new File((String) os.nonOptionArguments().get(2));
            if (!configurationFile.exists()) {
                badOptions(p, "Configuration file does not exist");
                return;
            }
            Configuration config = mapper.readValue(configurationFile, Configuration.class);
            final RunConfiguration runConfig;
            if (corpusMethod == RunConfiguration.CorpusMethod.CRAWL) {
                runConfig = new RunConfiguration(corpusURL, inclusionList, true, kgMethod, maxpages, domain, extractDomainModel);
            } else {
                runConfig = new RunConfiguration(corpusFile, corpusMethod, inclusionList, true, kgMethod, extractDomainModel, domainModelFile);
            }

            execute(runConfig, outputFolder, config, saffronDatasetName, new CommandLineSaffronRunListener(outputFolder));
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    public static void execute(RunConfiguration run, File datasetFolder,
            Configuration config, String saffronDatasetName,
            SaffronRunListener listener) throws Exception {

        new SaffronPipeline(run, datasetFolder, config, saffronDatasetName, listener).executePipeline();

    }

    private final RunConfiguration run;
    private final File datasetFolder;
    private final Configuration config;
    private final String runName;
    private final ObjectMapper mapper;
    private final ObjectWriter ow;
    private final SaffronRunListener status;

    private SaffronPipeline(RunConfiguration run, File datasetFolder, Configuration config, String runName, SaffronRunListener listener) {
        this.run = run;
        this.datasetFolder = datasetFolder;
        this.config = config;
        this.runName = runName;
        this.mapper = new ObjectMapper();
        this.ow = mapper.writerWithDefaultPrettyPrinter();
        this.status = listener;
    }

    private void executePipeline() throws Exception {
        scaleThreads(config);

        if (!datasetFolder.exists()) {
            if (!datasetFolder.mkdirs()) {
                System.err.println("Could not make dataset folder, this run is likely to fail!");
            }
        }
        ow.writeValue(new File(datasetFolder, "config.json"), config);

        Corpus corpus = makeCorpus();
        status.setStageComplete("Loading corpus", runName);
        Corpus searcher = preprocessCorpus(corpus);
        List<String> domainModelTerms = extractDomainModelTerms(corpus);
        TermExtraction.Result r = extractTerms(searcher, domainModelTerms);
        List<Term> terms = new ArrayList<>(r.terms);
        extractAuthors(searcher);
        Collection<AuthorTerm> authorTerms = connectAuthors(searcher, terms, r.docTerms);
        connectTerms(r.docTerms);
        authorSimilarity(authorTerms);
        switch (run.kgMethod) {
            case TAXO:
                Taxonomy taxo = buildTaxonomy(terms, r.docTerms);
                KnowledgeGraph kgTaxo = new KnowledgeGraph();
                kgTaxo.setTaxonomy(taxo);
                exportKG(kgTaxo, new File(datasetFolder, "taxonomy.rdf"), config.baseURL);
                break;
            case KG:
            default:
                KnowledgeGraph kg = buildKG(terms);
                exportKG(kg, new File(datasetFolder, "kg.rdf"), config.baseURL);
        }

        status.end(runName);
    }

	private List<String> extractDomainModelTerms(Corpus corpus)
			throws IOException, JsonParseException, JsonMappingException {

		Set<Term> domainModelTerms = null;
		if (run.domainModelFile != null) {
      status.setStageStart("Reading domain model terms from file", runName);
			domainModelTerms = mapper.readValue(run.domainModelFile, mapper.getTypeFactory().constructCollectionType(Set.class, Term.class));
      status.setStageComplete("Reading domain model terms from file", runName);
    } else {
    	if(run.extractDomainModel) {
          status.setStageStart("Extracting domain model terms", runName);
    	    final DomainTermExtraction extractor = new DomainTermExtraction(config.dmExtraction);
          final Result dmResult = extractor.extractDomainModelTerms(corpus);
          domainModelTerms = dmResult.terms;
          status.setStageComplete("Extracting domain model terms", runName);
    	}
    }

		List<String> domainModelStrings = null;
		if(domainModelTerms != null) {
			status.setDomainModelTerms(runName,domainModelTerms);

			domainModelStrings = new ArrayList<String>();
    		for(Term term: domainModelTerms) {
    			domainModelStrings.add(term.getString());
    		}
		}
        return domainModelStrings;
	}

    private Corpus makeCorpus() throws Exception {
        status.setStageStart("Loading corpus", runName);
        switch (run.corpusMethod) {
            case INFER:
                CorpusTools.readFile(run.corpusFile);
            case JSON:
                return CorpusTools.fromJson(run.corpusFile);
            case ZIP:
                if (run.corpusFile.getName().endsWith(".tgz") || run.corpusFile.getName().endsWith(".tar.gz")) {
                    return CorpusTools.fromTarball(run.corpusFile);
                } else {
                    return CorpusTools.fromZIP(run.corpusFile);
                }
            case CRAWL:
                final File f = Files.createTempDir();
                final String crawlStorageFolder = f.getAbsolutePath();
                return SaffronCrawler.crawl(crawlStorageFolder, new File(datasetFolder, runName),
                        null, run.maxPages, run.domain ? "\\w+://\\Q" + run.crawlURL.getHost() + "\\E.*" : ".*",
                        run.crawlURL.toString(), 7);
            case PROVIDED:
                return run.providedCorpus;
            default:
                throw new UnsupportedOperationException("Unreachable");
        }
    }

    private Corpus preprocessCorpus(Corpus corpus) throws Exception {
        status.setStageStart("Indexing Corpus", runName);
        status.setCorpus(runName, corpus);
        status.setStageComplete("Indexing Corpus", runName);
        return corpus;
    }

    private TermExtraction.Result extractTerms(Corpus searcher, List<String> domainModelTerms) throws IOException {
        status.setStageStart("Extracting Terms", runName);
        final TermExtraction extractor;
        if (domainModelTerms == null || domainModelTerms.isEmpty())
        	extractor = new TermExtraction(config.termExtraction);
        else
        	extractor = new TermExtraction(config.termExtraction, domainModelTerms);
        TermExtraction.Result res = extractor.extractTerms(searcher, run.inclusionList.getRequiredTerms(), run.inclusionList.getExcludedTerms(), status);
        List<Term> terms = new ArrayList<>(res.terms);
        status.setTerms(runName, terms);
        status.setDocTerms(runName, res.docTerms);
        status.setStageComplete("Extracting Terms", runName);
        return res;
    }

    private void extractAuthors(Corpus searcher) throws IOException {
        status.setStageStart("Extracting authors from corpus", runName);
        Set<Author> authors = Consolidate.extractAuthors(searcher, status);
        Map<Author, Set<Author>> consolidation = new ConsolidateAuthors().consolidate(authors, status);
        searcher = applyConsolidation(searcher, consolidation, status);
        status.setCorpus(runName, searcher);
        status.setStageComplete("Extracting authors from corpus", runName);
    }

    private Collection<AuthorTerm> connectAuthors(Corpus searcher, List<Term> terms, List<DocumentTerm> docTerms) throws IOException {
        status.setStageStart("Connecting authors to terms", runName);
        TFIDF.addTfidf(docTerms);
        ConnectAuthorTerm cr = new ConnectAuthorTerm(config.authorTerm);
        Collection<AuthorTerm> authorTerms = cr.connectResearchers(terms, docTerms, searcher.getDocuments(), status);
        status.setAuthorTerms(runName, authorTerms);
        status.setStageComplete("Connecting authors to terms", runName);
        return authorTerms;

    }

    private void connectTerms(List<DocumentTerm> docTerms) throws IOException {
        status.setStageStart("Connecting terms", runName);
        TermSimilarity ts = new TermSimilarity(config.termSim);
        final List<TermTerm> termSimilarity = ts.termSimilarity(docTerms, status);
        status.setTermSim(runName, termSimilarity);
        status.setStageComplete("Connecting terms", runName);
    }

    private void authorSimilarity(Collection<AuthorTerm> authorTerms) throws IOException {
        status.setStageStart("Connecting authors to authors", runName);
        AuthorSimilarity as = new AuthorSimilarity(config.authorSim);
        final List<AuthorAuthor> authorSim = as.authorSimilarity(authorTerms, runName, status);
        status.setAuthorSim(runName, authorSim);
        status.setStageComplete("Connecting authors to authors", runName);
    }

    private Taxonomy buildTaxonomy(List<Term> terms, List<DocumentTerm> docTerms) throws IOException {

        status.setStageStart("Building term map and taxonomy", runName);
        Map<String, Term> termMap = loadMap(terms, mapper, status);
        if (config.taxonomy.modelFile == null) {
            config.taxonomy.modelFile = new SaffronPath("${saffron.home}/models/default.json");
        }
        Model model = mapper.readValue(config.taxonomy.modelFile.toFile(), Model.class);
        SupervisedTaxo supTaxo = new SupervisedTaxo(docTerms, termMap, model);
        TaxonomySearch search = TaxonomySearch.create(config.taxonomy.search, supTaxo, termMap.keySet());
        final Taxonomy graph = search.extractTaxonomyWithBlackWhiteList(termMap, run.inclusionList.getRequiredRelations(), run.inclusionList.getExcludedRelations());
        status.setTaxonomy(runName, graph);
        status.setStageComplete("Building term map and taxonomy", runName);
        return graph;
    }

    private KnowledgeGraph buildKG(List<Term> terms) throws IOException, UnsupportedKerasConfigurationException, InvalidKerasConfigurationException {
        status.setStageStart("Building term map and taxonomy", runName);
        Map<String, Term> termMap = loadMap(terms, mapper, status);
        status.setStageComplete("Building term map and taxonomy", runName);
        status.setStageStart("Building knowledge graph", runName);
        BERTBasedRelationClassifier relationClassifier = BERTBasedRelationClassifier.getInstance(
        		config.kg.kerasModelFile.getResolvedPath(), config.kg.bertModelFile.getResolvedPath(), config.kg.numberOfRelations);
        KGSearch kgSearch = KGSearch.create(config.taxonomy.search, config.kg, relationClassifier, termMap.keySet());
        final KnowledgeGraph kGraph = kgSearch.extractKnowledgeGraphWithDenialAndAllowanceList(termMap,
                run.inclusionList.getRequiredRelations(), run.inclusionList.getExcludedRelations(), relationClassifier.typeMap.keySet());
        status.setKnowledgeGraph(runName, kGraph);
        status.setStageComplete("Building knowledge graph", runName);
        return kGraph;
    }

    private void exportKG(KnowledgeGraph kg, File outputFile, String baseUrl) throws IOException {
        status.setStageStart("Exporting Knowledge Graph", runName);
        org.apache.jena.rdf.model.Model model = ConvertKGToRDF.convertToRDF(baseUrl, kg);
        ConvertKGToRDF.writeRDFToFile(model, ConvertKGToRDF.RDFFormats.XML, outputFile);        
        status.setStageComplete("Exporting Knowledge Graph", runName);
    }

    private void scaleThreads(Configuration config) {
        long heapSize = Runtime.getRuntime().maxMemory();
        if ((long) config.termExtraction.numThreads * 1024 * 1024 * 400 > heapSize) {
            int numThreads = (int) Math.ceil((double) heapSize / 1024 / 1024 / 400);
            System.err.println(String.format("System memory %d MB", heapSize / 1024 / 1024));
            System.err.println(String.format("Insufficient memory for %d threads, reducing to %d", config.termExtraction.numThreads, numThreads));
            System.err.println("Try setting the -Xmx flag to the Java Runtime to improve performance");
            config.termExtraction.numThreads = numThreads;
        }
    }
}
