package org.insightcentre.nlp.saffron.term;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Objects;

/**
 *
 * @author John McCrae <john@mccr.ae>
 */
public class FrequencyStats {
    public Object2IntMap<String> termFrequency = new Object2IntOpenHashMap<>();
    public Object2IntMap<String> docFrequency = new Object2IntOpenHashMap<>();
    public long tokens = 0;
    public long documents = 0;
        
    public void add(FrequencyStats other) {
        for(Object2IntMap.Entry<String> tf2 : other.termFrequency.object2IntEntrySet()) {
            termFrequency.put(tf2.getKey(), termFrequency.getInt(tf2.getKey()) + tf2.getIntValue());
        }
        for(Object2IntMap.Entry<String> tf2 : other.docFrequency.object2IntEntrySet()) {
            docFrequency.put(tf2.getKey(), docFrequency.getInt(tf2.getKey()) + tf2.getIntValue());
        }
        tokens += other.tokens;
        documents += other.documents;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.termFrequency);
        hash = 59 * hash + Objects.hashCode(this.docFrequency);
        hash = 59 * hash + (int) (this.tokens ^ (this.tokens >>> 32));
        hash = 59 * hash + (int) (this.documents ^ (this.documents >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FrequencyStats other = (FrequencyStats) obj;
        if (this.tokens != other.tokens) {
            return false;
        }
        if (this.documents != other.documents) {
            return false;
        }
        if (!Objects.equals(this.termFrequency, other.termFrequency)) {
            return false;
        }
        if (!Objects.equals(this.docFrequency, other.docFrequency)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TermExtractionStats{" + "termFrequency=" + termFrequency + ", docFrequency=" + docFrequency + ", tokens=" + tokens + ", documents=" + documents + '}';
    }
    
    
}
