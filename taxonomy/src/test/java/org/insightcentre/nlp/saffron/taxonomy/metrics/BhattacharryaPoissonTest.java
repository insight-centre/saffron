package org.insightcentre.nlp.saffron.taxonomy.metrics;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.insightcentre.nlp.saffron.data.TaxoLink;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jmccrae
 */
public class BhattacharryaPoissonTest {
    
    public BhattacharryaPoissonTest() {
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

    /**
     * Test of deltaScore method, of class BhattacharryaPoisson.
     */
    @Test
    public void testDeltaScore() {
        System.out.println("deltaScore");
        HierarchicalScore instance = new BhattacharryaPoisson(new NullScore(),
                new HashSet<>(Arrays.asList("", "a","b","c","d")), 2.0, 1.0);
        // Initial BP = 0.0
        assertEquals(2.081, instance.deltaScore(new TaxoLink("", "a")), 0.001);
        // Now BP = 2.081
        instance = instance.next(new TaxoLink("", "a"), null);
        // Now BP = 2.081
        assertEquals(0.0, instance.deltaScore(new TaxoLink("", "b")), 0.001);
        instance = instance.next(new TaxoLink("", "b"), null);
        // Now BP = 1.699
        assertEquals(-0.382, instance.deltaScore(new TaxoLink("", "c")), 0.001);
        instance = instance.next(new TaxoLink("", "c"), null);
        // Now BP = 2.673
        assertEquals(0.973, instance.deltaScore(new TaxoLink("b", "d")), 0.001);
    }

}
