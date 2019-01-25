package org.insightcentre.nlp.saffron.term;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Sequence;
import opennlp.tools.util.Span;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.term.TermExtraction.Result;
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
public class TermExtractionTest {

    public TermExtractionTest() {
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

    /**
     * Test of extractStats method, of class TermExtraction.
     */
    @Test
    public void testExtractStats() throws Exception {
        System.out.println("extractStats");
        final POSTagger tagger = new POSTagger() {
            @Override
            public String[] tag(String[] strings) {
                String[] x = new String[strings.length];
                for (int i = 0; i < strings.length; i++) {
                    if ("test".equals(strings[i])) {
                        x[i] = "NN";
                    } else if ("good".equals(strings[i])) {
                        x[i] = "JJ";
                    } else {
                        x[i] = "DT";
                    }
                }
                return x;
            }

            @Override
            public String[] tag(String[] strings, Object[] os) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Sequence[] topKSequences(String[] strings) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Sequence[] topKSequences(String[] strings, Object[] os) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        Tokenizer _tokenizer = new Tokenizer() {
            @Override
            public String[] tokenize(String string) {
                return string.split(" ");
            }

            @Override
            public Span[] tokenizePos(String string) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };

        ThreadLocal<Tokenizer> tokenizer = new ThreadLocal<Tokenizer>() {
            @Override
            protected Tokenizer initialValue() {
                return _tokenizer;
            }

        };
        Corpus searcher = new Corpus() {
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
            public int size() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
            
        };
        TermExtraction instance = new TermExtraction(10, new ThreadLocal<POSTagger>() {
            @Override
            protected POSTagger initialValue() {
                return tagger;
            }

        }, tokenizer);
        FrequencyStats expResult = new FrequencyStats();
        FrequencyStats result = instance.extractStats(searcher, null, null);
        expResult.docFrequency.put("test", 4);
        expResult.docFrequency.put("good test", 2);
        expResult.termFrequency.put("test", 5);
        expResult.termFrequency.put("good test", 2);
        expResult.tokens = 21;
        expResult.documents = 4;
        assertEquals(expResult, result);
    }

    /**
     * Test of extractTopics method, of class TermExtraction.
     */
    @Test
    public void testExtractTopics() throws Exception {
        System.out.println("extractStats");
        final POSTagger tagger = new POSTagger() {
            @Override
            public String[] tag(String[] strings) {
                String[] x = new String[strings.length];
                for (int i = 0; i < strings.length; i++) {
                    if ("test".equals(strings[i])) {
                        x[i] = "NN";
                    } else if ("good".equals(strings[i])) {
                        x[i] = "JJ";
                    } else {
                        x[i] = "DT";
                    }
                }
                return x;
            }

            @Override
            public String[] tag(String[] strings, Object[] os) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Sequence[] topKSequences(String[] strings) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Sequence[] topKSequences(String[] strings, Object[] os) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        Tokenizer _tokenizer = new Tokenizer() {
            @Override
            public String[] tokenize(String string) {
                return string.split(" ");
            }

            @Override
            public Span[] tokenizePos(String string) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };

        ThreadLocal<Tokenizer> tokenizer = new ThreadLocal<Tokenizer>() {
            @Override
            protected Tokenizer initialValue() {
                return _tokenizer;
            }

        };
        Corpus searcher = new Corpus() {
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
            public int size() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
            
        };
        TermExtraction instance = new TermExtraction(10, new ThreadLocal<POSTagger>() {
            @Override
            protected POSTagger initialValue() {
                return tagger;
            }

        }, tokenizer);
        Result res = instance.extractTopics(searcher);
    }
    
        /**
     * Test of extractTopics method, of class TermExtraction.
     */
    @Test
    public void testExtractTopics2() throws Exception {
        System.out.println("extractStats");
        final POSTagger tagger = new POSTagger() {
            @Override
            public String[] tag(String[] strings) {
                String[] x = new String[strings.length];
                for (int i = 0; i < strings.length; i++) {
                    if ("test".equals(strings[i]) || "exam".equals(strings[i])) {
                        x[i] = "NN";
                    } else if ("good".equals(strings[i])) {
                        x[i] = "JJ";
                    } else {
                        x[i] = "DT";
                    }
                }
                return x;
            }

            @Override
            public String[] tag(String[] strings, Object[] os) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Sequence[] topKSequences(String[] strings) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Sequence[] topKSequences(String[] strings, Object[] os) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        Tokenizer _tokenizer = new Tokenizer() {
            @Override
            public String[] tokenize(String string) {
                return string.split(" ");
            }

            @Override
            public Span[] tokenizePos(String string) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };

        ThreadLocal<Tokenizer> tokenizer = new ThreadLocal<Tokenizer>() {
            @Override
            protected Tokenizer initialValue() {
                return _tokenizer;
            }

        };
        Corpus searcher = new Corpus() {
            @Override
            public Iterable<Document> getDocuments() {
                return Arrays.asList(new Document[]{
                    mkDoc("this is a test"),
                    mkDoc("this is also a test"),
                    mkDoc("this is a good exam"),
                    mkDoc("a good test is also a test")
                });
            }

            @Override
            public int size() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
            
        };
        TermExtractionConfiguration config = new TermExtractionConfiguration();
        TermExtraction instance = new TermExtraction(10, new ThreadLocal<POSTagger>() {
            @Override
            protected POSTagger initialValue() {
                return tagger;
            }

        }, tokenizer, 
                config.maxDocs, 
                0, 
                null, 
                new HashSet<String>(Arrays.asList(TermExtractionConfiguration.ENGLISH_STOPWORDS)),
                config.preceedingTokens,
                config.headTokens,
                config.middleTokens,  
                config.ngramMin,  
                config.ngramMax,  
                config.headTokenFinal,  
                config.method,  
                config.features,  
                null,  
                1,  
                config.baseFeature,  
                config.blacklist,  
                true);
        Result res = instance.extractTopics(searcher);
        assert(res.topics.size() > 1);
    }


}
