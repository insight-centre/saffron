package org.insightcentre.nlp.saffron.topicextraction.data;


import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.insightcentre.nlp.saffron.topicextraction.util.WordUtils;
import org.insightcentre.nlp.saffron.topiccollector.ExtractedTopic;


/**
 * Convert GATE ExtractedTopic to Topic/MorphologicalVariation representation of text
 * extraction from a single document.  
 */
public class TopicAdapter {
	
	private List<ExtractedTopic> extractedTopics;
	
	public TopicAdapter(List<ExtractedTopic> extractedTopics, String text) 
			throws IOException {
		this.extractedTopics = extractedTopics;
	}
	
	/**
	 * Converts ExtractedTopic objects to Topic/Morphological Variation objects 
	 * which are more user-friendly when used as a service.
	 */
    public Set<Topic> convertExtractedTopics() {
    	HashMap<String/*rootSequence*/, Topic> topicMap = new HashMap<>();
    	
        Set<Topic> topics = new HashSet<Topic>();
        for (ExtractedTopic extractedTopic : extractedTopics) {
        	String rootSequence = extractedTopic.getRootSequence();
        	
        	String context = extractedTopic.getContext();
        	String contextP = extractedTopic.getContextPattern();
        	//logger.debug("\nExtracted: "+rootSequence+"\nContext: "
        	//				+context+"\nPattern: "+contextP);
        	
        	Topic topic = null;
        	if (topicMap.containsKey(rootSequence)) {
        		topic = topicMap.get(rootSequence);
        		updateTopic(topic, extractedTopic);
        	} else {
                topic = createTopic(extractedTopic);
                topics.add(topic);
        	}
        	topicMap.put(rootSequence, topic);
        }
        return topics;
    }
    
    /**
     * Add morphological variation or update term occurrences of morphological
     * variation for topic.  
     */
    private void updateTopic(Topic topic, ExtractedTopic extractedTopic) {
    	String topicString = extractedTopic.getTopicString();
    	
		//If the variation already exists, increment it. Otherwise create it.
		boolean variationExists = false;
		for (MorphologicalVariation mv : topic.getMorphologicalVariations()) {
			if (mv.getTermString().equals(topicString)) {
				mv.setExtractedTermOccurrences(mv.getExtractedTermOccurrences()+1);
				variationExists = true;
				break;
			}
		}
		if (!variationExists) {
	        MorphologicalVariation mv = createMorphologicalVariation(extractedTopic);
	        topic.addMorphologicalVariation(mv);
		}
    }
    
    private Topic createTopic(ExtractedTopic extractedTopic) {
        String rootSequence = extractedTopic.getRootSequence();
        Integer tokenCount;
        tokenCount = WordUtils.computeTokensNo(extractedTopic.getPattern());

        Topic topic = new Topic();
        topic.setRootSequence(rootSequence);
        topic.setNumberOfTokens(tokenCount);

        MorphologicalVariation mv = createMorphologicalVariation(extractedTopic);
        topic.addMorphologicalVariation(mv);

        return topic;
    }
    
    private MorphologicalVariation createMorphologicalVariation(ExtractedTopic extractedTopic) {
        MorphologicalVariation mv = new MorphologicalVariation();
        mv.setPattern(extractedTopic.getPattern());
        mv.setExpandedAcronym(extractedTopic.getExpandedAcronym());
        mv.setExtractedTermOccurrences(1);
        mv.setAcronym(extractedTopic.getAcronym());
        mv.setTermString(extractedTopic.getTopicString());
    	return mv;
    }
}
