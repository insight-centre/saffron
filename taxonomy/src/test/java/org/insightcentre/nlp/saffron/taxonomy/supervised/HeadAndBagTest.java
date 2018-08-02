package org.insightcentre.nlp.saffron.taxonomy.supervised;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
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
public class HeadAndBagTest {
    
    public HeadAndBagTest() {
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

    private static class TestSupervisedTaxo extends SupervisedTaxo {

        public TestSupervisedTaxo() throws IOException {
            super((Features)null, null, null);
        }

        @Override
        public double predict(String top, String bottom) {
            if(bottom.startsWith(top)) {
                return (double)(top.length() + 1.0) / (bottom.length() + 1.0);
            } else {
                return 0.0;
            }
        }
        
    }
    
    /**
     * Test of extractTaxonomy method, of class GreedySplitTaxoExtract.
     */
    @Test
    public void testExtractTaxonomy() throws Exception {
        System.out.println("extractTaxonomy");
        Set<String> topics = new HashSet<>();
        topics.add("");
        topics.add("a");
        topics.add("b");
        topics.add("c");
        topics.add("ab");
        topics.add("ac");
        topics.add("abc");
        topics.add("ba");
        topics.add("bd");
        
        HeadAndBag instance = new HeadAndBag(new TestSupervisedTaxo(), 0.5);
        Taxonomy result = instance.extractTaxonomy(topics);
        assertEquals("", result.root);
        assertEquals(2, result.children.size());
    }
    
}
