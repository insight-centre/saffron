package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class AuthorsTermsResponse {
    List<AuthorTermsResponse> termsList = new ArrayList<>();

    public List<AuthorTermsResponse> getTerms() {
        return this.termsList;
    }

    public void setTerms(List<AuthorTermsResponse> termsList) {
        this.termsList = termsList;
    }
}
