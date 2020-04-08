package org.insightcentre.nlp.saffron.taxonomy.classifiers;

import java.util.Arrays;
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
public class BERTBasedRelationClassifierTest {

    public BERTBasedRelationClassifierTest() {
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
    
    String toNiceString(double[] d) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(double f : d) {
            sb.append(String.format("%.3f", f));
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    /**
     * Test of predict method, of class BERTBasedRelationClassifier.
     */
    @Test
    public void testPredict() throws Exception {
        System.out.println("predict");
        String simpleMLPFilePath = "../models/model_test_bert_new_softmax.h5";
        String bertModelFilePath = "../models/bert_model_SavedModule";
//        String[][] tests = new String[][] { 
//            new String[] { "hardship withdrawl", "next year" },
//            new String[] { "traditional ira", "phone number" },
//            new String[] { "bank account", "interest rate" },
//            new String[] { "automatic withdrawal", "hsa account" },
//            new String[] { "account", "bank account" }
//        };
//        BERTBasedRelationClassifier instance = new BERTBasedRelationClassifier(simpleMLPFilePath, bertModelFilePath);
//        for(String [] s : tests) {
//            System.err.printf("%s <=> %s = %s\n", s[0], s[1], toNiceString(instance.predict(s[0], s[1])));
//            System.err.printf("%s <=> %s = %s\n", s[1], s[0], toNiceString(instance.predict(s[1], s[0])));
//            System.err.printf("%s <=> %s = %s\n", s[0], s[0], toNiceString(instance.predict(s[0], s[0])));
//            System.err.printf("%s <=> %s = %s\n", s[1], s[1], toNiceString(instance.predict(s[1], s[1])));
//        }
    }

}