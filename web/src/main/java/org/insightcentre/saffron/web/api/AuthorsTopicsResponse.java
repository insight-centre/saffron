package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement()
public class AuthorsTopicsResponse {
    List<AuthorTopicsResponse> termsList = new ArrayList<>();

    public List<AuthorTopicsResponse> getTerms() {
        return this.termsList;
    }

    public void setTerms(List<AuthorTopicsResponse> termsList) {
        this.termsList = termsList;
    }
}
