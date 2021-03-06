
package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
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
public class DocumentTest {
    ObjectMapper mapper = new ObjectMapper();
    
    public DocumentTest() {
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
        final String data = "{\"file\":\"test.txt\",\"id\":\"test\",\"name\":\"Test Document\",\"authors\":[{\"name\":\"Joe Bloggs\"}]}";
        final Document document = mapper.readValue(data, Document.class);
        assertEquals("Test Document", document.name);
        assertEquals("test", document.id);
        assertEquals(new SaffronPath("test.txt"), document.file);
        assertEquals(1, document.authors.size());
        assertEquals(new Author("Joe Bloggs"), document.authors.get(0));
        assertEquals(null, document.mimeType);
        final String json = mapper.writeValueAsString(document);
        assertEquals(document, mapper.readValue(json, Document.class));
        
    }
    
    @Test
    public void test2() throws IOException {
        final String data = "{\"contents\":\"These are the document contents\",\"id\":\"test\",\"name\":\"Test Document\",\"authors\":[{\"name\":\"Joe Bloggs\"}]}";
        final Document document = mapper.readValue(data, Document.class);
        assertEquals("Test Document", document.name);
        assertEquals("test", document.id);
        assertEquals("These are the document contents", document.contents());
        assertEquals(1, document.authors.size());
        assertEquals(new Author("Joe Bloggs"), document.authors.get(0));
        assertEquals("text/plain", document.mimeType);
        final String json = mapper.writeValueAsString(document);
        assertEquals(document, mapper.readValue(json, Document.class));
        
    }
    
    @Test
    public void testDate() throws IOException {
        
        final String data = "{\"contents\":\"These are the document contents\",\"id\":\"test\",\"name\":\"Test Document\",\"authors\":[{\"name\":\"Joe Bloggs\"}],\"date\":\"2018-01-01\"}";
        final Document document = mapper.readValue(data, Document.class);
        assertEquals("Test Document", document.name);
        assertEquals("test", document.id);
        assertEquals("These are the document contents", document.contents());
        assertEquals(1, document.authors.size());
        assertEquals(new Author("Joe Bloggs"), document.authors.get(0));
        assertEquals("text/plain", document.mimeType);
        final String json = mapper.writeValueAsString(document);
        assertEquals(document, mapper.readValue(json, Document.class));
        
        mapper.readValue("{\"contents\":\"These are the document contents\",\"id\":\"test\",\"name\":\"Test Document\",\"authors\":[{\"name\":\"Joe Bloggs\"}],\"date\":\"2018-01\"}", Document.class);
        mapper.readValue("{\"contents\":\"These are the document contents\",\"id\":\"test\",\"name\":\"Test Document\",\"authors\":[{\"name\":\"Joe Bloggs\"}],\"date\":\"2018\"}", Document.class);
        mapper.readValue("{\"contents\":\"These are the document contents\",\"id\":\"test\",\"name\":\"Test Document\",\"authors\":[{\"name\":\"Joe Bloggs\"}],\"date\":\"2018-01-01 T15:35\"}", Document.class);
        
    }
}