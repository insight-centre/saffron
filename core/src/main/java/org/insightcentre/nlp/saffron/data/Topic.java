package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Topic implements Comparable<Topic> {

    @JsonProperty("topic_string")
    /** The unique string for this topic */
    public final String topicString;
    /** The number of times this occurs in text */
    public int occurrences;
    /** The number of documents this occurs in */
    public int matches;
    /** The importance of this topic */
    public double score;
    /** Any detected morphological variations of this term */
    public final List<MorphologicalVariation> mvList;
    /** The link to DBpedia (may be null) */
    public URL dbpedia_url;

    @JsonCreator
    public Topic(
        @JsonProperty(value="topic_string", required = true) String topic_string, 
        @JsonProperty(value="occurrences") int occurrences,
        @JsonProperty(value="matches") int matches,
        @JsonProperty(value="score") double score, 
        @JsonProperty(value="mv_list") List<MorphologicalVariation> mvList) {
        super();
        this.topicString = topic_string;
        this.occurrences = occurrences;
        this.matches     = matches;
        this.score = score;
        this.mvList = mvList == null ? Collections.EMPTY_LIST : mvList;
    }

    public void addMorphologicalVariation(MorphologicalVariation mv) {
        mvList.add(mv);
    }
    
    @Override
    public int compareTo(Topic o) {
        int i0 = Double.compare(score, o.score);
        if (i0 == 0) {
            int i1 = topicString.compareTo(o.topicString);
            return i1;
        }
        return i0;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.topicString);
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
        final Topic other = (Topic) obj;
        if (!Objects.equals(this.topicString, other.topicString)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Topic{" + "topicString=" + topicString + '}';
    }

    public static class MorphologicalVariation {
        
        /** The string form of this topic as it occurs */
        public final String string;
        /** The number of times this variant occurs */
        public int occurrences;
        /** The pattern that this term matched */
        //public String pattern;
        /** The expanded version of the acronym (if any) */
        //public String expanded_acronym;
        /** The acronym for the term */
        //public String acronym;

        @JsonCreator
        public MorphologicalVariation(@JsonProperty(value="string") String string) {
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
    
}
