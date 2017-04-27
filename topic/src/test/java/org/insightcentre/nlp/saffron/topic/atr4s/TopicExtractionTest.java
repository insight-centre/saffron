package org.insightcentre.nlp.saffron.topic.atr4s;

import org.insightcentre.nlp.saffron.topic.atr4s.TopicExtraction;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import org.insightcentre.nlp.saffron.topic.atr4s.Main.Configuration;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
import org.insightcentre.nlp.saffron.data.index.SearchException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author jmccrae
 */
public class TopicExtractionTest {
    
    public TopicExtractionTest() {
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
     * Test of extractTopics method, of class TopicExtraction.
     */
    @org.junit.Test
    public void testExtractTopics() throws SearchException {
        System.out.println("extractTopics");
        Document d1 = mock(Document.class);
        when(d1.getId()).thenReturn("id1");
        when(d1.getContents()).thenReturn("The IBM company. IBM is a company.");
        DocumentSearcher searcher = mock(DocumentSearcher.class);
        when(searcher.allDocuments()).thenReturn(Arrays.asList(d1));
        Configuration config = new Configuration();
        config.method = Main.WeightingMethod.one;
        config.features = Arrays.asList(Main.Feature.weirdness);
        File f = new File("src/test/resources/termoccs.txt");
        if(f.exists()) {
            config.corpus = f.getAbsolutePath();
            TopicExtraction instance = new TopicExtraction(config);
            TopicExtraction.Result expResult = new TopicExtraction.Result();
            expResult.topics = new HashSet<>();
            expResult.topics.add(new Topic("company", 2, 1, 655.4725860581262, Arrays.asList(new Topic.MorphologicalVariation("company"))));
            expResult.docTopics = new ArrayList<>();
            expResult.docTopics.add(new DocumentTopic("id1", "company", 2, null, null));
            TopicExtraction.Result result = instance.extractTopics(searcher);
            assertEquals(expResult, result);
        } else {
            System.err.println("Skip test as term co-occ file not downloaded");
        }
    }
    
}
