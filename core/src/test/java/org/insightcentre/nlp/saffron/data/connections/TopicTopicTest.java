
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
        final String data = "{\""+ TermTerm.JSON_TERM1_ID + "\": \"t1\", \""+ TermTerm.JSON_TERM2_ID + "\": \"t2\", \""+ TermTerm.JSON_SIMILARITY + "\": 0.3 }";
        final TermTerm tt = mapper.readValue(data, TermTerm.class);
        assertEquals("t1", tt.getTerm1());
        assertEquals("t2", tt.getTerm2());
        assertEquals(0.3, tt.getSimilarity(), 0.0);
        final String json = mapper.writeValueAsString(tt);
        assertEquals(tt, mapper.readValue(json, TermTerm.class));
        
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
        final String data = "{\""+ "topic1_id" + "\": \"t1\", \""+ "topic2_id" + "\": \"t2\", \""+ TermTerm.JSON_SIMILARITY + "\": 0.3 }";
        final TermTerm tt = mapper.readValue(data, TermTerm.class);
        assertEquals("t1", tt.getTerm1());
        assertEquals("t2", tt.getTerm2());
        assertEquals(0.3, tt.getSimilarity(), 0.0);
        final String json = mapper.writeValueAsString(tt);
        assertEquals(tt, mapper.readValue(json, TermTerm.class));
        
    }
}