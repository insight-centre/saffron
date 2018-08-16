package org.insightcentre.nlp.saffron.term;

import org.insightcentre.nlp.saffron.config.TermExtractionConfiguration;
import org.insightcentre.nlp.saffron.term.lda.NovelTopicModel;
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
public class FeaturesTest {

    public FeaturesTest() {
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

    public FrequencyStats stats() {
        FrequencyStats stats = new FrequencyStats();
        stats.docFrequency.put("this test", 2);
        stats.docFrequency.put("test", 3);
        stats.docFrequency.put("this test case", 1);
        stats.termFrequency.put("this test", 5);
        stats.termFrequency.put("test", 10);
        stats.termFrequency.put("this test case", 1);
        stats.tokens = 100;
        stats.documents = 3;
        return stats;
    }

    public FrequencyStats ref() {
        FrequencyStats stats = new FrequencyStats();
        stats.docFrequency.put("this test", 20);
        stats.docFrequency.put("test", 30);
        stats.termFrequency.put("this test", 50);
        stats.termFrequency.put("test", 100);
        stats.tokens = 1000;
        stats.documents = 50;
        return stats;
    }

    /**
     * Test of calcFeature method, of class Features.
     */
    @Test
    public void testCalcFeature() {
        System.out.println("calcFeature");
        TermExtractionConfiguration.Feature feat = TermExtractionConfiguration.Feature.weirdness;
        String term = "this test";
        final FrequencyStats stats = stats();
        Lazy<FrequencyStats> ref = new Lazy<FrequencyStats>() {
            @Override
            protected FrequencyStats init() {
                return ref();
            }
        };
        Lazy<InclusionStats> incl = new Lazy<InclusionStats>() {
            @Override
            public InclusionStats init() {
                return new InclusionStats(stats.termFrequency);
            }
        };
        double expResult = 1.017049;
        double result = Features.calcFeature(feat, term, stats, ref, incl, null);
        assertEquals(expResult, result, 0.00001);
    }

    /**
     * Test of weirdness method, of class Features.
     */
    @Test
    public void testWeirdness() {
        System.out.println("weirdness");
        String term = "this test";
        FrequencyStats stats = stats();
        FrequencyStats ref = ref();
        double expResult = 1.017049;
        double result = Features.weirdness(term, stats, ref);
        assertEquals(expResult, result, 0.0001);
    }

    /**
     * Test of aveTermFreq method, of class Features.
     */
    @Test
    public void testAveTermFreq() {
        System.out.println("aveTermFreq");
        String term = "this test";
        FrequencyStats stats = stats();
        double expResult = 2.5;
        double result = Features.aveTermFreq(term, stats);
        assertEquals(expResult, result, 0.0001);
    }

    /**
     * Test of residualIDF method, of class Features.
     */
    @Test
    public void testResidualIDF() {
        System.out.println("residualIDF");
        String term = "this test";
        FrequencyStats stats = stats();
        double expResult = 5.1*0.2235399;
        double result = Features.residualIDF(term, stats);
        assertEquals(expResult, result, 0.0001);
    }

    /**
     * Test of totalTFIDF method, of class Features.
     */
    @Test
    public void testTotalTFIDF() {
        System.out.println("totalTFIDF");
        String term = "this test";
        FrequencyStats stats = stats();
        double expResult = 2.027326;
        double result = Features.totalTFIDF(term, stats);
        assertEquals(expResult, result, 0.0001);
    }

    /**
     * Test of cValue method, of class Features.
     */
    @Test
    public void testCValue() {
        System.out.println("cValue");
        String term = "this test";
        FrequencyStats freq = stats();
        InclusionStats incl = new InclusionStats(freq.docFrequency);
        double expResult = 4.281557;
        double result = Features.cValue(term, freq, incl);
        assertEquals(expResult, result, 0.0001);
    }

    /**
     * Test of basic method, of class Features.
     */
    @Test
    public void testBasic() {
        System.out.println("basic");
        String term = "this test";
        double alpha = 0.75;
        FrequencyStats freq = stats();
        InclusionStats incl = new InclusionStats(freq.termFrequency);
        double expResult = 5.450994;
        double result = Features.basic(term, alpha, freq, incl);
        assertEquals(expResult, result, 0.0001);
    }

    /**
     * Test of basicCombo method, of class Features.
     */
    @Test
    public void testBasicCombo() {
        System.out.println("basicCombo");
        String term = "this test";
        double alpha = 0.75;
        double beta = 0.1;
        FrequencyStats freq = stats();
        InclusionStats incl = new InclusionStats(freq.termFrequency);
        double expResult = 5.550994;
        double result = Features.basicCombo(term, alpha, beta, freq, incl);
        assertEquals(expResult, result, 0.0001);
    }

    /**
     * Test of relevance method, of class Features.
     */
    @Test
    public void testRelevance() {
        System.out.println("relevance");
        String term = "this test";
        FrequencyStats freq = stats();
        FrequencyStats ref = ref();
        double expResult = 0.2963561;
        double result = Features.relevance(term, freq, ref);
        assertEquals(expResult, result, 0.0001);
    }

}
