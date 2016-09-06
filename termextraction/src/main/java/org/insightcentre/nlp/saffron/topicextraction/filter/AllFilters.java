package org.insightcentre.nlp.saffron.topicextraction.filter;

import org.insightcentre.nlp.saffron.topiccollector.ExtractedTopic;
import org.insightcentre.nlp.saffron.topicextraction.Config;

import java.util.List;
import java.util.Set;

public class AllFilters implements ExtractedTopicsFilter {
    //private static Logger log = Logger.getLogger(AllFilters.class);

    public List<ExtractedTopic> filter(List<ExtractedTopic> extracted, Set<String> stopWords) {
    	if (Config.maxTopicsPerDocument != null) {
    		extracted = new MaxExtractedTopicsFilter(Config.maxTopicsPerDocument).filter(extracted, stopWords);
    	}
        extracted = new ProperExtractedTopicFilter().filter(extracted, stopWords);
        extracted = new TokenCountRangeFilter(Config.minTokens, Config.maxTokens).filter(extracted, stopWords);
        
        return extracted;
    }
}
