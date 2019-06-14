package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class AuthorsSimilarityResponse extends BaseResponse {

    List<AuthorSimilarityResponse> topicsList = new ArrayList<>();

    public List<AuthorSimilarityResponse> getTopics() {
        return this.topicsList;
    }

    public void setTopics(List<AuthorSimilarityResponse> topicsList) {
        this.topicsList = topicsList;
    }
}
