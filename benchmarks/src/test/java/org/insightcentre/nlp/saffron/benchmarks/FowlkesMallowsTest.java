/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.insightcentre.nlp.saffron.benchmarks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Taxonomy;
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
public class FowlkesMallowsTest {
    
    public FowlkesMallowsTest() {
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

    private Taxonomy makeTaxo(String root, Taxonomy... children) {
        return new Taxonomy(root, 0, 0, Arrays.asList(children));
    }
    
    /**
     * Test of clusteringAtLevel method, of class FowlkesMallows.
     */
    @Test
    public void testClusteringAtLevel() {
        System.out.println("clusteringAtLevel");
        Taxonomy t = makeTaxo("",
                makeTaxo("a", makeTaxo("ab")),
                makeTaxo("b", makeTaxo("ba"), makeTaxo("bb", makeTaxo("bbc"))));
        int level = 1;
        List<Set<String>> expResult = new ArrayList<>();
        expResult.add(new HashSet<String>());
        expResult.get(0).add("a");
        expResult.get(0).add("ab");
        expResult.add(new HashSet<String>());
        expResult.get(1).add("b");
        expResult.get(1).add("ba");
        expResult.get(1).add("bb");
        expResult.get(1).add("bbc");
        List<Set<String>> result = FowlkesMallows.clusteringAtLevel(t, level);
        assertEquals(expResult, result);
    }

    /**
     * Test of fowlkesMallows method, of class FowlkesMallows.
     */
    @Test
    public void testFowlkesMallows() {
        System.out.println("fowlkesMallows");
        Taxonomy t1 = makeTaxo("",
                makeTaxo("a", makeTaxo("ab")),
                makeTaxo("b", makeTaxo("ba"), makeTaxo("bb", makeTaxo("bbc"))));
        Taxonomy t2 = makeTaxo("",
                makeTaxo("a", makeTaxo("ab"), makeTaxo("bbc")),
                makeTaxo("b", makeTaxo("ba"), makeTaxo("bb")));
        double expResult = 4.0/Math.sqrt(42.0) / 3.0;
        double result = FowlkesMallows.fowlkesMallows(t1, t2);
        assertEquals(expResult, result, 0.0);
    }
    
}
