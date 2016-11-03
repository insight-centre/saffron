package org.insightcentre.nlp.saffron.topic;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public interface ExtractedTopic extends Comparable<ExtractedTopic> {

    public String getAcronym();
    public void setAcronym(String acronym);
    public String getContext();
    public void setContext(String context);
    public String getContextPattern();
    public void setContextPattern(String contextPattern);
    public String getExpandedAcronym();
    public void setExpandedAcronym(String expandedAcronym);
    public String getPattern();
    public void setPattern(String pattern);
    public String getRootSequence();
    public void setRootSequence(String rootSequence);
    public String getTopicString();
    public void setTopicString(String topicString);
    public long getOffset();
}
