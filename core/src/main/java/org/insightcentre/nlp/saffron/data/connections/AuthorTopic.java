package org.insightcentre.nlp.saffron.data.connections;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class AuthorTopic {
    public String researcher_id;
    public String topic_id;
    public int matches;
    public int occurrences;
    public int paper_count;  // Number of papers for researcher that topic occurs in.
    public double tfirf;     // Like TFIDF but with researchers instead of documents.
                             // See "Domain adaptive extraction of topical hierarchies 
                             // for Expertise Mining" (Georgeta Bordea (2013)) for
                             // evaluations of different methods.
    public double score;     // tfirf * topic score
    public double researcher_score; // score for researcher's ranking for this particular topic

    @JsonProperty("researcher_id")
    public String getResearcherId() {
        return researcher_id;
    }

    @JsonProperty("researcher_id")
    public void setResearcherId(String researcher_id) {
        this.researcher_id = researcher_id;
    }

    @JsonProperty("topic_id")
    public String getTopicId() {
        return topic_id;
    }

    @JsonProperty("topic_id")
    public void setTopicId(String topic_id) {
        this.topic_id = topic_id;
    }

    public int getMatches() {
        return matches;
    }

    public void setMatches(int matches) {
        this.matches = matches;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
    }

    @JsonProperty("paper_count")
    public int getPaperCount() {
        return paper_count;
    }

    @JsonProperty("paper_count")
    public void setPaperCount(int paper_count) {
        this.paper_count = paper_count;
    }

    public double getTfirf() {
        return tfirf;
    }

    public void setTfirf(double tfirf) {
        this.tfirf = tfirf;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @JsonProperty("researcher_score")
    public double getResearcherScore() {
        return researcher_score;
    }

    @JsonProperty("researcher_score")
    public void setResearcherScore(double researcher_score) {
        this.researcher_score = researcher_score;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.researcher_id);
        hash = 43 * hash + Objects.hashCode(this.topic_id);
        hash = 43 * hash + this.matches;
        hash = 43 * hash + this.occurrences;
        hash = 43 * hash + this.paper_count;
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.tfirf) ^ (Double.doubleToLongBits(this.tfirf) >>> 32));
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.score) ^ (Double.doubleToLongBits(this.score) >>> 32));
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.researcher_score) ^ (Double.doubleToLongBits(this.researcher_score) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AuthorTopic other = (AuthorTopic) obj;
        if (!Objects.equals(this.researcher_id, other.researcher_id)) {
            return false;
        }
        if (!Objects.equals(this.topic_id, other.topic_id)) {
            return false;
        }
        if (this.matches != other.matches) {
            return false;
        }
        if (this.occurrences != other.occurrences) {
            return false;
        }
        if (this.paper_count != other.paper_count) {
            return false;
        }
        if (Double.doubleToLongBits(this.tfirf) != Double.doubleToLongBits(other.tfirf)) {
            return false;
        }
        if (Double.doubleToLongBits(this.score) != Double.doubleToLongBits(other.score)) {
            return false;
        }
        if (Double.doubleToLongBits(this.researcher_score) != Double.doubleToLongBits(other.researcher_score)) {
            return false;
        }
        return true;
    }
}
