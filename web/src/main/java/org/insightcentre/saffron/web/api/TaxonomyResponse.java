package org.insightcentre.saffron.web.api;

import com.mongodb.BasicDBObject;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement()
public class TaxonomyResponse extends BasicDBObject {


    private String root;
    private double score;
    private double linkScore;
    private List<TaxonomyResponse> children;


    public void setRoot(String root) {
        this.root = root;
    }

    public String getRoot() {
        return root;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public void setLinkScore(double linkScore) {
        this.linkScore = linkScore;
    }

    public double getLinkScore() {
        return linkScore;
    }


    public void setChildren(List<TaxonomyResponse> children) {
        this.children = children;
    }

    public List<TaxonomyResponse> getChildren() {
        return children;
    }


}
