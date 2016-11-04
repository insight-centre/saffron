
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
public class DomainModelTest {
    ObjectMapper mapper = new ObjectMapper();

    public DomainModelTest() {
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
        final String data = "{\"terms\":[\"term1\",\"term2\"]}";
        final DomainModel domainModel = mapper.readValue(data, DomainModel.class);
        assertEquals(2, domainModel.terms.size());
        assertEquals("term1", domainModel.terms.get(0));
        assertEquals("term2", domainModel.terms.get(1));
        final String json = mapper.writeValueAsString(domainModel);
        assertEquals(domainModel, mapper.readValue(json, DomainModel.class));
        
    }
}