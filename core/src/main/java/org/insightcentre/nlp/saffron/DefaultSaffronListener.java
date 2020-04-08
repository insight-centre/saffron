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
    public void setStageComplete(String statusMessage, String taxonomyId) {
        System.err.println(statusMessage);
    }

    @Override
    public void warning(String message, Throwable cause) {

    }

    @Override
    public void fail(String message, Throwable cause) {
        System.err.println(message);
        cause.printStackTrace();
    }

    @Override
    public void setStageStart(String statusMessage, String taxonomyId) {
        System.err.println(statusMessage);
    }

}
