
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
        final String data = "{\""+ DocumentTerm.JSON_DOCUMENT_ID +"\": \"doc1\", \""+ DocumentTerm.JSON_TERM_STRING + "\": \"t1\"}";
        final DocumentTerm tt = mapper.readValue(data, DocumentTerm.class);
        assertEquals("doc1", tt.getDocumentId());
        assertEquals("t1", tt.getTermString());
        final String json = mapper.writeValueAsString(tt);
        assertEquals(tt, mapper.readValue(json, DocumentTerm.class));
    }
    
    /**
     * Test compatibility with data from Saffron 3.3
     * 
     * To be deprecated in version 4
     * @throws IOException
     */
    @Test
    public void test2() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        final String data = "{\""+ DocumentTerm.JSON_DOCUMENT_ID +"\": \"doc1\", \""+ "topic_string" + "\": \"t1\"}";
        final DocumentTerm tt = mapper.readValue(data, DocumentTerm.class);
        assertEquals("doc1", tt.getDocumentId());
        assertEquals("t1", tt.getTermString());
        final String json = mapper.writeValueAsString(tt);
        assertEquals(tt, mapper.readValue(json, DocumentTerm.class));
    }
}