package org.insightcentre.nlp.saffron;

/**
 * A listener for events in Saffron processes
 * @author John McCrae
 */
public interface SaffronListener {

    /**
     * Record a message
     * @param message The message
     */
    public void log(String message);
    
    /**
     * Indicate progress in a task
     */
    public void tick();
    
    /**
     * Indicate the completion of a task
     */
    public void endTick();
    
    /**
     * Indicate that a process failed
     * @param message The failure message
     * @param cause A (maybe null) cause
     */
    public void fail(String message, Throwable cause);
    
}
