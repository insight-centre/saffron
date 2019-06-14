package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement()
public class AuthorSimilarityResponse {
    private double similarity;
    private String id;
    private String topicString1;
    private String topicString2;
    private Date runDate;
    private String run;



    public void setTopicString1(String topicString1) {
        this.topicString1 = topicString1;
    }

    public String getTopicString1() {
        return topicString1;
    }

    public void setTopicString2(String topicString2) {
        this.topicString2 = topicString2;
    }

    public String getTopicString2() {
        return topicString2;
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
