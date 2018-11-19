package org.insightcentre.nlp.saffron.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A taxonomy of topics
 * 
 * @author John McCrae <john@mccr.ae>
 */
public class Taxonomy {
    /** The topic string of this node in the taxonomy */
    public final String root;
    /** The score associated with this topic (its importance) */
    public final double score;
    /** The score relating this node to its parent (NaN if there is no parent) */
    public final double linkScore;
    /** The list of child nodes */
    public final List<Taxonomy> children;

    @JsonCreator
    public Taxonomy(@JsonProperty("root") String root, 
                    @JsonProperty("score") double score,
                    @JsonProperty("linkScore") double linkScore,
                    @JsonProperty("children") List<Taxonomy> children) {
        this.root = root;
        this.score = score;
        this.linkScore = linkScore;
        this.children = children == null ? new ArrayList<Taxonomy>() : children;
        //this.children = Collections.unmodifiableList(children == null ? new ArrayList<Taxonomy>() : children);
    }
    
    /**
     * Build a Taxonomy object from a JSON file
     * @param file The json file to read from
     * @return a Taxonomy object
     * 
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public static Taxonomy fromJsonFile(File file) throws JsonParseException, JsonMappingException, IOException{
    	ObjectMapper objectMapper = new ObjectMapper();
    	return objectMapper.readValue(file, Taxonomy.class);
    }

    /**
     * Get the string for the root topic
     * @return 
     */
    public String getRoot() {
        return root;
    }

    /**
     * Get the list of children nodes
     * @return A list of taxonomy nodes
     */
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
     * The maximum depth of the taxonomy
     * @return The maximum depth of the taxonomy
     */
    public int depth(){
        int depth = 0;
        for(Taxonomy t : children) {
            depth = Math.max(depth, t.depth() + 1);
        }
        return depth;
    }
    
    /**
     * The minimum depth in the taxonomy
     * @return The minimum depth in the taxonomy
     */
    public int minDepth() {
    	int minDepth = 0;
    	
    	boolean firstChild = true;
    	for(Taxonomy t: children) {
    		if (firstChild) {
    			minDepth = t.minDepth() + 1;
    			firstChild = false;
    		} else {
    			minDepth = Math.min(minDepth, t.minDepth() + 1);
    		}
    		
    		if (minDepth == 1) {
    			return minDepth;
    		}
    	}
    	
    	return minDepth;
    }
    
    /**
     * The median depth of the taxonomy
     * @return The median depth of the taxonomy
     */
    public double medianDepth() {
    	//Calculate the depth to each leaf node (i.e. each node without children)
    	Map<String, Integer> leafDepths = this.leavesDepths(0);
    	
    	//Calculate the median depth
    	Integer[] depthArray = new Integer[leafDepths.values().size()]; 
    	depthArray = leafDepths.values().toArray(depthArray);    	
    	Arrays.sort(depthArray);
    	
    	double median;
    	if (depthArray.length % 2 == 0)
    	    median = ((double)depthArray[depthArray.length/2] + (double)depthArray[depthArray.length/2 - 1])/2;
    	else
    	    median = (double) depthArray[depthArray.length/2];
    	
    	return median;
    }
    
    /**
     * Return all leaf nodes and their correspondent depths
     * 
     * @param currentDepth The depth of the parent node
     * @return A map with 
     * 		'key': the label(root) of each leaf node, and 
     * 		'value': the depth of each leaf node.
     */
    protected Map<String, Integer> leavesDepths(Integer currentDepth) {
    	Map<String, Integer> leafDepths = new HashMap<String, Integer>();
    	
    	for(Taxonomy child: children) {
    		leafDepths.putAll(child.leavesDepths(currentDepth+1));
    	}
    	if(children.isEmpty()) {
    		leafDepths.put(root, currentDepth);
    	}
    	
		return leafDepths;
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
     * Create a copy of this with a new link score
     * @param linkScore The new link score
     * @return A new taxonomy instance
     */
    public Taxonomy withLinkScore(double linkScore) {
        return new Taxonomy(this.root, this.score, linkScore, this.children);
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
        return new Taxonomy(this.root, this.score, this.linkScore, newChildren);
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
