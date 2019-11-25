
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
public class TermTest {

    public TermTest() {
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
        final String data = "{\""+ Term.JSON_TERM_STRING+"\": \"term\", \""+ Term.JSON_OCCURRENCES + "\": 5, \"" + Term.JSON_MORPHOLOGICAL_VARIATION_LIST + "\": [{\"string\":\"mv1\"}]}";
        final Term term = mapper.readValue(data, Term.class);
        assertEquals("term", term.getString());
        assertEquals(5, term.getOccurrences());
        assertEquals(1, term.getMorphologicalVariationList().size());
        assertEquals("mv1", term.getMorphologicalVariationList().get(0).string);
        term.setDbpediaUrl(new URL("http://dbpedia.org/resource/Example"));
        final String json = mapper.writeValueAsString(term);
        assertEquals(term, mapper.readValue(json, Term.class));
        
    }
    
    /**
     * Test for compatibility with data prepared for version 3.3
     * 
     * To be deprecated in version 4
     * @throws IOException
     */
    @Test
    public void test2() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        final String data = "{\""+ "topic_string" +"\": \"term\", \""+ Term.JSON_OCCURRENCES + "\": 5, \"" + Term.JSON_MORPHOLOGICAL_VARIATION_LIST + "\": [{\"string\":\"mv1\"}]}";
        final Term term = mapper.readValue(data, Term.class);
        assertEquals("term", term.getString());
        assertEquals(5, term.getOccurrences());
        assertEquals(1, term.getMorphologicalVariationList().size());
        assertEquals("mv1", term.getMorphologicalVariationList().get(0).string);
        term.setDbpediaUrl(new URL("http://dbpedia.org/resource/Example"));
        final String json = mapper.writeValueAsString(term);
        assertEquals(term, mapper.readValue(json, Term.class));
        
    }
}