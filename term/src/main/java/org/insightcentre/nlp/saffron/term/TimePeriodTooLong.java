package org.insightcentre.nlp.saffron.term;

/**
 * Indicates that the temporal period is too long
 * 
 * @author John McCrae
 */
public class TimePeriodTooLong extends Exception {

    public TimePeriodTooLong() {
    }

    public TimePeriodTooLong(String message) {
        super(message);
    }

    public TimePeriodTooLong(String message, Throwable cause) {
        super(message, cause);
    }

    public TimePeriodTooLong(Throwable cause) {
        super(cause);
    }

}
