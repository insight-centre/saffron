package org.insightcentre.nlp.saffron.authors.sim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.insightcentre.nlp.saffron.SaffronListener;
import org.insightcentre.nlp.saffron.config.AuthorSimilarityConfiguration;
import org.insightcentre.nlp.saffron.data.connections.AuthorAuthor;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
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
public class AuthorSimilarityTest {

    public AuthorSimilarityTest() {
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
    private AuthorTerm makeAuthorTerm(String authorName, String termName, double score) {
        AuthorTerm at = new AuthorTerm();
        at.setAuthorId(authorName);
        at.setTermId(termName);
        at.setScore(score);
        return at;
    }

    /**
     * Test of authorSimilarity method, of class AuthorSimilarity.
     */
    @Test
    public void testAuthorSimilarity_Collection_String() {
        System.out.println("authorSimilarity");
        List<AuthorTerm> ats = new ArrayList<>();
        ats.add(makeAuthorTerm("A. Author", "Term 1", 0.9));
        ats.add(makeAuthorTerm("B. Author", "Term 1", 0.9));
        ats.add(makeAuthorTerm("C. Author", "Term 2", 0.9));
        ats.add(makeAuthorTerm("A. Author", "Term 2", 0.9));
        ats.add(makeAuthorTerm("D. Author", "Term 3", 0.9));
        ats.add(makeAuthorTerm("E. Author", "Term 2", 0.9));
        String saffronDatasetName = "saffron";
        AuthorSimilarity instance = new AuthorSimilarity(new AuthorSimilarityConfiguration());
        List<AuthorAuthor> result = instance.authorSimilarity(ats, saffronDatasetName);
        assertEquals(8, result.size());
        
    }


}