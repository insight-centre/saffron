package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A taxonomy of topics
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class Taxonomy {
    public final String root;
    public final List<Taxonomy> children;

    @JsonCreator
    public Taxonomy(@JsonProperty("root") String root, 
                    @JsonProperty("children") List<Taxonomy> children) {
        this.root = root;
        this.children = children == null ? new ArrayList<Taxonomy>() : children;
    }

    public String getRoot() {
        return root;
    }

    public List<Taxonomy> getChildren() {
        return children;
    }
    
    /**
     * Is the target value anywhere in this taxonomy
     * @param name The root value that may be in this taxonomy
     * @return 
     */
    public boolean hasDescendent(String name) {
        if(this.root.equals(name))
            return true;
        for(Taxonomy child : children) {
            if(child.hasDescendent(name))
                return true;
        }
        return false;
    } 

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.root);
        hash = 73 * hash + Objects.hashCode(this.children);
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
        final Taxonomy other = (Taxonomy) obj;
        if (!Objects.equals(this.root, other.root)) {
            return false;
        }
        if (!Objects.equals(this.children, other.children)) {
            return false;
        }
        return true;
    }


}
