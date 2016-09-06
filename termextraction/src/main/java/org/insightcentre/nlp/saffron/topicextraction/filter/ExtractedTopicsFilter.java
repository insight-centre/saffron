package org.insightcentre.nlp.saffron.topicextraction.filter;


import java.util.List;
import java.util.Set;
import org.insightcentre.nlp.saffron.topiccollector.ExtractedTopic;

public interface ExtractedTopicsFilter {
    public List<ExtractedTopic> filter(List<ExtractedTopic> extracted, Set<String> stopWords);
}