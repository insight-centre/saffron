package org.insightcentre.nlp.saffron.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import org.insightcentre.nlp.saffron.exceptions.InvalidOperationException;
import org.insightcentre.nlp.saffron.exceptions.InvalidValueException;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A taxonomy of terms
 * 
 * @author John McCrae &lt;john@mccr.ae&gt;
 * @author Bianca Pereira
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Taxonomy {
	
    /** The term string of this node in the taxonomy */
    public String root;
    /** The original parent node of this node in the taxonomy */
    public String originalParent;
    /** The original Term string of this node in the taxonomy */
    public String originalTerm;
    /** The score associated with this term (its importance) */
    public final double score;
    /** The score relating this node to its parent (NaN if there is no parent) */
    public final double linkScore;
    /** The list of child nodes */
    public List<Taxonomy> children;
    /** The status of the term string */
    public Status status;

    public List<Taxonomy> parent;

    protected Taxonomy() {
    	score = 0.0;
    	linkScore = 0.0;
    	children = new ArrayList<Taxonomy>();
    }

    @JsonCreator
    @JsonIgnoreProperties(ignoreUnknown = true)
    public Taxonomy(@JsonProperty("root") String root,
                    @JsonProperty("score") double score,
                    @JsonProperty("linkScore") double linkScore,
                    @JsonProperty("children") List<Taxonomy> children,
                    @JsonProperty("status") Status status) {
        this.root = root;
        this.score = score;
        this.linkScore = linkScore;
        this.children = children == null ? new ArrayList<Taxonomy>() : children;
        this.status = status;
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
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    	return objectMapper.readValue(file, Taxonomy.class);
    }

    /**
     * Get the status of the relation between the root term and its parent
     * @return 
     */
    public Status getStatus() {
        return status;
    }


    /**
     * Set the status of the relation between the the root term and its parent
     * @return
     */
    public void setStatus(Status status) {

        this.status = status;
    }
    
    /**
     * Traverse the taxonomy and modify the status of a parent-child relationship.
     * 
     * @param childTerm - the child term
     * @param status - the new status
     * @throws InvalidOperationException - thrown in case of a "rejected" status. Taxonomy is a single connected
     *   component, therefore parent-child relations cannot be rejected.
     */
    public void setParentChildStatus(String childTerm, Status status) throws InvalidOperationException {
    	/*
    	 * 1 - If status = "rejected", reject the operation.
    	 * 2 - Otherwise, traverse the taxonomy until finding "childTerm" then change the status of "childTerm"
    	 */
    	if (childTerm == null || childTerm.equals(""))
    		throw new InvalidValueException("The child term cannot be empty or null.");
    	if (status == null)
    		throw new InvalidValueException("The status of parent-child relation cannot be null.");
		if (status.equals(Status.rejected))
			throw new InvalidOperationException("Parent-child relations cannot be rejected. Choose a new parent instead.");
		
		for(Taxonomy child: this.children) {
			if (child.getRoot().equals(childTerm)) {
				child.setStatus(status);
			} else {
				child.setParentChildStatus(childTerm, status);
			}
		}
	}

    /**
     * Get the string for the root term
     * @return
     */
    public String getRoot() {
        return root;
    }

    /**
     * Set the string for the root term
     * @return
     */
    public void setRoot(String root) {

        this.root = root;
    }

    /**
     * Get the double of linkScore
     * @return double
     */
    public double getLinkScore() {
        return linkScore;
    }

    /**
     * Get the double for the score
     * @return double
     */
    public double getScore() {
        return score;
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
        for(Taxonomy child : this.children) {
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
     * Is the originalTerm value below the newParent in the taxonomy
     * @param originalTerm The root value that may be in this taxonomy
     * @return
     */
    public boolean hasDescendentParent(String originalTerm) {
        if(this.root.equals(originalTerm))
            return true;
        for(Taxonomy child : children) {
            if(child.hasDescendent(originalTerm))
                return true;
        }
        return false;
    }



    /**
     * Search this taxonomy for a taxonomy with a given root
     * @param name The name to search for
     * @return A taxonomy whose root is name or null if no taxonomy is found
     */
    public Taxonomy antecendent(String name, String previous, Taxonomy previousTaxo, Taxonomy current) {
          if(this.root.equals(name)){
            return previousTaxo;
          }
          for(Taxonomy child : this.getChildren()) {
              Taxonomy d = child.antecendent(name, previous, this, child);
              if (d != null)
                return d;
          }
          return null;
    }


    /**
     * Get the parent of a given term or {@code null} if child term does not exist in
     * the taxonomy
     * @param termChild - the child term
     * @return a {@link String} representing the parent as in the taxonomy, or {@code null} if
     * child does not exist
     */
    public Taxonomy getParent(String termChild) {
    	
    	if(termChild == null || termChild.equals(""))
    		throw new InvalidValueException("term child cannot be null or empty");
    	
    	for(Taxonomy child: this.getChildren()) {
    		if (child.getRoot().equals(termChild))
    			return this;
    		else {
    			Taxonomy parent = child.getParent(termChild);
    			if (parent != null)
    				return parent;
    		}
    	}
		return null;
	}
    
    /**
     * Update the parent of a given term
     * 
     * @param termChild - the term to be moved to a new parent
     * @param termNewParent - the new parent term
     * 
     * @throws InvalidValueException - if any parameter is either {@code null} or an empty string
     * @throws InvalidOperationException - if the new parent is a descendant of the termChild
     * @throws RuntimeException - if either child or new parent term do not exist in this taxonomy
     */
    public void updateParent(String termChild, String termNewParent) {
		/*
		 * 1 - Verify if child exists, otherwise throw Exception
		 * 2 - Find new parent. If parent is child of termChild then throw InvalidOperationException.
		 * 3 - Remove termChild and its branch from older parent
		 * 4 - Add termChild and its branch to the new parent.
		 */
    	
    	if (termChild == null || termChild.equals(""))
    		throw new InvalidValueException("The term child parameter cannot be null or empty");
    	
    	if (termNewParent == null || termNewParent.equals(""))
    		throw new InvalidValueException("The new parent parameter cannot be null or empty");
    	
    	// 1 - Verify if child exists, otherwise throw Exception
    	Taxonomy child = this.descendent(termChild);
    	if (child == null)
    		throw new RuntimeException("The child term '" + termChild + "' does not exist in this taxonomy");
    	
    	// 2 - Find new parent. If parent is child of termChild then throw InvalidOperationException.
    	if (child.descendent(termNewParent) != null)
    		throw new InvalidOperationException("The new parent '" + termNewParent + "' cannot be a descendent of the term '" + termChild+ "'.");
    	Taxonomy newParent = this.descendent(termNewParent);
    	if (newParent == null)
    		throw new RuntimeException("The parent term '" + termNewParent + "' does not exist in this taxonomy");
    	
    	// 3 - Remove termChild and its branch from older parent
    	Taxonomy oldParent = this.getParent(termChild);
    	oldParent.removeChildBranch(termChild);
    	
    	// 4 - Add termChild and its branch to the new parent
    	newParent.addChild(child);
    }

    /**
     * If a term exists in the taxonomy, remove it and move its children to the parent.
     * 
     * @param termString - the term to be removed
     */
    public void removeDescendent(String termString) {
    	
    	for(Taxonomy child: this.getChildren()) {
    		if(child.getRoot().equals(termString)) {
    			List<Taxonomy> newChildren = this.getChildren();
    			for (Taxonomy grandchild: child.getChildren()) {
    				newChildren.add(grandchild);
    			}
    			newChildren.remove(child);
    			this.children = newChildren;
    			return;
    		} else {
    			child.removeDescendent(termString);
    		}
    	}
    }

    /**
     * Adds a taxonomy as child of this taxonomy node
     * 
     * @param child - the taxonomy to be added
     * 
     * @throws {@link InvalidValueException} - if the parameter is either null or an empty String root
     * @throws {@link InvalidOperationException} - if there is already a term with same String root as
     *  the one provided as parameter 
     */
    public void addChild(Taxonomy child) {
    	
    	if (child == null || child.getRoot().equals(""))
    		throw new InvalidValueException("The child term cannot be empty or null.");
    	
    	if (this.hasDescendent(child.getRoot()))
    		throw new InvalidOperationException("There is already a descendent with the specified term string value");
    	else
    		this.children.add(child);
    }
    
    /**
     * Search this taxonomy for a taxonomy with a given root
     * @param newParent The name to search for
     * @return A taxonomy whose root is name or null if no taxonomy is found
     */
    public Taxonomy addChild(Taxonomy child, Taxonomy newParent, String oldParentString) {

       // return newParent.(child);
        List<Taxonomy> newChildren = new ArrayList<>();
        for(Taxonomy childTaxo : newParent.getChildren()) {
            if(!childTaxo.root.equals(oldParentString)) {
                newChildren.add(childTaxo);
            }
        }

        newChildren.add(child);

        return new Taxonomy(this.root, this.score, this.linkScore, newChildren, this.status);
    }

    /**
     * Search this taxonomy for a taxonomy with a given root
     * @param currentTaxo The name to search for
     * @return A taxonomy whose root is name or null if no taxonomy is found
     */
    public Taxonomy addChildTaxo(Taxonomy child, Taxonomy currentTaxo, String parentString) {
        List<Taxonomy> newChildren = new ArrayList<>();
        if(this.root.equals(parentString)){
            newChildren.add(child);
            for(Taxonomy childTaxo : this.getChildren()) {
                newChildren.add(childTaxo);
            }
            return new Taxonomy(this.root, this.score, this.linkScore, newChildren, this.status);
        }

        for(Taxonomy childTaxo : currentTaxo.getChildren()) {
            if(!childTaxo.root.equals(parentString)) {
               newChildren.add(childTaxo);
            }
        }
        newChildren.add(child);
        return new Taxonomy(this.root, this.score, this.linkScore, newChildren, this.status);
    }

    /**
     * Remove a child term and all its branches, if the child exists
     * 
     * @param termString - the child to be removed
     */
    public void removeChildBranch(String termString) {
    	
    	if (termString == null || termString.equals(""))
    		throw new InvalidValueException("The child term cannot be empty or null.");
    	
    	for(int i=0; i<this.children.size(); i++) {
    		if (this.children.get(i).getRoot().equals(termString)) {
    			this.children.remove(i);
    			break;
    		}
    	}
    }

    /**
     * The size of the taxonomy (number of terms). Note this calculates the size 
     * and so takes O(N) time!
     * @return The number of terms in the taxonomy
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
    	return calculateMedian(leafDepths.values());
    	
    }
    
    /**
     * Calculate the median of a list of values
     * @param valueList The list of values to be considered
     * @return The median value
     */
    private double calculateMedian(Collection<Integer> valueList) {
    	Integer[] depthArray = new Integer[valueList.size()]; 
    	depthArray = valueList.toArray(depthArray);    	
    	Arrays.sort(depthArray);
    	
    	double median;
    	if (depthArray.length % 2 == 0)
    	    median = ((double)depthArray[depthArray.length/2] + (double)depthArray[depthArray.length/2 - 1])/2;
    	else
    	    median = (double) depthArray[depthArray.length/2];
    	
    	return median;
    }
    
    /**
     * Calculates the number of leaf nodes in the taxonomy
     * @return The number of leaf nodes in the taxonomy
     */
    public int numberOfLeafNodes() {
    	int numberOfLeaves = 0;
    	
    	for(Taxonomy child: children) {
    		if (child.children.isEmpty()) {
    			numberOfLeaves++;
    		} else {
    			numberOfLeaves+=child.numberOfLeafNodes();
    		}
    	}
    	
    	return numberOfLeaves;
    }
    
    /**
     * Calculates the number of branch nodes in the taxonomy
     * @return The number of branch nodes in the taxonomy
     */
    public int numberOfBranchNodes() {
    	//The root node is neither a leaf or a branch
    	return this.size() - (numberOfLeafNodes() + 1); 
    }
    
    /**
     * Return all leaf nodes and their correspondent depths
     * 
     * @param currentDepth The depth of the current node
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
     * Maximum degree of a node in the taxonomy (using graph theory, i.e. any edge counts including parent)
     * @return The maximum degree of a node in the taxonomy
     */
    public int maxDegree() {
    	int maximumDegree = children.size();
    	
    	for(Taxonomy child: children){
    		maximumDegree = Math.max(maximumDegree, child.maxDegree() + 1);
    	}
    	
    	return maximumDegree;
    }
    
    /**
     * Average degree of nodes in the taxonomy (using graph theory, i.e. any edge counts including parent)
     * @return The average degree of nodes in the taxonomy
     */
    public double avgDegree() {
    	int agg = children.size();
    	Queue<Taxonomy> nodesToVisit = new LinkedList<Taxonomy>();
    	nodesToVisit.addAll(children);
    	
    	do {
    		Taxonomy visitedNode = nodesToVisit.poll();
    		if (!visitedNode.children.isEmpty()) {
    			nodesToVisit.addAll(visitedNode.children);
    			agg+= visitedNode.children.size() +1;
    		} else {
    			agg+=1;
    		}
    	} while (!nodesToVisit.isEmpty());
    	
    	return ((double) agg)/size();
    }
    
    /**
     * Median degree of nodes in the taxonomy (using graph theory, i.e. any edge counts including parent)
     * @return The median of the degree of nodes in the taxonomy
     */ 
    public double medianDegree() {
    	Map<String,Integer> nodeDegrees = this.nodeDegrees(true);
    	return calculateMedian(nodeDegrees.values());
    }
    
    /**
     * Return all nodes and their correspondent degrees (using graph theory, i.e. any edge counts including parent)
     * 
     * @param isRoot Inform if the current node is the root of the whole graph
     * @return A map with 
     * 		'key': the label(root) of each node, and 
     * 		'value': the degree of each node.
     */
    protected Map<String, Integer> nodeDegrees(boolean isRoot) {
    	Map<String, Integer> nodeDegrees = new HashMap<String, Integer>();
    	
    	for(Taxonomy child: children) {
    		nodeDegrees.putAll(child.nodeDegrees(false));
    	}
    	
		if(isRoot)
			nodeDegrees.put(root, children.size());
		else
			nodeDegrees.put(root, children.size() + 1);
    	
		return nodeDegrees;
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
     * Verifies if all scores in the tree are real (except the root)
     * @return 
     */
    public boolean scoresValid() {
        return children.stream().allMatch((Taxonomy t) -> t._scoresValid());
        
    }
    
    private boolean _scoresValid() {
        return Double.isFinite(linkScore) && children.stream().allMatch((Taxonomy t) -> t._scoresValid());
    }

    /**
     * Create a copy of this with a new link score
     * @param linkScore The new link score
     * @return A new taxonomy instance
     */
    public Taxonomy withLinkScore(double linkScore) {
        return new Taxonomy(this.root, this.score, linkScore, this.children, this.status);
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
        return new Taxonomy(this.root, this.score, this.linkScore, newChildren, this.status);
    }

    /**
     * Create a deep copy of this taxonomy
     * @return A copy of this taxonomy
     */
    public Taxonomy deepCopyNewTaxo(String newParent, Taxonomy newTaxo, Taxonomy newParentTaxo) {
        List<Taxonomy> newChildren = new ArrayList<>();

        if(this.root.equals(newParent)) {
            for(Taxonomy t : this.children) {

                newChildren.add(t.deepCopy());
            }
            newChildren.add(newTaxo);
            return new Taxonomy(this.root, this.score, this.linkScore, newChildren, this.status);
        }

        for(Taxonomy t : newParentTaxo.children) {
            if(t.root.equals(newParent)){

                t = t.addChildTaxo(newTaxo, t, newParent);
                newChildren.add(t.deepCopy());
            } else {

                newChildren.add(t.deepCopyNewTaxo(newParent, newTaxo, t));
            }
        }

        return new Taxonomy(this.root, this.score, this.linkScore, newChildren, this.status);
    }

    /**
     * Create a deep copy of this taxonomy
     * @return A copy of this taxonomy
     */
    public Taxonomy deepCopyUpdatedTaxo(String newParent, Taxonomy newTaxo, Taxonomy newParentTaxo) {
        List<Taxonomy> newChildren = new ArrayList<>();
        Taxonomy descendent = newParentTaxo.antecendent(newParent, "", newParentTaxo, null);
        if (descendent != null) {
            for(Taxonomy t : newParentTaxo.children) {
                if(t.root.equals(descendent.root)){
                    t.children = new ArrayList<Taxonomy>();
                    t.children = newTaxo.children;
                    newChildren.add(t.deepCopy());
                } else {
                    newChildren.add(t.deepCopyUpdatedTaxo(newParent, newTaxo, t));
                }
            }
        } else {
            for(Taxonomy t : newParentTaxo.children) {
                    newChildren.add(t.deepCopyUpdatedTaxo(newParent, newTaxo, t));
            }
        }
        return new Taxonomy(newParentTaxo.root, newParentTaxo.score, newParentTaxo.linkScore, newChildren, newParentTaxo.status);
    }


    /**
     * Create a deep copy of this taxonomy
     * @return A copy of this taxonomy
     */
    public Taxonomy deepCopyNewParent(String termString, String oldParent, String newParent, Taxonomy newTaxo, Taxonomy newParentTaxo) {

        List<Taxonomy> newChildren = new ArrayList<>();
        for(Taxonomy t : children) {
            if (!t.root.equals(termString)) {
                if (t.root.equals(newParent)){
                    if (this.root.equals(newParent)) {
                        t.setStatus(Status.none);
                    }
                    for (Taxonomy newChild:newParentTaxo.children){
                        if (newChild.root.equals(termString)){
                            t.children.add(newChild);
                            newChildren.add(t.deepCopyNewParent(termString, oldParent, newParent, newTaxo, newParentTaxo));
                        }
                    }

                } else {
                    newChildren.add(t.deepCopyNewParent(termString, oldParent, newParent, newTaxo, newParentTaxo));
                }

            }
        }
        return new Taxonomy(this.root, this.score, this.linkScore, newChildren, this.status);
    }

    /**
     * Create a deep copy of this taxonomy
     * @return A copy of this taxonomy
     */
    public Taxonomy deepCopyNewTerm(String termString, String newTermString) {

        List<Taxonomy> newChildren = new ArrayList<>();
        for(Taxonomy t : children) {

            if (!t.root.equals(termString)) {
                newChildren.add(t.deepCopyNewTerm(termString, newTermString));
            } else {
                t.setRoot(newTermString);
                newChildren.add(t.deepCopyNewTerm(termString, newTermString));
            }
        }
        return new Taxonomy(this.root, this.score, this.linkScore, newChildren, this.status);
    }

    /**
     * Create a deep copy of this taxonomy
     * @return A copy of this taxonomy
     */
    public Taxonomy deepCopySetTermStatus(String termString, Status status) {

        List<Taxonomy> newChildren = new ArrayList<>();
        for(Taxonomy t : children) {

            if (!t.root.equals(termString)) {
                newChildren.add(t.deepCopySetTermStatus(termString, status));
            } else {
                t.setRoot(termString);
                newChildren.add(t.deepCopySetTermStatus(termString, status));
            }
        }
        return new Taxonomy(this.root, this.score, this.linkScore, newChildren, this.status);
    }

    /**
     * Create a deep copy of this taxonomy
     * @return A copy of this taxonomy
     */
    public Taxonomy deepCopySetTermRelationshipStatus(String termString, Status status) {

        List<Taxonomy> newChildren = new ArrayList<>();
        for(Taxonomy t : children) {

            if (!t.root.equals(termString)) {
                newChildren.add(t.deepCopySetTermRelationshipStatus(termString, status));
            } else {
                t.setStatus(status);
                newChildren.add(t.deepCopySetTermRelationshipStatus(termString, status));
            }
        }
        return new Taxonomy(this.root, this.score, this.linkScore, newChildren, this.status);
    }

    /**
     * Create a deep copy of this taxonomy
     * @return A copy of this taxonomy
     */
    public Taxonomy deepCopyMoveChildTerms(String termString, Taxonomy term, Taxonomy termParent) {

        List<Taxonomy> newChildren = new ArrayList<>();
        if (term != null) {
            for(Taxonomy t : term.children) {
                newChildren.add(t.deepCopy());
            }
            for(Taxonomy t : termParent.children) {
                if (!t.root.equals(termString)) {
                    newChildren.add(t.deepCopy());
                }
            }
        }

        return new Taxonomy(termParent.root, termParent.score, termParent.linkScore, newChildren, this.status);
    }


    /**
     * Build a Taxonomy object from a string
     * @param json file to read from
     * @return a Taxonomy object
     *
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static Taxonomy fromJsonString(String json) throws JsonParseException, JsonMappingException, IOException{
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, Taxonomy.class);
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
    
    public static class Builder{
    	
    	private Taxonomy taxonomy;
    	
    	public Builder() {
    		taxonomy = new Taxonomy();
    	}
    	
    	public Builder(Taxonomy taxonomy) {
    		this.taxonomy = taxonomy;
    	}
    	
    	public Builder root(String root) {
    		taxonomy.root = root;
    		return this;
    	}
    	public Builder status(Status status) {
    		taxonomy.status = status;
    		return this;
    	}
    	
    	public Builder addChild(Taxonomy childBranch) {
    		taxonomy.children.add(childBranch);
    		
    		return this;
    	}
    	
    	public Taxonomy build() {
    		return taxonomy;
    	}
	}
}
