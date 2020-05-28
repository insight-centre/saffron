package org.insightcentre.nlp.saffron.authors.sim;

import java.util.Iterator;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author John McCrae
 */
public class TopNListTest {

    public TopNListTest() {
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

    private final Random random = new Random();
    /**
     * Test of size method, of class TopNList.
     */
    @Test
    public void testSize() {
        System.out.println("size");
        TopNList instance = new TopNList(10);
        int expResult = 0;
        int result = instance.size();
        assertEquals(expResult, result);
        for(int i = 0; i < 20; i++) {
            instance.offer(random.nextDouble(), random.nextDouble());
        }
        assertEquals(10, instance.size());
    }

    /**
     * Test of offer method, of class TopNList.
     */
    @Test
    public void testOffer() {
        System.out.println("offer");
        double a = 1.0;
        double score = 1.0;
        TopNList<Double> instance = new TopNList<Double>(1);
        boolean expResult = true;
        boolean result = instance.offer(a, score);
        assertEquals(expResult, result);
        assertEquals(true, instance.offer(2.0, 2.0));
        assertEquals(false, instance.offer(1.0, 1.0));
    }

    /**
     * Test of iterator method, of class TopNList.
     */
    @Test
    public void testIterator() {
        System.out.println("iterator");
        TopNList<Double> instance = new TopNList<>(10);
        for(double i = 0; i < 15; i++) {
            instance.offer(i, i);
        }
        Iterator result = instance.iterator();
        double d = 14;
        while(result.hasNext()) {
            assertEquals(d, (double)result.next(), 0.00001);
            d -= 1;
        }
    }
    
    @Test
    public void testEmpty() {
        TopNList<Double> instance = new TopNList<>(10);
        for(double d : instance) {
            fail("Iterating returned value for empty list");
        }
    }

}