
package org.insightcentre.nlp.saffron.term;

import static java.lang.Math.max;
import static java.lang.Math.min;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

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
    
    private long divCeil(long x, long y) {
        if(x % y == 0) {
            return x / y;
        } else {
            return x / y + 1;
        }
    }
    
    public void add(FrequencyStats stats, LocalDateTime time) {
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
    
    /**
     * Predict the corpus probability for a future term frequency
     * @param word The word to predict for
     * @param intervalsAfterEnd The number of intervals beyond the most recent one observed to predict for
     * @param degree The degree of the approximation (2 is a good value)
     * @return The prediction (a double between 0 and 1)
     */
    public double predict(String word, int intervalsAfterEnd, int degree) {
        if(freqs.isEmpty()) 
            throw new RuntimeException("Cannot predict future term frequency (likely no dates provided in corpus)");
        if(degree <= 0)
            throw new IllegalArgumentException("Degree must be greater than one");
        RealMatrix x = new Array2DRowRealMatrix(freqs.size(), degree + 1);
        RealVector y = new ArrayRealVector(freqs.size());
        for(int i = 0; i < freqs.size(); i++) {
            for(int j = 0; j <= degree; j++) {
                x.setEntry(i, j, Math.pow(i - freqs.size() + 1, j));
            }
            y.setEntry(i, (double)freqs.get(i).termFrequency.getInt(word) / freqs.get(i).tokens);
        }
        RealMatrix xtx_inv = MatrixUtils.inverse(x.transpose().multiply(x));
        RealVector a = xtx_inv.operate(x.preMultiply(y));
        
        double prediction = 0.0;
        for(int i = 0; i < degree; i++) {
            prediction += a.getEntry(i) * Math.pow(intervalsAfterEnd, i);
        }
        return max(min(prediction, 1), 0);
    }

}
