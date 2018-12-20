/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.insightcentre.nlp.saffron.term.lda;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.SearchException;
import org.insightcentre.nlp.saffron.term.FrequencyStats;
import org.insightcentre.nlp.saffron.term.TermExtraction;
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
public class NovelTopicModelTest {

    public NovelTopicModelTest() {
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
     * Test of initialize method, of class NovelTopicModel.
     */
    @Test
    public void testInitialize() throws Exception {
        System.out.println("initialize");
        DocumentSearcher searcher = new DocumentSearcher() {
            @Override
            public Iterable<Document> getDocuments() {
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

            @Override
            public int size() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void updateDocument(String id, Document doc) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
           
            
        };
        Tokenizer _tokenizer = WhitespaceTokenizer.INSTANCE;
        
        ThreadLocal<Tokenizer> tokenizer = new ThreadLocal<Tokenizer>() { 
            @Override
            protected Tokenizer initialValue() {
                return _tokenizer;
            }
          
        };
        NovelTopicModel result = NovelTopicModel.initialize(searcher, tokenizer);

    }

    /**
     * Test of novelTopicModel method, of class NovelTopicModel.
     */
    @Test
    public void testNovelTopicModel() throws IOException, SearchException {
        System.out.println("novelTopicModel");
        
        DocumentSearcher searcher = new DocumentSearcher() {
            @Override
            public Iterable<Document> getDocuments() {
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

            @Override
            public int size() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void updateDocument(String id, Document doc) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
            
            

        };
        Tokenizer _tokenizer = WhitespaceTokenizer.INSTANCE;
        
        ThreadLocal<Tokenizer> tokenizer = new ThreadLocal<Tokenizer>() { 
            @Override
            protected Tokenizer initialValue() {
                return _tokenizer;
            }
          
        };
        NovelTopicModel instance = NovelTopicModel.initialize(searcher, tokenizer);

        String term = "this test";
        FrequencyStats stats = stats();
        double expResult = 4.0;
        double result = instance.novelTopicModel(term, stats);
        assertEquals(expResult, result, 2.0);
    }

}
