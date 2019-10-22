package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;



@XmlRootElement()
public class TopicsResponse extends BaseResponse {


    List<TopicResponse> termsList = new ArrayList<>();

    public List<TopicResponse> getTerms() {
        return this.termsList;
    }

    public void setTerms(TopicResponse termsList) {
        this.termsList.add(termsList);
    }
}

