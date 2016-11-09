
package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.util.List;
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
public class TopicTest {

    public TopicTest() {
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
        final String data = "{\"topic_string\": \"topic\", \"occurrences\": 5, \"mv_list\": [{\"string\":\"mv1\"}]}";
        final Topic topic = mapper.readValue(data, Topic.class);
        assertEquals("topic", topic.topicString);
        assertEquals(5, topic.occurrences);
        assertEquals(1, topic.mvList.size());
        assertEquals("mv1", topic.mvList.get(0).string);
        final String json = mapper.writeValueAsString(topic);
        assertEquals(topic, mapper.readValue(json, Topic.class));
        
    }
}