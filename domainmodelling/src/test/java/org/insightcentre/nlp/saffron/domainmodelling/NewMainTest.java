package org.insightcentre.nlp.saffron.domainmodelling;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.insightcenter.nlp.saffron.documentindex.DocumentSearcher;
import org.insightcentre.nlp.saffron.domainmodelling.posextraction.ExtractionResultsWrapper;
import org.insightcentre.nlp.saffron.domainmodelling.termextraction.KPInfoProcessor;
import org.insightcentre.nlp.saffron.domainmodelling.termextraction.Keyphrase;
import org.insightcentre.nlp.saffron.domainmodelling.termextraction.KeyphraseExtractor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author jmccrae
 */
public class NewMainTest {
    Configuration config = new Configuration();
    
    public NewMainTest() {
        config.corpusPath = new File("src/test/resources/corpus_small");
        config.nlp = new Configuration.NLPConfiguration();
        config.keyphrase = new Configuration.KeyPhraseConfiguration();
        config.kpSim = new Configuration.KeyphraseSimConfiguration();
        new File("output").mkdirs();
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
     * Test of run method, of class NewMain.
     */
    @Test
    public void testRun() throws Exception {
        System.out.println("run");
        NewMain instance = new NewMain(config);
        instance.run();
    }

    /**
     * Test of processNLP method, of class NewMain.
     */
    @Test
    public void testProcessNLP() throws Exception {
        System.out.println("processNLP");
        KeyphraseExtractor ke = mock(KeyphraseExtractor.class);
        when(ke.extractPOS(config.loadCorpus(), config.nlp.lengthThreshold)).
            thenReturn(new ExtractionResultsWrapper(Collections.EMPTY_MAP, 
                Collections.EMPTY_MAP, Collections.EMPTY_MAP, 
                Collections.EMPTY_MAP, 0, 0));
        DocumentSearcher searcher = mock(DocumentSearcher.class);
        Set<String> stopWords = new TreeSet<>();
        NewMain instance = new NewMain(config);
        instance.processNLP(ke, searcher, stopWords);
    }

    /**
     * Test of assignKpDoc method, of class NewMain.
     */
    @Test
    public void testAssignKpDoc() throws Exception {
        System.out.println("assignKpDoc");
        Map<String, Keyphrase> kpMap = new HashMap<>();
        KPInfoProcessor kpip = mock(KPInfoProcessor.class);
        KeyphraseExtractor ke = mock(KeyphraseExtractor.class);
        Set<String> stopWords = new TreeSet<>();
        NewMain instance = new NewMain(config);
        instance.assignKpDoc(kpMap, kpip, ke, 4, stopWords);
    }

    /**
     * Test of assignKpSimDoc method, of class NewMain.
     */
    @Test
    public void testAssignKpSimDoc() throws Exception {
        System.out.println("assignKpSimDoc");
        Map<String, Keyphrase> kpMap = new HashMap<>();
        KPInfoProcessor kpip = mock(KPInfoProcessor.class);
        DocumentSearcher searcher = mock(DocumentSearcher.class);
        KeyphraseExtractor ke = mock(KeyphraseExtractor.class);
        Set<String> stopWords = new TreeSet<>();
        NewMain instance = new NewMain(config);
        instance.assignKpSimDoc(kpMap, kpip, searcher, ke, stopWords);
    }

    /**
     * Test of printKPRanks method, of class NewMain.
     */
    @Test
    public void testPrintKPRanks() throws Exception {
        System.out.println("printKPRanks");
        Map<String, Keyphrase> kpMap = new HashMap<>();
        KPInfoProcessor kpip = mock(KPInfoProcessor.class);
        Set<String> stopWords = new TreeSet<>();
        NewMain instance = new NewMain(config);
        instance.printKPRanks(kpMap, kpip, 2, stopWords);
    }

    /**
     * Test of rankDomainModel method, of class NewMain.
     */
    @Test
    public void testRankDomainModel() throws Exception {
        System.out.println("rankDomainModel");
        DocumentSearcher searcher = mock(DocumentSearcher.class);
        NewMain instance = new NewMain(config);
        instance.rankDomainModel(new HashMap<String, Double>(), searcher);
    }

    /**
     * Test of main method, of class NewMain.
     */
    @Test
    public void testMain() throws Exception {
        System.out.println("main");
        Configuration config = new ObjectMapper().
            readValue("{\"corpusPath\":\"src/test/resources/corpus_small\",\"nlp\":{},\"keyphrase\":{},\"kpSim\":{}}", Configuration.class);
        NewMain instance = new NewMain(config);
        instance.run();
    }
    
}
