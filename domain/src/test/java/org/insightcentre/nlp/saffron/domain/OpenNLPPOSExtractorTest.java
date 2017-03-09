
package org.insightcentre.nlp.saffron.domain;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import opennlp.tools.chunker.Chunker;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.sentdetect.SentenceDetector;
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
 * @author jmccrae
 */
public class OpenNLPPOSExtractorTest {

    public OpenNLPPOSExtractorTest() {
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
     * Test of processFile method, of class OpenNLPPOSExtractor.
     */
    @Test
    public void testProcessFile_5args_1() throws Exception {
        System.out.println("processFile");
    }

    /**
     * Test of processFile method, of class OpenNLPPOSExtractor.
     */
    @Test
    public void testProcessFile_5args_2() throws IOException {
        System.out.println("processFile");
        POSTagger tagger = mock(POSTagger.class);
        Tokenizer tokenizer = mock(Tokenizer.class);
        Chunker chunker = mock(Chunker.class);
        SentenceDetector detector = mock(SentenceDetector.class);
        
        String documentText = "Welcome to the world of double rent and half standards. Having spent six hours asleep on the tables of an OBriens sandwich bar in the company of the more-than-patient staff, we arrive at our Edinburgh accommodation.".toLowerCase();
        String[] tokens = new String[] {
            "welcome", "to", "the", "world", "of", 
            "double", "rent", "and", "half", "standards", 
            "having", "spent", "six", "hours", "asleep", 
            "on", "the", "tables", "of", "an", 
            "obriens", "sandwich", "bar" };
        String[] tags = new String[] {
            "U", "U", "U", "N", "U", 
            "A", "N", "U", "U", "N", 
            "V", "V", "U", "N", "V", 
            "U", "U", "N", "U", "U", 
            "N", "N", "N" };
        String[] chunks = new String[] {
            "O", "O", "O", "B-NP", "I-NP",
            "I-NP", "I-NP", "O", "B-NP", "I-NP",
            "B-VP", "O", "B-NP", "B-NP", "O",
            "O", "O", "B-NP", "O", "O",
            "B-NP", "I-NP", "I-NP" };
        when(tokenizer.tokenize(documentText)).thenReturn(tokens);
        when(tagger.tag(tokens)).thenReturn(tags);
        when(chunker.chunk(tokens, tags)).thenReturn(chunks);
        when(detector.sentDetect(documentText)).thenReturn(new String[] { documentText });
 
    
        
        Object2IntMap<String> wordFreq = new Object2IntOpenHashMap<>();
        Object2IntMap<Keyphrase> phraseFreq = new Object2IntOpenHashMap<>();
        Object2IntMap<NearbyPair> pairs = new Object2IntOpenHashMap<>();
        int span = 5;
        OpenNLPPOSExtractor instance = new OpenNLPPOSExtractor(tagger, tokenizer, chunker, detector);
        instance.processFile(documentText, wordFreq, phraseFreq, pairs, span);
        assertEquals(11, wordFreq.size());
        assertEquals(6, phraseFreq.size());
        assertEquals(26, pairs.size());
    }

}
