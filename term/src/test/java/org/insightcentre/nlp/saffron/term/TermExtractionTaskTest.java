package org.insightcentre.nlp.saffron.term;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import opennlp.tools.lemmatizer.Lemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.SearchException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class TermExtractionTaskTest {

    public TermExtractionTaskTest() {
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

    /**
     * Test of call method, of class TermExtractionTask.
     */
    @Test
    public void testCall() throws Exception {
        System.out.println("call");
        Document doc = mock(Document.class);
        when(doc.contents()).thenReturn("this is a test");
        Tokenizer _tokenizer = mock(Tokenizer.class);
        String[] tokens = new String[]{"this", "is", "a", "test"};
        String[] lemmas = new String[]{"this", "be", "a", "test"};
        final String[] tags = new String[]{"DT", "VBZ", "DT", "NN"};
        when(_tokenizer.tokenize("this is a test")).thenReturn(tokens);
        final Lemmatizer lemmatizer = mock(Lemmatizer.class);
        when(lemmatizer.lemmatize(tokens, tags)).thenReturn(lemmas);
        final POSTagger tagger = mock(POSTagger.class);
        when(tagger.tag(tokens)).thenReturn(tags);
        FrequencyStats result = new FrequencyStats();
        ConcurrentLinkedQueue<DocumentTopic> dts = new ConcurrentLinkedQueue<>();
        CasingStats casing = new CasingStats();
        ThreadLocal<Tokenizer> tokenizer = new ThreadLocal<Tokenizer>() {
            @Override
            protected Tokenizer initialValue() {
                return _tokenizer;
            }

        };
        TermExtractionTask instance = new TermExtractionTask(doc, new ThreadLocal<POSTagger>() {
            @Override
            public POSTagger get() {
                return tagger;
            }

        }, new ThreadLocal<Lemmatizer>() {
            @Override
            protected Lemmatizer initialValue() {
                return lemmatizer;
            }

        }, tokenizer, new HashSet<>(Arrays.asList(TermExtractionConfiguration.ENGLISH_STOPWORDS)), 1, 4,
                new HashSet<String>(),
                new HashSet<String>(),
                new HashSet<String>(Arrays.asList(new String[]{"NN", "NNS"})),
                true, result, dts, casing, Collections.EMPTY_SET);
        FrequencyStats expResult = new FrequencyStats();
        expResult.docFrequency.put("test", 1);
        expResult.termFrequency.put("test", 1);
        expResult.tokens = 4;
        expResult.documents = 1;
        instance.run();
        CasingStats expCasing = new CasingStats();
        expCasing.addCasing("test");
        assertEquals(expResult, result);
        assertEquals(1, dts.size());
        assertEquals(expCasing, casing);
    }

    /**
     * Test of call method, of class TermExtractionTask.
     */
    @Test
    public void testCallMiddle() throws Exception {
        System.out.println("call");
        Document doc = mock(Document.class);
        when(doc.contents()).thenReturn("this is a test of steel");
        Tokenizer _tokenizer = mock(Tokenizer.class);
        String[] tokens = new String[]{"this", "is", "a", "test", "of", "steel"};
        String[] lemmas = new String[]{"this", "be", "a", "test", "of", "steel"};
        final String[] tags = new String[]{"DT", "VBZ", "DT", "NN", "IN", "NN"};
        when(_tokenizer.tokenize("this is a test of steel")).thenReturn(tokens);

        ThreadLocal<Tokenizer> tokenizer = new ThreadLocal<Tokenizer>() {
            @Override
            protected Tokenizer initialValue() {
                return _tokenizer;
            }

        };
        final Lemmatizer lemmatizer = mock(Lemmatizer.class);
        when(lemmatizer.lemmatize(tokens, tags)).thenReturn(lemmas);
        final POSTagger tagger = mock(POSTagger.class);
        when(tagger.tag(tokens)).thenReturn(tags);
        FrequencyStats result = new FrequencyStats();
        TermExtractionTask instance = new TermExtractionTask(doc, new ThreadLocal<POSTagger>() {
            @Override
            public POSTagger get() {
                return tagger;
            }

        }, new ThreadLocal<Lemmatizer>() {
            @Override
            protected Lemmatizer initialValue() {
                return lemmatizer;
            }

        }, tokenizer, new HashSet<>(Arrays.asList(TermExtractionConfiguration.ENGLISH_STOPWORDS)), 1, 4,
                new HashSet<String>(Arrays.asList(new String[]{"NN", "NNS"})),
                new HashSet<String>(Arrays.asList(new String[]{"IN"})),
                new HashSet<String>(Arrays.asList(new String[]{"NN", "NNS"})),
                true, result, null, null, Collections.EMPTY_SET);
        FrequencyStats expResult = new FrequencyStats();
        expResult.docFrequency.put("test", 1);
        expResult.termFrequency.put("test", 1);
        expResult.docFrequency.put("test of steel", 1);
        expResult.termFrequency.put("test of steel", 1);
        expResult.docFrequency.put("steel", 1);
        expResult.termFrequency.put("steel", 1);
        expResult.tokens = 6;
        expResult.documents = 1;
        instance.run();
        assertEquals(expResult, result);
    }

    /**
     * Test of call method, of class TermExtractionTask.
     */
    @Test
    public void testCallNgramMax() throws Exception {
        System.out.println("call");
        Document doc = mock(Document.class);
        when(doc.contents()).thenReturn("this is a test of steel");
        Tokenizer _tokenizer = mock(Tokenizer.class);
        String[] tokens = new String[]{"this", "is", "a", "test", "of", "steel"};
        String[] lemmas = new String[]{"this", "be", "a", "test", "of", "steel"};
        final String[] tags = new String[]{"DT", "VBZ", "DT", "NN", "IN", "NN"};
        when(_tokenizer.tokenize("this is a test of steel")).thenReturn(tokens);

        ThreadLocal<Tokenizer> tokenizer = new ThreadLocal<Tokenizer>() {
            @Override
            protected Tokenizer initialValue() {
                return _tokenizer;
            }

        };
        final Lemmatizer lemmatizer = mock(Lemmatizer.class);
        when(lemmatizer.lemmatize(tokens, tags)).thenReturn(lemmas);
        final POSTagger tagger = mock(POSTagger.class);
        when(tagger.tag(tokens)).thenReturn(tags);
        FrequencyStats result = new FrequencyStats();
        TermExtractionTask instance = new TermExtractionTask(doc, new ThreadLocal<POSTagger>() {
            @Override
            public POSTagger get() {
                return tagger;
            }

        }, new ThreadLocal<Lemmatizer>() {
            @Override
            protected Lemmatizer initialValue() {
                return lemmatizer;
            }

        }, tokenizer, new HashSet<>(Arrays.asList(TermExtractionConfiguration.ENGLISH_STOPWORDS)), 1, 2,
                new HashSet<String>(Arrays.asList(new String[]{"NN", "NNS"})),
                new HashSet<String>(Arrays.asList(new String[]{"IN"})),
                new HashSet<String>(Arrays.asList(new String[]{"NN", "NNS"})),
                true, result, null, null, Collections.EMPTY_SET);
        FrequencyStats expResult = new FrequencyStats();
        expResult.docFrequency.put("test", 1);
        expResult.termFrequency.put("test", 1);
        expResult.docFrequency.put("steel", 1);
        expResult.termFrequency.put("steel", 1);
        expResult.tokens = 6;
        expResult.documents = 1;
        instance.run();
        assertEquals(expResult, result);
    }

    /**
     * Test of call method, of class TermExtractionTask.
     */
    @Test
    public void testCallHeadInitial() throws Exception {
        System.out.println("call");
        Document doc = mock(Document.class);
        when(doc.contents()).thenReturn("an fhaiche mhor");
        Tokenizer _tokenizer = mock(Tokenizer.class);
        String[] tokens = new String[]{"an", "fhaiche", "mhor"};
        String[] lemmas = new String[]{"an", "faiche", "mor"};
        final String[] tags = new String[]{"DT", "NN", "JJ"};
        when(_tokenizer.tokenize("an fhaiche mhor")).thenReturn(tokens);

        ThreadLocal<Tokenizer> tokenizer = new ThreadLocal<Tokenizer>() {
            @Override
            protected Tokenizer initialValue() {
                return _tokenizer;
            }

        };
        final Lemmatizer lemmatizer = mock(Lemmatizer.class);
        when(lemmatizer.lemmatize(tokens, tags)).thenReturn(lemmas);
        final POSTagger tagger = mock(POSTagger.class);
        when(tagger.tag(tokens)).thenReturn(tags);
        FrequencyStats result = new FrequencyStats();
        TermExtractionTask instance = new TermExtractionTask(doc, new ThreadLocal<POSTagger>() {
            @Override
            public POSTagger get() {
                return tagger;
            }

        }, new ThreadLocal<Lemmatizer>() {
            @Override
            protected Lemmatizer initialValue() {
                return lemmatizer;
            }

        }, tokenizer, new HashSet<>(Arrays.asList(TermExtractionConfiguration.ENGLISH_STOPWORDS)), 1, 4,
                new HashSet<String>(Arrays.asList(new String[]{"NN", "JJ"})),
                new HashSet<String>(Arrays.asList(new String[]{"IN"})),
                new HashSet<String>(Arrays.asList(new String[]{"NN", "NNS"})),
                false, result, null, null, Collections.EMPTY_SET);
        FrequencyStats expResult = new FrequencyStats();
        expResult.docFrequency.put("faiche mhor", 1);
        expResult.termFrequency.put("faiche mhor", 1);
        expResult.docFrequency.put("faiche", 1);
        expResult.termFrequency.put("faiche", 1);
        expResult.tokens = 3;
        expResult.documents = 1;
        instance.run();
        assertEquals(expResult, result);
    }

    //@Test
    public void testOpenNlp() throws Exception {
        String s = "The term anarchism is a compound word composed from the word anarchy and the suffix -ism, themselves derived respectively from the Greek , i.e. anarchy (from , anarchos, meaning \"one without rulers\"; from the privative prefix ἀν- (an-, i.e. \"without\") and , archos, i.e. \"leader\", \"ruler\"; (cf. archon or , arkhē, i.e. \"authority\", \"sovereignty\", \"realm\", \"magistracy\")) and the suffix  or  (-ismos, -isma, from the verbal infinitive suffix -ίζειν, -izein). The first known use of this word was in 1539. Various factions within the French Revolution labelled opponents as anarchists (as Robespierre did the Hébertists) although few shared many views of later anarchists.  There would be many revolutionaries of the early nineteenth century who contributed to the anarchist doctrines of the next generation, such as William Godwin and Wilhelm Weitling, but they did not use the word anarchist or anarchism in describing themselves or their beliefs.";
        String[] tokens = SimpleTokenizer.INSTANCE.tokenize(s);
        String[] tags = new POSTaggerME(new POSModel(new File("../models/en-pos-maxent.bin"))).tag(tokens);

    }

    @Test
    public void testOpenNLPTokenizer() throws Exception {
        File f = new File("../models/en-token.bin");
        if (f.exists()) {
            Tokenizer tokenizer = new TokenizerME(new TokenizerModel(f));
            //Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
            String[] tokens = tokenizer.tokenize("401k account");
            assertArrayEquals(new String[]{"401k", "account"}, tokens);
        }
    }

    @Test
    public void testExtraction() throws Exception {
        String s = "We don't want any bugs in Saffron 3.1";
        final File file = new File("../models/en-token.bin");
        if (file.exists()) {
            //String[] tokens = SimpleTokenizer.INSTANCE.tokenize(s);
            String[] tokens = new TokenizerME(new TokenizerModel(file)).tokenize(s);

            assertArrayEquals(new String[]{"We", "do", "n't", "want", "any", "bugs", "in", "Saffron", "3.1"}, tokens);
            final File file1 = new File("../models/en-pos-maxent.bin");
            if (file1.exists()) {

                String[] tags = new POSTaggerME(new POSModel(file1)).tag(tokens);

                assertArrayEquals(new String[]{"PRP", "VBP", "RB", "VB", "DT", "NNS", "IN", "NNP", "CD"}, tags);
            }
        }
    }

    /**
     * Test of call method, of class TermExtractionTask.
     */
    @Test
    public void testBlackList() throws Exception {
        System.out.println("call");
        Document doc = mock(Document.class);
        when(doc.contents()).thenReturn("this is a test of steel");
        Tokenizer _tokenizer = mock(Tokenizer.class);
        String[] tokens = new String[]{"this", "is", "a", "test", "of", "steel"};
        String[] lemmas = new String[]{"this", "be", "a", "test", "of", "steel"};
        final String[] tags = new String[]{"DT", "VBZ", "DT", "NN", "IN", "NN"};
        when(_tokenizer.tokenize("this is a test of steel")).thenReturn(tokens);

        ThreadLocal<Tokenizer> tokenizer = new ThreadLocal<Tokenizer>() {
            @Override
            protected Tokenizer initialValue() {
                return _tokenizer;
            }

        };
        final Lemmatizer lemmatizer = mock(Lemmatizer.class);
        when(lemmatizer.lemmatize(tokens, tags)).thenReturn(lemmas);
        final POSTagger tagger = mock(POSTagger.class);
        when(tagger.tag(tokens)).thenReturn(tags);
        FrequencyStats result = new FrequencyStats();
        Set<String> blacklist = new HashSet<>();
        blacklist.add("steel");
        TermExtractionTask instance = new TermExtractionTask(doc, new ThreadLocal<POSTagger>() {
            @Override
            public POSTagger get() {
                return tagger;
            }

        }, new ThreadLocal<Lemmatizer>() {
            @Override
            protected Lemmatizer initialValue() {
                return lemmatizer;
            }

        }, tokenizer, new HashSet<>(Arrays.asList(TermExtractionConfiguration.ENGLISH_STOPWORDS)), 1, 4,
                new HashSet<String>(Arrays.asList(new String[]{"NN", "NNS"})),
                new HashSet<String>(Arrays.asList(new String[]{"IN"})),
                new HashSet<String>(Arrays.asList(new String[]{"NN", "NNS"})),
                true, result, null, null, blacklist);
        FrequencyStats expResult = new FrequencyStats();
        expResult.docFrequency.put("test", 1);
        expResult.termFrequency.put("test", 1);
        expResult.tokens = 6;
        expResult.documents = 1;
        instance.run();
        assertEquals(expResult, result);
    }

    @Test
    public void testBlackList2() throws Exception {
        File tokenizerFile = new File("../models/en-token.bin");
        if (tokenizerFile.exists()) {

            String[] tokens = new String[]{"this", "is", "a", "@test_of", "@test_of", "steel"};
            final String[] tags = new String[]{"DT", "VBZ", "DT", "NN", "NN", "NN"};
            final POSTagger tagger = mock(POSTagger.class);
            when(tagger.tag(tokens)).thenReturn(tags);
            Tokenizer _tokenizer = new TokenizerME(new TokenizerModel(tokenizerFile));

            ThreadLocal<Tokenizer> tokenizer = new ThreadLocal<Tokenizer>() {
                @Override
                protected Tokenizer initialValue() {
                    return _tokenizer;
                }

            };
            Document doc = mock(Document.class);
            when(doc.contents()).thenReturn("this is a @test_of @test_of steel");
            final Lemmatizer lemmatizer = new Lemmatizer() {
                @Override
                public String[] lemmatize(String[] toks, String[] tags) {
                    return toks;
                }

                @Override
                public List<List<String>> lemmatize(List<String> toks, List<String> tags) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            };
            FrequencyStats result = new FrequencyStats();
            Set<String> blacklist = new HashSet<>();
            blacklist.add("@test_of");
            TermExtractionTask instance = new TermExtractionTask(doc, new ThreadLocal<POSTagger>() {
                @Override
                public POSTagger get() {
                    return tagger;
                }

            }, new ThreadLocal<Lemmatizer>() {
                @Override
                protected Lemmatizer initialValue() {
                    return lemmatizer;
                }

            }, tokenizer, new HashSet<>(Arrays.asList(TermExtractionConfiguration.ENGLISH_STOPWORDS)), 1, 4,
                    new HashSet<String>(Arrays.asList(new String[]{"NN", "NNS"})),
                    new HashSet<String>(Arrays.asList(new String[]{"IN"})),
                    new HashSet<String>(Arrays.asList(new String[]{"NN", "NNS"})),
                    true, result, null, null, blacklist);
            FrequencyStats expResult = new FrequencyStats();
            expResult.docFrequency.put("steel", 1);
            expResult.termFrequency.put("steel", 1);
            expResult.tokens = 6;
            expResult.documents = 1;
            instance.run();
            assertEquals(expResult, result);
        }
    }
}
