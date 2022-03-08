package org.insightcentre.nlp.saffron;

import org.insightcentre.nlp.saffron.config.Configuration;

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
     * Indicate the completion of a stage
     * @param statusMessage The message associated with the stage
     * @param taxonomyId A taxonomy ID
     */
    void setStageComplete(String statusMessage, String taxonomyId);

    /**
     * Indicate that a process has a warning
     * @param message The warning message
     * @param cause A (maybe null) cause
     */
    void warning(String message, Throwable cause);

    /**
     * Indicate that a process failed
     * @param message The failure message
     * @param cause A (maybe null) cause
     */
    public void fail(String message, Throwable cause);

    /**
     * Indicates the start of a stage
     * @param statusMessage The status method
     * @param taxonomyId The taxonomy stage
     */
    void setStageStart(String statusMessage, String taxonomyId);

    /**
     * Indicates that a pipeline has started
     * @param taxonomyId The taxonomy ID
     * @param configuration The configuration of this run
     */
    void start(String taxonomyId, Configuration configuration);

    /**
     * Indicates the pipeline has completed
     * @param taxonomyId The taxonomy ID
     */
    default void end(String taxonomyId) {}
}
