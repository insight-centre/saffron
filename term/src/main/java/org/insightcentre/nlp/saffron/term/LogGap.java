package org.insightcentre.nlp.saffron.term;

import static java.lang.Math.log;
import java.util.Arrays;

/**
 * Produce nicer more smoothly distributed data
 * @author John McCrae
 */
public class LogGap {
    private final double[] values, diffs;
    private final double sumdiff;

    private LogGap(double[] values, double[] diffs, double sumdiff) {
        this.values = values;
        this.diffs = diffs;
        this.sumdiff = sumdiff;
    }

    public static LogGap makeModel(double[] values) {
        double[] diffs = new double[values.length];
        Arrays.sort(values);
        double sumdiff = 0;
        for(int i = 0; i < values.length - 1; i++) {
            diffs[i+1] = log(values[i+1] - values[i] + 1);
            sumdiff += diffs[i+1];
        }
        for(int i = 1; i < values.length; i++) {
            diffs[i] = diffs[i-1] + diffs[i] / sumdiff;
        }
        return new LogGap(values, diffs, sumdiff);
    }


    public double normalize(double d) {
        int i = Arrays.binarySearch(values, d);
        if(i >= 0) {
            return diffs[i];
        }
        i = -i - 1;
        if(i == 0) {
            return -Math.log(values[0] - d + 1) / sumdiff;
        } else if(i >= values.length) {
            return 1 + Math.log(d - values[values.length - 1] + 1) / sumdiff;
        } else {
            double a = log(d - values[i-1] + 1);
            double b = log(values[i] - d + 1);
            double c = log(values[i] - values[i-1] + 1);
            return diffs[i-1] + c * a / (a + b) / sumdiff;
        }
    }
}
