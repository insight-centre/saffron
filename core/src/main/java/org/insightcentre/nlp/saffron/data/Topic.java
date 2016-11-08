package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Topic implements Comparable<Topic> {

    public final String topicString, slug;
    public final int occurrences;
    public final double score;
    public final List<MorphologicalVariation> mvList;
    public URL dbpediaURL;

    @JsonCreator
    public Topic(
        @JsonProperty(value="topic_string", required = true) String topic_string, 
        @JsonProperty(value="slug", required = true) String slug, 
        @JsonProperty(value="occurrences") int occurrences,
        @JsonProperty(value="score") double score, 
        @JsonProperty(value="mv_list") List<MorphologicalVariation> mvList) {
        super();
        this.topicString = topic_string;
        this.slug = slug;
        this.occurrences = occurrences;
        this.score = score;
        this.mvList = mvList == null ? Collections.EMPTY_LIST : mvList;
    }

    @JsonProperty("topic_string")
    public String getTopic_string() {
        return topicString;
    }

    public String getSlug() {
        return slug;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public double getScore() {
        return score;
    }

    public URL getDbpediaURL() {
        return dbpediaURL;
    }

    public void setDbpediaURL(URL dbpediaURL) {
        this.dbpediaURL = dbpediaURL;
    }

    @JsonProperty("mv_list")
    public List<MorphologicalVariation> getMvList() {
        return mvList;
    }

    public void addMorphologicalVariation(MorphologicalVariation mv) {
        mvList.add(mv);
    }
    
    @Override
    public int compareTo(Topic o) {
        int i0 = Double.compare(score, o.score);
        if (i0 == 0) {
            int i1 = topicString.compareTo(o.topicString);
            if (i1 == 0) {
                return slug.compareTo(o.slug);
            }
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

    public static class MorphologicalVariation {
        
        public final String string;
        public int extractedTermOccurrences;
        public String pattern;
        public String expandedAcronym;
        public String acronym;

      
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

        public int getExtractedTermOccurrences() {
            return extractedTermOccurrences;
        }

        public void setExtractedTermOccurrences(int extractedTermOccurrences) {
            this.extractedTermOccurrences = extractedTermOccurrences;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getExpandedAcronym() {
            return expandedAcronym;
        }

        public void setExpandedAcronym(String expandedAcronym) {
            this.expandedAcronym = expandedAcronym;
        }

        public String getAcronym() {
            return acronym;
        }

        public void setAcronym(String acronym) {
            this.acronym = acronym;
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
