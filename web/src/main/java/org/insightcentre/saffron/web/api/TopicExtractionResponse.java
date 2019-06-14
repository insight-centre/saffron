package org.insightcentre.saffron.web.api;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.List;

@XmlRootElement()
public class TopicExtractionResponse {

    private String id;
    private String topic;
    private Integer matches;
    private Integer occurences;
    private Double score;
    private List<String> mvList;
    private Date runDate;
    private String run;
    private String dbpediaUrl;




    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }


    public void setScore(Double score) {
        this.score = score;
    }

    public Double getScore() {
        return score;
    }

    public void setMvList(List<String> mvList) {
        this.mvList = mvList;
    }

    public List<String> getMvList() {
        return mvList;
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

    public void setDbpediaUrl(String dbpediaUrl) {
        this.dbpediaUrl = dbpediaUrl;
    }

    public String getDbpediaUrl() {
        return dbpediaUrl;
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

    public void setMatches(Integer matches) {
        this.matches = matches;
    }

    public Integer getMatches() {
        return matches;
    }
}
