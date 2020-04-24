package org.insightcentre.nlp.saffron.authors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

    @Test
    public void testConsolidate4() {
        List<Author> authors = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            authors.add(new Author(generateName()));
        }
        Map<Author, Set<Author>> result = ConsolidateAuthors.consolidate(authors);
        //assert (result.size() > 1500);
    }

    private static String[] firstName = {
        "Emma",
        "Olivia",
        "Ava",
        "Isabella",
        "Sophia",
        "Charlotte",
        "Mia",
        "Amelia",
        "Harper",
        "Evelyn",
        "Abigail",
        "Emily",
        "Elizabeth",
        "Mila",
        "Ella",
        "Avery",
        "Sofia",
        "Camila",
        "Aria",
        "Scarlett",
        "Victoria",
        "Madison",
        "Luna",
        "Grace",
        "Chloe",
        "Penelope",
        "Layla",
        "Riley",
        "Zoey",
        "Nora",
        "Lily",
        "Eleanor",
        "Hannah",
        "Lillian",
        "Addison",
        "Aubrey",
        "Ellie",
        "Stella",
        "Natalie",
        "Zoe",
        "Leah",
        "Hazel",
        "Violet",
        "Aurora",
        "Savannah",
        "Audrey",
        "Brooklyn",
        "Bella",
        "Claire",
        "Skylar",
        "Liam",
        "Noah",
        "William",
        "James",
        "Oliver",
        "Benjamin",
        "Elijah",
        "Lucas",
        "Mason",
        "Logan",
        "Alexander",
        "Ethan",
        "Jacob",
        "Michael",
        "Daniel",
        "Henry",
        "Jackson",
        "Sebastian",
        "Aiden",
        "Matthew",
        "Samuel",
        "David",
        "Joseph",
        "Carter",
        "Owen",
        "Wyatt",
        "John",
        "Jack",
        "Luke",
        "Jayden",
        "Dylan",
        "Grayson",
        "Levi",
        "Isaac",
        "Gabriel",
        "Julian",
        "Mateo",
        "Anthony",
        "Jaxon",
        "Lincoln",
        "Joshua",
        "Christopher",
        "Andrew",
        "Theodore",
        "Caleb",
        "Ryan",
        "Asher",
        "Nathan",
        "Thomas",
        "Leo"
    };

    private static String[] lastName = {
        "Smith",
        "Johnson",
        "Williams",
        "Jones",
        "Brown",
        "Davis",
        "Miller",
        "Wilson",
        "Moore",
        "Taylor",
        "Anderson",
        "Thomas",
        "Jackson",
        "White",
        "Harris",
        "Martin",
        "Thompson",
        "Garcia",
        "Martinez",
        "Robinson",
        "Clark",
        "Rodriguez",
        "Lee",
        "Walker",
        "Hall",
        "Allen",
        "Young",
        "Hernandez",
        "King",
        "Wright",
        "Lopez",
        "Hill",
        "Scott",
        "Green",
        "Adams",
        "Baker",
        "Gonzalez",
        "Nelson",
        "Carter",
        "Mitchell",
        "Perez",
        "Roberts",
        "Turner",
        "Phillips",
        "Campbell",
        "Parker",
        "Evans",
        "Edwards",
        "Collins",
        "Stewart",
        "Sanchez",
        "Morris",
        "Reed",
        "Cook",
        "Morgan",
        "Bell",
        "Murphy",
        "Bailey",
        "Rivera",
        "Cooper",
        "Richardson",
        "Cox",
        "Howard",
        "Ward",
        "Torres",
        "Peterson",
        "Gray",
        "Ramirez",
        "James",
        "Watson",
        "Brooks",
        "Kelly",
        "Sanders",
        "Price",
        "Bennett",
        "Wood",
        "Barnes",
        "Ross",
        "Henderson",
        "Coleman",
        "Jenkins",
        "Perry",
        "Powell",
        "Long",
        "Patterson",
        "Hughes",
        "Flores",
        "Washington",
        "Butler",
        "Simmons",
        "Foster",
        "Gonzales",
        "Bryant",
        "Alexander",
        "Russell",
        "Griffin",
        "Diaz",
        "Hayes"
    };

    private static String generateName() {
        Random r = new Random();
        StringBuilder name = new StringBuilder();
        name.append(firstName[r.nextInt(100)]).append(" ");
        if (r.nextDouble() <= 0.3) {
            name.append(firstName[r.nextInt(100)]).append(" ");
        } else if (r.nextDouble() <= 0.4) {
            name.append(firstName[r.nextInt(100)].charAt(0)).append(".").append(" ");
        }
        name.append(lastName[r.nextInt(98)]);
        return name.toString();
    }
}
