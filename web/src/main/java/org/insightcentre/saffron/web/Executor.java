package org.insightcentre.saffron.web;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.insightcentre.nlp.saffron.run.InclusionList;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.*;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.flogger.FluentLogger;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.run.RunConfiguration;
import org.insightcentre.nlp.saffron.run.SaffronPipeline;
import org.insightcentre.nlp.saffron.run.SaffronRunListener;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class Executor extends AbstractHandler {

    private final SaffronDataSource data;
    private final File parentDirectory;
    private final Map<String, Status> statuses;
    private Configuration defaultConfig;
    private final File logFile;
    static String storeCopy = System.getenv("STORE_LOCAL_COPY");
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    public Executor(SaffronDataSource data, File directory, File logFile) {
        this.data = data;
        this.parentDirectory = directory;
        this.statuses = new HashMap<>();
        try {
            this.defaultConfig = new ObjectMapper().readValue(new SaffronPath("${saffron.home}/configs/config.json").toFile(), Configuration.class);
        } catch (IOException x) {
            this.defaultConfig = new Configuration();
            logger.atInfo().log("Could not load config.json in models folder... using default configuration (" + x.getMessage() + ")");

        }
        this.logFile = logFile;

    }

    /**
     * Initialize a new Saffron Dataset
     *
     * @param name The name
     * @return True if a new dataset was created
     */
    public boolean newDataSet(String name) throws IOException {
        if (data.containsKey(name)) {
            return false;
        } else {
            data.addRun(name, new Date(), this.defaultConfig);
            return true;
        }
    }

    public boolean isExecuting(String name) {
        return statuses.containsKey(name) && statuses.get(name).stage > 0 && !statuses.get(name).completed;
    }

    public File getParentDirectory() {
        return this.parentDirectory;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest hsr,
            HttpServletResponse response) throws IOException, ServletException {
        try {
            if ("/execute".equals(target)) {
                String name = hsr.getParameter("name");
                doExecute(name, response, baseRequest, hsr);
            } else if (target.startsWith("/execute/advanced/")) {
                final String saffronDatasetName = target.substring("/execute/advanced/".length());
                doAdvancedExecute(hsr, saffronDatasetName, response, baseRequest);

//            } else if (target.startsWith("/api/v1/run/rerun")) {
//                final String saffronDatasetName = target.substring("/api/v1/run/rerun/".length());
//                if (saffronDatasetName != null && data.containsKey(saffronDatasetName)) {
//                    doRerun(saffronDatasetName, response, baseRequest);
//                }
              } else if ("/execute/status".equals(target)) {
                String saffronDatasetName = hsr.getParameter("name");
                Status status = getStatus(saffronDatasetName);
                if (status != null) {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.writeValue(response.getWriter(), statuses.get(saffronDatasetName));
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "No executing run: " + saffronDatasetName);
                    baseRequest.setHandled(true);
                }
            }
        } catch (Exception x) {
            x.printStackTrace();
            throw new ServletException(x);
        }
    }

    public Status getStatus(String saffronDatasetName) throws IOException {
        return statuses.get(saffronDatasetName);
    }

