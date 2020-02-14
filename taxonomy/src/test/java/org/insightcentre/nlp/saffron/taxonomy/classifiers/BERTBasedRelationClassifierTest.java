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

    /**
     * Test of predict method, of class BERTBasedRelationClassifier.
     */
    @Test
    public void testPredict() throws Exception {
        System.out.println("predict");
        String simpleMLPFilePath = "../models/model_test_bert_new_softmax.h5";
        String bertModelFilePath = "../models/bert_model_SavedModule";
        String source = "automatic withdrawal";
        String target = "hsa account";
        /*BERTBasedRelationClassifier instance = new BERTBasedRelationClassifier(simpleMLPFilePath, bertModelFilePath);
        double[] expResult = null;
        double[] result = instance.predict(source, target);
        System.err.println(Arrays.toString(result));*/
    }

}