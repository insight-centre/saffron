package org.insightcentre.nlp.saffron.taxonomy.search;

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
public class BeamTest {
    
    public BeamTest() {
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
     * Test of push method, of class Beam.
     */
    @Test
    public void testPush() {
        System.out.println("push");
        String[] k = new String[] { "a", "b", "c", "d", "e" };
        double[] score = new double[] { 1 , 2, 0, -1, 3 };
        Beam instance = new Beam(2);
        assertEquals(true, instance.push(k[0], score[0]));
        assertEquals(true, instance.push(k[1], score[1]));
        assertEquals(false, instance.push(k[2], score[2]));
        assertEquals(false, instance.push(k[3], score[3]));
        assertEquals(true, instance.push(k[4], score[4]));
    }

    /**
     * Test of isEmpty method, of class Beam.
     */
    @Test
    public void testIsEmpty() {
        System.out.println("isEmpty");
        String[] k = new String[] { "a", "b", "c", "d", "e" };
        double[] score = new double[] { 1 , 2, 0, -1, 3 };
        Beam instance = new Beam(2);
        assertEquals(true, instance.isEmpty());
        instance.push(k[0], score[0]);
        instance.push(k[1], score[1]);
        instance.push(k[2], score[2]);
        instance.push(k[3], score[3]);
        instance.push(k[4], score[4]);
        assertEquals(false, instance.isEmpty());
        instance.pop();
        instance.pop();
        assertEquals(true, instance.isEmpty());
        
    }

    /**
     * Test of pop method, of class Beam.
     */
    @Test
    public void testPop() {
        System.out.println("pop");        
        String[] k = new String[] { "a", "b", "c", "d", "e" };
        double[] score = new double[] { 1 , 2, 0, -1, 3 };
        Beam instance = new Beam(2);
        instance.push(k[0], score[0]);
        instance.push(k[1], score[1]);
        instance.push(k[2], score[2]);
        instance.push(k[3], score[3]);
        instance.push(k[4], score[4]);
        assertEquals("e", instance.pop());
        assertEquals("b", instance.pop());
    }
    
}
