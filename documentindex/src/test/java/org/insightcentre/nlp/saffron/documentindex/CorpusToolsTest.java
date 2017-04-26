package org.insightcentre.nlp.saffron.documentindex;

import java.io.File;
import org.insightcentre.nlp.saffron.data.Corpus;
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
public class CorpusToolsTest {
    
    public CorpusToolsTest() {
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
     * Test of fromFolder method, of class CorpusTools.
     */
    @Test
    public void testFromFolder() {
        System.out.println("fromFolder");
        File folder = new File("src/test/resources/corpus");
        Corpus result = CorpusTools.fromFolder(folder);
        int files = 0;
        boolean found = true;
        for(Document d : result.getDocuments()) {
            files++;
            found = found ||
                    d.name.equals("2002-GWBush.txt");
        }
        assertEquals(8, files);
        assert(found);
    }

    /**
     * Test of fromZIP method, of class CorpusTools.
     */
    @Test
    public void testFromZIP() {
        System.out.println("fromZIP");
        File folder = new File("src/test/resources/corpus.zip");
        Corpus result = CorpusTools.fromZIP(folder);
        int files = 0;
        boolean found = true;
        for(Document d : result.getDocuments()) {
            files++;
            found = found ||
                    d.name.equals("2002-GWBush.txt");
        }
        assertEquals(8, files);
        assert(found);
    }

    /**
     * Test of fromTarball method, of class CorpusTools.
     */
    @Test
    public void testFromTarball() {
        System.out.println("fromTarball");
        File folder = new File("src/test/resources/corpus.tar.gz");
        Corpus result = CorpusTools.fromTarball(folder);
        int files = 0;
        boolean found = true;
        for(Document d : result.getDocuments()) {
            files++;
            found = found ||
                    d.name.equals("2002-GWBush.txt");
        }
        assertEquals(8, files);
        assert(found);
    }
    
}
