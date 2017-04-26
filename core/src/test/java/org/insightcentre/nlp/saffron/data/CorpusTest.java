
package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
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
public class CorpusTest {
    ObjectMapper mapper = new ObjectMapper();

    public CorpusTest() {
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
        final String data = "{\"documents\":[]}";
        final IndexedCorpus corpus = mapper.readValue(data, IndexedCorpus.class);
        assertEquals(Collections.EMPTY_LIST, corpus.documents);
        final String json = mapper.writeValueAsString(corpus);
        assertEquals(corpus, mapper.readValue(json, IndexedCorpus.class));
        
    }
}