package org.insightcentre.nlp.saffron.term;

import opennlp.tools.postag.POSTagger;
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
        POSTagger tagger = mock(POSTagger.class);
        when(tagger.tag(tokens)).thenReturn(new String[] { "DT", "VBZ", "DT", "NN" });
        
        TermExtractionTask instance = new TermExtractionTask(doc, tagger, tokenizer);
        FrequencyStats expResult = new FrequencyStats();
        expResult.docFrequency.put("test", 1);
        expResult.termFrequency.put("test", 1);
        expResult.tokens = 4;
        expResult.documents = 1;
        FrequencyStats result = instance.call();
        assertEquals(expResult, result);
    }
    
}
