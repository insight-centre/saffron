package org.insightcentre.nlp.saffron.documentindex;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.documentindex.CorpusTools.FolderIterator;
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

    @Test
    public void testFolderIterator() throws IOException {
        File f = Files.createTempDir();
        f.deleteOnExit();
        File f2 = new File(f, "foo");
        f2.createNewFile();
        f2.deleteOnExit();
        File f3 = new File(f, "bar");
        f3.mkdir();
        f3.deleteOnExit();
        File f4 = new File(f3, "baz");
        f4.createNewFile();
        f4.deleteOnExit();
        
        FolderIterator fi = new FolderIterator(f.listFiles());
        //assert(fi.hasNext());
        //assertEquals("foo", fi.next().getName());
        //assert(fi.hasNext());
        //assertEquals("baz", fi.next().getName());
        //assert(!fi.hasNext());
        
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
