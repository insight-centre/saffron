package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A taxonomy of topics
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class Taxonomy {
    public final String root;
    public final double score;
    public final List<Taxonomy> children;

    @JsonCreator
    public Taxonomy(@JsonProperty("root") String root, 
                    @JsonProperty("score") double score,
                    @JsonProperty("children") List<Taxonomy> children) {
        this.root = root;
        this.score = score;
        this.children = Collections.unmodifiableList(children == null ? new ArrayList<Taxonomy>() : children);
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
    
    /**
     * Search this taxonomy for a taxonomy with a given root
     * @param name The name to search for
     * @return A taxonomy whose root is name or null if no taxonomy is found
     */
    public Taxonomy descendent(String name) {
        if(this.root.equals(name))
            return this;
        for(Taxonomy child : children) {
            Taxonomy d = child.descendent(name);
            if(d != null)
                return d;
        }
        return null;
    } 
    
 
    
    /**
     * The size of the taxonomy (number of topics). Note this calculates the size 
     * and so takes O(N) time!
     * @return The number of topics in the taxonomy
     */
    public int size() {
        int size = 1;
        for(Taxonomy t : children) {
            size += t.size();
        }
        return size;
    }
    
    /**
     * Verify if there are no loops in this taxonomy
     * @return true if there are no loops
     */
    public boolean verifyTree() {
        Set<String> terms = new HashSet<>();
        return _isTree(terms);
    }
    

    private boolean _isTree(Set<String> terms) {
        if(terms.contains(root)) {
            return false;
        } else {
            terms.add(root);
            for(Taxonomy t : children) {
                if(!t._isTree(terms)) {
                    return false;
                }
            }
            terms.remove(root);
            return true;
        }
    }

    /**
     * Create a deep copy of this taxonomy
     * @return A copy of this taxonomy
     */
    public Taxonomy deepCopy() {
        List<Taxonomy> newChildren = new ArrayList<>();
        for(Taxonomy t : children) {
            newChildren.add(t.deepCopy());
        }
        return new Taxonomy(this.root, this.score, newChildren);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.root);
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.score) ^ (Double.doubleToLongBits(this.score) >>> 32));
        hash = 97 * hash + Objects.hashCode(this.children);
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
        final Taxonomy other = (Taxonomy) obj;
        if (Double.doubleToLongBits(this.score) != Double.doubleToLongBits(other.score)) {
            return false;
        }
        if (!Objects.equals(this.root, other.root)) {
            return false;
        }
        if (!Objects.equals(this.children, other.children)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s (%.4f) { %s }", root, score, children.toString());
    }
}
