package org.insightcentre.nlp.saffron.taxonomy.search;

import org.insightcentre.nlp.saffron.taxonomy.metrics.SumScore;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Status;
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
            super((Features) null, null, null);
        }

        @Override
        public double predict(String top, String bottom) {
            if (bottom.startsWith(top)) {
                return (double) (top.length() + 1.0) / (bottom.length() + 1.0);
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

    /**
     * Test of extractTaxonomy method, of class GreedySplitTaxoExtract.
     */
    @Test
    public void testExtractTaxonomyWithBlackWhiteList() throws Exception {
        System.out.println("extractTaxonomyWithBlackWhiteList");
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

        Set<TaxoLink> whiteList = new HashSet<>();
        Set<TaxoLink> blackList = new HashSet<>();
        whiteList.add(new TaxoLink("", "ab"));
        blackList.add(new TaxoLink("", "c"));

        BeamSearch instance = new BeamSearch(new SumScore(new TestSupervisedTaxo()), 10);
        Taxonomy result = instance.extractTaxonomyWithBlackWhiteList(topics, whiteList, blackList);
        assertEquals("", result.root);
        assertEquals(3, result.children.size());
        assert (result.children.stream().anyMatch((Taxonomy t) -> t.root.equals("a")));
        assert (result.children.stream().anyMatch((Taxonomy t) -> t.root.equals("b") && t.status == Status.none));
        assert (result.children.stream().anyMatch((Taxonomy t) -> t.root.equals("ab") && t.status == Status.accepted));
    }

    @Test
    public void randomizedTest() throws Exception {
        int n = 10;
        int trials = 100;
        for (int trial = 0; trial < trials; trial++) {
            Random r = new Random(0);
            double[][] scores = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    scores[i][j] = r.nextDouble();
                }
            }
            HashMap<String, Topic> topics = new HashMap<>();
            for (int i = 0; i < n; i++) {
                addTopic(topics, "" + i, 0.0);
            }

            BeamSearch instance = new BeamSearch(new SumScore(new SupervisedTaxo((Features)null, null, null) {
                @Override
                public double predict(String top, String bottom) {
                    int i = Integer.parseInt(top);
                    int j = Integer.parseInt(bottom);
                    return scores[i][j];
                }
            }), 10);
            Taxonomy result = instance.extractTaxonomy(topics);
            assert(result.verifyTree());
            assert(result.scoresValid());
            System.err.println("Passed trial " + trial);
        }

    }
}
