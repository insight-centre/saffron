package org.insightcentre.nlp.saffron.term.enrich;

import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.tokenize.Tokenizer;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.term.FrequencyStats;
import org.insightcentre.nlp.saffron.term.enrich.EnrichTopics.WordTrie;
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
 * @author John McCrae
 */
public class EnrichTopicTaskTest {

    public EnrichTopicTaskTest() {
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
     * Test of run method, of class EnrichTopicTask.
     */
    @Test
    public void testRun() {
        System.out.println("run");
        String docText = "this is a test document that should work";
        String[] tokens = docText.split(" ");
        Document doc = new Document(null, "test", null, "test", null, Collections.EMPTY_LIST, Collections.EMPTY_MAP, docText);
        ThreadLocal<POSTagger> tagger = new ThreadLocal<POSTagger>() {
            @Override
            protected POSTagger initialValue() {
                POSTagger t = mock(POSTagger.class);
                when(t.tag(tokens)).thenReturn(new String[] { "X","X","X","X","X","X","X","X" });
                        return t;
            }
        };
        ThreadLocal<Tokenizer> tokenizer = new ThreadLocal<Tokenizer>() {
            @Override
            protected Tokenizer initialValue() {
                Tokenizer t = mock(Tokenizer.class);
                when(t.tokenize(docText)).thenReturn(tokens);
                return t;
            }
            
        };
        FrequencyStats summary = new FrequencyStats();
        WordTrie topicStrings = new WordTrie("");
        topicStrings.addTokenized(new String[] { "test", "document" });
        topicStrings.addTokenized(new String[] { "document" });
        topicStrings.addTokenized(new String[] { "work" });
        ConcurrentLinkedQueue<DocumentTerm> docTopics = new ConcurrentLinkedQueue<>();
                
        
        EnrichTopicTask instance = new EnrichTopicTask(doc, tagger, null, tokenizer, summary, topicStrings, docTopics);
        instance.run();
        
        assertEquals(3, summary.termFrequency.size());
        assert(summary.termFrequency.containsKey("test document"));
    }

}