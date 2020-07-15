package org.insightcentre.nlp.saffron.term;

/**
 * Indicates that the temporal period is too long
 * 
 * @author John McCrae
 */
public class IntervalTooLong extends Exception {

    public IntervalTooLong() {
    }

    public IntervalTooLong(String message) {
        super(message);
    }

    public IntervalTooLong(String message, Throwable cause) {
        super(message, cause);
    }

    public IntervalTooLong(Throwable cause) {
        super(cause);
    }

}
