
package org.insightcentre.nlp.saffron.data;

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
public class AuthorTest {
    ObjectMapper mapper = new ObjectMapper();

    public AuthorTest() {
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
        final String data = "{\"name\":\"Joe Bloggs\"}";
        final Author author = mapper.readValue(data, Author.class);
        assertEquals("Joe Bloggs", author.name);
        final String json = mapper.writeValueAsString(author);
        assertEquals(author, mapper.readValue(json, Author.class));
        
    }
}