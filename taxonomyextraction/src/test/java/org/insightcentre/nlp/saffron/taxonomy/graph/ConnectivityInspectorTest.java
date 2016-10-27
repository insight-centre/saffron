/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.insightcentre.nlp.saffron.taxonomy.graph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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
public class ConnectivityInspectorTest {
    
    public ConnectivityInspectorTest() {
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
     * Test of connectedSets method, of class ConnectivityInspector.
     */
    @Test
    public void testConnectedSets() {
        System.out.println("connectedSets");
        Node n1 = new Node(0, "t1");
        Node n2 = new Node(1, "t2");
        Node n3 = new Node(2, "t3");
        Node n4 = new Node(3, "t4");
        DirectedGraph graph = new DirectedGraph(Arrays.asList(n1,n2,n3,n4), 
            new AdjacencyList(
                new Edge(n1, n2, 1.0),
                new Edge(n2, n3, 1.0),
                new Edge(n1, n3, 1.0)));
        ConnectivityInspector instance = new ConnectivityInspector(graph);
        Set<TreeSet<Node>> expResult = new HashSet<TreeSet<Node>>(Arrays.asList(
            new TreeSet<Node>(Arrays.asList(n1,n2,n3)),
            new TreeSet<Node>(Arrays.asList(n4))));
        Set<Set<Node>> result = instance.connectedSets();
        assertEquals(expResult.size(), result.size());
    }
    
}
