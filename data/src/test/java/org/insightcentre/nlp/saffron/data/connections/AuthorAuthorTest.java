package org.insightcentre.nlp.saffron.data.connections;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.insightcentre.nlp.saffron.data.Author;
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
public class AuthorAuthorTest {

    public AuthorAuthorTest() {
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
        final String data = "{\"author1_id\": \"Joe\", \"author2_id\": \"Mary\", \"similarity\": 0.2 }";
        final AuthorAuthor author = mapper.readValue(data, AuthorAuthor.class);
        assertEquals("Joe", author.author1_id);
        assertEquals("Mary", author.author2_id);
        assertEquals(0.2, author.similarity, 0.0);
        final String json = mapper.writeValueAsString(author);
        assertEquals(author, mapper.readValue(json, AuthorAuthor.class));

    }
}
