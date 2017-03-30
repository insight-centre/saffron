/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.insightcentre.nlp.saffron.taxonomy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.data.connections.DocumentTopic;
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
public class SimpleTaxonomyTest {

    public SimpleTaxonomyTest() {
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
     * Test of optimisedSimilarityGraph method, of class SimpleTaxonomy.
     */
    @Test
    public void testOptimisedSimilarityGraph() {
        System.out.println("optimisedSimilarityGraph");
        List<DocumentTopic> docTopics = Arrays.asList(
                new DocumentTopic("doc1", "topic1", 1, null, null),
                new DocumentTopic("doc1", "topic2", 1, null, null),
                new DocumentTopic("doc2", "topic2", 1, null, null),
                new DocumentTopic("doc2", "topic3", 1, null, null),
                new DocumentTopic("doc2", "topic4", 1, null, null),
                new DocumentTopic("doc3", "topic4", 1, null, null),
                new DocumentTopic("doc3", "topic5", 1, null, null)
        );
        Map<String, Topic> topics = new HashMap<String, Topic>() {{
             put("topic1", new Topic("topic1", 1, 0, 0, Collections.EMPTY_LIST));
             put("topic2", new Topic("topic2", 2, 0, 0, Collections.EMPTY_LIST));
             put("topic3", new Topic("topic3", 1, 0, 0, Collections.EMPTY_LIST));
             put("topic4", new Topic("topic4", 2, 0, 0, Collections.EMPTY_LIST));
             put("topic5", new Topic("topic5", 1, 0, 0, Collections.EMPTY_LIST));
        }};
        Taxonomy result = SimpleTaxonomy.optimisedSimilarityGraph(docTopics, topics);
        assertEquals("topic4", result.root);
        assertEquals(2, result.children.size());
        assertEquals("topic5", result.children.get(0).root);
        assertEquals("topic2", result.children.get(1).root);
        assertEquals(2, result.children.get(1).children.size());
        assertEquals(0, result.children.get(0).children.size());
        assertEquals(0, result.children.get(1).children.get(0).children.size());
    }

    /**
     * Test of getMinimumSpanningTree method, of class SimpleTaxonomy.
     */
    @Test
    public void testGetMinimumSpanningTree() {
        System.out.println("getMinimumSpanningTree");
        List<Topic> topics = Arrays.asList(
                new Topic("0", 0, 0, 0.1, Collections.EMPTY_LIST),
                new Topic("1", 0, 0, 0.1, Collections.EMPTY_LIST),
                new Topic("2", 0, 0, 0.1, Collections.EMPTY_LIST),
                new Topic("3", 0, 0, 0.1, Collections.EMPTY_LIST),
                new Topic("4", 0, 0, 0.1, Collections.EMPTY_LIST),
                new Topic("5", 0, 0, 0.1, Collections.EMPTY_LIST),
                new Topic("6", 0, 0, 0.1, Collections.EMPTY_LIST),
                new Topic("7", 0, 0, 0.1, Collections.EMPTY_LIST),
                new Topic("8", 0, 0, 0.1, Collections.EMPTY_LIST)
        );
        SimpleTaxonomy.WeightedGraph graph = new SimpleTaxonomy.WeightedGraph(Arrays.asList(
                new SimpleTaxonomy.Edge(topics.get(7), topics.get(6), 19),
                new SimpleTaxonomy.Edge(topics.get(8), topics.get(2), 18),
                new SimpleTaxonomy.Edge(topics.get(6), topics.get(5), 18),
                new SimpleTaxonomy.Edge(topics.get(0), topics.get(1), 16),
                new SimpleTaxonomy.Edge(topics.get(2), topics.get(5), 16),
                new SimpleTaxonomy.Edge(topics.get(8), topics.get(6), 14),
                new SimpleTaxonomy.Edge(topics.get(2), topics.get(3), 13),
                new SimpleTaxonomy.Edge(topics.get(7), topics.get(8), 13),
                new SimpleTaxonomy.Edge(topics.get(0), topics.get(7), 12),
                new SimpleTaxonomy.Edge(topics.get(1), topics.get(2), 12),
                new SimpleTaxonomy.Edge(topics.get(3), topics.get(4), 11),
                new SimpleTaxonomy.Edge(topics.get(5), topics.get(4), 10),
                new SimpleTaxonomy.Edge(topics.get(1), topics.get(7), 9),
                new SimpleTaxonomy.Edge(topics.get(3), topics.get(5), 6)
        ));
        SimpleTaxonomy.WeightedGraph expResult = new SimpleTaxonomy.WeightedGraph(Arrays.asList(
                new SimpleTaxonomy.Edge(topics.get(7), topics.get(6), 19),
                new SimpleTaxonomy.Edge(topics.get(8), topics.get(2), 18),
                new SimpleTaxonomy.Edge(topics.get(6), topics.get(5), 18),
                new SimpleTaxonomy.Edge(topics.get(0), topics.get(1), 16),
                new SimpleTaxonomy.Edge(topics.get(2), topics.get(5), 16),
                new SimpleTaxonomy.Edge(topics.get(2), topics.get(3), 13),
                new SimpleTaxonomy.Edge(topics.get(0), topics.get(7), 12),
                new SimpleTaxonomy.Edge(topics.get(3), topics.get(4), 11)
        ));
        SimpleTaxonomy.WeightedGraph result = SimpleTaxonomy.getMinimumSpanningTree(graph);
        assertEquals(expResult.getEdges(), result.getEdges());
    }

}
