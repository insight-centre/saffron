package org.insightcentre.nlp.saffron.topicextraction.filter;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.insightcentre.nlp.saffron.topicextraction.util.FilterUtils;
import org.insightcentre.nlp.saffron.topiccollector.ExtractedTopic;


public class ProperExtractedTopicFilter implements ExtractedTopicsFilter {
	
    @Override
    public List<ExtractedTopic> filter(List<ExtractedTopic> extracted, Set<String> stopWords) {
        List<ExtractedTopic> filtered = new ArrayList<ExtractedTopic>();

        for (ExtractedTopic extractedTopic : extracted) {
            if (FilterUtils.isProperTopic(extractedTopic.getTopicString(), stopWords)) {
                filtered.add(extractedTopic);
                //logger.debug("PROPER TOPIC: rootSequence=" + extractedTopic.getRootSequence()
                //		+ "; topicString="+extractedTopic.getTopicString());
            } else {
            	//logger.info("NOT A PROPER TOPIC: rootSequence=" + extractedTopic.getRootSequence()
            	//		+ "; topicString="+extractedTopic.getTopicString());
            }
            
        }

        return filtered;
    }
}
