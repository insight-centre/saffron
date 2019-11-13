package org.insightcentre.saffron.web.api;


import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement()
public class TermSimilarityResponse {

    private double similarity;
    private String id;
    private String termString1;
    private String termString2;
    private Date runDate;
    private String run;



    public void setTermString1(String termString1) {
        this.termString1 = termString1;
    }

    public String getTermString1() {
        return termString1;
    }

    public void setTermString2(String termString2) {
        this.termString2 = termString2;
    }

    public String getTermString2() {
        return termString2;
    }


    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    public Date getRunDate() {
        return runDate;
    }

    public void setRun(String run) {
        this.run = run;
    }

    public String getRun() {
        return run;
    }


    public void setId(String id) {
        this.id = id;
    }


    public String getId() {
        return id;
    }
}
