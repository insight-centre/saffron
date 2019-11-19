package org.insightcentre.nlp.saffron.taxonomy.supervised;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
        Set<String> terms = new HashSet<>();
        terms.add("");
        terms.add("a");
        terms.add("b");
        terms.add("c");
        terms.add("ab");
        terms.add("ac");
        terms.add("abc");
        terms.add("ba");
        terms.add("bd");
        
        HeadAndBag instance = new HeadAndBag(new TestSupervisedTaxo(), 0.5);
        Taxonomy result = instance.extractTaxonomy(terms);
        assertEquals("", result.root);
        assertEquals(2, result.children.size());
    }
    
}
