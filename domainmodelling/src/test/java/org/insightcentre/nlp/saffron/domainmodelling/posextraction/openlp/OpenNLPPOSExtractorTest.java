/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.insightcentre.nlp.saffron.domainmodelling.posextraction.openlp;

import java.io.File;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.POSBearer;
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
public class OpenNLPPOSExtractorTest {
    
    public OpenNLPPOSExtractorTest() {
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
     * Test of getPOSFromUrl method, of class OpenNLPPOSExtractor.
     */
    @Test
    public void testGetPOSFromUrl() throws Exception {
        System.out.println("getPOSFromUrl");
    }

    /**
     * Test of getPOSFromText method, of class OpenNLPPOSExtractor.
     */
    @Test
    public void testGetPOSFromText() throws Exception {
        System.out.println("getPOSFromText");
        File tagModel = new File("models/en-pos-maxent.bin");
        File tokModel = new File("models/en-token.bin");
        File chunkModel = new File("models/en-chunker.bin");
        if(tagModel.exists() && tokModel.exists() && chunkModel.exists()) {
            String documentText = "this is a test file";
            OpenNLPPOSExtractor instance = new OpenNLPPOSExtractor(tokModel, tagModel, chunkModel);
            POSBearer result = instance.getPOSFromText(documentText);
            assertEquals(2, result.getNounList().size());
            assertEquals(1, result.getVerbList().size());
            assertEquals(0, result.getAdjectiveList().size());
            assertEquals(2, result.getNounphraseMap().size());
        }
    }
    
}
