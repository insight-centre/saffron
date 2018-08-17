package org.insightcentre.nlp.saffron.term.domain;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.SearchException;
import org.insightcentre.nlp.saffron.term.FrequencyStats;
import org.insightcentre.nlp.saffron.term.InclusionStats;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jmccrae
 */
public class DomainStatsTest {
    
    public DomainStatsTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    static int docs = 0;

    private Document mkDoc(String contents) {
        return new Document(null, "doc" + (docs++), null, null, "text/plain", Collections.EMPTY_LIST, Collections.EMPTY_MAP, contents);
    }

    
    public FrequencyStats stats() {
        FrequencyStats stats = new FrequencyStats();
        stats.docFrequency.put("this test", 2);
        stats.docFrequency.put("test", 3);
        stats.docFrequency.put("this test case", 1);
        stats.termFrequency.put("this test", 5);
        stats.termFrequency.put("test", 10);
        stats.termFrequency.put("this test case", 1);
        stats.tokens = 100;
        stats.documents = 3;
        return stats;
    }
    
    /**
     * Test of initialize method, of class DomainStats.
     */
    @Test
    public void testInitialize() throws Exception {
        System.out.println("initialize");
        
        DocumentSearcher searcher = new DocumentSearcher() {
            @Override
            public Iterable<Document> allDocuments() throws SearchException {
                return Arrays.asList(new Document[]{
                    mkDoc("this is a test"),
                    mkDoc("this is also a test"),
                    mkDoc("this is a good test"),
                    mkDoc("a good test is also a test")
                });
            }

            @Override
            public Iterable<Document> search(String searchTerm) throws SearchException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void close() throws IOException {
            }
        };
        int nThreads = 2;
        Tokenizer tokenizer = WhitespaceTokenizer.INSTANCE;
        int maxLength = 2;
        int maxDocs = 10000;
        FrequencyStats stats = stats();
        InclusionStats incl = new InclusionStats(stats.termFrequency);
        DomainStats result = DomainStats.initialize(searcher, nThreads, tokenizer, maxLength, maxDocs, stats, incl);
    }

    /**
     * Test of score method, of class DomainStats.
     */
    @Test
    public void testScore() throws SearchException {
        System.out.println("score");
        String term = "this test";
        
        DocumentSearcher searcher = new DocumentSearcher() {
            @Override
            public Iterable<Document> allDocuments() throws SearchException {
                return Arrays.asList(new Document[]{
                    mkDoc("this is a test"),
                    mkDoc("this is also a test"),
                    mkDoc("this test is good"),
                    mkDoc("a good test is also a test")
                });
            }

            @Override
            public Iterable<Document> search(String searchTerm) throws SearchException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void close() throws IOException {
            }
        };
        int nThreads = 2;
        Tokenizer tokenizer = WhitespaceTokenizer.INSTANCE;
        int maxLength = 2;
        int maxDocs = 10000;
        FrequencyStats stats = stats();
        InclusionStats incl = new InclusionStats(stats.termFrequency);
        DomainStats instance = DomainStats.initialize(searcher, nThreads, tokenizer, maxLength, maxDocs, stats, incl);
        double expResult = 0.3023;
        double result = instance.score(term);
        assertEquals(expResult, result, 0.001);
    }
    
}
