/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.insightcentre.nlp.saffron.taxonomy.search;

import org.insightcentre.nlp.saffron.taxonomy.metrics.SumScore;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.Topic;
import org.insightcentre.nlp.saffron.taxonomy.supervised.Features;
import org.insightcentre.nlp.saffron.taxonomy.supervised.SupervisedTaxo;
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
public class BeamSearchTest {
    
    public BeamSearchTest() {
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
    
    private void addTopic(HashMap<String, Topic> topics, String t, double score) {
        topics.put(t, new Topic(t, 0, 0, score, Collections.EMPTY_LIST));
    }
    
    /**
     * Test of extractTaxonomy method, of class BeamSearchSplitTaxoExtract.
     */
    @Test
    public void testExtractTaxonomy() throws Exception {
        System.out.println("extractTaxonomy");
        HashMap<String, Topic> topics = new HashMap<>();
        addTopic(topics, "", 0.0);
        addTopic(topics, "a", 0.0);
        addTopic(topics, "b", 0.0);
        addTopic(topics, "c", 0.0);
        addTopic(topics, "ab", 0.0);
        addTopic(topics, "ac", 0.0);
        addTopic(topics, "abc", 0.0);
        addTopic(topics, "ba", 0.0);
        addTopic(topics, "bd", 0.0);
        
        BeamSearch instance = new BeamSearch(new SumScore(new TestSupervisedTaxo()), 2);
        Taxonomy result = instance.extractTaxonomy(topics);
        assertEquals("", result.root);
        assertEquals(3, result.children.size());
    }
}
