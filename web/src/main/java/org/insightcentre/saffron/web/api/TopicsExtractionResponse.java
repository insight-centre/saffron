package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class TopicsExtractionResponse {
    List<TopicExtractionResponse> topicsList = new ArrayList<>();

    public List<TopicExtractionResponse> getTopics() {
        return this.topicsList;
    }

    public void setTopics(List<TopicExtractionResponse> topicsList) {
        this.topicsList = topicsList;
    }
}
