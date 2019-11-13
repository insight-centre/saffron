package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class AuthorsSimilarityResponse extends BaseResponse {

    List<AuthorSimilarityResponse> termsList = new ArrayList<>();

    public List<AuthorSimilarityResponse> getTerms() {
        return this.termsList;
    }

    public void setTerms(List<AuthorSimilarityResponse> termsList) {
        this.termsList = termsList;
    }
}
