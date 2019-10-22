package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class TopicsExtractionResponse {
    List<TopicExtractionResponse> termsList = new ArrayList<>();

    public List<TopicExtractionResponse> getTerms() {
        return this.termsList;
    }

    public void setTopics(List<TopicExtractionResponse> termsList) {
        this.termsList = termsList;
    }
}
