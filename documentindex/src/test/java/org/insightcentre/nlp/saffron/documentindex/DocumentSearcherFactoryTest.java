package org.insightcentre.nlp.saffron.documentindex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Corpus;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.SaffronPath;
import org.insightcentre.nlp.saffron.data.index.DocumentSearcher;
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
public class DocumentSearcherFactoryTest {

    public DocumentSearcherFactoryTest() {
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

    public void rmfr(File f) throws IOException {
        Path rootPath = f.toPath();
        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(System.out::println)
                    .forEach(File::delete);
        }
    }

    /**
     * Test of index method, of class DocumentSearcherFactory.
     */
    @Test
    public void testIndex_3args() throws Exception {
        System.out.println("index");
        File f = File.createTempFile("lucene", ".d");
        f.delete();
        try {
            List<Document> docs = new ArrayList<>();
            List<Author> authors = new ArrayList<>();
            authors.add(new Author("jb","Joe Bloggs", new HashSet<String>() {{ add("Jospeh Bloggs"); }}));
            Document doc1 = new Document(null, "id1", null, "test", null, authors, new HashMap<>(), "this is a test document", LocalDateTime.now());
            docs.add(doc1);
            Corpus corpus = new IndexedCorpus(docs, new SaffronPath(f.getAbsolutePath()));
            File index = f;
            Boolean isInitialRun = true;
            DocumentSearcher result = DocumentSearcherFactory.index(corpus, index, isInitialRun);
            int i = 0;
            for(Document d : result.getDocuments()) {
                assertEquals(d, doc1);
                assertEquals(0, i++);
            }
        } finally {
            rmfr(f);
        }
    }

}
