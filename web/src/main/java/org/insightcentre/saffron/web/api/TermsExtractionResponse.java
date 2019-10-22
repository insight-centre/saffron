package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class TermsExtractionResponse {
    List<TermExtractionResponse> termsList = new ArrayList<>();

    public List<TermExtractionResponse> getTerms() {
        return this.termsList;
    }

    public void setTopics(List<TermExtractionResponse> termsList) {
        this.termsList = termsList;
    }
}
