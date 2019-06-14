package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class TopicsCorrespondenceResponse {
    List<TopicCorrespondenceResponse> topicsList = new ArrayList<>();

    public List<TopicCorrespondenceResponse> getTopics() {
        return this.topicsList;
    }

    public void setTopics(List<TopicCorrespondenceResponse> topicsList) {
        this.topicsList = topicsList;
    }
}
