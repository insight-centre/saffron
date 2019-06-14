package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;



@XmlRootElement()
public class TopicsResponse extends BaseResponse {


    List<TopicResponse> topicsList = new ArrayList<>();

    public List<TopicResponse> getTopics() {
        return this.topicsList;
    }

    public void setTopics(TopicResponse topicsList) {
        this.topicsList.add(topicsList);
    }
}

