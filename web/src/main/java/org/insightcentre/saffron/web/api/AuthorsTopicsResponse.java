package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class AuthorsTopicsResponse {
    List<AuthorTopicsResponse> topicsList = new ArrayList<>();

    public List<AuthorTopicsResponse> getTopics() {
        return this.topicsList;
    }

    public void setTopics(List<AuthorTopicsResponse> topicsList) {
        this.topicsList = topicsList;
    }
}
