
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
public class DocumentTopicTest {

    public DocumentTopicTest() {
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
        final String data = "{\"document_id\": \"doc1\", \"topic_string\": \"t1\"}";
        final DocumentTopic tt = mapper.readValue(data, DocumentTopic.class);
        assertEquals("doc1", tt.document_id);
        assertEquals("t1", tt.topic_string);
        final String json = mapper.writeValueAsString(tt);
        assertEquals(tt, mapper.readValue(json, DocumentTopic.class));
        
    }
}