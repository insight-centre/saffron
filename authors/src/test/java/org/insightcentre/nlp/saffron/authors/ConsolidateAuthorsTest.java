
package org.insightcentre.nlp.saffron.authors;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.insightcentre.nlp.saffron.data.Author;
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
public class ConsolidateAuthorsTest {

    public ConsolidateAuthorsTest() {
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
     * Test of consolidate method, of class ConsolidateAuthors.
     */
    @Test
    public void testConsolidate() {
        System.out.println("consolidate");
        List<Author> authors = Arrays.asList(new Author("John P. McCrae"),
                                             new Author("John McCrae"),
                                             new Author("Paul Buitelaar"));
        Map<Author, Set<Author>> result = ConsolidateAuthors.consolidate(authors);
        assertEquals(2, result.size());
    }

    /**
     * Test of isSimilar method, of class ConsolidateAuthors.
     */
    @Test
    public void testIsSimilar() {
        System.out.println("isSimilar");
        Author author = new Author("John McCrae");
        Author author2 = new Author("John P. McCrae");
        boolean expResult = true;
        boolean result = ConsolidateAuthors.isSimilar(author, author2);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testConsolidate2() {
                List<Author> authors = Arrays.asList(new Author("Eamon B. O'Dea"),
                                             new Author("Xiang Li"),
                                             new Author("Alessia Di Donfrancesco"),
                                             new Author("Joseph T. Wu"),
                                             new Author("Le Cai"));
        Map<Author, Set<Author>> result = ConsolidateAuthors.consolidate(authors);
        assertEquals(5, result.size());
                
    }
    
    @Test
    public void testConsolidate3() {
        List<Author> authors = Arrays.asList(new Author("Buhimschi, Irina A"), new Author("Irina A Buhimschi"));
        Map<Author, Set<Author>> result = ConsolidateAuthors.consolidate(authors);
        assertEquals(1, result.size());
        
    }
}