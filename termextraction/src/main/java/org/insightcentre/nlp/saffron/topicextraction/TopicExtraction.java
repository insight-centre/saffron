package org.insightcentre.nlp.saffron.topicextraction;

import org.insightcentre.nlp.saffron.topicextraction.data.DomainModel;
import org.insightcentre.nlp.saffron.topicextraction.data.Topic;
import org.insightcentre.nlp.saffron.topicextraction.data.TopicAdapter;
import org.insightcentre.nlp.saffron.topicextraction.filter.ExtractedTopicsFilter;
import org.insightcentre.nlp.saffron.topicextraction.topicextractor.TopicBearer;
import org.insightcentre.nlp.saffron.topicextraction.topicextractor.TopicExtractor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.insightcentre.nlp.saffron.topiccollector.DocumentConversionException;
import org.insightcentre.nlp.saffron.topiccollector.ExtractedTopic;
import org.insightcentre.nlp.saffron.topiccollector.TextExtractor;

/**
 * Ties everything together (extraction, filtering, indexing, data adaptors)
 * See project README.txt for diagram
 */
public class TopicExtraction {
    private final TopicExtractor topicExtractor;
    //private static Logger logger = Logger.getLogger(TopicExtraction.class);
    private final ExtractedTopicsFilter extractedTopicsFilter;
    
//    public TopicExtraction(final TopicExtractor extractor) {
//        topicExtractor = TopicExtractorGate.getInstance();
//        extractedTopicsFilter = new AllFilters();
//    }
//    
    public TopicExtraction(TopicExtractor topicExtractor, ExtractedTopicsFilter extractedTopicsFilter) {
		this.topicExtractor = topicExtractor;
		this.extractedTopicsFilter = extractedTopicsFilter;
	}
    
	private void filterTopics(TopicBearer tb, Set<String> stopWords) {
        //Filter topics
        List<ExtractedTopic> extractedTopicList = tb.getExtractedTopics();
        if (extractedTopicList != null) {
            List<ExtractedTopic> filtered = extractedTopicsFilter.filter(extractedTopicList, stopWords);
            tb.setExtractedTopics(filtered);
        }
    }

    public Set<Topic> extractTopics(String text, DomainModel domainModel, Set<String> stopWords) 
    		throws IOException {
        TopicBearer tb = topicExtractor.extractTopics(text, domainModel);
        filterTopics(tb, stopWords);
        
        TopicAdapter topicAdapter = new TopicAdapter(tb.getExtractedTopics(), text);
        Set<Topic> topics = topicAdapter.convertExtractedTopics();
        return topics; 
    }

    public Set<Topic> extractTopics(File f, DomainModel domainModel, Set<String> stopWords) 
    		throws IOException, DocumentConversionException {
    	return extractTopics(TextExtractor.extractText(f), domainModel, stopWords);
    }
    
    public Set<Topic> extractTopics(InputStream is, String mimeType, DomainModel domainModel, Set<String> stopWords) 
    		throws IOException, DocumentConversionException {
    	return extractTopics(TextExtractor.extractText(is, mimeType), domainModel, stopWords);
    }
}


