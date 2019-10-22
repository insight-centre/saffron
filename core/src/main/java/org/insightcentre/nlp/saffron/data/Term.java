package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Term implements Comparable<Term> {

    
    /**
     * The unique string for this topic
     */
	public final static String JSON_TERM_STRING = "term_string";
	@JsonAlias("topic_string") //Enable compatibility with previous Saffron version
	@JsonProperty(JSON_TERM_STRING)
    private String termString;
	
    /**
     * The number of times this occurs in text
     */
	public final static String JSON_OCCURRENCES = "occurrences";
	@JsonProperty(JSON_OCCURRENCES)
    private int occurrences;
    
    /**
     * The number of documents this occurs in
     */
    public final static String JSON_MATCHES = "matches";
    @JsonProperty(JSON_MATCHES)
    private int matches;
    
    /**
     * The importance of this topic
     */
    public final static String JSON_SCORE = "score";
    @JsonProperty(JSON_SCORE)
    private double score;
    
    /**
     * Any detected morphological variations of this term
     */
    public final static String JSON_MORPHOLOGICAL_VARIATION_LIST = "mv_list";
    @JsonProperty(JSON_MORPHOLOGICAL_VARIATION_LIST)
    private final List<MorphologicalVariation> mvList;
    
    /**
     * The link to DBpedia (may be null)
     */
    public final static String JSON_DBPEDIA_URL = "dbpedia_url";
    @JsonProperty(JSON_DBPEDIA_URL)
    private URL dbpediaUrl;
    
    /**
     * The statue of the topic
     */
    public final static String JSON_STATUS = "status";
    @JsonProperty(JSON_STATUS)
    private Status status;
    
    /**
     * The original string
     */
    public final static String JSON_ORIGINAL_TOPIC = "original_topic";
    @JsonProperty(JSON_ORIGINAL_TOPIC)
    private final String originalTopic;

    private Term(String topicString) {
    	this.termString = topicString;
    	this.mvList = Collections.EMPTY_LIST;
    	this.originalTopic = topicString;
    }
    
    @JsonCreator
    public Term(
            @JsonAlias("topic_string") @JsonProperty(value = JSON_TERM_STRING, required = true) String topic_string,
            @JsonProperty(value = JSON_OCCURRENCES) int occurrences,
            @JsonProperty(value = JSON_MATCHES) int matches,
            @JsonProperty(value = JSON_SCORE) double score,
            @JsonProperty(value = JSON_MORPHOLOGICAL_VARIATION_LIST) List<MorphologicalVariation> mvList,
            @JsonProperty(value = JSON_STATUS) String status) {
        super();
        this.termString = topic_string;
        this.occurrences = occurrences;
        this.matches = matches;
        this.score = score;
        this.mvList = mvList == null ? Collections.EMPTY_LIST : mvList;
        this.originalTopic = topic_string;
        try {
        	if (status != null)
        		this.status = Status.valueOf(status);
        	else
        		this.status = Status.none;
        } catch (IllegalArgumentException e) {
        	this.status = Status.none;
        }
    }

    public String getString() {
		return termString;
	}

	public void setString(String string) {
		this.termString = string;
	}

	public int getOccurrences() {
		return occurrences;
	}

	public void setOccurrences(int occurrences) {
		this.occurrences = occurrences;
	}

	public int getMatches() {
		return matches;
	}

	public void setMatches(int matches) {
		this.matches = matches;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}
	
	public List<MorphologicalVariation> getMorphologicalVariationList() {
		return mvList;
	}
	
	public void addMorphologicalVariation(MorphologicalVariation mv) {
        mvList.add(mv);
    }

	public URL getDbpediaUrl() {
		return dbpediaUrl;
	}

	public void setDbpediaUrl(URL dbpediaUrl) {
		this.dbpediaUrl = dbpediaUrl;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getOriginalTopic() {
		return originalTopic;
	}

    @Override
    public int compareTo(Term o) {
        int i0 = Double.compare(score, o.score);
        if (i0 == 0) {
            int i = Integer.compare(occurrences, o.occurrences);
            if (i == 0) {
                int i1 = termString.compareTo(o.termString);
                return i1;
            } else {
                return -i;
            }
        }
        return -i0;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.termString);
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
        final Term other = (Term) obj;
        if (!Objects.equals(this.termString, other.termString)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Topic{" + "topicString=" + termString + '}';
    }

    public static class MorphologicalVariation {

        /**
         * The string form of this topic as it occurs
         */
        public final String string;
        /**
         * The number of times this variant occurs
         */
        public int occurrences;

        /**
         * The pattern that this term matched
         */
        //public String pattern;
        /**
         * The expanded version of the acronym (if any)
         */
        //public String expanded_acronym;
        /**
         * The acronym for the term
         */
        //public String acronym;

        @JsonCreator
        public MorphologicalVariation(@JsonProperty(value = "string") String string) {
            super();
            this.string = string;
        }

        public String getString() {
            return string;
        }

        @Override
        public String toString() {
            return "MorphologicalVariation [string=" + string + "]";
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + Objects.hashCode(this.string);
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
            final MorphologicalVariation other = (MorphologicalVariation) obj;
            if (!Objects.equals(this.string, other.string)) {
                return false;
            }
            return true;
        }

    }
    
    public static class Builder {
    	
    	Term topic;
    	
    	public Builder(String topicString) {
    		topic = new Term(topicString);
    	}
    	
    	public Builder occurrences(int occurences) {
    		topic.occurrences = occurences;
    		return this;
    	}
    	
    	public Builder matches(int matches) {
    		topic.matches = matches;
    		return this;
    	}
    	
    	public Builder score(double score) {
    		topic.score = score;
    		return this;
    	}
    	
    	public Builder dbpediaUrl(URL url) {
    		topic.dbpediaUrl = url;
    		return this;
    	}
    	
    	public Builder status(Status status) {
    		topic.status = status;
    		return this;
    	}
    	
    	public Term build() {
    		return topic;
    	}
    }

}