//    private void doRerun(final String saffronDatasetName, HttpServletResponse response, Request baseRequest) throws IOException, FileNotFoundException, NumberFormatException, JSONException {
//        Status _status = makeStatus();
//        _status.name = saffronDatasetName;
//        statuses.put(saffronDatasetName, _status);
//        response.setContentType("text/html");
//        response.setStatus(HttpServletResponse.SC_OK);
//        baseRequest.setHandled(true);
//        FileReader reader = new FileReader(new File("static/executing.html"));
//        Writer writer = new StringWriter();
//        char[] buf = new char[4096];
//        int p = 0;
//        while ((p = reader.read(buf)) >= 0) {
//            writer.write(buf, 0, p);
//        }
//        response.getWriter().write(writer.toString().replace("{{name}}", saffronDatasetName));
//        String mongoUrl = System.getenv("MONGO_URL");
//        if (mongoUrl == null) {
//            mongoUrl = "localhost";
//        }
//        String mongoPort = System.getenv("MONGO_PORT");
//        if (mongoPort == null) {
//            mongoPort = "27017";
//        }
//
//        MongoDBHandler mongo = new MongoDBHandler();
//        String run = mongo.getRun(saffronDatasetName);
//        JSONObject configObj = new JSONObject(run);
//        String confJson = (String) configObj.get("config");
//        JSONObject config = new JSONObject(confJson);
//        JSONObject termExtractionConfig = (JSONObject) config.get("termExtraction");
//        JSONObject authorTermConfig = (JSONObject) config.get("authorTerm");
//        JSONObject authorSimConfig = (JSONObject) config.get("authorSim");
//        JSONObject termSimConfig = (JSONObject) config.get("termSim");
//        JSONObject taxonomyConfig = (JSONObject) config.get("taxonomy");
//        //JSONObject conceptConsolidation = (JSONObject) config.get("conceptConsolidation");
//        final Configuration newConfig = new Configuration();
//        TermExtractionConfiguration terms
//                = new ObjectMapper().readValue(termExtractionConfig.toString(), TermExtractionConfiguration.class);
//        AuthorTermConfiguration authorTerm
//                = new ObjectMapper().readValue(authorTermConfig.toString(), AuthorTermConfiguration.class);
//        AuthorSimilarityConfiguration authorSimilarityConfiguration
//                = new ObjectMapper().readValue(authorSimConfig.toString(), AuthorSimilarityConfiguration.class);
//        TermSimilarityConfiguration termSimilarityConfiguration
//                = new ObjectMapper().readValue(termSimConfig.toString(), TermSimilarityConfiguration.class);
//        TaxonomyExtractionConfiguration taxonomyExtractionConfiguration
//                = new ObjectMapper().readValue(taxonomyConfig.toString(), TaxonomyExtractionConfiguration.class);
//        //ConceptConsolidationConfiguration conceptConsolidationConfiguration
//        //        = conceptConsolidation != null ? new ObjectMapper().readValue(conceptConsolidation.toString(), ConceptConsolidationConfiguration.class) : new ConceptConsolidationConfiguration();
//        newConfig.authorSim = authorSimilarityConfiguration;
//        newConfig.authorTerm = authorTerm;
//        newConfig.taxonomy = taxonomyExtractionConfiguration;
//        newConfig.termExtraction = terms;
//        newConfig.termSim = termSimilarityConfiguration;
//        //newConfig.conceptConsolidation = conceptConsolidationConfiguration;
//
//        Corpus corpus = mongo.getCorpus(saffronDatasetName);
//
//        new Thread(new Runnable() {
//
//            @Override
//            @SuppressWarnings("UseSpecificCatch")
//            public void run() {
//                _status.stage = 1;
//                try {
//                    RunConfiguration runConfig = new RunConfiguration(corpus, getInclusionList(saffronDatasetName), false, RunConfiguration.KGMethod.TAXO, false, null);
//                    getStatus(saffronDatasetName).runConfig = runConfig;
//                    SaffronPipeline.execute(runConfig, new File(parentDirectory, saffronDatasetName), newConfig, saffronDatasetName, getStatus(saffronDatasetName));
//                } catch (Throwable x) {
//                    Status _status = statuses.get(saffronDatasetName);
//                    _status.fail(x.getMessage(), x);
//                    x.printStackTrace();
//                }
//            }
//        }).start();
//    }

    private boolean doAdvancedExecute(HttpServletRequest hsr, final String saffronDatasetName, HttpServletResponse response, Request baseRequest) throws IOException {
        BufferedReader r = hsr.getReader();
        StringBuilder sb = new StringBuilder();
        final Configuration newConfig;
        try {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append("\n");
            }
            newConfig = new ObjectMapper().readValue(sb.toString(), Configuration.class);
        } catch (Exception x) {
            x.printStackTrace();
            return true;
        }
        // Clear the 'advanced' status so the system switches to the spinner
        statuses.get(saffronDatasetName).advanced = false;
        new Thread(new Runnable() {
            @Override
            @SuppressWarnings("UseSpecificCatch")
            public void run() {
                try {
                    SaffronPipeline.execute(getStatus(saffronDatasetName).runConfig, new File(parentDirectory, saffronDatasetName), newConfig, saffronDatasetName, getStatus(saffronDatasetName));
                } catch (Exception x) {
                    Status _status = statuses.get(saffronDatasetName);
                    _status.fail(x.getMessage(), x);
                    x.printStackTrace();
                }
            }
        }).start();
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        return false;
    }

    public void doExecute(String name, HttpServletResponse response, Request baseRequest, HttpServletRequest hsr) throws IOException {
        if (statuses.containsKey(name) && statuses.get(name).advanced) {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            String page = FileUtils.readFileToString(new File("static/advanced.html"));
            page = page.replace("{{config}}", new ObjectMapper().writeValueAsString(defaultConfig));
            page = page.replace("{{name}}", hsr.getParameter("name"));
            response.getWriter().print(page);
        } else {
            final String saffronDatasetName = hsr.getParameter("name");
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            FileReader reader = new FileReader(new File("static/executing.html"));
            Writer writer = new StringWriter();
            char[] buf = new char[4096];
            int i = 0;
            while ((i = reader.read(buf)) >= 0) {
                writer.write(buf, 0, i);
            }
            response.getWriter().write(writer.toString().replace("{{name}}", saffronDatasetName));
        }
    }

    public void startWithZip(final File tmpFile, final boolean advanced, final String saffronDatasetName) {
        final Status _status = makeStatus();
        _status.name = saffronDatasetName;
        statuses.put(saffronDatasetName, _status);
        new Thread(new Runnable() {
            @Override
            public void run() {
                _status.stage = 1;
                _status.setStageStart("Loading corpus" + tmpFile.getPath(), saffronDatasetName);
                try {
                    final RunConfiguration runConfig = new RunConfiguration(tmpFile, RunConfiguration.CorpusMethod.ZIP, getInclusionList(saffronDatasetName), true, RunConfiguration.KGMethod.TAXO, false, null);
                    _status.runConfig = runConfig;
                    if (advanced) {
                        _status.advanced = true;
                    } else {
                        SaffronPipeline.execute(runConfig, new File(parentDirectory, saffronDatasetName), defaultConfig, saffronDatasetName, _status);
                    }
                } catch (Throwable x) {
                    _status.fail(x.getMessage(), x);
                    logger.atInfo().log("Error: " + x.getMessage() + ")");
                    x.printStackTrace();
                }
                try {
                    _status.close();
                } catch (IOException x) {
                }
            }
        }).start();
    }

    private Status makeStatus() {
        try {
            if (logFile != null && !logFile.exists()) {
                PrintWriter out = new PrintWriter(logFile);
                out.close();
            }
            return new Status(logFile == null ? null : new PrintWriter(new FileWriter(logFile, true)), data, parentDirectory);
        } catch (IOException x) {
            System.err.println("Could not create logging file: " + x.getMessage());
            return new Status(null, data, parentDirectory);
        }
    }

    public void startWithCrawl(final String url, final int maxPages, final boolean domain,
            final boolean advanced, final String saffronDatasetName) {
        final Status _status = makeStatus();
        _status.name = saffronDatasetName;
        statuses.put(saffronDatasetName, _status);
        new Thread(new Runnable() {
            @Override
            public void run() {
                _status.stage = 1;
                _status.setStageStart("Loading corpus " + url, saffronDatasetName);
                try {
                    URL url2 = new URL(url);
                    final RunConfiguration runConfig = new RunConfiguration(url2, getInclusionList(saffronDatasetName), true, RunConfiguration.KGMethod.TAXO, maxPages, domain, false);
                    _status.runConfig = runConfig;
                    if (advanced) {
                        _status.advanced = true;
                    } else {
                        SaffronPipeline.execute(runConfig, new File(parentDirectory, saffronDatasetName), defaultConfig, saffronDatasetName, _status);
                    }
                } catch (Exception x) {
                    _status.fail(x.getMessage(), x);
                }
                try {
                    _status.close();
                } catch (IOException x) {
                    x.printStackTrace();
                }
            }
        }).start();
    }

    public void startWithJson(final File tmpFile, final boolean advanced, final String saffronDatasetName) {
        final Status _status = makeStatus();
        _status.name = saffronDatasetName;
        statuses.put(saffronDatasetName, _status);
        new Thread(new Runnable() {
            @Override
            public void run() {
                _status.stage = 1;
                _status.setStageStart("Loading corpus" + tmpFile.getPath(), saffronDatasetName);
                try {
                    final RunConfiguration runConfig = new RunConfiguration(tmpFile, RunConfiguration.CorpusMethod.JSON, getInclusionList(saffronDatasetName), true, RunConfiguration.KGMethod.TAXO, false, null);
                    _status.runConfig = runConfig;
                    if (advanced) {
                        _status.advanced = true;
                    } else {
                        SaffronPipeline.execute(runConfig, new File(parentDirectory, saffronDatasetName), defaultConfig, saffronDatasetName, _status);
                    }
                } catch (Exception x) {
                    _status.fail(x.getMessage(), x);
                    x.printStackTrace();
                }
                try {
                    _status.close();
                } catch (IOException x) {
                }
            }
        }).start();
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

    private InclusionList getInclusionList(String saffronDatasetName) throws JsonParseException, JsonMappingException, IOException {
                InclusionList allowDenyList = extractInclusionList(saffronDatasetName);
        if (allowDenyList == null) {
            allowDenyList = new InclusionList<TaxoLink>();
        }
        return allowDenyList;
    }

    public InclusionList extractInclusionList(String datasetName) throws JsonParseException, JsonMappingException, IOException {

        return new InclusionList();
    }

    public static class Status implements SaffronRunListener, Closeable {

        public int stage = 0;
        public boolean failed = false;
        public boolean completed = false;
        public boolean advanced = false;
        public boolean warning = false;
        public String name;
        public String statusMessage;
        private final PrintWriter out;
        private final SaffronDataSource data;
        public RunConfiguration runConfig;
        private final ObjectMapper mapper;
        private final ObjectWriter writer;
        private final File outputFolder;

        public Status(PrintWriter out, SaffronDataSource data) {
            this.out = out;
            this.data = data;
            this.outputFolder = null;
            this.mapper = new ObjectMapper();
            this.writer = mapper.writerWithDefaultPrettyPrinter();
        }

        public Status(PrintWriter out, SaffronDataSource data, File outputFolder) {
            this.out = out;
            this.data = data;

            this.outputFolder = outputFolder;
            this.mapper = new ObjectMapper();
            this.writer = mapper.writerWithDefaultPrettyPrinter();
        }

        public void setStatusMessage(String statusMessage) {
            logger.atInfo().log("[STAGE %d] %s\n", stage, statusMessage);
            if (out != null) {
                out.printf("[STAGE %d] %s\n", stage, statusMessage);
            }
            if (out != null) {
                out.flush();
            }
        }

        public void setErrorMessage(String errorMessage) {
            logger.atSevere().log("[STAGE %d] %s\n", stage, errorMessage);
            this.statusMessage = errorMessage;
        }

        public void setWarningMessage(String warningMessage) {
            logger.atWarning().log("[STAGE %d] %s\n", stage, warningMessage);
            this.statusMessage = warningMessage;
        }

        @Override
        public void setStageStart(String statusMessage, String taxonomyId) {
            logger.atInfo().log("[STAGE %d] %s\n", stage, statusMessage);
            String run = data.getRun(taxonomyId);

            if (out != null) {
                out.printf("[STAGE %d] %s\n", stage, statusMessage);
            }
            if (out != null) {
                out.flush();
            }
            this.statusMessage = statusMessage;
        }

        @Override
        public void setStageComplete(String statusMessage, String taxonomyId) {
            String run = data.getRun(taxonomyId);
            stage++;
        }

        @Override
        public void warning(String message, Throwable cause) {
            this.warning = true;
            setWarningMessage("Warning: " + message);
            cause.printStackTrace();
            if (out != null) {
                cause.printStackTrace(out);
            }
            if (out != null) {
                out.flush();
            }
        }

        @Override
        public void fail(String message, Throwable cause) {
            this.failed = true;

            setErrorMessage("Failed: " + message);

            data.remove(name);
            if (cause != null) {
                cause.printStackTrace();
                if (out != null) {
                    cause.printStackTrace(out);
                }
                logger.atSevere().log("Failed due to " + cause.getClass().getName() + ": " + message);
            } else {
                logger.atSevere().log("Failed: " + message);
            }
            if (out != null) {
                out.flush();
            }
        }

        @Override
        public void log(String message) {
            logger.atInfo().log("[STAGE %d] %s\n", stage, message);
            if (out != null) {
                out.println(message);
            }
            if (out != null) {
                out.flush();
            }
        }

        @Override
        public void endTick() {
            logger.atInfo().log("[STAGE %d] %s\n", stage, "");
            if (out != null) {
                out.println();
            }
            if (out != null) {
                out.flush();
            }
        }

        @Override
        public void tick() {
            logger.atInfo().log("[STAGE %d] %s\n", stage, ".");
            if (out != null) {
                out.print(".");
            }
            if (out != null) {
                out.flush();
            }
        }

        @Override
        public void close() throws IOException {
            if (out != null) {
                out.close();
            }
        }

        @Override
        public void setTerms(String saffronDatasetName, List<Term> terms) {
            data.setTerms(saffronDatasetName, terms);
            try {
                File outputFolder2 = new File(outputFolder.getName() + "/" + name);
                writer.writeValue(new File(outputFolder2, "terms.json"), terms);
            } catch(IOException x) {
                throw new RuntimeException(x);
            }
        }

        @Override
        public void setDocTerms(String saffronDatasetName, List<DocumentTerm> docTerms) {
            data.setDocTerms(saffronDatasetName, docTerms);
            try {
                File outputFolder2 = new File(outputFolder.getName() + "/" + name);
                writer.writeValue(new File(outputFolder2, "doc-terms.json"), docTerms);
            } catch(IOException x) {
                throw new RuntimeException(x);
            }
        }

        @Override
        public void setCorpus(String saffronDatasetName, Corpus searcher) {
            data.setCorpus(saffronDatasetName, searcher);
            try {
                File outputFolder2 = new File(outputFolder + "/" + name);
                writer.writeValue(new File(outputFolder2, "corpus.json"), searcher);
            } catch(IOException x) {
                throw new RuntimeException(x);
            }
        }

        @Override
        public void setAuthorTerms(String saffronDatasetName, Collection<AuthorTerm> authorTerms) {
            data.setAuthorTerms(saffronDatasetName, authorTerms);
            try {
                File outputFolder2 = new File(outputFolder + "/" + name);
                writer.writeValue(new File(outputFolder2, "author-terms.json"), authorTerms);
            } catch(IOException x) {
                throw new RuntimeException(x);
            }
        }

        @Override
        public void setTermSim(String saffronDatasetName, List<TermTerm> termSimilarity) {
            data.setTermSim(saffronDatasetName, termSimilarity);
            try {
                File outputFolder2 = new File(outputFolder + "/" + name);
                writer.writeValue(new File(outputFolder2, "term-sim.json"), termSimilarity);
            } catch(IOException x) {
                throw new RuntimeException(x);
            }
        }

        @Override
        public void setAuthorSim(String saffronDatasetName, List<AuthorAuthor> authorSim) {
            data.setAuthorSim(saffronDatasetName, authorSim);
            try {
                File outputFolder2 = new File(outputFolder + "/" + name);
                writer.writeValue(new File(outputFolder2, "author-sim.json"), authorSim);
            } catch(IOException x) {
                throw new RuntimeException(x);
            }
        }

        @Override
        public void setTaxonomy(String saffronDatasetName, Taxonomy graph) {
            data.setTaxonomy(saffronDatasetName, graph);
            try {
                File outputFolder2 = new File(outputFolder + "/" + name);
                writer.writeValue(new File(outputFolder2, "taxonomy.json"), graph);
            } catch(IOException x) {
                throw new RuntimeException(x);
            }
        }

        @Override
        public void setKnowledgeGraph(String saffronDatasetName, KnowledgeGraph kGraph) {
            data.setKnowledgeGraph(saffronDatasetName, kGraph);
            try {
                File outputFolder2 = new File(outputFolder + "/" + name);
                writer.writeValue(new File(outputFolder2, "kg.json"), kGraph);
            } catch(IOException x) {
                throw new RuntimeException(x);
            }
        }

        @Override
        public void start(String taxonomyId, Configuration config) {
            data.deleteRun(taxonomyId);
            data.addRun(taxonomyId, new Date(), config);
            this.advanced = false;
        }

        @Override
        public void end(String taxonomyId) {
            this.completed = true;
        }

		@Override
		public void setDomainModelTerms(String saffronDatasetName, Set<Term> terms) {
			//Not available to Web module
		}


    }

}
