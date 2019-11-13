package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * A taxonomy with the relative size of each term.
 * 
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class TaxonomyWithSize  {
    
    public final String root;
    public final double score;
    public final List<TaxonomyWithSize> children;
    public final int size;

    @JsonCreator
    public TaxonomyWithSize(@JsonProperty("root") String root, 
                    @JsonProperty("score") double score,
                    @JsonProperty("children") List<TaxonomyWithSize> children,
                    @JsonProperty("size") int size) {
        this.root = root;
        this.score = score;
        this.children = children == null ? new ArrayList<TaxonomyWithSize>() : children;
        this.size = size;
    }

    public String getRoot() {
        return root;
    }

    public List<TaxonomyWithSize> getChildren() {
        return children;
    }

    public int getSize() {
        return size;
    }
}
