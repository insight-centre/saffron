package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class TopicsSimilarityResponse extends BaseResponse {

    List<TopicSimilarityResponse> termsList = new ArrayList<>();

    public List<TopicSimilarityResponse> getTerms() {
        return this.termsList;
    }

    public void setTerms(List<TopicSimilarityResponse> termsList) {
        this.termsList = termsList;
    }
}
