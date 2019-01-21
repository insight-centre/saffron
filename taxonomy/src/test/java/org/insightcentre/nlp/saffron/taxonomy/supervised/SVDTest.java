package org.insightcentre.nlp.saffron.taxonomy.supervised;

import Jama.Matrix;
import java.util.Arrays;
import java.util.List;
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
public class SVDTest {

    public SVDTest() {
    
    
    }
    
    private Matrix vectorByAve(String sent) {
        String[] sents = PrettyGoodTokenizer.tokenize(sent);
        if (sents.length == 0) {
            throw new RuntimeException("Empty sentence");
        }
        int m = 0;
        int n = 1;
        Matrix v = null;
        while (v == null && m < sents.length) {
            v = getVectorForWord(sents[m++]);
        }
        for (int i = m; i < sents.length; i++) {
            Matrix v2 = getVectorForWord(sents[i]);
            if (v2 != null) {
                v.plus(v2);
                n++;
            }
        }
        return v == null ? null : v.times(1.0 / n);
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
     * Test of solve method, of class SVD.
     */
    @Test
    public void testSolve() {
        System.out.println("solve");
        SVD instance = new SVDImpl();
        
        instance.addExample("test", "example", 1.0);
        instance.addExample("thing", "test", 0.7);
        instance.addExample("thing", "cat", 0.6);
        
        instance.solve();
        
        
    }

    private final List<String> words = Arrays.asList(new String[] { "test", "example", "thing", "cat" });
    
    private Matrix getVectorForWord(String sent) {
        double[] v = new double[words.size()];
        v[words.indexOf(sent)] = 1;
        return new Matrix(v, v.length);
    }

    public class SVDImpl extends SVD {

       @Override
        public double[] vector(String t) {
            Matrix v = vectorByAve(t);
            return v == null ? null : v.getColumnPackedCopy();
        }
    }

}