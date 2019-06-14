package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class TopicsSimilarityResponse extends BaseResponse {

    List<TopicSimilarityResponse> topicsList = new ArrayList<>();

    public List<TopicSimilarityResponse> getTopics() {
        return this.topicsList;
    }

    public void setTopics(List<TopicSimilarityResponse> topicsList) {
        this.topicsList = topicsList;
    }

}
