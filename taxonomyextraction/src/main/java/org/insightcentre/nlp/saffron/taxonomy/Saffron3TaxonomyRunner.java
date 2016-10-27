package org.insightcentre.nlp.saffron.taxonomy;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ie.deri.unlp.javaservices.documentindex.DocumentIndexFactory;
import ie.deri.unlp.javaservices.documentindex.DocumentIndexFactory.LuceneAnalyzer;
import ie.deri.unlp.javaservices.documentindex.DocumentIndexer;
import ie.deri.unlp.javaservices.documentindex.DocumentSearcher;
import ie.deri.unlp.javaservices.documentindex.IndexingException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import org.apache.lucene.store.Directory;
import org.insightcentre.nlp.saffron.taxonomy.config.SaffronConfiguration;
import org.insightcentre.nlp.saffron.taxonomy.db.DAO;
import org.insightcentre.nlp.saffron.taxonomy.db.SaffronDB;
import org.insightcentre.nlp.saffron.taxonomy.db.saffron2.MemoryDAO;
import org.insightcentre.nlp.saffron.taxonomy.db.saffron2.SQLiteDAO;
import org.insightcentre.nlp.saffron.taxonomy.db.saffron2.Saffron2DAO;
import org.insightcentre.nlp.saffron.taxonomy.db.Saffron2Paper;
import org.insightcentre.nlp.saffron.taxonomy.graph.DirectedGraph;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class Saffron3TaxonomyRunner {

    public static void main(String[] args) throws Exception {
        if(args.length != 1) {
            System.err.println("Usage:\n\tsaffron config.json");
            System.exit(-1);
        }

        File configFile = new File(args[0]);
        if(!configFile.exists()) {
            System.err.printf("Could not find %s\n", args[0]);
            System.exit(-1);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            SaffronConfiguration config = mapper.readValue(configFile, SaffronConfiguration.class);

            final DAO db;
            
            if(config.jdbcUrl.startsWith("jdbc:mysql")) {
                db = new Saffron2DAO(config.jdbcUrl, config.dbUser, config.dbPass);
            } else if(config.jdbcUrl.startsWith("jdbc:sqlite:")) {
                db = new SQLiteDAO(config.jdbcUrl);
            } else if(config.jdbcUrl.startsWith("mem:")) {
                SaffronDB data = mapper.readValue(new File(config.jdbcUrl.substring(4)), SaffronDB.class);
                db = new MemoryDAO(data);
            } else {
                System.err.println("Unrecognized JDBC URL: " + config.jdbcUrl);
                System.exit(-1);
                 throw new RuntimeException("Unreachable");
            }
            
            System.err.println("Initialising Lucene index...");

            DocumentSearcher ds = indexLucenePapers(db, config.indexPath, !config.usePreviousCalc);

            System.err.println("Getting topics from database...");

            List<String> topics = db.topRankingTopicStrings(config.numTopics);

            System.err.println("Computing taxonomy...");

            DirectedGraph result = TaxonomyConstructor.optimisedSimilarityGraph(db, ds, 
                config.simThreshold, config.spanSize, topics, config.minCommonDocs, config.usePreviousCalc);
            
            mapper.writeValue(config.outputPath, result);

        } catch(JsonParseException x) {
            System.err.println("Could not parse config");
            System.err.println(x.getMessage());
            System.exit(-1);
        } catch(JsonMappingException x) {
            System.err.println("Could not map config");
            System.err.println(x.getMessage());
            System.exit(-1);
        }
    }

    public static void printUsage() {

        System.out.println("Parameters: ");

        System.out.println(" outputPath: Output folder to write graphs, lucene index and PMI calculations.");

        System.out.println(" usePreviousCalc: True/False: If True, use Lucene index and PMI from previous runs.");

        System.out.println(" jdbcUrl: JDBC URL string for the Saffron 2 database.");

        System.out.println(" dbUser: Database username.");

        System.out.println(" dbPass: Database password.");

        System.out.println(" numTopics: Number of top scoring topics to use in taxonomy.");

        System.out.println(" simThreshold: Threshold for similarity score.");

        System.out.println(" spanSize: Spanslop Lucene value to be used when computing SumPMI.");

        System.out.println(" minCommonDocs: Minimum number of documents that two topics should both occur in to be considered for edge construction stage.");

        System.exit(1);

    }

    public static DocumentSearcher indexLucenePapers(DAO db, File indexPath, boolean overwriteIndex)
        throws IOException, IndexingException, SQLException {

        boolean indexExists = indexPath.exists();

        Directory directory = DocumentIndexFactory.luceneFileDirectory(indexPath, false);

        LuceneAnalyzer analyzer = DocumentIndexFactory.LuceneAnalyzer.LOWERCASE_ONLY;

        if (!indexExists || overwriteIndex) {

            indexDocs(db, directory, analyzer);

        }

        return DocumentIndexFactory.luceneSearcher(directory, analyzer);

    }

    private static void indexDocs(DAO db, Directory directory, LuceneAnalyzer analyzer)
        throws IndexingException, SQLException, IOException {

        DocumentIndexer indexer = DocumentIndexFactory.luceneIndexer(directory, analyzer);

        int indexed = 0;

        Iterator<Saffron2Paper> papers = db.getPapers();

        while (papers.hasNext()) {

            Saffron2Paper paper = papers.next();

            if (paper.getText() != null && paper.getText().length() > 0) {

                indexer.indexDoc(paper.getId(), paper.getText());

                indexed++;

                if (indexed % 100 == 0) {

                    System.out.printf("Indexed %d papers\n", indexed);

                }

            }

        }

        indexer.close();

    }

        

        
}
