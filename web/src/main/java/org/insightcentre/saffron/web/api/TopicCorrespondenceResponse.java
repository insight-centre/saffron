package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.List;

@XmlRootElement()
public class TopicCorrespondenceResponse {

    private String id;
    private String acronym;
    private String term;
    private String pattern;
    private Integer occurences;
    private String documentId;
    private String tfidf;
    private Date runDate;
    private String run;



    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getTerm() {
        return term;
    }


    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentId() {
        return documentId;
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

    public void setOccurrences(Integer occurrences) {
        this.occurences = occurrences;
    }

    public Integer getOccurances() {
        return occurences;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    public void setTfidf(String tfidf) {
        this.tfidf = tfidf;
    }

    public String getTfidf() {
        return tfidf;
    }
}
