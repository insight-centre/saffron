package org.insightcentre.nlp.saffron;

/**
 * The default listener, which prints to STDERR.
 * @author John McCrae
 */
public class DefaultSaffronListener implements SaffronListener {

    @Override
    public void log(String message) {
        System.err.println(message);
    }

    @Override
    public void tick() {
        System.err.printf(".");
    }

    @Override
    public void endTick() {
        System.err.println();
    }

    @Override
    public void fail(String message, Throwable cause) {
        System.err.println(message);
        cause.printStackTrace();
    }

}
