
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

    /**
     * Test of _max method, of class ConsolidateAuthors.
     */
    @Test
    public void test_max() {
        System.out.println("_max");
        List<Integer> labels = Arrays.asList(1,2,4);
        int expResult = 4;
        int result = ConsolidateAuthors._max(labels);
        assertEquals(expResult, result);
    }

    /**
     * Test of join method, of class ConsolidateAuthors.
     */
    @Test
    public void testJoin() {
        System.out.println("join");
        List<ConsolidateAuthors.NameElem> firstnames = Arrays.asList(
            new ConsolidateAuthors.NameElem(0.0, "John"),
            new ConsolidateAuthors.NameElem(0.0, "McCrae"));
        String expResult = "John McCrae";
        String result = ConsolidateAuthors.join(firstnames);
        assertEquals(expResult, result);
    }

    /**
     * Test of _choose method, of class ConsolidateAuthors.
     */
    @Test
    public void test_choose() {
        System.out.println("_choose");
        Collection<String> names = Arrays.asList("John McCrae", "J McCrae");
        String expResult = "John McCrae";
        String result = ConsolidateAuthors._choose(names);
        assertEquals(expResult, result);
    }

    /**
     * Test of count method, of class ConsolidateAuthors.
     */
    @Test
    public void testCount() {
        System.out.println("count");
        String w = "foo bar boo";
        char c = 'o';
        int expResult = 4;
        int result = ConsolidateAuthors.count(w, c);
        assertEquals(expResult, result);
    }

    /**
     * Test of _min method, of class ConsolidateAuthors.
     */
    @Test
    public void test_min() {
        System.out.println("_min");
        Set<Integer> s = new HashSet<>(Arrays.asList(1,2,4));
        int expResult = 1;
        int result = ConsolidateAuthors._min(s);
        assertEquals(expResult, result);
    }

    /**
     * Test of sum_word_occ method, of class ConsolidateAuthors.
     */
    @Test
    public void testSum_word_occ() {
        System.out.println("sum_word_occ");
        Object2DoubleMap word_occ = new Object2DoubleOpenHashMap();
        word_occ.put("cat", 1);
        word_occ.put("dog", 2);
        List name = Arrays.asList("cat", "dog", "cat");
        double expResult = 4.0;
        double result = ConsolidateAuthors.sum_word_occ(word_occ, name);
        assertEquals(expResult, result, 0.00001);
    }

    /**
     * Test of browse_links method, of class ConsolidateAuthors.
     */
    @Test
    public void testBrowse_links() {
        System.out.println("browse_links");
        ConsolidateAuthors.Researcher self = null;
        Set<ConsolidateAuthors.Researcher> researchers = null;
        Map<ConsolidateAuthors.Researcher, Map<ConsolidateAuthors.Researcher, ConsolidateAuthors.Link>> alllinks = null;
        ConsolidateAuthors.browse_links(self, researchers, alllinks);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of intersection method, of class ConsolidateAuthors.
     */
    @Test
    public void testIntersection() {
        System.out.println("intersection");
        boolean expResult = false;
        Set<String> s1 = new HashSet<>(Arrays.asList("a","b","c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("1","2","3"));
        Set<String> s3 = new HashSet<>(Arrays.asList("a","2","q"));
        assert(!ConsolidateAuthors.intersection(s1, s2));
        assert(ConsolidateAuthors.intersection(s1, s3));
    }

    /**
     * Test of _cluster method, of class ConsolidateAuthors.
     */
    @Test
    public void test_cluster() {
        System.out.println("_cluster");
        List<ConsolidateAuthors.Researcher> cluster = null;
        ConsolidateAuthors instance = new ConsolidateAuthors();
        instance._cluster(cluster);
        fail("The test case is a prototype.");
    }

    /**
     * Test of _match_words method, of class ConsolidateAuthors.
     */
    @Test
    public void test_match_words() {
        System.out.println("_match_words");
        Collection<ConsolidateAuthors.Researcher> researcher = null;
        Object expResult = null;
        Object result = ConsolidateAuthors._match_words(researcher);
        assertEquals(expResult, result);
        fail("The test case is a prototype.");
    }

}