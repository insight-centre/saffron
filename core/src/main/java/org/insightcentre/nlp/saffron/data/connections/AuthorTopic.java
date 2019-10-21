package org.insightcentre.nlp.saffron.data.connections;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorTopic {
    private String authorId;
    private String termId;
    private int matches;
    private int occurrences;
    private int paperCount;  // Number of papers for researcher that topic occurs in.
    private double tfIrf;     // Like TFIDF but with researchers instead of documents.
                             // See "Domain adaptive extraction of topical hierarchies 
                             // for Expertise Mining" (Georgeta Bordea (2013)) for
                             // evaluations of different methods.
    private double score;     // tfirf * topic score
    private double researcherScore; // score for researcher's ranking for this particular topic
    
    public String getAuthorId() {
		return authorId;
	}

	public void setAuthorId(String authorId) {
		this.authorId = authorId;
	}

	public String getTermId() {
		return termId;
	}

	public void setTermId(String termId) {
		this.termId = termId;
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

	public int getPaperCount() {
		return paperCount;
	}

	public void setPaperCount(int paperCount) {
		this.paperCount = paperCount;
	}

	public double getTfIrf() {
		return tfIrf;
	}

	public void setTfIrf(double tfIrf) {
		this.tfIrf = tfIrf;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public double getResearcherScore() {
		return researcherScore;
	}

	public void setResearcherScore(double researcherScore) {
		this.researcherScore = researcherScore;
	}

	@Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.authorId);
        hash = 43 * hash + Objects.hashCode(this.termId);
        hash = 43 * hash + this.matches;
        hash = 43 * hash + this.occurrences;
        hash = 43 * hash + this.paperCount;
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.tfIrf) ^ (Double.doubleToLongBits(this.tfIrf) >>> 32));
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.score) ^ (Double.doubleToLongBits(this.score) >>> 32));
        hash = 43 * hash + (int) (Double.doubleToLongBits(this.researcherScore) ^ (Double.doubleToLongBits(this.researcherScore) >>> 32));
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
        if (!Objects.equals(this.authorId, other.authorId)) {
            return false;
        }
        if (!Objects.equals(this.termId, other.termId)) {
            return false;
        }
        if (this.matches != other.matches) {
            return false;
        }
        if (this.occurrences != other.occurrences) {
            return false;
        }
        if (this.paperCount != other.paperCount) {
            return false;
        }
        if (Double.doubleToLongBits(this.tfIrf) != Double.doubleToLongBits(other.tfIrf)) {
            return false;
        }
        if (Double.doubleToLongBits(this.score) != Double.doubleToLongBits(other.score)) {
            return false;
        }
        if (Double.doubleToLongBits(this.researcherScore) != Double.doubleToLongBits(other.researcherScore)) {
            return false;
        }
        return true;
    }
}
