package org.insightcentre.nlp.saffron.topic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.insightcentre.nlp.saffron.topic.Topic.MorphologicalVariation;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class TopicExtraction {
    private final TopicExtractor topicExtractor;
    private final int maxTopics;
    private final int minTokens, maxTokens;

    public TopicExtraction(TopicExtractor topicExtractor, int maxTopics, int minTokens, int maxTokens) {
        this.topicExtractor = topicExtractor;
        this.maxTopics = maxTopics;
        this.minTokens = minTokens;
        this.maxTokens = maxTokens;
    }

    

    public Set<Topic> extractTopics(String text, List<String> domainModel, Set<String> stopWords) 
    		throws IOException {
        List<ExtractedTopic> tb = topicExtractor.extractTopics(text, domainModel);
        tb = filterTopics(tb, stopWords);
        
        Set<Topic> topics = convertExtractedTopics(tb, text);

        return topics; 
    }
    
	private List<ExtractedTopic> filterTopics(List<ExtractedTopic> extractedTopicList, Set<String> stopWords) {
        //Filter topics
        if (extractedTopicList != null) {
            return filterExtractedTopics(extractedTopicList, stopWords);
        } else {
            throw new RuntimeException();
        }
    }

	/**
	 * Converts ExtractedTopic objects to Topic/Morphological Variation objects 
	 * which are more user-friendly when used as a service.
	 */
    public Set<Topic> convertExtractedTopics(List<ExtractedTopic> extractedTopics, String text) {
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
        tokenCount = computeTokensNo(extractedTopic.getPattern());

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

    private List<ExtractedTopic> filterExtractedTopics(List<ExtractedTopic> extractedTopics, Set<String> stopWords) {
        List<ExtractedTopic> filtered = new ArrayList<>();

        int size = maxTopics;
        Collections.sort(filtered);
        for(int i=0; i < Math.min(extractedTopics.size(), size); i++) {
            ExtractedTopic extractedTopic = extractedTopics.get(i);
            if (isProperTopic(extractedTopic.getTopicString(), stopWords)) {
                filtered.add(extractedTopic);
                int tokens = computeTokensNo(extractedTopic.getPattern());
                if (tokens >= minTokens && tokens <= maxTokens) {
                    filtered.add(extractedTopic);
                }
            }
        }

        return filtered;
    }
 
    public static int computeTokensNo(String s) {
        return s.split(" ").length;
    }

    public static boolean isProperTopic(String rootSequence, Set<String> stopWords) {
        String s = rootSequence;

        if (s.length() < 2) {
            return false;
        }
        // all words need to have at least 2 characters
        String[] words = s.split(" ");
        for(String word : words) {
            if(word.length() < 2) {
                return false;
            }
        }

        if (s.contains("- ") || s.contains(" -")) {
            return false;
        }
        final char[] chars = s.toCharArray();

        // first character must be alphabetic
        if (!Character.isAlphabetic(chars[0])) {
            return false;
        }
        if (!Character.isLetterOrDigit(chars[chars.length - 1])) {
            return false;
        }

        // first or last word not in stopwords
    	String firstWord = words[0].toLowerCase();
    	String lastWord = words[words.length-1].toLowerCase();
    	if (stopWords.contains(firstWord) || stopWords.contains(lastWord)) {
            return false;
        }

        // is alpha numeric
        for (int x = 0; x < chars.length; x++) {
            final char c = chars[x];
            if (!Character.isLetterOrDigit(c) && c != '-' && c != ' ') {
                return false;
            }
        }
        return true;
    }
}
