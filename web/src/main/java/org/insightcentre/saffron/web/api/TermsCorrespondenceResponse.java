package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class TermsCorrespondenceResponse {
    List<TermCorrespondenceResponse> termsList = new ArrayList<>();

    public List<TermCorrespondenceResponse> getTerms() {
        return this.termsList;
    }

    public void setTopics(List<TermCorrespondenceResponse> termsList) {
        this.termsList = termsList;
    }
}
