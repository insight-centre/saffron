package org.insightcentre.nlp.saffron.term;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
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
    public void testPredict() throws Exception {
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
    
    @Test(expected = TimePeriodTooLong.class)
    public void testTimePeriodTooLong() throws TimePeriodTooLong {
        TemporalFrequencyStats tfs = new TemporalFrequencyStats(Duration.ofDays(1000000));
        Random rand = new Random();
        for(int i = 0; i < 50; i++) {
            FrequencyStats fs = new FrequencyStats();
            int r0 = rand.nextInt(1000);
            fs.termFrequency.put("a", r0);
            int r1 = rand.nextInt(1000);
            fs.termFrequency.put("b", r1);
            int r2 = rand.nextInt(1000);
            fs.termFrequency.put("c", r2);
            int r3 = rand.nextInt(1000);
            fs.termFrequency.put("d", r3);
            fs.tokens = r0 + r1 + r2 + r3;
            tfs.add(fs, LocalDateTime.now());
        }
        tfs.predict("c", 5, 2);
        
    }
}