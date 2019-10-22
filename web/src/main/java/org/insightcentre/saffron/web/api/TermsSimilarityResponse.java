package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class TermsSimilarityResponse extends BaseResponse {

    List<TermSimilarityResponse> termsList = new ArrayList<>();

    public List<TermSimilarityResponse> getTerms() {
        return this.termsList;
    }

    public void setTerms(List<TermSimilarityResponse> termsList) {
        this.termsList = termsList;
    }
}
