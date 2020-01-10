
package org.insightcentre.nlp.saffron.term;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author John McCrae
 */
public class TemporalFrequencyStats {
    private final Duration interval;
    private LocalDateTime start = null;
    
    public List<FrequencyStats> freqs = new ArrayList<>();

    public TemporalFrequencyStats(Duration interval) {
        this.interval = interval;
    }
    
    private LocalDateTime convertToLocalDateViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime();
    }
    
    private long divCeil(long x, long y) {
        if(x % y == 0) {
            return x / y;
        } else {
            return x / y + 1;
        }
    }
    
    public void add(FrequencyStats stats, Date d) {
        LocalDateTime time = convertToLocalDateViaInstant(d);
        if(start == null) {
            start = time;
            freqs.add(stats);
        } else if(time.isBefore(start)) {
            Duration delta = Duration.between(time, start);
            long window = divCeil(delta.getSeconds(), interval.getSeconds());
            for(int i = 0; i < window; i++) {
                freqs.add(0, new FrequencyStats());
            }
            if(window > 0)
                start = start.minus(window * interval.getSeconds(), ChronoUnit.SECONDS);
           freqs.get(0).add(stats);
        } else {
            Duration delta = Duration.between(start, time);
            long n = delta.getSeconds() / interval.getSeconds();
            while(n >= freqs.size()) {
                freqs.add(new FrequencyStats());
            }
            freqs.get((int)n).add(stats);
        }
    }

}
