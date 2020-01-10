package org.insightcentre.nlp.saffron.term;

import java.time.Duration;
import java.util.Calendar;
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

    private Date mkDate(int year, int month, int day) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(year, month, day);
        return gc.getTime();
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
        Date d1 = mkDate(2020, 1, 10);
        instance.add(s1, d1);
        }
        {        
        FrequencyStats s2 = new FrequencyStats();
        s2.termFrequency.put("cat", 3);
        s2.termFrequency.put("dog", 4);
        Date d2 = mkDate(2020, 1, 30);
        instance.add(s2, d2);
        }
        {        
        FrequencyStats s2 = new FrequencyStats();
        s2.termFrequency.put("cat", 1);
        s2.termFrequency.put("dog", 2);
        Date d2 = mkDate(2020, 1, 1);
        instance.add(s2, d2);
        }
        {        
        FrequencyStats s2 = new FrequencyStats();
        s2.termFrequency.put("cat", 1);
        s2.termFrequency.put("dog", 2);
        Date d2 = mkDate(2020, 1, 11);
        instance.add(s2, d2);
        }
        assertEquals(instance.freqs.size(), 5);
        assertEquals(instance.freqs.get(2).termFrequency.getInt("cat"), 3);
        
        
    }

}