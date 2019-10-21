
package org.insightcentre.nlp.saffron.data.connections;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
public class TopicTopicTest {

    public TopicTopicTest() {
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

    @Test
    public void test() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        final String data = "{\""+ TopicTopic.JSON_TERM1_ID + "\": \"t1\", \""+ TopicTopic.JSON_TERM2_ID + "\": \"t2\", \""+ TopicTopic.JSON_SIMILARITY + "\": 0.3 }";
        final TopicTopic tt = mapper.readValue(data, TopicTopic.class);
        assertEquals("t1", tt.getTerm1());
        assertEquals("t2", tt.getTerm2());
        assertEquals(0.3, tt.getSimilarity(), 0.0);
        final String json = mapper.writeValueAsString(tt);
        assertEquals(tt, mapper.readValue(json, TopicTopic.class));
        
    }
}