package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class TopicsCorrespondenceResponse {
    List<TopicCorrespondenceResponse> termsList = new ArrayList<>();

    public List<TopicCorrespondenceResponse> getTerms() {
        return this.termsList;
    }

    public void setTopics(List<TopicCorrespondenceResponse> termsList) {
        this.termsList = termsList;
    }
}
