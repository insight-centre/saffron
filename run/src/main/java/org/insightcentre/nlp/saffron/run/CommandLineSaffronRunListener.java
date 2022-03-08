package org.insightcentre.nlp.saffron.run;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.insightcentre.nlp.saffron.config.Configuration;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.KnowledgeGraph;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * The saffron run listener that is used on the command line
 * @author John McCrae
 */
public class CommandLineSaffronRunListener implements SaffronRunListener {
    private final ObjectMapper mapper;
    private final ObjectWriter writer;
    private final File outputFolder;
    private int stage = 1;

    public CommandLineSaffronRunListener(File outputFolder) {
        this.outputFolder = outputFolder;
        this.mapper = new ObjectMapper();
        this.writer = mapper.writerWithDefaultPrettyPrinter();
    }
    

    @Override
    public void setDomainModelTerms(String saffronDatasetName, Set<Term> terms) {
        try {
            writer.writeValue(new File(outputFolder, "domain-model.json"), terms);
        } catch(IOException x) {
            throw new RuntimeException(x);
        }
    }
    
    @Override
    public void setTerms(String saffronDatasetName, List<Term> terms) {
        try {
            writer.writeValue(new File(outputFolder, "terms.json"), terms);
        } catch(IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void setDocTerms(String saffronDatasetName, List<DocumentTerm> docTerms) {
        try {
            writer.writeValue(new File(outputFolder, "doc-terms.json"), docTerms);
        } catch(IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void setCorpus(String saffronDatasetName, Corpus searcher) {
        try {
            writer.writeValue(new File(outputFolder, "corpus.json"), searcher);
        } catch(IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void setAuthorTerms(String saffronDatasetName, Collection<AuthorTerm> authorTerms) {
        try {
            writer.writeValue(new File(outputFolder, "author-terms.json"), authorTerms);
        } catch(IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void setTermSim(String saffronDatasetName, List<TermTerm> termSimilarity) {
        try {
            writer.writeValue(new File(outputFolder, "term-sim.json"), termSimilarity);
        } catch(IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void setAuthorSim(String saffronDatasetName, List<AuthorAuthor> authorSim) {
        try {
            writer.writeValue(new File(outputFolder, "author-sim.json"), authorSim);
        } catch(IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void setTaxonomy(String saffronDatasetName, Taxonomy graph) {
        try {
            writer.writeValue(new File(outputFolder, "taxonomy.json"), graph);
        } catch(IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void setKnowledgeGraph(String saffronDatasetName, KnowledgeGraph kGraph) {
        try {
            writer.writeValue(new File(outputFolder, "kg.json"), kGraph);
        } catch(IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public void log(String message) {
        System.err.println("message");
    }

    @Override
    public void tick() {
        System.err.print(".");
    }

    @Override
    public void endTick() {
        System.err.println();
    }

    @Override
    public void setStageComplete(String statusMessage, String taxonomyId) {
        stage++;
    }

    @Override
    public void warning(String message, Throwable cause) {
        System.err.println(message);
        cause.printStackTrace();
    }

    @Override
    public void fail(String message, Throwable cause) {
        System.err.println(message);
        cause.printStackTrace();
    }

    @Override
    public void setStageStart(String statusMessage, String taxonomyId) {
        System.err.println("###################################################");
        System.err.printf("## Step % 2d: %-36s ##\n", stage, statusMessage);
        System.err.println("###################################################");
    }

    @Override
    public void start(String taxonomyId, Configuration configuration) {
    }

}
