
package org.insightcentre.nlp.saffron.data;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

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
        final String data = "{\""+ Term.JSON_TERM_STRING+"\": \"topic\", \""+ Term.JSON_OCCURRENCES + "\": 5, \"" + Term.JSON_MORPHOLOGICAL_VARIATION_LIST + "\": [{\"string\":\"mv1\"}]}";
        final Term topic = mapper.readValue(data, Term.class);
        assertEquals("topic", topic.getString());
        assertEquals(5, topic.getOccurrences());
        assertEquals(1, topic.getMorphologicalVariationList().size());
        assertEquals("mv1", topic.getMorphologicalVariationList().get(0).string);
        topic.setDbpediaUrl(new URL("http://dbpedia.org/resource/Example"));
        final String json = mapper.writeValueAsString(topic);
        assertEquals(topic, mapper.readValue(json, Term.class));
        
    }
}