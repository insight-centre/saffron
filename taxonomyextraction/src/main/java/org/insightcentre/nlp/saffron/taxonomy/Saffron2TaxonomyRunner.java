package org.insightcentre.nlp.saffron.taxonomy;

import ie.deri.unlp.javaservices.documentindex.DocumentIndexFactory;
import ie.deri.unlp.javaservices.documentindex.DocumentIndexFactory.LuceneAnalyzer;
import ie.deri.unlp.javaservices.documentindex.DocumentIndexer;
import ie.deri.unlp.javaservices.documentindex.DocumentSearcher;
import ie.deri.unlp.javaservices.documentindex.IndexingException;
import org.insightcentre.nlp.saffron.taxonomy.db.DAO;
import org.insightcentre.nlp.saffron.taxonomy.db.saffron2.Saffron2DAO;
import org.insightcentre.nlp.saffron.taxonomy.db.Saffron2Paper;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.store.Directory;

public class Saffron2TaxonomyRunner {

    private static Logger logger = Logger.getLogger(Saffron2TaxonomyRunner.class);

    public static void main(String[] args) throws Exception {

        if (args.length != 9) {

            printUsage();

            System.exit(1);

        }

        File outputPath = new File(args[0]);

        boolean usePreviousCalc = Boolean.parseBoolean(args[1].toLowerCase());

        String jdbcUrl = args[2], dbUser = args[3], dbPass = args[4];

        int numTopics = Integer.parseInt(args[5]);

        double simThreshold = Double.parseDouble(args[6]);

        int spanSize = Integer.parseInt(args[7]);

        int minCommonDocs = Integer.parseInt(args[8]);

        DAO db = new Saffron2DAO(jdbcUrl, dbUser, dbPass);


        logger.info("Initialising Lucene index...");

        outputPath.mkdirs();

        DocumentSearcher ds = indexLucenePapers(db, outputPath, !usePreviousCalc);

        logger.info("Getting topics from database...");

        List<String> topics = db.topRankingTopicStrings(numTopics);

        logger.info("Computing taxonomy...");

        TaxonomyConstructor.optimisedSimilarityGraph(db, ds, 
            simThreshold, spanSize, topics, minCommonDocs, usePreviousCalc);

        logger.info("Creating GraphML file...");

        //String dotFile = new File(outputPath, TaxonomyConstructor.prunedGraphOutput).getAbsolutePath();

        //String graphMLFile = new File(outputPath, "graph.graphml").getAbsolutePath();

        throw new RuntimeException("TODO");
//        GephiProcess.dotToGraphML(dotFile, graphMLFile);

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

    public static DocumentSearcher indexLucenePapers(DAO db, File outputPath, boolean overwriteIndex)
        throws IOException, IndexingException, SQLException {

        File indexPath = new File(FilenameUtils.concat(outputPath.getAbsolutePath(), "index"));

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
