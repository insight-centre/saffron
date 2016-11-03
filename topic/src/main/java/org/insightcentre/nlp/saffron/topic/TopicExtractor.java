package org.insightcentre.nlp.saffron.topic;

import java.util.List;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public interface TopicExtractor {

    public List<ExtractedTopic> extractTopics(String text, List<String> domainModel);

}
