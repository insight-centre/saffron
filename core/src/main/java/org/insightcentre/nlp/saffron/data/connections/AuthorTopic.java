package org.insightcentre.nlp.saffron.data.connections;

import java.util.Objects;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class AuthorTopic {
    public String author_id;
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.author_id);
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
        if (!Objects.equals(this.author_id, other.author_id)) {
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
