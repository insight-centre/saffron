package org.insightcentre.nlp.saffron.taxonomy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Topic implements Comparable<Topic> {

    private final String preferredString, rootSequence;
    private final int overallOccurrence, overallMatches;
    private final double rank;
    private final List<MorphologicalVariation> mvList;

    @JsonCreator
    public Topic(
        @JsonProperty(value="@id", required = true) String preferredString, 
        @JsonProperty(value="slug", required = true) String rootSequence, 
        @JsonProperty(value="overallOccurrence") int overallOccurrence,
        @JsonProperty(value="overallFrequency") int overallFrequency, 
        @JsonProperty(value="score", required = true) double rank, 
        @JsonProperty(value="mvList") List<MorphologicalVariation> mvList) {
        super();
        this.preferredString = preferredString;
        this.rootSequence = rootSequence;
        this.overallOccurrence = overallOccurrence;
        this.overallMatches = overallFrequency;
        this.rank = rank;
        this.mvList = mvList == null ? Collections.EMPTY_LIST : mvList;
    }

    @JsonProperty(value="@id")
    public String getPreferredString() {
        return preferredString;
    }

    @Override
    public String toString() {
        return "Topic [preferredString=" + preferredString + ", rootSequence=" + rootSequence
            + ", overallOccurrence=" + overallOccurrence + ", overallMatches="
            + overallMatches + ", rank=" + rank + ", mvList=" + mvList + "]";
    }

    @JsonProperty(value="slug")
    public String getRootSequence() {
        return rootSequence;
    }

    public int getOverallOccurrence() {
        return overallOccurrence;
    }

    public int getOverallMatches() {
        return overallMatches;
    }

    @JsonProperty(value="score")
    public double getRank() {
        return rank;
    }

    public List<MorphologicalVariation> getMvList() {
        return mvList;
    }

    @Override
    public int compareTo(Topic o) {
        int i0 = Double.compare(rank, o.rank);
        if (i0 == 0) {
            int i1 = preferredString.compareTo(o.preferredString);
            if (i1 == 0) {
                return rootSequence.compareTo(o.rootSequence);
            }
            return i1;
        }
        return i0;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.preferredString);
        hash = 37 * hash + Objects.hashCode(this.rootSequence);
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.rank) ^ (Double.doubleToLongBits(this.rank) >>> 32));
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
        if (!Objects.equals(this.preferredString, other.preferredString)) {
            return false;
        }
        if (!Objects.equals(this.rootSequence, other.rootSequence)) {
            return false;
        }
        if (Double.doubleToLongBits(this.rank) != Double.doubleToLongBits(other.rank)) {
            return false;
        }
        return true;
    }

}
