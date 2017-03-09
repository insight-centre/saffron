
package org.insightcentre.nlp.saffron.topic.gate;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.insightcentre.nlp.saffron.topic.ExtractedTopic;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jmccrae
 */
public class TopicExtractorGateTest {
    final File gateHome = new File("/home/jmccrae/external/gate");

    public TopicExtractorGateTest() {
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
     * Test of extractTopics method, of class TopicExtractorGate.
     */
    @Test
    public void testExtractTopics() {
        System.out.println("extractTopics");
        String documentText = "this is a test";
        List<String> domainModel = Arrays.asList("test", "word");
        if(gateHome.exists()) {
            TopicExtractorGate instance = new TopicExtractorGate(gateHome);
            List<ExtractedTopic> result = instance.extractTopics(documentText, domainModel);
            System.err.println(result);
        }
    }

    /**
     * Test of initGate method, of class TopicExtractorGate.
     */
    @Test
    public void testInitGate() throws Exception {
        System.out.println("initGate");
        if(gateHome.exists()) {
            TopicExtractorGate instance = new TopicExtractorGate(gateHome);
            instance.initGate();
        }
    }

    @Test
    public void testExtractTopics2() {
        System.out.println("extractTopics");
        String documentText = "The EA Frostbite 2 Engine was further developed into EA Frostbite 3";
        List<String> domainModel = Arrays.asList("test", "word");
        if(gateHome.exists()) {
            TopicExtractorGate instance = new TopicExtractorGate(gateHome);
            List<ExtractedTopic> result = instance.extractTopics(documentText, domainModel);
            System.err.println(result);
        }
 
    }
    
}