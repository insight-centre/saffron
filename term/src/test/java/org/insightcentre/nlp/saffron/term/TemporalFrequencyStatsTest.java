package org.insightcentre.nlp.saffron.term;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.GregorianCalendar;
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
public class TemporalFrequencyStatsTest {

    public TemporalFrequencyStatsTest() {
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

    private LocalDateTime mkDate(int year, int month, int day) {
        return LocalDateTime.of(year, month, day, 0, 0);
    }
    
    /**
     * Test of add method, of class TemporalFrequencyStats.
     */
    @Test
    public void testAdd() {
        System.out.println("add");
        Duration d = Duration.ofDays(7);
                
        TemporalFrequencyStats instance = new TemporalFrequencyStats(d);
        {
        FrequencyStats s1 = new FrequencyStats();
        s1.termFrequency.put("cat", 2);
        s1.termFrequency.put("dog", 1);
        LocalDateTime d1 = mkDate(2020, 1, 10);
        instance.add(s1, d1);
        }
        {        
        FrequencyStats s2 = new FrequencyStats();
        s2.termFrequency.put("cat", 3);
        s2.termFrequency.put("dog", 4);
        LocalDateTime d2 = mkDate(2020, 1, 30);
        instance.add(s2, d2);
        }
        {        
        FrequencyStats s2 = new FrequencyStats();
        s2.termFrequency.put("cat", 1);
        s2.termFrequency.put("dog", 2);
        LocalDateTime d2 = mkDate(2020, 1, 1);
        instance.add(s2, d2);
        }
        {        
        FrequencyStats s2 = new FrequencyStats();
        s2.termFrequency.put("cat", 1);
        s2.termFrequency.put("dog", 2);
        LocalDateTime d2 = mkDate(2020, 1, 11);
        instance.add(s2, d2);
        }
        assertEquals(instance.freqs.size(), 5);
        assertEquals(instance.freqs.get(2).termFrequency.getInt("cat"), 3);
        
        
    }

    @Test
    public void testPredict() {
        Duration d = Duration.ofDays(7);
                
        TemporalFrequencyStats instance = new TemporalFrequencyStats(d);
        {
        FrequencyStats s1 = new FrequencyStats();
        s1.termFrequency.put("cat", 1);
        s1.tokens = 10;
        LocalDateTime d1 = mkDate(2020, 1, 1);
        instance.add(s1, d1);
        }
        
        {
        FrequencyStats s1 = new FrequencyStats();
        s1.termFrequency.put("cat", 2);
        s1.tokens = 10;
        LocalDateTime d1 = mkDate(2020, 1, 8);
        instance.add(s1, d1);
        }
        
        {
        FrequencyStats s1 = new FrequencyStats();
        s1.termFrequency.put("cat", 3);
        s1.tokens = 10;
        LocalDateTime d1 = mkDate(2020, 1, 15);
        instance.add(s1, d1);
        }
        
        {
        FrequencyStats s1 = new FrequencyStats();
        s1.termFrequency.put("cat", 4);
        s1.tokens = 10;
        LocalDateTime d1 = mkDate(2020, 1, 22);
        instance.add(s1, d1);
        }
        double prediction = instance.predict("cat", 2, 2);
        assertEquals(0.6, prediction, 0.01);
    }
}