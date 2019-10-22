package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;



@XmlRootElement()
public class TermsResponse extends BaseResponse {


    List<TermResponse> termsList = new ArrayList<>();

    public List<TermResponse> getTerms() {
        return this.termsList;
    }

    public void setTerms(TermResponse termsList) {
        this.termsList.add(termsList);
    }
}

