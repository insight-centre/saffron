package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement()
public class TermResponse {
    private String termString;
    private Integer occurrences;
    private Integer matches;
    private double score;
    private List<String> mvList;
    private String id;
    private String status;


    public void setTermString(String termString) {
        this.termString = termString;
    }

    public String getTermString() {
        return termString;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setOccurrences(Integer occurrences) {
        this.occurrences = occurrences;
    }

    public Integer getOccurances() {
        return occurrences;
    }

    public void setMatches(Integer matches) {
        this.matches = matches;
    }

    public Integer getMatches() {
        return matches;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public void setMvList(List<String> mvList) {
        this.mvList = mvList;
    }

    public List<String> getMvList() {
        return mvList;
    }

    public void setId(String id) {
        this.id = id;
    }


    public String getId() {
        return id;
    }
}
