package org.insightcentre.nlp.saffron.taxonomy.metrics;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.insightcentre.nlp.saffron.data.TaxoLink;
import org.insightcentre.nlp.saffron.taxonomy.search.TaxonomySolution;
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
public class TransitiveScoreTest {

    public TransitiveScoreTest() {
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

    /**
     * Test of deltaScore method, of class TransitiveScore.
     */
    @Test
    public void testDeltaScore() throws IOException {
        System.out.println("deltaScore");
        TaxoLink tl = new TaxoLink("a", "ab");
        TaxonomySolution soln = TaxonomySolution.empty(new HashSet<String>(Arrays.asList("", "a", "ab", "abc")));
        HierarchicalScore instance = new TransitiveScore(new TestSupervisedTaxo());
        instance = instance.next(new TaxoLink("", "a"), soln.add("", "a", 0, 0, 0, false));
        instance = instance.next(new TaxoLink("ab", "abc"), soln.add("ab", "abc", 0, 0, 0, false));
        double expResult = 2.0 / 3.0 + 1.0 / 3.0 + 2.0 / 4.0 + 1.0/4.0 - 0.5 * 4;
        double result = instance.deltaScore(tl);
        assertEquals(expResult, result, 0.000001);
    }

    @Test
    public void testAddMany() throws IOException {
        TaxoLink[] tls = new TaxoLink[]{
            new TaxoLink("", "a"), // 1/2
            new TaxoLink("", "b"), // 1/2
            new TaxoLink("", "c"), // 1/2
            new TaxoLink("a", "ab"), // 2/3 + 1/3
            new TaxoLink("a", "ac"), // 2/3 + 1/3
            new TaxoLink("b", "ba"), // 2/3 + 1/3
            new TaxoLink("ba", "bac"), // 3/4 + 2/4 + 1/4
            new TaxoLink("bac", "bacd"), // 4/5 + 3/5 + 2/5 + 1/5
            new TaxoLink("c", "ca"), // 2/3 + 1/3
            new TaxoLink("c", "cb"), // 2/3 + 1/3
            new TaxoLink("cb", "cbc"), // 3/4 + 2/4 + 1/4
            new TaxoLink("cbc", "cbcc"), // 4/5 + 3/5 + 2/5 + 1/5
            new TaxoLink("cb", "cba"), // 3/4 + 2/4 + 1/4
            new TaxoLink("cbc", "cbcd") // 4/5 + 3/5 + 2/5 + 1/5
        };
        double expSolution = 0.0; //(1.0/2.0) * 3 + (1.0) * 5 + 6.0/4.0 * 3 + 2.0 * 3 - 34 * 0.5
        for (int i = 0; i < 5; i++) {
            shuffleArray(tls);
            TaxonomySolution soln = TaxonomySolution.empty(new HashSet<String>());
            HierarchicalScore instance = new TransitiveScore(new TestSupervisedTaxo());
            double score = 0.0;
            for (TaxoLink tl : tls) {
                score += instance.deltaScore(tl);
                instance = instance.next(tl, soln.add(tl.getTop(), tl.getBottom(), 0.0, 0.0, 0.0, false));
                
            }
            assertEquals(expSolution, score, 0.001);
        }
    }

    // Implementing Fisher–Yates shuffle
    static void shuffleArray(TaxoLink[] ar) {
        // If running on Java 6 or older, use `new Random()` on RHS here
        Random rnd = ThreadLocalRandom.current();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            TaxoLink a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }
}
