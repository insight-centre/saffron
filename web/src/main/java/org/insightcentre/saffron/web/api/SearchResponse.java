package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement()
public class SearchResponse {

    private String termString;
    private String id;
    private String location;
    private String snippet;

    public void setTermString(String termString) {
        this.termString = termString;
    }

    public String getTermString() {
        return termString;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getSnippet() {
        return snippet;
    }
}
