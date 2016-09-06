package org.insightcentre.nlp.saffron.topicextraction.topicextractor;


import java.util.List;
import org.insightcentre.nlp.saffron.topiccollector.ExtractedTopic;

public class TopicBearer {
    private List<ExtractedTopic> extractedTopics;
    private Integer tokensNumber = 0;

    public void setTokensNumber(Integer tokensNumber) {
        this.tokensNumber = tokensNumber;
    }

    public Integer getTokensNumber() {
        return tokensNumber;
    }

    public void setExtractedTopics(List<ExtractedTopic> extractedTopics) {
        this.extractedTopics = extractedTopics;
    }

    public List<ExtractedTopic> getExtractedTopics() {
        return extractedTopics;
    }
}
