
package org.insightcentre.nlp.saffron.documentindex.tika;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import org.insightcentre.nlp.saffron.data.Document;
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
public class DocumentAnalyzerTest {

    public DocumentAnalyzerTest() {
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
     * Test of analyze method, of class DocumentAnalyzer.
     */
    @Test
    public void testAnalyze() throws Exception {
        System.out.println("analyze");
        Document txtFile = new Document(new File("src/test/resources/test.txt"), "test" , "test", null, Collections.EMPTY_LIST, Collections.EMPTY_MAP, null);
        Document pdfFile = new Document(new File("src/test/resources/test.pdf"), "test" , "test", null, Collections.EMPTY_LIST, Collections.EMPTY_MAP, null);
        Document docFile = new Document(new File("src/test/resources/test.doc"), "test" , "test", null, Collections.EMPTY_LIST, Collections.EMPTY_MAP, null);
        DocumentAnalyzer instance = new DocumentAnalyzer();
        
        for(Document d : Arrays.asList(txtFile, pdfFile, docFile)) {
            Document result = instance.analyze(d);
            assertEquals("This is a test file.", result.contents().trim());
        }
    }

}