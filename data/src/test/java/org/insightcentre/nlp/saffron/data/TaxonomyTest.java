
package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
public class TaxonomyTest {

    public TaxonomyTest() {
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
        final String data = "{\"root\":\"Root topic\",\"children\":[{\"root\": \"topic1\"}]}";
        final Taxonomy taxonomy = mapper.readValue(data, Taxonomy.class);
        assertEquals("Root topic", taxonomy.root);
        assertEquals(1, taxonomy.children.size());
        assertEquals(0, taxonomy.children.get(0).children.size());
        final String json = mapper.writeValueAsString(taxonomy);
        assertEquals(taxonomy, mapper.readValue(json, Taxonomy.class));
    }
}