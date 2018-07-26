package org.insightcentre.nlp.saffron.term;

import java.io.File;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import org.insightcentre.nlp.saffron.data.Document;
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
        Tokenizer tokenizer = mock(Tokenizer.class);
        String[] tokens = new String[] {"this","is","a","test" };
        when(tokenizer.tokenize("this is a test")).thenReturn(tokens);
        final POSTagger tagger = mock(POSTagger.class);
        when(tagger.tag(tokens)).thenReturn(new String[] { "DT", "VBZ", "DT", "NN" });
        FrequencyStats result = new FrequencyStats();
        TermExtractionTask instance = new TermExtractionTask(doc, new ThreadLocal<POSTagger>() {
            @Override
            public POSTagger get() {
                return tagger;
            }
            
        }, tokenizer, result);
        FrequencyStats expResult = new FrequencyStats();
        expResult.docFrequency.put("test", 1);
        expResult.termFrequency.put("test", 1);
        expResult.tokens = 4;
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
}
